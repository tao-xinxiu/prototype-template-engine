package com.orange.model;

import java.util.Map;

public class DeploymentConfig {
	private Map<String, PaaSTarget> targets;
	private Application app;

	public Map<String, PaaSTarget> getTargets() {
		return targets;
	}

	public void setTargets(Map<String, PaaSTarget> targets) {
		this.targets = targets;
	}

	public Application getApp() {
		return app;
	}

	public void setApp(Application app) {
		this.app = app;
	}

	
}
