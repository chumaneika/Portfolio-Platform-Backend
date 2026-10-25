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

class WorkExperienceControllerIntegrationTests extends ControllerIntegrationTestSupport {

	@Test
	@DisplayName("Work experience controller supports the complete CRUD flow")
	void workExperienceLifecycle() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);

		var location = mockMvc.perform(authenticated(post("/api/v1/profile/me/work-experiences"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("Company", "Developer", true, null)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.companyName").value("Company"))
				.andExpect(jsonPath("$.displayOrder").value(0))
				.andReturn()
				.getResponse()
				.getHeader("Location");

		mockMvc.perform(authenticated(get("/api/v1/profile/me/work-experiences"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].companyName").value("Company"));

		mockMvc.perform(authenticated(put(location), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("New Company", "Senior Developer", false, "2025-01-01")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.companyName").value("New Company"))
				.andExpect(jsonPath("$.endedOn").value("2025-01-01"));

		mockMvc.perform(authenticated(delete(location), accountId))
				.andExpect(status().isNoContent());

		mockMvc.perform(authenticated(get("/api/v1/profile/me/work-experiences"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Work experience controller maps invalid periods to a business API error")
	void rejectsInvalidPeriod() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);

		mockMvc.perform(authenticated(post("/api/v1/profile/me/work-experiences"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("Company", "Developer", true, "2025-01-01")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_work_experience_period"));
	}

	private static String request(
			String companyName,
			String positionTitle,
			boolean currentPosition,
			String endedOn) {
		var endDate = endedOn == null ? "null" : "\"" + endedOn + "\"";
		return """
				{
				  "companyName": "%s",
				  "positionTitle": "%s",
				  "employmentType": "FULL_TIME",
				  "location": "Moscow",
				  "description": "Backend development",
				  "startedOn": "2024-01-01",
				  "endedOn": %s,
				  "currentPosition": %s
				}
				""".formatted(companyName, positionTitle, endDate, currentPosition);
	}
}
