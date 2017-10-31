package swg.crafting.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import devtools.SWGResourceClassGenerator;
import swg.crafting.Stat;
import swg.model.SWGPlanet;
import swg.tools.ZString;

/**
 * This type represents a resource class in SWG. Resource classes are organized
 * in a hierarchical tree structure. A super-generic class "Resource" is not
 * used by any schematic, but from the four classes at the next level all other
 * classes are rooted, these four classes are: Energy, Inorganic, Organic, and
 * Space Resource. More than 800 distinct resource classes exist in SWG.
 * <p>
 * Concrete objects of this type are in themselves singletons, consider them
 * constants. They just carry the static properties of a particular resource
 * class. The class files exist in the {@code swg.crafting.resources.types}
 * package and they are machine generated from the <i>resourcetree.XML</i> file
 * by {@link SWGResourceClassGenerator}.
 * <p>
 * This class must not be extended other than by the generated sub-classes.
 * Together with {@link SWGResourceClassTree} and {@link SWGResourceClassInfo}
 * this type forms the interface to SWGAides's model of resource classes.
 * <p>
 * This class implements {@link Serializable}. However, each subclass makes sure
 * that only one instance per resource class is created, the singleton pattern.
 * <p>
 * First of all spawnable instances of this type denote that type of a resource;
 * resources use the composition pattern and just reference a resource class
 * instance of the proper type. Anther use is to filter on resource type.
 * <p>
 * Obtain an instance of this type from on of the static methods {@code
 * <a-type>.getInstance()}, {@link #rc(String)}, or {@link #rc(int)}.
 * <p>
 * <b>Historical Notes</b><br />
 * In 2005 the first version of {@link SWGResource} was published, which was
 * resource and resource class in the same type. Subclasses of that version of
 * {@link SWGResource} denoted the particular resource class. That worked well
 * as each instance contained resource class, name, stats, etc.
 * <p>
 * However, to obtain an instance just to read the resource class properties, or
 * to filter on resource class, an instance of a resource had to be instantiated
 * in full, with dummy values for the payload, no matter if the resource class
 * could spawn in the worlds or if it was a more generic class.
 * <p>
 * Furthermore, resource instances contained the data that was reported to
 * SWGCraft or data for a local instance. Not seldom that data was updated,
 * edited or corrected. In worst case, if the resource class was corrected, the
 * instance had to be replaced, and all old instances in all collections and
 * wrappers became obsolete, unless they were hunted down and updated.
 * <p>
 * Thus, in August 2009 this type was introduced to split out resource class
 * from the resource. Now resources use the composition pattern, which makes it
 * easy to replace any kind of data.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public abstract class SWGResourceClass implements Serializable,
        Comparable<SWGResourceClass> {

    /**
     * A convenience list of resource class names.
     */
    private static List<String> classNames;

    /**
     * A helper instance that represents the abstract top resource class. This
     * instance must only be used in GUI trees and their models.
     */
    @SuppressWarnings("synthetic-access")
    public static final SWGResourceClass INSTANCE = new SWGResourceClassRoot();

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -8288806973182080790L;

    /**
     * An instance of a space or recycled resource. This variable is only used
     * for {@link #spaceOrRecycled()} by the appropriate instances of this type.
     */
    transient SWGKnownResource spaceOrRecycled;

    /**
     * The default constructor for this type. This class must not to be extended
     * other than by machine generated sub-classes.
     */
    protected SWGResourceClass() {
        // prevent remote instantiation but use protected to allow sub-classes
        // in package swg.crafting.resources.types to access this constructor
    }

    /**
     * This method is using the sort order defined by {@link #sortIndex()}.
     * <p>
     * {@inheritDoc}
     */
    public final int compareTo(SWGResourceClass o) {
        return (sortIndex() - o.sortIndex());
    }

    /**
     * Returns the number of <i>expected stats</i> for this resource class.
     * 
     * @return the number of <i>expected stats</i>
     */
    public abstract int expectedStats();

    /**
     * Determines if this resource class is expected to have the specified stat.
     * 
     * @param s a stat constant
     * @return {@code true} if this resource class is expected to have the stat
     */
    public abstract boolean has(Stat s);

    /**
     * Determines if it is possible to harvest resources of this resource class
     * from any <b>creature</b> in the worlds. Default value is {@code false},
     * this method returns {@code true} only if this is a creature resources and
     * if it can be harvested off of creature carcasses in the worlds.
     * <p>
     * Remember that <i>all classes </i> of creature resources do spawn in the
     * SWG servers, but not all are possible to harvest naturally; it is still
     * possible to claim them via the 30k veteran resource crate. This method
     * returns {@code false} for these "synthetic" resources.
     * 
     * @return {@code true} if this kind of resource can really be harvested
     */
    public boolean isHarvested() {
        return false;
    }

    /**
     * Determines if this is a JTL resource class. JTL resources were introduced
     * for Shipwrights in 2004 and they are special in two respects: they have
     * no lower or upper caps which effectively makes it possible for these
     * resources to surpass caps of their parent resource classes, and they have
     * a longer minimum and maximum spawning time, ranging 13 to 22 days.
     * 
     * @return {@code true} if this resource is a JTL resource
     */
    public boolean isJTL() {
        return false;
    }

    /**
     * Determines if this resource class denotes space resources obtained from
     * mining vessels or recycled resource compounds. Many resources can be
     * recycled to more generic compounds with fixed stats. Space resources are
     * static and neither spawn nor despawn and they too have fix stats. This
     * method returns {@code true} if this is such a resource class. Also see
     * {@link SWGKnownResource#spaceOrRecycled(SWGResourceClass)}.
     * 
     * @return {@code true} if this resource class is space or recycled
     */
    public boolean isSpaceOrRecycled() {
        return false;
    }

    /**
     * Determines if this resource class can spawn in the worlds. That is, his
     * method determines if this resource class is a leaf in the resource tree,
     * only then it can spawn as a real resource.
     * <p>
     * <b>Notice:</b> these exceptions apply:
     * <ul>
     * <li>recycled resource classes are always leafs but cannot <i>spawn </i>
     * in the worlds >> <b>false</b>; see {@link #isSpaceOrRecycled()}</li>
     * <li>space resource classes that also are leafs do spawn in the worlds,
     * but they are static with fix stats >> <b>false</b>; see
     * {@link #isSpaceOrRecycled()}</li>
     * <li>planetary restrictions are not considered, use
     * {@link #canSpawnAt(SWGResourceClass, SWGPlanet)}</li>
     * <li>creature resources, their harvest-ability is not considered, use
     * {@link #isHarvested()}</li>
     * </ul>
     * 
     * @return {@code true} if this resource class can spawn in the worlds, not
     *         considering the exceptions
     */
    public boolean isSpawnable() {
        return false;
    }

    /**
     * Determines if this is a resource class that is a sub-type of the
     * suggested super-class. This method returns {@code true} if this instance
     * is the same class or a sub-class of the argument.
     * 
     * @param zuper the suggested super-type
     * @return {@code true} if {@code this} &le; {@code zuper}
     */
    public boolean isSub(Class<? extends SWGResourceClass> zuper) {
        return zuper.isAssignableFrom(this.getClass());
    }

    /**
     * Determines if this is a resource class that is a sub-type of the
     * suggested super-class; invokes {@link #isSub(Class)}.
     * 
     * @param zuper the suggested super-class
     * @return {@code true} if {@code this} &le; {@code zuper}
     */
    public boolean isSub(SWGResourceClass zuper) {
        return isSub(zuper.getClass());
    }

    /**
     * Returns the upper cap for {@code stat} for this resource class. This
     * method returns 1000 if this resource has no upper cap. If {@code stat} is
     * not an expected stat this method returns 0.
     * 
     * @param s a stat constant
     * @return the upper cap
     */
    public abstract int max(Stat s);

    /**
     * Returns the lower cap for {@code stat} for this resource class. This
     * method returns 1 if this resource has no lower cap. If {@code stat} is
     * not an expected stat this method returns 0.
     * 
     * @param s a stat constant
     * @return the lower cap
     */
    public abstract int min(Stat s);

    /**
     * Returns the integer identifier used by SWGCraft for this resource class.
     * 
     * @return an integer ID
     */
    public abstract int rcID();

    /**
     * Returns the proper name of this resource class.
     * 
     * @return a name
     */
    public abstract String rcName();

    /**
     * Returns a resource class token that is an identifier used by SWGCraft.
     * The token is "swgcraft_type_id" in the exported files, an "swgcraft_id"
     * in the resourcetree.xml file.
     * 
     * @return a resource class token
     */
    public abstract String rcToken();

    /**
     * Returns the index for sorting in resource class order. That is the same
     * order as resource classes are read in the 30k veteran resource crate,
     * which is the same order as they are read in the file "resourcetree.xml".
     * 
     * @return an index for sorting in resource class order
     */
    public abstract int sortIndex();

    /**
     * If this type denotes a space or recycled resource class this method
     * returns a resource instance for this type.
     * 
     * @return an instance of a space or recycled resource
     * @throws UnsupportedOperationException if this type is not space or
     *         recycled
     */
    public final synchronized SWGKnownResource spaceOrRecycled() {
        if (spaceOrRecycled == null)
            spaceOrRecycled = SWGKnownResource.spaceOrRecycled(this);

        return spaceOrRecycled;
    }

    @Override
    public final String toString() {
        return rcName();
    }

    /**
     * Determines if a resource class with the specified name can spawn spawn at
     * and only at the specified planet; see
     * {@link #canSpawnAt(SWGResourceClass, SWGPlanet)}.
     * 
     * @param rcName the proper name of a resource class
     * @param p a planet constant
     * @return {@code true} if the resource class spawns at the planet
     * @throws NullPointerException if an argument is {@code null}
     */
    public static boolean canSpawnAt(String rcName, SWGPlanet p) {
        String n = rcName.substring(0, Math.min(rcName.length(), 4));
        // but Lok has only 3 letters
        return (p.getName().startsWith(n) || p == SWGPlanet.LOK
                    && n.equals("Loki"));
    }

    /**
     * Determines if the specified resource class can spawn spawn at and only at
     * the specified planet. This method is used for planetary resources and
     * returns {@code false} if the specified class and the specified planet
     * does not match.
     * <p>
     * <b>Notice:</b> non-planetary resource classes may spawn at any planet
     * except at Kashyyyk and Mustafar.
     * 
     * @param rc a resource class constant
     * @param p a planet constant
     * @return {@code true} if the resource class spawns at the planet
     * @throws NullPointerException if an argument is {@code null}
     */
    public static boolean canSpawnAt(SWGResourceClass rc, SWGPlanet p) {
        return canSpawnAt(rc.rcName(), p);
    }

    /**
     * Returns the greatest resource class ID. This ID is somewhat higher than
     * the amount of resource classes in SWG; no resource class has ID = 0 and
     * there are some holes in the numbering at SWGCraft.
     * 
     * @return the greatest ID
     */
    public static final int maxID() {
        // SWGResourceClassInfo is not public in SWGAide
        return SWGResourceClassInfo.LISTSIZE;
    }

    /**
     * Returns the resource class constant for the specified ID. The ID is the
     * resource class ID used at SWGCraft and in the resourcetree.xml file.
     * 
     * @param id an integer ID
     * @return a resource class constant
     * @throws IllegalArgumentException if the ID is invalid
     */
    public static final SWGResourceClass rc(int id) {
        if (id <= 0 || id > SWGResourceClassInfo.LISTSIZE)
            throw new IllegalArgumentException("Invalid ID: " + id);

        SWGResourceClass rc = SWGResourceClassInfo.swgIDtoInstance.get(id);
        if (rc == null)
            throw new IllegalArgumentException("Unused ID: " + id);

        return rc;
    }

    /**
     * Returns the resource class constant for the specified token or proper
     * name.
     * 
     * @param s a name or token
     * @return a resource class constant
     * @throws IllegalArgumentException if the string is invalid
     */
    public static final SWGResourceClass rc(String s) {
        Integer ret = SWGResourceClassInfo.swgTypeToInt.get(s);
        if (ret == null)
            throw new IllegalArgumentException("Invalid string: " + s);

        return SWGResourceClassInfo.swgIDtoInstance.get(ret.intValue());
    }

    /**
     * Returns the SWGCraft resource class ID for the specified token or proper
     * resource class name, or -1 if the argument is invalid.
     * 
     * @param str a name or token
     * @return an ID, or -1
     */
    public static final int rcID(String str) {
        Integer ret = SWGResourceClassInfo.swgTypeToInt.get(str);
        return ret != null
                ? ret.intValue()
                : -1;
    }

    /**
     * Returns the proper resource class name for the specified ID, or {@code
     * null}.
     * 
     * @param id an integer ID
     * @return a proper resource class name, or {@code null}
     */
    public static final String rcName(int id) {
        // there is no element at id=0, real list size is
        // SWGResourceInfo.LISTSIZE + 1, with the range
        // [0 SWGResourceInfo.LISTSIZE]
        if (id <= 0 || id > SWGResourceClassInfo.LISTSIZE)
            return null;
        SWGResourceClass type = SWGResourceClassInfo.swgIDtoInstance.get(id);
        return type == null
                ? null
                : type.rcName();
    }

    /**
     * Returns an alphabetically sorted list of resource class names.
     * 
     * @return a list of names
     */
    public static final List<String> rcNames() {
        if (classNames == null) {
            classNames = new ArrayList<String>(SWGResourceClassInfo.LISTSIZE);
            synchronized (classNames) {
                for (SWGResourceClass rc : SWGResourceClassInfo.swgIDtoInstance)
                    if (rc != null) classNames.add(rc.rcName());

                Collections.sort(classNames);
            }
        }
        return classNames;
    }

    /**
     * Returns a list of resource class names that matches the specified
     * abbreviation, or an empty list. The input string must be delimited by
     * white spaces, as the resource class is, and each stem is matched against
     * the names of resource class names.
     * 
     * @param abbrev an input string
     * @return a list of resource class names, or an empty list
     */
    public static final List<String> rcNames(String abbrev) {
        String input = abbrev.trim();

        String pattern = "^";
        String[] splitted = input.split(" +");
        for (int i = 0; i < splitted.length; ++i) {
            String s = splitted[i];
            s = s.toLowerCase(Locale.ENGLISH);
            pattern += s;
            pattern += "\\S*";
            if (i < splitted.length - 1)
                pattern += "\\s+";
        }
        pattern += ".*";

        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

        List<String> list = new ArrayList<String>();
        for (String typeName : rcNames()) {
            Matcher m = p.matcher(typeName);
            if (m.find())
                list.add(typeName);
        }
        return list;
    }

    /**
     * Returns a list of resource classes which matches the comparator. In
     * particular, the the comparator's {@link Comparable#compareTo(Object)}
     * method returns 0 for all elements contained in the returned list. The
     * returned list is not sorted.
     * 
     * @param comparable an instance of {@link Comparable}
     * @return an unsorted list of matching resource classes
     */
    public static final List<SWGResourceClass> rcsFor(
            Comparable<SWGResourceClass> comparable) {

        List<SWGResourceClass> cls = new ArrayList<SWGResourceClass>
                (SWGResourceClassInfo.LISTSIZE);

        for (SWGResourceClass rc : SWGResourceClassInfo.swgIDtoInstance)
            if (rc != null && comparable.compareTo(rc) == 0)
                cls.add(rc);

        return cls;
    }

    /**
     * Determines if all integers of the specified argument are within caps; see
     * {@link #validate(SWGResourceStats, SWGResourceClass)}.
     * 
     * @param values the values to validate
     * @param rc a resource class constant
     * @return {@code null} or an error message
     * @throws IllegalArgumentException if {@code values.length != }
     *         {@link Stat#COUNT}
     * @throws NullPointerException if an argument is {@code null}
     */
    public static final String validate(int[] values, SWGResourceClass rc) {
        if (values.length != Stat.COUNT)
            throw new IllegalArgumentException("Invalid size: " + values.length);

        ZString err = new ZString();

        String sf = "%s: %4d [%4d - %4d]\n";
        for (Stat s : Stat.values()) {
            int v = values[s.i];
            if (v < rc.min(s) || v > rc.max(s))
                err.app(String.format(sf, s.name(),
                        Integer.valueOf(v), Integer.valueOf(rc.min(s)),
                        Integer.valueOf(rc.max(s))));
        }

        return (err.isEmpty()
                ? null
                : err.pre("Stats outside caps:\n", false).trimmed());
    }

    /**
     * Determines if all integers of the specified argument are within the caps
     * which are set by the specified resource class. If all values are valid
     * this method returns {@code null}, otherwise it returns a text that reads
     * the errors.
     * <p>
     * The error text is at the following, one line per offending value:
     * 
     * <pre> Stats outside caps:
     * CD:   23 [  60 -   75]
     * SR: 1000 [ 650 -  680]</pre>
     * 
     * @param stats values for a resource
     * @param rc a resource class constant
     * @return {@code null} or an error message
     * @throws NullPointerException if an argument is {@code null}
     */
    public static final String validate(
            SWGResourceStats stats, SWGResourceClass rc) {
        return validate(stats.safeValues(), rc);
    }

    /**
     * A helper type that denotes the top-most node of the tree of resource
     * classes. This type is only used to represent the abstract topmost node of
     * {@link SWGResourceClassTree} and in GUI trees and their models.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    @SuppressWarnings("serial")
    public static final class SWGResourceClassRoot extends SWGResourceClass {

        /**
         * Creates an instance of this type.
         */
        private SWGResourceClassRoot() {
            super();
        }

        @Override
        public int expectedStats() {
            return 0;
        }

        @Override
        public boolean has(Stat s) {
            return false;
        }

        @Override
        public boolean isSub(SWGResourceClass superType) {
            return false;
        }

        @Override
        public int max(Stat s) {
            return 0;
        }

        @Override
        public int min(Stat s) {
            return 0;
        }

        @Override
        public int rcID() {
            return 0;
        }

        @Override
        public String rcName() {
            return "All Resource Classes";
        }

        @Override
        public String rcToken() {
            return "";
        }

        @Override
        public int sortIndex() {
            return 0;
        }
    }
}
