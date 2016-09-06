package com.orange.paas.cf;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.paas.RouteFactory;

public class CloudFoundryRouteFactory extends RouteFactory {
	private CloudFoundryOperations operations;
	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryRouteFactory.class);

	public CloudFoundryRouteFactory(Map<String, String> domains, CloudFoundryOperations operations) {
		super(domains);
		this.operations = operations;
	}
	
	private String getDomainId(String domainKey) {
		logger.error("domain {}", domains);
		logger.error("local domain: {}; global domain: {}", this.domains.get("local"), this.domains.get("global")); 
		return operations.getDomainId(this.domains.get(domainKey));
	}
	
	@Override
	public String getRouteId(String hostname, String domainKey) {
		return operations.getRouteId(getDomainId(domainKey), hostname);
	}

	@Override
	public String createRouteIfNotExist(String hostname, String domainKey) {
		String routeId = getRouteId(hostname, domainKey);
		if (routeId != null) {
			logger.info("route existed with id: {}", routeId);
			return routeId;
		}
		routeId = operations.createRoute(hostname, getDomainId(domainKey));
		logger.info("route created with id: {}", routeId);
		return routeId;
	}

	@Override
	public void createRouteMapping(String appId, String routeId) {
		operations.createRouteMapping(appId, routeId);
	}

	@Override
	public void deleteRouteMapping(String appId, String routeId) {
		String routeMappingId = operations.getRouteMappingId(appId, routeId);
		if (routeMappingId != null) {
			logger.info("route-mapping id found: {}", routeMappingId);
			operations.deleteRouteMapping(routeMappingId);
			logger.info("route unmapped", routeMappingId);
		} else {
			logger.info("route-mapping not found");
		}
	}

}
