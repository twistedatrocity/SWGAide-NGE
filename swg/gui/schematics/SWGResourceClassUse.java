package swg.gui.schematics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import swg.SWGAide;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceClassTree;
import swg.crafting.resources.SWGResourceFilter;
import swg.crafting.schematics.SWGExperimentGroup;
import swg.crafting.schematics.SWGExperimentLine;
import swg.crafting.schematics.SWGProfession;
import swg.crafting.schematics.SWGProfessionLevel;
import swg.crafting.schematics.SWGResourceSlot;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGListCellRenderer;
import swg.gui.resources.SWGResController;
import swg.model.SWGCGalaxy;
import swg.tools.ZString;
import swg.tools.ZWriter;

/**
 * This type is a GUI element which displays the resource class use for a
 * selected profession. This type also works the other way around, for a
 * selected resource class it displays which schematics it is useful for. The
 * displayed results can be filtered to limit the display in a sensible way.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGResourceClassUse extends JPanel {

    /**
     * A flag which the user toggles on to include show <i>all </i> schematics
     * that can make use of a selected resource class.
     */
    private boolean displayMoreSchems;

    /**
     * A flag which determines if the tree of resource classes should expand
     * (default), or collapse. This member is set by the user, only for the
     * session.
     */
    private boolean expandTree = true;

    /**
     * The URL for the resource class use help page. This is the page which is
     * displayed when a sub-panel does not have its own help pages.
     */
    private final URL helpPage;

    /**
     * A flag which indicates if creation of this element is complete or if it
     * is yet a stub.
     */
    private boolean isGuiFinished;

    /**
     * A flag which denotes if the GUI is currently updating and possible events
     * should be ignored.
     */
    private boolean isGuiUpdating;

    /**
     * A combo box for the professions to select from.
     */
    private JComboBox<Object> professionBox;

    /**
     * A list which displays resource classes which are called for by name by
     * schematics for the selected profession or by the selected schematic.
     * <p>
     * Similarly to {@link #resClassTree} this list displays the resource
     * classes which are used by the selected profession or schematic. However,
     * this list only displays the resource classes called for <i>by name</i>,
     * not possible sub-classes.
     */
    private JList<SWGResourceClass> resClassList;

    /**
     * A list model for the list of resource classes.
     * <p>
     * <b>Notice:</b>The underlying content of this element is identical as for
     * {@link #resClassTreeModel}, hence clients are referred to that model to
     * obtain data.
     */
    private ResClassListModel resClassListModel;

    /**
     * A tree for resource classes which can be used by a selected profession or
     * schematic. This tree is dynamically updated whenever the user makes a
     * selection or changes a filter.
     * <p>
     * Similarly to {@link #resClassList} this tree displays the resource
     * classes that are used by the selected profession or schematic. However,
     * this tree also displays sub-classes which are possible to use in the place
     * of the classes for by name.
     */
    private JTree resClassTree;

    /**
     * A tree model for the resource classes.
     */
    private ResClassTreeModel resClassTreeModel;
    
    /**
     * A flag that denotes if the list of schematics should display HQ, LQ, or
     * all schematics. The default value is 0 which denotes all schematics, a
     * negative values denotes LQ, and a positive value denotes HQ.
     */
    private int schematicsDisplayHqLqAll = 0;

    /**
     * A list which displays schematics that makes use of a specified resource
     * class.
     */
    private JList<Object> schematicsList;

    /**
     * A list model for the list of schematics.
     */
    private SchemListModel schematicsListModel;

    /**
     * A label for experiment weights of a schematic.
     */
    private JLabel schematicWeights;

    /**
     * The container for this GUI element, the schematic tab.
     */
    private SWGSchematicTab schemTab;

    /**
     * The resource class which is most recently selected. During an action
     * event this is the previous class which may be different than one that is
     * "in the air".
     */
    private SWGResourceClass selectedRC;

    /**
     * Creates an instance of this GUI element. This constructor creates just a
     * stub of this type, its content is complemented lazily on demand.
     * 
     * @param parent the container of this element
     */
    SWGResourceClassUse(SWGSchematicTab parent) {
        this.schemTab = parent;

        helpPage = SWGAide.class.getResource(
                "docs/help_schematics_res_class_use_en.html");

        parent.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                focusGained();
            }
        });
    }

    /**
     * Called when the user toggles on/off the popup menu option to display also
     * schematics which can make use of s selected resource class, or to display
     * only schematics that specifically call for a selected class.
     * 
     * @param b {@code true} to display more schematics, a relaxed selection
     */
    private void actionDisplayMoreSchematics(boolean b) {
        displayMoreSchems = b;
        SWGFrame.getPrefsKeeper().add(
                "schemRCUseRelaxSchematicsView", Boolean.valueOf(b));

        int sel = schematicsList.getSelectedIndex();
        if (sel >= 0)
            schematicsList.setSelectedIndex(sel);
        else {
            // ignore selection in tree
            // if not required and -> off is OK, otherwise always OK
            SWGResourceClass s = resClassList.getSelectedValue();
            actionResClassSelected(s, false);
        }
    }

    /**
     * Called when the user selects a profession, or "All", at the profession
     * chooser element. This method obtains a list of resource classes which the
     * specified profession makes use of in one schematic or anther. The list is
     * then used to form an inheritance tree for resource classes. If the
     * argument is {@code null} this method clears the GUI.
     * 
     * @param p a profession, or {@code null}
     */
    private void actionProfessionSelected(SWGProfession p) {
        TreePath path = resClassTree.getSelectionPath();
        schematicsListModel.setContent(null);
        schematicWeights.setText("");
        selectedRC = null;

        if (p == null) {
            setResClassContent(null);
            return;
        }
        SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        List<SWGSchematic> sl = SWGSchematicsManager.getSchematics(p,gxy);
        List<SWGResourceClass> wanted = SWGSchematicsManager.getResClasses(sl);
        setResClassContent(wanted);
        
        resClassTree.setSelectionPath(path);
    }

    /**
     * Called when the user clicks the list of resource classes. If it is a
     * right-click this method displays a popup dialog.
     * 
     * @param e the event that triggers the call
     */
    private void actionResClassListMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            boolean enable = professionBox.getSelectedIndex() >= 0
                    && !"All".equals(professionBox.getSelectedItem())
                    && resClassTreeModel.elementCount() > 0;
            boolean guardHide = ((Boolean) SWGFrame.getPrefsKeeper().get(
                            "schemRCHideForExistingGuard",
                            Boolean.TRUE)).booleanValue();

            SWGResourceClass rc;
            int row = resClassList.locationToIndex(e.getPoint());
            rc = resClassListModel.getElementAt(row);

            SWGWeights wgs = guardWeights();

            if (resClassList.getSelectedIndex() != row)
                resClassHandlePopup(rc, rc);

            JPopupMenu popup = new JPopupMenu();

            popup.add(SWGResController.inventoryFilterMenu(
                    rc, null, null, false));
            popup.add(SWGResController.currentFilterMenu(rc,
                    schematicsList.isSelectionEmpty()
                            ? null
                            : wgs, null));
            popup.addSeparator();

            JMenuItem m;
            m = SWGResController.guardPlainMenu(rc, this, guardNotes());
            m.setEnabled(!(guardHide && SWGResController.guardExist(
                    rc, new SWGResourceFilter(Stat.OQ, 1), null)));
            popup.add(m);

            m = SWGResController.guardQualityMenu(rc, wgs, this, guardNotes());
            m.setEnabled(wgs != null &&
                    !(guardHide && SWGResController.guardExist(rc, wgs, null)));
            popup.add(m);

            popup.add(SWGSchematicTab.guardDisableOptionsMenu(
                    guardHide, "schemRCHideForExistingGuard"));

            popup.addSeparator();

            popup.add(makeMenuWriteList(enable));

            popup.show(resClassList, e.getX(), e.getY());
        }
    }

    /**
     * Called when the user selects a node at the resource class tree. This
     * method updates the list of schematics for which the selected resource
     * class is useful, either directly or indirectly.
     * 
     * @param rc a node in the resource class tree
     * @param unnamed {@code true} to allow selection of unnamed class
     * @throws NullPointerException if the argument is {@code null}
     */
    private void actionResClassSelected(SWGResourceClass rc, boolean unnamed) {
        if (isGuiUpdating || rc == selectedRC) return;
        isGuiUpdating = true;

        SWGResourceClass temp = selectedRC;
        SWGResourceClass list = null;
        SWGResourceClass tree = rc;
        selectedRC = rc;

        if (resClassTreeModel.elementListed(selectedRC))
            list = selectedRC;
        else if (unnamed) {
            list = selectedRC;
            do {
                list = SWGResourceClassTree.getParent(list);
            } while (!resClassTreeModel.elementListed(list));
            resClassList.setSelectedValue(list, true);
        } else if (displayMoreSchems) {
            resClassList.clearSelection();
        } else
            list = tree = selectedRC = temp;

        TreePath tp = resClassTreeModel.elementPath(
                tree, unnamed | displayMoreSchems);
        resClassTree.setSelectionPath(tp);
        resClassTree.scrollPathToVisible(tp);

        resClassList.setSelectedValue(list, true);

        schematicsSet(selectedRC);

        isGuiUpdating = false;
    }
    
    /**
     * Called when the user clicks the tree of resource classes. If it is a
     * right-click this method displays a popup dialog.
     * 
     * @param e the event that triggers the call
     */
    private void actionResClassTreeMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            SWGResourceClass rcl = null;
            SWGResourceClass rct = null;
            TreePath p;
            p = resClassTree.getClosestPathForLocation(e.getX(), e.getY());
            if (p != null) {
                rct = (SWGResourceClass) p.getLastPathComponent();
                for (int i = p.getPathCount() - 1; i >= 0; --i) {
                    rcl = (SWGResourceClass) p.getPathComponent(i);
                    if (resClassTreeModel.elementListed(rcl)) break;
                }
            }
            resClassHandlePopup(rcl, rct);

            popup.add(SWGResController.inventoryFilterMenu(
                    rct, null, null, false));
            popup.add(SWGResController.currentFilterMenu(rct, null, null));
            popup.addSeparator();

            popup.add(makeMenuDisplayMoreSchems());
            popup.add(makeMenuTreeFolding());

            popup.show(resClassTree, e.getX(), e.getY());
        }
    }

    /**
     * Called when the user mouse-clicks the list of schematics. If it is a
     * left-click this method selects the schematics at other panels, if not
     * opted against that. If it is a right-click this method displays a popup
     * dialog.
     * 
     * @param e the event that triggers the call
     */
    private void actionSchematicMouse(MouseEvent e) {
        int em = e.getButton();
        if (em == MouseEvent.BUTTON1) {
            int row = schematicsList.getSelectedIndex();
            if (row < 0) return;
            SWGSchematic s = schematicsListModel.element(row);
            schemTab.schematicSelect(s, this);
        } else if (em == MouseEvent.BUTTON3) {
            schematicsPopup(e);
        }
    }

    /**
     * Called when the user selects a schematic. This method updates the list
     * and the tree with the resource classes which the specified schematic
     * calls for.
     * 
     * @param schem a schematic
     * @throws NullPointerException if the argument is {@code null}
     */
    private void actionSchematicSelected(SWGSchematic schem) {
        schematicWeights.setText(getWeight(schem));

        if (isGuiUpdating && schem == null) return;

        List<SWGResourceClass> rcl = null;
        if (schem != null) {
            rcl = new ArrayList<SWGResourceClass>();
            for (SWGResourceSlot rs : schem.getResourceSlots())
                if (!rcl.contains(rs.getResourceClass()))
                    rcl.add(rs.getResourceClass());
        }
        setResClassContent(rcl);
    }

    /**
     * Called when the user selects to write resource classes to file. This
     * method creates or overwrites a file "crafting/PROF_res_class_use.txt" for
     * the currently selected profession. Only the content of the list is
     * written, not possible sub-classes which are displayed at the resource
     * tree. If there is an error it is caught and a message is written to
     * SWGAide's log file.
     */
    private void actionWriteResourceClasses() {
        String p = (String) professionBox.getSelectedItem();
        String fn = SWGProfession.getFromName(p).getName();
        File f = new File("crafting", fn + "_res_class_use.txt");
        try {
            ZWriter wr = ZWriter.newTextWriterExc(f, false);
            wr.writeExc("# SWGAide: Resource Class Use for ");
            wr.writelnExc(p);
            wr.writelnExc("# Data by swgaide.com");
            wr.eol();

            for (int i = 0; i < resClassTreeModel.elementCount(); ++i) {
                SWGResourceClass rc = resClassTreeModel.element(i);
                wr.writelnExc(rc.rcName());
            }
            wr.eol();
            wr.close();

            fn = "Wrote \"[SWGAide]\\" + fn + "\"";
        } catch (Throwable e) {
            SWGAide.printError("SWGResourceClassUse:writeResClasses", e);
            fn = "Error, see log files";
        }
        JOptionPane.showMessageDialog(this, fn, "Finished",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * This method is invoked when the user selects any one of the tabs in this
     * tabbed pane, or if the collection of assignees is modified in any way. If
     * this element is yet a stub it is populated.
     */
    void focusGained() {
        if (schemTab.frame.getTabPane().getSelectedComponent() == schemTab
                && schemTab.getSelectedComponent() == this) {

            if (!isGuiFinished) make();

            professionBox.requestFocusInWindow();

            SWGHelp.push(helpPage);
            schemTab.updateStatbar2();
            this.repaint();
        } else
            SWGHelp.remove(helpPage);
    }

    /**
     * Helper method which returns a string with experimentation weights for the
     * specified schematic. The string is formatted as a HTML element with
     * weights per line. If the schematic has no experimentation "n/a" is
     * returned.
     * 
     * @param schem a schematic
     * @return a HTML element as string, or {@code null}
     */
    private String getWeight(SWGSchematic schem) {
        if (schem == null || !SWGSchematicsManager.isQuality(schem))
            return "n/a";

        List<SWGWeights> wl = new ArrayList<SWGWeights>();
        List<SWGExperimentGroup> gls = schem.getExperimentGroups();
        for (SWGExperimentGroup g : gls)
            for (SWGExperimentLine l : g.getExperimentalLines())
                if (!wl.contains(l.getWeights()))
                    wl.add(l.getWeights());

        ZString z = new ZString("<html>");
        for (SWGWeights w : wl) {
            w.toString(z, false);
            z.app("<br/>");
        }
        return z.app("</html>").toString();
    }

    /**
     * Helper method which returns a string with schematic names. If a schematic
     * is selected only its name is returned. Otherwise the returned string
     * contains at most the 5 top-most elements from the GUI list of schematics,
     * each at its own line. If there are more than five a note
     * "... and more ..." is appended.
     * 
     * @return a string with schematic names
     */
    private String guardNotes() {
        int sel = schematicsList.getSelectedIndex();
        if (sel >= 0)
            return schematicsListModel.element(sel).getName();

        ZString z = new ZString();
        int i;
        for (i = 0; i < 5 && i < schematicsListModel.getSize(); ++i)
            z.app(schematicsListModel.getElementAt(i)).nl();

        if (i >= 4) z.app("... and more ...");
        return z.toString();
    }

    /**
     * Helper method which creates and returns an instance of resource weights
     * which suits a quality guard, or {@code null} if the schematic has no
     * experiments. This method sets the values for the weights in this fashion,
     * use the experimental values from...
     * <ol>
     * <li>... a schematic in the favorite list; XXX: fix this when better
     * logic for favorites,</li>
     * <li>... the selected schematic; if its title is prioritized,</li>
     * <li>... the selected schematic; the first line,</li>
     * <li>... the first schematic from the list that has experiments.</li>
     * </ol>
     * 
     * @return experimental resource weights, or {@code null}
     */
    private SWGWeights guardWeights() {
        // XXX: the favorite list support

        int sel = schematicsList.getSelectedIndex();
        if (sel >= 0) {
            SWGSchematic s = schematicsListModel.element(sel);
            return guardWeightsPrio(s);
        }

        for (int i = 0; i < schematicsListModel.getSize(); ++i) {
            SWGSchematic s = schematicsListModel.element(i);
            SWGWeights g = guardWeightsPrio(s);
            if (g != null) return g;
        }

        return null;
    }

    /**
     * Helper method which returns an instance of experimental weights, or
     * {@code null}. This method enumerates the experimental groups of the
     * specified schematic and if a prioritized group exists its weight is
     * returned. Otherwise, if only one group exists it is used, or if several
     * groups exist the first group is used (random, huh?). If the schematic is
     * low quality, or if does not have any experimental group, this method
     * returns {@code null}.
     * <p>
     * <b>Limitation</b>: if a group contains more than one experimental lines
     * the first line is used and this method ignores other lines.
     * 
     * @param schem a schematic
     * @return experimental weights, or {@code null}
     */
    private SWGWeights guardWeightsPrio(SWGSchematic schem) {
        if (!SWGSchematicsManager.isQuality(schem)) return null;

        List<SWGExperimentGroup> egl = schem.getExperimentGroups();
        SWGExperimentGroup grp = null;
        if (egl.size() > 1) // several groups
            for (SWGExperimentGroup eg : egl)
                if (SWGSchemController.getPriority(eg, schem) >= 0) {
                    grp = eg;
                    break;
                }
        // else or if none was prioritized
        if (grp == null) grp = egl.get(0); // first group == any group

        SWGExperimentLine el = grp.getExperimentalLines().get(0);
        return new SWGWeights(el.getWeights().values());
    }

    /**
     * Helper method which creates the interior of this GUI element when the
     * user selects this element for the first time. This method must only be
     * called once, from {@link #focusGained()}.
     */
    private synchronized void make() {
        if (isGuiFinished) return; // safety

        this.setLayout(new BorderLayout());
        this.add(makeWest(), BorderLayout.LINE_START);
        this.add(makeCenter(), BorderLayout.CENTER);
        isGuiFinished = true;

        displayMoreSchems = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "schemRCUseRelaxSchematicsView", Boolean.FALSE)).booleanValue();
    }

    /**
     * Helper method which creates and returns the middle element of this panel.
     * This element contains a list of resource classes and a resource class
     * tree.
     * 
     * @return a GUI element
     */
    private Component makeCenter() {
        Box hb = Box.createHorizontalBox();
        hb.add(makeCenterList());
        hb.add(makeCenterTree());
        return hb;
    }

    /**
     * Helper method which creates and returns the center element of this panel.
     * This element contains a list for resource classes which is populated
     * dynamically and in parallel with the resource class tree when the user
     * selects a profession, a schematic, or adjusts a filter.
     * 
     * @return a GUI element
     */
    private Component makeCenterList() {
        resClassListModel = new ResClassListModel();
        resClassList = new JList<SWGResourceClass>(resClassListModel);
        resClassList.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 0));
        
        resClassList.setCellRenderer(new SWGListCellRenderer<SWGResourceClass>(
                SWGGuiUtils.fontBold(), SwingConstants.LEADING, 3) {
            @Override
            protected void colorBackground(JList<? extends SWGResourceClass> list, SWGResourceClass value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                // TODO: make this coloring ignore schematics which are not in
                // the lists of favorites

                SWGResourceClass rc = value;
                if (!SWGResController.inventoryExists(
                        rc, SWGFrame.getSelectedGalaxy(), false)) {
                    label.setBackground(isSelected
                            ? SWGGuiUtils.colorDarker(Color.PINK, 0.9f)
                            : Color.PINK);
                } else
                    super.colorBackground(
                            list, value, index, isSelected, cellHasFocus);
            }

            @Override
            protected String labelString(SWGResourceClass value) {
                return value == null
                        ? ""
                        : value.rcName();
            }
        });

        resClassList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resClassList.addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Object s = resClassList.getSelectedValue();
                    if (s != null)
                        actionResClassSelected((SWGResourceClass) s, false);
                }
            }
        });
        resClassList.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                actionResClassListMouse(e);
            }
        });

        JScrollPane jsp = new JScrollPane(resClassList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        Box bp = new Box(BoxLayout.Y_AXIS) {
            
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                // In %: 100 - 25 = 75 and want about 40% of that => .3
                d.width = (int) (schemTab.getWidth() * .3);
                return d;
            }
        };
        bp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Resource Classes called for by name"));
        bp.add(jsp);

        return bp;
    }

    /**
     * Helper method which creates and returns the east-most element of this
     * panel. This element contains a resource class tree which is populated
     * dynamically when the user selects a profession, a schematic, or adjusts a
     * filter.
     * 
     * @return a GUI element
     */
    private Component makeCenterTree() {
        resClassTreeModel = new ResClassTreeModel();
        resClassTree = new JTree(resClassTreeModel);

        resClassTree.setCellRenderer(new DefaultTreeCellRenderer() {
            
            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean sel, boolean expanded, boolean leaf,
                    int row, boolean hasFocuz) {

                super.getTreeCellRendererComponent(tree, value,
                        sel, expanded, leaf, row, hasFocuz);

                if (resClassTreeModel.elementListed((SWGResourceClass) value))
                    setForeground(Color.BLUE);

                return this;
            }
        });

        resClassTree.addTreeSelectionListener(new TreeSelectionListener() {
            
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath p = resClassTree.getSelectionPath();
                if (p != null && p.getLastPathComponent() != null)
                    actionResClassSelected((SWGResourceClass)
                            p.getLastPathComponent(), false);
            }
        });

        JScrollPane jsp = new JScrollPane(resClassTree,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        MouseListener ml = new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                actionResClassTreeMouse(e);
            }
        };
        resClassTree.addMouseListener(ml);
        jsp.addMouseListener(ml);

        Box bp = new Box(BoxLayout.Y_AXIS) {
            
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                // In % 100 - 25 = 75 and want about 60% of that => .45
                d.width = (int) (schemTab.getWidth() * .45);
                return d;
            }
        };
        bp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Resource Class Tree --- including sub-classes "));
        bp.add(jsp);
        return bp;
    }

    /**
     * Helper method which creates a check-box menu item for how to display
     * schematics for a selected resource class. The user toggles the check-box
     * on/off to display all schematics which can use the selected resource
     * class, or just those that explicitly calls for the class.
     * 
     * @return a menu item
     */
    private JCheckBoxMenuItem makeMenuDisplayMoreSchems() {
        final JCheckBoxMenuItem cb = new JCheckBoxMenuItem(
                "More schematics", displayMoreSchems);
        cb.setToolTipText("Display also schematics that can use selected class");
        cb.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                actionDisplayMoreSchematics(cb.isSelected());
            }
        });

        return cb;
    }
    
    /**
     * Helper method which creates and returns a menu item for expanding or
     * collapsing the tree of resource classes. The inverse of the member
     * {@link #expandTree} determines how to set name and tool-tip text for this
     * menu item, that is, if the tree should expand, or collapse. If the user
     * selects this option {@link #expandTree} is toggled and invokes
     * {@link #resClassTreeExpand()}.
     * 
     * @return a menu item s
     */
    private JMenuItem makeMenuTreeFolding() {
        String title = !expandTree
                ? "Expand tree"
                : "Collapse tree";
        String tt = !expandTree
                ? "Expand the tree to show all relevant nodes"
                : "Collapse the tree -- only a few top nodes remains visible";

        JMenuItem fold = new JMenuItem(title);
        fold.setToolTipText(tt);
        fold.setEnabled(resClassTreeModel.elementCount() > 0);

        fold.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                expandTree = !expandTree; // toggle
                resClassTreeExpand();
            }
        });

        return fold;
    }

    /**
     * Helper method which creates and returns a menu for writing resource
     * classes to file. If the user selects this option the current display at
     * the list of resource classes are written to a file named after the
     * currently selected profession. This menu is disable if no profession is
     * selected, or if "All" is selected, or if the list of resource classes is
     * empty.
     * 
     * @param enable {@code true} if there is a job to perform
     * @return a menu item
     */
    private JMenuItem makeMenuWriteList(boolean enable) {
        JMenuItem write = new JMenuItem("Write to file");
        write.setToolTipText("Write the list of resource classes to file");
        write.setEnabled(enable);

        write.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                actionWriteResourceClasses();
            }
        });

        return write;
    }

    /**
     * Helper method which creates and returns the west-most element of this
     * panel. This element contains a profession chooser and a list of
     * schematics. This list is populated when/if the user selects a resource
     * class at the tree, then with schematics which can use the selected
     * resource class.
     * 
     * @return a GUI element
     */
    private Component makeWest() {
        JPanel wp = new JPanel(new BorderLayout()) {
            
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = schemTab.getWidth() >> 2;
                return d;
            }
        };
        wp.add(makeWestProfessionChooser(), BorderLayout.PAGE_START);
        wp.add(makeWestSchematicsList(), BorderLayout.CENTER);
        wp.add(makeWestWeights(), BorderLayout.PAGE_END);
        return wp;
    }

    /**
     * Helper method which creates and returns a profession chooser, a drop-down
     * list. The component displays all professions and a default option "All".
     * When the user selects a profession the tree of resource classes is
     * updated.
     * 
     * @return a GUI component
     */
    private Component makeWestProfessionChooser() {
    	SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        List<String> pl = SWGProfession.getNames(gxy.getType());
        pl.add("- - - - -");
        String all = pl.get(0);
        pl.set(0, "<select a profession>");
        pl.add(all); // Have "All" last

        professionBox = new JComboBox<Object>(pl.toArray());
        professionBox.setToolTipText(
                "Select a profession updates the resource classes");

        final JButton resetProf = new JButton(" ");
        resetProf.setToolTipText(
                "Reset display to the selected profession -- Alt-X");
        resetProf.setMnemonic(KeyEvent.VK_X);
        resetProf.setEnabled(false);
        resetProf.addActionListener(new ActionListener() {

            
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = (String) professionBox.getSelectedItem();
                SWGProfession p = SWGProfession.getFromName(s);
                actionProfessionSelected(p);
            }
        });

        professionBox.addActionListener(new ActionListener() {

            boolean first = true;

            Object prevProf = null;

            
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = (String) professionBox.getSelectedItem();
                if (s.startsWith("- -")) {
                    if (prevProf == null)
                        return;
                    else if ("Spy".equals(prevProf))
                        s = "All";
                    else if ("All".equals(prevProf))
                        s = "Spy";
                    else
                        s = (String) prevProf;
                    professionBox.setSelectedItem(s);
                } else if (s.startsWith("<select")) return;
                prevProf = s;

                SWGProfession p = SWGProfession.getFromName(s);
                actionProfessionSelected(p);
                resetProf.setText(p.getName());

                if (first) {
                    resetProf.setEnabled(true);
                    professionBox.removeItemAt(0);
                    first = false;
                }
            }
        });

        Box hb = Box.createHorizontalBox();
        hb.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Profession "));
        hb.add(professionBox);
        hb.add(Box.createHorizontalStrut(3));
        hb.add(resetProf);

        Box bp = Box.createVerticalBox();
        bp.add(hb);
        bp.add(Box.createVerticalStrut(15));
        return bp;
    }

    /**
     * Helper method which creates and returns a GUI list which displays
     * schematics that makes use of a specified resource class. This list is
     * populated when the user selects an element at the resource class tree.
     * The schematics are sorted and colored so that direct use of the specified
     * resource class are topmost, then indirect use in any order. Options to
     * filter and limit the selection are always respected.
     * 
     * @return a GUI component
     */
    private Component makeWestSchematicsList() {
        schematicsListModel = new SchemListModel();
        schematicsList = new JList<Object>(schematicsListModel);
        schematicsList.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
        schematicsList.setToolTipText(
                "Schematics which use the selected resource class");

        schematicsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        schematicsList.addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int idx = schematicsList.getSelectedIndex();
                    actionSchematicSelected(idx >= 0
                            ? schematicsListModel.schems.get(idx)
                            : null);
                }
            }
        });
        schematicsList.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                actionSchematicMouse(e);
            }
        });

        JScrollPane jsp = new JScrollPane(schematicsList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        Box bp = Box.createVerticalBox();
        bp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Schematics "));
        bp.setToolTipText("Schematics which use the selected resource class");
        bp.add(jsp);
        return bp;
    }

    /**
     * Helper method which creates and returns a component which displays
     * experiment weights for a selected schematic.
     * 
     * @return a GUI component
     */
    private Component makeWestWeights() {
        schematicWeights = new JLabel("");
        schematicWeights.setFont(SWGGuiUtils.fontPlain());
        schematicWeights.setToolTipText(
                "Experimental weights for the selected schematic, or n/a");
        schematicWeights.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Experimental weights "));
        return schematicWeights;
    }

    /**
     * Helper method which handles selection of resource classes and the list of
     * schematics when a popup dialog is displayed. This method is invoked if
     * the user right clicks a resource class other than what is selected and
     * this implementation selects the class under the mouse.
     * 
     * @param rcl a resource class constant for the res-class list
     * @param rct a resource class constant for the res-class tree
     */
    private void resClassHandlePopup(SWGResourceClass rcl,SWGResourceClass rct) {
        isGuiUpdating = true; // dodge de-selection at schematics list
        resClassList.setSelectedValue(rcl, true);

        TreePath tp = resClassTreeModel.elementPath(rct, true);
        resClassTree.setSelectionPath(tp);
        resClassTree.scrollPathToVisible(tp);

        if (schematicsList.getSelectedIndex() < 0)
            schematicsSet(rcl); // only if no schematic is selected

        isGuiUpdating = false;
    }

    /**
     * Selects the specified resource class at this GUI element. This method
     * first sets any selected profession to "All" to override possible filters,
     * then invokes {@link #actionResClassSelected(SWGResourceClass, boolean)}.
     * If the argument is {@code null} this method does nothing. If this panel
     * is not yet ready created this method invoked {@link #make()}.
     * <p>
     * This method is chiefly invoked from other panels that want to select the
     * resource class at this panel.
     * 
     * @param rc a resource class constant
     */
    void resClassSelect(SWGResourceClass rc) {
        if (rc != null) {
            if (!isGuiFinished)
                make();
            professionBox.setSelectedItem("All");
            actionResClassSelected(rc, true);
        }
    }

    /**
     * Helper method which expands or collapses the tree of resource classes. If
     * {@link #expandTree} this method makes all elements in the tree model
     * visible, but no deeper than that. Otherwise this method collapses the
     * tree so that only the top elements for each major resource class is
     * visible, if the tree was collapsed (usually after an update) this method
     * rather makes only the top elements visible.
     */
    private void resClassTreeExpand() {
        TreePath tp;
        if (expandTree) {
            for (int i = 0; i < resClassTreeModel.elementCount(); ++i) {
                SWGResourceClass rc = resClassTreeModel.element(i);
                tp = resClassTreeModel.elementPath(rc, displayMoreSchems);
                resClassTree.makeVisible(tp);
            }
        } else {
            isGuiUpdating = true;
            resClassTree.clearSelection();
            for (int i = resClassTreeModel.elementCount() - 1; i >= 0; --i) {
                SWGResourceClass rc = resClassTreeModel.element(i);
                tp = resClassTreeModel.elementPath(rc, displayMoreSchems);
                resClassTree.collapsePath(tp);
            }
            isGuiUpdating = false;
        }
    }

    /**
     * Helper method which for the specified resource class returns a sorted
     * list of schematics, or {@code null}. The list contains just one instance
     * of each schematic and each instance calls for the specified resource
     * class by name. The list is sorted alphabetically. If the resource class
     * is not called for by name this method returns {@code null}.
     * <p>
     * XXX: If the user has applied filters this method applies them
     * accordingly.
     * 
     * @param rc a resource class
     * @return a list of schematics
     */
    private List<SWGSchematic> schematics(SWGResourceClass rc) {
        if (rc == null) return null;
        if (!displayMoreSchems &&
                !resClassTreeModel.elementListed(rc)) return null;

        String pn = (String) professionBox.getSelectedItem();
        SWGProfession p = SWGProfession.getFromName(pn);
        SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        List<SWGSchematic> sl = SWGSchematicsManager.getSchematics(p,gxy);
        sl = SWGSchematicsManager.getSchematics(sl, rc, displayMoreSchems);
        sl = SWGSchemController.schematics(sl, schematicsDisplayHqLqAll);

        return sl.isEmpty()
                ? null
                : sl;
    }

    /**
     * Helper method which displays a popup dialog for the list of schematics.
     * 
     * @param e the event that triggers this invocation
     */
    private void schematicsPopup(MouseEvent e) {
        int row = schematicsList.locationToIndex(e.getPoint());
        if (row < 0) return;

        schematicsList.setSelectedIndex(row);
        final SWGSchematic s = schematicsListModel.element(row);

        JPopupMenu popup = new JPopupMenu();
        popup.add(schemTab.schematicSelectMenu(s, this));

        popup.addSeparator();

        ActionListener ac = new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent ee) {
                String c = ee.getActionCommand();
                if (c.equals("Only HQ"))
                    schematicsDisplayHqLqAll = 1;
                else if (c.equals("Only LQ"))
                    schematicsDisplayHqLqAll = -1;
                else if (c.equals("All")) schematicsDisplayHqLqAll = 0;

                SWGResourceClass rc = selectedRC;
                selectedRC = null;
                actionResClassSelected(rc, false);
            }
        };
        int lqAhq = schematicsDisplayHqLqAll;
        ButtonGroup bg = new ButtonGroup();
        SWGSchematicTab.schemRadioButtonMenu("Only HQ", ac, bg, popup, lqAhq);
        SWGSchematicTab.schemRadioButtonMenu("All", ac, bg, popup, lqAhq);
        SWGSchematicTab.schemRadioButtonMenu("Only LQ", ac, bg, popup, lqAhq);

        popup.show(schematicsList, e.getX(), e.getY());
    }

    /**
     * Helper method which updates the list of schematics and the text field for
     * experimental weights based on the specified resource class. If the
     * argument is {@code null} this method clears these elements.
     * 
     * @param rc a resource class constant
     */
    private void schematicsSet(SWGResourceClass rc) {
        List<SWGSchematic> schems = schematics(rc);
        schematicsListModel.setContent(schems);
        schematicWeights.setText(schems != null && schems.size() == 1
                ? getWeight(schems.get(0))
                : "");
    }

    /**
     * Helper method which sets the content for the list and the tree of
     * resource classes. If the argument is {@code null} the content is cleared.
     * Finally this method invokes {@link #resClassTreeExpand()}.
     * 
     * @param resClasses a list of resource classes, or {@code null}
     */
    private void setResClassContent(List<SWGResourceClass> resClasses) {
        resClassListModel.setContent(resClasses);
        resClassTreeModel.setElements(resClasses);
        if (resClasses != null)
            resClassTreeExpand();
    }

    /**
     * A model for the list of resource classes.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class ResClassListModel extends AbstractListModel<SWGResourceClass> {

        /**
         * A list of resource classes for this model. This list is updated when
         * the user makes selections at the GUI.
         */
        List<SWGResourceClass> elems = Collections.emptyList();

        /**
         * Creates an instance of this model for resource classes.
         */
        ResClassListModel() {
            super();
        }

        @Override
        public SWGResourceClass getElementAt(int index) {
            return (elems.isEmpty())
                    ? null
                    : elems.get(index);
        }

        @Override
        public int getSize() {
            return elems.size();
        }

        /**
         * Sets new content for this model, or clears it. This invocation
         * triggers a refresh of the GUI, hence the call must be done at the
         * event thread. If the argument is {@code null} the GUI content is
         * cleared.
         * 
         * @param resClasses a list of resource classes, or {@code null}
         */
        
        void setContent(List<SWGResourceClass> resClasses) {
            resClassList.clearSelection();
            if (resClasses == null)
                this.elems = Collections.emptyList();
            else
                this.elems = resClasses;
            fireContentsChanged(this, 0, getSize());
        }
    }

    /**
     * A tree model for the resource class tree.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class ResClassTreeModel implements TreeModel {

        /**
         * A list of resource classes for this model. This list is replaced when
         * the user selects a profession, schematic, or adjusts the filters.
         */
        private List<SWGResourceClass> elems;

        /**
         * A list of tree listeners.
         */
        private final Vector<TreeModelListener> listeners;

        /**
         * A list of resource classes connected to the root. This list is
         * replaced when the user selects a profession, schematic, or adjusts
         * the filters, see {@link #setRootChildren(List)}.
         */
        private List<SWGResourceClass> rootChildren;

        /**
         * Creates an instance of this model.
         */
        ResClassTreeModel() {
            elems = Collections.emptyList();
            rootChildren = Collections.emptyList();
            listeners = new Vector<TreeModelListener>();
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            if (!listeners.contains(l))
                listeners.add(l);
        }

        /**
         * Returns a resource class at the specified index. This is a resource
         * class which is called for by name for the current selection. The
         * instance may or may not be a child of the root.
         * 
         * @param index an index
         * @return a resource class
         */
        SWGResourceClass element(int index) {
            return elems.get(index);
        }

        /**
         * Returns the number of the resource classes listed by this model. This
         * is the number of resource classes which are called for by name for
         * the current selection. The count may be greater than root's child
         * count.
         * 
         * @return number of resource classes, always &ge; 0
         */
        int elementCount() {
            return elems.size();
        }

        /**
         * Returns {@code true} if this model contains the specified resource
         * class. This means that the argument is one of the elements which is
         * listed for the selected profession or schematic, it is listed by
         * name and is not just a sub-class of a listed resource class.
         * 
         * @param rc a resource class
         * @return {@code true} if this model lists the argument
         */
        boolean elementListed(SWGResourceClass rc) {
            return elems.contains(rc);
        }

        /**
         * Returns a tree-path for the specified resource class, or {@code null}
         * if the argument is not an element of this model. The boolean argument
         * determines if this method should return a path to the specified
         * resource class also if it is not listed in this model, if {@code
         * false} only contained classes are allowed and this method may return
         * {@code null}.
         * 
         * @param rc a resource class
         * @param alien {@code true} for classes not listed in this model
         * @return a tree-path, or {@code null}
         */
        TreePath elementPath(SWGResourceClass rc, boolean alien) {
            if (rc == null
                    || rc == SWGResourceClassTree.getRoot()
                    || (!alien && !elems.contains(rc)))
                return null;

            List<SWGResourceClass> tmp = new ArrayList<SWGResourceClass>();
            tmp.add(rc);

            SWGResourceClass t = rc;
            while (!rootChildren.contains(t)) {
                t = SWGResourceClassTree.getParent(t);
                tmp.add(t);
            }

            tmp.add((SWGResourceClass) getRoot());

            Collections.reverse(tmp);
            return new TreePath(tmp.toArray());
        }

        @Override
        public Object getChild(Object p, int idx) {
            if (p == getRoot())
                return rootChildren.get(idx);

            return SWGResourceClassTree.getChild((SWGResourceClass)
                    p, idx);
        }

        @Override
        public int getChildCount(Object p) {
            if (elems.isEmpty()) return 0;

            if (p == getRoot())
                return rootChildren.size();

            return SWGResourceClassTree.getChildCount((SWGResourceClass) p);
        }

        @Override
        public int getIndexOfChild(Object p, Object c) {
            if (elems.isEmpty()) return -1;

            if (p == getRoot())
                return rootChildren.indexOf(c);

            if (p instanceof SWGResourceClass && c instanceof SWGResourceClass)
                return SWGResourceClassTree.getIndexOfChild(
                        (SWGResourceClass) p, (SWGResourceClass) c);

            return -1;
        }

        @Override
        public Object getRoot() {
            return SWGResourceClassTree.getRoot();
        }

        @Override
        public boolean isLeaf(Object node) {
            SWGResourceClass rc = ((SWGResourceClass) node);
            return (rc.isSpawnable() || rc.isSpaceOrRecycled());
        }

        /**
         * Helper method which notifies all listeners about changes to this
         * model.
         */
        private void notifyListeners() {
            Object root = getRoot();
            TreeModelEvent evt = new TreeModelEvent(this, new TreePath(root));
            for (TreeModelListener listener : listeners)
                listener.treeStructureChanged(evt);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }

        /**
         * Replaces the content for this model with the resource classes the
         * current selection calls for. This method also triggers the GUI to
         * become updated. If the argument is {@code null} the tree of resource
         * classes is emptied.
         * 
         * @param resClasses a list of resource classes, or {@code null}
         */
        
        void setElements(List<SWGResourceClass> resClasses) {
            resClassTree.clearSelection();

            elems = resClasses;
            if (elems == null)
                elems = Collections.emptyList();

            setRootChildren(elems);

            notifyListeners();
        }

        /**
         * Helper method which populates {@link #rootChildren}. This method
         * determines which of the elements in the specified list that inherit
         * another and only those elements that form a root of the sub-trees are
         * added to this special list which is used by the root of the tree
         * modeled by this type.
         * 
         * @param list a list of resource class, or an empty list
         */
        private void setRootChildren(List<SWGResourceClass> list) {
            if (list.isEmpty()) {
                rootChildren = Collections.emptyList();
                return;
            }

            rootChildren = new ArrayList<SWGResourceClass>();

            SWGResourceClass rootChild = null;
            for (int i = 0; i < list.size(); ++i) {
                SWGResourceClass e = list.get(i);

                boolean contained = false;
                for (SWGResourceClass k : rootChildren) {
                    if (e.isSub(k)) {
                        contained = true;
                        break;
                    }
                }

                if (!contained) {
                    rootChild = e;

                    // find the parent class for rootChild
                    for (int j = 0; j < list.size(); ++j) {
                        SWGResourceClass f = list.get(j);
                        if (rootChild.isSub(f))
                            rootChild = f; // step up
                    }

                    rootChildren.add(rootChild);
                    rootChild = null; // reset
                }
            }

            Collections.sort(rootChildren);
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            // pass
        }
    }

    /**
     * A model for the list of schematics.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SchemListModel extends AbstractListModel<Object> {

        /**
         * A list of schematics for this model. This list is updated when the
         * user makes selections at the GUI.
         */
        private List<SWGSchematic> schems;

        /**
         * Creates an instance of this model for schematics.
         */
        SchemListModel() {
            super();
        }

        /**
         * Returns a schematic at the specified index, or {@code null}.
         * 
         * @param index an index
         * @return a schematic
         * @throws IndexOutOfBoundsException if {@code index} is invalid
         */
        SWGSchematic element(int index) {
            return (schems != null)
                    ? schems.get(index)
                    : null;
        }

        @Override
        public Object getElementAt(int index) {
            return (schems != null)
                    ? schems.get(index).getName()
                    : null;
        }

        @Override
        public int getSize() {
            return (schems != null)
                    ? schems.size()
                    : 0;
        }

        /**
         * Sets new content for this model, or clears it. This invocation
         * triggers a refresh of the GUI, hence the call must be done at the
         * event thread. If the argument is {@code null} the GUI content is
         * cleared.
         * 
         * @param schems a list of schematics, or {@code null}
         */
        
        void setContent(List<SWGSchematic> schems) {
            schematicsList.clearSelection();
            this.schems = schems;
            fireContentsChanged(this, 0, getSize());
        }
    }
}
