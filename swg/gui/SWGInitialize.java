package swg.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import swg.SWGAide;
import swg.SWGConstants;
import swg.gui.common.SWGDoTask;
import swg.gui.common.SWGDoTask.TaskCallback;
import swg.model.SWGAliases;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;
import swg.model.SWGStation;
import swg.model.SWGUniverse;
import swg.model.mail.SWGMailMessage;

/**
 * This class is used to initialize / updates SWGAide. Initialization is either
 * a new install or when there is no DAT file and SWGAide recovers what is found
 * in an existing folder.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGInitialize extends JPanel {

    /**
     * The panel that contains the interactive buttons that will be dynamically
     * updated and replaced while running the logic; this is only used during
     * initialization.
     */
    private JPanel buttons;

    /**
     * The pane to show text upon
     */
    private JEditorPane ed = null;

    /**
     * The frame holding the tabbed pane embracing this panel
     */
    private SWGFrame frame;

    /**
     * The file system folder for SWG.
     */
    private File swgDir = null;

    /**
     * Creates this object for the update logic. Initialization GUI is deferred
     * for lazy creation.
     * 
     * @param appFrame the frame holding the tabbed pane embracing this panel
     */
    SWGInitialize(SWGFrame appFrame) {
        frame = appFrame;
    }

    /**
     * Helper method which aborts the initialization and resets this object.
     */
    private void abort() {
        frame.getTabPane().remove(this);
        frame.putToStatbar("Aborted");
        SWGFrame.getPrefsKeeper().clearAll();
    }

    /**
     * Begins the GUI user interaction which possibly leads to initiating the
     * application. This is the entry point for initialization when no previous
     * DAT file is found. This method invokes {@link #initiateShowLicense()}.
     */
    void iniateStart() {
        frame.putToStatbar("Welcome");
        lazyCreate();

        setHtml("docs/welcome_en.htm");

        JButton initiateButton = new JButton("Next");
        initiateButton.setMnemonic('N');
        initiateButton.setToolTipText("Initiate SWGAide: display license");
        initiateButton.addActionListener(new AbstractAction() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                initiateShowLicense();
            }
        });
        buttons.add(initiateButton);

        String tabTitle = "Initiating";
        frame.getTabPane().addTab(tabTitle, this);
        int index = frame.getTabPane().indexOfTab(tabTitle);
        frame.getTabPane().setSelectedIndex(index);
    }

    /**
     * Helper method which presents the license agreement. This method is
     * invoked by the user action in {@link #iniateStart()} and continues with
     * {@link #initiateSwgFind(boolean)}.
     */
    private void initiateShowLicense() {
        buttons.removeAll();

        frame.putToStatbar("License Agreement");
        setHtml("docs/license_en.html");

        JButton initiateButton = new JButton("Accept");
        initiateButton.setMnemonic('A');
        initiateButton.setToolTipText("Accept the license agreement");
        initiateButton.addActionListener(new AbstractAction() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                initiateSwgFind(true);
            }
        });
        buttons.add(initiateButton);

        JButton cancel = new JButton("Decline");
        cancel.setMnemonic('D');
        cancel.setToolTipText("Decline the license agreement");
        cancel.addActionListener(new AbstractAction() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                initiateSwgFind(false);
            }
        });
        buttons.add(cancel);
        initiateButton.requestFocusInWindow();
    }

    /**
     * Helper method which continues with the step that displays initialization
     * information and starts scanning for SWG. The boolean argument determines
     * if the user accepted or declined the license in the previous step. If the
     * argument is {@code false} a dialog is displayed and the user selects to
     * abort or to continue.
     * <p>
     * This method is invoked by the user action in
     * {@link #initiateShowLicense()} and continues with
     * {@link #initiateUniverse(boolean)}.
     * 
     * @param accept {@code true} if the user accepts the license agreement
     */
    private void initiateSwgFind(boolean accept) {
        buttons.removeAll();
        buttons.repaint();
        if (!accept && JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                this, "Abort initialization?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
            abort();
            return;
        }

        frame.putToStatbar("Initiating");
        setHtml("docs/initiate_en.htm");

        final JLabel initLabel = new JLabel(
                "Step 1: Is scanning for StarWarsGalaxies on C:\\      ");

        final JButton okButton = new JButton("OK");
        okButton.setEnabled(false);
        okButton.setMnemonic('O');
        okButton.setToolTipText("Continue");
        okButton.addActionListener(new AbstractAction() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                initiateUniverse(true);
            }
        });
        final JButton skip = new JButton("Skip scanning");
        skip.setMnemonic('S');
        skip.setToolTipText("Find SWG manually");
        // find SWG client directory while waiting for user input but defer
        // start until end of this method
        final SwingWorker<File, Void> worker = new SwingWorker<File, Void>() {
            @Override
            public File doInBackground() {
                return new File(SWGUniverse.findClientDir(), "testcenter");
            }

            @SuppressWarnings("synthetic-access")
            @Override
            public void done() {
                try {
                    swgDir = get();
                    frame.setCursor(Cursor.getPredefinedCursor(
                            Cursor.DEFAULT_CURSOR));

                    skip.setEnabled(false);
                    initLabel.setText(swgDir == null
                            ? "Step 1: Could not find StarWarsGalaxies on C:\\         "
                            : "Step 1: Found " + swgDir.getAbsolutePath()
                                    + "        ");

                    okButton.setEnabled(true);
                    okButton.requestFocusInWindow();
                } catch (Exception e) {/* ignore */
                }
            }
        };

        JButton cancel = new JButton("Cancel");
        cancel.setMnemonic('C');
        cancel.setToolTipText("Find manually or Abort initialization");
        cancel.addActionListener(new AbstractAction() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                worker.cancel(true);
                initiateUniverse(false);
            }
        });

        skip.addActionListener(new AbstractAction() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                worker.cancel(true);
                initiateUniverse(true);
            }
        });

        buttons.add(initLabel);
        buttons.add(okButton);
        buttons.add(cancel);
        buttons.add(skip);

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        worker.execute();
    }

    /**
     * Helper error catching wrapper for {@link #initiateUniverseReal(boolean)}.
     * If there is an error it is caught and logged to SWGAide's error log file.
     * 
     * @param isContinue as defined by {@link #initiateUniverseReal(boolean)}
     */
    private void initiateUniverse(boolean isContinue) {
        try {
            initiateUniverseReal(isContinue);
        } catch (Throwable e) {
            SWGAide.printError("SWGInitialize:iniateUniverse", e);
        }
    }

    /**
     * Helper method which requires the user to select a valid file path for
     * SWG. The boolean argument determines if the user canceled the action in
     * the previous step. If the argument is {@code false} a dialog is displayed
     * and the user confirms to abort or decides to continue.
     * <p>
     * This method is invoked by the user action in
     * {@link #initiateSwgFind(boolean)} and continues with
     * {@link #scanAll(SWGUniverse, boolean)}.
     * 
     * @param isCont {@code true} of OK was clicked, the initialization will
     *        continue, {@code false} will abort initialization
     * @throws Throwable if there is an error
     */
    private void initiateUniverseReal(boolean isCont) throws Throwable {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        buttons.removeAll();
        buttons.repaint();
        if (!isCont && JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                this, "Abort initialization?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
            abort();
            return;
        }

        frame.putToStatbar("Select folder");
        final JLabel initLabel = new JLabel(
                "Step 1: Find / Accept folder for StarWarsGalaxies");
        buttons.add(initLabel);

        JFileChooser fc = SWGFrame.getFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setSelectedFile(swgDir);
        String msg = "Select folder for StarWarsGalaxies (step 1)";

        while ((swgDir == null
                || (fc.getSelectedFile() != null
                    && fc.getSelectedFile().getName().equals("testcenter"))
				) && !(fc.getSelectedFile() != null && SWGUniverse.isValidSWGPath(fc.getSelectedFile()))) {

            if (swgDir != null) fc.setSelectedFile(swgDir);
            int retVal = fc.showDialog(this, msg);
            if (retVal != JFileChooser.APPROVE_OPTION
                    && JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                            this, "ABORT\nAbort initialization?", "Confirm",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE)) {
                abort();
                return;
            }
            msg = "Select valid folder (step 1)";
        }

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        swgDir = fc.getSelectedFile();
        final SWGUniverse universe = new SWGUniverse(swgDir, false);

        SWGFrame.getPrefsKeeper().add("swgUniverse", universe);

        testCenterFind(universe);

        final JButton okButton = new JButton("Done");
        okButton.setEnabled(false);
        okButton.setMnemonic('D');
        okButton.setToolTipText("Finished the initialization");
        okButton.addActionListener(new AbstractAction() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                frame.setCursor(Cursor.getPredefinedCursor(
                        Cursor.WAIT_CURSOR));
                progressRemove();
                frame.populateTabPane(0);
                frame.setCursor(Cursor.getPredefinedCursor(
                        Cursor.DEFAULT_CURSOR));
            }
        });
        buttons.add(okButton);

        initLabel.setText("Executing step 2      ");
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    scanAll(universe, true);
                } catch (Exception e) {
                    SWGAide.printError("SWGInitialize:iniateUniverseReal", e);
                }
                return null;
            }

            @SuppressWarnings("synthetic-access")
            @Override
            protected void done() {
                frame.setCursor(
                        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                okButton.setEnabled(true);
                okButton.requestFocusInWindow();
            }
        };
        worker.execute();
    }

    /**
     * Helper method which creates the GUI elements for initialization of
     * SWGAide which is only done when SWGAide is launched for the first time.
     */
    private void lazyCreate() {
        this.setLayout(new BorderLayout());

        ed = new JEditorPane();
        ed.setEditable(false);
        JScrollPane jsp = new JScrollPane(ed);
        int vs = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        jsp.setVerticalScrollBarPolicy(vs);
        this.add(jsp, BorderLayout.CENTER);

        buttons = new JPanel();

        this.add(buttons, BorderLayout.PAGE_END);
    }

    /**
     * Helper method that removes updating progress from stat bar and log bars.
     */
    private void progressRemove() {
        try {
            SwingUtilities.invokeLater(new Runnable() {

                @SuppressWarnings("synthetic-access")
                public void run() {
                    frame.putToLogbar_1(null);
                    frame.putToLogbar_2(null);

                    buttons.removeAll();
                    buttons.repaint();
                    int i = frame.getTabPane().indexOfTab("Initiating");
                    frame.getTabPane().remove(i);
                }
            });
        } catch (Exception e) {/* Ignore */
        }
    }

    /**
     * Publishes the progress of scanning the universe at the GUI log bars. If
     * the boolean argument is {@code false} this method does nothing.
     * 
     * @param str the string to present, {@code null} will retain the previous
     *        string, an empty string will replace/erase the previous string
     * @param step the value to set to the progress bar. Note, the value is not
     *        accumulating but is the current result, ranging 0 to 100.
     * @param first {@code true} if this is first-time initialization
     */
    private void publishProgess(final String str, final int step, boolean first) {
        if (first) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @SuppressWarnings("synthetic-access")
                    public void run() {
                        if (str != null)
                            frame.putToLogbar_1(str);
                        frame.progressBar.setValue(step);
                    }
                });
            } catch (Exception e) {
                SWGAide.printError("SWGInitialize:publishProgress:", e);
            }
        }
    }

    /**
     * This method is the entry point to updating the specified universe. Hence
     * this method is invoked each time SWGAide is launched, either from
     * SWGFrame's constructor or from {@link #initiateUniverse(boolean)} if it
     * is a first-time initialization. This implementation invokes helper
     * methods which scans for aliases, screen-shots, stations, galaxies, and
     * characters, in that order. This method finally initiates a scan for new
     * mails on another background thread before it returns.
     * 
     * @param universe the universe to scan
     * @param b {@code true} if it is a first-time scan
     * @throws Exception if there is an error
     */
    void scanAll(final SWGUniverse universe, boolean b) throws Exception {
        frame.putToStatbar("Scanning SWG directories");
        frame.putToLogbar_2(frame.progressBar);

        final SWGUniverse tc = testCenterFind(universe);

        publishProgess("Scanning for aliases", 1, b);
        scanForAliases(universe);
        scanForAliases(tc);

        publishProgess("Scanning for station(s)", 20, b);
        scanForStations(universe);
        publishProgess(null, 25, b);
        scanForStations(tc);

        publishProgess("Scanning for galaxies", 30, b);
        scanForGalaxies(universe, 30, 35);
        publishProgess("Scanning for galaxies (TC)", 35, b);
        scanForGalaxies(tc, 35, 40);

        publishProgess("Scanning for characters", 40, b);
        scanForCharacters(universe, 40, 65);
        publishProgess("Scanning for characters (TC)", 65, b);
        scanForCharacters(tc, 65, 70);

        if (((Boolean) SWGFrame.getPrefsKeeper().get(
                "mailCopyToSWGAide", Boolean.TRUE)).booleanValue()) {
            // otherwise on demand and first-time after update to 0.9.0

            final ExecutorService exec = Executors.newSingleThreadExecutor();

            exec.execute(new Runnable() {

                @SuppressWarnings("synthetic-access")
                @Override
                public void run() {
                    scanForMails(universe);
                    scanForMails(tc);
                    if (SWGMailMessage.hasError)
                        JOptionPane.showMessageDialog(frame,
                                "Problems reading some mails.\nSee the \"logs" +
                                        "\\mail-error.txt\" for details",
                                "Error parsing mails",
                                JOptionPane.ERROR_MESSAGE);

                    frame.beginPostLaunchTasks();
                    exec.shutdown();
                }
            });
        }
        publishProgess("", 100, b);
        frame.putToStatbar("Scanning finished");
    }

    /**
     * Helper method which scans the specified universe for new aliases files
     * and adds them to the universe. This method does no longer remove aliases
     * files that does not exist because if SWGAide or the DAT file is moved to
     * another computer these may not exist but should be retained.
     * 
     * @param universe the universe to scan
     */
    private void scanForAliases(SWGUniverse universe) {
        if (universe == null)
            return;

        List<SWGAliases> knownAls = universe.aliases();
        List<SWGAliases> newAls = SWGAliases.scanForNewAliasesFiles(universe);
        for (SWGAliases al : newAls)
            knownAls.add(al);
    }

    /**
     * Scans all galaxies for new characters and removed characters within the
     * specified universe's file system, this is within SWG. This implementation
     * scans each station contained by the universe, and for each station it
     * scans its galaxies, and for each galaxy new or deleted characters are
     * handled accordingly. Only the SWGAide.DAT file is affected by this scan,
     * the file system is not mutated.
     * 
     * @param univ the universe to scan
     * @param base the log status base value
     * @param limit the log status upper limit
     */
    private void scanForCharacters(final SWGUniverse univ, int base, int limit) {
        if (univ == null) return;

        // TODO: walk through all initialization steps and slim and grease them,
        // first of all, make as many file paths as possible relative to pave
        // the way so that the client path can easily be edited by the user

        double up = ((double) limit - base);
        if (!univ.stations().isEmpty())
            up = up / univ.stations().size();
        double sup = base;

        for (SWGStation stn : univ.stations()) {
            double gp = up;
            if (stn.galaxies().size() > 0)
                gp = up / stn.galaxies().size();
            double gup = sup;

            for (SWGGalaxy gxy : stn.galaxies()) {
                List<SWGCharacter> lst = SWGCharacter.scanForNewCharacters(gxy);
                double cp = gp;
                if (lst.size() > 0)
                    cp = gp / lst.size();
                double cup = gup;

                for (SWGCharacter ch : lst) {
                    gxy.characterAdd(ch);
                    cup += cp;
                    publishProgess(null, (int) cup, true);
                }
                gup += gp;
                publishProgess(null, (int) gup, true);
            }
            sup += up;
            publishProgess(null, (int) sup, true);
        }

        frame.addExitCallback(new SWGDoTask(new TaskCallback() {
            @Override
            public void execute() {
                for (SWGStation s : univ.stations())
                    for (SWGGalaxy g : s.galaxies())
                        for (SWGCharacter c : g.characters())
                            c.mailBox().doExit();
            }
        }));

        publishProgess(null, limit, true);
    }

    /**
     * Scans all stations for new galaxies and removed galaxies within the
     * specified universe's file system, this is within SWG. This implementation
     * scans each station contained by the universe, and for each station new or
     * removed galaxies are handled accordingly. Only the SWGAide.DAT file is
     * affected by this scan, the file system is not modified.
     * 
     * @param univ the universe to scan
     * @param base the status log base value
     * @param limit the status log upper limit
     */
    private void scanForGalaxies(SWGUniverse univ, int base, int limit) {
        if (univ == null)
            return;

        double up = univ.stations().size() / ((double) limit - base);

        for (SWGStation stn : univ.stations()) {
            if (stn != null)
                for (SWGGalaxy gxy : SWGGalaxy.scanForNewGalaxies(stn))
                    stn.galaxyAdd(gxy);

            publishProgess(null, (int) (base + up++), true);
        }
        publishProgess(null, limit, true);
    }

    /**
     * Scans the specified universe for mails. This implementation scans all
     * stations, and for each station it scans its galaxies, and for each galaxy
     * its characters, and for each character its mails.
     * <p>
     * The first phase is just done within SWGAide's mail folder and does not
     * change anything at the hard disk. Stray mails are picked up and added to
     * the character's mailbox. Mails that are deleted within SWGAide's mail
     * folder are removed from the mailbox.
     * 
     * @param univ the universe to scan
     */
    private void scanForMails(SWGUniverse univ) {
        if (univ == null) return;

        Collection<SWGStation> stations = univ.stations();
        for (SWGStation stn : stations) {
            List<SWGGalaxy> galaxies = stn.galaxies();
            for (SWGGalaxy gxy : galaxies) {
                List<SWGCharacter> chars = gxy.characters();
                for (SWGCharacter ch : chars) {
                    ch.mailBox().fetch();
                }
            }
        }
    }

    /**
     * Scans the specified universe for stations. This implementation removes
     * from the universe stations which are not found at the hard disk anymore
     * and it adds any new stations that it finds. The removal/adding just
     * affects the file SWGAide.DAT and does not mutate the file system.
     * <p>
     * Stations that have disappeared may be the result of a player that has
     * deleted them from the hard disk, or if the player has installed SWG at
     * new computer and does not play at these stations anymore or has not yet
     * played at them. In the latter case he also copied the file SWGAide.DAT to
     * the new computer.
     * 
     * @param univ the universe to scan
     * @throws Exception if there is an error
     */
    private void scanForStations(SWGUniverse univ) throws Exception {
        if (univ == null)
            return;

        // better safe than sorry
        univ.stationRemove("Lfg");

        for (SWGStation stn : SWGStation.scanForNewStations(univ))
            univ.stationAdd(stn);
    }

    /**
     * Helper method that sets the content of the specified file to the editor
     * pane for this instance.
     * 
     * @param url a path to a document
     */
    private void setHtml(String url) {
        URL u = SWGAide.class.getResource(url);
        if (u != null) {
            try {
                ed.setPage(u);
            } catch (Exception e) {
                SWGAide.printError("SWGInitialize:setHtml:" + url, e);
            }
        } else
            SWGAide.printError(
                    "SWGInitialize:setHtml: no " + url, null);
    }

    /**
     * Helper method which determines if SWG also contains a test-center
     * directory and returns an object for it if it exists. This is if the user
     * also plays or played at TestCenter. TC contains a file structure that
     * equals to the base SWG client, just one folder deeper. The argument
     * determines the base directory for SWGAide. If the test-center does not
     * already exist in the file SWGAide.DAT it is created but not populated. If
     * no test-center exists {@code null} is returned.
     * 
     * @param swg the common SWG universe
     * @return a test-center object, or {@code null}
     */
    private SWGUniverse testCenterFind(SWGUniverse swg) {
        SWGUniverse testCenter = (SWGUniverse)
                SWGFrame.getPrefsKeeper().get("swgTestCenter");

        // XXX: testcenter may be empty but exists in the DAT file, how to deal
        // with that now? If it also has data in the SWGAide file structure we
        // cannot really remove it
        if (testCenter != null)
            return testCenter;

        File tc = new File(swg.swgPath(), "testcenter");
        File tcp = new File(tc, "profiles");
        if (tcp.exists()) {
            try {
                testCenter = new SWGUniverse(tc, true);
                SWGFrame.getPrefsKeeper().add("swgTestCenter", testCenter);
                return testCenter;
            } catch (Throwable e) {
                SWGAide.printDebug(
                        "init", 1, "SWGUniverse:testCenterFind: " + e);
            }
        }
        return null;
    }

    /**
     * Updates various data structures to the current version. This method
     * invokes helper methods and if an upgrade is required the helper method
     * carries out the task. Otherwise the helper methods do nothing.
     * <p>
     * Updates may depend on either the current version versus the version of
     * the loaded DAT file, or on the state of some member of some object.
     * <p>
     * This method obtains any required objects by itself.
     */
    static void update() {
        if (SWGFrame.getPrefsKeeper().getVersion().compareTo(
                SWGConstants.version) >= 0) return;

        SWGUniverse u, tc;
        u = (SWGUniverse) SWGFrame.getPrefsKeeper().get("swgUniverse");
        tc = (SWGUniverse) SWGFrame.getPrefsKeeper().get("swgTestCenter");

        if (SWGFrame.getPrefsKeeper().getVersion().compareTo("0.9.0") < 0) {
            updateMails(u);
            updateMails(tc);
        }
        // if...
    }

    /**
     * This method determines if a dialog should be displayed the user or not.
     * Depending on the specified argument it displays one text or the other,
     * otherwise it does nothing. If several updates will take place this method
     * only displays the oldest message, but all updates will take place.
     * <p>
     * The version string must resemble {@link SWGConstants#version}.
     * 
     * @param version a version string
     */
    public static void updateDialog(String version) {
        String msg = null;
        if (version.compareTo("0.9.0") < 0) {
            msg = "SWGAide will update the SWGAide.DAT file.\n" +
                    "If you have not read \"README_UPDATE.txt\"\n" +
                    "you should exit now and read first!\n\nExit SWGAide?";
            if (JOptionPane.NO_OPTION != JOptionPane.showConfirmDialog(
                    SWGAide.frame(), msg, "Exit SWGAide?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE)) System.exit(-1);

            msg = "This update may take several minutes";
        }

        if (msg != null)
            JOptionPane.showMessageDialog(SWGAide.frame(),
                    msg, "Alert", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Updates mails and folders of all mail-boxes to current version. If the
     * mail-folders of this box are already up-to-date this method does nothing.
     * 
     * @param u a universe
     */
    private static void updateMails(SWGUniverse u) {
        if (u != null) {
            for (SWGStation stn : u.stations())
                for (SWGGalaxy gxy : stn.galaxies())
                    for (SWGCharacter ch : gxy.characters())
                        ch.mailBox().update();
        }
    }
}
