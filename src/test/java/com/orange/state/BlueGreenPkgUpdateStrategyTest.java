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
import com.orange.model.state.MicroserviceState;
import com.orange.model.state.Architecture;
import com.orange.model.state.ArchitectureMicroservice;
import com.orange.model.state.ArchitectureSite;
import com.orange.model.state.Route;
import com.orange.nextstate.strategy.BlueGreenPkgUpdateStrategy;
import com.orange.nextstate.strategy.Strategy;

public class BlueGreenPkgUpdateStrategyTest {
    private static final String site1name = "site1";
    private static final String site2name = "site2";
    private static final String msName = "msName";
    private static final String oldMsVersion = "v1.0.0";
    private static final String newMsVersion = "v1.1.0";
    private static final int msNbProcesses = 3;
    private static final String domain = "orange.com";
    private static final Map<String, String> msEnv = new HashMap<>(Collections.singletonMap("foo", "bar"));
    private static final Set<Route> msRoutes = Collections.singleton(new Route("test", domain));
    private static final Set<Route> msTmpRoutes = Collections
	    .singleton(new Route(msName + StrategySiteConfig.getDefaulttmproutehostsuffix(), domain));
    private static final String oldMsSite1Id = "oldMs-guid-site1";
    private static final String oldMsSite2Id = "oldMs-guid-site2";
    private static final String newMsSite1Id = "newMs-guid-site1";
    private static final String newMsSite2Id = "newMs-guid-site2";
    private static final String oldMsPath = String.format("/ms/path/ms_%s.zip", oldMsVersion);
    private static final String newMsPath = String.format("/ms/path/ms_%s.zip", newMsVersion);
    private static final String memory = "1G";
    private static final String disk = "1G";
    private static final Set<String> msServices = new HashSet<>();

    private final static StrategyConfig config = config();

    private static final PaaSSite site1 = new PaaSSite(site1name, "CloudFoundry", "site1-api", "site1-user",
	    "site1-pwd", "site1-org", "site1-space", true);
    private static final PaaSSite site2 = new PaaSSite(site2name, "CloudFoundry", "site2-api", "site2-user",
	    "site2-pwd", "site2-org", "site2-space", true);
    private final static Architecture initState = initState();
    private final static Architecture finalState = finalState();

