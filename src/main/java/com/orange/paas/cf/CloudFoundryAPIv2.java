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
import com.orange.model.state.MicroserviceState;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.model.state.ArchitectureSite;
import com.orange.model.state.Route;
import com.orange.model.state.cf.CFMicroserviceArchitecture;
import com.orange.model.state.cf.CFMicroserviceDesiredState;
import com.orange.model.state.cf.CFMicroserviceState;
import com.orange.model.workflow.Step;
import com.orange.paas.PaaSAPI;

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
		String msId = operations.create(desiredMicroservice.getName(), desiredMicroservice.getNbProcesses(),
			desiredMicroservice.getEnv());
		CFMicroserviceArchitecture currentMicroservice = new CFMicroserviceArchitecture(msId,
			desiredMicroservice.getName(), null, CFMicroserviceState.CREATED,
			desiredMicroservice.getNbProcesses(), desiredMicroservice.getEnv(), new HashSet<>(),
			new HashSet<>());
		operations.updateRoutesIfNeed(msId, currentMicroservice.getRoutes(), desiredMicroservice.getRoutes());
		operations.updateServicesIfNeed(msId, currentMicroservice.getServices(),
			desiredMicroservice.getServices());
		operations.updateStateIfNeed(currentMicroservice, desiredMicroservice);
	    }
	};
    }

    @Override
    public Step remove(ArchitectureMicroservice microservice) {
	return new SiteStep(String.format("remove microservice [%s]", microservice.getGuid())) {
	    @Override
	    public void exec() {
		operations.updateServicesIfNeed(microservice.getGuid(), microservice.getServices(), new HashSet<>());
		operations.delete(microservice.getGuid());
	    }
	};
    }

    @Override
    public Step update(ArchitectureMicroservice currentMicroservice, ArchitectureMicroservice desiredMicroservice) {
	CFMicroserviceArchitecture currentCFMicroservice = new CFMicroserviceArchitecture(currentMicroservice);
	CFMicroserviceArchitecture desiredCFMicroservice = new CFMicroserviceArchitecture(desiredMicroservice);
	return new SiteStep(
		String.format("update microservice from %s to %s", currentCFMicroservice, desiredCFMicroservice)) {
	    @Override
	    public void exec() {
		String msId = currentCFMicroservice.getGuid();
		operations.updateRoutesIfNeed(msId, currentCFMicroservice.getRoutes(),
			desiredCFMicroservice.getRoutes());
		operations.updateNameIfNeed(msId, currentCFMicroservice.getName(), desiredCFMicroservice.getName());
		if (desiredCFMicroservice.getState() != CFMicroserviceState.CREATED
			&& desiredCFMicroservice.getPath() != null
			&& !desiredCFMicroservice.getPath().equals(currentCFMicroservice.getPath())) {
		    operations.updatePath(msId, desiredCFMicroservice.getPath(), currentCFMicroservice.getEnv());
		    currentCFMicroservice.setState(CFMicroserviceState.UPLOADED);
		}
		boolean needRestage = false;
		if (!currentCFMicroservice.getServices().equals(desiredMicroservice.getServices())) {
		    operations.updateServicesIfNeed(msId, currentCFMicroservice.getServices(),
			    desiredCFMicroservice.getServices());
		    needRestage = true;
		}
		if (!currentCFMicroservice.getEnv().equals(desiredCFMicroservice.getEnv())) {
		    operations.updateEnv(msId, desiredCFMicroservice.getEnv());
		    needRestage = true;
		}
		if (needRestage && stagedMicroservice(currentCFMicroservice.getState())) {
		    operations.restage(currentCFMicroservice.getGuid());
		    currentCFMicroservice.setState(CFMicroserviceState.staging);
		}
		operations.updateNbProcessesIfNeed(msId, currentCFMicroservice.getNbProcesses(),
			desiredCFMicroservice.getNbProcesses());
		operations.updateStateIfNeed(currentCFMicroservice, desiredCFMicroservice);
	    }
	};

    }

    private boolean stagedMicroservice(CFMicroserviceState msState) {
	Set<CFMicroserviceState> upstagedStates = new HashSet<>(
		Arrays.asList(CFMicroserviceState.CREATED, CFMicroserviceState.UPLOADED));
	return !upstagedStates.contains(msState);
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
	    super(stepName + " at site " + site.getName());
	}
    }
}
