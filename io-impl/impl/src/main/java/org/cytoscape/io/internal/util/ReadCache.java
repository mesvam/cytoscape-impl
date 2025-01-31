package org.cytoscape.io.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.cytoscape.io.internal.read.xgmml.handler.XGMMLParseUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
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

public class ReadCache {
	
	/* Map of new to old element IDs */
	private Map<Long, Object> oldIdMap;
	
	/* Maps of XML ID's to elements (the keys should be a Long if reading a Cy3 session file) */
	private Map<Object, CyNetwork> networkByIdMap;
	private Map<Object, CyNetworkView> networkViewByIdMap;
	private Map<Object, CyNode> nodeByIdMap;
	private Map<Object, CyEdge> edgeByIdMap;
	
	/* Maps of node labels to nodes (necessary because of 2.x sessions, which uses the node label as its session ID) */
	private Map<Object, CyNode> nodeByNameMap;
	
	private Map<CyNetwork, Set<Long>> nodeLinkMap;
	private Map<CyNetwork, Set<Long>> edgeLinkMap;
	private Map<CySubNetwork, Set<CyNode>> unresolvedNodeMap;
	private Map<CyNode, Object/*network's id*/> networkPointerMap;
	
	private boolean readingSessionFile;
	
	private final CyServiceRegistrar serviceRegistrar;
	
	private final Object lock = new Object();
	
	private static final Logger logger = LoggerFactory.getLogger("org.cytoscape.application.userlog");
	
	public ReadCache(final CyServiceRegistrar serviceRegistrar) {
		this.serviceRegistrar = serviceRegistrar;
	}

	public void init() {
		synchronized (lock) {
			oldIdMap = new HashMap<>();
			nodeByIdMap = new HashMap<>();
			edgeByIdMap = new HashMap<>();
			networkByIdMap = new HashMap<>();
			networkViewByIdMap = new HashMap<>();
			nodeByNameMap = new HashMap<>();
			nodeLinkMap = new WeakHashMap<>();
			edgeLinkMap = new WeakHashMap<>();
			unresolvedNodeMap = new WeakHashMap<>();
			networkPointerMap = new WeakHashMap<>();
		}
	}
	
	public void dispose() {
		synchronized (lock) {
			nodeByIdMap = null;
			edgeByIdMap = null;
			networkByIdMap = null;
			networkViewByIdMap = null;
			nodeByNameMap = null;
			nodeLinkMap = null;
			edgeLinkMap = null;
			unresolvedNodeMap = null;
			networkPointerMap = null;
		}
	}
	
	/**
	 * Cache the element for future reference.
	 * @param xgmmlId The XGMML id of the element.
	 * @param element A CyNetwork, CyNetworkView, CyNode or CyEdge.
	 */
	public void cache(Object xgmmlId, CyIdentifiable element) {
		if (xgmmlId != null) {
			if (element instanceof CyNode) {
				nodeByIdMap.put(xgmmlId, (CyNode) element);
			} else if (element instanceof CyEdge) {
				edgeByIdMap.put(xgmmlId, (CyEdge) element);
			} else if (element instanceof CyNetwork) {
				networkByIdMap.put(xgmmlId, (CyNetwork) element);
			} else if (element instanceof CyNetworkView) {
				networkViewByIdMap.put(xgmmlId, (CyNetworkView) element);
			}

			oldIdMap.put(element.getSUID(), xgmmlId);
		}
    }
	
	/**
	 * Probably only necessary when parsing 2.x session files.
	 * @param name
	 * @param node
	 */
	public void cacheNodeByName(String name, CyNode node) {
		if (name != null && !name.isEmpty() && node != null)
			nodeByNameMap.put(name,  node);
	}
	
	public void addNetworkPointer(CyNode node, Object networkId) {
		if (node == null)
			throw new NullPointerException("Cannot parse network pointer: node is null.");
		if (networkId == null)
			throw new NullPointerException("Cannot parse network pointer: network id is null.");
		
		synchronized (lock) {
			networkPointerMap.put(node, networkId);
		}
	}
	
	public Object getNetworkPointerId(CyNode node) {
		synchronized (lock) {
			return networkPointerMap.get(node);
		}
	}
	
	public boolean hasNetworkPointers() {
		synchronized (lock) {
			return !networkPointerMap.isEmpty();
		}
	}
	
