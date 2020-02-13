package swg.gui.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import swg.tools.ZNumber;

/**
 * This type extends the default table cell renderer and provides generic
 * methods for various parts that use to be set by the super class.
 * <p>
 * The implementation of
 * {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}
 * has factored out all logical sections to helper methods. Hence sub-classes
 * can override either one, see the my*** methods.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGDecoratedTableCellRenderer extends DefaultTableCellRenderer {

    /**
     * The default font for table cells.
     */
    protected final Font defaultFont;

    /**
     * The default background color for a selected table cell.
     */
    protected final Color defaultSelectionBackground;

    /**
     * The default foreground color for a selected table cell.
     */
    protected final Color defaultSelectionForeground;

    /**
     * The table model to use for a decorated table. If this member is {@code
     * null} this renderer does not inherently support those features,
     */
    private final DecoratedTableModel tableModel;

	private Color defaultBackground;

	private Color defaultForeground;

    /**
     * Creates a basic instance of this type. This constructor sets the members
     * for default back- and foreground colors to values which are obtained from
     * {@link UIManager}.
     */
    public SWGDecoratedTableCellRenderer() {
        this(null);
    }

    /**
     * Creates an instance of this type that supports the features provided by
     * the enhanced {@link DecoratedTableModel}. All helper methods that make
     * use of decorated values is passed a {@link TableCellDecorations}.
     * <p>
     * This constructor sets the default members to values which are obtained
     * from {@link UIManager}. Usage:
     * 
     * <pre> DecoratedTableModel dModel = new DecoratedTableModel();
     * JTable table = new SWGJTable(dModel);
     * table.setDefaultRenderer(Foo.class, 
     *         new SWGDecoratedTableCellRenderer(dModel));</pre>
     * 
     * @param decoratedTableModel a table model
     */
    public SWGDecoratedTableCellRenderer(DecoratedTableModel decoratedTableModel) {
        super();
        setOpaque(true);
        defaultBackground = UIManager.getColor("Table.background");
        defaultForeground = UIManager.getColor("Table.foreground");
        defaultSelectionBackground = UIManager.getColor("Table.selectionBackground");
        defaultSelectionForeground = UIManager.getColor("Table.selectionForeground");
        defaultFont = UIManager.getFont("Table.font");
        setFont(defaultFont);
        tableModel = decoratedTableModel;
    }

    /**
     * This implementation has factored out all of the content to logical
     * methods so that sub-classing types can override either one of the helper
     * methods; see the my*** methods. If this instance serves a decorated table
     * model it invokes
     * {@link DecoratedTableModel#getCellDecor(int, int, Object)} once and
     * passes the obtained object to related helper methods, otherwise {@code
     * null} is used.
     * <p>
     * <b>Notice:</b> all invocations to the helper methods use row and column
     * <b>already converted</b> from table to model.
     * <p>
     * The very first invocation is to
     * {@link #myFirst(JTable, Object, boolean, boolean, int, int)} which
     * determines if this method should proceed or instantly return this
     * instance as is. The remaining helper methods are invoked in order:<br/>
     * if decorated mode:
     * {@link DecoratedTableModel#getCellDecor(int, int, Object)}<br/>
     * {@link #myBackground(JTable, Object, boolean, boolean, int, int, TableCellDecorations)}
     * <br/>
     * {@link #myForeground(JTable, Object, boolean, boolean, int, int, TableCellDecorations)}
     * <br/>
     * {@link #myFont(JTable, Object, boolean, boolean, int, int, TableCellDecorations)}
     * <br/>
     * {@link #myAlignment(JTable, Object, boolean, boolean, int, int, TableCellDecorations)}
     * <br/>
     * {@link #myBorder(JTable, Object, boolean, boolean, int, int, TableCellDecorations)}
     * <br/>
     * {@link #myTooltip(JTable, Object, boolean, boolean, int, int, TableCellDecorations)}
     * <br/>
     * {@link #myValue(JTable, Object, boolean, boolean, int, int)}
     */
    @Override
    public final Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column) {

        int r = table.convertRowIndexToModel(row);
        int c = table.convertColumnIndexToModel(column);
        if (!myFirst(table, value, isSelected, hasFocus, r, c))
            return this;

        TableCellDecorations tcd = tableModel != null
                ? tableModel.getCellDecor(r, c, value)
                : null;

        // back- and fore-ground colors
        myBackground(table, value, isSelected, hasFocus, r, c, tcd);
        myForeground(table, value, isSelected, hasFocus, r, c, tcd);

        // font and alignment
        myFont(table, value, isSelected, hasFocus, r, c, tcd);
        myAlignment(table, value, isSelected, hasFocus, r, c, tcd);

        // border handling
        myBorder(table, value, isSelected, hasFocus, r, c, tcd);

        myTooltip(table, value, isSelected, hasFocus, r, c, tcd);

        // set the data from the data-color-pair
        mySetValue(myValue(table, value, isSelected, hasFocus, r, c));

        return this;
    }
    
    /**
     * Helper method which aligns the content for this renderer at the specified
     * cell. A sub-class may optionally use {@code decor} to contain an
     * alignment and override this method.
     * <p>
     * This particular method, if {@code value} is assignable from
     * {@link Number} this renderer is right-aligned, otherwise left-aligned. If
     * {@code value == null} this method does nothing
     * 
     * @param table the table for this renderer
     * @param value the value to assign to the cell at {@code [row, column]}
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus {@code true} if the cell has focus
     * @param row the row that refers to the table model
     * @param column the column that refers to the table model
     * @param decor table cell decorations, or {@code null}
     */
    protected void myAlignment(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column, TableCellDecorations decor) {

        if (value == null) return;
        setHorizontalAlignment(value instanceof Number
                ? SwingConstants.TRAILING
                : SwingConstants.LEADING);
    }

    /**
     * Helper method which sets a background color for this renderer at the
     * specified cell.
     * <p>
     * This particular method, if {@code decor == null && decor.bgColor == null}
     * then {@code isSelected} determines if {@link #defaultSelectionBackground}
     * or Color.WHITE is used, otherwise {@code isSelected} determines if the
     * specified color is darkened or used as is; compare
     * {@link SWGGuiUtils#colorDarker(Color, float)}.
     * 
     * @param table the table for this renderer
     * @param value the value to assign to the cell at {@code [row, column]}
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus {@code true} if the cell has focus
     * @param row the row that refers to the table model
     * @param column the column that refers to the table model
     * @param decor table cell decorations, or {@code null}
     */
    protected void myBackground(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column, TableCellDecorations decor) {

        Color bg = decor != null
                ? decor.bgColor
                : null;

        if (bg == null)
            bg = (isSelected)
                    ? defaultSelectionBackground
                    : defaultBackground;
        else
            bg = (isSelected)
                    ? SWGGuiUtils.colorDarker(bg, 0.9f)
                    : bg;

        setBackground(bg);
    }

    /**
     * Helper method which sets a border for this renderer at the specified
     * cell. An implementation can may let {@code decor} contain a border and
     * override this method.
     * <p>
     * This particular method, if {@code hasFocus} it obtains a border from
     * {@link DefaultLookup} determined by {@code isSelected}, otherwise it uses
     * {@link DefaultTableCellRenderer#noFocusBorder}.
     * 
     * @param table the table for this renderer
     * @param value the value to assign to the cell at {@code [row, column]}
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus {@code true} if the cell has focus
     * @param row the row that refers to the table model
     * @param column the column that refers to the table model
     * @param decor table cell decorations, or {@code null}
     */
    protected void myBorder(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column, TableCellDecorations decor) {
        Border border = null;
        if (hasFocus) {
            if (isSelected)
                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");

            if (border == null)
                border = UIManager.getBorder("Table.focusCellHighlightBorder");
        } else
            border = noFocusBorder;

        setBorder(border);
    }

    /**
     * Helper method which is invoked before anything else is done in
     * {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}
     * This method makes it possible to intercept invocations of the mentioned
     * method, for example to handle the case that the specified value is
     * {@code null}, or for other reasons.
     * <p>
     * If this method returns {@code true} the calling method continues,
     * otherwise, if this method returns {@code false} the calling function
     * should abort and return itself as is. This method is free to do anything
     * sensible before returning {@code true} or {@code false}.
     * <p>
     * This particular method, if {@code value == null} it invokes {@code
     * setValue(null)}. It always returns {@code true}.
     * 
     * @param table the table for this renderer
     * @param value the value to assign to the cell at {@code [row, column]}
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus {@code true} if the cell has focus
     * @param row the row that refers to the table model
     * @param column the column that refers to the table model
     * @return {@code false} if the caller should instantly abort
     */
    protected boolean myFirst(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (value == null) setValue(value);
        return true;
    }

    /**
     * Sets a font for this renderer at the specified cell. A sub-class may
     * optionally use {@code decor} to contain a font and override this method.
     * <p>
     * 
     * @param table the table for this renderer
     * @param value the value to assign to the cell at {@code [row, column]}
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus {@code true} if the cell has focus
     * @param row the row that refers to the table model
     * @param column the column that refers to the table model
     * @param decor table cell decorations, or {@code null}
     */
    protected void myFont(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column, TableCellDecorations decor) {
    	if(decor.value() instanceof Font) {
    		Font f = (Font) decor.value();
    		setFont(f);
    	}
    }

    /**
     * Helper method which sets a foreground color for this renderer at the
     * specified cell.
     * <p>
     * This particular method, if {@code decor == null && decor.fgColor == null}
     * then {@code isSelected} determines if {@link #defaultSelectionForeground}
     * or Color.BLACK is used, otherwise the specified foreground color.
     * 
     * @param table the table for this renderer
     * @param value the value to assign to the cell at {@code [row, column]}
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus {@code true} if the cell has focus
     * @param row the row that refers to the table model
     * @param column the column that refers to the table model
     * @param decor table cell decorations, or {@code null}
     */
    protected void myForeground(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column, TableCellDecorations decor) {

        Color fg = (decor != null)
                ? decor.fgColor
                : null;

        if (fg == null)
            fg = (isSelected)
                    ? defaultSelectionForeground
                    : defaultForeground;

        setForeground(fg);
    }

    /**
     * Helper method which sets the specified object to be rendered by this
     * renderer at the specified cell.
     * <p>
     * This particular method invokes {@link #setValue(Object)} with the
     * argument as is.
     * 
     * @param value the value to assign to the cell at {@code [row, column]}
     */
    protected void mySetValue(Object value) {
        setValue(value);
    }

    /**
     * Helper method which sets a tool-tip text for this renderer at the
     * specified cell.
     * <p>
     * This particular method sets the tool-tip text to {@code null} if {@code
     * decor == null || decor.tooltip == null}, otherwise to the specified
     * value.
     * 
     * @param table the table for this renderer
     * @param value the value to assign to the cell at {@code [row, column]}
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus {@code true} if the cell has focus
     * @param row the row that refers to the table model
     * @param column the column that refers to the table model
     * @param decor table cell decorations, or {@code null}
     */
    protected void myTooltip(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column, TableCellDecorations decor) {

        setToolTipText(decor != null
                ? decor.tooltip
                : null);
    }

    /**
     * Helper method which return the object to be rendered by this renderer at
     * the specified cell.
     * <p>
     * This particular method returns the specified value as is.
     * 
     * @param table the table for this renderer
     * @param value the value to assign to the cell at {@code [row, column]}
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus {@code true} if the cell has focus
     * @param row the row that refers to the table model
     * @param column the column that refers to the table model
     * @return the value to assign to the cell at {@code [row, column]}
     */
    protected Object myValue(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        return value;
    }

    /**
     * Creates and returns an instance of this type that formats and renders
     * number values per the current locale. The returned instance overrides
     * {@link #mySetValue(Object)} so its sets the return value from
     * {@link ZNumber#asText(Number)} to be rendered; other methods have
     * their default implementation.
     * 
     * @return a number renderer
     */
    public static final SWGDecoratedTableCellRenderer newNumberRenderer() {
        return new SWGDecoratedTableCellRenderer() {
            @Override
            protected void mySetValue(Object value) {
                setValue(ZNumber.asText((Number)value));
            }
        };
    }

    /**
     * This interface specifies one method that the enclosing type &mdash;
     * {@link SWGDecoratedTableCellRenderer} &mdash; will use for decorated
     * cells. If the renderer is created for a table model of this type its
     * {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}
     * invokes this interface method to obtain a container with back- and
     * foreground colors, tool-tip text, and optional data, all of these are
     * optional; more details are specified by {@link TableCellDecorations}.
     * <p>
     * A model that implements this interface is free to return any value that
     * satisfies the specific implementation of the renderer, including {@code
     * null}. A renderer that utilizes this interface method may, for its scope,
     * invoke it only once, passing the data to its related helper methods.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public interface DecoratedTableModel extends TableModel {

        /**
         * Returns an object for the specified cell, or {@code null}. The object
         * contains back- and foreground colors for a table cell, tool-tip text,
         * and other optional data, all of these values are optional and may be
         * {@code null}; also, {@code null} is a valid return value.
         * <p>
         * This method optionally takes an object. The argument probably equals
         * the value obtained from the model for for {@code [row, column]}, an
         * implementation may decide to rather use this argument in the case it
         * is "cheaper" than to compute the value again.
         * <p>
         * <b>Notice:</b> row and column are <b>already converted</b> to model.
         * 
         * @param row the row that refers to the table model
         * @param column the column that refers to the table model
         * @param value a value to be rendered, or {@code null}
         * @return a color-data object, or {@code null}
         * @throws IndexOutOfBoundsException if an index is out of bounds,
         *         unless the implementation allows for unusual values
         */
        public TableCellDecorations getCellDecor(
                int row, int column, Object value);
    }

    /**
     * A light-weight, helper container for a table cell renderer. An instance
     * contains back- and foreground color, a field for tool-tip text, and
     * optional data. This type is made quite generic and clients must
     * themselves ensure the "producer" and "consumer" agrees on content for the
     * array of values.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public final static class TableCellDecorations {

        /**
         * A background, or {@code null}. A {@code null} value signals a default
         * color.
         */
        public final Color bgColor;

        /**
         * A foreground, or {@code null}. A {@code null} value signals a default
         * color.
         */
        public final Color fgColor;

        /**
         * A tool-tip text, or {@code null}.
         */
        public final String tooltip;

        /**
         * An array with the one or several values for this container. Clients
         * must themselves ensure the "producer" and "consumer" agrees on
         * content.
         */
        public final Object[] values;

        /**
         * Creates an instance of this type. {@code null} for a color signals a
         * default color to the client. The values argument takes {@code null}
         * or an arbitrary number of objects, the developer must ensure that the
         * "consumer" and "producer" agree.
         * 
         * @param bg a background color, or {@code null}
         * @param fg a foreground color, or {@code null}
         * @param tt a tool-tip text, or {@code null}
         * @param values some data, one or several objects, or {@code null}
         */
        public TableCellDecorations(Color bg, Color fg, String tt,
                Object... values) {
            this.bgColor = bg;
            this.fgColor = fg;
            this.tooltip = tt;
            this.values = values;
        }

        /**
         * Returns the first object from the array of values, or {@code null}.
         * If the array itself is {@code null} this method returns {@code null};
         * otherwise it returns the first element, which may be {@code null}.
         * 
         * @return an object, or {@code null}
         */
        public Object value() {
            return (values == null)
                    ? null
                    : values[0];
        }
    }
}
