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
import com.orange.model.state.AppState;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.OverviewSite;
import com.orange.model.state.Route;
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
    public OverviewSite getOverviewSite() {
	logger.info("Start getting the current state ...");
	return new OverviewSite(operations.listSpaceApps().parallelStream()
		.map(appInfo -> new OverviewApp(appInfo.getId(), parseName(appInfo.getName()),
			parseInstVersion(appInfo.getName()), parsePath(appInfo), parseState(appInfo),
			appInfo.getInstances(), parseEnv(appInfo), parseRoutes(appInfo), parseServices(appInfo)))
		.collect(Collectors.toSet()));
    }

    /**
     * Get OverviewApp name from CF stored appName
     * 
     * @param appName
     *            CF stored microservice instance unique name, mapped as "name"
     *            + "_" + "instanceVersion" to microservice model (OverviewApp)
     * @return
     */
    private String parseName(String appName) {
	int delimiterPosition = appName.lastIndexOf("_");
	if (delimiterPosition == -1) {
	    return appName;
	} else {
	    return appName.substring(0, delimiterPosition);
	}
    }

    private String parseInstVersion(String appName) {
	int delimiterPosition = appName.lastIndexOf("_");
	if (delimiterPosition == -1) {
	    return "";
	} else {
	    return appName.substring(delimiterPosition + 1);
	}
    }

    private AppState parseState(SpaceApplicationSummary appInfo) {
	if (appInfo.getRunningInstances() > 0) {
	    return AppState.RUNNING;
	}
	switch (appInfo.getPackageState()) {
	case "FAILED":
	    return AppState.FAILED;
	case "STAGED":
	    return AppState.STAGED;
	default:
	    if (appInfo.getPackageUpdatedAt() == null) {
		return AppState.CREATED;
	    } else {
		return AppState.UPLOADED;
	    }
	}
    }

    private Map<String, String> parseEnv(SpaceApplicationSummary appInfo) {
	Map<String, String> env = new HashMap<>(appInfo.getEnvironmentJsons().entrySet().stream()
		.collect(Collectors.toMap(entry -> entry.getKey(), entry -> (String) entry.getValue())));
	env.remove(pathKeyInEnv);
	return env;
    }

    private Set<Route> parseRoutes(SpaceApplicationSummary appInfo) {
	return appInfo.getRoutes().stream().map(route -> new Route(route.getHost(), route.getDomain().getName()))
		.collect(Collectors.toSet());
    }

    private String parsePath(SpaceApplicationSummary appInfo) {
	return (String) appInfo.getEnvironmentJsons().get(pathKeyInEnv);
    }

    private Set<String> parseServices(SpaceApplicationSummary appInfo) {
	return new HashSet<>(appInfo.getServiceNames());
    }
}
