package swg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import swg.gui.SWGFrame;
import swg.model.SWGUniverse;
import swg.tools.ZNumber;
import swg.tools.ZReader;
import swg.tools.ZString;
import swg.tools.ZWriter;

/**
 * This is the main class which launches the frame.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGAide {

    /**
     * A static reference to the GUI frame of SWGAide. This reference may be
     * used by some static utility classes which occasionally must raise a GUI
     * message dialog.
     */
    private static SWGFrame FRAME;

    /**
     * A flag which is {@code true} if an error has occurred during a session.
     */
    private static boolean hasError = false;

    /**
     * A synchronization lock for the error printer.
     */
    private static final String LOCK_ERROR = "LOCK_ERROR";

    /**
     * A synchronization lock for the log printer.
     */
    private static final String LOCK_LOG = "LOCK_DEBUG";

    /**
     * Repository of optional log groups.
     */
    private static Hashtable<String, Integer> logGroups;

    /**
     * The general log level besides log groups; default is level 1.
     * <p>
     * Valid log levels are:
     * <ol start=0>
     * <li>disabled</li>
     * <li>important messages, the default level</li>
     * <li>"high-level" informative messages</li>
     * <li>standard messages</li>
     * <li>detailed information</li>
     * </ol>
     */
    private static int logLevel = 1;

    /**
     * A string which is specified in SWGAide's INI file, or {@code null}. If
     * {@code swgBaseFolder != null} it denotes the file path for SWG's client
     * folder. This path may equal the path in SWGAide's DAT file, but if the
     * two differs this path is supposed to be correct, however this is not
     * validated by this type.
     */
    private static String swgBaseFolder;

    /**
     * Helper method which determines if the current Java version is acceptable.
     * If the available version is out-dated this method exits the application
     * with an error message.
     */
    private static void checkJavaVersion() {
        String ver = System.getProperty("java.version");
        if (ver.compareTo("1.7.0") >= 0) {
        	if (ver.compareTo("1.9.0") <= 0) {
        		return;
        	}
        }

        ZString msg = ZString.fz("This application requires a Java Runtime%n" +
                "Environment (JRE) version 1.7 or 1.8.%n" +
                "Find a free copy at http://java.com/download%n" +
                "A minimal installation is just the JRE.%n" +
                "Your version: %s", ver);
        printMessage(msg);
        displayExitDialog(msg.toString(), "Error", false);
    }
