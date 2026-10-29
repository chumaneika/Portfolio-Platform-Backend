package com.portfolio.profile_service.client.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Bean
	ExchangeFilterFunction bearerTokenRelayFilter() {
		return (request, next) -> {
			var authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!request.headers().containsHeader(HttpHeaders.AUTHORIZATION)
					&& authentication instanceof JwtAuthenticationToken jwtAuthentication) {
				var requestWithToken = ClientRequest.from(request)
						.headers(headers -> headers.setBearerAuth(jwtAuthentication.getToken().getTokenValue()))
						.build();
				return next.exchange(requestWithToken);
			}
			return next.exchange(request);
		};
	}

	@Bean
	@LoadBalanced
	WebClient.Builder serviceWebClientBuilder(
			@Qualifier("bearerTokenRelayFilter") ExchangeFilterFunction tokenRelayFilter) {
		return WebClient.builder().filter(tokenRelayFilter);
	}
}
