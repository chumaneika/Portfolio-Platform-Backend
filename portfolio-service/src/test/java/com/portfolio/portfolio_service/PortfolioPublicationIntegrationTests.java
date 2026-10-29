package com.portfolio.portfolio_service;

import com.jayway.jsonpath.JsonPath;
import com.portfolio.portfolio_service.client.ProfileClient;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false",
		"spring.datasource.url=jdbc:h2:mem:portfolio_publication_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class PortfolioPublicationIntegrationTests {

	@TestConfiguration
	static class ProfileClientTestConfig {
		@Bean
		@Primary
		RecordingProfileClient recordingProfileClient() {
			return new RecordingProfileClient();
		}
	}

	static class RecordingProfileClient implements ProfileClient {
		UUID lastAccountId;
		boolean missing;

		@Override
		public Optional<Map<String, Object>> getPublicProfile(UUID accountId) {
			lastAccountId = accountId;
			if (missing) {
				return Optional.empty();
			}
			return Optional.of(Map.of("profile", Map.of("firstName", "Test")));
		}
	}

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("springSecurityFilterChain")
	private Filter securityFilterChain;

	@Autowired
	private SecretKey jwtSecretKey;

	@Autowired
	private RecordingProfileClient profileClient;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		profileClient.lastAccountId = null;
		profileClient.missing = false;
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.addFilters(securityFilterChain)
				.build();
	}

	@Test
	void publishesFullPortfolioAndHidesItWhenUnpublished() throws Exception {
		var owner = UUID.randomUUID();
		var slug = "work-" + UUID.randomUUID();
		var privatePath = createPortfolio(owner, slug);
		var publicPath = "/api/v1/portfolios/" + slug;
		var projectsPath = privatePath + "/projects";
		var project = mockMvc.perform(authenticated(post(projectsPath), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"sample\",\"title\":\"Sample\",\"featured\":true}"))
				.andExpect(status().isCreated())
				.andReturn();
		var projectId = JsonPath.<String>read(project.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(authenticated(post(projectsPath + "/" + projectId + "/technologies"), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"technologyCode\":\"Java\",\"displayName\":\"Java\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get(publicPath))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("portfolio_not_found"));
		mockMvc.perform(authenticated(post(privatePath + "/publish"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PUBLISHED"))
				.andExpect(jsonPath("$.publishedAt").exists());
		mockMvc.perform(get("/api/v1/portfolios/" + slug.toUpperCase()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value(slug))
				.andExpect(jsonPath("$.publishedAt").exists())
				.andExpect(jsonPath("$.profile.profile.firstName").value("Test"))
				.andExpect(jsonPath("$.projects[0].id").value(projectId))
				.andExpect(jsonPath("$.projects[0].technologies[0].technologyCode").value("java"));
		assertThat(profileClient.lastAccountId).isEqualTo(owner);

		mockMvc.perform(authenticated(post(privatePath + "/draft"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.publishedAt").doesNotExist());
		mockMvc.perform(get(publicPath)).andExpect(status().isNotFound());
		mockMvc.perform(authenticated(post(privatePath + "/archive"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ARCHIVED"));
		mockMvc.perform(get(publicPath)).andExpect(status().isNotFound());
		mockMvc.perform(authenticated(post(privatePath + "/draft"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DRAFT"));
		mockMvc.perform(authenticated(post(privatePath + "/publish"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PUBLISHED"));
		mockMvc.perform(authenticated(post(privatePath + "/archive"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ARCHIVED"))
				.andExpect(jsonPath("$.publishedAt").doesNotExist());
		mockMvc.perform(get(publicPath)).andExpect(status().isNotFound());
	}

	@Test
	void returnsOnlyPublishedPortfoliosForAccount() throws Exception {
		var owner = UUID.randomUUID();
		var other = UUID.randomUUID();
		var publishedSlug = "published-" + UUID.randomUUID();
		var publishedPath = createPortfolio(owner, publishedSlug);
		createPortfolio(owner, "draft-" + UUID.randomUUID());
		var otherPath = createPortfolio(other, "other-" + UUID.randomUUID());
		mockMvc.perform(authenticated(post(publishedPath + "/publish"), owner))
				.andExpect(status().isOk());
		mockMvc.perform(authenticated(post(otherPath + "/publish"), other))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/portfolios/by-account/{accountId}", owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].slug").value(publishedSlug));
		mockMvc.perform(get("/api/v1/portfolios/by-account/{accountId}", UUID.randomUUID()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		profileClient.missing = true;
		mockMvc.perform(get("/api/v1/portfolios/by-account/{accountId}", owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].profile").doesNotExist());
	}

	@Test
	void rejectsInvalidTransitionsAndProtectsOwnerRoutes() throws Exception {
		var owner = UUID.randomUUID();
		var other = UUID.randomUUID();
		var privatePath = createPortfolio(owner, "work-" + UUID.randomUUID());

		mockMvc.perform(post(privatePath + "/publish"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/portfolios/me"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(authenticated(post(privatePath + "/publish"), other))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("portfolio_not_found"));
		mockMvc.perform(authenticated(post(privatePath + "/draft"), owner))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("invalid_portfolio_status_transition"));
		mockMvc.perform(authenticated(post(privatePath + "/publish"), owner))
				.andExpect(status().isOk());
		mockMvc.perform(authenticated(post(privatePath + "/publish"), owner))
				.andExpect(status().isConflict());
		mockMvc.perform(authenticated(post(privatePath + "/archive"), owner))
				.andExpect(status().isOk());
		mockMvc.perform(authenticated(post(privatePath + "/publish"), owner))
				.andExpect(status().isConflict());
		mockMvc.perform(authenticated(post(privatePath + "/archive"), owner))
				.andExpect(status().isConflict());
	}

	@Test
	void rejectsReservedPublicSlug() throws Exception {
		var owner = UUID.randomUUID();
		mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"ME\",\"title\":\"Portfolio\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_portfolio_slug"));
	}

	private String createPortfolio(UUID accountId, String slug) throws Exception {
		var created = mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"" + slug + "\",\"title\":\"Portfolio\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		return created.getResponse().getHeader("Location");
	}

	private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, UUID accountId) {
		var now = Instant.now();
		var claims = JwtClaimsSet.builder()
				.issuer("http://identity-service:8081")
				.subject(accountId.toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900))
				.build();
		var encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
		var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return request.header("Authorization", "Bearer " + token);
	}
}
