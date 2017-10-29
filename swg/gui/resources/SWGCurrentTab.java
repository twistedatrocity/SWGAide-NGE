package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import swg.SWGAide;
import swg.SWGConstants;
import swg.crafting.SWGValues;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGPlanetAvailabilityInfo;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceClassTree;
import swg.crafting.resources.SWGResourceFilter;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.SWGWeightComparator;
import swg.crafting.resources.types.SWGCreatureResources;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGDocNumberFilter;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGJTable;
import swg.gui.common.SWGResourceStatRenderer;
import swg.gui.common.SWGTextInputDialogue;
import swg.gui.common.SWGDecoratedTableCellRenderer.DecoratedTableModel;
import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;
import swg.gui.common.SWGTextInputDialogue.TextValidation;
import swg.gui.schematics.SWGSchemController;
import swg.gui.schematics.SWGSchemResViewer;
import swg.model.SWGCGalaxy;
import swg.model.SWGCharacter;
import swg.model.SWGNotes;
import swg.model.SWGStation;
import swg.swgcraft.SWGPets;
import swg.swgcraft.SWGResourceManager;
import swg.tools.ColorChooser;
import swg.tools.SpringUtilities;
import swg.tools.ZHtml;
import swg.tools.ZNumber;
import swg.tools.ZReader;
import swg.tools.ZString;
import swg.tools.ZStuff;
import swg.tools.ZWriter;

