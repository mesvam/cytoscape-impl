package org.cytoscape.ding.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.RootPaneContainer;
import javax.swing.Timer;

import org.cytoscape.cg.event.CustomGraphicsLibraryUpdatedListener;
import org.cytoscape.ding.DVisualLexicon;
import org.cytoscape.ding.PrintLOD;
import org.cytoscape.ding.debug.DebugProgressMonitorFactory;
import org.cytoscape.ding.debug.DingDebugMediator;
import org.cytoscape.ding.icon.VisualPropertyIconFactory;
import org.cytoscape.ding.impl.canvas.CompositeGraphicsCanvas;
import org.cytoscape.ding.impl.canvas.MainRenderComponent;
import org.cytoscape.ding.impl.canvas.NetworkImageBuffer;
import org.cytoscape.ding.impl.canvas.NetworkTransform;
import org.cytoscape.ding.impl.cyannotator.AnnotationFactoryManager;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.event.DebounceTimer;
import org.cytoscape.graph.render.stateful.EdgeDetails;
import org.cytoscape.graph.render.stateful.GraphLOD;
import org.cytoscape.graph.render.stateful.GraphLOD.RenderEdges;
import org.cytoscape.graph.render.stateful.LabelInfoCache;
import org.cytoscape.graph.render.stateful.LabelInfoProvider;
import org.cytoscape.graph.render.stateful.NodeDetails;
import org.cytoscape.graph.render.stateful.RenderDetailFlags;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewListener;
import org.cytoscape.view.model.CyNetworkViewSnapshot;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.UpdateNetworkPresentationEvent;
import org.cytoscape.view.model.spacial.SpacialIndex2D;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2016 The Cytoscape Consortium
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
 * This class acts as a controller for rendering one CyNetworkView.
 * It initializes all the classes needed for rendering and acts as a bridge
 * between them.
 *
 */
public class DRenderingEngine implements RenderingEngine<CyNetwork>, Printable, CyNetworkViewListener {

	private static final Logger logger = LoggerFactory.getLogger(DRenderingEngine.class);
	protected static int DEF_SNAPSHOT_SIZE = 400;
	
	public enum UpdateType {
		ALL_FAST,         // Render a fast frame only
		ALL_FULL,         // Render a fast frame, then start rendering a full frame async
		JUST_ANNOTATIONS, // Just render annotations fast
		JUST_EDGES,       // for animated edges
		JUST_SELECTION;   // Just re-render selected nodes/edges
		
		public boolean renderEdges() {
			return this == ALL_FAST || this == ALL_FULL || this == JUST_EDGES || this == JUST_SELECTION;
		}
		public boolean renderNodes() {
			return this == ALL_FAST || this == ALL_FULL || this == JUST_SELECTION;
		}
		public boolean renderAnnotations() {
			return this == ALL_FAST || this == ALL_FULL || this == JUST_ANNOTATIONS;
		}
	}
	
	private final CyServiceRegistrar serviceRegistrar;
	private final CyEventHelper eventHelper;
	
	private final DVisualLexicon lexicon;

	private final CyNetworkView viewModel;
	private CyNetworkViewSnapshot viewModelSnapshot;
	
	// Common object lock used for state synchronization
	final DingLock dingLock = new DingLock();

	private final NodeDetails nodeDetails;
	private final EdgeDetails edgeDetails;
	
	private final DingGraphLODAll dingGraphLODAll = new DingGraphLODAll();
	private final DingGraphLOD dingGraphLOD;

	private MainRenderComponent renderComponent;
	private NetworkPicker picker;
	
	// Snapshot of current view.  Will be updated by CONTENT_CHANGED event.
	private BufferedImage snapshotImage;
	// Represents current snapshot is latest version or not.
	private boolean latestSnapshot;

	private final Properties props;
	private final CyAnnotator cyAnnotator;
	
	//Flag that indicates that the content has changed and the graph needs to be redrawn.
	private volatile boolean contentChanged = true;

