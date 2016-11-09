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
import com.orange.model.PaaSSite;
import com.orange.paas.PaaSAPI;

public class CloudFoundryAPI extends PaaSAPI {
	private static final int timeout = 5;
	private static final String processType = "web";
	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryAPI.class);
	private CloudFoundryOperations operations;
	
	public CloudFoundryAPI(PaaSSite site) {
		super(site, new CloudFoundryRouteFactory(site));
		this.operations = ((CloudFoundryRouteFactory)this.routeFactory).getOperations();
	}
	
	@Override
	public void prepareApp(String appId, Application appProperty) {
		String packageId = operations.createPackage(appId, PackageType.BITS, null);
		uploadPackageAndWaitUntilReady(packageId, appProperty.getPath());
		String dropletId = operations.createDroplet(packageId, null, null);
		createDropletAndWaitUntilStaged(dropletId);
		operations.stopApp(appId); //app should be stopped before assigning current droplet
		operations.assignDroplet(appId, dropletId);	
	}
	
	private void uploadPackageAndWaitUntilReady(String packageId, String bitsPath) {
		operations.uploadPackage(packageId, bitsPath, 60 * timeout);
		while (operations.getPackageState(packageId) != State.READY) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("package ready");
	}
	
	private void createDropletAndWaitUntilStaged(String dropletId) {
		while (operations.getDropletState(dropletId) != org.cloudfoundry.client.v3.droplets.State.STAGED) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("droplet staged");
	}

	@Override
	public String createAppIfNotExist(Application appProperty) {
		String appId = operations.getAppId(appProperty.getName());
		if (appId != null) {
			logger.info("app existed with id: {}", appId);
			return appId;
		}
		assert appId == null;
		Lifecycle lifecycle = Lifecycle.builder().type(Type.BUILDPACK).data(
				BuildpackData.builder().buildpack(appProperty.getBuildpack()).stack(appProperty.getStack()).build())
				.build();
		appId = operations.createApp(appProperty.getName(), appProperty.getEnv(), lifecycle);
		logger.info("app created with id: {}", appId);
		return appId;
	}

	@Override
	public void startAppAndWaitUntilRunning(String appId) {
		operations.startApp(appId);
		while (!operations.getProcessesState(appId, processType).contains("RUNNING")) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("app {} at {} running", appId, site.getName());
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
		Lifecycle lifecycle = Lifecycle.builder().type(Type.BUILDPACK).data(
				BuildpackData.builder().buildpack(appProperty.getBuildpack()).stack(appProperty.getStack()).build())
				.build();
		operations.updateApp(appId, appProperty.getName(), appProperty.getEnv(), lifecycle);
		logger.info("app {} updated with name {}; env {}; lifecycle {}.", appId, appProperty.getName(), appProperty.getEnv(), lifecycle);
	}

	@Override
	public Object getAppEnv(String appId, String envKey) {
		return operations.getAppEnv(appId).get(envKey);
	}
}
