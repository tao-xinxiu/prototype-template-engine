package com.orange.strategy.impl;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.strategy.Strategy;
import com.orange.strategy.Transition;
import com.orange.util.SetUtil;

public class UpdateRemoveStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(UpdateRemoveStrategy.class);

    public UpdateRemoveStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(updateTransition, library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	return true;
    }

    protected Transition updateTransition = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
		    Set<Microservice> nextMss = SetUtil.searchByName(nextArchitecture.getSiteMicroservices(site),
			    (String) desiredMs.get("name"));
		    if (nextMss.size() == 0) {
			Microservice addedMs = new Microservice(desiredMs);
			// Add non-exist microservice
			addedMs.set("guid", null);
			nextArchitecture.getSite(site).addMicroservice(addedMs);
			logger.info("{} detected as a new microservice.", addedMs);
		    } else {
			// update from the most similar microservice (i.e. path and env equals)
			Microservice updatedMicroservice = SetUtil.getOneMicroservice(nextMss,
				ms -> ms.get("path").equals(desiredMs.get("path"))
					&& ms.get("env").equals(desiredMs.get("env")));
			if (updatedMicroservice == null) {
			    // update from any version of the microservice when not exist path and env eq ms
			    updatedMicroservice = nextMss.iterator().next();
			}
			if (!updatedMicroservice.isInstantiation(desiredMs)) {
			    updatedMicroservice.copyAttrExcept(Arrays.asList("guid"), desiredMs);
			    logger.info("{} detected as a updated microservice", updatedMicroservice);
			}
		    }
		}
	    }
	    return nextArchitecture;
	}
    };
}
