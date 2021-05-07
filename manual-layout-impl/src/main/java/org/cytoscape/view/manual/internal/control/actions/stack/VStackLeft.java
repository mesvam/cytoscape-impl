package org.cytoscape.view.manual.internal.control.actions.stack;

/*
 * #%L
 * Cytoscape Manual Layout Impl (manual-layout-impl)
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

import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.cytoscape.view.manual.internal.control.actions.AbstractControlAction;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;


/**
 *
 */
public class VStackLeft extends AbstractControlAction {

	public VStackLeft(Icon i,CyApplicationManager mgr) {
		super("",i,mgr);
	}

	protected void control(List<View<CyNode>> nodes) {
		if (nodes.size() <= 1)
			return;

		Collections.sort(nodes, new YComparator());

		
//		double d = Y_max - Y_min;
//		d = d / (nodes.size() - 1);

		//Note: X, Y are at node centers
		for (int i = 1; i < nodes.size(); i++) {
			nodes.get(i).setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION,
					nodes.get(i-1).getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION) + 
					nodes.get(i-1).getVisualProperty(BasicVisualLexicon.NODE_HEIGHT) *0.5 +
					nodes.get(i).getVisualProperty(BasicVisualLexicon.NODE_HEIGHT) * 0.5
					);
			nodes.get(i).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION,
					nodes.get(i-1).getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION) -
					nodes.get(i-1).getVisualProperty(BasicVisualLexicon.NODE_WIDTH) * 0.5 +
					nodes.get(i).getVisualProperty(BasicVisualLexicon.NODE_WIDTH) * 0.5
					);
			
		}
	}
}
