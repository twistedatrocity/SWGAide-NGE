package swg.crafting.resources;

import swg.crafting.SWGValues;
import swg.crafting.Stat;

/**
 * This type models a filter for resource values. Filters are supposed to have
 * values in the range [0 1000] but no further restrictions. In particular, this
 * type does not consider any caps of a resource class, just the value as such.
 * <p>
 * <b>Terminology:</b> this type is using a strict terminology, while in casual
 * speaking that is most often not the case:
 * <ul>
 * <li><b>stat:</b> a specific statistic of a resource, such as CD, OQ, etc.</li>
 * <li><b>value:</b> the value of a specific stat for a resource, such as OQ=965
 * where the value is 965</li>
 * <li><b>expected stat:</b> a stat that is required by the resource class of
 * the resource, one can also say <b>expected value</b> which is a corollary</li>
 * </ul>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGResourceFilter extends SWGValues {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -703985588660263501L;

    /**
     * Do not use the default constructor, an empty filter is invalid.
     */
    private SWGResourceFilter() {
        super();
        // disallow creation of empty filters
    }

    /**
     * Creates a resource filter based on the argument. This implementation
     * validates the argument for size and that all values are in the range [0
     * 1000].
     * 
     * @param values filter values
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException if the argument is {@code null}
     */
    public SWGResourceFilter(int[] values) {
        super(values);
    }

    /**
     * Creates a resource filter based on the arguments. This implementation
     * creates a plain filter for the specified stat with minimum value. It is
     * possible to add more stat/value pairs to the created instance.
     * 
     * @param stat a stat constant
     * @param value the value for, in the range [0 1000]
     * @throws IllegalArgumentException if validation fails
     */
    public SWGResourceFilter(Stat stat, int value) {
        this();
        set(stat, value);
    }

    /**
     * Determines if the argument has at least one <i>value</i> in union with
     * this instance. That is, this method returns {@code true} for any stat
     * <i>s</i>: {@code this.value(s) > 0 && resource.value(s) > 0 }.
     * <p>
     * This implementation may return {@code false} if expected values are
     * missing in {@code resource} and such a missing value would have met the
     * condition. This is because of the flaw in the resource's data.
     * 
     * @param resource the resource to evaluate
     * @return {@code true} if a <i>value</i> in union with this instance
     * @throws NullPointerException if the argument is {@code null}
     */
    public boolean hasMinimumOneValue(SWGResource resource) {
        SWGValues rStats = resource.stats();
        if (rStats == null)
            return false;

        for (Stat s : Stat.values())
            if (values[s.i] > 0 && rStats.value(s) > 0)
                return true;

        return false;
    }

    /**
     * Determines if the argument matches this filter. If the boolean argument
     * is {@code true} <i>all </i> values of the resource must meet this filter,
     * otherwise just <i>one </i> value is enough.
     * 
     * @param resource the resource to evaluate
     * @param all {@code true} if to match all non-zero values of this filter
     * @return {@code true} if this filter is matched
     * @throws NullPointerException if the argument is {@code null}
     */
    public boolean isBetter(SWGResource resource, boolean all) {
        SWGValues rValues = resource.stats();

        for (Stat s : Stat.values()) {
            int tv = this.value(s); // `this´ value
            if (tv <= 0) continue; // `this´ does not contribute

            int ov = rValues.value(s);
            if (all) {
                if (ov < tv)
                    return false; // if all, _all_ must be >= v
            } else if (ov >= tv)
                return true; // if ~all any s >= v is OK
        }
        return all
                ? true // came this far without returning false
                : false; // the converse
    }
}
