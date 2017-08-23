package com.orange.model.state.cf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.orange.model.state.OverviewApp;
import com.orange.model.state.Route;

public class CFOverviewApp {
    private String guid;
    private String name;
    private String path;
    private CFAppState state;
    private int nbProcesses;
    private Map<String, String> env = new HashMap<>();
    private Set<Route> routes = new HashSet<>();
    private Set<String> services = new HashSet<>();

    public CFOverviewApp(String guid, String name, String path, CFAppState state, int nbProcesses,
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

    public CFOverviewApp(OverviewApp app) {
	this.guid = app.getGuid();
	this.name = app.getName() + "_" + app.getVersion();
	this.path = app.getPath();
	this.state = app.getState().asCFState();
	this.nbProcesses = app.getNbProcesses();
	this.env = app.getEnv();
	this.routes = app.listRoutes();
	this.services = app.getServices();
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

    public CFAppState getState() {
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

    public void setPath(String path) {
	this.path = path;
    }

    public void setState(CFAppState state) {
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
	return "CFOverviewApp [guid=" + guid + ", name=" + name + ", path=" + path + ", state=" + state
		+ ", nbProcesses=" + nbProcesses + ", env=" + env + ", routes=" + routes + ", services=" + services
		+ "]";
    }
}
