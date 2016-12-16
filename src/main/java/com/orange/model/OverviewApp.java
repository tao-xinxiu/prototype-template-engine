package com.orange.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OverviewApp {
	private String guid;
	private String name;
	private String path;
	private DropletState state;
	private int instances;
	private Map<String, String> env = new HashMap<>();
	private List<Route> routes = new ArrayList<>();

	public OverviewApp() {
	}

	public OverviewApp(String guid, String name, String path, DropletState state, int instances,
			Map<String, String> env, List<Route> routes) {
		this.guid = guid;
		this.name = name;
		this.path = path;
		this.state = state;
		this.instances = instances;
		this.env = env;
		this.routes = routes;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getRoutes() {
		return routes.stream().map(Route::toString).collect(Collectors.toList());
	}

	public void setRoutes(List<String> routes) {
		this.routes = routes.stream().map(Route::new).collect(Collectors.toList());
	}

	public List<Route> listRoutes() {
		return routes;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public DropletState getState() {
		return state;
	}

	public void setState(DropletState state) {
		this.state = state;
	}

	public int getInstances() {
		return instances;
	}

	public void setInstances(int instances) {
		this.instances = instances;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	@Override
	public String toString() {
		return "OverviewApp [guid=" + guid + ", name=" + name + ", path=" + path + ", state=" + state + ", instances="
				+ instances + ", env=" + env + ", routes=" + routes + "]";
	}
}
