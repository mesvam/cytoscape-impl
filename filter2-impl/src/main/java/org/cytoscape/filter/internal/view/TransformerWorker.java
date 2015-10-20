package org.cytoscape.filter.internal.view;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.filter.TransformerManager;
import org.cytoscape.filter.internal.LifecycleTransformer;
import org.cytoscape.filter.model.CompositeFilter;
import org.cytoscape.filter.model.Transformer;
import org.cytoscape.filter.model.TransformerSink;
import org.cytoscape.filter.model.TransformerSource;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;

public class TransformerWorker extends AbstractWorker<TransformerPanel, TransformerPanelController> {
	private TransformerManager transformerManager;
	
	private FilterSource filterSource;
	private Sink sink;
	
	public TransformerWorker(LazyWorkQueue queue, CyApplicationManager applicationManager, TransformerManager transformerManager) {
		super(queue, applicationManager);
		this.transformerManager = transformerManager;
		
		filterSource = new FilterSource();
		sink = new Sink();
	}
	
	@Override
	public void doWork() {
		if (controller == null) {
			return;
		}
		
		CyNetworkView networkView = applicationManager.getCurrentNetworkView();
		CyNetwork network;
		if (networkView != null) {
			network = networkView.getModel();
		} else {
			network = applicationManager.getCurrentNetwork();
		}
		
		if (network == null) {
			return;
		}
		
		sink.resetCounts();
		TaskMonitor monitor = controller.getTaskMonitor(view);
		
		monitor.setProgress(-1.0); // indeterminate
		monitor.setStatusMessage(null);
		long startTime = System.currentTimeMillis();
		try {
			List<Transformer<CyNetwork, CyIdentifiable>> transformers = controller.getTransformers(view);
			FilterElement selected = (FilterElement) controller.getStartWithComboBoxModel().getSelectedItem();
			TransformerSource<CyNetwork, CyIdentifiable> source = createSource(network, selected);
			sink.network = network;
			
			for(Transformer<?,?> transformer : transformers) {
				if(transformer instanceof LifecycleTransformer) {
					((LifecycleTransformer) transformer).setUp();
				}
			}
			try {
				transformerManager.execute(network, source, transformers, sink);
			}
			finally {
				for(Transformer<?,?> transformer : transformers) {
					if(transformer instanceof LifecycleTransformer) {
						((LifecycleTransformer) transformer).tearDown();
					}
				}
			}
			
			if (networkView != null) {
				networkView.updateView();
			}
		} finally {
			long duration = System.currentTimeMillis() - startTime;
			monitor.setProgress(1.0);
			monitor.setStatusMessage(String.format("Selected %d %s and %d %s in %dms",
					sink.nodeCount,
					sink.nodeCount == 1 ? "node" : "nodes",
					sink.edgeCount,
					sink.edgeCount == 1 ? "edge" : "edges",
					duration));

			isCancelled = false;
		}
	}

	private TransformerSource<CyNetwork, CyIdentifiable> createSource(CyNetwork network, FilterElement selected) {
		if (selected.getFilter() == null) {
			return SelectionSource.instance;
		} else {
			filterSource.filter = selected.getFilter();
			return filterSource;
		}
	}
	
	static abstract class AbstractSource implements TransformerSource<CyNetwork, CyIdentifiable> {
		@Override
		public Class<CyNetwork> getContextType() {
			return CyNetwork.class;
		}
		
		@Override
		public Class<CyIdentifiable> getElementType() {
			return CyIdentifiable.class;
		}

		@Override
		public int getElementCount(CyNetwork context) {
			// Just a hint
			return context.getNodeCount() + context.getEdgeCount();
		}
	}
	
	static class SelectionSource extends AbstractSource {
		public static TransformerSource<CyNetwork, CyIdentifiable> instance = new SelectionSource();

		@Override
		public List<CyIdentifiable> getElementList(CyNetwork context) {
			int maximum = getElementCount(context);
			ArrayList<CyIdentifiable> elements = new ArrayList<CyIdentifiable>(maximum);

			// Clear selection state while collecting elements
			for (CyNode node : context.getNodeList()) {
				CyRow row = context.getRow(node);
				if (row.get(CyNetwork.SELECTED, Boolean.class)) {
					row.set(CyNetwork.SELECTED, false);
					elements.add(node);
				}
			}
			for (CyEdge edge : context.getEdgeList()) {
				CyRow row = context.getRow(edge);
				if (row.get(CyNetwork.SELECTED, Boolean.class)) {
					row.set(CyNetwork.SELECTED, false);
					elements.add(edge);
				}
			}
			return elements;
		}
	}
	
	static class FilterSource extends AbstractSource {
		CompositeFilter<CyNetwork, CyIdentifiable> filter;

		@Override
		public List<CyIdentifiable> getElementList(CyNetwork context) {
			int maximum = getElementCount(context);
			ArrayList<CyIdentifiable> elements = new ArrayList<CyIdentifiable>(maximum);
			
			if(filter instanceof LifecycleTransformer) {
				((LifecycleTransformer) filter).setUp();
			}
			try {
				// Clear selection state while collecting elements
				for (CyNode node : context.getNodeList()) {
					CyRow row = context.getRow(node);
					if (row.get(CyNetwork.SELECTED, Boolean.class)) {
						row.set(CyNetwork.SELECTED, false);
					}
					if (filter.accepts(context, node)) {
						elements.add(node);
					}
				}
				for (CyEdge edge : context.getEdgeList()) {
					CyRow row = context.getRow(edge);
					if (row.get(CyNetwork.SELECTED, Boolean.class)) {
						row.set(CyNetwork.SELECTED, false);
					}
					if (filter.accepts(context, edge)) {
						elements.add(edge);
					}
				}
			}
			finally {
				if(filter instanceof LifecycleTransformer) {
					((LifecycleTransformer) filter).tearDown();
				}
			}
			return elements;
		}
	}
	
	static class Sink implements TransformerSink<CyIdentifiable> {
		CyNetwork network;
		int nodeCount;
		int edgeCount;
		
		@Override
		public void collect(CyIdentifiable element) {
			if (element instanceof CyNode) {
				nodeCount++;
			} else if (element instanceof CyEdge) {
				edgeCount++;
			}
			
			network.getRow(element).set(CyNetwork.SELECTED, true);
		}
		
		void resetCounts() {
			nodeCount = 0;
			edgeCount = 0;
		}
	}
}
