package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Barthierium Carbonate Ore"
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
public final class SWGBarthieriumCarbonateOre extends SWGCarbonateOre {

  private static final long serialVersionUID = 2087460L;

  private static final int[] minStats = {0, 361, 62, 0, 0, 453, 361, 1, 0, 62, 270};
  private static final int[] maxStats = {0, 501, 202, 0, 0, 573, 501, 1000, 0, 202, 430};

  private static final SWGBarthieriumCarbonateOre INSTANCE = new SWGBarthieriumCarbonateOre();

  SWGBarthieriumCarbonateOre() { super(); }

  public static SWGBarthieriumCarbonateOre getInstance() { return INSTANCE; }

  public int expectedStats() { return 7; }
  public int sortIndex() { return 727; }
  public int rcID() { return 263; }
  public String rcName() { return "Barthierium Carbonate Ore";}
  public String rcToken() { return "bacrb";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}