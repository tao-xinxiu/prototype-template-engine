package com.orange.model;

public class PaaSTarget {
	private String name;
	private String api;
	private String user;
	private String pwd;
	private String org;
	private String space;
	private boolean skipSslValidation;
	private String localDomain;
	private String globalDomain;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String api) {
		this.api = api;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		if (user.startsWith("$")) {
			user = System.getenv(user.substring(1));
		}
		this.user = user;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		if (pwd.startsWith("$")) {
			pwd = System.getenv(pwd.substring(1));
		}
		this.pwd = pwd;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public String getSpace() {
		return space;
	}

	public void setSpace(String space) {
		this.space = space;
	}

	public boolean getSkipSslValidation() {
		return skipSslValidation;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	public String getLocalDomain() {
		return localDomain;
	}

	public void setLocalDomain(String localDomain) {
		this.localDomain = localDomain;
	}

	public String getGlobalDomain() {
		return globalDomain;
	}

	public void setGlobalDomain(String globalDomain) {
		this.globalDomain = globalDomain;
	}

	public boolean valid() {
		if (api != null && user != null && pwd != null && org != null && space != null && localDomain != null
				&& globalDomain != null) {
			return true;
		} else {
			return false;
		}
	}
}