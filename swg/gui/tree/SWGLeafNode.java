package swg.gui.tree;

import java.util.EventObject;

/**
 * This type is the node for a simple leaf.
 * <p>
 * 
 * 
 */
public class SWGLeafNode extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -9061380605370701866L;

    /**
     * Creates a node for {@code galaxy}
     * 
     * @param leaf the string represented by this node
     */
    SWGLeafNode(String leaf) {
        super(leaf);
        this.setAllowsChildren(true);
    }

    @Override
    protected void focusLost() {
        // nothing to do
    }

    /**
     * Returns the string that is wrapped by this node.
     * 
     * @return a string
     */
    String leaf() {
        return (String) userObject;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

	@Override
	protected void focusGained(EventObject evt) {
		// TODO Auto-generated method stub
		
	}
}
