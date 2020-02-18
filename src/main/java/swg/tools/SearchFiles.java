package swg.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * This is a class of utilities for finding files and directories as well as
 * searching for text strings
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public class SearchFiles {

    /**
     * Checks whether <code>file</code> contains <code>str</code> or not,
     * case sensitive
     * 
     * @param file
     *            the file to search
     * @param str
     *            the text string to search for
     * @return <code>true</code> if <code>f</code> contains <code>str</code>
     *         at least once, <code>false</code> otherwise
     * @throws IOException
     *             if anything happened
     */
    public static boolean containsString(File file, String str)
            throws IOException {
        return containsString(file, str, false);
    }

    /**
     * Checks whether <code>file</code> contains <code>str</code> or not
     * 
     * @param file
     *            the file to search
     * @param str
     *            the text string to search for
     * @param ignoreCase
     *            <code>true</code> if any case is sufficient,
     *            <code>false</code> otherwise
     * @return <code>true</code> if <code>f</code> contains <code>str</code>
     *         at least once, <code>false</code> otherwise
     * @throws IOException
     *             if anything happened
     */
    public static boolean containsString(File file, String str,
            boolean ignoreCase) throws IOException {
        String st = str;
        if (ignoreCase) st = str.toLowerCase(Locale.ENGLISH);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                if (ignoreCase) line = line.toLowerCase(Locale.ENGLISH);
                if (line.indexOf(st) >= 0) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            br.close();
        }
        return false;
    }

    /**
     * Recursively get the number of files in root directory <code>drive</code>,
     * for DOS/Windows systems
     * 
     * @param drive
     *            the drive to get its size for, such as C
     * @return the number of files in <code>drive</code>
     */
    public static int fileSystemSize(String drive) {
        File[] fl = new File(drive + ":\\").listFiles();
        return size(fl);
    }

    /**
     * Helper method to searchForDir, doing the breadth-first search on the
     * first-level directories within <code>startDir</code>
     * 
     * @param name
     *            the directory name to find
     * @param startDir
     *            the directory to start the search within
     * @return a directory denoted by <code>name</code>, <code>null</code>
     *         if none was found
     */
    private static File findDir(String name, File[] startDir) {
        if (startDir == null) return null;
        for (File f : startDir) {
            if (f != null) {
                if (f.getName().equals(name)) return f;

                File[] lst = f.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });

                if (lst != null) {
                    for (File ft : lst) {
                        if (ft != null && ft.getName().equals(name)) return ft;
                    }
                }
            }
        }
        // else depth-first
        return findDirRecursive(name, startDir);
    }

    /**
     * Helper method for findDir which recursively searches all directories from
     * <code>startDir</code> and returns the first occurrence of
     * <code>name</code> it finds
     * 
     * @param name
     *            the name to search for
     * @param startDir
     *            the directory to start the search within
     * @return a directory denoted by <code>name</code>, <code>null</code>
     *         if none was found
     */
    private static File findDirRecursive(String name, File[] startDir) {
        if (startDir == null) return null;
        for (File f : startDir) {
            if (f != null) {
                if (f.getName().equals(name)) return f;

                File[] lst = f.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });
                File t = findDirRecursive(name, lst);
                if (t != null && t.getName().equals(name)) return t;
            }
        }
        return null;
    }

    /**
     * Helper method to searchForFile
     * 
     * @param name
     *            the name of the file to find
     * @param startDir
     *            the directory to start searching at
     * @return a file object with <code>name</code>, <code>null</code> if
     *         none was found
     */
    private static File findFile(String name, File[] startDir) {
        if (startDir == null) return null;
        for (File f : startDir) {
            if (f == null) {
                continue;
            } else if (f.isFile()) {
                if (f.getName().equals(name)) return f;
            } else {
                File ret = findFile(name, f.listFiles());
                if (ret != null && ret.getName().equals(name)) return ret;
            }
        }
        return null;
    }

    /**
     * A test drive
     * 
     * @param args
     *            possible arguments
     */
    public static void main(String[] args) {
        File clientDir = searchForDir("StarWarsGalaxxxies", null);
        File client = searchForFile("SwgClient_r.exxxe", clientDir);

        System.out.println(client);
    }

    /**
     * Recursively search for the specified directory name. This method first
     * uses a breadth-first search on the first-level directories in "C:\" and
     * if necessary performs depth-first search. The returned directory is the
     * first occurrence of <code>name</code> using the mentioned scheme.
     * 
     * @param name the file name to search for
     * @param startDir the directory to start the search within, or
     *        <code>null</code> to start from <code>C:\</code>
     * @return the file object of the search result, <code>null</code> if no
     *         such object was found
     * @throws IllegalArgumentException <code>startDir</code> is not a directory
     *         or <code>name</code> is invalid
     */
    public static File searchForDir(String name, File startDir) {
        File sd = startDir;
        if (sd == null)
            sd = new File("C:\\");
        else if (!sd.isDirectory()) { throw new IllegalArgumentException(
                    "Bad Argument: startDir is not a directory: "
                            + sd.getAbsolutePath()); }
        if (name == null || name.isEmpty()) {
            String str = "Bad Argument: name is " + (name == null
                    ? "null"
                    : "empty");
            throw new IllegalArgumentException(str);
        }

        File[] lst = sd.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        return findDir(name, lst);
    }

    /**
     * Recursively search for the specified file name using depth-first search.
     * <p>
     * NOTE: This method will return the very first occurence of the specified
     * name, even though several other files with the same name may exist.
     * 
     * @param name
     *            the name to search for
     * @param startDir
     *            the directory to start the search within, or <code>null</code>
     *            to start from <code>C:\</code>
     * @return the file object of the search result, <code>null</code> if no
     *         such object was found. This is the very first occurence found of
     *         the specified name.
     */
    public static File searchForFile(String name, File startDir) {
        File sd = startDir;
        if (sd == null) sd = new File("C:\\");
        else if (!sd.isDirectory()) {
            throw new IllegalArgumentException(
                    "Bad Argument: startDir is not a directory: "
                            + sd.getAbsolutePath());
        }

        return findFile(name, sd.listFiles());
    }

    /**
     * Returns the number of files in the specified file, if it is a directory
     * the count is done recursively, otherwise the response is <code>1</code>
     * 
     * @param file
     *            the file to start at
     * @return <code>1</code> if <code>file</code> is a plain file, the
     *         number of files contained in the tree stemming from
     *         <code>file</code> if it is a directory
     */
    public static int size(File file) {
        return size(file.listFiles());
    }

    /**
     * Returns the number of files in the specifies list of files, recursively
     * computed
     * 
     * @param fileList
     *            the list of files to scan
     * @return the number of files in the specified list of files
     */
    public static int size(File[] fileList) {
        if (fileList == null) return 0;
        int size = 0;
        for (File f : fileList) {
            if (f == null) {
                // do nothing
            } else if (f.isDirectory()) {
                size += size(f.listFiles());
            } else { // is file
                ++size;
            }
        }
        return size;
    }
}
