package org.cytoscape.app.internal.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ListUpdatesTaskFactory extends AbstractTaskFactory {
	final AppManager appManager;

	public ListUpdatesTaskFactory(final AppManager appManager) {
		this.appManager = appManager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ListUpdatesTask(appManager));
	}

	@Override
	public boolean isReady() { return true; }

}
