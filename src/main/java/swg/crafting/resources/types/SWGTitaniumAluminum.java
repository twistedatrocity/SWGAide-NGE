package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Titanium Aluminum"
 *
 * <b>WARNING:</b>
 * This class is generated by SWGResourceClassGenerator.
 * Do not manually modify this class as your changes are
 * erased when the classes are re-generated.
 *
 * @author Steven M. Doyle <shadow@triwizard.net>
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a>
 * aka Chimaera.Zimoon
 */
@SuppressWarnings("all")
public final class SWGTitaniumAluminum extends SWGAluminum {

  private static final long serialVersionUID = 2340484L;

  private static final int[] minStats = {300, 1, 1, 0, 0, 200, 300, 1, 0, 300, 300};
  private static final int[] maxStats = {408, 174, 174, 0, 0, 330, 452, 1000, 0, 430, 430};

  private static final SWGTitaniumAluminum INSTANCE = new SWGTitaniumAluminum();

  SWGTitaniumAluminum() { super(); }

  public static SWGTitaniumAluminum getInstance() { return INSTANCE; }

  public int expectedStats() { return 8; }
  public int sortIndex() { return 674; }
  public int rcID() { return 295; }
  public String rcName() { return "Titanium Aluminum";}
  public String rcToken() { return "tialu";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}
