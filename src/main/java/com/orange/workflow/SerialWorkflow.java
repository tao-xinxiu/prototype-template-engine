package com.orange.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialWorkflow extends Workflow {
	private static final Logger logger = LoggerFactory.getLogger(SerialWorkflow.class);
	
	public SerialWorkflow(String stepName) {
		super(stepName);
	}

	@Override
	public void exec() {
		for (Step step : steps) {
			logger.info("start {} ...", step);
			step.exec();
			logger.info("Step {} Done!", step);
		}
	}
}
