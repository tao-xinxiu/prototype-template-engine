package com.orange.paas.cf;

import java.util.Arrays;
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
import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.MicroserviceState;
import com.orange.model.architecture.cf.CFMicroservice;
import com.orange.model.architecture.cf.CFMicroserviceDesiredState;
import com.orange.model.architecture.cf.CFMicroserviceState;
import com.orange.model.architecture.cf.Route;
import com.orange.model.workflow.Step;
import com.orange.paas.PaaSAPI;

public class CloudFoundryAPIv2 extends PaaSAPI {
    public static final String pathKeyInEnv = "RUM_PATH";
    private final Logger logger;

    private CloudFoundryOperations operations;

    public CloudFoundryAPIv2(PaaSSiteAccess site, OperationConfig operationConfig) {
	super(site, operationConfig);
	this.logger = LoggerFactory.getLogger(String.format("%s(%s)", getClass(), site.getName()));
	this.operations = Main.getCloudFoundryOperations(site, operationConfig);
    }

    @Override
    public Set<Microservice> get() {
	logger.info("Start getting the current architecture ...");
	return operations.listSpaceApps().parallelStream()
		.map(info -> init(info.getId(), parseName(info.getName()), parseVersion(info.getName()),
			parsePath(info), parseState(info), info.getInstances(), parseEnv(info), parseRoutes(info),
			parseServices(info), info.getMemory() + "M", info.getDiskQuota() + "M"))
		.collect(Collectors.toSet());
    }

    public Set<Microservice> getStabilizedMicroservices() {
	logger.info("Start getting the current architecture and stabilize it ...");
	return operations.listSpaceApps().parallelStream()
		.map(info -> init(info.getId(), parseName(info.getName()), parseVersion(info.getName()),
			parsePath(info), stabilizeState(info), info.getInstances(), parseEnv(info), parseRoutes(info),
			parseServices(info), info.getMemory() + "M", info.getDiskQuota() + "M"))
		.collect(Collectors.toSet());
    }

    @Override
    public Step add(Microservice microservice) {
	CFMicroservice desiredMicroservice = new CFMicroservice(microservice);
	return new Step(String.format("add microservice %s", desiredMicroservice)) {
	    @SuppressWarnings("unchecked")
	    @Override
	    public void exec() {
		String msId = operations.create((String) desiredMicroservice.get("name"),
			(int) desiredMicroservice.get("nbProcesses"),
			(Map<String, String>) desiredMicroservice.get("env"));
		CFMicroservice currentMicroservice = initCFMs(msId, (String) desiredMicroservice.get("name"), null,
			CFMicroserviceState.CREATED, (int) desiredMicroservice.get("nbProcesses"),
			(Map<String, String>) desiredMicroservice.get("env"), new HashSet<>(), new HashSet<>());
		operations.updateRoutesIfNeed(msId, (Set<Route>) currentMicroservice.get("routes"),
			(Set<Route>) desiredMicroservice.get("routes"));
		operations.updateServicesIfNeed(msId, (Set<String>) currentMicroservice.get("services"),
			(Set<String>) desiredMicroservice.get("services"));
		operations.updateStateIfNeed(currentMicroservice, desiredMicroservice);
	    }
	};
    }

    @Override
    public Step remove(Microservice microservice) {
	return new SiteStep(String.format("remove microservice [%s]", microservice.get("guid"))) {
	    @SuppressWarnings("unchecked")
	    @Override
	    public void exec() {
		operations.updateServicesIfNeed((String) microservice.get("guid"),
			(Set<String>) microservice.get("services"), new HashSet<>());
		operations.delete((String) microservice.get("guid"));
	    }
	};
    }

