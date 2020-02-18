package swg.gui.common;

import java.awt.AWTEvent;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import swg.SWGAide;

/**
 * This type a light weight wrapper for {@link JTable} which overrides a few
 * methods. The overridden methods catch exceptions and log these to SWGAide's
 * error log file. This also patches some inconsistencies in GUI elements that
 * are caused by such errors.
 * <p>
 * <!-- DEVELOPER_NOTE --> This is the preferred type over {@link JTable}.
 * Extend this type when more exceptions are found, select a method that will
 * catch errors early to reduce inconsistencies.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGJTable extends JTable {

    /**
     * See {@link JTable#JTable()}
     */
    public SWGJTable() {
        super();
    }

    /**
     * See {@link JTable#JTable(TableModel)}
     */
    public SWGJTable(TableModel dm) {
        super(dm);
    }

    @Override
    public Component prepareRenderer(
            TableCellRenderer renderer, int row, int column) {
        try {
            return super.prepareRenderer(renderer, row, column);
        } catch (Exception e) {
            SWGAide.printError("SWGJTable:prepareRenderer: " +
                    renderer + "::" + row + ":" + column, e);
        }
        return new JLabel("ERROR: " + row + " : " + column);
    }

    @Override
    protected void processEvent(AWTEvent e) {
        try {
            super.processEvent(e);
        } catch (Exception exc) {
            SWGAide.printError("SWGJTable:processEvent", exc);
        }
    }
}
