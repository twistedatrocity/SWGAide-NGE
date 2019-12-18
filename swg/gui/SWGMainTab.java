package swg.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import swg.SWGAide;
import swg.gui.mail.SWGMailPane;
import swg.gui.tree.SWGGalaxyNode;
import swg.gui.tree.SWGRoot;
import swg.gui.tree.SWGStationNode;
import swg.gui.tree.SWGTreeNode;
import swg.gui.tree.SWGUniverseNode;
import swg.model.SWGCGalaxy;
import swg.model.SWGCharacter;
import swg.model.SWGGalaxy;
import swg.model.SWGUniverse;

/**
 * This class encompasses all GUI components viewed at the main tab. In short
 * that is the tree for the SWG universe and the optional TestCenter universe.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGMainTab extends JSplitPane {

    /**
     * The frame containing the tabbed pane holding this view
     */
    SWGFrame frame;

    /**
     * Container for nodes that user has chosen to be hidden
     */
    public Hashtable<String, String> hiddenNodes;

    /**
     * The mail client to be shown for the selected character
     */
    public SWGMailPane mailClient = null;

    /**
     * The GUI tree in the main view
     */
    public JTree tree;

    /**
     * The root of the GUI tree for the main view.
     */
    private SWGRoot treeRoot;

    /**
     * a menu item for un/hiding nodes
     */
    private JMenuItem unhideMenuItem;

    /**
     * Creates a split pane that holds the GUI components of the main view
     * 
     * @param owner the frame holding the tabbed pane for this object
     */
    public SWGMainTab(SWGFrame owner) {
        this.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        frame = owner;

        unhideMenuItem();

        makeTree();
        tree.setCellRenderer(new DefaultTreeCellRenderer() {

            final Color noExist = new Color(204, 0, 102);

            @Override
            public Component getTreeCellRendererComponent(JTree t, Object val,
                    boolean sel, boolean exp, boolean leaf, int row, boolean hf) {

                super.getTreeCellRendererComponent(
                        tree, val, sel, exp, leaf, row, hf);

                setForeground(((SWGTreeNode) val).exists()
                        ? Color.BLACK
                        : noExist);
                return this;
            }
        });

        tree.setRootVisible(false);
        tree.setMinimumSize(new Dimension(200, 150));
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

        if (SWGTreeNode.focusedNode() != null) {
            TreePath p = new TreePath(SWGTreeNode.focusedNode().getPath());
            tree.setSelectionPath(p);
            tree.expandPath(p);
        } else
            tree.expandRow(0);

        JScrollPane leftPane = new JScrollPane(tree);
        leftPane.setMinimumSize(new Dimension(200, 150));
        this.setLeftComponent(leftPane);
        this.setRightComponent(new JLabel());

        this.setDividerLocation(-1);
        this.setDividerSize(5);

        MouseListener ml = new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                if (selRow >= 0) {
                    TreePath selPath = tree.getClosestPathForLocation(
                            e.getX(), e.getY());
                    tree.setSelectionPath(selPath);
                    treeMouseClicked(e, selPath);
                }
            }
        };
        tree.addMouseListener(ml);

        tree.getInputMap().put(KeyStroke.getKeyStroke("SPACE"),
                "treeNodeAction");
        tree.getActionMap().put("treeNodeAction", new AbstractAction() {
            
            public void actionPerformed(ActionEvent e) {
                treeSpaceTyped(tree.getSelectionPath());
            }
        });

        frame.getTabPane().addChangeListener(new ChangeListener() {
            
            public void stateChanged(ChangeEvent e) {
                focusChanged(e);
            }
        });
    }

    /**
     * Called whenever this tabbed pane is de/selected, i.e. focus is changed
     * 
     * @param evt the change event
     */
    private void focusChanged(ChangeEvent evt) {
        SWGTreeNode fn = SWGTreeNode.focusedNode();
        if (frame.getTabPane().getSelectedComponent() == this) {
            frame.editMenuAdd(unhideMenuItem);
            SWGTreeNode.focusTransition(fn, evt);

        } else {
            frame.editMenuRemove(unhideMenuItem);
            SWGTreeNode.focusTransition(null, evt);
        }
    }

    /**
     * Returns a reference to the mail client of this application
     * 
     * @return a reference to the mail client of this application
     */
    public SWGMailPane getMailClient() {
        return mailClient;
    }

    /**
     * Helper method which creates a tree for the GUI. If there is an error it
     * is caught and written to SWGAide's log file and a dummy tree is created.
     */
    @SuppressWarnings("unchecked")
    private void makeTree() {
        hiddenNodes = (Hashtable<String, String>) SWGFrame.getPrefsKeeper().
                get("mainTabHiddenNodes", new Hashtable<String, String>(97));
        updateHiddenNodes(hiddenNodes);

        try {
            DefaultTreeModel model = new DefaultTreeModel(makeTreeModel());
            tree = new JTree(model);
        } catch (Throwable e) {
            SWGAide.printError("SWGMainTab:makeTree", e);
            JOptionPane.showMessageDialog(frame,
                    "Error creating GUI tree, see log\n" +
                            "file and report at swgaide.com\n\n" +
                            "Exiting", "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    /**
     * Returns a root node containing an SWG universe and optionally a
     * testcenter node. This method populates the tree branching from this root.
     * 
     * @return the root node of a tree containing an SWG universe with
     *         descendants
     */
    private SWGRoot makeTreeModel() {
        treeRoot = new SWGRoot(frame, this);

        SWGUniverse u;
        u = (SWGUniverse) SWGFrame.getPrefsKeeper().get("swgUniverse");
        if (u != null) {
	        if (u.exists()) {
				try {
					SWGInitialize.scanAll(u, false);
				} catch (Exception e) {
					SWGAide.printError("SWGMainTab:makeTreeModel:", e);
				}
	        }
        }
        SWGRoot.createPopulatedTree(u, treeRoot, this);

        return treeRoot;
    }

    /**
     * Returns a sorted list of galaxy constants for the currently selected
     * universe, or an empty list. The elements denotes galaxies that are
     * visible in at least one station at SWGAide's main display. The list is
     * sorted alphabetically by the names of the galaxies.
     * 
     * @return a list of galaxy constants
     */
    public List<SWGCGalaxy> galaxies() {
        synchronized (treeRoot) {
            List<SWGCGalaxy> ret = new ArrayList<SWGCGalaxy>();

            SWGCharacter ch = SWGFrame.getSelectedCharacter();
            if (ch == null) return ret;

            SWGUniverseNode un = treeRoot.findUniverse(
                    ch.galaxy().station().universe());

            for (Enumeration<?> se = un.children(); se.hasMoreElements();) {
                Object os = se.nextElement();
                if (os instanceof SWGStationNode) {
                    SWGStationNode sn = (SWGStationNode) os;
                    for (Enumeration<?> ge = sn.children(); ge.hasMoreElements();) {
                        Object o = ge.nextElement();
                        if (o instanceof SWGGalaxyNode) {
                            SWGGalaxyNode gn = (SWGGalaxyNode) o;
                            SWGGalaxy g = (SWGGalaxy) gn.getUserObject();
                            if (!ret.contains(g.gxy()))
                                ret.add(g.gxy());
                        }
                    }
                }
            }
            Collections.sort(ret, new Comparator<SWGCGalaxy>() {
                @Override
                public int compare(SWGCGalaxy o1, SWGCGalaxy o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            return ret;
        }
    }

    /**
     * Sets the main pane to default state
     */
    public void setMainPaneDefault() {
        setRightComponent(new JLabel());
        if (mailClient != null) {
            mailClient.focusGained(false);
        }
    }

    /**
     * Shows the aliases text component for the current universe
     * 
     * @param aliasesPane the aliases text component to display
     */
    public void showAliases(SWGAliasesPane aliasesPane) {
        JScrollPane scr = new JScrollPane(aliasesPane);
        this.setRightComponent(scr);
    }

    /**
     * Shows the macros text component for the current station
     * 
     * @param macrosPane the macros text component to display
     */
    public void showMacros(SWGMacrosPane macrosPane) {
        JScrollPane scr = new JScrollPane(macrosPane);
        this.setRightComponent(scr);
    }

    /**
     * Creates and shows the mail client
     */
    public void showMail() {
        if (mailClient == null) {
            mailClient = new SWGMailPane(frame);
        }
        setRightComponent(mailClient);
    }

    /**
     * Shows the notes text component for the current station
     * 
     * @param notesPane the notes text component to display
     */
    public void showNotes(SWGNotesPane notesPane) {
        JScrollPane scr = new JScrollPane(notesPane);
        this.setRightComponent(scr);
    }

    /**
     * Takes care of passing the user action to the correct node
     * 
     * @param evt the mouse event causing the event
     * @param selPath the path to the selected node
     */
    private void treeMouseClicked(MouseEvent evt, TreePath selPath) {
        SWGTreeNode node = (SWGTreeNode) selPath.getLastPathComponent();
        SWGTreeNode.focusTransition(node, evt);
    }

    /**
     * Takes care of passing the user action to the correct node
     * 
     * @param selPath the tree path for the selected node
     */
    private void treeSpaceTyped(TreePath selPath) {
        SWGTreeNode node = (SWGTreeNode) selPath.getLastPathComponent();
        SWGTreeNode.focusTransition(node, 
                new TreeSelectionEvent(tree, null, true, null, null));
    }

    /**
     * Creates a menu item
     */
    private void unhideMenuItem() {
        unhideMenuItem = new JMenuItem("Unhide...", KeyEvent.VK_U);
        unhideMenuItem.setToolTipText("Unhide all hidden objects");
        unhideMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (hiddenNodes.size() > 0) {
                    unhideNodes();
                }
            }
        });
    }

    /**
     * Unhides all hidden nodes
     */
    public void unhideNodes() {
        hiddenNodes.clear();
        ((DefaultTreeModel) tree.getModel()).setRoot(makeTreeModel());
    }

    /**
     * Helper method which updates all hidden nodes. Previously nodes were made
     * hidden by their absolute path but when SWGAide was made host independent
     * nodes are hidden by the string "Station[_Galaxy]" with the values from
     * respective node's toString() method.
     * 
     * @param hn a map of hidden nodes
     */
    private void updateHiddenNodes(Hashtable<String, String> hn) {
        if (SWGFrame.getPrefsKeeper().get("mainTabHiddenNodesUpdated") == null) {
            Set<String> kss = hn.keySet();
            List<String> ks = new ArrayList<String>(kss);
            kss.clear();

            String p = "profiles";
            for (String k : ks) {
                int i = k.indexOf(p);
                if (i >= 0) {
                    i += p.length() + 1; // plus file separator
                    k = k.substring(i);
                    k = k.replace(File.separatorChar, '_');

                    i = k.lastIndexOf('_') + 1;
                    if (i > 0) k = k.substring(0, i)
                            + SWGCGalaxy.fromName(k.substring(i)).getName();

                    hn.put(k, "hidden");
                }
            }

            SWGFrame.getPrefsKeeper().add("mainTabHiddenNodesUpdated", "0.9.0");
        }
    }
}
