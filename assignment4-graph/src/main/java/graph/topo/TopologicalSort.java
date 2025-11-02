package graph.topo;

import graph.common.Graph;
import java.util.*;

public class TopologicalSort {
    private final Graph graph;
    private final TopoMetrics metrics;
    
    public TopologicalSort(Graph graph) {
        this.graph = graph;
        this.metrics = new TopoMetrics();
    }

    public List<Integer> sort() {
        int n = graph.getN();
        int[] inDegree = new int[n];
        
        long startTime = System.nanoTime();
        metrics.reset();
        
        // Calculate in-degrees
        for (int u = 0; u < n; u++) {
            for (Graph.Edge edge : graph.getNeighbors(u)) {
                inDegree[edge.to]++;
            }
        }
        
        // Initialize queue with vertices having in-degree 0
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
                metrics.pushes++;
            }
        }
        
        List<Integer> topoOrder = new ArrayList<>();
        
        while (!queue.isEmpty()) {
            int u = queue.poll();
            metrics.pops++;
            topoOrder.add(u);
            
            // Reduce in-degree for neighbors
            for (Graph.Edge edge : graph.getNeighbors(u)) {
                inDegree[edge.to]--;
                if (inDegree[edge.to] == 0) {
                    queue.offer(edge.to);
                    metrics.pushes++;
                }
            }
        }
        
        long endTime = System.nanoTime();
        metrics.setElapsedTime(endTime - startTime);
        
        // If not all vertices are in the order, there's a cycle
        if (topoOrder.size() != n) {
            return Collections.emptyList();
        }
        
        return topoOrder;
    }

    public List<Integer> sortDFS() {
        int n = graph.getN();
        boolean[] visited = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();
        
        long startTime = System.nanoTime();
        metrics.reset();
        
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                if (!dfs(i, visited, stack, new boolean[n])) {
                    return Collections.emptyList(); // Cycle detected
                }
            }
        }
        
        long endTime = System.nanoTime();
        metrics.setElapsedTime(endTime - startTime);
        
        List<Integer> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }
    
    private boolean dfs(int u, boolean[] visited, Deque<Integer> stack, boolean[] recStack) {
        visited[u] = true;
        recStack[u] = true;
        metrics.pushes++;
        
        for (Graph.Edge edge : graph.getNeighbors(u)) {
            if (!visited[edge.to]) {
                if (!dfs(edge.to, visited, stack, recStack)) {
                    return false;
                }
            } else if (recStack[edge.to]) {
                return false; // Cycle detected
            }
        }
        
        recStack[u] = false;
        stack.push(u);
        metrics.pops++;
        return true;
    }
    
    public TopoMetrics getMetrics() {
        return metrics;
    }
    
    public static class TopoMetrics implements graph.common.MetricsInterface {
        public int pushes = 0;
        public int pops = 0;
        private long elapsedTime = 0;
        
        @Override
        public void reset() {
            pushes = 0;
            pops = 0;
            elapsedTime = 0;
        }
        
        @Override
        public long getElapsedTime() {
            return elapsedTime;
        }
        
        @Override
        public void setElapsedTime(long nanos) {
            this.elapsedTime = nanos;
        }
        
        @Override
        public String getSummary() {
            return String.format("Pushes: %d, Pops: %d, Time: %.3f ms",
                    pushes, pops, elapsedTime / 1_000_000.0);
        }
    }
}


