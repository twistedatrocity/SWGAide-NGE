package swg.crafting.resources;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import swg.crafting.SWGWeights;
import swg.gui.SWGFrame;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.swgcraft.SWGResourceManager;
import swg.tools.ZString;

/**
 * This type represents a {@link Set} of <I>known resources</I>. More formally,
 * this type models the mathematical <I>set&nbsp;</I> abstraction: at most one
 * unique {@link SWGKnownResource} per known resource in SWG must be contained
 * in this set. Instances of known resources are in themselves unique,
 * {@link SWGResourceManager} ensures that property and this implementation
 * depends on the resource manager for the uniqueness property.
 * <P>
 * Unlike {@link Set} elements are ordered similar to {@link List}. This type
 * disallows {@code null} elements. Utility methods are provided that allow for
 * manipulation of the set, making it simpler to locate the desired resource(s),
 * etc. For reference, see {@link SWGResourceManager} for similar methods that
 * operate at the complete cache of known resources in SWGAide.
 * <P>
 * This type provides thread safe methods implemented with the least intrusive
 * synchronization. However, methods which are non-modifying ((such as
 * {@link #size()}) or {@link #contains(Object)}), and getter methods, are not
 * thread safe. Furthermore, there is no guarantee that a return value from such
 * methods are valid in the case concurrent threads change the content.
 * <P>
 * Some methods return a set of known resources, if the set is empty an
 * immutable {@link Collections#emptySet()} is returned.
 * <P>
 * <B>Persistent Storage</B>
 * <P>
 * <B>NOTE: </B>Clients <B><U>must never store</U></B> instances of known
 * resources on their own. Instances of known resources <B>must be stored by
 * SWGAide</B>: use the {@link SWGFrame#getPrefsKeeper()}.
 * <P>
 * SWGAide exploits the properties of <A href=
 * "http://java.sun.com/javase/6/docs/platform/serialization/spec/serial-arch.html"
 * >Java Object Serialization Specification</A> which ensures that reference
 * graphs are maintained through serialization-and-deserialization; no spurious
 * objects will occur. Thus, any "private" storage outside the serialized stream
 * will severely break the uniqueness property.
 * <P>
 * See {@link SWGResource} for a complete discussion on resources and the very
 * strict rules on persistent storage, also {@link SWGKnownResource}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public final class SWGResourceSet implements Set<SWGKnownResource>,
    Serializable {

    /**
     * An immutable constant for an empty set, {@link Collections#emptySet()}.
     */
    public static final SWGResourceSet EMPTY = new SWGResourceSet(0);

    /**
     * Serialization version info, don't meddle with this or break the
     * deserialization.
     */
    private static final long serialVersionUID = 5352665685156808330L;

    static {
        EMPTY.storage = Collections.emptyList();
    }

    /**
     * The internal storage for resources.
     * <P>
     * This implementation uses a {@link List} for simplicity and its decent
     * performance. That is since resource objects will be searched for in
     * several fashions and that it usually exists just a moderate number of
     * elements to marshal.
     * <P>
     * This class manages serialization and deserialization, thus the
     * implementation can be replaced at any time.
     */
    private transient List<SWGKnownResource> storage;

    /**
     * Creates an empty set for known resources, with an initial capacity of 10.
     */
    public SWGResourceSet() {
        this(10);
    }

    /**
     * Creates a set containing the known resource objects in the provided
     * collection. This implementation maintains the property of a {@link Set}.
     * 
     * @param knownResources
     *            the collection of known resource to add to this instance. The
     *            collection is assumed to contain at most one element of any
     *            instance and they must not be {@code null}.
     * @throws NullPointerException
     *             if an element is {@code null}
     * @throws IllegalArgumentException
     *             if some property of an element prevents it from being added
     *             to this set
     */
    public SWGResourceSet(Collection<SWGKnownResource> knownResources) {
        this((int) (knownResources.size() * 1.2));
        addAll(knownResources);
    }

    /**
     * Creates an empty set for known resources with the specified initial
     * capacity.
     * 
     * @param initialCapacity
     *            the initial capacity of the set
     * @throws IllegalArgumentException
     *             if the specified initial capacity is negative
     */
    public SWGResourceSet(int initialCapacity) {
        storage = new ArrayList<SWGKnownResource>(initialCapacity);
    }

    public boolean add(SWGKnownResource e) {
        if (e == null)
            throw new NullPointerException("Argument is null");

        // Since not two instances of SWGKnownResource identifies the same
        // resource, no matter which of the three identifiers that is used, we
        // can assume that the equals operator == works for contains(e)

        synchronized (storage) {
            if (!storage.contains(e))
                return storage.add(e);
        }
        return false;
    }

    public boolean addAll(Collection<? extends SWGKnownResource> c) {
        // check content before adding any element to this instance
        for (SWGKnownResource kr : c) {
            if (kr == null)
                throw new NullPointerException("Element is null");
        }
        synchronized (storage) {
            boolean ret = false;
            for (SWGKnownResource kr : c)
                ret |= add(kr);
            return ret;
        }
    }

    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
    }

    public boolean contains(Object o) {
        return storage.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return storage.containsAll(c);
    }

    /**
     * Convenience method that returns this instance. If this instance is empty
     * {@link #EMPTY} is returned.
     * 
     * @return this instance, or {@link #EMPTY}
     */
    public SWGResourceSet get() {
        return this.isEmpty()
            ? EMPTY
            : this;
    }

    /**
     * Returns the element at the specified position in this ordered set.
     * 
     * @param index
     *            index of the element to return
     * @return the element at the specified position in this ordered set
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >= size())
     */
    public SWGKnownResource get(int index) {
        return storage.get(index);
    }

    /**
     * Returns an instance of a known resource identified by {@code name}
     * <I>and&nbsp;</I> {@code galaxy}. A resource name is unique for its
     * galaxy. The name must be a properly capitalized. If the requested
     * instance is not contained in this set {@code null} is returned.
     * <P>
     * <B>Note: </B>If {@link SWGResource#id()} is known
     * {@link #getByID(long)} is more efficient.
     * 
     * @param name
     *            a proper, capitalized resource name
     * @param galaxy
     *            a galaxy constant
     * @return an instance of a known resource identified by its name and
     *         galaxy, {@code null} if none is found
     * @throws IllegalArgumentException
     *             if name is invalid
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    public SWGKnownResource getBy(String name, SWGCGalaxy galaxy) {
        if (name == null || galaxy == null)
            throw new NullPointerException("An argument is null");
        if (name.length() < 3)
            throw new IllegalArgumentException("Invalid name: " + name + ':'
                + galaxy);

        for (SWGKnownResource kr : storage)
            if (kr.galaxy().equals(galaxy) && kr.getName().equals(name))
                return kr;

        return null;
    }

    /**
     * Returns an instance of a known resource identified by {@code name}
     * <I>and&nbsp;</I> {@code type}. The risk for a doubled combination at
     * another galaxy is negligible. The arguments must be a proper, capitalized
     * resource name and a spawnable resource class. If the requested instance
     * is not contained in this set {@code null} is returned.
     * <P>
     * <B>Note: </B>If {@link SWGResource#id()} is known
     * {@link #getByID(long)} is more efficient. If galaxy is known it is a tad
     * safer to use {@link #getBy(String, SWGCGalaxy)}.
     * 
     * @param name
     *            a proper, capitalized resource name
     * @param type
     *            a spawnable resource class
     * @return an instance of a known resource identified by its name and
     *         resource class, {@code null} if none is found
     * @throws IllegalArgumentException
     *             if any of the arguments is invalid
     * @throws NullPointerException
     *             if any of the arguments is {@code null}
     */
    public SWGKnownResource getBy(String name, SWGResourceClass type) {
        if (name.length() < 3)
            throw new IllegalArgumentException("Invalid name: " + name + ':'
                + type);
        if (!type.isSpawnable())
            throw new IllegalArgumentException("Not spawnable: " + type + ':'
                + name);

        for (SWGKnownResource kr : storage)
            if (kr.rc() == type && kr.getName().equals(name))
                return kr;

        return null;
    }

    /**
     * Returns an instance of a known resource identified by {@code swgcraftID}.
     * The argument is a unique SWGCraft resource ID. If the requested instance
     * is not contained in this set {@code null} is returned.
     * 
     * @param swgcraftID
     *            a unique SWGCraft resource ID identifying the known resource
     * @return an instance of a known resource, {@code null} if none is found
     * @throws IllegalArgumentException
     *             if {@literal swgcraftID <= 0}
     */
    public SWGKnownResource getByID(long swgcraftID) {
        if (swgcraftID <= 0)
            throw new IllegalArgumentException("Invalid ID: " + swgcraftID);

        for (SWGKnownResource kr : storage)
            if (kr.id() == swgcraftID)
                return kr;

        return null;
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    /**
     * Returns an iterator over the elements in this set in proper sequence.
     * This implementation returns a {@link ListIterator}.
     * 
     * @return an iterator over the elements in this set in proper sequence, a
     *         {@link ListIterator}
     */
    public Iterator<SWGKnownResource> iterator() {
        return storage.iterator();
    }

    /**
     * Helper method for {code SWGResourceManager} and {code SWGResourceCache}
     * so they can add resources without the safety check in performed by
     * {@link #add(SWGKnownResource)}. This method must only be called when
     * there is no doubts about robustness.
     * 
     * @param res
     *            the resource to add
     */
    void privateAdd(SWGKnownResource res) {
        // this stealth adder is just for performance
        storage.add(res);
    }

    /**
     * Deserialize a {@link SWGResourceSet} instance.
     * 
     * @see #writeObject(ObjectOutputStream)
     * @param ois
     *            the stream to read from
     * @throws ClassNotFoundException
     *             if the class of a serialized object could not be found.
     * @throws IOException
     *             if an I/O error occurs
     */
    private void readObject(ObjectInputStream ois) throws IOException,
        ClassNotFoundException {

        ois.defaultReadObject();

        @SuppressWarnings("unchecked")
        List<SWGKnownResource> lst = (List<SWGKnownResource>) ois.readObject();

        // in the future, create the appropriate type of storage is changed
        // e.g. storage = new ArrayList<SWGKnownResource>(lst);

        storage = lst;
    }

    public boolean remove(Object o) {
        synchronized (storage) {
            return storage.remove(o);
        }
    }

    public boolean removeAll(Collection<?> c) {
        synchronized (storage) {
            return storage.removeAll(c);
        }
    }

    public boolean retainAll(Collection<?> c) {
        synchronized (storage) {
            return storage.retainAll(c);
        }
    }

    public int size() {
        return storage.size();
    }

    /**
     * Sorts this set of resources by the specified comparator. This
     * implementation is thread safe and locks on the internal collection.
     * However, non-modifying methods can query this instance.
     * <P>
     * <B>Note: </B>This method mutates the content of this instance. If this is
     * not acceptable a copy or an array from this instance should be used.
     * 
     * @param comparator
     *            the comparator object to sort this instance by
     */
    public void sort(Comparator<SWGKnownResource> comparator) {
        synchronized (storage) {
            Collections.sort(storage, comparator);
        }
    }

    /**
     * Returns from this set a subset of resources which all meets the
     * comparator. In particular, the specified object's method
     * {@link Comparable#compareTo(Object)} returns 0 for all elements contained
     * in the returned set. If no resource meet the requirements {@link #EMPTY}
     * is returned.
     * 
     * @param comparable
     *            an object of type {@link Comparable} which is used to accept
     *            or deny resources to be contained in the returned set
     * @return a set of matching resources, or {@link #EMPTY}
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public SWGResourceSet subsetBy(Comparable<SWGKnownResource> comparable) {
        if (comparable == null)
            throw new NullPointerException("Argument is null");

        SWGResourceSet result = new SWGResourceSet(size());
        for (SWGKnownResource kr : storage)
            if (comparable.compareTo(kr) == 0)
                result.storage.add(kr);

        if (result.storage.isEmpty())
            return EMPTY;
        return result;
    }

    /**
     * Returns from this set a subset of known resources filtered for the
     * identified planet. More formally, the elements contained in the returned
     * set are known at the identified planet, they are either in spawn or are
     * recently depleted. Should no resource meet the requirements
     * {@link #EMPTY} is returned.
     * 
     * @param planet
     *            the planet for which to return resources
     * @return a set of matching resources, or {@link #EMPTY}
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public SWGResourceSet subsetBy(SWGPlanet planet) {
        if (planet == null)
            throw new NullPointerException("Argument is null");

        SWGResourceSet result = new SWGResourceSet(size());
        for (SWGKnownResource kr : storage) {
            List<SWGPlanet> plist = kr.availability();
            for (SWGPlanet p : plist) {
                if (p == planet) {
                    result.storage.add(kr); // surpass our checkpoints
                    break; // break inner loop, one instance of kr is enough
                }
            }
        }
        if (result.storage.isEmpty())
            return EMPTY;
        return result;
    }

    /**
     * Returns from this set a subset of known resources that are subclasses of
     * the argument. More formally, the elements contained in the returned set
     * are known resources of which each instance's type field equals or is a
     * subclass of the argument. Should no resource meet the requirements
     * {@link #EMPTY} is returned.
     * <P>
     * The argument can be obtained from {@link SWGResourceClass} by ID, name,
     * or token.
     * 
     * @param type
     *            the resources class to filter through
     * @return a set of known resources which type is determined by the
     *         argument, or {@link #EMPTY}
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public SWGResourceSet subsetBy(SWGResourceClass type) {

        Class<? extends SWGResourceClass> typeCls = type.getClass();
        SWGResourceSet result = new SWGResourceSet(size());

        for (SWGKnownResource kr : storage) {
            if (typeCls.isAssignableFrom(kr.rc().getClass()))
                result.storage.add(kr); // surpass our checkpoints
        }
        if (result.storage.isEmpty())
            return EMPTY;
        return result;
    }

    /**
     * Returns from this set a subset of known resources that meet the
     * identified filter, considering the boolean argument. Should no resource
     * pass the filter {@link #EMPTY} is returned.
     * 
     * @param filter
     *            the filter to sift the resources through
     * @param all
     *            {@code true} if <I>all&nbsp;</I> expected values must meet the
     *            filter, {@code false} if it suffices with at least <I>one</I>
     * @return a set of resources that meet the identified filter while also
     *         considering the boolean arguments, or {@link #EMPTY}
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public SWGResourceSet subsetBy(SWGResourceFilter filter, boolean all) {
        SWGResourceSet result = new SWGResourceSet(size());
        for (SWGKnownResource kr : storage) {
            if (filter.isBetter(kr, all))
                result.storage.add(kr); // surpass our checkpoints
        }
        if (result.storage.isEmpty())
            return EMPTY;
        return result;
    }

    /**
     * Returns from this set a subset of known resources that meet the
     * arguments. More formally, each elements in the returned set is of the
     * same resource class as {@code capsFrom} or a sub-type thereof, and they
     * have a computed weight equal to or better than the specified threshold.
     * Each element's weight is computed from {@code weights} while considering
     * {@code capsFrom} and {@code zeroIsMax}, compare
     * {@link SWGWeights#rate(SWGResource, SWGResourceClass, boolean)}.
     * Should no resource meet the requirements {@link #EMPTY} is returned.
     * <P>
     * Valid arguments are a valid instance of {@link SWGWeights}, a non-{@code
     * null} instance of {@link SWGResourceClass}, and a threshold in the range
     * [0.0 1000.0].
     * <P>
     * The argument {@code capsFrom} can be obtained from
     * {@link SWGResourceClass} by ID, name, or token.
     * 
     * @param weights
     *            the values to compute the weight of the resources from
     * @param capsFrom
     *            the resource class to derive caps from
     * @param zeroIsMax
     *            {@code true} if a zero-value should be treated as its capped
     *            value, otherwise {@code false}
     * @param threshold
     *            the minimum weight for elements in the returned set, in the
     *            range [0.0 1000.0]
     * @return a set of resources that meet the arguments, or {@link #EMPTY}
     * @throws IllegalArgumentException
     *             if an argument is invalid
     * @throws NullPointerException
     *             if any of the object arguments is {@code null}
     */
    public SWGResourceSet subsetBy(SWGWeights weights,
        SWGResourceClass capsFrom, boolean zeroIsMax, double threshold) {

        if (threshold > 1000) {
        	threshold = 1000;
        }
    	
    	if (!weights.isValid())
            throw new IllegalArgumentException("Invalid weights: " + weights);
        if (threshold < 0 || threshold > 1000)
            throw new IllegalArgumentException("Invalid threshold: "
                + threshold);

        Class<? extends SWGResourceClass> typeCls = capsFrom.getClass();
        SWGResourceSet result = new SWGResourceSet(size());

        for (SWGKnownResource kr : storage) {
            if (typeCls.isAssignableFrom(kr.rc().getClass())) {
                double w = weights.rate(kr, capsFrom, zeroIsMax);
                if (w >= threshold)
                    result.storage.add(kr); // surpass our checkpoints
            }
        }
        if (result.storage.isEmpty())
            return EMPTY;
        return result;
    }

    public Object[] toArray() {
        return storage.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return storage.toArray(a);
    }

    @Override
    public String toString() {
        ZString z = new ZString(getClass().getSimpleName());
        return z.app(storage.toString()).toString();
    }

    /**
     * Serialize the logical state of this {@link SWGResourceSet} instance.
     * 
     * @see #readObject(ObjectInputStream)
     * @param ous
     *            the stream to write to
     * @throws IOException
     *             if I/O errors occur while writing to the underlying stream
     * @serialData A logical {@link List} of the resource objects contained in
     *             this instance. Even if this instance is empty a list is
     *             emitted.
     */
    private void writeObject(ObjectOutputStream ous) throws IOException {
        // in the future, create a list if type of storage is changed
        List<SWGKnownResource> lst = storage;

        ous.defaultWriteObject();
        ous.writeObject(lst);
    }
}
