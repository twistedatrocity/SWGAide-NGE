package swg.gui.resources;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import swg.SWGAide;
import swg.crafting.Stat;
import swg.crafting.resources.ResourceUpdate;
import swg.crafting.resources.ResourceUpdate.UpdateType;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGMutableResource;
import swg.crafting.resources.SWGPlanetAvailabilityInfo;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.SWGResourceStats;
import swg.crafting.resources.types.SWGCreatureResources;
import swg.crafting.resources.types.SWGMustafarianFiberplast;
import swg.crafting.resources.types.SWGOrganic;
import swg.gui.SWGFrame;
import swg.gui.common.SWGDocFilter;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGHelp;
import swg.gui.resources.SWGSimilarNameDialog.Wrapper;
import swg.model.SWGCGalaxy;
import swg.model.SWGNotes;
import swg.model.SWGPlanet;
import swg.model.SWGStation;
import swg.swgcraft.SWGCraft;
import swg.swgcraft.SWGResourceManager;
import swg.swgcraft.SWGSoapAvailResResponse;
import swg.swgcraft.SWGSoapEditResResponse;
import swg.swgcraft.SWGSoapNOResResponse;
import swg.swgcraft.SWGSoapStatusResponse;
import swg.tools.SpringUtilities;
import swg.tools.ZReader;
import swg.tools.ZString;
import swg.tools.ZStuff;

