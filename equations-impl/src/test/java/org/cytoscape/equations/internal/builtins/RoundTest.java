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


import junit.framework.*;


public class RoundTest extends TestCase {
	public void testAll() throws Exception {
		assertTrue(Framework.executeTest("=ROUND(2.15, 1)", Double.valueOf(2.2)));
		assertTrue(Framework.executeTest("=ROUND(2.149,1)", Double.valueOf(2.1)));
		assertTrue(Framework.executeTest("=ROUND(-1.475,2)", Double.valueOf(-1.48)));
		assertTrue(Framework.executeTest("=ROUND(21.5, -1)", Double.valueOf(20.0)));
	}
}
