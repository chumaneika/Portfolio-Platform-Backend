package com.portfolio.portfolio_service.services.exceptions;

import java.util.Objects;

public class ProjectTechnologyServiceException extends RuntimeException {

	private final Reason reason;

	private ProjectTechnologyServiceException(Reason reason, String message) {
		super(message);
		this.reason = Objects.requireNonNull(reason, "reason must not be null");
	}

	public static ProjectTechnologyServiceException portfolioNotFound() {
		return new ProjectTechnologyServiceException(Reason.PORTFOLIO_NOT_FOUND, "Portfolio was not found");
	}

	public static ProjectTechnologyServiceException projectNotFound() {
		return new ProjectTechnologyServiceException(Reason.PROJECT_NOT_FOUND, "Project was not found");
	}

	public static ProjectTechnologyServiceException technologyNotFound() {
		return new ProjectTechnologyServiceException(Reason.TECHNOLOGY_NOT_FOUND, "Project technology was not found");
	}

	public static ProjectTechnologyServiceException technologyAlreadyExists() {
		return new ProjectTechnologyServiceException(Reason.TECHNOLOGY_ALREADY_EXISTS,
				"Technology code already exists in this project");
	}

	public Reason getReason() {
		return reason;
	}

	public enum Reason {
		PORTFOLIO_NOT_FOUND,
		PROJECT_NOT_FOUND,
		TECHNOLOGY_NOT_FOUND,
		TECHNOLOGY_ALREADY_EXISTS
	}
}
