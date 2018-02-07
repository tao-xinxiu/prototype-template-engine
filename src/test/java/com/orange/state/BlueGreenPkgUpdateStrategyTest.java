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
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.ArchitectureSite;
import com.orange.model.architecture.MicroserviceState;
import com.orange.model.architecture.Route;
import com.orange.strategy.Strategy;
import com.orange.strategy.impl.BlueGreenPkgUpdateStrategy;

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
    private final static Architecture initArchitecture = initArchitecture();
    private final static Architecture finalArchitecture = finalArchitecture();

    @Test
    public void should_get_expected_midArchitecture1() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Assert.assertFalse(initArchitecture.isInstantiation(finalArchitecture));
	Assert.assertTrue(bgStrategy.valid(initArchitecture, finalArchitecture));
	// Assert.assertTrue(bgStrategy.transits().get(0).condition(initArchitecture,
	// finalArchitecture));
	Assert.assertEquals(bgStrategy.getTransitions().get(0).next(initArchitecture, finalArchitecture), midArchitecture1());
    }

    @Test
    public void should_get_expected_midArchitecture2() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Architecture currentArchitecture = midArchitecture1Instantiated();
	Assert.assertFalse(currentArchitecture.isInstantiation(finalArchitecture));
	Assert.assertTrue(bgStrategy.valid(currentArchitecture, finalArchitecture));
	// Assert.assertFalse(bgStrategy.transits().get(0).condition(currentArchitecture,
	// finalArchitecture));
	// Assert.assertTrue(bgStrategy.transits().get(1).condition(currentArchitecture,
	// finalArchitecture));
	Assert.assertEquals(midArchitecture2(), bgStrategy.getTransitions().get(1).next(currentArchitecture, finalArchitecture));
    }

    @Test
    public void should_get_expected_midArchitecture3() {
	Strategy bgStrategy = new BlueGreenPkgUpdateStrategy(config);
	Architecture currentArchitecture = midArchitecture2();
	Assert.assertFalse(currentArchitecture.isInstantiation(finalArchitecture));
	Assert.assertTrue(bgStrategy.valid(currentArchitecture, finalArchitecture));
	// Assert.assertFalse(bgStrategy.transits().get(0).condition(currentArchitecture,
	// finalArchitecture));
	// Assert.assertFalse(bgStrategy.transits().get(1).condition(currentArchitecture,
	// finalArchitecture));
	// Assert.assertTrue(bgStrategy.transits().get(2).condition(currentArchitecture,
	// finalArchitecture));
	Architecture midArchitecture3 = bgStrategy.getTransitions().get(2).next(currentArchitecture, finalArchitecture);
	Assert.assertEquals(midArchitecture3(), midArchitecture3);
	Assert.assertTrue(bgStrategy.valid(midArchitecture3, finalArchitecture));
    }

    private final static StrategyConfig config() {
	StrategyConfig config = new StrategyConfig();
	config.setSiteConfig(site1name, new StrategySiteConfig(domain));
	config.setSiteConfig(site2name, new StrategySiteConfig(domain));
	return config;
    }

    private final static Architecture initArchitecture() {
	Architecture initArchitecture = new Architecture();
	initArchitecture
		.addPaaSSite(site1,
			new ArchitectureSite(Collections.singleton(new Microservice(oldMsSite1Id, msName,
				oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes,
				msServices, memory, disk))));
	initArchitecture
		.addPaaSSite(site2,
			new ArchitectureSite(Collections.singleton(new Microservice(oldMsSite2Id, msName,
				oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes,
				msServices, memory, disk))));
	return initArchitecture;
    }

    private final static Architecture finalArchitecture() {
	Microservice newMs = new Microservice(null, msName, null, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk);
	Architecture finalArchitecture = new Architecture();
	finalArchitecture.addPaaSSite(site1, new ArchitectureSite(Collections.singleton(newMs)));
	finalArchitecture.addPaaSSite(site2, new ArchitectureSite(Collections.singleton(newMs)));
	return finalArchitecture;
    }

    private final static Architecture midArchitecture1() {
	Set<Microservice> site1Ms = new HashSet<>();
	site1Ms.add(new Microservice(oldMsSite1Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(new Microservice(null, msName, config.getUpdatingVersion(), newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Set<Microservice> site2Ms = new HashSet<>();
	site2Ms.add(new Microservice(oldMsSite2Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(new Microservice(null, msName, config.getUpdatingVersion(), newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Architecture midArchitecture1 = new Architecture();
	midArchitecture1.addPaaSSite(site1, new ArchitectureSite(site1Ms));
	midArchitecture1.addPaaSSite(site2, new ArchitectureSite(site2Ms));
	return midArchitecture1;
    }

    private final static Architecture midArchitecture1Instantiated() {
	Set<Microservice> site1Ms = new HashSet<>();
	site1Ms.add(new Microservice(oldMsSite1Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(new Microservice(newMsSite1Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Set<Microservice> site2Ms = new HashSet<>();
	site2Ms.add(new Microservice(oldMsSite2Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(new Microservice(newMsSite2Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Architecture instantiatedArchitecture1 = new Architecture();
	instantiatedArchitecture1.addPaaSSite(site1, new ArchitectureSite(site1Ms));
	instantiatedArchitecture1.addPaaSSite(site2, new ArchitectureSite(site2Ms));
	return instantiatedArchitecture1;
    }

    private final static Architecture midArchitecture2() {
	Set<Microservice> site1Ms = new HashSet<>();
	site1Ms.add(new Microservice(oldMsSite1Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(new Microservice(newMsSite1Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	Set<Microservice> site2Ms = new HashSet<>();
	site2Ms.add(new Microservice(oldMsSite2Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(new Microservice(newMsSite2Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	Architecture midArchitecture2 = new Architecture();
	midArchitecture2.addPaaSSite(site1, new ArchitectureSite(site1Ms));
	midArchitecture2.addPaaSSite(site2, new ArchitectureSite(site2Ms));
	return midArchitecture2;
    }

    private final static Architecture midArchitecture3() {
	Architecture midArchitecture3 = new Architecture();
	midArchitecture3
		.addPaaSSite(site1,
			new ArchitectureSite(Collections.singleton(new Microservice(newMsSite1Id, msName,
				newMsVersion, newMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes,
				msServices, memory, disk))));
	midArchitecture3
		.addPaaSSite(site2,
			new ArchitectureSite(Collections.singleton(new Microservice(newMsSite2Id, msName,
				newMsVersion, newMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes,
				msServices, memory, disk))));
	return midArchitecture3;
    }

}
