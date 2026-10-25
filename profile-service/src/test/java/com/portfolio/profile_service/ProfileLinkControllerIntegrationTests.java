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

class ProfileLinkControllerIntegrationTests extends ControllerIntegrationTestSupport {

	@Test
	@DisplayName("Profile link controller supports normalized CRUD flow")
	void linkLifecycle() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);

		var location = mockMvc.perform(authenticated(post("/api/v1/profile/me/links"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(" GitHub ", "Source code", "https://github.com/user")))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.platform").value("github"))
				.andExpect(jsonPath("$.displayOrder").value(0))
				.andReturn()
				.getResponse()
				.getHeader("Location");

		mockMvc.perform(authenticated(get("/api/v1/profile/me/links"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].platform").value("github"));

		mockMvc.perform(authenticated(put(location), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request(
						"LinkedIn",
						"Professional profile",
						"https://linkedin.com/in/user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.platform").value("linkedin"))
				.andExpect(jsonPath("$.label").value("Professional profile"));

		mockMvc.perform(authenticated(delete(location), accountId))
				.andExpect(status().isNoContent());

		mockMvc.perform(authenticated(get("/api/v1/profile/me/links"), accountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Profile link controller maps invalid URLs to a business API error")
	void rejectsInvalidUrl() throws Exception {
		var accountId = UUID.randomUUID();
		createProfile(accountId);

		mockMvc.perform(authenticated(post("/api/v1/profile/me/links"), accountId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(request("github", null, "github.com/user")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_profile_link_url"));
	}

	private static String request(String platform, String label, String url) {
		var jsonLabel = label == null ? "null" : "\"" + label + "\"";
		return """
				{
				  "platform": "%s",
				  "label": %s,
				  "url": "%s"
				}
				""".formatted(platform, jsonLabel, url);
	}
}
