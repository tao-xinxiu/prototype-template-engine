package com.orange.model.architecture;

import java.util.HashMap;
import java.util.Map;

public class Microservice {
    private Map<String, Object> attributes = new HashMap<>();
    // private String guid;
    // private String name;
    // // disallow "_" in version, delimiter between name and version in PaaS
    // // mapping as site unique micro-service name.
    // private String version;
    // private String path;
    // private MicroserviceState state;
    // private int nbProcesses;
    // private Map<String, String> env = new HashMap<>();
    // private Set<Route> routes = new HashSet<>();
    // private Set<String> services = new HashSet<>();
    // private String memory;
    // private String disk;

    public Microservice() {
    }

    public Microservice(Map<String, Object> attributes) {
	this.attributes = attributes;
    }

    public Microservice(Microservice other) {
	attributes = new HashMap<>(other.attributes);
    }

    public Map<String, Object> getAttributes() {
	return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
	this.attributes = attributes;
    }

    public Object get(String key) {
	return attributes.get(key);
    }

    public void set(String key, Object value) {
	attributes.put(key, value);
    }

    // public Set<String> getRoutes() {
    // return routes.stream().map(Route::toString).collect(Collectors.toSet());
    // }
    //
    // public void setRoutes(Set<String> routes) {
    // this.routes =
    // routes.stream().map(Route::new).collect(Collectors.toSet());
    // }
    //
    // public Set<Route> listRoutes() {
    // return routes;
    // }

    /**
     * return whether "this" is an instantiation of desiredMicroservice.
     * 
     * @param desiredMicroservice
     * @return
     */
    public boolean isInstantiation(Microservice desiredMicroservice) {
	if (desiredMicroservice == null) {
	    return false;
	}
	if (desiredMicroservice.get("guid") != null && !desiredMicroservice.get("guid").equals(get("guid"))) {
	    return false;
	}
	if (desiredMicroservice.get("version") != null && !desiredMicroservice.get("version").equals(get("version"))) {
	    return false;
	}
	if (!get("name").equals(desiredMicroservice.get("name")) || !get("path").equals(desiredMicroservice.get("path"))
		|| !get("state").equals(desiredMicroservice.get("state"))
		|| get("nbProcesses") != desiredMicroservice.get("nbProcesses")
		|| !get("env").equals(desiredMicroservice.get("env"))
		|| !get("routes").equals(desiredMicroservice.get("routes"))
		|| !get("memory").equals(desiredMicroservice.get("memory"))
		|| !get("disk").equals(desiredMicroservice.get("disk"))) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "Microservice [attributes=" + attributes + "]";
    }
}
