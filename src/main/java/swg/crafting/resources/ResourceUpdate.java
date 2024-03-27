package swg.crafting.resources;

import swg.crafting.UpdateNotification;
import swg.crafting.UpdateSubscriber;
import swg.model.SWGCGalaxy;
import swg.swgcraft.SWGResourceManager;

/**
 * This type represents a notification regarding resources when when there is an
 * update of some kind. The update can be about a single resource or a
 * collection of resources, and there is no distinction between depletion or
 * spawning. The subscriber determines whether to execute some routine, or to
 * ignore the notification, and also if the job must be dispatched on a worker
 * thread.
 * <P>
 * It is {@link SWGResourceManager} that publishes notifications of this type.
 * Clients implements the interface {@link UpdateSubscriber} and subscribes via
 * {@link SWGResourceManager#addSubscriber(UpdateSubscriber)} and revokes via
 * {@link SWGResourceManager#removeSubscriber(UpdateSubscriber)}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public final class ResourceUpdate implements UpdateNotification {

    /**
     * An optional object for the subscriber, or {@code null}. See the comments
     * for the specified type if there is a requirement on this argument.
     */
    public final Object optional;

    /**
     * A constant which identifies the type of notification for this instance.
     */
    public final UpdateType type;

    /**
     * Creates an instance of this type with an optional object that relates to
     * the specified type of update.
     * <P>
     * <B>Note: </B>See the comments for the specified {@link UpdateType} to
     * determine if there is a requirement on the optional object.
     * 
     * @param type
     *            the type of update this notification is about
     * @param optional
     *            an optional object, which can be of any kind; see comment for
     *            the specified type if there is a requirement on this argument
     * @throws IllegalArgumentException
     *             if {@code optional} is invalid for the specified type
     * @throws NullPointerException
     *             if there is an argument which is {@code null}
     */
    public ResourceUpdate(UpdateType type, Object optional) {
        if (type == null)
            throw new NullPointerException("Argument is null");
        if ((type == UpdateType.LOCAL_SUBMISSION || type == UpdateType.NEW_DOWNLOAD)
            && !SWGCGalaxy.class.isAssignableFrom(optional.getClass()))
            throw new IllegalArgumentException("Galaxy is invalid");

        this.type = type;
        this.optional = optional;
    }

    /**
     * A constant which identifies the type of notification.
     */
    public enum UpdateType {

        // Enum can be extended if necessary

        /**
         * A local submission of one or several new, updated, and/or depleted
         * resources is finished and an updated set of resources can be
         * requested from the resource manager. When this is the specified
         * notification type {@link #optional} must be galaxy constant of type
         * {@link SWGCGalaxy}.
         */
        LOCAL_SUBMISSION,

        /**
         * A new download of current resources is finished and an updated set of
         * resources can be requested from the resource manager. When this is
         * the specified notification type {@link #optional} must be galaxy
         * constant of type {@link SWGCGalaxy}.
         */
        NEW_DOWNLOAD,
        
        /**
         * This reflects a change in how the user wants resource caps to be treated.
         * When it's true, this app is to use JTL resources as the upper limits for
         * the impacted resource types. When it's false, it should use the pre-existing
         * rules for determining resource caps. When this is the specified notification 
         * type {@link #optional} must be {@link boolean} that shows the new state.
         */
        JTL_RESOURCE_CAP;
    }
}
