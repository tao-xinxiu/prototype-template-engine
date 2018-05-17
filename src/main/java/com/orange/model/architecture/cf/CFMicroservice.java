package com.orange.model.architecture.cf;

import java.util.Map;

import com.orange.model.architecture.Microservice;

public class CFMicroservice extends Microservice {
    // private String guid;
    // private String name;
    // private String path;
    // private CFMicroserviceState state;
    // private int nbProcesses;
    // private Map<String, String> env = new HashMap<>();
    // private Set<Route> routes = new HashSet<>();
    // private Set<String> services = new HashSet<>();

    public CFMicroservice(Microservice microservice) {
	set("guid", microservice.get("guid"));
	set("name", microservice.get("name") + "_" + microservice.get("version"));
	set("path", microservice.get("path"));
	set("state", microservice.get("state")); //TODO asCFState()
	set("nbProcesses", microservice.get("nbProcesses"));
	set("env", microservice.get("env"));
	set("routes", microservice.get("routes"));
	set("services", microservice.get("services"));
    }

    public CFMicroservice(Map<String, Object> attributes) {
	super(attributes);
    }

}