    @Test
    public void should_get_expected_midstate1() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Assert.assertFalse(initState.isInstantiation(finalState));
	Assert.assertTrue(bgStrategy.valid(initState, finalState));
	// Assert.assertTrue(bgStrategy.transits().get(0).condition(initState,
	// finalState));
	Assert.assertEquals(bgStrategy.getTransitions().get(0).next(initState, finalState), midState1());
    }

    @Test
    public void should_get_expected_midstate2() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Architecture currentState = midState1Instantiated();
	Assert.assertFalse(currentState.isInstantiation(finalState));
	Assert.assertTrue(bgStrategy.valid(currentState, finalState));
	// Assert.assertFalse(bgStrategy.transits().get(0).condition(currentState,
	// finalState));
	// Assert.assertTrue(bgStrategy.transits().get(1).condition(currentState,
	// finalState));
	Assert.assertEquals(midState2(), bgStrategy.getTransitions().get(1).next(currentState, finalState));
    }

    @Test
    public void should_get_expected_midstate3() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Architecture currentState = midState2();
	Assert.assertFalse(currentState.isInstantiation(finalState));
	Assert.assertTrue(bgStrategy.valid(currentState, finalState));
	// Assert.assertFalse(bgStrategy.transits().get(0).condition(currentState,
	// finalState));
	// Assert.assertFalse(bgStrategy.transits().get(1).condition(currentState,
	// finalState));
	// Assert.assertTrue(bgStrategy.transits().get(2).condition(currentState,
	// finalState));
	Architecture midState3 = bgStrategy.getTransitions().get(2).next(currentState, finalState);
	Assert.assertEquals(midState3(), midState3);
	Assert.assertTrue(bgStrategy.valid(midState3, finalState));
    }

    private final static StrategyConfig config() {
	StrategyConfig config = new StrategyConfig();
	config.setSiteConfig(site1name, new StrategySiteConfig(domain));
	config.setSiteConfig(site2name, new StrategySiteConfig(domain));
	return config;
    }

    private final static Architecture initState() {
	Architecture initState = new Architecture();
	initState
		.addPaaSSite(site1,
			new ArchitectureSite(Collections.singleton(new ArchitectureMicroservice(oldMsSite1Id, msName,
				oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes,
				msServices, memory, disk))));
	initState
		.addPaaSSite(site2,
			new ArchitectureSite(Collections.singleton(new ArchitectureMicroservice(oldMsSite2Id, msName,
				oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes,
				msServices, memory, disk))));
	return initState;
    }

    private final static Architecture finalState() {
	ArchitectureMicroservice newMs = new ArchitectureMicroservice(null, msName, null, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk);
	Architecture finalState = new Architecture();
	finalState.addPaaSSite(site1, new ArchitectureSite(Collections.singleton(newMs)));
	finalState.addPaaSSite(site2, new ArchitectureSite(Collections.singleton(newMs)));
	return finalState;
    }

    private final static Architecture midState1() {
	Set<ArchitectureMicroservice> site1Ms = new HashSet<>();
	site1Ms.add(new ArchitectureMicroservice(oldMsSite1Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(new ArchitectureMicroservice(null, msName, config.getUpdatingVersion(), newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Set<ArchitectureMicroservice> site2Ms = new HashSet<>();
	site2Ms.add(new ArchitectureMicroservice(oldMsSite2Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(new ArchitectureMicroservice(null, msName, config.getUpdatingVersion(), newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Architecture midState1 = new Architecture();
	midState1.addPaaSSite(site1, new ArchitectureSite(site1Ms));
	midState1.addPaaSSite(site2, new ArchitectureSite(site2Ms));
	return midState1;
    }

    private final static Architecture midState1Instantiated() {
	Set<ArchitectureMicroservice> site1Ms = new HashSet<>();
	site1Ms.add(new ArchitectureMicroservice(oldMsSite1Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(new ArchitectureMicroservice(newMsSite1Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Set<ArchitectureMicroservice> site2Ms = new HashSet<>();
	site2Ms.add(new ArchitectureMicroservice(oldMsSite2Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(new ArchitectureMicroservice(newMsSite2Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Architecture midState1 = new Architecture();
	midState1.addPaaSSite(site1, new ArchitectureSite(site1Ms));
	midState1.addPaaSSite(site2, new ArchitectureSite(site2Ms));
	return midState1;
    }

    private final static Architecture midState2() {
	Set<ArchitectureMicroservice> site1Ms = new HashSet<>();
	site1Ms.add(new ArchitectureMicroservice(oldMsSite1Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(new ArchitectureMicroservice(newMsSite1Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	Set<ArchitectureMicroservice> site2Ms = new HashSet<>();
	site2Ms.add(new ArchitectureMicroservice(oldMsSite2Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(new ArchitectureMicroservice(newMsSite2Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	Architecture midState2 = new Architecture();
	midState2.addPaaSSite(site1, new ArchitectureSite(site1Ms));
	midState2.addPaaSSite(site2, new ArchitectureSite(site2Ms));
	return midState2;
    }

    private final static Architecture midState3() {
	Architecture midState3 = new Architecture();
	midState3
		.addPaaSSite(site1,
			new ArchitectureSite(Collections.singleton(new ArchitectureMicroservice(newMsSite1Id, msName,
				newMsVersion, newMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes,
				msServices, memory, disk))));
	midState3
		.addPaaSSite(site2,
			new ArchitectureSite(Collections.singleton(new ArchitectureMicroservice(newMsSite2Id, msName,
				newMsVersion, newMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes,
				msServices, memory, disk))));
	return midState3;
    }

}
