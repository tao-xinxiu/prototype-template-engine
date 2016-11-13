package com.orange.model;

import java.util.ArrayList;
import java.util.List;

public class OverviewSite {
	private PaaSSite site;
	private List<OverviewApp> overviewApps;

	public OverviewSite(PaaSSite site) {
		this.site = site;
		this.overviewApps = new ArrayList<>();
	}

	public PaaSSite getSite() {
		return site;
	}

	public List<OverviewApp> getOverviewApps() {
		return overviewApps;
	}

	public void addOverviewApp(OverviewApp overviewApp) {
		this.overviewApps.add(overviewApp);
	}
}
