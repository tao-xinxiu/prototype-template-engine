package com.orange.util;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryFunction<T> {
    private static Logger logger = LoggerFactory.getLogger(RetryFunction.class);
    private int retried;
    private int maxTries;
    private int backoffSec;

    public RetryFunction(int maxTries, int backoffSec) {
	this.maxTries = maxTries;
	this.backoffSec = backoffSec;
    }

    public T run(Supplier<T> function) {
	retried = 0;
	while (++retried < maxTries) {
	    try {
		return function.get();
	    } catch (Exception e) {
		logger.error("Command failed on {} of {} tries.", retried, maxTries);
		logger.error("Exception: ", e);
		if (retried < maxTries) {
		    try {
			Thread.sleep(1000 * backoffSec);
		    } catch (InterruptedException ex) {
			logger.error("InterruptedException", ex);
		    }
		}
	    }
	}
	throw new IllegalStateException(String.format("Command failed on all of %s tries", maxTries));
    }
}