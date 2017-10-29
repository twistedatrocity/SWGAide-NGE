package swg.gui.common;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * A non-extendable document filter for numeric input; for basic implementation
 * details see {@link SWGDocFilter}. Non-numerical values are rejected, possibly
 * except the empty string or {@code null}.
 * <p>
 * This type adds the following features to the default implementation:
 * <ul>
 * <li>always trims the resulting content before validation, but not the
 * displayed string</li>
 * <li>validation of lower limit, or {@link Integer#MIN_VALUE}</li>
 * <li>validation of upper limit, or {@link Integer#MAX_VALUE}</li>
 * <li>(optional) update an empty document with a pre-specified number</li>
 * </ul>
 * This type implements the {@link #isValid(String)} method and validates input
 * against lower and upper limits.
 * <p>
 * If an instance of this type optionally should handle "display nbr for empty"
 * this type prepares the argument for before invoking {@link #isValid(String)},
 * but if the pre-specified number is rejected this filter does nothing furher
 * but modification of the document is rejected. If this filter <i>should not
 * handle </i> "display nbr for empty" the validator must handle such cases.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGDocNumberFilter extends SWGDocFilter {

    /**
     * A flag that determines if the empty string is accepted as "zero". This
     * flag does overrule {@link #preVal} for zero but not for any other value.
     */
    protected boolean acceptEmpty;

    /**
     * The highest acceptable value.
     */
    private final long max;

    /**
     * The lowest acceptable value.
     */
    private final int min;

    /**
     * A value that is is used for "display nbr for empty", or {@code null}.
     * Compare {@link #acceptEmpty}.
     */
    private final Number preVal;

    /**
     * Creates an instance of this type that "beeps" at invalid input. The
     * filter allows empty an empty document and any numerical value between
     * 0 and {@link Long#MAX_VALUE}.
     */
    public SWGDocNumberFilter() {
        this(true, null, 0, Long.MAX_VALUE);
    }

    /**
     * Creates an instance of this type. The boolean argument determines if this
     * filter should "beep" or silently reject invalid input. If the number is
     * not {@code null} this filter handles "display nbr for empty" (see the
     * general class comment), otherwise input is handled as-is.
     * 
     * @param beep enables beeps for invalid input
     * @param nbr a pre-specified number for empty input, or {@code null}
     * @param min the lowest acceptable value
     * @param max the highest acceptable value
     */
    public SWGDocNumberFilter(boolean beep, Number nbr, int min, long max) {
        super(beep);

        preVal = nbr;

        this.min = min;
        this.max = max;
    }

    /**
     * Helper method which returns a string builder for the document of the
     * specified filter-by-pass object.
     * 
     * @param fb a filter-by-pass object
     * @return a string builder
     * @throws BadLocationException if there is an error
     */
    private StringBuilder getSB(FilterBypass fb) throws BadLocationException {
        Document doc = fb.getDocument();
        return new StringBuilder(doc.getText(0, doc.getLength()));
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string,
            AttributeSet attrs) throws BadLocationException {

        String s = string == null
                ? ""
                : string;

        StringBuilder sb = getSB(fb);
        sb.insert(offset, s);

        String t = sb.toString().trim();

        if (isEmptyHelper(t, fb, attrs)) return;

        if (isValidHelper(t)) fb.insertString(offset, s, attrs);
    }

    /**
     * A helper method which handles the case of an empty document. Only if
     * 
     * <pre> preVal != null 
     * && t.isEmpty() 
     * && isValidHelper(preVal.toString())</pre>
     * 
     * this method replaces any content of the specified document with the text
     * representation of {@link #preVal} and returns {@code true}. Otherwise
     * this method returns {@code false}.
     * 
     * @param t a trimmed string
     * @param fb a filter-by-pass
     * @param attrs a set of attributes
     * @return {@code true} if this method updated the document
     * @throws BadLocationException if there is an error
     */
    private boolean isEmptyHelper(String t, FilterBypass fb, AttributeSet attrs)
            throws BadLocationException {

        if (t.isEmpty()) {
            if (acceptEmpty && (preVal == null || preVal.intValue() == 0)) {
                fb.remove(0, fb.getDocument().getLength());
                return true;
            }
            if (preVal != null && isValidHelper(preVal.toString())) {
                fb.replace(0, fb.getDocument().getLength(),
                        preVal.toString(), attrs);
                return true;
            }
        }
        return false;
    }

    /**
     *{@inheritDoc}
     * <p>
     * <b>This implementation</b> invokes {@link Double#parseDouble(String)},
     * which is valid for any number, and returns {@code true} if the parsed
     * value is within the specified range. If {@link #acceptEmpty} is {@code
     * true} and the argument is {@code null} or the empty string this method
     * returns {@code true}. This method intercepts
     * {@link NumberFormatException} but no other errors.
     */
    @Override
    protected boolean isValid(String s) {
        try {
            if (acceptEmpty && s == null || s.isEmpty()) return true;
            double d = Double.parseDouble(s);
            return min <= d && d <= max;
        } catch (NumberFormatException e) {/* ignore */
        }
        return false;
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {

        StringBuilder sb = getSB(fb);
        sb.delete(offset, offset + length);

        String t = sb.toString().trim();

        if (isEmptyHelper(t, fb, null)) return;

        if (isValidHelper(t)) fb.remove(offset, length);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text,
            AttributeSet attrs) throws BadLocationException {

        String s = text == null
                ? ""
                : text;

        StringBuilder sb = getSB(fb);
        sb.replace(offset, (offset + length), s);

        String t = sb.toString().trim();

        if (isEmptyHelper(t, fb, null)) return;

        if (isValidHelper(t)) fb.replace(offset, length, s, attrs);
    }
}
