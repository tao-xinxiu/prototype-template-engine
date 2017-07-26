package com.orange.paas.cf;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.CreateApplicationRequest;
import org.cloudfoundry.client.v2.applications.CreateApplicationResponse;
import org.cloudfoundry.client.v2.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationRoutesResponse;
import org.cloudfoundry.client.v2.applications.RestageApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.applications.UploadApplicationRequest;
import org.cloudfoundry.client.v2.domains.GetDomainRequest;
import org.cloudfoundry.client.v2.domains.GetDomainResponse;
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
import org.cloudfoundry.client.v2.routes.GetRouteRequest;
import org.cloudfoundry.client.v2.routes.GetRouteResponse;
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
import com.orange.model.PaaSSite;
import com.orange.model.state.Route;
import com.orange.model.state.cf.CFAppDesiredState;
import com.orange.util.RetryFunction;

public class CloudFoundryOperations {
    private static final String runningState = "RUNNING";
    private static final String stagedState = "STAGED";

    private final Logger logger;
    private final String siteInfo;

    private PaaSSite site;
    private CloudFoundryClient cloudFoundryClient;
    private String spaceId;
    private OperationConfig opConfig;
    private Duration timeout;

    public CloudFoundryOperations(PaaSSite site, OperationConfig opConfig) {
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
		    .connectTimeout(connectTimeout).sslHandshakeTimeout(connectTimeout);
	    if (proxy_host != null && proxy_port != null) {
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

    public OperationConfig getOpConfig() {
	return opConfig;
    }

    public String getSiteName() {
	return site.getName();
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

    public SummaryApplicationResponse getAppSummary(String appId) {
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

    public boolean appRunning(String appId) {
	try {
	    if (!CFAppDesiredState.STARTED.toString().equals(getAppSummary(appId).getState())) {
		return false;
	    }
	    ApplicationInstancesRequest request = ApplicationInstancesRequest.builder().applicationId(appId).build();
	    ApplicationInstancesResponse response = retry(
		    () -> cloudFoundryClient.applicationsV2().instances(request).block(timeout));
	    return response.getInstances().entrySet().stream()
		    .anyMatch(entity -> runningState.equals(entity.getValue().getState()));
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during getting whether app [%s] running." + siteInfo, appId), e);
	}
    }

    public boolean appStaged(String appId) {
	return stagedState.equals(getAppSummary(appId).getPackageState());
    }

    public String createApp(String name, int instances, Map<String, String> env) {
	try {
	    CreateApplicationRequest request = CreateApplicationRequest.builder().name(name).spaceId(spaceId)
		    .instances(instances).environmentJsons(env).build();
	    CreateApplicationResponse response = retry(
		    () -> cloudFoundryClient.applicationsV2().create(request).block(timeout));
	    logger.info("App [{}] created.", name);
	    return response.getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during creating app [name=%s, instances=%s, env=%s]." + siteInfo, name,
			    instances, env),
		    e);
	}
    }

    public void deleteApp(String appId) {
	try {
	    DeleteApplicationRequest request = DeleteApplicationRequest.builder().applicationId(appId).build();
	    retry(() -> cloudFoundryClient.applicationsV2().delete(request).block(timeout));
	    logger.info("App {} at {} deleted.", appId, site.getName());
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during deleting app with id: " + appId + siteInfo, e);
	}
    }

    public void uploadApp(String appId, String path) {
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

    public void restageApp(String appId) {
	RestageApplicationRequest request = RestageApplicationRequest.builder().applicationId(appId).build();
	retry(() -> cloudFoundryClient.applicationsV2().restage(request).block(timeout));
    }

    private String getDomainId(String domain) {
	try {
	    ListDomainsRequest request = ListDomainsRequest.builder().name(domain).build();
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

    public String getDomainName(String domainId) {
	if (domainId == null) {
	    return null;
	}
	GetDomainRequest request = GetDomainRequest.builder().domainId(domainId).build();
	GetDomainResponse response = retry(() -> cloudFoundryClient.domains().get(request).block(timeout));
	if (response.getEntity() == null) {
	    return null;
	} else {
	    return response.getEntity().getName();
	}
    }

    public void createAndMapAppRoute(String appId, Route route) {
	String domainId = getDomainId(route.getDomain());
	String routeId = getRouteId(route.getHostname(), domainId);
	if (routeId == null) {
	    routeId = createRoute(route.getHostname(), domainId);
	}
	createRouteMapping(appId, routeId);
	logger.info("route [{}] mapped to the app [{}]", routeId, appId);
    }

    public void unmapAppRoute(String appId, Route route) {
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

    public String getRouteHost(String routeId) {
	GetRouteRequest request = GetRouteRequest.builder().routeId(routeId).build();
	GetRouteResponse response = retry(() -> cloudFoundryClient.routes().get(request).block(timeout));
	if (response.getEntity() == null) {
	    return null;
	} else {
	    return response.getEntity().getHost();
	}
    }

    public String getRouteDomainId(String routeId) {
	GetRouteRequest request = GetRouteRequest.builder().routeId(routeId).build();
	GetRouteResponse response = retry(() -> cloudFoundryClient.routes().get(request).block(timeout));
	if (response.getEntity() == null) {
	    return null;
	} else {
	    return response.getEntity().getDomainId();
	}
    }

    public Route getRoute(String routeId) {
	GetRouteRequest request = GetRouteRequest.builder().routeId(routeId).build();
	GetRouteResponse response = retry(() -> cloudFoundryClient.routes().get(request).block(timeout));
	if (response.getEntity() == null) {
	    return null;
	} else {
	    String host = response.getEntity().getHost();
	    String domainId = response.getEntity().getDomainId();
	    return new Route(host, getDomainName(domainId));
	}
    }

    public Set<String> listMappedRoutesId(String appId) {
	try {
	    ListApplicationRoutesRequest request = ListApplicationRoutesRequest.builder().applicationId(appId).build();
	    ListApplicationRoutesResponse response = retry(
		    () -> cloudFoundryClient.applicationsV2().listRoutes(request).block(timeout));
	    return response.getResources().stream().map(resource -> resource.getMetadata().getId())
		    .collect(Collectors.toSet());
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during listing mapped routes of app: [%s]", appId) + siteInfo, e);
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
    public void updateApp(String appId, String name, Map<String, String> env, Integer instances,
	    CFAppDesiredState state) {
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

    public void bindAppServices(String appId, String serviceName) {
	CreateServiceBindingRequest bindRequest = CreateServiceBindingRequest.builder().applicationId(appId)
		.serviceInstanceId(getServiceInstanceId(serviceName)).build();
	retry(() -> cloudFoundryClient.serviceBindingsV2().create(bindRequest).block(timeout));
    }

    public void unbindAppServices(String appId, String serviceName) {
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
	return new RetryFunction<T>(opConfig.getGeneralRetry(), opConfig.getGeneralBackoff()).run(function);
    }
}
