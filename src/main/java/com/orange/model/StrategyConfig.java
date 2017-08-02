package com.orange.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StrategyConfig {
    private int canaryNbr = 1;
    private int canaryIncrease = 1;
    private Map<String, StrategySiteConfig> siteConfigs = new HashMap<>();

    public StrategyConfig() {
    }

    public void setSiteConfig(String site, StrategySiteConfig siteConfig) {
	siteConfigs.put(site, siteConfig);
    }

    public StrategySiteConfig getSiteConfig(String siteName) {
	return siteConfigs.get(siteName);
    }

    public Set<String> getSites() {
	return siteConfigs.keySet();
    }

    public Map<String, StrategySiteConfig> getSiteConfigs() {
	return siteConfigs;
    }

    public void setSiteConfigs(Map<String, StrategySiteConfig> siteConfigs) {
	this.siteConfigs = siteConfigs;
    }

    public int getCanaryNbr() {
	return canaryNbr;
    }

    public void setCanaryNbr(int canaryNbr) {
	this.canaryNbr = canaryNbr;
    }

    public int getCanaryIncrease() {
	return canaryIncrease;
    }

    public void setCanaryIncrease(int canaryIncrease) {
	this.canaryIncrease = canaryIncrease;
    }

    @Override
    public String toString() {
	return "StrategyConfig [canaryNbr=" + canaryNbr + ", canaryIncrease=" + canaryIncrease + ", siteConfigs="
		+ siteConfigs + "]";
    }
}
