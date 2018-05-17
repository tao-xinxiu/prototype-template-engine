package com.orange.paas.cf;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationEnvironmentRequest;
import org.cloudfoundry.client.v2.applications.ApplicationEnvironmentResponse;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.CreateApplicationRequest;
import org.cloudfoundry.client.v2.applications.CreateApplicationResponse;
import org.cloudfoundry.client.v2.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v2.applications.RestageApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.applications.UploadApplicationRequest;
import org.cloudfoundry.client.v2.domains.ListDomainsRequest;
import org.cloudfoundry.client.v2.domains.ListDomainsResponse;
import org.cloudfoundry.client.v2.organizations.ListOrganizationsRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationsResponse;
import org.cloudfoundry.client.v2.routemappings.CreateRouteMappingRequest;
import org.cloudfoundry.client.v2.routemappings.DeleteRouteMappingRequest;
import org.cloudfoundry.client.v2.routemappings.ListRouteMappingsRequest;
import org.cloudfoundry.client.v2.routemappings.ListRouteMappingsResponse;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.ListRoutesRequest;
import org.cloudfoundry.client.v2.routes.ListRoutesResponse;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.ListServiceBindingsRequest;
import org.cloudfoundry.client.v2.servicebindings.ListServiceBindingsResponse;
import org.cloudfoundry.client.v2.spaces.GetSpaceSummaryRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceSummaryResponse;
import org.cloudfoundry.client.v2.spaces.ListSpaceServiceInstancesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceServiceInstancesResponse;
import org.cloudfoundry.client.v2.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpacesResponse;
import org.cloudfoundry.client.v2.spaces.SpaceApplicationSummary;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.Main;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSSiteAccess;
import com.orange.model.architecture.Route;
import com.orange.model.architecture.cf.CFMicroservice;
import com.orange.model.architecture.cf.CFMicroserviceDesiredState;
import com.orange.model.architecture.cf.CFMicroserviceState;
import com.orange.util.RetryFunction;
import com.orange.util.Wait;

public class CloudFoundryOperations {
    private static final String runningState = "RUNNING";
    private static final String stagedState = "STAGED";

    private final Logger logger;
    private final String siteInfo;

    private PaaSSiteAccess site;
    private CloudFoundryClient cloudFoundryClient;
    private String spaceId;
    private OperationConfig opConfig;
    private Duration timeout;
    private int healthCheckTimeout = 180;

    public CloudFoundryOperations(PaaSSiteAccess site, OperationConfig opConfig) {
	this.site = site;
	this.logger = LoggerFactory.getLogger(String.format("%s(%s)", getClass(), site.getName()));
	this.siteInfo = String.format(" at site [%s]", site.getName());
	this.opConfig = opConfig;
	this.timeout = Duration.ofSeconds(opConfig.getGeneralTimeout());
	String proxy_host = System.getenv("proxy_host");
	String proxy_port = System.getenv("proxy_port");
	Duration connectTimeout = Duration.ofSeconds(opConfig.getConnectTimeout());
	try {
	    DefaultConnectionContext.Builder connectionContext = DefaultConnectionContext.builder()
		    .apiHost(site.getApi()).skipSslValidation(site.getSkipSslValidation())
		    .connectTimeout(connectTimeout).sslHandshakeTimeout(connectTimeout).keepAlive(true);
	    if (proxy_host != null && proxy_port != null) {
		logger.info("Proxy setted: {}:{}", proxy_host, proxy_port);
		ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().host(proxy_host)
			.port(Integer.parseInt(proxy_port)).build();
		connectionContext.proxyConfiguration(proxyConfiguration);
	    }
	    TokenProvider tokenProvider = PasswordGrantTokenProvider.builder().password(site.getPwd())
		    .username(site.getUser()).build();
	    this.cloudFoundryClient = ReactorCloudFoundryClient.builder().connectionContext(connectionContext.build())
		    .tokenProvider(tokenProvider).build();
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during creating client" + siteInfo, e);
	}
	this.spaceId = requestSpaceId();
    }

    public List<SpaceApplicationSummary> listSpaceApps() {
	try {
	    GetSpaceSummaryRequest request = GetSpaceSummaryRequest.builder().spaceId(spaceId).build();
	    logger.trace("Start requesting space application summary...");
	    GetSpaceSummaryResponse response = retry(
		    () -> cloudFoundryClient.spaces().getSummary(request).block(timeout));
	    logger.trace("Got space apps!");
	    return response.getApplications();
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during getting space apps" + siteInfo, e);
	}
    }

