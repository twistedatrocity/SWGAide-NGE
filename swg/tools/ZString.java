package swg.tools;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * This type is a mutable string and it provides some convenience methods that
 * are related to strings. This type can be used for {@link String} and for
 * {@link StringBuilder} if the string is anticipated to be modified; its
 * implementation is conservative, hence light weight. For strings that won't be
 * modified it is better to use the usual <tt>String s = "a string";</tt>
 * pattern.
 * <p>
 * An instance of this type is supposed to be used in a contained scope, hence
 * it is not thread-safe.
 * <p>
 * For developers: add methods to this type as needed; for less used features
 * you may rather use {@link #toString()} or {@link #sb()}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class ZString implements Serializable, Comparable<String> {

    /**
     * The regular expression pattern for alphabetical characters only.
     */
    private static final Pattern ALPHA_ONLY = Pattern.compile("^[A-Za-z]+$");
    
    /**
     * A constant for the empty string.
     */
    public static final ZString EMPTY = new ZString();

    /**
     * System dependent line separator.
     */
    public static final String EOL = System.getProperty("line.separator");

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 1523223665095586129L;

    /**
     * A string builder, {@code null} until needed. If this member is not
     * {@code null} this is the chief member of this instance.
     */
    private transient StringBuilder sb;

    /**
     * The string for this instance. Until {@link #sb} is needed and initialized
     * this is the chief member of this instance, but this member is obsolete if
     * {@link #sb}{@code != null}.
     */
    private transient String str;

    /**
     * Creates an instance of this type with an empty string; this type is never
     * {@code null}.
     */
    public ZString() {
        this.str = "";
    }

    /**
     * Creates an instance of this type for the specified string.
     * 
     * @param str a string
     */
    public ZString(String str) {
        this.str = str;
    }

    /**
     * Appends the argument to this instance.
     * 
     * @param b a boolean
     * @return {@code this}
     */
    public ZString app(boolean b) {
        sb().append(b);
        return this;
    }

    /**
     * Appends the argument to this instance.
     * 
     * @param c a char
     * @return {@code this}
     */
    public ZString app(char c) {
        sb().append(c);
        return this;
    }

    /**
     * Appends the argument to this instance.
     * 
     * @param d a double
     * @return {@code this}
     */
    public ZString app(double d) {
        sb().append(d);
        return this;
    }

    /**
     * Appends the argument to this instance.
     * 
     * @param f a float
     * @return {@code this}
     */
    public ZString app(float f) {
        sb().append(f);
        return this;
    }

    /**
     * Appends the argument to this instance.
     * 
     * @param i an int
     * @return {@code this}
     */
    public ZString app(int i) {
        sb().append(i);
        return this;
    }

    /**
     * Appends the argument to this instance.
     * 
     * @param l a long
     * @return {@code this}
     */
    public ZString app(long l) {
        sb().append(l);
        return this;
    }

    /**
     * Append the object's {@code toString()} representation.
     * 
     * @param o an object
     * @return {@code this}
     */
    public ZString app(Object o) {
        sb().append(o);
        return this;
    }

    /**
     * Append the object's {@code toString()} representation and a new line
     * character, a {@link ZString#EOL}.
     * 
     * @param s an object
     * @return {@code this}
     */
    public ZString appnl(String s) {
        sb().append(s).append(EOL);
        return this;
    }

    @Override
    public int compareTo(String o) {
        return toString().compareTo(o);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || sb != null
                ? sb.toString().equals(obj)
                : str.equals(obj);
    }

    @Override
    public int hashCode() {
        return sb != null
                ? sb.toString().hashCode()
                : str.hashCode();
    }

    /**
     * Returns {@code true} if this instance has length zero. This method does
     * not trim this instance before determining its length.
     * 
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return sb != null
                ? sb.length() <= 0
                : str.isEmpty();
    }

    /**
     * Returns the length of this instance.
     * 
     * @return a length
     */
    public int length() {
        return sb != null
                ? sb.length()
                : str.length();
    }

    /**
     * Append a new line (a {@link ZString#EOL}) to this instance. Compare
     * {@link #pnl()}.
     * 
     * @return {@code this}
     */
    public ZString nl() {
        sb().append(EOL);
        return this;
    }

    /**
     * Prepend a new line (a {@link ZString#EOL}) to this instance. Compare
     * {@link #nl()}.
     * 
     * @return {@code this}
     */
    public ZString pnl() {
        sb().insert(0, EOL);
        return this;
    }

    /**
     * Prepend the string to this instance. The boolean argument determines if a
     * white space is inserted as a delimiter between the string argument and
     * {@code this}.
     * 
     * @param s a string
     * @param space {@code true} for a white space
     * @return {@code this}
     */
    public ZString pre(String s, boolean space) {
        if (space && sb().length() > 0) sb().insert(0, ' ');
        sb().insert(0, s == null
                ? ""
                : s);
        return this;
    }

    /**
     * Restores the state of this instance from a stream.
     * 
     * @param ois a stream
     * @throws IOException if there is an error
     * @throws ClassNotFoundException if there is an error
     */
    private void readObject(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        str = (String) ois.readObject();
    }

    /**
     * Returns the string builder for this instance; if needed it is created.
     * Its content is based on the original string and modifying operations made
     * on this object.
     * 
     * @return a string builder
     */
    public StringBuilder sb() {
        if (sb == null) {
            if (this == EMPTY) throw new IllegalAccessError("is constant");
            sb = new StringBuilder(512);
            sb.append(str);
        }
        return sb;
    }

    /**
     * Returns a new string that is a substring of this instance.
     * 
     * @param start the begin index, inclusive
     * @return a string
     * @throws IndexOutOfBoundsException if the argument is invalid
     */
    public String sub(int start) {
        return sb != null
                ? sb.substring(start)
                : str.substring(start);
    }

    /**
     * Returns a new string that is a substring of this instance.
     * 
     * @param start the begin index, inclusive
     * @param end the end index, exclusive
     * @return a string
     * @throws IndexOutOfBoundsException if an argument is invalid
     */
    public String sub(int start, int end) {
        return sb != null
                ? sb.substring(start, end)
                : str.substring(start, end);
    }

    /**
     * Returns this instance trimmed and properly capitalized as defined by
     * {@link #tac(String)}.
     * 
     * @return this
     */
    public String tac() {
        return tac(toString());
    }

    @Override
    public String toString() {
        return sb != null
                ? sb.toString()
                : str;
    }

    /**
     * Returns a trimmed string from this instance; this instance remains as-is.
     * 
     * @return a trimmed string
     */
    public String trimmed() {
        return toString().trim();
    }

    /**
     * Serialize the state of this instance to an object stream.
     * 
     * @param oos a stream
     * @throws IOException if there is an error
     * @serialData the result from {@link #toString()}.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(toString());
    }

    /**
     * Returns a formatted string using the specified format string and
     * arguments as defined by {@link String#format(String, Object...)}.
     * 
     * @param f format string
     * @param os arguments
     * @return a formatted string
     * @throws IllegalFormatException if illegal syntax...
     */
    public static String fs(String f, Object... os) {
        return String.format(f, os);
    }

    /**
     * Returns an instance of this type as defined by
     * {@link #fs(String, Object...)}.
     * 
     * @param f format string
     * @param os arguments
     * @return a formatted instance of this type
     * @throws IllegalFormatException if illegal syntax...
     */
    public static ZString fz(String f, Object... os) {
        return new ZString(String.format(f, os));
    }

    /**
     * Returns {@code true} if the specified string contains only alphabetical
     * characters. This method returns {@code false} if a non-alphabetical
     * character is found. This implementation is using the regular expression
     * <nobr>{@code "^[A-Za-z]+$"}</nobr>.
     * 
     * @param string a string
     * @return {@code true} if the string contains only alphabetical characters
     */
    public static boolean isAlpha(String string) {
        return ALPHA_ONLY.matcher(string).find();
    }

    /**
     * Returns a string that is trimmed and properly capitalized, or {@code
     * null}. If the argument is {@code null} or empty it is returned as-is.
     * This implementation turns the initial character and every first character
     * after a white space and a dash (that is {@code char '-'}) to upper case,
     * all others to lower case.
     * 
     * @param s a string
     * @return a trimmed and capitalized string, {@code null}
     */
    public static String tac(String s) {
        if (s == null) return null;

        String str = s.trim().toLowerCase(Locale.ENGLISH);

        if (str.isEmpty()) return str;

        StringBuilder sb = new StringBuilder(str);
        boolean replace = true; // first character
        for (int i = 0; i < sb.length(); ++i) {
            char ch = sb.charAt(i);
            if (replace && ch != ' ') {
                String tmp = Character.toString(ch); // string handles locale
                sb.setCharAt(i, tmp.toUpperCase(Locale.ENGLISH).charAt(0));
                replace = false;
            }
            if (ch == ' ' || ch == '-') replace = true;
        }
        return sb.toString();
    }

    /**
     * Returns a trimmed string, or {@code null}. This method returns {@code
     * null} if the trimmed string is empty or if the argument is {@code null}.
     * 
     * @param s a string, or {@code null}
     * @return a trimmed string, or {@code null}
     */
    public static String ton(String s) {
        String str = s;
        if (str != null) {
            str = str.trim();
            if (str.isEmpty()) str = null;
        }
        return str;
    }

}
