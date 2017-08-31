package com.orange.model;

public class OperationConfig {
    private int generalTimeout = 5 * 60;
    private int connectTimeout = 120;
    private int generalRetry = 3;
    private int generalBackoff = 5;
    private int uploadTimeout = 20 * 60;
    private int prepareTimeout = 15 * 60;
    private int startTimeout = 20 * 60;
    private boolean parallelUpdateMicroservices = false;

    public OperationConfig() {
    }

    public int getGeneralTimeout() {
	return generalTimeout;
    }

    public void setGeneralTimeout(int generalTimeout) {
	this.generalTimeout = generalTimeout;
    }

    public int getConnectTimeout() {
	return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
	this.connectTimeout = connectTimeout;
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

    public int getGeneralRetry() {
	return generalRetry;
    }

    public void setGeneralRetry(int generalRetry) {
	this.generalRetry = generalRetry;
    }

    public int getGeneralBackoff() {
	return generalBackoff;
    }

    public void setGeneralBackoff(int generalBackoff) {
	this.generalBackoff = generalBackoff;
    }

    public boolean isParallelUpdateMicroservices() {
        return parallelUpdateMicroservices;
    }

    public void setParallelUpdateMicroservices(boolean parallelUpdateMicroservices) {
        this.parallelUpdateMicroservices = parallelUpdateMicroservices;
    }

    @Override
    public String toString() {
	return "OperationConfig [generalTimeout=" + generalTimeout + ", connectTimeout=" + connectTimeout
		+ ", generalRetry=" + generalRetry + ", generalBackoff=" + generalBackoff + ", uploadTimeout="
		+ uploadTimeout + ", prepareTimeout=" + prepareTimeout + ", startTimeout=" + startTimeout
		+ ", parallelUpdateMicroservices=" + parallelUpdateMicroservices + "]";
    }
}
