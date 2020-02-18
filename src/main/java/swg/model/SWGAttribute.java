package swg.model;

import java.io.Serializable;

import swg.tools.ZString;

/**
 * This type reflects a specified amount of a modifier type that in SWG is known
 * a basic attribute. Attributes are either amended to as the character levels
 * up, or they are augmented by buffs or wearable items. The accumulated amount
 * of the attribute determines how well a character may perform.
 * <p>
 * <b>Basic attributes:</b> Agility, Constitution, Luck, Precision, Stamina, and
 * Strength. All profession have many or all of these basic attributes.
 * <p>
 * This type references {@link SWGModifier} using the composition pattern: this
 * type <i>is&nbsp;</i> a modifier by reference through its name. An instance of
 * this type reads the modifier name and the amount of the attribute which the
 * instance reflects.
 * <p>
 * This type implements {@link Serializable} and clients may freely save a
 * persistent reference to an instance of this type.
 * <p>
 * Compare this type with {@link SWGSkill} and {@link SWGAbility}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGAttribute implements Comparable<SWGAttribute> {

    /**
     * The only modifiers which are considered basic attributes.
     */
    public static final String[] ATTRIBUTES = {"Agility",
        "Constitution", "Luck", "Precision", "Stamina", "Strength"};

    /**
     * The amount of the attribute that this instance reflects.
     */
    private final int amount;

    /**
     * The name of this attribute.
     */
    private final String modifierName;

    /**
     * Creates an instance of this type based on the specified arguments. The
     * specified name must be a basic attribute.
     * 
     * @param name
     *            the proper name of this attribute (modifier)
     * @param amount
     *            the amount that the created instance reflects
     * @throws IllegalArgumentException
     *             if the name is not a basic attribute or if amount is &le; 1
     * @throws NullPointerException
     *             if the name is {@code null}
     */
    public SWGAttribute(String name, int amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        validateName(name);

        modifierName = name;
        this.amount = amount;
    }

    @Override
    public int compareTo(SWGAttribute o) {
        int ret = this.modifierName.compareTo(modifierName);
        return ret != 0
            ? (this.amount - o.amount)
            : ret;
    }

    /**
     * Returns the amount of the attribute which this instance reflects.
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
     * Returns the name of the attribute which this instance reflects.
     * 
     * @return the name of this modifier
     */
    public String getName() {
        return modifierName;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SWGAttribute").app('[').app(modifierName);
        return z.app(" (").app(amount).app(")").app(']').toString();
    }

    /**
     * Validates the specified attribute name. If the name is not one of
     * Agility, Constitution, Luck, Precision, Stamina, and Strength this method
     * throws an exception.
     * 
     * @param n
     *            the attribute name to validate
     * @throws IllegalArgumentException
     *             if the name is not a basic attribute
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    private static void validateName(String n) {
        for (String s : ATTRIBUTES)
            if (n.equals(s))
                return;
        throw new IllegalArgumentException("Not basic attribute: " + n);
    }
}
