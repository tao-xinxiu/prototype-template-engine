package com.orange.model;

import java.util.Map;

public class PaaSSite {
	private String name;
	private PaaSAccessInfo accessInfo;
	private Map<String, String> domains;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public PaaSAccessInfo getAccessInfo() {
		return accessInfo;
	}

	public void setAccessInfo(PaaSAccessInfo accessInfo) {
		this.accessInfo = accessInfo;
	}
	
	public Map<String, String> getDomains() {
		return domains;
	}

	public void setDomains(Map<String, String> domains) {
		this.domains = domains;
	}

	public boolean valid() {
		if (accessInfo != null && accessInfo.valid() && validDomain()) {
			return true;
		} else {
			return false;
		}
	}

	private boolean validDomain() {
		if (domains == null) {
			return false;
		} else if (domains.get("local") == null || domains.get("global") == null || domains.get("tmp") == null) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String toString() {
		return String.format(
				"{name: %s; accessInfo: %s; domains:%s}", name, accessInfo, domains);
	}
}
