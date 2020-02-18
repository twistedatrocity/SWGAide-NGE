package swg.model;

import org.w3c.dom.Element;

import swg.tools.ZHtml;
import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type denotes an ability in SWG, such as "Reduced Vendor Fees" or
 * "Assault (Mark 1)"; often combat professions say <i>specials&nbsp;</i>. An
 * ability is a profession specific benefit, command, or talent which is awarded
 * a character only once. Some abilities are repeated with the same name, such
 * as "Reduced Vendor Fees"; to my knowledge these cannot be utilized
 * individually but the most powerful ability is always used. However, many
 * abilities are enumerated, such as "Assault (Mark 1)", "Assault (Mark 2)",
 * etc., each an individual element with its associated power; these abilities
 * can often be used individually if the player rather wants to utilize the
 * lesser power or the associated "cost".
 * <p>
 * An instance of this type contains the name, the value, and the description of
 * the ability, static data as it is read in SWG. Instances are obtained from
 * {@link SWGProfessionManager}.
 * <p>
 * This type does not implement {@code Serializable}. Clients that must save a
 * persistent reference to an instance of this type would rather save save the
 * name <i>and &nbsp;</i>the profession level to which the instance pertains and
 * later obtain an instance on demand.
 * <p>
 * Compare this type with {@link SWGAttribute}, {@link SWGModifier}, and
 * {@link SWGSkill}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGAbility implements Comparable<SWGAbility> {

    /**
     * A description for this ability, or {@code null} if it is unknown.
     */
    private final String desc;

    /**
     * The name of this ability.
     */
    private final String name;

    /**
     * The value or worth of this ability. If this is a "repeated" ability this
     * is the accumulated value up to and including this instance, not the delta
     * value from the previous occasion this ability was awarded.
     */
    private final int value;

    /**
     * Creates an instance of this type based on the specified arguments. The
     * specified value is the worth or power of this ability, or 1 if there is
     * no associated value.
     * <p>
     * If this is a "repeat" ability the name must be augmented to discern the
     * instances from each other. This is done by augmenting the name with the
     * "occasion", such as "Reduced Vendor Fees(1)", enumerated naturally.
     * <p>
     * If this is a "repeat" ability it is the accumulated value up to and
     * including this instance, not the delta value from a previous occasions
     * this ability was awarded.
     * 
     * @param name
     *            the proper name of this ability
     * @param description
     *            a description for this ability
     * @param value
     *            the value or worth of this ability, or 1
     * @throws IllegalArgumentException
     *             if the value is invalid
     * @throws NullPointerException
     *             if the name is {@code null}
     */
    private SWGAbility(String name, String description, int value) {
        if (name == null)
            throw new NullPointerException("Name is null");
        if (value <= 0)
            throw new IllegalArgumentException(
                "Illegal value: " + value);

        this.name = name;
        this.desc = description;
        this.value = value;
    }

    @Override
    public int compareTo(SWGAbility o) {
        return this.name.compareTo(name);
    }

    /**
     * Returns a description of this ability. If a description is not known the
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
     * Returns the name of this ability.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value or the worth of this ability. The worth can be a
     * percent reduction of something, an additional damage, an amount of armor
     * improvement, etc. If the ability is not measured the returned value is
     * always 1.
     * <p>
     * If this is an ability that appears several times throughout the
     * character's progression the returned value denotes the worth for this
     * instance. However, in this case the value should be the accumulated
     * value, not the delta value from the previous award.
     * 
     * @return the value of this ability, or 1
     */
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGAbility").app('[');
        return z.app(name).app(" (").app(value).app(")").app(']').toString();
    }

    /**
     * Creates an instance of this type based on the specified XML element. The
     * XML element is on the format:
     * 
     * <pre>{@literal <ability name="" description="" value="" /> }</pre>
     * 
     * The specified value is the worth or power of this ability. However, if
     * this is a "repeat" ability it is the accumulated value up to and
     * including this instance, not the delta value from a previous occasions
     * this ability was awarded.
     * <p>
     * This method should only be invoked by {@link SWGProfessionManager} when
     * the collection of abilities is populated.
     * 
     * @param xml
     *            an XML element for the returned instance
     * @return an instance of this type
     * @throws IllegalArgumentException
     *             if the value is negative
     * @throws NullPointerException
     *             if the name is {@code null}
     */
    static SWGAbility newInstance(Element xml) {
        // <ability name="" description="" amount="" />

        String n = ZXml.stringFromAttr(xml, "name");

        String d = ZXml.stringFromAttr(xml, "description");
        d = ZHtml.normalize(d);

        int v = ZXml.intFromAttr(xml, "value");
        v = v != 0 ? v : 1;

        return new SWGAbility(n, d, v);
    }
}
