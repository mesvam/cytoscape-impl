package org.cytoscape.filter.internal.filters.column;

import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNode;

public class ColumnElement implements Comparable<ColumnElement> {
	private final Class<?> tableType;
	private final SelectedColumnType colType;
	private final String name;
	private final boolean list;
	private final boolean placeholder;
	
	public static enum SelectedColumnType {
		NONE,
		INTEGER,
		DOUBLE,
		STRING,
		BOOLEAN;
		
		private static SelectedColumnType fromColumn(CyColumn col) {
			Class<?> colType = col.getType();
			if(colType.isAssignableFrom(List.class))
				colType = col.getListElementType();
			
			if(colType == Integer.class || colType == Long.class) // treat longs as ints for now, 
				return INTEGER;
			if(colType == Double.class)
				return DOUBLE;
			if(colType == Boolean.class)
				return BOOLEAN;
			if(colType == String.class)
				return STRING;
			return NONE;
		}
		
		private static boolean isList(CyColumn col) {
			Class<?> colType = col.getType();
			return colType.isAssignableFrom(List.class);
		}
	}
	
	
	public ColumnElement(Class<?> tableType, CyColumn col) {
		this.tableType = tableType;
		this.colType = SelectedColumnType.fromColumn(col);
		this.list = SelectedColumnType.isList(col);
		this.name = col.getName();
		this.placeholder = false;
	}
	
	
	public ColumnElement() {
		this.tableType = null;
		this.colType = SelectedColumnType.NONE;
		this.name = "Choose column...";
		this.list = false;
		this.placeholder = true;
	}
	
	public boolean isPlaceholder() {
		return placeholder;
	}
	
	public Class<?> getTableType() {
		return tableType;
	}


	public SelectedColumnType getColType() {
		return colType;
	}

	public boolean isList() {
		return list;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public int compareTo(ColumnElement other) {
		if (tableType == null && other.tableType == null) {
			return String.CASE_INSENSITIVE_ORDER.compare(name, other.name);
		}
		if (tableType == null) {
			return -1;
		}
		if (other.tableType == null) {
			return 1;
		}
		if (tableType.equals(other.tableType)) {
			return String.CASE_INSENSITIVE_ORDER.compare(name, other.name);
		}
		if (tableType.equals(CyNode.class)) {
			return -1;
		}
		return 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((colType == null) ? 0 : colType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((tableType == null) ? 0 : tableType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		ColumnElement other = (ColumnElement) obj;
		if(colType != other.colType)
			return false;
		if(name == null) {
			if(other.name != null)
				return false;
		} else if(!name.equals(other.name))
			return false;
		if(tableType == null) {
			if(other.tableType != null)
				return false;
		} else if(!tableType.equals(other.tableType))
			return false;
		return true;
	}
	
	
}
