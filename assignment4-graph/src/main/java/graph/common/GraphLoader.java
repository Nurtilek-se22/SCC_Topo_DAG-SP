package graph.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GraphLoader {
    
    /**
     * Loads a single graph from JSON file (backward compatibility).
     */
    public static GraphData loadFromJson(String filename) throws IOException {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(new FileReader(filename), JsonObject.class);
        
        boolean directed = json.get("directed").getAsBoolean();
        int n = json.get("n").getAsInt();
        String weightModel = json.has("weight_model") ? json.get("weight_model").getAsString() : "edge";
        int source = json.has("source") ? json.get("source").getAsInt() : 0;
        String density = json.has("density") ? json.get("density").getAsString() : "unknown";
        String variant = json.has("variant") ? json.get("variant").getAsString() : "unknown";
        int id = json.has("id") ? json.get("id").getAsInt() : 0;
        
        Graph graph = new Graph(n, directed);
        
        JsonArray edges = json.getAsJsonArray("edges");
        for (int i = 0; i < edges.size(); i++) {
            JsonObject edge = edges.get(i).getAsJsonObject();
            int u = edge.get("u").getAsInt();
            int v = edge.get("v").getAsInt();
            double w = edge.get("w").getAsDouble();
            graph.addEdge(u, v, w);
        }
        
        return new GraphData(id, graph, source, weightModel, density, variant);
    }
    
    /**
     * Loads all graphs from a JSON file with validation (supports multiple graphs format).
     */
    public static List<GraphData> loadAllGraphs(String filepath) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filepath)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) throw new IOException("Invalid JSON format");

            JsonArray graphsArray = root.getAsJsonArray("graphs");
            if (graphsArray == null) {
                // Single graph format - wrap it
                JsonObject singleGraph = root;
                List<GraphData> result = new ArrayList<>();
                result.add(parseSingleGraph(singleGraph));
                return result;
            }

            List<GraphData> graphsList = new ArrayList<>();

            // Parse each graph
            for (JsonElement element : graphsArray) {
                JsonObject graphJson = element.getAsJsonObject();
                graphsList.add(parseSingleGraph(graphJson));
            }

            return graphsList;
        } catch (IOException e) {
            // Re-throw without printing - caller will handle gracefully
            throw e;
        }
    }
    
    private static GraphData parseSingleGraph(JsonObject graphJson) {
        int id = graphJson.has("id") ? graphJson.get("id").getAsInt() : 0;
        boolean directed = graphJson.has("directed") ? graphJson.get("directed").getAsBoolean() : true;
        int n = graphJson.get("n").getAsInt();

        if (n <= 0) throw new IllegalArgumentException("Vertices must be > 0");

        int source = graphJson.has("source") ? graphJson.get("source").getAsInt() : 0;
        if (source < 0 || source >= n) {
            throw new IllegalArgumentException("Source out of bounds");
        }

        String density = graphJson.has("density") ? graphJson.get("density").getAsString() : "unknown";
        String variant = graphJson.has("variant") ? graphJson.get("variant").getAsString() : "unknown";
        String weightModel = graphJson.has("weight_model") ? graphJson.get("weight_model").getAsString() : "edge";

        Graph graph = new Graph(n, directed);

        // Parse edges
        JsonArray edges = graphJson.getAsJsonArray("edges");
        if (edges != null) {
            for (JsonElement edgeElem : edges) {
                JsonObject edge = edgeElem.getAsJsonObject();
                int u = edge.get("u").getAsInt();
                int v = edge.get("v").getAsInt();
                double w = edge.get("w").getAsDouble();

                if (u < 0 || u >= n || v < 0 || v >= n) {
                    throw new IllegalArgumentException("Edge vertex out of bounds");
                }

                graph.addEdge(u, v, w);
            }
        }

        return new GraphData(id, graph, source, weightModel, density, variant);
    }
    
    public static class GraphData {
        private final int id;
        public final Graph graph;
        public final int source;
        public final String weightModel;
        private final String density;
        private final String variant;
        
        public GraphData(int id, Graph graph, int source, String weightModel, String density, String variant) {
            this.id = id;
            this.graph = graph;
            this.source = source;
            this.weightModel = weightModel;
            this.density = density;
            this.variant = variant;
        }
        
        public GraphData(Graph graph, int source, String weightModel) {
            this(0, graph, source, weightModel, "unknown", "unknown");
        }
        
        public int getId() {
            return id;
        }
        
        public String getDensity() {
            return density;
        }
        
        public String getVariant() {
            return variant;
        }
    }
}


