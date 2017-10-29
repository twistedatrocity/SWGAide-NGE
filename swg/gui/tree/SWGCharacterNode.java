package swg.gui.tree;

import java.awt.event.MouseEvent;
import java.util.EventObject;

import swg.gui.SWGFrame;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;

/**
 * This type is a GUI node for a SWG character.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGCharacterNode extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 8418166255661446506L;

    /**
     * Creates a node for {@code galaxy}.
     * 
     * @param character the character represented by this node
     */
    SWGCharacterNode(SWGCharacter character) {
        super(character);
        this.setAllowsChildren(false);
    }

    /**
     * Returns the character that is wrapped by this node.
     * 
     * @return a character
     */
    SWGCharacter character() {
        return (SWGCharacter) userObject;
    }

    @Override
    protected void focusGained(EventObject evt) {
        if (evt instanceof MouseEvent
                && ((MouseEvent) evt).getButton() == MouseEvent.BUTTON3)
            return;

        SWGFrame.getPrefsKeeper().add("currentlySelectedCharacter", character());

        SWGGalaxy g = character().galaxy();
        String buf = String.format("%s \u2014 %s \u2014 %s%s",
                g.station().getName(), g.getName(),
                character().getNameComplete(), (g.exists()
                        ? ""
                        : " (no SWG)"));
        frame.putToLogbar_1(buf);
        frame.putToLogbar_2(null);

        mainTab.showMail();
        mainTab.mailClient.setCharacter(character());
        mainTab.mailClient.focusGained(true);
    }

    @Override
    protected void focusLost() {
        if (mainTab.mailClient != null) mainTab.mailClient.focusGained(false);
    }
}
