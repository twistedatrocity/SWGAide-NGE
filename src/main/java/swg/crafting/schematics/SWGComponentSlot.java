package swg.crafting.schematics;

import org.w3c.dom.Element;

import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type models a component slot in a schematic, not the component as such,
 * but an instance of this type references a schematic, a category, or an item.
 * Each component slot in a schematic reads which content it accepts, its
 * description, its amount, if its content is optional, if its content must be
 * identical or if similar suffices, etc., and it is reflected by an instance of
 * this type which reads that data.
 * <p>
 * <dl>
 * <dt>Component</dt>
 * <dd>An item which is crafted or manufactured from a schematic, or it is a
 * looted item.</dd>
 * <dt>Component category</dt>
 * <dd>A designation, it is a term for several items which fits into the
 * category, such as "Drink Container" which denotes Small Glass, Large Glass,
 * etc.</dd>
 * </dl>
 * <p>
 * This type does not implement {@code Serializable}. A client that must save a
 * persistent reference for a component slot can use the schematic ID which the
 * slot pertains to <b>and</b> the description of the slot as a unique key.
 * <p>
 * The XML element for the data contained by instances of this type has the
 * following layout:
 * 
 * <pre>{@literal <component id="" type="schematic|category|item|error" number="" 
     similar="" optional="" looted="" desc=""/> }</pre>
 * Description of the attributes:
 * <p>
 * <dl>
 * <dt>id</dt>
 * <dd>the integer ID of a schematic or a category, or the text name of an item;
 * which of the three is determined by the type attribute</dd>
 * <dt>type</dt>
 * <dd>one of schematic, category, item, or error</dd>
 * <dt>number</dt>
 * <dd>the number of items for this slot</dd>
 * <dt>similar</dt>
 * <dd>denotes if the item can be similar; default is "no", the one or several
 * items must be identical</dd>
 * <dt>optional</dt>
 * <dd>denotes if filling this slot is optional; default is "no"</dd>
 * <dt>looted_only_component</dt>
 * <dd>denotes if the item for this slot is only obtained as loot and it
 * <i>cannot&nbsp;</i> be crafted; default is "no"</dd>
 * <dt>desc</dt>
 * <dd>the description for this slot as it is read in the schematic</dd>
 * </dl>
 * <p>
 * <b>Notice:</b> The general in-game rule for "similar" versus "identical" is
 * that unless the schematic explicitly reads "similar" the one or several items
 * must be "identical". This is true also if the slot calls for just one item
 * because a manufacture schematic reads a serial number for the slot which
 * equals to "identical". For manual crafting: in a slot that calls for just one
 * item <i>any&nbsp;</i> item will do; this is not an exception but it is a
 * special case. In summary: <b>default is "identical"</b>.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGComponentSlot {

    /**
     * The number of items that this slot calls for, minimum is 1.
     */
    private final int amount;

    /**
     * The description for this slot as it is read at the schematic, for example
     * "Module Frame" or "Armor Core", or {@code null} if the description is
     * unknown.
     */
    private final String desc;

    /**
     * The ID which for schematic and category is the integer ID as text, and
     * for an item it is its name. Thus value must not be {@code null}.
     */
    private final String id;

    /**
     * A flag which denotes if the item for this slot is looted-only and it
     * cannot be crafted; default is {@code false}.
     */
    private final boolean looted;

    /**
     * A flag which denotes if filling this slot is optional. The default value
     * is {@code false}, this slot must take one or several items.
     */
    private final boolean optional;

    /**
     * A flag which denotes if the content in this slot can be "similar". The
     * default value is {@code false}, the one or several items in this slot
     * must be identical. See the final note in the documentation of this class.
     */
    private final boolean similar;

    /**
     * This value determines the kind of component for this slot and is one of
     * schematic, category, item, or error.
     */
    private final String type;

    /**
     * Creates an instance of this type based on the specified XML element.
     * 
     * @param xml
     *            the XML element for this component slot
     * @throws IllegalArgumentException
     *             if there is an error or if the content's name or if amount is
     *             not defined
     * @throws NullPointerException
     *             if something is {@code null}
     */
    SWGComponentSlot(Element xml) {
        // <component id="" type="schematic|category|item|error" number=""
        // similar="" optional="" looted_only_component="" desc=""/>

        id = ZXml.stringFromAttr(xml, "id");

        type = ZXml.stringFromAttr(xml, "type");

        amount = ZXml.intFromAttr(xml, "number");

        similar = ZXml.booleanFromAttr(xml, "similar");

        optional = ZXml.booleanFromAttr(xml, "optional");

        looted = ZXml.booleanFromAttr(xml, "looted");

        desc = ZXml.stringFromAttr(xml, "desc");

        if (id == null || type == null || amount < 1 || type.equals("error"))
            throw new IllegalArgumentException("Invalid argument(s): " + this);
    }

    /**
     * Returns the number of items which this component slot calls for.
     * 
     * @return the number of items
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Returns the ID number which uniquely identifies a component category for
     * this slot, or -1 if the item for this slot is not a category; see
     * {@link #getType()}.
     * 
     * @return a category ID, or -1
     */
    public int getCategoryId() {
        return type.equals("category") ? Integer.parseInt(id) : -1;
    }

    /**
     * Returns the description for this component slot as it is read at the
     * schematic, or {@code null} if it is unknown.
     * 
     * @return a description, or {@code null}
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Returns the name of the non-craftable item used in this slot, or {@code
     * null} if the item for this slot is not an item; see {@link #getType()}.
     * 
     * @return an item name, or {@code null}
     */
    public String getItemName() {
        return type.equals("item") ? id : null;
    }

    /**
     * Returns the ID number which uniquely identifies the schematic for the
     * component used in this slot, or -1 if the item for this slot is not a
     * schematic; see {@link #getType()}.
     * 
     * @return a schematic ID, or -1
     */
    public int getSchematicId() {
        return type.equals("schematic") ? Integer.parseInt(id) : -1;
    }

    /**
     * Returns a string which determines the kind of component for this slot and
     * is one of "schematic", "category", or "item".
     * 
     * @return the type of component
     */
    public String getType() {
        return type;
    }

    /**
     * Returns {@code true} if the item for this slot is looted-only and it
     * <i>cannot&nbsp;</i> be crafted; default return value is {@code false}.
     * 
     * @return {@code true} if the item for this slot is looted-only
     */
    public boolean isLootedOnly() {
        return looted;
    }

    /**
     * Returns {@code true} if filling this slot is optional; default return
     * value is {@code false}, this slot must take one or several items.
     * 
     * @return {@code true} if filling this slot is optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Returns {@code true} if the content in this slot can be "similar";
     * default return value is {@code false}, the one or several items in this
     * slot must be identical.
     * <p>
     * <b>Notice:</b> The general in-game rule for "similar" versus "identical"
     * is that unless the schematic explicitly reads "similar" the one or
     * several items must be "identical". This is true also if the slot calls
     * for just one item because a manufacture schematic reads a serial number
     * for the slot which equals to "identical". While manually crafting: in a
     * slot that calls for just one item <i>any&nbsp;</i> item will do; this is
     * not an exception but it is a special case. In summary: <b>default is
     * "identical"</b>.
     * 
     * @return {@code false} if the one or several items in this slot must be
     *         identical (default)
     */
    public boolean isSimilar() {
        return similar;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGComponentSlot[");
        z.app(amount).app(' ').app(desc).app(", ").app(type).app(':').app(id);
        z.app(similar ? ", similar" : ", identical");
        z.app(optional ? ", optional" : "");
        return z.app(looted ? ", looted" : "").app(']').toString();
    }
}
