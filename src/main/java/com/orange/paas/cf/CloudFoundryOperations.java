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
import com.orange.model.PaaSAccessInfo;

public class CloudFoundryOperations {
	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryOperations.class);
	// cf-java-client supported CF API version
	private static final Version SUPPORTED_API_VERSION = Version.valueOf("2.54.0");
	private static final String path_CF_HOME_dir = System.getProperty("user.home") + "/cf_homes/";

	private PaaSAccessInfo siteAccessInfo;
	private CloudFoundryClient cloudFoundryClient;
	private String spaceId;
	private boolean compatible;

	public CloudFoundryOperations(PaaSAccessInfo siteAccessInfo) {
		this.siteAccessInfo = siteAccessInfo;
		String proxy_host = System.getenv("proxy_host");
		String proxy_port = System.getenv("proxy_port");
		ConnectionContext connectionContext;
		if (proxy_host != null && proxy_port != null) {
			ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().host(proxy_host)
					.port(Integer.parseInt(proxy_port)).build();
			connectionContext = DefaultConnectionContext.builder().apiHost(siteAccessInfo.getApi())
					.skipSslValidation(siteAccessInfo.getSkipSslValidation()).proxyConfiguration(proxyConfiguration)
					.build();
		} else {
			connectionContext = DefaultConnectionContext.builder().apiHost(siteAccessInfo.getApi())
					.skipSslValidation(siteAccessInfo.getSkipSslValidation()).build();
		}
		TokenProvider tokenProvider = PasswordGrantTokenProvider.builder().password(siteAccessInfo.getPwd())
				.username(siteAccessInfo.getUser()).build();
		this.cloudFoundryClient = ReactorCloudFoundryClient.builder().connectionContext(connectionContext)
				.tokenProvider(tokenProvider).build();
		this.spaceId = requestSpaceId();
		Version siteVersion = Version.valueOf(getApiVersion());
		if (siteVersion.greaterThan(SUPPORTED_API_VERSION)) {
			compatible = false;
		} else {
			compatible = true;
		}
	}

	private String getApiVersion() {
		GetInfoRequest request = GetInfoRequest.builder().build();
		GetInfoResponse response = cloudFoundryClient.info().get(request).block();
		return response.getApiVersion();
	}

	private String requestOrgId() {
		try {
			ListOrganizationsRequest request = ListOrganizationsRequest.builder().name(siteAccessInfo.getOrg()).build();
			ListOrganizationsResponse response = cloudFoundryClient.organizations().list(request).block();
			return response.getResources().get(0).getMetadata().getId();
		} catch (Exception e) {
			throw new IllegalStateException("expcetion during getting org id", e);
		}
	}

	private String requestSpaceId() {
		try {
			ListSpacesRequest request = ListSpacesRequest.builder().organizationId(requestOrgId())
					.name(siteAccessInfo.getSpace()).build();
			ListSpacesResponse response = cloudFoundryClient.spaces().list(request).block();
			return response.getResources().get(0).getMetadata().getId();
		} catch (Exception e) {
			throw new IllegalStateException("expcetion during getting space id", e);
		}
	}

	public List<ApplicationResource> listSpaceApps() {
		try {
			ListApplicationsRequest request = ListApplicationsRequest.builder().spaceId(spaceId).build();
			ListApplicationsResponse response = cloudFoundryClient.applicationsV3().list(request).block();
			return response.getResources();
		} catch (Exception e) {
			throw new IllegalStateException("expcetion during getting space apps", e);
		}
	}

	public String getAppId(String appName) {
		try {
			ListApplicationsRequest request = ListApplicationsRequest.builder().spaceId(spaceId).name(appName).build();
			ListApplicationsResponse response = cloudFoundryClient.applicationsV3().list(request).block();
			if (response.getResources().size() == 0) {
				logger.info(
						"CloudFoundryOperations.getAppId : Not found app with specific name {} in the space at site {}.",
						appName, siteAccessInfo.getName());
				return null;
			}
			assert response.getResources().size() == 1;
			String appId = response.getResources().get(0).getId();
			logger.info(
					"CloudFoundryOperations.getAppId : Got app id {} with specific name {} in the space at site {}.",
					appId, appName, siteAccessInfo.getName());
			return appId;
		} catch (Exception e) {
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

	public Map<String, Object> getDropletEnv(String dropletId) {
		GetDropletRequest request = GetDropletRequest.builder().dropletId(dropletId).build();
		GetDropletResponse response = cloudFoundryClient.droplets().get(request).block();
		return response.getEnvironmentVariables();
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
			throw new IllegalStateException(
					"expcetion during updating app with arg: " + name + "; " + env + "; " + lifecycle, e);
		}
	}

	public void deleteApp(String appId) {
		try {
			DeleteApplicationRequest request = DeleteApplicationRequest.builder().applicationId(appId).build();
			cloudFoundryClient.applicationsV3().delete(request).block();
			logger.info("App {} at {} deleted.", appId, siteAccessInfo.getName());
		} catch (Exception e) {
			throw new IllegalStateException("expcetion during deleting app with id: " + appId, e);
		}
	}

	public void startApp(String appId) {
		try {
			StartApplicationRequest request = StartApplicationRequest.builder().applicationId(appId).build();
			cloudFoundryClient.applicationsV3().start(request).block();
			logger.info("App {} at {} desired state changed to \"STARTED\".", appId, siteAccessInfo.getName());
		} catch (Exception e) {
			throw new IllegalStateException("expcetion during starting app with id: " + appId, e);
		}
	}

	public void stopApp(String appId) {
		try {
			StopApplicationRequest request = StopApplicationRequest.builder().applicationId(appId).build();
			cloudFoundryClient.applicationsV3().stop(request).block();
			logger.info("App {} at {} desired state changed to \"STOPPED\".", appId, siteAccessInfo.getName());
		} catch (Exception e) {
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
			String packageId = response.getId();
			logger.info("package (type: {}, data: {}) for app {} created with id: {}", type, data, appId, packageId);
			return packageId;
		} catch (Exception e) {
			throw new IllegalStateException(
					"expcetion during creating package with arg: " + appId + "; " + type + "; " + data, e);
		}
	}

	public void uploadPackage(String packageId, String packageBitsPath, long timeout) {
		try {
			UploadPackageRequest request = UploadPackageRequest.builder().packageId(packageId)
					.bits(new FileInputStream(new File(packageBitsPath))).build();
			logger.info("package {} with bits path {} uploading with timeout: {} seconds", packageId, packageBitsPath,
					timeout);
			cloudFoundryClient.packages().upload(request).block(Duration.ofSeconds(timeout));
			logger.info("package {} with bits path {} uploaded", packageId, packageBitsPath);
		} catch (Exception e) {
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
			throw new IllegalStateException("expcetion during getting package with arg: " + packageId, e);
		}
	}

	public void deletePackage(String packageId) {
		try {
			DeletePackageRequest request = DeletePackageRequest.builder().packageId(packageId).build();
			cloudFoundryClient.packages().delete(request).block();
		} catch (Exception e) {
			throw new IllegalStateException("expcetion during deleting package with arg: " + packageId, e);
		}
	}

	public String createDroplet(String packageId, Map<String, ? extends String> env, Lifecycle lifecycle) {
		try {
			StagePackageRequest request = StagePackageRequest.builder().packageId(packageId).environmentVariables(env)
					.lifecycle(lifecycle).build();
			StagePackageResponse response = cloudFoundryClient.packages().stage(request).block();
			String dropletId = response.getId();
			logger.info("droplet created with id: {}", dropletId);
			return dropletId;
		} catch (Exception e) {
			throw new IllegalStateException(
					"expcetion during creating droplet with arg: " + packageId + "; " + env + "; " + lifecycle, e);
		}
	}

	public void deleteDroplet(String dropletId) {
		try {
			DeleteDropletRequest request = DeleteDropletRequest.builder().dropletId(dropletId).build();
			cloudFoundryClient.droplets().delete(request).block();
		} catch (Exception e) {
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
				cfCliLogin();
				executeCFCliCommand(Arrays.asList("cf", "curl", String.format("v3/apps/%s/droplets/current", appId),
						"-X", "PUT", "-d", String.format("{\\\"droplet_guid\\\": \\\"%s\\\"}", dropletId)));
				logger.info("droplet {} assigned to app {}", dropletId, appId);
			}
		} catch (Exception e) {
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
			throw new IllegalStateException("expcetion during getting droplet state with arg: " + dropletId, e);
		}
	}

	public List<String> listAppDropletsId(String appId) {
		ListApplicationDropletsRequest request = ListApplicationDropletsRequest.builder().applicationId(appId).build();
		ListApplicationDropletsResponse response = cloudFoundryClient.applicationsV3().listDroplets(request).block();
		List<String> dropletsId = new ArrayList<>();
		List<DropletResource> dropletResources = response.getResources();
		if (response.getResources() == null)
			return dropletsId;
		for (DropletResource resource : dropletResources) {
			dropletsId.add(resource.getId());
		}
		return dropletsId;
	}

	public String getCurrentDropletId(String appId) {
		cfCliLogin();
		return executeCFCliPipedCommand(
				Arrays.asList("cf", "curl", String.format("v3/apps/%s/droplets/current", appId)),
				Arrays.asList("jq", "-r", ".guid"));
	}

	public String getDomainId(String domain) {
		try {
			ListDomainsRequest request = ListDomainsRequest.builder().name(domain).build();
			ListDomainsResponse response = cloudFoundryClient.domains().list(request).block();
			if (response.getResources().size() == 0) {
				return null;
			}
			assert response.getResources().size() == 1;
			return response.getResources().get(0).getMetadata().getId();
		} catch (Exception e) {
			throw new IllegalStateException("expcetion during getting domain id with arg: " + domain, e);
		}
	}

	public String getDomainString(String domainId) {
		if (domainId == null) {
			return null;
		}
		GetDomainRequest request = GetDomainRequest.builder().domainId(domainId).build();
		GetDomainResponse response = cloudFoundryClient.domains().get(request).block();
		if (response.getEntity() == null) {
			return null;
		} else {
			return response.getEntity().getName();
		}
	}

	public String getRouteId(String hostname, String domainId) {
		ListRoutesRequest request = ListRoutesRequest.builder().domainId(domainId).host(hostname).build();
		ListRoutesResponse response = cloudFoundryClient.routes().list(request).block();
		if (response.getResources().size() == 0) {
			return null;
		}
		assert response.getResources().size() == 1;
		return response.getResources().get(0).getMetadata().getId();
	}

	public String createRoute(String hostname, String domainId) {
		try {
			CreateRouteRequest request = CreateRouteRequest.builder().domainId(domainId).spaceId(spaceId).host(hostname)
					.build();
			CreateRouteResponse response = cloudFoundryClient.routes().create(request).block();
			return response.getMetadata().getId();
		} catch (Exception e) {
			throw new IllegalStateException(String
					.format("expcetion during creating route with domainId: {}; hostname: {}", domainId, hostname), e);
		}
	}

	public String getRouteHost(String routeId) {
		GetRouteRequest request = GetRouteRequest.builder().routeId(routeId).build();
		GetRouteResponse response = cloudFoundryClient.routes().get(request).block();
		if (response.getEntity() == null) {
			return null;
		} else {
			return response.getEntity().getHost();
		}
	}

	public String getRouteDomainId(String routeId) {
		GetRouteRequest request = GetRouteRequest.builder().routeId(routeId).build();
		GetRouteResponse response = cloudFoundryClient.routes().get(request).block();
		if (response.getEntity() == null) {
			return null;
		} else {
			return response.getEntity().getDomainId();
		}
	}

	public String[] listMappedRoutesId(String appId) {
		try {
			cfCliLogin();
			String routeslink = executeCFCliPipedCommand(
					Arrays.asList("cf", "curl", String.format("v3/apps/%s/route_mappings", appId)),
					Arrays.asList("jq", "-r", ".resources[]?.links?.route?.href"));
			String routesId = routeslink.replace("/v2/routes/", "");
			return routesId.split("\\r?\\n");
		} catch (Exception e) {
			throw new IllegalStateException(String.format("expcetion in listMappedRoutesId with appId: [%s]", appId),
					e);
		}
	}

	public void createRouteMapping(String appId, String routeId) {
		try {
			cfCliLogin();
			executeCFCliCommand(Arrays.asList("cf", "curl", "v3/route_mappings", "-X", "POST", "-d",
					String.format(
							"{\\\"relationships\\\": {\\\"app\\\": {\\\"guid\\\":\\\"%s\\\"},\\\"route\\\":{\\\"guid\\\":\\\"%s\\\"}}}",
							appId, routeId)));
		} catch (Exception e) {
			throw new IllegalStateException(
					"expcetion during creating route mapping with arg: " + appId + "; " + routeId, e);
		}
	}

	public String getRouteMappingId(String appId, String routeId) {
		try {
			cfCliLogin();
			return executeCFCliPipedCommand(
					Arrays.asList("cf", "curl",
							String.format("v3/apps/%s/route_mappings?route_guids=%s", appId, routeId)),
					Arrays.asList("jq", "-r", ".resources[]?.guid"));
		} catch (Exception e) {
			throw new IllegalStateException(
					"expcetion during getting route mapping with arg: " + appId + "; " + routeId, e);
		}
	}

	public void deleteRouteMapping(String routeMappingId) {
		try {
			cfCliLogin();
			executeCFCliCommand(
					Arrays.asList("cf", "curl", String.format("v3/route_mappings/%s", routeMappingId), "-X", "DELETE"));
		} catch (Exception e) {
			throw new IllegalStateException(
					String.format("expcetion in deleteRouteMapping with routeMappingId: [%s]", routeMappingId), e);
		}
	}

	public List<String> listProcessesState(String appId, String processType) {
		try {
			GetApplicationProcessStatisticsRequest request = GetApplicationProcessStatisticsRequest.builder()
					.applicationId(appId).type(processType).build();
			GetApplicationProcessStatisticsResponse response = cloudFoundryClient.applicationsV3()
					.getProcessStatistics(request).block();
			List<String> states = new ArrayList<>();
			if (response.getResources() == null) {
				return states;
			}
			for (ProcessStatisticsResource resource : response.getResources()) {
				states.add(resource.getState());
			}
			return states;
		} catch (Exception e) {
			throw new IllegalStateException(String
					.format("expcetion in getProcessesState with appId [%s]; processType [%s]", appId, processType), e);
		}
	}

	private void cfCliLogin() {
		try {
			List<String> loginCommand = new ArrayList<>(
					Arrays.asList("cf", "login", "-a", siteAccessInfo.getApi(), "-u", siteAccessInfo.getUser(), "-p",
							siteAccessInfo.getPwd(), "-o", siteAccessInfo.getOrg(), "-s", siteAccessInfo.getSpace()));
			if (siteAccessInfo.getSkipSslValidation()) {
				loginCommand.add("--skip-ssl-validation");
			}
			mkdirs(path_CF_HOME_dir + siteAccessInfo.getName());
			executeCFCliCommand(loginCommand);
		} catch (Exception e) {
			throw new IllegalStateException(
					String.format("expcetion during login to %s with cf cli.", siteAccessInfo.getName()), e);
		}
	}

	/**
	 * execute a command, throw IllegalStateException in case of error or
	 * command not exist with 0.
	 * 
	 * @param command
	 *            command to be executed with its args
	 */
	private void executeCFCliCommand(List<String> command) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			if (System.getenv("DEBUG") != null) {
				processBuilder.inheritIO();
			}
			setCFHome(processBuilder);
			Process process = processBuilder.start();
			process.waitFor();
			int existCode = process.exitValue();
			if (existCode != 0) {
				throw new IllegalStateException(String.format("command [%s] exit with code: [%d]", command, existCode));
			}
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException(String.format("expcetion during executing command: [%s]", command), e);
		}
	}

	private String executeCFCliPipedCommand(List<String> srcCommand, List<String> targetCommand) {
		try {
			ProcessBuilder srcProcessBuilder = new ProcessBuilder(srcCommand);
			ProcessBuilder targetProcessBuilder = new ProcessBuilder(targetCommand);
			setCFHome(srcProcessBuilder);
			setCFHome(targetProcessBuilder);
			Process srcProcess = srcProcessBuilder.start();
			Process targetProcess = targetProcessBuilder.start();
			redirect(srcProcess.getInputStream(), targetProcess.getOutputStream());
			srcProcess.waitFor();
			targetProcess.waitFor();
			int existCode = targetProcess.exitValue();
			if (existCode != 0) {
				throw new IllegalStateException(String.format("piped command [%s|%s] exit with code: [%d]", srcCommand,
						targetCommand, existCode));
			}
			return new BufferedReader(new InputStreamReader(targetProcess.getInputStream())).lines()
					.collect(Collectors.joining("\n"));
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException(
					String.format("expcetion during executing piped command: [%s|%s]", srcCommand, targetCommand), e);
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
			throw new IllegalStateException("IOExpcetion during redirect", e);
		}
	}

	private static void mkdirs(String path) {
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}
		if (!file.exists()) {
			throw new IllegalStateException(String.format("Creating directory [%s] failed", path));
		}
	}

	private void setCFHome(ProcessBuilder processBuilder) {
		processBuilder.environment().put("CF_HOME", path_CF_HOME_dir + siteAccessInfo.getName());
	}
}
