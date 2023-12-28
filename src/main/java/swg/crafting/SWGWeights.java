package swg.crafting;

import java.util.Arrays;

import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.types.SWGMetal;

/**
 * This type models experimental weights used for crafting in SWG. It also
 * provides methods related to experimental weights, resources, and schematics.
 * <p>
 * Valid weights are in the range [0 100] and the sum of the weights must be
 * close to 100. The range [98 102] is considered valid for the sum, which is
 * due to the fact that in-game we find integer weights such as 17, 20, 33,
 * etc., and not all combinations sum up to 100.00.
 * <p>
 * <b>Terminology: </b>This type is using a strict terminology, while in casual
 * speaking that is most often not the case:
 * <ul>
 * <li><b>Stat: </b>a specific statistic of a resource, such as CD, OQ, etc.</li>
 * <li><b>Value</b> and <b>weight: </b>the value to apply for a specific stat
 * while weighing a resource, such as OQ=66 where the value is 66. In this
 * context weight and value has the same meaning.</li>
 * <li><b>Expected stat: </b>a stat that is required by the particular resource
 * class, one can also say <b>expected value</b> as that is a corollary.</li>
 * </ul>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 * @author John Thomas aka Chilastra.Oarun
 */
public final class SWGWeights extends SWGValues {

    /**
     * A constant for an experimental weight for LQ resources. All values are
     * set to 9 which is a valid sum for a weight object with 11 stats set.
     */
    public static final SWGWeights LQ_WEIGHTS =
            new SWGWeights(new int[] { 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9 });

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -8011338581375257491L;

    /**
     * Do not use the default constructor
     */
    @SuppressWarnings("unused")
    private SWGWeights() {
        // block creation of empty filters
    }

    /**
     * Creates an instance for experimental weights. The argument is validated,
     * see {@link SWGValues#validate(int[])}.
     * 
     * @param weights the experimental weights
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException if the argument is {@code null}
     */
    public SWGWeights(int[] weights) {
        aConstructorHelper(weights, true);
    }

    /**
     * Creates an instance for experimental weights. The size of the argument
     * must always be {@link Stat#COUNT} and optionally the sum of the weights
     * must be in the range [98 102] whereof none is outside [0 100].
     * <p>
     * <b>Note:</b> This constructor <u>must be avoided</u> but if it is used it
     * must be under the contract that the client shortly corrects and/or
     * adjusts and validates this instance. One such use is handling of user
     * typed values where the values are out of bonds while creating this
     * instance but it is adjusted before it is used.
     * <p>
     * Some corner cases exist where an instance from this constructor proves
     * valid but the total sum is out of bounds. For example, evenly distributed
     * weights, a resource with 8 expected caps will distribute them evenly with
     * the value 12 for each expected weight, which adds to 96. However, to my
     * understanding all ordinary schematics meet the regular range.
     * 
     * @param weights the experimental weights
     * @param validate {@code true} if the argument should be validated
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException if the argument is {@code null}
     */
    public SWGWeights(int[] weights, boolean validate) {
        aConstructorHelper(weights, validate);
    }

    /**
     * Creates an instance for experimental weights. The argument is validated.
     * 
     * @param stat a stat to set
     * @param value the value for the stat, in the range [0 1000]
     * @throws IllegalArgumentException if validation of the argument fails
     */
    public SWGWeights(Stat stat, int value) {
        super();
        set(stat, value);
        aConstructorHelper(values, true);
    }

    /**
     * Helper method for the constructors of this type. See
     * {@link #SWGWeights(int[], boolean)} for details on validation.
     * 
     * @param weights the payload of the created instance
     * @param validateSum {@code true} if the total sum should be validated
     * @throws IllegalArgumentException if validation fails
     * @throws NullPointerException if the argument is {@code null}
     */
    private void aConstructorHelper(int[] weights, boolean validateSum) {
        if (validateSum)
            validate(weights);
        if (validateSum && !isValid(sum(weights)))
            throw new IllegalArgumentException(String.format(
                    "Invalid sum: %s of %s",
                    Integer.toString(sum(weights)), Arrays.toString(weights)));

        values = Arrays.copyOf(weights, weights.length);
    }

    /**
     * Helper method to #getWeight(...) which returns a decimal value with which
     * to adjust for cap a value of a resource stat, or 1. The return value is
     * {@code 1000.0/cap}, or 1.0 if the argument is 0.
     * 
     * @param cap the upper cap for a stat
     * @return the decimal value with which to adjust a value, or 1.0
     * @throws IllegalArgumentException if cap is outside [0 1000]
     */
    private double adjCap(int cap) {
        if (cap < 0 || cap > 1000)
            throw new IllegalArgumentException("Invalid cap: " + cap);
        return cap > 0
                ? (1000.0 / cap)
                : 1.0;
    }

