package swg.swgcraft;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import swg.SWGAide;
import swg.SWGConstants;
import swg.crafting.Stat;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGMutableResource;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceMgr;
import swg.crafting.resources.SWGResourceStats;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.tools.SOAPManager;
import swg.tools.ZCrypto;
import swg.tools.ZNumber;
import swg.tools.ZString;

/**
 * This class together with {@link SWGCraft} are the only communication channels
 * with SWGCraft.org. This class manages SOAP connections with SWGCraft.org in
 * accordance with the public WSDL. None of the features of this implementation
 * has public access modifiers.
 * <P>
 * <B>NOTE: </B>See {@link SWGCraft} for further details on how communication
 * with SWGCraft.org is structured and how it must be managed.
 * <P>
 * In short though, classes in this package must not provide public access to
 * any of their features. All requests for service must be handled by the
 * appropriate managers which, however, are located in this package.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
final class SWGSoapManager extends SOAPManager {

    /**
     * The constant name space prefix for the SOAP server at SWGCraft.org.
     */
    private static final String NAME_SPACE_PREFIX = "tns";

    /**
     * The SOAP handler for communicating with the SOAP server at SWGCraft.org.
     */
    private static SWGSoapManager soapHandler;

    /**
     * The constant target name space for the SOAP server at SWGCraft.org.
     */
    private static final String TARGET_NAME_SPACE = "urn:swgcraft";

    /**
     * The cached password to SWGCraft.org for the current user, this value is
     * in the hashed form which is sent over the Internet.
     * 
     * @see #resetUserData()
     */
    private String hashedUserPassword;

    /**
     * The cached user ID at SWGCraft.org for the current user.
     * 
     * @see #resetUserData()
     */
    private int userID = -1;

    /**
     * Creates a SOAP manager for communication with SWGCraft.org and sets the
     * name space and the target name space. This constructor does not set the
     * URL for the SOAP server. Thus, before each call to the SOAP server the
     * request/send-methods call {@link #setServerURL()} in the case the user
     * has edited the URL at {@link SWGCraftOptionsPanel}.
     * 
     * @see #setServerURL()
     * @throws Exception if there is an error creating the {@link SOAPManager}
     */
    SWGSoapManager() throws Exception {
        super();
        setTargetNameSpace(TARGET_NAME_SPACE);
        setNameSpacePrefix(NAME_SPACE_PREFIX);
    }

    /**
     * Helper method which creates a SOAP element containing the the stats of
     * the identified resource. The new element is added as a child of the
     * specified SOAP element which is contained in the body element.
     * <P>
     * If the argument {@code allValues} is {@code true} all stat values of the
     * resource are contained in the SOAP element, also zero-values. Otherwise,
     * if {@code allValues} is {@code false}, only values greater than zero are
     * added to the element and zero-values are ignored.
     * 
     * @param res the resource from which to obtain the stats
     * @param se the SOAP element to add a child element with stats to
     * @param sbe the SOAP body element
     * @param allValues {@code true} if all stats should be added to the SOAP
     *        element, {@code false} if only values greater than zero should be
     *        added
     * @throws SOAPException if there is an error
     */
    private void addStats(SWGKnownResource res, SOAPElement se,
            SOAPBodyElement sbe, boolean allValues) throws SOAPException {

        // TODO: examine Sobuno's comment.
        // editResource cannot take 0 stats in todays version, only > 0
        // However, Sobuno@SWGCraft says that this would work:
        // "SoapEditResource seems to work fine for me with 0's (<fl
        // xsi:type="xsd:integer">0</fl>) or nulls (<fl xsi:nil="true"/>)"

        // create and add the new child to the element contained in the body
        SOAPElement st = newElement("Stats", sbe);
        se.addChildElement(st);

        SWGResourceStats rs = res.stats();
        for (Stat s : Stat.values()) {
            int v = rs.value(s);
            if (allValues || v > 0) {
                addChildElement(s.getName(), validateInt(v), sbe, st);
            }
        }
    }

    /**
     * Helper method that returns the encrypted form of the current user's
     * password for SWGCraft.org. The encryption is performed locally, using an
     * encryption key and initialization vector obtained from SWGCraft.org. If
     * the response is a SOAP fault message {@code null} is returned.
     * <P>
     * This implementation caches the value until it is reset by
     * {@link #resetUserData()}.
     * 
     * @return the encrypted password for the current user, or {@code null}
     */
    private String getEncryptedPassword() {
        if (hashedUserPassword == null) {
            // request the encryption key and IV from SWGCraft.org
            // for use in encrypting the password using AES cryptography
            SWGSoapCryptKeyResponse e = requestEncryptionKey();
            if (!e.isFaultless())
                return null;

            hashedUserPassword = ZCrypto.encryptedAES(
                    SWGCraft.getUserPassword(), e.getKey(), e.getIV());
        }
        return hashedUserPassword;
    }

    /**
     * Helper method that returns the integer userID. The value is the user ID
     * at SWGCraft.org and it is obtained in the response from
     * {@link #requestUserInfo()}. If the user is not known at SWGCraft.org, -1
     * is returned.
     * <P>
     * This implementation caches the value until it is reset by
     * {@link #resetUserData()}.
     * 
     * @see #userID
     * @return the SWGCraft userID, or -1
     * @throws Exception if there is an error
     */
    private int getUserID() throws Exception {
        if (userID > 0)
            return userID;

        SWGSoapUserResponse sui = requestUserInfo();
        if (sui == null)
            userID = -1;
        else
            userID = sui.getUserID();

        return userID;
    }

    /**
     * Helper method that determines if the specified SOAP message contains a
     * fault message from the server. If there is a fault message this
     * implementation updates the response with the complete fault message as
     * well as its short fault message.
     * 
     * @see SWGSoapResponse#faultMessage
     * @see SWGSoapResponse#faultMessageShort
     * @param message the SOAP message which is the response from the server
     * @param response the instance which is populated with the response data
     * @return {@code true} if there is a fault message, {@code false} otherwise
     * @throws SOAPException if there is an error
     */
    private boolean hasFault(SOAPMessage message, SWGSoapResponse response)
            throws SOAPException {

        if (message.getSOAPBody().hasFault()) {
            SOAPFault fault = message.getSOAPBody().getFault();
            Name code = fault.getFaultCodeAsName();
            String faultStr = fault.getFaultString();
            String actor = fault.getFaultActor();

            ZString z = new ZString("Fault: ");
            z.app("Code: ").app(code.getQualifiedName());
            z.app("; Reason: ").app(faultStr);
            if (actor != null) z.app("; Actor: ").app(actor);

            response.faultMessage = z.toString();
            response.faultMessageShort = faultStr;
            return true;
        }
        return false;
    }

    /**
     * Helper method that creates and returns a SOAP element containing the
     * details to login the the current user to SWGCraft.org. The new element is
     * added as a child to the identified body. However, the returned element
     * can be added as a child to any other SOAP element which is contained by
     * the body.
     * 
     * @param sbe the SOAP body for the new element
     * @return a SOAP element which contains the login details
     * @throws Exception if there is an error
     */
    private SOAPElement newLoginChild(SOAPBodyElement sbe) throws Exception {
        SOAPElement lse = newElement("Login", sbe);
        addChildElement("userid", itos(getUserID()), sbe, lse);
        addChildElement("md5pass", getEncryptedPassword(), sbe, lse);
        return lse;
    }

    /**
     * Helper method that creates and returns a SOAP element containing the
     * program info (Name and Version) for SWGAide. The new element is added as
     * a child to the identified body. However, the returned element can be
     * added as a child to any other SOAP element which is contained by the
     * body.
     * 
     * @param sbe the SOAP body for the new element
     * @return a SOAP element which contains the program info
     * @throws Exception if there is an error
     */
    private SOAPElement newProgramInfoChild(SOAPBodyElement sbe)
            throws Exception {
        SOAPElement lse = newElement("ProgramInfo", sbe);
        addChildElement("Name", "SWGAide", sbe, lse);
        addChildElement("Version", SWGConstants.version, sbe, lse);
        return lse;
    }

    /**
     * Helper method that requests from SWGCraft.org a new key and
     * initialization vector for the AES encryption algorithm. If there is an
     * error it is intercepted and a message is written to SWGAide's error log,
     * as well as added as a fault message in the returned object. If there is
     * an error it is intercepted and a message is added as a fault message to
     * the response.
     * 
     * @see ZCrypto#encryptedAES(String, String, String)
     * @return a new key and initialization vector for AES encryption
     */
    private SWGSoapCryptKeyResponse requestEncryptionKey() {

        SWGSoapCryptKeyResponse response = new SWGSoapCryptKeyResponse();

        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("CryptKeyIV", msg);

            newProgramInfoChild(sbe);

            SOAPMessage respMsg = sendMessage(msg);
            if (hasFault(respMsg, response))
                return response;

            // simpleTransform(response, System.out);

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);
            response.parseNode(n);
        } catch (Exception e) {
            String msg =
                    "SWGSoapManager:requestEncryptionKey: " + e.getMessage();
            SWGAide.printDebug("soap", 1, msg);
            response.faultMessage = msg;
            response.faultMessageShort = e.getMessage();
        }
        return response;
    }

    /**
     * Requests from SWGCraft.org and returns a response which contains a
     * {@link SWGMutableResource} for the specified resource ID. If there is an
     * error it is intercepted, the contained resource is {@code null}, a
     * message is written to SWGAide's log file, and a message is added as a
     * fault message to the response.
     * 
     * @param swgcraftID the unique SWGCraft resource ID
     * @return a response containing a {@link SWGMutableResource}, or {@code
     *         null} and a fault message
     * @throws IllegalArgumentException if the ID is invalid
     */
    synchronized SWGSoapResourceResponse requestResource(long swgcraftID) {

        SWGSoapResourceResponse response =
                new SWGSoapResourceResponse(swgcraftID);
        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("GetResourceInfoFromID", msg);

            addChildElement("id", itos(swgcraftID), sbe, null);

            newProgramInfoChild(sbe);

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response))
                return response;

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);
            response.parseNode(n);
        } catch (Exception e) {
            String s = "SWGSoapManager:requestResource(id): " + e.getMessage();
            SWGAide.printDebug("soap", 1, s);
            response.faultMessage = e.toString();
            response.faultMessageShort = "Error";
        }
        return response;
    }

    /**
     * Requests from SWGCraft.org and returns a response which contains a
     * {@link SWGMutableResource} for the specified galaxy and the resource
     * name. If there is an error it is intercepted, the contained resource is
     * {@code null}, a message is written to SWGAide's log file, and a message
     * is added as a fault message to the response.
     * 
     * @param resourceName the proper name for the resource
     * @param galaxy a galaxy constant
     * @return a response containing a {@link SWGMutableResource}, or {@code
     *         null} and a fault message
     * @throws NullPointerException if an argument is {@code null}
     */
    synchronized SWGSoapResourceResponse requestResource(String resourceName,
            SWGCGalaxy galaxy) {

        SWGSoapResourceResponse response =
                new SWGSoapResourceResponse(resourceName, galaxy);
        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("GetResourceInfo", msg);

            // populate the body element
            SOAPElement ise = newElement("Input", sbe);

            addChildElement("name", resourceName, sbe, ise);
            addChildElement("server", itos(galaxy.id()), sbe, ise);

                newProgramInfoChild(sbe);

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response))
                return response;

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);
            response.parseNode(n);
        } catch (Exception e) {
            String s = "SWGSoapManager:requestResource(n,g): " + e.getMessage();
            SWGAide.printDebug("soap", 1, s);
            response.faultMessage = e.getMessage();
            response.faultMessageShort = "Error";
        }
        return response;
    }

    /**
     * Requests from SWGCraft.org and returns a list of resources for the
     * identified galaxy. The response contains a list with none, one, or
     * several elements of type {@link SWGResourceTuple}. Each tuple represents
     * a resource which has spawned at the identified galaxy, a resource which
     * name begins with the specified first letters. If there is an error a
     * message is added as a fault message to the response.
     * <P>
     * Even if the complete name for a resource is provided this implementation
     * may return a response with a list which size is greater than one. Then
     * several resources have names that are longer than the identified name and
     * the beginning of these names all equal the identified name.
     * 
     * @param firstLetters the first letters of a resource name, or its full
     *        name
     * @param galaxy a galaxy constant
     * @return a response with a list of resource tuples
     * @throws NullPointerException if an argument is {@code null}
     */
    synchronized SWGSoapListResResponse requestResources(String firstLetters,
            SWGCGalaxy galaxy) {

        SWGSoapListResResponse response = new SWGSoapListResResponse(galaxy);

        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("FindResources", msg);

            // populate the body element
            SOAPElement ise = newElement("Input", sbe);

            addChildElement("name", firstLetters, sbe, ise);
            addChildElement("server", itos(galaxy.id()), sbe, ise);

            newProgramInfoChild(sbe);

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response))
                return response;

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);
            response.parseNode(n);
        } catch (Exception e) {
            String msg = "SWGSoapManager:findResources: " + e.getMessage();
            SWGAide.printDebug("soap", 1, msg);
            response.faultMessage = msg;
            response.faultMessageShort = "Error";
        }
        return response;
    }

    /**
     * Requests from SWGCraft.org and returns basic user info for the current
     * user. If the current user is not known at SWGCraft.org {@code null} is
     * returned. If there is an error it is intercepted, information is written
     * to SWGAide's log file, and this method throws the exception as is.
     * 
     * @return basic user info for the current user, or {@code null}
     * @throws Exception if there is an error
     */
    synchronized SWGSoapUserResponse requestUserInfo() throws Exception {
        return requestUserInfo(SWGCraft.getUserName());
    }

    /**
     * Requests from SWGCraft.org and returns basic user info for the specified
     * user name. If the user is not known at SWGCraft.org {@code null} is
     * returned. If there is an error it is intercepted, information is written
     * to SWGAide's log file, and this method throws the exception as is.
     * 
     * @param userName the user name to request info for
     * @return basic user info, or {@code null}
     * @throws Exception if there is an error
     */
    synchronized SWGSoapUserResponse requestUserInfo(String userName)
            throws Exception {

        SWGSoapUserResponse response = new SWGSoapUserResponse(userName);

        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("GetUserInfo", msg);

            // populate body element
            addChildElement("Input", userName, sbe, null);

            newProgramInfoChild(sbe);

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response)) {
                SWGAide.printDebug("soap", 1,
                        "SWGSoapManager:requestUserInfo: name:" + userName
                                + ' '
                                + response.getFaultMessageShort());
                return null;
            }

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);
            response.parseNode(n);

            return response;
        } catch (Exception e) {
            SWGAide.printDebug("soap", 1,
                    "SWGSoapManager:requestUserInfo: name:" + userName + ' '
                            + e);
            throw e;
        }
    }

    /**
     * Requests from SWGCraft.org and returns detailed user info for the current
     * user. This request requires that the user provides a valid user name and
     * password for SWGCraft.org at {@link SWGCraftOptionsPanel}. If the user
     * name and/or password are invalid an error message is added as a fault
     * message to the response. If there is an error it is intercepted, an error
     * message is written to SWGAide's log file, and a more friendly message is
     * added as a fault message to the response.
     * 
     * @return detailed user info for the current user
     */
    synchronized SWGSoapUserDetailedResponse requestUserInfoDetailed() {

        SWGSoapUserDetailedResponse response =
                new SWGSoapUserDetailedResponse();

        try {

            // setting the user-ID should be in the constructor, but...
            // A) obtaining the ID must be in the try/catch
            // B) the response must be created outside the try/catch block so
            // it is possible to add a fault message within the catch block

            int uid = getUserID();
            response.setUserID(uid);

            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("GetDetailedUserInfo", msg);

            // add login to the body
            newLoginChild(sbe);

            newProgramInfoChild(sbe);

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response)) {
                if (response.faultMessage.contains("password"))
                    resetUserData();

                SWGAide.printDebug("soap", 1, response.toString());
                return response; // user name and/or password invalid
            }

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);
            response.parseNode(n);
        } catch (SOAPException e) {
            SWGAide.printDebug("soap", 1,
                    "SWGSoapManager:requestUserInfoDetailed_a: "
                            + e.getMessage());
            response.faultMessage = "Connection error!\nIs SWGCraft.org down?";
            response.faultMessageShort = "Internet error";
        } catch (Exception e) {
            String msg =
                    "SWGSoapManager:requestUserInfoDetailed_b: "
                            + e.getMessage();
            SWGAide.printDebug("soap", 1, msg);
            response.faultMessage = msg;
            response.faultMessageShort = "Error";
        }
        return response;
    }

    /**
     * Requests from SWGCraft.org and returns basic user info for the specified
     * user ID. If the ID is not known at SWGCraft.org {@code null} is returned.
     * If there is an error it is intercepted, information is written to
     * SWGAide's log file, and this method throws the exception as is.
     * 
     * @param swgcraftID the user ID for SWGCraft.org
     * @return basic user info, or {@code null}
     * @throws IllegalArgumentException if the user ID is invalid
     */
    synchronized SWGSoapUserResponse requestUserName(int swgcraftID) {

        if (swgcraftID <= 0)
            throw new IllegalArgumentException("Invalid ID: " + swgcraftID);

        SWGSoapUserResponse response = new SWGSoapUserResponse(swgcraftID);

        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("GetUserInfoByID", msg);

            addChildElement("GetUserInfoByID", itos(swgcraftID), sbe, null);

            newProgramInfoChild(sbe);

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response)) {
                SWGAide.printDebug("soap", 1, "id=" + swgcraftID + ' '
                        + response.toString());
                return null;
            }

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);
            response.parseNode(n);
            return response;
        } catch (Exception e) {
            SWGAide.printDebug("soap", 1, "SWGSoapManager:requestUserName: "
                    + e);
            return null;
        }
    }

    /**
     * Resets the cached hashed user password and the cached user ID. This is
     * usually the result from a failed identification with SWGCraft.org.
     * Resetting the values will force a future transaction to implicitly
     * refresh {@link #hashedUserPassword} and {@link #userID}.
     */
    synchronized void resetUserData() {
        hashedUserPassword = null;
        userID = -1;
    }

    /**
     * Sends an "addAvailability" message to SWGCraft.org which marks the
     * identified resource as available at {@code planet}. The status codes of
     * the response reads the outcome. If there is an error it is intercepted
     * and a message is added as a fault message to the response.
     * <P>
     * NOTE: Only {@link SWGResourceMgr} is allowed to call this method via
     * {@link SWGResourceManager}. See {@link SWGResource} for a detailed
     * rationale.
     * 
     * @param res the resource to mark as available at planet
     * @param planet the planet for which to mark the resource available
     * @return the response from the server
     * @throws NullPointerException if an argument is {@code null}
     */
    synchronized SWGSoapAvailResResponse sendAvailable(SWGKnownResource res,
            SWGPlanet planet) {

        if (planet == null)
            throw new NullPointerException("Planet is null: " + res);

        SWGSoapAvailResResponse response =
                new SWGSoapAvailResResponse(res, planet);

        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("SoapAddAvailability", msg);

            // populate the body element
            SOAPElement avi = newElement("AddAvailabilityInput", sbe);

            addChildElement("ResourceID", itos(res.id()), sbe, avi);
            addChildElement("Server", itos(res.galaxy().id()), sbe, avi);
            addChildElement("Planet", itos(planet.id()), sbe, avi);

            avi.addChildElement(newLoginChild(sbe));
            avi.addChildElement(newProgramInfoChild(sbe));

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response))
                return response;

            Node n = getFirstChild(respMsg.getSOAPBody(), 1);
            response.parseNode(n);
            if (response.status == 999)
                resetUserData();

        } catch (Exception e) {
            String msg = "SWGSoapManager:sendAvailable: " + e.getMessage();
            SWGAide.printDebug("soap", 1, msg);
            response.faultMessage = msg;
            response.faultMessageShort = "Error";
        }
        return response;
    }

    /**
     * Sends a "deplete" message to SWGCraft.org which marks the identified
     * resource as depleted at its galaxy. The status codes of the response
     * reads the outcome. If there is an error it is intercepted and a message
     * is added as a fault message to the response.
     * <P>
     * NOTE: Only {@link SWGResourceMgr} is allowed to call this method via
     * {@link SWGResourceManager}. See {@link SWGResource} for a detailed
     * rationale.
     * 
     * @param swgcraftID the unique SWGCraft ID for the resource to mark as
     *        depleted
     * @return the response from the server
     * @throws IllegalArgumentException if the argument is invalid
     */
    synchronized SWGSoapStatusResponse sendDespawn(long swgcraftID) {

        if (swgcraftID <= 0)
            throw new IllegalArgumentException("Invalid ID: " + swgcraftID);

        SWGSoapStatusResponse response = new SWGSoapStatusResponse(swgcraftID);

        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("SoapDespawn", msg);

            // populate the body element
            SOAPElement di = newElement("DespawnInput", sbe);
            addChildElement("ResourceID", itos(swgcraftID), sbe, di);

            di.addChildElement(newLoginChild(sbe));
            di.addChildElement(newProgramInfoChild(sbe));

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response))
                return response;

            Node n = getFirstChild(respMsg.getSOAPBody(), 1);
            response.status = Integer.parseInt(n.getTextContent());
            if (response.status == 999) {
                resetUserData();
            }
        } catch (Exception e) {
            String msg = "SWGSoapManager:sendDespawn: " + e.getMessage();
            SWGAide.printDebug("soap", 1, msg);
            response.faultMessage = msg;
            response.faultMessageShort = e.getMessage();
        }
        return response;
    }

    /**
     * Sends an "editResource" message to SWGCraft.org which updates the values
     * for the identified resource. This implementation does not validate the
     * resource. Optionally a comment can be attached to the message. The status
     * codes of the response reads the outcome. If there is an error it is
     * intercepted and a message is added as a fault message to the response.
     * <P>
     * NOTE: Only {@link SWGResourceMgr} is allowed to call this method via
     * {@link SWGResourceManager}. See {@link SWGResource} for a detailed
     * rationale.
     * 
     * @param res the updated resource which augments the data at SWGCraft.org
     * @param comment an optional comment
     * @return the response from the server
     * @throws NullPointerException if an argument is {@code null}
     */
    synchronized SWGSoapEditResResponse sendEdit(SWGKnownResource res,
            String comment) {

        String cmt = comment == null
                ? ""
                : comment;

        SWGSoapEditResResponse response = new SWGSoapEditResResponse(res);

        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("SoapEditResource", msg);

            // populate the body element
            SOAPElement eri = newElement("EditResourceInput", sbe);

            addChildElement("ResourceID", itos(res.id()), sbe, eri);
            addChildElement("Name", res.getName(), sbe, eri);
            addChildElement("Server", itos(res.galaxy().id()), sbe, eri);
            addChildElement("Class", res.rc().rcName(), sbe, eri);

            // add only non-zero values as stats
            addStats(res, eri, sbe, false);

            addChildElement("Comment", cmt, sbe, eri);

            eri.addChildElement(newLoginChild(sbe));
            eri.addChildElement(newProgramInfoChild(sbe));

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response))
                return response;

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);
            response.parseNode(n);
            if (response.status == 999) {
                resetUserData();
            }
        } catch (Exception e) {
            String msg = "SWGSoapManager:addResource: " + e.getMessage();
            SWGAide.printDebug("soap", 1, msg);
            response.faultMessage = msg;
            response.faultMessageShort = e.getMessage();
        }
        return response;
    }

    /**
     * Sends an "addResource" message to SWGCraft.org which adds the identified
     * resource as new at {@code planet}. This implementation does not validate
     * the argument. If the resource is already reported its data is possibly
     * updated.
     * <P>
     * The status codes of the response reads the outcome. If there is an error
     * it is intercepted and a message is added as a fault message to the
     * response.
     * <P>
     * NOTE: Only {@link SWGResourceMgr} is allowed to call this method via
     * {@link SWGResourceManager}. See {@link SWGResource} for the detailed
     * rationale.
     * 
     * @param res the new resource to submit
     * @param planet the planet to add the new resource to
     * @return the response from the server
     * @throws NullPointerException if an argument is {@code null}
     */
    synchronized SWGSoapNOResResponse sendNew(SWGKnownResource res,
            SWGPlanet planet) {

        if (planet == null)
            throw new NullPointerException("Planet is null: " + res);

        return sendNewOld(res, planet, false);
    }

    /**
     * Helper method which send an "addResource" message to SWGCraft.org for
     * either a new or an old (historical) resource. This method intercepts any
     * error and logs to file and/or amends the returned object with a message.
     * 
     * @param kr the resource to submit
     * @param planet a planet, or {@code null}
     * @param old {@code true} if the resource is historical
     * @return the response from the server
     */
    private SWGSoapNOResResponse sendNewOld(
            SWGKnownResource kr, SWGPlanet planet, boolean old) {
        SWGSoapNOResResponse response = new SWGSoapNOResResponse(kr, planet);

        try {
            setServerURL();

            SOAPMessage msg = newSoapMessage();
            SOAPBodyElement sbe = newBodyElement("SoapAddResource", msg);

            // populate the body element
            SOAPElement ari = newElement("AddResourceInput", sbe);

            addChildElement("Name", kr.getName(), sbe, ari);
            addChildElement("Server", itos(kr.galaxy().id()), sbe, ari);
            addChildElement("Planet", itos(planet.id()), sbe, ari);
            addChildElement("Class", kr.rc().rcName(), sbe, ari);
            if (old) addChildElement("Current", "false", sbe, ari);

            // add stats, also zero values
            addStats(kr, ari, sbe, true);

            ari.addChildElement(newLoginChild(sbe));
            ari.addChildElement(newProgramInfoChild(sbe));

            // simpleTransform(msg, System.out);
            SOAPMessage respMsg = sendMessage(msg);

            // simpleTransform(respMsg, System.out);
            if (hasFault(respMsg, response))
                return response;

            Node n = getFirstChild(respMsg.getSOAPBody(), 2);

            response.parseNode(n);
            if (response.status == 999) {
                resetUserData();
            }
        } catch (Exception e) {
            String msg = "SWGSoapManager:sendNewOld: " + e.getMessage();
            SWGAide.printDebug("soap", 1, msg);
            response.faultMessage = msg;
        }
        return response;
    }

    /**
     * Sends an "addResource" message to SWGCraft.org which adds the identified
     * resource as "old" or "historical" to the database. This implementation
     * does not validate the argument.
     * <p>
     * The status codes of the response reads the outcome. If there is an error
     * it is intercepted and a message is added as a fault message to the
     * response.
     * <p>
     * <b>NOTICE:</b> Only {@link SWGResourceManager} is allowed to invoke this
     * method. See {@link SWGResource} for the detailed rationale.
     * 
     * @param kr the resource to submit as historical
     * @return the response from the server
     * @throws NullPointerException if the argument is {@code null}
     */
    synchronized SWGSoapNOResResponse sendOld(SWGKnownResource kr) {
        return sendNewOld(kr, SWGPlanet.DUMMY, true);
    }

    /**
     * Helper method that sets the URL for the SOAP server. More specifically,
     * this implementation obtains the values from SWGAide's preference keeper
     * and creates an URL which is used. The values are optionally set by the
     * user at {@link SWGCraftOptionsPanel}. Thus, before a call to the SOAP
     * server the request/send-methods must call this method. If no values are
     * set by the user the default values are used.
     * 
     * @see SWGCraft#getBaseURL()
     * @see SWGCraft#getSOAPServerPath()
     * @see #setURL(URL)
     * @throws MalformedURLException if the URL specifies an unknown protocol
     */
    private void setServerURL() throws MalformedURLException {
        URL url = new URL(SWGCraft.getBaseURL() + SWGCraft.getSOAPServerPath());
        setURL(url);
    }

    /**
     * Returns the SOAP handler for communicating with the SOAP server at
     * SWGCraft.org. If there is an error creating the SOAP handler it is
     * intercepted and a message is written to SWGAide's debug log and {@code
     * null} is returned.
     * 
     * @return the SOAP handler for SWGCraft.org, or {@code null}
     */
    static SWGSoapManager getSOAPHandler() {
        // lock on something never null and otherwise not locked on
        synchronized (NAME_SPACE_PREFIX) {
            if (soapHandler == null) {
                try {
                    soapHandler = new SWGSoapManager();
                } catch (Exception e) {
                    SWGAide.printError("SWGCraft:getSOAPHandler: ", e);
                }
            }
        }
        return soapHandler;
    }

    /**
     * A convenience integer-to-string method. This method is just a shorthand
     * for {@link Long#toString(long)}.
     * 
     * @param i the value
     * @return the string representation for {@code i}
     */
    static String itos(long i) {
        return Long.toString(i);
    }

    // /**
    // * For testing purposes only
    // */
    // public static void main(String... args) throws Exception {
    // SWGSoapManager sp = new SWGSoapManager();

    // SWGCraftUserInfoID sui = sp.getUserInfo("Zimoon");
    // SWGCraftUserInfoName sui = sp.getUserInfo(171);
    // SWGCraftUserDetailedInfo sui = sp.getDetailedUserInfo();

    // SWGCraftResourceInfo sui = sp
    // .getResourceInfo("SWGCraft.co.uk", "");

    // SWGCraftAddResourceResponse sui = sp.addResource("aspdothertestine",
    // "SWGCraft.co.uk", "Corellia", "Desh Copper", 0, 0, 0, 0, 0, 0,
    // 0, 0, 0, 0, 0);// 843623

    // SWGSoapEditResResponse sui =
    // sp.editResource("", "SWGCraft.co.uk", "Desh Copper", 843623, 500,
    // 2, 2, 0, 0, 222, 505, 1, 0, 343, 333, "test");

    // SWGCraftAddResourceResponse sui = sp.addResource("doubletestfive",
    // "SWGCraft.co.uk", "Corellia", "Desh Copper", 500, 0, 0, 0, 0,
    // 0, 0, 0, 0, 0, 0);

    // int id = sui.getSwgResID();
    // SWGCraftAddAvailableResponse sui = sp.addAvailable(843623,
    // "SWGCraft.co.uk", "");

    // SWGCraftStatusResponse sui = sp.despawnResource(843623);

    // SWGCraftAddAvailableResponse sui = sp.addAvailable(743796,
    // "SWGCraft.co.uk", "Endor");

    // SWGCraftCryptKeyResponse sui = sp.getCryptoKey();
    // String sui = sp.getHashPass();

    // SWGSoapListResResponse sui =
    // sp.requestResources("SWGCraft.co.uk", "azu");
    //
    // SWGSoapResourceResponse sui = sp.requestResource(670317);
    //
    // SWGMutableResource mr = new SWGMutableResource("Testsendoldfifth",
    // SWGDeshCopper.getInstance());
    // mr.galaxy(SWGCGalaxy.SWGCRAFT_CO_UK);
    // SWGKnownResource kr = new SWGKnownResource(mr);
    // SWGSoapNOResResponse sui = sp.sendOld(kr);
    //
    // System.err.println(sui.toString() + ' '//
    // + (sui.faultMessage != null
    // ? sui.faultMessage
    // : ""));
    // }

    /**
     * Returns the {@code int} value from the element contained in the list of
     * nodes and identified by its name. If the element is not found 0 is
     * returned. This implementation logs errors to SWGAide's log file.
     * 
     * @param nodes a list of nodes
     * @param elementName the name of the element
     * @return the {@code int} value from the identified element, or 0
     * @throws NullPointerException if the list is {@code null}
     */
    static int parseInt(NodeList nodes, String elementName) {
        return (int) parseLong(nodes, elementName);
    }

    /**
     * Returns the {@code long} value from the element contained in the list of
     * nodes and identified by its name. If the element is not found 0 is
     * returned. This implementation logs errors to SWGAide's log file.
     * 
     * @param nodes a list of nodes
     * @param elementName the name of the element
     * @return the {@code long} value from the identified element, or 0
     * @throws NullPointerException if the list is {@code null}
     */
    static long parseLong(NodeList nodes, String elementName) {
        try {
            String l = parseString(nodes, elementName);
            return ZNumber.longExc(l);
        } catch (Exception e) {
            String msg = "SWGSoapManager:parseLong: " + elementName + ':';
            SWGAide.printDebug("soap", 1, msg + e.getMessage());
            if (SWGConstants.DEV_DEBUG) SWGAide.printError(msg, e);
        }
        return 0;
    }

    /**
     * Returns the text content from the element contained in the list of nodes
     * and identified by its name. If the element is not found the empty string
     * is returned.
     * 
     * @param nodes a list of nodes
     * @param elementName the name of the element
     * @return the text content from the identified element, or the empty string
     * @throws NullPointerException if the list is {@code null}
     */
    static String parseString(NodeList nodes, String elementName) {
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node nn = nodes.item(i);
            String nName = nn.getNodeName();
            if (nName.equalsIgnoreCase(elementName)) { return nn.getTextContent(); }
        }
        return "";
    }

    /**
     * Returns the integer argument as a string. If the argument is outside the
     * range [1 1000] "0" is returned.
     * 
     * @param value the integer value
     * @return the integer argument as a string, or "0"
     */
    static String validateInt(int value) {
        if (0 < value && value <= 1000)
            return Integer.toString(value);
        return "0";
    }
}
