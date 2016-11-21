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
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		PaaSSite other = (PaaSSite) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
