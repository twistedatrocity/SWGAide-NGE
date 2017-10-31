package swg.model.mail;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;
import swg.model.mail.SWGMailMessage.Type;
import swg.tools.ZString;
import swg.tools.ZWriter;

/**
 * This type models the core of a basic mail client. Mails are sorted on
 * different folders, either by type or the whim of the user. Each character in
 * the SWG universe has its own mail-box of this type. Remember that mails are
 * saved by SWG at each /mailsave command as long as they are read within the
 * in-game nail client.
 * <p>
 * November 2010 SWGAide was reworked to become host independent. At that point
 * the mail handling was also reworked. Host independency opens up for several
 * use cases which affects mails; a user may have SWGAide at a movable memory
 * stick and keep mails and stuff fully contained with SWGAide no matter the
 * host and are fine with mails moved into the "mails" folder, another user may
 * want mails to stay where they are but also have them contained with SWGAide,
 * and yet another user does not care much about mails but reads only those that
 * are present at the current system. SWGAide now provides support for these use
 * cases: optionally copy mails to the "mails" folder but never move, and
 * another option to delete mails from the current host after being copied
 * &mdash; if the host permits that for SWGAide.
 * <p>
 * Also, SWGAide no longer modifies the file suffix of mails. Hence all mail
 * files are retained as is, all on one folder. Furthermore, this type no longer
 * saves the content of all folders to SWGAide's DAT file but only auction mails
 * (to always support the trade panels), trash mails (to retain information on
 * what the user has trashed), and custom mail folders (because this info is
 * otherwise lost). At demand SWGAide refreshes a mail-box and then all mails
 * that are not already known are sorted into specialized folders or the inbox
 * respectively.
 * <p>
 * Sensitive methods are synchronized on the smallest possible scope, most often
 * this is the list of folders.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGMailBox implements Serializable {

    /**
     * A dummy file instance for non-existing paths. Its exists() method is
     * overridden to always return false.
     */
    @SuppressWarnings("serial")
    static final File DUMMY_FILE = new File("") {
        @Override
        public boolean exists() {
            return false;
        }
    };

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 828152435029499770L;

    /**
     * A temporary, helper map of known mails.
     */
    private transient Map<String, SWGMailMessage> allMails;

    /**
     * The file path for this mailbox within SWGAide. This path is on the form
     * "mails\Station\Galaxy\Character" as opposed to a path within the SWG
     * "profiles" folder.
     */
    private final File boxPath;

    /**
     * A folder for Auction mails, a default folder that must not be deleted.
     */
    private final SWGMailFolder folderAuction;

    /**
     * A folder for the Inbox, a default folder that must not be deleted.
     */
    private final SWGMailFolder folderInbox;

    /**
     * A folder for ISDroids, a default folder that must not be deleted.
     */
    private final SWGMailFolder folderISDroid;

    /**
     * A list of mail folders for this mail-box.
     */
    private final List<SWGMailFolder> folders;

    /**
     * A folder for Sent mails, a default folder that must not be deleted. Mails
     * that are CC'd to oneself (to the owner of this mail-box) wind up here.
     */
    private final SWGMailFolder folderSent;

    /**
     * The Trash folder, a default folder that must not be deleted.
     */
    private final SWGMailFolder folderTrash;

    /**
     * A flag that denotes if {@link #fetchSWGAide()} has run at least once for
     * this mail-box during the session. The method {@link #fetch()} must not
     * run before this box is refreshed because this box must know of mails that
     * exist within SWGAide's "mails" folder.
     */
    private transient boolean isRefreshed;

    /**
     * The character this mailbox pertains to.
     */
    final SWGCharacter owner;

    /**
     * The absolute path to SWG's mail folder for this mail-box. This is a path
     * to the "profiles" folder at the current file system at the form {@code
     * pathTo\SWG\profiles\Station\Galaxy\mail_Character_Name}. If SWGAide is
     * launched without a valid path to SWG this value is {@code null} or
     * undefined.
     */
    private transient File swgDirPath;

    /**
     * Creates a new instance of this type for the specified character. This
     * constructor is only invoked from the constructor of SWGCharacter. This
     * implementation creates its default mail folders and its mail path within
     * SWGAide.
     * 
     * @param character the owner of this mailbox
     */
    public SWGMailBox(SWGCharacter character) {
        owner = character;

        ArrayList<SWGMailFolder> fl = new ArrayList<SWGMailFolder>();

        folderInbox = new SWGMailFolder("Inbox", this, true);
        fl.add(folderInbox);

        folderSent = new SWGMailFolder("Sent", this, true);
        fl.add(folderSent);

        folderTrash = new SWGMailFolder("Trash", this, true);
        fl.add(folderTrash);

        folderAuction = new SWGMailFolder("Auction", this, true);
        fl.add(folderAuction);

        folderISDroid = new SWGMailFolder("ISDroid", this, true);
        fl.add(folderISDroid);

        folders = fl;

        SWGGalaxy g = owner.galaxy();
        File f = new File(String.format("mails/%s/%s/%s", g.station().getName(),
                        g.gxy().getNameComplete(), owner.getNameComplete()));
        f.mkdirs(); // mails\Station\Galaxy\Character FamilyName
        boxPath = f;
    }

    /**
     * Helper method which returns a mail for the specified name, or {@code
     * null}. Notice that this map contains also mails that the user deletes
     * from the system during the session.
     * <p>
     * This method is used by {@link #fetch()} and {@link #fetchSWGAide()} when
     * scanning hundreds of files. The map of mails are retained for the session
     * and thus the mentioned methods must update the map when appropriate.
     * 
     * @param mailName a mail name as obtained from mail.getName()
     * @return a mail, or {@code null}
     */
    private SWGMailMessage contains(String mailName) {
        if (allMails == null) {
            int size = 4711;
            for (SWGMailFolder fl : folders())
                size += fl.size();
            allMails = new HashMap<String, SWGMailMessage>((size << 1) + 1);

            for (SWGMailFolder fl : folders())
                for (SWGMailMessage m : fl.mailsInternal())
                    allMails.put(m.getName(), m);
        }
        return allMails.get(mailName);
    }

    /**
     * Deletes the specified mail from the specified folder. In particular:
     * <p>
     * If the folder is the Trash folder the boolean argument is ignored and
     * this method tries to delete the mail from the current file system and if
     * successful the mail is removed from the Trash folder.
     * <p>
     * Otherwise, if the folder is not the Trash folder, this method always
     * moves the mail to the Trash folder. Then, if the boolean argument is
     * {@code true} this method tries to delete the mail from the current file
     * system and if successful the mail is removed from the Trash folder.
     * <p>
     * This mean that if there is an error the mail is retained in the Trash
     * folder so that SWGAide will not pick it up again at next refresh.
     * <p>
     * 
     * @param m a mail
     * @param f a folder to remove from
     * @param erase {@code true} to erase the mail from file system
     * @throws IllegalArgumentException if the folder is unknown
     * @throws NullPointerException if an argument is {@code null}
     * @throws SecurityException if there is an error
     */
    public void delete(SWGMailMessage m, SWGMailFolder f, boolean erase)
            throws SecurityException {

        synchronized (folders) {
            if (f.equals(folderTrash)) {
                m.fileDelete();
                f.remove(m);
            } else if (folders().contains(f)) {
                if (f.contains(m)) {
                    folderTrash.add(m);
                    f.remove(m);
                    if (erase) {
                        m.fileDelete();
                        folderTrash.remove(m);
                    }
                }
            } else
                throw new IllegalArgumentException("Folder not in this mailbox");
        }
    }

    /**
     * A callback that is invoked by the exit routine before SWGAide exits. This
     * method clears the Inbox, ISDroid, and the Sent folder. These folders are
     * re-populated at next start and if SWGAide is moved to another file system
     * without these files they won't show up.
     */
    public void doExit() {
        folderInbox.clear();
        folderISDroid.clear();
        folderSent.clear();
    }

    /**
     * Fetch mails from the mail folder within SWG's profiles folder, at the
     * current file system. If the user option to copy mails to SWGAide is
     * selected all unknown mails are copied (unknown rather than new in the
     * case the user launch SWGAide at a computer with old but unknown mails).
     * If the user option to delete mails in SWG after copy is selected SWGAide
     * tries to delete the files. If there is an error it is caught and a
     * message is written to the log files.
     * <p>
     * The player must have issued the in-game command {@code /mailsave} to have
     * mails saved to his file system.
     */
    public void fetch() {
        synchronized (folders) {
            if (!isRefreshed) fetchSWGAide(); // populate folders, see doExit()

            if (!swgPath().exists()) return; // no more work

            File[] mails = swgPath().listFiles(new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().endsWith(".mail");
                }
            });
            if (mails == null) return;

            File sp = swgAidePath();
            String boxOwner = ownerSelfSent();
            boolean copy = ((Boolean) SWGFrame.getPrefsKeeper().get(
                    "mailCopyToSWGAide", Boolean.TRUE)).booleanValue();
            boolean del = ((Boolean) SWGFrame.getPrefsKeeper().get(
                    "mailDeleteAfterCopy", Boolean.FALSE)).booleanValue();

            File to = null;
            try {
                if (copy && !sp.exists() && !sp.mkdirs())
                    throw new SecurityException("failed create dirs");

                ZString sb = null;
                for (File f : mails) {
                    String fn = f.getName();
                    SWGMailMessage mail = contains(fn);

                    if (mail != null)
                        fetchCopyDelete(copy, del, f, fn);

                    else {
                        try {
                            File tgt = fetchCopyDelete(copy, del, f, fn);
                            mail = newMail(tgt, owner);

                            if (mail.type() == Type.Auction)
                                folderAuction.add(mail);
                            else if (mail.type() == Type.ISDroid)
                                folderISDroid.add(mail);
                            else if (mail.fromLine().equalsIgnoreCase(boxOwner))
                                folderSent.add(mail); // is CC to self
                            else
                                folderInbox.add(mail);

                            allMails.put(fn, mail);
                        } catch (SecurityException e) {
                            throw e; // pass it outwards
                        } catch (Exception e) {
                            if (sb == null) sb = new ZString();
                            erraticMail(f, sb);
                        }
                    }
                }
                erraticMail(null, sb);
            } catch (SecurityException e) {
                SWGAide.printDebug("mbox", 1, "SWGMailBox:fetch:",
                        e.getMessage(), ":", "" + to, ZString.EOL,
                        "Windows Vista or Win 7 security? See README file");
            } catch (Throwable e) {
                SWGAide.printError("SWGMailBox:fetch", e);
            }
        }
    }

    /**
     * Helper method which may copy mails from SWG to SWGAide and returns a file
     * to create a mail from. If {@code copy} is {@code false} this method does
     * nothing. Otherwise, if the target does not exist this method copies the
     * file from source to target; then, if {@code delete} is {@code true} and
     * copy was successful, or if target exists, this method tries to delete the
     * source.
     * <p>
     * This method returns {@code src} if {@code copy} is {@code false},
     * otherwise a file within SWGAide is returned.
     * 
     * @param copy {@code true} to copy mails from SWG to SWGAide
     * @param del {@code true} if a copied mail should be deleted
     * @param src the source file
     * @param fn the name of the original file
     * @return a file for the mail
     */
    private File fetchCopyDelete(boolean copy, boolean del, File src, String fn) {
        if (!copy) return src; // nothing to do

        File to = new File(swgAidePath(), fn);
        if (!to.exists()) {
            if (del) {
                // try the cheaper move, if it works
                try {
                    if (src.renameTo(to))
                        return to;
                } catch (Exception e) { /* pass */
                }
            }
            if (!ZWriter.copy(src, to)) return src;
        }

        if (del) src.delete();
        return to;
    }

    /**
     * Helper method which scans SWGAide's "mails" folder for stray mails that
     * pertain to this mail-box. General mails are added to the Inbox but
     * auction mails and ISDroid reports are added to folders respectively.
     * <p>
     * Stray mails may be the result of a corrupted or replaced DAT file, but
     * also if SWGAide is re-installed or if the user has manually moved mails
     * into the folder.
     */
    private void fetchSWGAide() {

        // only invoked from fetch() which already is synchronized

        // refresh is-droid folder
        if (!folderISDroid.isEmpty()) {
            for (int i = folderISDroid.size() - 1; i >= 0; --i) {
                SWGMailMessage m = folderISDroid.get(i);
                if (m.getType() != Type.ISDroid) {
                    m.type(Type.Trash);
                    folderISDroid.remove(i);
                    folderTrash.addInternal(m);
                }
            }
        }

        // find files
        File[] list = boxPath.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isFile()
                        && !f.getName().toLowerCase().endsWith(".txt");
            }
        });
        if (list == null) return;

        ZString z = null;
        for (File f : list) {
            String fn = f.getName();
            if (contains(fn) == null) {
                try {
                    SWGMailMessage mail = newMail(f, owner);

                    fn = fn.toLowerCase();
                    if (!fn.endsWith("mail")
                            /* XXX: remove in a while, added in 0.9.0 */
                            && contains(mail.getName()) != null) continue;

                    if (mail.type() == Type.Auction)
                        folderAuction.add(mail);
                    else if (mail.type() == Type.ISDroid)
                        folderISDroid.add(mail);

                    // some old suffixes, if any
                    else if (mail.type() == Type.Trash
                            || fn.endsWith(SWGMailMessage.Type.Trash.suffix))
                        folderTrash.addInternal(mail);
                    else if (mail.type() == Type.Sent
                            || fn.endsWith(SWGMailMessage.Type.Sent.suffix))
                        folderSent.add(mail);
                    else
                        folderInbox.add(mail);

                    allMails.put(mail.getName(), mail);
                } catch (Exception e) {
                    if (z == null) z = new ZString();
                    erraticMail(f, z);
                }
            }
        }
        erraticMail(null, z);
        isRefreshed = true;
    }

    /**
     * Returns a mail folder with the specified name name, or {@code null}. This
     * method is case sensitive and returns null if this mail-box does not
     * contain a folder that matches the name.
     * 
     * @param name a folder name
     * @return a mail folder for the name, or {@code null}
     */
    public SWGMailFolder folder(String name) {
        synchronized (folders) {
            for (SWGMailFolder fld : folders())
                if (name.equals(fld.getName())) return fld;
            return null;
        }
    }

    /**
     * Creates, adds, and returns a new mail folder with the specified name. If
     * a folder exists for the name this method does nothing and returns {@code
     * null}.
     * 
     * @param name a folder name
     * @return the new folder, or {@code null}
     */
    public SWGMailFolder folderAdd(String name) {
        /*
         * If adding a method that accepts a ready-made folder, ensure the this
         * box is set as its owner
         */

        if (folder(name) == null) {
            synchronized (folders) {
                List<SWGMailFolder> fl = folders();

                SWGMailFolder f = new SWGMailFolder(name, this);

                fl.add(f);
                Collections.sort(fl);

                return f;
            }
        }
        return null;
    }

    /**
     * Deletes a folder with the specified name from this mailbox. If the folder
     * is a default folder or if it is not empty this method does nothing.
     * 
     * @param name a folder name
     */
    public void folderDelete(String name) {
        synchronized (folders) {
            for (SWGMailFolder fld : folders()) {
                if (name.equals(fld.getName())) {
                    if (fld.isDefault() || !fld.isEmpty()) return;

                    folders().remove(fld);
                    break;
                }
            }
        }
    }

    /**
     * Returns an array of folder names for this mail box. If the specified
     * string denotes an existing folder it is excluded from the returned array.
     * 
     * @param string a folder name to exclude, or {@code null}
     * @return an array of folder names
     */
    public String[] folderNames(String string) {
        synchronized (folders) {
            List<SWGMailFolder> fl = folders();
            List<String> ret = new ArrayList<String>(fl.size());
            for (int i = 0; i < fl.size(); ++i) {
                SWGMailFolder f = fl.get(i);
                if (string == null || !f.getName().equals(string))
                    ret.add(f.getName());
            }
            return ret.toArray(new String[ret.size()]);
        }
    }

    /**
     * Returns a list of all mail folders of this mailbox. The returned list is
     * <b>read-only</b>. If the returned folder is modified while iterating over
     * it the result is undefined and may throw exceptions; you may lock at the
     * folder itself for synchronization.
     * 
     * @return a list of folders
     */
    public List<SWGMailFolder> folders() {
        return folders;
    }

    /**
     * Moves the specified mail from one folder to the other. This method does
     * nothing if this mail-box does not contain any of the folders, or if the
     * mail does not exist or is {@code null}.
     * 
     * @param m a mail
     * @param from a source folder
     * @param to a target folder
     * @throws NullPointerException if a folder is {@code null}
     */
    public void move(SWGMailMessage m, SWGMailFolder from, SWGMailFolder to) {
        synchronized (folders) {
            if (m != null && !to.equals(from) && folders().contains(from)
                    && folders().contains(to) && from.contains(m)) {

                Type t = m.getType();
                if (to == folderTrash)
                    m.type(Type.Trash);
                else if (to == folderAuction) {
                    // non-auction mails cause errors at the trade panel
                    if (t == Type.Auction)
                        m.type(Type.Auction);
                    else
                        return;
                } else if (to == folderISDroid) {
                    // non-ISDroid mails cause errors at the ISDroid panel
                    if (t == Type.ISDroid)
                        m.type(Type.ISDroid);
                    else
                        return;
                } else if (m.type() == Type.Trash) // any other folder
                    m.type(t);

                from.remove(m);
                to.add(m);
            }
        }
    }

    /**
     * Returns the canonical name for the owner of this mail-box, on the form
     * "SWG.Europe-Chimaera.some_name"; this is used for mails sent to self.
     * 
     * @return the sent-to-self form of the owner's name
     */
    private String ownerSelfSent() {
        return String.format("SWG.%s.%s",
                owner.gxy().getNameComplete(), owner.getName());
    }

    /**
     * Resolves this instance at deserialization and possibly updates member
     * fields.
     * 
     * @return this
     */
    private Object readResolve() {
        swgDirPath = null;
        return this;
    }

    /**
     * Returns a relative path to a directory used by this mailbox. This path is
     * on the form "mails\Station\Galaxy\Character" which is relative SWGAide,
     * as opposed to a path within the SWG "profiles" folder.
     * 
     * @return a file path within SWGAide
     */
    public File swgAidePath() {
        return boxPath;
    }

    /**
     * Returns an absolute path to SWG's mail folder for this mail-box. This is
     * a path at the current file system at the form {@code
     * pathTo\SWG\profiles\Station\Galaxy\mail_Character_Name}. If SWGAide is
     * launched without a valid path to SWG this value is a dummy file.
     * 
     * @return an absolute path, or a dummy file
     */
    File swgPath() {
        if (swgDirPath == null)
            swgDirPath = owner.galaxy().exists()
                    ? new File(owner.galaxy().swgPath(),
                            "mail_" + owner.getNameComplete())
                    : DUMMY_FILE;
        return swgDirPath;
    }

    @Override
    public String toString() {
        return String.format("%s's SWGMailBox", owner);
    }

    /**
     * Updates mails and folders of this mail-box to a current version. If the
     * mail-folders of this box are already up-to-date this method does nothing.
     */
    public void update() {
        //Empty as the former necessary updates are very old
    }

    /**
     * Helper method which handles a mail file that raised an exception. This
     * method adds text for the file to the string-builder, <i>or </i> , if the
     * file is {@code null} it writes the content of the string-builder to
     * "logs/mail-error.txt". Thus this method is first invoked for each erratic
     * mail and then finally invoked to write to file. If the string-builder is
     * {@code null} this method does nothing.
     * 
     * @param file a file that caused an error, or {@code null} to write to file
     * @param z a string, or {@code null}
     */
    private static void erraticMail(File file, ZString z) {
        if (z == null) return;
        if (file == null) {
            try {
                ZString zz = new ZString().nl();
                zz.appnl(SWGAide.time());
                zz.app(z);

                File errorReport = new File("logs", "mail-error.txt");
                ZWriter.writeExc(zz.toString(), errorReport, true);
            } catch (Exception e) {
                SWGAide.printError("SWGMailBox:erraticMail", e);
            }
        } else
            z.nl().app(file.getAbsolutePath());
    }

    /**
     * Creates and returns an instance for a mail. The file must exist at the
     * current file system and no argument can be {@code null}.
     * 
     * @param f a file
     * @param o a character
     * @return an instance for a mail
     * @throws Exception if there is an error
     * @throws NullPointerException if an argument is {@code null}
     */
    public static SWGMailMessage newMail(File f, SWGCharacter o)
            throws Exception {
        return SWGMailMessage.newInstance(f, o);
    }
}
