package swg.model.images;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import swg.SWGAide;
import swg.SWGConstants;
import swg.gui.common.SWGGui;
import swg.tools.ZWriter;

/**
 * A wrapper type for images (screen shots).
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGImage implements Serializable, Comparable<SWGImage>, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    // XXX: retained for backward compatibility ... for some time from 2010 Oct.
    private static final long serialVersionUID = -2700609515114105711L;

    /**
     * The underlying image file at the current system.
     */
    private final File file;

    /**
     * Creates a an instance for the specified image, assuming it is an image.
     * 
     * @param file the image file at the file system
     * @throws IllegalArgumentException if the argument does not exist
     * @throws NullPointerException if the argument is {@code null}
     */
    SWGImage(File file) {
        if (!file.exists()) throw new IllegalArgumentException(
                "SWGImage: file does not exist: " + file.getAbsolutePath());
        this.file = file.getAbsoluteFile();
    }

    public int compareTo(SWGImage o) {
        return getName().compareTo(o.getName());
    }

    /**
     * Writes a copy of this image file to the specified target. If there is an
     * error it is caught and logged to SWGAide's log file and this method
     * returns {@code null}. This task is thread safe and executes at a worker
     * thread.
     * 
     * @param target the target file
     * @throws IllegalArgumentException if the target equals this' file
     */
    public void copyTo(final File target) {
        if (target.equals(file)) throw new IllegalArgumentException(
                "Illegal copy to self: " + target.getAbsolutePath());

        SwingWorker<Void, Void> wrk = new SwingWorker<Void, Void>() {
            @Override
            @SuppressWarnings( { "synthetic-access" })
            protected Void doInBackground() {
                synchronized (file) {
                    ZWriter.copy(file, target);
                    return null;
                }
            }
        };
        wrk.execute();
    }

    /**
     * Determines if the specified file equals the underlying file of this
     * instance. This method is defined by {@link File#equals(Object)}
     * 
     * @param f a file that is an absolute path
     * @return {@code false} if the argument is not equal
     */
    public boolean equals(File f) {
        return f.equals(file);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SWGImage && file.equals(((SWGImage) obj).file));
    }

    /**
     * Determines whether this image exists at the current file system. This
     * implementation returns {@code true} if a path for this image exists.
     * 
     * @return {@code true} if exists
     */
    public boolean exists() {
        return file.exists();
    }

    /**
     *{@inheritDoc}
     * <p>
     * This is the absolute path for this instance.
     */
    public String getDescription() {
        return file.getAbsolutePath();
    }

    public String getName() {
        return file.getName();
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    /**
     * Returns this instance as an AWT image. If there is an error it is caught
     * and written to SWGAide's log file and this method returns {@code null}.
     * <p>
     * <b>Notice:</b> only BMP and JPG image formats are supported.
     * 
     * @return the image, or {@code null}
     */
    public Image image() {
        return image(file);
    }

    /**
     * Returns this image scaled to the specified size. If any value is &le; 0
     * this method returns {@code null}. Otherwise the image is scaled so that
     * its width-height relation is retained but that width and height are
     * within the specified values. If there is an error it is caught and
     * written to SWGAide's error log file and this method returns {@code null}.
     * <p>
     * <b>Notice:</b> only BMP and JPG image formats are supported.
     * 
     * @param wMax max width
     * @param hMax max height
     * @return an image, or {@code null}
     */
    public Image imageScaled(int wMax, int hMax) {
        return imageScaled(image(file), wMax, hMax);
    }

    /**
     * Returns a thumb-nail for this object. If <tt>max</tt> &le; 0 a this
     * method returns {@code null}. Otherwise the image is scaled so that its
     * width-height relation is retained but that width and height are within
     * the specified value. As a side effect this method stores a thumb-nail in
     * the folder from {@link #imageCache()}. If there is an error it is caught
     * and written to SWGAide's error log file and this method returns {@code
     * null}.
     * 
     * @param max maximum width and height
     * @return a thumb-nail, or {@code null}
     */
    public ImageIcon imageThumb(int max) {
        File tf = new File(imageCache(), "thumb_" + file.getName());

        Image image = tf.exists()
                ? imageScaled(image(), max, max)
                : imageThumbWrite(image(), tf, max);

        return image != null
                ? new ImageIcon(image)
                : null;
    }

    /**
     * Returns this image's media type which is BMP or JPG.
     * <p>
     * Notice: SWGAide does not support the TGA image type.
     * 
     * @return the media type
     */
    public String imageType() {
        String n = file.getName();
        return n.substring(n.lastIndexOf(".") + 1);
    }

    /**
     * Returns the modification time for the underlying file for this instance.
     * This method is defined by {@link File#lastModified()}.
     * 
     * @return last modification time
     */
    public long lastModified() {
        return file.lastModified();
    }

    /**
     * Returns the file size for this instance. This method is defined by
     * {@link File#length()}.
     * 
     * @return the file size in bytes
     */
    public long size() {
        return file.length();
    }

    @Override
    public String toString() {
        return String.format("SWGImage[%s]", file.getAbsolutePath());
    }

    /**
     * Returns the specified file as an AWT image. If there is an error it is
     * caught and logged to SWGAide's error log file and this method returns
     * {@code null}.
     * <p>
     * <b>Notice</b>: SWGAide supports only the BMP and JPG image formats.
     * 
     * @param f an image file path
     * @return an image, or {@code null}
     */
    private static Image image(File f) {
        try {
            return ImageIO.read(f.getAbsoluteFile());
            // Toolkit does not support BMP
            // Toolkit.getDefaultToolkit().getImage(f.getAbsolutePath());
        } catch (Throwable e) {
            SWGAide.printError("SWGImage:image(f)", e);
        }
        return null;
    }

    /**
     * Returns a directory for thumb-nails at the local system. This is the
     * folder {@link SWGConstants#getCacheDirectory()}<tt>\images\</tt>.
     * 
     * @return a cache directory
     */
    private static File imageCache() {
        return new File(SWGConstants.getCacheDirectory(), "images");
    }

    /**
     * Helper method which returns a scaled image for the specified path, or
     * {@code null}. If any size is &le; 0 this method returns {@code null}.
     * Otherwise the image is scaled so that its width-height relation is
     * retained but that width and height are within the specified values. If
     * there is an error it is caught and written to SWGAide's error log file
     * and this method returns {@code null}.
     * 
     * @param image an image
     * @param wMax max width
     * @param hMax max height
     * @return a scaled image, or {@code null}
     */
    private static BufferedImage imageScaled(Image image, int wMax, int hMax) {
        if (image == null || wMax <= 0 || hMax <= 0) return null;
        BufferedImage bi = null;
        try {
            MediaTracker mt = new MediaTracker(new Container());
            mt.addImage(image, 0);
            mt.waitForID(0);

            int w = wMax;
            int h = hMax;
            int iw = image.getWidth(null);
            int ih = image.getHeight(null);
            double mRatio = (double) w / (double) h;
            double iRatio = (double) iw / (double) ih;
            if (mRatio < iRatio) {
                h = (int) (wMax / iRatio);
                w = wMax;
            } else {
                w = (int) (hMax * iRatio);
                h = hMax;
            }

            bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = bi.createGraphics();
            graphics2D.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(image, 0, 0, w, h, null);
        } catch (Throwable e) {
            SWGAide.printError("SWGImage:imageScaled", e);
        }
        return bi;
    }

    /**
     * Helper method which saves a thumb nail for the specified image. The file
     * is written to the system's general folder for temporary files as
     * specified by {@link SWGConstants#getCacheDirectory()}. This method also
     * returns the obtained scaled image.
     * 
     * @param img the source image
     * @param tgt the target path
     * @param max width
     * @return a scaled image, or {@code null}
     */
    private static Image imageThumbWrite(Image img, File tgt, int max) {
        BufferedImage bi = null;
        try {
            bi = imageScaled(img, max, max);
            if (bi != null) {
                if (!tgt.getParentFile().exists())
                    tgt.getParentFile().mkdirs();

                BufferedOutputStream out = new BufferedOutputStream(
                        new FileOutputStream(tgt));

                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bi);
                param.setQuality(.5f, false);
                encoder.setJPEGEncodeParam(param);
                encoder.encode(bi);

                out.flush();
                out.close();
            }
        } catch (Throwable e) {
            SWGAide.printError("SWGAlbum:imageThumbWrite", e);
        }
        return bi;
    }
}
