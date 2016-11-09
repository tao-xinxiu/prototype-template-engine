package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.paas.PaaSAPI;
import com.orange.workflow.Step;

public class Deploy {
	private PaaSAPI api;
	private Application desiredApp;

	public Deploy(PaaSAPI api, Application application) {
		this.api = api;
		this.desiredApp = application;
	}

	public Step update() {
		return new Step(String.format("Deploy %s.%s", api.getSiteName(), desiredApp.getName())) {
			@Override
			public void exec() {
				String appId = api.createAppIfNotExist(desiredApp);
				api.prepareApp(appId, desiredApp);

				String globalRouteId = api.createRouteIfNotExist(desiredApp.getHostnames().get("global"), "global");
				api.createRouteMapping(appId, globalRouteId);

				api.startAppAndWaitUntilRunning(appId);

				String localRouteId = api.createRouteIfNotExist(desiredApp.getHostnames().get("local"), "local");
				api.createRouteMapping(appId, localRouteId);
			}
		};
	}
}
