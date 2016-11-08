package com.orange.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParallelWorkflow extends Workflow {
	private static final Logger logger = LoggerFactory.getLogger(ParallelWorkflow.class);
	
	public ParallelWorkflow(String stepName) {
		super(stepName);
	}

	@Override
	public void exec() {
		if (steps.size() == 0) {
			return;
		}
		ExecutorService executor = Executors.newFixedThreadPool(steps.size());
		List<Callable<Void>> tasks = new ArrayList<>();
		for (Step step : steps) {
			tasks.add(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					Thread.currentThread().setName(step.toString());
					logger.info("start {} ...", step);
					step.exec();
					logger.info("Step {} Done!", step);
					return null;
				}
			});
		}
		try {
			List<Future<Void>> futures = executor.invokeAll(tasks);
			for (Future<Void> future : futures) {
				future.get();
			}
			executor.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			throw new IllegalStateException(e);
		}
	}
}
