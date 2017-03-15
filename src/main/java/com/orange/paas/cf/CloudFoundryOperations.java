package com.orange.paas.cf;

import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.client.CloudFoundryClient;
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
import org.cloudfoundry.client.v2.spaces.GetSpaceSummaryRequest;
import org.cloudfoundry.client.v2.spaces.GetSpaceSummaryResponse;
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

import com.orange.model.AppDesiredState;
import com.orange.model.OperationConfig;
import com.orange.model.PaaSAccessInfo;
import com.orange.model.state.Route;
import com.orange.util.RetryFunction;

public class CloudFoundryOperations {
    private final Logger logger;
    private PaaSAccessInfo siteAccessInfo;
    private CloudFoundryClient cloudFoundryClient;
    private String spaceId;
    private OperationConfig opConfig;

    public CloudFoundryOperations(PaaSAccessInfo siteAccessInfo, OperationConfig opConfig) {
	this.siteAccessInfo = siteAccessInfo;
	this.logger = LoggerFactory.getLogger(String.format("%s(%s)", getClass(), siteAccessInfo.getName()));
	this.opConfig = opConfig;
	String proxy_host = System.getenv("proxy_host");
	String proxy_port = System.getenv("proxy_port");
	DefaultConnectionContext.Builder connectionContext = DefaultConnectionContext.builder()
		.apiHost(siteAccessInfo.getApi()).skipSslValidation(siteAccessInfo.getSkipSslValidation())
		.socketTimeout(Duration.ofSeconds(opConfig.getGeneralTimeout()));
	if (proxy_host != null && proxy_port != null) {
	    ProxyConfiguration proxyConfiguration = ProxyConfiguration.builder().host(proxy_host)
		    .port(Integer.parseInt(proxy_port)).build();
	    connectionContext.proxyConfiguration(proxyConfiguration);
	}
	TokenProvider tokenProvider = PasswordGrantTokenProvider.builder().password(siteAccessInfo.getPwd())
		.username(siteAccessInfo.getUser()).build();
	this.cloudFoundryClient = ReactorCloudFoundryClient.builder().connectionContext(connectionContext.build())
		.tokenProvider(tokenProvider).build();
	this.spaceId = requestSpaceId();
    }

    private String requestOrgId() {
	try {
	    ListOrganizationsRequest request = ListOrganizationsRequest.builder().name(siteAccessInfo.getOrg()).build();
	    ListOrganizationsResponse response = retry(() -> cloudFoundryClient.organizations().list(request).block());
	    logger.trace("Got organization id.");
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during getting org id", e);
	}
    }

    private String requestSpaceId() {
	try {
	    ListSpacesRequest request = ListSpacesRequest.builder().organizationId(requestOrgId())
		    .name(siteAccessInfo.getSpace()).build();
	    ListSpacesResponse response = retry(() -> cloudFoundryClient.spaces().list(request).block());
	    logger.trace("Got space id.");
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during getting space id", e);
	}
    }

    public List<SpaceApplicationSummary> listSpaceApps() {
	try {
	    GetSpaceSummaryRequest request = GetSpaceSummaryRequest.builder().spaceId(spaceId).build();
	    logger.trace("Start requesting space application summary...");
	    GetSpaceSummaryResponse response = retry(() -> cloudFoundryClient.spaces().getSummary(request).block());
	    logger.trace("Got space apps!");
	    return response.getApplications();
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during getting space apps", e);
	}
    }

    public SummaryApplicationResponse getAppSummary(String appId) {
	try {
	    SummaryApplicationRequest request = SummaryApplicationRequest.builder().applicationId(appId).build();
	    SummaryApplicationResponse response = retry(
		    () -> cloudFoundryClient.applicationsV2().summary(request).block());
	    return response;
	} catch (Exception e) {
	    throw new IllegalStateException(String.format("Expcetion during getting app [%s] summary.", appId), e);
	}
    }

    public String createApp(String name, int instances, Map<String, String> env) {
	try {
	    CreateApplicationRequest request = CreateApplicationRequest.builder().name(name).spaceId(spaceId)
		    .instances(instances).environmentJsons(env).build();
	    CreateApplicationResponse response = retry(
		    () -> cloudFoundryClient.applicationsV2().create(request).block());
	    return response.getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(String
		    .format("Expcetion during creating app [name=%s, instances=%s, env=%s].", name, instances, env), e);
	}
    }

    public void deleteApp(String appId) {
	try {
	    DeleteApplicationRequest request = DeleteApplicationRequest.builder().applicationId(appId).build();
	    retry(() -> cloudFoundryClient.applicationsV2().delete(request).block());
	    logger.info("App {} at {} deleted.", appId, siteAccessInfo.getName());
	} catch (Exception e) {
	    throw new IllegalStateException("expcetion during deleting app with id: " + appId, e);
	}
    }

    public void uploadApp(String appId, String path) {
	try {
	    UploadApplicationRequest request = UploadApplicationRequest.builder().applicationId(appId)
		    .application(new FileInputStream(new File(path))).build();
	    logger.info("App [{}] package [{}] start uploading", appId, path);
	    retry(() -> cloudFoundryClient.applicationsV2().upload(request).block());
	    logger.info("App [{}] package [{}] uploaded.", appId, path);
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during uploading app [%s] with path [%s]", appId, path), e);
	}
    }

