package swg.crafting.schematics;

import org.w3c.dom.Element;

import swg.crafting.resources.SWGResourceClass;
import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type models a resource slot in a schematic. Each resource slot in a
 * schematic reads which resource class it accepts, its amount, and its
 * description; this type reads these data.
 * <p>
 * This type does not implement {@code Serializable}.
 * <p>
 * The XML element for the data contained by instances of this type has the
 * following layout:
 * 
 * <pre>{@literal <resource swgcraft_id="" units="" desc="" /> }</pre>
 * <p>
 * <b>Notice:</b> the "swgcraft_id" is the old .com resource class token, not
 * the integer ID.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGResourceSlot {

    /**
     * The description for this resource slot as it is read at the schematic.
     */
    private final String desc;

    /**
     * The resource class which this resource slot calls for.
     */
    private final SWGResourceClass resClass;

    /**
     * The number of units it takes to fill this resource slot.
     */
    private final int units;

    /**
     * Creates an instance of this type based on the specified XML element.
     * 
     * @param xml
     *            the XML element for this resource slot
     * @throws IllegalArgumentException
     *             if there is an error in the parsed data
     * @throws NullPointerException
     *             if something is {@code null}
     */
    SWGResourceSlot(Element xml) {
        // <resource swgcraft_id="" units="" desc="" />

        String rc = ZXml.stringFromAttr(xml, "id");
        resClass = SWGResourceClass.rc(rc);

        units = ZXml.intFromAttr(xml, "units");
        if (units <= 0)
            throw new IllegalArgumentException("Invalid units: " + units);

        desc = ZXml.stringFromAttr(xml, "desc");
    }

    /**
     * Returns the description for this resource slot as it is read at the
     * schematic, or {@code null} if it is unknown.
     * 
     * @return a description, or {@code null}
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Returns the resource class specified for this resource slot.
     * 
     * @return the resource class
     */
    public SWGResourceClass getResourceClass() {
        return resClass;
    }

    /**
     * Returns the number of units it takes to fill this resource slot.
     * 
     * @return the number of units
     */
    public int getUnits() {
        return units;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGResourceSlot[").app(units).app(' ');
        return z.app(resClass.rcName()).app(", ").app(desc).app(']').toString();
    }
}
