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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.orange.model.DeploymentConfig;
import com.orange.model.Overview;
import com.orange.model.OverviewSite;
import com.orange.model.PaaSSite;
import com.orange.model.Strategy;
import com.orange.paas.cf.CloudFoundryAPIv2;
import com.orange.state.MidStateCalculator;
import com.orange.workflow.Workflow;
import com.orange.workflow.calculator.WorkflowCalculator;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class })
@RestController
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	private MidStateCalculator midStateCalculator;

	@RequestMapping(value = "/current_state", method = RequestMethod.POST)
	public @ResponseBody Overview getCurrentState(@RequestBody Collection<PaaSSite> managingSites) {
		Map<String, PaaSSite> sites = managingSites.stream()
				.collect(Collectors.toMap(site -> site.getName(), site -> site));
		Map<String, OverviewSite> overviewSites = managingSites.parallelStream().collect(
				Collectors.toMap(site -> site.getName(), site -> new CloudFoundryAPIv2(site).getOverviewSite()));
		logger.info("Got current state!");
		return new Overview(sites, overviewSites);
	}

	@RequestMapping(value = "/change", method = RequestMethod.POST)
	public @ResponseBody Overview change(@RequestBody Overview desiredState) {
		Overview currentState = getCurrentState(desiredState.listPaaSSites());
		validDesiredState(currentState, desiredState);
		Workflow updateWorkflow = new WorkflowCalculator(currentState, desiredState).getUpdateWorkflow();
		updateWorkflow.exec();
		logger.info("Workflow {} finished!", updateWorkflow);
		return getCurrentState(desiredState.listPaaSSites());
	}

	@RequestMapping(value = "/mid_states", method = RequestMethod.POST)
	public @ResponseBody Overview calcMidStates(@RequestBody Overview finalState) {
		Overview currentState = getCurrentState(finalState.listPaaSSites());
		validDesiredState(currentState, finalState);
		return midStateCalculator.calcMidStates(currentState, finalState);
	}

	@RequestMapping(value = "/set_update_config", method = RequestMethod.PUT)
	public void setUpdateConfig(@RequestParam("strategy") String strategy,
			@RequestBody DeploymentConfig deploymentConfig) {
		this.midStateCalculator = new MidStateCalculator(Strategy.valueOf(strategy.toUpperCase()), deploymentConfig);
	}

	private void validDesiredState(Overview currentState, Overview desiredState) {
		if (desiredState.getOverviewSites().values().stream().anyMatch(site -> site.getOverviewApps().stream()
				.anyMatch(app -> app.getGuid() == null && app.getPath() == null))) {
			throw new IllegalStateException("The path of all new apps should be specified.");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}