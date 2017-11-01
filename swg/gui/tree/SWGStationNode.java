package swg.gui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import swg.model.SWGNotes;
import swg.model.SWGStation;

/**
 * This class is the node for a SWG station, one node each station.
 * <p>
 * If no valid path exists for the station this node is rendered gray and some
 * features are invalid.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGStationNode extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 3403555217794642436L;

    /**
     * A menu item for the frame. This is a menu item for creating a new notes
     * file.
     */
    private transient JMenuItem newNotesMenu;

    /**
     * Creates a node for <code>station</code>
     * 
     * @param station the station represented by this node
     */
    SWGStationNode(SWGStation station) {
        super(station);
        this.setAllowsChildren(true);
        newNotesMenu = addNotesMenuItem();
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    JMenuItem addNotesMenuItem() {
        JMenuItem a = new JMenuItem("New notes file");
        a.setToolTipText("Add another notes file to the selected station");
        a.setMnemonic(KeyEvent.VK_N);
        a.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                addNotesObject();
            }
        });
        return a;
    }

    /**
     * Adds another notes file under this station node
     */
    private void addNotesObject() {
        SWGNotesNode node = SWGNotesNode.newNode(this);
        if (node == null) return;
        ((DefaultTreeModel) mainTab.tree.getModel()).insertNodeInto(node, this,
                this.getChildCount() - 1);
        mainTab.tree.setSelectionPath(new TreePath(node.getPath()));
        node.focusGained(null);
    }

    @Override
    public boolean exists() {
        return station().exists();
    }

    @Override
    protected void focusGained(EventObject evt) {
        if (exists()) {
            if (evt instanceof MouseEvent) {
                final MouseEvent mev = (MouseEvent) evt;
                if (mev.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();

                    popup.add(addNotesMenuItem());
                    popup.add(refreshNotesFilesMenuItem());

                    popup.addSeparator();
                    JMenuItem hide = new JMenuItem("Hide", KeyEvent.VK_H);
                    hide.setToolTipText("Hide this node from the view");
                    hide.addActionListener(new ActionListener() {
                        
                        public void actionPerformed(ActionEvent e) {
                            if (JOptionPane.OK_OPTION
                                    == JOptionPane.showConfirmDialog(
                                            mev.getComponent(),
                                            "Hide this node from view?",
                                            "Hide node",
                                            JOptionPane.OK_CANCEL_OPTION,
                                            JOptionPane.QUESTION_MESSAGE))
                                        hideNode();
                                }
                    });
                    popup.add(hide);

                    popup.show(mev.getComponent(), mev.getX(), mev.getY());
                    return;
                } // mouse-event

                frame.editMenuAdd(newNotesMenu);
            }
        }

        mainTab.setMainPaneDefault();

        String s = station().getName();
        frame.putToLogbar_1(exists()
                ? s
                : s + " (no SWG)");
        frame.putToLogbar_2(null);
    }

    @Override
    protected void focusLost() {
        frame.editMenuRemove(newNotesMenu);
    }

    /**
     * Hides this node and removes it from the tree o nodes
     */
    private void hideNode() {
        mainTab.hiddenNodes.put(station().getName(), "hidden");
        ((DefaultTreeModel) mainTab.tree.getModel()).removeNodeFromParent(this);

    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Refreshes the list of notes, if any
     */
    private void refreshNotes() {
        for (SWGNotes n : station().notesNew()) {
            station().notesAdd(n);
            SWGNotesNode node = new SWGNotesNode(n);
            ((DefaultTreeModel) mainTab.tree.getModel()).insertNodeInto(node,
                    this, this.getChildCount() - 1);
            mainTab.tree.setSelectionPath(new TreePath(node.getPath()));
            node.focusGained(null);
        }
    }

    /**
     * Returns a menu item for refreshing the selected station for possible new
     * notes files
     * 
     * @return a menu items for refreshing the list of notes files
     */
    private JMenuItem refreshNotesFilesMenuItem() {
        JMenuItem refresh = new JMenuItem("Refresh notes ");
        refresh.setToolTipText("Check for new notes files for this station");
        refresh.setMnemonic('R');
        refresh.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                refreshNotes();
            }
        });
        return refresh;
    }

    /**
     * Returns the station that is wrapped by this node.
     * 
     * @return a station
     */
    SWGStation station() {
        return (SWGStation) userObject;
    }
}
