package com.orange.model.state.cf;

import com.orange.model.state.AppState;

public enum CFAppState {
    CREATED(AppState.CREATED), UPLOADED(AppState.UPLOADED), STAGED(AppState.STAGED), RUNNING(AppState.RUNNING), FAILED(
	    AppState.FAILED), staging(AppState.UPLOADED), starting(AppState.STAGED);

    private AppState appState;

    private CFAppState(AppState appState) {
	this.appState = appState;
    }

    public AppState asAppState() {
	return appState;
    }
}