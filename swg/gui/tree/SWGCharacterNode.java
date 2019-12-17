package swg.gui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.EventObject;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

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
     * Deletes this character object from the system
     */
    private void delete() {
        if (character() == null
                || JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                        frame,
                        String.format("Delete mail folders and swgaide entry for \"%s\"", character().getName()),
                        "Confirm character deletion",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE)) {

        	File lf = new File( character().mailBox().swgAidePath().toString() );
            File sf = new File( character().mailBox().swgPath().toString() );
            
            if (lf.exists() && lf.isDirectory()) {
            	character().mailBox().deleteLocalDir(lf);
            }
            if (sf.exists() && sf.isDirectory()) {
            	character().mailBox().deleteLocalDir(sf);
            }

            SWGTreeNode p = (SWGTreeNode) this.getParent();
            mainTab.tree.setSelectionPath(new TreePath(p.getPath()));
            SWGTreeNode.focusTransition(p, new EventObject(this));
            ((DefaultTreeModel) mainTab.tree.getModel())
                    .removeNodeFromParent(this);
            SWGCharacter.scanForNewCharacters(character().galaxy());

            focusLost();
        }
    }
    
    private JMenuItem deleteMenuItem() {
        JMenuItem del = new JMenuItem("Delete");
        del.setToolTipText("Delete this characters mail folders and remove from swgaide");
        del.setMnemonic('D');
        del.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                delete();
            }
        });
        return del;
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
                && ((MouseEvent) evt).getButton() == MouseEvent.BUTTON3) {
        	JPopupMenu popup = new JPopupMenu();

            popup.add(deleteMenuItem());

            MouseEvent e = (MouseEvent) evt;
            popup.show(e.getComponent(), e.getX(), e.getY());
        }

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
