package com.orange.model.architecture.cf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.Route;

public class CFMicroservice {
    private String guid;
    private String name;
    private String path;
    private CFMicroserviceState state;
    private int nbProcesses;
    private Map<String, String> env = new HashMap<>();
    private Set<Route> routes = new HashSet<>();
    private Set<String> services = new HashSet<>();

    public CFMicroservice(String guid, String name, String path, CFMicroserviceState state, int nbProcesses,
	    Map<String, String> env, Set<Route> routes, Set<String> services) {
	super();
	this.guid = guid;
	this.name = name;
	this.path = path;
	this.state = state;
	this.nbProcesses = nbProcesses;
	this.env = env;
	this.routes = routes;
	this.services = services;
    }

    public CFMicroservice(Microservice microservice) {
	this.guid = microservice.getGuid();
	this.name = microservice.getName() + "_" + microservice.getVersion();
	this.path = microservice.getPath();
	this.state = microservice.getState().asCFState();
	this.nbProcesses = microservice.getNbProcesses();
	this.env = microservice.getEnv();
	this.routes = microservice.listRoutes();
	this.services = microservice.getServices();
    }

    public String getGuid() {
	return guid;
    }

    public String getName() {
	return name;
    }

    public String getPath() {
	return path;
    }

    public CFMicroserviceState getState() {
	return state;
    }

    public int getNbProcesses() {
	return nbProcesses;
    }

    public Map<String, String> getEnv() {
	return env;
    }

    public Set<Route> getRoutes() {
	return routes;
    }

    public void setGuid(String guid) {
	this.guid = guid;
    }

    public void setName(String name) {
	this.name = name;
    }

    public void setPath(String path) {
	this.path = path;
    }

    public void setState(CFMicroserviceState state) {
	this.state = state;
    }

    public void setNbProcesses(int nbProcesses) {
	this.nbProcesses = nbProcesses;
    }

    public void setEnv(Map<String, String> env) {
	this.env = env;
    }

    public void setRoutes(Set<Route> routes) {
	this.routes = routes;
    }

    public Set<String> getServices() {
	return services;
    }

    public void setServices(Set<String> services) {
	this.services = services;
    }

    @Override
    public String toString() {
	return "CFMicroserviceArchitecture [guid=" + guid + ", name=" + name + ", path=" + path + ", state=" + state
		+ ", nbProcesses=" + nbProcesses + ", env=" + env + ", routes=" + routes + ", services=" + services
		+ "]";
    }
}
