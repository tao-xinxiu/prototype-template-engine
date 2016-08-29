package com.orange.paas.cf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.domains.*;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.organizations.*;
import org.cloudfoundry.client.v2.routes.*;
import org.cloudfoundry.client.v2.spaces.*;
import org.cloudfoundry.client.v3.*;
import org.cloudfoundry.client.v3.Data;
import org.cloudfoundry.client.v3.applications.*;
import org.cloudfoundry.client.v3.droplets.*;
import org.cloudfoundry.client.v3.packages.*;
import org.cloudfoundry.client.v3.packages.State;
import org.cloudfoundry.client.v3.processes.ProcessStatisticsResource;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.zafarkhaja.semver.Version;
import com.orange.model.PaaSTarget;

public class CloudFoundryOperations {
	// TODO move complicate operations code into CloudFoundryAPI
	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryOperations.class);
	private static final Object processLock = new Object();
	private static final Version SUPPORTED_API_VERSION = Version.valueOf("2.54.0");

	private PaaSTarget target;
	private CloudFoundryClient cloudFoundryClient;
	private String spaceId;
	private boolean compatible;

	public CloudFoundryOperations(PaaSTarget target) {
		this.target = target;
		ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().host("127.0.0.1").port(3128).build();
		ConnectionContext connectionContext = DefaultConnectionContext.builder().apiHost(target.getApi()).skipSslValidation(target.getSkipSslValidation()).proxyConfiguration(proxyConfiguration).build();
		TokenProvider tokenProvider = PasswordGrantTokenProvider.builder().password(target.getPwd())
				.username(target.getUser()).build();
		this.cloudFoundryClient = ReactorCloudFoundryClient.builder().connectionContext(connectionContext)
				.tokenProvider(tokenProvider).build();
		// SpringCloudFoundryClient.builder().host(target.getApi()).username(target.getUser())
		// .password(target.getPwd()).skipSslValidation(target.getSkipSslValidation()).proxyHost("127.0.0.1").proxyPort(3128).build();
		this.spaceId = requestSpaceId();
		Version targetVersion = Version.valueOf(getApiVersion());
		if (targetVersion.greaterThan(SUPPORTED_API_VERSION)) {
			compatible = false;
		} else {
			compatible = true;
		}
	}

	public String getTargetName() {
		return target.getName();
	}

	private String getApiVersion() {
		GetInfoRequest request = GetInfoRequest.builder().build();
		GetInfoResponse response = cloudFoundryClient.info().get(request).block();
		return response.getApiVersion();
	}

	private String requestOrgId() {
		try {
			ListOrganizationsRequest request = ListOrganizationsRequest.builder().name(target.getOrg()).build();
			ListOrganizationsResponse response = cloudFoundryClient.organizations().list(request).block();
			return response.getResources().get(0).getMetadata().getId();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting org id");
			throw new IllegalStateException("expcetion during getting org id", e);
		}
	}

	private String requestSpaceId() {
		try {
			ListSpacesRequest request = ListSpacesRequest.builder().organizationId(requestOrgId())
					.name(target.getSpace()).build();
			ListSpacesResponse response = cloudFoundryClient.spaces().list(request).block();
			return response.getResources().get(0).getMetadata().getId();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting space id");
			throw new IllegalStateException("expcetion during getting space id", e);
		}
	}

	public List<ApplicationResource> listSpaceApps() {
		try {
			ListApplicationsRequest request = ListApplicationsRequest.builder().spaceId(spaceId).build();
			ListApplicationsResponse response = cloudFoundryClient.applicationsV3().list(request).block();
			return response.getResources();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting space apps");
			throw new IllegalStateException("expcetion during getting space apps", e);
		}
	}

	public String getAppId(String appName) {
		try {
			ListApplicationsRequest request = ListApplicationsRequest.builder().spaceId(spaceId).name(appName).build();
			ListApplicationsResponse response = cloudFoundryClient.applicationsV3().list(request).block();
			if (response.getResources().size() == 0) {
				return null;
			}
			assert response.getResources().size() == 1;
			ApplicationResource appResource = response.getResources().get(0);
			if (appResource != null) {
				return appResource.getId();
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting app id for: " + appName);
			throw new IllegalStateException("expcetion during getting app id for: " + appName, e);
		}
	}

	public String getAppName(String appId) {
		GetApplicationRequest request = GetApplicationRequest.builder().applicationId(appId).build();
		GetApplicationResponse response = cloudFoundryClient.applicationsV3().get(request).block();
		return response.getName();
	}

	public Map<String, Object> getAppEnv(String appId) {
		GetApplicationEnvironmentRequest request = GetApplicationEnvironmentRequest.builder().applicationId(appId)
				.build();
		GetApplicationEnvironmentResponse response = cloudFoundryClient.applicationsV3().getEnvironment(request)
				.block();
		return response.getEnvironmentVariables();
	}

	public Object getAppEnv(String appId, String envKey) {
		return getAppEnv(appId).get(envKey);
	}

	/**
	 * create a v3 app object
	 * 
	 * @param name
	 * @param env
	 *            can be null
	 * @param lifecycle
	 *            can be null
	 * @return app guid
	 */
	public String createApp(String name, Map<String, ? extends String> env, Lifecycle lifecycle) {
		try {
			Relationships relationships = Relationships.builder().space(Relationship.builder().id(spaceId).build())
					.build();
			CreateApplicationRequest request = CreateApplicationRequest.builder().name(name)
					.relationships(relationships).environmentVariables(env).lifecycle(lifecycle).build();
			CreateApplicationResponse response = cloudFoundryClient.applicationsV3().create(request).block();
			return response.getId();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during creating app with arg: " + name + "; " + env + "; " + lifecycle);
			throw new IllegalStateException(
					"expcetion during creating app with arg: " + name + "; " + env + "; " + lifecycle, e);
		}
	}

	public void updateApp(String appId, String name, Map<String, ? extends String> env, Lifecycle lifecycle) {
		try {
			UpdateApplicationRequest request = UpdateApplicationRequest.builder().applicationId(appId).name(name)
					.environmentVariables(env).lifecycle(lifecycle).build();
			cloudFoundryClient.applicationsV3().update(request).block();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during updating app with arg: " + name + "; " + env + "; " + lifecycle);
			throw new IllegalStateException(
					"expcetion during updating app with arg: " + name + "; " + env + "; " + lifecycle, e);
		}
	}

	public void deleteApp(String appId) {
		try {
			DeleteApplicationRequest request = DeleteApplicationRequest.builder().applicationId(appId).build();
			cloudFoundryClient.applicationsV3().delete(request).block();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during deleting app with id: " + appId);
			throw new IllegalStateException("expcetion during deleting app with id: " + appId, e);
		}
	}

	public void startApp(String appId) {
		try {
			StartApplicationRequest request = StartApplicationRequest.builder().applicationId(appId).build();
			cloudFoundryClient.applicationsV3().start(request).block();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during starting app with id: " + appId);
			throw new IllegalStateException("expcetion during starting app with id: " + appId, e);
		}
	}

	public void stopApp(String appId) {
		try {
			StopApplicationRequest request = StopApplicationRequest.builder().applicationId(appId).build();
			cloudFoundryClient.applicationsV3().stop(request).block();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during stopping app with id: " + appId);
			throw new IllegalStateException("expcetion during stopping app with id: " + appId, e);
		}
	}

	/**
	 * 
	 * @param appId
	 * @param type
	 *            PackageType.DOCKER or PackageType.BITS
	 * @param data
	 *            can be null
	 * @return package guid
	 */
	public String createPackage(String appId, PackageType type, Data data) {
		try {
			CreatePackageRequest request = CreatePackageRequest.builder().applicationId(appId).type(type).data(data)
					.build();
			CreatePackageResponse response = cloudFoundryClient.packages().create(request).block();
			return response.getId();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during creating package with arg: " + appId + "; " + type + "; " + data);
			throw new IllegalStateException(
					"expcetion during creating package with arg: " + appId + "; " + type + "; " + data, e);
		}
	}

	public void uploadPackage(String packageId, String packageBitsPath, long timeout) {
		try {
			UploadPackageRequest request = UploadPackageRequest.builder().packageId(packageId)
					.bits(new FileInputStream(new File(packageBitsPath))).build();
			cloudFoundryClient.packages().upload(request).block(Duration.ofSeconds(timeout));
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during uploading package with arg: " + packageId + "; " + packageBitsPath);
			throw new IllegalStateException(
					"expcetion during uploading package with arg: " + packageId + "; " + packageBitsPath, e);
		}
	}

	public State getPackageState(String packageId) {
		try {
			GetPackageRequest request = GetPackageRequest.builder().packageId(packageId).build();
			GetPackageResponse response = cloudFoundryClient.packages().get(request).block();
			return response.getState();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting package with arg: " + packageId);
			throw new IllegalStateException("expcetion during getting package with arg: " + packageId, e);
		}
	}

	public void deletePackage(String packageId) {
		try {
			DeletePackageRequest request = DeletePackageRequest.builder().packageId(packageId).build();
			cloudFoundryClient.packages().delete(request).block();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during deleting package with arg: " + packageId);
			throw new IllegalStateException("expcetion during deleting package with arg: " + packageId, e);
		}
	}

	public String createDroplet(String packageId, Map<String, ? extends String> env, Lifecycle lifecycle) {
		try {
			StagePackageRequest request = StagePackageRequest.builder().packageId(packageId).environmentVariables(env)
					.lifecycle(lifecycle).build();
			StagePackageResponse response = cloudFoundryClient.packages().stage(request).block();
			return response.getId();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during creating droplet with arg: " + packageId + "; " + env + "; " + lifecycle);
			throw new IllegalStateException(
					"expcetion during creating droplet with arg: " + packageId + "; " + env + "; " + lifecycle, e);
		}
	}

	public void deleteDroplet(String dropletId) {
		try {
			DeleteDropletRequest request = DeleteDropletRequest.builder().dropletId(dropletId).build();
			cloudFoundryClient.droplets().delete(request).block();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during deleting droplet with arg: " + dropletId);
			throw new IllegalStateException("expcetion during deleting droplet with arg: " + dropletId, e);
		}
	}

	public void assignDroplet(String appId, String dropletId) {
		try {
			if (compatible) {
				AssignApplicationDropletRequest request = AssignApplicationDropletRequest.builder().applicationId(appId)
						.dropletId(dropletId).build();
				cloudFoundryClient.applicationsV3().assignDroplet(request).block();
			} else {
				// avoid multi thread is targeting multiple cf instances
				synchronized (processLock) {
					cfCliLogin();
					executeCommand(Arrays.asList("cf", "curl", String.format("v3/apps/%s/droplets/current", appId),
							"-X", "PUT", "-d", String.format("{\\\"droplet_guid\\\": \\\"%s\\\"}", dropletId)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during assigning droplet with arg: " + appId + "; " + dropletId);
			throw new IllegalStateException("expcetion during assigning droplet with arg: " + appId + "; " + dropletId,
					e);
		}
	}

	public org.cloudfoundry.client.v3.droplets.State getDropletState(String dropletId) {
		try {
			GetDropletRequest request = GetDropletRequest.builder().dropletId(dropletId).build();
			GetDropletResponse response = cloudFoundryClient.droplets().get(request).block();
			return response.getState();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting droplet state with arg: " + dropletId);
			throw new IllegalStateException("expcetion during getting droplet state with arg: " + dropletId, e);
		}
	}

	private String getDomainId(String domain) {
		try {
			ListDomainsRequest request = ListDomainsRequest.builder().name(domain).build();
			ListDomainsResponse response = cloudFoundryClient.domains().list(request).block();
			if (response.getResources().size() == 0) {
				return null;
			}
			assert response.getResources().size() == 1;
			return response.getResources().get(0).getMetadata().getId();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting domain id with arg: " + domain);
			throw new IllegalStateException("expcetion during getting domain id with arg: " + domain, e);
		}
	}

	private String getRouteId(String domainId, String hostname) {
		ListRoutesRequest request = ListRoutesRequest.builder().domainId(domainId).host(hostname).build();
		ListRoutesResponse response = cloudFoundryClient.routes().list(request).block();
		if (response.getResources().size() == 0) {
			return null;
		}
		assert response.getResources().size() == 1;
		return response.getResources().get(0).getMetadata().getId();
	}

	public String getLocalRouteId(String hostname) {
		return getRouteId(getDomainId(target.getLocalDomain()), hostname);
	}

	public String getGlobalRouteId(String hostname) {
		return getRouteId(getDomainId(target.getGlobalDomain()), hostname);
	}

	private String createRoute(String domainId, String hostname) {
		try {
			CreateRouteRequest request = CreateRouteRequest.builder().domainId(domainId).spaceId(spaceId).host(hostname)
					.build();
			CreateRouteResponse response = cloudFoundryClient.routes().create(request).block();
			return response.getMetadata().getId();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during creating local route with domainId: {}; hostname: {}", domainId, hostname);
			throw new IllegalStateException(String.format(
					"expcetion during creating local route with domainId: {}; hostname: {}", domainId, hostname), e);
		}
	}

	public String createLocalRoute(String hostname) {
		return createRoute(getDomainId(target.getLocalDomain()), hostname);
	}

	public String createGlobalRoute(String hostname) {
		return createRoute(getDomainId(target.getGlobalDomain()), hostname);
	}

	public void createRouteMapping(String appId, String routeId) {
		try {
			// avoid multi thread is targeting multiple cf instances
			synchronized (processLock) {
				cfCliLogin();
				executeCommand(Arrays.asList("cf", "curl", "v3/route_mappings", "-X", "POST", "-d",
						String.format(
								"{\\\"relationships\\\": {\\\"app\\\": {\\\"guid\\\":\\\"%s\\\"},\\\"route\\\":{\\\"guid\\\":\\\"%s\\\"}}}",
								appId, routeId)));
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during creating route mapping with arg: " + appId + "; " + routeId);
			throw new IllegalStateException(
					"expcetion during creating route mapping with arg: " + appId + "; " + routeId, e);
		}
	}

	public String getRouteMappingId(String appId, String routeId) {
		try {
			// avoid multi thread is targeting multiple cf instances
			synchronized (processLock) {
				cfCliLogin();
				return executePipedCommand(
						Arrays.asList("cf", "curl",
								String.format("v3/apps/%s/route_mappings?route_guids=%s", appId, routeId)),
						Arrays.asList("jq", "-r", ".resources[].guid"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting route mapping with arg: " + appId + "; " + routeId);
			throw new IllegalStateException(
					"expcetion during getting route mapping with arg: " + appId + "; " + routeId, e);
		}
	}

	public void deleteRouteMapping(String routeMappingId) {
		try {
			// avoid multi thread is targeting multiple cf instances
			synchronized (processLock) {
				cfCliLogin();
				executeCommand(Arrays.asList("cf", "curl", String.format("v3/route_mappings/%s", routeMappingId), "-X",
						"DELETE"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during deleting route mapping with arg: " + routeMappingId);
			throw new IllegalStateException("expcetion during deleting route mapping with arg: " + routeMappingId, e);
		}
	}

	public List<String> getProcessesState(String appId, String processType) {
		try {
			GetApplicationProcessStatisticsRequest request = GetApplicationProcessStatisticsRequest.builder()
					.applicationId(appId).type(processType).build();
			GetApplicationProcessStatisticsResponse response = cloudFoundryClient.applicationsV3()
					.getProcessStatistics(request).block();
			List<String> states = new ArrayList<>();
			for (ProcessStatisticsResource resource : response.getResources()) {
				states.add(resource.getState());
			}
			return states;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during getting processes state for appId: {}; processType: {}", appId, processType);
			throw new IllegalStateException(
					"expcetion during getting processes state for appId: " + appId + "; processType: " + processType,
					e);
		}
	}

	private void cfCliLogin() {
		try {
			List<String> loginCommand = new ArrayList<>(Arrays.asList("cf", "login", "-a", target.getApi(), "-u",
					target.getUser(), "-p", target.getPwd(), "-o", target.getOrg(), "-s", target.getSpace()));
			if (target.getSkipSslValidation()) {
				loginCommand.add("--skip-ssl-validation");
			}
			executeCommand(loginCommand);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during login to {} with cf cli.", target.getApi());
			throw new IllegalStateException(String.format("expcetion during login to %s with cf cli.", target.getApi()),
					e);
		}
	}

	private void executeCommand(List<String> command) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
			Process process = processBuilder.start();
			process.waitFor();
			int existCode = process.exitValue();
			if (existCode != 0) {
				throw new IllegalStateException("command exit with code: " + existCode);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("expcetion during executing command: {}", command);
			throw new IllegalStateException("expcetion during executing command: " + command, e);
		}
	}

	private static String executePipedCommand(List<String> srcCommand, List<String> targetCommand) {
		try {
			ProcessBuilder srcProcessBuilder = new ProcessBuilder(srcCommand);
			ProcessBuilder targetProcessBuilder = new ProcessBuilder(targetCommand);
			Process srcProcess = srcProcessBuilder.start();
			Process targetProcess = targetProcessBuilder.start();
			redirect(srcProcess.getInputStream(), targetProcess.getOutputStream());
			srcProcess.waitFor();
			targetProcess.waitFor();
			int existCode = targetProcess.exitValue();
			if (existCode != 0) {
				throw new IllegalStateException("command exit with code: " + existCode);
			}
			return new BufferedReader(new InputStreamReader(targetProcess.getInputStream())).lines()
					.collect(Collectors.joining("\n"));
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("expcetion during executing command: " + srcCommand + "; " + targetCommand);
			throw new IllegalStateException("expcetion during executing command: " + srcCommand + "; " + targetCommand,
					e);
		}
	}

	private static void redirect(InputStream src, OutputStream target) {
		InputStreamReader srcOutput = new InputStreamReader(src);
		OutputStreamWriter targetInput = new OutputStreamWriter(target);
		try {
			int value;
			while ((value = srcOutput.read()) != -1) {
				targetInput.write(value);
				targetInput.flush(); // I'm pretty sure this isn't needed
			}
			srcOutput.close();
			targetInput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
