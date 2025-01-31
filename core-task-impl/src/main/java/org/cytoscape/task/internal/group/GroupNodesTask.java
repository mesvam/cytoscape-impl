package org.cytoscape.task.internal.group;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.command.StringToModel;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.internal.utils.NodeTunable;
import org.cytoscape.util.json.CyJSONUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.undo.UndoSupport;

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

public class GroupNodesTask extends AbstractTask implements ObservableTask {

	private CyNetworkView netView;
	private CyGroup newGroup;
	private final CyServiceRegistrar serviceRegistrar;

	private static int groupNumber = 1;

	@ContainsTunables
	public NodeTunable nodeTunable;

	@Tunable(description="Enter group name:", longDescription=StringToModel.GROUP_NAME_LONG_DESCRIPTION, exampleStringValue=StringToModel.GROUP_NAME_EXAMPLE_STRING)
	public String groupName;

	public GroupNodesTask(CyNetworkView netView, CyServiceRegistrar serviceRegistrar) {
		this.netView = netView;
		this.serviceRegistrar = serviceRegistrar;

		if (groupName == null) {
			groupName = "Group " + groupNumber;
			groupNumber++;
		}
	}

	public GroupNodesTask(CyServiceRegistrar serviceRegistrar) {
		this.netView = null;
		this.serviceRegistrar = serviceRegistrar;
		nodeTunable = new NodeTunable(serviceRegistrar);
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		tm.setProgress(0.0);
		if (netView == null && nodeTunable == null) {
			tm.showMessage(TaskMonitor.Level.ERROR, "No network view!");
			return;
		}

		List<CyNode> selNodes;
		CyNetwork net;
		if (netView != null) {
			net = netView.getModel();
			// Get all of the selected nodes
			selNodes = CyTableUtil.getNodesInState(net, CyNetwork.SELECTED, true);
		} else {
			net = nodeTunable.getNetwork();
			selNodes = nodeTunable.getNodeList();
		}

		// At some point, we'll want to seriously think about only adding
		// those edges that are also selected, but for now....
		CyGroupFactory factory = serviceRegistrar.getService(CyGroupFactory.class);
		newGroup = factory.createGroup(net, selNodes, null, true);
		serviceRegistrar.getService(UndoSupport.class).postEdit(new GroupEdit(newGroup, serviceRegistrar));

		// Now some trickery to actually name the group.  Note that we need to change
		// both the SERIALIZABLE_NAME and SHARED_NAME columns
		CyRow groupRow = ((CySubNetwork)net).getRootNetwork().getRow(newGroup.getGroupNode(), CyRootNetwork.SHARED_ATTRS);
 		groupRow.set(CyRootNetwork.SHARED_NAME, groupName);

		tm.showMessage(TaskMonitor.Level.INFO, "Created group "+groupName+" with "+selNodes.size()+" nodes");
		// mgr.addGroup(group);
		tm.setProgress(1.0d);
	}

	@Override
	public Object getResults(Class requestedType) {
		if (newGroup == null) return null;
		if (requestedType.equals(CyGroup.class))		return newGroup;
		if (requestedType.equals(String.class))			return newGroup.toString();
		if (requestedType.equals(JSONResult.class))  {
			CyJSONUtil jsonUtil = serviceRegistrar.getService(CyJSONUtil.class);
			JSONResult res = () -> {
				if (newGroup == null) return "{}";
				return "{\"group\":"+newGroup.getGroupNode().getSUID()+"}";
				/*
				String val = "{\"group\":"+newGroup.getGroupNode().getSUID();
				List<CyNode> nodes = newGroup.getNodeList();
				if (nodes != null && nodes.size() > 0)
					val += "\"nodes\":"+jsonUtil.cyIdentifiablesToJson(nodes);
				return val+"}";
				*/
			};
			return res;
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(String.class, CyGroup.class, JSONResult.class);
	}
}
