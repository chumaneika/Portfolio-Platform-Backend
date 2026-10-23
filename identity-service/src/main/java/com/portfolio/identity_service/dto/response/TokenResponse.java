package com.portfolio.identity_service.dto.response;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn) {
}