    public void restageApp(String appId) {
	RestageApplicationRequest request = RestageApplicationRequest.builder().applicationId(appId).build();
	retry(() -> cloudFoundryClient.applicationsV2().restage(request).block());
    }

    public String getDomainId(String domain) {
	try {
	    ListDomainsRequest request = ListDomainsRequest.builder().name(domain).build();
	    ListDomainsResponse response = retry(() -> cloudFoundryClient.domains().list(request).block());
	    if (response.getResources().size() == 0) {
		return null;
	    }
	    assert response.getResources().size() == 1;
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during getting domain id for domain name: [%s].", domain), e);
	}
    }

    public String getDomainName(String domainId) {
	if (domainId == null) {
	    return null;
	}
	GetDomainRequest request = GetDomainRequest.builder().domainId(domainId).build();
	GetDomainResponse response = retry(() -> cloudFoundryClient.domains().get(request).block());
	if (response.getEntity() == null) {
	    return null;
	} else {
	    return response.getEntity().getName();
	}
    }

    public String getRouteId(String hostname, String domainId) {
	try {
	    ListRoutesRequest request = ListRoutesRequest.builder().domainId(domainId).host(hostname).build();
	    ListRoutesResponse response = retry(() -> cloudFoundryClient.routes().list(request).block());
	    if (response.getResources().size() == 0) {
		return null;
	    }
	    assert response.getResources().size() == 1;
	    return response.getResources().get(0).getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(String.format(
		    "Exception during getting route id with hostname:[%s], domainId:[%s].", hostname, domainId), e);
	}
    }

    public String createRoute(String hostname, String domainId) {
	try {
	    CreateRouteRequest request = CreateRouteRequest.builder().domainId(domainId).spaceId(spaceId).host(hostname)
		    .build();
	    CreateRouteResponse response = retry(() -> cloudFoundryClient.routes().create(request).block());
	    return response.getMetadata().getId();
	} catch (Exception e) {
	    throw new IllegalStateException(String.format(
		    "Expcetion during creating route with hostname:[%s], domainId:[%s].", hostname, domainId), e);
	}
    }

    public String getRouteHost(String routeId) {
	GetRouteRequest request = GetRouteRequest.builder().routeId(routeId).build();
	GetRouteResponse response = retry(() -> cloudFoundryClient.routes().get(request).block());
	if (response.getEntity() == null) {
	    return null;
	} else {
	    return response.getEntity().getHost();
	}
    }

    public String getRouteDomainId(String routeId) {
	GetRouteRequest request = GetRouteRequest.builder().routeId(routeId).build();
	GetRouteResponse response = retry(() -> cloudFoundryClient.routes().get(request).block());
	if (response.getEntity() == null) {
	    return null;
	} else {
	    return response.getEntity().getDomainId();
	}
    }

    public Route getRoute(String routeId) {
	GetRouteRequest request = GetRouteRequest.builder().routeId(routeId).build();
	GetRouteResponse response = retry(() -> cloudFoundryClient.routes().get(request).block());
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
		    () -> cloudFoundryClient.applicationsV2().listRoutes(request).block());
	    return response.getResources().stream().map(resource -> resource.getMetadata().getId())
		    .collect(Collectors.toSet());
	} catch (Exception e) {
	    throw new IllegalStateException(String.format("Expcetion during listing mapped routes of app: [%s]", appId),
		    e);
	}
    }

    public void createRouteMapping(String appId, String routeId) {
	try {
	    CreateRouteMappingRequest request = CreateRouteMappingRequest.builder().applicationId(appId)
		    .routeId(routeId).build();
	    retry(() -> cloudFoundryClient.routeMappings().create(request).block());
	} catch (Exception e) {
	    throw new IllegalStateException(String.format(
		    "Expcetion during creating route mapping between app [%s] and route [%s].", appId, routeId), e);
	}
    }

    public String getRouteMappingId(String appId, String routeId) {
	try {
	    ListRouteMappingsRequest request = ListRouteMappingsRequest.builder().applicationId(appId).routeId(routeId)
		    .build();
	    ListRouteMappingsResponse response = retry(() -> cloudFoundryClient.routeMappings().list(request).block());
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
	    throw new IllegalStateException(String.format(
		    "Expcetion during getting route mapping id between app [%s] and route [%s].", appId, routeId), e);
	}
    }

    public void deleteRouteMapping(String routeMappingId) {
	try {
	    DeleteRouteMappingRequest request = DeleteRouteMappingRequest.builder().routeMappingId(routeMappingId)
		    .build();
	    retry(() -> cloudFoundryClient.routeMappings().delete(request).block());
	} catch (Exception e) {
	    throw new IllegalStateException(
		    String.format("Expcetion during deleting route mapping: [%s]", routeMappingId), e);
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
	    AppDesiredState state) {
	try {
	    UpdateApplicationRequest request = UpdateApplicationRequest.builder().applicationId(appId).name(name)
		    .environmentJsons(env).instances(instances).state(state == null ? null : state.name()).build();
	    retry(() -> cloudFoundryClient.applicationsV2().update(request).block());
	} catch (Exception e) {
	    throw new IllegalStateException(String.format(
		    "Exception during updating app [%s] with arg [name=%s, env=%s, instances=%s, state=%s]", appId,
		    name, env, instances, state), e);
	}

    }

    private <T> T retry(Supplier<T> function) {
	return new RetryFunction<T>(opConfig.getGeneralRetry(), opConfig.getGeneralBackoff()).run(function);
    }
}
