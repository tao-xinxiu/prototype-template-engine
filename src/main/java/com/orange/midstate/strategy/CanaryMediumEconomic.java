package com.orange.midstate.strategy;

import java.util.Set;

import com.orange.model.SiteDeploymentConfig;
import com.orange.model.state.OverviewApp;

public class CanaryMediumEconomic extends Canary {
    public CanaryMediumEconomic(Set<OverviewApp> updateApps, OverviewApp desiredApp, SiteDeploymentConfig config) {
	super(updateApps, desiredApp, config);
	if (currentRelatedApps.size() > 2) {
	    throw new IllegalStateException(
		    "Strategy unsupported situation: more than two versions of the app coexist");
	}
    }

    @Override
    public Set<OverviewApp> onNbProcessesUpdated() {
	Set<OverviewApp> desiredRelatedApps = Util.deepCopy(currentRelatedApps);
	OverviewApp instantiatedDesiredApp = instantiatedDesiredApp(desiredRelatedApps);
	Set<OverviewApp> oldApps = Util.exludedApps(desiredRelatedApps, instantiatedDesiredApp);
	if (oldApps.size() == 0) { // initial deploy
	    instantiatedDesiredApp.setNbProcesses(desiredApp.getNbProcesses());
	} else {
	    OverviewApp oldApp = oldApps.iterator().next();
	    boolean oldAppsScaled = (oldApp.getNbProcesses()
		    + instantiatedDesiredApp.getNbProcesses() == (desiredApp.getNbProcesses()));
	    if (oldAppsScaled) {
		instantiatedDesiredApp.setNbProcesses(instantiatedDesiredApp.getNbProcesses() + 1);
	    } else {
		oldApp.setNbProcesses(oldApp.getNbProcesses() - 1);
	    }
	}
	return desiredRelatedApps;
    }
}
