package com.portfolio.identity_service.client.rest;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

	@Bean
	ClientHttpRequestInterceptor bearerTokenRelayInterceptor() {
		return (request, body, execution) -> {
			var attributes = RequestContextHolder.getRequestAttributes();
			if (attributes instanceof ServletRequestAttributes servletAttributes
					&& !request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
				var authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
				if (authorization != null && authorization.startsWith("Bearer ")) {
					request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
				}
			}
			return execution.execute(request, body);
		};
	}

	@Bean
	@LoadBalanced
	RestClient.Builder restClientBuilder(
			@Qualifier("bearerTokenRelayInterceptor") ClientHttpRequestInterceptor interceptor) {
		return RestClient.builder().requestInterceptor(interceptor);
	}
}
