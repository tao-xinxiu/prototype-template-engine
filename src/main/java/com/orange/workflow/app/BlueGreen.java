package com.orange.workflow.app;

import com.orange.model.Application;
import com.orange.paas.PaaSAPI;
import com.orange.workflow.Step;

public class BlueGreen {
	private PaaSAPI api;
	private Application desiredApp;
	
	public BlueGreen(PaaSAPI api, Application application) {
		this.api = api;
		this.desiredApp = application;
	}
	
	/**
	 * update app from initial state to the intermediate state (midApp)
	 * @return
	 */
	public Step update() {
		return new Step(String.format("BlueGreen %s.%s", api.getSiteName(), desiredApp.getName())) {
			@Override
			public void exec() {
				Application midApp = new Application(desiredApp);
				String midAppName = desiredApp.getName() + desiredApp.getVersion();
				midApp.setName(midAppName);
				
				String midAppId = api.createAppIfNotExist(midApp);
				api.prepareApp(midAppId, midApp);
				String midRouteId = api.createRouteIfNotExist(midApp.getHostnames().get("tmp"), "tmp");
				api.createRouteMapping(midAppId, midRouteId);
				//midApp mapped to temporary local route
				api.startAppAndWaitUntilRunning(midAppId);
			}
		};
	}
	
	/**
	 * update app from intermediate state to the desired state
	 * @return
	 */
	public Step commit() {
		return new Step(String.format("commit BlueGreen %s.%s", api.getSiteName(), desiredApp.getName())) {
			@Override
			public void exec() {
				String midAppName = desiredApp.getName() + desiredApp.getVersion();
				String midAppId = api.getAppId(midAppName);
				// midApp mapped to app original local and global route
				// map first global route to ensure that global route correctly mapped when DNS detects it available
				String globalRouteId = api.createRouteIfNotExist(desiredApp.getHostnames().get("global"), "global");
				api.createRouteMapping(midAppId, globalRouteId);
				String localRouteId = api.createRouteIfNotExist(desiredApp.getHostnames().get("local"), "local");
				api.createRouteMapping(midAppId, localRouteId);
				// unmap tmp route, delete old version app and rename new version app
				String midRouteId = api.getRouteId(desiredApp.getHostnames().get("tmp"), "tmp");
				api.deleteRouteMapping(midAppId, midRouteId);
				String blueAppId = api.getAppId(desiredApp.getName());
				api.deleteApp(blueAppId);
				api.updateApp(midAppId, desiredApp);
			}
		};
	}
	
	public Step rollback() {
		return new Step(String.format("rollback BlueGreen %s.%s", api.getSiteName(), desiredApp.getName())) {
			@Override
			public void exec() {
				String midAppName = desiredApp.getName() + desiredApp.getVersion();
				String midAppId = api.getAppId(midAppName);
				api.deleteApp(midAppId);
			}
		};
	}
}
