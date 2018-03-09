package swg.tools;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * This type provides some convenience methods for stuff that does not fit
 * elsewhere.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class ZStuff {

    /**
     * An instance of {@link Calendar}, its time is undefined and must be set
     * before each use.
     */
    private static final Calendar CAL = Calendar.getInstance();

    /**
     * A date formatter for {@link DateFormat#SHORT}.
     */
    private static final DateFormat DATE_SHORT =
            DateFormat.getDateInstance(DateFormat.SHORT);

    /**
     * A date/time formatter with both parts from {@link DateFormat#SHORT}.
     */
    private static final DateFormat DATE_TIME_SHORT =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    /**
     * A time formatter for {@link DateFormat#SHORT}.
     */
    private static final DateFormat TIME_SHORT =
            DateFormat.getTimeInstance(DateFormat.SHORT);
    
    /**
     * A string for OS
     */
    private static String OS = null;

    /**
     * Returns the current date formatted per the user's locale and
     * {@link DateFormat#SHORT}. This if for example "M/d/yy" in the US locale.
     * If {@code weekName} this method also appends the name of the week day.
     * This is an example in the US locale: "12/24/10 --- Friday"
     * 
     * @param weekName {@code true} to append the week day
     * @return today's date
     */
    public static String dateString(boolean weekName) {
        synchronized (CAL) {
            String ret = dateString(System.currentTimeMillis());
            if (weekName)
                return new ZString(ret).app(" --- ").
                        app(CAL.getDisplayName(
                                Calendar.DAY_OF_WEEK, Calendar.LONG,
                                Locale.getDefault())).toString();
            // else
            return ret;
        }
    }

    /**
     * Returns the specified date formatted per the user's locale and
     * {@link DateFormat#SHORT}. This if for example "M/d/yy" in the US locale:
     * "12/24/10". The time is specified in milliseconds since January 1, 1970,
     * UTC.
     * 
     * @param time milliseconds since UTC
     * @return a date string
     */
    public static String dateString(long time) {
        synchronized (CAL) {
            CAL.setTimeInMillis(time);
            return DATE_SHORT.format(CAL.getTime());
        }
    }

    /**
     * Returns the current date-and-time formatted per the user's locale and
     * {@link DateFormat#SHORT}. This if for example "M/d/yy h:mm a" in the US
     * locale.
     * 
     * @return current time
     * @see #dateString(boolean)
     */
    public static String dateTimeString() {
        return dateTimeString(System.currentTimeMillis());
    }

    /**
     * Returns the specified value formatted per the user's locale and
     * {@link DateFormat#SHORT}. This if for example "M/d/yy h:mm a" in the US
     * locale: "12/24/10". The time is specified in milliseconds since January
     * 1, 1970, UTC.
     * 
     * @param time milliseconds since UTC
     * @return a date/time string
     */
    public static String dateTimeString(long time) {
        synchronized (CAL) {
            CAL.setTimeInMillis(time);
            return DATE_TIME_SHORT.format(CAL.getTime());
        }
    }

    /**
     * Computes and returns the Levenshtein distance for the two strings. This
     * distance is the minimum number of edits needed to transform one string
     * into the other using only insertion, deletion, or substitution of a
     * single character. Hence this distance denotes equality between two
     * strings, the order of the two strings is ignored.
     * <p>
     * 
     * @param s1 a string
     * @param s2 another string
     * @param ignoreCase {@code true} for case insensitivity
     * @return the Levenshtein distance
     * @throws NullPointerException if an argument is {@code null}
     */
    public static int levenshteinDistance(
            String s1, String s2, boolean ignoreCase) {

        // this implementation is based on the public domain code at
        // http://www.merriampark.com/ld.htm#JAVA

        String s = ignoreCase
                ? s1.toLowerCase()
                : s1;
        String t = ignoreCase
                ? s2.toLowerCase()
                : s2;
        if (s.equals(t)) return 0;

        int slen = s.length();
        int tlen = t.length();
        if (slen == 0) return tlen;
        if (tlen == 0) return slen;

        int m[][] = new int[slen + 1][tlen + 1];

        for (int i = 0; i <= slen; i++)
            m[i][0] = i;
        for (int j = 0; j <= tlen; j++)
            m[0][j] = j;

        for (int i = 1; i <= slen; i++) {
            char sch = s.charAt(i - 1);
            for (int j = 1; j <= tlen; j++) {
                m[i][j] = Math.min(m[i - 1][j] + 1, Math.min(
                        m[i][j - 1] + 1,
                        m[i - 1][j - 1] + (sch == t.charAt(j - 1)
                                ? 0
                                : 1)));
            }
        }
        return m[slen][tlen];
    }

    /**
     * Returns the current time formatted per the user's locale and
     * {@link DateFormat#SHORT}. This if for example "h:mm a" in the US locale.
     * 
     * @return current time
     * @see #dateString(boolean)
     */
    public static String timeString() {
        return timeString(System.currentTimeMillis());
    }

    /**
     * Returns the specified time formatted per the user's locale and
     * {@link DateFormat#SHORT}. This if for example "h:mm a" in the US locale.
     * 
     * @param time milliseconds since UTC
     * @return a time string
     */
    public static String timeString(long time) {
        synchronized (CAL) {
            CAL.setTimeInMillis(time);
            return TIME_SHORT.format(CAL.getTime());
        }
    }
    
    /**
     * Returns OS name, caches result into string.
     * @return an OS string
     */
    public static String getOsName() {
       if(OS == null) { OS = System.getProperty("os.name");
       }
       return OS;
    }

    /**
     * Simple check to see if we are on windows
     * @return boolean
     */
    public static boolean isWindows() {
       return getOsName().startsWith("Windows");
    }
}