    /**
     * Helper method to #getWeight(...) which returns the value after it is
     * adjusted based on {@code zeroIsMax} and {@code weight}.
     * <p>
     * The return value is one of {@code value}, {@code weight*10}, or 0:
     * <ul>
     * <li>if {@code value > 0}: {@code value} itself</li>
     * <li>else if {@code zeroIsMax}: the product {@code weight * 10}</li>
     * <li>else: the value 0</li>
     * </ul>
     * The rationale for {@code weight * 10} is that weights are percent values
     * in the range [0 100] but we want to return a fake <i>resource value </i>
     * in the range [1 1000].
     * 
     * @param value the value to possibly adjust
     * @param zeroIsMax {@code true} if zero values should fake a value
     * @param weight the weight that pertains to a stat
     * @return the value itself, or one of {@code weight*10} or 0
     */
    private double adjIfZero(int value, boolean zeroIsMax, double weight) {
        return value == 0 && zeroIsMax
                ? weight * 10.0
                : value;
    }

    /**
     * Adjusts this instance to a sum close to 100. This method overwrites the
     * non-zero weights of this instance with the adjusted values.
     * <p>
     * Because weights are integers, determined by the number of weights and
     * their values it is not always possible to achieve a total sum in the
     * range [98 102].
     */
    public void adjust() {
        double adj = (100.0 / sum());
        for (Stat s : Stat.values())
            values[s.i] = (int) Math.round(values[s.i] * adj);
    }

    /**
     * Determines if the argument has at least one <i>expected stat </i> in
     * union with this instance. That is, for any stat <i>s</i>: {@code
     * this.value(s) > 0 && resource.has(s)} this method returns {@code true}.
     * <p>
     * If a resource is reported without an expected stat, then this method
     * cannot make up for that error.
     * 
     * @param res the resource to evaluate
     * @return {@code true} if at least one <i>expected stat</i> is found
     * @throws NullPointerException if the argument is {@code null}
     */
    public boolean hasMinimumOneStat(SWGResource res) {
        for (Stat s : Stat.values())
            if (values[s.i] > 0 && res.rc().has(s))
                return true;

        return false;
    }

    /**
     * Validates the the total sum of the weights of this instance, that it is
     * within the range [98 102]. Some corner cases exist where the sum is
     * slightly out of bounds, see the class comment.
     * 
     * @return {@code true} if the sum is within [98 102]
     */
    public boolean isValid() {
        return isValid(values);
    }

    /**
     * Returns the rate for the specified resource as a decimal value in the
     * range [0.0 1000.0]. The rate is computed from the values of the resource
     * weighed by this instance while considering upper caps from the specified
     * resource class.
     * <p>
     * More formally, the returned rate is the sum of a sequence of computed
     * values. For all all non-zero weights of this instance, the resource value
     * that pertains to that stat is adjusted by by {@link #adjCap(int)} and
     * {@link #adjIfZero(int, boolean, double)}. Thus, for each
     * <i>w<sub>i</sub>&nbsp;</i> {@literal > 0}:
     * 
     * <pre>
     *     w<sub><i>x</i></sub>/100 * value<sub><i>x</i></sub> * 1000/cap<sub><i>x</i></sub> +
     *     w<sub><i>y</i></sub>/100 * value<sub><i>y</i></sub> * 1000/cap<sub><i>y</i></sub> +
     *     ...</pre>
     * Note: {@code w/100} is there to obtain the percent value, {@code
     * 1000/cap} is there to obtain an adjustment factor that is equal to or
     * greater than 1.0.
     * <p>
     * If a non-zero weight corresponds to a zero resource-value {@code
     * zeroIsMax} determines if rather the upper cap should replace the missing
     * value. Remember, in-game it suffices if just <i>one </i> resource meet a
     * stat called for by the schematic, but this method rates just one resource
     * of several resource slots.
     * <p>
     * If the resource class argument is {@code null} the resource class of the
     * resource determines the upper caps; effectively this means that the
     * adjustment factor for caps is 1.0.
     * <p>
     * If the resource <b>has no</b> <i>expected value </i> in union with this
     * instance the rate is 0, see {@link #hasMinimumOneStat(SWGResource)}.
     * 
     * @param res a resource to rate by this instance
     * @param caps the resource class for upper caps, or {@code null}
     * @param zeroIsMax {@code true} if zero-values are considered
     * @param useJTLcaps {@code true} if this method should adjust for server-side JTL resource rules
     * @return the resource rate in the range [0.0 1000.0]
     * @throws IllegalArgumentException if the resource class does not match the
     *         specified resource
     * @throws NullPointerException if the resource is {@code null}
     */
    public double rate(SWGResource res, SWGResourceClass caps, boolean zeroIsMax, boolean useJTLcaps) {
        if (caps != null && !res.rc().isSub(caps.getClass()))
            throw new IllegalArgumentException(String.format(
                    "%s is not subclass of %s",
                    res.rc().rcName(), caps.rcName()));

        if (!hasMinimumOneStat(res))
            return 0.0;

        if (useJTLcaps && caps.isAffectedByJTLcap())
        	caps = SWGMetal.getInstance();
        
        // allocating a local copy of these weights in case we have to remove
        // one or more to redistribute without destroying this instance
        double[] weights = new double[Stat.COUNT];
        for (Stat s : Stat.values())
            weights[s.i] = values[s.i];

        int[] rValues = res.stats().values;

        // for each weight, if a rValue does not exist we need to remove the
        // weight and later redistribute that weight evenly to the remaining
        // weights (this logic does not care for flawed resource data when an
        // expected value is missing)
        double weightSA = 0.0;
        if (zeroIsMax)
            for (Stat s : Stat.values()) {
                if (weights[s.i] > 0 && rValues[s.i] <= 0)
                    weights[s.i] = 0;

                weightSA += weights[s.i];
            }
        else
            weightSA = 100.0;

        weightSA = 100 / weightSA; // make Sum an Adjustor

        double resWeight = 0.0;
        for (Stat s : Stat.values()) {
            if (weights[s.i] > 0) {
                double weight = weights[s.i];

                // adjust the weights with the similar ratio as the remaining
                // weights
                weight = weight * weightSA;

                // the resource value adjusted for the option zeroIsMax
                double value = adjIfZero(rValues[s.i], zeroIsMax, weight);

                // the complete formula to compute a weighed value with is
                // sum += value * (1000 / cap) * weight / 100

                // if possible: 1000/cap which is >= 1.00
                double multiplier = caps != null
                        ? adjCap(caps.max(s))
                        : 1.0;

                resWeight += Math.min(1000.0, value * multiplier)
                        * weight / 100.0;
            }
        }
        return resWeight;
    }

