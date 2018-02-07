package com.orange.model;

public class PaaSSite {
    private String name;
    private String type; // ex. "CloudFoundry", "Heroku"
    private String api;
    private String user;
    private String pwd;
    private String org;
    private String space;
    private boolean skipSslValidation;

    public PaaSSite() {
    }

    public PaaSSite(String name, String type, String api, String user, String pwd, String org, String space,
	    boolean skipSslValidation) {
	this.name = name;
	this.type = type;
	this.api = api;
	this.user = user;
	this.pwd = pwd;
	this.org = org;
	this.space = space;
	this.skipSslValidation = skipSslValidation;
    }

    public PaaSSite(PaaSSite other) {
	this.name = other.name;
	this.type = other.type;
	this.api = other.api;
	this.user = other.user;
	this.pwd = other.pwd;
	this.org = other.org;
	this.space = other.space;
	this.skipSslValidation = other.skipSslValidation;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
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

    public boolean valid() {
	if (api != null && user != null && pwd != null && org != null && space != null) {
	    return true;
	} else {
	    return false;
	}
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

    @Override
    public String toString() {
	return String.format("{api: %s; user: %s; pwd: %s; org:%s; space:%s; skipSslValidation:%s}", api, user, pwd,
		org, space, skipSslValidation);
    }

}
