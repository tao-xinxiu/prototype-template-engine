package com.orange.nextstate.strategy;

import com.orange.model.StrategyConfig;
import com.orange.model.state.ArchitectureMicroservice;

public abstract class TagUpdatingVersionStrategy extends Strategy {
    public TagUpdatingVersionStrategy(StrategyConfig config) {
	super(config);
	if (config.getUpdatingVersion() == null) {
	    throw new IllegalStateException("Updating version is not set.");
	}
    }

    @Override
    protected boolean isInstantiation(ArchitectureMicroservice currentMicroservice,
	    ArchitectureMicroservice desiredMicroservice) {
	return super.isInstantiation(currentMicroservice, desiredMicroservice)
		&& !currentMicroservice.getVersion().equals(config.getUpdatingVersion());
    }
}
