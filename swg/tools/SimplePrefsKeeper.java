package swg.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import swg.SWGAide;
import swg.SWGConstants;
import swg.crafting.resources.SWGResource;
import swg.gui.SWGFrame;
import swg.gui.SWGInitialize;

/**
 * This class models a simplified {@link Map} which allows for persistent
 * storage of {@link Serializable} objects, but not raw data types. This
 * implementation allows an instance to contain other instances of this type,
 * but it is discouraged to contain an instance in itself.
 * <P>
 * SWGAide exploits the properties of <A href=
 * "http://java.sun.com/javase/6/docs/platform/serialization/spec/serial-arch.html"
 * >Java Object Serialization Specification</A> which ensures that object graphs
 * are maintained through serialization-deserialization.
 * <P>
 * <B>Note: </B>Clients in SWGAide must not by themselves serialize objects
 * which are, or refer to, objects contained in SWGAide's object storage,
 * maintained by {@link SWGFrame#getPrefsKeeper()}. If not properly implemented,
 * "private" serialization-deserialization outside the control of SWGAide's
 * object storage will inject unauthentic doubles which, if mixed with authentic
 * objects, severely will affect or destroy features in SWGAide.
 * <P>
 * Thence, serialized storage outside the control of SWGAide's object storage is
 * permitted <B>only if</B> the stored elements are <B>objects which are not
 * mixed</B> with objects stored by SWGAide's object storage. Also, each such
 * element's objects graph must not contain any object stored in SWGAide's
 * object storage, not at any level or depth.
 * <P>
 * Developers who deem it better to implement private storage must cautiously
 * use the modifier {@code transient} for any objects which are stored by
 * SWGAide, or if any object graphs contain such objects. For deserialization,
 * {@link ObjectInputStream#readObject()} must be implemented in a fashion so
 * that authentic objects are appropriately obtained from SWGAide.
 * <P>
 * <b>Attention: </b>Only objects implementing {@link Serializable} must be
 * contained in an instance of this type and this must be true also for each
 * element's complete object graph. If any object in an object graph does not
 * implement {@link SimplePrefsKeeper} this type cannot be serialized and/or
 * deserialized. This is also true for any future changes done to classes for
 * instances stored in an object stream.
 * <P>
 * This implementation does not allow for {@code null} keys or {@code null}
 * values to be stored. This implementation is thread safe and all modofying
 * methods are synchronized.
 * <P>
 * <B>Key Naming Convention: </B>
 * <P>
 * Keys must be unique. Make up a key which describes the context it logically
 * belongs to, such as "prefsKeeperBackupDate" or "resourceGuardMap". All keys
 * must be recorded in the file {@code swg/Annotations and
 * comments/prefKeys.txt} for general knowledge.
 * <P>
 * <B>Note: </B>From October 2009 the output object stream contains two objects
 * and not just one as the previous version did. The first object is an instance
 * of {@link String} which is {@link SWGConstants#version} from the version of
 * SWGAide used to save the stream. The second object is an instance of this
 * type. The static method {@link #version(File)} returns the string which can
 * be used to determine if the stream is compatible. The member method
 * {@link #getVersion()} returns the version recorded for this instance.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SimplePrefsKeeper implements Serializable {

    /**
     * Serialization version info. Don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = -7684542119186474024L;

    /**
     * The dictionary which contains the stored objects. {@link Hashtable} is in
     * itself synchronized so no access methods have to provide synchronization.
     * 
     * @serial the internal hash table
     */
    private Hashtable<String, Serializable> table;

    /**
     * A text field which denotes which version of SWGAide that saved the output
     * stream. The string is {@link SWGConstants#version} from that version.
     * This field is used to determine if the specified object stream is
     * compatible with the current SWGAide. For this field
     * {@link SWGConstants#version} is serialized and this field is set while
     * loading an object stream.
     */
    private transient String version;

    /**
     * Creates an instance of this type. A private dictionary for storage of
     * keys mapped to values is instantiated.
     */
    public SimplePrefsKeeper() {
        table = new Hashtable<String, Serializable>();
    }

    /**
     * Adds a key/value pair to this instance and returns the previous value of
     * the specified key. If the specified key did not have an associated value
     * {@code null} is returned. If the specified value is {@code null} this
     * implementation removes the key from this instance, as if
     * {@link #remove(String)} was called.
     * <P>
     * <B>Attention: </B>This implementation does not determine if a possible
     * reference graph rooted from the specified value only contains
     * {@link Serializable} instances. Clients must ensure this property,
     * otherwise {@link #store(File)} will fail.
     * 
     * @param key
     *            the key for the value
     * @param value
     *            the value to be stored, if the value is {@code null} the key
     *            is removed from this instance
     * @return the previous value of the specified key, or {@code null}
     * @throws NullPointerException
     *             if the key is {@code null}
     */
    public synchronized Serializable add(String key, Serializable value) {
        if (value == null)
            return table.remove(key);

        return table.put(key, value);
    }

    /**
     * Clears the content stored in this instance.
     */
    public synchronized void clearAll() {
        table.clear();
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this instance contains no mapping for the key.
     * 
     * @param key
     *            the key for the wanted value
     * @return the value to which the specified key is mapped, or {@code null}
     * @throws NullPointerException
     *             if the key is {@code null}
     */
    public synchronized Serializable get(String key) {
        return table.get(key);
    }

    /**
     * Returns the value to which the specified key is mapped, or the supplied
     * default value if this instance contains no mapping for the key. In the
     * latter case this implementation adds the supplied default value to the
     * map as a new mapping.
     * <P>
     * <B>Attention: </B>This implementation does not determine if a possible
     * reference graph rooted from the specified default value only contains
     * {@link Serializable} instances. Clients must ensure that property,
     * otherwise, this instance cannot be serialized and {@link #store(File)}
     * will fail.
     * 
     * @param key
     *            the key for the wanted value
     * @param defaultValue
     *            a default value for the key which will be used only if no
     *            previous value is associated with the key, not {@code null}
     * @return the value to which the specified key is mapped in this instance
     * @throws NullPointerException
     *             if the key or the default value is {@code null}
     */
    public synchronized Serializable get(String key, Serializable defaultValue) {
        Serializable o = table.get(key);
        if (o != null)
            return o;

        if (defaultValue == null)
            throw new NullPointerException("Default value is null, key: "
                + key);

        table.put(key, defaultValue);
        return defaultValue;
    }

    /**
     * Returns the version of SWGAide which was used to save this instance of
     * the preference keeper, which is the {@link SWGConstants#version} from the
     * date the object stream for this instance was saved. If this instance is
     * not loaded from an object stream the the empty string is returned.
     * 
     * @return a version of SWGAide
     */
    public String getVersion() {
        return version == null ? "" : version;
    }

    /**
     * Returns the keys contained in this instance.
     * 
     * @return the set of keys contained in this instance
     * @throws NullPointerException
     *             if the key is {@code null}
     */
    public synchronized Set<String> keySet() {
        return table.keySet();
    }

    /**
     * Removes the specified key (and its corresponding value) from this
     * instance, or nothing if it is not present.
     * 
     * @param key
     *            the key which needs to be removed
     * @return the value to which the key had been mapped in this instance, or
     *         {@code null} if the key did not have a mapping
     */
    public synchronized Serializable remove(String key) {
        return table.remove(key);
    }

    /**
     * Writes this instance to the specified file using
     * {@link ObjectOutputStream}. If the operation was successful {@code true}
     * is returned, otherwise {@code false}. If there is an error a message is
     * written to SWGAide's error log.
     * <P>
     * This implementation writes directly to the identified target file. For
     * safety reasons a client may want to write to a temporary file until this
     * method returns {@code true} or {@code false}.
     * <P>
     * <B>Note: </B>Writing this instance to an {@link ObjectOutputStream}
     * implies that all object graphs rooted from all elements contained in this
     * instance must only be {@link Serializable} objects. Clients must ensure
     * the {@link Serializable} property, otherwise this method fails.
     * <P>
     * <B>Note: </B>From October 2009 the written object stream contains two
     * objects and not just one as the previous version did. The first object is
     * an instance of {@link String} which is {@link SWGConstants#version} from
     * the version of SWGAide used to save the stream. The second object is an
     * instance of this type. The static method {@link #version(File)} returns
     * the string which can be used to determine if the stream is compatible.
     * 
     * @param target
     *            the file to write this instance to, as an object stream
     * @return {@code true} if this instance was successfully written, {@code
     *         false} otherwise
     */
    public synchronized boolean store(File target, String ver) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(target));
            if(ver.length() < 2) {
            	out.writeObject(SWGConstants.version);
            } else {
            	out.writeObject(ver);
            }
            out.writeObject(this);
            return true;
        } catch (Throwable e) {
            SWGAide.printError("SimplePrefsKeeper:store", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) { /* ignore */}
        }
        return false;
    }

    @Override
    public String toString() {
        ZString z = new ZString("SimplePrefsKeeper[");
        return z.app("size=").app(table.size()).app(']').toString();
    }
    
    /**
     * 
     * @param dat
     * @return {@code true} if this instance was successfully written, {@code
     *         false} otherwise
     */
    public synchronized boolean store(File dat) {
    	String v = "0";
    	return store(dat, v);
	}

    /**
     * Loads and returns an instance of this type from the specified file, using
     * {@link ObjectInputStream}. If there is an error it is intercepted and a
     * message is written to SWGAide's error log, and finally the exception is
     * thrown as is.
     * <P>
     * <B>Note: </B>From October 2009 the object output stream contains two
     * objects and not just one as the previous version did. The first object is
     * an instance of {@link String} which is {@link SWGConstants#version} from
     * the version of SWGAide used to save the stream. The second object is an
     * instance of this type. The static method {@link #version(File)} returns
     * the string which can be used to determine if the stream is compatible.
     * <P>
     * This method does not work with older, incompatible object streams. This
     * is due to the deep reaching changes for {@link SWGResource} and
     * sub-classes and supporting types.
     * 
     * @param source the file to read from
     * @return an instance of this type
     * @throws ClassCastException if any of the two objects in the source file
     *         is not {@link String} and {@link SimplePrefsKeeper} in that order
     * @throws ClassNotFoundException if a class for a serialized object cannot
     *         be found or if it cannot be deserialized for some other reason
     * @throws FileNotFoundException if the identified file does not exist or if
     *         it cannot be read
     * @throws IOException if there is any other I/O error
     * @throws Throwable if there is any other error
     */
    public synchronized static SimplePrefsKeeper load(File source)
            throws Throwable {

        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(source));
            String v = (String) in.readObject();

            SWGInitialize.updateDialog(v);

            SimplePrefsKeeper pk = (SimplePrefsKeeper) in.readObject();
            pk.version = v;
            return pk;
        } catch (Throwable e) {
            SWGAide.printError("SimplePrefsKeeper:load", e);
            throw e;
        } finally {
            try {
                in.close();
            } catch (Exception e1) {/* ignore */
            }
        }
    }

    /**
     * Loads and returns just the first object from the specified object stream.
     * It is assumed that the file is an object stream stored by SWGAide via an
     * instance of this type. From October 2009 such a file has an instance of
     * {@link String} as its first object, and an instance of
     * {@link SimplePrefsKeeper} as its second object. The string is
     * {@link SWGConstants#version} from the version of SWGAide used to store
     * the file. If the specified file does not contain a string as its first
     * object {@code null} is returned which denotes that the content of the
     * file is incompatible with the current version of SWGAide.
     * 
     * @param file
     *            the file to return a version string for
     * @return a string which reads {@link SWGConstants#version} from the date
     *         the specified file was stored, or {@code null} which denotes
     *         incompatibility
     */
    public static synchronized String version(File file) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            Object o = in.readObject(); // just the first object
            if (String.class.isAssignableFrom(o.getClass()))
                return (String) o;

            return null;
        } catch (Throwable e) {
            SWGAide.printError("SimplePrefsKeeper:version", e);
            return null;
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (Exception e1) {/* ignore */}
        }
    }

}
