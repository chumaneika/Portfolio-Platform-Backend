package com.portfolio.profile_service;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicProfileControllerIntegrationTests extends ControllerIntegrationTestSupport {

	@Test
	@DisplayName("Public profile returns the complete portfolio without authentication")
	void returnsCompletePublicProfile() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);
		var profileJson = mockMvc.perform(authenticated(get("/api/v1/profile/me"), accountId))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		var profileId = JsonPath.<String>read(profileJson, "$.id");

		mockMvc.perform(authenticated(post("/api/v1/profile/me/work-experiences"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "companyName": "Company",
						  "positionTitle": "Developer",
						  "employmentType": "FULL_TIME",
						  "startedOn": "2024-01-01",
						  "endedOn": null,
						  "currentPosition": true
						}
						"""))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticated(post("/api/v1/profile/me/educations"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "institutionName": "University",
						  "startedOn": "2020-09-01",
						  "endedOn": "2024-06-01",
						  "currentEducation": false
						}
						"""))
				.andExpect(status().isCreated());
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
		mockMvc.perform(authenticated(post("/api/v1/profile/me/links"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "platform": "github",
						  "label": "Source code",
						  "url": "https://github.com/user"
						}
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/profiles/{profileId}", profileId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.profile.id").value(profileId))
				.andExpect(jsonPath("$.profile.firstName").value("Test"))
				.andExpect(jsonPath("$.workExperiences[0].companyName").value("Company"))
				.andExpect(jsonPath("$.educations[0].institutionName").value("University"))
				.andExpect(jsonPath("$.skills[0].skillCode").value("java"))
				.andExpect(jsonPath("$.links[0].platform").value("github"));

		mockMvc.perform(get("/api/v1/profiles/by-account/{accountId}", accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.profile.id").value(profileId))
				.andExpect(jsonPath("$.workExperiences[0].companyName").value("Company"))
				.andExpect(jsonPath("$.educations[0].institutionName").value("University"))
				.andExpect(jsonPath("$.skills[0].skillCode").value("java"))
				.andExpect(jsonPath("$.links[0].platform").value("github"));
	}

	@Test
	@DisplayName("Public profile returns the API error contract for an unknown id")
	void reportsMissingPublicProfile() throws Exception {
		mockMvc.perform(get("/api/v1/profiles/{profileId}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("profile_not_found"));

		mockMvc.perform(get("/api/v1/profiles/by-account/{accountId}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("profile_not_found"));
	}
}
