package com.orange.model;

import java.util.ArrayList;
import java.util.List;

public class OverviewApp {
	private String guid;
	private String name;
	private List<String> routes;
	private List<OverviewDroplet> droplets;

	public OverviewApp(String guid, String name, List<String> routes) {
		this.guid = guid;
		this.name = name;
		this.routes = routes;
		this.droplets = new ArrayList<>();
	}

	public String getGuid() {
		return guid;
	}

	public String getName() {
		return name;
	}

	public List<String> getRoutes() {
		return routes;
	}

	public List<OverviewDroplet> getDroplets() {
		return droplets;
	}
	
	public void addOverviewDroplet(OverviewDroplet overviewDroplet) {
		this.droplets.add(overviewDroplet);
	}
}
