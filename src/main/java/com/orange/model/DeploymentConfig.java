package com.orange.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeploymentConfig {
	private Map<String, SiteDeploymentConfig> deploymentConfig = new HashMap<>();

	public DeploymentConfig() {
	}

	public DeploymentConfig getDeploymentConfig(Set<String> sites) {
		for (String site : sites) {
			if (deploymentConfig.get(site) == null) {
				deploymentConfig.put(site, new SiteDeploymentConfig());
			}
		}
		return this;
	}

	public void setDeploymentConfig(Map<String, SiteDeploymentConfig> deploymentConfig) {
		this.deploymentConfig = deploymentConfig;
	}
}
