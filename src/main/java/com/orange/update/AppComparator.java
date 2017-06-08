package com.orange.update;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.state.OverviewApp;
import com.orange.model.state.Route;

public class AppComparator {
    private OverviewApp currentApp;
    private OverviewApp desiredApp;

    private boolean appUpdated;
    private boolean pathUpdated;
    private boolean routesAdded;
    private boolean routesRemoved;
    private Set<Route> addedRoutes = new HashSet<>();
    private Set<Route> removedRoutes = new HashSet<>();
    private boolean stateUpdated;
    private boolean nbProcessesUpdated;
    private boolean envUpdated;

    public AppComparator(OverviewApp currentApp, OverviewApp desiredApp) {
	if (!currentApp.getGuid().equals(desiredApp.getGuid())) {
	    throw new IllegalStateException(
		    String.format("Illegal AppComparator with different guid in currentApp %s and desiredApp %s",
			    currentApp, desiredApp));
	}
	this.currentApp = currentApp;
	this.desiredApp = desiredApp;
	appUpdated = !currentApp.equals(desiredApp);
	if (!currentApp.getRoutes().equals(desiredApp.getRoutes())) {
	    addedRoutes = desiredApp.listRoutes().stream().filter(route -> !currentApp.listRoutes().contains(route))
		    .collect(Collectors.toSet());
	    routesAdded = !addedRoutes.isEmpty();
	    removedRoutes = currentApp.listRoutes().stream().filter(route -> !desiredApp.listRoutes().contains(route))
		    .collect(Collectors.toSet());
	    routesRemoved = !removedRoutes.isEmpty();
	}
	pathUpdated = (desiredApp.getPath() != null);
	stateUpdated = !(currentApp.getState() == desiredApp.getState());
	nbProcessesUpdated = !(currentApp.getNbProcesses() == desiredApp.getNbProcesses());
	envUpdated = !(currentApp.getEnv().equals(desiredApp.getEnv()));
    }

    public OverviewApp getCurrentApp() {
	return currentApp;
    }

    public OverviewApp getDesiredApp() {
	return desiredApp;
    }

    public Set<Route> getAddedRoutes() {
	return addedRoutes;
    }

    public Set<Route> getRemovedRoutes() {
	return removedRoutes;
    }

    public boolean isStateUpdated() {
	return stateUpdated;
    }

    public boolean isNbProcessesUpdated() {
	return nbProcessesUpdated;
    }

    public boolean isEnvUpdated() {
	return envUpdated;
    }

    public boolean isAppUpdated() {
        return appUpdated;
    }

    public boolean isPathUpdated() {
        return pathUpdated;
    }

    public boolean isRoutesAdded() {
        return routesAdded;
    }

    public boolean isRoutesRemoved() {
        return routesRemoved;
    }
}
