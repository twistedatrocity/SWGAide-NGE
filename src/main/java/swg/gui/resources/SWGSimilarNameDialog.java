package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

import swg.SWGAide;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGMutableResource;
import swg.gui.common.SWGJDialog;
import swg.gui.mail.SWGMailISDroidPanel;
import swg.swgcraft.SWGResourceManager;
import swg.swgcraft.SWGSoapNOResResponse;
import swg.swgcraft.SWGSoapStatusResponse;

/**
 * This dialog is used by clients that submit new resources to swgaide.com. A
 * client displays this dialog if a resource name is very similar to an existing
 * resource and the dialog presents the user with decisions and choices.
 * <p>
 * When submitting a new resource the resource manager determines if its name is
 * too similar to a cached resource and if that is the case it does not send the
 * resource but returns a {@link SWGSoapNOResResponse}. The client invokes
 * {@link #wrapperInstance(SWGSoapNOResResponse, SWGMutableResource)} and adds
 * the wrapper to a collection. Finally the client creates a dialog of this type
 * and invokes {@link #show(List)} to display this dialog The user has the
 * options to
 * <ul>
 * <li>do nothing and the resource is not submitted; this is when the suggested
 * resource is in error or there is an error that cannot be rectified here</li>
 * <li><b>submit new</b>; the similarity alert is a positive false and the
 * resource is submitted as new</li>
 * <li><b>submit name</b>; the name of the known resource is in error but the
 * suggested resource is correct, an edit message is sent</li>
 * </ul>
 * <p>
 * The feature to alert for similarly named resources is added to alert users
 * about a possible name problem so that the user looks into the issue and does
 * not submit something that causes an error. Not all name similarities are an
 * error but some are, hence this dialog has the options to to do nothing, to
 * send-new, or to send an edit message with a correction.
 * <p>
 * Currently SWGCraft does not guard against naming similarities; naming errors
 * occurs when a user submits a resource and another user submits this resource
 * without realizing the first entry, but which of the two that is wrong does
 * not matter. The second submission creates a double entry, if it is an Organic
 * the later submission marks the older unavailable which "hides" the problem,
 * otherwise the double is visible. If a third user submits this resource yet
 * again we have a triple and for Organics the former two are invisible. Only
 * very observant users will notice this type of errors.
 * <p>
 * To determine the similarity SWGAide uses the well known Levenshtein distance
 * which is the number of edits (remove, replace, or add) to transform one
 * string to the other; the smaller the value the higher the similarity.
 * However, the shorter the name the higher the likelihood that <i>any </i> edit
 * transforms to the other name and this algorithm emits false positives.
 * <p>
 * This type provides a nested class which wraps the two similar resources. A
 * list with such instances is provided and each such wrapper is displayed at
 * this dialog. The user has the options to submit-new (it is a false positive),
 * submit an edit-message for the older, or to do nothing.
 * <p>
 * A developer may want to update the client GUI for each submit action, then
 * see the private method {@link #updateParent(SWGMutableResource)}.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGSimilarNameDialog extends SWGJDialog {

    /**
     * The panel for GUI elements which display similarly named resources.
     * Initially this panel is empty and is populated by {@link #show(List)}.
     * The layout is {@link BoxLayout#PAGE_AXIS} which results in a list-like
     * display.
     */
    private JPanel alertList;

    /**
     * A counter that counts down for successful send-new and send-edit messages
     * that are sent to SWGCraft, when it reaches 0 all elements are handled
     * successfully.
     */
    private int count;

    /**
     * Creates an instance of this dialog ready to be displayed, see
     * {@link #show(List)}. Do not create this dialog for an empty list.
     * 
     * @param p the parent client that creates and uses this dialog
     */
    private SWGSimilarNameDialog(JComponent p) {
        super(p);
        make();
    }

    @Override
    protected void close() {
        // pass
    }

    /**
     * Helper method which creates the GUI stuff for this dialog.
     */
    private void make() {
        setTitle("Resolve naming similarities");
        setLocation(200, 50);

        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        panel.add(makeNorth(), BorderLayout.PAGE_START);
        panel.add(makeCenter(), BorderLayout.CENTER);
        panel.add(makeSouth(), BorderLayout.PAGE_END);

        add(panel);
    }

    /**
     * Helper method which creates and returns a panel for the center area; as a
     * side effect this method sets {@link #alertList} to this panel. This
     * component displays GUI elements for similarly named resources, each of
     * such element with buttons for send-new and send-edit messages.
     * 
     * @return a component
     */
    private Component makeCenter() {
        alertList = new JPanel();
        alertList.setLayout(new BoxLayout(alertList, BoxLayout.PAGE_AXIS));
        return alertList;
    }

    /**
     * Helper method which creates and returns a component for the northern
     * area. This component displays initial text for this dialog.
     * 
     * @return a component
     */
    private Component makeNorth() {
        JLabel l = new JLabel("The following resources have similar names:");
        return l;
    }

    /**
     * Helper method which creates and returns a component for the southern
     * area. This component contains usage text and a cancel button.
     * 
     * @return a component
     */
    private Component makeSouth() {
        JPanel p = new JPanel(new BorderLayout(0, 15));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                BorderFactory.createEmptyBorder(2, 3, 2, 3)));

        JButton cb = new JButton("Close");
        p.add(cb, BorderLayout.NORTH);
        cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        p.add(new JLabel("<html>&nbsp;<br/>" +
                "If your resource is wrong, do nothing<br/>" +
                "If your resource is correct and this is a false " +
                "alarm, press \"Submit new\"<br/>" +
                "If your resource is correct and you found a spelling " +
                "error, press \"Submit name\"<br/>" +
                "If both are wrong, skip and correct by other means" +
                "</html>"), BorderLayout.CENTER);

        return p;
    }

    /**
     * Helper method which for the specified response updates the label. If the
     * response is a success the text is tinted gray and {@link #count} is
     * decremented. If there is an error it is logged to SWGAide's log file and
     * the text of the label is tinted red.
     * 
     * @param l the label for this invocation
     * @param s the text that is displayed at the label
     * @param r the response from the SOAP action
     * @param mr the resource that was sent
     */
    private void response(
            JLabel l, String s, SWGSoapStatusResponse r, SWGMutableResource mr) {

        if (r.isFaultless()) {
            --count;
            mr.whatever = "OK";
        } else {
            mr.whatever = "ERR";

            // str.len minus len of </htlm>
            StringBuilder sb = new StringBuilder(s);
            sb.insert(s.length() - 7, "<br/>ERROR: see log file");
            l.setText(sb.toString());
            l.setForeground(Color.RED);

            SWGAide.printDebug("subd", 1,
                    "SWGSimilarNameDialog: " + r.getFaultMessage());
            pack();
        }
    }

    /**
     * Helper method which sends an edit-message to SWGCraft and invokes
     * {@link #response(JLabel, String, SWGSoapStatusResponse, SWGMutableResource)}
     * with the response.
     * 
     * @param mr the wrapper with the resource to send
     * @param kr the known resource to update
     * @param l the label for this invocation
     * @param s the text that is displayed at the label
     */
    private void sendEdit(
            SWGMutableResource mr, SWGKnownResource kr, JLabel l, String s) {
        
        SWGMutableResource ms = new SWGMutableResource(mr.getName(), kr.rc());
        SWGResourceManager.updateInstance(kr, ms);
        response(l, s, SWGResourceManager.sendEdit(kr), mr);
        updateParent(null);
    }

    /**
     * Helper method which sends a new-resource message to SWGCraft and invokes
     * {@link #response(JLabel, String, SWGSoapStatusResponse, SWGMutableResource)}
     * with the response.
     * 
     * @param mr the resource to send
     * @param l the label for this invocation
     * @param s the text that is displayed at the label
     */
    private void sendNew(SWGMutableResource mr, JLabel l, String s) {
        response(l, s, SWGResourceManager.sendNew(mr, true), mr);
        updateParent(mr);
    }

    /**
     * Makes this dialog visible and populated with the specified list of
     * wrappers for similarly named resources.
     * <p>
     * This method iterates over the elements of the argument and adds for each
     * element it a GUI component that displays the resources and presents the
     * user with two buttons for send-anyway and send an edit message.
     * 
     * @param wraps a list of wrappers
     */
    private void show(List<Wrapper> wraps) {
        count = wraps.size();
        for (Wrapper w : wraps) {
            wrapAdd(w);
            alertList.add(Box.createVerticalStrut(5));
        }

        pack();
        setVisible(true);
    }

    /**
     * Helper method which invokes an update method of {@link #parent}. If the
     * developer has not added code for his client to this method it does
     * nothing, except that if {@link #count} {@code <= 0} this dialog exits.
     * 
     * @param mr the resource just submitted, or {@code null}
     */
    private void updateParent(SWGMutableResource mr) {
        if (parent instanceof SWGSubmitTab)
            ((SWGSubmitTab) parent).updateMultiList();
        else if (parent instanceof SWGMailISDroidPanel)
            ((SWGMailISDroidPanel) parent).updateNewList(mr);

        if (count <= 0) {
            final Timer t = new Timer(2000, null); 
            t.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    t.stop();
                }
            });
            t.start();
            
        }
    }

    /**
     * Helper method which adds the specified wrapper to the GUI. This method
     * creates a GUI element which displays the content of the wrapper and two
     * buttons for send-new and send-edit. If the wrapper is {@code null} or in
     * error this method does nothing.
     * 
     * @param w a wrapper
     */
    
    private void wrapAdd(Wrapper w) {
        if (w == null || w.submit == null ||
                w.response == null || w.response.getStatus() != -2) return;

        final SWGMutableResource mr = w.submit;
        final List<JButton> bl = new ArrayList<JButton>();
        final List<JLabel> ll = new ArrayList<JLabel>();
        for (final SWGKnownResource kr : w.response.whatever) {
            final String s = String.format(
                    "<html>Yours: %s: %s<br/>Other: %s: %s</html>",
                    mr.getName(), mr.rc().rcName(),
                    kr.getName(), kr.rc().rcName());

            final JLabel l = new JLabel(s);
            ll.add(l);

            final JButton nb = new JButton("Submit new");
            nb.setToolTipText("Send as new: this alert is a false positive");
            bl.add(nb);

            final JButton eb = new JButton("Submit name");
            eb.setToolTipText("Send the name of YOUR resource to update the other");
            bl.add(eb);

            nb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (JLabel el : ll) el.setForeground(Color.GRAY);
                    for (JButton el : bl) el.setEnabled(false);
                    sendNew(mr, l, s);
                }
            });

            eb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (JLabel el : ll) el.setForeground(Color.GRAY);
                    for (JButton el : bl) el.setEnabled(false);
                    sendEdit(mr, kr, l, s);
                }
            });

            Box b = Box.createHorizontalBox();
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    BorderFactory.createEmptyBorder(2, 3, 2, 3)));
            b.add(l);
            b.add(Box.createHorizontalGlue());
            b.add(nb);
            b.add(eb);
            alertList.add(b);
        }
    }

    /**
     * Creates and displays this dialog; the dialog is populated with the
     * wrappers of the specified list. If the list is {@code null} or empty this
     * method does nothing.
     * <p>
     * <b>Notice:</b> while processing, for each element of the specified list
     * and determined by the result the field {@code Wrapper.submit.whatever} is
     * set to {code Boolean.TRUE} or {@code FALSE}. This field may be used by
     * the client GUI.
     * <p>
     * If the client should be updated per element, see the class comment and
     * add code to a helper method of this type.
     * 
     * @param c the invoking client
     * @param wrappers a list of wrappers for similarly named resources
     * @throws NullPointerException the component is {@code null}
     */
    public static void display(JComponent c, List<Wrapper> wrappers) {
        if (c == null) throw new NullPointerException("Component is null");
        if (wrappers == null || wrappers.isEmpty()) return;

        SWGSimilarNameDialog d = new SWGSimilarNameDialog(c);
        d.show(wrappers);
    }

    /**
     * Creates and returns a wrapper for the two arguments. The instance is used
     * for resources which have very similar names to avoid doubles during
     * submission. The wrapper should be added to the collection that is used to
     * show this dialog.
     * <p>
     * <b>Notice:</b> during later processing, determined by the result the
     * field {@code Wrapper.submit.whatever} is set to {@code Boolean.TRUE} or
     * {@code FALSE}. This field may be used by the client GUI.
     * <p>
     * If the client should be updated per wrapper, see the class comment and
     * add code to a helper method of this type.
     * 
     * @param resp the response from the resource manager
     * @param mr the resource the user tries to submit
     * @return a wrapper
     * @throws NullPointerException if an argument is {@code null}
     */
    
    public static Wrapper wrapperInstance(
            SWGSoapNOResResponse resp, SWGMutableResource mr) {
        return new Wrapper(resp, mr);
    }

    /**
     * This is a wrapper that contains a {@link SWGSoapNOResResponse} from
     * {@link SWGResourceManager#sendNew(SWGMutableResource, boolean)} with the
     * similarly named resource, and the resource the user wants to submit.
     * Instances of this type is created by a factory method of the enclosing
     * type.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public static class Wrapper {

        /**
         * The SOAP response from the resource manager
         */
        private final SWGSoapNOResResponse response;

        /**
         * The resource the user tries to submit
         */
        private final SWGMutableResource submit;

        /**
         * Creates a wrapper for the two argument.
         * 
         * @param resp a response from the resource manager
         * @param mr a resource that the user tries to submit
         * @throws NullPointerException if an argument is {@code null}
         */
        private Wrapper(SWGSoapNOResResponse resp, SWGMutableResource mr) {
            if (resp == null || mr == null)
                throw new NullPointerException("An argument is null");
            this.response = resp;
            this.submit = mr;
        }
    }
}
