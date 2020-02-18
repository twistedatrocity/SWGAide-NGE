package swg.crafting.schematics;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type denotes an experimentation group in a schematic. In-game each group
 * reads a description and displays one or several experimentation lines. This
 * type reflects just these two properties.
 * <p>
 * Most experimentation groups just contain one line, however, some schematics
 * have groups with several lines; an example is the weapon group "Experimental
 * Damage" which often has the two lines "Maximum Damage" and "Minimum Damage".
 * <p>
 * While experimenting on one group both lines are updated, each by its own dice
 * roll, but the game just displays the better of the two which sometimes
 * confuses new players.
 * <p>
 * This type does not implement {@code Serializable}. A client that must save a
 * persistent reference to a specific experimentation group would rather save
 * the schematic ID which the slot pertains to and the description for the slot,
 * a combination which is a unique key.
 * <p>
 * The XML element for the data contained by instances of this type has the
 * following layout and allows for several experiment elements:
 * 
 * <pre>{@literal <exp_grp desc="">
     <exp ... />
 </exp_grp> }</pre>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGExperimentGroup {

    /**
     * The description for this experimentation group, as read at the schematic.
     */
    private final String desc;

    /**
     * An array of experimentation lines, there is at least one line.
     */
    private final List<SWGExperimentLine> expLines;

    /**
     * Creates an instance of this type based on the specified XML element.
     * 
     * @param xml
     *            the XML element for this instance
     * @throws IllegalArgumentException
     *             if there is an error in the parsed data
     * @throws NullPointerException
     *             if something is {@code null}
     */
    SWGExperimentGroup(Element xml) {
        // <exp_grp desc="">
        // <exp ... />
        // </exp_grp>

        desc = ZXml.stringFromAttr(xml, "desc");

        NodeList lines = xml.getChildNodes();
        int nlLen = lines.getLength();

        if (nlLen < 3)
            throw new IllegalArgumentException("Invalid size, this: " + desc);

        // An element is supposed to also contain text nodes, one per element
        // plus one, this element does not, hence the right shift
        expLines = new ArrayList<SWGExperimentLine>(nlLen >> 1);

        for (int i = 0; i < nlLen; ++i) {
            Node n = lines.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE // just experiment elements
                && n.getNodeName().equals("exp") /* for sanity */) {

                SWGExperimentLine exp = new SWGExperimentLine((Element) n);
                expLines.add(exp);
            }
        }

        if ((nlLen >> 1) != expLines.size())
            throw new IllegalArgumentException("Size diff: " + desc);
    }

    /**
     * Returns the description for this experimentation group as it is read at
     * the schematic, or {@code null} if it is missing.
     * 
     * @return a description, or {@code null}
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Returns a list of experimentation lines for this group, the returned list
     * contains at least one element. The returned list is <b>read-only</b>.
     * 
     * @return a list of experimentation lines
     */
    public List<SWGExperimentLine> getExperimentalLines() {
        return expLines;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGExperimentGroup[").app(desc).app('=');
        return z.app(expLines.toString()).app(']').toString();
    }
}
