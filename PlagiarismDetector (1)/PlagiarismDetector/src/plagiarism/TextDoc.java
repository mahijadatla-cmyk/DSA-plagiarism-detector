package plagiarism;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * A document. Text is normalised (lower-case, letters/digits only, single
 * spaces) and every normalised char remembers its index in the original text
 * so matches can be highlighted in the untouched original.
 */
public class TextDoc {
    public final String name;
    public final String original;
    public final char[] norm;
    public final int[] origIndex;

    public TextDoc(String name, String text) {
        this.name = name;
        this.original = text;
        char[] buf = new char[text.length()];
        int[] map = new int[text.length()];
        int n = 0;
        boolean lastSpace = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                buf[n] = Character.toLowerCase(c);
                map[n++] = i;
                lastSpace = false;
            } else if (!lastSpace) {
                buf[n] = ' ';
                map[n++] = i;
                lastSpace = true;
            }
        }
        if (n > 0 && buf[n - 1] == ' ') n--;
        norm = new char[n];
        origIndex = new int[n];
        for (int i = 0; i < n; i++) { norm[i] = buf[i]; origIndex[i] = map[i]; }
    }

    public static TextDoc fromFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new FileReader(path));
        try {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        } finally { r.close(); }
        String name = path;
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (slash >= 0) name = path.substring(slash + 1);
        return new TextDoc(name, sb.toString());
    }
}
