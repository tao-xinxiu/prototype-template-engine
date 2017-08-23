package com.orange.midstate.strategy;

import com.orange.model.state.Overview;

public interface Transit {
    public abstract Overview next(Overview currentState, Overview finalState);
}
