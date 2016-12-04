package com.orange;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.orange.model.Overview;
import com.orange.model.OverviewApp;
import com.orange.model.OverviewSite;
import com.orange.model.PaaSSite;
import com.orange.paas.PaaSAPI;
import com.orange.paas.cf.CloudFoundryAPI;
import com.orange.state.AppComparator;
import com.orange.state.SiteComparator;
import com.orange.workflow.ParallelWorkflow;
import com.orange.workflow.SerialWorkflow;
import com.orange.workflow.StepCalculator;
import com.orange.workflow.Workflow;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class })
@RestController
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	@RequestMapping(value = "/current_state", method = RequestMethod.PUT)
	public @ResponseBody Overview getCurrentState(@RequestBody Collection<PaaSSite> managingSites) {
		Map<String, PaaSSite> sites = managingSites.stream()
				.collect(Collectors.toMap(site -> site.getName(), site -> site));
		Map<String, OverviewSite> overviewSites = managingSites.parallelStream()
				.collect(Collectors.toMap(site -> site.getName(), site -> new CloudFoundryAPI(site).getOverviewSite()));
		logger.info("Got current state!");
		return new Overview(sites, overviewSites);
	}

	@RequestMapping(value = "/change", method = RequestMethod.PUT)
	public @ResponseBody Overview change(@RequestBody Overview desiredState) {
		Overview currentState = getCurrentState(desiredState.listPaaSSites());
		validDesiredState(currentState, desiredState);
		Workflow updateSites = new ParallelWorkflow("parallel update sites");
		for (PaaSSite site : desiredState.listPaaSSites()) {
			Workflow updateSite = new ParallelWorkflow(
					String.format("parallel update site %s entities", site.getName()));
			SiteComparator comparator = new SiteComparator(currentState.getOverviewSite(site.getName()),
					desiredState.getOverviewSite(site.getName()));
			PaaSAPI api = new CloudFoundryAPI(site);
			for (OverviewApp app : comparator.getAddedApp()) {
				updateSite.addStep(StepCalculator.addApp(api, app));
			}
			for (OverviewApp app : comparator.getRemovedApp()) {
				updateSite.addStep(StepCalculator.removeApp(api, app));
			}
			for (AppComparator appComparator : comparator.getAppComparators()) {
				Workflow updateApp = new SerialWorkflow(String.format("serial update app from %s to %s at site %s",
						appComparator.getCurrentApp(), appComparator.getDesiredApp(), site.getName()));
				String appId = appComparator.getCurrentApp().getGuid();
				if (appComparator.isNameUpdated()) {
					updateApp.addStep(StepCalculator.updateAppName(api, appComparator.getDesiredApp()));
				}
				if (appComparator.isRoutesUpdated()) {
					updateApp.addStep(StepCalculator.addAppRoutes(api, appId, appComparator.getAddedRoutes()));
					updateApp.addStep(StepCalculator.removeAppRoutes(api, appId, appComparator.getRemovedRoutes()));
				}
				updateApp.addStep(StepCalculator.addDroplets(api, appComparator.getDesiredApp(),
						appComparator.getAddedDroplets()));
				updateApp.addStep(StepCalculator.removeDroplets(api, appComparator.getCurrentApp(),
						appComparator.getRemovedDroplets()));
				if (appComparator.isCurrentDropletUpdated()) {
					updateApp.addStep(
							StepCalculator.updateCurrentDroplet(api, appId, appComparator.getDesiredCurrentDroplet()));
				}
				if (appComparator.isAppStoped()) {
					updateApp.addStep(StepCalculator.stopApp(api, appId));
				}
				if (appComparator.isAppInstancesUpdated()) {
					updateApp.addStep(StepCalculator.scaleApp(api, appId, appComparator.getDesiredApp().runningDroplet().getInstances()));
				}
				updateSite.addStep(updateApp);
			}
			updateSites.addStep(updateSite);
		}
		updateSites.exec();
		logger.info("Workflow {} finished!", updateSites);
		return getCurrentState(desiredState.listPaaSSites());
	}

	private void validDesiredState(Overview currentState, Overview desiredState) {
		if (desiredState.getOverviewSites().values().stream()
				.anyMatch(site -> site.getOverviewApps().stream().anyMatch(app -> app.getDroplets().stream()
						.anyMatch(droplet -> droplet.getGuid() == null && droplet.getPath() == null)))) {
			throw new IllegalStateException("The path of all new droplets should be specified.");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}