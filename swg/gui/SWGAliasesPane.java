package swg.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

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
