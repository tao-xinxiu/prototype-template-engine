package com.orange.workflow.calculator;

import java.util.List;

import com.orange.model.AppState;
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
				String appId = api.createAppWaitUploaded(app);
				api.createAndMapAppRoutes(appId, app.listRoutes());
				switch (app.getState()) {
				case CREATED:
					break;
				case STAGED:
					api.stageAppWaitStaged(appId);
					break;
				case RUNNING:
					api.stageAndStartAppWaitRunning(appId);
					break;
				default:
					throw new IllegalStateException(
							String.format("Unsupported desired app state [%s].", app.getState()));
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
				api.propagateEnvChange(desiredApp.getGuid());
			}
		};
	}

	public Step addAppRoutes(String appId, List<Route> addedRoutes) {
		return new Step(
				String.format("map routes %s to app [%s] at site [%s]", addedRoutes, appId, api.getSiteName())) {
			@Override
			public void exec() {
				api.createAndMapAppRoutes(appId, addedRoutes);
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
				assert currentApp.getState() != AppState.FAILED && desiredApp.getState() != AppState.FAILED;
				assert currentApp.getState() != desiredApp.getState();
				switch (currentApp.getState()) {
				case CREATED:
					switch (desiredApp.getState()) {
					case STAGED:
						api.stageAppWaitStaged(currentApp.getGuid());
						break;
					case RUNNING:
						api.stageAndStartAppWaitRunning(currentApp.getGuid());
					default:
						throw new IllegalStateException(
								String.format("Unsupported desired app state [%s].", desiredApp.getState()));
					}
					break;
				case STAGED:
					if (desiredApp.getState() == AppState.RUNNING) {
						api.startAppWaitRunning(currentApp.getGuid());
						break;
					}
				case RUNNING:
					if (desiredApp.getState() == AppState.STAGED) {
						api.stopApp(currentApp.getGuid());
						break;
					}
				default:
					throw new IllegalStateException(String.format("Unsupported app state change from [%s] to [%s].",
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
