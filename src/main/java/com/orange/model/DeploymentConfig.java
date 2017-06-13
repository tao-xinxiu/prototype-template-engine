package com.orange.model;

import java.util.HashMap;
import java.util.Map;

public class DeploymentConfig {
    private Map<String, SiteDeploymentConfig> deploymentConfig = new HashMap<>();

    public void setDeploymentConfig(String site, SiteDeploymentConfig siteConfig) {
	deploymentConfig.put(site, siteConfig);
    }

    public SiteDeploymentConfig getSiteConfig(String siteName) {
	return deploymentConfig.get(siteName);
    }
}
