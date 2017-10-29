package swg.swgcraft;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.tools.ZString;

/**
 * This class represents the response for the SOAP action swgcraft#CryptKeyIV
 * and its content is a key and an init vector for encryption with AES. It is
 * defined by swgcraft:CryptKeyIVResponse
 * 
 * <pre>{@code <xsd:complexType name="CryptKeyIVOutput">
 *     <xsd:all>
 *         <xsd:element name="Key" type="xsd:string"/>
 *         <xsd:element name="IV" type="xsd:string"/>
 *     </xsd:all>
 * </xsd:complexType>}</pre>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
class SWGSoapCryptKeyResponse extends SWGSoapResponse {

    /**
     * The initialization vector for AES.
     */
    private String iv;

    /**
     * The encryption key to use for encryption.
     */
    private String key;

    /**
     * Returns the initialization vector for encryption with AES.
     * 
     * @return the init vector
     */
    String getIV() {
        return iv;
    }

    /**
     * Returns the encryption key.
     * 
     * @return the encryption key
     */
    String getKey() {
        return key;
    }

    /**
     * Parses the content of a CryptKeyIV request
     * 
     * @param node
     *            the node from the response to parse
     */
    @Override
    void parseNode(Node node) {
        NodeList lst = node.getChildNodes();
        key = SWGSoapManager.parseString(lst, "Key");
        iv = SWGSoapManager.parseString(lst, "IV");
    }

    @Override
    public String toString() {
        ZString z = new ZString("CryptKeyResponse[");
        z.app("key:").app(key).app(", iv:").app(iv).app(']');
        return z.toString();
    }
}
