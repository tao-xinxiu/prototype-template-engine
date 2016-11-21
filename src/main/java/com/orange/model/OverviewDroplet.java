package com.orange.model;

public class OverviewDroplet {
	private String guid;
	private String version;
	private DropletState state;

	public OverviewDroplet(String guid, String version, DropletState state) {
		this.guid = guid;
		this.version = version;
		this.state = state;
	}

	public String getGuid() {
		return guid;
	}

	public String getVersion() {
		return version;
	}

	public DropletState getState() {
		return state;
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
