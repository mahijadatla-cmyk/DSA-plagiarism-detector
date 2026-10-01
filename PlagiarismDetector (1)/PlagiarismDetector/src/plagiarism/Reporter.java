package plagiarism;

/** Renders highlighted output: ANSI (console) and HTML (browser report). */
public class Reporter {
    static final String RED = "\u001B[41;97m", OFF = "\u001B[0m";

    /** Converts normalised-char marks into marks over the ORIGINAL text. */
    public static boolean[] originalMarks(TextDoc d, boolean[] mark) {
        boolean[] o = new boolean[d.original.length()];
        for (int i = 0; i < mark.length; i++) {
            if (!mark[i]) continue;
            int from = d.origIndex[i];
            int to = (i + 1 < mark.length && mark[i + 1]) ? d.origIndex[i + 1] : from + 1;
            for (int p = from; p < to; p++) o[p] = true;
        }
        return o;
    }

    public static String ansi(TextDoc d, boolean[] mark) {
        boolean[] o = originalMarks(d, mark);
        StringBuilder sb = new StringBuilder();
        boolean on = false;
        for (int i = 0; i < d.original.length(); i++) {
            char c = d.original.charAt(i);
            if (o[i] && !on && c != '\n') { sb.append(RED); on = true; }
            if (!o[i] && on) { sb.append(OFF); on = false; }
            if (c == '\n' && on) { sb.append(OFF); on = false; }
            sb.append(c);
        }
        if (on) sb.append(OFF);
        return sb.toString();
    }

    static String esc(char c) {
        if (c == '<') return "&lt;";
        if (c == '>') return "&gt;";
        if (c == '&') return "&amp;";
        return String.valueOf(c);
    }

    public static String htmlBody(TextDoc d, boolean[] mark) {
        boolean[] o = originalMarks(d, mark);
        StringBuilder sb = new StringBuilder();
        boolean on = false;
        for (int i = 0; i < d.original.length(); i++) {
            char c = d.original.charAt(i);
            if (o[i] && !on) { sb.append("<mark>"); on = true; }
            if (!o[i] && on) { sb.append("</mark>"); on = false; }
            sb.append(esc(c));
        }
        if (on) sb.append("</mark>");
        return sb.toString();
    }

    public static String htmlReport(Result[] results, int n, int k) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Plagiarism Report</title><style>")
          .append("body{font-family:Segoe UI,Arial;margin:30px;background:#fafafa;color:#222}")
          .append("h1{color:#b00000}table{border-collapse:collapse;margin:10px 0 30px}")
          .append("td,th{border:1px solid #ccc;padding:6px 12px;text-align:center}th{background:#b00000;color:#fff}")
          .append(".pair{background:#fff;border:1px solid #ddd;padding:14px;margin:18px 0;border-radius:8px}")
          .append(".cols{display:flex;gap:16px}.col{flex:1;white-space:pre-wrap;font-size:14px;line-height:1.5}")
          .append("mark{background:#ffd54f;padding:0 1px}")
          .append(".hi{color:#b00000;font-weight:bold}</style></head><body>")
          .append("<h1>Plagiarism Detection Report</h1><p>Rolling hash window k = ").append(k).append(" characters</p>")
          .append("<table><tr><th>Document A</th><th>Document B</th><th>A copied %</th><th>B copied %</th><th>Overall %</th></tr>");
        for (int i = 0; i < n; i++) {
            Result r = results[i];
            sb.append("<tr><td>").append(r.a.name).append("</td><td>").append(r.b.name).append("</td><td>")
              .append(fmt(r.pctA)).append("</td><td>").append(fmt(r.pctB)).append("</td><td class='hi'>")
              .append(fmt(r.overall)).append("</td></tr>");
        }
        sb.append("</table>");
        for (int i = 0; i < n; i++) {
            Result r = results[i];
            sb.append("<div class='pair'><h3>").append(r.a.name).append(" vs ").append(r.b.name)
              .append(" &mdash; ").append(fmt(r.overall)).append("% similar</h3><div class='cols'><div class='col'>")
              .append(htmlBody(r.a, r.markA)).append("</div><div class='col'>")
              .append(htmlBody(r.b, r.markB)).append("</div></div></div>");
        }
        return sb.append("</body></html>").toString();
    }

    public static String fmt(double v) {
        long x = Math.round(v * 10);
        return (x / 10) + "." + (x % 10);
    }
}
