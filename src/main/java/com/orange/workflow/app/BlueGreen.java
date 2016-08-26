package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class BlueGreen extends Step {
	private PaaSAPI api;
	private Application application;

	public BlueGreen(PaaSAPI api, Application application) {
		super(String.format("BlueGreen %s.%s", api.getTargetName(), application.getName()));
		this.api = api;
		this.application = application;
	}

	public void exec() {
	}
}
