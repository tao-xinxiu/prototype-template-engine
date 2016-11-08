package com.orange.workflow.app;

import com.orange.paas.PaaSAPI;
import com.orange.workflow.Step;

public class Delete {
	private PaaSAPI api;
	private String appIdToDelete;

	public Delete(PaaSAPI api, String appId) {
		this.api = api;
		this.appIdToDelete = appId;
	}
	
	public Step update() {
		return new Step(String.format("Delete %s.%s", api.getTargetName(), appIdToDelete)) {
			@Override
			public void exec() {
				api.deleteApp(appIdToDelete);
			}
		};
	}

}
