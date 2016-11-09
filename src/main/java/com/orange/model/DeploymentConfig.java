package com.orange.model;

import java.util.Map;

public class DeploymentConfig {
	private Map<String, PaaSSite> sites;
	private Application app;

	public Map<String, PaaSSite> getSites() {
		return sites;
	}

	public void setSites(Map<String, PaaSSite> sites) {
		this.sites = sites;
	}

	public Application getApp() {
		return app;
	}

	public void setApp(Application app) {
		this.app = app;
	}

	
}
