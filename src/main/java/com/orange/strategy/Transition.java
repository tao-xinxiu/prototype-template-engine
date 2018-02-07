package com.orange.strategy;

import com.orange.model.architecture.Architecture;

public interface Transition {
    public abstract Architecture next(Architecture currentArchitecture, Architecture finalArchitecture);
}
