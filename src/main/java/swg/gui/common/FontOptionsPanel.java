package swg.gui.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import swg.SWGAide;
import swg.gui.SWGFrame;

/**
 * A GUI dialog for Font options
 * 
 * @author Mr-Miagi
 */
@SuppressWarnings("serial")
public class FontOptionsPanel extends JDialog implements ActionListener {
	
	/**
     * The cancel button.
     */
    private JButton cancelButton;
    
	/**
	 * The slider for font factor
	 */
    private JSlider fslider;
    
    /**
     * A boolean flag which indicates whether this dialog is open or closed.
     */
   private boolean isOpen = false;
   
   /**
    * Helper flag to cancel action events while updating the GUI.
    */
   private boolean isWorking = false;
   
   /**
    * The Save button.
    */
   private JButton saveButton;
   
   /**
    * The Sample Text.
    */
   private JLabel sLabel;
	
	/**
     * Creates an instance of this options dialog.
     * 
     * @param frame
     *            the application's GUI frame
     */
    public FontOptionsPanel(final SWGFrame frame) {
        super(frame, "Font Options (requires restart)", false);

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
        outer.setBorder(BorderFactory.createTitledBorder("Font Size Factor (requires restart)"));
        
        sLabel = new JLabel("Sample Text", SwingConstants.CENTER);
        outer.add(sLabel, BorderLayout.NORTH);

        fslider = new JSlider(SwingConstants.HORIZONTAL, 100, 280, Integer.parseInt(SWGGuiUtils.getFontSizeParam()));
        fslider.setMajorTickSpacing(10);
        fslider.setPaintLabels(true);
        fslider.setPaintTicks(true);
        fslider.setSnapToTicks(true);
        fslider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
            	JSlider source = (JSlider)ce.getSource();
                if(!source.getValueIsAdjusting()) {
                	actionSlider(ce);
                }
            }
        });
        fslider.setPreferredSize(new Dimension(Math.round(500*SWGGuiUtils.fontMultiplier()), Math.round(100*SWGGuiUtils.fontMultiplier())));
        outer.add(fslider, BorderLayout.CENTER);
        
        Box buttons = Box.createHorizontalBox();
        saveButton = new JButton("Save");
        saveButton.setToolTipText("Save the option");
        saveButton.setMnemonic('S');
        saveButton.addActionListener(this);
        buttons.add(saveButton);

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
    /**
     * Changes sample text in options window when slider is moved.
     * 
     * @param ce
     */
    protected void actionSlider(ChangeEvent ce) {
		Font f = SWGGuiUtils.fontBold();
		float multiplier = fslider.getValue() / 100.0f;
		int newSize = Math.round(SWGGuiUtils.defFontSize() * multiplier);
		sLabel.setFont(new Font(f.getName(), f.getStyle(), newSize));
	}

	/**
	 * Performs actions for Save and Cancel buttons
	 */
    public void actionPerformed(ActionEvent e) {
	    if (isWorking) return;
	
	    Object src = e.getSource();
	    if (src == cancelButton) {
	        // pass go and collect $200
	    } else if (src == saveButton) {
	    	saveButton.setEnabled(false); // user may click twice on a laggy host
	    	SWGGuiUtils.setFontSize(fslider.getValue());
	    	showConfirm();
	    	saveButton.setEnabled(true);
	    } else
	        SWGAide.printError("FontOptionsPanel:actionPerformed" + src, null);
	
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
