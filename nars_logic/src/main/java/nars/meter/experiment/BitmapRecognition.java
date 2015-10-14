package nars.meter.experiment;

import nars.Global;
import nars.NAR;
import nars.nal.nal2.Property;
import nars.nal.nal2.Similarity;
import nars.nal.nal4.Product;
import nars.nal.nal5.Conjunction;
import nars.nal.nal7.Temporal;
import nars.nal.nal7.Tense;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Atom;
import nars.term.Compound;
import nars.term.Statement;
import nars.term.Term;
import nars.util.java.NALObjects;

import java.util.Collection;
import java.util.List;

/**
 * Created by me on 10/13/15.
 */
public class BitmapRecognition {

    public static class TermBitmap {
        private Term id;
        int w;
        int h;
        private float[][] on;
        private Term[][] pixels;

        public TermBitmap() {

        }

        public TermBitmap(String id, int w, int h) {
            this.id = Atom.the(id);
            resize(w, h);

        }
        public final void resize(int w, int h) {
            this.w = w;
            this.h = h;

            on = new float[w][];
            pixels = new Compound[w][];
            Collection<Term> p = Global.newArrayList(w*h);
            for (int x = 0; x < w; x++) {
                on[x] = new float[h];
                pixels[x] = new Compound[h];
                for (int y = 0; y < h; y++) {
                    //on[x][y] = 0;
                    p.add(
                        pixels[x][y] = //Instance.make(
                            Product.make(Integer.toString(x), Integer.toString(y))
                            //id
                        //)
                    );
                }
            }
        }

        public void askWhich(NAR n, Term... possibilities) {
            Conjunction pixelTerms = applyAndGetPixelState(n);



            for (Term po : possibilities) {
                Statement tt = Similarity.make(
                    pixelTerms, po
                );
                Task q = n.task(tt + "?");// :|:");
                System.err.println("ASK: " + q);
                n.input(q);
            }

//            new AnswerReaction(n, q) {
//
//                @Override
//                public void onSolution(Task belief) {
//                    onSolution.accept(belief);
//                }
//            };
        }



        public Conjunction applyAndGetPixelState(NAR n) {
            List<Term> l = Global.newArrayList();
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    l.add( updatePixel(n, x, y) );
                }
            }
            return (Conjunction) Conjunction.make(l, Temporal.ORDER_CONCURRENT);
        }

        private Term updatePixel(NAR n, int x, int y) {
            Term p = pixels[x][y];
            float on = this.on[x][y];
            Compound term = Property.make(p, f(on));
            n.believe(
                term,
                Tense.Present, 1f, 0.95f /* conf */);
            return term;
        }

        private Term f(float on) {
            if (on > 0.5) {
                return Atom.the("Y");//f(on));
            }
            else {
                return Atom.the("N");
            }
        }


//        private Term f(float on) {
//            if (on > 0.5)
//                return Atom.the(id + "_on");
//            else
//                return Atom.the(id + "_off");
//        }

        public void tell(NAR n, Term similaritage) {

            Conjunction pixelTerms = applyAndGetPixelState(n);

            n.believe(
                Similarity.make(
                    pixelTerms,
                    similaritage
                ), Tense.Present, 1.0f, 0.95f
            );
        }

        public void fill(float v) {
            fill(0, 0, w, h, (x,y) -> v);
        }

        @FunctionalInterface interface PixelValue {
            float value(int px, int py);
        }

        public void fill(int x0, int y0, int x1, int y1, PixelValue p) {
            for (int x = x0; x < x1; x++)
                for (int y = y0; y< y1; y++)
                    on[x][y] = p.value(x, y);
        }

    }


    public static void main(String[] args) throws Exception {

        Global.DEBUG = true;
        int size = 2;

        NAR n = new Default();

        TermBitmap tb = new TermBitmap("i",size,size);

        tb = new NALObjects(n).build("i", tb);

        int exposureCycles = 2000;

        for (int i = 0; i < 5; i++) {
            tb.fill(0f);
            tb.tell(n, Atom.the("black"));
            n.frame(exposureCycles);

            tb.fill(1f);
            tb.tell(n, Atom.the("white"));
            n.frame(exposureCycles);
        }

        ///n.log();


        n.memory.eventAnswer.on(tt -> {
            //if (tt.getOne().isInput()) //if question is from input
                System.err.println("\tANS: " + tt.getOne() + ", " + tt.getTwo() );
        });


        for (int i = 0; i < 10; i++) {
            System.out.println(n.concepts().size());
            n.input("(--,<white <-> black>).");

            tb.fill((float)Math.random());
            tb.askWhich(n,
                    Atom.the("white"),
                    Atom.the("black")
                    //n.term("?x"),
                    //n.term("$x")
                    );
            n.frame(exposureCycles);
        }

    }
}