    @Override
    protected int validate(int value) {
        if (value < 0 || value > 100)
            throw new IllegalArgumentException(Integer.toString(value));
        return value;
    }

    /**
     * Determines if the argument is in the valid range [98 102].
     * 
     * @param sum the sum to verify
     * @return {@code true} if the argument is within the range [98 102]
     */
    public static boolean isValid(double sum) {
        return (sum >= 98 && sum <= 102);
    }

    /**
     * Determines if the argument is valid. The array length must be
     * {@link Stat#COUNT}, all weights must be in the range [0 100], and the
     * total sum must be in the range [98 102]. Some corner cases exist where
     * the sum is slightly out of bounds, see the class comment.
     * 
     * @param values the array to validate
     * @return {@code true} if the argument is valid
     * @throws NullPointerException if the argument is {@code null}
     */
    public static boolean isValid(int[] values) {
        if (values.length != Stat.COUNT)
            return false;
        
        int checkSum = 0;
        for (int w : values) {
            if (w < 0 || w > 100)
                return false;
            checkSum += w;
        }
        return isValid(checkSum);
    }

    /**
     * Returns the "wider" of the two arguments, or {@code null}. The definition
     * of "wider" is: the two share at least one weight, the wider has one or
     * several weights where the other has none, but the other has none which
     * the wider has not. The actual values are ignored, just the presence of
     * weights are considered. If the smaller has one or more stats which the
     * wider does not have, or if any of the two is {@code null}, this method
     * returns {@code null}.
     * <p>
     * These mock examples are OK:
     * 
     * <pre>
     * w1 =  33   0  33  33   <== the wider
     * w2 =   0   0 100   0
     * 
     * w1 =  33   0  33  33
     * w2 =  25  25  25  25   <== the wider </pre>
     * But these mock examples are not OK and this method returns {@code null}:
     * 
     * <pre>
     * w1 =  33   0  33  33
     * w2 =   0 100   0   0   === none in common
     * 
     * w1 =  33   0  33  33   <== could be the wider but...
     * w2 =   0  50  50   0   <== one extra weight </pre>
     * 
     * @param w1 a weight instance
     * @param w2 a weight instance
     * @return the wider of the two, or {@code null}
     */
    public static SWGWeights wider(SWGWeights w1, SWGWeights w2) {
        if (w1 == null || w2 == null) return null;
        if (w1.equals(w2)) return w1; // handles equal count

        SWGWeights ww1 = w1;
        SWGWeights ww2 = w2;
        // find the wider of the two
        if (w2.nonZero() > w1.nonZero()) {
            ww1 = w2;
            ww2 = w1;
        }

        boolean shared = false;
        for (Stat s : Stat.values()) {
            if (ww2.values[s.i] > 0) {
                if (ww1.values[s.i] <= 0) return null; // smaller has one extra
                // else
                shared = true; // one on common
            }
        }

        return shared
                ? ww1
                : null;
    }
}
