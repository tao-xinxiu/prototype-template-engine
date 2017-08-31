package com.orange.paas;

import com.orange.model.*;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.model.state.ArchitectureSite;
import com.orange.model.workflow.Step;

public abstract class PaaSAPI {
    protected PaaSSite site;
    protected OperationConfig operationConfig;

    public PaaSAPI(PaaSSite site, OperationConfig operationConfig) {
	this.site = site;
	this.operationConfig = operationConfig;
    }

    public abstract ArchitectureSite getSiteArchitecture();

    public abstract Step add(ArchitectureMicroservice microservice);

    public abstract Step remove(ArchitectureMicroservice microservice);

    public abstract Step update(ArchitectureMicroservice currentMicroservice,
	    ArchitectureMicroservice desiredMicroservice);
}
