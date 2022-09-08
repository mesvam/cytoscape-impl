package org.cytoscape.ding.impl.cyannotator.dialogs;

import static org.cytoscape.util.swing.LookAndFeelUtil.isAquaLAF;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.cytoscape.cg.model.BitmapCustomGraphics;
import org.cytoscape.cg.model.CustomGraphicsManager;
import org.cytoscape.cg.model.SVGCustomGraphics;
import org.cytoscape.cg.util.ImageCustomGraphicsSelector;
import org.cytoscape.ding.impl.DRenderingEngine;
import org.cytoscape.ding.impl.cyannotator.annotations.ImageAnnotationImpl;
import org.cytoscape.ding.impl.cyannotator.utils.ViewUtils;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2021 The Cytoscape Consortium
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

/**
 * Provides a way to create ImageAnnotations
 */
@SuppressWarnings("serial")
public class LoadImageDialog extends AbstractAnnotationDialog<ImageAnnotationImpl> {

	private static final String NAME = "Image";
	private static final String UNDO_LABEL = "Create Image Annotation";
	
	private JTabbedPane tabbedPane;
	private JFileChooser fileChooser;
	private ImageCustomGraphicsSelector imageSelector;
	
	private final CyServiceRegistrar serviceRegistrar;
	
	private static File lastDirectory;

	private static final Logger logger = LoggerFactory.getLogger(LoadImageDialog.class);

	public LoadImageDialog(
			DRenderingEngine re,
			Point2D start,
			Window owner,
			CyServiceRegistrar serviceRegistrar
	) {
		super(NAME, re, start, owner);
		
		this.serviceRegistrar = serviceRegistrar;
		
		setTitle("Select an Image");
		setResizable(true);
		
		getTabbedPane().addTab("From File", getFileChooser());
		getTabbedPane().addTab("From Image Browser", getImageSelector());
		
		pack();
	}
	
	@Override
	protected JTabbedPane createControlPanel() {
		return getTabbedPane();
	}

	@Override
	protected ImageAnnotationImpl getPreviewAnnotation() {
		return null;
	}
	
	@Override
	protected int getPreviewWidth() {
		return 0;
	}

	@Override
	protected int getPreviewHeight() {
		return 0;
	}
	
	@Override
	protected void apply() {
		var selectedComp = getTabbedPane().getSelectedComponent();
		
		try {
			if (selectedComp == getFileChooser()) {
				var file = fileChooser.getSelectedFile();
				annotation = createAnnotation(file);
				
				// Save current directory
				if (file.getParentFile().isDirectory())
					lastDirectory = file.getParentFile();
			} else {
				var cg = getImageSelector().getSelectedImage();
				annotation = createAnnotation(cg);
			}
			
			if (annotation != null) {
				var nodePoint = re.getTransform().getNodeCoordinates(startingLocation);
				var w = annotation.getWidth();
				var h = annotation.getHeight();
				
				annotation.setLocation(nodePoint.getX() - w / 2.0, nodePoint.getY() - h / 2.0);
				annotation.update();
				
				cyAnnotator.clearSelectedAnnotations();
				ViewUtils.selectAnnotation(re, annotation);
			}
		} catch (Exception ex) {
			logger.warn("Unable to load the selected image", ex);
		}
	}

	@Override
	protected JButton getApplyButton() {
		if (applyButton == null) {
			applyButton = new JButton(new AbstractAction("Insert") {
				@Override
				public void actionPerformed(ActionEvent evt) {
					apply();
					dispose();
				}
			});
		}
		
		return applyButton;
	}
	
	private JTabbedPane getTabbedPane() {
		if (tabbedPane == null) {
			tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		}
		
		return tabbedPane;
	}
	
