package com.orange.state;

import java.util.ArrayList;
import java.util.List;

import com.orange.model.OverviewApp;
import com.orange.model.OverviewDroplet;

public class AppComparator {
	private OverviewApp currentApp;
	private OverviewApp desiredApp;
	private boolean nameUpdated;
	private boolean routesUpdated;
	private List<OverviewDroplet> addedDroplets = new ArrayList<>();
	private List<OverviewDroplet> removedDroplets = new ArrayList<>();
	private List<DropletComparator> dropletComparators = new ArrayList<>();

	public AppComparator(OverviewApp currentApp, OverviewApp desiredApp) {
		assert currentApp.getGuid().equals(desiredApp.getGuid());
		this.currentApp = currentApp;
		this.desiredApp = desiredApp;
		if (!currentApp.getName().equals(desiredApp.getName())) {
			nameUpdated = true;
		}
		if (!currentApp.getRoutes().equals(desiredApp.getRoutes())) {
			routesUpdated = true;
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
}
