package swg.crafting.resources.types;

import swg.crafting.Stat;

/*
 * Represents a resource class of type "Nabooian Conifer Wood"
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
public final class SWGNabooianConiferWood extends SWGSoftWood {

  private static final long serialVersionUID = 4625607L;

  private static final int[] minStats = {0, 0, 1, 0, 0, 0, 600, 1, 0, 100, 1};
  private static final int[] maxStats = {0, 0, 600, 0, 0, 0, 1000, 1000, 0, 400, 300};

  private static final SWGNabooianConiferWood INSTANCE = new SWGNabooianConiferWood();

  SWGNabooianConiferWood() { super(); }

  public static SWGNabooianConiferWood getInstance() { return INSTANCE; }

  public int expectedStats() { return 5; }
  public int sortIndex() { return 548; }
  public int rcID() { return 584; }
  public String rcName() { return "Nabooian Conifer Wood";}
  public String rcToken() { return "nacwd";}
  public boolean isSpawnable() { return true; }
  public boolean has(Stat s) { return minStats[s.i] > 0; }
  public int max(Stat s) { return maxStats[s.i]; }
  public int min(Stat s) { return minStats[s.i]; }

  private Object readResolve() {
    return INSTANCE; // preserve singleton property
  }
}
