package swg.gui.common;

import java.awt.Color;

import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import swg.tools.ZNumber;

/**
 * This type is a table cell renderer for resource stats and related functions.
 * This type supports values of the types {@link Double}, {@link Integer}, and
 * {@link Long} and {@link #mySetValue(Object)} formats them accordingly.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGResourceStatRenderer extends SWGDecoratedTableCellRenderer {
	
	private static MetalTheme theme = MetalLookAndFeel.getCurrentTheme();
	private static boolean Dark = (theme.getName().contains("Dark")) ? true : false;
    /**
     * Creates an instance of this type; see class comment for details.
     * 
     * @param tableModel a table model for decorated tables
     */
    public SWGResourceStatRenderer(DecoratedTableModel tableModel) {
        super(tableModel);
        
    }

    @Override
    protected void mySetValue(Object value) {
        setValue(value instanceof Number
                ? ZNumber.asText((Number) value)
                : value);
    }

    /**
     * Helper method which returns a background color based on the argument.
     * 
     * @param quotient a quotient for value / cap, in the interval [0.0 1000.0]
     * @return a background color
     */
    public static Color getStatBackGround(double quotient) {
    	if(Dark) {
    		return null;
    	}
        double q = quotient > 1
                ? quotient / 1000.
                : quotient;

        if (q < SWGGuiUtils.statLimits[0])
            return null;

        if (q < SWGGuiUtils.statLimits[1])
            return SWGGuiUtils.statColors[0];

        if (q < SWGGuiUtils.statLimits[2])
            return SWGGuiUtils.statColors[2];

        return SWGGuiUtils.statColors[4];
    }

    /**
     * Returns a background color for a resource stat. If {@code cap <= 0} this
     * method returns Color.WHITE. Otherwise this method returns a color from
     * {@link SWGGuiUtils#statColors} which is determined by the quotient
     * {@code value / cap} relative {@link SWGGuiUtils#statLimits}; if the
     * quotient is lower that "fair" this method returns Color.WHITE.
     * 
     * @param value the integer value for a stat
     * @param cap the cap for a stat, use {@code cap <= 0} for <i>non-expected
     *        stats</i>, such as Flora does not have CD
     * @return a background color
     */
    public static Color getStatBackGround(int value, int cap) {
        if (cap <= 0) return null;
        if(Dark) {
    		return null;
    	}

        double rel = value / (double) cap;
        rel = Math.min(1.0, rel); // pander for JTL
        return getStatBackGround(rel);
    }

    /**
     * Helper method which returns a foreground color based on the argument.
     * 
     * @param quotient a quotient for value / cap, in the interval [0.0 1000.0]
     * @return a foreground color
     */
    public static Color getStatForeground(double quotient) {
    	if(Dark) {
    		return getRealColor(quotient);
    	}
        double q = quotient > 1
                ? quotient / 1000.
                : quotient;

        if (q < SWGGuiUtils.statLimits[0])
            return null;

        if (q < SWGGuiUtils.statLimits[1])
            return SWGGuiUtils.statColors[1];

        if (q < SWGGuiUtils.statLimits[2])
            return SWGGuiUtils.statColors[3];

        return SWGGuiUtils.statColors[5];
    }

    /**
     * Returns a foreground color for a resource stat. If {@code cap <= 0} this
     * method returns Color.BLACK. Otherwise this method returns a color from
     * {@link SWGGuiUtils#statColors} which is determined by the quotient
     * {@code value / cap} relative {@link SWGGuiUtils#statLimits}; if the
     * quotient is lower that "fair" this method returns Color.BLACK.
     * 
     * @param value the integer value for a stat
     * @param cap the cap for a stat, use {@code cap <= 0} for <i>non-expected
     *        stats</i>, such as Flora does not have CD
     * @return a foreground color
     */
    public static Color getStatForeground(int value, int cap) {
        if (cap <= 0) return null;

        double rel = value / (double) cap;
        return getStatForeground(rel);
    }
    
    public static Color getRealColor (double quotient) {
    	double q = quotient > 1
                ? quotient / 1000.
                : quotient;

        if (q < SWGGuiUtils.statLimits[0])
            return null;

        if (q < SWGGuiUtils.statLimits[1])
            return SWGGuiUtils.statColors[0];

        if (q < SWGGuiUtils.statLimits[2])
            return SWGGuiUtils.statColors[2];

        return SWGGuiUtils.statColors[4];
    }
}
