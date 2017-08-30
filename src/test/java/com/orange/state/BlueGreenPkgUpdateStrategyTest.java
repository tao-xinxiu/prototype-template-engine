package com.orange.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.orange.model.StrategyConfig;
import com.orange.model.PaaSSite;
import com.orange.model.StrategySiteConfig;
import com.orange.model.state.AppState;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.OverviewSite;
import com.orange.model.state.Route;
import com.orange.nextstate.strategy.BlueGreenPkgUpdateStrategy;
import com.orange.nextstate.strategy.Strategy;

public class BlueGreenPkgUpdateStrategyTest {
    private static final String site1name = "site1";
    private static final String site2name = "site2";
    private static final String appName = "appName";
    private static final String oldAppVersion = "v1.0.0";
    private static final String newAppVersion = "v1.1.0";
    private static final int appNbProcesses = 3;
    private static final String appDomain = "orange.com";
    private static final Map<String, String> appEnv = new HashMap<>(Collections.singletonMap("foo", "bar"));
    private static final Set<Route> appRoutes = Collections.singleton(new Route("app", appDomain));
    private static final Set<Route> appTmpRoutes = Collections
	    .singleton(new Route(appName + StrategySiteConfig.getDefaulttmproutehostsuffix(), appDomain));
    private static final String oldAppSite1Id = "oldApp-guid-site1";
    private static final String oldAppSite2Id = "oldApp-guid-site2";
    private static final String newAppSite1Id = "newApp-guid-site1";
    private static final String newAppSite2Id = "newApp-guid-site2";
    private static final String oldAppPath = String.format("/app/path/app_%s.zip", oldAppVersion);
    private static final String newAppPath = String.format("/app/path/app_%s.zip", newAppVersion);
    private static final String memory = "1G";
    private static final String disk = "1G";
    private static final Set<String> appServices = new HashSet<>();

    private final static StrategyConfig config = config();

    private static final PaaSSite site1 = new PaaSSite(site1name, "CloudFoundry", "site1-api", "site1-user",
	    "site1-pwd", "site1-org", "site1-space", true);
    private static final PaaSSite site2 = new PaaSSite(site2name, "CloudFoundry", "site2-api", "site2-user",
	    "site2-pwd", "site2-org", "site2-space", true);
    private final static Overview initState = initState();
    private final static Overview finalState = finalState();

