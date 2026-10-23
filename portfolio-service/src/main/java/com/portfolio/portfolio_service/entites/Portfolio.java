package com.portfolio.portfolio_service.entites;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "portfolios")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "owner_account_id", nullable = false)
	private UUID ownerAccountId;

	@Column(nullable = false, unique = true, length = 100)
	private String slug;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PortfolioStatus status = PortfolioStatus.DRAFT;

	@Column(name = "published_at")
	private OffsetDateTime publishedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Portfolio(UUID ownerAccountId, String slug, String title, String description) {
		this.ownerAccountId = Objects.requireNonNull(ownerAccountId, "ownerAccountId must not be null");
		this.slug = normalizeCode(slug, "slug");
		this.title = requireText(title, "title");
		this.description = trimToNull(description);
	}

	public void updateDetails(String title, String description) {
		this.title = requireText(title, "title");
		this.description = trimToNull(description);
	}

	public void changeSlug(String slug) {
		this.slug = normalizeCode(slug, "slug");
	}

	public void publish(OffsetDateTime publishedAt) {
		this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
		this.status = PortfolioStatus.PUBLISHED;
	}

	public void moveToDraft() {
		this.status = PortfolioStatus.DRAFT;
	}

	public void archive() {
		this.status = PortfolioStatus.ARCHIVED;
	}

	private static String normalizeCode(String value, String fieldName) {
		return requireText(value, fieldName).toLowerCase(Locale.ROOT);
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
