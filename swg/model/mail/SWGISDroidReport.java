package swg.model.mail;

import java.util.ArrayList;
import java.util.List;

import javax.naming.InvalidNameException;

import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGMutableResource;
import swg.crafting.resources.SWGPlanetAvailabilityInfo;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceSet;
import swg.model.SWGCGalaxy;
import swg.model.SWGPlanet;
import swg.model.mail.SWGMailMessage.Type;
import swg.swgcraft.SWGCraft;
import swg.tools.ZString;

/**
 * This type represents the information that is parsed from an Interplanetary
 * Survey Droid (ISDroid) report. An ISDroid report pertains to a specific
 * galaxy and planet pair, and to a specific generic resource class such as
 * Chemical, Flora, Gas, Geothermal, Mineral, or Water. When a report is sent it
 * is populated with a lists of all resources that are available; they read name
 * and class, but no values for their stats. This type provides basic accessors
 * but more important the methods to obtain particular collections related to
 * the report.
 * <p>
 * When this instance is created and populates {@link #wrappers} it compares the
 * content of the ISDroid report with what is currently reported at SWGCraft.org
 * and deduces which resources that are new and which that are supplemented or
 * corrected (name, planets, stats, or resource class). This method is a data
 * holder and it does not communicate with SWGCraft nor any other action.
 * <p>
 * <b>Notice:</b> a report of this type does not contain instances of depleted
 * resources.
 * <p>
 * <b>Warning:</b> because an ISDroid report is ephemeral &mdash; concurrently
 * new resources are spawning and others are depleted &mdash; only a fresh
 * report should be processed and used by SWGAide. Hence, if a report is older
 * than {@link #MAX_AGE_HOURS} the client should prompt the user for her consent
 * to proceed, at the user's peril.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class SWGISDroidReport implements Comparable<SWGISDroidReport> {

    /**
     * The suggested maximum age for an ISDroid report before it is too old to
     * upload to SWGCraft without human intervention.
     */
    public static final long MAX_AGE_HOURS = 4;

    /**
     * The galaxy which this report pertains to.
     */
    private final SWGCGalaxy galaxy;

    /**
     * The mail that this data pertains to.
     */
    private final SWGMailMessage mail;

    /**
     * The planet which this report pertains to.
     */
    private final SWGPlanet planet;

    /**
     * The generic resource class this report pertains to.
     */
    private final SWGResourceClass type;

    /**
     * A list of resources which are reported in this report.
     */
    private final List<Wrapper> wrappers;

    /**
     * Creates an empty report based on the specified mail. See
     * {@link #newInstance(SWGMailMessage, SWGResourceSet)}.
     * 
     * @param m a mail
     * @param p a planet constant
     * @param g a galaxy constant
     * @param t a resource class constant
     */
    private SWGISDroidReport(
            SWGMailMessage m, SWGPlanet p, SWGCGalaxy g, SWGResourceClass t) {

        this.mail = m;
        this.planet = p;
        this.galaxy = g;
        this.type = t;
        wrappers = new ArrayList<Wrapper>();
    }

    /**
     * Returns an array of all resources that are listed in this report. All of
     * these resources were available when the report was generated in-game.
     * 
     * @return a list of available resources deduced from this report
     */
    public Wrapper[] available() {
        return wrappers.toArray(new Wrapper[wrappers.size()]);
    }

    public int compareTo(SWGISDroidReport o) {
        int c = type.compareTo(o.type);
        return c == 0
                ? planet.compareTo(o.planet)
                : c;
    }

    /**
     * Extracts and returns a set of depleted resources. More formally, when
     * this report was generated these resources were no longer available. This
     * method deduces the result from this report and the specified set of known
     * resources; the set contains galaxy-specific resources which are
     * considered as in-spawn both locally and at SWGCraft.
     * 
     * @param inSpawn a set of known, non-depleted resources
     * @return a set of depleted resources
     * @throws IllegalArgumentException if the set is for the wrong galaxy
     * @throws NullPointerException if the argument is {@code null}
     */
    @SuppressWarnings("synthetic-access")
    public List<Wrapper> depleted(SWGResourceSet inSpawn) {
		if (!inSpawn.isEmpty() && inSpawn.get(0).galaxy().equals(gxy()) == false)
                throw new IllegalArgumentException("Wrong galaxy");

        // reduce the collection -> planet -> resource class
        SWGResourceSet depl = inSpawn.subsetBy(planet());
        depl = depl.subsetBy(type());

        // remove resources that are listed by this report, they are in spawn
        // res must be "known" to be possible to deplete, known are not null
        for (Wrapper wr : wrappers)
            if (wr.known != null) depl.remove(wr.known);

        // remove really fresh instances, those that have an availability date
        // at SWGCraft or locally that is "younger" than the mail's date
        depl.subsetBy(new Comparable<SWGKnownResource>() {
            @Override
            public int compareTo(SWGKnownResource o) {
                return o.availableFirst().available() > mail.date()
                        ? -1
                        : 0;
            }
        });

        // now we have a list of depleted resources, create wrappers
        List<Wrapper> ret = new ArrayList<Wrapper>(depl.size());
        for (SWGKnownResource kr : depl)
            ret.add(new Wrapper(this, new SWGMutableResource(kr), kr));

        return ret;
    }

    /**
     * Returns the galaxy constant for the galaxy this report pertains to.
     * 
     * @return a galaxy constant
     */
    public SWGCGalaxy gxy() {
        return galaxy;
    }

    /**
     * Returns the mail which this report is based on.
     * 
     * @return a mail
     */
    public SWGMailMessage mail() {
        return mail;
    }

    /**
     * Deduces and returns a list of new resources. More formally, each element
     * of the returned list has no ID. However, a resource can be locally known
     * without ID or it may have been reported at SWGCraft.org by some player
     * after the most recent download.
     * 
     * @return a list of resources without ID
     */
    public List<Wrapper> news() {
        List<Wrapper> news = new ArrayList<Wrapper>();
        for (Wrapper wr : wrappers)
            if (wr.mutable.id() <= 0) news.add(wr);

        return news;
    }

    /**
     * Returns the planet constant for the planet which this report pertains to.
     * 
     * @return a planet constant
     */
    public SWGPlanet planet() {
        return planet;
    }

    /**
     * Extracts and returns a list of known resources which are listed at
     * SWGCraft.org without stats. More formally, each element of the returned
     * list has an ID but it has no values for its stats.
     * 
     * @return a list of known resources short of stats
     */
    public List<Wrapper> statless() {
        List<Wrapper> statless = new ArrayList<Wrapper>();
        for (Wrapper wr : wrappers)
            if (wr.mutable.id() > 0
                    && !wr.mutable.stats().hasValues())
                statless.add(wr);

        return statless;
    }

    @Override
    public String toString() {
        return String.format("SWGISDroidReport[%s/%s/%s]",
                galaxy.getName(), planet.getName(), type.rcName());
    }

    /**
     * Returns the generic resource class which this report pertains to.
     * 
     * @return a resource class constant
     */
    public SWGResourceClass type() {
        return type;
    }

    /**
     * Extracts and returns a list of known resources which are not reported for
     * the planet which this report pertains to. Each element of the returned
     * list is a resource which has an ID but it lacks a planet availability
     * info for the planet of this report. Technically they have been spawning
     * at the planet all the time but this fact is not reported at SWGCraft.org.
     * 
     * @return a list of known resources that are not listed at {@link #planet}
     */
    public List<Wrapper> unreportedForPlanet() {
        List<Wrapper> ret = new ArrayList<Wrapper>();
        for (Wrapper wr : wrappers)
            if (wr.mutable.id() > 0 && wr.known != null
                    && !wr.known.isAvailableAt(planet()))
                ret.add(wr);

        return ret;
    }

    /**
     * Returns {@code true} if the specified argument is not older than
     * {@link #MAX_AGE_HOURS} hours old.
     * 
     * @param mail the mail to check
     * @return {@code true} if the mail is not older than {@link #MAX_AGE_HOURS}
     */
    public static boolean isNew(SWGMailMessage mail) {
        long now = System.currentTimeMillis() / 1000L;
        return (now - mail.date()) <= (SWGISDroidReport.MAX_AGE_HOURS * 3600);
    }

    /**
     * Creates and returns an instance of this type which contains data parsed
     * from the specified mail. This method must only be called from
     * {@link SWGMailMessage#isdroidData(SWGResourceSet)}.
     * 
     * @param m a mail that is an ISDroid report
     * @param known a set of locally known resources for the appropriate galaxy
     * @return an instance based on the specified arguments
     * @throws IllegalArgumentException if the mail is not an ISDroid report or
     *         if an argument or anything parsed is invalid
     * @throws IllegalStateException if there is an error parsing the mail body
     * @throws InvalidNameException if a resource name is invalid
     * @throws NullPointerException if an argument or anything is {@code null}
     */
    static SWGISDroidReport newInstance(SWGMailMessage m, SWGResourceSet known)
            throws IllegalArgumentException, IllegalStateException,
            InvalidNameException, NullPointerException {

        if (m.type() != Type.ISDroid)
            throw new IllegalArgumentException("Not ISDroid report: " + m);

        String tmp;

        // Galaxy --- do not assume initial SWG
        // SWG.Europe-Chimaera.interplanetary survey droid
        // SWG.SWGCraft.co.uk.interplanetary survey droid
        int start = m.fromLine().indexOf('.') + 1;
        int end = m.fromLine().lastIndexOf('.');
        tmp = m.fromLine().substring(start, end);
        SWGCGalaxy g = SWGCGalaxy.fromName(tmp);

        // Planet
        // Interplanetary Survey: Rori - Flora Resources
        start = m.subject().indexOf(':') + 1;
        end = m.subject().indexOf('-', start);
        tmp = m.subject().substring(start, end).trim();
        SWGPlanet p = SWGPlanet.fromName(tmp);

        // Resource class
        // Interplanetary Survey: Rori - Flora Resources
        start = end + 1;
        tmp = m.subject().substring(start).trim();
        SWGResourceClass rc = SWGResourceClass.rc(tmp);

        SWGISDroidReport isd = new SWGISDroidReport(m, p, g, rc);

        // fill the isd.wrappers with everything from the mail
        processMail(m, isd, known);

        return isd;
    }

    /**
     * Helper method which parses the mail and populates the list of wrappers
     * for the specified report. If a parsed resource is known the wrapper
     * references that known instance and the mutable resource is a copy;
     * possibly the mutable resource is augmented but nevertheless it is a copy
     * of the known instance. New resources are conveyed as mutable resources.
     * 
     * @param mail the mail to parse
     * @param report the report to populate
     * @param known a set of locally known resources for the appropriate galaxy
     * @throws IllegalArgumentException if there is an argument or anything
     *         parsed that is invalid
     * @throws IllegalStateException if there is an error parsing the mail body
     * @throws InvalidNameException if a resource name is invalid
     * @throws NullPointerException if an argument or anything is {@code null}
     */
    @SuppressWarnings("synthetic-access")
    private static void processMail(
            SWGMailMessage mail, SWGISDroidReport report, SWGResourceSet known)
            throws IllegalArgumentException, IllegalStateException,
            InvalidNameException, NullPointerException {

        String[] body = mail.body();
        if (body.length <= 1) return; // nothing to do, 1 element == 'unavail'

        // scan for error in the body AND trim strings
        for (int i = 0; i < body.length; ++i) {
            if (body[i].startsWith("ERROR"))
                throw new IllegalStateException(body[i]);
            // else
            body[i] = body[i].trim();
        }

        String user = SWGCraft.getUserName();
        SWGPlanetAvailabilityInfo pai = new SWGPlanetAvailabilityInfo(
                report.planet(), mail.date(), user);

        // mail FORMAT is read at the bottom of this method

        for (int rci = 0, ni = 1; ni < body.length; ++ni) {
            String name = body[ni];

            if (name.startsWith("\\#pcontrast1")) {
                // first resource name found

                SWGResourceClass rc = SWGResourceClass.rc(body[rci]);

                int start = name.indexOf(' ') + 1;
                int end = name.lastIndexOf('\\');
                name = ZString.tac(name.substring(start, end));

                if (!ZString.isAlpha(name))
                    throw new InvalidNameException(String.format(
                            "Resource name \"%s\" in %s", name, mail));

                SWGKnownResource r = known.getBy(name, report.gxy());
                SWGMutableResource m;
                if (r == null) {
                    m = new SWGMutableResource(name, rc);
                    m.depleted(false);
                    m.galaxy(report.gxy());
                } else
                    m = new SWGMutableResource(r);

                m.availability(pai);

                // add the resource wrapper
                report.wrappers.add(new Wrapper(report, m, r));

            } else { // nameStr is not a resource name

                // ni (name index) is updated in the for-loop each turn
                //
                // update rci (type index) when there is not another name, this
                // clause .. but can be anything -- unused until we have a name
                //
                // the next line is always one of
                // 1) another name with same type > upper clause
                // 2) not another name > this clause > type is following behind
                //
                // see sample mail below

                rci = ni;
            }
        }
        // \#pcontrast3 Planet: \#pcontrast1 Lok
        // \#pcontrast3 Resource Class: \#pcontrast1 Mineral
        //
        // \#pcontrast3 Resources located...\#.
        //
        // Radioactive
        // Known Radioactive
        // Class 4 Radioactive
        // \#pcontrast1 Hafov\#.
        // Metal
        // Ferrous Metal
        // Steel
        // Rhodium Steel
        // \#pcontrast1 Esaca\#.
        // \#pcontrast1 Onida\#.
        // Kiirium Steel
        // \#pcontrast1 Trifiebeium\#.
    }

    /**
     * This type is a plain wrapper for one element of the ISDroid report. An
     * instance of this type references the ISDroid report for this wrapper and
     * it contains a mutable resource, and a reference to a known instance or
     * {@code null}. If {@code known != null} it must be the source for {@code
     * mutable} and vice versa, {@code mutable} must be a copy of {@code known},
     * possibly augmented but nevertheless a copy. The {@code known} reference
     * is a convenience reference that enables a client to have instant access
     * to the known counterpart. If {@code known == null} the wrapper denotes a
     * new resource.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public final static class Wrapper {

        /**
         * A flag that indicates if this element is submitted to SWGCraft.org.
         */
        public boolean isSubmitted = false;

        /**
         * A reference to a known instance, or {@code null}. If this field is
         * not {@code null} it is the source for {@code mutable} and {@code
         * mutable} is a copy of this field, possibly augmented with stats
         * and/or planets.
         */
        public final SWGKnownResource known;

        /**
         * The resource for this wrapper. If {@code this.known != null} this
         * field is a copy of {@code known}, possibly augmented with stats
         * and/or planets, but nevertheless a copy.
         */
        public final SWGMutableResource mutable;

        /**
         * The report which this wrapper pertains to.
         */
        public final SWGISDroidReport report;

        /**
         * Creates a wrapper for the specified arguments. If {@code known ==
         * null} the wrapper denotes a new resource. If {@code known != code
         * null} then {@code mutable} must be a copy of {@code known} when it
         * comes to ID, name, and galaxy; this constructor <i>does not </i>
         * validate this.
         * 
         * @param r an ISDroid report
         * @param mutable a mutable resource
         * @param known the known original for {@code mutable}, or {@code null}
         */
        private Wrapper(SWGISDroidReport r,
                SWGMutableResource mutable, SWGKnownResource known) {

            this.report = r;
            this.mutable = mutable;
            this.known = known;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof Wrapper) {
                Wrapper o = (Wrapper) obj;
                return mutable.getName().equals(o.mutable.getName())
						&& mutable.galaxy().equals(o.mutable.galaxy());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return mutable.getName().hashCode() + mutable.galaxy().hashCode();
        }

        @Override
        public String toString() {
            return String.format("ISDroidWrapper[%s:%s:%s]",
                    report, mutable, known);
        }
    }
}
