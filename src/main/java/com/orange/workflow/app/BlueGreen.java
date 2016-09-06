package com.orange.workflow.app;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class BlueGreen extends Step {
	private PaaSAPI api;
	private Application application;
	
	private static final Logger logger = LoggerFactory.getLogger(BlueGreen.class);

	public BlueGreen(PaaSAPI api, Application application) {
		super(String.format("BlueGreen %s.%s", api.getTargetName(), application.getName()));
		this.api = api;
		this.application = application;
	}

	public void exec() {
		logger.info("start {} app: {} on the target: {}", this.getClass().getSimpleName(), application,
				api.getTargetName());
		Application greenApp = new Application(application);
		String greenAppSuffix = UUID.randomUUID().toString().replace("-", "");
		greenApp.setName(application.getName() + greenAppSuffix);
		
		String greenAppId = api.createAppIfNotExist(greenApp);
		api.prepareApp(greenAppId, greenApp);
		String greenRouteId = api.createLocalRouteIfNotExist(greenApp.getHostnames().get("local") + greenAppSuffix);
		api.createRouteMapping(greenAppId, greenRouteId);
		logger.info("green app mapped to temporary local route");
		api.startAppAndWaitUntilRunning(greenAppId);
		
		//TODO add test for green app
		String localRouteId = api.getLocalRouteId(greenApp.getHostnames().get("local"));
		api.createRouteMapping(greenAppId, localRouteId);
		logger.info("green app mapped to app original local route");
		String globalRouteId = api.getGlobalRouteId(greenApp.getHostnames().get("global"));
		api.createRouteMapping(greenAppId, globalRouteId);
		logger.info("green app mapped to app original global route");
		
		//TODO may insert further test from app origin route
		api.deleteRouteMapping(greenAppId, greenRouteId);
		logger.info("green app unmapped to temporary local route");
		String blueAppId = api.getAppId(application.getName());
		api.deleteApp(blueAppId);
		
		api.updateApp(greenAppId, application);
		
		logger.info("Step {} Done! App: {} running on the target: {}", this.getClass().getSimpleName(), application,
				api.getTargetName());
	}
}
