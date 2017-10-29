package swg.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import swg.gui.common.SWGGui;
import swg.model.SWGAliases.SWGAliasesList;
import swg.model.images.SWGImageAlbum;
import swg.tools.SearchFiles;

/**
 * This type represents the topmost element of a SWG universe, either SWG or TC.
 * This type models a client and its file and directory structure from a file
 * structure point of view. The optional "test-center" universe has an identical
 * structure though different galaxy names. "TC" is instantiated only if a user
 * also plays at "test-center". An instance of this type contains one or several
 * stations (user accounts), a screen-shot album, and a list of aliases files.
 * <p>
 * To make this type and its sub-types host independent the SWG file path must
 * be adjusted very early on, before any GUI stuff is rendered.
 * <p>
 * The following depicts the StarWarsGalaxies' file-and-directory structure:
 * <blockquote>The left-hand side outlines the relation between the different
 * Java classes for a universe. The signs-and-numbers at the horizontal lines
 * denotes the possible number of objects for that particular structure (notice
 * some exceptions commented in the classes respectively): <br/>
 * &nbsp; <tt>+</tt> denotes one or several objects <br/>
 * &nbsp; <tt>*</tt> denotes zero or several <br/>
 * &nbsp; <tt><i>n</i></tt> denotes a specified number, or an inclusive range
 * <p>
 * The right-hand side texts within parentheses are the relative file paths
 * within the current SWG client folder. Some folders or files does not always
 * exist, either at the current host or not at all for the player. All file
 * paths are relative within their context, either within the SWG client folder
 * or within SWGAide. The SWG client path is absolute for the session, but may
 * be different between sessions; based on the SWG path all other paths are
 * created, even though they do not always exist. </blockquote>
 * 
 * <pre>SWGUniverse { StarWarsGalaxies\ or StarWarsGalaxies\testcenter\ }
 *  |
 *  |-+- SWGAliases { .\ }
 *  |-1- SWGImageAlbum { .\screenshots\ }
 *  |     |
 *  |     |-*- SWGImage
 *  |     |-1- SWGImageAlbum 
 *  |           |
 *  |           |-*- SWGImage
 *  |
 *  |-+- SWGStation { .\profiles\MyStationName\ }
 *  |     |
 *  |     |-*- SWGNotes ( .\ )
 *  |     |-1- SWGMacros ( .\ )
 *  |     |
 *  |     |-*- SWGGalaxy { .\SomeGalaxy\ }
 *  |     |     |
 *  |     |     |-[1 3]- SWGCharacter
 *  |     |     |         |
 *  |     |     |         |-1- SWGMailBox { .\mail_CharacterName\ }
 *  |     |     |         |     |
 *  |     |     |         |     |-+- SWGMailFolder
 *  |     |     |         |     |     |
 *  |     |     |         |     |     |-*- SWGMailFolder # optional sub-folders
 *  |     |     |         |     |     |-+- mails
 *  |     |     |         |     |     |
 * </pre>
 * <p>
 * A player who subscribes for several accounts usually creates several
 * characters at the same galaxy. This is modeled by this data structure in the
 * same fashion as at the local hard disk: a universe which contains several
 * stations of which each contains galaxies which contain the characters for
 * that station; this is the physical representation. SWGAide's GUI displays
 * both this view and logical views, determined by the context.
 * <p>
 * All mutating methods, and those that so require, are synchronized on the
 * smallest possible scope.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGUniverse implements Serializable, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 346833251978240872L;

    /**
     * An album for screen shots and images.
     */
    private final SWGImageAlbum album;

    /**
     * A list of aliases files.
     */
    private final SWGAliasesList aliases;

    /**
     * An abstract, absolute file path for the SWG client, a directory. This may
     * be {@code "C:\Program Files\StarWarsGalaxies"} but may be any path at the
     * current host. This member is verified and updated when SWGAide launches:
     * if the stored path does not exist the SWGAide.INI file is queried for the
     * local path or the user is prompted a dialog for the path.
     */
    private File clientDir;

    /**
     * The name assigned to this universe.
     */
    private final String name;

    /**
     * A map of the stations for this universe. A station that is listed here
     * has at one point been found by SWGAide at a file system, it may be
     * subscribed or inactive. At another host one or several stations may not
     * exist locally, they are retained in SWGAide's DAT file; the user must
     * manually purge obsolete stations.
     */
    private final Map<String, SWGStation> stations;

    /**
     * Creates an instance of this type for the specified arguments. This
     * constructor sets the name to "SWG" or "TC" determined by the boolean
     * value. It also initiates the containers for album, aliases, and stations.
     * 
     * @param swgDir the absolute file path for the current SWG client folder
     * @param isTC {@code true} if this denotes "test-center"
     * @throws Throwable if the path is invalid, see {@link #swgPath(File)}
     */
    public SWGUniverse(File swgDir, boolean isTC) throws Throwable {
        this.name = isTC
                ? "TC"
                : "SWG";
        swgPath(swgDir);

        this.album = new SWGImageAlbum(this);
        this.aliases = new SWGAliasesList();
        this.stations = new HashMap<String, SWGStation>();
    }

    /**
     * Returns a list of the aliases files for this universe, or an empty list.
     * The list may be modified but is the list that is stored in the DAT file.
     * 
     * @return a list of aliases
     */
    public List<SWGAliases> aliases() {
        return aliases;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof SWGUniverse
                    && clientDir.equals(((SWGUniverse) obj).clientDir));
    }

    /**
     * Returns {@code true} if it exists a valid path to the SWG client at this
     * host. That is, if SWGAide is launched and cannot find a path and the user
     * decides to carry on anyway, then this method returns {@code false}.
     * 
     * @return {@code true} if {@link #swgPath()} returns a valid path
     */
    public boolean exists() {
        return swgPath().exists();
    }

    public String getDescription() {
        return swgPath().getAbsolutePath();
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return swgPath().hashCode();
    }

    /**
     * Returns the album of screen shots and images.
     * 
     * @return the album
     */
    public SWGImageAlbum imageAlbum() {
        return album;
    }

    /**
     * Returns a station for the specified name, or {@code null}. If no station
     * exists with a matching name this method returns {@code null}.
     * 
     * @param stationName the proper name for a station
     * @return a station with the specified name, or {@code null}
     */
    public SWGStation station(String stationName) {
        synchronized (stations) {
            return stations.get(stationName);
        }
    }

    /**
     * Adds the specified station to this universe. If this universe already
     * contains the station this method does nothing.
     * 
     * @param station a station to add
     * @return returns {@code true} if the station was added successfully
     */
    public boolean stationAdd(SWGStation station) {
        synchronized (stations) {
            if (stations.containsKey(station.getName())) return false;

            stations.put(station.getName(), station);
            return true;
        }
    }

    /**
     * Determines if this universe contains a station for the specified name.
     * 
     * @param stationName the name to search for
     * @return {@code true} if a station with that name exists
     */
    public boolean stationExists(String stationName) {
        synchronized (stations) {
            return stations.containsKey(stationName);
        }
    }

    /**
     * Returns an unsorted list of names of stations which pertain to this
     * universe, or an empty list. The list may be modified.
     * 
     * @return a list of station names
     */
    public List<String> stationNames() {
        synchronized (stations) {
            return new ArrayList<String>(stations.keySet());
        }
    }

    /**
     * Removes and returns a station with the specified name. This method
     * returns {@code null} if no such station exists in this universe. This
     * method just affects the DAT file, not the file system.
     * 
     * @param stationName the proper name for the station to remove
     * @return the removed station, or {@code null}
     */
    public SWGStation stationRemove(String stationName) {
        synchronized (stations) {
            return stations.remove(stationName);
        }
    }

    /**
     * Returns a list of all stations which pertain to this universe, or an
     * empty list. The list may be modified.
     * 
     * @return a list of stations, or an empty list
     */
    public List<SWGStation> stations() {
        synchronized (stations) {
            return new ArrayList<SWGStation>(stations.values());
        }
    }

    /**
     * Returns the absolute file path for the SWG client folder at the current
     * file system, or an invalid path from a previous session. This is the
     * session scope path, at another host the path may be different than what
     * it was the previous session. If this path <b>is not</b> set early during
     * the launch of SWGAide the return value is the previous path. In the
     * latter case {@link #exists()} returns {@code false} and clients must not
     * make use of this invalid path; only features that work without a SWG
     * client will function.
     * 
     * @return the absolute path to the SWG client folder at the current system,
     *         or an invalid path
     */
    public File swgPath() {
        return clientDir;
    }

    /**
     * Sets the abstract, absolute path for SWG client folder for this station.
     * This is the path that is used at this computer for this session. This
     * method does nothing if the argument equals {@link #clientDir}, otherwise
     * it validates the argument and throws an exception for any error.
     * <p>
     * When SWGAide or its DAT file is moved to another computer
     * {@link #clientDir} must be updated to refer to the current location of
     * SWG. This <i>must be done early </i> during launch, before any scanning
     * begins and before the GUI is rendered, because all related classes base
     * their file paths on this value to make SWGAide and its DAT file host
     * independent.
     * 
     * @param dir an absolute path for the current SWG client folder
     * @throws IllegalArgumentException if the argument is not a valid directory
     *         or if {@link #isValidSWGPath(File)} returns {@code false}
     * @throws NullPointerException if the argument is {@code null}
     * @throws Exception if there is an error
     */
    public void swgPath(File dir) throws Exception {
        synchronized (name) {
            if (dir.equals(clientDir)) return;

            String err = null;
            if (!dir.exists())
                err = "Does not exist";
            else if (!dir.isDirectory())
                err = "Is not a folder";
            else if (!isValidSWGPath(dir))
                err = "Missing crucial SWG files";
            else if (name.equals("TC") && !dir.getName().equals("testcenter"))
                err = "TC but not \"testcenter\"";
            if (err != null)
                throw new IllegalArgumentException(err + ": " + dir);

            clientDir = dir;
        }
    }

    @Override
    public String toString() {
        return String.format("SWGUniverse: [%s]", swgPath().getAbsolutePath());
    }

    /**
     * Returns a file path for a SWG client folder, or {@code null}. This method
     * invokes {@link SearchFiles#searchForDir(String, File)} with the arguments
     * "StarWarsGalaxies" and "C:\". This method returns {@code null} if no such
     * folder is found or if {@link #isValidSWGPath(File)} deems it invalid.
     * 
     * @return a path to a SWG client folder, or {@code null}
     */
    public static File findClientDir() {
        File clientDir = SearchFiles.searchForDir("StarWarsGalaxies", null);
        return isValidSWGPath(clientDir)
                ? clientDir
                : null;
    }

    /**
     * Determines if the specified directory contains some crucial SWG files
     * that must be present in a SWG installation. If the argument is {@code
     * null} this method returns {@code false}.
     * 
     * @param directory a directory to validate
     * @return {@code true} if the directory contains the crucial files
     */
    public static boolean isValidSWGPath(File directory) {
        return directory != null
                && new File(directory, "SwgClient_r.exe").exists();
    }
}
