package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class Deploy {
	private PaaSAPI api;
	private Application application;

	public Deploy(PaaSAPI api, Application application) {
		this.api = api;
		this.application = application;
	}

	public Step update() {
		return new Step(String.format("Deploy %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				String appId = api.createAppIfNotExist(application);
				api.prepareApp(appId, application);

				String globalRouteId = api.createRouteIfNotExist(application.getHostnames().get("global"), "global");
				api.createRouteMapping(appId, globalRouteId);

				api.startAppAndWaitUntilRunning(appId);

				String localRouteId = api.createRouteIfNotExist(application.getHostnames().get("local"), "local");
				api.createRouteMapping(appId, localRouteId);
			}
		};
	}
}
