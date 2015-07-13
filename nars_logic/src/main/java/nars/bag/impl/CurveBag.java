package nars.bag.impl;

import com.gs.collections.impl.map.mutable.UnifiedMap;
import nars.Global;
import nars.bag.Bag;
import nars.budget.Item;
import nars.util.CollectorMap;
import nars.util.data.sorted.SortedIndex;
import nars.util.sort.ArraySortedIndex;
import objenome.op.cas.E;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Bag which stores items, sorted, in one array.
 * Removal policy can select items by percentile via the array index.
 * A curve function maps a probabilty distribution to an index allowing the bag
 * to choose items with certain probabilities more than others.
 * <p>
 * In theory, the curve can be calculated to emulate any potential removal policy.
 * <p>
 * Insertion into the array is a O(log(n)) insertion sort, plus O(N) to shift items (unless the array is tree-like and can avoid this cost).
 * Removal is O(N) to shift items, and an additional possible O(N) if a specific item to be removed is not found at the index expected for its current priority value.
 * <p>
 * TODO make a CurveSampling interface with at least 2 implementations: Random and LinearScanning. it will use this instead of the 'boolean random' constructor argument
 */
public class CurveBag<K, E extends Item<K>> extends Bag<K, E> {

    @Deprecated final float MASS_EPSILON = 1.0e-5f;

    /**
     * mapping from key to item
     */
    public final CurveMap nameTable;

    /**
     * array of lists of items, for items on different level
     */
    public final SortedIndex<E> items;

    /**
     * defined in different bags
     */
    final int capacity;


    public final CurveSampler sampler;


    public static <E extends Item> SortedIndex<E> defaultIndex(int capacity) {
        //if (capacity < 50)            
        return new ArraySortedIndex(capacity);
        //else
        //    return new FractalSortedItemList<E>();
    }

    public CurveBag(Random rng, int capacity) {
        this(rng, capacity, new Power6BagCurve());
    }


    public CurveBag(Random rng, int capacity, BagCurve curve, SortedIndex<E> ind) {
        this(capacity, new RandomSampler(rng, curve), ind);
    }

    public CurveBag(Random rng, int capacity, BagCurve curve) {
        this(capacity, new RandomSampler(rng, curve), defaultIndex(capacity));
                
                                /*if (capacity < 128)*/
                //items = new ArraySortedItemList<>(capacity);
                /*else  {
                    //items = new FractalSortedItemList<>(capacity);
                    //items = new RedBlackSortedItemList<>(capacity);
                }*/

    }

    class CurveMap extends CollectorMap<K, E> {

        public CurveMap(Map<K, E> map) {
            super(map);
        }

        @Override
        public E remove(final K key) {
            E e = super.remove(key);

            if (Global.DEBUG && Global.DEBUG_BAG)
                CurveBag.this.size();

            return e;
        }

        @Override
        protected E removeItem(final E removed) {
            if (items.remove(removed)) {
                mass -= removed.getPriority();
                return removed;
            }
            return null;
        }

        @Override
        protected E addItem(final E i) {
            return items.insert(i);
        }
    }


    @FunctionalInterface
    public interface CurveSampler {
        /** which index to select */
        public int next(CurveBag b);
    }

    public static class RandomSampler implements CurveSampler {

        private final BagCurve curve;
        private final Random rng;

        public RandomSampler(Random rng, BagCurve curve) {
            this.curve = curve;
            this.rng = rng;
        }

        /** maps y in 0..1.0 to an index in 0..size */
        final int index(final float y, final int size) {

            if (y < 0) return 0;

            int i= (int) Math.floor(y * size);

            if (i >= size) return size-1;

            return i;

            /*if (result == size) {
                //throw new RuntimeException("Invalid removal index: " + x + " -> " + y + " " + result);
                return (size - 1);
            }*/

            //return result;

        }

        @Override
        public int next(final CurveBag b) {
            final int s = b.size();

            final float x = rng.nextFloat();
            final float y = curve.y(x);

            return index(y, s);
        }
    }

//FOR linear scanner, if re-implemented
//    /**
//     * Rate of sampling index when in non-random "scanning" removal mode.
//     * The position will be incremented/decremented by scanningRate/(numItems+1) per removal.
//     * Default scanning behavior is to start at 1.0 (highest priority) and decrement.
//     * When a value exceeds 0.0 or 1.0 it wraps to the opposite end (modulo).
//     * <p>
//     * Valid values are: -1.0 <= x <= 1.0, x!=0
//     */
//    final float scanningRate = -1.0f;


    public CurveBag(int capacity, CurveSampler sampler, SortedIndex<E> items) {
        super();
        this.capacity = capacity;
        this.sampler = sampler;


        items.clear();
        items.setCapacity(capacity);
        this.items = items;


        nameTable = new CurveMap(
                //new HashMap(capacity)
                //Global.newHashMap(capacity)
                new UnifiedMap(capacity)
                //new CuckooMap(capacity)
        );

    }


