package com.orange.workflow.app;

import java.util.UUID;

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
				String greenAppSuffix = UUID.randomUUID().toString().replace("-", "");
				greenApp.setName(application.getName() + greenAppSuffix);
				
				String greenAppId = api.createAppIfNotExist(greenApp);
				api.prepareApp(greenAppId, greenApp);
				String greenRouteId = api.createRouteIfNotExist(greenApp.getHostnames().get("local") + greenAppSuffix, "local");
				api.createRouteMapping(greenAppId, greenRouteId);
				//green app mapped to temporary local route
				api.startAppAndWaitUntilRunning(greenAppId);
				
				//TODO add test for green app
				// green app mapped to app original local and global route
				String localRouteId = api.getRouteId(greenApp.getHostnames().get("local"), "local");
				api.createRouteMapping(greenAppId, localRouteId);
				String globalRouteId = api.getRouteId(greenApp.getHostnames().get("global"), "global");
				api.createRouteMapping(greenAppId, globalRouteId);
				
				api.deleteRouteMapping(greenAppId, greenRouteId);
				String blueAppId = api.getAppId(application.getName());
				api.deleteApp(blueAppId);
				api.updateApp(greenAppId, application);
			}
		};
	}
	
}
