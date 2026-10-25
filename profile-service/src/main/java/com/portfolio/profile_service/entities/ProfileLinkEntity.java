package com.portfolio.profile_service.entities;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

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
@Table(name = "profile_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileLinkEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false)
	private ProfileEntity profileEntity;

	@Column(nullable = false, length = 50)
	private String platform;

	@Column(length = 100)
	private String label;

	@Column(nullable = false, length = 2048)
	private String url;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	public ProfileLinkEntity(ProfileEntity profileEntity, String platform, String label, String url, int displayOrder) {
		this.profileEntity = Objects.requireNonNull(profileEntity, "profile must not be null");
		this.platform = normalizePlatform(platform);
		this.label = trimToNull(label);
		this.url = requireText(url, "url");
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	public void update(String platform, String label, String url, int displayOrder) {
		this.platform = normalizePlatform(platform);
		this.label = trimToNull(label);
		this.url = requireText(url, "url");
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	private static String normalizePlatform(String value) {
		return requireText(value, "platform").toLowerCase(Locale.ROOT);
	}

	private static int requireNonNegative(int value, String fieldName) {
		if (value < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
		return value;
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
