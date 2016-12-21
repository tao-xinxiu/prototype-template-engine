package com.orange.model.state;

public class Route {
	private String hostname;
	private String domain;

	public Route(String hostname, String domain) {
		this.hostname = hostname;
		this.domain = domain;
	}

	public Route(String route) {
		String[] routeSplit = route.split("\\.", 2);
		if (routeSplit.length != 2) {
			throw new IllegalStateException(String.format("route [%s] format error", route));
		}
		hostname = routeSplit[0];
		domain = routeSplit[1];
	}

	public String getHostname() {
		return hostname;
	}

	public String getDomain() {
		return domain;
	}

	@Override
	public String toString() {
		return hostname + "." + domain;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((hostname == null) ? 0 : hostname.hashCode());
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
		Route other = (Route) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (hostname == null) {
			if (other.hostname != null)
				return false;
		} else if (!hostname.equals(other.hostname))
			return false;
		return true;
	}
}
