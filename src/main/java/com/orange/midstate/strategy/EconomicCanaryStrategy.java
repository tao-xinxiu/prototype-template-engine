package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;

// Strategy assume route not updated between Ainit and Af
public class EconomicCanaryStrategy extends CanaryStrategy {
    private static final Logger logger = LoggerFactory.getLogger(CanaryStrategy.class);

    public EconomicCanaryStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	for (String site : finalState.listSitesName()) {
	    for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		if (desiredApp.getState() != AppState.RUNNING) {
		    return false;
		}
		Set<OverviewApp> currentApps = SetUtil
			.searchByName(currentState.getOverviewSite(site).getOverviewApps(), desiredApp.getName());
		if (!SetUtil.uniqueByPathEnv(currentApps)) {
		    return false;
		}
	    }
	}
	return true;
    }

    @Override
    public List<TransitPoint> transitPoints() {
	return Arrays.asList(library.cleanNotUpdatableAppTransit, rolloutTransit, addCanaryTransit,
		updateServiceStateTransit, updateRouteTransit, scaleupTransit, library.removeUndesiredTransit);
    }

    // scale down non-desired microservice when the microservice routed running
    // instances equals to desired instances
    protected TransitPoint rolloutTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    Set<OverviewApp> currentApps = SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getState().equals(desiredApp.getState())
				    && app.getRoutes().equals(desiredApp.getRoutes()));
		    if (currentApps.stream().mapToInt(app -> app.getNbProcesses()).sum() == desiredApp
			    .getNbProcesses()) {
			OverviewApp nextApp = SetUtil.getUniqueApp(currentApps,
				app -> !app.getPath().equals(desiredApp.getPath())
					|| !app.getEnv().equals(desiredApp.getEnv()));
			if (nextApp.getNbProcesses() > 1) {
			    logger.info("rolloutTransit detected for microservice {}", desiredApp);
			    return true;
			}
		    }
		}
	    }
	    return false;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    Set<OverviewApp> nextApps = SetUtil.search(nextState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getState().equals(desiredApp.getState())
				    && app.getRoutes().equals(desiredApp.getRoutes()));
		    if (nextApps.stream().mapToInt(app -> app.getNbProcesses()).sum() == desiredApp.getNbProcesses()) {
			OverviewApp nextApp = SetUtil.getUniqueApp(nextApps,
				app -> !app.getPath().equals(desiredApp.getPath())
					|| !app.getEnv().equals(desiredApp.getEnv()));
			boolean canaryNotCreated = SetUtil.noneMatch(nextApps,
				app -> app.getPath().equals(desiredApp.getPath())
					&& app.getEnv().equals(desiredApp.getEnv()));
			int scaleDownNb = canaryNotCreated ? config.getCanaryNbr() : config.getCanaryIncrease();
			int nextNbr = nextApp.getNbProcesses() - scaleDownNb;
			if (nextNbr >= 1) {
			    nextApp.setNbProcesses(nextNbr);
			} else {
			    nextApps.remove(nextApp);
			}
		    }
		}
	    }
	    return nextState;
	}
    };

    protected TransitPoint scaleupTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    return library.desiredAppInstantiationExistCondition(currentState, finalState);
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    logger.info("start getting next architecture by scale up desired microservice and rollout old ones");
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    OverviewApp nextApp = SetUtil.getUniqueApp(nextState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getEnv().equals(desiredApp.getEnv()));
		    int nextNbr = nextApp.getNbProcesses() + config.getCanaryIncrease();
		    if (nextNbr > desiredApp.getNbProcesses()) {
			nextNbr = desiredApp.getNbProcesses();
		    }
		    nextApp.setNbProcesses(nextNbr);
		}
	    }
	    return nextState;
	}

    };
}
