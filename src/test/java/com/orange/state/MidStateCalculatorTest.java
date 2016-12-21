package com.orange.state;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.orange.midstate.MidStateCalculator;
import com.orange.model.DeploymentConfig;
import com.orange.model.PaaSAccessInfo;
import com.orange.model.PaaSSite;
import com.orange.model.SiteDeploymentConfig;
import com.orange.model.Strategy;
import com.orange.model.state.AppState;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.OverviewSite;
import com.orange.model.state.Route;

public class MidStateCalculatorTest {
	private static final String site1name = "site1";
	private static final String site2name = "site2";
	private static final String appName = "appName";
	private static final String appTmpName = appName + SiteDeploymentConfig.getDefaulttmpnamesuffix();
	private static final int appInstances = 10;
	private static final String appDomain = "orange.com";
	private static final Set<Route> appRoutes = Collections.singleton(new Route("app", appDomain));
	private static final Set<Route> appTmpRoutes = Collections
			.singleton(new Route(appName + SiteDeploymentConfig.getDefaulttmproutehostsuffix(), appDomain));
	private static final String oldAppSite1Id = "oldApp-guid-site1";
	private static final String oldAppSite2Id = "oldApp-guid-site2";
	private static final String newAppSite1Id = "newApp-guid-site1";
	private static final String newAppSite2Id = "newApp-guid-site2";
	private static final String newAppPath = "/app/path/app.zip";