    @Override
    public final void clear() {
        items.clear();
        nameTable.clear();
    }

    /**
     * The number of items in the bag
     *
     * @return The number of items
     */
    @Override
    public int size() {

        int in = nameTable.size();

        if (Global.DEBUG) {
            int is = items.size();
            if (Math.abs(is-in) > 2) {
                System.err.println("INDEX");
                for (Object o : nameTable.values()) {
                    System.err.println(o);
                }
                System.err.println("ITEMS:");
                for (Object o : items) {
                    System.err.println(o);
                }

                throw new RuntimeException("curvebag fault");
            }

//            //test for a discrepency of +1/-1 difference between name and items
//            if ((is - in > 2) || (is - in < -2)) {
//                System.err.println(this.getClass() + " inconsistent index: items=" + is + " names=" + in);
//                /*System.out.println(nameTable);
//                System.out.println(items);
//                if (is > in) {
//                    List<E> e = new ArrayList(items);
//                    for (E f : nameTable.values())
//                        e.remove(f);
//                    System.out.println("difference: " + e);
//                }*/
//                throw new RuntimeException(this.getClass() + " inconsistent index: items=" + is + " names=" + in);
//            }
        }

        return in;
    }


    /**
     * Get the average priority of Items
     *
     * @return The average priority of Items in the bag
     */
    @Override
    public float getPriorityMean() {
        final int s = size();
        if (s == 0) {
            return 0.01f;
        }
        float f = mass() / s;
        if (f > 1.0f)
            return 1.0f;
        if (f < 0.01f)
            return 0.01f;
        return f;
    }


    /**
     * Check if an item is in the bag
     *
     * @param it An item
     * @return Whether the Item is in the Bag
     */
    @Override
    public boolean contains(final E it) {
        return nameTable.containsValue(it);
    }

    /**
     * Get an Item by key
     *
     * @param key The key of the Item
     * @return The Item with the given key
     */
    @Override
    public E get(final K key) {
        return nameTable.get(key);
    }

    @Override
    public E remove(final K key) {
        return nameTable.remove(key);
    }


    /**
     * Choose an Item according to priority distribution and take it out of the
     * Bag
     *
     * @return The selected Item, or null if this bag is empty
     */
    @Override
    public E pop() {

        if (size() == 0) return null; // empty bag
        return removeItem(sampler.next(this));

    }


    @Override
    public E peekNext() {

        if (size() == 0) return null; // empty bag
        return items.get(sampler.next(this));

    }





//    public static long fastRound(final double d) {
//        if (d > 0) {
//            return (long) (d + 0.5d);
//        } else {
//            return (long) (d - 0.5d);
//        }
//    }
//    


    @Override
    public float getMinPriority() {
        if (items.isEmpty()) return 0;
        return items.getFirst().getPriority();
    }

    @Override
    public float getMaxPriority() {
        if (items.isEmpty()) return 0;
        return items.getLast().getPriority();
    }

    /**
     * Insert an item into the itemTable, and return the overflow
     *
     * @param i The Item to put in
     * @return The overflow Item, or null if nothing displaced
     */
    @Override
    public E put(E i) {


        float newPriority = i.getPriority();

        boolean contains = nameTable.containsKey(i.name());
        if ((nameTable.size() >= capacity) && (!contains)) {
            // the bag is full

            // this item is below the bag's already minimum item, no change (return the input as the overflow)
            if (newPriority < getMinPriority()) {
                return i;
            }

            E oldItem = removeItem(0);

            if (oldItem == null)
                throw new RuntimeException("required removal but nothing removed");

            /*else {
                if (Global.DEBUG) {
                    if (oldItem.name().equals(i.name())) {
                        throw new RuntimeException(nameTable.size() + "," + items.size() + ": this oldItem should have been removed on earlier nameTable.put call: " + oldItem + ", during put(" + i + ")");
                    }
                }
            }*/


            //insert
            nameTable.put(i.name(), i);



            return oldItem;
        } else {
            E existing = nameTable.remove(i.name());
            if (existing!=null) {
                merge(i, existing);
            }
            nameTable.put(i.name(), i);
            return null;
//
//
//                //TODO check this mass calculation
//                E existingToReplace = ;, i);
//
//                return null;
//            } else /* if (!contains) */ {
//
//                E shouldNotExist = nameTable.put(i.name(), i);
//                if (Global.DEBUG) {
//                    if (shouldNotExist != null)
//                        throw new RuntimeException(i.name() + " already expected null value but " + shouldNotExist + " was there");
//                }
//                return null;
//            }
        }


    }

//
//    protected synchronized E removeItem2(final int index) {
//
//        final E selected;
//
//        selected = items.remove(index);
//        if (selected != null) {
//            E removed = nameTable.removeKey(selected.name());
//
//            if (removed == null)
//                throw new RuntimeException(this + " inconsistent index: items contained " + selected + " but had no key referencing it");
//
//            //should be the same object instance
//            if ((removed != null) && (removed != selected)) {
//                throw new RuntimeException(this + " inconsistent index: items contained " + selected + " and index referenced " + removed + " + ");
//            }
//            mass -= selected.budget.getPriority();
//        } else {
//            throw new RuntimeException(this + " items array returned null item at index " + index);
//        }
//
//        return selected;
//    }


