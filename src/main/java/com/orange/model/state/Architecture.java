package com.orange.model.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.orange.model.PaaSSite;

public class Architecture {
    private Map<String, PaaSSite> sites = new HashMap<>();
    private Map<String, ArchitectureSite> architectureSites = new HashMap<>();

    public Architecture(Map<String, PaaSSite> sites, Map<String, ArchitectureSite> architectureSites) {
	this.sites = sites;
	this.architectureSites = architectureSites;
	assert valid();
    }

    public Architecture() {
    }

    public Architecture(Architecture other) {
	for (Entry<String, PaaSSite> entry : other.sites.entrySet()) {
	    this.sites.put(entry.getKey(), new PaaSSite(entry.getValue()));
	}
	for (Entry<String, ArchitectureSite> entry : other.architectureSites.entrySet()) {
	    this.architectureSites.put(entry.getKey(), new ArchitectureSite(entry.getValue()));
	}
    }

    public void addPaaSSite(PaaSSite site, ArchitectureSite architectureSite) {
	assert site != null && architectureSite != null;
	sites.put(site.getName(), site);
	architectureSites.put(site.getName(), architectureSite);
    }

    public Map<String, PaaSSite> getSites() {
	return sites;
    }

    public void setSites(Map<String, PaaSSite> sites) {
	this.sites = sites;
    }

    public Map<String, ArchitectureSite> getArchitectureSites() {
        return architectureSites;
    }

    public void setArchitectureSites(Map<String, ArchitectureSite> architectureSites) {
        this.architectureSites = architectureSites;
    }

    public ArchitectureSite getArchitectureSite(String siteName) {
	return architectureSites.get(siteName);
    }

    public Architecture getSubArchitecture(Set<String> siteNames) {
	Architecture subArchitecture = new Architecture();
	for (String siteName : siteNames) {
	    subArchitecture.addPaaSSite(sites.get(siteName), architectureSites.get(siteName));
	}
	return subArchitecture;
    }

    public void mergeArchitecture(Architecture subArhictecture) {
	architectureSites.putAll(subArhictecture.architectureSites);
    }

    public Set<String> listSitesName() {
	return sites.keySet();
    }

    public Set<PaaSSite> listPaaSSites() {
	return new HashSet<>(sites.values());
    }

    public boolean valid() {
	if (sites == null) {
	    return architectureSites == null;
	} else if (sites.keySet() == null) {
	    return architectureSites.keySet() == null;
	} else {
	    return sites.keySet().equals(architectureSites.keySet());
	}
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((architectureSites == null) ? 0 : architectureSites.hashCode());
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
	if (architectureSites == null) {
	    if (other.architectureSites != null)
		return false;
	} else if (!architectureSites.equals(other.architectureSites))
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
    public boolean isInstantiation(Architecture desiredState) {
	if (desiredState == null) {
	    return false;
	}
	if (!this.sites.equals(desiredState.sites)) {
	    return false;
	}
	for (String site : this.sites.keySet()) {
	    Set<ArchitectureMicroservice> desiredMicroservices = desiredState.getArchitectureSite(site)
		    .getArchitectureMicroservices();
	    Set<ArchitectureMicroservice> microservices = this.getArchitectureSite(site).getArchitectureMicroservices();
	    if (microservices.size() != desiredMicroservices.size()) {
		return false;
	    }
	    for (ArchitectureMicroservice desiredMicroservice : desiredMicroservices) {
		if (microservices.stream().noneMatch(ms -> ms.isInstantiation(desiredMicroservice))) {
		    return false;
		}
	    }
	}
	return true;
    }

    @Override
    public String toString() {
	return "Architecture [sites=" + sites + ", architectureSites=" + architectureSites + "]";
    }
}
