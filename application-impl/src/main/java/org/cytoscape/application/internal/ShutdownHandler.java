package org.cytoscape.application.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyShutdown;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownRequestedEvent;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * Cytoscape Application Impl (application-impl)
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

public class ShutdownHandler implements CyShutdown {

	private static final Logger logger = LoggerFactory.getLogger(CyUserLog.NAME);
	
	private final Bundle rootBundle;
	private final CyServiceRegistrar serviceRegistrar;

	public ShutdownHandler(final Bundle rootBundle, final CyServiceRegistrar serviceRegistrar) {
		this.rootBundle = rootBundle;
		this.serviceRegistrar = serviceRegistrar;
	}

	@Override
	public void exit(int retVal) {
		exit(retVal, false);
	}

	@Override
	public void exit(int retVal, boolean force) {
		CyShutdownRequestedEvent request = new CyShutdownRequestedEvent(ShutdownHandler.this, force);
		serviceRegistrar.getService(CyEventHelper.class).fireEvent(request);
		
		if (!request.actuallyShutdown()) {
			logger.info("NOT shutting down, per listener instruction: " + request.abortShutdownReason());
			return;
		}
		
		// Notify all listeners that cytoscape is actually shutting down.
		CyShutdownEvent ev = new CyShutdownEvent(ShutdownHandler.this, force);
		serviceRegistrar.getService(CyEventHelper.class).fireEvent(ev);
		
		try {
			logger.info("#CiaoBello", rootBundle);
			CyApplicationConfiguration c = serviceRegistrar.getService(CyApplicationConfiguration.class);
			removeFailSafeFile(c.getConfigurationDirectoryLocation().getAbsolutePath());
			rootBundle.stop();
		} catch (BundleException e) {
			logger.error("Error while shutting down", e);
		}
	}

	/*
	 *  This is the close routine that pairs with CyApplicationConfigurationImpl's constructor
	 *  It removes a hidden file in the Cytoscape installation directory to show a graceful exit
	 */
	final String activeSessionFilename = "tracker.active.session";

	private void removeFailSafeFile(String configPath) {
		Path path = Paths.get(configPath, activeSessionFilename);
		if (path != null && path.toFile().exists())
		{
			// eventually we may want to report this contents. Now its just the version 
			try {
				Files.delete(path);
			} catch (IOException e) {
				logger.error("Could not clean up " + activeSessionFilename + " in " + configPath);
			}
		}
		else
		{
			// the file called .failsafe didn't exist in the right directory
			// implying the check was turned off (or perhaps configPath changed)
			logger.info(activeSessionFilename + " did not exist in " + configPath);
		}
	}
}	