    /**
     * Take out the first or last E in a level from the itemTable
     *
     * @param level The current level
     * @return The first Item
     */
    protected E removeItem(final int index) {

        E selected = items.remove(index);

        if (selected == null)
            throw new RuntimeException(this + " inconsistent index: items contained #" + index + " but had no key referencing it");

        //should be the same object instance
        nameTable.removeKey(selected.name(), selected.getPriority());

        return selected;
    }


    @Override
    public float mass() {
        return nameTable.mass();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return super.toString() + '{' + items.getClass().getSimpleName() + '}';
    }

    @Override
    public Set<K> keySet() {
        return nameTable.keySet();
    }

    @Override
    public Collection<E> values() {
        return nameTable.values();
    }

    @Override
    public Iterator<E> iterator() {
        return items.descendingIterator();
    }

    /**
     * Defines the focus curve.  x is a proportion between 0 and 1 (inclusive).
     * x=0 represents low priority (bottom of bag), x=1.0 represents high priority
     *
     * @param x input mappig value
     * @return
     */
    public static interface BagCurve extends Serializable {

        public float y(float x);
    }


    public static class CubicBagCurve implements BagCurve {

        @Override
        public final float y(final float x) {
            //1.0 - ((1.0-x)^2)
            // a function which has domain and range between 0..1.0 but
            //   will result in values above 0.5 more often than not.  see the curve:        
            //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLjAtKCgxLjAteCleMikiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjAsImVxIjoiMS4wLSgoMS4wLXgpXjMpIiwiY29sb3IiOiIjMDAwMDAwIn0seyJ0eXBlIjoxMDAwLCJ3aW5kb3ciOlsiLTEuMDYyODU2NzAzOTk5OTk5MiIsIjIuMzQ1MDE1Mjk2IiwiLTAuNDM2NTc0NDYzOTk5OTk5OSIsIjEuNjYwNTc3NTM2MDAwMDAwNCJdfV0-       
            float nx = 1.0f - x;
            return 1.0f - (nx * nx * nx);
        }

        @Override
        public String toString() {
            return "CubicBagCurve";
        }
    }


    public static class Power4BagCurve implements BagCurve {

        @Override
        public final float y(final float x) {
            float nx = 1.0f - x;
            float nnx = nx * nx;
            return 1.0f - (nnx * nnx);
        }

        @Override
        public String toString() {
            return "Power4BagCurve";
        }
    }

    public static class Power6BagCurve implements BagCurve {

        @Override
        public final float y(final float x) {
            float nx = 1.0f - x;
            float nnx = nx * nx;
            return 1.0f - (nnx * nnx * nnx);
        }

        @Override
        public String toString() {
            return "Power6BagCurve";
        }
    }

    /**
     * Approximates priority -> probability fairness with an exponential curve
     */
    @Deprecated
    public static class FairPriorityProbabilityCurve implements BagCurve {

        @Override
        public final float y(final float x) {
            return (float) (1f - Math.exp(-5f * x));
        }

        @Override
        public String toString() {
            return "FairPriorityProbabilityCurve";
        }

    }


    public static class QuadraticBagCurve implements BagCurve {

        @Override
        public final float y(final float x) {
            //1.0 - ((1.0-x)^2)
            // a function which has domain and range between 0..1.0 but
            //   will result in values above 0.5 more often than not.  see the curve:        
            //http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIxLjAtKCgxLjAteCleMikiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjAsImVxIjoiMS4wLSgoMS4wLXgpXjMpIiwiY29sb3IiOiIjMDAwMDAwIn0seyJ0eXBlIjoxMDAwLCJ3aW5kb3ciOlsiLTEuMDYyODU2NzAzOTk5OTk5MiIsIjIuMzQ1MDE1Mjk2IiwiLTAuNDM2NTc0NDYzOTk5OTk5OSIsIjEuNjYwNTc3NTM2MDAwMDAwNCJdfV0-       
            float nx = 1f - x;
            return 1f - (nx * nx);
        }

        @Override
        public String toString() {
            return "QuadraticBagCurve";
        }

    }

    @Override
    public void forEach(final Consumer<? super E> action) {
        items.forEach(action);
    }


}
