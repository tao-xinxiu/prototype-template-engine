package com.orange.midstate.strategy;

import java.util.List;

import com.orange.model.DeploymentConfig;
import com.orange.model.state.Overview;

public abstract class Strategy {
    protected DeploymentConfig config;

    public Strategy(DeploymentConfig config) {
	this.config = config;
    }

    public abstract boolean valid(Overview currentState, Overview finalState);

    public abstract List<TransitPoint> transitPoints();

}
