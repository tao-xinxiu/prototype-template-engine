package com.orange.model.architecture.cf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.orange.model.architecture.Microservice;

public class CFMicroservice extends Microservice {
    @SuppressWarnings("unchecked")
    public CFMicroservice(Microservice microservice) {
	this(microservice.getAttributes());
	if (microservice.getClass() != CFMicroservice.class) {
	    set("name", microservice.get("name") + "_" + microservice.get("version"));
	    set("state", CFMicroserviceState.valueOf(microservice.get("state").toString()));
	    set("routes", parseRoute((Set<String>) microservice.get("routes")));
	}
    }

    public CFMicroservice(Map<String, Object> attributes) {
	super(attributes);
    }

    private Set<Route> parseRoute(Set<String> routesString) {
	return routesString.stream().map(Route::new).collect(Collectors.toSet());
    }

    @Override
    public Microservice deepCopy() {
	return new CFMicroservice(new HashMap<>(attributes));
    }

    @Override
    public void valid() {
	requiredKeys.addAll(Arrays.asList("env", "routes", "services", "memory", "disk"));
	super.valid();
    }
}
