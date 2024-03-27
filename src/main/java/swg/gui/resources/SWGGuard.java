package swg.gui.resources;

import java.io.Serializable;

import swg.crafting.SWGValues;
import swg.crafting.SWGWeights;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceFilter;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGui;
import swg.tools.ZString;

/**
 * This type models a guard or filter that alerts the user when resources match
 * its criteria. The term <b>filter</b> is ambiguous because this type is always
 * used to sift resources, in one way or another; the term <b>guard</b> denotes
 * that it also alerts the user about the success.
 * <p>
 * An instance of this type use either a plain or a weighed filter and for this
 * it keeps an instance of a {@link SWGResourceFilter} or a {@link SWGWeights}
 * respectively.
 * <dl>
 * <dt>plain filter</dt>
 * <dd>each resource stat must match its corresponding non-zero value of this
 * filter; a resource fails if a non-zero stats of this filter is unmatched</dd>
 * <dt>weighed filter</dt>
 * <dd>all resource stats that correspond to non-zero values of this filter are
 * weighed together based on the filter value and yields a <b>rate</b> of the
 * resource; a resource fails unless<tt> rate &ge; {@link #limit}</tt></dd>
 * </dl>
 * <p>
 * Comments for weighed guards:
 * <dl>
 * <dt>{@link #acceptNoStats}</dt>
 * <dd>for a weighed guard this flag determines if all non-zero stats must be
 * matched or if just one is enough; in SWG it suffices to fill each stat once,
 * thus, if it is possible players can often use resource classes without all
 * stat. If this flag is {@code true} and if a resource has at least one stat
 * for a non-zero filter value, all zero-values of the resource are ignored, all
 * of the remaining values are used by the filter</dd>
 * <dt>{@link #limit}</dt>
 * <dd>for a weighed guard this value is the minimum rate that a resource must
 * pass; the limit is within the range [0 1000]</dd> </dt>
 * </dl>
 * <p>
 * A few member variables have default, package-visible access modifiers. It is
 * the client that must validate these.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGGuard implements
        Serializable, Comparable<SWGGuard>, SWGGui {

    /**
     * A constant for the maximum length of optional notes for an instance.
     */
    // XXX: review and see if 512 is a good size
    static final int MAX_NOTES_LENGTH = 512;

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -4940069543342485618L;

    /**
     * If this field is {@code true} this instance should accept zero-values for
     * those stats that are set in this instance. Named zeroIsMax in other
     * locations.
     */
    boolean acceptNoStats;

    /**
     * The primary object that carries out the job for this instance, either a
     * {@link SWGResourceFilter}, or a {@link SWGWeights}.
     */
    private SWGValues guardValues;

    /**
     * The secondary logic object for this instance, or {@code null}. If this
     * member is set {@link #accept(SWGKnownResource)}, for each resource that
     * pass the primary logic it must also pass this logic which is either a
     * {@link SWGResourceFilter}, or a {@link SWGWeights}.
     */
    private SWGValues guardValues2nd;

    /**
     * The threshold for a weighted guard.
     */
    private double limit;

    /**
     * The name of this guard.
     */
    private String name;

    /**
     * A string for short notes; see {@link #MAX_NOTES_LENGTH}.
     */
    private String notes;

    /**
     * A constant for the resource class to guard or filter for.
     */
    private SWGResourceClass resourceClass;

    /**
     * A temporary variable which reads a date, or 0. Only if this guard has
     * found a resource which meets the criteria the date is set and if so the
     * date is the first-available date for the most recent resource, otherwise
     * it is 0. The value is measured in seconds since January 1, 1970.
     * <p>
     * This value is checked by the GUI table of guards and if it reads a date
     * the entry for this instance is highlighted. When there is an update to
     * the local set of spawning resources this flag must be reset, all
     * resources must be filtered yet again, and the date is again set.
     */
    transient long resourceRecentSpawnDate = 0;

    /**
     * If this field is {@code true} this guard should alert the user when there
     * is a resource which meets the criteria. otherwise not.
     */
    boolean useAlarm;

    /**
     * Creates an instance of this type for the given arguments.
     * 
     * @param name a name for the guard, not {@code null}
     * @param rc the resource class for this guard, not {@code null}
     * @param logic the object that carries out the logic, not {@code null}
     * @param threshold for a weighed guard this is its threshold within the
     *        range [0.0 1000.0, otherwise 0.0
     * @param doAlarm {@code true} if this guard should alert the user when a
     *        resource match this instance
     * @param acceptNoStats {@code true} if zero-values of the resource are
     *        accepted
     * @throws IllegalArgumentException if an argument is invalid
     * @throws NullPointerException if an argument is {@code null}
     */
    public SWGGuard(String name, SWGResourceClass rc, SWGValues logic,
            int threshold, boolean doAlarm, boolean acceptNoStats) {

        setName(name);
        rc(rc);
        threshold(threshold);
        logic(logic);
        this.useAlarm = doAlarm;
        this.acceptNoStats = acceptNoStats;
    }

    /**
     * Determines if the argument matches the criteria of this instance.
     * 
     * @param r a resource to evaluate
     * @return {@code true} if the resource matches this instance
     */
    boolean accept(SWGKnownResource r) {
        if (!resourceClass.getClass().isAssignableFrom(
                r.rc().getClass()))
            return false;
        // XXX lets not alert on resources with zero stats mmkay
        /*if (r.stats().sum() <= 0)
            return true; // this is a possible match, a stat-less resource */

        boolean ret = acceptHelper(r, guardValues);
        return (ret && guardValues2nd != null)
                ? acceptHelper(r, guardValues2nd)
                : ret;
    }

    /**
     * Helper method which is used when this instance is a plain filter. This
     * method invokes {@link SWGResourceFilter#isBetter(SWGResource, boolean)}.
     * 
     * @param r a resource to evaluate
     * @param f a filtered logic
     * @return {@code true} if the resource is equal or better than this guard
     */
    private boolean acceptFilter(SWGKnownResource r, SWGResourceFilter f) {
        return f.isBetter(r, true);
    }

    /**
     * Helper method for {@link #accept(SWGKnownResource)} that operates on the
     * specified guard.
     * 
     * @param r a resource
     * @param g a guard
     * @return {@code true} if the resource matches this instance
     */
    private boolean acceptHelper(SWGKnownResource r, SWGValues g) {
        if (!acceptNoStats && !g.hasAllValues(r))
            return false;

        return (isWeighted(g))
                ? acceptWeighted(r, (SWGWeights) g)
                : acceptFilter(r, (SWGResourceFilter) g);
    }

    /**
     * Helper method which is used when this instance is weighed. This method
     * invokes {@link SWGWeights#rate(SWGResource, SWGResourceClass, boolean)}
     * &ge; {@link #limit}.
     * 
     * @param r the resource to evaluate
     * @param w a weighed logic
     * @return {@code true} if the resource matches {@link #limit}
     */
    private boolean acceptWeighted(SWGKnownResource r, SWGWeights w) {
        boolean useJTLCap = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "optionUseJTLcaps", Boolean.FALSE)).booleanValue();

        return w.rate(r, resourceClass,
                acceptNoStats, useJTLCap) >= limit;
    }

    /**
     * Compares by {@link #resourceRecentSpawnDate} and name.
     * <p>
     * {@inheritDoc}
     */
    public int compareTo(SWGGuard o) {
        // a guard that is not triggered has date 0
        // guards are sorted by triggered-age and names
        long c = o.resourceRecentSpawnDate - this.resourceRecentSpawnDate;
        return c != 0
                ? Long.signum(c)
                : name.compareTo(o.name);
    }

    @Override
    public String getDescription() {
        ZString z = new ZString(resourceClass.rcName()).app(": ");
        guardValues.toString(z, false);
        return z.toString();
    }

    public String getName() {
        return name;
    }

    /**
     * Determines if this instance is a weighed or a plain guard.
     * 
     * @return {@code true} if this instance is a weighed guard
     */
    public boolean isWeighted() {
        return isWeighted(guardValues);
    }

    /**
     * Helper method which determines if the specified argument is a weighed or
     * a plain guard.
     * 
     * @param g a guard, weighed or filter
     * @return {@code true} if the argument is a weighed guard
     */
    private boolean isWeighted(SWGValues g) {
        return (g instanceof SWGWeights);
    }

    /**
     * Returns the filter or weighed object that carries out the workload for
     * this instance. The return value is either a {@link SWGResourceFilter} or
     * a {@link SWGWeights}.
     * 
     * @return the logic
     */
    public SWGValues logic() {
        return guardValues;
    }

    /**
     * Sets the filter or weighed object that carries out the workload for this
     * instance; either {@link SWGResourceFilter} or {@link SWGWeights}.
     * 
     * @param logic the logic for this instance
     * @throws NullPointerException if the argument is {@code null}
     */
    public void logic(SWGValues logic) {
        if (logic == null)
            throw new NullPointerException("Argument is null");

        this.guardValues = logic;
    }

    /**
     * Returns the filter or weighed object that supports the primary logic, or
     * {@code null}. If a resource pass the primary guards and if the secondary
     * logic is set then {@link #accept(SWGKnownResource)} exercises this logic
     * which is either a {@link SWGResourceFilter}, or a {@link SWGWeights}.
     * 
     * @return the logic, or {@code null}
     */
    public SWGValues logicSecondary() {
        return guardValues2nd;
    }

    /**
     * Sets the filter or weighed object that supports the primary logic, or
     * {@code null}. If a resource pass the primary guards and if the secondary
     * logic is set then {@link #accept(SWGKnownResource)} exercises this logic
     * which is either a {@link SWGResourceFilter}, or a {@link SWGWeights}. Use
     * {@code null} to turn off a secondary guard.
     * 
     * @param secondary the logic for this instance, or {@code null}
     */
    public void logicSecondary(SWGValues secondary) {
        this.guardValues2nd = secondary;
    }

    /**
     * Returns the notes for this instance, or an empty string.
     * 
     * @return the notes
     */
    public String notes() {
        return notes == null
                ? ""
                : notes;
    }

    /**
     * Sets the optional notes for this instance; if the argument is {@code
     * null} it erases any previous notes. The specified argument text is
     * truncated at {@link #MAX_NOTES_LENGTH}.
     * 
     * @param note the notes to set, or {@code null}
     */
    public void notes(String note) {
        String n = (note == null || (n = note.trim()).isEmpty())
                ? null
                : n;
        this.notes = n != null && n.length() > MAX_NOTES_LENGTH
                ? n.substring(0, MAX_NOTES_LENGTH)
                : n;
    }

    /**
     * Returns the resource class for this instance.
     * 
     * @return a resource class
     */
    public SWGResourceClass rc() {
        return resourceClass;
    }

    /**
     * Sets the resource class for this instance.
     * 
     * @param rc the resourceClass to set
     * @throws NullPointerException if the argument is {@code null}
     */
    public void rc(SWGResourceClass rc) {
        if (rc == null)
            throw new NullPointerException("Argument is null");

        this.resourceClass = rc;
    }

    /**
     * Sets the name for this guard.
     * 
     * @param nom the name for this guard
     * @throws IllegalArgumentException if the argument is the empty string
     * @throws NullPointerException if the argument is {@code null}
     */
    public void setName(String nom) {
        String n = nom;
        if ((n = nom.trim()).isEmpty())
            throw new IllegalArgumentException("Invalid argument: " + n);

        this.name = n;
    }

    /**
     * Returns the threshold for this instance, or 0 for a plain guard.
     * 
     * @return the threshold
     */
    public int threshold() {
        return isWeighted(guardValues)
                ? (int) limit
                : 0;
    }

    /**
     * Sets the threshold for this instance; the value is within the range [0
     * 1000].
     * 
     * @param threshold the threshold
     * @throws IllegalArgumentException if the argument is not in range [0 1000]
     */
    public void threshold(int threshold) {
        if (threshold < 0 || threshold > 1000)
            throw new IllegalArgumentException("Illegal argument: " + threshold);

        this.limit = threshold;
    }

    @Override
    public String toString() {
        ZString z = new ZString("ResourceGuard[").app(resourceClass.rcName());
        z.app(": ").app((isWeighted(guardValues)
                ? 'W'
                : 'F'));
        guardValues.toString(z, false);
        if (isWeighted(guardValues)) z.app(" limit=").app(limit);
        if (guardValues2nd != null) {
            z.app(' ').app((isWeighted(guardValues2nd)
                    ? 'W'
                    : 'F'));
            guardValues2nd.toString(z, false);
        }
        return z.app(']').toString();
    }
}
