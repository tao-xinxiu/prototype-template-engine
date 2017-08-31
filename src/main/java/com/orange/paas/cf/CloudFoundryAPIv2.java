package com.orange.paas.cf;

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
import com.orange.model.state.cf.CFMicroserviceDesiredState;
import com.orange.paas.PaaSAPI;

public class CloudFoundryAPIv2 extends PaaSAPI {
    private final Logger logger;
    private CloudFoundryOperations operations;
    public static final String pathKeyInEnv = "RUM_PATH";

    public CloudFoundryAPIv2(PaaSSite site, OperationConfig operationConfig) {
	super(site, operationConfig);
	this.operations = Main.getCloudFoundryOperations(site, operationConfig);
	this.logger = LoggerFactory.getLogger(String.format("%s(%s)", getClass(), site.getName()));
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

    private MicroserviceState stabilizeState(SpaceApplicationSummary info) {
	String msId = info.getId();
	if (operations.appRunning(msId)) {
	    return MicroserviceState.RUNNING;
	}
	// If microservice is not running, change its desired state to STOPPED
	// to attain an stable state.
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
}
