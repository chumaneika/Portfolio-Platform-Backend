package com.portfolio.profile_service.entities;

import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "profile_skills")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileSkillEntity {

	@EmbeddedId
	private ProfileSkillId id;

	@MapsId("profileId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false)
	private ProfileEntity profileEntity;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(length = 50)
	private String category;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	public ProfileSkillEntity(
			ProfileEntity profileEntity,
			String skillCode,
			String displayName,
			String category,
			int displayOrder) {
		this.profileEntity = Objects.requireNonNull(profileEntity, "profile must not be null");
		this.id = new ProfileSkillId(null, normalizeCode(skillCode));
		this.displayName = requireText(displayName, "displayName");
		this.category = trimToNull(category);
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	public String getSkillCode() {
		return id.getSkillCode();
	}

	public void updatePresentation(
			String displayName,
			String category,
			int displayOrder) {
		this.displayName = requireText(displayName, "displayName");
		this.category = trimToNull(category);
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	private static String normalizeCode(String value) {
		return requireText(value, "skillCode").toLowerCase(Locale.ROOT);
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
