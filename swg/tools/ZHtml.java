package swg.tools;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.net.URI;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import swg.SWGAide;

/**
 * A utility type that provides some methods related to HTML.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class ZHtml {

    /**
     * Opens a system web browser for the specified URL.
     * 
     * @param url the URL to open
     */
    public static void browser(final String url) {
        synchronized (ZHtml.class) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        URI u = new URI(url);
                        Desktop dd = Desktop.getDesktop();
                        dd.browse(u);
                    } catch (Throwable e) {
                        JOptionPane.showMessageDialog(SWGAide.frame(),
                                "Tried opening the browser, see log file",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        SWGAide.printDebug("post", 1,
                                "openWebBrowser:\n", e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * Helper method which returns a FontMetric object.
     * 
     * @param font a font for the metrics, or {@code null}
     * @return a font metric object
     */
    private static FontMetrics fontMetrics(Font font) {
        JLabel l = new JLabel();
        return l.getFontMetrics(font != null
                ? font
                : l.getFont());
    }

    /**
     * Determines the font width for the specified string and font. This method
     * returns -1 if it fails to execute.
     * 
     * @param str a string
     * @param font the font for the width
     * @return a width, or -1
     * @throws NullPointerException if the string is {@code null}
     */
    public static int fontWidth(String str, Font font) {
        FontMetrics fm = fontMetrics(font);
        if (fm != null) return fm.stringWidth(str);
        return -1;
    }

    /**
     * Returns a string that is normalized, or {@code null}. If the specified
     * argument contains specified HTML entities these are replaced by a
     * normalized representation. If the specified argument is {@code null} or
     * if the specified string does not contain anything to replace this method
     * does nothing but returns the specified argument as is.
     * <p>
     * <dl>
     * Current HTML strings and their replacements:
     * <dt>{@literal <br />} </td>
     * <dd>removed &mdash; this implementation does not replace the HTML string
     * with an EOL escape character</dd>
     * <dt>{@literal &lt;br /&gt;} </td>
     * <dd>removed &mdash; this implementation does not replace the HTML string
     * with an EOL escape character</dd>
     * </dl>
     * 
     * @param str a string
     * @return a normalized string, or {@code null}
     */
    public static String normalize(String str) {
        if (str == null) return str;

        String s = str;

        // the lines are already EOL-ended, just remove the <br />
        if (s.indexOf("<br />") >= 0)
            s = s.replaceAll("<br />", "");
        if (s.indexOf("&lt;br /&gt;") >= 0)
            s = s.replaceAll("&lt;br /&gt;", "");

        // fill in more if necessary

        return s;
    }

    /**
     * Returns a string that has all HTML {@literal <BR />} replaced with EOLs
     * (End-Of-Line characters); compare {@link #replaceEOL(String)}. This
     * method returns {@code null} if the argument is {@code null}.
     * 
     * @param string a string
     * @return the processed string, or {@code null}
     */
    public static String regainEOL(String string) {
        if (string == null) return null;

        return string.replace("<BR />", "\n");
    }

    /**
     * Returns a string that has all EOLs (End-Of-Line characters) replaced with
     * the HTML entity {@literal <BR />}; compare {@link #regainEOL}. This method
     * returns {@code null} if the argument is {@code null}.
     * 
     * @param string a string
     * @return the processed string, or {@code null}
     */
    public static String replaceEOL(String string) {
        if (string == null) return null;

        return string.replace("\n", "<BR />");
    }

    /**
     * This method returns the content of the specified text with no line
     * logically wider than the specified width. The lines in the returned text
     * is delimited by the HTML tag {@literal <br/>}.
     * <p>
     * If the specified filler is not {@code null} the filler is prepended to
     * each new line but not to the first line. The width of the filler is not
     * included in the specified width. The filler can for example be the HTML
     * white space {@literal &nbsp;} or something sensible in a HTML context.
     * <p>
     * For width it is the result from {@link FontMetrics#stringWidth(String)}
     * for the specified font that is used. If the system does not support font
     * metrics {@link #wrapToWidth(int, String, String...)} is invoked with the
     * specified values, except that 15% of width is used for {@code max}, a
     * value that corresponds to the number of characters for the specified
     * width using a default font.
     * <p>
     * If font is {@code null} the default font from {@link JLabel#getFont()} is
     * used for the font metric.
     * <p>
     * If the specified text contains HTML tags they are not treated specially,
     * but as any other text. If several texts are suggested they are merged to
     * one text with a white space between each element, the result is
     * processed, and finally one string is returned.
     * 
     * @param width the maximum advance for a line
     * @param filler filler that is prepended to each new line, or {@code null}
     * @param font font for the font metric, or {@code null}
     * @param text one or several texts to process
     * @return the text split over the necessary number of lines
     * @throws NullPointerException if a non-optional argument is {@code null}
     * @throws IllegalArgumentException if the width if is <tt>width &le; 0</tt>
     */
    public static String wrapToWidth(
            int width, String filler, Font font, String... text) {
        if (width <= 0)
            throw new IllegalArgumentException("Negative width: " + width);

        FontMetrics fm = fontMetrics(font);

        if (fm == null)
            return wrapToWidth((int) (width * .15), filler, text);

        StringBuilder sb = new StringBuilder(1024);
        for (String s : text)
            sb.append(s).append(' ');

        String txt = sb.toString();
        sb = new StringBuilder(txt.length() * 2);

        int wsp = fm.charWidth(' ');
        int len = 0;

        String[] split = txt.split(" ");
        for (String s : split) {
            if (len + fm.stringWidth(s) > width) {
                sb.append("<br/>");
                if (filler != null)
                    sb.append(filler);
                len = 0;
            } else {
                sb.append(' ');
                len += wsp;
            }
            sb.append(s);
            len += fm.stringWidth(s);
        }
        return sb.toString();
    }

    /**
     * This method returns the content of the specified text with no more than
     * the specified number of characters per line, HTML tags uncounted for. The
     * lines in the returned text is delimited by the HTML tag {@literal <br/>}.
     * <p>
     * If the specified filler is not {@code null} the filler is prepended to
     * each new line but not to the first line, no white space is added after
     * the filler. The width of the filler is not included in the specified
     * width. The filler can for example be the HTML white space {@literal
     * &nbsp;} or something sensible in a HTML context.
     * <p>
     * The specified width is the number of characters per line, the filler
     * excluded. This may cause a jagged output if the current font is not fixed
     * width and some lines have more wider characters than other lines do.
     * <p>
     * If the specified text contains HTML tags they are not treated specially,
     * but as any other text. If several texts are suggested they are merged to
     * one text with a white space between each element, the result is
     * processed, and finally one string is returned.
     * 
     * @param max the maximum number of characters per line
     * @param filler filler that is prepended to each new line, or {@code null}
     * @param text one or several texts to process
     * @return the text split over the necessary number of lines
     * @throws NullPointerException if a non-optional argument is {@code null}
     * @throws IllegalArgumentException if <tt>max &le; 0</tt>
     */
    public static String wrapToWidth(int max, String filler, String... text) {
        if (max <= 0)
            throw new IllegalArgumentException("Illegal max: " + max);

        StringBuilder sb = new StringBuilder(1024);
        for (String s : text)
            sb.append(s).append(' ');

        String txt = sb.toString();
        sb = new StringBuilder(txt.length() * 2);

        String[] split = txt.split(" ");
        int len = 0;
        for (String s : split) {
            if (len + 1 + s.length() > max) {
                sb.append("<br/>");
                if (filler != null)
                    sb.append(filler);
                len = 0;
            } else {
                sb.append(' ');
                ++len;
            }
            sb.append(s);
            len += s.length();
        }
        return sb.toString();
    }
}
