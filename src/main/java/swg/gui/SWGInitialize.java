package swg.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
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
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import swg.SWGAide;
import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.gui.common.SWGDoTask;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGDoTask.TaskCallback;
import swg.model.SWGAliases;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;
import swg.model.SWGStation;
import swg.model.SWGUniverse;
import swg.swgcraft.SWGCraftCache;
import swg.swgcraft.SWGCraftCache.CacheUpdate.UpdateType;

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
        frame.putToStatbar("Aborted",null);
        SWGFrame.getPrefsKeeper().clearAll();
    }

    /**
     * Begins the GUI user interaction which possibly leads to initiating the
     * application. This is the entry point for initialization when no previous
     * DAT file is found. This method invokes {@link #initiateShowLicense()}.
     */
    void iniateStart() {
        frame.putToStatbar("Welcome",null);
        lazyCreate();

        setHtml("docs/welcome_en.htm");

        JButton initiateButton = new JButton("Next");
        initiateButton.setMnemonic('N');
        initiateButton.setToolTipText("Initiate SWGAide: display license");
        initiateButton.addActionListener(new AbstractAction() {
            
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

        frame.putToStatbar("License Agreement",null);
        setHtml("docs/license_en.html");

        JButton initiateButton = new JButton("Accept");
        initiateButton.setMnemonic('A');
        initiateButton.setToolTipText("Accept the license agreement");
        initiateButton.addActionListener(new AbstractAction() {
            
            public void actionPerformed(ActionEvent e) {
                initiateSwgFind(true);
            }
        });
        buttons.add(initiateButton);

        JButton cancel = new JButton("Decline");
        cancel.setMnemonic('D');
        cancel.setToolTipText("Decline the license agreement");
        cancel.addActionListener(new AbstractAction() {
            
            public void actionPerformed(ActionEvent e) {
                initiateSwgFind(false);
            }
        });
        buttons.add(cancel);
        initiateButton.requestFocusInWindow();
    }

    /**
     * Helper method which continues with the step that displays initialization
     * information and starts dialog for choosing SWG folder. The boolean argument determines
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
            System.exit(0);
            return;
        }

        frame.putToStatbar("Initiating",null);
        setHtml("docs/initiate_en.htm");

        final JButton choose = new JButton("Select SWG Game Folder");
        choose.setMnemonic('C');
        choose.setToolTipText("Find SWG manually");

        JButton cancel = new JButton("Cancel");
        cancel.setMnemonic('C');
        cancel.setToolTipText("Abort initialization");
        cancel.addActionListener(new AbstractAction() {
            
            public void actionPerformed(ActionEvent e) {
                initiateUniverse(false);
            }
        });

        choose.addActionListener(new AbstractAction() {
            
            public void actionPerformed(ActionEvent e) {
                initiateUniverse(true);
            }
        });

        buttons.add(choose);
        buttons.add(cancel);
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
    @SuppressWarnings("unchecked")
	private void initiateUniverseReal(boolean isCont) throws Throwable {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        buttons.removeAll();
        buttons.repaint();
        if (!isCont && JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                this, "Abort initialization?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
            abort();
            System.exit(0);
            return;
        }

        frame.putToStatbar("Select folder",null);
        final JLabel initLabel = new JLabel(
                "Step 1: Select main SWG game folder");
        buttons.add(initLabel);

        JFileChooser fc = SWGFrame.getFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setSelectedFile(swgDir);
        String msg = "Select main SWG game folder (step 1)";

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
                System.exit(0);
                return;
            }
            msg = "Select valid folder (step 1)";
        }

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        swgDir = fc.getSelectedFile();
        final SWGUniverse universe = new SWGUniverse(swgDir);

        List<SWGUniverse> ul = (List<SWGUniverse>) SWGFrame.getPrefsKeeper().get("swgUniverseList", new ArrayList<SWGUniverse>());
        ul.add(universe);
        SWGFrame.getPrefsKeeper().add("swgUniverseList", (Serializable) ul);

        final JButton okButton = new JButton("Done");
        okButton.setEnabled(false);
        okButton.setMnemonic('D');
        okButton.setToolTipText("Finished the initialization");
        okButton.addActionListener(new AbstractAction() {
            
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
				SWGCraftCache.addSubscriber(new UpdateSubscriber()
                {
                    public void handleUpdate(UpdateNotification u)
                    {
						try {
							scanAll(universe, true);
						} catch (Exception e) {
							SWGAide.printError("SWGInitialize:iniateUniverseReal", e);
						}
					}
				}, UpdateType.SERVERS);
                SWGCraftCache.updateCache();
                
                frame.putToStatbar("Downloading remote files...", null);
                return null;
            }

            
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
        HTMLEditorKit kit = new HTMLEditorKit();
        ed.setEditorKit(kit);
        StyleSheet styleSheet = kit.getStyleSheet();
        Style style = styleSheet.getStyle("body");
        StyleConstants.setFontSize(style, SWGGuiUtils.fontPlain().getSize());
        style = styleSheet.getStyle("h1");
        StyleConstants.setFontSize(style, Math.round(StyleConstants.getFontSize(style)*SWGGuiUtils.fontMultiplier()) );
        style = styleSheet.getStyle("h2");
        StyleConstants.setFontSize(style, Math.round(StyleConstants.getFontSize(style)*SWGGuiUtils.fontMultiplier()) );
        style = styleSheet.getStyle("h3");
        StyleConstants.setFontSize(style, Math.round(StyleConstants.getFontSize(style)*SWGGuiUtils.fontMultiplier()) );
        style = styleSheet.getStyle("h4");
        StyleConstants.setFontSize(style, Math.round(StyleConstants.getFontSize(style)*SWGGuiUtils.fontMultiplier()) );
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
            	Thread t = new Thread(new Runnable() {
                    public void run() {
                        if (str != null) {
                            frame.putToLogbar_1(str);
                            frame.progressBar.setValue(step);
                        }
                    }
                });
            	t.start();
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
    public void scanAll(final SWGUniverse universe, boolean b) throws Exception {
        frame.putToStatbar("Scanning SWG directories",null);
        frame.putToLogbar_2(frame.progressBar);

        publishProgess("Scanning for aliases", 1, b);
        scanForAliases(universe);

        publishProgess("Scanning for station(s)", 20, b);
        scanForStations(universe);
        publishProgess(null, 25, b);

        publishProgess("Scanning for galaxies", 30, b);
        scanForGalaxies(universe, 30, 35);

        publishProgess("Scanning for characters", 40, b);
        scanForCharacters(universe, 40, 65);
        
        final ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(new Runnable() {
        	@Override
        	public void run() {
        		frame.beginPostLaunchTasks();
        		exec.shutdown();
        	}
        });
        

        publishProgess("", 100, b);
        frame.putToStatbar("Scanning finished",null);
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
}
