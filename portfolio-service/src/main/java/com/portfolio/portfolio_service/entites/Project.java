package com.portfolio.portfolio_service.entites;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "portfolio_id", nullable = false)
	private Portfolio portfolio;

	@Column(nullable = false, length = 100)
	private String slug;

	@Column(nullable = false, length = 150)
	private String title;

	@Column(length = 500)
	private String summary;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "source_code_url", length = 2048)
	private String sourceCodeUrl;

	@Column(name = "live_url", length = 2048)
	private String liveUrl;

	@Column(name = "cover_image_url", length = 2048)
	private String coverImageUrl;

	@Column(name = "started_on")
	private LocalDate startedOn;

	@Column(name = "completed_on")
	private LocalDate completedOn;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(nullable = false)
	private boolean featured;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Project(Portfolio portfolio, String slug, String title, int displayOrder) {
		this.portfolio = Objects.requireNonNull(portfolio, "portfolio must not be null");
		this.slug = normalizeCode(slug, "slug");
		this.title = requireText(title, "title");
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	public void updateContent(String title, String summary, String description) {
		this.title = requireText(title, "title");
		this.summary = trimToNull(summary);
		this.description = trimToNull(description);
	}

	public void updateLinks(String sourceCodeUrl, String liveUrl, String coverImageUrl) {
		this.sourceCodeUrl = trimToNull(sourceCodeUrl);
		this.liveUrl = trimToNull(liveUrl);
		this.coverImageUrl = trimToNull(coverImageUrl);
	}

	public void changeSlug(String slug) {
		this.slug = normalizeCode(slug, "slug");
	}

	public void setTimeline(LocalDate startedOn, LocalDate completedOn) {
		if (startedOn != null && completedOn != null && completedOn.isBefore(startedOn)) {
			throw new IllegalArgumentException("completedOn must not be before startedOn");
		}
		this.startedOn = startedOn;
		this.completedOn = completedOn;
	}

	public void changeDisplayOrder(int displayOrder) {
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	public void markFeatured(boolean featured) {
		this.featured = featured;
	}

	private static int requireNonNegative(int value, String fieldName) {
		if (value < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
		return value;
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
