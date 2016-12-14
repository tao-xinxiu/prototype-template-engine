package com.orange.paas.cf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v2.spaces.SpaceApplicationSummary;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.Type;
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
		super(site);
		this.operations = new CloudFoundryOperations(site.getAccessInfo());
	}

	@Override
	public void prepareApp(OverviewApp app) {
		String packageId = operations.createPackage(app.getGuid(), PackageType.BITS, null);
		uploadPackageAndWaitUntilReady(packageId, app.getPath());
		String dropletId = operations.createDroplet(packageId, null, null);
		createDropletAndWaitUntilStaged(dropletId);
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
	public String createAppIfNotExist(OverviewApp app) {
		String appId = operations.getAppId(app.getName());
		if (appId != null) {
			throw new IllegalStateException(String
					.format("Exception in creating app: app named [%s] existed with id: [%s]", app.getName(), appId));
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
		logger.info("app [{}] at [{}] running", appId, site.getName());
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
	public void updateAppName(String appId, String name) {
		operations.updateApp(appId, name, null, null);
		logger.info("app [{}] name updated to [{}].", appId, name);
	}

	@Override
	public void updateAppEnv(String appId, Map<String, String> env) {
		operations.updateApp(appId, null, env, null);
		logger.info("app [{}] env updated to [{}].", appId, env);
	}

	@Override
	public OverviewSite getOverviewSite() {
		return new OverviewSite(operations.listSpaceApps()
				.parallelStream().map(appInfo -> new OverviewApp(appInfo.getId(), appInfo.getName(), null,
						parseState(appInfo), appInfo.getInstances(), parseEnv(appInfo), parseRoutes(appInfo)))
				.collect(Collectors.toList()));
	}

	private DropletState parseState(SpaceApplicationSummary appInfo) {
		if (appInfo.getRunningInstances() > 0) {
			return DropletState.RUNNING;
		}
		switch (appInfo.getPackageState()) {
		case "FAILED":
			return DropletState.FAILED;
		case "STAGED":
			return DropletState.STAGED;
		default:
			return DropletState.CREATED;
		}
	}

	private Map<String, String> parseEnv(SpaceApplicationSummary appInfo) {
		return appInfo.getEnvironmentJsons().entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> (String) entry.getValue()));
	}

	private List<Route> parseRoutes(SpaceApplicationSummary appInfo) {
		return appInfo.getRoutes().stream().map(route -> new Route(route.getHost(), route.getDomain().getName()))
				.collect(Collectors.toList());
	}

	@Override
	public void scaleApp(String appId, int instances) {
		operations.scaleProcess(appId, processType, instances);
	}
	
	@Override
	public List<Route> listAppRoutes(String appId) {
		List<Route> appRoutes = new ArrayList<>();
		for (String routeId : operations.listMappedRoutesId(appId)) {
			appRoutes.add(operations.getRoute(routeId));
		}
		return appRoutes;
	}

	@Override
	public void mapAppRoutes(String appId, List<Route> routes) {
		for (Route route : routes) {
			String domainId = operations.getDomainId(route.getDomain());
			String routeId = operations.getRouteId(route.getHostname(), domainId);
			if (routeId == null) {
				routeId = operations.createRoute(route.getHostname(), domainId);
			}
			operations.createRouteMapping(appId, routeId);
			logger.info("route [{}] mapped to the app [{}]", routeId, appId);
		}
	}

	@Override
	public void unmapAppRoutes(String appId, List<Route> routes) {
		for (Route route : routes) {
			String domainId = operations.getDomainId(route.getDomain());
			String routeId = operations.getRouteId(route.getHostname(), domainId);
			if (routeId != null) {
				String routeMappingId = operations.getRouteMappingId(appId, routeId);
				if (routeMappingId != null) {
					operations.deleteRouteMapping(routeMappingId);
				}
			}
			logger.info("route [{}] unmapped from the app [{}]", routeId, appId);
		}
	}
}