	private JFileChooser getFileChooser() {
		if (fileChooser == null) {
			fileChooser = new JFileChooser(lastDirectory);
			fileChooser.setControlButtonsAreShown(false);
			fileChooser.setCurrentDirectory(null);
			fileChooser.setDialogTitle("");
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.addChoosableFileFilter(new ImageFilter());
		}

		return fileChooser;
	}
	
	private ImageCustomGraphicsSelector getImageSelector() {
		if (imageSelector == null) {
			imageSelector = new ImageCustomGraphicsSelector(serviceRegistrar);
			imageSelector.addActionListener(evt -> getApplyButton().doClick());	
			
			if (isAquaLAF())
				imageSelector.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")));
		}
		
		return imageSelector;
	}
	
	private ImageAnnotationImpl createAnnotation(File file) throws IOException {
		final ImageAnnotationImpl annotation;
		
		var cgManager = serviceRegistrar.getService(CustomGraphicsManager.class);
		
		// Read the selected Image, create an Image Annotation, repaint the
		// whole network and then dispose off this Frame
		var ext = FilenameUtils.getExtension(file.getName());
		var url = file.toURI().toURL();
		
		if (ext.equalsIgnoreCase("svg")) {
			// SVG...
			var sb = new StringBuilder();
			
			try (var in = new BufferedReader(new InputStreamReader(url.openStream()))) {
				String line = null;
				
				while ((line = in.readLine()) != null)
		            sb.append(line + "\n");
			}
			
			var svg = sb.toString();
			
			if (svg.isBlank())
				return null;
			
			cyAnnotator.markUndoEdit(UNDO_LABEL);
			
			annotation = new ImageAnnotationImpl(
					re,
					(int) startingLocation.getX(),
					(int) startingLocation.getY(),
          0d, // rotation
					url,
					svg,
					re.getZoom(),
					cgManager
			);
		} else {
			// Bitmap (PNG, JPG, etc.)...
			var image = ImageIO.read(file);
			
			cyAnnotator.markUndoEdit(UNDO_LABEL);
			
			annotation = new ImageAnnotationImpl(
					re,
					(int) startingLocation.getX(),
					(int) startingLocation.getY(),
          0d, // rotation
					url,
					image,
					re.getZoom(),
					cgManager
			);
		}
		
		return annotation;
	}
	
	private ImageAnnotationImpl createAnnotation(CyCustomGraphics<?> cg) {
		ImageAnnotationImpl annotation = null;
		
		var cgManager = serviceRegistrar.getService(CustomGraphicsManager.class);
		
		if (cg instanceof SVGCustomGraphics) {
			cyAnnotator.markUndoEdit(UNDO_LABEL);
			
			annotation = new ImageAnnotationImpl(
					re,
					(SVGCustomGraphics) cg,
					(int) startingLocation.getX(),
					(int) startingLocation.getY(),
          0d, // rotation
					re.getZoom(),
					cgManager
			);
		} else if (cg instanceof BitmapCustomGraphics) {
			cyAnnotator.markUndoEdit(UNDO_LABEL);
			
			annotation = new ImageAnnotationImpl(
					re,
					(BitmapCustomGraphics) cg,
					(int) startingLocation.getX(),
					(int) startingLocation.getY(),
          0d, // rotation
					re.getZoom(),
					cgManager
			);
		}
		
		return annotation;
	}

	/**
	 * This class provides a FileFilter for the JFileChooser.
	 */
	private class ImageFilter extends FileFilter {

		/**
		 * Accept all directories and all gif, jpg, tiff, png and svg files.
		 */
		@Override
		public boolean accept(File f) {
			if (f.isDirectory())
				return true;

			var ext = FilenameUtils.getExtension(f.getName()).toLowerCase();
			
			if (!ext.isEmpty())
				return ext.equals("tiff") || ext.equals("tif") || ext.equals("jpeg") || ext.equals("jpg")
						|| ext.equals("png") || ext.equals("gif") || ext.equals("svg");

			return false;
		}

		@Override
		public String getDescription() {
			return "Just Images";
		}
	}
}
