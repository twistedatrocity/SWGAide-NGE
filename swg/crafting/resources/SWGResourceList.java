package swg.crafting.resources;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import swg.model.SWGCGalaxy;
import swg.tools.ZString;

/**
 * This type is a light-weight convenience container for any kind of resource
 * objects. This type implements {@link List} and it can contain instances of
 * {@link SWGKnownResource} and {@link SWGMutableResource}, or a mix of the two.
 * Utility methods are provided that allow for manipulation of a list of this
 * type, making it simpler to locate the desired resource(s), etc. See section
 * named <I>Limitations</I>.
 * <P>
 * This type provides thread safe methods implemented with the least intrusive
 * synchronization. Methods that are non-intrusive (such as {@link #size()}), or
 * methods that test a condition and return a {@code boolean} (such as
 * {@link #contains(Object)}), or the methods getXX and sublistXX, are not
 * thread safe. However, there is no guarantee that return values from such
 * methods are valid in the case concurrent threads change the content.
 * <P>
 * A list of this type <B>must never be stored</B> persistently. See
 * {@link SWGResource} for a complete discussion on resources and the very
 * strict rules for handling resource objects. For storage, rather use
 * {@link SWGResourceSet}.
 * <P>
 * <B>LIMITATIONS</B>
 * <P>
 * This implementation does not implement all methods for {@link List}, these
 * methods are clearly documented.
 * <P>
 * This type does not robustly guard against doubles but executes some plain
 * checks. More specifically, equality is just determined by reference identity.
 * Only adder methods also checks the SWGCraft ID. However, see
 * {@link #indexOf(SWGResource, Comparator)}.
 * <P>
 * As mentioned, this implementation provides a limited synchronization, there
 * is no guarantee against concurrent changes, between consecutive calls. This
 * should not cause a problem since this type is thought to be used in very
 * narrow, controlled scopes.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGResourceList implements List<SWGResource> {

    /**
     * An immutable constant for an empty list, {@link Collections#emptyList()}.
     */
    public static final SWGResourceList EMPTY = new SWGResourceList(0);
    static {
        EMPTY.storage = Collections.emptyList();
    }

    /**
     * The internal storage for resources.
     */
    private List<SWGResource> storage;

    /**
     * Creates an empty list for resources, with an initial capacity of 10.
     */
    public SWGResourceList() {
        this(10);
    }

    /**
     * Creates a list containing the resource objects in the provided
     * collection. No duplicates nor {@code null} elements are added to this
     * instance but are silently ignored.
     * 
     * @param resources
     *            the collection of resources to add to this instance. Elements
     *            that are {@code null} or duplicates are ignored.
     * @throws IllegalArgumentException
     *             if some property of an element prevents it from being added
     *             to this list
     */
    public SWGResourceList(Collection<SWGResource> resources) {
        this((int) (resources.size() * 1.2));
        addAll(resources);
    }

    /**
     * Creates an empty list for resources with the specified initial capacity.
     * 
     * @param initialCapacity
     *            the initial capacity of the list
     * @throws IllegalArgumentException
     *             if the specified initial capacity is negative
     */
    public SWGResourceList(int initialCapacity) {
        storage = new ArrayList<SWGResource>(initialCapacity);
    }

    /**
     * Unimplemented.
     * 
     * @throws UnsupportedOperationException
     *             if this method is called
     */
    public void add(int index, SWGResource element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds an element to this instance. If the element is {@code null}, or if
     * the element has its SWGCraft ID set and itself or another object with
     * that ID already exists in this instance, it is ignored and {@code false}
     * is returned. For further documentation...
     * 
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(SWGResource e) {
        if (e == null || (e.id() > 0 && getByID(e.id()) != null))
            return false;

        synchronized (storage) {
            if (!storage.contains(e))
                return storage.add(e);
        }
        return false;
    }

    /**
     * Adds all elements in the specified collection to this instance. If an
     * element is {@code null}, or if the element has its SWGCraft ID set and
     * itself or another object with that ID already exists in this instance, it
     * is silently ignored. For further documentation...
     * 
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends SWGResource> c) {
        boolean ret = false;
        synchronized (storage) {
            for (SWGResource resource : c) {
                ret |= add(resource);
            }
        }
        return ret;
    }

    /**
     * Unimplemented.
     * 
     * @throws UnsupportedOperationException
     *             if this method is called
     */
    public boolean addAll(int index, Collection<? extends SWGResource> c) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#clear()
     */
    public void clear() {
        synchronized (storage) {
            storage.clear();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return storage.contains(o);
    }

    /**
     * Unimplemented.
     * 
     * @throws UnsupportedOperationException
     *             if this method is called
     */
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#get(int)
     */
    public SWGResource get(int index) {
        return storage.get(index);
    }

    /**
     * Returns an instance of a resource identified by {@code swgcraftID}. The
     * argument is a unique SWGCraft resource ID. If the requested instance is
     * not contained in this list {@code null} is returned.
     * 
     * @param swgcraftID
     *            a unique SWGCraft resource ID identifying the resource
     * @return an instance of a resource, {@code null} if none is found
     * @throws IllegalArgumentException
     *             if {@literal swgcraftID <= 0}
     */
    public SWGResource getByID(long swgcraftID) {
        if (swgcraftID <= 0)
            throw new IllegalArgumentException("Invalid ID: " + swgcraftID);

        for (SWGResource r : storage)
            if (r.id() == swgcraftID)
                return r;

        return null;
    }

    /**
     * Returns an instance of a resource identified by {@code name}
     * <I>and&nbsp;</I> {@code galaxy}. A resource name is unique for its
     * galaxy. The name must be a properly capitalized. If the requested
     * instance is not contained in this list {@code null} is returned.
     * <P>
     * <B>Note: </B>If {@link SWGResource#id()} is known
     * {@link #getByID(long)} is more efficient.
     * 
     * @param name
     *            a proper, capitalized resource name
     * @param galaxy
     *            a galaxy constant
     * @return an instance of a resource identified by its name and galaxy,
     *         {@code null} if none is found
     * @throws IllegalArgumentException
     *             if name is invalid
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    public SWGResource getByNameAndGalaxy(String name, SWGCGalaxy galaxy) {
        if (galaxy == null || name == null)
            throw new IllegalArgumentException("An argument is null");
        if (name.length() < 3)
            throw new IllegalArgumentException("Invalid name: " + name + ':'
                + galaxy);

        for (SWGResource r : storage)
            if (r.galaxy().equals(galaxy) && r.getName().equals(name))
                return r;

        return null;
    }

    /**
     * Returns an instance of a resource identified by {@code name}
     * <I>and&nbsp;</I> {@code type}. The risk for a doubled combination at
     * another galaxy is negligible. The arguments must be a proper, capitalized
     * resource name and a spawnable resource class. If the requested instance
     * is not contained in this list {@code null} is returned.
     * <P>
     * <B>Note: </B>If {@link SWGResource#id()} is known
     * {@link #getByID(long)} is more efficient. If galaxy is known it is a tad
     * safer to use {@link #getByNameAndGalaxy(String, SWGCGalaxy)}.
     * 
     * @param name
     *            a proper, capitalized resource name
     * @param type
     *            a spawnable resource class
     * @return an instance of a resource identified by its name and resource
     *         class, {@code null} if none is found
     * @throws IllegalArgumentException
     *             if any of the arguments is invalid
     * @throws NullPointerException
     *             if any of the arguments is {@code null}
     */
    public SWGResource getByNameAndType(String name, SWGResourceClass type) {
        if (name.length() < 3)
            throw new IllegalArgumentException("Invalid name: " + name + ':'
                + type);
        if (!type.isSpawnable() && !type.isSpaceOrRecycled())
            throw new IllegalArgumentException("Not spawnable: " + type + ':'
                + name);

        for (SWGResource r : storage)
            if (r.rc() == type && r.getName().equals(name))
                return r;

        return null;
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        if (o instanceof SWGResource) {
            return storage.indexOf(o);
        }
        return -1;
    }

    /**
     * Returns the index of the first occurrence of the specified resource in
     * this list using the specified {@link Comparator} to determine equality.
     * If this list does not contain the element -1 is returned.
     * 
     * @param o
     *            the resource object to find the index for
     * @param comp
     *            the comparator with which to establish identity
     * @return the index of the specified resource in this list, or -1
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    public int indexOf(SWGResource o, Comparator<SWGResource> comp) {
        for (int i = 0; i < storage.size(); ++i) {
            SWGResource r = storage.get(i);
            if (r == o || comp.compare(r, o) == 0)
                return i;
        }
        return -1;
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty() {
        return storage.isEmpty();
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#iterator()
     */
    public Iterator<SWGResource> iterator() {
        return storage.iterator();
    }

    /**
     * Unimplemented.
     * 
     * @throws UnsupportedOperationException
     *             if this method is called
     */
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unimplemented.
     * 
     * @throws UnsupportedOperationException
     *             if this method is called
     */
    public ListIterator<SWGResource> listIterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unimplemented.
     * 
     * @throws UnsupportedOperationException
     *             if this method is called
     */
    public ListIterator<SWGResource> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Prohibits deserialization.
     * 
     * @param ois
     *            the object stream
     * @throws NotSerializableException
     *             if anybody tries to deserialize this object
     */
    private void readObject(ObjectInputStream ois)
        throws NotSerializableException {
        // It should be impossible to get here since this class does not
        // implement the Serializable interface, but ...
        throw new NotSerializableException("This container is not for storage");
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#remove(int)
     */
    public SWGResource remove(int index) {
        synchronized (storage) {
            return storage.remove(index);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        synchronized (storage) {
            return storage.remove(o);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> c) {
        synchronized (storage) {
            return storage.removeAll(c);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> c) {
        synchronized (storage) {
            return storage.retainAll(c);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#set(int, java.lang.Object)
     */
    public SWGResource set(int index, SWGResource element) {
        if (element == null)
            throw new NullPointerException("Null elements are not allowed");

        synchronized (storage) {
            return storage.set(index, element);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#size()
     */
    public int size() {
        return storage.size();
    }

    /**
     * Unimplemented.
     * 
     * @throws UnsupportedOperationException
     *             if this method is called
     */
    public List<SWGResource> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns from this list a sublist of resources that are subclasses of the
     * argument. More formally, the elements contained in the returned list are
     * resources of which each instance's type field equals or is a subclass of
     * the argument.
     * <P>
     * The argument can be obtained from {@link SWGResourceClass} by ID, name,
     * or token.
     * 
     * @param type
     *            the resources class to filter through
     * @return a list of resources which type is determined by the argument, or
     *         an empty list
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    public SWGResourceList sublistByResourceClass(SWGResourceClass type) {

        Class<? extends SWGResourceClass> typeCls = type.getClass();
        SWGResourceList result = new SWGResourceList(size());

        for (SWGResource r : storage) {
            if (typeCls.isAssignableFrom(r.rc().getClass()))
                result.storage.add(r); // surpass our checkpoints
        }
        if (result.storage.isEmpty())
            return EMPTY;
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        return storage.toArray();
    }

    /*
     * (non-Javadoc)
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] a) {
        return storage.toArray(a);
    }

    /**
     * Convenience method that returns this instance unless it is empty. If this
     * instance is empty {@link #EMPTY} is returned.
     * 
     * @return this instance or {@link #EMPTY}
     */
    public SWGResourceList toReturn() {
        return this.isEmpty()
            ? EMPTY
            : this;
    }

    @Override
    public String toString() {
        ZString z = new ZString(getClass().getSimpleName());
        return z.app(storage.toString()).toString();
    }

    /**
     * Prohibits serialization.
     * 
     * @param ois
     *            the object stream
     * @throws NotSerializableException
     *             if anybody tries to serialize this object
     */
    private void writeObject(ObjectOutputStream ois)
        throws NotSerializableException {
        // It should be impossible to get here since this class does not
        // implement the Serializable interface, but ...
        throw new NotSerializableException("This container is not for storage");
    }
}
