package com.orange.model.architecture;

import java.util.HashSet;
import java.util.Set;

public class ArchitectureSite {
    private Set<ArchitectureMicroservice> architectureMicroservices = new HashSet<>();
    // TODO add architectureServices

    public ArchitectureSite() {
    }

    public ArchitectureSite(Set<ArchitectureMicroservice> architectureMicroservices) {
	this.architectureMicroservices = architectureMicroservices;
    }

    public ArchitectureSite(ArchitectureSite other) {
	for (ArchitectureMicroservice architectureMicroservice : other.architectureMicroservices) {
	    this.architectureMicroservices.add(new ArchitectureMicroservice(architectureMicroservice));
	}
    }

    public Set<ArchitectureMicroservice> getArchitectureMicroservices() {
	return architectureMicroservices;
    }

    public void setArchitectureMicroservices(Set<ArchitectureMicroservice> architectureMicroservices) {
	this.architectureMicroservices = architectureMicroservices;
    }

    public void addArchitectureMicroservice(ArchitectureMicroservice architectureMicroservice) {
	this.architectureMicroservices.add(architectureMicroservice);
    }

    public void addArchitectureMicoservices(Set<ArchitectureMicroservice> architectureMicroservices) {
	this.architectureMicroservices.addAll(architectureMicroservices);
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((architectureMicroservices == null) ? 0 : architectureMicroservices.hashCode());
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
	if (architectureMicroservices == null) {
	    if (other.architectureMicroservices != null)
		return false;
	} else if (!architectureMicroservices.equals(other.architectureMicroservices))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	return "ArchitectureSite [architectureMicroservices=" + architectureMicroservices + "]";
    }
}
