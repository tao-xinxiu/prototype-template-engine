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

import com.orange.model.DeploymentConfig;
import com.orange.model.Overview;
import com.orange.model.OverviewApp;
import com.orange.model.OverviewSite;
import com.orange.model.PaaSSite;
import com.orange.model.Requirement;
import com.orange.paas.PaaSAPI;
import com.orange.paas.cf.CloudFoundryAPI;
import com.orange.state.AppComparator;
import com.orange.state.SiteComparator;
import com.orange.workflow.ParallelWorkflow;
import com.orange.workflow.SerialWorkflow;
import com.orange.workflow.StepCalculator;
import com.orange.workflow.Workflow;
import com.orange.workflow.WorkflowCalculator;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class })
@RestController
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	static DeploymentConfig desiredState;
	static WorkflowCalculator workflowCalculator;
	static Collection<PaaSSite> managingSites;

	@RequestMapping(value = "/set", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody String setDesiredState(@RequestBody DeploymentConfig desiredState) {
		for (Map.Entry<String, PaaSSite> entry : desiredState.getSites().entrySet()) {
			PaaSSite site = entry.getValue();
			site.setName(entry.getKey());
			if (!site.valid()) {
				throw new IllegalStateException(
						"DeploymentConfig not valid, missing mandatory property for the PaaS site: " + site);
			}
			site.getAccessInfo().setName(entry.getKey());
		}
		logger.info("DeploymentConfig sites valid");
		if (!desiredState.getApp().valid()) {
			throw new IllegalStateException("DeploymentConfig not valid, missing mandatory property for app: "
					+ desiredState.getApp().getName());
		}
		logger.info("DeploymentConfig app valid");
		Main.desiredState = desiredState;
		if (managingSites == null) {
			managingSites = desiredState.getSites().values();
		}
		return "\n OK! \n";
	}

	@RequestMapping(value = "/update", method = RequestMethod.PUT)
	public @ResponseBody String update(@RequestBody String require) throws InterruptedException {
		Requirement requirement = Requirement.valueOf(require.toUpperCase());
		if (desiredState == null) {
			throw new IllegalStateException("desiredState not configured!");
		}
		workflowCalculator = new WorkflowCalculator(requirement, desiredState, managingSites);
		Workflow workflow = workflowCalculator.getUpdateWorkflow();
		workflow.exec();
		logger.info("Workflow {} finished!", workflow);
		return "\n OK! \n";
	}

	@RequestMapping(value = "/commit", method = RequestMethod.PUT)
	public @ResponseBody String commit() {
		if (workflowCalculator == null) {
			throw new IllegalStateException("update should be called before commit!");
		}
		Workflow workflow = workflowCalculator.getCommitWorkflow();
		workflow.exec();
		logger.info("Workflow {} finished!", workflow);
		managingSites = desiredState.getSites().values();
		return "\n OK! \n";
	}

	@RequestMapping(value = "/rollback", method = RequestMethod.PUT)
	public @ResponseBody String rollback() {
		if (workflowCalculator == null) {
			throw new IllegalStateException("update should be called before commit!");
		}
		Workflow workflow = workflowCalculator.getRollbackWorkflow();
		workflow.exec();
		logger.info("Workflow {} finished!", workflow);
		return "\n OK! \n";
	}

	@RequestMapping(value = "/current_state", method = RequestMethod.PUT)
	public @ResponseBody Overview getCurrentState(@RequestBody Collection<PaaSSite> managingSites) {
		Main.managingSites = managingSites;
		return getCurrentState();
	}

	@RequestMapping(value = "/change", method = RequestMethod.PUT)
	public @ResponseBody Overview change(@RequestBody Overview desiredState) {
		Overview currentState = getCurrentState();
		if (!currentState.listPaaSSites().equals(desiredState.listPaaSSites())) {
			throw new IllegalStateException("Get current_state for all the sites to be managed first.");
		}
		Workflow updateSites = new ParallelWorkflow("parallel update sites");
		for (PaaSSite site : managingSites) {
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

				updateSite.addStep(updateApp);
			}
			updateSites.addStep(updateSite);
		}
		updateSites.exec();
		logger.info("Workflow {} finished!", updateSites);
		return getCurrentState();
	}

	private Overview getCurrentState() {
		Map<String, PaaSSite> sites = managingSites.stream()
				.collect(Collectors.toMap(site -> site.getName(), site -> site));
		Map<String, OverviewSite> overviewSites = managingSites.parallelStream()
				.collect(Collectors.toMap(site -> site.getName(), site -> new CloudFoundryAPI(site).getOverviewSite()));
		logger.info("Got current state!");
		return new Overview(sites, overviewSites);
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}