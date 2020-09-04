package swg.swgcraft;

import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGResource;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;

/**
 * An abstract type servicing some sub-types related to resources.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
abstract class SWGSoapResResponse extends SWGSoapStatusResponse {

    /**
     * A galaxy constant denoting the galaxy this response is about.
     */
    protected SWGCGalaxy galaxy;

    /**
     * The planet which this response is about. If this response is not about a
     * planet the value is {@code null}.
     */
    protected SWGPlanet planet;

    /**
     * The resource which this response is about.
     */
    protected SWGKnownResource resource;
    protected SWGResource gresource;

    /**
     * Creates an instance of this type using the identified arguments. If this
     * response is not about a planet it is {@code null}.
     * 
     * @param res
     *            the new resource which this response is about
     * @param planet
     *            the planet which this response is about, or {@code null}
     * @throws NullPointerException
     *             if the resource is {@code null}
     */
    protected SWGSoapResResponse(SWGKnownResource res, SWGPlanet planet) {
        super(res.id());
        this.resource = res;
        this.planet = planet;
        setGalaxy(res.galaxy());
    }
    
    protected SWGSoapResResponse(SWGResource res, SWGPlanet planet) {
        super(res.id());
        this.gresource = res;
        this.planet = planet;
        setGalaxy(res.galaxy());
    }

    /**
     * Returns the resource which this response is about.
     * 
     * @return the resource which this response is about
     */
    public final SWGKnownResource getResource() {
        return resource;
    }

    /**
     * Helper method that sets the galaxy this response is about. If the
     * argument is {@code null} and SWGAide is run in -ae mode this method
     * asserts.
     * 
     * @param galaxy
     *            the galaxy constant denoting the galaxy this response is about
     */
    private void setGalaxy(SWGCGalaxy galaxy) {
        this.galaxy = galaxy;
        assert (galaxy != null) : "galaxy is null";
    }
}
