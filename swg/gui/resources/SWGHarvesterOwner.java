package swg.gui.resources;

import java.io.Serializable;

import swg.model.SWGCharacter;

/**
 * This type represents an owner of one or more harvesters, see
 * {@link SWGHarvester}. Currently a player defines a "owner", which is kind of
 * an assignee, with a name and the harvesting specific statistics. An instance
 * is final in the sense the owner-name is immutable, however, the statistics
 * for the owner are editable.
 * <P>
 * <I>It is planned for making all of the following the proper type definition,
 * however not there yet: Usually an instance of this type is "an interface" to
 * an in-game character from which harvesting statistics are queried. However,
 * the original design is still in place and it is possible for players to
 * define phony assignees. If {@link #character} is set all other fields are not
 * set but the getters query the character for the statistics.</I>
 * <P>
 * Serialization info: <I>For simplicity all fields are serialized, even those
 * that are not used. </I><BR/>
 * 
 * @author John Thomas aka Chilastra.Oarun
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGHarvesterOwner implements Serializable,
    Comparable<SWGHarvesterOwner> {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -1400654706952532102L;

    /**
     * The character who has the harvesting statistics conveyed by this type, or
     * {@code null} if this instance if a phony harvester owner.
     */
    private SWGCharacter character;

    /**
     * This represents how many points the owner has spent on the Harvester
     * Energy Efficiency expertise
     */
    private int energyEfficiencyLevel;

    /**
     * This represents the level of <I>Harvester Fair</I>, an Entertainer buff,
     * the owner has.
     */
    private int harvestFair;

    /**
     * This represents how many points the owner has spent on the Advanced
     * Harvesting Technology expertise
     */
    private int harvestingTechnologyLevel;

    /**
     * This represents how many points the owner has spent on the Harvester
     * Maintenance Efficiency expertise
     */
    private int maintenanceEfficiencyLevel;

    /**
     * Name of the character associated with this owner object
     */
    private final String name;

    /**
     * This represents how many points the owner has spent on the Harvester
     * Storage Efficiency expertise
     */
    private int storageEfficiencyLevel;

    /**
     * Creates a phony harvester owner with the given name, expertise modifiers
     * and harvest fair buff. All values are validated by their setters
     * respectively.
     * <P>
     * <B>Note: </B>(<U>Not yet implemented</U>--This is a player created
     * assignee, a phony, which represents an in-game harvester owner rather
     * than an instance of {@link SWGCharacter}.)
     * 
     * @param phony
     *            a description/name for a phony owner, later immutable; a valid
     *            string is not {@code null} and longer than 2 characters
     * @param storage
     *            the number of expertise points spent in Harvester Storage
     *            Efficiency, [0 4]
     * @param maint
     *            the number of expertise points spent in Harvester Maintenance
     *            Efficiency, [0 4]
     * @param energy
     *            the number of points expertise spent in Harvester Energy
     *            Efficiency, [0 4]
     * @param tech
     *            the number of points expertise spent in Advanced Harvesting
     *            Technology, [0 2]
     * @param fair
     *            the level of the Entertainer buff Harvest Fair, [0 5]
     * @throws IllegalArgumentException
     *             if there is an argument is invalid
     * @throws NullPointerException
     *             if {@code phony} is {@code null}
     */
    SWGHarvesterOwner(String phony, int storage, int maint, int energy,
        int tech, int fair) {
        if (phony.trim().length() < 3)
            throw new IllegalArgumentException("Illegal value: " + phony);

        this.name = phony;
        setStorageEfficiency(storage);
        setMaintEfficiency(maint);
        setEnergyEfficiency(energy);
        setHarvestingTechnology(tech);
        setHarvestFair(fair);
    }

    /**
     * Creates a wrapper of this type for the given character. The created
     * instance will query the statistics from the specified character. Thus, if
     * the expertise is changed it is instantly transparent via this instance.
     * 
     * @param character
     *            the in-game character
     * @throws NullPointerException
     *             if {@code character} is {@code null}
     */
    @SuppressWarnings("unused")
    private SWGHarvesterOwner(SWGCharacter character) {
        // XXX fix when characters are amended to
        if (character == null)
            throw new NullPointerException("Argument is null");
        name = null;
        this.character = character;
    }

    public int compareTo(SWGHarvesterOwner o) {
        return name.compareToIgnoreCase(o.name);
    }

    /**
     * Returns the character wrapped by this instance. If this instance rather
     * is a player created phony harvester owner {@code null} is returned.
     * 
     * @return the character
     */
    @SuppressWarnings("unused")
    private SWGCharacter getCharacter() {
        return character;
    }

    /**
     * Returns the points the owner has spent on the <I>Harvester Energy
     * Efficiency&nbsp;</I> expertise, zero to four.
     * 
     * @return the energyEfficiencyLevel
     */
    int getEnergyEfficiency() {
        if (character != null)
            return energyEfficiencyLevel; // XXX
        return energyEfficiencyLevel;
    }

    /**
     * Returns the level of <I>Harvest Fair&nbsp;</I> for this owner, zero to
     * five.
     * 
     * @return the harvestFair
     */
    int getHarvestFair() {
        return harvestFair;
    }

    /**
     * Returns the points the owner has spent on the <I>Advanced Harvesting
     * Technology&nbsp;</I> expertise, zero to two.
     * 
     * @return the harvestingTechnologyLevel
     */
    int getHarvestingTechnology() {
        if (character != null)
            return harvestingTechnologyLevel; // XXX
        return harvestingTechnologyLevel;
    }

    /**
     * Returns the points the owner has spent on the <I>Harvester Maintenance
     * Efficiency&nbsp;</I> expertise, zero to four.
     * 
     * @return the maintenanceEfficiencyLevel
     */
    int getMaintEfficiency() {
        if (character != null)
            return maintenanceEfficiencyLevel; // XXX
        return maintenanceEfficiencyLevel;
    }

    /**
     * Returns the name of the character, or the description of the phony owner
     * if no character is set.
     * 
     * @return the name
     */
    String getName() {
        if (character != null)
            character.getName();
        return name;
    }

    /**
     * Returns the points the owner has spent on the <I>Harvester Storage
     * Efficiency&nbsp;</I> expertise, zero to four.
     * 
     * @return the storageEfficiencyLevel
     */
    int getStorageEfficiency() {
        if (character != null)
            return storageEfficiencyLevel; // XXX
        return storageEfficiencyLevel;
    }

    /**
     * Sets the number of expertise points spent in <I>Harvester Energy
     * Efficiency</I>, zero to four points.
     * 
     * @param energyEfficiencyLevel
     *            the level to set, [0 4]
     * @throws IllegalArgumentException
     *             if the argument is invalid
     * @throws IllegalStateException
     *             if this method is invoked and the character is set and this
     *             method
     * @throws IllegalStateException
     *             if this method is invoked and the character is set and this
     *             method
     */
    void setEnergyEfficiency(int energyEfficiencyLevel) {
        if (character != null)
            throw new IllegalStateException("Unsupported when character is set");
        if (energyEfficiencyLevel < 0 || energyEfficiencyLevel > 4)
            throw new IllegalArgumentException("Illegal value: "
                + energyEfficiencyLevel);
        this.energyEfficiencyLevel = energyEfficiencyLevel;
    }

    /**
     * Sets the level of <I>Harvester Fair</I>, an Entertainer buff, zero to
     * five packages. This method is supported also when {@link #character} is
     * set
     * 
     * @param harvestFair
     *            the level to set, [0 5]
     * @throws IllegalArgumentException
     *             if the argument is invalid
     */
    void setHarvestFair(int harvestFair) {
        if (harvestFair < 0 || harvestFair > 5)
            throw new IllegalArgumentException("Illegal value: " + harvestFair);
        this.harvestFair = harvestFair;
    }

    /**
     * Sets the number of expertise points spent in <I>Advanced Harvesting
     * Technology</I>, zero to two points.
     * 
     * @param harvestingTechnologyLevel
     *            the level to set, [0 2]
     * @throws IllegalArgumentException
     *             if the argument is invalid
     * @throws IllegalStateException
     *             if this method is invoked and the character is set and this
     *             method
     */
    void setHarvestingTechnology(int harvestingTechnologyLevel) {
        if (character != null)
            throw new IllegalStateException("Unsupported when character is set");
        if (harvestingTechnologyLevel < 0 || harvestingTechnologyLevel > 2)
            throw new IllegalArgumentException("Illegal value: "
                + harvestingTechnologyLevel);
        this.harvestingTechnologyLevel = harvestingTechnologyLevel;
    }

    /**
     * Sets the number of expertise points spent in <I>Harvester Maintenance
     * Efficiency</I>, zero to four points.
     * 
     * @param maintenanceEfficiencyLevel
     *            the level to set, [0 4]
     * @throws IllegalArgumentException
     *             if the argument is invalid
     * @throws IllegalStateException
     *             if this method is invoked and the character is set and this
     *             method
     */
    void setMaintEfficiency(int maintenanceEfficiencyLevel) {
        if (character != null)
            throw new IllegalStateException("Unsupported when character is set");
        if (maintenanceEfficiencyLevel < 0 || maintenanceEfficiencyLevel > 4)
            throw new IllegalArgumentException("Illegal value: "
                + maintenanceEfficiencyLevel);
        this.maintenanceEfficiencyLevel = maintenanceEfficiencyLevel;
    }

    /**
     * Sets the number of expertise points spent in <I>Harvester Storage
     * Efficiency</I>, zero to four points.
     * 
     * @param storageEfficiencyLevel
     *            the level to set, [0 4]
     * @throws IllegalArgumentException
     *             if the argument is invalid
     * @throws IllegalStateException
     *             if this method is invoked and the character is set and this
     *             method
     */
    void setStorageEfficiency(int storageEfficiencyLevel) {
        if (character != null)
            throw new IllegalStateException("Unsupported when character is set");
        if (storageEfficiencyLevel < 0 || storageEfficiencyLevel > 4)
            throw new IllegalArgumentException("Illegal value: "
                + storageEfficiencyLevel);

        this.storageEfficiencyLevel = storageEfficiencyLevel;
    }

    @Override
    public String toString() {
        return name;
    }
}
