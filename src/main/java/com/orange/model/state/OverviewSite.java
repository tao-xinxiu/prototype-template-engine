package com.orange.model.state;

import java.util.HashSet;
import java.util.Set;

public class OverviewSite {
    private Set<OverviewApp> overviewApps = new HashSet<>();
    // TODO add overviewServices

    public OverviewSite() {
    }

    public OverviewSite(Set<OverviewApp> overviewApps) {
	this.overviewApps = overviewApps;
    }

    public OverviewSite(OverviewSite other) {
	for (OverviewApp overviewApp : other.overviewApps) {
	    this.overviewApps.add(new OverviewApp(overviewApp));
	}
    }

    public Set<OverviewApp> getOverviewApps() {
	return overviewApps;
    }

    public void setOverviewApps(Set<OverviewApp> overviewApps) {
	this.overviewApps = overviewApps;
    }

    public void addOverviewApp(OverviewApp overviewApp) {
	this.overviewApps.add(overviewApp);
    }

    public void addOverviewApps(Set<OverviewApp> overviewApps) {
	this.overviewApps.addAll(overviewApps);
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((overviewApps == null) ? 0 : overviewApps.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	OverviewSite other = (OverviewSite) obj;
	if (overviewApps == null) {
	    if (other.overviewApps != null)
		return false;
	} else if (!overviewApps.equals(other.overviewApps))
	    return false;
	return true;
    }
}
