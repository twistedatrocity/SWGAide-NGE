package swg.tools;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.Vector;

/**
 * A class that provides the developer with the means to setup one's own
 * traversal order
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class ZTraversalPolicy extends FocusTraversalPolicy {

    /**
     * A vector with the components in the order they are traversed
     */
    private Vector<Component> order;

    /**
     * Creates FocusTraversalPolicy that defines the order in which components
     * with a particular focus cycle root are traversed
     * 
     * @param order a vector with the components ordered in the way they should
     *        be traversed
     */
    public ZTraversalPolicy(Vector<Component> order) {
        this.order = new Vector<Component>(order.size());
        this.order.addAll(order);
    }

    @Override
    public Component getComponentAfter(Container focusCycleRoot,
            Component aComponent) {
        int idx = (order.indexOf(aComponent) + 1) % order.size();
        return order.get(idx);
    }

    @Override
    public Component getComponentBefore(Container focusCycleRoot,
            Component aComponent) {
        int idx = order.indexOf(aComponent) - 1;
        if (idx < 0) {
            idx = order.size() - 1;
        }
        return order.get(idx);
    }

    @Override
    public Component getDefaultComponent(Container focusCycleRoot) {
        return order.get(0);
    }

    @Override
    public Component getFirstComponent(Container focusCycleRoot) {
        return order.get(0);
    }

    @Override
    public Component getLastComponent(Container focusCycleRoot) {
        return order.lastElement();
    }
}
