package com.orange.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Overview {
	private Map<PaaSSite, List<OverviewApp>> overviewSites;

	public Overview(Map<PaaSSite, List<OverviewApp>> overviewSites) {
		this.overviewSites = overviewSites;
	}

	public Overview() {
		this.overviewSites = new HashMap<>();
	}

	public void addPaaSSite(PaaSSite site, List<OverviewApp> overviewApps) {
		overviewSites.put(site, overviewApps);
	}

	public Map<PaaSSite, List<OverviewApp>> getOverviewSites() {
		return overviewSites;
	}

	public List<OverviewApp> getOverviewApps(PaaSSite site) {
		return overviewSites.get(site);
	}

	public Set<PaaSSite> listPaaSSites() {
		return overviewSites.keySet();
	}

	/**
	 * number of PaaSSite, List<OverviewApp> mappings, i.e. number of PaaSSite
	 * 
	 * @return
	 */
	public int size() {
		return overviewSites.size();
	}
}
