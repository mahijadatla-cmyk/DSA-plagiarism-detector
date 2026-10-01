package plagiarism;

/** Comparison outcome for a pair of documents. */
public class Result {
    public TextDoc a, b;
    public boolean[] markA, markB;      // per normalised char: covered by a match
    public Passage[] passagesA, passagesB;
    public int coveredA, coveredB;
    public double pctA, pctB, overall;  // % of A / B copied, combined similarity
    public int windowsCompared, hashHits, collisionsRejected;
    public long micros;
}
