package org.cytoscape.view.vizmap.gui.internal.model;

import static org.cytoscape.view.vizmap.gui.internal.util.NotificationNames.*;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JPanel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.NetworkViewRenderer;
import org.cytoscape.application.TableViewRenderer;
import org.cytoscape.application.events.CyStartEvent;
import org.cytoscape.application.events.CyStartListener;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.session.events.SessionAboutToBeLoadedEvent;
import org.cytoscape.session.events.SessionAboutToBeLoadedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.table.CyTableViewFactory;
import org.cytoscape.view.model.table.CyTableViewManager;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.RenderingEngineFactory;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.vizmap.TableVisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.events.SetCurrentVisualStyleEvent;
import org.cytoscape.view.vizmap.events.SetCurrentVisualStyleListener;
import org.cytoscape.view.vizmap.events.VisualStyleAboutToBeRemovedEvent;
import org.cytoscape.view.vizmap.events.VisualStyleAboutToBeRemovedListener;
import org.cytoscape.view.vizmap.events.VisualStyleAddedEvent;
import org.cytoscape.view.vizmap.events.VisualStyleAddedListener;
import org.cytoscape.view.vizmap.events.VisualStyleChangedEvent;
import org.cytoscape.view.vizmap.events.VisualStyleChangedListener;
import org.cytoscape.view.vizmap.events.table.ColumnAssociatedVisualStyleSetEvent;
import org.cytoscape.view.vizmap.events.table.ColumnAssociatedVisualStyleSetListener;
import org.cytoscape.view.vizmap.gui.internal.VisualStyleSet;
import org.cytoscape.view.vizmap.gui.internal.util.ServicesUtil;
import org.cytoscape.view.vizmap.gui.util.PropertySheetUtil;
import org.puremvc.java.multicore.patterns.proxy.Proxy;

