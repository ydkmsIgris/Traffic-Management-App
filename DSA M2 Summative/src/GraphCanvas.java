import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * GraphCanvas - Custom JPanel that visually renders the road network.
 * Supports drag-to-move nodes, visual highlighting of paths/traversals.
 */
public class GraphCanvas extends JPanel {

    // ─── Constants ───────────────────────────────────────────────────────────
    private static final int NODE_RADIUS = 22;
    private static final Color BG_COLOR        = new Color(15, 23, 42);
    private static final Color GRID_COLOR       = new Color(30, 41, 59);
    private static final Color NODE_COLOR       = new Color(56, 189, 248);
    private static final Color NODE_BORDER      = new Color(14, 165, 233);
    private static final Color NODE_TEXT        = Color.WHITE;
    private static final Color EDGE_COLOR       = new Color(100, 116, 139);
    private static final Color HIGHLIGHT_PATH   = new Color(34, 197, 94);
    private static final Color HIGHLIGHT_BFS    = new Color(251, 191, 36);
    private static final Color HIGHLIGHT_DFS    = new Color(249, 115, 22);
    private static final Color CONGESTION_LOW   = new Color(34, 197, 94);
    private static final Color CONGESTION_MED   = new Color(234, 179, 8);
    private static final Color CONGESTION_HIGH  = new Color(239, 68, 68);
    private static final Color START_NODE_COLOR = new Color(34, 197, 94);
    private static final Color END_NODE_COLOR   = new Color(239, 68, 68);

    // ─── State ───────────────────────────────────────────────────────────────
    private final TrafficGraph graph;
    private final Map<String, Point> positions = new LinkedHashMap<>();

    private List<String> highlightedPath = new ArrayList<>();
    private List<String> traversalOrder  = new ArrayList<>();
    private String highlightMode = "none"; // "path", "bfs", "dfs"
    private String startNode = null;
    private String endNode   = null;

    private String dragging = null;
    private Point dragOffset = new Point();

