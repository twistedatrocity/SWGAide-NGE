package swg.gui.resources;

import java.io.Serializable;

import swg.crafting.resources.SWGKnownResource;
import swg.tools.ZString;

/**
 * This type is a plain wrapper for resources in resource inventories and its
 * inventory data for a particular resource. This type is used for the data
 * displayed at the GUI panel named "Inventory". A particular resource has no
 * knowledge of this type nor that it is listed in a resource inventory.
 * <p>
 * From the deep reaching changes in SWGAide during July to October 2009 this
 * type will transparently display any changes made to the wrapped resource as
 * now there is one and only one instance per resource.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGInventoryWrapper implements Serializable,
        Comparable<SWGInventoryWrapper> {

    /**
     * A dummy constant to denote an invalid wrapper.
     */
    static final SWGInventoryWrapper DUMMY = new SWGInventoryWrapper();
    
    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 6854209355736865736L;

    /**
     * The number of units for the resource this instance pertains to.
     * 
     * @serial a long integer
     */
    private long amount = 1;

    /**
     * The name of the assignee for this wrapper. This may be the name of a
     * character but it can also be some token assigned at the user's choice.
     * 
     * @serial a string
     */
    private String assignee;

    /**
     * A temporary member that allows for replacing, adding, subtracting units
     * to {@link #amount} of an existing wrapper. If this member is {@code null}
     * (not initiated) the old style should be used, which is to ignore
     * duplicate entries. Methods that update inventory collections may use this
     * member, if there is an entry that equals this instance by assignee and
     * resource these methods updates {@link #amount} of the older instance with
     * the value in this instance. That is, this instance conveys the difference
     * or the replacement value. Legal values are:
     * <ul>
     * <li><tt> = </tt>replace old value</li>
     * <li><tt> + </tt>add to old value</li>
     * <li><tt> - </tt>subtract from old value</li>
     * <li><tt>null </tt>ignore this field</li>
     * </ul>
     */
    public transient String equalAddSub;

    /**
     * Notes for the inventory entry.
     * 
     * @serial a string of notes or {@code null}
     */
    private String notes;

    /**
     * The resource this object pertains to.
     * 
     * @serial a resource
     */
    private final SWGKnownResource resource;

    /**
     * A private constructor only used by {@link #DUMMY}.
     */
    private SWGInventoryWrapper() {
        // should not be used unless for the dummy
        resource = null;
        assignee = "";
    }

    
    /**
     * Creates an object of this type for the specified arguments.
     * 
     * @param resource the resource for the inventory entry
     * @param assigne the assignee for this inventory entry
     * @throws NullPointerException if there is a {@code null} argument
     */
    SWGInventoryWrapper(SWGKnownResource resource, String assigne) {
        if (resource == null || assigne == null) { throw new NullPointerException(
                "Illegal argument: " + resource + ':' + assigne); }
        this.resource = resource;
        this.setAssignee(assigne);
    }

    public int compareTo(SWGInventoryWrapper o) {
        return this.resource.compareTo(o.resource);
    }
    
    /**
     * hashcode method
     */
    @Override
    public int hashCode() {
        int hashcode = 0;
        hashcode += assignee.hashCode();
        hashcode += resource.hashCode();
        return hashcode;
    }
    
    /**
     * override equals to prevent dups
     * Use Set to invoke rather than List
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SWGInventoryWrapper) {
    		SWGInventoryWrapper wr = (SWGInventoryWrapper) obj;
            boolean flag=(wr.assignee.equals(this.assignee) && wr.resource.equals(this.resource)); 
            return flag;
        } else {
                return false;
        }
     }

    /**
     * Creates and returns a copy of this object but for the specified assignee.
     * 
     * @param ass the assignee for the copy
     * @return a copy of this object
     */
    SWGInventoryWrapper copy(String ass) {
        SWGInventoryWrapper ret = new SWGInventoryWrapper(this.resource, ass);
        ret.amount = this.amount;
        ret.notes = this.notes;
        return ret;
    }

    /**
     * Returns the number of units for this object.
     * 
     * @return the number of units
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Returns the assignee for this object.
     * 
     * @return the assignee
     */
    String getAssignee() {
        return assignee;
    }

    /**
     * Returns the optional user notes for this object, or the empty string if
     * there are no notes.
     * 
     * @return the notes
     */
    public String getNotes() {
        return notes == null
                ? ""
                : notes;
    }

    /**
     * Returns the resource this object pertains to.
     * 
     * @return the resource
     */
    public SWGKnownResource getResource() {
        return resource;
    }

    /**
     * Sets the number of units for this object. If the argument is negative
     * this implementation silently sets it to 0.
     * 
     * @param amount the number of units to set
     */
    void setAmount(long amount) {
        if (amount < 0)
            this.amount = 0;
        else
            this.amount = amount;
    }

    /**
     * Sets the assignee for this object.
     * 
     * @param assignee the assignee to set
     * @throws IllegalArgumentException if the argument is the empty string, or
     *         {@code null}
     */
    void setAssignee(String assignee) {
        if (assignee == null || assignee.isEmpty())
            throw new IllegalArgumentException("Illegal argument: " + assignee);
        this.assignee = assignee;
    }

    /**
     * Sets the optional user notes for this object. If the argument is {@code
     * null} the previous notes are erased.
     * 
     * @param notes the notes to set
     */
    void setNotes(String notes) {
        String s = notes;
        if (s != null && (s = s.trim()).isEmpty())
            s = null;

        this.notes = s;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGInventoryWrapper").app('[');
        z.app(resource.toString()).app('|').app("units:").app(amount).app(']');
        return z.toString();
    }
}
