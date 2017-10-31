package swg.gui.mail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.InvalidNameException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.sun.xml.internal.messaging.saaj.SOAPExceptionImpl;

import swg.SWGAide;
import swg.crafting.Stat;
import swg.crafting.resources.ResourceUpdate;
import swg.crafting.resources.SWGKnownResource;
import swg.crafting.resources.SWGMutableResource;
import swg.crafting.resources.SWGResource;
import swg.crafting.resources.SWGResourceClass;
import swg.crafting.resources.SWGResourceMgr;
import swg.crafting.resources.SWGResourceSet;
import swg.crafting.resources.SWGResourceStats;
import swg.crafting.resources.ResourceUpdate.UpdateType;
import swg.gui.SWGFrame;
import swg.gui.common.SWGHelp;
import swg.gui.common.SWGListCellRenderer;
import swg.gui.resources.SWGResController;
import swg.gui.resources.SWGSimilarNameDialog;
import swg.model.SWGCGalaxy;
import swg.model.SWGCharacter;
import swg.model.SWGNotes;
import swg.model.SWGPlanet;
import swg.model.SWGStation;
import swg.model.mail.SWGISDroidReport;
import swg.model.mail.SWGMailMessage;
import swg.model.mail.SWGISDroidReport.Wrapper;
import swg.swgcraft.SWGResourceManager;
import swg.swgcraft.SWGSoapAvailResResponse;
import swg.swgcraft.SWGSoapEditResResponse;
import swg.swgcraft.SWGSoapNOResResponse;
import swg.swgcraft.SWGSoapResponse;
import swg.swgcraft.SWGSoapStatusResponse;
import swg.tools.ZReader;
import swg.tools.ZString;

