package swg.gui.tree;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.EventObject;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import swg.gui.SWGNotesPane;
import swg.gui.common.SWGTextInputDialogue;
import swg.model.SWGNotes;
import swg.model.SWGStation;

/**
 * This type wraps an instance of {@link SWGNotes}.
 * <p>
 * If no valid path exists for the notes this node is rendered gray and some
 * features are invalid.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a>
 *         Chimaera.Zimoon
 */
public class SWGNotesNode extends SWGTreeNode {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 117760984237483942L;

    /**
     * The GUI component for the notes object associated with this node
     */
    private transient SWGNotesPane notesPane = null;

    /**
     * Creates a node for {@code notes}
     * 
     * @param notes the notes object represented by this node
     */
    public SWGNotesNode(SWGNotes notes) {
        super(notes);
        this.setAllowsChildren(false);
    }

    /**
     * Deletes this notes object from the system
     */
    private void delete() {
        if (notes() == null
                || JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                        frame,
                        String.format("Delete \"%s\"", notes().getName()),
                        "Confirm deletion",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE)) {

            if (notes() != null) notes().erase(true);

            SWGTreeNode p = (SWGTreeNode) this.getParent();
            mainTab.tree.setSelectionPath(new TreePath(p.getPath()));
            SWGTreeNode.focusTransition(p, new EventObject(this));
            ((DefaultTreeModel) mainTab.tree.getModel())
                    .removeNodeFromParent(this);

            focusLost();
        }
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem deleteMenuItem() {
        JMenuItem del = new JMenuItem("Delete");
        del.setToolTipText("Delete the notes file");
        del.setMnemonic('D');
        del.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                delete();
            }
        });
        return del;
    }

    @Override
    public boolean exists() {
        return notes().exists();
    }

    @Override
    protected void focusGained(EventObject evt) {
    	if (evt instanceof MouseEvent) {
    		int but = ((MouseEvent) evt).getButton();
    		if (but == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            popup.add(deleteMenuItem());

            MouseEvent e = (MouseEvent) evt;
            popup.show(e.getComponent(), e.getX(), e.getY());
    		}
        }
        if (exists()) {
            notesPane();
            mainTab.showNotes(notesPane);
            notesPane.focusGained(true);
        } else
            mainTab.setMainPaneDefault();

        frame.putToLogbar_1(String.format("%s \u2014 %s%s",
                notes().parentDir(), notes().getName(), exists()
                        ? ""
                        : " (no SWG)"));
    }

    @Override
    protected void focusLost() {
        if (notesPane != null) notesPane.focusGained(false);
    }

    /**
     * Returns the notes object that is wrapped by this node.
     * 
     * @return the notes
     */
    SWGNotes notes() {
        return (SWGNotes) userObject;
    }

    /**
     * Returns the GUI component for notes
     * 
     * @return the GUI component for notes
     */
    private SWGNotesPane notesPane() {
        if (notesPane == null) {
            notesPane = new SWGNotesPane(frame, mainTab,
                    notes().exists()
                            ? notes()
                            : null, this);
        }
        return notesPane;
    }

    /**
     * Creates and returns a new node.
     * 
     * @param parent the parent node, referring to a station in the universe
     * @return a new node for a notes file, {@code null} if user opted out
     */
    public static SWGNotesNode newNode(SWGStationNode parent) {
        final SWGStation stn = parent.station();
        Point pp = SWGTreeNode.frame.getLocationOnScreen();
        Dimension dim = SWGTreeNode.frame.getSize();
        pp.translate((dim.width >> 1) - 143, (dim.height >> 1) - 64);
        SWGTextInputDialogue.TextValidation val =
                new SWGTextInputDialogue.TextValidation() {
                    public boolean validateText(String name) {
                        if (name.contains("/") || name.contains("\\"))
                            return false;
                        String n = name.toLowerCase().endsWith(".txt")
                                ? name
                                : name + ".txt";
                        File tmp = new File(stn.swgPath(), n);
                        if (tmp.exists() || tmp.isDirectory()) return false;
                        if (stn.notes(n) != null) return false;
                        return true;
                    }
                };
        SWGTextInputDialogue diag = new SWGTextInputDialogue(SWGTreeNode.frame,
                val, pp, "Create notes file", "Enter a unique name", "");
        diag.setVisible(true);
        String name = diag.getTypedText();

        if (name == null) return null;

        SWGNotes.getInstance(name, stn, null, false);
        return null;
    }
}
