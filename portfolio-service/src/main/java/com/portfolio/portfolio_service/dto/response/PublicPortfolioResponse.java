package com.portfolio.portfolio_service.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicPortfolioResponse(
		UUID id,
		String slug,
		String title,
		String description,
		OffsetDateTime publishedAt,
		Map<String, Object> profile,
		List<PublicProjectResponse> projects) {
}
