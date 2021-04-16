package org.cytoscape.browser.internal.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JToolBar;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.browser.internal.view.TableBrowserMediator;

/*
 * #%L
 * Cytoscape Table Browser Impl (table-browser-impl)
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

@SuppressWarnings("serial")
public class TableOptionsAction extends AbstractCyAction {

	private static String TITLE = "Toggle Options";
	
	private final TableBrowserMediator mediator;

	public TableOptionsAction(Icon icon, float toolbarGravity, TableBrowserMediator mediator) {
		super(TITLE);
		this.mediator = mediator;
		
		putValue(SHORT_DESCRIPTION, TITLE);
		putValue(LARGE_ICON_KEY, icon);
		setIsInNodeTableToolBar(true);
		setIsInEdgeTableToolBar(true);
		setIsInNetworkTableToolBar(true);
		setIsInUnassignedTableToolBar(true);
		setToolbarGravity(toolbarGravity);
		insertSeparatorBefore = true;
		useToggleButton = true;
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		var source = evt.getSource();
		
		if (source instanceof AbstractButton) {
			var button = (AbstractButton) source;
			var parent = button.getParent();
			
			if (parent instanceof JToolBar)
				mediator.setOptionsBarVisible((JToolBar) parent, button.isSelected());
		}
	}
}
