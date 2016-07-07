package com.orange.strategy.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.cf.operations.PaaSClient;
import com.orange.model.Step;

public class Delete extends Step {
	private static final Logger logger = LoggerFactory.getLogger(Delete.class);

	private PaaSClient client;
	private String appId;

	public Delete(PaaSClient client, String appId) {
		super(String.format("Delete %s.%s", client.getTargetName(), appId));
		this.client = client;
		this.appId = appId;
	}

	public void exec() {
		logger.info("start {} app: {} on the target: {}", this.getClass().getName(), appId, client.getTargetName());
		client.deleteApp(appId);
		logger.info("Step {} Done! App deleted with id: {} on the target: {}", this.getClass().getName(), appId,
				client.getTargetName());
	}
}
