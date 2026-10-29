package com.portfolio.identity_service;

import com.portfolio.identity_service.dto.response.TokenResponse;
import com.portfolio.identity_service.dto.response.AccountSummaryResponse;
import com.portfolio.identity_service.dto.response.PublicAccountResponse;
import com.portfolio.identity_service.entities.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
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
				"spring.datasource.url=jdbc:h2:mem:account_controller_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.jpa.hibernate.ddl-auto=validate",
				"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ=",
				"security.jwt.issuer=identity-service-test",
				"security.jwt.access-token-ttl=15m",
				"security.jwt.refresh-token-ttl=30d"
		})
class AccountControllerTests {

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void returnsPublicAccountWithoutSensitiveFields() {
		var client = client();
		var email = register(client);
		var id = jdbcClient.sql("select id from accounts where email = :email")
				.param("email", email)
				.query(UUID.class)
				.single();

		var response = client.get().uri("/api/v1/accounts/{accountId}/public", id)
				.retrieve().body(PublicAccountResponse.class);
		assertThat(response).isNotNull();
		assertThat(response.id()).isEqualTo(id);
		assertThat(response.username()).isEqualTo(usernameOf(email));
		var json = client.get().uri("/api/v1/accounts/{accountId}/public", id)
				.retrieve().body(String.class);
		assertThat(json).doesNotContain("email", "status", "password");
		assertFailure(() -> client.get().uri("/api/v1/accounts/{accountId}/public", UUID.randomUUID())
				.retrieve().toBodilessEntity(), HttpStatus.NOT_FOUND, "account_not_found");
	}

	@Test
	void returnsCurrentAccountFromJwt() {
		var client = client();
		var email = register(client);
		var session = login(client, email, "strong-password");
		var path = "/api/v1/accounts/me";

		assertFailure(() -> client.get().uri(path).retrieve().toBodilessEntity(),
				HttpStatus.UNAUTHORIZED, null);
		var response = client.get().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.retrieve().body(AccountSummaryResponse.class);
		assertThat(response).isNotNull();
		assertThat(response.id()).isNotNull();
		assertThat(response.email()).isEqualTo(email);
		assertThat(response.username()).isEqualTo(usernameOf(email));
		assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
	}

	@Test
	void changesUsernameWithGlobalUniqueness() {
		var client = client();
		var firstEmail = register(client);
		var secondEmail = register(client);
		var session = login(client, firstEmail, "strong-password");
		var newUsername = "new-user-" + UUID.randomUUID();
		var path = "/api/v1/accounts/me/username";

		assertFailure(() -> client.patch().uri(path)
				.body(Map.of("username", newUsername))
				.retrieve().toBodilessEntity(),
				HttpStatus.UNAUTHORIZED, null);
		var changed = client.patch().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("username", newUsername.toUpperCase()))
				.retrieve().toBodilessEntity();
		assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(usernameOf(firstEmail)).isEqualTo(newUsername);

