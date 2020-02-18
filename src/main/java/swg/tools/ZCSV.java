package swg.tools;

/**
 * A utility class that provides methods for reading and writing CSV files.
 * <p>
 * Currently this type is very simple:
 * <ul>
 * <li>it supports just one character as delimiter per instance</li>
 * <li>it does not support quote embraced strings that contain the specified
 * delimiter</li>
 * <li>quotes or double quotes are ignored</li>
 * </ul>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class ZCSV {

    // /**
    // * For testing purposes only.
    // *
    // * @param args
    // */
    // public static void main(String... args) {
    // // String s = "  ;";
    // // String[] r = parse(s, ';');
    // // System.err.println(r.length);
    // // System.err.println(Arrays.toString(r));
    //
    // Object[] vars = {Integer.valueOf(4711), "foo", "    ", Color.WHITE};
    // System.err.println(merge(';', vars));
    // }

    /**
     * Merges the specified arguments to one CSV string, delimited by the
     * specified character which must be a comma or a semicolon. For each
     * specified argument its method {@link #toString()} is invoked and the
     * returned value is trimmed.
     * <p>
     * If any of the arguments in themselves emits a string which contains the
     * delimiter the returned string superficially looks valid, however, it will
     * misbehave when it is split by a user. This implementation does not
     * provide any means for this issue.
     * 
     * @param delimiter
     *            the delimiter
     * @param args
     *            an array of objects
     * @return a CSV string
     * @throws IllegalArgumentException
     *             if the delimiter is not a comma or semicolon
     * @throws NullPointerException
     *             if any of the arguments is {@code null}
     */
    public static String merge(char delimiter, Object... args) {
        if (delimiter != ',' && delimiter != ';')
            throw new IllegalArgumentException(
                "Invalid delimiter: " + delimiter);

        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < args.length; ++i)
            sb.append(args[i].toString().trim()).append(delimiter);

        return sb.toString();
    }

    /**
     * Returns an array of strings, split from the specified text line using the
     * specified delimiter. See {@link #parse(String, char, boolean)}.
     * 
     * @param line
     *            a line to split
     * @param delimiter
     *            the CSV delimiter
     * @return an array of strings
     */
    public static String[] parse(String line, char delimiter) {
        return parse(line, delimiter, true);
    }

    /**
     * Returns an array of strings, split from the specified text line using the
     * specified delimiter. If a string in itself contains the delimiter it is
     * split. If the boolean argument is {@code true} all strings are trimmed
     * from white spaces and then also, if the string begins and ends with
     * quotes the pair removed. An any case, if a string is the empty string it
     * is retained as such.
     * <p>
     * Each element and each line is supposed to end with a delimiter. However,
     * this implementation retains any text after the last delimiter as an
     * element; also if the trimmed text is the empty string it is retained.
     * <p>
     * Examples:<br/>
     * {@code "foo;bar;baz" --> [foo, bar, baz]}, length 3<br/>
     * {@code "foo;bar;baz;" --> [foo, bar, baz]}, length 3<br/>
     * {@code ";;baz;   " --> [, , baz, ]}, length 4<br/>
     * {@code " ;" --> []}, length 1<br/>
     * {@code ";" --> []}, length 0<br/>
     * 
     * @param line a line to split
     * @param delimiter the CSV delimiter
     * @param trim {@code true} if all elements should be trimmed
     * @return an array of strings
     */
    public static String[] parse(String line, char delimiter, boolean trim) {
        String[] ret = line.split(Character.toString(delimiter));
        if (trim)
            for (int i = 0; i < ret.length; ++i) {
                String s = ret[i];
                if (s.startsWith("\"") && s.endsWith("\""))
                    s = s.substring(1, s.length());
                ret[i] = s.trim();
            }

        return ret;
    }
}
