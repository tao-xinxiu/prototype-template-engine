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
import com.orange.model.architecture.Architecture;
import com.orange.model.architecture.ArchitectureSite;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSSite;
import com.orange.model.workflow.Workflow;
import com.orange.paas.cf.CloudFoundryAPIv2;
import com.orange.paas.cf.CloudFoundryOperations;
import com.orange.reconfig.WorkflowCalculator;
import com.orange.strategy.NextArchitectureCalculator;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class })
@RestController
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    // storePath and NextArchitectureCalculator(depending on strategy &
    // StrategyConfig) are supposed to be specific to a user
    public static final String storePath = "./store/";
    private static final String strategyPackage = "com.orange.strategy.impl";
    private static NextArchitectureCalculator nextArchitectureCalculator;
    private static OperationConfig operationConfig = new OperationConfig();
    // private static Map<String, PaaSAPI> connectedSites = new HashMap<>();
    private static Map<String, CloudFoundryOperations> connectedSites = new HashMap<>();

    @RequestMapping(value = "/pull", method = RequestMethod.PUT)
    public @ResponseBody Architecture getCurrentArchitecture(@RequestBody Collection<PaaSSite> managingSites) {
	Map<String, PaaSSite> sites = managingSites.stream()
		.collect(Collectors.toMap(site -> site.getName(), site -> site));
	Map<String, ArchitectureSite> architectureSites = managingSites.parallelStream().collect(Collectors.toMap(
		site -> site.getName(), site -> new CloudFoundryAPIv2(site, operationConfig).getSiteArchitecture()));
	Architecture currentArchitecture = new Architecture(sites, architectureSites);
	logger.info("Got current architecture: {} ", currentArchitecture);
	return currentArchitecture;
    }

    @RequestMapping(value = "/push", method = RequestMethod.POST)
    public @ResponseBody Architecture pushArchitecture(@RequestBody Architecture desiredArchitecture) {
	Architecture currentArchitecture = getCurrentStableArchitecture(desiredArchitecture.listPaaSSites());
	Workflow reconfigureWorkflow = new WorkflowCalculator(currentArchitecture, desiredArchitecture, operationConfig)
		.getReconfigureWorkflow();
	reconfigureWorkflow.exec();
	logger.info("Workflow {} finished!", reconfigureWorkflow);
	return getCurrentArchitecture(desiredArchitecture.listPaaSSites());
    }

    @RequestMapping(value = "/next", method = RequestMethod.POST)
    public @ResponseBody Architecture nextArchitecture(@RequestBody Architecture finalArchitecture) {
	if (nextArchitectureCalculator == null) {
	    throw new IllegalStateException("Strategy config not yet set.");
	}
	Architecture currentArchitecture = getCurrentStableArchitecture(finalArchitecture.listPaaSSites());
	return nextArchitectureCalculator.nextArchitecture(currentArchitecture, finalArchitecture);
    }

    @RequestMapping(value = "/set_strategy_config", method = RequestMethod.PUT)
    public void setStrategyConfig(@RequestParam("strategy") String strategy, @RequestBody StrategyConfig config) {
	strategy = strategyPackage + strategy;
	nextArchitectureCalculator = new NextArchitectureCalculator(strategy, config);
	logger.info("Strategy set: [{}]. Update config set: [{}]", strategy, config);
    }

    @RequestMapping(value = "/set_operation_config", method = RequestMethod.PUT)
    public void setOperationConfig(@RequestBody OperationConfig config) {
	operationConfig = config;
	logger.info("Operation config set! [{}]", config);
    }

    @RequestMapping(value = "/is_instantiation", method = RequestMethod.POST)
    public boolean isInstantiation(@RequestBody Architecture desiredArchitecture) {
	if (nextArchitectureCalculator == null) {
	    throw new IllegalStateException("Strategy config not yet set.");
	}
	Architecture currentArchitecture = getCurrentArchitecture(desiredArchitecture.listPaaSSites());
	return currentArchitecture.isInstantiation(desiredArchitecture);
    }

    @RequestMapping(value = "health", method = RequestMethod.GET)
    public String health() {
	return "UP";
    }

    /**
     * Upload the microservice binary or source file which is supposed to be
     * uploaded to the PaaS
     * 
     * @param file
     * @return the stored file name, which is supposed to fill out microservice
     *         path in the desired architecture description
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

    public static CloudFoundryOperations getCloudFoundryOperations(PaaSSite site, OperationConfig config) {
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
    private Architecture getCurrentStableArchitecture(Collection<PaaSSite> managingSites) {
	Map<String, PaaSSite> sites = managingSites.stream()
		.collect(Collectors.toMap(site -> site.getName(), site -> site));
	Map<String, ArchitectureSite> architectureSites = managingSites.parallelStream()
		.collect(Collectors.toMap(site -> site.getName(),
			site -> new CloudFoundryAPIv2(site, operationConfig).stabilizeSiteArchitecture()));
	Architecture currentArchitecture = new Architecture(sites, architectureSites);
	logger.info("Got and stabilized current architecture: {} ", currentArchitecture);
	return currentArchitecture;
    }

    public static void main(String[] args) {
	File storeDir = new File(storePath);
	if (!storeDir.exists()) {
	    storeDir.mkdirs();
	}
	SpringApplication.run(Main.class, args);
    }
}