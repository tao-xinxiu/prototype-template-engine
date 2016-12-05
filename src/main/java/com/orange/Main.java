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
import com.orange.model.OverviewSite;
import com.orange.model.PaaSSite;
import com.orange.paas.cf.CloudFoundryAPI;
import com.orange.workflow.Workflow;
import com.orange.workflow.calculator.WorkflowCalculator;

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
		Workflow updateWorkflow = new WorkflowCalculator(currentState, desiredState).getUpdateWorkflow();
		updateWorkflow.exec();
		logger.info("Workflow {} finished!", updateWorkflow);
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