package com.orange.paas.cf;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.AppDesiredState;
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.Route;
import com.orange.model.workflow.SerialWorkflow;
import com.orange.model.workflow.Step;
import com.orange.model.workflow.Workflow;
import com.orange.paas.UpdateStepDirectory;
import com.orange.update.AppComparator;

public class CloudFoundryAPIv2UpdateStepDirectory implements UpdateStepDirectory {
    private final Logger logger = LoggerFactory.getLogger(CloudFoundryAPIv2UpdateStepDirectory.class);
    private CloudFoundryOperations operations;

    public CloudFoundryAPIv2UpdateStepDirectory(CloudFoundryOperations operations) {
	this.operations = operations;
    }

    @Override
    public Step addApp(OverviewApp app) {
	return new Step(String.format("addApp [%s] at site [%s]", app.getName(), operations.getSiteName())) {
	    @Override
	    public void exec() {
		String appId = operations.createApp(app.getName(), app.getNbProcesses(), app.getEnv());
		operations.uploadApp(appId, app.getPath());
		app.listRoutes().parallelStream().forEach(route -> createAndMapAppRoute(appId, route));
		switch (app.getState()) {
		case CREATED:
		    return;
		case STAGED:
		    operations.updateApp(appId, null, null, null, AppDesiredState.STARTED);
		    operations.updateApp(appId, null, null, null, AppDesiredState.STOPPED);
		    waitStaged(appId);
		    break;
		case RUNNING:
		    operations.updateApp(appId, null, null, null, AppDesiredState.STARTED);
		    waitStaged(appId);
		    waitRunning(appId);
		    break;
		default:
		    throw new IllegalStateException(
			    String.format("Unsupported desired app state [%s].", app.getState()));
		}
	    }
	};
    }

    @Override
    public Step removeApp(OverviewApp app) {
	return new Step(String.format("removeApp [%s] at site [%s]", app.getName(), operations.getSiteName())) {
	    @Override
	    public void exec() {
		operations.deleteApp(app.getGuid());
	    }
	};
    }

    @Override
    public Step updateApp(AppComparator appComparator) {
	Workflow updateApp = new SerialWorkflow(String.format("serial update app from %s to %s at site %s",
		appComparator.getCurrentApp(), appComparator.getDesiredApp(), operations.getSiteName()));
	String appId = appComparator.getCurrentApp().getGuid();
	if (appComparator.isPathUpdated()) {
	    updateApp.addStep(updateAppPath(appId, appComparator.getDesiredApp().getPath()));
	}
	if (appComparator.isNbProcessesUpdated() || appComparator.isEnvUpdated() || appComparator.isStateUpdated()) {
	    updateApp.addStep(updateAppProperty(appComparator));
	}
	if (appComparator.isRoutesAdded()) {
	    updateApp.addStep(addAppRoutes(appId, appComparator.getAddedRoutes()));
	}
	if (appComparator.isRoutesRemoved()) {
	    updateApp.addStep(removeAppRoutes(appId, appComparator.getRemovedRoutes()));
	}
	return updateApp;
    }

    private Step updateAppPath(String id, String path) {
	return new Step(
		String.format("upload app [%s] with path [%s] at site [%s]", id, path, operations.getSiteName())) {
	    @Override
	    public void exec() {
		// change app desired state to STOPPED before upload package.
		operations.updateApp(id, null, null, null, AppDesiredState.STOPPED);
		operations.uploadApp(id, path);
	    }
	};
    }

    private Step updateAppProperty(AppComparator appComparator) {
	if (appComparator.isPathUpdated()) {
	    updateFromCREATEDApp(appComparator);
	}
	switch (appComparator.getCurrentApp().getState()) {
	case CREATED:
	    return updateFromCREATEDApp(appComparator);
	case STAGED:
	    return updateFromSTAGEDApp(appComparator);
	case RUNNING:
	    return updateFromRUNNINGApp(appComparator);
	default:
	    throw new IllegalStateException("Not yet support to transform app state from FAILED state");
	}
    }

    private Step updateFromCREATEDApp(AppComparator appComparator) {
	return new Step(String.format("updateApp [%s] to [%s] at site [%s]", appComparator.getCurrentApp(),
		appComparator.getDesiredApp(), operations.getSiteName())) {
	    @Override
	    public void exec() {
		OverviewApp desiredApp = appComparator.getDesiredApp();
		AppDesiredState updateState = (desiredApp.getState() == AppState.CREATED) ? null
			: AppDesiredState.STARTED;
		operations.updateApp(desiredApp.getGuid(), desiredApp.getName(), desiredApp.getEnv(),
			desiredApp.getNbProcesses(), updateState);
		switch (desiredApp.getState()) {
		case STAGED:
		    operations.updateApp(desiredApp.getGuid(), null, null, null, AppDesiredState.STOPPED);
		    waitStaged(desiredApp.getGuid());
		case RUNNING:
		    waitStaged(desiredApp.getGuid());
		    waitRunning(desiredApp.getGuid());
		default:
		    break;
		}
	    }
	};
    }

