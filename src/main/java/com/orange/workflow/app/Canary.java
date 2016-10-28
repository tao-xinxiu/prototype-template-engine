package com.orange.workflow.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class Canary extends Step {
	private PaaSAPI api;
	private Application application;
	
	private static final Logger logger = LoggerFactory.getLogger(Canary.class);

	public Canary(PaaSAPI api, Application application) {
		super(String.format("Canary %s.%s", api.getTargetName(), application.getName()));
		this.api = api;
		this.application = application;
	}

	@Override
	public void exec() {

	}

}
