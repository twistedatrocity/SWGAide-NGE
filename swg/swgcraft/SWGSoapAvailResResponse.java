package swg.swgcraft;

import org.w3c.dom.Node;

import swg.crafting.resources.SWGKnownResource;
import swg.model.SWGPlanet;
import swg.tools.ZString;

/**
 * This class represents the response for the SOAP action
 * swgcraft#SoapAddAvailability and it is defined by
 * swgcraft:SoapAddAvailabilityResponse.
 * <P>
 * The status codes are:
 * <OL>
 * <LI value="1">Already exists on chosen galaxy/planet, availability might be
 * updated</LI><BR/>
 * <LI value="2">Not spawnable</LI><BR/>
 * <LI value="3"><B>SUCCESS: </B>Exists on galaxy, updated for planet</LI><BR/>
 * <LI value="4">Resource does not exist on the chosen galaxy</LI><BR/>
 * <LI value="5">Galaxy does not exist</LI><BR/>
 * <li value="6">Planet does not exist</LI><BR/>
 * <LI value="7">The availability for this resource has been locked down; more
 * than 25 days have passed since it was first marked available</LI><BR/>
 * <LI value="999">User ID and password do not match</LI>
 * </OL>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public final class SWGSoapAvailResResponse extends SWGSoapResResponse {

    /**
     * Creates an instance of this type using the identified arguments.
     * 
     * @param resource
     *            the resource this report is about
     * @param planet
     *            the planet this response is about
     */
    SWGSoapAvailResResponse(SWGKnownResource resource, SWGPlanet planet) {
        super(resource, planet);
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
     * Returns the response status from server as text
     * 
     * @return response status from server as text
     */
    @Override
    public String getStatusString() {
        switch (status) {
        case 1:
            return "Already exists on " + galaxy.getName() + "/"
                    + planet.getName() + ", availability might be updated";
        case 2:
            return "Not spawnable";
        case 3:
            return "Success, exists on " + galaxy.getName()
                    + ", updated for " + planet.getName();
        case 4:
            return "Resource does not exist on " + galaxy.getName();
        case 5:
            return "\"" + galaxy.getName() + "\" does not exist";
        case 6:
            return "\"" + planet.getName() + "\" does not exist";
        case 7:
            return "The availability for this resource has been locked " +
                        "down; more than 25 days have passed since it was " +
                        "first marked available";
        case 999:
            return "User ID and password do not match";
        }
        return "Unknown status code: " + status;
    }

    @Override
    public boolean isFaultless() {
        return (status == 3 || status == 1);
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
        ZString z = new ZString("AddAvailable: [");
        z.app(this.isFaultless()
                ? getStatusString()
                : faultMessage).app(']');
        return z.toString();
    }
}
