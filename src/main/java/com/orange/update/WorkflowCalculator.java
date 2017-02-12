package com.orange.update;

import com.orange.Main;
import com.orange.model.PaaSSite;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.model.workflow.ParallelWorkflow;
import com.orange.model.workflow.SerialWorkflow;
import com.orange.model.workflow.Workflow;
import com.orange.paas.PaaSAPI;

public class WorkflowCalculator {
    private Overview currentState;
    private Overview desiredState;

    public WorkflowCalculator(Overview currentState, Overview desiredState) {
	this.currentState = currentState;
	this.desiredState = desiredState;
    }

    public Workflow getUpdateWorkflow() {
	Workflow updateSites = new ParallelWorkflow("parallel update sites");
	for (PaaSSite site : desiredState.listPaaSSites()) {
	    Workflow updateSite = new ParallelWorkflow(
		    String.format("parallel update site %s entities", site.getName()));
	    SiteComparator comparator = new SiteComparator(currentState.getOverviewSite(site.getName()),
		    desiredState.getOverviewSite(site.getName()));
	    PaaSAPI api = Main.getPaaSAPI(site);
	    UpdateStepDirectory stepDir = new UpdateStepDirectory(api);
	    for (OverviewApp app : comparator.getAddedApp()) {
		updateSite.addStep(stepDir.addApp(app));
	    }
	    for (OverviewApp app : comparator.getRemovedApp()) {
		updateSite.addStep(stepDir.removeApp(app));
	    }
	    for (AppComparator appComparator : comparator.getAppComparators()) {
		updateSite.addStep(getAppUpdateWorkflow(appComparator, stepDir,
			String.format("serial update app from %s to %s at site %s", appComparator.getCurrentApp(),
				appComparator.getDesiredApp(), site.getName())));
	    }
	    updateSites.addStep(updateSite);
	}
	return updateSites;
    }

    private Workflow getAppUpdateWorkflow(AppComparator appComparator, UpdateStepDirectory stepCalculator,
	    String workflowName) {
	Workflow updateApp = new SerialWorkflow(workflowName);
	String appId = appComparator.getCurrentApp().getGuid();
	if (appComparator.isEnvUpdated()) {
	    updateApp.addStep(stepCalculator.updateAppEnv(appComparator.getDesiredApp()));
	}
	if (appComparator.isNameUpdated()) {
	    updateApp.addStep(stepCalculator.updateAppName(appComparator.getDesiredApp()));
	}
	if (appComparator.isRoutesUpdated()) {
	    updateApp.addStep(stepCalculator.addAppRoutes(appId, appComparator.getAddedRoutes()));
	    updateApp.addStep(stepCalculator.removeAppRoutes(appId, appComparator.getRemovedRoutes()));
	}
	if (appComparator.isStateUpdated()) {
	    updateApp.addStep(
		    stepCalculator.updateAppState(appComparator.getCurrentApp(), appComparator.getDesiredApp()));
	}
	if (appComparator.isInstancesUpdated()) {
	    updateApp.addStep(stepCalculator.scaleApp(appId, appComparator.getDesiredApp().getInstances()));
	}
	return updateApp;
    }
}
