package swg.swgcraft;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.tools.ZString;

/**
 * This type represents the response from an interaction with SWGCraft's SOAP
 * server. This type is sub-typed for other actions, but actions directly using
 * this type are: <BR/>
 * swgcraft#SoapDespawn defined by swgcraft:SoapDespawnResponse<BR/>
 * swgcraft#HashPass defined by swgcraft:HashPassResponse
 * <OL>
 * <LI value="1">Success</LI><BR/>
 * <LI value="999">User ID and password do not match</LI>
 * </OL>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public class SWGSoapStatusResponse extends SWGSoapResponse {

    /**
     * The SWGCraft ID for the resource which this response is about, or -1 if
     * this response is not about a resource or if the ID is unknown.
     */
    protected long swgcraftID = -1;

    /**
     * Creates an instance of this type with the SWGCraft resource ID. If this
     * response is not about a resource, or if the ID is unknown, the value is
     * -1.
     * 
     * @param resourceID
     *            the SWGCraft resource ID for the resource which this response
     *            is about, or -1
     */
    protected SWGSoapStatusResponse(long resourceID) {
        swgcraftID = resourceID <= 0
            ? -1
            : resourceID;
    }

    @Override
    public String getStatusString() {
        switch (status) {
            case 1:
                return "Success";
            case 999:
                return "User ID and password do not match";
        }
        return "Unknown status number: " + status;
    }

    /**
     * Returns the SWGCraft ID for the resource which this response is about. If
     * this response is not about a resource, or if the ID is unknown, -1 is
     * returned.
     * 
     * @return the SWGCraft ID, or -1
     */
    public final long getSWGCraftID() {
        return swgcraftID;
    }

    @Override
    public boolean isFaultless() {
        return (status == 1);
    }

    @Override
    void parseNode(Node node) {
        NodeList lst = node.getChildNodes();
        status = SWGSoapManager.parseInt(lst, "Status");
    }

    @Override
    public String toString() {
        ZString z = new ZString("StatusResponse: [");
        z.app(faultMessage == null
                ? getStatusString()
                : faultMessage).app(']');
        return z.toString();
    }
}
