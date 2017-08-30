package com.orange;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.orange.model.StrategyConfig;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSSite;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewSite;
import com.orange.model.workflow.Workflow;
import com.orange.nextstate.NextStateCalculator;
import com.orange.paas.cf.CloudFoundryAPIv2;
import com.orange.paas.cf.CloudFoundryOperations;
import com.orange.update.WorkflowCalculator;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class })
@RestController
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    // storePath and nextStateCalculator(strategy&config) are specific to user
    public static final String storePath = "./store/";
    private static final String strategyPackage = "com.orange.nextstate.strategy.";
    private static NextStateCalculator nextStateCalculator;
    private static OperationConfig operationConfig = new OperationConfig();
    // private static Map<String, PaaSAPI> connectedSites = new HashMap<>();
    private static Map<String, CloudFoundryOperations> connectedSites = new HashMap<>();

    @RequestMapping(value = "/pull", method = RequestMethod.POST)
    public @ResponseBody Overview getCurrentState(@RequestBody Collection<PaaSSite> managingSites) {
	Map<String, PaaSSite> sites = managingSites.stream()
		.collect(Collectors.toMap(site -> site.getName(), site -> site));
	Map<String, OverviewSite> overviewSites = managingSites.parallelStream().collect(Collectors
		.toMap(site -> site.getName(), site -> new CloudFoundryAPIv2(site, operationConfig).getOverviewSite()));
	Overview currentState = new Overview(sites, overviewSites);
	logger.info("Got current state! {} ", currentState);
	return currentState;
    }

    /**
     * Upload the app binary or source file which is supposed to be uploaded to
     * the PaaS
     * 
     * @param file
     * @return the stored file name, which is supposed to fill out app path in
     *         the desired state description
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public @ResponseBody String handleFileUpload(@RequestParam("file") MultipartFile file) {
	// split original file name base and extension
	String[] fileOriginalNameSplited = file.getOriginalFilename().split("\\.(?=[^\\.]+$)");
	String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
	String fileStoredName = String.format("%s_%s.%s", fileOriginalNameSplited[0], timestamp,
		fileOriginalNameSplited[1]);
	if (!file.isEmpty()) {
	    try {
		byte[] bytes = file.getBytes();
		BufferedOutputStream stream = new BufferedOutputStream(
			new FileOutputStream(new File(storePath + fileStoredName)));
		stream.write(bytes);
		stream.close();
		logger.info("Successfully uploaded {} into {} !", file.getOriginalFilename(), fileStoredName);
		return fileStoredName;
	    } catch (Exception e) {
		throw new IllegalStateException("Failed to upload " + file.getOriginalFilename(), e);
	    }
	} else {
	    throw new IllegalStateException(String.format("Upload file [%s] is empty", file.getOriginalFilename()));
	}
    }

    // users should only clean their proper uploaded files
    @RequestMapping(value = "/cleanFiles", method = RequestMethod.PUT)
    public @ResponseBody String cleanFilesUploaded(@RequestParam("fileName") String fileName) {
	boolean success = new File(storePath + fileName).delete();
	if (success) {
	    return "File is successfully deleted.";
	} else {
	    return "File delete failed.";
	}
    }

    @RequestMapping(value = "/push", method = RequestMethod.POST)
    public @ResponseBody Overview pushState(@RequestBody Overview desiredState) {
	Overview currentState = getCurrentStableState(desiredState.listPaaSSites());
	Workflow updateWorkflow = new WorkflowCalculator(currentState, desiredState, operationConfig)
		.getUpdateWorkflow();
	updateWorkflow.exec();
	logger.info("Workflow {} finished!", updateWorkflow);
	return getCurrentState(desiredState.listPaaSSites());
    }

    @RequestMapping(value = "/next", method = RequestMethod.POST)
    public @ResponseBody Overview calcNextState(@RequestBody Overview finalState) {
	if (nextStateCalculator == null) {
	    throw new IllegalStateException("Update config not yet set.");
	}
	Overview currentState = getCurrentStableState(finalState.listPaaSSites());
	return nextStateCalculator.calcNextStates(currentState, finalState);
    }

    @RequestMapping(value = "/set_strategy_config", method = RequestMethod.PUT)
    public void setStrategyConfig(@RequestParam("strategy") String strategy, @RequestBody StrategyConfig config) {
	strategy = strategyPackage + strategy;
	nextStateCalculator = new NextStateCalculator(strategy, config);
	logger.info("Strategy set: [{}]. Update config set: [{}]", strategy, config);
    }

    @RequestMapping(value = "/set_operation_config", method = RequestMethod.PUT)
    public void setOperationConfig(@RequestBody OperationConfig config) {
	operationConfig = config;
	logger.info("Operation config set! [{}]", config);
    }

    @RequestMapping(value = "/is_instantiation", method = RequestMethod.POST)
    public boolean isInstantiation(@RequestBody Overview desiredState) {
	Overview currentState = getCurrentState(desiredState.listPaaSSites());
	return currentState.isInstantiation(desiredState);
    }

    @RequestMapping(value = "health", method = RequestMethod.GET)
    public String health() {
	return "UP";
    }

    public static CloudFoundryOperations getCloudFoundryOperations(PaaSSite site, OperationConfig config) {
	CloudFoundryOperations ops = connectedSites.get(site.getName());
	if (ops == null) {
	    ops = new CloudFoundryOperations(site, config);
	    connectedSites.put(site.getName(), ops);
	}
	return ops;
    }
    
    private Overview getCurrentStableState(Collection<PaaSSite> managingSites) {
	Map<String, PaaSSite> sites = managingSites.stream()
		.collect(Collectors.toMap(site -> site.getName(), site -> site));
	Map<String, OverviewSite> overviewSites = managingSites.parallelStream().collect(Collectors
		.toMap(site -> site.getName(), site -> new CloudFoundryAPIv2(site, operationConfig).stabilizeOverviewSite()));
	Overview currentState = new Overview(sites, overviewSites);
	logger.info("Got and stabilized current state! {} ", currentState);
	return currentState;
    }

    public static void main(String[] args) {
	File storeDir = new File(storePath);
	if (!storeDir.exists()) {
	    storeDir.mkdirs();
	}
	SpringApplication.run(Main.class, args);
    }
}