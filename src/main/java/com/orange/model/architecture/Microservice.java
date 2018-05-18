package com.orange.model.architecture;

import java.util.HashMap;
import java.util.Map;

public class Microservice {
    private Map<String, Object> attributes = new HashMap<>();

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

    public void setAttributes(Map<String, Object> attributes) {
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
	if (!get("name").equals(desiredMicroservice.get("name")) || !get("path").equals(desiredMicroservice.get("path"))
		|| !get("state").equals(desiredMicroservice.get("state"))
		|| get("nbProcesses") != desiredMicroservice.get("nbProcesses")
		|| !get("env").equals(desiredMicroservice.get("env"))
		|| !get("routes").equals(desiredMicroservice.get("routes"))
		|| !get("memory").equals(desiredMicroservice.get("memory"))
		|| !get("disk").equals(desiredMicroservice.get("disk"))) {
	    return false;
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
