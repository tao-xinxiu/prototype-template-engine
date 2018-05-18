package com.orange.model.architecture.cf;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import com.orange.model.architecture.Microservice;

public class CFMicroservice extends Microservice {
    @SuppressWarnings("unchecked")
    public CFMicroservice(Microservice microservice) {
	set("guid", microservice.get("guid"));
	set("name", microservice.get("name") + "_" + microservice.get("version"));
	set("path", microservice.get("path"));
	set("state", CFMicroserviceState.valueOf(microservice.get("state").toString()));
	set("nbProcesses", (int) microservice.get("nbProcesses"));
	set("env", (Map<String, String>) microservice.get("env"));
	set("services", new HashSet<String>((Collection<String>) microservice.get("services")));
	set("routes",
		((Collection<String>) microservice.get("routes")).stream().map(Route::new).collect(Collectors.toSet()));
    }

    public CFMicroservice(Map<String, Object> attributes) {
	super(attributes);
    }
}
