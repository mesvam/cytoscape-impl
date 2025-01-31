package org.cytoscape.task.internal.networkobjects;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.internal.utils.CoreImplDocumentationConstants;
import org.cytoscape.util.json.CyJSONUtil;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
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

public class GetNodeTask extends AbstractGetTask implements ObservableTask {
	
	@Tunable(description="Network to get node from", context="nogui", longDescription=StringToModel.CY_NETWORK_LONG_DESCRIPTION, exampleStringValue=StringToModel.CY_NETWORK_EXAMPLE_STRING)
	public CyNetwork network;

	@Tunable(description="Node to get", context="nogui", longDescription=CoreImplDocumentationConstants.NODE_LONG_DESCRIPTION, exampleStringValue="Node 1")
	public String node;

	private CyNode returnedNode;
	private final CyServiceRegistrar serviceRegistrar;
	
	protected GetNodeTask(CyServiceRegistrar serviceRegistrar) {
		this.serviceRegistrar = serviceRegistrar;
	}

	@Override
	public void run(final TaskMonitor tm) {
		if (network == null) {
			network = serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetwork();
			if (network == null) {
				tm.showMessage(TaskMonitor.Level.ERROR, "Network must be specified");
				throw new RuntimeException("Network must be specified");
			}
		}

		if (node == null) {
			tm.showMessage(TaskMonitor.Level.ERROR, "Node name or suid must be specified");
			throw new RuntimeException("Node name or suid must be specified");
		}

		returnedNode = getNode(network, node);
	}

	public Object getResults(Class type) {
		if (type.equals(CyNode.class)) {
			return returnedNode;
		} else if (type.equals(String.class)){
			if (returnedNode == null)
				return "<none>";
			return returnedNode.toString();
		} else if (type.equals(JSONResult.class)) {
			JSONResult res = () -> {if (returnedNode == null) 
				return "{}";
			else {
				CyJSONUtil cyJSONUtil = serviceRegistrar.getService(CyJSONUtil.class);
				return "{\"node\":"+cyJSONUtil.toJson(returnedNode)+"}";
			}};
			return res;
		}
		return returnedNode;
	}
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(CyNode.class, String.class, JSONResult.class);
	}
}
