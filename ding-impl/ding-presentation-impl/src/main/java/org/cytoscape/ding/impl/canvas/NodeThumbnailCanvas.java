package org.cytoscape.ding.impl.canvas;

import org.cytoscape.ding.impl.DEdgeDetails;
import org.cytoscape.ding.impl.DNodeDetails;
import org.cytoscape.ding.impl.work.ProgressMonitor;
import org.cytoscape.graph.render.immed.GraphGraphics;
import org.cytoscape.graph.render.stateful.GraphRenderer;
import org.cytoscape.graph.render.stateful.RenderDetailFlags;
import org.cytoscape.view.model.CyNetworkViewSnapshot;

public class NodeThumbnailCanvas<GP extends GraphicsProvider> extends DingCanvas<GP> {

	private final CyNetworkViewSnapshot snapshot;
	
	public NodeThumbnailCanvas(GP graphics, CyNetworkViewSnapshot snapshot) {
		super(graphics);
		this.snapshot = snapshot;
	}

	@Override
	public String getCanvasDebugName() {
		return "Node";
	}
	
	@Override
	public void paint(ProgressMonitor pm, RenderDetailFlags flags) {
		var graphics = new GraphGraphics(graphicsProvider);
		
		var edgeDetails = new DEdgeDetails(null);
		var nodeDetails = new DNodeDetails(null, null);
		
		GraphRenderer.renderNodes(pm, graphics, snapshot, flags, nodeDetails, edgeDetails, null);
	}
	
}
