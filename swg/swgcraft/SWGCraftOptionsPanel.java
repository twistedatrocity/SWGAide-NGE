package swg.swgcraft;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Vector;

import javax.crypto.SealedObject;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.model.SWGCGalaxy;
import swg.tools.ZCrypto;
import swg.tools.SimplePrefsKeeper;
import swg.tools.ZString;

/**
 * A GUI dialog for user options and preferences related to SWGCraft.org.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGCraftOptionsPanel extends JDialog {

    /**
     * The GUI check-box for selecting auto-update of galaxy resources for the
     * main galaxy.
     */
    private JCheckBox autoUpdateGalaxyResources;

    /**
     * The GUI text field for the base URL to SWGCraft.org.
     */
    private JTextField baseURL;

    /**
     * A GUI list of galaxies from which to select one main galaxy.
     */
    private JComboBox galaxyList;

    /**
     * The URL for the help page for this window.
     */
    private URL helpUrl;

    /**
     * A boolean flag which indicates whether this dialog is open or closed.
     */
   private boolean isOpen = false;

    /**
     * A GUI text field for the user's password for communication with
     * SWGCraft.org.
     */
    private JPasswordField passWord;

    /**
     * A GUI text field for the current-resource path that should be
     * concatenated with the base URL.
     */
    private JTextField resourcePath;

    /**
     * A GUI button to save user info to the preference keeper.
     */
    private JButton saveUserInfo;

    /**
     * The GUI check-box which unlocks the text fields for SWGCraft settings.
     */
    private JCheckBox serverUnlock;

    /**
     * A GUI text field for the path to the SOAP server.
     */
    private JTextField soapPath;

    /**
     * A GUI text field for the path to the server status file.
     */
    private JTextField statusPath;

    /**
     * The GUI component from which to select the user's time zone.
     */
    private JComboBox timeZones;

    /**
     * A GUI text field for the user name used to connect to SWGCraft.org.
     */
    private JTextField userName;

    /**
     * A GUI button with which the user verifies his/her user info.
     */
    private JButton verifyUserInfo;

    /**
     * Creates an instance of this options dialog.
     * 
     * @param frame
     *            the application's GUI frame
     */
    public SWGCraftOptionsPanel(final SWGFrame frame) {
        super(frame, "SWGCraft Options", false);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 7, 7, 7));

        helpUrl = SWGAide.class.getResource(
                "docs/help_swgcraft-options_en.html");

        Box first = Box.createHorizontalBox();
        first.add(makeGalaxyPanel());
        first.add(makeTimeZonePanel());
        contentPane.add(first);

        contentPane.add(makeUserInfoPanel());

        contentPane.add(makeServerPanel());

        contentPane.getInputMap().put(KeyStroke.getKeyStroke("F1"), "showHelp");
        contentPane.getActionMap().put("showHelp", new AbstractAction() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                showHelp();
            }
        });

        // TODO: have a second look at this construct, remove the isOpen flag
        this.addWindowListener(new WindowAdapter() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void windowActivated(WindowEvent e) {
                if (isOpen)
                    return;
                focusGained();
                super.windowActivated(e);
            }

            @SuppressWarnings("synthetic-access")
            @Override
            public void windowClosing(WindowEvent e) {
                actionClose();
                super.windowClosing(e);
            }
        });

        this.setContentPane(contentPane);
        this.pack();
        this.setMinimumSize(this.getSize());

        Point pp =
            (Point) SWGFrame.getPrefsKeeper().get("optionsGeneralLocation");
        if (pp == null) {
            pp = frame.getLocation();
            pp.x += 50;
            pp.y += 20;
        } else
            pp = SWGGuiUtils.ensureOnScreen(pp, getSize());
        setLocation(pp);
    }

    /**
     * This method is called when {@link #autoUpdateGalaxyResources} is changed.
     */
    private void actionAutoUpdateGalaxyToggled() {
        SWGFrame.getPrefsKeeper().add("optionAutoUpdate",
            Boolean.valueOf(autoUpdateGalaxyResources.isSelected()));
        if (autoUpdateGalaxyResources.isSelected())
            SWGFrame.getResourceManager().startAutoUpdate();
        else
            SWGFrame.getResourceManager().stopAutoUpdate();
    }

    /**
     * This method is called when this dialog window is closed and it saves
     * preferences to SWGAide's preference keeper.
     */
    private void actionClose() {
        SWGFrame.getPrefsKeeper().add("optionsGeneralLocation", getLocation());
        SWGFrame.getPrefsKeeper().add("optionBaseURL", getBaseURL());
        SWGFrame.getPrefsKeeper().add("optionResourcePath", getResourcePath());
        SWGFrame.getPrefsKeeper().add("optionStatusFile", getStatusPath());
        SWGFrame.getPrefsKeeper().add("optionSOAPpath", getSOAPpath());
        if (serverUnlock.isSelected())
            serverUnlock.doClick();
        SWGHelp.remove(helpUrl);
        isOpen = false;
    }

    /**
     * This method is called when a selection is made at {@link #galaxyList}.
     */
    private void actionGalaxySelected() {
        String glx = (String) galaxyList.getSelectedItem();
        SWGFrame.getPrefsKeeper().add("optionMainGalaxy", glx);
    }

    /**
     * This method is called when about to save user info.
     */
    private void actionSaveButton() {
        String uname = userName.getText();
        if (uname != null && !uname.isEmpty()) {
            SWGFrame.getPrefsKeeper().add("optionUserName", uname);

            String pswd = new String(passWord.getPassword());
            if (!pswd.isEmpty()) {
                SealedObject s = getEncrypted(pswd);
                SWGFrame.getPrefsKeeper().add("optionPassWord", s);
            }
        }
    }

    /**
     * This method is called when a time zone is selected.
     */
    private void actionTimeZoneSelected() {
        String value = (String) timeZones.getSelectedItem();
        if (value.startsWith("offset"))
            return;

        SWGFrame.getPrefsKeeper().add("optionTimeZone", value);

        value = value.substring(0, value.indexOf(' '));
        value = value.replace("+", "");
        value = value.replace("½", ".5");
        Double zon = new Double(value);
        SWGFrame.getPrefsKeeper().add("optionTimeZoneValue", zon);
    }

    /**
     * This method is called when the user toggles possibility to edit the
     * server data text fields.
     */
    private void actionUnlockServerData() {
        boolean b = serverUnlock.isSelected();
        baseURL.setEnabled(b);
        resourcePath.setEnabled(b);
        statusPath.setEnabled(b);
        soapPath.setEnabled(b);
    }

    /**
     * This method is called when the user wants to verify his/her
     * user-name/password.
     */
    private void actionVerifyUser() {
        SimplePrefsKeeper pKeeper = SWGFrame.getPrefsKeeper();

        // temporarily keep the current user data, reset later
        // this is since user may have edited GUI so now it differs from what is
        // in the prefsKeeper
        String uName = (String) pKeeper.get("optionUserName");
        SealedObject pWord = (SealedObject) pKeeper.get("optionPassWord");

        // store the user data visible at the GUI to the prefsKeeper, for now
        actionSaveButton();

        SWGSoapManager handler = SWGSoapManager.getSOAPHandler();
        handler.resetUserData(); // just in case

        SWGSoapUserDetailedResponse info = handler.requestUserInfoDetailed();
        if (info.isFaultless()) {
            JOptionPane.showMessageDialog(verifyUserInfo,
                "User name and password are correct", "Valid",
                JOptionPane.INFORMATION_MESSAGE);
            // everything fine, stored user name/password are also fine
            return;
        }

        // else
        ZString z = new ZString();
        if (info.getFaultMessage().startsWith("Temporary")) 
            z.app(info.getFaultMessage());
        else {
            z.appnl("User name and/or password are invalid,");
            z.appnl("or you have not registered at SWGCraft.org");
            z.app(info.getFaultMessage());
        }
        z.nl().nl();
        z.app("SWGAide will not be able to execute all features!");

        JOptionPane.showMessageDialog(verifyUserInfo, z.toString(), "Invalid",
            JOptionPane.ERROR_MESSAGE);

        // reset temporary user data to the prefsKeeper
        pKeeper.add("optionUserName", uName);
        pKeeper.add("optionPassWord", pWord);
        handler.resetUserData(); // reset just in case
    }

    /**
     * This method is called when this GUI dialog gains focus.
     */
    private void focusGained() {
        isOpen = true;
        SWGHelp.push(helpUrl);
    }

    /**
     * Returns the base URL as a string. If the string equals
     * {@link SWGCraft#getBaseURL()}, or if its length is zero, {@code null} is
     * returned.
     * 
     * @return the base URL as a string, or {@code null}
     */
    private String getBaseURL() {
        String burl = baseURL.getText().trim();
        if (burl.equals(SWGCraft.getBaseURL()) || burl.isEmpty()) return null;
        return burl;
    }

    /**
     * Returns the encrypted argument as a {@link SealedObject}. If there is an
     * error {@code null} is returned.
     * 
     * @param text
     *            the text to encrypt
     * @return the encrypted argument as a {@link SealedObject}, or {@code null}
     */
    private SealedObject getEncrypted(String text) {
        try {
            return ZCrypto.cipher(text);
        } catch (Exception e) {
            SWGAide.printError("SWGCraftOptionsPanel:getEncrypted", e);
        }
        return null;
    }

    /**
     * Returns the path to the file of current-resources at SWGCraft.org. If the
     * string equals {@link SWGCraft#getCurrentResourcesPath()}, or if its
     * length is zero, {@code null} is returned.
     * 
     * @return the path to the file of current-resources, or {@code null}
     */
    private String getResourcePath() {
        String str = resourcePath.getText();
        str = str.replace("%BASE%", "");
        str = str.replace("%GALAXY%.xml.gz", "").trim();
        if (str.equals(SWGCraft.getCurrentResourcesPath()) || str.isEmpty())
            return null;
        return str;
    }

    /**
     * Returns the path to the SOAP server at SWGCraft.org. If the string equals
     * {@link SWGCraft#getSOAPServerPath()}, or if its length is zero, {@code
     * null} is returned.
     * 
     * @return the path to the SOAP server, or {@code null}
     */
    private String getSOAPpath() {
        String soap = soapPath.getText();
        soap = soap.replaceAll("%BASE%", "").trim();
        if (soap.equals(SWGCraft.getSOAPServerPath()) || soap.isEmpty())
            return null;
        return soap;
    }

    /**
     * Returns the path to the status text file at SWGCraft.org. If the string
     * equals {@link SWGCraft#getStatusFileTXT()}, or if its length is zero,
     * {@code null} is returned.
     * 
     * @return the path to the status text file, or {@code null}
     */
    private String getStatusPath() {
        String stat = statusPath.getText();
        stat = stat.replace("%BASE%", "").trim();
        if (stat.equals(SWGCraft.getStatusFileTXT()) || stat.isEmpty())
            return null;
        return stat;
    }

    /**
     * Returns a vector of strings with common time zones and their
     * abbreviations.
     * 
     * @return a vector of strings with common time zones
     */
    private Vector<String> getTimeZones() {
        Vector<String> vec = new Vector<String>(33);
        vec.add("-12 (IDLW)");
        vec.add("-11 (NT)");
        vec.add("-10 (CAT, HST)");
        vec.add("-9 (AKST, YST) [HDT]");
        vec.add("-8 (PST) [AKDT, YDT, HADT]");
        vec.add("-7 (MST) [PDT]");
        vec.add("-6 (CST) [MDT]");
        vec.add("-5 (EST) [CDT]");
        vec.add("-4 (AST) [EDT]");
        vec.add("-3½ (NST)");
        vec.add("-3 [ADT]");
        vec.add("-2½ [NDT]");
        vec.add("-2 (AT)");
        vec.add("-1 (WAT)");
        vec.add("0 GMT, UTC (WET)");
        vec.add("+1 (CET) [BST, IST, WEDT]");
        vec.add("+2 (EET, ZP2) [CEDT]");
        vec.add("+3 (BT, ZP3) [EEDT]");
        vec.add("+4 (ZP4)");
        vec.add("+5 (ZP5)");
        vec.add("+5½ (IST)");
        vec.add("+6 (ZP6)");
        vec.add("+7 (CXT, ZP7)");
        vec.add("+8 (CCT, WST, ZP8) [WADT]");
        vec.add("+9 (JST, ZP9)");
        vec.add("+9½ [ACST]");
        vec.add("+10 (EST, GST, ZP10)");
        vec.add("+10½ [ACDT]");
        vec.add("+11 [EADT]");
        vec.add("+11½ (NFT)");
        vec.add("+12 (IDLE, NZT)");
        vec.add("+13 [NZDT]");
        vec.add("offset (standard time) [daylight time]");
        return vec;
    }

    /**
     * Returns a GUI component for the galaxy chooser.
     * 
     * @return a GUI component for the galaxy chooser
     */
    private Component makeGalaxyPanel() {
        JPanel glxPanel = new JPanel();
        glxPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        glxPanel.setBorder(BorderFactory.createTitledBorder("Main galaxy"));

        galaxyList = new JComboBox(new Vector<String>(SWGCGalaxy.namesActive()));
        galaxyList.setToolTipText(
                "Your main galaxy for which to download resources");
        galaxyList.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionGalaxySelected();
            }
        });
        String glx = (String) SWGFrame.getPrefsKeeper().get(
            "optionMainGalaxy", SWGCGalaxy.defaultGalaxy().getName());
        galaxyList.setSelectedItem(glx);
        glxPanel.add(galaxyList);

        autoUpdateGalaxyResources = new JCheckBox("Auto-update");
        autoUpdateGalaxyResources
            .setToolTipText("Automatically download resource statistics "
            + "for the selected galaxy");
        autoUpdateGalaxyResources.setSelected(((Boolean) SWGFrame
            .getPrefsKeeper().get("optionAutoUpdate", Boolean.FALSE))
            .booleanValue());
        autoUpdateGalaxyResources.addActionListener(new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionAutoUpdateGalaxyToggled();
            }
        });
        glxPanel.add(autoUpdateGalaxyResources);

        glxPanel.add(Box.createGlue());

        return glxPanel;
    }

    /**
     * Returns a panel containing a label with {@code labelText}.
     * 
     * @param labelText
     *            the text
     * @return a panel containing a label
     */
    private JPanel makePanelRow(String labelText) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(labelText);
        label.setMinimumSize(new Dimension(90, 20));
        label.setMaximumSize(new Dimension(90, 20));
        label.setPreferredSize(new Dimension(90, 20));
        panel.add(label);
        return panel;
    }

    /**
     * Returns a GUI component for the SWGCraft settings.
     * 
     * @return a GUI component for the SWGCraft settings
     */
    private JPanel makeServerFirstRow() {
        JPanel first = makePanelRow("Base URL: ");

        String baseURLs =
            (String) SWGFrame.getPrefsKeeper().get("optionBaseURL");
        baseURLs = baseURLs != null
            ? baseURLs
            : SWGCraft.getBaseURL();
        baseURL = new JTextField(baseURLs, 32);
        baseURL.setEnabled(false);
        first.add(baseURL);

        serverUnlock = new JCheckBox("Unlock");
        serverUnlock.setToolTipText("Unlock the input fields for editing");
        serverUnlock.addActionListener(new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionUnlockServerData();
            }
        });
        first.add(serverUnlock);
        return first;
    }

    /**
     * Returns a GUI component for editing URLs to SWGCraft.
     * 
     * @return a GUI component for editing URLs to SWGCraft
     */
    private Component makeServerPanel() {
        JPanel srvP = new JPanel();
        srvP.setBorder(BorderFactory.createTitledBorder("SWGCraft data"));
        srvP.setLayout(new BoxLayout(srvP, BoxLayout.Y_AXIS));

        JPanel first = makeServerFirstRow();
        srvP.add(first);

        JPanel second = makeServerSecondRow();
        srvP.add(second);

        JPanel third = makeServerThirdRow();
        srvP.add(third);

        return srvP;
    }

    /**
     * Returns a GUI component for the SWGCraft settings.
     * 
     * @return a GUI component for the SWGCraft settings
     */
    private JPanel makeServerSecondRow() {
        JPanel second = makePanelRow("Resource path: ");

        String resPath =
            (String) SWGFrame.getPrefsKeeper().get("optionResourcePath");
        resPath = resPath != null
            ? resPath
            : SWGCraft.getCurrentResourcesPath();
        resPath = "%BASE%" + resPath + "%GALAXY%.xml.gz";
        resourcePath = new JTextField(resPath, 38);
        resourcePath.setToolTipText("Part of URL to server resource data");
        resourcePath.setEnabled(false);
        second.add(resourcePath);
        return second;
    }

    /**
     * Returns a GUI component for the SWGCraft settings.
     * 
     * @return a GUI component for the SWGCraft settings
     */
    private JPanel makeServerThirdRow() {
        JPanel third = makePanelRow("Status file: ");

        String statStr =
            (String) SWGFrame.getPrefsKeeper().get("optionStatusFile");
        statStr = statStr != null
            ? statStr
            : SWGCraft.getStatusFileTXT();
        statStr = "%BASE%" + statStr;
        statusPath = new JTextField(statStr, 16);
        statusPath.setToolTipText("Part of URL to the server status file");
        statusPath.setEnabled(false);
        third.add(statusPath);

        JLabel soap = new JLabel("    SOAP: ");
        third.add(soap);

        String soapStr =
            (String) SWGFrame.getPrefsKeeper().get("optionSOAPpath");
        soapStr = soapStr != null
            ? soapStr
            : SWGCraft.getSOAPServerPath();
        soapStr = "%BASE%" + soapStr;
        soapPath = new JTextField(soapStr, 16);
        soapPath.setToolTipText("Part of URL to the SOAP server");
        soapPath.setEnabled(false);
        third.add(soapPath);

        return third;
    }

    /**
     * Returns a GUI component for the time zones.
     * 
     * @return a GUI component for the time zones
     */
    private Component makeTimeZonePanel() {
        JPanel timeP = new JPanel();
        timeP.setBorder(BorderFactory.createTitledBorder("Time zone"));

        Vector<String> vec = getTimeZones();
        timeZones = new JComboBox(vec);
        timeZones.setToolTipText("Select your time zone");
        timeZones.addActionListener(new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionTimeZoneSelected();
            }

        });
        String sel = (String) SWGFrame.getPrefsKeeper().get("optionTimeZone");
        timeZones.setSelectedItem(sel == null
            ? "0 GMT, UTC (WET)"
            : sel);

        timeP.add(timeZones);

        return timeP;
    }

    /**
     * Returns a GUI component for the user info related to SWGCraft.
     * 
     * @return a GUI component for the user info related to SWGCraft
     */
    private Component makeUserInfoPanel() {
        JPanel uPanel = new JPanel();
        uPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        uPanel.setBorder(BorderFactory
            .createTitledBorder("User info for SWGCraft"));

        JLabel uname = new JLabel("User name: ");
        uname.setToolTipText("User name used at SWGCraft");
        uPanel.add(uname);

        userName = new JTextField(8);
        userName.setToolTipText("User name used at SWGCraft");
        uPanel.add(userName);

        String name = (String) SWGFrame.getPrefsKeeper().get("optionUserName");
        userName.setText(name == null
            ? ""
            : name);

        uPanel.add(Box.createRigidArea(new Dimension(10, 10)));

        JLabel pwd = new JLabel("Password: ");
        pwd.setToolTipText("Password for SWGCraft (never sent anywhere)");
        uPanel.add(pwd);

        passWord = new JPasswordField(8);
        passWord.setToolTipText("Password for SWGCraft (never sent anywhere)");
        uPanel.add(passWord);

        String pswd = SWGCraft.getPassword();
        passWord.setText(pswd == null
            ? ""
            : pswd);

        uPanel.add(Box.createRigidArea(new Dimension(10, 10)));

        verifyUserInfo = new JButton("Verify");
        verifyUserInfo.setMnemonic('V');
        verifyUserInfo.setToolTipText("Verify user info with SWGCraft");
        verifyUserInfo.addActionListener(new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionVerifyUser();
            }
        });
        uPanel.add(verifyUserInfo);

        verifyUserInfo.setEnabled(true);

        uPanel.add(Box.createRigidArea(new Dimension(10, 10)));

        saveUserInfo = new JButton("Save");
        saveUserInfo.setMnemonic('S');
        saveUserInfo.setToolTipText("Save username and password locally");
        saveUserInfo.addActionListener(new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionSaveButton();
            }
        });
        uPanel.add(saveUserInfo);

        return uPanel;
    }

    /**
     * Opens the help page for this object.
     */
    private void showHelp() {
        SWGHelp.display();
    }
}
