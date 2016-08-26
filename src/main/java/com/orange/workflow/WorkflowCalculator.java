package com.orange.workflow;

import java.util.LinkedList;
import java.util.List;

import com.orange.model.*;
import com.orange.paas.PaaSAPI;
import com.orange.paas.cf.CloudFoundryAPI;
import com.orange.workflow.app.BlueGreen;
import com.orange.workflow.app.Delete;
import com.orange.workflow.app.Deploy;
import com.orange.workflow.app.StopRestart;
import com.orange.workflow.app.UpdateProperty;

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
			for (PaaSTarget target : deploymentConfig.getTargets().values()) {
				// TODO entity update order
				Workflow updateSite = new ParallelWorkflow(
						"parallel update each entity in the site " + target.getName());
				PaaSAPI api = new CloudFoundryAPI(target);
				updateSite.addSteps(deployNonExistApp(api));
				for (Application application : getVersionChangedApp(api)) {
					updateSite.addStep(new BlueGreen(api, application));
				}
				updateSite.addSteps(deleteNonDesiredApp(api));
				parallelUpdateSites.addStep(updateSite);
			}
			return parallelUpdateSites;
		case RESOURCE:
			// serial stop restart update
			Workflow serialUpdateSites = new SerialWorkflow("serial update sites");
			for (PaaSTarget target : deploymentConfig.getTargets().values()) {
				// TODO entity update order
				Workflow updateSite = new ParallelWorkflow(
						"parallel update each entity in the site " + target.getName());
				PaaSAPI api = new CloudFoundryAPI(target);
				updateSite.addSteps(deployNonExistApp(api));
				for (Application application : getVersionChangedApp(api)) {
					Workflow updateApp = new SerialWorkflow("serial update app " + application.getName() + " at target " + target.getName());
					updateApp.addStep(new UpdateProperty(api, application));
					updateApp.addStep(new StopRestart(api, application));
					updateSite.addStep(updateApp);
				}
				updateSite.addSteps(deleteNonDesiredApp(api));
				serialUpdateSites.addStep(updateSite);
			}
			return serialUpdateSites;
		case TEST:
			return getTestWorkflow();
		case CLEANUP:
			Workflow cleanupSites = new ParallelWorkflow("clean all entities on all sites");
			for (PaaSTarget target : deploymentConfig.getTargets().values()) {
				Workflow cleanupSite = new ParallelWorkflow("cleanup all entities on site " + target.getName());
				PaaSAPI api = new CloudFoundryAPI(target);
				for (String appId : api.listSpaceAppsId()) {
					cleanupSite.addStep(new Delete(api, appId));
				}
				cleanupSites.addStep(cleanupSite);
			}
			return cleanupSites;
		default:
			throw new IllegalStateException("not implemented requirement");
		}
	}

	private List<Step> deployNonExistApp(PaaSAPI api) {
		// TODO deploy order
		List<Step> steps = new LinkedList<>();
		for (Application application : deploymentConfig.getApps().values()) {
			String appId = api.getAppId(application.getName());
			if (appId == null) { // app not exist
				steps.add(new Deploy(api, application));
			}
		}
		return steps;
	}

	private List<Application> getVersionChangedApp(PaaSAPI api) {
		List<Application> apps = new LinkedList<>();
		for (Application application : deploymentConfig.getApps().values()) {
			String appId = api.getAppId(application.getName());
			if (appId != null) { // app exist
				String appVersion = (String) api.getAppVersion(appId);
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
	 * @param api
	 *            PaaSAPI of the target PaaS
	 * @return
	 */
	private List<Step> deleteNonDesiredApp(PaaSAPI api) {
		List<Step> steps = new LinkedList<>();
		for (String appId : api.listSpaceAppsId()) {
			if (!deploymentConfig.getApps().keySet().contains(api.getAppName(appId))) {
				steps.add(new Delete(api, appId));
			}
		}
		return steps;
	}

	private Workflow getTestWorkflow() {
		Workflow updateSites = new ParallelWorkflow("parallel update sites");
		updateSites.addStep(new Deploy(getAPI("lz"), getApp("hello")));
		updateSites.addStep(new Deploy(getAPI("obs"), getApp("hello")));
		return updateSites;
	}

	private Application getApp(String appName) {
		return deploymentConfig.getApps().get(appName);
	}

	private PaaSAPI getAPI(String targetName) {
		return new CloudFoundryAPI(deploymentConfig.getTargets().get(targetName));
	}
}
