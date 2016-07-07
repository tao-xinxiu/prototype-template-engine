package com.orange.model;

public abstract class Step {
	protected String stepName;

	public Step(String stepName) {
		this.stepName = stepName;
	}

	public abstract void exec();

	@Override
	public String toString() {
		return stepName;
	}
}
