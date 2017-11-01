package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;

import swg.SWGAide;
import swg.crafting.Stat;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGMutableResource;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceStats;
import swg.gui.JComboWiderPopup;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDocNumberFilter;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJDialog;
import swg.model.SWGCGalaxy;
import swg.swgcraft.SWGResourceManager;
import swg.tools.ZNumber;
import swg.tools.ZString;
import swg.tools.ZTraversalPolicy;

/**
 * A dialog for adding or editing an inventory entry for an assignee.
 * Precautions are taken so that this dialog hinders the user from actions which
 * are not consistent with how crafting in SWG works and not to pollute SWGAide
 * with invalid data.
 * <p>
 * Technical comments:<br/>
 * The GUI fields are quite passive, only performing tasks which are expected
 * intuitively. While creating an inventory wrapper all fields are editable.
 * However, while editing a wrapper only the amount and notes are editable, and
 * if the SWGCraft ID is unknown also the stats and the galaxy chooser are
 * enabled. The other components are disabled and they should be, certainly so
 * for resources which are known at SWGCraft.org. In any case, this decision
 * makes it somewhat easier to maintain the call chains.
 * <p>
 * XXX: Add so this dialog suggests to submit resources unknown at SWGCraft.org
 * as an old resource.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGInventoryDialog extends SWGJDialog implements ActionListener {

    /**
     * A list of resource classes for space and recycled resource classes.
     */
    private static Vector<SWGResourceClass> rcSpaceRecycled;

    /**
     * A list of resource classes which can spawn in the worlds.
     */
    private static Vector<SWGResourceClass> rcSpawnable;

    /**
     * The title for this dialog.
     */
    private static final String title = "Inventory entry";

    /**
     * A input field for the amount of the resource.
     */
    private JTextField amount;

    /**
     * A component for assignees. This component is editable to allow users to
     * add custom assignees.
     */
    private JComboBox<String> assigneeList;

    /**
     * The galaxy constant which is specified when this dialog is opened.
     */
    private SWGCGalaxy galaxy;

    /**
     * A component at which to select resource class.
     * <p>
     * <b>Note: </b>This field is disabled while editing a wrapper.
     */
    private JComboBox<Object> galaxyList;

    /**
     * A helper flag which denotes if GUI action listeners should ignore an
     * invocation. If this flag is {@code true} some methods does nothing.
     */
    private boolean isGuiUpdating;

    /**
     * A button for the action to look up resources at SWGCraft.org.
     */
    private JButton lookupButton;

    /**
     * A temporary reference to a resource which was just looked up. If the user
     * looked up a resource and only one was found this member references it,
     * otherwise this member is {@code null}. This reference makes it possible
     * for SWGAide to surpass stat-limits for resources which are known at
     * SWGCraft.org even if it has stats outside limits. It is 100% certain that
     * such resources exist, perhaps just at Mustafar.
     */
    private SWGKnownResource lookupResource;

    /**
     * A text input area for misc user notes regarding the entry.
     */
    private JTextArea notesField;

    /**
     * A component at which to select resource class.
     * <p>
     * <b>Note: </b>This field is disabled while editing a wrapper.
     */
    private JComboBox<SWGResourceClass> resourceClassNameList;

    /**
     * An input field for the name of the resource. This component is also used
     * during "lookup" to display multiple resources returned from SWGCraft.org.
     * <p>
     * <b>Note: </b>This field is disabled while editing a wrapper.
     */
    private JComboWiderPopup<String> resourceNameList;

    /**
     * A GUI check box which the user can toggle to create entries for space or
     * recycled resources.
     */
    private JCheckBox spaceOrRecycledCB;

    /**
     * An array of stat fields for a resource. This array is ordered per the
     * model while the GUI displays the stats in in-game order.
     */
    private JTextField[] statFields;

    /**
     * The wrapper for the resource which is currently edited, {@code null} if
     * the call pertains to creation.
     */
    private SWGInventoryWrapper wrap;

    /**
     * Creates a GUI dialog of this type.
     * 
     * @param inventoryTab the component which is the parent for this dialog
     */
    SWGInventoryDialog(SWGInventoryTab inventoryTab) {
        super(title, inventoryTab);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(0, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Vector<Component> travOrder = new Vector<Component>(20);

        contentPane.add(makeNorth(travOrder), BorderLayout.PAGE_START);
        contentPane.add(makeCenter(), BorderLayout.CENTER);
        contentPane.add(makeSouth(travOrder), BorderLayout.PAGE_END);

        this.setContentPane(contentPane);
        this.pack();
        this.setMinimumSize(this.getSize());

        Point p = inventoryTab.getLocation();
        p.translate(100, 100);
        p = (Point) SWGFrame.getPrefsKeeper().get(
                "inventoryAddDialogLocation", p);
        p = SWGGuiUtils.ensureOnScreen(p, this.getSize());
        this.setLocation(p);

        ZTraversalPolicy tp = new ZTraversalPolicy(travOrder);
        this.setFocusTraversalPolicy(tp);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        String act = e.getActionCommand();
        if (src == lookupButton) {
            if (wrap == null)
                lookupResourceName();
            else
                requestResourceInfo(getResourceName());
            return;
        } else if (act.equalsIgnoreCase("OK")) {
            if (!isContentValid() || !save()) {
                lookupResource = null;
                return;
            }
        } else if (act.equalsIgnoreCase("Cancel")) {
            // pass
        } else
            SWGAide.printError("InventoryDialog:actionPerformed: " + src, null);

        close();
    }

    /**
     * Called when the user selects to create an entry for a recycled resource.
     * This method enables and sets other GUI components accordingly.
     */
    private void actionRecycledSpaceToggled() {
        boolean b = spaceOrRecycledCB.isSelected();
        lookupButton.setEnabled(!b);
        galaxyList.setEnabled(!b);

        resourceClassUpdate(b);

        reset();
        resourceClassNameList.setEnabled(b);
    }

    /**
     * Called when the users selects a resource class, or the selection is done
     * as a result of a background action.
     */
    private void actionResourceClassSelected() {
        SWGResourceClass rc = getResourceClass();

        if (rc == null) {
            statFieldsEditable(null, false);
        } else {
            if (rc.isSpaceOrRecycled()) {
                statFieldsRecycledOrSpace(rc);
                resourceNameList.removeAllItems();
                resourceNameList.addItem(rc.rcName());
            } else if (wrap != null && wrap.getResource().id() > 0)
                statFieldsEditable(rc, false);
            else
                statFieldsEditable(rc, true);
        }
    }

    /**
     * Called when the user selects a resource name at the GUI name list or if
     * the focus is lost after an entry at the list is edited.
     * <ul>
     * <li>if no name is selected this method does nothing
     * <li>if only one resource name is available this method returns and defers
     * further handling to {@code lookupResourceName()}
     * <li>otherwise this method calls {@code setResourceInfo(String[])} with
     * the selected resource name
     * </ul>
     */
    private void actionResourceNameSelected() {
        ((JTextComponent) resourceNameList.getEditor().getEditorComponent())
                .setCaretPosition(0); // left-align the name

        String item = (String) resourceNameList.getSelectedItem();
        if (item == null || item.trim().isEmpty())
            reset();

        if (resourceNameList.getSelectedIndex() < 0 // nothing selected
                || resourceNameList.getItemCount() <= 1)
            return; // max one >> lookup-button

        String grf = getResourceName();
        if (grf == null)
            return;
        requestResourceInfo(grf);
    }

    @Override
    protected void close() {
        SWGFrame.getPrefsKeeper().add(
                "inventoryAddDialogLocation", getLocation());
        setVisible(false);
    }

    /**
     * Returns a galaxy constant for the galaxy which is selected at the galaxy
     * selection list, or the current galaxy if none is selected.
     * 
     * @return a galaxy constant
     */
    private SWGCGalaxy getGalaxy() {
        SWGCGalaxy g = (SWGCGalaxy) galaxyList.getSelectedItem();
        return g == null
                ? galaxy
                : g;
    }

    /**
     * Returns the selected resource class. If there is no item selected {@code
     * null} is returned.
     * 
     * @return the selected resource class, or {@code null}
     */
    private SWGResourceClass getResourceClass() {
        if (resourceClassNameList.getSelectedIndex() < 0)
            return null;
        return (SWGResourceClass) resourceClassNameList.getSelectedItem();
    }

    /**
     * Helper method which returns a resource name. This is the normalized
     * resource name entered or selected at {@code resourceNameList}. If no item
     * is selected {@code null} is returned.
     * 
     * @return a resource name, or {@code null}
     */
    private String getResourceName() {
        String item = (String) resourceNameList.getSelectedItem();
        if (item != null) {
            item = ZString.tac(item);
            int i = item.indexOf(" : ");
            if (i > 0)
                item = item.substring(0, i);
        }
        return item;
    }

    /**
     * Helper method which returns an integer array with the values from the
     * stat input fields.
     * 
     * @return the values from the stat input fields
     */
    private int[] getStatFieldValues() {
        int[] flt = new int[Stat.COUNT];
        for (int i = 0; i < flt.length; ++i)
            flt[i] = ZNumber.intVal(statFields[i].getText());
        return flt;
    }

    /**
     * Helper method which validates the current content at the GUI and returns
     * {@code true} if the dialog is valid. If there is invalid data this method
     * displays a GUI message dialog and returns {@code false}.
     * 
     * @return {@code true} if the dialog has valid content
     */
    private boolean isContentValid() {
        if (wrap != null && wrap.getResource().id() > 0)
            return true; // name, resources class, and stats are not editable

        String msg = null;
        String ttl = null;

        String name = getResourceName();
        SWGResourceStats stats = new SWGResourceStats(getStatFieldValues());
        if ((msg = SWGResourceClass.validate(
                stats, getResourceClass())) != null) {
            if (lookupResource != null
                    && getResourceClass().rcName().startsWith("Must")) {

                msg = "One or several stats are outside caps.\n"
                        + "Verify the stats, if they are correct press OK,\n"
                        + "otherwise Cancel, correct the resource at\n"
                        + "SWGCraft.org, and then add to inventory";
                return JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
                        SWGAide.frame(), msg, "Outside caps",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
            }
            // else
            ttl = "Stats outside caps";
        } else if (name == null || name.length() < 3) {
            msg = "Enter a resource name, 3 letters or more";
            ttl = "Invalid resource name";
        } else if (resourceClassNameList.getSelectedIndex() < 0) {
            msg = "Select a resource class for this resource";
            ttl = "No resource class";
        }
        if (msg != null) {
            JOptionPane.showMessageDialog(
                    amount, msg, ttl, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (wrap != null
                && !stats.equals(wrap.getResource().stats())
                && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                        amount, "Stats are edited, use the new values?",
                        "Confirm values", JOptionPane.YES_NO_OPTION, 
                        JOptionPane.QUESTION_MESSAGE)) { return false; }
        return true;
    }

    /**
     * Helper method to {@code lookupButton}. This method executes in two
     * logical phases, the latter is split up determined by the result obtained
     * from SWGCraft.org:
     * <ol>
     * <li>Validate the text from the resource name input field, possibly just
     * initial letters.</li>
     * <li>Lookup the input at SWGCraft.org and obtain a list of possible
     * resources.</li>
     * </ol>
     * <p>
     * <dl>
     * The content of the obtained list determines which path to choose, the
     * possible content is:
     * <dt>Empty</dt>
     * <dd>A GUI dialog displays the fact and this method exits.</dd>
     * <p>
     * <dt>Exactly one value...</dt>
     * <dd>
     * <dl>
     * <dt>Perfect match</dt>
     * <dd>The name of the obtained resource equals the user input; this method
     * dispatches to {@code obtainResourceInfo(String)}.</dd>
     * <dt>Mismatch</dt>
     * <dd>A GUI dialog gives the user the options to...<br/>
     * &mdash; choose the obtained value, handled as a "perfect match"<br/>
     * &mdash; cancel</dd>
     * </dl>
     * </dd>
     * <dt>Several values</dt>
     * <dd>This method adds the obtained list to the GUI component for resource
     * names and returns. Further actions are deferred and is driven by the user
     * who may select an entry at the resource name list.</dd>
     * </dl>
     * Basically, this method either does nothing, calls {@code
     * obtainResourceInfo(String)}, or defers further actions to the user.
     */
    private void lookupResourceName() {
        String resName = getResourceName();
        if (resName == null)
            return;

        lookupResource = null;
        String errorMsg = null;

        if (resName.isEmpty())
            errorMsg = "Enter a resource name or its first letters";
        else if (resName.length() < 3)
            errorMsg = "Enter minimum 3 letters for resource name";

        if (errorMsg != null) {
            JOptionPane.showMessageDialog(galaxyList, errorMsg,
                    "Invalid resource name", JOptionPane.ERROR_MESSAGE);
            reset();
            resourceNameList.addItem(resName);
            return;
        }

        SWGCGalaxy gxy = getGalaxy();

        List<String> response = ((SWGInventoryTab) parent).
                lookupResources(resName, gxy);

        reset();
        galaxyList.setSelectedItem(gxy);

        if (response.size() == 0) {
            errorMsg = String.format(
                    "No resource %s at %s is found at SWGCraft.org",
                    resName, gxy.getName());
            resourceClassNameList.setEnabled(true);
        } else if (response.size() == 1 && response.get(0).startsWith("ERROR"))
            errorMsg = response.get(0);
        else if (response.size() == 1) {
            String line = response.get(0);
            String name = line.substring(0, line.indexOf(" : "));
            name = ZString.tac(name);

            if (name.equals(resName)) { // exact match
                resourceNameList.addItem(name);
                requestResourceInfo(name);
            } else {
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                        galaxyList,
                        String.format(
                                "\"%s\" not found but found%n\"%s\"%n%nUse this resource?",
                                resName, line), "Resource ambiguity",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {

                    resourceNameList.addItem(name);
                    requestResourceInfo(name);
                } else {
                    resourceNameList.addItem(resName);
                    resourceClassNameList.setEnabled(true);
                }
            }
            try {
                lookupResource = SWGResourceManager.getInstance(name, gxy);
            } catch (IOException e) {
                SWGAide.printDebug(
                        "invd", 1, "SWGInventoryDialog:lookupResourceName: "
                                + e.getMessage());
            }
        } else { // res.size() > 1
            // the full line is added, looking as "Vader : Desh Copper"
            for (String rLine : response)
                resourceNameList.addItem(rLine);

            resourceNameList.setSelectedIndex(-1);
            resourceNameList.showPopup();
        }

        if (errorMsg != null) {
            JOptionPane.showMessageDialog(galaxyList, errorMsg,
                    "Lookup error", JOptionPane.ERROR_MESSAGE);
            resourceNameList.addItem(resName);
        }
    }

    /**
     * Creates and returns the GUI component for resource stats. The component
     * displays a row of text fields for the resource stat in in-game order, but
     * {@link #statFields} is ordered per the model.
     * 
     * @return a GUI component
     */
    private Component makeCenter() {
        JPanel b = new JPanel(new GridLayout(2, 11));
        b.setBorder(BorderFactory.createEmptyBorder(0, 2, 10, 2));

        JTextField[] stf = new JTextField[Stat.COUNT];
        final Vector<Component> travOrder = new Vector<Component>(Stat.COUNT);

        // create and add labels
        for (Stat s : Stat.gameOrder())
            b.add(makeTextFieldLabel(s.name()));

        // create and add text fields
        for (Stat s : Stat.gameOrder()) {
            JTextField tf = makeTextField(String.format(
                    "%s - %s", s.name(), s.getDescription()));
            stf[s.i] = tf;
            b.add(tf);
            travOrder.add(tf);
        }
        statFields = stf;

        b.setFocusCycleRoot(true);
        ZTraversalPolicy stp = new ZTraversalPolicy(travOrder) {
            
            @Override
            public Component getComponentAfter(Container root, Component comp) {
                int idx = travOrder.indexOf(comp) + 1;
                while (idx < Stat.COUNT
                        && !((JTextField) travOrder.get(idx)).isEditable())
                    ++idx; // find next editable field

                if (idx >= Stat.COUNT)
                    return amount; // move to next GUI component from last field

                return travOrder.get(idx);
            }
        };
        b.setFocusTraversalPolicy(stp);

        return b;
    }

    /**
     * Creates and returns a GUI component for basic resource info. This panel
     * contains two rows; one row for name, galaxy, and lookup button, and one
     * row for space/recycled check-box and resource class.
     * 
     * @param travOrder a vector for the focus traversal order
     * @return a GUI component
     */
    private Component makeNorth(Vector<Component> travOrder) {
        Box box = Box.createVerticalBox();
        box.add(makeNResName(travOrder));
        box.add(Box.createVerticalStrut(5));
        box.add(makeNResType(travOrder));
        return box;
    }

    /**
     * Creates and returns the GUI component for resource identity. This panel
     * contains the row with lists for name and galaxy, and the lookup-button.
     * 
     * @param travOrder a vector for the focus traversal order
     * @return a GUI component
     */
    private Box makeNResName(Vector<Component> travOrder) {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Lookup at SWGCraft.org "));

        Dimension dn = new Dimension(200, 26);
        Dimension dg = new Dimension(100, 26);

        JComboWiderPopup<String> rnl = new JComboWiderPopup<String>();
        rnl.setEditable(true);
        rnl.setToolTipText("Enter a name for a resource, or its first letters");
        rnl.setAlignmentX(Component.LEFT_ALIGNMENT);
        rnl.setAutoscrolls(true);
        rnl.setPreferredSize(dn);
        rnl.setMaximumSize(dn);
        rnl.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    actionResourceNameSelected();
            }
        });
        travOrder.add(rnl.getEditor().getEditorComponent());
        resourceNameList = rnl;

        JLabel rl = new JLabel("Name", SwingConstants.LEFT);
        rl.setToolTipText("Enter a name for a resource, or its first letters");

        Box rnb = Box.createVerticalBox();
        rnb.add(rl);
        rnb.add(rnl);
        box.add(rnb);

        // all historical galaxies because of cross-server resources
        JComboBox<Object> gxys = new JComboBox<Object>(SWGCGalaxy.values());
        gxys.setPreferredSize(dg);
        gxys.setMaximumSize(dg);
        gxys.setAlignmentX(Component.LEFT_ALIGNMENT);
        gxys.setAutoscrolls(true);
        gxys.setToolTipText("Select a galaxy");
        travOrder.add(gxys);
        galaxyList = gxys; // no action listener, passive

        JLabel gl = new JLabel("Galaxy", SwingConstants.LEFT);
        gl.setToolTipText("Select a galaxy");

        Box gb = Box.createVerticalBox();
        gb.add(gl);
        gb.add(gxys);
        box.add(gb);

        box.add(Box.createHorizontalStrut(3));

        JButton lb = new JButton("Lookup");
        lb.setToolTipText("Lookup resource name at SWGCraft.org");
        lb.setMnemonic('L');
        lb.addActionListener(this);
        travOrder.add(lb);
        lookupButton = lb;

        JLabel lbl = new JLabel(" ");
        lbl.setToolTipText("Lookup resource name at SWGCraft.org");

        Box lbb = Box.createVerticalBox();
        lbb.add(lbl);
        lbb.add(lb);
        box.add(lbb);

        return box;
    }

    /**
     * Creates and returns a GUI component for resource type. This panel
     * contains a row with a space/recycled check-box and a list for resource
     * classes.
     * 
     * @param travOrder a vector for the focus traversal order
     * @return a GUI component
     */
    private Box makeNResType(Vector<Component> travOrder) {
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3));

        JCheckBox srcb = new JCheckBox("Space / Recycled");
        srcb.setHorizontalTextPosition(SwingConstants.LEFT);
        srcb.addActionListener(new ActionListener() {
            @Override
            
            public void actionPerformed(ActionEvent e) {
                actionRecycledSpaceToggled();
            }
        });
        srcb.setToolTipText("Create an entry for a space or recycled resource");
        spaceOrRecycledCB = srcb;
        Box srb = Box.createVerticalBox();
        srb.setToolTipText("Create an entry for a space or recycled resource");
        srb.setBorder(BorderFactory.createEtchedBorder());
        srb.add(srcb);
        box.add(srb);
        box.add(Box.createHorizontalStrut(5));

        makeResourceClasses();

        JComboBox<SWGResourceClass> rcnl = new JComboBox<SWGResourceClass>();
        rcnl.setPreferredSize(new Dimension(200, 26));
        rcnl.setAlignmentX(Component.LEFT_ALIGNMENT);
        rcnl.setEnabled(false);
        rcnl.setAutoscrolls(true);
        rcnl.addItemListener(new ItemListener() {
            
            public void itemStateChanged(ItemEvent e) {
                if (!isGuiUpdating && e.getStateChange() == ItemEvent.SELECTED) {
                    actionResourceClassSelected();
                }
            }
        });
        rcnl.setToolTipText(
                "If resource is unknown at SWGCraft.org, select a resource class");
        travOrder.add(rcnl);
        resourceClassNameList = rcnl;
        box.add(rcnl);

        return box;
    }

    /**
     * Helper method which creates two lists of resource classes. One list of
     * classes which can spawn in the worlds and one list of resource classes
     * for space and recycled classes. These are the two variables
     * {@link #rcSpawnable} and {@link #rcSpaceRecycled}.
     */
    private void makeResourceClasses() {
        if (rcSpawnable != null) return; // static so init once

        rcSpawnable = new Vector<SWGResourceClass>(
                SWGResourceClass.rcsFor(
                        new Comparable<SWGResourceClass>() {
                            public int compareTo(SWGResourceClass o) {
                                if (o.isSpawnable())
                                    return 0;
                                return -1;
                            }
                        }));
        rcSpaceRecycled = new Vector<SWGResourceClass>(
                SWGResourceClass.rcsFor(
                        new Comparable<SWGResourceClass>() {
                            public int compareTo(SWGResourceClass o) {
                                if (o.isSpaceOrRecycled())
                                    return 0;
                                return -1;
                            }
                        }));
        Comparator<SWGResourceClass> cmp = new Comparator<SWGResourceClass>() {
            @Override
            public int compare(SWGResourceClass o1, SWGResourceClass o2) {
                return o1.rcName().compareTo(o2.rcName());
            }
        };
        Collections.sort(rcSpawnable, cmp);
        Collections.sort(rcSpaceRecycled, cmp);
    }

    /**
     * Creates and returns the bottommost GUI component for this dialog.
     * 
     * @param travOrder a vector for the focus traversal order
     * @return the bottommost GUI component for the resource dialog dialog
     */
    private Component makeSouth(Vector<Component> travOrder) {
        Box bottom = Box.createHorizontalBox();
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3));

        JTextArea nf = new JTextArea(5, 15);
        nf.setAlignmentX(Component.LEFT_ALIGNMENT);
        nf.setLineWrap(true);
        nf.setWrapStyleWord(true);
        JScrollPane ns = new JScrollPane(nf);
        ns.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel ln = new JLabel("Misc notes", SwingConstants.LEFT);
        Box nb = Box.createVerticalBox();
        nb.add(ln);
        nb.add(ns);
        bottom.add(nb);
        bottom.add(Box.createHorizontalStrut(5));
        notesField = nf;

        Box ha = Box.createHorizontalBox();
        ha.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField amt = new JTextField();
        amt.setText("1");
        amt.setToolTipText("Number of units in stock");
        amt.setMaximumSize(new Dimension(100, 21));
        amt.setBorder(BorderFactory.createLoweredBevelBorder());
        amt.setHorizontalAlignment(SwingConstants.RIGHT);
        ((AbstractDocument) amt.getDocument()).setDocumentFilter(
                new SWGDocNumberFilter());
        travOrder.add(amt);
        JLabel al = makeTextFieldLabel(" Amount");
        al.setToolTipText("Number of units in stock");
        ha.add(amt);
        ha.add(al);
        amount = amt;

        JComboBox<String> cnbl = new JComboBox<String>();
        cnbl.setEditable(true);
        cnbl.setPreferredSize(new Dimension(124, 26));
        cnbl.setMaximumSize(new Dimension(124, 26));
        cnbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        cnbl.setAutoscrolls(true);
        cnbl.setToolTipText("Select or define an assignee (optional)");
        cnbl.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        // must add editor to travOrder since the box is editable
        travOrder.add(cnbl.getEditor().getEditorComponent());
        assigneeList = cnbl;

        Box btb = Box.createHorizontalBox();
        btb.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton okb = new JButton("OK");
        okb.setActionCommand("OK");
        okb.setToolTipText("Add this entry to the inventory");
        okb.setMnemonic('O');
        okb.setAlignmentX(Component.RIGHT_ALIGNMENT);
        okb.addActionListener(this);
        travOrder.add(okb);
        btb.add(okb);

        JButton clb = new JButton("Cancel");
        clb.setActionCommand("Cancel");
        clb.setMnemonic('C');
        clb.addActionListener(this);
        clb.setAlignmentX(Component.RIGHT_ALIGNMENT);
        travOrder.add(clb);
        btb.add(clb);

        Box ob = Box.createVerticalBox();
        ob.add(new JLabel("  "));
        ob.add(ha);
        ob.add(cnbl);
        ob.add(Box.createVerticalGlue());
        ob.add(btb);

        bottom.add(ob);
        return bottom;
    }

    /**
     * Creates and returns a GUI text field. The specified text is the tool tip
     * text.
     * 
     * @param txt the text for the tool tip
     * @return the GUI component
     */
    private JTextField makeTextField(String txt) {
        final JTextField fld = new JTextField();
        fld.setToolTipText(txt);
        fld.setBorder(BorderFactory.createLoweredBevelBorder());
        fld.setHorizontalAlignment(SwingConstants.RIGHT);
        ((AbstractDocument) fld.getDocument()).setDocumentFilter(
                new SWGDocNumberFilter(true, null, 0, 1000));
        fld.setEditable(false);
        return fld;
    }

    /**
     * Helper method which creates and returns a label with the specified text.
     * 
     * @param str the text for the label
     * @return a label
     */
    private JLabel makeTextFieldLabel(String str) {
        JLabel l = new JLabel(str);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /**
     * Helper method which returns an instance of a resource with the specified
     * name which origins from the galaxy selected at the galaxy chooser. This
     * method always returns an instance, if it is known at SWGCraft.org it also
     * has a valid ID. Only if there is an error a dialog is displayed and
     * {@code null} is returned.
     * 
     * @return an instance of a resource
     */
    private SWGKnownResource requestKnownResource() {
        try {
            SWGMutableResource mr =
                    SWGResourceManager.getMutable(getName(), getGalaxy());
            if (mr == null) {
                mr = new SWGMutableResource(
                        getResourceName(), getResourceClass());
                mr.galaxy(getGalaxy());
            }
            return SWGResourceManager.getInstance(mr);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(galaxyList,
                    "Communication error with SWGCraft.org\n" + e.getMessage(),
                    "Lookup failure", JOptionPane.INFORMATION_MESSAGE);
        }
        return null;
    }

    /**
     * Helper method which returns an instance of a resource with the specified
     * name which origins from the galaxy selected at the galaxy chooser. This
     * method returns the instance which is cached locally. If the requested
     * resource is not cached locally and if it is unknown at SWGCraft.org, or
     * if there is an error a dialog is displayed and {@code null} is returned.
     * 
     * @param resName the name for the resource
     * @return a resource, or {@code null}
     */
    private SWGKnownResource requestKnownResource(String resName) {
        SWGKnownResource res = null;
        String err = null;

        try {
            res = SWGResourceManager.getInstance(resName, getGalaxy());
        } catch (IOException e) {
            err = e.getMessage();
        }

        if (res == null) {
            JOptionPane.showMessageDialog(galaxyList, err == null
                    ? "No such resource at SWGCraft.org"
                    : "Communication error with SWGCraft.org\n" + err,
                    "Lookup failure", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return res;
    }

    /**
     * This method requests resource info from SWGCraft.org for the specified
     * resource name and the galaxy which is selected at the galaxy chooser. If
     * no resource is found or if there is a network error a dialog displays a
     * message and this method does nothing further. If a resource is obtained
     * this method handles two cases:
     * <dl>
     * <dt>Create inventory entry</dt>
     * <dd>Use the obtained resource info to populate the GUI dialog</dd>
     * <dt>Edit inventory entry</dt>
     * <dd>The user wants to lookup unknown resource stats at SWGCraft. <br/>
     * If the stats exist at SWGCraft.org <u>and</u> the values are different,
     * the GUI is updated but updating the wrapper is deferred until the users
     * selects OK; this is possible only for resources without SWGCraft ID.
     * Otherwise this method does nothing further.</dd>
     * </dl>
     * <p>
     * 
     * @param resName the proper name of a resource
     */
    private void requestResourceInfo(String resName) {
        if (wrap == null) { // creation mode
            SWGKnownResource res = requestKnownResource(resName);
            if (res == null)
                return;

            resourceClassNameList.setSelectedItem(res.rc());
            statFieldsContent(res);
            statFieldsEditable(res.rc(), true);

        } else { // edit mode
            SWGKnownResource kr = wrap.getResource();
            try {
                SWGMutableResource mr =
                        SWGResourceManager.getMutable(kr.getName(), getGalaxy());

                if (mr == null) {
                    JOptionPane.showMessageDialog(galaxyList, 
                            "Resource unknown at SWGCraft.org",
                            "Unknown", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                if (!mr.getName().equals(kr.getName())
                        || mr.rc() != kr.rc()) {
                    String msg = String.format("<html>"
                            + "Resource from SWGCraft.org is different<br/>"
                            + "Aborting<br/><TABLE>"
                            + "<TR><TD>Galaxy:</TD><TD>%s</TD></TR>"
                            + "<TR><TD>Name:</TD><TD>%s</TD></TR>"
                            + "<TR><TD>ID:</TD><TD>%d</TD></TR>"
                            + "<TR><TD>Class:</TD><TD>%s</TD></TR>" +
                            "</TABLE></html>",
                            getGalaxy().getName(), mr.getName(),
                            Long.toString(mr.id()), mr.rc().rcName());
                    JOptionPane.showMessageDialog(galaxyList, msg,
                            "Conflict", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!mr.stats().equals(kr.stats())) {
                    statFieldsContent(mr);
                    statFieldsEditable(mr.rc(), false);
                }
                lookupButton.setEnabled(false);
                SWGAide.frame().putToStatbar("Lookup finished");
                amount.requestFocusInWindow();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(galaxyList,
                        "Communication error with SWGCraft.org\n"
                                + e.getMessage(),
                        "Lookup failure", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Helper method which clears some common fields and GUI components.
     */
    private void reset() {
        resourceNameList.removeAllItems();
        galaxyList.setSelectedItem(galaxy);
        resourceClassNameList.setSelectedIndex(-1);
        resourceClassNameList.setEnabled(false);
        resetStatFields();
        statFieldsEditable(null, false);
    }

    /**
     * Helper method which clears all stat input fields.
     */
    private void resetStatFields() {
        for (JTextField tf : statFields)
            tf.setText(null);
    }

    /**
     * Helper method which updates the list of resource classes determined by
     * the specified argument; if {@code true} this method fills the list with
     * space-and-recycled classes, otherwise with classes that spawn in the
     * worlds.
     * 
     * @param spaceOrRecycled {@code true} for space or recycled classes
     */
    private void resourceClassUpdate(boolean spaceOrRecycled) {
        isGuiUpdating = true;
        resourceClassNameList.removeAllItems();
        if (spaceOrRecycled)
            for (SWGResourceClass rc : rcSpaceRecycled)
                resourceClassNameList.addItem(rc);
        else
            for (SWGResourceClass rc : rcSpawnable)
                resourceClassNameList.addItem(rc);
        isGuiUpdating = false;
    }

    /**
     * Helper method which saves the dialog's content to the inventory for the
     * selected assignee at the current galaxy. This method is called from the
     * action handler for the OK-button. However, first the content is initially
     * validated by {@code isNameAndClassOK()}.
     * 
     * @return {@code false} if there is an error saving the content
     */
    private boolean save() {

        // Two main modes:
        // A: Create new wrapper, then this.wrapper is null, however, resource
        // .. is already looked up and thus it is in the local cache
        // .. only in creating mode the wrapper is saved
        // B: Edit this.wrapper, which has two sub-modes and in both cases
        // .. amount and notes are possible to edit
        // .. 1) its resource has a valid SWGCraft ID
        // .. 2) its resource has no ID, galaxy and stats are possible to edit
        // .. The existing wrapper is updated

        SWGResourceClass rc = lookupResource != null
                ? lookupResource.rc()
                : getResourceClass();
        SWGResourceStats stats = lookupResource != null
                ? lookupResource.stats()
                : new SWGResourceStats(getStatFieldValues());
        String assignee = (String) assigneeList.getSelectedItem();

        if (wrap == null && rc.isSpaceOrRecycled()) {
            SWGKnownResource kr = rc.spaceOrRecycled();
            wrap = new SWGInventoryWrapper(kr, assignee);
        } else if (wrap == null) {
            SWGKnownResource kr = lookupResource != null
                    ? lookupResource
                    : requestKnownResource();
            if (kr == null)
                return false;

            if (kr.rc() != rc) {
                String msg = String.format("<html>"
                        + "Cached resource has different resource class<br/>"
                        + "<TABLE>"
                        + "<TR><TD>Galaxy:</TD><TD>%s</TD></TR>"
                        + "<TR><TD>Name:</TD><TD>%s</TD></TR>"
                        + "<TR><TD>ID:</TD><TD>%s</TD></TR>"
                        + "<TR><TD>Class:</TD><TD>%s</TD></TR>"
                        + "</TABLE></html>",
                        getGalaxy().getName(), kr.getName(),
                        Long.toString(kr.id()), kr.rc().rcName());
                JOptionPane.showMessageDialog(amount, msg,
                        "Resource class error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (kr.stats().sum() > 0 && stats.sum() > 0
                    && !kr.stats().equals(stats)) {
                String msg = String.format("<html>"
                        + "Cached resource has different stats<BR/><TABLE>"
                        + "<TR><TD>Galaxy:</TD><TD>%s</TD></TR>"
                        + "<TR><TD>Name:</TD><TD>%s</TD></TR>"
                        + "<TR><TD>ID:</TD><TD>%s</TD></TR>"
                        + "<TR><TD>Stats:</TD><TD>%s</TD></TR>"
                        + "</TABLE></html>",
                        getGalaxy().getName(), kr.getName(),
                        Long.toString(kr.id()),
                        kr.stats().toString(true));
                JOptionPane.showMessageDialog(amount, msg,
                        "Resource stats error", JOptionPane.ERROR_MESSAGE);
                return false;
            } else if (kr.stats().sum() <= 0 && stats.sum() > 0) {
                SWGMutableResource mr = new SWGMutableResource(kr);
                mr.stats(stats, false); // already validated
                SWGResourceManager.updateInstance(kr, mr);
            }

            wrap = new SWGInventoryWrapper(kr, assignee);

        } else {
            if (!assignee.equals(wrap.getAssignee())) {
                // User can edit owner of the inventory entry, done here
                SWGResController.inventoryRemove(wrap, galaxy);
                wrap.setAssignee(assignee);
                wrap.equalAddSub = "+";
            }

            SWGKnownResource kr = wrap.getResource();
            if (!kr.rc().isSpaceOrRecycled()) {
                SWGCGalaxy gxy = getGalaxy();
                 if (kr.id() <= 0 && gxy != null && kr.galaxy().equals(gxy) == false) {
                    SWGMutableResource mr = new SWGMutableResource(kr);
                    mr.galaxy(gxy);
                    SWGResourceManager.updateInstance(kr, mr);
                }

                if (kr.id() <= 0 && !kr.stats().equals(stats)) {
                    SWGMutableResource mr = new SWGMutableResource(kr);
                    mr.stats(stats, false); // already validated
                    SWGResourceManager.updateInstance(kr, mr);
                }
            }
        }

        wrap.setAmount(ZNumber.longVal(amount.getText()));
        wrap.setNotes(notesField.getText());

        // always safe, also if exists
        SWGResController.inventoryAdd(wrap, galaxy);
        wrap.equalAddSub = null;

        ((SWGInventoryTab)parent).updateDisplay();
        SWGResourceTab.currentUpdateGUI();
        return true;
    }

    /**
     * Helper method which initiates the GUI with the content of the current
     * wrapper.
     */
    private void setupEditWrapper() {
        resourceNameList.addItem(wrap.getResource().getName());
        resourceClassNameList.setSelectedItem(wrap.getResource().rc());
        galaxyList.setSelectedItem(wrap.getResource().galaxy());
        statFieldsContent(wrap.getResource());

        amount.setText(ZNumber.asText(wrap.getAmount(), true, false));
        assigneeList.setSelectedItem(wrap.getAssignee());
        notesField.setText(wrap.getNotes());
    }

    /**
     * Helper method which sets the initial state of this dialog. In particular,
     * determined by the argument this method enables or disables these
     * elements: 1) the name selection list, 2) the galaxy selection list, and
     * 3) the lookup button. Elements for amount, notes, and assignees are
     * always editable. This method also sets the title for this dialog.
     * <p>
     * If the wrapper is {@code null} this is a creation session and the fields
     * are enabled. Otherwise this is an edit session and if the resource has a
     * valid SWGCraft ID, or if it is a space-or-recycled class, the mentioned
     * input fields are disabled. Otherwise, the galaxy selection list and the
     * stat input fields are enabled; this is a locally known resource, unknown
     * at SWGCraft.org.
     * 
     * @param wrapper an inventory wrapper, or {@code null}
     */
    private void setupInputFields(SWGInventoryWrapper wrapper) {
        boolean bEdit = wrapper != null; // edit versus create
        boolean rcOspace = wrapper != null
                && wrapper.getResource().rc().isSpaceOrRecycled();
        boolean bKnown = wrapper != null
                && (rcOspace || wrapper.getResource().id() > 0);

        resourceNameList.setEnabled(!bEdit);
        galaxyList.setEnabled(!bKnown);
        lookupButton.setEnabled(!rcOspace);

        spaceOrRecycledCB.setEnabled(!bEdit);
        resourceClassUpdate(rcOspace);

        statFieldsEditable(bEdit
                ? wrap.getResource().rc()
                : null,
                !bKnown);

        setTitle(bKnown && !rcOspace
                ? String.format("%s - [SWGCraft id %s]",
                        title, Long.toString(wrap.getResource().id()))
                : title);
    }

    /**
     * Helper method which opens this dialog. This method clears the GUI and
     * internal fields and. If {@code wrapper != null} its content is used to
     * populate this dialog and some components are disabled.
     * 
     * @param wrapper the wrapper to edit, or {@code null} to create a wrapper
     */
    void showAndBegin(SWGInventoryWrapper wrapper) {
        galaxy = SWGResourceTab.galaxy();
        wrap = wrapper;

        setupInputFields(wrap);

        for (String s : ((SWGInventoryTab)parent).assigneesAndCharacters())
            assigneeList.addItem(s);

        if (wrap != null) {
            setupEditWrapper();
            amount.requestFocusInWindow();
        } else {
            galaxyList.setSelectedItem(galaxy);
            assigneeList.setSelectedItem(((SWGInventoryTab)parent).assignee());
            resourceNameList.requestFocusInWindow();
        }

        setVisible(true);
    }

    /**
     * Helper method which sets the the stats of {@code resource} to the stat
     * fields and nullifies non-existent stats.
     * 
     * @param resource the resource with the stats to set
     */
    private void statFieldsContent(SWGResource resource) {
        SWGResourceStats st = resource.stats();
        for (Stat s : Stat.values())
            statFields[s.i].setText(
                    ZNumber.asText(st.value(s), false, false));
    }

    /**
     * Helper method which sets the stat fields editable or not determined by
     * the arguments. If the boolean argument is {@code true}, for each stat of
     * the resource class, if it is expected the field is set editable,
     * otherwise not-editable. If the specified resource class is {@code null}
     * all fields are set to non-editable.
     * 
     * @param resClass the resource class which determines which stats are
     *        editable, or {@code null}
     * @param editable {@code true} if the appropriate fields are editable
     */
    private void statFieldsEditable(SWGResourceClass resClass, boolean editable) {
        for (Stat s : Stat.values())
            statFields[s.i].setEditable(resClass == null
                    ? false
                    : editable && resClass.has(s));
    }

    /**
     * Helper method which sets and disables the stat fields for the recycled
     * resource class.
     * 
     * @param rc the recycled resource class
     */
    private void statFieldsRecycledOrSpace(SWGResourceClass rc) {
        statFieldsEditable(null, false);
        for (Stat s : Stat.values())
            statFields[s.i].setText(
                    ZNumber.asText(rc.min(s), false, false));
    }
}
