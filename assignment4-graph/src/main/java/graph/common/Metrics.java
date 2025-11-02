package graph.common;

/**
 * Tracks performance metrics for all algorithms.
 * Measures execution time and operation counts (comparisons, updates, etc.)
 */
public class Metrics implements MetricsInterface {
    private long startTime;
    private double executionTimeMs = 0.0;
    private String algorithmName;

    // SCC-specific metrics (Tarjan)
    private long dfsVisits = 0;
    private long edgeExplorations = 0;
    private long stackOperations = 0;
    private long lowLinkUpdates = 0;

    // Topological Sort metrics (Kahn)
    private long queueOperations = 0;
    private long inDegreeUpdates = 0;

    // DAG Shortest/Longest Path metrics
    private long relaxations = 0;
    private long distanceUpdates = 0;
    private long comparisons = 0;

    public Metrics(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public void startTimer() {
        startTime = System.nanoTime();
    }

    public void stopTimer() {
        long endTime = System.nanoTime();
        executionTimeMs = (endTime - startTime) / 1_000_000.0;
    }

    // Increment methods for each operation type
    public void incrementDFSVisit() {
        dfsVisits++;
    }

    public void incrementEdgeExploration() {
        edgeExplorations++;
    }

    public void incrementStackOperation() {
        stackOperations++;
    }

    public void incrementLowLinkUpdate() {
        lowLinkUpdates++;
    }

    public void incrementQueueOperation() {
        queueOperations++;
    }

    public void incrementInDegreeUpdate() {
        inDegreeUpdates++;
    }

    public void incrementRelaxation() {
        relaxations++;
    }

    public void incrementDistanceUpdate() {
        distanceUpdates++;
    }

    public void incrementComparison() {
        comparisons++;
    }

    // Getter methods
    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Calculates total operations based on algorithm type.
     */
    public long getTotalOperations() {
        if (algorithmName.contains("SCC") || algorithmName.contains("Tarjan")) {
            return dfsVisits + edgeExplorations + stackOperations + lowLinkUpdates;
        } else if (algorithmName.contains("Topo") || algorithmName.contains("Kahn")) {
            return queueOperations + inDegreeUpdates;
        } else if (algorithmName.contains("Path") || algorithmName.contains("DAG")) {
            return relaxations + distanceUpdates + comparisons;
        }
        return 0;
    }

    /**
     * Resets all metrics to zero.
     */
    public void reset() {
        dfsVisits = 0;
        edgeExplorations = 0;
        stackOperations = 0;
        lowLinkUpdates = 0;
        queueOperations = 0;
        inDegreeUpdates = 0;
        relaxations = 0;
        distanceUpdates = 0;
        comparisons = 0;
        executionTimeMs = 0.0;
    }

    /**
     * Writes data to CSV file with semicolon delimiter.
     */
    public static void writeCsv(String filePath, String[][] data, boolean append) throws java.io.IOException {
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(filePath, append))) {
            if (!append) {
                writer.write("graph_id;vertices;edges;density;variant;algorithm;total_operations_count;path_length;total_execution_time_ms\n");
            }
            for (String[] row : data) {
                writer.write(String.join(";", row) + "\n");
            }
        } catch (java.io.IOException e) {
            System.err.println("Error writing CSV: " + e.getMessage());
            throw e;
        }
    }

    // Legacy interface compatibility methods
    public long getElapsedTime() {
        return (long)(executionTimeMs * 1_000_000);
    }

    public void setElapsedTime(long nanos) {
        this.executionTimeMs = nanos / 1_000_000.0;
    }

    public String getSummary() {
        return String.format("Total Operations: %d, Time: %.3f ms", getTotalOperations(), executionTimeMs);
    }
}


