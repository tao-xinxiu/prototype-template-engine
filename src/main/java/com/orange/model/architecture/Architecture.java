package com.orange.model.architecture;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map.Entry;

import com.orange.model.PaaSSiteAccess;

public class Architecture {
    // private Map<String, PaaSSiteAccess> sites = new HashMap<>();
    private Map<String, Site> sites = new HashMap<>();

    public Architecture(Map<String, Site> sites) {
	this.sites = sites;
    }

    public Architecture() {
    }

    public Architecture(Architecture other) {
	for (Entry<String, Site> entry : other.sites.entrySet()) {
	    this.sites.put(entry.getKey(), new Site(entry.getValue()));
	}
    }

    public void addSite(String siteName, Site site) {
	sites.put(siteName, site);
    }

    public void addSite(PaaSSiteAccess siteAccess, Set<Microservice> microservices) {
	assert siteAccess != null && microservices != null;
	sites.put(siteAccess.getName(), new Site(siteAccess, microservices));
    }

    public Site getSite(String siteName) {
	return sites.get(siteName);
    }

    public Map<String, Site> getSites() {
	return sites;
    }

    public void setSites(Map<String, Site> sites) {
	this.sites = sites;
    }

    public Set<Microservice> getSiteMicroservices(String siteName) {
	return getSite(siteName).getMicroservices();
    }

    public Architecture getSubArchitecture(Set<String> siteNames) {
	Architecture subArchitecture = new Architecture();
	for (String siteName : siteNames) {
	    subArchitecture.addSite(siteName, sites.get(siteName));
	}
	return subArchitecture;
    }

    public void mergeArchitecture(Architecture subArhictecture) {
	sites.putAll(subArhictecture.sites);
    }

    public Set<String> listSitesName() {
	return sites.keySet();
    }

    public Set<PaaSSiteAccess> listPaaSSites() {
	return sites.values().stream().map(s -> s.getSiteAccess()).collect(Collectors.toSet());
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
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
	Architecture other = (Architecture) obj;
	if (sites == null) {
	    if (other.sites != null)
		return false;
	} else if (!sites.equals(other.sites))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	return "Architecture [sites=" + sites + "]";
    }

    /**
     * return whether "this" is an instantiation of finalArchitecture.
     * 
     * @param finalArchitecture
     * @return
     */
    public boolean isInstantiation(Architecture finalArchitecture) {
	if (finalArchitecture == null) {
	    return false;
	}
	for (String site : listSitesName()) {
	    Set<Microservice> desiredMicroservices = finalArchitecture.getSiteMicroservices(site);
	    Set<Microservice> microservices = getSiteMicroservices(site);
	    if (microservices.size() != desiredMicroservices.size()) {
		return false;
	    }
	    for (Microservice desiredMicroservice : desiredMicroservices) {
		if (microservices.stream().noneMatch(ms -> ms.isInstantiation(desiredMicroservice))) {
		    return false;
		}
	    }
	}
	return true;
    }

    public void valid() {
	listSitesName().forEach(site -> getSiteMicroservices(site).forEach(Microservice::valid));
    }
}
