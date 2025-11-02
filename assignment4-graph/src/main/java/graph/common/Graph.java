package graph.common;

import java.util.*;

public class Graph {
    private final int n; // number of vertices
    private final List<List<Edge>> adjList;
    private final boolean directed;

    public Graph(int n, boolean directed) {
        this.n = n;
        this.directed = directed;
        this.adjList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
    }

    public void addEdge(int u, int v, double weight) {
        adjList.get(u).add(new Edge(u, v, weight));
        if (!directed) {
            adjList.get(v).add(new Edge(v, u, weight));
        }
    }

    public int getN() {
        return n;
    }

    public List<Edge> getNeighbors(int u) {
        return adjList.get(u);
    }

    public boolean isDirected() {
        return directed;
    }

    public Graph reverse() {
        Graph rev = new Graph(n, true);
        for (int u = 0; u < n; u++) {
            for (Edge e : adjList.get(u)) {
                rev.addEdge(e.to, e.from, e.weight);
            }
        }
        return rev;
    }

    public static class Edge {
        public final int from;
        public final int to;
        public final double weight;

        public Edge(int from, int to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }
}

