package swg.gui.common;

import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * This type is a light weight list model for {@link JComboBox} and also plain
 * {@link JList}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGListModel extends AbstractListModel implements
        ComboBoxModel {

    /**
     * The current list of schematics.
     */
    private List<? extends Object> elements;

    /**
     * The selected item, or {@code null}.
     */
    private Object selectedElement;

    /**
     * Creates an instance of this type.
     */
    public SWGListModel() {
        super();
    }

    @Override
    public Object getElementAt(int index) {
        return elements != null
                ? elements.get(index)
                : null;
    }

    @Override
    public Object getSelectedItem() {
        return selectedElement;
    }

    @Override
    public int getSize() {
        return elements != null
                ? elements.size()
                : 0;
    }

    /**
     * Sets the content for this model to the specified list. The argument
     * should not be used further by the caller.
     * 
     * @param list a list of schematics
     */
    public void setElements(List<? extends Object> list) {
        elements = list;
        super.fireContentsChanged(this, 0, getSize() - 1);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if ((selectedElement != null && !selectedElement.equals(anItem))
                || selectedElement == null && anItem != null) {

            selectedElement = anItem;
            fireContentsChanged(this, -1, -1);
        }
    }
}
