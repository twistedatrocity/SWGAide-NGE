package swg.crafting.schematics;

import org.w3c.dom.Element;

import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type denotes an experimentation line contained in an experimentation
 * group of a schematic; one or several lines are contained in a group. An
 * experimentation line reads a description and it specifies the weights for a
 * limited set of resource stats in relation to a specified property of the
 * crafted item. For example, for food a line reads "Nutritional Value" (its
 * power) and the important resource stats and their weights are 66% PQ and 33%
 * OQ.
 * <p>
 * This type does not implement {@code Serializable}. A client that must save a
 * persistent reference to a specific experimentation line can save the ID for
 * the schematic the line pertains to, the description of the group, and the
 * description of the line, a combination which is a unique key.
 * <p>
 * The XML element for the data contained by instances of this type, which is a
 * sub-element of an experimentation-group, has the following layout:
 * 
 * <pre>{@literal <exp desc="" cd="" cr="" dr="" er="" fl="" hr="" ma="" oq="" 
 *      pe="" sr="" ut="" /> <!-- only stats greater than zero -->}</pre>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGExperimentLine {

    /**
     * The description for this experimental line as it is read at the
     * schematic, or {@code null} if it is unknown.
     */
    private final String desc;

    /**
     * The experimentation weights for this line.
     */
    private final SWGWeights weights;

    /**
     * Creates an instance of this type based on the specified XML element.
     * 
     * @param xml the XML element for this experimentation line
     * @throws IllegalArgumentException if there is an error in the parsed data
     * @throws NullPointerException if something is {@code null}
     */
    SWGExperimentLine(Element xml) {

        desc = ZXml.stringFromAttr(xml, "desc");

        int[] values = new int[Stat.COUNT];
        for (Stat s : Stat.values())
            values[s.i] = ZXml.intFromAttr(xml, s.getName());

        weights = new SWGWeights(values);
    }

    /**
     * Returns the description for this experimentation line as it is read at
     * the schematic, or {@code null} if it is unknown.
     * 
     * @return the description, or {@code null}
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Returns the experimentation weights for this line. The returned value is
     * <b>read-only</b>.
     * 
     * @return the weights
     */
    public SWGWeights getWeights() {
        return weights;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGExperimentLine[").app(desc).app(", ");
        weights.toString(z, false);
        return z.app(']').toString();
    }
}
