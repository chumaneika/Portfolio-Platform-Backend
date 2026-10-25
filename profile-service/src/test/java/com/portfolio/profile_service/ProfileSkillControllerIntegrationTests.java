package com.portfolio.profile_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileSkillControllerIntegrationTests extends ControllerIntegrationTestSupport {

	@Test
	@DisplayName("Profile skill controller supports normalized CRUD flow")
	void skillLifecycle() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);

		mockMvc.perform(authenticated(post("/api/v1/profile/me/skills"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "skillCode": " Java ",
						  "displayName": "Java",
						  "category": "Backend"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/profile/me/skills/java"))
				.andExpect(jsonPath("$.skillCode").value("java"))
				.andExpect(jsonPath("$.displayOrder").value(0));

		mockMvc.perform(authenticated(get("/api/v1/profile/me/skills"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].skillCode").value("java"));

		mockMvc.perform(authenticated(put("/api/v1/profile/me/skills/JAVA"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "displayName": "Java 21",
						  "category": "Languages"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Java 21"));

		mockMvc.perform(authenticated(delete("/api/v1/profile/me/skills/JAVA"), accountId))
				.andExpect(status().isNoContent());

		mockMvc.perform(authenticated(get("/api/v1/profile/me/skills"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Profile skill controller rejects duplicate normalized codes")
	void rejectsDuplicateSkill() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);
		var request = """
				{
				  "skillCode": "%s",
				  "displayName": "Java",
				  "category": "Backend"
				}
				""";

		mockMvc.perform(authenticated(post("/api/v1/profile/me/skills"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request.formatted("java")))
				.andExpect(status().isCreated());

		mockMvc.perform(authenticated(post("/api/v1/profile/me/skills"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request.formatted(" JAVA ")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("profile_skill_already_exists"));
	}
}
