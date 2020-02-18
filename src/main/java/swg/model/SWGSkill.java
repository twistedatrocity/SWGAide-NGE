package swg.model;

import java.io.Serializable;

import swg.tools.ZString;

/**
 * This type reflects a specified amount of a modifier type in SWG, a profession
 * specific skill, such as "Food Experimentation" or "Droid Speed". A skill is a
 * talent to which a character is awarded more points as it levels up, and it is
 * also possible to augment the skill with buffs and wearable items. The
 * accumulated amount of the skill sets the upper limit for what is possible to
 * accomplish, or it determines how well a character may perform.
 * <p>
 * This type references {@link SWGModifier} using the composition pattern: this
 * type <i>is&nbsp;</i> a modifier by reference through its name. An instance of
 * this type reads the modifier name and the amount of the skill which the
 * instance reflects, perhaps the amount which is awarded at a profession level.
 * <p>
 * This type implements {@link Serializable} and clients may freely save a
 * persistent reference to an instance of this type.
 * <p>
 * Compare this type with {@link SWGAttribute} and {@link SWGAbility}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGSkill implements Comparable<SWGSkill> {

    /**
     * The amount of the skill that this instance reflects.
     */
    private final int amount;

    /**
     * The name of this skill.
     */
    private final String modifierName;

    /**
     * Creates an instance of this type based on the specified arguments. The
     * specified name is not validated to be a known modifier.
     * 
     * @param name
     *            the proper name of this skill (modifier)
     * @param amount
     *            the amount that the created instance reflects
     * @throws IllegalArgumentException
     *             if the amount is less than one
     * @throws NullPointerException
     *             if the name is {@code null}
     */
    public SWGSkill(String name, int amount) {
        if (name == null)
            throw new NullPointerException("Name is null");
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);

        modifierName = name;
        this.amount = amount;
    }

    @Override
    public int compareTo(SWGSkill o) {
        int ret = this.modifierName.compareTo(modifierName);
        return ret != 0
            ? (this.amount - o.amount)
            : ret;
    }

    /**
     * Returns the amount of the skill which this instance reflects.
     * 
     * @return the amount of this modifier
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Convenience for {@code SWGProfessionManager#getModifier(this.getName)}
     * which returns the modifier which constitutes the base for this instance.
     * 
     * @return the modifier for this instance
     */
    public SWGModifier getModifier() {
        return SWGProfessionManager.getModifier(modifierName);
    }

    /**
     * Returns the name of the skill which this instance reflects.
     * 
     * @return the name of this modifier
     */
    public String getName() {
        return modifierName;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGSkill").app('[').app(modifierName);
        return z.app(" (").app(amount).app(")").app(']').toString();
    }
}
