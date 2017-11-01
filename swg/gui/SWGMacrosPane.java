package swg.gui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import swg.model.SWGMacros;

/**
 * This is a component for displaying macros files of the stations in an SWG
 * universe.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 * @see SWGMacros
 */
@SuppressWarnings("serial")
public class SWGMacrosPane extends JTextArea {

    /**
     * The frame of the application for this component
     */
    final SWGFrame frame;

    /**
     * A list of menu items for the "Edit" menu
     */
    ArrayList<JComponent> menuItems = null;

    /**
     * Creates a text editor component for the macros file on a station in an
     * universe
     * 
     * @param frame the frame of the application
     * @param macros the macros object presented by this component
     */
    public SWGMacrosPane(SWGFrame frame, SWGMacros macros) {
        this.frame = frame;

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        this.setEditable(false);

        this.setText(macros.content());
        this.setCaretPosition(0);

        Font f = this.getFont();
        int s = ((Integer) SWGFrame.getPrefsKeeper().get("macrosPaneFontSize",
                        new Integer(f.getSize()))).intValue();
        this.setFont(new Font(Font.MONOSPACED, f.getStyle(), s));

        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                macrosClicked(e);
            }
        });
        createKeyActions();
    }

    /**
     * Changes font size relatively to the value of <code>change</code>, a value
     * of 0 resets the font size to normal size, otherwise the new font is the
     * product of current font size times <code>change</code>
     * 
     * @param change the multiplier for the new font size, 0 resets font size to
     *        normal
     */
    void changeFont(double change) {
        int s = 0;
        Font f = null;
        if (change != 0) {
            f = this.getFont();
            s = Math.max((int) (Math.round(f.getSize() * change)), 6);
            f = new Font(Font.MONOSPACED, f.getStyle(), s);
        } else {
            f = frame.getFont();
        }
        this.setFont(new Font(Font.MONOSPACED, f.getStyle(), f.getSize()));
        SWGFrame.getPrefsKeeper().add("macrosPaneFontSize",
                new Integer(f.getSize()));
    }

    /**
     * Creates and applies key actions for the mail body font size
     */
    private void createKeyActions() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_MASK),
                "macrosFont+Action");
        this.getActionMap().put("macrosFont+Action", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                changeFont(1.2);
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(
                        KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                                InputEvent.CTRL_MASK), "macrosFont-Action");
        this.getActionMap().put("macrosFont-Action", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                changeFont(1 / 1.2);
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_MASK),
                "macrosFont0Action");
        this.getActionMap().put("macrosFont0Action", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                changeFont(0);
            }
        });
    }

    /**
     * Called when this GUI component gains or looses focus
     * 
     * @param gained <code>true</code> when this component gained focus,
     *        <code>false</code> when focus is lost
     */
    public void focusGained(boolean gained) {
        if (gained) {
            if (menuItems == null) {
                menuItems = new ArrayList<JComponent>();

                menuItems.add(new JPopupMenu.Separator());

                JMenuItem copy = macrosCopyMenuItem();
                menuItems.add(copy);
            }
            frame.editMenuAdd(menuItems);
        } else {
            frame.editMenuRemove(menuItems);
        }
    }

    /**
     * Handles button-3 mouse clicks on the mail body raising a popup dialogue
     * 
     * @param e the mouse event causing this action
     */
    protected void macrosClicked(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            JPopupMenu popup = new JPopupMenu();

            JMenuItem fontBigger = new JMenuItem("Font increase");
            fontBigger.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,
                    InputEvent.CTRL_MASK));
            fontBigger.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    changeFont(1.2);
                }
            });
            popup.add(fontBigger);

            JMenuItem fontLesser = new JMenuItem("Font decrease");
            fontLesser.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                    InputEvent.CTRL_MASK));
            fontLesser.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    changeFont(1 / 1.2);
                }
            });
            popup.add(fontLesser);

            JMenuItem fontNormal = new JMenuItem("Font normal");
            fontNormal.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0,
                    InputEvent.CTRL_MASK));
            fontNormal.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    changeFont(0);
                }
            });
            popup.add(fontNormal);

            popup.show(this, e.getX(), e.getY());
        }
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem macrosCopyMenuItem() {
        JMenuItem copy = new JMenuItem("Copy");
        copy.setToolTipText("Copy selected text to clipboard");
        copy.setMnemonic('C');
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                InputEvent.CTRL_MASK));
        copy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                copy();
            }
        });
        return copy;
    }
}
