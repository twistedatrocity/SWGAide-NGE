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
