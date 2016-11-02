package com.orange.workflow;

import java.util.LinkedList;
import java.util.List;

import com.orange.model.*;
import com.orange.paas.PaaSAPI;
import com.orange.paas.cf.CloudFoundryAPI;
import com.orange.workflow.app.BlueGreen;
import com.orange.workflow.app.Canary;
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
		case FAST:
			// parallel blue green update
			Workflow fastUpdateSites = new ParallelWorkflow("parallel update sites");
			for (PaaSTarget target : deploymentConfig.getTargets().values()) {
				Workflow updateSite = new ParallelWorkflow(
						"parallel update each entity in the site " + target.getName());
				PaaSAPI api = new CloudFoundryAPI(target);
				updateSite.addSteps(deployNonExistApp(api));
				for (Application application : getVersionChangedApp(api)) {
					updateSite.addStep(new BlueGreen(api, application).update());
				}
				updateSite.addSteps(deleteNonDesiredApp(api));
				fastUpdateSites.addStep(updateSite);
			}
			return fastUpdateSites;
		case CAUTIOUS:
			Workflow cautiousUpdateSites = new SerialWorkflow("serial update sites");
			for (PaaSTarget target : deploymentConfig.getTargets().values()) {
				Workflow updateSite = new ParallelWorkflow(
						"parallel update each entity in the site " + target.getName());
				PaaSAPI api = new CloudFoundryAPI(target);
				updateSite.addSteps(deployNonExistApp(api));
				for (Application application : getVersionChangedApp(api)) {
					updateSite.addStep(new Canary(api, application));
				}
				updateSite.addSteps(deleteNonDesiredApp(api));
				cautiousUpdateSites.addStep(updateSite);
			}
			return cautiousUpdateSites;
		case ECONOMICAL:
			// serial stop restart update
			Workflow economicalUpdateSites = new SerialWorkflow("serial update sites");
			for (PaaSTarget target : deploymentConfig.getTargets().values()) {
				Workflow updateSite = new ParallelWorkflow(
						"parallel update each entity in the site " + target.getName());
				PaaSAPI api = new CloudFoundryAPI(target);
				updateSite.addSteps(deployNonExistApp(api));
				for (Application application : getVersionChangedApp(api)) {
					Workflow updateApp = new SerialWorkflow("serial update app " + application.getName() + " at target " + target.getName());
					updateApp.addStep(new UpdateProperty(api, application).update());
					updateApp.addStep(new StopRestart(api, application).update());
					updateSite.addStep(updateApp);
				}
				updateSite.addSteps(deleteNonDesiredApp(api));
				economicalUpdateSites.addStep(updateSite);
			}
			return economicalUpdateSites;
		case TEST:
			return getTestWorkflow();
		case CLEANUP:
			Workflow cleanupSites = new ParallelWorkflow("clean all entities on all sites");
			for (PaaSTarget target : deploymentConfig.getTargets().values()) {
				Workflow cleanupSite = new ParallelWorkflow("cleanup all entities on site " + target.getName());
				PaaSAPI api = new CloudFoundryAPI(target);
				for (String appId : api.listSpaceAppsId()) {
					cleanupSite.addStep(new Delete(api, appId).update());
				}
				cleanupSites.addStep(cleanupSite);
			}
			return cleanupSites;
		default:
			throw new IllegalStateException("not implemented requirement");
		}
	}

	private List<Step> deployNonExistApp(PaaSAPI api) {
		List<Step> steps = new LinkedList<>();
		for (Application application : deploymentConfig.getApps().values()) {
			String appId = api.getAppId(application.getName());
			if (appId == null) { // app not exist
				steps.add(new Deploy(api, application).update());
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
				steps.add(new Delete(api, appId).update());
			}
		}
		return steps;
	}

	private Workflow getTestWorkflow() {
		Workflow updateSites = new ParallelWorkflow("parallel update sites");
		updateSites.addStep(new Deploy(getAPI("lz"), getApp("hello")).update());
		updateSites.addStep(new Deploy(getAPI("obs"), getApp("hello")).update());
		return updateSites;
	}

	private Application getApp(String appName) {
		return deploymentConfig.getApps().get(appName);
	}

	private PaaSAPI getAPI(String targetName) {
		return new CloudFoundryAPI(deploymentConfig.getTargets().get(targetName));
	}
}
