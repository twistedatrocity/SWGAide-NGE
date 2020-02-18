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
    ALL("All", 0),

    /**
     * A constant for Trader - Domestic.
     */
    DOMESTIC("Trader - Domestic", 9),

    /**
     * A constant for Trader - Engineer.
     */
    ENGINEER("Trader - Engineering", 11),

    /**
     * A constant for Trader - Munitions.
     */
    MUNITIONS("Trader - Munitions", 10),

    /**
     * A constant for Trader - Structures.
     */
    STRUCTURES("Trader - Structures", 8),

    /**
     * A constant for Bounty Hunter.
     */
    BH("Bounty Hunter", 2),

    /**
     * A constant for Commando.
     */
    COMMANDO("Commando", 3),

    /**
     * A constant for Entertainer.
     */
    ENTERTAINER("Entertainer", 4),

    /**
     * A constant for Jedi.
     */
    JEDI("Jedi", 5),

    /**
     * A constant for Medic.
     */
    MEDIC("Medic", 6),

    /**
     * A constant for Officer.
     */
    OFFICER("Officer", 7),

    /**
     * A constant for Smuggler.
     */
    SMUGGLER("Smuggler", 1),

    /**
     * A constant for Spy.
     */
    SPY("Spy", 12),
	
	/**
	* A constant for Unknown
	**/
	UNKNOWN("Unknown", 13);

    /**
     * The ID used at swgaide.com for this profession.
     */
    @SuppressWarnings("unused")
	private final int swgcraftID;

    /**
     * The proper name for this profession.
     */
    @SuppressWarnings("unused")
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