package com.orange.paas.cf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.Type;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.droplets.DropletResource;
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
	public String prepareDroplet(String appId, OverviewDroplet droplet) {
		String packageId = operations.createPackage(appId, PackageType.BITS, null);
		uploadPackageAndWaitUntilReady(packageId, droplet.getPath());
		String dropletId = operations.createDroplet(packageId, null, null);
		createDropletAndWaitUntilStaged(dropletId);
		return dropletId;
	}

	@Override
	public void deleteDroplet(String dropletId) {
		operations.deleteDroplet(dropletId);
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
	public String createAppWithOneDroplet(OverviewApp app) {
		assert app.getDroplets().size() == 1;
		OverviewDroplet droplet = app.getDroplets().get(0);
		String appId = operations.getAppId(app.getName());
		if (appId != null) {
			throw new IllegalStateException(String.format("app existed with id: [%s]", appId));
		}
		Lifecycle lifecycle = Lifecycle.builder().type(Type.BUILDPACK).data(BuildpackData.builder().build()).build();
		appId = operations.createApp(app.getName(), droplet.getEnv(), lifecycle);
		logger.info("app created with id: {}", appId);
		return appId;
	}

	@Override
	public String createAppIfNotExist(OverviewApp app) {
		String appId = operations.getAppId(app.getName());
		if (appId != null) {
			throw new IllegalStateException(String.format("app existed with id: [%s]", appId));
		}
		Lifecycle lifecycle = Lifecycle.builder().type(Type.BUILDPACK).data(BuildpackData.builder().build()).build();
		appId = operations.createApp(app.getName(), null, lifecycle);
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
	public void updateApp(OverviewApp app, Map<String, String> env) {
		operations.updateApp(app.getGuid(), app.getName(), env, null);
		logger.info("app {} updated with name {}; env {}.", app.getGuid(), app.getName(), env);
	}

	@Override
	public void updateApp(OverviewApp app) {
		operations.updateApp(app.getGuid(), app.getName(), null, null);
		logger.info("app {} updated with name {}.", app.getGuid(), app.getName());
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
		return toDropletState(operations.getDropletState(dropletId), appId, dropletId,
				operations.getCurrentDropletId(appId));
	}

	@Override
	public OverviewSite getOverviewSite() {
		return new OverviewSite(operations.listSpaceApps().parallelStream()
				.map(appInfo -> new OverviewApp(appInfo.getId(), appInfo.getName(), listAppRoutes(appInfo.getId()), listOverviewDroplets(appInfo.getId())))
				.collect(Collectors.toList()));
	}

	private List<OverviewDroplet> listOverviewDroplets(String appId) {
		List<OverviewDroplet> overviewDroplets = new ArrayList<>();
		// to min request
		String currentDropletId = operations.getCurrentDropletId(appId);
		for (DropletResource dropletInfo : operations.listAppDroplets(appId)) {
			String dropletId = dropletInfo.getId();
			DropletState state = toDropletState(dropletInfo.getState(), appId, dropletId, currentDropletId);
			if (state == DropletState.RUNNING) {
				overviewDroplets.add(
						new OverviewDroplet(dropletId, null, state, operations.getProcessesInstance(appId, processType),
								operations.getDropletEnv(appId, dropletId)));
			} else {
				overviewDroplets.add(
						new OverviewDroplet(dropletId, null, state, 0, operations.getDropletEnv(appId, dropletId)));
			}
		}
		return overviewDroplets;
	}

	private DropletState toDropletState(org.cloudfoundry.client.v3.droplets.State dropletState, String appId,
			String dropletId, String currentDropletId) {
		if (isCurrentDroplet(dropletId, currentDropletId) && isAppRunning(appId)) {
			return DropletState.RUNNING;
		} else {
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

	private boolean isCurrentDroplet(String dropletId, String currentDropletId) {
		return dropletId == null ? false : dropletId.equals(currentDropletId);
	}

	private boolean isAppRunning(String appId) {
		return operations.listProcessesState(appId, processType).contains("RUNNING");
	}

	@Override
	public void scaleApp(String appId, int instances) {
		operations.scaleProcess(appId, processType, instances);
	}
}
