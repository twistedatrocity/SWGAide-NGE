package swg.tools;

import java.io.PrintStream;
import java.net.URL;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.SWGAide;
import swg.SWGConstants;

/**
 * This class is a utility class that provides some basic SOAP management for
 * building SOAP messages and sending/receiving SOAP actions over Internet. This
 * implementation must be extended by a sub-class that implements the particular
 * SOAP actions that will be used. Hence most methods have the access modifier
 * {@code protected}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public class SOAPManager {

    /**
     * The target name space prefix.
     */
    private String nameSpacePrefix;

    /**
     * The SOAP connection factory for creating connections.
     */
    private SOAPConnectionFactory soapConnectionFactory;

    /**
     * The SOAP factory for creating SOAP messages.
     */
    private SOAPFactory soapFactory;

    /**
     * The target name space for the SOAP messaging.
     */
    private String targetNameSpace;

    /**
     * The URL for the SOAP service provider.
     */
    private URL url;

    /**
     * Creates a plain instance with a {@link SOAPConnectionFactory} and a
     * {@link SOAPFactory}. The setters listed below must be used before it is
     * possible to use this instance.
     * 
     * @see #setNameSpacePrefix(String)
     * @see #setTargetNameSpace(String)
     * @see #setURL(URL)
     * @throws Exception
     *             something went wrong with obtaining a new instance of a
     *             {@link SOAPConnectionFactory} or a {@link SOAPFactory}
     */
    protected SOAPManager() throws Exception {
        soapConnectionFactory = SOAPConnectionFactory.newInstance();
        soapFactory = SOAPFactory.newInstance();
    }

    /**
     * Adds a child element to the identified SOAP body element with the
     * identified value. More specifically, this implementation creates a
     * {@link Name} and adds it to the SOAP body element. Adding the name to the
     * body implicitly creates a new named SOAP element, the child, and adds it
     * to the body element. To this named child element the value is added.
     * <P>
     * Optionally the new child can be added to the specified SOAP element. If
     * the optional element is {@code null} the new element is added to the body
     * element.
     * 
     * @param childName
     *            the name for the child element
     * @param childValue
     *            the text content for the child element
     * @param sbe
     *            the body element for the child
     * @param se
     *            an optional SOAP element to which to add the new named child
     *            element, or {@code null} if the new child element should be
     *            added directly to the body element
     * @throws SOAPException
     *             is there is an error
     */
    protected void addChildElement(String childName, String childValue,
        SOAPBodyElement sbe, SOAPElement se) throws SOAPException {

        Name cn = soapFactory.createName(childName);
        SOAPElement element = sbe.addChildElement(cn);
        element.addTextNode(childValue);
        if (se != null) {
            se.addChildElement(element);
        }
    }

    /**
     * Returns a SOAP factory that can create SOAP message.
     * 
     * @return a SOAP factory
     */
    protected SOAPFactory getSoapFactory() {
        return soapFactory;
    }

    /**
     * Gives a name to the identified body and returns a new SOAPBodyElement.
     * More specifically this implementation creates a new {@link Name}
     * initialized with name for body, {@link #nameSpacePrefix}, and a name
     * space URI using {@link #targetNameSpace} plus the body name. When that
     * {@link Name} is added to the identified SOAP body a
     * {@link SOAPBodyElement} is implicitly created with the new name and added
     * to the body. The new element is returned and allows for adding more
     * children.
     * 
     * @param body
     *            the SOAP body to name
     * @param bodyName
     *            the name for the action
     * @return the SOAP body element to which more children can be added
     * @throws SOAPException
     *             if there is an error
     */
    protected SOAPBodyElement nameBody(SOAPBody body, String bodyName)
        throws SOAPException {

        String tns = targetNameSpace + '#' + bodyName;
        Name bn = soapFactory.createName(bodyName, nameSpacePrefix, tns);
        return body.addBodyElement(bn);
    }

    /**
     * Creates and returns a SOAP body element with the identified name. The
     * returned body element is wrapped by the identified SOAP message.
     * 
     * @param name
     *            the name for the body element
     * @param msg
     *            the SOAP message to obtain a SOAP body element for
     * @return an instance of a SOAP body element with the identified name
     * @throws SOAPException
     *             if there is an error
     */
    protected SOAPBodyElement newBodyElement(String name, SOAPMessage msg)
        throws SOAPException {

        SOAPBody body = getSOAPBody(msg);
        return nameBody(body, name);
    }

    /**
     * Creates and returns a SOAP element with the identified name. The returned
     * element is wrapped by the identified SOAP body message.
     * 
     * @param name
     *            the name for the element
     * @param sbe
     *            the SOAP body element to obtain an element for
     * @return an instance of a SOAP element with the identified name
     * @throws SOAPException
     *             if there is an error
     */
    protected SOAPElement newElement(String name, SOAPBodyElement sbe)
        throws SOAPException {

        Name n = soapFactory.createName(name);
        return sbe.addChildElement(n);
    }

    /**
     * Sends the given message to {@link #url} and returns the response from the
     * transaction. This implementation is blocking until the response is
     * returned.
     * <P>
     * This implementation requires that {@link #url} is already set prior.
     * 
     * @param msg the SOAP message to be sent
     * @return the response from the connection
     * @throws Exception if there is an error
     */
    protected SOAPMessage sendMessage(SOAPMessage msg) throws Exception {
        try {
            SOAPConnection conn = soapConnectionFactory.createConnection();
            return conn.call(msg, url);
        } catch (Exception e) {
            if (SWGConstants.DEV_DEBUG)
                SWGAide.printError("SOAPManager:sendMessage", e);
            throw e;
        }
    }

    /**
     * Sets the target name space prefix, e.g. "tns:".
     * 
     * @param nameSpacePrefix
     *            the target name space acronym
     */
    protected void setNameSpacePrefix(String nameSpacePrefix) {
        this.nameSpacePrefix = nameSpacePrefix;
    }

    /**
     * Sets the target name space, e.g. "urn:swgcraft".
     * 
     * @param targetNameSpace
     *            the target name space
     */
    protected void setTargetNameSpace(String targetNameSpace) {
        this.targetNameSpace = targetNameSpace;
    }

    /**
     * Sets the URL for the SOAP service provider location.
     * 
     * @param url
     *            the URL for the SOAP service provider location
     */
    protected void setURL(URL url) {
        this.url = url;
    }

    /**
     * Returns the first child of {@code node}'s first child, at {@code level}'s
     * depth. For level 0 this implementation returns the identified node
     * itself, at level 1 its first child, etc. If there is no such node at the
     * specified level {@code null} is returned. If the specified level is
     * greater than the tree's depth a {@link NullPointerException} is thrown.
     * 
     * @param node
     *            the node to obtain a child from
     * @param level
     *            number of levels down the tree to get the wanted node
     * @return the first child node found for a node at the specified level
     * @throws IllegalArgumentException
     *             if the specified level is negative
     * @throws NullPointerException
     *             if the argument is {@code null} or if level is greater than
     *             the tree's depth
     */
    protected static Node getFirstChild(Node node, int level) {
        if (level < 0)
            throw new IllegalArgumentException("Illegal level: " + level);
        Node n = node;
        for (int i = 0; i < level; ++i) {
            n = n.getFirstChild();
        }
        return n;
    }

    /**
     * Returns an empty SOAP body obtained from the argument.
     * 
     * @param msg
     *            the SOAP message to fill with a SOAP body
     * @return the new, empty SOAP body
     * @throws SOAPException
     *             if there is an error obtaining the SOAP envelope or the SOAP
     *             body
     */
    protected static SOAPBody getSOAPBody(SOAPMessage msg) throws SOAPException {
        SOAPPart soapPart = msg.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPBody body = envelope.getBody();

        return body;
    }

    /**
     * For testing purposes only
     * 
     * @param args
     *            possible args
     * @throws Exception
     *             an error
     */
    public static void main(String... args) throws Exception {
        SOAPManager sm = new SOAPManager();
        sm.setURL(new URL("http://www.swgcraft.co.uk/dev/soap/server.php"));
        sm.setTargetNameSpace("urn:swgcraft");
        sm.setNameSpacePrefix("tns");

        // create an empty message
        SOAPMessage msg = newSoapMessage();

        // create a SOAP body for the message and add an action
        SOAPBody body = getSOAPBody(msg);
        SOAPBodyElement sbe = sm.nameBody(body, "GetDetailedUserInfo");

        Name name = sm.getSoapFactory().createName("Login");
        SOAPElement se = sbe.addChildElement(name);

        Name n1 = sm.getSoapFactory().createName("userid");
        SOAPElement se1 = sbe.addChildElement(n1);
        se1.addTextNode("");
        se.addChildElement(se1);

        Name n2 = sm.getSoapFactory().createName("md5pass");
        SOAPElement se2 = sbe.addChildElement(n2);
        se2.addTextNode(ZCrypto.md5sum(""));
        se.addChildElement(se2);

        simpleTransform(msg, System.out);

        // Send the message
        SOAPMessage response = sm.sendMessage(msg);

        simpleTransform(response, System.out);

        Node n = getFirstChild(response.getSOAPBody(), 2);

        NodeList lst = n.getChildNodes();
        for (int i = 0; i < lst.getLength(); ++i) {
            Node nn = lst.item(i);
            System.out.println(nn + " " + nn.getTextContent());
        }
    }

    /**
     * Returns an empty SOAP message.
     * 
     * @return the new, empty SOAP message
     * @throws SOAPException
     *             anything went wrong initiating this SOAP message
     */
    protected static SOAPMessage newSoapMessage() throws SOAPException {
        return MessageFactory.newInstance().createMessage();
    }

    /**
     * Helper method that transforms {@code response} and writes the result to
     * the specified output stream. This method is useful for testing and
     * debugging.
     * 
     * @param response
     *            the response from a connection to be transformed and written
     * @param stream
     *            the stream to write to
     * @throws Exception
     *             if there is an error
     */
    protected static void simpleTransform(SOAPMessage response,
        PrintStream stream) throws Exception {

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        Source content = response.getSOAPPart().getContent();

        StreamResult result = new StreamResult(stream);
        transformer.transform(content, result);
        stream.println();
        // do not close the stream as we have no control of how it will be used
        // once we leave this scope
    }
}
