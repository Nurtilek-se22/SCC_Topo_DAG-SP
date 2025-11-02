package graph.topo;

import graph.common.Graph;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TopologicalSortTest {
    
    @Test
    public void testSimpleDAG() {
        // Linear DAG: 0 -> 1 -> 2 -> 3
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        
        TopologicalSort topo = new TopologicalSort(graph);
        List<Integer> order = topo.sort();
        
        assertEquals(4, order.size());
        // Should be in order: 0, 1, 2, 3
        assertEquals(0, order.get(0));
        assertEquals(3, order.get(3));
        
        // Verify ordering property
        assertTrue(order.indexOf(0) < order.indexOf(1));
        assertTrue(order.indexOf(1) < order.indexOf(2));
        assertTrue(order.indexOf(2) < order.indexOf(3));
    }
    
    @Test
    public void testDAGWithMultiplePaths() {
        // Diamond shape: 0 -> {1,2} -> 3
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 1);
        
        TopologicalSort topo = new TopologicalSort(graph);
        List<Integer> order = topo.sort();
        
        assertEquals(4, order.size());
        
        // 0 must come before 1 and 2
        assertTrue(order.indexOf(0) < order.indexOf(1));
        assertTrue(order.indexOf(0) < order.indexOf(2));
        
        // Both 1 and 2 must come before 3
        assertTrue(order.indexOf(1) < order.indexOf(3));
        assertTrue(order.indexOf(2) < order.indexOf(3));
    }
    
    @Test
    public void testCycleDetection() {
        // Graph with cycle: 0 -> 1 -> 2 -> 0
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        
        TopologicalSort topo = new TopologicalSort(graph);
        List<Integer> order = topo.sort();
        
        // Should return empty list for cyclic graph
        assertTrue(order.isEmpty());
    }
    
    @Test
    public void testSingleVertex() {
        Graph graph = new Graph(1, true);
        
        TopologicalSort topo = new TopologicalSort(graph);
        List<Integer> order = topo.sort();
        
        assertEquals(1, order.size());
        assertEquals(0, order.get(0));
    }
    
    @Test
    public void testDisconnectedDAG() {
        // Two separate chains: 0->1 and 2->3
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(2, 3, 1);
        
        TopologicalSort topo = new TopologicalSort(graph);
        List<Integer> order = topo.sort();
        
        assertEquals(4, order.size());
        assertTrue(order.indexOf(0) < order.indexOf(1));
        assertTrue(order.indexOf(2) < order.indexOf(3));
    }
    
    @Test
    public void testDFSVariant() {
        // Test DFS-based topological sort
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        
        TopologicalSort topo = new TopologicalSort(graph);
        List<Integer> order = topo.sortDFS();
        
        assertEquals(4, order.size());
        assertTrue(order.indexOf(0) < order.indexOf(1));
        assertTrue(order.indexOf(1) < order.indexOf(2));
        assertTrue(order.indexOf(2) < order.indexOf(3));
    }
    
    @Test
    public void testMetrics() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        
        TopologicalSort topo = new TopologicalSort(graph);
        topo.sort();
        
        TopologicalSort.TopoMetrics metrics = topo.getMetrics();
        
        assertTrue(metrics.pushes > 0);
        assertTrue(metrics.pops > 0);
        assertTrue(metrics.getElapsedTime() > 0);
    }
}

