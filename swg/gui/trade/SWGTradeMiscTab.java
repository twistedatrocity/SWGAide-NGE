package swg.gui.trade;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGDoTask;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGJTable;
import swg.model.SWGCharacter;
import swg.tools.ZNumber;
import swg.tools.ZWriter;

/**
 * This class contains some miscellaneous stuff for the trade tabbed pane
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
class SWGTradeMiscTab extends JLabel {

    /**
     * The Delete menu item at the Edit menu
     */
    private JMenuItem deleteMenuItem = null;

    /**
     * The URL for the help page for these objects
     */
    protected URL helpPage;

    /**
     * The table for mappings from price to name
     */
    private SWGJTable mappingTable;

    /**
     * The underlying model for the price to name mapping table
     */
    private SWGTradePriceMappingTableModel mappingTableModel;

    /**
     * An ugly helper variable for entering a new pair of data to the price2name
     * table
     */
    Integer newValue = null;

    /**
     * The check box for map always, that is, mappings are always applied with
     * this option selected (not default) and the items real names are
     * overwritten
     */
    JCheckBox overRideCheckBox;

    /**
     * The Save menu item for the applications File menu
     */
    private ActionListener saveListener;

    /**
     * The check box for shared mapping, that is, mappings are shared over
     * characters with this option selected (default)
     */
    private JCheckBox sharedCheckBox;

    /**
     * The character currently in focus, or maybe the previous character while a
     * focus shift
     */
    private SWGCharacter toon;

    /**
     * The tabbed pan containing this component
     */
    final SWGTradeTab tradeTab;

    /**
     * Creates an instance of this component and populates it
     * 
     * @param tradeTab
     *            the component embracing this component
     */
    SWGTradeMiscTab(SWGTradeTab tradeTab) {
        this.tradeTab = tradeTab;

        setLayout(new BorderLayout());

        helpPage = SWGAide.class.getResource("docs/help_trade_misc_en.html");

        tradeTab.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });

        tradeTab.frame.addExitCallback(new SWGDoTask(
                new SWGDoTask.TaskCallback() {

                    public void execute() {
                        saveToFile();
                    }
                }));
    }

    /**
     * Returns a menu item for this component
     * 
     * @return a menu item for this component
     */
    private JMenuItem deleteMenuItem() {
        deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.setMnemonic('D');
        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
        deleteMenuItem.setToolTipText("Delete selected price/name pair");
        deleteMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deletePair();
            }
        });
        return deleteMenuItem;
    }

    /**
     * Deletes a user selected row from the table after a confirmation dialog
     */
    void deletePair() {
        int row = mappingTable.getSelectedRow();
        if (row < 0 || row >= tradeTab.priceToNameMapping.size()) return;
        Integer key = tradeTab.getPriceMappingKeys().get(row);
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                mappingTable, "\"" + key.toString() + "\"/ \" "
                        + tradeTab.priceToNameMapping.get(key)
                        + "\"\nDo you want to delete this mapping?",
                "Confirm deletion", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE)) {
            tradeTab.priceToNameMapping.remove(key);
            mappingTableModel.fireTableRowsDeleted(row, row);
            saveToFile();
        }
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
            if (deleteMenuItem == null) {
                this.add(makeTopPanel(), BorderLayout.PAGE_START);
                this.add(makeMappingTable(), BorderLayout.WEST);
                this.add(makeEastPanel(), BorderLayout.CENTER);
                // this.add(makeBottomPanel(), BorderLayout.PAGE_END);
                deleteMenuItem();
                saveListener();
            }

            tradeTab.frame.editMenuAdd(deleteMenuItem);
            tradeTab.frame.saveAddListener(saveListener,
                    "Save price/item-name mapping");

            SWGHelp.push(helpPage);

            if (tradeTab.character.equals(toon)) {
                updateLogBar();
                return;
            }
            toon = tradeTab.character;

            String key = tradeTab.getPrefsKeyForMiscTab();
            Boolean b = (Boolean) SWGFrame.getPrefsKeeper().get(key);
            sharedCheckBox.setSelected(b != null ? b.booleanValue() : true);

            Boolean o = (Boolean) SWGFrame.getPrefsKeeper().get(
                    "tradeMiscOverride");
            overRideCheckBox.setSelected(o != null ? o.booleanValue() : false);

            tradeTab.resetMappingsMap();
            mappingTable.invalidate();
            mappingTable.repaint();

            updateLogBar();
        } else if (deleteMenuItem != null) {
            saveToFile();
            tradeTab.frame.editMenuRemove(deleteMenuItem);
            tradeTab.frame.saveRemoveListener(saveListener);

            SWGHelp.remove(helpPage);
        }
    }

    /**
     * Handles de/selections of the Global/Share mapping check box. That is, the
     * mappings file will be saved globally or individually depending on the
     * state of the check box.
     */
    protected void handleSharedSelection() {
        if (toon == null) return;
        boolean b = sharedCheckBox.isSelected();
        String key = tradeTab.getPrefsKeyForMiscTab();

        Boolean globalPrev = (Boolean) SWGFrame.getPrefsKeeper().get(key);
        boolean gp = globalPrev == null ? false : globalPrev.booleanValue();
        if (!gp && b) {
            // a local -> global transition
            // add current map to global map not to overwrite the latter
            Map<Integer, String> currentMap = tradeTab.priceToNameMapping;
            tradeTab.resetMappingsMap();
            tradeTab.priceToNameMapping.putAll(currentMap);
            mappingTableModel.fireTableDataChanged();
        }

        SWGFrame.getPrefsKeeper().add(key, Boolean.valueOf(b));
    }

    /**
     * Creates and returns a panel for the eastern part of this component
     * 
     * @return the eastern component
     */
    private Component makeEastPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel panel2 = new JPanel(new GridLayout(2, 1));
        panel.add(panel2);

        sharedCheckBox = new JCheckBox("Shared mapping");
        sharedCheckBox.setToolTipText("Individual or shared mappings");
        sharedCheckBox.setSelected(true);
        sharedCheckBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                handleSharedSelection();
            }
        });
        panel2.add(sharedCheckBox);

        overRideCheckBox = new JCheckBox("Map always");
        overRideCheckBox
                .setToolTipText("Always map, override existing item names");
        overRideCheckBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("tradeMiscOverride",
                        new Boolean(overRideCheckBox.isSelected()));
            }
        });
        panel2.add(overRideCheckBox);

        return panel;
    }

    /**
     * Returns a GUI component for the price to name mapping table
     * 
     * @return a GUI component for the price to name mapping table
     */
    private Component makeMappingTable() {
        mappingTableModel = new SWGTradePriceMappingTableModel();
        mappingTable = new SWGJTable(mappingTableModel);

        mappingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mappingTable.getTableHeader().setReorderingAllowed(false);
        // mappingTable.setAutoCreateRowSorter(true);

        mappingTable.setDefaultRenderer(Integer.class, 
                new SWGDecoratedTableCellRenderer() {
                    @Override
                    protected void mySetValue(Object value) {
                        double d = ((Integer)value).doubleValue();
                        setText(ZNumber.asText(d, 3, 0));
                    }
                });

        SWGGuiUtils.tableColumnSetWidth(mappingTable, 0, 55, 70, 85);

        mappingTable.getInputMap().put(KeyStroke.getKeyStroke("DELETE"),
                "tradeMiscDeleteAction");
        mappingTable.getActionMap().put("tradeMiscDeleteAction",
                new AbstractAction() {

                    public void actionPerformed(ActionEvent e) {
                        deletePair();
                    }
                });

        return new JScrollPane(mappingTable);
    }

    /**
     * Creates and returns a label for the upper part of this component
     * 
     * @return the upper component component
     */
    private Component makeTopPanel() {
        JLabel lab = new JLabel("Price to Item Name Mapping");
        lab.setHorizontalAlignment(SwingConstants.CENTER);
        lab.setOpaque(true);
        lab.setBorder(BorderFactory.createEtchedBorder());
        return lab;
    }

    /**
     * Returns an action listener
     * 
     * @return an action listener
     */
    private ActionListener saveListener() {
        saveListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        };
        return saveListener;
    }

    /**
     * Saves the table content to file
     */
    void saveToFile() {
        if (toon == null) return;

        try {
            File file = tradeTab.getPriceMappingsFile();
            ZWriter fw = ZWriter.newTextWriterExc(file, false);
            fw.writelnExc("# price to item name mapping, edit with care");
            for (Integer key : tradeTab.getPriceMappingKeys()) {
                fw.writeExc(key.toString());
                fw.writeExc(";");
                fw.writelnExc(tradeTab.priceToNameMapping.get(key));
            }
            fw.close();
        } catch (Throwable e) {
            SWGAide.printError("SWGTradeMisc:saveToFile: ", e);
        }
    }

    /**
     * Updates the log bar with some data
     */
    void updateLogBar() {
        tradeTab.frame.putToLogbar_2("     ");
    }

    /**
     * The underlying model for the table of prices mapped to names
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGTradePriceMappingTableModel extends
            AbstractTableModel {

        /**
         * Table column header titles
         */
        private String[] columnNames = { "Digits", "Mapped Name" };

        /**
         * Creates a model of this type.
         */
        SWGTradePriceMappingTableModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1) return String.class;
            return Integer.class;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public int getRowCount() {
            if (tradeTab.priceToNameMapping == null) return 1;
            return tradeTab.priceToNameMapping.size() + 1;
        }

        public Object getValueAt(int row, int column) {
            if (tradeTab.priceToNameMapping == null
                    || row >= tradeTab.priceToNameMapping.size())
                return column == 0
                        ? Integer.valueOf(0)
                        : "";

            if (column == 0)
                return tradeTab.getPriceMappingKeys().get(row);
            return tradeTab.priceToNameMapping.get(
                    tradeTab.getPriceMappingKeys().get(row));
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (rowIndex >= tradeTab.priceToNameMapping.size()) {
                // this must be a new row
                if (columnIndex == 0) { // price column
                    Integer v = (Integer) value;
                    int n = v.intValue();
                    if (n <= 0) return;

                    if (n > 999) n %= 1000;

                    newValue = Integer.valueOf(n);
                    tradeTab.priceToNameMapping.put(newValue, "");
                } else { // mapped name column
                    newValue = Integer.valueOf(0);
                    tradeTab.priceToNameMapping.put(newValue, value.toString());
                }
            } else {
                List<Integer> keys = tradeTab.getPriceMappingKeys();
                Integer key = keys.get(rowIndex);
                if (columnIndex == 0) {
                    Integer v = (Integer) value;
                    int n = v.intValue();
                    if (n <= 0) return;
                    if (n > 999) n %= 1000;
                    // change key mapping
                    String prevValue = tradeTab.priceToNameMapping.remove(key);
                    tradeTab.priceToNameMapping.put(
                            Integer.valueOf(n), prevValue);
                } else {
                    tradeTab.priceToNameMapping.put(key, (String) value);
                }
                newValue = null;
            }
            this.fireTableDataChanged();
        }
    }
}
