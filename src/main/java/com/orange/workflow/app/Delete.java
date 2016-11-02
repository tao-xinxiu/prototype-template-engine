package com.orange.workflow.app;

import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class Delete {
	private PaaSAPI api;
	private String appId;

	public Delete(PaaSAPI api, String appId) {
		this.api = api;
		this.appId = appId;
	}
	
	public Step update() {
		return new Step(String.format("Delete %s.%s", api.getTargetName(), appId)) {
			@Override
			public void exec() {
				api.deleteApp(appId);
			}
		};
	}

}
