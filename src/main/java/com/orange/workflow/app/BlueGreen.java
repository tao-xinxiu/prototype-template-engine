package com.orange.workflow.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class BlueGreen extends Step {
	private PaaSAPI api;
	private Application application;
	
	private static final Logger logger = LoggerFactory.getLogger(StopRestart.class);
	private static final String greenAppSuffix = "-green";
	private static final String greenRouteSuffix = "-green";

	public BlueGreen(PaaSAPI api, Application application) {
		super(String.format("BlueGreen %s.%s", api.getTargetName(), application.getName()));
		this.api = api;
		this.application = application;
	}

	public void exec() {
		logger.info("start {} app: {} on the target: {}", this.getClass().getName(), application,
				api.getTargetName());
		String appOriginName = application.getName();
		application.setName(appOriginName + greenAppSuffix);
		String greenAppId = api.createAppIfNotExist(application);
		api.prepareApp(greenAppId, application);
		String greenRouteId = api.createLocalRouteIfNotExist(application.getLocalHostname() + greenRouteSuffix);
		api.createRouteMapping(greenAppId, greenRouteId);
		logger.info("green app mapped to temporary local route");
		api.startAppAndWaitUntilRunning(greenAppId);
		
		//TODO add test for green app
		String localRouteId = api.getLocalRouteId(application.getLocalHostname());
		api.createRouteMapping(greenAppId, localRouteId);
		logger.info("green app mapped to app original local route");
		String globalRouteId = api.getGlobalRouteId(application.getGlobalHostname());
		api.createRouteMapping(greenAppId, globalRouteId);
		logger.info("green app mapped to app original global route");
		
		//TODO may insert further test from app origin route
		api.deleteRouteMapping(greenAppId, greenRouteId);
		logger.info("green app unmapped to temporary local route");
		String blueAppId = api.getAppId(appOriginName);
		api.deleteApp(blueAppId);
		
		application.setName(appOriginName);
		api.updateApp(greenAppId, application);
		
		logger.info("Step {} Done! App: {} running on the target: {}", this.getClass().getName(), application,
				api.getTargetName());
	}
}
