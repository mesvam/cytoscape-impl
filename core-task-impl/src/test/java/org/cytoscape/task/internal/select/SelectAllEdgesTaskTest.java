package org.cytoscape.task.internal.select;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.Task;
import org.junit.Before;
import org.junit.Test;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2021 The Cytoscape Consortium
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

public class SelectAllEdgesTaskTest extends AbstractSelectTaskTester {
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testRun() throws Exception {
		Set<CyRow> deselectedEdges = new HashSet<>();
		deselectedEdges.add(r1);
		deselectedEdges.add(r2);
		when(edgeTable.getMatchingRows(CyNetwork.SELECTED, false)).thenReturn(deselectedEdges);
		
		// run the task
		Task t = new SelectAllEdgesTask(net, serviceRegistrar);
		t.run(tm);

		// check that the expected rows were set
		verify(r1, times(1)).set("selected", true);
		verify(r2, times(1)).set("selected", true);
	}
}
