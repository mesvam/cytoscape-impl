package org.cytoscape.search.internal.ui;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.search.internal.index.SearchManager;
import org.cytoscape.search.internal.search.NetworkTableSearchTask;
import org.cytoscape.search.internal.search.SearchTask;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class NetworkSearchBox extends SearchBox {

	private static final Logger logger = LoggerFactory.getLogger(CyUserLog.NAME);
	
	private final CyServiceRegistrar registrar;
	private final SearchManager searchManager;
	
	public NetworkSearchBox(CyServiceRegistrar registrar, SearchManager searchManager) {
		super(registrar);
		this.registrar = registrar;
		this.searchManager = searchManager;
	}

	@Override
	public SearchTask getSearchTask(String queryString) {
		var appManager = registrar.getService(CyApplicationManager.class);
		var currentNetwork = appManager.getCurrentNetwork();
		if(currentNetwork == null) {
			logger.error("Could not find network for search");
			return null;
		}
		
		return new NetworkTableSearchTask(searchManager, queryString, currentNetwork);
	}
	
}
