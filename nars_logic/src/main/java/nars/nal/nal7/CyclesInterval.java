package nars.nal.nal7;

import nars.Op;
import nars.term.Atom;

import java.io.IOException;

/**
 * Interval represented directly as a measure of cycles encoded as an integer in some # of bits
 *
 * A virtual term which does not survive past normalization,
 * its value being collected into Sequence or Parallel intermval
 * components
 *
 * Its appearance in terms other than Sequence and Parallel
 * is meaningless.
 *
 * TODO realtime subclass which includes a number value that maps to external wall time
 */
final public class CyclesInterval extends Atom implements Interval {

    //final static int bytesPrecision = 4;

    final static CyclesInterval zero = new CyclesInterval(0);

    final int cyc;

    @Override
    public void rehash() {
        //nothing
    }

    @Override
    public final int hashCode() {
        throw new RuntimeException("N/A");
    }

    @Override
    public final int complexity() {
        throw new RuntimeException("N/A");
    }
    @Override
    public final int volume() {
        throw new RuntimeException("N/A");
    }

    public static CyclesInterval make(int numCycles) {
        if (numCycles == 0) return zero;
        return new CyclesInterval(numCycles);
    }

    protected CyclesInterval(int numCycles) {
        super((byte[]) null); //interval(numCycles, bytesPrecision));

        if (numCycles < 0)
            throw new RuntimeException("cycles must be >= 0");

        this.cyc = numCycles;
    }


//    public static CyclesInterval intervalLog(long mag) {
//        long time = Math.round( LogInterval.time(mag, 5 /* memory.duration()*/) );
//        return new CyclesInterval(time, 0);
//    }
//
//    public static byte[] interval(long numCycles, int bytesPrecision) {
//        /*switch (bytesPrecision) {
//            case 1:
//        }*/
//        return Longs.toByteArray(numCycles);
//    }

    @Override
    public final int duration() {
        return cyc;
    }


    @Override
    public final int structure() { return 0;     }


    @Override
    public final Op op() {
        return Op.INTERVAL;
    }

//    @Override
//    public final Term clone() {
//        return this; /*new CyclesInterval(cyc, duration); */
//    }

    @Override
    public final void append(Appendable output, boolean pretty) throws IOException {
        output.append('/').append(Long.toString(cyc)).append('/');
    }


    /** preferably use toCharSequence if needing a CharSequence; it avoids a duplication */
    @Override
    public StringBuilder toStringBuilder(final boolean pretty) {
        StringBuilder sb = new StringBuilder();
        try {
            append(sb, pretty);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb;
    }

    @Override
    public String toString() {
        return toStringBuilder(false).toString();
    }

//    /** filter any zero CyclesIntervals from the list and return a new one */
//    public static Term[] removeZeros(final Term[] relterms) {
//        int zeros = 0;
//        for (Term x : relterms)
//            if (x == zero)
//                zeros++;
//        Term[] t = new Term[relterms.length - zeros];
//
//        int p = 0;
//        for (Term x : relterms)
//            if (x != zero)
//                t[p++] = x;
//
//        return t;
//    }
}
