package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Crism Siliclastic Ore"
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
public final class SWGCrismSiliclasticOre extends SWGSiliclasticOre {

  private static final long serialVersionUID = 2111181L;

  private static final int[] minStats = {0, 498, 199, 0, 0, 498, 498, 1, 0, 171, 199};
  private static final int[] maxStats = {0, 685, 385, 0, 0, 685, 685, 1000, 0, 330, 385};

  private static final SWGCrismSiliclasticOre INSTANCE = new SWGCrismSiliclasticOre();

  SWGCrismSiliclasticOre() { super(); }

  public static SWGCrismSiliclasticOre getInstance() { return INSTANCE; }

  public int expectedStats() { return 7; }
  public int sortIndex() { return 738; }
  public int rcID() { return 266; }
  public String rcName() { return "Crism Siliclastic Ore";}
  public String rcToken() { return "crsil";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}
