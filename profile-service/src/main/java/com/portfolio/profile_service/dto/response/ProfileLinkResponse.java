package com.portfolio.profile_service.dto.response;

import java.util.UUID;

public record ProfileLinkResponse(
		UUID id,
		String platform,
		String label,
		String url,
		int displayOrder) {
}
