package com.orange.model.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ArchitectureMicroservice {
    private String guid;
    private String name;
    // disallow "_" in version, delimiter between name and version in PaaS
    // mapping as site unique micro-service name.
    private String version;
    private String path;
    private MicroserviceState state;
    private int nbProcesses;
    private Map<String, String> env = new HashMap<>();
    private Set<Route> routes = new HashSet<>();
    private Set<String> services = new HashSet<>();
    private String memory;
    private String disk;

    public ArchitectureMicroservice() {
    }

    public ArchitectureMicroservice(String guid, String name, String version, String path, MicroserviceState state,
	    int nbProcesses, Map<String, String> env, Set<Route> routes, Set<String> services, String memory,
	    String disk) {
	this.guid = guid;
	this.name = name;
	this.version = version;
	this.path = path;
	this.state = state;
	this.nbProcesses = nbProcesses;
	this.env = env;
	this.routes = routes;
	this.services = services;
	this.memory = memory;
	this.disk = disk;
    }

    public ArchitectureMicroservice(ArchitectureMicroservice other) {
	guid = other.guid;
	name = other.name;
	version = other.version;
	path = other.path;
	state = other.state;
	nbProcesses = other.nbProcesses;
	env = new HashMap<>(other.env);
	routes = new HashSet<>(other.routes);
	services = new HashSet<>(other.services);
	memory = other.memory;
	disk = other.disk;
    }

    public String getGuid() {
	return guid;
    }

    public void setGuid(String guid) {
	this.guid = guid;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getVersion() {
	return version;
    }

    public void setVersion(String version) {
	this.version = version;
    }

    public Set<String> getRoutes() {
	return routes.stream().map(Route::toString).collect(Collectors.toSet());
    }

    public void setRoutes(Set<String> routes) {
	this.routes = routes.stream().map(Route::new).collect(Collectors.toSet());
    }

    public Set<Route> listRoutes() {
	return routes;
    }

    public String getPath() {
	return path;
    }

    public void setPath(String path) {
	this.path = path;
    }

    public MicroserviceState getState() {
	return state;
    }

    public void setState(MicroserviceState state) {
	this.state = state;
    }

    public int getNbProcesses() {
	return nbProcesses;
    }

    public void setNbProcesses(int nbProcesses) {
	this.nbProcesses = nbProcesses;
    }

    public Map<String, String> getEnv() {
	return env;
    }

    public void setEnv(Map<String, String> env) {
	this.env = env;
    }

    public Set<String> getServices() {
	return services;
    }

    public void setServices(Set<String> services) {
	this.services = services;
    }

    public String getMemory() {
	return memory;
    }

    public void setMemory(String memory) {
	this.memory = memory;
    }

    public String getDisk() {
	return disk;
    }

    public void setDisk(String disk) {
	this.disk = disk;
    }

    @Override
    public String toString() {
	return "ArchitectureMicroservice [guid=" + guid + ", name=" + name + ", version=" + version + ", path=" + path
		+ ", state=" + state + ", nbProcesses=" + nbProcesses + ", env=" + env + ", routes=" + routes
		+ ", services=" + services + ", memory=" + memory + ", disk=" + disk + "]";
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((disk == null) ? 0 : disk.hashCode());
	result = prime * result + ((env == null) ? 0 : env.hashCode());
	result = prime * result + ((guid == null) ? 0 : guid.hashCode());
	result = prime * result + ((memory == null) ? 0 : memory.hashCode());
	result = prime * result + ((name == null) ? 0 : name.hashCode());
	result = prime * result + nbProcesses;
	result = prime * result + ((path == null) ? 0 : path.hashCode());
	result = prime * result + ((routes == null) ? 0 : routes.hashCode());
	result = prime * result + ((services == null) ? 0 : services.hashCode());
	result = prime * result + ((state == null) ? 0 : state.hashCode());
	result = prime * result + ((version == null) ? 0 : version.hashCode());
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
	ArchitectureMicroservice other = (ArchitectureMicroservice) obj;
	if (disk == null) {
	    if (other.disk != null)
		return false;
	} else if (!disk.equals(other.disk))
	    return false;
	if (env == null) {
	    if (other.env != null)
		return false;
	} else if (!env.equals(other.env))
	    return false;
	if (guid == null) {
	    if (other.guid != null)
		return false;
	} else if (!guid.equals(other.guid))
	    return false;
	if (memory == null) {
	    if (other.memory != null)
		return false;
	} else if (!memory.equals(other.memory))
	    return false;
	if (name == null) {
	    if (other.name != null)
		return false;
	} else if (!name.equals(other.name))
	    return false;
	if (nbProcesses != other.nbProcesses)
	    return false;
	if (path == null) {
	    if (other.path != null)
		return false;
	} else if (!path.equals(other.path))
	    return false;
	if (routes == null) {
	    if (other.routes != null)
		return false;
	} else if (!routes.equals(other.routes))
	    return false;
	if (services == null) {
	    if (other.services != null)
		return false;
	} else if (!services.equals(other.services))
	    return false;
	if (state != other.state)
	    return false;
	if (version == null) {
	    if (other.version != null)
		return false;
	} else if (!version.equals(other.version))
	    return false;
	return true;
    }

    /**
     * return whether "this" is an instantiated microservice of
     * "desiredMicroservice".
     * 
     * @param desiredMicroservice
     * @return
     */
    public boolean isInstantiation(ArchitectureMicroservice desiredMicroservice) {
	if (desiredMicroservice == null) {
	    return false;
	}
	if (desiredMicroservice.guid != null && !desiredMicroservice.guid.equals(this.guid)) {
	    return false;
	}
	if (desiredMicroservice.version != null && !desiredMicroservice.version.equals(this.version)) {
	    return false;
	}
	if (!this.name.equals(desiredMicroservice.name) || !this.path.equals(desiredMicroservice.path)
		|| !this.state.equals(desiredMicroservice.state) || this.nbProcesses != desiredMicroservice.nbProcesses
		|| !this.env.equals(desiredMicroservice.env) || !this.routes.equals(desiredMicroservice.routes)
		|| !this.memory.equals(desiredMicroservice.memory) || !this.disk.equals(desiredMicroservice.disk)) {
	    return false;
	}
	return true;
    }
}
