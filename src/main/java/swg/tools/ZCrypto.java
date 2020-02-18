package swg.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import swg.SWGAide;
import swg.SWGConstants;

/**
 * This class provides plain utility methods for cryptography. For example to
 * compute the md5sum for files, strings, and byte buffers, and to encrypt and
 * decipher serializable objects.
 * <p>
 * <b>md5sum: </b>An md5sum is a compact finger print, or a hash value, 32 bytes
 * long, that has very low likelihood to equal the md5sum of another file. See
 * <a href="http://en.wikipedia.org/wiki/Md5sum">WikiPedia md5sum</a>.
 * <p>
 * For numerous larger files, please consider <a
 * href="http://www.twmacinta.com/myjava/fast_md5.php">Fast MD5</a>.
 * 
 * @author Simon Gronlund <a href="mailto:simongronlund@gmail.com">Simon
 *         Gronlund</a> aka Europe-Chimaera.Zimoon
 */
public class ZCrypto {

    /**
     * The DES key object for this class
     */
    private static DESKeySpec dsk;

    /**
     * The factory for secret keys for this object
     */
    private static SecretKeyFactory kf;

    /**
     * The message digest instance for MD5
     */
    private static MessageDigest messageDigestInstance = null;

    /**
     * Returns a DES encrypted version of <code>obj</code>
     * 
     * @param obj
     *            the object to encrypt
     * @return an encrypted version of <code>obj</code>
     * @throws Exception
     *             something went wrong with something in the encryption process
     */
    public static SealedObject cipher(Serializable obj) throws Exception {
        SecretKey sk = getKey();

        Cipher dc = Cipher.getInstance("DES/ECB/PKCS5Padding");
        dc.init(Cipher.ENCRYPT_MODE, sk);

        return new SealedObject(obj, dc);
    }

    /**
     * Returns a deciphered object from <code>sealed</code>
     * 
     * @param sealed
     *            the sealed object to decipher
     * @return the deciphered object
     * @throws Exception
     *             something went wrong with something in the deciphering
     *             process
     */
    public static Object decipher(SealedObject sealed) throws Exception {
        SecretKey sk = getKey();

        Cipher dc = Cipher.getInstance("DES/ECB/PKCS5Padding");
        dc.init(Cipher.DECRYPT_MODE, sk);

        return sealed.getObject(sk);
    }

