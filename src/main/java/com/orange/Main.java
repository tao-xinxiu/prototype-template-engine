package com.orange;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.orange.model.Application;
import com.orange.model.DeploymentConfig;
import com.orange.model.PaaSTarget;
import com.orange.model.Requirement;
import com.orange.model.Workflow;
import com.orange.workflow.WorkflowCalculator;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class })
@RestController
public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	static DeploymentConfig deploymentConfig;
	static Requirement requirement;

	@RequestMapping(value = "/set", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody String setDeploymentConfig(@RequestBody DeploymentConfig deploymentConfig) {
		for (Map.Entry<String, PaaSTarget> entry : deploymentConfig.getTargets().entrySet()) {
			if (!entry.getValue().valid()) {
				throw new IllegalStateException("DeploymentConfig not valid, missing mandatory property for target: " + entry.getValue());
			}
			entry.getValue().setName(entry.getKey());
		}
		logger.info("DeploymentConfig targets valid");
		for (Map.Entry<String, Application> entry : deploymentConfig.getApps().entrySet()) {
			if (!entry.getValue().valid()) {
				throw new IllegalStateException("DeploymentConfig not valid, missing mandatory property for app: " + entry.getValue());
			}
			entry.getValue().setName(entry.getKey());
		}
		logger.info("DeploymentConfig apps valid");
		Main.deploymentConfig = deploymentConfig;
		return "\n OK! \n";
	}

	@RequestMapping(value = "/update", method = RequestMethod.PUT)
	public @ResponseBody String update(@RequestBody String require) throws InterruptedException {
		requirement = Requirement.valueOf(require.toUpperCase());
		assert deploymentConfig != null : "deploymentConfig not configured!";
		Workflow workflow = new WorkflowCalculator(requirement, deploymentConfig).getWorkflow();
		workflow.exec();
		return "\n OK! \n";
	}

	@RequestMapping(value = "/commit", method = RequestMethod.PUT)
	public @ResponseBody String commit() {
		return "\n OK! \n";
	}

	@RequestMapping(value = "/rollback", method = RequestMethod.PUT)
	public @ResponseBody String rollback() {
		return "\n OK! \n";
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}