package swg.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.SWGAide;
import swg.crafting.schematics.SWGProfession;
import swg.crafting.schematics.SWGProfessionLevel;
import swg.gui.SWGFrame;
import swg.swgcraft.SWGCraftCache;
import swg.tools.ZXml;

/**
 * This type is the manager for profession related information. When SWGAide is
 * launched this type populates collections for professions which exist in SWG,
 * profession level objects for the major levels when a character levels up, and
 * other collections which are queried for information. The files which these
 * collections are based on are downloaded from SWGCraft.org and from SWGAide's
 * home page.
 * <p>
 * This type is instantiated by {@link SWGFrame}; it must exist before any
 * client invokes its static methods, otherwise a {@link NullPointerException}
 * is generated. Also, of this type there must only exist one instance buy once
 * it is instantiated all information is provided by means of static methods.
 * <p>
 * None of the objects managed by this type do implement {@code Serializable}
 * but objects are dynamically requested each session. Clients that must store
 * data persistently may rather store the key values for the objects, usually
 * the string or integer value with which the objects are obtained to begin
 * with.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGProfessionManager  {

    /**
     * A map of profession ability objects, mapped by their proper names. For
     * "repeat" abilities the occasion is amended to the key, such as
     * "Reduced Vendor Fees(1)", enumerated naturally.
     */
    private static Map<String, SWGAbility> abilities;

    /**
     * A map of profession level objects, mapped by their proper names.
     */
    private static Map<String, SWGProfessionLevel> levels;

    /**
     * A map modifiers, mapped by their proper names.
     */
    private static Map<String, SWGModifier> modifiers;

    /**
     * Instantiates this type and populates the collections provided by this
     * manager.
     * <p>
     * <b>Notice:</b> The collection of profession level objects is {@code null}
     * until one instance of this type is created.
     */
    public SWGProfessionManager() {
        if (SWGProfessionManager.levels != null)
            throw new IllegalAccessError("Already instantiated");

        levels = new HashMap<String, SWGProfessionLevel>(0);
        modifiers = new HashMap<String, SWGModifier>(0);
        abilities = new HashMap<String, SWGAbility>(0);

        init();
        //SWGCraftCache.addSubscriber(this, UpdateType.PROF_LEVELS);
    }

    /*@Override
    public void handleUpdate(UpdateNotification u) {
        init();
    }*/

    /**
     * Helper method which initiates the collections of profession level
     * objects, modifiers, and abilities. This method uses data from locally
     * cached files.
     */
    private void init() {        
        initProfLevels();
        populateModifiers();
        populateAbilities();

        // TODO: add methods which reads a professions.xml file and populates
        // the collection of levels, must be null until then. Then also remove
        // synchronization as the list is not modified after launch
    }

    /**
     * Helper method which populates the map of profession levels.
     */
    private void initProfLevels() {
        try {
            Document doc = SWGCraftCache.getProfLevels();
            if (doc != null)
                synchronized (levels) { // not null :)
                    levels = parseProLevels(doc);
                }
            else
                SWGAide.printDebug("prlm", 1,
                    "SWGProfessionManager:initProfLevels: XML doc is null");
        } catch (Exception e) {
            SWGAide.printError("SWGProfManager:populateProfLevels", e);
        }
    }

    /**
     * Helper method which parses the specified document and returns a populated
     * map with profession level objects.
     * 
     * @param doc
     *            a document to parse
     * @return a map with profession level objects
     */
    private Map<String, SWGProfessionLevel> parseProLevels(Document doc) {
        HashMap<String, SWGProfessionLevel> plm =
            new HashMap<String, SWGProfessionLevel>(449);

        // Add two convenience instances for Novice and for Error
        SWGProfessionLevel pl = SWGProfessionLevel.NOVICE;
        plm.put(pl.getName(), pl); // used for the mandatory schematics
        pl = SWGProfessionLevel.ERROR;
        plm.put(pl.getName(), pl); // for erratic profession levels

        // the document is at the form
        // <professions ... last_updated="2010-01-10">
        // ... <profession name="">
        // ... ... <level name="" level="" description="">
        // ... ... ... ... elements
        // ... ... </level>
        // ... </profession>
        // </professions>

        NodeList np = doc.getChildNodes().item(0).getChildNodes(); // pros
        for (int i = 0; i < np.getLength(); ++i) {
            if (np.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element ep = (Element) np.item(i); // a profession element
                String pn = ZXml.stringFromAttr(ep, "name");
                SWGProfession p = SWGProfession.getFromName(pn);

                NodeList nl = ep.getChildNodes(); // levels
                for (int j = 0; j < nl.getLength(); ++j) {
                    if (nl.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) nl.item(j); // a level element
                        pl = new SWGProfessionLevel(p, el);
                        addLevel(plm, pl);
                    }
                }
            }
        }

        return plm;
    }

    /**
     * Helper method which populates the map of abilities with the content of
     * the specified file.
     */
    private void populateAbilities() {
    // if (questionmarkXML != null && questionmarkXML.exists()) {
    // // notice that for repeat abilities the XML file must be patched
    // }
    }

    /**
     * Helper method which populates the map of modifiers with the content of
     * the specified file.
     */
    private void populateModifiers() {
    // if (modifiersXML != null && modifiersXML.exists()) {
    // }
    }

    /**
     * Helper method which adds a profession level object to the specified
     * collection. The specified level must not already exist in the collection.
     * 
     * @param plm
     *            a map of profession levels
     * @param pl
     *            the profession level to add
     */
    private static void addLevel(
        Map<String, SWGProfessionLevel> plm, SWGProfessionLevel pl) {
        // XXX: remove the check once we know everything works
        if (plm.containsKey(pl.getName()))
            SWGAide.printDebug("prlm", 1, "Prof-level exists: " + pl.getName());
        else
            plm.put(pl.getName(), pl);
    }

    /**
     * Returns an instance of the ability with the specified name. If there is
     * no ability with the specified name {@code null} is returned.
     * 
     * @param name
     *            the proper name of the ability
     * @return an ability instance, or {@code null}
     */
    public static SWGAbility getAbility(String name) {
        // no need for synchronization, the map is not modified after launch
        return abilities.get(name);
    }

    /**
     * Returns a profession level object with the specified name, or {@code
     * null} if there exists no object for the specified name. This method is
     * thread safe.
     * 
     * @param name
     *            the proper name for the profession level to return
     * @return a profession level object, or {@code null}
     */
    public static SWGProfessionLevel getLevel(String name) {
        synchronized (levels) {
            return levels.get(name);
        }
    }

    /**
     * Returns a profession level object for the specified profession and the
     * specified level. If no level is defined that matches the specified
     * arguments {@code null} is returned.
     * <p>
     * The profession must not be {@link SWGProfession#ALL}. This method is
     * thread safe.
     * 
     * @param prof
     *            a profession constant to obtain a level for
     * @param level
     *            the specified level
     * @return a profession level object, or {@code null}
     * @throws IllegalArgumentException
     *             if the profession is "All" or if level is invalid
     * @throws NullPointerException
     *             if the profession is {@code null}
     */
    public static SWGProfessionLevel getLevel(SWGProfession prof, int level) {
        if (prof == null)
            throw new NullPointerException("Profession is null");
        if (prof == SWGProfession.getFromID(SWGProfession.ALL))
            throw new IllegalArgumentException("Invalid profession: ALL");
        if (level <= 0 || level > SWGProfessionLevel.MAX_LEVEL)
            throw new IllegalArgumentException("Invalid level: " + level);

        for (SWGProfessionLevel sl : levels.values())
            if (sl.getProfession() == prof && sl.getLevel() == level)
                return sl;

        return null;
    }

    /**
     * Returns an unordered list of profession level objects for all
     * professions. This method is thread safe.
     * 
     * @return a list of profession level objects
     */
    public static List<SWGProfessionLevel> getLevels() {
        return getLevels(SWGProfession.getFromID(SWGProfession.ALL));
    }

    /**
     * Returns an unordered list of profession level objects for the specified
     * profession. This method is thread safe.
     * 
     * @param profession
     *            a profession constant to obtain a list for
     * @return a list of profession level objects
     * @throws NullPointerException
     *             if the profession is {@code null}
     */
    public static List<SWGProfessionLevel> getLevels(SWGProfession profession) {
        synchronized (levels) {
            return getLvls(profession, SWGProfessionLevel.MAX_LEVEL);
        }
    }

    /**
     * Returns an unordered list of profession level objects for the specified
     * profession and where all elements are less than or equal to the specified
     * level. The specified profession must not be {@link SWGProfession#ALL}.
     * This method is thread safe.
     * 
     * @param profession
     *            a profession constant to obtain a list for
     * @param maxLevel
     *            the maximum level for the returned elements
     * @return a filtered list of profession level objects
     * @throws IllegalArgumentException
     *             if the profession is "All" or if level is invalid
     * @throws NullPointerException
     *             if the profession is {@code null}
     */
    public static List<SWGProfessionLevel> getLevels(
        SWGProfession profession, int maxLevel) {

        if (profession == SWGProfession.getFromID(SWGProfession.ALL))
            throw new IllegalArgumentException("Invalid profession: ALL");

        synchronized (levels) {
            return getLvls(profession, maxLevel);
        }
    }

    /**
     * Helper method which returns an unordered list of profession level objects
     * for the specified profession which levels are less than or equal to the
     * specified level.
     * 
     * @param p
     *            a profession constant to obtain a list for
     * @param lvl
     *            the max level for the returned elements
     * @return a filtered list of profession level objects
     * @throws IllegalArgumentException
     *             if the level is invalid
     * @throws NullPointerException
     *             if the profession is {@code null}
     */
    private static List<SWGProfessionLevel> getLvls(SWGProfession p, int lvl) {
        if (p == null)
            throw new NullPointerException("Profession is null");
        if (lvl <= 0 || lvl > SWGProfessionLevel.MAX_LEVEL)
            throw new IllegalArgumentException("Invalid level: " + lvl);

        List<SWGProfessionLevel> ret = new ArrayList<SWGProfessionLevel>(
            p == SWGProfession.getFromID(SWGProfession.ALL)
            ? 350
            : 25);

        for (SWGProfessionLevel sl : levels.values())
            if (sl.getLevel() <= lvl && sl.getProfession().equalsProfession(p))
                ret.add(sl);

        return ret;
    }

    /**
     * Returns an instance of the modifier with the specified name. The returned
     * object is either a basic attribute or a skill. If there is no modifier
     * with the specified name {@code null} is returned.
     * 
     * @param name
     *            the proper name of the modifier
     * @return a modifier instance, or {@code null}
     */
    public static SWGModifier getModifier(String name) {
        // no need for synchronization, the map is not modified after launch
        return modifiers.get(name);
    }

    // /**
    // * For testing purposes only.
    // *
    // * @param args
    // */
    // public static void main(String... args) {
    // SWGProfessionManager plm = new SWGProfessionManager();
    // System.err.println(getLevels());
    // System.err.println(plm);
    // }
}
