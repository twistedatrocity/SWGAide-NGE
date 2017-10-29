package swg.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/**
 * Extends the {@link JComboBox} with a wider popup list than the maximum
 * width allows for. Tips found at http://sdnshare.sun.com/view.jsp?id=2295.
 */
@SuppressWarnings("serial")
public class JComboWiderPopup extends JComboBox {

    /**
     * Creates a {@link JComboBox} with a wider popup list that usual
     */
    public JComboWiderPopup() {
        // Border not 100% good, inner border should only affect text area
        Border compound = BorderFactory.createCompoundBorder(
                new LineBorder(new Color(123, 138, 153)),//
                new LineBorder(new Color(186, 206, 230)));
        setBorder(compound);
        setUI(new ToolButtonComboBoxUI());
    }

    /**
     * Creates the object that provides the wider popup
     */
    class ToolButtonComboBoxUI extends BasicComboBoxUI {

        /**
         * Creates the object that provides the wider popup
         */
        public ToolButtonComboBoxUI() {
            super();
        }

        @Override
        protected JButton createArrowButton() {
            return new JComboButton();
        }

        @Override
        protected ComboPopup createPopup() {
            WiderPopup popup1 = new WiderPopup(comboBox);
            popup1.getAccessibleContext().setAccessibleParent(comboBox);
            return popup1;
        }
    }

    /**
     * The replacement popup that can be wider than default
     */
    class WiderPopup extends BasicComboPopup {

        /**
         * Creates a replacement popup that can be wider than default
         * 
         * @param comboBox
         *            the combo box for the popup
         */
        public WiderPopup(JComboBox comboBox) {
            super(comboBox);
            setLayout(new GridLayout(comboBox.getModel().getSize(), 1, 5, 5));
        }

        @Override
        public Dimension getPreferredSize() {
            return super.getPreferredSize();
            // use fix width bigger than the usual width
            // size.width += 200;
            // OEreturn size;
            // This class and method could be expanded to allow for more
            // dynamic adaption
        }
    }
}
