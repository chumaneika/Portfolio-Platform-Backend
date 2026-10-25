package com.portfolio.portfolio_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectTechnologyRequest(
		@NotBlank(message = "Display name must not be blank")
		@Size(max = 100, message = "Display name must not exceed 100 characters")
		String displayName) {
}
