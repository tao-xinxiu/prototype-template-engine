package com.orange.state;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.orange.model.DropletState;
import com.orange.model.OverviewApp;
import com.orange.model.OverviewSite;

public class SiteComparator {
	// list of apps to be created, i.e. apps in desiredState with guid null
	private List<OverviewApp> addedApp = new ArrayList<>();
	// list of apps to be deleted, i.e. app guid in currentState but not in
	// desiredState
	private List<OverviewApp> removedApp = new ArrayList<>();
	private List<AppComparator> appComparators = new ArrayList<>();

	public SiteComparator(OverviewSite currentState, OverviewSite desiredState) {
		List<String> desiredAppIds = new ArrayList<>();
		for (OverviewApp desiredApp : desiredState.getOverviewApps()) {
			desiredAppIds.add(desiredApp.getGuid());
			if (desiredApp.getGuid() == null) {
				if (desiredApp.getDroplets().stream().anyMatch(droplet -> droplet.getGuid() != null)) {
					throw new IllegalStateException(
							String.format("New app [%s] should not have a existed droplet (i.e. droplet guid not null)",
									desiredApp.getName()));
				}
				if (desiredApp.getDroplets().stream().filter(droplet -> droplet.getState() == DropletState.RUNNING)
						.count() > 1) {
					throw new IllegalStateException(String.format(
							"New app [%s] should not have more than one droplet RUNNING", desiredApp.getName()));
				}
				addedApp.add(desiredApp);
			} else {
				OverviewApp currentApp = currentState.getOverviewApps().stream()
						.filter(app -> app.getGuid().equals(desiredApp.getGuid())).findAny().orElse(null);
				if (currentApp == null) {
					throw new IllegalStateException(String.format(
							"Desired app guid [%s] is not present in the current state.", desiredApp.getGuid()));
				}
				appComparators.add(new AppComparator(currentApp, desiredApp));
			}
		}
		removedApp = currentState.getOverviewApps().stream()
				.filter(currentApp -> !desiredAppIds.contains(currentApp.getGuid())).collect(Collectors.toList());
	}

	public List<OverviewApp> getAddedApp() {
		return addedApp;
	}

	public List<OverviewApp> getRemovedApp() {
		return removedApp;
	}

	public List<AppComparator> getAppComparators() {
		return appComparators;
	}
}
