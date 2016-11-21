package state;

import java.util.Arrays;

import org.fest.assertions.Assertions;
import org.junit.Test;

import com.orange.model.DropletState;
import com.orange.model.Overview;
import com.orange.model.OverviewApp;
import com.orange.model.OverviewDroplet;
import com.orange.model.PaaSSite;
import com.orange.state.Comparator;

public class ComparatorTest {
	private static final String siteDev = "dev";
	private static final String siteProd = "prod";
	private static final String appGuidDev = "guid-dev-app-hello-v1.0.1";
	private static final String appGuidProd = "guid-prod-app-hello-v1.0.0";
	private static final String appName = "hello";
	private static final String appVersion1 = "1.0.0";
	private static final String appVersion2 = "1.0.1";
	private static final String appRouteGlobal = "global.route.app.hello";
	private static final String appRouteDev = "dev.route.app.hello";
	private static final String appRouteProd = "prod.route.app.hello";
	private static final String dropletGuidDev = "guid-dev-droplet-hello-v1.0.1";
	private static final String dropletGuidProd1 = "guid-prod-droplet-hello-v1.0.0";
	private static final String sitePrepod = "preprod";

	@Test
	public void should_valide_with_same_sites_in_two_states() {
		Comparator comparator = new Comparator(currentState(), desiredState());
		Assertions.assertThat(comparator.valide()).isTrue();
	}
	
	@Test
	public void should_not_valide_with_different_sites_in_two_states() {
		Comparator comparator = new Comparator(currentState(), erroneousDesiredState());
		Assertions.assertThat(comparator.valide()).isFalse();
	}
	
	@Test
	public void should_get_added_apps() {
		Comparator comparator = new Comparator(currentState(), desiredState());
		Assertions.assertThat(comparator.valide()).isTrue();
		Assertions.assertThat(comparator.getAddedApp(dev())).isEmpty();
		Assertions.assertThat(comparator.getAddedApp(prod())).isEqualTo(Arrays.asList(prodNewApp()));
	}
	
	@Test
	public void should_get_removed_apps() {
		Comparator comparator = new Comparator(currentState(), desiredState());
		Assertions.assertThat(comparator.valide()).isTrue();
		Assertions.assertThat(comparator.getRemovedApp(dev())).isEmpty();
		Assertions.assertThat(comparator.getRemovedApp(prod())).isEqualTo(Arrays.asList(prodOldApp()));
	}

	private PaaSSite dev() {
		PaaSSite devSite = new PaaSSite();
		devSite.setName(siteDev);
		return devSite;
	}

	private PaaSSite prod() {
		PaaSSite prodSite = new PaaSSite();
		prodSite.setName(siteProd);
		return prodSite;
	}
	
	private PaaSSite preprod() {
		PaaSSite prodSite = new PaaSSite();
		prodSite.setName(sitePrepod);
		return prodSite;
	}

	private Overview currentState() {
		Overview currentState = new Overview();
		currentState.addPaaSSite(dev(), Arrays.asList(devApp()));
		currentState.addPaaSSite(prod(), Arrays.asList(prodOldApp()));
		return currentState;
	}

	private Overview desiredState() {
		Overview desiredState = new Overview();
		desiredState.addPaaSSite(dev(), Arrays.asList(devApp()));
		desiredState.addPaaSSite(prod(), Arrays.asList(prodNewApp()));
		return desiredState;
	}
	
	private Overview erroneousDesiredState() {
		Overview desiredState = new Overview();
		desiredState.addPaaSSite(dev(), Arrays.asList(devApp()));
		desiredState.addPaaSSite(preprod(), Arrays.asList(prodNewApp()));
		return desiredState;
	}
	
	private OverviewApp devApp() {
		OverviewDroplet dropletDev = new OverviewDroplet(dropletGuidDev, appVersion2, DropletState.RUNNING);
		return new OverviewApp(appGuidDev, appName, Arrays.asList(appRouteGlobal, appRouteDev),
				Arrays.asList(dropletDev));
	}
	
	private OverviewApp prodOldApp() {
		OverviewDroplet dropletProdOld = new OverviewDroplet(dropletGuidProd1, appVersion1, DropletState.RUNNING);
		return new OverviewApp(appGuidProd, appName, Arrays.asList(appRouteGlobal, appRouteProd),
				Arrays.asList(dropletProdOld));
	}
	
	private OverviewApp prodNewApp() {
		OverviewDroplet dropletProdNew = new OverviewDroplet(null, appVersion2, DropletState.RUNNING);
		return new OverviewApp(null, appName, Arrays.asList(appRouteGlobal, appRouteProd),
				Arrays.asList(dropletProdNew));
	}
}
