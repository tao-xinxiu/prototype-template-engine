package com.orange.reconfig;

import java.util.Map.Entry;

import com.orange.model.OperationConfig;
import com.orange.model.PaaSSite;
import com.orange.model.state.Architecture;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.model.workflow.ParallelWorkflow;
import com.orange.model.workflow.SerialWorkflow;
import com.orange.model.workflow.Workflow;
import com.orange.paas.PaaSAPI;
import com.orange.paas.cf.CloudFoundryAPIv2;

public class WorkflowCalculator {
    private Architecture currentState;
    private Architecture desiredState;
    private OperationConfig config;

    public WorkflowCalculator(Architecture currentState, Architecture desiredState, OperationConfig config) {
	this.currentState = currentState;
	this.desiredState = desiredState;
	this.config = config;
    }

    public Workflow getUpdateWorkflow() {
	Workflow updateSites = new ParallelWorkflow("parallel update sites");
	for (PaaSSite site : desiredState.listPaaSSites()) {
	    Workflow updateSite = config.isParallelUpdateMicroservices()
		    ? new ParallelWorkflow(String.format("parallel update site %s entities", site.getName()))
		    : new SerialWorkflow(String.format("serial update site %s entities", site.getName()));
	    SiteComparator comparator = new SiteComparator(currentState.getArchitectureSite(site.getName()),
		    desiredState.getArchitectureSite(site.getName()));
	    PaaSAPI directory = new CloudFoundryAPIv2(site, config);
	    for (ArchitectureMicroservice microservice : comparator.getAddedMicroservice()) {
		updateSite.addStep(directory.add(microservice));
	    }
	    for (ArchitectureMicroservice microservice : comparator.getRemovedMicroservice()) {
		updateSite.addStep(directory.remove(microservice));
	    }
	    for (Entry<ArchitectureMicroservice, ArchitectureMicroservice> updatedmicroservice : comparator
		    .getUpdatedMicroservice().entrySet()) {
		updateSite.addStep(directory.update(updatedmicroservice.getKey(), updatedmicroservice.getValue()));
	    }
	    updateSites.addStep(updateSite);
	}
	return updateSites;
    }
}
