package com.orange.update;

import java.util.Map.Entry;

import com.orange.Main;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSSite;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.model.workflow.ParallelWorkflow;
import com.orange.model.workflow.SerialWorkflow;
import com.orange.model.workflow.Workflow;
import com.orange.paas.UpdateStepDirectory;
import com.orange.paas.cf.CloudFoundryAPIv2UpdateStepDirectory;

public class WorkflowCalculator {
    private Overview currentState;
    private Overview desiredState;
    private OperationConfig config;

    public WorkflowCalculator(Overview currentState, Overview desiredState, OperationConfig config) {
	this.currentState = currentState;
	this.desiredState = desiredState;
	this.config = config;
    }

    public Workflow getUpdateWorkflow() {
	Workflow updateSites = new ParallelWorkflow("parallel update sites");
	for (PaaSSite site : desiredState.listPaaSSites()) {
	    Workflow updateSite = config.isParallelUpdateApps()
		    ? new ParallelWorkflow(String.format("parallel update site %s entities", site.getName()))
		    : new SerialWorkflow(String.format("serial update site %s entities", site.getName()));
	    SiteComparator comparator = new SiteComparator(currentState.getOverviewSite(site.getName()),
		    desiredState.getOverviewSite(site.getName()));
	    UpdateStepDirectory directory = new CloudFoundryAPIv2UpdateStepDirectory(
		    Main.getCloudFoundryOperations(site, config));
	    for (OverviewApp app : comparator.getAddedApp()) {
		updateSite.addStep(directory.addApp(app));
	    }
	    for (OverviewApp app : comparator.getRemovedApp()) {
		updateSite.addStep(directory.removeApp(app));
	    }
	    for (Entry<OverviewApp, OverviewApp> updatedApp : comparator.getUpdatedApp().entrySet()) {
		updateSite.addStep(directory.updateApp(updatedApp.getKey(), updatedApp.getValue()));
	    }
	    updateSites.addStep(updateSite);
	}
	return updateSites;
    }
}
