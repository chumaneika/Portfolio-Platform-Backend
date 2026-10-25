package com.portfolio.profile_service.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "work_experiences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkExperienceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false)
	private ProfileEntity profileEntity;

	@Column(name = "company_name", nullable = false, length = 150)
	private String companyName;

	@Column(name = "position_title", nullable = false, length = 150)
	private String positionTitle;

	@Enumerated(EnumType.STRING)
	@Column(name = "employment_type", length = 30)
	private EmploymentType employmentType;

	@Column(length = 200)
	private String location;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "started_on", nullable = false)
	private LocalDate startedOn;

	@Column(name = "ended_on")
	private LocalDate endedOn;

	@Column(name = "current_position", nullable = false)
	private boolean currentPosition;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public WorkExperienceEntity(
			ProfileEntity profileEntity,
			String companyName,
			String positionTitle,
			EmploymentType employmentType,
			LocalDate startedOn,
			int displayOrder) {
		this.profileEntity = Objects.requireNonNull(profileEntity, "profile must not be null");
		this.companyName = requireText(companyName, "companyName");
		this.positionTitle = requireText(positionTitle, "positionTitle");
		this.employmentType = employmentType;
		this.startedOn = Objects.requireNonNull(startedOn, "startedOn must not be null");
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	public void updateDetails(
			String companyName,
			String positionTitle,
			EmploymentType employmentType,
			String location,
			String description) {
		this.companyName = requireText(companyName, "companyName");
		this.positionTitle = requireText(positionTitle, "positionTitle");
		this.employmentType = employmentType;
		this.location = trimToNull(location);
		this.description = trimToNull(description);
	}

	public void setPeriod(LocalDate startedOn, LocalDate endedOn, boolean currentPosition) {
		var start = Objects.requireNonNull(startedOn, "startedOn must not be null");
		if (currentPosition && endedOn != null) {
			throw new IllegalArgumentException("A current position cannot have an end date");
		}
		if (endedOn != null && endedOn.isBefore(start)) {
			throw new IllegalArgumentException("endedOn must not be before startedOn");
		}
		this.startedOn = start;
		this.endedOn = endedOn;
		this.currentPosition = currentPosition;
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
