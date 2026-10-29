package com.portfolio.api_gateway_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false"
})
class ApiGatewayServiceApplicationTests {
	@Autowired
	private RouteDefinitionLocator routeDefinitionLocator;

	@Test
	void contextLoads() {
	}

	@Test
	void limitedRoutesTakePriorityOverGeneralRoutes() {
		var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
		assertThat(routes).isNotNull();
		var ids = routes.stream().map(route -> route.getId()).toList();
		assertThat(ids.indexOf("auth-register")).isLessThan(ids.indexOf("identity-service"));
		assertThat(ids.indexOf("auth-login")).isLessThan(ids.indexOf("identity-service"));
		assertThat(ids.indexOf("auth-refresh")).isLessThan(ids.indexOf("identity-service"));
		assertThat(ids.indexOf("public-accounts")).isLessThan(ids.indexOf("identity-service"));
		assertThat(ids.indexOf("public-profiles")).isLessThan(ids.indexOf("profile-service"));
		assertThat(ids.indexOf("public-portfolios")).isLessThan(ids.indexOf("portfolio-service"));
		assertThat(routes.stream().filter(route -> route.getId().startsWith("auth-")
				|| route.getId().startsWith("public-")))
				.allSatisfy(route -> assertThat(route.getFilters())
						.anySatisfy(filter -> assertThat(filter.getName()).isEqualTo("RequestRateLimiter")));
	}

	@Test
	void clientIpUsesProxyHeaderOnlyWhenEnabled() {
		var request = MockServerHttpRequest.get("/")
				.remoteAddress(new InetSocketAddress("192.0.2.10", 12345))
				.header("X-Client-IP", "198.51.100.25")
				.build();
		var exchange = MockServerWebExchange.from(request);
		var config = new ClientIpRateLimitConfig();

		assertThat(config.clientIpKeyResolver(false).resolve(exchange).block()).isEqualTo("192.0.2.10");
		assertThat(config.clientIpKeyResolver(true).resolve(exchange).block()).isEqualTo("198.51.100.25");
	}

}
