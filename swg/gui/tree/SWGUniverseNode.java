package swg.gui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import swg.model.SWGUniverse;

/**
 * This class is the node for a SWG universe, the first visible node in the
 * tree. Optionally a TC node may be present, if the user also plays at
 * TestCenter.
 * <p>
 * If no valid path exists for the universe this node is rendered gray and some
 * features are invalid.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGUniverseNode extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 2557593679767796019L;

    /**
     * Creates a node for <code>universe</code>
     * 
     * @param universe the universe which is represented by this node
     */
    SWGUniverseNode(SWGUniverse universe) {
        super(universe);
        this.setAllowsChildren(true);
    }

    @Override
    protected void focusGained(EventObject evt) {
        if (evt instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent) evt;
            if (mev.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu pop = new JPopupMenu();

                JMenuItem hidden = new JMenuItem("Unhide...", KeyEvent.VK_U);
                hidden.setToolTipText("Unhide all hidden objects");
                hidden.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        mainTab.unhideNodes();
                    }
                });
                hidden.setEnabled(mainTab.hiddenNodes.size() > 0);
                pop.add(hidden);

                pop.show(mev.getComponent(), mev.getX(), mev.getY());
                return;
            }
        }
        mainTab.setMainPaneDefault();

        String s = universe().getName();
        s = exists()
                ? s
                : s + " (no folder for SWG)";
        frame.putToLogbar_1(s);
        frame.putToLogbar_2(null);
    }

    @Override
    protected void focusLost() {
        // pass
    }
    
    /**
     * Returns the universe that is wrapped by this node.
     * 
     * @return a universe
     */
    SWGUniverse universe() {
        return (SWGUniverse) userObject;
    }
}
