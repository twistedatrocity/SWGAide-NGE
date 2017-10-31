package swg.gui.resources;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import swg.SWGAide;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResourceClass;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJDialog;
import swg.tools.SimplePrefsKeeper;
import swg.tools.SpringUtilities;
import swg.tools.ZNumber;

/**
 * This type is the dialog for activating a defined harvester or editing one
 * which is actively harvesting a known resource. In particular, this is the
 * dialog related to the harvesting of a particular resource with a specified
 * harvester.
 * 
 * @author John Thomas aka Chilastra.Oarun
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGHarvesterActivateDialog extends SWGJDialog implements
    ActionListener {

    // This dialog works in two distinct modes:
    //
    // 1) Activating a harvester
    // This includes selecting a harvester and owner, setting initial values for
    // resource concentration, amount of maintenance, amount of power, etc.
    // If the selected harvester was deactivated/re-deeded the selected owner
    // updates the fields for bonuses.
    // Otherwise, if it stayed put, the harvester has retained the bonuses from
    // the previous owner, if a new owner is selected these bonuses stick. If
    // the
    // harvester at that time also was self-powered that attribute sticks.
    //
    // 2) Editing an active harvester
    // Most fields are deactivated and cannot be edited. In particular,
    // expertise bonuses, entertainer buff, and self-power attribute cannot be
    // changed*. The ownership can change but the bonuses stick.
    //
    // * Limitation: in-game, if there is a new owner and he replenish power
    // and/or maintenance the bonuses for these attributes are reset. Currently
    // SWGAide does not model this.

    /**
     * A check box where the user can select to auto-add to inventory.
     */
    private JCheckBox addToInv;

    /**
     * The cancel button.
     */
    private JButton cancelButton;

    /**
     * A text input field for the size of a cluster of harvesters.
     */
    private JTextField clusterCount;

    /**
     * A text input field for the resource concentration value.
     */
    private JTextField concentration;

    /**
     * The harvester currently being activated.
     */
    private SWGHarvester currentHarvester;

    /**
     * A list of harvesters. If {@link #currentResource} is not {@code null}
     * this list is filtered on resource class.
     */
    private List<SWGHarvester> currentHarvs;

    /**
     * The owner of the currentHarvester.
     */
    private SWGHarvesterOwner currentOwner;

    /**
     * The resource being harvested by currentHarvester.
     */
    private SWGKnownResource currentResource;

    /**
     * A label to display current details for the selected harvester.
     */
    private JLabel harvBer;

    /**
     * A label to display current details for the selected harvester.
     */
    private JLabel harvBonuses;

    /**
     * List of defined harvesters to select from.
     */
    private JComboBox<String> harvesterList;

    /**
     * A label to display current details for the selected harvester.
     */
    private JLabel harvHopper;

    /**
     * A label to display current details for the selected harvester.
     */
    private JLabel harvName;

    /**
     * A label to display current details for the selected harvester.
     */
    private JLabel harvOwnerEnergy;

    /**
     * A slider to display and edit details for the selected harvester.
     */
    private JSlider harvOwnerFair;

    /**
     * A label to display current details for the selected harvester.
     */
    private JLabel harvOwnerHopper;

    /**
     * A label to display current details for the selected harvester.
     */
    private JLabel harvOwnerMaint;

    /**
     * A label to display current details for the selected harvester.
     */
    private JLabel harvOwnerTech;

    /**
     * Helper flag to cancel action events while updating the GUI.
     */
    private boolean isWorking = false;

    /**
     * A text input field for the harvesters maintenance value.
     */
    private JTextField maintenance;

    /**
     * A text input area for misc notes regarding this harvester.
     */
    private JTextArea notesField;

    /**
     * The OK button.
     */
    private JButton okButton;

    /**
     * List of defined owners to select from
     */
    private JComboBox<String> ownerList;

    /**
     * A text input field for the harvesters power value.
     */
    private JTextField power;

    /**
     * A field for displaying the resource class of the selected resource.
     */
    private JTextField resourceClass;

    /**
     * A field for the name of the resource.
     */
    private JTextField resourceName;

    /**
     * A check box where the user selects if the harvester is self-powered.
     */
    private JCheckBox selfPowered;

    /**
     * Creates an instance of this dialog.
     * 
     * @param harvesterTab the component which is the parent for this instance
     */
    SWGHarvesterActivateDialog(SWGHarvestingTab harvesterTab) {
        super("Activate Harvester", harvesterTab);

        JPanel contentPane = new JPanel();

        // XXX: add traversal order support

        Box vb = Box.createVerticalBox();
        vb.add(makeTopRow());
        vb.add(makeSecondRow());
        vb.add(makeBottomRow());

        Box vdb = Box.createVerticalBox();
        vdb.add(makeDetailsPanel());
        vdb.add(Box.createVerticalGlue());
        vdb.add(makeClusterPanel());

        Box hb = Box.createHorizontalBox();
        hb.add(vb);
        hb.add(Box.createHorizontalStrut(5));
        hb.add(vdb);

        contentPane.add(hb);

        this.setContentPane(contentPane);

        Point p = harvesterTab.getLocation();
        p.translate(100, 100);
        p = (Point) SWGFrame.getPrefsKeeper().get(
                "resourceHarvesterActivateLocation", p);
        p = SWGGuiUtils.ensureOnScreen(p, this.getSize());
        this.setLocation(p);
    }

    /**
     * Called when the user selects a harvester from the GUI list of harvesters.
     */
    private void actionHarvesterSelected() {
        int idx = harvesterList.getSelectedIndex();
        if (idx < 0 || isWorking)
            return;

        SWGHarvester harv = currentHarvs.get(idx);

        if (harv.isActive()) {
            SWGAide.printError("HarvesterActivate:actionHarvSelected",
                new IllegalStateException("Is active"));
            closeDialog(); // sanity
        }
        currentHarvester = harv;

        // we are abandoning a harvester in progress, so we need to
        // save any collected resource to the inventory tab if the
        // addToInvetory flag is set

        // if the harvester is an energy harvester we force the self-powered
        // attribute to be always selected since they are by nature self-powered
        selfPowered.setSelected(currentHarvester.isSelfPowered());

        if (currentHarvester.isSelfPowered()) {
            power.setEnabled(false);
            power.setToolTipText("Power generators are self-powered");
            selfPowered.setEnabled(false);
            selfPowered.setToolTipText("Power generators are self-powered");
        } else {
            power.setEnabled(true);
            power.setToolTipText("Enter harvester power");
            selfPowered.setEnabled(true);
            selfPowered
                .setToolTipText("Modify this harvester to be self-powered");
        }
        refreshHarvDetails();
    }

    /**
     * Called when the user selects a name is selected from the GUI owner list.
     * This method updates {@code currentOwner}.
     * 
     * @param e the event which triggered this action
     */
    private void actionOwnerSelected(@SuppressWarnings("unused") ItemEvent e) {
        String sel = (String) ownerList.getSelectedItem();
        if (sel == null || isWorking) return;

        currentOwner = ((SWGHarvestingTab) parent).getOwner(sel);
        refreshHarvDetails();
    }

    public void actionPerformed(ActionEvent e) {
        if (isWorking) return;

        Object src = e.getSource();
        if (src == cancelButton) {
            // pass
        } else if (src == okButton) {
            okButton.setEnabled(false); // user may click twice on a laggy host
            if (!isDialogOK()) {
                okButton.setEnabled(true);
                return;
            }
            ((SWGHarvestingTab) parent).resetActiveHarvesters();
            SWGResourceTab.currentUpdateGUI();
            okButton.setEnabled(true);
        } else
            SWGAide.printError("HarvesterActivate:actionPerformed" + src, null);

        closeDialog();
    }

    /**
     * Called when the user clicks mouse at the harvester chooser list and at
     * the panel which displays harvester details. If the action event is a
     * right-hand mouse click and if the harvester is inactive and if the
     * harvesting bonuses are in "stayed-put" mode (as opposed to if the
     * harvester was re-deeded) this method resets the previous bonuses, buff,
     * and self-powered attribute. If any of the three requirements are false
     * this method does nothing.
     * 
     * @param e
     *            the event which triggered this action
     */
    private void actionResetHarvester(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3
            && currentHarvester != null
            && !currentHarvester.isActive()
            && currentHarvester.getOwner() != null
            && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
            ownerList, "Reset this harvester to deed state?\n" +
            "This removes harvesting bonuses", "Confirm",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {

            // reset and remove bonuses
            currentHarvester.activate(false, true);
            actionHarvesterSelected();
        }
    }

    @Override
    protected void close() {
        cancelButton.doClick();
    }

    /**
     * Called when the user decides to close this dialog. This method saves the
     * dialog's position and sets it invisible.
     */
    private void closeDialog() {
        SWGFrame.getPrefsKeeper().add("resourceHarvesterActivateLocation",
            getLocation());

        setVisible(false);
    }

    /**
     * Helper method which returns a list of harvesters which can harvest the
     * specified resource class. If no harvester is found for the resource class
     * an empty list is returned. If the dialog is in editing mode the list
     * contains only the active harvester.
     * 
     * @param rc the resource class to filter by
     * @return a list of harvesters,or an empty list
     */
    private List<SWGHarvester> filterHarvs(SWGResourceClass rc) {
        List<SWGHarvester> hs = new ArrayList<SWGHarvester>();
        if (currentHarvester != null)
            hs.add(currentHarvester);
        else
            for (SWGHarvester h : SWGResController.harvesters(
                    SWGResourceTab.galaxy()))
                if (!h.isActive() && rc.isSub(SWGHarvester.getType(h)))
                    hs.add(h);

        return hs;
    }

    /**
     * Initiates the GUI for a harvester which is active. This method sets the
     * GUI to display values from the arguments to {@code
     * showAndBegin(harvester, resource)}.
     */
    private void initiateActiveEntry() {
        String name = String.format("<html><i>%s</i></html>",
            currentHarvester.getName());
        harvesterList.setSelectedItem(name);
        harvesterList.setEnabled(false);

        ownerList.setSelectedItem(currentOwner.getName());

        concentration.setText(ZNumber.asText(currentHarvester.getConcentration(), true, true));
        maintenance.setText(ZNumber.asText(currentHarvester.getMaint(), true, false));
        power.setText(ZNumber.asText(currentHarvester.getPower(), true, false));
        clusterCount.setText(ZNumber.asText(currentHarvester.getSeveral(), true, false));
        notesField.setText(currentHarvester.getNotes());
        addToInv.setSelected(currentHarvester.addToInventory);
        selfPowered.setSelected(currentHarvester.isSelfPowered());

        // cannot remove the self-powered state from active harvester, it must
        // be re-deeded
        selfPowered.setEnabled(!currentHarvester.isSelfPowered());
    }

    /**
     * Prepares for activating a harvester. This method cleans the GUI to start
     * fresh, or selects some initial entries determined by the arguments to
     * {@code showAndBegin(harvester, resource)}.
     * 
     * @param notes notes for the harvesters notes field, or {@code null}
     */
    private void initiateNewEntry(String notes) {
        SimplePrefsKeeper pk = SWGFrame.getPrefsKeeper();

        harvesterList.setSelectedIndex(-1);
        concentration.setText("0");
        maintenance.setText(ZNumber.asText((Long) pk.get(
                "resourceHarvesterActivateMaint", Long.valueOf(0))));
        power.setText(ZNumber.asText((Long) pk.get(
                "resourceHarvesterActivatePower", Long.valueOf(0))));
        clusterCount.setText("1");
        power.setEnabled(true);
        notesField.setText(notes);
        selfPowered.setEnabled(true);
        selfPowered.setSelected(false);

        if (currentHarvs.size() == 1) {
            harvesterList.setSelectedIndex(0);
            currentHarvester = currentHarvs.get(0);
        }

        harvesterList.setEnabled(true);
        ownerList.setSelectedIndex(-1); // nobody was found
    }

    /**
     * Determines if the content at this dialog is valid. If there is an error a
     * message is displayed and {@code false} is returned. Otherwise, if the
     * content is valid this method invokes {@code updateHarvester()} and
     * finally {@code true} is returned.
     * 
     * @return {@code true} if the dialog has valid content, {@code false}
     *         otherwise
     */
    private boolean isDialogOK() {
        String msg = null;
        String ttl = null;

        if (currentResource == null) {
            msg = "Select a resource to be harvested";
            ttl = "Resource Missing";
        } else if (currentHarvester == null) {
            msg = "Select a harvester to activate";
            ttl = "Harvester Missing";
        } else if (currentOwner == null) {
            msg = "Select a owner";
            ttl = "Owner Missing";
        }

        // if the cells have text, check their validity
        if (msg == null) {
            try {
                long p = ZNumber.longExc(power.getText());
                long m = ZNumber.longExc(maintenance.getText());
                int c = ZNumber.intExc(concentration.getText());
                int x = ZNumber.intExc(clusterCount.getText());

                if (c < 1 || c > 100) {
                    msg = "Enter a concentration percent (integer 1 - 100)";
                    ttl = "Invalid Concentration";
                } else if (m < 1) {
                    msg = "Enter a maintenance value";
                    ttl = "Invalid Maintenance";
                } else if (p < 0) {
                    msg = "Enter a power value";
                    ttl = "Invalid Power";
                } else if (x < 1) {
                    msg = "Enter a cluster size greater than 0";
                    ttl = "Invalid Cluster Size";
                }
            } catch (Exception e) {
                msg = "Enter valid integers";
                ttl = "Not a number";
            }
        }

        if (msg != null) {
            JOptionPane.showMessageDialog(ownerList, msg, ttl,
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        updateHarvester();
        if (currentHarvester.addToInventory) {
            // add ONE initial unit to inventory
            String ass = currentHarvester.getOwner().getName();
            SWGInventoryWrapper wr = new SWGInventoryWrapper(
                    currentResource, ass);
            wr.setAmount(1);
            wr.setNotes(currentHarvester.getNotes());
            SWGResController.inventoryAdd(wr, SWGResourceTab.galaxy());
        }

        SimplePrefsKeeper pk = SWGFrame.getPrefsKeeper();
        pk.add("resourceHarvesterActivateMaint",
                Long.valueOf(ZNumber.longVal(maintenance.getText())));
        pk.add("resourceHarvesterActivatePower",
                Long.valueOf(ZNumber.longVal(power.getText())));

        return true;
    }

    /**
     * Helper method which creates and returns a GUI component for the lower
     * area of the dialog. This component contains the notes field, the check
     * boxes for inventory and self-powered, the input fields for concentration,
     * maintenance, and power, and the buttons OK and Cancel.
     * 
     * @return a GUI component
     */
    private Component makeBottomRow() {
        Box bottom = Box.createHorizontalBox();

        notesField = new JTextArea(4, 15);
        notesField.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesField.setLineWrap(true);
        notesField.setWrapStyleWord(true);
        JScrollPane ns =
            new JScrollPane(notesField,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ns.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel ln = new JLabel("Misc notes", SwingConstants.LEFT);

        Box nb = Box.createVerticalBox();
        nb.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        nb.add(ln);
        nb.add(ns);
        bottom.add(nb);

        Box iBox = Box.createHorizontalBox();
        iBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        addToInv = new JCheckBox("Add to Inventory");
        addToInv.setToolTipText("Automatically add totals to the inventory");
        addToInv.doClick();
        iBox.add(addToInv);

        Box sBox = Box.createHorizontalBox();
        sBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        selfPowered = new JCheckBox("Self powered");
        selfPowered.setToolTipText("Modify this harvester to be self powered");
        sBox.add(selfPowered);

        // XXX: add document input listeners that validates for numbers,
        // possibly also make it so if user types fraction value at
        // concentration it is converted to integer

        Box pBox = Box.createHorizontalBox();
        pBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        concentration = new JTextField("", 7);
        concentration.setMaximumSize(new Dimension(60, 21));
        concentration.setToolTipText("Enter the resource concentration");
        pBox.add(concentration);
        JLabel tl = new JLabel(" Concentration %", SwingConstants.LEFT);
        tl.setToolTipText("Enter the resource concentration (integer)");
        pBox.add(tl);

        Box mBox = Box.createHorizontalBox();
        mBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        maintenance = new JTextField("", 7);
        maintenance.setMaximumSize(new Dimension(60, 21));
        maintenance.setToolTipText("Enter harvester maintenance");
        mBox.add(maintenance);
        JLabel tm = new JLabel(" Maintenance", SwingConstants.LEFT);
        tm.setToolTipText("Enter harvester maintenance");
        mBox.add(tm);

        Box poBox = Box.createHorizontalBox();
        poBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        power = new JTextField("", 7);
        power.setMaximumSize(new Dimension(60, 21));
        power.setToolTipText("Enter harvester power");
        poBox.add(power);
        JLabel tp = new JLabel(" Power", SwingConstants.LEFT);
        tp.setToolTipText("Enter harvester power");
        poBox.add(tp);

        Box btb = Box.createHorizontalBox();
        okButton = new JButton("OK");
        okButton.setToolTipText("Activate this harvester");
        okButton.setMnemonic('O');
        okButton.addActionListener(this);
        btb.add(okButton);

        btb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('C');
        cancelButton.addActionListener(this);
        btb.add(cancelButton);

        Box ob = Box.createVerticalBox();
        ob.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        ob.add(new JLabel("     "));
        ob.add(iBox);
        ob.add(sBox);
        ob.add(pBox);
        ob.add(mBox);
        ob.add(poBox);
        ob.add(btb);

        bottom.add(ob);
        return bottom;
    }

    /**
     * Creates and returns a panel for specifying how many in-game harvesters
     * the current harvester denotes.
     * 
     * @return east-south GUI component
     */
    private Component makeClusterPanel() {
        JPanel southEast = new JPanel();
        southEast.setLayout(new SpringLayout());
        southEast.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
            " Harvester Cluster "));

        clusterCount = new JTextField();
        clusterCount.setToolTipText(
            "Specify the in-game cluster of harvesters this harvester denotes "
            + "(multiplies amount added to inventory)");
        clusterCount.setAutoscrolls(true);
        clusterCount.setPreferredSize(new Dimension(60, 26));
        clusterCount.setMaximumSize(new Dimension(175, 26));

        JLabel lc = new JLabel("Count: ");
        lc.setToolTipText(
            "Specify the in-game cluster of harvesters this harvester denotes "
            + "(multiplies amount added to inventory)");

        southEast.add(lc);
        southEast.add(clusterCount);

        SpringUtilities.makeCompactGrid(southEast, 1, 2, 0, 0, 0, 0);

        return southEast;
    }

    /**
     * Creates and returns a panel for displaying harvester details.
     * 
     * @return east-most GUI component
     */
    private Component makeDetailsPanel() {
        JPanel east = new JPanel();
        east.setLayout(new SpringLayout());
        east.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Harvester Details "));

        east.add(new JLabel("Name: ", SwingConstants.TRAILING));
        harvName = new JLabel();
        east.add(harvName);

        east.add(new JLabel("BER: ", SwingConstants.TRAILING));
        harvBer = new JLabel();
        east.add(harvBer);

        east.add(new JLabel("Hopper: ", SwingConstants.TRAILING));
        harvHopper = new JLabel();
        east.add(harvHopper);

        east.add(new JLabel(" "));
        east.add(new JLabel("                                         "));

        east.add(new JLabel("Bonuses: ", SwingConstants.TRAILING));
        harvBonuses = new JLabel();
        east.add(harvBonuses);

        east.add(new JLabel("Storage: ", SwingConstants.TRAILING));
        harvOwnerHopper = new JLabel();
        east.add(harvOwnerHopper);

        east.add(new JLabel("Maintance: ", SwingConstants.TRAILING));
        harvOwnerMaint = new JLabel();
        east.add(harvOwnerMaint);

        east.add(new JLabel("Energy: ", SwingConstants.TRAILING));
        harvOwnerEnergy = new JLabel();
        east.add(harvOwnerEnergy);

        east.add(new JLabel("Adv Tech: ", SwingConstants.TRAILING));
        harvOwnerTech = new JLabel();
        east.add(harvOwnerTech);

        east.add(new JLabel("Harv Fair: ", SwingConstants.TRAILING));
        harvOwnerFair = new JSlider(SwingConstants.HORIZONTAL, 0, 5, 0);
        harvOwnerFair.setMajorTickSpacing(1);
        harvOwnerFair.setPaintLabels(true);
        harvOwnerFair.setPaintTicks(true);
        harvOwnerFair.setSnapToTicks(true);
        east.add(harvOwnerFair);

        SpringUtilities.makeCompactGrid(east, 10, 2, 0, 0, 0, 0);

        east.addMouseListener(new MouseAdapter() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionResetHarvester(e);
            }
        });

        return east;
    }

    /**
     * Helper method which creates and returns the GUI element for the resource
     * properties. This component contains the field for the resource name and
     * resource class.
     * 
     * @return a GUI component
     */
    private Component makeSecondRow() {
        Box middle = Box.createHorizontalBox();

        resourceName = new JTextField();
        resourceName.setToolTipText("The name of the resource");
        resourceName.setAlignmentX(Component.LEFT_ALIGNMENT);
        resourceName.setAutoscrolls(true);
        resourceName.setPreferredSize(new Dimension(110, 26));
        resourceName.setMaximumSize(new Dimension(175, 26));
        resourceName.setEditable(false);

        JLabel ln = new JLabel("Resource name");
        ln.setToolTipText("The name of the resource");
        ln.setAlignmentX(Component.LEFT_ALIGNMENT);
        Box tn = Box.createVerticalBox();
        tn.add(ln);
        tn.add(resourceName);
        middle.add(tn);

        resourceClass = new JTextField();
        resourceClass.setToolTipText("Name of the resource class");
        resourceClass.setAlignmentX(Component.LEFT_ALIGNMENT);
        resourceClass.setAutoscrolls(true);
        resourceClass.setPreferredSize(new Dimension(110, 26));
        resourceClass.setMaximumSize(new Dimension(175, 26));
        resourceClass.setEditable(false);

        Box tc = Box.createVerticalBox();
        JLabel lc = new JLabel("Resource class", SwingConstants.LEFT);
        lc.setToolTipText("Name of the resource class");
        tc.add(lc);
        tc.add(resourceClass);
        middle.add(tc);

        return middle;
    }

    /**
     * Helper method which creates and returns the GUI elements for the upper
     * area of the dialog. This component contains the drop down lists for the
     * harvesters and owners.
     * 
     * @return a GUI component
     */
    private Component makeTopRow() {
        Box top = Box.createHorizontalBox();

        harvesterList = new JComboBox<String>();
        harvesterList.setPreferredSize(new Dimension(175, 26));
        harvesterList.setAlignmentX(Component.LEFT_ALIGNMENT);
        harvesterList.setToolTipText("Select a harvester");
        harvesterList.setAutoscrolls(true);
        harvesterList.addItemListener(new ItemListener() {

            @SuppressWarnings("synthetic-access")
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    actionHarvesterSelected();
                }
            }
        });
        harvesterList.addMouseListener(new MouseAdapter() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionResetHarvester(e);
            }
        });

        Box tn = Box.createVerticalBox();
        JLabel ln = new JLabel("Harvester");
        ln.setToolTipText("Select a harvester");
        tn.add(ln);
        tn.add(harvesterList);
        top.add(tn);

        ownerList = new JComboBox<String>();
        ownerList.setPreferredSize(new Dimension(175, 26));
        ownerList.setAlignmentX(Component.LEFT_ALIGNMENT);
        ownerList.setToolTipText("Select a owner");
        ownerList.setAutoscrolls(true);
        ownerList.addItemListener(new ItemListener() {

            @SuppressWarnings("synthetic-access")
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    actionOwnerSelected(e);
                }
            }
        });

        Box tc = Box.createVerticalBox();
        JLabel lc = new JLabel("Owner", SwingConstants.LEFT);
        lc.setToolTipText("Select a owner");
        tc.add(lc);
        tc.add(ownerList);
        top.add(tc);

        return top;
    }

    /**
     * Helper method which refreshes the content at the details panel.
     */
    private void refreshHarvDetails() {
        harvName.setText(currentHarvester == null
                ? null
                : currentHarvester.getName());
        harvBer.setText(currentHarvester == null
                ? null
                : ZNumber.asText(currentHarvester.ber, true, true));
        harvHopper.setText(currentHarvester == null
                ? null
                : ZNumber.asText(currentHarvester.hopperSize, true, true));

        // bonuses
        if (currentHarvester != null) {
            harvBonuses.setText(currentHarvester.isActive()
                    ? "applied"
                    : currentHarvester.getOwner() != null
                            ? "applied (stayed put)"
                            : currentOwner == null
                                    ? "pending (deed)"
                                    : "pending - " + currentOwner.getName());
            // active and getOwner != null >> harv has valid bonus values
            boolean ownerExists = currentHarvester.getOwner() != null;
            harvOwnerHopper.setText(ownerExists
                    ? ZNumber.asText(currentHarvester.getStorageEfficiencyLevel(), true, true)
                    : currentOwner == null
                            ? null
                            : ZNumber.asText(currentOwner.getStorageEfficiency(), true, true));
            harvOwnerMaint.setText(ownerExists
                    ? ZNumber.asText(currentHarvester.getMaintenanceEfficiencyLevel(), true, true)
                    : currentOwner == null
                            ? null
                            : ZNumber.asText(currentOwner.getMaintEfficiency(), true, true));
            harvOwnerEnergy.setText(ownerExists
                    ? ZNumber.asText(currentHarvester.getEnergyEfficiencyLevel(), true, true)
                    : currentOwner == null
                            ? null
                            : ZNumber.asText(currentOwner.getEnergyEfficiency(), true, true));
            harvOwnerTech.setText(ownerExists
                    ? ZNumber.asText(currentHarvester.getHarvestingTechnologyLevel(), true, true)
                    : currentOwner == null
                            ? null
                            : ZNumber.asText(currentOwner.getHarvestingTechnology(), true, true));
            harvOwnerFair.setValue(ownerExists
                    ? currentHarvester.getHarvestFair()
                    : currentOwner == null
                            ? 0
                            : currentOwner.getHarvestFair());
            harvOwnerFair.setEnabled(!currentHarvester.isActive()
                    && currentHarvester.getOwner() == null);
        } else {
            harvBonuses.setText("(harvester pending)");
            harvOwnerHopper.setText(null);
            harvOwnerMaint.setText(null);
            harvOwnerEnergy.setText(null);
            harvOwnerTech.setText(null);
            harvOwnerFair.setValue(0);
            harvOwnerFair.setEnabled(false);
        }
    }

    /**
     * Helper method which refreshes the content of the dynamic GUI lists for
     * harvesters and owners. This method is invoked when this dialog is made
     * visible and re-populates the mentioned drop down lists.
     */
    private void resetComboLists() {
        harvesterList.removeAllItems();
        ownerList.removeAllItems();
        resourceName.setText(null);
        resourceClass.setText(null);

        isWorking = true;
        for (SWGHarvester harvester : currentHarvs) {
            String name = harvester.getName();
            if (harvester.isActive())
                name = String.format("<html><i>%s</i></html>", name);
            else if (harvester.getOwner() == null)
                name = String.format("%s (deed)", name);
            harvesterList.addItem(name);
        }

        List<SWGHarvesterOwner> ows = ((SWGHarvestingTab) parent).getOwners();
        for (SWGHarvesterOwner owner : ows)
            ownerList.addItem(owner.getName());

        isWorking = false;
    }

    /**
     * Called when the user selects to open this dialog. This method is called
     * from the panel for "Current Resources" and from the harvesting tab when
     * about to create or edit an active harvester entry respectively. If the
     * harvester is {@code null} this call is about activating a harvester,
     * otherwise it is about editing.
     * <p>
     * If the call is to activate a harvester it is possible to pass along a a
     * string which is tentatively set as notes. The {@code notes} argument is
     * ignored in the case {@code harvester} is not {@code null}.
     * 
     * @param harvester
     *            an active harvester to edit, or {@code null}
     * @param resource
     *            the resource
     * @param notes
     *            notes for the harvesters notes field, or {@code null}
     * @throws IllegalArgumentException
     *             if the resource is {@code null}
     */
    void showAndBegin(SWGHarvester harvester, SWGKnownResource resource,
        String notes) {
        if (resource == null)
            throw new IllegalArgumentException("Resource is null");
        currentResource = resource;

        // set these early since updater methods depend on the two fields
        currentHarvester = harvester;
        currentOwner = harvester == null
            ? null
            : harvester.getOwner();

        currentHarvs = filterHarvs(currentResource.rc());
        Collections.sort(currentHarvs);
        if (currentHarvs.size() <= 0) {
            JOptionPane.showMessageDialog(this,
                String.format("No suitable harvester is defined for:" +
                "%n\"%s\"", currentResource.rc().rcName()),
                "No Harvester Type",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        resetComboLists();

        resourceName.setText(currentResource.getName());
        resourceClass.setText(currentResource.rc().rcName());
        resourceClass.setCaretPosition(0); // for really long class names

        if (harvester != null)
            initiateActiveEntry();
        else
            initiateNewEntry(notes);

        refreshHarvDetails();

        pack();
        setVisible(true);
        setMinimumSize(getSize());
    }

    /**
     * Helper method which updates the current harvester with the values parsed
     * from this dialog. This method is invoked by {@code isDialogOK()} and then
     * only if it is determined that the content at this dialog which is entered
     * by the user is valid.
     * <P>
     * <B>Note: </B>this method is invoked also if there is no content change
     * but the user clicks OK rather than cancels the dialog. Thus this
     * implementation determines whether fields must be updated or not so the
     * harvester object is not refreshed or reset without reason.
     * <P>
     * This method must also consider whether the current harvester is active,
     * this dialog is in editing mode, or if the harvester is activated. In the
     * latter case this implementation determines if bonuses, buff, and
     * self-powered attribute should be updated or if old bonuses must stick.
     */
    private void updateHarvester() {
        currentHarvester.setConcentration(ZNumber.intVal(
                concentration.getText()));
        currentHarvester.setSeveral(ZNumber.intVal(clusterCount.getText()));
        currentHarvester.setMaint(ZNumber.longVal(maintenance.getText()));
        currentHarvester.setNotes(notesField.getText());
        currentHarvester.addToInventory = addToInv.isSelected();

        // these are always safe, also if no content is changed
        currentHarvester.setResource(currentResource);
        currentHarvester.setSelfPowered(selfPowered.isSelected());
        currentHarvester.setPower(ZNumber.longVal(power.getText()));

        int old = currentOwner.getHarvestFair();
        int hf = harvOwnerFair.getValue();
        currentOwner.setHarvestFair(hf);
        if (old != hf && ((SWGHarvestingTab) parent).ownerTable != null)
            ((SWGHarvestingTab) parent).ownerTable.repaint();

        if (currentHarvester.isActive()) {
            // allow for structure-transfer without resetting bonuses
            currentHarvester.setOwner(currentOwner);
        } else {
            // owner must be set before activating
            currentHarvester.setOwner(currentOwner);
            // activate, the re-deed argument is void
            currentHarvester.activate(true, true);
        }
    }
}
