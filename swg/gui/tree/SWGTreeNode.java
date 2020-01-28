package swg.gui.tree;

import java.io.Serializable;
import java.util.EventObject;

import javax.swing.tree.DefaultMutableTreeNode;
import swg.gui.SWGFrame;
import swg.gui.SWGMainTab;
import swg.gui.common.SWGGui;
import swg.model.SWGCGalaxy;

/**
 * This type models a node in the GUI tree at the "Main" panel.
 * <p>
 * This class was remodeled in 2010 to provide for a more flexible tree.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public abstract class SWGTreeNode extends DefaultMutableTreeNode {

    /**
     * The node that currently is focused. During a focus transition this member
     * is not set until transition is finished.
     */
    private static SWGTreeNode focusedNode;

    /**
     * The frame containing the GUI where this node will be used.
     */
    protected static SWGFrame frame;
    
    /**
     * Galaxy reference
     */
    protected SWGCGalaxy galaxy;

    /**
     * The panel that contains the tree and related GUI elements.
     */
    protected static SWGMainTab mainTab;

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization. XXX: retain until it is safe to remove, old-old users
     * may have old DAT files with references to this type.
     */
    private static final long serialVersionUID = -2138669912143725162L;

    /**
     * Creates an instance of this type for the specified user object; compare
     * {@link DefaultMutableTreeNode#DefaultMutableTreeNode(Object)}
     * 
     * @param o the object wrapped by this node
     */
    protected SWGTreeNode(Object o) {
        super(o);
    }

    /**
     * Creates an instance of this type.
     * 
     * @param frame the frame for this application
     * @param mainTab the GUI main pane containing items updated from this node
     */
    protected SWGTreeNode(SWGFrame frame, SWGMainTab mainTab) {
        super();
        SWGTreeNode.frame = frame;
        SWGTreeNode.mainTab = mainTab;
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj
                || (obj instanceof SWGTreeNode
                        && userObject != null
                        && userObject.equals(((SWGTreeNode) obj).userObject));
    }

    /**
     * Returns {@code true} if this node should be painted at the GUI as if SWG
     * at the current host, no matter the correct state.
     * 
     * @return {@code true} to paint this node as if SWG exists
     */
    public boolean exists() {
        return true;
    }

    /**
     * Notifies the controller/component associated with this node to let it
     * initiate or resume states and prepare the GUI
     * 
     * @param evt the event object causing this action; a MouseEvent or a
     *        TreeSelectionEvent
     */
    protected abstract void focusGained(EventObject evt);

    /**
     * Notifies the controller/component associated with this node to let it
     * save states, reset GUI, and clean up
     */
    protected abstract void focusLost();

    @Override
    public int hashCode() {
        return userObject != null
                ? userObject.hashCode()
                : super.hashCode();
    }
    
    @Override
    public final String toString() {
    	if(userObject instanceof java.lang.String) {
    		return (String) userObject;
    	}
        return userObject != null
                ? ((SWGGui) userObject).getName()
                : "error";
    }

    /**
     * Returns the currently focused node
     * 
     * @return the focusedNode the node that has focus currently
     */
    public static SWGTreeNode focusedNode() {
        return focusedNode;
    }

    /**
     * Sets {@link #focusedNode} to the specified argument.
     * <p>
     * This method <b>must only</b> be invoked by this instance and one time by
     * {@link SWGRoot} while a universal tree is created.
     * 
     * @param fn a node
     */
    protected static void focusedNode(SWGTreeNode fn) {
        focusedNode = fn;
    }

    /**
     * Switches focus between two nodes at the universal tree, or when focus is
     * lost temporarily. This method invokes {@link #focusLost()} of the current
     * {@link #focusedNode} if it is {@code null}. If the specified node is not
     * {@code null} this method updates the entry in SWGAide's DAT file, invokes
     * its {@link #focusGained(EventObject)}, and sets {@link #focusedNode}.
     * <p>
     * A {@code null} node argument denotes lost focus, possibly because the
     * user navigates to another panel in SWGAide. In this case the current
     * node's {@link #focusLost()} method is invoked, and the event argument is
     * ignored.
     * 
     * @param node the node that gains focus, or {@code null}
     * @param e the event behind this call, {@code null} if node is {@code null}
     */
    public static final void focusTransition(SWGTreeNode node, EventObject e) {
        if (focusedNode() != null) focusedNode().focusLost();
        if (node != null) {
            SWGFrame.getPrefsKeeper().add("mainTabSelectedNode",
                    (Serializable) node.userObject);
            node.focusGained(e);
            focusedNode(node);
        }
    }
}
