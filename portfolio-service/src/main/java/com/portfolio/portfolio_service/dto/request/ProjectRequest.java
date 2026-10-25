package com.portfolio.portfolio_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectRequest(
		@NotBlank(message = "Slug must not be blank")
		@Size(max = 100, message = "Slug must not exceed 100 characters")
		@Pattern(regexp = "\\s*[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*\\s*",
				message = "Slug must contain letters, digits or hyphens")
		String slug,

		@NotBlank(message = "Title must not be blank")
		@Size(max = 150, message = "Title must not exceed 150 characters")
		String title,

		@Size(max = 500, message = "Summary must not exceed 500 characters")
		String summary,

		@Size(max = 10000, message = "Description must not exceed 10000 characters")
		String description,

		@Size(max = 2048, message = "Source code URL must not exceed 2048 characters")
		String sourceCodeUrl,

		@Size(max = 2048, message = "Live URL must not exceed 2048 characters")
		String liveUrl,

		@Size(max = 2048, message = "Cover image URL must not exceed 2048 characters")
		String coverImageUrl,

		LocalDate startedOn,

		LocalDate completedOn,

		@NotNull(message = "Featured must be specified")
		Boolean featured) {
}
