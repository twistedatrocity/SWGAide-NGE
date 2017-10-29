package swg.gui.common;

/**
 * A callback class that can be used by objects for callback methods.<BR/>
 * Example:
 * 
 * <PRE>myCallbackObject = new SWGDoTask(new SWGDoTask.TaskCallback() {
 * 
 *     public void execute() {
 *     // do something
 *     }
 * });
 * ...
 * myCallbackObject.callback.execute();</PRE>
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
public class SWGDoTask {

    /**
     * The callback object for the task
     */
    public TaskCallback callback;

    /**
     * Creates a wrapper for the callback that will be executed when called
     * 
     * @param callback the callback object
     */
    public SWGDoTask(TaskCallback callback) {
        this.callback = callback;
    }

    /**
     * The callback interface with its method.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public interface TaskCallback {

        /**
         * The callback method
         */
        void execute();
    }
}
