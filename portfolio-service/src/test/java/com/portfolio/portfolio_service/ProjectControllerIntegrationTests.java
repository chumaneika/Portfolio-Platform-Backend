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
		"spring.datasource.url=jdbc:h2:mem:project_controller_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class ProjectControllerIntegrationTests {

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
	@DisplayName("Owner creates, lists and updates projects; deletion compacts order and removes technologies")
	void projectLifecycle() throws Exception {
		var accountId = UUID.randomUUID();
		var portfolioId = createPortfolio(accountId);
		var collection = "/api/v1/portfolios/me/" + portfolioId + "/projects";

		var first = mockMvc.perform(authenticated(post(collection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(" Java-App ", "First", null, null, false)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.slug").value("java-app"))
				.andExpect(jsonPath("$.displayOrder").value(0))
				.andReturn();
		var firstId = UUID.fromString(JsonPath.read(first.getResponse().getContentAsString(), "$.id"));
		assertThat(first.getResponse().getHeader("Location"))
				.isEqualTo(collection + "/" + firstId);

		var second = mockMvc.perform(authenticated(post(collection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("second", "Second", null, null, true)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.displayOrder").value(1))
				.andReturn();
		var secondLocation = second.getResponse().getHeader("Location");
		var secondId = UUID.fromString(JsonPath.read(second.getResponse().getContentAsString(), "$.id"));

		mockMvc.perform(authenticated(get(collection), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(firstId.toString()))
				.andExpect(jsonPath("$[1].id").value(secondId.toString()));

		mockMvc.perform(authenticated(put(secondLocation), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("renamed", "Updated", "2024-01-01", "2025-01-01", false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slug").value("renamed"))
				.andExpect(jsonPath("$.title").value("Updated"))
				.andExpect(jsonPath("$.completedOn").value("2025-01-01"))
				.andExpect(jsonPath("$.featured").value(false))
				.andExpect(jsonPath("$.displayOrder").value(1));

		jdbcClient.sql("""
				insert into project_technologies (project_id, technology_code, display_name)
				values (:projectId, 'java', 'Java')
				""")
				.param("projectId", firstId)
				.update();
		mockMvc.perform(authenticated(delete(collection + "/" + firstId), accountId))
				.andExpect(status().isNoContent());

		mockMvc.perform(authenticated(get(collection), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(secondId.toString()))
				.andExpect(jsonPath("$[0].displayOrder").value(0));
		var technologyCount = jdbcClient.sql("select count(*) from project_technologies where project_id = :projectId")
				.param("projectId", firstId)
				.query(Long.class)
				.single();
		assertThat(technologyCount).isZero();
	}

	@Test
	@DisplayName("Project endpoints enforce JWT, portfolio ownership and portfolio/project pairing")
	void ownershipAndAuthentication() throws Exception {
		var owner = UUID.randomUUID();
		var other = UUID.randomUUID();
		var portfolioId = createPortfolio(owner);
		var otherPortfolioId = createPortfolio(owner);
		var collection = "/api/v1/portfolios/me/" + portfolioId + "/projects";
		var created = mockMvc.perform(authenticated(post(collection), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("private", "Private", null, null, false)))
				.andExpect(status().isCreated())
				.andReturn();
		var projectId = JsonPath.<String>read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(collection)).andExpect(status().isUnauthorized());
		mockMvc.perform(authenticated(get(collection), other))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("portfolio_not_found"));
		mockMvc.perform(authenticated(post(collection), other)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("intrusion", "No", null, null, false)))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticated(put(collection + "/" + projectId), other)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("intrusion", "No", null, null, false)))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticated(delete(collection + "/" + projectId), other))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticated(put("/api/v1/portfolios/me/{portfolioId}/projects/{projectId}",
				otherPortfolioId, projectId), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("wrong-parent", "No", null, null, false)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("project_not_found"));
		mockMvc.perform(authenticated(get(collection), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("Project slugs are unique within a portfolio but reusable in another")
	void slugScopeAndValidation() throws Exception {
		var accountId = UUID.randomUUID();
		var firstPortfolioId = createPortfolio(accountId);
		var secondPortfolioId = createPortfolio(accountId);
		var firstCollection = "/api/v1/portfolios/me/" + firstPortfolioId + "/projects";
		var secondCollection = "/api/v1/portfolios/me/" + secondPortfolioId + "/projects";

		mockMvc.perform(authenticated(post(firstCollection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("Java", "First", null, null, false)))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticated(post(firstCollection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(" JAVA ", "Duplicate", null, null, false)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("project_slug_already_exists"));
		mockMvc.perform(authenticated(post(secondCollection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("java", "Allowed", null, null, false)))
				.andExpect(status().isCreated());

		var another = mockMvc.perform(authenticated(post(firstCollection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("another", "Another", null, null, false)))
				.andExpect(status().isCreated())
				.andReturn();
		mockMvc.perform(authenticated(put(another.getResponse().getHeader("Location")), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("JAVA", "Collision", null, null, false)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("project_slug_already_exists"));

		mockMvc.perform(authenticated(post(firstCollection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("invalid/slug", "", null, null, false)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.slug").exists())
				.andExpect(jsonPath("$.fieldErrors.title").exists());
		mockMvc.perform(authenticated(post(firstCollection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("bad-dates", "Bad dates", "2025-01-01", "2024-01-01", false)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_project_timeline"));
	}

	private UUID createPortfolio(UUID accountId) throws Exception {
		var created = mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"slug":"portfolio-%s","title":"Portfolio"}
						""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(created.getResponse().getContentAsString(), "$.id"));
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

	private static String request(
			String slug,
			String title,
			String startedOn,
			String completedOn,
			boolean featured) {
		var start = startedOn == null ? "null" : "\"" + startedOn + "\"";
		var completion = completedOn == null ? "null" : "\"" + completedOn + "\"";
		return """
				{
				  "slug": "%s",
				  "title": "%s",
				  "summary": "Summary",
				  "description": "Description",
				  "sourceCodeUrl": "https://github.com/example/project",
				  "liveUrl": "https://example.com",
				  "coverImageUrl": "https://example.com/cover.png",
				  "startedOn": %s,
				  "completedOn": %s,
				  "featured": %s
				}
				""".formatted(slug, title, start, completion, featured);
	}
}
