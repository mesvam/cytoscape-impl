package org.cytoscape.cg.internal.charts;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;
import static org.cytoscape.cg.internal.charts.AbstractChart.AUTO_RANGE;
import static org.cytoscape.cg.internal.charts.AbstractChart.AXIS_COLOR;
import static org.cytoscape.cg.internal.charts.AbstractChart.AXIS_LABEL_FONT_SIZE;
import static org.cytoscape.cg.internal.charts.AbstractChart.AXIS_WIDTH;
import static org.cytoscape.cg.internal.charts.AbstractChart.BORDER_COLOR;
import static org.cytoscape.cg.internal.charts.AbstractChart.BORDER_WIDTH;
import static org.cytoscape.cg.internal.charts.AbstractChart.DATA_COLUMNS;
import static org.cytoscape.cg.internal.charts.AbstractChart.DOMAIN_LABELS_COLUMN;
import static org.cytoscape.cg.internal.charts.AbstractChart.DOMAIN_LABEL_POSITION;
import static org.cytoscape.cg.internal.charts.AbstractChart.GLOBAL_RANGE;
import static org.cytoscape.cg.internal.charts.AbstractChart.ITEM_LABELS_COLUMN;
import static org.cytoscape.cg.internal.charts.AbstractChart.ITEM_LABEL_FONT_SIZE;
import static org.cytoscape.cg.internal.charts.AbstractChart.RANGE;
import static org.cytoscape.cg.internal.charts.AbstractChart.RANGE_LABELS_COLUMN;
import static org.cytoscape.cg.internal.charts.AbstractChart.SHOW_DOMAIN_AXIS;
import static org.cytoscape.cg.internal.charts.AbstractChart.SHOW_ITEM_LABELS;
import static org.cytoscape.cg.internal.charts.AbstractChart.SHOW_RANGE_AXIS;
import static org.cytoscape.cg.internal.charts.AbstractChart.SHOW_RANGE_ZERO_BASELINE;
import static org.cytoscape.cg.model.AbstractCustomGraphics2.ORIENTATION;
import static org.cytoscape.util.swing.LookAndFeelUtil.isAquaLAF;
import static org.cytoscape.util.swing.LookAndFeelUtil.isWinLAF;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cg.internal.util.SortedListModel;
import org.cytoscape.cg.internal.util.SortedListModel.SortOrder;
import org.cytoscape.cg.internal.util.ViewUtil;
import org.cytoscape.cg.model.AbstractCustomGraphics2;
import org.cytoscape.cg.model.Orientation;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.color.BrewerType;
import org.cytoscape.util.color.PaletteType;
import org.cytoscape.util.swing.BasicCollapsiblePanel;
import org.cytoscape.util.swing.BasicCollapsiblePanel.CollapseListener;
import org.cytoscape.util.swing.ColorButton;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.view.presentation.property.values.CyColumnIdentifier;
import org.cytoscape.view.presentation.property.values.CyColumnIdentifierFactory;

@SuppressWarnings("serial")
public abstract class AbstractChartEditor<T extends AbstractCustomGraphics2<?>> extends JPanel {

	protected static Double[] ANGLES = new Double[] { 0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0 };
	
	private BasicCollapsiblePanel basicOptionsPnl;
	private BasicCollapsiblePanel advancedOptionsPnl;
	private DataPanel dataPnl;
	private JPanel rangePnl;
	private JPanel labelsPnl;
	private JPanel orientationPnl;
	private JPanel axesPnl;
	private JPanel borderPnl;
	protected ColorSchemeEditor<T> colorSchemeEditor;
	private JPanel otherBasicOptionsPnl;
	private JPanel otherAdvancedOptionsPnl;
	protected JLabel itemLabelsColumnLbl;
	protected JLabel domainLabelsColumnLbl;
	protected JLabel rangeLabelsColumnLbl;
	private JComboBox<CyColumnIdentifier> itemLabelsColumnCmb;
	private JComboBox<CyColumnIdentifier> domainLabelsColumnCmb;
	private JComboBox<CyColumnIdentifier> rangeLabelsColumnCmb;
	protected JLabel domainLabelPositionLbl;
	protected JComboBox<LabelPosition> domainLabelPositionCmb;
	private JCheckBox globalRangeCkb;
	private JCheckBox autoRangeCkb;
	private JLabel rangeMinLbl;
	private JTextField rangeMinTxt;
	private JButton refreshRangeBtn;
	private JLabel rangeMaxLbl;
	private JTextField rangeMaxTxt;
	private JCheckBox itemLabelsVisibleCkb;
	private JCheckBox domainAxisVisibleCkb;
	private JCheckBox rangeAxisVisibleCkb;
	private JCheckBox rangeZeroBaselineVisibleCkb;
	private JLabel itemFontSizeLbl;
	private JTextField itemFontSizeTxt;
	private JLabel axisWidthLbl;
	private JTextField axisWidthTxt;
	private JLabel axisColorLbl;
	private JLabel axisFontSizeLbl;
	private JTextField axisFontSizeTxt;
	private ColorButton axisColorBtn;
	private ButtonGroup orientationGrp;
	private JRadioButton verticalRd;
	private JRadioButton horizontalRd;
	private JLabel borderWidthLbl;
	private JTextField borderWidthTxt;
	private JLabel borderColorLbl;
	private ColorButton borderColorBtn;
	
	protected final boolean columnIsSeries;
	protected final boolean setRange;
	protected final boolean setOrientation;
	protected final boolean setItemLabels;
	protected final boolean setDomainLabels;
	protected final boolean setRangeLabels;
	protected final boolean hasAxes;
	protected final boolean hasZeroBaseline;
	protected final Map<CyColumnIdentifier, CyColumn> columns;
	protected final Map<CyColumnIdentifier, CyColumn> labelColumns;
	protected final T chart;
	protected final Class<?> dataType;
	
	protected Class<? extends CyIdentifiable> targetType;
	protected boolean initializing;
	
	protected final CyServiceRegistrar serviceRegistrar;

	// ==[ CONSTRUCTORS ]===============================================================================================
	
	protected AbstractChartEditor(
			T chart, 
			Class<?> dataType, 
			boolean columnIsSeries, 
			boolean setRange,
			boolean setOrientation, 
			boolean setItemLabels, 
			boolean setDomainLabels, 
			boolean setRangeLabels,
			boolean hasAxes, 
			boolean hasZeroBaseline, 
			CyServiceRegistrar serviceRegistrar
	) {
		if (chart == null)
			throw new IllegalArgumentException("'chart' argument must not be null.");
		if (dataType == null)
			throw new IllegalArgumentException("'dataType' argument must not be null.");
		if (serviceRegistrar == null)
			throw new IllegalArgumentException("'serviceRegistrar' argument must not be null.");
		
		this.chart = chart;
		this.columnIsSeries = columnIsSeries;
		this.dataType = dataType;
		this.setRange = setRange;
		this.setOrientation = setOrientation;
		this.setItemLabels = setItemLabels;
		this.setDomainLabels = setDomainLabels;
		this.setRangeLabels = setRangeLabels;
		this.hasAxes = hasAxes;
		this.hasZeroBaseline = hasZeroBaseline;
		this.serviceRegistrar = serviceRegistrar;
		
		var columnComparator = new ColumnComparator();
		columns = new TreeMap<>(columnComparator);
		labelColumns = new TreeMap<>(columnComparator);
		
		// TODO Move it to a shared "Chart Column Manager"
		var net = serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetwork();
		
		if (net != null) {
			var table = net.getDefaultNodeTable(); // TODO only node table for now, but may get edge table in the future
			var cols = table.getColumns();
			var colIdFactory = serviceRegistrar.getService(CyColumnIdentifierFactory.class);
			
			for (var c : cols) {
				if (c.getName() != CyIdentifiable.SUID) {
					var colId = colIdFactory.createColumnIdentifier(c.getName());
					columns.put(colId, c);
					
					if (List.class.isAssignableFrom(c.getType()))
						labelColumns.put(colId, c);
				}
			}
		}
		
		init();
	}
	
