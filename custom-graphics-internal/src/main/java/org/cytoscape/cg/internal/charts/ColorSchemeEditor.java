package org.cytoscape.cg.internal.charts;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static org.cytoscape.cg.internal.charts.AbstractChart.DATA_COLUMNS;
import static org.cytoscape.cg.model.AbstractCustomGraphics2.COLORS;
import static org.cytoscape.cg.model.AbstractCustomGraphics2.COLOR_SCHEME;
import static org.cytoscape.cg.model.ColorScheme.CUSTOM;
import static org.cytoscape.cg.model.ColorScheme.DEFAULT;
import static org.cytoscape.util.swing.LookAndFeelUtil.isAquaLAF;
import static org.cytoscape.util.swing.LookAndFeelUtil.makeSmall;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.cytoscape.cg.internal.charts.bar.BarChart;
import org.cytoscape.cg.internal.charts.bar.BarChart.BarChartType;
import org.cytoscape.cg.internal.util.ColorUtil;
import org.cytoscape.cg.internal.util.IconUtil;
import org.cytoscape.cg.model.AbstractCustomGraphics2;
import org.cytoscape.cg.model.ColorScheme;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.color.BrewerType;
import org.cytoscape.util.color.Palette;
import org.cytoscape.util.swing.CyColorPaletteChooserFactory;
import org.cytoscape.view.presentation.property.values.CyColumnIdentifier;

@SuppressWarnings("serial")
public class ColorSchemeEditor<T extends AbstractCustomGraphics2<?>> extends JPanel {

	private static final Color DEFAULT_COLOR = Color.LIGHT_GRAY;
	
	private final Border COLOR_BORDER;
	private final Border COLOR_HOVER_BORDER;
	
	private JLabel colorSchemeLbl;
	private JComboBox<ColorScheme> colorSchemeCmb;
    private JButton colorPaletteBtn;
	private JPanel colorListPnl;
	
	private Palette palette;
	
	protected final T chart;
	protected ColorScheme[] colorSchemes;
	protected final boolean columnIsSeries;
	protected final CyNetwork network;
	protected final CyServiceRegistrar serviceRegistrar;
	
	protected int total = 0;

	// ==[ CONSTRUCTORS ]===============================================================================================
	
	public ColorSchemeEditor(
			T chart,
			ColorScheme[] colorSchemes,
			boolean columnIsSeries,
			CyNetwork network,
			CyServiceRegistrar serviceRegistrar
	) {
		this(chart, colorSchemes, columnIsSeries, false, network, serviceRegistrar);
	}
	
