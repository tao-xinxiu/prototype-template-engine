package com.orange.reconfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.Site;
import com.orange.util.SetUtil;

public class SiteComparator {
    // microservices in desiredArchitecture but not in currentArchitecture
    private Set<Microservice> addedMicroservices = new HashSet<>();
    // microservices in currentArchitecture but not in desiredArchitecture
    private Set<Microservice> removedMicroservices = new HashSet<>();
    // microservices different in currentArchitecture and desiredArchitecture
    private Map<Microservice, Microservice> modifiedMicroservices = new HashMap<>();

    public SiteComparator(Site currentArchitecture, Site desiredArchitecture) {
	Set<String> desiredMicroserviceIds = new HashSet<>();
	for (Microservice desiredMicroservice : desiredArchitecture.getMicroservices()) {
	    if (desiredMicroservice.get("guid") == null) {
		Microservice currentMicroservice = SetUtil.getUniqueMicroservice(currentArchitecture.getMicroservices(),
			(String) desiredMicroservice.get("name"), (String) desiredMicroservice.get("version"));
		if (currentMicroservice == null) {
		    addedMicroservices.add(desiredMicroservice);
		} else {
		    desiredMicroservice.set("guid", currentMicroservice.get("guid"));
		    if (!currentMicroservice.equals(desiredMicroservice)) {
			modifiedMicroservices.put(currentMicroservice, desiredMicroservice);
		    }
		    desiredMicroserviceIds.add((String) desiredMicroservice.get("guid"));
		}
	    } else {
		desiredMicroserviceIds.add((String) desiredMicroservice.get("guid"));
		Microservice currentMicroservice = SetUtil.getUniqueMicroservice(currentArchitecture.getMicroservices(),
			(String) desiredMicroservice.get("guid"));
		if (currentMicroservice == null) {
		    throw new IllegalStateException(
			    String.format("Desired microservice guid [%s] is not present in the current architecture.",
				    desiredMicroservice.get("guid")));
		}
		if (!currentMicroservice.equals(desiredMicroservice)) {
		    modifiedMicroservices.put(currentMicroservice, desiredMicroservice);
		}
	    }
	}
	removedMicroservices = SetUtil.search(currentArchitecture.getMicroservices(),
		currentMicroservice -> !desiredMicroserviceIds.contains(currentMicroservice.get("guid")));
    }

    public Set<Microservice> getAddedMicroservices() {
	return addedMicroservices;
    }

    public Set<Microservice> getRemovedMicroservices() {
	return removedMicroservices;
    }

    public Map<Microservice, Microservice> getModifiedMicroservice() {
	return modifiedMicroservices;
    }
}
