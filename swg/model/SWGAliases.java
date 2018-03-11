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
import swg.tools.ZReader;
import swg.tools.ZWriter;

/**
 * This type represents an aliases file within SWG.
 * <p>
 * Several aliases files may exist in a SWG client folder, in-game they must be
 * loaded manually by the user by the command {@code /load <file-name.suffix>},
 * and any in-game modifications or additions to the aliases are automatically
 * saved by SWG to the most recently loaded aliases file. Hence, this type does
 * not store the content but it is just a wrapper for a file of aliases.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGAliases implements
        Comparable<SWGAliases>, Serializable, SWGGui {

	/**
     * A list of instances of this type. This list is a cache and a helper for
     * {@link #getInstance(String, SWGStation, SWGUniverse, boolean)}.
     */
    private static List<SWGAliases> aliases = new ArrayList<SWGAliases>();
    
	/**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -4007008949079840400L;
    
    /**
     * A list of the string of the aliases file. The content of this list should
     * never be trusted for more than momentarily; it is used by a few contained
     * methods of this instance and its content should be cleared the next time
     * such a method is invoked. This member is also used as the synchronization
     * lock by accessor methods.
     */
    private transient List<String> content;

    /**
     * A short description for this aliases file. The string may be used in GUI
     * applications. The setter method silently truncates this member to 20
     * characters.
     */
    private String desc;

    /**
     * A long description for this aliases file. The string may be used in GUI
     * applications.
     */
    private String description;

    /**
     * The file this instance pertains to. This file is relative the universe it
     * pertains to, not an absolute path.
     */
    private File file;

    /**
     * The universe that this aliases file pertains to.
     */
    private final SWGUniverse universe;

    /**
     * Creates an instance of this type. Only
     * {@link #scanForNewAliasesFiles(SWGUniverse)} invokes this constructor.
     * 
     * @param file an aliases file
     * @param universe the universe the aliases file pertains to
     */
    private SWGAliases(File file, SWGUniverse universe) {
        this.file = new File(file.getName());
        this.universe = universe;
    }

    public int compareTo(SWGAliases o) {
        return file.compareTo(o.file);
    }

    /**
     * Returns the content of this aliases file as text, or an the empty string
     * if the file is empty. If the file does not exist this method returns
     * {@code null}. If there is an error a string with the error is returned.
     * 
     * @return this aliases file as text, an empty string, or {@code null}
     */
    public String content()  {
        String s = ZReader.read(file());
        return s == null
                ? "Missing file on computer"
                : s;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || (obj instanceof SWGAliases
                    && file().equals(((SWGAliases) obj).file()));
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
        
            try {
                if (instance) {
                    if (universe != null) universe.aliasRemove(this);
                    aliases.remove(this);
                    file().delete();
                } else
                    setText("");
            } catch (Throwable e) {
                SWGAide.printError("SWGAliases:erase: " + file.toString(), e);
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
                    SWGAide.printError("SWGAliases:setText: " + file, e);
                    return e.getMessage();
                }
            }
        }
        return null;
    }
    
    /**
     * Determines if this instance exists at the current file system.
     * 
     * @return a boolean value as defined by {@link File#exists()}
     */
    public boolean exists() {
        return file().exists();
    }

    /**
     * Helper method which returns the absolute path that this instance pertains
     * to. The returned file is session scope and may not exist at the current
     * system.
     * 
     * @return the file for this instance
     */
    private File file() {
        return new File(universe.swgPath(), getName());
    }

    public String getDescription() {
        return description == null
                ? ""
                : description;
    }

    /**
     * Returns a short description of this aliases object, max 20 characters, or
     * an empty string.
     * 
     * @return a short description
     */
    public String getDescriptionShort() {
        return desc == null
                ? ""
                : desc;
    }

    public String getName() {
        return file.getName();
    }

    @Override
    public int hashCode() {
        return file().hashCode();
    }

    /**
     * Resolves this instance at deserialization. If {@link #file} is absolute
     * it is replaced with a file for its file name only, a relative file.
     * 
     * @return this
     */
    private Object readResolve() {
        if (file.isAbsolute()) file = new File(file.getName());

        return this;
    }

    /**
     * Sets a description for this instance. Use {@code null} to erase any
     * previous content.
     * 
     * @param longDescription a description, or {@code null}
     */
    public void setDescription(String longDescription) {
        description = longDescription;
    }

    /**
     * Sets a short description for this instance. Use {@code null} to erase any
     * previous content. The specified argument is silently truncated to 20
     * characters if necessary.
     * 
     * @param shortDescription a short description
     */
    public void setDescriptionShort(String shortDescription) {
        desc = shortDescription != null && shortDescription.length() > 20
                ? shortDescription.substring(0, 20)
                : shortDescription;
    }

    @Override
    public String toString() {
        return String.format("SWGAliases[%s]", file().getAbsolutePath());
    }

    /**
     * Returns the universe to which this instance pertains.
     * 
     * @return a universe
     */
    public SWGUniverse universe() {
        return universe;
    }

    /**
     * Determines whether the specified file is likely to be an aliases file.
     * This method scans the specified file for simple patterns that are
     * specific to an aliases file, however, if the specified file mimics this
     * pattern but yet not be an aliases file this method fails.
     * 
     * @param file a file to examine
     * @return {@code true} if the file is likely to be an aliases file
     */
    public static boolean isLikely(File file) {
        // sample:
        // harvpay /paym 16800; /addpow 18000; /pau 1; /structureStatus
        // if first char after white space is / -> true
        // unlikely but possible to fail
        boolean ret = false;
        ZReader sr = ZReader.newTextReader(file);
        if (sr != null) {
            String line = sr.line();
            if (line != null) {
                int i = line.indexOf(' ');
                if (i > 0 && ++i < line.length() && line.charAt(i) == '/')
                        ret = true;
            }
            sr.close();
        }
        return ret;
    }

    /**
     * Helper method which returns a list of all files in the specified universe
     * which are considered aliases files, or an empty list. The elements are
     * files relative to the specified universe, not absolute paths.
     * 
     * @param universe the universe to scan
     * @return a list of files, or an empty list
     */
    private static List<File> scanForAliasesFiles(SWGUniverse universe) {
        File ud = universe.swgPath();
        File[] fls = ud.listFiles(new FileFilter() {
            public boolean accept(File f) {
                String fn = f.getName().toLowerCase();
                return (f.isFile() && !fn.endsWith("tre")
                        && !fn.endsWith("dll") && !fn.endsWith("bmp")
                        && !fn.endsWith("exe") && !fn.endsWith("doc")
                        && !fn.endsWith("cfg") && !fn.endsWith("pdf")
                        && !fn.endsWith("pdf") && !fn.endsWith("rtf")
                        && !fn.endsWith("tmp") && !fn.contains(".log."));
            }
        });
        if (fls == null) return Collections.emptyList();

        ArrayList<File> als = new ArrayList<File>(fls.length);
        for (File f : fls)
            if (SWGAliases.isLikely(f)) als.add(new File(f.getName()));

        return als;
    }

    /**
     * Returns a list with new aliases files for the specified universe. The
     * returned list contains only files which are not already known in the
     * universe. If no new files are found the empty list is returned. If
     * SWGAide or its DAT file is moved to another computer this method returns
     * aliases files with names that are found at that computer but does not
     * remove any instances that are not found; see comment on {@link #file()}.
     * 
     * @param universe the universe to scan for new aliases files
     * @return a list of new aliases files, or an empty list
     */
    public static List<SWGAliases> scanForNewAliasesFiles(SWGUniverse universe) {
        List<File> unknown = scanForAliasesFiles(universe);
        if (unknown.isEmpty())
            return Collections.emptyList();

        // reduce to really unknown
        List<SWGAliases> known = universe.aliases();
        for (SWGAliases a : known)
            unknown.remove(a.file);

        List<SWGAliases> news = new ArrayList<SWGAliases>(unknown.size());
        for (File f : unknown)
            news.add(new SWGAliases(f, universe));

        return news;
    }

    /**
     * A convenience type which is a list of aliases files.
     * <p>
     * This implementation extends {@link ArrayList} and overrides adder methods
     * and provides a delete method. The adder methods ignores null elements and
     * equal elements; the add-methods are not implemented except for the most
     * basic add(X).
     */
    static final class SWGAliasesList extends ArrayList<SWGAliases> {

        /**
         * Serialization version info. Don't meddle with this or break the
         * deserialization.
         */
        private static final long serialVersionUID = -1451207884837081718L;

        @Override
        public void add(int index, SWGAliases aliases) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public boolean add(SWGAliases aliases) {
            synchronized (this) {
                if (aliases == null || this.contains(aliases))
                    return false;

                boolean b = super.add(aliases);
                Collections.sort(this);
                return b;
            }
        }

        @Override
        public boolean addAll(Collection<? extends SWGAliases> c) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public boolean addAll(int index, Collection<? extends SWGAliases> c) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public String toString() {
            return String.format("SWGAliasesList[%s]", super.toString());
        }
    } // end class AliasesList
}
