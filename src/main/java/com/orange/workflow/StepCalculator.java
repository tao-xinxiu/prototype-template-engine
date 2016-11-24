package com.orange.workflow;

import com.orange.model.OverviewApp;
import com.orange.model.OverviewDroplet;
import com.orange.paas.PaaSAPI;

public class StepCalculator {
	public static Step addApp(PaaSAPI api, OverviewApp app) {
		return new Step("createApp at " + api.getSiteName()) {
			@Override
			public void exec() {
				assert app.getDroplets().size() == 1;
				OverviewDroplet droplet = app.getDroplets().get(0);
				String appId = api.createAppWithOneDroplet(app);
				String dropletId = api.createDroplet(appId, droplet);
				switch (droplet.getState()) {
				case STAGED:
					break;
				case RUNNING:
					api.assignDroplet(appId, dropletId);
					api.startAppAndWaitUntilRunning(appId);
					api.mapAppRoutes(appId, app.getRoutes());
					break;
				default:
					throw new IllegalStateException("Abnormal desired droplet state");
				}
			}
		};
	}
}
