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
		logger.info("start {} app: {} on the target: {}", this.getClass().getName(), application, api.getTargetName());
		String appId = api.createAppIfNotExist(application);
		api.prepareApp(appId, application);

		String globalRouteId = api.createGlobalRouteIfNotExist(application.getGlobalHostname());
		api.createRouteMapping(appId, globalRouteId);
		logger.info("global route mapping created");

		api.startAppAndWaitUntilRunning(appId);

		// local route (used by AWS Route 53) should be mapped after app running
		String localRouteId = api.createLocalRouteIfNotExist(application.getLocalHostname());
		api.createRouteMapping(appId, localRouteId);
		logger.info("local route mapping created");

		logger.info("Step {} Done! App: {} running on the target: {}", this.getClass().getName(), application,
				api.getTargetName());
	}
}
