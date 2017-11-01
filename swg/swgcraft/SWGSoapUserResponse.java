package swg.swgcraft;

import java.util.Date;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.tools.ZString;

/**
 * This type is the super type for responses regarding registered users at
 * SWGCraft.org. In itself this type represents a merged response for two SOAP
 * actions implemented as two two different methods in {@link SWGSoapManager}.
 * <P>
 * SOAP actions at SWGCraft related to users are based on integer user ID's.
 * Only the SOAP action swgcraft#GetUserInfo obtains info from name. Thus, to
 * obtain any responses requiring user identity, the client must first request
 * swgcraft#SimpleUserInfo which contains the user ID. This implementation
 * stores the user name in SWGAide's preference keeper, but not the ID. If both
 * should be stored they still must be verified for mutual validity. The user
 * name is chosen since it is presented at {@link SWGCraftOptionsPanel}.
 * <P>
 * The method {@link SWGSoapManager#requestUserInfo(String)} implements the SOAP
 * action swgcraft#GetUserInfo and calls constructor
 * {@link #SWGSoapUserResponse(String)}. The response for that SOAP action has
 * the following definition
 * <P>
 * 
 * <pre>{@code
 * <xsd:complexType name="SimpleUserInfo">
 *     <xsd:all>
 *         <xsd:element name="UserID" type="xsd:integer"/>
 *         <xsd:element name="Server" type="xsd:integer"/>
 *         <xsd:element name="Planet" type="xsd:integer"/>
 *         <xsd:element name="Posts" type="xsd:integer"/>
 *         <xsd:element name="RegStamp" type="xsd:integer"/>
 *     </xsd:all>
 * </xsd:complexType>}</pre>
 * The method {@link SWGSoapManager#requestUserName(int)} implements the SOAP
 * action swgcraft#GetUserInfoByID and calls constructor
 * {@link #SWGSoapUserResponse(int)}. The response for that SOAP action has the
 * following definition
 * <P>
 * 
 * <pre>{@code <xsd:complexType name="SimpleUserInfoByID">
 *     <xsd:all>
 *         <xsd:element name="Name" type="xsd:string"/>
 *         <xsd:element name="Server" type="xsd:integer"/>
 *         <xsd:element name="Planet" type="xsd:integer"/>
 *         <xsd:element name="Posts" type="xsd:integer"/>
 *         <xsd:element name="RegStamp" type="xsd:integer"/>
 *     </xsd:all>
 * </xsd:complexType>}</pre>
 * Thus, this type contains getters for as well name as user ID. Both will
 * return valid values since the <I>"missing"&nbsp;</I> argument is specified to
 * the appropriate constructor.
 * <P>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
class SWGSoapUserResponse extends SWGSoapResponse {

    /**
     * The galaxy constant denoting the galaxy which the user has selected at
     * SWGCraft.org as his/her "home galaxy". If the user has not selected a
     * galaxy the default value from SWGCraft is SWGCraft.co.uk.
     */
    private SWGCGalaxy galaxy;

    /**
     * The planet constant for the planet the user has selected at SWGCraft.org
     * as their "home planet". If the user has not selected a planet, {@code
     * null} indicates "All".
     */
    private SWGPlanet planet;

    /**
     * The number of posts at SWGCraft.org
     */
    private int posts;

    /**
     * The date when the user registered him/herself at SWGCraft.org, counted in
     * seconds since January 1, 1970, UTC.
     */
    private long regStamp;

    /**
     * The user's ID at SWGCraft.org. Default value is -1 which indicates that
     * the ID is currently unknown.
     */
    int userID = -1;

    /**
     * The user name at SWGCraft.org, default value is {@code null}.
     */
    String userName;

    /**
     * The default constructor for this type.
     */
    protected SWGSoapUserResponse() {
    // only SWGSoapUserDetailed should use this constructor
    }

    /**
     * Creates an instance of this type. This constructor is only used by
     * {@link SWGSoapManager#requestUserName(int)}.
     * 
     * @param userID
     *            he user's user-ID at SWGCraft.org
     */
    SWGSoapUserResponse(int userID) {
        if (userID <= 0)
            throw new IllegalArgumentException("Invalid ID: " + userID);
        this.userID = userID;
    }

    /**
     * Creates an instance of this type. This constructor is only used by
     * {@link SWGSoapManager#requestUserInfo(String)}.
     * 
     * @param userName
     *            the user's user-name at SWGCraft.org
     */
    SWGSoapUserResponse(String userName) {
        if (userName == null)
            throw new NullPointerException("Argument is null");
        this.userName = userName;
    }

    /**
     * Returns the name of the galaxy which the user has selected at
     * SWGCraft.org as his/her home galaxy. If the user has not selected a
     * galaxy "SWGCraft.co.uk" is returned.
     * 
     * @return the name of the user's main server
     */
    final SWGCGalaxy getGalaxy() {
        return galaxy;
    }

    /**
     * Returns the planet which the user has selected at SWGCraft.org as the
     * "home planet". If the user has not selected a planet, {@code null} is
     * returned which at SWGCraft.org indicates "All".
     * 
     * @return the the user's home planet, or {@code null}
     */
    final SWGPlanet getPlanet() {
        return planet;
    }

    /**
     * Returns the date when the user registered him/herself at SWGCraft.
     * 
     * @return the date when the user joined SWGCraft
     */
    final Date getRegStamp() {
        return new Date(regStamp * 1000L);
    }

    /**
     * Returns the user's ID at SWGCraft.org. If the ID is currently unknown -1
     * is is returned.
     * 
     * @return the user's ID at SWGCraft.org, or -1
     */
    final int getUserID() {
        return userID;
    }

    /**
     * Returns the user name which identifies the current user at SWGCraft.org.
     * If the name is not set {@code null} is returned.
     * 
     * @return the user name at SWGCraft.org, or {@code null}
     */
    final String getUserName() {
        return userName;
    }

    /*
     * (non-Javadoc)
     * @see swg.swgcraft.SWGSoapUserInfo#parseNode(org.w3c.dom.Node)
     */
    @Override
    void parseNode(Node node) {
        NodeList lst = node.getChildNodes();

        int id = SWGSoapManager.parseInt(lst, "Server");
        galaxy = id > 0
            ? SWGCGalaxy.fromID(id)
            : null;
        id = SWGSoapManager.parseInt(lst, "Planet");
        planet = id > 0
            ? SWGPlanet.fromID(id)
            : null;
        posts = SWGSoapManager.parseInt(lst, "Posts");
        regStamp = SWGSoapManager.parseLong(lst, "RegStamp");

        // this method serves both SWGSoapManager#requestUserInfo(String)
        // and SWGSoapManager#requestUserName(int) thus the following two
        // sections.
        // Either value is set by either one of the two the constructors
        // while the other once has its default value.

        if (userID <= 0)
            userID = SWGSoapManager.parseInt(lst, "UserID");

        if (userName == null)
            userName = SWGSoapManager.parseString(lst, "Name");
    }

    @Override
    public final String toString() {
        ZString z = new ZString();
        if (isFaultless()) {
            z.app(getClass().getSimpleName()).app('[');
            z.app("name:").app(userName);
            if (userID > 0) z.app(", userID:").app(userID);
            z.app(", ").app(galaxy.getName());
            z.app(", ").app(planet.getName());
            z.app(", registered \"").app(getRegStamp().toString());
            z.app("\"").app(", posts:").app(posts);
            toStringHelper(z);
        } else {
            z.app(faultMessage);
        }
        return z.app(']').toString();
    }

    /**
     * Helper method for {@link #toString()}. The sub-type
     * {@link SWGSoapUserDetailedResponse} appends its type specific data to the
     * specified string builder.
     * <P>
     * {@link #toString()} has filled in the runtime class name, name, userID,
     * galaxy name, planet name, register date, and number of posts. Then it
     * calls this helper method and after this method returns a trailing bracket
     * is added and {@link #toString()} returns the string.
     * 
     * @param z a modifiable string
     */
    void toStringHelper(ZString z) {/* pass */}
}