/**
 * The GUI component for manually submitting resources to SWGCraft.org. This
 * type also includes the necessary logic and control. This GUI element supports
 * submitting a single resource which is typed into the GUI fields, and a parsed
 * in-game notes file for multiple resources.
 * <P>
 * Technical info:<BR/>
 * When this component handles resources parsed from notes file the objects are
 * instances of {@link SWGMutableResource} and {@link String}, see {code
 * multipleResources}. This component does not really handle known resources
 * other than to compare the state between the parsed object and the known
 * object and submit a possible difference to SWGCraft.org.
 * <P>
 * Known resource are not "edited" via this component but they can be amended
 * to. For example, a known resource without stats is updated if the user adds
 * stats to it via the notes file and submits the data to SWGCraft.org. The same
 * holds for adding a planet or several to a known resource.<BR/>
 * XXX: Add support to edit stats. Suggested: add a tag "edit" so the syntax
 * will be "edit,name,stats...". Using this flag would allow the user to
 * override current stats and send an edit-resource message to SWGCraft.org.
 * <P>
 * This component does not add or subtract from the local cache of resources,
 * nor any other GUI component, but let the resource manager handle that job.
 * After any submission is completed this component rather triggers an update
 * notification so clients which subscribe for those update themselves.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGSubmitTab extends JPanel {

    // XXX: add so use can specify own filter-combinations

    /**
     * The GUI component to filter for minimum age for resources written to the
     * notes file.
     */
    private JSpinner ageSpinner;

    /**
     * The button for clearing the GUI and resetting internal states.
     */
    private JButton clearButton;

    /**
     * The label onto which error messages are printed.
     */
    private JLabel errorLabel;

    /**
     *The label to display the current galaxy at.
     */
    private JLabel galaxyDisplay;

    /**
     * The URL for the help page for this view.
     */
    private final URL helpPage;

    /**
     * A check-box for submitting historical resources. This item is not
     * selected by default. When the user decides to submit resources, if this
     * item is selected, he is asked to confirm.
     */
    private JCheckBox historical;

    /**
     * A flag which is {@code false} until creation of the GUI is ready. Once
     * the GUI is created this flag is set to {@code true}. The intention of
     * this flag is to prevent unsafe action events.
     */
    private boolean isGUIcreated = false;

    /**
     * A helper flag which indicates if there is a background job running in
     * this component, initiated by the event thread.
     */
    private boolean isWorking = false;
    
    /**
     * A list of the filters that were used for the notes file for the current
     * submission.
     */
    private final List<String> multiFiltered;

    /**
     * The GUI component for multiple resources.
     */
    private JList<Object> multipleGUIList;

    /**
     * The model for the multiple resources GUI component.
     */
    private MultiResourceModel multipleListModel;

    /**
     * A list of the content parsed from the notes file, or {@code null}. This
     * list contains instances of {@link SWGMutableResource} ready to submit,
     * and/or {@link String}s for depleted resources or faulty entries which
     * must be rectified or they will not be submitted.
     * <P>
     * The resource objects are either new resources or duplicates of locally
     * cached, known resources. This component does not handle known resources
     * other than to compare the state between the parsed object and the known
     * object and submit a possible difference to SWGCraft.org.
     */
    private List<Object> multipleResources;

    /**
     * The button for submitting multiple resources from notes file.
     */
    private JButton multiSubmitButton;

    /**
     * The button for loading resources from notes file.
     */
    private JButton noteFileLoadButton;

    /**
     * The name of the notes file to use, currently fixed to "rem.txt".
     */
    private final String notesFileName = "rem.txt";

    /**
     * A list of resource classes which are blacklisted by the user. These
     * classes, or sub-classes of them, are not written to the notes file.
     */
    private List<SWGResourceClass> rcBlackList;

    /**
     * The recently visited galaxy, or {@code null}.
     */
    private SWGCGalaxy recentGalaxy;

    /**
     * The resource class for the data entered at the input fields.
     */
    private SWGResourceClass resourceClass;

    /**
     * The list to filter for resource classes written to the notes file.
     */
    private JComboBox<String> resourceClassFilter;

    /**
     * The GUI component which contains this object, the parent of this
     * instance.
     */
    private final SWGResourceTab resourceTab;

    /**
     * The label to display correct planets.
     */
    private JLabel singlePlanetDisplay;

    /**
     * The text input field for planet names.
     */
    private JTextField singlePlanetInput;

    /**
     * The display for correct resource class.
     */
    private JLabel singleResourceClassDisplay;

    /**
     * The text input field for the resource class.
     */
    private JTextField singleResourceClassInput;

    /**
     * The display for the name of the resource.
     */
    private JLabel singleResourceNameDisplay;

    /**
     * The text input field for the resource name.
     */
    private JTextField singleResourceNameInput;

    /**
     * The display for resource stats.
     */
    private JLabel singleResourceStatsDisplay;

    /**
     * The text input field for resource stats.
     */
    private JTextField singleResourceStatsTextField;

    /**
     * A mixed button. Its base mode is to submit a single resource. However,
     * its secondary mode is to edit resource selected at the list of multiple
     * resources. Its visible label is toggled accordingly.
     */
    private JButton singleSubmitButton;

    /**
     * Creates an instance of the GUI component. Its content is created lazily,
     * on demand.
     * 
     * @param resourceTab the GUI component which contains this object
     */
    SWGSubmitTab(final SWGResourceTab resourceTab) {
        this.resourceTab = resourceTab;

        multiFiltered = new ArrayList<String>();

        helpPage = SWGAide.class.getResource(
                "docs/help_resources_submit_manually_en.html");

        resourceTab.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                focusGained(true);
            }
        });
    }

    /**
     * Called when the user selects to clear the displayed content at the GUI.
     * This method clears the GUI and resets internal fields.
     */
    private void actionClearAndReset() {
        resourceClass = null;
        multipleResources = null;

        String recentSelected = (String) SWGFrame.getPrefsKeeper().get(
                "resourceSubmitSelectedClass");
        if (recentSelected != null)
            resourceClassFilter.setSelectedItem(recentSelected);

        multiFiltered.clear();

        resetSingleSubmitButton();
        resetSingleTextFields();
        errorLabel.setText("  ");

        multipleGUIList.clearSelection();

        updateMultiList();
        enableUserActions(SWGFrame.getSelectedCharacter().galaxy().exists());
    }

    /**
     * Called when the user selects to delete an entry at the main table of
     * entries. This method does not affect the notes file.
     * 
     * @param selectedIndex the index of the selected entry
     */
    private void actionDelete(int selectedIndex) {
        if (multipleResources != null
                && selectedIndex < multipleResources.size())
            multipleResources.remove(selectedIndex);
        updateMultiList();
    }

    /**
     * Called when the user selects to load the in-game notes file. This method
     * parses the notes file named {code notesFileName} and adds the result to
     * {code multipleResources}.
     */
    private void actionLoadNotes() {
        enableUserActions(false);

        // set focus, otherwise it gives focus to "clear" >> scary/not wanted
        multiSubmitButton.requestFocusInWindow();

        resetSingleTextFields();
        multipleGUIList.clearSelection();
        multipleResources = loadNotesFile();
        updateMultiList();
        enableUserActions(true);
    }

    /**
     * Called when the users selects to submit multiple resources to
     * SWGCraft.org. This method performs the following actions:
     * <OL>
     * <LI>Submits resources that the user have marked as depleted in the notes
     * file.</LI>
     * <LI>Submits resources which are derived as depleted by the resources
     * which are ready to submit as new.</LI>
     * <LI>Submits resources which are new or amended to.</LI>
     * </OL>
     * The communication with SWGCraft.org executes on a worker thread.
     */
    
    private void actionSubmitMulti() {

        if (multipleResources == null || multipleResources.size() <= 0) {
            errorLabel.setText("Nothing to submit");
            return;
        }
        
        /*
         * piggy-back on this submit-method also for historical resources, it is
         * quite cheap to let common code execute as usual
         */
        final boolean old = historical.isSelected();
        if (old && JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                multiSubmitButton, "Submit as old / historical?", "Confirm",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
            return ;


        // set focus, otherwise visible focus to res class list >>> confusing
        multiSubmitButton.requestFocusInWindow();

        submitBefore();

        // snapshot of fields in the case the user switches
        // galaxy while working

        final SWGCGalaxy gxy = recentGalaxy;

        // this list is a mix of strings and mutable-resources
        // * strings for depleted or errors
        // * mutable for both known-resources and new-not-yet-submitted
        final List<Object> mult = multipleResources;

        // a set of *all* resources, to be able to find known if-any
        final SWGResourceSet set = SWGResourceManager.getSet(gxy);

        final ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(new Runnable() {

            public void run() {
                String error = "";
                List<SWGSimilarNameDialog.Wrapper> sim =
                        new ArrayList<SWGSimilarNameDialog.Wrapper>();
                try {
                    error = submitMulti(gxy, mult, old, set, sim);
                } catch (Exception e) {
                    SWGAide.printError("SWGSubmitTab:actionSubmitMulti:b", e);
                    error = e.getMessage();
                }

                submitDone(gxy, error, sim);

                exec.shutdown();
            }
        });
    }

    /**
     * Called when the user clicks {code singleSubmitButton}. This method
     * dispatches to {code singleSubmit()} or {code editApply()} determined by
     * the current state of this component.
     */
    private void actionSubmitSingle() {
        if (singleSubmitButton.getText().equals("Submit"))
            submitSingle();
        else
            editApply(); // equals "Apply"
    }

    /**
     * Called when the user clicks the mouse at an entry at the main table for
     * multiple resources. If it is a left-hand mouse click this method adds the
     * selected line to the "Single resources / Edit" panel sets this component
     * to "Apply" state. If its is a right-hand mouse click a GUI popup dialog
     * is displayed.
     * 
     * @param e the event that triggered this action
     */
    private void actionTableClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && multipleResources != null) {
            Object o = multipleGUIList.getSelectedValue();
            if (o == null) return;
            if (SWGMutableResource.class.isAssignableFrom(o.getClass()))
                editPrepareResource((SWGMutableResource) o);
            else {
                String str = (String) o;
                if (str.toLowerCase(Locale.ENGLISH).startsWith("depl"))
                    return;
                editPrepareString(str);
            }
            singleSubmitButton.setText("Apply");
            singleSubmitButton
                    .setToolTipText("Apply edits to the selected resource");
        } else if (e.getButton() == MouseEvent.BUTTON3)
            multipleTablePopup(e);
    }

    /**
     * Called when the user selects to write the current resources to the notes
     * file {code notesFileName}. The resources are filtered on age and by the
     * selected resource class or planet. Also resources which are expected to
     * spawn in the worlds but are missing at SWGCraft.org are included.
     * 
     * @param writeButton the source of the triggered event
     */
    private void actionWriteNotes(JButton writeButton) {
        writeButton.setEnabled(false);

        SWGNotes notes = writeNotesCheck(writeButton);

        if (notes != null) writeNotes(notes); // action please

        writeButton.setEnabled(true);
    }

    /**
     * Helper method which updates {code rcBlackList}. The file of blacklisted
     * resource classes is written by the user and such resource classes should
     * not be written to a notes file.
     */
    private void createBlacklist() {
        rcBlackList = new ArrayList<SWGResourceClass>();

        File blf = new File("misc", "submit_blacklist.txt");
        if (blf.exists()) {
            ZReader sr = ZReader.newTextReader(blf);
            if (sr == null) return;

            List<String> sl = sr.lines(true, true);
            for (String line : sl) {
                int rci = SWGResourceClass.rcID(line);
                if (rci >= 0)
                    rcBlackList.add(SWGResourceClass.rc(rci));
                else
                    showErrorDialog(String.format("Parsing \"%s\"%n"
                                + "Invalid resource class: \"%s\"%n%n"
                                + "Ignored until it is corrected",
                                blf, line));
            }
        }
    }

    /**
     * Helper method which applies the edited data from the input fields to the
     * resource which is selected at the main table. If input data is incomplete
     * a GUI dialog alerts the user and either this method does nothing or the
     * input is applied as a string. If the input is valid a resource is created
     * and applied to the GUI list.
     */
    private void editApply() {
        int idx = multipleGUIList.getSelectedIndex();

        Object apply = singleResourceValidateAndCreate();
        if (apply == null) {
            apply = editCompileString();
            if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                    singleSubmitButton, "Input is incomplete, apply anyway?",
                    "Confirm", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE)) { return; }
        }
        multipleResources.set(idx, apply);
        updateMultiList();
        resetSingleTextFields();
        resetSingleSubmitButton();
    }

    /**
     * Helper method which compiles a single string from the content of the
     * different input fields.
     * 
     * @return a string
     */
    private String editCompileString() {
        ZString z = new ZString(singlePlanetDisplay.getText()).app(", ");
        z.app(singleResourceNameDisplay.getText()).app(", ");
        z.app(singleResourceClassDisplay.getText()).app(", ");
        z.app(singleResourceStatsTextField.getText());
        return z.toString();
    }

    /**
     * Helper method which adds the content of the specified resource to the
     * edit fields.
     * 
     * @param mr the resource to prepare for editing
     */
    private void editPrepareResource(SWGMutableResource mr) {
        String planets = planetsToString(mr.availability(), true);
        singlePlanetInput.setText(planets);
        singleResourceNameInput.setText(mr.getName());
        singleResourceStatsTextField.setText(mr.stats().toString(false));
        singleResourceClassInput.setText(mr.rc().rcName());
    }

    /**
     * Helper method which processes the specified argument for editing. This
     * method tries to split the string into "planets", "resource name",
     * "resource class", and "stats" and adds the result to the edit fields. If
     * no resource class is found the string is added as-is to the resource
     * class field.
     * 
     * @param string the string to process
     */
    private void editPrepareString(String string) {
        resetSingleTextFields();

        String[] splitted = string.split("[,.;:]+");
        int rcIdx = findResClassIndex(splitted);

        if (rcIdx <= 0) // no options available
            singleResourceClassInput.setText(string.trim());
        else {
            if (rcIdx >= 2) {
                int i = string.indexOf(splitted[rcIdx - 1]);
                String pls = string.substring(0, i).trim();
                singlePlanetInput.setText(pls);
            }

            singleResourceNameInput.setText(splitted[rcIdx - 1].trim());

            int j = string.indexOf(splitted[rcIdx]) + splitted[rcIdx].length();
            String stats = string.substring(j).trim();
            while (!stats.isEmpty() && !Character.isDigit(stats.charAt(0))) {
                stats = stats.substring(1);
            }
            singleResourceStatsTextField.setText(stats);

            singleResourceClassInput.setText(splitted[rcIdx].trim());
        }
    }

    /**
     * Helper method which enables or disables user input while a background
     * thread is processing. The argument is {@code false} during uploads to
     * SWGCraft.org, and {@code true} when no background job is executing. This
     * method must be called symmetrically by the dispatcher, which must make
     * sure to release the locker also in the case of errors.
     * 
     * @param allowActions {@code true} to enable user actions, {@code false} to
     *        disable
     */
    private void enableUserActions(boolean allowActions) {
        singleSubmitButton.setEnabled(allowActions);
        multiSubmitButton.setEnabled(allowActions);
        noteFileLoadButton.setEnabled(allowActions);
    }

    /**
     * Helper method which returns the index for a <I>possible&nbsp;</I>
     * resource class in {@code split}, -1 if not found. In particular, this
     * method returns an index for an element of the argument which does not
     * begin with a digit, iterating from end to start of the array.
     * 
     * @param split a string split on delimiters " ,.;:", quotes excluded
     * @return index for a possible resource class in {@code splitted}, or -1
     */
    private int findResClassIndex(String[] split) {
        // from back to front, find the first element not starting with digits
        // reg exp means begin-with-digit(s)
        Pattern pattern = Pattern.compile("^[0-9]+", Pattern.CASE_INSENSITIVE);
        int index = split.length - 1;
        for (; index >= 0; --index) {
            Matcher m = pattern.matcher(split[index].trim());
            if (!m.find() && !split[index].trim().isEmpty())
                return index;
        }
        return -1;
    }

    /**
     * This method is called for two different action events, in both cases it
     * is determined if this component has focus and which action to select:
     * <UL>
     * <LI>if focus is changed for{code resourceTab}; this is when the user
     * selects the tab named "Resources", or any one of its siblings</LI>
     * <LI>if focus is changed for this component; this is when the user selects
     * the tab for {@code this} component named "Current Resources", or any of
     * its siblings</LI>
     * </UL>
     * <P>
     * If this component gains focus this implementation updates the GUI.
     * 
     * @param focused {@code true} if there is any focus change for this
     *        component or its siblings, or if {@link #resourceTab} gained
     *        focus; {@code false} otherwise
     */
    void focusGained(boolean focused) {
        if (focused && resourceTab.getSelectedComponent() == this) {
            if (isWorking)
                return;
            isWorking = true;
            if (!isGUIcreated) make();

            if (SWGResourceTab.galaxy() != recentGalaxy)
                actionClearAndReset();
            recentGalaxy = SWGResourceTab.galaxy();

            galaxyDisplay.setText(" " + recentGalaxy + " ");
            SWGHelp.push(helpPage);
            SWGAide.frame().putToLogbar_2(null);
        } else {
            SWGHelp.remove(helpPage);
        }
        isWorking = false;
    }

    /**
     * Helper method which returns the notes object for the station of the
     * current character. This is the object denoted by {code notesFileName}.
     * 
     * @return a notes object, or {@code null} if there is no such notes file
     */
    private SWGNotes getNotes() {
        return getStation().notes(notesFileName);
    }

    /**
     * Helper method which from the specified object returns a resource class.
     * The argument is supposed to be an instance of {@link SWGResourceClass} or
     * {@link SWGKnownResource}.
     * 
     * @param o the object to return the resource class for
     * @return a resource class
     * @throw ClassCastException if the argument is not an instance of
     *        {@link SWGResourceClass} or {@link SWGKnownResource}
     */
    private SWGResourceClass getResourceClass(Object o) {
        if (SWGKnownResource.class.isAssignableFrom(o.getClass()))
            return ((SWGKnownResource) o).rc();
        return (SWGResourceClass) o;
    }

    /**
     * Helper method which returns the station for the current character.
     * 
     * @return a station
     */
    private SWGStation getStation() {
        return SWGFrame.getSelectedCharacter().galaxy().station();
    }

    /**
     * Helper method which determines if the specified resource class is
     * blacklisted. The file of blacklisted resource classes is written by the
     * user and such resource classes, or its sub-classes, must not be written
     * to a notes file.
     * 
     * @param c the resource class to check
     * @return {@code true} if the resource class is blacklisted, {@code false}
     *         otherwise
     */
    private boolean isBlacklisted(SWGResourceClass c) {
        if (rcBlackList == null)
            createBlacklist();

        for (SWGResourceClass b : rcBlackList)
            if (c.isSub(b))
                return true;

        return false;
    }

    /**
     * Helper method which determines if the specified resource class is
     * represented in the set of resources.
     * 
     * @param cl the resource class to scan for
     * @param set the set of resources to scan
     * @return {@code true} if there is a resource with the specified resource
     *         class, {@code false} otherwise
     */
    private boolean isRepresented(SWGResourceClass cl, SWGResourceSet set) {
        for (SWGKnownResource kr : set)
            if (kr.rc() == cl)
                return true;

        return false;
    }

    /**
     * Helper method which returns a list of objects parsed from an in-game
     * notes file. If there is nothing parsed an empty list is returned. See
     * {code multipleResources} for details on the content.
     * 
     * @return a list of objects parsed, or {@link Collections#emptyList()}
     */
    private List<Object> loadNotesFile() {
        List<String> nl = getNotes().lines();
        if (nl == null || !loadNotesFileHeader(nl))
            return Collections.emptyList();

        boolean nonHarvested = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceSubmitNonHarvested", Boolean.FALSE)).booleanValue();

        /*
         * piggy-back on this load-method also for historical resources, it is
         * quite cheap to let common code execute as usual
         */
        boolean old = historical.isSelected();
        if (old && JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(
                noteFileLoadButton, "Load as old / historical?", "Confirm",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
            return Collections.emptyList();

        List<Object> ret = new ArrayList<Object>();
        SWGResourceSet set = SWGResourceManager.getSet(recentGalaxy);

        for (int i = 0; i < nl.size(); ++i) {
            String line = nl.get(i).trim();

            if (line.isEmpty() || line.startsWith("#")) continue;

            String lc = line.toLowerCase(Locale.ENGLISH);
            boolean isDepl = lc.startsWith("depl");

            // stat-less resource not fixed by the user
            if (!isDepl && line.endsWith("add-stats")) continue;

            // blank name/age is ",resource class," >>> no action
            if (line.startsWith(",") && line.charAt(line.length() - 1) == ',')
                continue;

            Object o;
            if (isDepl) {
                SWGKnownResource kr = nonDepleted(recentGalaxy, set, line);
                if (kr != null) ret.add(line); // add the string as is
            } else if (lc.startsWith("=")) {
                if ((o = loadNotesFileNameEdit(line, set)) != null)
                    ret.add(o);
            } else if ((o = loadNotesFileLine(line, nonHarvested, old, set)) != null)
                    ret.add(o);
        }
        return ret;
    }

    /**
     * Helper method which determines if the header of the notes file is valid
     * and that the galaxy matches the current galaxy. If the header is valid
     * {@code true} is returned, otherwise this method displays a GUI error
     * dialog and returns {@code false}.
     * 
     * @param nl a list of strings for the notes
     * @return {@code true} if the file header is valid, {@code false} otherwise
     */
    private boolean loadNotesFileHeader(List<String> nl) {
        String errorMsg = null;

        // looking for a line:
        // # SWGAide : submit resources for: Galaxy
        int i = 0;
        for (; i < nl.size() && i < 5; ++i) { // sanity, max 5 lines
            String input = nl.get(i).trim();

            if (input.isEmpty() || !input.startsWith("#"))
                break; // no galaxy parsed so far
            else if (input.indexOf("SWGAide :") < 0)
                continue;

            int last = input.lastIndexOf(' ');
            if (last < 0)
                continue;

            String gp = input.substring(last).trim();
            String glx = SWGCGalaxy.properName(gp);
            if (glx != null) {
                if (glx.equals(recentGalaxy.getName()))
                    return true;

                errorMsg = String.format("Found %s, but %s is default%n%n" +
                        "Use %s or cancel", gp, recentGalaxy, recentGalaxy);
            }
        }
        if (i >= nl.size()) {
            showErrorDialog("Error parsing notes file");
            return false; // end of file
        }

        errorMsg = errorMsg == null
                ? "Found no galaxy"
                : errorMsg;
        String[] opts = { "Use " + recentGalaxy, "Cancel" };
        return (0 == JOptionPane.showOptionDialog(
                noteFileLoadButton, errorMsg, "Galaxy?",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opts, opts[1]));
    }

    /**
     * Helper method which returns an object parsed from {@code line}. For a
     * line which is not edited by the user {@code null} is returned; that is
     * since the line equals what was written by SWGAide to the notes file and
     * no further action regarding the line is necessary. {@code null} is also
     * returned if a the parsed data equals a locally known resource and adds
     * nothing to its state. If the line is not possible to parse the line
     * itself is returned. If it is possible to parse the line this method
     * returns a resource object parsed from the line. The resource is always of
     * type {@link SWGMutableResource}, also when it is a locally
     * <I>known&nbsp;</I> resource, which allows for this component to amend to
     * it for later submission to SWGCraft.org.
     * 
     * @param line the string to parse
     * @param nonHarv {@code true} if resources which are impossible to harvest
     *        naturally should be included in the result
     * @param old {@code true} if we are loading historical resources
     * @param set all locally cached resources known for the current galaxy
     * @return a resource object, the line itself, or {@code null}
     */
    private Object loadNotesFileLine(
            String line, boolean nonHarv, boolean old, SWGResourceSet set) {

        // input = c,dan,r,, y,alpha,de c,1,500,3,200,500,6,300,300

        // name,resClass,age is an old resource (no action)
        // name,resClass,[stats] is new or edited (action)
        // name,resClass,BLANK may be a stat-less resource without stats added
        // =name=oldname,resClass,* edited name

        // don't split on space since resource class contains spaces (which
        // gives that stats is possibly only one entry but split later)
        String[] split = line.split("[,.;:]+");

        int idx = findResClassIndex(split);

        if (idx < 0)
            return line; // neither resCls nor resName

        // probably "name,resClass,age", a known resource >> (no action)
        // but if last slot is longer than 2 characters it is not age
        if (split.length == 3 && idx == 1 && split[2].trim().length() < 3)
            return null;

        // get resource class name, possibly abbreviated
        List<String> rcl = SWGResourceClass.rcNames(split[idx].trim());
        if (rcl.size() != 1)
            return line; // no resource class or several

        if (idx < 1 || split[idx - 1].trim().isEmpty())
            return line; // no resource name AND resource class, both necessary

        // OK so far, try create a resource
        String inputResName = split[idx - 1];
        String rn = ZString.tac(inputResName);
        if (!ZString.isAlpha(rn))
            return line;

        SWGResourceClass rc = SWGResourceClass.rc(rcl.get(0));
        SWGKnownResource kr = set.getBy(rn, rc); // locally known?

        if (split.length == 2 && kr != null && !kr.stats().hasValues())
            return null; // stat-less but no added stats

        if (!rc.isSpawnable())
            return line;

        if (!nonHarv
                && rc.isSub(SWGCreatureResources.class)
                && !rc.isHarvested())
            return line;

        boolean amended = false;
        SWGMutableResource mr;
        if (kr != null) {
            // "manual" creation to deliberately omit planet info and stats
            mr = new SWGMutableResource(kr);
            if (kr.id() < 0) amended = true;
        } else {
            mr = new SWGMutableResource(rn, rc);
            mr.galaxy(recentGalaxy);
            mr.depleted(false);
            amended = true; // always true if there is no kr
        }

        // OK so far, parse and add stats
        if (kr == null || !kr.stats().hasValues()) {
            // only if there is no kr or if kr lacks stats mr can submit stats
            int statsStart = line.indexOf(split[idx]);
            statsStart += split[idx].length() + 1;
            if (statsStart < line.length()) {
                try {
                    String s = line.substring(statsStart).trim();
                    SWGResourceStats st = SWGResourceStats.newInstance(s, rc);
                    mr.stats(st, true);
                    amended = true;
                } catch (Exception e) {
                    return line;
                }
            }
        }

        if (old) return mr.stats().hasValues()
                ? mr
                : line + ": MISSING STATS";

        // OK so far, parse and add possible planets
        if (idx - 2 >= 0) {
            // idx is still pointing at resCls
            // and something exists before resName

            int atName = line.indexOf(inputResName);
            List<SWGPlanet> pls = new ArrayList<SWGPlanet>();

            // allow for space delimited planet names
            String[] ps = line.substring(0, atName).split("[,.;: ]+");
            for (String pn : ps) {
                SWGPlanet p = SWGPlanet.fromAbbrev(pn);
                if (p == null)
                    return line;
                pls.add(p);
            }
            if (validateClassVsPlanet(rc, pls) == null) {
                for (SWGPlanet p : pls)
                    if (kr == null || !kr.isAvailableAt(p)) {
                        mr.availability(new SWGPlanetAvailabilityInfo(
                                p, System.currentTimeMillis() / 1000,
                                SWGCraft.getUserName())); // only p not in kr
                        amended = true;
                    }
            } else
                return line;
        }

        // figure out if this resource is a planetary resource
        if (mr.availability().size() <= 0) {
            SWGPlanet p = planetFromResourceClass(rc, false);
            if (p != null && !mr.isAvailableAt(p)) {
                mr.availability(new SWGPlanetAvailabilityInfo(
                        p, System.currentTimeMillis() / 1000,
                        SWGCraft.getUserName()));
                amended = true; // if kr != null this is true now
            } else
                return line; // must be at least one planet to submit for
        }

        return amended
                ? mr
                : null;
    }

    /**
     * Helper method which returns a mutable resource, {@code null}, or the
     * specified line. This method is invoked if the line begins with an equal
     * sign, which denotes that the name of a known resource should be edited.
     * The two names can be in any order on the form {@code "=aname=bname, ..."}
     * but if the string is invalid it is returned as is. Otherwise this method
     * obtains a cached resource with the known name and returns a copy of it
     * with the new name. If none of the two names denotes a cached resource
     * this method returns {@code null}.
     * <p>
     * To avoid mistakes this method returns the line as-is if the Levenshtein
     * distance for the old and new names is greater than 4 edits, see
     * {@link ZStuff#levenshteinDistance(String, String, boolean)}.
     * 
     * @param l the line to parse
     * @param rs a set of current resources
     * @return an updated resource, {@code null}, or the string as is
     */
    private Object loadNotesFileNameEdit(String l, SWGResourceSet rs) {
        String[] p = l.split("[=,. ]+");
        if (p.length < 2
                || p[0].isEmpty() && p.length < 3) return l;

        String a = ZString.tac(
                !p[0].isEmpty()
                        ? p[0]
                        : p[1]);
        String b = ZString.tac(
                !p[0].isEmpty()
                        ? p[1]
                        : p[2]);

        // try 4 edits
        if (ZStuff.levenshteinDistance(a, b, false) > 4) 
            return l;

        SWGKnownResource kr = rs.getBy(a, SWGFrame.getSelectedGalaxy());
        kr = kr == null
                ? kr = rs.getBy(b, SWGFrame.getSelectedGalaxy())
                : kr;
        if (kr == null) return null;

        SWGMutableResource mr = new SWGMutableResource(kr);
        mr.name(kr.getName().equals(a)
                ? b // switch name
                : a);

        return mr;
    }

    /**
     * Helper method which creates the content of this component. This is the
     * main entry point for creation of the GUI, done lazily on demand.
     */
    private synchronized void make() {
        if (isGUIcreated) return;

        setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        setLayout(new BorderLayout(0, 2));

        add(makeSinglePanel(), BorderLayout.NORTH);
        add(makeMainPanel(), BorderLayout.CENTER);
        add(makeBottomPanel(), BorderLayout.SOUTH);

        isGUIcreated = true;
    }

    /**
     * Helper method which creates and returns a GUI component for selecting the
     * minimum age for what is written to notes file.
     * 
     * @param toolTip the tool tip text for this component
     * @return a GUI component
     */
    private Component makeAgeChooser(String toolTip) {
        // default age is set to 6 days
        SpinnerNumberModel nModel = new SpinnerNumberModel(6, 0, 15, 1);

        ageSpinner = new JSpinner(nModel);
        ageSpinner.setToolTipText(toolTip);
        ageSpinner.setPreferredSize(new Dimension(50, 26));
        ageSpinner.setMaximumSize(new Dimension(50, 26));

        // no action listener since this object is only read

        return ageSpinner;
    }

    /**
     * Helper method which creates and returns the bottommost panel for this GUI
     * component. The created panel provides the means for user selections and
     * user actions related to the in-game notes file.
     * 
     * @return the bottom GUI panel
     */
    private Component makeBottomPanel() {
        JPanel bottom = new JPanel();
        bottom.setBorder(BorderFactory.createTitledBorder("Manage notes file"));
        bottom.setLayout(new SpringLayout());

        String filterToolTip = "Filter on resource class, planet, or stat-less";
        bottom.add(makeLabel("Filter on ...", filterToolTip));

        String ageToolTip = "Filter on resource age (days)";
        bottom.add(makeLabel("Age", ageToolTip));

        String writeToolTip = String.format(
                "Write to notes file \"%s\"", notesFileName);
        bottom.add(makeLabel(" ", writeToolTip));

        bottom.add(Box.createHorizontalStrut(25));

        bottom.add(makeHistorical());

        bottom.add(Box.createHorizontalGlue());
        String submitToolTip = "Submit multiple resources to SWGCraft.org";
        bottom.add(makeMultiSubmitButton(submitToolTip));

        bottom.add(makeRCPFilterList(filterToolTip));
        bottom.add(makeAgeChooser(ageToolTip));
        bottom.add(makeWriteButton(writeToolTip));
        bottom.add(Box.createHorizontalStrut(25));
        bottom.add(makeLoadButton(String.format(
                "Load from notes file \"%s\" to main table", notesFileName)));
        bottom.add(Box.createHorizontalGlue());
        bottom.add(makeClearButton());

        SpringUtilities.makeCompactGrid(bottom, 2, 7, 0, 0, 1, 0);
        return bottom;
    }

    /**
     * Helper method which creates and returns a GUI button. The returned button
     * is the component which initiates that the GUI is cleared and reset.
     * 
     * @return a button
     */
    private Component makeClearButton() {
        clearButton = new JButton("  Clear  ");
        clearButton.setToolTipText("Clear and reset this panel");

        clearButton.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e) {
                actionClearAndReset();
            }
        });

        return clearButton;
    }

    /**
     * Helper method which creates and returns the GUI error label.
     * 
     * @return the GUI error field
     */
    private JLabel makeErrorField() {
        JLabel l = new JLabel(" ");
        l.setForeground(Color.RED);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /**
     * Helper method which creates and returns a GUI component for submitting
     * historical resources; see {@link #historical}.
     * 
     * @return a GUI component
     */
    private Component makeHistorical() {
        historical = new JCheckBox("Historical");
        historical.setToolTipText(
                "Load as and submit historical resources to SWGCraft");
        return historical;
    }

    /**
     * Helper method for other creation methods. This method creates and returns
     * a GUI label decorated with the specified text and tool tip text.
     * 
     * @param label the text for the label
     * @param toolTip the tool tip text
     * @return a label
     */
    private JLabel makeLabel(String label, String toolTip) {
        JLabel l = new JLabel(label);
        l.setToolTipText(toolTip);
        return l;
    }

    /**
     * Helper method which creates and returns a GUI button. The returned button
     * is the component which initiates loading from the in-game notes file.
     * 
     * @param toolTip the tool tip text for this component
     * @return a GUI button
     */
    private Component makeLoadButton(String toolTip) {
        noteFileLoadButton = new JButton(String.format(
                "Load from \"%s\"", notesFileName));
        noteFileLoadButton.setToolTipText(toolTip);

        noteFileLoadButton.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                actionLoadNotes();
            }
        });

        return noteFileLoadButton;
    }

    /**
     * Helper method which creates and returns the main GUI component. This
     * component contains the table of multiple resources.
     * 
     * @return the main GUI component
     */
    private Component makeMainPanel() {
        multipleGUIList = new JList<Object>();
        multipleGUIList
                .setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        multipleGUIList.setBorder(BorderFactory.createLoweredBevelBorder());
        multipleGUIList.setLayoutOrientation(JList.VERTICAL);
        multipleGUIList.setVisibleRowCount(-1);

        multipleListModel = new MultiResourceModel();
        multipleGUIList.setModel(multipleListModel);

        multipleGUIList.setCellRenderer(new MultipleResCellRenderer());

        // no selection listener except for the mouse listener

        multipleGUIList.addMouseListener(new MouseAdapter() {

            
            @Override
            public void mouseClicked(MouseEvent e) {
                actionTableClicked(e);
            }
        });

        JScrollPane jsp =
                new JScrollPane(multipleGUIList,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setBorder(BorderFactory.createTitledBorder("Multiple resources"));
        return jsp;
    }

    /**
     * Helper method which creates and returns a GUI button. The returned button
     * is the component which initiates submission to SWGCraft.org of the
     * resources displayed at the main table.
     * 
     * @param toolTip the tool tip text for this component
     * @return a button
     */
    private Component makeMultiSubmitButton(String toolTip) {
        multiSubmitButton = new JButton("  Submit  ");
        multiSubmitButton.setToolTipText(toolTip);

        multiSubmitButton.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e) {
                actionSubmitMulti();
            }
        });

        return multiSubmitButton;
    }

    /**
     * Helper method which creates and returns a GUI list for filtering what is
     * written to notes file. The component is populated with names for generic
     * resource classes which cannot spawn in the worlds of SWG and names of the
     * 12 planets.
     * 
     * @param toolTip the tool tip text for this component
     * @return a GUI list
     */
    private Component makeRCPFilterList(String toolTip) {
        List<SWGResourceClass> rcl = SWGResourceClass.rcsFor(
                new Comparable<SWGResourceClass>() {
                    public int compareTo(SWGResourceClass rc) {
                        return rc.isSpawnable() || rc.isSpaceOrRecycled()
                                ? -1
                                : 0;
                    }
                });
        ArrayList<String> ls = new ArrayList<String>(rcl.size() + 13);
        for (SWGResourceClass rc : rcl)
            ls.add(rc.rcName());
        Collections.sort(ls);

        ls.addAll(SWGPlanet.names());

        ls.add("Stat-less");
        String[] lsArr = new String[ls.size()];
        lsArr = ls.toArray(lsArr);
        resourceClassFilter = new JComboBox<String>( lsArr);
        resourceClassFilter.setPreferredSize(new Dimension(250, 26));
        resourceClassFilter.setMaximumSize(new Dimension(300, 26));
        resourceClassFilter.setToolTipText(toolTip);

        // no action listener since this combo box is just read

        resourceClassFilter.setSelectedItem(SWGFrame.getPrefsKeeper().get(
                "resourceSubmitSelectedClass"));

        return resourceClassFilter;
    }

    /**
     * Helper method which creates the upper GUI panel. This is the panel which
     * provides the means for submitting single resources or editing resources
     * selected at the main table.
     * 
     * @return a GUI component
     */
    private JComponent makeSinglePanel() {
        JPanel upper = new JPanel(new BorderLayout());

        errorLabel = makeErrorField();
        upper.add(errorLabel, BorderLayout.NORTH);

        JPanel mP = new JPanel();
        mP.setBorder(BorderFactory.createTitledBorder("Single input / Edit"));
        mP.setLayout(new SpringLayout());
        makeSinglePanel(mP);

        SpringUtilities.makeCompactGrid(mP, 3, 5, 2, 0, 1, 0);

        upper.add(mP, BorderLayout.CENTER);
        return upper;
    }

    /**
     * Helper method which populates the specified panel. This method creates
     * and adds GUI components to the specified panel, components which are
     * related to submission of single resources or editing of resources
     * selected at the main table.
     * 
     * @param middlePanel the GUI panel to populate
     */
    private void makeSinglePanel(JPanel middlePanel) {
        String pt = "Enter planet names, abbreviating is supported";
        String nt = "Enter the resource name";
        String rt = "Enter resource class, abbreviating is supported";
        String st = "Enter the stats, only those values read in-game";

        // Upper row
        middlePanel.add(makeLabel("Planets", pt));
        middlePanel.add(makeLabel("Name", nt));
        middlePanel.add(makeLabel("Resource class", rt));
        middlePanel.add(makeLabel("Stats", st));
        middlePanel.add(new JLabel());

        // Middle row
        singlePlanetInput = makeTextField(pt);
        singlePlanetInput.getDocument().addDocumentListener(
                new PlanetFieldListener());
        middlePanel.add(singlePlanetInput);

        singleResourceNameInput = makeTextField(nt);
        ((AbstractDocument) singleResourceNameInput.getDocument())
                .setDocumentFilter(new SWGDocFilter() {
                    
                    @Override
                    protected boolean isValid(String s) {
                        if (s == null || s.isEmpty() || ZString.isAlpha(s)) {
                            singleResourceNameDisplay.setText(ZString.tac(s));
                            return true;
                        }
                        return false;
                    }
                });
        middlePanel.add(singleResourceNameInput);

        singleResourceClassInput = makeTextField(rt);
        singleResourceClassInput.getDocument().addDocumentListener(
                new ResourceClassFieldListener());
        middlePanel.add(singleResourceClassInput);

        singleResourceStatsTextField = makeTextField(st);
        singleResourceStatsTextField.getDocument().addDocumentListener(
                new ResourceStatsFieldListener());
        middlePanel.add(singleResourceStatsTextField);

        galaxyDisplay = new JLabel();
        galaxyDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        galaxyDisplay.setBorder(BorderFactory.createLineBorder(Color.ORANGE));
        galaxyDisplay.setBackground(new Color(255, 255, 153));
        galaxyDisplay.setOpaque(true);
        middlePanel.add(galaxyDisplay);

        // Bottom row
        singlePlanetDisplay = makeLabel("", pt);
        middlePanel.add(singlePlanetDisplay);

        singleResourceNameDisplay = makeLabel("", nt);
        middlePanel.add(singleResourceNameDisplay);

        singleResourceClassDisplay = makeLabel("", rt);
        middlePanel.add(singleResourceClassDisplay);

        singleResourceStatsDisplay = makeLabel("", st);
        middlePanel.add(singleResourceStatsDisplay);

        singleSubmitButton = new JButton();
        resetSingleSubmitButton();
        singleSubmitButton.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e) {
                actionSubmitSingle();
            }
        });
        middlePanel.add(singleSubmitButton);
    }

    /**
     * Helper method which creates and returns a GUI text input field. The
     * component has the specified tool tip text and is decorated with a lowered
     * bevel border.
     * 
     * @param toolTipText the tool tip text for the text field
     * @return the new text input field
     */
    private JTextField makeTextField(String toolTipText) {
        JTextField tf = new JTextField();
        tf.setBorder(BorderFactory.createLoweredBevelBorder());
        tf.setToolTipText(toolTipText);
        return tf;
    }

    /**
     * Helper method which creates and returns a GUI button. The returned button
     * is the component which initiates that resources are written to the
     * in-game notes file.
     * 
     * @param toolTip the tool tip text for this component
     * @return a button
     */
    private Component makeWriteButton(String toolTip) {
        final JButton write = new JButton(String.format(
                "Write to \"%s\"", notesFileName));
        write.setToolTipText(toolTip);

        write.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                actionWriteNotes(write);
            }
        });
        write.setEnabled(SWGFrame.getSelectedCharacter().galaxy().exists());

        return write;
    }

    /**
     * Helper method which displays a GUI popup dialog.
     * 
     * @param e the event that triggered this action
     */
    private void multipleTablePopup(MouseEvent e) {
        resetSingleTextFields();
        resetSingleSubmitButton();

        final int row = multipleGUIList.locationToIndex(e.getPoint());
        multipleGUIList.setSelectedIndex(row);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem del = new JMenuItem("Delete entry");
        del.setToolTipText("Delete selected entry, does not affect the file");
        del.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent ev) {
                actionDelete(row);
            }
        });
        popup.add(del);
        del.setEnabled(multipleResources != null && row >= 0);

        popup.addSeparator();

        final JCheckBoxMenuItem nh =
                new JCheckBoxMenuItem("Allow non-harvestable");
        nh.setToolTipText("Allow resources impossible to harvest naturally");
        nh.setSelected(((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceSubmitNonHarvested", Boolean.FALSE)).booleanValue());
        nh.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e1) {
                SWGFrame.getPrefsKeeper().add("resourceSubmitNonHarvested",
                        Boolean.valueOf(nh.isSelected()));
            }
        });
        popup.add(nh);

        popup.show(multipleGUIList, e.getX(), e.getY());
    }

    /**
     * Helper method which returns a resource parsed from the specified string,
     * or null. The string is supposed to be at the form "depl, Resourcename..."
     * and this method returns a non-depleted resource with a matching name. If
     * the string is invalid, or if no resource exists with the name, or if the
     * resource is already in depleted state this method returns {@code null}.
     * resource
     * 
     * @param gxy a galaxy constant
     * @param set a set of resources for the galaxy
     * @param str a string to parse
     * @return a resource that is not depleted, or {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    private SWGKnownResource nonDepleted(
            SWGCGalaxy gxy, SWGResourceSet set, String str) {

        String[] spl = str.split("[ ,]+");
        if (spl.length <= 1) return null;

        String rn = ZString.tac(spl[1]);
        SWGKnownResource kr = set.getBy(rn, gxy);
        return kr == null || kr.isDepleted()
                ? null
                : kr;
    }

    /**
     * Helper method which returns a planet derived from the argument, or
     * {@code null} if no planet could be derived.
     * <P>
     * The boolean parameter is used for the special handling necessary for
     * non-mandatory resource classes from Mustafar, which is the only planet
     * which has its planet name in the resource class names while these are not
     * mandatory &mdash; mandatory in the sense there must always be at least
     * one such resource class in spawn.
     * 
     * @param rc a resource class
     * @param m {@code true} if only mandatory resource classes should be
     *        considered, {@code false} otherwise
     * @return a planet, or {@code null}
     */
    private SWGPlanet planetFromResourceClass(SWGResourceClass rc, boolean m) {
        SWGPlanet p = SWGPlanet.fromQuad(rc.rcName().substring(0, 4));

        // we must handle Mustafar specially since only this planet has its
        // planet name in non-mandatory resource classes

        if (!m || p != SWGPlanet.MUSTAFAR)
            return p;

        if (rc == SWGMustafarianFiberplast.getInstance()
                || rc.isSub(SWGOrganic.class))
            return p;

        return null;
    }

    /**
     * Helper method which resets {code singleSubmitButton}. This method sets
     * the visible text and the tool tip text to the default state, which is
     * "Submit".
     */
    private void resetSingleSubmitButton() {
        singleSubmitButton.setText("Submit");
        singleSubmitButton
                .setToolTipText("Submit resource data to SWGCraft.org");
    }

    /**
     * Helper method which empties the text input fields for single resources.
     */
    private void resetSingleTextFields() {
        singlePlanetInput.setText(null);
        singleResourceNameInput.setText(null);
        singleResourceClassInput.setText(null);
        singleResourceStatsTextField.setText(null);
    }

    /**
     * Helper method which displays a GUI error dialog with the specified
     * message. The dialog is displayed over the specified GUI component.
     * 
     * @param errorMsg the message
     */
    private void showErrorDialog(String errorMsg) {
        JOptionPane.showMessageDialog(this, errorMsg, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Helper method which parses and returns a list of planets. The input is
     * either user input or auto-set if the resource class is planetary. If
     * there is an error it is displayed at the GUI error label and {@code null}
     * is returned.
     * 
     * @return a list of planets, or {@code null}
     */
    private List<SWGPlanet> singleParsePlanets() {
        String input = singlePlanetInput.getText();

        List<SWGPlanet> planets = new ArrayList<SWGPlanet>();
        String error = null;

        for (String s : input.split("[ ,;.:]+")) {
            if (s.isEmpty()
                    || (s.equals("4") && planets.contains(SWGPlanet.YAVIN4)))
                continue;

            SWGPlanet p = SWGPlanet.fromAbbrev(s);
            if (p == null) {
                error = "No planet for \"" + s + "\"";
                break;
            } else if (!planets.contains(p))
                planets.add(p);
        }

        if (input.isEmpty() || (error != null && planets.size() <= 0))
            error = "No planets";

        if (error != null) {
            errorLabel.setText(error);
            return null;
        }

        errorLabel.setText("  ");
        singlePlanetDisplay.setText(planetsToString(planets, true));
        return planets;
    }

    /**
     * Helper method which creates and returns a mutable resource. If there is
     * an error a message is displayed at the GUI error label and {@code null}
     * is returned.
     * 
     * @param name the name for the resource
     * @param stats the stats for the resource
     * @param pls a list of planets for the resource
     * @return a mutable resource, or {@code null}
     */
    private SWGMutableResource singleResourceCreate(String name,
            SWGResourceStats stats, List<SWGPlanet> pls) {

        // resourceClass is already validated
        try {
            SWGMutableResource mr = new SWGMutableResource(name, resourceClass);
            mr.stats(stats, false); // already validated
            mr.depleted(false);
            mr.galaxy(recentGalaxy);
            for (SWGPlanet p : pls)
                mr.availability(new SWGPlanetAvailabilityInfo(
                        p, System.currentTimeMillis() / 1000,
                        SWGCraft.getUserName()));

            return mr;
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        }
        return null;
    }

    /**
     * Helper method which parses input fields, creates and returns a resource.
     * All input fields are parsed and validated and if there is an error a
     * message is displayed at the GUI error label and {@code null} is returned.
     * 
     * @return a resource, or {@code null}
     */
    private SWGMutableResource singleResourceValidateAndCreate() {
        SWGResourceStats st = statsParse();
        if (st == null)
            return null;
        String rn = singleSubmitValidatedName();
        if (rn == null)
            return null;
        List<SWGPlanet> pls = singleParsePlanets();
        if (pls == null)
            return null;
        if (!singleValidateClassVsPlanet(resourceClass, pls))
            return null;

        return singleResourceCreate(rn, st, pls);
    }

    /**
     * Helper method which validates the entered name. If the entered name is
     * three letters or more this method returns the name properly capitalized.
     * Otherwise a message is displayed at the GUI error label and {@code null}
     * is returned.
     * 
     * @return a valid name, or {@code null}
     */
    private String singleSubmitValidatedName() {
        String name = singleResourceNameDisplay.getText();
        if (name.length() >= 3)
            return ZString.tac(name);

        if (name.isEmpty())
            errorLabel.setText("No name");
        else
            errorLabel.setText('\"' + name + "\" is not a valid name");

        return null;
    }

    /**
     * Helper method which returns {@code true} if the specified resource class
     * can spawn at all planets in the specified list. This method is used for
     * single resource input. If there is a planet at which the resource class
     * cannot spawn this implementation updates the GUI error label and returns
     * false, otherwise {@code null} is returned.
     * 
     * @param resClass the resource class to check for
     * @param planets a list of planets
     * @return {@code false} if there is a planet the resource class cannot
     *         spawn at
     * @see #validateClassVsPlanet(SWGResourceClass, List)
     */
    private boolean singleValidateClassVsPlanet(
            SWGResourceClass resClass, List<SWGPlanet> planets) {

        String error = validateClassVsPlanet(resClass, planets);
        if (error == null)
            return true;

        errorLabel.setText(error);
        return false;
    }

    /**
     * Helper method which populates the display for resource stats. This is to
     * aid the user on which stats to add for the resource.
     */
    private void statsDisplayPopulate() {
        if (resourceClass != null) {
            ZString z = new ZString();
            for (Stat s : Stat.gameOrder())
                if (resourceClass.has(s)) z.app(s.name()).app("    ");

            singleResourceStatsDisplay.setText(z.toString());
        } else
            singleResourceStatsDisplay.setText("  ");
    }

    /**
     * Helper method which parses and validates user input at the stats input
     * fields and returns a stats object. If there is an error a message is
     * displayed at the error label and {@code null} is returned. If there is no
     * input to parse {@link SWGResourceStats#BLANK} is returned.
     * 
     * @return resource stats,{@link SWGResourceStats#BLANK}, or {@code null}
     */
    private SWGResourceStats statsParse() {
        String input = singleResourceStatsTextField.getText().trim();
        SWGResourceStats stats = null;
        String err = null;
        if (!input.isEmpty() && resourceClass == null)
            err = "No resource class specified";
        else if (!input.isEmpty()) {
            try {
                stats = SWGResourceStats.newInstance(input, resourceClass);
                if (stats != null)
                    err = SWGResourceClass.validate(stats, resourceClass);
            } catch (Exception e) {
                err = e.getMessage();
                stats = null;
            }
        } else
            stats = SWGResourceStats.BLANK; // zero-values is OK

        errorLabel.setText(err == null
                ? "  "
                : err);
        return stats;
    }

    /**
     * Helper method which initiates the GUI before submission of resources to
     * SWGCraft.org begins.
     */
    private void submitBefore() {
        enableUserActions(false);
        SWGFrame f = SWGAide.frame();
        f.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        f.progressBar.setIndeterminate(true);
        f.putToLogbar_2(f.progressBar);
    }

    /**
     * Helper method which from the specified list submits to SWGCraft.org
     * resources which the user has marked as depleted. Only locally cached,
     * known resources are submitted.
     * 
     * @param notes a list of objects containing strings for depleted objects to
     *        submit, and mutable resources which are ignored
     * @param gxy the galaxy to submit for
     * @param set a set of <I>all&nbsp;</I> resources for this galaxy
     * @return the empty string if there is no error, otherwise an error message
     */
    private ZString submitDepleted(List<Object> notes, SWGCGalaxy gxy,
            SWGResourceSet set) {

        ZString error = new ZString();
        for (int i = 0; i < notes.size(); ++i) {
            Object o = notes.get(i);
            if (o instanceof String) {
                String s = (String) o;
                if (s.toLowerCase(Locale.ENGLISH).startsWith("depl")) {
                    SWGKnownResource kr = nonDepleted(gxy, set, s);
                    if (kr == null || kr.isDepleted())
                        continue; // unknown or already depleted

                    String e = submitDepleted(kr, notes, i);
                    if (e.endsWith("password is incorrect"))
                        return new ZString(e);

                    if (!e.isEmpty()) error.app(e);
                }
            }
        }
        return error;
    }

    /**
     * Helper method which sends a depleted message to SWGCraft.org for the
     * specified resource. This method also updates the specified GUI list.
     * 
     * @param kr the depleted resource
     * @param notes the GUI list, {@code null} if GUI is not affected
     * @param index the index for {@code resource} at {@code notesList}, or -1
     * @return the empty string if there is no error, otherwise an error message
     */
    private String submitDepleted(
            SWGKnownResource kr, List<Object> notes, int index) {

        long t = System.currentTimeMillis() / 1000;

        SWGSoapStatusResponse resp = SWGResourceManager.sendDeplete(kr, t);

        if (!resp.isFaultless()) {
            if (resp.getStatus() == 999)
                return "User name and/or password is incorrect";

            return resp.getFaultMessage() + '\n';
        }
        if (notes != null) {
            // ugly hack, see get-cell-renderer for multiple-list
            notes.set(index, "DEPLETED, " + kr.getName());
            updateMultiList();
        }

        return "";
    }

    /**
     * Helper method which resets and enables the GUI of this component, invokes
     * {@link #submitDoneDialog(List)}, and triggers the resource controller to
     * scan guards and monitors for the specified galaxy. If the string denotes
     * error messages it is displayed for the user. This method executes on the
     * event background thread.
     * 
     * @param g a galaxy constant
     * @param err possible error messages, or the empty string
     * @param sim a list of similarly named resources, or {@code null} or empty
     */
    private void submitDone(
            final SWGCGalaxy g, final String err, final List<Wrapper> sim) {
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                if (!err.isEmpty()) showErrorDialog(
                    "These errors occurred while submitting:\n\n" + err);

                submitDoneDialog(sim);

                SWGFrame f = SWGAide.frame();
                f.putToStatbar("Done");
                f.putToLogbar_2(null);
                f.progressBar.setIndeterminate(false);

                f.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                enableUserActions(true);

                if (sim != null || err.isEmpty())
                    SWGResourceManager.notifySubscribers(
                            new ResourceUpdate(UpdateType.LOCAL_SUBMISSION, g));
            }
        });
    }

    /**
     * Helper method which displays a dialog for similarly named resources. If
     * the argument is {@code null} or empty this method does nothing.
     * 
     * @param sim a list for similarly named resources
     */
    private void submitDoneDialog(List<SWGSimilarNameDialog.Wrapper> sim) {
        if (sim != null && !sim.isEmpty()) {
            SWGSimilarNameDialog.display(SWGSubmitTab.this, sim);
            updateMultiList();
        }
    }

    /**
     * Helper method which sends an edit message to SWGCraft.org derived from
     * the mutable resource. It is assumed the the mutable resource has some
     * difference compared to the known resource. This method compares the two
     * and invokes the appropriate method at the resource manager. If there is
     * no difference or if the known resource is {@code null} this method does
     * nothing.
     * 
     * @param mr the resource which is edited or amended to in some way
     * @param kr the locally known resource, not yet updated
     * @return the empty string if there is no error, otherwise an error message
     */
    private String submitEdit(SWGMutableResource mr, SWGKnownResource kr) {
        if (kr == null)
            return "";

        // determine what the difference is between mr and kr, can be
        // * planet availability
        // * added stats
        // * both
        // * edited name

        if (!kr.stats().hasValues() && mr.stats().hasValues()
                || !kr.getName().equals(mr.getName())) {
            SWGResourceManager.updateInstance(kr, mr);
            SWGSoapEditResResponse resp = SWGResourceManager.sendEdit(kr);
            if (!resp.isFaultless())
                return resp.getFaultMessage() + '\n';
        }

        // mr only has planets not found in kr, see notesFileLine()
        List<SWGPlanet> pav = mr.availability();
        if (pav.size() > 0) {
            String u = (String) SWGFrame.getPrefsKeeper().get("optionUserName");
            long t = System.currentTimeMillis() / 1000;
            for (SWGPlanet p : pav) {
                SWGSoapAvailResResponse resp =
                        SWGResourceManager.sendAvailable(kr,
                                new SWGPlanetAvailabilityInfo(p, t, u));
                if (!resp.isFaultless())
                    return resp.getFaultMessage() + '\n';
            }
        }
        mr.whatever = "OK edit";
        return "";
    }

    /**
     * Helper method to {code actionSubmitMulti()}.
     * 
     * @param gxy the galaxy to submit for
     * @param notes a list of mutable resources and strings which are parsed
     *        from the notes file
     * @param old {@code true} if we are loading historical resources
     * @param set a set of <I>all&nbsp;</I> resources for this galaxy
     * @param sim a list to add similar resources to
     * @return the empty string if there is no error, otherwise an error message
     */
    private String submitMulti(SWGCGalaxy gxy, List<Object> notes,
            boolean old, SWGResourceSet set, List<Wrapper> sim) {

        if (notes == null) return "";

        if (old) return submitResources(notes, set, null);

        // handle resources the user has marked as "depl[eted]"
        ZString error = submitDepleted(notes, gxy, set);
        if (error.toString().endsWith("password is incorrect"))
            return error.toString();

        // submit those resources that the user has added as new
        error.app(submitResources(notes, set, sim));

        return error.toString();
    }

    /**
     * Helper method which submits the specified resource to SWGCraft.org as a
     * new resource. If the name of the resource is too similar to a resource
     * the resource manager aborts and returns a {@link SWGSoapNOResResponse}
     * with a reference to the similar resource. Otherwise this method always
     * returns a string, if there is an error the string reads the error
     * message, otherwise it is the empty string.
     * 
     * @param mr the resource to submit
     * @return a {@link SWGSoapNOResResponse}, an empty string, or an error
     *         message
     */
    private Object submitNew(SWGMutableResource mr) {
        try {
            SWGSoapStatusResponse resp = SWGResourceManager.sendNew(mr, false);
            if (resp == null)
                return "Error: unknown reason, maybe clue is in log files";

            if (!resp.isFaultless())
                return resp.getStatus() == -2
                        ? resp
                        : resp.getFaultMessage() + '\n';

            mr.whatever = "OK new";
            return "";
        } catch (RuntimeException e) {
            return e.getMessage() + '\n';
        }
    }

    /**
     * Helper method which submits the specified resource to SWGCraft.org as a
     * historical resource. This method always returns a string, if there is an
     * error the string reads the error message, otherwise an empty string.
     * 
     * @param mr the resource to submit
     * @return an error message, or an empty string
     */
    private String submitOld(SWGMutableResource mr) {
        try {
            SWGSoapNOResResponse resp = SWGResourceManager.sendOld(mr);

            if (!resp.isFaultless()) return resp.getFaultMessage() + '\n';

            mr.whatever = "OK old";
            return "";
        } catch (RuntimeException e) {
            return e.getMessage() + '\n';
        }
    }

    /**
     * Helper method which for each element in the specified list invokes
     * another helper method; if it is a new resource {code
     * submitNew(SWGMutableResource)}, or if it is a resource amended to {code
     * submitEdit(SWGMutableResource, SWGKnownResource)}, or if it is a
     * historical resource {@code submitOld(SWGMutableResource)}.
     * 
     * @param notes a list of objects containing mutable resources to submit,
     *        and string which are ignored
     * @param set a set of <I>all&nbsp;</I> locally known, cached resource for
     *        the specified galaxy
     * @param sim a list to add similar resources to, {@code null} if invocation
     *        is about historical resources
     * @return the empty string if there is no error, otherwise an error message
     */
    private String submitResources(
            List<Object> notes, SWGResourceSet set, List<Wrapper> sim) {

        ZString error = new ZString();
        for (int i = 0; i < notes.size(); ++i) {
            Object o = notes.get(i);
            if (o instanceof SWGMutableResource) {
                SWGMutableResource mr = (SWGMutableResource) o;

                if (sim == null)
                    error.app(submitOld(mr));
                else if (mr.id() <= 0) {
                    Object r = submitNew(mr);

                    if (r instanceof String)
                        error.app(r);
                    else {
                        SWGSoapNOResResponse k = (SWGSoapNOResResponse) r;
                        Wrapper w = SWGSimilarNameDialog.wrapperInstance(k, mr);
                        sim.add(w);
                    }
                } else
                    error.app(submitEdit(mr, set.getByID(mr.id())));

                updateMultiList();
            }
        }
        return error.toString();
    }

    /**
     * Helper method which submits a single resource to SWGCraft.org. Before
     * anything is submitted the entered data is validated and if there is an
     * error it is displayed at the GUI error label.
     */
    private void submitSingle() {

        final SWGMutableResource mr = singleResourceValidateAndCreate();
        if (mr == null)
            return;

        submitBefore();

        final ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(new Runnable() {

            
            public void run() {
                String error = "";
                try {
                    Object o = submitNew(mr);
                    error = (o instanceof String)
                            ? (String) o
                            : ((SWGSoapNOResResponse) o).getStatusString();
                } catch (Exception e) {
                    SWGAide.printError("SWGSubmitTab:singleSubmit:b", e);
                    error = e.getMessage();
                }
                submitDone(mr.galaxy(), error, null);
                exec.shutdown();
            }
        });
    }

    /**
     * Helper method which triggers a refresh of the main list of resources.
     */
    void updateMultiList() {
        multipleListModel.fireContentsChanged();
    }

    /**
     * Helper method which determines if the specified resource class can spawn
     * at all planets in the specified list. This method returns an error
     * message stating if there is a planet at which the resource class cannot
     * spawn, otherwise {@code null} is returned. These are the rules:
     * <ul>
     * <li>only planetary resource classes are restricted to planets</li>
     * <li>generic resources can spawn at any planet, ...</li>
     * <li><B>exception:</B> only Kashyyykian and Mustafarian resource classes
     * can spawn at Kashyyyk and Mustafar</li>
     * </ul>
     * <P>
     * This method does not validate that the specified resource class is
     * spawnable as such.
     * 
     * @param resClass the resource class to check for
     * @param planets a list of planets
     * @return an error message if there is a planet the resource class cannot
     *         spawn at, otherwise {@code null}
     */
    private String validateClassVsPlanet(
            SWGResourceClass resClass, List<SWGPlanet> planets) {

        if (resClass == null)
            return "Specify resource class";

        SWGPlanet p = SWGPlanet.fromQuad(resClass.rcName().substring(0, 4));

        if (p == null) {
            // a generic resource, these cannot spawn at Kashyyyk or Mustafar
            if (planets.contains(SWGPlanet.KASHYYYK)
                    || planets.contains(SWGPlanet.MUSTAFAR))
                return "Resource cannot spawn at Kashyyyk and/or Mustafar";

            return null;
        } // else ::: not generic, must be planetary

        if (planets.size() > 1)
            return "Planetary resource spawns only at: " + p.getName();

        p = planets.get(0);
        if (!SWGResourceClass.canSpawnAt(resClass, p))
            return "Resource class cannot spawn at: " + p.getName();

        return null;
    }

    /**
     * Helper method which is the entry point to write to notes file and to
     * handle the logic for that.
     * 
     * @param notes the notes to write to
     */
    private void writeNotes(SWGNotes notes) {
        // name of resource class or planet
        String str = (String) resourceClassFilter.getSelectedItem();
        SWGFrame.getPrefsKeeper().add("resourceSubmitSelectedClass", str);

        multiFiltered.add(str);
        SWGResourceSet set = writeNotesFilter(str);

        List<Object> mand;
        if (!str.equals("Stat-less")) {
            // ensure that mandatory classes are included in the list to write
            // completeness must be done prior to other filtering
            mand = writeNotesMandatory(set, str);
            mand = writeNotesAge(mand);
        } else
            mand = writeNotesStatless(set);

        mand = writeNotesBlacklist(mand);

        // sort in 30k-deed-wise
        Collections.sort(mand, new Comparator<Object>() {

            
            public int compare(Object o1, Object o2) {
                return getResourceClass(o1).compareTo(getResourceClass(o2));
            }

        });

        writeNotes(notes, mand, str);
    }

    /**
     * Helper method which writes the content of the specified list to the notes
     * file. If there is an error a message is displayed at a GUI error dialog.
     * 
     * @param notes the notes file to write to
     * @param mand a list of resources and resource classes to write
     * @param text the name of a resource class or a planet, for group spacing
     */
    private void writeNotes(SWGNotes notes, List<Object> mand, String text) {
        try {
            ZString z = new ZString();
            if (notes.length() <= 0)
                writeNotesHeader(z, recentGalaxy, text);
            else
                z.nl();

            if (mand.size() <= 0) z.app("# Nothing to update");

            // initiate
            // keep track of these variables for inserting empty line or not and
            // to keep resources grouped on super-class
            int emptyLine = 0;
            Class<?> parent, prev = null;
            boolean pl = SWGPlanet.isValid(text);

            // engage
            for (Object o : mand) {
                parent = getResourceClass(o).getClass().getSuperclass();
                if (prev == null) prev = parent;

                if (emptyLine >= 5 || (!pl && prev != parent)) {
                    z.nl();
                    prev = parent;
                    emptyLine = 0;
                }

                writeNotesLine(z, o, text);
                ++emptyLine;
            }

            notes.add(z.nl().toString(), true);
        } catch (Throwable e) {
            SWGAide.printDebug("subm", 1, "SWGSubmitTab:writeNotes:",
                    e.getMessage());
            showErrorDialog("Error writing notes file\n" + e.getMessage());
        }
    }

    /**
     * Helper method which returns a list in which the spawning resources are
     * filtered on their age. The age is selected by the user, the default age
     * is 6 days. The specified list includes both resources and instances of
     * mandatory resource classes, these are ignored and returned as-is, see
     * {code writeNotesMandatory(SWGResourceSet, List)}.
     * <p>
     * <B>Notice: </B>The returned list also includes resources which are
     * reported without stats, no matter their age. This is to promote the user
     * to add stats.
     * 
     * @param mand the list of objects to filter
     * @return a of current resources which are older than the specified age,
     *         also mandatory resource classes and resources without stats
     */
    private List<Object> writeNotesAge(List<Object> mand) {
        long limit = ((Integer) ageSpinner.getValue()).longValue() * 3600 * 24;
        limit = (System.currentTimeMillis() / 1000) - limit;

        List<Object> ret = new ArrayList<Object>(mand.size());
        for (Object o : mand) {
            if (SWGKnownResource.class.isAssignableFrom(o.getClass())) {
                SWGKnownResource kr = (SWGKnownResource) o;
                if (!kr.stats().hasValues())
                    ret.add(kr);
                else {
                    SWGPlanetAvailabilityInfo rav = kr.availableFirst();
                    long ra = rav.available();
                    if (ra > 0 && ra < limit)
                        ret.add(kr);
                }
            } else
                ret.add(o); // it is resource class
        }
        return ret;
    }

    /**
     * Helper method which filters the specified list and returns the remaining
     * elements. The returned list contains objects which the user has not added
     * to blacklisted resource classes, see {code rcBlackList}.
     * 
     * @param mand a list of objects to check
     * @return a list of objects in which no element is listed in the blacklist
     */
    private List<Object> writeNotesBlacklist(List<Object> mand) {
        List<Object> ret = new ArrayList<Object>(mand.size());
        for (Object o : mand) {
            SWGResourceClass c;
            if (SWGKnownResource.class.isAssignableFrom(o.getClass()))
                c = ((SWGKnownResource) o).rc();
            else
                c = (SWGResourceClass) o;

            if (!isBlacklisted(c))
                ret.add(o);
        }
        return ret;
    }

    /**
     * Helper method which determines if the notes file {code notesFileName}
     * exists. If the notes file does not exist it is created. Otherwise, this
     * method invokes {code writeNotesCheckEmpty(SWGNotes, JButton)}. If the
     * user canceled or if there is any error {@code null} is returned.
     * 
     * @param write the button for this event
     * @return the notes file, or {@code null}
     */
    private SWGNotes writeNotesCheck(JButton write) {
        SWGNotes notes = getNotes();
        if (notes == null)
            notes = SWGNotes.getInstance(
                    notesFileName, getStation(), null, true);
        if (notes == null || !writeNotesCheckEmpty(notes, write))
            return null;

        return notes;
    }

    /**
     * Helper method which determines if the specified notes file is empty. If
     * the file is not empty the user is opted to erase the content, append to
     * file, or to cancel. If the user selects to cancel {@code false} is
     * returned, otherwise the selected action executes and {@code true} is
     * returned.
     * 
     * @param notes the notes file to check
     * @param write the button for this event
     * @return {@code true} to continue, {@code false} to cancel
     */
    private boolean writeNotesCheckEmpty(SWGNotes notes, JButton write) {
        String[] options = { "Erase", "Append", "Cancel" };
        if (notes.length() > 0) {
            int res = JOptionPane.showOptionDialog(write, String.format(
                    "\"%s\" is not empty!", notesFileName), "Select action",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            
            if (res == 2)
                return false;
            if (res == 0)
                notes.erase(false);
        }
        return true;
    }

    /**
     * Helper method which returns a set of spawning resources filtered on the
     * selected resource class or the selected planet. The argument is a valid
     * name for resource class or for a planet.
     * 
     * @param text the textual representation of what to filter on
     * @return a set of resources
     * @throws IllegalArgumentException if the argument is invalid
     */
    private SWGResourceSet writeNotesFilter(String text) {
        SWGResourceSet coll = SWGResourceManager.getSpawning(recentGalaxy);
        if (text.equals("All Resources") || text.equals("Stat-less"))
            return coll;

        int rci = SWGResourceClass.rcID(text);
        if (rci >= 0) {
            SWGResourceClass rc = SWGResourceClass.rc(rci);
            SWGResourceSet set = coll.subsetBy(rc);

            if (rc.isSub(SWGCreatureResources.class)
                    && !((Boolean) SWGFrame.getPrefsKeeper().get(
                            "resourceSubmitNonHarvested", Boolean.FALSE)).
                            booleanValue()) {
                // return a set with only harvested resources
                SWGResourceSet s = new SWGResourceSet(set.size());
                for (SWGKnownResource kr : set)
                    if (kr.rc().isHarvested()) s.add(kr);

                return s;
            }
            // else
            return set;
        }

        return coll.subsetBy(SWGPlanet.fromName(text));
    }

    /**
     * Helper method which appends a header.
     * 
     * @param z a string
     * @param gxy the galaxy this action pertains to
     * @param txt the resource class or planet filtered for
     */
    private void writeNotesHeader(ZString z, SWGCGalaxy gxy, String txt) {
        z.app("# SWGAide : submit resources to: ").appnl(gxy.getName());

        z.app("# Date: ").app(ZStuff.dateString(true));
        z.app(" ---  \"").app(txt).appnl("\"");

        z.app("# Printed: name, resource class, age");
        z.appnl(" ||| if only a resource class >>> missing at SWGCraft.org");

        z.app("# REPLACE: newName, rc, stats ||| do not edit");
        z.appnl(" rc (res-class), let line stay if nothing changed");

        z.app("# ADD NEW: planet(s), name, rc, stats");
        z.appnl(" ||| planets and rc can be abbreviated");

        z.app("# EDIT: planet(s), newName, rc, stats ||| do not edit");
        z.appnl(" rc, let line stay if nothing changed");

        z.appnl("# DEPLETED: depl, name ||| that is all").nl();
    }

    /**
     * Helper method which appends one line to the notes file.
     * 
     * @param z a string
     * @param o the resource or resource class to append
     * @param txt the text that is selected at the
     *        resource-class/planet/stat-less filter
     */
    private void writeNotesLine(ZString z, Object o, String txt) {
        if (SWGKnownResource.class.isAssignableFrom(o.getClass())) {
            SWGKnownResource kr = (SWGKnownResource) o;
            if (kr.stats().hasValues()) {
                SWGPlanetAvailabilityInfo a = kr.availableFirst();
                long s = (System.currentTimeMillis() / 1000) - a.available();
                z.app(String.format("%s , %s, %s", kr.getName(),
                        kr.rc().rcName(), Long.toString(s / 86400)));
            } else
                z.app(String.format("%s , %s, %s", kr.getName(),
                        kr.rc().rcName(), txt.equals("Stat-less")
                                ? ""
                                : "add-stats"));
        } else
            z.app(String.format(" , %s, ", ((SWGResourceClass) o).rcName()));

        z.nl();
    }

    /**
     * Helper method which creates and returns a list of mandatory resource
     * objects. The returned list contains all spawning resources from the
     * specified set. Furthermore this method iterates over the list of
     * mandatory resource classes and for each class which is not represented in
     * the set of spawning resources that class is added to the list which is
     * returned. Thus, the returned list may contain both
     * {@link SWGKnownResource} and {@link SWGResourceClass}, objects which are
     * mandatory in the worlds of SWG.
     * 
     * @param spawning a set of spawning resources, as reported to SWGCraft.org
     * @param crl a list of mandatory resource classes
     * @return a list of spawning resources and resource classes
     */
    private List<Object> writeNotesMandatory(SWGResourceSet spawning,
            List<SWGResourceClass> crl) {

        List<Object> ret = new ArrayList<Object>(spawning);

        for (SWGResourceClass c : crl)
            if (!isRepresented(c, spawning))
                ret.add(c);

        return ret;
    }

    /**
     * Helper method which returns a list of mandatory objects. This method
     * ensures that mandatory resource classes, those which always are in spawn,
     * are present in the specified set of spawning resources. If a resource
     * class is missing it is added to the returned list. Thus, the returned
     * list may contain both {@link SWGKnownResource} and
     * {@link SWGResourceClass}.
     * 
     * @param spawning a set of spawning resources
     * @param text the name of a resource class or a planet
     * @return a list of resources and resource classes
     */
    private List<Object> writeNotesMandatory(SWGResourceSet spawning,
            final String text) {

        List<SWGResourceClass> crl;
        final boolean nonHarv = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "resourceSubmitNonHarvested", Boolean.FALSE)).booleanValue();

        if (SWGResourceClass.rcID(text) >= 0) {
            crl = SWGResourceClass.rcsFor(
                    new Comparable<SWGResourceClass>() {

                        SWGResourceClass c = SWGResourceClass.rc(text);

                        public int compareTo(SWGResourceClass o) {
                            return o.isSpawnable() && o.isSub(c)
                                    && (nonHarv || o.isHarvested())
                                    ? 0
                                    : -1;
                        }
                    });
            crl = writeNotesPlanetary(crl);
        } else { // planetary
            crl = SWGResourceClass.rcsFor(
                    new Comparable<SWGResourceClass>() {
                        SWGPlanet p = SWGPlanet.fromName(text);
                        public int compareTo(SWGResourceClass o) {
                            return (o.isSpawnable()
                                    && SWGResourceClass.canSpawnAt(o, p)
                                    && (nonHarv || o.isHarvested()))
                                            ? 0
                                            : -1;
                                }
                    });
        }

        return writeNotesMandatory(spawning, crl);
    }

    /**
     * Helper method which reduces the specified list of resource classes to
     * planetary classes. That is, all elements in the returned list are bound
     * to a planet, such as "Rori Fiberplast" and "Endor Wild Wheat".
     * 
     * @param crl a list of resource classes
     * @return a reduced list
     */
    private List<SWGResourceClass> writeNotesPlanetary(
            List<SWGResourceClass> crl) {

        List<SWGResourceClass> ret =
                new ArrayList<SWGResourceClass>(crl.size());
        for (SWGResourceClass c : crl) {
            SWGPlanet pl = planetFromResourceClass(c, true);
            if (pl != null)
                ret.add(c);
        }

        return ret;
    }

    /**
     * Helper method which returns a set of resources which are listed at
     * SWGCraft.org without stats. If no such resource exists an empty list is
     * returned.
     * 
     * @param set a set of current resources
     * @return a set of resources which are listed without stats
     */
    private List<Object> writeNotesStatless(SWGResourceSet set) {
        List<Object> ret = new ArrayList<Object>();
        for (SWGKnownResource kr : set)
            if (!kr.stats().hasValues())
                ret.add(kr);

        return ret;
    }

    /**
     * Helper method which returns a string for planets from the specified list.
     * If the list contains 3 planets or less the string contains their full
     * names, otherwise the specified boolean argument determines if the names
     * are abbreviated to fit at the GUI or not. The names are always delimited
     * with a white space, if the names are not abbreviated a comma sign is also
     * added with the delimiter.
     * 
     * @param planets a list planets
     * @param abbreviate {@code true} to abbreviate planet names if necessary
     * @return a string with planet names or abbreviations
     */
    private static String planetsToString(
            List<SWGPlanet> planets, boolean abbreviate) {

        Collections.sort(planets);
        ZString z = new ZString();
        for (SWGPlanet pl : planets) {
            if (!z.isEmpty()) z.app(", ");
            if (abbreviate && planets.size() > 3)
                z.app(pl.getName().substring(0, 3));
            else
                z.app(pl.getName());
        }
        return z.toString();
    }

    /**
     * This type is a cell renderer for the main table.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    static class MultipleResCellRenderer
            extends JLabel implements ListCellRenderer<Object> {

        /**
         * The color for depleted resources.
         */
        private final Color depleteColor;

        /**
         * The background color for erratic entries.
         */
        private final Color errorColor;

        /**
         * The border for selected cell.
         */
        private final Border selectedBorder;

        /**
         * The background color for stat-less resource objects.
         */
        private final Color statlessColor;

        /**
         * Create a cell renderer for the main table of multiple resources.
         */
        MultipleResCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEADING);

            setFont(SWGGuiUtils.fontPlain());

            errorColor = new Color(255, 223, 223);
            statlessColor = new Color(255, 255, 191);
            depleteColor = new Color(223, 223, 255);
            selectedBorder = BorderFactory.createLineBorder(Color.BLUE, 1);
        }

        
        public Component getListCellRendererComponent(JList<?> list, Object val,
                int index, boolean isSelected, boolean cellHasFocus) {

            if (isSelected)
                setBorder(selectedBorder);
            else
                setBorder(null);

            if (val == null)
                this.setText("  ");
            else if (SWGMutableResource.class.isAssignableFrom(val.getClass())) {
                SWGMutableResource mr = (SWGMutableResource) val;

                if (mr.whatever != null)
                    setBackground(((String) mr.whatever).startsWith("OK")
                            ? Color.LIGHT_GRAY
                            : errorColor /* false from the similar-dialog */);
                else if (mr.stats().sum() > 0)
                    setBackground(Color.WHITE);
                else
                    setBackground(statlessColor);

                ZString z = new ZString();
                if (mr.isDepleted() && mr.whatever != null
                        && !((String) mr.whatever).endsWith("old"))
                    z.app("depleted, ").app(mr.getName());
                else {
                    z.app(planetsToString(mr.availability(), false));
                    z.app(", ").app(mr.getName()).app(", ");
                    z.app(mr.rc().rcName()).app(", ");
                    mr.stats().toString(z, false);
                }
                setText(z.toString());
            } else {
                String str = (String) val;
                if (str.toLowerCase(Locale.ENGLISH).startsWith("depl")) {
                    String[] spl = str.split("[ ,]+");
                    if (spl.length < 2)
                        setBackground(errorColor);
                    else if (str.startsWith("DEPLETED")) {
                        // ugly hack, see submitDepleted(kr, list, i)
                        setBackground(Color.LIGHT_GRAY);
                        str = "depleted, " + spl[1];
                    } else
                        setBackground(depleteColor);
                } else
                    setBackground(errorColor);

                setText(str);
            }

            return this;
        }
    }

    /**
     * This type is the model for the main GUI list for multiple resources.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    class MultiResourceModel extends DefaultListModel<Object> {

        /**
         * A convenience dispatcher to
         * {@link DefaultListModel#fireContentsChanged(Object, int, int)}.
         */
        void fireContentsChanged() {
            super.fireContentsChanged(this, 0, getSize() - 1);
        }

        
        @Override
        public Object getElementAt(int index) {
            return (multipleResources == null
                    ? ""
                    : multipleResources.get(index));
        }

        
        @Override
        public int getSize() {
            return (multipleResources == null
                    ? 0
                    : multipleResources.size());
        }

    }

    /**
     * This type validates user input at the the planet input field. If there is
     * an error this type displays a message at the GUI error label. Otherwise
     * the planet label is updated.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    class PlanetFieldListener implements DocumentListener {

        
        public void changedUpdate(DocumentEvent e) {
            singleParsePlanets();
        }

        
        public void insertUpdate(DocumentEvent e) {
            singleParsePlanets();
        }

        
        public void removeUpdate(DocumentEvent e) {
            singleParsePlanets();
        }
    }

    /**
     * This type validates and processes input entered at the resource class
     * input field. If there is an error a message is displayed at the GUI error
     * label. Otherwise several other GUI fields are updated and {code
     * resourceClass} is updated.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    class ResourceClassFieldListener implements DocumentListener {

        public void changedUpdate(DocumentEvent e) {
            processInput();
        }

        public void insertUpdate(DocumentEvent e) {
            processInput();
        }

        /**
         * Helper method which nullifies some input fields and sets {code
         * resourceClass} to {@code null}.
         */
        
        private void nullifyFields() {
            singleResourceClassDisplay.setText("  ");
            singleResourceStatsDisplay.setText("  ");
            resourceClass = null;
        }

        /**
         * Helper method which determines if user input at the input field for
         * resource class is valid. If the input is empty this method invokes
         * {code nullifyFields()}, this is also done if the input is invalid and
         * then a message is displayed at the error label. If the input is valid
         * this method updates {code resourceClass} accordingly.
         */
        
        private void processInput() {
            String input = singleResourceClassInput.getText().trim();

            if (input.isEmpty()) { // field is emptied
                errorLabel.setText("  ");
                nullifyFields();
                return;
            }

            List<String> cls = SWGResourceClass.rcNames(input);

            if (cls.size() > 1) { // ambiguous input
                if (cls.size() > 4)
                    cls = cls.subList(0, 4); // fit the GUI labels

                String c = cls.toString();
                c = c.substring(1, c.length() - 1);
                if (cls.size() >= 4)
                    c += ", ...";
                errorLabel.setText(c);
                nullifyFields();

            } else if (cls.size() <= 0) { // invalid input
                errorLabel.setText(String.format(
                        "No resource classes for \"%s\"", input));
                nullifyFields();

            } else { // size == 1
                String cln = cls.get(0);
                resourceClass = SWGResourceClass.rc(cln);

                boolean nonHarv =
                        ((Boolean) SWGFrame.getPrefsKeeper().get(
                                "resourceSubmitNonHarvested", Boolean.FALSE))
                                .booleanValue();

                if (!nonHarv
                        && resourceClass.isSub(SWGCreatureResources.class)
                        && !resourceClass.isHarvested()) {

                    errorLabel.setText(String.format(
                            "\"%s\" is impossible to harvest naturally", cln));
                    resourceClass = null;

                } else if (!resourceClass.isSpawnable()) {
                    errorLabel.setText(String.format(
                            "\"%s\" does not spawn in the worlds or is "
                                    + "space or recycled", cln));
                    resourceClass = null;

                } else { // OK :)
                    singleResourceClassDisplay.setText(cln + ' ');
                    errorLabel.setText("  ");

                    SWGPlanet pl =
                            planetFromResourceClass(resourceClass, false);
                    if (pl != null)
                        singlePlanetInput.setText(pl.getName());

                    statsDisplayPopulate();
                    statsParse();
                }
            }
        }

        public void removeUpdate(DocumentEvent e) {
            processInput();
        }
    }

    /**
     * This type validates user input at the stats input fields. This type does
     * not deny user input but if there is an error a message is displayed at
     * the error label.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    class ResourceStatsFieldListener implements DocumentListener {

        
        public void changedUpdate(DocumentEvent e) {
            statsParse();
        }

        
        public void insertUpdate(DocumentEvent e) {
            statsParse();
        }

        
        public void removeUpdate(DocumentEvent e) {
            statsParse();
        }
    }
}
