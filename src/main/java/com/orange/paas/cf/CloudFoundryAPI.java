package com.orange.paas.cf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.Type;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.*;
import com.orange.paas.PaaSAPI;

public class CloudFoundryAPI extends PaaSAPI {
	private static final int timeout = 5;
	private static final String processType = "web";
	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryAPI.class);
	private CloudFoundryOperations operations;

	public CloudFoundryAPI(PaaSSite site) {
		super(site, new CloudFoundryRouteFactory(site));
		this.operations = ((CloudFoundryRouteFactory) this.routeFactory).getOperations();
	}

	@Override
	public String prepareApp(String appId, Application appProperty) {
		String packageId = operations.createPackage(appId, PackageType.BITS, null);
		uploadPackageAndWaitUntilReady(packageId, appProperty.getPath());
		String dropletId = operations.createDroplet(packageId, null, null);
		createDropletAndWaitUntilStaged(dropletId);
		return dropletId;
	}
	
	@Override
	public String createDroplet(String appId, OverviewDroplet droplet){
		String packageId = operations.createPackage(appId, PackageType.BITS, null);
		uploadPackageAndWaitUntilReady(packageId, droplet.getPath());
		String dropletId = operations.createDroplet(packageId, null, null);
		createDropletAndWaitUntilStaged(dropletId);
		return dropletId;
	}

	@Override
	public void assignDroplet(String appId, String dropletId) {
		// app should be stopped before assigning current droplet
		operations.stopApp(appId);
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
	public String createAppWithOneDroplet(OverviewApp app) {
		assert app.getDroplets().size() == 1;
		OverviewDroplet droplet = app.getDroplets().get(0);
		String appId = operations.getAppId(app.getName());
		if (appId != null) {
			logger.info("app existed with id: {}", appId);
			return appId;
		}
		assert appId == null;
		Lifecycle lifecycle = Lifecycle.builder().type(Type.BUILDPACK).data(
				BuildpackData.builder().build())
				.build();
		appId = operations.createApp(app.getName(), droplet.getEnv(), lifecycle);
		logger.info("app created with id: {}", appId);
		return appId;
	}

	@Override
	public void startAppAndWaitUntilRunning(String appId) {
		operations.startApp(appId);
		while (!operations.listProcessesState(appId, processType).contains("RUNNING")) {
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
		logger.info("app {} updated with name {}; env {}; lifecycle {}.", appId, appProperty.getName(),
				appProperty.getEnv(), lifecycle);
	}

	@Override
	// get user provided droplet env
	public Map<String, String> listDropletEnv(String appId, String dropletId) {
		return operations.getDropletEnv(appId, dropletId);
	}

	@Override
	public List<String> listAppDropletsId(String appId) {
		return operations.listAppDropletsId(appId);
	}

	@Override
	public DropletState getAppDropletState(String appId, String dropletId) {
		if (appId == null || dropletId == null) {
			throw new IllegalStateException(
					String.format("Exception in getDropletState with appId [%s], dropletId [%s]", appId, dropletId));
		}
		if (dropletId.equals(operations.getCurrentDropletId(appId))
				&& operations.listProcessesState(appId, processType).contains("RUNNING")) {
			return DropletState.RUNNING;
		} else {
			org.cloudfoundry.client.v3.droplets.State dropletState = operations.getDropletState(dropletId);
			switch (dropletState) {
			case STAGED:
				return DropletState.STAGED;
			case FAILED:
			case EXPIRED:
				return DropletState.FAILED;
			default:// case PENDING: case STAGING:
				return DropletState.CREATED;
			}
		}
	}
}
