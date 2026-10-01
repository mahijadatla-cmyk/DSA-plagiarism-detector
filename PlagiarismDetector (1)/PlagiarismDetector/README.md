# Plagiarism Detection System using Rolling Hash Techniques
DSA-3 (25CS2103E) - Team 10, Section 5

Rabin-Karp double rolling hash, custom hash table, no `java.util.*`
(only java.lang, java.io and javax.swing for the GUI).

## Structure
| File | Purpose |
|---|---|
| `IntList.java` | Growable int array (own ArrayList replacement) |
| `HashIndex.java` | Chained hash table: 64-bit key -> window positions |
| `RollingHash.java` | Polynomial rolling hash, two (base, mod) pairs = double hashing |
| `TextDoc.java` | Loads/normalises a document, keeps index map to original text |
| `Detector.java` | Indexes doc A, probes with doc B, verifies, builds passages and similarity % |
| `Reporter.java` | ANSI console highlighting and HTML report |
| `Main.java` | Command line entry (no args = launch GUI) |
| `PlagiarismGUI.java` | Swing interface with highlighted side-by-side view |
| `Benchmark.java` | Rolling hash vs naive comparison timing |
| `Server.java` | Local HTTP API (`/api/compare`) so `frontend/collate.html` can call the real detector |

## Build and run
    mkdir out
    javac -d out src/plagiarism/*.java

    java -cp out plagiarism.Main                       # GUI
    java -cp out plagiarism.Main -k 20 samples         # every .txt in a folder
    java -cp out plagiarism.Main -k 20 -html report.html a.txt b.txt c.txt
    java -cp out plagiarism.Benchmark                  # scalability test

## How it works
1. Normalise: lower-case, letters/digits only, single spaces (index map kept).
2. Hash every k-character window with H(i+1) = ((H(i) - c[i]*B^(k-1))*B + c[i+k]) mod M, for two (B, M) pairs.
3. Store document A's 64-bit keys in `HashIndex`; probe with document B's keys.
4. Verify each hit character by character (collisions rejected, never reported).
5. Merge marked characters into passages; similarity = copied chars / total chars.
   Overall = (copiedA + copiedB) / (lenA + lenB).

Larger `k` = stricter (fewer, longer matches). Default 25 characters.

## Web frontend (talks to this Java backend)
1. Start the API:
       java -cp out plagiarism.Server        # listens on http://localhost:8080
   (pass a port number as an argument to use a different one, e.g. `plagiarism.Server 9090`)
2. Open `frontend/collate.html` in a browser (just double-click it, no build step).
3. Check the status dot next to "Collate" says "Backend connected". If not, click Reconnect,
   or fix the address in the "Backend" box if you changed the port.
4. Upload 2+ `.txt` files, adjust match sensitivity if you like, click **Compare documents**.

The page never leaves your machine — it only calls `localhost`. All the detection (rolling
hash, double hashing, collision verification, similarity, highlighting) happens in
`Server.java` → `Detector.java` → `Reporter.java`, the same code the CLI and GUI use.
