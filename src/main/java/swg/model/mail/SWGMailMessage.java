package swg.model.mail;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InvalidNameException;

import swg.SWGAide;
import swg.crafting.resources.SWGResourceSet;
import swg.gui.common.SWGGui;
import swg.model.SWGCharacter;
import swg.swgcraft.SWGResourceManager;
import swg.tools.SearchFiles;
import swg.tools.ZReader;
import swg.tools.ZString;
import swg.tools.ZWriter;

/**
 * This type denotes a mail message in SWG. Typically this denotes a file within
 * SWG, at the path <SWG>\profiles\Station\Galaxy\mails_Character\
 * <p>
 * Mail messages are not automatically saved from in-game to file, the user must
 * execute the in-game /mailsave command; mails are then saved to the current
 * file system. Until a mail is deleted in-game the mail is saved to the current
 * disk; hence a multi-computer user can save a mail at several systems. When a
 * mail is deleted in-game it is not deleted from the file system.
 * <p>
 * Sample of a typical mail file:
 * 
 * <pre>
 *  123456789
 *  SWG.Europe-Chimaera.zimoon
 *  Customer Service Ticket
 *  TIMESTAMP: 1135290806
 *  Dear Sir, we hereby grant you a lifetime account and want to express... 
 * </pre>
 * <p>
 * Historically SWGAide moved all mails from its original location to within its
 * "mails" folder. This was for two reasons:
 * <ul>
 * <li>performance &mdash; avoid scanning several hundreds or thousands of mails
 * in each character's folder but have them at a recorded location</li>
 * <li>feature support &mdash; at the time it was easier to provide most of the
 * features about mails if they were located locally and if their file suffix
 * reflected the type of mail, such as .auct, .dead, .isdr, or .sent.</li>
 * </ul>
 * However, this was not ideal, users did not want SWGAide to fiddle with mails
 * and Windows Vista and 7 are not kind with Java applications.
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
 * another option to delete mails from the current host after being copied.
 * <p>
 * To make room for these changes both this type and the type for auction mails
 * are modified.
 * 
 * @author Steven M. Doyle <shadow@triwizard.net>
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGMailMessage
        implements Serializable, Comparable<SWGMailMessage>, SWGGui {

    /**
     * A dummy array for non-existing files with a string that reads
     * "Unavailable at this system".
     */
    private static final String[] DUMMY_STR_ARRAY =
            new String[] { "Message details unavailable, the .mail file does not exist on this computer." };

    /**
     * {@code true} if any error occurred while parsing mails, {@code false}
     * otherwise
     */
    public static boolean hasError;

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -5892350691324132130L;

    /**
     * A field for auction records, {@code null} if this is not an auction
     * record or if it is not yet initiated. This member is serialized to
     * support the trader features of SWGAide.
     */
    private SWGAuctionerData auctionerData;

    /**
     * The mail body as an array of text lines, or {@code null} if this member
     * is not yet instantiated.
     */
    private transient String[] body;

    /**
     * An abstract file path for this mail, or {@code null}. The members
     * {@link #id} and {@link #owner} provide the necessary information to
     * recreate this member, hence this type is host independent. Once called
     * for this it is an absolute path at the current file system throughout the
     * session. XXX: make this transient in a while, added in 0.9.0.
     */
    private File file;
    
    private String fileName;

    /**
     * The unique ID number that is read from header and the file name.
     */
    private final long id;

    /**
     * A field for ISDroid reports, or {@code null} if this is not an ISDroid
     * report or if it is not yet initiated.
     */
    private transient SWGISDroidReport isdroidReport;

    /**
     * If &gt; zero this is the previous local status time for the set of
     * downloaded resources. This member is set when isdroidData is requested by
     * a client and a new download is available.
     */
    private transient long isdroidTime;

    /**
     * The message send date as number of seconds since 1970, Jan 1, 00:00:00.
     */
    private final long messageDate;

    /**
     * The message sender, on the form SWG.Europe-Chimaera.Zimoon. If the sender
     * equals the owner the type of this mail is {@link Type#Sent} as this mail
     * is perhaps sent to somebody else but a copy is sent to self.
     */
    private String messageFrom;

    /**
     * The subject of this mail.
     */
    private String messageSubject;

    /**
     * The owner of the mail-box that contains this mail.
     */
    private SWGCharacter owner;

    /**
     * Denotes the type of mail.
     */
    private Type type;

    /**
     * Constructs an instance of this type based on the specified file. This
     * instance contains the header of the mail and if the file is available at
     * the current system the body is obtained at demand.
     * 
     * @param file a path to an existing file
     * @param o the owner of this mail
     * @throws Exception if there is an error
     */
    private SWGMailMessage(File file, SWGCharacter o) throws Exception {
        this.file = file;
        this.owner = o;
        ZReader reader = null;
        try {
            reader = ZReader.newTextReaderExc(file());

            String tmpid = reader.lineExc(false);
            if(tmpid.contains("_")) {
            	String[] chunk = tmpid.split("_");
            	if(chunk[0].length() <10) {
            		tmpid = chunk[0];
            	}else if(chunk[1].length() <10){
            		tmpid = chunk[1];
            	}
            }
            tmpid = tmpid.replaceAll( "[^\\d]", "" );
            messageFrom = reader.lineExc(false);
            messageSubject = reader.lineExc(false);

            String tmpStr;
            while ((tmpStr = reader.lineExc(false)) != null
                    && !tmpStr.startsWith("TIMESTAMP: ")) {
                messageSubject += ' ';
                messageSubject += tmpStr;
            }
            messageDate = tmpStr != null
                    ? Long.parseLong(tmpStr.substring("TIMESTAMP: ".length()))
                    : 0L;

            tmpid = new StringBuilder().append(messageDate).append(tmpid).toString();
            id = Long.parseLong(tmpid);
            fileName = file.getName();
            type = getType();
            auctionData(); // grab data

            reader.close();
        } catch (Exception e) {
            SWGAide.printError("SWGMailMessage:parseHeader: " + file(), e);
            hasError = true;
            throw e;
        } finally {
        	reader.close();
        }
    }

    /**
     * Returns {@code true} if this mail contains the specified string. The
     * search is case insensitive.
     * 
     * @param str a string
     * @return {@code true} if this mail contain the text
     */
    public boolean accept(String str) {
        if (exists()) {
            try {
                return SearchFiles.containsString(file(), str, true);
            } catch (Exception e) {
                SWGAide.printError("SWGMailMessage:accept" + file(), e);
            }
        }
        return false;
    }

    /**
     * Returns auction data from this message, or {@code null}. If this is not
     * an auction mail this method returns {@code null}. This data is always
     * available, also if the file for this mail does not exist at the current
     * file system.
     * 
     * @return auction data, or {@code null}
     */
    public SWGAuctionerData auctionData() {
        if (type == Type.Auction && auctionerData == null)
            auctionerData = SWGAuctionerData.fromMail(this);

        return auctionerData;
    }

    /**
     * Returns the body of this mail as an array of strings. If the body is
     * empty a zero-length array is returned, if the mail does not exist at the
     * current file system the one element of the array reads 'unavailable', and
     * if there is an error while parsing the body the final element reads an
     * error message.
     * 
     * @return an array of strings
     */
    String[] body() {
        if (body == null)
            synchronized (this) {
                if (body == null) body = body(file());
            }
        return body;
    }

    /**
     * Returns the body of this mail as a text string. If the body is empty this
     * method returns an empty string, if this mail does not exist at the
     * current file system the string reads 'unavailable', and if there is an
     * error the string begins with "ERROR: " and an error message.
     * 
     * @return the text body
     */
    public String bodyText() {
        String[] mb = body();
        ZString z = new ZString();
        for (String s : mb)
            z.appnl(s);

        return z.toString();
    }

    public int compareTo(SWGMailMessage o) {
        // note the inverse comparison, since we want newer mails first
        return o.id < this.id
                ? -1
                : o.id == this.id
                        ? 0
                        : 1;
    }

    /**
     * Returns the send date for this mail. This is when the message was sent
     * counted in seconds from 1970, Jan 1, 00:00:00.
     * 
     * @return the send date
     */
    public long date() {
        return messageDate;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof SWGMailMessage
                    && this.id == ((SWGMailMessage) obj).id);
    }

    /**
     * Determines if the argument equals the file for this mail. This method
     * compares the absolute path of the two.
     * 
     * @param f a file
     * @return {@code true} as defined by {@link File#equals(Object)}
     */
    public boolean equalsFile(File f) {
        return file().getAbsoluteFile().equals(f.getAbsoluteFile());
    }

    /**
     * Determines if this mail exists on the current file system.
     * 
     * @return {@code true} as defined by {@link File#exists()}
     */
    public boolean exists() {
        return file().exists();
    }

    /**
     * Returns a file path for this mail, or a dummy file. If SWGAide is
     * launched without a valid path within the SWGAide "mails" folder <i>and
     * </i> without a valid path to SWG this method always returns a dummy file
     * instance for which {@code exists()} always returns {@code false}.
     * 
     * @return a file path for this mail, or a non-existing file
     */
    File file() {
        if (file == null) {
            // get a directory
            File d = owner.mailBox().swgAidePath();
            if (!d.exists())
                d = owner.mailBox().swgPath();

            if (!d.exists())
                file = SWGMailBox.DUMMY_FILE;
            else {
                File f = new File(d, getRealName());
                file = f.exists()
                        ? f
                        : SWGMailBox.DUMMY_FILE;
            }
        }

        return file;
    }
    
    /**
     * Deletes the file for this mail from the current file system. This method
     * tries to delete this mail twice to possibly delete it from as well within
     * SWGAide as from within SWG.
     * 
     * @throws SecurityException if there is an error
     */
    void fileDelete() {
        File lf = new File(owner.mailBox().swgAidePath(), getRealName());
        File sf = new File(owner.mailBox().swgPath(), getRealName());
        
        if (lf.getAbsoluteFile().exists()) {
        	lf.getAbsoluteFile().delete();
        }
        
        if (sf.getAbsoluteFile().exists()) {
        	sf.getAbsoluteFile().delete();
        }

    }

    /**
     * Helper method that nullifies the file member for this mail.
     */
    void fileReset() {
        file = null;
    }
    /**
     * Returns the name of the sender. This is the character or game service
     * that sent this mail. For a character it is always the first name, not a
     * complete name.
     * 
     * @return the sender name
     */
    public String from() {
        return messageFrom.substring(messageFrom.lastIndexOf('.') + 1);
    }

    /**
     * Returns the full sender line of this mail. This is a string at the form
     * SWG.Galaxy.Name, such as {@code SWG.Europe-Chimaera.Zimoon}. The name is
     * just the sender's first name, not the full name.
     * 
     * @return the full sender line
     */
    public String fromLine() {
        return messageFrom;
    }

    @Override
    public String getDescription() {
        return file.toString() + getName();
    }

    /**
     * {@inheritDoc} This is the file name of this mail, its ID and file suffix.
     */
    public String getName() {
    	if(fileName == null) {
        	return SWGMailBox.DUMMY_FILE.getName();
        }
    	return fileName;
    }
    
    /**
     * Get the REAL filename
     */
    public String getRealName() {
    	if(fileName == null) {
    	return SWGMailBox.DUMMY_FILE.getName();
    	}
    	return fileName;
    }

    /**
     * Determines and returns the type of mail this is. In particular, this
     * method returns {@link Type#Auction} if this mail is from "auctioner",
     * {@link Type#Sent} if this mail is from the owner of the mailbox, and if
     * this mail is from "interplanetary survey droid" its age determines if
     * {@link Type#ISDroid} or {@link Type#Trash} is returned with 20 hours as
     * the limit. For all other types this method currently returns
     * {@link Type#Any}.
     * <p>
     * This method may return a different value than {@link #type()}; the
     * returned value denotes the type of this mail.
     * 
     * @return a type
     */
    Type getType() {
        if (fromLine().endsWith("interplanetary survey droid") ||
			fromLine().endsWith("Interplanetary Survey Droid")) {
            long now = System.currentTimeMillis() / 1000;
            now -= (60 * 60 * 20); // max 20 hours old
            return messageDate > now
                    ? Type.ISDroid
                    : Type.Trash;
        }
        if (fromLine().endsWith("auctioner")) return Type.Auction;
        if (from().equalsIgnoreCase(owner.getName())) return Type.Sent;

        return Type.Any;
    }

    @Override
    public int hashCode() {
        return (int) (17 + (49 * id));
    }

    /**
     * Returns the header for this mail. This method returns a string with the
     * ID, sender, subject, and send date.
     * 
     * @return the mail header
     */
    String header() {
        return String.format("%s%n%s%n%s%n%s", String.valueOf(id), messageFrom,
                messageSubject, String.valueOf(messageDate));
    }

    /**
     * Returns the unique ID number that is read from header and the file name.
     * The owner together with the ID is used to obtain the file.
     * 
     * @return an id
     */
    public long id() {
        return id;
    }

    /**
     * Returns the data for an ISDroid report from this mail, or {@code null}.
     * This method caches the data until another local download from SWGCraft is
     * ready, unless the user exits SWGAide. If this is not an ISDroid-report
     * this method returns {@code null}.
     * 
     * @param krSet a set of locally known resources
     * @return an ISDroid report, or {@code null}
     * @throws IllegalArgumentException if an argument or parsed is invalid
     * @throws IllegalStateException if there is an error parsing the mail body
     * @throws InvalidNameException if a resource name is invalid
     * @throws NullPointerException if anything is {@code null}
     */
    public SWGISDroidReport isdroidData(SWGResourceSet krSet)
            throws InvalidNameException {

        if (type != Type.ISDroid || !exists()) return null;

        long localStatus = SWGResourceManager.getStatusLocalTime(owner.gxy()).
                longValue();

        if (isdroidReport == null || localStatus > isdroidTime) {
            // if new or do a refresh when there is a new local download
            isdroidReport = SWGISDroidReport.newInstance(this, krSet);
            isdroidTime = localStatus;
        }
        return isdroidReport;
    }

    /**
     * Returns the owner of this mail.
     * 
     * @return an owner
     */
    public SWGCharacter owner() {
        return owner;
    }

    /**
     * Resolves this instance at deserialization and possibly updates member
     * fields.
     * 
     * @return this
     */
    private Object readResolve() {
        this.messageFrom = this.messageFrom.intern();
        this.messageSubject = this.messageSubject.intern();

        if (file != null && !file.getName().endsWith("mail")) {
            File f = new File(file.getParentFile(), getName());
            file.renameTo(f);
        }

        file = null;

        return this;
    }

    /**
     * Writes the header and body of this mail to the specified target.
     * 
     * @param target a file path
     */
    public void saveTo(File target) {
        if (target.getAbsoluteFile().equals(file().getAbsoluteFile()))
            throw new IllegalArgumentException(
                    "Copy to self: " + target.getAbsolutePath());

        ZWriter.copy(file(), target);
    }

    /**
     * Returns the subject line of the message.
     * 
     * @return the subject
     */
    public String subject() {
        return messageSubject;
    }

    @Override
    public String toString() {
        return String.format("SWGMailMessage[%s, %s]", getName(), file());
    }

    /**
     * Returns the type constant this mail is set to.
     * <p>
     * This method may returns a different value than {@link #getType()}.
     * 
     * @return a type constant
     */
    public Type type() {
        return type;
    }

    /**
     * Sets the type of mail to the specified constant.
     * 
     * @param t a type constant
     */
    void type(Type t) {
        this.type = t;
    }

    /**
     * Serialize this instance of {@link SWGMailMessage}. This method nullifies
     * {@link #file} and then invokes the {@code defaultWriteObject()} method.
     * 
     * @param oos an object output stream to write this instance to
     * @throws IOException if there is an I/O error
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        file = null;
        oos.defaultWriteObject();
    }

    /**
     * Reads the specified file and returns the content as an array of strings.
     * If the file is empty the array has length zero, if the file does not
     * exist at the current file system {@link #DUMMY_STR_ARRAY} is returned,
     * and if there is an error it is caught and written to SWGAide's log file
     * and the final element of the array reads an error message.
     * 
     * @param file a file
     * @return an array of strings, may be empty
     */
    private static String[] body(File file) {
        ArrayList<String> list = new ArrayList<String>(32);
        if (file.exists()) {
        	ZReader sr = null;
            try {
                sr = ZReader.newTextReaderExc(file);
                List<String> sl = sr.linesExc();
                boolean body = false;
                for (String line : sl) {
                    if (body)
                        list.add(line);
                    else if (line.startsWith("TIMESTAMP: ")) body = true;
                }
                sr.close();
            } catch (Exception e) {
                SWGAide.printDebug("mail", 1,
                        "SWGMailMessage:body:", e.getMessage());
                list.add("ERROR: mail:body: " + e.getMessage());
            } finally {
            	sr.close();
            }
        } else
            return DUMMY_STR_ARRAY;

        return list.toArray(new String[list.size()]);
    }

    /**
     * Creates and returns an instance of this type. The file must exist at the
     * current file system. This method must only be invoked by
     * {@link SWGMailBox#newMail(File, SWGCharacter)}.
     * 
     * @param f a file
     * @param o a character
     * @return an instance of this type
     * @throws Exception if there is an error
     * @throws NullPointerException if an argument is {@code null}
     */
    static SWGMailMessage newInstance(File f, SWGCharacter o) throws Exception {
        if (f == null || o == null) throw new NullPointerException(
                String.format("SWGMail:newInstance: %s %s", f, o));

        File ff = f;
        String n = f.getName();
        if (!n.toLowerCase().endsWith(Type.Any.suffix)) {
            // XXX: remove in a while, added with 0.9.0
            n = n.substring(0, n.length() - 4) + Type.Any.suffix;
            ff = new File(f.getParentFile(), n);
            f.renameTo(ff);
        }

        return new SWGMailMessage(ff, o);
    }

    /**
     * This enum type denotes the kind of a mail. SWGAide supports mails of type
     * Any (common or unclassified mails), Auction, City, Guild, ISDroid
     * reports, Sent (from self), and Trash bin. More types may be added when
     * uses are identified.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public enum Type {

        /**
         * Denotes common mails or mails yet not classified or for which no type
         * exists. This constant use the "mail" suffix.
         */
        Any("mail"),

        /**
         * Denotes an auction mail. This constant used the "auct" suffix.
         */
        Auction("auct"),

        /**
         * Denotes a city mail, which may be for the Mayor or for a citizen.
         * This constant use the "mail" suffix.
         */
        City("mail"),

        /**
         * Denotes a guild mail. This constant use the "mail" suffix.
         */
        Guild("mail"),

        /**
         * Denotes a mail for an ISDroid report. This constant used the "isdr"
         * suffix.
         */
        ISDroid("isdr"),

        /**
         * Denotes a mail sent to self. This constant used the "sent" suffix.
         */
        Sent("sent"),

        /**
         * Denotes a spam mail. This constant use the "mail" suffix.
         */
        Spam("mail"),

        /**
         * Denotes a mail in the trash bin, no matter its former type. This
         * constant used the "dead" suffix.
         */
        Trash("dead");

        /**
         * The suffix for mail files used by SWGAide before version 0.9.0. From
         * that version all mails use "mail" as its file suffix but the
         * {@link Type} determine its type.
         */
        public final String suffix;

        /**
         * Creates a constant of this type. The suffixes different than "mail"
         * are those that were used by SWGAide; a suffix supports backward
         * compatibility for the case a user picks up an old installation of
         * SWGAide while moving around.
         * <p>
         * From version 0.9.0 SWGAide does not modify any file suffix but uses
         * other strategies. The new types that are added from this version and
         * later use the "mail" suffix because no files exist with a suffix that
         * would denote the new types and no backward issues exist.
         * 
         * @param suffix a file suffix, the dot excluded
         */
        private Type(String suffix) {
            this.suffix = suffix;
        }
    }
}