		var unchanged = client.patch().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("username", newUsername.toUpperCase()))
				.retrieve().toBodilessEntity();
		assertThat(unchanged.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		assertFailure(() -> client.patch().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("username", usernameOf(secondEmail).toUpperCase()))
				.retrieve().toBodilessEntity(),
				HttpStatus.CONFLICT, "username_already_exists");
		assertFailure(() -> client.patch().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("username", "bad name"))
				.retrieve().toBodilessEntity(),
				HttpStatus.BAD_REQUEST, "validation_failed");
		assertThat(usernameOf(firstEmail)).isEqualTo(newUsername);
		assertThat(login(client, firstEmail, "strong-password").accessToken()).isNotBlank();
	}

	@Test
	void revokesOnlyCurrentAccountsSessions() {
		var client = client();
		var firstEmail = register(client);
		var secondEmail = register(client);
		var firstSession = login(client, firstEmail, "strong-password");
		var anotherSession = login(client, firstEmail, "strong-password");
		var otherAccountSession = login(client, secondEmail, "strong-password");
		var path = "/api/v1/accounts/me/sessions";

		assertFailure(() -> client.delete().uri(path).retrieve().toBodilessEntity(),
				HttpStatus.UNAUTHORIZED, null);
		var response = client.delete().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + firstSession.accessToken())
				.retrieve().toBodilessEntity();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		assertFailure(() -> refresh(client, firstSession.refreshToken()),
				HttpStatus.UNAUTHORIZED, "authentication_failed");
		assertFailure(() -> refresh(client, anotherSession.refreshToken()),
				HttpStatus.UNAUTHORIZED, "authentication_failed");
		assertThat(refresh(client, otherAccountSession.refreshToken()).accessToken()).isNotBlank();

		var repeated = client.delete().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + firstSession.accessToken())
				.retrieve().toBodilessEntity();
		assertThat(repeated.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(login(client, firstEmail, "strong-password").accessToken()).isNotBlank();
	}

	@Test
	void changesPasswordAndRevokesEveryRefreshToken() {
		var client = client();
		var email = register(client);
		var firstSession = login(client, email, "strong-password");
		var secondSession = login(client, email, "strong-password");

		var response = client.put()
				.uri("/api/v1/accounts/me/password")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + firstSession.accessToken())
				.body(Map.of(
						"currentPassword", "strong-password",
						"newPassword", "another-strong-password"))
				.retrieve()
				.toBodilessEntity();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		assertFailure(() -> refresh(client, firstSession.refreshToken()),
				HttpStatus.UNAUTHORIZED, "authentication_failed");
		assertFailure(() -> refresh(client, secondSession.refreshToken()),
				HttpStatus.UNAUTHORIZED, "authentication_failed");
		assertFailure(() -> login(client, email, "strong-password"),
				HttpStatus.UNAUTHORIZED, "authentication_failed");
		assertThat(login(client, email, "another-strong-password").accessToken()).isNotBlank();
	}

	@Test
	void requiresJwtAndCorrectCurrentPassword() {
		var client = client();
		var email = register(client);
		var session = login(client, email, "strong-password");
		var path = "/api/v1/accounts/me/password";

		assertFailure(() -> client.put().uri(path)
				.body(Map.of("currentPassword", "strong-password", "newPassword", "another-strong-password"))
				.retrieve().toBodilessEntity(),
				HttpStatus.UNAUTHORIZED, null);
		assertFailure(() -> client.put().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("currentPassword", "wrong-password", "newPassword", "another-strong-password"))
				.retrieve().toBodilessEntity(),
				HttpStatus.BAD_REQUEST, "invalid_current_password");
		assertFailure(() -> client.put().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("currentPassword", "strong-password", "newPassword", "strong-password"))
				.retrieve().toBodilessEntity(),
				HttpStatus.BAD_REQUEST, "password_unchanged");
		assertFailure(() -> client.put().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("currentPassword", "strong-password", "newPassword", "short"))
				.retrieve().toBodilessEntity(),
				HttpStatus.BAD_REQUEST, "validation_failed");
		assertFailure(() -> client.put().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("currentPassword", "strong-password", "newPassword", "é".repeat(37)))
				.retrieve().toBodilessEntity(),
				HttpStatus.BAD_REQUEST, "invalid_password");
		assertFailure(() -> client.put().uri(path)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
				.body(Map.of("currentPassword", "strong-password" + "x".repeat(60),
						"newPassword", "another-strong-password"))
				.retrieve().toBodilessEntity(),
				HttpStatus.BAD_REQUEST, "invalid_password");
		assertThat(refresh(client, session.refreshToken()).accessToken()).isNotBlank();
		assertThat(login(client, email, "strong-password").accessToken()).isNotBlank();
	}

	private RestClient client() {
		return RestClient.create("http://localhost:" + port);
	}

	private String usernameOf(String email) {
		return jdbcClient.sql("select username from accounts where email = :email")
				.param("email", email)
				.query(String.class)
				.single();
	}

	private static String register(RestClient client) {
		var suffix = UUID.randomUUID();
		var email = "change-" + suffix + "@example.com";
		var response = client.post().uri("/api/v1/auth/register")
				.body(Map.of("email", email, "username", "change-" + suffix, "password", "strong-password"))
				.retrieve().toBodilessEntity();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return email;
	}

	private static TokenResponse login(RestClient client, String email, String password) {
		return client.post().uri("/api/v1/auth/login")
				.body(Map.of("email", email, "password", password))
				.retrieve().body(TokenResponse.class);
	}

	private static TokenResponse refresh(RestClient client, String refreshToken) {
		return client.post().uri("/api/v1/auth/refresh")
				.body(Map.of("refreshToken", refreshToken))
				.retrieve().body(TokenResponse.class);
	}

	private static void assertFailure(Runnable request, HttpStatus expectedStatus, String expectedCode) {
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
