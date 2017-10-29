package swg.model.mail;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import swg.SWGAide;
import swg.model.SWGCharacter;
import swg.tools.ZString;

/**
 * This type contains the data of an auction mail. These mails have a sender
 * such as {@code SWG.Galaxy.auctioner}. Eight different types exist, each with
 * its individual header and body format; see the TYPE constants. The only
 * common denominator is {@link #item}, all other variables are optional.
 * <p>
 * Instances of this type are saved in SWGAide's DAT file because they must
 * exist even if SWGAide is launched with no access to neither mails nor SWG:
 * The collated information of a mail and its reference to an instance of this
 * type gives complete information of an auction mail.
 * <p>
 * Late 2010 SWGAide was made host independent and this type was reworked to
 * support this.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGAuctionerData implements Serializable {
    
    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 6439896064245239159L;

    /**
     * The amount of credits involved in this transaction (optional). If this
     * auction mail is neither for a sale nor buy this member is 0.
     */
    private final long credits;

    /**
     * The date this auction mail was sent, from {@link SWGMailMessage#date()}.
     */
    private final long date;

    /**
     * The item for which this transaction is all about.
     */
    private String item;

    /**
     * The location (planet and town/area) where this transaction took place
     * (optional). Example: "Mos Eisley, on Tatooine" or "Naboo, on Naboo". If
     * this auction mail is neither for a sale nor buy this member is {@code
     * null}.
     */
    private String location;

    /**
     * The unique ID for the mail this instance is parsed from. This ID together
     * with an owner is used to obtain the original mail, if possible.
     */
    private final long mailID;

    /**
     * The buyer or seller (optional). If this auction mail is neither for a
     * sale nor buy this member is {@code null}.
     */
    private String otherCharacter;

    /**
     * The owner of this auction mail.
     */
    private final SWGCharacter owner;

    /**
     * Denotes the kind of auction mail this is.
     */
    private final Type type;

    /**
     * The vendor that is involved in this transaction (optional). This member
     * is the name of a vendor or a description of the means for the deal, or
     * its value is obtained from {@link Type#vendor}. If this auction mail is
     * neither a sale nor buy then this member is {@code null}, and so is the
     * type's vendor string.
     */
    private String vendorName;

    /**
     * Creates an instance of this type; see {@link #fromMail(SWGMailMessage)}.
     * 
     * @param mail an auction mail
     * @param type the type of auction mail this is
     * @param item the item this mail is about
     * @param location the location for this transaction, or {@code null}
     * @param oc another character involved in this deal, or {@code null}
     * @param cr the amount of credits involved in this deal, or {@code 0}
     * @param vn the name of the vendor involved in this deal, or {@code null}
     */
    private SWGAuctionerData(SWGMailMessage mail, Type type, String item,
            String location, String oc, int cr, String vn) {

        this.type = type;
        this.credits = cr;
        this.item = strIntern(item.trim());
        this.location = strIntern(location);
        this.otherCharacter = strIntern(oc);
        this.vendorName = strIntern(vn);

        date = mail.date();
        mailID = mail.id();
        owner = mail.owner();
    }

    /**
     * Returns the send date for this auction mail. This is when the message was
     * sent counted in seconds from 1970, Jan 1, 00:00:00.
     * 
     * @return the send date
     */
    public long date() {
        return date;
    }

    /**
     * Returns the name or a description of the item for this auction mail.
     * 
     * @return an item name or description
     */
    public String item() {
        // color string can be wherever
        String i = item.replaceAll("\\\\#\\.", ""); // color default
        i = i.replaceAll("\\\\#[0-9a-fA-F]{6}", ""); // color hex
        return i;
    }

    /**
     * Returns the location where this transaction took place, or {@code null}.
     * This is a town/area and planet, e.g. "Mos Eisley, on Tatooine" or
     * "Naboo, on Naboo". If this auction mail is neither for a sale nor buy
     * this method returns {@code null}.
     * 
     * @return the location for this transaction, or {@code null}
     */
    public String location() {
        return location;
    }

    /**
     * Returns the unique ID for the mail this instance is parsed from. This ID
     * together with an owner is used to obtain the original mail, if possible.
     * 
     * @return a mailID
     */
    public long mailID() {
        return mailID;
    }

    /**
     * Returns the name of the opponent, or {@code null}. This is a buyer or a
     * seller. If this auction mail is neither for a sale nor buy this method
     * returns {@code null}.
     * 
     * @return the name of the opponent, or {@code null}
     */
    public String other() {
        return otherCharacter;
    }

    /**
     * Returns the owner of this auction mail. The ID together with the owner is
     * used to obtain the original mail, if possible.
     * 
     * @return an owner
     */
    public SWGCharacter owner() {
        return owner;
    }

    /**
     * Returns the amount of credits for this transaction, or 0. If this auction
     * mail is neither for a sale nor buy this method returns 0.
     * 
     * @return an amount, or 0
     */
    public long price() {
        return credits;
    }

    /**
     * Resolves this instance at deserialization and possibly updates member
     * fields. In particular this method interns all strings, chiefly to reduce
     * the size of the DAT file at serialization.
     * 
     * @return this
     */
    private Object readResolve() {
        this.item = this.item.intern();
        this.location = strIntern(this.location);
        this.otherCharacter = strIntern(this.otherCharacter);
        this.vendorName = strIntern(this.vendorName);

        return this;
    }

    @Override
    public String toString() {
        return String.format("SWGAuctionData[%s, %s, %s, %s cr, %s, %s, %s]",
                String.valueOf(mailID), type.subject, item,
                String.valueOf(credits), vendorName, location, otherCharacter);
    }

    /**
     * Returns a constant that denotes the kind of auction mail this is.
     * 
     * @return a type constant
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the name of the vendor or a description of the means behind this
     * deal, or {@code null}. If this auction mail is neither for a sale nor buy
     * this method returns {@code null}.
     * 
     * @return a vendor name or description, or {@code null}
     */
    public String vendor() {
        return vendorName == null
                ? type.vendor
                : vendorName;
    }

    /**
     * Returns an instance of this type that is parsed from the specified mail,
     * or {@code null}. If the specified mail does not exist, if it is not an
     * auction mail, or if there is an error, this method returns {@code null}.
     * Otherwise, this factory method determines which type of auction for this
     * message and sets the appropriate variables; redundant variables are set
     * to default values, {@code null} or 0.
     * 
     * @param mail a mail to parse
     * @return an instance of this type, or {@code null}
     */
    @SuppressWarnings("synthetic-access")
    static SWGAuctionerData fromMail(SWGMailMessage mail) {
        if (!mail.exists() || mail.type() != SWGMailMessage.Type.Auction)
            return null;

        try {
            String body = mail.bodyText();

            // walk through the different types of mails by their subject lines
            // sorted by estimated likelihood for performance

            if (mail.subject().equals("Vendor Sale Complete")) {
                Matcher m = Type.VENDOR_SALE.re.matcher(body);
                if (m.find()) return new SWGAuctionerData(mail,
                        Type.VENDOR_SALE, m.group(2), m.group(5), m.group(3),
                        Integer.parseInt(m.group(4)), m.group(1));

            } else if (mail.subject().equals("Vendor Item Purchased")) {
                Matcher m = Type.VENDOR_PURCHASE.re.matcher(body);
                if (m.find()) return new SWGAuctionerData(mail,
                        Type.VENDOR_PURCHASE, m.group(1), m.group(4),
                        m.group(2), Integer.parseInt(m.group(3)), null);

            } else if (mail.subject().equals("Instant Sale Complete")) {
                Matcher m = Type.INSTANT_SALE.re.matcher(body);
                if (m.find()) return new SWGAuctionerData(mail,
                        Type.INSTANT_SALE, m.group(1), m.group(4), m.group(2),
                        Integer.parseInt(m.group(3)), null);

            } else if (mail.subject().equals("Auction Unsuccessful")) {
                Matcher m = Type.SALE_UNSUCCESSFUL.re.matcher(body);
                if (m.find()) return new SWGAuctionerData(mail,
                        Type.SALE_UNSUCCESSFUL, m.group(1),
                        null, null, 0, null);

            } else if (mail.subject().equals("Instant Sale Item Purchased")) {
                Matcher m = Type.INSTANT_PURCHASE.re.matcher(body);
                if (m.find()) return new SWGAuctionerData(mail,
                        Type.INSTANT_PURCHASE, m.group(1), m.group(4),
                        m.group(2), Integer.parseInt(m.group(3)), null);

            } else if (mail.subject().equals("Auction Won")) {
                Matcher m = Type.AUCTION_WON.re.matcher(body);
                if (m.find()) return new SWGAuctionerData(mail,
                        Type.AUCTION_WON, m.group(1), m.group(4), m.group(2),
                        Integer.parseInt(m.group(3)), null);

            } else if (mail.subject().equals("Auction Item Expired")) {
                Matcher m = Type.ITEM_EXPIRED.re.matcher(body);
                if (m.find()) return new SWGAuctionerData(mail,
                        Type.ITEM_EXPIRED, m.group(1), null, null, 0, null);

            } else if (mail.subject().equals("Auction Outbid")) {
                Matcher m = Type.AUCTION_OUTBID.re.matcher(body);
                if (m.find()) return new SWGAuctionerData(mail,
                        Type.AUCTION_OUTBID, m.group(1), null, null, 0, null);
            }

            SWGAide.printDebug("aucm", 1, "SGWAuction:fromMail:",
                    mail.toString(), ZString.EOL, mail.header(),
                    ZString.EOL, body);

        } catch (Throwable e) {
            SWGAide.printError("SGWAuction:fromMail: " + mail.toString(), e);
        }
        return null;
    }

    /**
     * Returns an interned string for the specified argument, or {@code null}.
     * If the argument is {@code null} this method returns {@code null}.
     * 
     * @param s a string, or {@code null}
     * @return an interned string, or {@code null}
     */
    private static String strIntern(String s) {
        return s != null
                ? s.intern()
                : null;
    }

    /**
     * This enum type denotes the kind of auction message.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public enum Type {

        /**
         * Type of message. Mail owner was outbid in an auction at the bazaar.
         * Subject line: "Auction Outbid".
         */
        AUCTION_OUTBID("Auction Outbid", "Bazaar", Pattern.compile(
                ".+ outbid on the \"(.+)\" that you were bidding on\\.",
                Pattern.DOTALL)),

        /**
         * Type of message. Mail owner won an auction at the bazaar. Subject
         * line: "Auction Won".
         */
        AUCTION_WON("Auction Won", "Bazaar", Pattern.compile(
                ".+ auction of \"(.+)\" from \"(.+)\" for (\\d+) .+\\s+.+ " +
                        "at (.+)\\.", Pattern.DOTALL)),

        /**
         * Type of message. Mail owner bought an item at the bazaar. Subject
         * line: "Instant Sale Item Purchased".
         */
        INSTANT_PURCHASE("Instant Sale Item Purchased", "Bazaar",
                Pattern.compile(".+ of \"(.+)\" from \"(.+)\" for " +
                        "(\\d+) .+\\s+.+ at (.+)\\.", Pattern.DOTALL)),

        /**
         * Type of message. Mail owner sold an item at the bazaar. Subject line:
         * "Instant Sale Complete".
         */
        INSTANT_SALE("Instant Sale Complete", "Bazaar", Pattern.compile(
                ".+of (.+) has been sold to (.+) for (\\d+) " +
                        "credits\\s+.+at (.+)\\.", Pattern.DOTALL)),

        /**
         * Type of message. Mail owner did not pick up an item from either a
         * vendor or the bazaar, thus it expired from game. The item can be
         * either bought or put up for sale. Subject line:
         * "Auction Item Expired".
         */
        ITEM_EXPIRED("Auction Item Expired", null, Pattern.compile(
                ".+time, the \"(.+)\" that you.+", Pattern.DOTALL)),

        /**
         * Type of message. Mail owner's item was not sold at either a vendor,
         * or as an auction or instant sale at the bazaar, thus it has fallen
         * back to storage room. Subject line: "Auction Unsuccessful".
         */
        SALE_UNSUCCESSFUL("Auction Unsuccessful", null, Pattern.compile(
                "Your auction of (.+) has been completed.+", Pattern.DOTALL)),

        /**
         * Type of message. Mail owner bought an item from a vendor. Subject
         * line: "Vendor Item Purchased".
         */
        VENDOR_PURCHASE("Vendor Item Purchased", "Vendor", Pattern.compile(
                ".+ auction of \"(.+)\" from \"(.+)\" for (\\d+) .+\\s+.+ " +
                        "at (.+)\\.", Pattern.DOTALL)),

        /**
         * Type of message. An item is sold from one of Mail owner's vendors.
         * Subject line: "Vendor Sale Complete".
         */
        VENDOR_SALE("Vendor Sale Complete", "Vendor", Pattern.compile(
                "Vendor: (.+) has sold (.+) to (.+) for (\\d+) " +
                        "credits\\.\\s+.+at (.+)\\.", Pattern.DOTALL));

        /**
         * A regular expression for this constant.
         */
        private final Pattern re;

        /**
         * A readable description. This is the subject line of an auction mail.
         */
        public final String subject;

        /**
         * Vendor type, or {@code null}. Values are "Bazaar" and "Vendor".
         */
        public final String vendor;

        /**
         * Creates an instance of this constant with the specified values.
         * 
         * @param s the subject line of an auction mail
         * @param v a vendor type, or {@code null}
         * @param r a regular expression
         */
        private Type(String s, String v, Pattern r) {
            this.subject = s;
            this.vendor = v;
            this.re = r;
        }
    }
}
