package com.orange.strategy.impl;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.strategy.Strategy;
import com.orange.util.SetUtil;

public class InplaceStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(InplaceStrategy.class);

    public InplaceStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.directTransit(false));
    }

    /**
     * This strategy disallows two cases: 1) finalArchitecture contains any
     * microservices with null version while multiple versions of this
     * microservice coexist in the currentArchitecture. 2) finalArchitecture
     * contains any microservices has more than one null version deployment.
     * 
     * Because in these cases, the strategy can't decide the final microservice
     * correspond to which current microservice deployment.
     */
    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	for (String site : finalArchitecture.listSitesName()) {
	    Set<Microservice> notVersionedMicroservices = SetUtil
		    .search(finalArchitecture.getSiteMicroservices(site), m -> m.getVersion() == null);
	    if (!SetUtil.uniqueByName(notVersionedMicroservices)) {
		logger.error(
			"InplaceStrategy disallows the case that multi not versioned deployment of one microservices in finalArchitecture.");
		return false;
	    }
	    for (Microservice notVersionedMicroservice : notVersionedMicroservices) {
		Set<Microservice> currentMicroservices = SetUtil.searchByName(
			currentArchitecture.getSiteMicroservices(site), notVersionedMicroservice.getName());
		if (!SetUtil.uniqueByName(currentMicroservices)) {
		    logger.error(
			    "InplaceStrategy disallows the case that multi currently deployed microservices correspond to an unversioned microservice in finalArchitecture.");
		    return false;
		}
	    }
	}
	return true;
    }
}
