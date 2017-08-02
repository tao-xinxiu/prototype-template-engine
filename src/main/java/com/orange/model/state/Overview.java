package com.orange.model.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.orange.model.PaaSSite;

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

    public Overview(Overview other) {
	for (Entry<String, PaaSSite> entry : other.sites.entrySet()) {
	    this.sites.put(entry.getKey(), new PaaSSite(entry.getValue()));
	}
	for (Entry<String, OverviewSite> entry : other.overviewSites.entrySet()) {
	    this.overviewSites.put(entry.getKey(), new OverviewSite(entry.getValue()));
	}
    }

    public void addPaaSSite(PaaSSite site, OverviewSite overviewSite) {
	assert site != null && overviewSite != null;
	sites.put(site.getName(), site);
	overviewSites.put(site.getName(), overviewSite);
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
    
    public Overview getSubOverview(Set<String> siteNames) {
	Overview subOverview = new Overview();
	for (String siteName : siteNames) {
	    subOverview.addPaaSSite(sites.get(siteName), overviewSites.get(siteName));
	}
	return subOverview;
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

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((overviewSites == null) ? 0 : overviewSites.hashCode());
	result = prime * result + ((sites == null) ? 0 : sites.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Overview other = (Overview) obj;
	if (overviewSites == null) {
	    if (other.overviewSites != null)
		return false;
	} else if (!overviewSites.equals(other.overviewSites))
	    return false;
	if (sites == null) {
	    if (other.sites != null)
		return false;
	} else if (!sites.equals(other.sites))
	    return false;
	return true;
    }

    /**
     * return whether "this" is an instantiated state of "desiredState".
     * 
     * @param desiredState
     * @return
     */
    public boolean isInstantiation(Overview desiredState) {
	if (desiredState == null) {
	    return false;
	}
	if (!this.sites.equals(desiredState.sites)) {
	    return false;
	}
	for (String site : this.sites.keySet()) {
	    Set<OverviewApp> desiredApps = desiredState.getOverviewSite(site).getOverviewApps();
	    Set<OverviewApp> apps = this.getOverviewSite(site).getOverviewApps();
	    if (apps.size() != desiredApps.size()) {
		return false;
	    }
	    for (OverviewApp desiredApp : desiredApps) {
		if (apps.stream().noneMatch(app -> app.isInstantiation(desiredApp))) {
		    return false;
		}
	    }
	}
	return true;
    }

    @Override
    public String toString() {
	return "Overview [sites=" + sites + ", overviewSites=" + overviewSites + "]";
    }
}
