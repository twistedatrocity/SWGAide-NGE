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

import swg.model.SWGAliases;

/**
 * This is a component for displaying aliases files of a SWG universe.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGAliasesPane extends JTextArea {

    /**
     * The frame for SWGAide.
     */
    private final SWGFrame frame;

    /**
     * A list of menu items for the "Edit" menu.
     */
    private ArrayList<JComponent> menuItems;

    /**
     * Creates a text editor component for the specified aliases file.
     * 
     * @param frame the frame
     * @param aliases the aliases object displayed by this component
     */
    public SWGAliasesPane(SWGFrame frame, SWGAliases aliases) {
        this.frame = frame;

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        this.setEditable(false);

        this.setText(aliases.content());
        this.setCaretPosition(0);

        Font f = this.getFont();
        int s = ((Integer) SWGFrame.getPrefsKeeper().get(
                "aliasPaneFontSize", new Integer(f.getSize()))).intValue();
        this.setFont(new Font(Font.MONOSPACED, f.getStyle(), s));

        this.addMouseListener(new MouseAdapter() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void mouseClicked(MouseEvent e) {
                aliasesClicked(e);
            }
        });
        createKeyActions();
    }

    /**
     * Handles button-3 mouse clicks on the mail body raising a popup dialogue.
     * 
     * @param e
     *            the mouse event causing this action
     */
    private void aliasesClicked(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            JPopupMenu popup = new JPopupMenu();

            JMenuItem fontBigger = new JMenuItem("Font increase");
            fontBigger.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_PLUS, InputEvent.CTRL_MASK));
            fontBigger.addActionListener(new ActionListener() {

                @SuppressWarnings( {"hiding", "synthetic-access"})
                public void actionPerformed(ActionEvent e) {
                    changeFont(1.2);
                }
            });
            popup.add(fontBigger);

            JMenuItem fontLesser = new JMenuItem("Font decrease");
            fontLesser.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_MINUS, InputEvent.CTRL_MASK));
            fontLesser.addActionListener(new ActionListener() {

                @SuppressWarnings( {"hiding", "synthetic-access"})
                public void actionPerformed(ActionEvent e) {
                    changeFont(1 / 1.2);
                }
            });
            popup.add(fontLesser);

            JMenuItem fontNormal = new JMenuItem("Font normal");
            fontNormal.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_0, InputEvent.CTRL_MASK));
            fontNormal.addActionListener(new ActionListener() {

                @SuppressWarnings( {"hiding", "synthetic-access"})
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
    private JMenuItem aliasesCopyMenuItem() {
        JMenuItem copy = new JMenuItem("Copy");
        copy.setToolTipText("Copy selected text to clipboard");
        copy.setMnemonic('C');
        copy.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_C, InputEvent.CTRL_MASK));
        copy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                copy();
            }
        });
        return copy;
    }

    /**
     * Changes font size relatively to the value of {@code change}, a value of 0
     * resets the font size to normal size, otherwise the new font is the
     * product of current font size times {@code change}.
     * 
     * @param change
     *            the multiplier for the new font size, 0 resets font size to
     *            normal
     */
    private void changeFont(double change) {
        int s = 0;
        Font f;
        if (change != 0) {
            f = this.getFont();
            s = Math.max((int) (Math.round(f.getSize() * change)), 6);
            f = new Font(Font.MONOSPACED, f.getStyle(), s);
        } else
            f = frame.getFont();

        this.setFont(new Font(Font.MONOSPACED, f.getStyle(), f.getSize()));
        SWGFrame.getPrefsKeeper().add(
            "aliasPaneFontSize", new Integer(f.getSize()));
    }

    /**
     * Creates and applies key actions for the mail body font size
     */
    private void createKeyActions() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_MASK),
            "aliasesFont+Action");
        this.getActionMap().put("aliasesFont+Action", new AbstractAction() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                changeFont(1.2);
            }
            });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_MASK),
            "aliasesFont-Action");
        this.getActionMap().put("aliasesFont-Action", new AbstractAction() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                changeFont(1 / 1.2);
            }
            });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_MASK),
            "aliasesFont0Action");
        this.getActionMap().put("aliasesFont0Action", new AbstractAction() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                changeFont(0);
            }
            });
    }

    /**
     * Called when this GUI component gains or looses focus
     * 
     * @param gained
     *            {@code true} when this component gained focus, {@code false}
     *            when focus is lost
     */
    public void focusGained(boolean gained) {
        if (gained) {
            if (menuItems == null) {
                menuItems = new ArrayList<JComponent>();

                menuItems.add(new JPopupMenu.Separator());

                JMenuItem copy = aliasesCopyMenuItem();
                menuItems.add(copy);
            }
            frame.editMenuAdd(menuItems);
        } else
            frame.editMenuRemove(menuItems);
    }    
}
