package swg.gui.resources;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import swg.SWGAide;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.crafting.resources.ResourceUpdate;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGMutableResource;
import swg.crafting.resources.SWGPlanetAvailabilityInfo;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceClassTree;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.ResourceUpdate.UpdateType;
import swg.gui.SWGFrame;
import swg.model.SWGCGalaxy;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;
import swg.swgcraft.SWGResourceManager;
import swg.swgcraft.SWGSoapStatusResponse;

/**
 * This GUI component is for resource related stuff, the root and the main
 * container of all GUI components, except for the ISDroid GUI panel. This
 * implementation also provides a few features and some logic for the GUI
 * element it contains and which they share.
 * <p>
 * When initiated this type creates an instance of {@link SWGResController}
 * which is the main controller for tasks related to resources and to supervise
 * and manage guards, harvesters, monitors, and to provide access to collections
 * of resources for various purposes.
 * <p>
 * This implementation lazily instantiates the other GUI components, that is,
 * not until the user selects the GUI tab named "Resources" the GUI content of
 * this component is created.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings( { "serial" })
public final class SWGResourceTab extends JTabbedPane implements
        UpdateSubscriber {

    /**
     * The current galaxy; in particular, this is the galaxy for the character
     * who is currently selected at the main panel.
     */
    private static SWGCGalaxy galaxy;

    /**
     * The GUI panel for characters resource inventories.
     */
    private static SWGInventoryTab inventoryTab;

    /**
     * A helper flag which indicates if this GUI component is fully initialized.
     * Until the user has visited this GUI component it remains as a stub and
     * none of its components exists in any form.
     */
    private static boolean isGuiFinished = false;

    /**
     * A constant for this panel; this field is never {@code null} once SWGAide
     * is made visible.
     */
    static SWGResourceTab THIS;

    /**
     * A comparator which sorts resource collections on age, youngest first.
     */
    @SuppressWarnings( { "synthetic-access" })
    final ResourceAgeComparator ageComparator = new ResourceAgeComparator();

    /**
     * The controller which determines if there are resources which meet
     * resource guards and/or monitored resources which are depleted.
     */
    private final SWGResController controller;

    /**
     * The main GUI panel for viewing resources. This is the panel that is first
     * displayed for the user, displaying current resources.
     */
    private SWGCurrentTab currentResourcesTab;

    /**
     * The GUI panel for depleted resources, those marked depleted at
     * SWGCraft.org.
     */
    private SWGDepletedTab depletedTab;

    /**
     * The top-level frame for SWGAide, where everything is rooted.
     */
    private final SWGFrame frame;

    /**
     * The GUI panel for monitoring harvesters in the field.
     */
    private SWGHarvestingTab harvestingTab;

    /**
     * A helper flag which denotes if this GUI component is in focus. In
     * particular, if any panel in this container has focus this flag is {@code
     * true}.
     */
    private boolean hasFocus;

    /**
     * A helper flag which indicates if a background job is running in this
     * component, initiated by the event thread.
     */
    private boolean isWorking = false;

    /**
     * A tree model for resource classes.
     */
    private TreeModel resClassTreeModel;

    /**
     * The GUI panel for displaying resource class information such as upper and
     * lower cap, etc.
     */
    private SWGClassInfoTab resourceClassInfoTab;

    /**
     * A set of resources which are spawning at {@link #galaxy}.
     */
    private SWGResourceSet spawningResources;

    /**
     * The GUI panel for manually submitting resources to SWGCraft.org.
     */
    private SWGSubmitTab submitResourcesTab;

    /**
     * Creates an instance of this type, which is a tabbed pane for resource
     * related GUI components.
     * 
     * @param frame the frame holding this object
     */
    public SWGResourceTab(SWGFrame frame) {
        this.frame = frame;
        setOpaque(true);

        controller = new SWGResController(this);
        SWGResourceManager.addSubscriber(this);

        SWGCharacter ch = SWGFrame.getSelectedCharacter();
        galaxy = ch != null
                ? ch.gxy()
                : SWGCGalaxy.fromName((String) SWGFrame.getPrefsKeeper().get(
                		"optionMainGalaxy", SWGCGalaxy.defaultGalaxy().getName()));
        // XXX: replace galaxy-name in DAT file with galaxy-enum
        // 4 locations where optionMainGalaxy occurs

        // create interior lazily, see this#focusGained()
        frame.getTabPane().addChangeListener(new ChangeListener() {
            @SuppressWarnings("synthetic-access")
            public void stateChanged(ChangeEvent e) {
                focusGained();
            }
        });
        THIS = this;
    }

    /**
     * Called when the user selects to mark a resource depleted. This method
     * raises a GUI confirmation dialog and on a background worker thread it
     * dispatches to {@code sendDepleted(SWGKnownResource, SWGCGalaxy)}.
     * 
     * @param resource the depleted resource
     */
    void actionSendDepleted(final SWGKnownResource resource) {
        String msg = String.format(
                "Send a depleted-message to SWGCraft.org.%nResource: %s - %s",
                resource.getName(), resource.rc().rcName());
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                frame, msg, "Confirm depletion", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE)) {

            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            final ExecutorService exec = Executors.newSingleThreadExecutor();
            exec.execute(new Runnable() {

                @SuppressWarnings("synthetic-access")
                public void run() {
                    sendDepleted(resource);
                    exec.shutdown();
                }
            });
        }
    }

    /**
     * Creates and returns a menu item with an action listener which filters for
     * the specified resource class at the table of current resources. If the
     * argument is {@code null} no listener is attached and the menu item is
     * disabled. If experimental weights are specified the result is sorted by
     * the computed weights. If a known resource is specified and is part of the
     * filtered set it is selected.
     * 
     * @param rc a resource
     * @param wg experimental weights, or {@code null}
     * @param kr a resource to selected, or {@code null}
     * @return a menu item
     */
    JMenuItem currentFilterMenu(final SWGResourceClass rc,
            final SWGWeights wg, final SWGKnownResource kr) {

        JMenuItem c = new JMenuItem("Filter current");
        c.setToolTipText(String.format(
                "Filter Current Resources for resource class: %s", rc != null
                        ? rc.rcName()
                        : "none"));
        if (rc != null)
            c.addActionListener(new ActionListener() {
                @SuppressWarnings("synthetic-access")
                @Override
                public void actionPerformed(ActionEvent e1) {
                    if (!isGuiFinished) makeInterior();

                    setSelectedComponent(currentResourcesTab);
                    currentResourcesTab.currentFilterOnClass(rc, wg, kr);
                    frame.setCursor(Cursor.getPredefinedCursor(
                            Cursor.DEFAULT_CURSOR));
                }
            });
        c.setEnabled(rc != null);
        return c;
    }

    /**
     * Creates and returns a menu item with an action listener to select an
     * element at the table of current resources. The listener selects the
     * argument but if the argument is {@code null} or a depleted resource no
     * listener is attached and the menu item is disabled.
     * 
     * @param kr a resource
     * @return a menu item
     */
    JMenuItem currentSelectMenu(final SWGKnownResource kr) {
        JMenuItem c = new JMenuItem("Select current");
        c.setToolTipText(String.format(
                "Select \"%s\" at Current Resources panel", kr != null
                        ? kr.getName()
                        : "none"));
        if (kr != null && !kr.isDepleted())
            c.addActionListener(new ActionListener() {
                @SuppressWarnings("synthetic-access")
                @Override
                public void actionPerformed(ActionEvent e1) {
                    if (!isGuiFinished) makeInterior();

                    setSelectedComponent(currentResourcesTab);
                    currentResourcesTab.currentSelect(kr);
                    frame.setCursor(Cursor.getPredefinedCursor(
                            Cursor.DEFAULT_CURSOR));
                }
            });
        c.setEnabled(kr != null && !kr.isDepleted());
        return c;
    }

    /**
     * Helper method which silently tries to send current resources which do not
     * have an SWGCraft ID. These may be the result of a failed submission, if
     * SWGCraft was in error or if Internet was broken. If no ID-less element
     * exists in {@link #spawningResources} this method does nothing. This
     * method runs on an executor.
     */
    private void currentSend() {
        final SWGResourceSet noid = spawning().subsetBy(
                new Comparable<SWGKnownResource>() {
                    @Override
                    public int compareTo(SWGKnownResource o) {
                        return o.id() > 0
                                ? 1
                                : 0;
                    }
                });
        if (noid.isEmpty()) return;

        final ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(new Runnable() {
            public void run() {
                String s = "SWGResourceTab: submit resources that has no ID: ";
                try {
                    SWGSoapStatusResponse resp;
                    for (SWGKnownResource e : noid) {
                        resp = SWGResourceManager.sendNew(
                                new SWGMutableResource(e), false);
                        if (resp == null || resp.getStatus() == 999)
                            break;
                    }
                    s += noid.size();
                } catch (Exception e2) {/* ignore */
                    s += e2.getMessage();
                }
                SWGAide.printDebug("rtab", 1, s);
                exec.shutdown();
            }
        });
    }

    /**
     * Called when the user selects the tab "Resources" or any of its siblings.
     * This method is <i>not&nbsp;</i> called when any of the panels contained
     * within this component is selected. If the event pertains to this instance
     * this method determines if there is a change regarding the selected toon
     * and galaxy. If there is a change the set of current resources is updated.
     * Otherwise this method just performs some cleaning tasks.
     */
    private void focusGained() {
        if (!isWorking && frame.getTabPane().getSelectedComponent() == this) {

            final SWGCharacter toon = SWGFrame.getSelectedCharacter();
            if (toon == null) {
                JOptionPane.showMessageDialog(frame,
                        "Select a character at the \"Main\" tab\n ",
                        "No character", JOptionPane.INFORMATION_MESSAGE);
                frame.getTabPane().setSelectedIndex(0);
                return;
            }

            // only the event thread comes here, no synch necessary
            isWorking = true;
            hasFocus = true;

            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingUtilities.invokeLater(new Runnable() {

                @SuppressWarnings("synthetic-access")
                public void run() {
                    try {
                        focusGainedHelper(toon);
                    } catch (Exception e) {
                        SWGAide.printError("SWGResourceTab:focusGained", e);
                    }
                    isWorking = false;
                }
            });
        } else if (hasFocus) { // now rather, lost-focus
            hasFocus = false;
            if (isGuiFinished)
                focusSubPanels(false);
        } // else some other tab >>> don't care
    }

    /**
     * Helper method which is invoked when the user selects this GUI element.
     * This method invokes helper methods for the following tasks: <BR/>
     * * Creates the GUI elements contained in this tabbed pane, if it is not
     * done already. <BR/>
     * * Determines if current galaxy has changed. <BR/>
     * * Notifies the sub-panels.
     * 
     * @param toon the character who is most recently selected at the main
     *        panel, not {@code null}
     */
    private void focusGainedHelper(SWGCharacter toon) {
        if (!isGuiFinished) makeInterior();

        focusGalaxy(toon);
        focusSubPanels(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Helper method which method determines if another galaxy is selected and
     * if that is the case it updates the local set of resources. Some GUI
     * panels contained by this tabbed pane are notified.
     * <p>
     * <b>Note:</b> This method is non-blocking and promptly obtains a set of
     * resources from the resource manager. The first set may be aged but the
     * resource manager invokes a background task which determines if there is a
     * newer set available at SWGCraft.org. If that is the case the resource
     * manager updates SWGAide's internal cache and notifies all subscribers
     * that a new download is ready for the galaxy, including this element.
     * 
     * @param toon the character who is most recently selected at the main
     *        panel, not {@code null}
     */
    private void focusGalaxy(SWGCharacter toon) {
        SWGGalaxy g = toon.galaxy();
        frame.putToLogbar_1(String.format("%s @ %s @ %s",
                toon.getName(), g.getName(), g.station().getName()));

        if (g.gxy() == galaxy() && spawningResources != null)
            return; // no change or update is necessary

        galaxy = g.gxy();

        galaxyUpdated(galaxy);
        currentResourcesTab.updateDisplay();
    }

    /**
     * Notifies the contained tabs that this tabbed pane has gained or lost
     * focus.
     * 
     * @param isFocus {@code true} if this component is in focus, {@code false}
     *        otherwise
     */
    private void focusSubPanels(boolean isFocus) {
        controller.focusGained(isFocus);
        currentResourcesTab.focusGained(isFocus);
        inventoryTab().focusGained(isFocus);
        harvestingTab.focusGained(isFocus);
        submitResourcesTab.focusGained(isFocus);
        resourceClassInfoTab.focusGained(isFocus);
        depletedTab.focusGained(isFocus);
    }

    /**
     * Helper method which is called either if this component has gained focus
     * and the galaxy is initiated or changed, or if this component has received
     * an update notification for the specified galaxy.
     * <p>
     * <b>Note:</b> This implementation does nothing if the argument is
     * different than the current galaxy for this component, which can be the
     * case for update notifications. Thus the focus handling code must update
     * {@link #galaxy} before calling this helper method.
     * 
     * @param gxy the galaxy constant for the updated galaxy
     */
    private void galaxyUpdated(SWGCGalaxy gxy) {
        SWGResourceSet rs = SWGResourceManager.getSpawning(gxy);
        rs.sort(ageComparator); // sort before visible for others
        spawningResources = rs;
        if (depletedTab != null)
            depletedTab.resetGUI();
    }

    public void handleUpdate(UpdateNotification u) {
        final SWGCGalaxy gxy = (SWGCGalaxy) ((ResourceUpdate) u).optional;
        if (isGuiFinished && gxy == galaxy()) {

            SwingUtilities.invokeLater(new Runnable() {

                @SuppressWarnings("synthetic-access")
                public void run() {
                    galaxyUpdated(gxy);
                    currentUpdateGUI();
                    currentSend();
                }
            });
        }
        SWGDepletedTab.writeAuto(gxy); // always
    }

    /**
     * Returns a GUI dialog for activating defined harvesters.
     * 
     * @return a GUI dialog
     */
    SWGHarvesterActivateDialog harvesterActivateDialog() {
        return new SWGHarvesterActivateDialog(harvestingTab);
    }

    /**
     * Helper method which creates the interior of this GUI component. That is,
     * an empty stub of this instance is already created. This method creates
     * instances of these panels one after another and adds them to this tabbed
     * pane. If there is a panel which in its turn is created lazily only its
     * stub is created by this call.
     */
    private synchronized void makeInterior() {
        if (isGuiFinished) return;

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        currentResourcesTab = new SWGCurrentTab(this);
        inventoryTab = new SWGInventoryTab(this);
        harvestingTab = new SWGHarvestingTab(this);
        submitResourcesTab = new SWGSubmitTab(this);
        resourceClassInfoTab = new SWGClassInfoTab(this);
        depletedTab = new SWGDepletedTab(this);

        // add them after all are created, not to trigger GUI action events
        // until all are at least a pre-state
        add("Current Resources", currentResourcesTab);
        add("Inventory", inventoryTab);
        add("Harvesting", harvestingTab);
        add("Submit Resources", submitResourcesTab);
        add("Resource Info", resourceClassInfoTab);
        add("Depleted Resources", depletedTab);

        this.setMnemonicAt(0, KeyEvent.VK_U);
        this.setMnemonicAt(1, KeyEvent.VK_I);
        this.setMnemonicAt(2, KeyEvent.VK_V);
        this.setMnemonicAt(3, KeyEvent.VK_B);
        this.setMnemonicAt(4, KeyEvent.VK_N);
        this.setMnemonicAt(5, KeyEvent.VK_D);

        isGuiFinished = true;
    }

    /**
     * Returns a tree model for resource classes. Because resource classes never
     * change during run-time, they are static, this model is shared between all
     * clients. Of the same reason, the tree and the nodes are immutable, this
     * type is inherently thread safe, those methods that return mutable
     * collections rather return copies. The model is a local instantiation of
     * {@link SWGResourceClassTree}.
     * 
     * @return a tree model for resource classes
     */
    @SuppressWarnings("synthetic-access")
    TreeModel resClassTreeModel() {
        synchronized (this) {
            if (resClassTreeModel == null)
                resClassTreeModel = new ResClassTreeModel();
            return resClassTreeModel;
        }
    }
    

    /**
     * Helper method which sends a depleted-message for the specified resource
     * to SWGCraft.org via {@link SWGResourceManager}. If the resource is
     * successfully marked as depleted the local resource cache is updated. If
     * there is an error this method raises a GUI dialog and informs the user.
     * 
     * @param resource the depleted resource
     */
    private void sendDepleted(final SWGKnownResource resource) {
        SWGSoapStatusResponse resp = SWGResourceManager.sendDeplete(
                resource, System.currentTimeMillis() / 1000);

        if (!resp.isFaultless()) {
            String msg = String.format(
                    "Failed to deplete %s - %s%nat SWGCraft.org%n%n%s",
                    resource.getName(), resource.rc().rcName(), resp
                            .getFaultMessage() != null
                            ? resp.getFaultMessage()
                            : resp.getStatusString());

            JOptionPane.showMessageDialog(
                    frame, msg, "Error", JOptionPane.ERROR_MESSAGE);

        } else
            SWGResourceManager.notifySubscribers(new ResourceUpdate(
                    UpdateType.LOCAL_SUBMISSION, resource.galaxy()));

        SwingUtilities.invokeLater(new Runnable() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                frame.setCursor(
                        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    /**
     * Returns the resource set for the current galaxy. If the set is {@code
     * null} or empty {@link SWGResourceSet#EMPTY} is returned.
     * <p>
     * <b>Note:</b> The returned set must not be mutated but the client must
     * create a copy.
     * 
     * @return the spawning resources for the current galaxy, or
     *         {@link SWGResourceSet#EMPTY}
     */
    SWGResourceSet spawning() {
        if (!isGuiFinished)
            galaxyUpdated(SWGFrame.getSelectedGalaxy());
        return spawningResources == null
                ? SWGResourceSet.EMPTY
                : spawningResources;
    }

    /**
     * Helper method which parses the specified string array for names of stats
     * and sets their index respectively in the specified integer array.
     * <p>
     * In particular, the updated array is the conversion order for each
     * individual stat relative the order of the model of the stats in SWGAide,
     * which is alphabetical: CD,CR,DR,ER,FL,HR,MA,OQ,PE,SR,UT <BR/>
     * For example, the resulting array [3,0,2,1,...] reads that the stats in
     * the specified string array are ordered ER,CD,DR,CR,...
     * <p>
     * Note, the string array may contain several elements prior to or after the
     * stats this method is parsing. The argument {@code beginAt} determines
     * which is the first index to examine. This method reads no more than
     * {@link Stat#COUNT} elements starting at {@code beginAt}.
     * 
     * @param statsOrder the integer array to update, this argument is untouched
     *        until success is determined
     * @param splitted an array of strings, this argument is just read
     * @param beginAt the entry point in the string array
     * @throws IllegalArgumentException if there is an invalid argument or
     *         something parsed
     * @throws NullPointerException if an argument is {@code null}
     */
    void statsToDefaultOrder(int[] statsOrder, String[] splitted, int beginAt) {
        if (beginAt < 0 || statsOrder.length != Stat.COUNT
                || splitted.length - beginAt < Stat.COUNT)
            throw new IllegalArgumentException(String.format(
                    "Invalid argument: %s %s %d", statsOrder.toString(),
                    splitted.toString(), Integer.valueOf(beginAt)));

        // use a -1 filled temporary array to shelter the argument while parsing
        int[] temp = new int[Stat.COUNT];
        Arrays.fill(temp, -1);

        for (int i = 0; i < Stat.COUNT; ++i) {
            // some spreadsheet applications quotes elements in CSV files
            String str = splitted[i + beginAt].replace("\"", "").trim();

            // find the index for the stat
            for (Stat s : Stat.values()) {
                if (s.name().equalsIgnoreCase(str)) {
                    if (temp[i] != -1) {
                        // already been here >>> error
                        throw new IllegalArgumentException(String.format(
                                "Stat %s is doubled", s.name()));
                    }
                    temp[i] = s.i;
                    break;
                }
            }
            if (temp[i] < 0)
                throw new IllegalArgumentException(String.format( // no stats
                        "Unrecognized stat \"%s\"", str));
        }

        // fine so far, fill the argument
        for (int i = 0; i < statsOrder.length; ++i)
            statsOrder[i] = temp[i];
    }

    /**
     * Tints the specified GUI tab determined by the alertLevel. The level must
     * be negative, zero, or positive, which means alarm, OK, or alert
     * respectively.
     * <p>
     * Index 0 is Current Resources and an alarm indicates a monitor for a
     * depleted resource but the alert is for a resource guard.
     * <p>
     * Index 2 is Harvesting and an alarm denotes idling while an alert denotes
     * indicates that a harvester begins to idle soon.
     * 
     * @param alertLevel the level for the update
     * @param tabIndex the tab to tint
     * @throws IndexOutOfBoundsException if tabIndex is invalid
     */
    void tintTab(int alertLevel, int tabIndex) {
        if (isGuiFinished) {
            if (alertLevel > 0)
                setBackgroundAt(tabIndex, Color.YELLOW);
            else if (alertLevel < 0)
                setBackgroundAt(tabIndex, Color.PINK);
            else
                setBackgroundAt(tabIndex, null);
        }
    }

    /**
     * Filters the inventory for the specified resource class, or if the class
     * argument is {@code null} the listener clears the current selection and
     * exits, ignoring any other argument. If {@code select} is {@code true}
     * this method flips to the inventory tab.
     * <p>
     * If experimental weights are specified they update the filter fields. If a
     * resource is specified and exists exists in the filtered inventory it is
     * selected, otherwise.
     * 
     * @param cls a resource class, or {@code null}
     * @param wg experimental weights, or {@code null}
     * @param kr a resource to selected, or {@code null}
     * @param select make the inventory panel selected
     */
    static void actionInventoryFilter(SWGResourceClass cls, SWGWeights wg,
            SWGKnownResource kr, boolean select) {

        inventoryTab(); // ensure inventory-tab is ready

        if (select) THIS.setSelectedComponent(inventoryTab);
        inventoryTab.actionFilterInventory(cls, wg, kr);
    }

    /**
     * Invokes {@link SWGCurrentTab#updateDisplay()}; if the GUI component for
     * current resources is not yet created this method does nothing.
     */
    static void currentUpdateGUI() {
        if (isGuiFinished) THIS.currentResourcesTab.updateDisplay();
    }

    /**
     * @return the galaxy
     */
    static SWGCGalaxy galaxy() {
        return galaxy;
    }

    /**
     * Creates and returns a menu item with an action listener which filters the
     * table display for the specified resource class, or if the class argument
     * is {@code null} the listener clears the current selection and exits,
     * ignoring any other argument.
     * <p>
     * If experimental weights are specified they update the filter fields. If a
     * resource is specified and exists exists in the filtered inventory it is
     * selected, otherwise, if the resource is {@code null} and the boolean
     * argument is {@code true} the returned menu item is disabled.
     * 
     * @param cls a resource class, or {@code null}
     * @param wg experimental weights, or {@code null}
     * @param kr a resource to selected, or {@code null}
     * @param select make the inventory panel selected
     * @param disable disable the menu item if the resource is {@code null}
     * @return the menu item
     */
    static JMenuItem inventoryFilterMenu(final SWGResourceClass cls,
            final SWGWeights wg, final SWGKnownResource kr,
            final boolean select, boolean disable) {

        JMenuItem selInv = new JMenuItem("Filter inventory");
        selInv.setToolTipText(cls == null
                ? "No resource class >> clear inventory selection"
                : "Filter inventory for resource class: " + cls.rcName());
        if (kr == null && disable)
            selInv.setEnabled(false);
        else {
            selInv.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e1) {
                    actionInventoryFilter(cls, wg, kr, select);
                }
            });
        }

        return selInv;
    }

    /**
     * Helper method which returns the resource inventory panel. If
     * {@link #inventoryTab} is {@code null} a stub is created and returned,
     * otherwise the value is returned as is which is a stub or a readied panel.
     * 
     * @return the resource inventory panel
     */
    static SWGInventoryTab inventoryTab() {
        if (inventoryTab == null) THIS.makeInterior();
        THIS.frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return inventoryTab;
    }

    /**
     * This type models the tree of resource classes in SWG and is an
     * instantiation of {@link SWGResourceClassTree}.
     * <p>
     * All getters return an instance of {@link SWGResourceClass}.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    static class ResClassTreeModel implements TreeModel {

        /**
         * A list of tree model listeners.
         */
        private final Vector<TreeModelListener> listeners;

        /**
         * Creates an instance of this type.
         */
        private ResClassTreeModel() {
            listeners = new Vector<TreeModelListener>();
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            if (!listeners.contains(l)) listeners.add(l);
        }

        @Override
        public Object getChild(Object parent, int index) {
            return SWGResourceClassTree.getChild(
                    (SWGResourceClass) parent, index);
        }

        @Override
        public int getChildCount(Object parent) {
            return SWGResourceClassTree.getChildCount(
                    (SWGResourceClass) parent);
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return SWGResourceClassTree.getIndexOfChild(
                    (SWGResourceClass) parent, (SWGResourceClass) child);
        }

        @Override
        public Object getRoot() {
            return SWGResourceClassTree.getRoot();
        }

        @Override
        public boolean isLeaf(Object node) {
            return SWGResourceClassTree.isLeaf((SWGResourceClass) node);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            // pass
        }
    }

    /**
     * Comparator for resources, sort order is age only. In particular, the
     * comparator compares the age of the resources which is stated in UTC time
     * (number of seconds since the epoch January, 1, 1970) and the return value
     * <i>is not&nbsp;</i> oldest first but it is youngest first. Thus, the
     * return value is
     * <tt>r2<sub><i>age</i></sub> - r1<sub><i>age</i></sub></tt>.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final static class ResourceAgeComparator
            implements Comparator<SWGKnownResource> {

        /**
         * Compares its two arguments for order. Returns a negative integer,
         * zero, or a positive integer as the first argument is <b>younger</b>
         * than, equal to, or greater than the second.
         */
        public int compare(SWGKnownResource r1, SWGKnownResource r2) {
            SWGPlanetAvailabilityInfo fa1 = r1.availableFirst();
            SWGPlanetAvailabilityInfo fa2 = r2.availableFirst();
            return (int) (fa2.available() - fa1.available());
        }
    }
}
