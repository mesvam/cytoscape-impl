/*
 Copyright (c) 2006, 2007, 2010, The Cytoscape Consortium (www.cytoscape.org)

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/
package org.cytoscape.ding.impl;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Point2D;

import java.util.List;

import org.cytoscape.ding.DArrowShape;
import org.cytoscape.ding.DVisualLexicon;
import org.cytoscape.ding.EdgeView;
import org.cytoscape.ding.GraphView;
import org.cytoscape.ding.Label;
import org.cytoscape.graph.render.immed.EdgeAnchors;
import org.cytoscape.graph.render.immed.GraphGraphics;
import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.ArrowShape;
import org.cytoscape.view.presentation.property.values.Bend;
import org.cytoscape.view.presentation.property.values.Handle;
import org.cytoscape.view.presentation.property.values.LineType;


/**
 * Ding implementation of Edge View.
 */
public class DEdgeView extends AbstractDViewModel<CyEdge> implements EdgeView, Label, EdgeAnchors {
	
	private static final int DEFAULT_TRANSPARENCY = 255;
	
	static final float DEFAULT_ARROW_SIZE = 8.0f;
	static final Paint DEFAULT_ARROW_PAINT = Color.BLACK;
	static final float DEFAULT_EDGE_THICKNESS = 1.0f;
	static final Stroke DEFAULT_EDGE_STROKE = new BasicStroke(); 
	static final Color DEFAULT_EDGE_PAINT = Color.black;
	static final String DEFAULT_LABEL_TEXT = "";
	static final Font DEFAULT_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
	static final Paint DEFAULT_LABEL_PAINT = Color.black;
	
	// Parent network view.  This view exists only in this network view.
	final DGraphView m_view;
	
	final long m_inx; // Positive index of this edge view.
	boolean m_selected;
	
	private Integer transparency;

	Paint m_sourceUnselectedPaint;
	Paint m_sourceSelectedPaint;
	Paint m_targetUnselectedPaint;
	Paint m_targetSelectedPaint;
	int m_sourceEdgeEnd; // One of the EdgeView edge end constants.
	int m_targetEdgeEnd; // Ditto.
		
	private String m_toolTipText;
	
	private LineType lineType;
	private Float fontSize = DVisualLexicon.EDGE_LABEL_FONT_SIZE.getDefault().floatValue();

	/**
	 * 
	 * @param view
	 * @param inx
	 * @param model
	 */
	DEdgeView(final DGraphView view, final long inx, final CyEdge model) {
		super(model);
		if (view == null )
			throw new IllegalArgumentException("Constructor needs its parent DGraphView.");
		
		m_view = view;
		m_inx = inx;
		m_selected = false;
		transparency = DEFAULT_TRANSPARENCY;
		m_sourceUnselectedPaint = DEFAULT_ARROW_PAINT;
		m_sourceSelectedPaint = Color.red;
		m_targetUnselectedPaint = DEFAULT_ARROW_PAINT;
		m_targetSelectedPaint = Color.red;
		m_sourceEdgeEnd = GraphGraphics.ARROW_NONE;
		m_targetEdgeEnd = GraphGraphics.ARROW_NONE;
	}

	@Override
	public long getGraphPerspectiveIndex() {
		return m_inx;
	}


	@Override
	public long getRootGraphIndex() {
		return m_inx;
	}

	
	@Override
	public CyEdge getEdge() {
		return m_view.getNetwork().getEdge(m_inx);
	}

