package com.orange.paas.heroku;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.heroku.api.App;
import com.heroku.api.Build;
import com.heroku.api.Dyno;
import com.heroku.api.Heroku.Stack;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Source;
import com.heroku.api.util.Range;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSSiteAccess;
import com.orange.model.architecture.Microservice;
import com.orange.model.architecture.MicroserviceState;
import com.orange.model.workflow.Step;
import com.orange.paas.PaaSAPI;
import com.orange.util.Wait;

public class HerokuAPIImpl extends PaaSAPI {
    private final Logger logger;
    private HerokuAPI api;

    public HerokuAPIImpl(PaaSSiteAccess site, OperationConfig operationConfig) {
	super(site, operationConfig);
	this.logger = LoggerFactory.getLogger(String.format("%s(%s)", getClass(), site.getName()));
	this.api = new HerokuAPI(site.getApi());
    }

    @Override
    public Set<Microservice> get() {
	logger.info("Start getting the current architecture ...");
	List<App> apps = api.listApps();
	Set<Microservice> microservices = new HashSet<>();
	for (App app : apps) {
	    Microservice microservice = parseMicroservice(app);
	    microservices.add(microservice);
	}
	return microservices;
    }

    @Override
    public Step add(Microservice microservice) {
	return new Step(String.format("add microservice %s", microservice)) {
	    @SuppressWarnings("unchecked")
	    @Override
	    public void exec() {
		String name = microservice.get("name").toString();
		App app = api.createApp(new App().on(Stack.Cedar14).named(name));
		api.updateConfig(app.getId(), (Map<String, String>) microservice.get("env"));
		Source source = api.createSource();
		uploadFile(source.getSource_blob().getPut_url(), microservice.get("path").toString());
		Build build = new Build(source.getSource_blob().getGet_url(), microservice.get("version").toString(),
			new String[] { microservice.get("buildpack").toString() });
		api.createBuild(app.getName(), build);
		new Wait(operationConfig.getPrepareTimeout()).waitUntil(b -> msBuilt(b),
			String.format("wait until microservice [%s] staged", name), build);
		new Wait(operationConfig.getStartTimeout()).waitUntil(n -> msRunning(n),
			String.format("wait until microservice [%s] running", name), name);
	    }
	};
    }

    @Override
    public Step remove(Microservice microservice) {
	return new Step(String.format("remove microservice [%s]", microservice.get("name"))) {
	    @Override
	    public void exec() {
		String name = microservice.get("name").toString();
		if (api.appExists(name)) {
		    api.destroyApp(name);
		}
	    }
	};
    }

    @Override
    public Step modify(Microservice currentMicroservice, Microservice desiredMicroservice) {
	return new Step(String.format("update microservice from %s to %s", currentMicroservice, desiredMicroservice)) {
	    @SuppressWarnings("unchecked")
	    @Override
	    public void exec() {
		String name = desiredMicroservice.get("name").toString();
		updateNameIfNeed((String) currentMicroservice.get("name"), name);
		api.setMaintenanceMode(name, true);
		if (desiredMicroservice.get("state") != MicroserviceState.CREATED
			&& desiredMicroservice.get("path") != null
			&& !desiredMicroservice.get("path").equals(currentMicroservice.get("path"))) {
		    Source source = api.createSource();
		    uploadFile(source.getSource_blob().getPut_url(), desiredMicroservice.get("path").toString());
		    currentMicroservice.set("state", MicroserviceState.UPLOADED);
		    if (stagedMicroservice((MicroserviceState) currentMicroservice.get("state"))) {
			Build build = new Build(source.getSource_blob().getGet_url(),
				desiredMicroservice.get("version").toString(),
				new String[] { desiredMicroservice.get("buildpack").toString() });
			api.createBuild(name, build);
			new Wait(operationConfig.getPrepareTimeout()).waitUntil(b -> msBuilt(b),
				String.format("wait until microservice [%s] staged", name), build);
			currentMicroservice.set("state", MicroserviceState.STAGED);
			new Wait(operationConfig.getStartTimeout()).waitUntil(n -> msRunning(n),
				String.format("wait until microservice [%s] running", name), name);
			currentMicroservice.set("state", MicroserviceState.RUNNING);
		    }
		}
		if (!currentMicroservice.get("env").equals(desiredMicroservice.get("env"))) {
		    api.updateConfig(name, (Map<String, String>) desiredMicroservice.get("env"));
		    api.restartDynos(name);
		}
	    }
	};
    }

    private Microservice parseMicroservice(App app) {
	Map<String, Object> attributes = new HashMap<>();
	attributes.put("guid", app.getId());
	attributes.put("name", app.getName());
	attributes.put("path", app.getGitUrl());
	attributes.put("routes", app.getWebUrl());
	return new Microservice(attributes);
    }

    private void uploadFile(String urlString, String path) {
	try {
	    ClassLoader classLoader = getClass().getClassLoader();
	    File file = new File(classLoader.getResource(path).getFile());
	    URL url = new URL(urlString);
	    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
	    httpCon.setDoOutput(true);
	    httpCon.setRequestMethod("PUT");
	    @SuppressWarnings("resource")
	    InputStream inputStream = new FileInputStream(file);
	    OutputStream outputStream = httpCon.getOutputStream();
	    Integer c;
	    while ((c = inputStream.read()) != -1) {
		outputStream.write(c);
	    }
	    outputStream.close();
	    httpCon.getInputStream();
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new IllegalStateException(String.format("Uploading microservice path [%s] failed", path));
	}
    }

    private void updateNameIfNeed(String currentName, String desiredName) {
	if (currentName.equals(desiredName)) {
	    return;
	}
	api.renameApp(currentName, desiredName);
	logger.info("microservice name updated from {} to {}.", currentName, desiredName);
    }

    private boolean stagedMicroservice(MicroserviceState msState) {
	Set<MicroserviceState> unstagedStates = new HashSet<>(
		Arrays.asList(MicroserviceState.CREATED, MicroserviceState.UPLOADED));
	return !unstagedStates.contains(msState);
    }

    private boolean msBuilt(Build build) {
	return "succeeded".equals(build.getStatus());
    }

    private boolean msRunning(String name) {
	Range<Dyno> dynos = api.listDynos(name);
	if (dynos.size() == 0) {
	    return false;
	}
	return dynos.stream().anyMatch(dyno -> dyno.getState().equals("up"));
    }
}
