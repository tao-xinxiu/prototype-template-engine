package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.cf.CloudFoundryAPI;

public class BlueGreen extends Step {
	private CloudFoundryAPI client;
	private Application application;

	public BlueGreen(CloudFoundryAPI client, Application application) {
		super(String.format("BlueGreen %s.%s", client.getTargetName(), application.getName()));
		this.client = client;
		this.application = application;
	}

	public void exec() {
	}
}
