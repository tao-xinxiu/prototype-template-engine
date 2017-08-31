package com.orange.paas.cf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v2.spaces.SpaceApplicationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.Main;
import com.orange.model.*;
import com.orange.model.state.MicroserviceState;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.model.state.ArchitectureSite;
import com.orange.model.state.Route;
import com.orange.model.state.cf.CFMicroserviceArchitecture;
import com.orange.model.state.cf.CFMicroserviceDesiredState;
import com.orange.model.state.cf.CFMicroserviceState;
import com.orange.model.workflow.SerialWorkflow;
import com.orange.model.workflow.Step;
import com.orange.model.workflow.Workflow;
import com.orange.paas.PaaSAPI;
import com.orange.util.Wait;

public class CloudFoundryAPIv2 extends PaaSAPI {
    public static final String pathKeyInEnv = "RUM_PATH";
    private final Logger logger;

    private CloudFoundryOperations operations;

    public CloudFoundryAPIv2(PaaSSite site, OperationConfig operationConfig) {
	super(site, operationConfig);
	this.logger = LoggerFactory.getLogger(String.format("%s(%s)", getClass(), site.getName()));
	this.operations = Main.getCloudFoundryOperations(site, operationConfig);
    }

    @Override
    public ArchitectureSite getSiteArchitecture() {
	logger.info("Start getting the current state ...");
	return new ArchitectureSite(operations.listSpaceApps().parallelStream()
		.map(info -> new ArchitectureMicroservice(info.getId(), parseName(info.getName()),
			parseVersion(info.getName()), parsePath(info), parseState(info), info.getInstances(),
			parseEnv(info), parseRoutes(info), parseServices(info), info.getMemory() + "M",
			info.getDiskQuota() + "M"))
		.collect(Collectors.toSet()));
    }

    public ArchitectureSite stabilizeSiteArchitecture() {
	logger.info("Start getting the current state and make it stable ...");
	return new ArchitectureSite(operations.listSpaceApps().parallelStream()
		.map(info -> new ArchitectureMicroservice(info.getId(), parseName(info.getName()),
			parseVersion(info.getName()), parsePath(info), stabilizeState(info), info.getInstances(),
			parseEnv(info), parseRoutes(info), parseServices(info), info.getMemory() + "M",
			info.getDiskQuota() + "M"))
		.collect(Collectors.toSet()));
    }

    @Override
    public Step add(ArchitectureMicroservice microservice) {
	CFMicroserviceArchitecture desiredMicroservice = new CFMicroserviceArchitecture(microservice);
	return new Step(String.format("add microservice %s", desiredMicroservice)) {
	    @Override
	    public void exec() {
		String msId = operations.createApp(desiredMicroservice.getName(), desiredMicroservice.getNbProcesses(),
			desiredMicroservice.getEnv());
		CFMicroserviceArchitecture currentMicroservice = new CFMicroserviceArchitecture(msId,
			desiredMicroservice.getName(), null, CFMicroserviceState.CREATED,
			desiredMicroservice.getNbProcesses(), desiredMicroservice.getEnv(), Collections.emptySet(),
			new HashSet<>());
		updateRoutes(msId, currentMicroservice.getRoutes(), desiredMicroservice.getRoutes()).exec();
		updateServices(msId, currentMicroservice.getServices(), desiredMicroservice.getServices()).exec();
		if (currentMicroservice.getState() != desiredMicroservice.getState()) {
		    updateState(currentMicroservice, desiredMicroservice).exec();
		}
	    }
	};
    }

    @Override
    public Step remove(ArchitectureMicroservice microservice) {
	return new SiteStep(String.format("remove microservice [%s]", microservice.getGuid())) {
	    @Override
	    public void exec() {
		updateServices(microservice.getGuid(), microservice.getServices(), new HashSet<>()).exec();
		operations.deleteApp(microservice.getGuid());
	    }
	};
    }

