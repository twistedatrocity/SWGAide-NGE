package swg.gui.resources;

import java.applet.AudioClip;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import swg.SWGAide;
import swg.crafting.SWGValues;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.crafting.resources.ResourceUpdate;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGPlanetAvailabilityInfo;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceFilter;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.types.SWGCreatureResources;
import swg.crafting.resources.types.SWGEgg;
import swg.crafting.resources.types.SWGOrganic;
import swg.crafting.resources.types.SWGSeafood;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDecoratedTableCellRenderer;
import swg.gui.common.SWGDoTask;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGDecoratedTableCellRenderer.TableCellDecorations;
import swg.gui.common.SWGDoTask.TaskCallback;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.swgcraft.SWGPets;
import swg.swgcraft.SWGResourceManager;
import swg.tools.ZHtml;
import swg.tools.ZNumber;
import swg.tools.ZString;

/**
 * This type is the controller for all the collections which are related to
 * resources. Many of these are accessed within the Resources section but some
 * collections and supportive features have global visibility. This type does
 * not replace the resource manager which handles all the communication with
 * SWGCraft.org and the local cache of known resources; this type is in some
 * manner the controller in the MVC pattern and controls the access and mutation
 * of collections related to resources.
 * <p>
 * This type manages the collections of of defined resource guards, monitors,
 * resource inventories, harvesters and their related collections of users. When
 * a communication with SWGCraft.org is complete this type scans the collections
 * and if anything is triggered this type handles alerts and alarms via sound
 * and visible effects.
 * <p>
 * Technically, this controller does not execute on any timer but update
 * notification from the resource manager triggers another scan. No matter the
 * kind of notification this type scans all collections. The only entry point is
 * {@link #check()} which is invoked only when SWGAide is first instantiated,
 * when the resource tab gains focus, if a collection is mutated, and when an
 * update notification is received.
 * <p>
 * A scan just iterates over the guards for just the <i>current galaxy&nbsp;</i>
 * and/or the user's selected main galaxy, but it iterates over <i>all&nbsp;</i>
 * active harvesters and monitors. For each galaxy with active harvesters and
 * monitors a set of current resources is requested from the resource manager;
 * this may trigger a download from SWGCraft.org which, once finished, fires off
 * another notification that triggers a new scan.
 * <p>
 * On purpose this type just scans resource guards for the current galaxy. The
 * reason is to dodge unwanted downloads from SWGCraft when there are guards
 * which the user has forgotten about. Such a galaxy is automatically updated
 * when the user visits the "Resources" with this galaxy selected. It is assumed
 * that harvesters and monitors are actively pandered to by the user.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGResController implements UpdateSubscriber {

    /**
     * A GUI dialog for creating resource guards.
     */
    private static SWGGuardDialog guardDialog;

    /**
     * A map where galaxy constant map to lists of resource guards.
     */
    private static Map<SWGCGalaxy, List<SWGGuard>> guards;

    /**
     * A map where galaxy constants map to lists of harvesters.
     */
    private static Map<SWGCGalaxy, List<SWGHarvester>> harvesters;

    /**
     * A map of defined harvester owners, mapped to by galaxy.
     */
    private static Map<SWGCGalaxy, List<SWGHarvesterOwner>> harvOwners;

    /**
     * A map of resource inventory maps which are mapped to by galaxy constants.
     * An inventory map contains a number of unique assignees, each assignee
     * having a set of wrappers. The wrappers are contained in a list but is
     * maintained as a set.
     */
    private static Map<SWGCGalaxy, Map<String, List<SWGInventoryWrapper>>> inventoryMap;

    /**
     * A comparator that determines if a resource is from Kashyyyk or Mustafar,
     * however, it ignores if the resource is a Creature Resource.
     */
    private static Comparable<SWGKnownResource> kmComparator;

    /**
     * A lock object used by some methods must synchronize but which do not need
     * an object-wide lock.
     */
    private static final String LOCK = "check";

    /**
     * The maximum life time for a monitor, defined to 28 days. The maximum age
     * for resources are 22 days but a player may be out of town for a few days
     * and should be alerted. The time is in seconds.
     * <p>
     * This field does not affect active harvesters, they are active until the
     * user shuts them down.
     */
    private static final long MONITOR_MAX_AGE = 28L * 24 * 60 * 60;

    /**
     * A map where galaxy constants map to lists of active monitors.
     */
    private static Map<SWGCGalaxy, List<SWGMonitor>> monitors;

    /**
     * This field is used for coloring the Resources tab and its default value
     * is {@code null}. Only when {@link #check(SWGCGalaxy)} is working this
     * field may take a color, see {@link #guiStatus(int, boolean)}. Once check
     * is done this field is reset.
     */
    private static Color statusColor;

    /**
     * A reference to this instantiated type.
     */
    private static SWGResController THIS;

    /**
     * The sound clip for "gloomy alarms".
     */
    private AudioClip alarm;

    /**
     * The time when the sound clip {@code alarm} was played most recently. This
     * field is used to avoid excessive alarms, if the next alarm is triggered
     * within the grace period it is muted.
     */
    private long alarmRecentmost = 0L;

    /**
     * The sound clip for "happy alerts".
     */
    private AudioClip alert;

    /**
     * The time when the sound clip {@code alert} was played most recently. This
     * field is used to avoid excessive alerts, if the next alert is triggered
     * within the grace period it is muted.
     */
    private long alertRecentmost = 0L;

    /**
     * The sound clip for "warnings".
     */
    private AudioClip aWarn;

    /**
     * A helper flag which denotes if there is a scan in process. If this flag
     * is {@code true} consecutive calls are silently canceled.
     */
    private boolean isScanning = false;

    /**
     * The parent component for this instance.
     */
    private final SWGResourceTab resourceTab;

    /**
     * Creates an instance which is supervising defined resource guards,
     * harvesters, and monitors.
     * 
     * @param parent the parent component for this instance
     */
    @SuppressWarnings("unchecked")
    SWGResController(SWGResourceTab parent) {
        resourceTab = parent;
        THIS = this;

        kmComparator = kmResourceComparator();

        guards = (Map<SWGCGalaxy, List<SWGGuard>>)
                SWGFrame.getPrefsKeeper().get(
                        "resourceGuardMap",
                        new HashMap<SWGCGalaxy, List<SWGGuard>>());

        harvesters = (Map<SWGCGalaxy, List<SWGHarvester>>)
                SWGFrame.getPrefsKeeper().get(
                        "resourceActiveHarvesterMap",
                        new HashMap<SWGCGalaxy, List<SWGHarvester>>());

        inventoryMap =
                (Map<SWGCGalaxy, Map<String, List<SWGInventoryWrapper>>>)
                SWGFrame.getPrefsKeeper().get(
                        "resourceInventoryMap",
                        new HashMap<SWGCGalaxy,
                        Map<String, List<SWGInventoryWrapper>>>());

        monitors = (Map<SWGCGalaxy, List<SWGMonitor>>)
                SWGFrame.getPrefsKeeper().get(
                        "resourceMonitorMap",
                        new HashMap<SWGCGalaxy, List<SWGMonitor>>());

        harvOwners = (HashMap<SWGCGalaxy, List<SWGHarvesterOwner>>)
                SWGFrame.getPrefsKeeper().get(
                        "resourceHarvesterOwnerMap",
                        new HashMap<SWGCGalaxy, List<SWGHarvesterOwner>>());

        URL u = SWGAide.class.getResource("gui/docs/utinni.wav");
        alert = java.applet.Applet.newAudioClip(u);

        u = SWGAide.class.getResource("gui/docs/alarm.wav");
        alarm = java.applet.Applet.newAudioClip(u);

        u = SWGAide.class.getResource("gui/docs/sithdecel.wav");
        aWarn = java.applet.Applet.newAudioClip(u);

        // it is the update notifications that triggers the supervising actions
        SWGResourceManager.addSubscriber(this);

        SWGAide.frame().addExitCallback(new SWGDoTask(new TaskCallback() {
            @SuppressWarnings("synthetic-access")
            public void execute() {
                doExit();
            }
        }));
    }

    /**
     * Helper method which notifies the user an alarm or warning. This method is
     * invoked when a harvester needs attention or when a monitor is triggered.
     * The notification is a text at the status bar and a sound clip and it is
     * handled by {@link #alertGUI(String, String, AudioClip)}. The action is
     * determined by {@code kind} which takes one of these values:
     * <dl>
     * <dt>depleted</dt>
     * <dd>a monitor is triggered for a depleted resource</dd>
     * <dt>harvester</dt>
     * <dd>a harvester is idling, either for a depleted resource, or out of
     * power or maintenance</dd>
     * <dt>warning</dt>
     * <dd>a harvester will soon begin idling, the time is specified by the user
     * and the cause is either low power or low maintenance</dd>
     * </dl>
     * <p>
     * The boolean value determines if a sound should be emitted; this allows
     * for logics to invoke this method repeatedly but only call for sound its
     * last invocation. Any call to this method is assumed to already be
     * synchronized.
     * 
     * @param kind a string denoting the kind of action
     * @param useAlarm {@code false} to disallow sound
     */
    private void alarm(final String kind, final boolean useAlarm) {
        final Timer t = new Timer(6000, new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                if (kind.equals("harvester"))
                    alertGUI("Harvester alarm", "aa0000", useAlarm
                            ? alarm
                            : null);
                else if (kind.equals("depleted"))
                    alertGUI("Resource depleted", "aa0000", useAlarm
                            ? alarm
                            : null);
                else if (kind.equals("warning"))
                    alertGUI("Harvester warning", "ff7f24", useAlarm
                            ? aWarn
                            : null);
            }
        });
        t.setRepeats(false);
        t.start();
    }

    /**
     * Helper method which alerts the user when a guard, harvester, or monitor
     * is triggered. This method displays the specified message at SWGAide's
     * status bar and plays the specified sound clip. This method limits the
     * sound alerts to just one per type (alert and alarm) per the time period
     * that is obtained from {@link #alertSilentTime()}.
     * 
     * @param msg the message to display at the status bar
     * @param color the color code for the message (RGB)
     * @param clip the sound clip to play, or {@code null} for no sound
     */
    private void alertGUI(String msg, String color, AudioClip clip) {
        synchronized (alarm) { // synchronize on small scope
            SWGAide.frame().putToStatbar(String.format(
                    "<html><font color=#%s>%s</font></html>", color, msg));

            if (clip == null)
                return;

            // sounds the alert/alarm
            // obey the grace period per kind of sound
            long ctm = System.currentTimeMillis();
            if ((clip == alarm || clip == aWarn)
                    && ctm > alarmRecentmost + alertSilentTime() * 60L * 1000) {
                alarmRecentmost = ctm;
            } else if (clip == alert
                    && ctm > alertRecentmost + alertSilentTime() * 60L * 1000) {
                alertRecentmost = ctm;
            } else
                return;

            clip.play(); // once
        }
    }

    /**
     * Helper method which iterates over the resource guards for the specified
     * galaxy and it iterates over all galaxies which have active harvesters and
     * monitors. If the safety flag {@code isScanning} is {@code true} this
     * method does nothing, otherwise this method synchronizes on {@code LOCK}
     * and executes on a worker thread.
     * <p>
     * For a triggered guard this method invokes {@code alert(true)}.<br/>
     * For a triggered monitor it invokes {@code alarm("depleted", true)}.
     * 
     * @param galaxy the galaxy to scan guards for; all active harvesters and
     *        monitors are always scanned, no matter the galaxy
     */
    private void check(final SWGCGalaxy galaxy) {
        if (isScanning)
            return;
        synchronized (LOCK) {
            isScanning = true;
            SwingUtilities.invokeLater(new Runnable() {

                @SuppressWarnings("synthetic-access")
                public void run() {
                    int g = guardsCheck(galaxy);
                    if (g != 0) {
                        guiStatus(1, false);
                        alertGUI("Resource alert", "00aa00", g < 0
                                ? alert
                                : null);
                    }

                    if (monitorsCheck()) {
                        guiStatus(-1, false);
                        alarm("depleted", true);
                    }

                    guiStatus(harvestersCheck(), true);

                    isScanning = false;
                }
            });
        }
    }

    /**
     * This method performs some exit routines when SWGAide is closing down. In
     * particular, this method iterates over the collections of resource guards,
     * harvesters, and monitors, and if for a galaxy any list is empty then it
     * is removed from its containing map.
     */
    private void doExit() {
        synchronized (LOCK) {
            exitHelper(guards);
            exitHelper(harvesters);
            exitHelper(harvOwners);
            exitHelper(monitors);
        }
    }

    /**
     * Helper method to {@code doExit()}. This method iterates over the galaxies
     * in the argument and if a galaxy maps to an empty list the mapping is
     * removed.
     * 
     * @param <T> the type which to scan for
     * @param gxyMap a map having {@link SWGCGalaxy} as keys
     */
    private <T> void exitHelper(Map<SWGCGalaxy, List<T>> gxyMap) {
        Iterator<SWGCGalaxy> iter;
        if (gxyMap != null)
            for (iter = gxyMap.keySet().iterator(); iter.hasNext();)
                if (gxyMap.get(iter.next()).isEmpty())
                    iter.remove();
    }

    /**
     * This method is called if focus is changed for{@code resourceTab}; this is
     * when the user selects the tab named "Resources", or any one of its
     * siblings. If the argument is {@code true} that component gained focus and
     * this instance triggers a scan of the guards for the current galaxy.
     * Otherwise this method does nothing.
     * 
     * @param focused {@code true} if the {@code resourceTab} gained focus
     */
    void focusGained(boolean focused) {
        if (focused)
            synchronized (LOCK) {
                check(SWGResourceTab.galaxy());
            }
    }

    /**
     * {@inheritDoc}
     * <p>
     * No matter the kind of notification, as it is assumed to be related to a
     * change in some set of resources, this method is one of the two trigger
     * points for a scan and initiates a scan of all resource guards for the
     * current galaxy and a scan of all galaxies which have active harvesters
     * and active monitors. The other entry point is {@code check(boolean)}.
     */
    public void handleUpdate(UpdateNotification u) {
        ResourceUpdate uu = (ResourceUpdate) u;

        // This method dispatches to its helper method on every call, see the
        // class JDocu comments
        check((SWGCGalaxy) uu.optional);
    }

    /**
     * Helper method which iterates over the university and for each galaxy it
     * invokes {@code checkHarvesters(SWGCGalaxy)}. If there is a harvester
     * which is triggered this method calls {@code alarm(String, boolean)} with
     * "harvester" or "warning". Thus, the GUI is updated and the user is
     * notified. Finally the return value is returned.
     * 
     * @return zero if all harvesters are OK, {@code < 0} if a harvester is
     *         idling, {@code > 0} if a harvester soon begin to idle
     */
    private int harvestersCheck() {
        synchronized (LOCK) {
            int triggered = 0;
            for (SWGCGalaxy g : harvesters.keySet())
                if (g != SWGResourceTab.galaxy()) // defer current gxy somewhat
                    triggered |= harvestersCheckHelper(g);

            // always last to possibly update GUI for current galaxy
            triggered |= harvestersCheckHelper(SWGResourceTab.galaxy());

            if (triggered < 0)
                alarm("harvester", true);
            else if (triggered > 0)
                alarm("warning", true);
            return triggered;
        }
    }

    /**
     * Helper method which invokes {@link #harvestersCheck(SWGCGalaxy)} and then
     * invokes {@link SWGResourceTab#tintTab(int, int)} with the result. The net
     * effect is that the tab for the harvesting panel is tinted for alarm,
     * warning, or neutral state. Finally the return value is returned.
     * 
     * @param gxy a galaxy constant
     * @return zero if all harvesters are OK, {@code < 0} if a harvester is
     *         idling, {@code > 0} if a harvester soon begin to idle
     */
    private int harvestersCheckHelper(SWGCGalaxy gxy) {
        int ret = harvestersCheck(gxy);
        resourceTab.tintTab(gxy == SWGResourceTab.galaxy()
                ? ret
                : 0, 2);

        return ret;
    }

    /**
     * Returns the number of minutes for muting alert/alarm/warning sounds. For
     * constantly muted sound this method returns {@link Integer#MAX_VALUE}.
     * 
     * @return mute time in minutes
     */
    static int alertSilentTime() {
        return ((Integer) SWGFrame.getPrefsKeeper().get(
                "resourceAlertWarningMute", Integer.valueOf(10))).intValue();
    }

    /**
     * Sets the number of minutes to mute alert/alarm/warning. If the argument
     * is {@link Integer#MAX_VALUE} sound is constantly muted.
     * 
     * @param muteMinutes amount of minutes to mute sound
     */
    static void alertSilentTime(int muteMinutes) {
        if (muteMinutes <= 0) throw new IllegalArgumentException(
                "Invalid time: " + muteMinutes);
        SWGFrame.getPrefsKeeper().add(
                "resourceAlertWarningMute", Integer.valueOf(muteMinutes));
        // this makes it possible to in the future let user specify mute-time
    }

    /**
     * This method initiates a scan of all resource guards for the current
     * galaxy and all active harvesters and monitors for all galaxies. If a
     * guard, harvester or monitor triggers alerts and/or alarms are invoked and
     * the GUI is updated properly.
     */
    public static void check() {
        // always do global scan, the GUI may indicate alerts/alarms also for
        // other galaxies and should not be neutralized when current galaxy
        // is "fixed"
        THIS.check(SWGResourceTab.galaxy());
    }

    /**
     * Determines if the collection contains the specified guard. This
     * implementation returns {@code true} if there is a guard in the specified
     * collection which has a name and resource class which equals the specified
     * guard's name and resource class. If no guard in the collection meet these
     * two identifiers {@code false} is returned.
     * 
     * @param coll the collection to scan
     * @param guard the guard to check for
     * @return {@code true} if the collections contains a guards which has the
     *         same name and resource class
     */
    public static boolean contains(Collection<SWGGuard> coll, SWGGuard guard) {
        for (SWGGuard g : coll)
            if (g.rc() == guard.rc()
                    && g.getName().equals(guard.getName()))
                return true;

        return false;
    }

    /**
     * Creates and returns a menu item for opening a web browser with a page for
     * the specified resource class. If a web browser cannot be opened the error
     * is caught and written to SWGAide's log file and the user is informed.
     * 
     * @param cr a creature resource class
     * @return a menu item
     */
    private static JMenuItem creatureHarvBrowser(SWGCreatureResources cr) {
        JMenuItem mi = new JMenuItem("Browse SWGPets.com");
        try {
            final URL url = SWGPets.urlFor(cr);
            String tt = String.format(
                    "Open SWGPets.com in web browser for %s", cr.rcName());
            mi.setToolTipText(tt);
            if (url != null)
                mi.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ZHtml.browser(url.toExternalForm());
                    }
                });
            else
                mi.setEnabled(false);
        } catch (Exception e) {
            SWGAide.printError("SWGResController:creatureHarv", e);
            mi.setEnabled(false);
        }
        return mi;
    }

    /**
     * Displays a dialog which displays from which creatures the specified
     * resource class may be harvested.
     * 
     * @param cr a creature resource class
     * @param c the component to display the dialog over
     * @throws NullPointerException if an argument is {@code null}
     */
    private static void creatureHarvDialog(SWGCreatureResources cr, Component c) {
        String rn = cr.rcName();
        String t = "Credits to SWGPets.com";
        int m = JOptionPane.PLAIN_MESSAGE;
        String s;
        if (cr.isSpawnable()) {
            s = SWGPets.creatures(cr);
            if (s.equals(SWGPets.NONE)) {
                m = JOptionPane.INFORMATION_MESSAGE;
                s = String.format("No creature drops %s %n%n%s", rn, t);
            } else if (cr.isSub(SWGEgg.class) || cr.isSub(SWGSeafood.class))
                s = String.format("%s is harvested from %s", rn, s);
            else
                s = String.format("%s is harvested from:%n%s%n%n%s", rn, s, t);

        } else {
            m = JOptionPane.ERROR_MESSAGE;
            s = String.format("%s cannot spawn, it is a generic class", rn);
        }
        JOptionPane.showMessageDialog(c, s, "Creature harvesting", m);
    }

    /**
     * Creates and returns a menu for information on which creatures that drops
     * the specified resource class. This menu contains two menu items, one that
     * displays a dialog with those creatures or an informative text, and one
     * that opens a web browser at SWGPets.com for the specified class.
     * 
     * @param cr a creature resource class
     * @param c a component to show the dialog over
     * @return a menu item
     */
    public static JMenu creatureHarvMenu(
            final SWGCreatureResources cr, final Component c) {

        String tt = String.format(
                "Display which creatures to harvest %s from", cr.rcName());
        JMenuItem mi = new JMenuItem("Harvested from...");
        mi.setToolTipText(tt);
        mi.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                creatureHarvDialog(cr, c);
            }
        });

        JMenu menu = new JMenu("Creature harvesting");
        menu.setToolTipText(tt);
        menu.add(mi);
        menu.add(creatureHarvBrowser(cr));
        return menu;
    }

    /**
     * Returns a menu item with an action listener which filters for the
     * specified resource class at the table of current resources. If the
     * argument is {@code null} no listener is attached and the menu item is
     * disabled. If experimental weights are specified the result is sorted by
     * the computed weights. If a known resource is specified and is part of the
     * filtered set it is selected.
     * 
     * @param rc a resource class constant
     * @param wg experimental weights, or {@code null}
     * @param kr a resource to selected, or {@code null}
     * @return a menu item
     */
    public static JMenuItem currentFilterMenu(
            SWGResourceClass rc, SWGWeights wg, SWGKnownResource kr) {
        return THIS.resourceTab.currentFilterMenu(rc, wg, kr);
    }

    /**
     * Returns a menu item with an action for selecting an element at the
     * current resources panel. The listener selects the argument but if the
     * argument is {@code null} or a depleted resource no listener is attached
     * and the menu item is disabled. If necessary, as a side effect this call
     * creates the panel.
     * 
     * @param kr a resource
     * @return a menu item
     */
    public static JMenuItem currentSelectMenu(SWGKnownResource kr) {
        return THIS.resourceTab.currentSelectMenu(kr);
    }

    /**
     * Returns a string representation of {@code seconds} on the form<br/>
     * <i>n&nbsp;</i> m<br/>
     * <i>n&nbsp;</i> h<br/>
     * <i>n&nbsp;</i> d<br/>
     * <i>n&nbsp;</i> w<br/>
     * <i>n&nbsp;</i> y<br/>
     * for <i>n&nbsp;</i> number of <b>m</b>inutes, <b>h</b>ours, <b>d</b>ays,
     * <b>w</b>eeks, or <b>y</b>ears respectively. If the value indicates more
     * than 7 years this method returns "n/a" and if the argument is negative
     * "?" is returned.
     * 
     * @param seconds the number of seconds to represent as a string
     * @return the string representation of {@code seconds}
     */
    public static String dateString(long seconds) {
        if (seconds < 0)
            return "?";
        if (seconds < 60)
            return seconds + " s";
        if (seconds < 3600)
            return (seconds / 60) + " m";
        if (seconds < 86400)
            return (seconds / 3600) + " h";
        if (seconds < 86400 * 28)
            return (seconds / 86400) + " d";
        if (seconds < 86400 * 365)
            return (seconds / 86400 / 7) + " w";
        if (seconds < 86400 * 365 * 7)
            return (seconds / 86400 / 365.25) + " y"; // max 6 years
        return "n/a";
    }

    /**
     * Returns a guard for the identified galaxy with the specified resource
     * class and values, or {@code null}. This method iterates over all guards
     * for the galaxy and returns the first <i>exact match </i> it finds, or
     * {@code null}. The {@code grd} argument is either {@link SWGWeights} or
     * {@link SWGResourceFilter} and for a guard to match it must use the same
     * type. If the galaxy constant is {@code null} this method use the current
     * galaxy.
     * 
     * @param rc a resource class constant
     * @param grd a filter or weights
     * @param gxy a galaxy constant, or {@code null} for current galaxy
     * @return a guard or {@link NullPointerException}
     * @throws NullPointerException if an argument is null
     */
    static SWGGuard guard(SWGResourceClass rc, SWGValues grd, SWGCGalaxy gxy) {
        if (rc == null) throw new NullPointerException("RC is null");

        SWGCGalaxy gx = gxy == null
                ? SWGFrame.getSelectedGalaxy()
                : gxy;
        synchronized (guards) {
            for (SWGGuard g : guards(gx))
                if (rc == g.rc()
                        && grd.equals(g.logic()))
                    return g;

            return null;
        }
    }

    /**
     * Returns {@code true} if a guard exists at the identified galaxy with the
     * specified resource class and values. This method determines that the
     * return value from {@link #guard(SWGResourceClass, SWGValues, SWGCGalaxy)}
     * is not {@code null}. If the galaxy constant is {@code null} this method
     * use the current galaxy.
     * 
     * @param rc a resource class constant
     * @param grd a filter or weights
     * @param gxy a galaxy constant, or {@code null} for current galaxy
     * @return {@code true} if an exact match is found
     * @throws NullPointerException if an argument is null
     */
    public static boolean guardExist(
            SWGResourceClass rc, SWGValues grd, SWGCGalaxy gxy) {
        return guard(rc, grd, gxy) != null;
    }

    /**
     * Helper method which is called when the user selects to create a resource
     * guard for the specified argument. This method is used for both LQ and HQ
     * guards and creates a plain filter or displays the standard edit/create
     * dialog.
     * 
     * @param rc a resource class
     * @param stats the filter or weights
     * @param gxy a galaxy constant
     * @param name a name for the guard
     * @param notes optional notes for the guard, or {@code null}
     * @param comp a component for a dialog
     * @throws NullPointerException if an argument is {@code null}
     */
    private static void guardMenuAction(SWGResourceClass rc, SWGValues stats,
            SWGCGalaxy gxy, String name, String notes, Component comp) {

        SWGGuard g = guard(rc, stats, gxy);
        if (g != null) {
            ZString z = new ZString("One or several similar guards exist:");
            z.nl().appnl(g.getName()).app(g.rc()).nl();
            g.logic().toString(z, false);
            z.nl().appnl(g.notes()).nl().app("Create anyway?");

            if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(comp,
                    z.toString(), "Confirm", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE))
                return;
        }

        int t = stats instanceof SWGWeights
                ? (int) (SWGGuiUtils.statLimits[0] * 1000)
                : 0;
        g = new SWGGuard(name, rc, stats, t, true, true);
        g.notes(notes);

        if (stats instanceof SWGResourceFilter)
            SWGResController.guardsAdd(g, gxy, true);
        else
            SWGResController.guardsDialog().showAndBegin(rc, g, gxy);
    }

    /**
     * Returns a menu item for creating a plain resource guard for the specified
     * class at the current galaxy. The guard alerts for OQ &ge; 1, which means
     * any new spawn of the class. If an argument is {@code null} the menu item
     * is disabled.
     * <p>
     * When the user selects this menu option the action displays a warning
     * dialog if a similar guard exists, otherwise a confirmation dialog is
     * displayed over the specified component.
     * 
     * @param rc a resource class constant
     * @param comp the component for a subsequent dialog
     * @param notes optional notes for the guard, or {@code null}
     * @return a menu item
     */
    public static JMenuItem guardPlainMenu(
            final SWGResourceClass rc, final Component comp, final String notes) {

        final SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        final String rcn = rc == null
                ? "none"
                : rc.rcName();
        JMenuItem grd = new JMenuItem("Plain guard...");
        grd.setToolTipText(String.format(
                "Create a plain LQ guard for %s @ %s", rcn, gxy));

        boolean b = rc != null && comp != null;
        if (b) grd.addActionListener(new ActionListener() {
            @SuppressWarnings( { "synthetic-access" })
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = String.format(
                        "Create guard \"Any spawn\" for \"%s\"%n"
                                + "with OQ=1 at %s%n%n"
                                + "It will alert for any new spawn",
                                rcn, gxy.getName());
                if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                        comp, msg, "Confirm", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE)) return;

                guardMenuAction(rc, new SWGResourceFilter(Stat.OQ, 1),
                        gxy, "Any spawn", notes, comp);
            }
        });
        grd.setEnabled(b);

        return grd;
    }

    /**
     * Returns a menu item for creating a quality resource guard for the
     * specified class at the current galaxy. If an argument is {@code null} the
     * menu item is disabled.
     * <p>
     * When the user selects this menu option the action displays a warning
     * dialog if a similar guard exists. Otherwise the standard edit/create
     * dialog is initiated with the suggested arguments and the "fair" value as
     * a suggested threshold, compare {@link SWGGuiUtils#statColors}.
     * 
     * @param rc a resource class constant
     * @param wgs weights for the guard
     * @param comp the component for a subsequent dialog
     * @param notes optional notes for the guard, or {@code null}
     * @return a menu item
     */
    public static JMenuItem guardQualityMenu(final SWGResourceClass rc,
            final SWGWeights wgs, final Component comp, final String notes) {

        final SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        JMenuItem grd = new JMenuItem("Quality guard...");
        grd.setToolTipText(String.format("Create HQ guard for %s @ %s",
                rc == null
                        ? "none"
                        : rc.rcName(), gxy));

        boolean b = rc != null && wgs != null && comp != null;
        if (b) grd.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void actionPerformed(ActionEvent e) {
                guardMenuAction(rc, wgs, gxy, "HQ missing", notes, comp);
            }
        });
        grd.setEnabled(b);

        return grd;
    }

    /**
     * Returns a list of all resources guards for the specified galaxy, or an
     * empty list. This is the list which is stored by SWGAide and it should not
     * be mutated rashly.
     * 
     * @param gxy a galaxy constant
     * @return a list of resource guards
     * @throws NullPointerException if the argument is {@code null}
     */
    public static List<SWGGuard> guards(SWGCGalaxy gxy) {
        if (gxy == null)
            throw new NullPointerException("Galaxy is null");

        synchronized (guards) {
            List<SWGGuard> grds = guards.get(gxy);
            if (grds == null) {
                grds = new ArrayList<SWGGuard>();
                guards.put(gxy, grds);
            }
            return grds;
        }
    }

    /**
     * Adds {@code guard} to the list of guards for the specified galaxy. If the
     * guard already exists this method does nothing. Optionally this method
     * scans all current resources for the added guard.
     * 
     * @param guard the guard to add
     * @param gxy a galaxy constant
     * @param scan {@code true} if this method should scan the resources,
     *        {@code false} otherwise
     * @throws NullPointerException if an argument is {@code null}
     */
    public static void guardsAdd(SWGGuard guard, SWGCGalaxy gxy, boolean scan) {
        if (guard == null)
            throw new NullPointerException("Guard is null");

        synchronized (guards) {
            List<SWGGuard> grds = guards(gxy);
            if (!SWGResController.contains(grds, guard)) {
                grds.add(guard);
                if (scan)
                    synchronized (LOCK) {
                        check();
                    }
                SWGResourceTab.currentUpdateGUI();
            }
        }
    }

    /**
     * Helper method which iterates over all resource guards for the specified
     * galaxy and returns a non-zero value if at least one guard is triggered,
     * or zero if no guard triggered. A negative return value denotes that at
     * least one guard triggered and a sound is allowed to alert the user, but a
     * positive value denotes at least one triggered guard but no sound. This is
     * to support the option "alert once" which is per guard, but we do need to
     * know if anything is triggered, even if no sound alert should be emitted.
     * Sound has precedence over muted because it indicates a newer resource is
     * found.
     * <p>
     * As a side effect of the scan each individual guard will have its date
     * {@code resourceRecentSpawnDate} set to an appropriate value.
     * <p>
     * Any call to this method is assumed to already be synchronized.
     * 
     * @param gxy the galaxy to check guards for
     * @return zero if no guard triggered, see details above
     * @throws NullPointerException if the argument is {@code null}
     */
    private static int guardsCheck(SWGCGalaxy gxy) {
        if (gxy != SWGResourceTab.galaxy()
                && gxy != SWGCGalaxy.fromName((String)
                        SWGFrame.getPrefsKeeper().get("optionMainGalaxy",
                                SWGCGalaxy.SWGCRAFT_CO_UK.getName())))
            return 0; // only continue if current gxy or if default gxy

        SWGResourceSet resources = SWGResourceManager.getSpawning(gxy);

        // if disregard Kashyyyk and Mustafar is chosen...
        if (!resources.isEmpty()
                && ((Boolean) SWGFrame.getPrefsKeeper().get(
                        "resourceGuardsDisregardMK",
                        Boolean.FALSE)).booleanValue()) {

            // no resources from K & M, creature resources are always retained
            resources = resources.subsetBy(kmComparator);
        }

        if (resources.isEmpty())
            return 0;

        long now = System.currentTimeMillis() / 1000;
        long ageLimit = now;
        Integer maxHours = (Integer) SWGFrame.getPrefsKeeper().get(
                        "resourceGuardsMaxHours");
        // subtract allowed time span, or 36 h default
        if (maxHours != null)
            ageLimit -= maxHours.longValue() * 3600;
        else
            ageLimit -= 36 * 3600;

        // Resources are not aware of guards and guards do not keep track of
        // which resources have triggered or not; always consider resources
        // younger than 2400 seconds (40 minutes) as a new spawn
        long newSpawnLimit = now - 2400;

        // if alertOnce is true AND a guard has triggered once, only a new spawn
        // can make it trigger once again during the session
        boolean alertOnce = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceGuardAlertOnce", Boolean.FALSE)).booleanValue();

        int ret = 0;
        for (SWGGuard g : guards(gxy)) {
            boolean wasAlarmed = g.resourceRecentSpawnDate > 0;
            boolean allowSound = wasAlarmed && !alertOnce;

            // reset it, it might be aged, otherwise it is soon set again
            g.resourceRecentSpawnDate = 0;
            if (!g.useAlarm) continue; // reset but no reason to continue

            for (SWGKnownResource r : resources) {
                if (harvesterActive(r, gxy)) continue;
                if (g.accept(r)) {
                    SWGPlanetAvailabilityInfo pai = r.availableFirst();
                    long rt = pai.available();
                    if (rt > ageLimit) {
                        // for each triggered guard, find the most recent
                        // resource and use its first-available date
                        if (rt > g.resourceRecentSpawnDate)
                            g.resourceRecentSpawnDate = rt;

                        ret |= (rt > newSpawnLimit || allowSound)
                                ? -1 // allow sound
                                : 1; // alert but no sound
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Returns a dialog for creating/editing resource guards.
     * 
     * @return the resource guards dialog
     */
    public static SWGGuardDialog guardsDialog() {
        if (guardDialog == null)
            guardDialog = new SWGGuardDialog(SWGAide.frame());

        return guardDialog;
    }

    /**
     * Removes the specified resource from the list of guards for the specified
     * galaxy.
     * 
     * @param guard the guard to remove
     * @param gxy a galaxy constant
     * @throws NullPointerException if an argument is {@code null}
     */
    static void guardsRemove(SWGGuard guard, SWGCGalaxy gxy) {
        synchronized (guards) {
            List<SWGGuard> grds = guards(gxy);
            grds.remove(guard);
            check();
            SWGResourceTab.currentUpdateGUI();
        }
    }

    /**
     * Updates {@link #statusColor} based on the argument. A negative value
     * means alarm and has higher precedence than an alert or warning. A
     * positive value is an alert or warning. A zero is ignored as it has no
     * impact.
     * <p>
     * If the boolean argument is true this method first inspects the status
     * value and then tints the Resources tab with {code statusColor} and resets
     * the status color to {@code null}.
     * <p>
     * Hence, a checking method needs to update only this method. It is safe to
     * invoke this method with {@code guiStatus(0, true)} to just update the GUI
     * in a final call.
     * 
     * @param status a status value, 0 is ignored
     * @param updateGUI {@code true} to update the Resource tab
     */
    private static void guiStatus(int status, boolean updateGUI) {
        if (status < 0)
            statusColor = Color.PINK;
        else if (status > 0 && statusColor != Color.PINK)
            statusColor = SWGGuiUtils.colorAlert;

        if (updateGUI) {
            SWGAide.frame().getTabPane().setBackgroundAt(2, statusColor);
            SWGResourceTab.currentUpdateGUI();
            statusColor = null;
        }
    }

    /**
     * Determines if the specified resource is harvested at the specified
     * galaxy. This method returns {@code true} if there is an active harvester
     * for the resource.
     * 
     * @param kr a resource
     * @param gxy a galaxy constant
     * @return {@code true} if the resource is being harvested
     * @throws NullPointerException if an argument is {@code null}
     */
    public static boolean harvesterActive(SWGKnownResource kr, SWGCGalaxy gxy) {
        synchronized (harvesters) {
            List<SWGHarvester> hl = harvesters(gxy);
            for (SWGHarvester h : hl)
                if (h.isActive() && h.getResource() == kr) return true;

            return false;
        }
    }

    /**
     * Adds the specified harvester owner to the list of owners for the
     * specified galaxy. If the owner already exists this method dows nothing.
     * 
     * @param owner a harvester owner
     * @param gxy a galaxy constant
     * @throws NullPointerException if an argument is {@code null}
     */
    static void harvesterOwnerAdd(SWGHarvesterOwner owner, SWGCGalaxy gxy) {
        List<SWGHarvesterOwner> ol = harvesterOwners(gxy);
        if (!harvesterOwnerExists(owner.getName(), gxy))
            ol.add(owner);
    }

    /**
     * Determines if a harvester owner exists the specified name. This method
     * ignores upper and lower cases while it compares names in the list of
     * owners for the current galaxy.
     * 
     * @param name a name to check for
     * @param gxy a galaxy constant
     * @return {@code true} if the name equals a name for an owner name
     * @throws NullPointerException if an argument is {@code null}
     */
    static boolean harvesterOwnerExists(String name, SWGCGalaxy gxy) {
        List<SWGHarvesterOwner> ol = harvesterOwners(gxy);
        for (SWGHarvesterOwner e : ol)
            if (name.equalsIgnoreCase(e.getName())) return true;
        return false;
    }

    /**
     * Removes the specified harvester owner from the specified galaxy. If the
     * owner does not exist this method does nothing.
     * 
     * @param owner a harvester owner
     * @param gxy a galaxy constant
     * @throws NullPointerException if an argument is {@code null}
     */
    static void harvesterOwnerRemove(SWGHarvesterOwner owner, SWGCGalaxy gxy) {
        harvesterOwners(gxy).remove(owner);
    }

    /**
     * Returns a set of harvester owners for the specified galaxy, or an empty
     * list. These are the harvester owners the user has specified at the
     * harvesting panel. This collection is the list which is stored by SWGAide,
     * thus it must not be modified rashly.
     * 
     * @param gxy a galaxy constant
     * @return a list of harvester owners, or an empty list
     * @throws NullPointerException if the argument is {@code null}
     */
    static List<SWGHarvesterOwner> harvesterOwners(SWGCGalaxy gxy) {
        if (gxy == null) throw new NullPointerException("Galaxy is null");

        List<SWGHarvesterOwner> ol = harvOwners.get(gxy);
        if (ol == null) {
            ol = new ArrayList<SWGHarvesterOwner>();
            harvOwners.put(gxy, ol);
        }

        return ol;
    }

    /**
     * Returns a list of all harvesters for the specified galaxy, or an empty
     * list. The returned list also contains inactive harvesters and this
     * collection is the list which is stored by SWGAide, thus it must not be
     * modified rashly.
     * 
     * @param gxy a galaxy constant
     * @return a list of harvesters
     * @throws NullPointerException if the argument is {@code null}
     */
    static List<SWGHarvester> harvesters(SWGCGalaxy gxy) {
        if (gxy == null) throw new NullPointerException("Galaxy is null");

        synchronized (harvesters) {
            List<SWGHarvester> harvies = harvesters.get(gxy);
            if (harvies == null) {
                harvies = new ArrayList<SWGHarvester>();
                harvesters.put(gxy, harvies);
            }
            return harvies;
        }
    }

    /**
     * Returns a list of <i>active&nbsp;</i> harvesters for the specified
     * galaxy, or an empty list.
     * 
     * @param galaxy a galaxy constant
     * @return a list of active harvesters
     * @throws NullPointerException if the argument is {@code null}
     */
    static List<SWGHarvester> harvestersActive(SWGCGalaxy galaxy) {
        synchronized (harvesters) {
            List<SWGHarvester> hl = harvesters(galaxy);
            List<SWGHarvester> active = new ArrayList<SWGHarvester>(hl.size());
            for (SWGHarvester h : hl)
                if (h.isActive())
                    active.add(h);

            return active;
        }
    }

    /**
     * Adds the specified harvester to the list of harvesters for the specified
     * galaxy. If a harvester exists with an equal name, case ignored, this
     * method throws an exception.
     * 
     * @param h a harvester to add
     * @param gxy a galaxy constant
     * @throws IllegalStateException if the harvester is active
     * @throws NullPointerException if an argument is {@code null}
     */
    static void harvestersAdd(SWGHarvester h, SWGCGalaxy gxy) {
        synchronized (harvesters) {
            if (harvestersExists(h.getName(), gxy))
                throw new IllegalStateException("Duplicate harvester: " + h);

            harvesters(gxy).add(h);
        }
    }

    /**
     * Helper method which iterates over all active harvesters for the specified
     * galaxy and returns a non-zero integer if at least one harvester is
     * triggered. The return value is negative if a harvester is idling and
     * positive if a harvester is about to idle within the time the user has
     * specified for alerts. A harvester begins to idle if its resource is
     * depleted, if the hopper is full, or if maintenance or power is drained.
     * <p>
     * Any call to this method is assumed to already be synchronized.
     * 
     * @param galaxy the galaxy to check
     * @return zero if no harvester is triggered, {@literal < 0} if a harvester
     *         is idling, {@literal > 0} if a harvester soon begin to idle
     * @throws NullPointerException if the argument is {@code null}
     */
    private static int harvestersCheck(SWGCGalaxy galaxy) {
        long now = System.currentTimeMillis();
        List<SWGHarvester> active = harvestersActive(galaxy);

        int l = ((Integer) SWGFrame.getPrefsKeeper().get(
                "resourceHarvesterReminder", Integer.valueOf(24))).intValue();
        long warn = now + (l * 3600 * 1000);

        int ret = 0;
        for (SWGHarvester h : active) {
            // one harvester is enough to return true

            // handling each case separately in case we want to do something
            // different for each later

            // an idling harvester have higher priority, do these first
            if (h.getResource().isDepleted())
                return -1;
            if (h.getHopperFull() < now)
                return -1;
            if (h.getPowerEnds() < now)
                return -1;
            if (h.getMaintEnds() < now)
                return -1;

            // now warnings
            if (h.getHopperFull() < warn)
                ret = 1;
            if (h.getPowerEnds() < warn)
                ret = 1;
            if (h.getMaintEnds() < warn)
                ret = 1;
        }
        return ret;
    }

    /**
     * Determines if a harvester exists with the specified name at the specified
     * galaxy. This method ignores case for equality.
     * 
     * @param name the name to check
     * @param gxy a galaxy constant
     * @return {@code true} if a harvester with the specified name exists
     * @throws NullPointerException if an argument is {@code null}
     */
    static boolean harvestersExists(String name, SWGCGalaxy gxy) {
        synchronized (harvesters) {
            for (SWGHarvester h : harvesters(gxy))
                if (h.getName().equalsIgnoreCase(name))
                    return true;
            return false;
        }
    }

    /**
     * Removes the specified harvester from the list of harvesters for the
     * specified galaxy. If the harvester is active this method throws an
     * exception. If the specified harvester does not exist at the galaxy this
     * method does nothing.
     * 
     * @param h a harvester to remove
     * @param gxy a galaxy constant
     * @throws IllegalStateException if the harvester is active
     * @throws NullPointerException if an argument is {@code null}
     */
    static void harvestersRemove(SWGHarvester h, SWGCGalaxy gxy) {
        if (h.isActive())
            throw new IllegalStateException("Harvester is active: " + h);

        synchronized (harvesters) {
            harvesters(gxy).remove(h);
        }
    }

    /**
     * Returns the inventory for the specified assignee at the specified galaxy,
     * or an empty list, or {@code null}. If the assignee does not exist or if a
     * list does not exist for the specified assignee and if {@code create} is
     * {@code true} this method creates and adds a list for the assignee to the
     * map of resource inventories before it returns the new list; otherwise
     * this method returns {@code null}.
     * <p>
     * <b>Notice:</b> the returned list is the collection which is stored by
     * SWGAide and it must not be modified rashly.
     * 
     * @param ass an assignee
     * @param gxy a galaxy constant
     * @param create {@code true} to create and add a new list if none exists
     * @return a list of inventory elements, or an empty list, or {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    static List<SWGInventoryWrapper> inventory(
            String ass, SWGCGalaxy gxy, boolean create) {
        if (ass == null) throw new NullPointerException("Assignee is null");

        synchronized (inventoryMap) {
            Map<String, List<SWGInventoryWrapper>> ags =
                    inventoryAssignees(gxy, create);

            List<SWGInventoryWrapper> wl = null;
            if (ags != null) {
                wl = ags.get(ass);
                if (wl == null && create) {
                    wl = new ArrayList<SWGInventoryWrapper>();
                    ags.put(ass, wl);
                }
            }
            return wl;
        }
    }

    /**
     * Returns a list of all inventory wrappers for the specified galaxy, or an
     * empty list. If a resource is owned by several assignees the returned list
     * contains one element per resource-assignee wrapper. The returned list is
     * free to modify, unless it is the {@link Collections#EMPTY_LIST}.
     * 
     * @param gxy a galaxy constant
     * @return a list of inventory wrappers, or an empty list
     * @throws NullPointerException if the argument is {@code null}
     */
    public static List<SWGInventoryWrapper> inventory(SWGCGalaxy gxy) {
        synchronized (inventoryMap) {
            Map<String, List<SWGInventoryWrapper>> ags =
                    inventoryAssignees(gxy, false);

            if (ags == null) return Collections.emptyList();

            ArrayList<SWGInventoryWrapper> ret =
                    new ArrayList<SWGInventoryWrapper>(512);

            for (String assignee : ags.keySet()) {
                List<SWGInventoryWrapper> ws = ags.get(assignee);
                if (ws != null)
                    ret.addAll(ws);
            }
            return ret;
        }
    }

    /**
     * Adds the list of wrappers to the specified galaxy. Per element this
     * method invokes {@link #inventoryAdd(SWGInventoryWrapper, SWGCGalaxy)}.
     * 
     * @param wl a list of inventory wrappers
     * @param gxy a galaxy constant
     * @throws NullPointerException if an argument or element is {@code null}
     */
    static void inventoryAdd(List<SWGInventoryWrapper> wl, SWGCGalaxy gxy) {
        synchronized (inventoryMap) {
            for (SWGInventoryWrapper w : wl)
                inventoryAdd(w, gxy);
        }
    }

    /**
     * Adds the specified inventory wrapper to the specified galaxy. The
     * assignee is obtained from the wrapper and if the assignee does not exist
     * at the galaxy one is created and added.
     * <p>
     * If a previous wrapper exists the {@link SWGInventoryWrapper#equalAddSub}
     * determines if this method should update the amount of that wrapper or
     * not. If a previous wrapper exists and is updated, its notes is <i>not
     * </i> updated (old notes may be multi-lines which is not handled by
     * export/import of notes).
     * <p>
     * <b>Notice:</b> the rules for this implementation are:
     * <ul>
     * <li>if the update tag mentioned above is {@code null} the assignee of the
     * specified wrapper is used, and if a previous wrapper exists for the
     * assignee this method does nothing</li>
     * <li>else if no previous wrapper exist the argument is added for the
     * assignee</li>
     * <li>else ... one or several previous wrappers exist
     * <ul>
     * <li>if a previous wrapper exists for the assignee it is updated</li>
     * <li>else if only one previous wrapper exist it is updated</li>
     * <li>else if a previous wrapper exists for "All" it is updated</li>
     * <li>else an undefined wrapper is updated<br/>
     * <br/>
     * The two latter items imply a limitation to the update-from-notes-file
     * feature; if a previous wrappers exist the wrong wrapper may be updated if
     * "the wrong toon" is selected during the import.</li>
     * </ul>
     * </li>
     * </ul>
     * 
     * @param iw the wrapper to add or update with
     * @param gxy a galaxy constant
     * @throws NullPointerException if an argument is {@code null}
     */
    static void inventoryAdd(SWGInventoryWrapper iw, SWGCGalaxy gxy) {
        if (iw == null || iw.getAssignee() == null || gxy == null)
            throw new NullPointerException(
                    "Something is null: " + iw + ' ' + ' ' + gxy);

        synchronized (inventoryMap) {
            List<SWGInventoryWrapper> asl = inventory(
                    iw.getAssignee(), gxy, true);

            if (iw.equalAddSub == null)
                inventoryAddHelper(iw, asl);
            else
                inventoryAddUpdate(iw, gxy, asl);
        }
    }

    /**
     * Adds {@code amount} to an inventory wrapper for {@code resource} owned by
     * {@code assignee} at the specified galaxy. If a wrapper is found for the
     * specified resource and assignee that wrapper is updated. Otherwise, if no
     * inventory wrapper is found one is created.
     * <p>
     * This method is a convenience for clients that do not know about inventory
     * wrappers, such as the harvesting panel.
     * 
     * @param amount the amount to add
     * @param kr the resource constant
     * @param ass the assignee
     * @param gxy a galaxy constant
     * @throws IllegalArgumentException if the amount is negative
     * @throws NullPointerException if an argument is {@code null}
     */
    static void inventoryAddAmount(
            long amount, SWGKnownResource kr, String ass, SWGCGalaxy gxy) {

        if (amount < 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        if (kr == null || ass == null)
            throw new NullPointerException("An argument is null");

        synchronized (inventoryMap) {
            List<SWGInventoryWrapper> wl = inventory(ass, gxy, false);
            if (wl != null)
                for (SWGInventoryWrapper w : wl)
                if (w.getResource() == kr) {
                    w.setAmount(w.getAmount() + amount);
                    return;
                }

            // else, either no list of wrappers or no wrapper was found
            SWGInventoryWrapper w = new SWGInventoryWrapper(kr, ass);
            w.setAmount(amount);
            inventoryAdd(w, gxy);
        }
    }

    /**
     * Helper method which adds to the assignee of the specified wrapper. If a
     * previous wrapper exists this method does nothing. For further details see
     * {@link #inventoryAdd(SWGInventoryWrapper, SWGCGalaxy)}. The caller must
     * have synchronized on {@link #inventoryMap}.
     * 
     * @param iw an inventory wrapper
     * @param asl a list of wrappers for the assignee
     */
    private static void inventoryAddHelper(
            SWGInventoryWrapper iw, List<SWGInventoryWrapper> asl) {

        for (SWGInventoryWrapper w : asl)
            if (w.getResource() == iw.getResource())
                return; // and no duplicates allowed

        asl.add(iw);
    }

    /**
     * Helper method which updates or adds a wrapper, for further details see
     * {@link #inventoryAdd(SWGInventoryWrapper, SWGCGalaxy)}. The caller must
     * have synchronized on {@link #inventoryMap}.
     * 
     * @param iw an inventory wrapper
     * @param gxy a galaxy constant
     * @param asl a list of wrappers for an assignee
     */
    private static void inventoryAddUpdate(SWGInventoryWrapper iw,
            SWGCGalaxy gxy, List<SWGInventoryWrapper> asl) {

        String ass = iw.getAssignee();
        SWGKnownResource kr = iw.getResource();
        List<SWGInventoryWrapper> all = inventory(gxy); // safe if asl is safe

        SWGInventoryWrapper aw = null;
        List<SWGInventoryWrapper> found = new ArrayList<SWGInventoryWrapper>();
        for (SWGInventoryWrapper wr : all)
            if (wr.getResource() == kr) {
                if (wr.getAssignee().equals(ass)) {
                    aw = wr;
                    break;
                }
                found.add(wr);
            }

        if (aw == null && found.size() > 1) {
            for (SWGInventoryWrapper wr : found)
                if (wr.getAssignee().equals("All")) {
                    aw = wr;
                    break;
                }
        }
        // first or undefined (first in this list IS undefined imho)
        if (aw == null && !found.isEmpty())
            aw = found.get(0);

        if (aw != null) {
            long old = aw.getAmount();
            long eas = iw.getAmount();
            if (iw.equalAddSub.equals("="))
                aw.setAmount(eas); // replace
            else if (iw.equalAddSub.equals("+"))
                aw.setAmount(old + eas); // add
            else if (iw.equalAddSub.equals("-"))
                aw.setAmount(Math.max(old - eas, 0L));

            // do not update notes, the notes file may have just one line while
            // the old notes may have several
        } else
            asl.add(iw);
    }

    /**
     * Returns the total amount in inventory of the specified resource, 0 if it
     * exists but no amount is specified, or -1 if the resource is not in stock.
     * If zero or greater the amount is for <i>all&nbsp;</i> assignees at the
     * specified galaxy who owns stacks of the specified resource.
     * 
     * @param res the resource
     * @param gxy a galaxy constant
     * @return the total amount owned of the resource at the specified galaxy
     * @throws NullPointerException if an argument is {@code null}
     */
    public static long inventoryAmount(SWGKnownResource res, SWGCGalaxy gxy) {
        List<SWGInventoryWrapper> ws = inventoryBy(res, gxy);

        if (ws.size() <= 0)
            return -1;

        long amt = 0;
        for (SWGInventoryWrapper w : ws)
            amt += w.getAmount();
        return amt;
    }

    /**
     * Removes the specified assignee from the specified galaxy. If the assignee
     * owns a list of inventory wrappers it is discarded, thus the client that
     * invokes this method must guard against data loss.
     * 
     * @param ass an assignee
     * @param gxy a galaxy constant
     * @throws NullPointerException if an argument is {@code null}
     */
    static void inventoryAssigneeRemove(String ass, SWGCGalaxy gxy) {
        synchronized (inventoryMap) {
            Map<String, List<SWGInventoryWrapper>> ags =
                    inventoryAssignees(gxy, false);

            if (ags != null)
                ags.remove(ass);
        }
    }

    /**
     * Returns a map of assignees and their resource-inventories for the
     * specified galaxy. If no map exists for the specified galaxy and if
     * {@code create} is {@code true} this method creates a map, adds it to the
     * storage, and returns the map; otherwise this method returns {@code null}.
     * <p>
     * <b>Notice:</b> The returned map is a collection which is stored by
     * SWGAide and it must not be modified rashly.
     * 
     * @param gxy a galaxy constant
     * @param create {@code true} to create a new entry in the case none exists
     * @return a map of assignees-and-resource-inventories, or {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    static Map<String, List<SWGInventoryWrapper>> inventoryAssignees(
            SWGCGalaxy gxy, boolean create) {
        if (gxy == null) throw new NullPointerException("Galaxy is null");

        synchronized (inventoryMap) {
            Map<String, List<SWGInventoryWrapper>> ags = inventoryMap.get(gxy);
            if (ags == null && create) {
                ags = new HashMap<String, List<SWGInventoryWrapper>>();
                inventoryMap.put(gxy, ags);
            }
            return ags;
        }
    }

    /**
     * Returns a list of inventory wrappers for the specified resource and which
     * are owned by assignees at the specified galaxy, or an empty list.
     * 
     * @param kr the resource to scan for
     * @param gxy a galaxy constant
     * @return a list of inventory elements, or an empty list
     * @throws NullPointerException if an argument is null
     */
    private static List<SWGInventoryWrapper> inventoryBy(
            SWGKnownResource kr, SWGCGalaxy gxy) {
        if (kr == null) throw new NullPointerException("Resource is null");

        List<SWGInventoryWrapper> all = inventory(gxy);
        List<SWGInventoryWrapper> ret = new ArrayList<SWGInventoryWrapper>();

        for (SWGInventoryWrapper w : all)
            if (w.getResource() == kr)
                ret.add(w);

        return ret;
    }

    /**
     * Determines if a resource exist in inventory for the specified resource
     * class and galaxy. This method scans all assignees at the specified galaxy
     * for the specified resource class and return {@code true} if at least one
     * assignee owns <i>one unit </i> or more. The boolean argument determines
     * if the found resource class must be an exact match, or if sub-classes of
     * the argument are accepted.
     * 
     * @param rc a resource class
     * @param gxy a galaxy constant
     * @param exact {@code false} to accept also sub-classes of {@code resCls}
     * @return {@code true} if something exists in inventory
     * @throws NullPointerException if an argument is {@code null}
     */
    public static boolean inventoryExists(
            SWGResourceClass rc, SWGCGalaxy gxy, boolean exact) {

        synchronized (inventoryMap) {
            List<SWGInventoryWrapper> all = inventory(gxy);

            if (all.size() <= 0)
                return false;

            if (exact) {
                for (SWGInventoryWrapper w : all)
                    if (w.getResource().rc() == rc && w.getAmount() > 0)
                        return true;
            } else {
                Class<? extends SWGResourceClass> crc = rc.getClass();
                for (SWGInventoryWrapper w : all)
                    if (w.getResource().rc().isSub(crc)
                            && w.getAmount() > 0)
                        return true;
            }
            return false;
        }
    }

    /**
     * Returns a menu item with an action listener for the resource inventory.
     * If the specified resource class is {@code null} the current selection is
     * cleared and the method returns, otherwise, if the argument is specified
     * the inventory is filtered. Then, if the resource argument is specified
     * and if it exists in the filtered inventory it is selected, otherwise, if
     * the argument is {@code null} and the boolean argument is {@code true} the
     * menu item is disabled.
     * 
     * @param rc a resource class, or {@code null}
     * @param wg experimental weights, or {@code null}
     * @param kr a resource to selected, or {@code null}
     * @param disable disable the menu item if resource is {@code null}
     * @return a menu item, or {@code null}
     */
    public static JMenuItem inventoryFilterMenu(SWGResourceClass rc,
            SWGWeights wg, SWGKnownResource kr, boolean disable) {

        return SWGResourceTab.inventoryFilterMenu(rc, wg, kr, true, disable);
    }

    /**
     * Moves all inventory wrappers from the specified assignee to the target
     * assignee, both at the specified galaxy. This empties the inventory for
     * the specified assignee. If {@code from} does not exist this method does
     * nothing; if the target assignee does not exist one is created. If the
     * target assignee owns the same resource its amount is added to.
     * 
     * @param from the source assignee
     * @param to the target assignee
     * @param gxy a galaxy constant
     * @throws IllegalArgumentException if {@code to} is the empty string
     * @throws NullPointerException if an argument is {@code null}
     */
    static void inventoryMove(String from, String to, SWGCGalaxy gxy) {
        if (from == null || to == null || gxy == null)
            throw new NullPointerException("Something is null");

        synchronized (inventoryMap) {
            List<SWGInventoryWrapper> iwl = inventory(from, gxy, false);
            if (iwl == null) return;
            for (SWGInventoryWrapper wr : iwl) {
                wr.setAssignee(to);
                wr.equalAddSub = "+";
                inventoryAdd(wr, gxy);
                wr.equalAddSub = null;
            }
            iwl.clear();
        }
    }

    /**
     * Helper method which returns inventory notes for the specified resource,
     * or the empty string. This implementation scans all assignees at the
     * specified galaxy who own stacks of the specified resource and returns the
     * first non-empty notes it finds. If the resource is not in stock or if no
     * notes exists the empty string is returned.
     * 
     * @param kr the resource
     * @param gxy a galaxy constant
     * @return the notes string, or the empty string
     * @throws NullPointerException if an argument is {@code null}
     */
    public static String inventoryNotes(SWGKnownResource kr, SWGCGalaxy gxy) {
        List<SWGInventoryWrapper> ws = inventoryBy(kr, gxy);
        for (SWGInventoryWrapper w : ws) {
            String s = w.getNotes();
            if (!s.isEmpty()) return s;
        }
        return "";
    }

    /**
     * Removes the specified wrapper from the specified galaxy. If the specified
     * inventory wrapper is not found, or if its assignee does not exist, this
     * method does nothing.
     * 
     * @param iw an inventory wrapper
     * @param gxy a galaxy constant
     * @throws NullPointerException if an argument is {@code null}
     */
    static void inventoryRemove(SWGInventoryWrapper iw, SWGCGalaxy gxy) {
        synchronized (inventoryMap) {
            List<SWGInventoryWrapper> wl = inventory(
                    iw.getAssignee(), gxy, false);
            if (wl != null)
                wl.remove(iw);
        }
    }

    /**
     * Creates and returns a new comparator for Kashyyykian and Mustafarian
     * resources. This comparator's {@code compareTo(SWGKnownResource)} returns
     * 0 for a resource that is not from Kashyyyk or Mustafar and if it is a
     * creature resource, otherwise -1.
     * 
     * @return a resource comparator
     */
    public static Comparable<SWGKnownResource> kmResourceComparator() {
        return new Comparable<SWGKnownResource>() {
            @Override
            public int compareTo(SWGKnownResource o) {
                if (o.rc().isSub(SWGCreatureResources.class))
                    return 0; // retain creature res also from K & M

                if (o.availabilityFor(SWGPlanet.KASHYYYK) != null ||
                        o.availabilityFor(SWGPlanet.MUSTAFAR) != null)
                    return -1;

                return 0;
            }
        };
    }

    /**
     * Creates a monitor for the specified resource and adds it to the monitors
     * for the current galaxy. If a monitor for the specified resource already
     * exists this method does nothing.
     * 
     * @param resource the resource to monitor
     * @param s notes text, or {@code null}
     */
    private static void monitorAddAction(SWGKnownResource resource, String s) {
        synchronized (monitors) {
            if (!monitorExists(resource)) {
                SWGMonitor m = new SWGMonitor(resource);
                monitors(SWGResourceTab.galaxy()).add(m);
                m.notes(s);
            }
            check();
        }
    }

    /**
     * Creates and returns a menu item for monitoring the specified resource.
     * The action for this menu creates a new monitor, adds it to the list of
     * monitors for the current galaxy and triggers an update of the table of
     * monitors. If the resource is {@code null}, not spawning, or if a monitor
     * already exists the menu item is disabled.
     * 
     * @param kr a known resource
     * @param s notes text, or {@code null}
     * @return a menu item
     */
    public static JMenuItem monitorAddMenu(
            final SWGKnownResource kr, final String s) {

        JMenuItem mm = new JMenuItem("Add monitor");
        mm.setToolTipText("Add selected resource to the list of monitors");
        mm.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e1) {
                monitorAddAction(kr, s);
                SWGResourceTab.currentUpdateGUI();
            }
        });
        mm.setEnabled(kr != null && kr.id() > 0 && !kr.isDepleted()  
                && !monitorExists(kr));
        return mm;
    }

    /**
     * Determines if a monitor exists for the specified resource.
     * 
     * @param resource the resource to check for
     * @return {@code true} if a monitor exists for the specified resource
     * @throws NullPointerException if the argument is {@code null}
     */
    private static boolean monitorExists(SWGKnownResource resource) {
        synchronized (monitors) {
            for (SWGMonitor m : monitors(resource.galaxy()))
                if (m.getResource() == resource)
                    return true;

            return false;
        }
    }

    /**
     * Removes {@code monitor} from the list of monitors for the current galaxy.
     * 
     * @param monitor the monitor to remove
     */
    static void monitorRemove(SWGMonitor monitor) {
        synchronized (monitors) {
            List<SWGMonitor> mons = monitors(SWGResourceTab.galaxy());
            mons.remove(monitor);
            check();
        }
    }

    /**
     * Returns a list of monitors for the specified galaxy, or an empty list.
     * Any overaged monitor is purged from the list before it is returned, see
     * {@code MONITOR_MAX_AGE}.
     * 
     * @param gxy a galaxy constant
     * @return a list of resource monitors, or an empty list
     * @throws NullPointerException if an argument is {@code null}
     */
    static List<SWGMonitor> monitors(SWGCGalaxy gxy) {
        if (gxy == null) throw new NullPointerException("Galaxy is null");

        synchronized (monitors) {
            List<SWGMonitor> mons = monitors.get(gxy);
            if (mons == null) {
                mons = new ArrayList<SWGMonitor>();
                monitors.put(gxy, mons);
            }
            monitorsPurge(mons);
            return mons;
        }
    }

    /**
     * Helper method which returns {@code true} if a monitor is triggered for a
     * depleted resource. This method, for each galaxy with monitors it invokes
     * {@code monitorsCheck(galaxy)} and returns {@code true} for the first
     * monitor that is triggered. Otherwise, once all monitors are scanned this
     * method returns {@code false}.
     * 
     * @return {@code true} if a monitor is triggered
     */
    private static boolean monitorsCheck() {
        synchronized (monitors) {
            for (SWGCGalaxy g : monitors.keySet())
                if (monitorsCheck(g)) return true;

            return false;
        }
    }

    /**
     * Helper method which returns {@code true} if a monitor is triggered for a
     * depleted resource. This method obtains monitors for the galaxy from
     * {@code monitors(galaxy)} which implicitly purges any overaged monitor.
     * <p>
     * Any call to this method is assumed to already be synchronized.
     * 
     * @param galaxy a galaxy constant
     * @return {@code true} if at least one monitor is triggered
     * @throws NullPointerException if the argument is {@code null}
     */
    private static boolean monitorsCheck(SWGCGalaxy galaxy) {
        for (SWGMonitor m : monitors(galaxy))
            if (m.isDepleted())
                return true;

        return false;
    }

    /**
     * Helper method which purges overaged monitors from the specified list.
     * After this task no list element is older than {@code MONITOR_MAX_AGE}.
     * <p>
     * Any call to this method is assumed to already be synchronized.
     * 
     * @param monitorz a list of monitors
     */
    private static void monitorsPurge(List<SWGMonitor> monitorz) {
        long now = System.currentTimeMillis() / 1000L;
        Iterator<SWGMonitor> iter;
        for (iter = monitorz.iterator(); iter.hasNext();)
            if (iter.next().getMonitorStarted() + MONITOR_MAX_AGE < now)
                iter.remove();
    }

    /**
     * Return a color-data-triplet for the specified resource and age. This
     * method determines the back- and foreground colors by the resource class
     * and which colors the user has specified for fair, good, and great colors.
     * The age is wrapped in a {@link Long}.
     * 
     * @param res a resource
     * @param age age in seconds
     * @param tooltip a text that is used as a fly-over tooltip text
     * @return an object with the age and back- and foreground color
     */
    public static TableCellDecorations resourceAgeDecor(
            SWGKnownResource res, long age, String tooltip) {

        Color bg, fg;
        if (age == Integer.MAX_VALUE || age < 0) {
            bg = SWGGuiUtils.colorDepleted;
            fg = Color.WHITE;
        } else if (res.rc().isJTL()) { // lifetime is 13 to 22 days
            if (age < (13 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[0];
                fg = SWGGuiUtils.statColors[1];
            } else if (age < (15.25 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[2];
                fg = SWGGuiUtils.statColors[3];
            } else if (age < (17.5 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[4];
                fg = SWGGuiUtils.statColors[5];
            } else {
                bg = SWGGuiUtils.colorDepleted; // not yet but... ;)
                fg = Color.WHITE;
            }
        } else if (SWGOrganic.class.isAssignableFrom(res.getClass())) {
            // lifetime is 6 to 22 days
            if (age < (6 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[0];
                fg = SWGGuiUtils.statColors[1];
            } else if (age < (10.25 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[2];
                fg = SWGGuiUtils.statColors[3];
            } else if (age < (14.5 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[4];
                fg = SWGGuiUtils.statColors[5];
            } else {
                bg = SWGGuiUtils.colorDepleted;
                fg = Color.WHITE;
            }
        } else { // anything else, lifetime is 6 to 11 days
            if (age < (6 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[0];
                fg = SWGGuiUtils.statColors[1];
            } else if (age < (7.25 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[2];
                fg = SWGGuiUtils.statColors[3];
            } else if (age < (8.5 * 24 * 3600)) {
                bg = SWGGuiUtils.statColors[4];
                fg = SWGGuiUtils.statColors[5];
            } else {
                bg = SWGGuiUtils.colorDepleted;
                fg = Color.WHITE;
            }
        }

        return new TableCellDecorations(bg, fg, tooltip, (Object[]) null);
    }

    /**
     * Returns a resource class constant for the specified resource name. Any
     * exception for an invalid argument is intercepted and a message is written
     * to SWGAide's error log. This method should only be used by methods that
     * does not handle user's entered input but handles what is assumed to be
     * correct resource class names.
     * 
     * @param rcName the proper name for a resource class, or its token
     * @return a resource class constant
     */
    static SWGResourceClass resourceClassFrom(String rcName) {
        try {
            return SWGResourceClass.rc(rcName);
        } catch (IllegalArgumentException e) {
            SWGAide.printError(
                    "SWGResController:resourceClassFromString" + rcName, e);
            return null;
        }
    }

    /**
     * Appends resource data for the specified resource to the string. This
     * method adds three or four lines, all lines are finished with an
     * end-of-line character; if the resource is not in spawn the planet
     * availability information line is omitted. The format is:
     * 
     * <pre>
     * ResourceName   
     * Resource Class Name   Age: N a
     * ER=123 DR=456 HR=789 ...
     * Corellia, Naboo, ... </pre>
     * 
     * If the resource origins from another galaxy than what the currently
     * selected character pertains to "(Galaxy)" is prepended to the resource
     * name.
     * 
     * @param kr a resource
     * @param z a modifiable string
     * @throws NullPointerException if an argument is {@code null}
     */
    public static void resourceDetails(SWGKnownResource kr, ZString z) {
        SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        if (kr.galaxy() != gxy) z.app('(').app(gxy.getName()).app(')');
        z.appnl(kr.getName()).app(kr.rc().rcName());
        z.app("\tAge: ").appnl( SWGResController.dateString(kr.age()));
        kr.stats().toString(z, true);
        z.nl();

        if (!kr.isDepleted()) {
            boolean first = true;
            List<SWGPlanet> pls = kr.availability();
            SWGPlanet.sort(pls);
            for (SWGPlanet p : pls) {
                if (!first) z.app(", ");
                z.app(p.getName());
                first = false;
            }
            z.nl();
        }
    }

    /**
     * Creates and returns a string for the specified resource. This method
     * first invokes {@link #resourceDetails(SWGKnownResource, ZString)}, and
     * then appends the following: if the resource is in spawn the reporter is
     * appended, and, if the resource exists in inventory the amount and the
     * first notes found is appended, if any notes.
     * 
     * @param kr a resource
     * @param gxy a galaxy constant
     * @return a string about the resource
     * @throws NullPointerException if an argument is {@code null}
     */
    public static String resourceDetails(SWGKnownResource kr, SWGCGalaxy gxy) {
        ZString z = new ZString();

        resourceDetails(kr, z);

        if (!kr.isDepleted())
            z.app("Rep by: ").appnl(kr.availableFirst().availableBy());

        long amount = inventoryAmount(kr, gxy);
        if (amount >= 0) {
            z.app("Inventory: ").app(
                    ZNumber.asText(amount, true, true)).app(" units");
            String n = inventoryNotes(kr, gxy);
            if (!n.isEmpty()) z.app(": \"").app(n).app("\"");
        }
        return z.toString();
    }

    /**
     * Creates and returns a menu item which action is to display a dialog with
     * details about the specified resource over the identified component, very
     * much like the Details panel at Current Resources. If the specified
     * resource is {@code null} the returned menu item is disabled.
     * 
     * @param kr a resource
     * @param comp the component for the information dialog
     * @return a menu item
     * @throws NullPointerException the component is {@code null}
     */
    public static JMenuItem resourceDetailsMenu(
            final SWGKnownResource kr, final Component comp) {

        if (comp == null)
            throw new NullPointerException("An argument is null");

        JMenuItem jm = new JMenuItem("Resource details...");
        jm.setToolTipText("Display details for the selected resource");
        if (kr != null)
            jm.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String s =
                            resourceDetails(kr, SWGFrame.getSelectedGalaxy());
                    s = s.replaceAll("\t", "        ");
                    JOptionPane.showMessageDialog(comp, s, "Details",
                            JOptionPane.PLAIN_MESSAGE);
                }
            });
        jm.setEnabled(kr != null);
        return jm;
    }

    /**
     * Returns a sorted set of resources from the two collections that match the
     * specified class. The set contains resources that subclass the specified
     * class and it is sorted by the rate that is determined by the specified
     * weights and class; if the argument is {@link SWGWeights#LQ_WEIGHTS} caps
     * are derived from the resources themselves, otherwise from the specified
     * resource class; a recycled or a space resource only considers OQ.
     * 
     * @param rc a resource class constant
     * @param w a weights object
     * @param spawn a set of spawning resources
     * @param inv a list of inventory resources
     * @return a sorted set of resources
     */
    public static SWGResourceSet resources(
            final SWGResourceClass rc, final SWGWeights w,
            SWGResourceSet spawn, List<SWGInventoryWrapper> inv) {

        SWGResourceSet res = new SWGResourceSet(128);
        Class<? extends SWGResourceClass> crc = rc.getClass();
        for (SWGInventoryWrapper e : inv) {
            SWGKnownResource kr = e.getResource();
            if (kr.rc().isSub(crc))
                res.add(kr);
        }

        // the resource-set type guarantees the mathematical set property of
        // unique elements, hence, if the inventory contains duplicates the set
        // contains the first that is found

        res.addAll(spawn.subsetBy(rc));

        res.sort(new Comparator<SWGKnownResource>() {
            @Override
            public int compare(SWGKnownResource o1, SWGKnownResource o2) {
                SWGResourceClass rc1, rc2;
                boolean hq = w != SWGWeights.LQ_WEIGHTS;
                if (!hq) {
                    rc1 = o1.rc();
                    rc2 = o2.rc();
                } else
                    rc1 = rc2 = rc;

                double w1 = o1.rc().isSpaceOrRecycled()
                        ? o1.stats().value(Stat.OQ)
                        : w.rate(o1, rc1, hq);
                double w2 = o2.rc().isSpaceOrRecycled()
                        ? o2.stats().value(Stat.OQ)
                        : w.rate(o2, rc2, hq);

                int ret = Double.compare(w1, w2);
                return w == SWGWeights.LQ_WEIGHTS
                        ? ret // worst first
                        : -ret; // else best
            }
        });

        return res;
    }

    /**
     * A table cell renderer for the age of a resource.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    @SuppressWarnings("serial")
    public static class AgeRenderer extends SWGDecoratedTableCellRenderer {

        /**
         * Creates a age table cell renderer.
         * 
         * @param tableModel a table model for a decorated table
         */
        public AgeRenderer(DecoratedTableModel tableModel) {
            super(tableModel);
        }

        @Override
        protected void mySetValue(Object value) {
            long age = ((Long) value).longValue();
            String s = dateString(age);
            super.setValue(s);
        }
    }

    // /**
    // * Returns the URL for the "gloomy" sound clip which is played when a
    // * harvester is idling or a monitor alarms on a depleted resource.
    // *
    // * @return the alarm URL
    // */
    // public URL getAlarm() {
    // return null; // alarm;
    // }
    //
    // /**
    // * Returns the URL for the "happy" sound clip which is played when a guard
    // * is triggered.
    // *
    // * @return the alert
    // */
    // public URL getAlert() {
    // return null; // alert;
    // }
    //
    // /**
    // * Returns the URL for the "warning" sound clip which is played when a
    // * harvester soon will begin to idle.
    // *
    // * @return the warning URL
    // */
    // public URL getWarning() {
    // return null; // aWarn;
    // }
    //
    // /**
    // * Sets the URL for the "gloomy" sound clip to play when a harvester is
    // * idling or a monitor is triggered.
    // * <p>
    // * <b>Unimplemented</b>
    // *
    // * @param gloomyClip the URL for the alarm sound clip
    // */
    // public void setAlarm(@SuppressWarnings("unused") URL gloomyClip) {
    // // XXX: implement at some options dialog
    // // also see setAlert(URL)
    // }
    //
    // /**
    // * Sets the URL for the "happy" sound clip to play when a guard is
    // * triggered, see {@code alert}.
    // * <p>
    // * <b>Unimplemented</b>
    // *
    // * @param happyClip the URL for the alert sound clip
    // */
    // public void setAlert(@SuppressWarnings("unused") URL happyClip) {
    // // implement at some options dialog
    // }
    //
    // /**
    // * Sets the URL for the "warning" sound clip to play when a harvester soon
    // * begins to idle.
    // * <p>
    // * <b>Unimplemented</b>
    // *
    // * @param warningClip the URL for the warning sound clip
    // */
    // public void setWarning(@SuppressWarnings("unused") URL warningClip) {
    // // also see setAlert(URL)
    // }
}
