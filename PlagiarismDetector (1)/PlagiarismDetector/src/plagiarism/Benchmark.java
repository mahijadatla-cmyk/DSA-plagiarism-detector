package plagiarism;

/** Rolling hash (O(n+m)) versus naive window-by-window comparison (O(n*m*k)). */
public class Benchmark {
    static long seed = 12345;
    static int rnd(int n) { seed = seed * 6364136223846793005L + 1442695040888963407L; return (int) ((seed >>> 33) % n); }

    static String[] vocab = {"data","structure","algorithm","hash","window","string","match","tree","graph","array",
        "queue","stack","search","sort","index","table","memory","pointer","system","design","compare","document"};

    static String text(int words) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words; i++) sb.append(vocab[rnd(vocab.length)]).append(rnd(50) == 0 ? ". " : " ");
        return sb.toString();
    }

    static int naive(char[] a, char[] b, int k) {
        int hits = 0;
        for (int i = 0; i + k <= a.length; i++)
            for (int j = 0; j + k <= b.length; j++) {
                int t = 0;
                while (t < k && a[i + t] == b[j + t]) t++;
                if (t == k) hits++;
            }
        return hits;
    }

    public static void main(String[] args) {
        int[] sizes = {500, 1000, 2000, 4000, 8000};
        System.out.println("words,chars,rolling_ms,naive_ms");
        for (int s = 0; s < sizes.length; s++) {
            String base = text(sizes[s]);
            String other = base.substring(0, base.length() / 2) + text(sizes[s] / 2);
            TextDoc a = new TextDoc("a", base), b = new TextDoc("b", other);
            Detector d = new Detector(25);
            d.compare(a, b);                                   // warm-up
            long t0 = System.nanoTime();
            Result r = d.compare(a, b);
            double rolling = (System.nanoTime() - t0) / 1e6;
            t0 = System.nanoTime();
            naive(a.norm, b.norm, 25);
            double nv = (System.nanoTime() - t0) / 1e6;
            System.out.println(sizes[s] + "," + a.norm.length + "," + Reporter.fmt(rolling) + "," + Reporter.fmt(nv)
                    + "   similarity=" + Reporter.fmt(r.overall) + "% rejectedCollisions=" + r.collisionsRejected);
        }
    }
}
