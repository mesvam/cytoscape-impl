package org.cytoscape.internal.view;

import static org.cytoscape.internal.view.util.ViewUtil.invokeOnEDT;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.cytoscape.application.swing.CyAction;
import org.cytoscape.internal.model.RootNetworkManager;
import org.cytoscape.internal.task.CytoPanelTaskFactoryTunableAction;
import org.cytoscape.internal.task.DynamicTaskFactory;
import org.cytoscape.internal.task.DynamicTogglableTaskFactory;
import org.cytoscape.internal.task.TaskFactoryTunableAction;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.DynamicTaskFactoryProvisioner;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewCollectionTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.RootNetworkCollectionTaskFactory;
import org.cytoscape.task.TableTaskFactory;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.Togglable;

/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
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

/**
 * Creates the menu and tool bars for a Cytoscape window object. It
 * also provides access to individual menus and items.<BR>
 * <p>
 * AddAction takes one more optional argument to specify index. App
 * writers can use this function to specify the location of the menu item.
 * </p>
 */
public class CytoscapeMenuPopulator {
	
	private final CytoscapeDesktop app;
	private final RootNetworkManager rootNetManager;
	private final CyServiceRegistrar serviceRegistrar;

	private final Map<TaskFactory, CyAction> taskMap;
	private final Map<Object, TaskFactory> provisionerMap;
	
	/**
	 * This will construct the basic bar objects, but won't fill them with menu items and associated action listeners.
	 */
	public CytoscapeMenuPopulator(CytoscapeDesktop app, RootNetworkManager rootNetManager,
			CyServiceRegistrar serviceRegistrar) {
		this.app = app;
		this.rootNetManager = rootNetManager;
		this.serviceRegistrar = serviceRegistrar;

		taskMap = new HashMap<>();
		provisionerMap = new IdentityHashMap<>();
	}

	public void addTaskFactory(TaskFactory factory, Map<String, String> props) {
		var pref = (String) props.get("preferredTaskManager");
		
		if (pref != null && pref.equals("panel"))
			addAction(new CytoPanelTaskFactoryTunableAction(factory, null, props, serviceRegistrar), factory, props);
		else
			addFactory(factory, props);
	}

	public void removeTaskFactory(TaskFactory factory, Map<String, String> props) {
		removeFactory(factory, props);
	}

	public void addRootNetworkCollectionTaskFactory(RootNetworkCollectionTaskFactory factory, Map<String, String> props) {
		invokeOnEDT(() -> {
			var provisioner = factory instanceof Togglable ?
					new DynamicTogglableTaskFactory(factory, rootNetManager) :
					new DynamicTaskFactory(factory, rootNetManager);
			
			provisionerMap.put(factory, provisioner);
			addFactory(provisioner, props);
		});
	}
	
	public void removeRootNetworkCollectionTaskFactory(RootNetworkCollectionTaskFactory factory, Map<String, String> props) {
		invokeOnEDT(() -> {
			removeFactory(provisionerMap.remove(factory), props);
		});
	}
	
	public void addNetworkTaskFactory(NetworkTaskFactory factory, Map<String, String> props) {
		var factoryProvisioner = serviceRegistrar.getService(DynamicTaskFactoryProvisioner.class);
		var provisioner = factoryProvisioner.createFor(factory);
		
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkTaskFactory(NetworkTaskFactory factory, Map<String, String> props) {
		removeFactory(provisionerMap.get(factory), props);
	}

	public void addNetworkViewTaskFactory(NetworkViewTaskFactory factory, Map<String, String> props) {
		// Check to make sure this is supposed to be in the menus
		if (props.containsKey(IN_MENU_BAR) && !Boolean.parseBoolean(props.get(IN_MENU_BAR).toString()))
			return;
		
		var factoryProvisioner = serviceRegistrar.getService(DynamicTaskFactoryProvisioner.class);
		var provisioner = factoryProvisioner.createFor(factory);
		
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkViewTaskFactory(NetworkViewTaskFactory factory, Map<String, String> props) {
		removeFactory(provisionerMap.get(factory), props);
	}

	public void addNetworkViewCollectionTaskFactory(NetworkViewCollectionTaskFactory factory, Map<String, String> props) {
		var factoryProvisioner = serviceRegistrar.getService(DynamicTaskFactoryProvisioner.class);
		var provisioner = factoryProvisioner.createFor(factory);
		
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkViewCollectionTaskFactory(NetworkViewCollectionTaskFactory factory, Map<String, String> props) {
		removeFactory(provisionerMap.get(factory), props);
	}
	
	public void addNetworkCollectionTaskFactory(NetworkCollectionTaskFactory factory, Map<String, String> props) {
		var factoryProvisioner = serviceRegistrar.getService(DynamicTaskFactoryProvisioner.class);
		var provisioner = factoryProvisioner.createFor(factory);
		
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkCollectionTaskFactory(NetworkCollectionTaskFactory factory, Map<String, String> props) {
		removeFactory(provisionerMap.get(factory), props);
	}
	
	public void addTableTaskFactory(TableTaskFactory factory, Map<String, String> props) {
		var factoryProvisioner = serviceRegistrar.getService(DynamicTaskFactoryProvisioner.class);
		var provisioner = factoryProvisioner.createFor(factory);
		
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}
	
	public void removeTableTaskFactory(TableTaskFactory factory, Map<String, String> props) {
		removeFactory(provisionerMap.get(factory), props);
	}
	
	/**
	 * Wraps the task factory in a {@link TaskFactoryTunableAction}.
	 */
	private void addFactory(TaskFactory factory, Map<String, String> props) {
		final CyAction action;
		
		if (props.containsKey(ServiceProperties.ENABLE_FOR))
			action = new TaskFactoryTunableAction(factory, props, serviceRegistrar);
		else
			action = new TaskFactoryTunableAction(serviceRegistrar, factory, props);

		addAction(action, factory, props);
	}

	private void addAction(CyAction action, TaskFactory factory, Map<String, String> props) {
		taskMap.put(factory, action);
		app.addAction(action, props);
	}

	private void removeFactory(TaskFactory factory, Map<String, String> props) {
		var action = taskMap.remove(factory);
		
		if (action != null) {
			if (action.isInMenuBar())
				app.removeAction(action);

			if (action.isInToolBar())
				app.removeAction(action);
		}
	}
}