/**
 * A GUI component which presents the user with support for reports from
 * Interplanetary Survey Droids (ISDroids). This is the GUI element which is
 * visible at the mail panel (optional) and this type also manages the logic
 * which parses and uploads data from the reports to SWGCraft.org via {@code
 * SWGResourceManager}.
 * <P>
 * A non-trivial rewrite was done 2009/August to November due to the major
 * changes regarding resources and the introduction of the resource manager.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings( { "serial" })
public final class SWGMailISDroidPanel extends JPanel {

    /**
     * A comparator which sorts in order resource-class, resource name.
     */
    private final ClassNameComparator classNameComparator =
            new ClassNameComparator();

    /**
     * A comparator which sorts in order resource-class, resource name, planet.
     * More formally, the resource class is sorted by their sort-index.
     */
    private final ClassNamePlanetComparator classNamePlanetComparator =
            new ClassNamePlanetComparator();

    /**
     * A button for marking a resource class at a certain galaxy/planet as
     * completed, which it will be after a completed ISDroid session.
     */
    private JButton completedButton;

    /**
     * The currently displayed mail.
     */
    private SWGMailMessage currentMail = null;

    /**
     * A GUI list displaying depleted resources.
     */
    private JList<Wrapper> depletedList;

    /**
     * The list model for the GUI list for depleted resources.
     */
    private DefaultListModel<Wrapper> depletedListModel;

    /**
     * A list of depleted resources, one element per resource, sorted by
     * resource-class/name.
     */
    private List<Wrapper> depletedResources;

    /**
     * The frame for this application.
     */
    private final SWGFrame frame;

    /**
     * The URL to the help page for this object.
     */
    private final URL helpPage;

    /**
     * A flag which indicates if a background thread is working. While a
     * background thread is processing all user input is ignored.
     */
    private boolean isWorking;

    /**
     * The mail panel which contains this GUI component.
     */
    private final SWGMailPane mailPane;

    /**
     * A GUI list displaying new resources.
     */
    private JList<Wrapper> newList;

    /**
     * The list model for the GUI list of new resources.
     */
    private DefaultListModel<Wrapper> newListModel;

    /**
     * A list of resources which are new to the galaxy, one element per
     * resource@planet, sorted by resource-class/name/planet.
     */
    private List<Wrapper> newResources;

    /**
     * The name of the notes file which is used to write/read resource data
     * to/from, the file that works an mediator between SWGAide and game.
     */
    private final String notesFileName = "res.txt";

    /**
     * The GUI button for saving the contents of the two lists new-resources and
     * stat-less resources to a notes file.
     */
    private JButton notesFileSaveButton;

    /**
     * The reports to manage; if the user option single-report is true the list
     * contains only one report.
     */
    private List<SWGISDroidReport> reports;

    /**
     * A list of resource classes which the user has specified as blacklisted
     * and which must not be written to the notes file.
     */
    private List<SWGResourceClass> resourceClassBlackList;

    /**
     * A GUI list displaying stat-less resources.
     */
    private JList<Wrapper> statlessList;

    /**
     * The list model for the GUI list of stat-less resources.
     */
    private DefaultListModel<Wrapper> statlessListModel;

    /**
     * A list of stat-less resources, one element per resource, sorted by
     * resource-class/name.
     */
    private List<Wrapper> statlessResources;

    /**
     * The GUI button for submitting data parsed from the notes file. The user
     * can submit the content of {@code wrappers} right away, there is no
     * requirement to amend stats to the resource.
     */
    private JButton submitNotesFileButton;

    /**
     * A label which displays a suggestion to add stats to the resources in the
     * notes file.
     */
    private JLabel submitNotesLabel;

    /**
     * The GUI button for submitting the two lists of unreported-planets and
     * depleted-resources to SWGCraft.org. Unreported resources are those that
     * are known at SWGCraft but are not reported for one or several planets.
     */
    private JButton submitUnreportedAndDepletedButton;

    /**
     * A flag for suppressing some GUI dialogs during one batch.
     */
    private boolean suppressDialog;

    /**
     * The current/previous character.
     */
    private SWGCharacter toon;

    /**
     * A GUI list displaying unreported resources, these are known resources
     * which are not reported for one or several planets.
     */
    private JList<Wrapper> unreportedList;

    /**
     * The list model for the GUI list of unreported resources.
     */
    private DefaultListModel<Wrapper> unreportedListModel;

    /**
     * A list of unreported resources, these are known resources which are not
     * reported for one or several planets, one element per resource@planet,
     * sorted by resource-class/name/planet.
     */
    private List<Wrapper> unreportedPlanets;

    /**
     * A list of resource wrapper which are written to a notes file so that the
     * user can update the file with stats which will later be parsed and
     * uploaded to SWGCraft.org. This list is a merge of {@code newResources}
     * and {@code statlessResources} and has only one element per resource, that
     * is since when writing to file, each individual resource is complemented
     * to convey all information deduced from the different ISDroid reports, see
     * {@code mergeWriteResources()}.
     * <P>
     * Note, it is possible to submit the data to SWGCraft.org right away, after
     * the data is written to the notes file, the user is not enforced to supply
     * stats at the notes file.
     */
    private List<Wrapper> wrappers;

    /**
     * Creates an instance of this GUI component.
     * 
     * @param frame the frame for this application
     * @param mailPane the mail GUI component which contains this component
     */
    SWGMailISDroidPanel(SWGFrame frame, SWGMailPane mailPane) {
        this.frame = frame;
        this.mailPane = mailPane;

        this.setLayout(new BorderLayout());

        this.add(makeUpperRow(), BorderLayout.PAGE_START);
        this.add(makeMiddleRow(), BorderLayout.CENTER);

        helpPage = SWGAide.class.getResource("docs/help_isdroid_en.html");
    }

    /**
     * Called when the GUI button for mark-completed is selected.
     */
    private void actionCompleted() {
        // XXX: (low prio) implement
    }

    /**
     * Called from {@link SWGMailPane#updateMailBody()} when the user selects
     * the ISDroid folder at the mail panel or a specific mail in that folder.
     * When the user selects the ISDroid folder a mail is automatically selected
     * to initiate this GUI element to be populated.
     * <P>
     * This implementation does nothing if there is a background process in
     * progress, or if the argument is identical to the previous argument. If
     * the argument is {@code null} and the user has selected another character
     * this implementation resets this component and returns. Otherwise this
     * implementation continues to validate either the one argument or all mails
     * listed at the ISDroid mail folder (determined by the option for single or
     * multiple), see {@code handleOldMails(List)}. So far, unless the user has
     * not aborted the process a background job invokes {@code
     * handleISDroidReport(SWGMailMessage)};
     * 
     * @param mail the mail to process, assumed to be an ISDroid report
     * @param row the row number for the specified mail at the mail list model,
     *        or {@code -1} if no row is selected
     */
    void actionHandleISDroidReport(final SWGMailMessage mail, final int row) {
        if (isWorking) return;

        if (mail == null
                || (mailPane.toon == this.toon && mailPane.mails().size() <= 0)
                || !mailPane.toon.galaxy().exists()) {
            resetISDroid();
            return;
        } else if (mail == currentMail) { return; // no recomp necessary
        }

        isWorking = true;
        this.toon = mailPane.toon;
        currentMail = mail;

        final boolean singleReport = ((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidSingleSelection", Boolean.FALSE)).booleanValue();
        if (singleReport) {
            // single reports only
            List<SWGMailMessage> lst = new ArrayList<SWGMailMessage>(1);
            lst.add(mail);
            if (!handleOldMails(lst)) {
                toon = null;
                currentMail = null;
                isWorking = false;
                return;
            }
            mailPane.mailList.setEnabled(true);
            mailPane.mailList.getSelectionModel()
                    .setSelectionInterval(row, row);
        } else {
            if (!handleOldMails(mailPane.mails())) {
                toon = null;
                currentMail = null;
                isWorking = false;
                return;
            }
            mailPane.mailList.selectAll();
        }

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {

            @SuppressWarnings("synthetic-access")
            public void run() {
                handleISDroidReport(mail, singleReport);
                handleISDroidReportDone();
                executor.shutdown();
            }
        });

        mailPane.mailList.setEnabled(false);
        submitUnreportedAndDepletedButton.setEnabled(false);
        notesFileSaveButton.setEnabled(false);
        submitNotesFileButton.setEnabled(false);

    }

    /**
     * Called when the user selects to submit data from notes file to
     * SWGCraft.org. See {@code notesFileRead(SWGNotes)} and {@code
     * notesFileSubmitResources(SWGNotes, List)}.
     * <P>
     * If there is a background process this method does nothing.
     */
    private void actionSubmitNotesFile() {
        if (isWorking)
            return;
        SWGNotes notes = getNotes(notesFileName);
        if (notes == null || !notes.exists()) {
            JOptionPane.showMessageDialog(submitNotesFileButton,
                    "There is no notes file: \"" + notesFileName + '\"',
                    "Missing file", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            boolean autoDelete = notesFileRead(notes);
            notesFileSubmitResources(notes, wrappers, autoDelete);
        } catch (NullPointerException e) {
            printLog("actionSubmitNotesFile"
                    + Arrays.toString(e.getStackTrace()), null);

            JOptionPane.showMessageDialog(submitNotesFileButton,
                    "There is an error, see log file", "Null pointer error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Throwable e) {
            if (!e.getMessage().equals("ABORT"))
                printLog("actionSubmitNotesFile", e);
        }
    }

    /**
     * Called when the user selects to submit resources which are depleted and
     * resource which are not reported to SWGCraft.org for some planets. This
     * method invokes {@code submitUnreportedAndDepleted(List, List)} on a
     * background thread and when the task is finished it notifies resource
     * update subscribers.
     * <P>
     * If another background thread is processing this method does nothing.
     */
    private void actionSubmitUnreportedAndDepleted() {
        if (isWorking)
            return;
        isWorking = true;
        submitUnreportedAndDepletedButton.setEnabled(false);
        notesFileSaveButton.setEnabled(false);

        final List<Wrapper> urep = unreportedPlanets;
        final List<Wrapper> depl = depletedResources;

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                try {
                    submitUnreportedAndDepleted(urep, depl);
                    notifyUpdateSubscribers(depl.size() > 0
                            ? depl.get(0).report.gxy()
                            : urep.size() > 0
                                    ? urep.get(0).report.gxy()
                                    : null);
                } catch (SOAPExceptionImpl e) {
                    showSoapExceptionDialog();
                } finally {
                    resetGUI();
                    enableButtons();
                }
                executor.shutdown();
            }
        });
    }

    /**
     * Called when the user selects to save resources to notes file. This method
     * obtains a notes file or if the file does not exist it is created, and
     * invokes {@code notesFileWrite(File)}. This method also enables and
     * disables GUI elements determined by the outcome of the task. If there is
     * a background process, or if there is an error, this method does nothing.
     */
    private void actionWriteNotesFile() {
        if (isWorking)
            return;
        SWGNotes notes = getNotes(notesFileName);

        if (notes == null) {
            SWGStation station = getStation();
            notes = SWGNotes.getInstance(notesFileName, station, null, true);
            if (notes == null) {
                JOptionPane.showMessageDialog(notesFileSaveButton,
                        "Error creating \"" + notesFileName + "\"", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        notesFileWriteCheckEmpty(notes);
        notesFileWrite(notes);
        notesFileSaveButton.setEnabled(false);
        submitNotesLabel.setForeground(Color.BLACK);
        submitNotesFileButton.setEnabled(true);
    }

    /**
     * Helper method which enables the buttons determined by the content of the
     * four local collections which are filled as a side effect of populating
     * the GUI lists. That is, if there is content in the GUI lists the
     * corresponding buttons are enabled, otherwise not.
     */
    private void enableButtons() {
        SwingUtilities.invokeLater(new Runnable() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                if ((depletedResources != null && depletedResources.size() > 0)
                        || (unreportedPlanets != null && unreportedPlanets.size() > 0))
                    submitUnreportedAndDepletedButton.setEnabled(true);

                if ((newResources != null && newResources.size() > 0)
                        || (statlessResources != null && statlessResources.size() > 0))
                    notesFileSaveButton.setEnabled(true);
            }
        });
    }

    /**
     * Sets this GUI component in focused or unfocused mode and updates GUI
     * sub-components accordingly to reflect the current state; GUI components
     * such as menu items etc.
     * 
     * @param selected {@code true} to set the state of this component to
     *        focused, {@code false} otherwise
     */
    void focusGained(boolean selected) {
        if (selected) {
            SWGHelp.push(helpPage);
        } else {
            SWGHelp.remove(helpPage);
            mailPane.mailList.setEnabled(true);
        }
    }

    /**
     * Helper method which returns a set of resources for the galaxy specified
     * in the argument. This method determines if a a set of resources no older
     * than 45 minutes is available and returns that set. Otherwise this method
     * notifies the user via a dialog and returns {@code null}.
     * <p>
     * Unless {@code null} is returned the returned set contains all cached
     * resources for the galaxy, including depleted resources.
     * 
     * @param mail an ISDroid report from which a galaxy is parsed
     * @return a set of resources, or {@code null}
     */
    private SWGResourceSet known(final SWGMailMessage mail) {
        // use getSet() if some resources are wrongly "depleted"
        // SWGResourceSet known =
        // SWGResourceManager.getSet(mail.getFromGalaxy());

        // FIXME: clean up before publish
        // half an hour back, SWGCraft updates every 30 minutes

        // 45 minutes, if older something is wrong
        final long now = (System.currentTimeMillis() / 1000) - 2700;
        Long swg = SWGResourceManager.getStatusLocalTime(mail.owner().gxy());

        if (swg.longValue() <= now) {
            String[] opt = { "OK", "Cancel", "Help" };
            int ret = JOptionPane.showOptionDialog(SWGAide.frame(),
                    "Recent download is older than 45 minutes\n\n"
                            + "Wait until download is ready, then OK\n"
                            + "If download seems stuck, press Help",
                            "Aged download", JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, opt, opt[0]);
            if (ret == 0) {
                // pass, and use available resource-set
            } else {
                if (ret == 2) {
                    String m = "If the recent download is older than 45, "
                            + "check the following:\n"
                            + "1) At menu Options => SWGCraft, select a main"
                            + "galaxy and enable auto-download.\n"
                            + "2) Visit the Resources panel to trigger a "
                            + "status check and a download.\n"
                            + "3) Delete the file \\temp\\...-status.txt and "
                            + "visit the Resources panel.\n"
                            + "4) Restart SWGAide and verify that it fetches "
                            + "a new download.\n"
                            + "5) If this issue does not resolve, report to "
                            + "Zimoon, include as much details as possible";
                    JOptionPane.showMessageDialog(SWGAide.frame(), m,
                            "Aged download, Info", JOptionPane.PLAIN_MESSAGE);
                }
                return null;
            }
        }

        // if (swg >= now)
        // return known;
        //
        // // we must have an updated set of current resources, thus we must
        // make
        // // sure we have a recent download, or wait for one.
        // // We are on an executor thread and can afford to sleep.
        // int timeOut = 0;
        // while (swg < now && timeOut <= 60) {
        // // try for at most one minute
        // frame.putToStatbar(String.format(
        // "<html><font color=\"%s\">"
        // + "ISDroid: waiting for download (%s / 60)"
        // + "</font></html>", timeOut > 20
        // ? "Red"
        // : "#ff8000",
        // Integer.toString(timeOut)));
        //
        // try {
        // Thread.sleep(1000L);
        // } catch (InterruptedException e) { /* ignore */
        // }
        //
        // swg = SWGResourceManager.getStatusLocalTime(
        // mail.getFromGalaxy()).longValue();
        //
        // SWGAide.printDebug("isdr", 1,
        // "SWGISDroidPanel:obtainFreshestDownload: s=" + swg);
        //
        // ++timeOut;
        // }
        //
        // if (timeOut > 60) {
        // String m = "Could not download from SWGCraft.org in one minute.\n"
        // + "Verify if current is updated; if not, contact Zimoon.";
        // JOptionPane.showMessageDialog(
        // frame, m, "Timeout", JOptionPane.ERROR_MESSAGE);
        // }
        //
        // frame.putToStatbar(null);

        return SWGResourceManager.getSet(mail.owner().gxy());
    }

    /**
     * Helper method which returns a notes object specified by {@code fileName}
     * for the current station, {@code null} if none was found.
     * 
     * @param fileName the file name for the notes object to return
     * @return a notes object with {@code fileName}, or {@code null}
     */
    private SWGNotes getNotes(String fileName) {
        return getStation().notes(fileName);
    }

    /**
     * Helper method that returns the SWG station for the currently selected
     * character.
     * 
     * @return the station for the currently selected character
     */
    private SWGStation getStation() {
        return mailPane.getCharacter().galaxy().station();
    }

    /**
     * Helper method which invokes other helper methods that populate the GUI
     * lists new, depleted, stat-less, and amend-planets. This method invokes
     * the populate-X methods one by one and also {@link #enableButtons()}.
     * 
     * @param known a set of known resources for the galaxy, also depleted
     */
    private void handleISDroidPopulating(SWGResourceSet known) {
        populateDepletedList(known);
        populateUnreportedPlanetList();
        populateNewList();
        populateStatlessList();
        enableButtons();
    }

    /**
     * Helper method which runs on an executor thread. This method obtains
     * resources which are currently listed as in-spawn at SWGCraft.org. From
     * that collection of resources this method invokes helper methods to
     * <ol>
     * <li>build sub-collections of the four kinds: new, depleted, stat-less,
     * and amend-planets</li>
     * <li>populate the four GUI elements</li>
     * </ol>
     * 
     * @param mail the selected mail to handle, or one of them
     * @param singleReport {@code true} if the user has opted for single report
     */
    private void handleISDroidReport(SWGMailMessage mail, boolean singleReport) {
        try {
            final SWGResourceSet known = known(mail);
            if (known == null) return; // aged download

            reports = handleReportMerging(mail, singleReport, known);

            if (reports.size() > 0)
                handleISDroidPopulating(known);

        } catch (InvalidNameException e) {
            JOptionPane.showMessageDialog(
                    frame,
                    "There is a resource name with non-alphabetical letters,\n"
                            + "try to disable the in-game profanity filter, mail-save\n"
                            + "again and restart SWGAide. The error: \n"
                            + e.getMessage(),
                    "ISDroid preparation error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "There is a problem preparing the ISDroid reports\n"
                            + "See the log file", "ISDroid preparation error",
                    JOptionPane.ERROR_MESSAGE);
            printLog("handleISDroidReport", e);
        }
    }

    /**
     * Helper method which is called when parsing ISdroid reports is finished.
     * This is when the GUI is populated and the internal collections of
     * resources for the different GUI elements are updated.
     */
    private void handleISDroidReportDone() {
        SwingUtilities.invokeLater(new Runnable() {

            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                mailPane.mailList.setEnabled(
                        ((Boolean) SWGFrame.getPrefsKeeper().get(
                                "ISDroidSingleSelection", Boolean.FALSE)).booleanValue());
                frame.setCursor(
                        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                isWorking = false;
            }
        });
    }

    /**
     * Helper method which scans all mails. Old mails displays a dialog with the
     * options to keep the mail, to delete them one by one, or to delete all old
     * mails.
     * 
     * @param mails the list of mails to scan
     * @return {@code false} if the user closed the dialog and did not select an
     *         option
     */
    private boolean handleOldMails(List<SWGMailMessage> mails) {
        boolean deleteAllOld = false;
        for (int i = 0, j = 0; i < mails.size(); ++i, ++j) {
            SWGMailMessage mail = mails.get(i);
            if (mails.size() != 1)
                mailPane.mailList.getSelectionModel()
                        .setSelectionInterval(j, j);

            if (!SWGISDroidReport.isNew(mail)) {
                String msg = String.format(
                        "This ISDroid report is older than %s hours!%n"
                                + "Are you sure you want to use it?",
                        Long.toString(SWGISDroidReport.MAX_AGE_HOURS));
                int response = 0;
                if (!deleteAllOld && mails.size() <= 1) {
                    Object[] options = { "Yes", "Delete report" };
                    response =
                            JOptionPane.showOptionDialog(mailPane.folderList,
                                    msg, "Overaged report",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE, null, options,
                                    options[1]);
                } else if (!deleteAllOld) {
                    Object[] options =
                        { "Yes", "Delete report", "Delete all old" };
                    response =
                            JOptionPane.showOptionDialog(mailPane.folderList,
                                    msg, "Overaged report",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE, null, options,
                                    options[2]);
                    if (response == 2)
                        deleteAllOld = true;
                }
                if (response == JOptionPane.CLOSED_OPTION)
                    return false;
                if (response != 0 || deleteAllOld) {
                    mailPane.mailDelete(false);
                    --j;
                }
            }
        }
        return true;
    }

    /**
     * Helper method which adds an ISDroid report parsed from {@code mail} to
     * the specified list. If the mail is not an ISDroid report this method
     * Writes the error to SWGAide's log file and does not add anything.
     * 
     * @param mail the mail to parse for an ISDroid report
     * @param rls the list to add the ISDroid report
     * @param known a set of known resources for the current galaxy
     * @throws IllegalArgumentException if the mail is not an ISDroid report or
     *         if there is an invalid argument or a parsed error
     * @throws IllegalStateException if there is an error parsing the mail body
     * @throws InvalidNameException if a name contains illegal characters
     * @throws NullPointerException if there is a {@code null} argument
     */
    private void handleReportAdding(SWGMailMessage mail,
            List<SWGISDroidReport> rls, SWGResourceSet known) throws
            IllegalArgumentException, IllegalStateException,
            InvalidNameException {

        SWGISDroidReport isd = mail.isdroidData(known);
        if (isd != null)
            rls.add(isd);
        else
            printLog(String.format("handleReportMerging: mail=%s%n%s",
                    mail.toString(), mail.bodyText()),
                    new NullPointerException("Error in ISDroid data"));
    }

    /**
     * Helper method which returns a list of ISDroid reports for further
     * processing. If the user has opted for single-report the list contains
     * only one report. Otherwise this method fetches all mails from the
     * currently selected ISDroid mail folder which are added to the returned
     * list.
     * 
     * @param mail a mail to parse for a report from, this argument is ignored
     *        if {@code singleReport} is {@code false}
     * @param singleReport {@code true} if the user has opted for single report
     * @param known a set of known resources for the current galaxy
     * @return a list of ISDroid reports to process
     * @throws IllegalArgumentException if the mail is not an ISDroid report or
     *         if there is an invalid argument or parsed
     * @throws IllegalStateException if there is an error parsing the mail body
     * @throws InvalidNameException if a resource name contains illegal
     *         characters
     * @throws NullPointerException if there is a {@code null} argument or some
     *         other error
     */
    private List<SWGISDroidReport> handleReportMerging(
            SWGMailMessage mail, boolean singleReport, SWGResourceSet known)
            throws IllegalArgumentException, IllegalStateException,
            InvalidNameException {

        List<SWGISDroidReport> rls = new ArrayList<SWGISDroidReport>();

        if (singleReport)
            handleReportAdding(mail, rls, known);
        else
            for (SWGMailMessage m : mailPane.mails())
                handleReportAdding(m, rls, known);

        return rls;
    }

    /**
     * Returns a GUI button for the mark completed logic.
     * 
     * @return a GUI button
     */
    @SuppressWarnings("unused")
    private JButton makeCompletedButton() {
        // XXX implement
        completedButton = new JButton("Mark completed");
        completedButton
                .setToolTipText("Marks resource classes as completed at SWGCraft");
        completedButton.setEnabled(false);
        completedButton.addActionListener(new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionCompleted();
            }
        });
        return completedButton;
    }

    /**
     * Creates and returns a GUI component for the list of depleted resources.
     * 
     * @return a GUI component
     */
    private JPanel makeDepletedList() {
        depletedListModel = new DefaultListModel<Wrapper>();
        depletedList = new JList<Wrapper>(depletedListModel);
        depletedList.setEnabled(false);
        depletedList.setToolTipText("Depleted resources at this galaxy");
        depletedList.setCellRenderer(new ListRenderer());
        JScrollPane jsp = new JScrollPane(depletedList);
        jsp.setPreferredSize(new Dimension(257, 130));
        JPanel dPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dPanel.setPreferredSize(new Dimension(275, 165));
        dPanel.setBorder(BorderFactory
                .createTitledBorder("2 - Depleted resources"));
        dPanel.add(jsp);
        return dPanel;
    }

    /**
     * Creates and returns the middle GUI component for the ISDroid panel.
     * 
     * @return the middle GUI component
     */
    private Component makeMiddleRow() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEtchedBorder());

        panel.add(makeNewAtGalaxyList());
        panel.add(makeStatlessList());

        panel.add(makeNotesButtonsPanel());

        return panel;
    }

    /**
     * Creates and returns a GUI component for the list of new resources.
     * 
     * @return a GUI component
     */
    private Component makeNewAtGalaxyList() {
        newListModel = new DefaultListModel<Wrapper>();
        newList = new JList<Wrapper>(newListModel);
        newList.setEnabled(false);
        newList.setToolTipText("New resources at both planet and galaxy");
        newList.setCellRenderer(new ListRenderer());
        JScrollPane jsp = new JScrollPane(newList);
        jsp.setPreferredSize(new Dimension(257, 130));
        JPanel nPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nPanel.setPreferredSize(new Dimension(275, 165));
        nPanel.setBorder(BorderFactory
                .createTitledBorder("3 - New resources at galaxy & planet"));
        nPanel.add(jsp);
        return nPanel;
    }

    /**
     * Creates and returns a panel for the buttons related to new resources and
     * stat-less resources.
     * 
     * @return a GUI component
     */
    private JPanel makeNotesButtonsPanel() {
        JPanel bPanel = new JPanel();
        bPanel.setLayout(new BoxLayout(bPanel, BoxLayout.Y_AXIS));

        notesFileSaveButton = new JButton("Write to file");
        notesFileSaveButton
                .setToolTipText("Write resources at lists 3 and 4 to notes file \""
                        + notesFileName + '\"');
        notesFileSaveButton.addActionListener(new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionWriteNotesFile();
            }
        });
        notesFileSaveButton.setEnabled(false);
        bPanel.add(notesFileSaveButton);

        bPanel.add(Box.createVerticalStrut(5));

        submitNotesLabel = new JLabel("<html>Add stats to resources<br/>"
                + "at the in-game notepad<br/>(press F1 for help)</html>");
        bPanel.add(submitNotesLabel);

        bPanel.add(Box.createVerticalStrut(5));

        submitNotesFileButton = new JButton("Submit");
        submitNotesFileButton.setToolTipText("Submit data from \""
                + notesFileName + "\" to SWGCraft");
        submitNotesFileButton.addActionListener(new ActionListener() {

            @SuppressWarnings("synthetic-access")
            public void actionPerformed(ActionEvent e) {
                actionSubmitNotesFile();
            }
        });
        submitNotesFileButton.setEnabled(false);
        bPanel.add(submitNotesFileButton);

        return bPanel;
    }

    /**
     * Creates and returns a GUI component for the list of stat-less resources.
     * 
     * @return a GUI component
     */
    private Component makeStatlessList() {
        statlessListModel = new DefaultListModel<Wrapper>();
        statlessList = new JList<Wrapper>(statlessListModel);
        statlessList.setEnabled(false);
        statlessList
                .setToolTipText("Resources previously reported without stats");
        statlessList.setCellRenderer(new ListRenderer());
        JScrollPane jsp = new JScrollPane(statlessList);
        jsp.setPreferredSize(new Dimension(257, 130));
        JPanel sPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sPanel.setPreferredSize(new Dimension(275, 165));
        sPanel.setBorder(BorderFactory
                .createTitledBorder("4 - Resources without stats"));
        sPanel.add(jsp);
        return sPanel;
    }

    /**
     * Creates and returns the GUI button for submitting unreported and depleted
     * resources to SWGCraft.
     */
    private void makeSubmitUnreportedAndDepletedButton() {
        submitUnreportedAndDepletedButton =
                new JButton("<html><center>Submit<p>1 &amp; 2</center>");
        submitUnreportedAndDepletedButton
                .setToolTipText("Submit all resources at lists 1 and 2 to SWGCraft");
        submitUnreportedAndDepletedButton.setEnabled(false);
        submitUnreportedAndDepletedButton
                .addActionListener(new ActionListener() {

                    @SuppressWarnings("synthetic-access")
                    public void actionPerformed(ActionEvent e) {
                        actionSubmitUnreportedAndDepleted();
                    }
                });
    }

    /**
     * Creates and returns a GUI component for the list of unreported resources.
     * 
     * @return a GUI component
     */
    private JPanel makeUnreportedList() {
        unreportedListModel = new DefaultListModel<Wrapper>();
        unreportedList = new JList<Wrapper>(unreportedListModel);
        unreportedList.setEnabled(false);
        unreportedList
                .setToolTipText("Resources known at galaxy but yet not reported at this planet");
        unreportedList.setCellRenderer(new ListRenderer());
        JScrollPane jsp = new JScrollPane(unreportedList);
        jsp.setPreferredSize(new Dimension(257, 130));
        JPanel nPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nPanel.setPreferredSize(new Dimension(275, 165));
        nPanel.setBorder(BorderFactory
                .createTitledBorder("1 - Known at galaxy, unreported at planet"));
        nPanel.add(jsp);
        return nPanel;
    }

    /**
     * Creates and returns the upper GUI component for the ISDroid panel.
     * 
     * @return the upper GUI component
     */
    private Component makeUpperRow() {
        JPanel middle = new JPanel();
        middle.setLayout(new FlowLayout(FlowLayout.LEFT));
        middle.setBorder(BorderFactory.createEtchedBorder());

        middle.add(makeUnreportedList());
        middle.add(makeDepletedList());

        makeSubmitUnreportedAndDepletedButton();
        middle.add(submitUnreportedAndDepletedButton);

        return middle;
    }

    /**
     * Helper method which returns a list of all resources deemed available from
     * the current collection of ISDroid reports. There is only one element per
     * available resource in the returned list, however an arbitrary wrapper
     * from any one of the current reports.
     * <P>
     * <B>Note: </B>If the ISDroid reports do not cover all ten core planets the
     * returned list is most probably incomplete, an issue the user must care
     * for.
     * 
     * @return a list of available resources determined from the current reports
     */
    private List<Wrapper> mergeAllAvailableResources() {
        List<Wrapper> all =
                new ArrayList<Wrapper>(SWGResourceMgr.COLLECTION_SIZE);

        for (SWGISDroidReport d : reports) {
            Wrapper[] dl = d.available();
            for (Wrapper wr : dl)
                if (!all.contains(wr)) all.add(wr);
        }

        return all;
    }

    /**
     * Helper method which returns a sorted list of depleted resources for the
     * galaxy the current ISDroid reports pertain to. The sort order is
     * resource-class/name and the list contains just one entry per resource.
     * <P>
     * <B>Note: </B>If a resource is determined to be depleted from one report
     * but is available in another report it is not added to the returned list.
     * This is in the case a user at SWGCraft.org once wrongly marked a resource
     * as available for a planet which makes it into the set of known resources
     * for the galaxy. However, an ISDroid report for that planet does not list
     * it, which is correct since the user was wrong; hence it is deduced as
     * "depleted". This safety precaution <i>will still fail </i> if the number
     * of ISDroids do not cover enough planets.
     * 
     * @param known a set of known resources for the galaxy, also depleted
     * @return a list of depleted resources for the galaxy
     * @throws NullPointerException if the argument is {@code null}
     */
    private List<Wrapper> mergeDepletedResources(SWGResourceSet known) {

        // used to determine if a resource exists at any planet
        List<Wrapper> available = mergeAllAvailableResources();

        SWGResourceSet inSpawn = known.subsetBy(
                new Comparable<SWGKnownResource>() {
                    @Override
                    public int compareTo(SWGKnownResource o) {
                        return o.isDepleted()
                                ? 1
                                : 0;
                    }
                });

        List<Wrapper> depl = new ArrayList<Wrapper>(32);
        for (SWGISDroidReport r : reports) {
            List<Wrapper> dl = r.depleted(inSpawn);
            for (Wrapper wr : dl)
                // safety: if exists in ANY ISDroid report >>> it cannot be
                // depleted; ONE is enough
                if (!depl.contains(wr) && !available.contains(wr))
                    depl.add(wr);
        }

        Collections.sort(depl, classNameComparator);
        return depl;
    }

    /**
     * Helper method which returns a sorted list of resources which are new to
     * the galaxy. The returned list contains one element per resource/planet;
     * if a particular resource is found at four planets there are four
     * elements. The sort order is resource-class/name/planet.
     * 
     * @return a sorted list of resources which are new to the galaxy
     */
    private List<Wrapper> mergeNewResources() {
        List<Wrapper> news = new ArrayList<Wrapper>();
        for (SWGISDroidReport r : reports) {
            List<Wrapper> nl = r.news();
            for (Wrapper wr : nl)
                news.add(wr);
        }

        Collections.sort(news, classNamePlanetComparator);
        return news;
    }

    /**
     * Helper method which returns a sorted list of known resources which are
     * listed at SWGCraft.org without stats. In particular, each element of the
     * returned list has an ID but no stats and the list contains just one
     * element per resource. The sort order is class/name.
     * 
     * @return a sorted list of resources without stats
     */
    private List<Wrapper> mergeStatlessResources() {
        List<Wrapper> sls = new ArrayList<Wrapper>();
        for (SWGISDroidReport r : reports) {
            List<Wrapper> sl = r.statless();
            for (Wrapper wr : sl)
                if (!sls.contains(wr)) sls.add(wr);
        }

        Collections.sort(sls, classNameComparator);
        return sls;
    }

    /**
     * Helper method which returns sorted list of resources which can be
     * complemented with one or more planets, or an empty list. The elements are
     * resources that are listed at SWGCraft.org but one or several planets are
     * missing. The list is deduced from the current ISDroid reports and
     * contains one element per unreported resource-plus-planet; if a particular
     * resource is missing for two planets two elements exist for that resource.
     * The sort order is resource-class/name/planet.
     * 
     * @return a sorted list of resource-planet pairs, or an empty list
     */
    private List<Wrapper> mergeUnreportedPlanets() {
        List<Wrapper> upl = new ArrayList<Wrapper>();
        for (SWGISDroidReport r : reports)
            for (Wrapper wr : r.unreportedForPlanet())
                upl.add(wr);

        Collections.sort(upl, classNamePlanetComparator);
        return upl;
    }

    /**
     * Helper method which returns a list of new and stat-less resources. The
     * returned list is a merge of the two collections {@code newResources} and
     * {@code statlessResources}, merged so it contains only one element per
     * distinct resource and that each element's mutable resource object reads
     * one or several planets determined by the reports. The list is sorted by
     * resource-class/name.
     * 
     * @return a merged and sorted list of new and stat-less resources
     */
    private List<Wrapper> mergeWriteResources() {
        ArrayList<Wrapper> wrList =
                new ArrayList<Wrapper>(newResources.size());

        // newResources may have several elements per resource, it is already
        // sorted by class/name/planet so comparing resource names will work.
        //
        // unify resources so only one object is used for any identical resource
        // found at different planets
        SWGMutableResource prev = null;
        for (Wrapper wr : newResources) {
            if (prev == null || !wr.mutable.getName().equals(prev.getName())) {
                prev = wr.mutable;
                wrList.add(wr);
            } else
                prev.availability(
                        wr.mutable.availabilityFor(wr.report.planet()));
        }

        // statlessResources has just one element per resource and none is
        // listed in newResource
        wrList.addAll(statlessResources);

        Collections.sort(wrList, classNameComparator);
        return wrList;
    }

    /**
     * Helper method which erases the content if the specified notes file if the
     * user options and if the two boolean arguments are {@code true}. If the
     * auto-delete option is set to {@code false} or if {@code autoDelete} is
     * {@code false} this method does nothing. If both those are {@code true}
     * and {@code success} is {@code false} the user is asked to accept or not.
     * 
     * @param notes the notes file for which to erase its content
     * @param success {@code true} uploading was successful, {@code false}
     *        otherwise
     * @param autoDelete {@code false} if the notes file is not allowed to be
     *        auto-deleted after submission
     */
    private void notesFileEraseNotes(
            final SWGNotes notes, boolean success, boolean autoDelete) {

        boolean auto =
                ((Boolean) SWGFrame.getPrefsKeeper().get(
                        "ISDroidFileAutoDelete",
                        Boolean.TRUE)).booleanValue();
        if (autoDelete && auto) {
            if (!success
                    && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                            submitNotesFileButton,
                            "An issue happened during upload!\n\nDelete file \""
                                    + notesFileName + "\" anyway?",
                            "Confirm file delete",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE)) { return; }
            notes.erase(false);
        }
    }

    /**
     * Helper method which determines if the specified time is older than the
     * time specified by {@link SWGISDroidReport#MAX_AGE_HOURS}. If the time is
     * overaged this method presents the user with a GUI dialog with the options
     * to continue anyway or to abort.
     * <P>
     * This implementation returns {@code false} if the execution can continue.
     * Otherwise, if the time is overaged and the user <I>does not&nbsp;</I>
     * select "Yes" this method returns {@code true} and the task must abort.
     * 
     * @param headerLine the line the time is parsed from
     * @param time the time in seconds
     * @return {@code true} if the specified time is overaged and execution must
     *         be aborted, {@code false} otherwise
     */
    private boolean notesFileIsOveraged(String headerLine, long time) {
        long time2 = (time + (SWGISDroidReport.MAX_AGE_HOURS * 3600)) * 1000;
        long sctm = System.currentTimeMillis();
        if (time2 < sctm) {
            String m = String.format(
                    "ISDroid report is older than %s hours!%n\"%s\"%n%s%n%n" +
                            "Do you want to continue anyway?",
                    Long.toString(SWGISDroidReport.MAX_AGE_HOURS),
                    headerLine, (new Date(time * 1000)).toString());
            return JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                    submitNotesFileButton, m, "Overaged report",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }
        return false;
    }

    /**
     * Helper method which reads the specified notes file and updates the
     * elements in {@code wrappers}. If the user has not added data to the notes
     * file it is still parsed but with no effect.
     * 
     * @param notes the notes object to parse
     * @return {@code false} if there is an error parsing the notes file and if
     *         the user selected not to auto-delete the notes file after
     *         uploading
     * @throws RuntimeException if there is an error in the notes file or if
     *         there is an error in the data and the user selects to abort
     */
    private boolean notesFileRead(SWGNotes notes) {
        boolean success;
        if (((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidFileContinuous", Boolean.FALSE)).booleanValue())
            success = notesFileReadContinuous(notes);
        else
            success = notesFileReadPlanets(notes);

        if (!success
                && ((Boolean) SWGFrame.getPrefsKeeper().get(
                        "ISDroidFileAutoDelete", Boolean.TRUE)).booleanValue()
                && !notes.content().isEmpty()
                && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                        submitNotesFileButton,
                        String.format(
                                "There was an issue while parsing the notes file%n"
                                        + "Auto-delete \"%s\" after submitting?",
                                notesFileName),
                        "Confirm", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE))
            return false;

        return true;
    }

    /**
     * Helper method which parses a continuous list of resources from the
     * specified notes file. If there is nothing to parse or if there is an
     * error {@code false} is returned. This method skips initial comments and
     * invokes {@code notesFileReadCont(SWGNotes, int)}.
     * 
     * @param notes the notes file to parse
     * @return {@code true} if success, {@code false} if there is nothing to
     *         parse or if there is an error
     * @throws RuntimeException if the user wants to abort
     */
    private boolean notesFileReadContinuous(SWGNotes notes) {
        int row = 0;
        String line = null;

        List<String> nl = notes.lines();
        if (nl == null || nl.isEmpty()) return false;

        for (; row < nl.size(); ++row) {
            line = nl.get(row).trim();
            if (!line.isEmpty() && !line.startsWith("#"))
                break; // should be the header line
        }

        if (row >= nl.size()) return false;

        // parse header line
        long time = notesFileReadTimeLine(line);

        if (notesFileIsOveraged(line, time)) return false;

        for (++row; row < nl.size(); ++row) {
            line = nl.get(row).trim();
            String errorMsg;
            while ((errorMsg = notesFileReadResource(line)) != null)
                line = notesFileRectifyLine(line, errorMsg);
        }

        return true;
    }

    /**
     * Helper method which reads a block of resources from the list. This method
     * begins at the specified row in the range delimited by "swgcraft_start"
     * and "swgcraft_end". The return value contains the row number for the row
     * that is parsed most recently, and a status value. If nothing is parsed
     * this method returns successfully.
     * 
     * @param tl a list of strings, from a notes file
     * @param begin the index to begin at
     * @return a pair of integers, the indexes are
     *         <ul>
     *         <li>0 &mdash; the line number for the most recent line, or -1 if
     *         the final string of a block is not found</li>
     *         <li>1 &mdash; status
     *         <ul>
     *         <li>&gt; 0 is success</li>
     *         <li>&le; 0 is an error, the task must abort</li></li>
     *         </ul>
     *         </ul>
     * @throws RuntimeException if there is an error or if the user selects to
     *         abort
     */
    private int[] notesFileReadPlanet(List<String> tl, int begin) {
        assert begin >= 0 : "Illegal value " + begin;

        int[] returnValue = { begin, 1 };
        String line = "";
        int row = begin;

        for (; row < tl.size(); ++row) {
            line = tl.get(row).trim();
            if (line.startsWith("#")) return returnValue;
            if (!line.isEmpty()) break;

            returnValue[0] = row;
        }
        if (row >= tl.size()) return returnValue;

        if (notesFileIsOveraged(line, notesFileReadTimeLine(line))) {
            returnValue[1] = 0; // abort
            return returnValue;
        }

        for (; row < tl.size(); ++row)
            if (tl.get(row).trim().startsWith("swgcraft_start")) break;

        for (++row; row < tl.size(); ++row) {
            line = tl.get(row).trim();
            if (line.startsWith("swgcraft_end")) break;

            String errorMsg;
            while ((errorMsg = notesFileReadResource(line)) != null)
                line = notesFileRectifyLine(line, errorMsg);
        }
        returnValue[0] = line.startsWith("swgcraft_end")
                ? row
                : -1;
        return returnValue;
    }

    /**
     * Helper method which parses the notes file and invokes the helper method
     * planet by planet.
     * 
     * @param notes the notes object to parse
     * @return {@code true} if successful, {@code false} otherwise
     * @throws RuntimeException if there is an error and the user selects to
     *         abort
     */
    private boolean notesFileReadPlanets(SWGNotes notes) {
        boolean success = true;
        int row = 0;
        String line;

        List<String> tl = notes.lines();
        if (tl == null || tl.isEmpty()) return false;

        for (; row < tl.size(); ++row) {
            line = tl.get(row).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // status is [lineNumber, status]
            int[] status = notesFileReadPlanet(tl, row);
            if (status[1] <= 0) return false;

            row = status[0] + 1;
        }
        return success;
    }

    /**
     * Helper method which parses the specified line and updates the new or
     * stat-less resource which the line pertains to. This method returns
     * {@code null} if the operation is successful or if the specified line was
     * an expected information line. Otherwise an error message is returned.
     * 
     * @param resourceLine a line from the notes file which reads a resource
     *        name, resource class, and possibly user added stats
     * @return {@code null} if successful, otherwise an error message
     */
    private String notesFileReadResource(String resourceLine) {
        if (resourceLine == null)
            return null;
        String line = resourceLine.trim();
        if (line.isEmpty()
                || line.startsWith("#")
                || line.startsWith("swgcraft_end")
                || line.endsWith("*"))
            return null;

        ZString err =
                new ZString("Error in line: \"").app(line).appnl("\":");
        SWGMutableResource resource = null;
        try {
            StringTokenizer tok = new StringTokenizer(line, ",");

            String rName = tok.nextToken().trim();
            Wrapper wr = wrappersGetResource(rName);
            resource = wr.mutable;

            tok.nextToken(); // resource class, skip since we have it already

            if (tok.hasMoreTokens()) {
                SWGResourceStats stats = new SWGResourceStats();
                for (Stat s : Stat.gameOrder()) {
                    if (resource.rc().has(s)) {
                        int stat = Integer.parseInt(tok.nextToken(",.;: "));
                        stats.set(s, stat);
                    }
                }

                if (tok.hasMoreElements()
                        && !tok.nextElement().equals("swgcraft_end"))
                    throw new InputMismatchException(
                            "Unexpected data after stats or maybe duplicate");

                resource.stats(stats, true);
                if (wr.known != null)
                    SWGResourceManager.updateInstance(wr.known, resource);
            }
            return null;
        } catch (NumberFormatException e) {
            err.app("Number error f").app(e.getMessage().substring(1));
        } catch (IllegalArgumentException e) {
            err.app(e.getMessage()); // probably stat out of bounds
        } catch (InputMismatchException e) {
            err.app(e.getMessage());
        } catch (NoSuchElementException e) {
            err.app(resource == null
                    ? "No resource name"
                    : "Missing stat(s)");
        } catch (NullPointerException e) {
            if (resource == null && wrappers.size() > 0)
                err.app("Resource is not found in internal list, " +
                        "did you edit its name?");
            else if (wrappers.size() <= 0)
                err.app("Internal list of resources is empty, " +
                        "did you restart SWGAide?");
            else {
                printLog("notesFileReadResource: " + line, e);
                err.app("NullPointerException: unknown");
            }
        } catch (Exception e) {
            printLog("notesFileReadResource", e);
            err.app(e.getClass()).app(':').app(e.getMessage());
        }
        return err.toString();
    }

    /**
     * Helper method which parses the specified line and returns the time in
     * second, UTC time. If there is an error it is logged to SWGAide's log
     * file, a GUI dialog informs the user, and an exception is thrown. Examples
     * of the header line:
     * 
     * <PRE> Chimaera / CONTINUOUS-LIST / 1234567890
     * Chimaera / Corellia / 1234567890</PRE>
     * 
     * @param headerLine the header line to parse
     * @return seconds since January 1, 1970
     * @throws RuntimeException if there is an error in the header line
     */
    private long notesFileReadTimeLine(String headerLine) {
        try {
            int slash = headerLine.lastIndexOf('/');
            return Long.parseLong(headerLine.substring(slash + 1).trim());
        } catch (RuntimeException e) {
            printLog(String.format(
                    "notesFileReadTimeLine: %s :: %s", headerLine,
                    e.getMessage()),
                    null);
            String m = String.format(
                    "Not a valid header line:%n\"%s\"%n%n"
                            + "See log file and edit \"%s\"%n"
                            + "Aborting", headerLine, notesFileName);
            JOptionPane.showMessageDialog(submitNotesFileButton,
                    m, "Error in header line", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException("ABORT");
        }
    }

    /**
     * Helper method which presents the user with dialogs when a faulty input
     * line is parsed from the notes file. The first dialog is displayed by
     * {@code notesFileShowError(String)} and if the user selects to edit the
     * line this method provides a dialog at which it is possible to edit it.
     * <P>
     * The edited line is returned for further testing, or {@code null} if the
     * user selects to skip the faulty line and continue. If the user selects to
     * abort an exception is thrown.
     * 
     * @param faultyLine the faulty line
     * @param error an error message to present for the user
     * @return an edited line, or {@code null} if the user selects not to edit
     *         the faulty line
     * @throws RuntimeException if the user selected to abort further execution
     */
    private String notesFileRectifyLine(
            String faultyLine, String error) throws RuntimeException {
        String str = faultyLine;
        int response = notesFileShowError(error.trim());
        if (response == 0)
            throw new RuntimeException("ABORT");
        if (response == 2 || response == JOptionPane.CLOSED_OPTION)
            return null; // Skip line and continue

        // else response == 1 // Edit now
        return (String) JOptionPane.showInputDialog(
                submitNotesFileButton, "Edit:", "Rectify error",
                JOptionPane.QUESTION_MESSAGE, null, null, str);
    }

    /**
     * Helper method which shows a GUI option dialog with an error message and
     * returns the chosen option: <BR/>
     * 0 = Abort, edit the notes file and upload manually <BR/>
     * 1 = Edit the line now (default) <BR/>
     * 2 = Skip this line, the resource is submitted as-is <BR/>
     * CLOSED_OPTION if the user closed the dialog
     * 
     * @param errorMessage the error message to display at the dialog
     * @return the chosen option as an integer
     */
    private int notesFileShowError(String errorMessage) {
        String[] options = { "Abort", "Edit now", "Skip and continue" };
        return JOptionPane.showOptionDialog(submitNotesFileButton,
                errorMessage, "Input error", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE, null, options, options[1]);
    }

    /**
     * Helper method which submits the specified resource to SWGCraft.org,
     * either as a new resource or as a resource which <i>perhaps&nbsp;</i> is
     * updated with stats. If there is a similar-name issue this methods obtains
     * and adds a {@link SWGSimilarNameDialog.Wrapper} to the specified list and
     * returns {@code false}.
     * 
     * @param wrapper the wrapper with the resource to submit
     * @param sl a list of wrappers for similarly named resources
     * @return {@code true} if the task is successful
     * @throws SOAPExceptionImpl if there is a communication error
     */
    private boolean notesFileSubmitResource(
            final Wrapper wrapper, List<SWGSimilarNameDialog.Wrapper> sl)
            throws SOAPExceptionImpl {

        if (wrapper == null || wrapper.isSubmitted)
            return true;

        SWGMutableResource res = wrapper.mutable;
        final DefaultListModel<Wrapper> listModel;
        final JList<Wrapper> guiList;

        SWGSoapResponse response;
        if (res.id() <= 0 || wrapper.known == null) {
            // a new resource has no known instance
            guiList = newList;
            listModel = newListModel;
            response = sendNew(wrapper);

        } else if (res.stats().hasValues()) {
            // known but formerly stat-less, do not bother with not updated
            guiList = statlessList;
            listModel = statlessListModel;
            response = sendUpdated(wrapper);

        } else
            return true; // still stat-less, no error

        if (response == null) {
            JOptionPane.showMessageDialog(submitUnreportedAndDepletedButton,
                    "Error: unknown reason, maybe clue is in log files",
                    "No communication: " + res.getName(),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (response.isFaultless()) {
            SwingUtilities.invokeLater(new Runnable() {
                @SuppressWarnings("synthetic-access")
                @Override
                public void run() {
                    int index = wrapperIndexOf(wrapper, listModel);
                    guiList.ensureIndexIsVisible(index);
                    guiList.revalidate();
                    guiList.repaint();
                }
            });

            if (response.getStatus() == 3)
                return true; // success
        }

        // remember, sendNew can be successful but still have status
        // 1 == a resource "name" already exists, the stats have been updated if
        // it had none >> we continue
        //
        // sendUpdate writes error to log, so no need for GUI dialog

        if (response instanceof SWGSoapNOResResponse) {
            int status = response.getStatus();
            if (status == -2) {
                sl.add(SWGSimilarNameDialog.
                        wrapperInstance((SWGSoapNOResResponse) response, res));
                return true;
            }

            String rm = response.getFaultMessage() != null
                    ? response.getFaultMessage()
                    : response.getStatusString();

            printLog(String.format("<submitNew>: %s:%s:%s%n\t%s",
                    res.getName(), res.rc().rcName(), res.galaxy().getName(),
                    rm), null);

            if (rm.contains("Invalid Content-Type:text/html"))
                throw new SOAPExceptionImpl();

            if (status == 999)
                showUsernamePasswordDialog();
            else {
                if (status == 1 && suppressDialog) return false;

                ZString z = new ZString(res.getName()).app(':');
                z.appnl(res.rc().rcName());

                if (status == 1) {
                    z.appnl("already exists at SWGCraft.");
                    z.app("Verify its correctness.");
                } else if (status == 8) {
                    z.appnl("exists with different resource class!");
                    z.appnl("See the help text (F1) on how to");
                    z.app("fix this issue at SWGCraft");
                } else {
                    z.app("Error: \"").app(response.getStatusString());
                    z.appnl("\"").app('\"');
                    if (response.getFaultMessage().endsWith(
                            "Message send failed")) {
                        z.appnl("Unknown communication error.");
                        z.app("Perhaps the action was successful anyway.");
                    } else
                        z.app(response.getFaultMessage());
                    z.app('\"');
                }
                ZString rn = new ZString("Upload issue: ").app(res.getName());
                if (status == 1) {
                    String[] opts = { "OK", "OK to all" };
                    if (1 == JOptionPane.showOptionDialog(
                            submitUnreportedAndDepletedButton, z.toString(),
                            rn.toString(), JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, opts, opts[0]))

                    suppressDialog = true;
                } else
                    JOptionPane.showMessageDialog(
                            submitUnreportedAndDepletedButton, z.toString(),
                            rn.toString(), JOptionPane.WARNING_MESSAGE);
            }
        }
        return false;
    }

    /**
     * Helper method which uploads the content of the specified list of wrappers
     * to SWGCraft.org. For each element in the specified list this method
     * invokes {@link #notesFileSubmitResource(Wrapper, List)}.
     * 
     * @param wl the list of wrappers to submit
     * @param sl a list of wrappers for similarly named resources
     * @return {@code true} if the submission is successful
     * @throws SOAPExceptionImpl if there is a server error at SWGCraft
     */
    private boolean notesFileSubmitResources(
            List<Wrapper> wl, List<SWGSimilarNameDialog.Wrapper> sl)
            throws SOAPExceptionImpl {

        boolean success = true;
        for (Wrapper wr : wl)
            success &= notesFileSubmitResource(wr, sl);

        return success;
    }

    /**
     * Helper method which initiates the background worker which submits the
     * elements in {@code wrappers} to SWGCraft.org. This method invokes submits
     * the specified list of resources on a background thread.
     * 
     * @param notes the notes file which pertains to this upload
     * @param wraps a reference to the current {@code wrappers} in the case it
     *        is changed during the background job
     * @param autoDelete {@code false} if the notes file is not allowed to be
     *        auto-deleted after submission
     */
    private void notesFileSubmitResources(final SWGNotes notes,
            final List<Wrapper> wraps, final boolean autoDelete) {

        isWorking = true;
        suppressDialog = false;

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {

            @SuppressWarnings("synthetic-access")
            public void run() {
                List<SWGSimilarNameDialog.Wrapper> sl = new
                        ArrayList<SWGSimilarNameDialog.Wrapper>();

                try {
                    boolean success = notesFileSubmitResources(wraps, sl);
                    notesFileSubmitResourcesDone(
                            success, autoDelete, notes, wraps, sl);
                } catch (SOAPExceptionImpl e) {
                    showSoapExceptionDialog();
                } finally {
                    resetGUI();
                }
                executor.shutdown();
            }
        });
    }

    /**
     * Helper method which is invoked when {@code notesFileSubmitResources(...)}
     * is finished. This method executes at the AWT event thread.
     * 
     * @param success {@code false} if submission had an error or more
     * @param autoDelete {@code false} if the notes file is not allowed to be
     *        auto-deleted after submission
     * @param notes the notes file which pertains to this upload
     * @param wraps a list of {@code wrappers}
     * @param sl a list of wrappers for similarly named resources
     */
    private void notesFileSubmitResourcesDone(final boolean success,
            final boolean autoDelete, final SWGNotes notes,
            final List<Wrapper> wraps,
            final List<SWGSimilarNameDialog.Wrapper> sl) {

        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                newList.ensureIndexIsVisible(
                        newList.getModel().getSize() - 1);
                statlessList.ensureIndexIsVisible(
                        statlessList.getModel().getSize() - 1);

                if (!sl.isEmpty())
                    SWGSimilarNameDialog.display(SWGMailISDroidPanel.this, sl);

                notesFileEraseNotes(notes, success, autoDelete);
                frame.putToStatbar("Done");
                notifyUpdateSubscribers(wraps.size() > 0
                        ? wraps.get(0).report.gxy()
                        : null);
            }
        });
    }

    /**
     * Helper method which writes the list of new and stat-less resources to
     * the specified notes so that the user can add stats to the resources. The
     * specified file is appended to, thus, possible content is not overwritten.
     * If there is nothing to write this method does nothing.
     * 
     * @param notes the notes file
     */
    private void notesFileWrite(SWGNotes notes) {
        wrappers = mergeWriteResources();
        if (wrappers.size() <= 0)
            return;

        if (((Boolean) SWGFrame.getPrefsKeeper().get(
                "ISDroidFileContinuous", Boolean.FALSE)).booleanValue())
            notesFileWriteContinuous(notes);
        else
            notesFileWritePlanets(notes);
    }

    /**
     * Helper method which determines if the notes file for ISDroid reports is
     * empty. If the file is not empty the user can select to empty it. If the
     * user selects not to empty the file it is appended to by following helper
     * methods.
     * 
     * @param notes the notes object to check for emptiness
     */
    private void notesFileWriteCheckEmpty(SWGNotes notes) {
        if (notes.length() > 0
                && JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                        notesFileSaveButton, String.format(
                                "\"%s\" is not empty, erase content?",
                                notesFileName), "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE))
            notes.erase(false);
    }

    /**
     * Helper method which writes a continuous list of new and stat-less
     * resources to the specified notes. The source for this task is
     * {@link #wrappers}. This continuous output is easier for the player to use
     * together with a 30k veteran resource crate, as opposed to when the output
     * is written planet by planet.
     * 
     * @param notes the notes file
     */
    private void notesFileWriteContinuous(SWGNotes notes) {
        try {
            ZString z = new ZString();

            notesFileWriteHeader(z, wrappers.get(0), null);

            for (Wrapper wr : wrappers)
                if (!notesFileWriteIsBlacklisted(wr.mutable))
                    notesFileWriteResource(z, wr.mutable, null);

            z.nl().nl();
            if (((Boolean) SWGFrame.getPrefsKeeper().get("ISDroidFileHelpText",
                    Boolean.TRUE)).booleanValue())
                notesFileWriteHelpContinuous(z);

            notes.add(z.toString(), true);
        } catch (Throwable e) {
            printLog("notesFileWriteCont", e);
        }
    }

    /**
     * Helper method which creates the blacklist for those resource classes
     * which should not be written to {@code notesFileName}.
     */
    private void notesFileWriteCreateBlacklist() {
        resourceClassBlackList = new ArrayList<SWGResourceClass>();

        File bl = new File("misc", "isdroid_blacklist.txt");
        if (bl.exists()) {
            ZReader sr = ZReader.newTextReader(bl);
            if (sr == null) return;

            for (String line : sr.lines(true, true)) {
                try {
                    resourceClassBlackList.add(SWGResourceClass.rc(line));
                } catch (Exception e) {
                    String m = String.format("Invalid resource class: \"%s\"",
                            line);
                    JOptionPane.showMessageDialog(notesFileSaveButton,
                            m, "Invalid", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Helper method which appends a header for a notes file. If the user has
     * selected a planet-wise output a header for the specified planet is
     * appended. Otherwise, if the planet is {@code null}, this method appends
     * just one header.
     * 
     * @param z a string
     * @param w the wrapper around one of the resources
     * @param p a planet constant for a planet-wise header, or {@code null} for
     *        a continuous-list header
     */
    private void notesFileWriteHeader(ZString z, Wrapper w, SWGPlanet p) {
        // sample line: Chimaera / Corellia / 1198861603
        // sample line: Chimaera / CONTINUOUS-LIST / 1198861603

        z.nl().app(w.report.gxy().getName()).app(" / ").app(p != null
                ? p.getName()
                : "CONTINUOUS-LIST").app(" / ");
        z.appnl(Long.toString(w.report.mail().date()));
    }

    /**
     * Helper method which appends a brief help text for planetary output.
     * 
     * @param z a string builder
     */
    private void notesFileWriteHelp(ZString z) {
        z.appnl("# Help text (can be turned off in Options >> ISDroid)");
        z.appnl("# The content works as follows:");
        z.appnl("#");
        z.appnl("# Galaxy / Planet / time of the report in seconds UTC time");
        z.appnl("# swgcraft_start");
        z.appnl("# res-name-one, res-class-one,");
        z.appnl("# res-name-two, res-class-two,");
        z.appnl("# swgcraft_end");
        z.appnl("#");
        z.appnl("# swgcraft_start");
        z.appnl("# res-name-three, res-class-three,");
        z.appnl("# res-name-two, res-class-two, **");
        z.appnl("# res-name-four, res-class-four, 123,456,78,999");
        z.appnl("# swgcraft_end");
        z.appnl("#");
        z.appnl("# Notice in the second block, one resource has stars **");
        z.appnl("# after the class name which means that the resource is");
        z.appnl("# already written to a previous planet. It does not matter");
        z.appnl("# which of the planets you add resource stats for because");
        z.appnl("# SWGAide manages the planets.");
        z.appnl("# Find resources by sampling for them or by browsing the");
        z.appnl("# 30k Veteran Reward Resource Crate.");
        z.appnl("# Append resource stats after the resource class in the");
        z.appnl("# same order as they are read in game, each stat separated");
        z.appnl("# by a comma or characters mentioned in help texts (F1).");
        z.appnl("#");
        z.appnl("#");
        z.appnl("# SWGCRAFT.ORG: If you do not submit via SWGAide you can");
        z.appnl("# copy and paste the sections to SWGCraftor, see the help");
        z.appnl("# texts in SWGAide (F1).");
        z.appnl("# However, then you must remove the trailing stars and");
        z.appnl("# verify that stats are delimited by comma signs only.");
        z.appnl("# You can copy from the notes text panel in SWGAide. Copy");
        z.appnl("# all lines between AND INCLUDING the \"swgcraft_start\"");
        z.appnl("# AND \"swgcraft_end\" lines for each planet. Ensure");
        z.appnl("# yourself that you paste it to the correct planet");
    }

    /**
     * Helper method which appends a brief help text for the continuous output.
     * 
     * @param z a string
     */
    private void notesFileWriteHelpContinuous(ZString z) {
        z.appnl("# Help text (can be turned off in Options >> ISDroid)");
        z.appnl("# This format of the list is ONLY for automated handling");
        z.appnl("# by SWGAide. The content works as follows:");
        z.appnl("#");
        z.appnl("# Galaxy / time of the report in seconds UTC time");
        z.appnl("# res-name-one, res-class-one,");
        z.appnl("# res-name-two, res-class-two,");
        z.appnl("# etc...");
        z.appnl("#");
        z.appnl("# The resources are ordered by resource class in the same");
        z.appnl("# order as resource classes are ordered in the 30k veteran");
        z.appnl("# resource crate. There is only one entry per resource and");
        z.appnl("# SWGAide manages the planets.");
        z.appnl("# Append resource stats after the resource class in the");
        z.appnl("# same order as they are read in game, each stat separated");
        z.appnl("# by a comma or characters mentioned in help texts (F1).");
    }

    /**
     * Helper method which determines if the specified argument is a resource
     * class which is blacklisted and which should not be printed to the user's
     * notes file.
     * 
     * @param resource the resource to check
     * @return {@code true} if the argument's resource class is blacklisted,
     *         {@code false} otherwise
     */
    private boolean notesFileWriteIsBlacklisted(SWGResource resource) {
        if (resourceClassBlackList == null)
            notesFileWriteCreateBlacklist();

        for (SWGResourceClass rc : resourceClassBlackList)
            if (resource.rc().isSub(rc))
                return true;

        return false;
    }

    /**
     * Helper method which appends the resources for the specified planet. If
     * the boolean argument is {@code false} any later occurrences are written
     * at the end of each block, marked with trailing stars; otherwise they are
     * omitted.
     * 
     * @param p a planet constant
     * @param z a string builder
     * @param skip {@code true} to skip duplicate resources
     */
    private void notesFileWritePlanet(SWGPlanet p, ZString z, boolean skip) {
        List<Wrapper> written = skip
                ? null
                : new ArrayList<Wrapper>();

        boolean first = true;
        for (Wrapper wrapper : wrappers) {
            if (!wrapper.mutable.isAvailableAt(p))
                    continue;

            if (!notesFileWriteIsBlacklisted(wrapper.mutable)) {
                if (first) {
                    notesFileWriteHeader(z, wrapper, p);
                    z.appnl("swgcraft_start");
                    first = false;
                }
                if (!wrapper.isSubmitted)
                    notesFileWriteResource(z, wrapper.mutable, null);
                else if (written != null)
                    written.add(wrapper); // defer to end of current planet

                // temporarily mark it submitted, this is cleared in
                // notesFileWritePlanets(...) once all wrappers are written
                wrapper.isSubmitted = true;
            }
        }

        if (written != null) // write duplicates
            for (Wrapper wr : written)
                notesFileWriteResource(z, wr.mutable, "**");

        if (!first) // at least one resource is written for this planet
            z.appnl("swgcraft_end");
    }

    /**
     * Helper method which writes a list of new and stat-less resources to the
     * specified notes. The source for this task is {@link #wrappers}. The
     * output is written in chunks planet by planet, as opposed to the
     * continuous output which is easier for the player to use together with a
     * 30k veteran resource crate.
     * 
     * @param notes the notes file
     */
    private void notesFileWritePlanets(SWGNotes notes) {
        try {
            ZString z = new ZString();

            boolean skipDuplicates = ((Boolean) SWGFrame.getPrefsKeeper().get(
                    "ISDroidFileSkipDuplicates", Boolean.TRUE)).booleanValue();

            // planet by planet
            for (SWGPlanet planet : SWGPlanet.values())
                notesFileWritePlanet(planet, z, skipDuplicates);

            z.nl().nl();
            if (((Boolean) SWGFrame.getPrefsKeeper().get(
                    "ISDroidFileHelpText", Boolean.TRUE)).booleanValue())
                notesFileWriteHelp(z);

            notes.add(z.toString(), true);
        } catch (Throwable e) {
            printLog("notesFileWritePlanets", e);
        }
        // reset submitted value after file is written, see notesFileWritePlanet
        for (Wrapper wr : wrappers)
            wr.isSubmitted = false;

    }

    /**
     * Helper method which appends the specified resource as a line. If {@code s
     * != null} it is appended to the same line.
     * 
     * @param z a string 
     * @param r the resource to write
     * @param s an optional string to append, or {@code null}
     */
    private void notesFileWriteResource(ZString z, SWGResource r, String s) {
        z.app(r.getName()).app(", ").app(r.rc().rcName()).app(", ");
        if (s != null) z.app(s);
        z.nl();
    }

    /**
     * Helper method which updates clients which have subscribed for resource
     * update notifications.
     * 
     * @param galaxy the galaxy the update pertains to
     */
    private void notifyUpdateSubscribers(SWGCGalaxy galaxy) {
        if (galaxy != null)
            SWGResourceManager.notifySubscribers(new ResourceUpdate(
                    UpdateType.LOCAL_SUBMISSION, galaxy));
    }

    /**
     * Helper method which populates the GUI list for depleted resources. This
     * method invokes {@code mergeDepletedResources(SWGResourceSet)} to obtain a
     * list of depleted resources deduced from the set of known resources and
     * the current reports. This method also sets {@link #depletedResources}.
     * 
     * @param known a set of all known resources for the current galaxy, the set
     *        may include already depleted resource
     * @throws NullPointerException if the argument is {@code null}
     */
    private void populateDepletedList(SWGResourceSet known) {
        depletedResources = mergeDepletedResources(known);
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                depletedListModel.clear();
                for (Wrapper wr : depletedResources)
                    depletedListModel.addElement(wr);
            }
        });
    }

    /**
     * Helper method which populates the GUI list for resources which are new at
     * the galaxy. This method invokes {@code mergeNewResources()} to obtain a
     * list of new resources deduced from the current reports. This method also
     * sets {@link #newResources}.
     */
    private void populateNewList() {
        newResources = mergeNewResources();
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                newListModel.clear();
                for (Wrapper wr : newResources)
                    newListModel.addElement(wr);
            }
        });
    }

    /**
     * Helper method which populates the GUI list of known resources which are
     * listed at SWGCraft.org without stats. This method invokes {@code
     * mergeStatlessResources()} to obtains a list of resources without stats.
     * This method also sets {@link #statlessResources}.
     */
    private void populateStatlessList() {
        statlessResources = mergeStatlessResources();
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                statlessListModel.clear();
                for (Wrapper wr : statlessResources)
                    statlessListModel.addElement(wr);
            }
        });
    }

    /**
     * Helper method which populates the GUI list of resources which can be
     * complemented with one or more planets. These are resources listed at
     * SWGCraft.org but one or several planets are missing. This method invokes
     * {@code mergeUnreportedPlanets()} to obtain a list of such resources
     * deduced from the current reports. This method also sets
     * {@link #unreportedPlanets}.
     */
    private void populateUnreportedPlanetList() {
        unreportedPlanets = mergeUnreportedPlanets();
        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                unreportedListModel.clear();
                for (Wrapper wr : unreportedPlanets)
                    unreportedListModel.addElement(wr);
            }
        });
    }

    /**
     * Helper method which prints a debug message to log file. If the exception
     * is {@code null} the error message is written to SWGAide's general log
     * file, otherwise to the error log file. The error message is suffixed to
     * an initial string which reads "SWGISDroidPanel:".
     * 
     * @param errorMsg the message to print
     * @param e the exception to print a stack trace for, or {@code null}
     */
    private void printLog(String errorMsg, Throwable e) {
        String m = "SWGISDroidPanel:" + errorMsg;
        if (e == null)
            SWGAide.printDebug("isdr", 1, m);
        else
            SWGAide.printError(m, e);
    }

    /**
     * Helper method which resets GUI once upload is finished or aborted. This
     * method just resets the cursor and {@link #isWorking}.
     */
    private void resetGUI() {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        isWorking = false;
    }

    /**
     * Helper method which resets the state and GUI to neutral
     */
    private void resetISDroid() {
        reports = null;
        currentMail = null;
        toon = mailPane.toon;
        depletedResources = null;
        unreportedPlanets = null;
        newResources = null;
        statlessResources = null;
        depletedListModel.clear();
        unreportedListModel.clear();
        statlessListModel.clear();
        newListModel.clear();
        submitUnreportedAndDepletedButton.setEnabled(false);
        notesFileSaveButton.setEnabled(false);
        submitNotesLabel.setForeground(Color.LIGHT_GRAY);
        submitNotesFileButton.setEnabled(false);
    }

    /**
     * Helper method which submits a depleted message to SWGCraft.org for the
     * specified resource.
     * 
     * @param wrapper the wrapper for the depleted resource
     * @return the response from SWGCraft.org
     */
    private SWGSoapStatusResponse sendDepleted(Wrapper wrapper) {
        return SWGResourceManager.sendDeplete(
                wrapper.known, wrapper.report.mail().date());
    }

    /**
     * Helper method which submits the new resource to SWGCraft.org. If the task
     * is successful all wrappers in {@link #newResources} with an equal name
     * are marked as submitted. If no communication happened this method returns
     * {@code null} but if there was an error the returned response carries an
     * error message.
     * 
     * @param wrapper a wrapper with the new resource
     * @return the response from the SOAP action, or {@code null}
     * @throws IllegalArgumentException if there is any illegal value
     * @throws NullPointerException if there is a {@code null} argument or
     *         something parsed
     */
    private SWGSoapStatusResponse sendNew(Wrapper wrapper) {
        SWGMutableResource mr = wrapper.mutable;
        SWGSoapStatusResponse resp = SWGResourceManager.sendNew(mr, false);
        if (resp != null && resp.isFaultless()) {
            mr.whatever = "OK new";
            updateNewList(mr);
        }

        return resp;
    }

    /**
     * Helper method which submits planet availability information to
     * SWGCraft.org for the specified resource and the specified planet.
     * 
     * @param wrapper a wrapper for the resource and its planet record
     * @return the response from the SOAP action
     */
    private SWGSoapAvailResResponse sendUnreported(Wrapper wrapper) {

        SWGSoapAvailResResponse response =
                SWGResourceManager.sendAvailable(wrapper.known, wrapper.mutable
                        .availabilityFor(wrapper.report.planet()));

        if (!response.isFaultless() && response.getStatus() != 999) {
            // not username/password err
            ZString z = new ZString(wrapper.mutable.getName()).app(" : ");
            z.appnl(wrapper.mutable.rc().rcName());

            if (response.getStatus() == 7) {
                z.appnl("seems to be an old resource.");
                z.appnl("See the help text (F1) on how to");
                z.app("fix this issue at SWGCraft.org");
            } else {
                z.app("ERROR: \"");
                z.appnl(response.getStatusString()).app("\":");
                if (response.getFaultMessage().endsWith("Message send failed")) {
                    z.appnl("Unknown communication error, perhaps");
                    z.appnl("the action was successful anyway.");
                } else
                    z.app(response.getFaultMessage());
            }
            z.nl().nl().app("See log file. Will try to Continue.");
            JOptionPane.showMessageDialog(
                    submitUnreportedAndDepletedButton, z.toString(),
                    "Send planet available", JOptionPane.WARNING_MESSAGE);
        }
        return response;
    }

    /**
     * Helper method which submits the specified resource to SWGCraft.org as-is.
     * The resource is supposed to be updated in some way. If there is an error
     * this implementation just writes the SOAP fault message to SWGAide's log
     * file because there is no reason to deal with the error at the GUI.
     * 
     * @param wrapper a wrapper with the resource
     * @return the response from the SOAP action
     * @throws NullPointerException if there is a {@code null} argument or
     *         something parsed
     */
    private SWGSoapEditResResponse sendUpdated(Wrapper wrapper) {
        SWGSoapEditResResponse response = SWGResourceManager.
                sendEdit(wrapper.known);

        if (response.isFaultless()) {
            wrapper.mutable.whatever = "OK edit";
            wrapper.isSubmitted = true;
        } else
            printLog(String.format("sendUpdated: %s",
                    response.getFaultMessage() != null
                            ? response.getFaultMessage()
                            : response.getStatusString()), null);

        return response;
    }

    /**
     * Helper method which displays a dialog about SOAPExceptionImpl; this error
     * is either a communication error with content that cannot be parsed or it
     * is from a server error at SWGCraft.
     */
    private void showSoapExceptionDialog() {
        frame.putToStatbar("<html><font color=\"red\">ERROR</font></html>");
        JOptionPane.showMessageDialog(notesFileSaveButton,
                "Communication error, check log files\nAbort",
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Helper method which displays a GUI dialog about username/password error.
     */
    private void showUsernamePasswordDialog() {
        JOptionPane.showMessageDialog(submitUnreportedAndDepletedButton,
                "Username and/or password is incorrect\n"
                        + "See menu Options >> SWGCraft",
                "Username/password error",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Helper method which submits to SWGCraft.org the content of
     * {@link #unreportedPlanets} and {@link #depletedResources}.
     * 
     * @param unRep a safe reference to {@code unreportedPlanets}
     * @param depl a safe reference to {@code depletedResources}
     * @throws SOAPExceptionImpl if there is a communication error
     */
    private void submitUnreportedAndDepleted(
            List<Wrapper> unRep, List<Wrapper> depl) throws SOAPExceptionImpl {

        if (unRep.size() + depl.size() <= 0)
            return; // sanity

        if (submitUnreportedOrDepleted(unRep, unreportedList))
            if (!submitUnreportedOrDepleted(depl, depletedList)) return;

        SwingUtilities.invokeLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                depletedList.ensureIndexIsVisible(
                        depletedList.getModel().getSize() - 1);
                unreportedList.ensureIndexIsVisible(
                        unreportedList.getModel().getSize() - 1);
            }
        });
    }

    /**
     * Helper method which submits unreported planets or depleted resources to
     * SWGCraft.org. The GUI list argument determines which of the two the
     * invocation pertains to.
     * 
     * @param wraps a list of resources to submit, unreported-planets or
     *        depleted
     * @param guiList the GUI list which is currently being processed
     * @return {@code false} if the task must abort
     * @throws SOAPExceptionImpl if there is a communication error
     */
    private boolean submitUnreportedOrDepleted(
            List<Wrapper> wraps, final JList<Wrapper> guiList) throws SOAPExceptionImpl {

        // true if this batch pertains to the depleted-list
        boolean isDepl = guiList == depletedList;

        int count = 0; // allow repeated tries until success
        boolean continu; // false when there are no more work to do
        do {
            continu = false;
            for (int i = 0; i < wraps.size(); ++i) {
                // loop through all wrappers, it is cheap
                Wrapper wr = wraps.get(i);

                if (!wr.isSubmitted) {
                    continu = true;
                    SWGSoapResponse response;
                    if (isDepl)
                        response = sendDepleted(wr);
                    else
                        response = sendUnreported(wr);

                    if (response.isFaultless()) {
                        wr.isSubmitted = true;
                        final int ii = i;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                guiList.ensureIndexIsVisible(ii);
                                guiList.revalidate();
                                guiList.repaint();
                            }
                        });
                    } else {
                        if (response.getStatus() == 999) {
                            // username/password error
                            frame.putToStatbar("Username/Password error");
                            showUsernamePasswordDialog();
                            return false;
                        } // else

                        String rm = response.getFaultMessage() != null
                                ? response.getFaultMessage()
                                : response.getStatusString();

                        printLog(String.format("submitNewAndDepleted: %s", rm),
                                null);
                        if (rm.contains("Invalid Content-Type:text/html"))
                            throw new SOAPExceptionImpl();

                        // try to continue
                    }
                } // !isSubmitted
            } // for loop
            ++count;
        } while (continu && count < 10);

        if (continu && count >= 10) {
            JOptionPane.showMessageDialog(mailPane.folderList,
                    String.format("Some resources could not be marked%n" +
                            "as %s at SWGCraft.org", isDepl
                            ? "depleted"
                            : "available"), "Error",
                    JOptionPane.WARNING_MESSAGE);
        }
        return true;
    }

    /**
     * Updates the GUI list of new resources after a successful submit action of
     * the specified resource. If the argument is {@code null} or if its member
     * {@link SWGMutableResource#whatever} does not begin with "OK" this method
     * does nothing.
     * 
     * @param mr the resource that was submitted
     */
    public void updateNewList(final SWGMutableResource mr) {
        if (mr != null && mr.whatever != null
                && ((String) mr.whatever).startsWith("OK")) {
            SwingUtilities.invokeLater(new Runnable() {
                @SuppressWarnings("synthetic-access")
                @Override
                public void run() {
                    // mark this resource's wrappers submitted, one per planet
                    for (Wrapper wr : newResources)
                        if (wr.mutable.getName().equals(mr.getName()))
                            wr.isSubmitted = true;

                    newList.revalidate();
                    newList.repaint();
                }
            });
        }
    }

    /**
     * Helper method which determines and returns the index for the last
     * occurrence of the specified wrapper, or -1 if it is not found.
     * 
     * @param wrapper the wrapper for which to find its index
     * @param listModel the list model to scan
     * @return the index for the specified wrapper in the list model, or -1
     */
    private int wrapperIndexOf(Wrapper wrapper, DefaultListModel<Wrapper> listModel) {
        String wn = wrapper.mutable.getName();
        Object o = null;
        int i;
        for (i = 0; i < listModel.size(); ++i) {
            Wrapper w = listModel.get(i);
            if (wrapper == w) {
                o = w; // hit only once, the first time
            } else if (o != null && !wn.equals(w.mutable.getName()))
                return i - 1; // always sorted by name, this is first unequal
            // else continue
        }
        return i; // last wrapper(s)
    }

    /**
     * Helper method which returns a wrapper for a new or stat-less resource
     * from {@code wrappers}.
     * 
     * @param name the proper name for the resources
     * @return the wrapper containing the resource, or {@code null}
     */
    private Wrapper wrappersGetResource(String name) {
        for (Wrapper wr : wrappers)
            if (wr.mutable.getName().equals(name))
                return wr;

        return null;
    }

    /**
     * Comparator for wrappers. Sort order is resource-class, then resource
     * name.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    static class ClassNameComparator implements Comparator<Wrapper> {

        public int compare(Wrapper o1, Wrapper o2) {
            return o1.mutable.compareTo(o2.mutable);
        }
    }

    /**
     * Comparator for wrappers. Sort order is resource-class, resource name, and
     * then planet.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    static class ClassNamePlanetComparator implements Comparator<Wrapper> {

        public int compare(Wrapper o1, Wrapper o2) {
            int res = o1.mutable.compareTo(o2.mutable);
            if (res == 0)
                res = o1.report.planet().compareTo(o2.report.planet());

            return res;
        }
    }

    /**
     * This type renders the content at the GUI lists.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     * @see SWGListCellRenderer
     */
    private class ListRenderer extends SWGListCellRenderer<Wrapper> {

        /**
         * Creates an instance of this type.
         */
        ListRenderer() {
            super();
        }

        @Override
        protected void colorForeground(JList<Wrapper> list, Wrapper value, int index,
                boolean isSelected, boolean cellHasFocus) {

            Wrapper wr = value;
            if (wr.isSubmitted)
                setForeground(Color.LIGHT_GRAY);
            else
                super.colorForeground(
                        list, value, index, isSelected, cellHasFocus);
        }

        @SuppressWarnings("synthetic-access")
        @Override
        protected String labelString(JList<? extends Wrapper> list, Wrapper value, int index,
                boolean isSelected, boolean cellHasFocus) {

            Wrapper wr = value;

            ZString z = new ZString();
            z.app(wr.isSubmitted
                    ? "\u2714 "
                    : "     ");

            z.app(wr.mutable.getName()).app(", ");
            z.app(wr.mutable.rc().rcName());

            if (list == depletedList) {
                long avail = wr.known.age();
                String a = SWGResController.dateString(avail);
                z.app(" (").app(a).app(")");

            } else if (list == unreportedList || list == newList) {
                z.app(" (");
                z.app(wr.report.planet().getName().substring(0, 3)).app(")");
            }
            return z.toString();
        }
    }
}
