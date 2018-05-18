package com.orange.model.architecture;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Microservice {
    protected Map<String, Object> attributes = new HashMap<>();

    public Microservice() {
    }

    public Microservice(Map<String, Object> attributes) {
	this.attributes = attributes;
    }

    public Microservice(Microservice other) {
	attributes = new HashMap<>(other.attributes);
    }

    public Map<String, Object> getAttributes() {
	return attributes;
    }

    @SuppressWarnings("unchecked")
    public void setAttributes(Map<String, Object> attributes) {
	for (Entry<String, Object> attribute : attributes.entrySet()) {
	    if (attribute.getValue() instanceof Collection<?>) {
		attribute.setValue(new HashSet<String>((Collection<String>) attribute.getValue()));
	    }
	    if (attribute.getKey().equals("state")) {
		attribute.setValue(MicroserviceState.valueOf(attribute.getValue().toString()));
	    }
	}
	this.attributes = attributes;
    }

    public Object get(String key) {
	return attributes.get(key);
    }

    public void set(String key, Object value) {
	attributes.put(key, value);
    }

    /**
     * return whether "this" is an instantiation of desiredMicroservice.
     * 
     * @param desiredMicroservice
     * @return
     */
    public boolean isInstantiation(Microservice desiredMicroservice) {
	if (desiredMicroservice == null) {
	    return false;
	}
	if (desiredMicroservice.get("guid") != null && !desiredMicroservice.get("guid").equals(get("guid"))) {
	    return false;
	}
	if (desiredMicroservice.get("version") != null && !desiredMicroservice.get("version").equals(get("version"))) {
	    return false;
	}
	List<String> compareKeys = Arrays.asList("name", "path", "state", "nbProcesses", "env", "routes", "services",
		"memory", "disk");
	for (String key : compareKeys) {
	    if (!attributes.get(key).equals(desiredMicroservice.get(key))) {
		return false;
	    }
	}
	return true;
    }

    @Override
    public String toString() {
	return "Microservice [attributes=" + attributes + "]";
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Microservice other = (Microservice) obj;
	if (attributes == null) {
	    if (other.attributes != null)
		return false;
	} else if (!attributes.equals(other.attributes))
	    return false;
	return true;
    }
}
