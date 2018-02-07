package com.orange.paas;

import com.orange.model.*;
import com.orange.model.architecture.Microservice;
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

    public abstract Step add(Microservice microservice);

    public abstract Step remove(Microservice microservice);

    public abstract Step modify(Microservice currentMicroservice,
	    Microservice desiredMicroservice);
}
