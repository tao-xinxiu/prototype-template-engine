package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.paas.PaaSAPI;
import com.orange.workflow.Step;

public class Canary {
	private PaaSAPI api;
	private Application desiredApp;
	
	public Canary(PaaSAPI api, Application application) {
		this.api = api;
		this.desiredApp = application;
	}
	
	public Step update() {
		return new Step(String.format("Canary %s.%s", api.getSiteName(), desiredApp.getName())) {
			@Override
			public void exec() {
				// TODO
			}
		};
	}
	
	public Step commit() {
		return new Step(String.format("commit Canary %s.%s", api.getSiteName(), desiredApp.getName())) {
			@Override
			public void exec() {
				// TODO
			}
		};
	}
	
	public Step rollback() {
		return new Step(String.format("commit Canary %s.%s", api.getSiteName(), desiredApp.getName())) {
			@Override
			public void exec() {
				// TODO
			}
		};
	}
}