    @Test
    public void should_get_expected_midstate1() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Assert.assertFalse(initState.isInstantiation(finalState));
	Assert.assertTrue(bgStrategy.valid(initState, finalState));
	// Assert.assertTrue(bgStrategy.transits().get(0).condition(initState,
	// finalState));
	Assert.assertTrue(bgStrategy.transits().get(0).next(initState, finalState).isInstantiation(midState1()));
    }

    @Test
    public void should_get_expected_midstate2() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Overview currentState = midState1Instantiated();
	Assert.assertFalse(currentState.isInstantiation(finalState));
	Assert.assertTrue(bgStrategy.valid(currentState, finalState));
	// Assert.assertFalse(bgStrategy.transits().get(0).condition(currentState,
	// finalState));
	// Assert.assertTrue(bgStrategy.transits().get(1).condition(currentState,
	// finalState));
	Assert.assertEquals(midState2(), bgStrategy.transits().get(1).next(currentState, finalState));
    }

    @Test
    public void should_get_expected_midstate3() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Overview currentState = midState2();
	Assert.assertFalse(currentState.isInstantiation(finalState));
	Assert.assertTrue(bgStrategy.valid(currentState, finalState));
	// Assert.assertFalse(bgStrategy.transits().get(0).condition(currentState,
	// finalState));
	// Assert.assertFalse(bgStrategy.transits().get(1).condition(currentState,
	// finalState));
	// Assert.assertTrue(bgStrategy.transits().get(2).condition(currentState,
	// finalState));
	Overview midState3 = bgStrategy.transits().get(2).next(currentState, finalState);
	Assert.assertEquals(midState3(), midState3);
	Assert.assertTrue(midState3.isInstantiation(finalState));
    }

    private final static StrategyConfig config() {
	StrategyConfig config = new StrategyConfig();
	config.setSiteConfig(site1name, new StrategySiteConfig(appDomain));
	config.setSiteConfig(site2name, new StrategySiteConfig(appDomain));
	return config;
    }

    private final static Overview initState() {
	Overview initState = new Overview();
	initState.addPaaSSite(site1,
		new OverviewSite(Collections.singleton(new OverviewApp(oldAppSite1Id, appName, oldAppVersion,
			oldAppPath, AppState.RUNNING, appNbProcesses, appEnv, appRoutes, appServices, memory, disk))));
	initState.addPaaSSite(site2,
		new OverviewSite(Collections.singleton(new OverviewApp(oldAppSite2Id, appName, oldAppVersion,
			oldAppPath, AppState.RUNNING, appNbProcesses, appEnv, appRoutes, appServices, memory, disk))));
	return initState;
    }

    private final static Overview finalState() {
	OverviewApp newApp = new OverviewApp(null, appName, null, newAppPath, AppState.RUNNING, appNbProcesses, appEnv,
		appRoutes, appServices, memory, disk);
	Overview finalState = new Overview();
	finalState.addPaaSSite(site1, new OverviewSite(Collections.singleton(newApp)));
	finalState.addPaaSSite(site2, new OverviewSite(Collections.singleton(newApp)));
	return finalState;
    }

    private final static Overview midState1() {
	Set<OverviewApp> site1Apps = new HashSet<>();
	site1Apps.add(new OverviewApp(oldAppSite1Id, appName, null, oldAppPath, AppState.RUNNING, appNbProcesses,
		appEnv, appRoutes, appServices, memory, disk));
	site1Apps.add(new OverviewApp(null, appName, null, newAppPath, AppState.RUNNING, appNbProcesses, appEnv,
		appTmpRoutes, appServices, memory, disk));
	Set<OverviewApp> site2Apps = new HashSet<>();
	site2Apps.add(new OverviewApp(oldAppSite2Id, appName, null, oldAppPath, AppState.RUNNING, appNbProcesses,
		appEnv, appRoutes, appServices, memory, disk));
	site2Apps.add(new OverviewApp(null, appName, null, newAppPath, AppState.RUNNING, appNbProcesses, appEnv,
		appTmpRoutes, appServices, memory, disk));
	Overview midState1 = new Overview();
	midState1.addPaaSSite(site1, new OverviewSite(site1Apps));
	midState1.addPaaSSite(site2, new OverviewSite(site2Apps));
	return midState1;
    }

    private final static Overview midState1Instantiated() {
	Set<OverviewApp> site1Apps = new HashSet<>();
	site1Apps.add(new OverviewApp(oldAppSite1Id, appName, oldAppVersion, oldAppPath, AppState.RUNNING,
		appNbProcesses, appEnv, appRoutes, appServices, memory, disk));
	site1Apps.add(new OverviewApp(newAppSite1Id, appName, newAppVersion, newAppPath, AppState.RUNNING,
		appNbProcesses, appEnv, appTmpRoutes, appServices, memory, disk));
	Set<OverviewApp> site2Apps = new HashSet<>();
	site2Apps.add(new OverviewApp(oldAppSite2Id, appName, oldAppVersion, oldAppPath, AppState.RUNNING,
		appNbProcesses, appEnv, appRoutes, appServices, memory, disk));
	site2Apps.add(new OverviewApp(newAppSite2Id, appName, newAppVersion, newAppPath, AppState.RUNNING,
		appNbProcesses, appEnv, appTmpRoutes, appServices, memory, disk));
	Overview midState1 = new Overview();
	midState1.addPaaSSite(site1, new OverviewSite(site1Apps));
	midState1.addPaaSSite(site2, new OverviewSite(site2Apps));
	return midState1;
    }

    private final static Overview midState2() {
	Set<OverviewApp> site1Apps = new HashSet<>();
	site1Apps.add(new OverviewApp(oldAppSite1Id, appName, oldAppVersion, oldAppPath, AppState.RUNNING,
		appNbProcesses, appEnv, appRoutes, appServices, memory, disk));
	site1Apps.add(new OverviewApp(newAppSite1Id, appName, newAppVersion, newAppPath, AppState.RUNNING,
		appNbProcesses, appEnv, appRoutes, appServices, memory, disk));
	Set<OverviewApp> site2Apps = new HashSet<>();
	site2Apps.add(new OverviewApp(oldAppSite2Id, appName, oldAppVersion, oldAppPath, AppState.RUNNING,
		appNbProcesses, appEnv, appRoutes, appServices, memory, disk));
	site2Apps.add(new OverviewApp(newAppSite2Id, appName, newAppVersion, newAppPath, AppState.RUNNING,
		appNbProcesses, appEnv, appRoutes, appServices, memory, disk));
	Overview midState2 = new Overview();
	midState2.addPaaSSite(site1, new OverviewSite(site1Apps));
	midState2.addPaaSSite(site2, new OverviewSite(site2Apps));
	return midState2;
    }

    private final static Overview midState3() {
	Overview midState3 = new Overview();
	midState3.addPaaSSite(site1,
		new OverviewSite(Collections.singleton(new OverviewApp(newAppSite1Id, appName, newAppVersion,
			newAppPath, AppState.RUNNING, appNbProcesses, appEnv, appRoutes, appServices, memory, disk))));
	midState3.addPaaSSite(site2,
		new OverviewSite(Collections.singleton(new OverviewApp(newAppSite2Id, appName, newAppVersion,
			newAppPath, AppState.RUNNING, appNbProcesses, appEnv, appRoutes, appServices, memory, disk))));
	return midState3;
    }

}
