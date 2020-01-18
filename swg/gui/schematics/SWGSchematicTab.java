package swg.gui.schematics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import swg.SWGAide;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGJDialog;
import swg.gui.resources.SWGResController;
import swg.model.SWGCGalaxy;
import swg.swgcraft.SWGCraftCache;
import swg.swgcraft.SWGResourceManager;
import swg.tools.ZNumber;

/**
 * This GUI component is for schematic related stuff, the root and the main
 * container of schematics GUI components. This implementation also provides
 * shared features and logic for its sub-components.
 * <p>
 * This implementation populates itself lazily, initially just a stub is
 * instantiated and not until the user selects the GUI tab named "Schematics"
 * more content and its main panel are created. At that time also the sub-panels
 * are initiated, maybe as stubs.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGSchematicTab extends JTabbedPane {

    /**
     * A string constant with the text "UNKNOWN" in HTML code that makes it
     * tinted red and in bold; the tags HTML themselves are not included.
     */
    final static String UNKNOWN = "<font color=\"red\"><b>UNKNOWN</b></font>";

    /**
     * A dialog for managing schematics assignees. This member is {@code null}
     * until a user requests the dialog.
     */
    private SWGJDialog assigneeDialog;

    /**
     * A menu item for the assignee dialog. This item is used for SWGAide's Edit
     * menu bar.
     */
    private JMenuItem assigneeMenuItem;

    /**
     * A list of schematics assignees, or {@code null} until instantiation.
     */
    private static List<SWGSchematicAssignee> assignees;

    /**
     * The controller instance for schematics in SWGAide.
     */
    final private SWGSchemController controller;

    /**
     * The main GUI component for the schematics viewer. This is the panel that
     * is first displayed for the user, displaying all draft schematics.
     */
    private SWGDraftTab draftSchems;

    /**
     * The frame for SWGAide.
     */
    final SWGFrame frame;

    /**
     * The URL for the main help page for schematics. This is the page which is
     * displayed when a sub-panel does not have its own help pages.
     */
    private URL helpPage;

    /**
     * A helper flag which denotes if this GUI component is completely created
     * or if it is in a pre-state, being just a initialized stub.
     */
    private boolean isGuiFinished = false;

    /**
     * A helper flag that denotes if we are updating the GUI or not.
     */
    private boolean isSelectingSchem;

    /**
     * The GUI element for finding best resources for a schematic.
     */
    private SWGLaboratoryTab laboratory;

    /**
     * A GUI element for resource class use per profession and per schematic.
     */
    private SWGResourceClassUse resClassUse;

    /**
     * A menu item for select-at-all-panels. This item is used for SWGAide's
     * Option menu bar.
     */
    final private JMenuItem schemSelectAlways;

    /**
     * The GUI element for best spawning resources versus resource inventory.
     */
    SWGTodays todaysAlert;

    /**
     * Creates an instance of this type, which is a GUI tabbed pane for
     * schematics.
     * 
     * @param frame the frame for SWGAide
     */

    public SWGSchematicTab(SWGFrame frame) {
        this.frame = frame;

        schemSelectAlways = schematicSelectAlwaysMenu();

        @SuppressWarnings("unchecked")
        // safe cast, created here first time too
        List<SWGSchematicAssignee> l = (List<SWGSchematicAssignee>)
                SWGFrame.getPrefsKeeper().get("schemAssignees",
                            new ArrayList<SWGSchematicAssignee>());
        assignees = l;
        controller = new SWGSchemController(this);

        // create interior lazily, see this#focusGained()
        frame.getTabPane().addChangeListener(new ChangeListener() {
            
            public void stateChanged(ChangeEvent e) {
                focusGained();
            }
        });
    }

    /**
     * Called when the user selects to manage schematics assignees. This method
     * just displays the instance of {@link SWGAssigneeDialog}.
     */
    private void actionAssigneeDialog() {
        SWGJDialog ad = assigneeDialog();
        ad.setVisible(true);
    }

    /**
     * Adds the specified assignee to the list of assignees. If the argument is
     * an assignee with a name that equals an assignee in the current list this
     * method does nothing. This method invokes {@link #notifyAssigneeUsers()}
     * and it is also thread safe.
     * 
     * @param assignee an assignee
     * @throws NullPointerException if the argument is {@code null}
     */
    void assigneeAdd(SWGSchematicAssignee assignee) {
        synchronized (assignees) {
            if (!assigneeIsUnique(assignee.getName())) return;

            assignees.add(assignee);
            Collections.sort(assignees);
            notifyAssigneeUsers();
        }
    }

    /**
     * Helper method that returns the dialog for managing schematics assignees.
     * If necessary this method creates an instance of the dialog.
     * 
     * @return a dialog for managing schematics assignees
     */
    SWGJDialog assigneeDialog() {
        if (assigneeDialog == null)
            assigneeDialog = new SWGAssigneeDialog(this);
        return assigneeDialog;
    }

    /**
     * Determines if the specified string is a unique name for a schematics
     * assignee.
     * 
     * @param str a suggested name
     * @return {@code true} if no name of an assignee equals the string
     */
    boolean assigneeIsUnique(String str) {
        synchronized (assignees) {
            for (SWGSchematicAssignee a : assignees)
                if (a.getName().equals(str)) return false; // no dbls

            return true;
        }
    }

    /**
     * Helper method which creates and returns a menu item for the
     * schematics-assignee dialog. The returned instance is an item which action
     * listener invokes {@link #actionAssigneeDialog()} that displays an
     * instance of {@link SWGAssigneeDialog}.
     * 
     * @return a menu item
     */
    JMenuItem assigneeMenu() {
        JMenuItem ad = new JMenuItem("Manage assignees...");
        ad.setToolTipText("Open a dialog to manage assignees and schematics");
        ad.setMnemonic(KeyEvent.VK_M);
        ad.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                actionAssigneeDialog();
            }
        });
        return ad;
    }

    /**
     * Removes the specified assignee from the list of assignees. If the current
     * list does not contain the argument this method does nothing. This method
     * is thread safe.
     * 
     * @param remove an assignee
     * @throws NullPointerException if the argument is {@code null}
     */
    void assigneeRemove(SWGSchematicAssignee remove) {
        synchronized (assignees) {
            String msg = null;
            int size = remove.getFavorites().size();
            if (size > 0)
                msg = "Assignee \"" + remove.getName() + "\" has " + size
                                + " favorite schematics\nDelete anyway?";
            else
                msg = "Delete assignee \"" + remove.getName() + "\" ?";

            if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                    this, msg, "Confirm", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE))
                return;

            boolean changed = false;
            Iterator<SWGSchematicAssignee> iter;
            for (iter = assignees.iterator(); iter.hasNext();) {
                if (iter.next().getName().equals(remove.getName())) {
                    iter.remove();
                    changed = true;
                    break;
                }
            }
            if (changed) notifyAssigneeUsers();
        }
    }

    /**
     * Returns a sorted copy of the list of schematics assignees. The list is
     * alphabetically sorted and can be modified. This method is thread safe.
     * 
     * @return a list of assignees
     */
    static List<SWGSchematicAssignee> assignees() {
        synchronized (assignees) {
            return new ArrayList<SWGSchematicAssignee>(assignees);
        }
    }

    /**
     * Helper method which is invoked when the user selects any one of the tabs
     * in this tabbed pane. This implementation updates the help page viewer and
     * creates the GUI if it is not yet completed.
     */
    private void focusGained() {
        if (frame.getTabPane().getSelectedComponent() == this) {
            if (!isGuiFinished) make();

            verifyCharacterSelected();
            SWGSchemController.updateGalaxy();

            SWGHelp.push(helpPage);

            frame.editMenuAdd(assigneeMenuItem);
            frame.optionsMenuAdd(schemSelectAlways);
        } else {
            SWGHelp.remove(helpPage);
            frame.editMenuRemove(assigneeMenuItem);
            frame.optionsMenuRemove(schemSelectAlways);
        }

        if (isGuiFinished) {
            draftSchems.focusGained(); // always
            laboratory.focusGained();
            todaysAlert.focusGained();
            resClassUse.focusGained();
            tintTabs(false);
        }
    }

    /**
     * Helper method which completes the creation of this GUI component. This
     * instance already exists as a stub, this method completes this component,
     * instantiates its sub-panels and adds them to this tabbed pane. Sub-panels
     * may in their turn be instantiated as stubs and populated lazily.
     */
    private synchronized void make() {
        if (isGuiFinished) return;

        setOpaque(true);
        helpPage = SWGAide.class.getResource("docs/help_schematics__en.html");

        draftSchems = new SWGDraftTab(this);
        laboratory = new SWGLaboratoryTab(this);
        todaysAlert = new SWGTodays(this);
        resClassUse = new SWGResourceClassUse(this);

        // add them after all are created, not to trigger GUI action
        // events until all are at least a pre-state

        add("Draft Schematics", draftSchems);
        add("The Laboratory", laboratory);
        add("Today's Alert", todaysAlert);
        add("Resource Class Use", resClassUse);

        this.setMnemonicAt(0, KeyEvent.VK_D);
        this.setMnemonicAt(1, KeyEvent.VK_L);
        this.setMnemonicAt(2, KeyEvent.VK_T);
        this.setMnemonicAt(3, KeyEvent.VK_U);

        assigneeMenuItem = assigneeMenu();

        SWGResourceManager.addSubscriber(controller);

        isGuiFinished = true;
    }

    /**
     * Notifies users of the collection of assignees that it is modified, or
     * that any of its elements is modified.
     * <p>
     * <b>Notice:</b> This method should be called by any client that modifies
     * the collection of assignees or one of its elements.
     */
    void notifyAssigneeUsers() {
        laboratory.focusGained();
        todaysAlert.focusGained();
    }

    /**
     * Selects the specified resource class at the panel for resource class use.
     * If the argument is {@code null} this method does nothing. This method
     * resets any selected profession at the target GUI panel and selects the
     * resource class.
     * 
     * @param rc a resource class constant
     */
    void resClassSelect(SWGResourceClass rc) {
        if (rc != null) {
            if (resClassUse == null) make();

            setSelectedComponent(resClassUse);
            resClassUse.resClassSelect(rc);
        }
    }

    /**
     * Creates and returns a menu item with an action listener for the
     * schematics section and its panel for resource class use. If the specified
     * resource class is {@code null} the menu item is disabled, otherwise, if
     * the argument is specified the resource class is selected.
     * 
     * @param rc a resource class constant, or {@code null}
     * @return a menu item
     */
    JMenuItem resClassUse(final SWGResourceClass rc) {
        JMenuItem m = new JMenuItem("Select schematic use");
        m.setToolTipText(String.format(
                "Select \"%s\" at Schematics: Resource Class Use", rc == null
                        ? "none"
                        : rc.rcName()));
        m.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resClassSelect(rc);
            }
        });
        m.setEnabled(rc != null);
        return m;
    }

    /**
     * Selects the specified schematic at the draft schematics and the
     * laboratory panels. If the schematic is {@code null} this method does
     * nothing; if {@code src == null} all panels are invoked, otherwise the
     * source is assumed to be what triggers this invocation and the source
     * itself is not invoked. This method silently overrides any filters but
     * does not reset or change them; if necessary the panels are ready created.
     * <p>
     * The user can opt against this "auto-select" and then this method does
     * nothing.
     * 
     * @param s a schematic
     * @param src the source of the invocation, or {@code null}
     */
    void schematicSelect(SWGSchematic s, JComponent src) {
        schematicSelect(s, src, schemSelectAlways.isSelected());
    }

    /**
     * Selects the specified schematic at the draft schematics and the
     * laboratory panels.
     * 
     * @param s a schematic
     * @param src the source of the invocation, or {@code null}
     * @param go {@code false} to ignore the invocation
     */
    private void schematicSelect(SWGSchematic s, JComponent src, boolean go) {
        if (!isSelectingSchem && isGuiFinished && go && s != null) {
            isSelectingSchem = true;

            if (src != draftSchems) draftSchems.schemSelect(s);
            if (src != laboratory) laboratory.schemSelect(s);
            isSelectingSchem = false;
        }
    }

    /**
     * Creates and returns a menu check box which toggles the option to always
     * select a schematic at GUI panels. The default value is {@code true} which
     * makes {@link #schematicSelect(SWGSchematic, JComponent)} to always be
     * invoked when the user selects a schematic at any panel.
     * 
     * @return a menu item
     */
    private JMenuItem schematicSelectAlwaysMenu() {
        boolean b = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "schemSelectSchemAlways", Boolean.TRUE)).booleanValue();
        final JCheckBoxMenuItem m = new JCheckBoxMenuItem("Always select", b);
        m.setToolTipText("Always select a schematic at other panels");
        m.setMnemonic(KeyEvent.VK_A);
        m.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("schemSelectSchemAlways",
                        Boolean.valueOf(m.isSelected()));
            }
        });
        return m;
    }

    /**
     * Creates and returns a menu item for selecting a specified schematic at
     * select schematics GUI panels. If the specified schematic is {@code null}
     * the menu item is disabled. If {@code src == null} all panels are invoked,
     * otherwise the source is assumed to be what triggers this invocation and
     * the source itself is not invoked. This method silently overrides any
     * filters but does not reset or change them; if necessary the panels are
     * ready created.
     * <p>
     * Use this menu item for use within the Schematics section.
     * 
     * @param s a schematic
     * @param src the source of the invocation, or {@code null}
     * @return a menu item
     */
    JMenuItem schematicSelectMenu(final SWGSchematic s, final JComponent src) {
        JMenuItem m = new JMenuItem("Select schematic");
        m.setToolTipText("Select schematic at other panels");
        m.setMnemonic(KeyEvent.VK_S);
        if (s != null)
            m.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!isGuiFinished) make();
                    schematicSelect(s, src, true);
                }
            });
        m.setEnabled(s != null);
        return m;
    }

    /**
     * If Today's Alert has something to display this method tints the tab for
     * this component at the main tabbed pane and the tab for Today's Alert at
     * this tabbed pane. The boolean argument may trigger an update of the GUI
     * of Tday's Alert, if it is not fully created it is ignored.
     * 
     * @param updateGui {@code true} to also trigger a GUI update
     */
    void tintTabs(boolean updateGui) {
        if (SWGAide.frame().getTabPane().getComponentCount() >= 3) {
            if (todaysAlert != null) {
                if (updateGui) todaysAlert.guiUpdate();
                setBackgroundAt(2, SWGTodays.todaysTinted()
                        ? SWGGuiUtils.colorAlert
                        : null);
            }
            SWGAide.frame().getTabPane().setBackgroundAt(3,
                    SWGTodays.todaysTinted()
                            ? SWGGuiUtils.colorAlert
                            : null);
        }
    }

    /**
     * Helper method that updates SWGAide's status bar 2 with the most recent
     * date for downloaded schematics.
     */
    void updateStatbar2() {
        int nbr = SWGSchematicsManager.getAmount();
        LocalDateTime ld = SWGCraftCache.localDate(SWGCraftCache.schemXML);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern( "uuuu-MM-dd" );
        frame.putToLogbar_2(String.format(
                "%s schematics from swgaide.com (%s)",
                ZNumber.asText(nbr, true, true), ld.format(fmt)));
    }

    /**
     * Helper method which updates this type. If a character is selected at the
     * main panel this method updates the temporary collections of spawning
     * resources and inventory resources for the galaxy that pertains to the
     * character; this is always done in the case the user adds resources to the
     * inventory. Otherwise, if no character is selected this method displays a
     * dialog to warn the user.
     */
    private void verifyCharacterSelected() {
        if (SWGFrame.getSelectedCharacter() == null)
            JOptionPane.showMessageDialog(this,
                    "No character selected at main panel",
                    "Unknown character & galaxy", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Returns a string with the text used for draft schematics determined by
     * the complexity. The text is about the crafting tool and/or the crafting
     * station. If the specified complexity is &le; 0 {@link #UNKNOWN} is
     * returned.
     * 
     * @param complexity the complexity of a schematic
     * @return a string
     */
    static String complexityToolStation(int complexity) {
        if (complexity <= 0) return UNKNOWN;
        if (complexity <= 15) return "Generic";
        if (complexity <= 20) return "Specialized";
        if (complexity <= 25) return "Spec+Public";
        // else
        return "Spec + Private";
    }

    /**
     * Creates and returns a menu item for finding a schematic from text. The
     * returned menu item has no action listener.
     * 
     * @return a menu item
     */
    static JMenuItem findSchematicMenu() {
        JMenuItem find = new JMenuItem("Find...", KeyEvent.VK_F);
        find.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        find.setToolTipText("Find schematics with some text in their names");
        return find;
    }

    /**
     * Creates and returns a check-box menu item for the option to disable menu
     * options related to guards. If this item is enabled options related to
     * guards should be disabled if a matching guard exists, compare with
     * {@link SWGResController#guardExist(SWGResourceClass, swg.crafting.SWGValues, SWGCGalaxy)}
     * <p>
     * The returned menu item, if the user selects it (toggles the value) it is
     * stored in SWGAide's DAT file under the specified key.
     * 
     * @param enabled {@code true} if the returned check-box is enabled
     * @param key the key for the changed value
     * @return a menu item
     */
    static JMenuItem guardDisableOptionsMenu(boolean enabled, final String key) {
        final JMenuItem dis = new JCheckBoxMenuItem(
                "Disable guard menu", enabled);
        dis.setToolTipText("Disable guard menu-options if matching guard exists");
        dis.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ee) {
                boolean b = dis.isSelected();
                SWGFrame.getPrefsKeeper().add(key, Boolean.valueOf(b));
            }
        });
        return dis;
    }

    /**
     * Returns the specified integer as string or {@link #UNKNOWN}. If the
     * argument is &le; 0 the constant string is returned, otherwise the integer
     * value as string.
     * 
     * @param i an integer
     * @return the string or {@link #UNKNOWN}
     */
    static String intOrUnknown(int i) {
        return i <= 0
                ? UNKNOWN
                : ZNumber.asText(i, 1, 0);
    }

    /**
     * Creates a radio-button menu option for the specified arguments. This
     * method adds the item to the button-group and to the popup menu, and adds
     * the action listener to the created menu item. Determined by the integer
     * value the menu item is set-selected, the item is selected if
     * 
     * <pre>
     * title.equals("Only LQ") && lqAhq < 0 ||
     * title.equals("All") && lqAhq == 0 ||
     * title.equals("Only HQ") && lqAhq > 0 </pre>
     * 
     * @param title the title for the button and its action-command
     * @param ac an action listener
     * @param bg a button group
     * @param popup a popup menu
     * @param lqAhq an integer that determines if the item is selected
     */
    static void schemRadioButtonMenu(String title, ActionListener ac,
            ButtonGroup bg, JPopupMenu popup, int lqAhq) {

        JRadioButtonMenuItem rb = new JRadioButtonMenuItem(title);
        bg.add(rb);
        if ((title.equals("Only LQ") && lqAhq < 0)
                || (title.equals("All") && lqAhq == 0)
                || (title.equals("Only HQ") && lqAhq > 0))
            rb.setSelected(true);
        rb.addActionListener(ac);
        rb.setToolTipText(String.format("Display \"%s\" schematics", title));
        rb.setActionCommand(title);
        popup.add(rb);
    }

    /**
     * Returns the specified string or {@link #UNKNOWN}. If the argument is
     * {@code null} the constant string is returned, otherwise the argument is
     * returned as is.
     * 
     * @param str a string
     * @return the string or {@link #UNKNOWN}
     */
    static String stringOrUnknown(String str) {
        return str == null
                ? UNKNOWN
                : str;
    }
}
