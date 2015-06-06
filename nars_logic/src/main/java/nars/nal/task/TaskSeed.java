package nars.nal.task;

import nars.Global;
import nars.Memory;
import nars.Symbols;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.DirectBudget;
import nars.nal.DefaultTruth;
import nars.nal.Sentence;
import nars.nal.Task;
import nars.nal.Truth;
import nars.nal.nal7.Tense;
import nars.nal.nal8.Operation;
import nars.nal.stamp.IStamp;
import nars.nal.stamp.Stamp;
import nars.nal.stamp.Stamper;
import nars.nal.term.Compound;

/** utility method for creating new tasks following a fluent builder pattern
 *  warning: does not correctly support parent stamps, use .stamp() to specify one
 *
 *  TODO abstract this and move this into a specialization of it called FluentTaskSeed
 * */
public class TaskSeed<T extends Compound> extends DirectBudget {

    private final T term;
    private final Memory memory;

    private char punc;
    private Tense tense;
    private IStamp<T> stamp;
    private Truth truth;
    private Task parent;
    private Operation cause;
    private String reason;



    /** if non-UNPERCEIVED, it is allowed to override the value the Stamp applied */
    private long occurrenceTime = Stamp.UNPERCEIVED;
    private Sentence parentBelief;
    private Sentence solutionBelief;

    /** creates a TaskSeed from an existing Task  */
    public TaskSeed(Memory memory, Task task) {
        this(memory, task.sentence);

        parent(task.getParentTask(), task.getParentBelief());
        solution(task.getBestSolution());
        budget(task.getBudget());

        /* NOTE: this ignores:
                task.history         */
    }


    public TaskSeed<T> truth(Truth tv) {
        this.truth = tv;
        return this;
    }

    public TaskSeed<T> budget(float p, float d, float q) {
        budgetDirect(p, d, q);
        return this;
    }

    public TaskSeed<T> budget(Budget bv) {
        return budget(bv.getPriority(), bv.getDurability(), bv.getQuality());
    }

    public TaskSeed<T> budget(Budget bv, float priMult, float durMult) {
        return budget(bv.getPriority() * priMult, bv.getDurability() * durMult, bv.getQuality());
    }

    protected boolean ensureBudget() {
        if (isBudgetValid()) return true;
        if (truth == null) return false;

        this.priority = Budget.newDefaultPriority(punc);
        this.durability = Budget.newDefaultDurability(punc);

        return true;
    }

    /** uses default budget generation and multiplies it by gain factors */
    public TaskSeed<T> budgetScaled(float priorityFactor, float durFactor) {

        //TODO maybe lift this to Budget class
        if (!ensureBudget()) {
            throw new RuntimeException("budgetScaled unable to determine original budget values");
        }


        this.priority *= priorityFactor;
        this.durability *= durFactor;
        return this;
    }


    public TaskSeed<T> truth(boolean freqAsBoolean, float conf) {
        return truth(freqAsBoolean ? 1.0f : 0.0f, conf);
    }

    public TaskSeed<T> truth(float freq, float conf) {
        return truth(freq, conf, Global.TRUTH_EPSILON);
    }

    public TaskSeed<T> truth(float freq, float conf, float epsilon) {
        this.truth = new DefaultTruth(freq, conf, epsilon);
        return this;
    }

    /** alias for judgment */
    public TaskSeed<T> belief() { return judgment(); }

    public TaskSeed<T> judgment() { this.punc = Symbols.JUDGMENT; return this;}
    public TaskSeed<T> question() { this.punc = Symbols.QUESTION; return this;}
    public TaskSeed<T> quest() { this.punc = Symbols.QUEST; return this;}
    public TaskSeed<T> goal() { this.punc = Symbols.GOAL; return this;}


    //TODO make these return the task, as the final call in the chain
    public TaskSeed<T> eternal() { this.tense = Tense.Eternal; return this;}
    public TaskSeed<T> present() { this.tense = Tense.Present; return this;}
    public TaskSeed<T> past() { this.tense = Tense.Past; return this;}
    public TaskSeed<T> future() { this.tense = Tense.Future; return this;}

    public TaskSeed<T> parent(Task task) {
        this.parent = task;
        return this;
    }

    public TaskSeed<T> parent(Sentence<?> parentBelief) {
        this.parentBelief = parentBelief;
        return this;
    }


    public TaskSeed<T> stamp(IStamp<T> s) { this.stamp = s; return this;}

    public TaskSeed<T> budget(float p, float d) {
        return budget(p, d, Float.NaN);
    }

