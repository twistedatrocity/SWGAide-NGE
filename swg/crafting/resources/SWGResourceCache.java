package swg.crafting.resources;

import java.util.ArrayList;
import java.util.List;

import swg.gui.SWGFrame;
import swg.model.SWGCGalaxy;
import swg.swgcraft.SWGResourceManager;
import swg.tools.SimplePrefsKeeper;

/**
 * Together with {@link SWGResourceManager} this type maintains a local cache of
 * {@link SWGKnownResource} instances in SWGAide. It must exist just one
 * instance of this type so that it can maintain the singleton property of
 * resources. For cached resources this property must be true both for instance
 * equality using the == operator, and for logical equality using any of the
 * three identifiers for {@link SWGResource}. Details and rules regarding
 * resources and storage are read in {@link SWGResource} and
 * {@link SimplePrefsKeeper}.
 * <p>
 * Once instantiated and populated this type provides a cache of instances of
 * {@link SWGKnownResource} which are...
 * <ol>
 * <li>a currently spawning resource</li>
 * <li>a recently depleted resource which is not yet purged</li>
 * <li>a resource which is found in one or more object graphs in SWGAide</li>
 * </ol>
 * The first case does not need explanation. For the second case can be noted
 * that depleted resources are contained for a few days, managed by the resource
 * manager. Thus, instances of recently depleted resources can be obtained
 * without querying SWGCraft.org.
 * <p>
 * The third case is principal for maintaining instance control so that in
 * SWGAide it exists only one unique instance per resource in SWG. No matter
 * what kind of object, container, or wrapper, and no matter their purpose, as
 * long as a resource instance is found in any object graph it must be contained
 * in this cache. Thus, any other client that requests a resource, if it is
 * found in this cache that instance is handed to the client, otherwise the
 * resource manager creates a new instance which is added to the cache and
 * handed to the client.
 * <p>
 * This implies that <b>the resource manager must always first query this cache
 * before creating a new instance</b> using {@link #getInstance(SWGResource)}.
 * If there is a previous instance it is returned, otherwise one can be created
 * and added to the cache. By using the one instance there will never exist
 * objects, containers, nor wrappers which has different instances in their
 * object graphs for one and the same resource.
 * <p>
 * Clients must always request resource instances from the resource manager.
 * This is the reason why this type has member methods that grant access just
 * for the resource manager. Furthermore, no remove method exists in this type
 * as it is infeasible to track down all possible objects which have the
 * suggested instance in their object graphs. Resources that are not referenced
 * are either purged or they vanish at next exit/restart of SWGAide.
 * <p>
 * This type is very tightly coupled with the types {@link SWGResourceManager}
 * and {@link SWGKnownResource}. The cache is populated in two ways:
 * <ol>
 * <li>When SWGAide starts and is deserializing resource instances from the DAT
 * file, {@link SWGKnownResource#readResolve} will add each deserialized object
 * to this cache using {@link #addDeserialized(SWGKnownResource)}.</li>
 * <li>When the resource manager creates new instances, either on demand by user
 * action such as adding a resource to the inventory, or the result of
 * downloading resources from SWGCraft.org.</li>
 * </ol>
 * <p>
 * <b>Note: </b>This type must be instantiated before deserialization of
 * resource storage can begin. This is done so that {@link SWGFrame#SWGFrame()}
 * creates an instance of {@link SWGResourceManager} which creates an instance
 * of this cache. After deserialization of resources
 * {@link SWGResourceManager#initiate()} is called.
 * <p>
 * This type is not thread safe. However, as only the resource manager can
 * access this instance and its member functions this should cause no problem.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
final class SWGResourceCache {

    /**
     * The list that is the container in this implementation.
     */
    private static List<SWGKnownResource> cache;

    /**
     * The resource manager.
     */
    private static SWGResourceMgr manager;

    /**
     * Creates an instance of this type wit the specified initial size.
     * 
     * @param manager this type's public interface
     * @param initSize initial size of the cache
     * @throws IllegalStateException if there already is another instance
     * @throws NullPointerException if the argument is {@code null}
     */
    SWGResourceCache(SWGResourceMgr manager, int initSize) {
        if (SWGResourceCache.manager != null)
            throw new IllegalStateException("Must be singleton");
        if (manager == null)
            throw new NullPointerException("Argument is null");

        SWGResourceCache.manager = manager;

        cache = new ArrayList<SWGKnownResource>(initSize);
    }

    /**
     * Adds the specified resource object to this cache. If the argument is
     * {@code null} it is ignored.
     * <p>
     * This method <b>does not</b> guard against doubles, the resource manager
     * must call {@link #getInstance(SWGResource)} before calling this method.
     * 
     * @param resource the resource to add to the cache
     * @return {@code true} if the resource was added to this cache, {@code
     *         false} otherwise or if the argument is {@code null}
     */
    boolean add(SWGKnownResource resource) {
        return (resource == null)
                ? false
                : cache.add(resource);
    }
    
    /**
     * Clears all contents from the resource cache. This method must only be
     * used if there is an error loading the SWGAide.DAT file, to make room for
     * loading a backup file, if one exists.
     */
    void clear() {
        cache.clear();
    }

    /**
     * See comment for {@link #getInstance(SWGResource)}.
     * 
     * @param swgCraftID a unique SWGCraft ID
     * @return the wanted resource, or {@code null}
     * @throws IllegalArgumentException if {@literal ID <= 0}
     */
    SWGKnownResource getInstance(long swgCraftID) {
        if (swgCraftID <= 0)
            throw new IllegalArgumentException("Invalid ID: " + swgCraftID);

        for (SWGKnownResource kr : cache) {
            if (kr.id() == swgCraftID)
                return kr;
        }
        return null;
    }

    /**
     * See comment for {@link #getInstance(SWGResource)}.
     * 
     * @param name the proper name of the resource
     * @param galaxy the galaxy constant for where the resource has spawned
     * @return the wanted resource, or {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    SWGKnownResource getInstance(String name, SWGCGalaxy galaxy) {
        if (name == null || galaxy == null)
            throw new NullPointerException("An argument is null");

        for (SWGKnownResource kr : cache) {
            if (kr.galaxy().equals(galaxy) && kr.getName().equals(name))
                return kr;
        }
        return null;
    }

    /**
     * Returns an element that equals the argument, or {@code null};
     * <i>carefully read all of the following notes though!</i>
     * <p>
     * This method scans the cached in this manner and in this order: 1) by
     * reference (the == operator), 2) by SWGCraft ID, and 3) by name/galaxy.
     * The first element that matches is returned, otherwise {@code null}. See
     * {@link SWGResource} regarding the identifiers.
     * <p>
     * The caller, the resource manager must always use the returned instance
     * rather than creating a new instance. That is to preserve the consistency
     * of object graphs in SWGAide which may reference the previous instance.
     * However, the resource manager must always determine if the returned
     * instance is valid and if it can be updated.
     * <p>
     * <b>Note 1:</b> occasionally a cached resource may be superseded at
     * SWGCraft, this happens if a resource is <i>removed </i> and a new
     * resource is added in its place with the same name. In this case this
     * method returns the cached instance because of name/galaxy equality. The
     * resource manager must pander for that case and update the returned
     * instance.
     * <p>
     * <b>Note 2:</b> this method does not compare by name/resource-class of the
     * following reason: assume that the the argument is resource <br/>
     * "Foo" of class "Baz" at galaxy "Bar" and it exists a cached resource<br/>
     * "Foo" of class "Baz" but at galaxy "Foobar"; the cache tests 1 to 3 will
     * fail properly, but if a test for name/resource-class existed it would
     * return the resource for "Foobar".
     * 
     * @param res the resource to match against
     * @return an instance that logically equals the argument, or {@code null}
     * @throws NullPointerException if the argument is {@code null}
     */
    SWGKnownResource getInstance(SWGResource res) {
        if (res == null)
            throw new NullPointerException("Resource is null");

        return contains(res);
    }

    /**
     * Returns a set of resources which are spawning or have spawned at the
     * specified galaxy. If no instance is found {@link SWGResourceSet#EMPTY} is
     * returned.
     * 
     * @param galaxy a galaxy constant
     * @return a set of resources, or {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException if the argument is {@code null}
     */
    SWGResourceSet getSet(SWGCGalaxy galaxy) {
        if (galaxy == null)
            throw new NullPointerException("Argument is null");

        SWGResourceSet ret = new SWGResourceSet(cache.size());
        for (SWGKnownResource kr : cache) {
            if (kr.galaxy().equals(galaxy))
                ret.privateAdd(kr); // we are sure kr is valid
        }
        return ret.get();
    }

    /**
     * Returns the number of elements in this cache.
     * 
     * @return the number of elements in this cache
     */
    int getSize() {
        return cache.size();
    }

    /**
     * Returns a set of resources which are in spawn at the specified galaxy. If
     * no instances are found {@link SWGResourceSet#EMPTY} is returned.
     * 
     * @param galaxy
     *            a galaxy constant denoting where the resources are in spawn
     * @return a set of spawning resources, or {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    SWGResourceSet getSpawning(SWGCGalaxy galaxy) {
        if (galaxy == null)
            throw new NullPointerException("Argument is null");

        SWGResourceSet ret = new SWGResourceSet(cache.size());
        for (SWGKnownResource kr : cache) {
            if (kr.galaxy().equals(galaxy) && !kr.isDepleted())
                ret.privateAdd(kr); // we are sure kr is valid
        }
        return ret.get();
    }

    /**
     * Returns a set of resources of the specified type and which have spawned
     * at the specified galaxy. This implementation filters for resources which
     * resource class equals or inherits from the specified resource class. If
     * no instances are found {@link SWGResourceSet#EMPTY} is returned.
     * 
     * @param type
     *            the resource class to filter for
     * @param galaxy
     *            a galaxy constant denoting where the resources have spawned
     * @return a set of resources, or {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    SWGResourceSet getType(SWGResourceClass type, SWGCGalaxy galaxy) {
        if (type == null || galaxy == null)
            throw new NullPointerException("An argument is null");

        SWGResourceSet ret = new SWGResourceSet(cache.size());
        Class<? extends SWGResourceClass> c = type.getClass();
        for (SWGKnownResource kr : cache) {
            if (kr.galaxy().equals(galaxy) && kr.rc().isSub(c))
                ret.privateAdd(kr); // we are sure kr is valid
        }
        return ret.get();
    }

    /**
     * Helper method which is tightly coupled with
     * {@link SWGKnownResource#readResolve}, which is the only method that is
     * allowed to call this method. This method tries to add the argument to
     * this cache, an operation that returns any previous element that is
     * logically equal to the argument. If no previous element is found {@code
     * null} is returned.
     * 
     * @param deserialized
     *            the newly deserialized instance to add to the cache
     * @return {@code null} if no previous instance is contained in this cache,
     *         otherwise the previous instance
     */
    static SWGKnownResource addDeserialized(SWGKnownResource deserialized) {
        if (deserialized == null)
            // paranoia
            throw new NullPointerException("Deserialized object is null");

        SWGKnownResource prev = contains(deserialized);
        if (prev == null) {
            cache.add(deserialized);
            return null;
        }
        return prev;
    }

    /**
     * Helper method for {@link #getInstance(SWGResource)} and for
     * {@link #addDeserialized(SWGKnownResource)}. Scans this cache for an
     * element that is logically equal to the specified resource and if there is
     * such an element it is returned.
     * <p>
     * This method scans using these identifiers for comparison, in this order:
     * 1) by reference (the == operator), 2) by SWGCraft ID, 3) by name/galaxy,
     * and 4) by name/resource-class. The first element that equals any of these
     * identifiers is returned. If no such element is found {@code null} is
     * returned. See {@link SWGResource} regarding the identifiers.
     * 
     * @param res
     *            the resource to find
     * @return the first element logically equal to the argument, or {@code
     *         null}
     */
    private static SWGKnownResource contains(SWGResource res) {
        // Notes on performance: lists, and ArrayList in particular, have
        // extremely good marshaling performance plus the fact that there is
        // usually less than 500 concurrently spawning resources at a galaxy.
        // Even if a user tracks several galaxies this method will work well.

        long id = res.id();
        SWGCGalaxy gx = res.galaxy();
        String name = res.getName();

        for (SWGKnownResource kr : cache) {
            if (kr == res)
                return kr;
            if (id > 0 && id == kr.id())
                return kr;
            if (gx.equals(kr.galaxy()) && name.equals(kr.getName()))
                return kr;
        }
        return null;
    }
}
