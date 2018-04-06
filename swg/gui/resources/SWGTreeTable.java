package swg.gui.resources;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import swg.crafting.Stat;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceClassTree;
import swg.crafting.resources.types.SWGCreatureResources;
import swg.gui.common.AbstractCellEditor;
import swg.gui.common.AbstractTreeTableModel;
import swg.gui.common.JTreeTable;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGDecoratedTableCellRenderer.DecoratedTableModel;
import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJTable;
import swg.gui.common.TreeTableModel;
import swg.gui.common.TreeTableModelAdapter;

/**
 * This type is a tree-table GUI element, see {@link JTreeTable}. This type
 * displays a resource class tree, and the table displays caps for the stats of
 * the visible resource classes.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGTreeTable extends SWGJTable {

    /**
     * A subclass of JTree.
     */
    private TreeTableCellRenderer tree;

    /**
     * Creates an instance of this type for the specified. This constructor
     * creates a tree-table model using {@link SWGResourceClassTree} as the
     * backing root.
     */
    
    SWGTreeTable() {
        super();

        SWGTreeTableModel tm =
                new SWGTreeTableModel(SWGResourceClassTree.getRoot());

        // Create the tree. It will be used as a renderer and editor.
        tree = new TreeTableCellRenderer(tm);

        // Install a tableModel representing the visible rows in the tree.
        super.setModel(new TreeTableModelAdapter(tm, tree));

        // Force the JTable and JTree to share their row selection models.
        ListToTreeSelectionModelWrapper selectionWrapper =
                new ListToTreeSelectionModelWrapper();
        tree.setSelectionModel(selectionWrapper);
        setSelectionModel(selectionWrapper.getListSelectionModel());

        // Install the tree editor renderer and editor.
        setDefaultRenderer(TreeTableModel.class, tree);
        setDefaultRenderer(SWGResourceClass.class, makeCellRenderer());
        setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());

        int w = SWGGuiUtils.fontWidth(this, "1234567890123456789012345678901234567", this.getFont()) + 5;
        SWGGuiUtils.tableSetColumnWidths(this, 0, 0, w, 100);
        w = SWGGuiUtils.fontWidth(this, "1000 1000", this.getFont());
        SWGGuiUtils.tableSetColumnWidths(this, 1, 200, w, 100);
        SWGGuiUtils.setRowHeight(this);
        getTableHeader().setReorderingAllowed(false);

        setGridColor(Color.LIGHT_GRAY);

        // No inter-cell spacing
        setIntercellSpacing(new Dimension(0, 0));

    }

    @Override
    public int getEditingRow() {
        return (editingColumn == 0)
                ? -1
                : editingRow;
    }

    /**
     * Helper method which creates and returns a table cell renderer for the
     * table part of this GUI component.
     * 
     * @return a cell renderer
     */
    private TableCellRenderer makeCellRenderer() {
        SWGDecoratedTableCellRenderer cr = new SWGDecoratedTableCellRenderer() {

            /**
             * A convenience constant array of stats in game order.
             */
            private final Stat[] gOrder = Stat.gameOrder();

            // These members carry information from myValue to paintComponent,
            // this works as long as Java calls for "prepareRenderer" before any
            // painting has begun
            int max;
            int min;
            Color render;
            final Color statBG = SWGGuiUtils.statColors[4];
            final Color one = new Color(255, 179, 179);
            final Color sel = new Color(240, 240, 240);
            boolean select;
            int width;
            
            @Override
            protected void myAlignment(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column,
                    TableCellDecorations decor) {
                // short-cut super-class
            }

            @Override
            protected void myBackground(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column,
                    TableCellDecorations decor) {
                // shortcut super-classes and handle in paintComponent
            }

            @Override
            protected Object myValue(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                SWGResourceClass rc = (SWGResourceClass) value;

                if (rc.isJTL())
                    render = SWGGuiUtils.colorDarker(statBG, .9f);
                else if (rc.isSpawnable()
                        && rc.isSub(SWGCreatureResources.class)
                        && !rc.isHarvested())
                    render = SWGGuiUtils.colorNonHarvested;
                else
                    render = statBG;

                select = isSelected;
                width = table.getColumnModel().getColumn(column).getWidth();

                String v = null;
                Stat s = gOrder[column - 1];
                min = rc.min(s);
                max = rc.max(s);

                if (max > 0) {
                    if (max == min) {
                        if (max < 500)
                            setHorizontalAlignment(SwingConstants.LEADING);
                        else if (max == 500)
                            setHorizontalAlignment(SwingConstants.CENTER);
                        else if (max > 500)
                            setHorizontalAlignment(SwingConstants.TRAILING);

                        return Integer.toString(max);
                    }

                    int x = 1;
                    String vf;
                    do {
                        vf = String.format("%%s %%%ss", Integer.toString(x));
                        v = String.format(vf,
                                Integer.toString(min), Integer.toString(max));
                        ++x;
                    } while (SWGGuiUtils.fontWidth(
                            table, v, getFont()) < width - 10);
                    setHorizontalAlignment(SwingConstants.CENTER);
                }

                return v;
            }

            @Override
            protected void paintComponent(Graphics g) {
                if (g != null) {
                    Graphics tmpG = g.create();

                    // width is set in myValue
                    width -= 1;
                    int h = getHeight() - 1;

                    if (select) {
                        tmpG.setColor(sel);
                        tmpG.fillRect(0, 0, width, h);
                    }

                    if (max > 0) {
                        int x = (int) (min * width / 1000f);
                        int w = ((int) (max * width / 1000f)) - x;
                        x = x >= width
                                ? width - 1
                                : x;
                        w = w <= 0
                                ? 1
                                : w;

                        if (w == 1) {
                            tmpG.setColor(one);
                            tmpG.drawLine(x, 0, x, h - 1);
                        } else {
                            tmpG.setColor(render);
                            tmpG.fillRect(x, 0, w, h);
                        }
                    }
                }
                super.paintComponent(g);
            }
        };

        //cr.setFont(SWGGuiUtils.fontPlain());
        if (SWGGuiUtils.fontMultiplier() > 1.3) {
	        Font f = cr.getFont();
	        int s = Math.max((int) (Math.round(f.getSize() * 1 / 1.3)), 6);
	        f = new Font(f.getName(), f.getStyle(), s);
	        cr.setFont(f);
        }
        setBackground(Color.WHITE);
        return cr;
    }

    @Override
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
        if (tree != null && tree.getRowHeight() != rowHeight)
            tree.setRowHeight(getRowHeight());
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (tree != null)
            tree.updateUI();

        LookAndFeel.installColorsAndFont(this,
                "Tree.background", "Tree.foreground", "Tree.font");
    }

    /**
     * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel to
     * listen for changes in the ListSelectionModel it maintains. Once a change
     * in the ListSelectionModel happens, the paths are updated in the
     * DefaultTreeSelectionModel.
     */
    private final class ListToTreeSelectionModelWrapper extends
            DefaultTreeSelectionModel {

        /** Set to true when we are updating the ListSelectionModel. */
        private boolean updatingListSelectionModel;

        /**
         * Constructor
         */
        private ListToTreeSelectionModelWrapper() {
            super();
            getListSelectionModel().addListSelectionListener(
                    createListSelectionListener());
        }

        /**
         * Creates and returns an instance of ListSelectionHandler.
         * 
         * @return an instance of ListSelectionHandler
         */
        
        private ListSelectionListener createListSelectionListener() {
            return new ListSelectionHandler();
        }

        /**
         * Returns the list selection model. ListToTreeSelectionModelWrapper
         * listens for changes to this model and updates the selected paths
         * accordingly.
         * 
         * @return the list selection model
         */
        private ListSelectionModel getListSelectionModel() {
            return listSelectionModel;
        }

        /**
         * This is overridden to set {@code updatingListSelectionModel} and
         * message super. This is the only place DefaultTreeSelectionModel
         * alters the ListSelectionModel.
         */
        @Override
        public void resetRowSelection() {
            if (!updatingListSelectionModel) {
                updatingListSelectionModel = true;
                try {
                    super.resetRowSelection();
                } finally {
                    updatingListSelectionModel = false;
                }
            }
            // Notice how we don't message super if updatingListSelectionModel
            // is true. If updatingListSelectionModel is true, it implies the
            // ListSelectionModel has already been updated and the paths are the
            // only thing that needs to be updated.
        }

        /**
         * If {@code updatingListSelectionModel} is false, this will reset the
         * selected paths from the selected rows in the list selection model.
         */
        
        private void updateSelectedPathsFromSelectedRows() {
            if (!updatingListSelectionModel) {
                updatingListSelectionModel = true;
                try {
                    // This is way expensive, ListSelectionModel needs an
                    // enumerator for iterating.
                    int min = listSelectionModel.getMinSelectionIndex();
                    int max = listSelectionModel.getMaxSelectionIndex();

                    clearSelection();
                    if (min != -1 && max != -1) {
                        for (int counter = min; counter <= max; counter++) {
                            if (listSelectionModel.isSelectedIndex(counter)) {
                                TreePath selPath = tree.getPathForRow(counter);

                                if (selPath != null)
                                    addSelectionPath(selPath);
                            }
                        }
                    }
                } finally {
                    updatingListSelectionModel = false;
                }
            }
        }

        /**
         * Class responsible for calling updateSelectedPathsFromSelectedRows
         * when the selection of the list changes.
         */
        private final class ListSelectionHandler implements
                ListSelectionListener {

            
            public void valueChanged(ListSelectionEvent e) {
                updateSelectedPathsFromSelectedRows();
            }
        }
    }

    /**
     * The model for the tree-table GUI element.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGTreeTableModel extends AbstractTreeTableModel
            implements DecoratedTableModel {
        
        /**
         * A constant for column names.
         */
        private final Stat[] gOrder = Stat.gameOrder();

        /**
         * Creates an instance of this type.
         * 
         * @param root the root for the tree
         */
        private SWGTreeTableModel(Object root) {
            super(root);
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            /* pass */
        }

        @Override
        public TableCellDecorations getCellDecor(
                int row, int column, Object value) {

            return null;
        }

        @Override
        public Object getChild(Object parent, int index) {
            return SWGResourceClassTree.getChild(
                    (SWGResourceClass) parent, index);
        }

        @Override
        public int getChildCount(Object parent) {
            return SWGResourceClassTree.getChildCount((SWGResourceClass) parent);
        }

        @Override
        public Class<? extends Object> getColumnClass(int column) {
            if (column == 0) return TreeTableModel.class;
            return SWGResourceClass.class;
        }

        @Override
        public int getColumnCount() {
            return 12;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) return "Resource Classes";
            return gOrder[column - 1].name();
        }

        @Override
        public int getRowCount() {
            /* pass */
            return 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            /* pass */
            return null;
        }

        @Override
        public Object getValueAt(Object node, int column) {
            return node;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            /* pass */
            return false;
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return column == 0;
        }

        @Override
        public boolean isLeaf(Object node) {
            return SWGResourceClassTree.isLeaf((SWGResourceClass) node);
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            /* pass */
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            /* pass */
        }
    }

    /**
     * TreeTableCellEditor implementation. Component returned is the JTree.
     */
    private final class TreeTableCellEditor extends AbstractCellEditor
            implements TableCellEditor {

        
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int r, int c) {
            return tree;
        }

        
        @Override
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) e;
                MouseEvent newME = new MouseEvent(tree, me.getID(),
                        me.getWhen(), me.getModifiers(),
                        me.getX() - getCellRect(0, 0, true).x,
                        me.getY(), me.getClickCount(), me.isPopupTrigger());
                tree.dispatchEvent(newME);
            }
            return false;
        }
    }

    /**
     * A TreeCellRenderer that displays a JTree.
     */
    private final class TreeTableCellRenderer extends JTree implements
            TableCellRenderer {

        /**
         * Last table/tree row asked to renderer.
         */
        private int visibleRow;

        /**
         * Constructor
         * 
         * @param model the model
         */
        private TreeTableCellRenderer(TreeModel model) {
            super(model);
            setRootVisible(false);
            setShowsRootHandles(true);
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            if (isSelected)
                setBackground(table.getSelectionBackground());
            else
                setBackground(table.getBackground());

            visibleRow = row;
            return this;
        }

        @Override
        public void paint(Graphics g) {
            g.translate(0, -visibleRow * getRowHeight());
            super.paint(g);
        }

        @Override
        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, 0, w, SWGTreeTable.this.getHeight());
        }

        @Override
        public void setRowHeight(int rowHeight) {
            if (rowHeight > 0) {
                super.setRowHeight(rowHeight);
                if (SWGTreeTable.this != null

                && SWGTreeTable.this.getRowHeight() != rowHeight)
                    SWGTreeTable.this.setRowHeight(getRowHeight());
            }
        }

        @Override
        public void updateUI() {
            super.updateUI();

            TreeCellRenderer tcr = getCellRenderer();

            if (tcr instanceof DefaultTreeCellRenderer) {
                DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);

                dtcr.setBorderSelectionColor(null);
                dtcr.setTextSelectionColor(
                        UIManager.getColor("Table.selectionForeground"));
                dtcr.setBackgroundSelectionColor(
                        UIManager.getColor("Table.selectionBackground"));
            }
        }
    }
}
