package com.orange.strategy.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.orange.model.StrategyConfig;
import com.orange.model.PaaSSiteAccess;
import com.orange.model.StrategySiteConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.MicroserviceState;
import com.orange.strategy.Strategy;
import com.orange.strategy.impl.BlueGreenStrategy;

public class BlueGreenStrategyTest {
    private static final String site1name = "site1";
    private static final String site2name = "site2";
    private static final String msName = "msName";
    private static final String oldMsVersion = "v1.0.0";
    private static final String newMsVersion = "v1.1.0";
    private static final int msNbProcesses = 3;
    private static final String domain = "orange.com";
    private static final Map<String, String> msEnv = new HashMap<>(Collections.singletonMap("foo", "bar"));
    private static final Set<String> msRoutes = Collections.singleton("test." + domain);
    private static final Set<String> msTmpRoutes = Collections
	    .singleton(msName + StrategySiteConfig.getDefaulttmproutehostsuffix() + "." + domain);
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

    private static final PaaSSiteAccess site1 = new PaaSSiteAccess(site1name, "CloudFoundry", "site1-api", "site1-user",
	    "site1-pwd", "site1-org", "site1-space", true);
    private static final PaaSSiteAccess site2 = new PaaSSiteAccess(site2name, "CloudFoundry", "site2-api", "site2-user",
	    "site2-pwd", "site2-org", "site2-space", true);
    private final static Architecture initArchitecture = initArchitecture();
    private final static Architecture finalArchitecture = finalArchitecture();

    @Test
    public void should_get_expected_midArchitecture1() {
	Strategy bgStrategy = new BlueGreenStrategy(config);
	Assert.assertFalse(initArchitecture.isInstantiation(finalArchitecture));
	Assert.assertTrue(bgStrategy.valid(initArchitecture, finalArchitecture));
	Assert.assertEquals(midArchitecture1(),
		bgStrategy.getTransitions().get(0).next(initArchitecture, finalArchitecture));
    }

    @Test
    public void should_get_expected_midArchitecture2() {
	Strategy bgStrategy = new BlueGreenStrategy(config);
	Architecture currentArchitecture = midArchitecture1Instantiated();
	Assert.assertFalse(currentArchitecture.isInstantiation(finalArchitecture));
	Assert.assertTrue(bgStrategy.valid(currentArchitecture, finalArchitecture));
	Assert.assertEquals(currentArchitecture,
		bgStrategy.getTransitions().get(0).next(currentArchitecture, finalArchitecture));
	Assert.assertEquals(currentArchitecture,
		bgStrategy.getTransitions().get(1).next(currentArchitecture, finalArchitecture));
	Assert.assertEquals(midArchitecture2(),
		bgStrategy.getTransitions().get(2).next(currentArchitecture, finalArchitecture));
    }

    @Test
    public void should_get_expected_midArchitecture3() {
	Strategy bgStrategy = new BlueGreenStrategy(config);
	Architecture currentArchitecture = midArchitecture2();
	Assert.assertFalse(currentArchitecture.isInstantiation(finalArchitecture));
	Assert.assertTrue(bgStrategy.valid(currentArchitecture, finalArchitecture));
	Assert.assertEquals(currentArchitecture,
		bgStrategy.getTransitions().get(0).next(currentArchitecture, finalArchitecture));
	Assert.assertEquals(currentArchitecture,
		bgStrategy.getTransitions().get(1).next(currentArchitecture, finalArchitecture));
	Assert.assertEquals(currentArchitecture,
		bgStrategy.getTransitions().get(2).next(currentArchitecture, finalArchitecture));
	Architecture midArchitecture3 = bgStrategy.getTransitions().get(3).next(currentArchitecture, finalArchitecture);
	Assert.assertTrue(midArchitecture3.isInstantiation(midArchitecture3()));
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
	initArchitecture.addSite(site1, Collections.singleton(constructMs(oldMsSite1Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk)));
	initArchitecture.addSite(site2, Collections.singleton(constructMs(oldMsSite2Id, msName, oldMsVersion, oldMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk)));
	return initArchitecture;
    }

