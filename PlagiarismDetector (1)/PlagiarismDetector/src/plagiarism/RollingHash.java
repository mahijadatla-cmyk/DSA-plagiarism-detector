package plagiarism;

/**
 * Polynomial rolling hash (Rabin-Karp) with DOUBLE hashing:
 * two independent (base, modulus) pairs are combined into one 64-bit key,
 * making accidental collisions astronomically unlikely.
 *
 *   H(i+1) = ( (H(i) - c[i] * B^(k-1)) * B + c[i+k] ) mod M
 */
public class RollingHash {
    static final long B1 = 257,  M1 = 1_000_000_007L;
    static final long B2 = 131,  M2 = 998_244_353L;

    /** Returns key[i] for every window text[i..i+k-1]; length = n-k+1 (or 0). */
    public static long[] windowKeys(char[] t, int k) {
        int n = t.length;
        if (n < k || k <= 0) return new long[0];
        long p1 = 1, p2 = 1;                       // B^(k-1) mod M
        for (int i = 0; i < k - 1; i++) { p1 = p1 * B1 % M1; p2 = p2 * B2 % M2; }
        long h1 = 0, h2 = 0;
        for (int i = 0; i < k; i++) {
            h1 = (h1 * B1 + t[i]) % M1;
            h2 = (h2 * B2 + t[i]) % M2;
        }
        long[] keys = new long[n - k + 1];
        keys[0] = (h1 << 32) | h2;
        for (int i = 1; i <= n - k; i++) {
            h1 = ((h1 - t[i - 1] * p1 % M1 + M1) * B1 + t[i + k - 1]) % M1;
            h2 = ((h2 - t[i - 1] * p2 % M2 + M2) * B2 + t[i + k - 1]) % M2;
            keys[i] = (h1 << 32) | h2;
        }
        return keys;
    }
}
