package swg.gui.tree;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.gui.SWGMainTab;
import swg.model.SWGAliases;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;
import swg.model.SWGMacros;
import swg.model.SWGNotes;
import swg.model.SWGStation;
import swg.model.SWGUniverse;

/**
 * This is the topmost node of the tree model. It is invisible and only the SWG
 * and TC are the two visible and expandable nodes.
 * <p>
 * This class provides helper functions to create nodes for the GUI tree that
 * stems from this root.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGRoot extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 2599383142724872445L;

    /**
     * Creates the topmost node of the tree model
     * 
     * @param frame the frame of this application
     * @param mainTab the main tab onto which the tree is displayed
     */
    public SWGRoot(SWGFrame frame, SWGMainTab mainTab) {
        super(frame, mainTab);
        setAllowsChildren(true);

        // this.userObject is ONLY used while creating the tree, the node is
        // invisible after that and should not be used
        this.userObject = SWGFrame.getPrefsKeeper().get("mainTabSelectedNode");
    }

    @Override
    public boolean exists() {
        return true; // always
    }

    /**
     * Returns the node that pertains the specified universe, or {@code null}.
     * 
     * @param u a universe
     * @return a universe node, or {@code null}
     */
    public SWGUniverseNode findUniverse(SWGUniverse u) {
        for (Object o : children) {
            SWGUniverseNode un = (SWGUniverseNode) o;
            if (un.getUserObject().equals(u))
                return un;
        }
        return null;
    }

    @Override
    protected void focusGained(EventObject evt) {
        mainTab.setMainPaneDefault();
    }

    @Override
    protected void focusLost() {
        // pass
    }

    @Override
    public final int hashCode() {
        return 4711; // there is just one root so any fix number is OK
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    /**
     * Helper method which determines if the specified node had focus the most
     * recent time SWGAide exited. If {@code n.userObject == this.userObject}
     * this method updates {@link #focusedNode} to the specified node; {@code
     * this.userObject} is set when this node is instantiated. If {@code
     * focusedNode != null} this method does nothing.
     * 
     * @param n a node
     */
    private void setFocusNode(SWGTreeNode n) {
        // this.userObject is ONLY used while creating the tree, the node is
        // invisible after that and the member should not be used
        if (focusedNode() != null || this.userObject == null) return;
        if (this.userObject.equals(n.getUserObject())) focusedNode(n);
    }

    /**
     * Helper method which creates and adds aliases nodes to the specified
     * universe. This method iterates over the aliases of the universe and
     * creates nodes that are added to the universe node.
     * 
     * @param un the universe node to populate
     * @param root the root node of the populated tree
     */
    private static void addAliasNodes(SWGUniverseNode un, SWGRoot root) {
        List<SWGAliases> als = un.universe().aliases();
        if(!als.isEmpty()) {
        	SWGLeafNode leaf = new SWGLeafNode("Aliases");
        	un.add(leaf);
        	for (SWGAliases a : als) {
                SWGAliasesNode an = new SWGAliasesNode(a);
                root.setFocusNode(an);
                leaf.add(an);
            }
        }
        
    }

    /**
     * Helper method which creates and adds character nodes to the specified
     * node. This method iterates over the characters of the galaxy and creates
     * nodes that are added to the galaxy node.
     * 
     * @param gn the galaxy node to populate
     * @param root the root node for the GUI tree
     */
    private static void addCharacterNodes(SWGGalaxyNode gn, SWGRoot root) {
        List<SWGCharacter> cl = gn.galaxy().characters();
        Collections.sort(cl);

        for (SWGCharacter ch : cl) {
            SWGCharacterNode cn = new SWGCharacterNode(ch);
            root.setFocusNode(cn);
            gn.add(cn);
        }
    }

    /**
     * Helper method which creates and adds galaxy nodes to the specified node.
     * The galaxies are obtained from the station. This method iterates over the
     * galaxies of the station and creates nodes for its children and their
     * children; a node is added to the parent it pertains to.
     * 
     * @param sn the station node to populate
     * @param root the root node for the GUI tree
     * @param mt the main pane that displays the GUI tree
     */
    private static void addGalaxyNodes(
            SWGStationNode sn, SWGRoot root, SWGMainTab mt) {

        List<SWGGalaxy> gxs = sn.station().galaxies();
        Collections.sort(gxs);
        for (SWGGalaxy gxy : gxs) {
            if (!mt.hiddenNodes.containsKey(SWGGalaxyNode.hideString(gxy))) {
                SWGGalaxyNode gn = new SWGGalaxyNode(gxy);
                root.setFocusNode(gn);
                sn.add(gn);

                addCharacterNodes(gn, root);
            }
        }
    }

    /**
     * Helper method which creates and adds a macros nodes to the specified
     * station.
     * 
     * @param sn the station node to populate
     * @param root the root node of the populated tree
     */
    private static void addMacroNode(SWGStationNode sn, SWGRoot root) {
        SWGMacros macros = sn.station().macros();
        if(macros.exists()) {
	        SWGMacrosNode mn = new SWGMacrosNode(macros);
	        root.setFocusNode(mn);
	        sn.add(mn);
        }
    }

    /**
     * Helper method which creates and adds notes nodes to the specified
     * station. This method iterates over the notes of the station and creates
     * nodes that are added to the station node.
     * 
     * @param sn the station node to populate
     * @param root the root node of the populated tree
     */
    private static void addNotesNodes(SWGStationNode sn, SWGRoot root) {
        List<SWGNotes> nl = sn.station().notes();
        if(!nl.isEmpty()) {
	        SWGLeafNode leaf = new SWGLeafNode("Notes");
	        sn.add(leaf);
	        for (SWGNotes notes : nl) {
	            SWGNotesNode nn = new SWGNotesNode(notes);
	            root.setFocusNode(nn);
	            leaf.add(nn);
	        }
        }
    }

    /**
     * Helper method which creates and adds station nodes to the specified
     * universe node. This method iterates over the stations of the universe and
     * creates nodes for its children and their children; a node is added to the
     * parent it pertains to.
     * 
     * @param un the universe node to populate
     * @param root the root node of the populated tree
     * @param mt the main pane that displays the GUI tree
     */
    private static void addStationNodes(
            SWGUniverseNode un, SWGRoot root, SWGMainTab mt) {

        SWGUniverse u = un.universe();
        List<String> snl = u.stationNames();
        Collections.sort(snl);
        for (String snm : snl) {
            SWGStation s = u.station(snm);

            if (s == null)
                SWGAide.printDebug("trrt", 1, String.format(
                        "SWGRoot:addStnNds: no station for %s in %s", snm, u));

            else if (!mt.hiddenNodes.containsKey(s.getName())) {
                SWGStationNode sn = new SWGStationNode(s);
                root.setFocusNode(sn);
                un.add(sn);

                addGalaxyNodes(sn, root, mt);

                addNotesNodes(sn, root);
                addMacroNode(sn, root);
            }

        }
    }

    /**
     * Creates a tree rooted from the specified universe that is added to the
     * root node. This method iterates over the content of the universe and
     * creates nodes for its children and their children; a node is added to the
     * parent it pertains to. Finally the tree that is rooted in the universe is
     * added to the specified root node. If the specified node is {@code null}
     * this method creates and adds a node with name "error".
     * 
     * @param u a universe
     * @param root the root node for the GUI tree
     * @param mt the main pane that displays the GUI tree
     */
    public static void createPopulatedTree(
            SWGUniverse u, SWGRoot root, SWGMainTab mt) {

        if (u == null) root.add(new DefaultMutableTreeNode("error"));

        SWGUniverseNode un = new SWGUniverseNode(u);
        root.setFocusNode(un);
        root.add(un);
        
        addStationNodes(un, root, mt);
        addAliasNodes(un, root);

        if (focusedNode() == null && root.userObject != null)
            SWGFrame.getPrefsKeeper().add("mainTabSelectedNode", null);
    }
}
