package swg.gui.common;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.CellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;

import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;

/**
 * This type provides the ability to intercept and manipulate the old value that
 * is specified to be edited before it is displayed for the user and the user is
 * allowed to begin editing.
 * <p>
 * This is useful for a table cell that is populated by wrapper, for example an
 * instance of {@link TableCellDecorations} or a HTML text snippet but the cells
 * are also editable. This type allows you to extract and send the plain value
 * to the {@link CellEditor}.
 * <p>
 * This type provides developers to override the helper methods in a minimal
 * fashion or to override {@link #myValue(JTable, Object, int, int)} in full.
 * This can easily be done as anonymous where this type is instantiated.
 * 
 * @author John Thomas aka Chilastra.Oarun
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGTableCellEditor extends AbstractCellEditor implements
        TableCellEditor {

    // XXX: test how it impacts on performance to skip this type completely and
    // have the renderer/editor ask the table argument for data or simply use
    // the object when that works

    /**
     * This is the component that will handle the editing of the cell value.
     */
    protected JTextField component;

    /**
     * Creates an instance of this table cell editor with a {@link JTextField}.
     */
    public SWGTableCellEditor() {
        super();
        component = new JTextField();
    }

    /**
     * Creates an instance of this table cell editor with the specified
     * horizontal alignment set to the {@link JTextField}. Valid arguments are
     * the {@link SwingConstants#LEADING}, {@link SwingConstants#CENTER}, and
     * {@link SwingConstants#TRAILING}.
     * 
     * @param hAlign the horizontal alignment for this instance
     */
    public SWGTableCellEditor(int hAlign) {
        this();
        component.setHorizontalAlignment(hAlign);
    }

    public Object getCellEditorValue() {
        // This method is called after editing is completed.
        // It must return the new value to be stored in the cell.
        return component.getText();
    }

    /**
     * This method sets a value at this cell editor and then returns this
     * instance. However, before setting a value at itself it invokes
     * {@link #myValue(JTable, Object, int, int)}. Hence sub-classes may:
     * <ul>
     * <li>override this method and handle the invocation in full, not calling
     * {@code myValue(...)}</li>
     * <li>override both this method and {@code myValue(...)} for some purpose</li>
     * <li>override only {@code myValue(...)} and let this method set a value at
     * this editor</li>
     * </ul>
     * <p>
     * This method does not consider {@code isSelected}.
     */
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {

        String val = myValue(table, value, row, column);

        // Configure the component with the specified value
        component.setText(val);

        // Return the configured component
        return component;
    }

    /**
     * Helper method which returns a string, or an empty string. If both the
     * former helper methods returned {@code null} this method is invoked. This
     * method is used as a last resort and may return any string or an empty
     * string; whether the return value is based on whatever or not is at the
     * developers discretion.
     * <p>
     * This particular method always returns an empty string.
     * 
     * @return a string, or an empty string
     */
    protected String myValue() {
        return "";
    }

    /**
     * Helper method which returns a string derived from the specified table
     * cell, or {@code null}. In particular, a sub-class may return {@code null}
     * to neglect the table, which causes the caller to invoke next helper
     * function. This method also returns {@code null} if the argument is
     * {@code null} or if there is an error.
     * <p>
     * This particular method returns a string for the specified table cell if
     * {@code table != null} and if the object at the specified row and column
     * is not {@code null}. Otherwise {@code null} is returned.
     * 
     * @param table the {@link JTable} that is asking the editor to edit; can be
     *        {@code null}
     * @param row the row of the cell being edited
     * @param col the column of the cell being edited
     * @return a string, or {@code null}
     */
    protected String myValue(JTable table, int row, int col) {
        Object o;
        if (table != null && (o = table.getValueAt(row, col)) != null)
                return o.toString();
        return null;
    }

    /**
     * Helper method which returns a value determined by the arguments.
     * <p>
     * This particular method invokes helper methods and returns the first
     * {@code response != null}, invocations in order: {@link #myValue(Object)},
     * {@link #myValue(JTable, int, int)}, and {@link #myValue()}. If all of
     * these returns {@code null} this method returns an empty string.
     * <p>
     * This sequential construct makes it possible for sub-classes to override
     * this method, or the helper methods to achieve wanted result.
     * 
     * @param table the {@link JTable} that is asking the editor to edit; can be
     *        {@code null}
     * @param val the value of the cell to be edited; {@code null} is valid
     * @param row the row of the cell being edited
     * @param col the column of the cell being edited
     * @return the string to initiate this editor with, or the empty string
     */
    protected String myValue(JTable table, Object val, int row, int col) {
        String s = myValue(val);
        if (s != null) return s;

        s = myValue(table, row, col);
        if (s != null) return s;

        s = myValue();
        return s != null
                ? s
                : "";
    }

    /**
     * Helper method which returns a string determined by the argument, or
     * {@code null}. In particular, a sub-class may return {@code null} to
     * neglect any specified value, which causes the caller to invoke next
     * helper function. This method also returns {@code null} if the argument is
     * {@code null} or if it is insensible.
     * <p>
     * This particular method returns a string based on the specified value...<br>
     * - if it is a string that is not a HTML tag, the string as is<br/>
     * - if it is assignable from {@link Number}, its text value<br/>
     * - if it is a sub-class of {@link TableCellDecorations} and if {@code
     * val.values[0] != null}; , the latter as text<br/>
     * Otherwise this method returns {@code null}
     * 
     * @param val a value to evaluate
     * @return a string, or {@code null}
     */
    protected String myValue(Object val) {
        if (val != null) {
            Class<?> c = val.getClass();

            if (String.class.isAssignableFrom(c)) {
                String s = ((String) val).trim();
                if (!s.startsWith("<htm") && !s.startsWith("<HTM"))
                    return (String) val;
            }

            if (Number.class.isAssignableFrom(c))
                return val.toString();

            Object o;
            if (TableCellDecorations.class.isAssignableFrom(c)
                    && (o = ((TableCellDecorations) val).values[0]) != null)
                return o.toString();
        }
        return null;
    }
}
