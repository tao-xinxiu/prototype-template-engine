package com.orange.model;

public class PaaSSite {
	private String name;
	private PaaSAccessInfo accessInfo;

	public PaaSSite() {
	}

	public PaaSSite(String name, PaaSAccessInfo accessInfo) {
		this.name = name;
		this.accessInfo = accessInfo;
	}

	public PaaSSite(PaaSSite other) {
		this.name = other.name;
		this.accessInfo = new PaaSAccessInfo(other.accessInfo);
	}

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

	public boolean valid() {
		if (accessInfo != null && accessInfo.valid()) {
			return true;
		} else {
			return false;
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
