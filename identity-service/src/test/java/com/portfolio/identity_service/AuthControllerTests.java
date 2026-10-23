package com.portfolio.identity_service;

import com.portfolio.identity_service.dto.response.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"eureka.client.register-with-eureka=false",
				"eureka.client.fetch-registry=false",
				"spring.datasource.url=jdbc:h2:mem:identity_controller_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.jpa.hibernate.ddl-auto=validate",
				"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ=",
				"security.jwt.issuer=identity-service-test",
				"security.jwt.access-token-ttl=15m",
				"security.jwt.refresh-token-ttl=30d"
		})
class AuthControllerTests {

	@LocalServerPort
	private int port;

	@Test
	@DisplayName("AuthController supports register, login, refresh and logout")
	void fullAuthenticationFlow() {
		var client = client();
		var suffix = UUID.randomUUID().toString();
		var credentials = Map.of(
				"email", "flow-" + suffix + "@example.com",
				"username", "flow-" + suffix,
				"password", "strong-password");

		var registerResponse = client.post()
				.uri("/api/v1/auth/register")
				.body(credentials)
				.retrieve()
				.toBodilessEntity();
		assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		var loginResponse = client.post()
				.uri("/api/v1/auth/login")
				.body(Map.of(
						"email", credentials.get("email"),
						"password", credentials.get("password")))
				.retrieve()
				.body(TokenResponse.class);
		assertThat(loginResponse).isNotNull();
		assertThat(loginResponse.accessToken()).isNotBlank();
		assertThat(loginResponse.refreshToken()).isNotBlank();

		assertFailure(
				() -> client.get().uri("/api/v1/private-check").retrieve().toBodilessEntity(),
				HttpStatus.UNAUTHORIZED,
				null);
		assertFailure(
				() -> client.get()
						.uri("/api/v1/private-check")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.accessToken())
						.retrieve()
						.toBodilessEntity(),
				HttpStatus.NOT_FOUND,
				"resource_not_found");

		var refreshResponse = client.post()
				.uri("/api/v1/auth/refresh")
				.body(Map.of("refreshToken", loginResponse.refreshToken()))
				.retrieve()
				.body(TokenResponse.class);
		assertThat(refreshResponse).isNotNull();
		assertThat(refreshResponse.accessToken()).isNotEqualTo(loginResponse.accessToken());
		assertThat(refreshResponse.refreshToken()).isNotEqualTo(loginResponse.refreshToken());

		assertFailure(
				() -> client.post()
						.uri("/api/v1/auth/refresh")
						.body(Map.of("refreshToken", loginResponse.refreshToken()))
						.retrieve()
						.toBodilessEntity(),
				HttpStatus.UNAUTHORIZED,
				"authentication_failed");

		var logoutResponse = client.post()
				.uri("/api/v1/auth/logout")
				.body(Map.of("refreshToken", refreshResponse.refreshToken()))
				.retrieve()
				.toBodilessEntity();
		assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		assertFailure(
				() -> client.post()
						.uri("/api/v1/auth/refresh")
						.body(Map.of("refreshToken", refreshResponse.refreshToken()))
						.retrieve()
						.toBodilessEntity(),
				HttpStatus.UNAUTHORIZED,
				"authentication_failed");
	}

	@Test
	@DisplayName("AuthController returns understandable registration and login errors")
	void validationAndAuthenticationErrors() {
		var client = client();
		var suffix = UUID.randomUUID().toString();
		var email = "errors-" + suffix + "@example.com";
		var username = "errors-" + suffix;

		assertFailure(
				() -> client.post()
						.uri("/api/v1/auth/register")
						.body(Map.of(
								"email", "not-an-email",
								"username", "bad username",
								"password", "short"))
						.retrieve()
						.toBodilessEntity(),
				HttpStatus.BAD_REQUEST,
				"validation_failed");

		client.post()
				.uri("/api/v1/auth/register")
				.body(Map.of(
						"email", email,
						"username", username,
						"password", "strong-password"))
				.retrieve()
				.toBodilessEntity();

		assertFailure(
				() -> client.post()
						.uri("/api/v1/auth/register")
						.body(Map.of(
								"email", email.toUpperCase(),
								"username", "another-" + suffix,
								"password", "strong-password"))
						.retrieve()
						.toBodilessEntity(),
				HttpStatus.CONFLICT,
				"account_conflict");

		assertFailure(
				() -> client.post()
						.uri("/api/v1/auth/login")
						.body(Map.of("email", email, "password", "wrong-password"))
						.retrieve()
						.toBodilessEntity(),
				HttpStatus.UNAUTHORIZED,
				"authentication_failed");
	}

	private RestClient client() {
		return RestClient.create("http://localhost:" + port);
	}

	private static void assertFailure(
			Runnable request,
			HttpStatus expectedStatus,
			String expectedCode) {
		try {
			request.run();
			throw new AssertionError("Expected request to fail with " + expectedStatus);
		} catch (RestClientResponseException exception) {
			assertThat(exception.getStatusCode()).isEqualTo(expectedStatus);
			if (expectedCode != null) {
				assertThat(exception.getResponseBodyAsString())
						.contains("\"code\":\"" + expectedCode + "\"");
			}
		}
	}
}
