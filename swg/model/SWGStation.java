package swg.model;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import swg.SWGAide;
import swg.SWGConstants;
import swg.gui.common.SWGGui;

/**
 * This type represents a station in SWG, that is a player's account. A player
 * can have several accounts and play them all in parallel, at one or several
 * computers, and they can be subscribed or inactive. Each station may contain
 * several galaxies. This type contains station specific data such as galaxies,
 * notes files and macros.
 * <p>
 * SWGAide finds any station within the current SWG client folder, whether
 * active or inactive. It is not possible to determine the activity status for a
 * station other than by having a look at the most recent date somebody logged
 * into the station.
 * <p>
 * All mutating methods, and those that so require, are synchronized on the
 * smallest possible scope.
 * <p>
 * <b>Notice:</b> If the same DAT file is moved between several computers it is
 * possible that SWGAide does not always find all stations that are listed in
 * the DAT file; this is if the player has not played all stations at the
 * current computer. Hence SWGAide no more removes stations that are not found
 * at the current computer but they must be removed by the user.
 * <p>
 * Notice that a particular galaxy may exist in several stations, though with
 * different characters. This type models the SWG hierarchy but a GUI may
 * display a logical layout, or not.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGStation implements
        Serializable, Comparable<SWGStation>, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -8922425150131425175L;

    /**
     * A map of galaxies for this station.
     */
    private final Map<String, SWGGalaxy> galaxies;

    /**
     * The macros instance that pertains to this station.
     */
    private transient SWGMacros macros;

    /**
     * The name of this station.
     */
    private final String name;

    /**
     * A list of notes files. These notes files pertains to this instance but
     * not notes files that are located outside this instance.
     */
    private final List<SWGNotes> notesList;

    /**
     * The universe that this instance pertains to.
     */
    private final SWGUniverse universe;

    /**
     * Helper member that reads version info that is used for possible upgrading
     * during deserialization.
     */
    private String version;

    /**
     * Creates an instance of this type.
     * 
     * @param univ the universe for this station
     * @param name the name of this station
     * @throws NullPointerException if an argument is {@code null}
     */
    private SWGStation(SWGUniverse univ, String name) {
        if (univ == null || name == null) {
            NullPointerException e = new NullPointerException(String.format(
                    "SWGStation: univ=%s stn=%s", univ, name));
            SWGAide.printError("SWGStation:SWGStation", e);
            throw e;
        }

        this.universe = univ;
        this.name = name;

        this.galaxies = new HashMap<String, SWGGalaxy>();
        this.notesList = new ArrayList<SWGNotes>();
    }

    /**
     * Returns the character-list file for this station. This is a file named
     * "characterlist_STATION_NAME.txt" which is located in the SWG client
     * folder; the file may not exist at the current host.
     * 
     * @return a character-list file
     */
    File characterlist() {
        return new File(universe.swgPath(),
                String.format("characterlist_%s.txt", name));
    }

    public int compareTo(SWGStation o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof SWGStation
                        && name.equals(((SWGStation) obj).name)
                        && universe.equals(((SWGStation) obj).universe));
    }

    /**
     * Determines if this station exists at the current file system. For the
     * current file path this * method is defined by {@link File#exists()}.
     * 
     * @return {@code true} if this station exists at the current file system
     */
    public boolean exists() {
        return this.swgPath().exists();
    }

    /**
     * Returns a copy of the list of galaxies which pertain to this station.
     * 
     * @return a copied list of galaxies
     */
    public List<SWGGalaxy> galaxies() {
        synchronized (galaxies) {
            return new ArrayList<SWGGalaxy>(galaxies.values());
        }
    }

    /**
     * Returns a galaxy for the specified galaxy constant, or {@code null} if it
     * does not exist.
     * 
     * @param gxy a galaxy constant
     * @return a galaxy, or {@code null}
     */
    SWGGalaxy galaxy(SWGCGalaxy gxy) {
        synchronized (galaxies) {
            return galaxies.get(gxy.getName());
        }
    }

    /**
     * Adds the specified galaxy to this station. If the galaxy already exists,
     * or if the argument is {@code null}, this method does nothing. This method
     * should only be invoked by SWGAide's initializer or refresher.
     * 
     * @param galaxy the galaxy to add
     */
    public void galaxyAdd(SWGGalaxy galaxy) {
        synchronized (galaxies) {
            if (galaxy != null && !galaxies.containsKey(galaxy.getName()))
                galaxies.put(galaxy.getName(), galaxy);
        }
    }

    /**
     * Determines if a galaxy with the specified name exists at this station.
     * The name may be the shorter name from {@link SWGCGalaxy#getName()} or its
     * full name.
     * 
     * @param galaxyName a galaxy name
     * @return {@code true} if a galaxy exists for the name
     */
    boolean galaxyExists(String galaxyName) {
        synchronized (galaxies) {
            SWGCGalaxy g = SWGCGalaxy.fromName(galaxyName);
            return galaxies.containsKey(g.getName());
        }
    }

    /**
     * Removes the specified galaxy from this station and returns it. If this
     * station does not contain the specified galaxy, {@code null} is returned.
     * This method only affects SWGAide's DAT file, not the current file system.
     * 
     * @param galaxy the galaxy to be removed
     * @return the removed galaxy, or {@code null}
     */
    public SWGGalaxy galaxyRemove(SWGGalaxy galaxy) {
        synchronized (galaxies) {
            return (galaxy.equals(galaxies.get(galaxy.getName())))
                    ? galaxies.remove(galaxy.getName())
                    : null;
        }
    }

    public String getDescription() {
        return String.format("%s @ %s", name, universe.getName());
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + universe.hashCode();
    }

    /**
     * Returns the login date for this station, or 0. If the file does not exist
     * at the current computer, or if there is an error, this method returns 0.
     * The return value is seconds since the epoch, Jan 1, 1970, 00:00:00 GMT.
     * <p>
     * In particular, this method returns the last modification date for the
     * character-list file at the current host that pertains to this instance.
     * This file is updated when somebody logs into a station.
     * 
     * @return a login date in seconds, or 0
     */
    public long loginDate() {
        try {
            return characterlist().lastModified() / 1000L;
        } catch (Throwable e) {
            SWGAide.printError("SWGStation:loginDate", e);
            return 0L;
        }
    }

    /**
     * Returns the macros instance that pertains to this station.
     * 
     * @return a macros instance
     */
    public SWGMacros macros() {
        if (macros == null) macros = new SWGMacros(this);
        return macros;
    }

    /**
     * Returns a copy of the list of notes for this station.
     * 
     * @return a list of notes
     */
    public List<SWGNotes> notes() {
        synchronized (notesList) {
            return new ArrayList<SWGNotes>(notesList);
        }
    }

    /**
     * Returns a notes instance with the specified file name, or {@code null}.
     * The specified name must be the complete file name with its suffix, not a
     * file path or the short name.
     * 
     * @param fileName a valid file name
     * @return a notes instance, or {@code null}
     */
    public SWGNotes notes(String fileName) {
        synchronized (notesList) {
            for (SWGNotes n : notesList)
                if (n.getName().equals(fileName))
                    return n;

            return null;
        }
    }

    /**
     * Adds the specified notes instance to this station. If this station
     * contains the notes this method does nothing and returns {@code false}.
     * 
     * @param notes a notes instance
     * @return {@code true} as specified by {@link Collection#add(Object)}
     */
    public boolean notesAdd(SWGNotes notes) {
        synchronized (notesList) {
            return notesList.contains(notes)
                    ? false
                    : notesList.add(notes);
        }
    }

    /**
     * Returns a list of notes instanced which are new for this station. If no
     * new notes file is found this method returns an empty list.
     * 
     * @return a list of new notes, or an empty list if none was found
     */
    public List<SWGNotes> notesNew() {
        return notesCreate(this, notesFiles(this));
    }

    /**
     * Removes the specified notes instance from this station. In particular,
     * this method does not remove the instance from the current file system,
     * just from this station. If this station does not contain the argument
     * this method does nothing.
     * 
     * @param notes a notes instance to remove
     */
    void notesRemove(SWGNotes notes) {
        if (notes == null) return;
        synchronized (notesList) {
            notesList.remove(notes);
        }
    }

    /**
     * Resolves this instance at deserialization and if necessary updates this
     * to most recent state.
     * 
     * @return this
     */
    private Object readResolve() {
        this.version = SWGConstants.version;
        return this;
    }

    /**
     * Helper method which iterates over the galaxies and normalizes their
     * mapping to values defined by {@link SWGCGalaxy#getName()}.
     */
    private void resolveGalaxies() {
        // XXX: remove i future (added 0.9.0)
        String[] nms = galaxies.keySet().toArray(new String[galaxies.size()]);
        for (String n : nms) {
            SWGGalaxy g = galaxies.get(n);
            if (!n.equals(g.getName())) {
                galaxies.remove(n);
                galaxies.remove(g.getName());
                galaxies.put(g.getName(), g);
            }
        }
    }

    /**
     * Returns the relative path to this station in SWGAide's file structure.
     * This path is {@code SWGAide\mails\ThisStation}.
     * 
     * @return the relative file path to this station within SWGAide
     */
    public File swgAidePath() {
        return new File("mails", name);
    }

    /**
     * Returns the absolute file path for this station at the current file
     * system. This is a sessions scope file path on the form {@code
     * C:\xxxx\<SWG>\<SWGUniverse>\profiles\ThisStation}.
     * 
     * @return an absolute path for this station
     */
    public File swgPath() {
        return new File(new File(universe.swgPath(), "profiles"), name);
    }

    @Override
    public String toString() {
        return "SWGStation[" + name + ']';
    }

    /**
     * Returns the universe which this station pertains to. The return value is
     * either a SWG or TC universe.
     * 
     * @return the universe for this station
     */
    public SWGUniverse universe() {
        return universe;
    }

    /**
     * Serialize this {@link SWGStation} instance. In particular, this method
     * sets {@link #stationPath} {@code = null} and then invokes
     * {@link ObjectOutputStream#defaultWriteObject()}.
     * 
     * @param oos an object output stream to write this instance to
     * @throws IOException if there is an I/O error
     * @serialData see method comment
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    /**
     * Helper method which returns a list of notes instances for the specified
     * station and file array, or an empty list. The array must contain only
     * ".txt" files. For each element of the specified array, if the station
     * does not contain such a notes file this method creates a notes instance
     * and adds it to the returned list.
     * 
     * @param s a station
     * @param files an array of files
     * @return a list of new notes instances, or an empty list
     * @throws NullPointerException if the station is null
     */
    private static List<SWGNotes> notesCreate(SWGStation s, File[] files) {
        if (files == null) return Collections.emptyList();

        ArrayList<SWGNotes> newNotes = new ArrayList<SWGNotes>();
        synchronized (s.notesList) {
            for (File f : files)
                if (s.notes(f.getName()) == null)
                    SWGNotes.getInstance(f.getName(), s, null, false);
            // TODO: this is ugly from a quick fix when working with SWGNotes
            // and side effects, just now the station NODE has nothing to add,
            // adding to tree is done by getInstance but it should rather be
            // done in a GUI-controller
        }
        return newNotes;
    }

    /**
     * Helper method which returns an array of notes files. This method scans
     * the folder of the specified station for files with a ".txt" suffix and
     * returns all of them, with the exception for {@code macros.txt}. This
     * method does not determine if the station contains the files.
     * 
     * @param s the station to scan
     * @return an array of notes files
     */
    private static File[] notesFiles(SWGStation s) {
        return s.swgPath().listFiles(new FileFilter() {
            public boolean accept(File f) {
                // accept all txt files except macros.txt
                return f.isFile() && f.getName().endsWith(".txt")
                        && !f.getName().equals("macros.txt");
            }
        });
    }

    /**
     * Helper method which adds new notes to this station, utilizing other
     * helper methods.
     * 
     * @param s a station
     */
    private static void notesManage(SWGStation s) {
        File[] files = notesFiles(s);
        if (files == null) return;

        synchronized (s.notesList) {
            List<SWGNotes> newNotes = notesCreate(s, files);
            for (SWGNotes n : newNotes)
                s.notesList.add(n);

            Collections.sort(s.notesList);
        }
    }

    /**
     * Returns a list of new stations for the specified universe. This method
     * scans for stations which not already exist and for each new station it
     * creates an instance. If none is found this method returns an empty list.
     * This method does not add a new station to the universe.
     * <p>
     * As a side effect this method also scans for new notes files and adds
     * these to the stations they pertain to; this applies to both new and old
     * stations.
     * 
     * @param univ the universe to scan
     * @return a list of new stations, or an empty list
     * @throws Exception if there is an error
     */
    public static List<SWGStation> scanForNewStations(SWGUniverse univ)
            throws Exception {

        File p = new File(univ.swgPath(), "profiles");
        if (univ.getName().equals("TC") && !p.exists())
            return Collections.emptyList();

        File[] allStations = SWGStation.scanForStations(p);
        if (allStations == null) {
            SWGAide.printError(String.format(
                    "SWGStation:scanForNewStations: Verify that %s\\" +
                            "\"profiles\" exists", univ.swgPath()), null);
            return Collections.emptyList();
        }

        ArrayList<SWGStation> newStations = new ArrayList<SWGStation>();
        for (File sd : allStations) {
            String sdn = sd.getName();
            if (!sdn.equals("Lfg")) {
                SWGStation s = univ.station(sdn);
                if (s == null) {
                    s = new SWGStation(univ, sdn);
                    newStations.add(s);
                } else
                    s.resolveGalaxies();
                notesManage(s);
            }
        }
        return newStations;
    }

    /**
     * Helper method which returns all directories in the specified path, an
     * empty array, or {@code null}. These directories are assumed to be
     * stations; this method does not validate them.
     * 
     * @param dir the SWG profiles directory
     * @return as specified by {@link File#listFiles(FileFilter)}
     * @throws Exception if the argument is not a directory named "profiles", or
     *         if there is an error
     */
    private static File[] scanForStations(File dir) throws Exception {
        if (dir.isDirectory() && dir.getName().equals("profiles"))
            return dir.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
            });

        throw new IllegalArgumentException("Does not exist: " + dir);
    }
}
