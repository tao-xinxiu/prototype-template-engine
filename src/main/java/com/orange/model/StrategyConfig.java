package com.orange.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StrategyConfig {
    private Map<String, StrategySiteConfig> deploymentConfig = new HashMap<>();

    public StrategyConfig() {
    }

    public void setSiteConfig(String site, StrategySiteConfig siteConfig) {
	deploymentConfig.put(site, siteConfig);
    }

    public StrategySiteConfig getSiteConfig(String siteName) {
	return deploymentConfig.get(siteName);
    }

    public Set<String> getSites() {
	return deploymentConfig.keySet();
    }

    public Map<String, StrategySiteConfig> getDeploymentConfig() {
        return deploymentConfig;
    }

    public void setDeploymentConfig(Map<String, StrategySiteConfig> deploymentConfig) {
        this.deploymentConfig = deploymentConfig;
    }
}
