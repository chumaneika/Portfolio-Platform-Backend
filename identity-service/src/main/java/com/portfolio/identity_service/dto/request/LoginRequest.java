package com.portfolio.identity_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Size(max = 320) String login,
		@NotBlank @Size(max = 72) String password) {
}
