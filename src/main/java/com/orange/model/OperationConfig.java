package com.orange.model;

public class OperationConfig {
    private int generalTimeout = 60;
    private int uploadTimeout = 15 * 60;
    private int prepareTimeout = 15 * 60;
    private int startTimeout = 15 * 60;

    public int getGeneralTimeout() {
	return generalTimeout;
    }

    public void setGeneralTimeout(int generalTimeout) {
	this.generalTimeout = generalTimeout;
    }

    public int getUploadTimeout() {
	return uploadTimeout;
    }

    public void setUploadTimeout(int uploadTimeout) {
	this.uploadTimeout = uploadTimeout;
    }

    public int getPrepareTimeout() {
	return prepareTimeout;
    }

    public void setPrepareTimeout(int prepareTimeout) {
	this.prepareTimeout = prepareTimeout;
    }

    public int getStartTimeout() {
	return startTimeout;
    }

    public void setStartTimeout(int startTimeout) {
	this.startTimeout = startTimeout;
    }

}
