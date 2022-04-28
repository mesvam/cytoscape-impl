package org.cytoscape.io.internal.write.graphics.command;

import java.io.OutputStream;

import org.cytoscape.io.internal.write.graphics.PSWriter;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.work.Tunable;

public class ExportNetworkPSTask extends AbstractExportNetworkImageTask {
	
	
	@Tunable(
		longDescription = "If true (the default value), texts will be exported as fonts.",
		exampleStringValue = "true"
	)
	public boolean exportTextAsFont = true;
	
	
	@Tunable(
		longDescription = "If true then node and edge labels will not be visible in the image.",
		exampleStringValue = "false"
	)
	public boolean hideLabels;
	
	
	public ExportNetworkPSTask(CyServiceRegistrar registrar) {
		super(registrar);
	}

	@Override
	CyWriter createWriter(RenderingEngine<?> re, OutputStream outStream) {
		var writer = new PSWriter(re, outStream);
		writer.exportTextAsFont = exportTextAsFont;
		writer.hideLabels = hideLabels;
		return writer;
	}
	
}
