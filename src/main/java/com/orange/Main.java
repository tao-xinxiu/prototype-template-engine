package com.orange;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSSiteAccess;
import com.orange.model.workflow.Workflow;
import com.orange.paas.cf.CloudFoundryAPIv2;
import com.orange.paas.cf.CloudFoundryOperations;
import com.orange.reconfig.WorkflowCalculator;
import com.orange.strategy.NextArchitectureCalculator;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    // storePath and NextArchitectureCalculator(depending on strategy &
    // StrategyConfig) are supposed to be specific to a user
    public static final String storePath = "./store/";
    private static final String strategyPackage = "com.orange.strategy.impl.";
    private static NextArchitectureCalculator nextArchitectureCalculator;
    private static OperationConfig operationConfig = new OperationConfig();
    // private static Map<String, PaaSAPI> connectedSites = new HashMap<>();
    private static Map<String, CloudFoundryOperations> connectedSites = new HashMap<>();

    public Architecture pull(Collection<PaaSSiteAccess> managingSites) {
	Architecture currentArchitecture = new Architecture();
	for (PaaSSiteAccess siteAccess : managingSites) {
	    Set<Microservice> microservices = new CloudFoundryAPIv2(siteAccess, operationConfig).get();
	    currentArchitecture.addSite(siteAccess, microservices);
	}
	logger.info("Got current architecture: {} ", currentArchitecture);
	return currentArchitecture;
    }

    public Architecture push(Architecture desiredArchitecture) {
	Architecture currentArchitecture = pullAndStabilize(desiredArchitecture.listPaaSSites());
	Workflow reconfigureWorkflow = new WorkflowCalculator(currentArchitecture, desiredArchitecture, operationConfig)
		.getReconfigureWorkflow();
	reconfigureWorkflow.exec();
	logger.info("Workflow {} finished!", reconfigureWorkflow);
	return pull(desiredArchitecture.listPaaSSites());
    }

    public Architecture next(Architecture finalArchitecture) {
	if (nextArchitectureCalculator == null) {
	    throw new IllegalStateException("Strategy config not yet set.");
	}
	Architecture currentArchitecture = pullAndStabilize(finalArchitecture.listPaaSSites());
	return nextArchitectureCalculator.nextArchitecture(currentArchitecture, finalArchitecture);
    }

    public void setStrategyConfig(String strategy, StrategyConfig config) {
	strategy = strategyPackage + strategy;
	nextArchitectureCalculator = new NextArchitectureCalculator(strategy, config);
	logger.info("Strategy set: [{}]. Update config set: [{}]", strategy, config);
    }

    public void setOperationConfig(OperationConfig config) {
	operationConfig = config;
	logger.info("Operation config set! [{}]", config);
    }

    public boolean isInstantiation(Architecture desiredArchitecture) {
	Architecture currentArchitecture = pull(desiredArchitecture.listPaaSSites());
	return currentArchitecture.isInstantiation(desiredArchitecture);
    }

    public static CloudFoundryOperations getCloudFoundryOperations(PaaSSiteAccess site, OperationConfig config) {
	CloudFoundryOperations ops = connectedSites.get(site.getName());
	if (ops == null) {
	    ops = new CloudFoundryOperations(site, config);
	    connectedSites.put(site.getName(), ops);
	}
	return ops;
    }

    /**
     * Get the current architecture and stabilize it. (i.e. stop the currently
     * starting microservices)
     * 
     * @param managingSites
     * @return
     */
    private Architecture pullAndStabilize(Collection<PaaSSiteAccess> managingSites) {
	Architecture currentArchitecture = new Architecture();
	for (PaaSSiteAccess siteAccess : managingSites) {
	    Set<Microservice> microservices = new CloudFoundryAPIv2(siteAccess, operationConfig)
		    .getStabilizedMicroservices();
	    currentArchitecture.addSite(siteAccess, microservices);
	}
	logger.info("Got and stabilized current architecture: {} ", currentArchitecture);
	return currentArchitecture;
    }

    public static void main(String[] args) throws ParseException {
	if (args.length < 1) {
	    throw new IllegalArgumentException("Missing command.");
	}
	Options options = new Options();
	CommandLineParser parser = new DefaultParser();
	String[] optionArgs = Arrays.copyOfRange(args, 1, args.length);
	switch (args[0]) {
	case "pull":
	    System.out.println("pulling the current architecture ...");
	    options.addOption("sites", true, "related PaaS sites");
	    CommandLine cli = parser.parse(options, optionArgs);
	    System.out.println(cli.getOptionValue("sites"));
	    break;
	case "push":
	    System.out.println("pushing to the desired architecture ...");
	    break;
	case "next":
	    System.out.println("calculating the next desired architecture ...");
	    break;
	case "arrived":
	    System.out.println("calculating whether the desired architecture arrived ...");
	    break;
	default:
	    break;
	}
    }
}