	// ==[ PUBLIC METHODS ]=============================================================================================
	
	public void setTargetType(Class<? extends CyIdentifiable> targetType) {
		this.targetType = targetType;
		updateOptions();
	}

	// ==[ PRIVATE METHODS ]============================================================================================
	
	protected void init() {
		initializing = true;
		
		try {
			createLabels();
			setOpaque(!isAquaLAF()); // Transparent if Aqua
			
			var layout = new GroupLayout(this);
			setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(!isAquaLAF());
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addComponent(getBasicOptionsPnl(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(getAdvancedOptionsPnl(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(getBasicOptionsPnl(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(getAdvancedOptionsPnl(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
			
			ViewUtil.recursiveDo(this, c -> LookAndFeelUtil.makeSmall(c));
		} finally {
			initializing = false;
		}
		
		update(false);
	}
	
	protected void createLabels() {
		itemLabelsColumnLbl = new JLabel("Column:");
		itemFontSizeLbl = new JLabel("Font Size:");
		domainLabelsColumnLbl = new JLabel("Domain Labels Column:");
		rangeLabelsColumnLbl = new JLabel("Range Labels Column:");
		domainLabelPositionLbl = new JLabel("Domain Label Position:");
		rangeMinLbl = new JLabel("Min:");
		rangeMaxLbl = new JLabel("Max:");
		axisWidthLbl = new JLabel("Axis Width:");
		axisColorLbl = new JLabel("Axis Color:");
		axisFontSizeLbl = new JLabel("Axis Font Size:");
		borderWidthLbl = new JLabel("Border Width:");
		borderColorLbl = new JLabel("Border Color:");
	}

	protected BasicCollapsiblePanel getBasicOptionsPnl() {
		if (basicOptionsPnl == null) {
			basicOptionsPnl = new BasicCollapsiblePanel("Setup");
			basicOptionsPnl.setCollapsed(false);
			basicOptionsPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			
			var layout = new GroupLayout(basicOptionsPnl.getContentPane());
			basicOptionsPnl.getContentPane().setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(!isAquaLAF());
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addComponent(getOtherBasicOptionsPnl())
					.addComponent(getDataPnl())
					.addComponent(getRangePnl())
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(getOtherBasicOptionsPnl(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(getDataPnl(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addPreferredGap(UNRELATED)
					.addComponent(getRangePnl(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
			
			// Behaves like an accordion
			basicOptionsPnl.addCollapseListener(new CollapseListener() {
				@Override
				public void expanded() {
					getAdvancedOptionsPnl().setCollapsed(true);
				}
				@Override
				public void collapsed() {
					// Nothing to do here...
				}
			});
		}
		
		return basicOptionsPnl;
	}
	
	protected BasicCollapsiblePanel getAdvancedOptionsPnl() {
		if (advancedOptionsPnl == null) {
			advancedOptionsPnl = new BasicCollapsiblePanel("Customize");
			advancedOptionsPnl.setCollapsed(true);
			advancedOptionsPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			
			var layout = new GroupLayout(advancedOptionsPnl.getContentPane());
			advancedOptionsPnl.getContentPane().setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(!isAquaLAF());
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addComponent(getColorSchemeEditor())
					.addComponent(getLabelsPnl())
					.addComponent(getOrientationPnl())
					.addComponent(getAxesPnl())
					.addComponent(getBorderPnl())
					.addComponent(getOtherAdvancedOptionsPnl())
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(getColorSchemeEditor())
					.addComponent(getLabelsPnl())
					.addComponent(getOrientationPnl())
					.addComponent(getAxesPnl())
					.addComponent(getBorderPnl())
					.addComponent(getOtherAdvancedOptionsPnl())
			);
			
			// Behaves like an accordion
			advancedOptionsPnl.addCollapseListener(new CollapseListener() {
				@Override
				public void expanded() {
					getBasicOptionsPnl().setCollapsed(true);
				}
				@Override
				public void collapsed() {
					// Nothing to do here...
				}
			});
		}
		
		return advancedOptionsPnl;
	}
	
	protected DataPanel getDataPnl() {
		if (dataPnl == null) {
			dataPnl = new DataPanel();
			dataPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			dataPnl.refresh();
		}
		
		return dataPnl;
	}
	
	protected JPanel getRangePnl() {
		if (rangePnl == null) {
			rangePnl = new JPanel();
			rangePnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			rangePnl.setVisible(setRange);
			
			if (!rangePnl.isVisible())
				return rangePnl;
			
			var layout = new GroupLayout(rangePnl);
			rangePnl.setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(!isAquaLAF());
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addComponent(getGlobalRangeCkb())
					.addGroup(layout.createSequentialGroup()
							.addComponent(getAutoRangeCkb())
							.addPreferredGap(UNRELATED)
							.addComponent(rangeMinLbl)
							.addComponent(getRangeMinTxt())
							.addComponent(rangeMaxLbl)
							.addComponent(getRangeMaxTxt())
							.addComponent(getRefreshRangeBtn())
					)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(getGlobalRangeCkb())
					.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
							.addComponent(getAutoRangeCkb(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(rangeMinLbl)
							.addComponent(getRangeMinTxt(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(rangeMaxLbl)
							.addComponent(getRangeMaxTxt(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(getRefreshRangeBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
			);
		}
		
		return rangePnl;
	}
	
	protected JPanel getLabelsPnl() {
		if (labelsPnl == null) {
			labelsPnl = new JPanel();
			labelsPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			
			if (!labelsPnl.isVisible())
				return labelsPnl;
			
			var layout = new GroupLayout(labelsPnl);
			labelsPnl.setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(!isAquaLAF());
			
			var hGroup = layout.createParallelGroup(Alignment.LEADING, true);
			var vGroup = layout.createSequentialGroup();
			layout.setHorizontalGroup(hGroup);
			layout.setVerticalGroup(vGroup);
			
			if (setItemLabels) {
				hGroup.addGroup(layout.createSequentialGroup()
						.addComponent(getItemLabelsVisibleCkb())
						.addPreferredGap(UNRELATED)
						.addComponent(itemLabelsColumnLbl)
						.addComponent(getItemLabelsColumnCmb(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
						.addPreferredGap(UNRELATED)
						.addComponent(itemFontSizeLbl)
						.addComponent(getItemFontSizeTxt(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE));
				vGroup.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
						.addComponent(getItemLabelsVisibleCkb())
						.addComponent(itemLabelsColumnLbl)
						.addComponent(getItemLabelsColumnCmb())
						.addComponent(itemFontSizeLbl)
						.addComponent(getItemFontSizeTxt()));
			}
			
			if (setRangeLabels) {
				hGroup.addGroup(layout.createSequentialGroup()
						.addComponent(rangeLabelsColumnLbl)
						.addComponent(getRangeLabelsColumnCmb(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE));
				vGroup.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
						.addComponent(rangeLabelsColumnLbl)
						.addComponent(getRangeLabelsColumnCmb()));
					
			}
			
			if (setDomainLabels) {
				hGroup.addGroup(layout.createSequentialGroup()
						.addComponent(domainLabelsColumnLbl)
						.addComponent(getDomainLabelsColumnCmb(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE));
				vGroup.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
						.addComponent(domainLabelsColumnLbl)
						.addComponent(getDomainLabelsColumnCmb()));
			}
			
			hGroup.addGroup(layout.createSequentialGroup()
					.addComponent(domainLabelPositionLbl)
					.addComponent(getDomainLabelPositionCmb(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE));
			vGroup.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
					.addComponent(domainLabelPositionLbl)
					.addComponent(getDomainLabelPositionCmb()));
			
			var sep = new JSeparator();
			
			hGroup.addComponent(sep);
			vGroup.addComponent(sep, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE);
		}
		
		return labelsPnl;
	}
	
	protected JPanel getOrientationPnl() {
		if (orientationPnl == null) {
			orientationPnl = new JPanel();
			orientationPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			
			var layout = new GroupLayout(orientationPnl);
			orientationPnl.setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(!isAquaLAF());
			
			var sep = new JSeparator();
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addGroup(layout.createSequentialGroup()
							.addComponent(getVerticalRd())
							.addComponent(getHorizontalRd())
					).addComponent(sep)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
							.addComponent(getVerticalRd())
							.addComponent(getHorizontalRd())
					).addComponent(sep, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
		}
		
		return orientationPnl;
	}
	
	protected JPanel getAxesPnl() {
		if (axesPnl == null) {
			axesPnl = new JPanel();
			axesPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			
			var layout = new GroupLayout(axesPnl);
			axesPnl.setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(!isAquaLAF());
			
			var vsep = new JSeparator(JSeparator.VERTICAL);
			var sep = new JSeparator();
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(Alignment.LEADING, true)
							.addComponent(getDomainAxisVisibleCkb())
							.addComponent(getRangeAxisVisibleCkb())
							.addComponent(getRangeZeroBaselineVisibleCkb())
						)
						.addPreferredGap(UNRELATED)
						.addComponent(vsep, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
						.addPreferredGap(UNRELATED)
						.addGroup(layout.createParallelGroup(Alignment.LEADING, true)
							.addGroup(layout.createSequentialGroup()
								.addComponent(axisWidthLbl)
								.addComponent(getAxisWidthTxt(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							)
							.addGroup(layout.createSequentialGroup()
								.addComponent(axisColorLbl)
								.addComponent(getAxisColorBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							).addGroup(layout.createSequentialGroup()
								.addComponent(axisFontSizeLbl)
								.addComponent(getAxisFontSizeTxt(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							)
						)
					)
					.addComponent(sep)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
						.addGroup(layout.createSequentialGroup()
							.addComponent(getDomainAxisVisibleCkb())
							.addComponent(getRangeAxisVisibleCkb())
							.addComponent(getRangeZeroBaselineVisibleCkb())
						)
						.addComponent(vsep)
						.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
								.addComponent(axisWidthLbl)
								.addComponent(getAxisWidthTxt(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							)
							.addGroup(layout.createParallelGroup(Alignment.CENTER, true)
								.addComponent(axisColorLbl)
								.addComponent(getAxisColorBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							)
							.addGroup(layout.createParallelGroup(Alignment.CENTER, true)
									.addComponent(axisFontSizeLbl)
									.addComponent(getAxisFontSizeTxt(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							)
						)
					)
					.addComponent(sep, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
		}
		
		return axesPnl;
	}
	
	protected JPanel getBorderPnl() {
		if (borderPnl == null) {
			borderPnl = new JPanel();
			borderPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			
			var layout = new GroupLayout(borderPnl);
			borderPnl.setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(!isAquaLAF());
			
			var sep = new JSeparator();
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addGroup(layout.createSequentialGroup()
						.addComponent(borderWidthLbl)
						.addComponent(getBorderWidthTxt(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
						.addPreferredGap(UNRELATED)
						.addComponent(borderColorLbl)
						.addComponent(getBorderColorBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
					.addComponent(sep)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
						.addComponent(borderWidthLbl)
						.addComponent(getBorderWidthTxt())
						.addComponent(borderColorLbl)
						.addComponent(getBorderColorBtn())
					)
					.addComponent(sep, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
		}
		
		return borderPnl;
	}
	
	protected ColorSchemeEditor<T> getColorSchemeEditor() {
		if (colorSchemeEditor == null) {
			colorSchemeEditor = new ColorSchemeEditor<>(
					chart,
					columnIsSeries,
					getDefaultPaletteType(),
					getDefaultPaletteName(),
					serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetwork(),
					serviceRegistrar
			);
		}
		
		return colorSchemeEditor;
	}
	
	/**
	 * Should be overridden by the concrete subclass if it provides extra fields.
	 * @return
	 */
	protected JPanel getOtherBasicOptionsPnl() {
		if (otherBasicOptionsPnl == null) {
			otherBasicOptionsPnl = new JPanel();
			otherBasicOptionsPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			otherBasicOptionsPnl.setVisible(false);
		}
		
		return otherBasicOptionsPnl;
	}
	
	/**
	 * Should be overridden by the concrete subclass if it provides extra fields.
	 * @return
	 */
	protected JPanel getOtherAdvancedOptionsPnl() {
		if (otherAdvancedOptionsPnl == null) {
			otherAdvancedOptionsPnl = new JPanel();
			otherAdvancedOptionsPnl.setOpaque(!isAquaLAF()); // Transparent if Aqua
			otherAdvancedOptionsPnl.setVisible(false);
		}
		
		return otherAdvancedOptionsPnl;
	}
	
	protected JComboBox<CyColumnIdentifier> getItemLabelsColumnCmb() {
		if (itemLabelsColumnCmb == null) {
			itemLabelsColumnCmb = new CyColumnComboBox(labelColumns.keySet(), true);
			selectColumnIdItem(itemLabelsColumnCmb, chart.get(ITEM_LABELS_COLUMN, CyColumnIdentifier.class));
			
			itemLabelsColumnCmb.addActionListener(evt -> {
				var colId = (CyColumnIdentifier) itemLabelsColumnCmb.getSelectedItem();
				chart.set(ITEM_LABELS_COLUMN, colId != null ? colId.getColumnName() : null);
			});
		}

		return itemLabelsColumnCmb;
	}
	
	protected JComboBox<CyColumnIdentifier> getDomainLabelsColumnCmb() {
		if (domainLabelsColumnCmb == null) {
			domainLabelsColumnCmb = new CyColumnComboBox(labelColumns.keySet(), true);
			selectColumnIdItem(domainLabelsColumnCmb, chart.get(DOMAIN_LABELS_COLUMN, CyColumnIdentifier.class));
			
			domainLabelsColumnCmb.addActionListener(evt -> {
				var colId = (CyColumnIdentifier) domainLabelsColumnCmb.getSelectedItem();
				chart.set(DOMAIN_LABELS_COLUMN, colId != null ? colId.getColumnName() : null);
			});
		}
		
		return domainLabelsColumnCmb;
	}
	
	protected JComboBox<CyColumnIdentifier> getRangeLabelsColumnCmb() {
		if (rangeLabelsColumnCmb == null) {
			rangeLabelsColumnCmb = new CyColumnComboBox(labelColumns.keySet(), true);
			selectColumnIdItem(rangeLabelsColumnCmb, chart.get(RANGE_LABELS_COLUMN, CyColumnIdentifier.class));
			
			rangeLabelsColumnCmb.addActionListener(evt -> {
				var colId = (CyColumnIdentifier) rangeLabelsColumnCmb.getSelectedItem();
				chart.set(RANGE_LABELS_COLUMN, colId != null ? colId.getColumnName() : null);
			});
		}
		
		return rangeLabelsColumnCmb;
	}
	
	protected JCheckBox getGlobalRangeCkb() {
		if (globalRangeCkb == null) {
			globalRangeCkb = new JCheckBox("Same Value Range for All Charts");
			globalRangeCkb.setToolTipText("Use the same min/max values for all charts");
			globalRangeCkb.setSelected(chart.get(GLOBAL_RANGE, Boolean.class, Boolean.TRUE));
			globalRangeCkb.addItemListener(evt -> {
				var selected = evt.getStateChange() == ItemEvent.SELECTED;
				chart.set(GLOBAL_RANGE, selected);
				updateGlobalRange();
				
				if (selected)
					updateRangeMinMax(chart.getList(RANGE, Double.class).isEmpty());
			});
		}
		
		return globalRangeCkb;
	}
	
	protected JCheckBox getAutoRangeCkb() {
		if (autoRangeCkb == null) {
			autoRangeCkb = new JCheckBox("Automatic Range");
			autoRangeCkb.setSelected(chart.get(AUTO_RANGE, Boolean.class, Boolean.TRUE));
			autoRangeCkb.addItemListener(evt -> {
				var selected = evt.getStateChange() == ItemEvent.SELECTED;
				getRangeMinTxt().setEnabled(!selected);
				getRangeMaxTxt().setEnabled(!selected);
				getRangeMinTxt().requestFocus();
				chart.set(AUTO_RANGE, selected);
				
				if (selected)
					updateRangeMinMax(true);
			});
		}
		
		return autoRangeCkb;
	}
	
	protected JTextField getRangeMinTxt() {
		if (rangeMinTxt == null) {
			rangeMinTxt = new JTextField();
			var auto = chart.get(AUTO_RANGE, Boolean.class, Boolean.TRUE);
			rangeMinTxt.setEnabled(!auto);
			rangeMinTxt.setInputVerifier(new DoubleInputVerifier());
			rangeMinTxt.setMinimumSize(new Dimension(60, rangeMinTxt.getMinimumSize().height));
			rangeMinTxt.setHorizontalAlignment(JTextField.TRAILING);
			
			rangeMinTxt.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent evt) {
					setGlobalRange();
				}
			});
		}
		
		return rangeMinTxt;
	}
	
	protected JTextField getRangeMaxTxt() {
		if (rangeMaxTxt == null) {
			rangeMaxTxt = new JTextField();
			var auto = chart.get(AUTO_RANGE, Boolean.class, Boolean.TRUE);
			rangeMaxTxt.setEnabled(!auto);
			rangeMaxTxt.setInputVerifier(new DoubleInputVerifier());
			rangeMaxTxt.setMinimumSize(new Dimension(60, rangeMaxTxt.getMinimumSize().height));
			rangeMaxTxt.setHorizontalAlignment(JTextField.TRAILING);
			
			rangeMaxTxt.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent evt) {
					setGlobalRange();
				}
			});
		}
		
		return rangeMaxTxt;
	}
	
	protected JButton getRefreshRangeBtn() {
		if (refreshRangeBtn == null) {
			refreshRangeBtn = new JButton(IconManager.ICON_REFRESH);
			refreshRangeBtn.setFont(serviceRegistrar.getService(IconManager.class).getIconFont(12.0f));
			refreshRangeBtn.setToolTipText("Refresh automatic range values");
			
			refreshRangeBtn.addActionListener(evt -> {
				updateRangeMinMax(true);
				refreshRangeBtn.setEnabled(false);
			});
		}
		
		return refreshRangeBtn;
	}
	
	protected JCheckBox getItemLabelsVisibleCkb() {
		if (itemLabelsVisibleCkb == null) {
			itemLabelsVisibleCkb = new JCheckBox("Show Value Labels");
			itemLabelsVisibleCkb.setVisible(setItemLabels);
			
			if (setItemLabels) {
				itemLabelsVisibleCkb.setSelected(chart.get(SHOW_ITEM_LABELS, Boolean.class, false));
				itemLabelsVisibleCkb.addActionListener(evt -> {
					chart.set(SHOW_ITEM_LABELS, itemLabelsVisibleCkb.isSelected());
					
					itemLabelsColumnLbl.setEnabled(itemLabelsVisibleCkb.isSelected());
					getItemLabelsColumnCmb().setEnabled(itemLabelsVisibleCkb.isSelected());
					
					itemFontSizeLbl.setEnabled(itemLabelsVisibleCkb.isSelected());
					getItemFontSizeTxt().setEnabled(itemLabelsVisibleCkb.isSelected());
				});
			}
		}
		
		return itemLabelsVisibleCkb;
	}
	
	protected JCheckBox getDomainAxisVisibleCkb() {
		if (domainAxisVisibleCkb == null) {
			domainAxisVisibleCkb = new JCheckBox("Show Domain Axis");
			domainAxisVisibleCkb.setVisible(hasAxes);
			
			if (hasAxes) {
				domainAxisVisibleCkb.setSelected(chart.get(SHOW_DOMAIN_AXIS, Boolean.class, false));
				domainAxisVisibleCkb.addActionListener(evt -> {
					chart.set(SHOW_DOMAIN_AXIS, domainAxisVisibleCkb.isSelected());
				});
			}
		}
		
		return domainAxisVisibleCkb;
	}
	
	protected JCheckBox getRangeAxisVisibleCkb() {
		if (rangeAxisVisibleCkb == null) {
			rangeAxisVisibleCkb = new JCheckBox("Show Range Axis");
			rangeAxisVisibleCkb.setVisible(hasAxes);
			
			if (hasAxes) {
				rangeAxisVisibleCkb.setSelected(chart.get(SHOW_RANGE_AXIS, Boolean.class, false));
				rangeAxisVisibleCkb.addActionListener(evt -> {
					chart.set(SHOW_RANGE_AXIS, rangeAxisVisibleCkb.isSelected());
				});
			}
		}
		
		return rangeAxisVisibleCkb;
	}
	
	protected JCheckBox getRangeZeroBaselineVisibleCkb() {
		if (rangeZeroBaselineVisibleCkb == null) {
			rangeZeroBaselineVisibleCkb = new JCheckBox("Show Zero Baseline");
			rangeZeroBaselineVisibleCkb.setVisible(hasZeroBaseline);
			
			if (hasAxes) {
				rangeZeroBaselineVisibleCkb.setSelected(chart.get(SHOW_RANGE_ZERO_BASELINE, Boolean.class, false));
				rangeZeroBaselineVisibleCkb.addActionListener(evt -> {
					chart.set(SHOW_RANGE_ZERO_BASELINE, rangeZeroBaselineVisibleCkb.isSelected());
				});
			}
		}
		
		return rangeZeroBaselineVisibleCkb;
	}
	
	public JComboBox<LabelPosition> getDomainLabelPositionCmb() {
		if (domainLabelPositionCmb == null) {
			domainLabelPositionCmb = new JComboBox<>(LabelPosition.values());
			
			domainLabelPositionCmb.setRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index,
						boolean isSelected, boolean cellHasFocus) {
					var lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					
					if (value instanceof LabelPosition)
						lbl.setText(((LabelPosition) value).getLabel());

					return lbl;
				}
			});
			
			domainLabelPositionCmb.setSelectedItem(
					chart.get(DOMAIN_LABEL_POSITION, LabelPosition.class, LabelPosition.STANDARD));
			
			domainLabelPositionCmb.addActionListener(evt -> {
				var position = (LabelPosition) domainLabelPositionCmb.getSelectedItem();
				chart.set(DOMAIN_LABEL_POSITION, position);
			});
		}
		
		return domainLabelPositionCmb;
	}
	
	protected JTextField getAxisWidthTxt() {
		if (axisWidthTxt == null) {
			axisWidthTxt = new JTextField("" + chart.get(AXIS_WIDTH, Float.class, 0.25f));
			axisWidthTxt.setInputVerifier(new DoubleInputVerifier());
			axisWidthTxt.setPreferredSize(new Dimension(60, axisWidthTxt.getMinimumSize().height));
			axisWidthTxt.setHorizontalAlignment(JTextField.TRAILING);
			
			axisWidthTxt.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent evt) {
					try {
						float v = Float.parseFloat(axisWidthTxt.getText());
			            chart.set(AXIS_WIDTH, v);
			        } catch (NumberFormatException nfe) {
			        }
				}
			});
		}
		
		return axisWidthTxt;
	}
	
	protected ColorButton getAxisColorBtn() {
		if (axisColorBtn == null) {
			var color = chart.get(AXIS_COLOR, Color.class, Color.DARK_GRAY);
			axisColorBtn = new ColorButton(serviceRegistrar, null, BrewerType.ANY, color, false);
			axisColorBtn.setVisible(hasAxes);
			
			axisColorBtn.addPropertyChangeListener("color", evt -> {
				var newColor = (Color) evt.getNewValue();
				chart.set(AXIS_COLOR, newColor);
			});
		}
		
		return axisColorBtn;
	}
	
	protected JTextField getItemFontSizeTxt() {
		if (itemFontSizeTxt == null) {
			itemFontSizeTxt = new JTextField("" + chart.get(ITEM_LABEL_FONT_SIZE, Integer.class, 1));
			itemFontSizeTxt.setInputVerifier(new IntInputVerifier());
			itemFontSizeTxt.setPreferredSize(new Dimension(40, itemFontSizeTxt.getMinimumSize().height));
			itemFontSizeTxt.setHorizontalAlignment(JTextField.TRAILING);
			
			itemFontSizeTxt.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent evt) {
					try {
						int v = Integer.parseInt(itemFontSizeTxt.getText());
			            chart.set(ITEM_LABEL_FONT_SIZE, v);
			        } catch (NumberFormatException nfe) {
			        }
				}
			});
		}
		
 		return itemFontSizeTxt;
	}
	
	protected JTextField getAxisFontSizeTxt() {
		if (axisFontSizeTxt == null) {
			axisFontSizeTxt = new JTextField("" + chart.get(AXIS_LABEL_FONT_SIZE, Integer.class, 1));
			axisFontSizeTxt.setInputVerifier(new IntInputVerifier());
			axisFontSizeTxt.setPreferredSize(new Dimension(60, axisFontSizeTxt.getMinimumSize().height));
			axisFontSizeTxt.setHorizontalAlignment(JTextField.TRAILING);
			
			axisFontSizeTxt.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent evt) {
					try {
						int v = Integer.parseInt(axisFontSizeTxt.getText());
			            chart.set(AXIS_LABEL_FONT_SIZE, v);
			        } catch (NumberFormatException nfe) {
			        }
				}
			});
		}
		
		return axisFontSizeTxt;
	}
	
	private ButtonGroup getOrientationGrp() {
		if (orientationGrp == null) {
			orientationGrp = new ButtonGroup();
			orientationGrp.add(getVerticalRd());
			orientationGrp.add(getHorizontalRd());
		}
		
		return orientationGrp;
	}
	
	protected JRadioButton getVerticalRd() {
		if (verticalRd == null) {
			verticalRd = new JRadioButton("Vertical Orientation");
			verticalRd.setVisible(setOrientation);
			
			if (setOrientation) {
				verticalRd.addActionListener(evt -> setOrientation());
			}
		}
		
		return verticalRd;
	}
	
	protected JRadioButton getHorizontalRd() {
		if (horizontalRd == null) {
			horizontalRd = new JRadioButton("Horizontal Orientation");
			horizontalRd.setVisible(setOrientation);
			
			if (setOrientation) {
				horizontalRd.addActionListener(evt -> setOrientation());
			}
		}
		
		return horizontalRd;
	}
	
	protected void setOrientation() {
		var orientation = getHorizontalRd().isSelected() ? Orientation.HORIZONTAL : Orientation.VERTICAL;
		chart.set(ORIENTATION, orientation);
	}
	
	protected JTextField getBorderWidthTxt() {
		if (borderWidthTxt == null) {
			borderWidthTxt = new JTextField("" + chart.get(BORDER_WIDTH, Float.class, 0.25f));
			borderWidthTxt.setInputVerifier(new DoubleInputVerifier());
			borderWidthTxt.setPreferredSize(new Dimension(60, borderWidthTxt.getMinimumSize().height));
			borderWidthTxt.setHorizontalAlignment(JTextField.TRAILING);
			
			borderWidthTxt.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent evt) {
					try {
						float v = Float.parseFloat(borderWidthTxt.getText());
			            chart.set(BORDER_WIDTH, v);
			        } catch (NumberFormatException nfe) {
			        }
				}
			});
		}
		
		return borderWidthTxt;
	}
	
	protected ColorButton getBorderColorBtn() {
		if (borderColorBtn == null) {
			var color = chart.get(BORDER_COLOR, Color.class, Color.DARK_GRAY);
			borderColorBtn = new ColorButton(serviceRegistrar, null, BrewerType.ANY, color, false);
			
			borderColorBtn.addPropertyChangeListener("color", evt -> {
				var newColor = (Color) evt.getNewValue();
				chart.set(BORDER_COLOR, newColor);
			});
		}
		
		return borderColorBtn;
	}
	
	@SuppressWarnings("unchecked")
	protected List<Double> calculateAutoRange() {
		var range = new ArrayList<Double>(2);
		var net = serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetwork();
		
		if (net != null) {
			var nodes = net.getNodeList();
			var dataColumns = getDataPnl().getDataColumns();
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			
			for (var colId : dataColumns) {
				var column = columns.get(colId);
				
				if (column == null)
					continue;
				
				var colType = column.getType();
				var colListType = column.getListElementType();
				
				if (Number.class.isAssignableFrom(colType) ||
						(List.class.isAssignableFrom(colType) && Number.class.isAssignableFrom(colListType))) {
					for (var n : nodes) {
						List<? extends Number> values = null;
						var row = net.getRow(n);
						
						if (List.class.isAssignableFrom(colType))
							values = (List<? extends Number>) row.getList(column.getName(), colListType);
						else if (row.isSet(column.getName()))
							values = Collections.singletonList((Number)row.get(column.getName(), colType));
						
						double[] mm = minMax(min, max, values);
						min = mm[0];
						max = mm[1];
					}
				}
			}
			
			if (min != Double.POSITIVE_INFINITY && max != Double.NEGATIVE_INFINITY) {
				range.add(min);
				range.add(max);
			}
		}
		
		return range;
	}
	
	protected double[] minMax(double min, double max, List<? extends Number> values) {
		if (values != null) {
			for (var v : values) {
				if (v != null) {
					double dv = v.doubleValue();
					min = Math.min(min, dv);
					max = Math.max(max, dv);
				}
			}
		}
		
		return new double[]{ min, max };
	}
	
	protected void update(boolean recalculateRange) {
		if (setOrientation)
			updateOrientation();
		
		updateGlobalRange();
		updateRangeMinMax(recalculateRange);
		updateItemLabel();
		updateOptions();
	}
	
	protected void updateOptions() {
		// Hide options that would just make table "sparklines" too cramped
		boolean sparklines = targetType == CyColumn.class;
		
		getLabelsPnl().setVisible(!sparklines && (setItemLabels || setDomainLabels || setRangeLabels));
		getOrientationPnl().setVisible(!sparklines && setOrientation);
		getAxesPnl().setVisible(!sparklines && hasAxes);
		getBorderPnl().setVisible(!sparklines);
		
		if (sparklines)
			chart.set(BORDER_WIDTH, 0.0f);
	}

	protected void updateOrientation() {
		var orientation = chart.get(ORIENTATION, Orientation.class, Orientation.VERTICAL);
		var orientRd = orientation == Orientation.HORIZONTAL ? getHorizontalRd() : getVerticalRd();
		getOrientationGrp().setSelected(orientRd.getModel(), true);
	}
	
	protected void updateGlobalRange() {
		var global = chart.get(GLOBAL_RANGE, Boolean.class, Boolean.TRUE);
		getAutoRangeCkb().setVisible(global);
		rangeMinLbl.setVisible(global);
		rangeMaxLbl.setVisible(global);
		getRangeMinTxt().setVisible(global);
		getRangeMaxTxt().setVisible(global);
		getRefreshRangeBtn().setVisible(global);
	}
	
	protected void updateItemLabel() {
		var showItemLabels = chart.get(SHOW_ITEM_LABELS, Boolean.class, Boolean.FALSE);
		itemLabelsColumnLbl.setEnabled(showItemLabels);
		getItemLabelsColumnCmb().setEnabled(showItemLabels);
		itemFontSizeLbl.setEnabled(showItemLabels);
		getItemFontSizeTxt().setEnabled(showItemLabels);
	}

	protected void updateRangeMinMax(boolean recalculate) {
		var global = chart.get(GLOBAL_RANGE, Boolean.class, Boolean.TRUE);
		
		if (global && setRange) {
			var auto = chart.get(AUTO_RANGE, Boolean.class, Boolean.TRUE);
			var range = chart.getList(RANGE, Double.class);
			
			if (auto) {
				if (recalculate) {
					range = calculateAutoRange();
					getRefreshRangeBtn().setEnabled(false);
				} else {
					updateRefreshRangeBtn();
				}
			}
			
			if (range != null && range.size() >= 2) {
				chart.set(RANGE, range);
				getRangeMinTxt().setText(""+range.get(0));
				getRangeMaxTxt().setText(""+range.get(1));
			}
		}
	}
	
	private void updateRefreshRangeBtn() {
		if (setRange) {
			boolean b = chart.get(GLOBAL_RANGE, Boolean.class, Boolean.TRUE);
			b = b && chart.get(AUTO_RANGE, Boolean.class, Boolean.TRUE);
			
			if (b) {
				var range = chart.getList(RANGE, Double.class);
				b = b && (range == null || !range.equals(calculateAutoRange()));
			}
			
			getRefreshRangeBtn().setEnabled(b);
		}
	}
	
	private void setGlobalRange() {
		var global = chart.get(GLOBAL_RANGE, Boolean.class, Boolean.TRUE);
		
		if (global && setRange) {
			var minTxt = getRangeMinTxt().getText().trim();
			var maxTxt = getRangeMaxTxt().getText().trim();
			
			try {
	            double min = Double.parseDouble(minTxt);
	            double max = Double.parseDouble(maxTxt);
	            chart.set(RANGE, Arrays.asList(min, max));
	        } catch (NumberFormatException e) {
	        }
		}
	}
	
	protected boolean isDataColumn(CyColumn c) {
		var colType = c.getType();
		var colListType = c.getListElementType();
		
		return dataType.isAssignableFrom(colType) ||
				(List.class.isAssignableFrom(colType) && dataType.isAssignableFrom(colListType));
	}
	
	protected static void selectColumnIdItem(JComboBox<CyColumnIdentifier> cmb, CyColumnIdentifier columnId) {
		if (columnId != null) {
			for (int i = 0; i < cmb.getItemCount(); i++) {
				var colId = cmb.getItemAt(i);
				
				if (colId != null && colId.equals(columnId)) {
					cmb.setSelectedItem(colId);
					break;
				}
			}
		}
	}
	
	protected JComboBox<Double> createAngleComboBox(AbstractCustomGraphics2<?> cg2, String propKey, Double[] values) {
		if (values == null)
			values = ANGLES;
		
		var cmb = new JComboBox<>(values);
		cmb.setToolTipText("Starting from 3 o'clock and measuring clockwise (90\u00B0 = 6 o'clock)");
		cmb.setEditable(true);
		((JLabel)cmb.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		cmb.setSelectedItem(cg2.get(propKey, Double.class, 0.0));
		cmb.setInputVerifier(new DoubleInputVerifier());
		
		cmb.addActionListener(evt -> {
			var angle = cmb.getSelectedItem();
	        try {
	        	cg2.set(propKey, angle instanceof Number ? ((Number)angle).doubleValue() : 0.0);
	        } catch (NumberFormatException ex) {
	        }
		});
		
		return cmb;
	}
	
	protected PaletteType getDefaultPaletteType() {
		return BrewerType.ANY;
	}
	
	protected String getDefaultPaletteName() {
		return "Set3 colors";
	}
	
	// ==[ CLASSES ]====================================================================================================
	
	protected class DataPanel extends JPanel {
		
		private final Set<CyColumnIdentifier> dataColumns;
		
		private JList<CyColumnIdentifier> allColumnsLs;
		private JList<CyColumnIdentifier> selColumnsLs;
		private final DefaultListModel<CyColumnIdentifier> allModel;
		private final DefaultListModel<CyColumnIdentifier> selModel;
		private JButton addBtn;
		private JButton addAllBtn;
		private JButton removeBtn;
		private JButton removeAllBtn;
		private JButton moveUpBtn;
		private JButton moveDownBtn;

		protected DataPanel() {
			dataColumns = new LinkedHashSet<CyColumnIdentifier>();
			allModel = new DefaultListModel<>();
			selModel = new DefaultListModel<>();
			
			// Filter all columns that are list of numbers
			for (var colId : columns.keySet()) {
				var c = columns.get(colId);
				
				if (isDataColumn(c))
					dataColumns.add(colId);
			}
			
			var allColumnsLbl = new JLabel("Available Columns:");
			var selColumnsLbl = new JLabel("Selected Columns:");
			
			var listScr1 = new JScrollPane(getAllColumnsLs());
			listScr1.setPreferredSize(new Dimension(200, listScr1.getPreferredSize().height));
			var listScr2 = new JScrollPane(getSelColumnsLs());
			listScr2.setPreferredSize(new Dimension(200, listScr2.getPreferredSize().height));
			
			var layout = new GroupLayout(this);
			setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(isWinLAF());
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(Alignment.LEADING, true)
									.addComponent(allColumnsLbl, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
									.addGroup(layout.createSequentialGroup()
											.addComponent(listScr1, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
											.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
													.addComponent(getAddBtn(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
													.addComponent(getAddAllBtn(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
													.addComponent(getRemoveBtn(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
													.addComponent(getRemoveAllBtn(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
											)
									)
							)
							.addGroup(layout.createParallelGroup(Alignment.LEADING, true)
									.addGroup(layout.createSequentialGroup()
											.addComponent(selColumnsLbl, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
											.addComponent(getMoveUpBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
											.addPreferredGap(RELATED)
											.addComponent(getMoveDownBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
									)
									.addComponent(listScr2, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
							)
					)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.BASELINE, false)
							.addComponent(allColumnsLbl, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(selColumnsLbl, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(getMoveUpBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							.addComponent(getMoveDownBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					)
					.addGroup(layout.createParallelGroup(Alignment.BASELINE, true)
							.addComponent(listScr1, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
							.addGroup(layout.createSequentialGroup()
									.addComponent(getAddBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
									.addComponent(getAddAllBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
									.addGap(0, 20, Short.MAX_VALUE)
									.addComponent(getRemoveBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
									.addComponent(getRemoveAllBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
							)
							.addComponent(listScr2, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					)
			);
		}
		
		protected void refresh() {
			var chartDataColumns = chart.getList(DATA_COLUMNS, CyColumnIdentifier.class);
			
			for (var colId : chartDataColumns) {
				selModel.addElement(colId);
			}
			
			if (dataColumns != null) {
				for (var colId : dataColumns) {
					if (!chartDataColumns.contains(colId))
						allModel.addElement(colId);
				}
			}
			
			if (selModel.getSize() == 0 && allModel.getSize() > 0) {
				// Add at least one data column to begin with
				var colId = allModel.get(0);
				allModel.removeElement(colId);
				selModel.addElement(colId);
				
				chart.set(DATA_COLUMNS, getDataColumns());
				updateRangeMinMax(true);
			}
			
			updateButtons();
			getColorSchemeEditor().reset(false);
		}
		
		private JList<CyColumnIdentifier> getAllColumnsLs() {
			if (allColumnsLs == null) {
				allColumnsLs = new JList<>();
				allColumnsLs.setModel(
						new SortedListModel<CyColumnIdentifier>(allModel, SortOrder.ASCENDING, new ColumnComparator()));
				allColumnsLs.setCellRenderer(new CyColumnCellRenderer(true));
				
				allColumnsLs.getSelectionModel().addListSelectionListener(evt -> updateButtons());
				
				allColumnsLs.getModel().addListDataListener(new ListDataListener() {
					@Override
					public void intervalRemoved(ListDataEvent e) {
						updateButtons();
					}
					@Override
					public void intervalAdded(ListDataEvent e) {
						updateButtons();
					}
					@Override
					public void contentsChanged(ListDataEvent e) {
					}
				});
			}
			
			return allColumnsLs;
		}
		
		private JList<CyColumnIdentifier> getSelColumnsLs() {
			if (selColumnsLs == null) {
				selColumnsLs = new JList<>();
				selColumnsLs.setModel(selModel);
				selColumnsLs.setCellRenderer(new CyColumnCellRenderer(true));
				
				selColumnsLs.getSelectionModel().addListSelectionListener(evt -> updateButtons());
			}
			
			return selColumnsLs;
		}
		
		private JButton getAddBtn() {
			if (addBtn == null) {
				addBtn = new JButton(IconManager.ICON_ANGLE_RIGHT);
				addBtn.setFont(serviceRegistrar.getService(IconManager.class).getIconFont(14.0f));
				addBtn.setToolTipText("Add Selected");
				
				addBtn.addActionListener(evt -> {
					moveDataColumns(getAllColumnsLs(), getSelColumnsLs(), false);
				});
			}
			
			return addBtn;
		}
		
		private JButton getAddAllBtn() {
			if (addAllBtn == null) {
				addAllBtn = new JButton(IconManager.ICON_ANGLE_DOUBLE_RIGHT);
				addAllBtn.setFont(serviceRegistrar.getService(IconManager.class).getIconFont(14.0f));
				addAllBtn.setToolTipText("Add All");
				
				addAllBtn.addActionListener(evt -> {
					moveDataColumns(getAllColumnsLs(), getSelColumnsLs(), true);
				});
			}
			
			return addAllBtn;
		}
		
		private JButton getRemoveBtn() {
			if (removeBtn == null) {
				removeBtn = new JButton(IconManager.ICON_ANGLE_LEFT);
				removeBtn.setFont(serviceRegistrar.getService(IconManager.class).getIconFont(14.0f));
				removeBtn.setToolTipText("Remove Selected");
				
				removeBtn.addActionListener(evt -> {
					moveDataColumns(getSelColumnsLs(), getAllColumnsLs(), false);
				});
			}
			
			return removeBtn;
		}
		
		private JButton getRemoveAllBtn() {
			if (removeAllBtn == null) {
				removeAllBtn = new JButton(IconManager.ICON_ANGLE_DOUBLE_LEFT);
				removeAllBtn.setFont(serviceRegistrar.getService(IconManager.class).getIconFont(14.0f));
				removeAllBtn.setToolTipText("Remove All");
				
				removeAllBtn.addActionListener(evt -> {
					moveDataColumns(getSelColumnsLs(), getAllColumnsLs(), true);
				});
			}
			
			return removeAllBtn;
		}
		
		private JButton getMoveUpBtn() {
			if (moveUpBtn == null) {
				moveUpBtn = new JButton(IconManager.ICON_CARET_UP);
				moveUpBtn.setFont(serviceRegistrar.getService(IconManager.class).getIconFont(17.0f));
				moveUpBtn.setToolTipText("Move Selected Up");
				moveUpBtn.setBorderPainted(false);
				moveUpBtn.setContentAreaFilled(false);
				moveUpBtn.setOpaque(false);
				moveUpBtn.setFocusPainted(false);
				moveUpBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
				
				moveUpBtn.addActionListener(evt -> moveUp(getSelColumnsLs()));
			}
			
			return moveUpBtn;
		}
		
		private JButton getMoveDownBtn() {
			if (moveDownBtn == null) {
				moveDownBtn = new JButton(IconManager.ICON_CARET_DOWN);
				moveDownBtn.setFont(serviceRegistrar.getService(IconManager.class).getIconFont(17.0f));
				moveDownBtn.setToolTipText("Move Selected Down");
				moveDownBtn.setBorderPainted(false);
				moveDownBtn.setContentAreaFilled(false);
				moveDownBtn.setOpaque(false);
				moveDownBtn.setFocusPainted(false);
				moveDownBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
				
				moveDownBtn.addActionListener(evt -> moveDown(getSelColumnsLs()));
			}
			
			return moveDownBtn;
		}
		
		private void updateButtons() {
			getAddBtn().setEnabled(!getAllColumnsLs().getSelectionModel().isSelectionEmpty());
			getRemoveBtn().setEnabled(!getSelColumnsLs().getSelectionModel().isSelectionEmpty());
			
			getAddAllBtn().setEnabled(getAllColumnsLs().getModel().getSize() > 0);
			getRemoveAllBtn().setEnabled(getSelColumnsLs().getModel().getSize() > 0);
			
			var selIndices = getSelColumnsLs().getSelectedIndices();
			int size = getSelColumnsLs().getModel().getSize();
			boolean b = selIndices != null && selIndices.length > 0;
			
			getMoveUpBtn().setEnabled(b && selIndices[0] > 0);
			getMoveDownBtn().setEnabled(b && selIndices[selIndices.length - 1] < size - 1);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void moveDataColumns(JList<CyColumnIdentifier> src, JList<CyColumnIdentifier> tgt, boolean all) {
			var set = new HashSet<CyColumnIdentifier>();
			
			final DefaultListModel<CyColumnIdentifier> srcModel;
			final DefaultListModel<CyColumnIdentifier> tgtModel;
			
			if (src.getModel() instanceof SortedListModel)
				srcModel = (DefaultListModel) ((SortedListModel)src.getModel()).getUnsortedModel();
			else
				srcModel = (DefaultListModel) src.getModel();
			
			if (tgt.getModel() instanceof SortedListModel)
				tgtModel = (DefaultListModel) ((SortedListModel)tgt.getModel()).getUnsortedModel();
			else
				tgtModel = (DefaultListModel) tgt.getModel();
				
			for (int i = 0; i < srcModel.getSize(); i++) {
				int index = i;
				
				if (src.getModel() instanceof SortedListModel)
					index = ((SortedListModel)src.getModel()).toUnsortedModelIndex(i);
				
				var colId = srcModel.get(index);
				
				if (all || src.isSelectedIndex(i)) {
					set.add(colId);
					
					if (!tgtModel.contains(colId))
						tgtModel.addElement(colId);
				}
			}
			
			for (var colId : set) {
				srcModel.removeElement(colId);
			}
			
			chart.set(DATA_COLUMNS, getDataColumns());
			
			if (!initializing) {
				updateRangeMinMax(true);
				getColorSchemeEditor().reset(false);
			}
		}
		
		private void moveUp(JList<CyColumnIdentifier> list) {
			var model = (DefaultListModel<CyColumnIdentifier>) list.getModel();

			var all = new LinkedList<CyColumnIdentifier>();
			var selIndices = new int[list.getSelectedIndices().length];
			int selCount = 0;
			boolean move = true;
			
			for (int i = 0; i < model.getSize(); i++) {
				var colId = model.get(i);
				
				if (list.isSelectedIndex(i)) {
					if (i == 0) {
						move = false;
						break;
					}
					
					all.add(i - 1, colId);
					selIndices[selCount++] = i - 1;
				} else {
					all.add(colId);
				}
			}
			
			if (move)
				replaceAll(list, all, selIndices);
		}

		private void moveDown(JList<CyColumnIdentifier> list) {
			var model = (DefaultListModel<CyColumnIdentifier>) list.getModel();
			var all = new LinkedList<CyColumnIdentifier>();
			var selIndices = new int[list.getSelectedIndices().length];
			int selCount = 0;
			boolean move = true;
			
			for (int i = model.getSize() - 1; i >= 0; i--) {
				var colId = model.get(i);
				
				if (list.isSelectedIndex(i)) {
					if (i == model.getSize() - 1) {
						move = false;
						break;
					}
					
					all.add(1, colId);
					selIndices[selCount++] = i + 1;
				} else {
					all.add(0, colId);
				}
			}
			
			if (move)
				replaceAll(list, all, selIndices);
		}
		
		private void replaceAll(JList<CyColumnIdentifier> list, List<CyColumnIdentifier> elements,
				int[] selectedIndices) {
			var model = (DefaultListModel<CyColumnIdentifier>) list.getModel();
			model.removeAllElements();
			int i = 0;
			
			for (var colId : elements) {
				model.add(i++, colId);
			}
			
			list.setSelectedIndices(selectedIndices);
			chart.set(DATA_COLUMNS, getDataColumns());
		}
		
		protected List<CyColumnIdentifier> getDataColumns() {
			var columns = new ArrayList<CyColumnIdentifier>();
			var model = getSelColumnsLs().getModel();
			
			for (int i = 0; i < model.getSize(); i++) {
				var colId = model.getElementAt(i);
				columns.add(colId);
			}
			
			return columns;
		}
	}
	
	public class ColumnComparator implements Comparator<CyColumnIdentifier> {

		private final Collator collator = Collator.getInstance(Locale.getDefault());
		
		@Override
		public int compare(CyColumnIdentifier c1, CyColumnIdentifier c2) {
			return collator.compare(c1.getColumnName(), c2.getColumnName());
		}
	}
	
	public static class DoubleInputVerifier extends InputVerifier {

		@Override
		public boolean verify(JComponent input) {
	        try {
	            Double.parseDouble(((JTextField) input).getText().trim());
	            return true; 
	        } catch (NumberFormatException e) {
	            return false;
	        }
		}
	}
	
	public static class IntInputVerifier extends InputVerifier {

		@Override
		public boolean verify(JComponent input) {
	        try {
	            Integer.parseInt(((JTextField) input).getText().trim());
	            return true; 
	        } catch (NumberFormatException e) {
	            return false;
	        }
		}
	}
	
	protected static class CyColumnComboBox extends JComboBox<CyColumnIdentifier> {
		
		public CyColumnComboBox(Collection<CyColumnIdentifier> columnIds, boolean acceptsNull) {
			var values = new ArrayList<>(columnIds);
			
			if (acceptsNull && !values.contains(null))
				values.add(0, null);
			
			var model = new DefaultComboBoxModel<>(values.toArray(new CyColumnIdentifier[values.size()]));
			this.setModel(model);
			this.setRenderer(new CyColumnCellRenderer());
		}
	}
	
	protected static class CyColumnCellRenderer extends DefaultListCellRenderer {

		private final boolean showCount;

		public CyColumnCellRenderer() {
			this(false);
		}
		
		public CyColumnCellRenderer(boolean showCount) {
			this.showCount = showCount;
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			var c = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			
			if (value == null) {
				c.setText("-- none --");
			} else if (value instanceof CyColumnIdentifier) {
				if (showCount) {
					int totalLength = (int)(Math.log10(list.getModel().getSize()) + 1);
					int idxLength = (int)(Math.log10(index + 1) + 1);
					int dif = totalLength - idxLength;
					String count = "";
					
					while (dif-- > 0) count += "&nbsp;";
					count += (index + 1) + ". ";
					
					c.setText( "<html><font face='Monospaced'>" + count + "</font>" +
							   ((CyColumnIdentifier)value).getColumnName() + "</html>" );
				} else {
					c.setText(((CyColumnIdentifier)value).getColumnName());
				}
			} else {
				c.setText("[ invalid column ]"); // Should never happen
			}
				
			return c;
		}
	}
}
