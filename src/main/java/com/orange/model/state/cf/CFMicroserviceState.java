package com.orange.model.state.cf;

import com.orange.model.state.MicroserviceState;

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