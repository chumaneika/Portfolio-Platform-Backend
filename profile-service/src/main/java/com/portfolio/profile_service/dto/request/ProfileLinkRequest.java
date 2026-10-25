package com.portfolio.profile_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileLinkRequest(
		@NotBlank(message = "Platform must not be blank")
		@Size(max = 50, message = "Platform must not exceed 50 characters")
		String platform,

		@Size(max = 100, message = "Label must not exceed 100 characters")
		String label,

		@NotBlank(message = "URL must not be blank")
		@Size(max = 2048, message = "URL must not exceed 2048 characters")
		String url) {
}
