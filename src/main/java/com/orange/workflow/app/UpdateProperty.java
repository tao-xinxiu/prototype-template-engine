package com.orange.workflow.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class UpdateProperty extends Step {
	private static final Logger logger = LoggerFactory.getLogger(Delete.class);

	private PaaSAPI api;
	private Application application;

	public UpdateProperty(PaaSAPI api, Application application) {
		super(String.format("UpdateProperty %s.%s", api.getTargetName(), application.getName()));
		this.api = api;
		this.application = application;
	}

	@Override
	public void exec() {
		logger.info("start {} app: {} on the target: {}", this.getClass().getName(), application.getName(),
				api.getTargetName());
		String appId = api.getAppId(application.getName());
		assert appId != null;
		api.updateApp(appId, application);
		logger.info("Step {} Done! App: {} on the target: {}", this.getClass().getName(), application,
				api.getTargetName());
	}

}
