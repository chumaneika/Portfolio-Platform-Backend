package com.portfolio.portfolio_service;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.UUID;

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
		"spring.datasource.url=jdbc:h2:mem:project_technology_controller_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=validate",
		"security.jwt.secret=cG9ydGZvbGlvLWlkZW50aXR5LXNlcnZpY2UtdGVzdC1zZWNyZXQ="
})
class ProjectTechnologyControllerIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("springSecurityFilterChain")
	private Filter securityFilterChain;

	@Autowired
	private SecretKey jwtSecretKey;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.addFilters(securityFilterChain)
				.build();
	}

	@Test
	void createsUpdatesListsAndDeletesTechnologies() throws Exception {
		var owner = UUID.randomUUID();
		var path = createProjectPath(owner);
		var collection = path + "/technologies";

		mockMvc.perform(authenticated(post(collection), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest(" Java ", " Java ")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", collection + "/java"))
				.andExpect(jsonPath("$.technologyCode").value("java"))
				.andExpect(jsonPath("$.displayName").value("Java"))
				.andExpect(jsonPath("$.displayOrder").value(0));
		mockMvc.perform(authenticated(post(collection), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("spring", "Spring")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.displayOrder").value(1));

		mockMvc.perform(authenticated(put(collection + "/JAVA"), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Java 21\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.technologyCode").value("java"))
				.andExpect(jsonPath("$.displayName").value("Java 21"))
				.andExpect(jsonPath("$.displayOrder").value(0));
		mockMvc.perform(authenticated(get(collection), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].technologyCode").value("java"))
				.andExpect(jsonPath("$[1].technologyCode").value("spring"));

		mockMvc.perform(authenticated(delete(collection + "/JAVA"), owner))
				.andExpect(status().isNoContent());
		mockMvc.perform(authenticated(get(collection), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].technologyCode").value("spring"))
				.andExpect(jsonPath("$[0].displayOrder").value(0));
	}

	@Test
	void rejectsDuplicatesAndInvalidRequests() throws Exception {
		var owner = UUID.randomUUID();
		var collection = createProjectPath(owner) + "/technologies";
		mockMvc.perform(authenticated(post(collection), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("Java", "Java")))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticated(post(collection), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest(" JAVA ", "Duplicate")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("project_technology_already_exists"));
		mockMvc.perform(authenticated(post(collection), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest(" ", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.technologyCode").exists())
				.andExpect(jsonPath("$.fieldErrors.displayName").exists());
		mockMvc.perform(authenticated(put(collection + "/missing"), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Missing\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("project_technology_not_found"));
	}

	@Test
	void checksOwnershipAndProjectPairing() throws Exception {
		var owner = UUID.randomUUID();
		var other = UUID.randomUUID();
		var projectPath = createProjectPath(owner);
		var collection = projectPath + "/technologies";
		var otherProjectPath = createProjectPath(owner);
		mockMvc.perform(authenticated(post(collection), owner)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("java", "Java")))
				.andExpect(status().isCreated());

		mockMvc.perform(get(collection)).andExpect(status().isUnauthorized());
		mockMvc.perform(authenticated(get(collection), other))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("portfolio_not_found"));
		mockMvc.perform(authenticated(post(collection), other)
				.contentType(MediaType.APPLICATION_JSON)
				.content(createRequest("intrusion", "No")))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticated(put(collection + "/java"), other)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"No\"}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticated(delete(collection + "/java"), other))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticated(get(otherProjectPath + "/technologies"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		var wrongPortfolio = otherProjectPath.substring(0, otherProjectPath.indexOf("/projects/"));
		var wrongPair = wrongPortfolio + "/projects/" + projectPath.substring(projectPath.lastIndexOf('/') + 1)
				+ "/technologies";
		mockMvc.perform(authenticated(get(wrongPair), owner))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("project_not_found"));
	}

	private String createProjectPath(UUID accountId) throws Exception {
		var portfolio = mockMvc.perform(authenticated(post("/api/v1/portfolios/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"portfolio-" + UUID.randomUUID() + "\",\"title\":\"Portfolio\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		var portfolioId = JsonPath.<String>read(portfolio.getResponse().getContentAsString(), "$.id");
		var collection = "/api/v1/portfolios/me/" + portfolioId + "/projects";
		var project = mockMvc.perform(authenticated(post(collection), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"slug\":\"project\",\"title\":\"Project\",\"featured\":false}"))
				.andExpect(status().isCreated())
				.andReturn();
		var projectId = JsonPath.<String>read(project.getResponse().getContentAsString(), "$.id");
		return collection + "/" + projectId;
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

	private static String createRequest(String technologyCode, String displayName) {
		return "{\"technologyCode\":\"" + technologyCode + "\",\"displayName\":\"" + displayName + "\"}";
	}
}