    private Step updateFromSTAGEDApp(AppComparator appComparator) {
	return new Step(String.format("updateApp [%s] to [%s] at site [%s]", appComparator.getCurrentApp(),
		appComparator.getDesiredApp(), operations.getSiteName())) {
	    @Override
	    public void exec() {
		OverviewApp desiredApp = appComparator.getDesiredApp();
		AppDesiredState updateState = desiredApp.getState() == AppState.RUNNING ? AppDesiredState.STARTED
			: AppDesiredState.STOPPED;
		operations.updateApp(desiredApp.getGuid(), desiredApp.getName(), desiredApp.getEnv(),
			desiredApp.getNbProcesses(), updateState);
		if (appComparator.isEnvUpdated()) {
		    operations.restageApp(desiredApp.getGuid());
		    if (desiredApp.getState() == AppState.STAGED) {
			operations.updateApp(desiredApp.getGuid(), null, null, null, AppDesiredState.STOPPED);
		    }
		    waitStaged(desiredApp.getGuid());
		}
		if (desiredApp.getState() == AppState.RUNNING) {
		    waitRunning(desiredApp.getGuid());
		}
	    }
	};
    }

    private Step updateFromRUNNINGApp(AppComparator appComparator) {
	return new Step(String.format("updateApp [%s] to [%s] at site [%s]", appComparator.getCurrentApp(),
		appComparator.getDesiredApp(), operations.getSiteName())) {
	    @Override
	    public void exec() {
		OverviewApp desiredApp = appComparator.getDesiredApp();
		AppDesiredState updateState = desiredApp.getState() == AppState.RUNNING ? AppDesiredState.STARTED
			: AppDesiredState.STOPPED;
		operations.updateApp(desiredApp.getGuid(), desiredApp.getName(), desiredApp.getEnv(),
			desiredApp.getNbProcesses(), updateState);
		if (appComparator.isEnvUpdated()) {
		    operations.restageApp(desiredApp.getGuid());
		    if (desiredApp.getState() == AppState.STAGED) {
			operations.updateApp(desiredApp.getGuid(), null, null, null, AppDesiredState.STOPPED);
		    }
		    waitStaged(desiredApp.getGuid());
		    if (desiredApp.getState() == AppState.RUNNING) {
			waitRunning(desiredApp.getGuid());
		    }
		}
	    }
	};
    }

    private Step addAppRoutes(String appId, Set<Route> addedRoutes) {
	return new Step(
		String.format("map routes %s to app [%s] at site [%s]", addedRoutes, appId, operations.getSiteName())) {
	    @Override
	    public void exec() {
		addedRoutes.parallelStream().forEach(route -> createAndMapAppRoute(appId, route));
	    }
	};
    }

    private void createAndMapAppRoute(String appId, Route route) {
	String domainId = operations.getDomainId(route.getDomain());
	String routeId = operations.getRouteId(route.getHostname(), domainId);
	if (routeId == null) {
	    routeId = operations.createRoute(route.getHostname(), domainId);
	}
	operations.createRouteMapping(appId, routeId);
	logger.info("route [{}] mapped to the app [{}]", routeId, appId);
    }

    private Step removeAppRoutes(String appId, Set<Route> removedRoutes) {
	return new Step(String.format("unmap routes %s from app [%s] at site [%s]", removedRoutes, appId,
		operations.getSiteName())) {
	    @Override
	    public void exec() {
		removedRoutes.parallelStream().forEach(route -> unmapAppRoute(appId, route));
	    }
	};
    }

    private void unmapAppRoute(String appId, Route route) {
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

    private void waitStaged(String appId) {
	while (!isAppStaged(appId)) {
	    try {
		Thread.sleep(1000);
	    } catch (InterruptedException e) {
		logger.error("InterruptedException", e);
	    }
	}
	logger.info("App [{}] staged.", appId);
    }

    private void waitRunning(String appId) {
	while (!isAppRunning(appId)) {
	    try {
		Thread.sleep(1000);
	    } catch (InterruptedException e) {
		logger.error("InterruptedException", e);
	    }
	}
	logger.info("App [{}] running.", appId);
    }

    private boolean isAppStaged(String appId) {
	return operations.getAppSummary(appId).getPackageState().equals("STAGED");
    }

    private boolean isAppRunning(String appId) {
	Integer runningInstances = operations.getAppSummary(appId).getRunningInstances();
	return runningInstances != null && runningInstances > 0;
    }
}
