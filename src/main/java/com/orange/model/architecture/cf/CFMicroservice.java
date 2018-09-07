package com.orange.model.architecture.cf;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.architecture.Microservice;

public class CFMicroservice extends Microservice {
    @SuppressWarnings("unchecked")
    public CFMicroservice(Microservice microservice) {
	super(microservice);
	set("name", microservice.get("name") + "_" + microservice.get("version"));
	set("state", CFMicroserviceState.valueOf(microservice.get("state").toString()));
	if (microservice.get("routes") == null) {
	    set("routes", new HashSet<>());
	}
	set("routes", ((Set<String>) microservice.get("routes")).stream().map(Route::new).collect(Collectors.toSet()));
	if (microservice.get("services") == null) {
	    set("services", new HashSet<>());
	}
    }

    public CFMicroservice(Map<String, Object> attributes) {
	super(attributes);
    }
}
