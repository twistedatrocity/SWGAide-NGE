package swg.crafting;

import swg.gui.common.SWGGui;

/**
 * This type denotes a resource stat in SWG. Each constant is named after its
 * common known abbreviation but also reads its full description. Convenience
 * methods and constant variables are provided.
 * <p>
 * <b>Terminology:</b> this class and its sub-types are using a strict
 * terminology, while in casual speaking that is most often not the case::
 * <ul>
 * <li><b>stat:</b> a specific statistic of a resource, such as CD, OQ, etc.</li>
 * <li><b>value: </b> the value of a specific stat in a resource, such as OQ=965
 * where the value is 965.</li>
 * <li><b>expected stat:</b> a stat that is required by the particular resource
 * class, one can also say <b>expected value</b> as that is a corollary.</li>
 * </ul>
 * <p>
 * <b>Notice: </b>All stats in this model are ordered alphabetically, while, for
 * some reason, SWG is most often using the order: ER, CR, CD, DR, FL, HR, MA,
 * PE, OQ, SR, UT.
 * 
 * <pre>
 * model order:   CD,CR,DR,ER,FL,HR,MA,OQ,PE,SR,UT
 * in-game order: ER,CR,CD,DR,FL,HR,MA,PE,OQ,SR,UT
 * </pre>
 * <p>
 * Because this type does not need "mapped by integer" there is no index based
 * getter. Now constants are obtained by name via {@link #valueof(String)}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public enum Stat implements SWGGui {

    /**
     * Conductivity.
     */
    CD("Conductivity", 0),

    /**
     * Cold Resistance.
     */
    CR("Cold Resistance", 1),

    /**
     * Decay Resistance.
     */
    DR("Decay Resistance", 2),

    /**
     * Entangle Resistance.
     */
    ER("Entangle Resistance", 3),

    /**
     * Flavor.
     */
    FL("Flavor", 4),

    /**
     * Heat Resistance.
     */
    HR("Heat Resistance", 5),

    /**
     * Malleability.
     */
    MA("Malleability", 6),

    /**
     * Overall Quality.
     */
    OQ("Overall Quality", 7),

    /**
     * Potential Energy.
     */
    PE("Potential Energy", 8),

    /**
     * Shock Resistance.
     */
    SR("Shock Resistance", 9),

    /**
     * Unit toughness.
     */
    UT("Unit toughness", 10);

    /**
     * Total number of resource stats.
     */
    public static final int COUNT = 11;

    /**
     * The description for this constant.
     */
    private final String desc;
    
    /**
     * The index, or position, of this constant in the alphabetical sequence of
     * stats, in the interval [0 {@link #COUNT}].
     */
    public final int i;
    
    /**
     * Convenience string for the name in lower case;
     */
    private final String n;

    /**
     * Creates a constant for a stat. The specified description is its proper
     * name and the index is its position in the alphabetical sequence.
     * 
     * @param d a description
     * @param idx the index or position
     */
    private Stat(String d, int idx) {
        desc = d;
        i = idx;
        n = name().toLowerCase();
    }

    /**
     *{@inheritDoc} This is the proper name of this stat, such as Conductivity.
     */
    public String getDescription() {
        return desc;
    }

    /**
     * {@inheritDoc} This is the name of this constant in lower case, use
     * {@link #name()} for upper case.
     */
    public String getName() {
        return n;
    }

    /**
     * Before invocation of the implicit {code valueOf(String)} this method
     * converts the argument to upper case and it also trims the argument.
     * 
     * @param s a string
     * @return a constant
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException if the argument is invalid
     */
    public Stat valueof(String s) {
        String t = s.toUpperCase().trim();
        return valueOf(t);
    }

    /**
     * Returns an array of constants ordered as in-game; see class comment. Use
     * {@link #values()} for the alphabetical order that is used in SWGAide.
     * 
     * @return an array {@code [ER, CR, CD, DR, FL, HR, MA, PE, OQ, SR, UT]}
     */
    public static Stat[] gameOrder() {
        return new Stat[] { ER, CR, CD, DR, FL, HR, MA, PE, OQ, SR, UT };
    }
}
