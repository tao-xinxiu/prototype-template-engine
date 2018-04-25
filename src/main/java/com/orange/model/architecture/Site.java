package com.orange.model.architecture;

import java.util.HashSet;
import java.util.Set;

import com.orange.model.PaaSSiteAccess;

public class Site {
    private PaaSSiteAccess siteAccess;
    private Set<Microservice> microservices = new HashSet<>();
    // TODO add architectureServices for managing external services

    public Site() {
    }

    public Site(PaaSSiteAccess siteAccess, Set<Microservice> microservices) {
	super();
	this.siteAccess = siteAccess;
	this.microservices = microservices;
    }

    public Site(Site other) {
	this.siteAccess = new PaaSSiteAccess(other.siteAccess);
	for (Microservice microservice : other.microservices) {
	    this.microservices.add(new Microservice(microservice));
	}
    }

    public PaaSSiteAccess getSiteAccess() {
	return siteAccess;
    }

    public Set<Microservice> getMicroservices() {
	return microservices;
    }

    public void setMicroservices(Set<Microservice> microservices) {
	this.microservices = microservices;
    }

    public void addMicroservice(Microservice microservice) {
	this.microservices.add(microservice);
    }

    public void addMicoservices(Set<Microservice> microservices) {
	this.microservices.addAll(microservices);
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((microservices == null) ? 0 : microservices.hashCode());
	result = prime * result + ((siteAccess == null) ? 0 : siteAccess.hashCode());
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
	Site other = (Site) obj;
	if (microservices == null) {
	    if (other.microservices != null)
		return false;
	} else if (!microservices.equals(other.microservices))
	    return false;
	if (siteAccess == null) {
	    if (other.siteAccess != null)
		return false;
	} else if (!siteAccess.equals(other.siteAccess))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	return "Site [siteAccess=" + siteAccess + ", microservices=" + microservices + "]";
    }
}
