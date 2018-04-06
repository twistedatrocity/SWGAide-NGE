package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Component;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import swg.SWGAide;
import swg.gui.common.SWGHelp;

/**
 * This GUI element displays various data about resource classes. One feature is
 * a tree-table which displays resource caps, the lower and upper caps, with
 * values over a tinted background. Another element displays which creatures
 * that drop a specified resource class, or similar information for other
 * classes.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
class SWGClassInfoTab extends JPanel {

    /**
     * A flag that denotes if the GUI parts of this element are created or not.
     * This type is created lazily and sets this member {@code true} when done.
     */
    private boolean isGuiCreated;

    /**
     * The tabbed pane that contains this panel
     */
    private SWGResourceTab resourceTab;

    /**
     * Creates the object for presenting capped resources
     * 
     * @param resourceTab the tabbed pane containing this object
     */
    SWGClassInfoTab(SWGResourceTab resourceTab) {
        this.resourceTab = resourceTab;

        helpPage = SWGAide.class.getResource(
                "docs/help_resources_rinfo_en.html");

        resourceTab.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });
    }

    /**
     * This method is called for two different action events, in both cases it
     * is determined if this component has focus and which action to select:
     * <UL>
     * <LI>if focus is changed for{code resourceTab}; this is when the user
     * selects the tab named "Resources", or any one of its siblings</LI>
     * <LI>if focus is changed for this component; this is when the user selects
     * the tab for {@code this} component, or any of its siblings</LI>
     * </UL>
     * <P>
     * If this component gains focus this implementation updates the GUI.
     * 
     * @param focused {@code true} if there is any focus change for this
     *        component or its siblings, or if {@link #resourceTab} gained
     *        focus; {@code false} otherwise
     */
    void focusGained(boolean focused) {
        if (focused && resourceTab.getSelectedComponent() == this) {
            if (!isGuiCreated) makeInterior();

            SWGAide.frame().putToLogbar_2(null);
            SWGHelp.push(helpPage);
        } else {
            SWGHelp.remove(helpPage);
        }
    }

    /**
     * Helper method which creates and returns the center GUI element. This
     * element is a tree-table which displays resource classes and their caps.
     * 
     * @return a GUI element
     */
    private Component makeCenter() {
        return new JScrollPane(new SWGTreeTable(), ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS );
    }
    /**
     * The URL for the help page for this view.
     */
    private final URL helpPage;


    /**
     * Helper method creates creates the content of this GUI element. This
     * method invokes helper make* methods for the various parts.
     */
    private synchronized void makeInterior() {
        if (isGuiCreated) return; // safety
        
        setLayout(new BorderLayout());
        add(makeNorth(), BorderLayout.PAGE_START);
        add(makeCenter(), BorderLayout.CENTER);
    }

    /**
     * Helper method which creates the north GUI element.
     * 
     * @return a GUI element
     */
    private Component makeNorth() {
        // TODO: add information panel
        return new JLabel();
    }
}
