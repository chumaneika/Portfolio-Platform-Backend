package com.portfolio.portfolio_service.dto.response;

public record ProjectTechnologyResponse(
		String technologyCode,
		String displayName,
		int displayOrder) {
}
