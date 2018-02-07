package com.orange.nextstate.strategy;

import com.orange.model.state.Architecture;

public interface Transition {
    public abstract Architecture next(Architecture currentState, Architecture finalState);
}
