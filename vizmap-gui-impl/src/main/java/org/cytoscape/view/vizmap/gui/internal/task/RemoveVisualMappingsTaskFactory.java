package org.cytoscape.view.vizmap.gui.internal.task;

import java.util.HashSet;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.gui.VizMapGUI;
import org.cytoscape.view.vizmap.gui.internal.util.ServicesUtil;
import org.cytoscape.view.vizmap.gui.internal.view.VizMapperMainPanel;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

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

public class RemoveVisualMappingsTaskFactory extends AbstractTaskFactory {

	private final ServicesUtil servicesUtil;

	public RemoveVisualMappingsTaskFactory(final ServicesUtil servicesUtil) {
		this.servicesUtil = servicesUtil;
	}

	@Override
	public TaskIterator createTaskIterator() {
		var style = servicesUtil.get(VisualMappingManager.class).getCurrentVisualStyle();
		var mappings = new HashSet<VisualMappingFunction<?,?>>();
		var gui = servicesUtil.get(VizMapGUI.class);
		
		if (gui instanceof VizMapperMainPanel) {
			var vpSheet = ((VizMapperMainPanel)gui).getSelectedVisualPropertySheet();
			var selectedItems = vpSheet.getSelectedItems();
			
			for (var item : selectedItems) {
				if (item.getModel().getVisualMappingFunction() != null)
					mappings.add(item.getModel().getVisualMappingFunction());
			}
		}
		
		return new TaskIterator(new RemoveVisualMappingsTask(mappings, style, servicesUtil));
	}
	
	@Override
	public boolean isReady() {
		var gui = servicesUtil.get(VizMapGUI.class);
		
		if (gui instanceof VizMapperMainPanel) {
			var vpSheet = ((VizMapperMainPanel)gui).getSelectedVisualPropertySheet();
			
			if (vpSheet.getModel().getLexiconType() != CyNetwork.class) {
				for (var item : vpSheet.getSelectedItems()) {
					if (item.getModel().getVisualMappingFunction() != null)
						return true;
				}
			}
		}
		
		return false;
	}
}