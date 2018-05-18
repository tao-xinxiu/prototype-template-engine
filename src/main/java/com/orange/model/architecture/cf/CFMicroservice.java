package com.orange.model.architecture.cf;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.architecture.Microservice;

public class CFMicroservice extends Microservice {
    @SuppressWarnings("unchecked")
    public CFMicroservice(Microservice microservice) {
	set("name", microservice.get("name") + "_" + microservice.get("version"));
	set("state", CFMicroserviceState.valueOf(microservice.get("state").toString()));
	set("routes", ((Set<String>) microservice.get("routes")).stream().map(Route::new).collect(Collectors.toSet()));
    }

    public CFMicroservice(Map<String, Object> attributes) {
	super(attributes);
    }
}
