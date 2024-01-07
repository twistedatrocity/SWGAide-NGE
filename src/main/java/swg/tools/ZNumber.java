package swg.tools;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import swg.SWGAide;

/**
 * This type provides convenience methods and and related features for numbers,
 * integers as well as floating points. Formatters and strings use the current
 * locale.
 * <p>
 * The parsing methods do not replace for example {@link Long#parseLong(String)}
 * but special logic is added to them.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class ZNumber {

    /**
     * Helper decimal formatter for table cell rendering.
     */
    private static final NumberFormat df = NumberFormat.getNumberInstance();

    /**
     * A number formatter for decimal values using the current locale. This
     * instance always use two decimals.
     */
    private static final NumberFormat nfd = NumberFormat.getInstance();

    /**
     * A number formatter for integer values using the current locale.
     */
    private static final NumberFormat nfi = NumberFormat.getIntegerInstance();

    /**
     * A number formatter for percent values on the format "0.00%".
     */
    private static final DecimalFormat pf = new DecimalFormat("0.00%");

    static { // Static initializer
        nfd.setMinimumFractionDigits(2);
        nfd.setMaximumFractionDigits(2);
    }

    /**
     * Returns the specified value as formatted percentual text. The value is
     * formatted as "0.00%".
     * 
     * @param value an integer value
     * @return {@code value} as formatted text
     */
    public static String asPercent(double value) {
        return pf.format(value);
    }

    /**
     * Returns the specified {@code double} as formatted text. The second
     * argument determines the minimum number of integer parts; the third
     * argument determines how many fractions to display; if any of these
     * arguments are negative they default to zero.
     * 
     * @param value an integer value
     * @param ints minimum integer parts
     * @param fracs number of decimals
     * @return {@code value} as formatted text
     */
    public static String asText(double value, int ints, int fracs) {
        df.setMinimumIntegerDigits(ints);
        df.setMinimumFractionDigits(fracs);
        df.setMaximumFractionDigits(fracs);
        return df.format(value);
    }

    /**
     * Returns the specified argument as text, or {@code null}. The "format"
     * argument determines if the returned string should be formatted per the
     * current locale. The "zero" argument determines if {@code value == 0}
     * returns "0", otherwise {@code null}.
     * 
     * @param value an integer value
     * @param format {@code true} to format the text
     * @param zero {@code true} for zero as string
     * @return {@code value} as text, or {@code null}
     */
    public static String asText(long value, boolean format, boolean zero) {
        // dt.s
        if (value > 0 || (zero && value == 0))
            try {
                return format
                        ? nfi.format(value)
                        : Long.toString(value);
            } catch (Throwable e) {
                SWGAide.printError("ZNumber:asText: " + value, e);
            }
        return null;
    }
    
    /**
     * Returns the specified argument as text, or {@code null}. The "format"
     * argument determines if the returned string should be formatted per the
     * current locale. The "zero" argument determines if {@code value == 0}
     * returns "0", otherwise {@code null}.
     * 
     * @param value an double value
     * @param format {@code true} to format the text
     * @param zero {@code true} for zero as string
     * @return {@code value} as text, or {@code null}
     */
    public static String asText(double value, boolean format, boolean zero) {
        // dt.s
        if (value > 0 || (zero && value == 0))
            try {
                return format
                        ? nfd.format(value)
                        : Double.toString(value);
            } catch (Throwable e) {
                SWGAide.printError("ZNumber:asText: " + value, e);
            }
        return null;
    }

    /**
     * Returns the specified argument as text. If the argument is an instance of
     * {@link Long} or {@link Integer} this method returns the response from
     * {@code asText(value.longValue(), true, false)}. If the argument is an
     * instance of {@link Double} or {@link Float} this method returns the
     * response from {@link NumberFormat#format(double)} and always with two
     * decimals. Otherwise the argument is returned as-is.
     * 
     * @param value a number
     * @return {@code value} as text, or {@code null}
     */
    public static String asText(Number value) {
        if (value instanceof Long || value instanceof Integer)
            return asText(value.longValue(), true, false);

        if (value instanceof Double || value instanceof Float)
            return nfd.format(value.doubleValue());

        return null;
    }

    /**
     * Returns an integer as defined by {@link #longExc(String)}.
     * 
     * @param s a string, {@code null}, or an empty string
     * @return an integer, or {@code 0}
     * @throws Exception if the string is not parsable
     */
    public static int intExc(String s) throws Exception {
        return (int) ZNumber.longExc(s);
    }

    /**
     * Returns a long as defined by {@link #longExc(String)}. If there is an
     * error this method returns 0.
     * 
     * @param s a string
     * @return an integer, or {@code 0}
     */
    public static int intVal(String s) {
        return (int) ZNumber.longVal(s);
    }

    /**
     * Returns {@code true} if the specified string represents an integer. The
     * boolean value determines if this method should return {@code true} for an
     * empty string.
     * 
     * @param s a string
     * @param empty {@code true} to accept an empty string
     * @return {@code true} for an integer, or possibly an empty string
     */
    public static boolean isInteger(String s, boolean empty) {
        try {
            if (!(empty && s.isEmpty())) Long.parseLong(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Returns a long integer parsed from the argument, or 0. If the argument is
     * {@code null} or the empty string this method returns 0. This method trims
     * the string and strips it from possible double quotes before it is parsed.
     * If the number is formatted
     * 
     * @param s a string, {@code null}, or an empty string
     * @return a long integer, or {@code 0L}
     * @throws Exception if the string is not an integer
     */
    public static long longExc(String s) throws Exception {
        String ss;
        if (s == null || (ss = s.replace("\"", "").trim()).isEmpty())
            return 0L;
        return nfi.parse(ss).longValue();
    }

    /**
     * Returns a long as defined by {@link #longExc(String)}. If there is an
     * error this method returns 0.
     * 
     * @param s a string
     * @return a long, or {@code 0L}
     */
    public static long longVal(String s) {
        try {
            return longExc(s);
        } catch (Exception e) {
            return 0L;
        }
    }
}
