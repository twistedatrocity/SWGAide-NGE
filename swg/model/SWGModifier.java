package swg.model;

import org.w3c.dom.Element;

import swg.tools.ZHtml;
import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type models a modifier in SWG, an entity which can be created by ground
 * Reverse Engineering. In SWG a modifier is either a basic <b>attribute</b>
 * such as "Agility" or "Luck", or it is a <b>skill</b> such as "Droid Speed" or
 * "Food Experimentation".
 * <p>
 * An instance of this type reads the data associated with a named modifier;
 * that is its unique modifier name, its description, and its <i>conversion
 * ratio&nbsp;</i> which is used in ground Reverse Engineering. Instances of
 * this type are obtained from the {@link SWGProfessionManager}. Because the
 * data of a modifier is static there is no need to create duplicate instances.
 * <p>
 * This type is referenced by {@link SWGSkill} and {@link SWGAttribute} which
 * are using the composition pattern. In the future it is possible that ground
 * Reverse Engineering is added to SWGAide and then this type will be referenced
 * by SEA objects in the same manner.
 * <p>
 * This type does not implement {@code Serializable}. Clients that must save a
 * persistent reference to an instance of this type would rather save its unique
 * name and later obtain a reference on demand.
 * <p>
 * <b>Notice:</b> This type <i>does not&nbsp;</i> model <b>abilities</b>, also
 * known as specials, which are awarded a character while it is leveling up and
 * which cannot be enhanced by modifiers.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGModifier implements Comparable<SWGModifier> {

    /**
     * A description for this modifier, or {@code null} if it is unknown.
     */
    private final String desc;

    /**
     * The name of this modifier.
     */
    private final String name;

    /**
     * The conversion ratio for reverse engineering.
     */
    private final int ratio;

    /**
     * Creates an instance of this type based on the specified arguments. The
     * ratio is used in ground Reverse Engineering and must be an integer in the
     * range 1 to 20.
     * 
     * @param name
     *            the proper name of this modifier
     * @param description
     *            a description of this modifier, or {@code null}
     * @param conversionRatio
     *            the RE conversion ratio
     * @throws IllegalArgumentException
     *             if conversion ratio is outside the valid range [1 20]
     * @throws NullPointerException
     *             if name is {@code null}
     */
    private SWGModifier(String name, String description, int conversionRatio) {
        if (name == null)
            throw new NullPointerException("Name is null");
        if (conversionRatio <= 0 || conversionRatio > 20)
            throw new IllegalArgumentException(
                "Illegal ratio: " + conversionRatio);
        this.name = name;
        desc = description;
        ratio = conversionRatio;
    }

    @Override
    public int compareTo(SWGModifier o) {
        return this.name.compareTo(name);
    }

    /**
     * Returns the conversion ration for this modifier which is used in ground
     * reverse engineering.
     * 
     * @return the RE conversion ratio
     */
    public int getConversionRatio() {
        return ratio;
    }

    /**
     * Returns a description of this modifier. If a description is not known the
     * string "UNKNOWN" is returned.
     * 
     * @return a description, or "UNKNOWN"
     */
    public String getDescription() {
        return desc != null
            ? desc
            : "UNKNOWN";
    }

    /**
     * Returns the name of this modifier.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGModifier").app('[').app(name).app(" (");
        return z.app(ratio).app(")").app(']').toString();
    }

    /**
     * Creates and returns an instance of this type based on the content of the
     * specified XML element. The XML element is on the format:
     * 
     * <pre>{@literal <modifier name="" description="" conversion_ratio="" />
     * }</pre>
     * <p>
     * This method should only be invoked by {@link SWGProfessionManager} when
     * the collection of modifiers is populated.
     * 
     * @param xml
     *            an XML element
     * @return the created instance
     * @throws IllegalArgumentException
     *             if the conversion ratio is outside the valid range [1 20]
     * @throws NullPointerException
     *             if the name attribute is {@code null}
     */
    static SWGModifier newInstance(Element xml) {
        // <modifier name="" description="" conversion_ratio="" />

        String n = ZXml.stringFromAttr(xml, "name");

        String d = ZXml.stringFromAttr(xml, "description");
        d = ZHtml.normalize(d);

        int cr = ZXml.intFromAttr(xml, "conversion_ratio");

        return new SWGModifier(n, d, cr);
    }
}
