package swg.gui.schematics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import swg.crafting.SWGValues;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceFilter;
import swg.crafting.schematics.SWGSchematic;
import swg.tools.ZString;

/**
 * This type represents a unique pair of resource-class and weights. An instance
 * contains a set of the schematics that make use of its unique pair; no such
 * set is empty. The constant {@link #LQ_FILTER} is used for low-quality
 * schematics but for schematics not denotes as low-quality an instance of
 * {@link SWGWeights} is used.
 * <p>
 * This type is not thread safe but is not mutated once created; all members and
 * return values are read-only. Neither is {@link Serializable} implemented.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGRCWPair implements Comparable<SWGRCWPair> {

    /**
     * A constant low-quality filter for schematics that use any kind of quality
     * of a required resource class.
     */
    static final SWGResourceFilter LQ_FILTER =
            new SWGResourceFilter(Stat.OQ, 1);

    /**
     * The filter for this instance, either {@link #LQ_FILTER} or an instance of
     * {@link SWGWeights}. This member <b>is read-only</b>.
     */
    private final SWGValues filter;

    /**
     * The resource class for this instance.
     */
    private final SWGResourceClass resClass;

    /**
     * A list of sorted schematics which use this instance. Sort order is by
     * names of schematics and sorting is done by the schematics controller.
     * <p>
     * This list <b>is read-only</b>.
     */
    private final List<SWGSchematic> schematics;

    /**
     * Creates an instance of this type for the given arguments. Use {@code
     * null} for the weight argument to create an instance for no-quality
     * schematics, such an instance will use {@link #LQ_FILTER}. Otherwise the
     * created instance is used for high-quality schematics using the specified
     * arguments.
     * 
     * @param rc a resource class
     * @param wgs a weighed filter, or {@code null}
     * @param s an initial schematic
     */
    @SuppressWarnings("serial")
    SWGRCWPair(SWGResourceClass rc, SWGWeights wgs, SWGSchematic s) {
        if (rc == null || s == null)
            throw new NullPointerException("Null: rc=" + rc + " s=" + s);

        this.resClass = rc;
        this.filter = wgs == null
                ? LQ_FILTER
                : wgs;
        this.schematics = new ArrayList<SWGSchematic>() {
            @Override
            public boolean add(SWGSchematic e) {
                return super.contains(e)
                        ? false
                        : super.add(e);
            }
        };
        this.schematics.add(s);
    }

    @Override
    public int compareTo(SWGRCWPair o) {
        int c = resClass.compareTo(o.resClass);
        return c != 0
                ? c
                : filter.compareTo(o.filter);
    }

    @Override
    public boolean equals(Object obj) {
        // test for logical equivalence, but list of schematics may differ
        return this == obj
                || (obj instanceof SWGRCWPair
                        && this.resClass == ((SWGRCWPair) obj).resClass
                        && this.filter.equals(((SWGRCWPair) obj).filter));
    }

    /**
     * Helper method which determines of the two arguments equals this instance.
     * This method returns {@code true} if both the resource class and the
     * weights equal the corresponding members of this instance. If the
     * specified weight is {@code null} this method compares against
     * {@link #LQ_FILTER}.
     * 
     * @param rc a resource class
     * @param wgs an array of weights, or {@code null}
     * @return {@code true} if the argument equal this instance
     */
    boolean equals(SWGResourceClass rc, SWGValues wgs) {
        return resClass == rc
                && ((wgs == null && filter == LQ_FILTER)
                    || filter.equals(wgs));
    }

    /**
     * Returns the filter for this instance. The returned object is either
     * {@link #LQ_FILTER} or an instance of {@link SWGWeights}.
     * <p>
     * This member <b>is read-only</b>.
     * 
     * @return a filter or weights
     */
    SWGValues filter() {
        return filter;
    }

    @Override
    public int hashCode() {
        return 17 + 3 * resClass.hashCode() + 7 * filter.hashCode();
    }

    /**
     * Determines if this instance is for non-quality schematics. This method
     * returns {@code true} if a weighed filter is used.
     * 
     * @return {@code false} if this instance is using {@link #LQ_FILTER}
     */
    boolean isHQ() {
        return filter != LQ_FILTER;
    }

    /**
     * Returns the resource class constant for this instance.
     * 
     * @return a resource class constant
     */
    SWGResourceClass rc() {
        return resClass;
    }

    /**
     * Returns a list of the schematics that makes use of this instance; each
     * element is unique. This returned list <b>is read-only</b>.
     * 
     * @return a list of schematics
     */
    List<SWGSchematic> schematics() {
        return schematics;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGRCWPair[").app(resClass.rcName()).app(' ');
        if (filter == LQ_FILTER)
            z.app("LQ");
        else
            filter.toString(z, false);
        return z.app(" (").app(schematics.size()).app(")]").toString();
    }

    /**
     * Creates and returns a comparator for the specified resource and elements
     * of this type. The method {@code compare(SWGRCWPair, SWGRCWPair)} is implemented
     * so at that for each argument a rate for the resource is obtained from
     * {@link SWGWeights#rate(SWGResource, SWGResourceClass, boolean)}. If the
     * rates are unequal this method returns a value that indicates which of the
     * arguments that gives a better rate; if the rates are equal this method
     * returns the response from {@link SWGRCWPair#compareTo(SWGRCWPair)}.
     * 
     * @param kr a known resource, not {@code null}
     * @return a comparator
     */
    public static Comparator<SWGRCWPair> comparator(final SWGKnownResource kr) {
        return new Comparator<SWGRCWPair>() {
            @Override
            public int compare(SWGRCWPair r1, SWGRCWPair r2) {
                SWGWeights wg1 = (SWGWeights) r1.filter();
                SWGWeights wg2 = (SWGWeights) r2.filter();
                double w1 = wg1.rate(kr, r1.rc(), true);
                double w2 = wg2.rate(kr, r2.rc(), true);
                int c = Double.compare(w2, w1); // reversed order, best first
                return c != 0
                        ? c
                        : r1.compareTo(r2);
            }
        };
    }

    /**
     * Creates and returns a comparator for elements of this type. The method
     * {@code compare(SWGRCWPair, SWGRCWPair)}, for the two arguments it invokes
     * {@link SWGResourceClass#compareTo(SWGResourceClass)}.
     * 
     * @param reverse {@code true} for reverse compare
     * @return a comparator
     */
    public static Comparator<SWGRCWPair> comparatorRC(final boolean reverse) {
        return new Comparator<SWGRCWPair>() {
            @Override
            public int compare(SWGRCWPair r1, SWGRCWPair r2) {
                SWGResourceClass rc1 = r1.rc();
                SWGResourceClass rc2 = r2.rc();
                int r = rc1.compareTo(rc2);
                return reverse
                        ? -r
                        : r;
            }
        };
    }
}
