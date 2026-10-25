package com.portfolio.identity_service.dto.response;

import com.portfolio.identity_service.entities.AccountStatus;

import java.util.UUID;

public record AccountSummaryResponse(
		UUID id,
		String email,
		String username,
		AccountStatus status) {
}