    @Override
    public Step modify(Microservice currentMicroservice, Microservice desiredMicroservice) {
	CFMicroservice currentCFMicroservice = new CFMicroservice(currentMicroservice);
	CFMicroservice desiredCFMicroservice = new CFMicroservice(desiredMicroservice);
	return new SiteStep(
		String.format("update microservice from %s to %s", currentCFMicroservice, desiredCFMicroservice)) {
	    @SuppressWarnings("unchecked")
	    @Override
	    public void exec() {
		String msId = (String) currentCFMicroservice.get("guid");
		operations.updateRoutesIfNeed(msId, (Set<Route>) currentCFMicroservice.get("routes"),
			(Set<Route>) desiredCFMicroservice.get("routes"));
		operations.updateNameIfNeed(msId, (String) currentCFMicroservice.get("name"),
			(String) desiredCFMicroservice.get("name"));
		if (desiredCFMicroservice.get("state") != CFMicroserviceState.CREATED
			&& desiredCFMicroservice.get("path") != null
			&& !desiredCFMicroservice.get("path").equals(currentCFMicroservice.get("path"))) {
		    operations.updatePath(msId, (String) desiredCFMicroservice.get("path"),
			    (Map<String, String>) currentCFMicroservice.get("env"));
		    currentCFMicroservice.set("state", CFMicroserviceState.UPLOADED);

		}
		boolean needRestage = false;
		if (!currentCFMicroservice.get("services").equals(desiredMicroservice.get("services"))) {
		    operations.updateServicesIfNeed(msId, (Set<String>) currentCFMicroservice.get("services"),
			    (Set<String>) desiredCFMicroservice.get("services"));
		    needRestage = true;
		}
		if (!currentCFMicroservice.get("env").equals(desiredCFMicroservice.get("env"))) {
		    operations.updateEnv(msId, (Map<String, String>) desiredCFMicroservice.get("env"));
		    needRestage = true;
		}
		if (needRestage && stagedMicroservice((CFMicroserviceState) currentCFMicroservice.get("state"))) {
		    operations.restage((String) currentCFMicroservice.get("guid"));
		    currentCFMicroservice.set("state", CFMicroserviceState.staging);
		}
		operations.updateNbProcessesIfNeed(msId, (int) currentCFMicroservice.get("nbProcesses"),
			(int) desiredCFMicroservice.get("nbProcesses"));
		operations.updateStateIfNeed(currentCFMicroservice, desiredCFMicroservice);
	    }
	};

    }

    private boolean stagedMicroservice(CFMicroserviceState msState) {
	Set<CFMicroserviceState> unstagedStates = new HashSet<>(
		Arrays.asList(CFMicroserviceState.CREATED, CFMicroserviceState.UPLOADED));
	return !unstagedStates.contains(msState);
    }

    /**
     * Get microservice name from Cloud Foundry microservice name
     * 
     * @param name
     *            CF microservice instance unique name, mapped to microservice model
     *            as "name_version"
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
     *            CF microservice instance unique name, mapped to microservice model
     *            as "name_version"
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
     * get microservice current state and stabilize it (i.e. If exists microservice
     * not running, change its desired state to STOPPED to attain an stable state)
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
	    operations.stop(msId);
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

    private Set<String> parseRoutes(SpaceApplicationSummary info) {
	return info.getRoutes().stream().map(route -> route.getHost() + "." + route.getDomain().getName())
		.collect(Collectors.toSet());
    }

    private String parsePath(SpaceApplicationSummary info) {
	String path = (String) info.getEnvironmentJsons().get(pathKeyInEnv);
	return path == null ? "" : path;
    }

    private Set<String> parseServices(SpaceApplicationSummary info) {
	return new HashSet<>(info.getServiceNames());
    }

    private Microservice init(String guid, String name, String version, String path, MicroserviceState state,
	    int nbProcesses, Map<String, String> env, Set<String> routes, Set<String> services, String memory,
	    String disk) {
	Map<String, Object> attributes = new HashMap<>();
	attributes.put("guid", guid);
	attributes.put("name", name);
	attributes.put("version", version);
	attributes.put("path", path);
	attributes.put("state", state);
	attributes.put("nbProcesses", nbProcesses);
	attributes.put("env", env);
	attributes.put("routes", routes);
	attributes.put("services", services);
	attributes.put("memory", memory);
	attributes.put("disk", disk);
	return new Microservice(attributes);
    }

    private CFMicroservice initCFMs(String guid, String name, String path, CFMicroserviceState state, int nbProcesses,
	    Map<String, String> env, Set<Route> routes, Set<String> services) {
	Map<String, Object> attributes = new HashMap<>();
	attributes.put("guid", guid);
	attributes.put("name", name);
	attributes.put("path", path);
	attributes.put("state", state);
	attributes.put("nbProcesses", nbProcesses);
	attributes.put("env", env);
	attributes.put("routes", routes);
	attributes.put("services", services);
	return new CFMicroservice(attributes);
    }

    abstract class SiteStep extends Step {
	public SiteStep(String stepName) {
	    super(stepName + " at site " + site.getName());
	}
    }
}
