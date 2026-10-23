package com.portfolio.profile_service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.portfolio.profile_service.controllers.ApiExceptionHandler;
import com.portfolio.profile_service.exceptions.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import java.time.Instant;

@SpringBootTest(properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false",
		"spring.datasource.url=jdbc:h2:mem:profile_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class ProfileServiceApplicationTests {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private ApiExceptionHandler apiExceptionHandler;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private SecretKey jwtSecretKey;

	@Test
	void contextLoads() {
	}

	@Test
	@DisplayName("Flyway creates the profile schema")
	void migrationsCreateProfileSchema() {
		var tables = jdbcClient.sql("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = 'public'
					AND table_name IN (
						'profiles',
						'work_experiences',
						'educations',
						'profile_skills',
						'profile_links'
					)
				ORDER BY table_name
				""")
				.query(String.class)
				.list();

		assertThat(tables).containsExactly(
				"educations",
				"profile_links",
				"profile_skills",
				"profiles",
				"work_experiences");
	}

	@Test
	@DisplayName("Application errors use the standard API error contract")
	void apiErrorContract() {
		var request = new MockHttpServletRequest("GET", "/api/v1/profiles/missing");
		var response = apiExceptionHandler.handleApiException(
				new ApiException(HttpStatus.NOT_FOUND, "profile_not_found", "Profile not found"),
				request);

		assertThat(response.getStatusCode().value()).isEqualTo(404);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("profile_not_found");
		assertThat(response.getBody().path()).isEqualTo("/api/v1/profiles/missing");
	}

	@Test
	@DisplayName("JWT issued by identity-service can be validated")
	void validatesIdentityServiceJwt() {
		var now = Instant.now();
		var claims = JwtClaimsSet.builder()
				.issuer("http://identity-service:8081")
				.subject("2ac467db-e0cc-46e7-b290-18776093bc11")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900))
				.claim("email", "user@example.com")
				.build();
		var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		var encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
		var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		var decoded = jwtDecoder.decode(token);

		assertThat(decoded.getSubject()).isEqualTo("2ac467db-e0cc-46e7-b290-18776093bc11");
		assertThat(decoded.getClaimAsString("email")).isEqualTo("user@example.com");
	}

}
