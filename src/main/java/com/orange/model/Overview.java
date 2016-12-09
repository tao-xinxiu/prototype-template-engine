package com.orange.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Overview {
	private Map<String, PaaSSite> sites = new HashMap<>();
	private Map<String, OverviewSite> overviewSites = new HashMap<>();

	public Overview(Map<String, PaaSSite> sites, Map<String, OverviewSite> overviewSites) {
		this.sites = sites;
		this.overviewSites = overviewSites;
		assert valid();
	}

	public Overview() {
	}

	public void addPaaSSite(PaaSSite site, OverviewSite overviewApps) {
		assert site != null && overviewApps != null;
		sites.put(site.getName(), site);
		overviewSites.put(site.getName(), overviewApps);
	}

	public Map<String, PaaSSite> getSites() {
		return sites;
	}

	public void setSites(Map<String, PaaSSite> sites) {
		this.sites = sites;
	}

	public Map<String, OverviewSite> getOverviewSites() {
		return overviewSites;
	}

	public void setOverviewSites(Map<String, OverviewSite> overviewSites) {
		this.overviewSites = overviewSites;
	}

	public OverviewSite getOverviewSite(String siteName) {
		return overviewSites.get(siteName);
	}
	
	public Set<String> listSitesName() {
		return sites.keySet();
	}

	public Set<PaaSSite> listPaaSSites() {
		return new HashSet<>(sites.values());
	}

	public boolean valid() {
		if (sites == null) {
			return overviewSites == null;
		} else if (sites.keySet() == null) {
			return overviewSites.keySet() == null;
		} else {
			return sites.keySet().equals(overviewSites.keySet());
		}
	}
}
