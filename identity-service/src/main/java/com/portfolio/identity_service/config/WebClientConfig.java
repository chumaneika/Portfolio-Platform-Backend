package com.portfolio.identity_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Bean
	ExchangeFilterFunction bearerTokenRelayFilter() {
		return (request, next) -> {
			var attributes = RequestContextHolder.getRequestAttributes();
			if (attributes instanceof ServletRequestAttributes servletAttributes
					&& !request.headers().containsHeader(HttpHeaders.AUTHORIZATION)) {
				var authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
				if (authorization != null && authorization.startsWith("Bearer ")) {
					var requestWithToken = ClientRequest.from(request)
							.header(HttpHeaders.AUTHORIZATION, authorization)
							.build();
					return next.exchange(requestWithToken);
				}
			}
			return next.exchange(request);
		};
	}

	@Bean
	@LoadBalanced
	WebClient.Builder webClientBuilder(
			@Qualifier("bearerTokenRelayFilter") ExchangeFilterFunction filter) {
		return WebClient.builder().filter(filter);
	}
}