	public Object getOldId(Long suid) {
		synchronized (lock) {
			return oldIdMap.get(suid);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends CyIdentifiable> T getObjectById(Object oldId, Class<T> type) {
		synchronized (lock) {
			if (type == CyNetwork.class)
				return (T) networkByIdMap.get(oldId);
			if (type == CyNetworkView.class)
				return (T) networkViewByIdMap.get(oldId);
			if (type == CyNode.class)
				return (T) nodeByIdMap.get(oldId);
			if (type == CyEdge.class)
				return (T) edgeByIdMap.get(oldId);
		}
		
		return null;
	}
	
	public CyNetwork getNetwork(Object oldId) {
		synchronized (lock) {
			return networkByIdMap.get(oldId);
		}
	}
	
	public Collection<CyNetwork> getNetworks() {
		synchronized (lock) {
			return new ArrayList<>(networkByIdMap.values());
		}
	}
	
	public CyNetworkView getNetworkView(Object oldId) {
		synchronized (lock) {
			return networkViewByIdMap.get(oldId);
		}
	}
	
	public CyNode getNode(Object oldId) {
		synchronized (lock) {
			return nodeByIdMap.get(oldId);
		}
	}
	
	public CyEdge getEdge(Object oldId) {
		synchronized (lock) {
			return edgeByIdMap.get(oldId);
		}
	}
	
	public CyNode getNodeByName(String nodeName) {
		synchronized (lock) {
			return nodeByNameMap.get(nodeName);
		}
	}
	
	public void addUnresolvedNode(CyNode node, CySubNetwork net) {
		synchronized (lock) {
			var nodes = unresolvedNodeMap.get(net);
			
			if (nodes == null) {
				nodes = new HashSet<>();
				unresolvedNodeMap.put(net, nodes);
			}
			
			nodes.add(node);
		}
	}
	
	public boolean removeUnresolvedNode(CyNode node, CySubNetwork net) {
		synchronized (lock) {
			var nodes = unresolvedNodeMap.get(net);
			
			return nodes != null ? nodes.remove(node) : false;
		}
	}
	
	public void deleteUnresolvedNodes() {
		// Delete unresolved nodes from
		synchronized (lock) {
			for (var entry : unresolvedNodeMap.entrySet()) {
				var net = entry.getKey();
				var nodes = entry.getValue();
				
				if (net != null && nodes != null && !nodes.isEmpty()) {
					logger.error("The following nodes can't be resolved and will be deleted from network \"" + net + "\": " 
							+ nodes);
					net.removeNodes(nodes);
				}
			}
		}
	}

	public void addElementLink(String href, Class<? extends CyIdentifiable> clazz, CyNetwork net) {
		Map<CyNetwork, Set<Long>> map = null;
		var id = XGMMLParseUtil.getIdFromXLink(href);
		
		synchronized (lock) {
			if (clazz == CyNode.class)
				map = nodeLinkMap;
			else if (clazz == CyEdge.class)
				map = edgeLinkMap;
			
			if (map != null && net != null) {
				var idSet = map.get(net);
				
				if (idSet == null) {
					idSet = new HashSet<Long>();
					map.put(net, idSet);
				}
				
				idSet.add(id);
			}
		}
	}
	
	public Map<CyNetwork, Set<Long>> getNodeLinks() {
		return nodeLinkMap;
	}

	public Map<CyNetwork, Set<Long>> getEdgeLinks() {
		return edgeLinkMap;
	}
	
	public Map<Object, CyNetwork> getNetworkByIdMap() {
		return networkByIdMap;
	}
	
	public Map<Object, CyNetworkView> getNetworkViewByIdMap() {
		return networkViewByIdMap;
	}

	public Map<Object, CyNode> getNodeByIdMap() {
		return nodeByIdMap;
	}

	public Map<Object, CyEdge> getEdgeByIdMap() {
		return edgeByIdMap;
	}

	public Map<Object, CyNode> getNodeByNameMap() {
		return nodeByNameMap;
	}
	
	/**
	 * @return All network tables, except DEFAULT_ATTRS and SHARED_DEFAULT_ATTRS ones.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Set<CyTable> getNetworkTables() {
		final Set<CyTable> tables = new HashSet<>();
		final Set<CyNetwork> networks = new HashSet<>();
		final Class<?>[] types = new Class[] { CyNetwork.class, CyNode.class, CyEdge.class };
		
		synchronized (lock) {
			if (networkByIdMap.values() != null)
				networks.addAll(networkByIdMap.values());
		}
		
		var netTblMgr = serviceRegistrar.getService(CyNetworkTableManager.class);
		
		for (var n : networks) {
			for (Class t : types) {
				var tblMap = new HashMap<>(netTblMgr.getTables(n, t));
				tblMap.remove(CyNetwork.DEFAULT_ATTRS);
				
				if (tblMap != null)
					tables.addAll(tblMap.values());
				
				if (n instanceof CySubNetwork) {
					// Don't forget the root-network tables.
					tblMap = new HashMap<String, CyTable>(netTblMgr.getTables(((CySubNetwork) n).getRootNetwork(), t));
					tblMap.remove(CyRootNetwork.DEFAULT_ATTRS);
					tblMap.remove(CyRootNetwork.SHARED_DEFAULT_ATTRS);
					
					if (tblMap != null)
						tables.addAll(tblMap.values());
				}
			}
		}
			
		return tables;
	}
	
	public void createNetworkPointers() {
		synchronized (lock) {
			if (networkPointerMap == null)
				return;
		
			for (var entry : networkPointerMap.entrySet()) {
				var node = entry.getKey();
				var oldNetId = entry.getValue();
				var network = getNetwork(oldNetId);
				
				if (network != null) {
					node.setNetworkPointer(network);
				} else {
					logger.error("Cannot recreate network pointer: Cannot find network " + oldNetId);
				}
			}
		}
	}
	
	public boolean isReadingSessionFile() {
		synchronized (lock) {
			return readingSessionFile;
		}
	}

	public void setReadingSessionFile(boolean readingSessionFile) {
		synchronized (lock) {
			this.readingSessionFile = readingSessionFile;
		}
	}
}
