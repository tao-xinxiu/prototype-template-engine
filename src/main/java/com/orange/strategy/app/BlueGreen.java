package com.orange.strategy.app;

import com.orange.cf.operations.PaaSClient;
import com.orange.model.Application;
import com.orange.model.Step;

public class BlueGreen extends Step {
	private PaaSClient client;
	private Application application;

	public BlueGreen(PaaSClient client, Application application) {
		super(String.format("BlueGreen %s.%s", client.getTargetName(), application.getName()));
		this.client = client;
		this.application = application;
	}

	public void exec() {
	}
}
