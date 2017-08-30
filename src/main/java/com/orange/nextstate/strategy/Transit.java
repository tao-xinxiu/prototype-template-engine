package com.orange.nextstate.strategy;

import com.orange.model.state.Overview;

public interface Transit {
    public abstract Overview next(Overview currentState, Overview finalState);
}
