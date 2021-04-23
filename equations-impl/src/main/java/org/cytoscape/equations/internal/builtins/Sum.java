package org.cytoscape.equations.internal.builtins;

/*
 * #%L
 * Cytoscape Equations Impl (equations-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2013 The Cytoscape Consortium
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


import org.cytoscape.equations.AbstractFunction;
import org.cytoscape.equations.ArgDescriptor;
import org.cytoscape.equations.ArgType;
import org.cytoscape.equations.FunctionUtil;
import org.cytoscape.equations.internal.Categories;


public class Sum extends AbstractFunction {
	public Sum() {
		super(new ArgDescriptor[] { new ArgDescriptor(ArgType.OPT_FLOATS, "numbers", "One or more numbers or lists of numbers.") });
	}

	/**
	 *  Used to parse the function string.  This name is treated in a case-insensitive manner!
	 *  @return the name by which you must call the function when used in an attribute equation.
	 */
	public String getName() { return "SUM"; }
	
	@Override
	public String getCategoryName() { return Categories.NUMERIC; }

	/**
	 *  Used to provide help for users.
	 *  @return a description of what this function does
	 */
	public String getFunctionSummary() { return "Returns the sum of all of its arguments."; }

	public Class<?> getReturnType() { return Double.class; }

	/**
	 *  @param args the function arguments which must be either one object of type Double or Long
	 *  @return the sum of all the numbers in "args"
	 */
	public Object evaluateFunction(final Object[] args) {
		final double[] numbers;
		try {
			numbers = FunctionUtil.getDoubles(args);
		} catch (final Exception e) {
			throw new IllegalArgumentException("in a call to SUM(): could not convert one of the arguments to a number or list of numbers.");
		}

		double sum = 0.0;
		for (final double d : numbers)
			sum += d;

		return sum;
	}
}
