package com.orange.model.architecture.cf;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.architecture.Microservice;

public class CFMicroservice extends Microservice {

    @SuppressWarnings("unchecked")
    public CFMicroservice(Microservice microservice) {
	set("guid", microservice.get("guid"));
	set("name", microservice.get("name") + "_" + microservice.get("version"));
	set("path", microservice.get("path"));
	set("state", CFMicroserviceState.valueOf(microservice.get("state").toString()));
	set("nbProcesses", microservice.get("nbProcesses"));
	set("env", microservice.get("env"));
	set("routes", ((Set<String>) microservice.get("routes")).stream().map(Route::new).collect(Collectors.toSet()));
	set("services", microservice.get("services"));
    }

    public CFMicroservice(Map<String, Object> attributes) {
	super(attributes);
    }

}
