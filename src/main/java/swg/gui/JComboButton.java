package swg.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;

import javax.swing.JButton;

/**
 * The button for a custom JComboBox
 * <p>
 * http://sdnshare.sun.com/view.jsp?id=2295
 * 
 * @author david_david
 */
@SuppressWarnings("serial")
public class JComboButton extends JButton {

    /**
     * Creates a button for the a custom combo box
     */
    public JComboButton() {
        setBorderPainted(false);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(16, 0x7fffffff);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(5, 5);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(16, 16);
    }

    @Deprecated
    @Override
    public boolean isFocusTraversable() {
        return false;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        paintTriangle(g);
    }

    /**
     * Helper method for this{@link #paint(Graphics)}
     * 
     * @param g
     *            the <code>Graphics</code> context in which to paint
     */
    private final void paintTriangle(Graphics g) {
        int w = getWidth() / 2;
        int h = getHeight();
        g.setColor(Color.gray); // new Color(127, 159, 191)
        g.drawLine(0, 0, 0, h);
        if (isEnabled()) g.setColor(Color.BLACK);
        Polygon po = new Polygon();
        po.addPoint(w - 4, h / 2 - 2);
        po.addPoint(w + 4, h / 2 - 2);
        po.addPoint(w, 2 + h / 2);
        g.drawPolygon(po);
        g.fillPolygon(po);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds((x + width) - 18, y, 18, height);
    }
}
