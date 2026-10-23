package com.portfolio.identity_service.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;



@Getter
@Entity
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AccountStatus status = AccountStatus.ACTIVE;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private OffsetDateTime lockedUntil;

	@Column(name = "last_login_at")
	private OffsetDateTime lastLoginAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Account(String email, String username, String passwordHash) {
		this.email = normalize(email, "email");
		this.username = normalize(username, "username");
		this.passwordHash = requireText(passwordHash, "passwordHash");
	}

	public void changePasswordHash(String passwordHash) {
		this.passwordHash = requireText(passwordHash, "passwordHash");
	}

	public void verifyEmail() {
		this.emailVerified = true;
		if (status == AccountStatus.PENDING_VERIFICATION) {
			status = AccountStatus.ACTIVE;
		}
	}

	public void registerFailedLogin(int maximumAttempts, OffsetDateTime lockUntil) {
		if (maximumAttempts < 1) {
			throw new IllegalArgumentException("maximumAttempts must be positive");
		}

		failedLoginAttempts++;
		if (failedLoginAttempts >= maximumAttempts) {
			status = AccountStatus.LOCKED;
			lockedUntil = Objects.requireNonNull(lockUntil, "lockUntil must not be null");
		}
	}

	public void registerSuccessfulLogin(OffsetDateTime loggedInAt) {
		lastLoginAt = Objects.requireNonNull(loggedInAt, "loggedInAt must not be null");
		failedLoginAttempts = 0;
		lockedUntil = null;
		if (status == AccountStatus.LOCKED) {
			status = AccountStatus.ACTIVE;
		}
	}

	public void disable() {
		status = AccountStatus.DISABLED;
		lockedUntil = null;
	}

	private static String normalize(String value, String fieldName) {
		return requireText(value, fieldName).toLowerCase(Locale.ROOT);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}
}
