package org.cytoscape.ding.debug;

import java.util.List;

import org.cytoscape.ding.impl.work.ProgressMonitor;

public interface DebugProgressMonitor extends ProgressMonitor {

	List<DebugProgressMonitor> getSubMonitors();
	
	public long getStartTime();
	
	public long getEndTime();
	
	default public long getTime() {
		return getEndTime() - getStartTime();
	}
	
}
