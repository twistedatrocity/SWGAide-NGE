package swg.crafting;

import java.io.Serializable;
import java.util.Arrays;

import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceFilter;
import swg.crafting.resources.SWGResourceStats;
import swg.tools.ZString;

/**
 * This abstract type models the statistics for resources in SWG. This is the
 * super-type for SWGResourceStats, SWGResourceFilter and SWGWeights, types
 * which are heavily used in clients related to resources and crafting. This
 * type also provides utility and validation methods, of which some are
 * overridden by the sub-types.
 * <p>
 * <b>Terminology:</b> this class and its sub-types are using a strict
 * terminology, while in casual speaking that is most often not the case::
 * <ul>
 * <li><b>stat:</b> a specific statistic of a resource, such as CD, OQ, etc.</li>
 * <li><b>value: </b> the value of a specific stat in a resource, such as OQ=965
 * where the value is 965.</li>
 * <li><b>expected stat:</b> a stat that is required by the particular resource
 * class, one can also say <b>expected value</b> as that is a corollary.</li>
 * </ul>
 * <p>
 * <b>Notice: </b>All stats in this model are ordered alphabetically, while, for
 * some reason, SWG is most often using the order: ER, CR, CD, DR, FL, HR, MA,
 * PE, OQ, SR, UT.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public abstract class SWGValues implements Comparable<SWGValues>, Serializable {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 6811784297252925846L;

    /**
     * A constant field of zeroes to be used by EMPTY constants in sub-types.
     */
    protected static final int[] ZEROES = new int[Stat.COUNT];

    /**
     * An array for the values of the stats; each slot pertains to a stat.
     */
    protected int[] values;

    /**
     * Creates an instance for the stats of a resource, a filter, or
     * experimental weights. All values have default value zero.
     */
    protected SWGValues() {
        values = new int[Stat.COUNT]; // zero-filled by default
    }

    /**
     * Creates an instance for stats with the values read from the argument. The
     * values are for a resource, a filter, or experimental weights.
     * <p>
     * This implementation validates the argument for its length and its values,
     * see {@link #validate(int[])}.
     * 
     * @param values the proper values for the instance to create
     * @throws IllegalArgumentException if validation of the argument fails
     * @throws NullPointerException if the argument is {@code null}
     */
    protected SWGValues(int[] values) {
        validate(values);
        this.values = Arrays.copyOf(values, values.length); // defensive
    }

    /**
     *Compares the values of the argument in in-game order.
     * <p>
     *{@inheritDoc}
     */
    public int compareTo(SWGValues filter) {
        // the most common use is display at GUI, hence game order
        for (Stat s : Stat.gameOrder()) {
            if (values[s.i] != filter.values[s.i]) {
                if (values[s.i] == 0) return 1;
                if (filter.values[s.i] == 0) return -1;
                return values[s.i] - filter.values[s.i];
            }
        }
        return 0;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != getClass())
            return false;
        return Arrays.equals(this.values, ((SWGValues) obj).values);
    }

    /**
     * Determines if all values of the argument match this instance.<br/>
     * That is, for all stats <i>n</i>: {@code this.value(n) == 0 ||
     * res.value(n) > 0} must be true. This method also returns {@code false}
     * for a resource reported without stats.
     * 
     * @param res a resource to evaluate
     * @return {@code true} if all non-zero stats are met by the resource
     * @throws NullPointerException if the argument is {@code null}
     */
    public boolean hasAllValues(SWGResource res) {
        SWGResourceStats rStats = res.stats();
        for (Stat s : Stat.values())
            if (values[s.i] > 0 && rStats.values[s.i] <= 0)
                return false;

        return true;
    }

    @Override
    public final int hashCode() {
        int result = 17 * 31 + values.hashCode();
        result = result * 31 + getClass().hashCode();
        return result;
    }

    /**
     * Determines if at least one value of this instance is greater than zero.
     * 
     * @return {@code true} if a value is greater than zero
     */
    public final boolean hasValues() {
        for (int v : values)
            if (v > 0)
                return true;

        return false;
    }

    /**
     * Returns the number of stats with <tt>values<sub><i>i</i></sub> > 0</tt>.
     * 
     * @return the number of non-zero stats
     */
    public final int nonZero() {
        int iCount = 0;
        for (Stat s : Stat.values())
            if (values[s.i] > 0)
                iCount++;

        return iCount;
    }

    /**
     * Applies a value to the identified stat. Basic validation is done on both
     * arguments. This method <i>does not </i> validate for caps, that is rather
     * determined by a resource class constant; valid ranges are:
     * <ul>
     * <li>{@link SWGResourceStats}: [0 1000]</li>
     * <li>{@link SWGResourceFilter}: [0 1000]</li>
     * <li> {@link SWGWeights}: [0 100]</li>
     * </ul>
     * <p>
     * It is an error to call this method for an immutable instance, such as
     * {@link SWGResourceStats#BLANK}.
     * 
     * @param stat the stat to set
     * @param value a value for the stat
     * @throws IllegalArgumentException if the value is out of range
     * @throws NullPointerException if the stat is {@code null}
     * @throws UnsupportedOperationException if this instance is immutable
     */
    public final void set(Stat stat, int value) {
        if (values == ZEROES)
            throw new UnsupportedOperationException("Immutable object");
        try {
            validate(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(
                    "Invalid value: %s  for stat %s", e.getMessage(), stat));
        }
        values[stat.i] = value;
    }

    /**
     * Returns the sum of all values, or 0 if values are not set.
     * 
     * @return the sum of all values
     */
    public final int sum() {
        return Math.max(0, sum(values));
    }

    @Override
    public final String toString() {
        ZString z = new ZString(getClass().getSimpleName()).app('[');
        toString(z, false);
        return z.app(']').toString();
    }

    /**
     * Returns a comma separated string with <i>integer values </i> of this
     * object. The boolean argument determines if zero-values should be
     * included. The values in in-game order on the format: <br/>
     * <tt>ER, CR, CD, DR, FL, HR, MA, PE, OQ, SR, UT</tt>
     * <p>
     * To also include names of the stats, see
     * {@link #toString(ZString, boolean)}.
     * 
     * @param inclZeroes {@code true} if zero-values should be included
     * @return a string with the values of this object
     */
    public final String toString(boolean inclZeroes) {
        ZString z = new ZString();
        boolean first = true;
        for (Stat s : Stat.gameOrder())
            if (inclZeroes || values[s.i] > 0) {
                if (!first) z.app(", ");
                z.app(values[s.i]);
                first = false;
            }
        return z.toString();
    }

    /**
     * Appends space-separated non-zero values to the string with their stat
     * names. The boolean argument determines lower or upper case for the stat
     * name. The values are appended in in-game order on the format:<br/>
     * <tt>ER=123 CR=456 CD=789 ...</tt>
     * 
     * @param z a string
     * @param minuscule {@code true} for lower case stat names
     */
    public final void toString(ZString z, boolean minuscule) {
        boolean first = true;
        for (Stat s : Stat.gameOrder()) {
            if (values[s.i] > 0) {
                if (!first) z.app(' ');
                first = false;
                z.app(minuscule
                        ? s.getName()
                        : s.name()).app('=').app(values[s.i]);
            }
        }
    }

    /**
     * Validates and returns the argument. The value must be in range [0 1000],
     * except if this instance is {@link SWGWeights} when the range is [0 100].
     * 
     * @param value a value to validate
     * @return the argument as-is
     * @throws IllegalArgumentException if the argument is out of range
     */
    protected int validate(int value) {
        // overridden by SWGWeights
        return validateValue(value);
    }

    /**
     * Validates the argument to be of proper length and values within range;
     * otherwise this implementation throws an exception. The argument must be
     * of length {@link Stat#COUNT} and all values within the range [0 1000],
     * except if this instance is {@link SWGWeights} when the range is [0 100].
     * 
     * @param array the argument to validate
     * @throws IllegalArgumentException if validation of the argument failed
     * @throws NullPointerException if the argument is {@code null}
     */
    protected final void validate(int[] array) {
        validateSize(array);
        try {
            for (int v : array)
                validate(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(
                    "Invalid value: %s%n%s",
                    e.getMessage(), Arrays.toString(array)));
        }
    }

    /**
     * Returns the value for the specified stat.
     * 
     * @param stat a stat
     * @return the value
     */
    public final int value(Stat stat) {
        return values[stat.i];
    }

    /**
     * Returns a copy of the values of this instance.
     * 
     * @return the values
     */
    public final int[] values() {
        return Arrays.copyOf(values, values.length);
    }

    /**
     * Returns the sum of the argument, or -1 if the argument is {@code null}.
     * This method <i>does not </i> validate the values of the argument.
     * 
     * @param values an array of integer values
     * @return the sum of the argument, or -1
     * @throws IllegalArgumentException if the argument is not resource stats
     */
    public static int sum(int[] values) {
        if (values == null)
            return -1;
        validateSize(values);

        int sum = 0;
        for (int i : values)
            sum += i;

        return sum;
    }

    /**
     * Helper method which validates the length of the argument to be
     * {@link Stat#COUNT}.
     * 
     * @param array the array to validate
     * @throws IllegalArgumentException if validation of the argument failed
     */
    protected static void validateSize(int[] array) {
        if (array.length != Stat.COUNT)
            throw new IllegalArgumentException("Invalid size: " + array.length);
    }

    /**
     * Helper method which validates the specified value to be within [0 1000].
     * 
     * @param value a value
     * @return the argument as-is
     * @throws IllegalArgumentException if the argument is out of range
     */
    protected static int validateValue(int value) {
        if (value < 0 || value > 1000)
            throw new IllegalArgumentException(Integer.toString(value));
        return value;
    }
}
