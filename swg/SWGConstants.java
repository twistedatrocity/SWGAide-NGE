package swg;

import java.io.File;

/**
 * This class is a container of global static constants of various kinds.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGConstants {

    /*
     * This class is a collection of more or less static data, data that do not
     * fit naturally into any other class -- for convenience, comfort, and
     * all-at-one-place.
     */

    /**
     * The backup directory for SWGAide
     */
    public static String backupDir = "backups";

    /**
     * Eventual cache directory. Normally this variable is {@code null} and the
     * cache is dynamically achieved.
     * 
     * @see SWGConstants#getCacheDirectory()
     * @see SWGConstants#setCacheDirectory(File)
     */
    private static File cacheDir = null;

    /**
     * An array of bytes for misc usage.
     */
    public static final byte[] cKey = "Yoda43v3r".getBytes();

    /**
     * A flag which denotes if developer debugging is turned on, that is, all
     * error and exception messages are written to System.err for easier
     * readability and traceability while developing in an IDE, etc. Otherwise
     * all such output to file as for a normal user.
     */
    // DEVELOPER_NOTE: set this flag true while developing, to get the output
    // from stdout and stderr to the console in Eclipse rather than to file.
    public static final boolean DEV_DEBUG = false;

    /**
     * The relative path to crafting related files within the SWGAide directory.
     * Temporary, crafting related files should go to the "temp" folder.
     */
    public static final String repositoryPath = "crafting";

    /**
     * The URL to the SWGAide home and download page.
     */
    public static final String swgAideURL = "https://github.com/twistedatrocity/SWGAide-NGE";
    
    /**
     * The URL to the SWGAide releases page.
     */
    public static final String swgAideReleaseURL = "https://github.com/twistedatrocity/SWGAide-NGE/releases";
    
    /**
     * The URL to the SWGAide version file for update checking.
     */
    public static final String swgAideVersionURL = "https://raw.githubusercontent.com/twistedatrocity/SWGAide-NGE/master/SWGAide-version.TXT";

    /**
     * The text file containing version information.
     */
    public static final String swgVersionTXT = "SWGAide-version.TXT";

    /**
     * The version number of the SWGAide that is available for download.
     */
    public static final String version = "0.9.9-MrMiagi-0.1.17";

    /**
     * Returns the cache directory.
     * <p>
     * The cache directory is normally a sub-directory under the system
     * dependent directory, %TEMPPATH%\SWGAide or C:\TEMP\SWGAide.
     * 
     * @return SWGAide's cache directory within the system default TEMP
     *         directory, or a directory set by the user
     * @see SWGConstants#setCacheDirectory(File)
     */
    public static File getCacheDirectory() {
        if (cacheDir == null) {
            File tmp;
            String str = System.getProperty("java.io.tmpdir");
            tmp = str != null
                    ? new File(str, "SWGAide-NGE-" + version)
                    : null;
            if (tmp == null || (!tmp.exists() && !tmp.mkdir()))
                tmp = new File("temp");
            cacheDir = tmp;
        }
        return cacheDir;
    }

    /**
     * Sets the cache directory. This will override the default system cache
     * directory.
     * 
     * @param target
     *            the directory to use as cache directory
     */
    public static void setCacheDirectory(File target) {
        if (target == null)
            return;
        cacheDir = target;
    }
}
