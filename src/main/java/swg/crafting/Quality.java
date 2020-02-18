package swg.crafting;

import swg.gui.common.SWGGui;

/**
 * This generic type denotes some kind of quality for an object. It is made
 * generic both in form and package location so it may serve as well schematics
 * as future use cases.
 * <p>
 * <b>Schematics:</b> for an identified schematic this type denotes whether
 * crafted items are expected to be...
 * <ul>
 * <li>always HQ &mdash; such as weapons or harvesters</li>
 * <li>always LQ &mdash; such as entertainer props or furniture</li>
 * <li>mixed &mdash; such as a DE component which for some use must be HQ and
 * for other use can be LQ.</li>
 * </ul>
 * For schematics this property makes sense only for schematics that have one or
 * more experimental groups. For the "mixed" case the quality property is
 * determined by the use of the schematic. This is the general case, if a user
 * wants a particular item for decoration or if she ignores the quality the
 * quality property is irrelevant and "mixed" <i><b>must not </b></i> be used.
 * <p>
 * Future and optional use
 * <p>
 * <b>Experimenting groups:</b> for an identified group this type may denote the
 * quality level of the group. In this case the schematic is HQ but the user
 * selects to ignore some groups for one reason or the other.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public enum Quality implements SWGGui {

    /**
     * High Quality.
     */
    HQ,

    /**
     * Low Quality or bulk.
     */
    LQ,

    /**
     * Depends on the use case; HQ in some cases, any quality in other. This
     * property does not denote when a user just wants an item for decoration.
     */
    MIXED,

    /**
     * The property is yet unknown.
     */
    UNKNOWN;

    public String getDescription() {
        return "Quality[" + name() + ']';
    }

    public String getName() {
        return this == HQ || this == LQ
                ? name()
                : name().toLowerCase();
    }

    /**
     * Returns a quality constant which is determined by the specified string.
     * Valid arguments are "HQ", "LQ", and "MIXED" in upper or lower case. For
     * any other argument this method returns {@link #UNKNOWN}.
     * 
     * @param q a string
     * @return a quality constant
     */
    public static Quality valueof(String q) {
        if ("HQ".equalsIgnoreCase(q)) return HQ;
        if ("LQ".equalsIgnoreCase(q)) return LQ;
        if ("MIXED".equalsIgnoreCase(q)) return MIXED;

        return UNKNOWN;
    }
}
