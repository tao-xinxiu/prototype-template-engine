package com.orange.model;

public class SiteDeploymentConfig {
	private String temporaryNameSuffix = "-UPDATING";
	private String temporaryRoute;
	// TODO add timeout, retry times of deployment operations

	public String getTemporaryNameSuffix() {
		return temporaryNameSuffix;
	}

	public void setTemporaryNameSuffix(String temporaryNameSuffix) {
		this.temporaryNameSuffix = temporaryNameSuffix;
	}

	public String getTemporaryRoute() {
		return temporaryRoute;
	}

	public void setTemporaryRoute(String temporaryRoute) {
		this.temporaryRoute = temporaryRoute;
	}

}
