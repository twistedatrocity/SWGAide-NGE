package swg.swgcraft;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.SWGAide;
import swg.crafting.Stat;
import swg.crafting.resources.SWGMutableResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceStats;
import swg.model.SWGCGalaxy;
import swg.tools.ZString;

/**
 * This type represents the response for the SOAP action
 * swgcraft#GetResourceInfo and the SOAP action
 * swgcraft#GetResourceInfoFromIDRequest. The response is defined by
 * swgcraft:ResourceInfo
 * 
 * <pre>{@code <xsd:complexType name="ResourceInfo">
 *     <xsd:all>
 *         <xsd:element name="ID" type="xsd:integer"/>
 *         <xsd:element name="Class" type="xsd:integer"/>
 *         <xsd:element name="ER" type="xsd:integer"/>
 *         <xsd:element name="CR" type="xsd:integer"/>
 *         <xsd:element name="CD" type="xsd:integer"/>
 *         <xsd:element name="DR" type="xsd:integer"/>
 *         <xsd:element name="FL" type="xsd:integer"/>
 *         <xsd:element name="HR" type="xsd:integer"/>
 *         <xsd:element name="MA" type="xsd:integer"/>
 *         <xsd:element name="OQ" type="xsd:integer"/>
 *         <xsd:element name="SR" type="xsd:integer"/>
 *         <xsd:element name="UT" type="xsd:integer"/>
 *         <xsd:element name="PE" type="xsd:integer"/>
 *         <xsd:element name="AddedStamp" type="xsd:integer"/>
 *         <xsd:element name="AddedBy" type="xsd:integer"/>
 *         <xsd:element name="ServerID" type="xsd:integer"/>
 *         <xsd:element name="Name" type="xsd:string"/>
 *     </xsd:all>
 * </xsd:complexType>}</pre>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
final class SWGSoapResourceResponse extends SWGSoapResponse {

    /**
     * The date for when the resource was first reported to SWGCraft, or -1 if
     * the date is unknown. The time is in seconds since January 1, 1970.
     */
    private long available = -1;

    /**
     * The SWGCraft user ID for the person who first reported this resource, or
     * -1 if the person is unknown.
     */
    private int availableBy = -1;

    /**
     * The galaxy constant denoting the galaxy the resource spawned at.
     */
    private SWGCGalaxy galaxy;

    /**
     * The resource contained by this instance. This implementation sets all
     * fields there are values for.
     */
    private SWGMutableResource resource;

    /**
     * The name for the resource the response is about.
     */
    private String resourceName;

    /**
     * The unique SWGCraft resource ID, or -1 if the ID is unknown.
     */
    private long swgcraftID = -1;

    /**
     * Creates an instance of this type with the specified SWGCraft ID. This
     * constructor is only used by {@link SWGSoapManager#requestResource(long)}.
     * 
     * @param swgcraftID
     *            the unique SWGCraft resource ID
     * @throws IllegalArgumentException
     *             if the ID is invalid
     */
    SWGSoapResourceResponse(long swgcraftID) {
        if (swgcraftID <= 0)
            throw new IllegalArgumentException("Invalid ID: " + swgcraftID);

        this.swgcraftID = swgcraftID;
    }

    /**
     * Creates an instance of this type with the specified arguments. This
     * constructor is only used by
     * {@link SWGSoapManager#requestResource(String, SWGCGalaxy)}.
     * 
     * @param resourceName
     *            the name for the resource the response is about
     * @param galaxy
     *            a galaxy constant for the galaxy the resource spawned at
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    SWGSoapResourceResponse(String resourceName, SWGCGalaxy galaxy) {
        if (resourceName == null || galaxy == null)
            throw new NullPointerException("An argument is null");

        this.resourceName = resourceName;
        this.galaxy = galaxy;
    }

    /**
     * Returns the date this resource was first reported to SWGCraft. If the
     * date is unknown -1 is returned. The time is in seconds since January 1,
     * 1970.
     * 
     * @return the date this resource was first reported, or -1
     */
    long getAvailable() {
        return available;
    }

    /**
     * Returns the SWGCraft user ID for the person who first reported this
     * resource. If the person is unknown -1 is returned.
     * 
     * @return the SWGCraft user ID for who first reported this resource, or -1
     */
    int getAvailableBy() {
        return availableBy;
    }

    /**
     * Returns the galaxy constant denoting the galaxy the resource spawned at.
     * 
     * @return the galaxy constant
     */
    SWGCGalaxy getGalaxy() {
        return resource != null
            ? resource.galaxy()
            : galaxy;
    }

    /**
     * Returns the resource instance contained by this SOAP response.
     * 
     * @return the resource instance contained by this response
     */
    SWGMutableResource getResource() {
        return resource;
    }

    /**
     * Returns the name for the resource the response is about.
     * 
     * @return the name for the resource the response is about
     */
    String getResourceName() {
        return resource != null
            ? resource.getName()
            : resourceName;
    }

    /**
     * @return the swgcraftID
     */
    long getSwgcraftID() {
        return resource != null
            ? resource.id()
            : swgcraftID;
    }

    /*
     * (non-Javadoc)
     * @see swg.swgcraft.SWGSoapResponse#parseNode(org.w3c.dom.Node)
     */
    @Override
    void parseNode(Node node) {
        NodeList lst = node.getChildNodes();

        int typeID = SWGSoapManager.parseInt(lst, "Class");
        SWGResourceClass type = SWGResourceClass.rc(typeID);

        // need to differ between the two request types, ID vs. name/gxy
        if (resourceName == null)
            resourceName = SWGSoapManager.parseString(lst, "Name");

        // create the resource, now we know enough
        resource = new SWGMutableResource(resourceName, type);

        if (galaxy == null)
            galaxy = SWGCGalaxy.fromID(SWGSoapManager.parseInt(lst, "ServerID"));
        resource.galaxy(galaxy);

        long id = SWGSoapManager.parseLong(lst, "ID");
        if (swgcraftID <= 0 || id == swgcraftID)
            resource.id(id);
        else {
            SWGAide.printDebug("soap", 1,
                    "SoapResourceResponse:parse: ID differs, remote=" + id
                            + " local=" + swgcraftID);
        }

        SWGResourceStats stats = new SWGResourceStats();
        for (Stat s : Stat.values()) {
            int v = SWGSoapManager.parseInt(lst, s.getName());
            stats.set(s, v);
        }
        resource.stats(stats, false);

        available = SWGSoapManager.parseLong(lst, "AddedStamp");
        availableBy = SWGSoapManager.parseInt(lst, "AddedBy");
    }

    @Override
    public String toString() {
        ZString z = new ZString("ResourceInfo[");
        if (resource != null) {
            z.app(resource.toString()).app('[');
            resource.stats().toString(z, false);
            z.app(']');
        }
        return z.app(']').toString();
    }
}
