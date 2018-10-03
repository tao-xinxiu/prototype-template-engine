package com.orange.model.architecture.k8s;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.orange.model.architecture.Microservice;

public class K8sMicroservice extends Microservice {
    public K8sMicroservice(Microservice microservice) {
	super(microservice);
	Map<String, String> labels = new HashMap<>();
	labels.put("version", (String) microservice.get("version"));
	set("labels", labels);
    }

    public K8sMicroservice(Map<String, Object> attributes) {
	super(attributes);
    }

    @Override
    public void valid() {
	keys.addAll(Arrays.asList("labels", "selector", "env", "resources", "runningProcesses"));
	super.valid();
    }
}
