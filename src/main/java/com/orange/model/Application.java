package com.orange.model;

import java.util.HashMap;
import java.util.Map;

public class Application {
	private String name;
	private String version;
	private String path;
	private Map<String, String> hostnames;
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
		// update app version info in env
		this.env.put("APP_VERSION", version);
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
		// add app version info into env
		this.env.put("APP_VERSION", version);
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
	
	public Map<String, String> getHostnames() {
		return hostnames;
	}

	public void setHostnames(Map<String, String> hostnames) {
		this.hostnames = hostnames;
	}

	@Override
	public String toString() {
		return String.format("{name: %s; version: %s; path: %s; hostnames: %s; env:%s}", name,
				version, path, hostnames, env);
	}

	public boolean valid() {
		if (name != null && version != null && path != null && validHostname()) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean validHostname() {
		if (hostnames == null) {
			return false;
		}
		else if (hostnames.get("local")== null || hostnames.get("global") == null || hostnames.get("tmp") == null) {
			return false;
		}
		else {
			return true;
		}
	}

	// for Jackson map json to object
	public Application() {
	}

	public Application(Application application) {
		this.name = application.name;
		this.version = application.version;
		this.path = application.path;
		this.hostnames = new HashMap<>(application.hostnames);
		this.buildpack = application.buildpack;
		this.stack = application.stack;
		this.env = new HashMap<>(application.env);
	}
}
