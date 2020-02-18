package swg.gui.common;

import java.awt.Toolkit;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import swg.SWGAide;

/**
 * An abstract document filter, clients must implement {@link #isValid(String)}.
 * A filter of this type validates input prior to changes of the document it
 * pertains to, if {@link #isValid(String)} returns {@code false} this type
 * prevents any updates.
 * <p>
 * If the string argument for the insert or replace methods is {@code null} this
 * type converts the argument to an empty string.
 * <p>
 * Clients may override the filter methods to insert, remove, and replace, but
 * usually the {@link #isValid(String)} handles most situations. If a client
 * overrides any of these methods the implementation <b>must invoke</b>
 * {@link #isValidHelper(String)} which invokes {@link #isValid(String)} and
 * then manages beeps and error handling.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public abstract class SWGDocFilter extends DocumentFilter {

    /**
     * A flag that determines if this filter should "beep" at invalid input.
     */
    private final boolean beep;

    /**
     * Creates an instance of this type that "beeps" at invalid input.
     */
    public SWGDocFilter() {
        this(true);
    }

    /**
     * Creates an instance of this type. The argument determines if this filter
     * should "beep" or silently reject invalid input.
     * 
     * @param beep {@code true} for beeps at invalid input
     */
    public SWGDocFilter(boolean beep) {
        this.beep = beep;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string,
            AttributeSet attrs) throws BadLocationException {

        String s = string == null
                ? ""
                : string;

        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
        sb.insert(offset, s);

        if (isValidHelper(sb.toString())) fb.insertString(offset, s, attrs);
    }

    /**
     * Returns {@code true} if the specified string is valid for this filter.
     * The argument is always what the resulting content of the document would
     * become, but the document is not yet modified.
     * <p>
     * If this method returns {@code true} the invoking method executes; one of
     * {@link #insertString(FilterBypass, int, String, AttributeSet)},
     * {@link #replace(FilterBypass, int, int, String, AttributeSet)}, or
     * {@link #remove(FilterBypass, int, int)}. Otherwise they does not modify
     * the document this filter pertains to.
     * <p>
     * This method should catch expected errors, but it is allowed to throw
     * other errors. An error is caught by this type and written to SWGAide's
     * log files and considered as invalid input.
     * 
     * @param s a string, or {@code null}
     * @return {@code true} is the argument is valid
     */
    protected abstract boolean isValid(String s);

    /**
     * A helper method that invokes {@link #isValid(String)} and returns the
     * response. If response is {@code false} and if {@link #beep} is {@code
     * true} this method invokes {@link Toolkit#beep()}, otherwise it does
     * nothing but returns the response. If there is an error it is intercepted
     * and written to SWGAide's log file and this method returns {@code false}.
     * 
     * @param s a string, or {@code null}
     * @return {@code true} is the argument is valid
     */
    protected final boolean isValidHelper(String s) {
        try {
            boolean b = isValid(s);
            if (!b && beep) Toolkit.getDefaultToolkit().beep();
            return b;
        } catch (Throwable e) {
            SWGAide.printError("SWGDocFilter:isValidHelper", e);
        }
        return false;
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {

        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
        sb.delete(offset, offset + length);

        if (isValidHelper(sb.toString())) fb.remove(offset, length);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text,
            AttributeSet attrs) throws BadLocationException {

        String s = text == null
                ? ""
                : text;

        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
        sb.replace(offset, (offset + length), s);

        if (isValidHelper(sb.toString())) fb.replace(offset, length, s, attrs);
    }
}
