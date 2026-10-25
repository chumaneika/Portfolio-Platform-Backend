package com.portfolio.profile_service.dto.response;

public record ProfileSkillResponse(
		String skillCode,
		String displayName,
		String category,
		int displayOrder) {
}
