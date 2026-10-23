package com.portfolio.identity_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.identity_service.controllers.ApiExceptionHandler;
import com.portfolio.identity_service.dto.request.LoginRequest;
import com.portfolio.identity_service.dto.request.RegisterRequest;
import com.portfolio.identity_service.exceptions.ApiException;
import com.portfolio.identity_service.services.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false",
		"spring.datasource.url=jdbc:h2:mem:identity_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class IdentityServiceApplicationTests {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private ApiExceptionHandler apiExceptionHandler;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("Flyway creates the identity schema")
	void migrationsCreateIdentitySchema() {
		var tables = jdbcClient.sql("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = 'public'
					AND table_name IN ('accounts', 'refresh_tokens')
				ORDER BY table_name
				""")
				.query(String.class)
				.list();

		assertThat(tables).containsExactly("accounts", "refresh_tokens");
	}

	@Test
	@Transactional
	@DisplayName("Password login issues JWT and refresh tokens are rotated and revocable")
	void passwordAuthenticationFlow() {
		var registration = authService.register(
				new RegisterRequest("User@Example.com", "TestUser", "strong-password"),
				"test-agent",
				"127.0.0.1");
		var registrationJwt = jwtDecoder.decode(registration.accessToken());

		assertThat(registration.tokenType()).isEqualTo("Bearer");
		assertThat(registration.expiresIn()).isEqualTo(900);
		assertThat(registrationJwt.getClaimAsString("email")).isEqualTo("user@example.com");
		assertThat(registrationJwt.getClaimAsString("username")).isEqualTo("testuser");
		assertThat(registrationJwt.getIssuer().toString()).isEqualTo("http://identity-service:8081");

		var login = authService.login(
				new LoginRequest("USER@EXAMPLE.COM", "strong-password"),
				"test-agent",
				"127.0.0.1");
		assertThat(jwtDecoder.decode(login.accessToken()).getSubject()).isEqualTo(registrationJwt.getSubject());

		var rotated = authService.refresh(login.refreshToken(), "test-agent", "127.0.0.1");
		assertThat(rotated.refreshToken()).isNotEqualTo(login.refreshToken());
		assertThatThrownBy(() -> authService.refresh(login.refreshToken(), "test-agent", "127.0.0.1"))
				.isInstanceOf(ApiException.class);

		authService.logout(rotated.refreshToken());
		assertThatThrownBy(() -> authService.refresh(rotated.refreshToken(), "test-agent", "127.0.0.1"))
				.isInstanceOf(ApiException.class);

		var storedHashes = jdbcClient.sql("SELECT token_hash FROM refresh_tokens")
				.query(String.class)
				.list();
		assertThat(storedHashes)
				.doesNotContain(registration.refreshToken(), login.refreshToken(), rotated.refreshToken())
				.allMatch(hash -> hash.matches("[0-9a-f]{64}"));
	}

	@Test
	@DisplayName("Expected application errors use the standard API error contract")
	void expectedErrorContract() {
		var request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
		var response = apiExceptionHandler.handleApiException(
				new ApiException(
						HttpStatus.CONFLICT,
						"account_conflict",
						"An account with this email already exists"),
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(409);
		assertThat(response.getBody().error()).isEqualTo("Conflict");
		assertThat(response.getBody().code()).isEqualTo("account_conflict");
		assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/register");
		assertThat(response.getBody().fieldErrors()).isEmpty();
	}

}
