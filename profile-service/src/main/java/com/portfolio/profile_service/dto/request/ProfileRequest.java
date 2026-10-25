package com.portfolio.profile_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
		@NotBlank(message = "First name must not be blank")
		@Size(max = 100, message = "First name must not exceed 100 characters")
		String firstName,

		@NotBlank(message = "Last name must not be blank")
		@Size(max = 100, message = "Last name must not exceed 100 characters")
		String lastName,

		@Size(max = 200, message = "Headline must not exceed 200 characters")
		String headline,

		@Size(max = 5000, message = "Bio must not exceed 5000 characters")
		String bio,

		@Size(max = 200, message = "Location must not exceed 200 characters")
		String location,

		@Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
		String avatarUrl,

		@Size(max = 2048, message = "Resume URL must not exceed 2048 characters")
		String resumeUrl,

		@NotNull(message = "Available for work must be specified")
		Boolean availableForWork) {
}
