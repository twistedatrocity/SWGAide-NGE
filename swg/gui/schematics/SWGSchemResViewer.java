package swg.gui.schematics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import swg.SWGAide;
import swg.crafting.SWGWeights;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceClassTree;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.types.SWGCreatureResources;
import swg.crafting.resources.types.SWGOrganic;
import swg.crafting.schematics.SWGSchematic;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGDecoratedTableCellRenderer.DecoratedTableModel;
import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJDialog;
import swg.gui.common.SWGJTable;
import swg.gui.common.SWGResourceStatRenderer;
import swg.gui.common.SWGSac;
import swg.gui.resources.SWGInventoryWrapper;
import swg.gui.resources.SWGResController;
import swg.model.SWGCGalaxy;
import swg.tools.ZString;

/**
 * This viewer displays two tables with schematics and resources respectively.
 * For a given known resource the first table if populated with HQ schematics
 * that can use the resource, sorted by its rate for that schematic; only
 * schematics for which the rate of the resource is better than a selected limit
 * are displayed. When the user selects a schematic the second table populates
 * with all "competing" resources together with the given known resource; this
 * gives an idea of how good the given resource is compared to others.
 * <p>
 * To determine the rates this viewer uses caps of resource classes of the
 * listed schematics and zero-is-max.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGSchemResViewer extends SWGJDialog {

    /**
     * A reference to a client that can update this viewer, or {@code null}. A
     * client invokes {@link #updateDisplay(SWGKnownResource, JComponent)} for
     * continuous updates: if the component argument equals this member this
     * viewer is updated, otherwise not. If another client grabs the viewer this
     * member is updated and when this viewer is made invisible it is nullified;
     * then the reference no longer equals the first client.
     */
    private static JComponent client;

    /**
     * A constant color for named schematics using named resource classes.
     */
    private static final Color NAMED_LQ = new Color(200, 255, 255);

    /**
     * A reference to the only instance of this viewer.
     */
    private static SWGSchemResViewer THIS;

    /**
     * A reference to the currently displayed resource.
     */
    private SWGKnownResource currentRes;

    /**
     * A reference to the currently displayed schematic, or {@code null}. See
     * {@link SWGSchemController#schematics(SWGKnownResource, double, boolean)}
     * for the layout of a sac.
     */
    private SWGSac currentSchem;

    /**
     * A comparator for LQ schematics; this object returns 0 if {@code o.rc() ==
     * currentRes.rc()}, a non-zero value otherwise.
     */
    private final Comparable<SWGKnownResource> NAMED_LQ_COMP;

    /**
     * A check-box for including LQ schematics for "named" resource classes in
     * the display.
     */
    private JCheckBox namedLQ;

    /**
     * The minimum rate for schematics to display.
     */
    private double rateLimit;

    /**
     * A label for the resource class and weights that pertains to a selected
     * schematic.
     */
    private JLabel rcwLabel;

    /**
     * A label for the name and class of the resource.
     */
    private JLabel resNameClass;

    /**
     * A list of resources that competes with the given resource at this viewer.
     * Each element is a sac per the following: <br/>
     * <dl>
     * <dt>obj</dt>
     * <dd>an instance of {@link SWGKnownResource}</dd>
     * <dt>objects.get(0)</dt>
     * <dd>a {@link SWGRCWPair} that pertains to the currently selected
     * schematic and the resource, this object established the rate that is
     * calculated</dd>
     * <dt>objects.get(1)</dt>
     * <dd>a {@link Double} that is the rate of the resource for the rcw-pair</dd>
     * <dt>objects.get(2)</dt>
     * <dd>a {@link Long} the amount of the resource in inventory</dd>
     * </dl>
     */
    private List<SWGSac> resources;

    /**
     * The table for competing resources.
     */
    private SWGJTable resTable;

    /**
     * A list of schematics-rate sacs, or an empty list. This list is defined by
     * {@link SWGSchemController#schematics(SWGKnownResource, double, boolean)}.
     */
    private List<SWGSac> schematics;

    /**
     * The table for schematics.
     */
    private SWGJTable schemTable;

    /**
     * Creates a mode-less dialog of this type.
     */
    private SWGSchemResViewer() {
        super("SWGAide-NGE - Used-by-Schematic Viewer", false, null);
        // if (THIS != null) throw new IllegalStateException("No doubles");

        registerHelp(SWGAide.class.getResource(
                "docs/help_resources_schematics_viewer_en.html"));

        NAMED_LQ_COMP = new Comparable<SWGKnownResource>() {
            
            @Override
            public int compareTo(SWGKnownResource o) {
                return o.rc() == currentRes.rc()
                            ? 0
                            : -1;
            }
        };

        rateLimit = SWGGuiUtils.statLimits[2] * 1000.0; // great value
        schematics = Collections.emptyList();
        resources = Collections.emptyList();

        add(makeNorth(), BorderLayout.NORTH);
        add(makeCenter(), BorderLayout.CENTER);

        // location and dimension
        Point pp = (Point) SWGFrame.getPrefsKeeper().get(
                "schemRes4SchemDialogLocation", new Point(500, 5));
        Dimension dim = (Dimension) SWGFrame.getPrefsKeeper().get(
                "schemRes4SchemDialogDimension", new Dimension(600, 400));
        pp = SWGGuiUtils.ensureOnScreen(pp, dim);
        setLocation(pp);
        setSize(dim);
    }

    /**
     * Called when the user right-clicks at an entry at the table of competing
     * resources. This method displays a popup dialog. If the mouse click is not
     * at an entry this method does nothing.
     * 
     * @param e the event that initiates the call
     */
    private void actionResMouseRB(MouseEvent e) {
        int row = resTable.rowAtPoint(e.getPoint());
        if (row < 0) return;

        resTable.setRowSelectionInterval(row, row);
        row = resTable.convertRowIndexToModel(row);
        SWGSac sac = resources.get(row);
        SWGKnownResource kr = (SWGKnownResource) sac.obj;
        SWGRCWPair rcw = (SWGRCWPair) sac.object(0);

        JPopupMenu ppp = new JPopupMenu();

        SWGWeights w = rcw.filter() instanceof SWGWeights
                ? (SWGWeights) rcw.filter()
                : null;
        ppp.add(SWGResController.inventoryFilterMenu(rcw.rc(), w, kr, false));
        ppp.add(SWGResController.currentFilterMenu(rcw.rc(), w, kr));

        ppp.addSeparator();

        ppp.add(SWGResController.monitorAddMenu(
                kr, ((SWGSchematic) currentSchem.obj).getName()));

        if (kr.rc() instanceof SWGCreatureResources)
            ppp.add(SWGResController.creatureHarvMenu((SWGCreatureResources)
                    kr.rc(), this));

        ppp.show(resTable, e.getX(), e.getY());
    }

    /**
     * Called when the user right-clicks at an entry at the schematics table. If
     * the mouse click is not at an entry this method does nothing.
     * 
     * @param e the event that initiates the call
     */
    private void actionSchemMouseRB(MouseEvent e) {
        int row = schemTable.rowAtPoint(e.getPoint());
        if (row < 0) return;

        schemTable.setRowSelectionInterval(row, row);
        row = schemTable.convertRowIndexToModel(row);

        SWGSac sac = schematics.get(row);
        SWGSchematic s = (SWGSchematic) sac.obj;

        JPopupMenu ppp = new JPopupMenu();

        ppp.add(SWGSchemController.schematicSelectMenu(s, null));

        ppp.show(schemTable, e.getX(), e.getY());
    }

    /**
     * Called when the user selects a schematic at the table of schematics. This
     * method updates the table of competing resources for the specified
     * argument, if it is {@code null} this method clears the resource table.
     * 
     * @param sac a sac with a schematic, or {@code null}
     */
    private void actionSchemSelected(SWGSac sac) {
        if (sac == null) {
            resources = Collections.emptyList();
            rcwLabel.setText(null);
        } else {
            currentSchem = sac;

            SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
            SWGRCWPair rcw = (SWGRCWPair) currentSchem.object(1);
            List<SWGInventoryWrapper> il = SWGResController.inventory(gxy);

            if (rcw.filter() == SWGRCWPair.LQ_FILTER) {
                Double d = Double.valueOf(0);
                SWGResourceSet rs =
                        SWGSchemController.spawning().subsetBy(NAMED_LQ_COMP);
                List<SWGSac> sl = new ArrayList<SWGSac>();
                for (SWGKnownResource kr : rs)
                    sl.add(new SWGSac(kr, rcw, d, null, null));

                for (SWGInventoryWrapper w : il)
                    if (currentRes.rc() == w.getResource().rc()) {
                        SWGKnownResource kr = w.getResource();
                        long inv = SWGResController.inventoryAmount(kr, gxy);
                        Long l = inv > 0
                                ? Long.valueOf(inv)
                                : null;
                        String n = SWGResController.inventoryNotes(kr, gxy);
                        sl.add(new SWGSac(kr, rcw, d, l, n));
                    }

                resources = sl;
                rcwLabel.setText(ZString.fs(" RC: %s ", rcw.rc()));
            } else {
                SWGSchematic s = (SWGSchematic) currentSchem.obj;

                SWGSchematicWrapper w = SWGSchemController.wrapperDefault(s);
                SWGExperimentWrapper.refresh(w.experiments(),
                        SWGSchemController.spawning(), il);

                SWGResourceSet rs = new SWGResourceSet();
                for (SWGExperimentWrapper ew : w.experiments())
                    if (ew.rc() == rcw.rc()
                            && ew.weights().equals(rcw.filter()))
                        rs.addAll(ew.resources());

                List<SWGSac> sl = new ArrayList<SWGSac>(rs.size());
                for (SWGKnownResource kr : rs) {
                    if (rcw.rc() == SWGOrganic.getInstance() /* skip creature */
                            && kr.rc() instanceof SWGCreatureResources)
                        continue;

                    double r = ((SWGWeights)
                            rcw.filter()).rate(kr, rcw.rc(), true);
                    if (r >= rateLimit) {
                        long inv = SWGResController.inventoryAmount(kr, gxy);
                        Long l = inv > 0
                                ? Long.valueOf(inv)
                                : null;
                        String n = inv > 0
                                ? SWGResController.inventoryNotes(kr, gxy)
                                : null;
                        sl.add(new SWGSac(kr, rcw, Double.valueOf(r), l, n));
                    }
                }
                Collections.sort(sl, new Comparator<SWGSac>() {
                    @Override
                    public int compare(SWGSac o1, SWGSac o2) {
                        int c = ((Double) o2.object(1)).
                                compareTo((Double) o1.object(1)); // reverse
                        return c != 0
                                ? c
                                : ((SWGResource) o1.obj).compareTo(
                                        (SWGResource) o2.obj);
                    }
                });
                resources = sl;

                ZString z = new ZString(" RC&W: ").app(rcw.rc()).app(":  ");
                rcw.filter().toString(z, false);
                rcwLabel.setText(z.app(' ').toString());
            }
        }

        Dimension pd = getMinimumSize();
        Dimension sd = getSize();
        if (pd.width > sd.width) {
            pd = new Dimension(pd.width, sd.height);
            setLocation(SWGGuiUtils.ensureOnScreen(getLocation(), pd));
            setSize(pd);
        }

        // finally
        ((AbstractTableModel) resTable.getModel()).fireTableDataChanged();
        resTable.scrollRectToVisible(new Rectangle(0, 0, 10, 10));
    }

    @Override
    protected void close() {
        client = null;
        SWGFrame.getPrefsKeeper().add(
                "schemRes4SchemDialogLocation", getLocation());
        SWGFrame.getPrefsKeeper().add(
                "schemRes4SchemDialogDimension", getSize());
    }

    /**
     * Helper method which carries out the job explained in
     * {@link #updateDisplay(SWGKnownResource, JComponent)}.
     * 
     * @param kr a known resource
     */
    private void display(SWGKnownResource kr) {
        currentRes = kr;
        schematics = SWGSchemController.schematics(kr, rateLimit, false);

        if (namedLQ.isSelected())
            schematics.addAll(0, SWGSchemController.schematicsLQNamed(kr));

        resNameClass.setText(String.format(" %s (%s) ",
                kr.getName(), kr.rc().rcName()));

        ((AbstractTableModel) schemTable.getModel()).fireTableDataChanged();
        schemTable.scrollRectToVisible(new Rectangle(0, 0, 10, 10));

        // update resource table if just one schematic or if all have same rcw
        // else reset that table
        SWGSac sac = null;
        if (!schematics.isEmpty()) {
            sac = schematics.get(0);
            Object rcw = sac.object(1); // a rcw-pair but...
            for (SWGSac s : schematics)
                if (!s.object(1).equals(rcw)) {
                    sac = null;
                    break;
                }
        }
        actionSchemSelected(sac);
    }

    /**
     * Helper method that creates and returns the center element of this viewer.
     * This element contains the tables of schematics and resources.
     * 
     * @return a GUI component
     */
    private Box makeCenter() {
        Box hb = Box.createHorizontalBox();
        hb.add(makeWest());
        hb.add(makeEast());
        return hb;
    }

    /**
     * Helper method that creates and returns the east element of this viewer.
     * This is the component that contains the table of resources.
     * 
     * @return a GUI component
     */
    private Component makeEast() {
        
        RTableModel tm = new RTableModel();
        resTable = new SWGJTable(tm);

        resTable.setDefaultRenderer(String.class,
                new SWGDecoratedTableCellRenderer(tm) {
                    @Override
                    protected void myFont(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row,
                            int column, TableCellDecorations decor) {
                        setFont((Font) decor.value());
                    }
                });
        resTable.setDefaultRenderer(Double.class,
                new SWGResourceStatRenderer(tm) {
                    @Override
                    protected void myFont(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row,
                            int column, TableCellDecorations decor) {
                        setFont((Font) decor.value());
                    }
                });

        int w;
        w = SWGGuiUtils.fontWidth(resTable, "1 000.00", resTable.getFont());
        SWGGuiUtils.tableSetColumnWidths(resTable, 0, 0, w + 5, 5);
        SWGGuiUtils.tableSetColumnWidths(resTable, 3, 99, w + 20, 5);
        SWGGuiUtils.setRowHeight(resTable);

        resTable.setAutoCreateRowSorter(false);
        resTable.getTableHeader().setReorderingAllowed(false);
        resTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        resTable.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3)
                    actionResMouseRB(e);
            }
        });

        JScrollPane jsp = new JScrollPane(resTable);
        jsp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        return jsp;
    }

    /**
     * Helper method that creates and returns the north element of this viewer.
     * This component displays the given known resource and some options to
     * govern the output.
     * 
     * @return a GUI component
     */
    private Component makeNorth() {
        Box hb = Box.createHorizontalBox();
        hb.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        Box br = Box.createHorizontalBox();
        br.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        String tt = "Minimum rate for the resource versus schematic";
        JLabel l = new JLabel(" Min rate: ");
        l.setToolTipText(tt);
        br.add(l);

        final String[] spinVals = { "  Great  ", "  Good  ", "  Fair  " };
        final JSpinner spin = new JSpinner(new SpinnerListModel(spinVals));
        SWGGuiUtils.setDim(spin, " Great ", 50, 12, true);
        spin.setToolTipText(tt);

        DefaultEditor ed = new JSpinner.DefaultEditor(spin);
        ed.getTextField().setBackground(Color.WHITE);
        spin.setEditor(ed);

        spin.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                String v = (String) spin.getValue();
                if (v.equals(spinVals[0]))
                    rateLimit = SWGGuiUtils.statLimits[2] * 1000.0;
                else if (v.equals(spinVals[1]))
                    rateLimit = SWGGuiUtils.statLimits[1] * 1000.0;
                else
                    rateLimit = SWGGuiUtils.statLimits[0] * 1000.0;

                display(currentRes);
            }
        });
        br.add(spin);
        hb.add(br);

        hb.add(Box.createHorizontalStrut(5));

        namedLQ =
                new JCheckBox("LQ ", ((Boolean) SWGFrame.getPrefsKeeper().get(
                        "schemRes4SchemNamedLQ", Boolean.TRUE)).booleanValue());
        namedLQ.setToolTipText(
                "Include LQ schematics for \"named\" resource classes");
        namedLQ.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentRes != null) display(currentRes);
                SWGFrame.getPrefsKeeper().add("schemRes4SchemNamedLQ",
                        Boolean.valueOf(namedLQ.isSelected()));
            }
        });
        hb.add(namedLQ);

        resNameClass = new JLabel("");
        resNameClass.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        hb.add(resNameClass);

        hb.add(Box.createHorizontalGlue());

        rcwLabel = new JLabel("");
        rcwLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        hb.add(rcwLabel);

        return hb;
    }

    /**
     * Helper method that creates and returns the west element of this viewer.
     * This is the component that contains the table of schematics.
     * 
     * @return a GUI component
     */
    private Component makeWest() {
        
        STableModel tm = new STableModel();
        schemTable = new SWGJTable(tm);

        schemTable.setDefaultRenderer(String.class,
                new SWGDecoratedTableCellRenderer(tm));
        schemTable.setDefaultRenderer(Double.class,
                new SWGResourceStatRenderer(tm));

        int w;
        w = SWGGuiUtils.fontWidth(schemTable, "1 000.00", schemTable.getFont());
        SWGGuiUtils.tableSetColumnWidths(schemTable, 1, 999, w, 5);
        SWGGuiUtils.setRowHeight(schemTable);

        schemTable.setAutoCreateRowSorter(false);
        schemTable.getTableHeader().setReorderingAllowed(false);
        schemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        schemTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            int r = schemTable.getSelectedRow();
                            SWGSac s = null;
                            if (r >= 0) {
                                r = schemTable.convertRowIndexToModel(r);
                                s = schematics.get(r);
                            }
                            actionSchemSelected(s);
                        }
                    }
                });
        schemTable.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3)
                    actionSchemMouseRB(e);
            }
        });

        JScrollPane jsp = new JScrollPane(schemTable);
        jsp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        return jsp;
    }

    /**
     * Creates and returns a menu item for this viewer. The action listener
     * makes this viewer visible and populated. For continuous updating, see
     * {@link #updateDisplay(SWGKnownResource, JComponent)}. If the specified
     * resource is {@code null} the dialog is disabled and continuous updating
     * does not work.
     * <p>
     * For the specified resource the viewer lists matching schematics ordered
     * by usefulness. These schematics can use the specified resource and sort
     * sorter is determined by the rate of the resource per schematic, compare
     * {@link SWGSchemController#schematics(SWGKnownResource, double, boolean)}.
     * <p>
     * A client that invokes the listener can continue to update this viewer
     * using {@link #updateDisplay(SWGKnownResource, JComponent)} until this
     * viewer is made invisible or until another client begins to use it.
     * 
     * @param kr a known resource
     * @param c an invoking client
     * @return a menu item
     */
    public static JMenuItem displayMenu(
            final SWGKnownResource kr, final JComponent c) {

        JMenuItem dm = new JMenuItem("Schematics viewer");
        dm.setToolTipText(kr == null
                ? "Display resource quality versus schematics"
                : String.format("Display quality and schematics for %s (%s)",
                        kr.getName(), kr.rc().rcName()));

        dm.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                SWGSchemResViewer.client = c;
                updateDisplay(kr, c);
                THIS.setVisible(true);
            }
        });
        dm.setEnabled(kr != null);
        return dm;
    }

    /**
     * For the specified resource this viewer displays the best matching
     * schematics. The schematics can use the specified resource and they are
     * sorted by the rate of the resource for each schematic, compare
     * {@link SWGSchemController#schematics(SWGKnownResource, double, boolean)}.
     * <p>
     * A client invokes this method for continuous updates as long as this
     * method returns {@code true}. If another client grabs this viewer, or if
     * this viewer is made invisible, this method returns {@code false} and the
     * client should stop further invocations.
     * <p>
     * This method must be invoked only from a user action, on the event thread.
     * 
     * @param kr a known resource
     * @param c an invoking client
     * @return {@code false} if the client must stop invoking this method
     */
    public static boolean updateDisplay(SWGKnownResource kr, JComponent c) {
        if (THIS == null) THIS = new SWGSchemResViewer();
        if (c != client) return false;

        THIS.display(kr);
        return true;
    }

    /**
     * The table model for resources at this viewer.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class RTableModel extends AbstractTableModel
            implements DecoratedTableModel {

        /**
         * The table column header titles.
         */
        private final String[] colNames =
                { "Rate", "Name", "Class", "Inventory" };

        
        @Override
        public TableCellDecorations getCellDecor(
                int row, int column, Object value) {

            SWGSac sac = resources.get(row);
            SWGKnownResource kr = (SWGKnownResource) sac.obj;

            Font f = kr == currentRes
                    ? SWGGuiUtils.fontBold()
                    : SWGGuiUtils.fontPlain();

            String n = (String) sac.object(3); // notes, or null

            if (column == 0) { // rate
                double rate = ((Double) value).doubleValue();
                return new TableCellDecorations(
                        SWGResourceStatRenderer.getStatBackGround(rate),
                        SWGResourceStatRenderer.getStatForeground(rate),
                        n, f);
            }
            Color bg = sac.object(2) != null
                    ? SWGGuiUtils.statColors[0] // denotes inventory
                    : null;
            return new TableCellDecorations(bg, null, n, f);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0
                    ? Double.class
                    : columnIndex == 3
                            ? Long.class
                            : String.class;
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return colNames[col];
        }

        
        @Override
        public int getRowCount() {
            return resources.size();
        }

        
        @Override
        public Object getValueAt(int row, int col) {
            SWGSac sac = resources.get(row);
            if (col == 0) // rate
                return sac.object(1);

            SWGKnownResource kr = (SWGKnownResource) sac.obj;
            if (col == 1) return kr.getName();
            if (col == 2) return kr.rc().rcName();

            return sac.object(2); // inventory
        }
    }

    /**
     * The table model for schematics at this viewer.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class STableModel extends AbstractTableModel
            implements DecoratedTableModel {
        /**
         * The table column header titles.
         */
        private final String[] colNames = { "Schematic", "Rate" };

        
        @Override
        public TableCellDecorations getCellDecor(int row, int column,
                Object value) {

            SWGSac sac = schematics.get(row);
            SWGRCWPair rcw = (SWGRCWPair) sac.object(1);
            Color bg = rcw.filter() == SWGRCWPair.LQ_FILTER
                    ? NAMED_LQ
                    : null;
            String tt = bg != null
                    ? "LQ schematic for \"named\" resource class: " +
                            rcw.rc().rcName()
                    : null;
            if (column == 0) { // name
                SWGResourceClass rc = rcw.rc();
                if (rc != currentRes.rc()) {
                    int c = SWGResourceClassTree.distance(currentRes.rc(), rc);
                    c = 245 - (c * 16);
                    bg = new Color(c, c, 255);
                }
                return new TableCellDecorations(bg, null, tt);
            }

            // else, rate
            if (bg != null)
                return new TableCellDecorations(bg, null, tt);

            double rate = ((Double) value).doubleValue();
            return new TableCellDecorations(
                    SWGResourceStatRenderer.getStatBackGround(rate),
                    SWGResourceStatRenderer.getStatForeground(rate), tt);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0
                    ? String.class
                    : Double.class;
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return colNames[col];
        }

        
        @Override
        public int getRowCount() {
            return schematics.size();
        }

        
        @Override
        public Object getValueAt(int row, int col) {
            SWGSac sac = schematics.get(row);
            return col == 0
                    ? ((SWGSchematic) sac.obj).getName()
                    : (Double) sac.object(0);
        }
    }
}
