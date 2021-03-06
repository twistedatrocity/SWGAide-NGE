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
import swg.gui.schematics.SWGSchematicTab;
import swg.model.SWGCGalaxy;
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
     * if bypass is true, bypasses the dialogue and performs the delete.
     * @param boolean bypass
     */
    public void delete(boolean bypass) {
    	boolean doit = false;
        if (character() != null) {
        	if(bypass) {
        		doit = true;
        	} else if(JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                        frame,
                        String.format("Delete mail folders and swgaide entry for \"%s\"", character().getName()),
                        "Confirm character deletion",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE)) {
        			doit = true;
        	}
	        if(doit) {
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
    }
    
    /**
     * Displays delete menu item
     * @return JmenuItem
     */
    private JMenuItem deleteMenuItem() {
        JMenuItem del = new JMenuItem("Delete");
        del.setToolTipText("Delete this characters mail folders and remove from swgaide");
        del.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                delete(false);
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
        
        SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        if(galaxy == null) {
        	galaxy = gxy;
        }
        SWGGalaxy g = character().galaxy();
        SWGCGalaxy rg = character().gxy();
        if(!galaxy.equals(rg)) {
        	SWGSchematicTab st =SWGFrame.getSchematicTab(frame);
        	st.tintTabs(false);
        }
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