    /**
     * Create a microservice with specified name, nbProcesses and env.
     * 
     * @param name
     * @param nbProcesses
     * @param env
     * @return
     */
    public String create(String name, int nbProcesses, Map<String, String> env) {
	CreateApplicationRequest request = CreateApplicationRequest.builder().name(name).spaceId(spaceId)
		.instances(nbProcesses).environmentJsons(env).healthCheckTimeout(healthCheckTimeout).build();
	CreateApplicationResponse response = retry(
		() -> cloudFoundryClient.applicationsV2().create(request).block(timeout));
	String id = response.getMetadata().getId();
	logger.info("App [{}] created with id [{}].", name, id);
	return id;
    }

    /**
     * Delete a microservice
     * 
     * @param msId
     */
    public void delete(String msId) {
	DeleteApplicationRequest request = DeleteApplicationRequest.builder().applicationId(msId).build();
	retry(() -> cloudFoundryClient.applicationsV2().delete(request).block(timeout));
    }

    /**
     * Update app path. Note: in CF, app should be STOPPED before upload, so
     * that it could be staged later by operation of starting
     * 
     * @param msId
     * @param desiredPath
     * @param currentEnv
     */
    public void updatePath(String msId, String desiredPath, Map<String, String> currentEnv) {
	stop(msId);
	upload(msId, desiredPath);
	Map<String, String> envWithUpdatedPath = new HashMap<>(currentEnv);
	envWithUpdatedPath.put(CloudFoundryAPIv2.pathKeyInEnv, desiredPath);
	updateAppEnv(msId, envWithUpdatedPath);
    }

    /**
     * Update app env. As we use env to store path, so updating of env should
     * not change path value.
     * 
     * @param msId
     * @param env
     */
    public void updateEnv(String msId, Map<String, String> env) {
	// updateEnv should not change path value in env during update other env
	Map<String, String> envWithPath = new HashMap<>(env);
	String path = getEnv(msId, CloudFoundryAPIv2.pathKeyInEnv);
	envWithPath.put(CloudFoundryAPIv2.pathKeyInEnv, path);
	updateAppEnv(msId, envWithPath);
    }

    public void updateNbProcessesIfNeed(String msId, int currentNbProcesses, int desiredNbProcesses) {
	if (currentNbProcesses == desiredNbProcesses) {
	    return;
	}
	updateApp(msId, null, null, desiredNbProcesses, null);
	logger.info("app {} instances updated from {} to {}.", msId, currentNbProcesses, desiredNbProcesses);
    }

    public void updateNameIfNeed(String msId, String currentName, String desiredName) {
	if (currentName.equals(desiredName)) {
	    return;
	}
	updateApp(msId, desiredName, null, null, null);
	logger.info("microservice {} name updated from {} to {}.", msId, currentName, desiredName);
    }

    /**
     * update microservice bound routes if changed.
     * 
     * @param msId
     * @param currentRoutes
     * @param desiredRoutes
     */
    public void updateRoutesIfNeed(String msId, Set<Route> currentRoutes, Set<Route> desiredRoutes) {
	if (currentRoutes.equals(desiredRoutes)) {
	    return;
	}
	Set<Route> addedRoutes = desiredRoutes.stream().filter(route -> !currentRoutes.contains(route))
		.collect(Collectors.toSet());
	Set<Route> removedRoutes = currentRoutes.stream().filter(route -> !desiredRoutes.contains(route))
		.collect(Collectors.toSet());
	addedRoutes.stream().forEach(route -> createAndMapAppRoute(msId, route));
	removedRoutes.stream().forEach(route -> unmapAppRoute(msId, route));
    }

    public void updateServicesIfNeed(String msId, Set<String> currentServices, Set<String> desiredServices) {
	if (currentServices.equals(desiredServices)) {
	    return;
	}
	Set<String> bindServices = desiredServices.stream().filter(service -> !currentServices.contains(service))
		.collect(Collectors.toSet());
	Set<String> unbindServiecs = currentServices.stream().filter(service -> !desiredServices.contains(service))
		.collect(Collectors.toSet());
	bindServices.stream().forEach(service -> bindAppServices(msId, service));
	unbindServiecs.stream().forEach(service -> unbindAppServices(msId, service));
    }

