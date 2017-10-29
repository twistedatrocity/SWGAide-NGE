package swg.gui;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarFile;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import swg.SWGAide;
import swg.SWGConstants;
import swg.tools.SimplePrefsKeeper;
import swg.tools.ZHtml;
import swg.tools.ZReader;
import swg.tools.ZString;
import swg.tools.ZWriter;

/**
 * This type executes actions that must run after initialization of SWGAide,
 * once everything is properly set up. Thus {@link #postLaunch()} must be called
 * from code that only executes after the GUI is finished.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGPostLaunch {

    /**
     * An intro text at the index pages, in English.
     */
    private static final String DOCS_INTRO =
            " The documentation in SWGAide is most often per panel, hence,"
                    + "<br/>press <b>&nbsp;F1&nbsp;</b> while at the panel you have "
                    + "questions about.<br/>For further information and for "
                    + "questions, go to<br/>";

    /**
     * The frame for SWGAide.
     */
    private final SWGFrame frame;

    /**
     * Creates an instance of this class type.
     * 
     * @param frame the frame for SWGAide
     */
    SWGPostLaunch(SWGFrame frame) {
        this.frame = frame;
    }

    /**
     * Helper method which removes deprecated keys from the DAT file. The keys
     * to remove are listed inside this method.
     */
    private void cleanDATFile() {
        SimplePrefsKeeper pk = SWGFrame.getPrefsKeeper();

        synchronized (pk) {
            pk.remove("ISDroidOnlySelected");
            pk.remove("ISDroidNotesFileName");
            pk.remove("swgClientDirectory");
            pk.remove("resourceFilterFieldExp");
            pk.remove("resourceHarvesterMaintWarning");
            pk.remove("resourceHarvesterPowerWarning");
            pk.remove("resourceHarvesterHopperWarning");
            pk.remove("resourceHarvesterHopperWarningType");
            pk.remove("currentVersion");
            pk.remove("schemExpGroupTitlesFirstPrio");
            pk.remove("mailClientColumnSize0");
            pk.remove("mailClientColumnSize1");
            pk.remove("mainTabDividerLocation");

            // System.err.println(pk.keySet());
        }
    }

    /**
     * Helper method which copies the content from {@code in} to the specified
     * file as defined by {@link ZReader#copy(File)}.
     * 
     * @param in an input stream
     * @param out a target file
     */
    private void copyFile(InputStream in, File out) {
        ZReader sr = ZReader.newByteReader(in);
        if (sr != null) sr.copy(out);
    }

    /**
     * Helper method which deletes obsolete files. The files to delete are
     * listed inside this method.
     */
    private void deleteObsoleteFiles() {
        File datBak = new File("SWGAide.BAK");
        if (datBak.exists())
            datBak.delete();
    }

    /**
     * Helper method which copies the English documentation files from the JAR
     * file to the SWGAide folder. This method copies all HTML and PNG files
     * from "swg/docs" in the JAR file to [SWGAide]/docs/EN/ which is the
     * default location. It also creates a generic index.html file in SWGAide's
     * folder which just refers into docs, and it creates an index_EN.html file
     * in the docs folder. See swg/docs/developer_readme.txt for further info.
     * <p>
     * If made available, other languages are downloaded separately by SWGAide
     * and unzipped into respective folders (XXX: to do). Help file names are on
     * the form {@code help_feature_subfeature_xx.html} where {@code xx} is a
     * language tag such as en, de, fr, etc.
     * <p>
     * This method only copies files with the suffixes "html" and "png", but not
     * "htm" or any other file. Hence, files for any other use than being copied
     * to the public folder should use the HTM suffix or an appropriate suffix.
     */
    private void docsCopy() {
        try {
            // TODO: make this check for new language ZIP-file in docs folder,
            // at download area check for available language packs for the
            // locale of the user and prompt, otherwise return
            if (isSame()) return;

            final String jPath = "swg/docs";
            final File dPath = new File("docs\\EN");
            makeDirPath(dPath);

            List<String> fList = ZReader.entries(
                    new JarFile("SWGAide.jar"), jPath, ".html", ".png");
            docsCopy(fList, jPath.substring(4), dPath);

            Map<String, String> iMap = docsIndexMap(
                    SWGAide.class.getResourceAsStream("docs/index_en.txt"));

            docsWriteIndex(fList, iMap, dPath);
            docsWriteIndex();
            saveInfoFile("docs/translate.txt");
        } catch (Exception e) {
            SWGAide.printError("SWGPostLaunch:docsCopy", e);
        }
    }

    /**
     * Helper method which copies the listed file names from SWGAide.jar to the
     * specified directory. Not all of the elements in the list are copied, only
     * those that meet the criteria in swg/docs/developer_readme.txt, that is,
     * those with suffixes html and png.
     * <p>
     * This implementation copies the concatenation of {@code name/jarPath} from
     * SWGAide.jar to the specified target directory.
     * <p>
     * <b>Notice:</b> This method only works with SWGAide.jar and no other ZIP
     * archive.
     * 
     * @param names a list of file names
     * @param jarPath the path in SWGAide.jar
     * @param targetDir the target directory
     * @throws NullPointerException if anything is {@code null}
     */
    private void docsCopy(List<String> names, String jarPath, File targetDir) {
        for (String name : names) {
            if (name.endsWith("html") || name.endsWith("png")) {
                InputStream in =
                        SWGAide.class.getResourceAsStream(jarPath + '/' + name);
                File f = new File(targetDir, name);
                copyFile(in, f);
            }
        }
    }

    /**
     * Helper method which returns a collection which maps file-names to strings
     * that should be used in a HTML index file. A line in the specified input
     * stream reads {@code file_name.html Some Text to Use}; the first white
     * space is the delimiter. If there is no entry in the specified input
     * stream the returned map is empty.
     * <p>
     * This method is used for any language. See swg/docs/developer_readme.txt
     * for further info.
     * 
     * @param in the input stream
     * @return a map of file-names to strings
     * @throws IndexOutOfBoundsException if there is an invalid line
     */
    private Map<String, String> docsIndexMap(InputStream in) {
        HashMap<String, String> map = new HashMap<String, String>(29);
        ZReader sr = ZReader.newTextReader(in);
        if (sr != null) {
            for (String s : sr.lines(true, true)) {
                int i = s.indexOf(' ');
                map.put(s.substring(0, i), s.substring(i).trim());
            }
        }
        return map;
    }

    /**
     * Helper method which the specified arguments to the file writer as a HTML
     * link. The written string is on the form {@code <a href="file">text</a>}.
     * 
     * @param wr a file writer
     * @param f a valid file path for the link
     * @param s the displayed text
     * @throws Exception if there is an error
     */
    private void docsLink(ZWriter wr, String f, String s) throws Exception {
        wr.writeExc("<a href=\"");
        wr.writeExc(f);
        wr.writeExc("\">");
        docsWrite(wr, s);
        wr.writelnExc("</a>");
    }

    /**
     * Helper method which splits the argument and returns a pair of strings.
     * The argument is supposed to be in one of three forms:
     * 
     * <pre>
     * help_feature__xx.html
     * help_feature_subfeature_foo_bar_xx.html
     * text_xx.html </pre>
     * <p>
     * The returned array contains the feature part and the sub-feature part. If
     * there is no sub-feature the second slot is {@code null}, this the arrays
     * always is length 2 but the second slot may contain several words in one
     * string. For the third form the text part and {@code null} is returned.
     * 
     * @param str a help file name
     * @return a pair of strings
     */
    private String[] docsSplit(String str) {
        int mainFeature = str.indexOf('_');
        int subFeature = str.indexOf('_', mainFeature + 1);

        // the 3rd form
        if (subFeature < 0)
            return new String[] { ZString.tac(
                    str.substring(0, mainFeature)), null };

        // if no sub-feature the following gives |sf - lang| <= 1
        int language = str.lastIndexOf('_');

        String feat = ZString.tac(str.substring(
                mainFeature + 1, subFeature));

        String subf = null;
        if (Math.abs(language - subFeature) > 1) {
            subf = str.substring(subFeature + 1, language).replace('_', ' ');
            subf = ZString.tac(subf);
        }
        return new String[] { feat, subf };
    }

    /**
     * Helper method which writes the specified strings to the file writer. This
     * method begin and end with a HTML font specification for Arial. The items
     * of the vararg string argument are written without delimiter but they are
     * just sequentially written to the output.
     * 
     * @param wr a file writer
     * @param strings a string
     * @throws Exception if there is an error
     */
    private void docsWrite(ZWriter wr, String... strings) throws Exception {
        wr.writeExc("<font face=\"Arial\">");
        for (String s : strings)
            wr.writeExc(s);
        wr.writelnExc("</font>");
    }

    /**
     * Helper method which write a generic HTML index file in SWGAide's folder.
     * The index file creates links for the different languages that exist in
     * the docs folder.
     * 
     * @throws Exception if there is an error
     */
    private void docsWriteIndex() throws Exception {
        File f = new File("index.html");
        ZWriter wr = ZWriter.newTextWriterExc(f, false);

        wr.writelnExc("<html><head><title>SWGAide &mdash; Help Files</title></head>");

        wr.writelnExc("<body bgcolor=\"#ffffff\"><div align=\"center\">");
        wr.eol();

        docsWrite(wr, DOCS_INTRO);
        docsLink(wr, "http://www.swgcraft.org/forums/viewforum.php?f=43",
                "<nobr>SWGAide @ SWGCraft.org</nobr>");

        wr.writelnExc("<p><p><p>");

        docsWrite(wr, "Documentation is found, partial or complete,<br/>",
                "for the following languages:");
        wr.writeExc("<p>");

        File[] langDirs = new File("docs").listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        // DEVELOPER_NOTE: For language codes, see
        // Locale.LocaleISOData.isoLanguageTable // which leads to
        // LocaleISOData.isoLanguageTable
        if (langDirs == null)
            docsLink(wr, "docs", "ERROR: docs");
        else {
            Arrays.sort(langDirs);
            for (File d : langDirs) {
                String lang = d.getName().toLowerCase(Locale.ENGLISH);

                File idxFile = new File("docs", "index_" + lang + ".html");
                if (!idxFile.exists()) continue; // sanity

                Locale dirLoc = new Locale(lang);
                String langDef = dirLoc.getDisplayLanguage();
                String langEng = dirLoc.getDisplayLanguage(Locale.US);
                docsLink(wr, idxFile.getPath(),
                        ZString.tac(langDef)
                                + " &mdash; "
                                + ZString.tac(langEng));
                wr.writeExc("<p>");
            }
        }

        wr.eol();
        wr.writelnExc("</div></body></html>");
        wr.close();
    }

    /**
     * Helper method which writes a HTML index file for the specified files in
     * the specified directory's parent folder. Links in the index file refers
     * to files that exist in the specified directory. If a file name is a key
     * in the specified map its value is used as text for the link, otherwise a
     * text is constructed from the file name itself.
     * <p>
     * The list of file names is alphabetically sorted and when the index is
     * constructed the files are grouped as is described in the file
     * swg/docs/developer_readme.txt
     * <p>
     * The index file is named index_XX.html where XX is an upper case language
     * code. This code is the specified directories name and must follow the
     * intended style.
     * <p>
     * This method is used for any language. See swg/docs/developer_readme.txt
     * for further info.
     * 
     * @param files a list of file names for the index file
     * @param map a map of file names to texts
     * @param directory the target directory, on the form "docs/XX"
     * @throws Exception if there is an error
     */
    private void docsWriteIndex(List<String> files, Map<String, String> map,
            File directory) throws Exception {

        Collections.sort(files);

        final String lang = directory.getName();
        final String fn = String.format("index_%s.html", lang);

        File f = new File(directory.getParentFile(), fn);
        ZWriter wr = ZWriter.newTextWriterExc(f, false);

        wr.writeExc("<html><head><title>SWGAide &mdash; ");
        wr.writeExc(map.get("main_title"));
        wr.writelnExc("</title></head>");

        wr.writelnExc("<body bgcolor=\"#ffffff\"><div align=\"center\">");
        wr.eol();

        wr.writeExc("<h1>");
        docsWrite(wr, "SWGAide");
        wr.writeExc("</h1><br/><h3>");
        docsWrite(wr, map.get("main_title"));
        wr.writelnExc("</h3>");

        docsWrite(wr, DOCS_INTRO);
        docsLink(wr, "http://www.swgcraft.org/forums/viewforum.php?f=43",
                "<nobr>SWGAide @ SWGCraft.org</nobr>");

        wr.writelnExc("<p><table border=0 cellpadding=1>");

        for (String e : files) {
            if (e.endsWith("png")) continue;

            String m = map.get(e);
            String[] split = docsSplit(e); // with m it still tells main or sub
            wr.writeExc("<tr>");
            if (split[1] == null) {
                wr.writeExc("<td colspan=2>");
                wr.writeExc("&nbsp;<br/>");
                docsLink(wr, lang + "/" + e, m == null
                        ? split[0]
                        : m);
            } else {
                wr.writeExc("<td width=40>&nbsp;</td><td>");
                docsLink(wr, lang + "/" + e, m == null
                        ? split[1]
                        : m);
            }
            wr.writelnExc("</td></tr>");
        }
        wr.writelnExc("</table></div></body></html>");
        wr.close();
    }

    /**
     * Helper method which determines if an updated version of SWGAide is
     * available. If a new version is available this method displays a GUI
     * dialog that informs the user with the options to open a web browser or
     * not to.
     */
    private void existsUpdate() {
        try {
            URL url = new URL(
                    SWGConstants.swgAideURL + SWGConstants.swgVersionTXT);
            final String update = existsUpdate(url);
            if (update != null && !update.startsWith("null")) {
                // we touch Swing GUI, hence a swing safe thread
                SwingUtilities.invokeLater(new Runnable() {

                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void run() {
                        String u = SWGConstants.swgAideURL + "swgaide";
                        String[] opt = { "Open browser", "Another time" };
                        String msg = String.format("SWGAide version %s%n" +
                                "is available, visit%n%s", update, u);
                        int ret = JOptionPane.showOptionDialog(frame, msg,
                                "Update available",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null, opt, opt[0]);

                        if (ret == JOptionPane.OK_OPTION)
                            ZHtml.browser(u);
                    }
                });
            }
        } catch (Exception e) {
            SWGAide.printError("SWGPostLaunch:existsUpdate", e);
        }
    }

    /**
     * Helper method which determines if an updated version of SWGAide is
     * available. This method compares {@link SWGConstants#version} with the
     * content of the on-line version file. If a new version is available this
     * method returns a string reading the new version, otherwise {@code null}
     * is returned.
     * <p>
     * The format for the version string is "M.N.O." where M, N, and O are
     * integers for Major, miNor, and Other; see {@link SWGConstants#version}.
     * The returned string is two lines with the version string plus a line with
     * whatever is read as a second line in the on-line version file.
     * 
     * @param url the URL to the SWGAide version file
     * @return a version string, or {@code null}
     */
    private String existsUpdate(URL url) {
        try {
            ZReader bin = ZReader.newTextReader(url.openStream());
            if (bin != null) {
                String onlineVersion = bin.lineExc(false);
                String dateString = bin.lineExc();
                bin.close();

                if (SWGConstants.version.compareTo(onlineVersion) != 0)
                    return (onlineVersion + "\n" + dateString);
            }
        } catch (Exception e) {
            SWGAide.printDebug("post", 1, "SWGPostLaunch:existsUpdate: " + e);
        }
        return null;
    }

    /**
     * Helper method which determines if this instance of SWGAide is new or
     * updated, or not. This method returns {@code true} the version info in
     * SWGAide DAT file equals {@link SWGConstants#version}.
     * 
     * @return {@code true} if SWGAide is not new or updated
     */
    private boolean isSame() {
        return SWGFrame.getPrefsKeeper().getVersion().
                equals(SWGConstants.version);
    }

    /**
     * Helper method which creates the specified directory path; if the path
     * exists the invocation is void. This method invokes {@link File#mkdirs()}
     * and may throw the exceptions it specify.
     * 
     * @param path the directory to create
     * @throws NullPointerException if an argument is {@code null}
     */
    private void makeDirPath(File path) {
        if (!path.exists())
            path.mkdirs();
    }

    /**
     * Executes a number of post-launch tasks in order. This method should be
     * invoked from code that executes only after initialization is finished.
     * This method can be invoked by any thread because {@link SwingUtilities}
     * is invoked if Swing GUI is touched.
     */
    synchronized void postLaunch() {
        try {
            existsUpdate();
            docsCopy();

            if (isSame()) return; // if no update the following methods never
            // run

            saveInfoFiles();
            deleteObsoleteFiles();
            cleanDATFile();
        } catch (Throwable e) {
            SWGAide.printError("SWGPostLaunch:postLaunch", e);
        }
    }

    /**
     * Helper method which saves the specified file to disk. The specified file
     * is both the file within the SWGAide JAR file relative to the SWGAide
     * class, and the file name within the current working directory. That is,
     * in the JAR file it is swg/foo.txt which is written to file as foo.txt.
     * 
     * @param file the file to read and save
     */
    private void saveInfoFile(String file) {
        InputStream in = SWGAide.class.getResourceAsStream(file);
        copyFile(in, new File(file));
    }

    /**
     * Helper method which determines if this instance of SWGAide equals the
     * previous version and if so this method updates some info files to the
     * folder for SWGAide.
     */
    private void saveInfoFiles() {
        saveInfoFile("SWGAide-history.txt");
        saveInfoFile("SWGAide_README.txt");
        saveInfoFile("mail-TEMPLATE.txt");
    }
}
