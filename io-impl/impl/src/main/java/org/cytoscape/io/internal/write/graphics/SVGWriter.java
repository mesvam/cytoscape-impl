package org.cytoscape.io.internal.write.graphics;

import java.awt.Dimension;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
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

public class SVGWriter extends AbstractTask implements CyWriter {
	
	private static final Logger logger = LoggerFactory.getLogger(CyUserLog.NAME);

	@Tunable(
			description = "Export text as font:",
			longDescription = "If true (the default value), texts will be exported as fonts.",
			groups = { "_Others" },
			gravity = 2.1
	)
	public boolean exportTextAsFont = true;
	
	@Tunable(
			description = "Hide Labels:",
			longDescription = "If true then node and edge labels will not be visible in the image.",
			exampleStringValue = "true",
			groups = { "_Others" },
			gravity = 2.2
	)
	public boolean hideLabels;

	
	@ProvidesTitle
	public String getTitle() {
		return "Export Network as SVG";
	}
	
	private final Double width;
	private final Double height;
	private final RenderingEngine<?> engine;
	private final OutputStream stream;


	public SVGWriter(final RenderingEngine<?> engine, final OutputStream stream) {
		if (engine == null)
			throw new NullPointerException("Rendering Engine is null.");
		if (stream == null)
			throw new NullPointerException("Stream is null.");
		
		this.engine = engine;
		this.stream = stream;

		width = engine.getViewModel().getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH);
		height = engine.getViewModel().getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT);

		logger.debug("SVG Writer created.");
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		tm.setTitle("SVG Writer");
		tm.setStatusMessage("SVG Rendering Start...");
		tm.setProgress(0.0);
		logger.debug("SVG Rendering Start.");

		final SVGGraphics2D g = new SVGGraphics2D(stream, new Dimension(width.intValue(), height.intValue()));

		// this sets text as shape
		final Properties p = new Properties();
		p.setProperty("org.freehep.graphicsio.AbstractVectorGraphicsIO.TEXT_AS_SHAPES", Boolean.toString(!exportTextAsFont));
		g.setProperties(p);
		
		tm.setProgress(0.2);
		
		g.startExport();
		var props = Map.of("exportHideLabels", String.valueOf(hideLabels));
		engine.printCanvas(g, props);
		g.endExport();
		
		logger.debug("SVG Rendering DONE.");
		tm.setStatusMessage("SVG Rendering DONE.");
		tm.setProgress(1.0);
	}
}
