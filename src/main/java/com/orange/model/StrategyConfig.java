package com.orange.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    public void validSitesOrder(Set<String> completeSites) {
	if (sitesOrder.isEmpty()) {
	    throw new IllegalStateException(
		    "strategyConfig.sitesOrder is not specified for a non-parallel update sites strategy.");
	}
	List<String> sitesInOrder = sitesOrder.stream().flatMap(s -> s.stream()).collect(Collectors.toList());
	if (completeSites.size() != sitesInOrder.size()) {
	    throw new IllegalStateException(
		    "Number of sites in strategyConfig.sitesOrder is not equal to the number of sites specified in the finalArchitecture.");
	}
	if (!completeSites.equals(new HashSet<>(sitesInOrder))) {
	    throw new IllegalStateException(
		    "sites in strategyConfig.sitesOrder is not equal to the sites specified in the finalArchitecture");
	}
    }

    @Override
    public String toString() {
	return "StrategyConfig [parallelAllSites=" + parallelAllSites + ", sitesOrder=" + sitesOrder + ", canaryNbr="
		+ canaryNbr + ", canaryIncrease=" + canaryIncrease + ", siteConfigs=" + siteConfigs
		+ ", updatingVersion=" + updatingVersion + "]";
    }
}
