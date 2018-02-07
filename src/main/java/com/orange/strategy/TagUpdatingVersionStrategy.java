package com.orange.strategy;

import com.orange.model.StrategyConfig;

public abstract class TagUpdatingVersionStrategy extends Strategy {
    public TagUpdatingVersionStrategy(StrategyConfig config) {
	super(config);
	if (config.getUpdatingVersion() == null) {
	    throw new IllegalStateException("Updating version is not set.");
	}
    }

//    @Override
//    protected boolean isInstantiation(ArchitectureMicroservice currentMicroservice,
//	    ArchitectureMicroservice desiredMicroservice) {
//	return super.isInstantiation(currentMicroservice, desiredMicroservice)
//		&& !currentMicroservice.getVersion().equals(config.getUpdatingVersion());
//    }
}
