package swg.crafting.schematics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Element;

import swg.model.SWGAbility;
import swg.model.SWGAttribute;
import swg.model.SWGProfessionManager;
import swg.model.SWGSkill;
import swg.tools.ZHtml;
import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type models a profession level and contains the data which is associated
 * with a specific level of a specific profession. In particular, this type
 * models a <b>major level</b> of a profession; a major level is a level which
 * awards the character with new abilities and skills, and it reads its unique
 * name. Some major levels reward the character with profession specific items
 * or in-game titles, and some professions are awarded with more schematics at
 * major levels.
 * <p>
 * At both major and minor levels the character is awarded with improved basic
 * attributes. It would be possible to use instances of this type also for minor
 * levels, but such an instance would just read the integer level and the values
 * of the attributes, nothing more.
 * <p>
 * <b>Level groups:</b> In SWG the major levels are grouped and the name of the
 * level determines the group and the level within the group. A level name may
 * read: <tt>"Domestic Expertise VI"</tt>. The first part reads the profession;
 * the second part reads the name of the group, which may be <i>Fundamentals,
 * Essentials, Expertise,&nbsp;</i> and <i>Mastery&nbsp;</i> (ordered by level);
 * and the third part reads the level within such a group. A few schematics at
 * SWGCraft.org reads the group name <i>Novice</i> to denote that any profession
 * learns these schematics, perhaps this notion can be used for other, similar
 * situations.
 * <p>
 * Hence, there are two ways to specify a profession level:
 * <ul>
 * <li>profession + integer level</li>
 * <li>the unique profession level name</li>
 * </ul>
 * {@link SWGProfessionManager} makes heavy use of the unique name to look up
 * instances from its map.
 * <p>
 * This type does not implement {@code Serializable}. Clients which must
 * persistently save a reference to a an instance of this type would rather save
 * its unique name, or the profession&nbsp;<b>and</b>&nbsp;level, and request an
 * instance from the profession manager on demand.
 * <p>
 * This type provides clients with methods for what is learned at the profession
 * level an instance pertains to. Clients may also obtain the same information
 * from the profession manager, at a performance penalty.
 * <p>
 * The profession manager creates and maintains unique instances of this type.
 * Because the contained data is static there is no need for duplicate objects.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGProfessionLevel implements Comparable<SWGProfessionLevel> {

    /**
     * A constant used for schematics which have an incomplete profession level.
     * This instance has the name and description "Error" and level -1, all
     * other members have default values.
     */
    public static final SWGProfessionLevel ERROR = 
        new SWGProfessionLevel("Error", "Error", -1); 

    /**
     * A constant used for the Novice profession level. This instance has the
     * name "Novice", a description, and level 0, all other members have default
     * values.
     */
    public static final SWGProfessionLevel NOVICE = 
        new SWGProfessionLevel("Novice", "All professions beginning", 0); 

    /**
     * The maximum level for a character in SWG, commonly named as Combat Level
     * (CL)
     */
    public static final int MAX_LEVEL = 100049;

    /**
     * A list of abilities awarded at this profession level, or {@code null} if
     * there is none.
     */
    private final List<SWGAbility> abilities;

    /**
     * A list of basic attributes awarded at this profession level, or {@code
     * null} if unknown.
     */
    private final List<SWGAttribute> attributes;

    /**
     * A description of this profession level.
     */
    private final String desc;

    /**
     * The integer value of this profession level.
     */
    private final int level;

    /**
     * The proper name of this profession level, such as
     * "Force Sensitive Mastery VI".
     */
    private final String name;

    /**
     * The profession which this instance pertains to.
     */
    private final SWGProfession profession;

    /**
     * A list of rewards given the characters at this profession level, or
     * {@code null} if there is none.
     */
    private final List<String> rewards;

    /**
     * A list of skills awarded at this profession level, or {@code null} if
     * there is none.
     */
    private final List<SWGSkill> skills;

    /**
     * An in-game title which is awarded at this level, or {@code null} if there
     * is none, or if unknown.
     */
    private final String title;

    /**
     * Private helper constructor which creates a profession level instance
     * "Novice" for level 0, all other members have default values.
     * 
     * @param n
     *            the name for this instance
     * @param d
     *            a description
     * @param lvl the level for the instance
     */
    private SWGProfessionLevel(String n, String d, int lvl) {
        name = n;
        level = lvl;
        desc = d;
        abilities = null;
        attributes = null;
        profession = SWGProfession.getFromID(SWGProfession.ALL);
        rewards = null;
        skills = null;
        title = null;
    }

    /**
     * Creates an instance which represents a unique profession level based on
     * the specified XML element and for the specified profession. The specified
     * XML element is assumed to be on this format:
     * 
     * <pre>{@literal<level name="" level="" description="">
    <title name="" />
    <ability name="" amount="" />
    <skill name="" amount="" />
    <attribute name="" amount="" />
    <reward name="" />
</level> }</pre>
     * <p>
     * The format for the specified XML element is from the upcoming
     * professions.xml file.
     * 
     * @param profession
     *            the profession constant which the created instance pertains to
     * @param xml
     *            the XML element for the created profession level
     */
    public SWGProfessionLevel(SWGProfession profession, Element xml) {

        this.profession = profession;
        name = ZXml.stringFromAttr(xml, "name");
        level = ZXml.intFromAttr(xml, "level");

        if (profession == null || name == null || level <= 0)
            throw new IllegalArgumentException(String.format("p=%s n=%s, l=%s",
                profession, name, Integer.toString(level)));

        String ds = ZXml.stringFromAttr(xml, "description");
        desc = ZHtml.normalize(ds);

        // Now handle sub-elements
        Element se; // temporary sub-elements of the xml argument

        // element title :: <title name="" />
        se = ZXml.elementByTag(xml, "title");
        title = se != null
            ? ZXml.stringFromAttr(se, "name")
            : null;

        abilities = null;
        skills = null;
        attributes = null;
        rewards = null;
    }

    public int compareTo(SWGProfessionLevel o) {
        return this.name.compareTo(o.name);
    }

    /**
     * Returns an unordered list of abilities which are awarded at this
     * profession level, or {@link Collections#EMPTY_LIST} if there is none.
     * 
     * @return a list of abilities
     */
    public List<SWGAbility> getAbilities() {
        if (abilities != null)
            return new ArrayList<SWGAbility>(abilities); // protect content

        return Collections.emptyList();
    }

    /**
     * Returns an unordered list of basic attributes which are awarded at this
     * profession level, or {@link Collections#EMPTY_LIST} if unknown.
     * 
     * @return a list of basic attributes
     */
    public List<SWGAttribute> getAttributes() {
        if (attributes != null)
            return new ArrayList<SWGAttribute>(attributes); // protect content

        return Collections.emptyList();
    }

    /**
     * Returns a description of this profession level. If there is no
     * description the string "UNKNOWN" is returned.
     * 
     * @return a description, or "UNKNOWN"
     */
    public String getDescription() {
        return desc == null
            ? "UNKNOWN"
            : desc;
    }

    /**
     * Returns the integer level for this milestone.
     * 
     * @return the integer level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the proper name for this profession level, such as
     * "Force Sensitive Mastery VI".
     * 
     * @return the proper name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the profession constant which this instance pertains to.
     * 
     * @return a profession constant
     */
    public SWGProfession getProfession() {
        return profession;
    }

    /**
     * Returns an unordered list of rewards which are awarded the character at
     * this profession level; each element is the proper name of the reward. If
     * there is no reward {@link Collections#EMPTY_LIST} is returned.
     * 
     * @return a list of rewards
     */
    public List<String> getRewards() {
        if (rewards != null)
            return new ArrayList<String>(rewards); // protect content

        return Collections.emptyList();
    }

    /**
     * Returns an unordered list of skills which are awarded at this profession
     * level, or {@link Collections#EMPTY_LIST} if there is none.
     * 
     * @return a list of skills
     */
    public List<SWGSkill> getSkills() {
        if (skills != null)
            return new ArrayList<SWGSkill>(skills); // protect content

        return Collections.emptyList();
    }

    /**
     * Returns an in-game title which is awarded the character at this
     * profession level, or {@code null} if there is none.
     * 
     * @return an in-game title, or {@code null}
     */
    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGProfessionLevel").app('[').app(name);
        z.app(" (").app(level).app(") :: ").app(profession.getName());
        return z.app(']').toString();
    }

    /**
     * Returns a comparator which compares two profession level objects with
     * regards to their integer levels. This comparator does not consider
     * different professions.
     * 
     * @return a profession level comparator
     */
    public static Comparator<SWGProfessionLevel> getComparator() {
        return new Comparator<SWGProfessionLevel>() {
            public int compare(SWGProfessionLevel o1, SWGProfessionLevel o2) {
                return (o1.getLevel() - o2.getLevel());
            }
        };
    }
}
