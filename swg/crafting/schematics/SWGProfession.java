package swg.crafting.schematics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.SWGAide;
import swg.swgcraft.SWGCraftCache;
import swg.tools.ZString;
import swg.tools.ZXml;

/**
 * This type models a profession which contains a set of named items. 
 */
public final class SWGProfession implements Serializable {
    
	/**
	 * 
	 */
    private static final long serialVersionUID = -5440534198982364860L;
    
	/**
     * A constant for the ID of the top-most profession that is named "All".
     */
    public static final int ALL = 0;
    
    /**
     * A constant for the type of the top-most profession that is named "All".
     */
    public static final String ALLTYPE = "ALL";

    /**
     * A list of professions
     */
    private static ArrayList<SWGProfession> professions;

    /**
     * The unique ID for this profession, greater than zero.
     */
    private final int id;

    /**
     * The proper name for this profession.
     */
    private final String name;
    
    /**
     * The proper type for this profession. Either precu or nge
     */
    private final String type;
    
    public SWGProfession() {
    	this.id=0;
    	this.name="All";
    	this.type="ALL";
    	init();
    }
    /**
     * Creates an instance of this type, initiated with values from the
     * specified XML element, see {@link #SWGProfession(String name, String type, int id)} for
     * details. The element is on the form
     * 
     * <pre>{@literal <profession id="0" type="precu" name="Name" />}</pre>
     * 
     * 
     * @param xml
     *            a XML element
     * @throws IllegalArgumentException
     *             if there is an illegal value in the element
     * @throws NullPointerException
     *             if the element has no attribute for name
     */
    SWGProfession(Element xml) {
        this(
            ZXml.stringFromAttr(xml, "name"),
            ZXml.stringFromAttr(xml, "type"),
            ZXml.intFromAttr(xml, "id") );
    }
    
    /**
     * Creates an instance of this type, initiated with the specified arguments.
     * If a SWGCraft ID is unavailable the ID is greater than one million to
     * denote the difference.
     * 
     * @param name
     *            the proper name for this profession
     * @param type
     *            the proper type for this profession
     * @param id
     *            the unique ID for this profession
     * @throws IllegalArgumentException
     *             if {@code id < 0 || pid < 0} or if there is no name
     * @throws NullPointerException
     *             if name is {@code null}
     */
    public SWGProfession(String name, String type, int id) {
        if (name == null)
            throw new NullPointerException("Name is null");
        if (name.isEmpty())
            throw new IllegalArgumentException("No name");

        this.name = name;
        this.type = type;
        this.id = id;
    }
    
    /**
     * Determines if the specified object equals this object. In particular,
     * this implementation returns {@code true} if {@code this == other || other
     * == ALL || this == ALL}.
     * 
     * @param other
     *            the other profession object to check
     * @return {@code true} if the argument is considered equal
     */
    public boolean equalsProfession(SWGProfession other) {
        return (other == this || other.getID() == ALL || this.getID() == ALL);
    }
    
    void init() {
        this.add(new SWGProfession("All", "ALL", 0));
        Document doc = SWGCraftCache.getProfLevels();
        if (doc != null) {
        	Element main = (Element) doc.getElementsByTagName("professions").item(0);
	        NodeList ns = main.getChildNodes();
	        for (int i = 0; i < ns.getLength(); i++) {
	        	Node n = ns.item(i);
	            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("profession")) {
	            	Element cur = (Element) n;
	                try {
	                    this.add(new SWGProfession(cur));
	                } catch (Exception e) {
	                    SWGAide.printDebug("schm", 1, String.format(
	                            "SWGSchematicsManager:parseProfs: %s : %s",
	                            e.getMessage(), ZXml.stringFromAttr(
	                                    (Element) ns.item(i), "name")));
	                }
	            }
	        }
        } else {
            SWGAide.printDebug("profs", 1,
                    "SWGProfession:init: XML doc is null");
        }
    }

    /**
     * Adds the specified object to this instance. If this profession already
     * contains the argument this method does nothing.
     * <p>
     * This method is not thread-safe but access is restricted to just the
     * schematics manager and then only during the initialization phase of new
     * schematics and professions.
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

        if (obj instanceof SWGProfession)
            addProf((SWGProfession) obj);
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
    private void addProf(SWGProfession prof) {
        if (professions == null) {
        	professions = new ArrayList<SWGProfession>();
        }
        if (!professions.contains(prof)) {
        	professions.add(prof);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SWGProfession) {
            SWGProfession c = (SWGProfession) obj;
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
    public static List<SWGProfession> getProfessions() {
        if (professions == null)
            return Collections.emptyList();
        return professions;
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
     * Returns a list with names of professions in SWG.
     * <p>
     * The order of the list is alphabetical but Traders first, the very first
     * element is "All".
     * 
     * @param type The type to be filtered e.g. precu, nge or ALL
     * @return a list of profession names
     */
    public static List<String> getNames(String type) {
        ArrayList<String> ret = new ArrayList<String>();
        for (SWGProfession p : getProfessions()) {
        	if(p.type.equals(type) || p.type.equals(ALLTYPE)) {
        		ret.add(p.getName());
        	}
        }
        
        return ret;
    }
    
    public static SWGProfession filter(String type) {
		SWGProfession ret = new SWGProfession("All", "ALL", 0);
		for (SWGProfession p : getProfessions()) {
        	if(p.type == type) {
        		ret.add(p);
        	}
        }
		return ret;
	}
    
    /**
     * Returns a profession for the specified SWGCraft ID.
     * 
     * @param id
     *            a SWGCraft ID
     * @return a profession
     * @throws IllegalArgumentException
     *             if the argument does not denote a profession
     */
    public static SWGProfession getFromID(int id) {
        for (SWGProfession p : getProfessions())
            if (p.getID() == id)
                return p;

        throw new IllegalArgumentException(
            "Invalid id: " + id);
    }
    
    /**
     * Returns a profession constant for the proper name of a profession; also
     * the name from {@link #getNameShort()} is supported. If the argument is
     * invalid {@code null} is returned.
     * 
     * @param name
     *            a proper name for the profession
     * @return a profession constant, or {@code null}
     */
    public static SWGProfession getFromName(String name) {
        for (SWGProfession p : getProfessions())
            if (p.name.equals(name))
                return p;

        return null;
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

    @Override
    public String toString() {
        ZString z = new ZString("SWGProfession[").app(id).app(',');
        return z.app(type).app(',').app(name).app(']').toString();
    }
}
