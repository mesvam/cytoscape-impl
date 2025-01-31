package org.cytoscape.task.internal.vizmap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.cytoscape.io.read.VizmapReader;
import org.cytoscape.io.read.VizmapReaderManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

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

public class LoadVizmapFileTask extends AbstractTask {

	@ProvidesTitle
	public String getTitle() {
		return "Import Style";
	}
	
	@Tunable(
			description = "Styles File:",
			longDescription = "XML or properties file where one or more styles have been saved to.",
			exampleStringValue = "/Users/johndoe/Downloads/MyStyles.xml",
			required = true,
			params = "fileCategory=vizmap;input=true"
	)
	public File file;

	private AddVisualStylesTask addStyleTask;
	
	private final CyServiceRegistrar serviceRegistrar;

	public LoadVizmapFileTask(CyServiceRegistrar serviceRegistrar) {
		this.serviceRegistrar = serviceRegistrar;
	}

	@Override
	public void run(final TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setProgress(0.0);
		taskMonitor.setTitle("Load Styles from File");
		taskMonitor.setStatusMessage("Looking for a reader...");
		
		if (file == null) 
			throw new NullPointerException("No file specified.");

		VizmapReaderManager readerManager = serviceRegistrar.getService(VizmapReaderManager.class);
		VizmapReader reader = readerManager.getReader(file.toURI(), file.getName());
		taskMonitor.setProgress(0.9);

		if (reader == null) 
			throw new NullPointerException("Failed to find appropriate reader for file: " + file);

		addStyleTask = new AddVisualStylesTask(reader);

		insertTasksAfterCurrentTask(reader, addStyleTask);
		taskMonitor.setProgress(1.0);
	}

	public Set<VisualStyle> getStyles() {
		return addStyleTask.getStyles();
	}
	
	private class AddVisualStylesTask extends AbstractTask implements ObservableTask {

		private final VizmapReader reader;
		private Set<VisualStyle> styles; 

		public AddVisualStylesTask(VizmapReader reader) {
			this.reader = reader;
		}

		@Override
		public void run(TaskMonitor taskMonitor) throws Exception {
			taskMonitor.setProgress(0.0);
			taskMonitor.setTitle("Add Styles");
			taskMonitor.setStatusMessage("Adding styles...");
			styles = reader.getVisualStyles();

			if (styles != null) {
				int count = 1;
				int total = styles.size();
				VisualMappingManager vmManager = serviceRegistrar.getService(VisualMappingManager.class);

				for (VisualStyle vs : styles) {
					if (cancelled)
						break;
					
					taskMonitor.setStatusMessage(count + " of " + total + ": " + vs.getTitle());
					vmManager.addVisualStyle(vs);
					taskMonitor.setProgress(count / total);
					count++;
				}

				if (cancelled) {
					// Remove recently added styles
					for (VisualStyle vs : styles)
						vmManager.removeVisualStyle(vs);
				}
			}
			
			taskMonitor.setProgress(1.0);
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Object getResults(Class type) {
			if (type == List.class || type == Collection.class)
				return styles != null ? new ArrayList<>(styles) : Collections.emptyList();
			
			if (type == String.class) {
				String res = "";
				
				if (styles != null && !styles.isEmpty()) {
					res += "Loaded styles:\n";
					
					for (VisualStyle style : styles)
						res += style.toString() + "\n";
					
					res = res.substring(0, res.length() - 1);
				} else {
					res = "No styles were loaded.";
				}
				
				return res;
			}
			
			if (type == JSONResult.class) {
				JsonArray jsonArr = new JsonArray();
				
				if (styles != null && !styles.isEmpty()) {
					for (VisualStyle vs : styles)
						jsonArr.add(new JsonPrimitive(vs.getTitle()));
				}
				
				String json = new Gson().toJson(jsonArr);
				JSONResult res = () -> { return json; };
				
				return res;
			}
			
			return styles;
		}
		
		@Override
		public List<Class<?>> getResultClasses() {
			return Arrays.asList(String.class, List.class, JSONResult.class);
		}

		public Set<VisualStyle> getStyles() {
			return styles; 
		}
	}
}
