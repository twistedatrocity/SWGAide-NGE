package swg.gui.trade;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJTable;
import swg.model.mail.SWGAuctionerData;
import swg.tools.ZNumber;
import swg.tools.ZString;
import swg.tools.ZStuff;

/**
 * This is the first component at the at the Trade tab, the Sales panel. It
 * displays a table of sales at auction and vendors and some features for
 * filtering.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
class SWGTradePurchaseTab extends JPanel implements TableColumnModelListener {

    /**
     * A button that resets all filters.
     */
    private JButton clearButton;

    /**
     * A filter field for the buyers column.
     */
    private JTextField filterSellers;

    /**
     * A filter field for the dates column.
     */
    private JTextField filterDate;

    /**
     * A filtered list of auction data for purchase transactions.
     */
    private List<SWGAuctionerData> filteredPurchaseMails;

    /**
     * A filter field for the items column.
     */
    private JTextField filterItems;

    /**
     * A filter field for the prices column.
     */
    private JTextField filterPrices;

    /**
     * A filter field for the location column.
     */
    private JTextField filterLocations;

    /**
     * A flag that denotes if this component is fully created, or yet a stub.
     */
    private boolean isGuiFinished;

    /**
     * A flag that denotes if the GUI is updating itself and some action should
     * not be allowed.
     */
    private boolean isWorking;

    /**
     * The table model that supports the purchase table.
     */
    private PurchaseModel purchaseModel;

    /**
     * The main table that displays purchase transactions.
     */
    private SWGJTable purchaseTable;

    /**
     * The parent component for this instance, that is the Trade tabbed pane.
     */
    private final SWGTradeTab tradeTab;

    /**
     * Creates an instance of this type. This constructor lazily creates a stub
     * and the first time the user visits this panel creation is finished.
     * 
     * @param tradeTab the Trade tabbed pane that contains this component
     */
    SWGTradePurchaseTab(SWGTradeTab tradeTab) {
        this.tradeTab = tradeTab;

        tradeTab.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });
    }

    public void columnAdded(TableColumnModelEvent e) {
        /* ignore */
    }

    public void columnMarginChanged(ChangeEvent e) {
        resizeFilters();
    }

    public void columnMoved(TableColumnModelEvent e) {
        resizeFilters();
    }

    public void columnRemoved(TableColumnModelEvent e) {
        /* ignore */
    }

    public void columnSelectionChanged(ListSelectionEvent e) {
        /* ignore */
    }

    /**
     * Sets the specified seller to the {@link #filterSellers} field. This method
     * first resets all filter fields and finally triggers filtering.
     * 
     * @param buyer the name of a buyer
     */
    void filterForSeller(String seller) {
        resetGUI();
        filterSellers.setText(seller);
    }

    /**
     * Assembles filters from the text in the different filter text fields and
     * applies them. Empty fields turn out as regular expressions accepting any
     * input.
     */
    private void filterMailList() {
        if (isWorking) return;
        String input = filterSellers.getText();
        Pattern patternSeller = filterPattern(input);

        input = filterItems.getText();
        Pattern patternItem = filterPattern(input);

        input = filterLocations.getText();
        Pattern patternLocations = filterPattern(input);

        input = filterPrices.getText().trim();
        int highPrice = Integer.MAX_VALUE;
        int lowPrice = 0;
        int dash = input.indexOf("-");
        if (input.startsWith("<")) { // less than
            highPrice = parseInteger(input, highPrice);
        } else if (input.startsWith(">")) { // greater than
            lowPrice = parseInteger(input, lowPrice);
        } else if (dash > 0) {// between
            lowPrice = parseInteger(input.substring(0, dash), lowPrice);
            highPrice = parseInteger(input.substring(dash + 1), highPrice);
        } else if (!input.isEmpty()) { // exact price
            int r = parseInteger(input);
            if (r >= 0) {
                lowPrice = highPrice = r;
            } // else parse error
        } // else free price
        
        List<SWGAuctionerData> purchaseMails = tradeTab.getPurchaseMails();
        List<SWGAuctionerData> tmp = new ArrayList<SWGAuctionerData>(
                purchaseMails.size());
        for (SWGAuctionerData au : purchaseMails) {
            Matcher ms = patternSeller.matcher(au.other());
            Matcher mi = patternItem.matcher(au.item());
            Matcher mv = patternLocations.matcher(au.location());
            long p = au.price();
            if (ms.find() && mi.find() && mv.find()
                    && (lowPrice <= p && p <= highPrice)) { // match ALL filters
                tmp.add(au);
            }
        }
        filteredPurchaseMails = tmp;
        purchaseModel.fireTableDataChanged();
        purchaseTable.doLayout();
        resizeFilters();

        tradeTab.frame.putToLogbar_2(filteredPurchaseMails.size() + " mails   ");
    }

    /**
     * Helper method which creates and returns a pattern for the specified
     * input. If there is an error this method returns a patterns for the empty
     * string, i.e. it matched anything. The pattern is compiled using the
     * {@link Pattern#LITERAL} and {@link Pattern#CASE_INSENSITIVE} flags.
     * 
     * @param input a string
     * @return an RE pattern
     */
    private Pattern filterPattern(String input) {
        try {
            return Pattern.compile(input, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            return Pattern.compile("");
        }
    }

    /**
     * Called whenever the Trade tabbed pane for this component is de/selected,
     * or when anyone of the tabs at the tabbed pane for this component is
     * de/selected
     * 
     * @param tradeTabHasFocus <code>true</code> if the Trade tabbed pane has
     *        focus or gained focus, <code>false</code> otherwise
     */
    void focusGained(boolean tradeTabHasFocus) {
        if (tradeTabHasFocus && tradeTab.getSelectedComponent() == this) {
            if (!isGuiFinished) make();

            filterMailList();

            tradeTab.frame.putToLogbar_2(filteredPurchaseMails.size()
                    + " mails   ");
        }
    }

    /**
     * Helper method which lazily finishes creation of this type. If GUI is
     * already finished this method does nothing.
     */
    private synchronized void make() {
        if (isGuiFinished) return;

        setLayout(new BorderLayout());

        purchaseTable = makeTable();
        add(new JScrollPane(purchaseTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER);
        add(makeFilters(), BorderLayout.PAGE_END);

        isGuiFinished = true;
    }

    /**
     * Helper method which creates and returns a text input field for filtering.
     * This method creates the component with the specified tool-tip, adds an
     * action listener, and returns the instance.
     * 
     * @param tt a tool-tip text
     * @return a text input field
     */
    private JTextField makeFilter(String tt) {
        JTextField tf = new JTextField();
        tf.setToolTipText(tt);
        tf.setBorder(BorderFactory.createLoweredBevelBorder());
        tf.getDocument().addDocumentListener(new DocumentListener() {
            @SuppressWarnings("synthetic-access")
            public void changedUpdate(DocumentEvent e) {
                filterMailList();
            }

            @SuppressWarnings("synthetic-access")
            public void insertUpdate(DocumentEvent e) {
                filterMailList();
            }

            @SuppressWarnings("synthetic-access")
            public void removeUpdate(DocumentEvent e) {
                filterMailList();
            }
        });
        return tf;
    }

    /**
     * Helper method which creates and returns a component with the filter input
     * fields and the "Clear" button.
     * 
     * @return a GUI component
     */
    private Component makeFilters() {
        Box b = Box.createHorizontalBox();

        // TODO: Filter on date span
        filterDate = makeFilter("Filter on dates - TODO");
        filterDate.setEnabled(false);
        filterDate.setOpaque(false);
        b.add(filterDate);

        // TODO: make input fields filter as you type

        filterSellers = makeFilter("Filter on seller name");
        b.add(filterSellers);

        filterItems = makeFilter("Filter on item");
        b.add(filterItems);

        filterPrices = makeFilter("Filter on price [interval]");
        b.add(filterPrices);

        filterLocations = makeFilter("Filter on location name");
        b.add(filterLocations);

        clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear all filter text boxes");
        clearButton.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                resetGUI();
                filterMailList();
            }
        });
        clearButton.setMnemonic('C');
        b.add(clearButton);

        return b;
    }

    /**
     * Helper method which creates and returns the main table of purchase
     * transactions. This method also instantiates {@link #purchaseModel} for the
     * returned table.
     * 
     * @return a GUI component
     */
    @SuppressWarnings("synthetic-access")
    private SWGJTable makeTable() {
        purchaseModel = new PurchaseModel();
        SWGJTable st = new SWGJTable(purchaseModel);

        st.setDefaultRenderer(Long.class,
                SWGDecoratedTableCellRenderer.newNumberRenderer());
        SWGDecoratedTableCellRenderer cr = new SWGDecoratedTableCellRenderer() {
            private final String nfm =
                    "<html><font color=\"blue\">%s</font></html>";

            @Override
            protected void myAlignment(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column,
                    TableCellDecorations decor) {

                if (value == null) return;
                setHorizontalAlignment(column == 3
                        ? SwingConstants.TRAILING
                        : SwingConstants.LEADING);
            }

            @Override
            protected Object myValue(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                if (value == null) return null;
                if (column == 0)
                    return ZStuff.dateTimeString(((Long) value).longValue());
                if (column == 2 && ((String) value).endsWith(": ¤"))
                    return ZString.fs(nfm, value);
                if (column == 3)
                    return ZNumber.asText((Long) value);

                return value;
            }
        };
        st.getColumnModel().getColumn(0).setCellRenderer(cr);
        st.setDefaultRenderer(String.class, cr);

        st.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        st.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        st.setAutoCreateRowSorter(true);

        st.getTableHeader().setReorderingAllowed(false);

        SWGGuiUtils.tableColumnSetWidth(st, 0, 100, 115, 150);
        SWGGuiUtils.tableColumnSetWidth(st, 1, 100, 115, 200);
        SWGGuiUtils.tableColumnSetWidth(st, 3, 55, 80, 115);
        SWGGuiUtils.tableColumnSetWidth(st, 4, 100, 115, 500);
        SWGGuiUtils.tableColumnSetWidth(st, 5, 100, 200, 500);

        st.getColumnModel().addColumnModelListener(this);
        st.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (isWorking || e.getValueIsAdjusting()) return;
                        int col = purchaseTable.getSelectedColumn();
                        int row = purchaseTable.getSelectedRow();
                        if (row < 0 || col < 1 || col > 4) return;

                        row = purchaseTable.convertRowIndexToModel(row);
                        Object obj = purchaseModel.getValueAt(row, col);
                        if (col == 1)
                            filterSellers.setText(obj.toString());
                        else if (col == 2)
                            filterItems.setText(obj.toString());
                        else if (col == 3)
                            filterPrices.setText(obj.toString());
                        else if (col == 4)
                            filterLocations.setText(obj.toString());

                        filterMailList();
                    }
                });

        return st;
    }

    /**
     * Helper method which returns an integer parsed from the specified string,
     * or {@code -1} if the input is not parsable.
     * 
     * @param value a string
     * @return an integer, or -1
     */
    private int parseInteger(String value) {
        String in = value.trim().replace(",", "").replace(".", "");
        try {
            return Integer.parseInt(in);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Helper method which returns an integer parsed from the specified string,
     * or {@code def} if the input is not parsable. This method is used for
     * string which begins with &lt; or &gt; and begins parsing at index 1.
     * 
     * @param value a string
     * @param def a default integer
     * @return an integer, or the default value
     */
    private int parseInteger(String value, int def) {
        String in = value.substring(1).trim();
        int r = parseInteger(in);
        return r >= 0
                ? r
                : def;
    }

    /**
     * This method resets filter input fields from any text, no further action.
     */
    void resetGUI() {
        isWorking = true;
        filterSellers.setText(null);
        filterItems.setText(null);
        filterPrices.setText(null);
        filterLocations.setText(null);
        purchaseTable.getRowSorter().setSortKeys(null);
        isWorking = false;
    }

    /**
     * Helper method which resizes the specified component to the width of the
     * specified column.
     * 
     * @param comp a component to resize
     * @param col a table column
     */
    private void resizeComp(Component comp, TableColumn col) {
        Dimension d = new Dimension(col.getWidth(), 27);
        comp.setMinimumSize(d);
        comp.setPreferredSize(d);
        comp.setMaximumSize(d);
    }

    /**
     * Helper method which aligns the width of filter input fields with the
     * corresponding columns.
     */
    private void resizeFilters() {
        if (isGuiFinished && !isWorking)
            SwingUtilities.invokeLater(new Runnable() {
                @SuppressWarnings("synthetic-access")
                public void run() {
                    TableColumnModel tm = purchaseTable.getColumnModel();
                    resizeComp(filterDate, tm.getColumn(0));
                    resizeComp(filterSellers, tm.getColumn(1));
                    resizeComp(filterItems, tm.getColumn(2));
                    resizeComp(filterPrices, tm.getColumn(3));
                    resizeComp(filterLocations, tm.getColumn(4));
                    resizeComp(clearButton, tm.getColumn(5));
                    clearButton.invalidate();
                    tradeTab.repaint();
                }
            });
    }

    /**
     * The model for the table of purchase transactions.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class PurchaseModel extends AbstractTableModel {

        /**
         * Table column header titles
         */
        private String[] columnNames = { "Date", "Seller", "Item", "Price",
                "Location", "Type" };

        /**
         * Creates a model of this type.
         */
        private PurchaseModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Date.class;
            if (columnIndex == 3) return Long.class;
            return String.class;
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
            return filteredPurchaseMails != null
                    ? filteredPurchaseMails.size()
                    : 0;
        }

        @SuppressWarnings("synthetic-access")
        public Object getValueAt(int row, int column) {
            if (filteredPurchaseMails == null) return "";
            SWGAuctionerData auct = filteredPurchaseMails.get(row);
            switch (column) {
            case 0:
                return Long.valueOf(auct.date() * 1000);
            case 1:
                return auct.other();
            case 2:
                return tradeTab.getItemName(auct);
            case 3:
                return Long.valueOf(auct.price());
            case 4:
                return auct.location();
            case 5:
                return auct.vendor();
            default:
                return "ERROR";
            }
        }
    }
}
