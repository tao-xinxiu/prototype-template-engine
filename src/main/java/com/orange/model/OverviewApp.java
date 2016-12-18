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
	private AppState state;
	private int instances;
	private Map<String, String> env = new HashMap<>();
	private List<Route> routes = new ArrayList<>();

	public OverviewApp() {
	}

	public OverviewApp(String guid, String name, String path, AppState state, int instances, Map<String, String> env,
			List<Route> routes) {
		this.guid = guid;
		this.name = name;
		this.path = path;
		this.state = state;
		this.instances = instances;
		this.env = env;
		this.routes = routes;
	}

	public OverviewApp(OverviewApp other) {
		guid = other.guid;
		name = other.name;
		path = other.path;
		state = other.state;
		instances = other.instances;
		env = new HashMap<>(other.env);
		routes = new ArrayList<>(other.routes);
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

	public AppState getState() {
		return state;
	}

	public void setState(AppState state) {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((env == null) ? 0 : env.hashCode());
		result = prime * result + ((guid == null) ? 0 : guid.hashCode());
		result = prime * result + instances;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((routes == null) ? 0 : routes.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OverviewApp other = (OverviewApp) obj;
		if (env == null) {
			if (other.env != null)
				return false;
		} else if (!env.equals(other.env))
			return false;
		if (guid == null) {
			if (other.guid != null)
				return false;
		} else if (!guid.equals(other.guid))
			return false;
		if (instances != other.instances)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (routes == null) {
			if (other.routes != null)
				return false;
		} else if (!routes.equals(other.routes))
			return false;
		if (state != other.state)
			return false;
		return true;
	}

	/**
	 * return whether "this" is an instantiated app of "desiredApp".
	 * 
	 * @param desiredApp
	 * @return
	 */
	public boolean isInstantiation(OverviewApp desiredApp) {
		if (desiredApp == null) {
			return false;
		}
		if (this.guid == null || this.path != null) {
			return false;
		}
		if (desiredApp.guid != null && this.guid != desiredApp.guid) {
			return false;
		}
		if (!this.name.equals(desiredApp.name) || !this.state.equals(desiredApp.state)
				|| this.instances != desiredApp.instances || !this.env.equals(desiredApp.env)
				|| !this.routes.equals(desiredApp.routes)) {
			return false;
		}
		return true;
	}
}
