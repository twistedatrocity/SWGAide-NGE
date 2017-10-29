package swg.swgcraft;

import org.w3c.dom.Node;

/**
 * This class is the abstract super type for all types of SOAP responses from
 * interactions with SWGCraft's SOAP server.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public abstract class SWGSoapResponse {

    /**
     * The complete fault message, otherwise {@code null}.
     */
    protected String faultMessage;

    /**
     * The short fault message, otherwise {@code null}.
     */
    protected String faultMessageShort;

    /**
     * The integer response status from the SOAP server. The default value is
     * -1. If a sub-type does not utilize this field the default value is used.
     */
    protected int status = -1;

    /**
     * Returns the message contained in a fault error from the SOAP server,
     * otherwise {@code null}. If an exception occurs and no SOAP fault message
     * is parsed from the response, this implementation adds from the
     * exception's message to this message.
     * <P>
     * <B>NOTE: </B>The fault message can also be a standard warning message,
     * developers should use the methods {@code getStatus()} and {@code
     * getStatusString()} in those sub-types that provide them.
     * 
     * @return the fault message, or {@code null}
     */
    public final String getFaultMessage() {
        return faultMessage;
    }

    /**
     * Returns the short fault string, or {@code null}.
     * 
     * @return the short fault message, or {@code null}
     */
    public final String getFaultMessageShort() {
        return faultMessageShort;
    }

    /**
     * Returns the integer response status from the SOAP server. If it is not
     * possible to parse a proper response or if the implementing sub-type does
     * not implement this method, -1 is returned.
     * 
     * @return the response status from the SOAP server, or -1
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the response status from server as text. If it is not possible to
     * parse a proper response or if the implementing sub-type does not
     * implement this method, the empty string is returned.
     * 
     * @return the response status from server as text, or the empty string
     */
    public String getStatusString() {
        return "";
    }

    /**
     * Returns {@code true} if status equals "success", {@code false} otherwise.
     * 
     * @return {@code true} if status equals "success", {@code false} otherwise
     */
    public boolean isFaultless() {
        return (faultMessage == null);
    }

    /**
     * Parses and adds to this object's info from the argument.
     * 
     * @param node
     *            the node to parse
     */
    abstract void parseNode(Node node);

}
