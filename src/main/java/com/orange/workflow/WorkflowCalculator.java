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
	private List<Application> updateApps;

	public WorkflowCalculator(Requirement require, DeploymentConfig deploymentConfig) {
		this.require = require;
		this.deploymentConfig = deploymentConfig;
	}

	public Workflow getUpdateWorkflow() {
		Workflow updateSites;
		switch (require) {
		case FAST:
			updateSites = new ParallelWorkflow("parallel update sites");
			break;
		case CAUTIOUS: case ECONOMICAL:
			updateSites = new SerialWorkflow("serial update sites");
			break;
		case CLEANUP:
			return getCleanupWorkflow();
		default:
			throw new IllegalStateException("not implemented requirement");
		}
		for (PaaSTarget target : deploymentConfig.getTargets().values()) {
			Workflow updateSite = new ParallelWorkflow(
					"parallel update each entity in the site " + target.getName());
			PaaSAPI api = new CloudFoundryAPI(target);
			for (Application application : getMissingApp(api)) {
				updateSite.addStep(new Deploy(api, application).update());
			}
			updateApps = getVersionChangedApp(api);
			for (Application application : updateApps) {
				switch (require) {
				case FAST:
					updateSite.addStep(new BlueGreen(api, application).update());
					break;
				case CAUTIOUS:
					updateSite.addStep(new Canary(api, application).update());
					break;
				case ECONOMICAL:
					Workflow updateApp = new SerialWorkflow(String.format("serial update %s.%s", target.getName(), application.getName()));
					updateApp.addStep(new UpdateProperty(api, application).update());
					updateApp.addStep(new StopRestart(api, application).update());
					updateSite.addStep(updateApp);
					break;
				default:
					break;
				}
			}
			updateSites.addStep(updateSite);
		}
		return updateSites;
	}
	
	public Workflow getCommitWorkflow() {
		Workflow commitSites;
		switch (require) {
		case FAST:
			commitSites = new ParallelWorkflow("parallel commit change in sites");
			break;
		case CAUTIOUS:
			commitSites = new SerialWorkflow("serial commit change in sites");
			break;
		default:
			throw new IllegalStateException("not implemented requirement");
		}
		for (PaaSTarget target : deploymentConfig.getTargets().values()) {
			Workflow commitSite = new ParallelWorkflow(
					"parallel commit change of each entity in the site " + target.getName());
			PaaSAPI api = new CloudFoundryAPI(target);
			updateApps = getVersionChangedApp(api);
			for (Application application : updateApps) {
				switch (require) {
				case FAST:
					commitSite.addStep(new BlueGreen(api, application).commit());
					break;
				case CAUTIOUS:
					commitSite.addStep(new Canary(api, application).commit());
					break;
				default:
					break;
				}
			}
			for (String appId : getNotDesiredAppIds(api)) {
				commitSite.addStep(new Delete(api, appId).update());
			}
			commitSites.addStep(commitSite);
		}
		return commitSites;
	}
	
	public Workflow getRollbackWorkflow() {
		Workflow rollbackSites;
		switch (require) {
		case FAST:
			rollbackSites = new ParallelWorkflow("parallel rollback change in sites");
			break;
		case CAUTIOUS:
			rollbackSites = new SerialWorkflow("serial rollback change in sites");
			break;
		default:
			throw new IllegalStateException("not implemented requirement");
		}
		for (PaaSTarget target : deploymentConfig.getTargets().values()) {
			Workflow rollbackSite = new ParallelWorkflow(
					"parallel rollback change of each entity in the site " + target.getName());
			PaaSAPI api = new CloudFoundryAPI(target);
			updateApps = getVersionChangedApp(api);
			for (Application application : updateApps) {
				switch (require) {
				case FAST:
					rollbackSite.addStep(new BlueGreen(api, application).rollback());
					break;
				case CAUTIOUS:
					rollbackSite.addStep(new Canary(api, application).rollback());
					break;
				default:
					break;
				}
			}
			for (Application application : getMissingApp(api)) {
				String appId = api.getAppId(application.getName());
				rollbackSite.addStep(new Delete(api, appId).update());
			}
			rollbackSites.addStep(rollbackSite);
		}
		return rollbackSites;
	}

	private List<Application> getMissingApp(PaaSAPI api) {
		List<Application> apps = new LinkedList<>();
		for (Application application : deploymentConfig.getApps().values()) {
			String appId = api.getAppId(application.getName());
			if (appId == null) { // desired app not exist in the target PaaS
				apps.add(application);
			}
		}
		return apps;
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
	 * get applications' id whose name are not contained in the desire
	 * deployment config
	 * 
	 * @param api
	 *            PaaSAPI of the target PaaS
	 * @return
	 */
	private List<String> getNotDesiredAppIds(PaaSAPI api) {
		List<String> appIds = new LinkedList<>();
		for (String appId : api.listSpaceAppsId()) {
			String appName = api.getAppName(appId);
			for (Application desiredApp : deploymentConfig.getApps().values()) {
				if (!desiredApp.getName().equals(appName) && !(desiredApp.getName() + desiredApp.getVersion()).equals(appName)) {
					appIds.add(appId);
				}
			}
		}
		return appIds;
	}
	
	private Workflow getCleanupWorkflow() {
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
	}
}
