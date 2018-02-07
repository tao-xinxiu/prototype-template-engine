package com.orange.model.architecture;

import com.orange.model.architecture.cf.CFMicroserviceState;

public enum MicroserviceState {
    CREATED(CFMicroserviceState.CREATED), UPLOADED(CFMicroserviceState.UPLOADED), STAGED(
	    CFMicroserviceState.STAGED), RUNNING(CFMicroserviceState.RUNNING), FAILED(CFMicroserviceState.FAILED);

    private CFMicroserviceState cfState;

    private MicroserviceState(CFMicroserviceState cfState) {
	this.cfState = cfState;
    }

    public CFMicroserviceState asCFState() {
	return cfState;
    }
}