    // ─── Constructor ─────────────────────────────────────────────────────────
    public GraphCanvas(TrafficGraph graph) {
        this.graph = graph;
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(700, 500));

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onPress(e); }
            @Override public void mouseDragged(MouseEvent e) { onDrag(e); }
            @Override public void mouseReleased(MouseEvent e) { dragging = null; }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public void addNode(String name) {
        if (!positions.containsKey(name)) {
            // Place new nodes in a circle layout, with some randomness
            int n = positions.size();
            double angle = (2 * Math.PI * n) / Math.max(1, graph.nodeCount());
            int cx = getWidth() / 2 == 0 ? 350 : getWidth() / 2;
            int cy = getHeight() / 2 == 0 ? 250 : getHeight() / 2;
            int r = Math.min(cx, cy) - 80;
            int x = (int) (cx + r * Math.cos(angle));
            int y = (int) (cy + r * Math.sin(angle));
            positions.put(name, new Point(x, y));
        }
        repaint();
    }

    public void removeNode(String name) {
        positions.remove(name);
        if (name.equals(startNode)) startNode = null;
        if (name.equals(endNode))   endNode   = null;
        highlightedPath.clear();
        traversalOrder.clear();
        repaint();
    }

    public void clearHighlights() {
        highlightedPath.clear();
        traversalOrder.clear();
        highlightMode = "none";
        startNode = null;
        endNode   = null;
        repaint();
    }

    public void highlightPath(List<String> path, String start, String end) {
        highlightedPath = new ArrayList<>(path);
        traversalOrder.clear();
        highlightMode = "path";
        startNode = start;
        endNode   = end;
        repaint();
    }

    public void highlightTraversal(List<String> order, String mode, String start) {
        traversalOrder = new ArrayList<>(order);
        highlightedPath.clear();
        highlightMode = mode;
        startNode = start;
        endNode   = null;
        repaint();
    }

    public void refresh() {
        // Re-layout if nodes exist but have no position yet
        for (String node : graph.getNodes()) addNode(node);
        repaint();
    }

    public void resetLayout() {
        positions.clear();
        int n = graph.nodeCount();
        if (n == 0) { repaint(); return; }
        int cx = getWidth() / 2 == 0 ? 350 : getWidth() / 2;
        int cy = getHeight() / 2 == 0 ? 250 : getHeight() / 2;
        int r = Math.min(cx, cy) - 80;
        int i = 0;
        for (String node : graph.getNodes()) {
            double angle = (2 * Math.PI * i++) / n - Math.PI / 2;
            positions.put(node, new Point(
                    (int) (cx + r * Math.cos(angle)),
                    (int) (cy + r * Math.sin(angle))
            ));
        }
        repaint();
    }

    // ─── Painting ────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        drawEdges(g2);
        drawNodes(g2);
        drawLegend(g2);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(0.5f));
        for (int x = 0; x < getWidth(); x += 40)
            g2.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 40)
            g2.drawLine(0, y, getWidth(), y);
    }

    private void drawEdges(Graphics2D g2) {
        Set<String> drawn = new HashSet<>();

        for (String from : graph.getNodes()) {
            Point pFrom = positions.get(from);
            if (pFrom == null) continue;

            for (TrafficGraph.Edge edge : graph.getEdges(from)) {
                String key = from.compareTo(edge.to) < 0
                        ? from + "-" + edge.to : edge.to + "-" + from;
                if (drawn.contains(key)) continue;
                drawn.add(key);

                Point pTo = positions.get(edge.to);
                if (pTo == null) continue;

                boolean isPathEdge = isPathEdge(from, edge.to);
                boolean isTraversalEdge = isTraversalEdge(from, edge.to);

                Color edgeColor;
                float strokeWidth;

                if (isPathEdge) {
                    edgeColor = HIGHLIGHT_PATH;
                    strokeWidth = 3.5f;
                } else if (isTraversalEdge) {
                    edgeColor = highlightMode.equals("bfs") ? HIGHLIGHT_BFS : HIGHLIGHT_DFS;
                    strokeWidth = 2.5f;
                } else {
                    edgeColor = congestionColor(edge.congestion);
                    strokeWidth = 1.8f;
                }

                g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(edgeColor);
                g2.drawLine(pFrom.x, pFrom.y, pTo.x, pTo.y);

                // Draw edge weight label
                int mx = (pFrom.x + pTo.x) / 2;
                int my = (pFrom.y + pTo.y) / 2;
                String label = edge.weight + "m";
                if (edge.congestion > 0) label += " [" + edge.congestion + "%]";
                drawEdgeLabel(g2, label, mx, my, edgeColor);
            }
        }
    }

    private void drawEdgeLabel(Graphics2D g2, String text, int x, int y, Color accent) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 6;
        int h = fm.getHeight();
        g2.setColor(new Color(15, 23, 42, 210));
        g2.fillRoundRect(x - w / 2, y - h / 2, w, h, 4, 4);
        g2.setColor(accent.brighter());
        g2.drawString(text, x - fm.stringWidth(text) / 2, y + fm.getAscent() / 2 - 1);
    }

    private void drawNodes(Graphics2D g2) {
        for (String name : graph.getNodes()) {
            Point p = positions.get(name);
            if (p == null) continue;

            boolean isStart = name.equals(startNode);
            boolean isEnd   = name.equals(endNode);
            boolean inPath  = highlightedPath.contains(name);
            boolean inTraversal = traversalOrder.contains(name);

            Color fill, border;
            if (isStart)           { fill = START_NODE_COLOR; border = fill.brighter(); }
            else if (isEnd)        { fill = END_NODE_COLOR;   border = fill.brighter(); }
            else if (inPath)       { fill = HIGHLIGHT_PATH;   border = fill.brighter(); }
            else if (inTraversal)  {
                fill = highlightMode.equals("bfs") ? HIGHLIGHT_BFS : HIGHLIGHT_DFS;
                border = fill.brighter();
            }
            else                   { fill = NODE_COLOR;       border = NODE_BORDER; }

            // Glow effect
            if (inPath || inTraversal || isStart || isEnd) {
                g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 50));
                g2.fillOval(p.x - NODE_RADIUS - 6, p.y - NODE_RADIUS - 6,
                        (NODE_RADIUS + 6) * 2, (NODE_RADIUS + 6) * 2);
            }

            // Node circle
            g2.setColor(new Color(15, 23, 42));
            g2.fillOval(p.x - NODE_RADIUS, p.y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            g2.setColor(fill);
            g2.fillOval(p.x - NODE_RADIUS + 2, p.y - NODE_RADIUS + 2,
                    (NODE_RADIUS - 2) * 2, (NODE_RADIUS - 2) * 2);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(p.x - NODE_RADIUS, p.y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

            // Node label
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(NODE_TEXT);
            g2.drawString(name, p.x - fm.stringWidth(name) / 2,
                    p.y + fm.getAscent() / 2 - 1);

            // Traversal order badge
            if (inTraversal && !traversalOrder.isEmpty()) {
                int idx = traversalOrder.indexOf(name) + 1;
                String badge = String.valueOf(idx);
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                g2.setColor(new Color(15, 23, 42));
                int bx = p.x + NODE_RADIUS - 8;
                int by = p.y - NODE_RADIUS + 8;
                g2.fillOval(bx - 7, by - 7, 14, 14);
                g2.setColor(fill.brighter());
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(bx - 7, by - 7, 14, 14);
                g2.setColor(Color.WHITE);
                g2.drawString(badge, bx - g2.getFontMetrics().stringWidth(badge) / 2, by + 3);
            }
        }
    }

    private void drawLegend(Graphics2D g2) {
        if (highlightMode.equals("none") && highlightedPath.isEmpty()) return;

        int x = 12, y = getHeight() - 80;
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(30, 41, 59, 200));
        g2.fillRoundRect(x - 4, y - 14, 190, 70, 8, 8);

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        if (highlightMode.equals("path")) {
            drawLegendItem(g2, x, y,      HIGHLIGHT_PATH, "■ Shortest Path");
            drawLegendItem(g2, x, y + 18, START_NODE_COLOR, "● Start Node");
            drawLegendItem(g2, x, y + 36, END_NODE_COLOR, "● End Node");
        } else if (highlightMode.equals("bfs")) {
            drawLegendItem(g2, x, y,      HIGHLIGHT_BFS, "■ BFS Traversal");
            drawLegendItem(g2, x, y + 18, START_NODE_COLOR, "● Start Node");
            drawLegendItem(g2, x, y + 36, new Color(200,200,200), "# = Visit Order");
        } else if (highlightMode.equals("dfs")) {
            drawLegendItem(g2, x, y,      HIGHLIGHT_DFS, "■ DFS Traversal");
            drawLegendItem(g2, x, y + 18, START_NODE_COLOR, "● Start Node");
            drawLegendItem(g2, x, y + 36, new Color(200,200,200), "# = Visit Order");
        }
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color c, String text) {
        g2.setColor(c);
        g2.drawString(text, x, y);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean isPathEdge(String a, String b) {
        for (int i = 0; i < highlightedPath.size() - 1; i++) {
            String n1 = highlightedPath.get(i), n2 = highlightedPath.get(i + 1);
            if ((n1.equals(a) && n2.equals(b)) || (n1.equals(b) && n2.equals(a))) return true;
        }
        return false;
    }

    private boolean isTraversalEdge(String a, String b) {
        for (int i = 0; i < traversalOrder.size() - 1; i++) {
            String n1 = traversalOrder.get(i), n2 = traversalOrder.get(i + 1);
            // Only highlight if they are actually connected
            if ((n1.equals(a) && n2.equals(b)) || (n1.equals(b) && n2.equals(a))) {
                // Check if edge exists
                for (TrafficGraph.Edge e : graph.getEdges(a)) {
                    if (e.to.equals(b)) return true;
                }
            }
        }
        return false;
    }

    private Color congestionColor(int congestion) {
        if (congestion < 30)  return CONGESTION_LOW;
        if (congestion < 70)  return CONGESTION_MED;
        return CONGESTION_HIGH;
    }

    // ─── Mouse Interaction ───────────────────────────────────────────────────

    private void onPress(MouseEvent e) {
        for (Map.Entry<String, Point> entry : positions.entrySet()) {
            Point p = entry.getValue();
            if (p.distance(e.getPoint()) <= NODE_RADIUS) {
                dragging = entry.getKey();
                dragOffset.setLocation(e.getX() - p.x, e.getY() - p.y);
                return;
            }
        }
    }

    private void onDrag(MouseEvent e) {
        if (dragging != null) {
            positions.get(dragging).setLocation(
                    e.getX() - dragOffset.x, e.getY() - dragOffset.y);
            repaint();
        }
    }
}