/**
 * This GUI component is the first panel that users will see on the tab which is
 * named "Resources". This component contains the main table of resources which
 * are spawning at the current galaxy. Furthermore it contains sub-panels for
 * resource info, resource guards and filters, and resource monitors. Many GUI
 * actions which are available at this component are redirected to its siblings
 * at other tabs at the main tabbed panel.
 * <P>
 * This component trusts that {@link SWGResourceTab} manages items such as when
 * the user is switching between galaxy, provides a set of spawning resources,
 * and notifies this component when there is a change.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGCurrentTab extends JPanel implements ActionListener {

    /**
     * A filtered set of resources to display at the main GUI table. This list
     * may be filtered on resource class, values, weights, or resource guards.
     */
    private SWGResourceSet currentList;

    /**
     * The table model for main table of current resources.
     */
    private ResourcesTableModel currentResourcesModel;

    /**
     * The main GUI table of current resources.
     */
    private SWGJTable currentResourcesTable;

    /**
     * A flag which denotes if resource-stat coloring based on resource class is
     * disabled. Default is {@code false}.
     */
    private boolean disableColoring;

    /**
     * The button for clearing the filters at the filter panel.
     */
    private JButton filterClearButton;

    /**
     * A check box so the user can select to consider caps or not while
     * filtering resources.
     */
    private JCheckBox filterConsiderCaps;

    /**
     * A check box so the user can select how a missing <I>expected
     * value&nbsp;</I> should be handled while filtering resources.
     */
    private JCheckBox filterConsiderNoStat;

    /**
     * The text fields on the filter panel for the resource stats. This array
     * contains the stats in the order of the model, not in in-game order.
     * However, at the GUI they are displayed in in-game order.
     */
    private final JTextField[] filterFields = new JTextField[Stat.COUNT];

    /**
     * A list of resource guards for the current galaxy.
     */
    private List<SWGGuard> guardsList;

    /**
     * The underlying table model for the table of guards.
     */
    private GuardsModel guardsModel;

    /**
     * The GUI table for the resource guards.
     */
    private SWGJTable guardsTable;

    /**
     * The URL for the help page for this view.
     */
    private final URL helpPage;

    /**
     * Internal flag which denotes if event processing should be temporarily
     * blocked. The use case is when a process triggers an action on a GUI
     * component which implicitly will spawn an event to update the GUI, an
     * event that should be deferred until the initial process has finished its
     * job. If this field is {@code true} some methods do nothing and silently
     * return. The default value is {@code false}, nothing is blocked.
     */
    private boolean isBlocking = false;

    /**
     * A helper flag which indicates if this GUI component is completely created
     * or if it is in a pre-state, having only a stub initialized.
     */
    private boolean isGuiFinished = false;

    /**
     * A helper flag which indicates if {@link #tintTab(char, boolean)} is
     * processing, which may trigger successive calls that should be dodged.
     */
    private boolean isTinting;

    /**
     * A text input field where the user can set an age in hours for which
     * resources should be displayed at the main panel, compare {code
     * maxAgeToggle}.
     */
    private JTextField maxAgeHours;

    /**
     * A check box so the user can select if a user specified age should be
     * considered for the displayed resources, compare {code maxAgeHours}.
     */
    private JCheckBox maxAgeToggle;

    /**
     * A list of resource monitors for the current galaxy.
     */
    private List<SWGMonitor> monitorsList;

    /**
     * The underlying table model for the table of monitors.
     */
    private MonitorModel monitorsModel;

    /**
     * The GUI table of resource monitors.
     */
    private SWGJTable monitorsTable;

    /**
     * The GUI tabbed panel for the details view, guards, monitors, and possibly
     * more.
     */
    private JTabbedPane northEastTabbedPane;

    /**
     * The tree view of the resource classes. This view is displayed at the
     * north-west area of this component, where the user can narrow down and
     * filter the display on a resource class.
     */
    private JTree resourceClassTree;

    /**
     * A text area for presenting details about a selected resource.
     */
    private JTextArea resourceDetails;

    /**
     * The tabbed pane containing this object. This is the main component named
     * "Resources" at SWGAide's main tabbed pane.
     */
    private final SWGResourceTab resourceTab;

    /**
     * A selected resource guards to filter for, or {@code null} if none is
     * selected.
     */
    private SWGGuard selectedGuard;
    /**
     * A selected resource used to retain selections at the current table and to
     * display details at the details view, or {@code null} if none is selected.
     */
    private SWGKnownResource selectedResource;

    /**
     * A selected resource class to filter for, or {@code null} if none is
     * selected.
     */
    private SWGResourceClass selectedResourceClass;

    /**
     * Integer values at the filter input fields.
     */
    private int[] temporaryFilterStats;

    /**
     * A weight for computing the "rate" column at the main GUI table.
     */
    private SWGWeights temporaryWeight;

    /**
     * A flag that denotes if this panel may update the schematic-resource
     * viewer continuously. Default is {@code false}.
     */
    private boolean updateViewer;

    /**
     * Creates an instance of this type which is the main table of spawning
     * resources for the selected galaxy, and more.
     * 
     * @param resourceTab the tabbed pane that contains this object
     */
    SWGCurrentTab(final SWGResourceTab resourceTab) {
        this.resourceTab = resourceTab;

        helpPage = SWGAide.class.getResource("docs/help_resources__en.html");

        setLayout(new BorderLayout());

        add(makeNorthPanel(), BorderLayout.PAGE_START);
        add(new JScrollPane(makeMainTable(),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER);
        add(makeFilterPanel(), BorderLayout.PAGE_END);

        resourceTab.addChangeListener(new ChangeListener() {

            @SuppressWarnings("synthetic-access")
            public void stateChanged(ChangeEvent e) {
                if (isGuiFinished)
                    focusGained(true);
            }
        });
        isGuiFinished = true;
    }

    /**
     * Called when the user selects to add the specified resource to the list of
     * defined harvesters for the current galaxy. This method dispatches to
     * {code harvesterActivate(SWGKnownResource)}.
     * 
     * @param resource the selected resource
     */
    private void actionCurrentAddToHarvester(SWGKnownResource resource) {
        resourceTab.harvesterActivateDialog().showAndBegin(
                null, resource, selectedGuard == null
                        ? null
                        : selectedGuard.getName());
    }

    /**
     * Called when a user clicks the main table of spawning resources. This
     * method dispatches to {code currentTableShowPopup(int, MouseEvent)}
     * 
     * @param e the event that initiates the call
     */
    private void actionCurrentMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            int row = currentResourcesTable.rowAtPoint(e.getPoint());
            if (row < 0 || row >= currentGet().size())
                return; // sanity

            currentResourcesTable.setRowSelectionInterval(row, row);
            row = currentResourcesTable.convertRowIndexToModel(row);
            
            currentShowPopup(row, e);
        }
    }

    /**
     * Called when the user selects a resource at the main table. This method
     * builds a blob of textual data for the resource and displays the result on
     * {code resourceDetails}.
     */
    private void actionCurrentSelect() {
        int row = currentResourcesTable.getSelectedRow();
        if (row < 0) {
            resourceDetails.setText(null);
            selectedResource = null;
            return;
        }

        row = currentResourcesTable.convertRowIndexToModel(row);
        selectedResource = currentGet().get(row);

        String s = SWGResController.resourceDetails(
                selectedResource, SWGResourceTab.galaxy());

        resourceDetails.setText(s);
        resourceDetails.setCaretPosition(0);

        if (updateViewer)
            updateViewer = SWGSchemResViewer.updateDisplay(
                    selectedResource, this);
    }

    /**
     * Called when the user selects to create a new resource guard. This method
     * raises {@link SWGGuardDialog} and returns.
     * 
     * @param resClass the resource class to create a guard for, or {@code null}
     */
    private void actionGuardCreate(SWGResourceClass resClass) {
        SWGResController.guardsDialog().showAndBegin(
                resClass, null, SWGResourceTab.galaxy());
    }

    /**
     * Called when the user selects to delete a resource guard. This method
     * raises a GUI confirmation dialog, deletes the guard, and refreshes the
     * GUI.
     * 
     * @param guard the guard to delete
     */
    private void actionGuardDelete(SWGGuard guard) {
        String msg = String.format(
                "Delete resource guard: \"%s\"", guard.getName());

        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                northEastTabbedPane, msg, "Verify delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {

            SWGResController.guardsRemove(guard, SWGResourceTab.galaxy());
        }
    }

    /**
     * Called when the user selects to edit a resource guard. This method raises
     * {@link SWGGuardDialog} and returns.
     * 
     * @param grd the guard to edit
     */
    private void actionGuardEdit(SWGGuard grd) {
        SWGResController.guardsDialog().showAndBegin(
                null, grd, SWGResourceTab.galaxy());
    }

    /**
     * Called when the user selects to delete all resource guards for the
     * current galaxy. This method raises a GUI confirmation dialog, clears the
     * list of resource guards, and refreshes the GUI.
     */
    private void actionGuardsDeleteAll() {
        SWGGuard theSelected = selectedGuard;
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                northEastTabbedPane, "Delete all resource guards",
                "Confirm deletion", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE)) {

            SWGResController.guards(SWGResourceTab.galaxy()).clear();
            resetGuards();

            if (theSelected != null)
                updateCurrent(); // a guard was selected >> refresh main table
        }
    }

    /**
     * Called when the user selects a guard using the keyboard. This method is
     * the primary action selection listener for the table of resource guards
     * and it dispatches to {@code guardSelection()} and if that method returns
     * {@code true} this method triggers a refresh of the table of current
     * resources.
     * 
     * @param e the event that initiates the call
     */
    private void actionGuardSelect(ListSelectionEvent e) {
        if (e == null || !e.getValueIsAdjusting())
            if (guardSelection())
                updateCurrent();
    }

    /**
     * Called when the user selects to import resource guards from file. This
     * method raises a file chooser dialog and dispatches to {code
     * guardsReadFile(File)}.
     */
    private void actionGuardsImportFile() {
        JFileChooser fc = SWGFrame.getFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (fc.showOpenDialog(guardsTable) == JFileChooser.APPROVE_OPTION)
            guardsReadFile(fc.getSelectedFile());
    }

    /**
     * Called when the user selects to edit the max age for resources which will
     * trigger resource guards. This method raises a GUI dialog handling the
     * interaction. The selected value is stored in SCGAide's preference keeper
     * and the list of guards is reset.
     * 
     * @param xy the location for the GUI dialog
     * @param currentMaxAge the current limit, or {@code null} if none is set
     */
    private void actionGuardsMaxAgeDialog(Point xy, Integer currentMaxAge) {
        String msg =
                String.format("Define the max age for resources which will%n"
                        + "trigger guards, in hours, minimum 12 hours.%n%s",
                        currentMaxAge == null
                                ? "Currently unlimited"
                                : String.format("The current limit: %d hours",
                                        currentMaxAge));

        SWGTextInputDialogue diag =
                new SWGTextInputDialogue(SWGAide.frame(),
                        new TextValidation() {
                            public boolean validateText(String text) {
                                try {
                                    if (ZNumber.intExc(text) >= 12)
                                        return true;
                                } catch (Exception e) {
                                    Toolkit.getDefaultToolkit().beep();
                                }
                                return false;
                            }
                        }, xy, "Resource trigger age", msg, "");

        diag.setVisible(true);

        String input = diag.getTypedText();
        if (input != null) {
            Integer inp = Integer.valueOf(input.trim());

            if (!inp.equals(currentMaxAge)) {
                SWGFrame.getPrefsKeeper().add("resourceGuardsMaxHours", inp);
                SWGResController.check();
            }
        } // else the user dismissed the dialog >> no action
    }

    /**
     * Called when the user mouse-clicks a resource guard. This method
     * dispatches to {code guardsShowPopup(int, MouseEvent)}.
     * 
     * @param e the event that initiates the call
     */
    private void actionGuardsMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            guardsShowPopup(e);
        }
    }

    /**
     * Called when the user selects to save all guards for the current galaxy to
     * file.
     */
    private void actionGuardsWriteFile() {
        List<SWGGuard> grds = SWGResController.guards(SWGResourceTab.galaxy());

        File file = new File(String.format(
                "crafting/%s/guards.csv", SWGResourceTab.galaxy().getName()));
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            SWGAide.printError("SWGCurrentResTab:guardsWriteFile",
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
            guardsWriteHeader(wr);
            for (SWGGuard grd : grds)
                guardsWriteGuard(wr, grd);
            wr.close();

            msg = String.format("%s guards saved to%n%s%s",
                    ZNumber.asText(grds.size(), true, true),
                    file.getAbsolutePath(), bak);
        } catch (Throwable e) {
            SWGAide.printError("SWGCurrentTab:actionGuardsWriteFile", e);
            msg = "Error, see log files";
        }
        JOptionPane.showMessageDialog(northEastTabbedPane,
                    msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Called from document listeners when the user edits {code maxAgeHours}.
     * This method stores the user input to SWGAide's preference keeper and
     * resets the table of spawning resources.
     */
    private void actionMaxAge() {
        if (!isGuiFinished)
            return;
        String s = maxAgeHours.getText();
        SWGFrame.getPrefsKeeper().add("resourceFilterMaxHours", s);
        updateCurrent();
    }

    /**
     * Called when the user selects to edit a comment for a monitor. This method
     * displays a modal dialog with the current user notes, if any, and if the
     * user closes the dialog with "OK" the content replaces the old notes.
     * 
     * @param monitor a monitor
     */
    private void actionMonitorNotes(SWGMonitor monitor) {
        String n = SWGGuiUtils.dialogTextInput(
                monitorsTable, "Edit monitor notes", monitor.notes());

        if (n != null) monitor.notes(n);
    }

    /**
     * Called when the user clicks the table of resource monitors. If it is a
     * left-click this method clears any selected guard or resource-class and
     * selects the current resource that the monitor pertains to. If it is a
     * right-click this method invokes {code monitorsShowPopup(int, e)}.
     * 
     * @param e the event that triggers the call
     */
    private void actionMonitorsMouse(MouseEvent e) {
        int row = monitorsTable.rowAtPoint(e.getPoint());
        if (row < 0)
            return;

        int b = e.getButton();
        if (b == MouseEvent.BUTTON3) {
            monitorsTable.setRowSelectionInterval(row, row);
            monitorsShowPopup(row, e);

        } else if (b == MouseEvent.BUTTON1) {
            int rm = monitorsTable.convertRowIndexToModel(row);
            SWGMonitor m = monitorsGet().get(rm);
            if (!m.getResource().isDepleted()) {
                resetTab();
                
                currentSelect(m.getResource());
                monitorsTable.setRowSelectionInterval(row, row);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (!isGuiFinished)
            return;
        Object source = e.getSource();

        if (source == filterClearButton) {
            resetTab();
            return;
        }

        if (source == filterConsiderCaps)
            SWGFrame.getPrefsKeeper().add("resourceFilterFieldCaps",
                    Boolean.valueOf(filterConsiderCaps.isSelected()));
        else if (source == filterConsiderNoStat)
            SWGFrame.getPrefsKeeper().add("resourceConsiderNoStat",
                    Boolean.valueOf(filterConsiderNoStat.isSelected()));
        else if (source == maxAgeToggle)
            SWGFrame.getPrefsKeeper().add("resourceFilterUseMaxAge",
                    Boolean.valueOf(maxAgeToggle.isSelected()));

        updateDisplay();
    }

    /**
     * Called when the user clicks the right-hand mouse button at a node at the
     * resource class tree. This method dispatches to {code
     * resourceClassTreeShowPopup(MouseEvent)}.
     * 
     * @param e the event that initiates the call
     */
    private void actionResourceClassTreeMouse(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            resourceClassTreeShowPopup(e);
        }
    }

    /**
     * Called when the user selects a node at the resource class tree. This
     * method is the primary action selection listener for the resource class
     * tree and it dispatches to {@code resourceClassSelection()} and if that
     * method returns {@code true} this method triggers a refresh of the table
     * of current resources.
     */
    private void actionResourceClassTreeSelect() {
        if (resourceClassSelection())
            updateCurrent();
    }

    /**
     * Called when the user selects a stat color chooser button. This method
     * raises a GUI color chooser initiated with the current colors, the
     * selected colors are stored in SWGAide's preference keeper, and finally
     * the display is reset for the colors to take effect.
     * 
     * @param button the button which initiated this action
     */
    private void actionStatColorChooserButton(JButton button) {
        String str = "resourceColor" + button.getText();
        Color bg = (Color) SWGFrame.getPrefsKeeper().get(str);
        Color fg = (Color) SWGFrame.getPrefsKeeper().get(str + "Text");

        ColorChooser cc = SWGAide.frame().getColorChooser();
        cc.showDialog(bg, fg);

        bg = cc.getSelectedBackgroundColor();
        fg = cc.getSelectedForegroundColor();
        button.setBackground(bg);
        button.setForeground(fg);

        SWGFrame.getPrefsKeeper().add(str, bg);
        SWGFrame.getPrefsKeeper().add(str + "Text", fg);

        SWGGuiUtils.statColorLimitSet();
        currentResourcesModel.fireTableDataChanged();
    }

    /**
     * Called when the user edits a text input field for a limit of cell
     * coloring. That is the lower limit for when a particular color is used for
     * "fair", "good", and "great" stats. The edited value is stored in
     * SWGAide's preference keeper, and finally the display is reset for the
     * colors to take effect.
     * 
     * @param statLimitField the text field which initiates this action
     */
    private void actionStatLimitField(JTextField statLimitField) {
        String txt = statLimitField.getText();
        int limit = txt.isEmpty()
                ? 0
                : Integer.parseInt(txt);
        if (limit <= 0)
            return;

        String str = "resourceLimit" + statLimitField.getName();
        SWGFrame.getPrefsKeeper().add(str, new Integer(limit));
        SWGGuiUtils.statColorLimitSet();
        currentResourcesModel.fireTableDataChanged();
    }

    /**
     * Called when the user selects to write the specified resource to a notes
     * file named "news.txt". This method obtains or creates such a file in the
     * currently selected station and writes details similar to the Details
     * window to file. If {@code append} this method appends the details to the
     * file, otherwise any content is overwritten.
     * 
     * @param kr a resource
     * @param append {@code true} to append to the file
     */
    private void actionWriteNews(SWGKnownResource kr, boolean append) {
        SWGCharacter ch = SWGFrame.getSelectedCharacter();
        if (ch == null) {
            JOptionPane.showMessageDialog(resourceDetails,
                    "Select a character at the Main window",
                    "Unknown character", JOptionPane.ERROR_MESSAGE);
            return; // sanity
        }
        ZString z = new ZString();
        if (!append) {
            z.app("SWGAide -- SWGCraft.org -- ");
            z.appnl(ZStuff.dateString(true)).nl();
        }
        SWGResController.resourceDetails(kr, z);

        SWGStation s = ch.galaxy().station();
        SWGNotes notes = s.notes("news.txt");
        if (notes == null)
            notes = SWGNotes.getInstance("news.txt", s, null, false);

        if (kr.rc().isSub(SWGCreatureResources.class))
            z.app("Harvested from: ").
                    appnl(SWGPets.creatures((SWGCreatureResources) kr.rc()));

        if (append) {
            notes.add(z.nl().toString(), true);
        } else {
            notes.erase(false);
            notes.setText(z.toString());
        }
    }

    /**
     * Helper method. If {code maxAgeToggle} is selected this method returns a
     * set of resources in which all elements are younger than the the specified
     * number of hours. Otherwise the specified argument is returned as-is.
     * 
     * @param resources a set of resources to filter
     * @return a set of resources filtered on age, or the supplied argument
     */
    private SWGResourceSet currentFilterOnAge(SWGResourceSet resources) {
        long max;
        if (maxAgeToggle.isSelected()
                && (max = ZNumber.longVal(maxAgeHours.getText())) > 0) {

            SWGResourceSet c = new SWGResourceSet(resources.size());
            long min = (System.currentTimeMillis() / 1000) - (max * 3600);
            for (SWGKnownResource r : resources)
                if (r.availableFirst().available() > min)
                    c.add(r);

            return c;
        }
        return resources;
    }

    /**
     * Filters for the specified resource class at the table of current
     * resources. If the resource class is {@code null} this method does
     * nothing. If experimental weights are specified the result is sorted by
     * the computed weights. If a known resource is specified and is part of the
     * filtered set it is selected.
     * 
     * @param rc a resource class constant
     * @param wg experimental weights, or {@code null}
     * @param kr a resource to selected, or {@code null}
     */
    void currentFilterOnClass(
            SWGResourceClass rc, SWGWeights wg, SWGKnownResource kr) {

        if (rc != null) {
            resetTab();

            if (wg != null) {
                isBlocking = true;
                for (Stat s : Stat.values()) {
                    int v = wg.value(s);
                    if (v > 0) filterFields[s.i].setText(String.valueOf(v));
                }
                isBlocking = true;
                temporaryFilterStats = null;
            }

            selectedResource = kr;
            temporaryWeight = wg;

            TreePath path = SWGResourceClassTree.getTreePath(rc);
            resourceClassTree.setSelectionPath(path);
            resourceClassTree.scrollPathToVisible(path);
        }
    }

    /**
     * Helper method which filters the specified set of resources based on the
     * filter. If the specified filter does not contains any values this method
     * returns the specified set as-is. If the specified filter contains values
     * this method returns a set of resources with values that all are better
     * than the specified filter. If {code filterConsiderCaps} is selected and
     * if {code selectedResourceClass} is not {@code null} this implementation
     * adjusts the filter, otherwise the specified filter is used as-is. If
     * {@link #filterConsiderNoStat} is not selected only resources which have
     * all values specified in the filter are accepted.
     * 
     * @param resources a set of resources
     * @param filter the filter to sift resources through
     * @return a set of resources
     */
    private SWGResourceSet currentFilterOnFilter(
            SWGResourceSet resources, int[] filter) {

        if (filter == null || SWGValues.sum(filter) <= 0)
            return resources; // no work to do

        SWGResourceFilter flt = new SWGResourceFilter(filter);

        // the following logic may tamper with the resource filter and
        // "adjust" the values for the filter towards the specified caps
        if (selectedResourceClass != null && filterConsiderCaps.isSelected())
            for (Stat s : Stat.values())
            flt.set(s, currentFilterValueConsiderCap(
                    flt.value(s), selectedResourceClass.max(s)));

        return resources.subsetBy(flt, !filterConsiderNoStat.isSelected());
    }

    /**
     * Helper method which returns a set of resources filtered on the specified
     * guard. If the option "Show from inventory" is selected the returned set
     * also includes inventory resources that meet the specified guard. If it is
     * a weighed guard the returned set is sorted on the weights of the
     * resources.
     * 
     * @param resources the resource set to filter
     * @param guard the guard to filter the resources with
     * @return a set of resources
     */
    private SWGResourceSet currentFilterOnGuard(SWGResourceSet resources,
            SWGGuard guard) {

        if (((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceGuardsMixInventory", Boolean.FALSE)).booleanValue()) {

            List<SWGInventoryWrapper> wrappers = SWGResController.inventory(
                    SWGResourceTab.galaxy());

            for (SWGInventoryWrapper wr : wrappers)
                resources.add(wr.getResource()); // sifted in next step
        }

        // now filter the set of current and inventory resources
        SWGResourceSet rs2 = new SWGResourceSet(resources.size());
        for (SWGKnownResource r : resources)
            if (guard.accept(r))
                rs2.add(r);

        // sort on weights if weighed guard
        if (guard.isWeighted())
            rs2.sort(new SWGWeightComparator(
                    (SWGWeights) guard.logic(),
                    guard.rc(),
                    guard.acceptNoStats));

        return rs2;
    }

    /**
     * Helper method which filters resources for the option "allow-zeroes". If
     * this option is selected, or if the specified filter does not contain any
     * values, then the specified set of resources is returned as-is, otherwise
     * the set of resources is filtered so that all resources in the returned
     * set contains all of the stats that the user specifies at the filter input
     * fields.
     * 
     * @param resources a set of resources
     * @param filter the filter to sift resources through
     * @return a set of resources
     */
    private SWGResourceSet currentFilterOnZeroes(
            SWGResourceSet resources, int[] filter) {

        if (filterConsiderNoStat.isSelected()
                || filter == null || SWGValues.sum(filter) <= 0)
            return resources; // no work to do

        SWGResourceFilter flt = new SWGResourceFilter(filter);
        SWGResourceSet ret = new SWGResourceSet(resources.size());
        for (SWGKnownResource e : resources)
            if (flt.hasAllValues(e))
                ret.add(e);

        return ret;
    }

    /**
     * Helper method which returns a value adjusted for cap, though never
     * exceeding cap.
     * 
     * <pre>min(cap, value * cap / 1000)</pre>
     * 
     * @param value the value to adjust
     * @param cap the cap to adjust for
     * @return the adjusted value
     */
    private int currentFilterValueConsiderCap(int value, int cap) {
        return Math.min((value * cap / 1000), cap);
    }
    
    /**
     * This method returns a set of current resources which content is
     * determined by the state of several options and GUI selections made by the
     * user. If the field {code currentList} is {@code null} this method creates
     * a new set of resources and filters it by the possibly changed options and
     * selections; finally it updates SWGAide's log bar.
     * 
     * @return a set of current resources
     */
    private SWGResourceSet currentGet() {
        if (!isGuiFinished)
            return SWGResourceSet.EMPTY;

        if (currentList == null) {
            SWGResourceSet set = new SWGResourceSet(resourceTab.spawning());
            set = currentFilterOnAge(set);

            // if filter always filter
            int[] filter = filterFieldsArray();

            if (!SWGWeights.isValid(filter))
                set = currentFilterOnFilter(set, filter);

            if (selectedGuard != null)
                set = currentFilterOnGuard(set, selectedGuard);
            else if (selectedResourceClass != null)
                set = set.subsetBy(selectedResourceClass);

            // defer this filtering, if resource class is selected the set must
            // not contain other classes and if weighted filtering this will
            // also re-sort the set if a guard is selected
            if (SWGWeights.isValid(filter)) {
                set = currentFilterOnZeroes(set, filter);
                set.sort(new SWGWeightComparator(
                        new SWGWeights(filter), selectedResourceClass,
                        filterConsiderNoStat.isSelected()));
            }

            currentList = set;
            currentResourcesToLogBar(set);
        }
        return currentList;
    }

    /**
     * Helper method which return a resource instance from the set of current
     * resources, or {@code null} if the index is out of bounds. This method
     * synchronizes on {@code this} and called from table models to avoid race
     * conditions when the set of resource is changed while the model is
     * updating.
     * 
     * @param index the index for the resource
     * @return a resource instance or {@code null}
     */
    private SWGKnownResource currentGet(int index) {
        SWGResourceSet rs = currentGet();
        if (rs.size() > index)
            return rs.get(index);

        return null;
    }
    
    /**
     * Helper method which displays some info at SWGAide's log bar, the number
     * of resources on display and the most recent fetch from SWGCraft.org.
     * 
     * @param set the set of resources to parse info from
     */
    private void currentResourcesToLogBar(SWGResourceSet set) {
        SWGFrame f = SWGAide.frame();
        if (resourceTab != f.getTabPane().getSelectedComponent()
                || SWGResourceTab.galaxy() == null)
            return;

        long now = System.currentTimeMillis();
        long time = SWGResourceManager.getStatusLocalTime(
                SWGResourceTab.galaxy()).longValue();

        Calendar n = Calendar.getInstance();
        n.setTimeInMillis(now);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time * 1000L);

        String tstr = String.format("%d resources (%s) from SWGCraft.org",
                Integer.valueOf(set.size()),
                n.get(Calendar.DAY_OF_MONTH) != c.get(Calendar.DAY_OF_MONTH)
                        ? DateFormat.getDateTimeInstance(
                                DateFormat.MEDIUM,
                                DateFormat.SHORT).format(c.getTime())
                        : DateFormat.getTimeInstance(
                                DateFormat.SHORT).format(c.getTime()));
        f.putToLogbar_2(tstr);
    }

    /**
     * Selects the specified resource at the table of current resources. If the
     * argument is {@code null}, or if it is depleted, this method does nothing.
     * 
     * @param kr a known resource
     */
    void currentSelect(SWGKnownResource kr) {
        if (kr == null || kr.isDepleted()) return;

        resetTab();
        selectedResource = kr;
        updateCurrent();
    }

    /**
     * Helper method which method creates and displays a popup dialog about a
     * selected resource. Each option is given its individual action listener.
     * 
     * @param row the selected row number, already converted to the model
     * @param e the event that initiates the call
     */
    @SuppressWarnings("synthetic-access")
    private void currentShowPopup(final int row, MouseEvent e) {

        JPopupMenu ppp = new JPopupMenu();
        final SWGKnownResource res = currentGet().get(row);

        String s = null;
        if (selectedGuard != null) {
            s = selectedGuard.getName();
            String n = selectedGuard.notes().trim();
            if (!n.isEmpty()) s = s + ": \n" + n;
        }

        ppp.add(SWGSchemResViewer.displayMenu(res, this));
        updateViewer = true; // by chance, it is reset if...

        ppp.addSeparator();

        ppp.add(inventoryAddMenu(res));
        ppp.add(SWGResController.monitorAddMenu(res, s));

        // Add to harvester panel
        SWGCGalaxy g = SWGResourceTab.galaxy();
        JMenuItem addHarv = new JMenuItem("Add to harvester panel");
        boolean hp = !SWGResController.harvesters(g).isEmpty()
                && !SWGResController.harvesterOwners(g).isEmpty();
        addHarv.setToolTipText(hp
                ? "Add this resource to the harvester panel"
                : "Define harvester & owner at \"Harvesting\"");
        addHarv.setEnabled(hp);
        if (hp)
            addHarv.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e1) {
                    actionCurrentAddToHarvester(res);
                }
            });
        ppp.add(addHarv);

        ppp.addSeparator();

        // send depleted message to SWGCraft.org
        JMenuItem sendDepl = new JMenuItem("Mark depleted");
        sendDepl.setToolTipText(
                "Send depleted message to SWGCraft.org about this resource");
        sendDepl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                resourceTab.actionSendDepleted(res);
            }
        });
        ppp.add(sendDepl);

        ppp.addSeparator();

        SWGResourceClass rc = res.rc();
        ppp.add(SWGResController.inventoryFilterMenu(rc, null, res, true));
        ppp.add(SWGSchemController.resClassUse(rc));

        if (rc != null && rc.isSub(SWGCreatureResources.class))
            ppp.add(SWGResController.creatureHarvMenu((SWGCreatureResources)
                    rc, sendDepl));

        ppp.addSeparator();

        ppp.add(makeWriteNews(res, false));
        ppp.add(makeWriteNews(res, true));

        ppp.show(currentResourcesTable, e.getX(), e.getY());
    }

    /**
     * Helper method which is called from the document listeners for the filter
     * input fields. If there is some work in progress this method does nothing,
     * see {code isBlocking}. Otherwise this method triggers so the GUI is
     * refreshed.
     */
    private void filterChanged() {
        if (isBlocking || !isGuiFinished)
            return;

        temporaryFilterStats = null;
        temporaryWeight = null;
        updateCurrent();
    }

    /**
     * Helper method which aligns the filter input fields with the width of
     * their corresponding columns.
     */
    private void filterFieldsAlign() {
        SwingUtilities.invokeLater(new Runnable() {

            @SuppressWarnings("synthetic-access")
            public void run() {
                int i = 2;
                for (Stat s : Stat.gameOrder()) {
                    filterFieldsAlignTF(filterFields[s.i], i);
                    ++i;
                }
                filterFieldsAlignTF(filterClearButton, 13); // already updated
                filterClearButton.invalidate();
            }
        });
    }

    /**
     * Helper method which aligns the width of the specified GUI component with
     * the width of the specified table column.
     * 
     * @param comp the component to align its width for
     * @param col the table column which corresponds to the component
     */
    private void filterFieldsAlignTF(Component comp, int col) {
        TableColumn tc = currentResourcesTable.getColumnModel().getColumn(col);
        Dimension d = new Dimension(tc.getWidth(), comp.getHeight());
        if (col == 13) {
            tc = currentResourcesTable.getColumnModel().getColumn(col + 1);
            d.width += tc.getWidth() + 18;
        }
        comp.setMinimumSize(d);
        comp.setPreferredSize(d);
        comp.setMaximumSize(d);
    }

    /**
     * Helper method which reads the filter input fields and returns an array
     * with their integer values. The array is ordered by the model used in
     * SWGAide.
     * 
     * @return the values from the filter input fields
     */
    private int[] filterFieldsArray() {
        if (temporaryFilterStats == null) {
            int[] stats = new int[Stat.COUNT];
            for (Stat s : Stat.values())
                stats[s.i] = ZNumber.intVal(filterFields[s.i].getText());

            temporaryFilterStats = stats;
        }
        return temporaryFilterStats;
    }

    /**
     * Helper method to action event handlers. This method clears all filter
     * input fields without side-effects.
     */
    private void filterFieldsClear() {
        isBlocking = true;
        for (JTextField f : filterFields)
            f.setText(null);

        temporaryFilterStats = null;
        temporaryWeight = null;

        isBlocking = false;
    }

    /**
     * This method is called for two different action events, in both cases it
     * is determined if this component has focus and which action to select:
     * <UL>
     * <LI>if focus is changed for{code resourceTab} the argument is {@code
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
     *        or its siblings, or if {code resourceTab} gained focus; {@code
     *        false} otherwise
     */
    void focusGained(boolean focus) {
        if (isGuiFinished
                && focus && resourceTab.getSelectedComponent() == this) {

            filterFieldsAlign();
            updateDisplay();

            SWGHelp.push(helpPage);
        } else {
            SWGHelp.remove(helpPage);
        }
    }

    /**
     * Helper method. If the argument is {@code true} this method tints the
     * resource guards GUI tab in red.
     * 
     * @param isAlerted {@code true} if there is an alerted guard, {@code false}
     *        otherwise
     */
    private void guardsAlerted(boolean isAlerted) {
        northEastTabbedPane.setBackgroundAt(0, isAlerted
                ? SWGGuiUtils.colorAlert
                : null /* this.getBackground() */);
        tintTab('g', isAlerted);
    }
    
    /**
     * Helper method which determines if at least one guards in {@code guards}
     * is alarming.
     * 
     * @param guards a list of guards to scan
     * @return {@code true} if at least one guard in {@code guards} is alarming,
     *         {@code false} otherwise
     */
    private boolean guardsAlerted(List<SWGGuard> guards) {
        for (SWGGuard g : guards)
            if (g.resourceRecentSpawnDate > 0)
                return true;

        return false;
    }

    /**
     * Helper method which determines which resource guard is selected at the
     * GUI table and updates {code selectedGuard} accordingly. If a guard is
     * selected this method clears any selection at the resource class tree. If
     * no guard is selected at the resource guards table, the field is set to
     * {@code null}.
     * 
     * @return {@code true} if {code selectedResourceClass} is set to a guard,
     *         false if it is set to {@code null}
     */
    private boolean guardSelection() {
        selectedGuard = null; // set the field later if so
        selectedResourceClass = null;

        int row = guardsTable.getSelectedRow();
        if (row < 0)
            return false;

        disableColoring = false; // never disable for guards

        // a guard is selected, dismiss selected resource class and filters
        resourceClassTree.clearSelection();
        monitorsTable.clearSelection();
        filterFieldsClear();

        row = guardsTable.convertRowIndexToModel(row);
        selectedGuard = guardsGet().get(row);
        return true;
    }

    /**
     * Helper method which returns a list of resource guards for the current
     * galaxy. In particular, this is the list of guards to display at the
     * guards GUI table. If the user has selected to hide "empty" guards all
     * elements in returned list are currently triggered by some resource,
     * otherwise the list contains all guards for the galaxy.
     * 
     * @return a list of resource guards displayed at the table of guards
     */
    private List<SWGGuard> guardsGet() {
        if (!isGuiFinished)
            return Collections.emptyList();

        if (guardsList == null) {
            List<SWGGuard> galaxyGuards = new ArrayList<SWGGuard>(
                    SWGResController.guards(SWGResourceTab.galaxy()));

            if (((Boolean) SWGFrame.getPrefsKeeper().get(
                    "resourceGuardHideGuards", Boolean.FALSE)).booleanValue()) {

                List<SWGGuard> g = new ArrayList<SWGGuard>(galaxyGuards.size());

                for (SWGGuard grd : galaxyGuards)
                    if (grd.resourceRecentSpawnDate > 0)
                        g.add(grd);

                galaxyGuards = g;
            }
            Collections.sort(galaxyGuards);
            guardsList = galaxyGuards;
            guardsAlerted(guardsAlerted(guardsList));
        }
        return guardsList;
    }

    /**
     * Helper method which handles imports the specified file of resource
     * guards.
     * <P>
     * The file format is comma separated (CSV) one-liners: <BR/>
     * {@code guardName, resource class, CD,CR,DR,ER,FL,HR,MA,OQ,PE,SR,UT,
     * limit, alert[, allowZeroes[, anything]]}<BR/>
     * All elements but the stats must occur in the specified order.
     * <DL>
     * <DT>guardName</DT>
     * <DD>is just anything without a comma sign</DD>
     * <DT>resource class</DT>
     * <DD>is either the type token, or the proper resource class name, as read
     * in <i>resourcetree.XML</i>
     * <DD/>
     * <DT>stats</DT>
     * <DD>are <U>in any order</U> and in the range [0 1000]; values can be
     * weighed values such as 33, 66, etc., or lower limits such as 950
     * <DD/>
     * <DT>limit</DT>
     * <DD>is in the range [0 1000] (or [0 100] for compatibility) and is only
     * used for weighed values; this method scales the value accordingly
     * <DD/>
     * <DT>alert</DT>
     * <DD>is 0 or non-0; the value determines if an alert is triggered when
     * resources meet the filter, zero is for no alert</DD>
     * </DL>
     * <P>
     * <B>The file header</B> must be valid only in the case the user has
     * reordered the stats. If there is no file header or if the file header is
     * edited and it cannot be parsed this implementation assumes the default
     * order of the elements, listed above. Notice, it is only the stats that
     * can be in any order (this may change in future versions of SWGAide).
     * 
     * @param file the file which is selected to read from
     */
    private void guardsReadFile(File file) {
        if (!file.exists()) {
            JOptionPane.showMessageDialog(guardsTable, "No such file:\n"
                    + file.getAbsolutePath(), "File error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int[] statsOrder = guardsReadHeader(file);
        if (statsOrder[0] < 0) return; // abort

        ZReader sr = ZReader.newTextReader(file);
        if (sr == null) return;

        boolean error = false;
        for (String line : sr.lines(true, true))
            error |= guardsReadGuard(line, statsOrder);

        if (error) JOptionPane.showMessageDialog(
                northEastTabbedPane, String.format(
                        "There is one or more errors while parsing file:%n" +
                                "%s%n%nSee log file", file.getAbsolutePath()),
                "Error", JOptionPane.ERROR_MESSAGE);

        SWGResController.check();
    }

    /**
     * Helper method which parses the specified line, creates a resource guard
     * which is added to the list of resource guards for the current galaxy, and
     * finally {@code true} is returned. If there is an error it is intercepted
     * and a message is written to SWGAide's log file and {@code false} is
     * returned.
     * <P>
     * The file format is comma separated (CSV) one-liners: <BR/>
     * {@code guardName,resource
     * class,CD,CR,DR,ER,FL,HR,MA,OQ,PE,SR,UT,limit,alert[,
     * allowZeroes[,anything]]}<BR/>
     * All elements but stats must occur in the specified order.
     * <DL>
     * <DT>guardName</DT>
     * <DD>is anything without a comma</DD>
     * <DT>resource class</DT>
     * <DD>is either the type token, or the proper resource class name, as read
     * in <i>resourcetree.XML</i>
     * <DD/>
     * <DT>stats</DT>
     * <DD>are <U>in any order</U> and in the range [0 1000]; values can be
     * weighed values such as 33, 66, etc., or lower limits such as 950
     * <DD/>
     * <DT>limit</DT>
     * <DD>is in the range [0 1000] (or [0 100] for compatibility) and is only
     * used for weighed values; this method scales the value accordingly
     * <DD/>
     * <DT>alert</DT>
     * <DD>is 0 or non-0; the value determines if an alert is triggered when
     * resources meet the filter, zero is for no alert</DD>
     * </DL>
     * <P>
     * If the file header is invalid but the user selected to continue this
     * method parses the line using the default order.
     * 
     * @param line the line to parse
     * @param statsOrder an integer array that defines the order of stats in the
     *        specified line, see
     *        {@link SWGResourceTab#statsToDefaultOrder(int[], String[], int)}
     * @return {@code true} if there is an error, {@code false} otherwise
     */
    private boolean guardsReadGuard(String line, int[] statsOrder) {
        String l = line.replace("\"", "");
        StringTokenizer tok = new StringTokenizer(l, ",");
        try {
            // note that stats can have any order
            // guardName,resource class,CD,CR,DR,ER,FL,HR,MA,OQ,PE,SR,UT,
            // limit,alert[,allowZeros[,notes]]

            String name = tok.nextToken().trim();
            // replace comma replacement, if any, from guardsSave()
            name = name.replace('\u00b8', ',');

            String cn = tok.nextToken().trim().replace("\"", "");
            SWGResourceClass clazz = SWGResourceClass.rc(cn);

            int[] values = new int[Stat.COUNT];
            for (int i = 0; i < values.length; ++i)
                values[statsOrder[i]] = ZNumber.intExc(tok.nextToken());

            int limit = ZNumber.intExc(tok.nextToken());
            if (limit < 103)
                limit = Math.min(limit * 10, 1000); // scale with sanity

            SWGValues val;
            if (SWGWeights.isValid(SWGValues.sum(values)))
                val = new SWGWeights(values);
            else {
                val = new SWGResourceFilter(values);
                limit = 0; // no limit for plain filters
            }

            boolean alert = ZNumber.intExc(tok.nextToken()) != 0;

            boolean noStats = filterConsiderNoStat.isSelected();
            if (tok.hasMoreTokens())
                            // optional
                noStats = ZNumber.intExc(tok.nextToken()) != 0;

            SWGGuard grd =
                    new SWGGuard(name, clazz, val, limit, alert, noStats);
            SWGResController.guardsAdd(grd, SWGResourceTab.galaxy(), false);

            if (tok.hasMoreTokens()) { // optional
                String remainder = tok.nextToken("\n");
                grd.notes(ZHtml.regainEOL(remainder.substring(1)));
            }
        } catch (Exception e) {
            SWGAide.printDebug("curr", 1, "Error parsing guard:",
                    e.getClass().getSimpleName(), e.getMessage(), "\t", line);
            return true;
        }
        return false;
    }

    /**
     * Helper method which parses the expected header lines and returns the
     * order of the stats as parsed in the file. If it is determined that the
     * header lines are edited the user is opted to continue or to abort,
     * however, if the user continues it is the default order of the stats that
     * is used.
     * <P>
     * In particular, the returned array is the conversion order for each
     * individual stat relative the order of the model of the stats in SWGAide,
     * which is alphabetical: CD,CR,DR,ER,FL,HR,MA,OQ,PE,SR,UT <BR/>
     * For example, the array [3,0,2,1,...] reads that the stats in the file are
     * ordered ER, CD, DR, CR, ...
     * <P>
     * <B>The file header</B> must be valid only in the case the user has
     * reordered the stats. If there is no file header or if the file header is
     * edited and it cannot be parsed this implementation assumes the default
     * order of the elements, listed above. Notice, it is only the stats that
     * can be in any order (this may change in future versions of SWGAide).
     * 
     * @param file the file to read from
     * @return the order of the stats, if index 0 is -1 the user selected to
     *         abort
     */
    private int[] guardsReadHeader(File file) {
        // header looks like, note it can be #ABC or # ABC
        // # Exported guards/filters for ...
        // # guardName,resource class,CD,CR,DR,ER,FL,HR,MA,OQ,PE,SR,UT,
        // limit,alert[,allowZeros[,notes]]

        ZReader sr = ZReader.newTextReader(file);
        if (sr == null) return new int[] { -1 }; // abort

        String line;
        while ((line = sr.line(false)) != null)
            if (line.startsWith("#") || line.startsWith("#"))
                if (line.indexOf("guardName,") > 0)
                    break; // skip lines until we have # guardName,
        sr.close();

        int[] defaultOrder = new int[Stat.COUNT];
        for (int i = 0; i < defaultOrder.length; ++i)
            defaultOrder[i] = i;

        if (line == null
                || !(line.startsWith("#") || line.startsWith("\"#"))) {
            if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                    guardsTable, "The selected file does not have an " +
                            "expected header line.\nContinue?", "Warning",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE))
                return new int[] { -1 }; // abort
            // else
            return defaultOrder;
        }

        try {
            String[] split = line.split(","); // stats start at index 2
            resourceTab.statsToDefaultOrder(defaultOrder, split, 2);
            return defaultOrder;
        } catch (Throwable e) {
            JOptionPane.showMessageDialog(guardsTable, String.format(
                    "Determining order of stats caused an error:%n" +
                            "%s%n%nAborting", e.getMessage()), "Error",
                    JOptionPane.ERROR_MESSAGE);
            SWGAide.printDebug("curr", 1,
                    "SWGCurrentResourcesTab:guardsReadHeader:", e.getMessage());
            if (SWGConstants.DEV_DEBUG)
                SWGAide.printError("SWGCurrentResTab:guardsReadHeader", e);
            return new int[] { -1 }; // abort
        }
    }

    /**
     * Helper method called from event action dispatchers. This method creates
     * and shows a popup dialog related to resource guards.
     * 
     * @param e the event that initiates the call
     */
    @SuppressWarnings("synthetic-access")
    private void guardsShowPopup(MouseEvent e) {
        int r = guardsTable.rowAtPoint(e.getPoint());
        final int row = r >= 0
                ? guardsTable.convertRowIndexToModel(r)
                : r;

        guardsTable.getSelectionModel().setSelectionInterval(r, r);

        final SWGGuard g;
        if (row >= 0)
            g = guardsGet().get(row);
        else
            g = null;

        final int len = SWGResController.guards(SWGResourceTab.galaxy()).size();
        final Point pp = e.getLocationOnScreen();

        JPopupMenu popup = new JPopupMenu();

        if (row >= 0) {
            JMenuItem edit = new JMenuItem("Edit guard");
            edit.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e1) {
                    actionGuardEdit(g);
                }
            });
            popup.add(edit);
        }

        JMenuItem create = new JMenuItem("Create guard");
        create.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1) {
                actionGuardCreate(null);
            }
        });
        popup.add(create);

        JMenuItem imp = new JMenuItem("Import guards");
        imp.setToolTipText("Import guards from file");
        imp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1) {
                actionGuardsImportFile();
            }
        });
        popup.add(imp);

        if (len > 0) {
            JMenuItem save = new JMenuItem("Save guards");
            save.setToolTipText("Save guards to file");
            save.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e1) {
                    actionGuardsWriteFile();
                }
            });
            popup.add(save);
        }

        popup.addSeparator();

        // Select res-class in inventory
        SWGResourceClass rc = g == null
                ? null
                : g.rc();
        SWGWeights w = (SWGWeights) (g != null && g.isWeighted()
                ? g.logic()
                : null);
        popup.add(SWGResController.inventoryFilterMenu(rc, w, null, false));
        popup.add(SWGSchemController.resClassUse(rc));
        if (rc != null && rc.isSub(SWGCreatureResources.class))
            popup.add(SWGResController.creatureHarvMenu(
                    (SWGCreatureResources) rc, guardsTable));

        popup.addSeparator();

        final Integer mh = (Integer) SWGFrame.getPrefsKeeper().get(
                "resourceGuardsMaxHours");
        JMenuItem age = new JMenuItem(String.format("Set time%s", mh == null
                ? "36"
                : String.format(" (%d h)", mh)));
        age.setToolTipText("Only display resources younger than X hours");
        age.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                actionGuardsMaxAgeDialog(pp, mh);
            }
        });
        popup.add(age);

        final JMenuItem hide = new JCheckBoxMenuItem("Hide \"empty\" guards");
        hide.setToolTipText("Hide/show guards that have nothing to show");
        hide.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1) {
                SWGFrame.getPrefsKeeper().add("resourceGuardHideGuards",
                        Boolean.valueOf(hide.isSelected()));
                updateGuards();
                updateCurrent();
            }
        });
        hide.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceGuardHideGuards", Boolean.FALSE)).booleanValue());
        popup.add(hide);

        final JMenuItem alertOnce = new JCheckBoxMenuItem("Alert once");
        alertOnce.setToolTipText("Sound the alerts only once each session");
        alertOnce.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1) {
                SWGFrame.getPrefsKeeper().add("resourceGuardAlertOnce",
                        Boolean.valueOf(alertOnce.isSelected()));
            }
        });

        alertOnce.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceGuardAlertOnce", Boolean.FALSE)).booleanValue());
        popup.add(alertOnce);

        final JMenuItem mute = new JCheckBoxMenuItem("Mute sound");
        mute.setToolTipText("Mute all sound alerts/alarms");
        mute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                SWGResController.alertSilentTime(mute.isSelected()
                        ? Integer.MAX_VALUE
                        : 10);
            }
        });
        mute.setSelected(
                SWGResController.alertSilentTime() == Integer.MAX_VALUE);
        popup.add(mute);

        final JMenuItem dkm = new JCheckBoxMenuItem("Disregard K & M");
        dkm.setToolTipText("Mute alerts for resources from " +
                "Kashyyyk & Mustafar");
        dkm.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1) {
                SWGFrame.getPrefsKeeper().add("resourceGuardsDisregardMK",
                        Boolean.valueOf(dkm.isSelected()));
                SWGResController.check();
            }
        });
        dkm.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceGuardsDisregardMK", Boolean.FALSE)).booleanValue());
        popup.add(dkm);

        final JMenuItem inv = new JCheckBoxMenuItem("Show from inventory");
        inv.setToolTipText("Include resources from inventory in display");
        inv.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1) {
                SWGFrame.getPrefsKeeper().add("resourceGuardsMixInventory",
                        Boolean.valueOf(inv.isSelected()));
                updateCurrent();
            }
        });
        inv.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceGuardsMixInventory", Boolean.FALSE)).booleanValue());
        popup.add(inv);

        if (row >= 0) {
            popup.addSeparator();
            JMenuItem delete = new JMenuItem("Delete guard");
            delete.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e1) {
                    actionGuardDelete(g);
                }
            });
            popup.add(delete);
        }

        if (len > 0) {
            JMenuItem deleteAll = new JMenuItem("Delete all guards");
            deleteAll.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e1) {
                    actionGuardsDeleteAll();
                }
            });
            popup.add(deleteAll);
        }

        popup.show(guardsTable, e.getX(), e.getY());
    }

    /**
     * Helper method which creates and returns a text representation of the
     * guard for which a tool tip text is displayed.
     * 
     * @param event the event that initiates the call
     * @return the text of the tool tip
     */
    private String guardsToolTipText(MouseEvent event) {
        Point pp = event.getPoint();
        int row = guardsTable.rowAtPoint(pp);
        if (row < 0 || row >= guardsGet().size())
            return "";

        row = guardsTable.convertRowIndexToModel(row);
        SWGGuard grd = guardsGet().get(row);

        ZString z = new ZString(grd.rc().rcName()).app(' ');
        grd.logic().toString(z, false);
        return z.app(' ').app(grd.notes()).toString();
    }

    /**
     * Helper method which writes {@code guard} as one line to {@code writer}.
     * 
     * @param wr the output writer
     * @param g the guard to write
     * @throws Exception if there is an error
     */
    private void guardsWriteGuard(ZWriter wr, SWGGuard g) throws Exception {
        // guardName,resource class,ER,CR,CD,DR,FL,HR,MA,PE,OQ,SR,UT,limit,alert
        // [,allowZeros[,notes]]

        // replace comma signs in guard names
        String n = g.getName().replace(',', '\u00b8');
        wr.writeExc(n);
        wr.writeExc(",");
        wr.writeExc(g.rc().rcName());
        wr.writeExc(",");
        wr.writeExc(g.logic().toString(true));
        wr.writeExc(",");
        wr.writeExc(Integer.toString(g.threshold()));
        wr.writeExc(",");
        wr.writeExc(Integer.toString((g.useAlarm
                ? 1
                : 0)));
        wr.writeExc(",");
        wr.writeExc(Integer.toString((g.acceptNoStats
                ? 1
                : 0)));
        wr.writeExc(",");
        wr.writelnExc(ZHtml.replaceEOL(g.notes()));
    }

    /**
     * Helper method which writes the header for exported guards.
     * 
     * @param wr the writer to write to
     * @throws Exception if there is an I/O error
     */
    private void guardsWriteHeader(ZWriter wr) throws Exception {
        wr.writeExc("# Exported guards/filters for ");
        wr.writeExc(SWGResourceTab.galaxy().getName());
        wr.writeExc(", ");
        DateFormat df = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT);
        wr.writeExc(df.format(new Date()));
        wr.writeExc(", by SWGAide: ");
        wr.writelnExc(SWGConstants.swgAideURL);

        wr.writeExc("# guardName,resource class,");
        wr.writeExc("ER,CR,CD,DR,FL,HR,MA,PE,OQ,SR,UT");
        wr.writelnExc(",limit,alert[,allowZeros[,notes]]");
    }

    /**
     * Helper method which adds the specified resource to the currently selected
     * character's inventory.
     * 
     * @param resource the resource to add
     */
    private void inventoryAdd(SWGKnownResource resource) {
        SWGCharacter toon = SWGFrame.getSelectedCharacter();

        SWGInventoryWrapper wr =
                new SWGInventoryWrapper(resource, toon.getName());

        if (selectedGuard != null)
            wr.setNotes(selectedGuard.getName());

        SWGResourceTab.inventoryTab().dialog().showAndBegin(wr);
    }

    /**
     * Helper method which returns a menu item which action listener adds the
     * specified resource to inventory. If the resource already exists in
     * inventory the menu item is disabled.
     * 
     * @param res a resource
     * @return a menu item
     */
    private JMenuItem inventoryAddMenu(final SWGKnownResource res) {
        boolean isStocked = SWGResController.inventoryAmount(
                res, SWGFrame.getSelectedGalaxy()) >= 0;

        JMenuItem addInv = new JMenuItem("Add to inventory");
        addInv.setToolTipText("Add the selected resource to inventory");
        addInv.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e1) {
                inventoryAdd(res);
            }
        });
        addInv.setEnabled(!isStocked);
        return addInv;
    }
    
    /**
     * Creates and returns a panel with a text input field and a colored button
     * for choosing the color for the table cell backgrounds for resource stats.
     * The integer indicates fair, good, or great; compare
     * {@link SWGGuiUtils#statLimits} and colors.
     * 
     * @param text the text for the button
     * @param fgg integer that indicates fair, good, or great
     * @return a button panel
     */
    @SuppressWarnings("synthetic-access")
    private Component makeColorChooser(final String text, final int fgg) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEADING));

        final JTextField txtFld = makeFilterTextField("The lower limit for " +
                text.toLowerCase(Locale.ENGLISH) + " resources", false,
                new SWGDocNumberFilter(true, Integer.valueOf(0), 0, 1000) {

                    final int idx = fgg;

                    @Override
                    protected boolean isValid(String value) {
                        boolean b = super.isValid(value);
                        if (!b) return false; // false is false, always

                        // keep values in relation to each other
                        double[] sl = SWGGuiUtils.statLimits.clone();
                        sl[idx] = Integer.parseInt(value) / 1000.0;
                        return sl[0] < sl[1] && sl[1] < sl[2];
                    }
                });
        txtFld.setName(text);

        txtFld.setEnabled(true);
        panel.add(txtFld);

        final JButton button = new JButton(text);
        Dimension d = new Dimension(65, 20);
        button.setMinimumSize(d);
        button.setPreferredSize(d);
        button.setToolTipText("Choose the color for "
                + text.toLowerCase(Locale.ENGLISH) + " resource stats");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionStatColorChooserButton(button);
            }

        });
        panel.add(button);

        Integer i;
        i = Integer.valueOf((int) (SWGGuiUtils.statLimits[fgg] * 1000));
        button.setBackground(SWGGuiUtils.statColors[fgg * 2]);
        button.setForeground(SWGGuiUtils.statColors[(fgg * 2) + 1]);
        txtFld.setText(i.toString());

        txtFld.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                actionStatLimitField(txtFld);
            }

            public void insertUpdate(DocumentEvent e) {
                actionStatLimitField(txtFld);
            }

            public void removeUpdate(DocumentEvent e) {
                actionStatLimitField(txtFld);
            }
        });
        return panel;
    }

    /**
     * Creates and returns a GUI component for considering caps components.
     * 
     * @return a component for considering caps.
     */
    private Component makeConsiderCap() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        filterConsiderCaps = new JCheckBox("Consider caps");
        filterConsiderCaps.setToolTipText("Make plain filters " +
                "consider caps for selected resource class");
        filterConsiderCaps.addActionListener(this);
        filterConsiderCaps.setMnemonic('P');
        filterConsiderCaps.setSelected(((Boolean) SWGFrame.getPrefsKeeper()
                .get("resourceFilterFieldCaps", Boolean.TRUE)).booleanValue());
        panel.add(filterConsiderCaps);
        return panel;
    }

    /**
     * Creates and returns a GUI component for considering caps
     * 
     * @return a GUI component for considering caps
     */
    private Component makeConsiderNonStat() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        filterConsiderNoStat = new JCheckBox("Allow zero");
        filterConsiderNoStat.setToolTipText("Weighed filters: " +
                "make use of missing stats");
        filterConsiderNoStat.addActionListener(this);
        filterConsiderNoStat.setMnemonic('T');
        filterConsiderNoStat.setSelected(((Boolean) SWGFrame.getPrefsKeeper()
                .get("resourceConsiderNoStat", Boolean.TRUE)).booleanValue());
        panel.add(filterConsiderNoStat);
        return panel;
    }

    /**
     * Creates and returns a GUI component for limiting view to max age
     * 
     * @return a GUI component for limiting view to max age
     */
    @SuppressWarnings("synthetic-access")
    private Component makeFilterAge() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        maxAgeToggle = new JCheckBox("Max hours");
        maxAgeToggle.setToolTipText("Show only resources less than hours");
        maxAgeToggle.addActionListener(this);
        maxAgeToggle.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceFilterUseMaxAge", Boolean.FALSE)).booleanValue());

        panel.add(maxAgeToggle);

        maxAgeHours = makeFilterTextField(
                "Resources' maximum age in hours", false,
                new SWGDocNumberFilter(true, null, 0, 24 * 22) {
                    @Override
                    protected boolean isValid(String v) {
                        return isBlocking || super.isValid(v);
                    }
                });

        maxAgeHours.setColumns(3);
        maxAgeHours.setText((String) SWGFrame.getPrefsKeeper().get(
                "resourceFilterMaxHours", ""));
        maxAgeHours.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                actionMaxAge();
            }

            public void insertUpdate(DocumentEvent e) {
                actionMaxAge();
            }

            public void removeUpdate(DocumentEvent e) {
                actionMaxAge();
            }
        });
        panel.add(maxAgeHours);

        return panel;
    }

    /**
     * Creates and returns the panel for the resource filters which directly
     * filters the set of resources displayed at the main table.
     * 
     * @return the panel for the resource filters
     */
    private Component makeFilterPanel() {
        Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());
        for (Stat s : Stat.gameOrder()) {
            JTextField tf = makeFilterTextField(
                    String.format("%s - %s", s.name(), s.getDescription()),
                    true, new SWGDocNumberFilter(true, null, 0, 1000) {
                        @SuppressWarnings("synthetic-access")
                        @Override
                        protected boolean isValid(String v) {
                            return isBlocking || super.isValid(v);
                        }
                    });
            filterFields[s.i] = tf;
            b.add(tf);
        }

        filterClearButton = new JButton("Clear");
        filterClearButton.setToolTipText("Clear and reset this panel - Alt-C ");
        filterClearButton.addActionListener(this);
        filterClearButton.setMnemonic('C');
        b.add(filterClearButton);

        b.setAlignmentX((float) 55.5);

        return b;
    }

    /**
     * Creates and returns a GUI text field with the supplied text as tool tip
     * text and a lowered bevel border
     * 
     * @param txt the text for the tool tip
     * @param addDocListener {@code true} if a document listener should be
     *        added, {@code false} otherwise
     * @param fff a filter field filter for this text field
     * @return the GUI component
     */
    @SuppressWarnings("synthetic-access")
    private JTextField makeFilterTextField(
            String txt, boolean addDocListener, SWGDocNumberFilter fff) {
        final JTextField fld = new JTextField();
        fld.setToolTipText(txt);
        fld.setBorder(BorderFactory.createLoweredBevelBorder());
        fld.setHorizontalAlignment(SwingConstants.RIGHT);
        ((AbstractDocument) fld.getDocument()).setDocumentFilter(fff);

        if (addDocListener) {
            fld.getDocument().addDocumentListener(new DocumentListener() {

                public void changedUpdate(DocumentEvent e) {
                    filterChanged();
                }

                public void insertUpdate(DocumentEvent e) {
                    filterChanged();
                }

                public void removeUpdate(DocumentEvent e) {
                    filterChanged();
                }
            });
        }

        return fld;
    }

    /**
     * Creates and returns the component which contains the main table of
     * current resources.
     * 
     * @return the main table of current resources
     */
    @SuppressWarnings("synthetic-access")
    private Component makeMainTable() {
        currentResourcesModel = new ResourcesTableModel();
        currentResourcesTable = new SWGJTable(currentResourcesModel);

        currentResourcesTable.setDefaultRenderer(String.class,
                new SWGDecoratedTableCellRenderer(currentResourcesModel));
        currentResourcesTable.setDefaultRenderer(Integer.class,
                new SWGResourceStatRenderer(currentResourcesModel));
        currentResourcesTable.setDefaultRenderer(Long.class,
                new SWGResController.AgeRenderer(currentResourcesModel));

        SWGGuiUtils.tableColumnSetWidth(currentResourcesTable, 0, 20, 150, 150);
        int w;
        w = SWGGuiUtils.fontWidth(this, "1 000", SWGGuiUtils.fontPlain()) + 5;
        SWGGuiUtils.tableSetColumnWidths(currentResourcesTable, 2, 999, w, 5);

        currentResourcesTable
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        currentResourcesTable.setAutoCreateRowSorter(true);
        currentResourcesTable.getTableHeader().setReorderingAllowed(false);

        currentResourcesTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting())
                            actionCurrentSelect();
                    }
                });
        currentResourcesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionCurrentMouse(e);
            }
        });
        return currentResourcesTable;
    }

    /**
     * Creates and returns the GUI component for the lists of resource guards.
     * 
     * @return the GUI lists of resource guards
     */
    @SuppressWarnings("synthetic-access")
    private Component makeNEGuardsPanel() {
        guardsModel = new GuardsModel();
        guardsTable = new SWGJTable(guardsModel) {

            @Override
            public String getToolTipText(MouseEvent event) {
                return guardsToolTipText(event);
            }
        };
        guardsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        guardsTable.setDefaultRenderer(String.class,
                new SWGDecoratedTableCellRenderer(guardsModel));
        guardsTable.setDefaultRenderer(Integer.class,
                new SWGDecoratedTableCellRenderer(guardsModel));
        guardsTable.setAutoCreateRowSorter(true);
        guardsTable.getTableHeader().setReorderingAllowed(false);
        int w;
        w = SWGGuiUtils.fontWidth(this, "1 000", SWGGuiUtils.fontPlain()) + 5;
        SWGGuiUtils.tableSetColumnWidths(guardsTable, 3, 3, w, 10);
        guardsTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {

                    public void valueChanged(ListSelectionEvent e) {
                        actionGuardSelect(e);
                    }
                });
        guardsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionGuardsMouse(e);
            }
        });

        JScrollPane jsp =
                new JScrollPane(guardsTable,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // make table bottom clickable
        jsp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionGuardsMouse(e);
            }
        });
        return jsp;
    }

    /**
     * Creates and returns the GUI component which contains the lists of
     * resource monitors.
     * 
     * @return the GUI lists of resource monitors
     */
    @SuppressWarnings("synthetic-access")
    private Component makeNEMonitorsPanel() {
        monitorsModel = new MonitorModel();
        monitorsTable = new SWGJTable(monitorsModel);
        monitorsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        monitorsTable.setDefaultRenderer(String.class,
                new SWGDecoratedTableCellRenderer(monitorsModel));
        monitorsTable.setDefaultRenderer(Long.class,
                new SWGResController.AgeRenderer(monitorsModel));
        monitorsTable.setAutoCreateRowSorter(true);
        monitorsTable.getTableHeader().setReorderingAllowed(false);
        SWGGuiUtils.tableColumnFixWidth(monitorsTable, 2, 40);
        monitorsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionMonitorsMouse(e);
            }
        });

        JScrollPane jsp =
                new JScrollPane(monitorsTable,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return jsp;
    }

    /**
     * Creates and returns the GUI object for the display options.
     * 
     * @return the display options panel
     */
    private Component makeNEOptionsPanel() {
        JPanel disp = new JPanel();
        disp.setLayout(new SpringLayout());
        disp.add(makeConsiderCap());
        disp.add(makeColorChooser("Fair", 0));
        disp.add(makeConsiderNonStat());
        disp.add(makeColorChooser("Good", 1));
        disp.add(makeFilterAge());
        disp.add(makeColorChooser("Great", 2));
        SpringUtilities.makeCompactGrid(disp, 3, 2, 0, 0, 0, 0);
        return disp;
    }

    /**
     * Creates and returns a GUI text area for displaying resource details.
     * 
     * @return a details text areas
     */
    private Component makeNorthDetailsPanel() {
        resourceDetails = new JTextArea(5, 20);
        resourceDetails.setEditable(false);
        resourceDetails.setLineWrap(true);
        resourceDetails.setWrapStyleWord(true);
        
        resourceDetails.setBorder(BorderFactory.createLoweredBevelBorder());

        resourceDetails.addMouseListener(new MouseAdapter() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu p = new JPopupMenu();
                    p.add(makeWriteNews(selectedResource, false));
                    p.add(makeWriteNews(selectedResource, true));
                    p.show(resourceDetails, e.getX(), e.getY());
                }
            }
        });

        JScrollPane jsp = new JScrollPane(resourceDetails,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = 350;
                return d;
            }
        };
        jsp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                "Resource Details"));
        return jsp;
    }

    /**
     * Creates and returns a tabbed pane for the north east corner of this GUI
     * object which contains a list of resource guards, a list of monitored
     * resources, and the options panel.
     * 
     * @return a tabbed pane for the east area of the upper panel
     */
    private Component makeNorthEastTabbedPane() {
        northEastTabbedPane = new JTabbedPane();
        northEastTabbedPane.add("Guards", makeNEGuardsPanel());
        northEastTabbedPane.add("Monitors", makeNEMonitorsPanel());
        northEastTabbedPane.add("Display Options", makeNEOptionsPanel());
        return northEastTabbedPane;
    }

    /**
     * Creates and returns the north panel for the smaller GUI objects for
     * resource tree, the resource details, etc.
     * 
     * @return the north panel
     */
    private Component makeNorthPanel() {
        Box box = new Box(BoxLayout.LINE_AXIS) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int h = d.height / 5;
                d.height = h > 130
                        ? h
                        : 130;
                return d;
            }
        };
        JScrollPane jsp = new JScrollPane(makeResourceClassTreePanel(),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                if (d.width < 100)
                    d.width = 100;
                else if (d.width > 250) d.width = 250;
                return d;
            }            
        };
        box.add(jsp);
        box.add(makeNorthDetailsPanel());
        box.add(makeNorthEastTabbedPane());
        return box;
    }

    /**
     * Helper method which creates and returns a menu item for creating a guard.
     * This method creates a menu item for creating a guard for the specified
     * resource class. The action listener for the item invokes
     * {@link #actionGuardCreate(SWGResourceClass)}.
     * 
     * @param rcl a resource class, not {@code null}
     * @return a menu item
     */
    private JMenuItem makeResClassCreateGuardMenu(final SWGResourceClass rcl) {
        JMenuItem guard = new JMenuItem(
                "Create resource guard for: " + rcl.rcName());
        guard.setToolTipText(
                "Open the create guard dialog for " + rcl.rcName());
        guard.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent evt) {
                actionGuardCreate(rcl);
            }
        });
        return guard;
    }

    /**
     * Helper method which creates and returns a menu item that toggles on/off
     * default resource class coloring. The action listener for the menu item
     * momentarily disables default coloring of resource stats for a selected
     * resource class; this is useful while drilling down the resource class
     * tree but we the user still wants to see what is great in itself. If the
     * user selects this option again it toggles itself on/off. If the user
     * selects a guard, or resets the panel, this option is reset. This option
     * is not saved by SWGAide but it is ephemeral.
     * 
     * @return a menu item
     */
    private JMenuItem makeResClassDisableColoring() {
        JMenuItem item = new JMenuItem(disableColoring
                ? "Enable default coloring"
                : "Disable default coloring (momentarily)");
        item.setToolTipText("Toggle what coloring is based on");
        item.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent evt) {
                disableColoring = !disableColoring;
                updateCurrent();
            }
        });
        item.setEnabled(selectedGuard == null);
        return item;
    }

    /**
     * Creates and returns the GUI component for the resource class tree.
     * 
     * @return the resource class tree component
     */
    @SuppressWarnings("synthetic-access")
    private Component makeResourceClassTreePanel() {
        resourceClassTree = new JTree(resourceTab.resClassTreeModel());
        resourceClassTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

        resourceClassTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                actionResourceClassTreeSelect();
            }
        });

        resourceClassTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                actionResourceClassTreeMouse(e);
            }
        });

        return resourceClassTree;
    }

    /**
     * Helper method that creates and returns a menu item with attached action
     * listener for writing resource details to notes file. Selecting this
     * option will write the details for the specified resource to an in-game
     * notes file named "news.txt", the relevant details as read at the details
     * window. If {@code append} the menu and the action will not overwrite any
     * previous content but rather append to the notes file.
     * 
     * @param kr a resource
     * @param append {@code true} if the menu is to append
     * @return a menu item
     */
    private JMenuItem makeWriteNews(
            final SWGKnownResource kr, final boolean append) {

        JMenuItem mi = new JMenuItem(append
                ? "Append to notes"
                : "Write to notes");
        mi.setToolTipText(append
                ? "Write resource details to notes file -- appends"
                : "Write resource details to in-game notes file -- overwrite");
        mi.setEnabled(kr != null
                && SWGFrame.getSelectedCharacter().galaxy().exists());

        mi.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                actionWriteNews(kr, append);
            }
        });

        return mi;
    }
    
    /**
     * Helper method. If the argument is {@code true} this method tints the
     * monitor GUI tab in red.
     * 
     * @param isAlerted {@code true} if there is a depleted resource which
     *        triggers a monitor, {@code false} otherwise
     */
    private void monitorsAlerted(boolean isAlerted) {
        northEastTabbedPane.setBackgroundAt(1, isAlerted
                ? Color.PINK
                : this.getBackground());
        tintTab('m', isAlerted);
    }

    /**
     * Helper method which determines if at least one monitor in {@code
     * monitors} is alarming, {@code false} otherwise.
     * 
     * @param monList a list of monitors to scan
     * @return {@code true} if at least one monitor in {@code monitors} is
     *         depleted, {@code false} otherwise
     */
    private boolean monitorsAlerted(List<SWGMonitor> monList) {
        for (SWGMonitor m : monList)
            if (m.getResource().isDepleted())
                return true;

        return false;
    }

    /**
     * Helper method called by clients related to monitors. This method returns
     * a list of resource monitors for the current galaxy and as a side effect,
     * if there is depleted resources that is monitored this method tints the
     * monitor GUI tab in red.
     * 
     * @return a list of resource monitors for the current galaxy
     */
    private List<SWGMonitor> monitorsGet() {
        if (!isGuiFinished)
            return Collections.emptyList();

        if (monitorsList == null) {
            monitorsList =
                    SWGResController.monitors(SWGResourceTab.galaxy());
            monitorsAlerted(monitorsAlerted(monitorsList));
        }
        return monitorsList;
    }

    /**
     * Helper method for {code actionMonitorsTableMouseRB(MouseEvent)}. This
     * method creates and displays a popup dialog with options to handle the
     * selected monitor. Each option is given its individual action listener.
     * 
     * @param row the selected row at the list of resource monitors, <B>not</B>
     *        converted to the model
     * @param e the event that initiates the call
     */
    @SuppressWarnings("synthetic-access")
    private void monitorsShowPopup(int row, MouseEvent e) {
        monitorsTable.getSelectionModel().setSelectionInterval(row, row);
        int rw = monitorsTable.convertRowIndexToModel(row);
        final SWGMonitor monitor = monitorsGet().get(rw);
        final SWGKnownResource res = monitor.getResource();
        final SWGResourceClass rc = res.rc();

        JPopupMenu ppp = new JPopupMenu();

        JMenuItem notes = new JMenuItem("Notes");
        notes.setToolTipText("Add / edit notes for the selected monitor");
        notes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e1) {
                actionMonitorNotes(monitor);
            }
        });
        ppp.add(notes);
        ppp.addSeparator();

        ppp.add(SWGResController.currentSelectMenu(res));
        ppp.add(SWGSchemController.resClassUse(rc));
        ppp.add(SWGSchemResViewer.displayMenu(res, this));
        updateViewer = true; // by chance, it is reset if...

        if (rc != null && rc.isSub(SWGCreatureResources.class))
            ppp.add(SWGResController.creatureHarvMenu((SWGCreatureResources)
                    rc, monitorsTable));

        ppp.addSeparator();

        ppp.add(inventoryAddMenu(res));

        // Select res-class in inventory
        ppp.add(SWGResController.inventoryFilterMenu(rc, null, res, true));

        ppp.addSeparator();

        JMenuItem markDepleted = new JMenuItem("Mark depleted");
        markDepleted.setToolTipText(
                "Send depleted message to SWGCraft.org about this resource");
        markDepleted.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                resourceTab.actionSendDepleted(res);
            }
        });
        markDepleted.setEnabled(!res.isDepleted());
        ppp.add(markDepleted);

        JMenuItem remove = new JMenuItem("Remove");
        remove.setToolTipText("Stop monitor this resource");
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                SWGResController.monitorRemove(monitor);
                updateMonitors();
            }
        });
        ppp.add(remove);

        ppp.show(monitorsTable, e.getX(), e.getY());
    }

    /**
     * Helper method which clears any selection at the main table and triggers
     * the GUI to update itself.
     */
    private void resetCurrent() {
        currentList = null;
        currentResourcesTable.clearSelection(); // >> actionCurrentSelection()
    }

    /**
     * Helper method which clears any selected resource guard and triggers its
     * GUI to update itself.
     */
    private void resetGuards() {
        guardsList = null;
        guardsTable.clearSelection(); // >> guardSelection()
        updateGuards();
    }

    /**
     * This method completely resets the display of this component and is
     * invoked if the the users selects another galaxy or if the users presses
     * the "Clear" button. This implementation resets the view of resources,
     * guards, and monitors, this includes selections and sorted columns are
     * cleared; user control options are not reset. Finally this method triggers
     * a refresh of GUI components for current resources, resource guards, and
     * monitors.
     */
    private void resetTab() {
        selectedGuard = null;
        selectedResource = null;
        disableColoring = false;
        currentResourcesTable.getRowSorter().setSortKeys(null);
        guardsTable.getRowSorter().setSortKeys(null);

        // the order is important as some de/selections triggers event calls
        filterFieldsClear();
        resourceClassTree.clearSelection(); // >> resourceClassSelection
        for (int i = resourceClassTree.getRowCount() - 1; i > 0; --i) {
            TreePath tp = resourceClassTree.getPathForRow(i);
            resourceClassTree.collapsePath(tp);
        }
        resetGuards(); // >> guardSelection()
        resetCurrent(); // >> actionCurrentSelection()
        updateMonitors();
        updateCurrent();
        currentResourcesTable.scrollRectToVisible(
                currentResourcesTable.getCellRect(0, 0, true));
        resourceClassTree.scrollRowToVisible(0);
    }

    /**
     * Helper method which determines which resource class is selected at {code
     * resourceClassTree} and updates {code selectedResourceClass} accordingly.
     * If there is no node selected at the resource class tree, or if the
     * selected node is the "All Resources", the field is set to {@code null}.
     * If there is a node selected this method clears any selection at the table
     * of resource guards.
     * 
     * @return {@code true} if {code selectedResourceClass} is set to a real
     *         value, false if it is set to {@code null}
     */
    private boolean resourceClassSelection() {
        selectedResourceClass = null; // set field later if it should
        selectedGuard = null;

        TreePath tp = resourceClassTree.getSelectionPath();
        if (tp == null)
            return false;

        // OK, a resource class is selected, dismiss any guard
        guardsTable.clearSelection();
        monitorsTable.clearSelection();

        SWGResourceClass sel = (SWGResourceClass) tp.getLastPathComponent();
        if (sel != SWGResourceClass.INSTANCE)
            selectedResourceClass = sel;

        return true;
    }

    /**
     * Helper method which creates and displays a GUI popup dialog with options
     * to handle the selected monitor. Each option is given its individual
     * action listener. As a side effect this method selects the node the user
     * clicked at which updates the main table.
     * 
     * @param e the event that initiates the call
     */
    private void resourceClassTreeShowPopup(MouseEvent e) {
        int row = resourceClassTree.getRowForLocation(e.getX(), e.getY());
        if (row <= 0) // disregard, "All Resources" is at index 0
            return;

        // sets the resource class and updates the main table
        resourceClassTree.setSelectionRow(row);

        final SWGResourceClass rc = selectedResourceClass;
        if (rc == null)
            return;

        JPopupMenu popup = new JPopupMenu();

        popup.add(makeResClassDisableColoring());
        popup.add(makeResClassCreateGuardMenu(rc));
        popup.add(SWGSchemController.resClassUse(rc));
        if (rc.isSub(SWGCreatureResources.class))
            popup.add(SWGResController.creatureHarvMenu(
                    (SWGCreatureResources) rc, resourceClassTree));

        popup.show(resourceClassTree, e.getX(), e.getY());
    }

    /**
     * Helper method which returns a background color, or {@code null}. This
     * method returns the "fair" color if the specified resource is stocked in
     * inventory, otherwise if the resource class is not possible to harvest in
     * the worlds it returns {@link SWGGuiUtils#colorNonHarvested}, otherwise it
     * returns {@code null}.
     * 
     * @param inventoryAmount the inventory stock of the specified resource
     * @param res a resource
     * @return a background color, or {@code null}
     */
    private Color stockedHarvested(long inventoryAmount, SWGKnownResource res) {
        if (res == null) return null;
        if (inventoryAmount >= 0)
            return SWGGuiUtils.statColors[0];
        if (res.rc().isSub(SWGCreatureResources.class)
                && !res.rc().isHarvested())
            return SWGGuiUtils.colorNonHarvested;

        return null;
    }

    /**
     * Helper method which tints the tab for this panel at the tabbed pane. The
     * source argument is either monitors or guards and this method queries the
     * other part to ensure the priority is maintained, a monitor that alerts
     * for a depleted resource has higher priority than a guard that alerts. If
     * neither a monitor nor a guard alerts this method sets a neutral color.
     * 
     * @param src the source for of the call, {@code 'm'} or {@code 'g'}
     * @param alert {@code true} if an element at the source is alerting
     */
    private void tintTab(char src, boolean alert) {
        if (isTinting) return;
        isTinting = true;
        int a = 0;
        if (src == 'm') {
            if (alert)
                a = -1;
            else if (guardsAlerted(guardsGet()))
                a = 1;
        } else { // if (src == 'g') {
            if (monitorsAlerted(monitorsGet()))
                a = -1;
            else if (alert)
                a = 1;
        }

        resourceTab.tintTab(a, 0);
        isTinting = false;
    }

    /**
     * Helper method which returns a tool-tip text, or {@code null}.
     * 
     * @param inventoryAmount the inventory stock of the specified resource
     * @param res a resource
     * @return a tool-tip text, or {@code null}
     */
    private String toolTipText(long inventoryAmount, SWGKnownResource res) {
        if (res == null) return null;

        if (inventoryAmount >= 0)
            return String.format("Units owned: %s",
                    ZNumber.asText(inventoryAmount, true, true));

        if (res.rc().isSub(SWGCreatureResources.class)
                && !res.rc().isHarvested())
            return "Not possible to harvest in the worlds";

        return null;
    }

    /**
     * Helper method which triggers an update of the GUI table of current
     * resources.
     */
    private void updateCurrent() {
        SWGKnownResource sr = selectedResource;
        selectedResource = null;
        currentList = null;
        resourceDetails.setText(null);

        currentResourcesModel.fireTableDataChanged();

        // retain the selection, if possible
        if (sr != null && sr.galaxy() == SWGFrame.getSelectedGalaxy()) {
            selectedResource = sr;
            SWGResourceSet cl = currentGet();
            for (int i = 0; i < cl.size(); ++i) {
                if (sr == cl.get(i)) {
                    int j = currentResourcesTable.convertRowIndexToView(i);
                    currentResourcesTable.
                            getSelectionModel().setSelectionInterval(j, j);
                    currentResourcesTable.scrollRectToVisible(
                            currentResourcesTable.getCellRect(j, 0, true));
                    return;
                }
            }
            selectedResource = null;
            // the selected resource not found in view, correct the issue
        }
    }

    /**
     * This method is called when it is determined that the GUI should be
     * updated. This is when this component gains focus or when the set of
     * current resources is updated.
     */
    void updateDisplay() {
        updateGuards();
        updateCurrent();
        updateMonitors();
    }

    /**
     * Helper method which triggers an update of the GUI table of resource
     * guards.
     */
    private void updateGuards() {
        SWGGuard sg = selectedGuard;
        selectedGuard = null;
        guardsList = null;

        guardsModel.fireTableDataChanged();

        if (sg != null) {
            // retain the selection, if possible
            List<SWGGuard> gl = guardsGet();
            for (int i = 0; i < gl.size(); ++i) {
                if (sg == gl.get(i)) {
                    selectedGuard = sg;
                    int j = guardsTable.convertRowIndexToView(i);
                    guardsTable.getSelectionModel().setSelectionInterval(j, j);
                    return;
                }
            }
            // the selected resource not found in view, correct the issue
        }
    }

    /**
     * Helper method which updates the list of resource monitors which triggers
     * an update of the table of monitors.
     */
    private void updateMonitors() {
        monitorsList = null;
        monitorsModel.fireTableDataChanged();
    }

    /**
     * Helper method for the model for the main GUI table, used for the "rate"
     * column. This method returns a weighed object with evenly distributed
     * weights based on the caps of the SWGResource passed in, or, in the case
     * values are entered at the filter input fields these are used.
     * 
     * @return resource class weights
     */
    private SWGWeights weightFromResourceCaps() {
        if (temporaryWeight == null) {
            SWGWeights wgt;
            int[] filter = filterFieldsArray();

            if (SWGWeights.isValid(filter)) {
                // use the weights the user has entered, if any
                wgt = new SWGWeights(filter);

            } else if (SWGValues.sum(filter) > 0) {
                // use the values added as a plain filter but adjust as weights
                wgt = new SWGWeights(filter, false);
                wgt.adjust();

            } else {
                int[] stats = new int[Stat.COUNT];

                if (selectedResourceClass != null) {
                    int iNumStats = selectedResourceClass.expectedStats();
                    // divide by zero sanity check
                    if (iNumStats <= 0)
                        iNumStats = Stat.COUNT;

                    int iWeight = 100 / iNumStats;

                    // evenly distribute the weight across all valid stats
                    for (Stat s : Stat.values())
                        stats[s.i] = selectedResourceClass.has(s)
                                ? iWeight
                                : 0;

                    wgt = new SWGWeights(stats, false);
                    wgt.adjust();
                } else {
                    Arrays.fill(stats, 100 / Stat.COUNT);
                    wgt = new SWGWeights(stats, false);
                }
            }
            temporaryWeight = wgt;
        }
        return temporaryWeight;
    }

    /**
     * The model for the table of resource guards.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private class GuardsModel extends AbstractTableModel 
        implements DecoratedTableModel {

        /**
         * The table column header titles.
         */
        private final String[] colNames = { "Guard", "Class", "Values", "Min" };

        @SuppressWarnings("synthetic-access")
        @Override
        public TableCellDecorations getCellDecor(int row, int column,
                Object value) {

            SWGGuard guard = guardsGet().get(row);
            Color bg = guard.resourceRecentSpawnDate > 0
                    ? SWGGuiUtils.colorAlert
                    : null;
            return new TableCellDecorations(bg, null, null, (Object[]) null);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex <= 2) return String.class;
            return Integer.class;
        }

        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return colNames[col];
        }

        @SuppressWarnings("synthetic-access")
        public int getRowCount() {
            return guardsGet().size();
        }

        @SuppressWarnings("synthetic-access")
        public Object getValueAt(int rowIndex, int columnIndex) {

            SWGGuard guard = guardsGet().get(rowIndex);

            switch (columnIndex) {
            case 0:
                return guard.getName();
            case 1:
                return guard.rc().rcName();
            case 2: {
                ZString z = new ZString();
                guard.logic().toString(z, false);
                return z.toString();
            }
            case 3:
                return Integer.valueOf(guard.threshold());
            default:
                return null;
            }
        }
    }

    /**
     * The model for the table of resource monitors.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private class MonitorModel extends AbstractTableModel 
        implements DecoratedTableModel {

        /**
         * The table column header titles.
         */
        private final String[] colNames = { "Resource", "Class", "Age" };

        @SuppressWarnings("synthetic-access")
        @Override
        public TableCellDecorations getCellDecor(int row, int column,
                Object value) {

            SWGMonitor monitor = monitorsGet().get(row);

            if (column == 2)
                return SWGResController.resourceAgeDecor(
                        monitor.getResource(), ((Long) value).longValue(), null);

            Color bg = monitor.isDepleted()
                    ? Color.PINK
                    : null;
            String tt = monitor.notes().isEmpty()
                    ? null
                    : monitor.notes();

            return new TableCellDecorations(bg, null, tt, (Object[]) null);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex <= 1) return String.class;
            return Long.class;
        }

        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @SuppressWarnings("synthetic-access")
        public int getRowCount() {
            return monitorsGet().size();
        }

        @SuppressWarnings("synthetic-access")
        public Object getValueAt(int rowIndex, int columnIndex) {
            SWGMonitor monitor = monitorsGet().get(rowIndex);
            SWGKnownResource resource = monitor.getResource();

            switch (columnIndex) {
            case 0:
                return resource.getName();
            case 1:
                return resource.rc().rcName();
            case 2:
                long t = System.currentTimeMillis() / 1000;
                SWGPlanetAvailabilityInfo pa = resource.availableFirst();
                t -= pa.available();
                return Long.valueOf(t);
            default:
                return null;
            }
        }
    }

    /**
     * The table model for the main table of current resources. This model
     * implements {@link DecoratedTableModel}.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private class ResourcesTableModel extends AbstractTableModel
        implements DecoratedTableModel {

        /**
         * Table column header titles.
         */
        private final String[] columnNames =
                { "Name", "Class", "ER", "CR", "CD", "DR", "FL", "HR", "MA",
                        "PE", "OQ", "SR", "UT", "Rate", "Age" };

        /**
         * A convenience constant array of stats in game order.
         */
        private final Stat[] gOrder = Stat.gameOrder();

        @SuppressWarnings("synthetic-access")
        @Override
        public TableCellDecorations getCellDecor(
                int row, int column, Object value) {
            SWGKnownResource res = currentGet(row);
            if (res == null) return null;

            if (column <= 1 && res.stats().sum() <= 0)
                return new TableCellDecorations( // a stat-less resource
                        Color.PINK, null, null, (Object[]) null);

            long amt = SWGResController.inventoryAmount(
                    res, SWGResourceTab.galaxy());
            String toolTip = toolTipText(amt, res);

            if (column <= 1)
                return new TableCellDecorations(
                        stockedHarvested(amt, res), null, toolTip,
                        (Object[]) null);

            // else...

            SWGResourceClass capRes =
                    selectedResourceClass != null && !disableColoring
                            ? selectedResourceClass
                            : selectedGuard != null
                                    ? selectedGuard.rc()
                                    : res.rc();

            if (column <= 12) { // stats capRes.max(j));
                Stat s = gOrder[column - 2];
                int stat = res.stats().value(s);
                return new TableCellDecorations(
                        SWGResourceStatRenderer.getStatBackGround(
                                stat, capRes.max(s)),
                        SWGResourceStatRenderer.getStatForeground(
                                stat, capRes.max(s)),
                        toolTip, (Object[]) null);
            }
            if (column == 13) {
                int i = ((Integer) value).intValue();
                return new TableCellDecorations(
                        SWGResourceStatRenderer.getStatBackGround(i / 1000.0),
                        SWGResourceStatRenderer.getStatForeground(i / 1000.0),
                        toolTip, (Object[]) null);
            }
            if (column == 14) {
                long l = ((Long) value).longValue();
                return SWGResController.resourceAgeDecor(res, l, null);
            }
            return null;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex <= 1) return String.class;
            if (columnIndex == 14) return Long.class;
            return Integer.class;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @SuppressWarnings("synthetic-access")
        public int getRowCount() {
            return currentGet().size();
        }

        @SuppressWarnings("synthetic-access")
        public Object getValueAt(int rowIndex, int columnIndex) {
            SWGKnownResource res = currentGet(rowIndex);
            if (res == null) return null;

            SWGResourceClass capRes = selectedResourceClass != null
                    ? selectedResourceClass
                    : selectedGuard != null
                            ? selectedGuard.rc()
                            : res.rc();

            switch (columnIndex) {
            case 0:
                return res.getName();
            case 1:
                return res.rc().rcName();
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
                return Integer.valueOf(res.stats().value(s));
            }
            case 13: { // rate
                SWGWeights weights = null;
                SWGGuard guard = selectedGuard;

                // if a guard is currently selected and it is a weight
                // let it determine the rating
                if (guard != null && guard.isWeighted())
                    weights = (SWGWeights) guard.logic();
                
                else if (temporaryWeight != null)
                    weights = temporaryWeight;

                else {
                    // otherwise base the values off the caps of the
                    // currently selected resource class, if any, or
                    // possible filtering values
                    weights = weightFromResourceCaps();
                }

                // using the weights system to take advantage of caps being
                // something other than 1000
                int stat = (int) weights.rate(res, capRes, true);
                return Integer.valueOf(stat);
            }
            case 14: { // age
                return Long.valueOf(res.age());
            }
            }
            return null;
        }
    } // table model
}
