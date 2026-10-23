package com.portfolio.identity_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
		String issuer,
		String secret,
		Duration accessTokenTtl,
		Duration refreshTokenTtl) {

	public JwtProperties {
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("security.jwt.issuer must not be blank");
		}
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("security.jwt.secret must not be blank");
		}
		if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
			throw new IllegalArgumentException("security.jwt.access-token-ttl must be positive");
		}
		if (refreshTokenTtl == null || refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
			throw new IllegalArgumentException("security.jwt.refresh-token-ttl must be positive");
		}
	}
}
