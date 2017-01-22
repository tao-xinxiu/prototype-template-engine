package com.orange.model.workflow;

import java.util.LinkedList;
import java.util.List;

public abstract class Workflow extends Step {
    protected List<Step> steps = new LinkedList<Step>();

    public Workflow(String stepName) {
	super(stepName);
    }

    public void addStep(Step step) {
	steps.add(step);
    }

    public void addSteps(List<Step> steps) {
	this.steps.addAll(steps);
    }

}
