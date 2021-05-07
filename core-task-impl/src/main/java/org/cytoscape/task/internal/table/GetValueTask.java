package org.cytoscape.task.internal.table;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.command.StringToModel;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.internal.utils.DataUtils;
import org.cytoscape.task.internal.utils.TableTunable;
import org.cytoscape.work.ContainsTunables;
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

public class GetValueTask extends AbstractTableDataTask implements ObservableTask {
	
	Object resultValue;
	CyTable table;

	@ContainsTunables
	public TableTunable tableTunable;

	@Tunable(description="Key value for row", context="nogui", longDescription=StringToModel.ROW_LONG_DESCRIPTION, exampleStringValue = StringToModel.ROW_EXAMPLE)
	public String keyValue;

	@Tunable(description="Name of column", context="nogui", longDescription=StringToModel.COLUMN_LONG_DESCRIPTION, exampleStringValue = StringToModel.COLUMN_EXAMPLE)
	public String column;

	public GetValueTask(CyServiceRegistrar serviceRegistrar) {
		super(serviceRegistrar);
		tableTunable = new TableTunable(serviceRegistrar);
	}

	@Override
	public void run(final TaskMonitor tm) {
		table = tableTunable.getTable();
		if (table == null) {
			tm.showMessage(TaskMonitor.Level.ERROR, "Unable to find table '"+tableTunable.getTableString()+"'");
			return;
		}

		if (keyValue == null) {
			tm.showMessage(TaskMonitor.Level.ERROR,  "Key of desired row must be specified");
			return;
		}

		if (column == null) {
			tm.showMessage(TaskMonitor.Level.ERROR,  "Column name must be specified");
			return;
		}

		// Get the primary key column
		CyColumn primaryKColumn = table.getPrimaryKey();
		Class keyType = primaryKColumn.getType();
		Object key = null;
		try {
			key = DataUtils.convertString(keyValue, keyType);
		} catch (NumberFormatException nfe) {
			tm.showMessage(TaskMonitor.Level.ERROR,  "Unable to convert "+keyValue+" to a "+keyType.getName()+": "+nfe.getMessage());
			return;
		}
		if (key == null) {
			tm.showMessage(TaskMonitor.Level.ERROR, "Unable to convert "+keyValue+" to a "+keyType.getName());
			return;
		}

		CyColumn targetColumn = table.getColumn(column);
		if (targetColumn == null) {
			tm.showMessage(TaskMonitor.Level.ERROR,  "Can't find the '"+column+"' column in this table");
			return;
		}

		CyRow row = table.getRow(key);
		if (row == null) {
			tm.showMessage(TaskMonitor.Level.ERROR,  "Can't find a '"+keyValue+"' row in this table");
			return;
		}
		
		Class columnType = targetColumn.getType();
		if (targetColumn.getType().equals(List.class)) {
			Class elementType = targetColumn.getListElementType();
			List<?> valueList = row.getList(column, elementType);
			tm.showMessage(TaskMonitor.Level.INFO, " " + column + "=" + ((valueList == null) ? "<null>" : DataUtils.convertData(valueList)));
			resultValue = valueList;
		} else {
			Object value = row.get(column, columnType);
			tm.showMessage(TaskMonitor.Level.INFO,  " " + column + "="  + ((value == null) ? "<null>" : DataUtils.convertData(value)));
			resultValue = value;
		}

	}
	
	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(String.class, JSONResult.class);
	}

	@Override
	public Object getResults(Class requestedType) {
		if (resultValue == null) return null;
		if (requestedType.equals(String.class)) 	return DataUtils.convertData(resultValue);
		if (requestedType.equals(JSONResult.class)) {
			JSONResult res = () -> {	
				return "{ \"table\": "+table.getSUID()+
				       ", \"column\":"+column+
							 ", \"row\":"+keyValue+
							 " \"value\":"+DataUtils.convertDataJSON(resultValue)+"}"; 
			};
			return res;
		}
		return resultValue;
	}
}
