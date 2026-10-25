package com.portfolio.portfolio_service.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
		UUID id,
		String slug,
		String title,
		String summary,
		String description,
		String sourceCodeUrl,
		String liveUrl,
		String coverImageUrl,
		LocalDate startedOn,
		LocalDate completedOn,
		int displayOrder,
		boolean featured,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
