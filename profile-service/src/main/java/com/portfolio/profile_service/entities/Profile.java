package com.portfolio.profile_service.entities;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "account_id", nullable = false, unique = true)
	private UUID accountId;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(length = 200)
	private String headline;

	@Column(columnDefinition = "TEXT")
	private String bio;

	@Column(length = 200)
	private String location;

	@Column(name = "avatar_url", length = 2048)
	private String avatarUrl;

	@Column(name = "resume_url", length = 2048)
	private String resumeUrl;

	@Column(name = "available_for_work", nullable = false)
	private boolean availableForWork;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	public Profile(UUID accountId, String firstName, String lastName) {
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
		this.firstName = requireText(firstName, "firstName");
		this.lastName = requireText(lastName, "lastName");
	}

	public void updateName(String firstName, String lastName) {
		this.firstName = requireText(firstName, "firstName");
		this.lastName = requireText(lastName, "lastName");
	}

	public void updatePresentation(String headline, String bio, String location) {
		this.headline = trimToNull(headline);
		this.bio = trimToNull(bio);
		this.location = trimToNull(location);
	}

	public void updateMedia(String avatarUrl, String resumeUrl) {
		this.avatarUrl = trimToNull(avatarUrl);
		this.resumeUrl = trimToNull(resumeUrl);
	}

	public void setAvailableForWork(boolean availableForWork) {
		this.availableForWork = availableForWork;
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
