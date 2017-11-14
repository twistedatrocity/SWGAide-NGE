package swg.gui.resources;

import java.io.Serializable;

import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.types.SWGChemical;
import swg.crafting.resources.types.SWGFloraResources;
import swg.crafting.resources.types.SWGGas;
import swg.crafting.resources.types.SWGGeothermalEnergy;
import swg.crafting.resources.types.SWGMineral;
import swg.crafting.resources.types.SWGRadioactive;
import swg.crafting.resources.types.SWGSolarEnergy;
import swg.crafting.resources.types.SWGWater;
import swg.crafting.resources.types.SWGWindEnergy;
import swg.tools.ZString;

/**
 * This type denotes a harvester in SWG. An instance of this type contains data
 * specific for a harvester and modifiers based on the owner's expertise and
 * entertainer buff that was applied the date a harvester is put down in the
 * world, as well as applied self-powered deed. A defined harvester is immutable
 * as such; ephemeral data fields are mutable.
 * <P>
 * For simplicity this type provides package-wide visibility to a few of its
 * fields, most have getter/setter-methods.
 * <P>
 * This type implements {@link Serializable} and all of its member fields are
 * serialized as is. This type is not thread safe but most fields are just read.
 * 
 * @author John Thomas aka Chilastra.Oarun
 * @author <a href="mailto:simongronlun@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGHarvester implements Serializable, Comparable<SWGHarvester> {

    /*
     * Comments: In October 2009 this type was refactored, mainly as a
     * side-effect of the deep reaching changes to resources. Now this type
     * properly supports that buffs and expertise stick as long as the
     * harvester stays put, see activate(boolean, boolean). Many fields are now
     * private and safety checks are added to setter methods. Also, this
     * type was made half-final mimicking in-game harvesters. However, the
     * structure and the overall logic is still the work of Oarun. / Zimoon
     */

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 8738683861412424036L;

    /**
     * An array of all harvester types in SWG.
     */
    final static String[] TYPES =
            // careful with these names as they are used to determine some states
        {"Elite Chemical", "Elite Flora", "Elite Gas", "Elite Mineral",
            "Elite Water", "Radioactive Energy", "Heavy Chemical",
            "Heavy Flora", "Heavy Gas", "Heavy Mineral", "Heavy Water",
            "Medium Chemical", "Medium Flora", "Medium Gas", "Medium Mineral",
            "Medium Water", "Geothermal Energy", "Solar Energy",
            "Personal Chemical", "Personal Flora", "Personal Gas",
            "Personal Water", "Personal Mineral", "Wind Energy"};

    /**
     * Determines whether or not to automatically add the current hopper total
     * to an inventory entry.
     * 
     * @serial boolean: addToInventory
     */
    boolean addToInventory;

    /**
     * The fixed base extraction rate for this harvester, in units per minute.
     * 
     * @serial int: BER
     */
    final int ber;
    
    /**
     * The extraction modifier
     * 
     * @serial double: bmod
     */
    double bmod = 1.0;

    /**
     * The concentration level of the resource being harvested. This is the
     * integer value set by the user which is read at the survey device, in the
     * range [0 100], commonly named percent.
     * 
     * @serial int: local concentration
     */
    private int concentration;

    /**
     * The number of expertise points in <I>Harvester Energy
     * Efficiency&nbsp;</I> that applies to this harvester. The value applies
     * from the date this harvester is put down in the world until it is pulled
     * up. This value must never be reset as long as this harvester stays put.
     * <P>
     * The values is in the range [0 4].
     * 
     * @serial int: energyEfficiencyLevel
     */
    private int energyEfficiencyLevel;

    /**
     * The level of the Entertainer buff <I>Harvester Fair&nbsp;</I> that
     * applies to this harvester. The value applies from the date this harvester
     * is put down in the world until it is pulled up. This value must never be
     * reset as long as this harvester stays put.
     * <P>
     * The values is in the range [0 5].
     * 
     * @serial int: harvestFair
     */
    private int harvestFair;

    /**
     * The number of expertise points in <I>Advanced Harvesting
     * Technology&nbsp;</I> that applies to this harvester. The value applies
     * from the date this harvester is put down in the world until it is pulled
     * up. This value must never be reset as long as this harvester stays put.
     * <P>
     * The values is in the range [0 2].
     * 
     * @serial int: harvestingTechnologyLevel
     */
    private int harvestingTechnologyLevel;

    /**
     * The fixed hopper capacity size for this harvester, in units.
     * 
     * @serial int: hopper size
     */
    final int hopperSize;

    /**
     * Signifies whether this harvester is active or not.
     * 
     * @serial boolean: isActive
     */
    private boolean isActive;

    /**
     * The time when the harvester was last updated by the user, in
     * milliseconds.
     * 
     * @serial long: last updated
     */
    private long lastUpdated;

    /**
     * The amount of maintenance in the harvester, in credits.
     * 
     * @serial long: maintenance
     */
    // it would suffice with an int if we could assume that no user is silly
    // enough to feed the harvester with more than 2 million credits ;)
    // this field is also often used together with time in milliseconds so ...
    private long maint;

    /**
     * The number of points in <I>Harvester Maintenance Efficiency&nbsp;</I>
     * that applies to this harvester. The value applies from the date this
     * harvester is put down in the world until it is pulled up. This value must
     * never be reset as long as this harvester stays put.
     * <P>
     * The values is in the range [0 4].
     * 
     * @serial int: maintenanceEfficiencyLevel
     */
    private int maintenanceEfficiencyLevel;

    /**
     * The fixed base maintenance consumption rate for this harvester, in
     * credits per hour. The value is determined by the harvester type:
     * <DL>
     * <DT>Elite</DT>
     * <DD>126 cr per hour</DD>
     * <DT>Heavy</DT>
     * <DD>46 cr per hour</DD>
     * <DT>Medium</DT>
     * <DD>30 cr per hour</DD>
     * <DT>Personal</DT>
     * <DD>16 cr per hour</DD>
     * <DT>Energy</DT>
     * <DD>30 cr per hour</DD>
     * </DL>
     * 
     * @serial int: maint-rate
     */
    final int maintRate;

    /**
     * A user definable name for this harvester.
     * 
     * @serial string: name
     */
    private String name;

    /**
     * Miscellaneous notes associated with this harvester.
     * 
     * @serial string: notes
     */
    private String notes;

    /**
     * The current owner of this harvester.
     * <P>
     * <B>Note: </B>When a harvester is deactivated and the argument {@code
     * reDeed} is {@code true} the owner is set to {@code null}. Otherwise, if
     * the argument is {@code false} that denotes that the harvester stays put,
     * which means that expertise bonuses, buff, and self-power attribute stick
     * as they were the date the harvester was initially put down. Thus, if the
     * harvester is inactive and this value is {@code null} it denotes that the
     * harvester was re-deeded, but if this value is a owner the harvester stayed
     * put. See {@link #setOwner(SWGHarvesterOwner)} for further details.
     * 
     * @serial harvester owner
     */
    private SWGHarvesterOwner owner;

    /**
     * The current level of power in the harvester, in units. If the harvester
     * is self-powered this value can be 0, but it is no requirement. If this is
     * a power generator this value is always 0.
     * 
     * @serial int: power level
     */
    // it would suffice with an int if we could assume that no user is silly
    // enough to feed the harvester with more than 2 million units ;)
    // this field is also often used together with time in milliseconds so ...
    private long power;

    /**
     * The base power consumption rate for this harvester, in credits per hour.
     * The value is determined by the harvester type:
     * <DL>
     * <DT>Elite</DT>
     * <DD>206 cr per hour</DD>
     * <DT>Heavy</DT>
     * <DD>75 cr per hour</DD>
     * <DT>Medium</DT>
     * <DD>50 cr per hour</DD>
     * <DT>Personal</DT>
     * <DD>25 cr per hour</DD>
     * <DT>Energy</DT>
     * <DD>self-powered</DD>
     * </DL>
     * 
     * @serial int: power use
     */
    final int powerRate;

    /**
     * The resource the harvester is currently gathering.
     * 
     * @serial current resource
     */
    private SWGKnownResource resource;

    /**
     * Signifies whether the harvester is self-powered or not.
     * 
     * @serial boolean: self-powered
     */
    private boolean selfPowered;

    /**
     * Denotes how many harvesters this instance represents in the world. One
     * harvester in SWGAide can represent several harvesters put down at a
     * location and this harvester's yield is then multiplied by this value when
     * the inventory is updated. The default value is 1.
     * 
     * @serial int cluster count
     */
    private int several = 1;

    /**
     * This represents how many points of <I>Harvester Storage
     * Efficiency&nbsp;</I> that applies to this harvester. The value applies
     * from the date this harvester is put down in the world until it is pulled
     * up. This value must never be reset as long as this harvester stays put.
     * <P>
     * The values is in the range [0 4].
     * 
     * @serial int: storageEfficiencyLevel
     */
    private int storageEfficiencyLevel;

    /**
     * The type of harvester this object is, for example: Elite Mineral,
     * Personal Chemical, etc.
     * 
     * @see #TYPES
     * @serial string: harvester type
     */
    final String type;

    /**
     * Creates a harvester for the given harvester type. The instance is final
     * and only ephemeral values are mutable.
     * 
     * @param name
     *            the name for this harvester, mutable
     * @param type
     *            the type of harvester, see {@link #TYPES}, immutable
     * @param BER
     *            the BER of the harvester, immutable
     * @param hopper
     *            hopper capacity of the harvester, immutable
     * @param BMOD
     *            the extraction modifier for the server, immutable
     * @throws IllegalArgumentException
     *             if an argument is invalid
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    SWGHarvester(String name, String type, int BER, int hopper, double BMOD) {
        if (name == null || type == null)
            throw new NullPointerException(String.format("name=%s and type%s",
                name, type));
        if (BER <= 0 || hopper <= 0)
            throw new IllegalArgumentException(String.format(
                "BER=%d and hopper=%d", Integer.valueOf(BER), Integer
                .valueOf(hopper)));

        if (BMOD > 0.0) {
        	this.bmod = BMOD;
        } else {
        	this.bmod = 1.0;
        }
        this.name = name;
        this.type = type;
        this.ber = BER;
        this.hopperSize = hopper;

        // fill out the harvester type specific properties
        if (type.contains("Elite")) {
            maintRate = 126;
            powerRate = 206;
        } else if (type.contains("Heavy")) {
            maintRate = 46;
            powerRate = 75;
        } else if (type.contains("Medium")) {
            maintRate = 30;
            powerRate = 50;
        } else if (type.contains("Personal")) {
            maintRate = 16;
            powerRate = 25;
        } else if (type.contains("Energy")) {
            // some kind of energy harvester
            // if we are here that means the harvester is an energy
            maintRate = 30;
            powerRate = 0;
            selfPowered = true;
        } else
            throw new IllegalArgumentException("Invalid type: " + type);
    }

    /**
     * Invoked to activate or deactivate this harvester. If this harvester is
     * already active this method does nothing. If the invocation deactivates
     * this harvester this method also invokes {@link #activateHelper(boolean)}
     * and the argument {@code reDeed} determines which attributes are reset and
     * which attributes are retained; in particular, if the argument is {@code
     * false} the current owner sticks and denotes that this harvester stayed
     * put.
     * <P>
     * To fully reset an inactive harvester this method is invoked with the
     * arguments {@code false} and {@code true} respectively.
     * 
     * @param activate
     *            {@code true} to activate this harvester, {@code false} to
     *            deactivate this harvester
     * @param reDeed
     *            {@code true} if this harvester is re-deeded, only if the
     *            harvester is being deactivated this argument is considered
     */
    void activate(boolean activate, boolean reDeed) {
        if (activate && isActive)
            return;
        else if (activate)
            lastUpdated = System.currentTimeMillis();
        else
            activateHelper(reDeed);

        isActive = activate;
    }

    /**
     * Resets member fields to default values. If the argument is {@code true}
     * all values are reset as if this harvester is re-deeded, which includes
     * the owner. Otherwise, if the argument is {@code false} as if this
     * harvester stays put, the owner, expertise bonuses, buff, and self-powered
     * attribute are retained, but attributes related to a particular resource
     * which was harvested are reset.
     * 
     * @param reDeed
     *            {@code true} if this harvester is re-deeded, {@code false} if
     *            it stays put
     */
    private void activateHelper(boolean reDeed) {
        notes = null;
        resource = null;
        concentration = 0;
        lastUpdated = 0;
        several = 1;
        if (reDeed) {
            owner = null;
            setSelfPowered(false);
            storageEfficiencyLevel = 0;
            maintenanceEfficiencyLevel = 0;
            energyEfficiencyLevel = 0;
            harvestingTechnologyLevel = 0;
            harvestFair = 0;
        }
    }

    public int compareTo(SWGHarvester o) {
        return name.compareTo(o.name);
    }

    /**
     * Returns the actual extraction rate for this active harvester, in units
     * per minute. This is the adjusted rate based on {@link #ber} with
     * concentration, expertise bonuses and buffs, applied. The returned value
     * is for example 78.9, always &ge; 0.0 and not truncated.
     * 
     * @return the AER
     */
    double getAER() {
        // BER * concentration/100 * modifier
        // (concentration is stored in the range [0 100] >> divide by 100)
        return ber * concentration / 100d * getBerModifier();
    }

    /**
     * Returns the modifier for the base extraction rate determined by applied
     * expertise bonuses and entertainer buff. The 50% bonus from December 2005
     * is included, thus the returned value is always &ge; 1.5.
     * 
     * @return the BER modifier, &ge; 1.5
     */
    double getBerModifier() {
        // XXX 50% (the bonus from 2005/12 when harvesters are operated per owner)
        double techMod = 1.0; // base
        if (harvestingTechnologyLevel == 1)
            techMod = 1.2;
        else if (harvestingTechnologyLevel == 2)
            techMod = 1.3;
        return bmod * (techMod + (harvestFair * .01));
    }

    /**
     * Returns the concentration of the resource which is currently gathered,
     * percent in the range [0 100].
     * 
     * @return the current concentration
     */
    int getConcentration() {
        return concentration;
    }

    /**
     * Returns the current level of the expertise <I>Harvester Energy
     * Efficiency&nbsp;</I> applied to this harvester, in the range [0 4].
     * 
     * @return the level of <I>Harvester Energy Efficiency</I>
     */
    int getEnergyEfficiencyLevel() {
        return energyEfficiencyLevel;
    }

    /**
     * Returns the modifier for the power consumption rate, based on
     * {@link #energyEfficiencyLevel}.
     * 
     * @return the power consumption modifier, <= 1.0
     */
    private double getEnergyModifier() {
        return 1.0 - (energyEfficiencyLevel * .05);
    }

    /**
     * Returns the level of the Entertainer buff <I>Harvester Fair&nbsp;</I>
     * that the owner had who put down this harvester in the world. The level
     * applies until the harvester is pulled up, re-deeded.
     * 
     * @return the level of harvest fair
     */
    int getHarvestFair() {
        return harvestFair;
    }

    /**
     * Returns the current level of the expertise <I>Advanced Harvesting
     * Technology&nbsp;</I> applied to this harvester, in the range [0 2].
     * 
     * @return the level of <I>Advanced Harvesting Technology</I>
     */
    int getHarvestingTechnologyLevel() {
        return harvestingTechnologyLevel;
    }

    /**
     * Returns the modified hopper capacity, in units. This is the current
     * capacity with expertise bonus applied.
     * 
     * @return the modified hopper capacity
     */
    int getHopperCapacity() {
        return (int) (hopperSize * getStorageModifier());
    }

    /**
     * Returns the date, in milliseconds, for when the hopper is full.
     * 
     * @return the date the hopper is full
     */
    long getHopperFull() {
        // need to find out how many millisecond it will take to fill up the
        // hopper. The extraction rate is how many units we can harvest per
        // minute then hopper-capacity/AER * ms/minute + last-update
        return lastUpdated + ((long) (getHopperCapacity() / getAER() * 60000L));
    }

    /**
     * Returns the amount of currently gathered resources, in units. This is the
     * number of units since the last update, expertise bonus, buff, and 50%
     * included.
     * 
     * @return the current amount at the hopper
     */
    int getHopperUnits() {
        // 1/60/1000 (~ .0000166) is the magic number that represents how many
        // units of a resource you can harvest per millisecond
        // if you are operating at an extraction rate of 1; this is "t"
        final double dt = 1.0 / 60000.0;
        long t = System.currentTimeMillis() - lastUpdated;
        return (int) (t * dt * getAER());
    }

    /**
     * Returns the date, in milliseconds, when the harvester was last updated by
     * the user.
     * 
     * @return the date for the last update
     * @see #refreshLastUpdated()
     */
    long getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Returns the amount of maintenance in this harvester, in credits. This is
     * the amount that was set by the user, not the current amount as if it is
     * withdrawn from every hour.
     * 
     * @return the amount of maintenance
     */
    long getMaint() {
        return maint;
    }

    /**
     * Returns the current level of the expertise <I>Harvester Maintenance
     * Efficiency&nbsp;</I> applied to this harvester, in the range [0 4].
     * 
     * @return the level of <I>Harvester Maintenance Efficiency</I>
     */
    int getMaintenanceEfficiencyLevel() {
        return maintenanceEfficiencyLevel;
    }

    /**
     * Returns the date, in milliseconds, for when the current maintenance runs
     * out.
     * 
     * @return the date the maintenance is drained
     */
    long getMaintEnds() {
        // compute the number of milliseconds until the maint runs out
        // maint / (maintRate * mod) >>> hours * ms-per-hour >>>
        return lastUpdated
            + (long) (3600000 * maint / (maintRate * getMaintModifier()));
    }

    /**
     * Returns the modifier for the maintenance consumption rate based on
     * {@link #maintenanceEfficiencyLevel}.
     * 
     * @return the maintenance consumption modifier, <= 1.0
     */
    private double getMaintModifier() {
        return 1.0 - (maintenanceEfficiencyLevel * .04);
    }

    /**
     * Returns the remaining maintenance as percent. The return value starts at
     * 1.0 and decreases as time goes. If maintenance has run out the value is
     * increasingly negative.
     * 
     * @return the remaining maintenance &le; 1.0f
     */
    float getMaintRemains() {
        return remains(getMaintEnds());
    }
    
    /**
     * Returns the user defined name for this harvester.
     * 
     * @return the name
     */
    String getName() {
        return name;
    }

    /**
     * Returns the notes set by the user for this harvester, or the empty string
     * if no notes are defined.
     * 
     * @return the notes, never {@code null}
     */
    String getNotes() {
        return notes == null
            ? ""
            : notes;
    }

    /**
     * Returns the current owner of this harvester, or {@code null}.
     * <P>
     * <B>Note: </B>The returned owner is always the current owner and is not
     * necessarily the one who initially put down this harvester and at that
     * date set the bonuses...
     * <P>
     * When a harvester is deactivated <I>and&nbsp;</I> re-deeded its owner is
     * set to {@code null}. Otherwise, if a harvester stays put the expertise
     * bonuses, buff, and self-powered attribute must stick as they were from
     * the date the harvester was initially put down. Thus, if a harvester <U>is
     * inactive</U> a {@code null} return value denotes that the harvester was
     * re-deeded, but if the return value is an owner the harvester stayed put.
     * If the harvester is active the return value is always a valid owner, but
     * not necessarily the one who once put down the harvester.
     * 
     * @return the current owner of this harvester
     * @see #activate(boolean, boolean)
     * @see #setOwner(SWGHarvesterOwner)
     */
    SWGHarvesterOwner getOwner() {
        return owner;
    }

    /**
     * Returns the current level of power in the harvester, in units.This is the
     * amount that was set by the user, not the current amount as if it is
     * withdrawn from every hour.
     * <P>
     * If the harvester is self-powered this value can be 0, but that is not a
     * requirement. If this is a power generator this value is always 0.
     * 
     * @return the amount of power
     */
    long getPower() {
        return power;
    }

    /**
     * Returns the date, in milliseconds, for when the current power runs out.
     * If this harvester is self-powered this method returns
     * {@link Long#MAX_VALUE}.
     * 
     * @return the date the power is drained
     */
    long getPowerEnds() {
        // include power-rate just for div-by-zero sanity
        if (selfPowered || powerRate <= 0)
            return Long.MAX_VALUE;

        // compute the number of milliseconds until the power runs out
        // power / (powerRate * mod) >>> hours * ms-per-hour >>>
        return lastUpdated
            + ((long) (3600000 * power / (powerRate * getEnergyModifier())));
    }

    /**
     * Returns the remaining maintenance as percent. The return value starts at
     * 1.0 and decreases as time goes. If maintenance has run out the value is
     * increasingly negative.
     * 
     * @return the remaining maintenance &le; 1.0f
     */
    float getPowerRemains() {
        return remains(getPowerEnds());
    }

    /**
     * Returns the resource this harvester is gathering.
     * 
     * @return the currently gathered resource
     */
    SWGKnownResource getResource() {
        return resource;
    }

    /**
     * Returns the number of harvesters this instance represents. This instance
     * can represent a cluster of harvesters put down at a location. This
     * harvester's yield is then multiplied by this return value when the
     * inventory is updated; this is done by the client. The returned value is
     * always &ge; 1.
     * 
     * @return the number of harvester this instance represents, &ge; 1
     */
    int getSeveral() {
        return several;
    }

    /**
     * Returns the current level of the expertise <I>Harvester Storage
     * Efficiency&nbsp;</I> applied to this harvester, in the range [0 4].
     * 
     * @return the level of <I>Harvester Storage Efficiency</I>
     */
    int getStorageEfficiencyLevel() {
        return storageEfficiencyLevel;
    }

    /**
     * Returns the modifier for the maintenance consumption rate based on
     * {@link #maintenanceEfficiencyLevel}.
     * 
     * @return the maintenance consumption modifier, >= 1.0
     */
    private double getStorageModifier() {
        return 1.0 + (storageEfficiencyLevel * .03);
    }

    /**
     * Returns {@code true} if this harvester is active.
     * 
     * @return {@code true} if this harvester is active
     */
    boolean isActive() {
        return isActive;
    }

    /**
     * Determines if this harvester is self-powered. All power-generators are
     * inherently self-powered, others can be if a deed <I>Make a Harvester Self
     * Powered&nbsp;</I> is temporarily applied. Such a deed sticks until the
     * harvester is re-deeded.
     * 
     * @return {@code true} if this harvester is self-powered
     */
    boolean isSelfPowered() {
        return selfPowered;
    }

    /**
     * Refreshes this harvester. This method updates the time for the last
     * refresh and sets it to the current time. This denotes the time for when
     * the user most recently pandered to the in-game harvester which this
     * instance denotes.
     */
    void refreshLastUpdated() {
        lastUpdated = System.currentTimeMillis();
    }

    /**
     * Helper method which returns a percent value based on the argument. The
     * return value starts at 1.0 and decreases as time goes. If now is past the
     * specified time the returned value is increasingly negative.
     * 
     * @param end the end time in milliseconds
     * @return a percent value
     */
    private float remains(long end) {
        long denom = end - lastUpdated;
        return denom == 0
                ? 0.0f
                : ((float) end - System.currentTimeMillis()) / denom;
    }

    /**
     * Sets the concentration for the currently harvested resource. This is the
     * integer value set by the user which is read at the survey device, in the
     * range [0 100]. Note, this is not the AER which is read at the harvester's
     * <I>Operate&nbsp;</I> screen, but we must trust the user.
     * 
     * @param concentration
     *            the base concentration for the resource currently gathered
     * @throws IllegalArgumentException
     *             if the argument is outside the range [0 100]
     */
    void setConcentration(int concentration) {
        if (concentration < 0 || concentration > 100)
            throw new IllegalArgumentException(
                "Invalid concentration: " + concentration);
        this.concentration = concentration;
    }

    /**
     * Sets a currency amount of maintenance for this harvester. If the
     * specified amount is &le; 0 this method does nothing.
     * 
     * @param maintenance
     *            the amount to set
     */
    void setMaint(long maintenance) {
        if (maintenance > 0)
            maint = maintenance;
    }
    
    /**
     * Set modifier for harvester
     * 
     * @param modifier
     * 		the value to set
     */
    void setBmod(double modifier) {
    	if(modifier > 0)
    		bmod = modifier;
    }

    /**
     * Sets a user defined name for this harvester.
     * 
     * @param name
     *            the name to set
     * @throws IllegalArgumentException
     *             if the argument is the empty string or {@code null}
     */
    void setName(String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Invalid argument: " + name);
        this.name = name;
    }

    /**
     * Sets user provided, miscellaneous notes for this harvester. A {@code
     * null} argument clears any previous notes.
     * 
     * @param notes
     *            the notes to set
     */
    void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Sets the current owner of this harvester and <I>possibly&nbsp;</I>
     * updates the modifier fields.
     * <P>
     * <B>Note: </B>If this harvester is inactive and {@link #owner} is {@code
     * null} the expertise bonuses and entertainer buff are set, read from the
     * specified owner. If this harvester is inactive and {@link #owner} is not
     * {@code null} it denotes that the harvester stayed put in the world and
     * bonuses are retained. If this harvester is active only ownership is
     * updated, ownership is always transferred to the specified owner.
     * <P>
     * Remember that bonuses apply on a harvester from the date it is put down
     * until it is pulled up (re-deeded). This is true also if ownership is
     * transferred to another character using the in-game command {@code
     * /structureTransfer}. However, in-game, if the new owner replenish power
     * and/or maintenance the bonuses for these attributes are reset and their
     * values are determined from the current owner; SWGAide does not model
     * this.
     * <P>
     * Thus, when {@link #activate(boolean, boolean)} is invoked to deactivate a
     * harvester the argument {@code reDeed} determines if the harvester is
     * fully reset or if the bonuses stick.
     * 
     * @param owner
     *            the owner to set
     * @throws NullPointerException
     *             if the owner is {@code null}
     */
    void setOwner(SWGHarvesterOwner owner) {
        if (owner == null)
            throw new NullPointerException("Argument is null");

        if (!isActive && this.owner == null) {
            harvestFair = owner.getHarvestFair();
            storageEfficiencyLevel = owner.getStorageEfficiency();
            maintenanceEfficiencyLevel = owner.getMaintEfficiency();
            energyEfficiencyLevel = owner.getEnergyEfficiency();
            harvestingTechnologyLevel = owner.getHarvestingTechnology();
        }
        this.owner = owner;
    }

    /**
     * Sets the amount of power for the harvester, in units. If the argument is
     * &lt; 0 or if this harvester is a power generator this method does
     * nothing.
     * 
     * @param units
     *            the amount of power to set
     */
    void setPower(long units) {
        if (units >= 0 && !type.contains("Energy"))
            power = units;
    }

    /**
     * Sets the resource to harvest. The resource must be spawnable in the
     * worlds but no further validation is performed.
     * 
     * @param resource
     *            the resource to harvest
     * @throws IllegalArgumentException
     *             if the resource is not spawnable or if it is {@code null}
     */
    void setResource(SWGKnownResource resource) {
        if (resource == null || !resource.rc().isSpawnable())
            throw new IllegalArgumentException("Invalid argument: " + resource);
        this.resource = resource;
    }

    /**
     * Sets this harvester in self-powered mode. If this harvester is a power
     * generator this method does nothing.
     * 
     * @param selfPowered
     *            {@code true} to set this harvester in self-powered mode
     */
    void setSelfPowered(boolean selfPowered) {
        if (!type.contains("Energy"))
            this.selfPowered = selfPowered;
    }

    /**
     * Sets the number of harvesters this instance represents. This instance can
     * represent a cluster of harvesters put down at a location. This
     * harvester's yield is then multiplied by the specified value when the
     * inventory is updated; this is done by the client. This implementation
     * does not validate for any upper limit.
     * 
     * @param several
     *            the number of harvesters this instance should represent
     * @throws IllegalArgumentException
     *             if the specified value is &lt; 1
     */
    void setSeveral(int several) {
        if (several < 1)
            throw new IllegalArgumentException("Invalid argument: " + several);
        this.several = several;
    }

    @Override
    public String toString() {
        ZString z = new ZString(getClass().getSimpleName()).app('[');
        z.app(name).app(", ").app(owner).app(", ");
        if (getResource() != null)
            z.app(resource.getName()).app(':').app(resource.rc().rcName());

        return z.app(']').toString();
    }

    /**
     * Returns the generic resource class which the specified harvester can
     * gather. The returned resource class is one of Chemical, Flora, Gas,
     * Geothermal, Mineral, Radioactive, Solar, Water, or Wind.
     * 
     * @param harv
     *            the harvester to return a resource class for
     * @return the generic resource class the harvester can gather
     */
    static Class<? extends SWGResourceClass> getType(SWGHarvester harv) {
        String harvType = harv.type;
        if (harvType.contains("Chemical"))
            return SWGChemical.class;
        if (harvType.contains("Flora"))
            return SWGFloraResources.class;
        if (harvType.contains("Gas"))
            return SWGGas.class;
        if (harvType.contains("Geothermal"))
            return SWGGeothermalEnergy.class;
        if (harvType.contains("Mineral"))
            return SWGMineral.class;
        if (harvType.contains("Radioactive"))
            return SWGRadioactive.class;
        if (harvType.contains("Solar"))
            return SWGSolarEnergy.class;
        if (harvType.contains("Water"))
            return SWGWater.class;
        if (harvType.contains("Wind"))
            return SWGWindEnergy.class;
        throw new IllegalStateException("Illegal type: " + harvType);
    }
}
