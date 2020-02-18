package swg.gui.schematics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.schematics.SWGExperimentGroup;
import swg.crafting.schematics.SWGExperimentLine;
import swg.crafting.schematics.SWGResourceSlot;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.common.SWGGui;
import swg.gui.resources.SWGInventoryWrapper;
import swg.gui.resources.SWGResController;
import swg.tools.ZString;

/**
 * This type represents an experimental weight for one or several groups or
 * lines and it provides the logic to provide filtered information for a
 * specified resource class. A schematic that requires HQ resources obtains a
 * list of instances of this type (for LQ see a following paragraph). For each
 * schematic this type merges equal experiment lines into one instance, that is
 * if both resource class and weights are equal; this merge reduces the visible
 * display. As a result, a schematic wrapper contains as many objects of this
 * type as the amount of different experiment weights times the number of
 * different resource classes.
 * <p>
 * Instances of this type are created on demand and there is no need to maintain
 * cached or recycled instances. This type has no accessible constructor but a
 * factory that takes a schematic wrapper and returns a list of instances of
 * this type, see {@link #createInstances(SWGSchematicWrapper)}.
 * <p>
 * An instance contains a merged name of the experiment group, a resource class
 * constant, an experimental weight, and a list of all resources that meet the
 * requirements. If an instance is a merge its name is collated from the merged
 * experiment groups or lines. The list of resources is ordered by their weighed
 * rate and it contains both resources currently in spawn and resources from
 * inventory. This type provides several convenience methods to obtain subsets
 * of these resources.
 * <p>
 * <b>Low Quality</b>: a special use is LQ schematics which accept any resource
 * quality of the specified resource class. In this case a generic weight with
 * all stats set to lowest possible values is used and the resources are sorted
 * in the order 1) recycled resources (if possible), 2) lowest rate. The latter
 * makes sure that low values from a resource which resource class has low caps
 * will not slip through.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGExperimentWrapper implements SWGGui {

    /**
     * A short description of the resource class and weights for this instance.
     * The string is on the format "Duralloy Steel SR/UT 66/33" or
     * "Duralloy Steel".
     */
    private final String desc;

    /**
     * A list of the experiment lines that pertains to this instance.
     */
    private final List<SWGExperimentLine> expLines;

    /**
     * A flag that denotes if this wrapper is for a prioritized experimental
     * line. XXX this may be changed if/when a way to add graded prio is added.
     */
    private boolean isPrio;

    /**
     * The name for this instance. If this is a merge of several experiment
     * groups or lines the name is a merge.
     */
    private String name = "";

    /**
     * The resource class for this instance.
     */
    private final SWGResourceClass resClass;

    /**
     * A sorted list of resources that matches the resource class for this
     * instance. The order is determined by {@link #weights}.
     */
    private SWGResourceSet resources;

    /**
     * The weights for this instance.
     */
    private final SWGWeights weights;

    /**
     * Creates an instance of this type. This instance is later augmented on
     * demand.
     * 
     * @param weights the weights to use for this instance
     * @param resClass the resource class for this instance
     */
    private SWGExperimentWrapper(SWGWeights weights, SWGResourceClass resClass) {
        this.weights = weights;
        this.resClass = resClass;
        expLines = new ArrayList<SWGExperimentLine>();

        desc = getDescription(resClass, weights);
    }

    /**
     * Returns a list of the experimental lines that pertains to this instance.
     * The returned list is <b>read-only</b>.
     * 
     * @return a list of lines
     */
    List<SWGExperimentLine> expLines() {
        return expLines;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is a string on the format "Duralloy Steel SR/UT 66/33" or
     * "Duralloy Steel"
     */
    @Override
    public String getDescription() {
        return desc;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This is the name of the experiment group or line, or a merge named.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the resource class for this instance.
     * 
     * @return the resource class
     */
    SWGResourceClass rc() {
        return resClass;
    }

    /**
     * Helper method that refreshes the list of resources in this instance. The
     * resources are also sorted per the current weights.
     * 
     * @param spawn a set of spawning resources
     * @param inv a list of inventory resources
     */
    private synchronized void refresh(
            SWGResourceSet spawn, List<SWGInventoryWrapper> inv) {

        resources = SWGResController.resources(resClass, weights, spawn, inv);
    }

    /**
     * Returns a sorted list of resources that matches the resource class for
     * this instance. The order is determined by {@link #weights}. If this
     * wrapper is not yet initiated this method returns {@code null}. See
     * {@link #refresh(List, SWGResourceSet, List)}.
     * <p>
     * It is safe to call this method even if a concurrent thread is refreshing
     * the resources for a wrapper, the return value is the previous list, or
     * {@code null}. The returned list is <b>read-only</b>.
     * 
     * @return a set of resources sorted by weight, or {@code null}
     */
    SWGResourceSet resources() {
        return resources;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGExperimentWrapper[");
        return z.app(getDescription()).app(']').toString();
    }

    /**
     * Returns the weights used for this instance. The returned object is
     * <b>read-only</b>.
     * 
     * @return the weights
     */
    SWGWeights weights() {
        return weights;
    }

    /**
     * Creates and returns a list of uninitiated instances of this type. For the
     * specified schematic this implementation iterates over its list of
     * experimentation groups and over their lines. For each new combination of
     * resource-class and weight an instance is created, but for each existing
     * combination that instance is complemented. If the schematic has no
     * experimenting properties the elements are for the resource classes and
     * their weight is {@link SWGWeights#LQ_WEIGHTS}.
     * <p>
     * Later the elements are initiated by
     * {@link #refresh(List, SWGResourceSet, List)}.
     * <p>
     * The returned list is sorted in the order...<br/>
     * 1 - experimental importance<br/>
     * 2 - resource class sort order<br/>
     * 3 - order of stats in weight
     * <p>
     * The "importance" is defined in SWGAide and is target for updates without
     * further ado; currently a static list.
     * 
     * @param sw a schematics wrapper
     * @return instances of this type
     * @throws NullPointerException if there is a {@code null} error
     */
    static List<SWGExperimentWrapper> createInstances(SWGSchematicWrapper sw) {
        List<SWGExperimentWrapper> ret = new ArrayList<SWGExperimentWrapper>();

        SWGSchematic s = sw.schem();
        if (SWGSchematicsManager.isQuality(s)) {
            // if not LQ it is HQ per default, in this context
            for (SWGExperimentGroup eg : s.getExperimentGroups())
                handleExpGroup(eg, sw, ret);
        } else
            for (SWGResourceSlot rs : s.getResourceSlots())
                handleResSlot(rs, null, false, false, ret); // only res slots

        Collections.sort(ret, new Comparator<SWGExperimentWrapper>() {
            
            @Override
            public int compare(SWGExperimentWrapper o1, SWGExperimentWrapper o2) {
                if (o1.isPrio == o2.isPrio) {
                    int r = o1.resClass.compareTo(o2.resClass);
                    return r != 0
                            ? r
                            : o1.weights.compareTo(o2.weights);
                }
                return o1.isPrio
                        ? -1
                        : 1;
            }
        });
        // XXX: sort list, hopefully by importance, if there is a way to tell
        return ret;
    }

    /**
     * Helper method which from the specified list returns an instance of this
     * type for the specified resource class and weights, or {@code null} if
     * there is no matching instance.
     * 
     * @param rc a resource class
     * @param wg a weight object
     * @param ret a list of instances of this type
     * @return a matching instance, or {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    private static SWGExperimentWrapper get(
            SWGResourceClass rc, SWGWeights wg, List<SWGExperimentWrapper> ret) {
        
        for (SWGExperimentWrapper ew : ret)
            if (rc == ew.rc() && wg.equals(ew.weights()))
                return ew;

        return null;
    }

    /**
     * Helper method which assembles and returns a simple description of the
     * resource class and weights for an instance of this type. This is a string
     * on the format "Duralloy Steel SR/UT 66/33" or "Duralloy Steel".
     * 
     * @param rc the resource class for the instance
     * @param wgs the weights for the instance
     * @return a text description
     */
    private static String getDescription(SWGResourceClass rc, SWGWeights wgs) {
        ZString z = new ZString();
        z.app(rc.rcName()).app("    ");

        boolean statNames = true;
        if (wgs != SWGWeights.LQ_WEIGHTS)
            for (int i = 0; i < 2; ++i) {
                int sum = 0;
                for (Stat s : Stat.values()) {
                    int v = wgs.value(s);
                    if (v > 0) {
                        if (statNames)
                            z.app(s.name());
                        else
                            z.app(v);

                        sum += v;
                        if (sum < 98) z.app('/');
                    }
                }
                z.app(' ');
                statNames = false;
            }

        return z.toString();
    }

    /**
     * Helper method that iterates over the specified experiment group and calls
     * {@link #handleExpLine(SWGExperimentLine, SWGSchematicWrapper,boolean, List)}
     * 
     * @param eg an experiment group
     * @param sw a schematic wrapper
     * @param ret the list to populate
     * @throws NullPointerException if there is a {@code null} error
     * @throws IllegalArgumentException if there is an error
     */
    private static void handleExpGroup(SWGExperimentGroup eg,
            SWGSchematicWrapper sw, List<SWGExperimentWrapper> ret) {

        boolean prio = SWGSchemController.getPriority(
                eg, sw.schem()) >= 0;

        for (SWGExperimentLine el : eg.getExperimentalLines())
            handleExpLine(el, sw, prio, ret);
    }

    /**
     * Helper method which for the specified schematic's resource slots invokes
     * {@link #handleResSlot(SWGResourceSlot, SWGExperimentLine, boolean, boolean, List)}
     * For the HQ/LQ, either the user has specified this information which the
     * wrapper contains, or this information is obtained some other way.
     * 
     * @param el an experiment line
     * @param sw a schematic wrapper
     * @param prio {@code true} if this is about a prioritized experiment
     * @param ret the list to populate
     * @throws NullPointerException if there is a {@code null} error
     * @throws IllegalArgumentException if there is an error
     */
    private static void handleExpLine(SWGExperimentLine el,
            SWGSchematicWrapper sw, boolean prio, List<SWGExperimentWrapper> ret) {

        for (SWGResourceSlot rs : sw.schem().getResourceSlots())
            handleResSlot(rs, el, true, prio, ret);
    }

    /**
     * Helper method handles the specified resource slot for the schematic it
     * pertains to. If the specified list contains an instance of this type for
     * the specified resource class <i>and&nbsp;</i> experiment line the
     * instance is amended. Otherwise this method creates an instance and adds
     * it to the list.
     * <p>
     * If the boolean value is {@code false} this method manages LQ resources.
     * 
     * @param rs a resource class
     * @param el an experiment line, or {@code null}
     * @param hq {@code true} for a HQ experiment
     * @param prio {@code true} if this is about a prioritized experiment
     * @param ret the list to populate
     * @throws NullPointerException if the resource class or the list is {@code
     *         null}
     * @throws IllegalArgumentException if {@code (el == null && hq)}
     */
    private static void handleResSlot(SWGResourceSlot rs, SWGExperimentLine el,
            boolean hq, boolean prio, List<SWGExperimentWrapper> ret) {

        if (hq && el == null)
            throw new IllegalArgumentException("HQ but no experiment");

        SWGResourceClass rc = rs.getResourceClass();
        SWGWeights wg = hq
                ? el.getWeights()
                : SWGWeights.LQ_WEIGHTS;

        SWGExperimentWrapper ew = get(rc, wg, ret);
        if (ew == null) {
            ew = new SWGExperimentWrapper(wg, rc);
            ew.name = hq
                    ? el.getDescription() != null
                            ? replMinMax(el.getDescription())
                            : "UNKNOWN"
                    : "Any quality";
            ew.isPrio |= prio; // retain prio if achieved
            ret.add(ew);
        } else if (hq) {
            mergeNames(ew, el);
        }
        ew.expLines.add(el);
    }

    /**
     * Creates and sets a merged name for the specified instance based on the
     * current name and the specified experiment line. If the current name is
     * "UNKNOWN" it is replaced. If the specified experiment line, or its
     * description, is {@code null} this method does nothing.
     * 
     * @param ew the instance to augment
     * @param el the experiment line to append to the name from
     */
    private static void mergeNames(SWGExperimentWrapper ew, SWGExperimentLine el) {
        if (el == null || el.getDescription() == null) return;
        if (ew.name.equals("UNKNOWN"))
            ew.name = replMinMax(el.getDescription());
        else {
            String wName = ew.name;
            String lName = replMinMax(el.getDescription());
            if (wName.contains(lName)) return; // nothing more to do

            StringBuilder sb = new StringBuilder(wName);

            String repl = "XYZ";
            String wTemp = replTemporary(wName, repl);
            String lTemp = replTemporary(lName, repl);

            int widx = wTemp.indexOf(lTemp);
            if (widx >= 0) {
                // almost same, probable difference is Max versus Min

                if (wName.contains("Max/Min")) return; // nothing more to do

                widx = wTemp.indexOf(repl);
                int lidx = lTemp.indexOf(repl);
                if (wName.substring(widx, widx + 3).equals(
                        lName.substring(lidx, lidx + 3))) return;
                sb.replace(widx, widx + 3, "Max/Min");
            } else {
                sb.append(',').append(' ').append(lName);
            }
            ew.name = sb.toString();
        }
    }

    /**
     * Refreshes the experiment wrappers of the specified list. For each element
     * its list of matching resources is replaced and the resources are sorted
     * by the weights of the element. The matching resources are filtered from
     * the specified set of resources which are currently spawning and the list
     * of all inventory resources for the current galaxy.
     * 
     * @param expWrappers instances of this type
     * @param spawn a set of resource currently in spawn
     * @param inv a set of resources inventory resources
     * @throws NullPointerException if an argument is {@code null}
     */
    static void refresh(List<SWGExperimentWrapper> expWrappers,
            SWGResourceSet spawn, List<SWGInventoryWrapper> inv) {

        for (SWGExperimentWrapper ew : expWrappers)
            ew.refresh(spawn, inv);
    }

    /**
     * Helper method which in the specified string abbreviates "Maximum" to
     * "Max" and "Minimum" to "Min" and returns the modified string. If the
     * string contains neither string an identical string is returned.
     * 
     * @param str a string
     * @return the modified string
     * @throws NullPointerException if the argument is {@code null}
     */
    private static String replMinMax(String str) {
        String r = str;
        r = r.replaceAll("Maximum", "Max");
        r = r.replaceAll("Minimum", "Min");
        return r;
    }

    /**
     * Helper method which returns the specified string with all occurrences of
     * "Max" and "Min" with the replacement string. If the string contains
     * neither of the two an identical string is returned.
     * 
     * @param str a string
     * @param repl the replacement string
     * @return the modified string
     */
    private static String replTemporary(String str, String repl) {
        String s = str;
        s = s.replaceAll("Max", repl);
        s = s.replaceAll("Min", repl);
        return s;
    }
}