    @Override
    public Step update(ArchitectureMicroservice currentMicroservice, ArchitectureMicroservice desiredMicroservice) {
	CFMicroserviceArchitecture currentCFMicroservice = new CFMicroserviceArchitecture(currentMicroservice);
	CFMicroserviceArchitecture desiredCFMicroservice = new CFMicroserviceArchitecture(desiredMicroservice);

	Workflow serial = new SiteSerialWorkflow(String.format("serial update microservice from %s to %s",
		currentCFMicroservice, desiredCFMicroservice));
	String msId = currentCFMicroservice.getGuid();
	if (!currentCFMicroservice.getRoutes().equals(desiredCFMicroservice.getRoutes())) {
	    serial.addStep(updateRoutes(msId, currentCFMicroservice.getRoutes(), desiredCFMicroservice.getRoutes()));
	    currentCFMicroservice.setRoutes(desiredCFMicroservice.getRoutes());
	}
	if (!currentCFMicroservice.getName().equals(desiredCFMicroservice.getName())) {
	    serial.addStep(updateName(msId, desiredCFMicroservice.getName()));
	}
	if (desiredCFMicroservice.getState() != CFMicroserviceState.CREATED && desiredCFMicroservice.getPath() != null
		&& !desiredCFMicroservice.getPath().equals(currentCFMicroservice.getPath())) {
	    serial.addStep(updatePath(msId, desiredCFMicroservice.getPath(), currentCFMicroservice.getEnv()));
	    currentCFMicroservice.setPath(desiredCFMicroservice.getPath());
	    currentCFMicroservice.setState(CFMicroserviceState.UPLOADED);
	}
	if (!currentCFMicroservice.getServices().equals(desiredMicroservice.getServices())) {
	    serial.addStep(
		    updateServices(msId, currentCFMicroservice.getServices(), desiredCFMicroservice.getServices()));
	    currentCFMicroservice.setServices(desiredCFMicroservice.getServices());
	    restageIfNeeded(currentCFMicroservice, serial);
	}
	if (!currentCFMicroservice.getEnv().equals(desiredCFMicroservice.getEnv())) {
	    serial.addStep(updateEnv(msId, desiredCFMicroservice.getEnv()));
	    currentCFMicroservice.setEnv(desiredCFMicroservice.getEnv());
	    restageIfNeeded(currentCFMicroservice, serial);
	}
	if (currentCFMicroservice.getNbProcesses() != desiredCFMicroservice.getNbProcesses()) {
	    serial.addStep(updateNbProcesses(msId, desiredCFMicroservice.getNbProcesses()));
	    currentCFMicroservice.setNbProcesses(desiredCFMicroservice.getNbProcesses());
	}
	if (currentCFMicroservice.getState() != desiredCFMicroservice.getState()) {
	    serial.addStep(updateState(currentCFMicroservice, desiredCFMicroservice));
	    currentCFMicroservice.setState(desiredCFMicroservice.getState());
	}
	return serial;
    }

    /**
     * add step restage currentCFMicroservice into the workflow if needed
     * 
     * @param currentCFMicroservice
     *            The current microservice architecture, its state might be
     *            updated
     * @param workflow
     *            The workflow within which to add restage step if needed
     */
    private void restageIfNeeded(CFMicroserviceArchitecture currentCFMicroservice, Workflow workflow) {
	Set<CFMicroserviceState> upstagedStates = new HashSet<>(
		Arrays.asList(CFMicroserviceState.CREATED, CFMicroserviceState.UPLOADED));
	if (!upstagedStates.contains(currentCFMicroservice.getState())) {
	    workflow.addStep(restage(currentCFMicroservice.getGuid()));
	    currentCFMicroservice.setState(CFMicroserviceState.staging);
	}
    }

    private Step updatePath(String msId, String desiredPath, Map<String, String> currentEnv) {
	return new SiteStep(String.format("upload microservice [%s] with path [%s]", msId, desiredPath)) {
	    @Override
	    public void exec() {
		// microservice should be STOPPED before upload, so that it
		// could be staged later by operation of starting
		operations.stopApp(msId);
		operations.uploadApp(msId, desiredPath);
		Map<String, String> envWithUpdatedPath = new HashMap<>(currentEnv);
		envWithUpdatedPath.put(CloudFoundryAPIv2.pathKeyInEnv, desiredPath);
		operations.updateAppEnv(msId, envWithUpdatedPath);
	    }
	};
    }

    private Step updateEnv(String msId, Map<String, String> env) {
	return new SiteStep(String.format("update microservice [%s] env to [%s]", msId, env)) {
	    @Override
	    public void exec() {
		// should not change path value in env during update other env
		Map<String, String> envWithPath = new HashMap<>(env);
		String path = operations.getAppEnv(msId, CloudFoundryAPIv2.pathKeyInEnv);
		envWithPath.put(CloudFoundryAPIv2.pathKeyInEnv, path);
		operations.updateAppEnv(msId, envWithPath);
	    }
	};
    }

    private Step updateNbProcesses(String msId, int nbProcesses) {
	return new SiteStep(String.format("update microservice [%s] nbProcesses to [%d]", msId, nbProcesses)) {
	    @Override
	    public void exec() {
		operations.scaleApp(msId, nbProcesses);
	    }
	};
    }

