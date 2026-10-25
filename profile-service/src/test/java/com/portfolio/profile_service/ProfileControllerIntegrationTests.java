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

class ProfileControllerIntegrationTests extends ControllerIntegrationTestSupport {

	@Test
	@DisplayName("Profile controller supports authenticated create, read and update flow")
	void profileLifecycle() throws Exception {
		var accountId = UUID.randomUUID();

		mockMvc.perform(authenticated(post("/api/v1/profile/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "firstName": " Malik ",
						  "lastName": "Testov"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/profile/me"));

		mockMvc.perform(authenticated(get("/api/v1/profile/me"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Malik"))
				.andExpect(jsonPath("$.lastName").value("Testov"))
				.andExpect(jsonPath("$.availableForWork").value(false));

		mockMvc.perform(authenticated(put("/api/v1/profile/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "firstName": "Malik",
						  "lastName": "Testov",
						  "headline": "Backend Developer",
						  "bio": "Java developer",
						  "location": "Moscow",
						  "avatarUrl": "https://example.com/avatar.png",
						  "resumeUrl": "https://example.com/resume.pdf",
						  "availableForWork": true
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headline").value("Backend Developer"))
				.andExpect(jsonPath("$.availableForWork").value(true));
	}

	@Test
	@DisplayName("Profile controller enforces authentication, validation and duplicate protection")
	void profileErrors() throws Exception {
		mockMvc.perform(get("/api/v1/profile/me"))
				.andExpect(status().isUnauthorized());

		var accountId = UUID.randomUUID();
		mockMvc.perform(authenticated(post("/api/v1/profile/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"", "lastName":"User"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_failed"))
				.andExpect(jsonPath("$.fieldErrors.firstName").exists());

		createProfile(accountId);
		mockMvc.perform(authenticated(post("/api/v1/profile/me"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"Test", "lastName":"User"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("profile_already_exists"));
	}

	@Test
	@DisplayName("Deleting a profile cascades its child data and allows recreation")
	void deletesProfileWithChildren() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);
		mockMvc.perform(authenticated(post("/api/v1/profile/me/skills"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "skillCode": "java",
						  "displayName": "Java",
						  "category": "Backend"
						}
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(authenticated(delete("/api/v1/profile/me"), accountId))
				.andExpect(status().isNoContent());

		mockMvc.perform(authenticated(get("/api/v1/profile/me"), accountId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("profile_not_found"));

		createProfile(accountId);
		mockMvc.perform(authenticated(get("/api/v1/profile/me/skills"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}
}