/**
     * Helper method which returns the filename of SWGAide. Usually SWGAide.jar or SWGAide-PreCU.jar, but the user may have changed the name for some reason.
     * @return the filename of SWGAide
     */
    public static String getFilename() {
        File filename;
        try {
            filename = new File(SWGAide.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if(filename.isFile()) {
                return filename.getName();
            }
            //Possibly running in developer mode, i.e. outside of a packaged file
            return "SWGAide-NGE.jar";
        } catch (URISyntaxException e) {
            return "SWGAide-NGE.jar";
        }
    }

    /**
     * Helper method which determines that java is using the folder with
     * SWGAide.jar as its working directory. Otherwise it is possible that the
     * "jar" file type is associated with something different that javaw.exe and
     * the working directory is elsewhere; hence SWGAide exits.
     */
    private static void checkJavaWorking() {
        if (new File(getFilename()).exists()) return;

        String msg = "Java or Windows error\n" +
                "There is an error that makes Java work in a folder\n" +
                "that is not the folder where you put down SWGAide-NGE.jar\n" +
                "The solution depends on your setup, the README file\n" +
                "suggests one solution: ensure that javaw.exe is the\n" +
                "selected file association for \".jar\" files\n\n" +

				"Exiting" + getFilename();
        displayExitDialog(msg, "Error", false);
    }


    /**
     * Helper method which displays a dialog end potentially exits SWGAide. The
     * specified arguments set the text and style of the dialog. Parameter
     * {@code confirm} determines if the dialog is a plain message dialog or a
     * confirmation dialog for which the message ends in "Exit?". If it is a
     * plain dialog, or if the user selects YES or cancels the dialog, this
     * method exits SWGAide.
     * 
     * @param msg a massage to display
     * @param title a title
     * @param confirm {@code true} for a confirmation dialog with YES/NO
     */
    static void displayExitDialog(String msg, String title, boolean confirm) {
        JFrame f = new JFrame();
        int s = JOptionPane.ERROR_MESSAGE;

        if (confirm) {
            if (JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(
                    f, msg, title, JOptionPane.YES_NO_OPTION, s))
                return; // don't exit
        } else
            JOptionPane.showMessageDialog(f, msg, title, s);

        System.exit(-1);
    }

    /**
     * Returns the frame for SWGAide.
     * 
     * @return SWGAide's frame
     */
    public static final SWGFrame frame() {
        return FRAME;
    }

    /**
     * Helper method which returns an abstract INI file for SWGAide. This file
     * is located in the same folder as SWGAide but it may not yet exist.
     * 
     * @return an INI file
     */
    private static File iniFile() {
        // try the Windows' TXT suffix which is sometimes appended to text files
        // do not rectify if the user has trouble to easily edit the file
        File wFile = new File("SWGAide.INI.txt");
        return wFile.exists()
                ? wFile
                : new File("SWGAide.INI");
    }

    /**
     * Helper method which executes initial house keeping tasks.
     */
    private static void initiate() {
        File fd = new File("logs");
        if (!fd.exists()) fd.mkdir();
    }

    /**
     * The entry point that launches this application. Exceptions of type
     * {@link Throwable} are caught by this method and printed to the system
     * error output, which may eventually be set by the user to an error logging
     * output print stream.
     * <p>
     * The application is essentially launched in this order:
     * <ol>
     * <li>Reads an optional SWGAide.INI file</li>
     * <li>Reads optional startup arguments from command line. These overrides
     * any settings from the INI</li>
     * <li>Sets the log and error output to files respectively</li>
     * <li>Initiates the GUI after which the application is event driven; some
     * background tasks are automated, such as checking for updates, etc.</li>
     * </ol>
     * 
     * @param args command line arguments, these are explained in the INI file
     */
    public static void main(String[] args) {
        try {
            checkJavaWorking();

            initiate();
            setLog(null);

            parseIni();
            parseArgs(args);

            if (!SWGConstants.DEV_DEBUG) setErrorFile();
            checkJavaVersion();

            printStart();
            FRAME = new SWGFrame();
            FRAME.delayedConstructor(FRAME);
        } catch (Throwable e) {
            printError("SWGAide:main", e);
        }
    }

    /**
     * Helper method which makes log-groups from the specified argument. The
     * array contains strings from an INI file or command line arguments. If
     * there are log-groups each one is added to {@link #logGroups} together
     * with its limit. This method returns the index for the element that reads
     * "end-loggroup", or -1 if there are no loggroups or if the argument is
     * invalid.
     * <p>
     * The log-groups must be on the format:
     * <p>
     * <tt>loggroup<br>
     * groupnameOne level<br>
     * groupnameTwo level<br>
     * ...<br>
     * end-loggroup</tt>
     * <p>
     * Log-group names must be unique and must not contain white spaces. It is
     * preferred that they are short because they are printed to the log file.
     * Levels must be integers, in the range [1 4], see {@code logLevel},
     * 
     * @param args the unmodified strings from the INI file or command line args
     * @return an index which denotes "end-loggroup", or -1
     * @throws Exception if there is an exception
     */
    private static int makeLogGroups(String[] args) throws Exception {
        logGroups = new Hashtable<String, Integer>();
        boolean b = false;
        for (int a = 0; a < args.length; ++a) {
            String str = args[a].trim().replaceAll("^-", "");
            String s = str.toLowerCase(Locale.ENGLISH);

            if (b && s.startsWith("end-loggroup"))
                return a;

            if (b) {
                int i = str.indexOf(' ');
                s = str.substring(0, i);
                String t = str.substring(i);
                logGroups.put(s, Integer.valueOf(ZNumber.intExc(t)));
            } else if (s.startsWith("loggroup"))
                b = true;
        }
        return -1;
    }

    /**
     * Helper method which parses the array of arguments and sets their
     * variables respectively. The array of arguments are either from the INI
     * file or the command line arguments, the latter override arguments from
     * the INI file.
     * 
     * @param args a list of arguments to parse
     * @throws Exception if there is an error
     */
    private static void parseArgs(String[] args) throws Exception {
        for (int i = 0; i < args.length; ++i) {
            String str = args[i].trim();
            str = str.replaceAll("^-", "");

            int idx = str.indexOf('=');
            String s = str.toLowerCase(Locale.ENGLISH);

            if (s.equals("help"))
                printUsage(null);
            else if (s.startsWith("swgclientfolder"))
                swgBaseFolder = str.substring(idx + 1).trim();
            else if (s.startsWith("loglevel"))
                setLogLevel(ZNumber.intExc(str.substring(idx + 1).trim()));
            else if (s.startsWith("loggroup")) {
                int j = makeLogGroups(args);
                i = j >= 0
                        // sanity
                        ? j
                        : i;
            } else
                printUsage(String.format("Unknown argument: \"%s\"", str));
        }
    }

    /**
     * Helper method which reads a file named SWGAide.INI. If the file does not
     * exists this method does nothing. If the file exists this method invokes
     * {@link #parseArgs(String[])}.
     * 
     * @throws Exception if there is an I/O error
     */
    private static void parseIni() throws Exception {
        File iFile = iniFile();
        if (iFile.exists()) {
            ZReader in = ZReader.newTextReaderExc(iFile);
            List<String> lines = in.lines(true, true);
            parseArgs(lines.toArray((new String[lines.size()])));
            in.close();
        }
    }

    /**
     * Prints log or debug messages to the general log file. The specified
     * messages are printed if the specified level is &le; {@link #logLevel} and
     * if the log-group and its log-group-level admits &mdash;
     * {@link #makeLogGroups(String[])}.
     * <p>
     * Defined log-groups with their individual levels make it possible to have
     * {@link #logLevel} at default or disabled but to let specific log-groups
     * have higher levels, overriding the general level. However, if
     * {@link #logLevel} is high enough all invocations are serviced. Log levels
     * are:
     * <ol start=0>
     * <li>disabled</li>
     * <li>important messages, default</li>
     * <li>"high-level" informative messages</li>
     * <li>standard messages</li>
     * <li>detailed information</li>
     * </ol>
     * <p>
     * Output format Where TIMESTAMP and group are optional: <br/>
     * <tt>[TIMESTAMP] [group] message</tt> A single white space is added after
     * each string in the message array, hence the clients need not add
     * delimiters.
     * 
     * @param g the log group for the invocation, or {@code null}
     * @param lvl the log level for the invocation, in the range [1 4]
     * @param msg the message, or an array of strings
     */
    @SuppressWarnings("all")
    public static void printDebug(String g, int lvl, String... msg) {
        synchronized (LOCK_LOG) {
            assert (1 <= lvl && lvl <= 4); // Log level out of bounds [1 4]
            assert (msg != null && msg.length > 0);

            int lvlGr = 0;
            if (logGroups != null && g != null) {
                Integer lG = logGroups.get(g);
                lvlGr = (lG == null
                        ? 0
                        : lG.intValue());
            }

            if (SWGConstants.DEV_DEBUG || lvl <= logLevel || lvl <= lvlGr) {
                ZString z = new ZString();
                z.app("[").app(time()).app("] ").app("[").app(g).app("] ");

                for (String m : msg)
                    z.app(m).app(' ');

                printMessage(z);
            }
        }
    }

    /**
     * Prints an error message to the error output stream. If the specified
     * exception is {@code null} the string "== ERROR ==" is prepended to the
     * message. Otherwise "== EXCEPTION ==" is prepended to the argument.
     * <p>
     * The format for the output is, assuming {@code exc != null}:
     * 
     * <pre>
     * [date | version X.Y.Z == EXCEPTION == message: exc.cause]
     *    stack trace:
     *       frame 0
     *          frame 1...</pre>
     * 
     * @param message the message to print, no colon or space is necessary
     * @param exc a {@link Throwable} object, or {@code null}
     */
    public static void printError(String message, Throwable exc) {
        synchronized (LOCK_ERROR) {
            assert (message != null && !message.isEmpty());

            if (FRAME != null) FRAME.errorAlert();
            hasError = true;

            ZString z = new ZString().app("[").app(time());
            z.app(" | version ").app(SWGConstants.version);

            z.app(exc != null
                    ? " == EXCEPTION == "
                    : " == ERROR == ").app(message);

            if (exc != null) z.app(": ").app(exc.getMessage());

            printMessage(z, exc, System.err);
        }
    }

    /**
     * Helper method which prints the specified message to the general log file.
     * 
     * @param z the message to print
     */
    @SuppressWarnings("all")
    private static void printMessage(ZString z) {
        printMessage(z, null, SWGConstants.DEV_DEBUG
                ? System.err
                : System.out);
    }

    /**
     * Helper method which prints the specified message to the specified output
     * stream. If the specified exception is not {@code null} its stack trace is
     * printed.
     * 
     * @param z the message to print
     * @param exc an instance of type {@link Throwable}, or {@code null}
     * @param out the output stream
     */
    private static void printMessage(ZString z, Throwable exc, PrintStream out) {
        out.println(z.toString());
        if (exc != null) exc.printStackTrace(out);
    }

    /**
     * Helper method which prints a startup string to the log file.
     */
    private static void printStart() {
        printMessage(new ZString("=== STARTED: ").app(time()));
    }

    /**
     * Prints a string reading "stop" with a time stamp to the log file. If
     * {@link #hasError} is {@code true} this method prints the same string to
     * the error log file.
     */
    public static void printStop() {
        synchronized (LOCK_LOG) {
            ZString z = ZString.fz("=== STOPPED: %s%n", time());
            printMessage(z);
            if (hasError) printMessage(z, null, System.err);
        }
    }

    /**
     * Helper method which prints the specified message plus usage information
     * and exits the application.
     * 
     * @param msg a message, or {@code null}
     */
    private static void printUsage(String msg) {
        if (msg != null) System.err.println(msg);

        System.err.println("Usage;");
        System.err.println("   java -jar SWGAide.jar <argument[s]>");
        System.err.println("   arguments may be:");
        System.err.println("      loglevel=N   ...where N is an integer");
        System.err.println("Arguments are better kept in the SWGAide.INI file");
        System.err.println("More information is read in the file SWGAide.INI");
        System.err.println("Exiting");
        System.exit(1);
    }

    /**
     * Helper method which sets {@link System#err} to the log file
     * "logs\SWGAide-ERROR.TXT".
     */
    private static void setErrorFile() {
        File f = new File("logs", "SWGAide-ERROR.TXT");
        try {
            System.setErr(new PrintStream(new FileOutputStream(f, true), true));
        } catch (IOException e) {
            printError("SWGAide:setErrFile", e);
        }
    }

    /**
     * Redirects log output to the specified file. if the argument is {@code
     * null} this method sets output to "logs\SWGAide.log". This method returns
     * {@code true} only if the log file is successfully set to the specified
     * file. If there is an IO error it is intercepted and logged.
     * 
     * @param file a file, or {@code null} for default file
     * @return {@code false} if there is an error
     * @throws Exception if there is an error
     */

    public static boolean setLog(File file) throws Exception {
        synchronized (LOCK_LOG) {
            File f = file == null
                    ? new File("logs", "SWGAide.LOG")
                    : file;
            try {
                System.setOut(
                        new PrintStream(new FileOutputStream(f, true), true));
                return true;
            } catch (IOException e) {
                printError("SWGAide:setLog: " + file, e);
            }
            return false;
        }
    }

    /**
     * Sets the general log level which is used for log and debug messages. If a
     * log-group's level is different the greatest value of the two always takes
     * precedence. Valid log levels are:
     * <ol start=0>
     * <li>disabled</li>
     * <li>important messages, the default value</li>
     * <li>"high-level" informative messages</li>
     * <li>standard messages</li>
     * <li>detailed information</li>
     * </ol>
     * 
     * @param level the general log level
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static void setLogLevel(int level) {
        synchronized (LOCK_LOG) {
            if (level < 0 || level > 4) {
                if (FRAME == null)
                    printUsage("Invalid log-level, expected 0 to 4: " + level);
                else
                    throw new IllegalArgumentException(
                            "Invalid level [0 4]: " + level);
            } else
                logLevel = level;
        }
    }

    /**
     * Returns the abstract file for SWG's client folder (a directory), or
     * {@code null}. The returned file may not exist and it may be invalid.
     * 
     * @return a path for SWG's client directory, or {@code null}
     */
    public static File swgPath() {
        return swgBaseFolder == null
                ? null
                : new File(swgBaseFolder).getAbsoluteFile();
    }

    /**
     * Sets the path for SWG's client folder (directory). This method validates
     * the path, see {@link SWGUniverse#isValidSWGPath(File)}; then it updates
     * the local SWGAide.INI file with the specified argument. If the path
     * equals the current path this method does nothing.
     * <p>
     * <b>Notice:</b> as long as SWGAide can find SWG it is OK id the specified
     * path is relative.
     * 
     * @param swgPath a directory path for SWG's client
     * @throws Exception if there is an error
     */
    public static void swgPath(File swgPath) throws Exception {
        if (!SWGUniverse.isValidSWGPath(swgPath))
            throw new IllegalArgumentException("Invalid path: " + swgPath);

        String s = swgPath.getPath();
        if (s.equals(swgBaseFolder)) return;

        swgBaseFolder = s;
        updateIniFile("SWGClientFolder", swgBaseFolder);
    }

    /**
     * Helper method which returns the current time as a string. This is the
     * result from (new Date()).toString().
     * 
     * @return current date-time
     */
    public static String time() {
        return (new Date()).toString();
    }

    /**
     * Helper method which writes the specified key-value pair to the INI file.
     * If the key exists its value is updated, but if the value is {@code null}
     * the previous pair is erased. This method does not validate the key or
     * value for correctness. This method use {@code oldkey.startsWith(key)} to
     * match for an existing key, hence any other key with a similar beginning
     * may falsely match the specified key, be cautious with keys.
     * 
     * @param key a key
     * @param value a value, or {@code null}
     * @throws Exception if there is an error
     */
    public static void updateIniFile(String key, String value) throws Exception {
        File f = iniFile();
        String k = key.trim().toLowerCase(Locale.ENGLISH);

        ZReader sr = ZReader.newTextReaderExc(f);
        List<String> ls = sr.linesExc();
        for (int i = ls.size() - 1; i >= 0; --i) {
            String line = ls.get(i).trim();
            if (ZReader.isComment(line)) continue;
            if (line.toLowerCase(Locale.ENGLISH).startsWith(k)) {
                if (value == null)
                    ls.remove(i);
                else {
                    line = key.trim() + "=" + value.trim();
                    ls.set(i, line); // replace
                }
            }
        }
        sr.close();
        ZWriter wr = ZWriter.newTextWriterExc(f, false);
        wr.write(ls);
        wr.close();
    }
}
