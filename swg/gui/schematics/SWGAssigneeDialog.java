package swg.gui.schematics;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import swg.SWGAide;
import swg.crafting.schematics.SWGProfession;
import swg.crafting.schematics.SWGSchematic;
import swg.crafting.schematics.SWGSchematicsManager;
import swg.gui.SWGFrame;
import swg.gui.common.SWGGui;
import swg.gui.common.SWGGuiUtils;
import swg.gui.common.SWGJDialog;
import swg.gui.common.SWGListCellRenderer;
import swg.gui.common.SWGListModel;
import swg.model.SWGCGalaxy;

/**
 * This type is a dialog for managing schematics assignees. The GUI elements are
 * a list of schematics to the the west, a panel in the middle with filters,
 * left/right buttons and a list of defined assignees and means to create new
 * assignees, and a list of favorite schematics to the east. The western list is
 * filtered by profession and the eastern list is the schematics for the
 * selected assignee.
 * <p>
 * This type presents the user with logic to easily select schematics for a
 * selected assignee, to withdraw schematics from an assignee, to define and to
 * and to delete an assignee.
 * <p>
 * This type is provided as a dialog rather than a tabbed panel because it will
 * not be used as often as a panel is supposed to. Should this turn out to be
 * wrong this type is implemented in a way that it is easily converted to a
 * panel which can be added to the tabbed pane.
 * <p>
 * This type <i>is not </i> made to manage schematics wrappers, these are a
 * different animal. This is the relationship: a schematics assignee has a list
 * of favorite schematics, a schematic may be listed as favorite by many
 * assignees. An assignee has (TODO) a list of schematic wrappers which are used
 * at the schematics inventory panel. Hence, when these features are implemented
 * this dialog must guard against deleting an assignee that also lists inventory
 * items.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
final class SWGAssigneeDialog extends SWGJDialog {

    /**
     * A text field at which to define an assignee.
     */
    private JTextField assigneeDefine;

    /**
     * A list for schematic assignees.
     */
    private JList<SWGSchematicAssignee> assigneeList;

    /**
     * A list of favorite schematics for a selected assignee.
     */
    private JList<SWGSchematic> favoriteList;

    /**
     * A combo box for selecting a profession.
     */
    private JComboBox<SWGProfession> professionChooser;

    /**
     * A list for all schematics or a filtered set.
     */
    private JList<SWGSchematic> schematicsList;

    /**
     * Creates an instance of this type.
     * 
     * @param schemTab the GUI element that owns this instance
     */
    SWGAssigneeDialog(SWGSchematicTab schemTab) {
        super("Manage Assignees and Favorites", schemTab);

        makeInterior();
    }

    /**
     * Called when the user right-clicks at the list of assignees. This method
     * displays a popup menu with options to delete and to rename assignees; see
     * {@link SWGSchematicTab#assigneeRemove(SWGSchematicAssignee)}.
     * 
     * @param e the action that triggers the call
     */
    private void actionAssignee(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();

        final SWGSchematicAssignee as = assigneeList.getSelectedValue();

        JMenuItem del = new JMenuItem("Delete assignee...");
        del.setToolTipText("Delete the selected assignee");
        del.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ee) {
                ((SWGSchematicTab)parent).assigneeRemove(as);
                updateAssignees();
            }
        });
        del.setEnabled(as != null);
        popup.add(del);

        JMenuItem rename = new JMenuItem("Rename assignee...");
        rename.setToolTipText("Delete the selected assignee");
        rename.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ee) {
                actionAssigneeRename(as);
            }
        });
        rename.setEnabled(as != null);
        popup.add(rename);

        popup.show(assigneeList, e.getX(), e.getY());
    }

    /**
     * Called when the user has defined an assignee. This method determines that
     * the input is valid, if not this method does nothing, otherwise it invokes
     * {@link SWGSchematicTab#assigneeAdd(SWGSchematicAssignee)}.
     */
    private void actionAssigneeDefine() {
        String an = assigneeDefine.getText();
        if (an == null || an.trim().isEmpty()) return;

        ((SWGSchematicTab)parent).assigneeAdd(new SWGSchematicAssignee(an));
        updateAssignees();
        assigneeDefine.setText(null);
    }

    /**
     * Called when the user selects to rename an assignee. This method displays
     * a dialog with a text input field. If the input defines a unique name the
     * specified assignee is renamed, otherwise this method alerts the user and
     * aborts.
     * 
     * @param as a schematics assignee
     */
    private void actionAssigneeRename(SWGSchematicAssignee as) {
        String resp = (String) JOptionPane.showInputDialog(this,
                "Define a unique name", "Rename assignee",
                JOptionPane.PLAIN_MESSAGE, null, null, null);

        if (resp == null) return;

        resp = resp.trim();
        if (!((SWGSchematicTab)parent).assigneeIsUnique(resp)) {
            JOptionPane.showMessageDialog(this, "\"" + resp
                    + "\" is not unique", "Abort", JOptionPane.ERROR_MESSAGE);
            return;
        }

        as.setAssignee(resp);

    }

    /**
     * Called when the user selects an assignee. This method updates the list of
     * favorite schematic.
     * 
     * @param as the selected assignee
     */
    private void actionAssigneeSelected(SWGSchematicAssignee as) {
        if (as == null) return;
        ((SWGListModel<SWGSchematic>) favoriteList.getModel()).setElements(as.getFavorites());
    }

    /**
     * Called when the user clicks the mouse at the list of favorite schematics.
     * If the action is a double click the schematic under the mouse is obtained
     * and this method invokes {@link #remSfromA(SWGSchematic)}.
     * 
     * @param e the action that triggers the call
     */
    private void actionFavoriteList(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1
                && e.getClickCount() > 1) { // double-click

            int i = favoriteList.locationToIndex(e.getPoint());
            if (i < 0) return;

            remSfromA(favoriteList.getModel().getElementAt(i));
        }
    }

    /**
     * Called when the user clicks the mouse at the list of schematics. If the
     * action is a double click the schematic under the mouse is obtained and
     * this method invokes {@link #addStoA(SWGSchematic)}.
     * 
     * @param e the event that triggers the call
     */
    private void actionSchematicList(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1
                && e.getClickCount() > 1) { // double-click
            int idx = schematicsList.locationToIndex(e.getPoint());
            if (idx < 0) return;

            addStoA(schematicsList.getModel().getElementAt(idx));
        }
    }

    /**
     * Helper method which adds the specified schematic to a selected assignee,
     * see {@link SWGSchematicAssignee#addFavorite(SWGSchematic)}. If the
     * schematic is {@code null} or if no assignee is selected a dialog alerts
     * the user and this method does nothing.
     * 
     * @param s a schematic
     */
    private void addStoA(SWGSchematic s) {
        SWGSchematicAssignee a = assigneeList.getSelectedValue();

        if (s == null || a == null) {
            alertSelectBoth();
        } else {
            a.addFavorite(s);
            actionAssigneeSelected(a);
            ((SWGSchematicTab)parent).notifyAssigneeUsers();
        }
    }

    /**
     * Helper method that displays an information message that the user must
     * select both a schematic and an assignee.
     */
    private void alertSelectBoth() {
        JOptionPane.showMessageDialog(this,
                "Select both a schematic and an assignee",
                "Make selections", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    protected void close() {
        // pass
    }

    /**
     * Helper method which creates and returns the center GUI element. This
     * element contains...
     * <ul>
     * <li>a profession filter which limits the content at the western list of
     * schematics</li>
     * <li>buttons which adds/removes schematics to/from the eastern list of
     * favorite schematic</li>
     * <li>an editable list of assignees, selecting an assignee populates the
     * eastern list of favorite schematics</li>
     * </ul>
     * <p>
     * This method invokes helper methods for each of these three elements.
     * 
     * @return a GUI element
     */
    private Component makeCenter() {
        Box vb = new Box(BoxLayout.Y_AXIS) {
            @Override
            public Dimension getMinimumSize() {
                Dimension d = super.getMinimumSize();
                d.width = 85;
                return d;
            }
        };

        vb.add(makeCenterProfessionChooser());
        vb.add(Box.createVerticalStrut(3));
        vb.add(makeCenterButtons());
        vb.add(Box.createVerticalStrut(3));
        vb.add(makeCenterAssigneeChooser());

        vb.add(Box.createVerticalGlue());

        return vb;
    }

    /**
     * Helper method which creates and returns the assignee chooser and a text
     * input field. The list displays the names of the schematic assignees and
     * its selection listener updates the eastern list of favorite schematics.
     * At the text input field it is possible to define a new assignee.
     * 
     * @return a GUI element
     */
    private Component makeCenterAssigneeChooser() {
        assigneeList = new JList<SWGSchematicAssignee>(new SWGListModel<SWGSchematicAssignee>());
        assigneeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assigneeList.setToolTipText("Select the assignee to manage");

        assigneeList.setCellRenderer(new SWGListCellRenderer<SWGSchematicAssignee>() {
            @Override
            protected String labelString(SWGSchematicAssignee value) {
                return value.getName();
            }
        });

        assigneeList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    actionAssigneeSelected(assigneeList.getSelectedValue());
            }
        });
        assigneeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3)
                    actionAssignee(e);
            }
        });

        assigneeDefine = new JTextField(10) {
            @Override
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = 20;
                float m = SWGGuiUtils.fontMultiplier();
                d.height = Math.round(d.height * m);
                return d;
            }
        };
        assigneeDefine.setToolTipText("Define an assignee and hit Enter");
        assigneeDefine.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionAssigneeDefine();
            }
        });

        Box nest = Box.createVerticalBox();
        Box vb = Box.createVerticalBox();
        vb.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Assignees "));

        vb.add(new JScrollPane(assigneeList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        vb.add(Box.createVerticalStrut(3));
        Box vvb = Box.createVerticalBox();
        vvb.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Create New Assignee "));
        vvb.add(assigneeDefine);
        nest.add(vb);
        nest.add(vvb);

        return nest;
    }

    /**
     * Helper method which creates and returns an element with buttons which
     * adds/removes schematics to/from the eastern list of favorite schematics.
     * Each button invokes a helper action method in this type.
     * 
     * @return a GUI element
     */
    private Component makeCenterButtons() {
        JButton add = new JButton(new ImageIcon(
                SWGGui.class.getResource("images/Forward16.gif")));
        add.setToolTipText(
                "Add selected schematic (left-hand side) to the assignee");
        add.setMnemonic(KeyEvent.VK_RIGHT);
        add.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addStoA(schematicsList.getSelectedValue());
            }
        });

        JButton remove = new JButton(new ImageIcon(
                SWGGui.class.getResource("images/Back16.gif")));
        remove.setMnemonic(KeyEvent.VK_LEFT);
        remove.setToolTipText(
                "Remove selected schematic (right-hand side) from the assignee");
        remove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                remSfromA(favoriteList.getSelectedValue());
            }
        });

        JPanel bp = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = 40;
                return d;
            }
        };
        bp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Add / Remove "));
        bp.setLayout(new BorderLayout(0, 3));

        bp.add(add, BorderLayout.PAGE_START);
        bp.add(remove, BorderLayout.CENTER);

        return bp;
    }

    /**
     * Helper method which creates and returns the profession chooser. This is a
     * combo-box with the names of professions. Its action listener updates the
     * western list of schematics.
     * 
     * @return a GUI element
     */
    private Component makeCenterProfessionChooser() {
    	SWGCGalaxy gxy = SWGFrame.getSelectedGalaxy();
        List<String> pls = SWGProfession.getNames(gxy.getType());
        professionChooser = new JComboBox<SWGProfession>((ComboBoxModel<SWGProfession>) pls) {
            @Override
            public Dimension getMaximumSize() {
                Dimension d = super.getMaximumSize();
                d.height = 23;
                return d;
            }
        };
        professionChooser.setToolTipText(
                "Select a profession for the left-hand list of schematics");
        professionChooser.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                        " Profession "));
        professionChooser.setRenderer(new SWGListCellRenderer<SWGProfession>() {
            @Override
            protected String labelString(SWGProfession value) {
                return value.getName();
            }
        });

        professionChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SWGProfession p = (SWGProfession)
                        professionChooser.getSelectedItem();

                List<SWGSchematic> sl = SWGSchematicsManager.getSchematics(p);

                Collections.sort(sl);
                ((SWGListModel<SWGSchematic>) schematicsList.getModel()).setElements(sl);
            }
        });

        return professionChooser;
    }

    /**
     * Helper method which creates and returns a list for favorite schematics.
     * This list is populated with favorites for the selected assignee. The only
     * action listener which is available at this element invokes
     * {@link #actionFavoriteList(MouseEvent)}.
     * 
     * @return a GUI element
     */
    private Component makeEast() {
        favoriteList = new JList<SWGSchematic>(new SWGListModel<SWGSchematic>());
        favoriteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        favoriteList.setToolTipText("Select a schematic to remove");

         favoriteList.setCellRenderer(new SWGListCellRenderer<SWGSchematic>() {
            @Override
            protected String labelString(SWGSchematic value) {
                return value.getName();
            }
        });

        favoriteList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionFavoriteList(e);
            }
        });

        final JDialog dg = this;
        JScrollPane jsp = new JScrollPane(favoriteList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int wl = favoriteList.getPreferredSize().width * 2 / 3;
                int wd = dg.getWidth() / 3;
                wl = wd > wl
                        ? wd
                        : wl;
                d.width = wl;
                return d;
            }
        };
        jsp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Favorites "));
        return jsp;
    }

    /**
     * Helper method which populates this dialog and returns its content pane.
     * This method invokes {@link #makeWest()}, {@link #makeCenter()}, and
     * {@link #makeEast()}.
     */
    private void makeInterior() {
        super.setMinimumSize(new Dimension(800, 400));

        JPanel hp = new JPanel(new BorderLayout());

        hp.add(makeWest(), BorderLayout.LINE_START);
        hp.add(makeCenter(), BorderLayout.CENTER);
        hp.add(makeEast(), BorderLayout.LINE_END);

        this.add(hp);
        registerHelp(SWGAide.class.getResource(
                "docs/help_schematics_manage_assignees_en.html"));
    }

    /**
     * Helper method which creates and returns a list for schematics. This list
     * is populated with all or a filtered set of schematics. The only action
     * listener which is available at this element invokes
     * {@link #actionSchematicList(MouseEvent)}.
     * 
     * @return a GUI element
     */
    private Component makeWest() {
        schematicsList = new JList<SWGSchematic>(new SWGListModel<SWGSchematic>());
        schematicsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        schematicsList.setToolTipText(
                "Select a schematic to be added to the selected assignee");

        schematicsList.setCellRenderer(new SWGListCellRenderer<SWGSchematic>() {
            @Override
            protected String labelString(SWGSchematic value) {
                return value.getName();
            }
        });

        schematicsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                actionSchematicList(e);
            }
        });

        final JDialog dg = this;
        JScrollPane jsp = new JScrollPane(schematicsList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                int wl = schematicsList.getPreferredSize().width * 2 / 3;
                int wd = dg.getWidth() / 3;
                wl = wd > wl
                        ? wd
                        : wl;
                d.width = wl;
                return d;
            }
        };
        jsp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                " Schematics "));
        return jsp;
    }

    /**
     * Helper method which removes the specified schematic from a selected
     * assignee, see {@link SWGSchematicAssignee#removeFavorite(SWGSchematic)}.
     * If the schematic is {@code null} or if no assignee is selected a dialog
     * alerts the user and this method does nothing.
     * 
     * @param s a schematic
     */
    private void remSfromA(SWGSchematic s) {
        SWGSchematicAssignee a = assigneeList.getSelectedValue();

        if (s == null || a == null) {
            alertSelectBoth();
        } else {
            a.removeFavorite(s);
            actionAssigneeSelected(a);
            ((SWGSchematicTab)parent).notifyAssigneeUsers();
        }
    }

    @Override
    public void setVisible(boolean b) {
        if (!b) {
            SWGFrame.getPrefsKeeper().add(
                    "schemAssigneeDialogLocation", getLocationOnScreen());
            super.setVisible(false);
            return;
        }

        if (schematicsList.getModel().getSize() <= 0) // first time
            professionChooser.setSelectedItem(SWGProfession.ALL);

        updateAssignees();

        pack();
        Point p = parent.getLocationOnScreen();
        p = SWGGuiUtils.ensureOnScreen((Point) SWGFrame.getPrefsKeeper().get(
                "schemAssigneeDialogLocation", p), getMaximumSize());
        setLocation(p);
        super.setVisible(true);
    }

    /**
     * Updates the model for the GUI list of assignees. If an element is
     * selected the selection is retained.
     */
    void updateAssignees() {
        // always set assignees, in the case the list is updated while invisible
        Object o = assigneeList.getSelectedValue();
        assigneeList.clearSelection();
        List<SWGSchematicAssignee> as = SWGSchematicTab.assignees();
        ((SWGListModel<SWGSchematicAssignee>) assigneeList.getModel()).setElements(as);
        if (o != null) assigneeList.setSelectedValue(o, true);
    }
}
