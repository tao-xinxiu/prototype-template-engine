package com.orange.paas.cf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v2.spaces.SpaceApplicationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.*;
import com.orange.paas.PaaSAPI;

public class CloudFoundryAPIv2 extends PaaSAPI {
	private static final int uploadTimeout = 60 * 5;
	private final Logger logger;
	private CloudFoundryOperations operations;

	public CloudFoundryAPIv2(PaaSSite site) {
		super(site);
		this.operations = new CloudFoundryOperations(site.getAccessInfo());
		this.logger = LoggerFactory.getLogger(String.format("%s(%s)", getClass(), site.getName()));
	}

	@Override
	public String createAppWaitUploaded(OverviewApp app) {
		String appId = operations.createApp(app);
		logger.info("App [{}] created with id: [{}].", app.getName(), appId);
		operations.uploadApp(appId, app.getPath(), uploadTimeout);
		return appId;
	}

	@Override
	public void stageAppWaitStaged(String appId) {
		throw new UnsupportedOperationException(
				"CloudFoundryAPIv2 not support stage app without starting it for the moment.");
	}

	@Override
	public void startAppWaitRunning(String appId) {
		operations.updateApp(appId, null, null, null, AppDesiredState.STARTED);
		waitRunning(appId);
	}

	@Override
	public void stageAndStartAppWaitRunning(String appId) {
		operations.updateApp(appId, null, null, null, AppDesiredState.STARTED);
		waitStaged(appId);
		waitRunning(appId);
	}

	private void waitStaged(String appId) {
		while (!isAppStaged(appId)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("App [{}] staged.", appId);
	}

	private void waitRunning(String appId) {
		while (!isAppRunning(appId)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("App [{}] running.", appId);
	}

	private boolean isAppStaged(String appId) {
		return operations.getAppSummary(appId).getPackageState().equals("STAGED");
	}

	private boolean isAppRunning(String appId) {
		Integer instance = operations.getAppSummary(appId).getRunningInstances();
		return instance != null && instance > 0;
	}

	@Override
	public void stopApp(String appId) {
		operations.updateApp(appId, null, null, null, AppDesiredState.STOPPED);
	}

	@Override
	public void deleteApp(String appId) {
		operations.deleteApp(appId);
	}

	@Override
	public void updateAppName(String appId, String name) {
		operations.updateApp(appId, name, null, null, null);
		logger.info("app [{}] name updated to [{}].", appId, name);
	}

	@Override
	public void updateAppEnv(String appId, Map<String, String> env) {
		operations.updateApp(appId, null, env, null, null);
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
		operations.updateApp(appId, null, null, instances, null);
	}

	@Override
	public void propagateEnvChange(String appId) {
		operations.restageApp(appId);
		waitStaged(appId);
		waitRunning(appId);
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
	public void createAndMapAppRoutes(String appId, List<Route> routes) {
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
