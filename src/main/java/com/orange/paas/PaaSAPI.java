package com.orange.paas;

import com.orange.model.*;
import com.orange.model.state.ArchitectureSite;

public abstract class PaaSAPI {
    protected PaaSSite site;
    protected OperationConfig operationConfig;

    public PaaSAPI(PaaSSite site, OperationConfig operationConfig) {
	this.site = site;
	this.operationConfig = operationConfig;
    }

    public abstract ArchitectureSite getSiteArchitecture();
}
