package swg.gui.schematics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.crafting.schematics.SWGCategory;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.SWGFrame;
import swg.model.SWGCGalaxy;
import swg.swgcraft.SWGCraftCache.CacheUpdate;
import swg.swgcraft.SWGCraftCache.CacheUpdate.UpdateType;

/**
 * This type models a tree of categories for schematics based on the content of
 * categories and eventually schematics and items contained by a category. The
 * topmost levels are the Bazaar categories which all schematics eventually
 * pertains to, the lower levels are component categories as they are defined
 * and used in schematics in SWG.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGSchematicTreeModel implements TreeModel, UpdateSubscriber {

    /**
     * A flag that determines if empty category nodes are displayed or not; the
     * default value is {@code true} so the nodes are not displayed.
     */
    boolean hideEmptyCategoryNodes;

    /**
     * A list of schematics that is used to filter which schematics to display,
     * or {@code null} for no filtering. This list is set from the GUI when the
     * user selects something that asks for an updated model, either to a list
     * or to {@code null}.
     * <p>
     * Only schematics in this list should be provided by this model.
     */
    private List<SWGSchematic> matcher;

    /**
     * The list of model listeners
     */
    private Vector<TreeModelListener> modelListeners;

    /**
     * Creates an instance of this type which models the categories for items
     * and schematics in SWG.
     * 
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    SWGSchematicTreeModel() {
        hideEmptyCategoryNodes = ((Boolean) SWGFrame.getPrefsKeeper().get(
            "schemDraftHideEmptyNodes", Boolean.TRUE)).booleanValue();
        modelListeners = new Vector<TreeModelListener>();
        SWGSchematicsManager.addSubscriber(this);
    }

    /**
     * Helper method that adds all elements of the specified source list to the
     * target list. The source is unmodified and if the source is empty this
     * method does nothing.
     * 
     * @param src
     *            a source list
     * @param target
     *            a target list
     */
    private void addElements(List<? extends Object> src, List<TNode> target) {
        if (src.size() > 0) {
            Object[] a = src.toArray();
            for (Object o : a)
                target.add(new TNode(o));
        }
    }

    public void addTreeModelListener(TreeModelListener l) {
        if (!modelListeners.contains(l))
            modelListeners.add(l);
    }

    /**
     * Helper method that creates and returns a list of schematics that matches
     * {@link #matcher}, which is set via {@link #setSchematics(List)} triggered
     * by a user action. This means that each element in the returned list also
     * exists i the matcher list, if an element in the specified list does not
     * exist in the matcher list it is skipped. If the matcher is {@code null}
     * the returned list contains all elements. If the specified argument is
     * empty {@link Collections#EMPTY_LIST} is returned.
     * <p>
     * The purpose of this method is to support the GUI option to filter
     * schematics for profession and level.
     * 
     * @param schems
     *            a list of schematics
     * @return a filtered list of schematics
     */
    private List<SWGSchematic> filterSchematics(List<SWGSchematic> schems) {
        if (schems.size() <= 0)
            return schems;
        List<SWGSchematic> ret = new ArrayList<SWGSchematic>(schems);
        synchronized (this) { // matcher may be null so no synch on it
            if (matcher != null)
                ret.retainAll(matcher);
            return ret;
        }
    }

    public Object getChild(Object parent, int index) {
        if (parent instanceof TNode)
            return getChildElements((TNode) parent).get(index);

        return null;
    }

    public int getChildCount(Object parent) {
        if (parent instanceof TNode
            && ((TNode) parent).getContent() instanceof SWGCategory)
            return getChildElements((TNode) parent).size();

        return 0;
    }

    /**
     * Helper method which returns an ordered list of children of the specified
     * node, or an empty list. The returned list contains nodes with instances
     * of, in this order: {@link SWGCategory}, {@link SWGSchematic}, and
     * {@link String} for item names.
     * <p>
     * If {@link #hideEmptyCategoryNodes} is {@code true}, which is the default
     * value, this method recursively determines if the specified node and its
     * children contain at least one schematic or item. If not the empty list is
     * returned.
     * <p>
     * For categories this method invokes {@link #filterSchematics(List)} which
     * returns a list with respect to options set by the user.
     * 
     * @param node the node to return children for
     * @return an ordered list of children, or an empty list
     */
    private List<TNode> getChildElements(TNode node) {
    	SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        // only categories may have children
        if (node != null && node.getContent() instanceof SWGCategory) {
            ArrayList<TNode> ret = new ArrayList<TNode>();
            SWGCategory cat = (SWGCategory) node.getContent();
            if(cat.getType().equals(gxy.getType()) || cat.getType().equals("ALL") ) {
	            addElements(cat.getCategories(), ret);
	            addElements(filterSchematics(cat.getSchematics()), ret);
	            addElements(cat.getItems(), ret);
            }

            if (!hideEmptyCategoryNodes)
                return ret; // display all

            // else, reduce to hide but always include loot categories
            ArrayList<TNode> ret2 = new ArrayList<TNode>();
            for (TNode tn : ret) {
                if (tn.getContent() instanceof SWGSchematic
                        || tn.getContent() instanceof String
                        || getChildElements(tn).size() > 0
                        || (tn.getContent() instanceof SWGCategory
                                && SWGSchematicsManager.isSpecial((SWGCategory) tn.getContent()) ) )
                    ret2.add(tn); // is or has content >>> keep it
            }
            return ret2;
        }
        return Collections.emptyList();
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof TNode && child instanceof TNode) {
            TNode p = (TNode) parent;
            TNode c = (TNode) child;
            return getChildElements(p).indexOf(c);
        }
        return -1;
    }

    public Object getRoot() {
    	SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        SWGCategory c = SWGSchematicsManager.getCategory(0, gxy.getType());
        return c == null ? null : new TNode(c);
    }

    @Override
    public void handleUpdate(UpdateNotification u) {
        if (u instanceof CacheUpdate
            && ((CacheUpdate) u).type == UpdateType.SCHEMATICS)

            notifyListeners();
    }
    
    public boolean isLeaf(Object node) {
        if (node instanceof TNode
            && ((TNode) node).getContent() instanceof SWGCategory)
            return false;

        return true;
    }

    /**
     * Determines if the specified node is visible or if it is hidden by
     * filters. This method returns {@code true} if no filter is hiding the
     * node, {@code false} otherwise. Filters that affect the response are
     * hide-empty, profession, or profession level. If the content of the node
     * is neither a category or a schematic {@code false} is returned.
     * 
     * @param node
     *            the node to check
     * @return {@code true} if no filter hides the specified node
     */
    boolean isVisible(TNode node) {
        if (node.getContent() instanceof SWGCategory)
            return getChildElements(node).size() > 0;
        if (node.getContent() instanceof SWGSchematic) {
            SWGSchematic s = (SWGSchematic) node.getContent();
            return pathFromSchematicID(s.getID()) != null;
        }
        return false;
    }

    /**
     * Helper method which notifies all listeners about changes to this model.
     */
    private void notifyListeners() {
        Object root = getRoot();
        if (root != null) {
            TreeModelEvent evt = new TreeModelEvent(this, new TreePath(root));
            for (TreeModelListener listener : modelListeners)
                listener.treeStructureChanged(evt);
        }
    }

    /**
     * Helper method which returns a sorted node list of categories. If there is
     * an error {@code null} is returned.
     * 
     * @param c
     *            a category
     * @return a sorted list of nodes, or {@code null}
     */
    private List<TNode> pathFromCategory(SWGCategory c) {
    	SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        SWGCategory cc = c;
        List<TNode> nodes = new ArrayList<TNode>();
        while (cc != null) {
            nodes.add(new TNode(cc));
            if (cc.getID() < 0)
                return null; // error
            if (cc.getID() == 0)
                break; // top category aka "All"

            cc = SWGSchematicsManager.getCategory(cc.getParentID(), gxy.getType());
        }
        if (cc == null)
            return null;

        Collections.reverse(nodes); // top first
        return nodes;
    }

    /**
     * Creates and returns a tree path for the specified category. This method
     * returns {@code null} if the argument is invalid or if there is an error.
     * 
     * @param c a category
     * @return a tree path, or {@code null}
     */
    TreePath pathFromCategoryID(SWGCategory c) {
        if (c == null)
            return null;

        List<TNode> nodes = pathFromCategory(c);
        if (nodes == null)
            return null;
        return new TreePath(nodes.toArray());
    }

    /**
     * Creates and returns a tree path for the specified schematic ID. This
     * method returns {@code null} if the specified ID is invalid, if the
     * schematic does not exist in the current set of schematics set by the user
     * via {@link #setSchematics(List)}, or if there is an error.
     * 
     * @param sid
     *            a schematic ID
     * @return a tree path, or {@code null}
     */
    TreePath pathFromSchematicID(int sid) {
        if (sid < 0 || sid > SWGSchematicsManager.maxSchematicID())
            return null;

        SWGSchematic s = SWGSchematicsManager.getSchematic(sid);
        if (s == null || (matcher != null && !matcher.contains(s)))
            return null;
        
        SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        SWGCategory c = SWGSchematicsManager.getCategory(s.getCategory(), gxy.getType());
        List<TNode> nodes = pathFromCategory(c);
        if (nodes == null)
            return null;

        nodes.add(new TNode(s));
        return new TreePath(nodes.toArray());
    }

    public void removeTreeModelListener(TreeModelListener l) {
        modelListeners.remove(l);
    }

    /**
     * Sets the list of schematics that is used to filter which categories and
     * their children to display, or {@code null} for no filtering. This method
     * is invoked by a GUI client when the user selects something that asks for
     * an updated model. This method invokes all listeners about the change to
     * this model.
     * 
     * @param schems
     *            a list of schematics, or {@code null} to remove filtering
     */
    void setSchematics(List<SWGSchematic> schems) {
        synchronized (this) {
            matcher = schems;
        }
        notifyListeners();
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException(
            "Editing nodes in this tree is unsupported");
    }

    /**
     * A node in the tree. This type wraps an instance of {@link SWGCategory},
     * {@link SWGSchematic}, or {@link String} for item names.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    final class TNode {

        /**
         * The object that pertains to this node, an instance of
         * {@link SWGCategory}, {@link SWGSchematic}, or {@link String} for item
         * names.
         */
        private Object object;

        /**
         * Creates a node for the specified object. The argument an instance of
         * {@link SWGCategory}, {@link SWGSchematic}, or {@link String} for item
         * names.
         * 
         * @param o
         *            the object to wrap in this node
         */
        TNode(Object o) {
            object = o;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TNode)
                return object.equals(((TNode) obj).object);
            return false;
        }

        /**
         * Returns the object wrapped by this node, an instance of
         * {@link SWGCategory}, {@link SWGSchematic}, or {@link String} for item
         * names.
         * 
         * @return the object
         */
        Object getContent() {
            return object;
        }

        @Override
        public int hashCode() {
            return object.hashCode();
        }

        @Override
        public String toString() {
            if (object instanceof SWGCategory)
                return ((SWGCategory) object).getName();
            if (object instanceof SWGSchematic)
                return ((SWGSchematic) object).getName();
            return object.toString();
        }
    }
}
