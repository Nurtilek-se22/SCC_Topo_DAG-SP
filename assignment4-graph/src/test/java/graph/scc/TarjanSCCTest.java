package graph.scc;

import graph.common.Graph;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TarjanSCCTest {
    
    @Test
    public void testSimpleDAG() {
        // Simple DAG: 0 -> 1 -> 2 -> 3
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        
        TarjanSCC tarjan = new TarjanSCC(graph);
        List<List<Integer>> sccs = tarjan.findSCCs();
        
        // Each vertex should be its own SCC
        assertEquals(4, sccs.size());
        for (List<Integer> scc : sccs) {
            assertEquals(1, scc.size());
        }
    }
    
    @Test
    public void testSimpleCycle() {
        // Simple cycle: 0 -> 1 -> 2 -> 0
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        
        TarjanSCC tarjan = new TarjanSCC(graph);
        List<List<Integer>> sccs = tarjan.findSCCs();
        
        // Should be one SCC containing all 3 vertices
        assertEquals(1, sccs.size());
        assertEquals(3, sccs.get(0).size());
    }
    
    @Test
    public void testMultipleSCCs() {
        // Two cycles: (0->1->0) and (2->3->2), plus 1->2 connecting them
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 2, 1);
        
        TarjanSCC tarjan = new TarjanSCC(graph);
        List<List<Integer>> sccs = tarjan.findSCCs();
        
        // Should be 2 SCCs
        assertEquals(2, sccs.size());
        
        // Each SCC should have 2 vertices
        for (List<Integer> scc : sccs) {
            assertEquals(2, scc.size());
        }
    }
    
    @Test
    public void testDisconnectedComponents() {
        // Two separate components: 0->1 and 2->3
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(2, 3, 1);
        
        TarjanSCC tarjan = new TarjanSCC(graph);
        List<List<Integer>> sccs = tarjan.findSCCs();
        
        // Each vertex is its own SCC
        assertEquals(4, sccs.size());
    }
    
    @Test
    public void testSingleVertex() {
        Graph graph = new Graph(1, true);
        
        TarjanSCC tarjan = new TarjanSCC(graph);
        List<List<Integer>> sccs = tarjan.findSCCs();
        
        assertEquals(1, sccs.size());
        assertEquals(1, sccs.get(0).size());
        assertEquals(0, sccs.get(0).get(0));
    }
    
    @Test
    public void testCondensationGraph() {
        // Create graph with known structure
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1); // SCC: {0,1,2}
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1); // Separate vertices: {3}, {4}
        
        TarjanSCC tarjan = new TarjanSCC(graph);
        List<List<Integer>> sccs = tarjan.findSCCs();
        TarjanSCC.CondensationGraph condensation = tarjan.buildCondensation(sccs);
        
        // Should have 3 SCCs
        assertEquals(3, sccs.size());
        assertEquals(3, condensation.graph.getN());
    }
    
    @Test
    public void testMetrics() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        
        TarjanSCC tarjan = new TarjanSCC(graph);
        tarjan.findSCCs();
        
        TarjanSCC.SCCMetrics metrics = tarjan.getMetrics();
        
        // Should have visited all vertices
        assertEquals(3, metrics.dfsVisits);
        assertTrue(metrics.getElapsedTime() > 0);
        assertTrue(metrics.edgesExplored > 0);
    }
}

