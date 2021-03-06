package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Hardened Arveshium Steel"
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
public final class SWGHardenedArveshiumSteel extends SWGSteel {

  private static final long serialVersionUID = 5598168L;

  private static final int[] minStats = {1, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1};
  private static final int[] maxStats = {1000, 1000, 1000, 0, 0, 1000, 1000, 1000, 0, 1000, 1000};

  private static final SWGHardenedArveshiumSteel INSTANCE = new SWGHardenedArveshiumSteel();

  SWGHardenedArveshiumSteel() { super(); }

  public static SWGHardenedArveshiumSteel getInstance() { return INSTANCE; }

  public int expectedStats() { return 8; }
  public int sortIndex() { return 654; }
  public int rcID() { return 707; }
  public String rcName() { return "Hardened Arveshium Steel";}
  public String rcToken() { return "hastl";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public boolean  isJTL() { return true; }

  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}
