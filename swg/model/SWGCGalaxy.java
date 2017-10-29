package swg.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.swgcraft.SWGCraftCache;
import swg.swgcraft.SWGCraftCache.CacheUpdate.UpdateType;
import swg.tools.ZXml;

public class SWGCGalaxy implements Serializable {
	
	
	private static ArrayList<SWGCGalaxy> servers = new ArrayList<SWGCGalaxy>();
	
	
	static {
		initializeServers();
		
		
		
	}
	
	private static void initializeServers()
	{
		servers.clear();
		try
		{
			Document xml = ZXml.parse(new File("crafting/servers.xml"));
			
			Element main = (Element) xml.getElementsByTagName(
                    "servers").item(0);
			
			//Loop through servers
		      NodeList nl = main.getChildNodes();
	            for (int j = 0; j < nl.getLength(); ++j) {
	                Node n = nl.item(j);
	                if (n.getNodeType() == Node.ELEMENT_NODE
	                        && n.getNodeName().equals("server"))
	                {
	                	Element cur = (Element) n;
	                	//Initialize a server and throw it in our list
	                	String name = cur.getAttribute("name");
	                	int swgcraftID = ZXml.intFromAttr(cur, "swgcraft_id");
	                	String swgcraftName = cur.getAttribute("swgcraft_name");
	                	//System.out.println(cur);
	                	Boolean active = ZXml.booleanFromAttr(cur, "active");
	                	SWGCGalaxy galaxy = new SWGCGalaxy(name, swgcraftName, swgcraftID, active);
	                	servers.add(galaxy);
	                }
	            }
		}
		catch(IOException e)
		{
			System.out.println("No server file found");
			SWGCraftCache.addSubscriber(new UpdateSubscriber()
				    {
		    	public void handleUpdate(UpdateNotification u)
		    	{
		    		SWGCGalaxy.initializeServers();
		    	}
		    	
		    
		    }, UpdateType.SERVERS);
			SWGCraftCache.updateCache();
		}
		catch(ParserConfigurationException e)
		{
			
		}
		catch(SAXException e)
		{
			
		}
		
	}

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
    * The name of this galaxy constant on SWGCraft.
    */
    private final String swgcraftName;

    /**
     * @param name
     *            the name of the galaxy
     * @param id
     *            the SWGCraft galaxy ID
     * @param active
     *            {@code false} if the galaxy is closed down
     */
    private SWGCGalaxy(String name, String swgcraftName, int id, boolean active) {
        this.name = name;
		this.swgcraftName = swgcraftName;
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
        return swgcraftName;
    }

    /**
     * Returns the complete name for this galaxy constant. This method returns a
     * name such as Europe-Chimaera rather than Chimaera.
     * 
     * @return the full name
     */
    public String getNameComplete() {
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
		//Inefficient, yes
    	for(SWGCGalaxy server : servers)
    	{
    		if(server.id() == id)
    		{
    			return server;
    		}
    	}
    	throw new IndexOutOfBoundsException("Invalid ID");
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
		//Inefficient, yes
    	for(SWGCGalaxy server : servers)
    	{
    		if(server.getName().equals(galaxy) || server.getNameComplete().equals(galaxy))
    		{
    			return server;
    		}
    	}
    	throw new IllegalArgumentException("Invalid name");
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
        for(SWGCGalaxy server : servers)
    	{
    		if(server.getName().equals(name) || server.getNameComplete().equals(name))
    		{
    			return true;
    		}
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
            List<String> l = new ArrayList<String>();
            for (SWGCGalaxy g : servers)
            {
            	l.add(g.getName());
            }
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
        List<String> ret = new ArrayList<String>();
        for (SWGCGalaxy g : servers)
        {
            if (g.isActive())
                ret.add(g.getName());
        }
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
	
	public static SWGCGalaxy defaultGalaxy()
    {
    	if(isNameValid("SWGCraft.co.uk"))
    	{
    		return fromName("SWGCraft.co.uk");
    	}
    	else
    	{
    		SWGCGalaxy defaultGalaxy = new SWGCGalaxy("SWGCraft.co.uk", "SWGCraft.co.uk", 99, true);
    		servers.add(defaultGalaxy);
    		return defaultGalaxy;
    	}
    }
    
    public static Object[] values()
    {
    	return servers.toArray();
    }
    
   /* public static void main(String[] args)
    {
    	//Do nothing
    	System.out.println("Hej");
    	
    	System.out.println(names());
    }*/
}
