package swg.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import swg.SWGAide;
import swg.gui.common.SWGDoTask;
import swg.gui.tree.SWGNotesNode;
import swg.model.SWGNotes;
import swg.tools.ZWriter;

/**
 * This is a component for displaying notes files of the stations in an SWG
 * universe. This text component also provides editing features for the notes
 * file. Be aware of the limitations read in the SWGNotes class and method
 * descriptions.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a>
 *         Europe-Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGNotesPane extends JTextArea {

    /**
     * The frame of the application for this component
     */
    final SWGFrame frame;

    /**
     * <code>true</code> if the presented text is not edited since loaded or
     * last saved, <code>false</code> if the text is edited and not saved
     */
    boolean isSaved = true;

    /**
     * The component embracing this component
     */
    private SWGMainTab mainTab;

    /**
     * A list of menu items for the "Edit" menu
     */
    ArrayList<JComponent> menuItems = null;

    /**
     * The tree node associated with this notes object
     */
    private final SWGNotesNode node;

    /**
     * The notes object presented by this component
     */
    private final SWGNotes notes;

    /**
     * The document listener for this component
     */
    private SWGNotesDocumentListener notesDocumentListener;

    /**
     * The undo manager for the document of this text component
     */
    protected UndoManager notesUndo;

    /**
     * The action listener for "Save As..." at the "File" menu
     */
    private ActionListener saveAsListener;

    /**
     * The action listener for "Save..." at the "File" menu
     */
    private ActionListener saveListener;

    /**
     * Creates a text editor component for notes files on a station in an
     * universe
     * 
     * @param frame the frame of the application
     * @param mainTab the main tab of this application, on which this object is
     *        displayed
     * @param notes the notes object presented by this component
     * @param node the node referring to this component
     */
    public SWGNotesPane(SWGFrame frame, SWGMainTab mainTab, SWGNotes notes,
            SWGNotesNode node) {
        this.mainTab = mainTab;

        this.frame = frame;
        this.mainTab = mainTab;
        this.notes = notes;
        this.node = node;

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        this.setEditable(true);

        this.setText(notes != null
                ? notes.content()
                : "");
        this.setCaretPosition(0);

        notesUndo = new UndoManager();
        getDocument().addUndoableEditListener(
                new SWGNotesUndoableEditListener());

        notesDocumentListener = new SWGNotesDocumentListener();
        getDocument().addDocumentListener(notesDocumentListener);

        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                notesClicked(e);
            }
        });

        frame.addExitCallback(new SWGDoTask(new SWGDoTask.TaskCallback() {

            public void execute() {
                if (!isSaved) {
                    notesSaveQuery();
                }
            }
        }));
    }

    /**
     * Called when this GUI component gains or looses focus
     * 
     * @param gained <code>true</code> when this component gained focus,
     *        <code>false</code> when focus is lost
     */
    public void focusGained(boolean gained) {
        if (gained) {
            if (menuItems == null) {
                menuItems = new ArrayList<JComponent>();

                menuItems.add(new JPopupMenu.Separator());

                JMenuItem undo = notesUndoMenuItem();
                menuItems.add(undo);

                menuItems.add(new JPopupMenu.Separator());

                JMenuItem copy = notesCopyMenuItem();
                menuItems.add(copy);

                JMenuItem cut = notesCutMenuItem();
                menuItems.add(cut);

                JMenuItem paste = notesPasteMenuItem();
                menuItems.add(paste);

                menuItems.add(new JPopupMenu.Separator());

                JMenuItem bak = notesBackupMenuItem();
                menuItems.add(bak);

                JMenuItem reload = notesReloadMenuItem();
                menuItems.add(reload);

                saveAsListener = notesSaveAsListener();
                saveListener = notesSaveListener();
            }
            frame.saveAsAddListener(saveAsListener, "Save notes as ...");
            frame.saveAddListener(saveListener, "Save notes");
            frame.editMenuAdd(menuItems);

            notesReload();

            frame.putToLogbar_2("Size: " + getDocument().getLength() + "   ");
        } else {
            if (!isSaved) {
                TreePath newPath = mainTab.tree.getSelectionPath();
                mainTab.tree.setSelectionPath(new TreePath(node.getPath()));
                notesSaveQuery();
                mainTab.tree.setSelectionPath(newPath);
            }
            frame.saveAsRemoveListener(saveAsListener);
            frame.saveRemoveListener(saveListener);
            frame.editMenuRemove(menuItems);
        }
    }

    /**
     * Does a backup the notes file presented by this component
     */
    protected void notesBackup() {
        if (notes != null) notes.backup();
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem notesBackupMenuItem() {
        JMenuItem bak = new JMenuItem("Backup...");
        bak.setToolTipText("Backup notes the file");
        bak.setMnemonic('B');
        bak.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                notesBackup();
            }
        });
        return bak;
    }

    /**
     * Handles button-3 mouse clicks on the mail body raising a popup dialogue
     * 
     * @param e the mouse event causing this action
     */
    protected void notesClicked(MouseEvent e) {
        if (e.getModifiersEx() == InputEvent.BUTTON3_DOWN_MASK) {
            JPopupMenu popup = new JPopupMenu();

            JMenuItem bak = notesBackupMenuItem();
            popup.add(bak);

            JMenuItem reload = notesReloadMenuItem();
            reload.setText("Reload");
            popup.add(reload);

            JMenuItem save = new JMenuItem("Save");
            save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                    InputEvent.CTRL_DOWN_MASK));
            save.addActionListener(saveListener);
            popup.add(save);

            JMenuItem saveAs = new JMenuItem("Save As...");
            saveAs.setMnemonic('A');
            saveAs.addActionListener(saveAsListener);
            popup.add(saveAs);

            JMenuItem undo = notesUndoMenuItem();
            popup.add(undo);

            popup.show(this, e.getX(), e.getY());
        }
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem notesCopyMenuItem() {
        JMenuItem copy = new JMenuItem("Copy");
        copy.setToolTipText("Copy selected text to cliboard");
        copy.setMnemonic('C');
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                InputEvent.CTRL_DOWN_MASK));
        copy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                copy();
            }
        });
        return copy;
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem notesCutMenuItem() {
        JMenuItem cut = new JMenuItem("Cut");
        cut.setToolTipText("Cut selected test to clipboard");
        cut.setMnemonic('T');
        cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                InputEvent.CTRL_DOWN_MASK));
        cut.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cut();
            }
        });
        return cut;
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem notesPasteMenuItem() {
        JMenuItem paste = new JMenuItem("Paste");
        paste.setToolTipText("Paste text from clipboard");
        paste.setMnemonic('P');
        paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                InputEvent.CTRL_DOWN_MASK));
        paste.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                paste();
            }
        });
        return paste;
    }

    /**
     * Reloads the currently displayed notes file
     */
    void notesReload() {
        if (notes == null
                || !isSaved
                && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                        frame, '\"' + notes.getName()
                                + "\" has changed but is not saved.\n"
                                + "Do you want to continue without saving?",
                        "Confirm", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE))
            return;

        this.getDocument().removeDocumentListener(notesDocumentListener);
        isSaved = true;
        this.setText(notes.content());
        this.setCaretPosition(0);
        this.getDocument().addDocumentListener(notesDocumentListener);
        frame.putToLogbar_2("Size: " + getDocument().getLength() + "   ");
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem notesReloadMenuItem() {
        JMenuItem paste = new JMenuItem("Reload notes file");
        paste.setToolTipText("Reload from file");
        paste.setAccelerator(KeyStroke.getKeyStroke("F5"));
        paste.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                notesReload();
            }
        });
        return paste;
    }

    /**
     * Saves this notes object to a user selected file
     * 
     * @param showDialog <code>true</code> if a "Save As" dialog should be
     *        presented the user, <code>false</code> otherwise
     */
    protected void notesSaveAs(boolean showDialog) {
        if (notes == null || !notes.exists()) return;
        if (showDialog) {
            JFileChooser fc = SWGFrame.getFileChooser();
            fc.setSelectedFile(new File(notes.getName()));
            int ret = fc.showSaveDialog(frame);

            if (ret == JFileChooser.APPROVE_OPTION) {
                File dest = fc.getSelectedFile();

                if (!dest.getName().toLowerCase().endsWith(".txt")) {
                    ret = JOptionPane.showConfirmDialog(frame,
                            "Do you want to save to file name\n\"" +
                                    dest.getName() + "\"?", "Confirm",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (ret == JOptionPane.CANCEL_OPTION) return;
                }
                if (dest.exists()) {
                    ret = JOptionPane.showConfirmDialog(frame,
                            "File exists!\nDo you want to replace\n\"" +
                                    dest.getName() + "\"?", "File exists",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (ret == JOptionPane.CANCEL_OPTION) return;
                }
                String text = this.getText();

                ZWriter.write(text, dest, false);
                isSaved = true;
                frame.putToLogbar_2("Size: " + getDocument().getLength() + "  ");
            }
        } else { // simple "Save"
            notes.setText(this.getText()); // if !exist this type is not created
            isSaved = true;
            frame.putToLogbar_2("Size: " + getDocument().getLength() + "   ");
        }
    }

    /**
     * Returns an action listener
     * 
     * @return an action listener
     */
    private ActionListener notesSaveAsListener() {
        ActionListener sa = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                notesSaveAs(true);
            }
        };
        return sa;
    }

    /**
     * Returns an action listener
     * 
     * @return an action listener
     */
    private ActionListener notesSaveListener() {
        ActionListener save = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                notesSaveAs(false);
            }
        };
        return save;
    }

    /**
     * Questions the user to save the notes file if it is unsaved
     */
    void notesSaveQuery() {
        if (notes == null || !notes.exists()) return;
        int ret = JOptionPane.showConfirmDialog(frame,
                "\"" + notes.getName() + "\" are edited but not saved!\n" +
                        "Do you want to save the changes now?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ret == JOptionPane.YES_OPTION) {
            notesSaveAs(false);
        } else {
            isSaved = true;
        }
    }

    /**
     * Returns a menu item with an action listener
     * 
     * @return a menu item
     */
    private JMenuItem notesUndoMenuItem() {
        JMenuItem undo = new JMenuItem("Undo");
        undo.setToolTipText("Undo recent text editing");
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                InputEvent.CTRL_DOWN_MASK));
        undo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!notesUndo.canUndo()) return;
                try {
                    notesUndo.undo();
                } catch (CannotUndoException ex) {
                    SWGAide.printError("SWGNotesPane:notesUndoAction", ex);
                }
            }
        });
        undo.setEnabled(notes != null && notes.exists());
        return undo;
    }

    /**
     * A simple document listener for this notes component
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Europe-Chimaera.Zimoon
     */
    class SWGNotesDocumentListener implements DocumentListener {

        public void changedUpdate(DocumentEvent e) {
            // ignore
        }

        public void insertUpdate(DocumentEvent e) {
            isSaved = false;
            frame.putToLogbar_2("Size: " + getDocument().getLength()
                    + " \u2605   ");
        }

        public void removeUpdate(DocumentEvent e) {
            isSaved = false;
            frame.putToLogbar_2("Size: " + getDocument().getLength()
                    + " \u2605   ");
        }
    }

    /**
     * A simple undoable edit listener class
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Europe-Chimaera.Zimoon
     */
    class SWGNotesUndoableEditListener implements UndoableEditListener {

        public void undoableEditHappened(UndoableEditEvent e) {
            notesUndo.addEdit(e.getEdit());
        }
    }
}
