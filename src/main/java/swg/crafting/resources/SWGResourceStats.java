package swg.crafting.resources;

import swg.SWGAide;
import swg.crafting.SWGValues;
import swg.crafting.Stat;
import swg.tools.ZString;

/**
 * This type models the values of the stats of a resource. Valid values are in
 * the range [0 to 1000] and simple validation is performed by this type but
 * caps is validated by the resource class respectively. Expected stats have
 * values in the range [1 1000] and non-expected or unknown values must be 0.
 * <p>
 * <b>Terminology:</b> this type is using a strict terminology, while in casual
 * speaking that is most often not the case:
 * <ul>
 * <li><b>Stat:</b> a specific stat of a resource, such as CD, OQ, etc.</li>
 * <li><b>Value:</b> a value of a specific stat for a resource, such as OQ=965
 * where the value is 965.</li>
 * <li><b>Expected stat:</b> a stat that is required by the particular resource
 * class, if a stat is expected one can also say <b>expected value</b> as that
 * is a corollary.</li>
 * </ul>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGResourceStats extends SWGValues {

    /**
     * A constant that denotes missing stats. It is an error to mutate the
     * values of this constant.
     */
    public static final SWGResourceStats BLANK = new SWGResourceStats();

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -2848749308010122703L;

    static {
        BLANK.values = ZEROES;
    }

    /**
     * Creates an instance for the the stats of a resource. All values of the
     * stats are initiated to zero.
     */
    public SWGResourceStats() {
        super();
    }

    /**
     * Creates an instance that contains the specified values for a resource.
     * This constructor validates the values to be in the range [0 1000]. Values
     * versus caps must be validated by the resource class of the resource.
     * 
     * @param values the values for a resource
     * @throws IllegalArgumentException if the argument is invalid
     */
    public SWGResourceStats(int[] values) {
        super(values);
    }

    /**
     * Returns the integer array of this instance. This method must only be
     * invoked by a caller that <i>never modifies </i> the content of the array;
     * this method remedies unwanted creation of instances and arrays.
     * 
     * @return the values of this instance
     */
    int[] safeValues() {
        return values;
    }
    
    /**
     * Creates and returns an instance that contains the values that are parsed
     * from the arguments, or {@code null}. This factory iterates over tokens of
     * the specified string and matches them against the <i>expected values </i>
     * of the resource class. If the string is empty {@code null} is returned.
     * <p>
     * The string is a sequence of integers <u>in in-game order</u>. Delimiters
     * are any of the characters {@code " ,;.:"}, inclusive white space but
     * quotes excluded, as defined by {@link String#split(String)}.
     * <p>
     * Examples:<br />
     * If the resource class is a water and the input string is "123, 456" it is
     * matched to DR=123 and OQ=456 as these are the expected stats of water.
     * 
     * @param str a sequence of integers in in-game order
     * @param rc a resource class constant
     * @return an instance of this type, or {@code null}
     * @throws IllegalArgumentException if there is an error, see its message
     * @throws NullPointerException if an argument is {@code null}
     */
    public static SWGResourceStats newInstance(String str, SWGResourceClass rc) {
        // tokenize
        String[] statsArray = str.split("[ ,.;:]+");
        if (statsArray.length <= 0)
            return null;

        ZString err = new ZString();
        SWGResourceStats stats = new SWGResourceStats();

        try {
            Stat[] gOrder = Stat.gameOrder();
            int stat = 0;

            // enumerate the stats from the input string
            for (int pid = 0; pid < statsArray.length; ++pid) {
                for (; stat < Integer.MAX_VALUE; ++stat) {

                    // possible error: IndexOutOfBoundsException if there are
                    // more tokens than expected stats
                    Stat s = gOrder[stat];

                    if (rc.has(s)) {

                        // possible error: NumberFormatException
                        int value = Integer.parseInt(statsArray[pid]);

                        // possible error:
                        // IllegalArgumentException if the value is invalid
                        stats.set(s, value);

                        // break inner loop and parse the next token
                        ++stat;
                        break;
                    }
                }
            }

            // possible error: all tokens are consumed but still expected stats
            for (; stat < Stat.COUNT; ++stat)
                if (rc.has(gOrder[stat]))
                    err.app(err.isEmpty()
                            ? "Missing stat "
                            : ", ").app(gOrder[stat].name());

        } catch (NumberFormatException e) {
            err.app("Number error ").app(e.getMessage());
        } catch (IllegalArgumentException e) {
            err.app(e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            err.app("More values than expected for ").app(rc.rcName());
        } catch (Exception e) {
            err.app(e.getClass().getSimpleName()).app(":").app(e.getMessage());
            SWGAide.printError("SWGResource:parseStats:", e);
        }
        if (!err.isEmpty()) throw new IllegalArgumentException(err.toString());
        // else
        return stats;
    }
}
