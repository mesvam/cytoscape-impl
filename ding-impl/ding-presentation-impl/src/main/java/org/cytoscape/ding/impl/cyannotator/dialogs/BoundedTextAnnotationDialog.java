package org.cytoscape.ding.impl.cyannotator.dialogs;

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


import java.awt.Point;
import java.awt.Robot;
import java.awt.geom.Point2D;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.cytoscape.view.presentation.annotations.BoundedTextAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;

import org.cytoscape.ding.impl.DGraphView;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.ding.impl.cyannotator.annotations.BoundedTextAnnotationImpl;
import org.cytoscape.ding.impl.cyannotator.annotations.ShapeAnnotationImpl;
import org.cytoscape.ding.impl.cyannotator.annotations.TextAnnotationImpl;

public class BoundedTextAnnotationDialog extends JDialog {

	private javax.swing.JButton applyButton;
	private javax.swing.JButton cancelButton;

	private ShapeAnnotationPanel shapeAnnotation1;  
	private TextAnnotationPanel textAnnotation1;  

	private final CyAnnotator cyAnnotator;    
	private final DGraphView view;    
	private final Point2D startingLocation;
	private final BoundedTextAnnotationImpl mAnnotation;
	private BoundedTextAnnotationImpl preview;
	private final boolean create;
		
	public BoundedTextAnnotationDialog(DGraphView view, Point2D start) {
		this.view = view;
		this.cyAnnotator = view.getCyAnnotator();
		this.startingLocation = start;
		this.mAnnotation = new BoundedTextAnnotationImpl(cyAnnotator, view);
		this.create = true;

		initComponents();		        
	}

	public BoundedTextAnnotationDialog(BoundedTextAnnotationImpl mAnnotation) {
		this.mAnnotation=mAnnotation;
		this.cyAnnotator = mAnnotation.getCyAnnotator();
		this.view = cyAnnotator.getView();
		this.create = false;
		this.startingLocation = null;

		initComponents();	
	}
    
	private void initComponents() {
		int SHAPE_HEIGHT = 220;
		int SHAPE_WIDTH = 500;
		int TEXT_HEIGHT = 220;
		int TEXT_WIDTH = 500;
		int PREVIEW_WIDTH = 500;
		int PREVIEW_HEIGHT = 220;

		// Create the preview panel
		preview = new BoundedTextAnnotationImpl(cyAnnotator, view);
		preview.setUsedForPreviews(true);
		preview.setText(mAnnotation.getText());
		preview.setFont(mAnnotation.getFont());
		preview.fitShapeToText();
		
		PreviewPanel previewPanel = new PreviewPanel(preview, PREVIEW_WIDTH, PREVIEW_HEIGHT);

		shapeAnnotation1 = new ShapeAnnotationPanel((ShapeAnnotation)mAnnotation, previewPanel, SHAPE_WIDTH, SHAPE_HEIGHT);
		textAnnotation1 = new TextAnnotationPanel((TextAnnotation)mAnnotation, previewPanel, TEXT_WIDTH, TEXT_HEIGHT);

		applyButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();

		if (create)
			setTitle("Create Bounded Text Annotation");
		else
			setTitle("Modify Bounded Text Annotation");

		setResizable(false);
		getContentPane().setLayout(null);

		getContentPane().add(shapeAnnotation1);
		shapeAnnotation1.setBounds(5, 0, shapeAnnotation1.getWidth(), shapeAnnotation1.getHeight());
		getContentPane().add(textAnnotation1);
		textAnnotation1.setBounds(5, SHAPE_HEIGHT, textAnnotation1.getWidth(), textAnnotation1.getHeight());

		getContentPane().add(previewPanel);
		previewPanel.setBounds(5, SHAPE_HEIGHT+TEXT_HEIGHT+5, PREVIEW_WIDTH, PREVIEW_HEIGHT);

		int y = PREVIEW_HEIGHT+SHAPE_HEIGHT+TEXT_HEIGHT+10;

		applyButton.setText("OK");
		applyButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				applyButtonActionPerformed(evt);
			}
		});
		getContentPane().add(applyButton);
		applyButton.setBounds(350, y+20, applyButton.getPreferredSize().width, 23);

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		getContentPane().add(cancelButton);
		cancelButton.setBounds(430, y+20, cancelButton.getPreferredSize().width, 23);

		pack();
		setSize(520, y+80 );
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setModalityType(DEFAULT_MODALITY_TYPE);
	}

	private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {
		dispose();           
		//Apply
		mAnnotation.setFont(textAnnotation1.getNewFont());
		mAnnotation.setTextColor(textAnnotation1.getTextColor());
		mAnnotation.setText(textAnnotation1.getText());
		mAnnotation.setShapeType(preview.getShapeType());
		mAnnotation.setFillColor(preview.getFillColor());
		mAnnotation.setFillOpacity(preview.getFillOpacity());
		mAnnotation.setBorderColor(preview.getBorderColor());
		mAnnotation.setBorderOpacity(preview.getBorderOpacity());
		mAnnotation.setBorderWidth((int)preview.getBorderWidth());

		if (!create) {
			mAnnotation.update(); 
			return;
		}

		mAnnotation.getComponent().setLocation((int)startingLocation.getX(), (int)startingLocation.getY());
		mAnnotation.addComponent(null);
		mAnnotation.update();

		// Update the canvas
		view.getCanvas(DGraphView.Canvas.FOREGROUND_CANVAS).repaint();

		// Set this shape to be resized
		cyAnnotator.resizeShape(mAnnotation);

		try {
			// Warp the mouse to the starting location (if supported)
			Point start = mAnnotation.getComponent().getLocationOnScreen();
			Robot robot = new Robot();
			robot.mouseMove((int)start.getX()+100, (int)start.getY()+100);
		} catch (Exception e) {}
	}

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
		//Cancel
		dispose();
	}
}

