import java.util.*;

/**
 * TrafficGraph - Core data structure for the Traffic Management System
 * Uses: Adjacency List (HashMap + LinkedList), Priority Queue (Min-Heap)
 * Algorithms: Dijkstra's, BFS, DFS
 */
public class TrafficGraph {

    // ─── Inner Classes ───────────────────────────────────────────────────────

    public static class Edge {
        String to;
        int weight;        // travel time in minutes
        int congestion;    // 0–100 congestion level

        public Edge(String to, int weight, int congestion) {
            this.to = to;
            this.weight = weight;
            this.congestion = congestion;
        }

        public int effectiveWeight() {
            // Optimization: factor in congestion to real travel cost
            return (int) (weight * (1 + congestion / 100.0));
        }
    }

    public static class PathResult {
        public List<String> path;
        public int totalCost;
        public String algorithm;

        public PathResult(List<String> path, int totalCost, String algorithm) {
            this.path = path;
            this.totalCost = totalCost;
            this.algorithm = algorithm;
        }
    }

    // ─── Fields ──────────────────────────────────────────────────────────────

    // HashMap for O(1) node lookup — Data Structure #1
    private final Map<String, List<Edge>> adjList = new LinkedHashMap<>();

    // ─── Node Management ─────────────────────────────────────────────────────

    public boolean addNode(String name) {
        if (adjList.containsKey(name)) return false;
        adjList.put(name, new LinkedList<>());  // LinkedList — Data Structure #3
        return true;
    }

    public boolean removeNode(String name) {
        if (!adjList.containsKey(name)) return false;
        adjList.remove(name);
        // Remove all edges pointing to this node
        for (List<Edge> edges : adjList.values()) {
            edges.removeIf(e -> e.to.equals(name));
        }
        return true;
    }

    public Set<String> getNodes() {
        return adjList.keySet();
    }

    public boolean hasNode(String name) {
        return adjList.containsKey(name);
    }

    // ─── Edge Management ─────────────────────────────────────────────────────

    public boolean addEdge(String from, String to, int weight, int congestion) {
        if (!adjList.containsKey(from) || !adjList.containsKey(to)) return false;
        if (from.equals(to)) return false;
        // Remove existing edge if any (update)
        removeEdgeDirected(from, to);
        removeEdgeDirected(to, from);
        adjList.get(from).add(new Edge(to, weight, congestion));
        adjList.get(to).add(new Edge(from, weight, congestion));
        return true;
    }

    public boolean removeEdge(String from, String to) {
        if (!adjList.containsKey(from) || !adjList.containsKey(to)) return false;
        boolean a = removeEdgeDirected(from, to);
        boolean b = removeEdgeDirected(to, from);
        return a || b;
    }

    private boolean removeEdgeDirected(String from, String to) {
        List<Edge> edges = adjList.get(from);
        if (edges == null) return false;
        return edges.removeIf(e -> e.to.equals(to));
    }

    public boolean updateCongestion(String from, String to, int congestion) {
        if (!adjList.containsKey(from)) return false;
        for (Edge e : adjList.get(from)) {
            if (e.to.equals(to)) { e.congestion = congestion; }
        }
        if (!adjList.containsKey(to)) return false;
        for (Edge e : adjList.get(to)) {
            if (e.to.equals(from)) { e.congestion = congestion; }
        }
        return true;
    }

    public List<Edge> getEdges(String node) {
        return adjList.getOrDefault(node, Collections.emptyList());
    }

    public Map<String, List<Edge>> getAdjList() {
        return adjList;
    }

    // ─── Algorithm 1: Dijkstra's Shortest Path ───────────────────────────────
    // Time Complexity:  O((V + E) log V)
    // Space Complexity: O(V)

    public PathResult dijkstra(String start, String end) {
        if (!adjList.containsKey(start) || !adjList.containsKey(end))
            return null;

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();

        for (String node : adjList.keySet()) dist.put(node, Integer.MAX_VALUE);
        dist.put(start, 0);

        // Priority Queue (Min-Heap) — Data Structure #2
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.offer(start);

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {
            String curr = pq.poll();
            if (visited.contains(curr)) continue;
            visited.add(curr);

            if (curr.equals(end)) break; // Early termination optimization

            for (Edge edge : adjList.get(curr)) {
                if (visited.contains(edge.to)) continue;
                int newDist = dist.get(curr) + edge.effectiveWeight();
                if (newDist < dist.get(edge.to)) {
                    dist.put(edge.to, newDist);
                    prev.put(edge.to, curr);
                    pq.offer(edge.to);
                }
            }
        }

        if (dist.get(end) == Integer.MAX_VALUE) return null;

        List<String> path = new ArrayList<>();
        for (String at = end; at != null; at = prev.get(at)) path.add(at);
        Collections.reverse(path);

        return new PathResult(path, dist.get(end), "Dijkstra's");
    }

    // ─── Algorithm 2: BFS Traversal ──────────────────────────────────────────
    // Time Complexity:  O(V + E)
    // Space Complexity: O(V)

    public List<String> bfs(String start) {
        if (!adjList.containsKey(start)) return Collections.emptyList();

        List<String> order = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();  // Queue for BFS

        queue.offer(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String curr = queue.poll();
            order.add(curr);

            List<Edge> neighbors = new ArrayList<>(adjList.get(curr));
            neighbors.sort(Comparator.comparing(e -> e.to)); // consistent order

            for (Edge edge : neighbors) {
                if (!visited.contains(edge.to)) {
                    visited.add(edge.to);
                    queue.offer(edge.to);
                }
            }
        }
        return order;
    }

    // ─── Algorithm 3: DFS Traversal ──────────────────────────────────────────
    // Time Complexity:  O(V + E)
    // Space Complexity: O(V)

    public List<String> dfs(String start) {
        if (!adjList.containsKey(start)) return Collections.emptyList();

        List<String> order = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        Stack<String> stack = new Stack<>();  // Stack for DFS

        stack.push(start);

        while (!stack.isEmpty()) {
            String curr = stack.pop();
            if (visited.contains(curr)) continue;
            visited.add(curr);
            order.add(curr);

            List<Edge> neighbors = new ArrayList<>(adjList.get(curr));
            neighbors.sort(Comparator.comparing((Edge e) -> e.to).reversed());

            for (Edge edge : neighbors) {
                if (!visited.contains(edge.to)) {
                    stack.push(edge.to);
                }
            }
        }
        return order;
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        for (String node : adjList.keySet()) {
            if (detectCycleDFS(node, visited, recStack, null)) return true;
        }
        return false;
    }

    private boolean detectCycleDFS(String node, Set<String> visited,
                                   Set<String> recStack, String parent) {
        if (recStack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        recStack.add(node);
        for (Edge edge : adjList.get(node)) {
            if (edge.to.equals(parent)) continue;
            if (detectCycleDFS(edge.to, visited, recStack, node)) return true;
        }
        recStack.remove(node);
        return false;
    }

    public int nodeCount() { return adjList.size(); }

    public int edgeCount() {
        int count = 0;
        for (List<Edge> edges : adjList.values()) count += edges.size();
        return count / 2;
    }

    public boolean isEmpty() { return adjList.isEmpty(); }
}