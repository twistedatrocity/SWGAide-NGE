package swg.gui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.EventObject;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import swg.SWGAide;
import swg.gui.SWGFrame;
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
        String tip = "<html><b>Folder: </b>"+ universe.swgPath().toString() + "<br>RIght click for more options</html>";
        this.toolTip = tip;
    }
    
    private void change () {
    	if (universe() != null
                && JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                        frame,
                        String.format("Change folder for \"%s\"", universe().getName()),
                        "Confirm SWG change folder",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE)) {
    		try {
				mainTab.changeSWG(universe());
			} catch (Throwable e) {
				SWGAide.printError("SWGMainTab:changeSWG:", e);
			}
    	}
    }
    
    @SuppressWarnings("unchecked")
	private void delete () {
    	if (universe() != null
                && JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                        frame,
                        String.format("Delete swgaide entry for \"%s\"", universe().getName()),
                        "Confirm SWG Install reference deletion",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE)) {
    		List<SWGUniverse> ul = (List<SWGUniverse>) SWGFrame.getPrefsKeeper().get("swgUniverseList");
    		if(ul.contains(universe())) {
    			universe().stationNames().forEach( (s) -> {
    				universe().stationRemove(s);
    			});
    			ul.remove(universe());
    		}
    		SWGFrame.getPrefsKeeper().add("swgUniverseList",(Serializable) ul);
    		
            SWGTreeNode p = (SWGTreeNode) this.getParent();
            mainTab.tree.setSelectionPath(new TreePath(p.getPath()));
            SWGTreeNode.focusTransition(p, new EventObject(this));
            ((DefaultTreeModel) mainTab.tree.getModel())
                    .removeNodeFromParent(this);

            focusLost();
        }
    }

    /**
     * Menu item for change folder
     * 
     * @return JMenuItem change menu
     */
    private JMenuItem changeItem () {
    	JMenuItem change = new JMenuItem("Change game folder...");
        change.setToolTipText("Change this SWG game folder to a new location");
        change.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                change();
            }
        });
    	return change;
    }
    
    /**
     * Menu item for delete folder
     * 
     * @return JMenuItem change menu
     */
    private JMenuItem deleteItem () {
    	JMenuItem change = new JMenuItem("Delete game folder...");
        change.setToolTipText("Delete the reference to this game folder");
        change.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                delete();
            }
        });
    	return change;
    }
    
    @Override
    protected void focusGained(EventObject evt) {
        if (evt instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent) evt;
            if (mev.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu pop = new JPopupMenu();
                pop.add(changeItem());
                pop.add(deleteItem());
                
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
