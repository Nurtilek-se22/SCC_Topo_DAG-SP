package graph.scc;

import graph.common.Graph;
import java.util.*;

public class TarjanSCC {
    private final Graph graph;
    private final SCCMetrics metrics;
    
    private int[] lowLink;
    private int[] ids;
    private boolean[] onStack;
    private Deque<Integer> stack;
    private int id;
    private List<List<Integer>> sccs;
    
    public TarjanSCC(Graph graph) {
        this.graph = graph;
        this.metrics = new SCCMetrics();
    }

    public List<List<Integer>> findSCCs() {
        int n = graph.getN();
        lowLink = new int[n];
        ids = new int[n];
        onStack = new boolean[n];
        stack = new ArrayDeque<>();
        sccs = new ArrayList<>();
        
        Arrays.fill(ids, -1);
        id = 0;
        
        long startTime = System.nanoTime();
        metrics.reset();
        
        // Run DFS from all unvisited nodes
        for (int i = 0; i < n; i++) {
            if (ids[i] == -1) {
                dfs(i);
            }
        }
        
        long endTime = System.nanoTime();
        metrics.setElapsedTime(endTime - startTime);
        
        return sccs;
    }
    
    private void dfs(int at) {
        metrics.dfsVisits++;
        
        ids[at] = lowLink[at] = id++;
        stack.push(at);
        onStack[at] = true;
        
        // Visit all neighbors
        for (Graph.Edge edge : graph.getNeighbors(at)) {
            metrics.edgesExplored++;
            int to = edge.to;
            
            if (ids[to] == -1) {
                dfs(to);
            }
            if (onStack[to]) {
                lowLink[at] = Math.min(lowLink[at], lowLink[to]);
            }
        }
        
        // Found SCC root
        if (ids[at] == lowLink[at]) {
            List<Integer> scc = new ArrayList<>();
            while (true) {
                int node = stack.pop();
                onStack[node] = false;
                scc.add(node);
                if (node == at) break;
            }
            sccs.add(scc);
            metrics.sccCount++;
        }
    }

    public CondensationGraph buildCondensation(List<List<Integer>> sccs) {
        int n = graph.getN();
        int numSCCs = sccs.size();
        
        // Map each vertex to its SCC index
        int[] vertexToSCC = new int[n];
        for (int i = 0; i < sccs.size(); i++) {
            for (int v : sccs.get(i)) {
                vertexToSCC[v] = i;
            }
        }
        
        // Build condensation graph
        Graph condensation = new Graph(numSCCs, true);
        Set<String> addedEdges = new HashSet<>();
        
        for (int u = 0; u < n; u++) {
            int sccU = vertexToSCC[u];
            for (Graph.Edge edge : graph.getNeighbors(u)) {
                int sccV = vertexToSCC[edge.to];
                if (sccU != sccV) {
                    String edgeKey = sccU + "->" + sccV;
                    if (!addedEdges.contains(edgeKey)) {
                        condensation.addEdge(sccU, sccV, edge.weight);
                        addedEdges.add(edgeKey);
                    }
                }
            }
        }
        
        return new CondensationGraph(condensation, sccs, vertexToSCC);
    }
    
    public SCCMetrics getMetrics() {
        return metrics;
    }
    
    public static class CondensationGraph {
        public final Graph graph;
        public final List<List<Integer>> sccs;
        public final int[] vertexToSCC;
        
        public CondensationGraph(Graph graph, List<List<Integer>> sccs, int[] vertexToSCC) {
            this.graph = graph;
            this.sccs = sccs;
            this.vertexToSCC = vertexToSCC;
        }
    }
    
    public static class SCCMetrics implements graph.common.MetricsInterface {
        public int dfsVisits = 0;
        public int edgesExplored = 0;
        public int sccCount = 0;
        private long elapsedTime = 0;
        
        @Override
        public void reset() {
            dfsVisits = 0;
            edgesExplored = 0;
            sccCount = 0;
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
            return String.format("DFS Visits: %d, Edges Explored: %d, SCCs Found: %d, Time: %.3f ms",
                    dfsVisits, edgesExplored, sccCount, elapsedTime / 1_000_000.0);
        }
    }
}