	@Test
	public void should_get_blueGreenMidStates_withTmpRoute() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.BLUEGREEN, deploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(initState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		OverviewApp expectedNewApp1 = new OverviewApp(null, appTmpName, newAppPath, AppState.RUNNING, appInstances,
				newAppEnv(), appTmpRoutes);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(), expectedNewApp1);
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(), expectedNewApp1);

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2.getOverviewSites()).hasSize(2);
		assertThat(midState2.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(), new OverviewApp(
				newAppSite1Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState2.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(), new OverviewApp(
				newAppSite2Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState3 = midStateCalculator.calcMidStates(midState2, finalState());
		assertThat(midState3.getOverviewSites()).hasSize(2);
		assertThat(midState3.getOverviewSite(site1name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite1Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState3.getOverviewSite(site2name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite2Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState4 = midStateCalculator.calcMidStates(midState3, finalState());
		assertThat(midState4.getOverviewSites()).hasSize(2);
		assertThat(midState4.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite1Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState4.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite2Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState5 = midStateCalculator.calcMidStates(midState4, finalState());
		assertThat(midState5).isNull();
	}

	@Test
	public void should_get_blueGreenMidStates_withoutTmpRoute() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.BLUEGREEN, new DeploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(initState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		OverviewApp expectedNewApp = new OverviewApp(null, appTmpName, newAppPath, AppState.RUNNING, appInstances,
				newAppEnv(), appRoutes);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(), expectedNewApp);
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(), expectedNewApp);

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2.getOverviewSites()).hasSize(2);
		assertThat(midState2.getOverviewSite(site1name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite1Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState2.getOverviewSite(site2name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite2Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState3 = midStateCalculator.calcMidStates(midState2, finalState());
		assertThat(midState3.getOverviewSites()).hasSize(2);
		assertThat(midState3.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite1Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState3.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite2Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState4 = midStateCalculator.calcMidStates(midState3, finalState());
		assertThat(midState4).isNull();
	}

	@Test
	public void should_get_blueGreenMidStates_initDeploy() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.BLUEGREEN, new DeploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(emptyState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		OverviewApp expectedNewApp = new OverviewApp(null, appName, newAppPath, AppState.RUNNING, appInstances,
				newAppEnv(), appRoutes);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps()).containsOnly(expectedNewApp);
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps()).containsOnly(expectedNewApp);

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2).isNull();
	}

	@Test
	public void should_get_canaryMidStates_withTmpRoute() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.CANARY, deploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(initState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(),
				new OverviewApp(null, appTmpName, newAppPath, AppState.RUNNING, 1, newAppEnv(), appTmpRoutes));
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(),
				new OverviewApp(null, appTmpName, newAppPath, AppState.RUNNING, 1, newAppEnv(), appTmpRoutes));

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2.getOverviewSites()).hasSize(2);
		assertThat(midState2.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(),
				new OverviewApp(newAppSite1Id, appTmpName, null, AppState.RUNNING, 1, newAppEnv(), appRoutes));
		assertThat(midState2.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(),
				new OverviewApp(newAppSite2Id, appTmpName, null, AppState.RUNNING, 1, newAppEnv(), appRoutes));

		final Overview midState3 = midStateCalculator.calcMidStates(midState2, finalState());
		assertThat(midState3.getOverviewSites()).hasSize(2);
		assertThat(midState3.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(), new OverviewApp(
				newAppSite1Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState3.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(), new OverviewApp(
				newAppSite2Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState4 = midStateCalculator.calcMidStates(midState3, finalState());
		assertThat(midState4.getOverviewSites()).hasSize(2);
		assertThat(midState4.getOverviewSite(site1name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite1Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState4.getOverviewSite(site2name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite2Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState5 = midStateCalculator.calcMidStates(midState4, finalState());
		assertThat(midState5.getOverviewSites()).hasSize(2);
		assertThat(midState5.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite1Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState5.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite2Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState6 = midStateCalculator.calcMidStates(midState5, finalState());
		assertThat(midState6).isNull();
	}

	@Test
	public void should_get_canaryMidStates_withoutTmpRoute() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.CANARY, new DeploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(initState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(),
				new OverviewApp(null, appTmpName, newAppPath, AppState.RUNNING, 1, newAppEnv(), appRoutes));
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(),
				new OverviewApp(null, appTmpName, newAppPath, AppState.RUNNING, 1, newAppEnv(), appRoutes));

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2.getOverviewSites()).hasSize(2);
		assertThat(midState2.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(), new OverviewApp(
				newAppSite1Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState2.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(), new OverviewApp(
				newAppSite2Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState3 = midStateCalculator.calcMidStates(midState2, finalState());
		assertThat(midState3.getOverviewSites()).hasSize(2);
		assertThat(midState3.getOverviewSite(site1name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite1Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState3.getOverviewSite(site2name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite2Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState4 = midStateCalculator.calcMidStates(midState3, finalState());
		assertThat(midState4.getOverviewSites()).hasSize(2);
		assertThat(midState4.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite1Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState4.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite2Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState5 = midStateCalculator.calcMidStates(midState4, finalState());
		assertThat(midState5).isNull();
	}

	@Test
	public void should_get_canaryMidStates_initDeploy() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.CANARY, new DeploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(emptyState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps())
				.containsOnly(new OverviewApp(null, appName, newAppPath, AppState.RUNNING, 1, newAppEnv(), appRoutes));
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps())
				.containsOnly(new OverviewApp(null, appName, newAppPath, AppState.RUNNING, 1, newAppEnv(), appRoutes));

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2.getOverviewSites()).hasSize(2);
		assertThat(midState2.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite1Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState2.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite2Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState3 = midStateCalculator.calcMidStates(midState2, finalState());
		assertThat(midState3).isNull();
	}

	@Test
	public void should_get_stopRestartMidStates_withTmpRoute() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.STOPRESTART, deploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(initState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(), new OverviewApp(
				null, appTmpName, newAppPath, AppState.CREATED, appInstances, newAppEnv(), appTmpRoutes));
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(), new OverviewApp(
				null, appTmpName, newAppPath, AppState.CREATED, appInstances, newAppEnv(), appTmpRoutes));

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2.getOverviewSites()).hasSize(2);
		assertThat(midState2.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite1Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite1Id, appTmpName, null, AppState.CREATED, appInstances, newAppEnv(),
						appTmpRoutes));
		assertThat(midState2.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite2Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite2Id, appTmpName, null, AppState.CREATED, appInstances, newAppEnv(),
						appTmpRoutes));

		assertThat(mockInstantiateState(midState2)).isEqualTo(midState2);
		final Overview midState3 = midStateCalculator.calcMidStates(midState2, finalState());
		assertThat(midState3.getOverviewSites()).hasSize(2);
		assertThat(midState3.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite1Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite1Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(),
						appTmpRoutes));
		assertThat(midState3.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite2Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite2Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(),
						appTmpRoutes));

		final Overview midState4 = midStateCalculator.calcMidStates(midState3, finalState());
		assertThat(midState4.getOverviewSites()).hasSize(2);
		assertThat(midState4.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite1Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite1Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(),
						appRoutes));
		assertThat(midState4.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite2Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite2Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(),
						appRoutes));

		final Overview midState5 = midStateCalculator.calcMidStates(midState4, finalState());
		assertThat(midState5.getOverviewSites()).hasSize(2);
		assertThat(midState5.getOverviewSite(site1name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite1Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState5.getOverviewSite(site2name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite2Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState6 = midStateCalculator.calcMidStates(midState5, finalState());
		assertThat(midState6.getOverviewSites()).hasSize(2);
		assertThat(midState6.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite1Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState6.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite2Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState7 = midStateCalculator.calcMidStates(midState6, finalState());
		assertThat(midState7).isNull();
	}

	@Test
	public void should_get_stopRestartMidStates_withoutTmpRoute() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.STOPRESTART, new DeploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(initState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps()).containsOnly(oldAppSite1(),
				new OverviewApp(null, appTmpName, newAppPath, AppState.CREATED, appInstances, newAppEnv(), appRoutes));
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps()).containsOnly(oldAppSite2(),
				new OverviewApp(null, appTmpName, newAppPath, AppState.CREATED, appInstances, newAppEnv(), appRoutes));

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2.getOverviewSites()).hasSize(2);
		assertThat(midState2.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite1Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite1Id, appTmpName, null, AppState.CREATED, appInstances, newAppEnv(),
						appRoutes));
		assertThat(midState2.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite2Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite2Id, appTmpName, null, AppState.CREATED, appInstances, newAppEnv(),
						appRoutes));

		final Overview midState3 = midStateCalculator.calcMidStates(midState2, finalState());
		assertThat(midState3.getOverviewSites()).hasSize(2);
		assertThat(midState3.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite1Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite1Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(),
						appRoutes));
		assertThat(midState3.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(oldAppSite2Id, appName, null, AppState.STAGED, appInstances, oldAppEnv(), appRoutes),
				new OverviewApp(newAppSite2Id, appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(),
						appRoutes));

		final Overview midState4 = midStateCalculator.calcMidStates(midState3, finalState());
		assertThat(midState4.getOverviewSites()).hasSize(2);
		assertThat(midState4.getOverviewSite(site1name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite1Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState4.getOverviewSite(site2name).getOverviewApps()).containsOnly(new OverviewApp(newAppSite2Id,
				appTmpName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState5 = midStateCalculator.calcMidStates(midState4, finalState());
		assertThat(midState5.getOverviewSites()).hasSize(2);
		assertThat(midState5.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite1Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState5.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(newAppSite2Id, appName, null, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState6 = midStateCalculator.calcMidStates(midState5, finalState());
		assertThat(midState6).isNull();
	}

	@Test
	public void should_get_stopRestartMidStates_initDeploy() {
		MidStateCalculator midStateCalculator = new MidStateCalculator(Strategy.STOPRESTART, new DeploymentConfig());
		final Overview midState1 = midStateCalculator.calcMidStates(emptyState(), finalState());
		assertThat(midState1.getOverviewSites()).hasSize(2);
		assertThat(midState1.getOverviewSite(site1name).getOverviewApps()).containsOnly(
				new OverviewApp(null, appName, newAppPath, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));
		assertThat(midState1.getOverviewSite(site2name).getOverviewApps()).containsOnly(
				new OverviewApp(null, appName, newAppPath, AppState.RUNNING, appInstances, newAppEnv(), appRoutes));

		final Overview midState2 = midStateCalculator.calcMidStates(mockInstantiateState(midState1), finalState());
		assertThat(midState2).isNull();
	}

	private Overview mockInstantiateState(final Overview abstractState) {
		Overview instantiateState = new Overview(abstractState);
		instantiateState.getOverviewSite(site1name).getOverviewApps().stream()
				.forEach(app -> mockInstantiateApp(app, newAppSite1Id));
		instantiateState.getOverviewSite(site2name).getOverviewApps().stream()
				.forEach(app -> mockInstantiateApp(app, newAppSite2Id));
		return instantiateState;
	}

	private void mockInstantiateApp(OverviewApp abstractApp, String appId) {
		if (abstractApp.getGuid() == null) {
			abstractApp.setGuid(appId);
			abstractApp.setPath(null);
		}
	}

	private DeploymentConfig deploymentConfig() {
		DeploymentConfig deploymentConfig = new DeploymentConfig();
		deploymentConfig.getSiteDeploymentConfig(site1name).setTmpRouteDomain(appDomain);
		deploymentConfig.getSiteDeploymentConfig(site2name).setTmpRouteDomain(appDomain);
		return deploymentConfig;
	}

	private Overview emptyState() {
		Overview initState = new Overview();
		initState.addPaaSSite(site1(), new OverviewSite());
		initState.addPaaSSite(site2(), new OverviewSite());
		return initState;
	}

	private Overview initState() {
		Overview initState = new Overview();
		initState.addPaaSSite(site1(), new OverviewSite(Collections.singleton(oldAppSite1())));
		initState.addPaaSSite(site2(), new OverviewSite(Collections.singleton(new OverviewApp(oldAppSite2Id, appName,
				null, AppState.RUNNING, appInstances, oldAppEnv(), appRoutes))));
		return initState;
	}

	private Overview finalState() {
		Overview finalState = new Overview();
		finalState.addPaaSSite(site1(), new OverviewSite(Collections.singleton(
				new OverviewApp(null, appName, newAppPath, AppState.RUNNING, appInstances, newAppEnv(), appRoutes))));
		finalState.addPaaSSite(site2(), new OverviewSite(Collections.singleton(
				new OverviewApp(null, appName, newAppPath, AppState.RUNNING, appInstances, newAppEnv(), appRoutes))));
		return finalState;
	}

	private OverviewApp oldAppSite1() {
		return new OverviewApp(oldAppSite1Id, appName, null, AppState.RUNNING, appInstances, oldAppEnv(), appRoutes);
	}

	private OverviewApp oldAppSite2() {
		return new OverviewApp(oldAppSite2Id, appName, null, AppState.RUNNING, appInstances, oldAppEnv(), appRoutes);
	}

	private PaaSSite site1() {
		return new PaaSSite(site1name, new PaaSAccessInfo(site1name, "site1-api", "site1-user", "site1-pwd",
				"site1-org", "site1-space", true));
	}

	private PaaSSite site2() {
		return new PaaSSite(site2name, new PaaSAccessInfo(site2name, "site2-api", "site2-user", "site2-pwd",
				"site2-org", "site2-space", true));
	}

	private Map<String, String> oldAppEnv() {
		Map<String, String> env = new HashMap<>();
		env.put("APP_VERSION", "1.0.0");
		return env;
	}

	private Map<String, String> newAppEnv() {
		Map<String, String> env = new HashMap<>();
		env.put("APP_VERSION", "1.0.1");
		return env;
	}
}
