package swg.swgcraft;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import org.w3c.dom.Document;

import swg.SWGAide;
import swg.SWGConstants;
import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.swgcraft.SWGCraftCache.CacheUpdate.UpdateType;
import swg.tools.ZReader;
import swg.tools.ZWriter;
import swg.tools.ZXml;

/**
 * This type provides data blobs based on files which are downloaded from
 * SWGCraft.org and cached locally; usually this is XML files for which this
 * type provides XML documents. SWGAide clients request and obtain data blobs
 * and can also subscribe for update notifications. This type checks for updates
 * to specified files, downloads and caches a copy when an update is available,
 * and finally notifies all subscribers that an updated data blob is available.
 * <p>
 * This type provides no other logic than providing basic access to the data
 * blobs, maintaining the local cache of the specified data files, and handling
 * a limited backup of the cached files. The locally cached files are stored in
 * the "crafting" folder, as opposed to the downloaded temporary resource files.
 * <p>
 * This type is thread safe and it synchronizes on a relevant lock object for
 * the smallest possible scope. This type is not instantiated but provides
 * static access to its methods.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGCraftCache {

    // XXX: implement hourly checks for updates ??
    // support for category subscribers ??

    /**
     * The abstract file for the categories XML file.
     */
    private static final File catXML = new File("crafting", "categories.xml");
    
    /**
     * The abstract file for the professions XML file.
     */
    private static final File profXML = new File("crafting", "professions.xml");
	
	/**
     * The abstract file for the servers XML file.
     */
    private static final File serversXML = new File("crafting", "servers.xml");
    
	/**
	 * The abstract file for the schematics XML file.
	 */
	public static final File schemXML = new File("crafting", "schematics.xml");

	// XXX: implement hourly checks for updates ??
	// support for category subscribers ??
	
	/**
	 * A list of clients which subscribe for notifications regarding server
	 * information. This static list is instantiated when this type is
	 * first loaded.
	 */
	private static final List<UpdateSubscriber> serversSubscribers =
	        new ArrayList<UpdateSubscriber>();

	/**
	 * A list of clients which subscribe for notifications regarding categories.
	 * This static list is instantiated when this type is first loaded.
	 */
	private static final List<UpdateSubscriber> categorySubscribers =
	    new ArrayList<UpdateSubscriber>();

	/**
     * A list of clients which subscribe for notifications regarding profession
     * level information. This static list is instantiated when this type is
     * first loaded.
     */
    private static final List<UpdateSubscriber> profLevelSubscribers =
        new ArrayList<UpdateSubscriber>();

    /**
     * A list of clients which subscribe for notifications regarding schematics.
     * This static list is instantiated when this type is first loaded.
     */
    private static final List<UpdateSubscriber> schemSubscribers =
        new ArrayList<UpdateSubscriber>();

    /**
     * This type should not be instantiated.
     */
    private SWGCraftCache() {
        throw new AssertionError(); // prevent instantiation
    }

    /**
     * Adds the specified subscriber to the list of subscribers for the
     * specified type of notifications. When an updated local XML document of
     * the appropriate type is available all subscribers are notified according
     * to the {@link UpdateSubscriber} interface. The notifier uses an instance
     * of {@link CacheUpdate} and its type specifies which type of update the
     * notification pertains to.
     * <p>
     * This method is thread safe.
     * <p>
     * <b>Notice:</b> Notification executes at a background
     * {@link ExecutorService}, hence if the subscriber touches Swing GUI the
     * callback <b>must dispatch</b> the job on the event thread.
     * <p>
     * <b>Notice:</b> Currently {@link UpdateType#CATEGORIES} is unused.
     * 
     * @param subscriber
     *            a client interested in update notifications
     * @param type
     *            the wanted type of notifications
     */
    public static void addSubscriber(
        UpdateSubscriber subscriber, CacheUpdate.UpdateType type) {
        if (subscriber == null || type == null)
            throw new NullPointerException("Argument is null");

        if (type == UpdateType.SCHEMATICS) {
            synchronized (schemSubscribers) {
                schemSubscribers.add(subscriber);
            }
        } else if (type == UpdateType.CATEGORIES) {
            synchronized (categorySubscribers) {
                categorySubscribers.add(subscriber);
            }
        } else if (type == UpdateType.PROF_LEVELS) {
            synchronized (profLevelSubscribers) {
                profLevelSubscribers.add(subscriber);
            }
        }
		else if (type == UpdateType.SERVERS) {
            synchronized (serversSubscribers) {
                serversSubscribers.add(subscriber);
            }
        }
    }

    /**
     * Returns an XML document for the cached categories file, or {@code null}
     * if no file exists or if there is an error. The returned document is not
     * cached by this type but is parsed from file on demand; clients must not
     * repeatedly obtain a document unless they are notified about an update.
     * 
     * @return the categories XML document, or {@code null}
     */
    public static Document getCategories() {
        synchronized (catXML) {
            if (catXML.exists()) {
                try {
                    return ZXml.parse(catXML);
                } catch (Exception e) {
                    SWGAide.printError("SWGCraftCache:getCategories", e);
                }
            }
        }
        return null;
    }
    
    public static File getserversXMLpath() {
    	return serversXML;
    }

    /**
     * Returns an XML document for the cached profession levels file, or {@code
     * null} if no file exists or if there is an error. The returned document is
     * not cached by this type but is parsed from file on demand; clients must
     * not repeatedly obtain a document unless they are notified about an
     * update.
     * 
     * @return the profession levels XML document, or {@code null}
     */
    public static Document getProfLevels() {
        synchronized (profXML) {
            if (profXML.exists()) {
                try {
                    return ZXml.parse(profXML);
                } catch (Exception e) {
                    SWGAide.printError("SWGCraftCache:getProfLevels", e);
                }
            }
        }
        return null;
    }

    /**
     * Returns an XML document for the cached schematics file, or {@code null}
     * if no file exists or if there is an error. The returned document is not
     * cached by this type but is parsed from file on demand; clients must not
     * repeatedly obtain a document unless they are notified about an update.
     * 
     * @return the schematics XML document, or {@code null}
     */
    public static Document getSchematics() {
        synchronized (schemXML) {
            if (schemXML.exists()) {
                try {
                    return ZXml.parse(schemXML);
                } catch (Exception e) {
                    SWGAide.printError("SWGCraftCache:getSchematics", e);
                }
            }
        }
        return null;
    }

    /**
     * Helper method which returns the most recent modification date for the
     * specified URL; the returned string is on the form YYYY-MM-DD. This method
     * invokes {@link HttpsURLConnection#getLastModified()}; if the service provider
     * does not return the true modification date the return value is undefined,
     * or "1970-01-01", or "0". If there is an error it is caught, a message is
     * logged, and this method returns "0".
     * 
     * @param url
     *            the URL to return a modification date for
     * @return a date at the form YYYY-MM-DD, or "0"
     */
    private static LocalDateTime lastModified(URL url) {
        try {
        	LocalDateTime ld = null;
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            ld = LocalDateTime.ofInstant(Instant.ofEpochMilli(conn.getLastModified()), ZoneId.of("UTC") );
            conn.disconnect();
            ld = ld.truncatedTo(ChronoUnit.HOURS);
            if(SWGConstants.DEV_DEBUG) {
            	SWGAide.printDebug("debug", 9, "SWGCraftCache:lastModified " + url.toString() + " Remote Date: " + ld.toEpochSecond(ZoneOffset.UTC));
            }
            return ld;
        } catch (Exception e) {
            SWGAide.printDebug("cach", 1,
                "SWGCraftCache:lastModified: " + e.getMessage());
        }
        return null;
    }

    /**
     * Helper method which returns a date at the form YYYY-MM-DD which is read
     * from the specified file. In particular, the expected string must be on
     * the format {@code last_updated="2010-12-31"} and it must be found within
     * the first ten lines of the specified file. If there is no date within the
     * first ten lines this methods invokes {@link File#lastModified()} for a
     * date. If there is an error it is caught, a message is logged, and "0" is
     * returned.
     * 
     * @param file a local file
     * @return a date at the form YYYY-MM-DD, or "0"
     */
    public static LocalDateTime localDate(File file) {
        String date = "0";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss zzz" );
        LocalDateTime ld = null;
        ZReader sr = ZReader.newTextReader(file);
        if (sr != null) {
            String helper = "last_updated=\"";
            String line;
            while (sr.next() < 10 && (line = sr.line()) != null) {
                int f = line.indexOf(helper);
                if (f >= 0) {
                    f = f + helper.length();
                    int l = line.indexOf('\"', f);
                    if (l >= 0 && l < line.length()) {
                        date = line.substring(f, l);
                        if(date.length()>10) {
                        	ld = LocalDateTime.parse( date , fmt );
                        }else {
                        	ld = LocalDateTime.parse( date + " 00:00:00 UTC" , fmt );
                        }
                    }
                    break;
                }
            }
            sr.close();
        }
        
        
        if (date.equals("0")) {
            ld = LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), TimeZone.getDefault().toZoneId() );
        }
        ld = ld.truncatedTo(ChronoUnit.HOURS);
        if(SWGConstants.DEV_DEBUG) {
        	SWGAide.printDebug("debug", 9, "SWGCraftCache:localDate " + file.toString() + " XML Date: " + ld.toEpochSecond(ZoneOffset.UTC));
        }
        return ld;
    }

    /**
     * Helper method which notifies listeners that have subscribed for updates
     * of the specified type. This method is thread-safe.
     * 
     * @param type
     *            the type of notification
     */
    private static void notifySubscribers(UpdateType type) {
        CacheUpdate up = new CacheUpdate(type);
        if (type == UpdateType.SCHEMATICS) {
            synchronized (schemSubscribers) {
                for (UpdateSubscriber s : schemSubscribers)
                    s.handleUpdate(up);
            }
        } else if (type == UpdateType.CATEGORIES) {
            synchronized (categorySubscribers) {
                for (UpdateSubscriber s : categorySubscribers)
                    s.handleUpdate(up);
            }
        } else if (type == UpdateType.PROF_LEVELS) {
            synchronized (profLevelSubscribers) {
                for (UpdateSubscriber s : profLevelSubscribers)
                    s.handleUpdate(up);
            }
		} else if (type == UpdateType.SERVERS) {
            synchronized (serversSubscribers) {
                for (UpdateSubscriber s : serversSubscribers)
                    s.handleUpdate(up);
            }
        }
    }

    /**
     * Removes the specified subscriber from the list for the specified type. If
     * the subscriber is not found this method does nothing. This method is
     * thread safe.
     * 
     * @param subscriber
     *            a exiting subscriber
     * @param type
     *            the type of notification to unsubscribe for
     */
    public static void removeSubscriber(
        UpdateSubscriber subscriber, CacheUpdate.UpdateType type) {
        if (type == UpdateType.SCHEMATICS) {
            synchronized (schemSubscribers) {
                schemSubscribers.remove(subscriber);
            }
        } else if (type == UpdateType.CATEGORIES) {
            synchronized (categorySubscribers) {
                categorySubscribers.remove(subscriber);
            }
        } else if (type == UpdateType.PROF_LEVELS) {
            synchronized (profLevelSubscribers) {
                profLevelSubscribers.remove(subscriber);
            }
        } else if (type == UpdateType.SERVERS) {
            synchronized (serversSubscribers) {
                serversSubscribers.remove(subscriber);
            }
        }
    }

    /**
     * Helper method which updates the specified local file. If there is an
     * error it is caught, a message is written to the appropriate log file, and
     * this method returns {@code false}.
     * 
     * @param local
     *            the locally cached file
     * @param url
     *            the remote resource
     * @param zipped
     *            {@code true} if the content is compressed
     * @return {@code true} if the local file is updated
     */
    private static boolean update(File local, URL url, boolean zipped) {
        try {
            if (updateExists(local, url)) {
                File tmp = new File(
                    SWGConstants.getCacheDirectory(), local.getName());
                if (SWGCraft.downloadURLToDisk(url, tmp, zipped)) {
                    return updateLocally(local, tmp);
                }
                SWGAide.printDebug("cach", 1,
                    "SWGCraftCache:update:download failed: " + local.getName());
            }
        } catch (IOException e) {
            // probably network error, not a SWGAide error, hence plain log
            SWGAide.printDebug("cach", 1,
                "SWGCraftCache:update: " + e.getMessage());
        } catch (Throwable e) {
            SWGAide.printError("SWGCraftCache:update", e);
        }
        return false;
    }
    
    /**
     * Exact same thing as updateCache but this one does not run in background.
     * This is necessary for startup when files do not exist, we don't want things clashing
     * until these files are retrieved.
     */
    
    public static void updateCacheBlocking() {
    	try {
            if (update(profXML, urlProfLevels(), false))
                notifySubscribers(UpdateType.PROF_LEVELS);
        } catch (MalformedURLException e) {
            SWGAide.printError("SWGCraftCache:updatCache:prof", e);
        }
        boolean catUpdated = false;// XXX: maybe remove later
        try {
            if (update(catXML, urlCategories(), false))
                catUpdated = true; // notifyCatSubscribers();
        } catch (MalformedURLException e) {
            SWGAide.printError("SWGCraftCache:updatCache:cat", e);
        }
        try {
			if(update(serversXML, urlServers(), false))
            {
            	notifySubscribers(UpdateType.SERVERS);
            }
        } catch (MalformedURLException e) {
            SWGAide.printError("SWGCraftCache:updatCache:servers", e);
        }
        try {
            if (update(schemXML, urlSchematics(), true)
                || catUpdated)
                notifySubscribers(UpdateType.SCHEMATICS);
        } catch (MalformedURLException e) {
            SWGAide.printError("SWGCraftCache:updatCache:schem", e);
        }
    }
    
    /**
     * Updates the local cache of files maintained by this type. More
     * specifically, this method invokes several helper methods and for one by
     * one of the maintained files it is determined if no local file exists or
     * if the remote file is more recent, downloads the file, makes a backup of
     * the local file, replaces it with the new file, and notifies possible
     * subscribers. If a local file is up-to-date this method does nothing but
     * continues with the next file.
     * <p>
     * If there is an update available, notified subscribers decides whether to
     * obtain an updated data blob or not. This method does not guarantee the
     * files to be checked and updated in any particular order, clients must
     * themselves maintain possible interdependencies.
     * <p>
     * This method is thread safe.
     * <p>
     * <b>Notice:</b> Notification executes at a background
     * {@link ExecutorService}, hence if the subscriber touches Swing GUI the
     * callback <b>must dispatch</b> the job on the event thread.
     */
    public static void updateCache() {
        synchronized (SWGCraftCache.class) {
            final ExecutorService exec = Executors.newSingleThreadExecutor();
            exec.execute(new Runnable() {

                public void run() {
                    try {
                        if (update(profXML, urlProfLevels(), false))
                            notifySubscribers(UpdateType.PROF_LEVELS);
                    } catch (MalformedURLException e) {
                        SWGAide.printError("SWGCraftCache:updatCache:prof", e);
                    }
                    boolean catUpdated = false;// XXX: maybe remove later
                    try {
                        if (update(catXML, urlCategories(), false))
                            catUpdated = true; // notifyCatSubscribers();
                    } catch (MalformedURLException e) {
                        SWGAide.printError("SWGCraftCache:updatCache:cat", e);
                    }
                    try {
						if(update(serversXML, urlServers(), false))
                        {
                        	notifySubscribers(UpdateType.SERVERS);
                        }
                    } catch (MalformedURLException e) {
                        SWGAide.printError("SWGCraftCache:updatCache:servers", e);
                    }
                    try {
                        if (update(schemXML, urlSchematics(), true)
                            || catUpdated)
                            notifySubscribers(UpdateType.SCHEMATICS);
                    } catch (MalformedURLException e) {
                        SWGAide.printError("SWGCraftCache:updatCache:schem", e);
                    }
                    exec.shutdown();
                }
            });
        }
    }

    /**
     * Helper method which determines if the specified file is updated as
     * compared to the specified URL. This method returns {@code true} if no
     * local copy exists, or if the remote resource is more recent than the
     * local file, the granularity is by calendar date. If there is an error it
     * is caught and a message is logged, and this method returns {@code false}.
     * 
     * @param file
     *            the file to check for
     * @param url
     *            the URL to compare with
     * @return {@code true} if an update exists
     */
    private static boolean updateExists(File file, URL url) {
        if (!file.exists()) {
        	if(SWGConstants.DEV_DEBUG) {
        		SWGAide.printDebug("debug", 9, "SWGCraftCache:filenotexists : getting new file " + file.toString() );
        	}
            return true;
        }

        Boolean result = false;
        LocalDateTime ld = localDate(file);
        LocalDateTime rd = lastModified(url);
        if (ld.compareTo(rd) != 0) {
        	result = true;
        	if(SWGConstants.DEV_DEBUG) {
        		SWGAide.printDebug("debug", 9, "SWGCraftCache:updateExists : getting new file " + file.toString() + " LD: " + ld.toEpochSecond(ZoneOffset.UTC) + " RD: " + rd.toEpochSecond(ZoneOffset.UTC) );
        	}
        }
        return result;
    }

    /**
     * Helper method which backups the specified local file and replaces it with
     * the specified temporary file. If there is an error it is caught, a
     * message is logged, and {@code false} is returned, otherwise {@code true}
     * is returned.
     * 
     * @param local
     *            the local file, may not exist yet
     * @param tmp
     *            a temporary file
     * @return {@code true} if successful
     */
    private static boolean updateLocally(File local, File tmp) {
        synchronized (local) {
            if (local.exists()) {
                if (ZWriter.backup(local, "", 3) < 0)
                    SWGAide.printError(
                        "SWGCraftCache:updateLocally: backup failed: "
                        + local.getName(), null);

                if (!local.delete())
                    SWGAide.printError(
                        "SWGCraftCache:updateLocally: delete failed: "
                        + local.getName(), null);
            }

            if (tmp.renameTo(local))
                return true;

            // else
            /*SWGAide.printError("SWGCraftCache:updateLocally: replace failed: "
                + local.getName(), null); */
            SWGAide.printDebug("cc", 1, "SWGCraftCache:updateLocally: replace failed: " + local.getName());
            return false;
        }
    }

    /**
     * Helper method which returns an URL for the categories XML file at
     * SWGCraft.org.
     * 
     * @return a categories URL
     * @throws MalformedURLException
     *             if there is an error
     */
    private static URL urlCategories() throws MalformedURLException {
        return new URL(SWGCraft.getBaseURL() + SWGCraft.getCategoriesPath());
    }
	
	/**
     * Helper method which returns an URL for the servers XML file at
     * SWGCraft.org.
     * 
     * @return a servers URL
     * @throws MalformedURLException
     *             if there is an error
     */
    private static URL urlServers() throws MalformedURLException {
        return new URL(SWGCraft.getBaseURL() + SWGCraft.getServersPath());
     }

    /**
     * Helper method which returns an URL for the professions XML file at
     * SWGCraft.org.
     * 
     * @return a professions URL
     * @throws MalformedURLException
     *             if there is an error
     */
    private static URL urlProfLevels() throws MalformedURLException {
        return new URL(SWGCraft.getBaseURL() + SWGCraft.getProfLevelsPath());
    }

    /**
     * Helper method which returns an URL for the schematics XML file at
     * SWGCraft.org.
     * 
     * @return a schematics URL
     * @throws MalformedURLException
     *             if there is an error
     */
    private static URL urlSchematics() throws MalformedURLException {
        return new URL(SWGCraft.getBaseURL() + SWGCraft.getSchematicsPath());
    }

    /**
     * This type denotes an update to the local cache. The field {@link #type}
     * denotes the updated file.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public static class CacheUpdate implements UpdateNotification {

        /**
         * A constant which identifies the updated data blob.
         */
        public final UpdateType type;

        /**
         * Creates an instance of this type for the specified type of update.
         * 
         * @param type
         *            the type of update this notification pertains to
         * @throws NullPointerException
         *             if the argument is {@code null}
         */
        public CacheUpdate(UpdateType type) {
            if (type == null)
                throw new NullPointerException("Argument is null");

            this.type = type;
        }

        /**
         * A constant which identifies the updated data blob.
         */
        public enum UpdateType {

            // Enum can be extended if necessary

            /**
             * Denotes that the notification pertains to categories.
             */
            CATEGORIES,

            /**
             * Denotes that the notification pertains to profession levels.
             */
            PROF_LEVELS,

            /**
             * Denotes that the notification pertains to schematics.
             */
            SCHEMATICS,
            
            /**
             * Denotes that the notification pertains to servers.
             */
            SERVERS;
        }
    }
}
