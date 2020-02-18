package swg.gui.common;

import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

/**
 * This type is a light weight list cell renderer for {@link JList} and for
 * {@link JComboBox}.
 * <p>
 * This type makes it possible to tailor the string which is rendered at the
 * cell, to determine the background color, and it is easy to add further stub
 * methods when a need arise. The extra methods in this type provides the same
 * functions as a default cell renderer.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGListCellRenderer<T> implements ListCellRenderer<T> {

    /**
     * The label for this renderer.
     */
    protected final JLabel label;

    /**
     * Creates an instance of this renderer. This instance is opaque with
     * {@link Font#PLAIN}, text aligned with {@link SwingConstants#LEADING}, and
     * the text at an offset of 3 empty from the leading edge.
     */
    public SWGListCellRenderer() {
        this(SWGGuiUtils.fontPlain(), SwingConstants.LEADING, 3);
    }

    /**
     * Creates an opaque instance of this renderer with the specified arguments,
     * compare with {@link #SWGListCellRenderer()}.
     * 
     * @param f a font
     * @param hAlign horizontal alignment, one of {@link SwingConstants}
     * @param offset offset for the text from the leading edge, or 0
     */
    public SWGListCellRenderer(Font f, int hAlign, int offset) {
        label = new JLabel();
        label.setOpaque(true);
        label.setFont(f);
        label.setHorizontalAlignment(hAlign);
        label.setBorder(BorderFactory.createEmptyBorder(0, offset, 0, 0));
    }

    /**
     * Set the background color for this renderer. This method determines the
     * color just by the value of {@code isSelected}. Extend this method to
     * determine the color by any other means.
     * 
     * @param list the list which this instance pertains to
     * @param value the object returned by list.getModel().getElementAt(index)
     * @param index the cell's index
     * @param isSelected {@code true} if the specified cell is selected
     * @param cellHasFocus {@code true} if the specified cell has the focus
     */
    protected void colorBackground(JList<? extends T> list, T value, int index,
            boolean isSelected, boolean cellHasFocus) {

        if (isSelected)
            label.setBackground(list.getSelectionBackground());
        else
            label.setBackground(list.getBackground());
    }

    /**
     * Set the foreground color for this renderer. This method determines the
     * color just by the value of {@code isSelected}. Extend this method to
     * determine the color by any other means.
     * 
     * @param list the list which this instance pertains to
     * @param value the object returned by list.getModel().getElementAt(index)
     * @param index the cell's index
     * @param isSelected {@code true} if the specified cell is selected
     * @param cellHasFocus {@code true} if the specified cell has the focus
     */

    protected void colorForeground(JList<T> list, T value, int index,
            boolean isSelected, boolean cellHasFocus) {

        if (isSelected)
            label.setForeground(list.getSelectionForeground());
        else
            label.setForeground(list.getForeground());
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends T> list,
            T value, int index, boolean isSelected,
            boolean cellHasFocus) {

        label.setText(labelString(list, value, index, isSelected, cellHasFocus));

        colorBackground(list, value, index, isSelected, cellHasFocus);

        return label;
    }

    /**
     * Returns a string for this renderer. This method is invoked by
     * {@link #getListCellRendererComponent(JList, Object, int, boolean, boolean)}
     * and invokes {@link #labelString(Object)}. Extend this method or the light
     * weight {@link #labelString(Object)} to determine the string by any other
     * means.
     * 
     * @param list the list which this instance pertains to
     * @param value the object returned by list.getModel().getElementAt(index)
     * @param index the cell's index
     * @param isSelected {@code true} if the specified cell is selected
     * @param cellHasFocus {@code true} if the specified cell has the focus
     * @return the string to render
     */

    protected String labelString(JList<? extends T> list, T value, int index,
            boolean isSelected, boolean cellHasFocus) {

        return labelString(value);
    }

    /**
     * Returns a string for this renderer. This method returns the value from
     * {@link Object#toString()} or the empty string if {@code value == null}.
     * Extend this method to determine the string by any other means.
     * 
     * @param value the object to render as text
     * @return the string to render
     */
    protected String labelString(T value) {
        return value == null
                ? ""
                : value.toString();
    }
}
