package org.cytoscape.equations.internal.builtins;

/*
 * #%L
 * Cytoscape Equations Impl (equations-impl)
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.*;


public class OrTest extends TestCase {
	public void testAll() throws Exception {
		final List<Object> list = new ArrayList<Object>();
		list.add("false");
		list.add(Long.valueOf(0L));
		list.add(Double.valueOf(1.3));

		final Map<String, Object> variablesAndValues = new HashMap<String, Object>();
		variablesAndValues.put("LIST", list);

		assertTrue(Framework.executeTest("=OR($LIST,\"false\",FALSE)", variablesAndValues, Boolean.valueOf(true)));
		assertTrue(Framework.executeTest("=OR()", Boolean.valueOf(false)));
		assertTrue(Framework.executeTest("=OR(TRUE)", variablesAndValues, Boolean.valueOf(true)));
		assertTrue(Framework.executeTest("=OR(FALSE)", variablesAndValues, Boolean.valueOf(false)));
		assertTrue(Framework.executeTest("=OR(FALSE,FALSE)", variablesAndValues, Boolean.valueOf(false)));
		assertTrue(Framework.executeTest("=OR(FALSE,TRUE)", variablesAndValues, Boolean.valueOf(true)));
	}
}
