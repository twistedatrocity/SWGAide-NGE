package swg.crafting.schematics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type models a category which contains a set of named items. A category
 * is a designation, a defined term for one or several items or sub-categories.
 * An instance of this type reads the name of the category and some identifying
 * attributes, and it contains a set of one or more elements. The elements are
 * discrete items, schematics, or yet other categories recursively.
 * <p>
 * To model categories in SWG we must use two kinds of categories, though both
 * are embraced by this type: Bazaar categories and Component categories.<br/>
 * <b>Bazaar categories</b>: they are displayed at the Bazaar terminal, in the
 * inventory's non-icon view, and at the schematics in the datapad; usually
 * there are only two levels, a top category and its sub-categories.<br/>
 * <b>Component categories</b>: such a category is <i>defined by name&nbsp;</i>
 * by a schematic's component slot which calls does not call for a named item or
 * schematic. Component categories are not visible at the Bazaar but they are
 * indeed defined by and visible in schematics.
 * <p>
 * All schematics are categorized by what they produce, for example "Food" or
 * "Pistol". The categories "Food" and "Pistol" are sub-categories under "Misc"
 * and "Weapon" respectively. However, some schematics fall in a component
 * category because they can be used in a component slot which calls for a
 * component category. An example is the component category "Drink Container"
 * which contains Small Glass, Large Glass, etc., and any of these items are
 * acceptable in a slot that calls for "Drink Container". A component category
 * is always contained by a parent category, which eventually is a Bazaar
 * category.
 * <p>
 * Some items in component categories may be better than others, such as Cask is
 * the best Drink Container. Other items rather contribute different properties
 * to the final item, such as the variety of droid modules.
 * <p>
 * Some schematics also accept components of inferior power than the named
 * schematic the component slot calls for. For example, schematics that call for
 * Medium or Heavy Food Additive also accept Light or Medium Food Additive. To
 * model this both SWGCraft and SWGAide use component categories that chain each
 * other from the weaker to the stronger. Hence, clients may display schematics
 * and items from the named component category and its children as suitable
 * options.
 * <p>
 * At a high level categories are used to filter the display for specified items
 * or schematics, in SWG as well as in SWGAide. At a low level categories are
 * used by component slots in schematics.
 * <p>
 * This type does not implement {@code Serializable}. A client that must save a
 * persistent reference to a specific category would rather save its SWGCraft ID
 * and obtain an instance from {@link SWGSchematicsManager} on demand. This type
 * is thread safe by design, there is no public setter.
 * <p>
 * <b>Notice:</b> Each category is unique and there must exist at most one
 * instance per category. Hence instances must only be created and populated by
 * {@link SWGSchematicsManager} which maintains the singleton property.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGCategory implements Comparable<SWGCategory> {
    
    /**
     * A constant for the ID of the top-most category that is named "All".
     */
    public static final int ALL = 0;
    
    /**
     * A constant for the type of the top-most category that is named "All".
     */
    public static final String ALLTYPE = "ALL";

    /**
     * A list of sub-categories which fall into this category, or {@code null}
     * if there is no sub-category.
     */
    private List<SWGCategory> categories;

    /**
     * The unique ID for this category, greater than zero. If a SWGCraft ID is
     * not available this ID is greater than one million to denote the
     * difference.
     */
    private final int id;

    /**
     * A list of names of items which fall into this category, or {@code null}
     * if there is no item in this category. In SWGAide elements in this list
     * should be non-craftable items because schematics are stand-ins for all
     * craftable items.
     */
    private List<String> items;

    /**
     * The proper name for this category. There is no requirement that the name
     * is unique, in SWG there is a Bazaar sub-category "Component" within
     * "Component".
     */
    private final String name;
    
    /**
     * The proper type for this category. Either precu or nge
     */
    private final String type;

    /**
     * The unique SWGCraft ID for the parent category that contains this
     * instance. If this is a top-level Bazaar category there is no parent
     * category and this value is 0; see {@link #id}.
     */
    private final int parentID;

    /**
     * A list of schematics which fall into this category, or {@code null} if
     * there is no schematic in this category.
     */
    private List<SWGSchematic> schematics;

    /**
     * Creates an instance of this type, initiated with values from the
     * specified XML element, see {@link #SWGCategory(String, int, int)} for
     * details. The element is on the form
     * 
     * <pre>{@literal <category id="0" type="precu" parent_id="0" name="Name" />}</pre>
     * 
     * <b>Notice:</b> This constructor should only be invoked by
     * SWGCategoryManager which maintains the singleton property for this type
     * and populates instances of this type properly.
     * <p>
     * It is not required that the specified parent category already is
     * instantiated, something that is managed by the category manager.
     * 
     * @param xml
     *            a XML element
     * @throws IllegalArgumentException
     *             if there is an illegal value in the element
     * @throws NullPointerException
     *             if the element has no attribute for name
     */
    SWGCategory(Element xml) {
        this(
            ZXml.stringFromAttr(xml, "name"),
            ZXml.stringFromAttr(xml, "type"),
            ZXml.intFromAttr(xml, "id"),
            ZXml.intFromAttr(xml, "parent_id"));
    }
    
    /**
     * Creates an instance of this type, initiated with the specified arguments.
     * If a SWGCraft ID is unavailable the ID is greater than one million to
     * denote the difference. If this is a top-level Bazaar category the parent
     * ID is zero.
     * <p>
     * <b>Notice:</b> This constructor should only be invoked by
     * SWGCategoryManager which maintains the singleton property for this type
     * and populates instances of this type properly.
     * <p>
     * It is not required that the specified parent category already is
     * instantiated, something that is managed by the category manager.
     * 
     * @param name
     *            the proper name for this category
     * @param type
     *            the proper type for this category
     * @param id
     *            the unique ID for this category
     * @param pid
     *            the ID for the parent category, or 0 if this is a top-level
     *            category
     * @throws IllegalArgumentException
     *             if {@code id < 0 || pid < 0} or if there is no name
     * @throws NullPointerException
     *             if name is {@code null}
     */
    SWGCategory(String name, String type, int id, int pid) {
        if (name == null)
            throw new NullPointerException("Name is null");
        if (name.isEmpty())
            throw new IllegalArgumentException("No name");
        if (id < 0 || pid < 0)
            throw new IllegalArgumentException(
                String.format("Invalid ID: %s %d %d",
                name, Integer.valueOf(id), Integer.valueOf(pid)));

        this.name = name;
        this.type = type;
        this.id = id;
        parentID = pid;
    }

    /**
     * Adds the specified object to this instance. If this category already
     * contains the argument this method does nothing. The argument is a
     * {@link SWGCategory}, a {@link SWGSchematic}, or the name of a
     * non-craftable item as {@link String}.
     * <p>
     * This method is not thread-safe but access is restricted to just the
     * schematics manager and then only during the initialization phase of new
     * schematics and categories.
     * 
     * @param obj
     *            an object
     * @throws IllegalArgumentException
     *             if the argument equals this instance itself
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    void add(Object obj) {
        if (obj == null)
            throw new NullPointerException("Argument is null");

        if (obj instanceof SWGCategory)
            addCat((SWGCategory) obj);

        else if (obj instanceof SWGSchematic)
            addSchem((SWGSchematic) obj);

        else {
            if (items == null)
                items = new ArrayList<String>();

            String s = (String) obj;
            if (!items.contains(s))
                items.add(s);
        }
    }

    /**
     * Helper method which adds the specified category to the list of
     * sub-categories. If this category already contains the argument, this
     * method does nothing.
     * 
     * @param cat
     *            a category
     * @throws IllegalArgumentException
     *             if the argument equals this instance
     */
    private void addCat(SWGCategory cat) {
        if (cat == this || cat.id == this.id)
            throw new IllegalArgumentException("Adding self to self");

        if (categories == null)
            categories = new ArrayList<SWGCategory>();
        
        if (!categories.contains(cat))
            categories.add(cat);
    }

    /**
     * Helper method which adds the specified schematic to this instance. If
     * this category already contains the argument this method does nothing.
     * 
     * @param schem
     *            a schematic
     */
    private void addSchem(SWGSchematic schem) {
        if (schematics == null)
            schematics = new ArrayList<SWGSchematic>();

        if (!schematics.contains(schem))
            schematics.add(schem);
    }

    @Override
    public int compareTo(SWGCategory o) {
        return name.compareToIgnoreCase(o.name);
    }

    /**
     * Determines of this category contains the specified argument. The argument
     * is a category, a schematic, or an item.
     * 
     * @param obj
     *            the object to find
     * @return {@code true} if this instance contains the object
     */
    public boolean contains(Object obj) {
        if (obj instanceof SWGCategory)
            return categories != null && categories.contains(obj);
        if (obj instanceof SWGSchematic)
            return schematics != null && schematics.contains(obj);
        if (obj instanceof SWGSchematic)
            return items != null && items.contains(obj);
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SWGCategory) {
            SWGCategory c = (SWGCategory) obj;
            return c.id == this.id;
        }

        return false;
    }

    /**
     * Returns a list of sub-categories which are contained by this category, or
     * an empty list. The returned list is <b>read-only</b>.
     * 
     * @return a list of sub-categories, or {@link Collections#EMPTY_LIST}
     */
    public List<SWGCategory> getCategories() {
        if (categories == null)
            return Collections.emptyList();
        return categories;
    }

    /**
     * Returns the unique ID for this category. Usually this is the SWGCraft ID
     * but if a SWGCraft ID is unavailable the value is greater than one million
     * to denote the difference. {@link #ALL} is used for the top-most umbrella
     * category.
     * 
     * @return the ID
     */
    public int getID() {
        return id;
    }

    /**
     * Returns a list of items which are contained by this category, or an empty
     * list. The returned list is <b>read-only</b>.
     * 
     * @return a list of items, or {@link Collections#EMPTY_LIST}
     */
    public List<String> getItems() {
        if (items == null)
            return Collections.emptyList();
        return items;
    }

    /**
     * Returns the proper name for this category.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the proper type for this category.
     * 
     * @return the name
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the SWGCraft ID of the parent category that contains this
     * category, or 0 if this is a top-level Bazaar category. Usually this is
     * the SWGCraft ID but if a SWGCraft ID is unavailable the value is greater
     * than one million to denote the difference.
     * 
     * @return the parent ID, or 0
     */
    public int getParentID() {
        return parentID;
    }

    /**
     * Returns a list of schematics which are contained by this category, or an
     * empty list. The returned list is <b>read-only</b>.
     * 
     * @return a list of schematics, or {@link Collections#EMPTY_LIST}
     */
    public List<SWGSchematic> getSchematics() {
        if (schematics == null)
            return Collections.emptyList();
        return schematics;
    }

    @Override
    public int hashCode() {
        int ret = 17;
        ret += 499 * id;
        return ret;
    }

    /**
     * Returns the total number of elements contained by this category. The
     * value is the sum of categories, schematics, and items; the elements are
     * directly contained by this instance, not recursively traversed.
     * 
     * @return the total number of elements
     */
    public int size() {
        int s = categories == null ? 0 : categories.size();
        s += schematics == null ? 0 : schematics.size();
        s += items == null ? 0 : items.size();
        return s;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGCategory[").app(id).app(',');
        return z.app(parentID).app(',').app(name).app(']').toString();
    }
}
