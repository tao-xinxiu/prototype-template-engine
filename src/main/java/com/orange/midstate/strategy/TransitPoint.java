package com.orange.midstate.strategy;

import com.orange.model.state.Overview;

public interface TransitPoint {
    public abstract boolean condition(Overview currentState, Overview finalState);

    public abstract Overview next(Overview currentState, Overview finalState);
}
