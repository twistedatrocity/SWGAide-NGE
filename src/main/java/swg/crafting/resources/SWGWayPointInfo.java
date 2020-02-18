package swg.crafting.resources;

import java.io.Serializable;
import swg.swgcraft.SWGResourceManager;
import swg.tools.ZString;

/**
 * This type is a waypoint record for a particular resource. Records of this
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
 * @author Mr-Miagi
 */
public final class SWGWayPointInfo implements Serializable {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -7570127670539464302L;

    /**
     * A constant indicating missing or lapsed waypoint. This
     * constant has the dummy values:
     * 
     * <pre>
     * wid:         0          
     * wptext:   "n/a"     </pre>
     */
    //public static final SWGWayPointInfo VOID = new SWGWayPointInfo(0, "n/a");

    /**
     * The waypoint text.
     */
    final String wptext;

    /**
     * The waypoint id to which this information pertains.
     */
    final int wid;

    /**
     * Creates a new waypoint availability record. Only the id is validated,
     * if the other two arguments are invalid they are set to default values.
     * 
     * @param wid a waypoint id
     * @param wptext the waypoint text, or {@code null} or the empty string
     * @throws IllegalArgumentException if {@code wid} is {@code null}
     */
    public SWGWayPointInfo(int wid, String wptext) {
        if (wid < 1)
            throw new IllegalArgumentException("Waypoint ID is null");

        this.wid = wid;
        this.wptext = wptext != null
                ? wptext
                : "";
    }

    /**
     * Returns the complete text for the waypoint, or the empty string.
     * 
     * @return wptext, or the empty string
     */
    public String wptext() {
        return wptext;
    }

    /**
     * Returns the constant for the waypoint ID to which this record pertains, never
     * {@code null}.
     * 
     * @return a planet constant, not {@code null}
     */
    public int wid() {
        return wid;
    }

    @Override
    public String toString() {
        ZString z = new ZString("Waypoint[");
            z.app(wid).app(',');
            z.app(wptext);
        z.app(']');
        return z.toString();
    }
}
