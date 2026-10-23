package com.portfolio.identity_service.entities;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

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

	public RefreshToken(Account account, String tokenHash, OffsetDateTime expiresAt) {
		this.account = account;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public boolean isActiveAt(OffsetDateTime now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(OffsetDateTime now) {
		this.revokedAt = now;
	}
}
