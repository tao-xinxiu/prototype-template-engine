package com.orange.paas;

import com.orange.model.*;
import com.orange.model.architecture.ArchitectureMicroservice;
import com.orange.model.architecture.ArchitectureSite;
import com.orange.model.workflow.Step;

public abstract class PaaSAPI {
    protected PaaSSite site;
    protected OperationConfig operationConfig;

    public PaaSAPI(PaaSSite site, OperationConfig operationConfig) {
	this.site = site;
	this.operationConfig = operationConfig;
    }

    public abstract ArchitectureSite get();

    public abstract Step add(ArchitectureMicroservice microservice);

    public abstract Step remove(ArchitectureMicroservice microservice);

    public abstract Step modify(ArchitectureMicroservice currentMicroservice,
	    ArchitectureMicroservice desiredMicroservice);
}
