package com.orange.strategy.app;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.cf.operations.PaaSClient;
import com.orange.model.Application;
import com.orange.model.Step;

public class Deploy extends Step {
	private static final String processType = "web";
	private static final int timeout = 5;
	private static final Logger logger = LoggerFactory.getLogger(Deploy.class);

	private PaaSClient client;
	private Application application;

	public Deploy(PaaSClient client, Application application) {
		super(String.format("Deploy %s.%s", client.getTargetName(), application.getName()));
		this.client = client;
		this.application = application;
	}

	public void exec() {
		logger.info("start {} app: {} on the target: {}", this.getClass().getName(), application,
				client.getTargetName());
		String appId = client.getAppId(application.getName());
		assert appId == null : "Should not deploy existed app!";
		Map<String, String> env = new HashMap<String, String>();
		env.put("APP_VERSION", application.getVersion());
		appId = client.createApp(application.getName(), env, null);
		logger.info("app created with id: {}", appId);
		
		String packageId = client.createPackage(appId, PackageType.BITS, null);
		logger.info("package created with id: {}", packageId);
		logger.info("package uploading with timeout: {} minutes", timeout);
		client.uploadPackage(packageId, application.getPath(), 1000 * 60 * timeout);
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
		client.assignDroplet(appId, dropletId);
		logger.info("droplet assigned");
		
		String localRouteId = client.getLocalRouteId(application.getLocalHostname());
		if (localRouteId == null) {
			client.createLocalRoute(application.getLocalHostname());
			logger.info("local route created with id: {}", localRouteId);
		} else {
			logger.info("local route existed with id: {}", localRouteId);
		}
		client.createRouteMapping(appId, localRouteId);
		logger.info("local route mapping created");
		String globalRouteId = client.getGlobalRouteId(application.getGlobalHostname());
		if (globalRouteId == null) {
			client.createGlobalRoute(application.getGlobalHostname());
			logger.info("global route created with id: {}", globalRouteId);
		} else {
			logger.info("global route existed with id: {}", globalRouteId);
		}
		client.createRouteMapping(appId, globalRouteId);
		logger.info("global route mapping created");
		
		client.startApp(appId);
		logger.info("app starting");
		while (!client.getProcessesState(appId, processType).contains("RUNNING")) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("Step {} Done! App: {} running on the target: {}", this.getClass().getName(), application,
				client.getTargetName());
	}
}
