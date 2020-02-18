package swg.gui.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import swg.SWGAide;
import swg.gui.SWGFrame;

/**
 * A GUI dialog for Theme chooser
 * 
 * @author Mr-Miagi
 */
@SuppressWarnings("serial")
public class ThemeChooserPanel extends JDialog implements ActionListener, ItemListener {
	
    
    private ButtonGroup TG;
    
    
    /**
     * A boolean flag which indicates whether this dialog is open or closed.
     */
   private boolean isOpen = false;
   
   /**
    * Helper flag to cancel action events while updating the GUI.
    */
   private boolean isWorking = false;
   
   /**
    * The cancel button.
    */
   private JButton cancelButton;
   
   /**
    * The Save button.
    */
   private JButton saveButton;
   
   /**
    * The Sample Text.
    */
   private JLabel sLabel;
   
   private JRadioButton STD;
   private JRadioButton SWG;
   private JRadioButton Dark;
	
	/**
     * Creates an instance of this options dialog.
     * 
     * @param frame
     *            the application's GUI frame
     */
    public ThemeChooserPanel(final SWGFrame frame) {
        super(frame, "Theme Chooser (requires restart)", false);

        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent e) {
                if (isOpen)
                    return;
                focusGained();
                super.windowActivated(e);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                actionClose();
                super.windowClosing(e);
            }
        });
    }
    
    /**
     * Initialize everything.
     */
    private void init() {
    	JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 7, 7, 7));

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createTitledBorder("Theme Chooser (requires restart)"));
        
        sLabel = new JLabel("Choose Theme", SwingConstants.CENTER);
        outer.add(sLabel, BorderLayout.NORTH);
        
        STD = new JRadioButton();
        STD.addItemListener(this);
        SWG = new JRadioButton();
        SWG.addItemListener(this);
        Dark = new JRadioButton();
        Dark.addItemListener(this);
        TG = new ButtonGroup();
        STD.setText("Standard (light)");
        SWG.setText("SWG (darkish)");
        Dark.setText("Midnight (dark)");
        TG.add(STD);
        TG.add(SWG);
        TG.add(Dark);
        Box radio = Box.createVerticalBox();
        radio.add(STD);
        radio.add(SWG);
        radio.add(Dark);
        
        MetalTheme theme = MetalLookAndFeel.getCurrentTheme();
        String tname = theme.getName();
        switch (tname) {
        	case "Std" :
        		STD.setSelected(true);
        		sLabel.setText(STD.getText() + " selected");
        		break;
        	case "MidnightDark" :
        		Dark.setSelected(true);
        		sLabel.setText(Dark.getText() + " selected");
        		break;
        	case "SWGDark" :
        		SWG.setSelected(true);
        		sLabel.setText(SWG.getText() + " selected");
        		break;
        }
        
        outer.add(radio, BorderLayout.CENTER);
        
        Box buttons = Box.createHorizontalBox();
        saveButton = new JButton("Save");
        saveButton.setToolTipText("Save your choice");
        saveButton.setMnemonic('S');
        saveButton.addActionListener(this);
        buttons.add(saveButton);

        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('C');
        cancelButton.addActionListener(this);
        buttons.add(cancelButton);
        
        contentPane.add(outer);
        contentPane.add(buttons);
        this.setContentPane(contentPane);
        this.pack();
        this.setMinimumSize(this.getSize());
        
        setMinimumSize(new Dimension(400, 300));

        Point pp =
            (Point) SWGFrame.getPrefsKeeper().get("optionsGeneralLocation");
        if (pp == null) {
            pp = this.getLocation();
            pp.x += 50;
            pp.y += 20;
        } else
            pp = SWGGuiUtils.ensureOnScreen(pp, getSize());
        setLocation(pp);
		
	}
    
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == STD) {
            if (e.getStateChange() == 1) {
                sLabel.setText(STD.getText() + " selected");
            }
        }
        if (e.getSource() == SWG) {
            if (e.getStateChange() == 1) {
            	sLabel.setText(SWG.getText() + " selected");
            }
        }
        if (e.getSource() == Dark) {
            if (e.getStateChange() == 1) {
            	sLabel.setText(Dark.getText() + " selected");
            }
        }
    }

	/**
	 * Performs actions for Save/Cancel buttons
	 */
    public void actionPerformed(ActionEvent e) {
	    if (isWorking) return;
	    Object src = e.getSource();
	    if (src == cancelButton) {
	        // pass go and collect $200
	    } else if (src == saveButton) {
	    	saveButton.setEnabled(false); // user may click twice on a laggy host
	    	if(STD.isSelected()) {
	    		SWGGuiUtils.setTheme("Std");
	    	}
	    	if(SWG.isSelected()) {
	    		SWGGuiUtils.setTheme("SWGDark");
	    	}
	    	if(Dark.isSelected()) {
	    		SWGGuiUtils.setTheme("MidnightDark");
	    	}
	    	showConfirm();
	    	saveButton.setEnabled(true);
	    } else
	        SWGAide.printError("ThemeChooserPanel:actionPerformed" + src, null);
	
	    actionClose();
	}

	/**
	 * This method is called when this dialog window is closed and it saves
	 * preferences to SWGAide's preference keeper.
	 */
	private void actionClose() {
		SWGFrame.getPrefsKeeper().add("optionsGeneralLocation",
	            getLocation());
	    isOpen = false;
	    setVisible(false);
	}
	
	/**
     * This method is called when this GUI dialog gains focus.
     */
    private void focusGained() {
        isOpen = true;
    }
    
    /**
     * Override setVisible so all values are re-initialized
     */
    @Override
    public void setVisible(boolean bVisible)
    {
        if(bVisible == false)
        {
            super.setVisible(bVisible);
            return;
        }

        init();
        super.setVisible(bVisible);
        return;
    }
    private void showConfirm () {
    	JOptionPane pane = new JOptionPane("\nThe application must be restarted to apply changes\n"
        		+ "Please restart the application after clicking OK.\nThank You",JOptionPane.PLAIN_MESSAGE);
        JDialog d = pane.createDialog(null, "SWGAide Requires Restart");
        d.pack();
        d.setModal(true);
        d.setVisible(true);
        while (pane.getValue() == JOptionPane.UNINITIALIZED_VALUE) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
        	  //
          }
        }
        // call the doExit() so things get saved to the DAT
        SWGFrame.doExit();
    }
}
