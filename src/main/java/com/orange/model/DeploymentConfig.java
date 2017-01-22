package com.orange.model;

import java.util.HashMap;
import java.util.Map;

public class DeploymentConfig {
    private Map<String, SiteDeploymentConfig> deploymentConfig = new HashMap<>();

    public DeploymentConfig() {
    }

    public void setDeploymentConfig(Map<String, SiteDeploymentConfig> deploymentConfig) {
	this.deploymentConfig = deploymentConfig;
    }

    public SiteDeploymentConfig getSiteDeploymentConfig(String siteName) {
	SiteDeploymentConfig siteDeploymentConfig = deploymentConfig.get(siteName);
	if (siteDeploymentConfig == null) {
	    siteDeploymentConfig = new SiteDeploymentConfig();
	    deploymentConfig.put(siteName, siteDeploymentConfig);
	}
	return siteDeploymentConfig;
    }
}
