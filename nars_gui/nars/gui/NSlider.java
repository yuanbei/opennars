/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.NumberFormat;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicBorders;
import org.python.google.common.util.concurrent.AtomicDouble;

/**
 *
 * @author me
 */
public class NSlider extends JLabel implements MouseListener, MouseMotionListener {
    final AtomicDouble value;
    private final double min;
    private final double max;
    private Color barColor = null;
    private boolean dragging;
    NumberFormat nf = NumberFormat.getInstance();
     
    public NSlider(double initialValue, double min, double max) {
        this(new AtomicDouble(initialValue), min, max);
    }
    
    public NSlider(AtomicDouble value, double min, double max) {
        super();
        
        nf.setMaximumFractionDigits(3);
        
        this.value = value;        
        this.min = min;
        this.max = max;
        setBorder(BasicBorders.getButtonBorder());
        
        addMouseListener(this);
        addMouseMotionListener(this);
        
    }

    public double value() { return value.doubleValue(); }
        
    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.clearRect(0, 0, w, h);

        double p = (value.doubleValue() - min) / (max-min);
        if (barColor == null) {
            g.setColor(Color.getHSBColor( (1f - (float)p) / 3.0f , 0.5f, 0.9f));
        }
        else {
            g.setColor(barColor);
        }
        
        int wp = (int)(((double)w) * p );
        g.setColor(barColor);
        g.fillRect(0, 0, wp, h);
        super.paint(g);
    }
    
    public void onValueUpdated() {
        setText(getText());
    }

    @Override
    public String getText() {
        if (value!=null)
            return nf.format(value.doubleValue());
        return "";
    }
    

    
    
 /*
    public static void main(String[] args) {
        NSlider n = new NSlider(new AtomicDouble(5), 1, 10);
        
        //test
        JFrame jf = new JFrame();
        jf.setVisible(true);
        jf.setSize(200, 100);
        jf.getContentPane().add(n);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
    }
    */

    protected void updatePosition(int x) {
        double p = ((double)x) / ((double)getWidth());
        double v = p * (max-min) + min;
        v = Math.max(v, min);
        v = Math.min(v, max);        
        setValue(v);
        repaint();
    }
    
    public void setValue(double v) {
        if (v != value.doubleValue()) {
            value.set( v );     
            onChange(v);
        }
    }
    
    public void onChange(double v) {
        
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragging = true;

        updatePosition(e.getX());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        updatePosition(e.getX());
        dragging = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updatePosition(e.getX());        
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (dragging) {
            updatePosition(e.getX());
        }
    }


}
