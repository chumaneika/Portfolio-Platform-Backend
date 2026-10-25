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

class EducationControllerIntegrationTests extends ControllerIntegrationTestSupport {

	@Test
	@DisplayName("Education controller supports the complete CRUD flow")
	void educationLifecycle() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);

		var location = mockMvc.perform(authenticated(post("/api/v1/profile/me/educations"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("University", true, null)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.institutionName").value("University"))
				.andExpect(jsonPath("$.displayOrder").value(0))
				.andReturn()
				.getResponse()
				.getHeader("Location");

		mockMvc.perform(authenticated(get("/api/v1/profile/me/educations"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].institutionName").value("University"));

		mockMvc.perform(authenticated(put(location), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("New University", false, "2025-06-01")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.institutionName").value("New University"))
				.andExpect(jsonPath("$.endedOn").value("2025-06-01"));

		mockMvc.perform(authenticated(delete(location), accountId))
				.andExpect(status().isNoContent());

		mockMvc.perform(authenticated(get("/api/v1/profile/me/educations"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Education controller returns not found when another account owns the record")
	void hidesAnotherAccountsEducation() throws Exception {
		var ownerAccountId = UUID.randomUUID();
		var otherAccountId = UUID.randomUUID();
		createProfile(ownerAccountId);
		createProfile(otherAccountId);

		var location = mockMvc.perform(authenticated(post("/api/v1/profile/me/educations"), ownerAccountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("University", true, null)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader("Location");

		mockMvc.perform(authenticated(delete(location), otherAccountId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("education_not_found"));
	}

	private static String request(
			String institutionName,
			boolean currentEducation,
			String endedOn) {
		var endDate = endedOn == null ? "null" : "\"" + endedOn + "\"";
		return """
				{
				  "institutionName": "%s",
				  "degree": "Bachelor",
				  "fieldOfStudy": "Computer Science",
				  "description": "Description",
				  "startedOn": "2024-01-01",
				  "endedOn": %s,
				  "currentEducation": %s
				}
				""".formatted(institutionName, endDate, currentEducation);
	}
}
