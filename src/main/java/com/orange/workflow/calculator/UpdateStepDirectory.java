package com.orange.workflow.calculator;

import java.util.List;

import com.orange.model.DropletState;
import com.orange.model.OverviewApp;
import com.orange.model.Route;
import com.orange.paas.PaaSAPI;
import com.orange.workflow.Step;

public class UpdateStepDirectory {
	private PaaSAPI api;

	public UpdateStepDirectory(PaaSAPI api) {
		this.api = api;
	}

	public Step addApp(OverviewApp app) {
		return new Step(String.format("addApp [%s] at site [%s]", app.getName(), api.getSiteName())) {
			@Override
			public void exec() {
				String appId = api.createAppIfNotExist(app);
				app.setGuid(appId);
				api.mapAppRoutes(appId, app.listRoutes());
				api.scaleApp(appId, app.getInstances());
				api.updateAppEnv(appId, app.getEnv());
				api.prepareApp(app);
				switch (app.getState()) {
				case STAGED:
					break;
				case RUNNING:
					api.startAppAndWaitUntilRunning(appId);
					break;
				default:
					throw new IllegalStateException("Abnormal desired droplet state");
				}
			}
		};
	}

	public Step removeApp(OverviewApp app) {
		return new Step(String.format("removeApp [%s] at site [%s]", app.getName(), api.getSiteName())) {
			@Override
			public void exec() {
				api.deleteApp(app.getGuid());
			}
		};
	}

	public Step updateAppName(OverviewApp desiredApp) {
		return new Step(String.format("updateApp [%s] name to [%s] at site [%s]", desiredApp.getGuid(),
				desiredApp.getName(), api.getSiteName())) {
			@Override
			public void exec() {
				api.updateAppName(desiredApp.getGuid(), desiredApp.getName());
			}
		};
	}
	
	public Step updateAppEnv(OverviewApp desiredApp) {
		return new Step(String.format("updateApp [%s] env to [%s] at site [%s]", desiredApp.getGuid(),
				desiredApp.getEnv(), api.getSiteName())) {
			@Override
			public void exec() {
				api.updateAppEnv(desiredApp.getGuid(), desiredApp.getEnv());
			}
		};
	}

	public Step addAppRoutes(String appId, List<Route> addedRoutes) {
		return new Step(
				String.format("map routes %s to app [%s] at site [%s]", addedRoutes, appId, api.getSiteName())) {
			@Override
			public void exec() {
				api.mapAppRoutes(appId, addedRoutes);
			}
		};
	}

	public Step removeAppRoutes(String appId, List<Route> removedRoutes) {
		return new Step(
				String.format("unmap routes %s from app [%s] at site [%s]", removedRoutes, appId, api.getSiteName())) {
			@Override
			public void exec() {
				api.unmapAppRoutes(appId, removedRoutes);
			}
		};
	}

	public Step updateAppState(OverviewApp currentApp, OverviewApp desiredApp) {
		return new Step(String.format("change app [%s] state from [%s] to [%s] at site [%s]", currentApp.getGuid(),
				currentApp.getState(), desiredApp.getState(), api.getSiteName())) {
			@Override
			public void exec() {
				assert currentApp.getGuid().equals(desiredApp.getGuid());
				assert currentApp.getState() != DropletState.FAILED && desiredApp.getState() != DropletState.FAILED;
				assert currentApp.getState() != desiredApp.getState();
				switch (currentApp.getState()) {
				case CREATED:
					api.prepareApp(currentApp);
					if (desiredApp.getState() == DropletState.RUNNING) {
						api.startAppAndWaitUntilRunning(currentApp.getGuid());
					}
					break;
				case STAGED:
					if (desiredApp.getState() == DropletState.RUNNING) {
						api.startAppAndWaitUntilRunning(currentApp.getGuid());
						break;
					}
				case RUNNING:
					if (desiredApp.getState() == DropletState.STAGED) {
						api.stopApp(currentApp.getGuid());
						break;
					}
				default:
					throw new IllegalStateException(String.format("Unsupported app state change from [%s] to [%s]",
							currentApp.getState(), desiredApp.getState()));
				}
			}
		};
	}

	public Step scaleApp(String appId, int instances) {
		return new Step(
				String.format("scaleApp [%s] to [%s] instances at site [%s]", appId, instances, api.getSiteName())) {
			@Override
			public void exec() {
				api.scaleApp(appId, instances);
			}
		};
	}
}
