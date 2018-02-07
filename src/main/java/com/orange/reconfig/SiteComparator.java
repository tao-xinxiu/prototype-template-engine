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
    private Set<ArchitectureMicroservice> addedMicroservices = new HashSet<>();
    // microservices in currentArchitecture but not in desiredArchitecture
    private Set<ArchitectureMicroservice> removedMicroservices = new HashSet<>();
    // microservices different in currentArchitecture and desiredArchitecture
    private Map<ArchitectureMicroservice, ArchitectureMicroservice> modifiedMicroservices = new HashMap<>();

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
		    addedMicroservices.add(desiredMicroservice);
		} else {
		    desiredMicroservice.setGuid(currentMicroservice.getGuid());
		    if (!currentMicroservice.equals(desiredMicroservice)) {
			modifiedMicroservices.put(currentMicroservice, desiredMicroservice);
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
		    modifiedMicroservices.put(currentMicroservice, desiredMicroservice);
		}
	    }
	}
	removedMicroservices = SetUtil.search(currentArchitecture.getArchitectureMicroservices(),
		currentMicroservice -> !desiredMicroserviceIds.contains(currentMicroservice.getGuid()));
    }

    public Set<ArchitectureMicroservice> getAddedMicroservices() {
	return addedMicroservices;
    }

    public Set<ArchitectureMicroservice> getRemovedMicroservices() {
	return removedMicroservices;
    }

    public Map<ArchitectureMicroservice, ArchitectureMicroservice> getModifiedMicroservice() {
	return modifiedMicroservices;
    }
}
