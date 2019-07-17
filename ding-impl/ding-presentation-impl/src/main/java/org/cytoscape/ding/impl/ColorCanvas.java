package org.cytoscape.ding.impl;

import java.awt.Color;

public class ColorCanvas extends DingCanvas {

	public static final Color DEFAULT_COLOR = Color.WHITE;
	
	private Color color;
	private boolean dirty = true;
	
	public ColorCanvas(Color color) {
		setColor(color);
	}
	
	public ColorCanvas() {
		this(DEFAULT_COLOR);
	}
	
	public void setColor(Color color) {
		this.color = color == null ? DEFAULT_COLOR : color;
		dirty = true;
	}

	@Override
	public void setViewport(int width, int height) {
		super.setViewport(width, height);
		dirty = true;
	}
	
	@Override
	public void paintImage() {
		if(dirty) {
			image.fill(color);
			dirty = false;
		}
	}
	
}