	public View<CyEdge> getEdgeView() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GraphView getGraphView() {
		return m_view;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStrokeWidth(final float width) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.overrideSegmentThickness(model, width);
			m_view.m_contentChanged = true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getStrokeWidth() {
		synchronized (m_view.m_lock) {
			return m_view.m_edgeDetails.segmentThickness(model);
		}
	}

	@Override
	public void setStroke(Stroke stroke) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.overrideSegmentStroke(model, stroke);
			m_view.m_contentChanged = true;
		}
	}

	@Override
	public Stroke getStroke() {
		synchronized (m_view.m_lock) {
			return m_view.m_edgeDetails.segmentStroke(model);
		}
	}

	@Override
	public void setLineType(int lineType) {
		if ((lineType == EdgeView.CURVED_LINES)
				|| (lineType == EdgeView.STRAIGHT_LINES)) {
			synchronized (m_view.m_lock) {
				m_view.m_edgeDetails.m_lineType.put(model, lineType);
				m_view.m_contentChanged = true;
			}
		} else
			throw new IllegalArgumentException("unrecognized line type");
	}

	@Override
	public int getLineType() {
		return m_view.m_edgeDetails.lineType(model);
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setUnselectedPaint(final Paint paint) {
		synchronized (m_view.m_lock) {
			if (paint == null)
				throw new NullPointerException("paint is null");

			m_view.m_edgeDetails.setUnselectedPaint(model, paint);

			if (!isSelected())
				m_view.m_contentChanged = true;

			setSourceEdgeEnd(m_sourceEdgeEnd);
			setTargetEdgeEnd(m_targetEdgeEnd);
		}
	}

	@Override
	public Paint getUnselectedPaint() {
		return m_view.m_edgeDetails.unselectedPaint(model);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSelectedPaint(Paint paint) {
		synchronized (m_view.m_lock) {
			if (paint == null)
				throw new NullPointerException("paint is null");

			m_view.m_edgeDetails.setSelectedPaint(model, paint);

			if (isSelected())
				m_view.m_contentChanged = true;
		}
	}

	
	@Override
	public Paint getSelectedPaint() {
		return m_view.m_edgeDetails.selectedPaint(model);
	}

	@Override
	public Paint getSourceEdgeEndPaint() {
		return m_sourceUnselectedPaint;
	}

	@Override
	public Paint getSourceEdgeEndSelectedPaint() {
		return m_sourceSelectedPaint;
	}

	@Override
	public Paint getTargetEdgeEndPaint() {
		return m_targetUnselectedPaint;
	}

	@Override
	public Paint getTargetEdgeEndSelectedPaint() {
		return m_targetSelectedPaint;
	}

	@Override
	public void setSourceEdgeEndSelectedPaint(Paint paint) {
		synchronized (m_view.m_lock) {
			if (paint == null)
				throw new NullPointerException("paint is null");

			m_sourceSelectedPaint = paint;

			if (isSelected()) {
				m_view.m_edgeDetails.overrideSourceArrowPaint(model,
						m_sourceSelectedPaint);
				m_view.m_contentChanged = true;
			}
		}
	}


	@Override
	public void setTargetEdgeEndSelectedPaint(Paint paint) {
		
		synchronized (m_view.m_lock) {
			if (paint == null)
				throw new NullPointerException("paint is null");

			m_targetSelectedPaint = paint;

			if (isSelected()) {
				m_view.m_edgeDetails.overrideTargetArrowSelectedPaint(model,
						m_targetSelectedPaint);
				m_view.m_contentChanged = true;
			}
		}
	}


	@Override
	public void setSourceEdgeEndPaint(Paint paint) {
		synchronized (m_view.m_lock) {
			if (paint == null)
				throw new NullPointerException("paint is null");

			m_sourceUnselectedPaint = paint;

			if (!isSelected()) {
				m_view.m_contentChanged = true;
			}
		}
	}

	@Override
	public void setTargetEdgeEndPaint(Paint paint) {
		synchronized (m_view.m_lock) {
			if (paint == null)
				throw new NullPointerException("paint is null");

			m_targetUnselectedPaint = paint;

			if (!isSelected()) {
				m_view.m_contentChanged = true;
			}
		}
	}

	
	@Override public void select() {
		final boolean somethingChanged;

		synchronized (m_view.m_lock) {
			somethingChanged = selectInternal(false);

			if (somethingChanged)
				m_view.m_contentChanged = true;
		}
	}

	// Should synchronize around m_view.m_lock.
	boolean selectInternal(boolean selectAnchors) {
		if (m_selected)
			return false;

		m_selected = true;
		m_view.m_edgeDetails.select(model);		
		m_view.m_selectedEdges.insert(m_inx);

		List<Handle> handles = m_view.m_edgeDetails.bend(model).getAllHandles();
		for (int j = 0; j < handles.size(); j++) {
			final Handle handle = handles.get(j);
			final Point2D newPoint = handle.calculateHandleLocation(m_view.getViewModel(),this);
			final double x = newPoint.getX();
			final double y = newPoint.getY();
			final double halfSize = m_view.getAnchorSize() / 2.0;
			
			m_view.m_spacialA.insert((m_inx << 6) | j,
					(float) (x - halfSize), (float) (y - halfSize),
					(float) (x + halfSize), (float) (y + halfSize));

			if (selectAnchors)
				m_view.m_selectedAnchors.insert((m_inx << 6) | j);
		}
		return true;
	}

	
	@Override public void unselect() {
		final boolean somethingChanged;

		synchronized (m_view.m_lock) {
			somethingChanged = unselectInternal();

			if (somethingChanged)
				m_view.m_contentChanged = true;
		}
	}

	// Should synchronize around m_view.m_lock.
	boolean unselectInternal() {
		if (!m_selected)
			return false;

		m_selected = false;
		m_view.m_edgeDetails.unselect(model);
		m_view.m_selectedEdges.delete(m_inx);

		final int numHandles = m_view.m_edgeDetails.bend(model).getAllHandles().size();
		for (int j = 0; j < numHandles; j++) {
			m_view.m_selectedAnchors.delete((m_inx << 6) | j);
			m_view.m_spacialA.delete((m_inx << 6) | j);
		}
		return true;
	}

	@Override
	public boolean setSelected(boolean state) {
		if (state)
			select();
		else
			unselect();

		return true;
	}

	@Override
	public boolean isSelected() {
		return m_selected;
	}

	@Override
	public boolean getSelected() {
		return m_selected;
	}

	
	@Override
	final public boolean isHidden() {
		return m_view.isHidden(this);
	}

	
	@Override
	public void setSourceEdgeEnd(final int rendererTypeID) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.overrideSourceArrow(model, (byte) rendererTypeID);
		}

		m_sourceEdgeEnd = rendererTypeID;
		m_view.m_contentChanged = true;
	}

	@Override
	public void setTargetEdgeEnd(final int rendererTypeID) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.overrideTargetArrow(model, (byte) rendererTypeID);
		}

		m_targetEdgeEnd = rendererTypeID;
		m_view.m_contentChanged = true;

	}

	@Override
	public int getSourceEdgeEnd() {
		return m_sourceEdgeEnd;
	}

	@Override
	public int getTargetEdgeEnd() {
		return m_targetEdgeEnd;
	}

	@Override
	public void clearBends() {
		removeAllHandles();
	}

	@Override
	public Label getLabel() {
		return this;
	}

	@Override
	public void setToolTip(String tip) {
		m_toolTipText = tip;
	}

	@Override
	public String getToolTip() {
		return m_toolTipText;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Paint getTextPaint() {
		synchronized (m_view.m_lock) {
			return m_view.m_edgeDetails.labelPaint(model, 0);
		}
	}

	@Override
	public void setTextPaint(Paint textPaint) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.overrideLabelPaint(model, 0, textPaint);
			m_view.m_contentChanged = true;
		}
	}

	@Override
	public String getText() {
		synchronized (m_view.m_lock) {
			return m_view.m_edgeDetails.labelText(model, 0);
		}
	}

	@Override
	public void setText(final String text) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.overrideLabelText(model, 0, text);

			if (DEFAULT_LABEL_TEXT.equals(m_view.m_edgeDetails.labelText(model, 0)))
				m_view.m_edgeDetails.overrideLabelCount(model, 0); // TODO is this correct?
			else
				m_view.m_edgeDetails.overrideLabelCount(model, 1);

			m_view.m_contentChanged = true;
		}
	}

	@Override
	public Font getFont() {
		synchronized (m_view.m_lock) {
			return m_view.m_edgeDetails.labelFont(model, 0);
		}
	}
	
	
	@Override
	public void setFont(final Font font) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.overrideLabelFont(model, 0, font);
			m_view.m_contentChanged = true;
		}
	}


	protected final void moveHandleInternal(final int inx, double x, double y) {
		final Bend bend = m_view.m_edgeDetails.bend(model);
		final Handle handle = bend.getAllHandles().get(inx);
		handle.defineHandle(m_view.getViewModel(),this, x, y);

		if (m_view.m_spacialA.delete((m_inx << 6) | inx))
			m_view.m_spacialA.insert((m_inx << 6) | inx,
					(float) (x - (m_view.getAnchorSize() / 2.0d)),
					(float) (y - (m_view.getAnchorSize() / 2.0d)),
					(float) (x + (m_view.getAnchorSize() / 2.0d)),
					(float) (y + (m_view.getAnchorSize() / 2.0d)));
	}


	/**
	 * Add a new handle and returns its index.
	 * 
	 * @param pt location of handle
	 * @return new handle index.
	 */
	protected int addHandlePoint(final Point2D pt) {
		synchronized (m_view.m_lock) {
			
			// Obtain existing Bend object
			final Bend bend = m_view.m_edgeDetails.bend(model, true);
			
			if (bend.getAllHandles().size() == 0) {
				// anchors object is empty. Add first handle.
				addHandleInternal(0, pt);
				// Index of this handle, which is first (0)
				return 0;
			}

			final Point2D sourcePt = m_view.getDNodeView(getEdge().getSource()).getOffset();
			final Point2D targetPt = m_view.getDNodeView(getEdge().getTarget()).getOffset();
			final Handle firstHandle = bend.getAllHandles().get(0); 
			final Point2D point = firstHandle.calculateHandleLocation(m_view.getViewModel(),this);
			double bestDist = (pt.distance(sourcePt) + pt.distance(point)) - sourcePt.distance(point);
			int bestInx = 0;

			for (int i = 1; i < bend.getAllHandles().size(); i++) {
				final Handle handle1 = bend.getAllHandles().get(i);
				final Handle handle2 = bend.getAllHandles().get(i-1);
				final Point2D point1 = handle1.calculateHandleLocation(m_view.getViewModel(),this);
				final Point2D point2 = handle2.calculateHandleLocation(m_view.getViewModel(),this);

				final double distCand = (pt.distance(point2) + pt.distance(point1)) - point1.distance(point2);

				if (distCand < bestDist) {
					bestDist = distCand;
					bestInx = i;
				}
			}

			final int lastIndex = bend.getAllHandles().size() - 1;
			final Handle lastHandle = bend.getAllHandles().get(lastIndex);
			final Point2D lastPoint = lastHandle.calculateHandleLocation(m_view.getViewModel(),this);
			
			final double lastCand = (pt.distance(targetPt) + pt.distance(lastPoint)) - targetPt.distance(lastPoint);

			if (lastCand < bestDist) {
				bestDist = lastCand;
				bestInx = bend.getAllHandles().size();
			}

			addHandleInternal(bestInx, pt);

			return bestInx;
		}
	}

	
	/**
	 * Insert a new handle to bend object.
	 * 
	 * @param insertInx
	 * @param handleLocation
	 */
	private void addHandleInternal(final int insertInx, final Point2D handleLocation) {
		synchronized (m_view.m_lock) {
			final Bend bend = m_view.m_edgeDetails.bend(model);			
			final Handle handle = new HandleImpl(handleLocation.getX(), handleLocation.getY());
			handle.defineHandle(m_view, this, handleLocation.getX(), handleLocation.getY());
			
			bend.insertHandleAt(insertInx, handle);

			if (m_selected) {
				for (int j = bend.getAllHandles().size() - 1; j > insertInx; j--) {
					m_view.m_spacialA.exists((m_inx << 6) | (j - 1),
							m_view.m_extentsBuff, 0);
					m_view.m_spacialA.delete((m_inx << 6) | (j - 1));
					m_view.m_spacialA.insert((m_inx << 6) | j,
							m_view.m_extentsBuff[0], m_view.m_extentsBuff[1],
							m_view.m_extentsBuff[2], m_view.m_extentsBuff[3]);

					if (m_view.m_selectedAnchors.delete((m_inx << 6) | (j - 1)))
						m_view.m_selectedAnchors.insert((m_inx << 6) | j);
				}
				
				m_view.m_spacialA.insert((m_inx << 6) | insertInx,
						(float) (handleLocation.getX() - (m_view.getAnchorSize() / 2.0d)),
						(float) (handleLocation.getY() - (m_view.getAnchorSize() / 2.0d)),
						(float) (handleLocation.getX() + (m_view.getAnchorSize() / 2.0d)),
						(float) (handleLocation.getY() + (m_view.getAnchorSize() / 2.0d)));
			}

			m_view.m_contentChanged = true;
		}
	}

	
	void removeHandle(int inx) {
		synchronized (m_view.m_lock) {
			final Bend bend = m_view.m_edgeDetails.bend(model);
			bend.removeHandleAt(inx);
			//m_anchors.remove(inx);

			if (m_selected) {
				m_view.m_spacialA.delete((m_inx << 6) | inx);
				m_view.m_selectedAnchors.delete((m_inx << 6) | inx);

				for (int j = inx; j < bend.getAllHandles().size(); j++) {
					m_view.m_spacialA.exists((m_inx << 6) | (j + 1),
							m_view.m_extentsBuff, 0);
					m_view.m_spacialA.delete((m_inx << 6) | (j + 1));
					m_view.m_spacialA.insert((m_inx << 6) | j,
							m_view.m_extentsBuff[0], m_view.m_extentsBuff[1],
							m_view.m_extentsBuff[2], m_view.m_extentsBuff[3]);

					if (m_view.m_selectedAnchors.delete((m_inx << 6) | (j + 1)))
						m_view.m_selectedAnchors.insert((m_inx << 6) | j);
				}
			}
			m_view.m_contentChanged = true;
		}
	}

	
	private void removeAllHandles() {
		synchronized (m_view.m_lock) {
			final Bend bend = m_view.m_edgeDetails.bend(model);
			
			if (m_selected) {
				for (int j = 0; j < bend.getAllHandles().size(); j++) {
					m_view.m_spacialA.delete((m_inx << 6) | j);
					m_view.m_selectedAnchors.delete((m_inx << 6) | j);
				}
			}
			m_view.m_contentChanged = true;
		}
	}


	// Interface org.cytoscape.graph.render.immed.EdgeAnchors:
	@Override
	public int numAnchors() {
		final Bend bend; 
		if(isValueLocked(DVisualLexicon.EDGE_BEND))
			bend = this.getVisualProperty(DVisualLexicon.EDGE_BEND);
		else
			bend = m_view.m_edgeDetails.bend(model);
		
		final int numHandles = bend.getAllHandles().size();
		
		if (numHandles == 0)
			return 0;
		
		if (m_view.m_edgeDetails.lineType(model) == EdgeView.CURVED_LINES)
			return numHandles;
		else
			return 2 * numHandles;
	}

	/**
	 * Actual method to be used in the Graph Renderer.
	 */
	@Override
	public void getAnchor(int anchorIndex, float[] anchorArr, int offset) {
		final Bend bend; 
		if(isValueLocked(DVisualLexicon.EDGE_BEND))
			bend = this.getVisualProperty(DVisualLexicon.EDGE_BEND);
		else
			bend = m_view.m_edgeDetails.bend(model);
		
		final Handle handle;
		if (m_view.m_edgeDetails.lineType(model) == EdgeView.CURVED_LINES)
			handle = bend.getAllHandles().get(anchorIndex);
		else
			handle = bend.getAllHandles().get(anchorIndex/2);

		final Point2D newPoint = handle.calculateHandleLocation(m_view.getViewModel(),this);
		anchorArr[offset] = (float) newPoint.getX();
		anchorArr[offset + 1] = (float) newPoint.getY();
	}


	public void setLabelWidth(double width) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.overrideLabelWidth(model, width);
			m_view.m_contentChanged = true;
		}
	}

	public double getLabelWidth() {
		synchronized (m_view.m_lock) {
			return m_view.m_edgeDetails.labelWidth(model);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getTransparency() {
		return transparency;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTransparency(final int trans) {
		synchronized (m_view.m_lock) {
			if (trans < 0 || trans > 255) {
				// If out of range, use default value.
				transparency = BasicVisualLexicon.EDGE_TRANSPARENCY.getDefault();
			} else
				transparency = trans;

			final Color unselectedColor = (Color) getUnselectedPaint();
			if(unselectedColor.getAlpha() != transparency)
				setUnselectedPaint(new Color(unselectedColor.getRed(), unselectedColor.getGreen(), unselectedColor.getBlue(), transparency));
			
			m_view.m_contentChanged = true;
		}
	}
	
	@Override
	public void setBend(final Bend bend) {
		synchronized (m_view.m_lock) {
			m_view.m_edgeDetails.m_edgeBends.put(model, bend);
		}
		m_view.m_contentChanged = true;
	}
	
	
	@Override
	public Bend getBend() {
		synchronized (m_view.m_lock) {
			return m_view.m_edgeDetails.bend(model);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T, V extends T> void applyVisualProperty(final VisualProperty<? extends T> vpOriginal, V value) {
		VisualProperty<?> vp = vpOriginal;
		
		// If value is null, simply use the VP's default value.
		if(value == null)
			value = (V) vp.getDefault();
		
		if (vp == DVisualLexicon.EDGE_STROKE_SELECTED_PAINT) {
			setSelectedPaint((Paint) value);
		} else if (vp == DVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT) {
			if(value == null)
				return;
			else
				setUnselectedPaint((Paint) value);
		} else if (vp == DVisualLexicon.EDGE_SELECTED_PAINT) {
			if(value == null)
				return;
			setSelectedPaint((Paint) value);			
			setSourceEdgeEndSelectedPaint((Paint) value);
			setTargetEdgeEndSelectedPaint((Paint) value);

		} else if (vp == DVisualLexicon.EDGE_UNSELECTED_PAINT) {
			if(value == null)
				return;
			
			setSourceEdgeEndPaint((Paint) value);
			setTargetEdgeEndPaint((Paint) value);
			setUnselectedPaint((Paint) value);
		} else if (vp == DVisualLexicon.EDGE_WIDTH) {
			
			final float currentWidth = this.getStrokeWidth();
			
			final float newWidth = ((Number) value).floatValue();			
			if(currentWidth != newWidth) {
				setStrokeWidth(newWidth);
				setStroke(DLineType.getDLineType(lineType).getStroke(newWidth));
			}			
		} else if (vp == DVisualLexicon.EDGE_LINE_TYPE) {
			lineType = (LineType) value;
			final Stroke newStroke = DLineType.getDLineType(lineType).getStroke(getStrokeWidth());
			setStroke(newStroke);
		} else if (vp == DVisualLexicon.EDGE_TRANSPARENCY) {
			setTransparency(((Number) value).intValue());
		} else if (vp == DVisualLexicon.EDGE_LABEL_TRANSPARENCY) {
			final int opacity = ((Number) value).intValue();
			final Color labelColor = (Color) getTextPaint();
			if(labelColor.getAlpha() != opacity)
				setTextPaint(new Color(labelColor.getRed(), labelColor.getGreen(), labelColor.getBlue(), opacity));
		} else if (vp == DVisualLexicon.EDGE_SOURCE_ARROW_SELECTED_PAINT) {
			setSourceEdgeEndSelectedPaint((Paint) value);
		} else if (vp == DVisualLexicon.EDGE_TARGET_ARROW_SELECTED_PAINT) {
			setTargetEdgeEndSelectedPaint((Paint) value);
		} else if (vp == DVisualLexicon.EDGE_SOURCE_ARROW_UNSELECTED_PAINT) {
			setSourceEdgeEndPaint((Paint) value);
		} else if (vp == DVisualLexicon.EDGE_TARGET_ARROW_UNSELECTED_PAINT) {
			setTargetEdgeEndPaint((Paint) value);
		} else if (vp == BasicVisualLexicon.EDGE_SELECTED) {
			setSelected((Boolean) value);
		} else if (vp == BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE) {
			final ArrowShape shape = (ArrowShape) value;
			final String shapeID = shape.getSerializableString();
			setTargetEdgeEnd(DArrowShape.parseArrowText(shapeID).getRendererTypeID());
		} else if (vp == BasicVisualLexicon.EDGE_SOURCE_ARROW_SHAPE) {
			final ArrowShape shape = (ArrowShape) value;
			final String shapeID = shape.getSerializableString();
			setSourceEdgeEnd(DArrowShape.parseArrowText(shapeID).getRendererTypeID());
		} else if (vp == BasicVisualLexicon.EDGE_LABEL) {
			setText(value.toString());
		} else if (vp == DVisualLexicon.EDGE_TOOLTIP) {
			setToolTip(value.toString());
		} else if (vp == DVisualLexicon.EDGE_LABEL_FONT_FACE) {
			final Font newFont = ((Font) value).deriveFont(fontSize);
			setFont(newFont);
		} else if (vp == DVisualLexicon.EDGE_LABEL_FONT_SIZE) {
			float newSize = ((Number) value).floatValue();
			if (newSize != this.fontSize) {
				final Font newFont = getFont().deriveFont(newSize);
				setFont(newFont);
				fontSize = newSize;
			}
		} else if (vp == BasicVisualLexicon.EDGE_LABEL_COLOR) {
			setTextPaint((Paint) value);
		} else if (vp == BasicVisualLexicon.EDGE_VISIBLE) {
			if (((Boolean) value).booleanValue())
				m_view.showGraphObject(this);
			else
				m_view.hideGraphObject(this);
		} else if(vp == DVisualLexicon.EDGE_CURVED) {
			final Boolean curved = (Boolean) value;
			if(curved)
				setLineType(EdgeView.CURVED_LINES);
			else
				setLineType(EdgeView.STRAIGHT_LINES);
		} else if(vp == DVisualLexicon.EDGE_BEND) {
			setBend((Bend) value);
		}		
	}
}
