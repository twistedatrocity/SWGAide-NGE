package swg.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * This class uniquely identifies species in SWG, such as Human or Wookiee;
 * currently only species that can be chosen for a character are identified by
 * this class. No logic nor managing is provided by this class, it is just a
 * container of data.
 * 
 * @author Simon Gronlund <a href="mailto:simongronlund@gmail.com">Simon
 *         Gronlund</a> aka Europe-Chimaera.Zimoon
 */
public class SWGSpecies implements Comparable<SWGSpecies>, Serializable {

    /**
     * Map of species, mapped by their name to species constants
     */
    // this collection must be created first, thus the underscore
    private static Map<String, SWGSpecies> _speciesMap //
    = new Hashtable<String, SWGSpecies>(17);

    /*
     * This class must be enhanced from its current state to contain the
     * specific attributes and characteristics for species in SWG
     */

    /**
     * Constant for the Bothan species
     */
    public static final SWGSpecies BOTHAN = new SWGSpecies("Bothan");

    /**
     * Constant for the Human species
     */
    public static final SWGSpecies HUMAN = new SWGSpecies("Human");

    /**
     * Constant for the Ithorian species
     */
    public static final SWGSpecies ITHORIAN = new SWGSpecies("Ithorian");

    /**
     * Constant for the Mon Calamari species
     */
    public static final SWGSpecies MON_CALAMARI = new SWGSpecies("Mon Calamari");

    /**
     * Constant for the Rodian species
     */
    public static final SWGSpecies RODIAN = new SWGSpecies("Rodian");

    /**
     * Serialization version info; do not meddle with this or break the
     * deserialization
     */
    private static final long serialVersionUID = -8660891776226948541L;

    /**
     * Constant for the Sullustan species
     */
    public static final SWGSpecies SULLUSTAN = new SWGSpecies("Sullustan");

    /**
     * Constant for the Trandoshan species
     */
    public static final SWGSpecies TRANDOSHAN = new SWGSpecies("Trandoshan");

    /**
     * Constant for the Twi'lek species
     */
    public static final SWGSpecies TWILEK = new SWGSpecies("Twi'lek");

    /**
     * Constant for the Wookiee species
     */
    public static final SWGSpecies WOOKIEE = new SWGSpecies("Wookiee");

    /**
     * Constant for the Zabrak species
     */
    public static final SWGSpecies ZABRAK = new SWGSpecies("Zabrak");

    /**
     * The name of the species represented by this object
     */
    private final String speciesName;

    /**
     * Creates an instance of this class with the given name
     * 
     * @param name
     *            the name for a species
     */
    private SWGSpecies(String name) {
        speciesName = name;
        _speciesMap.put(name, this);
    }

    public int compareTo(SWGSpecies o) {
        return speciesName.compareTo(o.speciesName);
    }

    /**
     * Returns the name for the species represented by this object
     * 
     * @return the name for the species represented by this object
     */
    public String getName() {
        return speciesName;
    }

    /**
     * Returns an unordered list of the constants for species
     * 
     * @return an unordered list of the constants for species
     */
    public synchronized ArrayList<SWGSpecies> getSpecies() {
        return new ArrayList<SWGSpecies>(_speciesMap.values());
    }

    /**
     * Returns a species constant mapped by <code>name</code>, <code>null</code>
     * if no object is found
     * 
     * @param name
     *            the name of the species to return
     * @return a species constant mapped by <code>name</code>, or
     *         <code>null</code>
     */
    public synchronized SWGSpecies getSpecies(String name) {
        return _speciesMap.get(name);
    }

    /**
     * Returns an unordered list of names of the species
     * 
     * @return an unordered list of names of the species
     */
    public synchronized List<String> getSpeciesNames() {
        return new ArrayList<String>(_speciesMap.keySet());
    }

    @Override
    public String toString() {
        return speciesName;
    }
}
