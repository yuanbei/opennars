package nars.nal.meta.match;

import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.Substitution;

import java.util.Collection;

/**
 * implementation which stores its series of subterms as a Term[]
 */
public class CollectionEllipsisMatch extends EllipsisMatch<Term> {

    public final Collection<Term> term;

    public CollectionEllipsisMatch(Collection<Term> term) {
        this.term = term;
    }

    @Override
    public Term build(Term[] subterms, Compound superterm) {
        return superterm.clone(subterms);
    }

    @Override
    public boolean resolve(Substitution substitution, Collection<Term> target) {
        target.addAll(term);
        return true;
    }

    @Override
    public int size() {
        return term.size();
    }
}