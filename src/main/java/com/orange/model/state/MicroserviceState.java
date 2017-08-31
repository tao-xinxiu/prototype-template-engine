package com.orange.model.state;

import com.orange.model.state.cf.CFMicroserviceState;

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
