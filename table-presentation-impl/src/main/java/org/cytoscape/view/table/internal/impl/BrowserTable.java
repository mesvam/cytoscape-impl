package org.cytoscape.view.table.internal.impl;

/*
 * #%L
 * Cytoscape Table Browser Impl (table-browser-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2019 The Cytoscape Consortium
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
import static org.cytoscape.view.table.internal.impl.BrowserTableModel.ViewMode.ALL;
import static org.cytoscape.view.table.internal.impl.BrowserTableModel.ViewMode.AUTO;
import static org.cytoscape.view.table.internal.impl.BrowserTableModel.ViewMode.SELECTED;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.equations.EquationCompiler;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.events.ColumnNameChangedEvent;
import org.cytoscape.model.events.ColumnNameChangedListener;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.util.swing.TextWrapToolTip;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.events.AboutToRemoveColumnViewEvent;
import org.cytoscape.view.model.events.AboutToRemoveColumnViewListener;
import org.cytoscape.view.model.events.AddedColumnViewEvent;
import org.cytoscape.view.model.events.AddedColumnViewListener;
import org.cytoscape.view.model.table.CyTableView;
import org.cytoscape.view.presentation.property.table.BasicTableVisualLexicon;
import org.cytoscape.view.table.internal.util.TableBrowserUtil;
import org.cytoscape.view.table.internal.util.ValidatedObjectAndEditString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class BrowserTable extends JTable implements MouseListener, ActionListener, MouseMotionListener,
													 AboutToRemoveColumnViewListener, AddedColumnViewListener,
													 ColumnNameChangedListener, RowsSetListener {

	private static final Logger logger = LoggerFactory.getLogger(CyUserLog.NAME);
	
	private static final String LINE_BREAK = "\n";
	private static final String CELL_BREAK = "\t";

	private static final Font BORDER_FONT = UIManager.getFont("Label.font").deriveFont(11f);

	private Clipboard systemClipboard;
	private CellEditorRemover editorRemover;
	private final TableCellRenderer cellRenderer;
	private final HashMap<String, Integer> columnWidthMap = new HashMap<>();

	// For right-click menu
	private JPopupMenu popupMenu;
	private JPopupMenu headerPopupMenu;
	private JMenuItem openFormulaBuilderMenuItem;

	private MultiLineTableCellEditor multiLineCellEditor;
	
	private final EquationCompiler compiler;
	private final PopupMenuHelper popupMenuHelper;
	private final CyServiceRegistrar serviceRegistrar;
	
	private boolean ignoreRowSelectionEvents;
	private boolean ignoreRowSetEvents;

	// ==[ CONSTRUCTORS ]===============================================================================================
	
	public BrowserTable(EquationCompiler compiler, PopupMenuHelper popupMenuHelper, CyServiceRegistrar serviceRegistrar) {
		this.compiler = compiler;
		this.popupMenuHelper = popupMenuHelper;
		this.serviceRegistrar = serviceRegistrar;
		
		cellRenderer = new BrowserTableCellRenderer(serviceRegistrar);
		multiLineCellEditor = new MultiLineTableCellEditor();
		
		init();
		setAutoCreateColumnsFromModel(false);
		setAutoCreateRowSorter(true);
		setCellSelectionEnabled(true);
		setShowGrid(false);
		setDefaultEditor(Object.class, multiLineCellEditor);
		getPopupMenu();
		getHeaderPopupMenu();
		setKeyStroke();
		setTransferHandler(new BrowserTableTransferHandler());
	}

	// ==[ PUBLIC METHODS ]=============================================================================================
	
	@Override
	public void setModel(final TableModel dataModel) {
//		assert(dataModel instanceof BrowserTableModel);
		super.setModel(dataModel);
		
		BrowserTableColumnModel columnModel = new BrowserTableColumnModel();
		setColumnModel(columnModel);

		if (dataModel instanceof BrowserTableModel) {
			BrowserTableModel model = (BrowserTableModel) dataModel;
			
			for (int i = 0; i < model.getColumnCount(); i++) {
				String name = model.getColumnName(i);
				View<CyColumn> view = model.getTableView().getColumnView(name);
				boolean visible = view.getVisualProperty(BasicTableVisualLexicon.COLUMN_VISIBLE);
				double gravity  = view.getVisualProperty(BasicTableVisualLexicon.COLUMN_GRAVITY);
				
				TableColumn tableColumn = new TableColumn(i);
				tableColumn.setHeaderValue(name);
				tableColumn.setHeaderRenderer(new BrowserTableHeaderRenderer(serviceRegistrar));
				columnModel.addColumn(tableColumn, view.getSUID(), visible, gravity);
			}
		}
		
		columnModel.reorderColumnsToRespectGravity();
	}

	public BrowserTableModel getBrowserTableModel() {
		return (BrowserTableModel) getModel();
	}
	
	@Override
	public synchronized void addMouseListener(final MouseListener listener) {
		// Hack to prevent selected rows from being deselected when the user
		// CONTROL-clicks one of those rows on Mac (popup trigger).
		super.addMouseListener(new ProxyMouseListener(listener));
	}
	
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return cellRenderer;
	}

	@Override
	public boolean isCellEditable(final int row, final int column) {
		return this.getModel().isCellEditable(convertRowIndexToModel(row), convertColumnIndexToModel(column));
	}

	@Override
	public boolean editCellAt(int row, int column, EventObject e) {
		if (cellEditor != null && !cellEditor.stopCellEditing())
			return false;

		if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount())
			return false;

		if (!isCellEditable(row, column))
			return false;

		if (editorRemover == null) {
			KeyboardFocusManager fm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			editorRemover = new CellEditorRemover(fm);
			fm.addPropertyChangeListener("permanentFocusOwner", editorRemover);
		}

		TableCellEditor editor = getCellEditor(row, column);

		// remember the table row, because tableModel will disappear if
		// user click on open space on canvas, so we have to remember it before it is gone
		BrowserTableModel model = (BrowserTableModel) this.getModel();
		CyRow cyRow = model.getCyRow(convertRowIndexToModel(row));
		String columnName = model.getColumnName(convertColumnIndexToModel(column));
		editorRemover.setCellData(cyRow, columnName);

		if ((editor != null) && editor.isCellEditable(e)) {
			// Do this first so that the bounds of the JTextArea editor will be correct.
			setEditingRow(row);
			setEditingColumn(column);
			setCellEditor(editor);
			editor.addCellEditorListener(this);

			editorComp = prepareEditor(editor, row, column);

			if (editorComp == null) {
				removeEditor();
				return false;
			}

			Rectangle cellRect = getCellRect(row, column, false);

			if (editor instanceof MultiLineTableCellEditor) {
				Dimension prefSize = editorComp.getPreferredSize();
				((JComponent) editorComp).putClientProperty(MultiLineTableCellEditor.UPDATE_BOUNDS, Boolean.TRUE);
				editorComp.setBounds(cellRect.x, cellRect.y, Math.max(cellRect.width, prefSize.width),
						Math.max(cellRect.height, prefSize.height));
				((JComponent) editorComp).putClientProperty(MultiLineTableCellEditor.UPDATE_BOUNDS, Boolean.FALSE);
			} else {
				editorComp.setBounds(cellRect);
			}
			
			editorComp.addKeyListener(new KeyListener() {

				private void deselect(KeyEvent e) {
					e.consume();
					Component comp = e.getComponent();
					comp.removeKeyListener(this);
					editor.stopCellEditing();
					
				}

				@Override
				public void keyTyped(KeyEvent e) {
					
				}

				@Override
				public void keyReleased(KeyEvent e) {
				}

				@Override
				public void keyPressed(KeyEvent event) {
					int new_row = row, new_column = column;
					final int modifiers = event.getModifiers();

					if (event.getKeyCode() == KeyEvent.VK_ENTER) {
						if (modifiers == 0) {
							new_row += 1;
						} else if (modifiers == KeyEvent.VK_SHIFT) {
							new_row -= 1;
						} else {
							return;
						}
					} else if (event.getKeyCode() == KeyEvent.VK_TAB) {
						if (modifiers == 0) {
							new_column += 1;
						} else if (modifiers == KeyEvent.VK_SHIFT) {
							new_column -= 1;
						} else {
							return;
						}
					} else {
						return;
					}
					deselect(event);
					changeSelection(new_row, new_column, false, false);
				}
			});

			add(editorComp);
			editorComp.validate();

			return true;
		}

		return false;
	}

	@Override
	public void addRowSelectionInterval(int index0, int index1) {
		super.addRowSelectionInterval(index0, index1);
		// Make sure this is set, so the selected cell is correctly rendered when there is more than one selected row
		((BrowserTableListSelectionModel) selectionModel).setLastSelectedRow(index1);
	}
	
	@Override
	public int getSelectedRow() {
		// Try the last selected row first
		int row = ((BrowserTableListSelectionModel) selectionModel).getLastSelectedRow();
		
		if (row < 0 || !isRowSelected(row))
			row = selectionModel.getMinSelectionIndex();
		
		return row;
	}
	
	@Override
	public JToolTip createToolTip() {
		TextWrapToolTip tip = new TextWrapToolTip();
		tip.setMaximumSize(new Dimension(480, 320));
		tip.setComponent(this);
		return tip;
	}
	
	public void showListContents(int modelRow, int modelColumn, MouseEvent e) {
		final BrowserTableModel model = (BrowserTableModel) getModel();
		final Class<?> columnType = model.getColumn(modelColumn).getType();

		if (columnType == List.class) {
			final ValidatedObjectAndEditString value = (ValidatedObjectAndEditString) model.getValueAt(modelRow, modelColumn);

			if (value != null) {
				final List<?> list = (List<?>) value.getValidatedObject();
				
				if (list != null && !list.isEmpty())
					showCellMenu(List.class, list, "Entries", e);
			}
		}
	}

	/**
	 * This method initializes popupMenu
	 * 
	 * @return the inilialised pop-up menu
	 */
	public JPopupMenu getPopupMenu() {
		if (popupMenu != null)
			return popupMenu;

		popupMenu = new JPopupMenu();
		openFormulaBuilderMenuItem = new JMenuItem("Open Formula Builder");

		final JTable table = this;
		
		openFormulaBuilderMenuItem.addActionListener(evt -> {
			final int cellRow = table.getSelectedRow();
			final int cellColumn = table.getSelectedColumn();
			final BrowserTableModel tableModel = (BrowserTableModel) getModel();
			final JFrame rootFrame = (JFrame) SwingUtilities.getRoot(table);
			
			if (cellRow == -1 || cellColumn == -1 || !tableModel.isCellEditable(cellRow, cellColumn)) {
				JOptionPane.showMessageDialog(rootFrame, "Can't enter a formula w/o a selected cell.",
						"Information", JOptionPane.INFORMATION_MESSAGE);
			} else {
				// MKTODO
//				final String columnName = tableModel.getColumnName(cellColumn);
//				FormulaBuilderDialog formulaBuilderDialog = new FormulaBuilderDialog(compiler, BrowserTable.this,
//						rootFrame, columnName);
//				formulaBuilderDialog.setLocationRelativeTo(rootFrame);
//				formulaBuilderDialog.setVisible(true);
			}
		});

		return popupMenu;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		maybeShowPopup(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		selectFocusedCell(e);
		maybeShowPopup(e);
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e) && (getSelectedRows().length != 0)) {
			final int viewColumn = getColumnModel().getColumnIndexAtX(e.getX());
			final int viewRow = e.getY() / getRowHeight();
			final int modelColumn = convertColumnIndexToModel(viewColumn);
			final int modelRow = convertRowIndexToModel(viewRow);
			
			final BrowserTableModel tableModel = (BrowserTableModel) this.getModel();
			
			// Bail out if we're at the ID column:
			if (tableModel.isPrimaryKey(modelColumn))
				return;

			// Make sure the column and row we're clicking on actually exists!
			if (modelColumn >= tableModel.getColumnCount() || modelRow >= tableModel.getRowCount())
				return;
			
			// Display List menu.
			showListContents(modelRow, modelColumn, e);
		}
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
	}

	@Override
	public void mouseExited(final MouseEvent e) {
	}
	
	@Override
	public void mouseDragged(final MouseEvent e) {
		// save the column width, if user adjust column width manually
		if (e.getSource() instanceof JTableHeader) {
			final int index = getColumnModel().getColumnIndexAtX(e.getX());
			
			if (index != -1) {
				int colWidth = getColumnModel().getColumn(index).getWidth();
				this.columnWidthMap.put(this.getColumnName(index), new Integer(colWidth));
			}
		}
	}

	@Override
	public void mouseMoved(final MouseEvent e) {
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		if (event.getActionCommand().compareTo("Copy") == 0)
			copyToClipBoard();
	}

	@Override
	public void paint(Graphics graphics) {
		BrowserTableModel model = (BrowserTableModel) getModel();
		ReadWriteLock lock = model.getLock();
		lock.readLock().lock();
		try {
			if (!model.isDisposed()) {
				super.paint(graphics);
			}
		} finally {
			lock.readLock().unlock();
		}
	}
	

	@Override
	public void handleEvent(final AddedColumnViewEvent e) {
		BrowserTableModel model = (BrowserTableModel) getModel();
		CyTableView tableView = model.getTableView();
		
		if (e.getSource() != tableView)
			return;

		model.fireTableStructureChanged();
		
		var columnModel = (BrowserTableColumnModel) getColumnModel();

		CyColumn col = e.getColumnView().getModel();
		model.addColumn(col.getName());
		
		int colIndex = columnModel.getColumnCount();
		
		String name = col.getName();
		View<CyColumn> view = model.getTableView().getColumnView(name);
		boolean visible = view.getVisualProperty(BasicTableVisualLexicon.COLUMN_VISIBLE);
		double gravity  = view.getVisualProperty(BasicTableVisualLexicon.COLUMN_GRAVITY);
		
		TableColumn tableColumn = new TableColumn(colIndex);
		tableColumn.setHeaderValue(name);
		tableColumn.setHeaderRenderer(new BrowserTableHeaderRenderer(serviceRegistrar));
		columnModel.addColumn(tableColumn, view.getSUID(), visible, gravity);
		
		columnModel.reorderColumnsToRespectGravity();
	}

	@Override
	public void handleEvent(final AboutToRemoveColumnViewEvent e) {
		BrowserTableModel model = (BrowserTableModel) getModel();
		CyTableView tableView = model.getTableView();
		
		if (e.getSource() != tableView)
			return;

		model.fireTableStructureChanged();

		var columnModel = (BrowserTableColumnModel) getColumnModel();
		final String columnName = e.getColumnView().getModel().getName();
		boolean columnFound = false;
		int removedColIndex = -1;
		
		List<String> attrNames = model.getAllAttributeNames();
		
		for (int i = 0; i < attrNames.size(); ++i) {
			if (attrNames.get(i).equals(columnName)) {
				removedColIndex = i;
				columnModel.removeColumn (columnModel.getColumn(convertColumnIndexToView(i)));
				columnFound = true;
			} else if (columnFound){ //need to push back the model indexes for all of the columns after this
				
				TableColumn nextCol = columnModel.getColumnByModelIndex(i); 
				nextCol.setModelIndex(i- 1);
			}
		}
		
		if (removedColIndex != -1){ //remove the item after the loop is done
			model.removeColumn(removedColIndex);
		}
	}

	@Override
	public void handleEvent(final ColumnNameChangedEvent e) {
		BrowserTableModel model = (BrowserTableModel) getModel();
		CyTable dataTable = model.getDataTable();

		if (e.getSource() != dataTable)
			return;
		
		renameColumnName(e.getOldColumnName(), e.getNewColumnName());
		if (SwingUtilities.isEventDispatchThread()) {
			tableHeader.repaint();
		} else {
			SwingUtilities.invokeLater (new Runnable () {
				public void run () {
					tableHeader.repaint();
				}
			});
		}
	}
	
	@Override
	public void handleEvent(final RowsSetEvent e) {
		if (isEditing()){
			getCellEditor().stopCellEditing();
		}
		if (ignoreRowSetEvents)
			return;
		
		final BrowserTableModel model = (BrowserTableModel) getModel();
		final CyTable dataTable = model.getDataTable();

		if (e.getSource() != dataTable)
			return;		

		if (model.getViewMode() == SELECTED || model.getViewMode() == AUTO) {
			model.clearSelectedRows();
			boolean foundANonSelectedColumnName = false;
			
			for(String column : e.getColumns()) {
				if(!CyNetwork.SELECTED.equals(column)) {
					foundANonSelectedColumnName = true;
					break;
				}
			}
			
			if (!foundANonSelectedColumnName) {
				model.fireTableDataChanged();
				return;
			}
		}

		final Collection<RowSetRecord> rows = e.getPayloadCollection();

		synchronized (this) {
			model.fireTableDataChanged();
			if(model.getViewMode() == ALL) {
				CyTableManager tableManager = serviceRegistrar.getService(CyTableManager.class);
				if (!tableManager.getGlobalTables().contains(dataTable)) {
					bulkUpdate(rows);
				}
			}
		}
	}

	// ==[ PRIVATE METHODS ]============================================================================================
	
	protected void init() {
		final JTableHeader header = getTableHeader();
		header.setOpaque(false);
//		header.setDefaultRenderer(new BrowserTableHeaderRenderer(serviceRegistrar.getService(IconManager.class)));
		header.getColumnModel().setColumnSelectionAllowed(true);
		header.addMouseMotionListener(this);
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				maybeShowHeaderPopup(e);
			}
			@Override
			public void mouseReleased(final MouseEvent e) {
				maybeShowHeaderPopup(e);
			}
		});
		
		setSelectionModel(new BrowserTableListSelectionModel());
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		addMouseListener(this);
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				if (!e.getValueIsAdjusting() && !ignoreRowSelectionEvents)
					selectFromTable();
			}
		});
	}
	
	private void setKeyStroke() {
		final int modifiers = LookAndFeelUtil.isMac() ? ActionEvent.META_MASK : ActionEvent.CTRL_MASK;
		final KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, modifiers, false);
		// Identifying the copy KeyStroke user can modify this to copy on some other Key combination.
		this.registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);
		systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	}
	
	private JPopupMenu getHeaderPopupMenu() {
		if (headerPopupMenu == null)
			headerPopupMenu = new JPopupMenu();
		
		return headerPopupMenu;
	}
	
	private void showCellMenu(final Class<?> type, final List<?> listItems, final String borderTitle,
			final MouseEvent e) {
		final JPopupMenu cellMenu = new JPopupMenu();
		final Border popupBorder = BorderFactory.createTitledBorder(null, borderTitle,
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, BORDER_FONT);
		cellMenu.setBorder(popupBorder);
		
		JMenu curItem = null;
		String dispName;

		for (final Object item : listItems) {
			dispName = item.toString();

			if (dispName.length() > 60) {
				dispName = dispName.substring(0, 59) + " ...";
			}

			curItem = new JMenu(dispName);
			curItem.add(getPopupMenu());

			JMenuItem copyAll = new JMenuItem("Copy all entries");
			copyAll.addActionListener(evt -> {
				final StringBuilder builder = new StringBuilder();
				
				for (Object oneEntry : listItems)
					builder.append(oneEntry.toString() + CELL_BREAK);

				final StringSelection selection = new StringSelection(builder.toString());
				systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				systemClipboard.setContents(selection, selection);
			});
			curItem.add(copyAll);

			JMenuItem copy = new JMenuItem("Copy this entry");
			copy.addActionListener(evt -> {
				final StringSelection selection = new StringSelection(item.toString());
				systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				systemClipboard.setContents(selection, selection);
			});

			curItem.add(copy);
			if (dispName != null && (dispName.startsWith("http:") || dispName.startsWith("https:")))
				curItem.add(popupMenuHelper.getOpenLinkMenu(dispName));
			cellMenu.add(curItem);
		}

		cellMenu.show(e.getComponent(), e.getX(), e.getY());
	}
	
	private void maybeShowHeaderPopup(final MouseEvent e) {
		if (e.isPopupTrigger()) {
			final int column = getColumnModel().getColumnIndexAtX(e.getX());
			final BrowserTableModel tableModel = (BrowserTableModel) getModel();

			// Make sure the column we're clicking on actually exists!
			if (column >= tableModel.getColumnCount() || column < 0)
				return;

			// Ignore clicks on the ID column:
			if (tableModel.isPrimaryKey(convertColumnIndexToModel(column)))
				return;

			final CyColumn cyColumn = tableModel.getColumn(convertColumnIndexToModel(column));
			popupMenuHelper.createColumnHeaderMenu(cyColumn, tableModel.getTableType(), BrowserTable.this,
					e.getX(), e.getY());
		}
	}
	
	private void maybeShowPopup(final MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (LookAndFeelUtil.isWindows())
				selectFocusedCell(e);
			
			// Show context menu
			final int viewColumn = getColumnModel().getColumnIndexAtX(e.getX());
			final int viewRow = e.getY() / getRowHeight();
			final int modelColumn = convertColumnIndexToModel(viewColumn);
			final int modelRow = convertRowIndexToModel(viewRow);
			
			final BrowserTableModel tableModel = (BrowserTableModel) this.getModel();
			
			// Bail out if we're at the ID column:
			if (tableModel.isPrimaryKey(modelColumn))
				return;

			// Make sure the column and row we're clicking on actually exists!
			if (modelColumn >= tableModel.getColumnCount() || modelRow >= tableModel.getRowCount())
				return;
			
			final CyColumn cyColumn = tableModel.getColumn(modelColumn);
			final Object primaryKeyValue = ((ValidatedObjectAndEditString) tableModel.getValueAt(modelRow,
					tableModel.getDataTable().getPrimaryKey().getName())).getValidatedObject();
			popupMenuHelper.createTableCellMenu(cyColumn, primaryKeyValue, tableModel.getTableType(), this,
					e.getX(), e.getY(), this);
		}
	}
	
	private void selectFocusedCell(final MouseEvent e) {
		final int row = e.getY() / getRowHeight();
		final int column = getColumnModel().getColumnIndexAtX(e.getX());
		
		final int[] selectedRows = this.getSelectedRows();
		int binarySearch = Arrays.binarySearch(selectedRows, row);
		
		// Select clicked cell, if not selected yet
		if (binarySearch < 0) {
			// Clicked row not selected: Select only the right-clicked row/cell
			this.changeSelection(row, column, false, false);
		} else {
			// Row is already selected: Just guarantee the last clicked cell is highlighted properly
			((BrowserTableListSelectionModel) selectionModel).setLastSelectedRow(row);
			this.setColumnSelectionInterval(column, column);
			
			final BrowserTableModel tableModel = (BrowserTableModel) this.getModel();
			tableModel.fireTableRowsUpdated(selectedRows[0], selectedRows[selectedRows.length-1]);
		}
	}
	
	private String copyToClipBoard() {
		final StringBuffer sbf = new StringBuffer();

		/*
		 * Check to ensure we have selected only a contiguous block of cells.
		 */
		final int numcols = this.getSelectedColumnCount();
		final int numrows = this.getSelectedRowCount();

		final int[] rowsselected = this.getSelectedRows();
		final int[] colsselected = this.getSelectedColumns();

		// Return if no cell is selected.
		if (numcols == 0 && numrows == 0)
			return null;

		if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0] && numrows == rowsselected.length)
				&& (numcols - 1 == colsselected[colsselected.length - 1] - colsselected[0] 
						&& numcols == colsselected.length))) {
			final JFrame rootFrame = (JFrame) SwingUtilities.getRoot(this);
			JOptionPane.showMessageDialog(rootFrame, "Invalid Copy Selection", "Invalid Copy Selection",
					JOptionPane.ERROR_MESSAGE);

			return null;
		}

		for (int i = 0; i < numrows; i++) {
			for (int j = 0; j < numcols; j++) {
				final Object cellValue = this.getValueAt(rowsselected[i], colsselected[j]);
				final String cellText = cellValue instanceof ValidatedObjectAndEditString ?
						((ValidatedObjectAndEditString) cellValue).getEditString() : null;
				
				sbf.append(cellText != null ? escape(cellText) : "");

				if (j < numcols - 1)
					sbf.append(CELL_BREAK);
			}

			sbf.append(LINE_BREAK);
		}
		
		final StringSelection selection = new StringSelection(sbf.toString());
		systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		systemClipboard.setContents(selection, selection);

		return sbf.toString();
	}
	
	private String escape(final String cellValue) {
		return cellValue.replace(LINE_BREAK, " ").replace(CELL_BREAK, " ");
	}
	
	private void renameColumnName(final String oldName, final String newName) {
		var columnModel = (BrowserTableColumnModel) getColumnModel();
		BrowserTableModel model = (BrowserTableModel) getModel();
		
		int index = model.mapColumnNameToColumnIndex(oldName);
		if (index >= 0){
			model.setColumnName(index, newName);
			columnModel.getColumn(convertColumnIndexToView(index)).setHeaderValue(newName);
			return;
		}
	
		throw new IllegalStateException("The specified column " + oldName +" does not exist in the model.");
	}

	/**
	 * Select rows in the table when something selected in the network view.
	 * @param rows
	 */
	private void bulkUpdate(final Collection<RowSetRecord> rows) {
		final Set<Long> suidSelected = new HashSet<>();
		final Set<Long> suidUnselected = new HashSet<>();

		for (RowSetRecord rowSetRecord : rows) {
			if (rowSetRecord.getColumn().equals(CyNetwork.SELECTED)) {
				if (((Boolean)rowSetRecord.getValue()) == true)
					suidSelected.add(rowSetRecord.getRow().get(CyIdentifiable.SUID, Long.class));
				else
					suidUnselected.add(rowSetRecord.getRow().get(CyIdentifiable.SUID, Long.class));
			}
		}

		changeRowSelection(suidSelected, suidUnselected);
	}

	public void changeRowSelection(final Set<Long> suidSelected, final Set<Long> suidUnselected) {
		final BrowserTableModel model = (BrowserTableModel) getModel();
		final CyTable dataTable = model.getDataTable();
		final String pKeyName = dataTable.getPrimaryKey().getName();
		final int rowCount = getRowCount();
		
		try {
			ignoreRowSelectionEvents = true;
			
			for (int i = 0; i < rowCount; i++) {
				// Getting the row from data table solves the problem with hidden or moved SUID column.
				// However, since the rows might be sorted we need to convert the index to model
				int modelRow = convertRowIndexToModel(i);
				final ValidatedObjectAndEditString tableKey =
						(ValidatedObjectAndEditString) model.getValueAt(modelRow, pKeyName);
				Long pk = null;
				
				try {
					// TODO: Temp fix: is it a requirement that all CyTables have a Long SUID column as PK?
					pk = Long.parseLong(tableKey.getEditString());
				} catch (NumberFormatException nfe) {
					logger.error("Error parsing long from table " + getName(), nfe);
				}
				
				if (pk != null) {
					if (suidSelected.contains(pk))
						addRowSelectionInterval(i, i);
					else if (suidUnselected.contains(pk))
						removeRowSelectionInterval(i, i);
				}
			}
		} finally {
			ignoreRowSelectionEvents = false;
		}
	}

	private void selectFromTable() {
		final TableModel model = this.getModel();
		
		if (model instanceof BrowserTableModel == false)
			return;

		final BrowserTableModel btModel = (BrowserTableModel) model;

		if (btModel.getViewMode() != BrowserTableModel.ViewMode.ALL)
			return;

		final CyTable table = btModel.getDataTable();
		final CyColumn pKey = table.getPrimaryKey();
		final String pKeyName = pKey.getName();

		final int[] rowsSelected = getSelectedRows();
		
		if (rowsSelected.length == 0)
			return;

		final int selectedRowCount = getSelectedRowCount();
		final Set<CyRow> targetRows = new HashSet<>();
		
		for (int i = 0; i < selectedRowCount; i++) {
			// getting the row from data table solves the problem with hidden or
			// moved SUID column. However, since the rows might be sorted we
			// need to convert the index to model
			final ValidatedObjectAndEditString selected = (ValidatedObjectAndEditString) btModel.getValueAt(
					convertRowIndexToModel(rowsSelected[i]), pKeyName);
			targetRows.add(btModel.getRow(selected.getValidatedObject()));
		}

		// Clear selection for non-global table
		final CyTableManager tableManager = serviceRegistrar.getService(CyTableManager.class);
		
		if (tableManager.getGlobalTables().contains(table) == false) {
			List<CyRow> allRows = btModel.getDataTable().getAllRows();
			
			try {
				ignoreRowSetEvents = true;
				
				for (CyRow row : allRows) {
					final Boolean val = row.get(CyNetwork.SELECTED, Boolean.class);
					
					if (targetRows.contains(row)) {
						row.set(CyNetwork.SELECTED, true);
						continue;
					}
	
					if (val != null && (val == true))
						row.set(CyNetwork.SELECTED, false);
				}
				
				final CyApplicationManager applicationManager = serviceRegistrar.getService(CyApplicationManager.class);
				final CyNetworkView curView = applicationManager.getCurrentNetworkView();
				
				if (curView != null) {
					final CyEventHelper eventHelper = serviceRegistrar.getService(CyEventHelper.class);
					eventHelper.flushPayloadEvents();
					curView.updateView();
				}
			} finally {
				ignoreRowSetEvents = false;
			}
			
			repaint();
		}
	}
	
	// ==[ CLASSES ]====================================================================================================
	
	private class ProxyMouseListener extends MouseAdapter {
		
		private final MouseListener listener;
		
		public ProxyMouseListener(final MouseListener listener) {
			this.listener = listener;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// In order to prevent selected rows from being deselected when the user
			// CONTROL-clicks one of those rows on Mac.
			if (listener instanceof BrowserTable || !e.isPopupTrigger())
				listener.mouseClicked(e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			listener.mouseEntered(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			listener.mouseExited(e);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// In order to prevent selected rows from being deselected when the user
			// CONTROL-clicks one of those rows on Mac.
			if (listener instanceof BrowserTable || !e.isPopupTrigger())
				listener.mousePressed(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			listener.mouseReleased(e);
		}
	}
	
	private class BrowserTableListSelectionModel extends DefaultListSelectionModel {
		
		private static final long serialVersionUID = 7119443698611148406L;
		
		private int lastSelectedRow = -1;
		
		int getLastSelectedRow() {
			return lastSelectedRow;
		}
		
		public void setLastSelectedRow(int lastSelectedRow) {
			this.lastSelectedRow = lastSelectedRow;
		}
	}
	
	private class CellEditorRemover implements PropertyChangeListener {
		
		private final KeyboardFocusManager focusManager;
		private CyRow cyRow;
		private String columnName;

		public CellEditorRemover(final KeyboardFocusManager fm) {
			this.focusManager = fm;
		}

		@Override
		public void propertyChange(PropertyChangeEvent ev) {
			if (!isEditing()) {
				return;
			}

			Component c = focusManager.getPermanentFocusOwner();

			while (c != null) {
				if (c == BrowserTable.this) {
					// focus remains inside the table
					return;
				} else if (c instanceof Window) {
					if (c == SwingUtilities.getRoot(BrowserTable.this)) {

						try {
							getCellEditor().stopCellEditing();
						} catch (Exception e) {
							getCellEditor().cancelCellEditing();
							// Update the cell data based on the remembered
							// value
							updateAttributeAfterCellLostFocus();
						}
					}

					break;
				}

				c = c.getParent();
			}
		}

		/**
		 *  Cell data passed from previous TableModel, because tableModel will disappear if
		 *  user click on open space on canvas, so we have to remember it before it is gone.
		 * @param row
		 * @param columnName
		 */
		public void setCellData(CyRow row, String columnName) {
			this.cyRow = row;
			this.columnName = columnName;
		}

		private void updateAttributeAfterCellLostFocus() {
			final List<?> parsedData = TableBrowserUtil.parseCellInput(cyRow.getTable(), columnName,
					MultiLineTableCellEditor.lastValueUserEntered);

			if (parsedData.get(0) != null) {
				cyRow.set(columnName, MultiLineTableCellEditor.lastValueUserEntered);
			} else {
				// Error
				// discard the change
			}
		}
	}
	
	private static class BrowserTableTransferHandler extends TransferHandler {
		
		private static final long serialVersionUID = -4096856015305489834L;

		@Override
		protected Transferable createTransferable(JComponent source) {
			// Encode cell data in Excel format so we can copy/paste list attributes as multi-line cells.
			StringBuilder builder = new StringBuilder();
			BrowserTable table = (BrowserTable) source;
			
			for (int rowIndex : table.getSelectedRows()) {
				boolean firstColumn = true;
				
				for (int columnIndex : table.getSelectedColumns()) {
					if (!firstColumn) {
						builder.append(CELL_BREAK);
					} else {
						firstColumn = false;
					}
					
					Object object = table.getValueAt(rowIndex, columnIndex);
					
					if (object instanceof ValidatedObjectAndEditString) {
						ValidatedObjectAndEditString raw = (ValidatedObjectAndEditString) object;
						Object validatedObject = raw.getValidatedObject();
						
						if (validatedObject instanceof Collection) {
							builder.append("\"");
							boolean firstRow = true;
							
							for (Object member : (Collection<?>) validatedObject) {
								if (!firstRow) {
									builder.append("\r");
								} else {
									firstRow = false;
								}
								builder.append(member.toString().replaceAll("\"", "\"\""));
							}
							builder.append("\"");
						} else {
							builder.append(validatedObject.toString());
						}
					} else {
						if (object != null) {
							builder.append(object.toString());
						}
					}
				}
				builder.append(LINE_BREAK);
			}
			
			return new StringSelection(builder.toString());
		}
		
		@Override
		public int getSourceActions(JComponent c) {
			return TransferHandler.COPY;
		}
	}
}
