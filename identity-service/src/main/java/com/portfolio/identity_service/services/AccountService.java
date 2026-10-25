package com.portfolio.identity_service.services;

import com.portfolio.identity_service.dto.request.ChangePasswordRequest;
import com.portfolio.identity_service.dto.request.ChangeUsernameRequest;
import com.portfolio.identity_service.dto.response.AccountSummaryResponse;
import com.portfolio.identity_service.entities.AccountStatus;
import com.portfolio.identity_service.repositories.AccountRepository;
import com.portfolio.identity_service.repositories.RefreshTokenRepository;
import com.portfolio.identity_service.services.exceptions.AccountServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional(readOnly = true)
	public AccountSummaryResponse getAccount(UUID accountId) {
		return accountRepository.findAccount(accountId)
				.orElseThrow(AccountServiceException::accountNotFound);
	}

	@Transactional
	public void changePassword(UUID accountId, ChangePasswordRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var account = accountRepository.findById(accountId)
				.orElseThrow(AccountServiceException::accountNotFound);
		if (account.getStatus() != AccountStatus.ACTIVE) {
			throw AccountServiceException.accountInactive();
		}
		if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
				|| request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw AccountServiceException.passwordTooLong();
		}
		if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
			throw AccountServiceException.invalidCurrentPassword();
		}
		if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
			throw AccountServiceException.passwordUnchanged();
		}
		account.changePasswordHash(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.revokeAllForAccount(accountId, OffsetDateTime.now(ZoneOffset.UTC));
	}

	@Transactional
	public void revokeSessions(UUID accountId) {
		accountRepository.findById(accountId)
				.orElseThrow(AccountServiceException::accountNotFound);
		refreshTokenRepository.revokeAllForAccount(accountId, OffsetDateTime.now(ZoneOffset.UTC));
	}

	@Transactional
	public void changeUsername(UUID accountId, ChangeUsernameRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		var account = accountRepository.findById(accountId)
				.orElseThrow(AccountServiceException::accountNotFound);
		if (account.getStatus() != AccountStatus.ACTIVE) {
			throw AccountServiceException.accountInactive();
		}
		var username = request.username().trim().toLowerCase(Locale.ROOT);
		if (account.getUsername().equals(username)) {
			return;
		}
		if (accountRepository.existsByUsername(username)) {
			throw AccountServiceException.usernameAlreadyExists();
		}
		account.changeUsername(username);
		try {
			accountRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw AccountServiceException.usernameAlreadyExists();
		}
	}
}
