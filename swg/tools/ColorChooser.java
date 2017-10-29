package swg.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * An enhanced dialog for choosing a color, and a text color in black or white.
 * <p>
 * Usage:<BR/>
 * Get a color chooser object 'cc'
 * 
 * <pre>cc.showDialog(bgColor, fgColor);
 // user GUI interaction
 bgColor = cc.getSelectedBackgroundColor();
 fgColor = cc.getSelectedForegroundColor();</pre>
 * <p>
 * If the user cancels the dialog the colors returned by the getters are the
 * colors which were the arguments to open this color chooser dialog.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class ColorChooser extends JDialog {

    /**
     * A check box to select black or white text for the chosen color, selected
     * means "black" and unselected is "white".
     */
    private JCheckBox blackWhite;

    /**
     * The color chooser.
     */
    private JColorChooser colorChooser;

    /**
     * The selected background color, or the old color if the dialog is
     * canceled.
     */
    private Color newColor;

    /**
     * The selected foreground color, or the old color if the dialog is
     * canceled.
     */
    private Color newTextColor;

    /**
     * A label that displays the background color and black or white text.
     */
    private JLabel previewLabel;

    /**
     * Creates a color chooser dialog.
     * 
     * @param frame a frame
     */
    public ColorChooser(JFrame frame) {
        // must be modal to allow the caller to ask for selected values after
        // this dialog is closed
        super(frame, "Color Chooser", true);

        colorChooser = new JColorChooser();

        BottomPanel bp = new BottomPanel(colorChooser.getColor());
        colorChooser.getSelectionModel().addChangeListener(bp);
        colorChooser.setPreviewPanel(bp);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(colorChooser, BorderLayout.CENTER);
        contentPane.add(bp, BorderLayout.PAGE_END);

        this.add(contentPane);
        this.pack();
        this.setMinimumSize(this.getSize());
    }

    /**
     * Helper method which closes this dialog. If the user selects "OK" the
     * selected color is made available for
     * {@link #getSelectedBackgroundColor()} and
     * {@link #getSelectedForegroundColor()}. If the user canceled this dialog
     * the argument is {@code null}.
     * 
     * @param selectedColor the selected color, or {@code null}
     */
    protected void closeDialog(Color selectedColor) {
        if (selectedColor != null) {
            // save the selected color only when OK, preserves the old color
            newColor = selectedColor;
            if (blackWhite.isSelected())
                newTextColor = Color.BLACK;
            else
                newTextColor = Color.WHITE;
        }
        setVisible(false);
    }

    /**
     * Returns the selected background color. If the user canceled this dialog
     * the return value is the argument for {@link #showDialog(Color, Color)}.
     * 
     * @return a color
     */
    public Color getSelectedBackgroundColor() {
        return newColor;
    }

    /**
     * Returns the selected text color, black or white. If the user canceled the
     * dialog the return value is the argument to open this dialog.
     * 
     * @return black or white
     */
    public Color getSelectedForegroundColor() {
        return newTextColor;
    }

    /**
     * Displays this color chooser dialog with the specified colors. See
     * {@link #getSelectedBackgroundColor()} and
     * {@link #getSelectedForegroundColor()}. For text color only black and
     * white are supported, any other color will set the text to white.
     * 
     * @param backGround an initial background color
     * @param textColor an initial background color, only black and white is
     *        supported
     */
    public void showDialog(Color backGround, Color textColor) {
        newColor = backGround;
        newTextColor = textColor;

        colorChooser.setColor(newColor);

        if (textColor.equals(Color.BLACK))
            blackWhite.setSelected(true);
        else
            blackWhite.setSelected(false);

        previewLabel.setForeground(textColor);
        setVisible(true);
    }

    /**
     * An internal preview panel.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    private class BottomPanel extends JPanel implements ChangeListener {

        /**
         * Creates the preview panel
         * 
         * @param color the initial color to use at the preview panel
         */
        @SuppressWarnings("synthetic-access")
        public BottomPanel(Color color) {
            super(new FlowLayout(FlowLayout.TRAILING));

            previewLabel = new JLabel("ffffff");
            previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
            previewLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK,
                    1));
            Dimension dim = new Dimension(60, 17);
            previewLabel.setPreferredSize(dim);
            previewLabel.setMaximumSize(dim);
            previewLabel.setToolTipText("Preview");
            previewLabel.setBackground(color);
            previewLabel.setOpaque(true);
            this.add(previewLabel);

            blackWhite = new JCheckBox("Text b/w");
            blackWhite.setToolTipText("Toggle text color b/w");
            blackWhite.setSelected(true);
            blackWhite.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (blackWhite.isSelected())
                        previewLabel.setForeground(Color.BLACK);
                    else
                        previewLabel.setForeground(Color.WHITE);
                }
            });
            this.add(blackWhite);

            // JButton darker = new JButton("darker");
            // darker.addActionListener(new ActionListener() {
            //
            // @Override
            // public void actionPerformed(ActionEvent e) {
            // Color c = previewLabel.getBackground();
            // double f = 0.95;
            // c = new Color(
            // (int) (c.getRed() * f),
            // (int) (c.getGreen() * f),
            // (int) (c.getBlue() * f));
            // previewLabel.setBackground(c);
            // }
            // });
            // this.add(darker);

            JButton ok = new JButton("OK");
            ok.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    closeDialog(colorChooser.getColor());
                }
            });
            this.add(Box.createHorizontalStrut(80));
            this.add(ok);

            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    closeDialog(null);
                }
            });
            this.add(cancel);

            Dimension d = new Dimension(100, 40);
            this.setMinimumSize(d);
            this.setPreferredSize(d);
        }

        @SuppressWarnings("synthetic-access")
        public void stateChanged(ChangeEvent e) {
            Color tmpColor = colorChooser.getColor();

            previewLabel.setBackground(tmpColor);

            int r = tmpColor.getRed();
            int g = tmpColor.getGreen();
            int b = tmpColor.getBlue();

            previewLabel.setText("  "
                    + (r < 10
                    ? "0"
                    : "") + Integer.toHexString(r)
                    + (g < 10
                    ? "0"
                    : "") + Integer.toHexString(g)
                    + (b < 10
                    ? "0"
                    : "") + Integer.toHexString(b)
                    + "  ");
        }
    }

    // Test code follows

    /**
     * Creates and show the GUI
     * <p>
     * For TESTING purposes
     */
    static void createAndShowGUI() {
        JFrame frame = new JFrame("ColorChooser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ColorChooser cc = new ColorChooser(frame);
        cc.showDialog(Color.WHITE, Color.BLACK);
        System.out.println(cc.getSelectedBackgroundColor().toString() + ' '
                + cc.getSelectedForegroundColor().toString());
        System.exit(0);
    }

    /**
     * Testing testing, sound check, 1 2 3
     * 
     * @param args not used
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                createAndShowGUI();
            }
        });
    }
}
