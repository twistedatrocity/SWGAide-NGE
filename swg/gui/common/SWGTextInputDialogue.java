package swg.gui.common;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;

import swg.gui.SWGFrame;

/**
 * A custom, modal dialogue for user text input. The caller must implement the
 * interface {@link SWGTextInputDialogue} to use this class, which validates the
 * typed input.
 * 
 * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
 *         Europe-Chimaera.Zimoon
 */
@SuppressWarnings("serial")
public class SWGTextInputDialogue extends JDialog implements ActionListener,
    PropertyChangeListener {

    /**
     * Text of button 1, also used by the logic
     */
    private String btnString1 = "OK";

    /**
     * Text of button 2, also used by the logic
     */
    private String btnString2 = "Cancel";

    /**
     * The object that uses this class; must implement interface
     * SWGNameValidation.
     */
    private TextValidation caller;

    /**
     * An internal option pane.
     */
    JOptionPane optionPane;

    /**
     * The GUI component at which the user types.
     */
    JTextField textField;

    /**
     * The text the user typed.
     */
    private String typedText;

    /**
     * Creates a modal dialogue for user typed text. No more than 15 characters
     * of text can be entered.
     * 
     * @param frame the frame of the application
     * @param caller the client using this instance
     * @param location the wanted location on screen
     * @param title the title for the dialogue
     * @param msgOne the first message to show
     * @param msgTwo the second message to show
     */
    public SWGTextInputDialogue(SWGFrame frame, TextValidation caller,
            Point location, String title, String msgOne, String msgTwo) {

        super(frame, title, true);
        this.caller = caller;

        textField = new JTextField(10);
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(
                new SWGDocFilter() {
                    @Override
                    protected boolean isValid(String s) {
                        return s == null || s.length() <= 15;
                    }
                });
        String msg1 = msgOne;
        String msg2 = msgTwo;
        Object[] objs = { msg1, msg2, textField };
        Object[] options = { btnString1, btnString2 };
        optionPane = new JOptionPane(objs,
                JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
                null, options, options[0]);
        this.setContentPane(optionPane);

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
            }
        });
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent ce) {
                textField.requestFocusInWindow();
            }
        });
        textField.addActionListener(this);
        optionPane.addPropertyChangeListener(this);

        this.setLocation(location);
        this.pack();
    }

    /*
     * (non-Javadoc)
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        optionPane.setValue(btnString1);
    }

    /**
     * This method clears the dialog and hides it.
     */
    private void clearAndHide() {
        textField.setText(null);
        setVisible(false);
    }

    /**
     * Returns the validated text, or {@code null} if no valid text is entered
     * or if the dialog was dismissed.
     * 
     * @return some valid text, or {@code null}
     */
    public String getTypedText() {
        return typedText;
    }

    /*
     * (non-Javadoc)
     * @seejava.beans.PropertyChangeListener#propertyChange(java.beans.
     * PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (isVisible()
            && (e.getSource() == optionPane)
            && (JOptionPane.VALUE_PROPERTY.equals(prop) || JOptionPane.INPUT_VALUE_PROPERTY
                .equals(prop))) {

            Object value = optionPane.getValue();

            if (value == JOptionPane.UNINITIALIZED_VALUE)
                return; // no need for reset

            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

            if ("OK".equals(value)) {
                typedText = textField.getText().trim();
                if (!typedText.isEmpty() && caller.validateText(typedText))
                    clearAndHide();
                else {
                    textField.selectAll();
                    JOptionPane.showMessageDialog(SWGTextInputDialogue.this,
                        String.format("\"%s\" is invalid", typedText),
                        "Try again", JOptionPane.ERROR_MESSAGE);
                    typedText = null;
                    textField.requestFocusInWindow();
                }
            } else { // user closed dialog or clicked cancel
                typedText = null;
                clearAndHide();
            }
        }
    }

    /**
     * An interface for validation of user typed text. Validation is determined
     * by the implementation of {@link #validateText(String)}.
     * 
     * @author <a href="mailto:simongronlund@gmail.com">Simon Gronlund</a> aka
     *         Chimaera.Zimoon
     */
    public interface TextValidation {

        /**
         * Determines if the specified text is valid according to the particular
         * implementation.
         * 
         * @param text the text to validate
         * @return {@code true} if the specified text is valid
         */
        boolean validateText(String text);
    }
}
