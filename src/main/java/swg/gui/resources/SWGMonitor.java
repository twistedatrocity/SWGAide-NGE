package swg.gui.resources;

import java.io.Serializable;

import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResourceMgr;
import swg.tools.ZString;

/**
 * This type monitors a specified resource until it is depleted. Monitors are
 * maintained per galaxy by {@link SWGResController}. Regularly a monitor is
 * invoked to determine if the particular resource is marked as depleted or not.
 * This type takes advantage of {@link SWGResourceMgr} which marks resources as
 * depleted if the resource collection downloaded from SWGCraft does not contain
 * them any longer.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGMonitor implements Serializable {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 4642000269573174330L;

    /**
     * The date this monitor was created, in seconds since January 1, 1970 (Unix
     * time).
     */
    private final long monitorStarted;
    
    /**
     * The monitored resource.
     */
    private final SWGKnownResource resource;

    /**
     * A string for arbitrary user notes, or {@code null}.
     */
    private String userNotes;

    /**
     * Creates a monitor for {@code resource}.
     * 
     * @param resource the resource to monitor
     * @throws IllegalArgumentException if the resource is unknown at SWGCraft,
     *         hence impossible to monitor
     * @throws NullPointerException if the argument is {@code null}
     */
    SWGMonitor(SWGKnownResource resource) {
        if (resource.id() <= 0) {
            throw new IllegalArgumentException("Unknown at SWGCraft: "
                + resource.getName());
        }
        this.resource = resource;
        monitorStarted = (System.currentTimeMillis() / 1000);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SWGMonitor) {
            SWGMonitor o = (SWGMonitor) obj;
            return resource == o.resource;
        }
        return false;
    }

    /**
     * Returns the date this monitor was created, in seconds since January 1,
     * 1970 (Unix time).
     * 
     * @return the date this monitor was created, in Unix time
     */
    long getMonitorStarted() {
        return monitorStarted;
    }

    /**
     * Returns the monitored resource.
     * 
     * @return the resource
     */
    SWGKnownResource getResource() {
        return resource;
    }

    @Override
    public int hashCode() {
        return (int) (17 + 31 * resource.id());
    }

    /**
     * Returns {@code true} if the mnitored resource is marked as depleted,
     * {@code false} otherwise
     * 
     * @return <code>true</code> if the resource monitored by this object is
     *         marked as depleted, <code>false</code> if it not
     */
    boolean isDepleted() {
        return resource.isDepleted();
    }

    /**
     * Returns the arbitrary user notes for this monitor. If no notes are set
     * this method returns an empty string.
     * 
     * @return user notes
     */
    public String notes() {
        return userNotes == null
                ? ""
                : userNotes;
    }

    /**
     * Sets arbitrary user notes for this monitor. The invocation replaces any
     * previous notes, or erases them if the argument is {@code null}.
     * 
     * @param notes arbitrary notes, or {@code null}
     */
    public void notes(String notes) {
        this.userNotes = notes;
    }

    @Override
    public String toString() {
        ZString z = new ZString("ResourceMonitor[").app(resource.getName());
        return z.app(':').app(resource.rc().rcName()).app(']').toString();
    }
}
