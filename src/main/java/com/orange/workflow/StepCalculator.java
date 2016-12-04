package com.orange.workflow;

import java.util.List;

import com.orange.model.DropletState;
import com.orange.model.OverviewApp;
import com.orange.model.OverviewDroplet;
import com.orange.model.Route;
import com.orange.paas.PaaSAPI;

public class StepCalculator {
	public static Step addApp(PaaSAPI api, OverviewApp app) {
		return new Step(String.format("addApp [%s] at site [%s]", app.getName(), api.getSiteName())) {
			@Override
			public void exec() {
				String appId = api.createAppIfNotExist(app);
				app.setGuid(appId);
				api.mapAppRoutes(appId, app.listRoutes());
				for (OverviewDroplet droplet : app.getDroplets()) {
					api.updateApp(app, droplet.getEnv());
					String dropletId = api.prepareDroplet(appId, droplet);
					droplet.setGuid(dropletId);
					switch (droplet.getState()) {
					case STAGED:
						break;
					case RUNNING:
						StepCalculator.changeCurrentDroplet(api, appId, droplet);
						break;
					default:
						throw new IllegalStateException("Abnormal desired droplet state");
					}
				}
			}
		};
	}

	public static Step removeApp(PaaSAPI api, OverviewApp app) {
		return new Step(String.format("removeApp [%s] at site [%s]", app.getName(), api.getSiteName())) {
			@Override
			public void exec() {
				api.deleteApp(app.getGuid());
			}
		};
	}

	public static Step updateAppName(PaaSAPI api, OverviewApp desiredApp) {
		return new Step(String.format("updateApp [%s] name to [%s] at site [%s]", desiredApp.getGuid(),
				desiredApp.getName(), api.getSiteName())) {
			@Override
			public void exec() {
				api.updateApp(desiredApp);
			}
		};
	}

	public static Step addAppRoutes(PaaSAPI api, String appId, List<Route> addedRoutes) {
		return new Step(
				String.format("map routes %s to app [%s] at site [%s]", addedRoutes, appId, api.getSiteName())) {
			@Override
			public void exec() {
				api.mapAppRoutes(appId, addedRoutes);
			}
		};
	}

	public static Step removeAppRoutes(PaaSAPI api, String appId, List<Route> removedRoutes) {
		return new Step(
				String.format("unmap routes %s from app [%s] at site [%s]", removedRoutes, appId, api.getSiteName())) {
			@Override
			public void exec() {
				api.unmapAppRoutes(appId, removedRoutes);
			}
		};
	}

	public static Step addDroplets(PaaSAPI api, OverviewApp app, List<OverviewDroplet> addedDroplets) {
		return new Step(String.format("addDroplets %s of app %s at site [%s]", addedDroplets, app, api.getSiteName())) {
			@Override
			public void exec() {
				for (OverviewDroplet droplet : addedDroplets) {
					api.updateApp(app, droplet.getEnv());
					String appId = app.getGuid();
					String dropletId = api.prepareDroplet(appId, droplet);
					droplet.setGuid(dropletId);
					switch (droplet.getState()) {
					case STAGED:
						break;
					case RUNNING:
						StepCalculator.changeCurrentDroplet(api, appId, droplet);
						break;
					default:
						throw new IllegalStateException("Abnormal desired droplet state");
					}
				}
			}
		};
	}

	public static Step removeDroplets(PaaSAPI api, OverviewApp app, List<OverviewDroplet> removedDroplets) {
		return new Step(
				String.format("removeDroplet %s of app %s at site [%s]", removedDroplets, app, api.getSiteName())) {
			@Override
			public void exec() {
				for (OverviewDroplet droplet : removedDroplets) {
					if (droplet.getState() == DropletState.RUNNING) {
						api.stopApp(app.getGuid());
					}
					api.deleteDroplet(droplet.getGuid());
				}
			}
		};
	}

	public static Step updateCurrentDroplet(PaaSAPI api, String appId, OverviewDroplet newCurrentDroplet) {
		return new Step(String.format("changeCurrentDroplet of app [%s] to [%s] at site [%s]", appId, newCurrentDroplet,
				api.getSiteName())) {
			@Override
			public void exec() {
				StepCalculator.changeCurrentDroplet(api, appId, newCurrentDroplet);
			}
		};
	}

	public static Step stopApp(PaaSAPI api, String appId) {
		return new Step(String.format("stopApp [%s] at site [%s]", appId, api.getSiteName())) {
			@Override
			public void exec() {
				api.stopApp(appId);
			}
		};
	}

	public static Step scaleApp(PaaSAPI api, String appId, int instances) {
		return new Step(String.format("scaleApp [%s] to [%s] instances at site [%s]", appId,
				instances, api.getSiteName())) {
			@Override
			public void exec() {
				api.scaleApp(appId, instances);
			}
		};
	}
	
	private static void changeCurrentDroplet(PaaSAPI api, String appId, OverviewDroplet currentDroplet){
		api.assignDroplet(appId, currentDroplet.getGuid());
		api.scaleApp(appId, currentDroplet.getInstances());
		api.startAppAndWaitUntilRunning(appId);
	}
}