    private Step updateName(String msId, String name) {
	return new SiteStep(String.format("update microservice [%s] name to [%s]", msId, name)) {
	    @Override
	    public void exec() {
		operations.updateAppName(msId, name);
	    }
	};
    }

    private Step updateRoutes(String msId, Set<Route> currentRoutes, Set<Route> desiredRoutes) {
	return new SiteStep(String.format("update microservice [%s] routes from [%s] to [%s]", msId, currentRoutes,
		desiredRoutes)) {
	    @Override
	    public void exec() {
		Set<Route> addedRoutes = desiredRoutes.stream().filter(route -> !currentRoutes.contains(route))
			.collect(Collectors.toSet());
		Set<Route> removedRoutes = currentRoutes.stream().filter(route -> !desiredRoutes.contains(route))
			.collect(Collectors.toSet());
		addedRoutes.stream().forEach(route -> operations.createAndMapAppRoute(msId, route));
		removedRoutes.stream().forEach(route -> operations.unmapAppRoute(msId, route));
	    }
	};
    }

    private Step updateServices(String msId, Set<String> currentServices, Set<String> desiredServices) {
	return new SiteStep(String.format("update microservice [%s] bound services from [%s] to [%s]", msId,
		currentServices, desiredServices)) {
	    @Override
	    public void exec() {
		Set<String> bindServices = desiredServices.stream()
			.filter(service -> !currentServices.contains(service)).collect(Collectors.toSet());
		Set<String> unbindServiecs = currentServices.stream()
			.filter(service -> !desiredServices.contains(service)).collect(Collectors.toSet());
		bindServices.stream().forEach(service -> operations.bindAppServices(msId, service));
		unbindServiecs.stream().forEach(service -> operations.unbindAppServices(msId, service));
	    }
	};
    }

    private Step updateState(CFMicroserviceArchitecture currentMicroservice,
	    CFMicroserviceArchitecture desiredMicroservice) {
	String msId = currentMicroservice.getGuid();
	Workflow serial = new SiteSerialWorkflow(String.format("update microservice [%s] state from [%s] to [%s]", msId,
		currentMicroservice.getState(), desiredMicroservice.getState()));
	switch (desiredMicroservice.getState()) {
	case RUNNING:
	    switch (currentMicroservice.getState()) {
	    case CREATED:
		serial.addStep(updatePath(msId, desiredMicroservice.getPath(), currentMicroservice.getEnv()));
	    case UPLOADED:
	    case STAGED:
		serial.addStep(start(msId));
	    case staging:
		serial.addStep(waitStaged(msId));
	    case starting:
		serial.addStep(waitRunning(msId));
		break;
	    case FAILED:
		serial.addStep(restage(msId));
		serial.addStep(waitStaged(msId));
		serial.addStep(waitRunning(msId));
		break;
	    default:
		throw new IllegalStateException(
			String.format("Unsupported microservice [%s] to update state from [%s] to [%s]", msId,
				currentMicroservice.getState(), desiredMicroservice.getState()));
	    }
	    break;
	case STAGED:
	    switch (currentMicroservice.getState()) {
	    case CREATED:
		serial.addStep(updatePath(msId, desiredMicroservice.getPath(), currentMicroservice.getEnv()));
	    case UPLOADED:
		serial.addStep(start(msId));
	    case staging:
		serial.addStep(waitStaged(msId));
	    case starting:
	    case RUNNING:
		serial.addStep(stop(msId));
		break;
	    case FAILED:
		serial.addStep(restage(msId));
		serial.addStep(stop(msId));
		serial.addStep(waitStaged(msId));
		break;
	    default:
		throw new IllegalStateException(
			String.format("Unsupported microservice [%s] to update state from [%s] to [%s]", msId,
				currentMicroservice.getState(), desiredMicroservice.getState()));
	    }
	    break;
	case UPLOADED:
	    switch (currentMicroservice.getState()) {
	    case CREATED:
		serial.addStep(updatePath(msId, desiredMicroservice.getPath(), currentMicroservice.getEnv()));
		break;
	    case FAILED:
		serial.addStep(restage(msId));
		serial.addStep(stop(msId));
		break;
	    default:
		throw new IllegalStateException(
			String.format("Unsupported microservice [%s] to update state from [%s] to [%s]", msId,
				currentMicroservice.getState(), desiredMicroservice.getState()));
	    }
	    break;
	default:
	    throw new IllegalStateException(
		    String.format("Unsupported desired state [%s]", desiredMicroservice.getState()));
	}
	return serial;
    }

