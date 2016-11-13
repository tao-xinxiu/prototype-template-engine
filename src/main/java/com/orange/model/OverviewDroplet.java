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

}
