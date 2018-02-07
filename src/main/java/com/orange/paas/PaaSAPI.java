package com.orange.paas;

import java.util.Set;

import com.orange.model.*;
import com.orange.model.architecture.Microservice;
import com.orange.model.workflow.Step;

public abstract class PaaSAPI {
    protected PaaSSiteAccess site;
    protected OperationConfig operationConfig;

    public PaaSAPI(PaaSSiteAccess site, OperationConfig operationConfig) {
	this.site = site;
	this.operationConfig = operationConfig;
    }

    public abstract Set<Microservice> get();

    public abstract Step add(Microservice microservice);

    public abstract Step remove(Microservice microservice);

    public abstract Step modify(Microservice currentMicroservice,
	    Microservice desiredMicroservice);
}
