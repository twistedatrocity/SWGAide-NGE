package swg.tools;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.plaf.UIResource;

@SuppressWarnings("serial")
public class ImageIconUIResource extends ImageIcon implements UIResource {
	/**
     * Calls the superclass constructor with the same parameter.
     *
     * @param imageData an array of pixels
     * @see javax.swing.ImageIcon#ImageIcon(byte[])
     */
    public ImageIconUIResource(byte[] imageData) {
        super(imageData);
    }

    /**
     * Calls the superclass constructor with the same parameter.
     *
     * @param image an image
     * @see javax.swing.ImageIcon#ImageIcon(Image)
     */
    public ImageIconUIResource(Image image) {
        super(image);
    }
}
