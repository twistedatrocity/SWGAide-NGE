package swg.gui.schematics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import swg.SWGAide;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.common.SWGGui;
import swg.model.SWGProfession;
import swg.tools.ZString;

/**
 * This type contains miscellaneous data related to schematics for an assignee.
 * An assignee is identified by a unique text that the player defines; if the
 * text is not unique the result is undefined but one of the several instances
 * will overwrite the others in the serialized map that is stored in SWGAide's
 * data file.
 * <p>
 * This type is thread-safe and locks on smallest possible scope. This type
 * implements the serializable interface and data types are either serialized or
 * referred to by their suggested means, respectively.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGSchematicAssignee implements SWGGui, Serializable,
        Comparable<SWGSchematicAssignee> {

    /**
     * A constant for a default assignee. This assignee will return the widest
     * and most relaxed response when its getters are invoked, this assignee
     * also pretends best possible expertise.
     */
    public transient static final SWGSchematicAssignee DEFAULT =
            new SWGSchematicAssignee("All");

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 101674101523172608L;

    /**
     * The assignee for this instance. This is a unique text that the player
     * defines.
     * 
     * @serial a string for the assignee
     */
    private String assignee;

    /**
     * A list of schematic IDs for the schematics which the player selects as
     * favorites for this assignee. This list is initialized and never {@code
     * null}.
     * 
     * @serial a list of integers for schematic IDs
     */
    private List<Integer> favIDs;

    /**
     * A temporary list of schematics that the player selects as favorites for
     * this assignee.
     */
    private transient List<SWGSchematic> favorites;

    /**
     * A profession constant that the user has selected for this instance, or
     * {@link SWGProfession#ALL}.
     * 
     * @serial the selected profession
     */
    private SWGProfession profession;

    /**
     * The number of expertise points the user has set for this assignee. This
     * value is multiplied by ten and added to the weighed average result after
     * it is adjusted for caps. Valid values are [0 4].
     * 
     * @serial integer for resource refinery
     */
    private int resourceRefinery;

    /**
     * Creates an instance of this type for the specified assignee. This
     * constructor does not verify the uniqueness.
     * 
     * @param assignee a unique identifying text
     * @throws IllegalArgumentException if the argument is empty or {@code null}
     */
    SWGSchematicAssignee(String assignee) {
        this.setAssignee(assignee);
        this.favIDs = new ArrayList<Integer>();
        this.profession = SWGProfession.ALL;
    }

    /**
     * Adds a favorite schematic for this assignee. This method does nothing if
     * this instance already contains the argument.
     * 
     * @param favorite a schematic
     * @throws NullPointerException if the argument is {@code null}
     */
    void addFavorite(SWGSchematic favorite) {
        if (this == DEFAULT) return;
        synchronized (favIDs) {
            if (favIDs.contains(Integer.valueOf(favorite.getID()))) //
                return; // no doubles

            if (favorites == null)
                getFavorites(); // ensure "favorites" is initiated

            // no adding until after possible initialization
            favIDs.add(Integer.valueOf(favorite.getID()));
            favorites.add(favorite);
            Collections.sort(favorites);
        }
    }

    @Override
    public int compareTo(SWGSchematicAssignee o) {
        if (this == DEFAULT) return o == DEFAULT
                ? 0
                : -1;
        return this.assignee.compareTo(o.assignee);
    }

    @Override
    public String getDescription() {
        return "";
    }

    /**
     * Returns a sorted copy of the list of favorite schematics that the user
     * has selected for this assignee, or an empty list. The list is sorted in
     * alphabetical order.
     * 
     * @return a sorted list of favorite schematics, or an empty list
     */
    List<SWGSchematic> getFavorites() {
        synchronized (favIDs) {
            if (favorites == null) {
                if (this == DEFAULT) {
                    favorites = SWGSchematicsManager.getSchematics();
                } else {
                    favorites = new ArrayList<SWGSchematic>(favIDs.size() + 16);
                    Iterator<Integer> iter;
                    SWGSchematic s;
                    for (iter = favIDs.iterator(); iter.hasNext();) {
                        Integer sid = iter.next();
                        s = SWGSchematicsManager.getSchematic(sid.intValue());
                        if (s != null)
                            favorites.add(s);
                        else { // schematic removed from XML
                            SWGAide.printDebug("sass", 1,
                                    "Removed schematic from assignee: ",
                                    this.getName(), ", ", sid.toString());
                            iter.remove(); 
                        }
                    }
                }
                Collections.sort(favorites);
            }

            return new ArrayList<SWGSchematic>(favorites);
        }
    }

    @Override
    public String getName() {
        return assignee;
    }

    /**
     * Returns the profession for this instance. If the user has not selected a
     * profession {@link SWGProfession#ALL} is returned.
     * 
     * @return a profession
     */
    SWGProfession getProfession() {
        return profession;
    }

    /**
     * Returns the number of expertise points that is set for this assignee, in
     * the range [0 4]. The returned value is multiplied by ten and added to the
     * weighed average result after it is adjusted for caps.
     * 
     * @return the number of points in resource refinery
     */
    int getResourceRefinery() {
        if (this == DEFAULT) return 4;
        return resourceRefinery;
    }

    /**
     * Removes the specified favorite schematic from this assignee. This method
     * does nothing if this instance does not contain the argument.
     * 
     * @param remove a schematic
     * @throws NullPointerException if the argument is {@code null}
     */
    void removeFavorite(SWGSchematic remove) {
        if (this == DEFAULT) return;
        synchronized (favIDs) {
            if (favIDs.remove(Integer.valueOf(remove.getID()))
                    && favorites != null /* safety belt */)
                favorites.remove(remove);
        }
    }

    /**
     * Sets a new assignee text for this instance, this method does not verify
     * the uniqueness.
     * 
     * @param assignee the assignee to set
     * @throws IllegalArgumentException if the argument is empty or {@code null}
     */
    void setAssignee(String assignee) {
        if (this == DEFAULT) return;
        if (assignee == null || assignee.trim().isEmpty())
            throw new IllegalArgumentException(String.format(
                    "Invalid argument: \"%s\"", assignee));
        this.assignee = assignee.trim();
    }

    /**
     * Sets the profession for this instance. If the argument is {@code null}
     * this method sets the profession to {@link SWGProfession#ALL}.
     * 
     * @param profession a profession, or {@code null}
     */
    void setProfession(SWGProfession profession) {
        if (this == DEFAULT) return;
        this.profession = profession == null
                ? SWGProfession.ALL
                : profession;
    }

    /**
     * Sets the number of expertise points for this assignee.
     * 
     * @param points the number of points in resource refinery
     * @throws IllegalArgumentException if the value is not in range [0 4]
     */
    void setResourceRefinery(int points) {
        if (this == DEFAULT) return;
        if (points < 0 || points > 4)
            throw new IllegalArgumentException("Invalid value [0 4]: " + points);
        this.resourceRefinery = points;
    }

    @Override
    public String toString() {
        return new ZString("Assignee[").app(assignee).app(']').toString();
    }
}
