package graph.dagsp;

import graph.common.Graph;
import graph.topo.TopologicalSort;
import java.util.*;

public class DAGShortestPath {
    private final Graph graph;
    private final DAGSPMetrics metrics;
    
    public DAGShortestPath(Graph graph) {
        this.graph = graph;
        this.metrics = new DAGSPMetrics();
    }

    public PathResult shortestPaths(int source) {
        int n = graph.getN();
        
        long startTime = System.nanoTime();
        metrics.reset();
        
        // Get topological order
        TopologicalSort topo = new TopologicalSort(graph);
        List<Integer> topoOrder = topo.sort();
        
        if (topoOrder.isEmpty()) {
            throw new IllegalArgumentException("Graph contains a cycle - not a DAG");
        }
        
        // Initialize distances
        double[] dist = new double[n];
        int[] pred = new int[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(pred, -1);
        dist[source] = 0;
        
        // Process vertices in topological order
        for (int u : topoOrder) {
            if (dist[u] != Double.POSITIVE_INFINITY) {
                for (Graph.Edge edge : graph.getNeighbors(u)) {
                    metrics.relaxations++;
                    if (dist[u] + edge.weight < dist[edge.to]) {
                        dist[edge.to] = dist[u] + edge.weight;
                        pred[edge.to] = u;
                    }
                }
            }
        }
        
        long endTime = System.nanoTime();
        metrics.setElapsedTime(endTime - startTime);
        
        return new PathResult(dist, pred);
    }

    public PathResult longestPaths(int source) {
        int n = graph.getN();
        
        long startTime = System.nanoTime();
        metrics.reset();
        
        // Get topological order
        TopologicalSort topo = new TopologicalSort(graph);
        List<Integer> topoOrder = topo.sort();
        
        if (topoOrder.isEmpty()) {
            throw new IllegalArgumentException("Graph contains a cycle - not a DAG");
        }
        
        // Initialize distances (use negative infinity for longest path)
        double[] dist = new double[n];
        int[] pred = new int[n];
        Arrays.fill(dist, Double.NEGATIVE_INFINITY);
        Arrays.fill(pred, -1);
        dist[source] = 0;
        
        // Process vertices in topological order (maximize instead of minimize)
        for (int u : topoOrder) {
            if (dist[u] != Double.NEGATIVE_INFINITY) {
                for (Graph.Edge edge : graph.getNeighbors(u)) {
                    metrics.relaxations++;
                    if (dist[u] + edge.weight > dist[edge.to]) {
                        dist[edge.to] = dist[u] + edge.weight;
                        pred[edge.to] = u;
                    }
                }
            }
        }
        
        long endTime = System.nanoTime();
        metrics.setElapsedTime(endTime - startTime);
        
        return new PathResult(dist, pred);
    }

    public CriticalPathResult findCriticalPath() {
        int n = graph.getN();
        double maxLength = Double.NEGATIVE_INFINITY;
        int bestSource = -1;
        int bestTarget = -1;
        PathResult bestResult = null;
        
        // Try all possible source vertices
        for (int source = 0; source < n; source++) {
            PathResult result = longestPaths(source);
            for (int target = 0; target < n; target++) {
                if (result.distances[target] != Double.NEGATIVE_INFINITY && 
                    result.distances[target] > maxLength) {
                    maxLength = result.distances[target];
                    bestSource = source;
                    bestTarget = target;
                    bestResult = result;
                }
            }
        }
        
        List<Integer> path = reconstructPath(bestResult, bestTarget);
        return new CriticalPathResult(path, maxLength, bestSource, bestTarget);
    }

    public List<Integer> reconstructPath(PathResult result, int target) {
        if (result.predecessors[target] == -1 && result.distances[target] != 0) {
            return Collections.emptyList(); // No path exists
        }
        
        List<Integer> path = new ArrayList<>();
        int current = target;
        while (current != -1) {
            path.add(current);
            current = result.predecessors[current];
        }
        Collections.reverse(path);
        return path;
    }
    
    public DAGSPMetrics getMetrics() {
        return metrics;
    }
    
    public static class PathResult {
        public final double[] distances;
        public final int[] predecessors;
        
        public PathResult(double[] distances, int[] predecessors) {
            this.distances = distances;
            this.predecessors = predecessors;
        }
    }
    
    public static class CriticalPathResult {
        public final List<Integer> path;
        public final double length;
        public final int source;
        public final int target;
        
        public CriticalPathResult(List<Integer> path, double length, int source, int target) {
            this.path = path;
            this.length = length;
            this.source = source;
            this.target = target;
        }
    }
    
    public static class DAGSPMetrics implements graph.common.MetricsInterface {
        public int relaxations = 0;
        private long elapsedTime = 0;
        
        @Override
        public void reset() {
            relaxations = 0;
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
            return String.format("Relaxations: %d, Time: %.3f ms",
                    relaxations, elapsedTime / 1_000_000.0);
        }
    }
}