	public ColorSchemeEditor(
			T chart,
			ColorScheme[] colorSchemes,
			boolean columnIsSeries,
			boolean upAndDow,
			CyNetwork network,
			CyServiceRegistrar serviceRegistrar
	) {
		this.chart = chart;
		this.colorSchemes = colorSchemes;
		this.columnIsSeries = columnIsSeries;
		this.network = network;
		this.serviceRegistrar = serviceRegistrar;
		
		COLOR_BORDER = BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(UIManager.getColor("TextField.background"), 2),
				BorderFactory.createLineBorder(UIManager.getColor("TextField.inactiveForeground"), 1));
		COLOR_HOVER_BORDER = BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(UIManager.getColor("Focus.color"), 2),
				BorderFactory.createLineBorder(UIManager.getColor("TextField.background"), 1));
		
		init();
		updateColorList(false);
	}

	// ==[ PUBLIC METHODS ]=============================================================================================

	public void setColorSchemes(ColorScheme[] colorSchemes) {
		this.colorSchemes = colorSchemes;
		getColorSchemeCmb().removeAllItems();
		
		for (var scheme : colorSchemes)
			getColorSchemeCmb().addItem(scheme);
		
		updateColorSchemeCmb();
		reset();
	}
	
	public void reset() {
		total = 0;
		updateColorList(false);
	}
	
	// ==[ PRIVATE METHODS ]============================================================================================

	private void init() {
		colorSchemeLbl = new JLabel("Color Scheme:");
		
		setOpaque(false);
		
		var layout = new GroupLayout(this);
		setLayout(layout);
		layout.setAutoCreateContainerGaps(false);
		layout.setAutoCreateGaps(!isAquaLAF());
		
		var sep = new JSeparator();
		var colorListScr = new JScrollPane(getColorListPnl(),
				JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		colorListScr.setPreferredSize(new Dimension(160, 50));
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, true)
				.addGroup(layout.createSequentialGroup()
						.addComponent(colorSchemeLbl)
						.addComponent(getColorSchemeCmb(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
						.addComponent(getColorPaletteBtn(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
				)
				.addComponent(colorListScr)
				.addComponent(sep)
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
						.addComponent(colorSchemeLbl)
						.addComponent(getColorSchemeCmb())
						.addComponent(getColorPaletteBtn())
				)
				.addComponent(colorListScr, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
				.addComponent(sep, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
		);
		
		makeSmall(getColorPaletteBtn());
	}
	
	protected JComboBox<ColorScheme> getColorSchemeCmb() {
		if (colorSchemeCmb == null) {
			colorSchemeCmb = new JComboBox<>(colorSchemes);
			colorSchemeCmb.setRenderer(new ColorSchemeComboBoxRenderer());
			updateColorSchemeCmb();
			
			colorSchemeCmb.addActionListener(evt -> {
				var newScheme = (ColorScheme) colorSchemeCmb.getSelectedItem();
				
				if (newScheme != CUSTOM)
					palette = null;
				
				chart.set(COLOR_SCHEME, newScheme);
				updateColorList(true);
			});
		}
		
		return colorSchemeCmb;
	}
	
	protected JButton getColorPaletteBtn() {
		if (colorPaletteBtn == null) {
			colorPaletteBtn = new JButton("More Palettes...");

			colorPaletteBtn.addActionListener(evt -> {
				// Open color chooser
				var chooserFactory = serviceRegistrar.getService(CyColorPaletteChooserFactory.class);
				var type = palette != null ? palette.getType() : BrewerType.ANY;
				var chooser = chooserFactory.getColorPaletteChooser(type, true);
				var title = "Palettes";
				var chartType = chart.get(BarChart.TYPE, BarChartType.class);
				
				int size = 1;
				
				if (chartType == BarChartType.UP_DOWN) {
					size = 2;
				} else if (chartType == BarChartType.HEAT_STRIPS) {
					size = 3;
				} else {
					size = getTotal();
					size = Math.max(size, 5);
				}
				
				chooser.showDialog(this, title, palette, size);
				palette = chooser.getSelectedPalette();

				if (palette != null) {
					getColorSchemeCmb().setSelectedItem(CUSTOM);
					
					var newScheme = new ColorScheme(palette);
					chart.set(COLOR_SCHEME, newScheme);
					updateColorList(true);
				}
			});
		}

		return colorPaletteBtn;
	}
	
	protected JPanel getColorListPnl() {
		if (colorListPnl == null) {
			colorListPnl = new JPanel();
			colorListPnl.setOpaque(false);
			colorListPnl.setLayout(new BoxLayout(colorListPnl, BoxLayout.X_AXIS));
		}
		
		return colorListPnl;
	}
	
	private void updateColorSchemeCmb() {
		if (colorSchemeCmb == null)
			return;
		
		var scheme = chart.get(COLOR_SCHEME, ColorScheme.class, DEFAULT);
		
		if (Arrays.asList(colorSchemes).contains(scheme)) {
			colorSchemeCmb.setSelectedItem(scheme);
		} else if (scheme.getPalette() != null) {
			colorSchemeCmb.setSelectedItem(CUSTOM);
			palette = scheme.getPalette();
		} else {
			scheme = CUSTOM;
			colorSchemeCmb.setSelectedItem(scheme);
			chart.set(COLOR_SCHEME, scheme);
		}
	}
	
	protected void updateColorList(boolean newScheme) {
		List<Color> colors = new ArrayList<>(chart.getList(COLORS, Color.class));
		var scheme = chart.get(COLOR_SCHEME, ColorScheme.class, DEFAULT);
		int nColors = getTotal();
		
		if (nColors > 0) {
			if (newScheme || colors.isEmpty()) {
				if (CUSTOM.equals(scheme)) {
					int newSize = Math.max(colors.size(), nColors);
					colors = new ArrayList<Color>(newSize);
					
					for (int i = 0; i < newSize; i++)
						colors.add(i%2 == 0 ? DEFAULT_COLOR : DEFAULT_COLOR.darker());
				} else {
					colors = scheme.getColors(nColors);
				}
			} else if (colors.size() < nColors) {
				// Just update existing list of colors (add new ones if there are more values now)
				var newColors = scheme.getColors(nColors);
				
				if (newColors.size() > colors.size())
					colors.addAll(newColors.subList(colors.size(), newColors.size()));
			}
		}
		
		// Build list of editable colors
		getColorListPnl().removeAll();
		
		for (int i = 0; i < total; i++) {
			var c = colors.size() > i ? colors.get(i) : Color.GRAY;
			var cp = new ColorPanel(c, "");
			style(cp, i);
			
			getColorListPnl().add(cp);
		}
		
		getColorListPnl().repaint();
	
		if (!colors.isEmpty())
			chart.set(COLORS, colors);
	}
	
	protected void style(ColorPanel cp, int index) {
		var label = "" + (index + 1);
		cp.setText(label);
	}
	
	protected int getTotal() {
		if (total <= 0) {
			var dataColumns = chart.getList(DATA_COLUMNS, CyColumnIdentifier.class);
			
			if (columnIsSeries) {
				// Each column represents a data series
				total = dataColumns.size();
			} else if (network != null) {
				// Columns represent data categories--each list element is an item/color
				int nColors1 = 0;
				int nColors2 = 0;
				
				var allNodes = network.getNodeList();
				var table = network.getDefaultNodeTable();
				
				for (var colId : dataColumns) {
					var column = table.getColumn(colId.getColumnName());
					if (column == null) continue;
					
					if (column.getType() == List.class) {
						for (var node : allNodes) {
							var row = network.getRow(node);
							var values = row.getList(column.getName(), column.getListElementType());
							
							if (values != null)
								nColors1 = Math.max(nColors1, values.size());
						}
					} else {
						nColors2++;
					}
				}
				
				total = Math.max(nColors1, nColors2);
			}
		}
		
		return total;
	}
	
	// ==[ CLASSES ]====================================================================================================

	protected class ColorPanel extends JLabel {

		private Color color;

		ColorPanel(Color color, String label) {
			super(label, IconUtil.emptyIcon(20, 20), JLabel.CENTER);
			
			if (color == null)
				color = Color.LIGHT_GRAY;
			
			this.color = color;
			
			this.setFont(this.getFont().deriveFont(10.0f));
			this.setHorizontalTextPosition(JLabel.CENTER);
			this.setOpaque(true);
			this.setBackground(color);
			this.setForeground(ColorUtil.getContrastingColor(color));
			this.setBorder(COLOR_BORDER);
			this.setToolTipText(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
			
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					chooseColor();
					onColorsUpdated();
				}
				@Override
				public void mouseEntered(MouseEvent e) {
					ColorPanel.this.setBorder(COLOR_HOVER_BORDER);
				}
				@Override
				public void mouseExited(MouseEvent e) {
					ColorPanel.this.setBorder(COLOR_BORDER);
				}
			});
		}
		
		public Color getColor() {
			return color;
		}
		
		private void chooseColor() {
			var chooserFactory = serviceRegistrar.getService(CyColorPaletteChooserFactory.class);
			var chooser = chooserFactory.getColorPaletteChooser(BrewerType.ANY, false);
			var chartType = chart.get(BarChart.TYPE, BarChartType.class);
			
			int size = 1;
			
			if (chartType == BarChartType.UP_DOWN)
				size = 2;
			else if (chartType == BarChartType.HEAT_STRIPS)
				size = 3;
			else {
				if (palette != null)
					size = palette.size();
				else
					size = Math.max(5, getTotal());
			}
			
			var newColor = chooser.showDialog(this, "Colors", palette, color, size);
			
			if (newColor != null) {
				color = newColor;
				setBackground(newColor);
				setForeground(ColorUtil.getContrastingColor(newColor));
			}
		}
		
		@SuppressWarnings("rawtypes")
		private void onColorsUpdated() {
			var rows = getColorListPnl().getComponents();
			var newColors = new ArrayList<Color>();
			
			if (rows != null) {
				for (var c : rows) {
					if (c instanceof ColorSchemeEditor.ColorPanel) {
						var color = ((ColorSchemeEditor.ColorPanel) c).getColor();
						newColors.add(color);
					}
				}
			}
			
			chart.set(COLORS, newColors);
		}
	}
	
	private static class ColorSchemeComboBoxRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			var c = (DefaultListCellRenderer) super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus);
			
			if (value == null)
				c.setText("-- none --");
			else if (value instanceof ColorScheme)
				c.setText(((ColorScheme)value).getLabel());
			else if (value instanceof String)
				c.setText((String)value);
			else
				c.setText("[ invalid value ]"); // Should never happen
				
			return c;
		}
	}
}
