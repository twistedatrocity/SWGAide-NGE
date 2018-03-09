package swg.crafting.schematics;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.SWGAide;
import swg.SWGConstants;
import swg.crafting.Quality;
import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.crafting.resources.SWGResourceClass;
import swg.gui.SWGFrame;
import swg.model.SWGProfession;
import swg.model.SWGProfessionLevel;
import swg.swgcraft.SWGCraftCache;
import swg.swgcraft.SWGCraftCache.CacheUpdate;
import swg.swgcraft.SWGCraftCache.CacheUpdate.UpdateType;
import swg.tools.ZNumber;
import swg.tools.ZReader;
import swg.tools.ZXml;

/**
 * This type manages schematics and categories. Schematics are based on the file
 * "schem.xml" and categories are based on the file "categories.xml". The XML
 * documents are obtained from and maintained by {@link SWGCraftCache}; both
 * files are provided by SWGCraft.org.
 * <p>
 * The schematic objects and their sub-objects are light-weight wrappers which
 * contain the smallest possible data set; the top-level {@link SWGSchematic}
 * contains references to the different supporting types.
 * <p>
 * This type must be instantiated only once by {@link SWGFrame} and that must be
 * done <i>after &nbsp;</i>instantiation of {@link SWGProfessionManager} because
 * schematics references elements provided by that manager. Until this type is
 * instantiated the return values from the static getter methods are undefined.
 * <p>
 * This type maintains two collections and it provides methods to derive subsets
 * of the two:
 * <ul>
 * <li>a set of schematics uniquely identified by their integer ID</li>
 * <li>a set of categories uniquely identified by their integer ID</li>
 * </ul>
 * A schematic often references yet other schematics or categories for its
 * component slots. A category always references one or several schematics;
 * caution must be taken to break reference loops.
 * <p>
 * This type does not implement or support {@code Serializable} but populates
 * itself when it is instantiated. Clients that must save a persistent reference
 * to a schematic or a category would rather save their unique integer IDs.
 * <p>
 * This type implements {@link UpdateSubscriber}; if an updated XML is available
 * this type is notified which makes it re-populate itself. Finally clients that
 * subscribe for notifications from this type are notified about the change.
 * Once this type has re-populated itself the behavior is undefined if clients
 * mix old and new references; they should purge any temporary references when
 * they receive an update notification.
 * <p>
 * This type is thread safe and it synchronizes on smallest possible scope.
 * <p>
 * <b>Notice:</b> For the access methods the general rule is that return values
 * and their content are <b>read-only</b> unless the opposite is documented.<br/>
 * In particular, collections obtained from schematics, or their supporting
 * types, are always read-only. This type may return collections of various
 * kinds which in themselves are not read-only, such collections may be sorted,
 * added to or removed from, but the contained elements are read-only, and the
 * return values they provide.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
/**
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGSchematicsManager implements UpdateSubscriber {

    /**
     * A map of categories, mapped by the proper name of the category.
     */
    private static List<SWGCategory> categories;

    /**
     * An array of integer pairs that translates a schematic ID to the minimum
     * level to claim it from the expertise system. Remember that one expertise
     * point is given every 2 skill points. An example, the schematics for elite
     * harvesters and their components are unlocked when the character spend 16
     * points in "Structure". Hence, 16 points plus the new box in itself:
     * {@code (16 + 1) * 2 = 34} which is the minimum skill level for these
     * expertise schematics.
     * <p>
     * The structure for this constant is {@code [[ID, minLevel], ...]} and if
     * as schematic ID is not listed it is either unknown or it is not an ID for
     * an expertise schematic.
     */
    // DEVELOPER_NOTE: update this array for changes to the expertise
    private static final int[][] EXPERTISE_TO_MINIMUM_LEVEL =
    {
            // Reverse Engineering
            { 691, 34 }, // Socket Retrofitting Tool
            // Domestic
            { 1401, 34 }, // Multisaccharide Dimate
            { 1403, 34 }, // Hyper Yeast Additive
            { 1404, 34 }, // Micronutrient Supplement
            { 1406, 34 }, // Hyper Yeast Concentrate
            { 1407, 34 }, // Multisaccharide Tetramate
            { 1408, 34 }, // Broad-spectrum Nutrients
            { 1433, 34 }, // Multisaccharide Pentamate
            { 1434, 34 }, // Edible Nano Constructors
            { 1435, 34 }, // Edible Nano Constructors
            // Engineer
            { 1415, 34 }, // Deed for: Battle Droid
            { 1416, 34 }, // Chassis - Battle Droid
            { 1417, 34 }, // Chassis - Droideka Droid
            { 1418, 34 }, // Deed for: Droideka
            { 1506, 34 }, // Deed for: Mining Droid MK3
            { 1507, 34 }, // Chassis - Mining Droid MK3
            { 1508, 34 }, // Module - Droid Data 7
            { 1509, 34 }, // Module - Droid Item Storage 7
            { 1766, 10 }, // Antilles BioGen 980 Series Left Arm
            { 1767, 10 }, // Antilles BioGen 980 Series Left Forearm
            { 1768, 10 }, // Antilles BioGen 980 Series Left Hand
            { 1769, 10 }, // Antilles BioGen 980 Series Right Arm
            { 1770, 10 }, // Antilles BioGen 980 Series Right Forearm
            { 1771, 10 }, // Antilles BioGen 980 Series Right Hand
            // Structure Trader
            { 1632, 34 }, // Advanced Turbo Fluidic Drilling Pump Unit
            { 1633, 34 }, // Advanced Ore Mining Unit
            { 1634, 34 }, // Deed for: Elite Mineral Mining Installation
            { 1635, 34 }, // Deed for: Elite Deep Crust Chemical Extractor
            { 1636, 34 }, // Deed for: Elite Efficiency Moisture Vaporator
            { 1637, 34 }, // Advanced Heavy Harvesting Mechanism
            { 1638, 34 }, // Deed for: Elite Flora Farm
            { 1639, 34 }, // Deed for: Elite Natural Gas Processor
            { 1842, 34 }, // Huge Cargo Hold (POB only)
            { 1843, 34 }, // Huge Cargo Hold (Starfighter)
            { 1844, 34 }, // Mark VI Weapons Capacitor
            { 1845, 34 }, // Mark VI Fusion Reactor
            { 1846, 34 }, // Engine Overhaul - Mark I
            { 1847, 34 }, // Engine Overhaul - Mark II
            { 1848, 34 }, // Engine Stabilizer - Mark I
            { 1849, 34 } // Engine Stabilizer - Mark II
            };
    
    /**
     * A container that maps food and drink names to the buffs these provide.
     * This content of this map is parsed from {@code food_drink.txt} which is
     * based on SciGuy's FoodChart spreadsheet.
     */
    private static Map<String, String> foodDrinkBuffs;

    /**
     * A helper variable to keep track of the highest ID for a schematic
     */
    private static int highestID = 0;

    /**
     * An array of string pairs that maps non-crafted items or categories to a
     * brief description.
     */
    private static final String[][] INFO_MAP =
    {
            { "Armor Appearance Enhancement",
                    "A former loot drop, e.g. Brackaset Plates" },
            { "Armor Core Enhancement",
                    "This optional enhancement was never added to SWG" },
            { "Armor Segment Enhancement",
                    "A former loot drop, e.g. Rancor Hide" },
            { "AV-21 Power Plant",
                    "Looted from containers at the Corellian Corvette instance" },
            { "Biological-Mechanical Cartridge",
                    "This optional enhancement was never added to SWG" },
            { "Blue Wiring", "Looted in combat" },
            { "Cheap Copper Battery", "Looted in combat" },
            { "DE-10 Pistol Barrel", "Looted from Death Watch mob at Endor" },
            { "Feed Tube", "Looted in combat" },
            { "Geonosian Power Cube", "Looted from Geonosians at Yavin IV" },
            { "Geonosian Reinforcement Core",
                    "Looted from Geonosians at Yavin IV (also as schematic)" },
            { "Geonosian Sword Core", "Looted from Geonosians at Yavin IV" },
            { "Gem Accent Bead", "Looted from Nightsisters at Dathomir" },
            { "Green Accent Bead", "Looted from Nightsisters at Dathomir" },
            { "Gold Accent Bead", "Looted from Nightsisters at Dathomir" },
            { "Gold Soft Wire", "Looted from Nightsisters at Dathomir" },
            { "Jewelery Clasp", "Looted from Nightsisters at Dathomir" },
            { "Giant Dune Kimogila Scales",
                    "Looted from Giant Dune Kimogila at Lok" },
            { "Gurk King Hide", "Looted from Reclusive Gurk Kings at Lok" },
            { "Heating Element", "Looted in combat" },
            { "Heavy Duty Clasp", "Looted in combat" },
            { "Live Creature Sample",
                    "Looted while searching lairs for eggs and while foraging" },
            { "Melee Weapon Augmentation Device", "Looted in combat (yellow)" },
            { "Melee Weapon Enhancement Device", "Looted in combat (blue)" },
            { "Nak'tra Crystal", "Looted in Myyydril Caverns at Kashyyyk" },
            { "Nightsister Vibro Blade Unit",
                    "Looted from Dark Jedi Masters at Dantooine" },
            { "Peko Peko Albatross Feather",
                    "Looted from Peko Peko Albatross at Naboo" },
            { "Processor Attachments", "Looted in combat" },
            { "Pulverizer", "Looted in combat" },
            { "Ranged Weapon Augmentation Device", "Looted in combat (yellow)" },
            { "Ranged Weapon Enhancement Device", "Looted in combat (blue)" },
            { "Red Wiring", "Looted in combat" },
            { "Small Power Motor", "Looted in combat" },
            { "Spinner Blade", "Looted in combat" },
            { "Trandoshan Hunter's Rifle Barrel",
                    "Looted from Trandoshan Guards at Avatar Platform" },
            { "Woolamander Harrower Bone Fragment",
                            "Looted from Woolamander Harrowers at Yavin IV" },
            /* --- Space --- */
            { "Ancient Asteroid Chunk", "Looted in space" },
            { "Colorful Asteroid Chunk", "Looted in space" },
            { "Glowing Asteroid Chunk", "Looted in space" },
            { "Misshapen Asteroid Chunk", "Looted in space" },
            { "Strange Asteroid Chunk", "Looted in space" },
            { "Unknown/Extinct Item", "Container for unknown or extinct items" },
            /* --- Fishing --- */
            { "Bubbling stone", "Obtained while fishing" },
            { "Transparisteel Front Panel", "Obtained while fishing" },
            { "Transparisteel Left Panel", "Obtained while fishing" },
            { "Transparisteel Rear Panel", "Obtained while fishing" },
            { "Transparisteel Right Panel", "Obtained while fishing" },
            { "Tubing", "Obtained while fishing" },// Unknown/Extinct Item
            /* --- One undead item for all, see the getter --- */
            { "Destroyed Stormtrooper ",
                    "Looted from the undead in Quarantine Zone" },
            { "Rotting ", "Looted from the undead in Quarantine Zone" },
            /*--- RE-bits ---*/
            { "Modifier Bit", "Created by Reverse Engineers from junk loot" },
            { "Power Bit", "Created by Reverse Engineers from junk loot" }
    };

    /**
     * An ordered list of all schematics. The size of this list is 1 + "the
     * highest schematic integer ID". Schematics are simply indexed by their ID
     * and the slot of an unused ID is set to {@code null}.
     * <p>
     * <b>Notice:</b> The list contains {@code null} elements.
     */
    private static List<SWGSchematic> schematics;

    /**
     * The number of schematics that this manager contains.
     */
    private static int schemCount;

    /**
     * A list of clients which subscribe for notifications from this type. This
     * static list is instantiated when this type is first loaded.
     */
    private static final List<UpdateSubscriber> subscribers =
            new ArrayList<UpdateSubscriber>();

    /**
     * Creates an instance of this type. This constructor instantiates the two
     * collections and invokes helper methods which populate them based on the
     * content of an obtained XML document.
     * 
     * @throws IllegalStateException an instance already exists
     */
    public SWGSchematicsManager() {
        if (schematics != null)
            throw new IllegalStateException("Instance already exists");

        highestID = ((Integer) SWGFrame.getPrefsKeeper().get(
                "schematicsMaxID", Integer.valueOf(2000))).intValue() + 1;
        schematics = new ArrayList<SWGSchematic>(0); // dummy for now

        categories = new ArrayList<SWGCategory>(0); // dummy for now

        SWGCraftCache.addSubscriber(this, UpdateType.SCHEMATICS);

        init(); // background job
    }

    /**
     * Helper method which returns a category with the specified ID. If it does
     * not exist {@code null} is returned.
     * 
     * @param id the category ID to find
     * @param cl a list of categories
     * @return the specified category, or {@code null}
     */
    private SWGCategory catGet(int id, List<SWGCategory> cl) {
        for (SWGCategory c : cl)
            if (id == c.getID())
                return c;

        return null;
    }

    /**
     * Helper method which splits the specified line and returns an array of
     * strings. The returned object contains the category's ID, PID, and name,
     * in that order.
     * 
     * @param line a category
     * @return an array of strings
     */
    private String[] catSplitLine(String line) {
        String[] split = line.split(";");
        split[0] = split[0].trim();
        split[1] = split[1].trim();
        split[2] = split[2].trim();
        return split;
    }

    @Override
    public void handleUpdate(UpdateNotification u) {
        if (u.getClass() == CacheUpdate.class
                && (((CacheUpdate) u).type == UpdateType.SCHEMATICS
                    // treat them the same for now
                || ((CacheUpdate) u).type == UpdateType.CATEGORIES))
            init();
    }

    /**
     * Helper method which populates the collections of schematics and
     * categories. This implementation obtains documents for categories and
     * schematics from {@link SWGCraftCache}, it parses their contents in full
     * before it replaces the old collections, and finally it notifies update
     * subscribers.
     * <p>
     * This method is invoked from this type's constructor or from
     * {@link #handleUpdate(UpdateNotification)} if a new update is available.
     * The job executes on an {@link ExecutorService}.
     */
    private void init() {
        synchronized (SWGSchematicsManager.class) {
            final ExecutorService exec = Executors.newSingleThreadExecutor();
            exec.execute(new Runnable() {

                
                public void run() {
                    try {
                        initFoodDrinkMap();
                        
                        List<SWGCategory> cats = initCats();

                        List<SWGSchematic> sl = initSchems();
                        if (sl == null)
                            return;
                        initFini(sl, cats);

                        // testCategories();
                        // testPrintSchems(scs);
                        // testPrettyPrintSchems(scs);
                    } catch (Throwable e) {
                        SWGAide.printError("SWGSchematicsManager:init", e);
                    }
                    exec.shutdown();
                }
            });
        }
    }

    /**
     * Helper method which initiates and returns an unordered list of
     * categories. This method parses a local file, creates instances of
     * {@link SWGCategory}, and returns a list of those elements. The elements
     * are also updated so each category added to its parent category.
     * Furthermore, a phony category named "All" is added as the top node and so
     * is a node named "UNKNOWN" to be used by schematics with unknown category.
     * 
     * @return an unordered list of categories
     */
    private List<SWGCategory> initCats() {
        ArrayList<SWGCategory> cl = new ArrayList<SWGCategory>(300);
        cl.add(new SWGCategory("All", SWGCategory.ALL, SWGCategory.ALL));

        Document doc = SWGCraftCache.getCategories();
        if (doc != null)
            parseCats(doc, cl);
        else
            SWGAide.printDebug("schm", 1,
                    "SWGSchematicsManager:initCats: XML doc is null");

        // parse optional categories from file bundled in JAR file
        parseCatsCSV(cl);

        // for schematics with unknown category
        cl.add(new SWGCategory("UNKNOWN", Integer.MAX_VALUE, SWGCategory.ALL));

        initCatsFini(cl);

        return cl;
    }

    /**
     * Helper method which adds each category to its parent. If a category is in
     * error it is logged and this implementation continues. This method is
     * invoked after that the list of categories is populated.
     * 
     * @param cl a list of categories
     */
    private void initCatsFini(ArrayList<SWGCategory> cl) {
        for (SWGCategory cat : cl)
            try {
                if (cat.getID() > 0)
                    catGet(cat.getParentID(), cl).add(cat);
            } catch (NullPointerException e) {
                SWGAide.printDebug("schm", 1,
                        "SWGSchematicsManager:initCatsFini: no pid: "
                                + cat.toString());
            } catch (Exception e) {
                SWGAide.printError("SWGSchematicsManager:initCatsFini: error: "
                        + cat, e);
            }
    }

    /**
     * Helper method which finishes the update of this type's collections. This
     * method adds each schematic to its category and the old collections are
     * replaced with the specified lists of schematics and categories. This
     * method also updates {@link #highestID} and invokes notification of update
     * subscribers.
     * 
     * @param schems a list of schematics
     * @param cats a list of categories
     */
    private void initFini(List<SWGSchematic> schems, List<SWGCategory> cats) {
        Comparator<SWGProfessionLevel> comp =
                new Comparator<SWGProfessionLevel>() {

                    @Override
                    public int compare(SWGProfessionLevel o1,
                            SWGProfessionLevel o2) {
                        return o1.getProfession().getNameShort().compareTo(
                                o2.getProfession().getNameShort());
                    }
                };

        int sc = 0;
        for (SWGSchematic s : schems) {
            if (s != null) {
                ++sc;
                initSchem2cats(s, cats);
                Collections.sort(s.getSkillLevels(), comp);
            }
        }
        schemCount = sc;

        for (SWGCategory cat : cats) {
            if (cat.getCategories().size() > 0)
                Collections.sort(cat.getCategories());
            if (cat.getSchematics().size() > 0)
                Collections.sort(cat.getSchematics());
            if (cat.getItems().size() > 0)
                Collections.sort(cat.getItems(), new Comparator<String>() {

                    @Override
                    public int compare(String o1, String o2) {
                        return o1.compareToIgnoreCase(o2);
                    }
                });
        }

        // XXX: remove when found all spelling errors and such
        // The synchronization objects are alive until garbage collected, once
        // the collections are replaced any consecutive invocation locks on the
        // new objects. This way we do not risk concurrent exceptions in clients
        // that have used getSchematics().

        // final String padd =
        // "                                        ";
        // HashSet<String> set = new HashSet<String>(400);
        // for (SWGSchematic el : schems) {
        // if (el == null) continue;
        // for (SWGExperimentGroup eg : el.getExperimentGroups()) {
        // StringBuilder sb = new StringBuilder(256);
        //
        // // XXX: for misspelled names
        // String s = eg.getDescription();
        // if (s == null)
        // sb.append(el.getName()).append(el.getID()).append(padd);
        // else
        // sb.append(s).append(padd);
        // sb.delete(42, 999);
        //
        // for (SWGExperimentLine exl : eg.getExperimentalLines()) {
        // exl.getWeights().toString(sb);
        // sb.append("\t\t");
        // set.add(sb.toString());
        // }
        // }
        // }
        // String[] sa = set.toArray(new String[0]);
        // Arrays.sort(sa);
        // try {
        // FileWriter fw = new FileWriter("abababab.txt");
        // for (String el : sa) {
        // fw.write(el);
        // fw.write(SWGConstants.EOL);
        // }
        // fw.flush();
        // fw.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

        synchronized (schematics) {
            schematics = schems;
        }

        synchronized (categories) {
            categories = cats;
        }

        // FIXME remove when no use for this anymore
        // List<String> crl = new ArrayList<String>(128);
        // for (SWGSchematic el : schems) {
        // if (el != null && !el.getSkillLevels().isEmpty()
        // && el.getSkillLevels().get(0).
        // getProfession() == SWGProfession.STRUCTURES
        // && el.isManufacturable()
        // && el.getCrateSize()
        // ==0
        // //> 0
        // )
        // crl.add("\n" + //el.getCrateSize() + ' ' +
        // el.getName());
        // }
        // Collections.sort(crl);
        // System.err.println(crl);

        notifySubscribers();
    }

    /**
     * Helper method which populates the map of food and drink names and the
     * buffs that pertain to the name. This method uses the content of {@code
     * food_drink.txt} and for each line it adds an entry to the map.
     */
    private void initFoodDrinkMap() {
    	ZReader sr = null;
        try {
            foodDrinkBuffs = new HashMap<String, String>(113);

            sr = ZReader.newTextReaderExc(
                    SWGSchematicsManager.class.getResourceAsStream(
                            "food_drink.txt"));

            List<String> sl = sr.lines(true, true);
            for (String line : sl) {
                String[] split = line.split(";", 2);
                String k = split[0].trim();
                String v = split[1].trim();
                v = v.replaceAll("  ;  ", ", ");
                foodDrinkBuffs.put(k, v);
            }
        } catch (Throwable e) {
            SWGAide.printError("SWGSchematicsManager:initFoodDrink", e);
        } finally {
        	sr.close();
        }
    }

    /**
     * Helper method which appends the specified schematic to its category which
     * is contained in the specified list. If the specified schematic is {@code
     * null} this method does nothing. If the category does not exist the
     * schematic is added to a phony category named "UNKNOWN" which is a
     * sub-category of "All".
     * 
     * @param schem a schematic
     * @param cats a list of categories
     */
    private void initSchem2cats(SWGSchematic schem, List<SWGCategory> cats) {
        int sc = schem.getCategory();
        if (sc > 0)
            for (SWGCategory c : cats)
                if (c.getID() == sc)
                    c.add(schem);
    }

    /**
     * Helper method which obtains and parses a XML document and returns a list
     * with the schematics. This method does not associate schematics with their
     * categories. If no document is available {@code null} is returned.
     * 
     * @return a list of schematics, or {@code null}
     */
    private List<SWGSchematic> initSchems() {
        ArrayList<SWGSchematic> sl = null;
        Document doc = SWGCraftCache.getSchematics();
        if (doc != null) {
            sl = initSchemsBegin(highestID + 1);
            parseSchems(doc, sl);
        } else
            SWGAide.printDebug("schm", 1,
                    "SWGSchematicsManager:initSchems: XML doc is null");
        return sl;
    }

    /**
     * Helper method which returns a null-padded list for schematics with the
     * specified size. The list is filled with {@code null} to make it possible
     * to insert elements at any {@code index <= highestID}. The specified size
     * is supposed to be {@code highestID + 1}.
     * 
     * @param size the size for the returned list, {@code (highestID + 1)}
     * @return a null-padded list
     */
    private ArrayList<SWGSchematic> initSchemsBegin(int size) {
        ArrayList<SWGSchematic> ret = new ArrayList<SWGSchematic>(size);
        for (int i = 0; i < size; ++i)
            ret.add(null); // fill it up to get the slots

        return ret;
    }

    /**
     * Helper method which adds the specified schematic to the specified list,
     * the schematic is set at the index that equals its ID. If {@code list_size
     * <= schematic_ID} the list is enlarged and the new slots are set to
     * {@code null} to make room for indexing by ID.
     * 
     * @param s a schematic
     * @param list a list of schematics
     */
    private void initSetSchem(SWGSchematic s, ArrayList<SWGSchematic> list) {
        int id = s.getID();
        int oldSize = list.size();

        // Ensure capacity and fill with null up to ID to get the slots.
        // Possibly just one step, but this is generic to any new ID.
        if (id >= oldSize)
            for (int i = oldSize; i <= id; ++i)
                list.add(null);

        list.set(id, s);
    }

    /**
     * Helper method which notifies subscribers that an update is available.
     */
    private void notifySubscribers() {
        synchronized (subscribers) {
            CacheUpdate up = new CacheUpdate(CacheUpdate.UpdateType.SCHEMATICS);
            for (UpdateSubscriber s : subscribers)
                s.handleUpdate(up);
        }
    }

    /**
     * Helper method which parses the specified XML document and adds its
     * content to the list for categories. If there is an error it is caught and
     * a message is logged to the general log file, and this method continues to
     * parse the document.
     * 
     * @param doc the XML document to parse
     * @param cats the list to add categories to
     */
    private void parseCats(Document doc, ArrayList<SWGCategory> cats) {
        NodeList ns = doc.getChildNodes().item(0).getChildNodes();
        for (int i = 0; i < ns.getLength(); i++) {
            if (ns.item(i).getNodeType() == Node.ELEMENT_NODE) {
                try {
                    Element e = (Element) ns.item(i);
                    cats.add(new SWGCategory(e));
                } catch (Exception e) {
                    SWGAide.printDebug("schm", 1, String.format(
                            "SWGSchematicsManager:parseCats: %s : %s",
                            e.getMessage(), ZXml.stringFromAttr(
                                    (Element) ns.item(i), "name")));
                }
            }
        }
    }

    /**
     * Helper method which parses a bundled file that contains categories which
     * are appended to the specified list. This method does not replace existing
     * categories. The bundled file comes with the JAR file and is only used in
     * special cases. If the file does not exist this method does nothing.
     * 
     * @param cats a list of existing categories
     */
    private void parseCatsCSV(ArrayList<SWGCategory> cats) {
        ZReader sr = null;
    	try {
            URL u = SWGCategory.class.getResource("categories.csv");
            if (u == null)
                return;

            sr = ZReader.newTextReader(u.openStream());
            if (sr == null) return;

            List<String> sl = sr.lines(true, true);
            for (String line : sl) {
                String[] split = catSplitLine(line);
                SWGCategory c = new SWGCategory(split[2],
                        Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                if (!cats.contains(c))
                    cats.add(c);
            }
        } catch (Throwable e) {
            SWGAide.printError("SWGSchematicsManager:parseCatsCSV", e);
        } finally {
        	sr.close();
        }
    }

    /**
     * Helper method which parses the specified XML document and adds its
     * content to the null-padded list for schematics. If there is an error it
     * is caught and a message is logged to the general log file, and this
     * method continues to parse the document.
     * 
     * @param doc the XML document to parse
     * @param schems the list to add schematics to
     */
    private void parseSchems(Document doc, ArrayList<SWGSchematic> schems) {
        int highest = 0;

        NodeList ns = doc.getChildNodes().item(0).getChildNodes();
        for (int i = 0; i < ns.getLength(); ++i) {
            if (ns.item(i).getNodeType() == Node.ELEMENT_NODE) {
                try {
                    Element e = (Element) ns.item(i);
                    SWGSchematic s = new SWGSchematic(e);

                    if (s.getType().equals("No longer craftable"))
                        continue;

                    if (schems.contains(s))
                        throw new IllegalArgumentException(
                                "Schematic doubled: " + s.getID());

                    initSetSchem(s, schems);

                    highest = Math.max(highest, s.getID());

                } catch (Exception e) {
                    String s = String.format(
                            "SWGSchematicsManager:parseSchems: %s : %s",
                            e.getMessage(), ZXml.stringFromAttr(
                                    (Element) ns.item(i), "name"));
                    SWGAide.printDebug("schm", 1, s);
                    if (SWGConstants.DEV_DEBUG)
                        System.err.println(s);
                }
            }
        }

        highestID = highest;
        SWGFrame.getPrefsKeeper().add(
                "schematicsMaxID", Integer.valueOf(highestID));
    }

    /**
     * Adds the specified client to the list of subscribers. When this type is
     * updated with new content all subscribers are notified according to the
     * {@link UpdateSubscriber} interface.
     * <p>
     * This method is thread safe. Notifying executes at a background
     * {@link ExecutorService}, hence if the subscriber touches Swing GUI it
     * <b>must dispatch</b> the job on the event thread by itself.
     * 
     * @param subscriber a client interested in update notifications
     */
    public static void addSubscriber(UpdateSubscriber subscriber) {
        synchronized (subscribers) {
            subscribers.add(subscriber);
        }
    }

    /**
     * Helper method which returns the minimum skill for the specified
     * schematic, or 0 if its minimum level is unknown.
     * 
     * @param schem a schematic
     * @return minimum level for the schematic
     * @throws IllegalArgumentException if the argument is not from expertise
     */
    private static int expertiseLevel(SWGSchematic schem) {
        if (schem.getExpertise() == null)
            throw new IllegalArgumentException("Not an expertise schematic");
        int id = schem.getID();
        for (int i = 0; i < EXPERTISE_TO_MINIMUM_LEVEL.length; ++i)
            if (EXPERTISE_TO_MINIMUM_LEVEL[i][0] == id)
                return EXPERTISE_TO_MINIMUM_LEVEL[i][1];

        // else unknown expertise
        if (SWGConstants.DEV_DEBUG)
            System.err.println("unknown expertise schematic: " + id);
        return 0;
    }

    /**
     * Returns a list of schematics that match the specified string. This method
     * scans all schematics and if the specified string is found in the name of
     * a schematic it is added to the list to be returned. If no schematic is
     * found or if the specified argument is the empty string this method
     * returns an empty list.
     * <p>
     * This method is completely case insensitive, that aside only exact matches
     * are used. The returned list is not read-only.
     * 
     * @param string a text
     * @return a list of schematics
     * @throws NullPointerException if the argument is {@code null}
     */
    public static List<SWGSchematic> findSchematics(String string) {
        List<SWGSchematic> all = getSchematicsNull();
        if (all.isEmpty())
            return Collections.emptyList();

        String str = string.toLowerCase(Locale.ENGLISH);
        List<SWGSchematic> ret = new ArrayList<SWGSchematic>(64);
        for (SWGSchematic s : all)
            if (s != null
                    && s.getName().toLowerCase(Locale.ENGLISH).contains(str))
                ret.add(s);

        return ret;
    }

    /**
     * Returns a list of schematics which can be made with the specified
     * resource class, or an empty list. This method scans all schematics and if
     * one of its resource slots calls for a resource class that equals or is a
     * sub-class of the specified argument it is added to the returned list.
     * Sub-components are not inspected. The returned list is not read-only.
     * 
     * @param rClass a resource class
     * @return a list of schematics
     * @throws NullPointerException if the argument is {@code null}
     */
    public static List<SWGSchematic> findSchematics(SWGResourceClass rClass) {
        List<SWGSchematic> all = getSchematicsNull();
        if (all.isEmpty())
            return Collections.emptyList();

        List<SWGSchematic> ret = new ArrayList<SWGSchematic>(512);
        for (SWGSchematic s : all)
            if (s != null)
                for (SWGResourceSlot rs : s.getResourceSlots())
                    if (rClass.isSub(rs.getResourceClass())) {
                        ret.add(s);
                        break; // one per schematic is enough
            }

        return ret;
    }

    /**
     * Returns the buffs that the specified schematic provides, or {@code null}.
     * If the schematic is not food or drink, or if its name is not mapped to a
     * buff, this method returns {@code null}.
     * 
     * @param s a schematic
     * @return a string with buffs, or {@code null}
     */
    public static String foodDrinkBuffs(SWGSchematic s) {
        if (s != null && foodDrinkBuffs != null)
            return foodDrinkBuffs.get(s.getName());

        return null;
    }

    /**
     * Returns the number of available schematics. This is the amount that
     * managed by this type and it may possibly differ from what is exported
     * from SWGCraft.org, though that is currently not the case.
     * 
     * @return number of available schematics
     */
    public static int getAmount() {
        int ret = 0;
        if (schematics != null)
            for (SWGSchematic e : schematics)
                if (e != null) ++ret;

        return ret;
    }
    
    /**
     * Returns an unsorted list of all categories. The returned list <b>is
     * read-only</b>.
     * 
     * @return a read-only list of all categories
     */
    public static List<SWGCategory> getCategories() {
        return categories;
    }

    /**
     * Returns an category, or {@code null} if the specified ID does not exist.
     * 
     * @param id the unique ID for the category
     * @return a category, or {@code null}
     */
    public static SWGCategory getCategory(int id) {
        synchronized (categories) {
            for (SWGCategory c : categories)
                if (id == c.getID())
                    return c;

            return null;
        }
    }

    /**
     * Returns a sorted list of resource classes which are called for by the
     * schematics in the specified list, or an empty list. The returned list
     * contains just one element of each resource class, which makes it a
     * mathematical set. Furthermore, each element is called for by name by one
     * or more schematics from the argument. The sorted order is defined by
     * {@link SWGResourceClass#sortIndex()}.
     * <p>
     * If the argument is {@code null}, or if no resource class is found, this
     * method returns {@link Collections#emptyList()}. Otherwise the returned
     * list <b>is not</b> read-only.
     * 
     * @param schems a list of schematics
     * @return an ordered list of resource classes, or an empty list
     */
    public static List<SWGResourceClass> getResClasses(List<SWGSchematic> schems) {
        if (schems == null || schems.isEmpty()) return Collections.emptyList();

        List<SWGResourceClass> ret = new ArrayList<SWGResourceClass>(128);
        for (SWGSchematic e : schems)
            if (e != null) {
                for (SWGResourceSlot rs : e.getResourceSlots())
                    if (!ret.contains(rs.getResourceClass()))
                        ret.add(rs.getResourceClass());
            }
        if (ret.isEmpty()) return Collections.emptyList();

        Collections.sort(ret);
        return ret;
    }

    /**
     * Returns a schematic which has the specified ID, or {@code null} if no
     * schematic exists with that ID. The specified ID must be within the range
     * 0 to {@link #maxSchematicID()}.
     * 
     * @param id the ID for a schematic
     * @return a schematic instance, or {@code null}
     * @throws IndexOutOfBoundsException if the argument is outside the range
     *         for IDs
     */
    public static SWGSchematic getSchematic(int id) {
        synchronized (schematics) {
            if (schematics.size() <= 0)
                return null; // init failed or not run yet
            return schematics.get(id);
        }
    }

    /**
     * Returns a list of schematic names. The returned list <b>is not</b>
     * read-only.
     * 
     * @return a modifiable list names
     */
    public static List<String> getSchematicNames() {
        synchronized (schematics) {
            ArrayList<String> ret = new ArrayList<String>(schematics.size());
            for (SWGSchematic s : schematics)
                if (s != null)
                    ret.add(s.getName());

            return ret;
        }
    }

    /**
     * Returns a new, unordered list of all schematics. The returned list is
     * free to modify, and it contains no {@code null} elements.
     * 
     * @return a list of all schematics
     */
    public static List<SWGSchematic> getSchematics() {
        synchronized (schematics) {
            List<SWGSchematic> ret = new ArrayList<SWGSchematic>(schemCount);
            for (SWGSchematic s : schematics)
                if (s != null) ret.add(s);

            return ret;
        }
    }

    /**
     * Returns a sorted list of schematics which do use the specified resource
     * class, or an empty list. This method scans the specified list of
     * schematics and if any of them make use of the resource class it is added
     * to the returned list. The returned list contains just one element of each
     * schematic, which makes it a mathematical set. The boolean argument
     * determines whether the resource class is a <i>required&nbsp;</i> class,
     * or if it may be a sub-class of the required resource classes which the
     * resource slots call for. The returned list is sorted alphabetically. If
     * no schematic is found {@link Collections#emptyList()} is returned.
     * Otherwise the returned list is <b>is not</b> read-only.
     * 
     * @param schems a list of schematics to scan
     * @param rc a resource class constant
     * @param strict {@code true} to include just <i>required &nbsp;</i> classes
     * @return a sorted list of schematics, or an empty list
     * @throws NullPointerException if an argument or an element is {@code null}
     */
    public static List<SWGSchematic> getSchematics(
            List<SWGSchematic> schems, SWGResourceClass rc, boolean strict) {

        if (schems.isEmpty()) return Collections.emptyList();

        List<SWGSchematic> ret = new ArrayList<SWGSchematic>(256);
        for (SWGSchematic e : schems)
            for (SWGResourceSlot rs : e.getResourceSlots())
                if ((rs.getResourceClass() == rc
                        || (strict && rc.isSub(rs.getResourceClass())))
                        && !ret.contains(e))
                        ret.add(e);

        if (ret.isEmpty()) return Collections.emptyList();

        Collections.sort(ret);
        return ret;
    }

    /**
     * Returns a list of schematics which are awarded the specified profession.
     * If no schematic is awarded the specified profession an empty list is
     * returned. The returned list <b>is not</b> read-only.
     * 
     * @param profession a profession constant
     * @return a modifiable list of schematics, or an empty list
     * @throws NullPointerException if the argument is {@code null}
     */
    public static List<SWGSchematic> getSchematics(SWGProfession profession) {
        return getSchematics(profession, -1, SWGProfessionLevel.MAX_LEVEL);
    }

    /**
     * Returns a list of schematics which are awarded the specified profession
     * within the specified range determined by {@code [min max]}. No element in
     * the returned list is {@code null}. If there is no schematic that meets
     * the arguments an empty list is returned. The returned list <b>is not</b>
     * read-only.
     * <p>
     * If {@code min >= 0} only schematics with complete profession skill level
     * are included in the returned list. Notice that the mandatory schematics
     * Meat Jerky, Small Glass, and Spiced Tea, which all professions learn,
     * have skill level 0. Schematics with {@link SWGProfessionLevel#ERROR} have
     * level -1, which can be used by {@code min} but not {@code max}.
     * 
     * @param prof a profession constant
     * @param min the minimum profession level; {@code min <= max} and in range
     *        [0 {@link SWGProfessionLevel#MAX_LEVEL}], use -1 for unknown
     *        profession levels
     * @param max the maximum profession level, in range [-1
     *        {@link SWGProfessionLevel#MAX_LEVEL}]
     * @return a modifiable list of schematics
     * @throws IllegalArgumentException if an argument is invalid
     * @throws NullPointerException if the argument is {@code null}
     */
    public static List<SWGSchematic> getSchematics(
            SWGProfession prof, int min, int max) {
        if (prof == null)
            throw new NullPointerException("Argument is null");
        if (min < -1 || min > max)
            throw new IllegalArgumentException("Invalid min: " + min);
        if (max < 0 || max > SWGProfessionLevel.MAX_LEVEL)
            throw new IllegalArgumentException("Invalid max: " + max);

        synchronized (schematics) {
            ArrayList<SWGSchematic> ret =
                    new ArrayList<SWGSchematic>(schematics.size());

            for (SWGSchematic s : schematics)
                if (s != null) {
                    boolean found = false;
                    if (s.getExpertise() != null) {
                        for (Object[] obj : s.getExpertise())
                            if (prof.equalsProfession((SWGProfession) obj[0])) {
                                int l = expertiseLevel(s);
                                if (l <= max && l >= min)
                                    ret.add(s);
                                // one schematic is enough but because this kind
                                // of schematics are not regular and the skill
                                // level for a particular schematic is the same
                                // we shortcut the following test
                                found = true;
                                break;
                            }
                    }
                    if (!found)
                        for (SWGProfessionLevel p : s.getSkillLevels()) {
                            if (prof.equalsProfession(p.getProfession())
                                    && p.getLevel() <= max
                                    && min <= p.getLevel()) {
                                ret.add(s);
                                break; // one schematic is enough
                    }
                        }
                }
            return ret;
        }
    }

    /**
     * Returns a list of all schematics. The returned list <b>is read-only</b>.
     * <p>
     * The returned list is sorted by the schematics' unique ID numbers and it
     * is also indexed by ID. However, because not all ID numbers are in use all
     * invalid IDs are initialized to {@code null}.
     * <p>
     * <b>Notice:</b> The returned list contains {@code null} elements.
     * 
     * @return a read-only list of all schematics and {@code null} elements
     */
    public static List<SWGSchematic> getSchematicsNull() {
        return schematics;
    }

    /**
     * Collocates and returns a list of objects for the resource requirements of
     * the specified schematic. The returned list includes the resources for
     * each non-optional component slot that specifies a component or that
     * specifies a category with only one possible component; if a slot refers
     * to a category with several elements or if the component slot is optional
     * it is ignored.
     * <p>
     * If there is an error a message is written to log file and {@code null} is
     * returned.
     * 
     * @param schem a schematic
     * @return a list of resource class elements, or {@code null}
     */
    public static List<ResourceAmount> getShopping(SWGSchematic schem) {
        try {
            return getShoppingHelper(schem, 1);
        } catch (Exception e) {
            String s = String.format(
                    "Error, report the following at SWGCraft.org:%n"
                            + "Schematic name: %s%nSchematic ID: %s%n"
                            + "In SWGAide:getShoppingList%nThanks",
                    schem.getName(), Integer.toString(schem.getID()));
            SWGAide.printDebug("schm", 1, s);
            if (SWGConstants.DEV_DEBUG)
                System.err.println(schem);
            return null;
        }
    }

    /**
     * Helper method for {@link #getShoppingHelper(SWGSchematic, int)}. If the
     * specified category contains more or less than one element this method
     * returns an empty list, otherwise it invokes the get-shopping-helper
     * method for the schematic or for the category that is contained.
     * 
     * @param c a category
     * @param m a multiplier
     * @return a list of resource class elements, or an empty list
     */
    private static List<ResourceAmount> getShoppingHelper(SWGCategory c, int m) {
        if (c.getSchematics().size() + c.getCategories().size() != 1)
            return Collections.emptyList();

        if (c.getSchematics().size() > 0)
            return getShoppingHelper(c.getSchematics().get(0), m);

        return getShoppingHelper(c.getCategories().get(0), m);
    }

    /**
     * Helper method for {@link #getShopping(SWGSchematic)} which multiplies the
     * result with the specified amount.
     * 
     * @param s a schematic
     * @param m a multiplier
     * @return a list of resource class elements
     */
    private static List<ResourceAmount> getShoppingHelper(SWGSchematic s, int m) {
        List<ResourceAmount> ret = new ArrayList<ResourceAmount>();

        for (SWGResourceSlot rs : s.getResourceSlots()) {
            SWGResourceClass rc = rs.getResourceClass();
            int i = ResourceAmount.indexOf(ret, rc);
            if (i >= 0)
                ret.get(i).addUnits(rs.getUnits() * m);
            else
                ret.add(new ResourceAmount(rc, rs.getUnits() * m));
        }

        for (SWGComponentSlot cs : s.getComponentSlots()) {
            if (cs.isOptional() || cs.getType().equals("item"))
                continue;
            if (cs.getType().equals("schematic"))
                getShoppingMerge(getShoppingHelper(
                        getSchematic(cs.getSchematicId()), cs.getAmount() * m),
                        ret);
            else
                getShoppingMerge(getShoppingHelper(
                        getCategory(cs.getCategoryId()), cs.getAmount() * m),
                        ret);
        }

        return ret;
    }

    /**
     * Helper method which adds or merges all elements in the source list to the
     * target list. If elements with equal resource class exist in both lists
     * the amount is updated in the target list but the element is not added.
     * 
     * @param source the source list
     * @param target the target list
     */
    private static void getShoppingMerge(
            List<ResourceAmount> source, List<ResourceAmount> target) {
        for (ResourceAmount ra : source) {
            SWGResourceClass rc = ra.getResourceClass();
            int i = ResourceAmount.indexOf(target, rc);
            if (i >= 0)
                target.get(i).addUnits(ra.getUnits());
            else
                target.add(ra);
        }
    }

    /**
     * Returns brief information for the specified item or category, or {@code
     * null} if no such item is found.
     * 
     * @param name the proper name of the item
     * @return informative text, or {@code null}
     */
    public static String informationText(String name) {
        if (name != null)
            for (int i = 0; i < INFO_MAP.length; ++i)
            if (name.startsWith(INFO_MAP[i][0]))
                return INFO_MAP[i][1];

        return null;
    }

    /**
     * Determines if the specified schematic is a HQ schematic. This method
     * returns {@code false} if the schematic is tagged as LQ or if it has no
     * experimenting properties. Otherwise this method returns {@code true},
     * also for a schematic which which is tagged as "mixed" or unknown.
     * <p>
     * Hence, {@code true} may be a false positive due to incomplete
     * information. For a schematic which is {@link Quality#MIXED}, see
     * {@link SWGSchematicsManager#isQuality(SWGSchematic, SWGSchematic)}.
     * 
     * @param s a schematic
     * @return {@code false} if the schematic is not HQ
     * @throws NullPointerException if the argument is {@code null}
     */
    public static boolean isQuality(SWGSchematic s) {
        if (s.quality == Quality.LQ || s.getExperimentGroups().isEmpty())
            return false;
    
        return true;
    }

    /**
     * Determines if the specified component must be HQ when used in the
     * specified schematic. This method returns {@code false} if any of the two
     * is tagged as LQ or if any of the two lacks experimenting properties. This
     * method returns {@code true} if the component is tagged as "mixed" and it
     * is associated with {@code usedIn} as a HQ item. Otherwise, if the
     * component is not required as HQ in {@code usedIn} this method returns
     * {@code false}.
     * <p>
     * A map of HQ-associations of <tt>comp<->hq-user</tt> is managed by this
     * controller TODO . This may change if SWGCraft.org in the future provides
     * these adjacencies.
     * 
     * @param comp a schematic which is a component in {@code usedIn}
     * @param usedIn a schematic that contains {@code comp}
     * @return {@code false} if the schematic is not HQ
     */
    static boolean isQuality(SWGSchematic comp, SWGSchematic usedIn) {
        if (!isQuality(comp) || !isQuality(usedIn))
            return false;
    
        // TODO: add info for known use of "mixed"
        return true;
    }

    /**
     * Determines of if the specified category is a non-crafted item or a
     * category for looted or extinct items.
     * 
     * @param c a category
     * @return {@code true} if the category is "special"
     */
    public static boolean isSpecial(SWGCategory c) {
        if (c == null) return false;
        int cid = c.getID();
        return (cid == 433 || cid == 476 || cid == 479 || cid == 481
                || cid == 483 || cid == 579 || cid == 580 || cid == 626);
    }

    /**
     * Returns the schematic ID that is greatest of all known schematics. The
     * returned value is a unique SWGCraft ID for an existing schematic, all IDs
     * are greater than 0 and to the returned value. All integers in this range
     * no longer map to an existing schematic, hence {@link #getSchematic(int)}
     * returns {@code null} for non-existing schematics.
     * 
     * @return the greatest ID for a known schematic
     */
    public static int maxSchematicID() {
        return highestID;
    }

    // /**
    // * /** For testing purposes only
    // *
    // * @param args
    // */
    // public static void main(String... args) {
    // SWGCraftCache.updateCache();
    // SWGSchematicsManager sm = new SWGSchematicsManager();
    // List<SWGSchematic> sl = sm.initSchems();
    // List<SWGCategory> cl = sm.initCats();
    // for (SWGCategory c : cl)
    // System.err.println(c);
    // for (SWGSchematic s : sl)
    // if (s != null)
    // System.err.println(s);
    //
    // }

    // /**
    // * Testing method only
    // */
    // private static void testCategories() {
    // List<String> lk = new ArrayList<String>(getCategoryNames());
    // Collections.sort(lk);
    // for (String k : lk)
    // System.out.println(k);
    // 
    // System.out.println("size="+lk.size());
    // }

    // /**
    // * Testing method only
    // *
    // * @param list
    // */
    // private static void testPrintSchems(List<SWGSchematic> list) {
    // int i = 0, j = 0;
    // for (SWGSchematic s : list) {
    // if (s != null)
    // System.out.println(s.toString());
    // else {
    // System.out.println("empty slöt id=" + i);
    // ++j;
    // }
    // ++i;
    // }
    // System.out.println("size=" + list.size());
    // System.out.println("null=" + j);
    // }

    // /**
    // * Testing method only
    // *
    // * @param list
    // */
    // private static void testPrettyPrintSchems(List<SWGSchematic> list) {
    // try {
    // FileWriter fw = new FileWriter("schem_pp.txt");
    // for (SWGSchematic s : list)
    // if (s != null) {
    // fw.write(s.prettyPrint());
    // fw.write(SWGConstants.EOL);
    // }
    // fw.flush();
    // fw.close();
    // } catch (IOException e) {
    // Auto-generated catch block
    // e.printStackTrace();
    // }
    // }

    /**
     * Removes the specified client from the list of subscribers. If the client
     * is not found this method does nothing. This method is thread safe.
     * 
     * @param subscriber a non-subscriber
     */
    public static void removeSubscriber(UpdateSubscriber subscriber) {
        synchronized (subscribers) {
            subscribers.remove(subscriber);
        }
    }

    /**
     * Returns the number of schematics currently maintained by this manager. If
     * this manager is not instantiated the return value is 0.
     * 
     * @return number of schematics
     */
    public static int schematicsCount() {
        return schemCount;
    }

    /**
     * Determines if the first schematic is used as a component by the second
     * schematic and returns the amount. This method iterates over the component
     * slots of the second schematic and accumulates the amount it is either
     * directly called for, or if a category contains the specified schematic.
     * In both cases it must be used first-hand, not as a sub-component of a
     * component.
     * 
     * @param s1 a suggested component
     * @param s2 a schematic
     * @return the amount of s1 that is used first-hand in s2, or 0
     */
    public static int schemCompAmount(SWGSchematic s1, SWGSchematic s2) {
        int ret = 0;
        for (SWGComponentSlot cs : s2.getComponentSlots())
            if (cs.getType().equals("schematic")
                    && getSchematic(cs.getSchematicId()) == s1)
                ret += cs.getAmount();
            else if (cs.getType().equals("category"))
                for (SWGSchematic s : getCategory(cs.getCategoryId()).getSchematics())
                    if (s == s1) ret += cs.getAmount();

        return ret;
    }

    /**
     * This is a helper type that conveys the number of units of the specified
     * resource class.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public static final class ResourceAmount {

        /**
         * The resource class for this instance.
         */
        private final SWGResourceClass rc;

        /**
         * The accumulated amount of the resource class.
         */
        private int units = 0;

        /**
         * Creates an instance of this type. The specified resource class is
         * final but the initial amount can be added to.
         * 
         * @param rc a resource class
         * @param units the initial amount or zero
         * @throws NullPointerException if resource class is {@code null}
         */
        public ResourceAmount(SWGResourceClass rc, int units) {
            if (rc == null)
                throw new NullPointerException("Resource class is null");
            this.rc = rc;
            this.addUnits(units);
        }

        /**
         * Adds the specified amount to this instance.
         * 
         * @param u the amount to add
         * @throws IllegalArgumentException if the argument is negative
         */
        public void addUnits(int u) {
            if (u < 0)
                throw new IllegalArgumentException("Amount is negative");
            units += u;
        }

        /**
         * Returns the resource class for this instance.
         * 
         * @return the resource class
         */
        public SWGResourceClass getResourceClass() {
            return rc;
        }

        /**
         * Returns the accumulated amount of the resource class.
         * 
         * @return the amount
         */
        public int getUnits() {
            return units;
        }

        @Override
        public String toString() {
            return String.format("ResourceAmount[%s : %s]",
                    rc.rcName(), ZNumber.asText(Integer.valueOf(units)));
        }

        /**
         * Helper method which returns the index of the element which resource
         * class equals the specified class. If no such element is found -1 is
         * returned.
         * 
         * @param lst the list to scan
         * @param rc a resource class
         * @return an index in the list, or -1
         */
        static int indexOf(List<ResourceAmount> lst, SWGResourceClass rc) {
            for (int i = 0; i < lst.size(); ++i)
                if (lst.get(i).rc == rc)
                    return i;

            return -1;
        }
    }
}
