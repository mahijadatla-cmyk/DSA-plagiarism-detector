package plagiarism;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Minimal local HTTP API in front of the detection engine, so a web
 * frontend can call the same Rabin-Karp code the CLI/GUI use.
 *
 *   java -cp out plagiarism.Server [port]      (default port 8080)
 *
 * POST /api/compare?k=25   multipart/form-data, one or more "files" parts.
 * Response: JSON with one entry per document pair, already highlighted
 * (uses Reporter.htmlBody), sorted by similarity, most similar first.
 */
public class Server {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/compare", new CompareHandler());
        server.createContext("/api/health", new HealthHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Plagiarism API listening on http://localhost:" + port);
        System.out.println("  POST /api/compare?k=25   (multipart/form-data, field name: files)");
        System.out.println("Open the frontend HTML file in a browser and point it at this address.");
    }

    static class HealthHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex);
            if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            byte[] b = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, b.length);
            OutputStream os = ex.getResponseBody(); os.write(b); os.close();
        }
    }

    static class CompareHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            cors(ex);
            if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(204, -1); return; }
            if (!ex.getRequestMethod().equals("POST")) {
                sendJson(ex, 405, "{\"error\":\"Use POST\"}");
                return;
            }
            try {
                int k = queryInt(ex.getRequestURI().getQuery(), "k", 25);
                String contentType = ex.getRequestHeaders().getFirst("Content-Type");
                String boundary = boundaryOf(contentType);
                if (boundary == null) { sendJson(ex, 400, "{\"error\":\"Expected multipart/form-data\"}"); return; }

                byte[] body = readAll(ex.getRequestBody());
                TextDoc[] docs = parseParts(body, boundary);
                if (docs.length < 2) { sendJson(ex, 400, "{\"error\":\"Upload at least two documents\"}"); return; }

                Detector det = new Detector(k);
                int pairCount = docs.length * (docs.length - 1) / 2;
                Result[] res = new Result[pairCount];
                int p = 0;
                for (int i = 0; i < docs.length; i++)
                    for (int j = i + 1; j < docs.length; j++) res[p++] = det.compare(docs[i], docs[j]);
                Main.sortDesc(res);

                StringBuilder json = new StringBuilder();
                json.append("{\"k\":").append(k).append(",\"pairs\":[");
                for (int i = 0; i < res.length; i++) {
                    Result r = res[i];
                    int shared = 0;
                    for (int t = 0; t < r.passagesA.length; t++) shared += r.passagesA[t].end - r.passagesA[t].start;
                    if (i > 0) json.append(',');
                    json.append('{')
                        .append("\"a\":\"").append(esc(r.a.name)).append("\",")
                        .append("\"b\":\"").append(esc(r.b.name)).append("\",")
                        .append("\"similarity\":").append(round1(r.overall)).append(',')
                        .append("\"pctA\":").append(round1(r.pctA)).append(',')
                        .append("\"pctB\":").append(round1(r.pctB)).append(',')
                        .append("\"passages\":").append(r.passagesA.length).append(',')
                        .append("\"sharedChars\":").append(shared).append(',')
                        .append("\"htmlA\":\"").append(esc(Reporter.htmlBody(r.a, r.markA))).append("\",")
                        .append("\"htmlB\":\"").append(esc(Reporter.htmlBody(r.b, r.markB))).append("\"")
                        .append('}');
                }
                json.append("]}");
                sendJson(ex, 200, json.toString());
            } catch (Exception e) {
                sendJson(ex, 500, "{\"error\":\"" + esc(String.valueOf(e.getMessage())) + "\"}");
            }
        }
    }

    // ---- multipart/form-data parsing (byte-preserving via ISO-8859-1) ----
    static TextDoc[] parseParts(byte[] body, String boundary) {
        String raw = new String(body, StandardCharsets.ISO_8859_1);
        String delim = "--" + boundary;
        TextDoc[] tmp = new TextDoc[64];
        int count = 0;
        int pos = raw.indexOf(delim);
        while (pos >= 0) {
            int partStart = pos + delim.length();
            if (raw.startsWith("--", partStart)) break; // final boundary
            int next = raw.indexOf(delim, partStart);
            if (next < 0) break;
            String part = raw.substring(partStart, next);
            int headerEnd = part.indexOf("\r\n\r\n");
            if (headerEnd >= 0) {
                String headers = part.substring(0, headerEnd);
                String content = part.substring(headerEnd + 4);
                if (content.endsWith("\r\n")) content = content.substring(0, content.length() - 2);
                String filename = attr(headers, "filename");
                String name = attr(headers, "name");
                if (filename != null && filename.length() > 0) {
                    byte[] raw8859 = content.getBytes(StandardCharsets.ISO_8859_1);
                    String text = new String(raw8859, StandardCharsets.UTF_8);
                    if (count == tmp.length) { TextDoc[] bigger = new TextDoc[count * 2]; System.arraycopy(tmp, 0, bigger, 0, count); tmp = bigger; }
                    tmp[count++] = new TextDoc(filename, text);
                }
            }
            pos = next;
        }
        TextDoc[] out = new TextDoc[count];
        System.arraycopy(tmp, 0, out, 0, count);
        return out;
    }

    static String attr(String headerBlock, String key) {
        String marker = key + "=\"";
        int i = headerBlock.indexOf(marker);
        if (i < 0) return null;
        int start = i + marker.length();
        int end = headerBlock.indexOf('"', start);
        return end < 0 ? null : headerBlock.substring(start, end);
    }

    static String boundaryOf(String contentType) {
        if (contentType == null) return null;
        int i = contentType.indexOf("boundary=");
        if (i < 0) return null;
        String b = contentType.substring(i + 9);
        if (b.startsWith("\"") && b.endsWith("\"")) b = b.substring(1, b.length() - 1);
        return b;
    }

    static int queryInt(String query, String key, int def) {
        if (query == null) return def;
        int i = query.indexOf(key + "=");
        if (i < 0) return def;
        int start = i + key.length() + 1;
        int end = query.indexOf('&', start);
        String v = end < 0 ? query.substring(start) : query.substring(start, end);
        try { return Integer.parseInt(v); } catch (Exception e) { return def; }
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toByteArray();
    }

    static void cors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, b.length);
        OutputStream os = ex.getResponseBody(); os.write(b); os.close();
    }

    static String round1(double v) {
        long x = Math.round(v * 10);
        return (x / 10) + "." + Math.abs(x % 10);
    }

    static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
