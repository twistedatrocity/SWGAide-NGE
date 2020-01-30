package swg.swgcraft;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import swg.SWGAide;
import swg.SWGConstants;
import swg.crafting.Stat;
import swg.crafting.UpdateSubscriber;
import swg.crafting.resources.ResourceUpdate;
import swg.crafting.resources.ResourceUpdate.UpdateType;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGMutableResource;
import swg.crafting.resources.SWGPlanetAvailabilityInfo;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceList;
import swg.crafting.resources.SWGResourceMgr;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.SWGResourceStats;
import swg.crafting.resources.SWGWayPointInfo;
import swg.crafting.resources.types.SWGFiberplast;
import swg.crafting.resources.types.SWGOrganic;
import swg.gui.SWGFrame;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.tools.SimplePrefsKeeper;
import swg.tools.ZReader;
import swg.tools.ZString;
import swg.tools.ZStuff;
import swg.tools.ZWriter;
import swg.tools.ZXml;

/**
 * This type is a sub-type to {@link SWGResourceMgr} and together they form an
 * interface to all resource handling in SWGAide and to all communication with
 * SWGCraft.org in relation to resources. In SWGAide no other client but this
 * type is allowed to manage {@link SWGKnownResource} and to communicate
 * resource data with SWGCraft.org. For the rationale behind this strict rule,
 * see {@link SWGResource}. Clients better always use this sub-type which has
 * the richer feature set, all inherited public methods plus those provided in
 * this type.
 * <P>
 * This "interface" provides methods to obtain <I>known resource&nbsp;</I>
 * objects, to submit resources, etc. Most methods are static so normally there
 * is no need for a reference to an instance of this type. All methods are
 * thread safe, some methods are blocking until networking is complete, other
 * methods are non-blocking and if such a feature is provided it will notify the
 * caller when data is ready.
 * <P>
 * The super-type manages instance control and creation of <I>known
 * resource&nbsp;</I> objects, and this class manages all communication
 * regarding resources with SWGCraft.org. This implementation manages two types
 * of communication: 1) it downloads files of current resources for the user's
 * main galaxy and for other galaxies on demand; 2) it submits and requests data
 * to and from the SOAP server via {@link SWGSoapManager}.
 * <P>
 * <B>Note: </B>Clients must not persistently store instances of <I>known
 * resource&nbsp;</I> objects. Detailed information is read in
 * {@link SWGResource} and in {@link SimplePrefsKeeper}.
 * <P>
 * See {@link SWGCraft} for further details on how communication with
 * SWGCraft.org is structured and how it must be managed. In short though,
 * classes in this package must not provide public access to any of their
 * features. All requests for service must be handled by the appropriate
 * managers which are located in this package.
 * <P>
 * <B>Appendix</B> <BR/>
 * The XML resource export files from SWGCraft.org has the following format:
 * 
 * <PRE>{@literal <resource_data>
    <planets>
      <planet name="Corellia" swgaide_id="1">
        <resources>
          <resource swgaide_id="123456789">
            <name>Hansolo</name>
            <type>Polymer</type>
            <swgaide_type_id>pol</swgaide_type_id>
            <available_swgaide_id>12345</available_swgaide_id>
            <available_timestamp>1234567890</available_timestamp>
            <available_by>zimoon</available_by>
            <available_by_swgaide_id>123</available_by_swgaide_id>
            <verified>false</verified>
          </resource>
        </resources>
      </planet>
     </planets>
   </resource_data>}</PRE>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public final class SWGResourceManager extends SWGResourceMgr {

    /**
     * The required levenshtein distance for resources; if the distance is
     * shorter the likelihood for error in spelling, etc., are significant.
     */
    private static final int LEVENSHTEIN_MIN = 4;

    /**
     * A list of locks denoting galaxies which are currently being updated. If a
     * galaxy is contained in this list it is being processed. Any consecutive
     * attempt to update such a galaxy should be aborted rather than being
     * queued on the lock only to initiate an unnecessary download.
     */
    static final List<SWGCGalaxy> locker = new ArrayList<SWGCGalaxy>();

    /**
     * The most recent time in Unix time when the status text file at
     * SWGCraft.org was read.
     */
    private static long previousStatusCheck = 0L;

    /**
     * The most recent "status time" at SWGCraft.org. This is the time for when
     * the resource export files was updated at SWGCraft.org. The default value
     * is 0.
     */
    private static Long previousStatusTime = Long.valueOf(0);
	
	/**
     * Denotes whether the last attempt at fetching the status time at SWGCraft.org was successful
     */
    private static boolean lastSuccessful = true;

    /**
     * A list of clients which are subscribing for update notifications. All
     * clients will be notified when a download is ready. The download may be
     * unrelated to the client, which determines how to proceed.
     * <P>
     * This list of subscribers is not stored by SWGAide but is created when
     * this resource manager is first instantiated. This list is thread safe
     * using {@link Collections#synchronizedList(List)}.
     */
    private static List<UpdateSubscriber> subscribers;

    /**
     * The timer which fires automatic updates for the user's main galaxy.
     */
    private transient static Timer timer = null;

    /**
     * Creates an instance of this type which together with
     * {@link SWGResourceMgr} forms the "interface" for all resource management
     * in SWGAide.
     * <P>
     * <B>Note: </B>This instance must be created before SWGAide's object
     * storage is loaded.
     * 
     * @param frame the frame for this application
     * @throws NullPointerException if the argument is {@code null}
     */
    public SWGResourceManager(SWGFrame frame) {
        super(frame);
        subscribers = Collections.synchronizedList(
                new ArrayList<UpdateSubscriber>());
    }

    /**
     * Starts a timer which a regular intervals tries to update the user's main
     * galaxy. If there already is a timer this method does nothing, or if the
     * user has not selected a main galaxy at the options panel for SWGCraft.org
     * {@link #updateMainGalaxy()} does nothing.
     */
    public void startAutoUpdate() {
        Boolean autoUpdate =
            (Boolean) SWGFrame.getPrefsKeeper().get("optionAutoUpdate");
        if (autoUpdate != null && autoUpdate.booleanValue()) {
            startAutoUpdate(6);
        }
    }

    /**
     * Helper method which starts a timer which every {@code minutes} will try
     * to update the user's main galaxy. If there already is a timer this method
     * does nothing, or if the user has not selected a main galaxy at the
     * options panel for SWGCraft.org {@link #updateMainGalaxy()} does nothing.
     * 
     * @param minutes
     *            number of minutes between each repeat action
     */
    private void startAutoUpdate(int minutes) {
        if (timer != null)
            return;

        timer = new Timer(minutes * 60 * 1000, // repeat every N minute
            new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateMainGalaxy();
                }
            });
        // set this to 10 seconds to give the rest of the app plenty of time to finish loading. 100 millis was just too short and things would clash
        // TODO need to probably just make this blocking during startup as the time delay is not a guarantee, but it does work well for now.
        timer.setInitialDelay(10000);
        timer.start();
    }

    /**
     * Stops and releases the automatic update timer. If there is an update in
     * progress it is not aborted but future updates are disabled.
     */
    static void stopAutoUpdate() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    /**
     * Helper member method which updates the resources for the main galaxy
     * which is selected at the SWGCraft options dialog. If the user has not
     * selected a main galaxy at the options panel for SWGCraft.org this method
     * rather disables auto-updating, writes a message to SWGAide's log file.
     */
    public static void updateMainGalaxy() {
        Integer gid = (Integer) SWGFrame.getPrefsKeeper().get("optionMainGalaxy");
        if (gid == 0) {
            stopAutoUpdate();
            SWGFrame.getPrefsKeeper().add("optionAutoUpdate", Boolean.FALSE);
            SWGAide.printDebug("cmgr", 1,
                "Disabled auto-update, set galaxy at Options>>SWGCraft");
            return;
        }

        SWGCGalaxy galaxy = SWGCGalaxy.fromID(gid);
        downloadCurrent(galaxy);
    }

    /**
     * Adds the specified subscriber to receive update notifications from this
     * resource manager. All clients will be notified when a download is ready.
     * The download may be unrelated to the client, which determines how to
     * proceed.
     * 
     * @param subscriber
     *            the subscriber for update notifications
     */
    public static final void addSubscriber(UpdateSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * An implementation which initiates a background job which for the
     * specified galaxy downloads current resources from SWGCraft.org. Once the
     * job is started this method returns. If there is an ongoing download for
     * the specified galaxy this method does nothing.
     * <P>
     * <B>Note: </B>This is the entry point for downloading a set of current
     * resources from SWGCraft.org. See {@link #downloadHelper(SWGCGalaxy)} for
     * further details.
     * 
     * @param galaxy
     *            a galaxy constant
     * @throws NullPointerException
     *             if galaxy is {@code null}
     */
    private static void downloadCurrent(final SWGCGalaxy galaxy) {
        if (galaxy == null)
            throw new NullPointerException("Galaxy is null");

        synchronized (locker) {
            // abort consecutive calls for the same galaxy while being processed
            if (locker.contains(galaxy))
                return;
            locker.add(galaxy);
        }

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {

            
            public void run() {
                downloadHelper(galaxy);
                synchronized (locker) {
                    locker.remove(galaxy);
                }
                executor.shutdown();
            }
        });
    }

    /**
     * Helper method which carries out its workload on a background thread. If
     * there is an error it is intercepted at some level and it is added to
     * SWGAide's general or error log file, and this method exits gracefully.
     * For the specified galaxy this implementation invokes helper methods for
     * the following steps:
     * <p>
     * <ol>
     * <li>determines if there is a download available, aborts if there is no
     * update &mdash; {@link #isGalaxyUpdated(SWGCGalaxy)}</li>
     * <li>downloads the available update, aborts if there is an error &mdash;
     * {@link SWGCraft#downloadURLToDisk(URL, File, boolean)}</li>
     * <li>updates the local status file &mdash;
     * {@link #statusLocalWrite(SWGCGalaxy, Long)}</li>
     * <li>creates a list of current resource objects &mdash;
     * {@link #localXmlParse(File, SWGCGalaxy)}</li>
     * <li>calls the super-type which updates its cache &mdash;
     * {@link SWGResourceMgr#updateFromDownload(SWGResourceList)}</li>
     * <li>notifies update subscribers &mdash;
     * {@link SWGResourceManager#notifySubscribers(ResourceUpdate)}</li>
     * </ol>
     * 
     * @param galaxy a galaxy constant
     * @throws NullPointerException if the argument is {@code null}
     */
    private static void downloadHelper(SWGCGalaxy galaxy) {
        File cf = localXML(galaxy);
        long prevChk = previousStatusCheck; // 0 the first time ever
        if (cf.exists() && !isGalaxyUpdated(galaxy)) {
            if (prevChk == 0) // notify anyway, to trigger checks and whatever
                SWGResourceManager.notifySubscribers(new ResourceUpdate(
                        UpdateType.NEW_DOWNLOAD, galaxy));
            return;
        }

        frame.putToStatbar("Updating " + galaxy.getName());

        try {
            Long statusTime = statusSWGCraftTime();

            URL u = swgcraftUrlXml(galaxy);
            if (!SWGCraft.downloadURLToDisk(u, cf, true)) {
                downloadStatusAbort(galaxy);
                return;
            }

            //SWGAide.printDebug(Thread.currentThread().getName(), 9, "SWGResourceManager:downloadHelper update reslist");
            SWGResourceList resList = localXmlParse(cf, galaxy);
            //SWGAide.printDebug(Thread.currentThread().getName(), 9, "SWGResourceManager:downloadHelper2 update reslist:" + resList);
            updateFromDownload(resList);
            statusLocalWrite(galaxy, statusTime);

        } catch (IOException e) {
            if (e.getMessage().equals("Not in GZIP format")) {
                JOptionPane.showMessageDialog(frame,
                        "Temporary download error", "Download error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                SWGAide.printDebug("cmgr", 1,
                        "SWGResourceManager:downloadHelper1: " + e.getMessage());
                JOptionPane.showMessageDialog(frame, "Connection error:\n"
                        + e.getMessage(), "Connection error",
                        JOptionPane.ERROR_MESSAGE);
            }
            downloadStatusAbort(galaxy);
            return;
        } catch (Exception e) {
            SWGAide.printDebug("cmgr", 1,
                    "SWGResourceManager:downloadHelper2: " + e.toString());
            e.printStackTrace(System.out);

            JOptionPane.showMessageDialog(frame,
                    "Problem while downloading resource data from SWGCraft"
                            + "\nGalaxy: " + galaxy.getName()
                            + "\nError type: "
                            + e.getMessage()
                            + "\nDetails are read in the log file",
                    "Communication error", JOptionPane.WARNING_MESSAGE);
            downloadStatusAbort(galaxy);
            return;
        }
        //SWGAide.printDebug(Thread.currentThread().getName(), 9, "SWGResourceManager:downloadHelper: send subscriber update");
        SWGResourceManager.notifySubscribers(new ResourceUpdate(
                UpdateType.NEW_DOWNLOAD, galaxy));
        
    }

    /**
     * Helper method which updates SWGAide's status bar if there is an error.
     * 
     * @param gxy a galaxy constant
     */
    private static void downloadStatusAbort(SWGCGalaxy gxy) {
        frame.putToStatbar("<html><font color=\"red\">Update aborted for "
                + gxy.getName() + "</font></html>");
    }

    /**
     * Returns a set of resources which are depleted at the specified galaxy.
     * This implementation is non-blocking and returns a set promptly. Thus, if
     * SWGAide is just launched the resource manager is perhaps not updated and
     * the returned set may be dated. If no resources are found
     * {@link SWGResourceSet#EMPTY} is returned.
     * <P>
     * As a side effect from a call to this method a background job starts which
     * tries to download current resources for {@code galaxy} from SWGCraft.org.
     * Once a download is ready and resource availability is updated update
     * notifications are sent to subscribers. Thus, it is suggested that a
     * client which is interested in resource sets from this method also
     * subscribes for update notifications, see
     * {@link SWGResourceManager#notifySubscribers(ResourceUpdate)}.
     * 
     * @param galaxy
     *            a galaxy constant
     * @return a set of resources which are depleted, or
     *         {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if galaxy is {@code null}
     */
    public static SWGResourceSet getDepleted(SWGCGalaxy galaxy) {
        downloadCurrent(galaxy);
        return SWGResourceMgr.cachedDepleted(galaxy);
    }

    // TODO: these methods will return the local instance. However, if it is an
    // old resource it will never be updated. Add method that enforces lookup
    // also for old, locally known resources, if there is any difference in
    // their stats or whatsoever, inform the user and either update SWGCraft or
    // the local copy, but refuse a difference since there should be none

    /**
     * Returns an instance of {@link SWGKnownResource} with the specified ID. If
     * the instance is cached locally it is returned promptly. Otherwise the
     * resource is requested from SWGCraft.org and a local instance is created,
     * cached, and returned. If the ID is unknown {@code null} is returned. If
     * there is an error a message is written to SWGAide's log file and an
     * exception is thrown.
     * <P>
     * This method is blocking and does not return until communication with
     * SWGCraft.org is complete. If the ID is known at SWGCraft.org this
     * implementation is guaranteed to return an instance.
     * <P>
     * If the client does not known the unique ID but knows the name and galaxy,
     * rather see {@link #getInstance(String, SWGCGalaxy)}.
     * 
     * @param id
     *            the unique SWGCraft ID
     * @return the resource instance, or {@code null}
     * @throws IllegalArgumentException
     *             if the ID is invalid
     * @throws IOException
     *             if there is an I/O error with SWGCraft.org
     */
    public static SWGKnownResource getInstance(long id) throws IOException {
        SWGKnownResource ret = cachedInstance(id);
        if (ret != null)
            return ret;

        try {
            SWGMutableResource mr = getMutable(id);
            return helperKnown(mr);
        } catch (Exception e) {
            SWGAide.printDebug("cmgr", 1,
                "SWGResourceManager:getInstance(id): " + e.toString());
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
    }

    /**
     * Returns an instance of {@link SWGKnownResource} with the specified name
     * from the specified galaxy. If the instance is cached locally it is
     * returned promptly. Otherwise the resource is requested from SWGCraft.org
     * and a local instance is created, cached, and returned. If the identifiers
     * are unknown at SWGCraft.org {@code null} is returned. If there is an
     * error a message is written to SWGAide's log file and an exception is
     * thrown.
     * <P>
     * This method is blocking and does not return until possible communication
     * with SWGCraft.org is complete. If the identifiers are known at
     * SWGCraft.org this implementation is guaranteed to return an instance.
     * <P>
     * If neither the unique SWGCraft ID nor the name-and-galaxy are known, see
     * {@link #getInstance(SWGMutableResource)}.
     * 
     * @param resourceName the proper resource name
     * @param galaxy a galaxy constant
     * @return the resource instance, or {@code null}
     * @throws IllegalArgumentException if an argument is invalid
     * @throws IOException if there is an I/O error with SWGCraft.org
     * @throws NullPointerException if an argument is {@code null}
     */
    public static SWGKnownResource getInstance(
            String resourceName, SWGCGalaxy galaxy) throws IOException {

        SWGKnownResource ret = cachedInstance(resourceName, galaxy);
        if (ret != null
                && SWGResourceClass.validate(
                        ret.stats(), ret.rc()) == null)
                return ret;

        try {
            SWGMutableResource mr = (ret != null && ret.id() > 0)
                    ? getMutable(ret.id())
                    : getMutable(resourceName, galaxy);
            return helperKnown(mr);
        } catch (Exception e) {
            SWGAide.printDebug("cmgr", 1,
                    "SWGResourceManager:getInstance(n,g): " + e.toString());
            IOException io = new IOException();
            io.initCause(e);
            throw io;
        }
    }

    /**
     * Returns an instance of {@link SWGKnownResource} based on the specified
     * argument. If the instance is cached locally it is returned promptly.
     * Otherwise the resource is requested from SWGCraft.org and a local
     * instance is created, cached, and returned. If the resource is unknown at
     * SWGCraft.org a local instance is created, cached, and returned. If there
     * is an error a message is written to SWGAide's log file and an exception
     * is thrown.
     * <P>
     * If the instance is locally cached or if it is found at SWGCraft.org the
     * returned instance may have values different then the argument. Otherwise
     * the returned instance has identical values.
     * <P>
     * This method is blocking and does not return until possible communication
     * with SWGCraft.org is complete. If the argument is valid this
     * implementation is guaranteed to return an instance.
     * 
     * @param source
     *            the resource to base the return value on
     * @return an instance of type {@link SWGKnownResource}
     * @throws IOException
     *             if there is an I/O error with SWGCraft.org
     * @throws IllegalArgumentException
     *             if the argument is not valid
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public static SWGKnownResource getInstance(SWGMutableResource source)
        throws IOException {
        
        if (source.rc().isSpaceOrRecycled())
            return source.rc().spaceOrRecycled();

        SWGKnownResource ret = null;
        if (source.id() > 0)
            ret = cachedInstance(source.id());
        if (ret == null)
            ret = cachedInstance(source.getName(), source.galaxy());

        if (ret != null)
            return ret;

        // TODO: optimize so that if the argument has been queried for within a
        // very short time SWGCraft.org (within seconds) is left aside and a
        // local instance is created right away

        SWGMutableResource remote;
        if (source.id() > 0)
            remote = getMutable(source.id());
        else
            remote = getMutable(source.getName(), source.galaxy());

        ret = helperKnown(remote);
        return ret != null
            ? ret
            : getKnownTrusted(source);
    }

    /**
     * Returns a resource of type {@link SWGMutableResource} with the specified
     * ID. This implementation requests the resource from SWGCraft.org and
     * return the response. If the ID is unknown {@code null} is returned. If
     * there is an error a message is written to SWGAide's log file and an
     * exception is thrown.
     * <P>
     * This method is blocking and does not return until communication with
     * SWGCraft.org is complete. If the ID is known at SWGCraft.org this
     * implementation is guaranteed to return an instance.
     * <P>
     * The purpose of this method is to supersede {@link #getInstance(long)} and
     * to obtain non-spawning resources from SWGCraft to for example update
     * locally known instances. See
     * {@link #updateInstance(SWGKnownResource, SWGMutableResource)}.
     * <P>
     * This method should not be used to obtain resources for persistent
     * storage.
     * 
     * @param id
     *            the unique SWGCraft ID
     * @return the resource instance, or {@code null}
     * @throws IllegalArgumentException
     *             if the ID is invalid
     * @throws IOException
     *             if there is an I/O error with SWGCraft.org
     */
    public static SWGMutableResource getMutable(long id) throws IOException {
        try {
            SWGSoapResourceResponse resp =
                SWGSoapManager.getSOAPHandler().requestResource(id);

            if (resp.isFaultless())
                return resp.getResource();

        } catch (Exception e) {
            SWGAide.printDebug("cmgr", 1, "SWGResourceManager:getMutable(id): "
                + e.toString());
            throw new IOException(e);
        }
        return null;
    }

    /**
     * Returns an instance of {@link SWGMutableResource} with the specified name
     * from the specified galaxy. This implementation requests the resource from
     * SWGCraft.org and return the response. If the identifiers are unknown
     * {@code null} is returned. If there is an error a message is written to
     * SWGAide's log file and an exception is thrown.
     * <P>
     * This method is blocking and does not return until possible communication
     * with SWGCraft.org is complete. If the identifiers are known at
     * SWGCraft.org this implementation is guaranteed to return an instance.
     * <P>
     * The purpose of this method is to supersede {@link #getInstance(long)} and
     * to obtain non-spawning resources from SWGCraft to for example update
     * locally known instances. See
     * {@link #updateInstance(SWGKnownResource, SWGMutableResource)}.
     * <P>
     * This method should not be used to obtain resources for persistent
     * storage.
     * 
     * @param resourceName
     *            the proper resource name
     * @param galaxy
     *            a galaxy constant
     * @return the resource instance, or {@code null}
     * @throws IOException
     *             if there is an I/O error with SWGCraft.org
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    public static SWGMutableResource getMutable(String resourceName,
        SWGCGalaxy galaxy) throws IOException {

        try {
            SWGSoapResourceResponse resp =
                SWGSoapManager.getSOAPHandler().requestResource(resourceName,
                    galaxy);

            if (resp.isFaultless())
                return resp.getResource();

        } catch (Exception e) {
            SWGAide.printDebug("cmgr", 1,
                "SWGResourceManager:getMutable(n,g): " + e.toString());
            throw new IOException(e);
        }
        return null;
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
     * @param firstLetters
     *            the first letters of a resource name, or its full name
     * @param galaxy
     *            a galaxy constant
     * @return a response with a list of resource tuples
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    public static SWGSoapListResResponse getResources(String firstLetters,
        SWGCGalaxy galaxy) {
        return SWGSoapManager.getSOAPHandler().requestResources(firstLetters,
            galaxy);
    }

    /**
     * Returns a set of old and new resources which have spawned at the
     * specified galaxy. This implementation is non-blocking and returns a set
     * promptly. Thus, if SWGAide is just launched the resource manager is
     * perhaps not updated and the returned set may be dated. If no resources
     * are found {@link SWGResourceSet#EMPTY} is returned.
     * <P>
     * As a side effect from a call to this method a background job starts which
     * tries to download current resources for {@code galaxy} from SWGCraft.org.
     * Once a download is ready update notifications are sent to subscribers.
     * Thus, it is suggested that a client which is interested in resource sets
     * from this method also subscribes for update notifications, see
     * {@link SWGResourceManager#notifySubscribers(ResourceUpdate)}.
     * 
     * @param galaxy
     *            a galaxy constant
     * @return a set of old and new resources, or {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if galaxy is {@code null}
     */
    public static SWGResourceSet getSet(SWGCGalaxy galaxy) {
        downloadCurrent(galaxy);
        return cachedSet(galaxy);
    }

    /**
     * Returns a set of resources which are spawning at the specified galaxy.
     * This implementation is non-blocking and returns a set promptly. Thus, if
     * SWGAide is just launched the resource manager is perhaps not updated and
     * the returned set may be dated. If no resources are found
     * {@link SWGResourceSet#EMPTY} is returned.
     * <P>
     * As a side effect from a call to this method a background job starts which
     * tries to download current resources for {@code galaxy} from SWGCraft.org.
     * Once a download is ready update notifications are sent to subscribers.
     * Thus, it is suggested that a client which is interested in resource sets
     * from this method also subscribes for update notifications, see
     * {@link SWGResourceManager#notifySubscribers(ResourceUpdate)}.
     * 
     * @param galaxy
     *            a galaxy constant
     * @return a set of resources which are currently spawning, or
     *         {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if galaxy is {@code null}
     */
    public static SWGResourceSet getSpawning(SWGCGalaxy galaxy) {
        downloadCurrent(galaxy);
        return cachedSpawning(galaxy);
    }

    /**
     * Returns the timestamp for the locally cached resource export file for the
     * specified galaxy. If there is no local cache 0 is returned. The time is
     * in seconds since the epoch, so called Unix time, which begun at January
     * 1, 1970. If no status file exists a message is added to SWGAide's general
     * log file.
     * 
     * @param galaxy a galaxy constant
     * @return the timestamp for the locally cached resource export file, or 0
     * @throws NullPointerException if galaxy is {@code null}
     */
    public static Long getStatusLocalTime(SWGCGalaxy galaxy) {
        File statusFile = statusLocalFile(galaxy);
        if (statusFile.exists()) {
            try {
                String ret = ZReader.read(statusFile);
                if (ret != null) return Long.valueOf(ret);
            } catch (Exception e) {
                SWGAide.printDebug("cmgr", 1,
                        "SWGResourceManager:getStatusLocalTime:",
                        e.getMessage());
            }
        } else {
            SWGAide.printDebug("cmgr", 3,
                    "SWGResourceManager:getStatusLocalTime:",
                    "no file for", galaxy.getName());
        }
        return Long.valueOf(0);
    }

    /**
     * Helper method which returns a known instance based on the argument, or
     * {@code null} if the argument is {@code null} or if the galaxy differs
     * between a locally cached instance and the argument.
     * 
     * @param mr the resource to base the return value on
     * @return a known instance based on the argument, or {@code null}
     */
    private static SWGKnownResource helperKnown(SWGMutableResource mr) {
        if (mr != null) {
            if (mr.rc().isSpaceOrRecycled())
                return mr.rc().spaceOrRecycled();

            SWGKnownResource kr = SWGResourceMgr.getKnown(mr);
            if (mr.id() > 0 && kr.id() == mr.id()
                    && SWGResourceClass.validate(
                            kr.stats(), kr.rc()) != null // invalid
                    && SWGResourceClass.validate(
                            mr.stats(), mr.rc()) == null) { // valid
                updateInstance(kr, mr, true);
                return kr;
            }
            if (kr.galaxy().equals(mr.galaxy()))
                return kr;
            else if (kr.isDepleted())
                SWGResourceMgr.remove(kr); // remove from general map
        }
        return null;
    }

    /**
     * Helper method which for the specified galaxy determines if there is a
     * more recent resource export file available at SWGCraft.org than SWGAide
     * has in its local file cache. In particular, for the specified galaxy this
     * implementation returns {@code true} if there is no local status file or
     * if the timestamp obtained from SWGCraft.org is more recent than the local
     * timestamp. In the following cases {@code false} is returned:
     * <ul>
     * <li>if the timestamp obtained from SWGCraft.org equals the local
     * timestamp</li>
     * <li>if there is fresh query with SWGCraft.org, see
     * {@link SWGCraft#STATUS_CHECK_DELAY}</li>
     * <li>if there is a network error &mdash; a network error will probably
     * obstruct an immediate download anyway</li>
     * </ul>
     * <p>
     * 
     * @param galaxy a galaxy constant
     * @return {@code true} if there is a more recent download available
     * @throws NullPointerException if the argument is {@code null}
     */
    private static boolean isGalaxyUpdated(SWGCGalaxy galaxy) {
        return statusIsUpdated(getStatusLocalTime(galaxy));
    }

    /**
     * Helper method that returns a set of known resources with similar names,
     * or {@code null}. This method iterates over the specified collection and
     * invokes {@link ZStuff#levenshteinDistance(String, String, boolean)}
     * to obtain the similarity between the name of the specified resources and
     * the element of the collection; if less than {@link #LEVENSHTEIN_MIN} this
     * method refines its logic based on the length of the shorter name. For a
     * possible alert this method determines if the first characters of each
     * stem of the class names of the two also are similar and if so it adds the
     * resource from the collection to the returned set. This method finally
     * returns the set, or {@code null} if no similar resource is found.
     * <p>
     * The part about class name was added because the high number of false
     * alerts and the assumption that an error is more likely to also have a
     * similar resource class name, either a typo in an abbreviation of the
     * resource class in SWGAide or at SWGCraft, or a mis-click at a drop-down
     * list at SWGCraft.
     * 
     * @param r a resource
     * @param rs a set of resource for a specified galaxy
     * @return a set of resource with very similarly names, or {@code null}
     */
    private static SWGResourceSet levenshteinDistanceCheck(
            SWGResource r, SWGResourceSet rs) {

        String rn = r.getName();
        String cn = levenshteinRC(r);
        int rnlen = rn.length();

        SWGResourceSet ret = new SWGResourceSet();

        for (SWGKnownResource kr : rs) {
            String kn = kr.getName();
            if (r.rc() == kr.rc() && kn.equals(rn)) continue;
            
            int ld = ZStuff.levenshteinDistance(rn, kn, false);
            if (ld < LEVENSHTEIN_MIN) {
                int len = Math.min(rnlen, kn.length());
                if (len >= LEVENSHTEIN_MIN * 2
                        || (len >= LEVENSHTEIN_MIN && ld <= 2)
                        || (len >= 3 && ld <= 1)) {

                    String kc = levenshteinRC(kr);

                    if (SWGConstants.DEV_DEBUG)
                        System.err.print(String.format(
                                "%s: %s: %s --- %s- %s",
                                Integer.toString(ld), rn, cn, kn, kc));

                    if (ZStuff.levenshteinDistance(
                            cn, kc, false) > LEVENSHTEIN_MIN) {
                        if (SWGConstants.DEV_DEBUG)
                            System.err.println(" --- pass");
                        continue; // do not add to ret
                    }

                    if (SWGConstants.DEV_DEBUG)
                        System.err.println(" --- warn");

                    ret.add(kr);
                }
            }
        }
        return ret.isEmpty()
                ? null
                : ret;
    }

    /**
     * Helper method which returns a string based on the class name of the
     * specified resource. The string is the first 3 characters of the first
     * three stems of the class name; no stem exists that is shorter than that.
     * 
     * @param r a resource
     * @return a string
     */
    private static String levenshteinRC(SWGResource r) {
        int i = 0;
        String[] cp = r.rc().rcName().split(" ");
        ZString z = new ZString();
        for (String s : cp) {
            z.app(s.substring(0, Math.min(s.length(), 3)));
            ++i;
            if (i >= 3) break;
        }
        return z.toString();
    }

    /**
     * Helper method which returns an abstract file which denotes the location
     * of the cached file of current resources. The file may not yet exist in
     * the local file system but this is the file to read/write for the
     * specified galaxy.
     * 
     * @param galaxy a galaxy constant
     * @return an abstract file for the cache file
     * @throws NullPointerException if the argument is {@code null}
     */
    private static File localXML(SWGCGalaxy galaxy) {
        String gxy = String.format("swgcraft-%s-current.xml",
                galaxy.getName().toLowerCase(Locale.ENGLISH));
        return new File(SWGConstants.getCacheDirectory(), gxy);
    }

    /**
     * Helper method which from the specified XML file returns a list of
     * resources. The XML file is a resource export file which is already
     * downloaded from SWGCraft.org for the specified galaxy. The returned list
     * contains objects of type {@link SWGMutableResource}. If there is an error
     * {@link SWGResourceList#EMPTY} is returned.
     * <p>
     * This implementation <i>does not </i> evaluate the elements in the
     * returned list with the local cache of known resources.
     * 
     * @param xmlFile the XML file to parse from
     * @param gxy a galaxy constant
     * @return a list of {@link SWGMutableResource}, or an empty list
     * @throws IOException if there is an I/O error
     * @throws NullPointerException if an argument is {@code null}
     * @throws ParserConfigurationException if there is a document error
     * @throws SAXException if there is an XML parse error
     */
    private static SWGResourceList localXmlParse(File xmlFile, SWGCGalaxy gxy)
            throws ParserConfigurationException, SAXException, IOException {

        if (xmlFile == null || gxy == null) throw new NullPointerException(
                "Argument is null, file=" + xmlFile + ", galaxy=" + gxy);

        Document xml = ZXml.parse(xmlFile);

        SWGResourceList resources = new SWGResourceList(COLLECTION_SIZE);

        NodeList nList = xml.getElementsByTagName("resource");
        int length = nList.getLength();
        for (int i = 0; i < length; ++i) {
            Element resElem = (Element) nList.item(i);

            long id = ZXml.longFromAttr(resElem, "swgaide_id");

            SWGMutableResource mr = (SWGMutableResource) resources.getByID(id);

            if (mr == null) { // this one is new
                mr = resourceFromXML(resElem, id, gxy);
                if (mr == null) continue; // may be a space resource, ignore
                resources.add(mr);
            }
            resourceAddPlanet(mr, resElem);
            // parse waypoints
            // TODO finish this
            NodeList waypoints = resElem.getElementsByTagName("waypoints");
            Element wrapper = (Element) waypoints.item(0);
            NodeList wList = wrapper.getElementsByTagName("waypoint");
            int wlength = wList.getLength();
            if(wlength>0) {
            	for (int w = 0; w < wlength; ++w) {
            		Element wp = (Element) wList.item(w);
            		int wid = ZXml.intFromAttr(wp, "swgaide_id");
            		String wptext = ZXml.stringFromElem(wp, "wptext");
            		if(wid>0) {
            			SWGWayPointInfo wInfo = new SWGWayPointInfo(wid, wptext);
            			mr.waypointAdd(wInfo);
            		}
            		//SWGAide.printDebug(Thread.currentThread().getName(), 9, "SWGResourceManager: waypoint Data: " + wid + " " + wptext);
            	}
            }
        }
        return resources.toReturn();
    }

    /**
     * Notifies all subscribers with the specified notification. See
     * {@link SWGResourceManager#addSubscriber(UpdateSubscriber)}.
     * 
     * @param notice
     *            the notification to send to all subscribers
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public static void notifySubscribers(ResourceUpdate notice) {
    	synchronized (subscribers) {
            for (UpdateSubscriber us : subscribers)
                us.handleUpdate(notice);            
        }
    }

    /**
     * Removes the specified subscriber from being notified by this resource
     * manager when a download is ready.
     * 
     * @param subscriber
     *            the subscriber to remove
     */
    public static void removeSubscriber(UpdateSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Helper method which adds a planet availability record to the specified
     * resource, see {@link SWGPlanetAvailabilityInfo}. The added record is
     * based on the content of the soecified XML element.
     * <p>
     * Technical Note:<br/>
     * The specified XML element is in fact contained within the element that
     * reads the planetary data; see the footnote of {@link SWGResourceManager}
     * for the layout of the XML file. This implementation traverses upwards to
     * find the planet.
     * 
     * @param mr the resource to add a planet record to
     * @param xml the XML element to start with
     * @throws NullPointerException if an argument is {@code null}
     */
    private static void resourceAddPlanet(SWGMutableResource mr, Element xml) {
        if (mr == null || xml == null)
            throw new NullPointerException("An argument is null");

       
        NodeList planets = xml.getElementsByTagName("planets");
        Element wrapper = (Element) planets.item(0);
        NodeList pList = wrapper.getElementsByTagName("planet");
        int length = pList.getLength();
        for (int i = 0; i < length; ++i) {
        	Element pElem = (Element) pList.item(i);

            int pid = ZXml.intFromAttr(pElem, "swgaide_id");
            if(pid>0) {
	            SWGPlanetAvailabilityInfo pInfo = new SWGPlanetAvailabilityInfo(
	                    SWGPlanet.fromID(pid),
	                    ZXml.longFromElem(xml, "available_timestamp"),
	                    ZXml.stringFromElem(xml, "available_by"));
	
	            mr.availability(pInfo);
            }
        }
    }

    /**
     * Helper method which creates and returns a {@link SWGMutableResource} from
     * the XML element, or {@code null} if this is a space resource. It is not
     * likely this method will throw an exception.
     * <p>
     * <b>Notice:</b> this implementation does not add planet availability. Only
     * ID, galaxy, name, type, and stats are added to the resource. See the
     * footnote of {@link SWGResourceManager} for the layout of the XML file.
     * 
     * @param xml the element that contains the SWGCraft-style resource data
     * @param id the unique SWGCraft ID for the resource to parse
     * @param galaxy a galaxy constant
     * @return a new mutable resource object, or {@code null}
     * @throws IllegalArgumentException if there is something invalid
     * @throws NullPointerException if something is parsed to {@code null}
     */
    private static SWGMutableResource resourceFromXML(
            Element xml, long id, SWGCGalaxy galaxy) {

        if (galaxy == null || id <= 0)
            throw new IllegalArgumentException(
                    "Invalid argument: " + galaxy + ' ' + id);

        String tok = ZXml.stringFromElem(xml, "swgaide_type_id");
        SWGResourceClass type = SWGResourceClass.rc(tok);
        if (type.isSpaceOrRecycled()) return null;

        String name = ZXml.stringFromElem(xml, "name");

        SWGMutableResource mr = new SWGMutableResource(name, type);
        mr.galaxy(galaxy);
        mr.id(id);

        // remember this, new instances are depleted by default, undeplete it
        mr.depleted(false);

        int[] values = new int[Stat.COUNT];
        for (Stat s : Stat.values()) {
            values[s.i] = ZXml.intFromElem(xml, s.getName());
        }
        SWGResourceStats rs = new SWGResourceStats(values);
        mr.stats(rs, false); // trust SWGCraft

        return mr;
    }

    /**
     * Sends an availability message for the specified planet to SWGCraft.org
     * and returns the SOAP response from the action.
     * <P>
     * This method is blocking until communication is ready or if there is an
     * error. If the action is successful the planet record is also added to the
     * specified resource. If there is an error it is read in the respone's
     * fault message.
     * 
     * @param resource
     *            the resource to augment
     * @param pai
     *            a planet availability record for the interesting planet
     * @return the response from the SOAP server
     * @throws NullPointerException
     *             if the argument or something parsed is {@code null}
     */
    public static SWGSoapAvailResResponse sendAvailable(
        SWGKnownResource resource, SWGPlanetAvailabilityInfo pai) {

        frame.putToStatbar(String.format("Availability: %s @ %s",
            resource.getName(), pai.planet()));

        SWGSoapAvailResResponse response =
            SWGSoapManager.getSOAPHandler().sendAvailable(resource,
            pai.planet());

        if (response.isFaultless())
            updatePlanet(resource, pai);

        return response;
    }

    /**
     * Sends a deplete message to SWGCraft.org and returns the SOAP response
     * from the action. The date is measured in seconds since January 1, 1970.
     * <P>
     * This method is blocking until communication is ready or if there is an
     * error. If the action is successful the argument is also set as depleted
     * at the specified date. If there is an error it is read in the respone's
     * fault message.
     * 
     * @param resource
     *            the resource to mark as depleted
     * @param date
     *            the time the resource was determined as depleted, in UTC time
     * @return the response from the SOAP server
     * @throws IllegalArgumentException
     *             if an argument is invalid
     * @throws NullPointerException
     *             if the resource is {@code null}
     */
    public static SWGSoapStatusResponse sendDeplete(
        SWGKnownResource resource, long date) {

        frame.putToStatbar(String.format("Depleting: %s / %s",
            resource.getName(), resource.rc().rcName()));

        SWGSoapStatusResponse response;
        if (resource.id() > 0)
            response =
                SWGSoapManager.getSOAPHandler().sendDespawn(resource.id());
        else {
            // local resource, must return a response
            response = new SWGSoapStatusResponse(-1);
            response.status = 1;
        }

        if (response.isFaultless())
            updateDepleted(resource, date);

        return response;
    }

    /**
     * Sends an edit-resource message to SWGCraft.org and returns the SOAP
     * response from the action.
     * <P>
     * This method is blocking until communication is ready or if there is an
     * error. If there is an error it is read in the respone's fault message.
     * 
     * @param resource
     *            the resource to send an update for
     * @return the response from the SOAP server
     * @throws NullPointerException
     *             if the argument or something parsed is {@code null}
     */
    public static SWGSoapEditResResponse sendEdit(SWGKnownResource resource) {
        
        frame.putToStatbar(String.format("Updating: %s / %s",
            resource.getName(), resource.rc().rcName()));
        
        SWGSoapEditResResponse response =
            SWGSoapManager.getSOAPHandler().sendEdit(resource, null);

        return response;
    }

    /**
     * Sends an add-resource message to SWGCraft.org and returns the SOAP
     * response from the action, or {@code null}. This method also sends
     * add-availability messages for all planets of the specified argument,
     * hence it must contain at least one planet. If no communication happened
     * this method returns {@code null} but if there was an error the returned
     * response carries an error message.
     * <p>
     * If a resource with a valid ID is cached locally the argument is not a new
     * resource and {@link SWGSoapAvailResResponse} is returned. Otherwise
     * {@link SWGSoapNOResResponse} is returned and the argument is amended
     * with the obtained SWGCraft ID.
     * <p>
     * If a resource with a similar name exists a {@link SWGSoapNOResResponse}
     * is returned with status set to -2; see its status-string. Possibly this
     * is a spelling error either at SWGCraft or in the to-be-submitted resource
     * and submission is aborted. However, false positives are common, hence the
     * boolean flag makes it possible to override the name similarity check.
     * <p>
     * This method is blocking until communication is finished or if there is an
     * error. If there is a communication error the returned response will
     * contain a resource instance and the error is read in the respone's fault
     * message. If there is an error adding planet availability it is rather
     * read in SWGAide's log file. Any other returns a response with {@code
     * null} as resource and an error message;
     * 
     * @param mr the resource to submit
     * @param skipNameCheck {@code true} to skip name similarity check
     * @return the response from the SOAP server, or {@code null}
     * @throws IllegalArgumentException if the argument or its content is
     *         invalid, or if no planet is specified
     * @throws NullPointerException if the argument or something parsed from it
     *         is {@code null}
     */
    public static SWGSoapStatusResponse sendNew(
            SWGMutableResource mr, boolean skipNameCheck) {

        if (mr.availability().size() <= 0)
            throw new IllegalArgumentException("No planets");

        SWGKnownResource kr;
        if (!skipNameCheck) {
            // check that no resource exists with a similar name
            SWGResourceSet rs = cachedSpawning(mr.galaxy());
            SWGResourceSet ret = levenshteinDistanceCheck(mr, rs);
            if (ret != null && !ret.isEmpty()) {
                SWGSoapNOResResponse r = 
                    new SWGSoapNOResResponse(ret.get(0), null);
                r.whatever = ret;
                r.status = -2;
                return r;
            }
        }

        // first request an instance of a known resource
        // we must possibly remove this instance determined by error messages
        // from SWGCraft.org
        kr = getKnown(mr);

        if (mr.galaxy().equals(kr.galaxy()) == false) {
            // trust the caller to be sending for the proper galaxy
            kr = getKnownTrusted(mr);
        }

        List<SWGPlanet> planets = kr.availability();

        // always, but also if it was erratically depleted
        updateDepleted(kr, -1);

        SWGSoapStatusResponse response = null;
        if (kr.id() <= 0) {
            // still unknown, at least locally

            if (kr.rc().isSub(SWGOrganic.class)
                    || kr.rc().isSub(SWGFiberplast.class)) {
                // locally mark resources depleted not to wait for next download
                long d = System.currentTimeMillis() / 1000L;
                for (SWGKnownResource k : cachedType(kr.rc(), kr.galaxy()))
                    if (k != kr) updateDepleted(k, d);
            }

            frame.putToStatbar(String.format("Submit new: %s / %s",
                    kr.getName(), kr.rc().rcName()));

            response = SWGSoapManager.getSOAPHandler().sendNew(
                    kr, planets.get(0));
            if (!response.isFaultless()) {
                SWGSoapNOResResponse r = (SWGSoapNOResResponse) response;
                if (r.status != 1 && r.status != 3) {
                    r.resource = null;
                    remove(kr);
                }
                return r;
            }

            mr.id(response.getSWGCraftID());
        }

        int i = response == null
                ? 0
                : 1; // if already added one planet in sendNew()

        SWGSoapAvailResResponse resp = null;
        for (; i < planets.size(); ++i) {
            SWGPlanet pl = planets.get(i);
            resp = sendAvailable(kr, kr.availabilityFor(pl));
            if (!resp.isFaultless())
                SWGAide.printDebug("cmgr", 1, "SWGResourceManager:sendNew:add "
                        + pl.getName() + ':' + resp.faultMessage);
        }

        return response == null
                ? resp
                : response; // retain and return the send-new response, if any
    }

    /**
     * Sends an add-historical-resource message to SWGCraft.org resource and
     * returns the SOAP response from the action. If there was an error the
     * returned response carries an error message.
     * <p>
     * The argument must be a known resource without ID if a resource is cached
     * locally, it is amended with the obtained SWGCraft ID. Otherwise the
     * argument must be a mutable resource and if communication is successful a
     * known resource is created, cached, and wrapped by the returned response.
     * If such a new and cached resource is not persistently stored it is soon
     * wasted; see {@link SWGKnownResource} for details on persistent storage.
     * <p>
     * This method is blocking until communication is finished or if there is an
     * error.
     * 
     * @param r the resource to submit
     * @return the response from the SOAP server
     * @throws IllegalArgumentException if its ID denotes a known resource or if
     *         the resource is invalid, such as no stats or they are out-of-caps
     * @throws NullPointerException if the argument or something parsed from it
     *         is {@code null}
     */
    public static SWGSoapNOResResponse sendOld(SWGResource r) {
        if (r.id() > 0)
            throw new IllegalArgumentException("Not unknown: " + r);
        String vs = SWGResourceClass.validate(r.stats(), r.rc());
        if (vs != null) throw new IllegalArgumentException(ZString.fs(
                    "Invalid stats: %s %s", r, vs));

        SWGKnownResource kr = r instanceof SWGKnownResource
                ? (SWGKnownResource) r
                : getKnown((SWGMutableResource) r);

        frame.putToStatbar(String.format("Submit old: %s / %s",
                    kr.getName(), kr.rc().rcName()));

        SWGSoapNOResResponse resp = SWGSoapManager.getSOAPHandler().sendOld(kr);
        if (!resp.isFaultless()) {
            // do not remove known, it is cached locally for some reason
            if (r != kr && resp.status != 1 && resp.status != 3) {
                resp.resource = null;
                remove(kr);
            }
        }
        return resp;
    }

    /**
     * Sends an message to SWGCraft.org to mark the specified resource available
     * again and returns the SOAP response. This method is used for a resource
     * that is wrongly marked as depleted. If the argument is not depleted this
     * method does nothing but returns {@code null}.
     * <P>
     * This method is blocking until communication is ready or if there is an
     * error. If the action is successful the specified resource is set tagged
     * as available again. If there is an error it is read in the respone's
     * fault message.
     * <p>
     * Finally this method nullifies SWGAide's stat-bar and initiates that a
     * notification is sent to resource update subscribers.
     * 
     * @param resource the resource to revive
     * @return the response from the SOAP server, or {@code null}
     * @throws NullPointerException if the argument or something parsed is
     *         {@code null}
     */
    public static SWGSoapStatusResponse sendRevive(
            SWGKnownResource resource) {

            frame.putToStatbar(String.format("Reviving: %s / %s",
                resource.getName(), resource.rc().rcName()));

            SWGSoapStatusResponse response;
            if (resource.id() > 0)
                response =
                    SWGSoapManager.getSOAPHandler().sendRespawn(resource.id());
            else {
                // local resource, must return a response
                response = new SWGSoapStatusResponse(-1);
                response.status = 1;
            }

            if (response.isFaultless())
                SWGResourceMgr.updateDepleted(resource, -1);

            frame.putToStatbar(null);
            final ExecutorService exec = Executors.newSingleThreadExecutor();
            exec.execute(new Runnable() {

                @Override
                public void run() {
                    notifySubscribers(new ResourceUpdate(
                            UpdateType.LOCAL_SUBMISSION, resource.galaxy()));
                    exec.shutdown();
                }
            });

            return response;
        }

    /**
     * Helper method which determines if the resource export files at
     * SWGCraft.org are updated more recently than the specified timestamp. The
     * arguments {@code null} or 0 always evaluates to {@code true}. This method
     * invokes {@link #statusSWGCraftTime()} to obtain a timestamp.
     * 
     * @param timeStamp a local timestamp for a galaxy
     * @return {@code true} if SWGCraft.org is updated more recently
     */
    private static boolean statusIsUpdated(Long timeStamp) {
        if (timeStamp == null || timeStamp.longValue() <= 0)
            return true;
        Long s = statusSWGCraftTime();
        return (s.compareTo(timeStamp) > 0);
    }

    /**
     * Helper method which returns an abstract status file object for the
     * identified galaxy. If there is no previous download for the specified
     * galaxy the file does not exist at the local file system. If the file
     * exists it reads the most recent timestamp for when the resource export
     * file for the galaxy was downloaded from SWGCraft.org.
     * 
     * @param galaxy a galaxy constant
     * @return an abstract file object for the local status file
     * @throws NullPointerException if the argument is {@code null}
     */
    private static File statusLocalFile(SWGCGalaxy galaxy) {
        String gxy = String.format("swgcraft-%s-status.txt",
                galaxy.getName().toLowerCase(Locale.ENGLISH));
        File statusFile = new File(SWGConstants.getCacheDirectory(), gxy);
        return statusFile;
    }

    /**
     * Helper method which writes the timestamp to file for the specified
     * galaxy. This method is only called from
     * {@link #downloadHelper(SWGCGalaxy)} when there is an updated local file
     * cached for the galaxy.
     * 
     * @param galaxy a galaxy constant
     * @param timestamp the timestamp to write to the local file
     * @throws NullPointerException if galaxy is {@code null}
     */
    private static void statusLocalWrite(SWGCGalaxy galaxy, Long timestamp) {
        ZWriter.write(timestamp.toString(), statusLocalFile(galaxy), false);
    }

    /**
     * Helper method which returns the most recent time for when the resource
     * export files was updated at SWGCraft.org. The returned value is the value
     * which is obtained by reading the resource status file at SWGCraft.org. If
     * there is an error 0 is returned.
     * <P>
     * This implementation allows for real queries to SWGCraft.org every
     * <I>n&nbsp;</I> seconds as defined in {@link SWGCraft#STATUS_CHECK_DELAY}.
     * If this method is called within the delay time the most recent time is
     * returned.
     * <P>
     * <blockquote><i><B>Unix time</B> format is the number of seconds since
     * January 1st, 1970, 00.00.00</i>.</blockquote>
     * 
     * @return the status time in <I>Unix time&nbsp;</I> format, or 0
     */
    private static Long statusSWGCraftTime() {
        long current = System.currentTimeMillis() / 1000L;
        if (current < (previousStatusCheck + SWGCraft.STATUS_CHECK_DELAY))
            return previousStatusTime;

        URL url = null;
        try {
            synchronized (previousStatusTime) {
                // check again in the case one was waiting on the lock
                if (current >= previousStatusCheck
                        + SWGCraft.STATUS_CHECK_DELAY) {
                    url = SWGCraft.getStatusTextURL();
                    String statusTime = ZReader.read(url.openStream());
                    previousStatusTime = Long.valueOf(statusTime);
                    previousStatusCheck = current;
					lastSuccessful = true;
                }
                return previousStatusTime;
            }
        } catch (UnknownHostException e) {
			//Making sure the unknown host dialog doesn't keep popping up on every attempt
            if(lastSuccessful) {
            SWGCraft.showUnknownHostDialog(url, e);
			lastSuccessful = false;
            }
            previousStatusCheck = current-SWGCraft.STATUS_CHECK_DELAY-90; //Do not check again for (at least) 90 seconds

        } catch (Throwable e) {
            SWGAide.printDebug("cmgr", 1,
                    "SWGResourceManager:statusSWGCraftTime:", e.toString());
        }
        return Long.valueOf(0);
    }

    /**
     * Helper method which just supplements the specified argument with the
     * unique SWGCraft ID. No other actions is performed by this method.
     * <P>
     * It is an error to call this method if the ID is already known. Once set
     * the ID never change, unless updated from unknown to known.
     * 
     * @param res
     *            the resource to update
     * @param swgcraftID
     *            the ID to supply
     * @throws IllegalArgumentException
     *             IllegalArgumentException if the argument is invalid
     * @throws NullPointerException
     *             if the resource is {@code null}
     */
    protected static void supplyID(SWGKnownResource res, long swgcraftID) {
        SWGResourceMgr.supplyID(res, swgcraftID);
    }

    /**
     * Helper method for {@link #swgcraftUrlCsv(SWGCGalaxy)} and
     * {@link #swgcraftUrlXml(SWGCGalaxy)}. The returned value is an URL for a
     * resource export file at SWGCraft.org. If there is an error while creating
     * the URL, it is intercepted, a message is written to SWGAide's error log,
     * and an exception is thrown.
     * 
     * @param galaxy a galaxy constant
     * @param suffix the proper suffix for the path, "xml.gz" or "csv.gz"
     * @return the URL asked for
     * @throws NullPointerException if galaxy is {@code null}
     * @throws MalformedURLException if there is an error creating the URL
     */
    private static URL swgcraftURL(SWGCGalaxy galaxy, String suffix)
            throws MalformedURLException {

        // suffix is assumed to be valid since called from local methods

        if (galaxy == null)
            throw new IllegalArgumentException("Galaxy is null");

        Integer gid = galaxy.id();

        ZString z = new ZString(SWGCraft.getBaseURL());
        z.app(SWGCraft.getCurrentResourcesPath()).app(gid).app('.').app(suffix);
        try {
            return new URL(z.toString());
        } catch (MalformedURLException e) {
            SWGAide.printError("SWGResourceManager:swgcraftURL" + z, e);
            throw e;
        }
    }

    /**
     * Helper method which returns a valid URL for the specified galaxy with the
     * suffix "csv.gz". The returned value is an URL for a resource export file
     * at SWGCraft.org which is a gzip'd CSV file. If there is an error while
     * creating the URL, it is intercepted, a message is written to SWGAide's
     * error log, and an exception is thrown.
     * 
     * @param galaxy
     *            a galaxy constant
     * @return the URL to the zipped CSV file
     * @throws NullPointerException
     *             if an argument is {@code null}
     * @throws MalformedURLException
     *             if there is an error creating the URL
     */
    @SuppressWarnings("unused")
    private static URL swgcraftUrlCsv(SWGCGalaxy galaxy)
        throws MalformedURLException {
        // unused, exists for completeness
        return swgcraftURL(galaxy, "csv.gz");
    }

    /**
     * Helper method which returns a valid URL the specified galaxy with the
     * suffix "xml.gz". The returned value is an URL for a resource export file
     * at SWGCraft.org which is a gzip'd XML file. If there is an error while
     * creating the URL, it is intercepted, a message is written to SWGAide's
     * error log, and an exception is thrown.
     * 
     * @param galaxy
     *            a galaxy constant
     * @return the URL to the zipped XML file
     * @throws NullPointerException
     *             if an argument is {@code null}
     * @throws MalformedURLException
     *             if there is an error creating the URL
     */
    private static URL swgcraftUrlXml(SWGCGalaxy galaxy)
        throws MalformedURLException {
        return swgcraftURL(galaxy, "xml.gz");
    }

    /**
     * Updates a known instance based on the other resource object. This
     * implementation examines all fields of the known instance relative the
     * other resource. If there is a difference the known instance is updated
     * and non-negligible changes are individually logged to SWGAide's log file.
     * Basic validation is always performed. See {@link SWGResource} for further
     * information. This method is called in the following cases:
     * <UL>
     * <LI>if {@code other} is obtained from SWGCraft.org via a SOAP action</LI>
     * <LI>if {@code other} is obtained from SWGCraft.org via a downloaded
     * resource export file</LI>
     * <LI>if {@code known} is updated before submission to SWGCraft.org due to
     * missing or faulty data</LI>
     * <LI>if {@code known} is unknown at SWGCraft.org but the user edits data,
     * for example at the resource inventory</LI>
     * </UL>
     * 
     * @param known
     *            a known resource
     * @param other
     *            another resource, assumed to be "better"
     * @throws IllegalArgumentException
     *             if {@code other} has invalid data
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    public static void updateInstance(SWGKnownResource known,
        SWGMutableResource other) {
        SWGResourceMgr.updateInstance(known, other, false);
    }
}
