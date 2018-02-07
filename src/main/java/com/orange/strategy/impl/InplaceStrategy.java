package com.orange.strategy.impl;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.strategy.Strategy;
import com.orange.util.SetUtil;

public class InplaceStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(InplaceStrategy.class);

    public InplaceStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.directTransit(false));
    }

    /**
     * 
     * 
     * If microservice name in finalArchitecture not unique, then return false.
     * i.e. This strategy only deal with the case that only one deployment
     * version (i.e. ArchitectureMicroservice) per microservice (identify by
     * name).
     */
    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	for (String site : finalArchitecture.listSitesName()) {
	    if (!SetUtil.uniqueByName(finalArchitecture.getSiteMicroservices(site))) {
		logger.error("InplaceStrategy should be used for the case that multi versions in currentArchitecture.");
		return false;
	    }
	}
	return true;
    }
}
