package com.orange.model;

import java.util.List;

public class OverviewApp {
	private String name;
	private String version;
	private AppState state; //TODO version & state may be specified to each droplet
	private List<String> routes;

	public OverviewApp(String name, String version, AppState state, List<String> routes) {
		this.name = name;
		this.version = version;
		this.state = state;
		this.routes = routes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public AppState getState() {
		return state;
	}

	public void setState(AppState state) {
		this.state = state;
	}

	public List<String> getRoutes() {
		return routes;
	}

	public void setRoutes(List<String> routes) {
		this.routes = routes;
	}
}