    public TaskSeed(Memory memory, T t) {
        /** budget triple - to be valid, at least the first 2 of these must be non-NaN (unless it is a question)  */
        super();

        this.memory = memory;
        this.term = t;
    }

    public TaskSeed(Memory memory, Sentence<T> t) {
        this(memory, t.getTerm());
        this.punc = t.punctuation;
        this.truth = t.truth;
        this.stamp = t;
    }



    /** attempt to build the task, and insert into memory. returns non-null if successful */
    public Task input() {

        Task t = get();
        if (t == null) return null;

        if (memory.input(t) == 0) {
            return null;
        }

        return t;

    }

    /** attempt to build the task. returns non-null if successful */
    public Task get() {
        if (punc == 0)
            throw new RuntimeException("Punctuation must be specified before generating a default budget");

        if (stamp != null && tense!=Tense.Eternal) {
            throw new RuntimeException("both tense " + tense + " and stamp " + stamp + "; only use one to avoid any inconsistency");
        }

        if ((truth == null) && ((punc!=Symbols.QUEST) || (punc!=Symbols.QUESTION))) {
            truth = new DefaultTruth(punc);
        }

//        if (this.budget == null) {
//            //if budget not specified, use the default given the punctuation and truth
//            //TODO avoid creating a Budget instance here, it is just temporary because Task is its own Budget instance
//            this.budget = new Budget(punc, truth);
//        }

        Sentence s = new Sentence(term, punc, truth,
                stamp == null ? memory : stamp);

        if (s == null)
            return null;

        if (stamp == null && !isEternal()) {

            /* apply the Tense on its own, with respect to the creation time and memory duration */
            s.setOccurrenceTime(Stamp.getOccurrenceTime(s.getCreationTime(), tense, memory.duration()) );
        }

        if (this.occurrenceTime != Stamp.UNPERCEIVED) {
            s.setOccurrenceTime(this.occurrenceTime);
        }


        /** if q was not specified, and truth is, then we can calculate q from truthToQuality */
        if (!Float.isFinite(quality) && truth != null) {
            quality = BudgetFunctions.truthToQuality(truth);
        }

        Task t = new Task(s,
                /* Budget*/ this,
                Global.reference(getParentTask()),
                getParentBelief(),
                solutionBelief);

        if (this.cause!=null) t.setCause(cause);
        if (this.reason!=null) t.addHistory(reason);

        return t;
    }

    public Task getParentTask() {
        return parent;
    }

    public Sentence getParentBelief() {
        return parentBelief;
    }


    public TaskSeed<T> punctuation(final char punctuation) {
        this.punc = punctuation;
        return this;
    }

    public TaskSeed<T> time(long creationTime, long occurrenceTime) {
        return stamp(new Stamper(memory,creationTime, occurrenceTime));
    }

    public TaskSeed<T> cause(Operation operation) {
        this.cause = operation;
        return this;
    }

    public TaskSeed<T> reason(String reason) {
        this.reason = reason;
        return this;
    }

    public boolean isEternal() {
        if (stamp instanceof Stamper) {
            return ((Stamper)stamp).isEternal();
        }
        if (tense!=null)
            return tense!=Tense.Eternal;
        return true;
    }

    /** if a stamp exists, determine if it will be cyclic;
     *  otherwise assume that it is not. */
    public boolean isStampCyclic() {
        if (stamp!=null) return stamp.isCyclic();
        else if ((getParentTask()!=null) && (getParentBelief()==null)) {
            //if only the parent task is known (no parent belief)
            //the evidential set will be the same. so we'll compare its cyclicity
            return getParentTask().sentence.isCyclic();
        }
        return false;
    }


    public TaskSeed<T> parent(Task parentTask, Sentence<?> parentBelief) {
        return parent(parentTask).parent(parentBelief);
    }

    public TaskSeed<T> solution(Sentence<?> solutionBelief) {
        this.solutionBelief = solutionBelief;
        return this;
    }

    public TaskSeed<T> occurr(long occurenceTime) {
        this.occurrenceTime = occurenceTime;
        return this;
    }

    public boolean isGoal() { return punc == Symbols.GOAL;     }
    public boolean isJudgment() { return punc == Symbols.JUDGMENT;     }
    public boolean isQuestion() { return punc == Symbols.QUESTION;     }
    public boolean isQuest() { return punc == Symbols.QUEST;     }

    public T getTerm() {
        return term;
    }

    public Truth getTruth() {
        return truth;
    }


    public char getPunctuation() {
        return punc;
    }
}