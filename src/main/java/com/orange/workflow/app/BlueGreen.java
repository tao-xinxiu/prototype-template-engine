package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class BlueGreen {
	private PaaSAPI api;
	private Application application;
	
	public BlueGreen(PaaSAPI api, Application application) {
		this.api = api;
		this.application = application;
	}
	
	public Step update() {
		return new Step(String.format("BlueGreen %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				Application greenApp = new Application(application);
				String greenAppName = application.getName() + application.getVersion();
				greenApp.setName(greenAppName);
				
				String greenAppId = api.createAppIfNotExist(greenApp);
				api.prepareApp(greenAppId, greenApp);
				String greenRouteId = api.createRouteIfNotExist(greenApp.getHostnames().get("tmp"), "tmp");
				api.createRouteMapping(greenAppId, greenRouteId);
				//green app mapped to temporary local route
				api.startAppAndWaitUntilRunning(greenAppId);
			}
		};
	}
	
	public Step commit() {
		return new Step(String.format("commit BlueGreen %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				String greenAppName = application.getName() + application.getVersion();
				String greenAppId = api.getAppId(greenAppName);
				// green app mapped to app original local and global route
				// map first global route to ensure that global route correctly mapped when DNS detects it available
				String globalRouteId = api.createRouteIfNotExist(application.getHostnames().get("global"), "global");
				api.createRouteMapping(greenAppId, globalRouteId);
				String localRouteId = api.createRouteIfNotExist(application.getHostnames().get("local"), "local");
				api.createRouteMapping(greenAppId, localRouteId);
				// unmap tmp route, delete old version app and rename new version app
				String greenRouteId = api.getRouteId(application.getHostnames().get("tmp"), "tmp");
				api.deleteRouteMapping(greenAppId, greenRouteId);
				String blueAppId = api.getAppId(application.getName());
				api.deleteApp(blueAppId);
				api.updateApp(greenAppId, application);
			}
		};
	}
	
	public Step rollback() {
		return new Step(String.format("rollback BlueGreen %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				String greenAppName = application.getName() + application.getVersion();
				String greenAppId = api.getAppId(greenAppName);
				api.deleteApp(greenAppId);
			}
		};
	}
}
