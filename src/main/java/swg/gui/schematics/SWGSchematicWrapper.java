package swg.gui.schematics;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import swg.crafting.resources.SWGResourceSet;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.common.SWGGui;
import swg.tools.ZString;

/**
 * This type contains options and selections that pertains to a wrapped
 * schematic or items made or manufactured from it. It may exist several
 * wrappers per unique schematic which allows for the user to assign different
 * statistics per wrapper. Serialization is supported.
 * <p>
 * <b>Notice:</b> On demand one default instance per schematic is created, <b>it
 * is an error</b> to modify some fields of that instance.
 * <p>
 * This type is thread safe and locks on smallest possible scope. Only types
 * that support serialization are persistently saved; it is the schematics ID
 * that is stored.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGSchematicWrapper implements Serializable, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -903155204433026635L;

    /**
     * A comparator for schematic wrappers, see {@link #comparator()}.
     */
    private static Comparator<SWGSchematicWrapper> wrapperComp;

    /**
     * The amount of items in stock of the kind this wrapper models.
     * 
     * @serial an integer for the in-game stock count
     */
    private int count;

    /**
     * A list of wrappers for experiment groups that pertain to this instance.
     * This list must not be serialized but is refreshed on demand.
     */
    private transient List<SWGExperimentWrapper> expWrappers;

    /**
     * A flag that marks whether this is a default wrapper or not.
     */
    private final boolean isDefault;

    /**
     * The notes for this wrapper, or {@code null} if no notes are set.
     */
    private String notes;

    /**
     * A schematic wrapped by this type.
     */
    private transient SWGSchematic schematic;

    /**
     * The ID for the wrapped schematic.
     * 
     * @serial an integer id for the wrapped schematic
     */
    private final Integer schemID;

    /**
     * The in-game serial ID for the items crafted from the wrapped schematic,
     * or {@code null} if the user has not set this value.
     * 
     * @serial a string for an in-game serial number
     */
    private String schemSerial;

    /**
     * Creates a non-default instance of this type for the specified schematic.
     * Compare {@link #createDefault(SWGSchematic)}.
     * 
     * @param schem a schematic
     */
    SWGSchematicWrapper(SWGSchematic schem) {
        this(schem, false);
    }

    /**
     * Helper creator which creates an instance of this type for the specified
     * schematic.
     * 
     * @param schem a schematic
     * @param isDefault {@code true} to create a default wrapper
     */
    private SWGSchematicWrapper(SWGSchematic schem, boolean isDefault) {
        this.schematic = schem;
        this.schemID = Integer.valueOf(schem.getID());
        this.isDefault = isDefault;
    }

    /**
     * Returns a list of wrappers for experiment groups and lines that pertain
     * to this instance. If the wrapped schematic contains no experiments this
     * method returns a list of wrappers for LQ resources. If the list is not
     * yet initiated this call creates a list of uninitiated wrappers, see
     * {@link SWGExperimentWrapper#refresh(List, SWGResourceSet, List)}.
     * 
     * @return a list of experiment wrappers, possibly uninitialized
     */
    List<SWGExperimentWrapper> experiments() {
        synchronized (this) {
            if (expWrappers == null)
                expWrappers = SWGExperimentWrapper.createInstances(this);

            return expWrappers;
        }
    }

    @Override
    public String getDescription() {
        return "";
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is the name of the schematic this wrapper pertains to.
     */
    @Override
    public String getName() {
        return schem().getName();
    }

    /**
     * Determines if this is a default wrapper for its schematic.
     * 
     * @return {@code true} if this instance is a default wrapper
     */
    boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns the notes for this wrapper.
     * 
     * @return the notes, or {@code null}
     */
    String notes() {
        return notes;
    }

    /**
     * Sets the notes for this wrapper to the specified string, or to {@code
     * null}. Any previous notes are erased.
     * 
     * @param notes the new notes, or {@code null}
     */
    void notes(String notes) {
        this.notes = ZString.ton(notes);
    }

    /**
     * This method adds the deserialized object to SWGAide's cache and returns
     * {@code this}. If there is an instance that logically equals {@code this}
     * the cached instance is returned.
     * 
     * @return this
     */
    private Object readResolve() {
        // XXX: remove in a while, added with 0.9.0
        if (isDefault) schemSerial = null;
        return this;
    }

    /**
     * Returns the schematic wrapped by this instance.
     * 
     * @return a schematic
     */
    SWGSchematic schem() {
        synchronized (schemID) {
            if (schematic == null)
                schematic = SWGSchematicsManager.getSchematic(schemID());
            return schematic;
        }
    }

    /**
     * Returns the integer ID of the schematic wrapped by this instance.
     * 
     * @return a schematic ID
     */
    int schemID() {
        return schemID.intValue();
    }

    /**
     * Returns in-game serial number for the items crafted from the wrapped
     * schematic. If the value is not set this method returns "-- ? --".
     * 
     * @return the in-game serial number
     */
    String serial() {
        return schemSerial == null
                ? isDefault
                        ? schem().getName()
                        : "-- ? --"
                : schemSerial;
    }

    /**
     * Sets the in-game serial number for the items crafted from the wrapped
     * schematic; {@code null} clears this value. If this instance is a default
     * wrapper this method does nothing.
     * 
     * @param serial an in-game serial number, or {@code null}
     */
    void serial(String serial) {
        this.schemSerial = ZString.ton(serial);
    }

    /**
     * Returns the amount of items in stock of the kind this wrapper models.
     * 
     * @return the stock count
     */
    int stock() {
        return count;
    }

    /**
     * Sets the amount of items in stock of the kind this wrapper models to the
     * specified value. If this instance is a default wrapper this method does
     * nothing.
     * 
     * @param count the stock count
     * @throws IllegalArgumentException if {@code count < 0}
     */
    void stock(int count) {
        if (count < 0)
            throw new IllegalArgumentException("Invalid value: " + count);
        this.count = count;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGSchematicWrapper[").app(getName()).app(',');
        return z.app(serial()).app(',').app(count).app(']').toString();
    }

    /**
     * Returns a comparator for schematic wrappers with sort order:<br/>
     * 1 - schematic ID<br/>
     * 2 - default wrapper<br/>
     * 3 - schematic serial<br/>
     * 
     * @return a comparator
     */
    static Comparator<SWGSchematicWrapper> comparator() {
        if (wrapperComp == null)
            wrapperComp = new Comparator<SWGSchematicWrapper>() {

                @Override
                public int compare(//
                        SWGSchematicWrapper o1, SWGSchematicWrapper o2) {
                    if (o1 == o2) return 0;

                    // first: schematic IDs
                    int ret = o1.schemID() - o2.schemID();
                    if (ret != 0) return ret;

                    // second: is-default
                    if (o1.isDefault() && !o2.isDefault()) return -1;
                    if (!o1.isDefault() && o2.isDefault()) return 1;

                    // third: serial
                    return o1.serial().compareTo(o2.serial());
                }
            };
        return wrapperComp;
    }

    /**
     * Creates and returns a default wrapper for the specified schematic. A
     * default wrapper is per schematic and users shall not modify it.
     * <p>
     * <b>Notice:</b> It is an error to call this factory more than once per
     * schematic, however, this implementation does not guard against this.
     * 
     * @param schem a schematic
     * @return a default schematic wrapper
     */
    static SWGSchematicWrapper createDefault(SWGSchematic schem) {
        return new SWGSchematicWrapper(schem, true);
    }
}
