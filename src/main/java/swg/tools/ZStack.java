package swg.tools;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EmptyStackException;

/**
 * This type implements a classic stack without additional overhead. This type
 * accepts {@code null} elements. All modifying method are thread safe.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 * @param <E> the type contained by this type
 */
public class ZStack<E> implements Serializable {

    /**
     * Serialization support. If {@link #elements} is changed the write- and
     * read-object methods must be updated but serialization is not broken.
     */
    private static final long serialVersionUID = -5108906916704335023L;

    /**
     * The backing array for this stack. This type is interchangeable
     */
    protected transient Object[] elements;

    /**
     * The number of elements in this stack.
     * 
     * @serial int: the size of this stack
     */
    protected int size;

    /**
     * Creates an empty stack, initial capacity is 16 elements.
     */
    public ZStack() {
        this(16);
    }

    /**
     * Creates a stack with the specified initial capacity.
     * 
     * @param capacity
     *            initial capacity
     */
    public ZStack(int capacity) {
        elements = new Object[capacity];
        size = 0;
    }

    /**
     * Clears this stack so it is empty.
     */
    public synchronized void clear() {
        size = 0;
    }

    /**
     * Increases the capacity of this stack so it can hold at least the
     * specified number of elements.
     * 
     * @param minCapacity
     *            the desired minimum capacity
     */
    public synchronized void ensureCapacity(int minCapacity) {
        if (minCapacity > elements.length) {
            // a stack should not grow much once its peek is found
            int newCapacity = elements.length + 16;

            if (newCapacity < minCapacity)
                newCapacity = minCapacity;

            elements = Arrays.copyOf(elements, newCapacity);
        }
    }

    /**
     * Determines if this stack is empty.
     * 
     * @return {@code true} if this stack contains no items
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Peeks at the top-most element without removing it from this stack.
     * 
     * @return the element at the top of this stack
     * @exception EmptyStackException
     *                if this stack is empty
     */
    @SuppressWarnings("unchecked")
    public synchronized E peek() {
        if (size <= 0)
            throw new EmptyStackException();

        return (E) elements[size - 1];
    }

    /**
     * Pops and returns the element at the top of this stack. If the element is
     * {@code null} it is returned as is.
     * 
     * @return the element at the top of this stack
     * @exception EmptyStackException
     *                if this stack is empty
     */
    public synchronized E pop() {
        E obj = peek();
        --size;
        return obj;
    }

    /**
     * Pushes an element onto the top of this stack. If necessary this method
     * increases the capacity of this stack. If the element is {@code null} it
     * is pushed onto the stack as is.
     * 
     * @param elem
     *            an element
     * @return the argument as is
     */
    public synchronized E push(E elem) {
        ensureCapacity(size + 1);
        elements[size] = elem;
        ++size;
        return elem;
    }

    /**
     * Deserialize an instance from a stream.
     * 
     * @param s
     *            the stream
     * @throws IOException if there is an I/O error
     * @throws ClassNotFoundException if there is a missing class
     */
    private synchronized void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {

        // read in size and any hidden stuff
        s.defaultReadObject();

        // array length and allocate array
        int arrayLength = s.readInt();
        Object[] a = elements = new Object[arrayLength];

        // read all elements
        for (int i = 0; i < size; ++i)
            a[i] = s.readObject();
    }

    /**
     * Returns the 1-based distance from the top of this stack to the specified
     * object, or -1 if the object is not on this stack. The topmost item on the
     * stack is considered to be at distance {@code 1}. This implementation
     * returns the distance using {@code o.equals(element)} or if {@code o} is
     * {@code null} the {@code o == element} is used.
     * 
     * @param o
     *            an object
     * @return the 1-based distance from the top of this stack to where the
     *         object is located, or -1
     */
    public synchronized int search(Object o) {
        int i = size - 1;
        if (o == null) {
            for (; i >= 0; --i)
                if (elements[i] == null)
                    break;
        } else {
            for (; i >= 0; --i)
                if (o.equals(elements[i]))
                    break;
        }
        if (i >= 0)
            return size - i;

        return -1;
    }

    /**
     * Returns the number of elements in this stack.
     * 
     * @return the size
     */
    public int size() {
        return size;
    }

    /**
     * Shrinks the capacity to the current size of this stack. If size is less
     * than 16 elements this method does nothing to avoid future enlargement.
     */
    public synchronized void trimSize() {
        if (size > 16 && size < elements.length)
            elements = Arrays.copyOf(elements, size);
    }

    /**
     * Serialize the state of this stack to a stream.
     * 
     * @param s
     *            a stream
     * @throws IOException if there is an I/O error
     * @serialData the length of the backing array is emitted (int), followed by
     *             all its elements within size
     */
    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws IOException {
        // write size and any hidden stuff
        s.defaultWriteObject();

        // write array length
        s.writeInt(elements.length);

        // Write out all elements in the proper order.
        for (int i = 0; i < size; ++i)
            s.writeObject(elements[i]);
    }
}
