package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Mustafarian Reactive Gas"
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
public final class SWGMustafarianReactiveGas extends SWGKnownReactiveGas {

  private static final long serialVersionUID = 4933980L;

  private static final int[] minStats = {0, 0, 800, 0, 0, 0, 0, 500, 0, 0, 0};
  private static final int[] maxStats = {0, 0, 1000, 0, 0, 0, 0, 1000, 0, 0, 0};

  private static final SWGMustafarianReactiveGas INSTANCE = new SWGMustafarianReactiveGas();

  SWGMustafarianReactiveGas() { super(); }

  public static SWGMustafarianReactiveGas getInstance() { return INSTANCE; }

  public int expectedStats() { return 2; }
  public int sortIndex() { return 782; }
  public int rcID() { return 623; }
  public String rcName() { return "Mustafarian Reactive Gas";}
  public String rcToken() { return "muktv";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}
