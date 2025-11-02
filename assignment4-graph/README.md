# Assignment 4: Graph Algorithms - SCC, Topological Sort, and DAG Shortest Paths

## Overview

This project implements three fundamental graph algorithms for analyzing task dependencies in a "Smart City/Smart Campus Scheduling" scenario:

1. **Strongly Connected Components (SCC)** - Using Tarjan's algorithm
2. **Topological Sorting** - Using Kahn's algorithm (BFS-based)
3. **Shortest/Longest Paths in DAGs** - Using dynamic programming over topological order

## Project Structure

```
assignment4-graph/
 pom.xml                          # Maven configuration
 README.md                        # This file
 data/                           # Generated test datasets (9 files)
    small_1_simple_dag.json
    small_2_one_cycle.json
    small_3_dense_cycles.json
    medium_1_sparse_dag.json
    medium_2_multiple_sccs.json
    medium_3_mixed.json
    large_1_dag.json
    large_2_dense.json
    large_3_sparse_sccs.json
 src/
    main/java/graph/
       Main.java               # Main runner
       common/
          Graph.java          # Graph data structure
          GraphLoader.java    # JSON loader
          Metrics.java        # Common metrics interface
       scc/
          TarjanSCC.java      # Tarjan's SCC algorithm
       topo/
          TopologicalSort.java # Topological sorting
       dagsp/
          DAGShortestPath.java # DAG shortest/longest paths
       generator/
           DatasetGenerator.java # Dataset generation
    test/java/graph/            # JUnit tests
        scc/TarjanSCCTest.java
        topo/TopologicalSortTest.java
        dagsp/DAGShortestPathTest.java
```

## Features

### 1. Strongly Connected Components (Tarjan's Algorithm)
- **Time Complexity**: O(V + E)
- **Implementation**: `graph.scc.TarjanSCC`
- **Features**:
  - Finds all SCCs in a directed graph
  - Builds condensation graph (DAG of components)
  - Tracks DFS visits, edges explored, and execution time

### 2. Topological Sort
- **Time Complexity**: O(V + E)
- **Implementation**: `graph.topo.TopologicalSort`
- **Features**:
  - Kahn's algorithm (BFS-based with in-degree tracking)
  - Alternative DFS-based implementation
  - Cycle detection (returns empty list for cyclic graphs)
  - Tracks pushes, pops, and execution time

### 3. DAG Shortest/Longest Paths
- **Time Complexity**: O(V + E)
- **Implementation**: `graph.dagsp.DAGShortestPath`
- **Features**:
  - Single-source shortest paths
  - Single-source longest paths (critical path)
  - Path reconstruction
  - Tracks edge relaxations and execution time

### Instrumentation
All algorithms implement the `Metrics` interface providing:
- Operation counters (DFS visits, edge traversals, relaxations, etc.)
- Timing via `System.nanoTime()`
- Summary reports

## Building and Running

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Build the Project
```bash
cd assignment4-graph
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Generate Datasets
```bash
mvn exec:java -Dexec.mainClass="graph.generator.DatasetGenerator"
```

This generates 9 datasets in the `data/` directory:
- **Small (6-10 nodes)**: Simple cases with 0-2 cycles
- **Medium (10-20 nodes)**: Mixed structures with multiple SCCs
- **Large (20-50 nodes)**: Performance testing with various densities

### Run Analysis on a Dataset
```bash
mvn exec:java -Dexec.mainClass="graph.Main" -Dexec.args="data/small_1_simple_dag.json"
```

Or after building:
```bash
java -cp target/classes:~/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar graph.Main data/medium_2_multiple_sccs.json
```

## Dataset Descriptions

| File | Vertices | Edges | Type | Description |
|------|----------|-------|------|-------------|
| `small_1_simple_dag.json` | 8 | 10 | DAG | Simple directed acyclic graph |
| `small_2_one_cycle.json` | 10 | 15 | Cyclic | Contains one small cycle |
| `small_3_dense_cycles.json` | 8 | 20 | Cyclic | Dense graph with multiple cycles |
| `medium_1_sparse_dag.json` | 15 | 20 | DAG | Sparse directed acyclic graph |
| `medium_2_multiple_sccs.json` | 18 | 35 | Cyclic | Multiple strongly connected components |
| `medium_3_mixed.json` | 20 | 40 | Cyclic | Mixed structure with some cycles |
| `large_1_dag.json` | 40 | 60 | DAG | Large directed acyclic graph |
| `large_2_dense.json` | 30 | 120 | Cyclic | Large dense graph with SCCs |
| `large_3_sparse_sccs.json` | 50 | 80 | Cyclic | Large sparse graph with multiple SCCs |

**Weight Model**: All datasets use edge-based weights (specified in the `weight_model` field).

## Input Format

JSON format for graph data:
```json
{
  "directed": true,
  "n": 8,
  "edges": [
    {"u": 0, "v": 1, "w": 3},
    {"u": 1, "v": 2, "w": 2}
  ],
  "source": 0,
  "weight_model": "edge"
}
```

- `directed`: Boolean indicating if graph is directed
- `n`: Number of vertices (numbered 0 to n-1)
- `edges`: Array of edges with source (`u`), destination (`v`), and weight (`w`)
- `source`: Default source vertex for path algorithms
- `weight_model`: Either "edge" (use edge weights) or "node" (use node durations)

## Algorithm Details

### Tarjan's SCC Algorithm
1. Performs DFS traversal maintaining discovery time and low-link values
2. Uses a stack to track vertices in current SCC
3. When a root node is found (id == lowLink), pops an entire SCC
4. **Condensation Graph**: Creates a DAG where each node is an SCC

### Topological Sort (Kahn's Algorithm)
1. Calculates in-degrees for all vertices
2. Starts with vertices having in-degree 0
3. Processes vertices in queue, reducing neighbors' in-degrees
4. If all vertices are processed, returns valid order; otherwise, cycle detected

### DAG Shortest/Longest Paths
1. Computes topological ordering first
2. **Shortest Path**: Initializes distances to , relaxes edges in topological order
3. **Longest Path**: Initializes distances to -, maximizes instead of minimizes
4. **Critical Path**: Finds the longest path across all source-target pairs

## Performance Analysis

### Complexity Summary
| Algorithm | Time | Space | Bottleneck |
|-----------|------|-------|------------|
| Tarjan SCC | O(V+E) | O(V) | DFS traversal |
| Topological Sort | O(V+E) | O(V) | In-degree computation + Queue ops |
| DAG Shortest Path | O(V+E) | O(V) | Topological sort + Edge relaxation |

### Expected Performance Characteristics
- **Sparse graphs** (E  V): Nearly linear performance
- **Dense graphs** (E  V): Still O(V) but with higher constants
- **Many SCCs**: Condensation reduces problem size significantly
- **Long chains**: Topological sort performs well; many relaxations in path finding

## Testing

The project includes comprehensive JUnit 5 tests covering:

### SCC Tests (`TarjanSCCTest`)
- Simple DAGs (no cycles)
- Simple cycles
- Multiple SCCs
- Disconnected components
- Condensation graph construction

### Topological Sort Tests (`TopologicalSortTest`)
- Linear DAGs
- Diamond-shaped DAGs
- Cycle detection
- Disconnected components
- Both Kahn's and DFS variants

### DAG Shortest Path Tests (`DAGShortestPathTest`)
- Shortest path computation
- Longest path computation
- Multiple routes (verification of optimality)
- Path reconstruction
- Unreachable vertices
- Critical path finding

Run all tests:
```bash
mvn test
```

## Sample Output

```
================================================================================
GRAPH ALGORITHM ANALYSIS - Assignment 4
================================================================================
Input file: data/medium_2_multiple_sccs.json

