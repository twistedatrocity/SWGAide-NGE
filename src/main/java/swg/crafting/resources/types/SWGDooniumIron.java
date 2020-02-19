package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Doonium Iron"
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
public final class SWGDooniumIron extends SWGIron {

  private static final long serialVersionUID = 2166530L;

  private static final int[] minStats = {357, 570, 799, 0, 0, 856, 428, 1, 0, 828, 828};
  private static final int[] maxStats = {456, 730, 939, 0, 0, 956, 548, 1000, 0, 948, 948};

  private static final SWGDooniumIron INSTANCE = new SWGDooniumIron();

  SWGDooniumIron() { super(); }

  public static SWGDooniumIron getInstance() { return INSTANCE; }

  public int expectedStats() { return 8; }
  public int sortIndex() { return 666; }
  public int rcID() { return 273; }
  public String rcName() { return "Doonium Iron";}
  public String rcToken() { return "doirn";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}