package swg.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultTreeModel;

import swg.SWGAide;
import swg.SWGConstants;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGui;
import swg.gui.tree.SWGNotesNode;
import swg.gui.tree.SWGStationNode;
import swg.gui.tree.SWGTreeNode;
import swg.tools.ZReader;
import swg.tools.ZString;
import swg.tools.ZWriter;

/**
 * This is a wrapper type for in-game notes; these are plain text files that are
 * saved at the local file system. A notes file is read and/or written by the
 * in-game notepad. This type provides methods for read, write, backup, copy,
 * etc., and they synchronize on {@link #content} for the smallest scope. None
 * of these methods execute on any background thread.
 * <p>
 * This type is host independent, its {@link #file} is either absolute or it is
 * relative a station or the SWG client. However, it does not guarantee that the
 * file of an instance exists at the current host; hence, the instance is an
 * abstraction for a notes file that may exist or not exist at a certain host.
 * <p>
 * The in-game notepad is opened by the chat-bar command {@code /notepad
 * <filename>}. Remember that notes always have the file suffix ".txt" but to
 * open them the player <i>must omit </i> the suffix. If the user opens the
 * notepad for a notes file which does not exist a file is created. If the user
 * types the filename with its suffix, such as {@code /notepad abc.txt} he
 * rather opens or creates a file named "abc.txt.txt".
 * <p>
 * Notes files are great channels between game and applications such as SWGAide.
 * Examples: transfer data to and from game; type commands or actions at the
 * in-game notepad which are understood and executed by an application, either
 * automatically or triggered by the user. Results from such commands can be
 * written to the same or to another notes file and opened and read at the
 * in-game notepad.
 * <p>
 * The default location for a notes file is in the station folder, but since
 * Chapter 6.8 a notes file can exist wherever, also outside of the SWG folder.
 * In-game such a file is opened by a file path that is relative to the station
 * for the character. This type supports such files, if the notes file is within
 * a station path an instance of this type is relative the station; otherwise
 * {@link #station} is {@code null} and if the file is within the SWG client the
 * instance is relative SWG, otherwise the instance is an absolute path. In any
 * case an instance of this type denote a file that may, or may not, exist at
 * the current file system.
 * <p>
 * <b>Notice:</b> in-game modifications of a notes file are not saved to file
 * until the user closes the in-game notepad. Hence, modifications are not made
 * visible for SWGAide until the notepad is closed. Similarly, if the in-game
 * notepad is left open then any modification that SWGAide does to the file is
 * later overwritten by the game when the notepad is closed.
 * <p>
 * <b>WARNING:</b> modifier methods does not automatically backup the file
 * before execution; use {@link #backup()}.
 * <p>
 * Remark: before Chapter 6.8 there was a limit on the file size of about
 * 4&nbsp;kB; data above that limit became corrupted by SWG. However, it seems
 * this limit is changed or removed; however, sometimes trailing corrupt data is
 * noticed.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGNotes implements Comparable<SWGNotes>, Serializable, SWGGui {

    /**
     * A list of instances of this type. This list is a cache and a helper for
     * {@link #getInstance(String, SWGStation, SWGUniverse, boolean)}.
     */
    private static List<SWGNotes> notes = new ArrayList<SWGNotes>();

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -6318261149561909458L;

    /**
     * A list of the string of the notes file. The content of this list should
     * never be trusted for more than momentarily; it is used by a few contained
     * methods of this instance and its content should be cleared the next time
     * such a method is invoked. This member is also used as the synchronization
     * lock by accessor methods.
     */
    private transient List<String> content;

    /**
     * The abstract representation of the file for this instance. If
     * {@link #station} is {@code null} this member is either an absolute path
     * or it is relative SWG itself; otherwise it is a path relative the
     * station. The file may exist at the current file system, or not.
     */
    private File file;

    /**
     * A session-scope abstract representation of the absolute path for this
     * instance. This is either station+file, SWG+file, or {@link #file} as-is.
     */
    private transient File fileTemp;

    /**
     * The station that this instance pertains to, or {@code null}.
     */
    private final SWGStation station;

    /**
     * The universe that this instance pertains to, or {@code null}.
     */
    private final SWGUniverse universe;

    /**
     * Helper member that reads version info that is used for possible upgrading
     * during deserialization.
     */
    private String version;

    /**
     * Private scope constructor which creates an instance of this type; invoked
     * by {@link #getInstance(String, SWGStation, SWGUniverse, boolean)}. This
     * constructor does not validate the existence of the instance, it may exist
     * at another host or it may be realized soon. This constructor guarantees
     * that if it is possible the instance is made relative the station or SWG,
     * otherwise the file is retained as-is.
     * <p>
     * If the station is {@code null} the path must be absolute. If a universe
     * is specified the file must be contained within it and the instance is
     * made relative to it, otherwise the file is retained as-is.
     * <p>
     * If the station is not {@code null} the file argument must denote a path
     * within the station. The file argument may be just the name of the notes
     * file, or it must be a path with the station as one of its parents but or
     * is not required to be absolute.
     * 
     * @param file a notes file path
     * @param stn the station this instance pertains to, or {@code null}
     * @param univ the universe this instance pertains to, or {@code null}
     * @throws IllegalArgumentException if the file suffix is not ".txt", or if
     *         the station is {@code null} but the file is not absolute, or if a
     *         station or a universe is specified but the file is outside their
     *         scope
     * @throws NullPointerException if the file is {@code null}
     */
    private SWGNotes(File file, SWGStation stn, SWGUniverse univ) {
        content = new ArrayList<String>(64);
        String name = file.getName();
        if (!name.toLowerCase(Locale.ENGLISH).endsWith(".txt"))
            throw new IllegalArgumentException("Not a TXT file: " + name);

        if (stn == null) {
            if (!file.isAbsolute())
                throw new IllegalArgumentException("No absolute path: " + file);

            // make file relative SWG for host independence
            if (univ != null) {
                String pf = parentPath(file, univ.swgPath().getName());
                if (pf != null) {
                    this.file = pf.isEmpty()
                            ? new File(name) // relative but directly in SWG/TC
                            : new File(pf, name); // relative path SWG/TC
                } else
                    throw new IllegalArgumentException(String.format(
                            "File not within universe: %s / %s", univ, file));
            } else
                this.file = file; // absolute file as-is
        }
        // ELSE: create path relative the station -- there are several options:
        // 1 - file is just a name, no parent --> make station the parent
        // 2 - file's parent is the station
        // 3 - file exists deeper but within the station
        else if (file.getParent() == null
                || file.getParentFile().getName().equals(stn.getName())) {
            this.file = new File(name);

        } else {
            String pf = parentPath(file, stn.getName());
            if (pf == null)
                throw new IllegalArgumentException(String.format(
                        "File not within station: %s / %s ", stn, file));

            this.file = new File(pf, name);
        }

        this.station = stn;
        this.universe = univ;
    }

    /**
     * This method appends or prepends the specified text to this notes file. If
     * the argument is {@code null}, the empty string, or only white spaces this
     * method does nothing. If no file exists for this instance it is created.
     * If there is an error it is caught and written to SWGAide's log file.
     * <p>
     * <b>Notice:</b> if no valid path to SWG exists a client should not invoke
     * this method, the result is undefined.
     * <p>
     * The text is added as is, no white space or EOL character is added to the
     * text. Hence the result depends on as well the content of the file as the
     * specified string.
     * <p>
     * This method is synchronized and locks on {@link #content}.
     * 
     * @param str a text
     * @param append {@code true} to append, {@code false} to prepend
     */
    public void add(String str, boolean append) {
        if (str == null || str.trim().isEmpty()) return;

        synchronized (content) {
            try {
                String cnt = content();
                cnt = cnt == null
                        ? ""
                        : cnt;
                ZString z = new ZString();
                if (append)
                    z.app(cnt).app(str);
                else
                    z.app(str).app(cnt);
                setText(z.toString());
            } catch (Throwable e) {
                SWGAide.printError("SWGNotes:add", e);
            }
        }
    }

    /**
     * Makes a backup of this instance. Up to 10 backups are made with the help
     * of {@link ZWriter#backup(File, String, int)}; if {@link #station}
     * is not {@code null} its name is {@code subDir}, else no sub-directory.
     * <p>
     * This method is synchronized and locks on {@link #content}.
     */
    public void backup() {
        File f = file();
        if (!exists() || f.length() <= 0)
            return; // nothing to backup

        synchronized (content) {
            String subDir = station != null
                    ? subDir = station.getName()
                    : null;

            ZWriter.backup(f, subDir, 10);
        }
    }

    public int compareTo(SWGNotes o) {
        return this.getName().compareTo(o.getName());
    }

    /**
     * Returns the content as a string, or {@code null}. If no file exists for
     * this instance this method returns {@code null}. Otherwise the text is
     * returned as-is without modifications; the text may be an empty string. If
     * there is an error it is caught and written to SWGAide's log file and the
     * error message is returned.
     * <p>
     * This method is synchronized and locks on {@link #content}.
     * 
     * @return the content of this instance as a string, or {@code null}
     * @see #lines()
     */
    public String content() {
        if (!exists()) return null;

        synchronized (content) {
            contentRepopulate();
            if (content.isEmpty()) return "";
            if (content.size() == 1) return content.get(0);

            ZString z = new ZString();
            for (String s : content)
                z.appnl(s);

            return z.sub(0, z.length() - 1); // strip EOL
        }
    }

    /**
     * Helper method which populates {@link #content}. First this implementation
     * clears the list, then reads the file for this instance and adds line by
     * line to the list. If no file exists for this instance the list is empty,
     * otherwise each line of the file is added to the list as-is but without
     * EOL character. If there is an error it is caught and written to SWGAide's
     * log file and the error is added to the list as its only element. This
     * method is not synchronized, the caller must ensure that.
     */
    private void contentRepopulate() {
        content.clear();

        ZReader sr = null;
        try {
            sr = ZReader.newTextReaderExc(file());
            content.addAll(sr.linesExc());
        } catch (Throwable e) {
            SWGAide.printDebug("note", 1, "SWGNotes:content: ",
                    Arrays.toString(e.getStackTrace()));
            content.clear();
            content.add("ERROR: " + e.getMessage());
        } finally {
        	sr.close();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof SWGNotes
                        && file().getName().equals(((SWGNotes)
                                obj).file().getName())
                        && file().getParentFile().getName().equals(((SWGNotes)
                                obj).file().getParentFile().getName()));
    }

    /**
     * Erases either the content of this instance, or the instance itself. If
     * the boolean value is true this method deletes this instance from SWGAide
     * and from the file system, and if possible it deletes itself from
     * {@link #station}. Otherwise this method just empties the file.
     * <p>
     * <b>Notice:</b> this method first invokes {@link #backup()}. This method
     * is synchronized and locks on {@link #content}.
     * 
     * @param instance {@code true} to erase this instance
     */
    public void erase(boolean instance) {
        synchronized (content) {
            try {
                backup();
                if (instance) {
                    if (station != null) station.notesRemove(this);
                    notes.remove(this);
                    file().delete();
                } else
                    setText("");
            } catch (Throwable e) {
                SWGAide.printError("SWGNotes:erase: " + file.toString(), e);
            }
        }
    }

    /**
     * Returns {@code true} if the file for this instance exists. This method is
     * defined by {@link File#exists()}.
     * 
     * @return {@code true} if a file exists
     */
    public boolean exists() {
        return file().exists();
    }

    /**
     * Helper method which returns a session-scope absolute file path for this
     * instance at the current host. The file is an abstract path that may, or
     * may not, exist at the current file system.
     * <p>
     * This implementation makes use of {@link #fileTemp}, the first invocation
     * sets it, then on that is the return value.
     * <p>
     * This method is synchronized and locks on {@link #content}.
     * 
     * @return a file
     */
    private File file() {
        if (fileTemp == null) {
            synchronized (content) {
                if (fileTemp != null) return fileTemp;
                if (station != null)
                    fileTemp = new File(station.swgPath(), file.getPath());
                else if (universe != null)
                    fileTemp = new File(universe.swgPath(), file.getPath());
                else
                    fileTemp = file;
            }
        }
        return fileTemp;
    }

    public String getDescription() {
        return file().toString();
    }

    public String getName() {
        return file.getName();
    }

    @Override
    public int hashCode() {
        return file().getName().hashCode()
                + 17 * file().getParentFile().getName().hashCode();
    }

    /**
     * Returns the length of the file for this instance, or -1 if it does not
     * exist. This method is defined by {@link File#length()}.
     * 
     * @return the file size, or -1
     */
    public long length() {
        return exists()
                ? file().length()
                : -1;
    }

    /**
     * Returns the zero-based line number for the specified text, or -1. This
     * method scans from {@code begin} and returns the first occurrence it
     * finds. The text must be contained within one line and read identically,
     * though it may be located wherever at that line. This method returns -1 if
     * the text is not found, if {@code begin} is too large, if a file does not
     * exist for this instance, or if there is an error.
     * <p>
     * This method is synchronized and locks on {@link #content}.
     * 
     * @param str a string to search for
     * @param begin the line to begin searching at, or 0
     * @return the line number for the first occurrence, or -1
     * @throws IndexOutOfBoundsException if {@code begin < 0}
     */
    public int line(String str, int begin) {
        if (exists()) {
            synchronized (content) {
                contentRepopulate();
                for (int i = begin; i < content.size(); ++i)
                    if (content.get(i).indexOf(str) >= 0) return i;
            }
        }
        return -1;
    }

    /**
     * Returns the content of this instance as a list of strings. If no file
     * exists this method returns {@code null}. If the file is empty this method
     * returns an empty list. Otherwise the elements are the text lines as-is
     * without modifications but without EOL. If there is an error it is caught
     * and written to SWGAide's log file and returned as one, single element.
     * <p>
     * This method is synchronized and locks on {@link #content}. The list is a
     * copy and may be modified.
     * 
     * @return this instance as a list of strings, or {@code null}
     */
    public List<String> lines() {
        if (!exists()) return null;

        synchronized (content) {
            contentRepopulate();
            return new ArrayList<String>(content);
        }
    }

    /**
     * Returns the name of the parent directory for this instance.
     * 
     * @return the name of the parent folder
     */
    public String parentDir() {
        return station != null
                ? station.getName()
                : universe != null
                        ? universe.getName()
                        : file.getParentFile() != null
                                ? file.getParentFile().getName()
                                : "?";
    }

    /**
     * Resolves this instance at deserialization. If {@link #version} indicates
     * an upgrade action this implementation executes this.
     * 
     * @return this
     */
    private Object readResolve() {
        content = new ArrayList<String>(64);
        if (this.version == null) {
            // first upgrade ever
            // notes <= 0.8.16 were always located in the station folder
            this.file = new File(this.file.getName());
        }

        this.version = SWGConstants.version;
        notes.add(this);
        return this;
    }

    /**
     * Helper method which collates the list to a string and invokes
     * {@link #setText(String)}. If there is an error it is caught, written to
     * SWGAide's log file and returned; otherwise this method returns {@code
     * null}.
     * 
     * @param list a list of strings to write
     * @return an error message or {@code null}
     */
    private String setList(List<String> list) {
        try {
            int size = list.size();
            ZString z = new ZString();
            for (int i = 0; i < size; ++i) {
                z.app(list.get(i));
                if (i < size - 1) z.nl();
            }

            return setText(z.toString());
        } catch (Throwable e) {
            SWGAide.printError("SWGNotes:setList: " + file.toString(), e);
            return e.getMessage();
        }
    }

    /**
     * This method appends, prepends, or replaces the specified string to this
     * instance and returns replaced text as a string, otherwise {@code null}.
     * The endpoints are zero-based and they are inclusive so they are replaced.
     * The replacement string may contain any number of lines, EOL delimited.
     * This instance is finally saved to file. If there is an error it is caught
     * and written to SWGAide's log file and the error message is returned. Also
     * see {@link #add(String, boolean)}.
     * <p>
     * <b>Notice:</b> if no valid path to SWG exists a client should not invoke
     * this method, the result is undefined.
     * <p>
     * <b>Notice:</b> all trailing EOL characters are removed from the string
     * before it is inserted and one EOL character is appended.
     * <p>
     * <b>Notice:</b> if {@code first} is larger than the number of lines in
     * this instance this method does nothing but returns {@code null}. If no
     * file exists for this instance and {@code first == 0} then the string is
     * written to file as-is and this method returns the empty string.
     * <p>
     * The value of the <b>last</b> parameter rules many actions of this method:
     * <ul>
     * <li><b>less than</b> {@code first} &mdash; the string is inserted just
     * before {@code first} without replacement.</li>
     * <li><b>equal to</b> {@code first} &mdash; that one row is replaced.</li>
     * <li><b>larger than</b> {@code first} &mdash; all lines of the specified
     * rage are replaced, the two lines included.</li>
     * <li><b>larger than</b> the amount of lines in this instance &mdash; all
     * lines from and including {@code first} are replaced.</li>
     * </ul>
     * <p>
     * The <b>replacement string</b> may take the following forms:
     * <ul>
     * <li>any length and any number EOL characters; trailing EOLs are erased.</li>
     * <li>the empty string which erases the specified block; a string of just
     * EOL characters equals an empty string.</li>
     * </ul>
     * <p>
     * This method is synchronized and locks on {@link #content}.
     * 
     * @param first the first line of the block (inclusive)
     * @param last the last line of the block (inclusive)
     * @param str the replacement string
     * @return the replaced text, or {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public String setText(int first, int last, String str) {
        if (str == null) throw new NullPointerException("str is null");

        boolean exists = exists();
        if (first > 0 && !exists) return null;

        synchronized (content) {
            String st = shaveEOL(str);

            if (!exists && first == 0) {
                st = setText(st);
                return st == null
                        ? ""
                        : st;
            }

            contentRepopulate();
            if (content.size() > 0 && content.get(0).startsWith("ERROR: "))
                return content.get(0);

            int end = content.size();
            if (first >= end) return null;
            int frst = Math.max(0, first);
            end = Math.min(last, end - 1);

            // Cases for replace, mixed
            // 1) normal string --> replaces as expected
            // 2) "the empty string", i.e. "" --> delete (special replace)
            // a) last < first --> insert st just before first except empty st
            // b) last == first --> replace one line, possibly delete
            // c) last > first --> replace frst to end, possibly delete

            String[] replaced = new String[0];
            if (last < first) {
                if (!st.isEmpty()) content.add(frst, st);
            } else if (last == frst) {
                replaced = new String[] { content.get(frst) };
                if (st.isEmpty())
                    content.remove(frst);
                else
                    content.set(frst, st);
            } else { // last > first
                replaced = content.subList(frst, end + 1).toArray(replaced);
                content.set(frst, st);
                content.subList(frst + 1, end + 1).clear();
            }

            st = setList(content);
            if (st != null) return st; // error

            ZString z = new ZString();
            for (String s : replaced)
                z.appnl(s);
            return z.toString();
        }
    }

    /**
     * Sets the text of this instance to the file for this instance. If no file
     * exists it is created. This method writes the content to file as is,
     * without modifications. If the argument is {@code null} this method does
     * nothing. If there is an error it is caught, written to SWGAide's log file
     * and returned; otherwise this method returns {@code null}.
     * <p>
     * <b>Notice:</b> if no valid path to SWG exists a client should not invoke
     * this method, the result is undefined.
     * <p>
     * This method is synchronized and locks on {@link #content}.
     * 
     * @param str the text to write
     * @return an error message or {@code null}
     */
    public String setText(String str) {
        if (str != null) {
            synchronized (content) {
                try {
                    File f = file();
                    f.getParentFile().mkdirs();

                    ZWriter.writeExc(str, f, false);
                } catch (Throwable e) {
                    SWGAide.printError("SWGNotes:setText: " + file, e);
                    return e.getMessage();
                }
            }
        }
        return null;
    }

    /**
     * Helper method which shaves off all trailing EOL characters and returns
     * the string. If the argument is {@code null} or if there are no EOLs this
     * method returns the string as-is.
     * 
     * @param str a string
     * @return a shaved string, or the argument itself
     */
    private String shaveEOL(String str) {
        String st = str;
        while (st != null && st.endsWith(ZString.EOL))
            st = st.substring(0, st.length() - 1);
        return st;
    }

    /**
     * Returns the station that this instance pertains to, or {@code null} if
     * this instance is located outside a station.
     * 
     * @return a station, or {@code null}
     */
    public SWGStation station() {
        return station;
    }

    @Override
    public String toString() {
        return String.format("SWGNotes[%s, stn=%s, univ=%s] %s",
                file, station, universe, file());
    }

    /**
     * Helper method that returns a cached notes instance, or {@code null}. This
     * method scans {@link #notes} for an instance that matches the argument. If
     * none is found or if the arguments is not an absolute path this method
     * returns {@code null}.
     * 
     * @param f a file
     * @return a notes instance that matches the argument, or {@code null}
     * @throws IllegalArgumentException if the file is not absolute
     */
    private static SWGNotes getCached(File f) {
        if (!f.isAbsolute())
            throw new IllegalArgumentException("File not absolute path");

        for (SWGNotes n : notes)
            if (f.equals(n.file())) return n;

        return null;
    }

    /**
     * Returns a notes instance for the specified arguments. If no instance
     * exists in SWGAide it is created, added to the GUI, and returned, this
     * method does not validate for existence at the current file system.
     * <p>
     * Specify a station only if the file is located within a station, it is
     * then preferred to set universe to {@code null}. Specify a universe only
     * if the file is located within a universe and it does not exist within a
     * station, which then is {@code null}.
     * <p>
     * If the specified name does not end with ".txt" it is appended.
     * <p>
     * If a station is specified the universe is ignored and the instance
     * pertains to the station; the name is either just a name or it must be a
     * path with the station as one of the parent folders.
     * <p>
     * Otherwise the name must always be an absolute file path. The instance
     * either pertains to the specified universe or the path is used as-is.
     * <p>
     * If the boolean argument is {@code true} and an instance is created a
     * dialog is displayed.
     * <p>
     * If there is an error it is caught and written to SWGAide's log file and
     * this method returns {@code null}.
     * 
     * @param name a file name, a path, or an absolute path
     * @param stn a station for the notes, or {@code null}
     * @param univ a universe for the notes, or {@code null}
     * @param dialog {@code true} to display an info dialog
     * @return a notes file instance
     * @throws IllegalArgumentException if the station is {@code null} but the
     *         file is not absolute, or if the arguments together do not produce
     *         an absolute file path
     * @throws NullPointerException if the file is {@code null}
     */
    public static SWGNotes getInstance(
            String name, SWGStation stn, SWGUniverse univ, boolean dialog) {

        synchronized (notes) {
            String nm = name.toLowerCase().endsWith(".txt")
                    ? name
                    : name + ".txt";
            File f = stn != null
                    ? new File(stn.swgPath(), nm)
                    : new File(nm);
            SWGNotes n = getCached(f);
            if (n != null) return n; // use cached

            SWGFrame frame = SWGAide.frame();
            try {
                if (!f.exists()) f.createNewFile();
                if (stn != null) {
                    n = new SWGNotes(f, stn, null);
                    stn.notesAdd(n);

                    // FIXME: remove this and make it happen at the invoker
                    // or in a controller
                    SWGNotesNode nn = new SWGNotesNode(n);
                    SWGTreeNode node = SWGTreeNode.focusedNode();
                    if (node != null) {
                        while (!(node instanceof SWGStationNode))
                            node = (SWGTreeNode) node.getParent();

                        ((DefaultTreeModel) frame.getMainTab().tree.getModel()).
                                insertNodeInto(nn, node,
                                        node.getChildCount() - 1);
                    }
                    if (dialog && node != null)
                        JOptionPane.showMessageDialog(frame,
                                String.format("Created notes file \"%s\"", nm),
                                "New file", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    n = new SWGNotes(f, null, univ); // FIXME: add to univ node
                }

                notes.add(n);
                return n;
            } catch (Throwable e) {
                if (dialog) {
                    JOptionPane.showMessageDialog(frame,
                            String.format("Failed creating file \"%s\"%n\t%s",
                                    name, e.getMessage()),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    SWGAide.printDebug("note", 1, "SWGNotes:getInstance: ",
                            Arrays.toString(e.getStackTrace()));
                } else
                    SWGAide.printError(String.format(
                            "SWGNotes:getInstance: %s %s", name, stn), e);
            }
            return null;
        }
    }

    /**
     * Returns an array of names for the notes files in {@code station}, or an
     * empty array if none was found. The boolean argument determines if also
     * names for instances for which no files exist at the current file system,
     * or if only names for instances with existing files are included.
     * 
     * @param station a station
     * @param exists {@code false} to include also names for non-existing files
     * @return a list of names for the notes files, or an empty list
     */
    public static String[] notesNames(SWGStation station, boolean exists) {
        List<SWGNotes> ns = station.notes();
        List<String> names = new ArrayList<String>(ns.size());
        for (SWGNotes n : ns)
            if (!exists || n.exists())
                names.add(n.getName());

        return names.toArray(new String[names.size()]);
    }

    /**
     * Helper method which returns the parent part of the specified file that is
     * relative to the specified string, or {@code null}. This method returns
     * returns {@code null} if the specified string is never found in the file
     * path, or the empty string if the immediate parent equals the string.
     * Otherwise this method returns the path relative to the string, but
     * without the string and the file itself.
     * 
     * @param f the file to process
     * @param p a string for a parent folder
     * @return a path relative to the string, or {@code null}
     */
    private static String parentPath(File f, String p) {
        ZString z = new ZString();
        File child = f;

        File parent = null;
        while ((parent = child.getParentFile()) != null
                && !parent.getName().equals(p)) {

            if (!z.isEmpty() && !parent.getName().isEmpty())
                z.pre(File.separator, false);

            z.pre(parent.getName(), false);
            child = parent;
        }

        return parent != null
                ? z.toString()
                : null;
    }
}
