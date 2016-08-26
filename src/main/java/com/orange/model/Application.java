package com.orange.model;

import java.util.HashMap;
import java.util.Map;

public class Application {
	private String name;
	private String version;
	private String path;
	private String localHostname;
	private String globalHostname;
	private Map<String, String> env = new HashMap<>(); 
	private String buildpack;
	private String stack;

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
		env.put("APP_VERSION", version);
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

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public String getBuildpack() {
		return buildpack;
	}

	public void setBuildpack(String buildpack) {
		this.buildpack = buildpack;
	}

	public String getStack() {
		return stack;
	}

	public void setStack(String stack) {
		this.stack = stack;
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
