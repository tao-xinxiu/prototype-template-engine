package com.orange.model;

import java.util.HashMap;
import java.util.Map;

public class OverviewDroplet {
	private String guid;
	private String path;
	private DropletState state;
	private int instances;
	private Map<String, String> env = new HashMap<>();

	public OverviewDroplet() {
	}

	public OverviewDroplet(String guid, String path, DropletState state, int instances, Map<String, String> env) {
		this.guid = guid;
		this.path = path;
		this.state = state;
		this.instances = instances;
		this.env = env;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((env == null) ? 0 : env.hashCode());
		result = prime * result + ((guid == null) ? 0 : guid.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		OverviewDroplet other = (OverviewDroplet) obj;
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
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (state != other.state)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OverviewDroplet [guid=" + guid + ", path=" + path + ", state=" + state + ", env=" + env + "]";
	}
}
