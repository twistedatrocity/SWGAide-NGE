package swg.gui.mail;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.soap.SOAPException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDoTask;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJTable;
import swg.gui.common.SWGTextInputDialogue;
import swg.gui.common.SWGTextInputDialogue.TextValidation;
import swg.model.SWGCharacter;
import swg.model.mail.SWGMailBox;
import swg.model.mail.SWGMailFolder;
import swg.model.mail.SWGMailMessage;
import swg.model.mail.SWGMailMessage.Type;
import swg.tools.SimplePrefsKeeper;
import swg.tools.ZString;

/**
 * This class mimics a mail client and will present the view to the underlying
 * model constituted by SWGMailBox.
 * 
 * @see SWGMailBox
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGMailPane extends JSplitPane implements TextValidation {

    /**
     * A date formatter.
     */
    private final static DateFormat df = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM, DateFormat.SHORT);

    /**
     * Regular expression used to split from lines
     */
    private static Pattern regexp = Pattern.compile("[SWG]*\\.([\\w\\-]*)\\.(.+)");
    
    /**
     * The GUI list of mail folders
     */
    SWGFolderList folderList;

    /**
     * The application frame containing this mail panel
     */
    private final SWGFrame frame;

    /**
     * A menu for ISDroids options.
     */
    private JMenuItem isDroidMenu;

    /**
     * The component for the ISDroid specific GUI and its features
     */
    private SWGMailISDroidPanel ISDroidPanel;

    /**
     * The mail body text for the selected mail
     */
    private SWGMailBody mailBody;

    /**
     * The scroll pane containing the mail body, or the ISDroid panel if that is
     * chosen
     */
    private JScrollPane mailBodyJSP;

    /**
     * The mail header for the selected mail
     */
    private SWGMailHeader mailHeader;

    /**
     * The GUI list of mails for the selected mail folder, implemented by a
     * table
     */
    SWGMailList mailList;

    /**
     * The supporting model for the GUI table of list of mails
     */
    private SWGMailTableModel mailModel;

    /**
     * A listener for save mail menu items
     */
    private final ActionListener mailSaveAs;

    /**
     * A list of current menu items
     */
    private final List<JComponent> menuItems;

    /**
     * A list of the mail messages for the recentmost selected folder
     */
    private List<SWGMailMessage> messages;

    /**
     * A menu item with options for copying and deleting mails.
     */
    private JMenu optionMailCopy;

    /**
     * Select whether to use the special ISDroid panel or not
     */
    private JCheckBox showISDroidPanel;

    /**
     * The current toon for this mail client. This variable will shift according
     * to the most recently selected character.
     */
    SWGCharacter toon;

    /**
     * The split pane for the upper part of the view, that is the folder list
     * and the mail list.
     */
    private JSplitPane upperSplitPane;

    /**
     * Creates the GUI component for the mail client
     * 
     * @param frame the application for for this object
     */
    public SWGMailPane(SWGFrame frame) {
        super(JSplitPane.VERTICAL_SPLIT);

        menuItems = new ArrayList<JComponent>();
        this.optionMailCopy = makeOptionMailCopy();

        this.isDroidMenu = makeOptionsISDroid();

        this.frame = frame;
        this.setDividerSize(5);

        this.setLeftComponent(makeNorthPanel());
        this.setRightComponent(makeSouthPanel());

        SimplePrefsKeeper pk = SWGFrame.getPrefsKeeper();
        this.setDividerLocation(((Integer) pk.get(
                "mailClientDividerLocation",
                Integer.valueOf(150))).intValue());
        upperSplitPane.setDividerLocation(((Integer) pk.get(
                "mailClientUpperDividerLocationUp",
                Integer.valueOf(100))).intValue());

        mailSaveAs = new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                mailSaveAs();
            }
        };

        frame.addExitCallback(new SWGDoTask(new SWGDoTask.TaskCallback() {
            
            public void execute() {
                doExit();
            }
        }));
        makeKeyActions();
    }

    /**
     * Raises a popup menu for the clicked list of folders
     * 
     * @param e the mouse event that caused this call
     */
    private void actionFolderList(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            int sel = folderList.locationToIndex(e.getPoint());
            if (folderList.getSelectedIndex() != sel) {
                folderList.setSelectedIndex(sel);
            }
            String folder = folderList.getSelectedValue();
            if (folder == null) return;

            boolean nono = toon.mailBox().folder(folder).isDefault();

            JPopupMenu popup = new JPopupMenu();

            JMenuItem folderAdd = folderAddMenuItem();
            popup.add(folderAdd);

            JMenuItem folderRename = folderRenameMenuItem();
            folderRename.setEnabled(!nono);
            popup.add(folderRename);

            JMenuItem folderDelete = folderDeleteMenuItem();
            folderDelete.setEnabled(!nono);
            popup.add(folderDelete);

            popup.show(folderList, e.getX(), e.getY());
        }
    }

    /**
     * Handles button-3 mouse clicks on the mail body raising a popup dialogue
     * 
     * @param e the mouse event causing this action
     */
    
    private void actionMailBody(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            boolean b = !messages.isEmpty() && toon.galaxy().exists();

            JMenuItem copy = mailCopyMenuItem();
            popup.add(copy);

            JMenuItem saveAs = mailSaveAsMenuItem();
            saveAs.setEnabled(b);
            popup.add(saveAs);

            popup.addSeparator();

            JMenuItem fontBigger = new JMenuItem("Font increase");
            fontBigger.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,
                    InputEvent.CTRL_MASK));
            fontBigger.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mailBodyChangeFont(1.2);
                }
            });
            popup.add(fontBigger);

            JMenuItem fontLesser = new JMenuItem("Font decrease");
            fontLesser.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                    InputEvent.CTRL_MASK));
            fontLesser.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mailBodyChangeFont(1 / 1.2);
                }
            });
            popup.add(fontLesser);

            JMenuItem fontNormal = new JMenuItem("Font normal");
            fontNormal.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0,
                    InputEvent.CTRL_MASK));
            fontNormal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mailBodyChangeFont(0);
                }
            });
            popup.add(fontNormal);

            popup.show(mailBody, e.getX(), e.getY());
        }
    }

    /**
     * Handles button-3 mouse clicks on the list of mails raising a popup
     * dialogue
     * 
     * @param e the mouse event
     * @param row the row of the view under the mouse click
     */
    private void actionMailList(MouseEvent e, int row) {
        mailListEnsureSelect(row);

        boolean be = toon.galaxy().exists();
        boolean bb = be && !messages.isEmpty();

        JPopupMenu popup = new JPopupMenu();

        JMenuItem mailSave = mailSaveAsMenuItem();
        mailSave.setEnabled(bb);
        popup.add(mailSave);

        JMenuItem mailMove = mailMoveMenuItem();
        popup.add(mailMove);

        JMenuItem mailDel = mailDeleteMenuItem();
        popup.add(mailDel);

        popup.addSeparator();

        JMenuItem mailSearch = mailSearchMenuItem();
        mailSearch.setEnabled(be);
        popup.add(mailSearch);

        popup.show(mailList, e.getX(), e.getY());
    }

    /**
     * Execute some exit routines, such as saving user settings
     * 
     * @see SWGDoTask
     */
    private void doExit() {
        if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH)
            return;

        SWGFrame.getPrefsKeeper().add("mailClientDividerLocation",
                new Integer(getDividerLocation()));
        SWGFrame.getPrefsKeeper().add("mailClientUpperDividerLocationUp",
                new Integer(upperSplitPane.getDividerLocation()));

        for (int i = 0; i < mailList.getColumnCount(); ++i) {
            TableColumn col = mailList.getColumnModel().getColumn(i);
            SWGFrame.getPrefsKeeper().add("mailClientColumnSize" + i,
                    Integer.valueOf(col.getWidth()));
        }
    }

    /**
     * Sets this pane focused or unfocused and updates GUI components
     * accordingly to reflect the current state; GUI components may be menu
     * items etcetera
     * 
     * @param selected <code>true</code> to set the state of this component to
     *        focused, <code>false</code> otherwise
     */
    public void focusGained(boolean selected) {
        if (selected) {
            frame.editMenuAdd(menuItems);
            frame.saveAsAddListener(mailSaveAs, "Save a selected mail as...");
            frame.optionsMenuAdd(isDroidMenu);
            frame.optionsMenuAdd(optionMailCopy);
        } else {
            frame.editMenuRemove(menuItems);
            frame.saveAsRemoveListener(mailSaveAs);
            frame.optionsMenuRemove(isDroidMenu);
            frame.optionsMenuRemove(optionMailCopy);
        }
        if (ISDroidPanel != null && showISDroidPanel.isSelected()) {
            ISDroidPanel.focusGained(selected);
        }
    }

    /**
     * Adds another folder to the currently selected character's mail box
     */
    private void folderAddFolder() {
        SWGMailBox box = toon.mailBox();
        SWGTextInputDialogue diag = new SWGTextInputDialogue(frame, this,
                folderList.getLocationOnScreen(), "Add folder",
                "Add another folder to mail client", "The folder name:");
        diag.setVisible(true);
        String name = diag.getTypedText();

        if (name != null) {
            box.folderAdd(name);
            folderList.setListData(box.folderNames(null));
        }
    }

    /**
     * Returns a menu item for adding new folders to the list of folders
     * 
     * @return the new menu item
     */
    private JMenuItem folderAddMenuItem() {
        JMenuItem folderAdd = new JMenuItem("Add folder...");
        folderAdd.setToolTipText("Add another folder");
        folderAdd.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                folderAddFolder();
            }
        });
        return folderAdd;
    }

    /**
     * Deletes the selected folder from the list of mail folders if it is not a
     * default folder and it is not empty
     */
    private void folderDelete() {
        String folder = folderList.getSelectedValue();
        if (folder == null)
            return;

        if (toon.mailBox().folder(folder).isDefault()) {
            JOptionPane.showMessageDialog(folderList, '\"' + folder
                    + "\" is a default folder.\nDeletion not allowed!",
                    "Default folder", JOptionPane.ERROR_MESSAGE);
        } else if (toon.mailBox().folder(folder).size() > 0) {
            JOptionPane.showMessageDialog(folderList,
                    "The album is not empty.\nCannot continue!", "Not empty",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            int response =
                    JOptionPane.showConfirmDialog(folderList,
                            "Confirm deletion of \"" + folder + '\"',
                            "Delete folder",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
            if (response == JOptionPane.OK_OPTION) {
                toon.mailBox().folderDelete(folder);
                folderList.setListData(toon.mailBox().folderNames(null));
            }
        }
    }

    /**
     * Returns a menu item for deletion of a folder from the list of folders
     * 
     * @return the new menu item
     */
    private JMenuItem folderDeleteMenuItem() {
        JMenuItem folderDelete = new JMenuItem("Delete folder");
        folderDelete.setToolTipText("Delete selected folder");
        folderDelete.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                folderDelete();
            }
        });
        return folderDelete;
    }

    /**
     * Reacts on selections in the list of folders
     * 
     * @param folderName the selected folder name
     */
    private void folderListSelected(String folderName) {
        if (folderName == null) return;

        boolean isDeflt = toon.mailBox().folder(folderName).isDefault();
        for (JComponent cmp : menuItems) {
            if (cmp instanceof JMenuItem) {
                JMenuItem jmi = (JMenuItem) cmp;
                String txt = jmi.getText().toLowerCase(Locale.ENGLISH);
                if (txt.indexOf("folder") >= 0 && !txt.startsWith("add"))
                    jmi.setEnabled(!isDeflt);
            }
        }

        if (folderName.equals("ISDroid"))
            showISDroidPanel.setEnabled(true);
        else
            showISDroidPanel.setEnabled(false);

        selectedISDroid();
        updateMailList(folderName, 0);
    }

    /**
     * Renames a selected folder
     */
    private void folderRename() {
        String folder = folderList.getSelectedValue();
        if (folder == null) return;

        if (toon.mailBox().folder(folder).isDefault()) {
            JOptionPane.showMessageDialog(folderList,
                    "Renaming default folder is not allowed: " + folder,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SWGTextInputDialogue diag = new SWGTextInputDialogue(frame, this,
                folderList.getLocationOnScreen(), "Rename folder",
                "Rename folder \"" + folder + "\"", "New name:");
        diag.setVisible(true);
        String name = diag.getTypedText();

        if (name != null) {
            SWGMailFolder fld = toon.mailBox().folder(folder);
            fld.setName(name);
            folderList.setListData(toon.mailBox().folderNames(null));
        }
    }

    /**
     * Returns a menu item for renaming folders in the list of folders
     * 
     * @return the new menu item
     */
    private JMenuItem folderRenameMenuItem() {
        JMenuItem folderRename = new JMenuItem("Rename folder...");
        folderRename.setToolTipText("Rename selected folder");
        folderRename.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                folderRename();
            }
        });
        return folderRename;
    }

    /**
     * Returns the currently selected character
     * 
     * @return the currently selected character
     */
    SWGCharacter getCharacter() {
        return toon;
    }

    /**
     * Returns a formatted string of a date calculated from <code>seconds</code>
     * 
     * @param seconds the date in seconds from Jan 1, 1970, 00:00:00 GMT
     * @return a formatted string of a date
     */
    private String getDateString(long seconds) {
        return df.format(new Date(seconds * 1000));
    }

    /**
     * Returns a string with the from address possibly reformatted to something
     * readable
     * 
     * @param from the from text string
     * @return a reformatted text string for better readability in the GUI
     */
    private String getFromString(String from) {
        // the string may have a few looks:
        // SWG.Europe-Chimaera.\#ff0000equipment by NN
        // SWG.Europe-Chimaera.@money/acct_n:bank
        // SWG.Europe-Chimaera.auctioner
        // SWG.Europe-Chimaera.interplanetary survey droid
        // SWG.Europe-Chimaera.@installation_n:item_factory
        // SWG.Europe-Chimaera.@player_structure:construction_complete_sender.Europe-Chimaera
        // SWG.Europe-Chimaera.@veteran:new_reward_from

        Matcher match = regexp.matcher(from);

        if (!match.find()) return from;

        String ff = match.group(2);
        if (ff.startsWith("\\#"))
            ff = ff.substring(8);
        else if (ff.startsWith("@money")) {
            int i = ff.indexOf(':');
            ff = ff.substring(i + 1);
        } else if (ff.equalsIgnoreCase("auctioner"))
            ff = "Market";
        else if (ff.equalsIgnoreCase("interplanetary survey droid"))
            ff = "ISD";
        else if (ff.startsWith("@installation_n:")) {
            int i = ff.indexOf(':');
            ff = ff.substring(i + 1);
        } else if (ff.startsWith("@player_structure:"))
            ff = "Structures Inc";
        else if (ff.startsWith("@veteran"))
            ff = "Veteran Union";

        ZString z = new ZString(ff);
        if(match.group(1).length() > 0) {
        	z.app('.').app(match.group(1));
        }
        return z.toString();
    }

    /**
     * Changes font size relatively to the value of <code>change</code>, a value
     * of 0 resets the font size to normal size, otherwise the new font is the
     * product of current font size times <code>change</code>
     * 
     * @param change the multiplier for the new font size, 0 resets font size to
     *        normal
     */
    private void mailBodyChangeFont(double change) {
        Font f = null;
        if (change == 0)
            f = frame.getFont();
        else {
            f = mailBody.getFont();
            int s = Math.max((int) (Math.round(f.getSize() * change)), 6);
            f = new Font(f.getName(), f.getStyle(), s);
        }
        mailBody.setFont(f);
        SWGFrame.getPrefsKeeper().add("mailBodyFontSize",
                new Integer(f.getSize()));
    }

    /**
     * Set the mail body to <code>comp</code>
     * 
     * @param comp the component to set as mail body
     */
    private void mailBodySet(Component comp) {
        mailBodyJSP.getViewport().removeAll();
        mailBodyJSP.getViewport().add(comp);
    }

    /**
     * Returns a menu component for actions related to deleting mails
     * 
     * @return a menu component with an added action listener
     */
    private JMenuItem mailCopyMenuItem() {
        JMenuItem md = new JMenuItem("Copy text");
        md.setToolTipText("Copy selected text");
        md.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_C, InputEvent.CTRL_MASK));
        md.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                mailBody.copy();
            }
        });
        return md;
    }

    /**
     * Deletes one or more selected mails
     * 
     * @param confirm <code>true</code> to show a confirm dialog,
     *        <code>false</code> otherwise
     */
    void mailDelete(boolean confirm) {
        SWGMailBox mb = toon.mailBox();
        SWGMailFolder trash = mb.folder("Trash");
        SWGMailFolder current = mb.folder(folderList.getSelectedValue());

        if (confirm && current.equals(trash)) {
            if (!toon.galaxy().exists()) {
                JOptionPane.showMessageDialog(folderList,
                        "Cannot delete from Trash with no path to SWG",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                    folderList,
                    "Permanently delete selected mails from this computer?",
                    "Confirm", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE)) return;

        } else if (confirm
                && JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                        folderList, "Move selected mails to Trash?",
                        "Confirm", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE)) return;

        int[] selRows = mailList.getSelectedRows();
        int nextMail = selRows[selRows.length - 1] // last index in list
                - selRows.length + 1; // find next mail after deletion

        int errors = 0;
        ZString z = null;
        for (int i : mailsConvertedToModel(selRows)) {
            try {
                mb.delete(messages.get(i), current, trash.equals(current));
            } catch (SecurityException e) {
                if (z == null) z = new ZString().appnl(e.getMessage());
                ++errors;
                if (errors >= 10) break;
            } catch (Throwable e) {
                SWGAide.printError("SWGMailPane:mailDelete", e);
                break;
            }
        }
        if (z != null) SWGAide.printDebug("mlpa", 1,
                "SWGMailPane:mailDelete", z.toString());

        updateMailList(current.getName(), nextMail);
    }

    /**
     * Returns a menu component for actions related to deleting mails
     * 
     * @return a menu component with an added action listener
     */
    private JMenuItem mailDeleteMenuItem() {
        JMenuItem md = new JMenuItem("Delete mails");
        md.setToolTipText("Delete selected mails");
        md.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
        md.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mailDelete(true);
            }
        });
        return md;
    }

    /**
     * Ensure the button-3 mouse click is over a selected row or a selection of
     * rows, otherwise change selection to the row under the mouse click
     * 
     * @param row the row of the view under the mouse click
     */
    private void mailListEnsureSelect(int row) {
        int[] selection = mailList.getSelectedRows();
        if (!mailListRowIsSelected(row, selection)) {
            mailList.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    /**
     * Returns <code>true</code> if <code>row</code> is contained in
     * <code>intArray</code>, <code>false</code> otherwise
     * 
     * @param row the integer to check for
     * @param intArray the array of integers to search
     * @return <code>true</code> if <code>row</code> is found in
     *         <code>intArray</code>, <code>false</code> otherwise
     */
    private boolean mailListRowIsSelected(int row, int[] intArray) {
        for (int i = 0; i < intArray.length; ++i) {
            if (row == intArray[i])
                return true;
        }
        return false;
    }

    /**
     * Returns the lead row, which can be one row of many selected rows, or -1
     * if the lead row is currently not selected. The return value is not
     * converted to the model indexing, important if the view is sorted.
     * 
     * @return the lead row of a selection, or -1 if the lead row is not
     *         selected, not converted to the model
     */
    private int mailListSelectedLeadRow() {
        int[] selection = mailList.getSelectedRows();
        int row = mailList.getSelectedRow();

        if (mailListRowIsSelected(row, selection))
            return row;
        return -1;
    }

    /**
     * Moves the selected mails to a user selected folder. This method interacts
     * with the user for this action to take place.
     */
    private void mailMove() {
        int[] rows = mailList.getSelectedRows();
        String fn = folderList.getSelectedValue();

        if (rows.length <= 0 || fn == null) return;

        SWGMailBox mb = toon.mailBox();
        SWGMailFolder fr = mb.folder(fn);

        String[] flds = toon.mailBox().folderNames(fr.getName());
        String tn = (String) JOptionPane.showInputDialog(folderList,
                "Select target folder", "Move mails",
                JOptionPane.QUESTION_MESSAGE, null, flds, flds[0]);
        if (tn == null) return;

        SWGMailFolder to = mb.folder(tn);

        rows = mailsConvertedToModel(rows);
        for (int i : rows)
            mb.move(messages.get(i), fr, to);

        updateMailList(fr.getName(), Math.max(rows[0] - 1, 0));
    }

    /**
     * Returns a menu component for actions related to moving mails
     * 
     * @return a menu component with an added action listener
     */
    private JMenuItem mailMoveMenuItem() {
        JMenuItem mv = new JMenuItem("Move mails...");
        mv.setToolTipText("Move selected mails to another folder");
        mv.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                mailMove();
            }
        });
        return mv;
    }

    /**
     * Returns the recentmost selected folder of mail messages
     * 
     * @return the recentmost selected folder of mail messages
     */
    List<SWGMailMessage> mails() {
        return messages;
    }

    /**
     * Saves a selected mail to a user selected file target
     */
    private void mailSaveAs() {
        if (messages.isEmpty()) return;

        if (mailList.getSelectedRowCount() > 1) {
            JOptionPane.showMessageDialog(folderList, "Select just one mail",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int row = mailList.convertRowIndexToModel(mailList.getSelectedRow());
        SWGMailMessage mail = toon.mailBox().folder(folderList.getSelectedValue()).mails().get(row);

        if (!mail.exists()) return;

        JFileChooser fc = SWGFrame.getFileChooser();
        int r = fc.showSaveDialog(frame);
        if (r == JFileChooser.APPROVE_OPTION) {
            File target = fc.getSelectedFile();
            if (mail.equalsFile(target)) {
                JOptionPane.showMessageDialog(frame,
                        "Overwrite self is disallowed", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (target.exists()
                    && JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                            frame, "Target exists, overwrite?", "Confirm",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE)) return;

            mail.saveTo(target);
        }
    }

    /**
     * Returns a menu component for actions related to saving mails
     * 
     * @return a menu component with an added action listener
     */
    private JMenuItem mailSaveAsMenuItem() {
        JMenuItem mv = new JMenuItem("Save mail as...");
        mv.setToolTipText("Save selected mail to another file");
        mv.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                mailSaveAs();
            }
        });
        return mv;
    }

    /**
     * Returns an array of indexes of mails selected in the list of mails,
     * converted from view to model indexes, and sorted in ascending order
     * 
     * @param rows array of selected row
     * @return an array of selected indexes converted for the model and sorted
     */
    private int[] mailsConvertedToModel(int[] rows) {
        for (int i = 0; i < rows.length; ++i)
            rows[i] = mailList.convertRowIndexToModel(rows[i]);

        // remove from list backwards not to screw up indexes
        Arrays.sort(rows);
        return rows;
    }

    /**
     * Searches all mails in the mail box of the current toon for a user typed
     * text string
     */
    private void mailSearch() {
        SWGMailSearch search = new SWGMailSearch();
        search.startDialogue(toon.mailBox());

    }

    /**
     * Returns a menu component for actions related to saving mails
     * 
     * @return a menu component with an added action listener
     */
    private JMenuItem mailSearchMenuItem() {
        JMenuItem ms = new JMenuItem("Search mails...");
        ms.setToolTipText("Search this character's folders for mails with text");
        ms.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                mailSearch();
            }
        });
        return ms;
    }

    /**
     * Creates key actions for the mail body font size
     */
    
    private void makeKeyActions() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_MASK),
                "mailFont+Action");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_MASK),
                "mailFont-Action");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_MASK),
                "mailFont0Action");

        this.getActionMap().put("mailFont+Action", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                mailBodyChangeFont(1.2);
            }
        });
        this.getActionMap().put("mailFont-Action", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                mailBodyChangeFont(1 / 1.2);
            }
        });
        this.getActionMap().put("mailFont0Action", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                mailBodyChangeFont(0);
            }
        });
    }

    /**
     * Returns a pane for the mail client folders for the current character
     * 
     * @return a pane for the mail folders
     */
    private JScrollPane makeNFolderPane() {
        folderList = new SWGFolderList();
        JScrollPane folderScroll = new JScrollPane(folderList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        folderScroll.setMinimumSize(new Dimension(70, 50));

        JLabel lab = new JLabel("Folders  ", SwingConstants.CENTER);
        lab.setPreferredSize(new Dimension(50, 20));
        lab.setBorder(BorderFactory.createRaisedBevelBorder());
        folderScroll.setColumnHeaderView(lab);

        folderList.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                actionFolderList(e);
            }
        });

        menuItems.add(new JPopupMenu.Separator());
        JMenuItem folderAdd = folderAddMenuItem();
        menuItems.add(folderAdd);

        JMenuItem folderRename = folderRenameMenuItem();
        menuItems.add(folderRename);

        JMenuItem folderDelete = folderDeleteMenuItem();
        menuItems.add(folderDelete);

        return folderScroll;
    }

    /**
     * Returns the uppermost pane containing the leftmost list folders and the
     * rightmost list (table) of mails for the selected folder
     * 
     * @return the mail clients uppermost pane containing a list of folders and
     *         a list (table) of mails for the selected folder
     */
    
    private Component makeNorthPanel() {
        upperSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        upperSplitPane.setDividerSize(5);

        upperSplitPane.setLeftComponent(makeNFolderPane());

        // The rightmost list of mails for the folder
        mailModel = new SWGMailTableModel();
        mailList = new SWGMailList(mailModel);

        menuItems.add(new JPopupMenu.Separator());

        JMenuItem copy = mailCopyMenuItem();
        menuItems.add(copy);

        JMenuItem mailMove = mailMoveMenuItem();
        menuItems.add(mailMove);

        JMenuItem mailDel = mailDeleteMenuItem();
        menuItems.add(mailDel);

        menuItems.add(new JPopupMenu.Separator());
        JMenuItem mailSearch = mailSearchMenuItem();
        menuItems.add(mailSearch);

        upperSplitPane.setRightComponent(new JScrollPane(mailList));

        return upperSplitPane;
    }

    /**
     * Helper method which creates a menu option for copy and deleting mails.
     * The menu item contains two sub-options: optionally copy mails from SWG to
     * SWGAide's "mails" folder; and optionally delete mails after successful
     * copy. The latter is {@code true} only if the move option is {@code true}.
     * The action listeners update respective members of SWGAide's DAT file.
     * 
     * @return a menu item
     */
    private JMenu makeOptionMailCopy() {
        JMenu opts = new JMenu("Mail options");

        boolean c = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "mailCopyToSWGAide", Boolean.TRUE)).booleanValue();
        boolean d = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "mailDeleteAfterCopy", Boolean.FALSE)).booleanValue();
        
        boolean stripColor = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "mailStripColor", Boolean.FALSE)).booleanValue();

        final JCheckBoxMenuItem del = new JCheckBoxMenuItem("Delete copied", d);
        del.setToolTipText("Delete mails in SWG after successful copy");
        del.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean v = del.isSelected();
                SWGFrame.getPrefsKeeper().add(
                        "mailDeleteAfterCopy", Boolean.valueOf(v));
            }
        });
        del.setEnabled(c);

        final JCheckBoxMenuItem copy = new JCheckBoxMenuItem("Copy mails", c);
        copy.setToolTipText("Copy mails from SWG to SWGAide's mails folder");
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean v = copy.isSelected();
                SWGFrame.getPrefsKeeper().add(
                        "mailCopyToSWGAide", Boolean.valueOf(v));

                if (!v) { // never delete if ~copy
                    SWGFrame.getPrefsKeeper().add(
                            "mailDeleteAfterCopy", Boolean.FALSE);
                    del.setSelected(false);
                }

                del.setEnabled(v);
            }
        });
        
        final JCheckBoxMenuItem strip = new JCheckBoxMenuItem("Strip color codes", stripColor);
        strip.setToolTipText("Strip color codes from mails");
        strip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean v = strip.isSelected();
                SWGFrame.getPrefsKeeper().add(
                        "mailStripColor", Boolean.valueOf(v));
                mailModel.fireTableDataChanged();
                
            }
        });

        opts.add(copy);
        opts.add(del);
        opts.add(strip);

        return opts;
    }

    /**
     * Returns a sub menu for options used by the ISDroid GUI panel
     * 
     * @return the sub menu item for ISDroid options
     */
    private JMenuItem makeOptionsISDroid() {
        JMenu isdroid = new JMenu("ISDroid handling");
        isdroid.setToolTipText("Options for ISDroid mail handling");
        isdroid.setMnemonic('I');

        isdroid.add(makeOptionsISDroidAllowStatless());
        isdroid.add(makeOptionsISDroidContinuous());
        isdroid.add(makeOptionsISDroidDuplicate());
        isdroid.add(makeOptionsISDroidAutodelete());
        isdroid.add(optionsISDroidSingleMenu());
        // isdroid.add(makeOptionsISDroidGUIInput());
        isdroid.add(makeOptionsISDroidHelp());

        return isdroid;
    }

    /**
     * Returns a menu item for ISDroid reporting
     * 
     * @return a menu item for ISDroid reporting
     */
    private JCheckBoxMenuItem makeOptionsISDroidAllowStatless() {
        final JCheckBoxMenuItem asl = new JCheckBoxMenuItem("Allow w/o stats");
        asl.setToolTipText("Allow uploads to SWGCraft without stats");
        asl.setMnemonic('A');

        asl.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidAllowStatless", Boolean.TRUE)).booleanValue());

        asl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("ISDroidAllowStatless",
                        Boolean.valueOf(asl.isSelected()));
            }
        });
        return asl;
    }

    /**
     * Returns a menu item for ISDroid reporting
     * 
     * @return a menu item for ISDroid reporting
     */
    private JMenuItem makeOptionsISDroidAutodelete() {
        final JCheckBoxMenuItem adel = new JCheckBoxMenuItem("Auto-delete");
        adel.setToolTipText("Delete content of \"res.txt\" after upload");
        adel.setMnemonic('D');

        adel.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidFileAutoDelete", Boolean.TRUE)).booleanValue());

        adel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("ISDroidFileAutoDelete",
                        Boolean.valueOf(adel.isSelected()));
            }
        });
        return adel;
    }

    /**
     * Returns a menu item for continuous resource list in "res.txt"
     * 
     * @return a menu item for continuous resource list in "res.txt"
     */
    private JMenuItem makeOptionsISDroidContinuous() {
        final JCheckBoxMenuItem cont = new JCheckBoxMenuItem("Long list");
        cont.setToolTipText("Write \"res.txt\" as long, continuous list");
        cont.setMnemonic('L');

        cont.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidFileContinuous", Boolean.TRUE)).booleanValue());

        cont.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("ISDroidFileContinuous",
                        Boolean.valueOf(cont.isSelected()));
            }
        });
        return cont;
    }

    /**
     * Returns a menu item for ISDroid reporting
     * 
     * @return a menu item for ISDroid reporting
     */
    private JMenuItem makeOptionsISDroidDuplicate() {
        final JCheckBoxMenuItem sdup = new JCheckBoxMenuItem("Skip duplicates");
        sdup.setToolTipText("Skip duplicate resource entries in \"res.txt\" ");
        sdup.setMnemonic('K');

        sdup.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidFileSkipDuplicates", Boolean.FALSE)).booleanValue());

        sdup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("ISDroidFileSkipDuplicates",
                        Boolean.valueOf(sdup.isSelected()));
            }
        });
        return sdup;
    }

    // /**
    // * Returns a menu item for ISDroid reporting
    // *
    // * @return a menu item for ISDroid reporting
    // */
    // private JMenuItem makeOptionsISDroidGUIInput() {
    // final JCheckBoxMenuItem gui = new JCheckBoxMenuItem("GUI input");
    // gui.setToolTipText("Use a GUI input field rather than the notes file");
    // gui.setMnemonic('G');
    //
    // gui.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
    // "ISDroidUseGUIPanel", Boolean.FALSE)).booleanValue());
    //
    // gui.addActionListener(new ActionListener() {
    // public void actionPerformed(ActionEvent e) {
    // // if (gui.isSelected())
    // // JOptionPane.showMessageDialog(optionsMenu,
    // // "Yet not implemented", "TODO",
    // // JOptionPane.INFORMATION_MESSAGE);
    // // getPrefsKeeper().add("ISDroidUseGUIPanel",
    // // Boolean.valueOf(gui.isSelected()));
    // }
    // });
    // gui.setEnabled(false);
    // return gui;
    // }

    /**
     * Returns a menu item for ISDroid reporting
     * 
     * @return a menu item for ISDroid reporting
     */
    private JMenuItem makeOptionsISDroidHelp() {
        final JCheckBoxMenuItem help = new JCheckBoxMenuItem("Help text");
        help.setToolTipText("Always append help text to \"res.txt\"");
        help.setMnemonic('H');

        help.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidFileHelpText", Boolean.TRUE)).booleanValue());

        help.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("ISDroidFileHelpText",
                        Boolean.valueOf(help.isSelected()));
            }
        });
        return help;
    }

    /**
     * Returns the lower pane containing a mail header and the mail's text body
     * for the selceted mail
     * 
     * @return the mail clients lower pane containing a mail header and its body
     *         for the selceted mail
     */
    private Component makeSouthPanel() {
        JPanel lower = new JPanel();
        lower.setLayout(new BorderLayout());

        mailHeader = new SWGMailHeader();
        lower.add(mailHeader, BorderLayout.PAGE_START);

        mailBody = new SWGMailBody();

        Font f = mailBody.getFont();
        int s = ((Integer) SWGFrame.getPrefsKeeper().get(
                "mailBodyFontSize", new Integer(f.getSize()))).intValue();
        mailBody.setFont(new Font(f.getName(), f.getStyle(), s));

        mailBodyJSP = new JScrollPane(mailBody);
        lower.add(mailBodyJSP, BorderLayout.CENTER);

        return lower;
    }

    /**
     * Creates and returns a menu item for ISDroid reporting.
     * 
     * @return a menu item
     */
    private JMenuItem optionsISDroidSingleMenu() {
        final JCheckBoxMenuItem single = new JCheckBoxMenuItem("Single report");
        single.setToolTipText("Process reports one by one at selection");
        single.setMnemonic('S');

        single.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidSingleSelection", Boolean.FALSE)).booleanValue());

        single.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                SWGFrame.getPrefsKeeper().add("ISDroidSingleSelection",
                        Boolean.valueOf(single.isSelected()));
                frame.getMainTab().getMailClient().updateMailBody();
            }
        });
        return single;
    }

    /**
     * Called upon from actions on the "use ISDroid panel" check box or whenever
     * a folder is selected. This method sets the appropriate component to the
     * mail body area, a text area or the ISDroid panel.
     */
    private void selectedISDroid() {
        if (folderList.getSelectedValue().equals("ISDroid")
                && showISDroidPanel.isSelected()) {

            if (ISDroidPanel == null)
                ISDroidPanel = new SWGMailISDroidPanel(frame, this);

            if (mailBodyJSP.getViewport().getComponent(0) != ISDroidPanel)
                mailBodySet(ISDroidPanel);

            ISDroidPanel.focusGained(true);

        } else if (mailBodyJSP.getViewport().getComponent(0) != mailBody) {
            mailBodySet(mailBody);
            if (ISDroidPanel != null) ISDroidPanel.focusGained(false);
        }

        SWGFrame.getPrefsKeeper().add("mailISDroidPanel",
                Boolean.valueOf(showISDroidPanel.isSelected()));
    }

    /**
     * Sets the newly selected character as owner and calls for the mail client
     * to be updated accordingly
     * 
     * @param character the character for the mail client
     */
    public void setCharacter(SWGCharacter character) {
        if (character == null)
            focusGained(false);
        else {
            if (character == toon) return;

            toon = character;
            updateClient();
        }
    }

    /**
     * Sets the menu items for mails to enabled or disabled
     * 
     * @param enabled <code>true</code> to enable the appropriate menu items,
     *        <code>false</code> to siable them
     */
    private void setMailGUI(boolean enabled) {
        boolean b = !messages.isEmpty() && toon.galaxy().exists();

        for (JComponent cmp : menuItems) {
            if (cmp instanceof JMenuItem) {
                JMenuItem jmi = (JMenuItem) cmp;
                String txt = jmi.getText().toLowerCase(Locale.ENGLISH);
                if (txt.startsWith("search"))
                    jmi.setEnabled(b && enabled);
                else if (txt.startsWith("copy") || txt.contains("mail"))
                    jmi.setEnabled(enabled);
            }
        }
        if (b && enabled)
            frame.saveAsAddListener(mailSaveAs, "Save selected mail as...");
        else
            frame.saveAsRemoveListener(mailSaveAs);
    }

    /**
     * Updates the mail client with the information bound to the most recently
     * selected character. This method updates the list of folders and selects
     * the "Inbox" which consecutively will update the other panes.
     */
    private void updateClient() {
        String[] folders = toon.mailBox().folderNames(null);
        folderList.setListData(folders);
        folderList.setSelectedIndex(0);
    }

    /**
     * Updates the mail header and body to the recentmost selected mail
     */
    void updateMailBody() {
        int row = mailListSelectedLeadRow();
        SWGMailMessage msg = null;
        if (row >= 0 && messages.size() > 0) {
            row = mailList.getSelectionModel().getLeadSelectionIndex();
            row = Math.min(row, messages.size() - 1);
            row = row >= 0
                    ? row
                    : 0;
            row = mailList.convertRowIndexToModel(row);

            msg = messages.get(row);
            mailHeader.date.setText(getDateString(msg.date()));
            mailHeader.filename.setText(msg.getName());
            
            boolean stripColor = ((Boolean) SWGFrame.getPrefsKeeper().get(
                    "mailStripColor", Boolean.FALSE)).booleanValue();
            if(stripColor) {
                mailHeader.from.setText(SWGGuiUtils.stripColorCodes(getFromString(msg.fromLine())));
                mailHeader.subject.setText(SWGGuiUtils.stripColorCodes(msg.subject()));
                mailBody.setText(SWGGuiUtils.stripColorCodes(msg.bodyText()));
            } else {
                mailHeader.from.setText(getFromString(msg.fromLine()));
                mailHeader.subject.setText(msg.subject());
                mailBody.setText(msg.bodyText());
            }

            mailBody.setCaretPosition(0);
        } else {
            mailHeader.from.setText("");
            mailHeader.date.setText("");
            mailHeader.filename.setText("");
            mailHeader.subject.setText("");
            mailBody.setText("");
        }
        if (ISDroidPanel != null && showISDroidPanel.isSelected()
                && folderList.getSelectedValue().equals("ISDroid")) {
            // row < 0 will clear the panel's content
            ISDroidPanel.actionHandleISDroidReport(msg, row);
        }
    }

    /**
     * Updates the list of mails with the contents of the recentmost selected
     * folder
     * 
     * @param folderName the name of the selected folder
     * @param row the row for where to start, 0 or the next mail to display
     */
    private void updateMailList(String folderName, int row) {
        if (folderName == null) return;

        toon.mailBox().fetch();

        SWGMailFolder folder = toon.mailBox().folder(folderName);
        messages = folder.mails();

        frame.putToLogbar_2(messages.size() + " mails  ");

        mailModel.fireTableDataChanged();

        if (messages.size() <= 0) {
            setMailGUI(false);
            mailBody.setText("To view mails you must first execute\n" +
                    "the in-game command /mailsave");
            if (folderName.equals("ISDroid"))
                ISDroidPanel.actionHandleISDroidReport(null, -1);
            return;
        }
        setMailGUI(true);

        int r = Math.min(row, mailList.getModel().getRowCount() - 1);
        mailList.getSelectionModel().setSelectionInterval(r, r);
    }

    public boolean validateText(String name) {
        String[] folders = toon.mailBox().folderNames(null);
        for (String n : folders)
            if (name.equalsIgnoreCase(n)) return false;

        return true;
    }

    /**
     * A class for rendering dates in this GUI component
     * 
     * @author Simon Gronlund <a href="mailto:simongronlund@gmail.com">Simon
     *         Gronlund</a> aka Chimaera.Zimoon
     */
    private final static class DateRenderer extends DefaultTableCellRenderer {

        /**
         * The date formatter
         */
        private final DateFormat dfss;

        /**
         * Creates an object of this class
         */
        DateRenderer() {
            super();
            dfss = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT);
        }

        @Override
        public void setValue(Object value) {
            setText(dfss.format(value));
        }
    }

    /**
     * A component that contains a list of the mail box' folder names and
     * listens for user selections
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGFolderList extends JList<String> {

        /**
         * Creates a plain list for folder names with a selection listener
         */
        SWGFolderList() {
            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.setLayoutOrientation(JList.VERTICAL);

            this.addListSelectionListener(new ListSelectionListener() {
                
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        String s = folderList.getSelectedValue();
                        folderListSelected(s);
                    }
                }
            });
        }
    }

    /**
     * A component that displays the text body of a mail
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGMailBody extends JTextArea {

        // class SWGMailBody extends JEditorPane {

        /**
         * Creates a GUI component for display of mail text bodies
         */
        SWGMailBody() {
            this.setLineWrap(true);
            this.setWrapStyleWord(true);
            this.setEditable(false);

            this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            this.addMouseListener(new MouseAdapter() {
                
                @Override
                public void mouseClicked(MouseEvent e) {
                    actionMailBody(e);
                }
            });
        }
    }

    /**
     * A component for display of the header of a mail. This component contains
     * other GUI components for the display.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGMailHeader extends JPanel {

        /**
         * The date the mail was sent
         */
        JLabel date;

        /**
         * The file name of the mail
         */
        JLabel filename;

        /**
         * The name of the mail's sender
         */
        JLabel from;

        /**
         * The subject line of the mail
         */
        JLabel subject;

        /**
         * Creates a GUI conponent for display of a mails header details
         */
        SWGMailHeader() {
            BoxLayout mBox = new BoxLayout(this, BoxLayout.Y_AXIS);
            this.setLayout(mBox);
            this.setBorder(BorderFactory.createRaisedBevelBorder());

            this.add(upperRow());
            this.add(lowerRow());
        }

        /**
         * Returns the lower component of the header panel
         * 
         * @return the lower component
         */
        
        private Component lowerRow() {
            JPanel p = new JPanel();
            BoxLayout bl = new BoxLayout(p, BoxLayout.X_AXIS);
            p.setLayout(bl);

            p.add(new JLabel("Subject:   "));
            subject = new JLabel();
            p.add(subject);

            p.add(Box.createHorizontalGlue());

            showISDroidPanel = new JCheckBox("ISDroidPanel");
            showISDroidPanel.setToolTipText(
                    "Display ISDroid Panel, not mails view");
            showISDroidPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectedISDroid();
                }
            });
            boolean b = ((Boolean) SWGFrame.getPrefsKeeper().get(
                    "mailISDroidPanel", Boolean.TRUE)).booleanValue();
            showISDroidPanel.setSelected(b);
            showISDroidPanel.setEnabled(false);
            p.add(showISDroidPanel);

            return p;
        }

        /**
         * Returns the upper component of the header panel
         * 
         * @return the upper component
         */
        private Component upperRow() {
            JPanel p = new JPanel();
            BoxLayout b = new BoxLayout(p, BoxLayout.X_AXIS);
            p.setLayout(b);

            p.add(new JLabel("From:   "));
            from = new JLabel();
            p.add(from);

            p.add(Box.createHorizontalGlue());

            p.add(new JLabel("Date:   "));
            date = new JLabel();
            p.add(date);

            p.add(new JLabel("      File:   "));
            filename = new JLabel();
            p.add(filename);

            return p;
        }
    }

    /**
     * A component that displays a list, or rather a table, of mails for the
     * selected folder
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    final class SWGMailList extends SWGJTable {

        /**
         * Creates a table for the list of mails and adds selection listeners to
         * it
         * 
         * @param model the model for this table of list of mails
         */
        private SWGMailList(SWGMailTableModel model) {
            super(model);

            int mil = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
            this.setSelectionMode(mil);

            this.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
            this.setAutoCreateRowSorter(true);

            this.setDefaultRenderer(Date.class, new DateRenderer());

            for (int i = 2; i < this.getColumnCount(); ++i) {
                TableColumn col = this.getColumnModel().getColumn(i);
                if (i >= 4)
                    SWGGuiUtils.tableColumnSetWidth(col, 22, 22, 30);
                else {
                    Integer is = (Integer) SWGFrame.getPrefsKeeper().get(
                                    "mailClientColumnSize" + i);
                    int s = is == null
                                ? 115
                                : is.intValue();
                    SWGGuiUtils.tableColumnSetWidth(col, 50, s, 130);
                }
            }
            this.getSelectionModel().addListSelectionListener(
                    new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            if (!e.getValueIsAdjusting()) {
                                updateMailBody();
                            }
                        }
                    });
            this.addMouseListener(new MouseAdapter() {
                
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        int row = rowAtPoint(e.getPoint());
                        if (row >= 0) actionMailList(e, row);
                    }
                }
            });
        }
    }

    /**
     * This class is used by the mail client GUI to search mails with certain
     * content
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGMailSearch implements TextValidation {

        /**
         * A list of the mails meeting the search criteria
         */
        List<SWGMailMessage> maillist;

        /**
         * Creates a search engine that will search for <code>findText</code> in
         * all mails contained in <code>mailBox</code>
         * 
         * @see #startDialogue(SWGMailBox)
         */
        SWGMailSearch() {
            this.maillist = new ArrayList<SWGMailMessage>(255);
        }

        /**
         * Marshals the given mail box to find <code>str</code> in any of its
         * contained mails
         * 
         * @param mailBox the mail box to marshal through
         * @param str the text string to search the mails for
         */
        private void marshalMailBox(SWGMailBox mailBox, String str) {
            for (SWGMailFolder fld : mailBox.folders()) {
                for (SWGMailMessage mail : fld.mails())
                    if (mail.accept(str)) maillist.add(mail);

                if (!maillist.isEmpty()) {
                    Collections.sort(maillist);
                    ((SWGMailTableModel) mailList.getModel()).
                            fireTableDataChanged();
                }
            }
        }

        /**
         * Starts scanning <code>mailBox</code>
         * 
         * @param mailBox the mail box to scan
         */
        
        void startDialogue(SWGMailBox mailBox) {
            if (mailBox == null) return;

            SWGTextInputDialogue diag = new SWGTextInputDialogue(frame, this,
                    folderList.getLocationOnScreen(), "Search",
                    "Search all mails for this character", "Search for:");
            diag.setVisible(true);
            String txt = diag.getTypedText();

            if (txt == null) return;

            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            List<SWGMailMessage> old = messages;
            messages = maillist;

            ((SWGMailTableModel) mailList.getModel()).fireTableDataChanged();

            marshalMailBox(mailBox, txt);

            frame.setCursor(null);
            if (maillist.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        String.format("No mails with \"%s\" found", txt),
                        "No result", JOptionPane.PLAIN_MESSAGE);

                messages = old;
                ((SWGMailTableModel) mailList.getModel()).
                        fireTableDataChanged();
            }
        }

        public boolean validateText(String name) {
            // accept any search term
            return true;
        }
    }

    /**
     * The table model that supports the list of mails
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private final class SWGMailTableModel extends DefaultTableModel {

        /**
         * Table column header titles
         */
        private final Object[] columnNames =
            { "Subject", "From", "Date", "Filename", "ISD", "VM" };

        /**
         * Creates a table model for the mail list
         */
        SWGMailTableModel() {
            super.setColumnIdentifiers(columnNames);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 2 ? Date.class : String.class;

            // columnIndex == 2
            // ? Date.class
            // :
                        //String.class;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        
        @Override
        public int getRowCount() {
            return messages == null
                    ? 0
                    : messages.size();
        }

        
        @Override
        public Object getValueAt(int row, int column) {
            if (messages == null) return "";
            SWGMailMessage msg = messages.get(row);
            boolean stripColor = ((Boolean) SWGFrame.getPrefsKeeper().get(
                        "mailStripColor", Boolean.FALSE)).booleanValue();
            switch (column) {
            case 0:
                if(stripColor) {
                    return SWGGuiUtils.stripColorCodes(msg.subject());
                }
                return msg.subject();
            case 1:
                if(stripColor) {
                    return SWGGuiUtils.stripColorCodes(getFromString(msg.fromLine()));
                }
                return getFromString(msg.fromLine());
            case 2:
                return msg.date()*1000;
            case 3:
                return msg.getName();
            case 4:
                return msg.type() == Type.ISDroid
                        ? "  X"
                        : "";
            case (5):
                return msg.type() == Type.Auction
                        ? "  X"
                        : "";
            default:
                return "ERROR";
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