	private final List<ContentChangeListener> contentChangeListeners = new CopyOnWriteArrayList<>();
	private final List<ThumbnailChangeListener> thumbnailChangeListeners = new CopyOnWriteArrayList<>();
	
	private Timer animationTimer;
	private final Timer checkDirtyTimer;
	private final DebounceTimer eventFireTimer;
	
	private final LabelInfoCache labelInfoCache;
	
	private final BendStore bendStore;
	private InputHandlerGlassPane inputHandler = null;
	private DebugProgressMonitorFactory debugProgressMonitorFactory;
	
	// This is Ding's own rendering thread. All rendering is single-threaded, but off the EDT
	private final ExecutorService singleThreadExecutor;
	
	
	public DRenderingEngine(
			CyNetworkView view,
			DVisualLexicon dingLexicon,
			AnnotationFactoryManager annMgr,
			DingGraphLOD dingGraphLOD,
			HandleFactory handleFactory,
			CyServiceRegistrar registrar
	) {
		this.serviceRegistrar = registrar;
		this.eventHelper = registrar.getService(CyEventHelper.class);
		this.props = new Properties();
		this.viewModel = view;
		this.lexicon = dingLexicon;
		this.dingGraphLOD = dingGraphLOD;
		
		this.singleThreadExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("ding-" + thread.getName());
			return thread;
		});
		
		this.bendStore = new BendStore(this, eventHelper, handleFactory);
		
		nodeDetails = new DNodeDetails(lexicon, registrar);
		edgeDetails = new DEdgeDetails(this);
		
		// Finally, intialize our annotations
		cyAnnotator = new CyAnnotator(this, annMgr, registrar);
		registrar.registerService(cyAnnotator, SessionAboutToBeSavedListener.class);
		registrar.registerService(cyAnnotator, CustomGraphicsLibraryUpdatedListener.class);
		
		renderComponent = new MainRenderComponent(this, dingGraphLOD);
		picker = new NetworkPicker(this, null);
		
		// Updating the snapshot for nested networks
		addContentChangeListener(() -> latestSnapshot = false);

		viewModelSnapshot = viewModel.createSnapshot();
		
		labelInfoCache = new LabelInfoCache(1000, DingDebugMediator.showDebugPanel(registrar)); // MKTODO should maxSize be hardcoded?
		
		eventFireTimer = new DebounceTimer(240);
		
		// Check if the view model has changed approximately 30 times per second
		checkDirtyTimer = new Timer(30, e -> checkModelIsDirty());
		checkDirtyTimer.setRepeats(true);
		checkDirtyTimer.start();
		
		renderComponent.addTransformChangeListener(() -> {
			fireThumbnailChanged(null);
		});
	}
	
	public void install(RootPaneContainer rootPane) {
		InputHandlerGlassPane glassPane = getInputHandlerGlassPane();
		rootPane.setGlassPane(glassPane);
		rootPane.setContentPane(renderComponent);
		glassPane.setVisible(true);
	}
	
	public void install(JComponent component) {
		component.setLayout(new BorderLayout());
		component.add(renderComponent, BorderLayout.CENTER);
	}
	
	public void setDebugProgressMonitorFactory(DebugProgressMonitorFactory factory) {
		this.debugProgressMonitorFactory = factory;
	}
	
	public DebugProgressMonitorFactory getDebugProgressMonitorFactory() {
		return debugProgressMonitorFactory;
	}
	
	public Image getImage() {
		return renderComponent.getImage();
	}
	
	public ExecutorService getSingleThreadExecutorService() {
		return singleThreadExecutor;
	}
	
	public LabelInfoProvider getLabelCache() {
		return labelInfoCache;
	}
	
	public Rectangle getComponentBounds() {
		return renderComponent.getBounds();
	}
	
	public Point getComponentCenter() {
		var bounds = renderComponent.getBounds();
		int centerX = bounds.x + bounds.width  / 2;
		int centerY = bounds.y + bounds.height / 2;
		return new Point(centerX, centerY);
	}
	
	public NetworkPicker getPicker() {
		return picker;
	}

	public NetworkTransform getTransform() {
		return renderComponent.getTransform();
	}
	
	public GraphLOD getGraphLOD() {
		return dingGraphLOD;
	}
	
	
	/**
	 * This is being called by a Swing Timer, so this method is being run on the EDT.
	 * Painting is also done on the EDT. This is how we make sure that viewModelSnapshot does not
	 * change while a frame is being rendered.
	 * 
	 * Also the EDT will coalesce paint events, so if the timer runs faster than the frame rate the
	 * EDT will take care of that.
	 */
	private void checkModelIsDirty() {
		boolean dirty = viewModel.dirty(true);
		if(dirty) {
			updateModel();
		}
		if(dirty || contentChanged) {
			if(viewModelSnapshot.isSelectionIncreased()) {
				updateView(UpdateType.JUST_SELECTION);
			} else {
				updateView(UpdateType.ALL_FULL);
			}
		}
		contentChanged = false;
	}
	
	
	public void updateView(UpdateType updateType) {
		renderComponent.updateView(updateType);
		
		if(contentChanged) {
			fireContentChanged();
		}
		setContentChanged(false);
		
		// Fire this event on another thread (and debounce) so that it doesn't block the renderer
		if(!eventFireTimer.isShutdown())
			eventFireTimer.debounce(() -> eventHelper.fireEvent(new UpdateNetworkPresentationEvent(getViewModel())));
	}
	
	private void updateModel() {
		// create a new snapshot, this should be very fast
		viewModelSnapshot = viewModel.createSnapshot();
		
		// Check for important changes between snapshots
		Paint backgroundPaint = viewModelSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT);
		renderComponent.setBackgroundPaint(backgroundPaint);
		
		Collection<View<CyEdge>> animatedEdges = viewModelSnapshot.getTrackedEdges(DingNetworkViewFactory.ANIMATED_EDGES);
		edgeDetails.updateAnimatedEdges(animatedEdges);
		if(animatedEdges.isEmpty() && animationTimer != null) {
			animationTimer.stop();
			animationTimer = null;
		} else if(!animatedEdges.isEmpty() && animationTimer == null) {
			animationTimer = new Timer(200, e -> advanceAnimatedEdges());
			animationTimer.setRepeats(true);
			animationTimer.start();
		}
		
		// update LOD
		boolean hd = viewModelSnapshot.getVisualProperty(DVisualLexicon.NETWORK_FORCE_HIGH_DETAIL);
		renderComponent.setLOD(hd ? dingGraphLODAll : dingGraphLOD);
		
		// update view (for example if "fit selected" was run)
		double x = viewModelSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION);
		double y = viewModelSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION);
		renderComponent.setCenter(x, y);
		
		double scaleFactor = viewModelSnapshot.getVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR);
		renderComponent.setScaleFactor(scaleFactor);
		
		boolean annotationSelection = viewModelSnapshot.getVisualProperty(DVisualLexicon.NETWORK_ANNOTATION_SELECTION);
		if(!annotationSelection)
			cyAnnotator.getAnnotationSelection().clear();
		
		setContentChanged(true);
	}
	
	public Color getBackgroundColor() {
		return renderComponent.getBackgroundPaint();
	}
	
	private void advanceAnimatedEdges() {
		edgeDetails.advanceAnimatedEdges();
		
		RenderDetailFlags flags = renderComponent.getLastFastRenderFlags();
		if(flags.renderEdges() != RenderEdges.NONE) {
			updateView(UpdateType.JUST_EDGES);
		}
	}
	
	
	public BendStore getBendStore() {
		return bendStore;
	}
	
	public synchronized InputHandlerGlassPane getInputHandlerGlassPane() {
		if(inputHandler == null) {
			inputHandler = new InputHandlerGlassPane(serviceRegistrar, this);
		}
		return inputHandler;
	}
	
	/**
	 * Mainly for using as a parent when showing dialogs and menus.
	 */
	public JComponent getComponent() {
		return getInputHandlerGlassPane();
	}

	
	public NodeDetails getNodeDetails() {
		return nodeDetails;
	}
	
	public EdgeDetails getEdgeDetails() {
		return edgeDetails;
	}
	
	
	public void setContentChanged() {
		setContentChanged(true);
	}
	
	private void setContentChanged(boolean b) {
		contentChanged = b;
	}
	
	private void fireContentChanged() {
		for(var l : contentChangeListeners) {
			l.contentChanged();
		}
		fireThumbnailChanged(null);
	}
	
	public void addContentChangeListener(ContentChangeListener l) {
		contentChangeListeners.add(l);
	}

	public void removeContentChangeListener(ContentChangeListener l) {
		contentChangeListeners.remove(l);
	}
	
	public void addTransformChangeListener(TransformChangeListener l) {
		renderComponent.addTransformChangeListener(l);
	}
	
	public void removeTransformChangeListener(TransformChangeListener l) {
		renderComponent.removeTransformChangeListener(l);
	}
	
	public void addThumbnailChangeListener(ThumbnailChangeListener l) {
		thumbnailChangeListeners.add(l);
	}
	
	public void removeThumbnailChangeListener(ThumbnailChangeListener l) {
		thumbnailChangeListeners.remove(l);
	}
	
	void fireThumbnailChanged(Image image) {
		for(var l : thumbnailChangeListeners) {
			l.thumbnailChanged(image);
		}
	}

	
	/**
	 * Set the zoom level and redraw the view.
	 */
	public void setZoom(double zoom) {
		synchronized (dingLock) {
			renderComponent.setScaleFactor(checkZoom(zoom, renderComponent.getTransform().getScaleFactor()));
		}
	}
	
	public double getZoom() {
		return renderComponent.getTransform().getScaleFactor();
	}
	
	@Override
	public void handleFitContent() {
		eventHelper.flushPayloadEvents();

		synchronized (dingLock) {
			if(!renderComponent.isInitialized()) {
				renderComponent.setInitializedCallback(() -> {
					renderComponent.setInitializedCallback(null);
					handleFitContent();
				});
				return;
			}
			
			// make sure we use the latest snapshot, don't wait for timer to check dirty flag
			CyNetworkViewSnapshot netViewSnapshot = getViewModel().createSnapshot();
			if(netViewSnapshot.getNodeCount() == 0)
				return;
			
			NetworkTransform transform = renderComponent.getTransform();
			if(transform.getWidth() == 0 || transform.getHeight() == 0)
				return;
			
			double[] extents = new double[4];
			netViewSnapshot.getSpacialIndex2D().getMBR(extents); // extents of the network
			cyAnnotator.adjustBoundsToIncludeAnnotations(extents); // extents of the annotation canvases

			netViewSnapshot.getMutableNetworkView().batch(netView -> {
				if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION))
					netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, (extents[0] + extents[2]) / 2.0d);
				
				if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION))
					netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, (extents[1] + extents[3]) / 2.0d);
	
				if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_SCALE_FACTOR)) {
					// Apply a factor 0.98 to zoom, so that it leaves a small border around the network and any annotations.
					final double zoom = Math.min(((double) transform.getWidth())  /  (extents[2] - extents[0]), 
					                             ((double) transform.getHeight()) /  (extents[3] - extents[1])) * 0.98;
					// Update view model.  Zoom Level should be modified.
					netView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, zoom);
				}
			});
		}
	}
	
	@Override
	public void handleUpdateView() {
		updateModel();
		updateView(UpdateType.ALL_FULL);
	}
	
	
	public void zoom(int ticks) {
		if(getViewModelSnapshot().isValueLocked(BasicVisualLexicon.NETWORK_SCALE_FACTOR))
			return;
		
		double factor;
		if (ticks < 0)
			factor = 1.1; // scroll up, zoom in
		else if (ticks > 0)
			factor = 0.9; // scroll down, zoom out
		else
			return;
		
		double scaleFactor = renderComponent.getTransform().getScaleFactor() * factor;
		setZoom(scaleFactor);
		
		getViewModel().batch(netView -> {
			netView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, scaleFactor);
		}, false);
		
	}
	
	
	public Panner startPan() {
		return new Panner();
	}
	
	public class Panner {
		private boolean changed = false;
		
		private Panner() {
			renderComponent.startPan();
		}
		
		public void continuePan(double dx, double dy) {
			synchronized (dingLock) { // MKTODO is this necessary?
				changed = true;
			
				NetworkTransform transform = renderComponent.getTransform();
				double x = transform.getCenterX() + dx;
				double y = transform.getCenterY() + dy;
				renderComponent.setCenter(x, y);
				
				updateView(UpdateType.ALL_FAST);
			}
		}
		
		public void endPan() {
			renderComponent.endPan();
			
			if(changed) {
				updateCenterVPs();
				updateView(UpdateType.ALL_FULL);
			}
		} 
	}
	
	
	/**
	 * Don't use this method to perform continuous mouse pan motions. 
	 * Only use to set the center as a one-time operation.
	 */
	public void setCenter(double x, double y) {
		synchronized (dingLock) {
			renderComponent.setCenter(x,y);
			updateCenterVPs();
		}
	}

	private void updateCenterVPs() {
		synchronized (dingLock) {
			getViewModel().batch(netView -> {
				NetworkTransform transform = renderComponent.getTransform();
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, transform.getCenterX());
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, transform.getCenterY());
			}, false); // don't set the dirty flag
		}
	}
	
	
	@Override
	public void handleFitSelected() {
		eventHelper.flushPayloadEvents();
		// Its not common for fitSelected() to be called immediately after creating a network 
		// like it is for fitContent(), so we won't worry about setting an initialized callback.
		
		// make sure we use the latest snapshot, don't wait for timer to check dirty flag
		CyNetworkViewSnapshot netViewSnapshot = getViewModel().createSnapshot();
		
		SpacialIndex2D<Long> spacial = netViewSnapshot.getSpacialIndex2D();
		Collection<View<CyNode>> selectedElms = netViewSnapshot.getTrackedNodes(DingNetworkViewFactory.SELECTED_NODES);
		if(selectedElms.isEmpty())
			return;
		
		float[] extents = new float[4];

		float xMin = Float.POSITIVE_INFINITY, yMin = Float.POSITIVE_INFINITY;
		float xMax = Float.NEGATIVE_INFINITY, yMax = Float.NEGATIVE_INFINITY;

		View<CyNode> leftMost = null;
		View<CyNode> rightMost = null;

		for(View<CyNode> nodeView : selectedElms) {
			spacial.get(nodeView.getSUID(), extents);
			if (extents[SpacialIndex2D.X_MIN] < xMin) {
				xMin = extents[SpacialIndex2D.X_MIN];
				leftMost = nodeView;
			}

			if (extents[SpacialIndex2D.X_MAX] > xMax) {
				xMax = extents[SpacialIndex2D.X_MAX];
				rightMost = nodeView;
			}

			yMin = Math.min(yMin, extents[SpacialIndex2D.Y_MIN]);
			yMax = Math.max(yMax, extents[SpacialIndex2D.Y_MAX]);
		}

		float xMinF = xMin - (getLabelWidth(leftMost) / 2);
		float xMaxF = xMax + (getLabelWidth(rightMost) / 2);
		float yMaxF = yMax;
		float yMinF = yMin;

		NetworkTransform transform = renderComponent.getTransform();
		
		netViewSnapshot.getMutableNetworkView().batch(netView -> {
			if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION)) {
				double zoom = Math.min(((double) transform.getWidth()) / (((double) xMaxF) - ((double) xMinF)),
						((double) transform.getHeight()) / (((double) yMaxF) - ((double) yMinF)));
				zoom = checkZoom(zoom, transform.getScaleFactor());
				
				// Update view model.  Zoom Level should be modified.
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_SCALE_FACTOR, zoom);
			}
			
			if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION)) {
				double xCenter = (((double) xMinF) + ((double) xMaxF)) / 2.0d;
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, xCenter);
			}
			
			if (!netView.isValueLocked(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION)) {
				double yCenter = (((double) yMinF) + ((double) yMaxF)) / 2.0d;
				netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, yCenter);
			}
		});
	}

	
	private int getLabelWidth(View<CyNode> nodeView) {
		var fontMetrics = renderComponent.getFontMetrics();
		if (nodeView == null || fontMetrics == null)
			return 0;

		String s = nodeDetails.getLabelText(nodeView);
		if (s == null)
			return 0;

		char[] lab = s.toCharArray();
		return fontMetrics.charsWidth(lab, 0, lab.length);
	}



	
	// File > Print
	@Override
	public int print(Graphics g, PageFormat pageFormat, int page) {
		if(page != 0)
			return NO_SUCH_PAGE;
		
		((Graphics2D) g).translate(pageFormat.getImageableX(), pageFormat.getImageableY());

		// make sure the whole image on the screen will fit to the printable area of the paper
		var transform = new NetworkTransform(renderComponent.getTransform());
		transform.setDPIScaleFactor(1.0);
		
		double image_scale = Math.min(pageFormat.getImageableWidth()  / transform.getWidth(),
									  pageFormat.getImageableHeight() / transform.getHeight());

		if (image_scale < 1.0d) {
			((Graphics2D)g).scale(image_scale, image_scale);
		}

		// from InternalFrameComponent
		g.clipRect(0, 0, renderComponent.getWidth(), renderComponent.getHeight());
		
		PrintLOD printLOD = new PrintLOD();
		CompositeGraphicsCanvas.paint((Graphics2D)g, getBackgroundColor(), printLOD, transform, this);
		
		return PAGE_EXISTS;
	}
	

	// File > Export Network to Image... (JPEG, PNG, PDF, POSTSCRIPT, SVG)
	@Override
	public void printCanvas(Graphics g) {
		final boolean contentChanged = this.contentChanged;
		
		// Check properties related to printing:
		boolean exportAsShape = "true".equalsIgnoreCase(props.getProperty("exportTextAsShape"));
		boolean transparent   = "true".equalsIgnoreCase(props.getProperty("exportTransparentBackground"));
		boolean hideLabels    = "true".equalsIgnoreCase(props.getProperty("exportHideLabels"));
		
		PrintLOD printLOD = new PrintLOD();
		printLOD.setPrintingTextAsShape(exportAsShape);
		printLOD.setExportLabels(!hideLabels);
		
		Color bg = transparent ? null : getBackgroundColor();
		
		// Don't use HiDPI transform when rendering an image.
		var transform = new NetworkTransform(renderComponent.getTransform());
		transform.setDPIScaleFactor(1.0);
		
		CompositeGraphicsCanvas.paint((Graphics2D)g, bg, printLOD, transform, this);
		
		// Keep previous dirty flags, otherwise the actual view canvas may not be updated next time.
		// (this method is usually only used to export the View as image, create thumbnails, etc,
		// therefore it should not flag the Graph View as updated, because the actual view canvas
		// may still have to be redrawn after this).
		setContentChanged(contentChanged);
	}
	
	
	/**
	 * Method to return a reference to an Image object, which represents the current network view.
	 * @param width Width of desired image.
	 * @param height Height of desired image.
	 */
	@Override 
	public Image createImage(int width, int height) {
		return createImage(width, height, false);
	}
	
	
	private Image createImage(int width, int height, boolean transparentBackground) {
		if (width < 0 || height < 0)
			throw new IllegalArgumentException("width and height arguments must be greater than zero");

		// Run on the same thread as renderComponent to make sure the canvas is not in the middle of painting
		// MKTODO could we reuse the birds-eye-view buffer instead of doing a full frame draw?
		Future<Image> future = getSingleThreadExecutorService().submit(() -> {
			// MKTODO copy-pasted from fitContent()
			double[] extents = new double[4];
			getViewModelSnapshot().getSpacialIndex2D().getMBR(extents); // extents of the network
			cyAnnotator.adjustBoundsToIncludeAnnotations(extents); // extents of the annotation canvases
			double xCenter = (extents[0] + extents[2]) / 2.0d;
			double yCenter = (extents[1] + extents[3]) / 2.0d;
			double zoom = Math.min(((double) width)  / (extents[2] - extents[0]), 
                                   ((double) height) / (extents[3] - extents[1])) * 0.98;
			
			NetworkTransform transform = new NetworkTransform(width, height, xCenter, yCenter, zoom);
			NetworkImageBuffer buffer = new NetworkImageBuffer(transform);
			Color bgColor = transparentBackground ? null : getBackgroundColor();
			
			CompositeGraphicsCanvas.paint(buffer.getGraphics(), bgColor, dingGraphLOD, transform, this);
			
			return buffer.getImage();
		});
		
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private double checkZoom(double zoom, double orig) {
		if (zoom > 0)
			return zoom;

		logger.debug("invalid zoom: " + zoom + "   using orig: " + orig);
		return orig;
	}


	@Override
	public Printable createPrintable() {
		return this;
	}
	
	@Override
	public Properties getProperties() {
		return this.props;
	}
	

	@Override
	public DVisualLexicon getVisualLexicon() {
		return lexicon;
	}


	// For now the viewModelSnapshot should only be re-assigned on the EDT.
	public CyNetworkViewSnapshot getViewModelSnapshot() {
		return viewModelSnapshot;
	}
	
	@Override
	public CyNetworkView getViewModel() {
		return viewModel;
	}

	@Override
	public <V> Icon createIcon(VisualProperty<V> vp, V value, int w, int h) {
		return VisualPropertyIconFactory.createIcon(value, w, h);
	}

	/**
	 * Returns the current snapshot image of this view.
	 *
	 * <p>
	 * No unnecessary image object will be created if networks in the current
	 * session does not contain any nested network, i.e., should not have
	 * performance/memory issue.
	 *
	 * @return Image of this view.  It is always up-to-date.
	 */
	protected TexturePaint getSnapshot(final double width, final double height) {
		if(!latestSnapshot) {
			// Need to update snapshot.
			snapshotImage = (BufferedImage) createImage(DEF_SNAPSHOT_SIZE, DEF_SNAPSHOT_SIZE, true);
			latestSnapshot = true;
		}

		// Handle non-square images
		// Get the height and width of the image
		int imageWidth = snapshotImage.getWidth();
		int imageHeight = snapshotImage.getHeight();
		double ratio = (double)imageHeight / (double) imageWidth;
		int adjustedWidth = (int)((double)width/ratio)+1;

		final Rectangle2D rect = new Rectangle2D.Double(-adjustedWidth / 2, -height / 2, adjustedWidth, height);
		final TexturePaint texturePaint = new TexturePaint(snapshotImage, rect);
		return texturePaint;
	}

	public CyAnnotator getCyAnnotator() {
		return cyAnnotator;
	}
	
	public CyServiceRegistrar getServiceRegistrar() {
		return serviceRegistrar;
	}
	
	@Override
	public void handleDispose() {
		dispose();
	}
	
	@Override
	public void dispose() {
		synchronized(this) {
			if (inputHandler != null)
				inputHandler.dispose();
			
			checkDirtyTimer.stop();
			eventFireTimer.shutdown();
			cyAnnotator.dispose();
			serviceRegistrar.unregisterAllServices(cyAnnotator);
			renderComponent.dispose();
		}
	}

	@Override
	public String getRendererId() {
		return DingRenderer.ID;
	}
	
}