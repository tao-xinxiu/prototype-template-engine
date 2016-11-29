package com.orange.workflow;

import java.util.List;

import com.orange.model.OverviewApp;
import com.orange.model.OverviewDroplet;
import com.orange.model.Route;
import com.orange.paas.PaaSAPI;

public class StepCalculator {
	public static Step addApp(PaaSAPI api, OverviewApp app) {
		return new Step(String.format("addApp %s at %s", app.getName(), api.getSiteName())) {
			@Override
			public void exec() {
				String appId = api.createAppIfNotExist(app);
				app.setGuid(appId);
				for (OverviewDroplet droplet : app.getDroplets()) {
					api.updateApp(app, droplet.getEnv());
					String dropletId = api.prepareDroplet(appId, droplet);
					switch (droplet.getState()) {
					case STAGED:
						break;
					case RUNNING:
						api.assignDroplet(appId, dropletId);
						api.startAppAndWaitUntilRunning(appId);
						api.mapAppRoutes(appId, app.listRoutes());
						break;
					default:
						throw new IllegalStateException("Abnormal desired droplet state");
					}
				}
			}
		};
	}

	public static Step removeApp(PaaSAPI api, OverviewApp app) {
		return new Step(String.format("removeApp %s at %s", app.getName(), api.getSiteName())) {
			@Override
			public void exec() {
				api.deleteApp(app.getGuid());
			}
		};
	}

	public static Step updateAppName(PaaSAPI api, OverviewApp desiredApp) {
		return new Step(String.format("updateApp [%s] name to %s at %s", desiredApp.getGuid(), desiredApp.getName(),
				api.getSiteName())) {
			@Override
			public void exec() {
				api.updateApp(desiredApp);
			}
		};
	}

	public static Step addAppRoutes(PaaSAPI api, String appId, List<Route> addedRoutes) {
		return new Step(String.format("map routes [%s] to %s at %s", addedRoutes, appId, api.getSiteName())) {
			@Override
			public void exec() {
				api.mapAppRoutes(appId, addedRoutes);
			}
		};
	}

	public static Step removeAppRoutes(PaaSAPI api, String appId, List<Route> removedRoutes) {
		return new Step(String.format("unmap routes [%s] from %s at %s", removedRoutes, appId, api.getSiteName())) {
			@Override
			public void exec() {
				api.unmapAppRoutes(appId, removedRoutes);
			}
		};
	}
}