    private Step start(String msId) {
	return new SiteStep(String.format("start microservice [%s]", msId)) {
	    @Override
	    public void exec() {
		operations.startApp(msId);
	    }
	};
    }

    private Step stop(String msId) {
	return new SiteStep(String.format("stop microservice [%s]", msId)) {
	    @Override
	    public void exec() {
		operations.stopApp(msId);
	    }
	};
    }

    private Step restage(String msId) {
	return new SiteStep(String.format("restage microservice [%s]", msId)) {
	    @Override
	    public void exec() {
		operations.restageApp(msId);
	    }
	};
    }

    private Step waitStaged(String msId) {
	return new SiteStep(String.format("wait until microservice [%s] staged", msId)) {
	    @Override
	    public void exec() {
		new Wait(operationConfig.getPrepareTimeout()).waitUntil(id -> operations.appStaged(id),
			String.format("wait until microservice [%s] staged", msId), msId);
	    }
	};

    }

    private Step waitRunning(String msId) {
	return new SiteStep(String.format("wait until microservice [%s] running", msId)) {
	    @Override
	    public void exec() {
		new Wait(operationConfig.getStartTimeout()).waitUntil(id -> operations.appRunning(id),
			String.format("wait until microservice [%s] running", msId), msId);
	    }
	};
    }

    /**
     * Get microservice name from Cloud Foundry microservice name
     * 
     * @param name
     *            CF microservice instance unique name, mapped to microservice
     *            model as "name_version"
     * @return
     */
    private String parseName(String name) {
	int delimiterPosition = name.lastIndexOf("_");
	if (delimiterPosition == -1) {
	    return name;
	} else {
	    return name.substring(0, delimiterPosition);
	}
    }

    /**
     * Get microservice version from Cloud Foundry microservice name
     * 
     * @param name
     *            CF microservice instance unique name, mapped to microservice
     *            model as "name_version"
     * @return
     */
    private String parseVersion(String name) {
	int delimiterPosition = name.lastIndexOf("_");
	if (delimiterPosition == -1) {
	    return "";
	} else {
	    return name.substring(delimiterPosition + 1);
	}
    }

    private MicroserviceState parseState(SpaceApplicationSummary info) {
	if (operations.appRunning(info.getId())) {
	    return MicroserviceState.RUNNING;
	}
	switch (info.getPackageState()) {
	case "FAILED":
	    return MicroserviceState.FAILED;
	case "STAGED":
	    return MicroserviceState.STAGED;
	default:
	    if (info.getPackageUpdatedAt() == null) {
		return MicroserviceState.CREATED;
	    } else {
		return MicroserviceState.UPLOADED;
	    }
	}
    }

    /**
     * get microservice current state and stabilize it (i.e. If exists
     * microservice not running, change its desired state to STOPPED to attain
     * an stable state)
     * 
     * @param info
     * @return
     */
    private MicroserviceState stabilizeState(SpaceApplicationSummary info) {
	String msId = info.getId();
	if (operations.appRunning(msId)) {
	    return MicroserviceState.RUNNING;
	}
	if (CFMicroserviceDesiredState.STARTED.toString().equals(info.getState())) {
	    logger.info("microservice {} state will be stabilized.", info);
	    operations.stopApp(msId);
	}
	switch (info.getPackageState()) {
	case "FAILED":
	    return MicroserviceState.FAILED;
	case "STAGED":
	    return MicroserviceState.STAGED;
	default:
	    if (info.getPackageUpdatedAt() == null) {
		return MicroserviceState.CREATED;
	    } else {
		return MicroserviceState.UPLOADED;
	    }
	}
    }

    private Map<String, String> parseEnv(SpaceApplicationSummary info) {
	Map<String, String> env = new HashMap<>(info.getEnvironmentJsons().entrySet().stream()
		.collect(Collectors.toMap(entry -> entry.getKey(), entry -> (String) entry.getValue())));
	env.remove(pathKeyInEnv);
	return env;
    }

    private Set<Route> parseRoutes(SpaceApplicationSummary info) {
	return info.getRoutes().stream().map(route -> new Route(route.getHost(), route.getDomain().getName()))
		.collect(Collectors.toSet());
    }

    private String parsePath(SpaceApplicationSummary info) {
	String path = (String) info.getEnvironmentJsons().get(pathKeyInEnv);
	return path == null ? "" : path;
    }

    private Set<String> parseServices(SpaceApplicationSummary info) {
	return new HashSet<>(info.getServiceNames());
    }

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
}
