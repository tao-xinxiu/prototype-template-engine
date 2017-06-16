package com.orange.model.state;

import com.orange.model.state.cf.CFAppState;

public enum AppState {
    CREATED(CFAppState.CREATED), UPLOADED(CFAppState.UPLOADED), STAGED(CFAppState.STAGED), RUNNING(
	    CFAppState.RUNNING), FAILED(CFAppState.FAILED);

    private CFAppState cfState;

    private AppState(CFAppState cfState) {
	this.cfState = cfState;
    }

    public CFAppState asCFState() {
	return cfState;
    }
}