    private final static Architecture finalArchitecture() {
	Microservice newMs = constructMs(null, msName, newMsVersion, newMsPath, MicroserviceState.RUNNING, msNbProcesses, msEnv,
		msRoutes, msServices, memory, disk);
	Architecture finalArchitecture = new Architecture();
	finalArchitecture.addSite(site1, Collections.singleton(newMs));
	finalArchitecture.addSite(site2, Collections.singleton(newMs));
	return finalArchitecture;
    }

    private final static Architecture midArchitecture1() {
	Set<Microservice> site1Ms = new HashSet<>();
	site1Ms.add(constructMs(oldMsSite1Id, msName, oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses,
		msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(constructMs(null, msName, newMsVersion, newMsPath, MicroserviceState.RUNNING,
		msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Set<Microservice> site2Ms = new HashSet<>();
	site2Ms.add(constructMs(oldMsSite2Id, msName, oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses,
		msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(constructMs(null, msName, newMsVersion, newMsPath, MicroserviceState.RUNNING,
		msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Architecture midArchitecture1 = new Architecture();
	midArchitecture1.addSite(site1, site1Ms);
	midArchitecture1.addSite(site2, site2Ms);
	return midArchitecture1;
    }

    private final static Architecture midArchitecture1Instantiated() {
	Set<Microservice> site1Ms = new HashSet<>();
	site1Ms.add(constructMs(oldMsSite1Id, msName, oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses,
		msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(constructMs(newMsSite1Id, msName, newMsVersion, newMsPath, MicroserviceState.RUNNING,
		msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Set<Microservice> site2Ms = new HashSet<>();
	site2Ms.add(constructMs(oldMsSite2Id, msName, oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses,
		msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(constructMs(newMsSite2Id, msName, newMsVersion, newMsPath, MicroserviceState.RUNNING,
		msNbProcesses, msEnv, msTmpRoutes, msServices, memory, disk));
	Architecture instantiatedArchitecture1 = new Architecture();
	instantiatedArchitecture1.addSite(site1, site1Ms);
	instantiatedArchitecture1.addSite(site2, site2Ms);
	return instantiatedArchitecture1;
    }

    private final static Architecture midArchitecture2() {
	Set<Microservice> site1Ms = new HashSet<>();
	site1Ms.add(constructMs(oldMsSite1Id, msName, oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses,
		msEnv, msRoutes, msServices, memory, disk));
	site1Ms.add(constructMs(newMsSite1Id, msName, newMsVersion, newMsPath, MicroserviceState.RUNNING,
		msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	Set<Microservice> site2Ms = new HashSet<>();
	site2Ms.add(constructMs(oldMsSite2Id, msName, oldMsVersion, oldMsPath, MicroserviceState.RUNNING, msNbProcesses,
		msEnv, msRoutes, msServices, memory, disk));
	site2Ms.add(constructMs(newMsSite2Id, msName, newMsVersion, newMsPath, MicroserviceState.RUNNING,
		msNbProcesses, msEnv, msRoutes, msServices, memory, disk));
	Architecture midArchitecture2 = new Architecture();
	midArchitecture2.addSite(site1, site1Ms);
	midArchitecture2.addSite(site2, site2Ms);
	return midArchitecture2;
    }

    private final static Architecture midArchitecture3() {
	Architecture midArchitecture3 = new Architecture();
	midArchitecture3.addSite(site1, Collections.singleton(constructMs(newMsSite1Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk)));
	midArchitecture3.addSite(site2, Collections.singleton(constructMs(newMsSite2Id, msName, newMsVersion, newMsPath,
		MicroserviceState.RUNNING, msNbProcesses, msEnv, msRoutes, msServices, memory, disk)));
	return midArchitecture3;
    }

    private static Microservice constructMs(String guid, String name, String version, String path,
	    MicroserviceState state, int nbProcesses, Map<String, String> env, Set<String> routes, Set<String> services,
	    String memory, String disk) {
	Map<String, Object> attributes = new HashMap<>();
	attributes.put("guid", guid);
	attributes.put("name", name);
	attributes.put("version", version);
	attributes.put("path", path);
	attributes.put("state", state);
	attributes.put("nbProcesses", nbProcesses);
	attributes.put("env", env);
	attributes.put("routes", routes);
	attributes.put("services", services);
	attributes.put("memory", memory);
	attributes.put("disk", disk);
	return new Microservice(attributes);
    }
}
