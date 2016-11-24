package com.orange.model;

import java.util.HashMap;
import java.util.Map;

public class OverviewDroplet {
	private String guid;
	private String version;
	private String path;
	private DropletState state;
	private Map<String, String> env = new HashMap<>();

	public OverviewDroplet() {
	}

	public OverviewDroplet(String guid, String version, String path, DropletState state, Map<String, String> env) {
		this.guid = guid;
		this.version = version;
		this.path = path;
		this.state = state;
		this.env = env;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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
		result = prime * result + ((guid == null) ? 0 : guid.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		if (guid == null) {
			if (other.guid != null)
				return false;
		} else if (!guid.equals(other.guid))
			return false;
		if (state != other.state)
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

}
