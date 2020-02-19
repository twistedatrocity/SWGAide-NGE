package swg.gui.schematics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import swg.SWGAide;
import swg.crafting.SWGValues;
import swg.crafting.SWGWeights;
import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.crafting.resources.ResourceUpdate;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.types.SWGCreatureResources;
import swg.crafting.resources.types.SWGOrganic;
import swg.crafting.schematics.SWGExperimentGroup;
import swg.crafting.schematics.SWGExperimentLine;
import swg.crafting.schematics.SWGResourceSlot;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDoTask;
import swg.gui.common.SWGSac;
import swg.gui.common.SWGDoTask.TaskCallback;
import swg.gui.resources.SWGInventoryWrapper;
import swg.gui.resources.SWGResController;
import swg.model.SWGCGalaxy;
import swg.model.SWGCharacter;
import swg.swgcraft.SWGCraftCache.CacheUpdate;
import swg.swgcraft.SWGCraftCache.CacheUpdate.UpdateType;
import swg.swgcraft.SWGResourceManager;

/**
 * This type is meant as a controller and a hub for data structures related to
 * schematics that are shared between the GUI elements but is not part of a pure
 * model. All access to mutable structures and are prone to thread-corruption
 * are synchronized, on the smallest scope possible. Return value should be
 * treated as <b>read-only</b> if nothing else is stated in the method comments.
 * <p>
 * This type also provides some common helper methods.
 * <p>
 * As many as possible of the methods are static which allows for access without
 * a chain of references to this type. This means that an instance of this type
 * must be created early on, but just once &mdash; currently the constructor for
 * {@link SWGSchematicTab} performs this task.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final public class SWGSchemController implements UpdateSubscriber {

    /**
     * The currently selected galaxy
     */
	private SWGCGalaxy galaxy;
	
	/**
     * A list of inventory wrappers for the current galaxy. This member is an
     * empty list until a character is selected.
     */
    private static List<SWGInventoryWrapper> inventory;
    
    /**
     * A lock object used by some methods must synchronize but which do not need
     * an object-wide lock.
     */
    private static final String LOCK = "check";

    /**
     * A list of elements which each is a unique resource-class-and-weight pair.
     */
    private static List<SWGRCWPair> rcwPairs;

    /**
     * A reference to the GUI schematics tab.
     */
    private SWGSchematicTab schemTab;

    /**
     * A list of schematic wrappers.
     */
    private static List<SWGSchematicWrapper> schemWrappers;

    /**
     * A set of spawning resources for the current galaxy. This member is an
     * empty set until a character is selected.
     */
    private static SWGResourceSet spawning;

    /**
     * Creates an instance of this type.
     * 
     * @param schemTab the schematics GUI tab
     * @throws IllegalStateException if an instance already exists
     */
    @SuppressWarnings("unchecked")
	public
    SWGSchemController(SWGSchematicTab sTab) {
        // if (expGroupTitlesFirstPrio != null)
        // throw new IllegalStateException("An instance exists already");

        // "schemExpGroupTitlesFirstPrio"

        schemTab = sTab;

        schemWrappers = (List<SWGSchematicWrapper>) SWGFrame.getPrefsKeeper().
                get("schemWrappers", new ArrayList<SWGSchematicWrapper>());

        SWGSchematicsManager.addSubscriber(this);
        SWGResourceManager.addSubscriber(this);
        
        SWGAide.frame().addExitCallback(new SWGDoTask(new TaskCallback() {

        	public void execute() {
        		doExit();
        	}
        }));
    }
    
    /**
     * This method performs some exit routines when SWGAide is closing down. In
     * particular, this method iterates over the collections of schematic wrappers
     * and if for a galaxy any notes or stock is empty then it
     * is removed from its containing map as there is no need to save a vanilla schem wrapper
     */
    private void doExit() {
        synchronized (LOCK) {
        	Iterator<SWGSchematicWrapper> iter = schemWrappers.iterator();
        	while (iter.hasNext()) {
        		SWGSchematicWrapper wr = iter.next();
        		if(wr.stock()<1 && wr.notes() == null) {
                    iter.remove();
        		}
            }
        }
    }

    @Override
    public void handleUpdate(UpdateNotification u) {
    	if (u instanceof CacheUpdate
                && ((CacheUpdate) u).type == UpdateType.SCHEMATICS) {
            rcwPairs = null;
        } else if (u instanceof ResourceUpdate) {
        	ResourceUpdate ru = (ResourceUpdate) u;
            if (ru.optional != null && ru.optional == SWGFrame.getSelectedGalaxy()) {
                synchronized (SWGSchemController.class) {
                    spawning = null;
                    inventory = null;
                }
                updateGalaxy();
            }
        }
    }

    /**
     * Returns the priority for the specified experimental group and schematic.
     * This method returns the priority that the user has graded the experiment
     * ( TODO ), or the nature of the specified experiment, which is a positive
     * value for a prioritized group, zero for default, and a negative value for
     * a down-prioritized group.
     * <p>
     * Currently this method scans its internal list of categories, schematic,
     * and experiments and returns 1 if a group is listed as prioritized and -1
     * if down; the default return value is 0.
     * 
     * @param g an experimental group
     * @param s a schematic
     * @return negative, 0, or positive
     * @throws NullPointerException if an argument is {@code null}
     */
    static int getPriority(SWGExperimentGroup g, SWGSchematic s) {
        if (!SWGSchematicsManager.isQuality(s)) return -1;

        // shortcut, there is no prio if just one group
        if (s.getExperimentGroups().size() == 1) return 0;

        // TODO: add features for user grading

        int cid = s.getCategory();
        String sn = s.getName();
        String gd = g.getDescription();

        if (cid == 19 /* component in component */
                && (sn.startsWith("Element Process") || sn.startsWith("Enzyme ")))
            return gd.contains("Purity")
                    ? 1
                    : -1;

        else if (cid == 20 || cid == 21) // food or drink
            return gd.contains("Nutrition")
                    ? 1
                    : -1;

        else if (cid == 98) // installation deed
            return gd.contains("Efficiency")
                    ? 1
                    : -1;

        else if (cid == 440) // Droid Defensive Module Harness
            return gd.contains("Effectiveness")
                    ? 1
                    : -1;

        else if (s.getID() == 1427) // Incubator
            return gd.contains("Quality")
                    ? 1
                    : -1;

        return 0; // default
    }

    /**
     * Returns a list of inventory wrappers for the current galaxy, or an empty
     * list if the user has not selected a character or if no inventory exists
     * for the galaxy.
     * 
     * @return a set of spawning resources
     */
    static List<SWGInventoryWrapper> inventory() {
        synchronized (SWGSchemController.class) {
            if (inventory == null) updateGalaxy();
            return inventory;
        }
    }

    /**
     * Adds to the specified list those instances of {@link SWGRCWPair} that
     * matches the specified schematic and HQ value. In particular, this method
     * adds an instance to the list if {@link SWGRCWPair#schematics()} contains the
     * specified schematic and if {@link SWGRCWPair#isHQ()}{@code == hq}. This
     * method does not add duplicates to the list.
     * 
     * @param s a schematic
     * @param hq {@code true} for HQ schematics
     * @param ls a list of RCWPairs
     */
    void rcwPair(SWGSchematic s, boolean hq, List<SWGRCWPair> ls, SWGCGalaxy gxy) {
        for (SWGRCWPair r : rcwPairs(gxy))
            if (hq == r.isHQ() && r.schematics().contains(s) && !ls.contains(r))
                ls.add(r);
    }

    /**
     * Returns an ordered list of resource-class-and-weight pairs for all
     * schematics. Each element in the list is a unique pair of resource-class
     * and weight, and each element has an ordered list of schematics that use
     * the combination. High-quality schematics with experimental groups that
     * match {@link SWGSchematicsManager#isQuality(SWGSchematic)} use experimental weights.
     * Low-quality schematics share {@link SWGRCWPair#LQ_FILTER}, a plain filter
     * with OQ = 1.
     * <p>
     * Sort order for main list:<br/>
     * {@link SWGResourceClass#compareTo(SWGResourceClass)} and
     * {@link SWGValues#compareTo(SWGValues)}<br/>
     * Schematics are sorted by {@link SWGSchematic#compareTo(SWGSchematic)}.
     * <p>
     * The return value is read-only.
     * 
     * @return a list of pairs
     */
    List<SWGRCWPair> rcwPairs(SWGCGalaxy gxy) {
    	SWGCGalaxy sgxy = SWGFrame.getSelectedGalaxy();
    	if(galaxy == null) {
    		galaxy = sgxy;
    	}
        synchronized (SWGRCWPair.class) {
        	if (rcwPairs != null) {
        		for (SWGRCWPair p : rcwPairs) {
        			if(!p.schematics().get(0).getBase().equals(galaxy.getType())) {
        				rcwPairs = null;
        				break;
        			}
        		}
        	}
            if (rcwPairs == null) {
                rcwPairs = rcwPairsInit(gxy);
            }
            return rcwPairs;
        }
    }

    /**
     * Returns a list of {@link SWGRCWPair}s. This method iterates over the
     * specified list of schematics and adds all matching {@link SWGRCWPair} to the
     * returned list. The returned list contains only elements that pertains to
     * the specified schematics and each element is unique.
     * <p>
     * This implementation filters for high-quality or low-quality determined by
     * the {@code hq}, see {@link SWGRCWPair#isHQ()}.
     * 
     * @param hq {@code true} for HQ resources
     * @param schems a list of schematics
     * @return a list of {@link SWGRCWPair}s
     */
    List<SWGRCWPair> rcwPairs(boolean hq, List<SWGSchematic> schems, SWGCGalaxy gxy) {
        List<SWGRCWPair> ret = new ArrayList<SWGRCWPair>(48);
        for (SWGSchematic s : schems)
            rcwPair(s, hq, ret,gxy);

        return ret;
    }

    /**
     * Returns list of resource-class-and-weight pairs for schematics that can
     * use the specified resource class. The boolean argument determines if the
     * schematics must be HQ or LQ. Each element in the list is a unique pair of
     * resource-class and weight, and each element has an ordered list of
     * schematics that use the combination. For LQ schematics the weight is
     * {@link SWGRCWPair#LQ_FILTER}, a plain filter with OQ = 1.
     * <p>
     * Notice that the returned list contains pairs for resource classes that
     * are super-classes to the specified argument.
     * 
     * @param rc a resource class constant
     * @param hq {@code true} for HQ pairs
     * @return a filtered list of HQ pairs
     */
    private List<SWGRCWPair> rcwPairs(SWGResourceClass rc, boolean hq, SWGCGalaxy gxy) {
        List<SWGRCWPair> rl = rcwPairs(gxy);
        ArrayList<SWGRCWPair> ret = new ArrayList<SWGRCWPair>(rl.size());
        for (SWGRCWPair r : rl)
            if (hq == r.isHQ() && rc.isSub(r.rc())) ret.add(r);

        return ret;
    }

    /**
     * Helper method which creates and returns a list of rcw-pairs. This method
     * populates a list of pairs, sorts the list, and finally it iterates over
     * the the pairs to sort each pair's list of schematics.
     * 
     * @return a list of rcw-pairs
     */
    private List<SWGRCWPair> rcwPairsInit(SWGCGalaxy gxy) {
        List<SWGSchematic> schems = SWGSchematicsManager.getSchematics(gxy);
        List<SWGRCWPair> ret = new ArrayList<SWGRCWPair>(schems.size() / 2); // rough
        for (SWGSchematic s : schems) {
            boolean hq = SWGSchematicsManager.isQuality(s);
            for (SWGResourceSlot r : s.getResourceSlots()) {
                SWGResourceClass rc = r.getResourceClass();
                if (hq) {
                    for (SWGExperimentGroup g : s.getExperimentGroups())
                        if (getPriority(g, s) >= 0)
                            for (SWGExperimentLine l : g.getExperimentalLines())
                                rcwPairsInitAdd(ret, s, rc, l.getWeights());

                } else
                    rcwPairsInitAdd(ret, s, rc, null);

            } // end for-resource-slots
        } // end for-schematics

        Collections.sort(ret);
        for (SWGRCWPair p : ret)
            Collections.sort(p.schematics());

        return ret;
    }

    /**
     * Helper method which appends to the list. If the pair is {@code null} a
     * new instance is created and added to the list, otherwise the pair exists
     * and the specified schematic is added to the list of schematics that
     * pertain to the pair.
     * 
     * @param list a list of instances
     * @param s a schematic
     * @param rc a resource class
     * @param w experimental weights, or {@code null} for low-quality
     */
    private static void rcwPairsInitAdd(List<SWGRCWPair> list,
            SWGSchematic s, SWGResourceClass rc, SWGWeights w) {

        SWGRCWPair rcwPair = rcwPairsInitGet(list, rc, w);

        if (rcwPair == null)
            list.add(new SWGRCWPair(rc, w, s));
        else
            rcwPair.schematics().add(s);
    }

    /**
     * Helper method which returns an instance for the specified arguments. If
     * the list contains an instance that matches the resource class and weights
     * it is returned, otherwise this method returns {@code null}.
     * 
     * @param pairs a list of RCWPairs
     * @param rc a resource class
     * @param w a weight instance, or {@link SWGRCWPair#LQ_FILTER}
     * @return the instance, or {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    private static SWGRCWPair rcwPairsInitGet(
            List<SWGRCWPair> pairs, SWGResourceClass rc, SWGValues w) {

        for (SWGRCWPair e : pairs)
            if (e.equals(rc, w))
                return e;

        return null;
    }

    /**
     * Selects the specified resource class at the panel for resource class use.
     * If the argument is {@code null} this method does nothing. This method
     * resets any selected profession at the target GUI panel and selects the
     * resource class.
     * <p>
     * This method can only be invoked if this type is instantiated.
     * 
     * @param rc a resource class constant
     */
    public void resClassSelect(SWGResourceClass rc) {
        schemTab.resClassSelect(rc);
    }

    /**
     * Returns a menu item with an action listener for the schematics section
     * and its panel for resource class use. If the specified resource class is
     * {@code null} the menu item is disabled, otherwise, if the argument is
     * specified the resource class is selected.
     * <p>
     * This method can only be invoked if this type is instantiated.
     * 
     * @param rc a resource class constant, or {@code null}
     * @return a menu item
     */
    public JMenuItem resClassUse(SWGResourceClass rc) {
        return schemTab.resClassUse(rc);
    }

    /**
     * Returns from the specified list a list of schematics which are HQ or LQ,
     * or the list as-is (which resembles All). This method filters the list
     * determined by the integer argument: a negative values denotes LQ and a
     * positive value denotes HQ, and the list as-is if the integer is zero.
     * 
     * @param schems a list of schematics
     * @param lqAHq negative, 0, or positive for LQ, All, or HQ respectively
     * @return a filtered list, or the argument as-is
     * @throws NullPointerException if the argument is {@code null}
     */
    static List<SWGSchematic> schematics(List<SWGSchematic> schems, int lqAHq) {
        if (lqAHq == 0 || schems.isEmpty()) return schems;

        List<SWGSchematic> ret = new ArrayList<SWGSchematic>(schems.size());
        for (SWGSchematic s : schems) {
            if (SWGSchematicsManager.isQuality(s)) {
                if (lqAHq > 0)
                    ret.add(s);
            } else if (lqAHq < 0) ret.add(s);
        }
        return ret;
    }

    /**
     * Returns a sorted list of HQ schematics that can use the specified
     * resource, or an empty list. Each element is a sac per the following: <br/>
     * <dl>
     * <dt>obj</dt>
     * <dd>an instance of {@link SWGSchematic}</dd>
     * <dt>objects.get(0)</dt>
     * <dd>a {@link Double} that is the rate of the specified resource for the
     * schematic that is referenced in {@code obj}</dd>
     * <dt>objects.get(1)</dt>
     * <dd>a {@link SWGRCWPair} that pertains to the schematic and the resource;
     * this object established the rate that is calculated</dd>
     * </dl>
     * <p>
     * This method iterates over all schematics that calls for the resource
     * class of the specified resource, or rather their experimental lines; the
     * schematic is added to the returned list if the weighed rate of the
     * resource is greater than the specified limit. The returned list contains
     * only HQ schematics without duplicates. The sort order is: first per
     * resource class from specific to more generic; then each resource class
     * group is sorted per weighed rate. If no matching schematic is found this
     * method returns an empty list.
     * <p>
     * <b>Notice:</b> a resource may match several experimental lines. This
     * method regards only the best rate.
     * <p>
     * The boolean argument determines if creature resources should be regarded
     * for schematics that call for Organic; it is easier to harvest Organic as
     * Flora and they have similar stats.
     * 
     * @param kr a resource
     * @param limit the minimum rate for inclusion
     * @param creature {@code true} to include creature for Organic
     * @return a sorted list of HQ schematics
     */
    List<SWGSac> schematics(
            final SWGKnownResource kr, double limit, boolean creature, SWGCGalaxy gxy) {

        List<SWGRCWPair> rl = rcwPairs(kr.rc(), true, gxy);
        if (rl.isEmpty()) return Collections.emptyList();

        if (!creature && kr.rc() instanceof SWGCreatureResources)
            for (Iterator<SWGRCWPair> iter = rl.iterator(); iter.hasNext();)
                if (iter.next().rc() == SWGOrganic.getInstance())
                    iter.remove();

        // sort rcw-pairs on rate before iterating over schematics, best first
        Collections.sort(rl, SWGRCWPair.comparator(kr));
        Collections.sort(rl, SWGRCWPair.comparatorRC(true));

        List<SWGSac> ret = new ArrayList<SWGSac>(rl.size());
        Object ex = new Object();
        for (SWGRCWPair r : rl) {
            double w = ((SWGWeights) r.filter()).rate(kr, r.rc(), true);
            if (w < limit) continue;

            for (SWGSchematic s : r.schematics()) {
                if (SWGSchematicsManager.isQuality(s)) {
                	List<SWGExperimentGroup> gr = s.getExperimentGroups();
                	for (SWGExperimentGroup g : gr) {
                		for (SWGExperimentLine l : g.getExperimentalLines()) {
                			if(l.getWeights().equals(r.filter())) {
                				ex = l.getDescription();
                				if(ex == null) ex = g.getDescription();
                			}
                		}
                	}
                    ret.add(new SWGSac(s, Double.valueOf(w), r, ex));
                }
            }
        }

        return ret;
    }

    /**
     * Returns a menu item for selecting a specified schematic at select
     * schematics GUI panels. If the specified schematic is {@code null} the
     * menu item is disabled. If {@code src == null} all panels are invoked,
     * otherwise the source is assumed to be what triggers this invocation and
     * the source itself is not invoked.
     * <p>
     * Use this menu item for use outside the Schematics section.
     * 
     * @param s a schematic
     * @param src the source of the invocation, or {@code null}
     * @return a menu item
     */
    JMenuItem schematicSelectMenu(SWGSchematic s, JComponent src) {
        JMenuItem m = schemTab.schematicSelectMenu(s, src);
        m.setToolTipText("Select schematic at Schematics panels");
        return m;
    }

    /**
     * Returns a sorted list of LQ schematics which explicitly call for the
     * resource class of the specified argument, or an empty list. Each element
     * is a sac per the following (mimicking with the elements of the returned
     * value {@link #schematics(SWGKnownResource, double, boolean)}):
     * <dl>
     * <dt>obj</dt>
     * <dd>an instance of {@link SWGSchematic}</dd>
     * <dt>objects.get(0)</dt>
     * <dd>a {@link Double} with value 0.0 because the schematics is LQ</dd>
     * <dt>objects.get(1)</dt>
     * <dd>a {@link SWGRCWPair} that pertains to the schematic and the resource;
     * this element equals the resource class of the argument</dd>
     * </dl>
     * <p>
     * This method iterates over all schematics and each one that is LQ and
     * calls for the resource class that calls for the resource class by name is
     * added to the returned list. The returned list contains only LQ schematics
     * without duplicates and it is sorted by name. The weight of the rcw-pairs
     * is {@link SWGRCWPair#LQ_FILTER}, a plain filter with OQ = 1.
     * 
     * @param kr a resource
     * @return a sorted list of LQ schematics
     */
    List<SWGSac> schematicsLQNamed(final SWGKnownResource kr) {
        List<SWGRCWPair> rl = rcwPairs(kr.rc(), false, kr.galaxy());
        if (rl.isEmpty()) return Collections.emptyList();

        List<SWGSchematic> tmp = new ArrayList<SWGSchematic>(rl.size() * 3);
        List<SWGSac> ret = new ArrayList<SWGSac>(rl.size());
        for (SWGRCWPair r : rl)
            if (r.rc() == kr.rc()) // only "named rc"
                for (SWGSchematic s : r.schematics())
                    if (!SWGSchematicsManager.isQuality(s) && !tmp.contains(s)) {
                        tmp.add(s);
                        ret.add(new SWGSac(s, Double.valueOf(0.0), r));
                    }

        return ret;
    }

    /**
     * Returns a set of resources that are spawning at the current galaxy, or an
     * empty set if the user has not selected a character or if there is no
     * resource reported at swgaide.com.
     * 
     * @return a set of spawning resources
     */
    static SWGResourceSet spawning() {
        synchronized (SWGSchemController.class) {
            if (spawning == null) updateGalaxy();
            
            spawning = SWGResourceManager.cachedSpawning(SWGFrame.getSelectedGalaxy());
            return spawning;
        }
    }

    /**
     * Helper method which updates this type. If a character is selected at the
     * main panel this method updates the temporary collections of spawning
     * resources and inventory resources for the galaxy that pertains to the
     * character; this is always done in the case the user adds resources to the
     * inventory. This method must just be invoked from {@link SWGSchematicTab}
     * when it gains focus and from {@link #handleUpdate(UpdateNotification)}.
     */
    static void updateGalaxy() {
        synchronized (SWGSchemController.class) {
            SWGCharacter c = SWGFrame.getSelectedCharacter();
            if (c != null) {
                spawning = SWGResourceManager.getSpawning(c.gxy());
                inventory = SWGResController.inventory(c.gxy());
            } else { // sanity
                spawning = SWGResourceSet.EMPTY;
                inventory = Collections.emptyList();
            }
        }
    }

    /**
     * Returns an instance of experimental weights for the specified schematic,
     * or {@code null}. This method returns the experimental weights of the
     * first group and its first line, as they are listed in the schematics XML
     * file from swgaide.com. If the schematic is {@code null} or LQ this
     * method returns {@code null}, see {@link #weightsWidest(SWGSchematic)}.
     * 
     * @param schem a schematic
     * @return experimental weights, or {@code null}
     */
    static SWGWeights weightsFirst(SWGSchematic schem) {
        if (schem == null || !SWGSchematicsManager.isQuality(schem)) return null;

        return schem.getExperimentGroups().get(0).
                getExperimentalLines().get(0).getWeights();
    }

    /**
     * Returns an instance of pre-defined or wider weights for the specified
     * schematic, or {@code null}. If the schematic is {@code null} or LQ, or if
     * no unique instance is determined this method returns {@code null}.
     * <p>
     * This method determines if the schematic contains just one weight, as next
     * step it iterates over all experimental groups, their lines, and their
     * weights to obtain the prioritized or widest instance; if a group is
     * prioritized by {@link #getPriority(SWGExperimentGroup, SWGSchematic)} its
     * weights is returned, otherwise, for all pair of weights this method
     * invokes {@link SWGWeights#wider(SWGWeights, SWGWeights)} but breaks as
     * soon as {@code null} is obtained, otherwise it returns the widest
     * weights; see also {@link #weightsFirst(SWGSchematic)}.
     * 
     * @param schem a schematic
     * @return experimental weights, or {@code null}
     */
    static SWGWeights weightsWidest(SWGSchematic schem) {
        if (schem == null || !SWGSchematicsManager.isQuality(schem)) return null;

        List<SWGExperimentGroup> egs = schem.getExperimentGroups();
        List<SWGExperimentLine> els = egs.get(0).getExperimentalLines();
        SWGWeights wg = els.get(0).getWeights(); // must be since HQ

        // just one possible weight
        if (egs.size() == 1 && els.size() == 1)
            return wg;

        // check if any group is prioritized
        for (SWGExperimentGroup eg : egs) {
            if (getPriority(eg, schem) > 0) {
                els = eg.getExperimentalLines();
                wg = els.get(0).getWeights(); // ensure wg is for this group

                if (els.size() == 1) return wg;

                for (SWGExperimentLine el : els) {
                    wg = SWGWeights.wider(wg, el.getWeights());
                    if (wg == null) return null; // no widest found
                }

                return wg; // it is prioritized group, no more search
            }
        }

        for (SWGExperimentGroup eg : egs)
            for (SWGExperimentLine el : eg.getExperimentalLines()) {
                wg = SWGWeights.wider(wg, el.getWeights());
                if (wg == null) return null; // no widest found
            }

        return wg;
    }

    /**
     * Adds the specified wrapper to the collection of schematics wrappers. This
     * implementation does not determine if the collection already contains the
     * wrapper. Hence, this collection allows for several instances which equal
     * each other logically. If the argument is {@code null} this method does
     * nothing.
     * 
     * @param w a schematic wrapper
     */
    static void wrapperAdd(SWGSchematicWrapper w) {
        if (w == null) return;
        synchronized (schemWrappers) {
            schemWrappers.add(w);
        }
    }

    /**
     * Returns a default schematic wrapper for the specified schematic. If the
     * argument is {@code null} or if the argument is an unknown schematic or if
     * no default exists, this method returns {@code null}; the two latter cases
     * should be impossible.
     * 
     * @param s a schematic
     * @return a schematic wrapper
     */
    static SWGSchematicWrapper wrapperDefault(SWGSchematic s) {
        if (s == null) return null;
        synchronized (schemWrappers) {
            // get list from wrappers in the case default is not yet created
            for (SWGSchematicWrapper w : wrappers(s))
                if (s.getID() == w.schemID() && w.isDefault()) return w;

            return null;
        }
    }

    /**
     * Removes the specified wrapper from the collection of schematics wrappers.
     * If the collection does not contain the wrapper, or if it is {@code null},
     * this method does nothing. For equality {@link Object#equals(Object)} is
     * used, which resembles to o1 == o2. Hence, this collection allows for
     * several instances which logically equal each other.
     * 
     * @param w a schematic wrapper
     */
    static void wrapperRemove(SWGSchematicWrapper w) {
        if (w == null || w.isDefault()) return;
        synchronized (schemWrappers) {
            schemWrappers.remove(w);
        }
    }

    /**
     * Returns an ordered list of schematics wrappers that pertains to the
     * specified schematic. The returned list contains at least a default
     * wrapper, see {@link SWGSchematicWrapper}. If the argument is {@code null}
     * this method returns an empty list.
     * <p>
     * The list is ordered with the default wrapper first and then in
     * alphabetical order by serial number. The list can be modified.
     * 
     * @param s a schematic
     * @return a list of wrappers, {@code size > 0}
     */
    static List<SWGSchematicWrapper> wrappers(SWGSchematic s) {
        if (s == null) return Collections.emptyList();
        synchronized (schemWrappers) {
            List<SWGSchematicWrapper> wl = new ArrayList<SWGSchematicWrapper>();
            for (SWGSchematicWrapper w : schemWrappers)
                if (s.getID() == w.schemID()) wl.add(w);

            if (wl.isEmpty()) {
                SWGSchematicWrapper sw = SWGSchematicWrapper.createDefault(s);
                wrapperAdd(sw);
                wl.add(sw);
            }

            Collections.sort(wl, SWGSchematicWrapper.comparator());
            return wl;
        }
    }
}
