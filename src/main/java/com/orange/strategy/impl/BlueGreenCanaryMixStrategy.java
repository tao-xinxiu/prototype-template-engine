package com.orange.strategy.impl;

import java.util.Arrays;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.strategy.Transition;
import com.orange.util.SetUtil;

public class BlueGreenCanaryMixStrategy extends CanaryStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenCanaryMixStrategy.class);

    public BlueGreenCanaryMixStrategy(StrategyConfig config) {
	super(config);
	transitions = Arrays.asList(addCanaryTransit, updateExceptInstancesRoutesTransit,
		library.updateRouteBeforeNbProcTransit, scaleTransit, library.removeUndesiredTransit);
    }

    @Override
    public boolean valid(Architecture currentArchitecture, Architecture finalArchitecture) {
	return true;
    }

    /**
     * next architecture: scale up desired microservice and rollout old ones
     */
    protected Transition scaleTransit = new Transition() {
	@Override
	public Architecture next(Architecture currentArchitecture, Architecture finalArchitecture) {
	    Architecture nextArchitecture = new Architecture(currentArchitecture);
	    for (String site : finalArchitecture.listSitesName()) {
		for (Microservice desiredMs : finalArchitecture.getSiteMicroservices(site)) {
		    Set<Microservice> nextMss = nextArchitecture.getSiteMicroservices(site);
		    if (SetUtil.noneMatch(nextMss, ms -> ms.isInstantiation(desiredMs))) {
			for (Microservice nextMs : SetUtil.searchByName(nextMss, (String) desiredMs.get("name"))) {
			    int desiredNbProc = (int) desiredMs.get("nbProcesses");
			    if (nextMs.get("version").equals(library.desiredVersion(desiredMs))) {
				int nextNbr = (int) nextMs.get("nbProcesses") + config.getCanaryIncrease();
				nextNbr = nextNbr > desiredNbProc ? desiredNbProc : nextNbr;
				nextMs.set("nbProcesses", nextNbr);
			    } else {
				int nextNbr = (int) nextMs.get("nbProcesses") - config.getCanaryIncrease();
				nextNbr = nextNbr < 1 ? 1 : nextNbr;
				nextMs.set("nbProcesses", nextNbr);
			    }
			    logger.info("scaled microservice {}", nextMs);
			}
		    }
		}
	    }
	    return nextArchitecture;
	}
    };
}
