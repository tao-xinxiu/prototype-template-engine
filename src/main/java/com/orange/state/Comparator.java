package com.orange.state;

import java.util.List;
import java.util.stream.Collectors;

import com.orange.model.Overview;
import com.orange.model.OverviewApp;
import com.orange.model.PaaSSite;

public class Comparator {
	private Overview currentState;
	private Overview desiredState;

	public Comparator(Overview currentState, Overview desiredState) {
		this.currentState = currentState;
		this.desiredState = desiredState;
	}

	// Comparator is valid iff currentState and desiredState are for the same
	// PaaS sites.
	public boolean valid() {
		return currentState.listPaaSSites().equals(desiredState.listPaaSSites());
	}

	/**
	 * return list of new apps to be created, i.e. apps in desiredState with
	 * guid null
	 * 
	 * @param site
	 * @return
	 */
	public List<OverviewApp> getAddedApp(PaaSSite site) {
		return desiredState.getOverviewSite(site.getName()).getOverviewApps().stream()
				.filter(app -> app.getGuid() == null).collect(Collectors.toList());
	}

	/**
	 * return list of apps to be deleted, i.e. apps with guid in currentState
	 * but not in desiredState
	 * 
	 * @param site
	 * @return
	 */
	public List<OverviewApp> getRemovedApp(PaaSSite site) {
		List<String> desiredAppIds = desiredState.getOverviewSite(site.getName()).getOverviewApps().stream()
				.map(OverviewApp::getGuid).collect(Collectors.toList());
		return currentState.getOverviewSite(site.getName()).getOverviewApps().stream()
				.filter(currentApp -> !desiredAppIds.contains(currentApp.getGuid())).collect(Collectors.toList());
	}

}
