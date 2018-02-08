package com.orange.strategy.impl;

import java.util.Arrays;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.reconfig.SiteComparator;
import com.orange.strategy.Strategy;

/**
 * A strategy used for initial deployment
 *
 */
public class DeployStrategy extends Strategy {

    public DeployStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(library.addNewTransit);
    }

    /**
     * The strategy is only for deploying new microservices
     */
    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	for (String siteName : finalArchitecture.listSitesName()) {
	    SiteComparator siteComparator = new SiteComparator(currentArchitecture.getSite(siteName),
		    finalArchitecture.getSite(siteName));
	    if (!siteComparator.getModifiedMicroservice().isEmpty()) {
		return false;
	    }
	    if (!siteComparator.getRemovedMicroservices().isEmpty()) {
		return false;
	    }
	}
	return true;
    }
    
}
