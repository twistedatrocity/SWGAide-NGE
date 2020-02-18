package swg.model;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import swg.SWGAide;
import swg.gui.common.SWGGui;

/**
 * This type represents one galaxy within a station. Such a station-specific
 * galaxy is the home for one, two, or three characters; only Elder Jedi may
 * have an unlocked, third character slot.
 * <p>
 * However, because players can delete and create a new character this type does
 * not limit the number of characters. Remnants of deleted characters are still
 * present at the local hard disk and users may want to retain old data, such as
 * mails.
 * <p>
 * Players can have multiple stations (accounts) which makes it for have several
 * characters at the same galaxy, these are contained in other galaxy instances
 * for those stations. This type reflects the galaxy part of the file system
 * within the SWG client directory and in SWGAide. However, GUI applications may
 * present the user with a logical view of the game.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGGalaxy implements
        Serializable, Comparable<SWGGalaxy>, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -1290886906079544881L;

    /**
     * A list of characters who populate this galaxy.
     */
    private final List<SWGCharacter> characters;

    /**
     * The galaxy constant for this galaxy.
     */
    private final SWGCGalaxy galaxy;

    /**
     * The station that contains this galaxy.
     */
    private final SWGStation station;

    /**
     * Creates an empty instance of a galaxy.
     * 
     * @param station the station for this instance
     * @param galaxyName the name for this galaxy
     * @throws IllegalArgumentException if an argument is invalid
     */
    private SWGGalaxy(SWGStation station, String galaxyName)
            throws IllegalArgumentException {

        this.station = station;
        this.galaxy = SWGCGalaxy.fromName(galaxyName);

        if (!SWGCGalaxy.isNameValid(galaxyName))
            throwException(new IllegalArgumentException(
                    "Invalid galaxy name: " + galaxyName));
        if (!exists())
            throwException(new IllegalArgumentException(
                    "Incorrect file path: " + swgPath()));

        characters = new ArrayList<SWGCharacter>();
    }

    /**
     * Adds the specified character to this instance. If the character is deemed
     * as invalid for this galaxy ({@link #isLikely(SWGCharacter)}), or if it is
     * already listed, this method does nothing.
     * 
     * @param character a new character
     * @return {@code true} (as specified by {@link Collection#add(Object)})
     */
    public boolean characterAdd(SWGCharacter character) {
        synchronized (characters) {
            if (isLikely(character) && !characters.contains(character))
                return characters.add(character);

            return false;
        }
    }

    /**
     * Determines if a character with the specified name exists in this
     * instance. The the boolean argument determines if the full name of a
     * character is tested for, otherwise only its first name is tested for.
     * 
     * @param name a character name
     * @param matchFullName {@code false} to test only the first name
     * @return {@code true} if a matching character is found
     */
    boolean characterExists(String name, boolean matchFullName) {
        synchronized (characters) {
            for (SWGCharacter ch : characters) {
                if (matchFullName)
                    return ch.getNameComplete().equals(name);
                return ch.getName().equals(name);
            }
            return false;
        }
    }

    /**
     * Removes the specified character from this galaxy. This method does not
     * touch the file system so remnants after the character may exist in either
     * the SWGAide file tree, or in the SWG profiles folder.
     * 
     * @param character a character
     * @return {@code true} if this galaxy contained the specified character
     */
    boolean characterRemove(SWGCharacter character) {
        synchronized (characters) {
            return characters.remove(character);
        }
    }

    /**
     * Returns a list of the characters at this galaxy. The returned list may
     * contain characters who still exist at the hard disk but who the player
     * has deleted in game. The returned list is <b>read-only</b> and is not
     * thread safe.
     * 
     * @return a list of the characters at this galaxy
     */
    public List<SWGCharacter> characters() {
        return characters;
    }

    public int compareTo(SWGGalaxy o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof SWGGalaxy
                        && galaxy == ((SWGGalaxy) obj).galaxy
                        && station.equals(((SWGGalaxy) obj).station));
    }

    /**
     * Determines if this galaxy exists within the local SWG file structure.
     * Remember that this galaxy may have been deleted from SWG but the user
     * wants to retain it within SWGAide, or the DAT file is from another
     * computer at which there are no traces of this galaxy.
     * 
     * @return {@code true} if the file path for this galaxy exists
     */
    public boolean exists() {
        return swgPath().exists();
    }

    public String getDescription() {
        return galaxy.getName() + " @ " + station.getName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The name as obtained from {@link SWGCGalaxy#getName()}.
     */
    public String getName() {
        return galaxy.getName();
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The name as obtained from {@link SWGCGalaxy#getName()}.
     */
    public String getType() {
        return galaxy.getType();
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * The name as obtained from {@link SWGCGalaxy#getNameComplete()}.
     */
	public String getNameComplete() {
		return galaxy.getNameComplete();
	}

    /**
     * Returns the galaxy constant for this galaxy.
     * 
     * @return a galaxy constant
     */
    public SWGCGalaxy gxy() {
    	SWGCGalaxy gxy = SWGCGalaxy.fromID(galaxy.id());
        return gxy;
    }

    @Override
    public int hashCode() {
        return galaxy.hashCode() + station.hashCode();
    }

    /**
     * Helper method which determines if the specified character may be a
     * citizen at this galaxy. This method returns {@code false} if the
     * character's galaxy path does not match this galaxy.
     * 
     * @param character the character to investigate
     * @return {@code true} if {@code character} has its file path in this
     *         galaxy, {@code false} otherwise
     */
    private boolean isLikely(SWGCharacter character) {
        return swgAidePath().equals(character.galaxy().swgAidePath());
    }

    /**
     * Scans for and returns a list of SWGAide mailbox names, or an empty list.
     * This method scans this galaxy and each mailbox with a folder name that
     * denotes a character who is unknown at this galaxy is added to the
     * returned list. A mailbox is a folder which contains at least one file
     * with a file name that ends in ".mail" or "auct". There could be "dead"
     * and "isd" too but... The name of the mailbox denotes the full name of a
     * character.
     * 
     * @return a list of mailbox names, or an empty list
     */
    List<String> scanForLostMailboxes() {
        File mailPath = swgAidePath();
        if (mailPath.exists()) {
            File[] dirs = mailPath.listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() && f.list().length > 0;
                }
            });
            if (dirs == null || dirs.length <= 0)
                return Collections.emptyList();

            ArrayList<String> mDirs = new ArrayList<String>();
            for (File dir : dirs) {
                if (characterExists(dir.getName(), true))
                    continue; // a known character

                File[] mails = dir.listFiles(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isFile()
                                && (f.getName().endsWith(".mail")
                                || f.getName().endsWith(".auct"));
                    }
                });
                if (mails != null && mails.length > 0)
                    mDirs.add(dir.getName());
            }
            return mDirs;
        }
        return Collections.emptyList();
    }

    /**
     * Returns the station that contains this galaxy.
     * 
     * @return returns the station for this galaxy
     */
    public SWGStation station() {
        return station;
    }

    /**
     * Returns the file path within SWGAide for this galaxy. The returned path
     * is relative SWGAide at the local file system and in the "mails" folder.
     * 
     * @return a path for this galaxy
     */
    public File swgAidePath() {
        return new File(station.swgAidePath(), galaxy.getNameComplete());
    }

    /**
     * Returns the session scope file path for this galaxy. The returned path is
     * absolute and refers to the SWG installation at the current file system.
     * 
     * @return a path for this galaxy
     */
    public File swgPath() {
        return new File(station.swgPath(), galaxy.getNameComplete());
    }

    /**
     * Helper method which prints the error to SWGAide's log file and the throws
     * the specified exception.
     * 
     * @param e an exception
     */
    private void throwException(RuntimeException e) {
        SWGAide.printError("SWGGalaxy:SWGGalaxy", e);
        throw e;
    }

    @Override
    public String toString() {
        return String.format("SWGGalaxy[%s]", getDescription());
    }

    /**
     * Returns a list of character first names for the specified galaxy. This
     * method scans the specified universe for all instances of the galaxy and
     * their inhabitants. This means, the list contains character names from
     * several stations but they all inhabit the same galaxy. Because a first
     * name is unique per galaxy the list contains only unique names.
     * 
     * @param u the universe to scan
     * @param gxy a galaxy constant
     * @return a list of unique character first names
     */
    public static List<String> characterNames(SWGUniverse u, SWGCGalaxy gxy) {
        List<String> chars = new ArrayList<String>();
        for (SWGStation st : u.stations()) {
            SWGGalaxy g = st.galaxy(gxy);
            if (g != null)
                for (SWGCharacter ch : g.characters())
                    chars.add(ch.getName());
        }
        return chars;
    }

    /**
     * Scans for new galaxies in the specified station. For each new galaxy this
     * method find it creates an empty instance and adds it the list; no
     * characters are added to the instance and it is not added to the station.
     * Otherwise an empty list is returned.
     * 
     * @param station a station
     * @return a list of new galaxies, or an empty list
     */
    public static List<SWGGalaxy> scanForNewGalaxies(SWGStation station) {
        File[] dirs = station.swgPath().listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        if (dirs == null)
            return Collections.emptyList();

        ArrayList<SWGGalaxy> news = new ArrayList<SWGGalaxy>();
        for (File d : dirs) {
            String dn = d.getName();
            if (SWGCGalaxy.isNameValid(dn) && !station.galaxyExists(dn)) {
                // add only new galaxies to this station
                // only valid galaxy names
                SWGGalaxy g = new SWGGalaxy(station, dn);
                news.add(g);
            }
        }

        return news;
    }

	public SWGUniverse universe() {
		return station().universe();
	}
}
