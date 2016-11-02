package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.model.Step;
import com.orange.paas.PaaSAPI;

public class Canary {
	private PaaSAPI api;
	private Application application;
	
	public Canary(PaaSAPI api, Application application) {
		this.api = api;
		this.application = application;
	}
	
	public Step update() {
		return new Step(String.format("Canary %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				// TODO
			}
		};
	}
	
	public Step commit() {
		return new Step(String.format("commit Canary %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				// TODO
			}
		};
	}
	
	public Step rollback() {
		return new Step(String.format("commit Canary %s.%s", api.getTargetName(), application.getName())) {
			@Override
			public void exec() {
				// TODO
			}
		};
	}
}
