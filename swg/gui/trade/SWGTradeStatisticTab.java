package swg.gui.trade;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJTable;
import swg.gui.trade.SWGTradeTab.SWGTradeCustomer;
import swg.gui.trade.SWGTradeTab.SWGTradeVendor;

/**
 * This class is the GUI component for the Statistics tab at the main Trade tab.
 * It contains a few views for different statistics and metrics.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
class SWGTradeStatisticTab extends JPanel {

    /**
     * A list of customer names from the current list of auction mails
     */
    List<String> customerNames = null;

    /**
     * The map of customers from the current list of auction mails
     */
    Map<String, SWGTradeCustomer> customers = null;

    /**
     * The table for the Customer tab of this component
     */
    private SWGJTable customerTable;

    /**
     * The tabbed pane contained at this GUI component
     */
    private JTabbedPane innerTabbedPane;

    /**
     * a map of items derived from the current list of sales mails
     */
    Map<String, SWGTradeTab.SWGTradeItem> itemMap = null;

    /**
     * A list sorted list of names of sold items
     */
    List<String> itemNames = null;

    /**
     * The table for the Items tab of this component
     */
    private SWGJTable itemTable;

    /**
     * The table for the common metrics
     */
    private SWGJTable metricsTable;

    /**
     * A label for the number of days the list of auction mails spans
     */
    private JLabel period;

    /**
     * A label for the date the current auction mails started out
     */
    private JLabel startDate;

    /**
     * The Trade tabbed pane containing this component
     */
    final SWGTradeTab tradeTab;

    /**
     * A map of vendors for the current list of sales mails
     */
    Map<String, SWGTradeVendor> vendorMap = null;

    /**
     * A list of vendor names for the data broken down on vendors
     */
    List<String> vendorNames = null;

    /**
     * The panel for the data broken down on vendors
     */
    private JPanel vendorsPanel;

    /**
     * A table for presenting data broken down on vendors
     */
    private SWGJTable vendorTable;

    /**
     * Creates a GUI component for the statistics panel to be present at the
     * Trade tab of this application
     * 
     * @param tradeTab
     *            the Trade tab containing this component
     */
    public SWGTradeStatisticTab(SWGTradeTab tradeTab) {
        this.tradeTab = tradeTab;

        this.setLayout(new BorderLayout(10, 0));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tradeTab.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });
    }

    /**
     * Raises a dialogue to present the user with some filtering options
     * 
     * @param e
     *            the event causing this action
     */
    protected void customerTableClicked(MouseEvent e) {
        Point pp = e.getPoint();
        int sel = customerTable.rowAtPoint(pp);
        if (sel < 0) return;
        sel = customerTable.convertRowIndexToModel(sel);

        final String name = customerNames.get(sel);

        JPopupMenu pop = new JPopupMenu();
        JMenuItem ch = new JMenuItem("Filter for \"" + name
                + "\" at Sales table");
        ch.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1) {
                customerTableSelect(name);
            }
        });
        pop.add(ch);
        pop.show(customerTable, pp.x, pp.y);
    }

    /**
     * Called when a customer name is clicked
     * 
     * @param name
     *            the name of the customer
     */
    void customerTableSelect(String name) {
        tradeTab.salesTab.filterForBuyer(name);
    }

    /**
     * Called whenever the Trade tabbed pane for this component is de/selected,
     * or when anyone of the tabs at the tabbed pane for this component is
     * de/selected
     * 
     * @param tradeTabHasFocus
     *            <code>true</code> if the Trade tabbed pane has focus or
     *            gained focus, <code>false</code> otherwise
     */
    public void focusGained(boolean tradeTabHasFocus) {
        if (tradeTabHasFocus && tradeTab.getSelectedComponent() == this) {
            if (this.getComponents().length == 0) {
                this.add(makeInnerTabbedPane(), BorderLayout.CENTER);
                this.add(makeRightPane(), BorderLayout.EAST);
            }
            updateCustomerTable();
            updateItemsTable();
            updateMetricsPanel();

            tradeTab.frame.putToLogbar_2("       ");
        }
    }

    /**
     * Returns a table which will display the customers
     * 
     * @return a table which will display the customers
     */
    private Component makeCustomerTable() {
        SWGTradeCustomerTableModel customerTableModel = new SWGTradeCustomerTableModel();
        customerTable = new SWGJTable(customerTableModel);

        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.setAutoCreateRowSorter(true);
        
        customerTable.setDefaultRenderer(Long.class,
                SWGDecoratedTableCellRenderer.newNumberRenderer());


        customerTable.getTableHeader().setReorderingAllowed(false);

        SWGGuiUtils.tableColumnSetWidth(customerTable, 1, 35, 50, 90);
        SWGGuiUtils.tableColumnSetWidth(customerTable, 2, 35, 50, 90);
        SWGGuiUtils.tableColumnSetWidth(customerTable, 3, 55, 85, 130);

        customerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                    customerTableClicked(e);
                }
            }
        });

        return new JScrollPane(customerTable);
    }

    /**
     * Returns a panel for the date and period for the current list of auction
     * mails
     * 
     * @return a panel for the date and period for the current list of auction
     *         mails
     */
    private Box makeDatePeriodPanel() {
        Box b = Box.createHorizontalBox();
        JLabel from = new JLabel(" From: ");
        b.add(from);

        startDate = new JLabel();
        startDate.setBorder(BorderFactory.createEtchedBorder());
        startDate.setOpaque(true);
        startDate.setBackground(Color.WHITE);
        b.add(startDate);

        b.add(new JLabel(" for "));

        period = new JLabel();
        period.setBorder(BorderFactory.createEtchedBorder());
        period.setOpaque(true);
        period.setBackground(Color.WHITE);
        b.add(period);

        return b;
    }

    /**
     * Returns a GUI component for a smaller tabbed pane for different
     * statistics
     * 
     * @return a GUI component for a smaller tabbed pane for different
     *         statistics
     */
    private Component makeInnerTabbedPane() {
        JPanel laftMost = new JPanel();
        laftMost.setLayout(new BoxLayout(laftMost, BoxLayout.X_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        innerTabbedPane = new JTabbedPane();
        innerTabbedPane.add("Customers", makeCustomerTable());
        innerTabbedPane.add("Items", makeItemsTable());

        laftMost.add(innerTabbedPane);

        return laftMost;
    }

    /**
     * Returns a table which will display sold items
     * 
     * @return a table which will display sold items
     */
    private Component makeItemsTable() {
        SWGTradeItemTableModel customerTableModel = new SWGTradeItemTableModel();
        itemTable = new SWGJTable(customerTableModel);

        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.setAutoCreateRowSorter(true);

        itemTable.setDefaultRenderer(String.class,
                new SWGTradeTab.ItemNameRenderer());
        itemTable.setDefaultRenderer(Long.class,
                SWGDecoratedTableCellRenderer.newNumberRenderer());

        itemTable.getTableHeader().setReorderingAllowed(false);

        SWGGuiUtils.tableColumnSetWidth(itemTable, 1, 35, 50, 65);
        SWGGuiUtils.tableColumnSetWidth(itemTable, 2, 55, 85, 130);

        return new JScrollPane(itemTable);
    }

    /**
     * Returns a panel for the basic metrics stuff
     * 
     * @return a panel for the basic metrics stuff
     */
    private JPanel makeMetricsPanel() {
        JPanel metrics = new JPanel(new BorderLayout(0, 10));
        metrics.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Metrics"));
        metrics.add(makeMetricsView(), BorderLayout.CENTER);
        Box b = makeDatePeriodPanel();
        metrics.add(b, BorderLayout.PAGE_END);
        return metrics;
    }

    /**
     * Returns a view with some common metrics
     * 
     * @return a view with some common metrics
     */
    private Component makeMetricsView() {
        JPanel metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        metricsPanel.setPreferredSize(new Dimension(250, 6 * 17));

        metricsTable = new SWGJTable(new SWGTradeMetricsTableModel());
        metricsTable.getTableHeader().setReorderingAllowed(false);
        SWGGuiUtils.tableColumnSetWidth(metricsTable, 1, 55, 85, 130);

        metricsTable.setDefaultRenderer(Long.class,
                SWGDecoratedTableCellRenderer.newNumberRenderer());

        metricsPanel
                .add(metricsTable.getTableHeader(), BorderLayout.PAGE_START);
        metricsPanel.add(metricsTable, BorderLayout.CENTER);

        return metricsPanel;
    }

    /**
     * Returns a GUI component for common statistics
     * 
     * @return a GUI component for common statistics
     */
    private Component makeRightPane() {
        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout(0, 10));

        JPanel metrics = makeMetricsPanel();
        JPanel mb = new JPanel(new BorderLayout());

        mb.add(Box.createRigidArea(new Dimension(200, 15)),
                BorderLayout.PAGE_START);
        mb.add(metrics, BorderLayout.CENTER);
        bottom.add(mb, BorderLayout.PAGE_START);

        JPanel vendors = makeVendorsPanel();
        bottom.add(vendors, BorderLayout.CENTER);

        return bottom;
    }

    /**
     * Returns a panel for data broken down on vendors
     * 
     * @return a panel for data broken down on vendors
     */
    private JPanel makeVendorsPanel() {
        JPanel vp = new JPanel();
        vp.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Vendors"));

        vp.add(makeVendorsTable());
        return vp;
    }

    /**
     * Returns a table for the vendor panel
     * 
     * @return a table for the vendor panel
     */
    private Component makeVendorsTable() {
        vendorsPanel = new JPanel(new BorderLayout());
        vendorsPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        vendorTable = new SWGJTable(new SWGTradeVendorTableModel());

        vendorTable.setDefaultRenderer(Long.class,
                SWGDecoratedTableCellRenderer.newNumberRenderer());

        vendorTable.getTableHeader().setReorderingAllowed(false);
        SWGGuiUtils.tableColumnSetWidth(vendorTable, 1, 50, 65, 80);
        SWGGuiUtils.tableColumnSetWidth(vendorTable, 2, 50, 85, 130);

        vendorsPanel.add(vendorTable.getTableHeader(), BorderLayout.PAGE_START);
        vendorsPanel.add(vendorTable, BorderLayout.CENTER);

        vendorTable = new SWGJTable();
        return vendorsPanel;
    }

    /**
     * Updates the data for the Customers tab on this component
     */
    private void updateCustomerTable() {
        customers = tradeTab.getCustomers();
        customerNames = tradeTab.getCustomerNames();
        ((AbstractTableModel) customerTable.getModel()).fireTableDataChanged();
    }

    /**
     * Updates the data for the Items tab on this component
     */
    private void updateItemsTable() {
        itemMap = tradeTab.getItemMap();
        itemNames = tradeTab.getItemNames();
        ((AbstractTableModel) itemTable.getModel()).fireTableDataChanged();
    }

    /**
     * Updates the data on the common metrics panel
     */
    private void updateMetricsPanel() {
        ((AbstractTableModel) metricsTable.getModel()).fireTableDataChanged();

        vendorMap = tradeTab.getVendorMap();
        vendorNames = new ArrayList<String>(vendorMap.keySet());
        Collections.sort(vendorNames);
        Dimension vpd = new Dimension(250, (vendorNames.size() + 1) * 17);
        vendorsPanel.setPreferredSize(vpd);
        vendorsPanel.invalidate();
        ((AbstractTableModel) vendorTable.getModel()).fireTableDataChanged();

        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        startDate.setText(' ' + df.format(tradeTab.getStartDate()) + ' ');
        period.setText(tradeTab.getDaysSpanned());
    }

    /**
     * The underlying model for the table of sales mails
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGTradeCustomerTableModel extends AbstractTableModel {

        /**
         * Table column header titles
         */
        private String[] columnNames = {"Customer", "Visits", "Buys", "Credits"};

        /**
         * Creates a model of this type.
         */
        SWGTradeCustomerTableModel() {
            super();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex == 0) return String.class;
            return Long.class;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public int getRowCount() {
            if (customers == null) return 0;
            return customers.size();
        }

        public Object getValueAt(int row, int column) {
            if (customers == null) return "";
            String name = customerNames.get(row);
            if (column == 0) return name;

            SWGTradeCustomer cust = customers.get(name);
            switch (column) {
                case 1:
                    return Long.valueOf(cust.visits);
                case 2:
                    return Long.valueOf(cust.itemCount);
                case 3:
                    return Long.valueOf(cust.sum);
                default:
                    return "ERROR";
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /**
     * The underlying model for the items table
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGTradeItemTableModel extends AbstractTableModel {

        /**
         * Table column header titles
         */
        private String[] columnNames = { "Item", "Count", "Income" };

        /**
         * Creates a model of this type.
         */
        SWGTradeItemTableModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return String.class;
            return Long.class;
        }


        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public int getRowCount() {
            if (itemNames == null) return 0;
            return itemNames.size();
        }

        public Object getValueAt(int row, int column) {
            String name = itemNames.get(row);
            if (column == 0) return name;

            SWGTradeTab.SWGTradeItem item = itemMap.get(name);
            return Long.valueOf(column == 1
                    ? item.numbersSold
                    : item.income);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /**
     * The underlying model for the metrics view
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGTradeMetricsTableModel extends AbstractTableModel {

        /**
         * Table column header titles
         */
        private String[] columnNames = { "Entity", "Total" };

        /**
         * The fix number of rows this table has
         */
        private final int ROW_COUNT = 5;

        /**
         * Creates model of this type.
         */
        SWGTradeMetricsTableModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0
                    ? String.class
                    : Long.class;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public int getRowCount() {
            return ROW_COUNT;
        }

        public Object getValueAt(int row, int column) {
            if (column == 0) {
                switch (row) {
                case 0:
                    return "Credits Earned";
                case 1:
                    return "Own purchases";
                case 2:
                    return "Net cash";
                case 3:
                    return "Unique Items";
                case 4:
                    return "Unique Customers";
                default:
                    return "ERROR";
                }
            } else if (column == 1) {
                switch (row) {
                case 0:
                    return Long.valueOf(tradeTab.getSalesSum());
                case 1:
                    return Long.valueOf(tradeTab.getPurchasePriceSum());
                case 2:
                    return Long.valueOf((tradeTab.getSalesSum() - tradeTab
                                .getPurchasePriceSum()));
                case 3:
                    return Long.valueOf(tradeTab.getItemCount());
                case 4:
                    return Long.valueOf(customerNames.size());
                default:
                    return "ERROR";
                }
            }
            return "ERROR";
        }
    }

    /**
     * The underlying model for the data broken down on vendors
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGTradeVendorTableModel extends AbstractTableModel {

        /**
         * Table column header titles
         */
        private String[] columnNames = { "Vendor", "Items Sold", "Income" };

        /**
         * Creates a model of this type.
         */
        SWGTradeVendorTableModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0
                    ? String.class
                    : Long.class;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public int getRowCount() {
            if (vendorMap == null) return 0;
            return vendorMap.size();
        }

        public Object getValueAt(int row, int column) {
            String name = vendorNames.get(row);
            if (column == 0) return name;

            SWGTradeVendor vendor = vendorMap.get(name);
            return Long.valueOf(column == 1
                    ? vendor.itemsSold
                    : vendor.income);
        }
    }
}
