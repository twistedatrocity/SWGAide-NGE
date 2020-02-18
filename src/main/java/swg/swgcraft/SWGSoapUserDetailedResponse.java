package swg.swgcraft;

import java.util.Date;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.tools.ZString;

/**
 * This class represents the response for the SOAP action
 * swgcraft#GetDetailedUserInfo. Only a user that can provide user name and
 * password for SWGCraft.org can utilize that SOAP action. The response is
 * defined by swgcraft:UserInfo
 * 
 * <pre>{@code <xsd:complexType name="UserInfo">
 *     <xsd:all>
 *         <xsd:element name="Username" type="xsd:string"/>
 *         <xsd:element name="Server" type="xsd:integer"/>
 *         <xsd:element name="Planet" type="xsd:integer"/>
 *         <xsd:element name="Posts" type="xsd:integer"/>
 *         <xsd:element name="RegStamp" type="xsd:integer"/>
 *         <xsd:element name="LastStamp" type="xsd:integer"/>
 *         <xsd:element name="Timezone" type="xsd:integer"/>
 *         <xsd:element name="Dateformat" type="xsd:string"/>
 *         <xsd:element name="Email" type="xsd:string"/>
 *     </xsd:all>
 * </xsd:complexType>}</pre>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
final class SWGSoapUserDetailedResponse extends SWGSoapUserResponse {

    /**
     * The particular date format this user has chosen.
     */
    private String dateFormat;

    /**
     * The user's email address.
     */
    private String email;

    /**
     * The most recent time when the user visited SWGCraft.org, counted in
     * seconds since January 1, 1970, UTC.
     */
    private long lastStamp;

    /**
     * The time zone the user has selected.
     */
    private String timezone;

    /**
     * Returns the date format the user has chosen at SWGCraft.org.
     * 
     * @return the date format the user has chosen
     */
    String getDateFormat() {
        return dateFormat;
    }

    /**
     * Returns the user's email address.
     * 
     * @return the user's email address
     */
    String getEmail() {
        return email;
    }

    /**
     * Returns the date for when the user's last visited SWGCraft.org.
     * 
     * @return the date for when the user's last visited SWGCraft.org
     */
    Date getLastStamp() {
        return new Date(lastStamp * 1000L);
    }

    /**
     * Returns the time zone the user has selected.
     * 
     * @return the time zone
     */
    String getTimezone() {
        return timezone;
    }

    @Override
    void parseNode(Node node) {
        NodeList lst = node.getChildNodes();
        userName = SWGSoapManager.parseString(lst, "Username");
        dateFormat = SWGSoapManager.parseString(lst, "Dateformat");
        email = SWGSoapManager.parseString(lst, "Email");
        lastStamp = SWGSoapManager.parseLong(lst, "LastStamp");
        timezone = SWGSoapManager.parseString(lst, "Timezone");

        // order of this call does not matter but userName should be set first
        super.parseNode(node);
    }

    /**
     * Sets the SWGCraft user ID for this instance.
     * 
     * @param id
     *            the SWGCraft user ID to set
     */
    void setUserID(int id) {
        // This should have been better done in a constructor, but see comment
        // in body of SWGSoapManager#requestUserInfoDetailed
        userID = id;
    }

    @Override
    protected void toStringHelper(ZString z) {
        z.app(", lastvisit \"").app(getLastStamp()).app("\"");
        z.app(", dateformat:").app(dateFormat);
        z.app(", timeZone:").app(timezone);
        z.app(", email:").app(email);
    }
}
