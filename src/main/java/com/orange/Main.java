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

import com.orange.midstate.MidStateCalculator;
import com.orange.model.DeploymentConfig;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSSite;
import com.orange.model.state.Overview;
import com.orange.model.state.OverviewApp;
import com.orange.model.state.OverviewSite;
import com.orange.model.workflow.Workflow;
import com.orange.paas.cf.CloudFoundryAPIv2;
import com.orange.paas.cf.CloudFoundryOperations;
import com.orange.update.WorkflowCalculator;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class })
@RestController
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    // storePath and MidStateCalculator(strategy&config) are specific to user
    private static final String storePath = "./store/";
    private MidStateCalculator midStateCalculator;
    private static OperationConfig operationConfig = new OperationConfig();
    // private static Map<String, PaaSAPI> connectedSites = new HashMap<>();
    private static Map<String, CloudFoundryOperations> connectedSites = new HashMap<>();

    @RequestMapping(value = "/current_state", method = RequestMethod.POST)
    public @ResponseBody Overview getCurrentState(@RequestBody Collection<PaaSSite> managingSites) {
	Map<String, PaaSSite> sites = managingSites.stream()
		.collect(Collectors.toMap(site -> site.getName(), site -> site));
	Map<String, OverviewSite> overviewSites = managingSites.parallelStream().collect(Collectors
		.toMap(site -> site.getName(), site -> new CloudFoundryAPIv2(site, operationConfig).getOverviewSite()));
	logger.info("Got current state! ");
	return new Overview(sites, overviewSites);
    }

    /**
     * Upload the app binary or source file which is supposed to be uploaded to the PaaS
     * 
     * @param file
     * @return the stored file name, which is supposed to fill out app path in the desired state description
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

    @RequestMapping(value = "/apply", method = RequestMethod.POST)
    public @ResponseBody Overview apply(@RequestBody Overview desiredState) {
	Overview currentState = getCurrentState(desiredState.listPaaSSites());
	validAndConfigAppPath(currentState, desiredState);
	Workflow updateWorkflow = new WorkflowCalculator(currentState, desiredState, operationConfig)
		.getUpdateWorkflow();
	updateWorkflow.exec();
	logger.info("Workflow {} finished!", updateWorkflow);
	return getCurrentState(desiredState.listPaaSSites());
    }

    @RequestMapping(value = "/mid_states", method = RequestMethod.POST)
    public @ResponseBody Overview calcMidStates(@RequestBody Overview finalState) {
	if (midStateCalculator == null) {
	    throw new IllegalStateException("Update config not yet set.");
	}
	Overview currentState = getCurrentState(finalState.listPaaSSites());
	return midStateCalculator.calcMidStates(currentState, finalState);
    }

    @RequestMapping(value = "/set_update_config", method = RequestMethod.PUT)
    public void setUpdateConfig(@RequestParam("strategy") String strategy,
	    @RequestBody DeploymentConfig deploymentConfig) {
	strategy = "com.orange.midstate.strategy." + strategy;
	this.midStateCalculator = new MidStateCalculator(strategy, deploymentConfig);
	logger.info("Update config set!");
    }

    @RequestMapping(value = "/is_instantiation", method = RequestMethod.POST)
    public boolean isInstantiation(@RequestBody Overview desiredState) {
	Overview currentState = getCurrentState(desiredState.listPaaSSites());
	return currentState.isInstantiation(desiredState);
    }

    private void validAndConfigAppPath(Overview currentState, Overview desiredState) {
	for (OverviewSite site : desiredState.getOverviewSites().values()) {
	    for (OverviewApp app : site.getOverviewApps()) {
		if (app.getPath() != null) {
		    String appFilename = app.getPath();
		    app.setPath(storePath + appFilename);
		    if (!new File(app.getPath()).exists()) {
			throw new IllegalStateException(String.format("App file [%s] not yet uploaded.", appFilename));
		    }
		}
	    }
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

    public static void main(String[] args) {
	File storeDir = new File(storePath);
	if (!storeDir.exists()) {
	    storeDir.mkdirs();
	}
	SpringApplication.run(Main.class, args);
    }
}