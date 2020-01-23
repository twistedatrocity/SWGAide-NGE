package swg.model;

/**
 * This is a DUMMY class so OLD dat files don't implode. It's not actually used anywhere, as the
 * new SWGProfession class is NOT an enumerator.
 * This just needs to exist in swg.model so when an OLD dat file loads, the serialized object can find the class
 * and not throw an exception.
 */
public enum SWGProfession {
    /**
     * A constant which denotes <i>all&nbsp;</i> professions.
     */
    ALL("All", 0);

    /**
     * The ID used at swgaide.com for this profession.
     */
    private final int swgcraftID;

    /**
     * The proper name for this profession.
     */
    private final String name;

    /**
     * Creates a new profession constant with the specified name and ID.
     * 
     * @param professionName
     *            the proper name for the profession
     * @param swgCraftID
     *            the ID at swgaide.com for the profession
     */
    private SWGProfession(String professionName, int swgCraftID) {
        name = professionName;
        swgcraftID = swgCraftID;
    }

    /**
     * Determines if the specified constant equals this constant. In particular,
     * this implementation returns {@code true} if {@code this == other || other
     * == ALL || this == ALL}.
     * 
     * @param other
     *            the other profession constant to check
     * @return {@code true} if the argument is considered equal
     */
    public boolean equalsProfession(SWGProfession other) {
        return (other == this || other == ALL || this == ALL);
    }

}