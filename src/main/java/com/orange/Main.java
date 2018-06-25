package com.orange;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.model.StrategyConfig;
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.Microservice;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public static Architecture pull(Collection<PaaSSiteAccess> managingSites) {
	Architecture currentArchitecture = new Architecture();
	for (PaaSSiteAccess siteAccess : managingSites) {
	    Set<Microservice> microservices = new CloudFoundryAPIv2(siteAccess, operationConfig).get();
	    currentArchitecture.addSite(siteAccess, microservices);
	}
	logger.info("Got current architecture: {} ", currentArchitecture);
	return currentArchitecture;
    }

    public static Architecture push(Architecture desiredArchitecture) {
	Architecture currentArchitecture = pullAndStabilize(desiredArchitecture.listPaaSSites());
	Workflow reconfigureWorkflow = new WorkflowCalculator(currentArchitecture, desiredArchitecture, operationConfig)
		.getReconfigureWorkflow();
	reconfigureWorkflow.exec();
	logger.info("Workflow {} finished!", reconfigureWorkflow);
	return pull(desiredArchitecture.listPaaSSites());
    }

    public static Architecture next(Architecture finalArchitecture) {
	if (nextArchitectureCalculator == null) {
	    throw new IllegalStateException("Strategy config not yet set.");
	}
	Architecture currentArchitecture = pullAndStabilize(finalArchitecture.listPaaSSites());
	return nextArchitectureCalculator.nextArchitecture(currentArchitecture, finalArchitecture);
    }

    public static void setStrategyConfig(String strategy, StrategyConfig config) {
	strategy = strategyPackage + strategy;
	nextArchitectureCalculator = new NextArchitectureCalculator(strategy, config);
	logger.info("Strategy set: [{}]. Update config set: [{}]", strategy, config);
    }

    public static void setOperationConfig(OperationConfig config) {
	operationConfig = config;
	logger.info("Operation config set! [{}]", config);
    }

    public static boolean isInstantiation(Architecture desiredArchitecture) {
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
    private static Architecture pullAndStabilize(Collection<PaaSSiteAccess> managingSites) {
	Architecture currentArchitecture = new Architecture();
	for (PaaSSiteAccess siteAccess : managingSites) {
	    Set<Microservice> microservices = new CloudFoundryAPIv2(siteAccess, operationConfig)
		    .getStabilizedMicroservices();
	    currentArchitecture.addSite(siteAccess, microservices);
	}
	logger.info("Got and stabilized current architecture: {} ", currentArchitecture);
	return currentArchitecture;
    }

    public static void main(String[] args)
	    throws ParseException, JsonParseException, JsonMappingException, IOException {
	if (args.length < 1) {
	    throw new IllegalArgumentException("Missing command (pull, push, next, or arrived).");
	}
	Options options = new Options();
	CommandLineParser parser = new DefaultParser();
	String[] optionArgs = Arrays.copyOfRange(args, 1, args.length);
	Option sitesOpt = Option.builder("s").longOpt("sites").desc("the related PaaS sites").hasArg().argName("sites")
		.required().build();
	Option opConfigOpt = Option.builder("oc").longOpt("opConfig").desc("the PaaS operations configuration file")
		.hasArg().argName("opConfigFile").build();
	Option architectureOpt = Option.builder("a").longOpt("architecture").desc("the desired architecture").hasArg()
		.argName("architecture").required().build();
	Option strategyOpt = Option.builder("sn").longOpt("strategy").desc("the choosen strategy name").hasArg()
		.argName("strategyName").required().build();
	Option strategyConfigOpt = Option.builder("sc").longOpt("strategyConfig")
		.desc("the strategy configuration file").hasArg().argName("strategyConfigFile").required().build();
	CommandLine cli;
	ObjectMapper mapper = new ObjectMapper();
	switch (args[0]) {
	case "pull":
	    logger.info("pulling the current architecture ...");
	    options.addOption(sitesOpt);
	    options.addOption(opConfigOpt);
	    cli = parser.parse(options, optionArgs);
	    if (cli.hasOption("oc")) {
		OperationConfig opConfig = mapper.readValue(new File(cli.getOptionValue("oc")), OperationConfig.class);
		setOperationConfig(opConfig);
	    }
	    Collection<PaaSSiteAccess> sites = mapper.readValue(new File(cli.getOptionValue("s")),
		    mapper.getTypeFactory().constructCollectionType(Collection.class, PaaSSiteAccess.class));
	    System.out.println(mapper.writeValueAsString(pull(sites)));
	    break;
	case "push":
	    logger.info("pushing to the desired architecture ...");
	    options.addOption(architectureOpt);
	    options.addOption(opConfigOpt);
	    cli = parser.parse(options, optionArgs);
	    if (cli.hasOption("oc")) {
		OperationConfig opConfig = mapper.readValue(new File(cli.getOptionValue("oc")), OperationConfig.class);
		setOperationConfig(opConfig);
	    }
	    Architecture desiredArchitecture = mapper.readValue(new File(cli.getOptionValue("a")), Architecture.class);
	    System.out.println(mapper.writeValueAsString(push(desiredArchitecture)));
	    break;
	case "next":
	    logger.info("calculating the next desired architecture ...");
	    options.addOption(architectureOpt);
	    options.addOption(strategyOpt);
	    options.addOption(strategyConfigOpt);
	    cli = parser.parse(options, optionArgs);
	    String strategyName = cli.getOptionValue("sn");
	    StrategyConfig strategyConfig = mapper.readValue(new File(cli.getOptionValue("sc")), StrategyConfig.class);
	    setStrategyConfig(strategyName, strategyConfig);
	    Architecture finalArchitecture = mapper.readValue(new File(cli.getOptionValue("a")), Architecture.class);
	    System.out.println(mapper.writeValueAsString(next(finalArchitecture)));
	    break;
	case "arrived":
	    logger.info("calculating whether the desired architecture arrived ...");
	    options.addOption(architectureOpt);
	    cli = parser.parse(options, optionArgs);
	    Architecture arrivedArchitecture = mapper.readValue(new File(cli.getOptionValue("a")), Architecture.class);
	    System.out.println(isInstantiation(arrivedArchitecture));
	    break;
	default:
	    throw new IllegalArgumentException(String.format(
		    "Unknown command: [%s]. Please use one of the following valid commands: pull, push, next, or arrived.",
		    args[0]));
	}
    }
}
