package com.portfolio.profile_service.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientConfig {

	@Bean
	ClientHttpRequestInterceptor bearerTokenRelayInterceptor() {
		return (request, body, execution) -> {
			var authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)
					&& authentication instanceof JwtAuthenticationToken jwtAuthentication) {
				request.getHeaders().setBearerAuth(jwtAuthentication.getToken().getTokenValue());
			}
			return execution.execute(request, body);
		};
	}

	@Bean
	@LoadBalanced
	RestClient.Builder serviceRestClientBuilder(
			@Qualifier("bearerTokenRelayInterceptor") ClientHttpRequestInterceptor tokenRelayInterceptor) {
		return RestClient.builder().requestInterceptor(tokenRelayInterceptor);
	}
}
