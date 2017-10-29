package swg.gui.tree;

import java.awt.event.MouseEvent;
import java.util.EventObject;

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
                && ((MouseEvent) evt).getButton() == MouseEvent.BUTTON3)
            return;

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
