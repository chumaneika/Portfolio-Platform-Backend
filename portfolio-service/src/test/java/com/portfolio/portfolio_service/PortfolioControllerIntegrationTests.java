package com.portfolio.portfolio_service;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false",
		"spring.datasource.url=jdbc:h2:mem:portfolio_controller_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class PortfolioControllerIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("springSecurityFilterChain")
	private Filter securityFilterChain;

	@Autowired
	private SecretKey jwtSecretKey;

	@Autowired
	private JdbcClient jdbcClient;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.addFilters(securityFilterChain)
				.build();
	}

	@Test
	@DisplayName("Owner can create, list, read, update and delete portfolios")
	void ownerCrud() throws Exception {
		var accountId = UUID.randomUUID();
		var slug = "work-" + UUID.randomUUID();
		var created = mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(slug.toUpperCase(), "First portfolio", "My work")))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.slug").value(slug))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.publishedAt").doesNotExist())
				.andReturn();
		var id = JsonPath.<String>read(created.getResponse().getContentAsString(), "$.id");
		var location = "/api/v1/portfolios/me/" + id;
		assertThat(created.getResponse().getHeader("Location"))
				.isEqualTo(location);

		mockMvc.perform(authenticated(get("/api/v1/portfolios/me"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].slug", hasItem(slug)));

		mockMvc.perform(authenticated(get(location), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("First portfolio"));

		var changedSlug = "updated-" + UUID.randomUUID();
		mockMvc.perform(authenticated(put(location), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(changedSlug.toUpperCase(), "Updated portfolio", "Updated description")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value(changedSlug))
				.andExpect(jsonPath("$.title").value("Updated portfolio"))
				.andExpect(jsonPath("$.description").value("Updated description"));

		mockMvc.perform(authenticated(delete(location), accountId))
				.andExpect(status().isNoContent());
		mockMvc.perform(authenticated(get(location), accountId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("portfolio_not_found"));
	}

	@Test
	@DisplayName("Portfolio operations require authentication and ownership")
	void authenticationAndOwnership() throws Exception {
		mockMvc.perform(get("/api/v1/portfolios/me"))
				.andExpect(status().isUnauthorized());

		var owner = UUID.randomUUID();
		var other = UUID.randomUUID();
		var created = mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("work-" + UUID.randomUUID(), "Private", null)))
				.andExpect(status().isCreated())
				.andReturn();
		var location = created.getResponse().getHeader("Location");

		mockMvc.perform(authenticated(get(location), other))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("portfolio_not_found"));
		mockMvc.perform(authenticated(put(location), other)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("other-" + UUID.randomUUID(), "Changed", null)))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticated(delete(location), other))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticated(get(location), owner))
				.andExpect(status().isOk());
		mockMvc.perform(authenticated(get("/api/v1/portfolios/me"), other))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Slug is globally unique and request fields are validated")
	void slugAndValidation() throws Exception {
		var first = UUID.randomUUID();
		var second = UUID.randomUUID();
		var slug = "shared-" + UUID.randomUUID();
		mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), first)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(slug, "First", null)))
				.andExpect(status().isCreated());

		mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), second)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(slug.toUpperCase(), "Second", null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("portfolio_slug_already_exists"));

		var otherPortfolio = mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), second)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("unique-" + UUID.randomUUID(), "Second", null)))
				.andExpect(status().isCreated())
				.andReturn();
		mockMvc.perform(authenticated(put(otherPortfolio.getResponse().getHeader("Location")), second)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(slug, "Second", null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("portfolio_slug_already_exists"));

		mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), second)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("invalid/slug", "", null)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_failed"))
				.andExpect(jsonPath("$.fieldErrors.slug").exists())
				.andExpect(jsonPath("$.fieldErrors.title").exists());
	}

	@Test
	@DisplayName("Deleting a portfolio also removes its projects and technologies")
	void deletionCascadesToProjects() throws Exception {
		var accountId = UUID.randomUUID();
		var created = mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("work-" + UUID.randomUUID(), "Work", null)))
				.andExpect(status().isCreated())
				.andReturn();
		var portfolioId = UUID.fromString(JsonPath.read(created.getResponse().getContentAsString(), "$.id"));
		var projectId = UUID.randomUUID();
		jdbcClient.sql("""
				insert into projects (id, portfolio_id, slug, title)
				values (:id, :portfolioId, 'project', 'Project')
				""")
				.param("id", projectId)
				.param("portfolioId", portfolioId)
				.update();
		jdbcClient.sql("""
				insert into project_technologies (project_id, technology_code, display_name)
				values (:projectId, 'java', 'Java')
				""")
				.param("projectId", projectId)
				.update();

		mockMvc.perform(authenticated(delete("/api/v1/portfolios/me/{portfolioId}", portfolioId), accountId))
				.andExpect(status().isNoContent());

		var projectCount = jdbcClient.sql("select count(*) from projects where id = :id")
				.param("id", projectId)
				.query(Long.class)
				.single();
		var technologyCount = jdbcClient.sql("select count(*) from project_technologies where project_id = :id")
				.param("id", projectId)
				.query(Long.class)
				.single();
		assertThat(projectCount).isZero();
		assertThat(technologyCount).isZero();
	}

	private MockHttpServletRequestBuilder authenticated(
			MockHttpServletRequestBuilder request,
			UUID accountId) {
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

	private static String request(String slug, String title, String description) {
		var jsonDescription = description == null ? "null" : "\"" + description + "\"";
		return """
				{
				  "slug": "%s",
				  "title": "%s",
				  "description": %s
				}
				""".formatted(slug, title, jsonDescription);
	}
}
