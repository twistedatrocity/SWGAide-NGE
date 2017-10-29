package swg.crafting.schematics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import swg.SWGAide;
import swg.crafting.Quality;
import swg.model.SWGProfession;
import swg.model.SWGProfessionLevel;
import swg.model.SWGProfessionManager;
import swg.tools.ZHtml;
import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type models a schematic in SWG. A schematic is a draft schematic, a
 * looted schematics, or a schematic which is obtained by other means. This type
 * does not model manufacture schematics, which have different properties, and
 * neither does this type model an item crafted from a schematic.
 * <P>
 * An instance of this type reads its individual data and it contains all
 * required sub-parts of a schematic. These are resources, components,
 * experimentation properties, etc.
 * <p>
 * Each schematic is uniquely identified by its integer ID, nothing else. A
 * schematic is unique in SWG, thus there must exist only one unique instance of
 * this type per in-game schematic. Hence, instances of this type must be fully
 * instance-controlled by {@link SWGSchematicsManager}.
 * <p>
 * This type does not implement {@code Serializable}. Thus, any client that must
 * persistently save a reference to a schematic would rather save the integer ID
 * for the schematic and later request an instance from the schematics manager.
 * <p>
 * The data modeled by this type is parsed from the schematics XML file from
 * SWGCraft.org which has the following layout:
 * 
 * <pre>{@literal <schematic name="" id="" category="">
     <misc desc="" />
     <statistics complexity="" data_size="" xp="" manufacture_allowed="yes|no" 
         type="Regular|Looted|Reward|Factional: Imperial|Factional: Rebel|
         |No longer craftable|Outdated|Deconstructed" crate=""
         quality="hq|lq|mixed|n/a"/ >
     <level profession="" level="" />
     <expertise profession="" name="" rank="" />
     <category></category>
     <component id="" type="schematic|category|item|error" number="" 
         similar="" optional="" looted="" desc=""/>
     <resource swgcraft_id="" units="" desc="" />
     <exp_grp desc="">
         <exp desc="" cd="" cr="" dr="" er="" fl="" hr="" ma="" 
         oq="" pe="" sr="" ut="" /> <!-- only stats greater than zero -->
     </exp_grp>
 </schematic> }</pre>
 * <p>
 * In the XML file for schematics all elements are listed at the same level.
 * These are the elements whereof some are optional and others exist several
 * times:
 * <dl>
 * <dt>misc</dt>
 * <dd>miscellaneous information about this schematic as it is read at the
 * schematic; in the future additional information may be added</dd>
 * <dt>statistics</dt>
 * <dd>the schematic's complexity; data size; the amount of XP that crafting an
 * item of this schematic yields, the base value without buffs and no practice
 * mode; manufacture determines if it possible to create manufacture schematics
 * from this schematic; an item which reads the origin of this schematic or when
 * it was removed from game; factory crate size; and if it should be crafted as
 * HQ, LQ, or if that is determined by the use, see {@link Quality}.</dd>
 * <dt>level</dt>
 * <dd>(optional) information about which profession and at which level this
 * schematic is awarded; for a non-draft schematic the level rather denotes the
 * minimum requirements to be able to learn and use this schematic; 0, 1, or
 * several elements &mdash; see expertise</dd>
 * <dt>expertise</dt>
 * <dd>(optional) information on which profession and which expertise name this
 * schematic is available; 0, 1, or several elements &mdash; if neither one of
 * level or expertise is present the schematic element is invalid</dd>
 * <dt>component</dt>
 * <dd>(optional) information about a component slot in this schematic; 0, 1, or
 * several elements</dd>
 * <dt>resource</dt>
 * <dd>(optional) information about a resource slot in this schematic; 0, 1, or
 * several elements</dd>
 * <dt>exp_grp</dt>
 * <dd>(optional) information about a group of experimental lines in this
 * schematic; 0, 1, or several elements</dd>
 * <dt>exp</dt>
 * <dd>information about one experimental line which is contained in an
 * experimentation group</dd>
 * </dl>
 * <p>
 * <B>Notice:</B> A component which is looted or obtained otherwise but which
 * cannot be crafted is not modeled by this type. {@link SWGComponentSlot}
 * models them and reads their name and amount; this may change in the future.
 * <p>
 * <B>Notice:</B> Currently not all of the mandatory elements are validated for
 * their existence or content; this may change in the future,
 * 
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGSchematic implements Comparable<SWGSchematic> {

    /**
     * Denotes the amount of XP awarded from crafting an item from this
     * schematic, or 0 if the value is unknown. The value is the base XP,
     * without any buff or bonus and not in practice mode.
     */
    private final int baseXP;

    /**
     * The category for this schematic. All schematics fall into a category, for
     * example "Foods" or "Pistol"; see {@link SWGCategory}.
     */
    private final int category;

    /**
     * The complexity of this schematic, or 0 if the value is unknown.
     */
    private final int complexity;

    /**
     * A list of the component slots for this schematic, or {@code null} if this
     * schematics does not call for any component.
     */
    private final List<SWGComponentSlot> componentSlots;

    /**
     * The default size of a factory crate.
     */
    private final int crateSize;

    /**
     * The datapad size of this schematic, or 0 if the value is unknown.
     */
    private final int dataSize;

    /**
     * The description for this schematic as read at the schematic.
     */
    private final String desc;

    /**
     * An array of one or several expertise objects, or {@code null} if this is
     * not a schematic learned from a profession expertise. Each element is a
     * pair with an instance of {@link SWGProfession} and a {@link String} that
     * reads the name of the expertise.
     */
    private final Object[][] expertise;

    /**
     * A list of experimentation groups for this schematic, or {@code null} if
     * this schematic does not provide any experiment.
     */
    private final List<SWGExperimentGroup> expGroups;

    /**
     * The unique integer ID which identifies this schematic at SWGCraft.org and
     * in SWGAide. The ID is validated and is an integer greater than zero.
     * <p>
     * This ID is an integer, not the old "swgcraft_id" string token which is
     * obsolete and no longer maintained.
     */
    private final int id;

    /**
     * A flag which denotes if if possible to create a manufacture schematic
     * from this instance. The default value is {@code true}, only a few special
     * schematics cannot be used for factory runs.
     */
    private final boolean manufacturable;

    /**
     * The proper name for this the schematic, not {@code null}.
     */
    private final String name;

    /**
     * A list of profession level objects, compare {@link #skillLevels}. This
     * list is populated when {@link #getSkillLevels()} is first invoked.
     */
    private ArrayList<SWGProfessionLevel> profList;

    /**
     * A quality constant for this schematic. This is HQ, LQ, mixed, or unknown.
     */
    public final Quality quality;

    /**
     * A list of the resource slots for this schematic, or {@code null} if this
     * schematics does not call for any resource.
     */
    private final List<SWGResourceSlot> resourceslots;

    /**
     * A string which denotes what type of schematic this is, one of:
     * <dl>
     * <dt>Deconstructed</dt>
     * <dd>a schematic which is the result from deconstructing an item</dd>
     * <dt>Factional: Imperial</dt>
     * <dd>a schematic bought from an imperial recruiter</dd>
     * <dt>Factional: Rebel</dt>
     * <dd>a schematic bought from a rebel recruiter</dd>
     * <dt>Looted</dt>
     * <dd>a schematic which is looted in combat from a defeated MOB</dd>
     * <dt>Outdated</dt>
     * <dd>an obsolete schematic, should not be displayed by SWGAide</dd>
     * <dt>No longer craftable</dt>
     * <dd>an obsolete schematic, should not be displayed by SWGAide</dd>
     * <dt>Regular</dt>
     * <dd>a plain draft schematic</dd>
     * <dt>Reward</dt>
     * <dd>a schematic which is obtained as a reward from some source</dd>
     * </dl>
     */
    private final String schematicType;

    /**
     * A flag that denotes if this schematic is represented with screenshots at
     * SWGCraft.org.
     */
    private final boolean screen;
    
    /**
     * An array of one or several profession-level objects, or {@code null} if
     * this if this is a schematic learned from a profession expertise. Each
     * element is a pair with an instance of {@link SWGProfession} and
     * {@link Integer} and it denotes the profession that have been awarded this
     * schematic and at which level.
     */
    private final Object[][] skillLevels;

    /**
     * Creates an instance of this type from the specified XML element.
     * 
     * @param xml the XML element to parse
     * @throws IllegalArgumentException if there is an error in the parsed data
     * @throws NullPointerException if something is {@code null}
     */
    SWGSchematic(Element xml) {
        // XXX: Add validation for all mandatory XML elements of a schematic

        name = ZXml.stringFromAttr(xml, "name");
        if (name == null)
            throw new NullPointerException("Schematic name is null: "
                    + xml.toString());

        id = ZXml.intFromAttr(xml, "id");
        if (id <= 0)
            throw new IllegalArgumentException(String.format(
                    "Schematic ID unknown: %s: %s",
                    name, xml.toString()));

        // element category
        int c = ZXml.intFromAttr(xml, "category");
        category = c > 0
                ? c
                : -1;
        if (category <= 0)
            SWGAide.printDebug("scmc", 1,
                    "SWGSchematic: missing category: " + name);

        List<String> cl = ZXml.commentsFromElem(xml);
        screen = cl.contains("Screenshot available");

        Element se; // temporary variable for elements of the xml element

        // element misc
        se = ZXml.elementByTag(xml, "misc");
        String ds = ZXml.stringFromAttr(se, "desc");
        desc = ZHtml.normalize(ds);

        // element statistics
        se = ZXml.elementByTag(xml, "statistics");
        complexity = ZXml.intFromAttr(se, "complexity");
        dataSize = ZXml.intFromAttr(se, "data");
        baseXP = ZXml.intFromAttr(se, "xp");
        manufacturable =
                ZXml.booleanFromAttr(se, "manufacture");
        schematicType = ZXml.stringFromAttr(se, "type");
        crateSize = ZXml.intFromAttr(se, "crate");
        quality = Quality.valueof(
                ZXml.stringFromAttr(se, "quality"));
        if (schematicType == null)
            throw new NullPointerException("'type' is null, this: " + name);

        NodeList nl; // temporary variable for node lists
        int nlLen; // temporary variable for node-list length

        // for each element "level", create an object and add it to the list
        // <level profession="" name="" level="" />
        nl = xml.getElementsByTagName("level");
        nlLen = nl.getLength();
        skillLevels = nlLen > 0
                ? new Object[nlLen][]
                : null;
        for (int i = 0; i < nlLen; ++i) {
            Element e = (Element) nl.item(i);
            try {
                skillLevels[i] = parseLevel(e);
            } catch (Exception ex) {
                SWGAide.printDebug("scmc", 1, String.format(
                        "SWGSchematic:PROF: %s : %s",
                        ex.getMessage(), name));
                skillLevels[i] = new Object[] { null, Integer.valueOf(1) };
            }
        }

        // for each element "expertise", create an object and add it to the list
        // <expertise profession="" name="" rank="" />
        nl = xml.getElementsByTagName("expertise");
        nlLen = nl.getLength(); // length always >= 1
        expertise = nlLen > 0
                ? new Object[nlLen][]
                : null;
        for (int i = 0; i < nlLen; ++i) {
            Element e = (Element) nl.item(i);
            expertise[i] = parseExpertise(e);
        }
        // one of profession-level or expertise must be known
        if (skillLevels == null && expertise == null)
            throw new IllegalArgumentException(
                    "no skill and no expertise, this: " + name);

        // for each element "component", create a slot and add it to the list
        nl = xml.getElementsByTagName("component");
        nlLen = nl.getLength(); // length >= 0
        if (nlLen > 0)
            componentSlots = new ArrayList<SWGComponentSlot>(nlLen);
        else
            componentSlots = Collections.emptyList();
        for (int i = 0; i < nlLen; ++i) {
            Element e = (Element) nl.item(i);
            SWGComponentSlot slot = new SWGComponentSlot(e);
            componentSlots.add(slot);
        }

        // for each element "resource", create a slot and add it to the list
        nl = xml.getElementsByTagName("resource");
        nlLen = nl.getLength(); // length >= 0

        if (nlLen > 0)
            resourceslots = new ArrayList<SWGResourceSlot>(nlLen);
        else
            resourceslots = Collections.emptyList();
        for (int i = 0; i < nlLen; ++i) {
            Element e = (Element) nl.item(i);
            SWGResourceSlot slot = new SWGResourceSlot(e);
            resourceslots.add(slot);
        }

        // for each element "exp_grp", create an experimentation group
        // and add it to the list --- each group populates itself
        nl = xml.getElementsByTagName("exp_grp");
        nlLen = nl.getLength(); // length >= 0
        if (nlLen > 0)
            expGroups = new ArrayList<SWGExperimentGroup>(nlLen);
        else
            expGroups = Collections.emptyList();
        for (int i = 0; i < nlLen; ++i) {
            Element e = (Element) nl.item(i);
            SWGExperimentGroup exGrp = new SWGExperimentGroup(e);
            expGroups.add(exGrp);
        }
    }

    public int compareTo(SWGSchematic other) {
        if (this.getCategory() == 726 && other.getCategory() == 726) // camps
            return this.getComponentSlots().size()
                    - other.getComponentSlots().size();
        return this.name.compareToIgnoreCase(other.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof SWGSchematic) {
            SWGSchematic o = (SWGSchematic) obj;
            return this.id == o.id;
        }
        return false;
    }

    /**
     * Returns the category ID which this schematic pertains to, or -1 if it is
     * unknown. The category is obtained from the schematics manager.
     * 
     * @return the category ID, -1
     */
    public int getCategory() {
        return category;
    }

    /**
     * Returns the complexity for this schematic, or 0 if it is unknown.
     * 
     * @return the complexity, or 0
     */
    public int getComplexity() {
        return complexity;
    }

    /**
     * Returns a list of the component slots for this schematic, or
     * {@link Collections#EMPTY_LIST} if this schematic does not call for any
     * component. The returned list is <b>read-only</b>.
     * 
     * @return a list of component slots
     */
    public List<SWGComponentSlot> getComponentSlots() {
        return componentSlots;
    }

    /**
     * Returns the default size of a factory crate for this schematic. This
     * method returns 0 if the size is unknown or if manufacture schematics
     * cannot be created from this schematic.
     * 
     * @return the size if a factory crate, or 0
     */
    public int getCrateSize() {
        return crateSize;
    }

    /**
     * Returns the datapad size for this schematic, or 0 if it is unknown.
     * 
     * @return the datapad size, or 0
     */
    public int getDataSize() {
        return dataSize;
    }

    /**
     * Returns description for this schematic as read at the schematic, or
     * {@code null} if it is unknown.
     * 
     * @return a description, or {@code null}
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Returns a list of the experimentation groups for this schematic, or
     * {@link Collections#EMPTY_LIST} if this schematic does not provide any
     * experiment. The returned list is <b>read-only</b>.
     * 
     * @return a list of experimentation groups
     */
    public List<SWGExperimentGroup> getExperimentGroups() {
        return expGroups;
    }

    /**
     * Returns an array of pairs that denotes the profession and the name of the
     * expertise from which this schematic is available. Each pair contains an
     * instance of {@link SWGProfession} and a {@link String}. If this is not a
     * schematic from an expertise {@code null} is returned.
     * <p>
     * The returned array is <b> read-only</b>.
     * 
     * @return an array that denotes an expertise
     */
    public Object[][] getExpertise() {
        return expertise;
    }

    /**
     * Returns the unique integer ID which identifies this schematic at
     * SWGCraft.org and in SWGAide.
     * 
     * @return the unique ID
     */
    public int getID() {
        return id;
    }

    /**
     * Returns the proper name for this schematic, not {@code null}.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a list of the resource slots for this schematic, or
     * {@link Collections#EMPTY_LIST} if this schematics does not call for any
     * resource. The returned list is <b>read-only</b>.
     * 
     * @return a list of resource slots
     */
    public List<SWGResourceSlot> getResourceSlots() {
        return resourceslots;
    }

    /**
     * Returns a list of profession levels for professions that have been
     * awarded this schematic and at which level respectively. If this is not a
     * regular schematic the elements denotes the required level for this
     * schematic. If this is a schematic learned from expertise the list
     * returned list is empty; see {@link #getExpertise()}.
     * <p>
     * <b>Notice:</b> If there is an error parsing the profession levels the
     * members of the returned object are set to profession = ALL, level = 0,
     * name = "Error". This reflects that we do not know the profession and the
     * level, but an error is indicated.
     * <p>
     * The returned list is <b>read-only</b>.
     * 
     * @return a list of professions level objects
     */
    public List<SWGProfessionLevel> getSkillLevels() {
        if (skillLevels == null)
            return Collections.emptyList();
        if (profList == null) {
            profList = new ArrayList<SWGProfessionLevel>(skillLevels.length);
            for (int i = 0; i < skillLevels.length; ++i) {
                SWGProfessionLevel pl = null;
                Object[] pair = skillLevels[i];
                if (pair[0] == null) // error
                    pl = SWGProfessionLevel.ERROR;
                else if (pair[0] == SWGProfession.ALL) // profession ALL
                    pl = SWGProfessionLevel.NOVICE;
                else {
                    try {
                        pl = SWGProfessionManager.getLevel(
                                ((SWGProfession) pair[0]),
                                ((Integer) pair[1]).intValue());
                    } catch (Exception e) {
                        SWGAide.printError(
                                "SWGSchematic:getSkillLevels: " + this, e);
                    }
                }
                if (pl != null) profList.add(pl);
            }
        }
        return profList;
    }

    /**
     * Returns a string which denotes what type of schematic this is; "Regular"
     * is the most common:
     * <dl>
     * <dt>Deconstructed</dt>
     * <dd>a schematic which is the result from deconstructing an item</dd>
     * <dt>Factional: Imperial</dt>
     * <dd>a schematic bought from an imperial recruiter</dd>
     * <dt>Factional: Rebel</dt>
     * <dd>a schematic bought from a rebel recruiter</dd>
     * <dt>Looted</dt>
     * <dd>a schematic is looted in combat from a defeated MOB</dd>
     * <dt>No longer craftable</dt>
     * <dd>an obsolete schematic, should not be displayed by SWGAide</dd>
     * <dt>Regular</dt>
     * <dd>a plain, draft schematic</dd>
     * <dt>Reward</dt>
     * <dd>a schematic which is obtained as a reward from some source</dd>
     * </dl>
     * 
     * @return the type of this schematic
     */
    public String getType() {
        return schematicType;
    }

    /**
     * Returns the amount of XP awarded from crafting an item based on this
     * schematic, or 0 if it is unknown. The returned value is the base XP,
     * without any buff or bonus and not in practice mode.
     * 
     * @return the base amount of XP
     */
    public int getXP() {
        return baseXP;
    }

    @Override
    public int hashCode() {
        return 17 + 39 * id;
    }

    /**
     * Returns {@code true} if this schematic is represented by a screenshot at
     * SWGCraft.org.
     * 
     * @return {@code true} if screenshot exists
     */
    public boolean hasScreenshot() {
        return screen;
    }

    /**
     * Returns {@code true} if it is possible to create manufacture schematics
     * based on this schematic.
     * 
     * @return {@code false} if this schematic cannot be manufactured
     */
    public boolean isManufacturable() {
        return manufacturable;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGSchematic[").app(name).app(' ').app('(');
        return z.app(id).app(')').app(']').toString();
    }

    /**
     * Helper method which returns a pair with the profession and the name for
     * the expertise that are parsed from the specified XML element. The pair
     * contains instances of a {@link SWGProfession} and a {@link String}.
     * 
     * @param xml an XML element for a profession level
     * @return a pair with profession and level
     */
    private static Object[] parseExpertise(Element xml) {
        // <expertise profession="" name="" rank="" />
        // rank is currently unused
        int pid = ZXml.intFromAttr(xml, "profession");
        SWGProfession p = SWGProfession.getFromID(pid);
        String n = ZXml.stringFromAttr(xml, "name");
        return new Object[] { p, n };
    }

    /**
     * Helper method which returns a pair with the profession and the level that
     * are parsed from the specified XML element. The pair contains instances of
     * a {@link SWGProfession} and a {@link Integer}. If the profession-level is
     * unknown at SWGCraft.org the profession slot is set to {@code null}.
     * 
     * @param xml an XML element for a profession level
     * @return a pair with profession and level
     */
    private static Object[] parseLevel(Element xml) {
        // <level profession="" level=""/>
        int pid = ZXml.intFromAttr(xml, "profession");
        SWGProfession p = pid < 0
                ? null
                : SWGProfession.getFromID(pid);
        int lvl = ZXml.intFromAttr(xml, "level");
        Integer l = Integer.valueOf(lvl);
        return new Object[] { p, l };
    }

    // /**
    // * Returns a formatted string which reads all sensible data from this
    // * schematic. The returned string is formatted over several indented lines
    // * for easy reading.
    // *
    // * @return a string representation of this instance
    // */
    // public String prettyPrint() {
    // ZString z = new ZString("Schematic: ");
    // z.app(name).app(" (").app(id).app(')').nl();
    // z.app("\tCategory: ").app(category).nl();
    // z.app("\tType:     ").app(schematicType).nl();
    // z.app("\tManufac:  ").app(manufacturable).nl();
    // z.app("\tComplex:  ").app(complexity).nl();
    // z.app("\tSize:     ").app(dataSize).nl();
    // if (resourceslots != null) {
    // z.app("\tResources: ").nl();
    // for (SWGResourceSlot r : resourceslots)
    // z.app("\t\t").app(r.toString()).nl();
    // }
    //
    // if (componentSlots != null) {
    // z.app("\tComponents: ").nl();
    // for (SWGComponentSlot c : componentSlots)
    // z.app("\t\t").app(c.toString()).nl();
    // }
    //
    // if (expGroups != null) {
    // for (SWGExperimentGroup g : expGroups) {
    // z.app("\t").app(g.getDescription()).nl();
    // for (SWGExperimentLine e : g.getExperimentalLines())
    // z.app("\t\t").app(e.toString()).nl();
    // }
    // }
    //
    // return z.app('\t').app(getDescription()).nl().toString();
    // }
}
