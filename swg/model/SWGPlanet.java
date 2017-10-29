package swg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import swg.gui.common.SWGGui;

/**
 * This constant uniquely identifies a planet within SWG.
 * 
 * @author Steven M. Doyle <shadow@triwizard.net>
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public enum SWGPlanet implements SWGGui {

    //
    // Do not sort this type, Kashyyyk and Mustafar must be 11 and 12, see
    // getFromID(int)
    //

    /**
     * Constant for Corellia
     */
    CORELLIA("Corellia", 1),

    /**
     * Constant for Dantooine
     */
    DANTOOINE("Dantooine", 2),

    /**
     * Constant for Dathomir
     */
    DATHOMIR("Dathomir", 3),

    /**
     * Constant for Endor
     */
    ENDOR("Endor", 4),

    /**
     * Constant for Lok
     */
    LOK("Lok", 5),

    /**
     * Constant for Naboo
     */
    NABOO("Naboo", 6),

    /**
     * Constant for Rori
     */
    RORI("Rori", 7),

    /**
     * Constant for Talus
     */
    TALUS("Talus", 8),

    /**
     * Constant for Tatooine
     */
    TATOOINE("Tatooine", 9),

    /**
     * Constant for Yavin 4
     */
    YAVIN4("Yavin 4", 10),

    /**
     * Constant for Kashyyyk
     */
    KASHYYYK("Kashyyyk", 11),

    /**
     * Constant for Mustafar
     */
    MUSTAFAR("Mustafar", 12),

    /**
     * Constant for a dummy planet named "none" with ID zero
     */
    DUMMY("none", 0);

    /**
     * A list of the planet names.
     */
    private static List<String> names;

    /**
     * The SWGCraft ID for the planet constant.
     */
    private final int id;

    /**
     * The name of this planet constant.
     */
    private final String name;

    /**
     * @param name the name of the planet
     * @param id the SWGCraft ID for the planet
     */
    private SWGPlanet(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s)", name, Integer.toString(id));
    }

    /**
     * Returns the name of the planet represented by this constant.
     * 
     * @return the name of the planet
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the SWGCraft ID for the planet represented by this constant.
     * 
     * @return the SWGCraft ID for the planet
     */
    public int id() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns a planet constant for the planet which name matches the
     * abbreviation. If no match is found {@code null} is returned.
     * <P>
     * <B>Note: </B>This implementation just reads enough of the abbreviation to
     * identify a planet constant, <I>perhaps just one letter</I>.
     * 
     * @param abbreviation an abbreviation of a planet name
     * @return a planet constant which name matches the beginning of the
     *         abbreviation, or {@code null}
     */
    public static SWGPlanet fromAbbrev(String abbreviation) {

        String abbr = abbreviation.toLowerCase(Locale.ENGLISH);

        if (abbr.startsWith("c"))
            return CORELLIA;
        if (abbr.startsWith("dan"))
            return DANTOOINE;
        if (abbr.startsWith("dat"))
            return DATHOMIR;
        if (abbr.startsWith("e"))
            return ENDOR;
        if (abbr.startsWith("l"))
            return LOK;
        if (abbr.startsWith("n"))
            return NABOO;
        if (abbr.startsWith("r"))
            return RORI;
        if (abbr.startsWith("tal"))
            return TALUS;
        if (abbr.startsWith("tat"))
            return TATOOINE;
        if (abbr.startsWith("y"))
            return YAVIN4;
        if (abbr.startsWith("k"))
            return KASHYYYK;
        if (abbr.startsWith("m"))
            return MUSTAFAR;

        return null;
    }

    /**
     * Returns a planetary constant for the specified SWGCraft planet ID.
     * 
     * @param id a SWGCraft planet id
     * @return the planet constant for the identified ID
     * @throws IllegalArgumentException if the argument is not a valid ID
     */
    public static SWGPlanet fromID(int id) {
        return values()[id - 1];
    }

    /**
     * Returns a planet constant for {@code name}. If the suggested name is
     * invalid an exception is thrown.
     * 
     * @param name the proper name for the planet
     * @return a planet constant
     * @throws IllegalArgumentException if the name is invalid
     * @throws NullPointerException if the argument is {@code null}
     */
    public static SWGPlanet fromName(String name) {
        if (name.startsWith("Yavin")) // variations Yavin4 and Yavin IV
            return YAVIN4;

        return valueOf(name.toUpperCase(Locale.ENGLISH));
    }

    /**
     * Returns a planet constant for the planet which name begins with the first
     * 4 letters of the specified string. This method is case insensitive and
     * any letter beyond the first 4 letters is ignored. If no match is found
     * {@code null} is returned.
     * <P>
     * Planet <B>Lok</B> is an exception, 3 letters is enough.
     * <P>
     * This implementation complements {@link #fromAbbrev(String)} and is used
     * where a tighter control is necessary.
     * 
     * @param quad four initial letters of a planet name (3 letters for Lok)
     * @return a planet constant which name matches the string, or {@code null}
     */
    public static SWGPlanet fromQuad(String quad) {

        String q = quad.toLowerCase(Locale.ENGLISH);

        if (q.startsWith("lok"))
            return LOK;
        if (q.startsWith("core"))
            return CORELLIA;
        if (q.startsWith("dant"))
            return DANTOOINE;
        if (q.startsWith("dath"))
            return DATHOMIR;
        if (q.startsWith("endo"))
            return ENDOR;
        if (q.startsWith("nabo"))
            return NABOO;
        if (q.startsWith("rori"))
            return RORI;
        if (q.startsWith("talu"))
            return TALUS;
        if (q.startsWith("tato"))
            return TATOOINE;
        if (q.startsWith("yavi"))
            return YAVIN4;
        if (q.startsWith("kash"))
            return KASHYYYK;
        if (q.startsWith("must"))
            return MUSTAFAR;

        return null;
    }

    /**
     * Determines if the specified string is a valid name for a planet. The
     * specified string must be a proper name, this implementation returns
     * {@code false} if the string is improperly capitalized or not trimmed.
     * 
     * @param name the string to check
     * @return {@code true} if the specified string is a proper name, {@code
     *         false} otherwise
     */
    public static boolean isValid(String name) {
        if (name.startsWith("Yavin")) // variations Yavin4 and Yavin IV
            return true;

        return names().contains(name);
    }

    /**
     * Returns a list of the names of the planets in SWG, sorted alphabetically.
     * The list is immutable, see {@link Collections#unmodifiableList(List)}.
     * 
     * @return an alphabetically sorted list of planetary names
     */
    public static List<String> names() {
        if (names == null) {
            List<String> l = new ArrayList<String>(12);
            for (SWGPlanet p : values())
                l.add(p.name);

            l.remove(l.size() - 1);

            Collections.sort(l);
            names = Collections.unmodifiableList(l);
        }
        return names;
    }

    /**
     * Sorts the specified list of planets by name, alphabetically.
     * 
     * @param planets a list of planets
     */
    public static void sort(List<SWGPlanet> planets) {
        Collections.sort(planets, new Comparator<SWGPlanet>() {

            @SuppressWarnings("synthetic-access")
            @Override
            public int compare(SWGPlanet o1, SWGPlanet o2) {
                return o1.name.compareTo(o2.name);
            }
        });
    }
}
