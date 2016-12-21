package com.orange.update;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.state.OverviewApp;
import com.orange.model.state.OverviewSite;

public class SiteComparator {
	// list of apps to be created, i.e. apps in desiredState with guid null
	private Set<OverviewApp> addedApp = new HashSet<>();
	// list of apps to be deleted, i.e. app guid in currentState but not in
	// desiredState
	private Set<OverviewApp> removedApp = new HashSet<>();
	private Set<AppComparator> appComparators = new HashSet<>();

	public SiteComparator(OverviewSite currentState, OverviewSite desiredState) {
		Set<String> desiredAppIds = new HashSet<>();
		for (OverviewApp desiredApp : desiredState.getOverviewApps()) {
			if (desiredApp.getGuid() == null) {
				addedApp.add(desiredApp);
			} else {
				desiredAppIds.add(desiredApp.getGuid());
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
				.filter(currentApp -> !desiredAppIds.contains(currentApp.getGuid())).collect(Collectors.toSet());
	}

	public Set<OverviewApp> getAddedApp() {
		return addedApp;
	}

	public Set<OverviewApp> getRemovedApp() {
		return removedApp;
	}

	public Set<AppComparator> getAppComparators() {
		return appComparators;
	}
}
