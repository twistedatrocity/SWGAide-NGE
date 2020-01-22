package swg.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This type provides commonly used XML methods.
 * 
 * @author Oarun @ Chilastra
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */

public final class ZXml {

    /**
     * Returns a boolean for the specified attribute from the element.
     * 
     * @param el the XML element that contains the attribute
     * @param attr the attribute to return a value for
     * @return a boolean
     * @throws IllegalArgumentException if the attribute does not read a boolean
     */
    public static boolean booleanFromAttr(Element el, String attr) {
        String r = trimmedStringFromAttribute(el, attr);
        if (r.isEmpty() || !(r.equals("yes") || r.equals("no")))
            //throw new IllegalArgumentException("No boolean: " + attr + "=" + r);
        	// FFS just return false;
        	return r.equals("no");

        return r.equals("yes");
    }

    /**
     * Returns a boolean from the specified XML child element.
     * 
     * @param xml the XML fragment to read from
     * @param child a child element
     * @return a boolean
     * @throws IllegalArgumentException if no such child exists or if it is not
     *         read "yes" or "no"
     */
    public static boolean booleanFromElem(Element xml, String child) {
        Element se = (Element) xml.getElementsByTagName(child).item(0);
        if (se == null)
            throw new IllegalArgumentException("No child: " + child);

        String value = se.getTextContent();
        if (!(value.equals("yes") || value.equals("no")))
            throw new IllegalArgumentException(
                    "No boolean: " + child + '=' + value);

        return value.equals("yes");
    }

    /**
     * Returns a list of comments from the specified XML child element, or an
     * empty list if no comments are parsed.
     * 
     * @param elem the XML fragment to read from
     * @return a list of comments, or an empty list
     */
    public static List<String> commentsFromElem(Element elem) {
        List<String> ret = new ArrayList<String>();
        NodeList nl = elem.getChildNodes();
        for (int i = 0; i < nl.getLength(); ++i) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.COMMENT_NODE)
                ret.add(n.getTextContent().trim());
        }
        return ret;
    }

    /**
     * Returns an element with the specified tag name from the specified
     * element, or {@code null}. If several elements with the specified tag name
     * exists the first element in document order is returned. If no such tag
     * name exists this method returns {@code null}.
     * 
     * @param elem an XML element
     * @param tagName a tag name
     * @return an element with the specified tag name, or {@code null}
     */
    public static Element elementByTag(Element elem, String tagName) {
        NodeList lst = elem.getElementsByTagName(tagName);
        return (Element) (lst.getLength() > 0
                ? lst.item(0)
                : null);
    }

    /**
     * Returns the number of elements of type {@link Node#ELEMENT_NODE} in the
     * specified document.
     * 
     * @param doc a document
     * @return the number of elements of type {@link Node#ELEMENT_NODE}
     */
    public static int elements(Document doc) {
        return elements(doc.getChildNodes().item(0));
    }

    /**
     * Returns the number of elements of type {@link Node#ELEMENT_NODE} in the
     * specified node.
     * 
     * @param node a node
     * @return the number of elements of type {@link Node#ELEMENT_NODE}
     */
    public static int elements(Node node) {
        NodeList list = node.getChildNodes();
        int count = 0;
        for (int i = 0; i < list.getLength(); i++)
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE) ++count;

        return count;
    }

    /**
     * Returns an integer for the specified attribute from the element, or 0 if
     * nothing was parsed.
     * 
     * @param elem an element
     * @param attr an attribute
     * @return an integer, or 0
     */
    public static int intFromAttr(Element elem, String attr) {
        return (int) longFromAttr(elem, attr);
    }

    /**
     * Returns an integer from the specified XML child element, or 0 if nothing
     * was parsed.
     * 
     * @param elem the XML fragment to read from
     * @param child a child element
     * @return an integer, or 0
     */
    public static int intFromElem(Element elem, String child) {
        return (int) longFromElem(elem, child);
    }

    /**
     * Returns a long for the specified attribute from the element, or 0 if
     * nothing was parsed.
     * 
     * @param elem an element
     * @param attr an attribute
     * @return a long, or 0
     */
    public static long longFromAttr(Element elem, String attr) {
        String ret = trimmedStringFromAttribute(elem, attr);
        return ZNumber.longVal(ret);
    }

    /**
     * Returns a long from the specified XML child element, or 0 if nothing was
     * parsed.
     * 
     * @param elem the XML fragment to read from
     * @param child a child element
     * @return a long, or 0
     */
    public static long longFromElem(Element elem, String child) {
        Element se = (Element) elem.getElementsByTagName(child).item(0);
        return se == null
                ? 0
                : ZNumber.longVal(se.getTextContent());
    }

    /**
     * Returns a DOM document parsed from the specified file object that is
     * supposed to be an XML file.
     * 
     * @param file an abstract file path for an XML document
     * @return a DOM document
     * @throws ParserConfigurationException if there is an error
     * @throws IOException if there is an I/O error
     * @throws SAXException if there is a parse error
     */
    public static Document parse(File file)
            throws ParserConfigurationException, SAXException, IOException {

        Document doc = null;
        DocumentBuilder builder;
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        doc = builder.parse(file);
        doc.normalize();
        return doc;
    }

    /**
     * Returns a trimmed string for the specified attribute from the element, or
     * {@code null} if nothing was parsed.
     * 
     * @param elem an element
     * @param attr an attribute
     * @return a string, or {@code null}
     */
    public static String stringFromAttr(Element elem, String attr) {
        String ret = trimmedStringFromAttribute(elem, attr);
        return ret.isEmpty()
                ? null
                : ret;
    }

    /**
     * Returns a string from the specified XML child element, or {@code null} if
     * nothing was parsed.
     * 
     * @param elem the XML fragment to read from
     * @param child a child element
     * @return a string, or {@code null}
     */
    public static String stringFromElem(Element elem, String child) {
        Element se = (Element) elem.getElementsByTagName(child).item(0);
        if (se == null) return null;

        return se.getTextContent();
    }

    /**
     * Helper method which returns a trimmed string for the specified attribute
     * from the XML element, and empty string if nothing was parsed or if the
     * content was just white space.
     * 
     * @param xml the XML element containing the attribute
     * @param attr the attribute to return a value for
     * @return a trimmed string, may be empty
     */
    private static String trimmedStringFromAttribute(Element xml, String attr) {
        String ret = xml.getAttribute(attr).trim();
        return ret;
    }
}
