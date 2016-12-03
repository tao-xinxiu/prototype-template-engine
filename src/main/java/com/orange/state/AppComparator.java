package com.orange.state;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.orange.model.DropletState;
import com.orange.model.OverviewApp;
import com.orange.model.OverviewDroplet;
import com.orange.model.Route;

public class AppComparator {
	private OverviewApp currentApp;
	private OverviewApp desiredApp;
	private boolean nameUpdated;
	private boolean routesUpdated;
	private List<Route> addedRoutes = new ArrayList<>();
	private List<Route> removedRoutes = new ArrayList<>();
	private List<OverviewDroplet> addedDroplets = new ArrayList<>();
	private List<OverviewDroplet> removedDroplets = new ArrayList<>();
	private boolean currentDropletUpdated;
	private OverviewDroplet desiredCurrentDroplet;
	private boolean appStoped;

	public AppComparator(OverviewApp currentApp, OverviewApp desiredApp) {
		if (!currentApp.getGuid().equals(desiredApp.getGuid())) {
			throw new IllegalStateException(
					String.format("Illegal AppComparator with different guid in currentApp %s and desiredApp %s",
							currentApp, desiredApp));
		}
		this.currentApp = currentApp;
		this.desiredApp = desiredApp;
		if (!currentApp.getName().equals(desiredApp.getName())) {
			nameUpdated = true;
		}
		if (!currentApp.getRoutes().equals(desiredApp.getRoutes())) {
			routesUpdated = true;
			addedRoutes = desiredApp.listRoutes().stream().filter(route -> !currentApp.listRoutes().contains(route))
					.collect(Collectors.toList());
			removedRoutes = currentApp.listRoutes().stream().filter(route -> !desiredApp.listRoutes().contains(route))
					.collect(Collectors.toList());
		}
		List<String> desiredDropletIds = new ArrayList<>();
		for (OverviewDroplet desiredDroplet : desiredApp.getDroplets()) {
			if (desiredDroplet.getGuid() == null) {
				addedDroplets.add(desiredDroplet);
			} else {
				desiredDropletIds.add(desiredDroplet.getGuid());
				OverviewDroplet currentDroplet = currentApp.getDroplets().stream()
						.filter(droplet -> droplet.getGuid().equals(desiredDroplet.getGuid())).findAny().orElse(null);
				if (currentDroplet == null) {
					throw new IllegalStateException(
							String.format("Desired droplet guid [%s] of app [%s] is not present in the current state.",
									desiredDroplet.getGuid(), desiredApp.getGuid()));
				}
			}
		}
		removedDroplets = currentApp.getDroplets().stream()
				.filter(droplet -> !desiredDropletIds.contains(droplet.getGuid())).collect(Collectors.toList());
		OverviewDroplet oldCurrentDroplet = currentApp.getDroplets().stream()
				.filter(droplet -> droplet.getState() == DropletState.RUNNING).findAny().orElse(null);
		desiredCurrentDroplet = desiredApp.getDroplets().stream()
				.filter(droplet -> droplet.getState() == DropletState.RUNNING).findAny().orElse(null);
		if (desiredCurrentDroplet == null) {
			appStoped = true;
		} else if (desiredCurrentDroplet.getGuid() != null) {
			currentDropletUpdated = !desiredCurrentDroplet.getGuid().equals(oldCurrentDroplet.getGuid());
		}
	}

	public OverviewApp getCurrentApp() {
		return currentApp;
	}

	public OverviewApp getDesiredApp() {
		return desiredApp;
	}

	public boolean isNameUpdated() {
		return nameUpdated;
	}

	public boolean isRoutesUpdated() {
		return routesUpdated;
	}

	public List<OverviewDroplet> getAddedDroplets() {
		return addedDroplets;
	}

	public List<OverviewDroplet> getRemovedDroplets() {
		return removedDroplets;
	}

	public List<Route> getAddedRoutes() {
		return addedRoutes;
	}

	public List<Route> getRemovedRoutes() {
		return removedRoutes;
	}

	public boolean isCurrentDropletUpdated() {
		return currentDropletUpdated;
	}

	public OverviewDroplet getDesiredCurrentDroplet() {
		return desiredCurrentDroplet;
	}

	public boolean isAppStoped() {
		return appStoped;
	}
}
