package graph.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class DatasetGenerator {
    
    private final Random random;
    
    public DatasetGenerator(long seed) {
        this.random = new Random(seed);
    }

    public void generateAllDatasets(String outputDir) throws IOException {
        // Small datasets (6-10 nodes)
        generateDataset(outputDir + "/small_1_simple_dag.json", 8, 10, true, false, 0, "Simple DAG");
        generateDataset(outputDir + "/small_2_one_cycle.json", 10, 15, true, true, 1, "One small cycle");
        generateDataset(outputDir + "/small_3_dense_cycles.json", 8, 20, true, true, 2, "Dense with cycles");
        
        // Medium datasets (10-20 nodes)
        generateDataset(outputDir + "/medium_1_sparse_dag.json", 15, 20, true, false, 0, "Sparse DAG");
        generateDataset(outputDir + "/medium_2_multiple_sccs.json", 18, 35, true, true, 3, "Multiple SCCs");
        generateDataset(outputDir + "/medium_3_mixed.json", 20, 40, true, true, 2, "Mixed structure");
        
        // Large datasets (20-50 nodes)
        generateDataset(outputDir + "/large_1_dag.json", 40, 60, true, false, 0, "Large DAG");
        generateDataset(outputDir + "/large_2_dense.json", 30, 120, true, true, 4, "Large dense with SCCs");
        generateDataset(outputDir + "/large_3_sparse_sccs.json", 50, 80, true, true, 5, "Large sparse with SCCs");
    }
    
    private void generateDataset(String filename, int n, int numEdges, 
                                 boolean directed, boolean allowCycles, 
                                 int numCycles, String description) throws IOException {
        System.out.println("Generating: " + filename + " (" + description + ")");
        
        GraphData data = new GraphData();
        data.directed = directed;
        data.n = n;
        data.source = 0;
        data.weight_model = "edge";
        data.description = description;
        data.edges = new ArrayList<>();
        
        Set<String> existingEdges = new HashSet<>();
        
        if (!allowCycles) {
            // Generate DAG
            generateDAG(data, n, numEdges, existingEdges);
        } else {
            // Generate graph with cycles
            generateWithCycles(data, n, numEdges, numCycles, existingEdges);
        }
        
        // Write to file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(data, writer);
        }
    }
    
    private void generateDAG(GraphData data, int n, int numEdges, Set<String> existingEdges) {
        // For DAG, only add edges from lower to higher numbered vertices
        int edgesAdded = 0;
        int attempts = 0;
        int maxAttempts = numEdges * 10;
        
        while (edgesAdded < numEdges && attempts < maxAttempts) {
            int u = random.nextInt(n);
            int v = random.nextInt(n);
            
            // Ensure u < v for DAG property
            if (u >= v) {
                int temp = u;
                u = v;
                v = temp;
            }
            
            if (u == v) {
                attempts++;
                continue;
            }
            
            String edgeKey = u + "->" + v;
            if (!existingEdges.contains(edgeKey)) {
                EdgeData edge = new EdgeData();
                edge.u = u;
                edge.v = v;
                edge.w = 1 + random.nextInt(10);
                data.edges.add(edge);
                existingEdges.add(edgeKey);
                edgesAdded++;
            }
            attempts++;
        }
    }
    
    private void generateWithCycles(GraphData data, int n, int numEdges, 
                                   int numCycles, Set<String> existingEdges) {
        int edgesAdded = 0;
        
        // First, create specific cycles
        for (int i = 0; i < numCycles && edgesAdded < numEdges; i++) {
            int cycleSize = 2 + random.nextInt(Math.min(5, n - 1));
            List<Integer> cycle = new ArrayList<>();
            Set<Integer> used = new HashSet<>();
            
            // Select random vertices for this cycle
            for (int j = 0; j < cycleSize; j++) {
                int v;
                do {
                    v = random.nextInt(n);
                } while (used.contains(v));
                cycle.add(v);
                used.add(v);
            }
            
            // Connect them in a cycle
            for (int j = 0; j < cycleSize; j++) {
                int u = cycle.get(j);
                int v = cycle.get((j + 1) % cycleSize);
                String edgeKey = u + "->" + v;
                
                if (!existingEdges.contains(edgeKey)) {
                    EdgeData edge = new EdgeData();
                    edge.u = u;
                    edge.v = v;
                    edge.w = 1 + random.nextInt(10);
                    data.edges.add(edge);
                    existingEdges.add(edgeKey);
                    edgesAdded++;
                }
            }
        }
        
        // Add remaining edges randomly
        int attempts = 0;
        int maxAttempts = (numEdges - edgesAdded) * 10;
        
        while (edgesAdded < numEdges && attempts < maxAttempts) {
            int u = random.nextInt(n);
            int v = random.nextInt(n);
            
            if (u == v) {
                attempts++;
                continue;
            }
            
            String edgeKey = u + "->" + v;
            if (!existingEdges.contains(edgeKey)) {
                EdgeData edge = new EdgeData();
                edge.u = u;
                edge.v = v;
                edge.w = 1 + random.nextInt(10);
                data.edges.add(edge);
                existingEdges.add(edgeKey);
                edgesAdded++;
            }
            attempts++;
        }
    }
    
    static class GraphData {
        boolean directed;
        int n;
        List<EdgeData> edges;
        int source;
        String weight_model;
        String description;
    }
    
    static class EdgeData {
        int u;
        int v;
        int w;
    }
    
    public static void main(String[] args) {
        try {
            DatasetGenerator generator = new DatasetGenerator(42);
            String outputDir = "data";
            new java.io.File(outputDir).mkdirs();
            generator.generateAllDatasets(outputDir);
            System.out.println("All datasets generated successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


