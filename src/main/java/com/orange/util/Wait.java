package com.orange.util;

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wait {
    private static Logger logger = LoggerFactory.getLogger(RetryFunction.class);
    private int timeoutSec;
    private int backoffSec = 1;

    public Wait(int timeoutSec) {
	this.timeoutSec = timeoutSec;
    }

    public void waitUntil(Predicate<String> predicate, String predicateText, String parameter) {
	logger.info("start " + predicateText);
	int waitedSec = 0;
	while (!predicate.test(parameter)) {
	    try {
		Thread.sleep(1000 * backoffSec);
	    } catch (InterruptedException e) {
		throw new IllegalStateException(e);
	    }
	    waitedSec += backoffSec;
	    if (waitedSec >= timeoutSec) {
		throw new IllegalStateException("Timeout during waiting " + predicateText);
	    }
	}
	logger.info(predicateText + " finished.");
    }
}
