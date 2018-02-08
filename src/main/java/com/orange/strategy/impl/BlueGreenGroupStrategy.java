package com.orange.strategy.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.strategy.Transition;
import com.orange.util.SetUtil;

public class BlueGreenGroupStrategy extends BlueGreenStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenStrategy.class);

    public BlueGreenGroupStrategy(StrategyConfig config) {
	super(config);
	newPkgEnvTransit = newPkgEnvGroupTransit;
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	if (config.getGroupSize() <= 0) {
	    return false;
	}
	return super.valid(currentArchitecture, finalArchitecture);
    }

    protected Transition newPkgEnvGroupTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		int deployingNumber = 0;
		Set<Microservice> currentMicroservices = nextArchitecture.getSiteMicroservices(site);
		for (Microservice desiredMicroservice : finalArchitecture.getSiteMicroservices(site)) {
		    if (SetUtil.noneMatch(currentMicroservices, ms -> ms.getName().equals(desiredMicroservice.getName())
			    && ms.getVersion().equals(library.desiredVersion(desiredMicroservice)))) {
			deployingNumber++;
			Microservice newMicroservice = new Microservice(desiredMicroservice);
			newMicroservice.setGuid(null);
			newMicroservice.setRoutes(library.tmpRoute(site, desiredMicroservice));
			if (newMicroservice.getVersion() == null) {
			    newMicroservice.setVersion(config.getUpdatingVersion());
			}
			nextArchitecture.getSite(site).addMicroservice(newMicroservice);
			logger.info("Added a new microservice deployment: {} ", newMicroservice);
			if (deployingNumber == config.getGroupSize()) {
			    break;
			}
		    }
		}
	    }
	    return nextArchitecture;
	}
    };

}
