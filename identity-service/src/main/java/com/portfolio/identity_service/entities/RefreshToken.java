package com.portfolio.identity_service.entities;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(name = "token_hash", nullable = false, unique = true, length = 128)
	private String tokenHash;

	@CreationTimestamp
	@Column(name = "issued_at", nullable = false, updatable = false)
	private OffsetDateTime issuedAt;

	@Column(name = "expires_at", nullable = false)
	private OffsetDateTime expiresAt;

	@Column(name = "revoked_at")
	private OffsetDateTime revokedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "replacement_token_id")
	private RefreshToken replacementToken;

	@Column(name = "user_agent", length = 512)
	private String userAgent;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	public RefreshToken(
			Account account,
			String tokenHash,
			OffsetDateTime expiresAt,
			String userAgent,
			String ipAddress) {
		this.account = Objects.requireNonNull(account, "account must not be null");
		this.tokenHash = requireText(tokenHash, "tokenHash");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		this.userAgent = trimToNull(userAgent);
		this.ipAddress = trimToNull(ipAddress);
	}

	public boolean isActiveAt(OffsetDateTime timestamp) {
		Objects.requireNonNull(timestamp, "timestamp must not be null");
		return revokedAt == null && expiresAt.isAfter(timestamp);
	}

	public void revoke(OffsetDateTime revokedAt) {
		this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
	}

	public void replaceWith(RefreshToken replacementToken, OffsetDateTime revokedAt) {
		var replacement = Objects.requireNonNull(replacementToken, "replacementToken must not be null");
		if (replacement == this) {
			throw new IllegalArgumentException("A refresh token cannot replace itself");
		}
		if (replacement.account != account) {
			throw new IllegalArgumentException("Replacement token must belong to the same account");
		}

		this.replacementToken = replacement;
		revoke(revokedAt);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	private static String trimToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
