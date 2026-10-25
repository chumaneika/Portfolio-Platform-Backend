package com.portfolio.portfolio_service.dto.response;

import com.portfolio.portfolio_service.entites.PortfolioStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PortfolioResponse(
		UUID id,
		String slug,
		String title,
		String description,
		PortfolioStatus status,
		OffsetDateTime publishedAt,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
