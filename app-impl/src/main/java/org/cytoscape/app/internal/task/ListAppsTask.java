package org.cytoscape.app.internal.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.cytoscape.app.internal.manager.App;
import org.cytoscape.app.internal.manager.App.AppStatus;
import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.app.internal.net.WebApp;
import org.cytoscape.app.internal.net.WebQuerier;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.json.JSONResult;
import org.json.JSONException;
import org.json.JSONObject;

public class ListAppsTask extends AbstractAppTask implements ObservableTask {
	AppStatus status;
	public ListAppsTask(final AppManager appManager, final AppStatus status) {
		super(appManager);
		this.status = status;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Listing "+status.toString()+" apps");
		updateApps();
	}

	@Override
	public List<Class<?>> getResultClasses() {
		return Arrays.asList(List.class, String.class, JSONResult.class);
	}
	
	public void appendJSONField(JSONObject appJSON, final String field, final String value) {
		
		try {
			appJSON.put(field, value);
		} catch (JSONException e) {
			try {
				appJSON.put(field, "#Invalid JSON#");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public <R> R getResults(Class<? extends R> type) {
		List<App> statusAppList = getApps(status);
		if (type.equals(JSONResult.class)) {
			JSONResult res = () -> {
				StringBuilder stringBuilder = new StringBuilder("[");
				int count = statusAppList.size();
				int index = 0;
				for (App app: statusAppList) {
					JSONObject appJSON = new JSONObject();
					appendJSONField(appJSON, "appName", app.getAppName());
					appendJSONField(appJSON, "version", app.getVersion());
					appendJSONField(appJSON, "description", app.getDescription());
					appendJSONField(appJSON, "status", app.getReadableStatus());
					stringBuilder.append(appJSON.toString());
					index++;
					if (index < count)
						stringBuilder.append(",");
				}
				stringBuilder.append("]");
				return stringBuilder.toString();
			};
			return (R)res;
		} else if (type.equals(String.class)) {
			List<String> appList = new ArrayList<String>(statusAppList.size());
			for (App app: statusAppList) {
				appList.add("name: "+app.getAppName()+
				            ", version: "+app.getVersion()+
				            ", status: "+app.getReadableStatus());
			}
			Collections.sort(appList);
			String list = "";
			for (String app: appList) {
				list += app+"\n";
			}
			return (R)list;
		} else if (type.equals(List.class)) {
			List<String> list = new ArrayList<>();
			for (App app: statusAppList) {
				list.add(app.getAppName());
			}
			return (R)list;
		}
		return null;
	}

}
