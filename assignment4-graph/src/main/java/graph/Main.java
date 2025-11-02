package graph;

import graph.common.Graph;
import graph.common.GraphLoader;
import graph.common.Metrics;
import graph.scc.TarjanSCC;
import graph.topo.TopologicalSort;
import graph.dagsp.DAGShortestPath;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import graph.common.Graph.Edge;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    
    public static void main(String[] args) {
        try {
            executeAnalysis(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void executeAnalysis(String[] args) throws IOException {
        List<String[]> csvData = new ArrayList<>();
        JsonArray sparseResults = new JsonArray();
        JsonArray denseResults = new JsonArray();

        performWarmup();

        // Try to process multi-graph datasets if they exist
        processDataset("data/input_sparse.json", sparseResults, csvData);
        processDataset("data/input_dense.json", denseResults, csvData);

        // Fallback: process existing individual graph files
        if (sparseResults.isEmpty() && denseResults.isEmpty()) {
            System.out.println("Processing existing graph files from data/ directory...");
            processExistingGraphFiles(sparseResults, denseResults, csvData, args);
            System.out.println("Processed " + (sparseResults.size() + denseResults.size()) + " graphs");
        }

        // Export all results
        System.out.println("Exporting results...");
        exportResults(csvData, sparseResults, denseResults);
        System.out.println("Analysis complete! Results saved to:");
        if (!csvData.isEmpty()) System.out.println("  - data/output.csv");
        if (!sparseResults.isEmpty()) System.out.println("  - data/output_sparse.json");
        if (!denseResults.isEmpty()) System.out.println("  - data/output_dense.json");
    }

    private static void processExistingGraphFiles(JsonArray sparseResults, JsonArray denseResults, 
                                                   List<String[]> csvData, String[] args) throws IOException {
        // If single file provided via args
        if (args.length > 0) {
            GraphLoader.GraphData graphData = GraphLoader.loadFromJson(args[0]);
            JsonObject result = analyzeGraph(graphData, csvData);
            sparseResults.add(result);
            return;
        }

        // Otherwise process all graphs from data/ directory
        String[] graphFiles = {
            "data/small_1_simple_dag.json",
            "data/small_2_one_cycle.json",
            "data/small_3_dense_cycles.json",
            "data/medium_1_sparse_dag.json",
            "data/medium_2_multiple_sccs.json",
            "data/medium_3_mixed.json",
            "data/large_1_dag.json",
            "data/large_2_dense.json",
            "data/large_3_sparse_sccs.json"
        };

        int processed = 0;
        for (String filename : graphFiles) {
            try {
                GraphLoader.GraphData graphData = GraphLoader.loadFromJson(filename);
                System.out.println("Processing: " + filename);
                JsonObject result = analyzeGraph(graphData, csvData);
                processed++;
                
                // Categorize by filename
                if (filename.contains("sparse") || filename.contains("_dag")) {
                    sparseResults.add(result);
                } else if (filename.contains("dense") || filename.contains("cycle")) {
                    denseResults.add(result);
                } else {
                    sparseResults.add(result); // default
                }
            } catch (IOException e) {
                // Skip files that don't exist
                continue;
            }
        }
        if (processed == 0 && args.length == 0) {
            System.out.println("No graph files found. Please provide a graph file as argument or ensure files exist in data/ directory.");
        }
    }

    private static void processDataset(String filename, JsonArray results, List<String[]> csvData) {
        try {
            List<GraphLoader.GraphData> graphs = GraphLoader.loadAllGraphs(filename);
            System.out.println("Loading " + graphs.size() + " graphs from " + filename);
            for (GraphLoader.GraphData graphData : graphs) {
                JsonObject result = analyzeGraph(graphData, csvData);
                results.add(result);
            }
        } catch (IOException e) {
            // Silently skip - will use fallback
        }
    }

    private static void exportResults(List<String[]> csvData, JsonArray sparseResults, JsonArray denseResults) throws IOException {
        if (!csvData.isEmpty()) {
            Metrics.writeCsv("data/output.csv", csvData.toArray(new String[0][]), false);
        }
        if (!sparseResults.isEmpty()) {
            saveJsonFile("data/output_sparse.json", sparseResults);
        }
        if (!denseResults.isEmpty()) {
            saveJsonFile("data/output_dense.json", denseResults);
        }
    }

    private static JsonObject analyzeGraph(GraphLoader.GraphData graphData, List<String[]> csvData) {
        Graph graph = graphData.graph;
        GraphAnalysis analysis = new GraphAnalysis();
        analysis.graphId = graphData.getId();
        analysis.graph = graph;
        analysis.graphData = graphData;
        analysis.sourceVertex = graphData.source;

        // Step 1: Find strongly connected components
        forceGarbageCollection();
        TarjanSCC tarjan = new TarjanSCC(graph);
        analysis.sccs = tarjan.findSCCs();
        analysis.tarjanMetrics = adaptMetrics(tarjan.getMetrics(), "Tarjan-SCC");

        // Step 2: Build condensation DAG
        forceGarbageCollection();
        TarjanSCC.CondensationGraph condensation = tarjan.buildCondensation(analysis.sccs);
        analysis.dag = condensation.graph;

        // Step 3: Topological ordering
        forceGarbageCollection();
        TopologicalSort topoSort = new TopologicalSort(analysis.dag);
        analysis.topoOrder = topoSort.sort();
        analysis.topoMetrics = adaptMetrics(topoSort.getMetrics(), "Kahn-TS");

        // Step 4: Shortest paths computation
        forceGarbageCollection();
        DAGShortestPath spSolver = new DAGShortestPath(analysis.dag);
        int dagSource = condensation.vertexToSCC[analysis.sourceVertex];
        analysis.spResult = spSolver.shortestPaths(dagSource);
        analysis.spMetrics = adaptMetrics(spSolver.getMetrics(), "DAG-ShortestPath");

        // Step 5: Critical path (longest path)
        forceGarbageCollection();
        DAGShortestPath lpSolver = new DAGShortestPath(analysis.dag);
        analysis.cpResult = lpSolver.findCriticalPath();
        analysis.lpMetrics = adaptMetrics(lpSolver.getMetrics(), "DAG-LongestPath");

        appendCsvRow(csvData, analysis);
        return serializeToJson(analysis);
    }

    private static Metrics adaptMetrics(graph.common.MetricsInterface oldMetrics, String algorithmName) {
        Metrics newMetrics = new Metrics(algorithmName);
        // Convert time from old metrics
        newMetrics.setElapsedTime(oldMetrics.getElapsedTime());
        
        // Extract operation counts from old metrics classes
        if (oldMetrics instanceof TarjanSCC.SCCMetrics) {
            TarjanSCC.SCCMetrics scc = (TarjanSCC.SCCMetrics) oldMetrics;
            // Manually set operations by calling increment methods
            for (int i = 0; i < scc.dfsVisits; i++) newMetrics.incrementDFSVisit();
            for (int i = 0; i < scc.edgesExplored; i++) newMetrics.incrementEdgeExploration();
            // Approximate stack operations and low link updates
            for (int i = 0; i < scc.dfsVisits * 2; i++) newMetrics.incrementStackOperation();
            for (int i = 0; i < scc.edgesExplored; i++) newMetrics.incrementLowLinkUpdate();
        } else if (oldMetrics instanceof TopologicalSort.TopoMetrics) {
            TopologicalSort.TopoMetrics topo = (TopologicalSort.TopoMetrics) oldMetrics;
            for (int i = 0; i < topo.pushes; i++) newMetrics.incrementQueueOperation();
            for (int i = 0; i < topo.pops; i++) {
                newMetrics.incrementQueueOperation();
                newMetrics.incrementInDegreeUpdate(); // Approximate
            }
        } else if (oldMetrics instanceof DAGShortestPath.DAGSPMetrics) {
            DAGShortestPath.DAGSPMetrics dag = (DAGShortestPath.DAGSPMetrics) oldMetrics;
            for (int i = 0; i < dag.relaxations; i++) {
                newMetrics.incrementRelaxation();
                newMetrics.incrementComparison();
            }
        }
        
        return newMetrics;
    }

    private static void forceGarbageCollection() {
        System.gc();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void performWarmup() {
        try {
            // Try to load graphs for warmup
            List<GraphLoader.GraphData> warmupGraphs = new ArrayList<>();
            
            // Try multi-graph format first
            try {
                warmupGraphs = GraphLoader.loadAllGraphs("data/input_dense.json");
            } catch (IOException e) {
                try {
                    warmupGraphs = GraphLoader.loadAllGraphs("data/input_sparse.json");
                } catch (IOException e2) {
                    // Fallback to single graph files
                    try {
                        warmupGraphs.add(GraphLoader.loadFromJson("data/small_1_simple_dag.json"));
                    } catch (IOException e3) {
                        return; // No warmup graphs available
                    }
                }
            }

            for (int iteration = 0; iteration < 3; iteration++) {
                for (GraphLoader.GraphData gd : warmupGraphs) {
                    Graph graph = gd.graph;
                    int source = gd.source;
            
            TarjanSCC tarjan = new TarjanSCC(graph);
            List<List<Integer>> sccs = tarjan.findSCCs();
            
                    TarjanSCC.CondensationGraph condensation = tarjan.buildCondensation(sccs);
                    Graph dag = condensation.graph;

                    TopologicalSort topoSort = new TopologicalSort(dag);
                    topoSort.sort();

                    DAGShortestPath shortestPath = new DAGShortestPath(dag);
                    int dagSource = condensation.vertexToSCC[source];
                    shortestPath.shortestPaths(dagSource);

                    DAGShortestPath longestPath = new DAGShortestPath(dag);
                    longestPath.findCriticalPath();
                }
            }
        } catch (Exception e) {
            // Warmup failed silently - not critical for execution
        }
    }

    private static void appendCsvRow(List<String[]> csvData, GraphAnalysis r) {
        int id = r.graphId;

        int vertices = r.graph.getN();
        int edges = countEdges(r.graph);

        String density = r.graphData.getDensity();
        String variant = r.graphData.getVariant();

        long spTotalOps = r.tarjanMetrics.getTotalOperations() +
                r.topoMetrics.getTotalOperations() +
                r.spMetrics.getTotalOperations();
        double spTotalTime = r.tarjanMetrics.getExecutionTimeMs() +
                r.topoMetrics.getExecutionTimeMs() +
                r.spMetrics.getExecutionTimeMs();

        long lpTotalOps = r.tarjanMetrics.getTotalOperations() +
                r.topoMetrics.getTotalOperations() +
                r.lpMetrics.getTotalOperations();
        double lpTotalTime = r.tarjanMetrics.getExecutionTimeMs() +
                r.topoMetrics.getExecutionTimeMs() +
                r.lpMetrics.getExecutionTimeMs();

        if (r.spResult != null) {
            int dagSource = findDagSourceVertex(r);
            List<Integer> spPath = extractPathFromResult(r.spResult, dagSource);
            double spLength = computePathWeight(r.dag, spPath);

            csvData.add(new String[]{
                    String.valueOf(id),
                    String.valueOf(vertices),
                    String.valueOf(edges),
                    density,
                    variant,
                    "DAG-ShortestPath",
                    String.valueOf(spTotalOps),
                    String.format("%.2f", spLength),
                    String.format("%.3f", spTotalTime)
            });
        }

        csvData.add(new String[]{
                String.valueOf(id),
                String.valueOf(vertices),
                String.valueOf(edges),
                density,
                variant,
                "DAG-LongestPath",
                String.valueOf(lpTotalOps),
                String.format("%.2f", r.cpResult.length),
                String.format("%.3f", lpTotalTime)
        });
    }

    private static int findDagSourceVertex(GraphAnalysis r) {
        // Find the source vertex in the DAG (from condensation)
        for (int i = 0; i < r.sccs.size(); i++) {
            if (r.sccs.get(i).contains(r.sourceVertex)) {
                return i;
            }
        }
        return 0;
    }

    private static int countEdges(Graph graph) {
        int count = 0;
        for (int i = 0; i < graph.getN(); i++) {
            count += graph.getNeighbors(i).size();
        }
        return count;
    }

    private static JsonObject serializeToJson(GraphAnalysis r) {
        JsonObject graphJson = new JsonObject();
        graphJson.addProperty("graph_id", r.graphId);

        JsonObject inputStats = new JsonObject();
        inputStats.addProperty("vertices", r.graph.getN());
        inputStats.addProperty("edges", countEdges(r.graph));
        inputStats.addProperty("density", r.graphData.getDensity());
        inputStats.addProperty("variant", r.graphData.getVariant());
        inputStats.addProperty("source", r.sourceVertex);
        graphJson.add("input_stats", inputStats);

        JsonObject tarjanJson = new JsonObject();
        tarjanJson.addProperty("num_sccs", r.sccs.size());
        JsonArray sccsArray = new JsonArray();
        for (List<Integer> scc : r.sccs) {
            JsonArray sccArray = new JsonArray();
            for (Integer v : scc) sccArray.add(v);
            sccsArray.add(sccArray);
        }
        tarjanJson.add("sccs", sccsArray);
        tarjanJson.addProperty("operations_count", r.tarjanMetrics.getTotalOperations());
        tarjanJson.addProperty("execution_time_ms", r.tarjanMetrics.getExecutionTimeMs());
        graphJson.add("tarjan_scc", tarjanJson);

        JsonObject condensationJson = new JsonObject();
        condensationJson.addProperty("vertices", r.dag.getN());
        condensationJson.addProperty("edges", countEdges(r.dag));
        graphJson.add("condensation_graph", condensationJson);

        JsonObject topoJson = new JsonObject();
        JsonArray topoOrder = new JsonArray();
        for (Integer v : r.topoOrder) topoOrder.add(v);
        topoJson.add("topological_order", topoOrder);
        topoJson.addProperty("operations_count", r.topoMetrics.getTotalOperations());
        topoJson.addProperty("execution_time_ms", r.topoMetrics.getExecutionTimeMs());
        graphJson.add("topological_sort", topoJson);

        if (r.spResult != null) {
            JsonObject spJson = new JsonObject();
            spJson.addProperty("source", r.sourceVertex);

            int dagSource = findDagSourceVertex(r);
            List<Integer> spPath = extractPathFromResult(r.spResult, dagSource);
            JsonArray spPathArray = new JsonArray();
            for (Integer v : spPath) spPathArray.add(v);
            spJson.add("path", spPathArray);

            double pathLength = 0;
            JsonArray spEdges = new JsonArray();
            for (int i = 0; i < spPath.size() - 1; i++) {
                int u = spPath.get(i);
                int v = spPath.get(i + 1);
                for (Edge e : r.dag.getNeighbors(u)) {
                    if (e.to == v) {
                        JsonObject edgeObj = new JsonObject();
                        edgeObj.addProperty("u", u);
                        edgeObj.addProperty("v", v);
                        edgeObj.addProperty("w", e.weight);
                        spEdges.add(edgeObj);
                        pathLength += e.weight;
                        break;
                    }
                }
            }
            spJson.add("edges", spEdges);
            spJson.addProperty("path_length", pathLength);
            spJson.addProperty("operations_count", r.spMetrics.getTotalOperations());
            spJson.addProperty("execution_time_ms", r.spMetrics.getExecutionTimeMs());

            long totalOps = r.tarjanMetrics.getTotalOperations() +
                    r.topoMetrics.getTotalOperations() +
                    r.spMetrics.getTotalOperations();
            double totalTime = r.tarjanMetrics.getExecutionTimeMs() +
                    r.topoMetrics.getExecutionTimeMs() +
                    r.spMetrics.getExecutionTimeMs();
            spJson.addProperty("total_operations_count", totalOps);
            spJson.addProperty("total_execution_time_ms", totalTime);

            graphJson.add("shortest_path", spJson);
        }

        JsonObject lpJson = new JsonObject();
        lpJson.addProperty("critical_path_length", r.cpResult.length);

        List<Integer> cpPath = r.cpResult.path;
        if (cpPath != null) {
            JsonArray cpArray = new JsonArray();
            for (Integer v : cpPath) cpArray.add(v);
            lpJson.add("critical_path", cpArray);

            JsonArray lpEdges = new JsonArray();
            for (int i = 0; i < cpPath.size() - 1; i++) {
                int u = cpPath.get(i);
                int v = cpPath.get(i + 1);
                for (Edge e : r.dag.getNeighbors(u)) {
                    if (e.to == v) {
                        JsonObject edgeObj = new JsonObject();
                        edgeObj.addProperty("u", u);
                        edgeObj.addProperty("v", v);
                        edgeObj.addProperty("w", e.weight);
                        lpEdges.add(edgeObj);
                        break;
                    }
                }
            }
            lpJson.add("edges", lpEdges);
        }

        lpJson.addProperty("operations_count", r.lpMetrics.getTotalOperations());
        lpJson.addProperty("execution_time_ms", r.lpMetrics.getExecutionTimeMs());

        long totalOps = r.tarjanMetrics.getTotalOperations() +
                r.topoMetrics.getTotalOperations() +
                r.lpMetrics.getTotalOperations();
        double totalTime = r.tarjanMetrics.getExecutionTimeMs() +
                r.topoMetrics.getExecutionTimeMs() +
                r.lpMetrics.getExecutionTimeMs();
        lpJson.addProperty("total_operations_count", totalOps);
        lpJson.addProperty("total_execution_time_ms", totalTime);

        graphJson.add("longest_path", lpJson);

        return graphJson;
    }

    private static List<Integer> extractPathFromResult(DAGShortestPath.PathResult pathResult, int source) {
        List<Integer> path = new ArrayList<>();
        double[] distances = pathResult.distances;
        int[] parent = pathResult.predecessors;

        for (int i = 0; i < distances.length; i++) {
            if (distances[i] != Double.POSITIVE_INFINITY && distances[i] != 0) {
                List<Integer> toVertex = new ArrayList<>();
                for (int v = i; v != -1; v = parent[v]) {
                    toVertex.add(v);
                }
                java.util.Collections.reverse(toVertex);
                if (toVertex.size() > path.size()) {
                    path = toVertex;
                }
            }
        }

        if (path.isEmpty()) {
            path.add(source);
        }

        return path;
    }

    private static double computePathWeight(Graph dag, List<Integer> path) {
        double length = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            int u = path.get(i);
            int v = path.get(i + 1);
            for (Edge e : dag.getNeighbors(u)) {
                if (e.to == v) {
                    length += e.weight;
                    break;
                }
            }
        }
        return length;
    }

    private static void saveJsonFile(String filepath, JsonArray results) throws IOException {
        JsonObject root = new JsonObject();
        root.add("results", results);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filepath)) {
            gson.toJson(root, writer);
        }
    }

    private static class GraphAnalysis {
        int graphId;
        Graph graph;
        GraphLoader.GraphData graphData;
        int sourceVertex;
        List<List<Integer>> sccs;
        Graph dag;
        List<Integer> topoOrder;
        DAGShortestPath.PathResult spResult;
        DAGShortestPath.CriticalPathResult cpResult;
        Metrics tarjanMetrics;
        Metrics topoMetrics;
        Metrics spMetrics;
        Metrics lpMetrics;
    }
}
