package com.orange.reconfig;

import java.util.Map.Entry;

import com.orange.model.OperationConfig;
import com.orange.model.PaaSSiteAccess;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.model.workflow.ParallelWorkflow;
import com.orange.model.workflow.SerialWorkflow;
import com.orange.model.workflow.Workflow;
import com.orange.paas.PaaSAPI;
import com.orange.paas.cf.CloudFoundryAPIv2;

public class WorkflowCalculator {
    private Architecture currentArchitecture;
    private Architecture desiredArchitecture;
    private OperationConfig config;

    public WorkflowCalculator(Architecture currentArchitecture, Architecture desiredArchitecture,
	    OperationConfig config) {
	this.currentArchitecture = currentArchitecture;
	this.desiredArchitecture = desiredArchitecture;
	this.config = config;
    }

    public Workflow getReconfigureWorkflow() {
	Workflow reconfigure = new ParallelWorkflow("parallel update sites");
	for (PaaSSiteAccess site : desiredArchitecture.listSitesAccess()) {
	    Workflow reconfigSite = config.isParallelUpdateMicroservices()
		    ? new ParallelWorkflow(String.format("parallel update site %s entities", site.getName()))
		    : new SerialWorkflow(String.format("serial update site %s entities", site.getName()));
	    SiteComparator comparator = new SiteComparator(currentArchitecture.getSite(site.getName()),
		    desiredArchitecture.getSite(site.getName()));
	    PaaSAPI api = parsePaaSApi(site, config);
	    for (Microservice addedMicroservice : comparator.getAddedMicroservices()) {
		reconfigSite.addStep(api.add(addedMicroservice));
	    }
	    for (Microservice removedMicroservice : comparator.getRemovedMicroservices()) {
		reconfigSite.addStep(api.remove(removedMicroservice));
	    }
	    for (Entry<Microservice, Microservice> modifiedMicroservice : comparator.getModifiedMicroservice()
		    .entrySet()) {
		reconfigSite.addStep(api.modify(modifiedMicroservice.getKey(), modifiedMicroservice.getValue()));
	    }
	    reconfigure.addStep(reconfigSite);
	}
	return reconfigure;
    }

    public static PaaSAPI parsePaaSApi(PaaSSiteAccess siteAccess, OperationConfig opConfig) {
	switch (siteAccess.getType()) {
	case "CloudFoundry":
	    return new CloudFoundryAPIv2(siteAccess, opConfig);
	case "Heroku":
		return new HerokuAPIImpl(siteAccess, opConfig);
	case "Kubernetes":
	    // TODO
	default:
	    throw new IllegalArgumentException("Unknown PaaS site type: " + siteAccess.getType());
	}
    }
}
