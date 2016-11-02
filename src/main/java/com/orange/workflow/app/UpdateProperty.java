package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class UpdateProperty {

	private PaaSAPI api;
	private Application application;

	public UpdateProperty(PaaSAPI api, Application application) {
		this.api = api;
		this.application = application;
	}
	
	public Step update() {
		return new Step(String.format("UpdateProperty %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				String appId = api.getAppId(application.getName());
				assert appId != null;
				api.updateApp(appId, application);
			}
		};
	}
}
