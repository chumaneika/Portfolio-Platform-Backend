package com.portfolio.profile_service.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EducationResponse(
		UUID id,
		String institutionName,
		String degree,
		String fieldOfStudy,
		String description,
		LocalDate startedOn,
		LocalDate endedOn,
		boolean currentEducation,
		int displayOrder,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
