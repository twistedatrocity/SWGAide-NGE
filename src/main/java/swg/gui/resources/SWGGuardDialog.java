package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import swg.crafting.SWGValues;
import swg.crafting.SWGWeights;
import swg.crafting.Stat;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceFilter;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDocNumberFilter;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJDialog;
import swg.model.SWGCGalaxy;
import swg.tools.ZNumber;
import swg.tools.ZTraversalPolicy;

/**
 * A dialog for creating resource guards, setting their values and adding some
 * notes. Guards are added to galaxy-specific lists contained by an instance of
 * {@link SWGResController}
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public final class SWGGuardDialog extends SWGJDialog implements ActionListener {

    /**
     * A GUI check box for the alarm.
     */
    private JCheckBox alarm;

    /**
     * A GUI check box for considering missing stats.
     */
    private JCheckBox allowZero;

    /**
     * The cancel button.
     */
    private JButton cancelButton;

    /**
     * The guard which is currently being created/edited.
     */
    private SWGGuard currentGuard;

    /**
     * An array of text input field for filter stats. The elements are ordered
     * by the model in SWGAide, however, at the GUI they are are ordered as
     * in.game.
     */
    private JTextField[] fields = new JTextField[Stat.COUNT];

    /**
     * A galaxy constant for the current event.
     */
    private SWGCGalaxy galaxy;

    /**
     * A text field for the name of the guard.
     */
    private JTextField guardName;

    /**
     * A local flag used while cleaning the GUI.
     */
    private boolean isCleaning;

    /**
     * A text input area for notes.
     */
    private JTextArea notesField;

    /**
     * The OK button.
     */
    private JButton okButton;

    /**
     * The resource type currently being edited.
     */
    private SWGResourceClass resourceClass;

    /**
     * A GUI combo box for choosing resource class names.
     */
    private JComboBox<String> resourceClassNameList;

    /**
     * The input text input field for the guard's threshold, if the instance is
     * a weighed guard.
     */
    private JTextField threshold;

    /**
     * Creates a dialog for creating/editing resource guards.
     * 
     * @param frame the frame of SWGAide
     */
    SWGGuardDialog(SWGFrame frame) {
        super("Resource Guard");

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 7, 7, 7));

        Vector<Component> travOrder = new Vector<Component>(20);

        contentPane.add(makeTopRow(travOrder), BorderLayout.PAGE_START);
        contentPane.add(makeValuesRow(travOrder), BorderLayout.CENTER);
        contentPane.add(makeBottomRow(travOrder), BorderLayout.PAGE_END);

        guardName.requestFocusInWindow();

        this.setContentPane(contentPane);
        this.pack();
        this.setMinimumSize(this.getSize());

        Point p = frame.getLocation();
        p.translate(100, 100);
        p = (Point) SWGFrame.getPrefsKeeper().get(
                "resourceGuardDialogLocation", p);
        p = SWGGuiUtils.ensureOnScreen(p, this.getSize());
        this.setLocation(p);

        ZTraversalPolicy to = new ZTraversalPolicy(travOrder);
        this.setFocusTraversalPolicy(to);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == okButton) {
            if (isDialogOK())
                saveGuard();
            else
                return;
        }
        close();
    }

    /**
     * Cleans all text input fields.
     */
    private void cleanFields() {
        isCleaning = true;
        if (resourceClass == null && currentGuard == null) {
            String resCls = (String) resourceClassNameList.getSelectedItem();
            resourceClass = SWGResController.resourceClassFrom(resCls);
        }
        threshold.setText(null);
        threshold.setEditable(false);
        for (JTextField f : fields)
            f.setText(null);

        resourceClassNameList.setSelectedIndex(-1);
        isCleaning = false;
    }

    @Override
    protected void close() {
        SWGFrame.getPrefsKeeper().add(
                "resourceGuardDialogLocation", getLocation());
        setVisible(false); // OK-button leads here but no invisible
    }

    /**
     * Initiates the GUI dialog to create a new guard.
     */
    private void createNewGuardInit() {
        cleanFields();
        alarm.setSelected(true);
        allowZero.setSelected(true);
        guardName.setText(null);
        notesField.setText(null);
        if (resourceClass != null)
            resourceClassNameList.setSelectedItem(resourceClass.rcName());
    }

    /**
     * Initiates the GUI dialog with the currently selected guard
     */
    private void editGuardInit() {
        guardName.setText(currentGuard.getName());
        resourceClassNameList.setSelectedItem(currentGuard.rc().rcName());

        for (Stat s : Stat.values())
            fields[s.i].setText(Integer.toString(currentGuard.logic().value(s)));

        threshold.setText(ZNumber.asText(
                currentGuard.threshold(), true, false));

        notesField.setText(currentGuard.notes());
        alarm.setSelected(currentGuard.useAlarm);
        allowZero.setSelected(currentGuard.acceptNoStats);
    }

    /**
     * Returns an array of integers denoting the values from the filter input
     * fields, that is a filter.
     * 
     * @return a resource filter
     */
    private int[] getFilterFieldValues() {
        int[] flt = new int[Stat.COUNT];
        for (int i = 0; i < Stat.COUNT; ++i)
            flt[i] = ZNumber.intVal(fields[i].getText());

        return flt;
    }

    /**
     * Returns a copy of type {@link SWGValues} which is either a plain filter
     * or a weighed filter, its kind is determined by the values parsed in the
     * text input fields.
     * 
     * @return a plain filter or a weighed filter
     */
    private SWGValues getGuardValues() {
        int[] values = getFilterFieldValues();
        int sum = SWGValues.sum(values);

        if (SWGWeights.isValid(sum))
            return new SWGWeights(values);

        return new SWGResourceFilter(values);
    }

    /**
     * Helper method which listens to the filter input text fields and toggles
     * the threshold/limit field enabled/disabled determined by if the values
     * form a valid weighed filter.
     */
    private void handleThreshold() {
        int sum = SWGValues.sum(getFilterFieldValues());
        threshold.setEditable(SWGWeights.isValid(sum));
    }

    /**
     * Determines if the input at this dialog is valid. More formally, this
     * method returns {@code true} if there is a selected resource class, a
     * name, and at least one value for a stat. Otherwise a GUI dialog informs
     * the user and {@code false} is returned.
     * 
     * @return {@code true} if the content for the dialog is valid, {@code
     *         false} otherwise
     */
    private boolean isDialogOK() {
        String msg = null;
        String ttl = null;
        if (resourceClass == null) {
            msg = "Select a resource class";
            ttl = "No resource class";
        } else if (guardName.getText() == null
            || guardName.getText().isEmpty()) {
            msg = "Enter a name for the guard";
            ttl = "No name";
        } else if (SWGValues.sum(getFilterFieldValues()) <= 0) {
            msg = "Enter values for at least one stat";
            ttl = "No values";
        }
        if (msg != null) {
            JOptionPane.showMessageDialog(alarm, msg, ttl,
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Creates and returns the bottommost GUI component for the resource guard
     * dialog.
     * 
     * @param travOrder
     *            a vector for the focus traversal order
     * @return the bottommost GUI component
     */
    private Component makeBottomRow(Vector<Component> travOrder) {
        Box bottom = Box.createHorizontalBox();

        notesField = new JTextArea(4, 15);
        notesField.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesField.setLineWrap(true);
        notesField.setWrapStyleWord(true);
        JScrollPane ns =
            new JScrollPane(notesField,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ns.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ln = new JLabel("Misc notes", SwingConstants.LEFT);
        Box nb = Box.createVerticalBox();
        nb.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        nb.add(ln);
        nb.add(ns);
        bottom.add(nb);

        Box vb = Box.createHorizontalBox();
        vb.setAlignmentX(Component.LEFT_ALIGNMENT);
        threshold =
            makeFieldTextField("The threshold for weighed guard in range [0 1000]");
        threshold.setMaximumSize(new Dimension(40, 21));
        vb.add(threshold);
        JLabel tl = makeFieldLabels(" Threshold");
        tl.setToolTipText("The threshold for weighed guard in range [0 1000]");
        travOrder.add(threshold);
        vb.add(tl);

        allowZero = new JCheckBox("Allow zeroes");
        allowZero.setToolTipText("Allow for missing stats in weighing");
        allowZero.setAlignmentX(Component.LEFT_ALIGNMENT);
        travOrder.add(allowZero);

        alarm = new JCheckBox("Alarm");
        alarm.setToolTipText("Alarm when a resource meets this guard");
        alarm.setAlignmentX(Component.LEFT_ALIGNMENT);
        travOrder.add(alarm);

        Box btb = Box.createHorizontalBox();
        okButton = new JButton("OK");
        okButton.setToolTipText("Add this guard to the list of guards");
        okButton.setMnemonic('O');
        okButton.addActionListener(this);
        travOrder.add(okButton);
        btb.add(okButton);

        btb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('C');
        cancelButton.addActionListener(this);
        travOrder.add(cancelButton);
        btb.add(cancelButton);

        Box ob = Box.createVerticalBox();
        ob.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        ob.add(new JLabel("     "));
        ob.add(vb);
        ob.add(allowZero);
        ob.add(alarm);
        ob.add(btb);

        bottom.add(ob);
        return bottom;
    }

    /**
     * Creates and returns a label with the specified text.
     * 
     * @param text
     *            the text for the label
     * @return a label
     */
    private JLabel makeFieldLabels(String text) {
        JLabel l = new JLabel(text);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /**
     * Creates and returns a GUI text input field. The returned component has
     * the supplied text as tool tip text and a lowered bevel border. Two
     * document listeners are attached, one for valid input in the range [0
     * 1000], and {@link #handleThreshold()} which enables/disables the
     * threshold input field.
     * 
     * @param txt the text for the tool tip
     * @return a GUI component
     */
    
    private JTextField makeFieldTextField(String txt) {
        final JTextField fld = new JTextField();
        fld.setToolTipText(txt);
        fld.setBorder(BorderFactory.createLoweredBevelBorder());
        fld.setHorizontalAlignment(SwingConstants.RIGHT);
        ((AbstractDocument) fld.getDocument()).setDocumentFilter(
                new SWGDocNumberFilter(true, null, 0, 1000) {
                    @Override
                    protected boolean isValid(String s) {
                        return isCleaning || super.isValid(s);
                    }
                });
        fld.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                handleThreshold();
            }

            public void insertUpdate(DocumentEvent e) {
                handleThreshold();
            }

            public void removeUpdate(DocumentEvent e) {
                handleThreshold();
            }
        });
        return fld;
    }

    /**
     * Creates and returns the GUI component for the uppermost row.
     * 
     * @param travOrder
     *            a vector for the focus traversal order
     * @return the topmost GUI component
     */
    private Component makeTopRow(Vector<Component> travOrder) {
        Box top = Box.createHorizontalBox();

        guardName = new JTextField("", 15);
        guardName.setToolTipText("Define a name or description for the guard");
        Box tn = Box.createVerticalBox();
        JLabel ln = new JLabel("Name / description");
        ln.setToolTipText("Define a name or description for the guard");
        tn.add(ln);
        tn.add(guardName);
        travOrder.add(guardName);
        top.add(tn);

        List<String> nameL = SWGResourceClass.rcNames();
        Vector<String> nameV = new Vector<String>(nameL);
        resourceClassNameList = new JComboBox<String>(nameV);
        resourceClassNameList.setPreferredSize(new Dimension(230, 25));
        resourceClassNameList.setAlignmentX(Component.LEFT_ALIGNMENT);
        resourceClassNameList.setAutoscrolls(true);
        resourceClassNameList.setToolTipText(
                "Select a resource class for the guard");
        
        resourceClassNameList.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                resourceClassNameSelected();
            }
        });
        travOrder.add(resourceClassNameList);

        Box tc = Box.createVerticalBox();
        JLabel lc = new JLabel("Resource class", SwingConstants.LEFT);
        lc.setToolTipText("Select a resource class for the guard");
        tc.add(lc);
        tc.add(resourceClassNameList);
        top.add(tc);

        return top;
    }

    /**
     * Creates and returns the GUI component for the guard values. This is the
     * row with a text field each stat.
     * 
     * @param travOrder a vector for the focus traversal order
     * @return the GUI component for stats
     */
    private Component makeValuesRow(Vector<Component> travOrder) {
        JPanel b = new JPanel(new GridLayout(2, 11));

        for (Stat s : Stat.gameOrder())
            b.add(makeFieldLabels(s.name()));

        for (Stat s : Stat.gameOrder()) {
            JTextField tf = makeFieldTextField(
                    String.format("%s - %s", s.name(), s.getDescription()));
            fields[s.i] = tf;
            travOrder.add(tf);
            b.add(tf);
        }

        return b;
    }

    /**
     * Called when the user selects a resource class name at the GUI.
     */
    private void resourceClassNameSelected() {
        if (isCleaning)
            return;

        String rcn = (String) resourceClassNameList.getSelectedItem();
        resourceClass = SWGResourceClass.rc(rcn);
    }

    /**
     * Helper method to the action listener for the "OK" button if it is
     * determined that the dialog's content is valid. This method creates a
     * guard and saves it to the list of guards for the current galaxy.
     */
    private void saveGuard() {
        int tres = 0;
        SWGValues v = getGuardValues();
        if (SWGWeights.class.isAssignableFrom(v.getClass()))
            tres = ZNumber.intVal(threshold.getText()); // only weighed

        SWGGuard g = new SWGGuard(guardName.getText(), resourceClass,
                v, tres, alarm.isSelected(), allowZero.isSelected());

        g.notes(notesField.getText());

        if (currentGuard != null)
            SWGResController.guardsRemove(currentGuard, galaxy); // replace
        else if (SWGResController.guardExist(resourceClass, v, galaxy)) {
            JOptionPane.showMessageDialog(this, "An identical guard exists.\n"
                    + "Abort", "Duplicate guard", JOptionPane.WARNING_MESSAGE);
        }

        SWGResController.guardsAdd(g, galaxy, true);
    }

    /**
     * Opens and displays the dialog which handles creation and editing of
     * resource guards. The arguments determine which kind of operation to
     * perform. In particular, if {@code guard} is {@code null} the operation is
     * to create a new guard.
     * 
     * @param resClass the resource class to create a guard for, or {@code null}
     *        for editing a guard or if resource class is yet undetermined
     * @param guard the guard to edit, or {@code null} if this operation is to
     *        create a new guard
     * @param gxy a galaxy constant
     */
    public void showAndBegin(
            SWGResourceClass resClass, SWGGuard guard, SWGCGalaxy gxy) {
        if (gxy == null) throw new NullPointerException("Galaxy is null");
        resourceClass = resClass;
        currentGuard = guard;
        galaxy = gxy;

        if (guard == null)
            createNewGuardInit();
        else
            editGuardInit();
        setVisible(true);
    }
}
