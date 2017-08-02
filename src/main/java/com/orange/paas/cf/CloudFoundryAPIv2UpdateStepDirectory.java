package com.orange.paas.cf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.OperationConfig;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.Route;
import com.orange.model.state.cf.CFAppDesiredState;
import com.orange.model.state.cf.CFAppState;
import com.orange.model.state.cf.CFOverviewApp;
import com.orange.model.workflow.SerialWorkflow;
import com.orange.model.workflow.Step;
import com.orange.model.workflow.Workflow;
import com.orange.paas.UpdateStepDirectory;
import com.orange.util.Wait;

public class CloudFoundryAPIv2UpdateStepDirectory implements UpdateStepDirectory {
    abstract class SiteStep extends Step {
	public SiteStep(String stepName) {
	    super(stepName + " at site " + operations.getSiteName());
	}
    }

    class SiteSerialWorkflow extends SerialWorkflow {
	public SiteSerialWorkflow(String stepName) {
	    super(stepName + " at site " + operations.getSiteName());
	}
    }

    private CloudFoundryOperations operations;
    private OperationConfig opConfig;

    public CloudFoundryAPIv2UpdateStepDirectory(CloudFoundryOperations operations) {
	this.operations = operations;
	this.opConfig = operations.getOpConfig();
    }

    @Override
    public Step addApp(OverviewApp app) {
	CFOverviewApp desiredApp = new CFOverviewApp(app);
	return new Step(String.format("addApp %s", desiredApp)) {
	    @Override
	    public void exec() {
		String appId = operations.createApp(desiredApp.getName(), desiredApp.getNbProcesses(),
			desiredApp.getEnv());
		CFOverviewApp currentApp = new CFOverviewApp(appId, desiredApp.getName(), null, CFAppState.CREATED,
			desiredApp.getNbProcesses(), desiredApp.getEnv(), Collections.emptySet(), new HashSet<>());
		updateAppRoutes(appId, currentApp.getRoutes(), desiredApp.getRoutes()).exec();
		updateAppServices(appId, currentApp.getServices(), desiredApp.getServices()).exec();
		if (currentApp.getState() != desiredApp.getState()) {
		    updateAppState(currentApp, desiredApp).exec();
		}
	    }
	};
    }

    @Override
    public Step removeApp(OverviewApp app) {
	return new SiteStep(String.format("removeApp [%s]", app.getGuid())) {
	    @Override
	    public void exec() {
		updateAppServices(app.getGuid(), app.getServices(), new HashSet<>()).exec();
		operations.deleteApp(app.getGuid());
	    }
	};
    }

    @Override
    public Step updateApp(OverviewApp currentApp, OverviewApp desiredApp) {
	CFOverviewApp currentCFApp = new CFOverviewApp(currentApp);
	CFOverviewApp desiredCFApp = new CFOverviewApp(desiredApp);

	Workflow serial = new SiteSerialWorkflow(
		String.format("serial update app properties from %s to %s", currentCFApp, desiredCFApp));
	String appId = currentCFApp.getGuid();
	if (!currentCFApp.getRoutes().equals(desiredCFApp.getRoutes())) {
	    serial.addStep(updateAppRoutes(appId, currentCFApp.getRoutes(), desiredCFApp.getRoutes()));
	    currentCFApp.setRoutes(desiredCFApp.getRoutes());
	}
	if (!currentCFApp.getName().equals(desiredCFApp.getName())) {
	    serial.addStep(updateAppName(appId, desiredCFApp.getName()));
	}

	if (desiredCFApp.getState() != CFAppState.CREATED && desiredCFApp.getPath() != null
		&& !desiredCFApp.getPath().equals(currentCFApp.getPath())) {
	    serial.addStep(updateAppPath(appId, desiredCFApp.getPath(), currentCFApp.getEnv()));
	    currentCFApp.setPath(desiredCFApp.getPath());
	    currentCFApp.setState(CFAppState.UPLOADED);
	}
	if (!currentCFApp.getServices().equals(desiredApp.getServices())) {
	    serial.addStep(updateAppServices(appId, currentCFApp.getServices(), desiredCFApp.getServices()));
	    currentCFApp.setServices(desiredCFApp.getServices());
	    restageIfNeeded(currentCFApp, serial);
	}
	if (!currentCFApp.getEnv().equals(desiredCFApp.getEnv())) {
	    serial.addStep(updateAppEnv(appId, desiredCFApp.getEnv()));
	    currentCFApp.setEnv(desiredCFApp.getEnv());
	    restageIfNeeded(currentCFApp, serial);
	}
	if (currentCFApp.getNbProcesses() != desiredCFApp.getNbProcesses()) {
	    serial.addStep(updateAppNbProcesses(appId, desiredCFApp.getNbProcesses()));
	    currentCFApp.setNbProcesses(desiredCFApp.getNbProcesses());
	}
	if (currentCFApp.getState() != desiredCFApp.getState()) {
	    serial.addStep(updateAppState(currentCFApp, desiredCFApp));
	    currentCFApp.setState(desiredCFApp.getState());
	}
	return serial;
    }

