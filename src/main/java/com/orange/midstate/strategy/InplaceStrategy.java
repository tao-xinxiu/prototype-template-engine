package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;
import com.orange.util.VersionGenerator;

public class InplaceStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(InplaceStrategy.class);

    public InplaceStrategy(StrategyConfig config) {
	super(config);
    }

    /**
     * If app name in currentState not unique, then return false. i.e. This
     * strategy only deal with the case that only one microservice instance
     * (i.e. OverviewApp) per microservice (identify by name).
     */
    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	for (String site : finalState.listSitesName()) {
	    if (!SetUtil.uniqueByName(finalState.getOverviewSite(site).getOverviewApps())) {
		return false;
	    }
	}
	return true;
    }

    @Override
    public List<TransitPoint> transitPoints() {
	return Arrays.asList(directTransit);
    }

    protected TransitPoint directTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    return true;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    logger.info("Start getting next architecture by direct in-place update");
	    Overview nextState = new Overview(finalState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp nextApp : nextState.getOverviewSite(site).getOverviewApps()) {
		    Set<OverviewApp> currentApps = SetUtil
			    .searchByName(currentState.getOverviewSite(site).getOverviewApps(), nextApp.getName());
		    if (currentApps.size() == 0) {
			// Add non-exist microservice
			nextApp.setGuid(null);
			nextApp.setInstanceVersion(VersionGenerator.random(new HashSet<>()));
			logger.info("{} detected as a new microservice.", nextApp);
		    } else {
			// prefer path and env equals app if exist
			OverviewApp currentApp = SetUtil.getOneApp(currentApps,
				app -> app.getPath().equals(nextApp.getPath())
					&& app.getEnv().equals(nextApp.getEnv()));
			if (currentApp == null) {
			    currentApp = currentApps.iterator().next();
			}
			nextApp.setGuid(currentApp.getGuid());
			nextApp.setInstanceVersion(currentApp.getInstanceVersion());
			logger.info("{} detected as a updated microservice", nextApp);
		    }
		}
	    }
	    return nextState;
	}
    };

}
