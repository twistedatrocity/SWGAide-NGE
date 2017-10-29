package swg.gui.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A light weight type for one chief object and an unmodifiable list of one or
 * more arbitrary objects. The purpose of this type is to convey a chief object
 * together with any number of objects of interest for a producer and consumer.
 * The producer and consumer must agree on a contract for the list of objects.
 * <p>
 * The methods {@link #equals(Object)} and {@link #hashCode()} invokes the same
 * methods of {@link #obj} &mdash; this effectively means that it is legal to
 * invoke {@link Collection#contains(Object)} with just the chief-object without
 * first having to create an instance of this type to test equality. No similar
 * logic exists for {@link #objects}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGSac {

    /**
     * The chief object.
     */
    public final Object obj;

    /**
     * An unmodifiable list of arbitrary objects. This list is not modifiable,
     * however, its elements are not protected.
     * 
     * @see Collections#unmodifiableList(List)
     */
    private final List<?> objects;

    /**
     * Creates an instance of this type. Notice that {@code null} is valid as
     * the second parameter but {@code javac} warns against it and suggests a
     * cast such as {@code (Object)null}.
     * 
     * @param o the chief object
     * @param objs one or several objects, or {@code null}
     * @throws NullPointerException if the chief object is {@code null}
     */
    public SWGSac(Object o, Object... objs) {
        if (o == null) throw new NullPointerException("Object is null");
        this.obj = o;
        this.objects = objs != null && objs.length > 0
                ? Collections.unmodifiableList(Arrays.asList(objs))
                : null;
    }

    /**
     * Returns an object for the specified index.
     * 
     * @param i an index
     * @return an object as defined by {@link List#get(int)}
     */
    public Object object(int i) {
        return objects != null
                ? objects.get(i)
                : null;
    }

    /**
     * Returns the unmodifiable list of objects; the list may be empty.
     * 
     * @return a list of objects
     * @see Collections#unmodifiableList(List)
     */
    public List<?> objects() {
        return objects;
    }

    /**
     * Returns the number of objects in the list of objects. The chief object is
     * excluded from the return value.
     * 
     * @return size as defined by {@link List#size()}
     */
    public int size() {
        return objects != null
                ? objects.size()
                : 0;
    }

    @Override
    public String toString() {
        return String.format("SWGCan[%s: %s]", obj, objects);
    }
}
