package swg.gui.common;

/**
 * This type defines some methods which GUI elements often invoke on objects.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public interface SWGGui {

    /**
     * Returns the description for this instance, never {@code null}.
     * 
     * @return a description
     */
    public String getDescription();

    /**
     * Returns the name of this instance, never {@code null}.
     * 
     * @return a name
     */
    public String getName();
}
