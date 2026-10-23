package com.portfolio.portfolio_service.entites;

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
@Table(name = "project_technologies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTechnology {

	@EmbeddedId
	private ProjectTechnologyId id;

	@MapsId("projectId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	public ProjectTechnology(Project project, String technologyCode, String displayName, int displayOrder) {
		this.project = Objects.requireNonNull(project, "project must not be null");
		this.id = new ProjectTechnologyId(null, normalizeCode(technologyCode));
		this.displayName = requireText(displayName, "displayName");
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	public String getTechnologyCode() {
		return id.getTechnologyCode();
	}

	public void updatePresentation(String displayName, int displayOrder) {
		this.displayName = requireText(displayName, "displayName");
		this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
	}

	private static String normalizeCode(String value) {
		return requireText(value, "technologyCode").toLowerCase(Locale.ROOT);
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
}
