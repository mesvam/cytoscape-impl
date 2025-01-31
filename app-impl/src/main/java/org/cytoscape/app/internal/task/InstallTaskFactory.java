package org.cytoscape.app.internal.task;

import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class InstallTaskFactory extends AbstractTaskFactory {
	final AppManager appManager;

	public InstallTaskFactory(final AppManager appManager) {
		this.appManager = appManager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new InstallTask(appManager));
	}

	@Override
	public boolean isReady() { return true; }

}
