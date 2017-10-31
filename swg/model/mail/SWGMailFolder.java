package swg.model.mail;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import swg.SWGAide;
import swg.gui.common.SWGGui;
import swg.model.SWGCGalaxy;
import swg.model.SWGCharacter;
import swg.model.mail.SWGMailMessage.Type;
import swg.tools.ZString;

/**
 * This type models a plain mail folder for a mail client.
 * <p>
 * This type is serializable, however, a mail-box determines whether to store.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGMailFolder
        implements Comparable<SWGMailFolder>, Serializable, SWGGui {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -4923003421490979054L;

    /**
     * A flag that denotes if this folder is one of the default folder types or
     * not. A default folder must not be erased from a mail-box.
     */
    private final boolean isDefault;

    /**
     * A list of mails contained by this folder.
     */
    private final List<SWGMailMessage> mailList;

    /**
     * The name of this folder.
     */
    private String name;

    /**
     * The mailbox that contains this folder.
     */
    private final SWGMailBox owner;

    /**
     * Creates a mail folder for the specified values. This implementation does
     * not add the folder to the box. If the instance is for temporary use the
     * box may be {@code null}, but if the purpose is to add the instance to a
     * mail-box the value must equal the box.
     * 
     * @param folderName a name
     * @param box the mailbox for this folder
     * @throws IllegalArgumentException if the name is empty
     * @throws NullPointerException if the name is {@code null}
     */
    public SWGMailFolder(String folderName, SWGMailBox box) {
        this(folderName, box, false);
    }

    /**
     * Creates a mail folder. The boolean value determines if the instance is a
     * default folder for the box or not. Only the box itself should invoke this
     * constructor.
     * 
     * @param folderName a name
     * @param box the mailbox for this folder
     * @param isDefault {@code true} if this is a default folder for the box
     * @throws IllegalArgumentException if the name is empty
     * @throws NullPointerException if the name is {@code null}
     */
    SWGMailFolder(String folderName, SWGMailBox box, boolean isDefault) {
        if (folderName.trim().isEmpty())
            throw new IllegalArgumentException("Invalid name");

        this.name = folderName;
        this.owner = box;
        this.isDefault = isDefault;

        mailList = new ArrayList<SWGMailMessage>();
    }

    /**
     * Adds the specified mail to this folder. If this folder already contains
     * the mail, or if it {@code null}, this method does nothing.
     * 
     * @param mail a mail
     */
    public void add(SWGMailMessage mail) {
        if (mail != null && !mailList.contains(mail)) mailList.add(mail);
    }

    /**
     * Helper method which adds the mail without validation. This method must
     * only be used by the mail-box.
     * 
     * @param m a mail
     */
    void addInternal(SWGMailMessage m) {
        mailList.add(m);
    }

    /**
     * Archives all mails of this folder to within the specified path.
     * <p>
     * <b>Notice:</b> this method must only be invoked if this is an auction
     * folder. All mails are removed from this folder and existing mails are
     * moved to folders on the form {@code mailbox.swgAidePath()\Auction\YEAR\}
     * where year is parsed from the mail. Mails that do not exist are simply
     * removed from this folder; however, archived mails may be reinstated if
     * found at another computer.
     * <p>
     * If the destination file exists this method just deletes the source file
     * and does not replace the target. If there is an error it is caught and
     * written to SWGAide's log file.
     * 
     * @param p the base destination path, without YEAR
     */
    public void auctionArchive(File p) {
        try {
            Calendar cal = new GregorianCalendar();
            for (Iterator<SWGMailMessage> iter = mailList.iterator(); iter.hasNext();) {
                SWGMailMessage mail = iter.next();
                iter.remove();

                if (!mail.exists()) continue;

                cal.setTime(new Date(mail.date() * 1000));
                File yf = new File(p, String.valueOf(cal.get(Calendar.YEAR)));
                if (!yf.exists()) yf.mkdir();

                File nf = new File(yf, mail.getName());
                if (nf.exists())
                    mail.file().delete();
                else
                    mail.file().renameTo(nf);
            }
        } catch (Throwable e) {
            SWGAide.printDebug("mfld", 1, "SWGMailFolder:auctionArchive",
                    e.getClass().toString(), e.getMessage());
            JOptionPane.showMessageDialog(SWGAide.frame(),
                    "Error archiving trade mails", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Restores all the specified files to this folder.
     * <p>
     * <b>Notice:</b> this method must only be invoked if this is an auction
     * folder. If the file is determined to be an auction mail it is moved, a
     * mail and auction-data is created and the mail is added to this folder,
     * otherwise the file is silently left behind. If there is an error it is
     * caught and written to SWGAide's log file.
     * 
     * @param mails an array of mails to restore
     */
    public void auctionArchiveRestore(File[] mails) {
        if (mails == null || mails.length <= 0) return;

        SWGCharacter ch = owner.owner;

        File ff = owner.swgAidePath();
        SWGCGalaxy gxy = ch.gxy();

        ZString z = null;
        for (File f : mails) {
            try {
                SWGMailMessage tmp = SWGMailBox.newMail(f, ch);
                if (tmp.owner().gxy() != gxy
                        || tmp.type() != Type.Auction) continue;

                File fn = new File(ff, tmp.getName());
                tmp.file().renameTo(fn);

                SWGMailMessage msg = SWGMailBox.newMail(fn, ch);

                this.add(msg);

            } catch (Exception e) {
                if (z == null) z = new ZString().nl();
                z.appnl(e.getMessage());
            }
        }
        if (z != null) SWGAide.printDebug("mfld", 1,
                "SWGMailFolder:auctionArchiveRestore:", z.toString());
    }

    /**
     * Returns the mailbox that contains this folder.
     * 
     * @return a mailbox
     */
    public SWGMailBox box() {
        return owner;
    }
    
    /**
     * Removes all mails from this folder as defined by {@link List#clear()}.
     * This method must only be invoked by the owner of this folder.
     */
    void clear() {
        mailList.clear();
    }

    public int compareTo(SWGMailFolder other) {
        if (this.name.equals(other.getName())) return 0;
        if (this.isDefault || other.isDefault) {
            // ordered by usability
            if (this.name.equalsIgnoreCase("inbox")) return -1;
            if (other.getName().equalsIgnoreCase("inbox")) return 1;

            if (this.name.equalsIgnoreCase("sent")) return -1;
            if (other.getName().equalsIgnoreCase("sent")) return 1;

            if (this.name.equalsIgnoreCase("auction")) return -1;
            if (other.getName().equalsIgnoreCase("auction")) return 1;

            if (this.name.equalsIgnoreCase("isdroid")) return -1;
            if (other.getName().equalsIgnoreCase("isdroid")) return 1;

            // Trash placed last
            if (this.name.equalsIgnoreCase("trash")) return 1;
            if (other.getName().equalsIgnoreCase("trash")) return -1;
        }
        return this.name.compareTo(other.getName());
    }

    /**
     * Returns {@code true} if this folder contains {@code mail}.
     * 
     * @param mail the mail to check for
     * @return {@code true} as defined by {@link List#contains(Object)}
     */
    public boolean contains(SWGMailMessage mail) {
        return mailList.contains(mail);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof SWGMailFolder
                        && name.equals(((SWGMailFolder) obj).name)
                        && owner == ((SWGMailFolder) obj).owner);
    }

    /**
     * Returns the mail for the specified index.
     * 
     * @param index an index in the underlying list
     * @return a mail for the specified index
     * @throws IndexOutOfBoundsException if the argument is invalid
     */
    public SWGMailMessage get(int index) {
        return mailList.get(index);
    }

    @Override
    public String getDescription() {
        return String.format("%s in %s", name, owner);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return (17 * name.hashCode()) + owner.hashCode();
    }

    /**
     * Returns {@code true} if this folder is a default folder that cannot be
     * removed from its mail box.
     * 
     * @return {@code true} if this is a default folder
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns {@code true} if this folder is empty.
     * 
     * @return {@code true} as defined by {@link List#isEmpty()}
     */
    public boolean isEmpty() {
        return mailList.isEmpty();
    }

    /**
     * Returns a sorted copy of the list of the mails contained by this folder.
     * 
     * @return a sorted list of mails
     */
    public List<SWGMailMessage> mails() {
        Collections.sort(mailList);
        return new ArrayList<SWGMailMessage>(mailList);
    }

    /**
     * Helper method which returns the list of mails as-is. The returned list is
     * <b>read-only</b>.
     * 
     * @return a list of mails
     */
    List<SWGMailMessage> mailsInternal() {
        return mailList;
    }

    /**
     * Resolves this instance at deserialization and possibly updates member
     * fields.
     * 
     * @return this
     */
    private Object readResolve() {
        return this;
    }

    /**
     * Removes a mail at the specified index without modifying the local file
     * system. This method is useful only when moving a mail between folders
     * because stray mails are soon re-entered to the mail-box. If the index is
     * out of bounds this method does nothing.
     * 
     * @param index an index
     */
    public void remove(int index) {
        if (index >= 0 && index < mailList.size()) mailList.remove(index);
    }

    /**
     * Removes the specified mail from this folder without modifying the local
     * file system. This method is useful only when moving a mail between
     * folders because stray mails are soon re-entered to the mail-box. If the
     * mail does not exist this folder is not modified.
     * 
     * @param mail a mail to remove
     */
    public void remove(SWGMailMessage mail) {
        mailList.remove(mail);
    }

    /**
     * Renames this mail folder to the specified name. If this is a default
     * folder this method does nothing.
     * 
     * @param newName a new name
     */
    public void setName(String newName) {
        if (!isDefault) name = newName;
    }

    /**
     * Returns the number of mails contained by this folder.
     * 
     * @return number of mails, as defined by {@link List#size()}
     */
    public int size() {
        return mailList.size();
    }

    @Override
    public String toString() {
        return String.format("SWGMailFolder[%s.%s]{ %s }",
                owner, name, mailList.toString());
    }
}
