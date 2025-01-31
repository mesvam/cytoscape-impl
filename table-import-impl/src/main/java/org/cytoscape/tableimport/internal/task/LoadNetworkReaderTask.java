package org.cytoscape.tableimport.internal.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.tableimport.internal.reader.AbstractMappingParameters;
import org.cytoscape.tableimport.internal.reader.ExcelNetworkSheetReader;
import org.cytoscape.tableimport.internal.reader.GraphReader;
import org.cytoscape.tableimport.internal.reader.NetworkTableMappingParameters;
import org.cytoscape.tableimport.internal.reader.NetworkTableReader;
import org.cytoscape.tableimport.internal.reader.SupportedFileType;
import org.cytoscape.tableimport.internal.reader.TextDelimiter;
import org.cytoscape.tableimport.internal.ui.PreviewTablePanel;
import org.cytoscape.tableimport.internal.util.AttributeDataType;
import org.cytoscape.tableimport.internal.util.ImportType;
import org.cytoscape.tableimport.internal.util.SourceColumnSemantic;
import org.cytoscape.tableimport.internal.util.TypeUtil;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;
import org.cytoscape.work.util.ListSelection;
import org.cytoscape.work.util.ListSingleSelection;

/*
 * #%L
 * Cytoscape Table Import Impl (table-import-impl)
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

public class LoadNetworkReaderTask extends AbstractTask implements CyNetworkReader, TunableValidator {
	
	private InputStream is;
	private String fileType;
	private CyNetwork[] networks;
	private String inputName;
	private GraphReader reader;
	private CyNetworkReader netReader;
	private PreviewTablePanel previewPanel;
	private String networkName;
	private URI uri;
	private File tempFile;
	private TaskMonitor taskMonitor;
	
	private final TableImportContext tableImportContext;
	private final CyServiceRegistrar serviceRegistrar;
	private boolean nogui;
	
	@ContainsTunables
	public DelimitersTunable delimiters = new DelimitersTunable();

	@Tunable(description = "Text delimiters for lists", 
	         longDescription = "Select the delimiters to use to separate list entries in a list, "+
					                   "from the list '``|``','``\\``','``/``', or '``,``'.  ``|`` is "+
														 "used by default",
	         exampleStringValue = "|,\\",
	         context = "both")
	public ListSingleSelection<String> delimitersForDataList;

	@Tunable(description = "Starting row", 
	         longDescription = "The starting row of the import.  This is used to skip over comments and "+
					                   "other non-data rows at the beginning of the file.",
	         exampleStringValue = "1",
	         context = "both")
	public int startLoadRow = -1;

	@Tunable(description = "Column names in first row?", 
	         exampleStringValue = "false",
	         longDescription = "If this is ``true`` then the first row should contain the names of the columns. "+
	                           "Note that ``startLoadRow`` must be set for this to work properly",
	         context = "both")
	public boolean firstRowAsColumnNames;

	@Tunable(description = "Source column number", 
	         exampleStringValue = "1",
	         longDescription = "The column index that contains the source node identifiers.",
	         required = false,
	         context = "both")
	public int indexColumnSourceInteraction = -1;

	@Tunable(description = "Target column number", 
	         longDescription = "The column index that contains the target node identifiers.  If this is not "+
	                           "specified then the resulting network will have no edges",
	         exampleStringValue = "3",
	         context = "both")
	public int indexColumnTargetInteraction = -1;

	@Tunable(description = "Interaction column number", 
	         longDescription = "The column index that contains the interaction type.  This is not required.",
	         exampleStringValue = "2",
	         context = "both")
	public int indexColumnTypeInteraction = -1;

	@Tunable(description = "Default interaction type",
	         longDescription = "Used to set the default interaction type to use when there is no interaction type column.",
	         exampleStringValue = "pp",
	         context = "both")
	public String defaultInteraction = TypeUtil.DEFAULT_INTERACTION;
	
	@Tunable(description = "Column data types",
	         longDescription = "List of column data types ordered by "+
					                   "column index (e.g. \"string,int,long,"+
														 "double,boolean,intlist\" or just "+
														 "\"s,i,l,d,b,il\"):", 
	         exampleStringValue = "string,int,string,double,double",
					 context = "nongui")
	public String dataTypeList;

	@Tunable(description = "Column import types",
	         longDescription = "List of column types ordered by "+
					                   "column index (e.g. \"source,target,interaction,source attribute,"+
														 "target attribute,edge attribute,skip\" or just "+
														 "\"s,t,i,sa,ta,ea,x\"):", 
	         exampleStringValue = "source,target,source attribute,source attribute,target attribute,target attribute",
					 context = "nongui")
	public String columnTypeList;
	
	@Tunable(description="Decimal character used in the decimal format",
			longDescription="Character that separates the integer-part (characteristic) and the fractional-part (mantissa) of a decimal number. The default value is the dot \".\"",
			exampleStringValue=".",
			context="nogui")
	public String decimalSeparator;
	private Character decSeparator;
	
	private NetworkTableMappingParameters ntmp;

	public LoadNetworkReaderTask(TableImportContext tableImportContext, CyServiceRegistrar serviceRegistrar) {
		this(tableImportContext, serviceRegistrar, false);
	}

	public LoadNetworkReaderTask(
			TableImportContext tableImportContext,
			CyServiceRegistrar serviceRegistrar,
			boolean nogui
	) {
		this.tableImportContext = tableImportContext;
		this.serviceRegistrar = serviceRegistrar;
	    
		var tempList = new ArrayList<String>();
		tempList.add(TextDelimiter.PIPE.getDelimiter());
		tempList.add(TextDelimiter.BACKSLASH.getDelimiter());
		tempList.add(TextDelimiter.SLASH.getDelimiter());
		tempList.add(TextDelimiter.COMMA.getDelimiter());
		delimitersForDataList = new ListSingleSelection<>(tempList);
		this.nogui = nogui;
	}
	
	public void setInputFile(
			InputStream is,
			String fileType,
			String inputName,
			URI uriName,
			IconManager iconManager
	) {
		this.is = is;
		this.fileType = fileType;
		this.inputName = inputName;
		this.uri = uriName;
		
		previewPanel = new PreviewTablePanel(ImportType.NETWORK_IMPORT, tableImportContext, iconManager);

		try{
			tempFile = File.createTempFile("temp", this.fileType);
			tempFile.deleteOnExit();
			var os = new FileOutputStream(tempFile);
			int read = 0;
			var bytes = new byte[1024];
		 
			while ((read = is.read(bytes)) != -1) {
				os.write(bytes, 0, read);
			}
			os.flush();
			os.close();
			
			this.is = new FileInputStream(tempFile);
		} catch(Exception e){
			this.is = null;
			e.printStackTrace();
		}
		
		delimiters.setSelectedValues(Arrays.asList(TextDelimiter.TAB, TextDelimiter.COMMA));
		delimitersForDataList.setSelectedValue(TextDelimiter.PIPE.getDelimiter());
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		tm.setTitle("Loading network from table");
		tm.setProgress(0.0);

		tm.setStatusMessage("Loading network...");
		taskMonitor = tm;
		
		if (decimalSeparator == null || decimalSeparator.isEmpty()) {
			decSeparator = AbstractMappingParameters.DEF_DECIMAL_SEPARATOR;
		} else {
			decSeparator = decimalSeparator.charAt(0);
		}
		
		var networkReaderManager = serviceRegistrar.getService(CyNetworkReaderManager.class);
		
		if (is != null)
			netReader = networkReaderManager.getReader(is, inputName);

		if (netReader == null)				
			netReader = networkReaderManager.getReader(uri, inputName);

		if (netReader instanceof CombineNetworkReaderAndMappingTask) {
			Workbook workbook = null;

			// Load Spreadsheet data for preview.
			if (fileType != null && (fileType.equalsIgnoreCase(
					SupportedFileType.EXCEL.getExtension())
					|| fileType.equalsIgnoreCase(SupportedFileType.OOXML.getExtension())) && workbook == null) {
				try {
					workbook = WorkbookFactory.create(new FileInputStream(tempFile));
				} catch (Exception e) {
					//e.printStackTrace();
					throw new IllegalArgumentException("Could not read Excel file.  Maybe the file is broken?" , e);
				} finally {
					
				}
			}
			
			netReader = null;
			
			if (startLoadRow > 0)
				startLoadRow--;
			
			int startLoadRowTemp = firstRowAsColumnNames ? 0 : startLoadRow;
			
			previewPanel.update(
					workbook,
					fileType,
					tempFile.getAbsolutePath(),
					new FileInputStream(tempFile),
					delimiters.getSelectedValues(),
					null,
					startLoadRowTemp,
					decSeparator
			);
			
			int colCount = previewPanel.getPreviewTable().getColumnModel().getColumnCount();
			Object curName = null;
			
			if (firstRowAsColumnNames) {
				previewPanel.setFirstRowAsColumnNames();
				startLoadRow++;
			}
	
			var types = previewPanel.getTypes();
			
			// Column Names:
			var attrNameList = new ArrayList<String>();

			for (int i = 0; i < colCount; i++) {
				curName = previewPanel.getPreviewTable().getColumnModel().getColumn(i).getHeaderValue();
				
				if (attrNameList.contains(curName)) {
					int dupIndex = 0;
	
					for (int idx = 0; idx < attrNameList.size(); idx++) {
						if (curName.equals(attrNameList.get(idx))) {
							dupIndex = idx;
	
							break;
						}
					}
	
					if (!TypeUtil.allowsDuplicateName(ImportType.NETWORK_IMPORT, types[i], types[dupIndex])) {
						// TODO add message to user (Duplicate Column Name Found)
						return;
					}
				}
	
				if (curName == null)
					attrNameList.add("Column " + i);
				else
					attrNameList.add(curName.toString());
			}
			
			var attributeNames = attrNameList.toArray(new String[attrNameList.size()]);
			var typesCopy = Arrays.copyOf(types, types.length);
			
			// Data Types:
			var dataTypes = previewPanel.getDataTypes();
			var dataTypesCopy = Arrays.copyOf(dataTypes, dataTypes.length);
			
			AttributeDataType[] tunableDataTypes = null;
			
			if (dataTypeList != null && !dataTypeList.trim().isEmpty())
				tunableDataTypes = TypeUtil.parseDataTypeList(dataTypeList);
			
			if (tunableDataTypes != null && tunableDataTypes.length > 0)
				System.arraycopy(
						tunableDataTypes, 0,
						dataTypesCopy, 0, 
						Math.min(tunableDataTypes.length, dataTypesCopy.length));
			
			// Semantic Types:
			SourceColumnSemantic[] tunableColumnTypes = null;
			
			if (columnTypeList != null && !columnTypeList.trim().isEmpty())
				tunableColumnTypes = TypeUtil.parseColumnTypeList(columnTypeList);

			if (tunableColumnTypes != null && tunableColumnTypes.length > 0) {
				System.arraycopy(
						tunableColumnTypes, 0,
						typesCopy, 0, 
						Math.min(tunableColumnTypes.length, typesCopy.length));
				// Set the source and target interaction columns
				int index = 1;

				for (SourceColumnSemantic scs : tunableColumnTypes) {
					if (scs.equals(SourceColumnSemantic.SOURCE))
						indexColumnSourceInteraction = index;
					else if (scs.equals(SourceColumnSemantic.TARGET))
						indexColumnTargetInteraction = index;
					else if (scs.equals(SourceColumnSemantic.INTERACTION))
						indexColumnTypeInteraction = index;
					
					index++;
				}
			}
			
			// Namespaces:
			var namespaces = previewPanel.getNamespaces();
			var namespacesCopy = Arrays.copyOf(namespaces, namespaces.length);

// TODO Set namespaces though Tunables as well
//			String[] tunableNamespaces = null;
//			
//			if (namespaceList != null && !namespaceList.trim().isEmpty())
//				tunableNamespaces = TypeUtil.parseDataTypeList(namespaceList);
//			
//			if (tunableNamespaces != null && tunableNamespaces.length > 0)
//				System.arraycopy(
//						tunableNamespaces, 0,
//						namespacesCopy, 0, 
//						Math.min(tunableNamespaces.length, namespacesCopy.length));

			if (nogui) {
				// Handle the validation
				nogui = false;
				var state = getValidationState(new StringBuffer(80));
				
				switch (state) {
					case INVALID:
						tm.showMessage(TaskMonitor.Level.ERROR, "Source column must be specified");
						return;
					case REQUEST_CONFIRMATION:
						tm.showMessage(TaskMonitor.Level.WARN, "Target column is not specified.  No edges will be created");
				}
				nogui = true;
			}

			var listDelimiters = previewPanel.getListDelimiters();

			if (listDelimiters == null || listDelimiters.length == 0) {
				listDelimiters = new String[dataTypes.length];
				
				if (delimitersForDataList.getSelectedValue() != null)
					Arrays.fill(listDelimiters, delimitersForDataList.getSelectedValue());
			}
			
			if (indexColumnSourceInteraction > 0)
				indexColumnSourceInteraction--;

			if (indexColumnTargetInteraction > 0)
				indexColumnTargetInteraction--;

			if (indexColumnTypeInteraction > 0)
				indexColumnTypeInteraction--;
			
			networkName = previewPanel.getSourceName();
			
			ntmp = new NetworkTableMappingParameters(networkName, delimiters.getSelectedValues(),
					listDelimiters, attributeNames, dataTypesCopy, typesCopy, namespacesCopy,
					indexColumnSourceInteraction, indexColumnTargetInteraction, indexColumnTypeInteraction,
					defaultInteraction, startLoadRow, null, decSeparator);
			
			try {
				if (fileType.equalsIgnoreCase(SupportedFileType.EXCEL.getExtension()) ||
				    fileType.equalsIgnoreCase(SupportedFileType.OOXML.getExtension())) {
					var sheet = workbook.getSheet(networkName);
					
					reader = new ExcelNetworkSheetReader(networkName, sheet, ntmp, nMap, rootNetwork, serviceRegistrar);
				} else {
					networkName = inputName;
					reader = new NetworkTableReader(networkName, new FileInputStream(tempFile), ntmp, nMap, rootNetwork, serviceRegistrar);
				}
			} catch (Exception ioe) {
				tm.showMessage(TaskMonitor.Level.ERROR, "Unable to read network: "+ioe.getMessage());
				return;
			}
			
			loadNetwork(tm);
			tm.setProgress(1.0);
		} else {
			networkName = inputName;
			insertTasksAfterCurrentTask(netReader);
		}
	}
	
	private void loadNetwork(TaskMonitor tm) throws IOException {
		final CyNetwork network;
		
		if (rootNetwork == null) {
			network = serviceRegistrar.getService(CyNetworkFactory.class).createNetwork();
			rootNetwork = serviceRegistrar.getService(CyRootNetworkManager.class).getRootNetwork(network);
		} else {
			network = rootNetwork.addSubNetwork(); //CytoscapeServices.cyNetworkFactory.createNetwork();
		}
		tm.setProgress(0.10);
		reader.setNetwork(network);

		if (cancelled)
			return;

		reader.read();
		tm.setProgress(0.80);

		if (cancelled)
			return;
		
		networks = new CyNetwork[] { network };
		tm.setProgress(1.0);
	}

	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork net) {
		if (netReader != null) {
			return netReader.buildCyNetworkView(net);
		} else {
			var view = networkViewFactory.createNetworkView(net);
			var layoutMgr = serviceRegistrar.getService(CyLayoutAlgorithmManager.class);
			var layout = layoutMgr.getDefaultLayout();
			var attribute = layoutMgr.getLayoutAttribute(layout, view);
			var itr = layout.createTaskIterator(view, layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, attribute);
			var nextTask = itr.next();
			
			try {
				nextTask.run(taskMonitor);
			} catch (Exception e) {
				throw new RuntimeException("Could not finish layout", e);
			}
	
			taskMonitor.setProgress(1.0);
			return view;	
		}
	}

	@Override
	public CyNetwork[] getNetworks() {
		if (netReader != null)
			return netReader.getNetworks();
		else
			return networks;
	}

	public String getName(){
		return networkName;
	}

	@Override
	public ValidationState getValidationState(Appendable errMsg) {
		// If we're in nogui mode, we really don't want to have this
		// handled by the TunableValidation.  We'll call this method
		// ourselves in the run method
		if (nogui) return ValidationState.OK;
		try {
			if (indexColumnSourceInteraction <= 0) {
				if (indexColumnTargetInteraction <= 0) {
					errMsg.append("The network cannot be created without selecting the source and target columns.");
					return ValidationState.INVALID;
				} else {
					errMsg.append("No edges will be created in the network; the source column is not selected.\nDo you want to continue?");
					return ValidationState.REQUEST_CONFIRMATION;
				}
			} else {
				if (indexColumnTargetInteraction <= 0) {
					errMsg.append("No edges will be created in the network; the target column is not selected.\nDo you want to continue?");
					return ValidationState.REQUEST_CONFIRMATION;
				} else {
					return ValidationState.OK;
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return ValidationState.INVALID;
		}
	}
	
	// support import network in different collection
	private CyRootNetwork rootNetwork;
	public void setRootNetwork(CyRootNetwork rootNetwork){
		this.rootNetwork = rootNetwork;
	}
	
	private Map<Object, CyNode> nMap;
	public void setNodeMap(Map<Object, CyNode> nMap){
		this.nMap = nMap;
	}

	private CyNetworkViewFactory networkViewFactory;
	public void setNetworkViewFactory(CyNetworkViewFactory networkViewFactory) {
		this.networkViewFactory = networkViewFactory;
	}
	
	//-----------------------------------------------------------------------------
	// temporary implementation  AT -- 27 June 2016
	
	private String getString()
	{
		StringBuilder str = new StringBuilder("LoadNetworkReaderTask\n");
		str.append("delimiters=").append(listToString(delimiters.getTunable())).append("\n");
		str.append("delimitersForDataList=").append(listToString(delimitersForDataList)).append("\n");
		str.append("startLoadRow=" + startLoadRow + "\n");
		str.append("firstRowAsColumnNames=").append(firstRowAsColumnNames ? "TRUE" : "FALSE").append("\n");
		str.append("indexColumnSourceInteraction=" + indexColumnSourceInteraction + "\n");
		str.append("indexColumnTargetInteraction=" + indexColumnTargetInteraction + "\n");
		str.append("indexColumnTypeInteraction=" + indexColumnTypeInteraction + "\n");
		str.append("defaultInteraction=").append(defaultInteraction).append("\n");
		str.append("dataTypeList=").append(dataTypeList).append("\n");
		return str.toString();
	}

	private void setString(String state)
	{
		loadList(delimiters.getTunable(), fieldFromString(state, "delimiters"));
		loadList(delimitersForDataList, fieldFromString(state, "delimitersForDataList"));
		startLoadRow = Integer.parseInt(fieldFromString(state, "startLoadRow"));
		firstRowAsColumnNames = "TRUE".equals(fieldFromString(state, "firstRowAsColumnNames"));
		indexColumnSourceInteraction = intFromString(state, "indexColumnSourceInteraction");
		indexColumnTypeInteraction = intFromString(state, "indexColumnTypeInteraction");
		defaultInteraction = fieldFromString(state, "defaultInteraction");
		dataTypeList = fieldFromString(state, "dataTypeList");
	}
	
	private int intFromString(String s, String fieldName)
	{
		String fld = fieldFromString(s, fieldName);
		try {
			return Integer.parseInt(fld);
		}
		catch (NumberFormatException e) {}
		return 0;
	}
	
	static String DELIM = "\b";
	private String listToString(ListSelection<String> set)
	{
		StringBuilder builder = new StringBuilder();
		for (String s : set.getPossibleValues())
			builder.append(s).append(DELIM);
		return builder.toString();
	}
	
	private void loadList(ListSelection<String> set, String input)
	{
		ArrayList<String> delims = new ArrayList<String>();
		String[] parsed = input.split(DELIM);
		delims.addAll(Arrays.asList(parsed));
		set.setPossibleValues(delims);
	}

	private String fieldFromString(String s, String field)
	{
		int start = s.indexOf(field + "=");
		if (start > 0)
		{
			start += field.length() + 1;
			int end = s.indexOf( "\n", start );
			if (end > 0)
				return s.substring(start, end);
		}
		return null;
	}
	//-----------------------------------------------------------------------------
	private String getProperty()
	{
		CyProperty<Properties> cyProperties = serviceRegistrar.getService(CyProperty.class, "(cyPropertyName=myApp.props)");
		String propertyValue = "";  /// cyProperties.getProperty("network.load.config");
		if (propertyValue != null && !propertyValue.isEmpty())
			setString(propertyValue);
		return null;
	}
	
	private void saveProperty()
	{
		Properties stateProps = new Properties();
		stateProps.setProperty("network.load.config", getString());
		// TODO put it into the session or config file
	}
	//-----------------------------------------------------------------------------
	
	private static class PropsReader extends AbstractConfigDirPropsReader {
        public PropsReader(String name, String fileName) {
            super(name, fileName, SavePolicy.SESSION_FILE);
        }
        public static PropsReader makeReader(String props) {
        	return new PropsReader("network.load.props", props);
        }
    }
	//-----------------------------------------------------------------------------

}
