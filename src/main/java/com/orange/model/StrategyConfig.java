package com.orange.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StrategyConfig {
    private boolean parallelAllSites = true;
    private List<Set<String>> sitesOrder = new ArrayList<>();
    private int canaryNbr = 1;
    private int canaryIncrease = 1;
    private Map<String, StrategySiteConfig> siteConfigs = new HashMap<>();
    private String updatingVersion = "UPDATING";

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

    public boolean isParallelAllSites() {
	return parallelAllSites;
    }

    public void setParallelAllSites(boolean parallelAllSites) {
	this.parallelAllSites = parallelAllSites;
    }

    public List<Set<String>> getSitesOrder() {
	return sitesOrder;
    }

    public void setSitesOrder(List<Set<String>> sitesOrder) {
	this.sitesOrder = sitesOrder;
    }

    public String getUpdatingVersion() {
	return updatingVersion;
    }

    public void setUpdatingVersion(String updatingVersion) {
	this.updatingVersion = updatingVersion;
    }

    @Override
    public String toString() {
	return "StrategyConfig [parallelAllSites=" + parallelAllSites + ", sitesOrder=" + sitesOrder + ", canaryNbr="
		+ canaryNbr + ", canaryIncrease=" + canaryIncrease + ", siteConfigs=" + siteConfigs
		+ ", updatingVersion=" + updatingVersion + "]";
    }
}
