package com.portfolio.identity_service.services;

import com.portfolio.identity_service.config.JwtProperties;
import com.portfolio.identity_service.entities.Account;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;

@Service
public class TokenService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	public String createAccessToken(Account account) {
		var issuedAt = clock.instant();
		var claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(properties.accessTokenTtl()))
				.subject(account.getId().toString())
				.claim("email", account.getEmail())
				.claim("username", account.getUsername())
				.build();
		var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public String createRefreshToken() {
		var bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String hashRefreshToken(String token) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			return java.util.HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	public long accessTokenExpiresInSeconds() {
		return properties.accessTokenTtl().toSeconds();
	}
}
