package com.orange.state;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
	private List<DropletComparator> dropletComparators = new ArrayList<>();

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

	public List<DropletComparator> getDropletComparators() {
		return dropletComparators;
	}

	public List<Route> getAddedRoutes() {
		return addedRoutes;
	}

	public List<Route> getRemovedRoutes() {
		return removedRoutes;
	}
}
