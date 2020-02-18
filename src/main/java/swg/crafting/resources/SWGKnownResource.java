package swg.crafting.resources;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import swg.SWGAide;
import swg.crafting.Stat;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.swgcraft.SWGResourceManager;

/**
 * This type represents a <i>known resource</i>. This means, a virtually
 * immutable instance of a resource that has spawned at a galaxy in SWG. See
 * {@link SWGResource} for all details regarding resources, this type, and its
 * sibling {@link SWGMutableResource}.
 * <p>
 * Objects of this type should be honored as immutable, however, it is
 * implemented so that {@link SWGResourceManager} can update any of its fields.
 * It is an error to try to update a field unless via the resource manager.
 * <p>
 * To ensure that an instance of this type is truly unique, so that not several
 * instances represents the same unique resource, this type does not provide any
 * public constructor. For the same reason instances of this type must be
 * requested from {@link SWGResourceManager} which maintains the uniqueness
 * property.
 * <p>
 * Instances obtained from the resource manager can be contained in collections
 * or wrappers for persistent storage; in {@link SWGResource} see section
 * <i>Persistent Storage</i>. Instances retain their uniqueness property and
 * updates made by the resource manager is transparent.
 * <p>
 * A new instance of this type is validated to have a proper name and a
 * spawnable resource class, other fields can be supplemented by the resource
 * manager. Valid stats are all-zero but if any value is non-zero all values
 * have been validated by the resource class of the instance.
 * <p>
 * This type implements {@link Serializable} but its super-type does not. Thus
 * {@link #writeObject(ObjectOutputStream)} and
 * {@link #readObject(ObjectInputStream)} manages serialization of all
 * non-transient fields.
 * <p>
 * XXX In a future no instance of this kind must exist without a SWGCraft ID.
 * Currently only a known galaxy is required.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public final class SWGKnownResource extends SWGResource implements Serializable {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -4772672592117310173L;

    /**
     * The default constructor for this type; should not be invoked from outside
     * this class.
     */
    private SWGKnownResource() {
        // only used by getRecycled(SWGResourceClass)
    }

    /**
     * Creates an instance of this type from the specified argument. This
     * constructor must only be invoked by {@link SWGResourceManager}.
     * <p>
     * It is assumed that some properties are tested by the resource manager and
     * by the constructor of {@link SWGMutableResource}.
     * <p>
     * This implementation defensively copies and/or creates new objects from
     * the fields of the argument.
     * 
     * @param res the source instance
     * @throws IllegalArgumentException if validation failed
     * @throws NullPointerException if something required is {@code null}
     */
    SWGKnownResource(SWGResource res) {
        if (res.galaxy() == null)
            throw new NullPointerException("Galaxy is null: " + res);

        name(res.getName());
        rc(res.rc());

        // argument passed the minimal tests
        // copy fields and defensively create objects
        depleted(res.depleted());
        availabilityCopy(res, this);
        waypointCopy(res, this);
        stats(res.safeStats()); // one copying is enuff
        galaxy(res.galaxy()); // set galaxy before ID
        if (res.id() > 0)
            id(res.id());
    }

    /**
     * Returns the age of this instance, or {@link Integer#MAX_VALUE}. This
     * method returns the difference between the current time and the
     * first-available date which is when it was first reported to SWGCraft.org,
     * the returned value is true also if this resource is depleted and the
     * first-available is still valid. Otherwise this method returns
     * {@link Integer#MAX_VALUE} which denotes that the age is unknown. Time is
     * measured in seconds since January 1, 1970.
     * 
     * @return the age in seconds, or {@link Integer#MAX_VALUE}
     */
    public long age() {
        // XXX: find locations where this method can replace local computations
        long a = availableFirst().availableDate;
        if (a <= 0) return Integer.MAX_VALUE;

        long t = System.currentTimeMillis() / 1000;
        return (t - a);
    }

    /**
     * Deserialize this instance of {@link SWGKnownResource}. This method is
     * required since the super type does not implement {@link Serializable}.
     * 
     * @param ois an object input stream to read this instance from
     * @throws ClassNotFoundException if the class for call this cannot be found
     * @throws IOException if there is an I/O error
     * @serialData The seven fields in {@link SWGResource} by their declared
     *             type and in the following order: depletedDate, galaxy, name,
     *             planetAvailMap, stats, swgcraftID, type.
     */
    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {

        ois.defaultReadObject();
        deserialDepletedDate(ois.readLong());
        deserialGalaxy((SWGCGalaxy) ois.readObject());
        deserialName((String) ois.readObject());

        // This cast is OK since we know the type of planetAvailMap
        @SuppressWarnings("unchecked")
        Map<SWGPlanet, SWGPlanetAvailabilityInfo> pam =
                (Map<SWGPlanet, SWGPlanetAvailabilityInfo>) ois.readObject();
        deserialPam(pam);
        deserialStats((SWGResourceStats) ois.readObject());
        deserialID(ois.readLong());
        deserialRC((SWGResourceClass) ois.readObject());
     // XXX wrap this in a try
        try {
    		@SuppressWarnings("unchecked")
            Map<Integer, SWGWayPointInfo> way =
                    (Map<Integer, SWGWayPointInfo>) ois.readObject();
            deserialWay(way);
        }catch(Exception e) {
        	//SWGAide.printDebug(Thread.currentThread().getName(), 9, "SWGKnownResource: could not read waypoint map from objectstream");
        }
    }

    /**
     * This method adds the deserialized object to SWGAide's cache and returns
     * {@code this}. If there is an instance that logically equals {@code this}
     * the cached instance is returned.
     * 
     * @return this
     */
    private Object readResolve() {
        SWGKnownResource ret;
        // there should not be a cached instance...
        // but if there is, log it as an error and unify so the cached is used
        SWGKnownResource cached = SWGResourceCache.addDeserialized(this);
        if (cached != null) {
            SWGAide.printError("SWGKnownResource:readResolve",
                    new IllegalStateException(String.format(
                            "Cache error: this=%s %s; cached=%s %s",
                            this, Integer.toHexString(hashCode()),
                            cached, Integer.toHexString(cached.hashCode()))));
            ret = cached; // unify on the already cached instance
        } else
            ret = this;

        SWGResourceClass t = ret.rc();
        if (t.isSpaceOrRecycled()) {
            if (t.spaceOrRecycled != null)
                return t.spaceOrRecycled; // prevent multiple
            // else
            t.spaceOrRecycled = ret;
        }
        return ret;
    }

    /**
     * Serialize this instance of {@link SWGKnownResource}. This method is
     * required since the super type does not implement {@link Serializable}.
     * 
     * @param oos an object output stream to write this instance to
     * @throws IOException if there is an I/O error
     * @serialData The seven fields in {@link SWGResource} by their declared
     *             type and in the following order: depletedDate, galaxy, name,
     *             planetAvailMap, stats, swgcraftID, type.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeLong(depleted());
        oos.writeObject(galaxy());
        oos.writeObject(getName());
        oos.writeObject(serialAvailMap());
        oos.writeObject(safeStats());
        oos.writeLong(id());
        oos.writeObject(rc());
        oos.writeObject(serialWayMap());
    }

    /**
     * Helper method to {@link SWGResourceClass} which creates and returns an
     * instance of the specified recycled resource class. This factory method
     * <b>must never</b> be invoked other than by {@link SWGResourceClass}.
     * 
     * @param rc the space or recycled resource class
     * @return an instance of a space or recycled resource
     * @throws UnsupportedOperationException if rc is not space or recycled
     */
    static SWGKnownResource spaceOrRecycled(SWGResourceClass rc) {
        if (!rc.isSpaceOrRecycled())
            throw new UnsupportedOperationException(
                    "Not space or recycled type: " + rc.rcName());

        // safety, if somebody invokes this method directly
        if (rc.spaceOrRecycled != null)
            return rc.spaceOrRecycled;

        SWGKnownResource sor = new SWGKnownResource();
        sor.deserialName(rc.rcName());
        sor.deserialRC(rc);
        SWGResourceStats st = new SWGResourceStats();
        for (Stat s : Stat.values())
            if (rc.has(s)) st.set(s, rc.max(s));
        sor.deserialStats(st);

        // safety, if somebody invokes this method directly
        rc.spaceOrRecycled = sor;

        return sor;
    }
}
