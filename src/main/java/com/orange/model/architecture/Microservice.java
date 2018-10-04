package com.orange.model.architecture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Microservice {
    protected Map<String, Object> attributes = new HashMap<>();
    // "guid" is not the required key
    protected final Set<String> requiredKeys = new HashSet<>(
	    Arrays.asList("name", "version", "path", "state", "nbProcesses"));

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

    public boolean eqAttr(String key, Microservice other) {
	if (other == null)
	    return false;
	if (attributes == null) {
	    if (other.attributes != null)
		return false;
	} else if (attributes.get(key) == null) {
	    if (other.attributes.get(key) != null) {
		return false;
	    }
	} else if (!attributes.get(key).equals(other.attributes.get(key)))
	    return false;
	return true;
    }

    public boolean eqAttr(List<String> keys, Microservice other) {
	return keys.stream().allMatch(key -> eqAttr(key, other));
    }

    public boolean eqAttrExcept(List<String> excludeKeys, Microservice other) {
	List<String> eqKeys = new ArrayList<>(other.attributes.keySet());
	eqKeys.removeAll(excludeKeys);
	return eqKeys.stream().allMatch(key -> eqAttr(key, other));
    }

    public void copyAttr(String key, Microservice other) {
	attributes.put(key, other.attributes.get(key));
    }

    public void copyAttr(List<String> keys, Microservice other) {
	keys.stream().forEach(key -> copyAttr(key, other));
    }

    public void copyAttrExcept(String excludeKey, Microservice other) {
	List<String> copyKeys = new ArrayList<>(other.attributes.keySet());
	copyKeys.remove(excludeKey);
	copyKeys.stream().forEach(key -> copyAttr(key, other));
    }

    public void copyAttrExcept(List<String> excludeKeys, Microservice other) {
	List<String> copyKeys = new ArrayList<>(other.attributes.keySet());
	copyKeys.removeAll(excludeKeys);
	copyKeys.stream().forEach(key -> copyAttr(key, other));
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
	Set<String> compareKeys = new HashSet<>(desiredMicroservice.attributes.keySet());
	compareKeys.remove("guid");
	for (String key : compareKeys) {
	    if (!attributes.get(key).equals(desiredMicroservice.get(key))) {
		return false;
	    }
	}
	return true;
    }

    public void valid() {
	if (!attributes.keySet().containsAll(requiredKeys)) {
	    Set<String> missingKeys = new HashSet<>(requiredKeys);
	    missingKeys.removeAll(attributes.keySet());
	    throw new IllegalArgumentException(String.format("Missing the attributes [%s] in: %s.", missingKeys, this));
	}
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
