package nars.util;

import nars.NAR;
import nars.config.Plugins;
import nars.gui.util.DefaultGraphizer;
import nars.gui.util.NARGraph;
import org.junit.Test;

import static nars.gui.util.NARGraph.IncludeEverything;



public class TestNARGraph {
    
    @Test
    public void testGraph() {
    
        NAR n = new NAR(new Plugins());
        
        n.addInput("<a --> b>.");
        
        n.run(2);
        
        //System.out.println(n);

        
        NARGraph g = new NARGraph();
        g.add(n, IncludeEverything, new DefaultGraphizer(true,true,true,true,0,true,true));
        
        //System.out.println(g);
        
        assert(g.vertexSet().size() > 0);
        assert(g.edgeSet().size() > 0);
    }
}