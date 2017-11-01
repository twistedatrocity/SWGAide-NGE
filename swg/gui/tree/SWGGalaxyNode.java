package swg.gui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;

import swg.gui.SWGMainTab;
import swg.model.SWGGalaxy;

/**
 * This type is the node for a SWG station, one node each station.
 * <p>
 * If no valid path exists for the universe this node is rendered gray and some
 * features are invalid.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGGalaxyNode extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -9061380605370701866L;

    /**
     * Creates a node for {@code galaxy}
     * 
     * @param galaxy the galaxy represented by this node
     */
    SWGGalaxyNode(SWGGalaxy galaxy) {
        super(galaxy);
        this.setAllowsChildren(true);
    }

    @Override
    protected void focusGained(EventObject evt) {
        if (evt instanceof MouseEvent) {
            final MouseEvent mev = (MouseEvent) evt;
            if (mev.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu popup = new JPopupMenu();

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
            }
        }

        if (galaxy().characters().isEmpty())
            mainTab.setRightComponent(new JLabel(String.format(
                    "<html>&nbsp;&nbsp;%s<br/>" +
                            "&nbsp;&nbsp;<tt>/mailsave</tt> %s</html>",
                    "Unless you have executed the in-game command",
                    "there are no characters to display.")));
        else
            mainTab.setMainPaneDefault();

        frame.putToLogbar_1(String.format("%s \u2014 %s",
                galaxy().station().getName(), galaxy().getName()));
        frame.putToLogbar_2(null);
    }

    @Override
    protected void focusLost() {
        // nothing to do
    }

    /**
     * Returns the galaxy that is wrapped by this node.
     * 
     * @return a galaxy
     */
    SWGGalaxy galaxy() {
        return (SWGGalaxy) userObject;
    }

    /**
     * Hides this node and removes it from the tree of nodes
     */
    private void hideNode() {
        mainTab.hiddenNodes.put(hideString(this.galaxy()), "hidden");
        ((DefaultTreeModel) mainTab.tree.getModel()).removeNodeFromParent(this);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns the string that is used as key in {@link SWGMainTab#hiddenNodes}
     * to hide nodes. This string must be identical to as well hide a node for
     * the specified galaxy, as to check if it is hidden; it is on the format
     * "Station_Galaxy" where both values are from {@code getName()}
     * respectively.
     * 
     * @param g a galaxy
     * @return a hide key
     */
    static String hideString(SWGGalaxy g) {
        return String.format("%s_%s", g.station().getName(), g.getName());
    }
}
