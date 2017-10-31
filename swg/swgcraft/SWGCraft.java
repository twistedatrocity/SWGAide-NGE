package swg.swgcraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;

import javax.crypto.SealedObject;
import javax.swing.JOptionPane;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.tools.SimplePrefsKeeper;
import swg.tools.ZCrypto;

/**
 * This class together with {@link SWGSoapManager} are the only communication
 * channels with SWGCraft.org. Hence this class provides a few generic utility
 * methods related to communication with SWGCraft.org. Specialized classes in
 * this package provides methods for the purpose of their classes respectively,
 * however, they utilize this class or the SOAP manager.
 * <p>
 * This class also contains generic static data related to SWGCraft.org, such as
 * URL and paths. For simplicity, also some data is contained which logically
 * should be contained in specialized classes.
 * <p>
 * <b>Historical Notes: </b>This class was originally the complete interface to
 * SWGCraft.org, a global Swiss knife. However, since SWGAide evolved in
 * functionality and features, and at the same time SWGCraft.org evolved, it was
 * logical to reduce the weight of this class. Even more important was that the
 * old version of this class provided public access to all features and it
 * became evident that developers, including myself, rather must obtain services
 * via different <i>managers</i>. These managers must maintain strict rules
 * while providing features and services to their clients.
 * <p>
 * Thus, 2009/8 this class was refactored and most of its content was split out
 * to specialized classes and to the mentioned managers.
 * <p>
 * <b>Technical notes: </b>Each manager is made up by two or more classes. The
 * top-most super-class is located in a package that relates to the feature set.
 * At that location the super-class has access to features with limited access
 * modifiers and the super-class makes available some features for sub-classes.
 * The sub-classes have access to features with limited access modifiers in the
 * package where they are located, which the super class has not. The net result
 * is that the sub-classes always provide a richer public feature set for
 * clients and they are the "public" interface, the super-class is not. At the
 * same time packages maintain as limited access modifiers as possible to hinder
 * clients from direct access to sensible data structures.
 * <p>
 * Clients better view such a construct as a logical entity, a manager and then
 * the richest type is given a name such as SWGxxx<b>Manager</b>.
 * <p>
 * <i>Zimoon</i>
 * 
 * @author Steven M. Doyle <shadow@triwizard.net>
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGCraft {

    /**
     * A flag used during tests.
     */
    private static final boolean ATEST = false;

    /**
     * The base URL to SWGCraft.org.
     */
    private static final String baseURL = "http://www.swgcraft.org";

    /**
     * The path and file name to the schematics XML file at SWGCraft.org.
     */
    private static final String categoriesXmlPath =
            "/dev/sendfile.php?file=categories_nge.xml";

    /**
     * The first part of the path to the file with current resources, the galaxy
     * name and file suffix must be supplemented.
     */
    private static final String currentResourcesPath =
            "/dev/sendfile.php?file=currentresources_";

    /**
     * The path and file name to the profession levels XML file at SWGCraft.org.
     */
    private static final String profLevelsXmlPath =
            "/dev/sendfile.php?file=myprofsecret_nge.xml";

    /**
     * The path and file name to the schematics XML file at SWGCraft.org.
     */
    private static final String schematicsXmlPath =
            "/dev/sendfile.php?file=schematics_nge.xml.gz";
			
	/**
     * The path and file name to the servers XML file at SWGCraft.org.
     */
    private static final String serversXmlPath =
            "/dev/sendfile.php?file=servers_detailed.xml";

    /**
     * The path to the SOAP server.
     */
    private static final String soapServerPath = "/dev/soap/server.php";

    /**
     * The minimum delay between obtaining the resource status file at
     * SWGCraft.org. The purpose of the delay is to minimize unwanted network
     * connections for something that was determined seconds ago, or less.
     * Currently 180 seconds.
     */
    final static int STATUS_CHECK_DELAY = 180;

    /**
     * The path and file name to the text status file at SWGCraft.org.
     */
    private static final String statusTxtPath = "/dev/public/status.txt";

    /**
     * The path and file name to the XML status file at SWGCraft.org.
     */
    private static final String statusXmlPath = "/dev/public/status.xml";

    /**
     * Do not create an instance of this class, all fields and methods are
     * static.
     */
    private SWGCraft() {
        throw new IllegalStateException("Do not instantiate");
    }

    /**
     * Downloads the content from the identified URL to the identified file.
     * This implementation optionally unzips the content; SWGCraft.org is using
     * the GZIP format for file compression. If the download is successful
     * {@code true} is returned, otherwise the error is handled and {@code
     * false} is returned or an exception is thrown.
     * 
     * @param source the URL to download from
     * @param target the file to store data at
     * @param isZipped <code>true</code> if the content is compressed
     * @return {@code false} if an error but no exception is thrown
     * @throws ConnectException if there is a connection error
     * @throws IOException if there is an I/O error
     * @throws NullPointerException if the argument is {@code null}
     */

    static boolean downloadURLToDisk(URL source, File target, boolean isZipped)
            throws ConnectException, IOException {

        InputStream from = null;
        FileOutputStream to = null;

        try {
            URLConnection uc = source.openConnection();
            from = uc.getInputStream();

            if (isZipped)
                from = new GZIPInputStream(from);

            to = new FileOutputStream(target);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = from.read(buffer)) >= 0) {
                to.write(buffer, 0, len);
            }
            return true;
        } catch (ConnectException e) {
            SWGAide.printDebug("crft", 1,
                    "SWGCraft: connection error: ", e.getMessage());
            throw e;
        } catch (UnknownHostException e) {
            showUnknownHostDialog(source, e);
        } catch (IOException e) {
            if (e.getMessage().equals("Not in GZIP format")) {
                SWGAide.printDebug("crft", 1,
                        "SWGCraft: temporary download error:",
                        e.getClass().toString(), ":", e.getMessage());
            } else {
                SWGAide.printDebug("crft", 1,
                        "SWGCraft:downloadURLToDisk:I/O:",
                        e.getClass().toString(), ":", e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            SWGAide.printError("SWGCraft:downloadURLToDisk", e);
        } finally {
            try {
                from.close();
            } catch (Exception e) { /* ignore exception */
            }
            try {
                to.close();
            } catch (Exception e) { /* ignore exception */
            }
        }
        return false;
    }

    /**
     * Returns the base URL to SWGCraft.org. This is the default value
     * {@link #baseURL}, or it a value which optionally is set by the user and
     * is contained in SWGAide's preference keeper.
     * 
     * @return the base URL to SWGCraft.org
     */
    static String getBaseURL() {
        String s = (String) getPrefs("optionBaseURL");
        return s == null
                ? baseURL
                : s;
    }

    /**
     * Returns path and file name to the categories XML file at SWGCraft.org.
     * This is the default value {@link #categoriesXmlPath}.
     * 
     * @return path to the categories XML file
     */
    static String getCategoriesPath() {
        return categoriesXmlPath;
    }
	
	/**
     * Returns path and file name to the servers XML file at SWGCraft.org.
     * This is the default value {@link #categoriesXmlPath}.
     * 
     * @return path to the servers XML file
     */
    static String getServersPath() {
        return serversXmlPath;
    }

    /**
     * Returns the major part of the path to the file with current resources;
     * this part must be complemented with the galaxy name and the file suffix.
     * This is the default value {@link #currentResourcesPath}, or it is a value
     * which is set by the user and is contained in SWGAide's preference keeper.
     * 
     * @return the major part of the path to the file with current resources
     */
    static String getCurrentResourcesPath() {
        String s = (String) getPrefs("optionResourcePath");
        return s == null
                ? currentResourcesPath
                : s;
    }

    /**
     * Returns the user's password for SWGCraft.org in plain text. The password
     * is stored in SWGAide's preference keeper in encrypted form. If the
     * password is not set this method returns {@code null}. If there is an
     * error while deciphering it is logged and {@code null} is returned.
     * 
     * @return the user's password in plain text, or {@code null}
     */
    static String getPassword() {
        Object o = getPrefs("optionPassWord");
        if (o != null) {
            try {
                return (String) ZCrypto.decipher((SealedObject) o);
            } catch (Exception e) {
                SWGAide.printError("SWGCraft:getPassword", e);
            }
        }
        return null;
    }

    /**
     * Convenience method which returns the value for {@code key} from SWGAide's
     * preference keeper. If no value is mapped to by {@code key}, or if the
     * preference keeper is unavailable, this method returns {@code null}.
     * 
     * @param key the key for the value to return
     * @return the mapped value, or {@code null}
     * @throws NullPointerException if the argument is {@code null}
     */
    static Object getPrefs(String key) {
        SimplePrefsKeeper pk = SWGFrame.getPrefsKeeper();
        return pk == null
                ? null
                : pk.get(key);
    }

    /**
     * Returns path and file name to the profession levels XML file at
     * SWGCraft.org. This is the default value {@link #profLevelsXmlPath}.
     * 
     * @return path to the profession levels XML file
     */
    static String getProfLevelsPath() {
        return profLevelsXmlPath;
    }

    /**
     * Returns path and file name to the schematics XML file at SWGCraft.org.
     * This is the default value {@link #schematicsXmlPath}.
     * 
     * @return path to the schematics XML file
     */
    static String getSchematicsPath() {
        return schematicsXmlPath;
    }

    /**
     * Returns the path to the SOAP server. This is the default value
     * {@link #soapServerPath}, or it a value which optionally is set by the
     * user and is contained in SWGAide's preference keeper.
     * 
     * @return the path to the SOAP server
     */
    static String getSOAPServerPath() {
        String s = (String) getPrefs("optionSOAPpath");
        return s == null
                ? soapServerPath
                : s;
    }

    /**
     * Returns the path to the text status file at SWGCraft.org. This is the
     * default value {@link #statusTxtPath}, or it a value which optionally is
     * set by the user and is contained in SWGAide's preference keeper.
     * 
     * @return the path to the text status file at SWGCraft.org
     */
    static String getStatusFileTXT() {
        String s = (String) getPrefs("optionStatusFile");
        return s == null
                ? statusTxtPath
                : s;
    }

    /**
     * Returns the path to the XML status file at SWGCraft.org. This is the
     * default value {@link #statusXmlPath}.
     * 
     * @return the path to the XML status file at SWGCraft.org
     */
    static String getStatusFileXML() {
        return statusXmlPath;
    }

    /**
     * Returns the URL for the text-based status file at SWGCraft.org. If there
     * is an error it is logged and this method throws a
     * {@link NullPointerException}.
     * 
     * @return the URL to the TXT status file at SWGCraft.org
     * @throws NullPointerException if there is an error creating the URL
     */
    static URL getStatusTextURL() {
        try {
            return new URL(getBaseURL() + getStatusFileTXT());
        } catch (MalformedURLException e) {
            throw malformedURLHelper("SWGCraft:getStatusTextURL", e);
        }
    }

    /**
     * Returns the URL for the XML-based status file at SWGCraft.org. If there
     * is an error it is logged and this method throws a
     * {@link NullPointerException}.
     * 
     * @return the URL to the XML status file at SWGCraft.org
     * @throws NullPointerException if there is an error creating the URL
     */
    static URL getStatusXmlURL() {
        // unused, exists for completeness
        try {
            return new URL(getBaseURL() + getStatusFileXML());
        } catch (MalformedURLException e) {
            throw malformedURLHelper("SWGCraft:getStatusXmlURL", e);
        }
    }

    /**
     * Returns the user name for SWGCraft.org. This is the name which is
     * specified by the user at {@link SWGCraftOptionsPanel} and it is stored in
     * SWGAide's preference keeper. If the user name is unknown this method
     * returns the empty string.
     * 
     * @return the user name for SWGCraft.org, or the empty string
     */
    public static String getUserName() {
        if (SWGCraft.ATEST) return "";

        String uName = (String) getPrefs("optionUserName");
        return uName != null
                ? uName
                : "";
    }

    /**
     * Returns the user's password for SWGCraft.org in plain text. The password
     * is set at {@link SWGCraftOptionsPanel} and is stored in SWGAide's
     * preference keeper in encrypted form. If the password is unknown the empty
     * string is returned.
     * 
     * @return the password in plain text, or the empty string
     */
    static String getUserPassword() {
        if (SWGCraft.ATEST) return "";

        String pswd = getPassword();
        return pswd != null
                ? pswd
                : "";
    }

    /**
     * Helper method which is invoked when there is an error creating a new URL.
     * The specified message is written to SWGAide's error log and this method
     * creates and returns a {@link NullPointerException}, an object to which
     * the specified error message and the specified exception's cause are set.
     * 
     * @param msg the error message
     * @param e the original exception
     * @return a chained exception which contains message and cause
     */
    private static NullPointerException malformedURLHelper(
            String msg, MalformedURLException e) {

        SWGAide.printError(msg, e);
        NullPointerException npe = new NullPointerException(msg);
        return (NullPointerException) npe.initCause(e);
    }

    /**
     * Helper method which is invoked when there is an error with connecting to
     * SWGCraft.org. This implementation displays the error at a GUI dialog and
     * writes the error to SWGAide's general log.
     * 
     * @param source the URL that fails
     * @param e the exception which triggers this invocation
     * @throws NullPointerException if an argument is {@code null}
     */
    static void showUnknownHostDialog(URL source, UnknownHostException e) {
        JOptionPane.showMessageDialog(SWGAide.frame(), source.getHost()
                + " unknown.\n"
                + "Verify that Options >> SWGCraft >> Base URL is correct.\n"
                + "If that URL is correct this is a temporary Internet error",
                "Unknown host", JOptionPane.ERROR_MESSAGE);
        SWGAide.printDebug("crft", 1, "SWGCraft connection:",
                e.getClass().toString(), ":", e.getMessage());
    }
}
