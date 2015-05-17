/*
 * Operator.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.nal.nal8;

import com.google.common.collect.Lists;
import nars.Events.EXE;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.nal.Task;
import nars.nal.Truth;
import nars.nal.nal7.Tense;
import nars.nal.stamp.Stamp;
import nars.nal.term.Term;
import nars.op.io.Echo;
import nars.util.event.AbstractReaction;
import nars.util.event.EventEmitter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

/**
 * An individual operate that can be execute by the system, which can be either
 * inside NARS or outside it, in another system or device.
 * <p>
 * This is the only file to modify when registering a new operate into NARS.
 *
 * An instance of an Operator must not be shared by multiple Memory
 * since it will be associated with a particular one.  Create a separate one for each
 */
abstract public class Operator extends AbstractReaction {

    protected NAR nar;

    protected Operator() {
        this(null); }

    protected Operator(EventEmitter source) {
        super(source, false);
    }


    @Override
    public void event(Class event, Object... args) {
        if (event == getClass())
            execute((Operation)args[0]);
    }

    /** adds this class as top the list of events watched */
    @Override
    public Class[] getEvents() {
        return ArrayUtils.addAll(super.getEvents(), getClass());
    }

    public boolean setEnabled(final NAR n, final boolean enabled) {
        if (enabled)
            this.nar = n;
        else
            this.nar = null;
        return true;
    }

    public Memory getMemory() {
        return nar.memory;
    }

    /**
     * Required method for every operate, specifying the corresponding
     * operation
     *
     * @param args Arguments of the operation, both input (constant) and output (variable)
     * @return The direct collectable results and feedback of the
     * reportExecution
     */
    protected abstract List<Task> execute(Operation operation);


    @Override
    public Operator clone() {
        //do not clone operators, just use as-is since it's effectively immutable
        return this;
    }


    /*
    <patham9_> when a goal task is processed, the following happens: In order to decide on whether it is relevant for the current situation, at first it is projected to the current time, then it is revised with previous "desires", then it is checked to what extent this projected revised desire is already fullfilled (which revises its budget) , if its below satisfaction threshold then it is pursued, if its an operation it is additionally checked if
    <patham9_> executed
    <patham9_> the consequences of this, to give examples, are a lot:
    <patham9_> 1 the system wont execute something if it has a strong objection against it. (example: it wont jump down again 5 meters if it previously observed that this damages it, no matter if it thinks about that situation again or not)
    <patham9_> 2. the system wont lose time with thoughts about already satisfied goals (due to budget shrinking proportional to satisfaction)
    <patham9_> 3. the system wont execute and pursue what is already satisfied
    <patham9_> 4. the system wont try to execute and pursue things in the current moment which are "sheduled" to be in the future.
    <patham9_> 5. the system wont pursue a goal it already pursued for the same reason (due to revision, it is related to 1)
    */
    public interface Executable {

        public boolean decide(final Operation op, final Memory memory);

    }

    /** do-nothing if executed, defult for any Atom */
    public static class NullExecutable implements Executable {

        @Override
        public boolean decide(Operation op, Memory memory) {
            return false;
        }
    }

    abstract public static class InvokeExecutable implements Executable {

        abstract public List<Task> execute(Operation op);


        /**
         * The standard way to carry out an operation, which invokes the execute
         * method defined for the operate, and handles feedback tasks as input
         *
         * @param op The operate to be executed
         * @param memory The memory on which the operation is executed
         * @return true if successful, false if an error occurred
         */
        public final boolean decide(final Operation op, final Memory memory) {

            if(!op.isExecutable(memory)) {
                return false;
            }

            final Term[] args = op.getArguments().term;

            List<Task> feedback;
            try {
                feedback = execute(op);
            }
            catch (Exception e) {
                feedback = Lists.newArrayList(new Echo(getClass(), e.toString()).newTask());
                e.printStackTrace();
            }

            //Display a message in the output stream to indicate the reportExecution of an operation
            memory.emit(EXE.class, new ExecutionResult(op, feedback));


            //internal notice of the execution
            if (!isImmediate()) {
                //TODO extract to its own method
                executedTask(op, new Truth.DefaultTruth(1f, Global.OPERATOR_EXECUTION_CONFIDENCE), memory);
            }

            //feedback tasks as input
            //should we allow immediate tasks to create feedback?
            if (feedback!=null) {
                for (final Task t : feedback) {
                    if (t == null) continue;
                    t.setCause(op);
                    t.addHistory("Feedback");

                    memory.input(t);
                }
            }

            return true;

        }

        /** Immediate operators are processed immediately and do not enter the reasoner's memory */
        public boolean isImmediate() { return false; }


        public boolean isExecutable(final Memory mem) {
            return true;
        }

        /**
         * ExecutedTask called in Operator.call
         *
         * @param operation The operation just executed
         */
        protected void executedTask(final Operation operation, Truth truth, final Memory memory) {
            final Task opTask = operation.getTask();
            memory.logic.TASK_EXECUTED.hit();

            memory.taskAdd(
                    memory.newTask(operation).
                            judgment().
                            truth(truth).
                            budget(operation.getTask()).
                            stamp(new Stamp(opTask.getStamp(), memory, Tense.Present)).
                            parent(opTask).
                            get().
                            setCause(operation),
                    "Executed");
        }
    }


//    public static String addPrefixIfMissing(String opName) {
//        if (!opName.startsWith("^"))
//            return '^' + opName;
//        return opName;
//    }





}

//        catch (NegativeFeedback n) {
//
//            if (n.freqOcurred >=0 && n.confidenceOcurred >= 0) {
//                memory.executedTask(operation, new TruthValue(n.freqOcurred, n.confidenceOcurred));
//            }
//
//            if (n.freqCorrection >= 0 && n.confCorrection >=0) {
//                //for inputting an inversely frequent goal to counteract a repeat invocation
//                BudgetValue b = operation.getTask().budget;
//                float priority = b.getPriority();
//                float durability = b.getDurability();
//
//                memory.addNewTask(
//                        memory.newTaskAt(operation, Symbols.GOAL, n.freqCorrection, n.confCorrection, priority, durability, (Tense)null),
//                        "Negative feedback"
//                );
//
//            }
//
//            if (!n.quiet) {
//                reportExecution(operation, args, n, memory);
//            }
//        }


//    public static class NegativeFeedback extends RuntimeException {
//
//        /** convenience method for creating a "never again" negative feedback"*/
//        public static NegativeFeedback never(String rule, boolean quiet) {
//            return new NegativeFeedback(rule, 0, executionConfidence,
//                    0, executionConfidence, quiet
//            );
//        }
//        /** convenience method for ignoring an invalid operation; does not recognize that it occurred, and does not report anything*/
//        public static NegativeFeedback ignore(String rule) {
//            return new NegativeFeedback(rule, -1, -1, -1, -1, true);
//        }
//
//        public final float freqCorrection;
//        public final float confCorrection;
//        public final float freqOcurred;
//        public final float confidenceOcurred;
//        public final boolean quiet;
//
//        public NegativeFeedback(String rule, float freqOcurred, float confidenceOccurred, float freqCorrection, float confCorrection, boolean quiet) {
//            super(rule);
//            this.freqOcurred = freqOcurred;
//            this.confidenceOcurred = confidenceOccurred;
//            this.freqCorrection = freqCorrection;
//            this.confCorrection = confCorrection;
//            this.quiet = quiet;
//        }
//    }
