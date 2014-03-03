package org.cytoscape.ding.impl.cyannotator.annotations;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.image.VolatileImage;
import java.net.URL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;

import org.cytoscape.view.presentation.annotations.ImageAnnotation;

import org.cytoscape.ding.customgraphics.CustomGraphicsManager;
import org.cytoscape.ding.customgraphics.ImageUtil;
import org.cytoscape.ding.customgraphics.bitmap.URLImageCustomGraphics;
import org.cytoscape.ding.impl.DGraphView;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
// import org.cytoscape.ding.impl.cyannotator.api.ImageAnnotation;
import org.cytoscape.ding.impl.cyannotator.dialogs.ImageAnnotationDialog;
import org.cytoscape.ding.impl.cyannotator.annotations.ShapeAnnotationImpl.ShapeType;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageAnnotationImpl extends ShapeAnnotationImpl implements ImageAnnotation {
	private BufferedImage image;
	private	URL url = null;

	private static final float MAX_CONTRAST=4.0f;
	
	protected double imageWidth=0, imageHeight=0;
	private BufferedImage resizedImage;
	private float opacity = 1.0f;
	private int brightness = 0;
	private int contrast = 0;
	private CyCustomGraphics cg = null;
	protected CustomGraphicsManager customGraphicsManager;

	private double borderWidth = 0.0;
	private Paint borderColor = null;

	private static final Logger logger = LoggerFactory.getLogger(ImageAnnotationImpl.class);


	// XXX HACK to force the custom graphics manager to respect these graphics
	public void preserveCustomGraphics() {
		for (CyCustomGraphics cg: customGraphicsManager.getAllCustomGraphics()) {
			customGraphicsManager.setUsedInCurrentSession(cg, true);
		}
	}

	public ImageAnnotationImpl(CyAnnotator cyAnnotator, DGraphView view) { super(cyAnnotator, view, 0, 0); }

	public ImageAnnotationImpl(ImageAnnotationImpl c) { 
		super((ShapeAnnotationImpl)c, 0, 0);
		this.image = c.image;
		this.customGraphicsManager = c.customGraphicsManager;
		imageWidth=image.getWidth();
		imageHeight=image.getHeight();
		this.url = c.url;
	}

	public ImageAnnotationImpl(CyAnnotator cyAnnotator, DGraphView view, double x, double y, 
	                           URL url, BufferedImage image, double zoom, 
	                           CustomGraphicsManager customGraphicsManager) {
		super(cyAnnotator, view, x, y, ShapeType.RECTANGLE, 0, 0, null, null, 0.0f);
		this.image=image;
		this.customGraphicsManager = customGraphicsManager;
		imageWidth=image.getWidth();
		imageHeight=image.getHeight();
		this.url = url;
		resizedImage=resizeImage((int)imageWidth, (int)imageHeight);
		final Long id = customGraphicsManager.getNextAvailableID();
		this.cg = new URLImageCustomGraphics(id, url.toString(), image);
		customGraphicsManager.addCustomGraphics(cg, url);
		customGraphicsManager.setUsedInCurrentSession(cg, true);
	}

	public ImageAnnotationImpl(CyAnnotator cyAnnotator, DGraphView view, 
	                           Map<String, String> argMap, CustomGraphicsManager customGraphicsManager) {
		super(cyAnnotator, view, argMap);
		this.customGraphicsManager = customGraphicsManager;

		imageWidth = getDouble(argMap, ImageAnnotation.WIDTH, 100.0);
		imageHeight = getDouble(argMap, ImageAnnotation.HEIGHT, 100.0);

		opacity = getFloat(argMap, OPACITY, 1.0f);
		brightness = getInteger(argMap, LIGHTNESS, 0);
		contrast = getInteger(argMap, CONTRAST, 0);

		this.image = null;
		this.resizedImage = null;

		if (!argMap.containsKey(URL))
			return;

		// Get the image from the image pool
		try {
			this.url = new URL(argMap.get(URL));
			this.cg = customGraphicsManager.getCustomGraphicsBySourceURL(this.url);
			if (cg != null) {
				this.image = ImageUtil.toBufferedImage(cg.getRenderedImage());
				customGraphicsManager.addCustomGraphics(cg, this.url);
				customGraphicsManager.setUsedInCurrentSession(cg, true);
				resizedImage=resizeImage((int)image.getWidth(), (int)image.getHeight());
			}
		} catch (Exception e) {
			logger.warn("Unable to restore image '"+argMap.get(URL)+"'",e);
			return;
		}
	}

	public Map<String,String> getArgMap() {
		Map<String, String> argMap = super.getArgMap();
		argMap.put(TYPE,ImageAnnotation.class.getName());
		argMap.put(URL, url.toString());
		argMap.put(ImageAnnotation.WIDTH, Double.toString(imageWidth));
		argMap.put(ImageAnnotation.HEIGHT, Double.toString(imageHeight));
		argMap.put(OPACITY, Float.toString(opacity));
		argMap.put(LIGHTNESS, Integer.toString(brightness));
		argMap.put(CONTRAST, Integer.toString(contrast));
		customGraphicsManager.setUsedInCurrentSession(cg, true);

		return argMap;
	}

	public void reloadImage() {
		// Get the image from the image pool again
		try {
			this.cg = customGraphicsManager.getCustomGraphicsBySourceURL(this.url);
			if (cg != null) {
				this.image = ImageUtil.toBufferedImage(cg.getRenderedImage());
				customGraphicsManager.addCustomGraphics(cg, this.url);
				customGraphicsManager.setUsedInCurrentSession(cg, true);
			} else {
				return;
			}
		} catch (Exception e) {
			logger.warn("Unable to restore image '"+this.url+"'",e);
			return;
		}
		resizedImage=resizeImage((int)imageWidth, (int)imageHeight);
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		if (image instanceof BufferedImage)
			this.image = (BufferedImage)image;
		else if (image instanceof VolatileImage)
			this.image = ((VolatileImage)image).getSnapshot();
		else
			return;

		this.imageWidth=this.image.getWidth();
		this.imageHeight=this.image.getHeight();
		
		int width = (int)this.image.getWidth();
		int height = (int)this.image.getHeight();
		if (resizedImage != null) {
			width = (int)resizedImage.getWidth();
			height = (int)resizedImage.getHeight();
		}
		resizedImage=resizeImage((int)width, (int)height);
		if (!usedForPreviews())
			getCanvas().repaint();
	}

	public void setImage(URL url) {
		this.url = url;
		reloadImage();
	}

	public URL getImageURL() {
		return url;
	}

	public void setImageOpacity(float opacity) {
		this.opacity = opacity;
		resizedImage=null;
	}
	public float getImageOpacity() { return this.opacity; }

	public void setImageBrightness(int brightness) {
		this.brightness = brightness;
		resizedImage=null;
	}
	public int getImageBrightness() { return this.brightness; }

	public void setImageContrast(int contrast) {
		this.contrast = contrast;
		resizedImage=null;
	}
	public int getImageContrast() { return this.contrast; }

	// Shape annotation methods.  We add these so we can get resizeImage functionality

	// At this point, we only support RECTANGLE.  At some point, it would be really
	// useful to clip the image to the shape
	public List<String> getSupportedShapes() {
		return Collections.singletonList(ShapeType.RECTANGLE.shapeName());
	}

	public void setSize(double width, double height) {
		this.imageWidth = width;
		this.imageHeight = height;

		// Resize the image
		resizedImage=resizeImage((int)imageWidth, (int)imageHeight);
		if (!usedForPreviews())
			getCanvas().repaint();

		setSize((int)width, (int)height);
	}

	public String getShapeType() {
		return ShapeType.RECTANGLE.shapeName();
	}

	public void setCustomShape(Shape shape) {
	}

	public void setShapeType(String type) {}

	public double getBorderWidth() {
		return borderWidth;
	}

	public void setBorderWidth(double width) {
		borderWidth = width*getZoom();
	}

	public Paint getBorderColor() {return borderColor;}
	public Paint getFillColor() {return null;}
	public double getFillOpacity() {return 100.0;}
	public double getBorderOpacity() {return 100.0;}

	public void setBorderColor(Paint border) {borderColor = border;}
	public void setFillColor(Paint fill) {}
	public void setFillOpacity(double opacity) {}
	public void setBorderOpacity(double opacity) {}

	public Shape getShape() {
		return new Rectangle2D.Double((double)getX(), (double)getY(), imageWidth, imageHeight);
	}
	

	//Returns a resizeImaged high quality BufferedImage
	private BufferedImage resizeImage(int width, int height)
	{
		if (image == null) {
			if (width == 0) width = 1;
			if (height == 0) height = 1;
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}

		int type = image.getType() == 0? BufferedImage.TYPE_INT_RGB : image.getType();
		if(height==0)
			height++;
		if(width==0)
			width++;

		BufferedImage adjustedImage = image;

		// Handle image adjustments
		if (contrast != 0 || brightness != 0) {
			BufferedImage source = image;
			// This only works for RGB
			if (type != BufferedImage.TYPE_INT_RGB) {
				BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), 
				                                           BufferedImage.TYPE_INT_RGB);
				Graphics2D g = rgbImage.createGraphics();
				g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), this);
				source = rgbImage;
			}
			adjustedImage = new BufferedImage(image.getWidth(), image.getHeight(), 
				                                BufferedImage.TYPE_INT_RGB);

			// Do Brightness first...
			// offset goes from -255 - 255 for RGB
			float offset = (float)brightness*255.0f/100.0f;
			RescaleOp op = new RescaleOp(1.0f, offset, null);
			op.filter(source, adjustedImage);

			float scaleFactor = 1.0f;
			// scaleFactor goes from 0-4.0 with a 
			if (contrast <= 0) {
				scaleFactor = 1.0f + ((float)contrast)/100.0f;
			} else
				scaleFactor = 1.0f + ((float)contrast)*3.0f/100.0f;
		
			op = new RescaleOp(scaleFactor, 0.0f, null);
			op.filter(adjustedImage, adjustedImage);
		}

		BufferedImage newImage = new BufferedImage(width, height, type);
		Graphics2D g = newImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
		g.setComposite(ac);
		g.drawImage(adjustedImage, 0, 0, width, height, this);
		g.dispose();
		return newImage;
	}

	public void dropImage() {
		customGraphicsManager.setUsedInCurrentSession(cg, false);
	}

	public JDialog getModifyDialog() {
			return new ImageAnnotationDialog(this);
	}

	@Override
	public void drawAnnotation(Graphics g, double x, double y, double scaleFactor) {
		super.drawAnnotation(g, x, y, scaleFactor);
		
		Graphics2D g2=(Graphics2D)g;

		int width = (int)Math.round(imageWidth*scaleFactor/getZoom());
		int height = (int)Math.round(imageHeight*scaleFactor/getZoom());
		BufferedImage newImage =resizeImage(width, height);
		if (newImage == null) return;

		boolean selected = isSelected();
		setSelected(false);
		g2.drawImage(newImage, (int)(x*scaleFactor), (int)(y*scaleFactor), null);
		setSelected(selected);
	}

	@Override
	public void paint(Graphics g) {				
		super.paint(g);

		Graphics2D g2=(Graphics2D)g;

		if (image == null)
			return;

		if (resizedImage == null)
			resizedImage = resizeImage((int)imageWidth, (int)imageHeight);

		// int x = getX();
		// int y = getY();
		int x = 0;
		int y = 0;

		if (usedForPreviews()) {
			x = 0; y = 0;
		}

		g2.drawImage(resizedImage, x, y, this);

		if (borderColor != null && borderWidth > 0.0) {
			g2.setPaint(borderColor);
			g2.setStroke(new BasicStroke((float)borderWidth));
			g2.drawRect(x, y, getAnnotationWidth(), getAnnotationHeight());
		}
		
		if(isSelected()) {
			g2.setColor(Color.YELLOW);
			g2.setStroke(new BasicStroke(2.0f));
			g2.drawRect(x, y, getAnnotationWidth(), getAnnotationHeight());
		}
	}

	@Override
	public void setSpecificZoom(double newZoom) {

		double factor=newZoom/getSpecificZoom();
		
		imageWidth=imageWidth*factor;
		imageHeight=imageHeight*factor;

		resizedImage=resizeImage((int)Math.round(imageWidth), (int)Math.round(imageHeight));

		setBounds(getX(), getY(), getAnnotationWidth(), getAnnotationHeight());
	   
		super.setSpecificZoom(newZoom);		
	}

	@Override
	public void setZoom(double newZoom) {

		double factor=newZoom/getZoom();
		
		imageWidth=imageWidth*factor;
		imageHeight=imageHeight*factor;

		borderWidth*=factor;

		resizedImage=resizeImage((int)Math.round(imageWidth), (int)Math.round(imageHeight));

		setBounds(getX(), getY(), getAnnotationWidth(), getAnnotationHeight());
				
		super.setZoom(newZoom);		
	}


	public int getAnnotationWidth() {
		return (int)Math.round(imageWidth);
	}

	public int getAnnotationHeight() {
		return (int)Math.round(imageHeight);
	}
}
