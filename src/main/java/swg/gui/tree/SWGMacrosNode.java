package swg.gui.tree;

import java.awt.event.MouseEvent;
import java.util.EventObject;

import swg.gui.SWGMacrosPane;
import swg.model.SWGMacros;

/**
 * This type wraps a {@link SWGMacros} instance which is a child to a station
 * node.
 * <p>
 * If no valid path exists for the macros this node is rendered gray and some
 * features are invalid.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
class SWGMacrosNode extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -5599509874716277195L;

    /**
     * A GUI element for this node.
     */
    private transient SWGMacrosPane macrosPane;

    /**
     * Creates a node for {@code macros}.
     * 
     * @param macros the macros for this node
     */
    SWGMacrosNode(SWGMacros macros) {
        super(macros);
        this.setAllowsChildren(false);
    }

    @Override
    public boolean exists() {
        return macros().exists();
    }

    @Override
    protected void focusGained(EventObject evt) {
        if (evt instanceof MouseEvent
                && ((MouseEvent) evt).getButton() == MouseEvent.BUTTON3)
            return;

        if (exists()) {
            macrosPane();
            mainTab.showMacros(macrosPane);
            macrosPane.focusGained(true);
        } else
            mainTab.setMainPaneDefault();

        frame.putToLogbar_1(String.format("%s \u2014 %s%s",
                getParent().toString(), macros().getName(), exists()
                        ? ""
                        : " (no SWG)"));
        frame.putToLogbar_2(null);
    }

    @Override
    protected void focusLost() {
        if (macrosPane != null) macrosPane.focusGained(false);
    }

    /**
     * Returns the macros object that is wrapped by this node.
     * 
     * @return the macros
     */
    SWGMacros macros() {
        return (SWGMacros) userObject;
    }

    /**
     * Returns the GUI element for macros.
     * 
     * @return a GUI component
     */
    private SWGMacrosPane macrosPane() {
        if (macrosPane == null)
            macrosPane = new SWGMacrosPane(frame, macros());
        return macrosPane;
    }
}
