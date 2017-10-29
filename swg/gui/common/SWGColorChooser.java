package swg.gui.common;

import java.awt.Color;
import java.awt.Point;

import swg.gui.SWGFrame;
import swg.tools.ColorChooser;

/**
 * A dialog for choosing background colors in SWGAide. Usually this is used to
 * select a background color for resource table cells, and its text color. For
 * further details see {@link ColorChooser}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGColorChooser extends ColorChooser {

    /**
     * Creates an instance of this color chooser dialog for SWGAide.
     * 
     * @param frame the frame for SWGAide
     */
    public SWGColorChooser(SWGFrame frame) {
        super(frame);
        Point p = frame.getLocation();
        p.translate(100, 100);
        p = (Point) SWGFrame.getPrefsKeeper().get(
                "colorChooserDialogLocation", p);
        p = SWGGuiUtils.ensureOnScreen(p, this.getSize());
        this.setLocation(p);
    }

    @Override
    protected void closeDialog(Color selectedColor) {
        SWGFrame.getPrefsKeeper().add(
                "colorChooserDialogLocation", getLocation());
        super.closeDialog(selectedColor);
    }
}
