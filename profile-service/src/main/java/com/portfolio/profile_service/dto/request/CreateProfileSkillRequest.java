package com.portfolio.profile_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProfileSkillRequest(
		@NotBlank(message = "Skill code must not be blank")
		@Size(max = 50, message = "Skill code must not exceed 50 characters")
		String skillCode,

		@NotBlank(message = "Display name must not be blank")
		@Size(max = 100, message = "Display name must not exceed 100 characters")
		String displayName,

		@Size(max = 50, message = "Category must not exceed 50 characters")
		String category) {
}
