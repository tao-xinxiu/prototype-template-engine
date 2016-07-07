package com.orange.workflow;

import java.util.LinkedList;
import java.util.List;

import org.cloudfoundry.client.v3.applications.ApplicationResource;

import com.orange.cf.operations.PaaSClient;
import com.orange.model.*;
import com.orange.strategy.app.BlueGreen;
import com.orange.strategy.app.Delete;
import com.orange.strategy.app.Deploy;
import com.orange.strategy.app.StopRestart;
import com.orange.strategy.app.UpdateProperty;

public class WorkflowCalculator {
	
	private Requirement require;
	private DeploymentConfig deploymentConfig;

	public WorkflowCalculator(Requirement require, DeploymentConfig deploymentConfig) {
		this.require = require;
		this.deploymentConfig = deploymentConfig;
	}

	public Workflow getWorkflow() {
		switch (require) {
		case PERFORMANCE:
		case SPEED:
			// parallel blue green update
			Workflow parallelUpdateSites = new ParallelWorkflow("paralle update sites");
			for (CloudFoundryTarget target : deploymentConfig.getTargets().values()) {
				// TODO entity update order
				Workflow updateSite = new ParallelWorkflow(
						"parallel update each entity in the site " + target.getName());
				PaaSClient client = new PaaSClient(target);
				updateSite.addSteps(deployNonExistApp(client));
				for (Application application : getVersionChangedApp(client)) {
					updateSite.addStep(new BlueGreen(client, application));
				}
				updateSite.addSteps(deleteNonDesiredApp(client));
				parallelUpdateSites.addStep(updateSite);
			}
			return parallelUpdateSites;
		case RESOURCE:
			// serial stop restart update
			Workflow serialUpdateSites = new SerialWorkflow("serial update sites");
			for (CloudFoundryTarget target : deploymentConfig.getTargets().values()) {
				// TODO entity update order
				Workflow updateSite = new ParallelWorkflow(
						"parallel update each entity in the site " + target.getName());
				PaaSClient client = new PaaSClient(target);
				updateSite.addSteps(deployNonExistApp(client));
				for (Application application : getVersionChangedApp(client)) {
					updateSite.addStep(new UpdateProperty(client, application));
					updateSite.addStep(new StopRestart(client, application));
				}
				updateSite.addSteps(deleteNonDesiredApp(client));
				serialUpdateSites.addStep(updateSite);
			}
			return serialUpdateSites;
		case TEST:
			return getTestWorkflow();
		case CLEANUP:
			Workflow cleanupSites = new ParallelWorkflow("clean all entities on all sites");
			for (CloudFoundryTarget target : deploymentConfig.getTargets().values()) {
				Workflow cleanupSite = new ParallelWorkflow("cleanup all entities on site " + target.getName());
				PaaSClient client = new PaaSClient(target);
				for (ApplicationResource applicationResource : client.listSpaceApps()) {
					cleanupSite.addStep(new Delete(client, applicationResource.getId()));
				}
				cleanupSites.addStep(cleanupSite);
			}
			return cleanupSites;
		default:
			throw new IllegalStateException("not implemented requirement");
		}
	}

	private List<Step> deployNonExistApp(PaaSClient client) {
		// TODO deploy order
		List<Step> steps = new LinkedList<>();
		for (Application application : deploymentConfig.getApps().values()) {
			String appId = client.getAppId(application.getName());
			if (appId == null) { // app not exist
				steps.add(new Deploy(client, application));
			}
		}
		return steps;
	}

	private List<Application> getVersionChangedApp(PaaSClient client) {
		List<Application> apps = new LinkedList<>();
		for (Application application : deploymentConfig.getApps().values()) {
			String appId = client.getAppId(application.getName());
			if (appId != null) { // app exist
				String appVersion = (String) client.getAppEnv(appId, "APP_VERSION");
				if (!application.getVersion().equals(appVersion)) {
					apps.add(application);
				}
			}
		}
		return apps;
	}

	/**
	 * delete applications whose name are not contained in the desire deployment
	 * config
	 * 
	 * @param client
	 *            PaaSClient of the cloud foundry target
	 * @return
	 */
	private List<Step> deleteNonDesiredApp(PaaSClient client) {
		List<Step> steps = new LinkedList<>();
		for (ApplicationResource applicationResource : client.listSpaceApps()) {
			if (!deploymentConfig.getApps().keySet().contains(applicationResource.getName())) {
				steps.add(new Delete(client, applicationResource.getId()));
			}
		}
		return steps;
	}

	private Workflow getTestWorkflow() {
		Workflow updateSites = new ParallelWorkflow("parallel update sites");
		updateSites.addStep(new Deploy(getClient("lz"), getApp("hello")));
		updateSites.addStep(new Deploy(getClient("obs"), getApp("hello")));
		return updateSites;
	}

	private Application getApp(String appName) {
		return deploymentConfig.getApps().get(appName);
	}

	private PaaSClient getClient(String targetName) {
		return new PaaSClient(deploymentConfig.getTargets().get(targetName));
	}
}
