package swg.gui.resources;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.Calendar;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import swg.SWGAide;
import swg.crafting.Stat;
import swg.crafting.UpdateNotification;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceSet;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGDecoratedTableCellRenderer.DecoratedTableModel;
import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGJTable;
import swg.gui.common.SWGResourceStatRenderer;
import swg.model.SWGCGalaxy;
import swg.model.SWGNotes;
import swg.model.SWGStation;
import swg.swgcraft.SWGResourceManager;
import swg.swgcraft.SWGSoapEditResResponse;
import swg.tools.ZString;
import swg.tools.ZStuff;
import swg.tools.ZWriter;

/**
 * This GUI element displays resources which are depleted recently. SWGAide does
 * not purge depleted resource right away but after some days, this way they are
 * available in the resource manager should the user for example add an entry to
 * the inventory somewhat after the resource is depleted, with no need to look
 * it up at SWGCraft.org. Also, any resource that is contained in a persistent
 * repository (inventory or any {@link SWGResourceSet}) that is stored in
 * SWGAide's DAT file.
 * <p>
 * This type is currently not meant to provide any other means than recently
 * depleted resources. This may change with valid requests in the future.
 * <p>
 * <b>Notice:</b> A number of variables and methods are static because this
 * class can be invoked by SWGResourceTab already before it is instantiated.
 * Hence, it is an error to create more than one instance of this type, however
 * not guarded against.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGDepletedTab extends JPanel {

    
    /**
     * An age comparator to sort the set of depleted resources.
     */
    private static Comparator<SWGKnownResource> ageComparator;

    /**
     * A set of depleted resources for the current galaxy.
     */
    private static SWGResourceSet depleted;

    /**
     * The URL for the help page for this view.
     */
    private final URL helpPage;

    /**
     * A flag that denotes if the GUI parts of this element are created or not.
     * This type is created lazily and sets this member {@code true} when done.
     */
    private boolean isGuiCreated;

    /**
     * A reference to the tabbed pane that contains this panel.
     */
    private final SWGResourceTab resourceTab;

    /**
     * The GUI table for depleted resources.
     */
    private SWGJTable table;

    /**
     * Creates an instance of this type.
     * 
     * @param resourceTab the tabbed pane that contains this panel
     */
    SWGDepletedTab(SWGResourceTab resourceTab) {
        this.resourceTab = resourceTab;

        helpPage = SWGAide.class.getResource(
                "docs/help_resources_depleted_en.html");

        // create GUI content lazily, see focusGained()
        resourceTab.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });
    }

    /**
     * Called when the user selects the option to add a resource to inventory.
     * This displays the add-to-inventory dialog for the specified resource and
     * the currently selected character.
     * 
     * @param kr the resource
     */
    private void actionInventory(SWGKnownResource kr) {
        String ass = SWGFrame.getSelectedCharacter().getName();

        SWGInventoryWrapper wr = new SWGInventoryWrapper(kr, ass);

        SWGResourceTab.inventoryTab().dialog().showAndBegin(
                wr);
    }

    /**
     * Called when the user selects the option at the popup menu to specify a
     * limit for the written output. This method displays a dialog with a number
     * spinner for the valid interval, if the user selects OK the selected value
     * is saved to SWGAide's preference keeper.
     */
    private void actionLimit() {
        int limit = writeAge();
        JSpinner sp = new JSpinner(new SpinnerNumberModel(
                limit, 1, SWGResource.RETIREMENT, 1));
        sp.setToolTipText("Specify a limit for the written output (days)");
        if (JOptionPane.OK_OPTION == JOptionPane.showOptionDialog(this, sp,
                "Specify limit (days)", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null)) {

            Integer v = (Integer) sp.getModel().getValue();
            SWGFrame.getPrefsKeeper().add("resourceDepletedLimitDays", v);
        }
    }

    /**
     * Called when the user selects the option to revive the specified resource.
     * This method prompts the user to verify the decision, and then submits the
     * revival request to SWGCraft.org via SWGAide's resource manager.
     * 
     * @param kr the resource to revive
     * @throws NullPointerException if the argument is {@code null}
     */
    private void actionRevive(SWGKnownResource kr) {
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(this,
                    "Submit \"revive\" to SWGCraft.org for:\n" +
                            kr.getName() + ", " + kr.rc().rcName(),
                    "Confirm Revive", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE)) {

            SWGSoapEditResResponse r = SWGResourceManager.sendRevive(kr);
            SWGAide.printDebug("depl", 1, ZString.fs(
                    "SWGDepletedTab:revive: %s %s%n\t%s",
                    kr.getName(), kr.rc().rcName(), r.getStatusString()));

            if (!r.isFaultless()) JOptionPane.showMessageDialog(this,
                    "ERROR: " + r.getFaultMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Called when the user mouse-clicks the table. If it is a right-click this
     * method displays a popup dialog.
     * 
     * @param e the event that triggers the call
     */
    private void actionTableMouse(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON3) return;

        SWGKnownResource kr = null;
        synchronized (SWGDepletedTab.class) {
            // do it here to keep scope small and obtain resource while safe
            int r = table.rowAtPoint(e.getPoint());
            if (r >= 0) {
                r = table.convertRowIndexToModel(r);
                table.getSelectionModel().setSelectionInterval(r, r);
                kr = getDepleted().get(r);
            }
        }
        final JPopupMenu popup = new JPopupMenu();

        addWrite(popup);

        popup.addSeparator();

        addLimit(popup);
        addFileNotesRadioButtons(popup);
        addAuto(popup);

        popup.addSeparator();

        addRevive(popup, kr);

        popup.addSeparator();

        addInventory(popup, kr);
        SWGResourceClass rc = kr == null
                ? null
                : kr.rc();
        popup.add(SWGResController.inventoryFilterMenu(rc, null, kr, true));

        popup.show(table, e.getX(), e.getY());
    }

    /**
     * Called when the user selects to write depleted resource to file. If the
     * user has selected one or more resources at the table only these are
     * written, otherwise this method obtains further options and writes to file
     * or to in.game notes file.
     */
    private void actionWrite() {
        SWGResourceSet depl;
        synchronized (SWGDepletedTab.class) {
            depl = getDepleted();
            int[] sel = table.getSelectedRows();

            if (sel.length > 0) {
                SWGResourceSet d = new SWGResourceSet(sel.length);
                for (int i = 0; i < sel.length; ++i) {
                    // the table may be sorted somehow, convert to model
                    int j = sel[i];
                    j = table.convertRowIndexToModel(j);
                    d.add(depl.get(j)); // pick selected
                }
                depl = d;
            }

        }
        SWGCGalaxy gxy = SWGResourceTab.galaxy();
        String sd = writeDepleted(depl, gxy);
        if (writeToFile()
                || !SWGFrame.getSelectedCharacter().galaxy().exists())
            writeDepleted(sd, gxy);
        else
            writeNotes(sd);

    }

    /**
     * Helper method which creates and adds a check-box for auto-write to the
     * specified popup dialog.
     * 
     * @param popup a popup dialog
     */
    private void addAuto(JPopupMenu popup) {
        boolean b = writeAuto();
        final JCheckBoxMenuItem auto =
                new JCheckBoxMenuItem("Write option: Auto-Write  ", b);
        auto.setToolTipText("Enable auto-write to file for main galaxy");
        popup.add(auto);

        auto.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("resourceDepletedAutoWrite",
                        Boolean.valueOf(auto.isSelected()));
            }
        });
    }

    /**
     * Helper method which creates and adds the file versus notes-file radio
     * buttons to the specified popup dialog.
     * 
     * @param popup a popup menu
     */
    private void addFileNotesRadioButtons(final JPopupMenu popup) {
        JMenu write = new JMenu("Write option: File / Notes");
        write.setToolTipText("Option: write to file or to in-game notes file");
        popup.add(write);

        final JRadioButton file = new JRadioButton("Write to File");
        file.setToolTipText("Write output to file in SWGAide\\misc\\");
        write.add(file);

        JRadioButton notes = new JRadioButton("Write to Notes");
        notes.setToolTipText("Write output to in-game notes file");
        write.add(notes);

        ButtonGroup rGroup = new ButtonGroup();
        rGroup.add(notes);
        rGroup.add(file);

        boolean bFile = writeToFile();
        rGroup.setSelected(file.getModel(), bFile);
        rGroup.setSelected(notes.getModel(), !bFile);

        ActionListener nfAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("resourceDepletedWriteFile",
                        Boolean.valueOf(file.isSelected()));
                popup.setVisible(false);
            }
        };
        notes.addActionListener(nfAction);
        file.addActionListener(nfAction);
    }

    /**
     * Helper method which creates and adds an option to the specified popup
     * menu. This option opens the add-to-inventory dialog for the specified
     * resource.
     * 
     * @param popup a popup dialog
     * @param kr the selected resource, or {@code null} if none is selected
     */
    private void addInventory(JPopupMenu popup, final SWGKnownResource kr) {
        JMenuItem inv = makeMenuItem("Add to Inventory...",
                "Add one selected resource to inventory",
                new ActionListener() {

                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        actionInventory(kr);
                    }
                });
        inv.setEnabled(kr != null);
        popup.add(inv);
    }

    /**
     * Helper method which creates and adds an option for limit to the specified
     * popup menu. This option item opens another dialog with a GUI spinner.
     * 
     * @param popup a popup menu
     */
    private void addLimit(JPopupMenu popup) {
        JMenuItem limit = makeMenuItem("Write option: Limit days...",
                "Specify a limit for the written output, in days",
                new ActionListener() {

                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        actionLimit();
                    }
                });
        popup.add(limit);
    }

    /**
     * Helper method which creates and adds a menu option to the specified popup
     * menu. The option is to submit a revive request to SWGCraft.
     * 
     * @param popup a popup menu
     * @param kr the selected resource, or {@code null} if none is selected
     */
    private void addRevive(JPopupMenu popup, final SWGKnownResource kr) {
        String tt = kr != null
                ? "Submit revive message to SWGCraft for the selected resource"
                : "Select one resource to revive";

        JMenuItem revive = makeMenuItem("Revive...", tt, new ActionListener() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                actionRevive(kr);
            }
        });
        revive.setEnabled(kr != null);
        popup.add(revive);
    }

    /**
     * Helper method which adds the "write" option to the specified popup menu.
     * When the use selects this option depleted resources are written to file,
     * everything determined by the other options.
     * 
     * @param popup a popup menu
     */
    private void addWrite(JPopupMenu popup) {
        JMenuItem write =
                makeMenuItem("Write", "Write depleted resources to a file",
                        new ActionListener() {

                            @SuppressWarnings("synthetic-access")
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                actionWrite();
                            }
                        });
        popup.add(write);
    }

    /**
     * Helper method which displays some info at SWGAide's log bar, the number
     * of depleted resources on display and the most recent fetch from
     * SWGCraft.org.
     * 
     * @param depl the set of depleted resources
     */
    private void depletedToLogBar(SWGResourceSet depl) {
        SWGFrame f = SWGAide.frame();
        if (depl == null) {
            f.putToLogbar_2(null);
            return;
        }
        Long time = SWGResourceManager.getStatusLocalTime(SWGResourceTab.galaxy());
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time.longValue() * 1000L);
        String tstr = String.format(
                "%s depleted the last %s days (%d:%d) from SWGCraft.org",
                Integer.valueOf(depl.size()),
                Integer.valueOf(SWGResource.RETIREMENT),
                Integer.valueOf(c.get(Calendar.HOUR_OF_DAY)),
                Integer.valueOf(c.get(Calendar.MINUTE)));
        f.putToLogbar_2(tstr);
    }

    /**
     * This method is called for two different action events, in both cases it
     * is determined if this component has focus and which action to select:
     * <UL>
     * <LI>if focus is changed for {@link #resourceTab}</LI>
     * <LI>if focus is changed for this component; this is when the user selects
     * the tab for {@code this} component, or any of its siblings</LI>
     * </UL>
     * If this component gains focus this implementation updates its GUI.
     * <p>
     * This method is synchronized on this type's class constant.
     * 
     * @param focused {@code true} if there is any focus change for this
     *        component or its siblings, or if {@link #resourceTab} gained
     *        focus; {@code false} otherwise
     */
    void focusGained(boolean focused) {
        if (focused && resourceTab.getSelectedComponent() == this) {
            if (SWGResourceTab.galaxy() == null) {
                JOptionPane.showMessageDialog(this,
                        "No character selected at Main,\n" +
                                "hence no galaxy for resources", "No galaxy",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            synchronized (SWGDepletedTab.class) {
                if (!isGuiCreated)
                        makeInterior(); // lazy creation
            }
            repaint();
            depletedToLogBar(depleted);
            SWGHelp.push(helpPage);
        } else {
            SWGHelp.remove(helpPage);
        }
    }
    
    /**
     * Helper method which returns a list of depleted resources for the current
     * galaxy, or an empty list. No element in the list has been depleted more
     * than {@link SWGResource#RETIREMENT} days.
     * 
     * @return a list of depleted resources, or an empty list
     */
    private SWGResourceSet getDepleted() {
        return getDepleted(SWGResourceTab.galaxy(), SWGResource.RETIREMENT);
    }

    /**
     * Helper method which dispatches the creation of the main table and the
     * bottom panel to their make-methods respectively. Once this method is
     * finished {code isGuiCreated} is set {@code true}.
     */
    private void makeInterior() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(makeMainTable());

        // close on "Alt-C"
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK),
                "depletedAltC");
        getActionMap().put("depletedAltC", new AbstractAction() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent actionEvent) {
                table.getRowSorter().setSortKeys(null);
                table.clearSelection();
            }
        });

        isGuiCreated = true;
    }

    /**
     * Creates and returns the main element for this panel, the table of
     * depleted resources.
     * 
     * @return a GUI element
     */
    @SuppressWarnings("synthetic-access")
    private Component makeMainTable() {
        DecoratedTableModel tableModel = new TableModel();
        table = new SWGJTable(tableModel);

        table.setDefaultRenderer(String.class,
                new SWGResourceStatRenderer(tableModel));
        table.setDefaultRenderer(Integer.class,
                new SWGResourceStatRenderer(tableModel));
        table.setDefaultRenderer(Long.class,
                new SWGDecoratedTableCellRenderer() {
                    @Override
                    protected void mySetValue(Object value) {
                        Long o = (Long) value;
                        String s = SWGResController.dateString(o.longValue());
                        setValue(s);
                    }
                });
        int w;
        w = SWGGuiUtils.fontWidth(this, "1 000", SWGGuiUtils.fontPlain()) + 5;
        SWGGuiUtils.tableSetColumnWidths(table, 2, 999, w, 0);

        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionTableMouse(e);
            }
        });

        JScrollPane jsp = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        return jsp;
    }

    /**
     * Helper method which creates a menu item with the specified text and its
     * tooltip text.
     * 
     * @param txt a text for the menu
     * @param tt a fly over text
     * @param al the action listener for the item
     * @return the menu item
     */
    private JMenuItem makeMenuItem(String txt, String tt, ActionListener al) {
        JMenuItem item = new JMenuItem(txt);
        item.setToolTipText(tt);
        item.addActionListener(al);
        return item;
    }

    /**
     * Resets the GUI table. This method is invoked if the user has switched
     * galaxy, or if the resource manager is updated.
     * <p>
     * This method executes on the event thread as a background task and is
     * synchronized on this type's class constant.
     */
    void resetGUI() {
        synchronized (SWGDepletedTab.class) {
            depleted = null;
            if (isGuiCreated)
                SwingUtilities.invokeLater(new Runnable() {
                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void run() {
                        ((AbstractTableModel) table.getModel()).
                                fireTableDataChanged();
                    }
                });
        }
    }

    /**
     * Helper method which writes the string of depleted resources to an in-game
     * notes file. This method obtains the recently selected character and for
     * its station writes a notes file named "depleted.txt". If the notes file
     * exists a backup copy is written.
     * 
     * @param sd a string of depleted resources
     */
    private void writeNotes(String sd) {
        SWGStation stn = SWGFrame.getSelectedCharacter().galaxy().station();
        String fName = "depleted.txt";
        SWGNotes notes = stn.notes(fName);
        if (notes == null)
            notes = SWGNotes.getInstance(fName, stn, null, false);
        else
            notes.backup();

        notes.setText(sd);
    }

    /**
     * Helper method which returns an age comparator. This comparator compares
     * on depletion dates, the most recently depleted first. If two dates are
     * equal the result is undefined.
     * 
     * @return an age comparator
     */
    private static Comparator<SWGKnownResource> getAgeComparator() {
        if (ageComparator == null)
            ageComparator = new Comparator<SWGKnownResource>() {

                @Override
                public int compare(SWGKnownResource o1, SWGKnownResource o2) {
                    // the filter assures no weird resource dates exist here
                    return (int) (o2.depleted() - o1.depleted());
                }
            };
        return ageComparator;
    }

    /**
     * Helper method which returns a new age filter for depleted resources. This
     * filter accepts only resources that has been depleted a shorter time than
     * the specified number of days and for these elements its method returns 0,
     * for unacceptable elements the filter's method returns -1.
     * <p>
     * The valid interval is <tt>[1 {@link SWGResource#RETIREMENT}]</tt>, if the argument is outside
     * this interval the value the closest end-point is used.
     * 
     * @param days max days
     * @return an age filter
     */
    private static Comparable<SWGKnownResource> getAgeFilter(int days) {
        final int max = days < SWGResource.RETIREMENT
                ? (days < 1
                        ? 1
                        : days)
                : SWGResource.RETIREMENT;
        return new Comparable<SWGKnownResource>() {

            long limit;
            {
                limit = System.currentTimeMillis() / 1000;
                limit -= max * 24 * 3600;
            }

            @Override
            public int compareTo(SWGKnownResource o) {
                long d = o.depleted();
                if (d <= 0 || d == Long.MAX_VALUE) return -1; // sanity
                if (d < limit) return -1;
                return 0; // OK
            }
        };
    }

    /**
     * Helper method which returns a list of depleted resources for the
     * specified galaxy, or an empty list. For details on {@code days}, see
     * {@link #getAgeFilter(int)}.
     * <p>
     * This method is synchronized on this type's class constant.
     * 
     * @param gxy a galaxy constant
     * @param days max age
     * @return a list of depleted resources, or an empty list
     */
    private static SWGResourceSet getDepleted(SWGCGalaxy gxy, int days) {
        synchronized (SWGDepletedTab.class) {
            if (depleted == null) {
                depleted = SWGResourceManager.getDepleted(gxy);
                depleted = depleted.subsetBy(getAgeFilter(days));
                depleted.sort(getAgeComparator());
            }
            return depleted;
        }
    }

    /**
     * Helper method which returns the limit specified by the user, or 3 which
     * is the default value. The return value is in the interval <tt>[1
     * {@link SWGResource#RETIREMENT}]</tt>.
     * 
     * @return max days
     */
    private static int writeAge() {
        return ((Integer) SWGFrame.getPrefsKeeper().get(
                "resourceDepletedLimitDays", Integer.valueOf(3))).intValue();
    }

    /**
     * Helper method which returns {@code true} if the user has opted to
     * auto-write depleted resources to file. Default value is {@code false}.
     * 
     * @return {@code true} if depleted resources should auto-write
     */
    private static boolean writeAuto() {
        return ((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceDepletedAutoWrite", Boolean.FALSE)).booleanValue();
    }

    /**
     * Writes recently depleted resources to file for the specified galaxy. If
     * the user has not enabled auto-write at the popup dialog for this panel,
     * or if the specified galaxy is not the user's main galaxy at is specified
     * in menu Options -> SWGCraft this method does nothing. Otherwise the file
     * selected by the user at the popup dialog is used, either an in-game notes
     * file or the [SWGAide]/misc/depleted_[galaxy].txt.
     * <p>
     * It is {@link SWGResourceTab#handleUpdate(UpdateNotification)} that
     * invokes this method each time the resource manager notifies its
     * subscribers.
     * 
     * @param gxy a galaxy constant
     */
    static void writeAuto(final SWGCGalaxy gxy) {
        if (!writeAuto()) return;

        Integer gid = (Integer) SWGFrame.getPrefsKeeper().get("optionMainGalaxy");
        if (gxy.id() == gid) {
            synchronized (SWGDepletedTab.class) {
                final ExecutorService ex = Executors.newSingleThreadExecutor();
                ex.execute(new Runnable() {                    
                    @SuppressWarnings("synthetic-access")
                    @Override
                    public void run() {
                        int days = writeAge();
                        SWGResourceSet depl = getDepleted(gxy, days);
                        String sd = writeDepleted(depl, gxy);
                        writeDepleted(sd, gxy);
                        ex.shutdown();
                    }
                });                
            }
        }
    }

    /**
     * Helper method which write the specified string of depleted resources to a
     * file for the specified galaxy. The file is located and named:
     * [SWGAide]\misc\depleted_[galaxy].txt
     * 
     * @param sd a string of depleted resources
     * @param gxy a galaxy constant
     */
    private static void writeDepleted(String sd, SWGCGalaxy gxy) {
        try {
            File f = new File("misc", "depleted_" + gxy.getName() + ".txt");
            ZWriter.writeExc(sd, f, false);
        } catch (Throwable e) {
            SWGAide.printDebug("depl", 1, "writeDepleted", e.getMessage());
        }
    }

    /**
     * A helper method which collates and returns a string that are obtained
     * from the specified set of depleted resources.
     * 
     * @param depl a set of depleted resources
     * @param gxy a galaxy constant
     * @return a string of depleted resources
     */
    private static String writeDepleted(SWGResourceSet depl, SWGCGalaxy gxy) {
        ZString z = new ZString("# Depleted resources: ");
        z.app(gxy.getName()).app(' ');
        z.app(ZStuff.dateString(true)).app(" --- ");
        z.appnl(ZStuff.timeString()).nl();

        int i = 0;
        for (SWGKnownResource kr : depl) {
            z.app(kr.getName()).app(", ").appnl(kr.rc().rcName());
            if (++i % 5 == 0) z.nl();
        }

        return z.nl().toString();
    }

    /**
     * Helper method which returns {@code true} if output should be written to
     * file rather than an in-game notes file. Default value is {@code true}.
     * 
     * @return {@code true} for write to file, not in-game notes
     */
    private static boolean writeToFile() {
        return ((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceDepletedWriteFile", Boolean.TRUE)).booleanValue();
    }

    /**
     * The model for the main table of depleted resources.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class TableModel extends AbstractTableModel
        implements DecoratedTableModel {

        /**
         * Table column header titles.
         */
        private final String[] columnNames =
                { "Name", "Class", "ER", "CR", "CD", "DR", "FL", "HR", "MA",
                        "PE", "OQ", "SR", "UT", "Time" };

        /**
         * A convenience constant array of stats in game order.
         */
        private final Stat[] gOrder = Stat.gameOrder();

        @SuppressWarnings("synthetic-access")
        @Override
        public TableCellDecorations getCellDecor(int row, int column,
                Object value) {

            if (value == null) return null;

            SWGKnownResource kr = getDepleted().get(row);

            if (column <= 1 || column >= 13) {
                if (SWGResController.inventoryAmount(
                        kr, SWGResourceTab.galaxy()) >= 0)
                    return new TableCellDecorations(
                            SWGGuiUtils.statColors[0],
                            SWGGuiUtils.statColors[1],
                            null, (Object[]) null);
                // else
                return null;
            }

            Stat s = gOrder[column - 2];
            int val = kr.stats().value(s);
            int cap = kr.rc().max(s);
            return new TableCellDecorations(
                    SWGResourceStatRenderer.getStatBackGround(val, cap),
                    SWGResourceStatRenderer.getStatForeground(val, cap),
                    null, (Object[]) null);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex <= 1) return String.class;
            if (columnIndex == 13) return Long.class;
            return Integer.class;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public int getRowCount() {
            return getDepleted().size();
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SWGKnownResource kr = getDepleted().get(rowIndex);

            switch (columnIndex) {
            case 0:
                return kr.getName();
            case 1:
                return kr.rc().rcName();
            case 2: // fall through, values for stats
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12: {
                Stat s = gOrder[columnIndex - 2];
                int val = kr.stats().value(s);
                return Integer.valueOf(val);
            }
            case 13: { // days
                long now = System.currentTimeMillis() / 1000;
                long dpl = kr.depleted();
                return Long.valueOf(now - dpl);
            }
            default:
                return null;
            }
        }
    }
}
