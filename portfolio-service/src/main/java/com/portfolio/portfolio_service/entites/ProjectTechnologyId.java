package com.portfolio.portfolio_service.entites;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTechnologyId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "technology_code", nullable = false, length = 50)
	private String technologyCode;
}
