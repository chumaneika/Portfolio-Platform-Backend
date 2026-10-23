package com.portfolio.identity_service.services;

import com.portfolio.identity_service.config.JwtProperties;
import com.portfolio.identity_service.dto.request.LoginRequest;
import com.portfolio.identity_service.dto.request.RegisterRequest;
import com.portfolio.identity_service.dto.response.TokenResponse;
import com.portfolio.identity_service.entities.Account;
import com.portfolio.identity_service.entities.AccountStatus;
import com.portfolio.identity_service.entities.RefreshToken;
import com.portfolio.identity_service.exceptions.ApiException;
import com.portfolio.identity_service.repositories.AccountRepository;
import com.portfolio.identity_service.repositories.RefreshTokenRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class AuthService {

	private static final int MAXIMUM_LOGIN_ATTEMPTS = 5;
	private static final java.time.Duration LOCK_DURATION = java.time.Duration.ofMinutes(15);

	private final AccountRepository accountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final JwtProperties jwtProperties;
	private final Clock clock;

	public AuthService(
			AccountRepository accountRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			TokenService tokenService,
			JwtProperties jwtProperties,
			Clock clock) {
		this.accountRepository = accountRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.jwtProperties = jwtProperties;
		this.clock = clock;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request, String userAgent, String ipAddress) {
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
			var account = accountRepository.saveAndFlush(
					new Account(email, username, passwordEncoder.encode(request.password())));
			return issueTokenPair(account, userAgent, ipAddress);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Email or username is already in use");
		}
	}

	@Transactional(noRollbackFor = ApiException.class)
	public TokenResponse login(LoginRequest request, String userAgent, String ipAddress) {
		var login = normalize(request.login());
		var account = accountRepository.findByEmailOrUsername(login, login)
				.orElseThrow(() -> authenticationFailed("Invalid login or password"));
		var now = OffsetDateTime.now(clock);

		if (account.getStatus() == AccountStatus.DISABLED
				|| account.getStatus() == AccountStatus.PENDING_VERIFICATION) {
			throw authenticationFailed("Account is not available for login");
		}
		if (account.getStatus() == AccountStatus.LOCKED
				&& account.getLockedUntil() != null
				&& account.getLockedUntil().isAfter(now)) {
			throw authenticationFailed("Account is temporarily locked");
		}
		if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
			account.registerFailedLogin(MAXIMUM_LOGIN_ATTEMPTS, now.plus(LOCK_DURATION));
			throw authenticationFailed("Invalid login or password");
		}

		account.registerSuccessfulLogin(now);
		return issueTokenPair(account, userAgent, ipAddress);
	}

	@Transactional
	public TokenResponse refresh(String rawRefreshToken, String userAgent, String ipAddress) {
		var currentToken = refreshTokenRepository
				.findByTokenHashForUpdate(tokenService.hashRefreshToken(rawRefreshToken))
				.orElseThrow(() -> authenticationFailed("Invalid refresh token"));
		var now = OffsetDateTime.now(clock);

		if (!currentToken.isActiveAt(now)) {
			throw authenticationFailed("Refresh token is expired or revoked");
		}
		if (currentToken.getAccount().getStatus() != AccountStatus.ACTIVE) {
			throw authenticationFailed("Account is not available for login");
		}

		var rawReplacement = tokenService.createRefreshToken();
		var replacement = refreshTokenRepository.save(new RefreshToken(
				currentToken.getAccount(),
				tokenService.hashRefreshToken(rawReplacement),
				now.plus(jwtProperties.refreshTokenTtl()),
				userAgent,
				ipAddress));
		currentToken.replaceWith(replacement, now);

		return response(currentToken.getAccount(), rawReplacement);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenRepository.findByTokenHashForUpdate(tokenService.hashRefreshToken(rawRefreshToken))
				.filter(token -> token.isActiveAt(OffsetDateTime.now(clock)))
				.ifPresent(token -> token.revoke(OffsetDateTime.now(clock)));
	}

	private TokenResponse issueTokenPair(Account account, String userAgent, String ipAddress) {
		var rawRefreshToken = tokenService.createRefreshToken();
		var now = OffsetDateTime.now(clock);
		refreshTokenRepository.save(new RefreshToken(
				account,
				tokenService.hashRefreshToken(rawRefreshToken),
				now.plus(jwtProperties.refreshTokenTtl()),
				userAgent,
				ipAddress));
		return response(account, rawRefreshToken);
	}

	private TokenResponse response(Account account, String rawRefreshToken) {
		return new TokenResponse(
				tokenService.createAccessToken(account),
				rawRefreshToken,
				"Bearer",
				tokenService.accessTokenExpiresInSeconds());
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

	private static ApiException authenticationFailed(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, "authentication_failed", message);
	}

	private static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, "account_conflict", message);
	}
}
