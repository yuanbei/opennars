package nars.nar.experimental;

import nars.*;
import nars.bag.Bag;
import nars.bag.impl.CacheBag;
import nars.bag.impl.CurveBag;
import nars.bag.impl.GuavaCacheBag;
import nars.budget.Budget;
import nars.clock.CycleClock;
import nars.concept.AbstractConcept;
import nars.concept.BeliefTable;
import nars.concept.Concept;
import nars.concept.TaskTable;
import nars.cycle.AbstractCycle;
import nars.io.DefaultPerception;
import nars.io.Perception;
import nars.io.out.TextOutput;
import nars.link.*;
import nars.nal.LogicPolicy;
import nars.nar.Default;
import nars.process.CycleProcess;
import nars.process.TaskProcess;
import nars.task.Sentence;
import nars.task.Task;
import nars.task.TaskAccumulator;
import nars.task.TaskComparator;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Iterator;
import java.util.Map;

/**
 * "Adaptive Logic And Neural Networks" Spiking continuous-time model
 * designed by TonyLo
 */
public class Alann extends NARSeed {

    public Alann() {
        setClock(new CycleClock());
    }

    public static void main(String[] args) {
        //temporary testing shell

        Global.DEBUG = true;
        Global.EXIT_ON_EXCEPTION = true;

        NAR n = new NAR(new Alann());
        TextOutput.out(n);

        n.input("<x --> y>.");
        n.input("<y --> z>.");
        n.frame(4);

    }

    @Override
    protected Memory newMemory(Param narParam, LogicPolicy policy) {
        Memory m = super.newMemory(narParam, policy);
        m.on(this); //default conceptbuilder
        return m;
    }

    @Override
    public CycleProcess newCycleProcess() {
        return new AlannCycle();
    }

    @Override
    protected CacheBag<Term, Concept> newIndex() {
        return new GuavaCacheBag<>();
    }

    @Override
    protected int getMaximumNALLevel() {
        return 8;
    }

    @Override
    public LogicPolicy getLogicPolicy() {
        return Default.newPolicy();
    }

    @Override
    public Perception newPerception() {
        return new DefaultPerception();
    }


    @Override
    public Concept newConcept(final Term t, final Budget b, final Memory m) {
        return new AlannConcept(t, b, m);
    }



    public static class AlannConcept extends AbstractConcept {

        final Map<Concept, Truth> linked = Global.newHashMap();

        public AlannConcept(Term term, Budget budget, Memory memory) {
            super(term, budget, memory);
        }

        @Override
        public Bag<Sentence, TaskLink> getTaskLinks() {
            return null;
        }

        @Override
        public Bag<TermLinkKey, TermLink> getTermLinks() {
            return null;
        }

        @Override
        public TaskLink activateTaskLink(TaskLinkBuilder taskLinkBuilder) {
            return null;
        }

        @Override
        public boolean linkTerms(Budget budgetRef, boolean b) {
            return false;
        }

        @Override
        public TermLink activateTermLink(TermLinkBuilder termLinkBuilder) {
            return null;
        }

        @Override
        public void updateTermLinks() {

        }

        @Override
        public boolean link(Task currentTask) {
            return false;
        }

        @Override
        public TermLinkBuilder getTermLinkBuilder() {
            return null;
        }

        @Override
        public TermLink nextTermLink(TaskLink taskLink) {
            return null;
        }

        @Override
        public BeliefTable getBeliefs() {
            return null;
        }

        @Override
        public BeliefTable getGoals() {
            return null;
        }

        @Override
        public TaskTable getQuestions() {
            return null;
        }

        @Override
        public TaskTable getQuests() {
            return null;
        }

        @Override
        public long getDeletionTime() {
            return 0;
        }

        @Override
        public boolean processBelief(TaskProcess nal, Task task) {
            System.out.println(this + " processBelief " + task);
            return false;
        }

        @Override
        public boolean processGoal(TaskProcess nal, Task task) {
            System.out.println(this + " processGoal " + task);
            return false;
        }

        @Override
        public Task processQuestion(TaskProcess nal, Task task) {
            System.out.println(this + " processQuestion " + task);
            return null;
        }
    }

    class AlannCycle extends AbstractCycle {

        /**
         * holds original (user-input) goals and question tasks
         */
        protected final TaskAccumulator commands = new TaskAccumulator(TaskComparator.Merging.Plus);

        /**
         * this is temporary if it can be is unified with the concept's main index
         */
        Bag<Term, Concept> concepts;

        final int MAX_CONCEPTS = 128 * 1024;

        private double conceptActivationThreshold = 0.05f; //this will be automatically tuned by a busy metric

        @Override
        public boolean accept(Task t) {
            if (t.isInput() && !t.isJudgment()) {
                commands.add(t);
            } else {
                add(t);
            }

            return false;
        }

        public double getConceptActivationThreshold() {
            return conceptActivationThreshold;
        }

        @Override
        public void reset(Memory m, Perception p) {
            super.reset(m, p);

            if (concepts == null)
                concepts = new CurveBag(memory.random, MAX_CONCEPTS);
            else
                concepts.clear();
        }

        /**
         * an input belief, or a derived task of any punctuation
         */
        protected boolean add(Task t) {
            Concept c = memory.conceptualize(t.getTerm(), t.getBudget());
            if (c == null)
                return false;

            TaskProcess.run(memory, t);

            return true;
        }

        @Override
        public int size() {
            return concepts.size();
        }

        @Override
        public void conceptPriorityHistogram(double[] bins) {
            concepts.getPriorityHistogram(bins);
        }

        @Override
        public void cycle() {
            inputNextPerception();
        }


        @Override
        public Concept conceptualize(final Term term, Budget budget, boolean createIfMissing) {
            return conceptualize(term, budget, createIfMissing, memory.time(), concepts);
        }


        @Override
        public Concept nextConcept() {
            return null;
        }

        @Override
        public boolean reprioritize(Term term, float newPriority) {
            return false;
        }

        @Override
        public Concept remove(Concept c) {
            return concepts.remove(c.getTerm());
        }

        @Override
        protected void on(Concept c) {

        }

        @Override
        protected void off(Concept c) {

        }

        @Override
        protected boolean active(Term t) {
            return concepts.get(t)!=null;
        }

        @Override
        public Iterator<Concept> iterator() {
            return concepts.iterator();
        }
    }
}
