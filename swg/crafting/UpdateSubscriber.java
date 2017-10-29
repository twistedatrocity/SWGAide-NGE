package swg.crafting;

/**
 * The interface to subscribe for update notifications from different managers
 * which accept subscribers to be notified for any kind of update. The client
 * which subscribes on notifications implements this interface. When an update
 * is ready, that instances's {@code handleUpdate} method is invoked.
 * <p>
 * When a client receives a notification it determines whether to execute some
 * routine, or to ignore the notification, and also if the job must be
 * dispatched on a worker thread.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public interface UpdateSubscriber {

    /**
     * Invoked by a update notifier. The argument is an implementation of
     * {@link UpdateNotification}. It can be an anonymous class or an
     * implementation defined in the package of the notifier. The implementation
     * may define variables or methods at the discretion of the notifier.
     * <p>
     * The invoked client determines whether to execute some routine, or to
     * ignore the notification, and also if the job must be dispatched on a
     * worker thread.
     * 
     * @param u
     *            the update notification
     */
    public void handleUpdate(UpdateNotification u);
}
