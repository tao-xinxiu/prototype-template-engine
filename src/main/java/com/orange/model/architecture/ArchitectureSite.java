package com.orange.model.architecture;

import java.util.HashSet;
import java.util.Set;

public class ArchitectureSite {
    private Set<Microservice> microservices = new HashSet<>();
    // TODO add architectureServices for managing external services

    public ArchitectureSite() {
    }

    public ArchitectureSite(Set<Microservice> microservices) {
	this.microservices = microservices;
    }

    public ArchitectureSite(ArchitectureSite other) {
	for (Microservice microservice : other.microservices) {
	    this.microservices.add(new Microservice(microservice));
	}
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
	ArchitectureSite other = (ArchitectureSite) obj;
	if (microservices == null) {
	    if (other.microservices != null)
		return false;
	} else if (!microservices.equals(other.microservices))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	return "ArchitectureSite [microservices=" + microservices + "]";
    }

}