    /**
     * update microservice state (i.e. manage its lifecycle), contains
     * updatePath if necessary for state change (i.e. change from CREATED state)
     * 
     * @param currentMicroservice
     * @param desiredMicroservice
     */
    @SuppressWarnings("unchecked")
    public void updateStateIfNeed(CFMicroservice currentMicroservice, CFMicroservice desiredMicroservice) {
	if (currentMicroservice.get("state") == desiredMicroservice.get("state")) {
	    return;
	}
	String msId = (String) currentMicroservice.get("guid");
	switch ((CFMicroserviceState) desiredMicroservice.get("state")) {
	case RUNNING:
	    switch ((CFMicroserviceState) currentMicroservice.get("state")) {
	    case CREATED:
		updatePath(msId, (String) desiredMicroservice.get("path"),
			(Map<String, String>) currentMicroservice.get("env"));
	    case UPLOADED:
	    case STAGED:
		start(msId);
	    case staging:
		waitStaged(msId);
	    case starting:
		waitRunning(msId);
		break;
	    case FAILED:
		restage(msId);
		waitStaged(msId);
		waitRunning(msId);
		break;
	    default:
		throw new IllegalStateException(
			String.format("Unsupported microservice [%s] to update state from [%s] to [%s]", msId,
				currentMicroservice.get("state"), desiredMicroservice.get("state")));
	    }
	    break;
	case STAGED:
	    switch ((CFMicroserviceState) currentMicroservice.get("state")) {
	    case CREATED:
		updatePath(msId, (String) desiredMicroservice.get("path"),
			(Map<String, String>) currentMicroservice.get("env"));
	    case UPLOADED:
		start(msId);
	    case staging:
		waitStaged(msId);
	    case starting:
	    case RUNNING:
		stop(msId);
		break;
	    case FAILED:
		restage(msId);
		stop(msId);
		waitStaged(msId);
		break;
	    default:
		throw new IllegalStateException(
			String.format("Unsupported microservice [%s] to update state from [%s] to [%s]", msId,
				currentMicroservice.get("state"), desiredMicroservice.get("state")));
	    }
	    break;
	case UPLOADED:
	    switch ((CFMicroserviceState) currentMicroservice.get("state")) {
	    case CREATED:
		updatePath(msId, (String) desiredMicroservice.get("path"),
			(Map<String, String>) currentMicroservice.get("env"));
		break;
	    case FAILED:
		restage(msId);
		stop(msId);
		break;
	    default:
		throw new IllegalStateException(
			String.format("Unsupported microservice [%s] to update state from [%s] to [%s]", msId,
				currentMicroservice.get("state"), desiredMicroservice.get("state")));
	    }
	    break;
	default:
	    throw new IllegalStateException(
		    String.format("Unsupported desired state [%s]", desiredMicroservice.get("state")));
	}
    }

    void stop(String msId) {
	updateApp(msId, null, null, null, CFMicroserviceDesiredState.STOPPED);
	logger.info("app {} desired state updated to STOPPED.", msId);
    }

    void restage(String msId) {
	RestageApplicationRequest request = RestageApplicationRequest.builder().applicationId(msId).build();
	retry(() -> cloudFoundryClient.applicationsV2().restage(request).block(timeout));
    }

    private void start(String msId) {
	updateApp(msId, null, null, null, CFMicroserviceDesiredState.STARTED);
	logger.info("app {} desired state updated to STARTED.", msId);
    }

    private void waitStaged(String msId) {
	new Wait(opConfig.getPrepareTimeout()).waitUntil(id -> appStaged(id),
		String.format("wait until microservice [%s] staged", msId), msId);
    }

    private void waitRunning(String msId) {
	new Wait(opConfig.getStartTimeout()).waitUntil(id -> appRunning(id),
		String.format("wait until microservice [%s] running", msId), msId);
    }

    private boolean appStaged(String appId) {
	return stagedState.equals(getAppSummary(appId).getPackageState());
    }

    boolean appRunning(String appId) {
	SummaryApplicationResponse appSummary = getAppSummary(appId);
	if (!CFMicroserviceDesiredState.STARTED.toString().equals(appSummary.getState())) {
	    return false;
	}
	if (!stagedState.equals(appSummary.getPackageState())) {
	    return false;
	}
	ApplicationInstancesRequest request = ApplicationInstancesRequest.builder().applicationId(appSummary.getId())
		.build();
	ApplicationInstancesResponse response = retry(
		() -> cloudFoundryClient.applicationsV2().instances(request).block(timeout));
	return response.getInstances().entrySet().stream()
		.anyMatch(entity -> runningState.equals(entity.getValue().getState()));
    }

    private String requestSpaceId() {
	try {
	    ListSpacesRequest request = ListSpacesRequest.builder().organizationId(requestOrgId()).name(site.getSpace())
		    .build();
	    ListSpacesResponse response = retry(() -> cloudFoundryClient.spaces().list(request).block(timeout));
	    logger.trace("Got space id.");
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during getting space id" + siteInfo, e);
	}
    }

