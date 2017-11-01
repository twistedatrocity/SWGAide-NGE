package swg.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import swg.SWGAide;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJTable;
import swg.gui.common.SWGListModel;
import swg.model.SWGUniverse;
import swg.model.images.SWGImage;
import swg.model.images.SWGImageAlbum.SWGImageSubAlbum;
import swg.tools.SimplePrefsKeeper;
import swg.tools.ZNumber;

/**
 * This type is a simple image/screen-shot viewer. For a selected universe it
 * displays a list of thumb-nails and a canvas for a selected image. The album
 * populates and refreshes itself on demand, that is when the user selects the
 * main Album tab and each time it gains focus. Images are searched for in
 * SWG\screenshots\ and SWGAide\album\
 * <p>
 * History:<br/>
 * This album in SWGAide and its supporting types were simplified during the
 * autumn 2010 to make SWGAide and the DAT file host independent so that a user
 * freely can move between computers. Thu the DAT file no longer contains
 * references to images.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGAlbumTab extends JPanel {

    /**
     * A constant for the tool-tip text for save-as menu item.
     */
    private static final String saveImageAsTT = "Save selected image anywhere";

    /**
     * A list of the albums SWG and TC, or just SWG.
     */
    private JList<SWGImageSubAlbum> albums;

    /**
     * The current album, SWG or TC. This value is selected by the user.
     */
    private SWGImageSubAlbum currentAlbum;

    /**
     * The currently displayed image.
     */
    private SWGImage currentImage;

    /**
     * The frame with the tabbed pane that contains this album.
     */
    private SWGFrame frame;

    /**
     * The main GUI element at which the current image is displayed.
     */
    private JPanel imageCanvas;

    /**
     * A GUI element that displays the current file path.
     */
    private JTextField imagePath;

    /**
     * A menu item for displaying image properties.
     */
    private JMenuItem imageProp;

    /**
     * An action listener a the Save As menu item.
     */
    private ActionListener imageSaveAs;

    /**
     * A menu item for toggling between full size and scaled size.
     */
    private JMenuItem imageScale;

    /**
     * A flag that denotes if the current image is scaled or full size; {@code
     * true} denotes scaled.
     */
    private boolean imageScaled = true;

    /**
     * A list of menu items which are used to populate SWGAide's edit menu when
     * this type gains focus.
     */
    private List<JComponent> menuItems;

    /**
     * The thumb nail list model.
     */
    private ThumbModel thumbModel;

    /**
     * The thumb nail GUI element, a table of one column.
     */
    private JTable thumbNails;

    /**
     * Creates an instance of this image viewer.
     * 
     * @param frame the frame that contains the tabbed pane for this album
     */
    SWGAlbumTab(SWGFrame frame) {
        this.frame = frame;

        // create interior lazily, see focusGained()
        frame.getTabPane().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                focusGained(e);
            }
        });
    }

    /**
     * Called when the user selects an album, SWG or TC. This method also
     * refreshes the selected album and updates the thumb-nail view.
     * 
     * @param e the event that triggers the call
     */
    private void actionAlbumSelect(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            SWGImageSubAlbum s = albums.getSelectedValue();
            if (s == null) return;

            currentAlbum = s;
            currentAlbum.album().refresh();

            thumbNails.getColumnModel().getColumn(0).
                    setHeaderValue(currentAlbum.getName());

            this.repaint();
            thumbModel.fireTableDataChanged();
        }
    }

    /**
     * Called when the user mouse-clicks the image canvas. If it is a
     * right-click and if {@link #currentImage} {@code != null} this method
     * invokes {@link #imagePopupDialog(MouseEvent)}.
     * 
     * @param e the event that triggers the call
     */
    private void actionImageMouse(MouseEvent e) {
        if (currentImage != null && e.getButton() == MouseEvent.BUTTON3)
            imagePopupDialog(e);
    }

    /**
     * Called when the user mouse-clicks the list of thumb-nails. If it is a
     * right-click this method invokes {@link #thumbPopupDialog(MouseEvent)}.
     * 
     * @param e the event that triggers the call
     */
    private void actionThumbMouse(final MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3)
            thumbPopupDialog(e);
    }

    /**
     * Called when the user selects a thumb-nail. This method updates the canvas
     * with the corresponding image. If the selection is invalid or if it equals
     * the current image this method does nothing.
     * 
     * @param e the event that triggers the call
     */
    private void actionThumbSelect(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int row = thumbNails.getSelectedRow();
            if (row < 0) return;

            SWGImage i = currentAlbum.get(row);
            if (i == currentImage) return;

            currentImage = i;
            imageScaled = true;
            imageDisplay(currentImage);
            imagePath.setText(currentImage.getDescription());
        }
    }

    /**
     * This method is invoked when the user selects the Album tab at the main
     * tabbed pane. If this element is yet a stub it is created and populated,
     * otherwise, if {@link #currentAlbum} {@code != null} this method triggers
     * a refresh of image folders.
     * 
     * @param e the event that triggers this call
     */
    private void focusGained(ChangeEvent e) {
        if (frame.getTabPane().getSelectedComponent() == this) {
            if (thumbModel == null)
                make();

            frame.editMenuAdd(menuItems);
            if (currentImage != null) {
                frame.saveAsAddListener(imageSaveAs, saveImageAsTT);
                imageDisplay(currentImage); // in case component is resized
            }

            if (currentAlbum != null) {
                currentAlbum.album().refresh();
                thumbModel.fireTableDataChanged();
            }

            frame.putToLogbar_1(null);
            frame.putToLogbar_2(null);
        } else if (menuItems != null) {
            frame.editMenuRemove(menuItems);
            frame.saveAsRemoveListener(imageSaveAs);
        }
    }

    /**
     * Helper method which displays the specified image at the canvas of this
     * type. If the argument is {@code null} this method invokes
     * {@link #imageDisplayNull()}.
     * 
     * @param image the image to show
     */
    private void imageDisplay(final SWGImage image) {
        if (image == null)
            imageDisplayNull();
        else
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Image img;
                    if (imageScaled) {
                        JViewport port = (JViewport) imageCanvas.getParent();
                        JScrollPane jsp = (JScrollPane) port.getParent();
                        Dimension dim = port.getExtentSize();

                        dim.width += jsp.getVerticalScrollBar().isShowing()
                                ? 5
                                : -10;
                        dim.height += jsp.getHorizontalScrollBar().isShowing()
                                ? 5
                                : -10;

                        img = image.imageScaled(dim.width, dim.height);
                    } else
                        img = image.image();

                    if (img == null) {
                        imageDisplayNull();
                        return;
                    }

                    imageCanvas.removeAll();
                    ImageIcon icon = new ImageIcon(img);
                    imageCanvas.add(new JLabel(icon));

                    imageCanvas.setPreferredSize(new Dimension(
                            icon.getIconWidth(), icon.getIconHeight()));

                    frame.repaint();
                    imageEnableGUI(true);
                }
            }); 
    }

    /**
     * Helper method which resets the image canvas and related GUI elements.
     * This method is invoked if there is no image to display or if there is an
     * error.
     */
    private void imageDisplayNull() {
        imageCanvas.removeAll();
        imageCanvas.add(new JLabel(""));
        frame.repaint();
        imageEnableGUI(false);
    }

    /**
     * Helper method which enables or disables action listener and GUI elements
     * thats relate to {@link #currentImage}.
     * 
     * @param enable {@code true} to enable
     */
    private void imageEnableGUI(boolean enable) {
        imageProp.setEnabled(enable);
        imageScale.setEnabled(enable);
        if (!enable) {
            frame.saveAsRemoveListener(imageSaveAs);
            currentImage = null;
        } else
            frame.saveAsAddListener(imageSaveAs, saveImageAsTT);
    }

    /**
     * Helper method which displays a popup dialog for the image canvas. This
     * method is invoked if the user right clicks the canvas and if
     * {@link #currentImage} {@code != null}.
     * 
     * @param e the event that triggers the call
     */
    private void imagePopupDialog(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();

        popup.add(imageResizeMenu(imageScaled
                ? "Full size"
                : "Scaled size"));

        popup.addSeparator();

        popup.add(imageSaveAsMenu(currentImage));

        popup.addSeparator();

        popup.add(imagePropertiesMenu(currentImage));

        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Helper method which displays a dialog that reads some properties about
     * the specified image. If the argument is {@code null} this method does
     * nothing.
     * 
     * @param image an image
     */
    private void imageProperties(SWGImage image) {
        if (image == null) return;

        String size = " bytes";
        double ds = image.size();
        if (ds > 1000) {
            ds /= 1024.0;
            size = " kB";
        }
        if (ds > 1000) {
            ds /= 1024.0;
            size = " MB";
        }
        size = ZNumber.asText(ds, 1, 2) + size;

        Image ig = image.image();
        Date d = new Date(image.lastModified());

        JOptionPane.showMessageDialog(imageCanvas,
                String.format("Properties:%n\"%s\"%n%nLast modified: %s%n" +
                        "Width x Height: %d x %d%nSize: %s%nMedia Type: %s",
                        image.getDescription(), d.toString(),
                        Integer.valueOf(ig.getWidth(null)),
                        Integer.valueOf(ig.getHeight(null)),
                        size, image.imageType().toUpperCase(Locale.ENGLISH)),
                "Image properties", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Helper method which creates and returns a menu item with an action
     * listener that displays the properties for an image. If the argument is
     * {@code null} the listener selects {@link #currentImage}. This item is
     * always enabled and it always invokes {@link #imageProperties(SWGImage)}.
     * 
     * @param img an image, or {@code null}
     * @return a menu item
     */
    private JMenuItem imagePropertiesMenu(final SWGImage img) {
        final JMenuItem im = new JMenuItem("Properties", KeyEvent.VK_P);
        im.setToolTipText("Display the properties of the image");
        im.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                imageProperties(img == null
                        ? currentImage
                        : img);
            }
        });
        return im;
    }

    /**
     * Helper method which creates and returns a menu item with an action
     * listener that toggles between full and scaled image size.
     * 
     * @param t the title for the menu item
     * @return a menu item
     */
    private JMenuItem imageResizeMenu(String t) {
        JMenuItem im = new JMenuItem(t, KeyEvent.VK_Z);
        im.setToolTipText("Toggle between full and scaled image size");
        im.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                if (currentImage == null) return;
                imageScaled = !imageScaled;
                imageDisplay(currentImage);
            }
        });
        return im;
    }

    /**
     * Helper method which saves the specified image to a file that is selected
     * by the user. This method displays a standard file-save dialog. The image
     * is copied to the selected path, not moved. If the argument is {@code
     * null} this method does nothing. If there is an error it is caught and
     * written to SWGAide's log file.
     * 
     * @param image an image
     */
    private void imageSaveAs(SWGImage image) {
        if (image == null) return;

        try {
            JFileChooser fc = SWGFrame.getFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setAcceptAllFileFilterUsed(true);

            String uh = System.getProperty("user.home");
            File dest = new File(uh, "Pictures");
            dest = (dest.exists())
                    ? new File(dest, image.getName())
                    : new File(image.getName());
            fc.setSelectedFile(dest);

            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                dest = fc.getSelectedFile();
                if (image.equals(dest)) {
                    JOptionPane.showMessageDialog(this,
                            "Overwrite original image not allowed",
                            "Invalid action", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!dest.getName().toUpperCase(Locale.ENGLISH).endsWith(
                        image.imageType().toUpperCase(Locale.ENGLISH))
                        && JOptionPane.showConfirmDialog(this,
                                "File ending does not match image type\n" +
                                        "Save anyway?", "Confirm",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE)
                        != JOptionPane.OK_OPTION)
                    return;

                if (dest.exists()
                        && JOptionPane.showConfirmDialog(this,
                                "File exists, overwrite\n\"" +
                                        dest.getName() + "\"?", "Confirm",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE)
                        != JOptionPane.OK_OPTION)
                    return;

                image.copyTo(dest);
            } // else CANCEL_OPTION chosen
        } catch (Throwable e) {
            SWGAide.printError("SWGImageAlbum:imageSave", e);
        }
    }

    /**
     * Creates and returns a menu item with an action listener for "Save as...".
     * If the argument is {@code null} no listener is added and the item is
     * disabled.
     * 
     * @param i an image
     * @return a menu item
     */
    private JMenuItem imageSaveAsMenu(final SWGImage i) {
        JMenuItem sa = new JMenuItem("Save as...");
        sa.setToolTipText(saveImageAsTT);
        sa.setEnabled(i != null);

        if (i != null)
            sa.addActionListener(i == currentImage
                    ? imageSaveAs
                    : new AbstractAction() {
                        public void actionPerformed(ActionEvent e1) {
                            imageSaveAs(i);
                        }
                    });

        return sa;
    }

    /**
     * Creates the interior of this tab pane.
     */
    private synchronized void make() {
        if (thumbModel != null) return; // sanity

        this.setLayout(new BorderLayout());
        this.add(makeWest(), BorderLayout.LINE_START);
        this.add(makeCenter(), BorderLayout.CENTER);
        this.add(makeSouth(), BorderLayout.PAGE_END);

        menuItems = new ArrayList<JComponent>(3);

        menuItems.add(new JPopupMenu.Separator());

        imageScale = imageResizeMenu("Resize image");
        imageScale.setEnabled(false);
        menuItems.add(imageScale);

        imageProp = imagePropertiesMenu(null);
        imageProp.setEnabled(false);
        menuItems.add(imageProp);

        imageSaveAs = new ActionListener() {
            public void actionPerformed(ActionEvent e1) {
                imageSaveAs(currentImage);
            }
        };
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (frame.getTabPane().getSelectedComponent() == SWGAlbumTab.this)
                    imageDisplay(currentImage);
            }
        });

        if (albums.getModel().getSize() == 1)
            albums.setSelectedIndex(0);
    }

    /**
     * Helper method which creates and returns a canvas for images. This is the
     * main panel for this type.
     * 
     * @return a GUI element
     */
    private Component makeCenter() {
        imageCanvas = new JPanel();
        imageCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionImageMouse(e);
            }
        });

        imageCanvas.add(new JLabel(new ImageIcon(
                SWGFrame.class.getResource("images/swg.png"))));

        return new JScrollPane(imageCanvas);
    }

    /**
     * Helper method which creates and returns the southern GUI element. This
     * element contains the album chooser and the notes editor fields.
     * 
     * @return a GUI element
     */
    private Component makeSouth() {
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));

        bottom.add(makeSouthAlbumChooser());
        bottom.add(makeSouthFileComponent());

        return bottom;
    }

    /**
     * Helper method which creates and returns a GUI element at which to select
     * an album, SWG or TC.
     * 
     * @return a GUI element
     */
    private Component makeSouthAlbumChooser() {
        SWGListModel<SWGImageSubAlbum> mm = new SWGListModel<SWGImageSubAlbum>();
        mm.setElements(makeSouthAlbumChooserList());

        albums = new JList<SWGImageSubAlbum>(mm);
        albums.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        albums.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                actionAlbumSelect(e);
            }
        });
        JScrollPane jsp = new JScrollPane(albums,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        jsp.setAlignmentY(Component.TOP_ALIGNMENT);
        jsp.setPreferredSize(new Dimension(150, 26));
        jsp.setMinimumSize(new Dimension(150, 26));
        jsp.setMaximumSize(new Dimension(150, 26));
        return jsp;
    }

    /**
     * Helper method which returns a list of albums, SWG and TC.
     * 
     * @return a list of albums
     */
    private List<SWGImageSubAlbum> makeSouthAlbumChooserList() {
        List<SWGImageSubAlbum> ual = new ArrayList<SWGImageSubAlbum>(2);
        SimplePrefsKeeper pk = SWGFrame.getPrefsKeeper();

        SWGUniverse u = (SWGUniverse) pk.get("swgUniverse");
        if (u != null)
            ual.add(u.imageAlbum().images());

        u = (SWGUniverse) pk.get("swgTestCenter");
        if (u != null)
            ual.add(u.imageAlbum().images());

        return ual;
    }

    /**
     * Helper method which creates and returns a GUI element that displays the
     * image file path.
     * 
     * @return a GUI element
     */
    private Component makeSouthFileComponent() {
        JPanel bp = new JPanel();
        bp.setLayout(new BoxLayout(bp, BoxLayout.X_AXIS));
        bp.setBorder(BorderFactory.createEtchedBorder());
        bp.setAlignmentY(Component.TOP_ALIGNMENT);

        bp.add(new JLabel("File: "));
        imagePath = new JTextField(90);
        imagePath.setEditable(false);
        bp.add(imagePath);
        return bp;
    }

    /**
     * Helper method which creates and returns the leftmost GUI element. This
     * component is the bar that displays thumb-nail icons for an album.
     * 
     * @return a GUI element
     */
    private Component makeWest() {
        thumbModel = new ThumbModel();

        thumbNails = new SWGJTable(thumbModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                // TODO: change so model rather emits SWGImage
                int row = rowAtPoint(e.getPoint());
                if (row < 0) return "";
                SWGImage img = currentAlbum.get(row);
                return img.getName();
            }
        };

        thumbModel.setColumnIdentifiers(new String[] { " " });
        thumbNails.setAutoCreateColumnsFromModel(false);
        thumbNails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        thumbNails.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        thumbNails.getColumnModel().getColumn(0).
                setCellRenderer(new ThumbRenderer());

        final int w = 150 - 19;
        SWGGuiUtils.tableColumnSetWidth(thumbNails, 0, w - 15, w, w + 15);
        thumbNails.setRowHeight(ThumbRenderer.ICON_WIDTH);

        thumbNails.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        if (!e.getValueIsAdjusting()) {
                            actionThumbSelect(e);
                        }
                    }
                });

        thumbNails.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                actionThumbMouse(evt);
            }
        });

        JScrollPane jsp = new JScrollPane(thumbNails,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jsp.setPreferredSize(new Dimension(150, 450));
        jsp.getVerticalScrollBar().setUnitIncrement(30);
        return jsp;
    }

    /**
     * Helper method which displays a popup menu for the thumb-nails.
     * 
     * @param e the event that triggers the call
     */
    private void thumbPopupDialog(final MouseEvent e) {
        JPopupMenu pop = new JPopupMenu();

        SWGImage img = currentAlbum.get(thumbNails.rowAtPoint(e.getPoint()));

        pop.add(imageSaveAsMenu(img));
        pop.addSeparator();
        pop.add(imagePropertiesMenu(img));

        pop.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * A table model for the list of thumb-nails.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class ThumbModel extends DefaultTableModel {

        /**
         * Creates an instance of this type.
         */
        ThumbModel() {
            super();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return SWGImage.class;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            if (currentAlbum == null) return 0;
            return currentAlbum.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return (currentAlbum == null)
                    ? null
                    : currentAlbum.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    } // end class SWGImageTableModel

    /**
     * A cell renderer for the table of thumb-nails.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final static class ThumbRenderer extends DefaultTableCellRenderer {

        /**
         * The width of the presented icon.
         */
        static final int ICON_WIDTH = 120;

        /**
         * Creates an instance of this type.
         */
        ThumbRenderer() {
            this.setVerticalAlignment(SwingConstants.CENTER);
            this.setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public void setValue(Object o) {
            ImageIcon ic = ((SWGImage) o).imageThumb(ICON_WIDTH);
            setIcon(ic);

            if (ic != null) {
                // TODO: add setTooltip when redone so o is SWGImage
            }
        }
    }
}
