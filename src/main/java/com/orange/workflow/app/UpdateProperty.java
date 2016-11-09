package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.paas.PaaSAPI;
import com.orange.workflow.Step;

public class UpdateProperty {

	private PaaSAPI api;
	private Application desiredApp;

	public UpdateProperty(PaaSAPI api, Application application) {
		this.api = api;
		this.desiredApp = application;
	}
	
	public Step update() {
		return new Step(String.format("UpdateProperty %s.%s", api.getSiteName(), desiredApp.getName())) {
			@Override
			public void exec() {
				String appId = api.getAppId(desiredApp.getName());
				assert appId != null;
				api.updateApp(appId, desiredApp);
			}
		};
	}
}
