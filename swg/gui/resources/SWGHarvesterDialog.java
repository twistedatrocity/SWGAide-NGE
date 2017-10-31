package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJDialog;
import swg.tools.ZNumber;
import swg.tools.SpringUtilities;

/**
 * This type is the dialog for creating or editing a harvester.
 * <P>
 * October 2009 most fields of a <I>defined&nbsp;</I> harvester were made final,
 * but {@link #name} can be updated at this dialog.
 * 
 * @author John Thomas aka Chilastra.Oarun
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGHarvesterDialog extends SWGJDialog implements ActionListener {

    /**
     * The GUI text input field for the harvesters BER.
     */
    private JTextField BER;

    /**
     * The cancel button.
     */
    private JButton cancelButton;

    /**
     * A check-box for cloning a harvester. If this dialog is about editing an
     * existing harvester this item is enabled and if the user selects this item
     * and clicks OK a new harvester is created with the same properties as the
     * existing harvester, assuming the name is unique.
     */
    private JCheckBox clone;

    /**
     * The harvester currently being created/edited, {@code null} if creating a
     * new harvester.
     */
    private SWGHarvester currentHarvester;

    /**
     * The GUI text input field for the harvesters hopper capacity.
     */
    private JTextField hopper;

    /**
     * The GUI text input field for the harvester's user defined name.
     */
    private JTextField name;

    /**
     * The OK button.
     */
    private JButton okButton;

    /**
     * The GUI drop down list for the harvester type.
     */
    private JComboBox<String> type;

    /**
     * Creates an instance of this dialog.
     * 
     * @param harvesterTab the parent component for this dialog
     */
    SWGHarvesterDialog(SWGHarvestingTab harvesterTab) {
        super(harvesterTab);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 7, 7, 7));

        contentPane.add(makeContent(), BorderLayout.NORTH);
        contentPane.add(makeButtons(), BorderLayout.SOUTH);

        setContentPane(contentPane);
        pack();
        setMinimumSize(getSize());

        Point p = harvesterTab.getLocation();
        p.translate(100, 100);
        p = (Point) SWGFrame.getPrefsKeeper().get(
                "resourceHarvesterDialogLocation", p);
        p = SWGGuiUtils.ensureOnScreen(p, getSize());
        setLocation(p);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cancelButton) {
            // pass
        } else if (src == okButton) {
            if (isDialogOK())
                saveHarvester();
            else
                return;
        } else if (src == clone) {
            name.requestFocusInWindow();
            return;
        } else {
            SWGAide.printError("HarvesterDialog:actionPerformed: " + src, null);
        }
        close();
    }

    /**
     * Called when the user selects a harvester type at the harvester combo.
     * 
     * @param e
     *            the user action
     */
    private void actionTypeSelected(@SuppressWarnings("unused") ItemEvent e) {
        if (type.getSelectedIndex() < 0)
            return;

        String harvType = (String) type.getSelectedItem();
        if (name.getText().isEmpty())
            name.setText(harvType);
    }

    @Override
    protected void close() {
        SWGFrame.getPrefsKeeper().add(
                "resourceHarvesterDialogLocation", getLocation());
        setVisible(false);
    }

    /**
     * Initiates the GUI for creating a new harvester.
     */
    private void initCreate() {
        setTitle("Create Harvester");

        clone.setEnabled(false);
        clone.setToolTipText("Clone an existing harvester");

        type.setSelectedIndex(-1);
        type.setToolTipText("Select a harvester type");
        type.setEnabled(true);

        name.setText(null); // always mutable

        BER.setText(null);
        BER.setToolTipText("Specify the base BER value");
        BER.setEnabled(true);

        hopper.setText(null);
        hopper.setToolTipText("Specify the base hopper size");
        hopper.setEnabled(true);
    }

    /**
     * Initiates the GUI dialog with the currently selected harvester. Only the
     * user definable name is mutable.
     * 
     * @param makeClone {@code true} if this dialog is for a "Clone" action
     */
    private void initEdit(boolean makeClone) {
        setTitle(makeClone
                ? "Clone Harvester"
                : "Edit Harvester");
        clone.setSelected(makeClone);

        clone.setEnabled(true);
        clone.setToolTipText("Create a clone of this harvester, " +
                "define a unique description");

        type.setSelectedItem(currentHarvester.type);
        type.setToolTipText("The harvester type");
        type.setEnabled(false);

        name.setText(currentHarvester.getName()); // always mutable

        BER.setText(ZNumber.asText(currentHarvester.ber, true, true));
        BER.setToolTipText("The Base Extraction Rate");
        BER.setEnabled(false);

        hopper.setText(ZNumber.asText(currentHarvester.hopperSize, true, true));
        hopper.setToolTipText("The harvester's hopper size");
        hopper.setEnabled(false);
    }

    /**
     * Determines if the content at the GUI dialog is valid. This method returns
     * {@code true} if there is a harvester type selected, a BER > 0, hopper
     * capacity > 0, and a description. Otherwise this method displays a GUI
     * message dialog on the matter and returns {@code false}.
     * 
     * @return {@code true} if the content at the dialog is valid
     */
    private boolean isDialogOK() {
        String msg = null;
        String ttl = null;
        try {
            String typ = (String) type.getSelectedItem();
            int h = ZNumber.intExc(hopper.getText());
            int b = ZNumber.intExc(BER.getText());

            if (typ == null || typ.isEmpty()) {
                msg = "Select a harvester type";
                ttl = "No harvester type";
            } else if (name.getText().length() < 3) {
                msg = "Enter a description, 3 letters or more";
                ttl = "Invalid description";
            } else if (SWGResController.harvestersExists(
                    name.getText(), SWGResourceTab.galaxy())) {
                msg = "A harvester with this name exists";
                ttl = "Name conflict";
            } else if (b <= 0) {
                msg = "Enter a BER value greater than 0";
                ttl = "Invalid BER value";
            } else if (b > 44) {
                // XXX: make this check for harvester type too
                msg = "Enter a BER value 44 or less";
                ttl = "Invalid BER value";
            } else if (h <= 0) {
                msg = "Enter a hopper size greater than 0";
                ttl = "Invalid size value";
            } else if (h > 500000) {
                msg = "Enter a correct hopper size";
                ttl = "Invalid size value";
            }
        } catch (Exception e) {
            msg = "Enter a valid value (integer)";
            ttl = "Not a number";
        }

        if (msg != null) {
            JOptionPane.showMessageDialog(name, msg, ttl,
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Creates and returns a GUI component for the OK and Cancel button.
     * 
     * @return a GUI component
     */
    private Component makeButtons() {
        Box bottom = Box.createHorizontalBox();

        clone = new JCheckBox("Clone");
        clone.addActionListener(this);
        bottom.add(clone);

        bottom.add(Box.createHorizontalGlue());

        okButton = new JButton("OK");
        okButton.setToolTipText("Add to the list of harvesters");
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
     * Creates and returns the GUI component for the harvester attributes.
     * 
     * @return a GUI component
     */
    private Component makeContent() {
        JPanel content = new JPanel();
        content.setLayout(new SpringLayout());

        type = new JComboBox<String>(SWGHarvester.TYPES);
        type.setPreferredSize(new Dimension(180, 26));
        type.setAlignmentX(Component.LEFT_ALIGNMENT);
        type.setAutoscrolls(true);
        type.addItemListener(new ItemListener() {

            @SuppressWarnings("synthetic-access")
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    actionTypeSelected(e);
                }
            }
        });

        JLabel lt = new JLabel("Harvester type", SwingConstants.LEFT);
        content.add(lt);
        content.add(type);

        name = new JTextField("", 15);
        name.setToolTipText("Enter a description for the harvester");

        JLabel ld = new JLabel("Description");
        ld.setToolTipText("Enter a description for the harvester");
        content.add(ld);
        content.add(name);

        BER = new JTextField("", 15);

        JLabel ln = new JLabel("Harvester BER");
        content.add(ln);
        content.add(BER);

        hopper = new JTextField("", 15);

        JLabel lc = new JLabel("Hopper size", SwingConstants.LEFT);
        content.add(lc);
        content.add(hopper);

        SpringUtilities.makeCompactGrid(content, 4, 2, 0, 0, 5, 3);
        return content;
    }

    /**
     * Called when the user selects to save the harvester. It is assumed that
     * before invoking this method the content at the GUI is validated by
     * {@link #isDialogOK()}. If this action pertains to creating a new
     * harvester this method displays a GUI confirmation dialog reminding about
     * harvesters' being immutable and if the user selects to continue the
     * created harvester is saved to the list of harvesters at the current
     * galaxy. If this action is about editing an existing harvester only the
     * description, its name, is saved.
     */
    private void saveHarvester() {
        try {
            if (currentHarvester == null || clone.isSelected()) { // create new
                // this far isDialogOK has validated input
                SWGResController.harvestersAdd(new SWGHarvester(
                        name.getText(), (String) type.getSelectedItem(),
                        ZNumber.intExc(BER.getText()),
                        ZNumber.intExc(hopper.getText())),
                        SWGResourceTab.galaxy());
            } else
                currentHarvester.setName(name.getText());

        } catch (Exception e) {
            SWGAide.printError("HarvesterDialog:saveHarvester", e);
        }

        ((SWGHarvestingTab) parent).resetHarvesters();
    }

    /**
     * Called when the user selects to create or edit an existing harvester.
     * This method initiates the GUI and displays this dialog.
     * 
     * @param harvester
     *            the harvester to edit, {@code null} when about to create a new
     *            harvester
     * @param makeClone
     *            {@code true} if the user selected the option "Create Clone"
     */
    void showAndBegin(SWGHarvester harvester, boolean makeClone) {
        currentHarvester = harvester;
        if (harvester == null)
            initCreate();
        else
            initEdit(makeClone);

        setVisible(true);
    }
}
