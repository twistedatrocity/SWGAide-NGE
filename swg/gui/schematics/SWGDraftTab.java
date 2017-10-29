package swg.gui.schematics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import swg.SWGAide;
import swg.crafting.Quality;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.schematics.SWGCategory;
import swg.crafting.schematics.SWGComponentSlot;
import swg.crafting.schematics.SWGExperimentGroup;
import swg.crafting.schematics.SWGExperimentLine;
import swg.crafting.schematics.SWGResourceSlot;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.crafting.schematics.SWGSchematicsManager.ResourceAmount;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJDialog;
import swg.gui.resources.SWGResController;
import swg.gui.schematics.SWGSchematicTreeModel.TNode;
import swg.model.SWGCharacter;
import swg.model.SWGNotes;
import swg.model.SWGProfession;
import swg.model.SWGProfessionLevel;
import swg.model.SWGStation;
import swg.tools.SpringUtilities;
import swg.tools.ZHtml;
import swg.tools.ZNumber;
import swg.tools.ZStack;
import swg.tools.ZString;

/**
 * This GUI element displays Draft Schematics with the characteristics they have
 * in SWG. Draft Schematics are awarded a character as he levels up a
 * profession, or it is a reward or comes from a looted item which has an option
 * to "learn" the schematic. This implementation displays schematics as they are
 * provided from SWGCraft.org, but it does not support editing and uploading
 * corrected/complemented data.
 * <p>
 * Schematics have several different attributes which are encompassed by
 * {@link SWGSchematic}, all of these attributes are displayed at this element.
 * If an attribute is unknown that is clearly marked at the display.
 * <p>
 * The west section is used to list, filter, and select schematics. The east
 * section displays the attributes of the selected schematic. In a future it
 * would be possible to add an image-display area, very much like the first
 * window at a crafting tool, this is not implemented at this point.
 * <p>
 * This type is not instantiated until the user selects the tab named
 * "Schematics" but then it is instantiated in full, there is no lazy creation
 * for this element as it is the first thing the user sees.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
class SWGDraftTab extends JSplitPane implements ClipboardOwner {

    /**
     * A list of schematics. The list is filtered when the user selects a
     * profession or a profession level.
     */
    private static List<SWGSchematic> filteredSchematics;

    /**
     * An assignee that is previously used for assigning schematics.
     */
    private SWGSchematicAssignee assignee;

    /**
     * A mutable constant for the width of text at the middle GUI element,
     * default is 230 pixels.
     */
    private int DRAFT_WIDTH = 250;

    /**
     * A text label for the category and it parent category of a schematic.
     */
    private JLabel draftCategory;

    /**
     * A label on which to display header data for a draft schematic.
     */
    private JLabel draftData;

    /**
     * A list of labels that display experimentation groups with its data.
     */
    private ArrayList<JLabel> draftExpGroups;

    /**
     * A panel for the labels that display experimentation groups. The labels
     * are contained in {@link #draftExpGroups}. When the user selects a
     * schematic the text at the labels are updated or set to {@code null}.
     */
    private JPanel draftExpPanel;

    /**
     * A label on which to display misc data for a draft schematic.
     */
    private JLabel draftMisc;

    /**
     * The panel that displays the data for a draft schematic.
     */
    private JPanel draftPanel;

    /**
     * A list of labels that display resource and component data. When the user
     * selects a schematic the text of the labels are updated or set to {@code
     * null}. Should the schematic require more labels {code
     * addDraftRCLabels(List, JPanel, int)} is invoked and more empty labels are
     * added before the text is appended.
     */
    private List<RCLabel> draftRCLabels;

    /**
     * A panel for the labels that display resource and component data. The
     * labels are contained in {@link #draftRCLabels}. When the user selects a
     * schematic the text at the labels are updated or set to {@code null}.
     */
    private JPanel draftRCPanel;

    /**
     * The titled border which displays the name of the selected schematic.
     */
    private TitledBorder draftTitle;

    /**
     * A GUI element on which to specify the number of items for a factory run.
     */
    private JSpinner factoryAmount;

    /**
     * The max possible factory batch for a schematic, default is 1000. If a
     * batch is limited by the required identical sub-components this amount is
     * reduced to the max possible.
     */
    private int factoryAmountMax;

    /**
     * A label on which to display the estimated time for a factory run.
     */
    private JLabel factoryHours;

    /**
     * A dialog to add favorite schematic for a selected assignee.
     */
    private FavoriteDialog favoriteDialog;

    /**
     * A menu item used in SWGAide's frame, the Edit menu.
     */
    private JMenuItem findMenu;

    /**
     * A string recently searched for, or {@code null}.
     */
    private String findTxt;

    /**
     * A helper flag that denotes if we are updating the GUI or not.
     */
    private boolean isUpdatingGUI;

    /**
     * Menu listeners used in SWGAide's frame, the Options menu.
     */
    private ArrayList<JMenuItem> menuItemsNavigate;

    /**
     * A text area for the notes of the default schematic's wrapper.
     */
    private JTextArea schemNotes;

    /**
     * The model for the combo box to select schematics at.
     */
    private SchemSelectionModel schemSelectModel;

    /**
     * A combo box for selecting schematics sorted in alphabetical order.
     */
    private JComboBox schemSelector;

    /**
     * The tabbed pane that contains this element. This is the component named
     * "Schematics" at SWGAide's main tabbed pane.
     */
    final SWGSchematicTab schemTab;

    /**
     * The GUI tree for categories and schematics.
     */
    private JTree schemTree;

    /**
     * The model for the tree of categories and schematics.
     */
    private SWGSchematicTreeModel schemTreeModel;

    /**
     * A category selected by the user, or {@code null}. This member is not
     * stored in the DAT file, it is only used to keep a tree-path unfolded in
     * the case the user filters so the selected profession is not visible.
     */
    private SWGCategory selectedCategory;

    /**
     * A profession level selected by the user, or the level that was stored in
     * the preference keeper, or the default level which is 90.
     */
    private Integer selectedLevel = Integer.valueOf(90);

    /**
     * A profession selected by the user, or the profession that was stored in
     * the preference keeper, or "All".
     */
    private SWGProfession selectedProfession = SWGProfession.ALL;

    /**
     * A schematic selected by the user, or {@code null}. This member is not
     * stored in the DAT file but its integer ID.
     */
    private SWGSchematic selectedSchematic;

    /**
     * The GUI element for displaying a shopping list.
     */
    private JEditorPane shoppingList;

    /**
     * A button for navigating backwards along the trace the user has made.
     */
    private JButton traceBack;

    /**
     * A button for navigating forward along the trace the user once made.
     */
    private JButton traceForward;

    /**
     * An object that traces the user while browsing schematics, this enables
     * backward and forward browsing buttons.
     */
    private Tracer tracer;

    /**
     * A button for navigating upwards the tree of categories.
     */
    private JButton traceUp;

    
    /**
     * The GUI element for displaying used-in list and find results.
     */
    private JList usedinAndFind;

    /**
     * The list model for the used-in and find-result GUI component.
     */
    private UsedInModel usedinAndFindModel;

    /**
     * Creates an instance of this GUI component.
     * 
     * @param owner the tabbed pane which contains this instance
     */
    SWGDraftTab(SWGSchematicTab owner) {
        super(JSplitPane.HORIZONTAL_SPLIT);

        schemTab = owner;

        tracer = new Tracer();

        make();

        if (schemTreeModel.getRoot() == null)
            JOptionPane.showMessageDialog(
                    owner.frame,
                    "Missing data, probably files\nare not fully downloaded yet",
                    "Missing data", JOptionPane.WARNING_MESSAGE);

        owner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                focusGained();
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void componentResized(ComponentEvent e) {
                actionAdjustWidths();
            }
        });
        getLeftComponent().addComponentListener(new ComponentAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void componentResized(ComponentEvent e) {
                actionAdjustWidths();
            }
        });

    }

    /**
     * Helper method which invokes swing-utilities invoke-later to merge
     * consecutive invocations to a few.
     */
    private void actionAdjustWidths() {
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                // let swing-utilities collate consecutive calls if so...
                adjustWidths();
            }
        });
    }

    /**
     * Called when the user clicks at the east side of the split panel. If it is
     * a right click this method displays a popup dialog with several options
     * determined by the context: if the argument for {@link RCLabel} is not
     * {@code null} the popup menu contains options that are related to the kind
     * of label and its content.
     * 
     * @param e the event that triggers the call
     * @param rcl a {@link RCLabel}, or {@code null}
     */
    @SuppressWarnings("synthetic-access")
    private void actionEastMouse(MouseEvent e, final RCLabel rcl) {
        if (e.getButton() != MouseEvent.BUTTON3) return;

        SWGSchematic s = selectedSchematic;

        JPopupMenu popup = new JPopupMenu();

        if (rcl != null && rcl.slot instanceof SWGResourceSlot) {
            SWGResourceSlot rs = (SWGResourceSlot) rcl.slot;
            SWGResourceClass rc = rs.getResourceClass();
            SWGWeights wg = SWGSchemController.weightsWidest(s);
            popup.add(SWGResController.currentFilterMenu(rc, wg, null));
            popup.add(SWGResController.inventoryFilterMenu(rc, wg, null, false));

            popup.addSeparator();

            String sn = s.getName();
            wg = wg == null
                    ? SWGSchemController.weightsFirst(s)
                    : wg;

            boolean ex = wg == null
                    || SWGResController.guardExist(rc, wg, null);
            JMenuItem m = SWGResController.guardQualityMenu(rc, wg, rcl, sn);
            m.setEnabled(!ex);
            popup.add(m);

            ex = SWGSchematicsManager.isQuality(s)
                    || SWGResController.guardExist(rc, SWGRCWPair.LQ_FILTER, null);
            m = SWGResController.guardPlainMenu(rc, rcl, sn);
            m.setEnabled(!ex);
            popup.add(m);

        } else if (rcl != null && rcl.slot instanceof SWGComponentSlot) {
            final SWGComponentSlot sl = (SWGComponentSlot) rcl.slot;
            SWGCategory cat = SWGSchematicsManager.
                    getCategory(sl.getCategoryId());
            if (sl.getType().equals("item")
                    || SWGSchematicsManager.isSpecial(cat)) {
                popup.add(informationMenu(rcl, sl.getType().equals("item")
                        ? sl.getItemName()
                        : cat.getName()));
            } else
                return; // XXX add options for regular components//categories
        } else { // ---- shopping list options ----

            final int count = factoryAmount();

            JMenuItem copyCB = new JMenuItem("Copy shopping list");
            copyCB.setToolTipText("Copy shopping list");
            copyCB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ee) {
                    copyToClipboard(shoppingList(count));
                }
            });
            popup.add(copyCB);

            boolean ex = SWGFrame.getSelectedCharacter().galaxy().exists();

            JMenuItem owNF = new JMenuItem("Write shopping list");
            owNF.setToolTipText("Write shopping-list to notes file named "
                    + "\"shop\", over-write old");
            owNF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ee) {
                    shoppingListWrite(shoppingList(count), false);
                }
            });
            owNF.setEnabled(ex);
            popup.add(owNF);

            JMenuItem aNF = new JMenuItem("Append shopping list");
            aNF.setToolTipText("Append shopping-list to notes file named \"shop\"");
            aNF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ee) {
                    shoppingListWrite(shoppingList(count), true);
                }
            });
            aNF.setEnabled(ex);
            popup.add(aNF);

            popup.addSeparator();

            JMenuItem ss = schemTab.schematicSelectMenu(s, this);
            ss.setEnabled(s != null);
            popup.add(ss);
        }
        popup.show((Component) e.getSource(), e.getX(), e.getY());
    }

    /**
     * Helper method which displays a dialog with which to select an assignee to
     * which the specified schematic is added.
     * 
     * @param e the event that triggers the call
     * @param schem a schematic
     */
    void actionFavoriteDialog(MouseEvent e, SWGSchematic schem) {
        if (favoriteDialog == null)
            favoriteDialog = new FavoriteDialog();

        List<SWGSchematicAssignee> als = SWGSchematicTab.assignees();
        Collections.sort(als);
        favoriteDialog.show(e, schem, als);
    }

    /**
     * Called when the user makes a selection that triggers an update to the
     * schematics. This method filters {@link #filteredSchematics} based on
     * profession and profession level and to update the GUI it invokes
     * {@link #actionSchematicSelected(SWGSchematic, JButton, boolean)}.
     */
    private void actionFilterSchematics() {
        filteredSchematics = SWGSchematicsManager.getSchematics(
                selectedProfession, -1, selectedLevel.intValue());

        isUpdatingGUI = true;
        schemTreeModel.setSchematics(filteredSchematics);

        schemSelector.setSelectedIndex(-1);
        schemSelectModel.fireContentChanged();

        isUpdatingGUI = false;
        actionSchematicSelected(selectedSchematic, traceForward, false);
    }

    /**
     * Called when the user clicks the notes text field under the shopping list.
     * This method displays a popup dialog where the user can edit the notes.
     * 
     * @param e the event that triggers the call
     */
    private void actionNotes(@SuppressWarnings("unused") MouseEvent e) {
        if (selectedSchematic != null) {
            SWGSchematicWrapper w = SWGSchemController.
                    wrapperDefault(selectedSchematic);

            String n = SWGGuiUtils.dialogTextInput(
                    schemNotes, "Edit schematic notes", w.notes());

            if (n != null) {
                String s = n.trim();
                w.notes(s);
                schemNotes.setText(s);
            }
        }
    }

    /**
     * Called when the user selects a schematic in one way or another and after
     * the list of schematics is filtered. This method stores the ID for the
     * specified schematic in SWGAide's preference keeper and updates the GUI.
     * If the specified schematics cannot be displayed because of user filters,
     * or if the argument is {@code null}, this method does nothing.
     * <p>
     * If the user navigates using the trace buttons, or by clicking other GUI
     * elements, the navigate object is the selected or the appropriate button.
     * If {@code direction == null && selectedSchematic != null} the invocation
     * denotes that a component was clicked and this method tries to use the
     * {@link #factoryAmount} for the selected schematic.
     * 
     * @param schem a schematic, or {@code null}
     * @param direction a trace button, or {@code null}
     * @param overrule {@code true} to overrule filters without popup dialogs
     */
    private void actionSchematicSelected(
            SWGSchematic schem, JButton direction, boolean overrule) {
        if (isUpdatingGUI || schem == null)
            return;
        isUpdatingGUI = true;

        SWGFrame.getPrefsKeeper().add(
                "schemDraftSelectedSchematic", Integer.valueOf(schem.getID()));

        TreePath p = schemTreeModel.pathFromSchematicID(schem.getID());

        int fact = factoryAmount();
        int fc = (direction == null && selectedSchematic != null)
                ? fact * SWGSchematicsManager.schemCompAmount(
                        schem, selectedSchematic)
                : 0;
        boolean isSame = selectedSchematic == schem;
        selectedSchematic = schem;
        if (fc > fact) factoryAmount.setValue(Integer.valueOf(fc));

        if (p != null) {
            schemTree.setSelectionPath(p);
            schemTree.scrollPathToVisible(p);

            selectedCategory = (SWGCategory) ((TNode) p.getParentPath().
                    getLastPathComponent()).getContent();

            if (!isSame)
                display(schem);
        } else if (overrule && !isSame) {
            display(schem);
        } else if (!isSame) {
            // a filter prohibit the display
            String pros = "";
            for (SWGProfessionLevel pl : schem.getSkillLevels())
                pros += pl.getProfession().getNameShort() + '\n';
            if (schem.getExpertise() != null)
                for (Object[] obj : schem.getExpertise()) {
                    pros += ((SWGProfession) obj[0]).getName() + '\n';
                    pros += obj[1].toString() + '\n';
                }
            String msg = String.format("Object is hidden by "
                        + "profession/level filters%n%n%s%n%s%n%n"
                        + "Display details?", schem.getName(), pros);
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                            schemTab.frame, msg, "Object hidden",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE))
                display(schem);
        }

        schemSelector.setSelectedIndex(schemSelectModel.getIndexOf(schem));
        updateTracer(schem, direction);

        schemTab.schematicSelect(schem, this);
        isUpdatingGUI = false;
    }

    /**
     * Called when the user clicks the GUI tree of categories and schematics. If
     * it is a right-click this method displays a popup dialog with options and
     * if necessary the GUI is updated.
     * 
     * @param e the event that triggers the call
     */
    private void actionSchemTreeMouse(final MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON3)
            return;

        JPopupMenu popup = new JPopupMenu();

        String ass = assignee == null
                ? "?"
                : ("\"" + assignee.getName() + "\"");

        boolean bf = false;
        JMenuItem addFavDialog = new JMenuItem("Add to favorite...");
        JMenuItem addFav = new JMenuItem("Add to " + ass);

        JMenuItem addAll = new JMenuItem("Add category to " + ass);
        addAll.setToolTipText("Add schematics under the category to " + ass);
        addAll.setEnabled(false);

        TreePath p = schemTree.getClosestPathForLocation(e.getX(), e.getY());
        JMenuItem select = null;
        if (p != null) {
            schemTree.setSelectionPath(p);
            TNode tn = (TNode) p.getLastPathComponent();
            if (tn.getContent() instanceof SWGSchematic) {
                final SWGSchematic ss = (SWGSchematic) tn.getContent();
                select = schemTab.schematicSelectMenu(ss, this);
                bf = true;

                addFavDialog.setToolTipText(String.format(
                        "Add \"%s\" to favorites", ss.getName()));
                if (assignee != null)
                    addFav.setToolTipText(String.format("Add \"%s\" to \"%s\"",
                            ss.getName(), assignee.getName()));

                addFavDialog.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ee) {
                        actionFavoriteDialog(e, ss);
                    }
                });
                addFav.addActionListener(new ActionListener() {
                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void actionPerformed(ActionEvent ee) {
                        assignee.addFavorite(ss);
                    }
                });
            } else if (tn.getContent() instanceof SWGCategory) {
                SWGCategory ct = (SWGCategory) tn.getContent();

                if (SWGSchematicsManager.isSpecial(ct)) {
                    popup.add(informationMenu(traceUp, ct.getName()));
                    popup.addSeparator();
                    popup.add(makeFindMenu());

                    popup.show((Component) e.getSource(), e.getX(), e.getY());
                    return; // special case
                }

                final List<SWGSchematic> sl = ct.getSchematics();
                addAll.addActionListener(new ActionListener() {
                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void actionPerformed(ActionEvent ee) {
                        for (SWGSchematic el : filteredSchematics)
                            if (sl.contains(el))
                                assignee.addFavorite(el);
                    }
                });
                addAll.setEnabled(assignee != null && !sl.isEmpty());
            }
        }
        if (!bf) {
            addFavDialog.setToolTipText("Not schematic");
            addFav.setToolTipText("Not schematic");
        }
        addFavDialog.setEnabled(bf);
        addFav.setEnabled(bf && assignee != null);

        popup.add(addAll);
        popup.add(addFav);
        popup.add(addFavDialog);

        popup.add(schemTab.assigneeMenu());

        popup.addSeparator();

        popup.add(makeFindMenu());

        if (bf)
            popup.add(select);

        popup.addSeparator();

        // menu option to display also empty schematics
        boolean bh = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "schemDraftHideEmptyNodes", Boolean.TRUE)).booleanValue();

        final JCheckBoxMenuItem hide = new JCheckBoxMenuItem("Hide empty", bh);
        hide.setToolTipText("Select this to hide empty categories");
        hide.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent ee) {
                boolean h = hide.isSelected();
                schemTreeModel.hideEmptyCategoryNodes = h;
                SWGFrame.getPrefsKeeper().add(
                        "schemDraftHideEmptyNodes", Boolean.valueOf(h));
                actionFilterSchematics();
            }
        });
        popup.add(hide);

        popup.show((Component) e.getSource(), e.getX(), e.getY());
    }

    /**
     * Called when the user mouse-clicks a trace-walk button. The source of the
     * argument determines which action to execute: the back and forward button
     * make this method selects a schematic in that direction; the up button
     * makes this method selects a category one level above the current level
     * until the root but not further. To disable or enable the buttons is
     * handled by
     * {@link #actionSchematicSelected(SWGSchematic, JButton, boolean)}.
     * 
     * @param e the event that triggers the call
     */
    private void actionTraceClicked(ActionEvent e) {
        String src = e.getActionCommand();

        if (src.equals("Forward") && tracer.existsForward()) {
            Object o = tracer.peekForward();
            if (o instanceof SWGSchematic)
                actionSchematicSelected((SWGSchematic) o, traceForward, false);
            else if (o instanceof SWGCategory) {
                displayCategoryPath((SWGCategory) o, traceForward);
            }
        } else if (src.equals("Back") && tracer.existsBack()) {
            Object o = tracer.stepBackward();
            if (o instanceof SWGSchematic) {
                SWGSchematic s = (SWGSchematic) o;
                int fc = SWGSchematicsManager.schemCompAmount(
                        selectedSchematic, s);
                if (fc > 0) {
                    int ft = factoryAmount() / fc;
                    if (ft > 0) factoryAmount.setValue(Integer.valueOf(ft));
                }
                actionSchematicSelected(s, traceBack, false);
            } else if (o instanceof SWGCategory) {
                displayCategoryPath((SWGCategory) o, traceBack);
            }
        } else if (src.equals("Up") && selectedCategory != null
                && selectedCategory.getID() > SWGCategory.ALL)
            displayCategoryPath(SWGSchematicsManager.getCategory(
                    selectedCategory.getParentID()), traceForward);
    }

    /**
     * Called when the user mouse-clicks the used-in list. This method displays
     * a popup dialog with options for the list.
     * 
     * @param e the event that triggers the call
     */
    private void actionUsedin(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            JMenuItem copy = new JMenuItem("Copy");
            copy.setToolTipText("Copy all to clip board");
            copy.addActionListener(new ActionListener() {
                @SuppressWarnings("synthetic-access")
                @Override
                public void actionPerformed(ActionEvent ee) {
                    copyToClipboard(usedinAndFindModel.getAsText());
                }
            });
            copy.setEnabled(usedinAndFindModel.getSize() > 0);
            popup.add(copy);

            popup.show(usedinAndFind, e.getX(), e.getY());
        }
    }

    /**
     * Helper method that sets preferred and maximum size for the component to
     * the specified dimension.
     * 
     * @param c a component
     * @param d a dimension
     */
    private void adjustWidth(Component c, Dimension d) {
        c.setPreferredSize(d);
        c.setMaximumSize(d);
    }

    /**
     * Helper method which adjusts the widths of some GUI elements as a reaction
     * when this element is resized.
     */
    private void adjustWidths() {
        isUpdatingGUI = true;
        final int minimumDraft = 250;
        final int minimumEast = 300;
        final int guiSlack = 20;

        int width = this.getSize().width;
        int divider = this.getDividerLocation();

        int min = schemTree.getParent().getParent().getMinimumSize().width;
        int maxDiv = width - minimumDraft - minimumEast - (3 * guiSlack);
        maxDiv = maxDiv < min
                ? min
                : maxDiv;
        if (divider > maxDiv) {
            // disallow divider to far to the right
            setDividerLocation(maxDiv);
            isUpdatingGUI = false;
            return;
        }

        int remaining = width - divider - minimumEast - (2 * guiSlack);

        // default/minimum value for DRAFT_WIDTH is 250
        // minimum text width, allows for 4-stat exp weighs
        DRAFT_WIDTH = remaining < minimumDraft
                // frame minus slack
                ? minimumDraft
                : minimumDraft + (remaining - minimumDraft) / 3;

        Dimension d = new Dimension(DRAFT_WIDTH, 32);
        adjustWidth(draftCategory, d);
        adjustWidth(draftMisc, d);

        isUpdatingGUI = false;

        display(selectedSchematic);
    }

    /**
     * Helper method that copies the specified string to the clip board. If the
     * argument is null this method does nothing.
     * 
     * @param str a string
     */
    private void copyToClipboard(ZString str) {
        StringSelection sel = new StringSelection(str.trimmed());
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(sel, this);
    }

    /**
     * Helper method which updates the display with the content of the specified
     * schematic. This implementation should be invoked when a new schematic is
     * selected by the user. If the argument is {@code null} this method does
     * nothing.
     * 
     * @param schem a schematic
     */
    private void display(SWGSchematic schem) {
        if (schem == null)
            return;

        displayDraft(schem);
        displayShopping(schem);
        displayUsedIn(schem);

        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                Rectangle r = draftPanel.getVisibleRect();
                r.width -= r.x;
                r.x = 0;
                draftPanel.scrollRectToVisible(r);
            }
        });
    }

    /**
     * Helper method which selects and displays the specified category. If the
     * argument is invalid or if there is an error this method does nothing.
     * 
     * @param c a category
     * @param direction the direction for the update
     */
    private void displayCategoryPath(SWGCategory c, JButton direction) {
        TreePath p = schemTreeModel.pathFromCategoryID(c);
        if (p != null) {
            updateTracer(c, direction);
            if (!SWGSchematicsManager.isSpecial(c)
                    && !schemTreeModel.isVisible(
                            ((TNode) p.getLastPathComponent()))) {

                ZString z = new ZString(c.getParentID() == 626
                        ? "Extinct or unknown item"
                        : "Category is hidden or filtered").appnl(":");

                boolean nf = false; // not-first
                for (Object o : p.getPath()) {
                    if (nf) z.app(" - ");
                    nf = true;
                    z.app(o.toString());
                }

                JOptionPane.showMessageDialog(draftRCPanel, z.toString(),
                        "Not visible", JOptionPane.WARNING_MESSAGE);
                return;
            }
            selectedCategory = c;

            schemTree.setSelectionPath(p);
            schemTree.scrollPathToVisible(p);
            schemTree.expandRow(schemTree.getRowForPath(p));

            if (c.getID() > SWGCategory.ALL)
                schemTree.requestFocusInWindow();
        }
    }

    /**
     * Helper method which updates the display for the draft details.
     * 
     * @param schem a schematic
     */
    private void displayDraft(SWGSchematic schem) {
        draftTitle.setTitle(schem.getName());
        draftCategory.setText(draftDataCategory(schem));
        draftData.setText(draftData(schem));
        draftResAndComps(schem);
        draftExpGroups(schem);
        draftMisc.setText(draftMisc(schem));
        draftPanel.repaint();
    }

    /**
     * Helper method which calculates the estimated time for a factory run based
     * on the specified schematics complexity and the value the user selects at
     * the GUI element for factory batch size.
     * 
     * @param schem a schematic
     * @param amount the selected amount
     */
    private void displayFactoryTime(SWGSchematic schem, int amount) {
        int sec = schem.getComplexity() * 8 * amount;
        if (sec <= 0) {
            factoryHours.setText(String.format("<html>%s</html>",
                    SWGSchematicTab.UNKNOWN));
            return;
        }

        String ts;
        if (sec > (3600 * 24) - 1) {
            ts = String.format("%s:%s:%s  d:h:m",
                    ZNumber.asText(sec / (3600 * 24), 1, 0),
                    ZNumber.asText((sec % (3600 * 24)) / 3600, 2, 0),
                    ZNumber.asText((sec % 3600) / 60, 2, 0));
            factoryHours.setToolTipText("days : hours : minutes "
                    + "(no expertise accounted for)");
        } else {
            ts = String.format("%s:%s:%s  h:m:s",
                    ZNumber.asText(sec / 3600, 2, 0),
                    ZNumber.asText((sec % 3600) / 60, 2, 0),
                    ZNumber.asText(sec % 60, 2, 0));
            factoryHours.setToolTipText("hours : minutes : seconds "
                    + "(no expertise accounted for)");
        }
        factoryHours.setText(ts);
    }

    /**
     * Helper method which updates the notes field of the specified schematic,
     * or sets them to "Custom notes". The notes are for the wrapper of the
     * default schematic, currently the only possible choice.
     * 
     * @param s a schematic
     */
    private void displayNotes(SWGSchematic s) {
        String n = SWGSchemController.wrapperDefault(s).notes();
        schemNotes.setText(n == null
                ? "Custom notes"
                : n);
    }

    /**
     * Helper method which updates the shopping list for a schematic.
     * 
     * @param schem a schematic
     */
    private void displayShopping(SWGSchematic schem) {
        int identical = 1;
        for (SWGComponentSlot sl : schem.getComponentSlots())
            if (!sl.isSimilar() && sl.getAmount() > 1)
                identical = Math.max(identical, sl.getAmount());
        factoryAmountMax = 1000 / identical; // int truncation :)
        int count = factoryAmount();
        if (count > factoryAmountMax) {
            factoryAmount.setValue(Integer.valueOf(factoryAmountMax));
            count = factoryAmountMax;
        }

        displayShoppingList(schem, count);
        displayFactoryTime(schem, count);
        displayNotes(schem);
    }

    /**
     * Helper method which updates the shopping list for the specified
     * schematic. The amount of each resource is multiplied with the specified
     * value.
     * 
     * @param schem a schematic
     * @param count the selected amount
     */
    private void displayShoppingList(SWGSchematic schem, int count) {
        String f = "<font face=\"arial,sans-serif\" size=\"-1\">";
        String err = "<font color=\"red\"><b>ERROR</b></font>";
        List<ResourceAmount> ral = shoppingResourcesSorted(schem);
        ZString z = new ZString();
        z.app("<html>");

        if (ral == null) {
            z.app(f).app("<font color=\"red\"><b>ERROR</b></font>");
            z.app(", see log file for details<br/>");
            z.app("Please report the error at SWGCraft</font></html>");
            shoppingList.setText(z.toString());
            return;
        }

        z.app("<table width=\"100%\"border=0 cellpadding=1>");
        z.app("<tr><td>").app(f).app(schem.getName());
        z.app("</font></td><td align=\"right\">").app(f);
        if (count > 1) z.app("Factory batch:  ").
                app(ZNumber.asText(count, true, true));
        z.app("&nbsp;</font></td></tr></table>");

        z.app("<table border=0 cellpadding=1>");
        int total = 0;
        for (ResourceAmount ra : ral) {
            z.app("<tr><td width=\"100\" align=\"right\">").app(f);
            int u = ra.getUnits() * count;
            z.app(ZNumber.asText(u, true, true));
            total += u;
            z.app("</font></td><td>").app(f).app("&nbsp; ");
            z.app(ra.getResourceClass().rcName()).app("</font></td></tr>");
        }
        z.app("<tr><td width=\"100\" align=\"right\">").app(f).app("= ");
        z.app(ZNumber.asText(total, true, true)).app("</font></td></tr>");
        // one empty row
        z.app("<tr><td>&nbsp;</td></tr>");

        int comps = 0;
        List<SWGComponentSlot> csl = schem.getComponentSlots();
        for (SWGComponentSlot cs : csl) {
            if (cs.isOptional()) continue;

            z.app("<tr><td width=\"100\" align=\"right\">").app(f);
            z.app(ZNumber.asText(cs.getAmount() * count, true, true));
            z.app("</font></td><td>").app(f).app("&nbsp; ");

            if (cs.getType().equals("schematic")) {
                SWGSchematic s =
                        SWGSchematicsManager.getSchematic(cs.getSchematicId());
                z.app(s != null
                        ? s.getName()
                        : err);
            } else if (cs.getType().equals("category")) {
                SWGCategory cc =
                        SWGSchematicsManager.getCategory(cs.getCategoryId());
                z.app(cc != null
                        ? cc.getName()
                        : err);
            } else
                z.app(cs.getItemName()); // item

            z.app("</font></td></tr>");
            ++comps;
        }

        for (int i = ral.size() + comps + 1; i < 10; ++i) {
            // pad to sensible size to avoid too much flicker
            z.app("<tr><td>").app(f).app("&nbsp;</font></td>");
            z.app("<td>").app(f).app("&nbsp;</font></td></tr>");
        }
        z.app("</table>");
        z.app(f).app("<font color=#bbbbbb>");
        z.app("Optional and interchangeable components are not included in the");
        z.app("<br/>");
        z.app("shopping list, nor is losses from manufacturing sub-components.");
        z.app("</font></font>").app("</html>");
        shoppingList.setText(z.toString());
    }

    /**
     * Helper method which updates the used-in list for a schematic.
     * 
     * @param schem a schematic
     */
    private void displayUsedIn(SWGSchematic schem) {
        List<SWGSchematic> used = usedIn(schem);
        usedinAndFindModel.setElements(used);
        usedinAndFind.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Helper method which creates and appends labels for experimentation groups
     * to the list and adds them to {@link #draftExpPanel}. The number of labels
     * to create is {@code number - labels.size()}, if this difference is zero
     * or less this method does nothing. The new labels are added by
     * {@link List#add(Object)} and {@link JPanel#add(Component)}.
     * 
     * @param labels the list to append labels to
     * @param number the amount of panels to to create and add
     */
    private void draftAddExpLabels(List<JLabel> labels, int number) {
        for (int i = labels.size(); i < number; ++i) {
            JLabel l = makeLabel();
            labels.add(l);
            draftExpPanel.add(l);
        }
    }

    /**
     * Helper method which creates and appends labels for resources and
     * components to the list and adds them to {@link #draftRCPanel}. The number
     * of labels to create is {@code number - labels.size()}, if this difference
     * is zero or less this method does nothing. The new labels are added by
     * {@link List#add(Object)} and {@link JPanel#add(Component)}.
     * 
     * @param labels the list to append labels to
     * @param number the amount of panels to to create and add
     */
    private void draftAddRCLabels(List<RCLabel> labels, int number) {
        for (int i = labels.size(); i < number; ++i) {
            RCLabel l = new RCLabel();
            labels.add(l);
            draftRCPanel.add(l);
        }
    }

    /**
     * Helper method which returns a string with data from the specified
     * resource slot.
     * 
     * @param slot a resource slot
     * @return the data for the resource slot
     */
    private String draftComp(SWGComponentSlot slot) {
        String d = slot.getDescription();
        String i = draftCompIdentical(slot);
        String n = draftCompName(slot);
        String o = draftCompOptional(slot);
        String s = String.format("<b>%s</b> %s<b>%s</b>%s",
                ZNumber.asText(slot.getAmount(), true, true), i, n, o);

        return draftWidth(d, s);
    }

    /**
     * Helper method which returns a string reading "Similar " or "Identical ".
     * If only one item is called for in this slot the empty string is returned.
     * 
     * @param slot a component slot
     * @return a string
     */
    private String draftCompIdentical(SWGComponentSlot slot) {
        if (slot.getAmount() <= 1)
            return "";
        return slot.isSimilar()
                ? "similar "
                : "identical ";
    }

    /**
     * Helper method which returns a string with the name of the item called
     * for. This method invokes {@link SWGComponentSlot#getType()} to determine
     * the type of item called for --- a schematic, category, or non-craftable
     * item --- and returns a name accordingly.
     * 
     * @param slot a component slot
     * @return a name
     */
    private String draftCompName(SWGComponentSlot slot) {
        String ret = null;
        try {
            if (slot.getType().equals("schematic"))
                ret = SWGSchematicsManager.getSchematic(
                        slot.getSchematicId()).getName();
            if (slot.getType().equals("category"))
                ret = SWGSchematicsManager.getCategory(
                        slot.getCategoryId()).getName();
            if (slot.getType().equals("item"))
                ret = slot.getItemName();
            if (ret != null)
                return "<b>" + ret + "</b>";
        } catch (Exception e) {
            return "<font color=\"red\"><b>Error</b></font>";
        }
        return SWGSchematicTab.stringOrUnknown(ret);
    }

    /**
     * Helper method which returns a string reading "(optional)" or the empty
     * string if this slot is not optional.
     * 
     * @param slot a component slot
     * @return a string
     */
    private String draftCompOptional(SWGComponentSlot slot) {
        if (slot.isOptional())
            return " (optional)";
        return "";
    }

    /**
     * Helper method which returns a string with the header data about the
     * specified schematic.
     * 
     * @param s a schematic
     * @return the data of the schematic
     */
    private String draftData(SWGSchematic s) {
        final String padd = "<td width=\"50\"></td>";

        String xp = (s.getSkillLevels().isEmpty()
                || s.getSkillLevels().get(0).
                        getLevel() < SWGProfessionLevel.MAX_LEVEL)
                        ? String.format("align=\"right\">%s</td>%s",
                                SWGSchematicTab.intOrUnknown(s.getXP()), padd)
                        : "colspan=\"2\">n/a";

        String crate = s.isManufacturable()
                ? String.format("<tr><td>Crate size:</td>" +
                        "<td align=\"right\">%s</td>%s</tr>",
                        SWGSchematicTab.intOrUnknown(s.getCrateSize()), padd)
                : "";

        String quality = SWGSchematicsManager.isQuality(s)
                || s.getExperimentGroups().size() > 0
                ? String.format("<tr><td>Quality:</td>" +
                        "<td align=\"right\">%s</td>%s</tr>",
                        s.quality == Quality.UNKNOWN
                                ? SWGSchematicTab.stringOrUnknown(null)
                                : s.quality.getName(), padd)
                : "";

        String str = String.format("<html><table cellpadding=\"0\">" +
                "<tr><td>Complexity:</td>" +
                "<td colspan=\"2\"><nobr>%s &minus; %s</nobr></td></tr>" +
                "<tr valign=\"top\"><td>Level:</td>" +
                "<td colspan=\"2\">%s</td></tr>" +
                "<tr><td>Base XP:</td><td %s</td></tr>" +
                "<tr><td>Type:</td><td colspan=\"2\">%s</td></tr>" +
                "<tr><td>Manufacture: &nbsp;</td>" +
                "<td align=\"right\">%s</td>%s</tr>"
                + "%s %s</table><br/></html>",
                SWGSchematicTab.intOrUnknown(s.getComplexity()),
                SWGSchematicTab.complexityToolStation(s.getComplexity()),
                draftDataProLevels(s),
                xp,
                draftDataType(s),
                s.isManufacturable()
                        ? "yes"
                        : "no", padd,
                crate, quality);
        return str;
    }

    /**
     * Helper method which returns a string for the specified schematic's
     * category and parent category.
     * 
     * @param s a schematic
     * @return a string for the category
     */
    private String draftDataCategory(SWGSchematic s) {
        String err = "<html><font color=\"red\"><b>Error</b></font>, %s</html>";
        SWGCategory c = SWGSchematicsManager.getCategory(s.getCategory());
        if (c == null)
            return String.format(err, "");
        SWGCategory p = SWGSchematicsManager.getCategory(c.getParentID());
        if (p == null)
            return String.format(err, c.getName());

        boolean fits = p.getName().length() + c.getName().length() + 2 < 38;
        return String.format("<html>%s%s%s</html>",
                p.getName(), fits
                        ? ", "
                        : "<br/>&mdash; ", c.getName());
    }

    /**
     * Helper method which returns a string for the profession levels in the
     * specified list. If the schematics is only or also available from
     * expertise the returned string reflects that as well.
     * 
     * @param s a schematic
     * @return a string for the profession levels
     */
    private String draftDataProLevels(SWGSchematic s) {
        ZString z = new ZString();
        for (SWGProfessionLevel p : s.getSkillLevels()) {
            if (p.getName().equals("Error"))
                z.app(SWGSchematicTab.UNKNOWN);
            else if (p.getName().equals("Novice"))
                z.app("Novice");
            else
                z.app(p.getProfession().getNameShort())
                        .app("&nbsp;(").app(p.getLevel()).app(')');
            z.app("<br/>");
        }

        if (s.getExpertise() != null)
            for (Object[] obj : s.getExpertise())
                z.app(obj[1]).app("<br/>");

        return z.sub(0, z.length() - 5);
    }

    /**
     * Helper method which returns a string for the schematic type. If the
     * schematics is only available from expertise the returned string reads
     * that.
     * 
     * @param s a schematic
     * @return a string for the type
     */
    private String draftDataType(SWGSchematic s) {
        if (s.getExpertise() != null && s.getSkillLevels().size() <= 0)
            return "Expertise";
        return s.getType();
    }

    /**
     * Helper method which returns a string with data from the specified
     * experimentation group.
     * 
     * @param eGrp an experimentation group
     * @return the data for the group
     */
    private String draftExpGroup(SWGExperimentGroup eGrp) {
        List<SWGExperimentLine> els = eGrp.getExperimentalLines();
        ZString z = new ZString();
        z.app("<html>");
        z.app(SWGSchematicTab.stringOrUnknown(eGrp.getDescription()));
        for (SWGExperimentLine el : els)
            draftExpLine(el, z);

        return z.app("</html>").toString();
    }

    /**
     * Helper method which updates the labels in {@link #draftExpGroups} for the
     * specified schematic; {@link #draftAddExpLabels(List, int)} is invoked to
     * ensure enough labels exist; unused labels are emptied but not removed.
     * 
     * @param s a schematic
     */
    private void draftExpGroups(SWGSchematic s) {
        List<SWGExperimentGroup> egl = s.getExperimentGroups();
        int len = egl.size() + 1;

        draftAddExpLabels(draftExpGroups, len);

        len = 0;
        for (SWGExperimentGroup eg : egl) {
            draftExpGroups.get(len).setText(draftExpGroup(eg));
            ++len;
        }
        for (; len < draftExpGroups.size(); ++len)
            draftExpGroups.get(len).setText(null);
    }

    /**
     * Helper method which adds a string with data from the specified
     * experimental line to the string builder.
     * 
     * @param eLine an experimental line
     * @param z a string builder
     */
    private void draftExpLine(SWGExperimentLine eLine, ZString z) {
        SWGWeights w = eLine.getWeights();
        z.app("<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        z.app(SWGSchematicTab.stringOrUnknown(eLine.getDescription()));
        z.app("<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        z.app("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>");
        boolean nf = false;
        for (Stat s : Stat.values())
            if (w.value(s) > 0) {
                if (nf) z.app("&nbsp;&nbsp;&nbsp;&nbsp;");
                nf = true;
                z.app(s.name()).app(' ').app(w.value(s)).app(' ').app('%');
            }
        z.app("</b>");
    }

    /**
     * Helper method which returns a string with misc data from the specified
     * schematic. This method finally appends extra information for the
     * specified schematic if such information is available, for example
     * food&drink buffs.
     * 
     * @param s a schematic
     * @return misc data
     */
    private String draftMisc(SWGSchematic s) {
        String extra = SWGSchematicsManager.foodDrinkBuffs(s);
        extra = extra != null
                ? String.format(" &mdash; <b>Buffs</b>: %s", extra)
                : "";

        String i = s.getDescription() != null
                ? ZHtml.wrapToWidth(DRAFT_WIDTH - 15, null,
                        SWGGuiUtils.fontPlain(), s.getDescription())
                : "Info: " + SWGSchematicTab.UNKNOWN;
        return String.format(
                "<html>&nbsp;<br/>%s%s<font color=#bbbbbb><br/>&mdash; " +
                        "SWGCraft-id:&nbsp;%s %s<br/>&nbsp;</font></html>",
                i, extra, Integer.toString(s.getID()), s.hasScreenshot()
                        ? ""
                        : "&mdash; <font color=\"red\">add screenshot</font>");
    }

    /**
     * Helper method which returns a string with data from the specified
     * resource slot.
     * 
     * @param slot a resource slot
     * @return the data for the resource slot
     */
    private String draftRes(SWGResourceSlot slot) {
        String d = slot.getDescription();
        String s = String.format("<b>%s</b> units of&nbsp;<b> %s</b>",
                ZNumber.asText(slot.getUnits(), true, true),
                slot.getResourceClass().rcName());

        return draftWidth(d, s);
    }

    /**
     * Helper method which updates the labels in {@link #draftRCLabels} for the
     * specified schematic; {@link #draftAddRCLabels(List, int)} is invoked to
     * ensure enough labels exist; unused labels are emptied but not removed.
     * 
     * @param s a schematic
     */
    private void draftResAndComps(SWGSchematic s) {
        List<SWGResourceSlot> rl = s.getResourceSlots();
        List<SWGComponentSlot> cl = s.getComponentSlots();
        int len = rl.size() + cl.size() + 2;

        draftAddRCLabels(draftRCLabels, len);

        len = 0;
        for (SWGResourceSlot rs : rl) {
            draftRCLabels.get(len).setContent(draftRes(rs), rs);
            ++len;
        }
        draftRCLabels.get(len).setContent(" ", " ");
        ++len;
        if (cl.size() > 0) {
            for (SWGComponentSlot cs : cl) {
                draftRCLabels.get(len).setContent(draftComp(cs), cs);
                ++len;
            }
            draftRCLabels.get(len).setContent(" ", " ");
            ++len;
        }
        for (; len < draftRCLabels.size(); ++len)
            draftRCLabels.get(len).eraseContent();
    }

    /**
     * Helper method which returns the specified strings so that the result is
     * no wider than {@link #DRAFT_WIDTH}. This method formats the arguments for
     * the resource and the component labels.
     * 
     * @param desc a description
     * @param unitsAndContent a string with units and content
     * @return a formatted string
     */
    private String draftWidth(String desc, String unitsAndContent) {
        String s = unitsAndContent;
        int clutter = 95; // for white spaces and HTML stuff
        int w = ZHtml.fontWidth(s, SWGGuiUtils.fontPlain());
        String f = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

        if (w > DRAFT_WIDTH + clutter)
            s = ZHtml.wrapToWidth(DRAFT_WIDTH + clutter,
                        f + f, SWGGuiUtils.fontPlain(), s);

        return String.format("<html>%s<br/>%s%s</html>",
                SWGSchematicTab.stringOrUnknown(desc), f, s);
    }

    /**
     * Helper method which returns the current integer value for
     * {@link #factoryAmount}, a value between 1 and 1000 or limited by
     * {@link #factoryAmountMax}.
     * 
     * @return the current value for {@link #factoryAmount}
     */
    private int factoryAmount() {
        return ((Integer) factoryAmount.getValue()).intValue();
    }

    /**
     * Helper method which displays a find dialog. This is the starting point
     * for the user to find schematics with a given text string in their names.
     * 
     * @param txt text for the first dialog
     */
    private void findSchematicsDialog(String txt) {
        String f = (String) JOptionPane.showInputDialog(traceUp,
                txt, "Find", JOptionPane.PLAIN_MESSAGE, null, null, findTxt);

        if (f == null || f.trim().isEmpty()) return;

        findTxt = f;
        List<SWGSchematic> res = SWGSchematicsManager.findSchematics(findTxt);
        if (res.size() <= 0)
            JOptionPane.showMessageDialog(traceUp,
                    String.format("Find failed for \"%s\"", findTxt),
                    "No Result", JOptionPane.WARNING_MESSAGE);
        else {
            Collections.sort(res);
            usedinAndFindModel.setElements(res);
        }
    }

    /**
     * This method is invoked when the user selects the schematics tab or any
     * one of the tabs in this tabbed pane. This implementation adds some menu
     * items to the frame's menu bar.
     */
    void focusGained() {
        if (schemTab.frame.getTabPane().getSelectedComponent() == schemTab
                && schemTab.getSelectedComponent() == this) {

            if (findMenu == null) 
                findMenu = makeFindMenu();            
            schemTab.frame.editMenuAdd(findMenu);

            if (menuItemsNavigate != null)
                for (JMenuItem mi : menuItemsNavigate)
                    schemTab.frame.optionsMenuAdd(mi);
            schemTab.updateStatbar2();
        } else {
            schemTab.frame.editMenuRemove(findMenu);
            if (menuItemsNavigate != null)
                for (JMenuItem mi : menuItemsNavigate)
                    schemTab.frame.optionsMenuRemove(mi);
        }
    }

    /**
     * Helper method which creates and returns a menu option for information on
     * special or looted items, or special categories. When the user selects
     * this option a plain message dialog is displayed with the specified text.
     * 
     * @param comp a component for the dialog
     * @param text the text to display
     * @return a menu item
     */
    private JMenuItem informationMenu(final Component comp, final String text) {
        JMenuItem m = new JMenuItem("Information");
        m.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e1) {
                JOptionPane.showMessageDialog(comp,
                        SWGSchematicsManager.informationText(text),
                        "Information", JOptionPane.PLAIN_MESSAGE);
            }
        });
        return m;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // ignore, added as part of clip board support
    }

    /**
     * Helper method which creates the interior of this GUI element when the
     * user selects the schematics tab for the first time.
     */
    private void make() {
        this.setLeftComponent(makeWest());
        this.setRightComponent(makeCenter());

        // now it is time to trigger GUI calls
        Integer sid = (Integer) SWGFrame.getPrefsKeeper().get(
                /* Small Glass --- anyone has it */
                "schemDraftSelectedSchematic", Integer.valueOf(690));
        if (sid != null)
            selectedSchematic = SWGSchematicsManager.getSchematic(
                    sid.intValue());
        if (selectedSchematic != null)
            selectedCategory = SWGSchematicsManager.getCategory(
                    selectedSchematic.getCategory());

        actionFilterSchematics();
        display(selectedSchematic);

        setDividerLocation(280);
        actionAdjustWidths();
    }

    /**
     * Helper method which creates and returns the center/main panel of this
     * component. This is the section that displays the draft schematic and, its
     * shopping list, and some other data.
     * 
     * @return a GUI component
     */
    private Component makeCenter() {
        JPanel rb = new JPanel(new BorderLayout(3, 0));
        rb.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        // create the draft panel
        draftPanel = makeCenterDraftPanel();
        rb.add(draftPanel, BorderLayout.LINE_START);

        // create the east-most panel
        JPanel detailsPanel = makeEast();
        rb.add(detailsPanel, BorderLayout.CENTER);

        JScrollPane jsp = new JScrollPane(rb,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setName("eastPanelJSP");
        jsp.getVerticalScrollBar().setUnitIncrement(20);

        // the modal layer is good enough but components with listeners are
        // honored, hence events outside such components are caught
        SWGAide.frame().getLayeredPane().add(jsp, JLayeredPane.MODAL_LAYER);
        jsp.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionEastMouse(e, null);
            }
        });

        return jsp;
    }

    /**
     * Helper method which creates and returns a panel on which to display the
     * details of a draft schematic.
     * 
     * @return a GUI component
     */
    private JPanel makeCenterDraftPanel() {
        // the inner box for the small elements
        final Box innerBox = Box.createVerticalBox();
        innerBox.setBorder(
                BorderFactory.createLineBorder(SWGGuiUtils.colorThinBorder));
        innerBox.setBackground(Color.WHITE);
        innerBox.setOpaque(true);

        // the label for category
        draftCategory = new JLabel() {
            @SuppressWarnings("synthetic-access")
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(DRAFT_WIDTH, 32);
            }
        };
        draftCategory.setVerticalAlignment(SwingConstants.TOP);
        draftCategory.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        draftCategory.setFont(SWGGuiUtils.fontPlain());
        draftCategory.setBackground(Color.WHITE);
        draftCategory.setOpaque(true);
        innerBox.add(draftCategory);

        innerBox.add(Box.createVerticalStrut(5));

        // the label for schematic data
        draftData = makeLabel();
        innerBox.add(draftData);

        // a basic container for resource and component labels
        draftRCPanel = new JPanel();
        draftRCPanel.setOpaque(true);
        draftRCPanel.setBackground(Color.WHITE);
        draftRCPanel.setLayout(new BoxLayout(
                draftRCPanel, BoxLayout.PAGE_AXIS));
        draftRCLabels = new ArrayList<RCLabel>(30);
        draftAddRCLabels(draftRCLabels, 10);
        innerBox.add(draftRCPanel);

        // a basic container for experimentation groups
        draftExpPanel = new JPanel();
        draftExpPanel.setLayout(new BoxLayout(
                draftExpPanel, BoxLayout.PAGE_AXIS));
        draftExpGroups = new ArrayList<JLabel>(10);
        draftAddExpLabels(draftExpGroups, 5);
        innerBox.add(draftExpPanel);

        // a label for misc information that reads at the schematic
        draftMisc = new JLabel() {
            @SuppressWarnings("synthetic-access")
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(DRAFT_WIDTH,
                        draftMisc.getMinimumSize().height);
            }
        };
        draftMisc.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        draftMisc.setFont(SWGGuiUtils.fontPlain());
        draftMisc.setBackground(Color.WHITE);
        draftMisc.setOpaque(true);
        innerBox.add(draftMisc);

        innerBox.add(Box.createVerticalGlue());

        // the outermost panel for the draft stuff
        JPanel draftBottom = new JPanel() {
            @Override
            public int getWidth() {
                return innerBox.getWidth() + 17;
            }
        };
        draftBottom.setLayout(new BoxLayout(draftBottom, BoxLayout.PAGE_AXIS));

        // a reference to the border for the draft panel, to set the title
        draftTitle = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder());
        draftBottom.setBorder(BorderFactory.createCompoundBorder(
                draftTitle, BorderFactory.createEmptyBorder(6, 2, 2, 2)));

        draftBottom.add(innerBox);
        return draftBottom;
    }

    /**
     * Helper method which creates and returns the panel that displays details
     * such as shopping list, used in, etc.
     * 
     * @return a GUI component
     */
    private JPanel makeEast() {
        JPanel dp = new JPanel();
        dp.setLayout(new BorderLayout(0, 6));

        dp.add(makeEastShoppingPanel(), BorderLayout.PAGE_START);
        dp.add(makeEastUsedinPanel(), BorderLayout.CENTER);

        return dp;
    }

    /**
     * Helper method which creates and returns a GUI element for manufacturing
     * and notes made at the default schematic's wrapper. This method invokes
     * {@link #makeEastFactoryAmount()} and makeEastNotes() and returns a panel
     * which contains the two.
     * 
     * @return a GUI element
     */
    private Component makeEastFactNotes() {
        Box b = Box.createHorizontalBox();
        b.add(makeEastFactoryAmount());
        b.add(makeEastNotes());
        return b;
    }

    /**
     * Helper method which creates and returns a GUI element that is related to
     * manufacturing. The element presents the user with an element to select a
     * size for a factory run, default value is one. When the user selects a
     * value the shopping list and the estimated time is updated.
     * 
     * @return a GUI component
     */
    private Component makeEastFactoryAmount() {
        final String fs = "Specify number of items for factory run";
        JPanel bp = new JPanel(new SpringLayout());

        JLabel l = new JLabel("Factory batch: ");
        l.setToolTipText(fs);
        bp.add(l);

        factoryAmount = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 100) {
            @SuppressWarnings("synthetic-access")
            @Override
            public Object getNextValue() {
                int i = ((Integer) getValue()).intValue();
                int j;
                if (i < 5)
                    j = 5;
                else if (i < 10)
                    j = 10;
                else if (i < 15)
                    j = 15;
                else if (i < 25)
                    j = 25;
                else if (i < 50)
                    j = 50;
                else if (i < 100)
                    j = 100;
                else {
                    Integer ji = (Integer) super.getNextValue();
                    j = ji != null
                            ? (ji.intValue() / 100) * 100
                            : 1000;
                }
                return Integer.valueOf(Math.min(j, factoryAmountMax));
            }

            // use the default crate sizes, then step by 100

            @Override
            public Object getPreviousValue() {
                int i = ((Integer) getValue()).intValue();
                if (i > 100) {
                    i = ((Integer) super.getPreviousValue()).intValue();
                    return Integer.valueOf(100 * Math.round(i / 100.0f));
                }
                if (i > 50)
                    return Integer.valueOf(50);
                if (i > 25)
                    return Integer.valueOf(25);
                if (i > 15)
                    return Integer.valueOf(15);
                if (i > 10)
                    return Integer.valueOf(10);
                if (i > 5)
                    return Integer.valueOf(5);

                return Integer.valueOf(1);
            }
        });
        factoryAmount.setToolTipText(fs);
        factoryAmount.addChangeListener(new ChangeListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void stateChanged(ChangeEvent e) {
                displayShopping(selectedSchematic);
            }
        });
        Dimension d = new Dimension(110, 23);
        factoryAmount.setPreferredSize(d);
        factoryAmount.setMaximumSize(d);
        bp.add(factoryAmount);

        // vb.add(Box.createHorizontalStrut(20));

        JLabel hl = new JLabel("Estimated time: ");
        bp.add(hl);
        factoryHours = makeLabel();
        factoryHours.setHorizontalAlignment(SwingConstants.TRAILING);
        factoryHours.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        factoryHours.setPreferredSize(d);
        factoryHours.setMaximumSize(d);
        bp.add(factoryHours);

        SpringUtilities.makeCompactGrid(bp, 2, 2, 0, 0, 10, 2);
        return bp;
    }

    /**
     * Helper method which creates and returns a GUI element for the notes made
     * at the default schematic's wrapper; that is the {@link #schemNotes}.
     * 
     * @return a GUI element
     */
    private Component makeEastNotes() {
        schemNotes = new JTextArea();
        schemNotes.setEditable(false);
        schemNotes.setToolTipText("Notes added for the default schematic");
        schemNotes.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionNotes(e);
            }
        });
        JScrollPane jsp = new JScrollPane(schemNotes);
        return jsp;
    }

    /**
     * Helper method which creates and returns a GUI element for the shopping
     * list for a selected schematic.
     * 
     * @return a GUI component
     */
    private JEditorPane makeEastShoppingList() {
        shoppingList = new JEditorPane();
        shoppingList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SWGGuiUtils.colorThinBorder),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        shoppingList.setContentType("text/html");
        shoppingList.setEditable(false);

        shoppingList.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK),
                new AbstractAction() {
                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        copyToClipboard(shoppingList(factoryAmount()));
                    }
                });
        shoppingList.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionEastMouse(e, null);
            }
        });

        return shoppingList;
    }

    /**
     * Helper method which creates and returns a text pane on which to display a
     * shopping list for the selected schematic.
     * 
     * @return a GUI component
     */
    private Component makeEastShoppingPanel() {
        JPanel sb = new JPanel();
        sb.setLayout(new BoxLayout(sb, BoxLayout.PAGE_AXIS));
        sb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "Shopping List"),
                BorderFactory.createEmptyBorder(6, 2, 2, 2)));

        sb.add(makeEastShoppingList());
        sb.add(Box.createVerticalStrut(5));
        sb.add(makeEastFactNotes());

        return sb;
    }

    /**
     * Helper method which creates and returns a GUI element for the used-in
     * list for a selected schematic.
     * 
     * @return a GUI component
     */
    private JPanel makeEastUsedinPanel() {
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        "Used In ... / Find Result"),
                BorderFactory.createEmptyBorder(6, 2, 2, 2)));

        usedinAndFindModel = new UsedInModel();
        usedinAndFind = new JList(usedinAndFindModel);
        usedinAndFind.setLayoutOrientation(JList.VERTICAL);

        usedinAndFind.setCellRenderer(new ListCellRenderer() {

            JLabel label = new JLabel();

            {
                label.setFont(SWGGuiUtils.fontPlain());
                label.setOpaque(true);
            }

            @SuppressWarnings("synthetic-access")
            @Override
            public Component getListCellRendererComponent(
                    JList l, Object val, int i, boolean sel, boolean focus) {

                SWGSchematic schem = (SWGSchematic) val;

                label.setText(schem != null
                        ? schem.getName()
                        : null);

                boolean directUse =
                        schem == null || selectedSchematic == null
                                ? false
                                : usedInSlot(schem.getComponentSlots(),
                                        selectedSchematic);

                if (sel) {
                    Color bg = usedinAndFind.getSelectionBackground();
                    label.setBackground(directUse
                            ? SWGGuiUtils.colorDarker(bg, 0.9f)
                            : bg);
                } else
                    label.setBackground(directUse
                            ? Color.WHITE
                            : SWGGuiUtils.colorComponent);

                return label;
            }
        });

        usedinAndFind.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SWGGuiUtils.colorThinBorder),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        usedinAndFind.addListSelectionListener(new ListSelectionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int i = usedinAndFind.getSelectedIndex();
                if (!e.getValueIsAdjusting() && i >= 0)
                    actionSchematicSelected((SWGSchematic)
                            usedinAndFindModel.getElementAt(i),
                            traceForward, false);
            }
        });

        usedinAndFind.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionUsedin(e);
            }
        });
        sb.add(usedinAndFind, BorderLayout.PAGE_START);

        return sb;
    }

    /**
     * Creates and returns a menu item which if it is selected invokes
     * {@link #findSchematicsDialog(String)}.
     * 
     * @return a menu item
     */
    private JMenuItem makeFindMenu() {
        final JMenuItem find = SWGSchematicTab.findSchematicMenu();
        find.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent ee) {
                findSchematicsDialog(find.getToolTipText());
            }
        });
        return find;
    }

    /**
     * Helper method which creates and returns a text label. The characteristics
     * are: font is {@link SWGGuiUtils#fontPlain()}, it is opaque, and its
     * background color is set to the specified color or default if the argument
     * is {@code null}.
     * 
     * @return a GUI component
     */
    private JLabel makeLabel() {
        JLabel l = new JLabel();
        l.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        l.setFont(SWGGuiUtils.fontPlain());
        l.setBackground(Color.WHITE);
        l.setOpaque(true);
        return l;
    }

    /**
     * Helper method which creates and returns the west main panel of this
     * component. This is the section where the user can filter for and select
     * the schematic to view.
     * 
     * @return a GUI component
     */
    private Component makeWest() {
        JPanel bp = new JPanel(new BorderLayout());
        bp.add(makeWestTraceBar(), BorderLayout.PAGE_START);
        bp.add(makeWestTreePanel(), BorderLayout.CENTER);
        bp.add(makeWestFilterPanel(), BorderLayout.PAGE_END);
        return bp;
    }

    /**
     * Helper method which creates and returns a panel with drop down filters
     * for professions and skill levels to filter the schematics with.
     * 
     * @return a GUI component
     */
    private Component makeWestFilterPanel() {
        Box h = Box.createHorizontalBox();
        h.add(makeWestFProfessionChooser());
        h.add(makeWestFLevelChooser());
        h.add(makeWestFSchematicsChooser());
        return h;
    }

    /**
     * Helper method which creates and returns a profession level chooser. The
     * user selects a maximum value from a spinner in the range 0 to 90.
     * 
     * @return a GUI component
     */
    private Component makeWestFLevelChooser() {
        selectedLevel = (Integer) SWGFrame.getPrefsKeeper().get(
                "schemDraftSelectedProfLevel",
                Integer.valueOf(SWGProfessionLevel.MAX_LEVEL));

        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                selectedLevel.intValue(), 0, SWGProfessionLevel.MAX_LEVEL, 1));
        spinner.setToolTipText("Filter by maximum profession level");
        Dimension d = new Dimension(40, 25);
        spinner.setMaximumSize(d);
        spinner.setPreferredSize(d);

        spinner.addChangeListener(new ChangeListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void stateChanged(ChangeEvent e) {
                selectedLevel = (Integer) spinner.getValue();
                SWGFrame.getPrefsKeeper().add(
                        "schemDraftSelectedProfLevel", selectedLevel);
                actionFilterSchematics();
            }
        });
        return spinner;
    }

    /**
     * Helper method which creates and returns a profession chooser, a drop-down
     * list. The component displays all professions and a default option "All".
     * 
     * @return a GUI component
     */
    private Component makeWestFProfessionChooser() {
        selectedProfession = (SWGProfession) SWGFrame.getPrefsKeeper().get(
                "schemDraftSelectedProfession", SWGProfession.ALL);

        List<String> pl = SWGProfession.getNames(false);
        final JComboBox cb = new JComboBox(pl.toArray());
        cb.setToolTipText("Filter by profession");

        Dimension d = new Dimension(100, 25);
        cb.setMinimumSize(d);
        cb.setPreferredSize(d);
        cb.setMaximumSize(d);

        cb.setSelectedItem(selectedProfession.getNameShort());
        cb.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedProfession = SWGProfession.getFromName(
                        (String) cb.getSelectedItem());
                SWGFrame.getPrefsKeeper().add(
                        "schemDraftSelectedProfession", selectedProfession);
                actionFilterSchematics();
            }
        });
        return cb;
    }

    /**
     * Helper method which creates and returns a drop down list with filtered
     * schematics in alphabetical order.
     * 
     * @return a GUI component
     */
    private JComboBox makeWestFSchematicsChooser() {
        schemSelector = new JComboBox();
        schemSelectModel = new SchemSelectionModel();
        schemSelector.setModel(schemSelectModel);
        schemSelector.setToolTipText("Select schematic, filters apply");

        schemSelector.setMinimumSize(new Dimension(100, 25));
        schemSelector.setPreferredSize(new Dimension(150, 25));

        schemSelector.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                int s = schemSelector.getSelectedIndex();
                if (isUpdatingGUI || s < 0)
                    return;

                actionSchematicSelected(
                        schemSelectModel.getSchematic(s), traceForward, false);
            }
        });
        return schemSelector;
    }

    /**
     * Helper method which creates and returns a tool-bar with navigation
     * buttons for backward, up, and forward. The back/forward buttons are
     * enabled/disabled as the user navigates or selects schematic. The up
     * button climbs up the category tree until at the root.
     * 
     * @return a GUI component
     */
    private JToolBar makeWestTraceBar() {
        ActionListener action = new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                actionTraceClicked(e);
            }
        };

        traceBack = SWGGuiUtils.makeButton("Back16", "Go back", "Back");
        traceBack.addActionListener(action);
        traceBack.setEnabled(false);

        traceUp = SWGGuiUtils.makeButton("Up16", "Climb up the tree", "Up");
        traceUp.addActionListener(action);

        traceForward =
                SWGGuiUtils.makeButton("Forward16", "Go forward", "Forward");
        traceForward.addActionListener(action);
        traceForward.setEnabled(false);

        JToolBar tb = new JToolBar();
        tb.setLayout(new BoxLayout(tb, BoxLayout.LINE_AXIS));
        tb.add(Box.createGlue());
        tb.add(traceBack);
        tb.add(traceUp);
        tb.add(traceForward);
        tb.add(Box.createGlue());

        tb.setFloatable(false);

        JMenuItem back = new JMenuItem("Back", KeyEvent.VK_B);
        back.addActionListener(action);
        back.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_LEFT, ActionEvent.ALT_MASK));

        JMenuItem forw = new JMenuItem("Forward", KeyEvent.VK_F);
        forw.addActionListener(action);
        forw.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_RIGHT, ActionEvent.ALT_MASK));

        menuItemsNavigate = new ArrayList<JMenuItem>(2);
        menuItemsNavigate.add(back);
        menuItemsNavigate.add(forw);

        return tb;
    }

    /**
     * Helper method which creates and returns a panel that displays a tree of
     * schematics, ordered by component categories.
     * 
     * @return a GUI component
     */
    private Component makeWestTreePanel() {
        schemTreeModel = new SWGSchematicTreeModel();
        schemTree = new JTree(schemTreeModel) {
            @Override
            public void scrollRectToVisible(Rectangle aRect) {
                aRect.width -= aRect.x; // adjust rect to keep tree left-aligned
                aRect.x = 0;
                super.scrollRectToVisible(aRect);
            }
        };
        schemTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

        schemTree.addTreeSelectionListener(new TreeSelectionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = schemTree.getSelectionPath();
                if (path == null)
                    return;

                Object o = path.getLastPathComponent();
                TNode tn = (TNode) o;
                if (tn.getContent() instanceof SWGSchematic) {
                    SWGSchematic s = (SWGSchematic) tn.getContent();
                    actionSchematicSelected(s, traceForward, false);
                } else if (tn.getContent() instanceof SWGCategory) {
                    selectedCategory = (SWGCategory) tn.getContent();
                    updateTracer(selectedCategory, traceForward);
                }
            }
        });

        schemTree.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                actionSchemTreeMouse(e);
            }
        });

        JScrollPane jsp = new JScrollPane(schemTree,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        jsp.setMinimumSize(new Dimension(240, 100));

        return jsp;
    }

    /**
     * Selects the specified schematic at this panel. If the argument is {@code
     * null} this method does nothing, otherwise the schematic is selected and
     * if necessary filters are overruled and no popup dialog is displayed.
     * 
     * @param s a schematic
     */
    void schemSelect(SWGSchematic s) {
        actionSchematicSelected(s, traceForward, true);
    }

    /**
     * Helper method that returns a shopping list as a string. The shopping list
     * includes resources, components or component categories, and items that
     * are used for the selected schematic. Everything is multiplied by the
     * specified count. If no schematic is selected the empty string is
     * returned.
     * 
     * @param count the number of schematics
     * @return a shopping list, not {@code null}
     */
    private ZString shoppingList(int count) {
        if (selectedSchematic == null) return ZString.EMPTY;

        List<ResourceAmount> ral = shoppingResourcesSorted(selectedSchematic);
        if (ral == null) return ZString.EMPTY;

        ZString z = new ZString();

        z.app(selectedSchematic.getName());
        z.app("  ---  count: ").app(count).nl().nl();

        for (ResourceAmount ra : ral) {
            z.app(String.format("%,9d", Integer.valueOf(ra.getUnits() * count)));
            z.app(' ').appnl(ra.getResourceClass().rcName());
        }
        z.nl();

        for (SWGComponentSlot cs : selectedSchematic.getComponentSlots()) {
            z.app(String.format("%,9d",
                    Integer.valueOf(cs.getAmount() * count))).app(' ');

            if (cs.getType().equals("schematic")) {
                SWGSchematic ss = SWGSchematicsManager.getSchematic(
                        cs.getSchematicId());
                z.app(ss != null
                        ? ss.getName()
                        : "ERROR");
            } else if (cs.getType().equals("category")) {
                SWGCategory cc = SWGSchematicsManager.getCategory(
                        cs.getCategoryId());
                z.app(cc != null
                        ? cc.getName()
                        : "ERROR");
            } else {// item
                z.app(cs.getItemName());
            }
            if (cs.isOptional()) z.app(' ').app("(optional)");
            if (cs.isSimilar()) z.app(' ').app("(similar)");
            z.nl();
        }

        // if editing this text, also update shoppingListWrite
        z.nl();
        z.appnl("Optional and interchangeable components are not included in the");
        z.appnl("shopping list, nor is losses from manufacturing sub-components.");
        return z;

    }

    /**
     * Helper method which writes the specified string to a notes file. The
     * location for the notes file is determined by the currently selected
     * character and is named "shop.txt" and the argument is written to file as
     * is. If no character is selected a dialog is displayed and nothing more.
     * If the string is {@code null} or empty this method does nothing.
     * <p>
     * If the boolean argument is {@code true} the string is appended to the
     * notes file, otherwise the string over-writes any content. If a notes file
     * does not exist it is created.
     * 
     * @param shop a shopping list for a schematic
     * @param append {@code true} to append text, {@code false} to over-write
     */
    private void shoppingListWrite(ZString shop, boolean append) {
        SWGCharacter toon = SWGFrame.getSelectedCharacter();
        if (toon == null) {
            JOptionPane.showMessageDialog(schemTab.frame,
                    "No character selected at main panel.\n" +
                            "Do not know where to write, bailing",
                    "Unknown target",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (shop.trimmed().isEmpty()) return;

        SWGStation st = toon.galaxy().station();
        SWGNotes notes = st.notes("shop.txt");
        if (notes == null)
            notes = SWGNotes.getInstance("shop.txt", st, null, false);
        else
            notes.backup();

        if (append) {
            ZString s = new ZString();
            int r = notes.line("Optional and interchangeable", 0);
            if (r < 1) {
                r = 1;
            } else
                s.nl();
            s.app(shop.toString());
            notes.setText(r, Integer.MAX_VALUE, s.toString());
        } else
            notes.setText(shop.toString());
    }

    /**
     * Helper method which sorts the list of resource-amount objects that is
     * obtained from {@link SWGSchematicsManager#getShopping(SWGSchematic)}. The
     * list is sorted by the amount of each resource in increasing order. If
     * there is an error {@code null} is returned.
     * 
     * @param schem a schematic
     * @return a sorted list, or {@code null}
     */
    private List<ResourceAmount> shoppingResourcesSorted(SWGSchematic schem) {
        List<ResourceAmount> ral = SWGSchematicsManager.getShopping(schem);
        if (ral != null)
            Collections.sort(ral, new Comparator<ResourceAmount>() {

                @Override
                public int compare(ResourceAmount o1, ResourceAmount o2) {
                    return o1.getUnits() - o2.getUnits();
                }
            });
        return ral;
    }

    /**
     * Helper method that updates the navigation tracer. The direction argument
     * determines forward or backward. If the node equals the top node at the
     * tracer this methods does nothing, hence it is valid to invoke this method
     * also for "void steps".
     * 
     * @param node the selected node, or {@code null}
     * @param direction the direction, or {@code null}
     */
    private void updateTracer(Object node, JButton direction) {
        if (node instanceof SWGSchematic) {
            if (direction == null || direction == traceForward) {
                if (tracer.isEmpty() || tracer.peek() != node)
                    tracer.stepForward(node);
            } else if (direction == traceBack) {
                /* pass, action taken before this call */
            }
        } else if (node instanceof SWGCategory) {
            SWGCategory cat = (SWGCategory) node;
            if (direction == null || direction == traceForward) {
                if (tracer.isEmpty() || tracer.peek() != node)
                    tracer.stepForward(node);
            } else if (direction == traceBack) {
                /* pass, action taken before this call */
            }
            traceUp.setEnabled(cat.getID() > SWGCategory.ALL);
        }

        // we can always do this
        traceForward.setEnabled(tracer.existsForward());
        traceBack.setEnabled(tracer.existsBack());
    }

    /**
     * Helper method that returns a sorted list of schematics that depends on
     * the specified schematic, or an empty list. The elements in the returned
     * list are schematics that are using the specified schematic directly via
     * components or via component categories. The list is sorted by name.
     * <p>
     * This implementation does not recursively determine the use of a parent
     * schematic. For example, if the specified schematics is <i>Control
     * Unit</i>, which is used as sub-component in several components, only the
     * direct use of a <i>Control Unit&nbsp;</i> is listed.
     * <p>
     * However, if the specified schematic is contained by a component category
     * this implementation recursively traverses the component categories
     * schematics that accept the component category or any of its parent
     * categories.
     * 
     * @param schem a schematic
     * @return a list of schematics
     * @throws NullPointerException if the argument is {@code null}
     */
    private List<SWGSchematic> usedIn(final SWGSchematic schem) {
        List<SWGSchematic> ret = new ArrayList<SWGSchematic>();

        List<SWGCategory> cats = new ArrayList<SWGCategory>();
        int id = schem.getCategory();
        while (id > SWGCategory.ALL) {
            SWGCategory c = SWGSchematicsManager.getCategory(id);
            cats.add(c);
            id = c.getParentID();
        }

        // scan all schematics
        for (SWGSchematic s : SWGSchematicsManager.getSchematics()) {
            for (SWGComponentSlot cs : s.getComponentSlots()) {
                // direct use
                id = cs.getSchematicId();
                if (id > 0 && id == schem.getID() && !ret.contains(s)) {
                    ret.add(s);
                    continue;
                }

                // use via component category
                id = cs.getCategoryId();
                if (id > 0) {
                    SWGCategory c = SWGSchematicsManager.getCategory(id);
                    if (cats.contains(c) && !ret.contains(s)) ret.add(s);
                }
            }
        }

        // This comparator orders element with higher precedence to those that
        // contains the specified argument
        Comparator<SWGSchematic> comp = new Comparator<SWGSchematic>() {

            @SuppressWarnings("synthetic-access")
            @Override
            public int compare(SWGSchematic o1, SWGSchematic o2) {
                boolean b1 = usedInSlot(o1.getComponentSlots(), schem);
                boolean b2 = usedInSlot(o2.getComponentSlots(), schem);
                if (b1 && b2) return o1.compareTo(o2);
                if (b1) return -1;
                if (b2) return 1;

                return o1.compareTo(o2);
            }
        };

        Collections.sort(ret, comp);
        return ret;
    }

    /**
     * Helper method which returns {@code true} if any the specified component
     * slots contains the specified schematic, {@code false} otherwise.
     * 
     * @param slots a list of component slots
     * @param s a schematic
     * @return true if schematic is found
     * @throws NullPointerException if the list is {@code null}
     */
    private boolean usedInSlot(List<SWGComponentSlot> slots, SWGSchematic s) {
        for (SWGComponentSlot el : slots)
            if (el.getSchematicId() == s.getID()) return true;

        return false;
    }

    /**
     * Returns a filtered list of schematics, its content is the result of what
     * the user has selected. The list may be empty, filtered, or all
     * schematics.
     * 
     * @return a filtered list of schematics
     */
    static List<SWGSchematic> getFilteredSchematics() {
        return filteredSchematics;
    }

    /**
     * A dialog to select or define an assignee and to a schematic for.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    final class FavoriteDialog extends SWGJDialog {

        /**
         * A constant for define new assignee.
         */
        private static final String ASSIGNEE = "enter assignee";

        /**
         * A list of existing assignees.
         */
        private final JList assigneeList;

        /**
         * A temporary list of assignees; this list is used by the OK-button
         * action listener and is reset at each display.
         */
        private List<SWGSchematicAssignee> currAssignees;

        /**
         * A temporary reference to the current schematic which to add as a
         * favorite; this is used by the OK-button action listener and is set at
         * each display.
         */
        private SWGSchematic currSchem;

        /**
         * A text field on which to define a new assignee.
         */
        private final JTextField defineAssignee;

        /**
         * A label to present schematic at.
         */
        private final JLabel label;

        /**
         * The model for the list of assignees.
         */
        private final DefaultListModel model;

        /**
         * Creates a modal instance of this type.
         */
        FavoriteDialog() {
            super("Select/Define Assignee");

            label = new JLabel("", SwingConstants.CENTER);

            assigneeList = new JList();
            assigneeList.setVisibleRowCount(4);
            assigneeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane jsp = new JScrollPane(assigneeList);
            model = new DefaultListModel();
            assigneeList.setModel(model);

            defineAssignee = new JTextField();
            defineAssignee.setToolTipText("Define a new assignee");

            JButton ok = new JButton("OK");
            ok.setMnemonic(KeyEvent.VK_O);
            ok.setToolTipText("Accept the selected/defined assignee");
            ok.addActionListener(new ActionListener() {
                @SuppressWarnings("synthetic-access")
                @Override
                public void actionPerformed(ActionEvent e) {
                    okAction();
                }
            });
            JButton cancel = new JButton("Cancel");
            cancel.setMnemonic(KeyEvent.VK_C);
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    setVisible(false);
                }
            });
            Box hb = Box.createHorizontalBox();
            hb.add(Box.createHorizontalGlue());
            hb.add(ok);
            hb.add(cancel);

            JPanel panel = new JPanel(new SpringLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            panel.setOpaque(true);

            panel.add(new JLabel("Select or define an assignee for",
                    SwingConstants.CENTER));
            panel.add(label);
            panel.add(new JLabel(" "));
            panel.add(jsp);
            panel.add(defineAssignee);
            panel.add(hb);

            SpringUtilities.makeCompactGrid(panel, 6, 1, 0, 0, 0, 0);
            this.add(panel);
            this.pack();
        }

        @Override
        protected void close() {
            // pass
        }

        /**
         * Helper method which returns the assignee the user selected or
         * defined. If text is added to the define-assignee text field and it
         * defines a new assignee it is created, added to the list of assignees,
         * and returned. Otherwise the selected element is returned. If nothing
         * is defined and no selection is made this method returns {@code null}.
         * 
         * @return an assignee, or {@code null}
         */
        private SWGSchematicAssignee getAssignee() {
            String ass = defineAssignee.getText();
            for (SWGSchematicAssignee as : currAssignees)
                if (as.getName().equals(ass))
                    return as; // no doubles

            if (ass != null && !ass.isEmpty() && !ass.equals(ASSIGNEE)) {
                SWGSchematicAssignee as = new SWGSchematicAssignee(ass);
                schemTab.assigneeAdd(as);
                return as;
            }

            int s = assigneeList.getSelectedIndex();
            if (s >= 0)
                return currAssignees.get(s);

            return null;
        }

        /**
         * Invoked when the OK button is selected. If a new assignee is defined
         * this method creates and adds it to SWGAide's storage. The specified
         * schematic is added as a favorite to the assignee.
         */
        @SuppressWarnings("synthetic-access")
        private void okAction() {
            assignee = getAssignee();
            if (assignee == null)
                return;

            assignee.addFavorite(currSchem);

            this.setVisible(false);
        }

        /**
         * Invoked when the user selects to add a favorite to an assignee. This
         * initiates this dialog with the specified arguments and displays it
         * for the user. If there is a previously selected assignee the
         * selection is preserved.
         * 
         * @param e the action that invokes this call
         * @param s a schematic
         * @param al a list of existing assignees
         */
        void show(MouseEvent e, SWGSchematic s, List<SWGSchematicAssignee> al) {
            // save these for OK button
            currAssignees = al;
            currSchem = s;

            String prev = defineAssignee.getText();
            prev = prev != null && !prev.isEmpty() && !prev.equals(ASSIGNEE)
                    ? prev
                    : (String) assigneeList.getSelectedValue();
            defineAssignee.setText(null);

            label.setText(String.format("\"%s\"", s.getName()));

            model.clear();
            for (SWGSchematicAssignee sa : al)
                model.addElement(sa.getName());

            if (currAssignees.isEmpty()) {
                defineAssignee.setText(ASSIGNEE);
                defineAssignee.selectAll();
                defineAssignee.requestFocus();
            } else
                assigneeList.setSelectedValue(prev, true);

            Point p = e.getLocationOnScreen();
            p.x = p.x - 75 < 0
                    ? 0
                    : p.x - 75;
            p.y = p.y - 150 < 0
                    ? 0
                    : p.y - 150;
            this.setLocation(p);

            this.setVisible(true);
        }
    }

    /**
     * A helper type that displays data for a resource slot or a component slot
     * that is set when a schematic is selected by the user. If the slot is set
     * to {@code null} this type does not display anything. This type listens
     * for user actions and invokes appropriate methods in the containing tab
     * for draft schematics.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    final class RCLabel extends JLabel {

        /**
         * The slot that is displayed by this label, or {@code null}. This is an
         * instance of {@link SWGResourceSlot} or {@link SWGComponentSlot}.
         */
        private Object slot;

        /**
         * Creates an instance of this type.
         */
        RCLabel() {
            this.setFont(SWGGuiUtils.fontPlain());
            this.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            this.addMouseListener(new MouseAdapter() {

                @SuppressWarnings("synthetic-access")
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        actionEastMouse(e, RCLabel.this);
                        return;
                    }
                    if (slot instanceof SWGComponentSlot) {
                        SWGComponentSlot cs = (SWGComponentSlot) slot;
                        if (cs.getType().equals("schematic")) {
                            SWGSchematic s = SWGSchematicsManager.getSchematic(
                                    cs.getSchematicId());
                            actionSchematicSelected(s, null, false);
                        } else if (cs.getType().equals("category")) {
                            displayCategoryPath(SWGSchematicsManager.
                                    getCategory(cs.getCategoryId()),
                                    traceForward);
                        }
                    }
                    setOpaque(false);
                }

                @SuppressWarnings("synthetic-access")
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (slot != null) {
                        setOpaque(true);
                        repaint();
                    }
                }

                @SuppressWarnings("synthetic-access")
                @Override
                public void mouseExited(MouseEvent e) {
                    if (slot != null) {
                        setOpaque(false);
                        repaint();
                    }
                }
            });
        }

        /**
         * Removes the content displayed at this label; the displayed slot and
         * the displayed text are set to {@code null}.
         */
        void eraseContent() {
            slot = null;
            setText(null);
        }

        /**
         * Sets the specified text at this label and updates the reference for
         * the specified slot. The slot must be an instance of
         * {@link SWGResourceSlot} or {@link SWGComponentSlot}.
         * <p>
         * <b>Special case</b>: To clear the content but let this component
         * retain a size at the GUI the string argument must be a white space
         * and the slot must be cleared; set both arguments to a white space.
         * 
         * @param str the string to set
         * @param slot the slot to display information for
         * @throws NullPointerException if an argument is {@code null}
         */
        void setContent(String str, Object slot) {
            if (str == null || slot == null)
                throw new NullPointerException("An argument is null");

            this.slot = !str.trim().isEmpty()
                    ? slot
                    : null;
            setText(str);
            if (slot instanceof SWGResourceSlot)
                this.setBackground(SWGGuiUtils.colorResource);
            else if (slot instanceof SWGComponentSlot) {
                SWGComponentSlot cs = (SWGComponentSlot) slot;
                if (cs.getType().equals("item"))
                    this.setBackground(SWGGuiUtils.colorItem);
                else if (cs.getType().equals("schematic"))
                    this.setBackground(SWGGuiUtils.colorComponent);
                else
                    this.setBackground(SWGGuiUtils.colorCategory);
            }
        }
    }

    /**
     * The model for the schematic selector which is a combo-box. In particular,
     * this type returns the name of the schematic to the combo-box, not the
     * schematic. However, clients can use {@link JComboBox#getSelectedIndex()}
     * and obtain the object from {@link SWGDraftTab#filteredSchematics} with
     * that index. This type and the GUI combo-box must only be accessed on the
     * event thread, which guarantees consistency.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    final class SchemSelectionModel extends DefaultComboBoxModel {

        /**
         * A copy of #filteredSchematics.
         */
        private List<SWGSchematic> schems;

        /**
         * Notifies listeners that content is changed; compare
         * {@link DefaultComboBoxModel#fireContentsChanged(Object, int, int)}.
         */
        @SuppressWarnings("synthetic-access")
        void fireContentChanged() {
            schems = new ArrayList<SWGSchematic>(filteredSchematics);
            Collections.sort(schems);
            super.fireContentsChanged(this, 0, this.getSize());
        }

        @Override
        public Object getElementAt(int index) {
            if (schems == null)
                return null;
            return schems.get(index).getName();
        }

        @Override
        public int getIndexOf(Object anObject) {
            if (schems == null)
                return 0;
            return schems.indexOf(anObject);
        }

        /**
         * Helper method which returns a schematic for the specified index, or
         * {@code null} if this model is empty.
         * 
         * @param index an index
         * @return a schematic
         * @throws IndexOutOfBoundsException if index is invalid
         */
        SWGSchematic getSchematic(int index) {
            if (schems == null)
                return null;
            return schems.get(index);
        }

        @Override
        public int getSize() {
            if (schems == null)
                return 0;
            return schems.size();
        }
    }

    /**
     * This type tracks which nodes in the tree of schematics and categories
     * that the user visits so that it is possible to navigate backward and
     * forward along the same path again. If the user unwinds and selects
     * another node than what is previously traced, the forward trace is cleared
     * and the new path is traced.
     * <p>
     * In most ways this type resembles a linked list, the user appends node by
     * node while just navigating "forward", navigating backward is just that
     * but the visited nodes are not removed. Anywhere along the trace but the
     * at the furthermost node, should the user select a node different than the
     * next forward-node it replaces the old node and a new trace begins. This
     * is the abstraction, the implementation is that all old forward-nodes are
     * ignored and eventually overwritten.
     * <p>
     * This type just piggy-backs on the super class. Hence, super class methods
     * must not be used, except {@code size()}, and {@code isEmpty()}. Valid
     * methods are:
     * 
     * <pre> existsBack() -> boolean
     * existsForward() -> boolean
     * isEmpty() -> boolean
     * peek() -> Object
     * peekFoward() -> Object
     * stepBackward() -> Object
     * size() -> int
     * stepForward(Object) </pre>
     * <p>
     * This type is not thread-safe but since it is supposed to be called only
     * on the event thread that should not be a problem.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    static class Tracer extends ZStack<Object> {

        // This implementation piggy-backs on ZStack with some tweaks.
        // The member "size" is abused as index for "current" which means
        // that anything between "size" and "ahead" is a recorded trace

        // Remember though:
        // size is one ahead of "current"
        // ahead is one ahead of further-most
        // slot at index 0 is initial slot, so minimum size is 1
        // initial is set at GUI creation time, always
        // ex 1: the user has taken just one step ever, above initial:
        // .... size = ahead = 2, current index is at 1
        // the user takes once step back:
        // .... size = 1, ahead = 2, current is index 0 (the initial)

        // XXX: detect repeated sequences and shrink, example:
        // S-A-B-C-D-A-B-C-D-E ---- shrink away elements 1 to 4 >> S-A-B-C-D-E

        /**
         * The index for the most advance trace, or equal to {@link #size()} if
         * there is nothing to advance to.
         */
        private int ahead;

        /**
         * Creates a tracer instance; see class JDoc comments on limitations.
         */
        Tracer() {
            super();
            ahead = 0;
        }

        /**
         * Determines if there is a trace to follow backward, determined from
         * the current position on the trace. This method returns {@code true}
         * if at least one step exists on the trace backward.
         * 
         * @return {@code false} if no trace backward exists
         */
        boolean existsBack() {
            return size() > 1;
        }

        /**
         * Determines if there is a trace to follow forward, determined from the
         * current position on the trace. This method returns {@code true} if at
         * least one step exists on the trace forward.
         * 
         * @return {@code false} if no trace forward exists
         */
        boolean existsForward() {
            return ahead > size();
        }

        /**
         * Returns the recorded node that exists one step ahead. This step
         * <b>does not</b> update the current position on the trace but if a
         * step is taken the client calls {@link #stepForward(Object)}.
         * 
         * @return the recorded node
         * @throw NoSuchElementException if no trace forward exists, compare
         *        {@link #existsForward()}
         */
        Object peekForward() {
            if (size() >= ahead)
                throw new NoSuchElementException("Exists no trace");

            return elements[size()];
        }

        /**
         * Takes a step backward on this trace and returns the recorded node.
         * This step updates the current position on the trace but does not
         * modify the trace.
         * 
         * @return the recorded node
         * @throw NoSuchElementException if no trace backward exists, compare
         *        {@link #existsBack()}
         */
        Object stepBackward() {
            if (size() < 2)
                throw new NoSuchElementException("Exists no trace");

            --size;
            return peek();
        }

        /**
         * The user takes a step forward and the selected node is added to this
         * trace. If a trace exists and the specified node does not equal this
         * trace forward the existing trace is cleared up to this point and the
         * specified node is recorded. This step updates the current record and
         * position on this trace.
         * 
         * @param node a node
         */
        void stepForward(Object node) {
            if (node != null) {
                int s = size();
                if (s >= ahead /* exists no further trace */
                        || elements[s] != node /* different trace */)
                    ahead = s + 1;

                push(node); // always safe, new or same trace alike
            }
        }
    }

    /**
     * This type is the model for the content at the used-in and find-result GUI
     * component.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    final class UsedInModel extends AbstractListModel {

        /**
         * The list of schematics for this model.
         */
        private SWGSchematic[] elements;

        /**
         * Returns the elements of this model as string, each element per line.
         * The returned string is the names of the displayed schematics, or an
         * empty string.
         * 
         * @return the content
         */
        ZString getAsText() {
            if (elements == null || elements.length <= 0) return ZString.EMPTY;

            ZString z = new ZString();
            for (int i = 0; i < elements.length; ++i)
                z.appnl(elements[i].getName());

            return z;
        }

        @Override
        public Object getElementAt(int index) {
            if (elements != null)
                return elements[index];

            return null;
        }

        @Override
        public int getSize() {
            if (elements != null)
                return elements.length;

            return 0;
        }

        /**
         * Sets the specified list of schematics as the content for this model.
         * It is a copy of the specified argument that is used.
         * 
         * @param elems a list of schematics
         * @throws NullPointerException if the argument is {@code null}
         */
        @SuppressWarnings("synthetic-access")
        void setElements(List<SWGSchematic> elems) {
            int old = this.getSize();
            old = old > 0
                    ? old - 1
                    : 0;
            elements = elems.toArray(new SWGSchematic[elems.size()]);
            usedinAndFind.removeSelectionInterval(0, old);
            fireContentsChanged(this, 0, old);
        }
    }
}
