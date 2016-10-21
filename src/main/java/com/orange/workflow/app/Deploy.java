package com.orange.workflow.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class Deploy extends Step {
	private static final Logger logger = LoggerFactory.getLogger(Deploy.class);

	private PaaSAPI api;
	private Application application;

	public Deploy(PaaSAPI api, Application application) {
		super(String.format("Deploy %s.%s", api.getTargetName(), application.getName()));
		this.api = api;
		this.application = application;
	}

	public void exec() {
		logger.info("start {} app: {} on the target: {}", this.getClass().getSimpleName(), application, api.getTargetName());
		
		String appId = api.createAppIfNotExist(application);
		api.prepareApp(appId, application);

		String globalRouteId = api.createRouteIfNotExist(application.getHostnames().get("global"), "global");
		api.createRouteMapping(appId, globalRouteId);

		api.startAppAndWaitUntilRunning(appId);

		String localRouteId = api.createRouteIfNotExist(application.getHostnames().get("local"), "local");
		api.createRouteMapping(appId, localRouteId);

		logger.info("Step {} Done! App: {} running on the target: {}", this.getClass().getSimpleName(), application,
				api.getTargetName());
	}
}
