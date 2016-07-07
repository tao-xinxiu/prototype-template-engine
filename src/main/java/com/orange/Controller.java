package com.orange;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import com.orange.model.Application;
import com.orange.model.CloudFoundryTarget;
import com.orange.model.DeploymentConfig;
import com.orange.model.Requirement;
import com.orange.model.Workflow;
import com.orange.workflow.WorkflowCalculator;

@RestController
public class Controller {
	private static final Logger logger = LoggerFactory.getLogger(Controller.class);
	static DeploymentConfig deploymentConfig;

	@RequestMapping(value = "/set", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody String setDeploymentConfig(@RequestBody DeploymentConfig deploymentConfig) {
		for (Map.Entry<String,CloudFoundryTarget> entry: deploymentConfig.getTargets().entrySet()) {
			if (!entry.getValue().valid()) {
				throw new IllegalStateException("DeploymentConfig not valid, missing mandatory property for targets.");
			}
			entry.getValue().setName(entry.getKey());
		}
		logger.info("DeploymentConfig targets valid");
		for (Map.Entry<String,Application> entry: deploymentConfig.getApps().entrySet()) {
			if (!entry.getValue().valid()) {
				throw new IllegalStateException("DeploymentConfig not valid, missing mandatory property for apps.");
			}
			entry.getValue().setName(entry.getKey());
		}
		logger.info("DeploymentConfig apps valid");
		Controller.deploymentConfig = deploymentConfig;
		return "\n OK! \n";
	}

	@RequestMapping(value = "/update", method = RequestMethod.PUT)
	public @ResponseBody String update(@RequestBody String require) throws InterruptedException {
		Requirement requirement = Requirement.valueOf(require.toUpperCase());
		assert deploymentConfig != null : "deploymentConfig not configured!";
		Workflow workflow = new WorkflowCalculator(requirement, deploymentConfig).getWorkflow();
		workflow.exec();
		return "\n OK! \n";
	}
}
