package com.portfolio.identity_service.services;

import com.portfolio.identity_service.dto.request.LoginRequest;
import com.portfolio.identity_service.dto.request.RegisterRequest;
import com.portfolio.identity_service.dto.response.TokenResponse;
import com.portfolio.identity_service.entities.Account;
import com.portfolio.identity_service.entities.AccountStatus;
import com.portfolio.identity_service.entities.RefreshToken;
import com.portfolio.identity_service.exceptions.ApiException;
import com.portfolio.identity_service.repositories.AccountRepository;
import com.portfolio.identity_service.repositories.RefreshTokenRepository;
import com.portfolio.identity_service.security.AccountUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final AccountRepository accountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtEncoder jwtEncoder;
	private final String jwtIssuer;
	private final Duration accessTokenTtl;
	private final Duration refreshTokenTtl;

	public AuthService(
			AccountRepository accountRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtEncoder jwtEncoder,
			@Value("${security.jwt.issuer}") String jwtIssuer,
			@Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl,
			@Value("${security.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
		this.accountRepository = accountRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		this.jwtIssuer = jwtIssuer;
		this.accessTokenTtl = accessTokenTtl;
		this.refreshTokenTtl = refreshTokenTtl;
	}

	@Transactional
	public void register(RegisterRequest request) {
		var email = normalize(request.email());
		var username = normalize(request.username());
		validatePasswordLength(request.password());

		if (accountRepository.existsByEmail(email)) {
			throw conflict("An account with this email already exists");
		}
		if (accountRepository.existsByUsername(username)) {
			throw conflict("An account with this username already exists");
		}

		try {
			accountRepository.saveAndFlush(
					new Account(email, username, passwordEncoder.encode(request.password())));
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Email or username is already in use");
		}
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		var email = normalize(request.email());
		try {
			var authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, request.password()));
			var user = (AccountUserDetails) authentication.getPrincipal();
			var account = accountRepository.getReferenceById(user.getAccountId());
			return issueTokens(account);
		} catch (AuthenticationException exception) {
			throw authenticationFailed("Invalid login or password");
		}
	}

	@Transactional
	public TokenResponse refresh(String rawRefreshToken) {
		var storedToken = refreshTokenRepository
				.findByTokenHashForUpdate(hashRefreshToken(rawRefreshToken))
				.orElseThrow(() -> authenticationFailed("Invalid refresh token"));
		var now = OffsetDateTime.now(ZoneOffset.UTC);

		if (!storedToken.isActiveAt(now)
				|| storedToken.getAccount().getStatus() != AccountStatus.ACTIVE) {
			throw authenticationFailed("Invalid refresh token");
		}

		storedToken.revoke(now);
		return issueTokens(storedToken.getAccount());
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenRepository
				.findByTokenHashForUpdate(hashRefreshToken(rawRefreshToken))
				.filter(token -> token.isActiveAt(OffsetDateTime.now(ZoneOffset.UTC)))
				.ifPresent(token -> token.revoke(OffsetDateTime.now(ZoneOffset.UTC)));
	}

	private TokenResponse issueTokens(Account account) {
		var issuedAt = Instant.now();
		var claims = JwtClaimsSet.builder()
				.issuer(jwtIssuer)
				.id(UUID.randomUUID().toString())
				.subject(account.getId().toString())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(accessTokenTtl))
				.claim("email", account.getEmail())
				.build();
		var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		var accessToken = jwtEncoder
				.encode(JwtEncoderParameters.from(header, claims))
				.getTokenValue();
		var rawRefreshToken = createRefreshToken();
		refreshTokenRepository.save(new RefreshToken(
				account,
				hashRefreshToken(rawRefreshToken),
				OffsetDateTime.now(ZoneOffset.UTC).plus(refreshTokenTtl)));

		return new TokenResponse(
				accessToken,
				rawRefreshToken,
				"Bearer",
				accessTokenTtl.toSeconds());
	}

	private static String createRefreshToken() {
		var bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hashRefreshToken(String token) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static void validatePasswordLength(String password) {
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"invalid_request",
					"Password must not exceed 72 UTF-8 bytes");
		}
	}

	private static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, "account_conflict", message);
	}

	private static ApiException authenticationFailed(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, "authentication_failed", message);
	}
}
