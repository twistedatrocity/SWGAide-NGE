package devtools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import swg.model.SWGAliases;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;
import swg.model.SWGStation;
import swg.model.SWGUniverse;
import swg.model.mail.SWGMailFolder;
import swg.tools.SimplePrefsKeeper;

/**
 * This utility prints most the content of an SWGAide.DAT file to a text file.
 * The idea is to be able to view its content and to find errors.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public class TestDatFile {

    /** */
    private static final String EOL = System.getProperty("line.separator");

    /**
     * The file writer that is used globally in this class
     */
    private static FileWriter out;

    /**
     * Runs the workload, which is to load the <code>datFile</code> file (a
     * serialized file) and print its output to <code>outFile</code>
     * 
     * @param datFile
     *            the SWGAide.DAT file to load
     * @param outFile
     *            the file to write to
     */
    private static void doJob(File datFile, File outFile) {
        try {
            out = new FileWriter(outFile, false);
            SimplePrefsKeeper pk = SimplePrefsKeeper.load(datFile);
            printObject(0, pk.getVersion());

            List<String> lst = new ArrayList<String>(pk.keySet());
            Collections.sort(lst);
            printObject(0, "KEYS: ======================================");
            for (String o : lst) {
                printObject(0, o);
                Object oo = pk.get(o);
                printObject(4, oo == null ? "null" : oo.toString());
            }

            printObject(0, "SWG: =======================================");
            SWGUniverse univ = (SWGUniverse) pk.get("swgUniverse");
            printUniverse(univ);

            printObject(0, "TEST_CENTER: ===============================");
            univ = (SWGUniverse) pk.get("swgTestCenter");
            printUniverse(univ);
            
            out.close();
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Starts this utility. The path arguments are both optional, the default
     * files are the files SWGAide.DAT and SWGAide.TXT located in the current
     * project. If you supply arguments and if there is white spaces in them,
     * remember to quote the path(s). In short, it is possible to optionally
     * specify any DAT file, and it is possibly to optionally specify any output
     * file. None, one, or both.
     * 
     * @param args
     *            optional one or two file paths
     */
    public static void main(String[] args) {
        File datFile = new File("SWGAide.DAT");
        File outFile = new File("SWGAide.TXT");
        if (args.length > 0) {
            if (args[0].indexOf("help") >= 0 || args[0].indexOf("?") >= 0) {
                printUsage();
            }
            if (args[0].endsWith("DAT")) {
                datFile = new File(args[0]);
            } else {
                outFile = new File(args[0]);
            }
            if (args.length == 2) {
                if (args[1].endsWith("DAT")) {
                    datFile = new File(args[1]);
                } else {
                    outFile = new File(args[1]);
                }
            }
        }
        if (!datFile.exists()) {
            System.err.println(datFile.toString() + " does not exist, exiting");
            printUsage();
        } else {
            printFiles(datFile, outFile);
            doJob(datFile, outFile);
        }
    }

    /**
     * Prints which files will be used.
     * 
     * @param datFile
     *            the SWGAide.DAT file to load
     * @param outFile
     *            the output file to write to
     */
    private static void printFiles(File datFile, File outFile) {
        System.out.println("Will use...");
        System.out.println("  DAT file: " + datFile.getAbsolutePath());
        System.out.println("  Out file: " + outFile.getAbsolutePath());
    }

    /**
     * Helper method which prints the specified number of white spaces and the
     * specified text to {@link #out}.
     * 
     * @param whites
     *            a number of white spaces for indentation
     * @param text
     *            the text to write
     * @throws IOException
     *             if there is an I/O error
     */
    private static void printObject(int whites, String text) throws IOException {
        for (int i = 0; i < whites; ++i) {
            out.write(" ");
        }
        out.write(text);
        out.write(EOL);
    }

    /**
     * Helper method which prints the specified universe to {@link #out}.
     * 
     * @param univ
     *            the universe to scan
     * @throws IOException
     *             if there is an I/O error
     */
    private static void printUniverse(SWGUniverse univ) throws IOException {
        if (univ == null) {
            printObject(0, "is NULL");
            return;
        }

        printObject(0, univ.toString());
        for (SWGAliases als : univ.aliases())
            printObject(4, als.toString());

        printObject(4, univ.imageAlbum().toString());
        printObject(8, univ.imageAlbum().images().toString());

        for (SWGStation stn : univ.stations()) {
            printObject(4, stn.getName());
            printObject(8, stn.macros().toString());
            printObject(8, stn.notes().toString());

            for (SWGGalaxy glx : stn.galaxies()) {
                printObject(8, glx.getName());

                for (SWGCharacter ch : glx.characters()) {
                    printObject(12, ch.getNameComplete());
                    printObject(16, ch.mailBox().swgAidePath().toString());

                    for (SWGMailFolder fld : ch.mailBox().folders())
                        printObject(20, fld.getName() + " size: " + fld.size());
                }
            }
        }
    }

    /**
     * Prints usage information and exits this utility.
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java TestDatFile");
        System.out.println("    This assumes the file SWGAide.DAT is in the "
            + "current working directory and");
        System.out.println("     prints to SWGAide.TXT, also in cwd");
        System.out.println("  java TestDatFile [\"<path>\\SWGAide.DAT\" ]"
            + "[\"<path>\\<out-file>\"]");
        System.out.println("    One or two files in any order");
        System.exit(0);
    }
}
