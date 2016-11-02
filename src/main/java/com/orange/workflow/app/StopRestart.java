package com.orange.workflow.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class StopRestart {
	private static final Logger logger = LoggerFactory.getLogger(StopRestart.class);
	private static final int waitDNS = 5; // wait DNS cache in min

	private PaaSAPI api;
	private Application application;

	public StopRestart(PaaSAPI api, Application application) {
		this.api = api;
		this.application = application;
	}
	
	public Step update() {
		return new Step(String.format("StopRestart %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				String appId = api.createAppIfNotExist(application);
				
				String localHostname = application.getHostnames().get("local");
				String localRouteId = api.getRouteId(localHostname, "local");
				api.deleteRouteMapping(appId, localRouteId);
				
				logger.info("start waiting {} min for DNS cache", waitDNS);
				try {
					Thread.sleep(waitDNS * 1000 * 60);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
				api.prepareApp(appId, application);
				api.startAppAndWaitUntilRunning(appId);
				
				localRouteId = api.createRouteIfNotExist(localHostname, "local");
				api.createRouteMapping(appId, localRouteId);
			}
		};
	}
}