    /**
     * add step restage currentCFApp into the workflow if needed
     * 
     * @param currentCFApp
     *            The current microservice overview, its state might be updated
     * @param workflow
     *            The workflow within which to add restage step if needed
     */
    private void restageIfNeeded(CFOverviewApp currentCFApp, Workflow workflow) {
	Set<CFAppState> upstagedStates = new HashSet<>(Arrays.asList(CFAppState.CREATED, CFAppState.UPLOADED));
	if (!upstagedStates.contains(currentCFApp.getState())) {
	    workflow.addStep(restageApp(currentCFApp.getGuid()));
	    currentCFApp.setState(CFAppState.staging);
	}
    }

    private Step updateAppPath(String appId, String desiredPath, Map<String, String> currentEnv) {
	return new SiteStep(String.format("upload app [%s] with path [%s]", appId, desiredPath)) {
	    @Override
	    public void exec() {
		// app should be STOPPED before upload, so that it could be
		// staged later by operation of starting
		operations.stopApp(appId);
		operations.uploadApp(appId, desiredPath);
		Map<String, String> envWithUpdatedPath = new HashMap<>(currentEnv);
		envWithUpdatedPath.put(CloudFoundryAPIv2.pathKeyInEnv, desiredPath);
		operations.updateAppEnv(appId, envWithUpdatedPath);
	    }
	};
    }

    private Step updateAppEnv(String appId, Map<String, String> env) {
	return new SiteStep(String.format("update app [%s] env to [%s]", appId, env)) {
	    @Override
	    public void exec() {
		// should not change path value in env during update other env
		Map<String, String> envWithPath = new HashMap<>(env);
		String path = operations.getAppEnv(appId, CloudFoundryAPIv2.pathKeyInEnv);
		envWithPath.put(CloudFoundryAPIv2.pathKeyInEnv, path);
		operations.updateAppEnv(appId, envWithPath);
	    }
	};
    }

    private Step updateAppNbProcesses(String appId, int nbProcesses) {
	return new SiteStep(String.format("update app [%s] nbProcesses to [%d]", appId, nbProcesses)) {
	    @Override
	    public void exec() {
		operations.scaleApp(appId, nbProcesses);
	    }
	};
    }

    private Step updateAppName(String appId, String name) {
	return new SiteStep(String.format("update app [%s] name to [%s]", appId, name)) {
	    @Override
	    public void exec() {
		operations.updateAppName(appId, name);
	    }
	};
    }

    private Step updateAppRoutes(String appId, Set<Route> currentRoutes, Set<Route> desiredRoutes) {
	return new SiteStep(
		String.format("update app [%s] routes from [%s] to [%s]", appId, currentRoutes, desiredRoutes)) {
	    @Override
	    public void exec() {
		Set<Route> addedRoutes = desiredRoutes.stream().filter(route -> !currentRoutes.contains(route))
			.collect(Collectors.toSet());
		Set<Route> removedRoutes = currentRoutes.stream().filter(route -> !desiredRoutes.contains(route))
			.collect(Collectors.toSet());
		addedRoutes.stream().forEach(route -> operations.createAndMapAppRoute(appId, route));
		removedRoutes.stream().forEach(route -> operations.unmapAppRoute(appId, route));
	    }
	};
    }

