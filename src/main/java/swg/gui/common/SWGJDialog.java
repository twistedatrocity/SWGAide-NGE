package swg.gui.common;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import swg.SWGAide;
import swg.gui.SWGFrame;

/**
 * This a light weight wrapper for a {@link JDialog} that adds some features
 * that are used in dialogs in SWGAide. This implementation adds so it closes by
 * a click at the Escape key and it invokes the closing routine.
 * <p>
 * Implementation details: <br/>
 * This type adds a window listener for {@code windowClosing()} and a keyboard
 * action that invokes {@link #close()} which sub-classes must implement to
 * takes care of routines just before the dialog turns invisible; or as an empty
 * method. However, a sub-class must not register a keyboard action for {@code
 * KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)}. This type also handles
 * {@code #setVisible(false)}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public abstract class SWGJDialog extends JDialog {

    /**
     * The parent of this type, or {@code null}. This is not the owner of the
     * dialog as such but a parent to interact with somehow. Common parents are
     * a panel or SWGFrame.
     */
    protected final Component parent;

    /**
     * Creates a modal instance of this type using SWGFrame as as its owner and
     * {@link #parent}{@code == null}.
     */
    protected SWGJDialog() {
        this(null, null);
    }

    /**
     * Creates a modal instance of this type for the specified parent using
     * SWGFrame as its owner. Common parents are a panel or SWGFrame.
     * 
     * @param p parent, or {@code null}
     */
    protected SWGJDialog(JComponent p) {
        this(null, p);
    }

    /**
     * Creates a modal instance of this type with the specified title, using
     * SWGFrame as its owner.
     * 
     * @param t a title for the dialog, or {@code null}
     */
    protected SWGJDialog(String t) {
        this(t, null);
    }

    /**
     * Creates an instance of this type for the specified arguments as defined
     * by {@link JDialog#JDialog(java.awt.Frame, String, boolean)}, using
     * SWGFrame as its owner.
     * 
     * @param t a title, or {@code null}
     * @param b {@code true} for a modal dialog
     * @param p parent, or {@code null}
     */
    protected SWGJDialog(String t, boolean b, Component p) {
        this(SWGAide.frame(), t, b, p);
    }

    /**
     * Creates a modal instance of this type for the specified arguments as
     * defined by {@link JDialog#JDialog(java.awt.Frame, String, boolean)},
     * using SWGFrame as its owner.
     * 
     * @param t a title, or {@code null}
     * @param p parent, or {@code null}
     */
    protected SWGJDialog(String t, Component p) {
        this(t, true, p);
    }

    /**
     * Creates an instance of this type for the specified arguments as defined
     * by {@link JDialog#JDialog(java.awt.Frame, String, boolean)}.
     * 
     * @param f the owner for this dialog
     * @param t a title, or {@code null}
     * @param b {@code true} for a modal dialog
     * @param p a parent, or {@code null}
     */
    private SWGJDialog(SWGFrame f, String t, boolean b, Component p) {
        super(f, t, b);
        parent = p;

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        getRootPane().registerKeyboardAction(
                new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        close();
                        setVisible(false);
                    }
                }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Executes arbitrary routines just before closing this dialog, or does
     * nothing if this is an empty implementation. Typical routines are to save
     * location and size. This method does not have to invoke {@code
     * #setVisible(false)} as it is handled by this instance.
     */
    protected abstract void close();

    /**
     * Register this dialog as key stroke listener for F1. If the use types F1
     * when this dialog is visible the action listener invokes
     * {@link SWGHelp#display(URL)} with the specified page.
     * 
     * @param page a registered page
     */
    protected final void registerHelp(final URL page) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("F1"), "showHelp");
        getRootPane().getActionMap().put("showHelp", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SWGHelp.display(page);
            }
        });
    }
}
