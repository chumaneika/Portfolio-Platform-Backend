package com.portfolio.portfolio_service;

import com.portfolio.portfolio_service.client.rest.RestProfileClient;
import com.portfolio.portfolio_service.exceptions.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestProfileClientTests {

	@Test
	void requestsProfileByAccountId() {
		var accountId = UUID.randomUUID();
		var builder = RestClient.builder();
		var server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://profile-service/api/v1/profiles/by-account/" + accountId))
				.andRespond(withSuccess("{\"profile\":{\"firstName\":\"Test\"}}", MediaType.APPLICATION_JSON));

		var response = new RestProfileClient(builder).getPublicProfile(accountId);

		assertThat(response).isPresent();
		assertThat(response.orElseThrow()).containsKey("profile");
		server.verify();
	}

	@Test
	void treatsMissingProfileAsOptional() {
		var accountId = UUID.randomUUID();
		var builder = RestClient.builder();
		var server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://profile-service/api/v1/profiles/by-account/" + accountId))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThat(new RestProfileClient(builder).getPublicProfile(accountId)).isEmpty();
		server.verify();
	}

	@Test
	void reportsProfileServiceFailure() {
		var accountId = UUID.randomUUID();
		var builder = RestClient.builder();
		var server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://profile-service/api/v1/profiles/by-account/" + accountId))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatThrownBy(() -> new RestProfileClient(builder).getPublicProfile(accountId))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
					assertThat(exception.getCode()).isEqualTo("profile_service_unavailable");
				});
		server.verify();
	}
}
