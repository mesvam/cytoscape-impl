package org.cytoscape.task.internal.export.web;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CySessionWriterFactory;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;
import org.cytoscape.work.swing.RequestsUIHelper;
import org.cytoscape.work.swing.TunableUIHelper;
import org.cytoscape.work.util.ListChangeListener;
import org.cytoscape.work.util.ListSelection;
import org.cytoscape.work.util.ListSingleSelection;

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

/**
 * Task to export all networks and styles as Cytoscape.js style JSON.
 */
public class ExportAsWebArchiveTask extends AbstractTask implements TunableValidator, RequestsUIHelper {

	private static final String FILE_EXTENSION = ".zip";
	
	private static final String AS_SPA = "Full web application";
	private static final String AS_SIMPLE_PAGE = "Simple viewer for current network only";
	private static final String AS_ZIPPED_ARCHIVE = "Networks and Style JSON files only (No HTML)";
	
	private TunableUIHelper helper;

	@ProvidesTitle
	public String getTitle() {
		return "Export as Cytoscape.js Web Page";
	}

	public File file; 
	@Tunable(description = "Export Networks and Styles as:", params = "fileCategory=archive;input=false", listenForChange="outputFormat")
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		if(file == null || file.getName().endsWith(FILE_EXTENSION))
			this.file = file;
		else {
			this.file = new File(file.getAbsolutePath() + FILE_EXTENSION);
			if(helper != null)
				helper.update(this);
		}
	}

	@Tunable(description = "Export as:")
	public ListSingleSelection<String> outputFormat;

	private final CySessionWriterFactory fullWriterFactory;
	private final CySessionWriterFactory simpleWriterFactory;
	private final CySessionWriterFactory zippedWriterFactory;
	private final CyServiceRegistrar serviceRegistrar;
	
	private CyWriter writer;

	public ExportAsWebArchiveTask(
			CySessionWriterFactory fullWriterFactory,
			CySessionWriterFactory simpleWriterFactory,
			CySessionWriterFactory zippedWriterFactory,
			CyServiceRegistrar serviceRegistrar
	) {
		this.fullWriterFactory = fullWriterFactory;
		this.simpleWriterFactory = simpleWriterFactory;
		this.zippedWriterFactory = zippedWriterFactory;
		this.serviceRegistrar = serviceRegistrar;
		
		this.outputFormat = new ListSingleSelection<String>(AS_SPA, AS_SIMPLE_PAGE, AS_ZIPPED_ARCHIVE);
		this.file = getSuggestedFile();
		
		outputFormat.addListener(new ListChangeListener<String>() {
			@Override
			public void selectionChanged(ListSelection<String> source) {
				file = getSuggestedFile();
			}
			@Override
			public void listChanged(ListSelection<String> source) {
			}
		});
	}

	/**
	 * Archive the data into a zip file.
	 */
	@Override
	public void run(TaskMonitor tm) throws Exception {
		tm.setProgress(0);
		
		if (file == null)
			return;
		
		// Get export type
		String exportType = outputFormat.getSelectedValue();

		// Add extension if missing.
		if (!file.getName().endsWith(FILE_EXTENSION))
			file = new File(file.getPath() + FILE_EXTENSION);

		// Compress everything as a zip archive.
		var os = new FileOutputStream(file);
		CyWriter writer = null;

		if (exportType.equals(AS_SPA)) {
			writer = fullWriterFactory.createWriter(os, null);
		} else if (exportType.equals(AS_SIMPLE_PAGE)) {
			writer = simpleWriterFactory.createWriter(os, null);
		} else if (exportType.equals(AS_ZIPPED_ARCHIVE)) {
			writer = zippedWriterFactory.createWriter(os, null);
		} else {
			os.close();
			throw new NullPointerException("Could not find web session writer.");
		}

		writer.run(tm);
		os.close();

		tm.setProgress(1.0);
	}

	@Override
	public void cancel() {
		super.cancel();
		
		if (writer != null)
			writer.cancel();
	}

	private File getSuggestedFile() {
		String exportName = null;
		var sessionManager = serviceRegistrar.getService(CySessionManager.class);
		var applicationManager = serviceRegistrar.getService(CyApplicationManager.class);
		
		if (!outputFormat.getSelectedValue().equals(AS_SIMPLE_PAGE)) {
			exportName = FilenameUtils.getBaseName(sessionManager.getCurrentSessionFileName());
		} else {
			var network = applicationManager.getCurrentNetwork();
			if (network == null) throw new RuntimeException("No network to export");
			exportName = network.getRow(network).get(CyNetwork.NAME, String.class);
		}
		
		if (exportName == null || exportName.trim().isEmpty())
			exportName = "Untitled";

		return new File(applicationManager.getCurrentDirectory(), exportName + FILE_EXTENSION);
	}

	@Override
	public ValidationState getValidationState(Appendable msg) {
		if (outputFormat.getSelectedValue() == null) {
			try {
				msg.append("Select a file type.");
			} catch (final Exception e) {
				/* Intentionally empty. */
			}

			return ValidationState.INVALID;
		}

		if (file == null) {
			try {
				msg.append("Enter a file name.");
			} catch (final Exception e) {
				/* Intentionally empty. */
			}

			return ValidationState.INVALID;
		}
		
		if (file.exists()) {
			try {
				msg.append("File already exists, are you sure you want to overwrite it?");
			} catch (final Exception e) {
				/* Intentionally empty. */
			}

			return ValidationState.REQUEST_CONFIRMATION;
		} else {
			return ValidationState.OK;
		}
	}

	@Override
	public void setUIHelper(TunableUIHelper helper) {
		this.helper = helper;
	}
}
