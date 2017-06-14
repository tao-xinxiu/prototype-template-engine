package com.orange.midstate.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.orange.model.DeploymentConfig;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.util.SetUtil;
import com.orange.util.VersionGenerator;

public class BlueGreenPkgUpdateStrategy extends Strategy {
    public BlueGreenPkgUpdateStrategy(DeploymentConfig config) {
	super(config);
    }

    @Override
    public boolean valid(Overview currentState, Overview finalState) {
	for (String site : finalState.listSitesName()) {
	    for (OverviewApp app : finalState.getOverviewSite(site).getOverviewApps()) {
		// app package name doesn't conform "xxxxx_va.b.c.yyy"
		if (!VersionGenerator.validPackage(app.getPath())) {
		    return false;
		}
	    }
	}
	return true;
    }

    @Override
    public List<TransitPoint> transitPoints() {
	return Arrays.asList(pkgUpdateTransit, routeUpdateTransit, removeUndesiredTransit);
    }

    private TransitPoint pkgUpdateTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath()))
			    .isEmpty()) {
			return true;
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
		    if (SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath()))
			    .isEmpty()) {
			OverviewApp newApp = new OverviewApp(desiredApp);
			newApp.setRoutes(
				Collections.singleton(config.getSiteConfig(site).getTmpRoute(desiredApp.getName())));
			newApp.setInstanceVersion(VersionGenerator.fromPackage(desiredApp.getPath()));
			nextState.getOverviewSite(site).addOverviewApp(newApp);
		    }
		}
	    }
	    return nextState;
	}
    };

    private TransitPoint routeUpdateTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp desiredApp : finalState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.search(currentState.getOverviewSite(site).getOverviewApps(),
			    app -> app.getName().equals(desiredApp.getName())
				    && app.getPath().equals(desiredApp.getPath())
				    && app.getRoutes().equals(desiredApp.getRoutes()))
			    .isEmpty()) {
			return true;
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
		    for (OverviewApp nextApp : nextState.getOverviewSite(site).getOverviewApps()) {
			if (nextApp.getName().equals(desiredApp.getName())
				&& nextApp.getPath().equals(desiredApp.getPath())) {
			    if (!nextApp.getRoutes().equals(desiredApp.getRoutes())) {
				nextApp.setRoutes(desiredApp.getRoutes());
			    }
			}
		    }
		}
	    }
	    return nextState;
	}
    };

    private TransitPoint removeUndesiredTransit = new TransitPoint() {
	@Override
	public boolean condition(Overview currentState, Overview finalState) {
	    for (String site : finalState.listSitesName()) {
		for (OverviewApp currentApp : currentState.getOverviewSite(site).getOverviewApps()) {
		    if (SetUtil.search(finalState.getOverviewSite(site).getOverviewApps(),
			    desiredApp -> desiredApp.getName().equals(currentApp.getName())
				    && desiredApp.getPath().equals(currentApp.getPath())
				    && desiredApp.listRoutes().equals(currentApp.listRoutes()))
			    .isEmpty()) {
			return true;
		    }
		}
	    }
	    return false;
	}

	@Override
	public Overview next(Overview currentState, Overview finalState) {
	    Overview nextState = new Overview(currentState);
	    for (String site : finalState.listSitesName()) {
		Iterator<OverviewApp> iterator = nextState.getOverviewSite(site).getOverviewApps().iterator();
		while (iterator.hasNext()) {
		    OverviewApp app = iterator.next();
		    if (SetUtil.search(finalState.getOverviewSite(site).getOverviewApps(),
			    desiredApp -> desiredApp.getName().equals(app.getName())
				    && desiredApp.getPath().equals(app.getPath())
				    && desiredApp.listRoutes().equals(app.listRoutes()))
			    .isEmpty()) {
			iterator.remove();
		    }
		}
	    }
	    return nextState;
	}
    };
}
