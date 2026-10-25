package com.portfolio.profile_service.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileResponse(
		UUID id,
		String firstName,
		String lastName,
		String headline,
		String bio,
		String location,
		String avatarUrl,
		String resumeUrl,
		boolean availableForWork,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
