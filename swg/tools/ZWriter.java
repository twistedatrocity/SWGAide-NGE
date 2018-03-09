package swg.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import swg.SWGAide;
import swg.SWGConstants;

/**
 * A utility type for writing to files and output streams (do not confuse the
 * name with {@link Writer} that is only for character streams). This type, and
 * its counterpart {@link ZReader}, provides simplified IO and error handling.
 * Common methods are doubled, some write errors to SWGAide's log files and
 * return sensibly, but others return exceptions.
 * <p>
 * This type supports both character and byte streams; for methods that read
 * "text" it is assumed that it operates on a character stream.
 * <p>
 * Instances of this type are thought to be short lived within a limited scope,
 * hence this type is not made thread safe. Neither does this type guard against
 * doubles being opened for the same stream.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class ZWriter {

    /**
     * A text writer.
     */
    private final Writer txt;

    /**
     * Creates an instance of this type. The boolean argument determines whether
     * to append to an existing file, or replace it. If the file does not exist
     * it is created; if the parent directories of the path does not exist they
     * are created.
     * 
     * @param f an abstract file path
     * @param append {@code true} to appended, otherwise replace
     * @throws Exception if there is an error
     */
    private ZWriter(File f, boolean append) throws Exception {
        File p = f.getAbsoluteFile().getParentFile();
        if (p != null) p.mkdirs();

        txt = new FileWriter(f, append);
    }

    /**
     * Flushes and closes the stream for this instance.
     */
    public void close() {
        try {
            txt.flush();
            txt.close();
        } catch (Throwable e) {/* ignore */
        }
    }

    /**
     * Writes one {@link ZString#EOL} to this text writer.
     */
    public void eol() {
        try {
            writeExc(ZString.EOL);
        } catch (Throwable e) {/* ignore */
        }
    }

    /**
     * Writes the list of strings as defined by {@link #writeExc(List)}. If the
     * there is an error it is intercepted and written to SWGAide's log file.
     * 
     * @param ls a list of strings
     */
    public void write(List<String> ls) {
        try {
            writeExc(ls);
        } catch (Throwable e) {
            SWGAide.printError("ZWriter:write", e);
        }
    }

    /**
     * Writes the specified string to this text writer. If the there is an error
     * it is intercepted and written to SWGAide's log file.
     * 
     * @param s a string
     */
    public void write(String s) {
        try {
            writeExc(s);
        } catch (Throwable e) {
            SWGAide.printError("ZWriter:write", e);
        }
    }

    /**
     * Writes the specified list of strings to this text writer; each line is
     * delimited by an {@link ZString#EOL}; add an empty line as the last
     * element to enforce a trailing EOL. If the list is empty or {@code null}
     * this method does nothing. This writer is flushed but left open for
     * further operations.
     * 
     * @param ls a list of strings
     * @throws Exception if there is an error
     */
    public void writeExc(List<String> ls) throws Exception {
        if (ls == null || ls.isEmpty()) return;

        Iterator<String> iter = ls.iterator();
        writeExc(iter.next());
        while (iter.hasNext()) {
            writeExc(ZString.EOL);
            writeExc(iter.next());
        }
        txt.flush();
    }

    /**
     * Writes the specified string to this text writer.
     * 
     * @param s a string
     * @throws Exception if there is an error
     */
    public void writeExc(String s) throws Exception {
        txt.write(s);
    }

    /**
     * Writes the specified string to this text writer and appends an EOL. If
     * the there is an error it is intercepted and written to SWGAide's log
     * file.
     * 
     * @param s a string
     */
    public void writeln(String s) {
        try {
            writelnExc(s);
        } catch (Throwable e) {
            SWGAide.printError("ZWriter:writeEOL", e);
        }
    }

    /**
     * Writes the specified string to this text writer and appends an EOL.
     * 
     * @param s a string
     * @throws Exception if there is an error
     */
    public void writelnExc(String s) throws Exception {
        writeExc(s);
        writeExc(ZString.EOL);
    }

    /**
     * Does a backup of the specified file to {@link SWGConstants#backupDir}. If
     * there is an existing file it is overwritten. The backup file is suffixed
     * ".bak", for example, both the files "myfile.txt" and "myfile" are given
     * the name "myfile.bak". If the main part if the file name is the empty
     * string this method does nothing and returns {@code false}.
     * <P>
     * <B>Note: </B>This implementation does not create any sub-directory within
     * the backup folder which is were the backup file will be written.
     * 
     * @param file the file to backup
     * @return {@code true} if successful, {@code false} otherwise
     * @throws NullPointerException if the argument is {@code null}
     */
    public static boolean backup(File file) {
        String fName = file.getName();
        int idx = fName.lastIndexOf('.');
        if (idx >= 0) fName = fName.substring(0, idx);

        if (fName.isEmpty()) return false;

        File target = new File(SWGConstants.backupDir, fName + ".bak");
        return ZWriter.copy(file, target);
    }

    /**
     * Backup the specified file to an optional sub-directory within
     * {@link SWGConstants#backupDir} using a 3-digit suffix within the range
     * {@code [000 max]}, inclusive. File names are always preserved except for
     * the suffix. This implementation does not create more than {@code max}
     * number of backup files. However, any existing files greater than {@code
     * max} are not touched by this method but will remain.
     * <P>
     * <B>The file suffix</B> is a string representation of an integer and is
     * always 3 digits no matter the value of {@code max}. The suffix wraps at
     * {@code max} and starts over at 000, existing files are overwritten.
     * Notice, this implementation does not rename or reorder the backup files.
     * The suffix is determined with the following heuristic (a "hole" is an
     * expected suffix which is unused):
     * <UL>
     * <LI>find the highest free {@code suffix <= max} while ignoring holes in
     * the range</LI>
     * <LI>else if reaching {@code max} and if there is a hole in the range,
     * {@code suffix = hole}<SUB><I>first</I></SUB></LI>
     * <LI>else if there is no hole, delete the file with the suffix 000 and use
     * the new hole</LI>
     * <LI>copy the specified file using the determined suffix and create a hole
     * to be used the next time, if possible
     * <UL>
     * <LI>if copy is successful, delete the file which equals to {@code suffix
     * + 1}</LI>
     * <LI>else delete the failed backup file, if any</LI>
     * </UL>
     * </LI>
     * </UL>
     * <P>
     * <B>Sub-directories</B> within SWGAide's backup folder are suported and if
     * a required sub-directory does not exist it is created. Sub-directories
     * are handled in the following manner, determined by the argument {@code
     * subDir}:
     * <UL>
     * <LI>if {@code subDir} is {@code null} the specified file is examined:
     * <UL>
     * <LI>if the file denotes an absolute file no sub-directory is used, see
     * {@link File#isAbsolute()}</LI>
     * <LI>else if the file's parent path is {@code null} no sub-directory is
     * used, see {@link File#getParentFile()}</LI>
     * <LI>else the file's parent path is used in full</LI>
     * </UL>
     * </LI>
     * <LI>else {@code subDir} overrides the automatic file path handling, in
     * both cases within the backup folder:
     * <UL>
     * <LI>if {@code subDir} is the empty string no sub-directory is used</LI>
     * <LI>else the path it denotes is used</LI>
     * </UL>
     * </LI>
     * </UL>
     * 
     * @param file a file object which denotes the file to backup
     * @param subDir a string, or {@code null}, see above
     * @param max max number of backup files, in the range [1 999]
     * @return if the operation is successful the integer value of the {@code
     *         suffix} that is used, otherwise {@code -1}
     * @throws IllegalArgumentException if a value is illegal
     * @throws NullPointerException if an argument is {@code null}
     */
    public static int backup(File file, String subDir, int max) {
        if (max <= 0 || max > 999)
            throw new IllegalArgumentException("Illegal max: " + max);

        // use more backups in dev-mode, will break sometimes for dev <--> nodev
		@SuppressWarnings("unused")
        int mx = SWGConstants.DEV_DEBUG && max < 10
                ? max + 10
                : max;

        File bakDir = new File(SWGConstants.backupDir);
        if (subDir != null)
            bakDir = new File(bakDir, subDir);
        else if (!file.isAbsolute() && file.getParent() != null)
            bakDir = new File(bakDir, file.getParentFile().getPath());

        // create if does not exist
        if (!bakDir.exists() && !bakDir.mkdirs())
            SWGAide.printError(
                    "ZWriter:backupFile: failed to create subdirectory: "
                            + bakDir.toString(), new IOException());

        String fName = file.getName();
        int idx = fName.lastIndexOf('.');
        if (idx >= 0)
            fName = fName.substring(0, idx);

        int suffix = 0;
        try {
            suffix = ZWriter.determineSuffix(bakDir, fName, mx);
        } catch (Throwable e) {
            SWGAide.printError("ZWriter:backupFile: error with \""
                    + file.getName(), e);
            return -1; // abort
        }

        String tName = String.format("%s.%03d", fName, Integer.valueOf(suffix));
        File target = new File(bakDir, tName);

        if (ZWriter.copy(file, target)) {
            // success, delete (suffix + 1) if any
            int delSuffix = suffix + 1;
            if (delSuffix > mx)
                delSuffix = 0; // wrap around

            String dName =
                    String.format("%s.%03d", fName, Integer.valueOf(delSuffix));
            File delTarget = new File(bakDir, dName);
            delTarget.delete();
        } else {
            SWGAide.printDebug("util", 1, "Utils:backupFile: \""
                    + file.getName() + "\" failed (del:" + target.delete()
                    + ')');
            return -1;
        }

        return suffix;
    }

    /**
     * Copies the content from the specified source to the target and returns
     * success. If the target exists it is silently overwritten, otherwise it is
     * created. If there is an error it is intercepted and written to SWGAide's
     * log file and this method returns {@code false}.
     * 
     * @param src the file to read from
     * @param to the file to write to
     * @return {@code true} if successful
     */
    public static boolean copy(File src, File to) {
    	InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(to);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            return true;
        } catch (Throwable e) {
            SWGAide.printError("ZReader:copy", e);
        } finally {
            try {
				is.close();
				os.close();
			} catch (IOException e) {
				SWGAide.printError("ZReader:copy", e);
			}
        }
        return false;
    }

    /**
     * Helper method that copies the content from the specified source to the
     * target and returns success. This method operates as defined by the
     * arguments respectively, by {@link InputStream#read(byte[])},
     * {@link OutputStream#write(byte[])} and a 1,024 bytes array. If there is
     * an error it is intercepted and written to SWGAide's log file.
     * 
     * @param in a stream to read from
     * @param out a stream to write to
     * @throws Exception if there is an error
     */
    static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[1024];
        for (int len; (len = in.read(buf)) >= 0;)
            out.write(buf, 0, len);

        out.flush();
        out.close();
        in.close();
    }

    /**
     * Helper method which returns a suffix for {@code fName} which is less than
     * or equal to {@code max}. There may exist a file which has the returned
     * suffix.
     * 
     * @param bakDir the directory for the backup file
     * @param fName the proper name of file to backup
     * @param max max number of backup files, within the range [1 999]
     * @return the determined suffix to use, a file with this suffix may exist
     * @throws NullPointerException if an argument is {@code null}
     * @throws NumberFormatException if a parsed suffix is not a number
     */
    private static int determineSuffix(File bakDir, String fName, int max) {
        // fName can be beginning of another file name, such as "foo" matches
        // "foobar", but all backup file have a dot so foo. makes name unique
        final String name = fName + '.';

        String[] files = bakDir.list(new FilenameFilter() {

            public boolean accept(File dir, String n) {
                return n.startsWith(name);
            }
        });
        if (files == null || files.length == 0)
            return 0;

        Arrays.sort(files); // sort now to get "hole" and highest right

        int nameLen = name.length(); // includes the dot
        int hole = -1;
        int highest = -1;
        for (int i = 0; i < files.length && i <= max; ++i) {
            highest = Integer.parseInt(files[i].substring(nameLen));
            if (hole < 0 && highest != i)
                hole = i; // first time the suffix is not i >>> the first hole
        }

        if (highest < max && highest >= 0)
            return highest + 1; // next free suffix
        else if (highest >= max && hole >= 0)
            return hole; // first hole
        // else
        return 0; // wrap around
    }

    /**
     * Creates and returns a text writer, or {@code null}; this method is
     * defined by {@link #newTextWriterExc(File, boolean)}. If the there is an
     * error it is intercepted and written to SWGAide's log file and this method
     * returns {@code null}.
     * 
     * @param f an abstract file path
     * @param append {@code true} to append, otherwise replace
     * @return a text writer, or {@code null}
     */
    public static ZWriter newTextWriter(File f, boolean append) {
        try {
            return new ZWriter(f, append);
        } catch (Throwable e) {
            SWGAide.printError("ZWriter:newTextWriter", e);
            return null;
        }
    }

    /**
     * Creates and returns a text writer. The boolean argument determines
     * whether to append to an existing file, or replace it. If the file does
     * not exist it is created; if the parent directories of the path does not
     * exist they are created.
     * 
     * @param f an abstract file path
     * @param append {@code true} to append, otherwise replace
     * @return a text writer
     * @throws Exception if there is an error
     */
    public static ZWriter newTextWriterExc(File f, boolean append)
            throws Exception {
        return new ZWriter(f, append);
    }

    /**
     * Writes to file as defined by {@link #writeExc(String, File, boolean)}. If
     * the there is an error it is intercepted and written to SWGAide's log
     * file.
     * 
     * @param s a string
     * @param f an abstract file path
     * @param append {@code true} to appended, otherwise replace
     */
    public static void write(String s, File f, boolean append) {
        try {
            writeExc(s, f, append);
        } catch (Throwable e) {
            SWGAide.printError("ZWriter:write", e);
        }
    }

    /**
     * Writes the specified string to the file. The boolean argument determines
     * whether to append to an existing file, or replace it. If the file does
     * not exist it is created; if the parent directories of the path does not
     * exist they are created.
     * 
     * @param s a string
     * @param f an abstract file path
     * @param append {@code true} to append, otherwise replace
     * @throws Exception if there is an error
     */
    public static void writeExc(String s, File f, boolean append)
            throws Exception {

        ZWriter fw = new ZWriter(f, append);
        fw.writeExc(s);
        fw.close();
    }
}
