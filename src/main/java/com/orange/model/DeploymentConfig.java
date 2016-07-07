package com.orange.model;

import java.util.Map;

public class DeploymentConfig {
	private Map<String, CloudFoundryTarget> targets;
	private Map<String, Application> apps;

	public Map<String, CloudFoundryTarget> getTargets() {
		return targets;
	}

	public void setTargets(Map<String, CloudFoundryTarget> targets) {
		this.targets = targets;
	}

	public Map<String, Application> getApps() {
		return apps;
	}

	public void setApps(Map<String, Application> apps) {
		this.apps = apps;
	}
}
