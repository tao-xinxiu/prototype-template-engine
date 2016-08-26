package com.orange.paas.cf;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.Type;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.Application;
import com.orange.model.PaaSTarget;
import com.orange.paas.PaaSAPI;

public class CloudFoundryAPI extends PaaSAPI {
	private CloudFoundryOperations operations;

	private static final int timeout = 5;
	private static final String processType = "web";
	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryAPI.class);

	public CloudFoundryAPI(PaaSTarget target) {
		super(target);
		this.operations = new CloudFoundryOperations(target);
	}

	@Override
	public void prepareApp(String appId, Application appProperty) {
		// TODO move log into operations
		String packageId = operations.createPackage(appId, PackageType.BITS, null);
		logger.info("package created with id: {}", packageId);
		logger.info("package uploading with timeout: {} minutes", timeout);
		operations.uploadPackage(packageId, appProperty.getPath(), 1000 * 60 * timeout);
		logger.info("package uploaded");
		while (operations.getPackageState(packageId) != State.READY) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("package ready");

		String dropletId = operations.createDroplet(packageId, null, null);
		logger.info("droplet created with id: {}", dropletId);
		while (operations.getDropletState(dropletId) != org.cloudfoundry.client.v3.droplets.State.STAGED) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("droplet staged");

		operations.stopApp(appId);
		logger.info("app stopped before assigning current droplet");
		operations.assignDroplet(appId, dropletId);
		logger.info("droplet assigned");
	}

	@Override
	public String createAppIfNotExist(Application appProperty) {
		String appId = operations.getAppId(appProperty.getName());
		if (appId != null) {
			logger.info("app existed with id: {}", appId);
			return appId;
		}
		assert appId == null;
		Lifecycle lifecycle = Lifecycle.builder().type(Type.BUILDPACK).data(BuildpackData.builder().buildpack(appProperty.getBuildpack()).stack(appProperty.getStack()).build()).build();
		appId = operations.createApp(appProperty.getName(), appProperty.getEnv(), lifecycle);
		logger.info("app created with id: {}", appId);
		return appId;
	}

	@Override
	public void startAppAndWaitUntilRunning(String appId) {
		operations.startApp(appId);
		logger.info("app starting");
		while (!operations.getProcessesState(appId, processType).contains("RUNNING")) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("app started");
	}

	@Override
	public void stopApp(String appId) {
		operations.stopApp(appId);
	}

	@Override
	public void deleteApp(String appId) {
		operations.deleteApp(appId);
	}

	@Override
	public String getLocalRouteId(String hostname) {
		return operations.getLocalRouteId(hostname);
	}

	@Override
	public String createLocalRouteIfNotExist(String hostname) {
		String localRouteId = operations.getLocalRouteId(hostname);
		if (localRouteId != null) {
			logger.info("local route existed with id: {}", localRouteId);
			return localRouteId;
		}
		localRouteId = operations.createLocalRoute(hostname);
		logger.info("local route created with id: {}", localRouteId);
		return localRouteId;
	}

	@Override
	public String createGlobalRouteIfNotExist(String hostname) {
		String globalRouteId = operations.getGlobalRouteId(hostname);
		if (globalRouteId != null) {
			logger.info("global route existed with id: {}", globalRouteId);
			return globalRouteId;
		}
		globalRouteId = operations.createGlobalRoute(hostname);
		logger.info("global route created with id: {}", globalRouteId);
		return globalRouteId;
	}

	@Override
	public void createRouteMapping(String appId, String routeId) {
		operations.createRouteMapping(appId, routeId);
	}

	@Override
	public void deleteRouteMapping(String appId, String routeId) {
		String routeMappingId = operations.getRouteMappingId(appId, routeId);
		if (routeMappingId != null) {
			logger.info("route-mapping id found: {}", routeMappingId);
			operations.deleteRouteMapping(routeMappingId);
			logger.info("route unmapped", routeMappingId);
		}
		else {
			logger.info("route-mapping not found");
		}
	}

	@Override
	public List<String> listSpaceAppsId() {
		List<String> spaceAppsId = new ArrayList<String>();
		for (ApplicationResource applicationResource : operations.listSpaceApps()) {
			spaceAppsId.add(applicationResource.getId());
		}
		return spaceAppsId;
	}

	@Override
	public String getAppId(String appName) {
		return operations.getAppId(appName);
	}
	
	@Override
	public String getAppName(String appId) {
		return operations.getAppName(appId);
	}

	@Override
	public void updateApp(String appId, Application appProperty) {
		Lifecycle lifecycle = Lifecycle.builder().type(Type.BUILDPACK).data(BuildpackData.builder().buildpack(appProperty.getBuildpack()).stack(appProperty.getStack()).build()).build();
		operations.updateApp(appId, appProperty.getName(), appProperty.getEnv(), lifecycle);
	}

	@Override
	public String getAppVersion(String appId) {
		return (String) operations.getAppEnv(appId, "APP_VERSION");
	}
}
