package swg.crafting.resources;

import java.io.Serializable;

import swg.model.SWGCGalaxy;
import swg.swgcraft.SWGResourceManager;

/**
 * This type is a temporary and mutable resource instance. The purpose of dummy
 * instances is to provide clients with the means to populate and/or edit
 * resources. The sibling to this class is {@link SWGKnownResource} which
 * represents <i>known resources</i>, a type that is virtually immutable and
 * suitable for persistent storage. See {@link SWGResource} for all details
 * regarding resources, this type, and its sibling.
 * <p>
 * This type is not for persistent storage or saved in any resource collections
 * or wrapper types, hence it does not implement {@link Serializable}. Only
 * {@link SWGKnownResource} is used for storage and {@link SWGResourceManager}
 * must be used to convert between the two; in {@link SWGResource} see the
 * section <i>Persistent Storage</i>.
 * <p>
 * These rules must be followed strictly for this type:
 * <ul>
 * A dummy resource object must ...
 * <li>... be used only in the local scope where it was obtained</li>
 * <li>... be left behind for garbage collection once leaving the local scope</li>
 * <li>... never be persistently stored or added to persistently stored
 * collections</li>
 * <li>... never be used as a <i>known resource</i></li>
 * </ul>
 * <p>
 * The minimum requirements for a valid instance is a proper resource name and a
 * spawnable resource class. In this type all fields are mutable, however some
 * validation may occur.
 * <p>
 * While {@link SWGKnownResource} must be obtained from
 * {@link SWGResourceManager} this type provides public constructors that may be
 * used under the strict rules listed above.
 * <p>
 * This type is not thread safe as it assumed that its scope is limited and no
 * more than one client has access to an instance at any type.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGMutableResource extends SWGResource {

    /**
     * Creates a default, empty instance of this type.
     */
    private SWGMutableResource() {
        super(); // do not use, but if so, never ever outside this package
    }

    /**
     * Creates an instance of this type from the specified arguments.
     * <p>
     * <b>Notice:</b> by default the new instance is <i>in depleted state</i>.
     * 
     * @param name a proper resource name
     * @param rc a spawnable resource class
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException if an argument is {@code null}
     */
    public SWGMutableResource(String name, SWGResourceClass rc) {
        this();
        name(name);
        rc(rc);
    }

    /**
     * Creates an instance of this type from the specified argument.
     * <p>
     * This implementation defensively copies and/or creates new objects from
     * the fields of the argument. Only name and resource class are required.
     * 
     * @param res the instance to copy from
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException if the argument is {@code null}
     */
    public SWGMutableResource(SWGResource res) {
        this(res.getName(), res.rc());

        depleted(res.depleted());
        availabilityCopy(res, this);
        stats(res.safeStats());
        galaxy(res.galaxy()); // set galaxy before ID
        if (res.id() > 0)
            id(res.id());
    }

    //
    // The following methods make public otherwise hidden methods
    //

    @Override
    public void availability(SWGPlanetAvailabilityInfo pi) {
        super.availability(pi);
    }
    
    @Override
    public void waypointAdd(SWGWayPointInfo wi) {
        super.waypointAdd(wi);
    }

    @Override
    public void depleted(boolean isDepleted) {
        super.depleted(isDepleted);
    }

    @Override
    public void depleted(long date) {
        super.depleted(date);
    }

    @Override
    public void galaxy(SWGCGalaxy gxy) {
        super.galaxy(gxy);
    }

    @Override
    public void id(long id) {
        super.id(id);
    }

    @Override
    public void name(String name) {
        super.name(name);
    }

    @Override
    public void rc(SWGResourceClass rc) {
        super.rc(rc);
    }

    /**
     * Sets the stats for this instance. This method always perform basic
     * validation which may throw an exception. Unless all values are zero the
     * boolean argument determines if also the resource class should validate;
     * {@link SWGResourceClass#validate(int[], SWGResourceClass)}.
     * <p>
     * This implementation defensively copies the argument.
     * 
     * @param values the resource values
     * @param validate {@code true} if the argument should be fully validated
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException if the argument is {@code null}
     */
    public void stats(int[] values, boolean validate) {
        SWGResourceStats st = new SWGResourceStats(values);

        if (validate && st.sum() > 0) {
            String err = SWGResourceClass.validate(st, rc());
            if (err != null)
                throw new IllegalArgumentException(err);
        }
        // new object is safe
        super.deserialStats(st);
    }

    /**
     * Sets the stats for this instance. This method always perform basic
     * validation which may throw an exception. Unless all values are zero the
     * boolean argument determines if also the resource class should validate;
     * {@link SWGResourceClass#validate(SWGResourceStats, SWGResourceClass)}.
     * <p>
     * This implementation defensively copies the argument.
     * 
     * @param stats the resource values
     * @param validate {@code true} if the argument should be fully validated
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException if the argument is {@code null}
     */
    public void stats(SWGResourceStats stats, boolean validate) {
        this.stats(stats.safeValues(), validate);
    }
}
