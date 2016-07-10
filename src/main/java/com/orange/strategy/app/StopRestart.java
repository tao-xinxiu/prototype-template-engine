package com.orange.strategy.app;

import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.cf.operations.PaaSClient;
import com.orange.model.Application;
import com.orange.model.Step;

public class StopRestart extends Step {
	private static final Logger logger = LoggerFactory.getLogger(StopRestart.class);
	private static final int uploadTimeout = 5; // upload package timeout in min
	private static final int waitDNS = 5; // wait DNS cache in min
	private static final String processType = "web";

	private PaaSClient client;
	private Application application;

	public StopRestart(PaaSClient client, Application application) {
		super(String.format("StopRestart %s.%s", client.getTargetName(), application.getName()));
		this.client = client;
		this.application = application;
	}

	@Override
	public void exec() {
		logger.info("start {} app: {} on the target: {}", this.getClass().getName(), application,
				client.getTargetName());
		String appId = client.getAppId(application.getName());
		// StopRestart should only be used when app existed.
		assert appId != null : "Should not StopRestart not existed app!";
		logger.info("app id found: {}", appId);
		
		String localRouteId = client.getLocalRouteId(application.getLocalHostname());
		if (localRouteId == null) {
			logger.info("local route not existed");
		} else {
			logger.info("local route id found: {} for hostname {}", localRouteId, application.getLocalHostname());
			String routeMappingId = client.getRouteMappingId(appId, localRouteId);
			logger.info("route-mapping id found: {}", routeMappingId);
			client.deleteRouteMapping(routeMappingId);
			logger.info("local route unmapped", routeMappingId);
			logger.info("start waiting {} min for DNS cache", waitDNS);
			try {
				Thread.sleep(waitDNS * 1000 * 60);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		
		String packageId = client.createPackage(appId, PackageType.BITS, null);
		logger.info("package created with id: {}", packageId);
		logger.info("package uploading with timeout: {} minutes", uploadTimeout);
		client.uploadPackage(packageId, application.getPath(), 1000 * 60 * uploadTimeout);
		logger.info("package uploaded");
		while (client.getPackageState(packageId) != State.READY) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("package ready");

		String dropletId = client.createDroplet(packageId, null, null);
		logger.info("droplet created with id: {}", dropletId);
		while (client.getDropletState(dropletId) != org.cloudfoundry.client.v3.droplets.State.STAGED) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("droplet staged");
		client.stopApp(appId);
		logger.info("app stopped before assigning current droplet");
		client.assignDroplet(appId, dropletId);
		logger.info("droplet assigned");

		client.startApp(appId);
		logger.info("app starting");
		while (!client.getProcessesState(appId, processType).contains("RUNNING")) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("app started");
		
		localRouteId = client.getLocalRouteId(application.getLocalHostname());
		if (localRouteId == null) {
			client.createLocalRoute(application.getLocalHostname());
			logger.info("local route created with id: {}", localRouteId);
		} else {
			logger.info("local route existed with id: {}", localRouteId);
		}
		client.createRouteMapping(appId, localRouteId);
		logger.info("local route mapping created");
		
		logger.info("Step {} Done! App: {} running on the target: {}", this.getClass().getName(), application,
				client.getTargetName());
	}
}
