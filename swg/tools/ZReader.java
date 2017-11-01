package swg.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import swg.SWGAide;

/**
 * A utility type for reading files and input streams (do not confuse the name
 * with {@link Reader} that is only for character streams). This type, and its
 * counterpart {@link ZWriter}, provides simplified IO and error
 * handling. Common methods are doubled, some write errors to SWGAide's log
 * files and return sensibly, but others return exceptions.
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
public final class ZReader {

    /**
     * The input stream to read from. This member is always valid and is either
     * a byte or a character stream; in the former case {@link #txt} is null.
     */
    final BufferedInputStream bin;

    /**
     * The text reader to read from, or {@code null} if this reader operarws on
     * a byte stream.
     */
    private final LineNumberReader txt;

    /**
     * Creates an instance of this type for the specified stream. The boolean
     * argument determines if {@link #txt} is set.
     * 
     * @param in an input stream
     * @param text {@code true} for character stream
     * @throws Exception if there is an error
     */
    private ZReader(InputStream in, boolean text) throws Exception {
        bin = new BufferedInputStream(in);
        txt = text
                ? new LineNumberReader(new InputStreamReader(bin))
                : null;
    }

    /**
     * Reads from this byte reader into the specified array as defined by
     * {@link BufferedInputStream#read(byte[])}.
     * 
     * @param buf a byte array
     * @return amount of read bytes, or -1
     * @throws Exception if there is an error
     */
    public int bytes(byte[] buf) throws Exception {
        return bin.read(buf);
    }

    /**
     * Closes this reader, as defined by {@link InputStream#close()}. If there
     * is an IO error it is intercepted but not logged, other errors are thrown.
     */
    public void close() {
        try {
            bin.close();
        } catch (IOException e) {/* ignore */
        }
    }

    /**
     * Copies this reader to the specified target, either byte or text. If the
     * target file exists it is silently overwritten, otherwise it is created.
     * If there is an error it is intercepted and written to SWGAide's log file.
     * 
     * @param to the file to write to
     */
    public void copy(File to) {
        try {
            ZWriter.copy(bin, new BufferedOutputStream(new FileOutputStream(to)));
        } catch (Throwable e) {
            SWGAide.printError("ZReader:copy", e);
        }
    }

    /**
     * Returns the next text line as defined by {@link #lineExc()}. If there is
     * an error it is intercepted and written to SWGAide's log file and this
     * method returns {@code null}.
     * 
     * @return a text line, or {@code null}
     */
    public String line() {
        try {
            return txt.readLine();
        } catch (Throwable e) {
            SWGAide.printError("ZReader:line", e);
        }
        return null;
    }

    /**
     * Returns the next text line as defined by {@link #lineExc(boolean)}. If
     * there is an error it is intercepted and written to SWGAide's log file and
     * this method returns {@code null}.
     * 
     * @param notComment {@code true} to skip comments and empty lines
     * @return a trimmed text line, an empty string, or {@code null}
     */
    public String line(boolean notComment) {
        try {
            return lineExc(notComment);
        } catch (Throwable e) {
            SWGAide.printError("ZReader:line", e);
        }
        return null;
    }

    /**
     * Returns the next text line from this reader. This is the text until the
     * next line-termination character, which is excluded. If no further line
     * exists this method returns {@code null}. This method returns the line
     * as-is, compare {@link #lineExc(boolean)}.
     * 
     * @return a text line, or {@code null}
     * @throws IOException if there is an I/O error
     */
    public String lineExc() throws IOException {
        return txt.readLine();
    }

    /**
     * Returns the next text line as defined by {@link #lineExc()} but the line
     * is always trimmed from white spaces before it is returned. The boolean
     * argument determines if the line is validated; {@link #isComment(String)}.
     * 
     * @param notComment {@code false} for any line
     * @return a trimmed text line, an empty string, or {@code null}
     * @throws Exception if there is an error
     */
    public String lineExc(boolean notComment) throws Exception {
        String line;
        while ((line = lineExc()) != null) {
            line = line.trim();
            if (notComment && isComment(line)) continue;

            return line;
        }
        return null;
    }

    /**
     * Returns a list of remaining text lines from this reader, or an empty
     * list. The boolean arguments determine if lines are trimmed and invocation
     * of {@link #isComment(String)}. If there is an error it is intercepted and
     * written to SWGAide's log file and this method returns a list with one
     * that reads "ERROR: ..." with the error message.
     * <p>
     * If anything has been read from this reader before invoking this method
     * only the remaining lines are returned. This method eventually closes this
     * reader.
     * 
     * @param notComment {@code true} to skip comments and empty lines
     * @param trim {@code true} to always trim lines
     * @return a list of strings
     */
    public List<String> lines(boolean notComment, boolean trim) {
        List<String> ret = new ArrayList<String>(128);
        try {
            List<String> ls = linesExc();
            for (String line : ls) {
                if (trim || notComment) {
                    String l = line.trim();
                    if (notComment && isComment(l)) continue;
                    line = trim
                            ? l
                            : line;
                }
                ret.add(line);
            }
        } catch (Throwable e) {
            SWGAide.printError("ZReader:lines", e);
            ret.clear();
            ret.add("ERROR: " + e.getMessage());
        }
        return ret;
    }

    /**
     * Returns a list of remaining text lines from this reader, or an empty
     * list. The lines are not modified.
     * <p>
     * If anything has been read from this reader before invoking this method
     * only the remaining lines are returned. This method eventually closes this
     * reader.
     * 
     * @return a list of strings
     * @throws Exception if there is an error
     */
    public List<String> linesExc() throws Exception {
        ArrayList<String> sl = new ArrayList<String>();
        String line;
        while ((line = lineExc()) != null)
            sl.add(line);

        close();
        return sl;
    }

    /**
     * Returns the line number for the next text line, zero-based. This method
     * does not consider if the next line is valid or not.
     * 
     * @return next line number
     */
    public int next() {
        return txt.getLineNumber(); // this line is yet not read
    }

    /**
     * Returns a list of strings filtered from the specified archive, or an
     * empty list. This method iterates over all entries from the specified
     * archive and if an entry name begins with the specified directory and if
     * its name ends with one of the suffixes in the array of strings this
     * method adds the name of the entry to the returned list; only the name is
     * added.
     * <p>
     * This method verifies that the entry exists in the specified directory and
     * not in a subdirectory. If the directory is the empty string this method
     * returns an empty list. If the array of suffixes contains just one element
     * which is the empty string this method adds all entries of the specified
     * directory (this method trims the one element to determine emptyness).
     * <p>
     * This method is implemented to use "/" as path delimiters, correct this if
     * necessary.
     * 
     * @param zip a Zip or JAR file
     * @param dir a directory path
     * @param ends an array of file suffixes
     * @return a list of file names
     * @throws Exception if there is an error
     * @throws NullPointerException if an arument is {@code null}
     */
    public static List<String> entries(ZipFile zip, String dir, String... ends)
            throws Exception {
    
        List<String> ret = new ArrayList<String>();
        if (dir.isEmpty()) return ret;
    
        String d = dir.endsWith("/")
                ? dir
                : dir + "/";
        boolean add = ends.length == 1 && ends[0].trim().isEmpty();
        int len = d.length();
    
        Enumeration<? extends ZipEntry> ents = zip.entries();
        while (ents.hasMoreElements()) {
            String entry = ents.nextElement().getName();
            if (entry.startsWith(d)
                    && entry.indexOf('/', len) < 0 /* no subfolders */) {
                if (add)
                    ret.add(entry.substring(len));
                else
                    for (String e : ends) {
                        if (entry.endsWith(e)) {
                            ret.add(entry.substring(len));
                            break;
                        }
                    }
            }
        }
        zip.close();
        return ret;
    }

    /**
     * Returns {@code true} if the specified line is a comment. This method a
     * line as a comment if it begins with a comment marker, one of {@code # "#
     * //}, or if the line is empty or {@code null}. This method does not trim
     * or modify the line before checking.
     * 
     * @param line a line, or {@code null}
     * @return {@code true} if the line is a comment
     */
    public static boolean isComment(String line) {
        return line == null || line.isEmpty() || line.startsWith("#")
                || line.startsWith("\"#") || line.startsWith("//");
    }

    /**
     * Creates and returns a byte reader. If there is an error it is intercepted
     * and written to SWGAide's log file, and this method returns {@code null}.
     * 
     * @param file a file for a byte stream
     * @return a byte reader, or {@code null}
     */
    public static ZReader newByteReader(File file) {
        try {
            return new ZReader(new FileInputStream(file), false);
        } catch (Throwable e) {
            SWGAide.printError("ZReader:newByteReader", e);
        }
        return null;
    }

    /**
     * Creates and returns a byte reader. If there is an error it is intercepted
     * and written to SWGAide's log file, and this method returns {@code null}.
     * 
     * @param stream a byte stream
     * @return a byte reader, or {@code null}
     */
    public static ZReader newByteReader(InputStream stream) {
        try {
            return new ZReader(stream, false);
        } catch (Throwable e) {
            SWGAide.printError("ZReader:newByteReader", e);
        }
        return null;
    }

    /**
     * Creates and returns a text reader. If there is an error it is intercepted
     * and written to SWGAide's log file, and this method returns {@code null}.
     * 
     * @param file an abstract file path
     * @return a text reader, or {@code null}
     */
    public static ZReader newTextReader(File file) {
        try {
            return newTextReaderExc(file);
        } catch (Throwable e) {
            SWGAide.printError("ZReader:newTextReader", e);
        }
        return null;
    }

    /**
     * Creates and returns a text reader. If there is an error it is intercepted
     * and written to SWGAide's log file, and this method returns {@code null}.
     * 
     * @param characters a character stream
     * @return a text reader, or {@code null}
     */
    public static ZReader newTextReader(InputStream characters) {
        try {
            return newTextReaderExc(characters);
        } catch (Throwable e) {
            SWGAide.printError("ZReader:newTextReader", e);
        }
        return null;
    }

    /**
     * Creates and returns a text reader; see {@link #newTextReader(File)}.
     * 
     * @param file an abstract file path
     * @return a text reader
     * @throws Exception if there is an error
     */
    public static ZReader newTextReaderExc(File file) throws Exception {
        return new ZReader(new FileInputStream(file), true);
    }

    /**
     * Creates and returns a text reader; see
     * {@link #newTextReader(InputStream)}.
     * 
     * @param chars a character stream
     * @return a text reader
     * @throws Exception if there is an error
     */
    public static ZReader newTextReaderExc(InputStream chars)
            throws Exception {
        return new ZReader(chars, true);
    }

    /**
     * Returns the text content of the specified file as a string. If the file
     * does not exist this method returns {@code null}. If there is an error it
     * is intercepted and written to SWGAide's log file, and this method returns
     * "ERROR: ..." with the error.
     * 
     * @param file an abstract file path
     * @return the text content as a string, or {@code null}
     */
    public static String read(File file) {
        if (!file.exists()) return null;

        try {
            return read(new FileInputStream(file));
        } catch (Throwable e) {
            SWGAide.printError("ZReader:read", e);
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Returns the content of the specified stream as a string, or {@code null}.
     * This method assumes it is a stream of characters. If there is an error it
     * is intercepted and written to SWGAide's log file, and this method returns
     * "ERROR: ..." with the error.
     * 
     * @param in an input stream
     * @return the content as a string, or {@code null}
     */
    public static String read(InputStream in) {
        try {
            ZReader sr = newTextReaderExc(in);
            List<String> sl = sr.lines(false, false);

            if (sl.isEmpty()) return null;
            if (sl.size() == 1) return sl.get(0);

            Iterator<String> iter = sl.iterator();
            ZString z = new ZString(iter.next());
            while (iter.hasNext())
                z.nl().app(iter.next());

            return z.toString();
        } catch (Throwable e) {
            SWGAide.printError("ZReader:read", e);
            return "ERROR: " + e.getMessage();
        }
    }
}
