package com.orange.model;

public class Application {
	private String name;
	private String version;
	private String path;
	private String localHostname;
	private String globalHostname;

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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getLocalHostname() {
		return localHostname;
	}

	public void setLocalHostname(String localHostname) {
		this.localHostname = localHostname;
	}

	public String getGlobalHostname() {
		return globalHostname;
	}

	public void setGlobalHostname(String globalHostname) {
		this.globalHostname = globalHostname;
	}

	@Override
	public String toString() {
		return String.format("{name: %s; version: %s; path: %s; local_hostname: %s; global_hostname:%s}", name, version,
				path, localHostname, globalHostname);
	}

	public boolean valid() {
		if (version != null && path != null && localHostname != null && globalHostname != null) {
			return true;
		} else {
			return false;
		}
	}
}
