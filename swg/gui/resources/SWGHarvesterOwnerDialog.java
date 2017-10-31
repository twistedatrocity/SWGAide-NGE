package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJDialog;
import swg.model.SWGCharacter;
import swg.tools.SpringUtilities;
import swg.tools.ZNumber;

/**
 * This type is a dialog for editing a harvester-owner.
 * 
 * @author John Thomas aka Chilastra.Oarun
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGHarvesterOwnerDialog extends SWGJDialog implements ActionListener {

    /**
     * The cancel button
     */
    private JButton cancelButton;

    /**
     * The harvester currently being created/edited
     */
    private SWGHarvesterOwner currentOwner;

    /**
     * A text input field for the energy value
     */
    private JTextField energyValue;

    /**
     * A text input field for the harvest fair buff value
     */
    private JTextField fairValue;

    /**
     * A text input field for the maintenance value
     */
    private JTextField maintenanceValue;

    /**
     * The OK button
     */
    private JButton okButton;

    /**
     * A drop down for the owner name
     */
    private JComboBox<String> ownerName;

    /**
     * A text input field for the storage value
     */
    private JTextField storageValue;

    /**
     * A text input field for the technology value
     */
    private JTextField techValue;

    /**
     * Creates an instance of this dialog.
     * 
     * @param harvesterTab the parent for this instance
     */
    SWGHarvesterOwnerDialog(SWGHarvestingTab harvesterTab) {
        super("Harvester Owner", harvesterTab);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 7, 7, 7));

        contentPane.add(makeContent(), BorderLayout.NORTH);
        contentPane.add(makeButtons(), BorderLayout.EAST);

        setContentPane(contentPane);
        setMinimumSize(getSize());

        Point p = harvesterTab.getLocation();
        p.translate(100, 100);
        p = (Point) SWGFrame.getPrefsKeeper().get(
                "resourceHarvesterOwnerDialogLocation", p);
        p = SWGGuiUtils.ensureOnScreen(p, getSize());
        setLocation(p);

        pack();
        setMinimumSize(getSize());
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cancelButton) {
            // pass
        } else if (src == okButton) {
            int[] smetf;
            if ((smetf = isDialogOK()) != null)
                saveOwner(smetf);
            else
                return;
        } else {
            SWGAide.printError("HarvesterOwnerDialog:actionPerformed: " + src,
                null);
        }
        close();
    }

    @Override
    protected void close() {
        SWGFrame.getPrefsKeeper().add(
                "resourceHarvesterOwnerDialogLocation", getLocation());
        setVisible(false);
    }

    /**
     * Initiates the GUI for creating a new owner.
     */
    private void initCreateNewOwner() {
        ownerName.removeAllItems();
        List<String> names = SWGGuiUtils.characterNames(SWGResourceTab.galaxy());
        for (String n : names)
            ownerName.addItem(n);
        SWGCharacter c = SWGFrame.getSelectedCharacter();
        ownerName.setSelectedItem(c != null
            ? c.getName()
            : null);
        storageValue.setText("4");
        maintenanceValue.setText("4");
        energyValue.setText("4");
        techValue.setText("2");
        fairValue.setText("5");
    }

    /**
     * Initiates the GUI dialog with the currently selected owner.
     */
    private void initEditOwner() {
        ownerName.removeAllItems();
        ownerName.addItem(currentOwner.getName());
        ownerName.setSelectedItem(currentOwner.getName());
        storageValue.setText(toString(ZNumber.asText(
            currentOwner.getStorageEfficiency(), true, true)));
        maintenanceValue.setText(toString(ZNumber.asText(
            currentOwner.getMaintEfficiency(), true, true)));
        energyValue.setText(toString(ZNumber.asText(
            currentOwner.getEnergyEfficiency(), true, true)));
        techValue.setText(toString(ZNumber.asText(
            currentOwner.getHarvestingTechnology(), true, true)));
        fairValue.setText(toString(ZNumber.asText(
            currentOwner.getHarvestFair(), true, true)));
    }

    /**
     * Determines of the dialog has valid content. This method returns an
     * integer array with the parsed <I>modifier values&nbsp;</I> if a owner
     * name is defined and the integer fields have valid values. If the content
     * is invalid this method raises a GUI message dialog on the matter and
     * returns {@code null}.
     * 
     * @return an integer array for the parsed integer values storage,
     *         maintenance, energy, adv-tech, fair, or {@code null} if there is
     *         an error
     */
    private int[] isDialogOK() {
        String msg = null;
        String ttl = null;
        String owner = (String) ownerName.getSelectedItem();
        if (owner.length() <= 2) {
            msg = "Enter a owner name 3 letters or more";
            ttl = "Invalid owner name";
        } else if (currentOwner == null
                && SWGResController.harvesterOwnerExists(owner,
                        SWGResourceTab.galaxy())) {
            msg = "A owner exists with this name";
            ttl = "Owner name conflict";
        }

        int[] smetf = null;
        if (msg == null) {
            try {
                int s = ZNumber.intExc(storageValue.getText());
                int m = ZNumber.intExc(maintenanceValue.getText());
                int e = ZNumber.intExc(energyValue.getText());
                int t = ZNumber.intExc(techValue.getText());
                int f = ZNumber.intExc(fairValue.getText());

                if (s < 0 || s > 4) {
                    msg = "Enter Storage expertise 0 to 4";
                    ttl = "Invalid Storage Expertise";
                } else if (m < 0 || m > 4) {
                    msg = "Enter Maintenance expertise 0 to 4";
                    ttl = "Invalid Maintenance Expertise";
                } else if (e < 0 || e > 4) {
                    msg = "Enter Energy expertise 0 to 4";
                    ttl = "Invalid Energy Expertise";
                } else if (t < 0 || t > 2) {
                    msg = "Enter Advanced Tech expertise 0 to 2";
                    ttl = "Invalid Tech Expertise";
                } else if (f < 0 || f > 5) {
                    msg = "Enter Harvester Fair 0 to 5";
                    ttl = "Invalid Buff Value";
                }
                smetf = new int[] { s, m, e, t, f };
            } catch (Exception e) {
                msg = "Enter a valid integer";
                ttl = "Not a number";
            }
        }

        if (msg == null)
            return smetf;

        JOptionPane.showMessageDialog(ownerName, msg, ttl,
                JOptionPane.ERROR_MESSAGE);
        return null;
    }

    /**
     * Returns a GUI component for the OK and Cancel button.
     * 
     * @return a GUI component
     */
    private Component makeButtons() {
        Box bottom = Box.createHorizontalBox();

        okButton = new JButton("OK");
        okButton.setToolTipText("Add to the list of owners");
        okButton.setMnemonic('O');
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.addActionListener(this);
        bottom.add(okButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('C');
        cancelButton.addActionListener(this);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottom.add(cancelButton);

        return bottom;
    }

    /**
     * Returns the GUI component for the owners expertise modifiers.
     * 
     * @return a GUI component
     */
    private Component makeContent() {
        JPanel content = new JPanel();
        content.setLayout(new SpringLayout());

        ownerName = new JComboBox<String>();
        ownerName.setPreferredSize(new Dimension(150, 24));
        ownerName.setAlignmentX(Component.LEFT_ALIGNMENT);
        ownerName.setToolTipText("Select a character");
        ownerName.setAutoscrolls(true);
        ownerName.setEditable(false);

        JLabel lc = new JLabel("Owner name");
        content.add(lc);
        content.add(ownerName);

        storageValue = new JTextField("", 15);
        storageValue.setToolTipText("Expertise points in "
            + "Harvester Storage Efficiency (0 to 4)");

        JLabel ls = new JLabel("Harvester Storage Efficiency");
        content.add(ls);
        content.add(storageValue);

        maintenanceValue = new JTextField("", 15);
        maintenanceValue.setToolTipText("Expertise points in "
            + "Harvester Maintenance Efficiency (0 to 4)");

        JLabel lm = new JLabel("Harvester Maintenance Efficiency");
        content.add(lm);
        content.add(maintenanceValue);

        energyValue = new JTextField("", 15);
        energyValue.setToolTipText("Expertise points in "
            + "Harvester Energy Efficiency (0 to 4)");

        JLabel le = new JLabel("Harvester Energy Efficiency");
        content.add(le);
        content.add(energyValue);

        techValue = new JTextField("", 15);
        techValue.setToolTipText("Expertise points in "
            + "Advanced Harvester Technology (0 to 2)");

        JLabel lt = new JLabel("Advanced Harvester Technology");
        content.add(lt);
        content.add(techValue);

        fairValue = new JTextField("", 15);
        fairValue
            .setToolTipText("Level of Harvest Fair (0 to 5), Entertainer buff");

        JLabel lh = new JLabel("Harvest Fair Buff");
        content.add(lh);
        content.add(fairValue);

        SpringUtilities.makeCompactGrid(content, 6, 2, 0, 0, 5, 3);
        return content;
    }

    /**
     * Called when the user clicks "OK" and {@link #isDialogOK()} determined
     * that the content at the dialog is valid. This method saves the owner to
     * the list of owners for the current galaxy.
     * 
     * @param smetf an integer array for the integer values storage,
     *        maintenance, energy, tech, fair parsed from the GUI
     */
    private void saveOwner(int[] smetf) {
        String n = (String) ownerName.getSelectedItem();
        if (currentOwner == null) { // equals "create owner"
            SWGResController.harvesterOwnerAdd(
                    new SWGHarvesterOwner(
                            n, smetf[0], smetf[1], smetf[2], smetf[3], smetf[4]),
                    SWGResourceTab.galaxy());
        } else {
            currentOwner.setStorageEfficiency(smetf[0]);
            currentOwner.setMaintEfficiency(smetf[1]);
            currentOwner.setEnergyEfficiency(smetf[2]);
            currentOwner.setHarvestingTechnology(smetf[3]);
            currentOwner.setHarvestFair(smetf[4]);
        }
        ((SWGHarvestingTab) parent).resetOwners();
    }

    /**
     * Invoked when the user selects to create or edit specified owner. This
     * method initiates the GUI.
     * 
     * @param owner
     *            a owner to edit, or {@code null} for creating a new owner
     */
    void showAndBegin(SWGHarvesterOwner owner) {

        currentOwner = owner;

        if (owner == null)
            initCreateNewOwner();
        else
            initEditOwner();

        ownerName.setEditable(owner == null);
        ownerName.setEnabled(owner == null);

        setVisible(true);
    }

    /**
     * Helper method which returns the string integer as is, or "0" id the
     * argument is {@code null}.
     * 
     * @param integer
     *            a string to return
     * @return the string, or "0" id the argument is {@code null}
     */
    private String toString(String integer) {
        return integer == null
            ? "0"
            : integer;

    }
}
