package com.portfolio.profile_service.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
@Table(name = "educations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EducationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false)
	private ProfileEntity profileEntity;

	@Column(name = "institution_name", nullable = false, length = 200)
	private String institutionName;

	@Column(length = 150)
	private String degree;

	@Column(name = "field_of_study", length = 150)
	private String fieldOfStudy;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "started_on")
	private LocalDate startedOn;

	@Column(name = "ended_on")
	private LocalDate endedOn;

	@Column(name = "current_education", nullable = false)
	private boolean currentEducation;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public EducationEntity(ProfileEntity profileEntity, String institutionName, int displayOrder) {
		this.profileEntity = Objects.requireNonNull(profileEntity, "profile must not be null");
		this.institutionName = requireText(institutionName, "institutionName");
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	public void updateDetails(
			String institutionName,
			String degree,
			String fieldOfStudy,
			String description) {
		this.institutionName = requireText(institutionName, "institutionName");
		this.degree = trimToNull(degree);
		this.fieldOfStudy = trimToNull(fieldOfStudy);
		this.description = trimToNull(description);
	}

	public void setPeriod(LocalDate startedOn, LocalDate endedOn, boolean currentEducation) {
		if (currentEducation && endedOn != null) {
			throw new IllegalArgumentException("Current education cannot have an end date");
		}
		if (startedOn != null && endedOn != null && endedOn.isBefore(startedOn)) {
			throw new IllegalArgumentException("endedOn must not be before startedOn");
		}
		this.startedOn = startedOn;
		this.endedOn = endedOn;
		this.currentEducation = currentEducation;
	}

	public void changeDisplayOrder(int displayOrder) {
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
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
