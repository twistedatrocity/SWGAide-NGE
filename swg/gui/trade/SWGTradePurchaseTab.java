package swg.gui.trade;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJTable;
import swg.model.mail.SWGAuctionerData;
import swg.tools.ZNumber;

/**
 * This class is the GUI component for the Purchased tab at the main Trade tab.
 * It contains a plain table to present the auction mails.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
class SWGTradePurchaseTab extends JPanel {

    /**
     * The panel for the sum of the purchases, for repainting reasons
     */
    private JPanel pricePanel;

    /**
     * The field presenting the sum of purchases
     */
    private JTextField priceSumField;

    /**
     * The list of purchase mails
     */
    List<SWGAuctionerData> purchaseMails;

    /**
     * The table presenting the purchase mails
     */
    private SWGJTable purchaseTable;

    /**
     * The table model supporting the table of mails
     */
    private SWGTradePurchaseTableModel purchaseTableModel;

    /**
     * The Trade tabbed pane containing this component
     */
    final SWGTradeTab tradeTab;

    /**
     * Creates a GUI component for the purchase mails to be present at the Trade
     * tab of this application
     * 
     * @param tradeTab
     *            the Trade tab containing this component
     */
    public SWGTradePurchaseTab(SWGTradeTab tradeTab) {
        this.tradeTab = tradeTab;

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        // create interior lazily
        tradeTab.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });
    }

    /**
     * Called whenever the tabbed pane for this component is de/selected, or
     * when anyone of the tabs at the tabbed pane for this component is
     * de/selected
     * 
     * @param hasFocus
     *            <code>true</code> if the main tabbed pane has focus or
     *            gained focus, <code>false</code> otherwise
     */
    public void focusGained(boolean hasFocus) {
        if (hasFocus && tradeTab.getSelectedComponent() == this) {
            if (this.getComponents().length == 0) {
                this.add(new JScrollPane(makeTable(),
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                        BorderLayout.CENTER);
                this.add(makeSumPanel(), BorderLayout.PAGE_END);
            }
            purchaseMails = tradeTab.getPurchaseMails();
            ((AbstractTableModel) purchaseTable.getModel())
                    .fireTableDataChanged();
            priceSum();
            tradeTab.frame.putToLogbar_2(purchaseMails.size() + " mails   ");
        }
    }

    /**
     * Returns a component for presenting the sum of all purchased items
     * 
     * @return a GUI component for presenting the sum of all purchased items
     */
    private Component makeSumPanel() {
        pricePanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));

        JLabel lab = new JLabel("Sum: ");
        pricePanel.add(lab);

        priceSumField = new JTextField(8);
        priceSumField.setHorizontalAlignment(SwingConstants.TRAILING);
        priceSumField.setEditable(false);
        priceSumField.setBackground(Color.WHITE);
        pricePanel.add(priceSumField);

        pricePanel.add(Box.createHorizontalStrut(200));

        return pricePanel;
    }

    /**
     * Returns a purchase mails table which will display the purchase mail
     * 
     * @return a table which will display the purchase mail
     */
    private Component makeTable() {
        purchaseTableModel = new SWGTradePurchaseTableModel();
        purchaseTable = new SWGJTable(purchaseTableModel);

        purchaseTable.setDefaultRenderer(Date.class,
                new SWGTradeTab.DateRenderer());
        purchaseTable.setDefaultRenderer(Long.class,
                SWGDecoratedTableCellRenderer.newNumberRenderer());

        purchaseTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        purchaseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        purchaseTable.setAutoCreateRowSorter(true);

        purchaseTable.getTableHeader().setReorderingAllowed(false);

        SWGGuiUtils.tableColumnSetWidth(purchaseTable, 0, 100, 115, 130);
        SWGGuiUtils.tableColumnSetWidth(purchaseTable, 3, 55, 70, 85);
        SWGGuiUtils.tableColumnSetWidth(purchaseTable, 5, 30, 55, 80);

        return purchaseTable;
    }

    /**
     * Presents the sum of all purchased items at the GUI
     */
    private void priceSum() {
        priceSumField.setText(ZNumber.asText(
                tradeTab.getPurchasePriceSum(), true, true));
    }

    @Override
    public void repaint() {
        if (pricePanel != null) {
            pricePanel.remove(2);
            int w = purchaseTable.getColumnModel().getColumn(4).getWidth();
            pricePanel.add(Box.createHorizontalStrut(w + 7 + 55));
        }
        super.repaint();
    }

    /**
     * The underlying model for the table of purchase mails
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGTradePurchaseTableModel extends AbstractTableModel {

        /**
         * Table column header titles
         */
        private String[] columnNames = { "Date", "Seller", "Item", "Price",
                "Location", "Type" };

        /**
         * Creates an instance of this type
         */
        SWGTradePurchaseTableModel() {
            super();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class getColumnClass(int columnIndex) {
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

        public int getRowCount() {
            if (purchaseMails == null) return 0;
            return purchaseMails.size();
        }

        public Object getValueAt(int row, int column) {
            if (purchaseMails == null) return "";

            // shouldn't happen
            if (row < 0 || row >= purchaseMails.size())
                return Integer.toString(row);

            SWGAuctionerData auct = purchaseMails.get(row);
            switch (column) {
            case 0:
                return new Date(auct.date() * 1000);
            case 1:
                return auct.other();
            case 2:
                return auct.item();
            case 3:
                return Long.valueOf(auct.price());
            case 4:
                return auct.location();
            case 5:
                return auct.type().vendor;
            }
            return "ERROR";
        }
    }
}
