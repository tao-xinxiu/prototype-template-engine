package com.orange.workflow;

import com.orange.model.OverviewApp;
import com.orange.paas.PaaSAPI;

public class StepCalculator {
	public static Step addApp(PaaSAPI api, OverviewApp app) {
		return new Step("create app") {
			@Override
			public void exec() {
			}
		};
	}

}
