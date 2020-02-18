package swg.gui.common;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.net.URL;
import java.util.Stack;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.tools.SimplePrefsKeeper;

/**
 * This displays help pages for SWGAide's windows and panels, or the general
 * help page if no particular page is set.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGHelp extends SWGJDialog {

    /**
     * A reference to the only instance of this viewer.
     */
    private static SWGHelp THIS;

    /**
     * A stack of help pages to display.
     */
    private final Stack<URL> pageStack;

    /**
     * The editor pane to display pages at.
     */
    private final JEditorPane textPane;

    /**
     * Creates an instance of this type.
     * 
     * @param frame the frame for SWGAide
     */
    public SWGHelp(SWGFrame frame) {
        super("Help", false, frame);
        THIS = this;

        pageStack = new Stack<URL>();
        pageStack.add(makeDefaultURL());
        
        textPane = new JEditorPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        textPane.setEditorKit(kit);
        StyleSheet styleSheet = kit.getStyleSheet();
        Style style = styleSheet.getStyle("body");
        StyleConstants.setFontSize(style, SWGGuiUtils.fontPlain().getSize());
        StyleConstants.setBackground(style, UIManager.getColor("TextArea.background"));
        StyleConstants.setForeground(style, UIManager.getColor("TextArea.foreground"));
        style = styleSheet.getStyle("h1");
        StyleConstants.setFontSize(style, Math.round(StyleConstants.getFontSize(style)*SWGGuiUtils.fontMultiplier()) );
        style = styleSheet.getStyle("h2");
        StyleConstants.setFontSize(style, Math.round(StyleConstants.getFontSize(style)*SWGGuiUtils.fontMultiplier()) );
        style = styleSheet.getStyle("h3");
        StyleConstants.setFontSize(style, Math.round(StyleConstants.getFontSize(style)*SWGGuiUtils.fontMultiplier()) );
        style = styleSheet.getStyle("h4");
        StyleConstants.setFontSize(style, Math.round(StyleConstants.getFontSize(style)*SWGGuiUtils.fontMultiplier()) );

        JScrollPane jsp = new JScrollPane(textPane);
        jsp.setMinimumSize(new Dimension(150, 120));
        jsp.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(jsp);
    }

    @Override
    protected void close() {
        SWGFrame.getPrefsKeeper().add("helpScreenLocation", getLocation());
        SWGFrame.getPrefsKeeper().add("helpScreenSize", getSize());
    }

    /**
     * Returns an URL for the default help page. If no page is found this method
     * returns {@code null}.
     * 
     * @return an URL, or @{code null}
     */
    private URL makeDefaultURL() {
        return SWGAide.class.getResource("docs/help_general_en.html");
    }

    /**
     * Returns the topmost URL from the stack.
     * 
     * @return an URL
     */
    private URL url() {
        return pageStack.peek();
    }

    /**
     * Makes this viewer visible and reading the topmost URL from the stack. A
     * client must first push a page on the stack, see {@link #push(URL)}, also
     * compare {@link #display(URL)}.
     */
    public static void display() {
        display(THIS.url());
    }

    /**
     * Makes this viewer visible and reading the specified URL.
     * 
     * @param index an index
     * @throws ArrayIndexOutOfBoundsException if the index is invalid
     */
    public static void display(int index) {
        display(THIS.pageStack.get(index));
    }

    /**
     * Makes this viewer visible and reading the specified URL. This method
     * displays the URL without adding it on the stack. If there is an error it
     * is intercepted and both written to SWGAide's log file and it is displayed
     * at this dialog.
     * 
     * @param page an URL for a page to display
     */
    public static void display(URL page) {
        try {
            THIS.textPane.setPage(page);
        } catch (IOException e) {
            SWGAide.printError("SWGHelp:show:", e);
            THIS.textPane.setText(e.getMessage());
        }
        setAppearance();
    }

    /**
     * Pushes the specified URL on the stack of help pages so it becomes the
     * topmost item. When a client lose focus it must also removes the URL, see
     * {@link #remove(URL)}.
     * 
     * @param page an URL to push on the stack
     */
    public static void push(URL page) {
        THIS.pageStack.push(page);
    }

    /**
     * Removes the specified URL from the stack of help pages. This method
     * iterates over the stack and removes all occurrences of the URL. If the
     * stack does not contain the URL this method does nothing.
     * 
     * @param page a page to remove from the stack
     */
    public static void remove(URL page) {
        while (THIS.pageStack.remove(page))
            continue;
    }

    /**
     * Updates this viewer's size and its location on screen.
     */
    private static void setAppearance() {
        SimplePrefsKeeper pk = SWGFrame.getPrefsKeeper();
        Dimension dim = (Dimension) pk.get("helpScreenSize");
        Point loc = (Point) pk.get("helpScreenLocation");

        if (dim == null || loc == null) {
            dim = THIS.parent.getSize();
            int w = dim.width > 800
                    ? 800
                    : dim.width - 50;
            int h = dim.height > 600
                    ? 600
                    : dim.height - 50;
            dim = new Dimension(w, h);

            loc = THIS.parent.getLocationOnScreen();
            loc.x += ((dim.width - w) >> 1);
            loc.y += ((dim.height - h) >> 1);
        }

        THIS.setSize(dim);
        THIS.setPreferredSize(dim);

        loc = SWGGuiUtils.ensureOnScreen(loc, THIS.getSize());
        THIS.setLocation(loc);

        THIS.setVisible(true);
    }
}
