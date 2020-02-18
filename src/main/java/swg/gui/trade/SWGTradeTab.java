package swg.gui.trade;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;

import swg.SWGAide;
import swg.SWGConstants;
import swg.gui.SWGFrame;
import swg.gui.common.SWGHelp;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;
import swg.model.mail.SWGAuctionerData;
import swg.model.mail.SWGMailBox;
import swg.model.mail.SWGMailFolder;
import swg.model.mail.SWGMailMessage;
import swg.model.mail.SWGMailMessage.Type;
import swg.tools.ZReader;
import swg.tools.ZWriter;

/**
 * This class encompasses all GUI components for the trade or vendor mail view.
 * That is a tree to browse characters having vendor mails from the SWG Auction
 * system and several statistics about these mails.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGTradeTab extends JTabbedPane {

    /**
     * A map of unique customers as objects
     */
    private Map<String, SWGTradeCustomer> allCustomerList = null;

    /**
     * A list of all customer names
     */
    private List<String> allCustomerNames = null;

    /**
     * A menu item at the Edit menu for archiving current auction mails
     */
    JMenuItem archiveMenuItem;

    /**
     * The menu item for restoring archived auction mails
     */
    JMenuItem archiveRestoreAuctionMailsMenuItem;

    /**
     * The menu item for viewing archived mails
     */
    JMenuItem archiveViewMailsMenuItem;

    /**
     * The mail folder for the currently selected character, containing vendor
     * mails
     */
    SWGMailFolder auctionFolder = null;

    /**
     * The character currently selected, or during a transition the previously
     * selected character
     */
    protected SWGCharacter character = null;

    /**
     * The Customer tab on this component
     */
    private SWGTradeCustomerTab customerTab;

    /**
     * The frame containing the tabbed pane holding this view
     */
    SWGFrame frame;

    /**
     * The URL for the help page for these objects
     */
    URL helpPage;

    /**
     * A menu item for importing auction mails into the current list of auction
     * mails
     */
    private JMenuItem importAuctionMails;

    /**
     * A table of unique items sold of the current list of sales mails
     */
    private HashMap<String, SWGTradeItem> itemMap = null;

    /**
     * A list of names of sold items
     */
    private List<String> itemNames;

    /**
     * The Misc tab on this component
     */
    SWGTradeMiscTab miscTab;

    /**
     * The file name to use for the price to name mapping
     */
    private final String priceToMapFileName = "price2nameMap.TXT";

    /**
     * A map that maps the last digits of a price to a user chosen name, used
     * whenever an item's name is blank
     */
    Map<Integer, String> priceToNameMapping = null;

    /**
     * A list of purchase sales mails
     */
    private List<SWGAuctionerData> purchaseMails = null;

    /**
     * The current sum of credits for purchases
     */
    private long purchasePriceSum = -1;

    /**
     * The Purchase tab on this component
     */
    private SWGTradePurchaseTab purchaseTab;

    /**
     * The menu item for reloading auction mails for the current character
     */
    private JMenuItem reloadAuctionMails;

    /**
     * A list of sales mails
     */
    private List<SWGAuctionerData> salesMails = null;

    /**
     * The total credits earned
     */
    private long salesSum = -1;

    /**
     * The Sales tab on this component
     */
    SWGTradeSalesTab salesTab;

    /**
     * The listener for the Save As menu iyem at the File menu at this
     * application
     */
    ActionListener saveCSVlistener;

    /**
     * The earliest date of the current list of auction mails, in seconds since
     * Jan 1, 1970
     */
    private long startDate = -1;

    /**
     * The Statistics tab on this component
     */
    private SWGTradeStatisticTab statisticsTab;

    /**
     * The most recent date of the current list of auction mails, in seconds
     * since Jan 1, 1970
     */
    private long stopDate = -1;

    /**
     * A map of vendors for the current list of sales mails
     */
    private Map<String, SWGTradeVendor> vendorMap = null;

    /**
     * Creates a tabbed pane that holds the GUI components
     * 
     * @param owner the frame holding this object
     */
    public SWGTradeTab(SWGFrame owner) {
        frame = owner;
        setOpaque(true);

        helpPage = SWGAide.class.getResource("docs/help_trade__en.html");

        // create interior lazily, see this#focusGained()
        frame.getTabPane().addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                focusGained();
            }
        });
    }

    /**
     * Archives the current auction mails to a folder named "Auction" in the
     * current character's mailbox path, separated into different years. This
     * method invokes {@link SWGMailFolder#auctionArchive(File)}.
     */
    private void archiveAuctionMails() {
        if (auctionFolder == null || auctionFolder.size() <= 0) return;
        File path = createArchiveFolder();
        if (path == null) return;

        if (JOptionPane.OK_OPTION != JOptionPane.showConfirmDialog(frame,
                String.format("All auction mails are archived to:%n\"" +
                        "SWGAide\\%s\"", path.toString()), "Confirm archiving",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
            return;

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        auctionFolder.auctionArchive(path);

        resetAllLists();
        updateTabs(true);
        updateMenuItems();
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Returns a menu item for archiving current auction mails
     * 
     * @return a menu item for archiving current auction mails
     */
    private JMenuItem archiveAuctionMailsMenuItem() {
        JMenuItem archive = new JMenuItem("Archive");
        archive.setToolTipText("Archive the present auction mails");
        archive.setMnemonic('A');
        archive.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e) {
                archiveAuctionMails();
            }
        });
        // TODO: disable until later, how to do when SWG is no available?
        archive.setEnabled(false);
        return archive;
    }

    /**
     * Restores archived mails to the currently viewed list of auction mails
     */
    protected void archiveRestoreAuctionMails() {
        String str = "Select an archive of auction mails to import";
        File archiveFolder = getArchiveFolders(str);
        if (archiveFolder == null) return;

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        resetAllLists();

        File[] mails = archiveFolder.listFiles();
        auctionFolder = character.mailBox().folder("Auction");
        auctionFolder.auctionArchiveRestore(mails);

        updateTabs(true);
        updateMenuItems();
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Returns a menu item for restoring archiving current auction mails
     * 
     * @return a menu item for restoring archiving current auction mails
     */
    private JMenuItem archiveRestoreMenuItem() {
        JMenuItem archive = new JMenuItem("Restore Archive");
        archive.setToolTipText("Restore archived auction mails");
        archive.setMnemonic('R');
        archive.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                archiveRestoreAuctionMails();
            }
        });
        return archive;
    }

    /**
     * Presents selected archived mails at the GUI but do not move them into the
     * application again. This method runs the logic to present the user with
     * options and handle user input.
     */
    void archiveViewMails() {
        String str = "Select an archive to view temporarily.\n"
                + "This will not import mails into SWHAide";
        File archiveFolder = getArchiveFolders(str);
        if (archiveFolder == null) return;

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SWGMailFolder fld = new SWGMailFolder("archive", null);
        File[] mails = archiveFolder.listFiles();

        for (File f : mails) {
            try {
                SWGMailMessage msg = SWGMailBox.newMail(
                        f.getAbsoluteFile(), character);
                fld.add(msg);
            } catch (Exception e) {
                SWGAide.printError("SWGTradeTab:viewArchivedMails: ", e);
            }
        }

        resetAllLists();
        auctionFolder = fld;
        updateTabs(true);
        archiveMenuItem.setEnabled(false);
        reloadAuctionMails.setEnabled(true);
        importAuctionMails.setEnabled(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Returns a menu item for archiving current auction mails
     * 
     * @return a menu item for archiving current auction mails
     */
    private JMenuItem archiveViewMailsMenuItem() {
        JMenuItem archive = new JMenuItem("View Archive");
        archive.setToolTipText("View archived auction mails");
        archive.setMnemonic('V');
        archive.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                archiveViewMails();
            }
        });
        return archive;
    }

    /**
     * Creates and returns a folder named "Auction" in the current character's
     * mailbox path
     * 
     * @return a folder named "Auction" in the current character's mailbox path,
     *         <code>null</code> if the current character is <code>null</code>
     */
    File createArchiveFolder() {
        if (character == null) return null;
        File path = character.mailBox().swgAidePath();
        path = new File(path, "Auction");
        path.mkdir();
        return path;
    }

    /**
     * Sets the general menu items for this component
     */
    void fixMenuItems() {
        frame.editMenuAdd(archiveMenuItem);
        frame.editMenuAdd(archiveRestoreAuctionMailsMenuItem);
        frame.editMenuAdd(archiveViewMailsMenuItem);
        frame.editMenuAdd(reloadAuctionMails);
        frame.editMenuAdd(importAuctionMails);
        frame.saveAsAddListener(saveCSVlistener,
                "Save auction mails in CSV format");
        updateMenuItems();
    }

    /**
     * Called whenever this trade tabbed pane is de/selected
     */
    protected void focusGained() {
        if (frame.getTabPane().getSelectedComponent() == this) {
            final SWGCharacter toon = SWGFrame.getSelectedCharacter();
            if (toon == null) {
                JOptionPane.showMessageDialog(frame,
                        "Select a character at Main tab",
                        "No character selected",
                        JOptionPane.INFORMATION_MESSAGE);
                frame.getTabPane().setSelectedIndex(0);
                return;
            }

            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        toon.mailBox().fetch();
                        auctionFolder = toon.mailBox().folder("Auction");
                        resetAllLists();
                        if (!toon.equals(character) && salesTab != null) {
                            salesTab.resetGUI();
                        }
                        character = toon;

                        if (getComponents().length == 0) {
                            makeInterior();
                        }

                        updateTabs(true);
                        fixMenuItems();

                        SWGGalaxy g = character.galaxy();

                        if (auctionFolder.size() == 0) {
                            JOptionPane.showMessageDialog(frame, String.format(
                                    "%s @ %s \u2014 %s%nhas no trade mails",
                                    character.getName(), g.getName(),
                                    g.station().getName()), "No trade mails",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }

                        SWGHelp.push(helpPage);

                        String str = g.station().getName();
                        str += " \u2014 ";
                        str += g.getName();
                        str += " \u2014 ";
                        str += character.getName();
                        frame.putToLogbar_1(str);
                        frame.setCursor(Cursor
                                .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    } catch (Exception e) {
                        SWGAide.printError("SWGAideTradeTab:focusGained", e);
                    }
                }
            });
        } else if (salesTab != null) {
            if (getComponents().length > 0) {
                updateTabs(false);
                frame.saveAsRemoveListener(saveCSVlistener);
                frame.editMenuRemove(archiveMenuItem);
                frame.editMenuRemove(archiveRestoreAuctionMailsMenuItem);
                frame.editMenuRemove(archiveViewMailsMenuItem);
                frame.editMenuRemove(reloadAuctionMails);
                frame.editMenuRemove(importAuctionMails);
            }
            SWGHelp.remove(helpPage);
        }
    }

    /**
     * Returns a list of folders from the folder for auction archives
     * 
     * @return a list of folders from the folder for auction archives,
     *         <code>null</code> if no such archive folder exists or an empty
     *         list if that folder is empty
     */
    File[] getArchiveFolders() {
        if (character == null) return null;
        File path = new File(character.mailBox().swgAidePath(), "Auction");
        if (path.exists()) return path.listFiles(new java.io.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() && f.list().length > 0;
            }
        });
        return null;
    }

    /**
     * Presents current archive folders and let the user select one of them
     * which is returned
     * 
     * @param message the message to present at the dialogue
     * @return the archive file object selected by the user, <code>null</code>
     *         if no archive was found or the user aborted
     */
    private File getArchiveFolders(String message) {
        File[] archives = getArchiveFolders();
        if (archives == null || archives.length == 0) {
            JOptionPane.showMessageDialog(frame, "No archive exists",
                    "No archive", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        String[] archNames = getNames(archives);
        String archName = (String) JOptionPane.showInputDialog(frame,
                message, "Select archive", JOptionPane.QUESTION_MESSAGE, null,
                archNames, archNames[0]);

        for (File f : archives)
            if (f.getName().equals(archName)) return f;

        return null;
    }

    /**
     * Returns a list of unique customer names
     * 
     * @return a list of unique customers names
     */
    public List<String> getCustomerNames() {
        if (allCustomerNames == null) {
            makeCustomerList();
        }
        return allCustomerNames;
    }

    /**
     * Returns a map of unique customers, mapped on their names
     * 
     * @return a map of unique customers, mapped on their names
     */
    public Map<String, SWGTradeCustomer> getCustomers() {
        if (allCustomerList == null) {
            makeCustomerList();
        }
        return allCustomerList;
    }

    /**
     * Returns the number of days the current list of auction mails spans
     * 
     * @return the number of days the current list of auction mails spans
     */
    public String getDaysSpanned() {
        if (stopDate < 0 || startDate < 0) {
            makeSalesMails();
        }
        if (stopDate < 0) return " no entry ";
        long d = stopDate - startDate;
        d = d / 3600;
        if (d < 24) return " " + d + " hours ";
        d /= 24;
        return " " + d + " days ";
    }

    /**
     * Returns the number of unique items sold
     * 
     * @return the number of unique items sold
     */
    public int getItemCount() {
        if (itemMap == null) makeItemMap();
        return itemMap.size();
    }

    /**
     * Returns a map of items, mapped on their names
     * 
     * @return a map of items, mapped on their names
     */
    public Map<String, SWGTradeItem> getItemMap() {
        if (itemMap == null) makeItemMap();
        return itemMap;
    }

    /**
     * Returns either the item name or a string mapped to from the item's price
     * 
     * @param auct the data to parse the item's name from
     * @return the string to present on tables as an item name
     */
    String getItemName(SWGAuctionerData auct) {
        String name = auct.item();
        Boolean override;
        if (name.isEmpty()
                || ((override = (Boolean) SWGFrame.getPrefsKeeper().get(
                        "tradeMiscOverride")) != null
                        && override.booleanValue())) {
            long price = auct.price();
            String nn = getNameFromPrice((int) price);
            if (!nn.isEmpty()) name = nn;
        }
        return name;
    }

    /**
     * Returns a sorted list names for sold items
     * 
     * @return a sorted list names for sold items
     */
    public List<String> getItemNames() {
        if (itemNames == null) makeItemMap();
        return itemNames;
    }

    /**
     * Returns a string mapped to from <code>digits</code>
     * 
     * @param digits the digits which to map for an item name
     * @return a string mapped to from <code>digits</code>
     */
    private String getNameFromPrice(int digits) {
        int ds = digits < 1000
                ? digits % 100
                : digits % 1000;
        Integer key = Integer.valueOf(ds);
        String s = priceToNameMapping.get(key);
        return s == null || s.isEmpty()
                ? ""
                : s + ": ¤";
    }

    /**
     * Returns an array of file names parsed from <code>fileArray</code>
     * 
     * @param fileArray the file array to parse
     * @return a list of file names
     */
    private String[] getNames(File[] fileArray) {
        String[] str = new String[fileArray.length];
        for (int i = 0; i < fileArray.length; ++i)
            str[i] = fileArray[i].getName();

        return str;
    }

    /**
     * Returns a key for the preference keeper to save the global setting per
     * character
     * 
     * @return a key for the preference keeper to save the global setting per
     *         character
     */
    String getPrefsKeyForMiscTab() {
        if (character == null) return "dummy";
        String key = "tradeMiscShared" + character.galaxy().station().getName()
                + character.galaxy().getName() + character.getNameComplete();
        return key;
    }

    /**
     * Returns a sorted list of keys for the
     * {@link SWGTradeTab#priceToNameMapping}, in fact this is a list of prices
     * 
     * @return a sorted list of keys for the
     *         {@link SWGTradeTab#priceToNameMapping}
     */
    List<Integer> getPriceMappingKeys() {
        ArrayList<Integer> priceToNameList = new ArrayList<Integer>(
                priceToNameMapping.keySet());
        Collections.sort(priceToNameList, new Comparator<Integer>() {

            public int compare(Integer i1, Integer i2) {
                if (miscTab.newValue != null && i1.equals(miscTab.newValue))
                    return 1;
                if (miscTab.newValue != null && i2.equals(miscTab.newValue))
                    return -1;

                return i1.compareTo(i2);
            }
        });
        return priceToNameList;
    }

    /**
     * Returns a file object for the price to item name mapping, which may or
     * may not exist on the file system
     * 
     * @return a file object for the price to item name mapping, which may or
     *         may not exist on the file system
     */
    File getPriceMappingsFile() {
        File file = null;
        Boolean b = (Boolean) SWGFrame.getPrefsKeeper().get(
                getPrefsKeyForMiscTab());
        if (b == null || b.booleanValue()) {
            file = new File("mails", priceToMapFileName);
        } else {
            File parent = character.mailBox().swgAidePath();
            parent = new File(parent, "pri2name");
            parent.mkdir();
            file = new File(parent, priceToMapFileName);
        }
        return file;
    }

    /**
     * Returns a list of purchase mails
     * 
     * @return a list of purchase mails
     */
    public List<SWGAuctionerData> getPurchaseMails() {
        if (purchaseMails == null) {
            makePurchaseMails();
        }
        return purchaseMails;
    }

    /**
     * Returns the sum of credits spent on purchases
     * 
     * @return the sum of credits spent on purchases
     */
    public long getPurchasePriceSum() {
        if (purchasePriceSum < 0) makePurchaseMails();
        return purchasePriceSum;
    }

    /**
     * Returns a list of mails from sales transactions
     * 
     * @return a list of mails from sales transactions, an empty list if there
     *         are no such mails
     */
    public List<SWGAuctionerData> getSalesMails() {
        if (salesMails == null) makeSalesMails();
        return salesMails;
    }

    /**
     * Returns the total sum of credits earned
     * 
     * @return the total sum of credits earned
     */
    public long getSalesSum() {
        if (salesSum < 0) makeSalesMails();
        return salesSum;
    }

    /**
     * Returns the earliest date of the current list of auction mails
     * 
     * @return the earliest date of the current list of auction mails
     */
    public Date getStartDate() {
        if (startDate < 0) makeSalesMails();
        return new Date(startDate * 1000);
    }

    /**
     * Returns a map of vendors, mapped on their names
     * 
     * @return a map of vendors, mapped on their names
     */
    public Map<String, SWGTradeVendor> getVendorMap() {
        if (vendorMap == null) makeVendorMap();
        return vendorMap;
    }

    /**
     * Returns a sorted list of vendor names
     * 
     * @return a sorted list of vendor names
     */
    public List<String> getVendorNames() {
        if (vendorMap == null) makeVendorMap();
        List<String> names = new ArrayList<String>(vendorMap.keySet());
        Collections.sort(names);
        return names;
    }

    /**
     * Handles the file chooser dialog for finding a file to save to
     * 
     * @return the option the user opted for at the file chooser
     */
    private int handleFileChooserCSV() {
        JFileChooser fc = SWGFrame.getFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        javax.swing.filechooser.FileFilter ff =
                new javax.swing.filechooser.FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        String fn = f.getName().toLowerCase(Locale.ENGLISH);
                        return (f.isDirectory() || fn.endsWith(".csv"));
                    }

                    @Override
                    public String getDescription() {
                        return "Comma Separated Values (CSV)";
                    }
                };
        fc.addChoosableFileFilter(ff);
        File file = (File) SWGFrame.getPrefsKeeper().get("auctionCSVFile");
        if (file != null) fc.setSelectedFile(file);
        int retVal = fc.showSaveDialog(frame);
        fc.removeChoosableFileFilter(ff);
        return retVal;
    }

    /**
     * Handles the file chooser dialog for finding a files to import
     * 
     * @return the option the user opted for at the file chooser
     */
    private int handleFileChooserImport() {
        JFileChooser fc = SWGFrame.getFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        javax.swing.filechooser.FileFilter ff =
                new javax.swing.filechooser.FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return "Folder with auction mails";
                    }
                };
        fc.setFileFilter(ff);
        int ret = fc.showOpenDialog(frame);
        fc.removeChoosableFileFilter(ff);
        return ret;
    }

    /**
     * Imports auction mails to the currently viewed list of auction mails from
     * anywhere at the file system. Only files that are auction mails are
     * imported by this method, other mail files are left where they are.
     */
    protected void importAuctionMails() {
        int retVal = handleFileChooserImport();
        if (retVal != JFileChooser.APPROVE_OPTION) return;

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        File dir = SWGFrame.getFileChooser().getSelectedFile();
        File[] mails = dir.listFiles(new java.io.FileFilter() {
            public boolean accept(File f) {
                return f.isFile();
            }
        });

        resetAllLists();

        auctionFolder = character.mailBox().folder("Auction");
        auctionFolder.auctionArchiveRestore(mails);

        updateTabs(true);
        updateMenuItems();
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Returns a menu item for importing auction mails from anywhere
     * 
     * @return a menu item for importing auction mails from anywhere
     */
    private JMenuItem importAuctionMailsMenuItem() {
        JMenuItem imp = new JMenuItem("Import...");
        imp.setToolTipText("Import auction mails from other folder");
        imp.setMnemonic('I');
        imp.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                importAuctionMails();
            }
        });
        return imp;
    }

    /**
     * Creates a list of customers based on the current list of sales mails
     */
    private void makeCustomerList() {
        allCustomerList = new HashMap<String, SWGTradeCustomer>();
        List<SWGAuctionerData> smList = getSalesMails();

        for (int i = smList.size() - 1; i >= 0; --i) {
            SWGAuctionerData aud = smList.get(i);
            SWGTradeCustomer ch = allCustomerList.get(aud
                    .other());
            if (ch == null) {
                ch = new SWGTradeCustomer();
                ch.name = aud.other();
                ch.lastVisit = aud.date();
                ch.sum += aud.price();
                allCustomerList.put(ch.name, ch);
            } else {
                long dat = aud.date();
                long period = 60 * 60 * 12; // 12 hours in between

                if (dat >= ch.lastVisit + period) {
                    ch.visits += 1;
                }
                ch.itemCount += 1;
                ch.sum += aud.price();
                ch.lastVisit = dat;
            }
        }
        allCustomerNames = new ArrayList<String>(allCustomerList.keySet());
        Collections.sort(allCustomerNames);
    }

    /**
     * Creates all tabs for this component
     */
    void makeInterior() {
        salesTab = new SWGTradeSalesTab(this);
        add("Sales", salesTab);

        purchaseTab = new SWGTradePurchaseTab(this);
        add("Purchases", purchaseTab);

        statisticsTab = new SWGTradeStatisticTab(this);
        add("Statistics", statisticsTab);

        customerTab = new SWGTradeCustomerTab(this);
        add("Customers", customerTab);

        miscTab = new SWGTradeMiscTab(this);
        add("Misc", miscTab);

        saveCSVlistener = notesSaveAsListener();
        archiveMenuItem = archiveAuctionMailsMenuItem();
        archiveRestoreAuctionMailsMenuItem = archiveRestoreMenuItem();
        archiveViewMailsMenuItem = archiveViewMailsMenuItem();
        reloadAuctionMails = reloadAuctionMailsMenuItem();
        importAuctionMails = importAuctionMailsMenuItem();
    }

    /**
     * Creates a map of sold items from the current list of sales mails
     */
    private void makeItemMap() {
        List<SWGAuctionerData> smList = getSalesMails();
        itemMap = new HashMap<String, SWGTradeItem>(smList.size() / 3);
        for (SWGAuctionerData ad : smList) {
            String name = getItemName(ad);
            SWGTradeItem item = itemMap.get(name);
            if (item == null) {
                item = new SWGTradeItem();
                item.name = name;
                itemMap.put(name, item);
            }
            item.income += ad.price();
            item.numbersSold += 1;
        }
        itemNames = new ArrayList<String>(itemMap.keySet());
        Collections.sort(itemNames);
    }

    /**
     * Resets the list of purchase mails to their initial content. To update
     * from hard disk it is necessary to flip between the main tabs
     */
    private void makePurchaseMails() {
        List<SWGMailMessage> sales = auctionFolder.mails();
        purchaseMails = new ArrayList<SWGAuctionerData>(sales.size());
        long sum = 0L;
        for (SWGMailMessage msg : sales) {
            if (msg.type() == Type.Auction) {
                SWGAuctionerData ad = msg.auctionData();
                if (ad == null) continue;

                SWGAuctionerData.Type type = ad.type();
                if (type == SWGAuctionerData.Type.INSTANT_PURCHASE
                        || type == SWGAuctionerData.Type.VENDOR_PURCHASE
                        || type == SWGAuctionerData.Type.AUCTION_WON) {
                    purchaseMails.add(ad);
                    sum += ad.price();
                }
            }
        }
        purchasePriceSum = sum;
    }

    /**
     * Creates a list of mails of sales mails from the current list of mails
     */
    private void makeSalesMails() {
        List<SWGMailMessage> mails = auctionFolder.mails();
        salesMails = new ArrayList<SWGAuctionerData>(mails.size());
        startDate = System.currentTimeMillis() / 1000;
        stopDate = -1;
        salesSum = 0L;
        for (SWGMailMessage msg : mails) {
            if (msg.type() == Type.Auction) {
                SWGAuctionerData ad = msg.auctionData();
                if (ad == null) continue;

                SWGAuctionerData.Type type = ad.type();
                if (type == SWGAuctionerData.Type.INSTANT_SALE
                        || type == SWGAuctionerData.Type.VENDOR_SALE) {
                    salesMails.add(ad);
                    salesSum += ad.price();
                }
                if (type != SWGAuctionerData.Type.ITEM_EXPIRED
                        && type != SWGAuctionerData.Type.SALE_UNSUCCESSFUL) {
                    long d = ad.date();
                    startDate = d < startDate
                            ? d
                            : startDate;
                    stopDate = d > stopDate
                            ? d
                            : stopDate;
                }
            }
        }
    }

    /**
     * Creates a map of vendors from the current list of sales mails
     */
    private void makeVendorMap() {
        List<SWGAuctionerData> smList = getSalesMails();
        vendorMap = new HashMap<String, SWGTradeVendor>();
        for (SWGAuctionerData ad : smList) {
            SWGTradeVendor vendor = vendorMap.get(ad.vendor());
            if (vendor == null) {
                vendor = new SWGTradeVendor();
                vendor.name = ad.vendor();
                vendorMap.put(vendor.name, vendor);
            }
            vendor.income += ad.price();
            vendor.itemsSold += 1;
        }
    }

    /**
     * Returns an action listener
     * 
     * @return an action listener
     */
    private ActionListener notesSaveAsListener() {
        ActionListener sa = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveToCSV();
            }
        };
        return sa;
    }

    /**
     * Returns <code>true</code> if a file was read successfully, which means
     * the mapping was filled, <code>false</code> otherwise
     * 
     * @return <code>true</code> if a file was read successfully,
     *         <code>false</code> otherwise
     */
    private boolean readPriceMapFromFile() {
        File file = getPriceMappingsFile();
        if (!file.exists()) return false;

        ZReader sr = ZReader.newTextReader(file);
        if (sr != null) {
            try {
                List<String> sl = sr.lines(true, true);
                for (String line : sl) {
                    int index = line.indexOf(";");
                    Integer key = Integer.valueOf(line.substring(0, index));
                    String val = line.substring(index + 1);
                    priceToNameMapping.put(key, val);
                }
                return true;
            } catch (Throwable e) {
                SWGAide.printError("SWGTradeTab:readPriceMapFromFile", e);
            } finally {
            	sr.close();
            }
        }
        return false;
    }

    /**
     * Reloads the auction mails for the current character, used in connection
     * with viewing archived mails
     */
    protected void reloadAuctionMails() {
        if (character == null) return;

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        resetAllLists();

        auctionFolder = character.mailBox().folder("Auction");

        updateTabs(true);
        updateMenuItems();
        reloadAuctionMails.setEnabled(false);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Returns a menu item for reloading current auction mails
     * 
     * @return a menu item for reloading current auction mails
     */
    private JMenuItem reloadAuctionMailsMenuItem() {
        JMenuItem reload = new JMenuItem("Reload");
        reload.setToolTipText("Reload the auction mails for "
                + character.getName());
        reload.setEnabled(false);
        reload.setMnemonic('D');
        reload.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                reloadAuctionMails();
            }
        });
        return reload;
    }

    /**
     * Resets all lists used by the different tabs at this tabbed pane
     */
    void resetAllLists() {
        allCustomerList = null;
        allCustomerNames = null;
        itemMap = null;
        itemNames = null;
        purchaseMails = null;
        purchasePriceSum = -1;
        salesMails = null;
        salesSum = -1;
        startDate = -1;
        stopDate = -1;
        vendorMap = null;
        resetMappingsMap();
    }

    /**
     * Initiates the map for prices to names. That is, creates the map and fills
     * it by reading the mapping information from file, or some sample entries
     */
    void resetMappingsMap() {
        priceToNameMapping = new HashMap<Integer, String>();
        if (readPriceMapFromFile()) return;

        priceToNameMapping
                .put(Integer.valueOf(123), "Example Entry for Blank Name");
    }

    /**
     * Saves the price to item name mapping table content to file
     */
    void savePriceMapToFile() {
        if (character == null) return;
        try {
            File f = getPriceMappingsFile();
            ZWriter wr = ZWriter.newTextWriterExc(f, false);
            wr.writelnExc("# price to item name mapping, edit with care");
            for (Integer key : getPriceMappingKeys()) {
                wr.writeExc(key.toString());
                wr.writeExc(";");
                wr.writelnExc(priceToNameMapping.get(key));
            }
            wr.close();
        } catch (Throwable e) {
            SWGAide.printError("SWGTradeTab:savePriceMapToFile", e);
        }
    }

    /**
     * Saves the entire list of auction mails to a comma separated values file,
     * if the user decides for that. Only mails about expired sales and expired
     * items that are destroyed are not saved to file, but sales and buys are
     * interleaved.
     */
    void saveToCSV() {
        int retVal = handleFileChooserCSV();
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File file = SWGFrame.getFileChooser().getSelectedFile();
            if (file.exists()) {
                int r = JOptionPane
                        .showConfirmDialog(frame, "File " + file.getName()
                                + " exists!\n" + "Do you want to overwrite?",
                                "File exists", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                if (r != JOptionPane.YES_OPTION) return;
            }
            SWGFrame.getPrefsKeeper().add("auctionCSVFile", file);
            writeCSVFile(file);
        }
    }

    /**
     * Updates the menu items for this component, that is, enables or disables
     * them
     */
    private void updateMenuItems() {
        File[] d = getArchiveFolders();
        archiveMenuItem.setEnabled(auctionFolder.size() > 0);
        archiveRestoreAuctionMailsMenuItem
                .setEnabled(d != null && d.length > 0);
        importAuctionMails.setEnabled(true);
        archiveViewMailsMenuItem.setEnabled(d != null && d.length > 0);
    }

    /**
     * Notifies the contained tabs that this tabbed pane has gained or lost
     * focus
     * 
     * @param hasFocus <code>true</code> if this component has gained focus,
     *        <code>false</code> otherwise
     */
    void updateTabs(boolean hasFocus) {
        try {
            salesTab.focusGained(hasFocus);
            purchaseTab.focusGained(hasFocus);
            customerTab.focusGained(hasFocus);
            statisticsTab.focusGained(hasFocus);
            miscTab.focusGained(hasFocus);
        } catch (NullPointerException e) {/* ignore */
        }
    }

    /**
     * Writes the content of the list of auction mails to file. Only mails about
     * expired sales and expired items that are destroyed are not saved to file,
     * but sales and buys are interleaved.
     * 
     * @param file a file
     */
    private void writeCSVFile(File file) {
        try {
            ZWriter wr = ZWriter.newTextWriterExc(file, false);
            writeHeader(wr);

            DateRenderer dr = new DateRenderer();
            for (SWGMailMessage mail : auctionFolder.mails()) {
                SWGAuctionerData ad = mail.auctionData();
                if (ad == null) continue;

                SWGAuctionerData.Type type = ad.type();
                if (type == SWGAuctionerData.Type.SALE_UNSUCCESSFUL
                        || type == SWGAuctionerData.Type.ITEM_EXPIRED)
                    continue;

                writeEntity(wr, dr.getText(new Date(ad.date() * 1000)));
                writeEntity(wr, ad.other());
                writeEntity(wr, ad.item());
                writeEntity(wr, Long.toString(ad.price()));
                writeEntity(wr, ad.vendor());
                writeEntity(wr, ad.location());
                writeEntityType(wr, ad.type());
            }
            wr.close();
        } catch (Throwable e) {
            SWGAide.printError("SWGTradeTab:saveToCSV", e);
        }
    }

    /**
     * Writes an entity to the line representing each transaction
     * 
     * @param wr the file writer to write to
     * @param str the string to write
     * @throws Exception if there is an error
     */
    private void writeEntity(ZWriter wr, String str) throws Exception {
        wr.writeExc("\"");
        wr.writeExc(str);
        wr.writeExc("\",");
    }

    /**
     * Writes an entity to the line representing each transaction and finally
     * this method writes an end-of-line character
     * 
     * @param wr the file writer to write to
     * @param type the type of auction to write
     * @throws Exception if there is an error
     */
    private void writeEntityType(ZWriter wr, SWGAuctionerData.Type type)
            throws Exception {

        wr.writeExc("\"");
        if (type == SWGAuctionerData.Type.INSTANT_SALE)
            wr.writeExc("Instant Sale");

        else if (type == SWGAuctionerData.Type.VENDOR_SALE)
            wr.writeExc("Vendor Sale");

        else if (type == SWGAuctionerData.Type.AUCTION_WON)
            wr.writeExc("Auction Won");

        else if (type == SWGAuctionerData.Type.INSTANT_PURCHASE)
            wr.writeExc("Instant Purchase");

        else if (type == SWGAuctionerData.Type.VENDOR_PURCHASE)
            wr.writeExc("Vendor Purchase");

        else
            wr.writeExc("ERROR");

        wr.writelnExc("\"");
    }

    /**
     * Write a header for the CSV file
     * 
     * @param wr the file writer to use
     * @throws Exception if there is an error
     */
    private void writeHeader(ZWriter wr) throws Exception {
        wr.writeExc("# Auction mails printed by SWGAide: ");
        wr.writeExc(SWGAide.time());
        wr.writeExc(" -- ");
        wr.writelnExc(SWGConstants.swgAideURL);

        wr.writeExc("# ");
        wr.writeExc(character.galaxy().station().getName());
        wr.writeExc("/");
        wr.writeExc(character.galaxy().getName());
        wr.writeExc("/");
        wr.writelnExc(character.getName());

        wr.writeExc("\"Date\",");
        wr.writeExc("\"Character\",");
        wr.writeExc("\"Item\",");
        wr.writeExc("\"Price\",");
        wr.writeExc("\"Vendor\",");
        wr.writeExc("\"Location\",");
        wr.writelnExc("\"Transaction Type\"");
    }

    /**
     * This class renders Dates in table cells on the style:
     * getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    static class DateRenderer extends DefaultTableCellRenderer {

        // table.setDefaultRenderer(Date.class, new DateRenderer());

        /**
         * A formatter for dates
         */
        DateFormat df;

        /**
         * Creates a date renderer on the format
         * getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
         */
        public DateRenderer() {
            super();
            df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT);
        }

        /**
         * Returns a formatted string from <code>value</code>
         * 
         * @param value the object to format
         * @return a formatted string from <code>value</code>
         */
        public String getText(Date value) {
            return df.format(value);
        }

        @Override
        public void setValue(Object value) {
            setText(df.format((Date) value));
        }
    }

    /**
     * This class renders Item names in table cells where "blank names are
     * rendered blue
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    static class ItemNameRenderer extends DefaultTableCellRenderer {

        // TODO : change this completely
        // table.setDefaultRenderer(Date.class, new DateRenderer());

        /**
         *
         */
        final String post = "</font>";

        /**
         *
         */
        final String pre = "<html><font color=\"blue\">";

        /**
         * Creates an item name renderer
         */
        public ItemNameRenderer() {
            super();
        }

        /**
         * Returns value possibly wrapped in HTML
         * 
         * @param value the value to possibly wrap in HTML
         * @return value possibly wrapped in HTML
         */
        public String getText(String value) {
            if (value.endsWith(": ¤")) { return (pre + value + post); }
            return value;
        }

        @Override
        public void setValue(Object value) {
            setText(getText((String) value));
        }
    }

    /**
     * A local wrapper class for Customers, enabling sorting on number unique
     * visits and amounts of credits spent
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    class SWGTradeCustomer {

        /**
         * The number of items bought by this customer
         */
        int itemCount = 1;

        /**
         * The date for the last visit for this customer
         */
        long lastVisit = 0;

        /**
         * The name of this customer
         */
        String name;

        /**
         * The total sum of credits spent by this customer
         */
        long sum = 0;

        /**
         * The total number of visits paid by this customer
         */
        int visits = 1;
    }

    /**
     * A wrapper for an Item providing data such as income and numbers sold
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    class SWGTradeItem {

        /**
         * The income from this kind of item
         */
        long income = 0;

        /**
         * The name of this kind of item
         */
        String name;

        /**
         * The number of this item that is sold
         */
        int numbersSold = 0;
    }

    /**
     * A wrapper for a Vendor providing data such as income and number of items
     * sold
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    class SWGTradeVendor {

        /**
         * The total income for this vendor
         */
        long income = 0;

        /**
         * The number of sold items for this vendor
         */
        int itemsSold = 0;

        /**
         * The name of this vendor
         */
        String name;
    }
}
