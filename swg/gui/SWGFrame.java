package swg.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import swg.SWGAide;
import swg.SWGConstants;
import swg.crafting.resources.SWGResource;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.common.FontOptionsPanel;
import swg.gui.common.SWGDoTask;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.resources.SWGResourceTab;
import swg.gui.schematics.SWGSchematicTab;
import swg.gui.trade.SWGTradeTab;
import swg.model.SWGCGalaxy;
import swg.model.SWGCharacter;
import swg.model.SWGProfessionManager;
import swg.model.SWGUniverse;
import swg.swgcraft.SWGCraftCache;
import swg.swgcraft.SWGCraftOptionsPanel;
import swg.swgcraft.SWGResourceManager;
import swg.tools.SimplePrefsKeeper;
import swg.tools.ZHtml;
import swg.tools.ZWriter;

/**
 * This class is the main frame that embraces all features that provides all
 * kinds of functionality. That includes objects that are in common, such as the
 * menu bar and the log bars. Furthermore a repository is provided so that
 * settings, options, and whatever can be safely stored between sessions at one
 * DAT file rather than one file each one feature.
 * <p>
 * In the current implementation an instance of this frame is created by the
 * SWGAide class, from that point everything is GUI event driven, except for
 * possible daemons built into the features.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGFrame extends JFrame implements ComponentListener,
    WindowListener, WindowStateListener {

    /**
     * The file chooser that may be used by components of this application
     */
    private static JFileChooser fileChooser = null;

    /**
     * The repository of preferences
     */
    private static SimplePrefsKeeper prefsKeeper = null;

    /**
     * The interface to resource collections and communications with SWGCraft
     */
    private static SWGResourceManager resourceManager;

    /**
     * The edit menu
     */
    private JMenu editMenu = null;

    /**
     * A list of callbacks to be executed before exit
     */
    private static List<SWGDoTask> exitList;

    /**
     * The file menu
     */
    JMenu fileMenu = null;

    /**
     * A small GUI utility box; this is just a reference to keep its
     * instantiated alive.
     */
    public final SWGGuiUtils guiUtils;

    /**
     * The help menu
     */
    private JMenu helpMenu = null;

    /**
     * The help page viewer for this application. It is statically accessed but
     * referenced for its default page.
     */
    @SuppressWarnings("unused")
    private SWGHelp helpViewer;

    /**
     * A flag which is set to true when the GUI is finished.
     */
    private boolean isGuiFinished;

    /**
     * The log bar number 1
     */
    private JLabel logbar_1 = null;

    /**
     * The log bar number 2
     */
    private JPanel logbar_2 = null;

    /**
     * A label that will have an icon if an error or exception has occured
     */
    private JLabel logError;

    /**
     * The main tab of the tabbed pane
     */
    private SWGMainTab mainTab;

    /**
     * The dialog for general options
     */
    private SWGCraftOptionsPanel optionsCraft = null;
    
    /**
     * The dialog for general options
     */
    private FontOptionsPanel optionsFont = null;

    /**
     * The menu item for general options
     */
    private JMenuItem optionsCraftMenuItem;
    
    /**
     * The menu item for Font options
     */
    private JMenuItem optionsFontMenuItem;

    /**
     * The options menu
     */
    JMenu optionsMenu = null;

    /**
     * An object for launching tasks that should be executed when everything
     * else is initialized and properly set up
     */
    private SWGPostLaunch postLaunch;

    /**
     * The profession manager for SWGAide.
     */
    private SWGProfessionManager professionManager;

    /**
     * The progress bar for the log bar at the main frame
     */
    final public JProgressBar progressBar;

    /**
     * The tab for the resource views
     */
    public SWGResourceTab resourceTab;

    /**
     * The "Save As..." menu item at the "File" menu
     */
    private JMenuItem saveAsMenuItem;

    /**
     * The "Save ..." menu item at the "File" menu
     */
    private JMenuItem saveMenuItem;

    /**
     * The schematics manager for SWGAide.
     */
    private SWGSchematicsManager schematicsManager;

    /**
     * The tab for the resource views
     */
    public SWGSchematicTab schematicTab;

    /**
     * The start up progress, used by the start progress splash screen
     */
    protected JProgressBar splashProgressBar = null;

    /**
     * The startup splash screen, enabling updates and disposal of it
     */
    private JWindow startScreen = null;

    /**
     * The status log bar
     */
    private JLabel statLog = null;

    /**
     * The main tabbed main pane, used by all application logic
     */
    private JTabbedPane tabPane = null;
    
    /**
     * Global variable for if user has verified credentials
     */
    public static Boolean verified = false;

    /**
     * Creates a SWGFrame object
     * 
     * @throws HeadlessException
     *             Thrown when code that is dependent on a keyboard, display, or
     *             mouse is called in an environment that does not support a
     *             keyboard, display, or mouse.
     */
    public SWGFrame() throws HeadlessException {
        guiUtils = new SWGGuiUtils(this);
        progressBar = initProgressBar();
        // from now on, see delayedConstructor(this) invoked from SWGAide
    }

    /**
     * Adds an object containing a callback to the list of callbacks that will
     * be called at exit of this application
     * 
     * @param callbackObject a callback object
     */
    public void addExitCallback(SWGDoTask callbackObject) {
        exitList.add(callbackObject);
    }

    /**
     * Saves the preference file to a uniquely named backup file.
     * 
     * @return the backup file of the preferences, <code>null</code> if not
     *         successful
     */
    protected File backupPrefs() {
        Integer backupNumber =
            (Integer) getPrefsKeeper().get("prefsFileBackupNumber",
            Integer.valueOf(0));

        int bn = backupNumber.intValue() + 1; // old number plus 1
        bn = bn > 999
            ? 1
            : bn; // start over at number 001
        String bName = "";
        if (bn > 99)
            bName = "" + bn;
        else if (bn > 9)
            bName = "0" + bn;
        else
            bName = "00" + bn;

        File backupFile =
            new File(SWGConstants.backupDir, "SWGAide_DAT." + bName);

        // save the incremented backup value and today's date if store was okay
        if (getPrefsKeeper().store(backupFile)) {
            getPrefsKeeper().add("prefsFileBackupNumber", new Integer(bn));
            getPrefsKeeper().add("prefsKeeperBackupDate",
                new Long(System.currentTimeMillis()));
            return backupFile;
        }
        return null;
    }

    /**
     * Helper method that begins some post-launch tasks. This method is invoked
     * from {@link SWGInitialize#scanAll(SWGUniverse, boolean)} once all
     * scanning is finished. This method also initiates {@link #postLaunch}.
     */
    void beginPostLaunchTasks() {
        resourceManager.startAutoUpdate();
        SWGCraftCache.updateCache();
        postLaunch = new SWGPostLaunch(this);
        postLaunch.postLaunch();        
        startBackupTimer();
    }

    /**
     * Check if the specified directory exists, otherwise create the directory.
     * 
     * @param directory
     *            the directory to check for
     */
    private void checkForDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                SWGAide.printError("SWGFrame:checkForFolder: "
                    + "failed creating \"" + directory + "\"", null);
            }
        }
    }

    public void componentHidden(ComponentEvent e) {
    // 
    }

    public void componentMoved(ComponentEvent e) {
        Point pp = getLocation();
        if (pp.x < 0 || pp.y < 0) return;

        getPrefsKeeper().add("frameLocation", pp);
    }

    public void componentResized(ComponentEvent e) {
        if (getExtendedState() != Frame.MAXIMIZED_BOTH) {
            getPrefsKeeper().add("frameDimension", getSize());
        }
    }

    public void componentShown(ComponentEvent e) {
    //
    }

    /**
     * A helper method for the constructor which is invoked from SWGAide just
     * after the frame is instantiated as a stub. First SWGAide creates this
     * instance and keeps a reference to this, then invokes this helper method.
     * 
     * @param swgFrame this frame
     * @throws IllegalArgumentException if {@code swgFrame != this}
     */
    public void delayedConstructor(SWGFrame swgFrame) {
        if (swgFrame != this)
            throw new IllegalArgumentException("Invalid frame");

        showStartScreen(true);
        setIconImage();

        exitList = new ArrayList<SWGDoTask>();

        // do this before the preference keeper to open an empty resource cache
        resourceManager = new SWGResourceManager(this);

        boolean firstTime = initEnvironment();

        // once the preference keeper is OK, initialize internal states
        resourceManager.initiate();
        guiUtils.initiate();
        helpViewer = new SWGHelp(this);

        initFrame();
        double step = 100.0 / 6; // progress steps
        splashProgressBar.setValue((int) step);

        // in this order: profession-levels MUST exist before schematics
        professionManager = new SWGProfessionManager();
        schematicsManager = new SWGSchematicsManager();

        setJMenuBar(initMenuBar());

        // create the main pane and add tabs and logbars to it
        JPanel pane = new JPanel(new BorderLayout());

        initLogBars(pane, firstTime);
        splashProgressBar.setValue((int) (2 * step));

        initTabPane(pane, firstTime, step);

        getContentPane().add(pane);
        splashProgressBar.setValue(100);

        setVisible(true);
        showStartScreen(false);
        isGuiFinished = true;

        addComponentListener(this);
        addWindowStateListener(this);
        addWindowListener(this);

        if (!firstTime) updatePostLaunch();
        
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> System.gc(), 5, TimeUnit.MINUTES);
    }

    /**
     * Helper method that displays a confirmation dialog with YES/NO options.
     * This method returns {@code true} if "yes" was selected, {@code false} if
     * "no" or if the user closed the dialog without selection. The integer
     * determines the message type of the dialog, one of
     * {@link JOptionPane#QUESTION_MESSAGE}, {@link JOptionPane#WARNING_MESSAGE}
     * or {@link JOptionPane#ERROR_MESSAGE}
     * 
     * @param msg the main message to display
     * @param title a title
     * @param mt the message type, e.g. {@link JOptionPane#QUESTION_MESSAGE}
     * @return {@code true} if YES was selected
     */
    private boolean displayConfirmDialog(String msg, String title, int mt) {
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                this, msg, title, JOptionPane.YES_NO_OPTION, mt);
    }

    /**
     * Does the exit routines, including calling each callback wrapped
     * 
     * @see SWGDoTask
     */
    public static void doExit() {

        for (SWGDoTask dx : exitList) {
            try {
                dx.callback.execute();
            } catch (Throwable e) {
                SWGAide.printError("SWGFrame:doExit", e);
            }
        }

        // following lines must be run lastly to allow updating prefs b4 exit
        if (getPrefsKeeper() != null
            && getPrefsKeeper().get("swgUniverse") != null) {
            
            File dat = new File("SWGAide.DAT");
            while (!getPrefsKeeper().store(dat)) {
                // XXX: Investigate how to remove this workaround
                // There is a risk that some background thread changes something
                // in SWGAide.DAT while it being stored, try again in 100 ms
                SWGAide.printDebug(
                    "frme", 1, "SWGFrame:doExit: error saving DAT, sleeping");
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e1) {/* ignore */}
            }
        }
        SWGAide.printStop();
        System.exit(0);
    }

    /**
     * Adds a menu item to the "Edit" menu. If the name of <code>menuItem</code>
     * equals "-=-" a menu separator is added
     * 
     * @param menuItem
     *            the menu item to add
     */
    public void editMenuAdd(JMenuItem menuItem) {
        if (menuItem.getText().equals("-=-")) {
            editMenu.addSeparator();
        } else {
            editMenu.add(menuItem);
        }
        editMenu.setEnabled(true);
    }

    /**
     * Adds all items in <code>menuComponents</code> to the "Edit" menu
     * 
     * @param menuComponents
     *            a list of the the menu items to add
     */
    public void editMenuAdd(List<JComponent> menuComponents) {
        for (JComponent cmp : menuComponents) {
            editMenu.add(cmp);
        }
        editMenu.setEnabled(true);
    }

    /**
     * Removes <code>menuItem</code> from the "Edit" menu. If there is no more
     * items left the Edit menu is disabled.
     * 
     * @param menuItem
     *            the menu item to remove from the Edit menu
     */
    public void editMenuRemove(JMenuItem menuItem) {
        if (menuItem == null || menuItem.getText().equals("-=-"))
            return;
        editMenu.remove(menuItem);
        if (editMenu.getMenuComponents().length == 0)
            editMenu.setEnabled(false);
    }

    /**
     * Removes all items in <code>menuComponents</code> from the "Edit" menu. If
     * there is no more items left the Edit menu is disabled.
     * 
     * @param menuComponents
     *            the menu item to remove from the Edit menu
     */
    public void editMenuRemove(List<JComponent> menuComponents) {
        for (JComponent cmp : menuComponents) {
            editMenu.remove(cmp);
        }
        if (editMenu.getMenuComponents().length == 0)
            editMenu.setEnabled(false);
    }

    /**
     * Removes all menu items from the "Edit" menu and disables the menu
     */
    public void editMenuRemoveAll() {
        editMenu.removeAll();
        editMenu.setEnabled(false);
    }

    /**
     * Raises an error dialog plus lights up the red bulb on the lower, right
     * corner
     */
    public void errorAlert() {
        if (!isGuiFinished) return;
        
        logError.setIcon(new ImageIcon(SWGFrame.class
                .getResource("images/red_signal.gif")));
        logError.setToolTipText("See logs\\SWGAide-ERROR.TXT");
        if (Desktop.isDesktopSupported()) {
            logError.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showErrorMailOption();
                }
            });
        }
    }

    /**
     * Returns the list of exit routines
     * 
     * @see SWGDoTask
     * @return the list of exit routines
     */
    public List<SWGDoTask> getExitList() {
        return exitList;
    }

    /**
     * Returns the main tab of the tabbed pane
     * 
     * @return the main tab of the tabbed pane
     */
    public SWGMainTab getMainTab() {
        return mainTab;
    }

    /**
     * Returns the dialog for general options of this application
     * 
     * @return the dialog for general options of this application
     */
    public SWGCraftOptionsPanel getOptionsCraft() {
        if (optionsCraft == null) {
            optionsCraft = new SWGCraftOptionsPanel(this);
        }
        return optionsCraft;
    }
    
    /**
     * Returns the dialog for general options of this application
     * 
     * @return the dialog for general options of this application
     */
    public FontOptionsPanel getOptionsFont() {
        if (optionsFont == null) {
            optionsFont = new FontOptionsPanel(this);
        }
        return optionsFont;
    }

    /**
     * Returns the profession manager for SWGAide.
     * 
     * @return the profession manager
     */
    SWGProfessionManager getProfessionManager() {
        return professionManager;
    }

    /**
     * Returns the schematics manager for SWGAide.
     * 
     * @return the schematics manager
     */
    public SWGSchematicsManager getSchematicsManager() {
        return schematicsManager;
    }

    /**
     * Returns the main tabbed pane of this application
     * 
     * @return the main tabbed pane
     */
    public JTabbedPane getTabPane() {
        return tabPane;
    }

    /**
     * Checks whether a backup of SWGAide.DAT is necessary, and if so
     * {@link #backupPrefs()} is called as a result of the check
     */
    private void initCheckBackupPrefsKeeperNecessary() {
        long timeToday = System.currentTimeMillis();
        Long formerDate = (Long) getPrefsKeeper().get("prefsKeeperBackupDate");

        // TODO: remove/restore when turns 0.8
        getPrefsKeeper().add("prefsKeeperBackupRepeatDays", Integer.valueOf(1));

        Integer backupRepeatDays = (Integer) getPrefsKeeper().get(
            "prefsKeeperBackupRepeatDays");
        // , /* default 2 d */Integer.valueOf(2));
        if (formerDate != null) {
            long nextBackupDate =
                formerDate.longValue()
                + (backupRepeatDays.intValue() * 24 * 60 * 60 * 1000);
            if (timeToday >= nextBackupDate) {
                backupPrefs();
            }
        } else {
            getPrefsKeeper().add("prefsKeeperBackupDate", Long.valueOf(0));
        }
    }
    
    /**
     * Initiates and populates the edit menu
     * 
     * @param bar
     *            the menu bar to attach this menu at
     */
    private void initEditMenu(JMenuBar bar) {
        editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        editMenu.setEnabled(false);
        bar.add(editMenu);
    }

    /**
     * Initiates and sets up the environment with necessary directories and
     * files. Thereafter it sets up the preference keeper from "SWGAide.DAT". It
     * is assumed that no DAT file indicates that this is the first time this
     * application is run.
     * 
     * @return <code>true</code> if no DAT file is found, <code>false</code>
     *         otherwise
     */
    private boolean initEnvironment() {
        checkForDirectory(SWGConstants.backupDir);
        checkForDirectory(SWGConstants.repositoryPath);
        checkForDirectory("logs");
        checkForDirectory("mails");
        checkForDirectory("misc");
        checkForDirectory("temp");
        checkForDirectory("trash");

        if (SWGConstants.DEV_DEBUG)
            SWGConstants.setCacheDirectory(new File("temp"));

        File ini = new File("SWGAide.DAT");
        File bak = new File(SWGConstants.backupDir, "SWGAide.BAK");

        if (ini.exists() || bak.exists()) {
            if (!ini.exists() && bak.exists()) {
                String msg = "No SWGAide.DAT exists but found SWGAide.BAK\n" +
                        "Exit and copy files manually or use backup file?" +
                        "\n\nExit SWGAide?";
                boolean b = displayConfirmDialog(msg, "Confirm exit",
                        JOptionPane.QUESTION_MESSAGE);

                SWGAide.printDebug(
                        "frme", 2, "SWGFrame:initEnvironment: BAK::" + (b
                                ? "exiting"
                                : "continuing"));

                if (b) doExit();
                // else
                ini = bak;
            }

            initPrefsKeeper(ini);

            if (getPrefsKeeper() != null) {
                initSWGClient();
                updatePreLaunch();
                initCheckBackupPrefsKeeperNecessary();
                verified = (Boolean) prefsKeeper.get("optionVerified");
                return false;
            }
        } // else: ini does not exist
        prefsKeeper = new SimplePrefsKeeper();
        getPrefsKeeper().add("prefsKeeperBackupDate", Long.valueOf(0));
        
        return true;
    }

    /**
     * Initiates and populates the file menu
     * 
     * @param bar
     *            the menu bar to attach this menu at
     */
    private void initFileMenu(JMenuBar bar) {
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
            InputEvent.CTRL_MASK));
        saveMenuItem.setEnabled(false);
        fileMenu.add(saveMenuItem);

        saveAsMenuItem = new JMenuItem("Save As...");
        saveAsMenuItem.setMnemonic(KeyEvent.VK_A);
        saveAsMenuItem.setEnabled(false);
        fileMenu.add(saveAsMenuItem);

        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem();
        exit.setText("Exit");
        exit.setMnemonic(KeyEvent.VK_X);
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,
            ActionEvent.ALT_MASK));
        exit.addActionListener(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                doExit();
            }
        });
        fileMenu.add(exit);

        bar.add(fileMenu);
    }

    /**
     * Initiates the frame with values read from the DAT file or with default
     * values, and fills the frame with some initial components
     */
    private void initFrame() {
        setTitle("SWGAide-NGE");

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        // default size 1280 X 800 or screen size limited
        int w = screen.width < 1280
            ? screen.width
            : 1280;
        int h = screen.height < 800
            ? screen.height
            : 800;
        Dimension dim = new Dimension(w, h);
        Dimension pd = (Dimension) getPrefsKeeper().add("frameDimension", dim);
        if (pd != null) dim = pd;
        Dimension md = new Dimension(1280,800);
        this.setMinimumSize(md);
        this.setSize(dim);

        // compute location mid screen
        w = (screen.width - dim.width) >> 1;
        h = (screen.height - dim.height) >> 1;
        Point location = new Point(w, h);
        location = (Point) getPrefsKeeper().get("frameLocation", location);
        location = SWGGuiUtils.ensureOnScreen(location, dim);
        setLocation(location);

        Boolean maximized =
            (Boolean) getPrefsKeeper().get("frameMaximized", Boolean.FALSE);
        if (maximized.booleanValue()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    /**
     * Initiates and populates the help menu
     * 
     * @param bar
     *            the menu bar to attach this menu at
     */
    private void initHelpMenu(JMenuBar bar) {
        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        JMenuItem hlp = new JMenuItem("Help");
        hlp.setToolTipText("View the help screen for the active screen, "
            + "or the General Help");
        hlp.setMnemonic('P');
        hlp.setAccelerator(KeyStroke.getKeyStroke("F1"));
        hlp.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SWGHelp.display();
            }

        });
        helpMenu.add(hlp);

        JMenuItem gnl = new JMenuItem("General Help");
        gnl.setToolTipText("View the General Help page");
        gnl.setMnemonic('G');
        gnl.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SWGHelp.display(0);
            }

        });
        helpMenu.add(gnl);

        JMenuItem stm = new JMenuItem("Statement");
        stm.setMnemonic('S');
        stm.addActionListener(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                showStatement();
            }

        });
        helpMenu.add(stm);

        JMenuItem lic = new JMenuItem("License text");
        lic.setMnemonic('L');
        lic.addActionListener(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                showLicense();
            }

        });
        helpMenu.add(lic);

        JMenuItem abt = new JMenuItem("About SWGAide-NGE");
        abt.setMnemonic('A');
        abt.addActionListener(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                showAbout();
            }
        });
        helpMenu.add(abt);

        helpMenu.addSeparator();

        JMenuItem goWeb = new JMenuItem("SWGAide-NGE Web Site");
        goWeb.setToolTipText("Open the SWGAide-NGE web page in a web browser");
        goWeb.setMnemonic('W');
        goWeb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ZHtml.browser(SWGConstants.swgAideURL);
            }
        });
        helpMenu.add(goWeb);

        bar.add(helpMenu);
    }

    /**
     * Initiates the log bar with its 3 log text fields
     * 
     * @param pane the pane to add the log bar to
     * @param firstTime <code>true</code> if the application is assumed to run
     *        for the first time, <code>false</code> otherwise
     */
    private void initLogBars(JPanel pane, boolean firstTime) {
        JPanel logs = new JPanel();
        GridLayout lyt = new GridLayout(1, 3);
        logs.setLayout(lyt);
        logs.setBorder(BorderFactory.createLoweredBevelBorder());

        statLog = new JLabel("   ");
        final Timer timer = new Timer(3000, new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                statLog.setText("   ");
            }
        });
        timer.setRepeats(false);
        statLog.setName("resetTimer");
        statLog.putClientProperty(statLog.getName(), timer);
        logs.add(statLog);

        logbar_1 = new JLabel();
        logs.add(logbar_1);
        putToLogbar_1(" ");

        logbar_2 = new JPanel();
        logs.add(logbar_2);
        putToLogbar_2(" ");

        JPanel bottomLogbar = new JPanel();
        bottomLogbar.setLayout(new BoxLayout(bottomLogbar, BoxLayout.X_AXIS));

        logError = new JLabel();
        logError.setIcon(new ImageIcon(SWGFrame.class
                .getResource("images/blank.gif")));
        logError.setToolTipText("No error to report");

        bottomLogbar.add(logs);
        bottomLogbar.add(logError);

        pane.add(bottomLogbar, BorderLayout.PAGE_END);

        if (firstTime)
            putToStatbar("Welcome");
    }

    /**
     * Returns a populated menu bar for this application
     * 
     * @return a populated menu bar
     */
    private JMenuBar initMenuBar() {
        JMenuBar bar = new JMenuBar();

        initFileMenu(bar);
        initEditMenu(bar);
        initOptionsMenu(bar);
        initHelpMenu(bar);

        return bar;
    }

    /**
     * Initiates and populates the options menu
     * 
     * @param bar
     *            the menu bar to attach this menu at
     */
    private void initOptionsMenu(JMenuBar bar) {
        optionsMenu = new JMenu("Options");
        optionsMenu.setToolTipText("Options for SWGAide");
        optionsMenu.setMnemonic(KeyEvent.VK_O);

        optionsMenu.add(optionsSWGCraftMenuItem());
        optionsMenu.add(optionsFontSize());

        optionsMenu.setEnabled(true);
        bar.add(optionsMenu);
    }

    /**
     * Initializes the preference keeper and handles possible errors:
     * <ul>
     * <li>Try to load SWGAide.DAT<br>
     * If an exception
     * <UL>
     * <LI >Move SWGAide.DAT to trash with systemtimemillis() as file suffix</li>
     * <li>Try to load &lt;backups&gt;\SWGAide.BAK</li>
     * <ul>
     * <li>If success, rename to SWGAide.DAT</li>
     * <li>Else, delete</li>
     * </ul>
     * <li>Inform user</li>
     * </ul>
     * </ul>
     * 
     * @param datFile the DAT file to load
     */
    private void initPrefsKeeper(File datFile) {
        // argument can be a BAK file

        String result = null;
        int mType = -1;
        try {
            prefsKeeper = SimplePrefsKeeper.load(datFile);
            SWGInitialize.update();
        } catch (Throwable e) {
            SWGAide.printDebug(
                    "frme", 1, "SWGFrame:initPrefsKeeper1:", e.getMessage());

            if (!datFile.getName().endsWith(".BAK")) {
                File bak = new File(SWGConstants.backupDir, "SWGAide.BAK");
                if (bak.exists()) {
                    try {
                        resourceManager.clear(); // empty the cache
                        prefsKeeper = SimplePrefsKeeper.load(bak);
                        result = "Error loading SWGAide.DAT\n" +
                                "Successfully loaded SWGAide.BAK\n" +
                                "See error log file";
                        mType = JOptionPane.WARNING_MESSAGE;
                    } catch (Throwable e1) {
                        SWGAide.printDebug("frme", 1, String.format(
                                "SWGFrame:initPrefsKeeper2: %s\n" +
                                        "You can try to restore a recent " +
                                        "backup file", e1.getMessage()));
                        result = "Error loading SWGAide.DAT and SWGAide.BAK\n" +
                                "See log file";
                        mType = JOptionPane.ERROR_MESSAGE;
                        prefsKeeper = null;
                    }
                } else {
                    result = "Error loading SWGAide.DAT and no SWGAide.BAK\n" +
                            "You can try to restore a recent backup file.";
                    mType = JOptionPane.ERROR_MESSAGE;
                }
            } else {
                result = "Error loading SWGAide.BAK";
                mType = JOptionPane.WARNING_MESSAGE;
                prefsKeeper = null;
            }
        }
        if (result != null) {
            startScreen.setAlwaysOnTop(false);
            JOptionPane.showMessageDialog(this, result, "Init problem", mType);
        }
    }

    /**
     * Creates and returns a GUI progress-bar for later use.
     * 
     * @return a GUI progress bar
     */
    private JProgressBar initProgressBar() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setBorder(BorderFactory.createLoweredBevelBorder());
        return pb;
    }

    /**
     * Helper method which verifies that the SWG client path is valid for the
     * current host. This implementation must be run <i>after </i> that the
     * SWGAide.DAT file exists and is loaded. Then it determines if the SWG path
     * for the known universe matches the current host. If that is so this
     * method exits.
     * <p>
     * If the SWG path does not match the current host this method queries
     * {@link SWGAide#swgPath()} for the value of the SWGAide.INI file.
     * If that file is {@code null} or wrong and if one or several previous
     * files are known by SWGAide this method determines if at most one of them
     * is valid; if several are valid (the user has several installations) this
     * method displays an option dialog with the valid files. If no previous
     * file is known or if the user cancels this dialog a common open-file
     * dialog is used so that the user selects a path to SWG. If the user
     * cancels this method will use an empty file for the next step.
     * <p>
     * Finally this method invokes {@link SWGUniverse#swgPath(File)} for SWG and
     * possibly TC with the selected file, or with an empty file.
     */
    private void initSWGClient() {
        try {
            SWGUniverse u = (SWGUniverse) getPrefsKeeper().get("swgUniverse");
            File f = u.swgPath();
            if (f == null || !SWGUniverse.isValidSWGPath(f))
                f = null;

            File fIni = null;
            if (f == null && (fIni = SWGAide.swgPath()) != null) {
                try {
                    f = initSWGUniverse(fIni, u);
                } catch (Exception e) {
                    SWGAide.printDebug("frme", 1,
                            "SWGFrame:initSWGClient:", e.getMessage());
                }
            }

            @SuppressWarnings("unchecked")
            // this is the only place we add/get the list, hence safe cast
            ArrayList<File> fp = (ArrayList<File>) getPrefsKeeper().get(
                    "swgClientPaths", new ArrayList<File>());

            if (f == null) f = initSWGClientPaths(fp);
            if (f == null) f = initSWGClientDialog();

            if (f == null)
                initSWGClientExitDialog(JOptionPane.WARNING_MESSAGE);
            else {
                initSWGUniverse(f, u);
                initSWGClientPaths(f, fp); // save selected file
                if (fIni != null) // only if user has edited INI
                    SWGAide.swgPath(f);
            }
        } catch (Throwable e) {
            SWGAide.printError("SWGFrame:initSWGClient", e);
            initSWGClientExitDialog(JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Helper method which displays a common open-file dialog and returns a
     * valid SWG client path, or {@code null}. This method reopens the file
     * chooser until a valid file is selected. If the user cancels this method
     * returns {@code null}.
     * 
     * @return a valid file, or {@code null}
     */
    private File initSWGClientDialog() {
        JFileChooser fc = getFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String msg = "Find and select the \"StarWarsGalaxies\" folder";

        File f = new File("C:\\");
        fc.setCurrentDirectory(f);
        do {
            fc.setDialogTitle(msg);
            int retVal = fc.showOpenDialog(this);
            if (retVal != JFileChooser.APPROVE_OPTION) return null;

            msg = "Try again, find a valid \"StarWarsGalaxies\" folder";
            f = fc.getSelectedFile();
            if (f.getName().equals("testcenter")) f = f.getParentFile();
        } while (!SWGUniverse.isValidSWGPath(f));
        return f;
    }

    /**
     * Helper method which displays a dialog and possibly kills SWGAide. This
     * method is invoked if no valid path to the SWG client exists or if the
     * user cancels the open-file dialog. The dialog presents the user with the
     * option to exit SWGAide, yes or no, and this method logs the response. If
     * exit, this method saves nothing but invokes {@link System#exit(int)}.
     * <p>
     * The exact message is determined by the argument, one of
     * {@link JOptionPane#WARNING_MESSAGE}or{@link JOptionPane#ERROR_MESSAGE}.
     * 
     * @param mt the dialog message type
     */
    private void initSWGClientExitDialog(int mt) {
        String s = String.format("%s folder for \"StarWarsGalaxies\"\n" +
                "If you continue some features are disabled%s\n\n" +
                "Exit SWGAide?",
                (mt == JOptionPane.WARNING_MESSAGE
                        ? "Found no"
                        : "Error locating"),
                (mt == JOptionPane.WARNING_MESSAGE
                        ? ""
                        : "\nExit SWGAide, read the log files, and\n" +
                                "report errors at swgaide.com, please"));

        if (displayConfirmDialog(s, "Confirm exit", mt)) {

            SWGAide.printDebug("frme", 1,
                    "SWGFrame:initSWGClient: exits SWG");
            SWGAide.printStop();
            System.exit(0); // hard kill, nothing to save
        }
        SWGAide.printDebug("frme", 1,
                "SWGFrame:initSWGClient: continues without SWG");
    }

    /**
     * Helper method which appends the specified file to the list. The list
     * contains recent SWG client paths and is stored in SWGAide's DAT file.
     * This method adds the file argument as the leading element, if it already
     * exists in the list it is promoted. If the file argument is {@code null}
     * this method does nothing.
     * <p>
     * This method ensures that only unique elements exist in the list and that
     * max 10 elements are retained. The latter is to shave off older files for
     * obsolete computers. Older files are pushed backwards so if the player has
     * no more than 10 installations with different client paths this will work.
     * 
     * @param f a file
     * @param fl a list of previous files
     * @throws NullPointerException if the list is {@code null}
     */
    private void initSWGClientPaths(File f, List<File> fl) {
        if (f == null
                || (!fl.isEmpty() && fl.get(0).equals(f))) return;

        while (fl.contains(f))
            fl.remove(f); // to promote it and ensure uniqueness

        fl.add(0, f); // leading position

        if (fl.size() > 10) fl.subList(10, fl.size()).clear(); // trim size
    }

    /**
     * Helper method which iterates over the list of files and returns one of
     * them, or {@code null}. This method determines if at most one of the files
     * is valid returns it. Otherwise, if several files are valid (the user has
     * several installations) this method displays an option dialog with the
     * valid files. This method returns the selected file, or it returns {@code
     * null} if no valid file is found or if the user cancels the dialog.
     * 
     * @param fp a list of files
     * @return a valid file, or {@code null}
     * @throws NullPointerException if the argument is {@code null}
     */
    private File initSWGClientPaths(List<File> fp) {
        if (fp.isEmpty()) return null;

        ArrayList<File> fl = new ArrayList<File>();
        for (File f : fp)
            if (f.exists() && SWGUniverse.isValidSWGPath(f)
                    && !fl.contains(f)) fl.add(f);

        if (fl.isEmpty()) return null;
        if (fl.size() == 1) return fl.get(0);

        File[] fs = fl.toArray(new File[fl.size()]);
        int r = JOptionPane.showOptionDialog(this,
                "Select valid SWG location", "Select folder",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, fs, fs[0]);
        return r < 0
                ? null
                : fs[r];
    }

    /**
     * Helper method which sets the specified file as the SWG client path for
     * the specified universe. The file <b>must exist</b> at the current host
     * and the universe <b>must be</b> the base SWG client, not TC. This method
     * determines if TC exists and is valid and is so it updates it.
     * 
     * @param f a valid path to the SWG client
     * @param swgUniv the base SWG universe, not TC
     * @return the argument as-is
     * @throws Exception if there is an error
     */
    private File initSWGUniverse(File f, SWGUniverse swgUniv) throws Exception {
        swgUniv.swgPath(f);

        SWGUniverse tc = (SWGUniverse) getPrefsKeeper().get("swgTestCenter");
        File tf = new File(f, "testcenter");
        if (tc != null && SWGUniverse.isValidSWGPath(tf)) tc.swgPath(tf);
        return f;
    }

    /**
     * Initiates the tabbed pane. If <code>firstTime</code> is <code>true</code>
     * the logic will continue and will initiate the application with user
     * assistance.
     * 
     * @param pane the panel to add the tabbed pane to
     * @param firstTime <code>true</code> if this is the first time this
     *        application is run, <code>false</code> otherwise
     * @param step the value with which to continue updating the splash screen
     * @see SWGInitialize#iniateStart()
     */
    private void initTabPane(JPanel pane, boolean firstTime, double step) {
        tabPane = new JTabbedPane();
        pane.add(tabPane, BorderLayout.CENTER);

        SWGUniverse universe = (SWGUniverse)
                getPrefsKeeper().get("swgUniverse");

        SWGInitialize initialize = new SWGInitialize(this);

        if (firstTime || universe == null) {
            initialize.iniateStart();
            splashProgressBar.setValue((int) (3 * step));
        } else {
            populateTabPane(step);
            try {
                if (universe.exists()) initialize.scanAll(universe, false);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, String.format(
                        "Initialization error:%n%s%nSee log file",
                        e.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
                SWGAide.printError("SWGFrame:init", e);
            }
            putToLogbar_2(null);
            putToStatbar(null);
        }
    }

    /**
     * Adds a menu item to the "Edit" menu. If the name of <code>menuItem</code>
     * equals "-=-" a menu separator is added
     * 
     * @param menuItem
     *            the menu item to add
     */
    public void optionsMenuAdd(JMenuItem menuItem) {
        optionsMenu.add(menuItem);
        optionsMenu.setEnabled(true);
    }

    /**
     * Removes <code>menuItem</code> from the "Edit" menu. If there is no more
     * items left the Edit menu is disabled.
     * 
     * @param menuItem
     *            the menu item to remove from the Edit menu
     */
    public void optionsMenuRemove(JMenuItem menuItem) {
        if (menuItem == null)
            return;
        optionsMenu.remove(menuItem);
        if (optionsMenu.getMenuComponents().length == 0)
            optionsMenu.setEnabled(false);
    }

    /**
     * Called when the general options menu is selected
     */
    protected void optionsSWGCraftClicked() {
        getOptionsCraft();
        optionsCraft.setVisible(true);
    }
    
    /**
     * Called when the general options menu is selected
     */
    protected void optionsFontSizeClicked() {
        getOptionsFont();
        optionsFont.setVisible(true);
    }

    /**
     * Returns a menu item for the Options menu, SWGCraft options
     * 
     * @return a menu item for the Options menu, SWGCraft options
     */
    private JMenuItem optionsSWGCraftMenuItem() {
        optionsCraftMenuItem = new JMenuItem("SWGAide...");
        optionsCraftMenuItem
            .setToolTipText("Options for connecting to swgaide.com");
        optionsCraftMenuItem.setMnemonic('S');
        optionsCraftMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                optionsSWGCraftClicked();
            }
        });
        return optionsCraftMenuItem;
    }
    
    /**
     * Returns a menu item for the Options menu, FontSize options
     * 
     * @return a menu item for the Options menu, FontSize options
     */
    private JMenuItem optionsFontSize() {
        optionsFontMenuItem = new JMenuItem("Font Size");
        optionsFontMenuItem
            .setToolTipText("Adjust the Font Size for the entire application");
        optionsFontMenuItem.setMnemonic('F');
        optionsFontMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                optionsFontSizeClicked();
            }
        });
        return optionsFontMenuItem;
    }

    /**
     * Populates the tabbed view with a few tabs, either directly at startup or
     * after initiating the application
     * 
     * @param step
     *            the value with which to continue updating the splash screen
     */
    protected void populateTabPane(double step) {
        mainTab = new SWGMainTab(this);
        splashProgressBar.setValue((int) (4 * step));

        SWGTradeTab tradeTab = new SWGTradeTab(this);
        resourceTab = new SWGResourceTab(this);
        schematicTab = new SWGSchematicTab(this);

        // add them after all are created, not to trigger GUI action events
        // until all are at least in pre-state
        tabPane.add("Main", mainTab);
        tabPane.add("Trade", tradeTab);
        tabPane.add("Resources", resourceTab);
        tabPane.add("Schematics", schematicTab);
        
        tabPane.setMnemonicAt(0, KeyEvent.VK_M);
        tabPane.setMnemonicAt(1, KeyEvent.VK_T);
        tabPane.setMnemonicAt(2, KeyEvent.VK_R);
        tabPane.setMnemonicAt(3, KeyEvent.VK_S);

        splashProgressBar.setValue((int) (5 * step));
    }

    /**
     * Sets the specified text to log bar number 1, the middle label of the
     * frame's log bar. This method invokes
     * {@link SwingUtilities#invokeLater(Runnable)} so it is safe to call from
     * any worked thread.
     * 
     * @param text
     *            the text to display at the log bar
     */
    public void putToLogbar_1(final String text) {
        SwingUtilities.invokeLater(new Runnable() {

            
            @Override
            public void run() {
                if (text == null)
                    logbar_1.setText(" ");
                else
                    logbar_1.setText(text);
                logbar_1.repaint();
            }
        });
    }

    /**
     * Sets the specified object to log bar number 2, the rightmost label of the
     * frame's log bar. This method invokes
     * {@link SwingUtilities#invokeLater(Runnable)} so it is safe to call from
     * any worked thread.
     * <p>
     * <B>Note: </B>The only supported objects are {@code String} and {@code
     * Component} classes, any other object will use its {@code toString()}
     * method.
     * 
     * @param o
     *            the object to display at log bar number 2, {@code null}
     *            removes any previous object
     */
    public void putToLogbar_2(final Object o) {
        SwingUtilities.invokeLater(new Runnable() {

            
            @Override
            public void run() {
                Object oo = o;
                logbar_2.removeAll();
                if (oo == null)
                    oo = "                   ";

                if (oo instanceof Component) {
                    logbar_2.add((Component) oo);
                } else {
                    JLabel lab = new JLabel();
                    if (oo instanceof String) {
                        lab.setText((String) oo);
                    } else {
                        lab.setText(oo.toString());
                    }
                    logbar_2.add(lab);
                }
                logbar_2.revalidate();
                logbar_2.repaint();
            }
        });
    }

    // /**
    // * Returns a sub menu for options used by the Trade GUI panel
    // *
    // * @return the sub menu item for Trades options
    // */
    // private JMenuItem optionsTradeScreen() {
    // JMenu tradeOpt = new JMenu("Trade");
    // tradeOpt.setToolTipText("Options for the Trade panel");
    // tradeOpt.setMnemonic('T');
    //
    // tradeOpt.add(makeOptionsTradeCSVWriteColors());
    //
    // return tradeOpt;
    // }
    //
    // /**
    // * Returns a menu item for Trade writing CSV without colors
    // *
    // * @return a menu item for Trade writing CSV without colors
    // */
    // private JMenuItem makeOptionsTradeCSVWriteColors() {
    // final JCheckBoxMenuItem colors = new JCheckBoxMenuItem("Colored CSV");
    // colors.setToolTipText("Allow color codes in output file");
    // colors.setMnemonic('C');
    //
    // colors.setSelected((Boolean) prefsKeeper.get("tradeAllowColorsInCSV",
    // Boolean.FALSE));
    //
    // colors.addActionListener(new ActionListener() {
    //
    // public void actionPerformed(ActionEvent e) {
    // prefsKeeper.add("tradeAllowColorsInCSV", colors.isSelected());
    // }
    // });
    // return colors;
    // }

    /**
     * Sets the specified text to the status bar, the leftmost label of the
     * frame's log bar. This method invokes
     * {@link SwingUtilities#invokeLater(Runnable)} so it is safe to call from
     * any worked thread.
     * 
     * @param text
     *            the text to display at the status bar
     */
    public void putToStatbar(final String text) {
        if (statLog == null)
            return;
        SwingUtilities.invokeLater(new Runnable() {

            
            @Override
            public void run() {
                if (text == null) statLog.setText("   ");
                else statLog.setText(text);
                statLog.repaint();
                ((Timer) statLog.getClientProperty(statLog.getName())).restart();
            }
        });
    }

    /**
     * Adds a listener to the "Save" menu item at the "File" menu
     * 
     * @param listener
     *            the listener to add
     * @param toolTip
     *            the tooltip for this menu item, <code>null</code> for no
     *        tooltip
     */
    public void saveAddListener(ActionListener listener, String toolTip) {
        ActionListener[] lsts = saveMenuItem.getActionListeners();
        for (ActionListener al : lsts) {
            saveMenuItem.removeActionListener(al);
        }
        saveMenuItem.addActionListener(listener);
        saveMenuItem.setToolTipText(toolTip);
        saveMenuItem.setEnabled(true);
    }

    /**
     * Adds a listener to the "Save As..." menu item at the "File" menu
     * 
     * @param listener
     *            the listener to add
     * @param toolTip
     *            the tooltip for this menu item, <code>null</code> for no
     *        tooltip
     */
    public void saveAsAddListener(ActionListener listener, String toolTip) {
        ActionListener[] lsts = saveAsMenuItem.getActionListeners();
        for (ActionListener al : lsts) {
            saveAsMenuItem.removeActionListener(al);
        }
        saveAsMenuItem.addActionListener(listener);
        saveAsMenuItem.setToolTipText(toolTip);
        saveAsMenuItem.setEnabled(true);
    }

    /**
     * Removes <code>listener</code> from the "Save As..." menu item at the
     * "File" menu. If there is no more listeners left the item is disabled.
     * 
     * @param listener
     *            the listener to remove from the Save As menu item
     */
    public void saveAsRemoveListener(ActionListener listener) {
        saveAsMenuItem.removeActionListener(listener);
        if (saveAsMenuItem.getActionListeners().length == 0) {
            saveAsMenuItem.setEnabled(false);
            saveAsMenuItem.setToolTipText(null);
        }
    }

    /**
     * Removes <code>listener</code> from the "Save" menu item at the "File"
     * menu. If there is no more listeners left the item is disabled.
     * 
     * @param listener
     *            the listener to remove from the Save As menu item
     */
    public void saveRemoveListener(ActionListener listener) {
        saveMenuItem.removeActionListener(listener);
        if (saveMenuItem.getActionListeners().length == 0) {
            saveMenuItem.setEnabled(false);
            saveMenuItem.setToolTipText(null);
        }
    }

    /**
     * Sets an icon image for the frame
     */
    private void setIconImage() {
        URL url = SWGFrame.class.getResource("images/icon.gif");
        if (url == null) return;
        setIconImage(new ImageIcon(url).getImage());
    }

    /**
     * Shows an about dialogue pane
     */
    protected void showAbout() {
        JOptionPane.showMessageDialog(this, "SWGAide-NGE " + SWGConstants.version +
        		"\n\nForked and Maintained by Holmes @ SWG:Legends\n" +
        		"    (aka Mr-Miagi)\n\n" +
        		"Originally Created by Simon Gronlund\n" +
                "Zimoon @ Chimaera\n" +
                "Oarun @ Chilastra\n\n" +
                "Resource and schematic data is provided by swgaide.com and SWGCraft.org\n" +
                "Creature harvesting data is obtained from SWGPets.com\n" +
                "Credits to SciGuy and Onyx for food & drink buff data\n\n" +
                "Your support for SWGCraft and SWGPets is important",
                "About", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Shows a message dialogue giving users information on where
     * to post bugs
     */
	protected void showErrorMailOption() {
    	JOptionPane.showMessageDialog(this, "SWGAide-NGE " + SWGConstants.version +
        		"\n\nAn Error has occured!\n\n" +
        		"Bug reports can be posted on github:\n" +
                "https://github.com/twistedatrocity/SWGAide-NGE/issues\n" +
                "Please describe the issue and attach any logs\n\n" +
                "Thank You",
                "Error", JOptionPane.PLAIN_MESSAGE);

        return;
        
    }

    /**
     * Shows the help screen with the license text
     */
    protected void showLicense() {
        SWGHelp.display(SWGAide.class.getResource("docs/license_en.html"));
    }

    /**
     * Shows the startup screen and progress meter
     * 
     * @param show
     *            <code>true</code> if the splash screen should show,
     *            <code>false</code> to make it invisible
     */
    private void showStartScreen(boolean show) {
        if (show) {
            startScreen = new JWindow();
            Dimension size = new Dimension(210, 225);
            startScreen.setSize(size);
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            startScreen.setLocation((dim.width - size.width) >> 1,
                (dim.height - size.height) >> 1);

            JPanel pane = new JPanel();
            pane.setLayout(new BorderLayout());
            pane.setBorder(BorderFactory.createEtchedBorder());

            JLabel label = new JLabel();
            label.setIcon(new ImageIcon(SWGFrame.class
                .getResource("images/splash.png")));
            pane.add(label, "North");

            splashProgressBar = new JProgressBar(0, 100);
            pane.add(splashProgressBar, "South");

            startScreen.getContentPane().add(pane);
            startScreen.setAlwaysOnTop(false);
            startScreen.setVisible(true);
        } else {
            startScreen.setVisible(false);
            startScreen.dispose();
            startScreen = null;
        }
    }

    /**
     * Shows the help screen with a statement on misc issues I want to vent
     */
    protected void showStatement() {
        SWGHelp.display(SWGAide.class.getResource("docs/statement_en.html"));
    }

    /**
     * Starts a timer that regularly backups the DAT file
     */
    private void startBackupTimer() {
        Timer timer = new Timer(10 * 60 * 1000, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                putToStatbar("Autosave");

                // only backup if auto-store was successful, otherwise we
                // destroys the previous backup
                File dat = new File("SWGAide.DAT");
                if (getPrefsKeeper().store(dat))
                    ZWriter.copy(dat, new File(
                        SWGConstants.backupDir, "SWGAide.BAK"));
            }
            });
        timer.start();
    }

    /**
     * Helper method which performs some post-launch update routines. If SWGAide
     * or its DAT file is up-to-date this method does nothing. Otherwise this
     * method may display dialogs or perform other tasks which are best done
     * after the GUI is visible.
     */
    private void updatePostLaunch() {
        //Empty as the former necessary updates are very old
    }

    /**
     * Helper method which performs some pre-launch update routines. If SWGAide
     * or its DAT file is up-to-date this method does nothing. Otherwise this
     * method may display dialogs or perform other tasks which must be done
     * before continuing launch of SWGAide.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private void updatePreLaunch() {
    	String pkver = SWGFrame.getPrefsKeeper().getVersion();
        if (pkver.contains("MrMiagi")) {
        	String[] parts = pkver.split("-MrMiagi-");
        	String ver = parts[1];
        	
        	// I don't trust compareTo methods on version numbering so splitting into integers 
        	String[] vparts = ver.split("\\.");
        	int ord1 = Integer.parseInt(vparts[0]);
        	int ord2 = Integer.parseInt(vparts[1]);
        	int ord3 = Integer.parseInt(vparts[2]);
        	
        	/**
        	 * Begin conversion of old Inventory map, taking into account any duplicates and trying to fix them
        	 */
        	// checks if version in dat file older than or equal to 0.1.9
        	if ( ord1 <= 0 && ord2 <= 1 && ord3 <= 9 ) {
        		
        		SWGAide.printDebug(Thread.currentThread().getName(), 9, "SWGFrame:updatePreLaunch doing upgrade  Data: " + pkver);
        		
				Map<Object, Map<String, List<Object>>> oldMap =
                        (Map<Object, Map<String, List<Object>>>)
                        SWGFrame.getPrefsKeeper().get(
                                "resourceInventoryMap",
                                new TreeMap<Object,
                                Map<String, List<Object>>>());
        		Set<Object> glist = new TreeSet<Object>();
        		oldMap.forEach( (g,v) -> {
                	glist.add(g.toString());
                });
        		Set<Object> alist = new TreeSet<Object>();
        		oldMap.forEach( (g,v) -> {
                	v.forEach( (ass, d) -> {
                		Object k = g + "@" + ass;
                		alist.add(k);
                	});
                });
        		SWGAide.printDebug(Thread.currentThread().getName(), 9, "SWGFrame:updatePreLaunch dump alist  Data: " + alist);
        		
        		TreeSet<Object> anlist = new TreeSet<Object>();
        		Set<Object> tmpr = new LinkedHashSet();
        		List<Object> tmpnr = new ArrayList();
        		HashMap<String, List<Object>> tmpags = new HashMap<String, List<Object>>();
                Map<String, Map<String, List<Object>>> tempmap = new HashMap<String, Map<String, List<Object>>>();
                for (Object g : glist) {
                	for (Object o : alist) {
                		String[] parts1 = o.toString().split("@");
                    	String gxy = parts1[0];
                    	String a = parts1[1];
                    	if(g.toString().equals(gxy)) {
                    		anlist.add(a);
                    		Iterator<Entry<Object, Map<String, List<Object>>>> parent = oldMap.entrySet().iterator();
                            while (parent.hasNext()) {
                                Entry<Object, Map<String, List<Object>>> parentPair = parent.next();
                                String gg = parentPair.getKey().toString();
                                if(gg.equals(gxy)) {
	                                Iterator<Entry<String, List<Object>>> child = (parentPair.getValue()).entrySet().iterator();
	                                while (child.hasNext()) {
	                                    Map.Entry childPair = child.next();
	                                    String ass = childPair.getKey().toString();
	                                    List<Object> d = (List<Object>) childPair.getValue();
	                                    if(ass.equals(a)) {
                    						for (Object res : d) {
                        						tmpr.add(res);
                        					}
                    					}
	
	                                    child.remove(); // avoids a ConcurrentModificationException
	                                }
                                }

                            }
                    		tmpnr.clear();
        					tmpnr.addAll(tmpr);
        					tmpags.put(a, tmpnr);
        					
        					tmpr = new LinkedHashSet();
        			        tmpnr = new ArrayList();
                    	}
                    	tempmap.put(g.toString(), tmpags);
                	}
                	tmpags = new HashMap<String, List<Object>>();
                	anlist = new TreeSet<Object>();
                	}
                SWGAide.printDebug(Thread.currentThread().getName(), 9, "SWGFrame:updatePreLaunch tempmap  Data: " + tempmap);
                
                SWGFrame.getPrefsKeeper().remove("resourceInventoryMap");
                
                Map<String, Map<String, List<Object>>> inventoryMap =
                        (Map<String, Map<String, List<Object>>>)
                        SWGFrame.getPrefsKeeper().get(
                                "resourceInventoryMap",
                                new TreeMap<String,
                                Map<String, List<Object>>>());
                inventoryMap.putAll(tempmap);
                
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					//
				}
                SWGAide.printDebug(Thread.currentThread().getName(), 1, "SWGFrame:updatePreLaunch DAT file upgrade complete ");
                JOptionPane pane = new JOptionPane("\nSWGAide.DAT file has been upgraded to the new version\n"
                		+ "Please restart the application after clicking OK.\nThank You",JOptionPane.PLAIN_MESSAGE);
                JDialog d = pane.createDialog(null, "SWGAide-NGE Upgrade Complete");
                d.pack();
                d.setModal(false);
                d.setVisible(true);
                while (pane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e) {
                	  //
                  }
                }
                // call the doExit() so things get saved to the DAT
                doExit();
        	}
        	// XXX End of Inventory Map conversion. ^^
        	/*
        	 * Begin update for stat colors. This just nukes the stored pref for colors
        	 * so the new defaults will show. Color picker never worked before this update
        	 * so all should be fine.
        	 */
        	// checks if version in dat file older than or equal to 0.1.10
        	if ( ord1 <= 0 && ord2 <= 1 && ord3 <= 10 ) {
        		SWGFrame.getPrefsKeeper().remove("resourceColorFair");
        		SWGFrame.getPrefsKeeper().remove("resourceColorGood");
        		SWGFrame.getPrefsKeeper().remove("resourceColorGreat");
        	}
        	// XXX End stat color update ^^
        	/*
        	 * Begin update for font size. This just nukes the stored pref for the old unused keys.
        	 * 
        	 */
        	// checks if version in dat file older than or equal to 0.1.11
        	if ( ord1 <= 0 && ord2 <= 1 && ord3 <= 11 ) {
        		SWGFrame.getPrefsKeeper().remove("mailBodyFontSize");
        		SWGFrame.getPrefsKeeper().remove("aliasPaneFontSize");
        		SWGFrame.getPrefsKeeper().remove("macrosPaneFontSize");
        		SWGFrame.getPrefsKeeper().remove("notesPaneFontSize");
        	}
        	// XXX End font size update ^^
        	// checks if version in dat file older than or equal to 0.1.20
        	if ( ord1 <= 0 && ord2 <= 1 && ord3 <= 20 ) {
        		SWGCraftCache.getserversXMLpath().delete();
        		SWGCraftCache.updateCache();
        		// we must clear the resource cache for ID integer sanity
        		SWGResourceManager.clearcache();
        		SWGFrame.getPrefsKeeper().remove("resourceInventoryMap");
        		SWGFrame.getPrefsKeeper().remove("resourceMonitorMap");
        		SWGFrame.getPrefsKeeper().remove("resourceGeneralMap");
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					//
				}
        		// add the new prefs key for verified credentials
        		Boolean verified = false;
        		SWGFrame.getPrefsKeeper().add("optionVerified", verified);
        		SWGFrame.getPrefsKeeper().add("resourceInventoryMap", new TreeMap<String,Map<String, List<Object>>>());
        		//SWGResourceManager.updateMainGalaxy();
        		JOptionPane pane = new JOptionPane("\nSWGAide.DAT file has been upgraded to the new version\n"
                		+ "Please restart the application after clicking OK.\nThank You",JOptionPane.PLAIN_MESSAGE);
                JDialog d = pane.createDialog(null, "SWGAide-NGE Upgrade Complete");
                d.pack();
                d.setModal(false);
                d.setVisible(true);
                while (pane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException e) {
                	  //
                  }
                }
        		doExit();
        	}
        } else {
        	// Putting a dialogue here and exit if trying to launch with an incompatible DAT file.
        	JOptionPane pane = new JOptionPane("\nYour SWGAide.DAT file is incompatible with this version\n"
        			+ "SWGAide-NGE is not compatible with pre-cu or other earlier versions.\nExiting",JOptionPane.PLAIN_MESSAGE);
            JDialog d = pane.createDialog(null, "SWGAide.DAT Incompatible");
            d.pack();
            d.setModal(false);
            d.setVisible(true);
            while (pane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
            	  //
              }
            }
            // calling system exit so nothing gets saved to the DAT
            System.exit(0);
        }
    }

    public void windowActivated(WindowEvent e) {
    //
    }

    public void windowClosed(WindowEvent e) {
    //
    }

    public void windowClosing(WindowEvent e) {
        doExit();
    }

    public void windowDeactivated(WindowEvent e) {
    //
    }

    public void windowDeiconified(WindowEvent e) {
    //
    }

    public void windowIconified(WindowEvent e) {
    //
    }

    public void windowOpened(WindowEvent e) {
    //
    }

    public void windowStateChanged(WindowEvent e) {
        int id = e.getID();
        // actually, we are atm only interested in mximize/restore actions and
        // to have them saved into prefsKeeper
        if (id == WindowEvent.WINDOW_STATE_CHANGED) {
            int extState = getExtendedState();
            if (extState == Frame.NORMAL) {
                getPrefsKeeper().add("frameMaximized", Boolean.FALSE);
            } else if (extState == Frame.MAXIMIZED_BOTH) {
                getPrefsKeeper().add("frameMaximized", Boolean.TRUE);
            }
        }
    }

    /**
     * Returns the file chooser object for this application
     * 
     * @return the file chooser object
     */
    public static synchronized JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setPreferredSize(new Dimension(800, 600));
            fileChooser.setLocation(10, 10);
        }

        return fileChooser;
    }

    /**
     * Returns SWGAide's object storage. This is a key/value dictionary that
     * must only hold objects that are Serializable which must be stored for
     * future use. If this type is not yet instantiated {@code null} is
     * returned.
     * <P>
     * Developers must carefully read the comments in {@link SimplePrefsKeeper}.
     * 
     * @return SWGAide's object storage, or {@code null}
     */
    public static SimplePrefsKeeper getPrefsKeeper() {
        return prefsKeeper;
    }

    /**
     * Returns the resource manager which is the only interface regarding
     * resources. If this type is not yet instantiated {@code null} is returned.
     * <P>
     * Developers must carefully read the comments in {@link SWGResource}.
     * 
     * @return the resource manager
     */
    public static SWGResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * Returns the character that was selected most recently, or {@code null}.
     * If the user selects a non-character node this value is not changed,
     * compare with {@link #getSelectedGalaxy()}. If no character is ever
     * selected this method returns {@code null}.
     * 
     * @return a character
     */
    public static SWGCharacter getSelectedCharacter() {
        return (SWGCharacter) SWGFrame.getPrefsKeeper().get(
                "currentlySelectedCharacter");
    }

    /**
     * Returns the galaxy constant for the currently selected character, or
     * {@link SWGCGalaxy#defaultGalaxy()}. If the user selects a galaxy node
     * which is not for the current character this value is not changed; is
     * short for {@code getSelectedCharacter().getGalaxy().getGalaxy()}. If no
     * character is even selected this method returns the constant for SWGCraft.
     * 
     * @return a galaxy constant
     */
    public static SWGCGalaxy getSelectedGalaxy() {
        SWGCharacter c = getSelectedCharacter();
        return c == null
        		? SWGCGalaxy.defaultGalaxy()
                : c.gxy();
    }
}
