package com.portfolio.portfolio_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PortfolioRequest(
		@NotBlank(message = "Slug must not be blank")
		@Size(max = 100, message = "Slug must not exceed 100 characters")
		@Pattern(regexp = "\\s*[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*\\s*",
				message = "Slug must contain letters, digits or hyphens")
		String slug,

		@NotBlank(message = "Title must not be blank")
		@Size(max = 120, message = "Title must not exceed 120 characters")
		String title,

		@Size(max = 1000, message = "Description must not exceed 1000 characters")
		String description) {
}
