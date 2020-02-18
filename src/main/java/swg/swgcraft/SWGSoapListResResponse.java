package swg.swgcraft;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.crafting.resources.SWGResourceClass;
import swg.model.SWGCGalaxy;
import swg.tools.ZString;

/**
 * This class represents the response for the SOAP action swgcraft#FindResources
 * and it is defined by swgcraft:SimpleResourceList. This implementation
 * provides a {@link List} of instances of {@link SWGResourceTuple} which is
 * defined by swgcraft:SimpleResourceInfo.
 * 
 * <pre>{@code <xsd:complexType name="SimpleResourceList">
 *     <xsd:complexContent>
 *         <xsd:restriction base="SOAP-ENC:Array">
 *             <xsd:attribute ref="SOAP-ENC:arrayType" 
 *                 wsdl:arrayType="tns:SimpleResourceInfo[]"/>
 *         </xsd:restriction>
 *     </xsd:complexContent>
 * </xsd:complexType>
 * 
 * <xsd:complexType name="SimpleResourceInfo">
 *     <xsd:all>
 *         <xsd:element name="Name" type="xsd:string"/>
 *         <xsd:element name="ClassID" type="xsd:integer"/>
 *     </xsd:all>
 * </xsd:complexType>}</pre>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public final class SWGSoapListResResponse extends SWGSoapResponse {

    /**
     * The galaxy constant denoting the galaxy this response is about.
     */
    private final SWGCGalaxy galaxy;

    /**
     * A list of resource tuples. Each element contains a resource name and its
     * resource class.
     */
    private final List<SWGResourceTuple> tuples;

    /**
     * Creates an instance of this type.
     * 
     * @param galaxy
     *            the galaxy for the response
     */
    SWGSoapListResResponse(SWGCGalaxy galaxy) {
        this.galaxy = galaxy;
        tuples = new ArrayList<SWGResourceTuple>();
    }

    /**
     * Returns the galaxy constant denoting the galaxy this response is about.
     * 
     * @return the galaxy constant for this response
     */
    public SWGCGalaxy getGalaxy() {
        return galaxy;
    }

    /**
     * Returns a copy of the list of resource tuples. The size of the list is
     * zero, one, or more elements. In the latter case all resources have names
     * that are longer than the name originally suggested by the user. The
     * beginning of these names all equal the suggested name.
     * 
     * @return a copy of the list of resource tuples, or an empty list
     */
    public List<SWGResourceTuple> getTuples() {
        return new ArrayList<SWGResourceTuple>(tuples);
    }

    /**
     * Helper method that parses a node that wraps a
     * swgcraft:SimpleResourceInfo. From the argument this implementation
     * creates and returns a {@link SWGResourceTuple}.
     * 
     * @param node
     *            the node to parse
     * @return the new tuple
     */
    private SWGResourceTuple newTuple(Node node) {
        Node nameNode = node.getFirstChild();
        Node name = nameNode.getFirstChild();

        Node typeNode = nameNode.getNextSibling();
        Node type = typeNode.getFirstChild();

        String n = name.getNodeValue();
        int typeID = Integer.parseInt(type.getNodeValue());
        SWGResourceClass t = SWGResourceClass.rc(typeID);
        SWGResourceTuple tuple = new SWGResourceTuple(n, t);
        return tuple;
    }

    @Override
    void parseNode(Node node) {
        NodeList lst = node.getChildNodes();

        final int len = lst.getLength();
        ((ArrayList<SWGResourceTuple>) tuples).ensureCapacity(len);

        for (int i = 0; i < len; ++i) {
            SWGResourceTuple t = newTuple(lst.item(i));
            tuples.add(t);
        }
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGSoapListResResponse: [");
        if (isFaultless()) {
            z.app(galaxy.getName()).app(": ");
            z.app(tuples.size()).app(" resources");
        } else
            z.app("No resources found");
        return z.app(']').toString();
    }
}
