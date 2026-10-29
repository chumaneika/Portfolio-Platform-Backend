package com.portfolio.portfolio_service.client.rest;

import com.portfolio.portfolio_service.client.ProfileClient;
import com.portfolio.portfolio_service.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class RestProfileClient implements ProfileClient {

	private final RestClient restClient;

	public RestProfileClient(@Qualifier("serviceRestClientBuilder") RestClient.Builder builder) {
		this.restClient = builder.clone().baseUrl("http://profile-service").build();
	}

	@Override
	public Optional<Map<String, Object>> getPublicProfile(UUID accountId) {
		try {
			return Optional.ofNullable(restClient.get()
					.uri("/api/v1/profiles/by-account/{accountId}", accountId)
					.retrieve()
					.body(new ParameterizedTypeReference<Map<String, Object>>() {}));
		} catch (HttpClientErrorException.NotFound exception) {
			return Optional.empty();
		} catch (RestClientException exception) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
					"profile_service_unavailable", "Profile service is unavailable");
		}
	}
}
