package swg.gui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import swg.gui.SWGAliasesPane;
import swg.model.SWGAliases;

/**
 * This type wraps an instance of SWGAliases. Aliases pertains to a SWG
 * universe.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGAliasesNode extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 4139881970020826319L;

    /**
     * The GUI element for this node.
     */
    private transient SWGAliasesPane aliasesPane;

    /**
     * Creates a node for {@code aliases}.
     * 
     * @param aliases the aliases object represented by this node
     */
    SWGAliasesNode(SWGAliases aliases) {
        super(aliases);
        this.setAllowsChildren(false);
    }
    
    /**
     * Deletes this aliases object from the system
     */
    private void delete() {
        if (aliases() == null
                || JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                        frame,
                        String.format("Delete \"%s\"", aliases().getName()),
                        "Confirm deletion",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE)) {

            if (aliases() != null) aliases().erase(true);

            SWGTreeNode p = (SWGTreeNode) this.getParent();
            mainTab.tree.setSelectionPath(new TreePath(p.getPath()));
            SWGTreeNode.focusTransition(p, new EventObject(this));
            ((DefaultTreeModel) mainTab.tree.getModel())
                    .removeNodeFromParent(this);

            focusLost();
        }
    }
    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem deleteMenuItem() {
        JMenuItem del = new JMenuItem("Delete");
        del.setToolTipText("Delete the notes file");
        del.setMnemonic('D');
        del.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                delete();
            }
        });
        return del;
    }

    /**
     * Returns the aliases object that is wrapped by this node.
     * 
     * @return an aliases object
     */
    SWGAliases aliases() {
        return (SWGAliases) userObject;
    }

    /**
     * Returns the GUI component for aliases
     * 
     * @return the GUI component for aliases
     */
    private SWGAliasesPane aliasesPane() {
        if (aliasesPane == null)
            aliasesPane = new SWGAliasesPane(frame, aliases());

        return aliasesPane;
    }

    @Override
    public boolean exists() {
        return aliases().exists();
    }

    @Override
    protected void focusGained(EventObject evt) {
        if (evt instanceof MouseEvent
                && ((MouseEvent) evt).getButton() == MouseEvent.BUTTON3) {
        	JPopupMenu popup = new JPopupMenu();
        	popup.add(deleteMenuItem());
        	MouseEvent e = (MouseEvent) evt;
            popup.show(e.getComponent(), e.getX(), e.getY());
        }

        boolean exists = exists();
        if (exists) {
            aliasesPane();
            mainTab.showAliases(aliasesPane);
            aliasesPane.focusGained(true);
        } else
            mainTab.setMainPaneDefault();

        frame.putToLogbar_1(String.format("%s%s", aliases().getName(), exists
                ? ""
                : " (no SWG)"));
        frame.putToLogbar_2(null);
    }

    @Override
    protected void focusLost() {
        if (aliasesPane != null) aliasesPane.focusGained(false);
    }
}
