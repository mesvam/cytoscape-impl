package org.cytoscape.ding.impl.cyannotator.utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.UIManager;

import org.cytoscape.ding.internal.util.MathUtil;
import org.cytoscape.util.swing.LookAndFeelUtil;

@SuppressWarnings("serial")
public class AnglePicker extends JComponent {
  
	private static final int POINT_RADIUS = 4;
	private static final int[] MAIN_ANGLES = { 0, 45, 90, 135, 180, 225, 270, 315 };
	
	private int angle = -1;
	
	private float[] fractions = { 0.0f, 0.5f, 1.0f };
	private Color[] colors = { Color.BLACK, Color.BLUE, Color.WHITE };
	
	public AnglePicker() {
		setOpaque(!LookAndFeelUtil.isAquaLAF());
		
		// We need the 360 value for the findClosestNumber function, or it will be hard to snap to 0 degrees
		final int[] allAngles = { 0, 45, 90, 135, 180, 225, 270, 315, 360 };
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				double oldValue = angle;
				
				int x = evt.getX() - getWidth() / 2;
				int y = evt.getY() - getHeight() / 2;
				angle = (int) Math.round(Math.toDegrees(Math.atan2(y, x)));
				angle = (int) Math.round(MathUtil.normalizeAngle(angle));
				
				if (evt.isShiftDown()) // Snap to closer main angle
					angle = MathUtil.findNearestNumber(allAngles, Math.round(angle));
				
				repaint();
				firePropertyChange("value", oldValue, angle);
			}
		});
	}
	
	/**
     * Optional, in case you want the canvas background to show the linear gradient it's modifying.
     */
    public void update(float[] fractions, Color[] colors, int angle) {
    	this.fractions = fractions;
    	this.colors = colors;
    	this.angle = (int) Math.round(MathUtil.normalizeAngle(angle));
    	repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		var g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		var insets = getInsets();
		
		int w = getWidth();
		int h = getHeight();
		int pr = POINT_RADIUS;
		int pad = Math.max(insets.top + insets.bottom, insets.left + insets.right) / 2;
		int r = Math.min(w, h) / 2 - pr - pad;
		
		var color1 = UIManager.getColor("Separator.foreground");
		var color2 = UIManager.getColor("Table.background");
		
		if (fractions != null && fractions.length > 0 && colors != null && colors.length > 0) {
			// Use the passed colors and fractions to paint our background with a linear gradient,
			// but with an updated center point, of course
			var bounds = new Rectangle(pad + pr, pad + pr, 2 * r - 4 + pr, 2 * r - 4 + pr);
			var line = MathUtil.getGradientAxis(bounds, angle);
			var paint = new LinearGradientPaint(line.getP1(), line.getP2(), fractions, colors);
			g2.setPaint(paint);
			g2.fill(bounds);
		}
		
		g2.translate(w / 2, h / 2);
		
		g2.setColor(color1);
		g2.drawOval(-r, -r, r * 2, r * 2); // external line
		g2.setColor(color2);
		g2.drawOval(-r + 1, -r + 1, (r - 1) * 2, (r - 1) * 2); // internal line
		
		g2.drawLine(-r, 0, r, 0);
		g2.drawLine(0, -r, 0, r);

		for (int angle : MAIN_ANGLES) {
			int x = (int) (r * Math.cos(Math.toRadians(angle)));
			int y = (int) (r * Math.sin(Math.toRadians(angle)));
			
			g2.setColor(color1);
			g2.drawOval(x - pr, y - pr, 2 * pr, 2 * pr);
			
			g2.setColor(color2);
			g2.fillOval(x - pr, y - pr, 2 * pr, 2 * pr);
		}

		if (angle >= 0) {
			int x = (int) (r * Math.cos(Math.toRadians(angle)));
			int y = (int) (r * Math.sin(Math.toRadians(angle)));
			
			g2.setColor(color2);
			g2.drawOval(x - pr, y - pr, 2 * pr, 2 * pr);
			
			g2.setColor(UIManager.getColor("Focus.color"));
			g2.fillOval(x - pr, y - pr, 2 * pr, 2 * pr);
		}
	}
}
