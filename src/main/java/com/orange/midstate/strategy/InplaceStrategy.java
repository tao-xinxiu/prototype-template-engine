package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;

public class InplaceStrategy extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(BlueGreenStrategy.class);

    public InplaceStrategy(StrategyConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	for (String site : finalState.listSitesName()) {
	    //TODO if app name not unique -> return false
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
	    logger.info("start getting next architecture by direct in-place update");
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredapp : finalState.getOverviewSite(site).getOverviewApps()) {
		    OverviewApp currentApp = SetUtil.searchByName(currentState.getOverviewSite(site).getOverviewApps(),
			    desiredapp.getName());
		    if (currentApp == null) {
			nextState.getOverviewSite(site).addOverviewApp(desiredapp);
			logger.info("Added a new microservice: {} ", desiredapp);
		    } else {

		    }
		}
	    }
	    return nextState;
	}
    };

}
