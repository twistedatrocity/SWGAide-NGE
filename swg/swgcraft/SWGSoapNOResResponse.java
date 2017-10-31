package swg.swgcraft;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResourceSet;
import swg.model.SWGPlanet;
import swg.tools.ZString;

/**
 * This class represents the response for the SOAP action
 * swgcraft#SoapAddResource and it is defined by swgcraft:AddResourceOutput, see
 * further down. This type is used for sending as well new as old (historical)
 * resources to SWGCraft.org.
 * <P>
 * The status codes are:
 * <OL>
 * <LI value="1">A resource named "name" already exists, the stats have been
 * updated if it had none</LI><BR/>
 * <LI value="2">Class cannot spawn on planet</LI><BR/>
 * <LI value="3"><B>SUCCESS</B>, resource added</LI><BR/>
 * <LI value="4">Class does not exist or can never spawn</LI><BR/>
 * <LI value="5">Server does not exist</LI><BR/>
 * <LI value="6">Planet does not exist</LI><BR/>
 * <LI value="7">Illegal characters in resource name</LI><BR/>
 * <LI value="8">A resource named "name" already exists, but does not have the
 * same class as the input resource</LI><BR/>
 * <LI value="999">User ID and password do not match</LI>
 * </OL>
 * Furthermore, before submitting a resource the resource manager determines if
 * it exists a known resource with the same resource class and a very similar
 * name, which would possibly be a spelling error at SWGCraft or in the
 * to-be-submitted resource. Then the manager aborts and sets the status to -2
 * and {@link #resource} to the known resource.
 * 
 * <pre>{@code <xsd:complexType name="AddResourceOutput">
 *     <xsd:all>
 *         <xsd:element name="Status" type="xsd:integer"/>
 *         <xsd:element name="ResourceID" type="xsd:integer"/>
 *     </xsd:all>
 * </xsd:complexType>}</pre>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGSoapNOResResponse extends SWGSoapResResponse {
    
    /**
     * A set of resources that denotes some property that is interesting to
     * convey in a response from submitting a new or old resource. If this
     * member is {@code null} or an empty set there is nothing to convey.
     */
    public SWGResourceSet whatever; 

    /**
     * Creates an instance of this type using the identified arguments.
     * 
     * @param res the resource which this response is about
     * @param planet the planet which this response is about
     */
    SWGSoapNOResResponse(SWGKnownResource res, SWGPlanet planet) {
        super(res, planet);
    }

    /**
     * Returns the planet which this response is about.
     * 
     * @return the planet which this response is about
     */
    public SWGPlanet getPlanet() {
        return planet;
    }

    /**
     * This method returns -2 if this instance denotes an attempt of send-new
     * and the resource manager found a similarly named known instance.
     * Otherwise it...
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int getStatus() {
        return super.getStatus();
    }

    @Override
    public String getStatusString() {
        switch (status) {
        case -2:
            return String.format("is very similar to %n%s: %s",
                        resource.getName(), resource.rc().rcName());
        case 1:
            return "A resource named \"" + resource.getName() + " already"
                    + " exists, the stats have been updated if it had none";
        case 2:
            return "Class cannot spawn on planet: "
                    + resource.rc().rcName();
        case 3:
            return "Success, resource added";
        case 4:
            return "Class does not exist or can never spawn: "
                    + resource.rc().rcName();
        case 5:
            return "Server does not exist: " + resource.galaxy();
        case 6:
            return "Planet does not exist: " + planet.getName();
        case 7:
            return "Illegal characters in resource name: "
                    + resource.getName();
        case 8:
            return "A resource named \""
                    + resource.getName()
                    + "\" already exists, but does not have the same class as the input resource: "
                    + resource.rc();
        case 999:
            return "User ID and password do not match";
        default:
			return "Unknown status number: " + status;
		}
    }

    @Override
    public boolean isFaultless() {
        return (status == 3 || status == 1);
    }

    @Override
    void parseNode(Node node) {
        NodeList lst = node.getChildNodes();
        status = SWGSoapManager.parseInt(lst, "Status");
        if (!isFaultless()) {
            faultMessage = getStatusString();
            return;
        }

        swgcraftID = SWGSoapManager.parseInt(lst, "ResourceID");
        if (resource.id() <= 0) {
            SWGResourceManager.supplyID(resource, swgcraftID);
        } else {
            // TODO: how to handle ID > 0 but have used sendNew(res) ???
            // should rather the resource manager handle this once it gets back
            // the response?
        }
    }

    @Override
    public String toString() {
        ZString z = new ZString("NOResource: [");
        if (isFaultless())
            z.app(getStatusString()).app(" | swgcraftID=").app(swgcraftID);
        else
            z.app(faultMessage);
        return z.app(']').toString();
    }
}
