package com.orange.model;

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
}
