package com.portfolio.profile_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String issuer, String secret) {

	public JwtProperties {
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("security.jwt.issuer must not be blank");
		}
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("security.jwt.secret must not be blank");
		}
	}
}
