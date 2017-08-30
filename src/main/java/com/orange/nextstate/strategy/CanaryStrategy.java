package com.orange.nextstate.strategy;

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
public class CanaryStrategy extends BlueGreenCanaryMixStrategy {
    private static final Logger logger = LoggerFactory.getLogger(CanaryStrategy.class);

    public CanaryStrategy(StrategyConfig config) {
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
    public List<Transit> transits() {
	return Arrays.asList(rolloutTransit, addCanaryTransit, updateExceptInstancesRoutesTransit, updateRouteTransit,
		scaleupTransit, library.removeUndesiredTransit);
    }

    /**
     * next architecture: scale down non-desired microservice when the
     * microservice routed running instances equals to desired instances
     */
    protected Transit rolloutTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    Set<OverviewApp> nextApps = nextState.getOverviewSite(site).getOverviewApps();
		    Set<OverviewApp> relatedApps = SetUtil.search(nextApps,
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getState().equals(desiredApp.getState())
				    && app.getRoutes().equals(desiredApp.getRoutes()));
		    if (relatedApps.stream().mapToInt(app -> app.getNbProcesses()).sum() == desiredApp
			    .getNbProcesses()) {
			OverviewApp nextApp = SetUtil.getUniqueApp(relatedApps,
				app -> !app.getVersion().equals(library.desiredVersion(desiredApp)));
			boolean canaryNotCreated = SetUtil.noneMatch(relatedApps,
				app -> app.getVersion().equals(library.desiredVersion(desiredApp)));
			int scaleDownNb = canaryNotCreated ? config.getCanaryNbr() : config.getCanaryIncrease();
			int nextNbr = nextApp.getNbProcesses() - scaleDownNb;
			if (nextNbr >= 1) {
			    nextApp.setNbProcesses(nextNbr);
			    logger.info("rolled out microservice {}", nextApp);
			} else {
			    nextApps.remove(nextApp);
			    logger.info("removed microservice {}", nextApp);
			}
		    }
		}
	    }
	    return nextState;
	}
    };

    /**
     * get next architecture: scale up desired microservices
     */
    protected Transit scaleupTransit = new Transit() {
	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    Set<OverviewApp> nextApps = nextState.getOverviewSite(site).getOverviewApps();
		    if (SetUtil.noneMatch(nextApps, app -> app.isInstantiation(desiredApp))) {
			OverviewApp nextApp = SetUtil.getUniqueApp(nextApps,
				app -> app.getName().equals(desiredApp.getName())
					&& app.getVersion().equals(library.desiredVersion(desiredApp)));
			int nextNbr = nextApp.getNbProcesses() + config.getCanaryIncrease();
			if (nextNbr > desiredApp.getNbProcesses()) {
			    nextNbr = desiredApp.getNbProcesses();
			}
			nextApp.setNbProcesses(nextNbr);
		    }
		}
	    }
	    return nextState;
	}

    };
}