    /**
     * Returns <code>text</code> encrypted with the AES (rijndael) algorithm
     * using <code>key</code> and <code>iv</code> retrieved from SWGCraft
     * 
     * @param text
     *            the text do encrypt
     * @param key
     *            the encryption key
     * @param iv
     *            the init vector to use in the algorithm
     * @return the encrypted string
     */
    public static String encryptedAES(String text, String key, String iv) {
        SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
        IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            return new String(toHexString(cipher.doFinal(padChar(text,
                    '\u0000', 16).getBytes())));
        } catch (Exception e) {
            SWGAide.printError("ZCrypto:encryptedAES: ", e);
        }
        return text;
    }

    /**
     * Returns a secret key for use within this object
     * 
     * @return a secret key for use within this object
     * @throws Exception
     *             an error
     */
    private static SecretKey getKey() throws Exception {
        if (dsk == null) {
            dsk = new DESKeySpec(SWGConstants.cKey);
            kf = SecretKeyFactory.getInstance("DES");
        }
        return kf.generateSecret(dsk);
    }

    /**
     * Computes the md5sum for the file given as argument
     * 
     * @param args
     *            the name for the file to scan
     * @throws Exception
     *             an I/O error occurred
     */
    public static void main(String... args) throws Exception {
        if (args.length <= 0) {
            System.err.println("Usage:");
            System.err.println("    java ZCrypto <file-name>");
            System.err.println("    java ZCrypto <string>");
            System.err.println("Returns the md5sum (finger print) for "
                    + "the file/string");
            System.exit(1);
        }
        File f = new File(args[0]);
        if (f.exists()) System.out.println(md5sum(f) + " " + args[0]);
        else System.out.println(md5sum(args[0]) + " \"" + args[0] + "\"");
    }

    /**
     * Returns the md5sum for <code>buf</code>
     * 
     * @param bytes
     *            the byte buffer for which to compute the md5sum
     * @return the md5sum as string
     */
    public static String md5sum(byte[] bytes) {
        return md5sum(bytes, -1);
    }

    /**
     * Returns the md5sum for <code>bytes</code> up to <code>len</code>
     * number of bytes. Note that limiting the number of bytes scanned will not
     * give the same finger print as scanning the full length of the same
     * buffer. This may, however, be useful for faster finger prints to verify
     * internal projects or when surely any differences will show up within the
     * limited length.
     * 
     * @param bytes
     *            the byte buffer for which to compute the md5sum
     * @param len
     *            the number of bytes of the buffer to maximally scan, or a
     *            negative value if unlimited
     * @return the md5sum as string
     */
    public static String md5sum(byte[] bytes, int len) {
        if (messageDigestInstance == null) {
            try {
                messageDigestInstance = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        messageDigestInstance.reset();

        byte[] md5sumAsHex;

        if (len < 0) {
            md5sumAsHex = messageDigestInstance.digest(bytes);
        } else {
            messageDigestInstance.update(bytes, 0, (len > bytes.length)
                    ? bytes.length
                    : len);
            md5sumAsHex = messageDigestInstance.digest();
        }

        return toHexString(md5sumAsHex);
    }

    /**
     * Returns the md5sum for <code>file</code>
     * 
     * @param file the file for which to compute the md5sum
     * @return the md5sum as string
     * @throws Exception an error occurred
     */
    public static String md5sum(File file) throws Exception {
        ZReader sr = ZReader.newByteReader(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len = 0;
        while ((len = sr.bytes(buf)) >= 0)
            bos.write(buf, 0, len);

        String md5sum = md5sum(bos.toByteArray(), bos.size());

        sr.close();
        bos.close();

        return md5sum;
    }

    /**
     * Returns the md5sum for <code>text</code>
     * 
     * @param text
     *            the text for which to compute a md5sum
     * @return the md5sum as string
     */
    public static String md5sum(String text) {
        try {
            return md5sum(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Pads <code>text</code> with <code>padChar</code> up to a multiple of
     * <code>length</code>. If for example <code>text</code> is 7
     * characters long and <code>length</code> is 5 the returned string will
     * be 10 characters long
     * 
     * @param text
     *            the text to pad
     * @param padChar
     *            the character to pad with
     * @param length
     *            pads up to length or a multiple of length
     * @return a string which is a multiple of <code>length</code> long,
     *         padded with <code>padChar</code>
     */
    private static String padChar(String text, char padChar, int length) {
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() % length != 0) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    /**
     * Returns a string with the hexadecimal value for <code>byte</code>
     * 
     * @param hByte
     *            the byte to hexify
     * @return the hex code for <code>byte</code> as a string
     */
    public static String toHexString(byte hByte) {
        StringBuilder sb = new StringBuilder();

        // a byte is always two hex digits (nibbles)
        // nibble one
        char ch = (char) ((hByte >> 4) & 0xf);
        if (ch > 9) ch = (char) ((ch - 10) + 'a');
        else ch = (char) (ch + '0');
        sb.append(ch);

        // nibble two
        ch = (char) (hByte & 0xf);
        if (ch > 9) ch = (char) ((ch - 10) + 'a');
        else ch = (char) (ch + '0');
        sb.append(ch);

        return sb.toString();
    }

    /**
     * Returns a string with the hexadecimal values for <code>bytes</code>
     * 
     * @param bytes
     *            the bytes array to hexify
     * @return the hexadecimal values for <code>bytes</code> as a string
     */
    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            sb.append(toHexString(bytes[i]));
        }
        String s = sb.toString();
        return s;
    }
}