    private String requestOrgId() {
	try {
	    ListOrganizationsRequest request = ListOrganizationsRequest.builder().name(site.getOrg()).build();
	    ListOrganizationsResponse response = retry(
		    () -> cloudFoundryClient.organizations().list(request).block(timeout));
	    logger.trace("Got organization id.");
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during getting org id" + siteInfo, e);
	}
    }

    private String getEnv(String appId, String envKey) {
	try {
	    ApplicationEnvironmentRequest request = ApplicationEnvironmentRequest.builder().applicationId(appId)
		    .build();
	    ApplicationEnvironmentResponse response = retry(
		    () -> cloudFoundryClient.applicationsV2().environment(request).block(timeout));
	    return (String) response.getEnvironmentJsons().get(envKey);
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during getting app [%s] env [%s]." + siteInfo, appId, envKey), e);
	}
    }

    private void upload(String appId, String path) {
	try {
	    UploadApplicationRequest request = UploadApplicationRequest.builder().applicationId(appId)
		    .application(new File(Main.storePath + path).toPath()).build();
	    logger.info("App [{}] package [{}] start uploading", appId, path);
	    retry(() -> cloudFoundryClient.applicationsV2().upload(request)
		    .block(Duration.ofSeconds(opConfig.getUploadTimeout())));
	    logger.info("App [{}] package [{}] uploaded.", appId, path);
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during uploading app [%s] with path [%s]", appId, path) + siteInfo, e);
	}
    }

    private SummaryApplicationResponse getAppSummary(String appId) {
	try {
	    SummaryApplicationRequest request = SummaryApplicationRequest.builder().applicationId(appId).build();
	    SummaryApplicationResponse response = retry(
		    () -> cloudFoundryClient.applicationsV2().summary(request).block(timeout));
	    return response;
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during getting app [%s] summary." + siteInfo, appId), e);
	}
    }

    private String getDomainId(String domain) {
	try {
	    ListDomainsRequest request = ListDomainsRequest.builder().name(domain).build();
	    @SuppressWarnings("deprecation")
	    ListDomainsResponse response = retry(() -> cloudFoundryClient.domains().list(request).block(timeout));
	    if (response.getResources().size() == 0) {
		return null;
	    }
	    assert response.getResources().size() == 1;
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during getting domain id for domain name: [%s].", domain) + siteInfo, e);
	}
    }

    private void createAndMapAppRoute(String appId, Route route) {
	String domainId = getDomainId(route.getDomain());
	if (domainId == null) {
	    throw new IllegalStateException(String.format("Can't find the domain [%s].", route.getDomain()));
	}
	String routeId = getRouteId(route.getHostname(), domainId);
	if (routeId == null) {
	    routeId = createRoute(route.getHostname(), domainId);
	}
	createRouteMapping(appId, routeId);
	logger.info("route [{}] mapped to the app [{}]", routeId, appId);
    }

    private void unmapAppRoute(String appId, Route route) {
	String domainId = getDomainId(route.getDomain());
	String routeId = getRouteId(route.getHostname(), domainId);
	if (routeId != null) {
	    String routeMappingId = getRouteMappingId(appId, routeId);
	    if (routeMappingId != null) {
		deleteRouteMapping(routeMappingId);
	    }
	}
	logger.info("route [{}] unmapped from the app [{}]", routeId, appId);
    }

