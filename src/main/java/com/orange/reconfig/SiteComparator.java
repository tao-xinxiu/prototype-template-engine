package com.orange.reconfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.orange.model.architecture.ArchitectureMicroservice;
import com.orange.model.architecture.ArchitectureSite;
import com.orange.util.SetUtil;

public class SiteComparator {
    // microservices in desiredArchitecture but not in currentArchitecture
    private Set<ArchitectureMicroservice> addedMicroservice = new HashSet<>();
    // microservices in currentArchitecture but not in desiredArchitecture
    private Set<ArchitectureMicroservice> removedMicroservice = new HashSet<>();
    private Map<ArchitectureMicroservice, ArchitectureMicroservice> updatedMicroservice = new HashMap<>();

    public SiteComparator(ArchitectureSite currentArchitecture, ArchitectureSite desiredArchitecture) {
	Set<String> desiredMicroserviceIds = new HashSet<>();
	for (ArchitectureMicroservice desiredMicroservice : desiredArchitecture.getArchitectureMicroservices()) {
	    if (desiredMicroservice.getGuid() == null) {
		ArchitectureMicroservice currentMicroservice = SetUtil.getUniqueMicroservice(
			currentArchitecture.getArchitectureMicroservices(),
			ms -> ms.getName().equals(desiredMicroservice.getName())
				&& ms.getVersion().equals(desiredMicroservice.getVersion()));
		if (currentMicroservice == null) {
		    if (desiredMicroservice.getPath() == null) {
			throw new IllegalStateException(
				String.format("The path of the new microservice [%s, %s] is not specified.",
					desiredMicroservice.getName(), desiredMicroservice.getVersion()));
		    }
		    addedMicroservice.add(desiredMicroservice);
		} else {
		    desiredMicroservice.setGuid(currentMicroservice.getGuid());
		    if (!currentMicroservice.equals(desiredMicroservice)) {
			updatedMicroservice.put(currentMicroservice, desiredMicroservice);
		    }
		    desiredMicroserviceIds.add(desiredMicroservice.getGuid());
		}
	    } else {
		desiredMicroserviceIds.add(desiredMicroservice.getGuid());
		ArchitectureMicroservice currentMicroservice = SetUtil.getUniqueMicroservice(
			currentArchitecture.getArchitectureMicroservices(),
			ms -> ms.getGuid().equals(desiredMicroservice.getGuid()));
		if (currentMicroservice == null) {
		    throw new IllegalStateException(
			    String.format("Desired microservice guid [%s] is not present in the current architecture.",
				    desiredMicroservice.getGuid()));
		}
		if (!currentMicroservice.equals(desiredMicroservice)) {
		    updatedMicroservice.put(currentMicroservice, desiredMicroservice);
		}
	    }
	}
	removedMicroservice = SetUtil.search(currentArchitecture.getArchitectureMicroservices(),
		currentMicroservice -> !desiredMicroserviceIds.contains(currentMicroservice.getGuid()));
    }

    public Set<ArchitectureMicroservice> getAddedMicroservice() {
	return addedMicroservice;
    }

    public Set<ArchitectureMicroservice> getRemovedMicroservice() {
	return removedMicroservice;
    }

    public Map<ArchitectureMicroservice, ArchitectureMicroservice> getUpdatedMicroservice() {
	return updatedMicroservice;
    }
}
