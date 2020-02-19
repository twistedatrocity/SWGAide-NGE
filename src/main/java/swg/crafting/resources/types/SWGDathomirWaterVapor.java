package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Dathomir Water Vapor"
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
public final class SWGDathomirWaterVapor extends SWGWater {

  private static final long serialVersionUID = 221408L;

  private static final int[] minStats = {0, 0, 1, 0, 0, 0, 0, 600, 0, 0, 0};
  private static final int[] maxStats = {0, 0, 1000, 0, 0, 0, 0, 1000, 0, 0, 0};

  private static final SWGDathomirWaterVapor INSTANCE = new SWGDathomirWaterVapor();

  SWGDathomirWaterVapor() { super(); }

  public static SWGDathomirWaterVapor getInstance() { return INSTANCE; }

  public int expectedStats() { return 2; }
  public int sortIndex() { return 603; }
  public int rcID() { return 27; }
  public String rcName() { return "Dathomir Water Vapor";}
  public String rcToken() { return "dawtr";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}