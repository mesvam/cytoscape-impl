package org.cytoscape.app.internal.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class AppManagerTaskFactory extends AbstractTaskFactory {
	final AppManager appManager;
	final CyServiceRegistrar serviceRegistrar;
	final CySwingApplication swingApplication;

	public AppManagerTaskFactory(final AppManager appManager, final CyServiceRegistrar serviceRegistrar, CySwingApplication swingApplication) {
		this.appManager = appManager;
		this.serviceRegistrar = serviceRegistrar;
		this.swingApplication = swingApplication;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AppManagerTask(appManager, serviceRegistrar, swingApplication));
	}

	@Override
	public boolean isReady() { return true; }

}
