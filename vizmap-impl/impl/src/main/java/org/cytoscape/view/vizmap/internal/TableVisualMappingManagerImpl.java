package org.cytoscape.view.vizmap.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.TableViewRenderer;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.events.TableViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.TableViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.table.CyTableView;
import org.cytoscape.view.vizmap.StyleAssociation;
import org.cytoscape.view.vizmap.TableVisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.events.table.ColumnAssociatedVisualStyleSetEvent;
import org.cytoscape.view.vizmap.events.table.ColumnVisualStyleSetEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TableVisualMappingManagerImpl implements TableVisualMappingManager, TableViewAboutToBeDestroyedListener {
	
	private static final Logger logger = LoggerFactory.getLogger(CyUserLog.NAME);
	
	public static final String DEFAULT_STYLE_NAME = "default";
	
	private final CyServiceRegistrar registrar;
	private final Object lock = new Object();
	
	// Styles for unassigned tables
	private final Map<View<CyColumn>, VisualStyle> column2VisualStyleMap = new WeakHashMap<>();
	
	// Styles associated with default network tables
	private final Map<VisualStyle,Map<String,VisualStyle>> associatedNodeStyles = new WeakHashMap<>();
	private final Map<VisualStyle,Map<String,VisualStyle>> associatedEdgeStyles = new WeakHashMap<>();

	private VisualStyle defaultStyle;
	
	
	public TableVisualMappingManagerImpl(VisualStyleFactory factory, CyServiceRegistrar serviceRegistrar) {
		this.registrar = Objects.requireNonNull(serviceRegistrar, "'serviceRegistrar' cannot be null");
		this.defaultStyle = factory.createVisualStyle(DEFAULT_STYLE_NAME);
	}

	@Override
	public VisualStyle getDefaultVisualStyle() {
		return defaultStyle;
	}
	
	@Override
	public void handleEvent(TableViewAboutToBeDestroyedEvent e) {
		CyTableView tableView = e.getTableView();
		for(View<CyColumn> colView : tableView.getColumnViews()) {
			clearStyle(colView);
		}
	}
	
	
	// Internal record, just used to return two things from a method.
	private record NetworkStyleAndTableType(VisualStyle networkStyle, Class<? extends CyIdentifiable> tableType) {
		final static NetworkStyleAndTableType NO_VIEWS = new NetworkStyleAndTableType(null, null);
		final static NetworkStyleAndTableType TOO_MANY_VIEWS = new NetworkStyleAndTableType(null, null);
		boolean isValid() { return this != NO_VIEWS && this != TOO_MANY_VIEWS; }
	}
	
	
	/**
	 * If then given colView is from a network's default node/edge/network table, then return the visual style
	 * currently applied to that network, and the type of the table.
	 * Note: Throws an exception if the network has no network views or more than one network view with different
	 * styles applied.
	 */
	private NetworkStyleAndTableType getNetworkStyleAndTableType(View<CyColumn> colView) {
		// If the column is from a default node/edge/network table, then make it part of the associated style
		var visualMappingManager = registrar.getService(VisualMappingManager.class);
		var networkViewManager   = registrar.getService(CyNetworkViewManager.class);
		var networkTableManager  = registrar.getService(CyNetworkTableManager.class);
				
		var table = colView.getModel().getTable();
		
		var namespace = networkTableManager.getTableNamespace(table);
		var network = networkTableManager.getNetworkForTable(table);
		var tableType = networkTableManager.getTableType(table);
		
		if(network != null && namespace == CyNetwork.DEFAULT_ATTRS && (tableType == CyNode.class || tableType == CyEdge.class)) {
			var netViews = networkViewManager.getNetworkViews(network);
			
			if(tableType != null && netViews != null) {
				if(netViews.isEmpty()) {
					return NetworkStyleAndTableType.NO_VIEWS;
				} 
				
				var netViewCount = netViews.size();
				var iter = netViews.iterator();
				var firstStyle = visualMappingManager.getVisualStyle(iter.next());
				
				if(netViewCount > 1) {
					while(iter.hasNext()) {
						var nextStyle = visualMappingManager.getVisualStyle(iter.next());
						if(!firstStyle.equals(nextStyle)) {
							return NetworkStyleAndTableType.TOO_MANY_VIEWS;
						}
					}
				}
				
				return new NetworkStyleAndTableType(firstStyle, tableType);
			}
		}
		
		return null;
	}
	
	
	/**
	 * Returns an associated Visual Style for the View Model.
	 */
	@Override
	public VisualStyle getVisualStyle(View<CyColumn> colView) {
		if (colView == null) {
			logger.warn("Attempting to get the visual style for a null column view.");
			return null;
		}

		var nsatt = getNetworkStyleAndTableType(colView);
		if(nsatt != null) {
			if(nsatt.isValid()) {
				var networkStyle = nsatt.networkStyle();
				var tableType = nsatt.tableType();
				var colName = colView.getModel().getName();
				return getAssociatedColumnVisualStyle(networkStyle, tableType, colName);
			} else {
				return null;
			}
		}
		
		synchronized (lock) {
			return column2VisualStyleMap.get(colView);
		}
	}
	
	@Override
	public void setVisualStyle(View<CyColumn> colView, VisualStyle columnStyle) {
		Objects.requireNonNull(colView, "Column view is null.");

		// If the column is from a default node/edge/network table, then make it part of the associated style.
		var nsatt = getNetworkStyleAndTableType(colView);
		if(nsatt != null) {
			if(nsatt == NetworkStyleAndTableType.NO_VIEWS && columnStyle == null) {
				return;
			} else if(nsatt == NetworkStyleAndTableType.NO_VIEWS) {
				throw new IllegalStateException("The given colView is for a default network table for a network that has no network views.");
			} else if(nsatt == NetworkStyleAndTableType.TOO_MANY_VIEWS && columnStyle == null) {
				return;
			} else if(nsatt == NetworkStyleAndTableType.TOO_MANY_VIEWS) {
				throw new IllegalStateException("The given colView is for a default network table for a network that has multiple network views "
						+ "with different styles. Please use setAssociatedVisualStyle() to specify the networkStyle to association with the column style.");
			}
			
			var networkStyle = nsatt.networkStyle();
			var tableType = nsatt.tableType();
			var colName = colView.getModel().getName();
			setAssociatedVisualStyle(networkStyle, tableType, colName, columnStyle);
			return;
		}
		
		// Otherwise create a direct mapping.
		boolean changed = false;
		synchronized (lock) {
			if (columnStyle == null) {
				changed = column2VisualStyleMap.remove(colView) != null;
			} else {
				VisualStyle previousStyle = column2VisualStyleMap.put(colView, columnStyle);
				changed = !columnStyle.equals(previousStyle);
			}
		}
		
		if (changed) {
			var eventHelper = registrar.getService(CyEventHelper.class);
			eventHelper.fireEvent(new ColumnVisualStyleSetEvent(this, columnStyle, colView));
		}
	}
	
	
	private void clearStyle(View<CyColumn> colView) {
		boolean changed = false;

		synchronized (lock) {
			changed = column2VisualStyleMap.remove(colView) != null;
		}

		if (changed) {
			var eventHelper = registrar.getService(CyEventHelper.class);
			eventHelper.fireEvent(new ColumnVisualStyleSetEvent(this, null, colView));
		}
	}

	
	@Override
	public Set<VisualStyle> getAllVisualStyles() {
		synchronized (lock) {
			return new HashSet<>(column2VisualStyleMap.values());
		}
	}
	
	@Override
	public Map<View<CyColumn>, VisualStyle> getAllVisualStylesMap() {
		synchronized (lock) {
			return new HashMap<>(column2VisualStyleMap);
		}
	}

	@Override
	public Set<VisualLexicon> getAllVisualLexicon() {
		Set<VisualLexicon> set = new LinkedHashSet<>();
		CyApplicationManager appManager = registrar.getService(CyApplicationManager.class);
		
		for(var renderer : appManager.getTableViewRendererSet()) {
			var factory = renderer.getRenderingEngineFactory(TableViewRenderer.DEFAULT_CONTEXT);
			if(factory != null) {
				var lexicon = factory.getVisualLexicon();
				if(lexicon != null) {
					set.add(lexicon);
				}
			}
		}
		
		return set;
	}
	
	
	private Map<VisualStyle,Map<String,VisualStyle>> getAssociatedStyleMap(Class<? extends CyIdentifiable> tableType) {
		if(tableType == CyNode.class)
			return associatedNodeStyles;
		else if(tableType == CyEdge.class)
			return associatedEdgeStyles;
		else
			throw new IllegalArgumentException("tableType is not a supported type, got: " + tableType);
	}
	
	
	public void setAssociatedVisualStyle(VisualStyle networkVisualStyle, Class<? extends CyIdentifiable> tableType, String colName, VisualStyle columnVisualStyle) {
		Objects.requireNonNull(networkVisualStyle, "networkVisualStyle is null");
		Objects.requireNonNull(tableType, "tableType is null");
		Objects.requireNonNull(colName, "colName is null");
		
		var styleMap = getAssociatedStyleMap(tableType);
		boolean changed = false;

		synchronized (lock) {
			var networkAssociatedStyles = styleMap.computeIfAbsent(networkVisualStyle, k -> new HashMap<>());
			
			if (columnVisualStyle == null) {
				changed = networkAssociatedStyles.remove(colName) != null;
			} else {
				var previousStyle = networkAssociatedStyles.put(colName, columnVisualStyle);
				changed = !columnVisualStyle.equals(previousStyle);
			}
		}
		
		if (changed) {
			var eventHelper = registrar.getService(CyEventHelper.class);
			var association = new StyleAssociation(networkVisualStyle, tableType, colName, columnVisualStyle);
			eventHelper.fireEvent(new ColumnAssociatedVisualStyleSetEvent(this, association));
		}
	}
	
	
	public Map<String,VisualStyle> getAssociatedColumnVisualStyles(VisualStyle networkVisualStyle, Class<? extends CyIdentifiable> tableType) {
		var styleMap = getAssociatedStyleMap(tableType);
		Map<String, VisualStyle> associatedStyles = styleMap.get(networkVisualStyle);
		if(associatedStyles == null)
			return Collections.emptyMap();
		return Collections.unmodifiableMap(associatedStyles);
	}
	

	// MKTODO Maybe use a reverse map to store this instead of searching through all the values.
	@Override
	public Set<VisualStyle> getAssociatedNetworkVisualStyles(VisualStyle columnVisualStyle) {
		Set<VisualStyle> networkStyles = new HashSet<>();
		
		synchronized (lock) {
			for(var styleMap : List.of(associatedNodeStyles, associatedEdgeStyles)) {
				for(var entry : styleMap.entrySet()) {
					var netStyle = entry.getKey();
					var colStyles = entry.getValue();
					if(colStyles.values().contains(columnVisualStyle)) {
						networkStyles.add(netStyle);
					}
				}
			}
		}
		
		return networkStyles;
	}
	
	@Override
	public Set<StyleAssociation> getAssociations(VisualStyle columnVisualStyle) {
		Objects.requireNonNull(columnVisualStyle);
		return getStyleAssociations(columnVisualStyle);
	}

	
	@Override
	public Set<StyleAssociation> getAllStyleAssociations() {
		return getStyleAssociations(null);
	}
	
	
	private Set<StyleAssociation> getStyleAssociations(VisualStyle columnVisualStyle) {
		Set<StyleAssociation> associations = new HashSet<>();
		
		synchronized (lock) {
			for(var tableType : List.of(CyNode.class, CyEdge.class)) {
				var networkMap = getAssociatedStyleMap(tableType);
				for(var netEntry : networkMap.entrySet()) {
					var netStyle = netEntry.getKey();
					var colStyles = netEntry.getValue();
					for(var colEntry : colStyles.entrySet()) {
						var colName = colEntry.getKey();
						var colStyle = colEntry.getValue();
						if(columnVisualStyle == null || colStyle.equals(columnVisualStyle)) {
							associations.add(new StyleAssociation(netStyle, tableType, colName, colStyle));
						}
					}
				}
			}
		}
		
		return associations;
	}


}
