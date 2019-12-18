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
import swg.tools.SimplePrefsKeeper;
import swg.tools.ZCrypto;
import swg.tools.ZString;

/**
 * A GUI dialog for user options and preferences related to swgaide.com.
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
     * A GUI list of galaxies from which to select one main galaxy.
     */
    private JComboBox<String> galaxyList;

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
     * swgaide.com.
     */
    private JPasswordField passWord;

    /**
     * A GUI button to save user info to the preference keeper.
     */
    private JButton saveUserInfo;

    /**
     * The GUI component from which to select the user's time zone.
     */
    private JComboBox<String> timeZones;

    /**
     * A GUI text field for the user name used to connect to swgaide.com.
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
        super(frame, "SWGAide Options", false);

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

        contentPane.getInputMap().put(KeyStroke.getKeyStroke("F1"), "showHelp");
        contentPane.getActionMap().put("showHelp", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                showHelp();
            }
        });

        // TODO: have a second look at this construct, remove the isOpen flag
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent e) {
                if (isOpen)
                    return;
                focusGained();
                super.windowActivated(e);
            }

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
		else {
			SWGFrame.getResourceManager();
			SWGResourceManager.stopAutoUpdate();
		}
    }

    /**
     * This method is called when this dialog window is closed and it saves
     * preferences to SWGAide's preference keeper.
     */
    private void actionClose() {
        SWGFrame.getPrefsKeeper().add("optionsGeneralLocation", getLocation());
        SWGHelp.remove(helpUrl);
        isOpen = false;
    }

    /**
     * This method is called when a selection is made at {@link #galaxyList}.
     */
    private void actionGalaxySelected() {
        String glx = (String) galaxyList.getSelectedItem();
        SWGFrame.getPrefsKeeper().add("optionMainGalaxy", SWGCGalaxy.fromName(glx).id());
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
        	SWGFrame.verified = true;
        	SWGFrame.getPrefsKeeper().add("optionVerified", SWGFrame.verified);
            JOptionPane.showMessageDialog(verifyUserInfo,
                "User name and password are correct", "Valid",
                JOptionPane.INFORMATION_MESSAGE);
            // everything fine, stored user name/password are also fine
            return;
        }

        // else
        ZString z = new ZString();
        if (info.getFaultMessage().startsWith("Temporary")) {
            z.app(info.getFaultMessage());
        } else {
            z.appnl("User name and/or password are invalid,");
            z.appnl("or you have not registered at swgaide.com");
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

        galaxyList = new JComboBox<String>(new Vector<String>(SWGCGalaxy.namesActive()));
        galaxyList.setToolTipText(
                "Your main galaxy for which to download resources");
        galaxyList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionGalaxySelected();
            }
        });
        String glx = SWGCGalaxy.fromID((Integer) SWGFrame.getPrefsKeeper().get(
            "optionMainGalaxy", SWGCGalaxy.defaultGalaxy().id())).getName();
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

            public void actionPerformed(ActionEvent e) {
                actionAutoUpdateGalaxyToggled();
            }
        });
        glxPanel.add(autoUpdateGalaxyResources);

        glxPanel.add(Box.createGlue());

        return glxPanel;
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
        timeZones = new JComboBox<String>(vec);
        timeZones.setToolTipText("Select your time zone");
        timeZones.addActionListener(new ActionListener() {

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
            .createTitledBorder("User info for SWGAide.com"));

        JLabel uname = new JLabel("User name: ");
        uname.setToolTipText("User name used at SWGAide.com");
        uPanel.add(uname);

        userName = new JTextField(8);
        userName.setToolTipText("User name used at SWGAide.com");
        uPanel.add(userName);

        String name = (String) SWGFrame.getPrefsKeeper().get("optionUserName");
        userName.setText(name == null
            ? ""
            : name);

        uPanel.add(Box.createRigidArea(new Dimension(10, 10)));

        JLabel pwd = new JLabel("Password: ");
        pwd.setToolTipText("Password for SWGAide.com (never sent anywhere)");
        uPanel.add(pwd);

        passWord = new JPasswordField(8);
        passWord.setToolTipText("Password for SWGAide.com (never sent anywhere)");
        uPanel.add(passWord);

        String pswd = SWGCraft.getPassword();
        passWord.setText(pswd == null
            ? ""
            : pswd);

        uPanel.add(Box.createRigidArea(new Dimension(10, 10)));

        verifyUserInfo = new JButton("Verify");
        verifyUserInfo.setMnemonic('V');
        verifyUserInfo.setToolTipText("Verify user info with SWGAide.com");
        verifyUserInfo.addActionListener(new ActionListener() {

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
