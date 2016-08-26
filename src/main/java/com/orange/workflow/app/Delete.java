package com.orange.workflow.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class Delete extends Step {
	private static final Logger logger = LoggerFactory.getLogger(Delete.class);

	private PaaSAPI api;
	private String appId;

	public Delete(PaaSAPI api, String appId) {
		super(String.format("Delete %s.%s", api.getTargetName(), appId));
		this.api = api;
		this.appId = appId;
	}

	public void exec() {
		logger.info("start {} app: {} on the target: {}", this.getClass().getName(), appId, api.getTargetName());
		api.deleteApp(appId);
		logger.info("Step {} Done! App deleted with id: {} on the target: {}", this.getClass().getName(), appId,
				api.getTargetName());
	}
}
