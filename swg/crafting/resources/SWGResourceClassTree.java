package swg.crafting.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import javax.swing.ImageIcon;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import swg.SWGAide;
import swg.crafting.resources.SWGResourceClass.SWGResourceClassRoot;
import swg.crafting.resources.types.SWGEnergy;
import swg.crafting.resources.types.SWGInorganic;
import swg.crafting.resources.types.SWGOrganic;
import swg.crafting.resources.types.SWGSpaceResource;
import swg.gui.SWGFrame;
import swg.tools.ZReader;
import swg.tools.ZString;

/**
 * This type models the tree of resource classes in SWG. That is, from root down
 * how resource classes descends from {@link SWGResourceClass} and downwards.
 * Leafs may either spawn in the worlds or they are space or recycled resource
 * classes, compare {@link SWGResourceClass#isSpawnable()}. For the static
 * methods to return sensible values this type must have been instantiated once
 * and a reference to it must be retained.
 * <p>
 * This type is, together with {@link SWGResourceClass} and
 * {@link SWGResourceClassInfo}, an abstract interface to SWGAide's model of
 * resource classes.
 * <p>
 * Notice: This model implements the methods of {@link TreeModel} with static
 * methods without actually signing the contract. This is deliberate, now only
 * one instance of this type is required, one that is initiated with the static
 * data structures; this is done early during SWGAide's launch by the resource
 * manager. Then local client implementations of {@link TreeModel} for resource
 * class trees only need to query the corresponding methods of this type.
 * <p>
 * Because this type is immutable and static it is inherently thread safe.
 * Methods that potentially returns mutable data rather returns a copy.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGResourceClassTree {

    /**
     * A sorted list of all nodes in this tree. This list is just for
     * convenience, to traverse for a specified resource class. The sort order
     * is distance from root, and secondly the sort order for resource classes.
     */
    private static List<RCNode> allNodes;
    
    /**
     * A map of icons for assorted resource classes used in GUI elements. If a
     * particular class does not map to an icon the getter method returns an
     * icon for the first parent class that maps to an icon.
     */
    private static Map<SWGResourceClass, ImageIcon> rcIcons;

    /**
     * The root of the tree structure.
     */
    private static RCNode root;

    /**
     * Creates an instance of this type and initiates the static tree structure.
     */
    @SuppressWarnings("synthetic-access")
    public SWGResourceClassTree() {
        if (root != null)
            throw new IllegalStateException("Instance exists already");

        allNodes = new ArrayList<RCNode>(SWGResourceClassInfo.LISTSIZE);

        root = new RCNode(null, SWGResourceClass.INSTANCE);
        populateRoot(root);
    }

    /**
     * Helper method which populates the specified node with children, for the
     * root node rather use the {@link #populateRoot(RCNode)}. The specified
     * node is made parent for those nodes that are created in this invocation.
     * For each new node, unless it is a leaf this method recursively invokes
     * itself and builds a tree. Each particular invocation, as a last task this
     * method sorts the list of children for the specified parent node.
     * 
     * @param p a node in this tree
     */
    @SuppressWarnings("synthetic-access")
    private void populate(RCNode p) {
        Class<? extends SWGResourceClass> c = p.resClass.getClass();
        for (int i = 1; i < SWGResourceClassInfo.swgIDtoInstance.size(); ++i) {
            SWGResourceClass rc = SWGResourceClassInfo.swgIDtoInstance.get(i);
            if (rc == null) continue;

            if (rc.getClass().getSuperclass().equals(c)) {
                // we have a direct child
                RCNode nn = new RCNode(p, rc);

                if (!(p.resClass.isSpawnable() || p.resClass.isSpaceOrRecycled()))
                    populate(nn); // recursive call
            }
        }

        Collections.sort(p.children);
    }

    /**
     * Helper method which populates this tree of resource classes. This method
     * creates the top nodes and invokes {@link #populate(RCNode)} with each one
     * of them. The four top resource classes are Energy, Organic, Inorganic,
     * and Space Resource.
     * 
     * @param r the root of this tree
     */
    @SuppressWarnings("synthetic-access")
    private void populateRoot(RCNode r) {
        populate(new RCNode(r, SWGEnergy.getInstance()));
        populate(new RCNode(r, SWGOrganic.getInstance()));
        populate(new RCNode(r, SWGInorganic.getInstance()));
        populate(new RCNode(r, SWGSpaceResource.getInstance()));

        Collections.sort(allNodes);
    }

    /**
     * Returns the distance between the child resource class and the more
     * generic resource class, or -1. This method returns
     * <ul>
     * <li>{@code -1 }if the child does not sub-class the other argument</li>
     * <li>{@code 0 }if the two arguments are the same class</li>
     * <li>the number of recursive calls to find the parent</li>
     * </ul>
     * 
     * @param rc a child resource class
     * @param rg a more generic resource class
     * @return the distance between the two classes, or -1
     * @throws IllegalArgumentException if the generic argument is the root
     * @throws NullPointerException if an argument is {@code null}
     */
    public static int distance(SWGResourceClass rc, SWGResourceClass rg) {
        if (rg == root.resClass) throw new IllegalArgumentException("Root");
        if (!rc.isSub(rg)) return -1;

        return distanceH(rc, rg);
    }

    /**
     * Helper method which returns the distance between the two arguments, see
     * {@link #distance(SWGResourceClass, SWGResourceClass)}. This method
     * recursively invokes itself for the distance.
     * 
     * @param rc a child resource class
     * @param rg a more generic resource class
     * @return the distance between the two classes, or -1
     */
    private static int distanceH(SWGResourceClass rc, SWGResourceClass rg) {
        if (rc == rg) return 0;

        return 1 + distanceH(getParent(rc), rg);
    }

    /**
     * Returns the child of {@code parent} at {@code index}. See
     * {@link TreeModel#getChild(Object, int)}.
     * 
     * @param parent a node in the tree
     * @param index the index for the child
     * @return a child
     * @throws NullPointerException if {@code parent} is {@code null}
     * @throws IndexOutOfBoundsException if {@code index} is invalid
     */
    public static SWGResourceClass getChild(SWGResourceClass parent, int index) {
        RCNode cn = getNode(parent);
        return cn.children.get(index).resClass;
    }

    /**
     * Returns the number of children of {@code parent}, or 0. See
     * {@link TreeModel#getChildCount(Object)}.
     * 
     * @param parent a node in the tree
     * @return the number of children
     * @throws NullPointerException if {@code parent} is {@code null}
     */
    public static int getChildCount(SWGResourceClass parent) {
        RCNode cn = getNode(parent);
        return cn.children.size();
    }

    /**
     * Returns a list of resource classes, or an empty list. Each element in the
     * returned list is a direct sub-class of the argument, not a sub-sub-class.
     * For example, if the argument is Metal the returned list contains Ferrous
     * Metal and Non-Ferrous Metal, but not Iron, Steel, Aluminum, or Copper. If
     * the argument can spawn in the worlds or if it is recycled an empty list
     * is returned.
     * 
     * @param parent a resource class
     * @return a list of child nodes, or an empty list
     * @throws NullPointerException if {@code parent} is {@code null}
     */
    public static List<SWGResourceClass> getChildren(SWGResourceClass parent) {
        if (parent.isSpawnable() || parent.isSpaceOrRecycled())
            return Collections.emptyList();

        List<RCNode> cn = getNode(parent).children;
        if (cn.size() == 0) return Collections.emptyList();

        List<SWGResourceClass> ret = new ArrayList<SWGResourceClass>(cn.size());
        for (RCNode e : cn)
            ret.add(e.resClass);

        return ret;
    }

    /**
     * Returns the index of {@code child} in {@code parent}, or -1. See
     * {@link TreeModel#getIndexOfChild(Object, Object)}.
     * 
     * @param parent a node in the tree
     * @param child the node we are interested in
     * @return the index of the child, or -1
     */
    public static int getIndexOfChild(
            SWGResourceClass parent, SWGResourceClass child) {

        if (parent != null && child != null) {
            List<RCNode> cn = getNode(parent).children;
            for (int i = 0; i < cn.size(); ++i)
                if (child.equals(cn.get(i).resClass))
                    return i;
        }

        return -1;
    }

    /**
     * Helper method which returns the node in the tree that corresponds to the
     * specified node.
     * 
     * @param rc a resource class
     * @return a node for the resource class
     * @throws NullPointerException if {@code rc} is {@code null}
     */
    private static RCNode getNode(SWGResourceClass rc) {
        for (RCNode e : allNodes)
            if (rc.equals(e.resClass)) return e;

        throw new IllegalStateException("Res class not found: " + rc);
    }

    /**
     * Returns the parent resource class for the specified argument, or {@code
     * null}. If the argument is one of the four top resource classes Energy,
     * Organic, Inorganic, or Space Resource, this method returns {@code null};
     * compare {@link #getRoot()}.
     * <p>
     * This is a convenience method provided outside the scope of a tree-model.
     * 
     * @param rc a resource class
     * @return the parent resource class for the argument, or {@code null}
     * @throws NullPointerException if the argument is {@code null}
     */
    public static SWGResourceClass getParent(SWGResourceClass rc) {
        if (rc.equals(root.resClass)) return null;

        RCNode p = getNode(rc);
        return p.parent == root
                ? null
                : p.parent.resClass;
    }

    /**
     * Returns the root of the tree that is modeled by this type. See
     * {@link TreeModel#getRoot()}.
     * <p>
     * <b>Notice:</b> The returned resource class <i>cannot </i> be used with
     * {@link SWGResourceClass#isSub(Class)} because it is not
     * really a super-class for any other resource class. The returned instance
     * is just a place holder for the root and is purely meant to be used with
     * GUI trees and their tree models, and then together with this model of a
     * resource class tree.
     * 
     * @return the root in the tree modeled by this type
     */
    public static SWGResourceClass getRoot() {
        return root.resClass;
    }

    /**
     * Returns a path in the tree of resource classes for the specified class.
     * The path starts with {@link SWGResourceClass#INSTANCE}, an instance of
     * {@link SWGResourceClassRoot}, and ends with the specified class. This
     * method returns {@code null} if the argument is {@code null} or if it
     * equals the synthetic root class, otherwise the path is valid because
     * there are no invalid constants in SWGAide.
     * 
     * @param rc a resource class constant
     * @return a tree path, or {@code null}
     */
    public static TreePath getTreePath(SWGResourceClass rc) {
        if (rc == null || rc == getRoot()) return null;

        List<SWGResourceClass> nl = new ArrayList<SWGResourceClass>();
        RCNode n = getNode(rc);
        while (n != null) {
            nl.add(n.resClass);
            n = n.parent;
        }
        Collections.reverse(nl);
        return new TreePath(nl.toArray());
    }
    
    /**
     * Returns an icon for the specified resource class. If the class does not
     * map to an icon this method returns an icon for the first parent class
     * that maps to an icon.
     * 
     * @param rc a resource class
     * @return an icon, never {@code null}
     * @throws NullPointerException if the argument is {@code null}
     */
    public static ImageIcon icon(SWGResourceClass rc) {
        if (rcIcons == null) makeIcons();
        ImageIcon i = rcIcons.get(rc);
        return i == null
                ? icon(getParent(rc)) // find parent with icon
                : i;
    }

    /**
     * Returns {@code true} if {@code node} is a leaf in the tree. See
     * {@link TreeModel#isLeaf(Object)}.
     * 
     * @param node a node in the tree
     * @return {@code true} if {@code rc} is a leaf
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public static boolean isLeaf(SWGResourceClass node) {
        if (node.isSpawnable() || node.isSpaceOrRecycled()) return true;

        return getNode(node).children.isEmpty();
    }

    /**
     * Helper method that instantiates and populates {@link #rcIcons}. This
     * method obtains images from swg/gui/common/images/ and creates icons which
     * are added to the map for the resource classes that are referenced by the
     * names of the images.
     */
    private static void makeIcons() {
        rcIcons = new HashMap<SWGResourceClass, ImageIcon>();
        try {
            for (String rn : ZReader.entries(new JarFile("SWGAide.jar"),
                    "swg/gui/common/images/", ".gif")) {

                if (rn.startsWith("rc_")) {
                    // file names are at the form rc_non-ferrous_metal.gif
                    // strip off initial rc_ and uppercase the string
                    String n = rn.substring(3, rn.length() - 4);
                    n = n.replace('_', ' ');
                    n = ZString.tac(n);
                    SWGResourceClass rc = SWGResourceClass.rc(n);

                    ImageIcon ic = new ImageIcon(SWGFrame.class.getResource(
                            "common/images/" + rn));

                    rcIcons.put(rc, ic);
                }
            }
        } catch (Throwable e) {
            SWGAide.printError("SWGResourceClassTree:makeIcons", e);
        }
    }

    /**
     * A helper type for a resource class tree node.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private static final class RCNode implements Comparable<RCNode> {

        /**
         * A sorted list of children under this node, or an empty list if this
         * node is a leaf. The sort order is what is default for resource
         * classes, see {@link SWGResourceClass#sortIndex()}.
         */
        final List<RCNode> children;

        /**
         * An integer that denotes the number of levels from the root to this
         * node; the four top resource classes are at level 1.
         */
        final int level;

        /**
         * The parent node for this node. If this is the root {@link #parent} is
         * {@code null} and {@link #level} is 0.
         */
        final RCNode parent;

        /**
         * The resource class for this node.
         */
        final SWGResourceClass resClass;

        /**
         * Creates an instance for the specified resource class. For the root
         * node the argument for parent is {@code null}. This constructor sets
         * {@code level = p.level + 1}, except for the root node which is level
         * 0. Furthermore, the created node is added to {@link #allNodes} and to
         * {@code p.children}.
         * 
         * @param p the parent node for this node
         * @param rc a resource class
         * @throws NullPointerException if {@code rc} is {@code null}
         */
        @SuppressWarnings("synthetic-access")
        private RCNode(RCNode p, SWGResourceClass rc) {
            parent = p;
            level = p == null
                    ? 0
                    : p.level + 1;

            resClass = rc;
            if (rc.isSpawnable() || rc.isSpaceOrRecycled())
                children = Collections.emptyList();
            else
                children = new ArrayList<RCNode>();

            if (parent != null) // not root
                parent.children.add(this);

            SWGResourceClassTree.allNodes.add(this);
        }

        @Override
        public int compareTo(RCNode o) {
            int ret = this.level - o.level;
            return ret != 0
                    ? ret
                    : this.resClass.compareTo(o.resClass);
        }

        @Override
        public String toString() {
            return resClass.rcName();
        }
    }

    // /**
    // * Test method that prints the tree to the specified stream.
    // *
    // * @param n the node to start with, no {@code null}
    // * @param ps a print stream
    // */
    // private static void prettyPrint(RCNode n, PrintStream ps) {
    // for (int i = 0; i < n.level; ++i)
    // ps.print("    ");
    //
    // ps.println(n);
    //
    // for (RCNode e : n.children)
    // prettyPrint(e, ps);
    // }
    //
    // /**
    // * Test
    // *
    // * @param args test
    // */
    // public static void main(String... args) {
    // SWGResourceClassTree rct = new SWGResourceClassTree();
    // prettyPrint(rct.root, System.out);
    // }
}
