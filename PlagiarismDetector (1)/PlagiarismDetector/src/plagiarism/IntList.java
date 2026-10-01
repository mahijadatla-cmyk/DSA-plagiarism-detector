package plagiarism;

/** Growable int array written from scratch (replaces ArrayList<Integer>). */
public class IntList {
    private int[] data = new int[16];
    private int size = 0;

    public void add(int v) {
        if (size == data.length) {
            int[] bigger = new int[size * 2];
            for (int i = 0; i < size; i++) bigger[i] = data[i];
            data = bigger;
        }
        data[size++] = v;
    }
    public int get(int i) { return data[i]; }
    public int size() { return size; }
}
