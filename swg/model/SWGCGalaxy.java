package swg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import swg.SWGConstants;

/**
 * This constant uniquely identifies a galaxy within SWG. Compare with
 * {@link SWGGalaxy} which is a wrapper type which contains characters for a
 * specific {@link SWGStation}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public enum SWGCGalaxy {

    // Do not sort this type, the order of the constants is important

    /**
     * A constant for Ahazi.
     */
    AHAZI("Ahazi", 1, true),

    /**
     * A constant for Bloodfin.
     */
    BLOODFIN("Bloodfin", 2, true),

    /**
     * A constant for Bria.
     */
    BRIA("Bria", 3, true),

    /**
     * A constant for Chilastra.
     */
    CHILASTRA("Chilastra", 4, true),

    /**
     * A constant for Chimaera.
     */
    CHIMAERA("Chimaera", 5, true),

    /**
     * A constant for Corbantis.
     */
    CORBANTIS("Corbantis", 6, false),

    /**
     * A constant for Eclipse
     */
    ECLIPSE("Eclipse", 7, true),

    /**
     * A constant for FarStar.
     */
    FARSTAR("FarStar", 8, true),

    /**
     * A constant for Flurry
     */
    FLURRY("Flurry", 9, true),

    /**
     * A constant for Gorath.
     */
    GORATH("Gorath", 10, true),

    /**
     * A constant for Infinity.
     */
    INFINITY("Infinity", 11, false),

    /**
     * A constant for Intrepid.
     */
    INTREPID("Intrepid", 12, false),

    /**
     * A constant for Kauri.
     */
    KAURI("Kauri", 13, false),

    /**
     * A constant for Kettemoor.
     */
    KETTEMOOR("Kettemoor", 14, false),

    /**
     * A constant for Lowca.
     */
    LOWCA("Lowca", 15, false),

    /**
     * A constant for Naritus.
     */
    NARITUS("Naritus", 16, false),

    /**
     * A constant for Radiant.
     */
    RADIANT("Radiant", 17, true),

    /**
     * A constant for Scylla.
     */
    SCYLLA("Scylla", 18, false),

    /**
     * A constant for Shadowfire.
     */
    SHADOWFIRE("Shadowfire", 19, true),

    /**
     * A constant for Starsider.
     */
    STARSIDER("Starsider", 20, true),

    /**
     * A constant for Sunrunner.
     */
    SUNRUNNER("Sunrunner", 21, true),

    /**
     * A constant for Tarquinas.
     */
    TARQUINAS("Tarquinas", 22, false),

    /**
     * A constant for Tempest.
     */
    TEMPEST("Tempest", 23, false),

    /**
     * A constant for Valcyn.
     */
    VALCYN("Valcyn", 24, false),

    /**
     * A constant for Wanderhome.
     */
    WANDERHOME("Wanderhome", 25, false),

    /**
     * A constant for SWGCraft.co.uk.
     */
    SWGCRAFT_CO_UK("SWGCraft.co.uk", 99, false),

    /**
     * A constant for TCPrime.
     */
    TCPRIME("TCPrime", 100, true),

    /**
     * A constant for TestCenter.
     */
    TESTCENTER("TestCenter", 101, true);

    /**
     * A list of the planet names.
     */
    private static List<String> names;

    /**
     * Denotes if this galaxy constant is an active galaxy; {@code false}
     * denotes that it is closed down.
     */
    private final boolean active;

    /**
     * The SWGCraft galaxy ID for this constant.
     */
    private final int id;

    /**
     * The name of this galaxy constant.
     */
    private final String name;

    /**
     * @param name
     *            the name of the galaxy
     * @param id
     *            the SWGCraft galaxy ID
     * @param active
     *            {@code false} if the galaxy is closed down
     */
    private SWGCGalaxy(String name, int id, boolean active) {
        this.name = name;
        this.id = id;
        this.active = active;
    }

    /**
     * Returns the proper name for this galaxy constant. This method returns the
     * shorter name for the European galaxies, and TestCenter which is spelled
     * Test Center at SWGCraft.org.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the complete name for this galaxy constant. This method returns a
     * name such as Europe-Chimaera rather than Chimaera.
     * 
     * @return the full name
     */
    public String getNameComplete() {
        if (this == CHIMAERA)
            return "Europe-Chimaera";
        if (this == FARSTAR)
            return "Europe-Farstar";
        if (this == INFINITY)
            return "Europe-Infinity";
        return name;
    }

    /**
     * Returns the SWGCraft galaxy ID for this constant. This is the integer
     * which is used in SOAP actions with SWGCraft.org.
     * 
     * @return the id
     */
    public int id() {
        return id;
    }

    /**
     * Returns {@code true} if this galaxy constant is an active server, {@code
     * false} if it is shut down by SOE.
     * 
     * @return {@code false} if this galaxy is shut down
     */
    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns a galaxy constant for the specified SWGCraft galaxy ID.
     * 
     * @param id
     *            a SWGCraft galaxy ID
     * @return a galaxy constant
     * @throws IndexOutOfBoundsException
     *             if the argument is not valid
     */
    public static SWGCGalaxy fromID(int id) {
        // first handle the irregular ID numbers
        if (id == 99)
            return SWGCRAFT_CO_UK;
        if (id == 100)
            return TCPRIME;
        if (id == 101)
            return TESTCENTER;

        return values()[id - 1];
    }

    /**
     * Returns the galaxy constant for the specified galaxy name.
     * 
     * @param galaxy
     *            a proper galaxy name
     * @return the galaxy constant for the specified name
     * @throws IllegalArgumentException
     *             if the name is invalid
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public static SWGCGalaxy fromName(String galaxy) {

        String g = galaxy.toUpperCase(Locale.ENGLISH);

        // handle variations first
        if (g.equals("EUROPE-CHIMAERA"))
            return CHIMAERA;
        if (g.equals("EUROPE-FARSTAR"))
            return FARSTAR;
        if (g.equals("EUROPE-INFINITY"))
            return INFINITY;
        if (g.startsWith("SWGCRAFT"))
            return SWGCRAFT_CO_UK;
        if (g.startsWith("TEST"))
            return TESTCENTER;

        return valueOf(g);
    }

    /**
     * Determines whether the argument is a valid galaxy name in SWG. The
     * argument must be a proper, capitalized galaxy name.
     * 
     * @param name
     *            the name to check
     * @return {@code true} if the argument is a valid and correctly capitalized
     *         name of a galaxy, {@code false} otherwise
     */
    public static boolean isNameValid(String name) {
        if (name != null) {
            if (name.equals("Europe-Chimaera") //
                || name.equals("Europe-FarStar") //
                || name.equals("Europe-Infinity") //
                || name.equals("SWGCraft.co.uk") //
                || name.equals("Test Center") //
                || name.equals("TestCenter"))
                return true;
            for (SWGCGalaxy g : values())
                if (name.equals(g.name))
                    return true;
        }
        return false;
    }

    /**
     * Returns a list of the names of all the galaxies in SWG. The list also
     * includes also galaxies that are shut down. The list is immutable, see
     * {@link Collections#unmodifiableList(List)}.
     * 
     * @return a list of galaxy names
     */
    public static List<String> names() {
        if (names == null) {
            List<String> l = new ArrayList<String>(28);
            for (SWGCGalaxy g : values())
                if (g != SWGCRAFT_CO_UK || SWGConstants.DEV_DEBUG)
                    l.add(g.name);
            names = Collections.unmodifiableList(l);
        }
        return names;
    }

    /**
     * Returns a list of the names of the galaxies that are active in SWG. not
     * shut down.
     * 
     * @return a list of names of galaxies that are active
     */
    public static List<String> namesActive() {
        List<String> ret = new ArrayList<String>(16);
        for (SWGCGalaxy g : values())
            if (g.active || (g == SWGCRAFT_CO_UK && SWGConstants.DEV_DEBUG))
                ret.add(g.name);
        return ret;
    }

    /**
     * Returns a proper galaxy name for {@code abbreviation}, or {@code null} if
     * none is found or if ambiguity is found
     * 
     * @param abbreviation an abbreviation of a galaxy name
     * @return a proper galaxy name for {@code abbreviation}, or {@code null} if
     *         none is found or if ambiguity is found
     */
    public static String properName(String abbreviation) {
        if (abbreviation == null)
            return null;
    
        String abbr = abbreviation.toLowerCase(Locale.ENGLISH);
        List<String> gxyNames = names();
    
        if (abbr.startsWith("euro"))
            abbr = abbr.substring(abbr.indexOf('-') + 1);
    
        String ret = null;
        for (String gn : gxyNames) {
            String g = gn.toLowerCase(Locale.ENGLISH);
            if (g.startsWith(abbr)) {
                // We must care for galaxy names with similar beginning
                if (ret == null)
                    ret = gn;
                else
                    return null; // double found,
            }
        }
        return ret;
    }
}
