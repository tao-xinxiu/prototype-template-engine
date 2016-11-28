package com.orange.state;

import java.util.ArrayList;
import java.util.List;

import com.orange.model.OverviewApp;
import com.orange.model.OverviewDroplet;

public class AppComparator {
	private boolean nameUpdated;
	private boolean routesUpdated;
	private List<OverviewDroplet> addedDroplets = new ArrayList<>();
	private List<OverviewDroplet> removedDroplets = new ArrayList<>();
	private List<DropletComparator> dropletComparators = new ArrayList<>();
	
	public AppComparator(OverviewApp currentApp, OverviewApp desiredApp) {
		
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