/*
 * #%L
 * Cytoscape VizMap GUI Impl (vizmap-gui-impl)
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

@SuppressWarnings("unchecked")
public class VizMapperProxy extends Proxy
							implements VisualStyleAddedListener, VisualStyleAboutToBeRemovedListener,
							  		   VisualStyleChangedListener, SetCurrentVisualStyleListener,
							  		   SetCurrentNetworkListener, SetCurrentNetworkViewListener, 
							  		   ColumnAssociatedVisualStyleSetListener, SessionAboutToBeLoadedListener, 
							  		   SessionLoadedListener, CyStartListener {

	public static final String NAME = "VisualStyleProxy";
	public static final String PRESET_VIZMAP_FILE = "default_vizmap.xml";
	
	private final SortedSet<VisualStyle> visualStyles;
	private final ServicesUtil servicesUtil;
	
	private VisualStyle originalDefaultVisualStyle;
	private RenderingEngine<CyTable> dummyTableRenderingEngine;

	private volatile boolean cytoscapeStarted;
	private volatile boolean loadingSession;
	private volatile boolean ignoreStyleEvents;
	
	private final Object lock = new Object();

	// ==[ CONSTRUCTORS ]===============================================================================================
	
	public VizMapperProxy(final ServicesUtil servicesUtil) {
		// Create the data object--a SortedSet that will store all the Visual Styles
		super(NAME, new TreeSet<>(
				new Comparator<VisualStyle>() {
					
					@Override
					public int compare(final VisualStyle vs1, final VisualStyle vs2) {
						// Locale-specific sorting
						final Collator collator = Collator.getInstance(Locale.getDefault());
						collator.setStrength(Collator.PRIMARY);
						
						return collator.compare(vs1.getTitle(), vs2.getTitle());
					}
				}
		));

		this.visualStyles = (SortedSet<VisualStyle>) getData();
		this.servicesUtil = servicesUtil;
	}
	
	// ==[ PUBLIC METHODS ]=============================================================================================
	
	public SortedSet<VisualStyle> getVisualStyles() {
		synchronized (lock) {
			return new TreeSet<>(visualStyles);
		}
	}
	
	public void loadVisualStyles() {
		boolean changed = false;
		SortedSet<VisualStyle> updatedStyles = null;
		
		synchronized (lock) {
			// Load the styles
			var allStyles = getAllVisualStyles();
			
			if (! (allStyles.isEmpty() && visualStyles.isEmpty())) {
				// Save the original default style, because the values of the actual "default" style
				// can change every time a new session is loaded or when the user explicitly changes them
				var defStyle = servicesUtil.get(VisualMappingManager.class).getDefaultVisualStyle();
				originalDefaultVisualStyle = servicesUtil.get(VisualStyleFactory.class).createVisualStyle(defStyle);
				
				visualStyles.clear();
				visualStyles.addAll(allStyles);
				updatedStyles = getVisualStyles();
				changed = true;
			}
		}
		
		if (changed && cytoscapeStarted)
			sendNotification(VISUAL_STYLE_SET_CHANGED, updatedStyles);
	}

	public void addVisualStyle(final VisualStyle vs) {
		synchronized (lock) {
			if (vs != null)
				servicesUtil.get(VisualMappingManager.class).addVisualStyle(vs);
		}
	}
	
	public void removeVisualStyle(final VisualStyle vs) {
		synchronized (lock) {
			if (vs != null)
				servicesUtil.get(VisualMappingManager.class).removeVisualStyle(vs);
		}
	}
	
	public VisualStyle getOriginalDefaultVisualStyle() {
		return originalDefaultVisualStyle;
	}
	
	public boolean isDefaultStyle(VisualStyle vs) {
		return vs.equals(getDefaultVisualStyle());
	}
	
	public VisualStyle getDefaultVisualStyle() {
		synchronized (lock) {
			return servicesUtil.get(VisualMappingManager.class).getDefaultVisualStyle();
		}
	}
	
	
	public RenderingEngine<CyTable> getDummyTableRenderingEngine() {
		if(dummyTableRenderingEngine == null) {
			var tableViewRenderer = servicesUtil.get(CyApplicationManager.class).getDefaultTableViewRenderer();
			var factory = tableViewRenderer.getRenderingEngineFactory(TableViewRenderer.DEFAULT_CONTEXT);
			
			var tableFactory = servicesUtil.get(CyTableFactory.class);
			var table = tableFactory.createTable("dummy_table", "pk", Long.class, false, false);
			var tableView = servicesUtil.get(CyTableViewFactory.class).createTableView(table);
			
			dummyTableRenderingEngine = factory.createRenderingEngine(new JPanel(), tableView);
		}
		return dummyTableRenderingEngine;
	}
	
	
	// This just returns the network visual style
	public VisualStyle getCurrentNetworkVisualStyle() {
		synchronized (lock) {
			var vmm = servicesUtil.get(VisualMappingManager.class);
			return vmm.getCurrentVisualStyle();
		}
	}
	
	// This includes all the associated column visual styles
	public VisualStyleSet getCurrentVisualStyleSet() {
		synchronized (lock) {
			var vmm = servicesUtil.get(VisualMappingManager.class);
			
			var netStyle = vmm.getCurrentVisualStyle();
			if(netStyle == null)
				return null;
			
			var tvmm = servicesUtil.get(TableVisualMappingManager.class);
			
			var nodeColStyles = tvmm.getAssociatedColumnVisualStyles(netStyle, CyNode.class);
			var edgeColStyles = tvmm.getAssociatedColumnVisualStyles(netStyle, CyEdge.class);
			
			return new VisualStyleSet(netStyle, nodeColStyles, edgeColStyles);
		}
	}

	public void setCurrentNetworkVisualStyle(VisualStyle vs) {
		var curVs = getCurrentNetworkVisualStyle();
		var vmMgr = servicesUtil.get(VisualMappingManager.class);
		
		if (vs != null && !vs.equals(curVs))
			vmMgr.setCurrentVisualStyle(vs);
	}

	public VisualStyle getNetworkVisualStyle(CyNetworkView view) {
		return servicesUtil.get(VisualMappingManager.class).getVisualStyle(view);
	}
	
	public CyNetwork getCurrentNetwork() {
		return servicesUtil.get(CyApplicationManager.class).getCurrentNetwork();
	}
	
	public CyNetworkView getCurrentNetworkView() {
		return servicesUtil.get(CyApplicationManager.class).getCurrentNetworkView();
	}
	
	public RenderingEngine<CyNetwork> getCurrentNetworkRenderingEngine() {
		return servicesUtil.get(CyApplicationManager.class).getCurrentRenderingEngine();
	}
	
	/**
	 * @param tableType When the lexiconType is CyColumn.class, then we must specify which network table engine is required.
	 */
	public RenderingEngine<?> getRenderingEngine(Class<? extends CyIdentifiable> lexiconType) {
		if (lexiconType == CyNode.class || lexiconType == CyEdge.class || lexiconType == CyNetwork.class) {
			return getCurrentNetworkRenderingEngine();
		} else { // CyColumn.class
			var engineManager = servicesUtil.get(RenderingEngineManager.class);
			var tableViewManager = servicesUtil.get(CyTableViewManager.class);
			
			CyNetwork net = getCurrentNetwork();
			if(net == null)	
				return null;
			
			// Assume that the default node and edge tables have the same renderer.
			CyTable table = net.getDefaultNodeTable();
			
			var tableView = tableViewManager.getTableView(table);
			var engines = engineManager.getRenderingEngines(tableView);
			
			return engines.isEmpty() ? null : engines.iterator().next();
		}
	}
	
	public RenderingEngineFactory<CyNetwork> getCurrentNetworkRenderingEngineFactory() {
		var nvRenderer = servicesUtil.get(CyApplicationManager.class).getCurrentNetworkViewRenderer();
		if (nvRenderer != null)
			return nvRenderer.getRenderingEngineFactory(NetworkViewRenderer.DEFAULT_CONTEXT);
		return servicesUtil.get(RenderingEngineFactory.class);
	}
	
	public NetworkViewRenderer getNetworkViewRenderer(final CyNetworkView netView) {
		return getNetworkViewRenderer(netView.getRendererId());
	}
	
	public NetworkViewRenderer getNetworkViewRenderer(final String rendererId) {
		return servicesUtil.get(CyApplicationManager.class).getNetworkViewRenderer(rendererId);
	}
	
	public RenderingEngineFactory<CyNetwork> getNetworkRenderingEngineFactory(final CyNetworkView netView) {
		return getNetworkViewRenderer(netView).getRenderingEngineFactory(NetworkViewRenderer.DEFAULT_CONTEXT);
	}
	
	public VisualLexicon getCurrentLexicon(Class<? extends CyIdentifiable> lexiconType) {
		if(lexiconType == CyNode.class || lexiconType == CyEdge.class || lexiconType == CyNetwork.class) {
			return getCurrentNetworkVisualLexicon();
		} else {
			return getCurrentTableVisualLexicon();
		}
	}
	
	
	public VisualLexicon getCurrentNetworkVisualLexicon() {
		VisualLexicon lexicon = null;
		
		var factory = getCurrentNetworkRenderingEngineFactory();
		if (factory != null) {
			lexicon = factory.getVisualLexicon();
		}
		
		if (lexicon == null) {
			var engine = getCurrentNetworkRenderingEngine();
			if(engine != null) {
				lexicon = engine.getVisualLexicon();
			}
		}
		
		if (lexicon == null) {
			lexicon = servicesUtil.get(RenderingEngineManager.class).getDefaultVisualLexicon();
		}
		
		return lexicon;
	}
	
	
	public VisualLexicon getCurrentTableVisualLexicon() {
		var defaultTableLexicon = servicesUtil.get(RenderingEngineManager.class).getDefaultTableVisualLexicon();
		
		var currNet = servicesUtil.get(CyApplicationManager.class).getCurrentNetwork();
		if(currNet == null) {
			return defaultTableLexicon;
		}
		
		// We assume the node/edge tables have the same renderer.
		var nodeTable = currNet.getDefaultNodeTable();
		var tableView = servicesUtil.get(CyTableViewManager.class).getTableView(nodeTable);
		if(tableView == null) {
			return defaultTableLexicon;
		}
		
		var engines = servicesUtil.get(RenderingEngineManager.class).getRenderingEngines(tableView);
		if(engines == null || engines.isEmpty()) {
			return defaultTableLexicon;
		}
		
		return engines.iterator().next().getVisualLexicon();
	}
	
	
	public Set<View<CyNode>> getSelectedNodeViews(final CyNetworkView netView) {
		final Set<View<CyNode>> views = new HashSet<>();
		
		if (netView != null) {
			final List<CyNode> nodes = CyTableUtil.getNodesInState(netView.getModel(), CyNetwork.SELECTED, true);
			
			for (final CyNode n : nodes) {
				final View<CyNode> nv = netView.getNodeView(n);
				
				if (nv != null)
					views.add(nv);
			}
		}
		
		return views;
	}
	
	public Set<View<CyEdge>> getSelectedEdgeViews(final CyNetworkView netView) {
		final Set<View<CyEdge>> views = new HashSet<>();
		
		if (netView != null) {
			final List<CyEdge> edges = CyTableUtil.getEdgesInState(netView.getModel(), CyNetwork.SELECTED, true);
			
			for (final CyEdge e : edges) {
				final View<CyEdge> ev = netView.getEdgeView(e);
				
				if (ev != null)
					views.add(ev);
			}
		}
		
		return views;
	}
	
	public Set<CyNetworkView> getNetworkViewsWithStyle(VisualStyle style) {
		var views = new HashSet<CyNetworkView>();
		
		if (style != null) {
			var vmMgr = servicesUtil.get(VisualMappingManager.class);
			var allNetViews = servicesUtil.get(CyNetworkViewManager.class).getNetworkViewSet();
			
			for (var nv : allNetViews) {
				if (style.equals(vmMgr.getVisualStyle(nv)))
					views.add(nv);
			}
		}
		
		return views;
	}
	
	public int countNetworkViewsWithStyle(VisualStyle style) {
		int count = 0;
		
		if (style != null) {
			var vmMgr = servicesUtil.get(VisualMappingManager.class);
			var allNetViews = servicesUtil.get(CyNetworkViewManager.class).getNetworkViewSet();
			
			for (var nv : allNetViews) {
				if (style.equals(vmMgr.getVisualStyle(nv)))
					count++;
			}
		}
		
		return count;
	}
	
	public static boolean isSupported(VisualLexicon lexicon, VisualProperty<?> vp) {
		return PropertySheetUtil.isCompatible(vp) && lexicon.isSupported(vp);
	}
	
	public static boolean isSupported(VisualLexicon lexicon, VisualPropertyDependency<?> dependency) {
		if (!isSupported(lexicon, dependency.getParentVisualProperty()))
			return false;
		
		for (final VisualProperty<?> vp : dependency.getVisualProperties()) {
			if (!isSupported(lexicon, vp))
				return false;
		}
		
		return true;
	}
	
	public void setIgnoreStyleEvents(final boolean b) {
		synchronized (lock) {
			ignoreStyleEvents = b;
		}
	}
	
	// --- Cytoscape EVENTS ---
	
	@Override
	public void handleEvent(final VisualStyleAddedEvent e) {
		synchronized (lock) {
			if (!cytoscapeStarted || ignoreStyleEvents)
				return;
		}
		
		final VisualStyle vs = e.getVisualStyleAdded();
		boolean changed = false;
		
		synchronized (lock) {
			changed = visualStyles.add(vs);
		}
		
		if (changed && !loadingSession)
			sendNotification(VISUAL_STYLE_ADDED, vs);
	}
	
	@Override
	public void handleEvent(final VisualStyleAboutToBeRemovedEvent e) {
		synchronized (lock) {
			if (!cytoscapeStarted || ignoreStyleEvents)
				return;
		}
		
		final VisualStyle vs = e.getVisualStyleToBeRemoved();
		boolean changed = false;
		
		synchronized (lock) {
			changed = visualStyles.remove(vs);
		}
		
		if (changed && !loadingSession)
			sendNotification(VISUAL_STYLE_REMOVED, vs);
	}
	
	@Override
	public void handleEvent(final VisualStyleChangedEvent e) {
		synchronized (lock) {
			if (ignoreStyleEvents)
				return;
		}
		
		if (cytoscapeStarted && !loadingSession)
			sendNotification(VISUAL_STYLE_UPDATED, e.getSource());
	}
	
	@Override
	public void handleEvent(final SetCurrentVisualStyleEvent e) {
		if (cytoscapeStarted && !loadingSession)
			sendNotification(CURRENT_VISUAL_STYLE_CHANGED, e.getVisualStyle());
	}
	
	@Override
	public void handleEvent(ColumnAssociatedVisualStyleSetEvent e) {
		if (cytoscapeStarted && !loadingSession) {
			sendNotification(TABLE_ASSOCIATED_VISUAL_STYLE_UPDATED, e.getAssociation());
		}
	}
	
	@Override
	public void handleEvent(final SetCurrentNetworkEvent e) {
		if (cytoscapeStarted && !loadingSession)
			sendNotification(CURRENT_NETWORK_CHANGED, e.getNetwork());
	}
	
	@Override
	public void handleEvent(final SetCurrentNetworkViewEvent e) {
		if (cytoscapeStarted && !loadingSession)
			sendNotification(CURRENT_NETWORK_VIEW_CHANGED, e.getNetworkView());
	}
	
	@Override
	public void handleEvent(final CyStartEvent e) {
		cytoscapeStarted = true;
		
		// Don't load the default styles if a session has already been loaded,
		// because that would add the default style list to the current session (which could even duplicate some styles)
		// and change the style of the current network view.
		// This can happen when Cytoscape is started with a command-line argument to open a session.
		if (servicesUtil.get(CySessionManager.class).getCurrentSessionFileName() == null)
			sendNotification(LOAD_DEFAULT_VISUAL_STYLES);
		else
			loadVisualStyles();
	}
	
	@Override
	public void handleEvent(final SessionAboutToBeLoadedEvent e) {
		loadingSession = true;
	}
	
	@Override
	public void handleEvent(final SessionLoadedEvent e) {
		loadingSession = false;
		
		if (e.getLoadedFileName() == null) // New empty session
			sendNotification(LOAD_DEFAULT_VISUAL_STYLES);
		else
			sendNotification(VISUAL_STYLE_SET_CHANGED, getVisualStyles());
	}
	
	// ==[ PRIVATE METHODS ]============================================================================================
	
	private Set<VisualStyle> getAllVisualStyles() {
		return servicesUtil.get(VisualMappingManager.class).getAllVisualStyles();
	}
}
