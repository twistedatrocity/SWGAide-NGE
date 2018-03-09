package swg.model;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import swg.SWGAide;
import swg.gui.common.SWGGui;
import swg.model.mail.SWGMailBox;
import swg.tools.ZReader;

/**
 * This type represents one character at a specific galaxy. An instance of this
 * type contains the character's name, galaxy, mailbox, and a notes field.
 * <p>
 * A unique instance is identified by its name, galaxy, and its station. If the
 * user changes the name for the character, which is possible since 2009, a new
 * instance is created. TODO: Add support to edit the name in SWGAide to retain
 * mails and other stuff.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGCharacter implements
        Serializable, Comparable<SWGCharacter>, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 5066932948632207711L;

    /**
     * The home galaxy for this character which is station-specific.
     */
    private final SWGGalaxy homeGalaxy;

    /**
     * The mailbox of this character.
     */
    private final SWGMailBox mailBox;

    /**
     * The full name of this character. Since 2009 SWG presents the player with
     * options to edit the name in SWG.
     */
    private String name;

    /**
     * A string for miscellaneous notes.
     */
    private String noteField;

    /**
     * The profession for this character. // XXX
     */
    // private SWGProfession profession = null;

    /**
     * Creates a character with the specified name at the specified galaxy.
     * Implicitly the character is bound to a station via the galaxy object.
     * 
     * @param name the full name of the character
     * @param galaxy the station-specific galaxy for this character
     */
    private SWGCharacter(String name, SWGGalaxy galaxy) {
        this.name = name;
        homeGalaxy = galaxy;
        mailBox = new SWGMailBox(this); // must come after galaxy is set
    }

    public int compareTo(SWGCharacter o) {
        return mailBox.swgAidePath().compareTo(o.mailBox.swgAidePath());
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof SWGCharacter
                    && compareTo((SWGCharacter) other) == 0);
    }

    /**
     * Returns the home galaxy for this character. Compare {@link #gxy()}.
     * 
     * @return a galaxy
     */
    public SWGGalaxy galaxy() {
        return homeGalaxy;
    }

    public String getDescription() {
        return name + " @ " + homeGalaxy.getName() + " @ "
                + homeGalaxy.station().getName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is the first name of this character, compare with
     * {@link #getNameComplete()}.
     */
    public String getName() {
        int idx = name.indexOf(' ');
        return idx > 0
                ? name.substring(0, idx)
                : name;
    }

    /**
     * Returns the complete name of this character. This method returns the
     * first name and the surname, if there is any.
     * 
     * @return the full name of this character
     */
    public String getNameComplete() {
        return name;
    }

    /**
     * Returns a constant for the home galaxy for this character. This is a
     * convenience for {@code galaxy().gxy()}.
     * 
     * @return a galaxy constant
     */
    public SWGCGalaxy gxy() {
        return homeGalaxy.gxy();
    }

    @Override
    public int hashCode() {
        return this.mailBox.swgAidePath().hashCode();
    }

    /**
     * Returns the mailbox of this character.
     * 
     * @return a mailbox
     */
    public SWGMailBox mailBox() {
        return mailBox;
    }

    /**
     * Returns the notes for this character, or an empty string. Do not confuse
     * these notes with an in-game notes file.
     * 
     * @return the notes
     */
    public String notes() {
        return noteField == null
                ? ""
                : noteField;
    }

    // /**
    // * Returns the profession for this character, {@code null} if it is
    // not
    // * known
    // *
    // * @return the profession for this character, {@code null} if it is
    // not
    // * known
    // */
    // public SWGProfession getProfession() { XXX
    // return profession;
    // }
    // /**
    // * Sets the profession for this character
    // *
    // * @param profession
    // * the profession to set
    // */
    // public void setProfession(SWGProfession profession) {
    // this.profession = profession;
    // }

    /**
     * Appends {@code note} to the notes text, but if the argument is {@code
     * null} this method does nothing. If there is a previous content the
     * delimiter is added between the old and the new text, otherwise, if there
     * is no former content it is ignored.
     * 
     * @param note a string to append to the former content
     * @param delim a delimiter between old and new
     */
    public void notesAppend(String note, char delim) {
        if (note != null)
            noteField = noteField == null || noteField.isEmpty()
                    ? noteField = note
                    : noteField + delim + note;
    }

    /**
     * Sets the notes for this character. This method <i>replaces </i> any
     * previous content; use {@code null} or an empty string to erase.
     * 
     * @param note a text string, or {@code null}
     */
    public void notesSet(String note) {
        noteField = note == null || note.trim().isEmpty()
                ? null
                : note;
    }

    @Override
    public String toString() {
        return String.format("SWGCharacter[%s]", getDescription());
    }

    /**
     * Helper method which determines if the first word of the two specified
     * strings equal each other. A word is up to but not including the first
     * white space of a strings or to its end no white space exists. The two
     * words are compared using {@link String#compareTo(String)}.
     * 
     * @param a a trimmed string
     * @param b a trimmed string
     * @return {@code true} if the first word of each string equal each other
     */
    private static boolean firstWordsEqual(String a, String b) {
        if (a == b) return true;

        String aw = a.split(" ", 1)[0];
        String bw = b.split(" ", 1)[0];
        return aw.compareTo(bw) == 0;
    }

    /**
     * Helper method which returns a list of all character names which are found
     * in a file {@code characterlist_<some-station>.txt}, or an empty list. The
     * specified galaxy determines which station's file to scan and only the
     * full names of characters who refer to that galaxy are added to the
     * returned list.
     * <p>
     * This method makes it possible to display in SWGAide any character who
     * exists in game but yet have no mails saved to the local file system,
     * otherwise they would be invisible.
     * <p>
     * Notice that there is a bug in SWG's login system which can make this file
     * read the name for a no longer existing character. If such a character's
     * first name equals the first name of a character that exists at the same
     * galaxy in the same station the existing character must be given
     * precedence. See the bug notes in {@link #scanForNewCharacters(SWGGalaxy)}.
     * 
     * @param galaxy the galaxy to scan
     * @return a list of complete character names, or an empty list
     */
    private static List<String> scanCharacterListFile(SWGGalaxy galaxy) {
        File f = galaxy.station().characterlistFile();
        if (f.exists()) {
            // Example of a line in the file:
            // Europe-Chimaera,Zimoon Hartnoord (Europe-Chimaera),0,8
            // group1: galaxy-name, group2: character-name, group3: remainder
            Pattern re = Pattern.compile(
                    "(\\w+-?\\w*),(.+)\\s(\\(.+\\),\\d+,\\d+)");

            ZReader sr = ZReader.newTextReader(f);
            if (sr != null) {
                List<String> characterNames = new ArrayList<String>();
                List<String> sl = sr.lines(false, false);
                for (String line : sl) {
                    Matcher mr = re.matcher(line);
                    if (mr.find() && mr.group(1).equals(galaxy.getName()))
                            characterNames.add(mr.group(2));
                }
                for (String line : sl) {
                    Matcher mr = re.matcher(line);
                    if (mr.find() && mr.group(1).equals(galaxy.getNameComplete()))
                            characterNames.add(mr.group(2));
                }
                sr.close();
                /* SWGAide.printDebug("char", 1,
                        "Testing HIT " + characterNames);*/
                return characterNames;
            }
        } // else
        return Collections.emptyList();
    }

    /**
     * Helper method which scans the specified galaxy and returns a list of
     * character names who have mail folders, or an empty list. This method,
     * within SWG's file system it scans the specified galaxy for mail folders,
     * from which character names are parsed and added to the returned list. A
     * mail-folder has a name on the syntax {@code "mail_Name Familyname"},
     * however the mail-part is shaved off and only the full name of the
     * character added to the list.
     * <p>
     * Notice: if the player has never issued the command /mailsave for an
     * existing character no mail folder exists for that character and this
     * method cannot report that character name.
     * 
     * @param galaxy the galaxy to scan
     * @return a list of complete character names, or an empty list
     */
    private static List<String> scanForMailFolders(SWGGalaxy galaxy) {
        File[] dirs = galaxy.swgPath().listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory()
                        && pathname.getName().startsWith("mail_");
            }
        });
        if (dirs == null)
            return Collections.emptyList();

        ArrayList<String> characterNames = new ArrayList<String>();
        for (File dir : dirs)
            characterNames.add(dir.getName().substring(5)); // skip mail_

        return characterNames;
    }

    /**
     * Scans the specified galaxy and returns a list of new characters, or an
     * empty list. More specifically, within SWG's "profiles" folder this
     * implementation scans the specified galaxy for mailboxes which denotes
     * existing characters. Also SWGAide's "mails" folder is scanned for remnant
     * mailboxes, plus that the file {@code characterlist_<some-station>.txt} is
     * scanned for characters at the specified galaxy. The content of these
     * three collections are compared with SWGAide's knowledge about the
     * specified galaxy -- see note below on a bug in SWG's login system.
     * <p>
     * A character which is not known by SWGAide but who has a mail-folder in
     * SWG's file system, or who has an remnant mailbox within SWGAide's mails
     * folder, or who is found in the character-list file, that character is
     * added to the returned list. If a SWGAide mail-folder exists for a
     * character with a first name that is found in the character-list file but
     * the family names do not equal, the character with the mail-folder or
     * mailbox is given higher precedence, see the bug notes. This method prints
     * a message to SWGAide's log file if a known character exists who in a
     * similar way clashes with a bugged character name.
     * <p>
     * Furthermore, if it is established that a known character's mail folder
     * within SWG does not exist anymore and neither is this character found in
     * the character-list file, then, only if it is also established that for
     * this character no mailbox exists within SWGAide's mail folder, then the
     * character is removed from SWGAide's DAT file. Just the DAT file is
     * affected by this.
     * <p>
     * <b>The character-list file:</b> The reason to scan and pick up characters
     * listed in the character-list file is that this is the only way to present
     * the user with a complete list of characters for that station and galaxy.
     * It is only if the player issues the /mailsave command that mail-folders
     * are created at the local hard-disk.
     * <p>
     * <b>Bug in SWG:</b> In December 2009 Belisama reported that a SOE GM had
     * renamed a character for her and after that SWGAide could not pick up the
     * mails from the character with the new name. It turned out that this
     * happens if a character is renamed only by the full name and not the first
     * name, from for example "Beli Old" to "Beli New". The new name is used
     * in-game and mails are saved to the a mail-folder with the new name,
     * however, the old name is used by the SWG login-system which at each login
     * wrongly updates the character-list file with the old name.
     * <p>
     * The logic of this method is changed so such an inconsistency is noted in
     * SWGAide's log file but the character with a mail-folder is used rather
     * than the bugged character who is read in the character-list.
     * 
     * @param galaxy the galaxy to scan
     * @return a list of new characters, or an empty list
     */
    public static List<SWGCharacter> scanForNewCharacters(SWGGalaxy galaxy) {

    	List<SWGCharacter> knownCharacters = galaxy.characters();
        List<String> lostBoxes = galaxy.scanForLostMailboxes();
        List<String> charFromFile = scanCharacterListFile(galaxy);
        List<String> mailFolders = scanForMailFolders(galaxy);

        // first remove characters from SWGAide's DAT file, but
        // to be removed a character must...
        // -- not have a mailbox within SWGAide's file system
        // -- not have a mail-folder within SWG's file system
        // -- not be listed in the character-list file
        for (int i = knownCharacters.size() - 1; i >= 0; --i) {
            SWGCharacter toon = knownCharacters.get(i);
            if (toon.mailBox().swgAidePath().exists()
                    || mailFolders.contains(toon.getNameComplete())
                    || charFromFile.contains(toon.getNameComplete()))
                continue;
            // else
            galaxy.characterRemove(toon);
        }

        // determine if there are bugged ghosts, and if so...
        // remove them from the char-from-file list
        for (int i = charFromFile.size() - 1; i >= 0; --i) {
            String clf = charFromFile.get(i);
            if (galaxy.characterExists(clf, true)
                    || mailFolders.contains(clf)
                    || lostBoxes.contains(clf))
                continue; // clf is definitely OK, exact name match is found

            int c = clf.indexOf(' ');
            if (c > 0) {
                // there is a first name, which is a prerequisite for the bug
                String err = null;

                if (galaxy.characterExists(clf.substring(0, c), false)) {
                    err = "SWGCharacter:scanForNewChars: SWG bug detected\n"
                                    + "\t\"" + clf
                                    + "\" clashes with known character at "
                                    + galaxy.toString();
                } else {
                    for (String lb : lostBoxes) {
                        if (firstWordsEqual(clf, lb)) {
                            err = "SWGCharacter:scanForNewChars: SWG bug "
                                    + "detected\n\t\"" + clf
                                    + "\" clashes with mailbox "
                                    + galaxy.toString() + '\\' + lb;
                            break;
                        }
                    }
                }
                if (err == null) {
                    for (String mf : mailFolders) {
                        if (firstWordsEqual(clf, mf)) {
                            err = "SWGCharacter:scanForNewChars: SWG bug "
                                    + "detected\n\t\"" + clf
                                    + "\" clashes with "
                                    + galaxy.swgPath() + "\\mail_" + mf;
                            break;
                        }
                    }
                }
                if (err != null) {
                    SWGAide.printDebug("char", 1, err);
                    charFromFile.remove(i);
                }
            }
        }

        // this far we have no bugged ghost in our collections, at least none
        // without a mail-folder or a mailbox, but we may have several "equal"
        // elements in the many collections
        ArrayList<String> fullNames = new ArrayList<String>(mailFolders);
        for (String b : lostBoxes)
            if (!fullNames.contains(b))
                fullNames.add(b);
        for (String f : charFromFile)
            if (!fullNames.contains(f))
                fullNames.add(f);

        // finally we remove known characters from the list
        for (SWGCharacter ch : knownCharacters)
            fullNames.remove(ch.getNameComplete());

        ArrayList<SWGCharacter> characters = new ArrayList<SWGCharacter>();
        for (String name : fullNames)
            characters.add(new SWGCharacter(name, galaxy));

        return characters;
    }
}
