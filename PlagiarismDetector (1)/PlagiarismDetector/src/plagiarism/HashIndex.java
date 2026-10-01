package plagiarism;

/**
 * Custom hash table (separate chaining) mapping a 64-bit double-hash key
 * to every window position that produced it. No java.util used.
 */
public class HashIndex {
    private final int[] buckets;   // head entry per bucket, -1 = empty
    private long[] keys;
    private int[] pos;
    private int[] next;
    private int count = 0;

    public HashIndex(int expected) {
        int cap = 16;
        while (cap < expected * 2) cap <<= 1;
        buckets = new int[cap];
        for (int i = 0; i < cap; i++) buckets[i] = -1;
        keys = new long[expected + 1];
        pos = new int[expected + 1];
        next = new int[expected + 1];
    }

    private int slot(long key) {
        long h = key * 0x9E3779B97F4A7C15L;
        return (int) ((h >>> 33) & (buckets.length - 1));
    }

    public void put(long key, int position) {
        if (count == keys.length) grow();
        int s = slot(key);
        keys[count] = key;
        pos[count] = position;
        next[count] = buckets[s];
        buckets[s] = count++;
    }

    /** First entry index for key, or -1. Use nextEntry to walk same-key entries. */
    public int find(long key) {
        int e = buckets[slot(key)];
        while (e != -1 && keys[e] != key) e = next[e];
        return e;
    }
    public int nextEntry(int e, long key) {
        e = next[e];
        while (e != -1 && keys[e] != key) e = next[e];
        return e;
    }
    public int positionAt(int e) { return pos[e]; }

    private void grow() {
        int n = keys.length * 2;
        long[] k = new long[n]; int[] p = new int[n]; int[] x = new int[n];
        for (int i = 0; i < count; i++) { k[i] = keys[i]; p[i] = pos[i]; x[i] = next[i]; }
        keys = k; pos = p; next = x;
    }
}