    private String getRouteId(String hostname, String domainId) {
	try {
	    ListRoutesRequest request = ListRoutesRequest.builder().domainId(domainId).host(hostname).build();
	    ListRoutesResponse response = retry(() -> cloudFoundryClient.routes().list(request).block(timeout));
	    if (response.getResources().size() == 0) {
		return null;
	    }
	    assert response.getResources().size() == 1;
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Exception during getting route id with hostname:[%s], domainId:[%s].", hostname,
			    domainId) + siteInfo,
		    e);
	}
    }

    private String createRoute(String hostname, String domainId) {
	try {
	    CreateRouteRequest request = CreateRouteRequest.builder().domainId(domainId).spaceId(spaceId).host(hostname)
		    .build();
	    CreateRouteResponse response = retry(() -> cloudFoundryClient.routes().create(request).block(timeout));
	    return response.getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during creating route with hostname:[%s], domainId:[%s].", hostname,
			    domainId) + siteInfo,
		    e);
	}
    }

    private void createRouteMapping(String appId, String routeId) {
	try {
	    CreateRouteMappingRequest request = CreateRouteMappingRequest.builder().applicationId(appId)
		    .routeId(routeId).build();
	    retry(() -> cloudFoundryClient.routeMappings().create(request).block(timeout));
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during creating route mapping between app [%s] and route [%s].", appId,
			    routeId) + siteInfo,
		    e);
	}
    }

    private String getRouteMappingId(String appId, String routeId) {
	try {
	    ListRouteMappingsRequest request = ListRouteMappingsRequest.builder().applicationId(appId).routeId(routeId)
		    .build();
	    ListRouteMappingsResponse response = retry(
		    () -> cloudFoundryClient.routeMappings().list(request).block(timeout));
	    if (response == null) {
		return null;
	    }
	    if (response.getResources() == null) {
		return null;
	    }
	    if (response.getResources().get(0) == null) {
		return null;
	    }
	    assert response.getResources().size() == 1;
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during getting route mapping id between app [%s] and route [%s].", appId,
			    routeId) + siteInfo,
		    e);
	}
    }

    private void deleteRouteMapping(String routeMappingId) {
	try {
	    DeleteRouteMappingRequest request = DeleteRouteMappingRequest.builder().routeMappingId(routeMappingId)
		    .build();
	    retry(() -> cloudFoundryClient.routeMappings().delete(request).block(timeout));
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during deleting route mapping: [%s]", routeMappingId) + siteInfo, e);
	}
    }

    /**
     * Update app properties. Unchanged app property parameter should be null.
     *
     * @param appId
     * @param name
     * @param env
     * @param instances
     * @param state
     */
    private void updateApp(String appId, String name, Map<String, String> env, Integer instances,
	    CFMicroserviceDesiredState state) {
	try {
	    UpdateApplicationRequest request = UpdateApplicationRequest.builder().applicationId(appId).name(name)
		    .environmentJsons(env).instances(instances).state(state == null ? null : state.name()).build();
	    retry(() -> cloudFoundryClient.applicationsV2().update(request).block(timeout));
	} catch (Exception e) {
	    throw new IllegalStateException(String.format(
		    "Exception during updating app [%s] with arg [name=%s, env=%s, instances=%s, state=%s]", appId,
		    name, env, instances, state) + siteInfo, e);
	}

    }

    private void updateAppEnv(String appId, Map<String, String> env) {
	updateApp(appId, null, env, null, null);
	logger.info("app {} env updated to {}.", appId, env);
    }

    private void bindAppServices(String appId, String serviceName) {
	CreateServiceBindingRequest bindRequest = CreateServiceBindingRequest.builder().applicationId(appId)
		.serviceInstanceId(getServiceInstanceId(serviceName)).build();
	retry(() -> cloudFoundryClient.serviceBindingsV2().create(bindRequest).block(timeout));
    }

    private void unbindAppServices(String appId, String serviceName) {
	String serviceBindId = getServiceBindingId(appId, getServiceInstanceId(serviceName));
	if (serviceBindId == null) {
	    logger.error(
		    "The binding between App [{}] and service instance [{}] not exists. Therefore the unbind is not performed.");
	    return;
	}
	DeleteServiceBindingRequest unBindRequest = DeleteServiceBindingRequest.builder()
		.serviceBindingId(serviceBindId).build();
	retry(() -> cloudFoundryClient.serviceBindingsV2().delete(unBindRequest).block(timeout));
    }

    private String getServiceBindingId(String appId, String serviceId) {
	ListServiceBindingsRequest request = ListServiceBindingsRequest.builder().applicationId(appId)
		.serviceInstanceId(serviceId).build();
	ListServiceBindingsResponse response = retry(
		() -> cloudFoundryClient.serviceBindingsV2().list(request).block(timeout));
	if (response.getResources().size() == 0) {
	    return null;
	}
	assert response.getResources().size() == 1;
	return response.getResources().get(0).getMetadata().getId();
    }

    private String getServiceInstanceId(String serviceName) {
	ListSpaceServiceInstancesRequest serviceInstancesRequest = ListSpaceServiceInstancesRequest.builder()
		.spaceId(spaceId).name(serviceName).returnUserProvidedServiceInstances(true).build();
	ListSpaceServiceInstancesResponse serviceInstancesResponse = retry(
		() -> cloudFoundryClient.spaces().listServiceInstances(serviceInstancesRequest).block(timeout));
	if (serviceInstancesResponse.getResources().size() == 0) {
	    throw new IllegalStateException(String.format("Service instance [%s] not exists" + siteInfo, serviceName));
	}
	assert serviceInstancesResponse.getResources().size() == 1;
	return serviceInstancesResponse.getResources().get(0).getMetadata().getId();
    }

    private <T> T retry(Supplier<T> function) {
	return new RetryFunction<T>(opConfig.getGeneralRetry(), opConfig.getGeneralBackoff()).run(function, siteInfo);
    }
}
