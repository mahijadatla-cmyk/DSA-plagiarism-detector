package plagiarism;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

/** Swing interface: pick documents, run comparison, see highlighted passages. */
public class PlagiarismGUI {
    private static TextDoc[] docs = new TextDoc[0];
    private static Result[] results = new Result[0];

    public static void launch() {
        SwingUtilities.invokeLater(new Runnable() { public void run() { build(); } });
    }

    private static void build() {
        final JFrame f = new JFrame("Plagiarism Detection System - Rolling Hash (Rabin-Karp)");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1100, 700);

        final JTextPane left = new JTextPane(), right = new JTextPane();
        left.setEditable(false); right.setEditable(false);
        final JLabel summary = new JLabel(" Load two or more .txt files to begin.");
        summary.setFont(summary.getFont().deriveFont(15f));
        final JTextField kField = new JTextField("25", 3);
        final DefaultListModel<String> model = new DefaultListModel<String>();
        final JList<String> list = new JList<String>(model);

        JButton load = new JButton("Load documents...");
        JButton run = new JButton("Detect plagiarism");
        run.setBackground(new Color(176, 0, 0)); run.setForeground(Color.WHITE);

        load.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(".");
            fc.setMultiSelectionEnabled(true);
            if (fc.showOpenDialog(f) != JFileChooser.APPROVE_OPTION) return;
            File[] sel = fc.getSelectedFiles();
            docs = new TextDoc[sel.length];
            try {
                for (int i = 0; i < sel.length; i++) docs[i] = TextDoc.fromFile(sel[i].getPath());
            } catch (Exception ex) { summary.setText(" Error: " + ex.getMessage()); return; }
            summary.setText(" " + sel.length + " document(s) loaded.");
        });

        run.addActionListener(e -> {
            if (docs.length < 2) { summary.setText(" Load at least two documents."); return; }
            Detector det = new Detector(Integer.parseInt(kField.getText().trim()));
            int pairs = docs.length * (docs.length - 1) / 2;
            results = new Result[pairs];
            int p = 0;
            for (int i = 0; i < docs.length; i++)
                for (int j = i + 1; j < docs.length; j++) results[p++] = det.compare(docs[i], docs[j]);
            Main.sortDesc(results);
            model.clear();
            for (int i = 0; i < pairs; i++)
                model.addElement(results[i].a.name + " vs " + results[i].b.name + "  -  "
                        + Reporter.fmt(results[i].overall) + "%");
            list.setSelectedIndex(0);
        });

        list.addListSelectionListener(e -> {
            int i = list.getSelectedIndex();
            if (i < 0 || i >= results.length) return;
            Result r = results[i];
            show(left, r.a, r.markA);
            show(right, r.b, r.markB);
            summary.setText(" " + r.a.name + " copied: " + Reporter.fmt(r.pctA) + "%   |   " + r.b.name
                    + " copied: " + Reporter.fmt(r.pctB) + "%   |   Overall similarity: "
                    + Reporter.fmt(r.overall) + "%   |   " + r.micros + " us");
        });

        JPanel top = new JPanel();
        top.add(load); top.add(new JLabel("Window k:")); top.add(kField); top.add(run);
        JPanel texts = new JPanel(new GridLayout(1, 2, 6, 6));
        texts.add(new JScrollPane(left)); texts.add(new JScrollPane(right));
        JScrollPane ls = new JScrollPane(list);
        ls.setPreferredSize(new java.awt.Dimension(260, 100));
        f.add(top, BorderLayout.NORTH);
        f.add(ls, BorderLayout.WEST);
        f.add(texts, BorderLayout.CENTER);
        f.add(summary, BorderLayout.SOUTH);
        f.setVisible(true);
    }

    private static void show(JTextPane pane, TextDoc d, boolean[] mark) {
        pane.setText(d.original);
        Highlighter h = pane.getHighlighter();
        h.removeAllHighlights();
        Highlighter.HighlightPainter painter =
                new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 213, 79));
        boolean[] o = Reporter.originalMarks(d, mark);
        String plain = pane.getText().replace("\r", "");
        int n = Math.min(o.length, plain.length());
        int i = 0;
        try {
            while (i < n) {
                if (o[i]) {
                    int s = i;
                    while (i < n && o[i]) i++;
                    h.addHighlight(s, i, painter);
                } else i++;
            }
        } catch (Exception ignored) { }
    }
}
