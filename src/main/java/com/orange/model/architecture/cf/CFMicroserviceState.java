package com.orange.model.architecture.cf;

import com.orange.model.architecture.MicroserviceState;

public enum CFMicroserviceState {
    CREATED(MicroserviceState.CREATED), UPLOADED(MicroserviceState.UPLOADED), STAGED(MicroserviceState.STAGED), RUNNING(
	    MicroserviceState.RUNNING), FAILED(MicroserviceState.FAILED), staging(MicroserviceState.UPLOADED), starting(
		    MicroserviceState.STAGED);

    private MicroserviceState state;

    private CFMicroserviceState(MicroserviceState state) {
	this.state = state;
    }

    public MicroserviceState asMicroserviceState() {
	return state;
    }
}