package swg.gui.schematics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import swg.SWGAide;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.types.SWGCreatureResources;
import swg.crafting.resources.types.SWGFloraResources;
import swg.crafting.resources.types.SWGOrganic;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGDecoratedTableCellRenderer.DecoratedTableModel;
import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;
import swg.gui.common.SWGGui;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGJTable;
import swg.gui.common.SWGListCellRenderer;
import swg.gui.common.SWGListModel;
import swg.gui.common.SWGResourceStatRenderer;
import swg.gui.resources.SWGInventoryWrapper;
import swg.gui.resources.SWGResController;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.model.SWGProfession;
import swg.tools.SpringUtilities;
import swg.tools.ZNumber;
import swg.tools.ZString;

/**
 * This type contains the logic and elements to display current resources which
 * are better than anything in the resource inventory. This type is located in
 * this package rather than in {@code swg/gui/resources/} because it restricts
 * its evaluation to the selected set of favorite schematics.
 * <p>
 * Technically this class creates a list of elements for each unique combination
 * of resource-class/experimental-weights, where each element has a list of
 * schematics for the unique combination. This list is created in full, when the
 * user selects a set of schematics a subsets is extracted. This smaller list is
 * traversed and for each element it is determined whether a resource of the
 * appropriate class is spawning within a time frame specified by the user, and
 * it there is such a resource it is evaluated against the resource inventory.
 * If the resource equals or surpasses anything in inventory it is displayed.
 * <p>
 * For this filtering it is the the experimental weights that are used, but only
 * those experiments that are specified in SWGAide's list of <i>interesting </i>
 * experiments, or those that the user has specified an interest in. If the user
 * toggles a check-box this type displays low-quality resources for schematics
 * without experiment groups.
 * <p>
 * For schematics without experiments this type just determines if each required
 * resource class is in stock. Currently this type does not check whether an
 * existing resource is a resource that is reserved for some high quality use so
 * it should not be used for non-quality schematics.
 * <p>
 * This type provides several options to tweak what should be displayed.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
class SWGTodays extends JPanel {

    /**
     * A check-box for displaying "great" while ignoring inventory.
     */
    // static just to support before-GUI-finished operations
    private static JCheckBox great;

    /**
     * A list of assignees for the professions in SWG; compare
     * {@link #assigneeProfession()}.
     */
    private static List<SWGSchematicAssignee> professionAssignees;

    /**
     * A constant for an instance of this type, or {@code null} if this instance
     * is not yet fully created but a stub.
     */
    private static SWGTodays THIS;

    /**
     * A spinner at which the user specifies maximum age for resources.
     */
    private JSpinner ageSpinner;

    /**
     * A list at which to select an assignee.
     */
    private JList<SWGSchematicAssignee> assigneeList;

    /**
     * A convenience constant array of stats in game order.
     */
    private final Stat[] gOrder = Stat.gameOrder();

    /**
     * The URL for the schematics laboratory help page. This is the page which
     * is displayed when a sub-panel does not have its own help pages.
     */
    private final URL helpPage;

    /**
     * A check-box for switching between HQ and LQ mode.
     */
    private JCheckBox hqlqCheck;

    /**
     * A spinner at which the user specifies minimum amount in inventory to base
     * comparing and existence at.
     */
    private JSpinner invSpinner;

    /**
     * A flag which denotes if GUI events should be ignored until later. This
     * flag is {@code true} if there is a process importing guards, or if the
     * GUI is resetting.
     */
    private boolean isWorking = false;

    /**
     * A check-box for ignoring Kashyyykian and Mustafarian resources, creature
     * resources excluded.
     */
    private JCheckBox kmCheck;

    /**
     * A text area at which to display details about a selected
     * {@link SWGRCWPair}.
     */
    private JEditorPane rcwPairDetails;

    /**
     * A list which displays the schematics that pertain to a selected
     * {@link SWGRCWPair}.
     */
    private JList<SWGSchematic> schematics;

    /**
     * The component which is the parent for this instance, the tabbed pane.
     */
    private final SWGSchematicTab schemTab;

    /**
     * The currently selected assignee, or {@code null}.
     */
    private SWGSchematicAssignee selectedAssignee;

    /**
     * A flag that denotes if a message dialog should display.
     */
    private boolean skipDialog;

    /**
     * The model for the table of resources.
     */
    private TodaysTableModel todaysModel;

    /**
     * The GUI table for resources which are better than inventory.
     */
    private SWGJTable todaysTable;

    /**
     * A flag that denotes if this panel may update the schematic-resource
     * viewer continuously. Default is {@code false}.
     */
    private boolean updateViewer;

    /**
     * Creates an instance of this type.
     * 
     * @param parent the parent container
     */
    SWGTodays(SWGSchematicTab parent) {
        this.schemTab = parent;

        helpPage = SWGAide.class.getResource(
                "docs/help_schematics_todays_en.html");

        parent.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                focusGained();
            }
        });
    }

    /**
     * Called when the user clicks the assignee list with the mouse. If it is a
     * right-click this method displays a popup dialog for the user.
     * 
     * @param e the event that triggers the call
     */
    private void actionAssigneeMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            int idx = assigneeList.locationToIndex(e.getPoint());

            JPopupMenu popup = new JPopupMenu();
            popup.add(schemTab.assigneeMenu());
            popup.add(assigneeDefaultMenu(idx));
            popup.show(assigneeList, e.getX(), e.getY());

            if (assigneeList.getSelectedIndex() != idx)
                assigneeList.setSelectedIndex(idx);
        }
    }

    /**
     * Called when the user selects an element at the list of assignees for the
     * current galaxy. This method is the starting point that updates the main
     * table of current resources.
     * <p>
     * This implementation invokes methods to obtain a list of
     * {@link SWGRCWPair} for the schematics of the assignee, to compare today's
     * spawning resources versus the resource inventory, and finally this method
     * updates the table model with the result which in its turn updates the
     * table. If the argument is {@code null} this method resets this GUI.
     */
    
    private void actionAssigneeSelected() {
        SWGSchematicAssignee o = assigneeList.getSelectedValue();
        Triplet tri = selectedAssignee == o
                ? todaysTableSelection()
                : null;
        selectedAssignee = o;

        if (selectedAssignee == null) {
            guiClear();
            return;
        }

        isWorking = true;

        boolean hq = hqlqCheck.isSelected();

        List<SWGInventoryWrapper> inv = Collections.emptyList();
        if (hq && !great.isSelected()) {
            inv = SWGSchemController.inventory();
            if (inv.isEmpty())
                JOptionPane.showMessageDialog(schemTab,
                        "You have no resource inventory for " +
                                SWGFrame.getSelectedGalaxy().getName() +
                                ".\nThis panel will just be able to display " +
                                "low-quality resources.", "Empty inventory",
                        JOptionPane.WARNING_MESSAGE);
        }

        List<Triplet> ts = todaysTS(hq, great.isSelected(),
                selectedAssignee.getFavorites(), inv);
        todaysModel.setElements(ts);
        // restore table selection, if possible
        isWorking = false;
        if (ts.isEmpty())
            guiClear();
        else if (tri != null)
            todaysTableSelect(tri);
        else
            todaysTable.getSelectionModel().setSelectionInterval(0, 0);

        if (ts.isEmpty() && selectedAssignee.getName().equals("Pro: All")) {
            if (!skipDialog)
                JOptionPane.showMessageDialog(assigneeList,
                        "No new resources outshine your inventory", "Info",
                        JOptionPane.PLAIN_MESSAGE);
            skipDialog = true; // once per session is enough
        }

        schemTab.tintTabs(false);
    }

    /**
     * Called when the user mouse-clicks the GUI. If it is a right-click this
     * method displays a popup-dialog with options. This method determines the
     * source for the event and hence the appropriate action. The source must be
     * a {@link Component}.
     * 
     * @param e the event that triggers the invocation
     */
    private void actionMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            final Triplet tri = todaysTableSelection();
            if (tri == null) return;

            JPopupMenu ppp = new JPopupMenu();
            Component src = (Component) e.getSource();

            if (src == schematics) {
                schematics.setSelectedIndex(
                            schematics.locationToIndex(e.getPoint()));
                SWGSchematic s = schematics.getSelectedValue();

                ppp.add(schemTab.schematicSelectMenu(s, this));

            } else {
                SWGKnownResource tr = tri.current;
                SWGResourceClass rc = tri.rcwPair.rc();
                SWGWeights wg = tri.rcwPair.filter() instanceof SWGWeights
                        ? (SWGWeights) tri.rcwPair.filter()
                        : null;
                SWGKnownResource ir = tri.inventory != null
                        ? tri.inventory.getResource()
                        : tr;
                if (src == todaysTable) {
                    ppp.add(SWGResController.resourceDetailsMenu(tr, src));
                    ppp.add(SWGResController.currentSelectMenu(tr));
                    ppp.addSeparator();
                } else if (src == rcwPairDetails)
                    tr = null;

                ppp.add(SWGResController.currentFilterMenu(rc, wg, tr));
                ppp.add(SWGResController.inventoryFilterMenu(rc, wg, ir, false));

                ppp.addSeparator();
                ppp.add(SWGSchemResViewer.displayMenu(tr, this));
                updateViewer = true; // by chance, it is reset if...
                ppp.addSeparator();

                if (tri.current.rc() instanceof SWGCreatureResources) {
                    ppp.add(SWGResController.creatureHarvMenu(
                            (SWGCreatureResources) tri.current.rc(),
                            todaysTable));
                }
                ppp.add(makeOrganicsMenu());
            }

            ppp.show(src, e.getX(), e.getY());
        }
    }

    /**
     * Called when the user selects a schematics. This method invokes
     * {@link SWGSchematicTab#schematicSelect(SWGSchematic, JComponent)}.
     */
    private void actionSchematicSelected() {
        SWGSchematic s = schematics.getSelectedValue();
        schemTab.schematicSelect(s, this);
    }

    /**
     * Called when the user selects an element at the table of resources. This
     * method updates the display text fields with information that pertains to
     * the selected element.
     */
    private void actionTodaysTableSelected() {
        if (isWorking) return;

        Triplet tri = todaysTableSelection();
        if (tri == null) return;

        rcwPairDetails.setText(tripletDetails(tri));
        rcwPairDetails.setCaretPosition(0);

        schematics.clearSelection();
        ((SWGListModel<SWGSchematic>) schematics.getModel()).setElements(
                schematics(tri, selectedAssignee));
        if (updateViewer)
            updateViewer = SWGSchemResViewer.updateDisplay(
                    tri.current, this);

    }

    /**
     * Helper method which creates and returns a menu to select a default
     * assignee for automatic alerts.
     * <p>
     * Notice that the profession "ALL" is stored as {@code null} in SWGAide's
     * preferences keeper.
     * 
     * @param idx the index for the mouse pointer
     * @return a menu item
     */
    private JMenuItem assigneeDefaultMenu(int idx) {
        final String an = assigneeList.getModel().getElementAt(idx).
                getName();

        String pn = (String) SWGFrame.getPrefsKeeper().get(
                "schemTodaysAlertDefaultAss");

        final JCheckBoxMenuItem m = new JCheckBoxMenuItem("Default assignee");
        m.setToolTipText("Auto-select and scan for matching resources");
        m.setSelected(an.equals(pn) || (pn == null && an.equals("Pro: All")));

        m.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                SWGFrame.getPrefsKeeper().add("schemTodaysAlertDefaultAss",
                        m.isSelected() && !an.equals("Pro: All")
                                ? an
                                : null); // remove previous -> prof-all
            }
        });

        return m;
    }

    /**
     * This method is invoked when the user selects any one of the tabs in this
     * tabbed pane, or if the collection of assignees is modified in any way. If
     * this element is yet a stub it is populated.
     */
    void focusGained() {
        if (schemTab.frame.getTabPane().getSelectedComponent() == schemTab
                && schemTab.getSelectedComponent() == this) {
            synchronized (this) {
                boolean first = false;
                if (THIS == null) {
                    make();
                    first = true;
                }

                guiUpdate();
                if (first) assigneeSelect();

                SWGHelp.push(helpPage);
                schemTab.updateStatbar2();
            }
        } else if (THIS != null) {
            SWGHelp.remove(helpPage);
            schemTab.tintTabs(false);
        }
    }

    /**
     * 
     */
    
    private void guiClear() {
        rcwPairDetails.setText("");
        schematics.clearSelection();
        ((SWGListModel<SWGSchematic>) schematics.getModel()).setElements(null);
        todaysModel.setElements(null);
    }

    /**
     * Helper method which updates the GUI. This method is invoked if the user
     * change an option or if anything else requires the GUI to update itself.
     * In particular, if an assignee is selected it is re-selected to trigger a
     * call to {@link #actionAssigneeSelected()}.
     */
    void guiUpdate() {
        if (isWorking || THIS == null) return;

        // Keep for later selection restore
        Triplet tri = todaysTableSelection();
        SWGSchematicAssignee prv = assigneeList.getSelectedValue();
        SWGSchematic sch = schematics.getSelectedValue();

        assigneeList.clearSelection();

        List<SWGSchematicAssignee> as = SWGSchematicTab.assignees();
        as.addAll(assigneeProfession());

        ((SWGListModel<SWGSchematicAssignee>) assigneeList.getModel()).setElements(as);

        assigneeList.setSelectedValue(prv, true); // restore, if possible
        todaysTableSelect(tri);
        schematics.setSelectedValue(sch, true);
    }

    /**
     * Helper method which creates the interior of this GUI element when the
     * user selects this element for the first time. This method must only be
     * called once, from {@link #focusGained()}.
     */
    private void make() {
        this.setLayout(new BorderLayout());
        this.add(makeNorth(), BorderLayout.PAGE_START);
        this.add(makeTable(), BorderLayout.CENTER);

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK),
                "keystrokeAltC");
        this.getActionMap().put("keystrokeAltC", new AbstractAction() {
            
            public void actionPerformed(ActionEvent actionEvent) {
                SWGSchematicAssignee as = assigneeList.getSelectedValue();
                assigneeList.clearSelection();
                todaysTable.getRowSorter().setSortKeys(null);
                assigneeList.setSelectedValue(as, true);
            }
        });

        THIS = this;
    }

    /**
     * Helper method which creates and returns a label with the specified text
     * and tool tip text. If any of the arguments is {@code null} it is ignored.
     * 
     * @param text the text for the label, or {@code null}
     * @param tt the tool tip for the label, or {@code null}
     * @return a label
     */
    private JLabel makeLabel(String text, String tt) {
        JLabel l = new JLabel(text);
        l.setToolTipText(tt);
        return l;
    }

    /**
     * Helper method which creates and returns the topmost GUI element. This
     * element contains elements to select an assignee, and for a selected
     * resource, to display details about the resource-class-and-weight that
     * selected it, to display details about the resource, and to display which
     * schematics that use it.
     * 
     * @return a GUI component
     */
    private Component makeNorth() {
        Box bp = Box.createHorizontalBox();

        bp.add(makeNorthASList());
        bp.add(makeNorthOptions());
        bp.add(makeNorthDetailsPanel());
        bp.add(makeNorthSchematicsList());

        return bp;
    }

    /**
     * Helper method which creates and returns the north-west GUI element to
     * select an assignee. This element contains a list to select an assignee
     * with its list of schematics. This list of schematics is the basis for the
     * subsequent compare on currently spawning resources using a list of
     * resource-class-and-weight that is obtained from the schematics
     * controller.
     * 
     * @return a GUI component
     */
    private Component makeNorthASList() {
        assigneeList = new JList<SWGSchematicAssignee>(new SWGListModel<SWGSchematicAssignee>());
        assigneeList.setToolTipText("Select assignee for favorite schematics");
        assigneeList.setCellRenderer(new SWGListCellRenderer<SWGGui>() {
            @Override
            protected String labelString(SWGGui value) {
                return value.getName();
            }
        });

        assigneeList.addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    actionAssigneeSelected();
                }
            }
        });
        assigneeList.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                actionAssigneeMouse(e);
            }
        });

        JScrollPane jsp = new JScrollPane(assigneeList);
        jsp.setToolTipText("Select assignee for favorite schematics");

        Box hb = new Box(BoxLayout.LINE_AXIS) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = 200;
                return d;
            }
        };
        hb.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Assignees"));

        hb.add(jsp);
        return hb;
    }

    /**
     * Helper method which creates and returns a GUI element for the details
     * view. This element contains the text are which displays the details of a
     * selected RCW-pair and best-of-inventory.
     * 
     * @return a GUI component
     */
    private Component makeNorthDetailsPanel() {
        rcwPairDetails = new JEditorPane();
        rcwPairDetails.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        rcwPairDetails.setContentType("text/html");
        rcwPairDetails.setEditable(false);

        rcwPairDetails.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                actionMouse(e);
            }
        });

        Box bp = Box.createHorizontalBox();
        bp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Resource class & weights and best from Inventory"));
        JScrollPane jsp = new JScrollPane(rcwPairDetails);
        bp.add(jsp);
        return bp;
    }

    /**
     * Helper method which creates and returns the north GUI element for user
     * options. This element contains a spinner at which to specify max age, a
     * spinner to specify minimum inventory stock for inventory to count, and a
     * check-box for HQ versus LQ.
     * 
     * @return a GUI component
     */
    private Component makeNorthOptions() {
        final JPanel op = new JPanel(new SpringLayout());
        op.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Options"));

        String tt = "Max age for displayed resources";
        op.add(makeLabel("Max days:", tt));
        op.add(makeNorthOptionsAge(tt));

        tt = "Ignore inventory stacks smaller than limit";
        op.add(makeLabel("Inventory:", tt));
        op.add(makeNorthOptionsInv(tt));

        tt = "Toggle between HQ / LQ display mode";
        op.add(makeLabel("HQ / LQ:", tt));
        op.add(makeNorthOptionsHqLq());

        tt = "Display anything better than \"Great\"";
        op.add(makeLabel("Show Great:", tt));
        op.add(makeNorthOptionsGreat(tt));

        tt = "Ignore resources from Kashyyyk and Mustafar";
        op.add(makeLabel("Ignore K/M:", tt));
        op.add(makeNorthOptionsKM(tt));

        Dimension d = new Dimension(80, 25);
        ageSpinner.setMaximumSize(d);
        invSpinner.setMaximumSize(d);
        ageSpinner.setPreferredSize(d);
        invSpinner.setPreferredSize(d);

        SpringUtilities.makeCompactGrid(op, 5, 2, 0, 0, 0, 1);

        Box ob = Box.createVerticalBox();
        ob.add(op);
        ob.add(Box.createVerticalGlue());
        return ob;
    }

    /**
     * Helper method which creates and returns a GUI element for specifying max
     * age for displayed resources. This method creates a number-spinner.
     * 
     * @param tt a tool tip text
     * @return a GUI element
     */
    private Component makeNorthOptionsAge(String tt) {
        int age = ((Integer) SWGFrame.getPrefsKeeper().get(
                "schemTodaysAlertMaxAge", Integer.valueOf(3))).intValue();
        
        if (age < 0 || age > 6) {
            // XXX: remove in the future, added in April 2011
            // fix for possible error in early version of this panel
            age = 3;
            SWGFrame.getPrefsKeeper().add(
                    "schemTodaysAlertMaxAge", Integer.valueOf(3));
        }
        
        ageSpinner = new JSpinner(new SpinnerNumberModel(age, 0, 6, 1));
        ageSpinner.setToolTipText(tt);

        ageSpinner.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                Integer a = (Integer) ageSpinner.getValue();
                SWGFrame.getPrefsKeeper().add("schemTodaysAlertMaxAge", a);
                guiUpdate();
            }
        });

        return ageSpinner;
    }

    /**
     * Helper method which creates and returns a GUI element for displaying
     * "Great" resources. "Great" is defined by the user at the Resources panel.
     * This method creates a check-box.
     * 
     * @param tt a tool tip text
     * @return a GUI element
     */
    private Component makeNorthOptionsGreat(String tt) {
        great = new JCheckBox();
        great.setToolTipText(tt);
        great.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                guiUpdate();
            }
        });
        return great;
    }

    /**
     * Helper method which creates and returns a GUI element for switching
     * between HQ and LQ display. This method creates a check-box.
     * 
     * @return a GUI element
     */
    private Component makeNorthOptionsHqLq() {
        final String tl = "Toggle to LQ display mode";
        final String tq = "Toggle to HQ display mode";

        hqlqCheck = new JCheckBox();
        hqlqCheck.setToolTipText(tl);
        hqlqCheck.doClick();
        hqlqCheck.addActionListener(new ActionListener() {

            /* Remember old great-value until next click */
            boolean oldGreat;

            
            @Override
            public void actionPerformed(ActionEvent e) {
                isWorking = true;
                boolean b = hqlqCheck.isSelected();
                // the following is to handle the great-mode for LQ versus HQ
                if (b) { // HQ
                    great.setEnabled(true);
                    if (oldGreat) great.doClick();
                } else { // LQ
                    oldGreat = great.isSelected();
                    if (oldGreat) great.doClick();
                    great.setEnabled(false);
                }

                hqlqCheck.setToolTipText(b
                        ? tl
                        : tq);
                isWorking = false;
                guiUpdate();
            }
        });
        return hqlqCheck;
    }

    /**
     * Helper method which creates and returns a GUI element for specifying min
     * amount in inventory for comparison. This method creates a number-spinner.
     * 
     * @param tt a tool tip text
     * @return a GUI element
     */
    private Component makeNorthOptionsInv(String tt) {
        int units = ((Integer) SWGFrame.getPrefsKeeper().get(
                "schemTodaysMinInventory", Integer.valueOf(50000))).intValue();
        invSpinner = new JSpinner(
                new SpinnerNumberModel(units, 0, 1000000, 10000));
        invSpinner.setToolTipText(tt);

        invSpinner.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                Integer u = (Integer) invSpinner.getValue();
                SWGFrame.getPrefsKeeper().add("schemTodaysMinInventory", u);
                guiUpdate();
            }
        });

        return invSpinner;
    }

    /**
     * Helper method which creates and returns a GUI element for inclusion of
     * Kashyyykian and Mustafarian resources, creature resources excluded. This
     * method creates a check-box.
     * 
     * @param tt a tool tip text
     * @return a GUI element
     */
    private Component makeNorthOptionsKM(String tt) {
        kmCheck = new JCheckBox();
        kmCheck.setToolTipText(tt);
        kmCheck.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("schemTodaysIgnoreKM",
                        Boolean.valueOf(kmCheck.isSelected()));
                guiUpdate();
            }
        });
        kmCheck.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                        "schemTodaysIgnoreKM", Boolean.TRUE)).booleanValue());

        return kmCheck;
    }

    /**
     * Helper method which creates and returns the north-east GUI element for
     * schematics. This element is a list that displays schematics for the
     * selected RCW-pair.
     * 
     * @return a GUI component
     */
    private Component makeNorthSchematicsList() {
        schematics = new JList<SWGSchematic>(new SWGListModel<SWGSchematic>());
        schematics.setCellRenderer(new SWGListCellRenderer<SWGSchematic>() {
            @Override
            protected String labelString(SWGSchematic value) {
                return value.getName();
            }
        });

        schematics.addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    actionSchematicSelected();
                }
            }
        });
        schematics.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                actionMouse(e);
            }
        });

        Box hb = new Box(BoxLayout.LINE_AXIS) {
            
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = schemTab.getWidth() / 5;
                return d;
            }
        };
        hb.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(
                        EtchedBorder.LOWERED), "Schematics"));

        JScrollPane jsp = new JScrollPane(schematics);
        hb.add(jsp);
        return hb;
    }

    /**
     * Creates and returns a check-box menu item for omitting creature resources
     * if Organic is called for. The action listener of this item sets a
     * preference in SWGAide's DAT file and triggers an update of this GUI.
     * 
     * @return a menu item
     */
    private JCheckBoxMenuItem makeOrganicsMenu() {
        final JCheckBoxMenuItem om = new JCheckBoxMenuItem(
                "Organic vs. Creature Resources");
        om.setToolTipText("Organic disregards Creature Resources");
        om.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "schemTodaysOrganicDisCR", Boolean.FALSE)).booleanValue());
        om.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Boolean b = Boolean.valueOf(om.isSelected());
                SWGFrame.getPrefsKeeper().add("schemTodaysOrganicDisCR", b);
                guiUpdate();
            }
        });
        return om;
    }

    /**
     * Helper method which creates and returns the center GUI element. This
     * table displays resources which are better than anything in inventory for
     * a list of {@link Triplet}.
     * 
     * @return a GUI component
     */
    
    private Component makeTable() {
        todaysModel = new TodaysTableModel();
        todaysTable = new SWGJTable(todaysModel);

        TableCellRenderer pr = new SWGDecoratedTableCellRenderer(todaysModel) {
            @Override
            protected Object myValue(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                return value != null
                        ? ZNumber.asPercent(((Double) value).doubleValue())
                        : "";
            }
        };
        todaysTable.setDefaultRenderer(Double.class, pr); // percent

        todaysTable.setDefaultRenderer(Long.class, // age
                new SWGResController.AgeRenderer(todaysModel));

        todaysTable.setDefaultRenderer(Integer.class, // stats
                new SWGResourceStatRenderer(todaysModel) {
                    @Override
                    protected void myFont(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row,
                            int column, TableCellDecorations decor) {
                        Font f;
                        if (column >= 13)
                            f = SWGGuiUtils.fontPlain();
                        else {
                            Triplet tri = todaysModel.getElement(row);
                            Stat s = gOrder[column - 2];
                            f = tri.rcwPair.filter().value(s) > 0
                                    ? SWGGuiUtils.fontBold()
                                    : SWGGuiUtils.fontPlain();
                        }
                        setFont(f);
                    }
                });

        todaysTable.setDefaultRenderer(
                String.class, new SWGDecoratedTableCellRenderer(todaysModel));

        todaysTable.setSelectionMode(
                ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        todaysTable.setAutoCreateRowSorter(true);
        todaysTable.getTableHeader().setReorderingAllowed(false);

        int w;
        w = SWGGuiUtils.fontWidth(this, "1 000", SWGGuiUtils.fontBold()) + 5;
        SWGGuiUtils.tableColumnSetWidth(todaysTable, 0, 20, 150, 150);
        SWGGuiUtils.tableSetColumnWidths(todaysTable, 2, 2 + 11, w, 5);
        SWGGuiUtils.tableColumnFixWidth(todaysTable, 14, 65);
        SWGGuiUtils.tableColumnFixWidth(todaysTable, 15, w);

        todaysTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting())
                            actionTodaysTableSelected();
                    }
                });
        todaysTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON3) return;

                int rp = todaysTable.rowAtPoint(todaysTable.getMousePosition());
                if (rp < 0)
                    return;
                else if (rp != todaysTable.getSelectedRow())
                    todaysTable.getSelectionModel().
                            setSelectionInterval(rp, rp);

                actionMouse(e);
            }
        });

        JScrollPane jsp = new JScrollPane(todaysTable,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return jsp;
    }

    /**
     * Helper method which returns a filtered list of schematics from the
     * specified triplet, or {@code null}. Each element in the returned list is
     * also a favorite schematic for the specified assignee. Rather than an
     * empty list {@code null} is returned.
     * 
     * @param tri a triplet
     * @param as a schematic assignee
     * @return a list of schematics, or {@code null}
     */
    private List<SWGSchematic> schematics(Triplet tri, SWGSchematicAssignee as) {
        if (as == null || tri == null) return null;
        List<SWGSchematic> ret = new ArrayList<SWGSchematic>();
        for (SWGSchematic s : tri.rcwPair.schematics())
            if (as.getFavorites().contains(s) && !ret.contains(s))
                ret.add(s);

        return ret.isEmpty()
                ? null
                : ret;
    }

    /**
     * Helper method which selects the specified triplet at the table. If the
     * argument is {@code null}, or if the argument is not visible, this method
     * does nothing.
     * 
     * @param tri a triplet, or {@code null}
     */
    
    private void todaysTableSelect(Triplet tri) {
        if (tri == null) return;
        int i = 0;
        for (; i < todaysModel.getRowCount(); ++i) {
            Triplet t = todaysModel.getElement(i);
            if (tri.equals(t)) {
                i = todaysTable.convertRowIndexToView(i);
                todaysTable.getSelectionModel().setSelectionInterval(i, i);
                break;
            }
        }
    }

    /**
     * Helper method which returns the selected element at the table of today's
     * best, or {@code null}. This method handles possible conversion between
     * table and model and returns the proper element, or {@code null} if
     * nothing is selected.
     * 
     * @return a selected element, or {@code null}
     */
    
    private Triplet todaysTableSelection() {
        int row = todaysTable.getSelectedRow();
        if (row < 0) return null;

        row = todaysTable.convertRowIndexToModel(row);
        return todaysModel.getElement(row);
    }

    /**
     * Helper methods which returns a string with details for the specified
     * triplet.
     * 
     * @param tri a triplet element
     * @return a string with details
     */
    private String tripletDetails(Triplet tri) {
        if (tri == null) return "";

        final String FS = "<font face=\"arial,sans-serif\" size=\"-1\">";

        ZString z = new ZString("<html>");
        z.app("<table border=0 cellspacing=0 cellpadding=0><tr><td>");
        z.app(FS).app("<b>Filter: </b></font></td><td>");
        z.app(FS).app(tri.rcwPair.rc()).app(" ¤ ");
        tri.rcwPair.filter().toString(z, false);
        z.app("</font></td></tr>");

        if (tri.inventory != null) {
            z.app("<tr><td>").app(FS);
            z.app("<b>Inventory: </b></font></td><td>").app(FS);
            SWGKnownResource kr = tri.inventory.getResource();
            if (kr.galaxy().equals(SWGFrame.getSelectedGalaxy()) == false)
                z.app('(').app(kr.galaxy().getName()).app(')');
            z.app(kr.getName()).app(" --- ");
            z.app(tri.inventory.getResource().rc().rcName());
            z.app("</font></td></tr>");

            z.app("<tr><td>&nbsp;</td><td>").app(FS);
            tri.inventory.getResource().stats().toString(z, true);
            z.app(" </font></td></tr>");

            z.app("<tr><td>").app(FS);
            z.app("<b>Amount: </b></font></td><td>").app(FS);
            z.app(ZNumber.asText(tri.inventory.getAmount(), true, true));
            z.app("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; <b>Rate: </b>");
            z.app(ZNumber.asText((int) tri.rateInv, true, true));
            z.app("</font></td></tr>");

            String n = tri.inventory.getNotes();
            if (!n.isEmpty()) {
                z.app("<tr><td>").app(FS);
                z.app("<b>Notes: </b></font></td><td>").app(FS);
                z.app(n).app("</font></td></tr>");
            }
        }
        return z.app("</table></html>").toString();
    }

    /**
     * Creates and returns a list of "assignees" with all schematics per
     * profession. The list is temporary and is referenced by
     * {@link #professionAssignees} during the session.
     * 
     * @return a list of assignees
     */
    private static List<SWGSchematicAssignee> assigneeProfession() {
        if (professionAssignees != null) return professionAssignees;

        List<SWGProfession> pl = Arrays.asList(SWGProfession.values());

        List<SWGSchematicAssignee> ret =
                new ArrayList<SWGSchematicAssignee>(pl.size());

        for (SWGProfession p : pl) {
            String n = "Pro: " + p.getNameShort();
            SWGSchematicAssignee a = new SWGSchematicAssignee(n);
            for (SWGSchematic s : SWGSchematicsManager.getSchematics(p))
                a.addFavorite(s);

            ret.add(a);
        }

        professionAssignees = ret;
        return professionAssignees;
    }

    /**
     * Helper method which finds and selects the default assignee at the list of
     * assignees. This method conveniently returns a list of favorite schematics
     * for the default assignee or profession. If this component is not fully
     * created this method just returns a list.
     * <p>
     * Notice that the profession "ALL" is stored as {@code null} in SWGAide's
     * preferences keeper.
     * 
     * @return a list of schematics
     */
    private static List<SWGSchematic> assigneeSelect() {
        String pn = (String) SWGFrame.getPrefsKeeper().get(
                "schemTodaysAlertDefaultAss");

        SWGSchematicAssignee ass = null;
        if (pn == null) {
        	pn = "Pro: All";
        }

        List<SWGSchematicAssignee> al = pn.startsWith("Pro: ")
                ? assigneeProfession()
                : SWGSchematicTab.assignees();
        for (SWGSchematicAssignee a : al) {
            if (a.getName().equals(pn)) {
                ass = a;
                break;
            }
        }

        if (THIS != null) {
            THIS.assigneeList.setSelectedValue(ass, true);
        }

        return ass.getName().equals("Pro: All")
                ? SWGSchematicsManager.getSchematics(SWGProfession.ALL)
                : ass.getFavorites();
    }

    /**
     * Helper method which returns a filtered set of current resources for the
     * current galaxy. All elements in the returned set are younger than the
     * specified number of days.
     * 
     * @param days max number of days for resources
     * @return a set of resources
     */
    private static SWGResourceSet current(int days) {
        final long d = days * 24 * 3600;
        final long ageLimit = (System.currentTimeMillis() / 1000) - d;

        final Comparable<SWGKnownResource> comp =
                new Comparable<SWGKnownResource>() {
                    @Override
                    public int compareTo(SWGKnownResource o) {
                        return (o.availableFirst().available()
                                > ageLimit)
                                        ? 0
                                        : -1;
                            }
                };
        SWGResourceSet current = SWGSchemController.spawning();
        return current.subsetBy(comp);
    }

    /**
     * Helper method which returns a filtered inventory. Each element in the
     * returned list has at least the specified number of units, or there is an
     * harvester active for the element and we assume the amount is soon met;
     * this is so that displayed resources are computed against inventory <i>or
     * </i> against what is harvested.
     * 
     * @param inv a resource inventory
     * @param amount minimum amount for inclusion
     * @param gxy a galaxy constant
     * @return a filtered inventory
     */
    private static List<SWGInventoryWrapper> inventory(
            List<SWGInventoryWrapper> inv, int amount, SWGCGalaxy gxy) {

        List<SWGInventoryWrapper> ret =
                new ArrayList<SWGInventoryWrapper>(inv.size());

        for (SWGInventoryWrapper iw : inv)
            if (iw.getAmount() > amount
                    || SWGResController.harvesterActive(iw.getResource(), gxy))
                ret.add(iw);

        return ret;
    }

    /**
     * Helper method which returns a list of {@link Triplet} elements that
     * outshines inventory. This method iterates over the specified list of
     * RCW-pairs and for each element, if a spawning resource that matches the
     * element also is better than anything in inventory a triplet is created
     * and added to the returned list. See also
     * {@link Triplet#Triplet(SWGRCWPair, SWGKnownResource, SWGInventoryWrapper)}
     * <p>
     * This method does not support {@link SWGRCWPair#LQ_FILTER} but just
     * weighed filters.
     * 
     * @param rcwps a list of {@link SWGRCWPair}
     * @param current a set of current resources
     * @param inv all resource inventory
     * @return a list of resource triplets that are better than inventory
     * @throws ClassCastException if an element is not for weighed filter
     */
    private static List<Triplet> todaysHQ(List<SWGRCWPair> rcwps,
            SWGResourceSet current, List<SWGInventoryWrapper> inv) {

        // local set of KM-free resources, only if inorganic-KM creature
        // resources is asked for all of current is used
        boolean notKM = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "schemTodaysIgnoreKM", Boolean.TRUE)).booleanValue();
        SWGResourceSet noKMinorganic = current.subsetBy(
                SWGResController.kmResourceComparator());

        List<Triplet> ret = new ArrayList<Triplet>(48);

        double minimum = 1000.0 * (great != null && great.isSelected()
                ? SWGGuiUtils.statLimits[2]
                : SWGGuiUtils.statLimits[0]);

        boolean notCR = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "schemTodaysOrganicDisCR", Boolean.FALSE)).booleanValue();

        for (SWGRCWPair rcw : rcwps) {
            SWGInventoryWrapper iw = todaysInventory(rcw, inv, !notCR);

            SWGWeights wg = ((SWGWeights) rcw.filter());
            SWGResourceClass rc = rcw.rc();

            double w = Math.max(minimum, iw != null
                    ? wg.rate(iw.getResource(), rc, true)
                    : 0);

            rc = notCR && rc == SWGOrganic.getInstance()
                    ? SWGFloraResources.getInstance()
                    : rc;

            SWGResourceSet curr = notKM
                    && !(SWGResourceClass.canSpawnAt(rc.rcName(),
                                 SWGPlanet.KASHYYYK)
                         || SWGResourceClass.canSpawnAt(rc.rcName(),
                                 SWGPlanet.MUSTAFAR))
                    ? noKMinorganic.subsetBy(wg, rc, true, w)
                    : current.subsetBy(wg, rc, true, w);
            for (SWGKnownResource kr : curr)
                if (iw == null || kr != iw.getResource()) {
                    // do not include "self" ... probably harvested
                    Triplet t = new Triplet(rcw, kr, iw);
                    ret.add(t);
                }
        }

        return ret;
    }

    /**
     * Helper method which returns the inventory wrapper that matches the
     * specified resource-class and weight the best. The boolean parameter is
     * regarded only if the the resource class of the specified
     * {@link SWGRCWPair} equals SWGOrganic: if the parameter is {@code true}
     * this method matches also inventory wrappers that are for Creature
     * Resources. If there are several matching wrappers the best of them is
     * returned. If no wrapper is found this method returns {@code null} .
     * 
     * @param rcwp the "filter"
     * @param inv a list of inventory resources
     * @param cr {@code true} to match Creature Resources for Organic
     * @return the best match, or {@code null}
     */
    private static SWGInventoryWrapper todaysInventory(
            SWGRCWPair rcwp, List<SWGInventoryWrapper> inv, boolean cr) {

        SWGWeights wg = ((SWGWeights) rcwp.filter());
        SWGResourceClass zuper = rcwp.rc();
        boolean in = zuper == SWGOrganic.getInstance()
                ? cr
                : true;

        double best = 0.0;
        SWGInventoryWrapper wrapper = null;
        for (SWGInventoryWrapper iw : inv) {
            SWGKnownResource kr = iw.getResource();
            if (kr.rc().isSub(zuper)) {
                if (in || !kr.rc().isSub(SWGCreatureResources.class)) {
                    double w = wg.rate(iw.getResource(), zuper, true);
                    if (w > best) {
                        best = w;
                        wrapper = iw;
                    }
                }
            }
        }

        return wrapper;
    }

    /**
     * Helper method which returns a list of {@link Triplet} elements. This
     * method iterates over the specified list and for each element, if a
     * spawning resource is not found in inventory a triplet is created and
     * added to the list. For further details, see comments on the constructor
     * {@link Triplet#Triplet(SWGRCWPair, SWGKnownResource, SWGInventoryWrapper)}
     * .
     * <p>
     * This method supports only {@link SWGRCWPair#LQ_FILTER}.
     * 
     * @param rcwps a list of {@link SWGRCWPair}
     * @param curr a set of current resources
     * @return a list of resource triplets
     */
    private static List<Triplet> todaysLQ(
            List<SWGRCWPair> rcwps, SWGResourceSet curr) {

        List<Triplet> ret = new ArrayList<Triplet>(48);

        for (SWGRCWPair r : rcwps) {
            if (SWGResController.inventoryExists(
                    r.rc(), SWGFrame.getSelectedGalaxy(), false)) continue;

            SWGResourceSet cur = curr.subsetBy(r.rc());
            if (cur.isEmpty()) continue;

            for (SWGKnownResource kr : cur) {
                Triplet t = new Triplet(r, kr, null);
                ret.add(t);
            }
        }

        return ret;
    }

    /**
     * Returns {@code true} if this component will display resources for
     * {@link SWGProfession#ALL}, {@code false} otherwise. This method is safe
     * also before this component is fully instantiated.
     * 
     * @return {@code true} if some resources outshine inventory
     */
    static boolean todaysTinted() {
        return great == null
                ? !todaysTS(true, false, assigneeSelect(),
                        SWGSchemController.inventory()).isEmpty()
                : THIS.todaysModel.getRowCount() > 0;
    }

    /**
     * Helper method which returns a list of {@link Triplet} elements for
     * spawning resources that outshine anything in inventory for the current
     * galaxy. This method obtains a list of RCW-pairs for the specified
     * schematics, a set of current resources
     * 
     * @param hq {@code true} to filter for HQ schematics
     * @param grt {@code true} if check-box Show Great is selected
     * @param schems a list of schematics for the RCW-pairs
     * @param inv an inventory list, may be empty
     * @return a list of Triplet elements
     */
    private static List<Triplet> todaysTS(boolean hq, boolean grt,
            List<SWGSchematic> schems, List<SWGInventoryWrapper> inv) {

        List<SWGRCWPair> rcwps = SWGSchemController.rcwPairs(hq, schems);

        List<SWGInventoryWrapper> inv2 = inv;
        if (hq && !grt) {
            int amount = ((Integer) SWGFrame.getPrefsKeeper().get(
                    "schemTodaysMinInventory",
                    Integer.valueOf(50000))).intValue();
            inv2 = inventory(inv2, amount, SWGFrame.getSelectedGalaxy());
        }

        int days = ((Integer) SWGFrame.getPrefsKeeper().get(
                "schemTodaysAlertMaxAge", Integer.valueOf(3))).intValue();
        SWGResourceSet current = current(days);

        List<Triplet> ts = hq
                ? todaysHQ(rcwps, current, inv2)
                : todaysLQ(rcwps, current);

        Collections.sort(ts, new Comparator<Triplet>() {
            @Override
            public int compare(Triplet o1, Triplet o2) {
                return (int) (o1.current.age() - o2.current.age());
            }
        });
        return ts;
    }

    /**
     * The model for the main table of current resources that are better than
     * resource inventory. This model implements {@link DecoratedTableModel}.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private class TodaysTableModel extends AbstractTableModel
            implements DecoratedTableModel {

        /**
         * Table column header titles.
         */
        private final String[] columnNames =
                { "Name", "Resource Class", "ER", "CR", "CD", "DR", "FL", "HR",
                        "MA", "PE", "OQ", "SR", "UT", "Rate", "%%", "Age" };

        /**
         * The list of triplets for resources that are better than inventory.
         */
        private List<Triplet> elements;

        
        @Override
        public TableCellDecorations getCellDecor(
                int row, int column, Object value) {

            Triplet triplet = getElement(row);
            SWGRCWPair rcw = triplet.rcwPair;
            SWGResourceClass rc = rcw.rc();

            ZString z = new ZString("For >> ").app(rc.rcName()).app(' ');
            rcw.filter().toString(z, false);
            String tt = z.toString();

            if (column <= 1)
                return new TableCellDecorations(null, null, tt, (Object[]) null);

            if (column <= 12) { // stats capRes.max(j));
                Stat s = gOrder[column - 2];
                int v = ((Integer) value).intValue();
                return new TableCellDecorations(
                        SWGResourceStatRenderer.getStatBackGround(v, rc.max(s)),
                        SWGResourceStatRenderer.getStatForeground(v, rc.max(s)),
                        tt, (Object[]) null);
            }
            if (column == 13) { // rate
                int i = ((Integer) value).intValue();
                return new TableCellDecorations(
                        SWGResourceStatRenderer.getStatBackGround(i / 1000.0),
                        SWGResourceStatRenderer.getStatForeground(i / 1000.0),
                        tt, (Object[]) null);
            }
            if (column == 14) { // % improvement
                Color fg = null, bg = null;
                if (value != null) {
                    double d = ((Double) value).doubleValue();
                    if (d > 0.15) {
                        bg = SWGGuiUtils.statColors[4];
                        fg = SWGGuiUtils.statColors[5];
                    } else if (d > 0.1) {
                        bg = SWGGuiUtils.statColors[2];
                        fg = SWGGuiUtils.statColors[3];
                    } else if (d > 0.05) {
                        bg = SWGGuiUtils.statColors[0];
                        fg = SWGGuiUtils.statColors[1];
                    }
                } else if (great.isSelected()) {
                    bg = SWGGuiUtils.colorLightGray;
                    fg = Color.WHITE;
                } else {
                    bg = Color.CYAN;
                    fg = Color.WHITE;
                }
                return new TableCellDecorations(bg, fg, tt, (Object[]) null);
            }
            if (column == 15) { // age
                long l = ((Long) value).longValue();
                return SWGResController.resourceAgeDecor(triplet.current, l, tt);
            }
            return null;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex <= 1) return String.class;
            if (columnIndex == 14) return Double.class;
            if (columnIndex == 15) return Long.class;
            return Integer.class;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        /**
         * Returns the element of this model specified by the argument. If this
         * model is empty this method returns {@code null}.
         * 
         * @param row a row index
         * @return an element for the index, or {@code null}
         * @throws IndexOutOfBoundsException if the index is out of bounds
         */
        private Triplet getElement(int row) {
            return (elements != null)
                    ? elements.get(row)
                    : null;
        }

        public int getRowCount() {
            return elements != null
                    ? elements.size()
                    : 0;
        }

        
        public Object getValueAt(int rowIndex, int columnIndex) {
            Triplet triplet = getElement(rowIndex);
            if (triplet == null) return null;

            switch (columnIndex) {
            case 0:
                return triplet.current.getName();
            case 1:
                return triplet.current.rc().rcName();
            case 2: // fall-through
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12: { // stats
                Stat s = gOrder[columnIndex - 2];
                return Integer.valueOf(triplet.current.stats().value(s));
            }
            case 13:
                return Integer.valueOf((int) triplet.rateCurr); // rate
            case 14: { // better than...
                double w = triplet.better();
                return w >= 0.0 && w < Double.MAX_VALUE
                        ? Double.valueOf(w)
                        : null;
            }
            case 15:
                return Long.valueOf(triplet.current.age()); // age
			default:
				return null;
            }
        }

        /**
         * Sets the elements for this model. If the argument is {@code null} the
         * content for this model is cleared.
         * 
         * @param elements a list of triplets, or {@code null}
         */
        private void setElements(List<Triplet> elements) {
            this.elements = elements;
            super.fireTableDataChanged();
        }
    } // table model

    /**
     * This type is a light weight triplet for an entry in the table of current
     * resources that are better than the resource inventory. An instance has
     * members that provides immutable references the {@link SWGRCWPair} that is
     * the base for the element, the best resource in inventory that the element
     * relates and compares itself with, and a current resource that is better
     * than inventory. For a particular {@link SWGRCWPair}, if several resources
     * surpasses the inventory there is one instance of this type per current
     * resource. If there is no resource in the inventory for the {@code
     * SWGRCWPair} that reference is {@code null}.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    static class Triplet {

        /**
         * The current resource that is better than anything in inventory.
         */
        final SWGKnownResource current;

        /**
         * The inventory instance that is surpassed by a current resource. If no
         * entry exists in the resource inventory this member is {@code null}.
         */
        final SWGInventoryWrapper inventory;

        /**
         * The rate of the current resource computed from the weight and
         * resource class from {@link #rcwPair}. If {@link #rcwPair} is for a
         * low-quality resource this value is 0.0.
         */
        final double rateCurr;

        /**
         * The rate of the inventory resource computed from the weight and
         * resource class from {@link #rcwPair}. If {@link #inventory} is
         * {@code null}, or if {@link #rcwPair} is for a low-quality resource,
         * this value is 0.0.
         */
        final double rateInv;

        /**
         * The resource-class-and-weight that is the base for this instance.
         */
        final SWGRCWPair rcwPair;

        /**
         * Creates an instance of this type.
         * 
         * @param rcwp the {@link SWGRCWPair} that is the base for this instance
         * @param curr the current resource that is better than the inventory
         * @param inv the inventory entry, or {@code null}
         */
        Triplet(SWGRCWPair rcwp, SWGKnownResource curr, SWGInventoryWrapper inv) {
            this.current = curr;
            this.inventory = inv;
            this.rcwPair = rcwp;

            rateCurr = (rcwp.filter() instanceof SWGWeights)
                    ? ((SWGWeights) rcwPair.filter()).rate(
                            current, rcwPair.rc(), true)
                    : 0.0;
            rateInv = (inv != null && rcwp.filter() instanceof SWGWeights)
                    ? ((SWGWeights) rcwPair.filter()).rate(
                            inv.getResource(), rcwPair.rc(), true)
                    : 0.0;
        }

        /**
         * Returns how much better this instance's current resource is compared
         * to inventory. The returned value is {@code rateCurr/ rateInv} with
         * the following exceptions:
         * <p>
         * {@code if rateCurr == 0} this method returns 0.0 <br/>
         * {@code else if rateInv == 0} this method returns 9.999
         * 
         * @return a better-than ratio for the current resource
         */
        double better() {
            if (inventory == null) return Double.MAX_VALUE;
            if (rateCurr > 0 && rateInv > 0)
                return (rateCurr / rateInv) - 1.0;
            return 0.0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof Triplet) {
                Triplet ot = (Triplet) obj;
                return current == ot.current
                        && inventory == ot.inventory
                        && rcwPair.equals(ot.rcwPair);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return current.hashCode()
                    + inventory.hashCode() + rcwPair.hashCode();
        }
    }
}
