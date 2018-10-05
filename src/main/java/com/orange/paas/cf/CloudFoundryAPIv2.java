package com.orange.paas.cf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v2.spaces.SpaceApplicationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	this.operations = CloudFoundryOperations.getCloudFoundryOperations(site, operationConfig);
    }

    @Override
    public Set<Microservice> get() {
	logger.info("Start getting the current architecture ...");
	return operations.listSpaceApps().parallelStream().map(info -> parseMicroservice(info))
		.collect(Collectors.toSet());
    }

    public Set<Microservice> getStabilizedMicroservices() {
	logger.info("Start getting the current architecture and stabilize it ...");
	Set<Microservice> microservices = new HashSet<>();
	for (SpaceApplicationSummary appSummary : operations.listSpaceApps()) {
	    Microservice microservice = parseMicroservice(appSummary);
	    microservice.set("state", stabilizeState(appSummary));
	    microservices.add(microservice);
	}
	return microservices;
    }

    @Override
    public Step add(Microservice microservice) {
	CFMicroservice desiredMicroservice = new CFMicroservice(microservice);
	return new Step(String.format("add microservice %s", desiredMicroservice)) {
	    @SuppressWarnings("unchecked")
	    @Override
	    public void exec() {
		String msId = operations.create(desiredMicroservice);
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("guid", msId);
		attributes.put("name", desiredMicroservice.get("name"));
		attributes.put("path", null);
		attributes.put("state", CFMicroserviceState.CREATED);
		attributes.put("nbProcesses", desiredMicroservice.get("nbProcesses"));
		attributes.put("env", desiredMicroservice.get("env"));
		attributes.put("routes", new HashSet<>());
		attributes.put("services", new HashSet<>());
		CFMicroservice currentMicroservice = new CFMicroservice(attributes);
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
		if (!currentCFMicroservice.eqAttr("name", desiredCFMicroservice)) {
		    operations.updateName(msId, (String) currentCFMicroservice.get("name"),
			    (String) desiredCFMicroservice.get("name"));
		    currentCFMicroservice.copyAttr("name", desiredCFMicroservice);
		}
		if (desiredCFMicroservice.get("state") != CFMicroserviceState.CREATED
			&& desiredCFMicroservice.get("path") != null
			&& !desiredCFMicroservice.get("path").equals(currentCFMicroservice.get("path"))) {
		    operations.updatePath(msId, (String) desiredCFMicroservice.get("path"),
			    (Map<String, String>) currentCFMicroservice.get("env"));
		    currentCFMicroservice.copyAttr("path", desiredCFMicroservice);
		    currentCFMicroservice.set("state", CFMicroserviceState.UPLOADED);
		}
		boolean needRestage = false;
		if (!currentCFMicroservice.get("services").equals(desiredCFMicroservice.get("services"))) {
		    operations.updateServicesIfNeed(msId, (Set<String>) currentCFMicroservice.get("services"),
			    (Set<String>) desiredCFMicroservice.get("services"));
		    currentCFMicroservice.copyAttr("services", desiredCFMicroservice);
		    needRestage = true;
		}
		if (!currentCFMicroservice.get("env").equals(desiredCFMicroservice.get("env"))) {
		    operations.updateEnv(msId, (Map<String, String>) desiredCFMicroservice.get("env"));
		    currentCFMicroservice.copyAttr("env", desiredCFMicroservice);
		    needRestage = true;
		}
		if (needRestage && stagedMicroservice((CFMicroserviceState) currentCFMicroservice.get("state"))) {
		    operations.restage((String) currentCFMicroservice.get("guid"));
		    currentCFMicroservice.set("state", CFMicroserviceState.staging);
		}
		operations.updateNbProcessesIfNeed(msId, (int) currentCFMicroservice.get("nbProcesses"),
			(int) desiredCFMicroservice.get("nbProcesses"));
		if ((int) currentCFMicroservice.get("nbProcesses") < (int) desiredCFMicroservice.get("nbProcesses")) {
		    currentCFMicroservice.set("state", CFMicroserviceState.starting);
		}
		operations.updateStateIfNeed(currentCFMicroservice, desiredCFMicroservice);
	    }
	};
    }

    private Microservice parseMicroservice(SpaceApplicationSummary info) {
	Map<String, Object> attributes = new HashMap<>();
	attributes.put("guid", info.getId());
	attributes.put("name", parseName(info.getName()));
	attributes.put("version", parseVersion(info.getName()));
	attributes.put("path", parsePath(info));
	attributes.put("state", parseState(info));
	attributes.put("nbProcesses", info.getInstances());
	attributes.put("env", parseEnv(info));
	attributes.put("routes", parseRoutes(info));
	attributes.put("services", parseServices(info));
	attributes.put("memory", info.getMemory());
	attributes.put("disk", info.getDiskQuota());
	return new Microservice(attributes);
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
	    logger.info("Waiting microservice {} state to be stabilized.", info);
	    operations.waitRunning(msId, info.getInstances());
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

    private Map<String, String> parseEnv(SpaceApplicationSummary info) {
	Map<String, String> env = new HashMap<>(info.getEnvironmentJsons().entrySet().stream()
		.collect(Collectors.toMap(entry -> entry.getKey(), entry -> (String) entry.getValue())));
	env.remove(pathKeyInEnv);
	return env;
    }

    private Set<String> parseRoutes(SpaceApplicationSummary info) {
	List<org.cloudfoundry.client.v2.routes.Route> routes = info.getRoutes();
	if (routes == null) {
	    return new HashSet<>();
	} else {
	    return routes.stream().map(route -> parseRoute(route)).collect(Collectors.toSet());
	}
    }

    private String parseRoute(org.cloudfoundry.client.v2.routes.Route route) {
	return route.getHost() + "." + route.getDomain().getName();
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
	    super(stepName + " at site " + site.getName());
	}
    }
}
