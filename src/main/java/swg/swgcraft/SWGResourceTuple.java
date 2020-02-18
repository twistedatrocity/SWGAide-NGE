package swg.swgcraft;

import swg.crafting.resources.SWGResourceClass;

/**
 * This type represents an identified resource as it is obtained from
 * SWGCraft.org for an identified galaxy. Instances of this type contains a
 * proper resource name and its corresponding resource-class constant.
 * <P>
 * <B>NOTE: </B>This implementation is thought to be used <I>only&nbsp;</I> in
 * the context where {@link SWGSoapListResResponse} is used. Clients are assumed
 * to use this type in a narrow scope for a very short time. Thus this type does
 * not include an identifier for the galaxy this instance was obtained for as it
 * is known in the scope.
 * <P>
 * This implementation provides public access to its two, immutable fields,
 * hence no getters are necessary.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
public final class SWGResourceTuple implements Comparable<SWGResourceTuple> {

    /**
     * The name of the resource.
     */
    public final String name;

    /**
     * The resource class, the resource's type.
     */
    public final SWGResourceClass type;

    /**
     * Creates an instance of this type for the two arguments. An instance of
     * this type represents an identified resource that has spawned at a galaxy
     * in SWG.
     * 
     * @param name
     *            the name of the resource
     * @param type
     *            the resource class
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    SWGResourceTuple(String name, SWGResourceClass type) {
        if (name == null || type == null)
            throw new NullPointerException("An argument is null");
        this.name = name;
        this.type = type;
    }

    public int compareTo(SWGResourceTuple o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name + type.rcName();
    }
}
