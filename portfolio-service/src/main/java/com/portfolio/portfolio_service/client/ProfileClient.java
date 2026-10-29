package com.portfolio.portfolio_service.client;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProfileClient {
	Optional<Map<String, Object>> getPublicProfile(UUID accountId);
}
