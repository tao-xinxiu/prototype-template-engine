package com.orange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import com.orange.model.architecture.Site;
import com.orange.model.architecture.cf.CFMicroservice;
import com.orange.model.architecture.k8s.K8sMicroservice;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSSiteAccess;
import com.orange.model.workflow.Workflow;
import com.orange.paas.PaaSAPI;
import com.orange.reconfig.WorkflowCalculator;
import com.orange.strategy.NextArchitectureCalculator;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String strategyPackage = "com.orange.strategy.impl.";

    private static Architecture pull(Collection<PaaSSiteAccess> managingSites, OperationConfig opConfig) {
	Architecture currentArchitecture = new Architecture();
	for (PaaSSiteAccess siteAccess : managingSites) {
	    PaaSAPI api = WorkflowCalculator.parsePaaSApi(siteAccess, opConfig);
	    Set<Microservice> microservices = api.get();
	    currentArchitecture.addSite(siteAccess, microservices);
	}
	logger.info("Got current architecture: {} ", currentArchitecture);
	return currentArchitecture;
    }

    private static void push(Architecture desiredArchitecture, OperationConfig opConfig) {
	desiredArchitecture.valid();
	Architecture currentArchitecture = pull(desiredArchitecture.listSitesAccess(), opConfig);
	Workflow reconfigureWorkflow = new WorkflowCalculator(currentArchitecture, desiredArchitecture, opConfig)
		.getReconfigureWorkflow();
	reconfigureWorkflow.exec();
	logger.info("Workflow {} finished.", reconfigureWorkflow);
	logger.info("Pushed the architecture: " + desiredArchitecture);
    }

    private static List<Architecture> preview(Architecture initArchitecture, Architecture finalArchitecture,
	    String strategy, StrategyConfig strategyConfig) {
	List<Architecture> archSequence = new ArrayList<>();
	Architecture currentArchitecture = initArchitecture;
	while (true) {
	    Architecture nextArchitecture = next(currentArchitecture, finalArchitecture, strategy, strategyConfig);
	    logger.info("preview calculated the next architecture: " + nextArchitecture);
	    if (nextArchitecture == null) {
		logger.info("preview reached the final architecture: " + finalArchitecture);
		break;
	    }
	    archSequence.add(nextArchitecture);
	    currentArchitecture = nextArchitecture;
	}
	return archSequence;
    }

    private static List<Architecture> preview(Architecture finalArchitecture, String strategy,
	    StrategyConfig strategyConfig, OperationConfig opConfig) {
	Architecture currentArchitecture = pull(finalArchitecture.listSitesAccess(), opConfig);
	return preview(currentArchitecture, finalArchitecture, strategy, strategyConfig);
    }

    private static void update(Architecture finalArchitecture, String strategy, StrategyConfig strategyConfig,
	    OperationConfig opConfig) {
	while (true) {
	    Architecture nextArchitecture = next(finalArchitecture, strategy, strategyConfig, opConfig);
	    logger.info("calculated the next architecture: " + nextArchitecture);
	    if (nextArchitecture == null) {
		logger.info("updated to the final architecture: " + finalArchitecture);
		System.out.println(true);
		break;
	    }
	    push(nextArchitecture, opConfig);
	}
    }

    private static Architecture next(Architecture currentArchitecture, Architecture finalArchitecture, String strategy,
	    StrategyConfig config) {
	strategy = strategyPackage + strategy;
	NextArchitectureCalculator nextArchitectureCalculator = new NextArchitectureCalculator(strategy, config);
	logger.info("Using strategy: [{}]. Using strategy config: [{}]", strategy, config);
	return nextArchitectureCalculator.nextArchitecture(currentArchitecture, finalArchitecture);
    }

    private static Architecture next(Architecture finalArchitecture, String strategy, StrategyConfig config,
	    OperationConfig opConfig) {
	Architecture currentArchitecture = pull(finalArchitecture.listSitesAccess(), opConfig);
	return next(currentArchitecture, finalArchitecture, strategy, config);
    }

    private static boolean isInstantiation(Architecture desiredArchitecture, OperationConfig opConfig) {
	Architecture currentArchitecture = pull(desiredArchitecture.listSitesAccess(), opConfig);
	return currentArchitecture.isInstantiation(desiredArchitecture);
    }

    private static OperationConfig parseOpConfig(CommandLine cli)
	    throws JsonParseException, JsonMappingException, IOException {
	OperationConfig opConfig;
	if (cli.hasOption("oc")) {
	    opConfig = new ObjectMapper().readValue(new File(cli.getOptionValue("oc")), OperationConfig.class);
	    logger.info("Using the customized operation config: [{}]", opConfig);
	} else {
	    opConfig = new OperationConfig();
	    logger.info("Using the default operation config: [{}]", opConfig);
	}
	return opConfig;
    }

    private static Architecture parseArchitecture(String optionValue)
	    throws JsonParseException, JsonMappingException, IOException {
	Architecture finalArchitecture = new ObjectMapper().readValue(new File(optionValue), Architecture.class);
	for (Site site : finalArchitecture.getSites().values()) {
	    switch (site.getSiteAccess().getType()) {
	    case "CloudFoundry":
		Set<Microservice> cfMicroservices = new HashSet<>();
		for (Microservice microservice : site.getMicroservices()) {
		    cfMicroservices.add(new CFMicroservice(microservice));
		}
		site.setMicroservices(cfMicroservices);
		break;
	    case "Kubernetes":
		Set<Microservice> k8sMicroservices = new HashSet<>();
		for (Microservice microservice : site.getMicroservices()) {
		    k8sMicroservices.add(new K8sMicroservice(microservice));
		}
		site.setMicroservices(k8sMicroservices);
		break;
	    default:
		throw new IllegalArgumentException("Unknown PaaS site type: " + site.getSiteAccess().getType());
	    }
	}
	finalArchitecture.valid();
	return finalArchitecture;
    }

    public static void main(String[] args)
	    throws ParseException, JsonParseException, JsonMappingException, IOException {
	if (args.length < 1) {
	    throw new IllegalArgumentException("Missing command (pull, push, next, arrived, or update).");
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
	options.addOption(opConfigOpt); // all the commands need the option opConfig

	CommandLine cli;
	ObjectMapper mapper = new ObjectMapper();
	switch (args[0]) {
	case "pull":
	    logger.info("pulling the current architecture ...");
	    options.addOption(sitesOpt);
	    cli = parser.parse(options, optionArgs);
	    Collection<PaaSSiteAccess> sites = mapper.readValue(new File(cli.getOptionValue("s")),
		    mapper.getTypeFactory().constructCollectionType(Collection.class, PaaSSiteAccess.class));
	    Architecture currentArchitecture = pull(sites, parseOpConfig(cli));
	    logger.info("got the current architecture: " + currentArchitecture);
	    System.out.println(mapper.writeValueAsString(currentArchitecture));
	    break;
	case "push":
	    logger.info("pushing to the desired architecture in the most direct way ...");
	    options.addOption(architectureOpt);
	    cli = parser.parse(options, optionArgs);
	    Architecture desiredArchitecture = parseArchitecture(cli.getOptionValue("a"));
	    push(desiredArchitecture, parseOpConfig(cli));
	    logger.info("pushed the desired architecture: " + desiredArchitecture);
	    System.out.println(true);
	    break;
	case "next":
	    logger.info("calculating the next desired architecture of the strategy ...");
	    options.addOption(architectureOpt);
	    options.addOption(strategyOpt);
	    options.addOption(strategyConfigOpt);
	    cli = parser.parse(options, optionArgs);
	    String strategyName = cli.getOptionValue("sn");
	    StrategyConfig strategyConfig = mapper.readValue(new File(cli.getOptionValue("sc")), StrategyConfig.class);
	    Architecture finalArchitecture = parseArchitecture(cli.getOptionValue("a"));
	    Architecture nextArchitecture = next(finalArchitecture, strategyName, strategyConfig, parseOpConfig(cli));
	    logger.info(strategyName + " calculated the next architecture: " + nextArchitecture);
	    System.out.println(mapper.writeValueAsString(nextArchitecture));
	    break;
	case "arrived":
	    logger.info("calculating whether the desired architecture arrived ...");
	    options.addOption(architectureOpt);
	    cli = parser.parse(options, optionArgs);
	    Architecture arrivedArchitecture = parseArchitecture(cli.getOptionValue("a"));
	    System.out.println(isInstantiation(arrivedArchitecture, parseOpConfig(cli)));
	    break;
	case "update":
	    logger.info("updating the microservices to the final architecture by following a strategy ...");
	    options.addOption(architectureOpt);
	    options.addOption(strategyOpt);
	    options.addOption(strategyConfigOpt);
	    cli = parser.parse(options, optionArgs);
	    String updStrategy = cli.getOptionValue("sn");
	    StrategyConfig updStrategyConfig = mapper.readValue(new File(cli.getOptionValue("sc")),
		    StrategyConfig.class);
	    OperationConfig opConfig = parseOpConfig(cli);
	    Architecture updFinalArchitecture = parseArchitecture(cli.getOptionValue("a"));
	    update(updFinalArchitecture, updStrategy, updStrategyConfig, opConfig);
	    break;
	case "preview":
	    logger.info("previewing the intermediate architectures by following a strategy ...");
	    options.addOption(architectureOpt);
	    options.addOption(strategyOpt);
	    options.addOption(strategyConfigOpt);
	    cli = parser.parse(options, optionArgs);
	    String prevStrategy = cli.getOptionValue("sn");
	    StrategyConfig prevStrategyConfig = mapper.readValue(new File(cli.getOptionValue("sc")),
		    StrategyConfig.class);
	    OperationConfig prevOpConfig = parseOpConfig(cli);
	    Architecture prevFinalArch = parseArchitecture(cli.getOptionValue("a"));
	    List<Architecture> archSeq = preview(prevFinalArch, prevStrategy, prevStrategyConfig, prevOpConfig);
	    System.out.println(mapper.writeValueAsString(archSeq));
	    break;
	default:
	    throw new IllegalArgumentException(String.format(
		    "Unknown command: [%s]. Please use one of the following valid commands: pull, push, next, arrived, or update.",
		    args[0]));
	}
    }
}
