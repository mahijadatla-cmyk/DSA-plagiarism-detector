package plagiarism;

/** Compares two documents with rolling hashes; O(n + m) expected per pair. */
public class Detector {
    private final int k;   // window length in normalised characters

    public Detector(int windowSize) { this.k = windowSize; }
    public int windowSize() { return k; }

    public Result compare(TextDoc a, TextDoc b) {
        long t0 = System.nanoTime();
        Result r = new Result();
        r.a = a; r.b = b;
        r.markA = new boolean[a.norm.length];
        r.markB = new boolean[b.norm.length];

        long[] keysA = RollingHash.windowKeys(a.norm, k);
        long[] keysB = RollingHash.windowKeys(b.norm, k);
        r.windowsCompared = keysB.length;

        HashIndex index = new HashIndex(keysA.length);
        for (int i = 0; i < keysA.length; i++) index.put(keysA[i], i);

        for (int j = 0; j < keysB.length; j++) {
            for (int e = index.find(keysB[j]); e != -1; e = index.nextEntry(e, keysB[j])) {
                int i = index.positionAt(e);
                r.hashHits++;
                // Third safety net: verify char-by-char so a collision can never
                // become a false positive.
                if (sameWindow(a.norm, i, b.norm, j)) {
                    for (int x = 0; x < k; x++) { r.markA[i + x] = true; r.markB[j + x] = true; }
                } else r.collisionsRejected++;
            }
        }
        r.passagesA = passages(r.markA);
        r.passagesB = passages(r.markB);
        r.coveredA = count(r.markA);
        r.coveredB = count(r.markB);
        int la = a.norm.length, lb = b.norm.length;
        r.pctA = la == 0 ? 0 : 100.0 * r.coveredA / la;
        r.pctB = lb == 0 ? 0 : 100.0 * r.coveredB / lb;
        r.overall = (la + lb) == 0 ? 0 : 100.0 * (r.coveredA + r.coveredB) / (la + lb);
        r.micros = (System.nanoTime() - t0) / 1000;
        return r;
    }

    private boolean sameWindow(char[] x, int i, char[] y, int j) {
        for (int t = 0; t < k; t++) if (x[i + t] != y[j + t]) return false;
        return true;
    }

    private static int count(boolean[] m) {
        int c = 0;
        for (int i = 0; i < m.length; i++) if (m[i]) c++;
        return c;
    }

    /** Turns marked chars into contiguous passages (runs). */
    static Passage[] passages(boolean[] m) {
        int runs = 0;
        for (int i = 0; i < m.length; i++) if (m[i] && (i == 0 || !m[i - 1])) runs++;
        Passage[] out = new Passage[runs];
        int n = 0, i = 0;
        while (i < m.length) {
            if (m[i]) {
                int s = i;
                while (i < m.length && m[i]) i++;
                out[n++] = new Passage(s, i);
            } else i++;
        }
        return out;
    }
}
