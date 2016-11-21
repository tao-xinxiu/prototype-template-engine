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
import com.orange.model.PaaSSite;
import com.orange.model.Requirement;
import com.orange.paas.cf.CloudFoundryAPI;
import com.orange.state.Comparator;
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
	public @ResponseBody void change(@RequestBody Overview desiredState) {
		Comparator comparator = new Comparator(getCurrentState(), desiredState);
		if (!comparator.valide()) {
			throw new IllegalStateException("Get current_state for all the sites to be managed first.");
		}
	}

	private Overview getCurrentState() {
		return new Overview(managingSites.parallelStream()
				.collect(Collectors.toMap(site -> site, site -> new CloudFoundryAPI(site).getOverviewSite())));
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}