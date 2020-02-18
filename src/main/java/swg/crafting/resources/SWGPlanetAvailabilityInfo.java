package swg.crafting.resources;

import java.io.Serializable;
import java.util.Date;

import swg.model.SWGPlanet;
import swg.swgcraft.SWGResourceManager;
import swg.tools.ZString;

/**
 * This type is a availability record for a particular resource at an identified
 * planet; one record per planet where the resource is spawning. Records of this
 * type are retained just as long as the resource is still spawning plus a few
 * days after the resource is reported as depleted; eventually the
 * {@link SWGResourceManager} purges them.
 * <p>
 * Instances of this type are immutable, thus clients can share an instance of
 * this type between several resources while submitting a batch of new resources
 * to SWGCraft. If a record must be corrected the resource manager replaces the
 * old record with a new.
 * <p>
 * It is unsuitable to create an instance of this type for depleted resources,
 * for for resources without availability data; rather use {@link #VOID}.
 * 
 * @author Steven M. Doyle <shadow@triwizard.net>
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGPlanetAvailabilityInfo implements Serializable {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -7570127670539464302L;

    /**
     * A constant indicating missing or lapsed planet availability. This
     * constant has the dummy values:
     * 
     * <pre>
     * Planet:         Corellia  
     * Available date: -1        
     * Available by:   "n/a"     </pre>
     */
    public static final SWGPlanetAvailabilityInfo VOID =
            new SWGPlanetAvailabilityInfo(SWGPlanet.CORELLIA, -1, "n/a");

    /**
     * The username of the person who reported this resource to SWGCraft.org.
     */
    final String availableBy;

    /**
     * The date when this resource was reported as available for {@code planet},
     * or -1 if unknown. Time is measured in seconds since January 1, 1970.
     */
    final long availableDate;

    /**
     * The planet to which this information pertains.
     */
    final SWGPlanet planet;

    /**
     * Creates a new planet availability record. Only the planet is validated,
     * if the other two arguments are invalid they are set to default values.
     * 
     * @param p a planet constant
     * @param date the date for this record, or -1
     * @param user the SWGCraft username, or {@code null} or the empty string
     * @throws IllegalArgumentException if {@code planet} is {@code null}
     */
    public SWGPlanetAvailabilityInfo(SWGPlanet p, long date, String user) {
        if (p == null)
            throw new IllegalArgumentException("Planet is null");

        this.planet = p;
        this.availableBy = user != null
                ? user
                : "";
        this.availableDate = date > 0
                ? date
                : -1;
    }

    /**
     * Returns the date for when this resource was reported as available at
     * {@link #planet}, or -1 if unknown. The time is measured in seconds since
     * January 1, 1970.
     * 
     * @return the availability date, or -1
     */
    public long available() {
        return availableDate;
    }

    /**
     * Returns a SWGCraft username for the informant, or the empty string. This
     * is the player who reported the resource as available at {@link #planet}.
     * 
     * @return a SWGCraft username, or the empty string
     */
    public String availableBy() {
        return availableBy;
    }

    /**
     * Returns the constant for the planet to which this record pertains, never
     * {@code null}.
     * 
     * @return a planet constant, not {@code null}
     */
    public SWGPlanet planet() {
        return planet;
    }

    @Override
    public String toString() {
        ZString z = new ZString("Planet availability[");
        if (this == VOID)
            z.app("zilch");
        else {
            z.app(planet).app(',');
            z.app(availableBy).app(',');
            if (availableDate > 0) z.app(new Date(availableDate * 1000));
        }
        z.app(']');
        return z.toString();
    }
}