Graph properties:
  Vertices: 18
  Directed: true
  Source: 0
  Weight model: edge

--------------------------------------------------------------------------------
1. STRONGLY CONNECTED COMPONENTS (Tarjan's Algorithm)
--------------------------------------------------------------------------------
Found 5 SCCs:
  SCC 0 (size 4): [0, 1, 2, 3]
  SCC 1 (size 3): [4, 5, 6]
  SCC 2 (size 2): [7, 8]
  SCC 3 (size 1): [9]
  SCC 4 (size 8): [10, 11, 12, 13, 14, 15, 16, 17]

Metrics: DFS Visits: 18, Edges Explored: 35, SCCs Found: 5, Time: 0.234 ms

--------------------------------------------------------------------------------
2. TOPOLOGICAL SORT (on Condensation DAG)
--------------------------------------------------------------------------------
Topological order of SCCs: [0, 1, 2, 3, 4]

Metrics: Pushes: 5, Pops: 5, Time: 0.156 ms

--------------------------------------------------------------------------------
3. SHORTEST PATHS IN DAG
--------------------------------------------------------------------------------
Computing shortest paths from SCC 0...
Shortest distances from source:
  To SCC 0: 0.00
  To SCC 1: 3.00
  To SCC 2: 8.00
  To SCC 3: 12.00
  To SCC 4: 15.00

Metrics: Relaxations: 8, Time: 0.187 ms

--------------------------------------------------------------------------------
4. LONGEST PATH / CRITICAL PATH IN DAG
--------------------------------------------------------------------------------
Critical Path:
  Path (SCCs): [0, 1, 2, 4]
  Length: 25.00
  From SCC 0 to SCC 4

================================================================================
ANALYSIS COMPLETE
================================================================================
```

## Implementation Notes

### Design Decisions
1. **Graph Representation**: Adjacency list for efficient traversal
2. **Weight Model**: Edge-based (stored in Edge objects)
3. **SCC Algorithm**: Tarjan over Kosaraju for single-pass efficiency
4. **Topological Sort**: Kahn's algorithm (easier to instrument than DFS)
5. **Path Algorithms**: DP over topological order (optimal for DAGs)

### Edge Cases Handled
- Empty graphs
- Single-vertex graphs
- Disconnected components
- Self-loops (treated as SCCs)
- Unreachable vertices
- Cyclic graphs (detected in topological sort)

## Code Quality

### Documentation
- Javadoc comments on all public classes and methods
- Inline comments for complex algorithms
- README with comprehensive usage instructions

### Modularity
- Separate packages for each algorithm family
- Common interfaces (`Metrics`, `Graph`)
- Clean separation of concerns

### Testing
- 20+ unit tests covering normal and edge cases
- Test coverage for all algorithms
- Deterministic tests for reproducibility

## Conclusion

This implementation demonstrates efficient algorithms for:
1. **Dependency analysis** via SCC detection
2. **Task ordering** via topological sort
3. **Critical path analysis** via longest path in DAG

The modular design allows easy extension for:
- Alternative SCC algorithms (Kosaraju)
- Node-weighted graphs
- Additional path metrics (bottleneck paths, etc.)
- Visualization of results

## References

- Tarjan, R. (1972). "Depth-first search and linear graph algorithms"
- Kahn, A. B. (1962). "Topological sorting of large networks"
- Cormen et al. "Introduction to Algorithms" (3rd ed.), Chapters 22-24

## Author

Assignment 4 - Algorithm Course  
October 2025


