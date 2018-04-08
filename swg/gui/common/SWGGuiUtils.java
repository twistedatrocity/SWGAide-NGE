package swg.gui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import swg.gui.SWGFrame;
import swg.model.SWGCGalaxy;
import swg.model.SWGGalaxy;
import swg.model.SWGUniverse;

/**
 * This class is a common place-holder for GUI helper methods and objects that
 * are used as immutable constants. Some objects are not created until this type
 * is instantiated, hence {@link SWGFrame} creates an instance early.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGGuiUtils {

    /**
     * A convenience constant for the alert color, used to tint tabs at tabbed
     * panes when there is an alert.
     */
    public static final Color colorAlert = new Color(255, 204, 102);

    /**
     * A convenience constant for the background color for mouse-hover over a
     * listening category.
     */
    public final static Color colorCategory = new Color(255, 240, 255);

    /**
     * A convenience constant for the background color for mouse-hover over a
     * listening component.
     */
    public final static Color colorComponent = new Color(237, 246, 255);

    /**
     * A convenience constant for the warning color, used to tint tabs at tabbed
     * panes when there is a warning.
     */
    public static final Color colorDepleted = new Color(153, 0, 153);

    /**
     * A convenience constant for the background color for mouse-hover over a
     * listening component.
     */
    public final static Color colorItem = new Color(255, 255, 204);

    /**
     * A convenience constant for a background color for selected white cells.
     */
    public static final Color colorLightGray = new Color(221, 221, 221);

    /**
     * A convenience constant for the background color for resources which are
     * not possible to harvest in the worlds.
     */
    public final static Color colorNonHarvested = new Color(221, 221, 255);

    /**
     * A convenience constant for the background color for mouse-hover over a
     * listening resource.
     */
    public final static Color colorResource = new Color(241, 255, 227);

    /**
     * A convenience constant for thin line borders.
     */
    public final static Color colorThinBorder = new Color(204, 224, 255);

    /**
     * A convenience constant for the warning color, used to tint tabs at tabbed
     * panes when there is a warning.
     */
    public static final Color colorWarning = Color.PINK;

    /**
     * A convenience constant for the bold font.
     */
    private static Font fontBold;

    /**
     * A convenience constant for the italic font.
     */
    private static Font fontItalic;

    /**
     * A convenience constant for the plain font.
     */
    private static Font fontPlain;
    
    /**
     * A convenience constant for the font multiplier.
     */
    private static float fontMultiplier = 0f;
    
    /**
     * A convenience constant for the original font size before applying any scalars.
     */
    private static int defFontSize;

    /**
     * An array of colors which are used in SWGAide to tint GUI elements with
     * resource qualities. This array contains the three pairs of colors,
     * "background color" and "text color" (default for text is black):
     * <OL>
     * <LI>Fair quality &mdash; default limit is 800 and light green</LI>
     * <LI>Good quality &mdash; default limit is 900 and light yellow</LI>
     * <LI>Great quality &mdash; default limit is 960 and light red</LI>
     * </OL>
     * This array is indexed as follows.
     * <table>
     * <tr>
     * <th>Quality</th>
     * <th>Color</th>
     * <th>Text</th>
     * </tr>
     * <tr align=center>
     * <td>Fair</td>
     * <td>0</td>
     * <td>1</td>
     * </tr>
     * <tr align=center>
     * <td>Good</td>
     * <td>2</td>
     * <td>3</td>
     * </tr>
     * <tr align=center>
     * <td>Great</td>
     * <td>4</td>
     * <td>5</td>
     * </tr>
     * </table>
     */
    public final static Color[] statColors = new Color[6];

    /**
     * An array of the three limits for resource qualities which are used in
     * SWGAide to determine the resource qualities, for example when while
     * tinting GUI components. Each value is the lowest integer value for one of
     * the tiers described in {@link #statColors}; Fair, Good, and Great, at
     * index 0, 1, and 2 respectively. The values are in the range [0.0 1.0].
     */
    public final static double[] statLimits = { 0.8, 0.9, 0.96 };

    /**
     * Creates and initiates an instance of this GUI utility box. Once SWGAide's
     * persistent storage is initiated {@link #initiate()} must be invoked.
     * 
     * @param frame the frame for this application
     */
	public SWGGuiUtils(SWGFrame frame) {
		Font tf = new JLabel().getFont();
        defFontSize = tf.getSize();
    }

    /**
     * Initiates this instance; this method must not be invoked before SWGAide's
     * persistent storage is initiated and it should be invoked just once.
     */
    @SuppressWarnings({ "rawtypes", "unused" })
	public void initiate() {
        statColorLimitSet();
        fontMultiplier();
        
        if (fontMultiplier != 0f) {
            UIDefaults defaults = UIManager.getDefaults();
            int i = 0;
            for (Enumeration e = defaults.keys(); e.hasMoreElements(); i++) {
                Object key = e.nextElement();
                Object value = defaults.get(key);
                if (value instanceof Font) {
                    Font font = (Font) value;
                    int newSize = Math.round(font.getSize() * fontMultiplier);
                    if (value instanceof FontUIResource) {
                        defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), newSize));
                    } else {
                        defaults.put(key, new Font(font.getName(), font.getStyle(), newSize));
                    }
                }
            }
        }
    	
        Font f = new JLabel().getFont();
        fontBold = new Font(f.getName(), Font.BOLD, f.getSize());
        fontItalic = new Font(f.getName(), Font.ITALIC, f.getSize());
        fontPlain = new Font(f.getName(), Font.PLAIN, f.getSize());
    }

    /**
     * Returns a list of character names for the specified galaxy. The returned
     * list contains the first name, not the full name, of all characters from
     * all stations in the current universe, all are inhabitants at the same
     * galaxy. For the current universe this is a convenience method for
     * {@link SWGGalaxy#characterNames(SWGUniverse, SWGCGalaxy)}.
     * 
     * @param gxy a galaxy constant
     * @return a list of names of characters
     */
    public static List<String> characterNames(SWGCGalaxy gxy) {
        SWGUniverse univ = SWGFrame.getSelectedCharacter().galaxy().station().
                universe();
        return SWGGalaxy.characterNames(univ, gxy);
    }

    /**
     * Creates and returns a new Color slightly darker than the specified color.
     * This implementation multiplies each red, green, and blue component of the
     * argument with {@code factor} for the new color.
     * <p>
     * Usually a factor 0.9 gives a good result. If the specified factor is not
     * within interval (0 1.0) then 0.9 is used, endpoints excluded.
     * 
     * @param c a color
     * @param factor the darkening factor
     * @return a slightly darker color
     */
    public static Color colorDarker(Color c, float factor) {
        float f = factor;
        if (factor <= 0 || factor >= 1) f = 0.9f;
        int r = (int) Math.max(c.getRed() * f, 0);
        int g = (int) Math.max(c.getGreen() * f, 0);
        int b = (int) Math.max(c.getBlue() * f, 0);
        return new Color(r, g, b);
    }

    /**
     * Creates and returns a text edit dialog for the specified arguments. The
     * dialog is modal and returns all of the text when it is closed, or {@code
     * null} if the user canceled the dialog.
     * 
     * @param c the component to display the dialog over
     * @param title the title for the dialog
     * @param text the initial text for the dialog
     * @return the text of the dialog, or {@code null} if it was canceled
     */
    public static String dialogTextInput(Component c, String title, String text) {
        JOptionPane pane = new JOptionPane(null,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog diag = pane.createDialog(c, title);

        JTextArea ta = new JTextArea(6, 20);
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 5, 0, 5),
                BorderFactory.createLoweredBevelBorder()));
        ta.setText(text);
        diag.add(ta, BorderLayout.PAGE_START);

        diag.pack();
        diag.setVisible(true);

        // wait for user to OK or cancel
        Object o = pane.getValue();
        return (o instanceof Integer // 
        && ((Integer) o).intValue() == JOptionPane.OK_OPTION)
                ? ta.getText()
                : null;
    }

    /**
     * Helper method for GUI components that ensures <code>loc</code> is well
     * within the rectangle of the current screen, considering the size of
     * <code>dim</code>
     * 
     * @param loc the location to verify, the location object will be updated if
     *        necessary
     * @param dim the dimension of the GUI component to verify its location for
     * @return a location well within the rectangle of the current screen
     */
    public static Point ensureOnScreen(Point loc, Dimension dim) {
        Point l = new Point(loc);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        if (l.x + dim.width > screen.width)
            l.x = screen.width - dim.width;
        if (l.x < 0)
            l.x = 0;

        if (l.y + dim.height > screen.height)
            l.y = screen.height - dim.height;
        if (l.y < 0)
            l.y = 0;

        return l;
    }

    /**
     * Returns a constant for a bold dialog font. If this instance is not
     * initiated {@code null} is returned.
     * 
     * @return a bold font
     */
    public static Font fontBold() {
        return fontBold;
    }

    /**
     * Returns a constant for a italic dialog font. If this instance is not
     * initiated {@code null} is returned.
     * 
     * @return an italic font
     */
    public static Font fontItalic() {
        return fontItalic;
    }

    /**
     * Returns a constant for a plain dialog font. If this instance is not
     * initiated {@code null} is returned.
     * 
     * @return a plain font
     */
    public static Font fontPlain() {
        return fontPlain;
    }

    // /**
    // * @param args arguments
    // */
    // public static void main(String... args) {
    // JLabel l = new JLabel();
    // Font f = l.getFont();
    // System.err.println(fontWidth(l, "1000",
    // new Font(f.getName(), Font.PLAIN, f.getSize())));
    // }

    /**
     * Computes and return the font metric width for the specified text and
     * font. This is a convenience for {@code
     * comp.getFontMetrics(font).stringWidth(text)}.
     * 
     * @param comp a component to obtain font metrics from
     * @param text the text to compute its width
     * @param font the font for the width
     * @return a width
     */
    public static int fontWidth(Component comp, String text, Font font) {
        FontMetrics fm = comp.getFontMetrics(font);
        return fm.stringWidth(text);
    }
    
    /**
     * Computes and returns the font height for specified font.
     * 
     * @param comp
     * @param font
     * @return a height
     * @author Mr-Miagi
     */
    public static int fontHeight(Component comp, Font font) {
    	FontMetrics fm = comp.getFontMetrics(font);
    	return fm.getHeight();
    }

    /**
     * Computes padding based on font height.
     * 
     * @param h height
     * @return padding integer
     * @author Mr-Miagi
     */
    private static int fontHeightPadding (int h) {
    	int p = 1;
    	if (h <= 18) {
    		p = 2;
    	} else if (h <= 20) {
    		p = 4;
    	} else if (h > 20) {
    		p = 6;
    	}
    	int t = h + p;
    	return t;
    }
    
    /**
     * Sets Row Height for supplied JTable using both
     * {@link #fontHeight(Component, Font)} and {@link #fontHeightPadding(int)}
     * 
     * @param comp
     * @author Mr-Miagi
     */
    public static void setRowHeight (JTable comp) {
    	comp.setRowHeight( fontHeightPadding( fontHeight(comp, comp.getFont()) ) );
    }
    
    /**
     * Computes height and for font and returns height to use.
     * 
     * @param comp Component
     * @return int height with padding
     * @author Mr-Miagi
     */
    public static int getRowHeight (Component comp) {
    	return fontHeightPadding( fontHeight(comp, comp.getFont()) ) ;
    }
    
    /**
     * Computes dimension using font.
     * The button should already contain text before calling this.
     * 
     * @param comp JButton
     * @return Dimension
     * @author Mr-Miagi
     */
    public static void setButtonDim (JButton comp, int minwidth) {
    	int h = fontHeightPadding( fontHeight(comp, comp.getFont()) );
    	int fw = fontWidth(comp, comp.getText(), comp.getFont());
    	int w = Math.max(fw+h+18, minwidth);
    	Dimension dm = new Dimension(w, h);
    	comp.setMinimumSize(dm);
        comp.setPreferredSize(dm);
    }
    
    /**
     * Computes dimension based on font and supplied text
     * 
     * @param comp
     * @param text
     * @param minwidth
     */
    public static void setDim (Component comp, String text, int minwidth, int minheight, boolean max) {
   		int h = fontHeightPadding( fontHeight(comp, comp.getFont()) );
    	int fw = fontWidth(comp, text, comp.getFont());
    	int w = Math.max(fw+h+18, minwidth);
    	h = Math.max(h, minheight);
    	Dimension dm = new Dimension(w, h);
    	comp.setMinimumSize(dm);
    	if(max) {
    		comp.setMaximumSize(dm);
    	}
        comp.setPreferredSize(dm);
    }
    
    /**
     * Gets fontSizeParam from prefs or sets default to 100 if null.
     * 
     * @return fontSizeParam
     */
    public static String getFontSizeParam () {
    	String fp = (String) SWGFrame.getPrefsKeeper().get("fontSizeParam");
    	if (fp == null) {
    		fp = "100";
    	}
    	return fp;
    }
    
    /**
     * Writes new fontSizeParam value to prefs storage.
     * 
     * @param fp
     */
    public static void setFontSize (int fp) {
    	if (fp < 100 || fp > 280 ) return;
    	SWGFrame.getPrefsKeeper().add("fontSizeParam", Integer.toString(fp) );
    }
    
    /**
     * Simple method to return a multiplier from {@link #fontSizeParam}
     * 
     * @return multiplier
     */
    public static float fontMultiplier () {
    	if(fontMultiplier == 0f) {
    		fontMultiplier = Integer.parseInt(getFontSizeParam()) / 100.0f;
    	}
    	return fontMultiplier;
    }
    
    /**
     * Simple method to return default font size
     * 
     * @return multiplier
     */
    public static int defFontSize () {
    	return defFontSize;
    }
    
    /**
     * Creates and returns a button with the specified image and tool tip. If
     * the image does not exist the returned button read the alternative text.
     * The images are located in the folder ./images/
     * <p>
     * This implementation does not add any action listener to the button.
     * 
     * @param image the name of the image
     * @param toolTip a tool tip text
     * @param alt an alternative button text
     * @return a button
     */
    public static JButton makeButton(String image, String toolTip, String alt) {
        JButton b = new JButton();
        b.setToolTipText(toolTip);

        URL u = SWGGuiUtils.class.getResource(
                String.format("images/%s.gif", image));
        if (u != null)
            b.setIcon(new ImageIcon(u, alt));
        else
            b.setText(alt); // no image found

        b.setActionCommand(alt);

        return b;
    }

    /**
     * Helper method which returns a color obtained from SWGAide's persistent
     * storage, or the specified default color.
     * 
     * @param pref the preference key
     * @param defaultColor a default color
     * @return a color obtained from SWGAide's preference keeper, or the
     *         specified default color
     */
    private static Color statColor(String pref, Color defaultColor) {
    	Color def = (Color) SWGFrame.getPrefsKeeper().get(pref, defaultColor);
    	if (def == null) {
    		def = (Color) defaultColor;
    		SWGFrame.getPrefsKeeper().add(pref, defaultColor);
    	}
    	return def;
    }

    /**
     * Sets the static values for fair/good/great limits and colors to the
     * values stored in SWGAide's persistent storage. This method is invoked
     * when SWGAide starts, and also when the user has changed any of the values
     * for colors or limits and the new value is already saved to storage; hence
     * such a call updates the static constant arrays with the new colors and
     * limits which makes the change visible in SWGAide.
     */
    public static void statColorLimitSet() {
        statLimits[0] = statLimit("resourceLimitFair", 800);
        statColors[0] = statColor("resourceColorFair", new Color(213, 224, 166));
        statColors[1] = statColor("resourceColorFairText", Color.BLACK);

        statLimits[1] = statLimit("resourceLimitGood", 900);
        statColors[2] = statColor("resourceColorGood", new Color(255, 255, 121));
        statColors[3] = statColor("resourceColorGoodText", Color.BLACK);

        statLimits[2] = statLimit("resourceLimitGreat", 960);
        statColors[4] = statColor("resourceColorGreat", new Color(255, 185, 185));
        statColors[5] = statColor("resourceColorGreatText", Color.BLACK);
    }

    /**
     * Helper method which returns a value obtained from SWGAide's persistent
     * storage, or a value based on the specified default value. The returned
     * value is always a floating point value in the range [0.0 1.0].
     * 
     * @param pref the preference key
     * @param defaultValue a default value, an integer in the range [1 1000]
     * @return a value from storage or based on the default value
     * @throws IllegalArgumentException if the argument is not within range
     */
    private static double statLimit(String pref, int defaultValue) {
        if (defaultValue < 1 || defaultValue > 1000)
            throw new IllegalArgumentException("Invalid value: " + defaultValue);
        return ((Integer) SWGFrame.getPrefsKeeper().get(
                pref, Integer.valueOf(defaultValue))).intValue() / 1000.0;
    }

    /**
     * Sets a fix width for the specified table column to the specified value.
     * This method sets minimum, preferred, and maximum width for the column and
     * the user cannot later modify its width. If the width is zero or negative
     * this method does nothing. This method is a convenience for
     * {@link #tableColumnSetWidth(JTable, int, int, int, int)} with all widths
     * the same value.
     * 
     * @param table a table
     * @param col index for the column to set
     * @param width the static width for the column
     * @throws ArrayIndexOutOfBoundsException if column is out of bounds<br/>
     *         type of exception is determined by the table model
     * @throws NullPointerException if the component is {@code null}
     */
    public static void tableColumnFixWidth(JTable table, int col, int width) {
        tableColumnSetWidth(table, col, width, width, width);
    }

    /**
     * Sets the width for the specified table column to the specified values for
     * for minimum, preferred, and maximum width. If a width is zero or negative
     * it is ignored, if all values are ignored this method does nothing. This
     * method is a convenience for
     * {@link #tableColumnSetWidth(TableColumn, int, int, int)}.
     * 
     * @param table a table
     * @param col index for the column to set
     * @param min minimum width for the column
     * @param pref preferred width for the column
     * @param max maximum width for the column
     * @throws ArrayIndexOutOfBoundsException if column is out of bounds<br/>
     *         type of exception is determined by the table model
     * @throws NullPointerException if the component is {@code null}
     */
    public static void tableColumnSetWidth(
            JTable table, int col, int min, int pref, int max) {
        TableColumn tc = table.getColumnModel().getColumn(col);
        tableColumnSetWidth(tc, min, pref, max);
    }

    /**
     * Sets the width for the specified table column to the specified values for
     * for minimum, preferred, and maximum width. If a width is zero or negative
     * it is ignored, if all values are ignored this method does nothing.
     * 
     * @param tc a table column
     * @param min minimum width for the column
     * @param pref preferred width for the column
     * @param max maximum width for the column
     * @throws NullPointerException if the component is {@code null}
     */
    public static void tableColumnSetWidth(
            TableColumn tc, int min, int pref, int max) {
        if (min > 0) tc.setMinWidth(min);
        if (pref > 0) tc.setPreferredWidth(pref);
        if (max > 0) tc.setMaxWidth(max);
        if (min > 0 && min == pref && pref == max) tc.setResizable(false);
    }
    
    /**
     * Sets table column widths for the specified table. This method traverses
     * the specified table columns and sets their specified widths. If <tt>width
     * &le; 0</tt>, or if {@code last < first}, this method does nothing. The
     * columns {@code first} and {@code last} are inclusive, equal values
     * specify one column. If <tt>last &ge; {@link 
     * JTable#getColumnCount()}</tt> all existing columns from {@code first} are
     * properly handled.
     * <p>
     * Minimum and preferred widths are always set to {@code width}. Maximum
     * width is set to {@code width + slack} if <tt>slack &gt; 0</tt>, otherwise
     * to {@code width}.
     * 
     * @param table the table to service
     * @param first the first column to set
     * @param last the last column to set, inclusive
     * @param width preferred width for the columns
     * @param slack if {@code slack > 0} the value adds to {@code width}
     * @throws IllegalArgumentException if {@code first < 0}
     * @throws NullPointerException if the component is {@code null}
     */
    public static void tableSetColumnWidths(
            JTable table, int first, int last, int width, int slack) {
        if (first < 0)
            throw new IllegalArgumentException("Invalid first: " + first);

        int end = table.getColumnCount() - 1;
        if (width <= 0 || first > end || last < first) return;
        end = end < last
                ? end
                : last;
        int max = slack > 0
                ? width + slack
                : width;
        TableColumnModel tcm = table.getColumnModel();
        for (int c = first; c <= end; ++c) {
            TableColumn col = tcm.getColumn(c);
            tableColumnSetWidth(col, width, width, max);
        }
    }
	
	/**
     * Strips SWG-style color codes (\#FFFFFF) from the given input
     * @param input String to strip color codes from
     * @return Cleansed string
     */
    public static String stripColorCodes(String input) {
        /**
         * Regular expression that matches color-tags
         */
        Pattern colorRegexp = Pattern.compile("\\\\(#[a-zA-z0-9]{6})\\\\?");
        
        return colorRegexp.matcher(input).replaceAll("");
    }
}
