package com.orange.model.architecture.cf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.orange.model.architecture.Microservice;

public class CFMicroservice extends Microservice {
    public CFMicroservice(Microservice microservice) {
	this(microservice.getAttributes());
	if (microservice.getClass() != CFMicroservice.class) {
	    set("state", CFMicroserviceState.valueOf(microservice.get("state").toString()));
	}
    }

    public CFMicroservice(Map<String, Object> attributes) {
	super(attributes);
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
