package com.portfolio.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank(message = "Current password must not be blank")
		String currentPassword,

		@NotBlank(message = "New password must not be blank")
		@Size(min = 8, max = 72, message = "New password must contain 8 to 72 characters")
		String newPassword) {
}
