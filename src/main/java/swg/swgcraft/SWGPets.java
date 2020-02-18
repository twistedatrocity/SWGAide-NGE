package swg.swgcraft;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceClassTree;
import swg.crafting.resources.types.SWGCreatureFood;
import swg.crafting.resources.types.SWGCreatureResources;
import swg.crafting.resources.types.SWGCreatureStructural;
import swg.crafting.resources.types.SWGEgg;
import swg.crafting.resources.types.SWGHorn;
import swg.crafting.resources.types.SWGSeafood;
import swg.model.SWGPlanet;
import swg.tools.ZReader;
import swg.tools.ZString;

/**
 * A utility type that manages harvesting information which it obtains from from
 * SWGPets.com. For a specified resources class this type provides two services
 * for clients in SWGAide: an URL for SWGPets.com that in a browser displays the
 * wanted information, and a string that lists the creature types that the class
 * can be harvested off.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGPets {

    /**
     * A constant that is used with a resource class for which it exists no
     * creatures in the worlds that it is harvested from.
     */
    public static final String NONE = "none";

    /**
     * A collection that maps from resource class to a string of creatures from
     * which the resource can be harvested from.
     */
    private static Map<SWGResourceClass, String> rcToCreatures;

    /**
     * A base URL for a resource query at SWGPets.com.
     */
    private static final String URL = "http://www.swgpets.com/creatures?";

    /**
     * A format string for an URL that is a query for a particular resource type
     * at a specified planet. The arguments for "%s%s=%s&planet=%s" is in order:
     * {@link #URL}, base-type, specified type, and planet.
     */
    private static final String URL_FORMAT_PLANET = "%s%s=%s&planet=%s";

    /**
     * Creates an instance of this type; should only be instantiated once.
     */
    private SWGPets() {
        if (rcToCreatures != null)
            throw new IllegalStateException("Already created");
    }

    /**
     * Helper method which returns a list of trimmed strings, one per line of
     * the content from the specified URL. The strings trimmed but no more, in
     * particular the list is not sorted.
     * 
     * @param u an URL
     * @return a list of trimmed strings
     * @throws IOException if there us an error
     */
    private static List<String> content(URL u) throws IOException {
        HttpURLConnection uc = (HttpURLConnection) u.openConnection();
        ZReader sr = ZReader.newTextReader(uc.getInputStream());

        List<String> ret = new ArrayList<String>(128);
        for (String line : sr.lines(true, true))
            ret.add(line);
        
        sr.close();
        uc.disconnect();
        return ret;
    }

    /**
     * Returns a string of creature types which drops the the specified resource
     * class, or an informative string.
     * 
     * @param rc a creature resource class
     * @return a string of creature types, or an informative string
     * @throws NullPointerException if an argument is {@code null}
     */
    public static String creatures(SWGCreatureResources rc) {
        if (rcToCreatures == null || rcToCreatures.isEmpty())
            initRCMap();

        if (!rc.isSpawnable()) return "Cannot spawn";

        String ret = rcToCreatures.get(rc);
        if (ret != null) return ret;

        if (rc.isSub(SWGEgg.class)) return "any lair";
        if (rc.isSub(SWGSeafood.class)) return "any water pond";
        if (rc.isSub(SWGHorn.class)) return "not called for in schematics";
        return "error";
    }

    /**
     * Returns a string of creature types that the specified resource class can
     * be harvested from. If no creature yields the specified resource class
     * "none" is returned. If there is an error it is printed to SWGAide's log
     * file and this method returns {@code null}.
     * <p>
     * The resource class must match {@link SWGResourceClass#isSpawnable()} but
     * not necessarily be possible to harvest; it must also match the planet.
     * <p>
     * The resources under Egg, Horn, or Seafood always yields the empty string;
     * this kind is always harvested from any lair or pond of water.
     * 
     * @param rc a creature resource class
     * @return a string of creature types, an empty string, or {@code null}
     * @throws IllegalArgumentException if the class is not spawnable or
     *         mismatch with the planet
     * @throws NullPointerException if an argument is {@code null}
     */
    private static String creaturesDownload(SWGCreatureResources rc) {
        try {
            if (rc.isSub(SWGEgg.class) || rc.isSub(SWGHorn.class) ||
                    rc.isSub(SWGSeafood.class)) return "";

            URL u = urlFor(rc);
            List<String> lines = content(u);

            int row = 0; // scan until <form name='swgpets' >> row-nbr
            for (; row < lines.size(); ++row)
                if (lines.get(row).startsWith("<form name='swgpets'"))
                    break;

            String line = lines.get(row);
            if (line.contains("No creatures matching Search")) return NONE;

            // find each pet in the line and add to temporary list
            // <span class='gen'><nobr><a href="/creature/female+tybis"><b>
            String find = "href=\"/creature/";
            List<String> temp = new ArrayList<String>();
            int stop, idx = 0;
            while ((idx = line.indexOf(find, idx)) > 0) {
                idx = line.indexOf("<b>", idx) + 3;
                stop = line.indexOf("</b>", idx);
                if (stop > idx + 1) { // avoid trailing number
                    String cn = ZString.tac(line.substring(idx, stop));
                    if (!temp.contains(cn)) temp.add(cn);
                }
            }
            return temp.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Helper method which initiates the map of creature resources and the
     * creature kinds they are harvested off of. If there is an error it is
     * written to SWGAide's log file and this method exits, the map is left in
     * an undefined state.
     */
    private static void initRCMap() {
        if (rcToCreatures == null)
            rcToCreatures = new HashMap<SWGResourceClass, String>();

        ZReader sr = ZReader.newTextReader(
                    SWGPets.class.getResourceAsStream(
                            "swgpets_creature_info.txt"));
        if (sr == null) return;

        List<String> sl = sr.lines(true, true);
        sr.close();
        for (String line : sl) {
            // sample: Endorian Domesticated Milk : Bolle Bol Calf, Bordok
            String[] split = line.split(":");

            SWGResourceClass rc = SWGResourceClass.rc(split[0].trim());
            String txt = split[1].trim();
            txt = txt.equals(NONE)
                        ? NONE
                        : txt;

            rcToCreatures.put(rc, txt);
        }
    }

    /**
     * Helper method which iterates over the specified list and for each element
     * prints the type of creatures that drops that resource class. For each
     * element, if it cannot spawn in the worlds this method recursively invokes
     * itself with the subclasses of the element, otherwise this method invokes
     * {@link #creaturesDownload(SWGCreatureResources)} and prints the return
     * value which is a string with the creatures that drop the specified class.
     * <p>
     * This method skips Egg and Seafood.
     * 
     * @param crs a list of resource classes, they must be for creatures
     * @throws ClassCastException if the elements of the list are not creature
     */
    private static void iterateOver(List<SWGResourceClass> crs) {
        for (SWGResourceClass el : crs) {
            SWGCreatureResources cr = (SWGCreatureResources) el;

            if (cr.isSub(SWGEgg.class) || 
                    cr.isSub(SWGSeafood.class)) continue;

            if (cr.isSpawnable()) {
                String s = creaturesDownload(cr);
                System.err.println(String.format(
                            "%s : %s", cr.rcName(), s));
            } else
                iterateOver(SWGResourceClassTree.getChildren(cr));
        }
    }

    /**
     * This method generates the content that is used to obtain the original
     * content to be used for swgpets_creature_info.txt which is located nearby.
     * However, this method does not do this automatically but the output is
     * manually merged for shorter lines, this is because we just want he MAIN
     * creature kind listed, not each species by itself.
     * 
     * @param args unused
     */
    public static void main(String[] args) {
        // try {
        // @SuppressWarnings("unused")
        // // instantiate and keep reference while processing.
        // SWGResourceClassTree t = new SWGResourceClassTree();
        //
        // SWGResourceClass rc = SWGCreatureResources.getInstance();
        //
        // List<SWGResourceClass> crs = SWGResourceClassTree.getChildren(rc);
        // iterateOver(crs);
        // } catch (Throwable e) {
        // System.err.print(e.getMessage());
        // }
        //
        // System.err.print(creatures(SWGDantooineAvianBones.getInstance()));
        iterateOver(null);
    }

    /**
     * Helper method which returns a planet for the specified resource class.
     * 
     * @param cr a resource class
     * @return a planet constant
     */
    private static SWGPlanet planet(SWGCreatureResources cr) {
        return SWGPlanet.fromAbbrev(cr.rcName().substring(0, 4));
    }

    /**
     * Returns an URL for SWGPets.com for the argument, or {@code null}.
     * <p>
     * <b>Notice:</b> for recycled resource classes, Egg, and Seafood this
     * method returns {@code null}. Egg are obtained from any lair wherever, and
     * Seafood is harvested in any water pond. If the resource class is generic
     * a proper link is returned, otherwise is is for the planet that the
     * resource class pertains to.
     * 
     * @param rc a creature resource class
     * @return an URL for the class, or {@code null}
     * @throws MalformedURLException if there is an error
     * @throws NullPointerException if the argument is {@code null}
     */
    public static URL urlFor(SWGCreatureResources rc)
            throws MalformedURLException {

        /*
         * and this method returns a short info string. Egg and Seafood are
         * harvested wherever, and Horn is currently unused.
         */

        if (rc == SWGCreatureResources.getInstance()
                || rc == SWGCreatureFood.getInstance()
                || rc == SWGCreatureStructural.getInstance())
            return new URL(URL);

        if (rc.isSpaceOrRecycled()
                || rc.isSub(SWGEgg.class)
                || rc.isSub(SWGHorn.class)
                || rc.isSub(SWGSeafood.class))
            return null;

        // Milk
        // Domesticated Milk
        // Corellian Domesticated Milk
        String[] split = rc.rcName().split(" ");

        // type is always last
        String type = split[split.length - 1].toLowerCase(Locale.ENGLISH);
        type = type.startsWith("bone")
                // class name is Bones if planetary
                ? "bone"
                : type;

        String spec = null;
        if (split.length > 1) {

            // handle SWGPets oddities

            // spec is always second last, if any
            spec = split[split.length - 2].equals("Animal")
                    ? "Mammal"
                    : split[split.length - 2];
            spec = spec.equals("Bristley")
                    ? "Bristly"
                    : spec;
        }

        if (!rc.isSpawnable()) {
            // http://www.swgpets.com/creatures?meat
            // http://www.swgpets.com/creatures?milk=Domesticated
            if (spec == null)
                return new URL(String.format("%s%s", URL, type));
            return new URL(String.format("%s%s%s%s", URL, type, "=", spec));
        }

        // otherwise planetary
        // http://www.swgpets.com/creatures?milk=Domesticated&planet=Dantooine

        SWGPlanet p = planet(rc);
        String planet = p == SWGPlanet.YAVIN4
                ? "Yavin4"
                : p.getName();

        String un = String.format(URL_FORMAT_PLANET, URL, type, spec, planet);
        return new URL(un);
    }
}
