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
	private Application desiredApp;
	private boolean appVersionChanged;
	private boolean appMissing;

	public WorkflowCalculator(Requirement require, DeploymentConfig deploymentConfig) {
		this.require = require;
		this.deploymentConfig = deploymentConfig;
		this.desiredApp = deploymentConfig.getApp();
	}

	public Workflow getUpdateWorkflow() {
		Workflow updateSites;
		switch (require) {
		case FAST:
			updateSites = new ParallelWorkflow("parallel update sites");
			break;
		case CAUTIOUS:
		case ECONOMICAL:
			updateSites = new SerialWorkflow("serial update sites");
			break;
		case CLEANUP:
			return getCleanupWorkflow();
		default:
			throw new IllegalStateException("not implemented requirement");
		}
		for (PaaSSite site : deploymentConfig.getSites().values()) {
			Workflow updateSite = new ParallelWorkflow("parallel update each entity in the site " + site.getName());
			PaaSAPI api = new CloudFoundryAPI(site);
			this.appMissing = isMissingApp(api);
			if(appMissing) {
				updateSite.addStep(new Deploy(api, desiredApp).update());
			}
			this.appVersionChanged = isVersionChangedApp(api);
			if(appVersionChanged) {
				switch (require) {
				case FAST:
					updateSite.addStep(new BlueGreen(api, desiredApp).update());
					break;
				case CAUTIOUS:
					updateSite.addStep(new Canary(api, desiredApp).update());
					break;
				case ECONOMICAL:
					Workflow updateApp = new SerialWorkflow(
							String.format("serial update %s.%s", site.getName(), desiredApp.getName()));
					updateApp.addStep(new UpdateProperty(api, desiredApp).update());
					updateApp.addStep(new StopRestart(api, desiredApp).update());
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
		for (PaaSSite site : deploymentConfig.getSites().values()) {
			Workflow commitSite = new ParallelWorkflow(
					"parallel commit change of each entity in the site " + site.getName());
			PaaSAPI api = new CloudFoundryAPI(site);
			if (appVersionChanged) {
				switch (require) {
				case FAST:
					commitSite.addStep(new BlueGreen(api, desiredApp).commit());
					break;
				case CAUTIOUS:
					commitSite.addStep(new Canary(api, desiredApp).commit());
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
		for (PaaSSite site : deploymentConfig.getSites().values()) {
			Workflow rollbackSite = new ParallelWorkflow(
					"parallel rollback change of each entity in the site " + site.getName());
			PaaSAPI api = new CloudFoundryAPI(site);
			if (appVersionChanged) {
				switch (require) {
				case FAST:
					rollbackSite.addStep(new BlueGreen(api, desiredApp).rollback());
					break;
				case CAUTIOUS:
					rollbackSite.addStep(new Canary(api, desiredApp).rollback());
					break;
				default:
					break;
				}
			}
			if (appMissing) {
				String appId = api.getAppId(desiredApp.getName());
				rollbackSite.addStep(new Delete(api, appId).update());
			}
			rollbackSites.addStep(rollbackSite);
		}
		return rollbackSites;
	}

	private boolean isMissingApp(PaaSAPI api) {
		String appId = api.getAppId(desiredApp.getName());
		if (appId == null) { // desired app not exist in the target PaaS site
			return true;
		}
		return false;
	}

	private boolean isVersionChangedApp(PaaSAPI api) {
		String appId = api.getAppId(desiredApp.getName());
		if (appId != null) { // app exist
			String appVersion = (String) api.getAppVersion(appId);
			if (!desiredApp.getVersion().equals(appVersion)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * get applications' id whose name are neither the name nor the temporal
	 * name(name+version) of the app specified in the desire deployment config
	 * 
	 * @param api
	 *            PaaSAPI of the target PaaS site
	 * @return
	 */
	private List<String> getNotDesiredAppIds(PaaSAPI api) {
		List<String> appIds = new LinkedList<>();
		for (String appId : api.listSpaceAppsId()) {
			String appName = api.getAppName(appId);
			if (!desiredApp.getName().equals(appName)
					&& !(desiredApp.getName() + desiredApp.getVersion()).equals(appName)) {
				appIds.add(appId);
			}
		}
		return appIds;
	}

	private Workflow getCleanupWorkflow() {
		Workflow cleanupSites = new ParallelWorkflow("clean all entities on all sites");
		for (PaaSSite site : deploymentConfig.getSites().values()) {
			Workflow cleanupSite = new ParallelWorkflow("cleanup all entities on site " + site.getName());
			PaaSAPI api = new CloudFoundryAPI(site);
			for (String appId : api.listSpaceAppsId()) {
				cleanupSite.addStep(new Delete(api, appId).update());
			}
			cleanupSites.addStep(cleanupSite);
		}
		return cleanupSites;
	}
}
