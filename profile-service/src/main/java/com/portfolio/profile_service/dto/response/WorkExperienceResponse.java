package com.portfolio.profile_service.dto.response;

import com.portfolio.profile_service.entities.EmploymentType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkExperienceResponse(
		UUID id,
		String companyName,
		String positionTitle,
		EmploymentType employmentType,
		String location,
		String description,
		LocalDate startedOn,
		LocalDate endedOn,
		boolean currentPosition,
		int displayOrder,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
