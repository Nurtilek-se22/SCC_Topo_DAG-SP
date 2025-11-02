package graph.dagsp;

import graph.common.Graph;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class DAGShortestPathTest {
    
    @Test
    public void testShortestPath() {
        // Simple DAG: 0 --(3)--> 1 --(2)--> 2
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 3);
        graph.addEdge(1, 2, 2);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        DAGShortestPath.PathResult result = dagsp.shortestPaths(0);
        
        assertEquals(0, result.distances[0], 0.001);
        assertEquals(3, result.distances[1], 0.001);
        assertEquals(5, result.distances[2], 0.001);
    }
    
    @Test
    public void testShortestPathWithMultipleRoutes() {
        // Diamond: 0 -> {1,2} -> 3 with different weights
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 10);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        DAGShortestPath.PathResult result = dagsp.shortestPaths(0);
        
        assertEquals(0, result.distances[0], 0.001);
        assertEquals(5, result.distances[1], 0.001);
        assertEquals(1, result.distances[2], 0.001);
        assertEquals(6, result.distances[3], 0.001); // 0->1->3 is shorter
    }
    
    @Test
    public void testLongestPath() {
        // Simple DAG: 0 --(3)--> 1 --(2)--> 2
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 3);
        graph.addEdge(1, 2, 2);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        DAGShortestPath.PathResult result = dagsp.longestPaths(0);
        
        assertEquals(0, result.distances[0], 0.001);
        assertEquals(3, result.distances[1], 0.001);
        assertEquals(5, result.distances[2], 0.001);
    }
    
    @Test
    public void testLongestPathWithMultipleRoutes() {
        // Diamond with different weights
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 10);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        DAGShortestPath.PathResult result = dagsp.longestPaths(0);
        
        assertEquals(0, result.distances[0], 0.001);
        assertEquals(11, result.distances[3], 0.001); // 0->2->3 is longer
    }
    
    @Test
    public void testCriticalPath() {
        // Linear path with weights
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 5);
        graph.addEdge(1, 2, 3);
        graph.addEdge(2, 3, 2);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        DAGShortestPath.CriticalPathResult critical = dagsp.findCriticalPath();
        
        assertEquals(10.0, critical.length, 0.001);
        assertEquals(0, critical.source);
        assertEquals(3, critical.target);
        assertEquals(4, critical.path.size());
    }
    
    @Test
    public void testPathReconstruction() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 3);
        graph.addEdge(1, 2, 2);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        DAGShortestPath.PathResult result = dagsp.shortestPaths(0);
        List<Integer> path = dagsp.reconstructPath(result, 2);
        
        assertEquals(3, path.size());
        assertEquals(0, path.get(0));
        assertEquals(1, path.get(1));
        assertEquals(2, path.get(2));
    }
    
    @Test
    public void testUnreachableVertex() {
        // 0->1, 2 is disconnected
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 5);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        DAGShortestPath.PathResult result = dagsp.shortestPaths(0);
        
        assertEquals(Double.POSITIVE_INFINITY, result.distances[2]);
    }
    
    @Test
    public void testSingleVertex() {
        Graph graph = new Graph(1, true);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        DAGShortestPath.PathResult result = dagsp.shortestPaths(0);
        
        assertEquals(0, result.distances[0], 0.001);
    }
    
    @Test
    public void testMetrics() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 3);
        graph.addEdge(1, 2, 2);
        
        DAGShortestPath dagsp = new DAGShortestPath(graph);
        dagsp.shortestPaths(0);
        
        DAGShortestPath.DAGSPMetrics metrics = dagsp.getMetrics();
        
        assertTrue(metrics.relaxations > 0);
        assertTrue(metrics.getElapsedTime() > 0);
    }
}

