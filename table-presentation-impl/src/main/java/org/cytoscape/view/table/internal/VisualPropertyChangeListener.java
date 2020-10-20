package org.cytoscape.view.table.internal;

import static org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.table.TableColumn;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.TableViewChangedEvent;
import org.cytoscape.view.model.events.TableViewChangedListener;
import org.cytoscape.view.model.table.CyColumnView;
import org.cytoscape.view.model.table.CyTableView;
import org.cytoscape.view.presentation.property.table.TableMode;
import org.cytoscape.view.table.internal.impl.BrowserTable;
import org.cytoscape.view.table.internal.impl.BrowserTableColumnModel;
import org.cytoscape.view.table.internal.impl.BrowserTableModel;
import org.cytoscape.view.table.internal.impl.BrowserTableModel.ViewMode;

public class VisualPropertyChangeListener implements TableViewChangedListener {

	private final CyTableView tableView;
	private final BrowserTable browserTable;
	
	public VisualPropertyChangeListener(BrowserTable browserTable, CyTableView tableView) {
		this.tableView = tableView;
		this.browserTable = browserTable;
	}
	
	@Override
	public void handleEvent(TableViewChangedEvent<?> e) {
		if(e.getSource() != tableView)
			return;
		
		boolean reorderCols = false;
		
		for(var record : e.getPayloadCollection()) {
			VisualProperty<?> vp = record.getVisualProperty();
			Object value = record.getValue();
			
			if(record.getView().getModel() instanceof CyColumn) {
				CyColumnView colView = (CyColumnView) record.getView();
				updateColumnVP(colView, vp, value);
				if(vp == COLUMN_GRAVITY) {
					reorderCols = true;
				}
			} else if(record.getView().getModel() instanceof CyTable) {
				if(vp == TABLE_VIEW_MODE) {
					changeSelectionMode((TableMode)record.getValue());
				}
			}
		}
		
		if(reorderCols) {
			var colModel = (BrowserTableColumnModel) browserTable.getColumnModel();
			colModel.reorderColumnsToRespectGravity();
		}
	}
	
	
	private void updateColumnVP(CyColumnView colView, VisualProperty<?> vp, Object value) {
		if(vp == COLUMN_VISIBLE) {
			boolean visible = !Boolean.FALSE.equals(value);
			browserTable.setColumnVisibility(colView.getModel().getName(), visible);
		} else if(vp == CELL_BACKGROUND_PAINT) {
			browserTable.repaint();
		} else if (vp == COLUMN_FORMAT) {
			browserTable.repaint();
		} else if (vp == COLUMN_GRAVITY) {
			if(value instanceof Number) {
				double gravity = ((Number)value).doubleValue();
				var colModel = (BrowserTableColumnModel) browserTable.getColumnModel();
				TableColumn col = colModel.getTableColumn(colView.getSUID());
				colModel.setColumnGravity(col, gravity);
			}
		}
	}
	
	// MKTODO this needs to go in the renderer
	private void changeSelectionMode(TableMode tableMode) {
		BrowserTableModel model = (BrowserTableModel) browserTable.getModel();
		
		ViewMode viewMode = ViewMode.fromVisualPropertyValue(tableMode);
		model.setViewMode(viewMode);
		model.updateViewMode();
		
		if(viewMode == ViewMode.ALL && browserTable.getColumn(CyNetwork.SELECTED) != null) {
			// Show the current selected rows
			Set<Long> suidSelected = new HashSet<>();
			Set<Long> suidUnselected = new HashSet<>();
			Collection<CyRow> selectedRows = tableView.getModel().getMatchingRows(CyNetwork.SELECTED, Boolean.TRUE);
	
			for(CyRow row : selectedRows) {
				suidSelected.add(row.get(CyIdentifiable.SUID, Long.class));
			}
	
			if(!suidSelected.isEmpty()) {
				browserTable.changeRowSelection(suidSelected, suidUnselected);
			}
		}
	}
	
}
