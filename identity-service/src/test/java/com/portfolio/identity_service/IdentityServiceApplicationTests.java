package com.portfolio.identity_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portfolio.identity_service.controllers.ApiExceptionHandler;
import com.portfolio.identity_service.dto.request.LoginRequest;
import com.portfolio.identity_service.dto.request.RegisterRequest;
import com.portfolio.identity_service.entities.AccountStatus;
import com.portfolio.identity_service.exceptions.ApiException;
import com.portfolio.identity_service.repositories.AccountRepository;
import com.portfolio.identity_service.security.AccountUserDetails;
import com.portfolio.identity_service.services.AuthService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false",
		"spring.datasource.url=jdbc:h2:mem:identity_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ=",
		"security.jwt.issuer=identity-service-test",
		"security.jwt.access-token-ttl=15m",
		"security.jwt.refresh-token-ttl=30d"
})
class IdentityServiceApplicationTests {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private AuthService authService;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Validator validator;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private ApiExceptionHandler apiExceptionHandler;

	@Autowired
	@Qualifier("restClientBuilder")
	private RestClient.Builder restClientBuilder;

	@Autowired
	@Qualifier("webClientBuilder")
	private WebClient.Builder webClientBuilder;

	@Test
	void contextLoads() {
		assertThat(restClientBuilder).isNotNull();
		assertThat(webClientBuilder).isNotNull();
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
	@DisplayName("Registration hashes the password and AuthenticationManager checks credentials")
	void passwordAuthenticationFlow() {
		authService.register(new RegisterRequest(
				"User@Example.com",
				"TestUser",
				"strong-password"));

		var userDetails = userDetailsService.loadUserByUsername("USER@EXAMPLE.COM");
		var account = accountRepository.findByEmail("user@example.com").orElseThrow();
		assertThat(account.getUsername()).isEqualTo("testuser");
		assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.isEmailVerified()).isFalse();
		assertThat(account.getPasswordHash()).isNotEqualTo("strong-password");
		assertThat(passwordEncoder.matches("strong-password", account.getPasswordHash())).isTrue();
		assertThat(userDetails).isInstanceOf(AccountUserDetails.class);
		assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
		assertThat(((AccountUserDetails) userDetails).getEmail()).isEqualTo("user@example.com");
		assertThat(authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken("USER@EXAMPLE.COM", "strong-password"))
				.isAuthenticated()).isTrue();
		assertThatThrownBy(() -> authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken("USER@EXAMPLE.COM", "wrong-password")))
				.isInstanceOf(AuthenticationException.class);
		assertThatThrownBy(() -> authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken("testuser", "strong-password")))
				.isInstanceOf(AuthenticationException.class);

		var tokenResponse = authService.login(
				new LoginRequest("USER@EXAMPLE.COM", "strong-password"));
		var jwt = jwtDecoder.decode(tokenResponse.accessToken());
		assertThat(tokenResponse.refreshToken()).isNotBlank();
		assertThat(tokenResponse.tokenType()).isEqualTo("Bearer");
		assertThat(tokenResponse.expiresIn()).isEqualTo(900);
		assertThat(jwt.getSubject()).isEqualTo(((AccountUserDetails) userDetails).getAccountId().toString());
		assertThat(jwt.getClaimAsString("email")).isEqualTo("user@example.com");
		assertThatThrownBy(() -> authService.login(
				new LoginRequest("USER@EXAMPLE.COM", "wrong-password")))
				.isInstanceOf(ApiException.class);
	}

	@Test
	@Transactional
	@DisplayName("Refresh token is one-time and rotates on use")
	void refreshTokenFlow() {
		authService.register(new RegisterRequest(
				"refresh@example.com",
				"refresh-user",
				"strong-password"));
		var loginResponse = authService.login(
				new LoginRequest("refresh@example.com", "strong-password"));

		var refreshResponse = authService.refresh(loginResponse.refreshToken());

		assertThat(refreshResponse.accessToken()).isNotBlank();
		assertThat(refreshResponse.accessToken()).isNotEqualTo(loginResponse.accessToken());
		assertThat(refreshResponse.refreshToken())
				.isNotBlank()
				.isNotEqualTo(loginResponse.refreshToken());
		assertThatThrownBy(() -> authService.refresh(loginResponse.refreshToken()))
				.isInstanceOfSatisfying(ApiException.class, exception ->
						assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	@Transactional
	@DisplayName("Logout revokes the refresh token")
	void logoutFlow() {
		authService.register(new RegisterRequest(
				"logout@example.com",
				"logout-user",
				"strong-password"));
		var loginResponse = authService.login(
				new LoginRequest("logout@example.com", "strong-password"));

		authService.logout(loginResponse.refreshToken());
		authService.logout(loginResponse.refreshToken());

		assertThatThrownBy(() -> authService.refresh(loginResponse.refreshToken()))
				.isInstanceOfSatisfying(ApiException.class, exception ->
						assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
	}

	@Test
	@Transactional
	@DisplayName("Registration rejects an occupied email or username")
	void registrationRejectsDuplicates() {
		authService.register(new RegisterRequest(
				"existing@example.com",
				"existing-user",
				"strong-password"));

		assertThatThrownBy(() -> authService.register(new RegisterRequest(
				"EXISTING@example.com",
				"another-user",
				"strong-password")))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(exception.getCode()).isEqualTo("account_conflict");
				});
		assertThatThrownBy(() -> authService.register(new RegisterRequest(
				"another@example.com",
				"EXISTING-USER",
				"strong-password")))
				.isInstanceOfSatisfying(ApiException.class, exception ->
						assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	@DisplayName("Registration request validates email, username and password")
	void registrationRequestValidation() {
		var request = new RegisterRequest("not-an-email", "bad username", "short");

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsExactlyInAnyOrder("email", "username", "password");
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
