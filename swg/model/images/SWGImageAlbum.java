package swg.model.images;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import swg.gui.common.SWGGui;
import swg.model.SWGUniverse;

/**
 * This type models an image and screen-shot album. It contains images but
 * populates itself just on demand. Images are temporary and wasted at exit.
 * <p>
 * History: <br/>
 * With Game Update 4 a folder named "screenshots" was added to the SWG client
 * folder and a bug in SWG was resolved &mdash; SWG overwrote existing screen
 * shots in some situations; hence SWGAide no longer moves images into safety.
 * <p>
 * Originally images were stored in SWGAide's DAT file but this was changed in
 * October 2010 so that now images are temporary and they are wasted at exit;
 * also, the former support for notes and comments were removed. This was done
 * as part of making SWGAide mobile. For some time and for back-and-forward
 * compatibility this type, {@link SWGImageSubAlbum}, and {@link SWGImage}
 * implement {@link Serializable}, however, its read and write methods are
 * overwritten and just care for the least possible data.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGImageAlbum implements Serializable, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -4301338986286072423L;

    /**
     * The default list of images.
     */
    private final SWGImageSubAlbum defaultList;

    /**
     * A list of sub-albums, sub-lists with images. This list is retained for
     * backward//forward compatibility. TODO: remove sometimes 2011.
     */
    private final ArrayList<SWGImageSubAlbum> subAlbums;

    /**
     * The universe in SWG to which this album pertains.
     */
    private final SWGUniverse universe;

    /**
     * Creates an instance of this type.
     * 
     * @param univ the universe for this album
     */
    @SuppressWarnings("synthetic-access")
    public SWGImageAlbum(SWGUniverse univ) {
        universe = univ;

        subAlbums = new ArrayList<SWGImageSubAlbum>();
        defaultList = new SWGImageSubAlbum(this, univ.getName());
        subAlbums.add(defaultList);
    }

    /**
     * Helper method that returns a path for this album within the folder for
     * SWGAide; this is "album" or "album\testcenter".
     * 
     * @return an album path
     */
    private File albumPath() {
        File f = new File("album");
        return getName().equals("TC")
                ? new File(f, "testcenter")
                : f;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof SWGImageAlbum && getName().equals(
                ((SWGImageAlbum) o).getName()));
    }

    public String getDescription() {
        return "Image album: " + getName();
    }

    public String getName() {
        return universe.getName();
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Returns the content of this album, a list of images.
     * 
     * @return the album content
     */
    public SWGImageSubAlbum images() {
        return defaultList;
    }

    /**
     * Helper method which returns a list of new image objects for the specified
     * file path; this album does not contain any image in the returned list and
     * each element in the list is unique. If no new image exists this method
     * returns an empty list.
     * 
     * @param f a path to images
     * @return a list of new images
     * @throws NullPointerException if the argument is {@code null}
     */
    private List<SWGImage> newImages(File f) {
        File[] lst = imageFiles(f);
        if (lst == null)
            return Collections.emptyList();

        List<SWGImage> ret = new ArrayList<SWGImage>(lst.length);
        for (File i : lst)
            if (!defaultList.contains(i))
                ret.add(new SWGImage(i));

        return ret;
    }

    /**
     * This method transforms this instance to conform to the new album setup,
     * that it populates itself on demand. Hence this method moves all images to
     * {@link #defaultList}, if an image does not exist it is ignored. Except
     * for the default list any previous sub-albums are removed.
     * <p>
     * Hence this method executes just the first time a user launches SWGAide
     * after that these changes are released, in the future an album instance
     * from the DAT file is empty and this method does nothing.
     * 
     * @return {@code this}
     */
    private Object readResolve() {
        if (subAlbums.size() > 1) {
            // XXX: remove sometimes 2011

            // sanity, clean the default list
            for (Iterator<SWGImage> iter = defaultList.iterator(); iter.hasNext();)
                if (!iter.next().exists())
                    iter.remove();

            // move all others that exist to default list
            for (SWGImageSubAlbum isa : subAlbums)
                if (isa != defaultList)
                    for (SWGImage i : isa)
                        if (i.exists() && !defaultList.contains(i))
                            defaultList.add(i);

            // remove all but the default list
            subAlbums.clear();
            subAlbums.add(defaultList);
        }
        return this;
    }

    /**
     * Refresh the album. This implementation scans this screen-shot album for
     * the current universe (the SWG client) and the album folder in SWGAide.
     * For each new image an instance of {@link SWGImage} is created and added
     * to this album; all instances for images are wasted when SWGAide exits;
     * see class comment.
     * <p>
     * This method is invoked each time the user selects the "Album" tab at
     * SWGAide's main frame, both to initiate the album and to refresh it.
     */
    public synchronized void refresh() {
        refreshHelper(new File(universe.swgPath(), "screenshots"));
        refreshHelper(albumPath());
    }

    /**
     * Helper method which scans this album for new images in the specified
     * directory. For each new image an instance is created and added to
     * {@link #defaultList}. After that this method scans the comments file that
     * pertains to the specified directory and adds comments that pertain to new
     * images. If the specified path does not exist this method does nothing.
     * 
     * @param dir a directory path
     */
    private void refreshHelper(File dir) {
        if (dir.exists()) {
            for (SWGImage i : newImages(dir))
                defaultList.add(i);

            Collections.sort(defaultList);
        }
    }

    @Override
    public String toString() {
        return "SWGAlbum[" + getName() + ']';
    }

    /**
     * Serialize this album instance to the specified stream. This method
     * empties {@link #defaultList} before this instance is serialized, then it
     * invokes {@link ObjectOutputStream#defaultWriteObject()}. Thus the
     * deserialized instance is empty.
     * 
     * @param oos an object output stream
     * @throws IOException if there is an I/O error
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        defaultList.clear();
        oos.defaultWriteObject();
    }

    /**
     * Helper method which returns an array of files from the specified path
     * which suffixes are "bmp", "jpg", or "tga". The return value is defined by
     * {@link File#listFiles(FileFilter)}.
     * 
     * @param dir a directory path
     * @return an array of files, an empty array, or {@code null}
     */
    private static File[] imageFiles(File dir) {
        File[] lst = dir.listFiles(new FileFilter() {
            public boolean accept(File p) {
                String pn = p.getName();
                return (p.isFile() && (pn.endsWith(".bmp")
                        || pn.endsWith(".jpg") || pn.endsWith(".tga")));
            }
        });
        return lst;
    }

    /**
     * This type models a list of images for the album it pertains to.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public static class SWGImageSubAlbum extends ArrayList<SWGImage> implements
            Comparable<SWGImageSubAlbum>, SWGGui {

        /**
         * Serialization version info. Don't meddle with this or break the
         * deserialization. XXX remove this is due time.
         */
        private static final long serialVersionUID = 2373697560653970701L;

        /**
         * The album that this list pertains to.
         */
        private final SWGImageAlbum album;

        /**
         * The name of this list.
         */
        private final String name1;

        /**
         * Creates an instance of this type.
         * 
         * @param album the main album
         * @param name a name
         */
        private SWGImageSubAlbum(SWGImageAlbum album, String name) {
            super();
            this.album = album;
            this.name1 = name;
        }

        /**
         * Returns the image album for this instance.
         * 
         * @return the main album
         */
        public SWGImageAlbum album() {
            return album;
        }

        public int compareTo(SWGImageSubAlbum o) {
            return name1.compareTo(o.name1);
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof File) {
                for (SWGImage i : this)
                    if (((File) o).getAbsolutePath().equals(
                            i.getDescription())) return true;

            } else if (o instanceof SWGImage)
                return super.contains(o);

            return false;
        }

        public String getDescription() {
            return name1 + " @ " + album.getName();
        }

        public String getName() {
            return name1;
        }

        @Override
        public String toString() {
            return name1;
            // "SWGImageSubAlbum[" + getDescription() + ']';
        }
    }
}
