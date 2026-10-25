package com.portfolio.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequest(
		@NotBlank(message = "Username must not be blank")
		@Size(min = 3, max = 50, message = "Username must contain 3 to 50 characters")
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$",
				message = "Username must contain only letters, numbers, dot, underscore or hyphen")
		String username) {
}
