package com.orange.model;

import java.util.ArrayList;
import java.util.List;

public class OverviewSite {
	private List<OverviewApp> overviewApps = new ArrayList<>();
	// TODO add overviewServices

	public OverviewSite() {
	}

	public OverviewSite(List<OverviewApp> overviewApps) {
		this.overviewApps = overviewApps;
	}

	public List<OverviewApp> getOverviewApps() {
		return overviewApps;
	}

	public void setOverviewApps(List<OverviewApp> overviewApps) {
		this.overviewApps = overviewApps;
	}
}
