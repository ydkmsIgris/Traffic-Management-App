import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * TrafficManagementApp - Main Swing GUI Application
 *
 * Data Structures Used:
 *   1. HashMap (adjacency list in TrafficGraph)
 *   2. Priority Queue / Min-Heap (Dijkstra's)
 *   3. LinkedList (edge lists, BFS queue)
 *   4. Stack (DFS)
 *
 * Algorithms:
 *   1. Dijkstra's Shortest Path  - O((V+E) log V)
 *   2. BFS Traversal             - O(V + E)
 *   3. DFS Traversal             - O(V + E)
 */
public class TrafficManagementApp extends JFrame {

    // ─── Colors / Theme ──────────────────────────────────────────────────────
    private static final Color BG_DARK     = new Color(15, 23, 42);
    private static final Color BG_PANEL    = new Color(30, 41, 59);
    private static final Color BG_INPUT    = new Color(51, 65, 85);
    private static final Color ACCENT      = new Color(56, 189, 248);
    private static final Color ACCENT2     = new Color(99, 102, 241);
    private static final Color SUCCESS     = new Color(34, 197, 94);
    private static final Color WARNING     = new Color(251, 191, 36);
    private static final Color DANGER      = new Color(239, 68, 68);
    private static final Color TEXT_MAIN   = new Color(226, 232, 240);
    private static final Color TEXT_DIM    = new Color(148, 163, 184);
    private static final Color BORDER_COL  = new Color(51, 65, 85);

    private static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD, 13);
    private static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font FONT_MONO   = new Font("Monospaced", Font.PLAIN, 12);

    // ─── Core ────────────────────────────────────────────────────────────────
    private final TrafficGraph graph = new TrafficGraph();
    private GraphCanvas canvas;

    // ─── Input Fields ────────────────────────────────────────────────────────
    private JTextField tfNodeName;
    private JTextField tfFrom, tfTo, tfWeight, tfCongestion;
    private JComboBox<String> cbFrom, cbTo, cbStart, cbEnd, cbTraversalStart;
    private JComboBox<String> cbCongFrom, cbCongTo;
    private JSlider sliderCongestion;

    // ─── Output ──────────────────────────────────────────────────────────────
    private JTextArea taOutput;
    private JLabel lblStatus;
    private DefaultTableModel edgeTableModel;

    // ─────────────────────────────────────────────────────────────────────────
    public TrafficManagementApp() {
        super("🚦 Traffic Management System — DSA Project");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        buildUI();
        loadSampleData();
        pack();
        setMinimumSize(new Dimension(1200, 720));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ─── UI Construction ─────────────────────────────────────────────────────

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildCanvasPanel());
        center.setDividerLocation(370);
        center.setDividerSize(4);
        center.setBorder(null);
        center.setBackground(BG_DARK);
        add(center, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(8, 47, 73));
        hdr.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("🚦 Traffic Management System");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(ACCENT);

        JLabel sub = new JLabel("Graph-based Route Optimization using Dijkstra's, BFS & DFS  |  CS106P Final Project");
        sub.setFont(FONT_SMALL);
        sub.setForeground(TEXT_DIM);

        JPanel info = new JPanel(new GridLayout(2, 1, 2, 2));
        info.setOpaque(false);
        info.add(title);
        info.add(sub);

        JButton btnReset = makeButton("Reset Layout", ACCENT2, Color.WHITE);
        btnReset.addActionListener(e -> { canvas.resetLayout(); setStatus("Canvas layout reset.", ACCENT); });

        JButton btnClear = makeButton("Clear Highlights", BG_INPUT, TEXT_DIM);
        btnClear.addActionListener(e -> { canvas.clearHighlights(); setStatus("Highlights cleared.", TEXT_DIM); });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnClear);
        right.add(btnReset);

        hdr.add(info, BorderLayout.WEST);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    private JScrollPane buildLeftPanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(BG_DARK);
        left.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 6));

        left.add(buildNodeSection());
        left.add(vgap(8));
        left.add(buildEdgeSection());
        left.add(vgap(8));
        left.add(buildAlgorithmSection());
        left.add(vgap(8));
        left.add(buildCongestionSection());
        left.add(vgap(8));
        left.add(buildOutputSection());

        JScrollPane sp = new JScrollPane(left);
        sp.setBorder(null);
        sp.setBackground(BG_DARK);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    private JPanel buildNodeSection() {
        JPanel p = section("🔵  Intersections (Nodes)");

        tfNodeName = styledField("Intersection name (e.g. A)");

        JButton btnAdd = makeButton("+ Add", SUCCESS, BG_DARK);
        JButton btnRemove = makeButton("✕ Remove", DANGER, Color.WHITE);

        btnAdd.addActionListener(e -> addNode());
        btnRemove.addActionListener(e -> removeNode());
        tfNodeName.addActionListener(e -> addNode());

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.add(tfNodeName, BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(1, 2, 5, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);
        btns.add(btnRemove);
        row.add(btns, BorderLayout.EAST);

        p.add(row);
        return p;
    }

    private JPanel buildEdgeSection() {
        JPanel p = section("🔗  Roads (Edges)");

        cbFrom = styledCombo();
        cbTo   = styledCombo();
        tfWeight     = styledField("Travel time (min)");
        tfCongestion = styledField("Congestion 0–100");

        JButton btnAdd    = makeButton("+ Add Road", SUCCESS, BG_DARK);
        JButton btnRemove = makeButton("✕ Remove Road", DANGER, Color.WHITE);
        btnAdd.addActionListener(e -> addEdge());
        btnRemove.addActionListener(e -> removeEdge());

        p.add(labeledRow("From:", cbFrom));
        p.add(vgap(4));
        p.add(labeledRow("To:", cbTo));
        p.add(vgap(4));
        p.add(labeledRow("Time(min):", tfWeight));
        p.add(vgap(4));
        p.add(labeledRow("Congestion:", tfCongestion));
        p.add(vgap(6));

        JPanel btns = new JPanel(new GridLayout(1, 2, 5, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);
        btns.add(btnRemove);
        p.add(fullRow(btns));

        // Edge table
        p.add(vgap(6));
        edgeTableModel = new DefaultTableModel(new String[]{"From", "To", "Time", "Cong%"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = new JTable(edgeTableModel);
        styleTable(tbl);
        JScrollPane tsp = new JScrollPane(tbl);
        tsp.setPreferredSize(new Dimension(330, 110));
        tsp.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        tsp.getViewport().setBackground(BG_INPUT);
        p.add(tsp);

        return p;
    }

    private JPanel buildAlgorithmSection() {
        JPanel p = section("⚡  Algorithms");

        // Dijkstra
        JPanel dijkPanel = subSection("Dijkstra's Shortest Path  [O((V+E) log V)]");
        cbStart = styledCombo();
        cbEnd   = styledCombo();
        JButton btnDijk = makeButton("▶ Find Shortest Path", ACCENT, BG_DARK);
        btnDijk.addActionListener(e -> runDijkstra());
        dijkPanel.add(labeledRow("Start:", cbStart));
        dijkPanel.add(vgap(4));
        dijkPanel.add(labeledRow("End:", cbEnd));
        dijkPanel.add(vgap(6));
        dijkPanel.add(fullRow(btnDijk));
        p.add(dijkPanel);
        p.add(vgap(6));

        // BFS / DFS
        JPanel travPanel = subSection("BFS / DFS Traversal  [O(V + E)]");
        cbTraversalStart = styledCombo();
        JButton btnBFS = makeButton("▶ BFS", WARNING, BG_DARK);
        JButton btnDFS = makeButton("▶ DFS", new Color(249, 115, 22), Color.WHITE);
        btnBFS.addActionListener(e -> runBFS());
        btnDFS.addActionListener(e -> runDFS());
        travPanel.add(labeledRow("Start:", cbTraversalStart));
        travPanel.add(vgap(6));

        JPanel travBtns = new JPanel(new GridLayout(1, 2, 5, 0));
        travBtns.setOpaque(false);
        travBtns.add(btnBFS);
        travBtns.add(btnDFS);
        travPanel.add(fullRow(travBtns));
        p.add(travPanel);

        return p;
    }

    private JPanel buildCongestionSection() {
        JPanel p = section("🚗  Traffic Congestion Simulation");

        cbCongFrom = styledCombo();
        cbCongTo   = styledCombo();
        sliderCongestion = new JSlider(0, 100, 30);
        sliderCongestion.setBackground(BG_PANEL);
        sliderCongestion.setForeground(ACCENT);
        sliderCongestion.setMajorTickSpacing(25);
        sliderCongestion.setMinorTickSpacing(5);
        sliderCongestion.setPaintTicks(true);
        sliderCongestion.setPaintLabels(true);
        sliderCongestion.setFont(FONT_SMALL);

        JLabel lblCongVal = new JLabel("30%");
        lblCongVal.setFont(FONT_TITLE);
        lblCongVal.setForeground(WARNING);

        sliderCongestion.addChangeListener(e -> {
            int v = sliderCongestion.getValue();
            lblCongVal.setForeground(v < 30 ? SUCCESS : v < 70 ? WARNING : DANGER);
            lblCongVal.setText(v + "%");
        });

        JButton btnSimulate = makeButton("▶ Apply Congestion", WARNING, BG_DARK);
        btnSimulate.addActionListener(e -> applyCongestion());

        p.add(labeledRow("Road From:", cbCongFrom));
        p.add(vgap(4));
        p.add(labeledRow("Road To:", cbCongTo));
        p.add(vgap(6));

        JPanel sliderRow = new JPanel(new BorderLayout(8, 0));
        sliderRow.setOpaque(false);
        sliderRow.add(sliderCongestion, BorderLayout.CENTER);
        sliderRow.add(lblCongVal, BorderLayout.EAST);
        p.add(sliderRow);
        p.add(vgap(6));
        p.add(fullRow(btnSimulate));

        return p;
    }

    private JPanel buildOutputSection() {
        JPanel p = section("📋  Output / Results");
        taOutput = new JTextArea(8, 30);
        taOutput.setFont(FONT_MONO);
        taOutput.setBackground(BG_INPUT);
        taOutput.setForeground(new Color(134, 239, 172));
        taOutput.setCaretColor(Color.WHITE);
        taOutput.setEditable(false);
        taOutput.setLineWrap(true);
        taOutput.setWrapStyleWord(true);
        taOutput.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane sp = new JScrollPane(taOutput);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        sp.setPreferredSize(new Dimension(330, 160));

        JButton btnClear = makeButton("Clear Log", BG_INPUT, TEXT_DIM);
        btnClear.addActionListener(e -> taOutput.setText(""));

        p.add(sp);
        p.add(vgap(4));
        p.add(fullRow(btnClear));
        return p;
    }

    private JPanel buildCanvasPanel() {
        canvas = new GraphCanvas(graph);
        canvas.setBackground(new Color(15, 23, 42));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_DARK);
        wrap.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 10));

        JLabel lbl = new JLabel("  Road Network Map  —  drag nodes to rearrange");
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_DIM);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(canvas, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(8, 47, 73));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        lblStatus = new JLabel("Ready. Add intersections and roads to get started.");
        lblStatus.setFont(FONT_SMALL);
        lblStatus.setForeground(TEXT_DIM);
        bar.add(lblStatus, BorderLayout.WEST);

        JLabel stats = new JLabel("Nodes: 0  |  Edges: 0");
        stats.setFont(FONT_SMALL);
        stats.setForeground(TEXT_DIM);
        bar.add(stats, BorderLayout.EAST);

        // Update stats dynamically
        new javax.swing.Timer(500, e -> stats.setText(
                "Nodes: " + graph.nodeCount() + "  |  Edges: " + graph.edgeCount()
        )).start();

        return bar;
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    private void addNode() {
        String name = tfNodeName.getText().trim().toUpperCase();
        if (name.isEmpty()) { setStatus("Please enter an intersection name.", DANGER); return; }
        if (!name.matches("[A-Z0-9 _-]+")) { setStatus("Use letters/numbers only.", DANGER); return; }
        if (graph.addNode(name)) {
            canvas.addNode(name);
            updateAllCombos();
            setStatus("✔ Added intersection: " + name, SUCCESS);
            log("Added node: " + name);
        } else {
            setStatus("Intersection '" + name + "' already exists.", WARNING);
        }
        tfNodeName.setText("");
        tfNodeName.requestFocus();
    }

    private void removeNode() {
        String name = tfNodeName.getText().trim().toUpperCase();
        if (name.isEmpty()) { setStatus("Enter the intersection name to remove.", DANGER); return; }
        if (graph.removeNode(name)) {
            canvas.removeNode(name);
            updateAllCombos();
            refreshEdgeTable();
            setStatus("✔ Removed intersection: " + name, SUCCESS);
            log("Removed node: " + name);
        } else {
            setStatus("Intersection '" + name + "' not found.", DANGER);
        }
        tfNodeName.setText("");
    }

    private void addEdge() {
        String from = (String) cbFrom.getSelectedItem();
        String to   = (String) cbTo.getSelectedItem();
        if (from == null || to == null || from.equals(to)) {
            setStatus("Select two different intersections.", DANGER); return;
        }
        int weight, congestion;
        try {
            weight = Integer.parseInt(tfWeight.getText().trim());
            String ct = tfCongestion.getText().trim();
            congestion = ct.isEmpty() ? 0 : Integer.parseInt(ct);
            if (weight <= 0) throw new NumberFormatException();
            congestion = Math.max(0, Math.min(100, congestion));
        } catch (NumberFormatException ex) {
            setStatus("Enter a valid travel time (positive integer).", DANGER); return;
        }
        if (graph.addEdge(from, to, weight, congestion)) {
            canvas.refresh();
            refreshEdgeTable();
            setStatus("✔ Road added: " + from + " ↔ " + to + " (" + weight + "min, " + congestion + "% congestion)", SUCCESS);
            log("Added edge: " + from + " ↔ " + to + " | time=" + weight + "min | congestion=" + congestion + "%");
        } else {
            setStatus("Failed to add road — check node names.", DANGER);
        }
        tfWeight.setText("");
        tfCongestion.setText("");
    }

    private void removeEdge() {
        String from = (String) cbFrom.getSelectedItem();
        String to   = (String) cbTo.getSelectedItem();
        if (from == null || to == null) { setStatus("Select nodes to remove road.", DANGER); return; }
        if (graph.removeEdge(from, to)) {
            canvas.refresh();
            refreshEdgeTable();
            setStatus("✔ Road removed: " + from + " ↔ " + to, SUCCESS);
            log("Removed edge: " + from + " ↔ " + to);
        } else {
            setStatus("Road between " + from + " and " + to + " not found.", DANGER);
        }
    }

    private void runDijkstra() {
        String start = (String) cbStart.getSelectedItem();
        String end   = (String) cbEnd.getSelectedItem();
        if (start == null || end == null) { setStatus("Select start and end nodes.", DANGER); return; }
        if (start.equals(end)) { setStatus("Start and end must be different.", DANGER); return; }

        long t0 = System.nanoTime();
        TrafficGraph.PathResult result = graph.dijkstra(start, end);
        long elapsed = System.nanoTime() - t0;

        if (result == null) {
            setStatus("No path found between " + start + " and " + end + ".", DANGER);
            log("Dijkstra: No path found from " + start + " to " + end);
            canvas.clearHighlights();
            return;
        }

        canvas.highlightPath(result.path, start, end);
        setStatus("✔ Shortest path found: " + String.join(" → ", result.path) +
                "  |  Cost: " + result.totalCost + " min", SUCCESS);

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("DIJKSTRA'S ALGORITHM RESULT\n");
        sb.append("═══════════════════════════════\n");
        sb.append("Route:   ").append(String.join(" → ", result.path)).append("\n");
        sb.append("Cost:    ").append(result.totalCost).append(" minutes (incl. congestion)\n");
        sb.append("Hops:    ").append(result.path.size() - 1).append(" roads\n");
        sb.append("Time:    ").append(String.format("%.3f", elapsed / 1_000_000.0)).append(" ms\n");
        sb.append("Complexity: O((V+E) log V)\n");
        log(sb.toString());
    }

    private void runBFS() {
        String start = (String) cbTraversalStart.getSelectedItem();
        if (start == null) { setStatus("Select a start node for BFS.", DANGER); return; }

        long t0 = System.nanoTime();
        List<String> order = graph.bfs(start);
        long elapsed = System.nanoTime() - t0;

        canvas.highlightTraversal(order, "bfs", start);
        setStatus("✔ BFS traversal from " + start + ": " + String.join(" → ", order), ACCENT);

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("BFS TRAVERSAL RESULT\n");
        sb.append("═══════════════════════════════\n");
        sb.append("Start:   ").append(start).append("\n");
        sb.append("Order:   ").append(String.join(" → ", order)).append("\n");
        sb.append("Visited: ").append(order.size()).append(" nodes\n");
        sb.append("Time:    ").append(String.format("%.3f", elapsed / 1_000_000.0)).append(" ms\n");
        sb.append("Complexity: O(V + E)\n");
        sb.append("Use: Finds shortest hop-count path\n");
        log(sb.toString());
    }

    private void runDFS() {
        String start = (String) cbTraversalStart.getSelectedItem();
        if (start == null) { setStatus("Select a start node for DFS.", DANGER); return; }

        long t0 = System.nanoTime();
        List<String> order = graph.dfs(start);
        long elapsed = System.nanoTime() - t0;

        canvas.highlightTraversal(order, "dfs", start);
        setStatus("✔ DFS traversal from " + start + ": " + String.join(" → ", order), new Color(249, 115, 22));

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("DFS TRAVERSAL RESULT\n");
        sb.append("═══════════════════════════════\n");
        sb.append("Start:   ").append(start).append("\n");
        sb.append("Order:   ").append(String.join(" → ", order)).append("\n");
        sb.append("Visited: ").append(order.size()).append(" nodes\n");
        sb.append("Time:    ").append(String.format("%.3f", elapsed / 1_000_000.0)).append(" ms\n");
        sb.append("Complexity: O(V + E)\n");
        sb.append("Use: Detects dead-ends, cycles\n");
        log(sb.toString());
    }

    private void applyCongestion() {
        String from = (String) cbCongFrom.getSelectedItem();
        String to   = (String) cbCongTo.getSelectedItem();
        if (from == null || to == null || from.equals(to)) {
            setStatus("Select two different intersections.", DANGER); return;
        }
        int level = sliderCongestion.getValue();
        if (graph.updateCongestion(from, to, level)) {
            canvas.refresh();
            refreshEdgeTable();
            String severity = level < 30 ? "🟢 LOW" : level < 70 ? "🟡 MODERATE" : "🔴 HIGH";
            setStatus("✔ Congestion on " + from + " ↔ " + to + " set to " + level + "% — " + severity, WARNING);
            log("Congestion updated: " + from + " ↔ " + to + " → " + level + "% (" + severity + ")");
            log("Effective travel time is now multiplied by " + String.format("%.2f", 1 + level / 100.0) + "x");
        } else {
            setStatus("Road between " + from + " and " + to + " not found.", DANGER);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void loadSampleData() {
        // Sample Davao City-inspired road network
        String[] nodes = {"Bankerohan", "Agdao", "Buhangin", "Panacan", "Toril",
                "Calinan", "Baguio Dist.", "Talomo", "Tibungco"};
        for (String n : nodes) graph.addNode(n);
        canvas.resetLayout();

        int[][] edges = {
                // from, to, weight, congestion (encoded as indices)
        };
        graph.addEdge("Bankerohan", "Agdao",      8,  45);
        graph.addEdge("Bankerohan", "Talomo",     12,  20);
        graph.addEdge("Bankerohan", "Toril",      18,  60);
        graph.addEdge("Agdao",      "Buhangin",    6,  30);
        graph.addEdge("Agdao",      "Panacan",    10,  50);
        graph.addEdge("Buhangin",   "Panacan",     5,  25);
        graph.addEdge("Buhangin",   "Tibungco",   15,  10);
        graph.addEdge("Panacan",    "Tibungco",    7,  40);
        graph.addEdge("Talomo",     "Toril",       9,  35);
        graph.addEdge("Talomo",     "Calinan",    14,  15);
        graph.addEdge("Toril",      "Calinan",    11,  20);
        graph.addEdge("Calinan",    "Baguio Dist.", 8, 10);
        graph.addEdge("Baguio Dist.", "Tibungco",  20, 5);

        updateAllCombos();
        canvas.refresh();
        log("═══════════════════════════════\n" +
                "Traffic Management System Ready\n" +
                "═══════════════════════════════\n" +
                "Sample Davao City road network loaded.\n" +
                "Nodes: " + graph.nodeCount() + "  |  Edges: " + graph.edgeCount() + "\n" +
                "\nData Structures in use:\n" +
                "  • HashMap (Adjacency List) — O(1) node lookup\n" +
                "  • Priority Queue (Min-Heap) — Dijkstra's\n" +
                "  • LinkedList — Edge storage, BFS Queue\n" +
                "  • Stack — DFS Traversal\n" +
                "\nTry: Dijkstra from Bankerohan to Tibungco!\n");
        refreshEdgeTable();
        setStatus("Sample Davao City road network loaded — " + graph.nodeCount() +
                " intersections, " + graph.edgeCount() + " roads.", ACCENT);
    }

    private void updateAllCombos() {
        JComboBox<?>[] combos = {cbFrom, cbTo, cbStart, cbEnd,
                cbTraversalStart, cbCongFrom, cbCongTo};
        List<String> nodes = new ArrayList<>(graph.getNodes());
        Collections.sort(nodes);
        for (JComboBox<?> cb : combos) {
            if (cb == null) continue;
            @SuppressWarnings("unchecked")
            JComboBox<String> sc = (JComboBox<String>) cb;
            String prev = (String) sc.getSelectedItem();
            sc.removeAllItems();
            for (String n : nodes) sc.addItem(n);
            if (prev != null && nodes.contains(prev)) sc.setSelectedItem(prev);
        }
    }

    private void refreshEdgeTable() {
        edgeTableModel.setRowCount(0);
        Set<String> seen = new HashSet<>();
        for (String from : graph.getNodes()) {
            for (TrafficGraph.Edge e : graph.getEdges(from)) {
                String key = from.compareTo(e.to) < 0 ? from + e.to : e.to + from;
                if (!seen.contains(key)) {
                    seen.add(key);
                    edgeTableModel.addRow(new Object[]{from, e.to, e.weight + "m", e.congestion + "%"});
                }
            }
        }
    }

    private void log(String msg) {
        taOutput.append(msg + "\n");
        taOutput.setCaretPosition(taOutput.getDocument().getLength());
    }

    private void setStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
    }

    // ─── UI Factories ────────────────────────────────────────────────────────

    private JPanel section(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(ACCENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        return p;
    }

    private JPanel subSection(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(22, 33, 52));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 75, 100)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(TEXT_DIM);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        return p;
    }

    private JTextField styledField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(FONT_NORMAL);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_MAIN);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        tf.putClientProperty("placeholder", placeholder);
        return tf;
    }

    private JComboBox<String> styledCombo() {
        JComboBox<String> cb = new JComboBox<>();
        cb.setFont(FONT_NORMAL);
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_MAIN);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        cb.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> l, Object v,
                                                          int i, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, i, sel, foc);
                setBackground(sel ? ACCENT2 : BG_INPUT);
                setForeground(TEXT_MAIN);
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return this;
            }
        });
        return cb;
    }

    private JButton makeButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_NORMAL);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private void styleTable(JTable tbl) {
        tbl.setFont(FONT_SMALL);
        tbl.setBackground(BG_INPUT);
        tbl.setForeground(TEXT_MAIN);
        tbl.setGridColor(BORDER_COL);
        tbl.setRowHeight(22);
        tbl.getTableHeader().setBackground(BG_PANEL);
        tbl.getTableHeader().setForeground(ACCENT);
        tbl.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tbl.setSelectionBackground(ACCENT2);
        tbl.setSelectionForeground(Color.WHITE);
        tbl.setShowVerticalLines(false);
    }

    private JPanel labeledRow(String label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_DIM);
        lbl.setPreferredSize(new Dimension(80, 28));
        row.add(lbl, BorderLayout.WEST);
        row.add(comp, BorderLayout.CENTER);
        return row;
    }

    private JPanel fullRow(JComponent comp) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.add(comp, BorderLayout.CENTER);
        return row;
    }

    private Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    // ─── Entry Point ─────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(TrafficManagementApp::new);
    }
}