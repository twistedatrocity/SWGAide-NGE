package swg.crafting.resources;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import swg.SWGAide;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDoTask;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.tools.SimplePrefsKeeper;

/**
 * This super-type represents an interface to all resource handling in SWGAide
 * and to all communication with SWGCraft.org in relation to resources. In
 * SWGAide no other client but this type is allowed to manage
 * {@link SWGKnownResource} and to communicate resource data with SWGCraft.org.
 * For the rationale behind this strict rule, see {@link SWGResource}.
 * <p>
 * This type provides methods to obtain <i>known resource&nbsp;</i> objects, to
 * submit resources, etc. Most methods are static so normally there is no need
 * for a reference to an instance of this type. All methods are thread safe,
 * some methods are blocking until networking is complete, other methods are
 * non-blocking and if such a feature is provided it will notify the caller when
 * data is ready.
 * <p>
 * <b>Known Resources</b>
 * <p>
 * This type provides <i>known resource </i> objects which are guaranteed to be
 * unique in SWGAide. In particular, if the requested resource object is locally
 * cached it is returned. Otherwise SWGCraft.org is queried for the resource
 * data and a new resource object is created. If the resource is not known at
 * SWGCraft.org a local resource object is created. In the two latter cases, the
 * created object is always cached before it is returned.
 * <p>
 * <b>Note: </b>Clients must not persistently store instances of <i>known
 * resource </i> objects. Detailed information is read in {@link SWGResource}
 * and in {@link SimplePrefsKeeper}.
 * <p>
 * <b>Technicalities</b>
 * </p>
 * Much of the implementation providing the services of this "interface" is
 * actually located in the sub-type of this type. It is located in the package
 * {@code swg.swgcraft} where it has access to services which have default
 * access modifiers, making the sub-type the richer type.
 * {@link SWGFrame#getResourceManager()} returns the sub-type.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGResourceMgr {

    // It is logical to have SWGResourceManager located in the package which
    // provides communication with SWGCraft, services which all are non-public.
    // Thus, I hope this construct will narrow the gap between swg.swgcraft and
    // this package and their classes respectively.

    /**
     * The cache for resource instances. This cache contains all resources which
     * are instantiated in SWGAide, that is, current, depleted but not purged,
     * and old resources contained by for example inventory resource
     * collections.
     */
    private static SWGResourceCache cache;

    /**
     * The standard size for a collection for all resources on a galaxy, with
     * some latitude for depleted resources which are retained for some days.
     */
    public static final int COLLECTION_SIZE = 512;

    /**
     * The frame for this application.
     */
    protected static SWGFrame frame;

    /**
     * A map of resource sets mapped to by galaxy names. Each set contains
     * current resources and also depleted resources until they are purged, but
     * a set does not contain older resources which eventually are contained by
     * collections of inventory resources or the similar. Resources are added to
     * a set when there is a new download from SWGCraft.org and when the user
     * uploads resources.
     * <p>
     * The only purpose with this map is to contain spawning and recently
     * depleted resources so they survive serialization-deserialization. All
     * instances which are contained in this map are also added to the cache
     * when SWGAide is launched. Hence, the contained sets do only have setters
     * but no getters or removers (with one exception). All resource instances
     * are rather obtained from the cache.
     * <p>
     * This map is set by {@link #initiate()} which obtains the map from
     * SWGAide's object storage. Purging of old resources is implemented so that
     * when SWGAide exits all sets are marshaled and depleted resources are
     * retired and purged. If there is an empty set it is removed from the map.
     * The resulting map is already contained int SWGAide's objects storage
     * which is saved to disk.
     */
    private static Map<String, SWGResourceSet> generalMap;

    /**
     * A reference to the resource class tree.
     */
    @SuppressWarnings("unused")
    private SWGResourceClassTree resClassTree;

    /**
     * Creates an object with the singleton property retained. This class should
     * only be instantiated once.
     * 
     * @param frame
     *            the frame for the application, used by some methods for GUI
     *            dialogs and for access to the exit routine handler
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    protected SWGResourceMgr(SWGFrame frame) {
        if (frame == null)
            throw new NullPointerException("Argument is null");
        if (SWGResourceMgr.frame != null)
            throw new IllegalStateException("Cannot break singleton property");

        SWGResourceMgr.frame = frame;

        // XXX: use cache.getSize() and an INI file or something
        cache = new SWGResourceCache(this, 2000);
    }
    
    /**
     * Clears all contents from the resource cache. This method must only be
     * used if there is an error loading the SWGAide.DAT file, to make room for
     * loading a backup file, if one exists.
     */
    public final void clear() {
        cache.clear();
    }

    /**
     * This implementation executes some exit routines for this instance, such
     * as saving resource collections an the similar.
     */
    final void doExit() {
        generalExitMap();

    }

    /**
     * Initiates this instance after which SWGAide's object storage is loaded.
     * <p>
     * <b>Notice:</b> This method must only be called once by
     * {@link SWGFrame#SWGFrame()} and after that the SWGAide's object storage
     * is loaded.
     */
    public final void initiate() {
        // Only stuff which cannot be instantiated in constructor goes here

        // We know that the unchecked cast is OK since this is the only time it
        // is also put it into SWGAide's object storage, as a default value
        @SuppressWarnings("unchecked")
        Map<String, SWGResourceSet> gxycoll =
                (Map<String, SWGResourceSet>) SWGFrame.getPrefsKeeper().get(
                        "resourceGeneralMap",
                        new HashMap<String, SWGResourceSet>());
        generalMap = gxycoll;

        // initiate but never use but through static methods
        resClassTree = new SWGResourceClassTree();

        // do not add exit-callback until SWGFrame.initEnvironment is finished
        // and we do have a a generalMap to handle
        frame.addExitCallback(new SWGDoTask(new SWGDoTask.TaskCallback() {
            public void execute() {
                doExit();
            }
        }));
    }

    /**
     * Helper method which adds {@code resource} to the cache. If {@code false}
     * is returned from {@code SWGResourceCache.add(SWGKnownResource)} a message
     * is written to SWGAide's error log.
     * 
     * @param resource
     *            the resource to add to the cache
     */
    private static final void cacheAdd(SWGKnownResource resource) {
        // This method is only called after the resource is validated and it is
        // determined a logically equal resource is not contained in the cache
        if (!cache.add(resource))
            SWGAide.printError("SWGResourceMgr:cacheAdd",
                new IllegalStateException(
                "false was returned: " + resource.toString()));
    }

    /**
     * Helper method which returns a set of resources from {@code galaxy} which
     * are depleted, or {@link SWGResourceSet#EMPTY} if none is found.
     * 
     * @param galaxy
     *            a galaxy constant denoting where the resource has spawned
     * @return a set of resources which are depleted, or
     *         {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if the galaxy is {@code null}
     */
    protected static final SWGResourceSet cachedDepleted(SWGCGalaxy galaxy) {
        SWGResourceSet coll = cachedSet(galaxy);
        SWGResourceSet depl = new SWGResourceSet(coll.size());
        for (SWGKnownResource kr : coll)
            if (kr.isDepleted())
                depl.privateAdd(kr);

        return depl.get();
    }

    /**
     * Helper method which returns a resource with the specified ID, or {@code
     * null} if none is found.
     * <p>
     * This method is thread safe and locks on {@link #cache}.
     * 
     * @param swgcraftID
     *            a unique SWGCraft ID
     * @return the wanted resource, or {@code null}
     * @throws IllegalArgumentException
     *             if the ID is invalid
     */
    protected static final SWGKnownResource cachedInstance(long swgcraftID) {
        synchronized (cache) {
            return cache.getInstance(swgcraftID);
        }
    }

    /**
     * Helper method which returns a resource with {@code name} from {@code
     * galaxy}, or {@code null} if none is found.
     * <p>
     * This method is thread safe and locks on {@link #cache}.
     * 
     * @param name
     *            the proper name of the resource
     * @param galaxy
     *            a galaxy constant denoting where the resource has spawned
     * @return the wanted resource, or {@code null}
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    protected static final SWGKnownResource cachedInstance(
        String name, SWGCGalaxy galaxy) {
        
        synchronized (cache) {
            return cache.getInstance(name, galaxy);
        }
    }

    /**
     * Helper method which returns a set of resources which are spawning or have
     * spawned at {@code galaxy}, or {@link SWGResourceSet#EMPTY} if none is
     * found.
     * <p>
     * This method is thread safe and locks on {@link #cache}.
     * 
     * @param galaxy
     *            a galaxy constant denoting where the resource has spawned
     * @return a set of resources, or {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if the galaxy is {@code null}
     */
    protected static final SWGResourceSet cachedSet(SWGCGalaxy galaxy) {
        synchronized (cache) {
            return cache.getSet(galaxy);
        }
    }

    /**
     * Helper method which returns a set of resources which are currently
     * spawning at {@code galaxy}, or {@link SWGResourceSet#EMPTY} if none is
     * found.
     * <p>
     * This method is thread safe and locks on {@link #cache}.
     * 
     * @param galaxy
     *            a galaxy constant denoting where the resource has spawned
     * @return a set of spawning resources, or {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if the galaxy is {@code null}
     */
    protected static final SWGResourceSet cachedSpawning(SWGCGalaxy galaxy) {
        synchronized (cache) {
            return cache.getSpawning(galaxy);
        }
    }

    /**
     * Helper method which returns a set of resources which are of the specified
     * type and which are spawning or have spawned at {@code galaxy}. The
     * resources' classes are equal to or inherit from the specified resource
     * class. {@link SWGResourceSet#EMPTY} is returned if none was found.
     * <p>
     * This method is thread safe and locks on {@link #cache}.
     * 
     * @param type
     *            the resource class to filter for
     * @param galaxy
     *            a galaxy constant denoting where the resource has spawned
     * @return a set of resources, or {@link SWGResourceSet#EMPTY}
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    protected static final SWGResourceSet cachedType(
        SWGResourceClass type, SWGCGalaxy galaxy) {
        synchronized (cache) {
            return cache.getType(type, galaxy);
        }
    }

    /**
     * Helper method which adds {@code resource} to the resource set for {@code
     * galaxy}. If the argument's galaxy is unknown this method does nothing.
     * 
     * @param resource
     *            the resource to add
     * @throws NullPointerException
     *             if the resource is {@code null}
     */
    private static final void generalAdd(SWGKnownResource resource) {
        // This method is only called after the resource is validated and
        // possibly updated if it is already contained in the cache

        SWGResourceSet rs = generalGet(resource.galaxy());
        rs.add(resource);
    }

    /**
     * Helper method which is executed when SWGAide is about to exit. For each
     * set this implementation calls {@link #generalExitSet(SWGResourceSet)} and
     * then, if the set is empty, removes its mapping from the map.
     * <p>
     * For information: This operation could be performed regularly during the
     * session, but before SWGAide's objects storage is saved to disk. However,
     * there is no gain in implementing a more complex logic than what this is.
     */
    private static final void generalExitMap() {
        synchronized (cache) {
            Iterator<SWGResourceSet> iter = generalMap.values().iterator();

            while (iter.hasNext()) {
                SWGResourceSet rs = iter.next();
                generalExitSet(rs);
                if (rs.isEmpty())
                    iter.remove();
            }
        }
    }
    

    /**
     * Helper method which is called when SWGAide is about to exit. This
     * implementation marshals the resource set and all depleted resources are
     * retired and purged. That is those resources which have been depleted more
     * than {@link SWGResource#RETIREMENT} days.
     * 
     * @param resources
     *            the resource set to marshal
     */
    private static final void generalExitSet(SWGResourceSet resources) {
        Iterator<SWGKnownResource> iter = resources.iterator();
        while (iter.hasNext()) {
            SWGKnownResource kr = iter.next();
            if (kr.retire()) {
                iter.remove();
            }
        }
    }

    /**
     * Helper method which returns a set of resources for {@code galaxy} from
     * {@link #generalMap}.
     * 
     * @param galaxy
     *            a galaxy constant denoting where the resource has spawned
     * @return a set of resources
     */
    private static final SWGResourceSet generalGet(SWGCGalaxy galaxy) {
        SWGResourceSet rs = generalMap.get(galaxy.getName());
        if (rs == null) {
            rs = new SWGResourceSet(COLLECTION_SIZE);
            generalMap.put(galaxy.getName(), rs);
        }
        return rs;
    }

    /**
     * Helper method which returns an instance based on the argument. This
     * method must only be called from the resource manager.
     * <p>
     * This implementation examines the cache for the most equal instance. If
     * there is an instance it is returned. Otherwise a new instance is created,
     * which is added to the cache, if its galaxy ID is known it is added to the
     * general map for the galaxy, and finally it is returned.
     * <p>
     * <b>Note: </b>Assume the argument is resource "Foo" at galaxy "Bar" of
     * resource class ""Baz" and there exists a resource "Foo" of class "Baz"
     * but at galaxy "Foobar" in the cache tests for ID and name+galaxy properly
     * fail, but the test for name+class will return the resource for "Foobar"
     * since name+class does match. Indeed there are some name+class clashes
     * between galaxies listed at SWGCraft.org, and there is a small risk this
     * can happen. Should this case occur this method appends a message to
     * SWGAide's log file and returns the instance.
     * <p>
     * <b>Note: </b>This method does not update the returned instance. That is,
     * if the instance exists in the cache and if the specified argument has
     * values which are more complete than the local instance, they are not
     * updated. Such an operation must be handled by updater methods.
     * <p>
     * This method is thread-safe and synchronizes on {@link #cache}.
     * 
     * @param res
     *            the resource to find or to create the new instance from
     * @return a known resource
     * @throws IllegalArgumentException
     *             if the argument is invalid or if the galaxy is unknown
     * @throws NullPointerException
     *             if the argument is {@code null}
     */
    protected static SWGKnownResource getKnown(SWGMutableResource res) {
        SWGKnownResource knownInstance;
        synchronized (cache) {
            knownInstance = cache.getInstance(res);
            if (knownInstance == null) {
                // create a new instance, cache it and add to the general map
                knownInstance = getKnownTrusted(res);
            }
        }
        return knownInstance;
    }

    /**
     * Secondary helper method which creates and returns an instance based on
     * the argument, the return value is never {@code null}. This method caches
     * the instance and adds it to the general map. This method <U>must only</U>
     * be called from the resource manager and then <b><U>only if</U></b> the
     * instance obtained from {@link #getKnown(SWGMutableResource)} returned an
     * instance which did not match the argument to 100%, see comment on galaxy
     * vs. resource class in the main getter method.
     * <p>
     * This method is thread-safe and synchronizes on {@link #cache}.
     * 
     * @param res the resource to find or to create the new instance from
     * @return a known resource
     * @throws IllegalArgumentException if the argument is invalid or if the
     *         galaxy is unknown
     * @throws NullPointerException if the argument is {@code null}
     */
    protected static SWGKnownResource getKnownTrusted(SWGMutableResource res) {
        synchronized (cache) {
            if (res.rc().isSpaceOrRecycled())
                return res.rc().spaceOrRecycled();

            SWGKnownResource knownInstance = new SWGKnownResource(res);
            cacheAdd(knownInstance);
            generalAdd(knownInstance);
            return knownInstance;
        }
    }

    /**
     * Helper method which removes the specified resource from the general map
     * of resources but not from the cache. Next time SWGAide is launched it is
     * implicitly removed from the cache unless this instance has leaked to some
     * collection, in which case it must be retained in the cache already now.
     * <p>
     * <b>Note: </b>This method must only be called from the resource manager
     * combo, and it must only be called from such a small scope that the
     * specified instance has never been made visible for any clients. A sample
     * use case is to send a new-resource message to SWGCraft.org but the return
     * response indicates that this instance is invalid. Unless that is a bug at
     * SWGCraft.org it is a bug at our side we do not want to pollute our cache
     * with flawed data, thus we promptly withdraw the instance before it is
     * used anywhere.
     * 
     * @param kr
     *            the instance to withdraw
     */
    protected static void remove(SWGKnownResource kr) {
        synchronized (cache) {
            // synchronize on cache as it is the general lock
            generalGet(kr.galaxy()).remove(kr);
        }
    }

    /**
     * Simple helper method which just supplements the specified argument with
     * the unique SWGCraft ID. No other actions is performed by this method.
     * This method must only be called from the resource manager and its
     * sub-type.
     * 
     * @param res
     *            the resource to update
     * @param swgcraftID
     *            the ID to supply
     * @throws IllegalArgumentException
     *             IllegalArgumentException if the argument is invalid
     * @throws NullPointerException
     *             if the resource is {@code null}
     */
    protected static void supplyID(SWGKnownResource res, long swgcraftID) {
        res.id(swgcraftID);
    }

    /**
     * Helper method which sets the depleted time for the specified resource.
     * The time is measured in seconds since January 1, 1970, or Long.MAX_VALUE
     * for an aged resource with unknown depletion date. If the argument is
     * negative the specified resource is marked available.
     * 
     * @param res
     *            the depleted resource
     * @param date
     *            the time the resource was determined to be depleted
     * @throws NullPointerException
     *             if the resource is {@code null}
     */
    protected static final void updateDepleted(SWGKnownResource res, long date) {
        res.depleted(date);
    }

    /**
     * Helper method which compares the local set of known resources against the
     * specified list of recently downloaded resources. For each instance in the
     * local set, if is not contained in the specified list it is marked as
     * depleted, but every other instance is verified so its state is
     * not-depleted.
     * 
     * @param newDownload
     *            a recently downloaded list of resources
     */
    private static final void updateDepleted(SWGResourceList newDownload) {

        SWGResourceSet generalSet = generalGet(newDownload.get(0).galaxy());

        // current time minus 45 minutes:
        // if an instance is not present in the download and if SWGAide is
        // restarted after resource submission we have a local copy for up to
        // half an hour not yet in download, plus safety slack
        long fortyFiveMinutes =
            (System.currentTimeMillis() / 1000L) - (45 * 60);

        for (SWGKnownResource kr : generalSet) {
            SWGResource nr;
            if (kr.id() > 0)
                nr = newDownload.getByID(kr.id());
            else
                nr = newDownload.getByNameAndType(kr.getName(), kr.rc());

            if (kr.isDepleted()) {
                if (nr != null) {
                    // revert its state, it is contained in the new download
                    // maybe resurrected at SWGCraft.org
                    kr.depleted(false);
                }
            } else if (nr == null) {
                SWGPlanetAvailabilityInfo pai = kr.availableFirst();
                long fa = pai.availableDate;

                if (fa < fortyFiveMinutes)
                    updateDepleted(kr, fortyFiveMinutes); // max 45 mins off ;)
            }
        }
    }

    /**
     * Helper method which is called from the sub-type with a list of resources
     * for a specific galaxy. The content of the list is examined versus the
     * cache and the general map of known resources. This implementation calls
     * helper methods for the following actions...
     * <ol>
     * <li>possibly updates depleted instances</li>
     * <li>possibly updates data of known instances</li>
     * <li>creates new instances which are added to the cache and to the general
     * map of current resources</li>
     * </ol>
     * <p>
     * This method is thread safe and locks on {@link #cache}.
     * 
     * @param newDownload
     *            a list of recently downloaded resources
     * @throws NullPointerException
     *             if the argument is {@code null}, or if an element has
     *             important data which is {@code null}
     */
    protected static final void updateFromDownload(SWGResourceList newDownload) {
        if (newDownload.isEmpty())
            return; // safety, shouldn't happen

        synchronized (cache) {            
            updateDepleted(newDownload);
            updateSpawning(newDownload);
        }
    }

    /**
     * Helper method which examines all fields of the known instance relative
     * the other resource. If there is a difference the known instance is
     * updated and non-negligible changes are individually logged to SWGAide's
     * log file. Basic validation is always performed. See {@link SWGResource}
     * for further information. This method is called in the following cases:
     * <ol>
     * <li>if {@code other} is obtained from SWGCraft.org via a SOAP action</li>
     * <li>if {@code other} is obtained from SWGCraft.org via a XML file</li>
     * <li>if {@code other} is just sent to SWGCraft.org as a new resource</li>
     * <li>if {@code known} is updated as part of sending an edit-message to
     * SWGCraft.org, in this case the flag {@code isTrusted} must be {@code
     * false} to avoid overwriting correct data in the case {@code other} is
     * incomplete</li>
     * <li>if {@code known} is unknown at SWGCraft.org but the user edits data,
     * for example at the resource inventory</li>
     * </ol>
     * 
     * @param known a known resource
     * @param other another resource, assumed to be "better"
     * @param isTrusted {@code true} if {@code other} is trusted, {@code false}
     *        otherwise (see item 4 above)
     * @throws IllegalArgumentException if {@code other} has invalid data
     * @throws NullPointerException if an argument is {@code null}
     */
    protected static void updateInstance(
            SWGKnownResource known, SWGMutableResource other, boolean isTrusted) {

        // galaxy
        if (other.galaxy() != null && known.id() <= 0) {
            known.galaxy(other.galaxy());
        }

        // name
        if (!known.getName().equals(other.getName())) {
            updateLog(known, "name", known.getName(), other.getName());
            known.name(other.getName());
        }

        // availability map
        for (SWGPlanet pl : SWGPlanet.values()) {
            SWGPlanetAvailabilityInfo pai = other.availabilityFor(pl);
            if (pai != null)// trust other, will be updated next download
                known.availability(pai);
            else if (isTrusted)
                known.availabilityRemove(pl);
        }

        // stats
        if (other.stats().hasValues()
                && !known.stats().equals(other.stats())) {
            known.stats(other.safeStats()); // negligible update, no log
        }

        // swgcraftID
        if (known.id() <= 0 && other.id() > 0) {
            known.id(other.id());
        }

        // type
        if (known.rc() != other.rc()) {
            updateLog(known, "type", known.rc().rcName(), 
                    other.rc().rcName());
            known.rc(other.rc());
        }

        // final validation of caps versus type, both stats and type may have
        // changed at this point, trigger no exception but log the error
        if (known.stats().hasValues()) {
            String error = SWGResourceClass.validate(
                    known.stats(), known.rc());
            if (error != null)
                SWGAide.printDebug("rmgr", 1, String.format(
                        "Updating: resource has stats but errors%n%s%n%s",
                        known.toString(), error));
        }

        // finally, if the former instance is --replaced-- at SWGCraft
        if (isTrusted
                && known.getName().equals(other.getName())
                && known.galaxy().equals(other.galaxy())
                && other.id() > 0
                && known.id() < other.id()) {
            // is-trusted: name and galaxy equals but known is older than other
            updateLog(known, "ID", Long.toString(known.id()),
                    Long.toString(other.id()));
            known.deserialID(other.id());
        }
    }
    

    /**
     * Helper method which writes a message to SWGAide's general log file about
     * the updated resource.
     * 
     * @param known
     *            the updated local instance
     * @param field
     *            the field which is being updated
     * @param from
     *            the from value
     * @param to
     *            the to value
     */
    private static final void updateLog(SWGKnownResource known, String field,
        String from, String to) {
        String m = String.format(
            "Updated %s: %s from %s to %s", known.toString(), field, from, to);
        SWGAide.printDebug("rmgr", 1, m);
    }

    /**
     * Helper method which appends the specified planet availability record to
     * the specified resource.
     * 
     * @param res
     *            the resource to amend
     * @param pai
     *            the planet availability record to append to the resource
     * @throws NullPointerException
     *             if an argument is {@code null}
     */
    protected static final void updatePlanet(
        SWGKnownResource res, SWGPlanetAvailabilityInfo pai) {
        res.availability(pai); // trust the caller, will be fixed next download
    }

    /**
     * Helper method which compares the list of recently downloaded resources
     * with the local cache of known resources. For each element in the
     * downloaded list, if it is not contained in the local cache a new
     * {@link SWGKnownResource} is created and added to the local cache;
     * otherwise it is compared with its corresponding local instance and if
     * there are updated data fields the local instance is updated.
     * 
     * @param download a list of recently downloaded resources
     */
    private static final void updateSpawning(SWGResourceList download) {

        for (SWGResource r : download) {
            SWGMutableResource mr = (SWGMutableResource) r;

            // case 1: a new resource in the download
            // case 2: not new, examine the local and update if necessary

            // scan cache in the case it is missing in general set
            SWGKnownResource kr = cache.getInstance(mr);
            if (kr == null) 
                getKnownTrusted(mr);// case 1
            else
                updateInstance(kr, mr, true);                // case 2
        }
    }
}
