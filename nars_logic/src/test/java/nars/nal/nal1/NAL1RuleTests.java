package nars.nal.nal1;

import nars.NAR;
import nars.meta.RuleTest;
import nars.meter.TestNAR;
import nars.nal.DerivationRules;
import nars.task.Task;
import nars.util.graph.TermLinkGraph;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 9/5/15.
 */
public class NAL1RuleTests {

    @Test
    public void testDoublePremise() {
        new RuleTest("<a --> b>.",
                "(A --> B), (B --> C), not_equal(A,C) |- (A --> C), (Truth:Deduction, Desire:Strong)") {

            @Override
            public void onRulesCreated(DerivationRules d) {
                assertEquals(3, d.size());
            }

            @Override
            protected void setupAfterTaskInput(NAR n) {
                n.stdout();
                n.input("<b --> c>.");

                new TestNAR(n).
                        mustBelieve(48, "<a-->c>", 0.9f).
                        run();
            }

            @Override
            public void onDerivations(List<Task> derivations) {
//                assertTrue(!derivations.isEmpty());
//                assertTrue(
//                        derivations.toString().contains(
//                                "<swimmer --> bird>. %1.00;0.47%"
//                        )
//                );
//                System.out.println("derived: " + derivations);
            }
        };
    }


    @Test
    public void testConversion() {
        new RuleTest("<bird --> swimmer>.",
                "(S --> P), S |- (P --> S), (Truth:Conversion)") {

            @Override
            public void onRulesCreated(DerivationRules d) {
                assertEquals(3, d.size());
            }

            @Override
            public void onDerivations(List<Task> derivations) {
                assertTrue(!derivations.isEmpty());
                assertTrue(
                        derivations.toString().contains(
                            "<swimmer --> bird>. %1.00;0.47%"
                        )
                );
                //System.out.println("derived: " + derivations);
            }
        };
    }

    @Test
    public void testBackwards() {
        /*
        (a --> b)?
        (b --> c).
            it should derive
        (a --> c)?
            from (A --> B), (B --> C), not_equal(A,C) |- (A --> C), (Truth:Deduction, Desire:Strong)  derived backward rule    (A --> C)?  (B --> C) |-  (A --> B)?
         */
        new RuleTest("(a --> b)?",
                "(A --> B), (B --> C), not_equal(A,C) |- (A --> C), (Truth:Deduction, Desire:Strong)") {

            @Override
            protected void setupAfterTaskInput(NAR n) {
                TermLinkGraph g = new TermLinkGraph(n);
                assertTrue(g.isConnected());
                //System.out.println(g.isConnected() + " " + g);
                //System.out.println(new TermLinkGraph.TermLinkTemplateGraph(n));

                List<Task> derived = new ArrayList();

                //n.stdout();
                n.input("(b-->c).");

                n.memory.eventDerived.on(t -> {
                    derived.add(t);
                });

                n.frame(1);

                g = new TermLinkGraph(n);
                assertTrue(g.isConnected());
                //System.out.println(g.isConnected() + " " + g);
                //System.out.println(new TermLinkGraph.TermLinkTemplateGraph(n));

                n.frame(1);

                g = new TermLinkGraph(n);
                assertTrue(g.isConnected());



                n.frame(4);

                //System.out.println(derived);
                assertTrue(derived.toString().contains("<a --> c>?"));

                //System.out.println(n.concept("(a-->c)").getTermLinks());
            }


            @Override
            public void onRulesCreated(DerivationRules d) {
                assertEquals(3, d.size());
            }

            @Override
            public void onDerivations(List<Task> derivations) {
            }
        };
    }



}
