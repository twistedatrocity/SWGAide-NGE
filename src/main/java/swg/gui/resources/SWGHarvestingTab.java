package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;

import swg.SWGAide;
import swg.SWGConstants;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGDecoratedTableCellRenderer.DecoratedTableModel;
import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;
import swg.gui.common.SWGDocNumberFilter;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGJTable;
import swg.gui.common.SWGTableCellEditor;
import swg.gui.schematics.SWGSchemController;
import swg.gui.schematics.SWGSchematicTab;
import swg.tools.SpringUtilities;
import swg.tools.ZNumber;
import swg.tools.ZReader;
import swg.tools.ZString;
import swg.tools.ZStuff;
import swg.tools.ZWriter;

/**
 * The GUI component for harvesters. The main section of this component is the
 * table which displays active harvesters and their current status. The upper
 * section displays defined harvesters, some user options, and defined
 * harvester-owners.
 * <P>
 * This component is the brilliant work of Oarun. In October 2009, as a side
 * effect of the deep reaching changes to resources and a tough overhaul of the
 * resource handling GUI components, this type also was reviewed, partly
 * refactored, and polished.
 * 
 * @author John Thomas aka Chilastra.Oarun
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGHarvestingTab extends JPanel {

    /**
     * The table model for active harvesters.
     */
    private ActiveModel activeHarvModel;

    /**
     * A list of active harvesters at the current galaxy. Clients should use
     * {"code activeHarvs()}.
     */
    private List<SWGHarvester> activeHarvs;

    /**
     * This object intercepts and modifies specified cells at the active
     * harvester table.
     */
    private final TableCellEditor activeHarvsCellEditor;

    /**
     * The main GUI table for active harvesters.
     */
    private SWGJTable activeHarvTable;

    /**
     * Flag which denotes if end/depletion dates should be displayed or not.
     */
    private boolean dateView = ((Boolean) SWGFrame.getPrefsKeeper().get(
            "resourceHarvesterEndingView", Boolean.TRUE)).booleanValue();

    /**
     * The dialog for creating and editing harvesters.
     */
    private SWGHarvesterDialog harvDialog;

    /**
     * A list of all harvesters at the current galaxies. Clients should use
     * {@code getHarvesters()}.
     */
    private List<SWGHarvester> harvies;

    /**
     * The table model for all harvesters at the current galaxy.
     */
    private SWGHarvesterModel harvModel;

    /**
     * The minor table for all harvesters at the current galaxy.
     */
    private SWGJTable harvTable;

    /**
     * The URL for the help page.
     */
    private final URL helpPage;

    /**
     * Flag which denotes if hopper column should display numerical values
     * or as a percentage.
     */
    // This field is updated by ^.actionActiveShowPopup if ...
    private boolean hopperNumerical = ((Boolean) SWGFrame.getPrefsKeeper().get(
            "resourceHarvesterHopperView", Boolean.TRUE)).booleanValue();

    /**
     * A helper flag which denotes if the current GUI is created.
     */
    private boolean isGuiCreated;

    /**
     * A helper flag which denotes if there is some background worker that would
     * hinder the invocation. If this flag is {@code true} some methods will
     * silently do nothing.
     */
    private boolean isWorking;

    /**
     * The dialog for creating and editing owners.
     */
    private SWGHarvesterOwnerDialog ownerDialog;

    /**
     * The table model for owners at the current galaxy.
     */
    private SWGHarvesterOwnerModel ownerModel;

    /**
     * A list of owners at the current galaxy. Clients should use {@code
     * getOwners()}.
     */
    private List<SWGHarvesterOwner> owners;

    /**
     * The GUI table for owners at the current galaxy.
     */
    SWGJTable ownerTable;

    /**
     * The text input field to modify the reminder time. The value is an integer
     * for the number of hours for a reminder if an active harvester will stop
     * with a full hopper, or depleted maintenance or power.
     */
    private JTextField reminderTime;

    /**
     * The number of hours for a reminder if an active harvester will stop with
     * a full hopper, or depleted maintenance or power.
     */
    private int reminderTimeLimit;

    /**
     * The tabbed component which contains this type. That is the GUI tab named
     * "Resources", this components parent.
     */
    final SWGResourceTab resourceTab;

    /**
     * A timer for auto-updating the current hopper totals.
     */
    private Timer timer;

    /**
     * Creates an instance of this type. the object for presenting harvesters
     * resources
     * 
     * @param resourceTab the tabbed pane containing this object
     */
    SWGHarvestingTab(final SWGResourceTab resourceTab) {
        this.resourceTab = resourceTab;

        activeHarvsCellEditor = makeTableCellEditor();

        helpPage = SWGAide.class.getResource(
                "docs/help_resources_harvesting_en.html");

        // GUI and timer is created lazily, on demand, see focusGained()

        resourceTab.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });
    }

    /**
     * Called when the user selects to deactivate an active harvester. The
     * boolean argument determines whether the harvester is re-deeded or if it
     * stays put and must retain possible modifiers. Remember that as long the
     * harvester stays put in the world it retains any modifiers which were
     * applied to it when it was put down.
     * 
     * @param harv the selected, active harvester
     * @param reDeed {@code true} if the harvester is re-deeded, {@code false}
     *        if it stays put in the world and should retain its modifiers
     */
    private void actionActiveDeactivate(SWGHarvester harv, boolean reDeed) {
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                activeHarvTable, String.format("Deactivate harvester \"%s\"?",
                        harv.getName()), "Confirm",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE)) {

            if (harv.addToInventory) {
                SWGKnownResource kr = harv.getResource();
                SWGResController.inventoryAddAmount(
                        Math.min(harv.getHopperCapacity(),
                                harv.getHopperUnits()) * harv.getSeveral(),
                        kr,
                        harv.getOwner().getName(),
                        kr.galaxy());
                SWGResourceTab.actionInventoryFilter(
                        kr.rc(), null, kr, false);
            }
            harv.activate(false, reDeed);
            resetActiveHarvesters();
            SWGResController.check();
        }
    }

    /**
     * Called when the user decides to refresh an active harvester. This method
     * resembles that the in-game owner of the harvester empties its hopper and
     * replenish maintenance and power. If the option to add-to-inventory is
     * selected this method updates the inventory.
     * 
     * @param harv the selected harvester
     */
    private void actionActiveRefresh(SWGHarvester harv) {
        if (harv.addToInventory)
            SWGResController.inventoryAddAmount(
                    Math.min(harv.getHopperCapacity(),
                            harv.getHopperUnits()) * harv.getSeveral(),
                    harv.getResource(), harv.getOwner().getName(),
                    harv.getResource().galaxy());

        harv.refreshLastUpdated();
        // resetActiveHarvesters();
        SWGResController.check();
        if (harv.addToInventory)
            SWGResourceTab.actionInventoryFilter(harv.getResource().rc(),
                    null, harv.getResource(), false);
    }
    
    /**
     * Called when the user decides to empty an active harvester. This method
     * resembles that the in-game owner of the harvester empties its hopper only.
     * If the option to add-to-inventory is selected this method updates the inventory.
     * 
     * @param empty the selected harvester
     */
    private void actionActiveEmpty(SWGHarvester harv) {
        if (harv.addToInventory)
            SWGResController.inventoryAddAmount(
                    Math.min(harv.getHopperCapacity(),
                            harv.getHopperUnits()) * harv.getSeveral(),
                    harv.getResource(), harv.getOwner().getName(),
                    harv.getResource().galaxy());

        harv.refreshHopperEmptied();
        SWGResController.check();
        if (harv.addToInventory)
            SWGResourceTab.actionInventoryFilter(harv.getResource().rc(),
                    null, harv.getResource(), false);
    }

    /**
     * Called when the user mouse-clicks the table of active harvesters.
     * Right-clicks invokes {@link #activeTableShowPopup(MouseEvent)}.
     * 
     * @param e the event which triggers the call
     */
    private void actionActiveTableMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3)
            activeTableShowPopup(e);
    }

    /**
     * Called when the user selects to export harvesters and owners defined for
     * the current galaxy. Both harvesters and owners are exported to the same
     * file.
     */
    private void actionExport() {
        File file = new File(String.format("crafting/%s/harvesting.csv",
                SWGResourceTab.galaxy().getName()));
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            SWGAide.printError("SWGHarvestingTab:export",
                    new IOException("Error creating folder" + file.getParent()));
            return;
        }

        String bak = "";
        if (file.exists()) {
            ZWriter.backup(file, null, 3);
            bak = String.format("\nBackup is saved to <SWGAide>\\%s\\%s\\",
                    SWGConstants.backupDir, file.getParent());
        }

        String msg;
        try {
            ZWriter wr = ZWriter.newTextWriterExc(file, false);

            writeHeader(wr);
            for (SWGHarvester h : getHarvesters())
                writeHarvester(wr, h);
            for (SWGHarvesterOwner u : getOwners())
                writeOwner(wr, u);
            wr.close();

            msg = String.format("Harvesters and owners saved to%n%s%s",
                    file.getAbsolutePath(), bak);
        } catch (Throwable e) {
            SWGAide.printError("SWGHarvestingTab:export", e);
            msg = "Error, see log files";
        }
        JOptionPane.showMessageDialog(reminderTime, msg, "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Called when the user selects to delete the selected harvester from
     * SWGAide. This action is irrevocable, thus a confirmation dialog is
     * displayed for the user.
     * 
     * @param row the row for the selected harvester, already converted to model
     */
    private void actionHarvesterDelete(int row) {
        List<SWGHarvester> hs = getHarvesters();
        SWGHarvester h = hs.get(row);

        if (h.isActive()) {
            JOptionPane.showMessageDialog(harvTable, String.format(
                    "Harvester \"%s\" is active!%n"
                            + "First deactivate the harvester", h.getName()),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(harvTable,
                String.format("Delete harvester \"%s\"?", h.getName()),
                "Confirm deletion", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE)) {

            SWGResController.harvestersRemove(h, SWGResourceTab.galaxy());
            resetHarvesters();
        }
    }

    /**
     * Called when the user clicks with the right-hand mouse button at the table
     * of all defined harvesters. This method displays a popup dialog regarding
     * the selected harvester. If no harvester is selected only generic options
     * are enabled.
     * 
     * @param row the row for the selected harvesters, <B>not</B> yet converted
     *        to the model
     * @param e the event which triggers the call
     */
    private void actionHarvShowPopup(int row, MouseEvent e) {
        final int rw = row >= 0
                ? harvTable.convertRowIndexToModel(row)
                : row;

        JPopupMenu popup = new JPopupMenu();

        JMenuItem neww = new JMenuItem("Create harvester");
        neww.setToolTipText("Create a new harvester for this galaxy");
        neww.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                harvesterCreateDialog().showAndBegin(null, false);
            }
        });
        popup.add(neww);

        JMenuItem edit = new JMenuItem("Edit harvester");
        edit.setToolTipText("Edit the description of the selected harvester");
        edit.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                SWGHarvester hr = getHarvesters().get(rw);
                harvesterCreateDialog().showAndBegin(hr, false);
            }
        });
        edit.setEnabled(rw >= 0);
        popup.add(edit);

        JMenuItem clone = new JMenuItem("Clone harvester");
        clone.setToolTipText("Create a clone of the selected harvester, " +
                "define a unique description");
        clone.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                SWGHarvester hr = getHarvesters().get(rw);
                harvesterCreateDialog().showAndBegin(hr, true);
            }
        });
        clone.setEnabled(rw >= 0);
        popup.add(clone);

        popup.addSeparator();

        JMenuItem exp = makeExportMenuItem();
        popup.add(exp);

        JMenuItem imp = makeImportMenuItem();
        popup.add(imp);

        popup.addSeparator();

        JMenuItem del = new JMenuItem("Delete harvester");
        del.setToolTipText("Permanently delete the selected harvester");
        del.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                actionHarvesterDelete(rw);
            }
        });
        del.setEnabled(rw >= 0);
        popup.add(del);

        popup.show(harvTable, e.getX(), e.getY());
    }

    /**
     * Called when the user selects to import harvesters and owners from file
     * for the current galaxy.
     */
    private void actionImport() {
        JFileChooser fc = SWGFrame.getFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fc.showOpenDialog(reminderTime) == JFileChooser.APPROVE_OPTION)
            importFile(fc.getSelectedFile());
    }

    /**
     * Called when the user selects to delete the selected owner from SWGAide.
     * This action is irrevocable, thus a confirmation dialog is displayed for
     * the user.
     * 
     * @param row the row for the selected owner, already converted to model
     */
    private void actionOwnerDelete(int row) {
        SWGHarvesterOwner u = getOwners().get(row);

        if (u != null
                && JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                        ownerTable, String.format("Delete owner \"%s\"?",
                                u.getName()),
                        "Confirm deletion", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE)) {

            SWGResController.harvesterOwnerRemove(u, SWGResourceTab.galaxy());
            resetOwners();
        }
    }

    /**
     * Called when the user clicks the GUI table of owners with the right-hand
     * mouse button. This method displays a popup dialog for handling the
     * selected owner. If no owner is selected only the generic options are
     * enabled.
     * 
     * @param row the row for the selected owner, <B>not</B> yet converted to
     *        the model, or -1 if the call is generic
     * @param e the event which triggers the call
     */
    private void actionOwnerShowPopup(int row, MouseEvent e) {
        final int rw = row >= 0
                ? ownerTable.convertRowIndexToModel(row)
                : row;

        JPopupMenu popup = new JPopupMenu();

        JMenuItem neww = new JMenuItem("Create owner");
        neww.setToolTipText("Create a new harvester owner at this galaxy");
        neww.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                ownerCreateDialog().showAndBegin(null);
            }
        });
        popup.add(neww);

        JMenuItem edit = new JMenuItem("Edit owner");
        edit.setToolTipText("Edit the selected owner");
        edit.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                SWGHarvesterOwner hr = getOwners().get(rw);
                ownerCreateDialog().showAndBegin(hr);
            }
        });
        edit.setEnabled(rw >= 0);
        popup.add(edit);

        popup.addSeparator();

        JMenuItem exp = makeExportMenuItem();
        popup.add(exp);

        JMenuItem imp = makeImportMenuItem();
        popup.add(imp);

        popup.addSeparator();

        JMenuItem del = new JMenuItem("Delete owner");
        del.setToolTipText("Permanently delete the selected owner");
        del.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                actionOwnerDelete(rw);
            }
        });
        del.setEnabled(rw >= 0);
        popup.add(del);

        popup.show(ownerTable, e.getX(), e.getY());
    }

    /**
     * Called when the user modifies any one of the preferences regarding
     * hours-until-warning.
     */
    private void actionReminderLimitModified() {
        String s = reminderTime.getText();
        Integer rtl = s == null || s.trim().isEmpty()
                ? Integer.valueOf(24)
                : Integer.valueOf(s);
        reminderTimeLimit = rtl.intValue();

        SWGFrame.getPrefsKeeper().add("resourceHarvesterReminder", rtl);
        SWGResController.check();
    }

    /**
     * Helper method to {@code activeHarvsCellEditor}. This method supports
     * editing of cells at the main table of active harvesters. This method
     * intercepts the value displayed at the cell, which may have been
     * concatenated with further information, and replaces it with the plain
     * value from the selected harvester. The user will rather see the plain
     * value at the editor component.
     * 
     * @param value the value to intercept and replace
     * @param component the component used as cell editor
     * @param row the row at the main table, not converted to model
     * @param col the column at the main table
     * @return a cell editor with the plain value to edit
     */
    private Component activeCellEditor(Object value, Component component,
            int row, int col) {

        final int rw = row >= 0
                ? activeHarvTable.convertRowIndexToModel(row)
                : row;

        SWGHarvester active = activeHarvs().get(rw);

        String newValue;
        switch (col) {
        case 4: // maintenance
            newValue = ZNumber.asText(active.getMaint(), true, true);
            break;
        case 5: // power
            newValue = ZNumber.asText(active.getPower(), true, true);
            break;
        default:
            newValue = (String) value;
        }

        ((JTextField) component).setText(newValue);
        return component;
    }

    /**
     * Helper method which returns a list of active harvesters for the current
     * galaxy.
     * 
     * @return a list of active harvesters for the current galaxy
     */
    private List<SWGHarvester> activeHarvs() {
        if (activeHarvs == null)
            activeHarvs = SWGResController.harvestersActive(
                    SWGResourceTab.galaxy());

        return activeHarvs;
    }

    /**
     * Helper method which displays a popup dialog. The popup dialog has options
     * for a selected harvester and generic options. If the user clicked the
     * table bottom outside the harvesters all harvester specific options are
     * disabled.
     * 
     * @param e the event which triggers the call
     */
    private void activeTableShowPopup(MouseEvent e) {
        int row = activeHarvTable.rowAtPoint(e.getPoint());
        final int rw = row >= 0
                    ? activeHarvTable.convertRowIndexToModel(row)
                    : row;

        final SWGHarvester hr = rw >= 0
                    ? activeHarvs().get(rw)
                    : null;

        if (row >= 0)
            activeHarvTable.getSelectionModel().setSelectionInterval(row, row);

        JPopupMenu popup = new JPopupMenu();

        JMenuItem update = new JMenuItem("Refresh harvester (complete)");
        update.setToolTipText(
                "Refresh: empty hopper and replenish maintenance and power");
        update.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e1) {
                actionActiveRefresh(hr);
            }
        });
        update.setEnabled(hr != null);
        popup.add(update);
        
        JMenuItem empty = new JMenuItem("Empty Hopper only");
        empty.setToolTipText(
                "Empty: empty hopper only, leaving maint. and power alone");
        empty.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e1) {
                actionActiveEmpty(hr);
            }
        });
        empty.setEnabled(hr != null);
        popup.add(empty);

        JMenuItem edit = new JMenuItem("Edit active harvester");
        edit.setToolTipText("Edit the selected, active harvester");
        edit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                resourceTab.harvesterActivateDialog().showAndBegin(
                            hr, hr.getResource(), null);
            }
        });
        edit.setEnabled(hr != null);
        popup.add(edit);

        popup.addSeparator();

        SWGKnownResource kr = hr != null
                    ? hr.getResource()
                    : null;
        SWGResourceClass rc = kr != null
                    ? kr.rc()
                    : null;

        popup.add(SWGResourceTab.inventoryFilterMenu(rc, null, kr, true, true));
        popup.add(SWGResController.currentSelectMenu(kr));
        SWGFrame fr = SWGAide.frame();
        SWGSchematicTab schemTab = SWGFrame.getSchematicTab(fr);
        SWGSchemController sc = new SWGSchemController(schemTab);
        popup.add(sc.resClassUse(rc));

        popup.addSeparator();

        JMenuItem newh = new JMenuItem("Create harvester");
        newh.setToolTipText("Create a new harvester at this galaxy");
        newh.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e1) {
                harvesterCreateDialog().showAndBegin(null, false);
            }
        });
        popup.add(newh);

        JMenuItem newu = new JMenuItem("Create owner");
        newu.setToolTipText("Create a new harvester owner at this galaxy");
        newu.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e1) {
                ownerCreateDialog().showAndBegin(null);
            }
        });
        popup.add(newu);

        popup.addSeparator();

        final JCheckBoxMenuItem hopper =
                    new JCheckBoxMenuItem("Numerical hopper totals");
        hopper.setToolTipText("Toggle number of units or percent hopper fill");
        hopper.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                    "resourceHarvesterHopperView", Boolean.TRUE)).booleanValue());
        hopper.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e1) {
                SWGFrame.getPrefsKeeper().add("resourceHarvesterHopperView",
                        Boolean.valueOf(hopper.isSelected()));
                hopperNumerical = hopper.isSelected();
                resetActiveHarvesters();
            }
        });
        popup.add(hopper);

        final JCheckBoxMenuItem dates =
                    new JCheckBoxMenuItem("Display end dates");
        dates.setToolTipText("Toggle display end date or not");
        dates.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                    "resourceHarvesterEndingView", Boolean.TRUE)).booleanValue());
        dates.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e1) {
                SWGFrame.getPrefsKeeper().add("resourceHarvesterEndingView",
                        Boolean.valueOf(dates.isSelected()));
                dateView = dates.isSelected();
                resetActiveHarvesters();
            }
        });
        popup.add(dates);

        popup.addSeparator();

        popup.add(makeExportMenuItem());
        popup.add(makeImportMenuItem());

        popup.addSeparator();

        // send depleted message to SWGCraft.org
        JMenuItem sendDepl = new JMenuItem("Mark depleted");
        sendDepl.setToolTipText(
                "Send message to swgaide.com that this resource is depleted");
        sendDepl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                resourceTab.actionSendDepleted(hr.getResource());
            }
        });
        sendDepl.setEnabled(hr != null);
        popup.add(sendDepl);

        popup.addSeparator();

        JMenuItem staysPut = new JMenuItem("Deactivate harvester (stays put)");
        staysPut.setToolTipText(
                "Deactivate harvester, it stays put (retains modifiers)");
        staysPut.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e1) {
                actionActiveDeactivate(hr, false);
            }
        });
        popup.add(staysPut);
        staysPut.setEnabled(hr != null);

        JMenuItem redeed = new JMenuItem("Deactivate harvester (re-deed)");
        staysPut.setToolTipText(
                "Deactivate harvester, it is pulled (revokes owner modifiers)");
        redeed.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e1) {
                actionActiveDeactivate(hr, true);
            }
        });
        redeed.setEnabled(hr != null);
        popup.add(redeed);

        popup.show(activeHarvTable, e.getX(), e.getY());
    }

    /**
     * This method is called for two different action events, in both cases it
     * is determined if this component has focus and which action to select:
     * <UL>
     * <LI>if focus is changed for{@code resourceTab} the argument is {@code
     * true} or {@code false}; this is when the user selects the tab named
     * "Resources", or any one of its siblings</LI>
     * <LI>if focus is changed for this component the argument is always {@code
     * true}; this is when the user selects the tab for {@code this} component,
     * or any of its siblings</LI>
     * </UL>
     * <P>
     * If this component gains focus this implementation updates the GUI.
     * 
     * @param focus {@code true} if there is any focus change for this component
     *        or its siblings, or if {@code resourceTab} gained focus; {@code
     *        false} otherwise
     */
    void focusGained(boolean focus) {
        if (focus && resourceTab.getSelectedComponent() == this) {
            if (isWorking)
                return;
            if (!isGuiCreated)
                makeInterior();

            resetTables();
            SWGHelp.push(helpPage);
            SWGAide.frame().putToLogbar_2(null);
        } else {
            SWGHelp.remove(helpPage);
            activeHarvs = Collections.emptyList();
            harvies = Collections.emptyList();
            owners = Collections.emptyList();
        }
    }

    /**
     * Convenience method which returns a list of all harvesters for the current
     * galaxy, or an empty list. This is the list which is stored by SWGAide,
     * use access methods in {@code SWGResController} to modify its content.
     * 
     * @return a list of all harvesters, or an empty list
     */
    private List<SWGHarvester> getHarvesters() {
        if (harvies == null || harvies == Collections.EMPTY_LIST)
            harvies = SWGResController.harvesters(SWGResourceTab.galaxy());

        return harvies;
    }

    /**
     * Returns a harvester owner with the specified name, or {@code null}. This
     * method should only be invoked for events that pertains to the current
     * display at the harvester panel. If no such owner exists this method
     * returns {@code null}.
     * 
     * @param name an owner name
     * @return a harvester owner, or {@code null}
     */
    SWGHarvesterOwner getOwner(String name) {
        for (SWGHarvesterOwner o : getOwners())
            if (o.getName().equals(name)) return o;

        return null;
    }

    /**
     * Returns a list of owners for the current galaxy, or an empty list. This
     * is the list which is stored by SWGAide and thus it must not be modified
     * rashly. This method should only be invoked for actions that pertains to
     * the current view of harvester owners, not
     * 
     * @return a list of owners, or an empty list
     */
    List<SWGHarvesterOwner> getOwners() {
        if (owners == null || owners == Collections.EMPTY_LIST)
            owners = SWGResController.harvesterOwners(SWGResourceTab.galaxy());

        return owners;
    }

    /**
     * Returns the GUI dialog for creating/editing harvesters.
     * 
     * @return the GUI dialog for creating/editing harvesters
     */
    private SWGHarvesterDialog harvesterCreateDialog() {
        if (harvDialog == null)
            harvDialog = new SWGHarvesterDialog(this);

        return harvDialog;
    }

    /**
     * Helper method which imports harvesters and guards from file and adds them
     * for the current galaxy. If an existing harvester or owner equals an item
     * in the file it is silently discarded.
     * 
     * @param file the file to read from
     */
    private void importFile(File file) {
        if (!file.exists()) {
            JOptionPane.showMessageDialog(reminderTime, "No such file:\n"
                    + file.getAbsolutePath(), "File error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        ZReader sr = ZReader.newTextReader(file);
        if (sr == null) return;

        boolean error = false;
        for (String line : sr.lines(true, true))
            error |= !importLine(line);

        if (error)
            JOptionPane.showMessageDialog(reminderTime, String.format(
                    "There is one or more errors importing file:%n"
                            + "%s%nSee log files", file.getAbsolutePath()),
                    "Error", JOptionPane.ERROR_MESSAGE);
        resetHarvesters();
        resetOwners();
        sr.close();
    }

    /**
     * Helper method which parses the specified line and adds the harvester to
     * the lists for the current galaxy.
     * 
     * @param split the line to parse, split on tokens
     * @return {@code true} if successful
     */
    private boolean importHarvester(String[] split) {
        // harvester,name,type,BER,hopper-size
        split[1] = split[1].replace('\u00b8', ',');
        if (SWGResController.harvestersExists(
                split[1], SWGResourceTab.galaxy()))
            return true; // discard silently
        
        double m = 1.0;
        
        if (split.length > 5) {
        	m = Double.parseDouble(split[5]);
        }

        try {
            SWGHarvester h = new SWGHarvester(
                    split[1],
                    split[2],
                    ZNumber.intExc(split[3]),
                    ZNumber.intExc(split[4]),
                    m);
            SWGResController.harvestersAdd(h, SWGResourceTab.galaxy());
            return true;
        } catch (Exception e) {
            SWGAide.printDebug("harv", 1,
                    String.format(
                            "SWGHarvesterTab:importHarvester: error: %s%n%s",
                            e.getMessage(), split));
        }
        return false;
    }

    /**
     * Helper method which determines if the specified line is for a harvester
     * or a owner and invokes the appropriate import helper method.
     * 
     * @param line the line to parse
     * @return {@code true} if successful
     */
    private boolean importLine(String line) {
        // harvester,name,type,BER,hopper-size
        // owner,name,hopper,maint,energy,tech,buff
        String l = line.replace("\"", "");
        String[] split = l.split(",");
        for (int i = 0; i < split.length; ++i)
            split[i] = split[i].trim();

        if (l.startsWith("harv"))
            return importHarvester(split);
        if (l.startsWith("owner"))
            return importOwner(split);

        SWGAide.printDebug("harv", 1,
                "SWGHarvesterTab:importLine: line does not begin with " +
                        "\"harvester\" or \"owner\"\n\t" + line);
        return false;
    }

    /**
     * Helper method which parses the specified line and adds the owner to the
     * lists for the current galaxy.
     * 
     * @param split the line to parse, split on tokens
     * @return {@code true} if successful
     */
    private boolean importOwner(String[] split) {
        // owner,name,hopper,maint,energy,tech,buff
        split[1] = importOwnerNormalizeName(split[1]);
        if (SWGResController.harvesterOwnerExists(split[1],
                SWGResourceTab.galaxy()))
            return true; // discard silently

        try {
            SWGHarvesterOwner u = new SWGHarvesterOwner(
                    split[1],
                    ZNumber.intExc(split[2]),
                    ZNumber.intExc(split[3]),
                    ZNumber.intExc(split[4]),
                    ZNumber.intExc(split[5]),
                    ZNumber.intExc(split[6]));
            SWGResController.harvesterOwnerAdd(u, SWGResourceTab.galaxy());
            return true;
        } catch (Exception e) {
            SWGAide.printDebug("harv", 1,
                    String.format("SWGHarvesterTab:importOwner: error: %s%n%s",
                            e.getMessage(), split));
        }
        return false;
    }

    /**
     * Helper method which normalizes the specified name and returns a name.
     * 
     * @param name the name to normalize
     * @return the normalized name
     */
    private String importOwnerNormalizeName(String name) {
        String n = name.replace('\u00b8', ',');
        int i = n.indexOf(' ');
        if (i > 0)
            n = n.substring(0, i); // just first name
        return n;
    }

    /**
     * Creates and returns the GUI component for the table of active harvester.
     * This is the main table displaying active harvesters for the current
     * galaxy.
     * 
     * @return a GUI component
     */
    
    private Component makeActiveHarvesterPanel() {
        activeHarvModel = new ActiveModel();
        activeHarvTable = new SWGJTable(activeHarvModel) {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override
                    public String getToolTipText(MouseEvent e) {
                        int i = columnModel.getColumnIndexAtX(e.getPoint().x);
                        return activeHarvModel.columnToolTips[i];
                    }
                };
            }
        };

        int rHeight = activeHarvTable.getRowHeight();
        rHeight = SWGGuiUtils.getRowHeight(activeHarvTable);
        activeHarvTable.setRowHeight((rHeight * 3) + 3);
        TableCellRenderer tc;
        tc = new SWGDecoratedTableCellRenderer(activeHarvModel) {
            @Override
            protected void myAlignment(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column,
                    TableCellDecorations decor) {

                if (value == null) return;
                setHorizontalAlignment(column < 11
                        ? SwingConstants.CENTER
                        : SwingConstants.LEADING);
                setVerticalAlignment(column < 10 && column != 6
                        ? SwingConstants.CENTER
                        : SwingConstants.TOP);
            }

            @Override
            protected Object myValue(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                SWGHarvester harv = activeHarvs().get(row);
                ZString zs = new ZString();
                if (value == null || column == 0)
                    return value; // toon-name

                else if (column == 1) {
                    long la = harv.getLastUpdated();
                    if (la <= 0) return "ERROR";

                    zs = ZString.fz("<center>%s<br/>%s</center>",
                            ZStuff.dateString(la), ZStuff.timeString(la));

                } else if (column == 2) {
                    zs = ZString.fz("<center>%s</center>", harv.getName());
                } else if (column == 3) {
                    zs = ZString.fz("<center>%s</center>", harv.type);
                } else if (column == 4) { // maintenance
                    float fr = ((Float) value).floatValue() * 100f;
                    String r = ZNumber.asText(fr, 1, 0);
                    String m = ZNumber.asText(harv.getMaint(), true, true);
                    String e = dateView
                            ? ZStuff.dateTimeString(harv.getMaintEnds())
                            : "";
                    zs = ZString.fz("<center>%s %% of %s<br/>%s</center>",
                            r, m, e);
                } else if (column == 5) { // power
                    if (harv.isSelfPowered())
                        zs.app("<center>Self<br/>Powered</center>");
                    else {
                        float fr = ((Float) value).floatValue() * 100f;
                        String r = ZNumber.asText(fr, 1, 0);
                        String p = ZNumber.asText(harv.getPower(), true, true);
                        String e = dateView
                                ? ZStuff.dateTimeString(harv.getPowerEnds())
                                : "";
                        zs = ZString.fz("<center>%s %% of %s<br/>%s</center>",
                                r, p, e);
                    }
                } else if (column == 6) { // the resource
                    SWGKnownResource kr = (SWGKnownResource) value;
                    zs = ZString.fz("<center>%s<br/>%s</center>",
                            kr.getName(), kr.rc().rcName());

                } else if (column == 7) { // concentration
                    return value;
                } else if (column == 8) { // adjusted BER
                    return ZNumber.asText(((Double) value).doubleValue(), 1, 1);
                } else if (column == 9) { // AER
                    return ZNumber.asText(((Double) value).doubleValue(), 1, 1);
                } else if (column == 10) { // Hopper
                    String dv = dateView
                            ? ZStuff.dateTimeString(harv.getHopperFull())
                            : "";
                    if (hopperNumerical) {
                        int c = harv.getHopperCapacity();
                        int u = harv.getHopperUnits();
                        u = Math.min(c, u);
                        String hu = ZNumber.asText(u, true, true);
                        String hc = ZNumber.asText(c >= 1000
                                ? c / 1000
                                : c, true, true);
                        zs = ZString.fz("<center>%s (%s%s)<br/>%s</center>",
                                hu, hc, c >= 1000
                                        ? "k"
                                        : "", dv);
                    } else
                        zs = ZString.fz("<center>%1.1f %%<br/>%s</center>",
                                value, dv);

                } else if (column == 11)
                        return value;

                return zs.pre("<html>", false).appnl("</html>").toString();
            }
        };
        activeHarvTable.setDefaultRenderer(String.class, tc);
        activeHarvTable.setDefaultRenderer(Number.class, tc);
        activeHarvTable.setDefaultRenderer(SWGResource.class, tc);

        int w = SWGGuiUtils.fontWidth(activeHarvTable, "A ssigne e", activeHarvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(activeHarvTable, 0, 1, w, 130);
        w = SWGGuiUtils.fontWidth(activeHarvTable, "D escriptio n", activeHarvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(activeHarvTable, 2, 3, w, 5);
        w = SWGGuiUtils.fontWidth(activeHarvTable, "0123456789012345", activeHarvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(activeHarvTable, 4, 6, w, 90);
        w = SWGGuiUtils.fontWidth(activeHarvTable, "100", activeHarvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(activeHarvTable, 7, 7, w, 5);
        w = SWGGuiUtils.fontWidth(activeHarvTable, "UBER", activeHarvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(activeHarvTable, 8, 9, w, 5);
        w = SWGGuiUtils.fontWidth(activeHarvTable, "0123456789012345", activeHarvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(activeHarvTable, 10, 10, w, 90);
        SWGGuiUtils.tableColumnSetWidth(activeHarvTable, 11, 200, 300, 4000);

        activeHarvTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeHarvTable.setAutoCreateRowSorter(true);
        activeHarvTable.getTableHeader().setReorderingAllowed(false);
        activeHarvTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionActiveTableMouse(e);
            }
        });

        // setting columns 4 and 5 (maintenance and power)
        // to use table cell editor, a custom editor
        TableColumn col = activeHarvTable.getColumnModel().getColumn(4);
        col.setCellEditor(activeHarvsCellEditor);
        col = activeHarvTable.getColumnModel().getColumn(5);
        col.setCellEditor(activeHarvsCellEditor);

        JScrollPane jsp = new JScrollPane(activeHarvTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionActiveTableMouse(e);
            }
        });

        Box box = Box.createHorizontalBox();
        box.setPreferredSize(new Dimension(1000, 200));
        box.add(jsp);
        return box;
    }

    /**
     * Creates and returns a menu item for exporting harvesters and owners to
     * file. If there is no harvester and no owners defined the returned item is
     * disabled. The action listener for the returned item invokes {@code
     * actionExport()}.
     * 
     * @return a menu item for exporting harvesters and owners
     */
    private JMenuItem makeExportMenuItem() {
        JMenuItem exp = new JMenuItem("Export");
        exp.setToolTipText("Export harvesters and owners to file");
        exp.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                actionExport();
            }
        });
        exp.setEnabled(getOwners().size() > 0 || getHarvesters().size() > 0);
        return exp;
    }

    /**
     * Creates and returns the GUI component for the table of defined
     * harvesters. This is the table displaying all defined harvesters for the
     * current galaxy.
     * 
     * @return a GUI component
     */
    
    private Component makeHarvesterPanel() {
        harvModel = new SWGHarvesterModel();
        harvTable = new SWGJTable(harvModel);

        TableCellRenderer tc = new SWGDecoratedTableCellRenderer() {
            @Override
            protected Object myValue(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row,
                    int column) {

                SWGHarvester harv = getHarvesters().get(row);
                String s = column > 1
                        ? ZNumber.asText((Number) value)
                        : (String) value;

                if (harv.isActive())
                    return String.format("<html><i>%s</i></html>", s);

                if (column == 0 && harv.getOwner() == null)
                    return String.format("%s (deed)", s);

                return s;
            }
        };

        harvTable.setDefaultRenderer(String.class, tc);
        harvTable.setDefaultRenderer(Number.class, tc);
        SWGGuiUtils.setRowHeight(harvTable);

        int w = SWGGuiUtils.fontWidth(harvTable, "BER", harvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(harvTable, 2, 2, w, 10);
        w = SWGGuiUtils.fontWidth(harvTable, "999,999", harvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(harvTable, 3, 3, w, 10);
        w = SWGGuiUtils.fontWidth(harvTable, "99.00", harvTable.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(harvTable, 4, 4, w, 10);

        harvTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        harvTable.setAutoCreateRowSorter(true);
        harvTable.getTableHeader().setReorderingAllowed(false);
        harvTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int row = harvTable.rowAtPoint(e.getPoint());
                    if (row >= 0) actionHarvShowPopup(row, e);
                }
            }
        });

        JScrollPane jsp = new JScrollPane(harvTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3)
                    actionHarvShowPopup(-1, e);
            }
        });

        jsp.setPreferredSize(new Dimension(400, 150));
        jsp.setMinimumSize(new Dimension(400, 130));
        return jsp;
    }

    /**
     * Creates and returns a menu item for importing harvesters and owners from
     * file. The action listener for the returned item invokes {@code
     * actionImport()}.
     * 
     * @return a menu item for importing harvesters and owners
     */
    private JMenuItem makeImportMenuItem() {
        JMenuItem imp = new JMenuItem("Import file...");
        imp.setToolTipText("Import harvesters and owners from file");
        imp.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e1) {
                actionImport();
            }
        });
        return imp;
    }

    /**
     * Helper method which creates the interior of this GUI element. This method
     * is called lazily when the user selects the tab that contains this
     * instance.
     */
    private void makeInterior() {
        isWorking = true;
        setLayout(new BorderLayout());
        add(makeTopPanel(), BorderLayout.PAGE_START);
        add(makeActiveHarvesterPanel(), BorderLayout.CENTER);

        // close on "Alt-C"
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK), "harvAltC");
        this.getActionMap().put("harvAltC", new AbstractAction() {
            
            public void actionPerformed(ActionEvent actionEvent) {
                activeHarvTable.clearSelection();
                harvTable.clearSelection();
                ownerTable.clearSelection();
                activeHarvTable.getRowSorter().setSortKeys(null);
                harvTable.getRowSorter().setSortKeys(null);
                ownerTable.getRowSorter().setSortKeys(null);
            }
        });

        makeTimer();

        isGuiCreated = true;
        isWorking = false;
    }

    /**
     * Creates and returns the GUI component for the table of defined owners.
     * This is the component which displays all defined owners for the current
     * galaxy.
     * 
     * @return a GUI component
     */
    private Component makeOwnersPanel() {
        ownerModel = new SWGHarvesterOwnerModel();
        ownerTable = new SWGJTable(ownerModel) {

            // Implement table header tool tips.
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    
                    @Override
                    public String getToolTipText(MouseEvent e) {
                        int i = columnModel.getColumnIndexAtX(e.getPoint().x);
                        return ownerModel.columnToolTips[i];
                    }
                };
            }
        };

        for (int i = 1; i < ownerModel.getColumnCount(); i++) {
        	int w = SWGGuiUtils.fontWidth(ownerTable, "Ag e", ownerTable.getFont()) + 5;
            SWGGuiUtils.tableSetColumnWidths(ownerTable, 1, 99, w, 0);
        }

        SWGGuiUtils.setRowHeight(ownerTable);
        ownerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ownerTable.setAutoCreateRowSorter(true);
        ownerTable.getTableHeader().setReorderingAllowed(false);
        ownerTable.addMouseListener(new MouseAdapter() {

            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int row = ownerTable.rowAtPoint(e.getPoint());
                    if (row >= 0)
                        actionOwnerShowPopup(row, e);
                }
            }
        });

        JScrollPane jsp =
                new JScrollPane(ownerTable,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.addMouseListener(new MouseAdapter() {

            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3)
                    actionOwnerShowPopup(-1, e);
            }
        });

        jsp.setPreferredSize(new Dimension(325, 150));
        jsp.setMinimumSize(new Dimension(325, 130));
        return jsp;
    }

    /**
     * Creates and returns the GUI component related to maintenance warnings.
     * This is the component on which to specify when a warning about drained
     * maintenance is displayed, in hours.
     * 
     * @param tt the tooltip text for this element
     * @return a GUI component
     */
    private Component makeReminderPanel(String tt) {
        reminderTime = new JTextField("", 3);
        reminderTime.setPreferredSize(new Dimension(50, 20));
        reminderTime.setMaximumSize(new Dimension(50, 20));
        reminderTime.setHorizontalAlignment(SwingConstants.RIGHT);

        Integer rtl = (Integer) SWGFrame.getPrefsKeeper().get(
                "resourceHarvesterReminder", Integer.valueOf(24));
        reminderTimeLimit = rtl.intValue();
        reminderTime.setText(rtl.toString());

        reminderTime.setToolTipText(tt);
        ((AbstractDocument) reminderTime.getDocument())
                .setDocumentFilter(new SWGDocNumberFilter()); // keep it
        // integers
        reminderTime.getDocument().addDocumentListener(new DocumentListener() {

            
            public void changedUpdate(DocumentEvent e) {
                actionReminderLimitModified();
            }

            
            public void insertUpdate(DocumentEvent e) {
                actionReminderLimitModified();
            }

            
            public void removeUpdate(DocumentEvent e) {
                actionReminderLimitModified();
            }
        });

        return reminderTime;
    }

    /**
     * Creates and returns a table cell editor which intercepts and modifies
     * actions at select table columns at the table of active harvesters.
     * 
     * @return a table cell editor
     */
    private TableCellEditor makeTableCellEditor() {
        return new SWGTableCellEditor(SwingConstants.TRAILING) {
            @Override
            public Object getCellEditorValue() {
                SWGResController.check();
                return component.getText();
            }

            
            @Override
            public Component getTableCellEditorComponent(JTable table,
                    Object value, boolean isSelected, int row, int col) {
                return activeCellEditor(value, component, row, col);
            }
        };
    }

    /**
     * Creates and returns a timer which updates the main table of active
     * harvesters. It is the columns Status, Maintenance, Power, and Hopper
     * which are refreshed every second. If this panel is not visible this timer
     * performs no work.
     */
    private void makeTimer() {
        timer = new Timer(1000, new ActionListener() {

            
            public void actionPerformed(ActionEvent arg0) {
                // ignore this if we don't have active harvesters
                if (!isGuiCreated || !isShowing() || isWorking)
                    return;

                int size = activeHarvs().size();
                if (size <= 0)
                    return; // nothing to do

                // status, maint, power, hopper
                for (int i = 0; i < size; i++) {
                    activeHarvModel.fireTableCellUpdated(i, 4);
                    activeHarvModel.fireTableCellUpdated(i, 5);
                    activeHarvModel.fireTableCellUpdated(i, 6);
                    activeHarvModel.fireTableCellUpdated(i, 11);
                }
            }
        });

        timer.start();
    }

    /**
     * Creates and returns the GUI component for the upper section of this
     * panel. The component contains the table for defined harvesters, the panel
     * for warning options, and the table for defined owners.
     * 
     * @return a GUI component
     */
    private Component makeTopPanel() {
    	Box box = new Box(BoxLayout.LINE_AXIS) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int h = d.height / 5;
                float m = SWGGuiUtils.fontMultiplier();
                h = Math.round(h * m);
                d.height = h > 200
                        ? h
                        : 200;
                return d;
            }
        };

        //box.setPreferredSize(new Dimension(1000, h));
        box.add(makeHarvesterPanel());
        box.add(makeWarningOptionsPanel());
        box.add(makeOwnersPanel());
        return box;
    }

    /**
     * Creates and returns the GUI component of options for warnings. This
     * component displays input fields for warnings related to when maintenance
     * and power are depleted, and when the hopper is full.
     * 
     * @return a GUI component
     */
    private Component makeWarningOptionsPanel() {
        JPanel options = new JPanel();
        options.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Harvester Warnings"));

        options.setLayout(new SpringLayout());

        String wt =
                "Emit warnings when a harvester will stop within specified time";
        JLabel wl = new JLabel("Reminder (hours):");
        wl.setToolTipText(wt);
        options.add(wl);
        options.add(makeReminderPanel(wt));

        SpringUtilities.makeCompactGrid(options, 1, 2, 0, 10, 5, 10);

        Box hb = Box.createVerticalBox();
        hb.add(options);
        hb.add(Box.createVerticalGlue());

        return hb;
    }

    /**
     * Returns the GUI dialog for creating/editing owners.
     * 
     * @return the GUI dialog for creating/editing owners
     */
    private SWGHarvesterOwnerDialog ownerCreateDialog() {
        if (ownerDialog == null)
            ownerDialog = new SWGHarvesterOwnerDialog(this);

        return ownerDialog;
    }

    /**
     * Resets the list of active harvesters and refreshes the GUI. Changes to
     * the list of active harvesters often affects the list of defined
     * harvesters so that GUI table is also refreshed, without resetting {@code
     * harvies}.
     */
    void resetActiveHarvesters() {
        if (isGuiCreated) {
            activeHarvs = null;
            activeHarvModel.fireTableDataChanged();
            harvModel.fireTableDataChanged();
        }
    }

    /**
     * Resets the list of defined harvesters and refreshes the GUI table.
     */
    void resetHarvesters() {
        harvies = null;
        harvModel.fireTableDataChanged();
    }

    /**
     * Resets the list of defined owners and refreshes the GUI table.
     */
    void resetOwners() {
        owners = null;
        ownerModel.fireTableDataChanged();
    }

    /**
     * Convenience method which dispatches to {@code resetOwners()}, {@code
     * resetHarvesters()} and {@code resetActiveHarvesters()}.
     */
    private void resetTables() {
        resetOwners();
        resetHarvesters();
        resetActiveHarvesters();
    }

    /**
     * Helper method which writes the specified harvester to file.
     * 
     * @param wr the writer to write to
     * @param h the harvester to write
     * @throws Exception if there is an error
     */
    private void writeHarvester(ZWriter wr, SWGHarvester h) throws Exception {
        // # harvester,name,type,BER,hopper-size
        wr.writeExc("harvester,");
        String n = h.getName().replace(',', '\u00b8');
        wr.writeExc(n);
        wr.writeExc(",");
        wr.writeExc(h.type);
        wr.writeExc(",");
        wr.writeExc(Integer.toString(h.ber));
        wr.writeExc(",");
        wr.writeExc(Integer.toString(h.hopperSize));
        wr.writeExc(",");
        wr.writelnExc(Double.toString(h.bmod));
    }

    /**
     * Helper method which writes the header for the export file.
     * 
     * @param wr the writer to write to
     * @throws Exception if there is an error
     */
    private void writeHeader(ZWriter wr) throws Exception {
        wr.writeExc("# Exported harvesters and owners for ");
        wr.writeExc(SWGResourceTab.galaxy().getName());
        wr.writeExc(", ");
        wr.writeExc(ZStuff.dateTimeString());
        wr.writeExc(", by SWGAide: ");
        wr.writelnExc(SWGConstants.swgAideURL);

        wr.writeExc("# harvester,name,type,BER,hopper-size,modifier");
        wr.writelnExc("");
        wr.writelnExc("# owner,name,hopper,maint,energy,tech,buff");
        wr.eol();
    }

    /**
     * Helper method which writes the specified harvester owner to file.
     * 
     * @param wr the writer to write to
     * @param o the owner to write
     * @throws Exception if there is an error
     */
    private void writeOwner(ZWriter wr, SWGHarvesterOwner o) throws Exception {
        // # owner,name,hopper,maint,energy,tech,buff
        wr.writeExc("owner,");
        String n = o.getName().replace(',', '\u00b8');
        wr.writeExc(n);
        wr.writeExc(",");
        wr.writeExc(Integer.toString(o.getStorageEfficiency()));
        wr.writeExc(",");
        wr.writeExc(Integer.toString(o.getMaintEfficiency()));
        wr.writeExc(",");
        wr.writeExc(Integer.toString(o.getEnergyEfficiency()));
        wr.writeExc(",");
        wr.writeExc(Integer.toString(o.getHarvestingTechnology()));
        wr.writeExc(",");
        wr.writelnExc(Integer.toString(o.getHarvestFair()));
    }

    /**
     * The table model for the table of active harvesters.
     * 
     * @author John Thomas aka Chilastra.Oarun
     * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class ActiveModel extends AbstractTableModel
            implements DecoratedTableModel {

        /**
         * Table column header titles.
         */
        private final String[] colNames =
            { "Owner", "Updated", "Description", "Type", "Maintenance",
                    "Power", "Resource", "%", "UBER", "AER", "Hopper", "Notes" };

        /**
         * An array of column header tool tips for the table of owners.
         */
        final String[] columnToolTips =
                {
                        null,
                        "The most recent date the harvester was refreshed",
                        null,
                        null,
                        "Remains of added amount and depletion date",
                        "Remains of added units and depletion date",
                        null,
                        "Concentration as read at the survey device",
                        "User Base Extraction Rate, adjusted for expertise, buff, and 50%",
                        "Actual Extraction Rate, UBER adjusted for concentration",
                        "Current fullness or amount at hopper, and full date",
                        null };

        
        @Override
        public TableCellDecorations getCellDecor(int row, int column,
                Object value) {

            if (column < 4 || (column > 6 && column != 10))
                return null;

            SWGHarvester harv = activeHarvs().get(row);
            long now = System.currentTimeMillis();
            int reminder = reminderTimeLimit <= 0
                    ? 24
                    : reminderTimeLimit;

            int check = 0; // 0 == OK, 1 == warn, 2 == ALARM
            if (column == 4) { // Maintenance
                long end = harv.getMaintEnds();
                if (end <= now)
                    check = 2;
                else {
                    long hoursLeft = (end - now) / (1000 * 60 * 60);
                    if (hoursLeft <= reminder)
                        check = 1;
                }
            } else if (column == 5) { // Power
                if (!harv.isSelfPowered()) {
                    long end = harv.getPowerEnds();
                    if (end <= now)
                        check = 2;
                    else {
                        long hoursLeft = (end - now) / (1000 * 60 * 60);
                        if (hoursLeft <= reminder)
                            check = 1;
                    }
                }
            } else if (column == 6) { // resource
                if (harv.getResource().isDepleted())
                    check = 2;
            } else if (column == 10) { // hopper
                long full = harv.getHopperFull();
                if (full < now)
                    check = 2;
                else {
                    float hoursLeft = (full - now) / ((float) 1000 * 60 * 60);
                    if (hoursLeft <= reminder)
                        check = 1;
                }
            }

            Color fg = null;
            Color bg = null;
            if (check > 0) {
                fg = check == 2
                        ? Color.WHITE // ALARM
                        : Color.BLACK; // warn
                bg = check == 2
                        ? Color.RED // ALARM
                        : Color.YELLOW; // warn
            }
            TableCellDecorations ret;
            MetalTheme theme = MetalLookAndFeel.getCurrentTheme();
            if(theme.getName().contains("Dark")) {
            	ret = new TableCellDecorations(null, bg, null, (Object[]) null);
            } else {
            	ret = new TableCellDecorations(bg, fg, null, (Object[]) null);
            }
            return ret;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
            case 1:
            case 4:
            case 5:
            case 7:
            case 8:
            case 9:
            case 10: // fall through
                return Number.class;
            case 6:
                return SWGResource.class;
            default:
                return String.class;
            }
        }

        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        
        public int getRowCount() {
            return activeHarvs().size();
        }

        
        public Object getValueAt(int row, int column) {
            SWGHarvester harv = activeHarvs().get(row);

            switch (column) {
            case 0:
                return harv.getOwner().getName();
            case 1:
                return Long.valueOf(harv.getLastUpdated());
            case 2:
                return harv.getName();
            case 3:
                return harv.type;
            case 4:
                return Float.valueOf(harv.getMaintRemains());
            case 5:
                return Float.valueOf(Math.max(harv.getPowerRemains(), 0.0f));
            case 6:
                return harv.getResource();
            case 7:
                return Integer.valueOf(harv.getConcentration());
            case 8:
                return Double.valueOf(harv.ber * harv.getBerModifier());
            case 9:
                return Double.valueOf(harv.getAER());
            case 10: {
                float f = harv.getHopperUnits() / harv.getHopperCapacity();
                f = Math.min(1.0f, f);
                Float value = Float.valueOf(100.0f * f);
                return value;
            }
            case 11:
                return harv.getNotes();
            default:
                return "ERROR";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // remember setValueAt if anything changes here
            if (columnIndex == 4 || columnIndex == 5 || columnIndex == 11)
                return true;
            return false;
        }

        
        @Override
        public void setValueAt(Object value, int row, int column) {
            SWGHarvester hv = activeHarvs().get(row);

            if (column == 11)
                hv.setNotes((String) value);
            else {
                try {
                    int i = ZNumber.intExc((String) value);
                    if (column == 4)
                        hv.setMaint(i);
                    else if (column == 5)
                        hv.setPower(i);
                } catch (Exception e) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
            fireTableRowsUpdated(row, row);
        }
    }

    /**
     * The table model for all defined harvesters.
     * 
     * @author John Thomas aka Chilastra.Oarun
     * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGHarvesterModel extends AbstractTableModel {

        /**
         * Table column header titles.
         */
        private final String[] colNames =
            { "Harvester description", "Type", "BER", "Hopper", "MOD" };

        @Override
        public Class<?> getColumnClass(int column) {
            if (column <= 1) return String.class;
            return Integer.class;
        }

        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        
        public int getRowCount() {
            return getHarvesters().size();
        }

        
        public Object getValueAt(int row, int column) {
            SWGHarvester harvester = getHarvesters().get(row);

            switch (column) {
            case 0:
                return harvester.getName();
            case 1:
                return harvester.type;
            case 2:
                return Integer.valueOf(harvester.ber);
            case 3:
                return Integer.valueOf(harvester.hopperSize);
            case 4:
            	return Double.valueOf(harvester.bmod);
            default:
                return "ERROR";
            }
        }
    }

    /**
     * The table model for harvester owners.
     * 
     * @author John Thomas aka Chilastra.Oarun
     */
    final class SWGHarvesterOwnerModel extends AbstractTableModel {

        /**
         * Table column header titles
         */
        private final String[] colNames =
            { "Owner name", "Hpr", "Mnt", "Eny", "Tch", "Buff" };

        /**
         * An array of column header tool tips for the table of owners.
         */
        final String[] columnToolTips =
            { "Owner name", "Harvester Storage Efficiency expertise",
                    "Harvester Maintenance Efficiency expertise",
                    "Harvester Energy Efficiency expertise",
                    "Advanced Harvesting Technology expertise",
                    "Harvester Fair (Entertainer buff)" };

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0
                    ? String.class
                    : Integer.class;
        }

        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        public int getRowCount() {
            return getOwners().size();
        }

        public Object getValueAt(int row, int column) {
            SWGHarvesterOwner owner = getOwners().get(row);

            switch (column) {
            case 0:
                return owner.getName();
            case 1:
                return Integer.valueOf(owner.getStorageEfficiency());
            case 2:
                return Integer.valueOf(owner.getMaintEfficiency());
            case 3:
                return Integer.valueOf(owner.getEnergyEfficiency());
            case 4:
                return Integer.valueOf(owner.getHarvestingTechnology());
            case 5:
                return Integer.valueOf(owner.getHarvestFair());
            default:
                return "ERROR";
            }
        }
    }
}
