package plagiarism;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Console entry point.
 *   java -cp out plagiarism.Main [-k 25] [-html report.html] file1 file2 [file3 ...]
 *   java -cp out plagiarism.Main [options] <directory>
 * With no arguments the Swing GUI is launched.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) { PlagiarismGUI.launch(); return; }
        int k = 25;
        String html = null;
        String[] paths = new String[args.length];
        int np = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-k")) k = Integer.parseInt(args[++i]);
            else if (args[i].equals("-html")) html = args[++i];
            else paths[np++] = args[i];
        }
        TextDoc[] docs = loadDocs(paths, np);
        if (docs.length < 2) { System.out.println("Need at least two documents."); return; }

        Detector det = new Detector(k);
        int pairs = docs.length * (docs.length - 1) / 2;
        Result[] res = new Result[pairs];
        int p = 0;
        for (int i = 0; i < docs.length; i++)
            for (int j = i + 1; j < docs.length; j++) res[p++] = det.compare(docs[i], docs[j]);
        sortDesc(res);

        System.out.println("=== Plagiarism Detection (Rabin-Karp double rolling hash, k=" + k + ") ===\n");
        System.out.println(String.format("%-22s %-22s %8s %8s %9s", "Doc A", "Doc B", "A%", "B%", "Overall%"));
        for (int i = 0; i < pairs; i++) {
            Result r = res[i];
            System.out.println(String.format("%-22s %-22s %8s %8s %9s", r.a.name, r.b.name,
                    Reporter.fmt(r.pctA), Reporter.fmt(r.pctB), Reporter.fmt(r.overall)));
        }
        Result top = res[0];
        System.out.println("\n--- Most similar pair: " + top.a.name + " vs " + top.b.name + " ---");
        System.out.println("Windows hashed: " + top.windowsCompared + ", hash hits: " + top.hashHits
                + ", collisions rejected: " + top.collisionsRejected + ", time: " + top.micros + " us");
        System.out.println("\n[" + top.a.name + "]\n" + Reporter.ansi(top.a, top.markA));
        System.out.println("\n[" + top.b.name + "]\n" + Reporter.ansi(top.b, top.markB));
        System.out.println("\nCopied passages in " + top.b.name + ":");
        for (int i = 0; i < top.passagesB.length; i++) {
            Passage ps = top.passagesB[i];
            System.out.println("  #" + (i + 1) + " \"" + new String(top.b.norm, ps.start, ps.end - ps.start).trim() + "\"");
        }
        if (html != null) {
            FileWriter w = new FileWriter(html);
            w.write(Reporter.htmlReport(res, pairs, k));
            w.close();
            System.out.println("\nHTML report written to " + html);
        }
    }

    static TextDoc[] loadDocs(String[] paths, int np) throws IOException {
        if (np == 1 && new File(paths[0]).isDirectory()) {
            File[] fs = new File(paths[0]).listFiles();
            String[] names = new String[fs.length];
            int c = 0;
            for (int i = 0; i < fs.length; i++)
                if (fs[i].isFile() && fs[i].getName().endsWith(".txt")) names[c++] = fs[i].getPath();
            for (int i = 1; i < c; i++) {                       // insertion sort, no java.util
                String key = names[i]; int j = i - 1;
                while (j >= 0 && names[j].compareTo(key) > 0) { names[j + 1] = names[j]; j--; }
                names[j + 1] = key;
            }
            paths = names; np = c;
        }
        TextDoc[] d = new TextDoc[np];
        for (int i = 0; i < np; i++) d[i] = TextDoc.fromFile(paths[i]);
        return d;
    }

    static void sortDesc(Result[] r) {
        for (int i = 1; i < r.length; i++) {
            Result key = r[i]; int j = i - 1;
            while (j >= 0 && r[j].overall < key.overall) { r[j + 1] = r[j]; j--; }
            r[j + 1] = key;
        }
    }
}
