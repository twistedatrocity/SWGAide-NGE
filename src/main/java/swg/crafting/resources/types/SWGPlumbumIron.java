package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Plumbum Iron"
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
public final class SWGPlumbumIron extends SWGIron {

  private static final long serialVersionUID = 2182344L;

  private static final int[] minStats = {1, 1, 300, 0, 0, 500, 1, 1, 0, 400, 400};
  private static final int[] maxStats = {82, 131, 414, 0, 0, 581, 98, 1000, 0, 498, 498};

  private static final SWGPlumbumIron INSTANCE = new SWGPlumbumIron();

  SWGPlumbumIron() { super(); }

  public static SWGPlumbumIron getInstance() { return INSTANCE; }

  public int expectedStats() { return 8; }
  public int sortIndex() { return 660; }
  public int rcID() { return 275; }
  public String rcName() { return "Plumbum Iron";}
  public String rcToken() { return "plirn";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}