    private Step updateAppServices(String appId, Set<String> currentServices, Set<String> desiredServices) {
	return new SiteStep(String.format("update app [%s] bound services from [%s] to [%s]", appId, currentServices,
		desiredServices)) {
	    @Override
	    public void exec() {
		Set<String> bindServices = desiredServices.stream()
			.filter(service -> !currentServices.contains(service)).collect(Collectors.toSet());
		Set<String> unbindServiecs = currentServices.stream()
			.filter(service -> !desiredServices.contains(service)).collect(Collectors.toSet());
		bindServices.stream().forEach(service -> operations.bindAppServices(appId, service));
		unbindServiecs.stream().forEach(service -> operations.unbindAppServices(appId, service));
	    }
	};
    }

    private Step updateAppState(CFOverviewApp currentApp, CFOverviewApp desiredApp) {
	String appId = currentApp.getGuid();
	Workflow serial = new SiteSerialWorkflow(String.format("update app [%s] state from [%s] to [%s]", appId,
		currentApp.getState(), desiredApp.getState()));
	switch (desiredApp.getState()) {
	case RUNNING:
	    switch (currentApp.getState()) {
	    case CREATED:
		serial.addStep(updateAppPath(appId, desiredApp.getPath(), currentApp.getEnv()));
	    case UPLOADED:
	    case STAGED:
		serial.addStep(startApp(appId));
	    case staging:
		serial.addStep(waitStaged(appId));
	    case starting:
		serial.addStep(waitRunning(appId));
		break;
	    case FAILED:
		serial.addStep(restageApp(appId));
		serial.addStep(waitStaged(appId));
		serial.addStep(waitRunning(appId));
		break;
	    default:
		throw new IllegalStateException(String.format("Unsupported app [%s] to update state from [%s] to [%s]",
			appId, currentApp.getState(), desiredApp.getState()));
	    }
	    break;
	case STAGED:
	    switch (currentApp.getState()) {
	    case CREATED:
		serial.addStep(updateAppPath(appId, desiredApp.getPath(), currentApp.getEnv()));
	    case UPLOADED:
		serial.addStep(startApp(appId));
	    case staging:
		serial.addStep(waitStaged(appId));
	    case starting:
	    case RUNNING:
		serial.addStep(stopApp(appId));
		break;
	    case FAILED:
		serial.addStep(restageApp(appId));
		serial.addStep(stopApp(appId));
		serial.addStep(waitStaged(appId));
		break;
	    default:
		throw new IllegalStateException(String.format("Unsupported app [%s] to update state from [%s] to [%s]",
			appId, currentApp.getState(), desiredApp.getState()));
	    }
	    break;
	case UPLOADED:
	    switch (currentApp.getState()) {
	    case CREATED:
		serial.addStep(updateAppPath(appId, desiredApp.getPath(), currentApp.getEnv()));
		break;
	    case FAILED:
		serial.addStep(restageApp(appId));
		serial.addStep(stopApp(appId));
		break;
	    default:
		throw new IllegalStateException(String.format("Unsupported app [%s] to update state from [%s] to [%s]",
			appId, currentApp.getState(), desiredApp.getState()));
	    }
	    break;
	default:
	    throw new IllegalStateException(String.format("Unsupported desired state [%s]", desiredApp.getState()));
	}
	return serial;
    }

    private Step startApp(String appId) {
	return new SiteStep(String.format("start app [%s]", appId)) {
	    @Override
	    public void exec() {
		operations.startApp(appId);
	    }
	};
    }

    private Step stopApp(String appId) {
	return new SiteStep(String.format("stop app [%s]", appId)) {
	    @Override
	    public void exec() {
		operations.stopApp(appId);
	    }
	};
    }

    private Step restageApp(String appId) {
	return new SiteStep(String.format("restage app [%s]", appId)) {
	    @Override
	    public void exec() {
		operations.restageApp(appId);
	    }
	};
    }

    private Step waitStaged(String appId) {
	return new SiteStep(String.format("wait until app [%s] staged", appId)) {
	    @Override
	    public void exec() {
		new Wait(opConfig.getPrepareTimeout()).waitUntil(id -> operations.appStaged(id),
			String.format("wait until app [%s] staged", appId), appId);
	    }
	};

    }

    private Step waitRunning(String appId) {
	return new SiteStep(String.format("wait until app [%s] running", appId)) {
	    @Override
	    public void exec() {
		new Wait(opConfig.getStartTimeout()).waitUntil(id -> operations.appRunning(id),
			String.format("wait until app [%s] running", appId), appId);
	    }
	};
    }
}
