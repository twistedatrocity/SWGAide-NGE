package swg.crafting.resources;

import java.util.Comparator;

import swg.crafting.SWGWeights;

/**
 * A comparator for resource weights. The only method returns a value that sorts
 * in reverse order, greatest value first.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGWeightComparator implements Comparator<SWGKnownResource> {

    /**
     * A resource class to derive caps from, or {@code null} to use caps from
     * the resources themselves.
     */
    private final SWGResourceClass capsFrom;

    /**
     * The weight instance for this comparator.
     */
    private final SWGWeights weights;

    /**
     * {@code true} if zero-values of the resources which are in union with the
     * specified weight should count as upper cap, {@code false} otherwise.
     */
    private final boolean zeroIsMax;

    /**
     * Creates this instance.
     * 
     * @param ws a weight object
     * @param rc a resource class constant to derive caps from, or {@code null}
     *        to use caps from the resources themselves
     * @param zim {@code true} if zero-values that are in union with the
     *        specified weight should count as upper cap (zero-is-max)
     */
    public SWGWeightComparator(SWGWeights ws, SWGResourceClass rc, boolean zim) {
        this.weights = ws;
        this.capsFrom = rc;
        this.zeroIsMax = zim;
    }

    public int compare(SWGKnownResource r1, SWGKnownResource r2) {
        double d1 = weights.rate(r1, capsFrom, zeroIsMax);
        double d2 = weights.rate(r2, capsFrom, zeroIsMax);
        return Double.compare(d2, d1);
    }
}
