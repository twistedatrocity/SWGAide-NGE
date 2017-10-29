package swg.gui.trade;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import swg.gui.SWGFrame;
import swg.gui.common.SWGDocNumberFilter;
import swg.gui.trade.SWGTradeTab.SWGTradeCustomer;
import swg.tools.ZNumber;
import swg.tools.ZString;

/**
 * This component displays customers for a character at the Trade tab. It
 * display customers and provides some filtering options and means to build a
 * list of customer names that can be pasted to the TO field of in-game mails.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGTradeCustomerTab extends JPanel {

    /**
     * A check box to filter for only <i>new </i> customers. That is customers
     * since the last "Save" was made.
     */
    private JCheckBox checkOnlyNewCustomers;

    /**
     * A check box to filter for customers since last time a "Save" was made.
     */
    private JCheckBox checkSinceLastTime;

    /**
     * A text field that displays all customers who are derived from the current
     * list of sales mails.
     */
    private JTextArea customersAll;

    /**
     * A text field that displays the filtered list of customers.
     */
    private JTextArea customersFiltered;

    /**
     * A filtered list of customers; just filters at this component apply.
     */
    private List<String> customersFilteredList;

    /**
     * A flag that denotes if this component is fully created, or yet a stub.
     */
    private boolean isGuiFinished;

    /**
     * A flag that denotes if the GUI is updating itself and some action should
     * not be allowed.
     */
    private boolean isWorking;

    /**
     * A text field to filter on max number of days since last visit.
     */
    private JTextField maxDays;

    /**
     * A text field to filter on minimum number of credits a customer has spent.
     */
    private JTextField minCredits;

    /**
     * A text field to filter on minimum number of visits for a customer.
     */
    private JTextField minVisits;

    /**
     * The parent component which is the main Trade tabbed pane.
     */
    private final SWGTradeTab tradeTab;

    /**
     * Creates a an instance of this type. This constructor creates a stub and
     * the first time the user visits this panel creation is finished lazily.
     * 
     * @param tradeTab the main Trade tabbed pane
     */
    SWGTradeCustomerTab(SWGTradeTab tradeTab) {
        this.tradeTab = tradeTab;

        tradeTab.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });
    }

    /**
     * Called when the user presses the "Clear" button. This method clears all
     * filter fields and triggers an update of the view.
     */
    private void actionClear() {
        isWorking = true;
        checkOnlyNewCustomers.setSelected(false);
        checkSinceLastTime.setSelected(false);
        minCredits.setText("");
        minVisits.setText("");
        maxDays.setText("");
        isWorking = false;
        actionFilterView();
    }

    /**
     * Called when a user action triggers an update of the filter view. This
     * method queries all filters, updates the filtered list of users, and
     * updates the GUI. If no filter is active the filtered list equals the list
     * of all customers.
     */
    private void actionFilterView() {
        if (isWorking) return;
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings( { "synthetic-access", "unchecked" })
            public void run() {
                String characterName = tradeTab.character.getNameComplete();

                int minCred = ZNumber.intVal(minCredits.getText());
                int minVist = ZNumber.intVal(minVisits.getText());
                long maxDys = ZNumber.longVal(maxDays.getText());
                long now = System.currentTimeMillis() / 1000; // seconds
                maxDys = maxDys > 0
                        ? Math.max(now - (maxDys * 3600 * 24), 0)
                        : 0;

                String ls = "tradeLatest" + characterName;
                Long ltst = (Long) SWGFrame.getPrefsKeeper().get(ls);
                long latest = ltst != null && checkSinceLastTime.isSelected()
                        ? ltst.longValue()
                        : 0;

                String cs = "tradeCustomers" + characterName;
                HashSet<String> set = checkOnlyNewCustomers.isSelected()
                        ? (HashSet<String>) SWGFrame.getPrefsKeeper().get(cs)
                        : null;

                Map<String, SWGTradeCustomer> map = tradeTab.getCustomers();
                customersFilteredList = new ArrayList<String>(map.size());
                for (SWGTradeCustomer cust : map.values()) {
                    if (cust.visits >= minVist && cust.sum >= minCred
                            && cust.lastVisit >= maxDys
                            && cust.lastVisit >= latest) {
                        if (set != null && set.contains(cust.name)) continue;
                        customersFilteredList.add(cust.name);
                    }
                }
                Collections.sort(customersFilteredList);

                customersFiltered.setText(
                        formatNames(customersFilteredList));

                updateLogBar();
            }
        });
    }

    /**
     * Called when the user presses the "Save" button. This method saves the
     * current state for future filtering with "Since ..." or "New since ...".
     */
    private void actionSave() {
        String characterName = tradeTab.character.getNameComplete();
        String keySet = "tradeCustomers" + characterName;
        HashSet<String> set = new HashSet<String>(tradeTab.getCustomerNames());
        SWGFrame.getPrefsKeeper().add(keySet, set);

        String keyTime = "tradeLatest" + characterName;
        Long now = new Long(System.currentTimeMillis() / 1000L);
        SWGFrame.getPrefsKeeper().add(keyTime, now);
    }

    /**
     * Invoked when the user visits or leaves the main Trade tabbed panel or any
     * of the siblings for this component.
     * 
     * @param hasFocus {@code true} if the main tabbed pane has or gained focus
     */
    void focusGained(boolean hasFocus) {
        if (hasFocus && tradeTab.getSelectedComponent() == this) {
            if (!isGuiFinished) make();

            customersAll.setText(formatNames(tradeTab.getCustomerNames()));
            actionFilterView();
            updateLogBar();
        }
    }

    /**
     * Helper method which returns a formatted string. The names are formatted
     * for easy copy and paste to in-game mails, sorted alphabetically, and
     * added to the returned string. The format is "name; anothername; ... ".
     * 
     * @param names a list of customer names
     * @return a string with names
     */
    private String formatNames(List<String> names) {
        ZString z = new ZString();
        for (String name : names)
            z.app(name).app("; ");

        return z.toString();
    }

    /**
     * Helper method which lazily finishes creation of this type. If GUI is
     * already finished this method does nothing.
     */
    private synchronized void make() {
        if (isGuiFinished) return;

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        customersAll = new JTextArea(10, 10);
        add(makeTextPane("All customers for the current trade mails",
                customersAll, false), BorderLayout.CENTER);

        customersFiltered = new JTextArea(9, 10);
        add(makeTextPane("Build a filtered list of customers",
                customersFiltered, true), BorderLayout.PAGE_END);

        isGuiFinished = true;
    }

    /**
     * Helper method which creates and returns a component with filtering
     * options.
     * 
     * @return a GUI component
     */
    private Component makeFilterArea() {
        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEADING));
        p.setMaximumSize(new Dimension(5000, 30));

        p.add(new JLabel("Min visits: "));
        minVisits = makeFilterField();
        p.add(minVisits);

        p.add(Box.createRigidArea(new Dimension(10, 5)));
        p.add(new JLabel("Min credits: "));
        minCredits = makeFilterField();
        p.add(minCredits);

        p.add(Box.createRigidArea(new Dimension(10, 5)));
        String tt = "Display only customers within days";
        JLabel mDL = new JLabel("Recent days: ");
        mDL.setToolTipText(tt);
        p.add(mDL);
        maxDays = makeFilterField();
        maxDays.setToolTipText(tt);
        p.add(maxDays);

        p.add(Box.createRigidArea(new Dimension(20, 5)));

        Box hb = Box.createHorizontalBox();
        hb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        checkOnlyNewCustomers = new JCheckBox("New-comers");
        checkOnlyNewCustomers.setToolTipText(
                "Display only new customers");
        checkOnlyNewCustomers.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionFilterView();
            }
        });
        hb.add(checkOnlyNewCustomers);

        checkSinceLastTime = new JCheckBox("Since ...");
        checkSinceLastTime.setToolTipText(
                "Display only customers since last \"Save\"");
        checkSinceLastTime.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionFilterView();
            }
        });
        hb.add(checkSinceLastTime);

        p.add(Box.createRigidArea(new Dimension(15, 15)));
        JButton s = new JButton("Save");
        s.setMnemonic('C');
        s.setToolTipText("Save current date and the customer list");
        s.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionSave();
            }
        });
        hb.add(s);
        p.add(hb);

        p.add(Box.createRigidArea(new Dimension(20, 5)));

        p.add(Box.createRigidArea(new Dimension(15, 15)));
        JButton c = new JButton("Clear");
        c.setMnemonic('C');
        c.setToolTipText("Clear the filters");
        c.addActionListener(new ActionListener() {
            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionClear();
            }
        });
        p.add(c);
        return p;
    }

    /**
     * Helper method which creates and returns a text input field with a number
     * document filter and a document-listener that invokes
     * {@link #actionFilterView()}.
     * 
     * @return a GUI text field
     */
    private JTextField makeFilterField() {
        JTextField tf = new JTextField(3);
        AbstractDocument ad = (AbstractDocument) tf.getDocument();
        ad.setDocumentFilter(new SWGDocNumberFilter());
        ad.addDocumentListener(new DocumentListener() {
            @SuppressWarnings("synthetic-access")
            public void changedUpdate(DocumentEvent e) {
                actionFilterView();
            }

            @SuppressWarnings("synthetic-access")
            public void insertUpdate(DocumentEvent e) {
                actionFilterView();
            }

            @SuppressWarnings("synthetic-access")
            public void removeUpdate(DocumentEvent e) {
                actionFilterView();
            }
        });
        return tf;
    }

    /**
     * Helper method which creates, adds components to, and returns a GUI
     * component that displays a list of customers. Optionally this method
     * invokes {@link #makeFilterArea()}.
     * 
     * @param title a title for for the border
     * @param textArea the main text area that is added
     * @param addFilters {@code true} to add a panel of filters
     * @return a GUI component
     */
    private Component makeTextPane(
            String title, JTextArea textArea, boolean addFilters) {

        Box box = Box.createVerticalBox();
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), title),
                BorderFactory.createEmptyBorder(5, 10, 10, 10)));

        if (addFilters) box.add(makeFilterArea());

        textArea.setEditable(false);
        textArea.setBackground(Color.WHITE);
        textArea.setBorder(BorderFactory.createLoweredBevelBorder());
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        box.add(new JScrollPane(textArea));
        return box;
    }

    /**
     * Updates the log bar with data for this component.
     */
    void updateLogBar() {
        if (customersFilteredList != null)
            tradeTab.frame.putToLogbar_2(tradeTab.getCustomers().size() + " / "
                    + customersFilteredList.size() + " customers   ");
    }
}
