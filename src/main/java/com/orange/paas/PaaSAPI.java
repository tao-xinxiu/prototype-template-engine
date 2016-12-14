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

	public abstract String createAppIfNotExist(OverviewApp app);

	public abstract void prepareApp(OverviewApp app);

	public abstract void startAppAndWaitUntilRunning(String appId);

	public abstract void stopApp(String appId);

	public abstract void deleteApp(String appId);

	public abstract void updateAppName(String appId, String name);

	public abstract void updateAppEnv(String appId, Map<String, String> env);

	public abstract void scaleApp(String appId, int instances);

	public abstract List<Route> listAppRoutes(String appId);

	public abstract void mapAppRoutes(String appId, List<Route> routes);

	public abstract void unmapAppRoutes(String appId, List<Route> routes);
}
