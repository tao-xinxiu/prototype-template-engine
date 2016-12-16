package com.orange.paas;

import java.util.List;
import java.util.Map;

import com.orange.model.*;

public abstract class PaaSAPI {
	protected PaaSSite site;

	public PaaSAPI(PaaSSite site) {
		this.site = site;
	}

	public String getSiteName() {
		return site.getName();
	}

	public abstract OverviewSite getOverviewSite();

	/**
	 * create and upload app package; i.e. app state: nonexistent -> created
	 * 
	 * @param app
	 * @return
	 */
	public abstract String createAppWaitUploaded(OverviewApp app);

	/**
	 * stage app; i.e. app state: created -> staged
	 * 
	 * @param app
	 */
	public abstract void stageAppWaitStaged(String appId);

	/**
	 * start app; i.e. app state: staged -> running
	 * 
	 * @param appId
	 */
	public abstract void startAppWaitRunning(String appId);

	/**
	 * app state: created -> running
	 * 
	 * @param appId
	 */
	public abstract void stageAndStartAppWaitRunning(String appId);

	/**
	 * app state: running -> staged
	 * 
	 * @param appId
	 */
	public abstract void stopApp(String appId);

	public abstract void deleteApp(String appId);

	public abstract void updateAppName(String appId, String name);

	public abstract void updateAppEnv(String appId, Map<String, String> env);

	public abstract void scaleApp(String appId, int instances);

	/**
	 * operations to make env change taking effect, ex. restage or restart
	 * 
	 * @param appId
	 */
	public abstract void propagateEnvChange(String appId);

	public abstract List<Route> listAppRoutes(String appId);

	public abstract void createAndMapAppRoutes(String appId, List<Route> routes);

	public abstract void unmapAppRoutes(String appId, List<Route> routes);
}
