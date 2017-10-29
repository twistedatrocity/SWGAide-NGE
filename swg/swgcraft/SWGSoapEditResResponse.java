package swg.swgcraft;

import org.w3c.dom.Node;

import swg.crafting.resources.SWGKnownResource;
import swg.tools.ZString;

/**
 * This class represents the response for the SOAP action
 * swgcraft#SoapEditResource and it is defined by SoapEditResourceResponse.
 * <P>
 * The status codes are:
 * <OL>
 * <LI value="1">Resource does not exist</LI><BR/>
 * <LI value="2">Not spawnable</LI><BR/>
 * <LI value="3"><B>SUCCESS</B>, resource updated</LI><BR/>
 * <LI value="4">Class does not exist or cannot spawn</LI><BR/>
 * <LI value="5">Server does not exist</LI><BR/>
 * <LI value="6">The stats you entered does not match the resource type</LI><BR/>
 * <LI value="7">The resource name contains invalid characters</LI><BR/>
 * <LI value="8">One or more stats are out out range (1-1000)</LI><BR/>
 * <LI value="9">Invalid datatype for a stat</LI><BR/>
 * <LI value="10">Resource name already exists on server</LI><BR/>
 * <LI value="998">Invalid datatypes</LI><BR/>
 * <LI value="999">User ID and password do not match</LI>
 * </OL>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public final class SWGSoapEditResResponse extends SWGSoapResResponse {

    /**
     * Creates an instance of this type using the identified argument.
     * 
     * @param resource
     *            the edited resource which this response is about
     */
    SWGSoapEditResResponse(SWGKnownResource resource) {
        super(resource, null);
    }

    /*
     * (non-Javadoc)
     * @see swg.swgcraft.SWGCraftStatusResponse#getStatusString()
     */
    @Override
    public String getStatusString() {
        switch (status) {
            case 1:
                return "Resource does not exist: " + resource.getName();
            case 2:
                return "Class cannot spawn: " + resource.rc().rcName();
            case 3:
                return "Success, resource updated";
            case 4:
                return "Class does not exist or cannot spawn: "
                    + resource.rc().rcName();
            case 5:
                return "Galaxy name does not exist: " + galaxy.getName();
            case 6:
                return "Entered stats mismatch the resource class";
            case 7:
                return "Resource name has invalid characters: "
                    + resource.getName();
            case 8:
                return "Some stats are out ouf range [1-1000]";
            case 9:
                return "Not an integer for a stat";
            case 10:
                return "Resource name already exists on galaxy: "
                    + resource.getName();
            case 998:
                return "Invalid data types";
            case 999:
                return "User ID and password do not match";
        }
        return "Unknown status number: " + status;
    }

    @Override
    public boolean isFaultless() {
        return (status == 3);
    }

    @Override
    void parseNode(Node node) {
        status = Integer.parseInt(node.getTextContent());
        if (status != 3) {
            faultMessage = getStatusString();
        }
    }

    @Override
    public String toString() {
        ZString z = new ZString("EditResource: [");
        if (isFaultless())
            z.app(getStatusString()).app("m swgcraftID=").app(swgcraftID);
        else
            z.app(faultMessage);
        return z.app(']').toString();
    }
}
