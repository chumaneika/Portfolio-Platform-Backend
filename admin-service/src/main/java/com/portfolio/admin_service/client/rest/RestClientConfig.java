package com.portfolio.admin_service.client.rest;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	@LoadBalanced
	RestClient.Builder serviceRestClientBuilder() {
		return RestClient.builder();
	}
}
