package org.cytoscape.app.internal.task;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.app.internal.manager.App;
import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;

public class UninstallTask extends AbstractAppTask implements ObservableTask {
	@Tunable(description="Name of app to uninstall", 
	         longDescription="The name of the app to uninstall",
	         exampleStringValue="stringApp",
	         context="nogui", required=true)
	public String app = null;
  String error = null;
  App appObject = null;

	public UninstallTask(final AppManager appManager) {
		super(appManager);
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		if (app == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "App name not provided");
			return;
		}
		taskMonitor.setTitle("Uninstalling app "+app);
		updateApps();
		appObject = getApp(app);
		if (appObject == null) {
			error = "Can't find app '"+app+"'";
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, error);
			return;
		}
		appManager.uninstallApp(appObject);
		updateApps();
		String msg = "App '"+app+"' uninstalled";
		taskMonitor.showMessage(TaskMonitor.Level.INFO, msg);
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(String.class, JSONResult.class);
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (type.equals(JSONResult.class)) {
			JSONResult res = () -> {
        if (error != null)
          return "{\"error\": \""+(R)error+"\"}" ;
				return "{\"appName\": \""+app+"\"}";
			};
			return (R)res;
		} else if (type.equals(String.class)) {
      if (error != null)
        return (R)error;
		  String msg = "App '"+app+"' uninstalled";
      return (R)msg;
		}
		return null;
	}

}
