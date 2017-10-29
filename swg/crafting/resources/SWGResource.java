package swg.crafting.resources;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import swg.crafting.resources.types.SWGOrganic;
import swg.gui.common.SWGGui;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.swgcraft.SWGResourceManager;
import swg.tools.ZString;

/**
 * This abstract type models spawnable resources in SWG. Instances of this type
 * contains data of a particular resource. This type provides methods for an
 * identified resource and for resources in general.
 * <p>
 * A valid instance of this type must have a proper name and a resource class
 * because it does not exist any resource without these two properties. At the
 * instantiation moment other properties may be unknown, later those can be
 * supplemented or updated.
 * <p>
 * <b>Known Resources</b>
 * <p>
 * It can exist only one unique instance for each <i>known resource</i>, hence,
 * known resources are fully instance-controlled by {@link SWGResourceManager}.
 * {@link SWGKnownResource} is tailored for <i>known resources&nbsp;</i> and is
 * the only that class implements {@link Serializable}; see the <i>Persistent
 * Storage&nbsp;</i> section.
 * <p>
 * The identity of a known resource is established in the following order:
 * <ul>
 * <li>if known at SWGCraft: its unique {@link #swgcraftID}</li>
 * <li>else if galaxy is known: its combination name and galaxy</li>
 * <li>else: its combination name and resource class &mdash; this may be doubled
 * at another galaxy and is also prone to errors if a resource name is
 * misspelled</li>
 * </ul>
 * <p>
 * Identifiers for known resources can be edited (supplemented or corrected),
 * thus, this type and {@link SWGResourceManager} take preventive measures to
 * correctly handle the following use cases:
 * <ul>
 * <li>a resource name is corrected &mdash; a common case</li>
 * <li>a resource class is corrected &mdash; a not so common case</li>
 * <li>a galaxy is changed &mdash; never happens; this operation is allowed
 * <u>only for known resources without ID</u> and in that case only when edited
 * from unknown to known galaxy or when correcting the value</li>
 * <li>an ID is changed &mdash; never happens; this operation is allowed <u>only
 * for resources without ID</u> and then only when it became known at SWGCraft
 * or in the process of submitting it there</li>
 * </ul>
 * <p>
 * <b>Comment:</b> changing an ID may indeed happen, then in this way: a
 * resource is submitted to SWGCraft and cached in SWGAide. For some reason it
 * is removed at SWGCraft and added again with the same name at the same galaxy
 * and thus given a new ID (users will hardly notice as only the ID changes plus
 * submit date and who submitted). If this happens while the former resource is
 * still cached in SWGAide the cached instance must be purged (or updated) to
 * the public ID, otherwise {@link SWGResourceCache#getInstance(SWGResource)} or
 * {@link SWGResourceCache#getInstance(String, SWGCGalaxy)} will return the
 * wrong instance for its name-galaxy equality. This special case is handled by
 * the resource manager.
 * <p>
 * An instance of a <i>known resource&nbsp;</i> must only be requested from
 * {@link SWGResourceManager} which will retrieve the instance from its cache,
 * from SWGCraft, or it creates a local instance. XXX In a future a <i>known
 * resource&nbsp;</i> will not be permitted without a SWGCraft ID and a galaxy,
 * however, currently that is not a requirement.
 * <p>
 * Clients must heed instances of {@link SWGKnownResource} as immutable. Known
 * resources are, however, implemented to be possible to supplement or edit, but
 * only {@link SWGResourceManager} is allowed to do that to ensure validity and
 * to maintain their uniqueness property.
 * <p>
 * <b>Persistent Storage</b>
 * <p>
 * <b>Notice:</b> in SWGAide clients <b><u>must never serialize</u></b>
 * instances of known resources on their own. Collections or individual
 * instances of known resources <b>must</b> be serialized <b>by SWGAide</b>.
 * <p>
 * SWGAide exploits the properties of <a href=
 * "http://java.sun.com/javase/6/docs/platform/serialization/spec/serial-arch.html"
 * >Java Object Serialization Specification</a> which ensures that object graphs
 * are maintained through serialization-deserialization. Hence, any "private"
 * serialization-deserialization outside the control of SWGAide injects stale
 * doubles that will severely break the uniqueness property.
 * <p>
 * However, clients may privately store any identifiers such as the unique ID,
 * or the name/galaxy pair. Then instances of known resources are requested from
 * the resource manager, using the identifiers; that is how importing an
 * inventory backup file works.
 * <p>
 * This abstract type and the sub-type {@link SWGMutableResource} <i>are
 * not&nbsp;</i> serializable, but {@link SWGKnownResource} implements
 * {@link ObjectOutputStream#writeObject(Object)} and
 * {@link ObjectInputStream#readObject()} to handle its
 * serialization-deserialization. Thus great care to preserve backward
 * compatibility must be taken if the definition of this type is changed.
 * <p>
 * <b>Dummy Resources</b>
 * <p>
 * The type {@link SWGMutableResource} can freely be used for temporary, mutable
 * instances, as opposed to known resources which are virtually immutable.
 * <p>
 * These rules must be followed strictly:
 * <ul>
 * A dummy resource object must ...
 * <li>... be used only in the local scope where it was obtained</li>
 * <li>... be left behind for garbage collection when leaving the local scope</li>
 * <li>... never be persistently stored or added to persistently stored
 * collections</li>
 * <li>... never be used as a <i>known resource</i></li>
 * </ul>
 * <p>
 * Use cases for dummy resources include but are not restricted to:
 * <ul>
 * Populating a temporary resource object with data while, for example, ...
 * <li>... realizing resources unknown at SWGCraft</li>
 * <li>... parsing user input</li>
 * <li>... parsing ISDroid reports</li>
 * <li>... parsing notes files</li>
 * <li>... parsing backup text files</li>
 * </ul>
 * <p>
 * If a dummy resource has been populated and is intended for persistent
 * storage, call {@link SWGResourceManager#getInstance(SWGMutableResource)} and
 * store <i>the returned instance</i>, but leave behind the dummy object for
 * garbage collection.
 * <p>
 * <b>Resource Classes</b>
 * <p>
 * This type uses the composition pattern and for its resource class it
 * references a {@link SWGResourceClass} constant. This constant has methods
 * that pertains to resource classes and an instance for a resource forwards
 * queries regarding resource class properties to its type instance.
 * <p>
 * <b>Historical Notes</b>
 * <p>
 * Originally this type was the abstract super type for the more than 800
 * resource type classes in the sub-package {@code ./types/}, and it also
 * contained the editable payload.
 * <p>
 * In August 2009 this type was refactored by <i>Zimoon&nbsp;</i> for the
 * following reasons: only spawnable resource classes have a payload with ID,
 * name, values for stats, etc., not the more generic parent resource classes.
 * Another and stronger reason was that a resource's class may be corrected, an
 * operation that earlier had the faulty instance replaced by a new instance
 * plus having to hunt down and update all possible references to the old
 * instance; now a resource's class can effortlessly be updated by the resource
 * manager. Because all instances of {@link SWGResourceClass} are immutable
 * singletons the general performance is improved.
 * <p>
 * As a consequence, mutating methods were moved to {@link SWGMutableResource},
 * some methods were moved and some superfluous methods were removed.
 * <p>
 * <b>Incompatibility note:</b> these changes makes clients and serialized
 * resource objects dated before 2009/8 incompatible with this new design.
 * 
 * @author Steven M. Doyle <shadow@triwizard.net>
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public abstract class SWGResource implements Comparable<SWGResource>, SWGGui {

    /**
     * The number of days after which resources can be retired. The grace period
     * for a resource begins when it is depleted.
     */
    public final static int RETIREMENT = 10;

    /**
     * The date for when this resource was marked depleted, or -1 if this
     * resource is available. The default value {@link Long#MAX_VALUE} denotes
     * that the depletion date is unknown but old. The time is measured in
     * seconds since January 1, 1970.
     * <p>
     * For known resources only the resource manager should update this field.
     * 
     * @serial long: depleted date in UTC
     */
    // The rationale for the default value is to avoid mistakes while creating
    // old resource instances. Thus, the depletion date must be set to -1 for
    // all spawning resources found in the batches downloaded from SWGCraft
    private long depletedDate = Long.MAX_VALUE;

    /**
     * A record of the planet this resource was first reported for.
     */
    private transient SWGPlanetAvailabilityInfo firstAvailable;

    /**
     * The galaxy constant for where this instance spawned; the default value is
     * {@code null}. If not {@code null} this field together with {@link #name}
     * uniquely identifies a resource in SWG.
     * <p>
     * It is an error to edit this field if {@link #swgcraftID} is set unless it
     * is to update this field from unknown to known galaxy. If a resource was
     * marked available at the wrong galaxy at SWGCraft, once the error is
     * corrected the resource entry is assigned a new swgcraftID; these IDs are
     * not recycled.
     * <p>
     * For known resources the galaxy constant is always required and only the
     * resource manager should update this field and then <b>only if
     * {@link #swgcraftID} is unknown</b>.
     * 
     * @serial galaxy constant
     */
    private SWGCGalaxy galaxy = null;

    /**
     * The proper name of this resource. All resources must have a proper name
     * and a resource class.
     * <p>
     * If {@link #swgcraftID} is unknown, this field together with
     * {@link #galaxy} uniquely identifies a resource in SWG. Should the galaxy
     * be unknown this field together with {@link #type} may identify a
     * resource, with some risk for errors.
     * <p>
     * For known resources only the resource manager should update this field.
     * 
     * @serial string: name
     */
    private String name;

    /**
     * A map of planet availability records. These are the planets where this
     * resource is/was spawning. Not many days after this resource is depleted
     * this member is set to {@code null}.
     * <p>
     * For known resources only the resource manager should update this map.
     * 
     * @serial map of planet availability
     */
    private Map<SWGPlanet, SWGPlanetAvailabilityInfo> planetAvailMap;

    /**
     * The object containing the values for the stats of this resource. The
     * resource class determines the expected stats and also the lower and upper
     * caps for the values. A resource can be reported without values, these may
     * be supplemented later.
     * <p>
     * This field is valid as {@code null} or with all values zeroed, but if any
     * value is non-zero all values must be validated <i>by the resource
     * class&nbsp;</i> of this instance; unexpected stats must be zero.
     * <p>
     * For known resources only the resource manager should update this field or
     * its referenced object.
     * 
     * @serial stats
     */
    private SWGResourceStats stats;

    /**
     * The unique SWGCraft ID for this instance. The default value is -1 which
     * denotes an unknown ID. Once it is set this ID never change.
     * <p>
     * An ID is never recycled at SWGCraft: if an error is corrected the faulty
     * entry may be removed from the database, a new entry is added and it is
     * assigned a new and unique ID.
     * <p>
     * For known resources only the resource manager should update this field.
     * 
     * @serial long: ID
     */
    private long swgcraftID = -1;

    /**
     * The resource class for this instance. This member must be a spawnable
     * resource class, not {@code null}.
     * <p>
     * If both the SWGCraft ID and the galaxy are unknown this field together
     * with {@link #name} identifies a resource, with a risk for errors.
     * <p>
     * For known resources only the resource manager should update this field.
     * 
     * @serial resource class
     */
    private SWGResourceClass type;

    /**
     * A field which a client may use in whatever fashion, no precaution is made
     * to preserve a value for some time for a particular client. A common use
     * is a {@link Boolean} or {@link String} to denote something.
     */
    public transient Object whatever;

    /**
     * The default constructor for this class, should not be called other than
     * by sub-classes.
     */
    protected SWGResource() {
        // used only by the sub-classes
    }

    /**
     * Returns a copy of the list of planets for where this resource is
     * available. If availability is unknown an empty list is returned. Not many
     * days after this resource is depleted this information is purged.
     * 
     * @return a list of planets, or an empty list
     */
    public final List<SWGPlanet> availability() {
        if (planetAvailMap == null)
            return Collections.emptyList();

        return new ArrayList<SWGPlanet>(planetAvailMap.keySet());
    }

    /**
     * Adds the argument to the availability map. If a record exists for the
     * planet of the record it is replaced.
     * 
     * @param pi a planetary record
     * @throws NullPointerException if the argument is {@code null}
     */
    void availability(SWGPlanetAvailabilityInfo pi) {
        if (pi == null)
            throw new NullPointerException("Argument is null");

        synchronized (name) { // map may be null
            if (planetAvailMap == null)
                planetAvailMap = new HashMap
                        <SWGPlanet, SWGPlanetAvailabilityInfo>();

            planetAvailMap.put(pi.planet(), pi);
            firstAvailable = null; // reset in the case pi is dated older
        }
    }

    /**
     * Returns a record of this resource's availability at the specified planet,
     * or {@code null}. If this resource is not available at the planet this
     * method returns {@code null}. Not many days after this resource is
     * depleted the availability information is purged.
     * 
     * @param p a planet constant
     * @return an availability record, or {@code null}
     */
    public final SWGPlanetAvailabilityInfo availabilityFor(SWGPlanet p) {
        return (planetAvailMap == null)
                ? null
                : planetAvailMap.get(p);
    }

    /**
     * Removes the record for the identified planet from the availability map.
     * If no record exists for the planet this method does nothing.
     * <p>
     * For known resources only the resource manager should update this state.
     * 
     * @param p the planet to remove
     */
    final void availabilityRemove(SWGPlanet p) {
        if (planetAvailMap != null && p != null) {
            synchronized (name) {
                planetAvailMap.remove(p);
            }
        }
    }

    /**
     * Returns the record for the planet where this resource was first reported,
     * or {@link SWGPlanetAvailabilityInfo#VOID} if no record is available.
     * 
     * @return an availability record, or {@link SWGPlanetAvailabilityInfo#VOID}
     */
    public final SWGPlanetAvailabilityInfo availableFirst() {
        if (firstAvailable != null)
            return firstAvailable;

        SWGPlanetAvailabilityInfo fa = SWGPlanetAvailabilityInfo.VOID;
        synchronized (name) { // map may be null at first
            for (SWGPlanet p : availability()) {
                SWGPlanetAvailabilityInfo pi = availabilityFor(p);
                if (pi.availableDate > 0) {
                    if (fa == SWGPlanetAvailabilityInfo.VOID
                            || pi.availableDate < fa.availableDate)
                        fa = pi;
                }
            }
        }
        return firstAvailable = fa;
    }

    public final int compareTo(SWGResource o) {
        int res = type.sortIndex() - o.type.sortIndex();
        return res == 0
                ? name.compareTo(o.name)
                : res;
    }

    /**
     * Returns the date for when this resource was reported as depleted, or -1
     * if the resource is spawning. The default value is {@link Long#MAX_VALUE}
     * which denotes that the depletion date is unknown but old. Time is
     * measured in seconds since January 1, 1970.
     * 
     * @return the depletion date, or -1
     */
    public final long depleted() {
        return depletedDate;
    }

    /**
     * Updates the depleted state for this resource. If the argument is {@code
     * false} this instance is revived. This method updates
     * {@link #depletedDate} conservatively.
     * <p>
     * For known resources only the resource manager should update this state.
     * 
     * @param isDepleted {@code true} if this resource is depleted
     */
    void depleted(boolean isDepleted) {
        if (!isDepleted)
            depletedDate = -1;
        else if (depletedDate < 0)
            depleted(System.currentTimeMillis() / 1000);
    }

    /**
     * Updates the depletion date for this resource. If the argument is -1 this
     * resource is marked as not depleted. To denote that the depletion date is
     * unknown but old, use the default value {@link Long#MAX_VALUE}. Time is
     * measured in seconds since January 1, 1970.
     * 
     * @param date a depletion date, or -1 or {@link Long#MAX_VALUE}
     */
    void depleted(long date) {
        if (date <= 0)
            this.depletedDate = -1;
        else if (this.depletedDate <= 0) // only update unknown depleted date
            this.depletedDate = depletionMaxAhead(date);
    }

    /**
     * Helper method which returns the max possible depletion date based on the
     * first-available date and the maximum life span for the resource class of
     * this instance. If the argument is {@link Long#MAX_VALUE} or if nothing
     * could be adjusted the argument itself is returned.
     * 
     * @param depleted a suggested depletion date
     * @return max possible depletion date
     */
    private long depletionMaxAhead(long depleted) {
        if (depleted == Long.MAX_VALUE) return depleted;

        SWGPlanetAvailabilityInfo first = availableFirst();
        if (first == SWGPlanetAvailabilityInfo.VOID) return depleted;

        long deadAtLatest = first.availableDate;
        if (type.isJTL() || type.isSub(SWGOrganic.class))
            deadAtLatest += 22 * 24 * 3600;
        else
            deadAtLatest += 11 * 24 * 3600;

        return depleted > deadAtLatest
                ? deadAtLatest
                : depleted;
    }

    /**
     * Helper method only for deserialization; sets the the depletion date.
     * 
     * @param date the depletion date
     */
    void deserialDepletedDate(long date) {
        depletedDate = date;
    }

    /**
     * Helper method only for deserialization; sets the the galaxy.
     * 
     * @param gxy the galaxy
     */
    void deserialGalaxy(SWGCGalaxy gxy) {
        galaxy = gxy;
    }

    /**
     * Helper method only for deserialization; sets the the ID.
     * 
     * @param id the id
     */
    void deserialID(long id) {
        swgcraftID = id;
    }

    /**
     * Helper method only for deserialization; sets the name.
     * 
     * @param n the name
     */
    void deserialName(String n) {
        name = n;
    }

    /**
     * Helper method only for deserialization; sets the planet availability map.
     * 
     * @param pam the map
     */
    void deserialPam(Map<SWGPlanet, SWGPlanetAvailabilityInfo> pam) {
        planetAvailMap = pam;
    }

    /**
     * Helper method only for deserialization; sets the resource class for this
     * resource.
     * 
     * @param rc the resource class constant
     */
    void deserialRC(SWGResourceClass rc) {
        type = rc;
    }

    /**
     * Helper method only for deserialization; sets the resource stats.
     * 
     * @param s the stats
     */
    void deserialStats(SWGResourceStats s) {
        stats = s;
    }

    /**
     * Returns the galaxy constant for where this resource spawned, or {@code
     * null} if the galaxy is unknown. Known resources always returns a galaxy.
     * 
     * @return a galaxy constant, or {@code null}
     */
    public final SWGCGalaxy galaxy() {
        return galaxy;
    }

    /**
     * Updates the galaxy constant for where this instance spawned. The galaxy
     * together with the name of this instance uniquely identifies a resource.
     * <p>
     * It is an error to call this method if the SWGCraft ID is already set.
     * This method returns silently if the argument is {@code null} or if the
     * galaxy and the resource ID are set.
     * <p>
     * For known resources a galaxy constant is always required and only the
     * resource manager should update this field and then only if ID is unknown
     * and galaxy is {@code null}.
     * <p>
     * <b>Notice:</b> the updater logic must first set the galaxy constant and
     * then set the resource ID.
     * 
     * @param gxy a galaxy constant
     */
    void galaxy(SWGCGalaxy gxy) {
        if (gxy == null)
            return;
        if (swgcraftID > 0 && this.galaxy != null)
            return;

        this.galaxy = gxy;
    }

    public final String getDescription() {
        return String.format("%s@%s: %s", getName(), galaxy(), rc());
    }

    public final String getName() {
        return name;
    }

    /**
     * Returns the ID that uniquely identifies this resource at SWGCraft, or -1
     * if the ID is unknown.
     * 
     * @return a unique SWGCraft ID, or -1
     */
    public final long id() {
        return swgcraftID;
    }

    /**
     * Sets the unique SWGCraft ID. The ID is an integer that uniquely
     * identifies this resource at SWGCraft and in SWGAide. If the ID is known
     * this method does nothing.
     * <p>
     * It is an error to call this method if the ID is already known; once set
     * the ID never change. An ID is never recycled at SWGCraft, if an error is
     * corrected the faulty entry may be removed from the database, a new entry
     * is added for the correct galaxy and it is assigned a new, unique ID.
     * <p>
     * For known resources only the resource manager should update the ID and
     * then only if the SWGCraft ID is unknown and in that case only if the ID
     * is updated from unknown to known.
     * <p>
     * <b>Notice:</b> an updater method must first set the galaxy constant and
     * then this unique resource ID.
     * 
     * @param id a unique ID
     * @throws IllegalArgumentException if the argument is invalid, or if the
     *         galaxy is not set before this call
     */
    void id(long id) {
        if (swgcraftID > 0)
            return; // ignore
        if (id <= 0)
            throw new IllegalArgumentException("Invalid ID: " + id);
        if (galaxy == null)
            throw new IllegalArgumentException("Set galaxy first: " + id);

        swgcraftID = id;
    }

    /**
     * Determines if this resource is available at the specified planet. This
     * method does not consider whether this instance is depleted but just if
     * the availability map contains the specified planet.
     * 
     * @param p a planet constant
     * @return {@code true} if an availability record exists
     */
    public final boolean isAvailableAt(SWGPlanet p) {
        return planetAvailMap != null && planetAvailMap.containsKey(p);
    }

    /**
     * Determines whether this resource is reported as depleted.
     * 
     * @return {@code true} if this resource is marked depleted
     */
    public final boolean isDepleted() {
        return depletedDate > 0;
    }

    /**
     * Updates the name of this resource. The name is an identifier for this
     * resource together with the galaxy and/or with the resource type. A proper
     * name has length &ge; 3. This method always capitalize the name.
     * <p>
     * For known resources only the resource manager should correct the name.
     * 
     * @param nom a proper name
     * @throws IllegalArgumentException if the argument is not valid
     * @throws NullPointerException if the argument is {@code null}
     */
    void name(String nom) {
        String n = ZString.tac(nom);
        if (n.length() < 3)
            throw new IllegalArgumentException("Illegal name: " + nom);

        this.name = n;
    }

    /**
     * Returns the resource class constant of this instance, never {@code null}.
     * 
     * @return a resource class constant
     */
    public final SWGResourceClass rc() {
        return type;
    }

    /**
     * Updates the resource class for this instance. The resource class together
     * with this instance's name identifies a resource in SWG, somewhat error
     * prone. The argument must be a spawnable resource class, not {@code null}.
     * <p>
     * For known resources only the resource manager should update the resource
     * class.
     * <p>
     * <b>Notice:</b> once the resource class is updated the client must
     * validate the stats for this instance.
     * 
     * @param rc a spawnable resource class
     * @throws IllegalArgumentException if the resource class cannot spawn
     * @throws NullPointerException if the argument is {@code null}
     */
    void rc(SWGResourceClass rc) {
        if (!rc.isSpawnable())
            throw new IllegalArgumentException("Not spawnable: " + rc);

        type = rc;
    }

    /**
     * Purge aged information from this instance. If this instance has not been
     * depleted for more than {@link #RETIREMENT} days this method does nothing.
     * 
     * @return {@code false} if this instance is not yet retired
     */
    final boolean retire() {
        if (depletedDate == Long.MAX_VALUE) // unknown deplete date
            return true;
        long now = System.currentTimeMillis() / 1000L;
        if (isDepleted() && (now - depletedDate) > (3600 * 24 * RETIREMENT)) {
            planetAvailMap = null;
            return true;
        }
        return false;
    }

    /**
     * Returns the resource stats of this instance. This method must only be
     * invoked by a caller that <i>never modifies </i> the content of the array;
     * this method remedies unwanted creation of instances and arrays.
     * 
     * @return the values of this instance
     */
    final SWGResourceStats safeStats() {
        return stats;
    }

    /**
     * Helper method only for serialization; returns the availability map.
     * 
     * @return the availability map
     */
    final Map<SWGPlanet, SWGPlanetAvailabilityInfo> serialAvailMap() {
        return planetAvailMap;
    }

    /**
     * Returns a copy of the resource stats, or {@link SWGResourceStats#BLANK}.
     * The returned object may have zero-values, but if any value is non-zero
     * all values should have been validated by the resource class of this
     * instance. If not set this method returns {@link SWGResourceStats#BLANK}.
     * <p>
     * For known resources only the resource manager should update the stats.
     * Mutable resource instances must use the methods provided by
     * {@link SWGMutableResource}.
     * 
     * @return a copy of the stats, or {@link SWGResourceStats#BLANK}
     */
    public final SWGResourceStats stats() {
        return stats == null
                ? SWGResourceStats.BLANK
                : new SWGResourceStats(stats.safeValues()); // defensively
    }

    /**
     * Sets the stats for this instance. If the argument is {@code null}
     * {@link #stats} is nullified, otherwise this implementation defensively
     * creates and sets a copy of the argument.
     * <p>
     * For known resources only the resource manager should correct the name.
     * This method does not validate the arguments versus the resource class,
     * that should be done by the resource class.
     * 
     * @param st the stats for this resource
     */
    void stats(SWGResourceStats st) {
        this.stats = (st != null && st != SWGResourceStats.BLANK)
                ? new SWGResourceStats(st.values())
                : null;
    }

    @Override
    public final String toString() {
        ZString z = new ZString();
        z.app('[').app(type.toString()).app(':').app(name);
        z.app('@').app(galaxy != null
                ? galaxy.getName()
                : "---");
        z.app('(').app(swgcraftID);
        if (this instanceof SWGMutableResource) z.app('m');
        return z.app(')').app(']').toString();
    }

    /**
     * Helper method for sub-types. This implementation defensively creates new
     * objects for the values of {@code source} and sets the map for {@code
     * target}. If the source map is {@code null} or empty, this method does
     * nothing.
     * 
     * @param source the instance to copy from
     * @param target the instance to copy to
     */
    static void availabilityCopy(SWGResource source, SWGResource target) {
        if (source.planetAvailMap == null || source.planetAvailMap.size() <= 0)
            return;

        synchronized (source.name) {
            Map<SWGPlanet, SWGPlanetAvailabilityInfo> cpy =
                    new HashMap<SWGPlanet, SWGPlanetAvailabilityInfo>();

            for (SWGPlanet p : source.planetAvailMap.keySet()) {
                SWGPlanetAvailabilityInfo pi = source.availabilityFor(p);
                cpy.put(p, new SWGPlanetAvailabilityInfo(p,
                        pi.availableDate, pi.availableBy));
            }
            target.planetAvailMap = cpy;
        }
    }
}
